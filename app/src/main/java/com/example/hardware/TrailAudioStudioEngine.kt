package com.example.hardware

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import com.example.ai.AiComposedTrack
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Hardware audio engine supporting:
 * 1) Real 16kHz PCM -> WAV microphone recording with live RMS level telemetry (for Gemini Audio Transcription & Voice Live)
 * 2) Real 44.1kHz stereo polyphonic Trail Music Synthesizer (AudioTrack) for AI-composed riding soundtracks
 */
class TrailAudioStudioEngine {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val _isRecording = MutableStateFlow(false)
  val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

  private val _micLevel = MutableStateFlow(0f)
  val micLevel: StateFlow<Float> = _micLevel.asStateFlow()

  private val _recordedDurationSec = MutableStateFlow(0f)
  val recordedDurationSec: StateFlow<Float> = _recordedDurationSec.asStateFlow()

  private val _isPlayingMusic = MutableStateFlow(false)
  val isPlayingMusic: StateFlow<Boolean> = _isPlayingMusic.asStateFlow()

  private val _musicProgress = MutableStateFlow(0f)
  val musicProgress: StateFlow<Float> = _musicProgress.asStateFlow()

  private var recordJob: Job? = null
  private var pcmAccumulator = ByteArrayOutputStream()
  private var musicJob: Job? = null
  private var activeMusicTrack: AudioTrack? = null

