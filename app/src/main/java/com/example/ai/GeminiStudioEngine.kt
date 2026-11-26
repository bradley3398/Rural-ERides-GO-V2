package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.core.AppCheckHealthManager
import com.example.core.LiveCloudServices
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val STUDIO_TAG = "GeminiStudioEngine"

data class GeneratedImageResult(
  val bitmap: Bitmap,
  val captionText: String,
  val modelUsed: String,
)

data class AudioTranscriptionResult(
  val transcript: String,
  val summary: String,
  val actionCategory: String,
)

data class VoiceChatTurn(
  val id: String,
  val isUser: Boolean,
  val text: String,
  val modelUsed: String = "",
  val timestampMs: Long = System.currentTimeMillis(),
)

data class GroundedPlacePin(
  val title: String,
  val subtitle: String,
  val url: String,
  val lat: Double? = null,
  val lng: Double? = null,
)

data class GroundedExploreResult(
  val answer: String,
  val places: List<GroundedPlacePin>,
  val webSources: List<Pair<String, String>>,
  val usedSearchTool: Boolean,
)

data class AiComposedTrack(
  val title: String,
  val genre: String,
  val bpm: Int,
  val musicalKey: String,
  val chords: List<String>,
  val leadNotesMidi: List<Int>,
  val bassNotesMidi: List<Int>,
  val vibeSummary: String,
  val lyricHook: String,
)

/**
 * Firebase AI Logic multimodal studio engine powering:
 * - Runtime image generation & photo editing (gemini-nano-banana-2.1)
 * - Direct audio WAV transcription (gemini-3.8-flash multimodal inline audio)
 * - Multi-turn hands-free voice conversation (gemini-3.8-flash / 3.5-flash-lite / 4-argon)
 * - Live Google Search & GPS Maps grounded exploration
 * - Structured AI trail soundtrack composition for 44.1kHz stereo PCM synthesis
 */
class GeminiStudioEngine {

  suspend fun generateOrEditImage(
    prompt: String,
    sourceBitmap: Bitmap? = null,
    aspectRatioKey: String = "1:1",
  ): Result<GeneratedImageResult> {
    val aspect =
      when (aspectRatioKey) {
        "16:9" -> AspectRatio.LANDSCAPE_16x9
        "9:16" -> AspectRatio.PORTRAIT_9x16
        else -> AspectRatio.SQUARE_1x1
      }
    val config =
      generationConfig {
        responseModalities = listOf(ResponseModality.IMAGE, ResponseModality.TEXT)
        imageConfig =
          ImageConfig.builder()
            .setAspectRatio(aspect)
            .setImageSize(ImageSize.SIZE_1K)
            .build()
      }

    var lastError: Exception? = null
    for (imageModelId in GeminiModelCatalog.imageFallbackChain) {
      try {
        val model =
          AppCheckHealthManager.createGenerativeModel(
            modelName = imageModelId,
            generationConfig = config,
          )
        val response =
          AppCheckHealthManager.runWithAppCheckRecovery(STUDIO_TAG) {
            withTimeout(60_000L) {
              if (sourceBitmap != null) {
                model.generateContent(
                  content {
                    image(sourceBitmap)
                    text(prompt)
                  }
                )
              } else {
                model.generateContent(prompt)
              }
            }
          }
        val parts = response.candidates.firstOrNull()?.content?.parts.orEmpty()
        val imagePart = parts.filterIsInstance<ImagePart>().firstOrNull()
        val bitmap = imagePart?.image
        if (bitmap != null) {
          val caption = response.text?.trim().orEmpty()
          return Result.success(
            GeneratedImageResult(
              bitmap = bitmap,
              captionText = caption,
              modelUsed = imageModelId,
            )
          )
        } else {
          lastError = IllegalStateException("Gemini returned text without an image. Try a more descriptive visual prompt.")
        }
      } catch (e: TimeoutCancellationException) {
        Log.e(STUDIO_TAG, "Image generation timed out on $imageModelId", e)
        lastError = Exception("Image generation timed out after 60 seconds. Please try again.")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(STUDIO_TAG, "Image generation failed on $imageModelId: ${e.message}")
        lastError = e
      }
    }
    return Result.failure(lastError ?: Exception("Image generation failed."))
  }

