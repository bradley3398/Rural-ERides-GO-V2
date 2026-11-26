package com.example.hardware

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.util.Base64
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.tanh
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val CALL_MEDIA_TAG = "NativeLiveCallEngine"

/**
 * ITU-T G.711 mu-law 8-bit voice codec for ultra-reliable low-bandwidth two-way call audio.
 * Compresses 16-bit 8kHz PCM by 2x (~8 KB/s) with crisp speech intelligibility.
 */
object G711MuLaw {
  private const val BIAS = 0x84
  private const val CLIP = 32635

  private val expLut =
    intArrayOf(
      0, 0, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3,
      4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
      5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
      5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
      6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
      6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
      6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
      6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
      7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
      7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
      7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
      7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
      7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
      7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
      7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
      7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
    )

  fun encodeSample(sample: Short): Byte {
    var pcm = sample.toInt()
    val sign = if (pcm < 0) {
      pcm = -pcm
      0x80
    } else {
      0x00
    }
    if (pcm > CLIP) pcm = CLIP
    pcm += BIAS
    val exponent = expLut[(pcm shr 7) and 0xFF]
    val mantissa = (pcm shr (exponent + 3)) and 0x0F
    return ((sign or (exponent shl 4) or mantissa).inv() and 0xFF).toByte()
  }

  fun decodeSample(muLawByte: Byte): Short {
    val inv = muLawByte.toInt().inv() and 0xFF
    val sign = inv and 0x80
    val exponent = (inv shr 4) and 0x07
    val mantissa = inv and 0x0F
    var t = ((mantissa shl 3) + BIAS) shl exponent
    t -= BIAS
    return (if (sign != 0) -t else t).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
  }

  fun encodePcm16(pcm: ShortArray, count: Int = pcm.size): ByteArray {
    val len = count.coerceIn(0, pcm.size)
    val out = ByteArray(len)
    for (i in 0 until len) {
      out[i] = encodeSample(pcm[i])
    }
    return out
  }

  fun decodeToPcm16(muLaw: ByteArray, gain: Float = 1.0f): ShortArray {
    val out = ShortArray(muLaw.size)
    val safeGain = gain.coerceIn(0.2f, 2.5f)
    for (i in muLaw.indices) {
      val raw = decodeSample(muLaw[i]).toFloat()
      val boosted =
        if (safeGain > 1.05f) {
          (tanh((raw / 32768.0) * safeGain) * 32700.0).toInt()
        } else {
          (raw * safeGain).toInt()
        }
      out[i] = boosted.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
    return out
  }
}

/**
 * Native Android full-duplex voice transceiver using hardware AudioRecord + AcousticEchoCanceler +
 * NoiseSuppressor + AudioTrack. Streams compact 8kHz G.711 mu-law voice packets over fixed-slot
 * Firestore documents without relying on WebView getUserMedia or STUN/TURN NAT traversal.
 */
class NativeCallVoiceEngine(private val context: Context) {
  companion object {
    const val SAMPLE_RATE = 8000
    private const val CHUNK_SAMPLES = 2560 // 320 ms at 8kHz -> ~3.4 KB base64
  }

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val audioOutQueue = Channel<ByteArray>(capacity = 10, onBufferOverflow = BufferOverflow.DROP_OLDEST)
  private val audioSeq = AtomicLong(0L)

  private var captureJob: Job? = null
  private var playbackJob: Job? = null

  @Volatile var isCallActive: Boolean = false
  @Volatile var isMicEnabled: Boolean = true
  @Volatile var isSpeakerBoost: Boolean = true

  private val _micLevel = MutableStateFlow(0f)
  val micLevel: StateFlow<Float> = _micLevel.asStateFlow()

  private val _voiceRxActive = MutableStateFlow(false)
  val voiceRxActive: StateFlow<Boolean> = _voiceRxActive.asStateFlow()

  private val _engineStatus = MutableStateFlow("Ready")
  val engineStatus: StateFlow<String> = _engineStatus.asStateFlow()

  private var lastPlayedSeq = -1L

  fun start(onPublishAudioChunk: suspend (seq: Long, active: Boolean, base64MuLaw: String) -> Boolean) {
    stop()
    lastPlayedSeq = -1L
    startPlaybackLoop()
    startCaptureLoop(onPublishAudioChunk)
  }