  @SuppressLint("MissingPermission")
  fun startMicRecording(onMaxLengthReached: (ByteArray) -> Unit = {}) {
    if (_isRecording.value) return
    val sampleRate = 16_000
    val minBuf = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(4096)
    val recorder =
      try {
        AudioRecord(
          MediaRecorder.AudioSource.VOICE_RECOGNITION,
          sampleRate,
          AudioFormat.CHANNEL_IN_MONO,
          AudioFormat.ENCODING_PCM_16BIT,
          minBuf * 2,
        )
      } catch (_: Exception) {
        return
      }
    if (recorder.state != AudioRecord.STATE_INITIALIZED) {
      runCatching { recorder.release() }
      return
    }

    pcmAccumulator = ByteArrayOutputStream()
    _recordedDurationSec.value = 0f
    _micLevel.value = 0f
    _isRecording.value = true

    recordJob =
      scope.launch {
        val shortBuf = ShortArray(1024)
        val byteBuf = ByteArray(shortBuf.size * 2)
        val maxBytes = sampleRate * 2 * 25 // Up to 25 seconds (~800KB WAV, well under 20MB Gemini inline limit)
        try {
          recorder.startRecording()
          while (isActive && _isRecording.value) {
            val read = recorder.read(shortBuf, 0, shortBuf.size)
            if (read > 0) {
              var sumSq = 0.0
              for (i in 0 until read) {
                val s = shortBuf[i].toInt()
                sumSq += s * s
                byteBuf[i * 2] = (s and 0xFF).toByte()
                byteBuf[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
              }
              val rms = (sqrt(sumSq / read) / 14000.0).toFloat().coerceIn(0f, 1f)
              _micLevel.value = rms
              synchronized(pcmAccumulator) {
                pcmAccumulator.write(byteBuf, 0, read * 2)
                _recordedDurationSec.value = pcmAccumulator.size() / (sampleRate * 2f)
              }
              if (pcmAccumulator.size() >= maxBytes) {
                _isRecording.value = false
                val wav = buildWavBytes(pcmAccumulator.toByteArray(), sampleRate)
                onMaxLengthReached(wav)
                break
              }
            }
          }
        } finally {
          runCatching { recorder.stop() }
          runCatching { recorder.release() }
          _micLevel.value = 0f
        }
      }
  }

  fun stopMicRecordingAndGetWav(): ByteArray {
    _isRecording.value = false
    recordJob?.cancel()
    recordJob = null
    val rawPcm = synchronized(pcmAccumulator) { pcmAccumulator.toByteArray() }
    return if (rawPcm.isEmpty()) ByteArray(0) else buildWavBytes(rawPcm, 16_000)
  }

  private fun buildWavBytes(pcm: ByteArray, sampleRate: Int): ByteArray {
    val channels = 1
    val bitsPerSample = 16
    val byteRate = sampleRate * channels * bitsPerSample / 8
    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
    header.put("RIFF".toByteArray(Charsets.US_ASCII))
    header.putInt(36 + pcm.size)
    header.put("WAVE".toByteArray(Charsets.US_ASCII))
    header.put("fmt ".toByteArray(Charsets.US_ASCII))
    header.putInt(16)
    header.putShort(1) // PCM
    header.putShort(channels.toShort())
    header.putInt(sampleRate)
    header.putInt(byteRate)
    header.putShort((channels * bitsPerSample / 8).toShort())
    header.putShort(bitsPerSample.toShort())
    header.put("data".toByteArray(Charsets.US_ASCII))
    header.putInt(pcm.size)
    return header.array() + pcm
  }

  fun playComposedTrack(track: AiComposedTrack, loopCount: Int = 2, volumePct: Int = 85) {
    stopMusic()
    _isPlayingMusic.value = true
    _musicProgress.value = 0f

    musicJob =
      scope.launch {
        val sr = 44_100
        val stepSec = (60.0 / track.bpm.coerceIn(76, 156)) / 2.0 // Eighth-note steps
        val stepsPerLoop = 16
        val totalSteps = stepsPerLoop * loopCount.coerceIn(1, 4)
        val totalSamples = (totalSteps * stepSec * sr).toInt()
        val pcm = ShortArray(totalSamples * 2) // Stereo
        val gain = (volumePct.coerceIn(15, 100) / 100.0) * 0.65
        val isSynth = track.genre.contains("Synth", true) || track.genre.contains("Cyber", true) || track.genre.contains("Electro", true)
        val isAcoustic = track.genre.contains("Country", true) || track.genre.contains("Acoustic", true) || track.genre.contains("Folk", true)

        fun midiToHz(m: Int): Double = 440.0 * 2.0.pow((m - 69) / 12.0)

        for (step in 0 until totalSteps) {
          val startIdx = (step * stepSec * sr).toInt()
          val stepLen = (stepSec * sr).toInt().coerceAtMost(totalSamples - startIdx)
          val leadMidi = track.leadNotesMidi[step % track.leadNotesMidi.size]
          val bassMidi = track.bassNotesMidi[(step / 2) % track.bassNotesMidi.size]
          val leadHz = midiToHz(leadMidi)
          val bassHz = midiToHz(bassMidi)
          val padHz = midiToHz(bassMidi + 12)
          val fifthHz = midiToHz(bassMidi + 19)
          val isKick = (step % 4 == 0) || (step % 8 == 6)
          val isSnare = (step % 4 == 2)
          val isHat = (step % 2 == 1)

          for (n in 0 until stepLen) {
            val t = n.toDouble() / sr
            val envLead = (1.0 - exp(-t * 95.0)) * exp(-t * (if (isAcoustic) 6.5 else 3.8))
            val envBass = (1.0 - exp(-t * 70.0)) * exp(-t * 3.0)
            val envPad = (1.0 - exp(-t * 14.0)) * exp(-t * 1.4)

            val leadWave =
              if (isSynth) {
                0.55 * sin(2.0 * PI * leadHz * t) +
                  0.28 * sin(2.0 * PI * leadHz * 2.0 * t) +
                  0.15 * sin(2.0 * PI * leadHz * 3.0 * t)
              } else {
                // Warm plucked acoustic guitar / steel resonance
                0.62 * sin(2.0 * PI * leadHz * t) +
                  0.24 * sin(2.0 * PI * leadHz * 2.0 * t) * exp(-t * 9.0) +
                  0.10 * sin(2.0 * PI * leadHz * 3.0 * t) * exp(-t * 14.0)
              }

            val bassWave =
              0.75 * sin(2.0 * PI * bassHz * t) +
                0.25 * sin(2.0 * PI * bassHz * 2.0 * t)

            val padWave =
              0.5 * sin(2.0 * PI * padHz * t) +
                0.35 * sin(2.0 * PI * fifthHz * t)

            var drum = 0.0
            if (isKick && t < 0.14) {
              val kickPitch = 52.0 + 115.0 * exp(-t * 36.0)
              drum += 0.55 * sin(2.0 * PI * kickPitch * t) * exp(-t * 18.0)
            }
            if (isSnare && t < 0.11) {
              val pseudoNoise = sin(12987.0 * t + 73.0 * sin(991.0 * t))
              drum += 0.28 * pseudoNoise * exp(-t * 28.0) + 0.16 * sin(2.0 * PI * 185.0 * t) * exp(-t * 24.0)
            }
            if (isHat && t < 0.045) {
              val hi = sin(31211.0 * t + 113.0 * sin(4421.0 * t))
              drum += 0.10 * hi * exp(-t * 68.0)
            }

            val sampleL = (leadWave * envLead * 0.36 + bassWave * envBass * 0.32 + padWave * envPad * 0.18 + drum) * gain
            val sampleR = (leadWave * envLead * 0.32 + bassWave * envBass * 0.32 + padWave * envPad * 0.22 + drum) * gain
            val idx = (startIdx + n) * 2
            if (idx + 1 < pcm.size) {
              pcm[idx] = (sampleL.coerceIn(-0.98, 0.98) * Short.MAX_VALUE).toInt().toShort()
              pcm[idx + 1] = (sampleR.coerceIn(-0.98, 0.98) * Short.MAX_VALUE).toInt().toShort()
            }
          }
        }

        val minBuf = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(8192)
        val audioTrack =
          AudioTrack.Builder()
            .setAudioAttributes(
              AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            )
            .setAudioFormat(
              AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sr)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build()
            )
            .setBufferSizeInBytes(minBuf * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        activeMusicTrack = audioTrack
        try {
          audioTrack.play()
          var offset = 0
          val chunk = 4096
          while (isActive && _isPlayingMusic.value && offset < pcm.size) {
            val len = minOf(chunk, pcm.size - offset)
            val written = audioTrack.write(pcm, offset, len)
            if (written <= 0) break
            offset += written
            _musicProgress.value = (offset.toFloat() / pcm.size.toFloat()).coerceIn(0f, 1f)
          }
        } finally {
          runCatching { audioTrack.stop() }
          runCatching { audioTrack.release() }
          if (activeMusicTrack === audioTrack) activeMusicTrack = null
          _isPlayingMusic.value = false
          _musicProgress.value = 0f
        }
      }
  }

  fun stopMusic() {
    _isPlayingMusic.value = false
    musicJob?.cancel()
    musicJob = null
    runCatching { activeMusicTrack?.stop() }
    runCatching { activeMusicTrack?.release() }
    activeMusicTrack = null
    _musicProgress.value = 0f
  }

  fun release() {
    stopMicRecordingAndGetWav()
    stopMusic()
  }
}
