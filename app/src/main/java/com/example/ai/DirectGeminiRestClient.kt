package com.example.ai

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Base64
import android.util.Log
import com.example.core.ApiKeyFailoverException
import com.example.core.ApiKeyRing
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

private const val REST_TAG = "DirectGeminiRestClient"
private const val GEMINI_V1BETA_BASE = "https://generativelanguage.googleapis.com/v1beta/models"
private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

data class GeminiChatTurnPayload(
  val role: String,
  val text: String,
  val images: List<Bitmap> = emptyList(),
  val wavAudio: ByteArray? = null,
)

data class GeminiRestResult(
  val text: String,
  val inlineImages: List<Bitmap> = emptyList(),
  val inlineAudioBytes: ByteArray? = null,
  val inlineAudioMime: String = "",
  val webSources: List<Pair<String, String>> = emptyList(),
  val modelUsed: String = "",
)

/**
 * Lightweight Gemini REST OpenAPI schema builder matching `generativelanguage.googleapis.com/v1beta` `Schema`.
 */
object GeminiJsonSchema {
  fun obj(
    properties: Map<String, JSONObject>,
    optionalProperties: List<String> = emptyList(),
    description: String? = null,
  ): JSONObject {
    val propsJson = JSONObject()
    val requiredArr = JSONArray()
    val optionalSet = optionalProperties.toSet()
    properties.forEach { (key, schema) ->
      propsJson.put(key, schema)
      if (key !in optionalSet) {
        requiredArr.put(key)
      }
    }
    return JSONObject().apply {
      put("type", "OBJECT")
      put("properties", propsJson)
      if (requiredArr.length() > 0) {
        put("required", requiredArr)
      }
      if (!description.isNullOrBlank()) {
        put("description", description)
      }
    }
  }

  fun string(description: String? = null): JSONObject =
    JSONObject().apply {
      put("type", "STRING")
      if (!description.isNullOrBlank()) put("description", description)
    }

  fun integer(description: String? = null): JSONObject =
    JSONObject().apply {
      put("type", "INTEGER")
      if (!description.isNullOrBlank()) put("description", description)
    }

  fun number(description: String? = null): JSONObject =
    JSONObject().apply {
      put("type", "NUMBER")
      if (!description.isNullOrBlank()) put("description", description)
    }

  fun bool(description: String? = null): JSONObject =
    JSONObject().apply {
      put("type", "BOOLEAN")
      if (!description.isNullOrBlank()) put("description", description)
    }

  fun enumeration(
    values: List<String>,
    description: String? = null,
  ): JSONObject =
    JSONObject().apply {
      put("type", "STRING")
      put("enum", JSONArray(values))
      if (!description.isNullOrBlank()) put("description", description)
    }

  fun array(
    items: JSONObject,
    description: String? = null,
  ): JSONObject =
    JSONObject().apply {
      put("type", "ARRAY")
      put("items", items)
      if (!description.isNullOrBlank()) put("description", description)
    }
}

/**
 * Direct Google Cloud Console Gemini REST API v1beta (`generativelanguage.googleapis.com`)
 * client using `OkHttp` + `ApiKeyRing.geminiKeys()` (`BuildConfig.GEMINI_API_KEY`,
 * `BuildConfig.GEMINI_API_KEY_2`, `BuildConfig.GOOGLE_CLOUD_API_KEY`, `BuildConfig.GOOGLE_API_KEY`).
 * Completely bypasses Firebase AI Logic and Firebase App Check.
 */