  fun enqueueRemoteAudio(seq: Long, base64MuLaw: String) {
    if (seq <= lastPlayedSeq || base64MuLaw.isBlank()) return
    lastPlayedSeq = seq
    runCatching {
      val bytes = Base64.decode(base64MuLaw, Base64.DEFAULT)
      if (bytes.isNotEmpty()) {
        _voiceRxActive.value = true
        audioOutQueue.trySend(bytes)
      }
    }
  }

  @SuppressLint("MissingPermission")
  private fun startCaptureLoop(
    onPublishAudioChunk: suspend (seq: Long, active: Boolean, base64MuLaw: String) -> Boolean,
  ) {
    val hasPerm =
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    if (!hasPerm) {
      _engineStatus.value = "Mic permission required"
      return
    }

    captureJob =
      scope.launch {
        var recorder: AudioRecord? = null
        var aec: AcousticEchoCanceler? = null
        var ns: NoiseSuppressor? = null
        try {
          val minBuf =
            AudioRecord.getMinBufferSize(
              SAMPLE_RATE,
              AudioFormat.CHANNEL_IN_MONO,
              AudioFormat.ENCODING_PCM_16BIT,
            ).coerceAtLeast(CHUNK_SAMPLES * 2)
          val bufSize = maxOf(minBuf * 2, CHUNK_SAMPLES * 4)

          recorder =
            runCatching {
              AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufSize,
              ).takeIf { it.state == AudioRecord.STATE_INITIALIZED }
            }.getOrNull()
              ?: AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufSize,
              )

          if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            _engineStatus.value = "Mic hardware busy"
            return@launch
          }

          runCatching {
            if (AcousticEchoCanceler.isAvailable()) {
              aec = AcousticEchoCanceler.create(recorder.audioSessionId)?.apply { enabled = true }
            }
          }
          runCatching {
            if (NoiseSuppressor.isAvailable()) {
              ns = NoiseSuppressor.create(recorder.audioSessionId)?.apply { enabled = true }
            }
          }

          recorder.startRecording()
          _engineStatus.value = "Live Voice Ready"

          val chunk = ShortArray(CHUNK_SAMPLES)
          var speechHangoverUntilMs = 0L
          var lastTxMs = 0L

          while (isActive) {
            var filled = 0
            while (filled < CHUNK_SAMPLES && isActive) {
              val read = recorder.read(chunk, filled, CHUNK_SAMPLES - filled)
              if (read > 0) {
                filled += read
              } else {
                break
              }
            }
            if (filled < CHUNK_SAMPLES / 2) continue

            if (!isMicEnabled) {
              _micLevel.value = 0f
              continue
            }

            var energy = 0.0
            for (i in 0 until filled) {
              val norm = chunk[i] / 32768.0
              energy += norm * norm
            }
            val rms = sqrt(energy / filled).toFloat().coerceIn(0f, 1f)
            _micLevel.value = (rms * 4.5f).coerceIn(0f, 1f)

            val nowMs = System.currentTimeMillis()
            if (rms > 0.008f) {
              speechHangoverUntilMs = nowMs + 1_100L
            }
            val shouldTransmit =
              isCallActive &&
                isMicEnabled &&
                (nowMs <= speechHangoverUntilMs || (nowMs - lastTxMs) >= 1_600L)

            if (shouldTransmit) {
              lastTxMs = nowMs
              val muLaw = G711MuLaw.encodePcm16(chunk, filled)
              val b64 = Base64.encodeToString(muLaw, Base64.NO_WRAP)
              val nextSeq = audioSeq.incrementAndGet()
              runCatching { onPublishAudioChunk(nextSeq, true, b64) }
            }
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.w(CALL_MEDIA_TAG, "Voice capture loop error: ${e.message}")
          _engineStatus.value = "Voice error: ${e.message?.take(40) ?: "Unavailable"}"
        } finally {
          _micLevel.value = 0f
          runCatching { aec?.release() }
          runCatching { ns?.release() }
          runCatching { recorder?.stop() }
          runCatching { recorder?.release() }
        }
      }
  }

  private fun startPlaybackLoop() {
    playbackJob =
      scope.launch {
        var track: AudioTrack? = null
        try {
          val minBuf =
            AudioTrack.getMinBufferSize(
              SAMPLE_RATE,
              AudioFormat.CHANNEL_OUT_MONO,
              AudioFormat.ENCODING_PCM_16BIT,
            ).coerceAtLeast(CHUNK_SAMPLES * 2)

          track =
            AudioTrack.Builder()
              .setAudioAttributes(
                AudioAttributes.Builder()
                  .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                  .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                  .build()
              )
              .setAudioFormat(
                AudioFormat.Builder()
                  .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                  .setSampleRate(SAMPLE_RATE)
                  .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                  .build()
              )
              .setBufferSizeInBytes(maxOf(minBuf * 3, CHUNK_SAMPLES * 6))
              .setTransferMode(AudioTrack.MODE_STREAM)
              .build()

          track.play()

          for (muLaw in audioOutQueue) {
            if (!isActive) break
            val gain = if (isSpeakerBoost) 1.75f else 1.0f
            val pcm = G711MuLaw.decodeToPcm16(muLaw, gain)
            if (pcm.isNotEmpty()) {
              track.write(pcm, 0, pcm.size)
            }
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.w(CALL_MEDIA_TAG, "Voice playback loop error: ${e.message}")
        } finally {
          _voiceRxActive.value = false
          runCatching { track?.stop() }
          runCatching { track?.release() }
        }
      }
  }

  fun stop() {
    captureJob?.cancel()
    captureJob = null
    playbackJob?.cancel()
    playbackJob = null
    while (audioOutQueue.tryReceive().isSuccess) {
      // Drain stale audio packets
    }
    _micLevel.value = 0f
    _voiceRxActive.value = false
  }

  fun release() {
    stop()
    scope.cancel()
  }
}

