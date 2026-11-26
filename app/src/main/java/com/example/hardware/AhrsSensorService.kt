package com.example.hardware

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Process
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Immutable 9-DOF AHRS + environmental sensor snapshot produced at a fixed rate
 * off the Android main thread and throttled prior to UI state emission.
 */
data class AhrsReading(
  val pitchDeg: Float = 0f,
  val rollDeg: Float = 0f,
  val yawDeg: Float = 0f,
  val gyroPitchRateDps: Float = 0f,
  val gyroRollRateDps: Float = 0f,
  val gyroYawRateDps: Float = 0f,
  val magMicroTesla: Float = 0f,
  val ahrsFused9Dof: Boolean = false,
  val sensorAccuracy: Int = 0,
  val peakLeanLeftDeg: Float = 0f,
  val peakLeanRightDeg: Float = 0f,
  val lateralG: Float = 0f,
  val longitudinalG: Float = 0f,
  val peakG: Float = 0f,
  val crashDetected: Boolean = false,
  val roadRoughnessG: Float = 0f,
  val barometerHpa: Float = 0f,
  val baroAltitudeM: Float = 0f,
  val ambientLightLux: Float = -1f,
  val proximityNear: Boolean = false,
  val ambientTempC: Float? = null,
  val stepCountSession: Int = 0,
  val activeSensorsList: List<String> = emptyList(),
  val sampleRateHz: Int = 10,
  val sampleSeq: Long = 0L,
)

/**
 * Dedicated background 9-DOF AHRS processor.
 * - Executes all SensorEventListener callbacks on a dedicated HandlerThread (never the main thread).
 * - Samples sensor staging state at a deterministic fixed rate on Dispatchers.Default.
 * - Throttles UI StateFlow updates via Flow.sample() so Compose never freezes from sensor bursts.
 */
@OptIn(FlowPreview::class)
class AhrsStreamProcessor(context: Context) : SensorEventListener {

  private val appContext = context.applicationContext
  private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  private val crashDetector = CrashImpactDetector()

  // Dedicated worker thread for SensorManager callbacks (keeps Main Looper 100% free)
  private var workerThread: HandlerThread? = null
  private var workerHandler: Handler? = null

  // Worker-thread-confined scratch matrices to avoid per-event GC allocations
  private val rotMatrix = FloatArray(9)
  private val incMatrix = FloatArray(9)
  private val orientRad = FloatArray(3)
  private val gravityVec = FloatArray(3)
  private val geomagVec = FloatArray(3)

  // Thread-safe staging state synchronized via stateLock
  private val stateLock = Any()
  private var staged = AhrsReading()
  private var pitchOffsetDeg: Float = 0f
  private var rollOffsetDeg: Float = 0f
  private var yawOffsetDeg: Float = 0f
  private var rawPitchDeg: Float = 0f
  private var rawRollDeg: Float = 0f
  private var rawYawDeg: Float = 0f
  private var hasRotationVectorFix: Boolean = false
  private var smoothedRoughnessG: Float = 0f
  private var stepBaseline: Float? = null

  private var recordingActive: Boolean = false
  private var rideStartTimeMs: Long = 0L
  private var crashThresholdG: Float = 8.0f

  private var sensorsRegistered = false
  private var activePollingHz = -1
  private var activeUiThrottleMs = 80L
  private var samplerJob: Job? = null
  private var throttleJob: Job? = null
  private var sampleSequence = 0L

