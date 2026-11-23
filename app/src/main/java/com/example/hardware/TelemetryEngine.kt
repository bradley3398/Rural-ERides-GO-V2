package com.example.hardware

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.LiveTelemetrySnapshot
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "TelemetryEngine"

internal class CrashImpactDetector(
  private val gracePeriodMs: Long = 3_000L,
  private val sampleWindowMs: Long = 350L,
  private val cooldownMs: Long = 5_000L,
) {
  private var consecutiveHighSamples = 0
  private var firstHighSampleMs = 0L
  private var cooldownUntilMs = 0L

  fun reset(nowMs: Long = System.currentTimeMillis(), applyCooldown: Boolean = false) {
    consecutiveHighSamples = 0
    firstHighSampleMs = 0L
    if (applyCooldown) {
      cooldownUntilMs = nowMs + cooldownMs
    }
  }

  fun evaluateSample(
    recording: Boolean,
    gForce: Float,
    thresholdG: Float,
    rideStartTimeMs: Long,
    nowMs: Long = System.currentTimeMillis(),
  ): Boolean {
    if (!recording || rideStartTimeMs <= 0L) {
      reset()
      return false
    }
    if (nowMs - rideStartTimeMs < gracePeriodMs || nowMs < cooldownUntilMs) {
      consecutiveHighSamples = 0
      return false
    }
    val effectiveThreshold = thresholdG.coerceIn(6.0f, 15.0f)
    if (gForce < effectiveThreshold) {
      consecutiveHighSamples = 0
      firstHighSampleMs = 0L
      return false
    }

    if (consecutiveHighSamples == 0 || (nowMs - firstHighSampleMs) > sampleWindowMs) {
      consecutiveHighSamples = 1
      firstHighSampleMs = nowMs
    } else {
      consecutiveHighSamples += 1
    }

    val extremeThreshold = maxOf(effectiveThreshold * 1.45f, 12.5f)
    val confirmed = consecutiveHighSamples >= 3 || (gForce >= extremeThreshold && consecutiveHighSamples >= 2)
    if (confirmed) {
      consecutiveHighSamples = 0
      firstHighSampleMs = 0L
    }
    return confirmed
  }
}

class TelemetryEngine(context: Context) {