/**
 * Decodes a base64 JPEG video frame off the main thread safely.
 */
fun decodeCallVideoFrame(base64Jpeg: String): Bitmap? {
  if (base64Jpeg.isBlank()) return null
  return runCatching {
    val clean = base64Jpeg.substringAfter("base64,", base64Jpeg)
    val bytes = Base64.decode(clean, Base64.DEFAULT)
    if (bytes.isEmpty()) return null
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
  }.getOrNull()
}

/**
 * Native Hardware-Accelerated CameraX Preview + Real-Time JPEG Frame Streamer.
 * Uses PreviewView.ImplementationMode.COMPATIBLE (TextureView) so Compose rounded PiP frames
 * and top-layer Dialogs composite cleanly on all Android phones, tablets, and Chromebooks.
 */
@Composable
fun NativeCallCameraPreview(
  modifier: Modifier = Modifier,
  useFrontCamera: Boolean,
  streamingEnabled: Boolean,
  lowBandwidth: Boolean,
  onStatusChange: (String) -> Unit,
  onEncodedFrame: suspend (seq: Long, jpegBase64: String) -> Unit,
) {
  val ctx = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  val currentStreaming by rememberUpdatedState(streamingEnabled)
  val currentLowBw by rememberUpdatedState(lowBandwidth)
  val currentOnFrame by rememberUpdatedState(onEncodedFrame)
  val currentOnStatus by rememberUpdatedState(onStatusChange)

  val frameSeq = remember { AtomicLong(0L) }
  val uploadInFlight = remember { AtomicBoolean(false) }
  val lastFrameAtMs = remember { AtomicLong(0L) }
  val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
  val uploadScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.IO) }

  var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

  DisposableEffect(Unit) {
    onDispose {
      uploadScope.cancel()
      runCatching { analysisExecutor.shutdown() }
    }
  }

  AndroidView(
    modifier = modifier,
    factory = { context ->
      PreviewView(context).apply {
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        scaleType = PreviewView.ScaleType.FILL_CENTER
        setBackgroundColor(android.graphics.Color.parseColor("#0B111A"))
        previewViewRef = this
      }
    },
    update = { pv ->
      if (previewViewRef !== pv) {
        previewViewRef = pv
      }
    },
  )

  DisposableEffect(previewViewRef, useFrontCamera, lifecycleOwner) {
    val pv = previewViewRef
    if (pv == null) {
      return@DisposableEffect onDispose {}
    }

    val hasCamPerm =
      ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    if (!hasCamPerm) {
      currentOnStatus("Camera permission required")
      return@DisposableEffect onDispose {}
    }

    var boundProvider: ProcessCameraProvider? = null
    val providerFuture = ProcessCameraProvider.getInstance(ctx)
    val mainExecutor = ContextCompat.getMainExecutor(ctx)

    providerFuture.addListener(
      {
        runCatching {
          val cameraProvider = providerFuture.get()
          boundProvider = cameraProvider

          val preferredSelector =
            if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
            else CameraSelector.DEFAULT_BACK_CAMERA
          val fallbackSelector =
            if (useFrontCamera) CameraSelector.DEFAULT_BACK_CAMERA
            else CameraSelector.DEFAULT_FRONT_CAMERA

          val resolvedSelector =
            when {
              runCatching { cameraProvider.hasCamera(preferredSelector) }.getOrDefault(false) -> preferredSelector
              runCatching { cameraProvider.hasCamera(fallbackSelector) }.getOrDefault(false) -> fallbackSelector
              else -> CameraSelector.DEFAULT_FRONT_CAMERA
            }
          val actuallyFront = resolvedSelector == CameraSelector.DEFAULT_FRONT_CAMERA

          val preview =
            Preview.Builder().build().also {
              it.surfaceProvider = pv.surfaceProvider
            }

          val resSelector =
            ResolutionSelector.Builder()
              .setResolutionStrategy(
                ResolutionStrategy(
                  Size(320, 240),
                  ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                )
              )
              .build()

          val imageAnalysis =
            ImageAnalysis.Builder()
              .setResolutionSelector(resSelector)
              .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
              .build()

          imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
            try {
              val now = System.currentTimeMillis()
              val intervalMs = if (currentLowBw) 460L else 220L
              if (!currentStreaming || uploadInFlight.get() || (now - lastFrameAtMs.get()) < intervalMs) {
                return@setAnalyzer
              }

              val rawBmp = runCatching { imageProxy.toBitmap() }.getOrNull() ?: return@setAnalyzer
              val rotDeg = imageProxy.imageInfo.rotationDegrees
              val targetMaxDim = if (currentLowBw) 208 else 320
              val jpegQuality = if (currentLowBw) 42 else 56

              val rotatedBmp =
                if (rotDeg != 0 || actuallyFront) {
                  val m = Matrix().apply {
                    if (rotDeg != 0) postRotate(rotDeg.toFloat())
                    if (actuallyFront) postScale(-1f, 1f)
                  }
                  Bitmap.createBitmap(rawBmp, 0, 0, rawBmp.width, rawBmp.height, m, true).also {
                    if (it !== rawBmp) rawBmp.recycle()
                  }
                } else {
                  rawBmp
                }

              val maxSide = maxOf(rotatedBmp.width, rotatedBmp.height).coerceAtLeast(1)
              val scaledBmp =
                if (maxSide > targetMaxDim) {
                  val scale = targetMaxDim.toFloat() / maxSide.toFloat()
                  val w = (rotatedBmp.width * scale).toInt().coerceAtLeast(64)
                  val h = (rotatedBmp.height * scale).toInt().coerceAtLeast(64)
                  Bitmap.createScaledBitmap(rotatedBmp, w, h, true).also {
                    if (it !== rotatedBmp) rotatedBmp.recycle()
                  }
                } else {
                  rotatedBmp
                }

              val baos = ByteArrayOutputStream()
              scaledBmp.compress(Bitmap.CompressFormat.JPEG, jpegQuality, baos)
              scaledBmp.recycle()
              val jpegBytes = baos.toByteArray()
              if (jpegBytes.isEmpty()) return@setAnalyzer

              val b64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
              if (b64.length > 90_000) return@setAnalyzer

              if (uploadInFlight.compareAndSet(false, true)) {
                lastFrameAtMs.set(now)
                val seq = frameSeq.incrementAndGet()
                uploadScope.launch {
                  try {
                    currentOnFrame(seq, b64)
                  } finally {
                    uploadInFlight.set(false)
                  }
                }
              }
            } catch (e: Exception) {
              Log.w(CALL_MEDIA_TAG, "Camera frame encode error: ${e.message}")
            } finally {
              imageProxy.close()
            }
          }

          cameraProvider.unbindAll()
          cameraProvider.bindToLifecycle(
            lifecycleOwner,
            resolvedSelector,
            preview,
            imageAnalysis,
          )
          currentOnStatus(if (actuallyFront) "Front Camera Active" else "Rear Camera Active")
        }.onFailure { err ->
          Log.w(CALL_MEDIA_TAG, "CameraX bind failed: ${err.message}")
          currentOnStatus("Camera unavailable: ${err.message?.take(48) ?: "Hardware busy"}")
        }
      },
      mainExecutor,
    )

    onDispose {
      runCatching { boundProvider?.unbindAll() }
    }
  }
}