  suspend fun transcribeAudioWav(
    wavBytes: ByteArray,
    vehicleContext: String,
  ): Result<AudioTranscriptionResult> {
    if (wavBytes.size < 256) {
      return Result.failure(IllegalArgumentException("Audio clip is too short to transcribe."))
    }
    val schema =
      Schema.obj(
        properties =
          mapOf(
            "transcript" to Schema.string("Verbatim spoken transcription of the audio clip"),
            "summary" to Schema.string("Concise 1-sentence rural rider note summary"),
            "actionCategory" to Schema.enumeration(listOf("RIDE_LOG", "TRAIL_HAZARD", "MECHANIC_NOTE", "GENERAL_VOICE")),
          )
      )
    var lastError: Exception? = null
    for (modelId in listOf(GeminiModelCatalog.GEMINI_3_8_FLASH, GeminiModelCatalog.GEMINI_3_5_FLASH_LITE)) {
      try {
        val model =
          AppCheckHealthManager.createGenerativeModel(
            modelName = modelId,
            generationConfig =
              generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
                temperature = 0.1f
              },
          )
        val response =
          AppCheckHealthManager.runWithAppCheckRecovery(STUDIO_TAG) {
            withTimeout(60_000L) {
              model.generateContent(
                content {
                  inlineData(wavBytes, "audio/wav")
                  text(
                    "Transcribe this microphone audio from an electric PEV rider accurately verbatim. " +
                      "Rider context: $vehicleContext. Return JSON with transcript, summary, and actionCategory."
                  )
                }
              )
            }
          }
        val raw = response.text ?: error("Empty transcription response.")
        val json = JSONObject(raw)
        return Result.success(
          AudioTranscriptionResult(
            transcript = json.optString("transcript").trim(),
            summary = json.optString("summary").trim(),
            actionCategory = json.optString("actionCategory", "GENERAL_VOICE"),
          )
        )
      } catch (e: TimeoutCancellationException) {
        Log.e(STUDIO_TAG, "Audio transcription timed out on $modelId", e)
        lastError = Exception("Audio transcription timed out after 60 seconds.")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(STUDIO_TAG, "Audio transcription failed on $modelId: ${e.message}")
        lastError = e
      }
    }
    return Result.failure(lastError ?: Exception("Audio transcription failed."))
  }

  suspend fun runVoiceConversationTurn(
    userMessage: String,
    audioWavBytes: ByteArray?,
    history: List<VoiceChatTurn>,
    personaRole: String,
    modelId: String,
    vehicleContext: String,
    locationLabel: String,
  ): Result<Pair<String, String>> {
    val chain = GeminiModelCatalog.fallbackChain(modelId)
    val sysInstruction =
      content {
        text(
          """
          You are the Rural ERides GO Live Voice Co-Pilot ($personaRole).
          Speak naturally, warmly, and concisely in 2 to 4 crisp spoken sentences (no markdown bullets or asterisks) so your reply sounds great read aloud while riding.
          Active Rig: $vehicleContext | Location: $locationLabel.
          If audio is attached, answer the spoken question directly.
          """
            .trimIndent()
        )
      }
    val chatHistory =
      history.takeLast(12).map { turn ->
        content(if (turn.isUser) "user" else "model") { text(turn.text) }
      }

    var lastError: Exception? = null
    for (activeModel in chain) {
      try {
        val model =
          AppCheckHealthManager.createGenerativeModel(
            modelName = activeModel,
            systemInstruction = sysInstruction,
            generationConfig = generationConfig { temperature = 0.7f },
          )
        val chat = model.startChat(history = chatHistory)
        val response =
          AppCheckHealthManager.runWithAppCheckRecovery(STUDIO_TAG) {
            withTimeout(60_000L) {
              if (audioWavBytes != null && audioWavBytes.size > 256) {
                chat.sendMessage(
                  content("user") {
                    inlineData(audioWavBytes, "audio/wav")
                    if (userMessage.isNotBlank()) text(userMessage)
                    else text("Listen to my voice message and answer conversationally as my riding co-pilot.")
                  }
                )
              } else {
                chat.sendMessage(userMessage)
              }
            }
          }
        val reply = response.text?.trim().orEmpty().ifBlank { "Copy that partner — signal was faint, say again?" }
        return Result.success(reply to activeModel)
      } catch (e: TimeoutCancellationException) {
        Log.e(STUDIO_TAG, "Voice turn timed out on $activeModel", e)
        lastError = Exception("Voice Co-Pilot timed out after 60s.")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(STUDIO_TAG, "Voice turn failed on $activeModel: ${e.message}")
        lastError = e
      }
    }
    return Result.failure(lastError ?: Exception("Voice Co-Pilot failed."))
  }

  suspend fun exploreWithSearchAndMaps(
    query: String,
    lat: Double,
    lng: Double,
    locationLabel: String,
    useGoogleSearch: Boolean,
    useMapPlaces: Boolean,
  ): Result<GroundedExploreResult> =
    try {
      val livePlaces = mutableListOf<GroundedPlacePin>()
      if (useMapPlaces && (lat != 0.0 || lng != 0.0 || query.length >= 3)) {
        runCatching {
          val geo = LiveCloudServices.searchNavDestinations(query, lat, lng).getOrDefault(emptyList())
          geo.take(5).forEach { p ->
            livePlaces +=
              GroundedPlacePin(
                title = p.name.ifBlank { p.displayLabel().substringBefore(",") },
                subtitle = p.displayLabel(),
                url = "https://www.google.com/maps/search/?api=1&query=${p.lat},${p.lng}",
                lat = p.lat,
                lng = p.lng,
              )
          }
        }
      }

      val coordCtx =
        if (lat != 0.0 || lng != 0.0) {
          "User GPS: ${"%.5f".format(lat)}, ${"%.5f".format(lng)} ($locationLabel)."
        } else {
          "User location: $locationLabel."
        }
      val placeSummary =
        if (livePlaces.isNotEmpty()) {
          "\nVerified local map matches:\n" + livePlaces.joinToString("\n") { "- ${it.title}: ${it.subtitle}" }
        } else ""

      val prompt =
        """
        You are the Rural ERides GO Live Trail, Charging & Parts Scout.
        $coordCtx$placeSummary
        User query: "$query"
        Provide accurate, up-to-date, practical rural PEV guidance including trail conditions, local charging/repair stops, store availability, or state e-bike/PEV rules as relevant.
        """
          .trimIndent()

      var usedSearch = false
      val webSources = mutableListOf<Pair<String, String>>()
      val response =
        AppCheckHealthManager.runWithAppCheckRecovery(STUDIO_TAG) {
          withTimeout(60_000L) {
            if (useGoogleSearch) {
              try {
                usedSearch = true
                AppCheckHealthManager.createGenerativeModel(
                  modelName = GeminiModelCatalog.GEMINI_3_8_FLASH,
                  tools = listOf(Tool.googleSearch()),
                ).generateContent(prompt)
              } catch (e: CancellationException) {
                throw e
              } catch (toolErr: Exception) {
                if (AppCheckHealthManager.isAppCheckError(toolErr)) throw toolErr
                Log.w(STUDIO_TAG, "Google Search tool fallback: ${toolErr.message}")
                usedSearch = false
                runCatching {
                  AppCheckHealthManager.createGenerativeModel(GeminiModelCatalog.GEMINI_3_8_FLASH).generateContent(prompt)
                }.getOrElse {
                  AppCheckHealthManager.createGenerativeModel(GeminiModelCatalog.GEMINI_3_5_FLASH_LITE).generateContent(prompt)
                }
              }
            } else {
              runCatching {
                AppCheckHealthManager.createGenerativeModel(GeminiModelCatalog.GEMINI_3_8_FLASH).generateContent(prompt)
              }.getOrElse {
                AppCheckHealthManager.createGenerativeModel(GeminiModelCatalog.GEMINI_3_5_FLASH_LITE).generateContent(prompt)
              }
            }
          }
        }

      runCatching {
        val grounding = response.candidates.firstOrNull()?.groundingMetadata
        grounding?.groundingChunks?.forEach { chunk ->
          val web = chunk.web
          if (web != null && !web.uri.isNullOrBlank()) {
            webSources += (web.title?.ifBlank { web.uri } ?: web.uri.orEmpty()) to web.uri.orEmpty()
          }
        }
      }

      val answer = response.text?.trim().orEmpty().ifBlank { "No grounded information returned for \"$query\"." }
      Result.success(
        GroundedExploreResult(
          answer = answer,
          places = livePlaces,
          webSources = webSources.distinctBy { it.second }.take(6),
          usedSearchTool = usedSearch,
        )
      )
    } catch (e: TimeoutCancellationException) {
      Log.e(STUDIO_TAG, "Grounded explore timed out", e)
      Result.failure(Exception("Search & Maps query timed out after 60 seconds."))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(STUDIO_TAG, "Grounded explore failed", e)
      Result.failure(e)
    }

  suspend fun composeTrailSoundtrack(
    vibePrompt: String,
    genre: String,
  ): Result<AiComposedTrack> {
    val schema =
      Schema.obj(
        properties =
          mapOf(
            "title" to Schema.string("Original rural PEV trail soundtrack title"),
            "bpm" to Schema.integer("Tempo in BPM between 84 and 148"),
            "musicalKey" to Schema.string("Key signature like D Major or A Minor"),
            "chords" to Schema.array(Schema.string(), "4 chord names e.g. D, A, Bm, G"),
            "leadNotesMidi" to Schema.array(Schema.integer(), "16 MIDI note numbers between 55 and 84 for the lead melody"),
            "bassNotesMidi" to Schema.array(Schema.integer(), "8 MIDI note numbers between 36 and 52 for the bassline"),
            "vibeSummary" to Schema.string("1-sentence production description"),
            "lyricHook" to Schema.string("Short 2-line rural electric riding chorus hook"),
          )
      )
    val prompt =
      """
      Compose an original playable instrumental trail music arrangement for an electric PEV rider.
      Genre: $genre
      Prompt / Ride Vibe: "$vibePrompt"
      Provide musical, harmonious, pentatonic/diatonic MIDI notes (16 lead notes 55..84, 8 bass notes 36..52) that sound great played together in a 4-bar loop!
      """
        .trimIndent()

    var lastError: Exception? = null
    for (modelId in listOf(GeminiModelCatalog.GEMINI_3_8_FLASH, GeminiModelCatalog.GEMINI_3_5_FLASH_LITE)) {
      try {
        val model =
          AppCheckHealthManager.createGenerativeModel(
            modelName = modelId,
            generationConfig =
              generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
                temperature = 0.85f
              },
          )
        val response =
          AppCheckHealthManager.runWithAppCheckRecovery(STUDIO_TAG) {
            withTimeout(50_000L) { model.generateContent(prompt) }
          }
        val raw = response.text ?: error("Empty music composition response.")
        val json = JSONObject(raw)

        fun readIntArray(key: String, min: Int, max: Int, fallback: List<Int>): List<Int> {
          val arr = json.optJSONArray(key) ?: return fallback
          val out = (0 until arr.length()).mapNotNull { i -> arr.optInt(i, -1).takeIf { it in min..max } }
          return if (out.size >= 4) out else fallback
        }

        val chordsArr = json.optJSONArray("chords")
        val chords =
          if (chordsArr != null && chordsArr.length() > 0) {
            (0 until chordsArr.length()).map { chordsArr.optString(it, "G") }
          } else listOf("G", "D", "Em", "C")

        return Result.success(
          AiComposedTrack(
            title = json.optString("title", "Backroad Volt Groove").ifBlank { "Backroad Volt Groove" },
            genre = genre,
            bpm = json.optInt("bpm", 112).coerceIn(80, 152),
            musicalKey = json.optString("musicalKey", "G Major"),
            chords = chords,
            leadNotesMidi = readIntArray("leadNotesMidi", 50, 88, listOf(67, 69, 71, 74, 71, 69, 67, 64, 67, 71, 74, 76, 74, 71, 69, 67)),
            bassNotesMidi = readIntArray("bassNotesMidi", 32, 55, listOf(43, 43, 38, 38, 40, 40, 36, 38)),
            vibeSummary = json.optString("vibeSummary", "Original rural trail groove synthesized at 44.1kHz."),
            lyricHook = json.optString("lyricHook", "Fifty-four volts down the gravel lane, quiet torque in the sunset rain."),
          )
        )
      } catch (e: TimeoutCancellationException) {
        Log.e(STUDIO_TAG, "Soundtrack composition timed out on $modelId", e)
        lastError = Exception("Music composition timed out after 50 seconds.")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(STUDIO_TAG, "Soundtrack composition failed on $modelId: ${e.message}")
        lastError = e
      }
    }
    return Result.failure(lastError ?: Exception("Music composition failed."))
  }
}

