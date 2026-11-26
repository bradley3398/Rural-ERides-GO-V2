package com.example.hardware

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.PlaybackParams
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Base64
import android.util.Log
import android.util.LruCache
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.ai.DirectGeminiRestClient
import com.example.ai.GeminiChatTurnPayload
import com.example.data.AiVoiceProfile
import com.example.data.AlertTonePreset
import com.example.data.CustomVoiceProfile
import com.example.data.RingtonePreset
import com.example.data.VibrationPatternPreset
import com.example.data.VoiceDspProfile
import com.example.data.getActiveCustomVoice
import com.example.data.resolveEffectiveBase
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tanh
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "VoiceAudioManager"
private const val GEMINI_TTS_MODEL = "gemini-3.1-flash-tts-preview"
private const val GEMINI_TTS_SAMPLE_RATE = 24000

/**
 * Studio-grade 44.1 kHz Polyphonic Audio, Touch Sound, Ringtone, Notification, Haptic,
 * Walkie-Talkie PTT, and Neural TTS Engine for Rural ERides GO.
 *
 * Replaces raw beeps and ToneGenerator alerts with harmonic polyphonic instruments
 * (Acoustic Pluck, Wooden Marimba, Warm Rhodes Bell, Studio Glass, Soft Tactile Click)
 * with realistic ADSR envelopes, overtone damping, and subtle acoustic delay tail.
 */
class VoiceAudioManager(private val context: Context) {

  private var tts: TextToSpeech? = null
  private var ttsReady = false
  private var pendingSpeech: String? = null
  private var pendingVolumePct: Int = 85
  private var pendingRate: Float = 1.03f
  private var pendingPitch: Float = 1.0f
  private var pendingVoiceProfile: AiVoiceProfile? = null
  private var pendingCustomVoice: CustomVoiceProfile? = null
  private var pendingRateMult: Float = 1.0f
  private var pendingPitchMult: Float = 1.0f

  private data class SynthesizedFragment(
    val pcm: ByteArray,
    val sampleRate: Int,
    val isNeural: Boolean,
  )

  private var audioRecord: AudioRecord? = null
  private var recordingBuffer = ByteArrayOutputStream()
  private var recordingThread: Thread? = null
  @Volatile private var isRecording = false

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var activeRingtone: Ringtone? = null
  private var ringtoneSynthJob: Job? = null
  private var activeRingtoneTrack: AudioTrack? = null
  @Volatile private var ringtoneLoopActive = false
  @Volatile private var lastTouchSoundMs = 0L

  private var activeSpeechJob: Job? = null
  @Volatile private var activeSpeechTrack: AudioTrack? = null
  private val neuralAudioCache = LruCache<String, ParsedWavPcm>(28)
  private val pcmFragmentCache = LruCache<String, SynthesizedFragment>(48)
  @Volatile var activeStreamingSession: StreamingTtsBufferSession? = null

  private val voiceSampleRate = 16000
  private val studioSampleRate = 44100

  enum class InstrumentVoice {
    ACOUSTIC_PLUCK,
    WARM_MARIMBA,
    STUDIO_BELL,
    RHODES_CHORD,
    TACTILE_WOOD,
    BRASS_HORN,
    SOFT_SYNTH,
  }

  enum class TouchCue {
    TAB_SWITCH,
    BUTTON_TAP,
    TOGGLE_ON,
    TOGGLE_OFF,
    ACTION_CONFIRM,
    SUCCESS_CHIME,
    MESSAGE_SENT,
    PHOTO_SHUTTER,
    RIDE_START,
    RIDE_STOP,
    BACK_NAV,
    ERROR_THUD,
  }

  private data class PolyNote(
    val startMs: Int,
    val durMs: Int,
    val freqsHz: List<Double>,
    val voice: InstrumentVoice,
    val gain: Double = 0.85,
  )