object DirectGeminiRestClient {
  private val httpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(65, TimeUnit.SECONDS)
      .writeTimeout(30, TimeUnit.SECONDS)
      .build()
  }

  @Volatile private var androidPackageHeader: String = ""
  @Volatile private var androidCertSha1Header: String = ""

  fun initAndroidIdentity(context: Context) {
    ApiKeyRing.init(context)
    if (androidPackageHeader.isNotBlank() && androidCertSha1Header.isNotBlank()) return
    runCatching {
      val appCtx = context.applicationContext
      val pkg = appCtx.packageName
      androidPackageHeader = pkg
      val pm = appCtx.packageManager
      val sigBytes: ByteArray? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
          info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
        } else {
          @Suppress("DEPRECATION")
          val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES)
          @Suppress("DEPRECATION")
          info.signatures?.firstOrNull()?.toByteArray()
        }
      if (sigBytes != null) {
        val digest = MessageDigest.getInstance("SHA-1").digest(sigBytes)
        androidCertSha1Header = digest.joinToString("") { "%02X".format(it) }
      }
    }.onFailure { err ->
      Log.w(REST_TAG, "Android identity header lookup skipped: ${err.message}")
    }
  }

  fun cleanJsonPayload(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return ""
    var cleaned =
      trimmed
        .removePrefix("```json")
        .removePrefix("```JSON")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()
    if (!cleaned.startsWith("{") && !cleaned.startsWith("[")) {
      val objIdx = cleaned.indexOf('{')
      val arrIdx = cleaned.indexOf('[')
      val startIdx =
        when {
          objIdx >= 0 && arrIdx >= 0 -> minOf(objIdx, arrIdx)
          objIdx >= 0 -> objIdx
          arrIdx >= 0 -> arrIdx
          else -> -1
        }
      if (startIdx >= 0) {
        val endChar = if (cleaned[startIdx] == '{') '}' else ']'
        val endIdx = cleaned.lastIndexOf(endChar)
        if (endIdx > startIdx) {
          cleaned = cleaned.substring(startIdx, endIdx + 1)
        }
      }
    }
    return cleaned.trim()
  }

  fun buildRequestJson(
    turns: List<GeminiChatTurnPayload>,
    systemInstruction: String? = null,
    temperature: Float? = null,
    responseMimeType: String? = null,
    responseSchema: JSONObject? = null,
    useGoogleSearch: Boolean = false,
    responseModalities: List<String>? = null,
    imageAspectRatio: String? = null,
    imageSize: String? = null,
    ttsVoiceName: String? = null,
    ttsLanguageCode: String? = null,
  ): JSONObject {
    val contentsArray = JSONArray()
    turns.forEach { turn ->
      val partsArray = JSONArray()
      if (turn.text.isNotBlank()) {
        partsArray.put(JSONObject().put("text", turn.text))
      }
      turn.images.forEach { bmp ->
        encodeBitmapJpegBase64(bmp)?.let { b64 ->
          partsArray.put(
            JSONObject().put(
              "inlineData",
              JSONObject()
                .put("mimeType", "image/jpeg")
                .put("data", b64),
            ),
          )
        }
      }
      val wav = turn.wavAudio
      if (wav != null && wav.isNotEmpty()) {
        partsArray.put(
          JSONObject().put(
            "inlineData",
            JSONObject()
              .put("mimeType", "audio/wav")
              .put("data", Base64.encodeToString(wav, Base64.NO_WRAP)),
          ),
        )
      }
      if (partsArray.length() > 0) {
        contentsArray.put(
          JSONObject()
            .put("role", if (turn.role == "model") "model" else "user")
            .put("parts", partsArray),
        )
      }
    }

    val root = JSONObject().put("contents", contentsArray)

    if (!systemInstruction.isNullOrBlank()) {
      root.put(
        "systemInstruction",
        JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction))),
      )
    }

    if (useGoogleSearch) {
      root.put("tools", JSONArray().put(JSONObject().put("googleSearch", JSONObject())))
    }

    val genConfig = JSONObject()
    if (temperature != null) {
      genConfig.put("temperature", temperature.toDouble())
    }
    if (!responseMimeType.isNullOrBlank()) {
      genConfig.put("responseMimeType", responseMimeType)
    }
    if (responseSchema != null) {
      genConfig.put("responseSchema", responseSchema)
    }
    if (!responseModalities.isNullOrEmpty()) {
      genConfig.put("responseModalities", JSONArray(responseModalities))
    }
    if (!imageAspectRatio.isNullOrBlank() || !imageSize.isNullOrBlank()) {
      val imgCfg = JSONObject()
      if (!imageAspectRatio.isNullOrBlank()) imgCfg.put("aspectRatio", imageAspectRatio)
      if (!imageSize.isNullOrBlank()) imgCfg.put("imageSize", imageSize)
      genConfig.put("imageConfig", imgCfg)
    }
    if (!ttsVoiceName.isNullOrBlank()) {
      val voiceConfig =
        JSONObject().put(
          "prebuiltVoiceConfig",
          JSONObject().put("voiceName", ttsVoiceName),
        )
      val speechConfig = JSONObject().put("voiceConfig", voiceConfig)
      if (!ttsLanguageCode.isNullOrBlank()) {
        speechConfig.put("languageCode", ttsLanguageCode)
      }
      genConfig.put("speechConfig", speechConfig)
    }
    if (genConfig.length() > 0) {
      root.put("generationConfig", genConfig)
    }

    return root
  }

  suspend fun generateText(
    modelId: String,
    prompt: String,
    images: List<Bitmap> = emptyList(),
    wavAudio: ByteArray? = null,
    systemInstruction: String? = null,
    temperature: Float? = null,
    responseMimeType: String? = null,
    responseSchema: JSONObject? = null,
    useGoogleSearch: Boolean = false,
  ): GeminiRestResult =
    generateContent(
      modelId = modelId,
      turns = listOf(GeminiChatTurnPayload(role = "user", text = prompt, images = images, wavAudio = wavAudio)),
      systemInstruction = systemInstruction,
      temperature = temperature,
      responseMimeType = responseMimeType,
      responseSchema = responseSchema,
      useGoogleSearch = useGoogleSearch,
    )

  suspend fun generateContent(
    modelId: String,
    turns: List<GeminiChatTurnPayload>,
    systemInstruction: String? = null,
    temperature: Float? = null,
    responseMimeType: String? = null,
    responseSchema: JSONObject? = null,
    useGoogleSearch: Boolean = false,
    responseModalities: List<String>? = null,
    imageAspectRatio: String? = null,
    imageSize: String? = null,
    ttsVoiceName: String? = null,
    ttsLanguageCode: String? = null,
  ): GeminiRestResult =
    withContext(Dispatchers.IO) {
      val keys = ApiKeyRing.geminiKeys()
      if (keys.isEmpty()) {
        throw IllegalStateException(
          "No Cloud Console Gemini API key configured. Add GEMINI_API_KEY (or GOOGLE_CLOUD_API_KEY) in the AI Studio Secrets panel and rebuild.",
        )
      }
      val normalizedModel = normalizeModelId(modelId)
      val requestJson =
        buildRequestJson(
          turns = turns,
          systemInstruction = systemInstruction,
          temperature = temperature,
          responseMimeType = responseMimeType,
          responseSchema = responseSchema,
          useGoogleSearch = useGoogleSearch,
          responseModalities = responseModalities,
          imageAspectRatio = imageAspectRatio,
          imageSize = imageSize,
          ttsVoiceName = ttsVoiceName,
          ttsLanguageCode = ttsLanguageCode,
        )
      val url = "$GEMINI_V1BETA_BASE/$normalizedModel:generateContent"

      ApiKeyRing.executeWithFailover("GEMINI_CLOUD_CONSOLE", keys) { apiKey ->
        val req = buildAuthorizedRequest(url, apiKey, requestJson)
        httpClient.newCall(req).execute().use { resp ->
          val bodyStr = resp.body?.string().orEmpty()
          if (!resp.isSuccessful) {
            val cleanErr = parseApiErrorMessage(resp.code, bodyStr)
            if (ApiKeyRing.isFailoverCode(resp.code) || cleanErr.contains("API_KEY", ignoreCase = true)) {
              throw ApiKeyFailoverException(resp.code, cleanErr)
            }
            throw IllegalStateException(cleanErr)
          }
          parseRestResponse(bodyStr, normalizedModel)
        }
      }
    }

  fun streamGenerateContent(
    modelId: String,
    prompt: String,
    images: List<Bitmap> = emptyList(),
    systemInstruction: String? = null,
    temperature: Float? = null,
    useGoogleSearch: Boolean = false,
  ): Flow<String> =
    flow {
      val keys = ApiKeyRing.geminiKeys()
      if (keys.isEmpty()) {
        throw IllegalStateException(
          "No Cloud Console Gemini API key configured. Add GEMINI_API_KEY (or GOOGLE_CLOUD_API_KEY) in the AI Studio Secrets panel and rebuild.",
        )
      }
      val normalizedModel = normalizeModelId(modelId)
      val requestJson =
        buildRequestJson(
          turns = listOf(GeminiChatTurnPayload(role = "user", text = prompt, images = images)),
          systemInstruction = systemInstruction,
          temperature = temperature,
          useGoogleSearch = useGoogleSearch,
        )
      val url = "$GEMINI_V1BETA_BASE/$normalizedModel:streamGenerateContent?alt=sse"
      val orderedKeys = ApiKeyRing.orderedKeys("GEMINI_CLOUD_CONSOLE", keys)
      var lastError: Throwable? = null
      var emittedAny = false

      for (apiKey in orderedKeys) {
        currentCoroutineContext().ensureActive()
        try {
          val req = buildAuthorizedRequest(url, apiKey, requestJson)
          httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
              val errBody = resp.body?.string().orEmpty()
              val cleanErr = parseApiErrorMessage(resp.code, errBody)
              if (ApiKeyRing.isFailoverCode(resp.code) || cleanErr.contains("API_KEY", ignoreCase = true)) {
                ApiKeyRing.markKeyThrottled("GEMINI_CLOUD_CONSOLE", apiKey, resp.code)
              }
              throw IllegalStateException(cleanErr)
            }
            val body = resp.body ?: throw IllegalStateException("Empty streaming response body")
            val source = body.source()
            val nonSseBuffer = StringBuilder()
            while (!source.exhausted()) {
              currentCoroutineContext().ensureActive()
              val line = source.readUtf8Line() ?: break
              val trimmed = line.trim()
              if (trimmed.startsWith("data:")) {
                val payload = trimmed.removePrefix("data:").trim()
                if (payload.isBlank() || payload == "[DONE]") continue
                val chunkText = extractTextFromResponseJson(payload)
                if (chunkText.isNotEmpty()) {
                  emittedAny = true
                  emit(chunkText)
                }
              } else if (trimmed.isNotEmpty() && !trimmed.startsWith(":")) {
                nonSseBuffer.append(trimmed)
              }
            }
            if (!emittedAny && nonSseBuffer.isNotBlank()) {
              val fallbackText = parseNonSseFallback(nonSseBuffer.toString())
              if (fallbackText.isNotEmpty()) {
                emittedAny = true
                emit(fallbackText)
              }
            }
            ApiKeyRing.markKeyHealthy("GEMINI_CLOUD_CONSOLE", apiKey)
          }
          if (emittedAny) return@flow
        } catch (t: Throwable) {
          if (t is kotlinx.coroutines.CancellationException) throw t
          lastError = t
          if (emittedAny) throw t
        }
      }

      if (!emittedAny) {
        throw lastError ?: IllegalStateException("All configured Gemini API keys failed during streaming.")
      }
    }.flowOn(Dispatchers.IO)

  suspend fun probeLiveCloudConsoleKey(
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
  ): Triple<Boolean, Long, String> =
    withContext(Dispatchers.IO) {
      val keys = ApiKeyRing.geminiKeys()
      if (keys.isEmpty()) {
        return@withContext Triple(
          false,
          0L,
          "No Cloud Console Gemini API key set in Secrets (add GEMINI_API_KEY or GOOGLE_CLOUD_API_KEY)",
        )
      }
      val start = System.currentTimeMillis()
      val normalizedModel = normalizeModelId(modelId)
      try {
        val result =
          generateText(
            modelId = normalizedModel,
            prompt = "Reply with OK.",
            temperature = 0.0f,
          )
        val ms = (System.currentTimeMillis() - start).coerceAtLeast(1L)
        val reply = result.text.trim().ifBlank { "OK" }
        Triple(true, ms, "$normalizedModel ready (${keys.size} key slot${if (keys.size == 1) "" else "s"}): ${reply.take(40)}")
      } catch (e: Exception) {
        val ms = (System.currentTimeMillis() - start).coerceAtLeast(1L)
        val clean = ApiKeyRing.redactSecrets(e.message ?: "Cloud Console key check failed")
        Triple(false, ms, clean.take(160))
      }
    }

  private fun normalizeModelId(raw: String): String =
    raw.trim().removePrefix("models/").ifBlank { GeminiModelCatalog.GEMINI_3_1_FLASH_LITE }

  private fun buildAuthorizedRequest(
    url: String,
    apiKey: String,
    bodyJson: JSONObject,
  ): Request {
    val builder =
      Request.Builder()
        .url(url)
        .addHeader("x-goog-api-key", apiKey)
        .addHeader("Content-Type", "application/json")
        .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
    if (androidPackageHeader.isNotBlank()) {
      builder.addHeader("X-Android-Package", androidPackageHeader)
    }
    if (androidCertSha1Header.isNotBlank()) {
      builder.addHeader("X-Android-Cert", androidCertSha1Header)
    }
    return builder.build()
  }

  private fun parseApiErrorMessage(httpCode: Int, rawBody: String): String {
    val parsedMsg =
      runCatching {
        val root = JSONObject(rawBody)
        val err = root.optJSONObject("error")
        val msg = err?.optString("message").orEmpty()
        val status = err?.optString("status").orEmpty()
        listOf(status, msg).filter { it.isNotBlank() }.joinToString(": ")
      }.getOrDefault("")
    val clean = ApiKeyRing.redactSecrets(parsedMsg.ifBlank { rawBody.take(260) })
    return "Cloud Console API ($httpCode): ${clean.ifBlank { "HTTP error $httpCode" }}"
  }

  private fun parseRestResponse(rawJson: String, modelUsed: String): GeminiRestResult {
    val root = JSONObject(rawJson)
    val blockReason =
      root.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty()
    val candidates = root.optJSONArray("candidates")
    if (candidates == null || candidates.length() == 0) {
      if (blockReason.isNotBlank()) {
        throw IllegalStateException("Safety filter blocked request ($blockReason)")
      }
      return GeminiRestResult(text = "", modelUsed = modelUsed)
    }

    val first = candidates.getJSONObject(0)
    val finishReason = first.optString("finishReason")
    if (finishReason == "SAFETY" || finishReason == "PROHIBITED_CONTENT") {
      val existingParts = first.optJSONObject("content")?.optJSONArray("parts")
      if (existingParts == null || existingParts.length() == 0) {
        throw IllegalStateException("Response blocked by safety policy ($finishReason)")
      }
    }

    val parts = first.optJSONObject("content")?.optJSONArray("parts")
    val textSb = StringBuilder()
    val images = mutableListOf<Bitmap>()
    var audioBytes: ByteArray? = null
    var audioMime = ""

    if (parts != null) {
      for (i in 0 until parts.length()) {
        val part = parts.optJSONObject(i) ?: continue
        val text = part.optString("text")
        if (text.isNotEmpty() && !part.optBoolean("thought", false)) {
          textSb.append(text)
        }
        val inline = part.optJSONObject("inlineData") ?: part.optJSONObject("inline_data")
        if (inline != null) {
          val mime = inline.optString("mimeType").ifBlank { inline.optString("mime_type") }
          val dataB64 = inline.optString("data")
          if (dataB64.isNotBlank()) {
            val decoded = runCatching { Base64.decode(dataB64, Base64.DEFAULT) }.getOrNull()
            if (decoded != null && decoded.isNotEmpty()) {
              if (mime.startsWith("image/", ignoreCase = true)) {
                BitmapFactory.decodeByteArray(decoded, 0, decoded.size)?.let { images += it }
              } else if (mime.startsWith("audio/", ignoreCase = true) && audioBytes == null) {
                audioBytes = decoded
                audioMime = mime
              }
            }
          }
        }
      }
    }

    val webSources = mutableListOf<Pair<String, String>>()
    val chunks = first.optJSONObject("groundingMetadata")?.optJSONArray("groundingChunks")
    if (chunks != null) {
      for (i in 0 until chunks.length()) {
        val web = chunks.optJSONObject(i)?.optJSONObject("web") ?: continue
        val uri = web.optString("uri").trim()
        val title = web.optString("title").trim().ifBlank { uri }
        if (uri.startsWith("http")) {
          webSources += (title to uri)
        }
      }
    }

    return GeminiRestResult(
      text = textSb.toString(),
      inlineImages = images,
      inlineAudioBytes = audioBytes,
      inlineAudioMime = audioMime,
      webSources = webSources,
      modelUsed = modelUsed,
    )
  }

  private fun extractTextFromResponseJson(rawJson: String): String =
    runCatching {
      val root = JSONObject(rawJson)
      val candidates = root.optJSONArray("candidates") ?: return@runCatching ""
      if (candidates.length() == 0) return@runCatching ""
      val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts") ?: return@runCatching ""
      val sb = StringBuilder()
      for (i in 0 until parts.length()) {
        val p = parts.optJSONObject(i) ?: continue
        if (!p.optBoolean("thought", false)) {
          sb.append(p.optString("text"))
        }
      }
      sb.toString()
    }.getOrDefault("")

  private fun parseNonSseFallback(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.startsWith("[")) {
      return runCatching {
        val arr = JSONArray(trimmed)
        buildString {
          for (i in 0 until arr.length()) {
            append(extractTextFromResponseJson(arr.getJSONObject(i).toString()))
          }
        }
      }.getOrDefault("")
    }
    return extractTextFromResponseJson(trimmed)
  }

  private fun encodeBitmapJpegBase64(bitmap: Bitmap): String? {
    if (bitmap.isRecycled) return null
    val maxSide = 1280
    val scale =
      if (bitmap.width > maxSide || bitmap.height > maxSide) {
        maxSide.toFloat() / maxOf(bitmap.width, bitmap.height).toFloat()
      } else {
        1.0f
      }
    val scaled =
      if (scale < 0.99f) {
        Bitmap.createScaledBitmap(
          bitmap,
          (bitmap.width * scale).toInt().coerceAtLeast(1),
          (bitmap.height * scale).toInt().coerceAtLeast(1),
          true,
        )
      } else {
        bitmap
      }
    return try {
      val out = ByteArrayOutputStream()
      scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
      Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    } finally {
      if (scaled !== bitmap && !scaled.isRecycled) {
        runCatching { scaled.recycle() }
      }
    }
  }
}
