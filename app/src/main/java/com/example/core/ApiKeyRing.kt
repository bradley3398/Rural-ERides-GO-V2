package com.example.core

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import java.util.concurrent.ConcurrentHashMap

private const val KEY_RING_TAG = "ApiKeyRing"

class ApiKeyFailoverException(
  val httpCode: Int,
  message: String,
) : IllegalStateException(message)

/**
 * Central API Key failover ring & secret redaction guard.
 * Reads configured secrets from BuildConfig and user-configured runtime keys.
 */
object ApiKeyRing {
  private const val COOLDOWN_MS = 5L * 60L * 1000L
  private val keyCooldownUntilMs = ConcurrentHashMap<String, Long>()
  private val urlSecretParamRegex =
    Regex("""(?i)([?&](?:key|api_key|apikey|access_token|appid|token)=)([^&\s]+)""")
  private val googleKeyPatternRegex =
    Regex("""\b(?:AIza[0-9A-Za-z_-]{20,}|AQ\.[0-9A-Za-z._-]{20,})\b""")

  @Volatile private var runtimeGeminiKey: String = ""

  fun init(context: Context) {
    if (runtimeGeminiKey.isBlank()) {
      runCatching {
        val prefs =
          context.applicationContext.getSharedPreferences(
            "rural_erides_runtime_secrets",
            Context.MODE_PRIVATE,
          )
        val saved = prefs.getString("runtime_gemini_api_key", null)?.trim().orEmpty()
        if (isRealKey(saved)) {
          runtimeGeminiKey = saved
          Log.i(KEY_RING_TAG, "Loaded user runtime Gemini API key (${saved.take(6)}...${saved.takeLast(4)})")
        }
      }
    }
  }

  fun sanitizeKeyInput(raw: String): String {
    var v = raw.trim().trim('"', '\'')
    val prefixes = listOf("GEMINI_API_KEY_2=", "GEMINI_API_KEY=", "GOOGLE_CLOUD_API_KEY=", "GOOGLE_API_KEY=")
    for (p in prefixes) {
      if (v.startsWith(p, ignoreCase = true)) {
        v = v.substring(p.length).trim().trim('"', '\'')
        break
      }
    }
    return v
  }

  fun setRuntimeGeminiKey(context: Context?, key: String) {
    val clean = sanitizeKeyInput(key)
    runtimeGeminiKey = clean
    context?.let { ctx ->
      runCatching {
        ctx.applicationContext
          .getSharedPreferences("rural_erides_runtime_secrets", Context.MODE_PRIVATE)
          .edit()
          .putString("runtime_gemini_api_key", clean)
          .apply()
      }
    }
    if (clean.isNotBlank()) {
      markKeyHealthy("GEMINI_CLOUD_CONSOLE", clean)
    }
  }

  fun getRuntimeGeminiKey(): String = runtimeGeminiKey

  fun getMaskedGeminiKey(): String {
    val active = geminiKeys().firstOrNull() ?: return "No key configured"
    return if (active.length > 8) "${active.take(6)}...${active.takeLast(4)}" else "***"
  }

  fun isRealKey(raw: String?): Boolean {
    val v = raw?.trim().orEmpty()
    if (v.length < 8) return false
    if (v.startsWith("MY_", ignoreCase = true)) return false
    if (v.startsWith("YOUR_", ignoreCase = true)) return false
    if (v.startsWith("REPLACE_", ignoreCase = true)) return false
    if (v.contains("PLACEHOLDER", ignoreCase = true)) return false
    if (v.equals("DEFAULT_API_KEY", ignoreCase = true)) return false
    if (v.equals("null", ignoreCase = true)) return false
    return true
  }

  fun collectPool(vararg rawSlots: String): List<String> =
    rawSlots
      .map { it.trim() }
      .filter { isRealKey(it) }
      .distinct()

  fun orderedKeys(service: String, pool: List<String>): List<String> {
    if (pool.size <= 1) return pool
    val now = System.currentTimeMillis()
    val healthy = pool.filter { key -> now >= (keyCooldownUntilMs[cooldownId(service, key)] ?: 0L) }
    val cooling = pool.filter { key -> now < (keyCooldownUntilMs[cooldownId(service, key)] ?: 0L) }
    return healthy + cooling
  }