  init {
    try {
      tts =
        TextToSpeech(context.applicationContext) { status ->
          if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setSpeechRate(pendingRate)
            tts?.setPitch(pendingPitch)
            ttsReady = true
            pendingSpeech?.let { msg ->
              pendingSpeech = null
              val vp = pendingVoiceProfile
              val cv = pendingCustomVoice
              val rMult = pendingRateMult
              val pMult = pendingPitchMult
              if (cv != null) {
                speakWithCustomVoice(msg, cv, pendingVolumePct, speechRateMult = rMult, pitchMult = pMult)
              } else if (vp != null) {
                speakWithVoiceProfile(msg, vp, pendingVolumePct, speechRateMult = rMult, pitchMult = pMult)
              } else {
                speak(msg, pendingVolumePct, pendingRate, pendingPitch)
              }
            }
          }
        }
    } catch (e: Exception) {
      Log.w(TAG, "TTS init skipped: ${e.message}")
    }
  }

  // ============================================================================
  // SPEECH & DISTINCT AI VOICE ENGINE (GEMINI 3.1 FLASH TTS + LOCAL DSP FALLBACK)
  // ============================================================================

  fun speak(
    text: String,
    volumePct: Int = 85,
    speechRate: Float = 1.03f,
    pitch: Float = 1.0f,
    interrupt: Boolean = true,
  ): Boolean {
    if (volumePct <= 0 || text.isBlank()) return false
    val clean = cleanSpeechText(text)
    if (clean.isBlank()) return false
    pendingVolumePct = volumePct
    pendingRate = speechRate.coerceIn(0.6f, 1.8f)
    pendingPitch = pitch.coerceIn(0.6f, 1.6f)
    pendingVoiceProfile = null
    pendingCustomVoice = null
    pendingRateMult = 1.0f
    pendingPitchMult = 1.0f
    if (interrupt) {
      val sess = activeStreamingSession
      activeStreamingSession = null
      sess?.cancel()
      cancelActiveSpeechPlayback()
    }
    if (!ttsReady || tts == null) {
      pendingSpeech = clean
      return true
    }
    try {
      tts?.setSpeechRate(pendingRate)
      tts?.setPitch(pendingPitch)
      val params = Bundle().apply {
        putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, (volumePct / 100f).coerceIn(0.1f, 1.0f))
      }
      val mode = if (interrupt) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
      tts?.speak(clean, mode, params, "re_${System.currentTimeMillis()}")
      return true
    } catch (e: Exception) {
      Log.w(TAG, "TTS speak failed: ${e.message}")
      return false
    }
  }

  // ============================================================================
  // BACKGROUND PRE-LOADING BUFFER FOR TTS FRAGMENTS & STREAMING PLAYBACK
  // ============================================================================

  class StreamingSentenceSplitter(private val onSentenceReady: (String) -> Unit) {
    private val buffer = StringBuilder()

    fun feed(chunk: String) {
      buffer.append(chunk)
      processBuffer(isFinal = false)
    }

    fun flush() {
      processBuffer(isFinal = true)
    }

    private fun processBuffer(isFinal: Boolean) {
      while (true) {
        val text = buffer.toString()
        val idx = findSentenceEnd(text)
        if (idx != -1) {
          val sentence = text.substring(0, idx + 1).trim()
          buffer.delete(0, idx + 1)
          if (sentence.isNotBlank()) {
            onSentenceReady(sentence)
          }
        } else {
          if (isFinal && text.isNotBlank()) {
            onSentenceReady(text.trim())
            buffer.clear()
          }
          break
        }
      }
    }

    private fun findSentenceEnd(text: String): Int {
      for (i in 0 until text.length) {
        val c = text[i]
        if (c == '.' || c == '!' || c == '?' || c == '\n') {
          if (c == '.' && i > 0 && i < text.length - 1 && text[i - 1].isDigit() && text[i + 1].isDigit()) {
            continue
          }
          if (i == text.length - 1 || text[i + 1].isWhitespace()) {
            return i
          }
        }
      }
      if (text.length > 120) {
        val commaIdx = text.indexOf(',', 60)
        if (commaIdx != -1) return commaIdx
      }
      return -1
    }
  }

  class StreamingTtsBufferSession internal constructor(
    private val voiceAudio: VoiceAudioManager,
    val profile: AiVoiceProfile,
    val customVoice: CustomVoiceProfile? = null,
    val volumePct: Int = 85,
    val speechRateMult: Float = 1.0f,
    val pitchMult: Float = 1.0f,
    val preferFastLocal: Boolean = false,
    val bufferDepth: Int = 2,
  ) {
    private val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val textChannel = Channel<String>(Channel.UNLIMITED)
    private val pcmChannel = Channel<SynthesizedFragment>(capacity = bufferDepth.coerceIn(1, 5))
    @Volatile private var isFinished = false
    @Volatile private var isCancelled = false

    init {
      // 1. Background Pre-loader Worker: pre-synthesizes up to bufferDepth sentence fragments ahead of playback
      sessionScope.launch {
        try {
          for (frag in textChannel) {
            if (isCancelled || !isActive) break
            val result = voiceAudio.synthesizeFragmentPcm(
              fragment = frag,
              profile = profile,
              customVoice = customVoice,
              speechRateMult = speechRateMult,
              pitchMult = pitchMult,
              preferFastLocal = preferFastLocal,
            )
            if (result != null && !isCancelled && isActive) {
              pcmChannel.send(result)
            } else if (!isCancelled && isActive) {
              voiceAudio.speakDirectTtsFallback(
                clean = frag,
                profile = profile,
                volumePct = volumePct,
                speechRateMult = speechRateMult,
                pitchMult = pitchMult,
                interrupt = false,
                customVoice = customVoice,
              )
            }
          }
        } catch (_: CancellationException) {
        } finally {
          pcmChannel.close()
        }
      }

      // 2. Continuous Playback Worker: plays PCM fragments sequentially with zero gap
      sessionScope.launch {
        try {
          val dsp = customVoice?.dspProfile ?: profile.dspProfile
          val (neuralSpeed, neuralPitch) = voiceAudio.computeNeuralPlaybackParams(profile, customVoice, speechRateMult, pitchMult)
          if (dsp == VoiceDspProfile.CB_RADIO) {
            voiceAudio.playSquelchChirp(isEnd = false, volumePct = volumePct)
          }
          for (frag in pcmChannel) {
            if (isCancelled || !isActive) break
            voiceAudio.playPcm16MonoCancellable(
              pcm = frag.pcm,
              sampleRate = frag.sampleRate,
              volumePct = volumePct,
              speedMult = if (frag.isNeural) neuralSpeed else 1.0f,
              pitchMult = if (frag.isNeural) neuralPitch else 1.0f,
            )
          }
          if (!isCancelled && isActive && dsp == VoiceDspProfile.CB_RADIO) {
            voiceAudio.playSquelchChirp(isEnd = true, volumePct = volumePct)
          }
        } catch (_: CancellationException) {
        }
      }
    }

    fun enqueueTextFragment(fragment: String) {
      if (isFinished || isCancelled) return
      val clean = fragment.trim()
      if (clean.isNotBlank()) {
        textChannel.trySend(clean)
      }
    }

    fun finish() {
      if (isFinished || isCancelled) return
      isFinished = true
      textChannel.close()
    }

    fun cancel() {
      isCancelled = true
      textChannel.close()
      pcmChannel.close()
      sessionScope.cancel()
      voiceAudio.cancelActiveSpeechPlayback()
    }
  }

  private fun computeNeuralPlaybackParams(
    profile: AiVoiceProfile,
    customVoice: CustomVoiceProfile?,
    speechRateMult: Float,
    pitchMult: Float,
  ): Pair<Float, Float> {
    val speed =
      if (customVoice != null) {
        val base = customVoice.resolveEffectiveBase()
        ((customVoice.speechRate / base.speechRate.coerceAtLeast(0.5f)) * speechRateMult).coerceIn(0.70f, 1.55f)
      } else {
        speechRateMult.coerceIn(0.70f, 1.55f)
      }
    val pitch =
      if (customVoice != null) {
        val base = customVoice.resolveEffectiveBase()
        ((customVoice.pitch / base.pitch.coerceAtLeast(0.5f)) * pitchMult).coerceIn(0.75f, 1.35f)
      } else {
        pitchMult.coerceIn(0.75f, 1.35f)
      }
    return speed to pitch
  }

  private suspend fun synthesizeFragmentPcm(
    fragment: String,
    profile: AiVoiceProfile,
    customVoice: CustomVoiceProfile? = null,
    speechRateMult: Float = 1.0f,
    pitchMult: Float = 1.0f,
    preferFastLocal: Boolean = false,
  ): SynthesizedFragment? {
    val clean = cleanSpeechText(fragment)
    if (clean.isBlank()) return null

    val effectiveProfile = customVoice?.resolveEffectiveBase() ?: profile
    val dsp = customVoice?.dspProfile ?: effectiveProfile.dspProfile
    val effectiveRate = ((customVoice?.speechRate ?: effectiveProfile.speechRate) * speechRateMult).coerceIn(0.68f, 1.55f)
    val effectivePitch = ((customVoice?.pitch ?: effectiveProfile.pitch) * pitchMult).coerceIn(0.65f, 1.45f)
    val voiceKey = customVoice?.let { "${it.id}:${it.geminiVoiceName}" } ?: effectiveProfile.id
    val cacheKey = "$voiceKey|${dsp.id}|${(effectiveRate * 100).toInt()}|${(effectivePitch * 100).toInt()}|$preferFastLocal|${clean.lowercase()}"
    pcmFragmentCache.get(cacheKey)?.let { return it }

    // 1. Try Studio Neural Gemini 3.1 Flash TTS first unless fast-local nav alert requested
    if (!preferFastLocal) {
      val neural = fetchOrCacheNeuralSpeechPcm(clean, effectiveProfile, customVoice)
      if (neural != null && neural.pcmBytes.isNotEmpty()) {
        val shaped =
          if (dsp == VoiceDspProfile.CB_RADIO) applyCbRadioToPcm16(neural.pcmBytes, neural.sampleRate)
          else neural.pcmBytes
        val out = SynthesizedFragment(shaped, neural.sampleRate, isNeural = true)
        pcmFragmentCache.put(cacheKey, out)
        return out
      }
    }

    // 2. Local DSP Synthesis fallback (fast & reliable offline)
    val engine = tts ?: return null
    if (!ttsReady) return null
    val tempWav = File(context.cacheDir, "frag_${System.nanoTime()}.wav")
    return try {
      val uttId = "frag_${System.nanoTime()}"
      val done = CompletableDeferred<Boolean>()
      engine.setOnUtteranceProgressListener(
        object : UtteranceProgressListener() {
          override fun onStart(utteranceId: String?) {}
          override fun onDone(utteranceId: String?) {
            if (utteranceId == uttId) done.complete(true)
          }
          @Deprecated("Deprecated in Java")
          override fun onError(utteranceId: String?) {
            if (utteranceId == uttId) done.complete(false)
          }
          override fun onError(utteranceId: String?, errorCode: Int) {
            if (utteranceId == uttId) done.complete(false)
          }
        }
      )
      applyBestEngineVoice(effectiveProfile, customVoice)
      engine.setSpeechRate(effectiveRate)
      engine.setPitch(effectivePitch)
      val res = engine.synthesizeToFile(clean, Bundle(), tempWav, uttId)
      if (res != TextToSpeech.SUCCESS) return null
      val ok = withTimeoutOrNull(4_500L) { done.await() } ?: false
      if (!ok || !tempWav.exists() || tempWav.length() <= 64L) return null

      val wav = parseWavPcm16(tempWav.readBytes()) ?: return null
      val shaped = applyLocalDspSignature(wav.pcmBytes, wav.sampleRate, dsp)
      val out = SynthesizedFragment(shaped, wav.sampleRate, isNeural = false)
      pcmFragmentCache.put(cacheKey, out)
      out
    } catch (ce: CancellationException) {
      throw ce
    } catch (e: Exception) {
      Log.w(TAG, "Fragment synthesis error: ${e.message}")
      null
    } finally {
      runCatching { tempWav.delete() }
    }
  }

  fun splitIntoSentenceFragments(text: String): List<String> {
    val clean = cleanSpeechText(text)
    if (clean.isBlank()) return emptyList()
    val list = mutableListOf<String>()
    val splitter = StreamingSentenceSplitter { list.add(it) }
    splitter.feed(clean)
    splitter.flush()
    return if (list.isEmpty()) listOf(clean) else list
  }

  fun startStreamingSpeechSession(
    profile: AiVoiceProfile,
    customVoice: CustomVoiceProfile? = null,
    volumePct: Int = 85,
    speechRateMult: Float = 1.0f,
    pitchMult: Float = 1.0f,
    preferFastLocal: Boolean = false,
    bufferDepth: Int = 2,
  ): StreamingTtsBufferSession {
    stopSpeaking()
    val session =
      StreamingTtsBufferSession(
        voiceAudio = this,
        profile = profile,
        customVoice = customVoice,
        volumePct = volumePct,
        speechRateMult = speechRateMult,
        pitchMult = pitchMult,
        preferFastLocal = preferFastLocal,
        bufferDepth = bufferDepth,
      )
    activeStreamingSession = session
    return session
  }

  fun speakWithCustomVoice(
    text: String,
    customVoice: CustomVoiceProfile,
    volumePct: Int = 85,
    interrupt: Boolean = true,
    speechRateMult: Float = 1.0f,
    pitchMult: Float = 1.0f,
    preferFastLocal: Boolean = false,
    preloadBufferEnabled: Boolean = true,
  ): Boolean {
    return speakWithVoiceProfile(
      text = text,
      profile = customVoice.resolveEffectiveBase(),
      volumePct = volumePct,
      interrupt = interrupt,
      speechRateMult = speechRateMult,
      pitchMult = pitchMult,
      preferFastLocal = preferFastLocal,
      preloadBufferEnabled = preloadBufferEnabled,
      customVoice = customVoice,
    )
  }

  fun previewCustomVoice(customVoice: CustomVoiceProfile, volumePct: Int = 85): Boolean {
    val sample = "Howdy from the custom voice studio! Pitch and speech rate configured. Trails are clear and telemetry is active."
    return speakWithCustomVoice(sample, customVoice, volumePct, preferFastLocal = false)
  }

  fun clearPcmCache() {
    pcmFragmentCache.evictAll()
    neuralAudioCache.evictAll()
  }

  fun speakWithActiveSettings(
    text: String,
    settings: com.example.data.MasterSettings,
    volumePct: Int = settings.globalVolume,
    interrupt: Boolean = true,
    preferFastLocal: Boolean = false,
  ): Boolean {
    val customVoice = settings.getActiveCustomVoice()
    return if (customVoice != null) {
      speakWithCustomVoice(
        text = text,
        customVoice = customVoice,
        volumePct = volumePct,
        interrupt = interrupt,
        speechRateMult = settings.ttsSpeechRate,
        pitchMult = settings.ttsPitch,
        preferFastLocal = preferFastLocal,
        preloadBufferEnabled = settings.ttsPreloadBufferEnabled,
      )
    } else {
      speakWithVoiceProfile(
        text = text,
        profile = settings.aiVoiceProfile,
        volumePct = volumePct,
        interrupt = interrupt,
        speechRateMult = settings.ttsSpeechRate,
        pitchMult = settings.ttsPitch,
        preferFastLocal = preferFastLocal,
        preloadBufferEnabled = settings.ttsPreloadBufferEnabled,
      )
    }
  }

  fun speakWithVoiceProfile(
    text: String,
    profile: AiVoiceProfile,
    volumePct: Int = 85,
    interrupt: Boolean = true,
    speechRateMult: Float = 1.0f,
    pitchMult: Float = 1.0f,
    preferFastLocal: Boolean = false,
    preloadBufferEnabled: Boolean = true,
    customVoice: CustomVoiceProfile? = null,
  ): Boolean {
    if (volumePct <= 0 || text.isBlank()) return false
    val clean = cleanSpeechText(text)
    if (clean.isBlank()) return false
    pendingVolumePct = volumePct
    val baseRate = customVoice?.speechRate ?: profile.speechRate
    val basePitch = customVoice?.pitch ?: profile.pitch
    pendingRate = (baseRate * speechRateMult).coerceIn(0.65f, 1.65f)
    pendingPitch = (basePitch * pitchMult).coerceIn(0.65f, 1.5f)
    pendingRateMult = speechRateMult
    pendingPitchMult = pitchMult
    pendingVoiceProfile = profile
    pendingCustomVoice = customVoice

    if (interrupt) {
      val sess = activeStreamingSession
      activeStreamingSession = null
      sess?.cancel()
      cancelActiveSpeechPlayback()
    }

    val fragments = splitIntoSentenceFragments(clean)
    if (preloadBufferEnabled && fragments.size > 1) {
      val session =
        startStreamingSpeechSession(
          profile = profile,
          customVoice = customVoice,
          volumePct = volumePct,
          speechRateMult = speechRateMult,
          pitchMult = pitchMult,
          preferFastLocal = preferFastLocal,
          bufferDepth = 2,
        )
      for (f in fragments) {
        session.enqueueTextFragment(f)
      }
      session.finish()
      return true
    }

    val prevJob = if (!interrupt) activeSpeechJob else null
    activeSpeechJob =
      scope.launch {
        try {
          if (prevJob != null && prevJob.isActive) {
            prevJob.join()
          }
          if (!isActive) return@launch

          val dsp = customVoice?.dspProfile ?: profile.dspProfile

          // 1. Try Studio Neural Gemini 3.1 Flash TTS first
          if (!preferFastLocal) {
            val neural = fetchOrCacheNeuralSpeechPcm(clean, profile, customVoice)
            if (neural != null && neural.pcmBytes.isNotEmpty() && isActive) {
              val processed =
                if (dsp == VoiceDspProfile.CB_RADIO) {
                  applyCbRadioToPcm16(neural.pcmBytes, neural.sampleRate)
                } else {
                  neural.pcmBytes
                }
              val (neuralSpeed, neuralPitch) = computeNeuralPlaybackParams(profile, customVoice, speechRateMult, pitchMult)
              if (dsp == VoiceDspProfile.CB_RADIO) {
                playSquelchChirp(isEnd = false, volumePct = volumePct)
              }
              playPcm16MonoCancellable(
                pcm = processed,
                sampleRate = neural.sampleRate,
                volumePct = volumePct,
                speedMult = neuralSpeed,
                pitchMult = neuralPitch,
              )
              if (isActive && dsp == VoiceDspProfile.CB_RADIO) {
                playSquelchChirp(isEnd = true, volumePct = volumePct)
              }
              return@launch
            }
          }

          if (!isActive) return@launch

          // 2. Offline / Fast-turn fallback: Local Neural Voice Slot + Real-Time PCM DSP Shaper
          val playedDsp = speakLocalWithDspCharacter(clean, profile, volumePct, speechRateMult, pitchMult, customVoice)
          if (!playedDsp && isActive) {
            speakDirectTtsFallback(clean, profile, volumePct, speechRateMult, pitchMult, interrupt = true, customVoice = customVoice)
          }
        } catch (ce: CancellationException) {
          throw ce
        } catch (e: Exception) {
          Log.w(TAG, "Voice profile playback error: ${e.message}")
          speakDirectTtsFallback(clean, profile, volumePct, speechRateMult, pitchMult, interrupt = true, customVoice = customVoice)
        }
      }
    return true
  }

  fun speakText(
    text: String,
    volumePct: Int = 85,
    speechRate: Float = 1.03f,
    pitch: Float = 1.0f,
  ): Boolean = speak(text, volumePct, speechRate, pitch)

  fun previewVoiceProfile(
    profile: AiVoiceProfile,
    volumePct: Int = 90,
    speechRateMult: Float = 1.0f,
    pitchMult: Float = 1.0f,
  ): Boolean =
    speakWithVoiceProfile(
      text = profile.sampleLine,
      profile = profile,
      volumePct = volumePct.coerceAtLeast(45),
      speechRateMult = speechRateMult,
      pitchMult = pitchMult,
      preferFastLocal = false,
    )

  private suspend fun fetchOrCacheNeuralSpeechPcm(
    cleanText: String,
    profile: AiVoiceProfile,
    customVoice: CustomVoiceProfile? = null,
  ): ParsedWavPcm? {
    val spokenExcerpt = trimToSentenceBoundary(cleanText, maxChars = 1400)
    if (spokenExcerpt.isBlank()) return null
    val voiceName = customVoice?.geminiVoiceName?.ifBlank { profile.geminiVoiceName } ?: profile.geminiVoiceName
    val localeTag = customVoice?.localeTag?.ifBlank { profile.localeTag } ?: profile.localeTag
    val directive = customVoice?.styleDirective?.ifBlank { profile.styleDirective } ?: profile.styleDirective
    val cacheKey = "${customVoice?.id ?: profile.id}|$voiceName|$localeTag|$directive|$spokenExcerpt"
    neuralAudioCache.get(cacheKey)?.let { cached ->
      return cached
    }
    return try {
      val directedPrompt = "$directive. Speak naturally and read this text aloud:\n$spokenExcerpt"
      val response =
        withTimeout(18_000L) {
          DirectGeminiRestClient.generateContent(
            modelId = GEMINI_TTS_MODEL,
            turns = listOf(GeminiChatTurnPayload(role = "user", text = directedPrompt)),
            responseModalities = listOf("AUDIO"),
            ttsVoiceName = voiceName,
            ttsLanguageCode = localeTag,
          )
        }
      val bytes = response.inlineAudioBytes
      val audioMime = response.inlineAudioMime
      if (bytes != null && bytes.size > 1024) {
        val parsedWav =
          if (bytes.size > 44 && bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte()
          ) {
            parseWavPcm16(bytes)
          } else {
            null
          }
        val mimeRate =
          Regex("rate=(\\d{4,5})").find(audioMime)?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(8000, 48000)
            ?: GEMINI_TTS_SAMPLE_RATE
        val result = parsedWav ?: ParsedWavPcm(sampleRate = mimeRate, pcmBytes = bytes)
        neuralAudioCache.put(cacheKey, result)
        result
      } else {
        null
      }
    } catch (ce: CancellationException) {
      throw ce
    } catch (e: TimeoutCancellationException) {
      Log.w(TAG, "Gemini neural TTS timed out (${profile.id}), using local DSP voice")
      null
    } catch (e: Exception) {
      Log.w(TAG, "Gemini neural TTS unavailable (${profile.id}): ${e.message}")
      null
    }
  }

  private suspend fun speakLocalWithDspCharacter(
    clean: String,
    profile: AiVoiceProfile,
    volumePct: Int,
    speechRateMult: Float,
    pitchMult: Float,
    customVoice: CustomVoiceProfile? = null,
  ): Boolean {
    val engine = tts ?: return false
    if (!ttsReady) return false
    val dsp = customVoice?.dspProfile ?: profile.dspProfile
    val baseRate = customVoice?.speechRate ?: profile.speechRate
    val basePitch = customVoice?.pitch ?: profile.pitch
    val tempWav = File(context.cacheDir, "re_tts_${System.nanoTime()}.wav")
    return try {
      val uttId = "re_synth_${System.nanoTime()}"
      val done = CompletableDeferred<Boolean>()
      engine.setOnUtteranceProgressListener(
        object : UtteranceProgressListener() {
          override fun onStart(utteranceId: String?) {}
          override fun onDone(utteranceId: String?) {
            if (utteranceId == uttId) done.complete(true)
          }
          @Deprecated("Deprecated in Java")
          override fun onError(utteranceId: String?) {
            if (utteranceId == uttId) done.complete(false)
          }
          override fun onError(utteranceId: String?, errorCode: Int) {
            if (utteranceId == uttId) done.complete(false)
          }
        }
      )
      applyBestEngineVoice(profile, customVoice)
      engine.setSpeechRate((baseRate * speechRateMult).coerceIn(0.68f, 1.55f))
      engine.setPitch((basePitch * pitchMult).coerceIn(0.65f, 1.45f))
      val res = engine.synthesizeToFile(clean, Bundle(), tempWav, uttId)
      if (res != TextToSpeech.SUCCESS) return false
      val ok = withTimeoutOrNull(9_000L) { done.await() } ?: false
      if (!ok || !tempWav.exists() || tempWav.length() <= 64L) return false

      val wav = parseWavPcm16(tempWav.readBytes()) ?: return false
      val shaped = applyLocalDspSignature(wav.pcmBytes, wav.sampleRate, dsp)
      if (dsp == VoiceDspProfile.CB_RADIO) {
        playSquelchChirp(isEnd = false, volumePct = volumePct)
      }
      playPcm16MonoCancellable(
        pcm = shaped,
        sampleRate = wav.sampleRate,
        volumePct = volumePct,
        speedMult = 1.0f,
        pitchMult = 1.0f,
      )
      if (dsp == VoiceDspProfile.CB_RADIO) {
        playSquelchChirp(isEnd = true, volumePct = volumePct)
      }
      true
    } catch (ce: CancellationException) {
      throw ce
    } catch (e: Exception) {
      Log.w(TAG, "Local DSP synthesis fallback failed: ${e.message}")
      false
    } finally {
      runCatching { tempWav.delete() }
    }
  }

  private fun speakDirectTtsFallback(
    clean: String,
    profile: AiVoiceProfile,
    volumePct: Int,
    speechRateMult: Float,
    pitchMult: Float,
    interrupt: Boolean,
    customVoice: CustomVoiceProfile? = null,
  ): Boolean {
    if (!ttsReady || tts == null) {
      pendingSpeech = clean
      return true
    }
    return try {
      val baseRate = customVoice?.speechRate ?: profile.speechRate
      val basePitch = customVoice?.pitch ?: profile.pitch
      applyBestEngineVoice(profile, customVoice)
      tts?.setSpeechRate((baseRate * speechRateMult).coerceIn(0.68f, 1.55f))
      tts?.setPitch((basePitch * pitchMult).coerceIn(0.65f, 1.45f))
      val params = Bundle().apply {
        putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, (volumePct / 100f).coerceIn(0.15f, 1.0f))
      }
      val mode = if (interrupt) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
      tts?.speak(clean, mode, params, "re_vp_${System.currentTimeMillis()}")
      true
    } catch (e: Exception) {
      Log.w(TAG, "Direct TTS fallback failed: ${e.message}")
      false
    }
  }

  private suspend fun playPcm16MonoCancellable(
    pcm: ByteArray,
    sampleRate: Int,
    volumePct: Int,
    speedMult: Float,
    pitchMult: Float,
  ) {
    if (pcm.isEmpty()) return
    val minBuf =
      AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
      ).coerceAtLeast(4096)
    val track =
      AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        )
        .setBufferSizeInBytes(minBuf * 2)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()

    activeSpeechTrack = track
    try {
      val gain = (volumePct / 100f).coerceIn(0.1f, 1.0f)
      track.setVolume(gain)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
        (kotlin.math.abs(speedMult - 1.0f) > 0.03f || kotlin.math.abs(pitchMult - 1.0f) > 0.03f)
      ) {
        runCatching {
          track.playbackParams = PlaybackParams().apply {
            speed = speedMult.coerceIn(0.65f, 1.65f)
            pitch = pitchMult.coerceIn(0.70f, 1.45f)
          }
        }
      }
      track.play()
      var offset = 0
      val chunk = 4096
      while (offset < pcm.size && kotlinx.coroutines.currentCoroutineContext().isActive) {
        val len = min(chunk, pcm.size - offset)
        val written = track.write(pcm, offset, len)
        if (written <= 0) break
        offset += written
      }
      if (kotlinx.coroutines.currentCoroutineContext().isActive) {
        val tailMs = ((minBuf.toDouble() / (sampleRate * 2.0)) * 1000.0).toLong().coerceIn(80L, 260L)
        delay(tailMs)
      }
    } finally {
      activeSpeechTrack = null
      runCatching { track.pause() }
      runCatching { track.flush() }
      runCatching { track.stop() }
      runCatching { track.release() }
    }
  }

  private data class ParsedWavPcm(val sampleRate: Int, val pcmBytes: ByteArray)

  private fun parseWavPcm16(wavBytes: ByteArray): ParsedWavPcm? {
    if (wavBytes.size < 44) return null
    val buf = ByteBuffer.wrap(wavBytes).order(ByteOrder.LITTLE_ENDIAN)
    val channels = buf.getShort(22).toInt().coerceAtLeast(1)
    val sampleRate = buf.getInt(24).coerceIn(8000, 48000)
    val bitsPerSample = buf.getShort(34).toInt()
    if (bitsPerSample != 16) return null

    var pos = 12
    while (pos + 8 <= wavBytes.size) {
      val id = String(wavBytes, pos, 4, Charsets.US_ASCII)
      val chunkSize = buf.getInt(pos + 4).coerceAtLeast(0)
      val dataStart = pos + 8
      if (id == "data") {
        val dataLen = min(chunkSize, wavBytes.size - dataStart)
        if (dataLen <= 2) return null
        if (channels == 1) {
          return ParsedWavPcm(sampleRate, wavBytes.copyOfRange(dataStart, dataStart + dataLen))
        }
        // Downmix multi-channel to mono 16-bit
        val frames = dataLen / (2 * channels)
        val mono = ByteArray(frames * 2)
        val inBuf = ByteBuffer.wrap(wavBytes, dataStart, dataLen).order(ByteOrder.LITTLE_ENDIAN)
        val outBuf = ByteBuffer.wrap(mono).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frames) {
          var sum = 0
          for (c in 0 until channels) sum += inBuf.short.toInt()
          outBuf.putShort((sum / channels).coerceIn(-32768, 32767).toShort())
        }
        return ParsedWavPcm(sampleRate, mono)
      }
      pos += 8 + chunkSize + (chunkSize and 1)
    }
    return null
  }

  private fun applyLocalDspSignature(
    pcmBytes: ByteArray,
    sampleRate: Int,
    signature: VoiceDspProfile,
  ): ByteArray {
    if (signature == VoiceDspProfile.CB_RADIO) {
      return applyCbRadioToPcm16(pcmBytes, sampleRate)
    }
    val sampleCount = pcmBytes.size / 2
    if (sampleCount < 16) return pcmBytes
    val inBuf = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
    val outBytes = ByteArray(pcmBytes.size)
    val outBuf = ByteBuffer.wrap(outBytes).order(ByteOrder.LITTLE_ENDIAN)

    val lpCutHz =
      when (signature) {
        VoiceDspProfile.DEEP_BARN -> 3200.0
        VoiceDspProfile.WARM_PORCH -> 4800.0
        VoiceDspProfile.WARM_SCOUT -> 5200.0
        VoiceDspProfile.BRIGHT_DISPATCH -> 7500.0
        VoiceDspProfile.TACTICAL_CRISP -> 6800.0
        VoiceDspProfile.OUTBACK_AU -> 6000.0
        VoiceDspProfile.HIGHLAND_UK -> 6400.0
        VoiceDspProfile.CB_RADIO -> 2900.0
      }
    val lpAlpha = (2.0 * PI * lpCutHz / sampleRate).let { w -> (w / (w + 1.0)).coerceIn(0.05, 0.95) }
    val bassAlpha = (2.0 * PI * 190.0 / sampleRate).let { w -> (w / (w + 1.0)).coerceIn(0.01, 0.4) }
    val airAlpha = (2.0 * PI * 2400.0 / sampleRate).let { w -> (w / (w + 1.0)).coerceIn(0.1, 0.8) }

    val bassBoost =
      when (signature) {
        VoiceDspProfile.DEEP_BARN -> 0.55
        VoiceDspProfile.WARM_PORCH -> 0.30
        VoiceDspProfile.WARM_SCOUT -> 0.22
        VoiceDspProfile.OUTBACK_AU -> 0.16
        VoiceDspProfile.BRIGHT_DISPATCH -> -0.18
        VoiceDspProfile.TACTICAL_CRISP -> -0.10
        VoiceDspProfile.HIGHLAND_UK -> 0.05
        VoiceDspProfile.CB_RADIO -> -0.60
      }
    val airBoost =
      when (signature) {
        VoiceDspProfile.BRIGHT_DISPATCH -> 0.45
        VoiceDspProfile.TACTICAL_CRISP -> 0.36
        VoiceDspProfile.HIGHLAND_UK -> 0.26
        VoiceDspProfile.WARM_SCOUT -> 0.14
        VoiceDspProfile.DEEP_BARN -> -0.12
        VoiceDspProfile.WARM_PORCH -> 0.05
        VoiceDspProfile.OUTBACK_AU -> 0.12
        VoiceDspProfile.CB_RADIO -> 0.20
      }
    val drive =
      when (signature) {
        VoiceDspProfile.DEEP_BARN -> 1.45
        VoiceDspProfile.TACTICAL_CRISP -> 1.32
        VoiceDspProfile.WARM_PORCH -> 1.15
        else -> 1.08
      }
    val echoSamples = if (signature == VoiceDspProfile.OUTBACK_AU || signature == VoiceDspProfile.HIGHLAND_UK) (sampleRate * 0.018).toInt() else 0
    val echoRing = if (echoSamples > 0) DoubleArray(echoSamples) else null

    var lpState = 0.0
    var bassState = 0.0
    var airPrev = 0.0
    for (i in 0 until sampleCount) {
      val x = inBuf.short / 32768.0
      lpState += lpAlpha * (x - lpState)
      bassState += bassAlpha * (lpState - bassState)
      val hpAir = lpState - airPrev
      airPrev += airAlpha * (lpState - airPrev)

      var y = lpState + bassState * bassBoost + hpAir * airBoost
      if (echoRing != null) {
        val idx = i % echoRing.size
        val delayed = echoRing[idx]
        echoRing[idx] = y
        y = y * 0.92 + delayed * 0.12
      }
      val saturated = tanh(y * drive) / tanh(drive)
      val s16 = (saturated * 30500.0).toInt().coerceIn(-32767, 32767).toShort()
      outBuf.putShort(s16)
    }
    return outBytes
  }

  private fun applyCbRadioToPcm16(pcmBytes: ByteArray, sampleRate: Int): ByteArray {
    val sampleCount = pcmBytes.size / 2
    if (sampleCount < 16) return pcmBytes
    val inBuf = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
    val outBytes = ByteArray(pcmBytes.size)
    val outBuf = ByteBuffer.wrap(outBytes).order(ByteOrder.LITTLE_ENDIAN)

    val hpRc = 1.0 / (2.0 * PI * 420.0)
    val dt = 1.0 / sampleRate
    val hpAlpha = hpRc / (hpRc + dt)
    val lpAlpha = (2.0 * PI * 2850.0 * dt).let { w -> (w / (w + 1.0)).coerceIn(0.1, 0.9) }

    var prevIn = 0.0
    var hpOut = 0.0
    var lpOut = 0.0
    var noiseSeed = 1337
    for (i in 0 until sampleCount) {
      val x = inBuf.short / 32768.0
      hpOut = hpAlpha * (hpOut + x - prevIn)
      prevIn = x
      lpOut += lpAlpha * (hpOut - lpOut)
      // Subtle radio noise floor + punchy hard broadcast limiting
      noiseSeed = (noiseSeed * 1103515245 + 12345) and 0x7fffffff
      val hiss = ((noiseSeed / 2147483647.0) - 0.5) * 0.008
      val driven = tanh((lpOut * 2.1) + hiss) * 0.92
      outBuf.putShort((driven * 31000.0).toInt().coerceIn(-32767, 32767).toShort())
    }
    return outBytes
  }

  private fun applyBestEngineVoice(profile: AiVoiceProfile, customVoice: CustomVoiceProfile? = null) {
    val engine = tts ?: return
    val localeTag = customVoice?.localeTag?.ifBlank { profile.localeTag } ?: profile.localeTag
    val preferFemale = customVoice?.preferFemale ?: profile.preferFemale
    val locale =
      when (localeTag) {
        "en-GB" -> Locale.UK
        "en-AU" -> Locale("en", "AU")
        else -> Locale.US
      }
    runCatching { engine.language = locale }
    val voices = runCatching { engine.voices?.toList().orEmpty() }.getOrDefault(emptyList())
    if (voices.isEmpty()) return

    val langMatches =
      voices.filter { v ->
        v.locale.language.equals(locale.language, ignoreCase = true) && !v.isNetworkConnectionRequired
      }.ifEmpty {
        voices.filter { v -> v.locale.language.equals(locale.language, ignoreCase = true) }
      }
    if (langMatches.isEmpty()) return

    val countryMatches =
      langMatches.filter { v -> v.locale.country.equals(locale.country, ignoreCase = true) }
        .ifEmpty { langMatches }

    // Separate gender candidates deterministically and rotate across distinct installed voices
    // so multiple male/female profiles pick DIFFERENT Android TTS voices whenever available.
    val matchingGender =
      countryMatches.filter { v ->
        val nameL = v.name.lowercase()
        val looksFemale =
          nameL.contains("female") || nameL.contains("sfg") || nameL.contains("tpf") ||
            nameL.contains("iob") || nameL.contains("iog") || nameL.contains("x-sfg") ||
            nameL.contains("x-tpc") || nameL.contains("x-rjs") || nameL.contains("#female")
        val looksMale =
          nameL.contains("male") || nameL.contains("iom") || nameL.contains("iol") ||
            nameL.contains("x-iom") || nameL.contains("x-tpd") || nameL.contains("x-iol") ||
            nameL.contains("x-gba") || nameL.contains("#male")
        if (preferFemale) looksFemale else looksMale
      }.ifEmpty { countryMatches }

    val rankedPool =
      matchingGender.sortedWith(
        compareByDescending<android.speech.tts.Voice> { it.quality >= android.speech.tts.Voice.QUALITY_HIGH }
          .thenByDescending { !it.isNetworkConnectionRequired }
          .thenBy { it.name }
      )
    if (rankedPool.isNotEmpty()) {
      val slotIndex = profile.ordinal % rankedPool.size
      runCatching { engine.voice = rankedPool[slotIndex] }
    }
  }

  private fun trimToSentenceBoundary(text: String, maxChars: Int): String {
    if (text.length <= maxChars) return text
    val slice = text.take(maxChars)
    val lastBreak = maxOf(slice.lastIndexOf('.'), slice.lastIndexOf('!'), slice.lastIndexOf('?'))
    return if (lastBreak >= maxChars / 2) slice.substring(0, lastBreak + 1) else "$slice..."
  }

  private fun cleanSpeechText(raw: String): String =
    raw
      .replace(Regex("\\[WIDGET:\\s*(TELEMETRY|WEATHER|SOS)\\]", RegexOption.IGNORE_CASE), " ")
      .replace(Regex("[*_`#~>]"), " ")
      .replace(Regex("https?://\\S+"), "")
      .replace(Regex("\\s+"), " ")
      .trim()
      .take(1600)

  private fun cancelActiveSpeechPlayback() {
    activeSpeechJob?.cancel()
    activeSpeechJob = null
    activeSpeechTrack?.let { tr ->
      runCatching { tr.pause() }
      runCatching { tr.flush() }
      runCatching { tr.stop() }
      runCatching { tr.release() }
    }
    activeSpeechTrack = null
    runCatching { tts?.stop() }
  }

  fun stopSpeaking() {
    pendingSpeech = null
    pendingCustomVoice = null
    val sess = activeStreamingSession
    activeStreamingSession = null
    sess?.cancel()
    cancelActiveSpeechPlayback()
  }

  // ============================================================================
  // STUDIO 44.1 kHz POLYPHONIC SYNTHESIS ENGINE (TOUCH SOUNDS, RINGTONES, ALERTS)
  // ============================================================================

  fun playTouchSound(
    enabled: Boolean,
    pack: String = "Country Acoustic",
    cue: TouchCue = TouchCue.BUTTON_TAP,
    volumePct: Int = 75,
  ) {
    if (!enabled || volumePct <= 0 || pack.equals("Silent", ignoreCase = true)) return
    val now = System.currentTimeMillis()
    if (now - lastTouchSoundMs < 42L) return
    lastTouchSoundMs = now

    val packLower = pack.trim().lowercase()
    val voice =
      when {
        packLower.contains("marimba") || packLower.contains("wood") -> InstrumentVoice.WARM_MARIMBA
        packLower.contains("glass") || packLower.contains("crystal") || packLower.contains("bell") -> InstrumentVoice.STUDIO_BELL
        packLower.contains("tactile") || packLower.contains("soft") || packLower.contains("mech") -> InstrumentVoice.TACTILE_WOOD
        else -> InstrumentVoice.ACOUSTIC_PLUCK
      }

    val notes: List<PolyNote> =
      when (cue) {
        TouchCue.TAB_SWITCH ->
          listOf(
            PolyNote(0, 85, listOf(392.0, 587.33), voice, 0.52),
            PolyNote(38, 105, listOf(523.25, 783.99), voice, 0.56),
          )
        TouchCue.BUTTON_TAP ->
          listOf(
            PolyNote(0, 72, listOf(440.0, 659.25), voice, 0.45),
          )
        TouchCue.TOGGLE_ON ->
          listOf(
            PolyNote(0, 80, listOf(440.0, 554.37), voice, 0.52),
            PolyNote(48, 120, listOf(659.25, 880.0), voice, 0.62),
          )
        TouchCue.TOGGLE_OFF ->
          listOf(
            PolyNote(0, 80, listOf(587.33, 440.0), voice, 0.48),
            PolyNote(48, 110, listOf(392.0, 293.66), voice, 0.44),
          )
        TouchCue.ACTION_CONFIRM, TouchCue.SUCCESS_CHIME ->
          listOf(
            PolyNote(0, 95, listOf(392.0, 493.88, 587.33), voice, 0.58),
            PolyNote(65, 165, listOf(587.33, 783.99, 987.77), voice, 0.66),
          )
        TouchCue.MESSAGE_SENT ->
          listOf(
            PolyNote(0, 90, listOf(523.25, 659.25), InstrumentVoice.STUDIO_BELL, 0.55),
            PolyNote(55, 150, listOf(783.99, 1046.5), InstrumentVoice.STUDIO_BELL, 0.62),
          )
        TouchCue.PHOTO_SHUTTER ->
          listOf(
            PolyNote(0, 32, listOf(1280.0, 2150.0), InstrumentVoice.TACTILE_WOOD, 0.68),
            PolyNote(42, 48, listOf(760.0, 1140.0), InstrumentVoice.TACTILE_WOOD, 0.52),
          )
        TouchCue.RIDE_START ->
          listOf(
            PolyNote(0, 140, listOf(293.66, 440.0, 587.33), InstrumentVoice.ACOUSTIC_PLUCK, 0.72),
            PolyNote(105, 140, listOf(369.99, 554.37, 739.99), InstrumentVoice.ACOUSTIC_PLUCK, 0.76),
            PolyNote(210, 280, listOf(440.0, 587.33, 880.0), InstrumentVoice.STUDIO_BELL, 0.82),
          )
        TouchCue.RIDE_STOP ->
          listOf(
            PolyNote(0, 150, listOf(587.33, 739.99, 880.0), InstrumentVoice.RHODES_CHORD, 0.72),
            PolyNote(130, 320, listOf(392.0, 493.88, 587.33), InstrumentVoice.RHODES_CHORD, 0.74),
          )
        TouchCue.BACK_NAV ->
          listOf(
            PolyNote(0, 70, listOf(493.88, 392.0), voice, 0.48),
            PolyNote(45, 95, listOf(329.63, 293.66), voice, 0.44),
          )
        TouchCue.ERROR_THUD ->
          listOf(
            PolyNote(0, 110, listOf(196.0, 233.08), InstrumentVoice.TACTILE_WOOD, 0.65),
            PolyNote(85, 145, listOf(164.81, 196.0), InstrumentVoice.TACTILE_WOOD, 0.58),
          )
      }

    val scaledVol = (volumePct * 0.68f).toInt().coerceIn(14, 78)
    playPolyphonicSequence(notes, scaledVol, AudioAttributes.USAGE_ASSISTANCE_SONIFICATION, addEcho = cue != TouchCue.BUTTON_TAP)
  }

  private fun renderPolyphonicPcm(
    notes: List<PolyNote>,
    volumePct: Int,
    addEcho: Boolean = true,
  ): ByteArray {
    if (notes.isEmpty()) return ByteArray(0)
    val echoTailMs = if (addEcho) 260 else 40
    val totalMs = notes.maxOf { it.startMs + it.durMs } + echoTailMs
    val totalSamples = ((totalMs / 1000.0) * studioSampleRate).toInt().coerceAtLeast(1)
    val mix = DoubleArray(totalSamples)

    for (n in notes) {
      val startIdx = ((n.startMs / 1000.0) * studioSampleRate).toInt().coerceAtLeast(0)
      val durSamples = ((n.durMs / 1000.0) * studioSampleRate).toInt().coerceAtLeast(1)
      val perVoiceNorm = n.gain / n.freqsHz.size.coerceAtLeast(1).toDouble()

      for (i in 0 until durSamples) {
        val idx = startIdx + i
        if (idx >= totalSamples) break
        val t = i.toDouble() / studioSampleRate
        val frac = i.toDouble() / durSamples

        // Smooth attack + natural exponential instrument envelope (zero clicks)
        val attackSamples =
          when (n.voice) {
            InstrumentVoice.TACTILE_WOOD -> 45
            InstrumentVoice.ACOUSTIC_PLUCK -> 90
            InstrumentVoice.WARM_MARIMBA -> 110
            InstrumentVoice.STUDIO_BELL -> 150
            InstrumentVoice.BRASS_HORN -> 420
            InstrumentVoice.RHODES_CHORD -> 260
            InstrumentVoice.SOFT_SYNTH -> 360
          }
        val attackEnv = min(1.0, i.toDouble() / attackSamples.toDouble())
        val decayRate =
          when (n.voice) {
            InstrumentVoice.TACTILE_WOOD -> 34.0
            InstrumentVoice.ACOUSTIC_PLUCK -> 5.8
            InstrumentVoice.WARM_MARIMBA -> 7.2
            InstrumentVoice.STUDIO_BELL -> 4.2
            InstrumentVoice.RHODES_CHORD -> 3.0
            InstrumentVoice.BRASS_HORN -> 1.8
            InstrumentVoice.SOFT_SYNTH -> 2.1
          }
        val tailFade = if (frac > 0.85) ((1.0 - frac) / 0.15).coerceIn(0.0, 1.0) else 1.0
        val env = attackEnv * exp(-decayRate * t) * tailFade

        var sampleVal = 0.0
        for (f in n.freqsHz) {
          val w = 2.0 * PI * f * t
          val tone =
            when (n.voice) {
              InstrumentVoice.ACOUSTIC_PLUCK -> {
                // Warm acoustic guitar/mandolin string: fast upper-harmonic damping
                val h2 = exp(-9.0 * t) * 0.42 * sin(2.0 * w)
                val h3 = exp(-15.0 * t) * 0.24 * sin(3.0 * w)
                val h4 = exp(-22.0 * t) * 0.12 * sin(4.0 * w)
                val body = 0.16 * sin(0.5 * w)
                0.72 * sin(w) + h2 + h3 + h4 + body
              }
              InstrumentVoice.WARM_MARIMBA -> {
                // Rosewood marimba bar: fundamental + 4th harmonic + crisp mallet strike
                val mallet = exp(-85.0 * t) * 0.35 * sin(6.2 * w)
                0.82 * sin(w) + exp(-14.0 * t) * 0.28 * sin(4.0 * w) + mallet
              }
              InstrumentVoice.STUDIO_BELL -> {
                // Crystal Rhodes/chime shimmer: detuned chorus pair + bell partial
                val shimmer = 0.55 * sin(w) + 0.32 * sin(w * 1.0025)
                val bellPartial = exp(-7.5 * t) * 0.26 * sin(2.756 * w) + exp(-11.0 * t) * 0.14 * sin(5.404 * w)
                shimmer + bellPartial
              }
              InstrumentVoice.RHODES_CHORD -> {
                // Warm electric piano with gentle tremolo & tine overtone
                val trem = 0.90 + 0.10 * sin(2.0 * PI * 4.8 * t)
                val tine = exp(-12.0 * t) * 0.28 * sin(2.0 * w) + exp(-18.0 * t) * 0.12 * sin(3.0 * w)
                trem * (0.78 * sin(w) + tine)
              }
              InstrumentVoice.TACTILE_WOOD -> {
                // Ultra-crisp wooden/mechanical detent thock
                val click = exp(-140.0 * t) * 0.55 * sin(3.2 * w)
                0.70 * sin(w) + click
              }
              InstrumentVoice.BRASS_HORN -> {
                // Rich warm country/convoy horn brass harmonics
                0.65 * sin(w) + 0.35 * sin(2.0 * w) + 0.22 * sin(3.0 * w) + 0.12 * sin(4.0 * w)
              }
              InstrumentVoice.SOFT_SYNTH -> {
                // Lush analog pad with chorus detune
                0.55 * sin(w) + 0.30 * sin(w * 1.004) + 0.22 * sin(w * 0.996) + 0.16 * sin(2.0 * w)
              }
            }
          sampleVal += tone * perVoiceNorm
        }
        mix[idx] += sampleVal * env
      }
    }

    // Subtle studio slapback delay for warm spatial depth
    if (addEcho) {
      val delay1 = (0.115 * studioSampleRate).toInt()
      val delay2 = (0.210 * studioSampleRate).toInt()
      for (i in mix.indices) {
        if (i >= delay1) mix[i] += mix[i - delay1] * 0.24
        if (i >= delay2) mix[i] += mix[i - delay2] * 0.11
      }
    }

    // Soft-clip limiter & 16-bit PCM quantization
    val masterGain = ((volumePct.coerceIn(0, 100) / 100.0) * 0.78).coerceIn(0.0, 0.90)
    val pcm = ByteArray(totalSamples * 2)
    for (i in mix.indices) {
      val x = mix[i] * masterGain
      // Smooth tanh-style soft saturation prevents any digital harshness
      val limited = x / (1.0 + kotlin.math.abs(x) * 0.35)
      val s16 = (limited.coerceIn(-0.98, 0.98) * 32767.0).toInt()
      pcm[i * 2] = (s16 and 0xFF).toByte()
      pcm[i * 2 + 1] = ((s16 shr 8) and 0xFF).toByte()
    }
    return pcm
  }

  private fun playPolyphonicSequence(
    notes: List<PolyNote>,
    volumePct: Int,
    usage: Int,
    addEcho: Boolean = true,
  ) {
    if (volumePct <= 0 || notes.isEmpty()) return
    scope.launch {
      try {
        val pcm = renderPolyphonicPcm(notes, volumePct, addEcho)
        if (pcm.isEmpty()) return@launch
        val minBuf =
          AudioTrack.getMinBufferSize(
            studioSampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
          )
        val track =
          AudioTrack.Builder()
            .setAudioAttributes(
              AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            )
            .setAudioFormat(
              AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(studioSampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            )
            .setBufferSizeInBytes(maxOf(minBuf, pcm.size))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(pcm, 0, pcm.size)
        track.play()
        val playMs = ((pcm.size / 2L) * 1000L / studioSampleRate) + 90L
        delay(playMs)
        runCatching {
          track.stop()
          track.release()
        }
      } catch (e: Exception) {
        Log.w(TAG, "Studio polyphonic synth error: ${e.message}")
      }
    }
  }

  // ============================================================================
  // HIGH-QUALITY NOTIFICATION & ALERT CHIMES (ZERO BEEP GENERATORS)
  // ============================================================================

  fun playAlertTone(
    preset: AlertTonePreset,
    volumePct: Int = 85,
    vibratePattern: VibrationPatternPreset? = null,
    hapticsEnabled: Boolean = true,
  ) {
    if (preset == AlertTonePreset.SILENT || volumePct <= 0) return
    if (hapticsEnabled && vibratePattern != null && vibratePattern != VibrationPatternPreset.OFF) {
      triggerVibrationPattern(vibratePattern)
    }
    val notes: List<PolyNote> =
      when (preset) {
        AlertTonePreset.CHIRP_TACTICAL ->
          // Warm Country Porch Pluck + Bell two-chord arrival
          listOf(
            PolyNote(0, 125, listOf(392.0, 493.88, 587.33), InstrumentVoice.ACOUSTIC_PLUCK, 0.85),
            PolyNote(95, 220, listOf(587.33, 739.99, 880.0), InstrumentVoice.STUDIO_BELL, 0.90),
          )
        AlertTonePreset.ROGER_BEEPS ->
          // Refined Dual-Harmonic Walkie Marimba Chime (warm & clean, not a harsh beep)
          listOf(
            PolyNote(0, 95, listOf(523.25, 783.99), InstrumentVoice.WARM_MARIMBA, 0.85),
            PolyNote(88, 175, listOf(659.25, 987.77), InstrumentVoice.STUDIO_BELL, 0.88),
          )
        AlertTonePreset.BELL_CLEAR ->
          // Clear Brass Trail Bell + resonant shimmer
          listOf(
            PolyNote(0, 240, listOf(783.99, 1174.66), InstrumentVoice.STUDIO_BELL, 0.90),
            PolyNote(120, 320, listOf(1046.5, 1567.98), InstrumentVoice.STUDIO_BELL, 0.85),
          )
        AlertTonePreset.CYBER_BLIP ->
          // Lush Rhodes Trio Arpeggio
          listOf(
            PolyNote(0, 110, listOf(440.0, 554.37), InstrumentVoice.RHODES_CHORD, 0.82),
            PolyNote(75, 120, listOf(554.37, 659.25), InstrumentVoice.RHODES_CHORD, 0.84),
            PolyNote(150, 210, listOf(659.25, 880.0), InstrumentVoice.STUDIO_BELL, 0.88),
          )
        AlertTonePreset.TWO_TONE_ALERT ->
          // Rich Warm Low-Battery / Caution Chord Pair
          listOf(
            PolyNote(0, 180, listOf(349.23, 440.0, 523.25), InstrumentVoice.RHODES_CHORD, 0.88),
            PolyNote(175, 280, listOf(293.66, 349.23, 440.0), InstrumentVoice.RHODES_CHORD, 0.85),
          )
        AlertTonePreset.SUBTLE_TAP ->
          // Soft Rosewood Kalimba Double-Tap
          listOf(
            PolyNote(0, 80, listOf(523.25, 659.25), InstrumentVoice.WARM_MARIMBA, 0.65),
            PolyNote(70, 110, listOf(659.25, 783.99), InstrumentVoice.WARM_MARIMBA, 0.62),
          )
        AlertTonePreset.ALARM_SIREN ->
          // Bold Brass Horn Safety Attention Fanfare
          listOf(
            PolyNote(0, 180, listOf(440.0, 554.37, 659.25), InstrumentVoice.BRASS_HORN, 0.92),
            PolyNote(195, 180, listOf(523.25, 659.25, 783.99), InstrumentVoice.BRASS_HORN, 0.94),
            PolyNote(390, 260, listOf(440.0, 554.37, 659.25), InstrumentVoice.BRASS_HORN, 0.90),
          )
        AlertTonePreset.SILENT -> emptyList()
      }
    playPolyphonicSequence(notes, volumePct, AudioAttributes.USAGE_NOTIFICATION, addEcho = true)
  }

  /** Rich polyphonic compositions for every Call Ringtone preset (NOT beeps!). */
  private fun ringtoneCompositionFor(preset: RingtonePreset): List<PolyNote> =
    when (preset) {
      RingtonePreset.VOLT_PULSE ->
        // Country Porch Acoustic Guitar Picking (G major -> D major -> Em -> C)
        listOf(
          PolyNote(0, 260, listOf(196.0, 293.66, 392.0), InstrumentVoice.ACOUSTIC_PLUCK, 0.88),
          PolyNote(180, 240, listOf(493.88, 587.33), InstrumentVoice.ACOUSTIC_PLUCK, 0.84),
          PolyNote(360, 260, listOf(392.0, 587.33, 783.99), InstrumentVoice.STUDIO_BELL, 0.86),
          PolyNote(560, 260, listOf(220.0, 329.63, 440.0), InstrumentVoice.ACOUSTIC_PLUCK, 0.85),
          PolyNote(740, 240, listOf(523.25, 659.25), InstrumentVoice.ACOUSTIC_PLUCK, 0.84),
          PolyNote(920, 380, listOf(293.66, 440.0, 587.33, 739.99), InstrumentVoice.RHODES_CHORD, 0.90),
        )
      RingtonePreset.CYBER_ARPEGGIO ->
        // Electric Horizon Rhodes + Bell Arpeggio
        listOf(
          PolyNote(0, 220, listOf(261.63, 392.0), InstrumentVoice.RHODES_CHORD, 0.84),
          PolyNote(140, 220, listOf(329.63, 493.88), InstrumentVoice.RHODES_CHORD, 0.84),
          PolyNote(280, 220, listOf(392.0, 587.33), InstrumentVoice.STUDIO_BELL, 0.86),
          PolyNote(420, 240, listOf(523.25, 783.99), InstrumentVoice.STUDIO_BELL, 0.88),
          PolyNote(580, 220, listOf(440.0, 659.25), InstrumentVoice.RHODES_CHORD, 0.85),
          PolyNote(740, 420, listOf(392.0, 523.25, 659.25, 1046.5), InstrumentVoice.SOFT_SYNTH, 0.90),
        )
      RingtonePreset.TACTICAL_RADAR ->
        // Sunrise Marimba & Kalimba Cascade
        listOf(
          PolyNote(0, 180, listOf(392.0, 587.33), InstrumentVoice.WARM_MARIMBA, 0.86),
          PolyNote(150, 180, listOf(493.88, 739.99), InstrumentVoice.WARM_MARIMBA, 0.88),
          PolyNote(300, 220, listOf(587.33, 880.0), InstrumentVoice.WARM_MARIMBA, 0.90),
          PolyNote(500, 180, listOf(493.88, 659.25), InstrumentVoice.WARM_MARIMBA, 0.85),
          PolyNote(660, 360, listOf(392.0, 493.88, 587.33, 783.99), InstrumentVoice.STUDIO_BELL, 0.90),
        )
      RingtonePreset.HYPER_SCOOTER ->
        // Energetic Backroad Rhythm Chords
        listOf(
          PolyNote(0, 190, listOf(329.63, 493.88, 659.25), InstrumentVoice.ACOUSTIC_PLUCK, 0.88),
          PolyNote(170, 170, listOf(392.0, 587.33), InstrumentVoice.WARM_MARIMBA, 0.85),
          PolyNote(340, 190, listOf(440.0, 659.25, 880.0), InstrumentVoice.ACOUSTIC_PLUCK, 0.90),
          PolyNote(560, 190, listOf(493.88, 739.99), InstrumentVoice.STUDIO_BELL, 0.88),
          PolyNote(740, 380, listOf(329.63, 493.88, 659.25, 987.77), InstrumentVoice.RHODES_CHORD, 0.92),
        )
      RingtonePreset.CLASSIC_BELL ->
        // Golden Trail Brass & Wind Chime Bell
        listOf(
          PolyNote(0, 300, listOf(659.25, 987.77), InstrumentVoice.STUDIO_BELL, 0.88),
          PolyNote(220, 300, listOf(783.99, 1174.66), InstrumentVoice.STUDIO_BELL, 0.90),
          PolyNote(460, 300, listOf(1046.5, 1318.51), InstrumentVoice.STUDIO_BELL, 0.92),
          PolyNote(760, 420, listOf(783.99, 1046.5, 1567.98), InstrumentVoice.STUDIO_BELL, 0.88),
        )
      RingtonePreset.RETRO_CHIME_95 ->
        // Lush Warm Studio Startup Chord Progression
        listOf(
          PolyNote(0, 300, listOf(261.63, 329.63, 392.0, 523.25), InstrumentVoice.SOFT_SYNTH, 0.86),
          PolyNote(260, 280, listOf(293.66, 369.99, 440.0, 587.33), InstrumentVoice.RHODES_CHORD, 0.88),
          PolyNote(520, 280, listOf(329.63, 415.30, 493.88, 659.25), InstrumentVoice.RHODES_CHORD, 0.88),
          PolyNote(780, 480, listOf(392.0, 493.88, 587.33, 783.99), InstrumentVoice.STUDIO_BELL, 0.92),
        )
      RingtonePreset.AVIATION_COCKPIT ->
        // Clean Crystal Cabin Chime (Eb major 9)
        listOf(
          PolyNote(0, 340, listOf(622.25, 932.33), InstrumentVoice.STUDIO_BELL, 0.88),
          PolyNote(320, 420, listOf(466.16, 698.46, 932.33), InstrumentVoice.RHODES_CHORD, 0.90),
          PolyNote(740, 420, listOf(622.25, 783.99, 1244.51), InstrumentVoice.STUDIO_BELL, 0.88),
        )
      RingtonePreset.LASER_PING ->
        // Bright Kalimba & Crystal Cascade
        listOf(
          PolyNote(0, 170, listOf(587.33, 880.0), InstrumentVoice.WARM_MARIMBA, 0.84),
          PolyNote(130, 170, listOf(659.25, 987.77), InstrumentVoice.STUDIO_BELL, 0.86),
          PolyNote(260, 190, listOf(783.99, 1174.66), InstrumentVoice.STUDIO_BELL, 0.88),
          PolyNote(430, 360, listOf(523.25, 783.99, 1046.5), InstrumentVoice.RHODES_CHORD, 0.90),
        )
      RingtonePreset.TRAIL_HORN ->
        // Rich Country Convoy Brass Fanfare
        listOf(
          PolyNote(0, 260, listOf(220.0, 329.63, 440.0), InstrumentVoice.BRASS_HORN, 0.88),
          PolyNote(260, 220, listOf(261.63, 392.0, 523.25), InstrumentVoice.BRASS_HORN, 0.90),
          PolyNote(490, 460, listOf(293.66, 440.0, 587.33), InstrumentVoice.BRASS_HORN, 0.92),
        )
      RingtonePreset.SYNTH_WAVE ->
        // Warm Sunset Analog Pad Melody
        listOf(
          PolyNote(0, 260, listOf(220.0, 329.63, 440.0), InstrumentVoice.SOFT_SYNTH, 0.86),
          PolyNote(240, 260, listOf(261.63, 392.0, 523.25), InstrumentVoice.SOFT_SYNTH, 0.86),
          PolyNote(480, 260, listOf(293.66, 440.0, 587.33), InstrumentVoice.RHODES_CHORD, 0.88),
          PolyNote(720, 440, listOf(329.63, 493.88, 659.25), InstrumentVoice.STUDIO_BELL, 0.90),
        )
      RingtonePreset.SONAR_ECHO ->
        // Deep Valley Wind Chime & Acoustic Harmonic Echo
        listOf(
          PolyNote(0, 380, listOf(293.66, 440.0, 587.33), InstrumentVoice.RHODES_CHORD, 0.86),
          PolyNote(380, 380, listOf(440.0, 659.25), InstrumentVoice.STUDIO_BELL, 0.84),
          PolyNote(760, 480, listOf(293.66, 587.33, 880.0), InstrumentVoice.STUDIO_BELL, 0.88),
        )
      RingtonePreset.SYSTEM_DEFAULT -> emptyList()
    }

  fun playAlertTonePreset(
    preset: AlertTonePreset,
    volumePct: Int = 85,
    vibratePattern: VibrationPatternPreset? = null,
    hapticsEnabled: Boolean = true,
  ) = playAlertTone(preset, volumePct, vibratePattern, hapticsEnabled)

  fun playRingtonePreset(
    preset: RingtonePreset,
    customUri: String = "",
    volumePct: Int = 85,
  ) = previewRingtone(preset, customUri, volumePct)

  fun playSoundPackPreview(pack: String, volumePct: Int = 85) {
    if (volumePct <= 0) return
    val p = pack.lowercase()
    val voice =
      when {
        p.contains("marimba") || p.contains("wood") -> InstrumentVoice.WARM_MARIMBA
        p.contains("glass") || p.contains("crystal") || p.contains("bell") -> InstrumentVoice.STUDIO_BELL
        p.contains("retro") || p.contains("synth") || p.contains("rhodes") -> InstrumentVoice.RHODES_CHORD
        p.contains("tactile") || p.contains("soft") -> InstrumentVoice.TACTILE_WOOD
        else -> InstrumentVoice.ACOUSTIC_PLUCK
      }
    val notes =
      listOf(
        PolyNote(0, 140, listOf(293.66, 440.0, 587.33), voice, 0.78),
        PolyNote(130, 150, listOf(369.99, 554.37, 739.99), voice, 0.82),
        PolyNote(260, 290, listOf(440.0, 587.33, 880.0), InstrumentVoice.STUDIO_BELL, 0.86),
      )
    playPolyphonicSequence(notes, volumePct, AudioAttributes.USAGE_ASSISTANCE_SONIFICATION, addEcho = true)
  }

  fun previewRingtone(
    preset: RingtonePreset,
    customUriStr: String = "",
    volumePct: Int = 85,
  ) {
    stopRingtone()
    if (preset == RingtonePreset.SYSTEM_DEFAULT || customUriStr.isNotBlank()) {
      startSystemRingtone(customUriStr, autoStopAfterMs = 2600L)
      return
    }
    val comp = ringtoneCompositionFor(preset)
    playPolyphonicSequence(comp, volumePct, AudioAttributes.USAGE_NOTIFICATION_RINGTONE, addEcho = true)
  }

  fun startIncomingRingtone(
    preset: RingtonePreset = RingtonePreset.VOLT_PULSE,
    customUriStr: String = "",
    volumePct: Int = 85,
    vibrationPattern: VibrationPatternPreset = VibrationPatternPreset.DOUBLE_TAP,
    hapticsEnabled: Boolean = true,
  ) {
    stopRingtone()
    if (volumePct <= 0) return
    ringtoneLoopActive = true

    if (preset == RingtonePreset.SYSTEM_DEFAULT || customUriStr.isNotBlank()) {
      startSystemRingtone(customUriStr, autoStopAfterMs = 0L)
      if (hapticsEnabled && vibrationPattern != VibrationPatternPreset.OFF) {
        ringtoneSynthJob =
          scope.launch {
            while (isActive && ringtoneLoopActive) {
              triggerVibrationPattern(vibrationPattern)
              delay(1800L)
            }
          }
      }
      return
    }

    val comp = ringtoneCompositionFor(preset)
    val pcm = renderPolyphonicPcm(comp, volumePct, addEcho = true)
    if (pcm.isEmpty()) return

    ringtoneSynthJob =
      scope.launch {
        while (isActive && ringtoneLoopActive) {
          if (hapticsEnabled && vibrationPattern != VibrationPatternPreset.OFF) {
            triggerVibrationPattern(vibrationPattern)
          }
          try {
            val minBuf =
              AudioTrack.getMinBufferSize(
                studioSampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
              )
            val track =
              AudioTrack.Builder()
                .setAudioAttributes(
                  AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                )
                .setAudioFormat(
                  AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(studioSampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
                )
                .setBufferSizeInBytes(maxOf(minBuf, pcm.size))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            activeRingtoneTrack = track
            track.write(pcm, 0, pcm.size)
            track.play()
            val playMs = ((pcm.size / 2L) * 1000L / studioSampleRate) + 120L
            delay(playMs)
            runCatching {
              track.stop()
              track.release()
            }
            if (activeRingtoneTrack === track) activeRingtoneTrack = null
          } catch (e: Exception) {
            Log.w(TAG, "Ringtone synth loop error: ${e.message}")
            break
          }
          delay(950L)
        }
      }
  }

  private fun startSystemRingtone(customUriStr: String, autoStopAfterMs: Long) {
    try {
      val uri =
        if (customUriStr.isNotBlank()) Uri.parse(customUriStr)
        else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
          ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      if (uri != null) {
        val ring = RingtoneManager.getRingtone(context.applicationContext, uri)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          ring?.isLooping = autoStopAfterMs == 0L
        }
        ring?.play()
        activeRingtone = ring
        if (autoStopAfterMs > 0L) {
          ringtoneSynthJob =
            scope.launch {
              delay(autoStopAfterMs)
              stopRingtone()
            }
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "System ringtone start failed: ${e.message}")
    }
  }

  fun stopRingtone() {
    ringtoneLoopActive = false
    ringtoneSynthJob?.cancel()
    ringtoneSynthJob = null
    runCatching {
      activeRingtoneTrack?.stop()
      activeRingtoneTrack?.release()
    }
    activeRingtoneTrack = null
    runCatching { activeRingtone?.stop() }
    activeRingtone = null
  }

  fun playIncomingCallChime(
    enabled: Boolean,
    volumePct: Int = 85,
    ringtonePreset: RingtonePreset = RingtonePreset.VOLT_PULSE,
    customUri: String = "",
    vibrationPattern: VibrationPatternPreset = VibrationPatternPreset.DOUBLE_TAP,
    hapticsEnabled: Boolean = true,
  ) {
    if (!enabled) {
      stopRingtone()
      return
    }
    startIncomingRingtone(
      preset = ringtonePreset,
      customUriStr = customUri,
      volumePct = volumePct,
      vibrationPattern = vibrationPattern,
      hapticsEnabled = hapticsEnabled,
    )
  }

  fun setCallSpeakerphone(enabled: Boolean) = setSpeakerphoneOn(enabled)

  fun resetCallAudioMode() = resetAudioMode()

  fun setSpeakerphoneOn(enabled: Boolean) {
    runCatching {
      val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
      // Stop any active ringtone before switching communication audio routing
      stopRingtone()
      am.mode = if (enabled) AudioManager.MODE_IN_COMMUNICATION else AudioManager.MODE_IN_COMMUNICATION
      @Suppress("DEPRECATION")
      runCatching { am.isSpeakerphoneOn = enabled }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val devices = am.availableCommunicationDevices
        if (enabled) {
          val speaker =
            devices.firstOrNull {
              it.type == android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            }
          if (speaker != null) {
            am.setCommunicationDevice(speaker)
          }
        } else {
          val earpiece =
            devices.firstOrNull {
              it.type == android.media.AudioDeviceInfo.TYPE_BUILTIN_EARPIECE ||
                it.type == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET ||
                it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET
            }
          if (earpiece != null) {
            am.setCommunicationDevice(earpiece)
          } else {
            am.clearCommunicationDevice()
          }
        }
      }
      if (enabled) {
        // Ensure neither voice call stream nor media stream (used by WebView WebRTC/WebAudio) is muted
        listOf(AudioManager.STREAM_VOICE_CALL, AudioManager.STREAM_MUSIC).forEach { stream ->
          runCatching {
            val maxVol = am.getStreamMaxVolume(stream).coerceAtLeast(1)
            val curVol = am.getStreamVolume(stream)
            val targetMin = (maxVol * 0.72f).toInt().coerceAtLeast(1)
            if (curVol < targetMin) {
              am.setStreamVolume(stream, targetMin, 0)
            }
          }
        }
      }
    }
  }

  fun resetAudioMode() {
    runCatching {
      val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        runCatching { am.clearCommunicationDevice() }
      }
      @Suppress("DEPRECATION")
      runCatching { am.isSpeakerphoneOn = false }
      am.mode = AudioManager.MODE_NORMAL
    }
  }

  /** Studio-grade Walkie-Talkie PTT squelch chime (replaces raw ToneGenerator beeps). */
  fun playSquelchChirp(volumePct: Int = 80, isEnd: Boolean = false) {
    if (volumePct <= 0) return
    val notes =
      if (isEnd) {
        listOf(
          PolyNote(0, 75, listOf(659.25, 987.77), InstrumentVoice.WARM_MARIMBA, 0.72),
          PolyNote(68, 125, listOf(493.88, 739.99), InstrumentVoice.STUDIO_BELL, 0.75),
        )
      } else {
        listOf(
          PolyNote(0, 80, listOf(493.88, 739.99), InstrumentVoice.WARM_MARIMBA, 0.74),
          PolyNote(62, 110, listOf(659.25, 987.77), InstrumentVoice.STUDIO_BELL, 0.78),
        )
      }
    playPolyphonicSequence(notes, volumePct, AudioAttributes.USAGE_ASSISTANCE_SONIFICATION, addEcho = true)
  }

  enum class RetroCueType {
    CLICK,
    STARTUP_CHIME,
    MISSION_START,
    MISSION_SAVED,
    MISSION_STOP,
    ALERT_CHORD,
    ALERT_WARNING,
    COACH_BRIEF,
    NOTIFY_DING,
  }

  /** High-quality polyphonic musical audio cues (zero ToneGenerator beeps). */
  fun playRetroAudioCue(
    enabled: Boolean,
    volumePct: Int = 80,
    cueType: RetroCueType = RetroCueType.CLICK,
  ) {
    if (!enabled || volumePct <= 0) return
    val notes =
      when (cueType) {
        RetroCueType.CLICK ->
          listOf(PolyNote(0, 60, listOf(523.25, 783.99), InstrumentVoice.TACTILE_WOOD, 0.50))
        RetroCueType.STARTUP_CHIME ->
          listOf(
            PolyNote(0, 220, listOf(293.66, 440.0, 587.33), InstrumentVoice.ACOUSTIC_PLUCK, 0.80),
            PolyNote(150, 220, listOf(369.99, 554.37, 739.99), InstrumentVoice.RHODES_CHORD, 0.84),
            PolyNote(300, 380, listOf(440.0, 587.33, 880.0, 1174.66), InstrumentVoice.STUDIO_BELL, 0.88),
          )
        RetroCueType.MISSION_START ->
          listOf(
            PolyNote(0, 150, listOf(392.0, 587.33), InstrumentVoice.ACOUSTIC_PLUCK, 0.80),
            PolyNote(110, 260, listOf(587.33, 783.99, 987.77), InstrumentVoice.STUDIO_BELL, 0.86),
          )
        RetroCueType.MISSION_SAVED ->
          listOf(
            PolyNote(0, 150, listOf(440.0, 554.37, 659.25), InstrumentVoice.RHODES_CHORD, 0.82),
            PolyNote(130, 300, listOf(587.33, 739.99, 880.0), InstrumentVoice.STUDIO_BELL, 0.88),
          )
        RetroCueType.MISSION_STOP ->
          listOf(
            PolyNote(0, 140, listOf(587.33, 440.0), InstrumentVoice.WARM_MARIMBA, 0.78),
            PolyNote(110, 240, listOf(392.0, 293.66), InstrumentVoice.RHODES_CHORD, 0.76),
          )
        RetroCueType.ALERT_CHORD, RetroCueType.ALERT_WARNING ->
          listOf(
            PolyNote(0, 160, listOf(349.23, 440.0, 523.25), InstrumentVoice.BRASS_HORN, 0.85),
            PolyNote(150, 220, listOf(329.63, 392.0, 493.88), InstrumentVoice.BRASS_HORN, 0.82),
          )
        RetroCueType.COACH_BRIEF ->
          listOf(
            PolyNote(0, 140, listOf(392.0, 587.33), InstrumentVoice.RHODES_CHORD, 0.75),
            PolyNote(105, 220, listOf(523.25, 783.99), InstrumentVoice.STUDIO_BELL, 0.80),
          )
        RetroCueType.NOTIFY_DING ->
          listOf(
            PolyNote(0, 180, listOf(659.25, 987.77), InstrumentVoice.STUDIO_BELL, 0.80),
            PolyNote(120, 260, listOf(880.0, 1318.51), InstrumentVoice.STUDIO_BELL, 0.84),
          )
      }
    playPolyphonicSequence(notes, volumePct, AudioAttributes.USAGE_ASSISTANCE_SONIFICATION, addEcho = true)
  }

  // ============================================================================
  // HAPTICS & VIBRATION
  // ============================================================================

  enum class HapticCue {
    LIGHT_TICK,
    MEDIUM_CONFIRM,
    MEDIUM_CLICK,
    SUCCESS_DOUBLE,
    WARNING_TRIPLE,
    WARNING_PULSE,
    HEAVY_IMPACT,
    HEAVY_THUD,
    BOOT_SWEEP,
  }

  private fun getVibrator(): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
      vm?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

  fun triggerHaptic(enabled: Boolean, durationMs: Int = 20) {
    if (!enabled) return
    try {
      val vibrator = getVibrator() ?: return
      if (!vibrator.hasVibrator()) return
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(
          VibrationEffect.createOneShot(
            durationMs.toLong().coerceIn(8L, 120L),
            VibrationEffect.DEFAULT_AMPLITUDE,
          )
        )
      } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(durationMs.toLong().coerceIn(8L, 120L))
      }
    } catch (_: Exception) {}
  }

  fun triggerHapticCue(enabled: Boolean, cue: HapticCue, durationMs: Int = 22): Boolean {
    if (!enabled) return false
    return try {
      val vibrator = getVibrator() ?: return false
      if (!vibrator.hasVibrator()) return false
      val scale = (durationMs / 22f).coerceIn(0.6f, 2.2f)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val effect =
          when (cue) {
            HapticCue.LIGHT_TICK ->
              VibrationEffect.createOneShot((12f * scale).toLong().coerceIn(8L, 45L), 110)
            HapticCue.MEDIUM_CONFIRM, HapticCue.MEDIUM_CLICK ->
              VibrationEffect.createOneShot((24f * scale).toLong().coerceIn(14L, 70L), 180)
            HapticCue.SUCCESS_DOUBLE ->
              VibrationEffect.createWaveform(longArrayOf(0, 22, 55, 36), intArrayOf(0, 150, 0, 220), -1)
            HapticCue.WARNING_TRIPLE, HapticCue.WARNING_PULSE ->
              VibrationEffect.createWaveform(longArrayOf(0, 34, 48, 34, 48, 44), intArrayOf(0, 200, 0, 220, 0, 255), -1)
            HapticCue.HEAVY_IMPACT, HapticCue.HEAVY_THUD ->
              VibrationEffect.createOneShot((55f * scale).toLong().coerceIn(30L, 120L), 255)
            HapticCue.BOOT_SWEEP ->
              VibrationEffect.createWaveform(longArrayOf(0, 18, 40, 28, 40, 42), intArrayOf(0, 90, 0, 155, 0, 235), -1)
          }
        vibrator.vibrate(effect)
      } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(durationMs.toLong().coerceIn(10L, 90L))
      }
      true
    } catch (_: Exception) {
      false
    }
  }

  fun triggerVibrationPattern(pattern: VibrationPatternPreset) {
    if (pattern == VibrationPatternPreset.OFF || pattern.timings.isEmpty()) return
    try {
      val vibrator = getVibrator() ?: return
      if (!vibrator.hasVibrator()) return
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createWaveform(pattern.timings, -1))
      } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(pattern.timings, -1)
      }
    } catch (_: Exception) {}
  }

  // ============================================================================
  // WALKIE-TALKIE PTT & VOICE NOTE RECORDING / PLAYBACK
  // ============================================================================

  fun hasMicPermission(): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
      PackageManager.PERMISSION_GRANTED

  fun startPttRecording(volumePct: Int = 80): Boolean {
    if (!hasMicPermission() || isRecording) return false
    val minBuf =
      AudioRecord.getMinBufferSize(
        voiceSampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
      )
    if (minBuf <= 0) return false
    return try {
      playSquelchChirp(isEnd = false, volumePct = volumePct)
      val rec =
        AudioRecord(
          MediaRecorder.AudioSource.MIC,
          voiceSampleRate,
          AudioFormat.CHANNEL_IN_MONO,
          AudioFormat.ENCODING_PCM_16BIT,
          minBuf * 2,
        )
      if (rec.state != AudioRecord.STATE_INITIALIZED) {
        rec.release()
        return false
      }
      recordingBuffer = ByteArrayOutputStream()
      audioRecord = rec
      isRecording = true
      rec.startRecording()
      recordingThread =
        Thread {
          val buf = ByteArray(minBuf)
          val maxBytes = voiceSampleRate * 2 * 7
          while (isRecording && recordingBuffer.size() < maxBytes) {
            val read = rec.read(buf, 0, buf.size)
            if (read > 0) recordingBuffer.write(buf, 0, read)
          }
        }.apply { start() }
      true
    } catch (e: SecurityException) {
      Log.w(TAG, "Mic permission denied: ${e.message}")
      false
    } catch (e: Exception) {
      Log.w(TAG, "PTT start error: ${e.message}")
      false
    }
  }

  fun stopPttRecordingAndEncode(volumePct: Int = 80): Pair<String, Long>? {
    if (!isRecording) return null
    isRecording = false
    return try {
      audioRecord?.stop()
      recordingThread?.join(400L)
      audioRecord?.release()
      audioRecord = null
      playSquelchChirp(isEnd = true, volumePct = volumePct)
      val pcm = recordingBuffer.toByteArray()
      if (pcm.size < 1600) return null
      val durationMs = (pcm.size / (voiceSampleRate * 2.0) * 1000.0).toLong().coerceAtLeast(300L)
      val b64 = Base64.encodeToString(pcm, Base64.NO_WRAP)
      b64 to durationMs
    } catch (e: Exception) {
      Log.w(TAG, "PTT stop error: ${e.message}")
      null
    }
  }

  fun playPcmBase64(audioBase64: String, volumePct: Int = 85) {
    if (audioBase64.isBlank() || volumePct <= 0) return
    scope.launch {
      try {
        val pcm = Base64.decode(audioBase64, Base64.DEFAULT)
        if (pcm.isEmpty()) return@launch
        playSquelchChirp(isEnd = false, volumePct = volumePct)
        val minBuf =
          AudioTrack.getMinBufferSize(
            voiceSampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
          )
        val track =
          AudioTrack.Builder()
            .setAudioAttributes(
              AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            )
            .setAudioFormat(
              AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(voiceSampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            )
            .setBufferSizeInBytes(maxOf(minBuf, pcm.size))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        val gain = (volumePct / 100f).coerceIn(0.1f, 1.0f)
        track.setVolume(gain)
        track.write(pcm, 0, pcm.size)
        track.play()
        val playMs = (pcm.size / (voiceSampleRate * 2.0) * 1000.0).toLong() + 180L
        delay(playMs)
        track.stop()
        track.release()
        playSquelchChirp(isEnd = true, volumePct = volumePct)
      } catch (e: Exception) {
        Log.w(TAG, "PCM playback failed: ${e.message}")
      }
    }
  }

  fun hasRecordPermission(): Boolean = hasMicPermission()

  fun startPttCapture(
    callerScope: CoroutineScope,
    maxMs: Long = 10_000L,
    onAutoFinish: (String, Long) -> Unit,
  ): Boolean {
    val started = startPttRecording()
    if (!started) return false
    callerScope.launch {
      delay(maxMs.coerceIn(1_500L, 15_000L))
      if (isRecording) {
        val res = stopPttRecordingAndEncode()
        onAutoFinish(res?.first.orEmpty(), res?.second ?: 0L)
      }
    }
    return true
  }

  fun finishPttCapture(): Pair<String, Long> = stopPttRecordingAndEncode() ?: ("" to 0L)

  fun playBase64Pcm(audioBase64: String, volumePct: Int = 85) = playPcmBase64(audioBase64, volumePct)

  fun playBase64Pcm(
    @Suppress("UNUSED_PARAMETER") callerScope: CoroutineScope,
    audioBase64: String,
    volumePct: Int = 85,
  ) = playPcmBase64(audioBase64, volumePct)

  fun promptBiometricUnlock(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit,
  ) {
    try {
      val authenticators =
        BiometricManager.Authenticators.BIOMETRIC_WEAK or
          BiometricManager.Authenticators.DEVICE_CREDENTIAL
      val bm = BiometricManager.from(activity)
      if (bm.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
        onFailure("No fingerprint, face unlock, or device PIN enrolled on this device.")
        return
      }
      val executor = ContextCompat.getMainExecutor(activity)
      val prompt =
        BiometricPrompt(
          activity,
          executor,
          object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
              onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
              onFailure(errString.toString())
            }

            override fun onAuthenticationFailed() {
              onFailure("Biometric did not match. Try again.")
            }
          },
        )
      val info =
        BiometricPrompt.PromptInfo.Builder()
          .setTitle("Unlock Rural ERides GO")
          .setSubtitle("Verify your identity to access your PEV cockpit")
          .setAllowedAuthenticators(authenticators)
          .build()
      prompt.authenticate(info)
    } catch (e: Exception) {
      onFailure(e.message ?: "Biometric unlock unavailable")
    }
  }

  fun release() {
    stopRingtone()
    stopSpeaking()
    neuralAudioCache.evictAll()
    pcmFragmentCache.evictAll()
    runCatching { tts?.shutdown() }
    tts = null
    ttsReady = false
  }
}