  private val appContext = context.applicationContext
  private val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)
  private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
  private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  private val _telemetry = MutableStateFlow(LiveTelemetrySnapshot())
  val telemetry: StateFlow<LiveTelemetrySnapshot> = _telemetry.asStateFlow()

  private var locationStarted = false
  private var nativeListenerRegistered = false
  private var gnssRegistered = false
  private var activeIntervalMs = -1L
  private var sensorsStarted = false
  private var requestedPollingHz = 10
  private var requestedUiThrottleMs = 80L
  private var lastLocation: Location? = null
  private var lastLocationUpdatedMs: Long = 0L
  private var lastAltitudeM: Double? = null
  private var rideStartTimeMs: Long = 0L
  private var crashThresholdG: Float = 8.0f
  private var gpsAccuracyLimitM: Int = 25

  // Background Looper for location & GNSS updates
  private var locationThread: HandlerThread? = null
  private var locationHandler: Handler? = null

  // Dedicated Background Service + Fallback AhrsStreamProcessor for zero-delay startup
  private var localFallbackProcessor: AhrsStreamProcessor? = null
  private var boundAhrsService: AhrsSensorService? = null
  private var serviceBound = false
  private var ahrsFlowCollectorJob: Job? = null

  private val ahrsServiceConnection =
    object : ServiceConnection {
      override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as? AhrsSensorService.AhrsBinder ?: return
        val ahrsService = binder.getService()
        boundAhrsService = ahrsService
        localFallbackProcessor?.stopSampling()
        localFallbackProcessor?.shutdown()
        localFallbackProcessor = null

        ahrsService.processor.updateCrashConfig(crashThresholdG)
        if (_telemetry.value.recording) {
          ahrsService.processor.setRideRecordingState(true, rideStartTimeMs)
        }
        if (sensorsStarted) {
          ahrsService.processor.startSampling(requestedPollingHz, requestedUiThrottleMs)
        }
        attachAhrsFlow(ahrsService.uiThrottledFlow)
      }

      override fun onServiceDisconnected(name: ComponentName?) {
        boundAhrsService = null
        serviceBound = false
        if (sensorsStarted) {
          val fallback = ensureFallbackProcessor()
          fallback.updateCrashConfig(crashThresholdG)
          fallback.startSampling(requestedPollingHz, requestedUiThrottleMs)
          attachAhrsFlow(fallback.uiThrottledFlow)
        }
      }
    }

  private fun ensureFallbackProcessor(): AhrsStreamProcessor {
    val existing = localFallbackProcessor
    if (existing != null) return existing
    return AhrsStreamProcessor(appContext).also { localFallbackProcessor = it }
  }

  private fun activeProcessor(): AhrsStreamProcessor =
    boundAhrsService?.processor ?: ensureFallbackProcessor()

  private fun attachAhrsFlow(flow: StateFlow<AhrsReading>) {
    ahrsFlowCollectorJob?.cancel()
    ahrsFlowCollectorJob =
      engineScope.launch {
        flow.collect { reading ->
          _telemetry.update { cur ->
            val nextBearing = if (cur.speedKmh < 2.0) reading.yawDeg else cur.bearingDeg
            cur.copy(
              pitchDeg = reading.pitchDeg,
              rollDeg = reading.rollDeg,
              yawDeg = reading.yawDeg,
              bearingDeg = nextBearing,
              gyroPitchRateDps = reading.gyroPitchRateDps,
              gyroRollRateDps = reading.gyroRollRateDps,
              gyroYawRateDps = reading.gyroYawRateDps,
              magMicroTesla = reading.magMicroTesla,
              ahrsFused9Dof = reading.ahrsFused9Dof,
              sensorAccuracy = reading.sensorAccuracy,
              peakLeanLeftDeg = reading.peakLeanLeftDeg,
              peakLeanRightDeg = reading.peakLeanRightDeg,
              lateralG = reading.lateralG,
              longitudinalG = reading.longitudinalG,
              peakG = maxOf(cur.peakG, reading.peakG),
              crashDetected = cur.crashDetected || reading.crashDetected,
              roadRoughnessG = reading.roadRoughnessG,
              barometerHpa = reading.barometerHpa,
              baroAltitudeM = reading.baroAltitudeM,
              ambientLightLux = reading.ambientLightLux,
              proximityNear = reading.proximityNear,
              ambientTempC = reading.ambientTempC,
              stepCountSession = reading.stepCountSession,
              activeSensorsList = reading.activeSensorsList,
            )
          }
        }
      }
  }

  private fun ensureLocationLooper(): Looper {
    val t = locationThread
    if (t != null && t.isAlive) return t.looper
    val created = HandlerThread("GpsTelemetryWorker", Process.THREAD_PRIORITY_BACKGROUND).apply { start() }
    locationThread = created
    locationHandler = Handler(created.looper)
    return created.looper
  }

  private val gnssStatusCallback =
    object : GnssStatus.Callback() {
      override fun onSatelliteStatusChanged(status: GnssStatus) {
        val visible = status.satelliteCount
        var used = 0
        var snrSum = 0f
        var snrCount = 0
        val constellations = linkedSetOf<String>()
        for (i in 0 until visible) {
          if (status.usedInFix(i)) {
            used += 1
          }
          val cn0 = status.getCn0DbHz(i)
          if (cn0 > 0f) {
            snrSum += cn0
            snrCount += 1
          }
          val tag =
            when (status.getConstellationType(i)) {
              GnssStatus.CONSTELLATION_GPS -> "GPS"
              GnssStatus.CONSTELLATION_GLONASS -> "GLONASS"
              GnssStatus.CONSTELLATION_GALILEO -> "Galileo"
              GnssStatus.CONSTELLATION_BEIDOU -> "BeiDou"
              GnssStatus.CONSTELLATION_QZSS -> "QZSS"
              GnssStatus.CONSTELLATION_SBAS -> "SBAS"
              else -> ""
            }
          if (tag.isNotBlank()) constellations += tag
        }
        val avgSnr = if (snrCount > 0) snrSum / snrCount else 0f
        _telemetry.update { cur ->
          cur.copy(
            satellitesUsed = used,
            satellitesVisible = visible,
            avgSatelliteSnrDb = avgSnr,
            satelliteConstellations = constellations.joinToString(" • "),
          )
        }
      }

      override fun onStopped() {
        _telemetry.update { it.copy(satellitesUsed = 0, satellitesVisible = 0, avgSatelliteSnrDb = 0f) }
      }
    }

  private val locationCallback =
    object : LocationCallback() {
      override fun onLocationResult(result: LocationResult) {
        val loc = result.lastLocation ?: return
        processLocation(loc)
      }
    }

  private val nativeLocationListener =
    object : LocationListener {
      override fun onLocationChanged(location: Location) {
        processLocation(location)
      }

      @Deprecated("Required for API 24 compatibility")
      override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

      override fun onProviderEnabled(provider: String) {
        refreshLocationNow()
      }

      override fun onProviderDisabled(provider: String) = Unit
    }

  fun updateConfig(accuracyLimitM: Int, crashG: Float) {
    gpsAccuracyLimitM = accuracyLimitM.coerceIn(5, 100)
    crashThresholdG = crashG.coerceIn(6.0f, 15.0f)
    boundAhrsService?.processor?.updateCrashConfig(crashThresholdG)
    localFallbackProcessor?.updateCrashConfig(crashThresholdG)
  }

  fun hasLocationPermission(): Boolean {
    val fine =
      ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    val coarse =
      ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    return fine || coarse
  }

  @SuppressLint("MissingPermission")
  fun startLocationUpdates(intervalMs: Long = 1000L) {
    if (!hasLocationPermission()) return
    if (locationStarted && activeIntervalMs == intervalMs) {
      if (_telemetry.value.lat == 0.0 && _telemetry.value.lng == 0.0) {
        seedBestLastKnownLocation()
      }
      return
    }
    if (locationStarted) {
      stopLocationUpdates()
    }
    activeIntervalMs = intervalMs
    val workerLooper = ensureLocationLooper()
    seedBestLastKnownLocation()
    try {
      val request =
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
          .setMinUpdateIntervalMillis(maxOf(400L, intervalMs / 2))
          .setWaitForAccurateLocation(false)
          .build()
      fusedClient.requestLocationUpdates(request, locationCallback, workerLooper)
      locationStarted = true
    } catch (e: Exception) {
      Log.e(TAG, "Failed starting fused location", e)
    }

    try {
      val lm = locationManager
      if (lm != null && !nativeListenerRegistered) {
        val minTime = maxOf(500L, intervalMs)
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
          lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, 0f, nativeLocationListener, workerLooper)
        }
        if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
          lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, 0f, nativeLocationListener, workerLooper)
        }
        nativeListenerRegistered = true
      }
    } catch (e: Exception) {
      Log.w(TAG, "Native LocationManager listener fallback skipped: ${e.message}")
    }

    try {
      val lm = locationManager
      val hasFine =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
          PackageManager.PERMISSION_GRANTED
      if (lm != null && hasFine && !gnssRegistered) {
        val gnssHandler = locationHandler ?: Handler(workerLooper)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          lm.registerGnssStatusCallback({ cmd -> gnssHandler.post(cmd) }, gnssStatusCallback)
        } else {
          @Suppress("DEPRECATION")
          lm.registerGnssStatusCallback(gnssStatusCallback, gnssHandler)
        }
        gnssRegistered = true
      }
    } catch (e: Exception) {
      Log.w(TAG, "GNSS satellite status callback skipped: ${e.message}")
    }
  }

  @SuppressLint("MissingPermission")
  fun refreshLocationNow() {
    if (!hasLocationPermission()) return
    seedBestLastKnownLocation()
    if (!locationStarted) {
      startLocationUpdates(if (activeIntervalMs > 0L) activeIntervalMs else 1000L)
    }
  }

  @SuppressLint("MissingPermission")
  private fun seedBestLastKnownLocation() {
    if (!hasLocationPermission()) return
    runCatching {
      fusedClient.lastLocation.addOnSuccessListener { loc ->
        if (loc != null) engineScope.launch { processLocation(loc) }
      }
      val curReq = CurrentLocationRequest.Builder()
        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
        .setMaxUpdateAgeMillis(15_000L)
        .build()
      fusedClient.getCurrentLocation(curReq, null).addOnSuccessListener { loc ->
        if (loc != null) engineScope.launch { processLocation(loc) }
      }
    }
    runCatching {
      val lm = locationManager ?: return@runCatching
      val candidates =
        listOf(
          LocationManager.GPS_PROVIDER,
          LocationManager.NETWORK_PROVIDER,
          LocationManager.PASSIVE_PROVIDER,
        ).mapNotNull { prov -> runCatching { lm.getLastKnownLocation(prov) }.getOrNull() }
      val best = candidates.maxByOrNull { it.time }
      if (best != null) engineScope.launch { processLocation(best) }
    }
  }

  fun stopLocationUpdates() {
    if (locationStarted) {
      runCatching { fusedClient.removeLocationUpdates(locationCallback) }
      locationStarted = false
    }
    if (nativeListenerRegistered) {
      runCatching { locationManager?.removeUpdates(nativeLocationListener) }
      nativeListenerRegistered = false
    }
    if (gnssRegistered) {
      runCatching { locationManager?.unregisterGnssStatusCallback(gnssStatusCallback) }
      gnssRegistered = false
    }
  }

  fun startSensors(pollingHz: Int = 10, uiThrottleMs: Long = 80L) {
    val cleanHz = pollingHz.coerceIn(5, 50)
    val cleanThrottle = uiThrottleMs.coerceIn(50L, 250L)
    if (sensorsStarted && requestedPollingHz == cleanHz && requestedUiThrottleMs == cleanThrottle) {
      return
    }
    requestedPollingHz = cleanHz
    requestedUiThrottleMs = cleanThrottle
    sensorsStarted = true

    // Bind dedicated background AhrsSensorService if not yet bound
    if (!serviceBound) {
      runCatching {
        val intent = Intent(appContext, AhrsSensorService::class.java)
        serviceBound = appContext.bindService(intent, ahrsServiceConnection, Context.BIND_AUTO_CREATE)
      }
    }

    val proc = activeProcessor()
    proc.updateCrashConfig(crashThresholdG)
    proc.startSampling(cleanHz, cleanThrottle)
    attachAhrsFlow(proc.uiThrottledFlow)
  }

  fun stopSensors() {
    if (!sensorsStarted) return
    sensorsStarted = false
    ahrsFlowCollectorJob?.cancel()
    ahrsFlowCollectorJob = null
    boundAhrsService?.processor?.stopSampling()
    localFallbackProcessor?.stopSampling()
  }

  fun shutdown() {
    stopSensors()
    stopLocationUpdates()
    if (serviceBound) {
      runCatching { appContext.unbindService(ahrsServiceConnection) }
      serviceBound = false
    }
    boundAhrsService = null
    localFallbackProcessor?.shutdown()
    localFallbackProcessor = null
    locationThread?.quitSafely()
    locationThread = null
    locationHandler = null
    engineScope.cancel()
  }

  fun calibrateLevelZero() {
    activeProcessor().calibrateLevelZero()
    _telemetry.update { it.copy(pitchDeg = 0f, rollDeg = 0f, peakLeanLeftDeg = 0f, peakLeanRightDeg = 0f) }
  }

  fun calibrateAhrsAllAxes(includeYaw: Boolean = false) {
    activeProcessor().calibrateAhrsAllAxes(includeYaw)
    val yawNow = activeProcessor().uiThrottledFlow.value.yawDeg
    _telemetry.update {
      it.copy(
        pitchDeg = 0f,
        rollDeg = 0f,
        yawDeg = if (includeYaw) 0f else yawNow,
        peakLeanLeftDeg = 0f,
        peakLeanRightDeg = 0f,
      )
    }
  }

  fun resetPeakLeanAngles() {
    activeProcessor().resetPeakLeanAngles()
    _telemetry.update { it.copy(peakLeanLeftDeg = 0f, peakLeanRightDeg = 0f) }
  }

  fun startRideRecording() {
    val nowMs = System.currentTimeMillis()
    rideStartTimeMs = nowMs
    activeProcessor().setRideRecordingState(true, nowMs)
    _telemetry.update {
      val hasFix = it.lat != 0.0 || it.lng != 0.0
      it.copy(
        recording = true,
        launchLat = if (hasFix) it.lat else null,
        launchLng = if (hasFix) it.lng else null,
        tripDistanceKm = 0.0,
        maxSpeedKmh = 0.0,
        avgSpeedKmh = 0.0,
        elapsedSec = 0L,
        elevationGainM = 0.0,
        peakG = 0f,
        crashDetected = false,
        trailPoints = if (hasFix) listOf(it.lat to it.lng) else emptyList(),
      )
    }
  }

  fun stopRideRecording(): LiveTelemetrySnapshot {
    activeProcessor().setRideRecordingState(false)
    val finalSnap = _telemetry.value.copy(recording = false)
    _telemetry.value = finalSnap
    return finalSnap
  }

  fun dismissCrashAlert() {
    activeProcessor().dismissCrashAlert()
    _telemetry.update { it.copy(crashDetected = false) }
  }

  fun updateSpeedLimitMeta(roadName: String, limitMph: Int?, inSchoolZone: Boolean) {
    val sanitizedLimit = limitMph?.takeIf { it in 5..85 }
    _telemetry.update {
      it.copy(roadName = roadName, speedLimitMph = sanitizedLimit, inSchoolZone = inSchoolZone)
    }
  }

  fun updateBatteryEstimate(pct: Int, volts: Double) {
    _telemetry.update { it.copy(batteryPct = pct.coerceIn(0, 100), estimatedVoltage = volts) }
  }

  private fun processLocation(loc: Location) {
    if (loc.latitude == 0.0 && loc.longitude == 0.0) return
    val prev = lastLocation
    val nowMs = System.currentTimeMillis()
    if (prev != null) {
      if (loc.time > 0L && prev.time > 0L && loc.time + 5_000L < prev.time) return
      val recentFineFix = (nowMs - lastLocationUpdatedMs) < 5_000L && prev.hasAccuracy() && prev.accuracy <= 25f
      if (recentFineFix && loc.hasAccuracy() && loc.accuracy > 100f) return
    }
    val speedKmh = if (loc.hasSpeed()) (loc.speed * 3.6).coerceAtLeast(0.0) else 0.0
    val accuracy = loc.accuracy
    val acceptable = accuracy <= gpsAccuracyLimitM || accuracy == 0f
    lastLocation = loc
    lastLocationUpdatedMs = nowMs

    _telemetry.update { cur ->
      var nextDistKm = cur.tripDistanceKm
      var nextGainM = cur.elevationGainM
      var nextTrail = cur.trailPoints
      var nextElapsed = cur.elapsedSec

      if (cur.recording && rideStartTimeMs > 0L) {
        nextElapsed = ((System.currentTimeMillis() - rideStartTimeMs) / 1000L).coerceAtLeast(1L)
        if (acceptable && prev != null) {
          val deltaM = prev.distanceTo(loc).toDouble()
          if (deltaM in 1.5..400.0) {
            nextDistKm += (deltaM / 1000.0)
            nextTrail = (nextTrail + (loc.latitude to loc.longitude)).takeLast(400)
          }
        }
        if (loc.hasAltitude()) {
          val prevAlt = lastAltitudeM
          if (prevAlt != null && loc.altitude > prevAlt + 0.8 && loc.altitude - prevAlt < 50.0) {
            nextGainM += (loc.altitude - prevAlt)
          }
          lastAltitudeM = loc.altitude
        }
      }

      var nextLaunchLat = cur.launchLat
      var nextLaunchLng = cur.launchLng
      if (cur.recording && nextLaunchLat == null && nextLaunchLng == null) {
        nextLaunchLat = loc.latitude
        nextLaunchLng = loc.longitude
        if (nextTrail.isEmpty()) nextTrail = listOf(loc.latitude to loc.longitude)
      }

      val nextMax = if (speedKmh > cur.maxSpeedKmh) speedKmh else cur.maxSpeedKmh
      val nextAvg =
        if (cur.recording && nextElapsed > 0) (nextDistKm / (nextElapsed / 3600.0)).coerceIn(0.0, 200.0)
        else cur.avgSpeedKmh

      cur.copy(
        lat = loc.latitude,
        lng = loc.longitude,
        launchLat = nextLaunchLat,
        launchLng = nextLaunchLng,
        altitudeM = if (loc.hasAltitude()) loc.altitude else cur.altitudeM,
        speedKmh = speedKmh,
        maxSpeedKmh = nextMax,
        avgSpeedKmh = nextAvg,
        tripDistanceKm = nextDistKm,
        elapsedSec = nextElapsed,
        elevationGainM = nextGainM,
        gpsAccuracyM = accuracy,
        bearingDeg = if (loc.hasBearing()) loc.bearing else cur.bearingDeg,
        trailPoints = nextTrail,
      )
    }
  }
}