  fun markKeyThrottled(service: String, key: String, httpCode: Int = 429) {
    keyCooldownUntilMs[cooldownId(service, key)] = System.currentTimeMillis() + COOLDOWN_MS
    Log.w(KEY_RING_TAG, "[$service] API key slot throttled/rejected (HTTP $httpCode); cooling down for 5m.")
  }

  fun markKeyHealthy(service: String, key: String) {
    keyCooldownUntilMs.remove(cooldownId(service, key))
  }

  fun isFailoverCode(httpCode: Int): Boolean =
    httpCode == 429 || httpCode == 403 || httpCode == 401 || httpCode == 402 || httpCode == 400

  fun hasHealthyKey(service: String, pool: List<String>): Boolean {
    if (pool.isEmpty()) return false
    val now = System.currentTimeMillis()
    return pool.any { key -> now >= (keyCooldownUntilMs[cooldownId(service, key)] ?: 0L) }
  }

  fun <T> executeWithFailover(
    service: String,
    pool: List<String>,
    block: (apiKey: String) -> T,
  ): T {
    val candidates = orderedKeys(service, pool)
    require(candidates.isNotEmpty()) { "No API key configured for $service" }
    var lastError: Throwable? = null
    for (key in candidates) {
      try {
        val result = block(key)
        markKeyHealthy(service, key)
        return result
      } catch (e: ApiKeyFailoverException) {
        markKeyThrottled(service, key, e.httpCode)
        lastError = e
      } catch (t: Throwable) {
        val msg = t.message.orEmpty()
        if (msg.contains("429") || msg.contains("403") || msg.contains("401")) {
          markKeyThrottled(service, key, if (msg.contains("429")) 429 else 403)
        }
        lastError = t
      }
    }
    throw lastError ?: IllegalStateException("All configured API keys failed for $service")
  }

  fun <T> executeWithCseFailover(
    pairs: List<Pair<String, String>>,
    block: (apiKey: String, cx: String) -> T,
  ): T {
    require(pairs.isNotEmpty()) { "No Google CSE key configured" }
    val now = System.currentTimeMillis()
    val ordered =
      pairs.sortedBy { (key, _) ->
        if (now >= (keyCooldownUntilMs[cooldownId("GOOGLE_CSE", key)] ?: 0L)) 0 else 1
      }
    var lastError: Throwable? = null
    for ((key, cx) in ordered) {
      try {
        val result = block(key, cx)
        markKeyHealthy("GOOGLE_CSE", key)
        return result
      } catch (e: ApiKeyFailoverException) {
        markKeyThrottled("GOOGLE_CSE", key, e.httpCode)
        lastError = e
      } catch (t: Throwable) {
        val msg = t.message.orEmpty()
        if (msg.contains("429") || msg.contains("403") || msg.contains("401")) {
          markKeyThrottled("GOOGLE_CSE", key, if (msg.contains("429")) 429 else 403)
        }
        lastError = t
      }
    }
    throw lastError ?: IllegalStateException("All configured Google CSE keys failed")
  }

  fun geminiKeys(): List<String> =
    collectPool(
      runtimeGeminiKey,
      BuildConfig.GEMINI_API_KEY_2,
      BuildConfig.GEMINI_API_KEY,
      BuildConfig.GOOGLE_CLOUD_API_KEY,
      BuildConfig.GOOGLE_API_KEY,
    )

  fun googleMapsKeys(): List<String> =
    collectPool(
      BuildConfig.GOOGLE_MAPS_API_KEY,
      BuildConfig.MAPS_API_KEY,
      BuildConfig.GOOGLE_CLOUD_API_KEY,
      BuildConfig.GOOGLE_API_KEY,
    )

  fun youtubeKeys(): List<String> =
    collectPool(
      BuildConfig.YOUTUBE_API_KEY,
      BuildConfig.YOUTUBE_API_KEY_2,
      BuildConfig.YOUTUBE_API_KEY_3,
      BuildConfig.GOOGLE_CLOUD_API_KEY,
      BuildConfig.GOOGLE_API_KEY,
    )

  fun mapboxTokens(): List<String> =
    collectPool(
      BuildConfig.MAPBOX_ACCESS_TOKEN,
      BuildConfig.MAPBOX_ACCESS_TOKEN_2,
    )