  private val _fixedRateFlow =
    MutableSharedFlow<AhrsReading>(
      replay = 1,
      extraBufferCapacity = 4,
      onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
  val fixedRateFlow: SharedFlow<AhrsReading> = _fixedRateFlow.asSharedFlow()

  private val _uiThrottledFlow = MutableStateFlow(AhrsReading())
  val uiThrottledFlow: StateFlow<AhrsReading> = _uiThrottledFlow.asStateFlow()

  fun startSampling(pollingHz: Int = 10, uiThrottleMs: Long = 80L) {
    val cleanHz = pollingHz.coerceIn(5, 50)
    val cleanThrottle = uiThrottleMs.coerceIn(50L, 250L)

    if (sensorsRegistered && activePollingHz == cleanHz && activeUiThrottleMs == cleanThrottle) {
      return
    }

    if (sensorsRegistered) {
      unregisterHardwareListeners()
    }

    activePollingHz = cleanHz
    activeUiThrottleMs = cleanThrottle

    if (workerThread == null || workerThread?.isAlive != true) {
      workerThread = HandlerThread("AhrsSensorWorker", Process.THREAD_PRIORITY_MORE_FAVORABLE).apply { start() }
      workerHandler = Handler(workerThread!!.looper)
    }
    val handler = workerHandler
    val delayUs = (1_000_000 / cleanHz)
    val detected = mutableListOf<String>()

    fun register(type: Int, label: String, rateUs: Int = delayUs) {
      sensorManager.getDefaultSensor(type)?.let { sensor ->
        val ok =
          if (handler != null) sensorManager.registerListener(this, sensor, rateUs, handler)
          else sensorManager.registerListener(this, sensor, rateUs)
        if (ok) detected += label
      }
    }

    register(Sensor.TYPE_ACCELEROMETER, "Accelerometer")
    register(Sensor.TYPE_GYROSCOPE, "Gyroscope")
    register(Sensor.TYPE_MAGNETIC_FIELD, "Compass")
    register(Sensor.TYPE_ROTATION_VECTOR, "3D Tilt & Heading")
    register(Sensor.TYPE_LINEAR_ACCELERATION, "Road Vibration")
    register(Sensor.TYPE_PRESSURE, "Barometer", SensorManager.SENSOR_DELAY_NORMAL)
    register(Sensor.TYPE_LIGHT, "Light Sensor", SensorManager.SENSOR_DELAY_NORMAL)
    register(Sensor.TYPE_PROXIMITY, "Proximity", SensorManager.SENSOR_DELAY_NORMAL)
    register(Sensor.TYPE_AMBIENT_TEMPERATURE, "Ambient Temp", SensorManager.SENSOR_DELAY_NORMAL)
    register(Sensor.TYPE_STEP_COUNTER, "Step Counter", SensorManager.SENSOR_DELAY_NORMAL)

    synchronized(stateLock) {
      staged = staged.copy(activeSensorsList = detected, sampleRateHz = cleanHz)
    }
    _uiThrottledFlow.value = synchronized(stateLock) { staged }
    sensorsRegistered = true

    startFlowPipelines(cleanHz, cleanThrottle)
  }

  private fun startFlowPipelines(pollingHz: Int, uiThrottleMs: Long) {
    samplerJob?.cancel()
    throttleJob?.cancel()

    val periodMs = (1000L / pollingHz.coerceIn(5, 50)).coerceAtLeast(20L)

    // Fixed-rate background sampler loop
    samplerJob =
      scope.launch {
        while (isActive) {
          val snapshot =
            synchronized(stateLock) {
              sampleSequence += 1L
              staged.copy(sampleRateHz = pollingHz, sampleSeq = sampleSequence)
            }
          _fixedRateFlow.tryEmit(snapshot)
          delay(periodMs)
        }
      }

    // Throttled UI-safe pipeline offloaded to Dispatchers.Default
    throttleJob =
      scope.launch {
        _fixedRateFlow
          .conflate()
          .sample(uiThrottleMs)
          .flowOn(Dispatchers.Default)
          .collect { reading ->
            _uiThrottledFlow.value = reading
          }
      }
  }

  private fun unregisterHardwareListeners() {
    runCatching { sensorManager.unregisterListener(this) }
    sensorsRegistered = false
  }

  fun stopSampling() {
    samplerJob?.cancel()
    samplerJob = null
    throttleJob?.cancel()
    throttleJob = null
    unregisterHardwareListeners()
  }

  fun shutdown() {
    stopSampling()
    workerThread?.quitSafely()
    workerThread = null
    workerHandler = null
    scope.cancel()
  }

  fun updateCrashConfig(thresholdG: Float) {
    synchronized(stateLock) {
      crashThresholdG = thresholdG.coerceIn(6.0f, 15.0f)
    }
  }

  fun setRideRecordingState(recording: Boolean, startTimeMs: Long = System.currentTimeMillis()) {
    synchronized(stateLock) {
      recordingActive = recording
      rideStartTimeMs = if (recording) startTimeMs else 0L
      crashDetector.reset(startTimeMs, applyCooldown = false)
      staged =
        if (recording) {
          staged.copy(peakG = 0f, crashDetected = false)
        } else {
          staged
        }
      _uiThrottledFlow.value = staged
    }
  }

  fun dismissCrashAlert() {
    synchronized(stateLock) {
      crashDetector.reset(System.currentTimeMillis(), applyCooldown = true)
      staged = staged.copy(crashDetected = false)
      _uiThrottledFlow.value = staged
    }
  }

  fun calibrateLevelZero() {
    synchronized(stateLock) {
      pitchOffsetDeg = rawPitchDeg
      rollOffsetDeg = rawRollDeg
      staged = staged.copy(pitchDeg = 0f, rollDeg = 0f, peakLeanLeftDeg = 0f, peakLeanRightDeg = 0f)
      _uiThrottledFlow.value = staged
    }
  }

  fun calibrateAhrsAllAxes(includeYaw: Boolean = false) {
    synchronized(stateLock) {
      pitchOffsetDeg = rawPitchDeg
      rollOffsetDeg = rawRollDeg
      yawOffsetDeg = if (includeYaw) rawYawDeg else 0f
      staged =
        staged.copy(
          pitchDeg = 0f,
          rollDeg = 0f,
          yawDeg = if (includeYaw) 0f else rawYawDeg,
          peakLeanLeftDeg = 0f,
          peakLeanRightDeg = 0f,
        )
      _uiThrottledFlow.value = staged
    }
  }

  fun resetPeakLeanAngles() {
    synchronized(stateLock) {
      staged = staged.copy(peakLeanLeftDeg = 0f, peakLeanRightDeg = 0f)
      _uiThrottledFlow.value = staged
    }
  }

  fun resetCalibrationOffsets() {
    synchronized(stateLock) {
      pitchOffsetDeg = 0f
      rollOffsetDeg = 0f
      yawOffsetDeg = 0f
      staged =
        staged.copy(
          pitchDeg = rawPitchDeg,
          rollDeg = rawRollDeg,
          yawDeg = rawYawDeg,
          peakLeanLeftDeg = 0f,
          peakLeanRightDeg = 0f,
        )
      _uiThrottledFlow.value = staged
    }
  }

  override fun onSensorChanged(event: SensorEvent) {
    when (event.sensor.type) {
      Sensor.TYPE_ROTATION_VECTOR -> {
        SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
        SensorManager.getOrientation(rotMatrix, orientRad)
        val azimuthDeg = ((Math.toDegrees(orientRad[0].toDouble()) + 360.0) % 360.0).toFloat()
        val pitchDegRaw = (-Math.toDegrees(orientRad[1].toDouble())).toFloat().coerceIn(-90f, 90f)
        val rollDegRaw = Math.toDegrees(orientRad[2].toDouble()).toFloat().coerceIn(-90f, 90f)

        synchronized(stateLock) {
          hasRotationVectorFix = true
          rawYawDeg = azimuthDeg
          rawPitchDeg = pitchDegRaw
          rawRollDeg = rollDegRaw

          val adjPitch = (rawPitchDeg - pitchOffsetDeg).coerceIn(-90f, 90f)
          val adjRoll = (rawRollDeg - rollOffsetDeg).coerceIn(-90f, 90f)
          val adjYaw = ((rawYawDeg - yawOffsetDeg + 360f) % 360f)
          val nextPeakLeft = if (adjRoll < 0f) maxOf(staged.peakLeanLeftDeg, kotlin.math.abs(adjRoll)) else staged.peakLeanLeftDeg
          val nextPeakRight = if (adjRoll > 0f) maxOf(staged.peakLeanRightDeg, adjRoll) else staged.peakLeanRightDeg

          staged =
            staged.copy(
              pitchDeg = adjPitch,
              rollDeg = adjRoll,
              yawDeg = adjYaw,
              ahrsFused9Dof = true,
              peakLeanLeftDeg = nextPeakLeft,
              peakLeanRightDeg = nextPeakRight,
            )
        }
      }
      Sensor.TYPE_GYROSCOPE -> {
        val wx = Math.toDegrees(event.values[0].toDouble()).toFloat()
        val wy = Math.toDegrees(event.values[1].toDouble()).toFloat()
        val wz = Math.toDegrees(event.values[2].toDouble()).toFloat()
        synchronized(stateLock) {
          staged = staged.copy(gyroPitchRateDps = wx, gyroRollRateDps = wy, gyroYawRateDps = wz)
        }
      }
      Sensor.TYPE_ACCELEROMETER -> {
        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]
        gravityVec[0] = ax
        gravityVec[1] = ay
        gravityVec[2] = az
        val gForce = sqrt(ax * ax + ay * ay + az * az) / SensorManager.GRAVITY_EARTH
        val linearLatG = kotlin.math.abs(gForce - 1.0f)

        val fallbackPitch = Math.toDegrees(atan2(-ax.toDouble(), sqrt((ay * ay + az * az).toDouble()))).toFloat()
        val fallbackRoll = Math.toDegrees(atan2(ay.toDouble(), az.toDouble())).toFloat()
        val nowMs = System.currentTimeMillis()
        var emergencyCrashSnap: AhrsReading? = null

        synchronized(stateLock) {
          if (!hasRotationVectorFix) {
            rawPitchDeg = fallbackPitch
            rawRollDeg = fallbackRoll
          }
          val confirmedCrash =
            !staged.crashDetected &&
              crashDetector.evaluateSample(
                recording = recordingActive,
                gForce = gForce,
                thresholdG = crashThresholdG,
                rideStartTimeMs = rideStartTimeMs,
                nowMs = nowMs,
              )
          val crash = staged.crashDetected || confirmedCrash
          val nextPitch = if (hasRotationVectorFix) staged.pitchDeg else (rawPitchDeg - pitchOffsetDeg).coerceIn(-90f, 90f)
          val nextRoll = if (hasRotationVectorFix) staged.rollDeg else (rawRollDeg - rollOffsetDeg).coerceIn(-90f, 90f)
          val nextPeakLeft = if (nextRoll < 0f) maxOf(staged.peakLeanLeftDeg, kotlin.math.abs(nextRoll)) else staged.peakLeanLeftDeg
          val nextPeakRight = if (nextRoll > 0f) maxOf(staged.peakLeanRightDeg, nextRoll) else staged.peakLeanRightDeg

          staged =
            staged.copy(
              pitchDeg = nextPitch,
              rollDeg = nextRoll,
              lateralG = linearLatG,
              peakG = maxOf(staged.peakG, gForce),
              peakLeanLeftDeg = nextPeakLeft,
              peakLeanRightDeg = nextPeakRight,
              crashDetected = crash,
            )
          if (confirmedCrash) {
            emergencyCrashSnap = staged
          }
        }
        // Crash triggers immediate UI dispatch outside the regular throttle window
        emergencyCrashSnap?.let { _uiThrottledFlow.value = it }
      }
      Sensor.TYPE_MAGNETIC_FIELD -> {
        val mx = event.values[0]
        val my = event.values[1]
        val mz = event.values[2]
        geomagVec[0] = mx
        geomagVec[1] = my
        geomagVec[2] = mz
        val fluxUt = sqrt(mx * mx + my * my + mz * mz)
        val hasMatrix = !hasRotationVectorFix && SensorManager.getRotationMatrix(rotMatrix, incMatrix, gravityVec, geomagVec)
        if (hasMatrix) {
          SensorManager.getOrientation(rotMatrix, orientRad)
        }
        synchronized(stateLock) {
          if (hasMatrix) {
            val azimuthDeg = ((Math.toDegrees(orientRad[0].toDouble()) + 360.0) % 360.0).toFloat()
            rawYawDeg = azimuthDeg
            val adjYaw = ((rawYawDeg - yawOffsetDeg + 360f) % 360f)
            staged = staged.copy(yawDeg = adjYaw, magMicroTesla = fluxUt)
          } else {
            staged = staged.copy(magMicroTesla = fluxUt)
          }
        }
      }
      Sensor.TYPE_PRESSURE -> {
        val hpa = event.values.firstOrNull() ?: return
        val baroAlt = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, hpa)
        synchronized(stateLock) {
          staged = staged.copy(barometerHpa = hpa, baroAltitudeM = baroAlt)
        }
      }
      Sensor.TYPE_LIGHT -> {
        val lux = (event.values.firstOrNull() ?: return).coerceAtLeast(0f)
        synchronized(stateLock) {
          staged = staged.copy(ambientLightLux = lux)
        }
      }
      Sensor.TYPE_PROXIMITY -> {
        val distCm = event.values.firstOrNull() ?: return
        val maxRange = event.sensor.maximumRange.coerceAtLeast(1f)
        val near = distCm < minOf(4f, maxRange)
        synchronized(stateLock) {
          staged = staged.copy(proximityNear = near)
        }
      }
      Sensor.TYPE_LINEAR_ACCELERATION -> {
        val lx = event.values[0]
        val ly = event.values[1]
        val lz = event.values[2]
        val latSignedG = (lx / SensorManager.GRAVITY_EARTH).coerceIn(-3f, 3f)
        val longSignedG = (ly / SensorManager.GRAVITY_EARTH).coerceIn(-3f, 3f)
        val magG = sqrt(lx * lx + ly * ly + lz * lz) / SensorManager.GRAVITY_EARTH
        synchronized(stateLock) {
          smoothedRoughnessG = (smoothedRoughnessG * 0.78f) + (magG * 0.22f)
          staged = staged.copy(
            lateralG = latSignedG,
            longitudinalG = longSignedG,
            roadRoughnessG = smoothedRoughnessG,
          )
        }
      }
      Sensor.TYPE_AMBIENT_TEMPERATURE -> {
        val tempC = event.values.firstOrNull() ?: return
        synchronized(stateLock) {
          staged = staged.copy(ambientTempC = tempC)
        }
      }
      Sensor.TYPE_STEP_COUNTER -> {
        val total = event.values.firstOrNull() ?: return
        synchronized(stateLock) {
          if (stepBaseline == null) stepBaseline = total
          val delta = (total - (stepBaseline ?: total)).toInt().coerceAtLeast(0)
          staged = staged.copy(stepCountSession = delta)
        }
      }
    }
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    if (sensor?.type == Sensor.TYPE_ROTATION_VECTOR || sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
      synchronized(stateLock) {
        staged = staged.copy(sensorAccuracy = accuracy.coerceIn(0, 3))
      }
    }
  }
}

/**
 * Dedicated bound Android Service hosting the fixed-rate 9-DOF AHRS sensor stream
 * on a background HandlerThread and coroutine flow pipeline.
 */
class AhrsSensorService : Service() {

  inner class AhrsBinder : Binder() {
    fun getService(): AhrsSensorService = this@AhrsSensorService
  }

  private val binder = AhrsBinder()
  lateinit var processor: AhrsStreamProcessor
    private set

  val uiThrottledFlow: StateFlow<AhrsReading>
    get() = processor.uiThrottledFlow

  val fixedRateFlow: SharedFlow<AhrsReading>
    get() = processor.fixedRateFlow

  override fun onCreate() {
    super.onCreate()
    processor = AhrsStreamProcessor(applicationContext)
  }

  override fun onBind(intent: Intent?): IBinder = binder

  override fun onUnbind(intent: Intent?): Boolean {
    processor.stopSampling()
    return super.onUnbind(intent)
  }

  override fun onDestroy() {
    processor.shutdown()
    super.onDestroy()
  }
}