  fun openRouteServiceKeys(): List<String> =
    collectPool(
      BuildConfig.OPENROUTESERVICE_API_KEY,
      BuildConfig.OPENROUTESERVICE_API_KEY_2,
    )

  fun tomTomKeys(): List<String> =
    collectPool(
      BuildConfig.TOMTOM_API_KEY,
      BuildConfig.TOMTOM_API_KEY_2,
    )

  fun openChargeMapKeys(): List<String> =
    collectPool(
      BuildConfig.OPENCHARGEMAP_API_KEY,
      BuildConfig.OPENCHARGEMAP_API_KEY_2,
    )

  fun elevationKeys(): List<String> =
    collectPool(
      BuildConfig.ELEVATION_API_KEY,
      BuildConfig.GOOGLE_MAPS_API_KEY,
      BuildConfig.MAPS_API_KEY,
      BuildConfig.GOOGLE_CLOUD_API_KEY,
      BuildConfig.GOOGLE_API_KEY,
    )

  fun weatherKeys(): List<String> =
    collectPool(
      BuildConfig.OPENWEATHER_API_KEY,
      BuildConfig.OPENWEATHER_API_KEY_2,
      BuildConfig.WEATHER_API_KEY,
    )

  fun tavilyKeys(): List<String> =
    collectPool(
      BuildConfig.TAVILY_API_KEY,
      BuildConfig.TAVILY_API_KEY_2,
    )

  fun braveKeys(): List<String> =
    collectPool(
      BuildConfig.BRAVE_API_KEY,
      BuildConfig.BRAVE_SEARCH_API_KEY,
      BuildConfig.BRAVE_API_KEY_2,
    )

  fun newsKeys(): List<String> =
    collectPool(
      BuildConfig.NEWS_API_KEY,
      BuildConfig.NEWS_API_KEY_2,
    )

  fun imgbbKeys(): List<String> =
    collectPool(
      BuildConfig.IMGBB_API_KEY,
      BuildConfig.IMGBB_API_KEY_2,
    )

  fun googleCsePairs(): List<Pair<String, String>> {
    val result = mutableListOf<Pair<String, String>>()
    val primaryKeys =
      collectPool(
        BuildConfig.GOOGLE_CSE_API_KEY,
        BuildConfig.GOOGLE_CLOUD_API_KEY,
        BuildConfig.GOOGLE_API_KEY,
      )
    if (isRealKey(BuildConfig.GOOGLE_CSE_CX)) {
      val cx1 = BuildConfig.GOOGLE_CSE_CX.trim()
      for (k in primaryKeys) {
        result += (k to cx1)
      }
    }
    if (isRealKey(BuildConfig.GOOGLE_CSE_API_KEY_2) && isRealKey(BuildConfig.GOOGLE_CSE_CX_2)) {
      result += (BuildConfig.GOOGLE_CSE_API_KEY_2.trim() to BuildConfig.GOOGLE_CSE_CX_2.trim())
    }
    return result.distinct()
  }

  /**
   * Scrubs any query-string secret parameters or raw configured secret values from logs or UI messages.
   */
  fun redactSecrets(raw: String?): String {
    var text = raw.orEmpty()
    if (text.isBlank()) return ""
    text = text.replace(urlSecretParamRegex, "$1[HIDDEN]")
    text = text.replace(googleKeyPatternRegex, "[HIDDEN]")
    val allSecrets =
      buildList {
        addAll(geminiKeys())
        addAll(googleMapsKeys())
        addAll(youtubeKeys())
        addAll(mapboxTokens())
        addAll(openRouteServiceKeys())
        addAll(tomTomKeys())
        addAll(openChargeMapKeys())
        addAll(elevationKeys())
        addAll(weatherKeys())
        addAll(tavilyKeys())
        addAll(braveKeys())
        addAll(newsKeys())
        addAll(imgbbKeys())
        googleCsePairs().forEach { (k, cx) ->
          add(k)
          add(cx)
        }
        if (isRealKey(BuildConfig.SUPABASE_ANON_KEY)) add(BuildConfig.SUPABASE_ANON_KEY.trim())
      }.distinct()
    for (secret in allSecrets) {
      if (secret.length >= 8 && text.contains(secret)) {
        text = text.replace(secret, "[HIDDEN]")
      }
    }
    return text
  }

  private fun cooldownId(service: String, key: String): String = "$service:${key.hashCode()}"
}
