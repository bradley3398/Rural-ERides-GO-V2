package com.example.core

import android.util.Log
import com.example.BuildConfig
import java.util.concurrent.ConcurrentHashMap

private const val KEY_RING_TAG = "ApiKeyRing"

class ApiKeyFailoverException(
  val httpCode: Int,
  message: String,
) : IllegalStateException(message)

/**
 * Central API Key validation & cooldown guard for single primary per-service keys.
 */
object ApiKeyRing {
  private const val COOLDOWN_MS = 5L * 60L * 1000L
  private val keyCooldownUntilMs = ConcurrentHashMap<String, Long>()

  fun isRealKey(raw: String?): Boolean {
    val v = raw?.trim().orEmpty()
    if (v.length < 8) return false
    if (v.startsWith("MY_", ignoreCase = true)) return false
    if (v.startsWith("YOUR_", ignoreCase = true)) return false
    if (v.startsWith("REPLACE_", ignoreCase = true)) return false
    if (v.contains("PLACEHOLDER", ignoreCase = true)) return false
    return true
  }

  fun collectPool(vararg rawSlots: String): List<String> =
    rawSlots
      .map { it.trim() }
      .filter { isRealKey(it) }
      .distinct()

  fun orderedKeys(service: String, pool: List<String>): List<String> = pool

  fun markKeyThrottled(service: String, key: String, httpCode: Int = 429) {
    keyCooldownUntilMs[cooldownId(service, key)] = System.currentTimeMillis() + COOLDOWN_MS
    Log.w(KEY_RING_TAG, "[$service] API key throttled/rejected (HTTP $httpCode); cooling down for 5m.")
  }

  fun markKeyHealthy(service: String, key: String) {
    keyCooldownUntilMs.remove(cooldownId(service, key))
  }

  fun isFailoverCode(httpCode: Int): Boolean =
    httpCode == 429 || httpCode == 403 || httpCode == 401 || httpCode == 402

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
    require(pool.isNotEmpty()) { "No API key configured for $service" }
    val key = pool.first()
    try {
      val result = block(key)
      markKeyHealthy(service, key)
      return result
    } catch (e: ApiKeyFailoverException) {
      markKeyThrottled(service, key, e.httpCode)
      throw e
    } catch (t: Throwable) {
      val msg = t.message.orEmpty()
      if (msg.contains("429") || msg.contains("403") || msg.contains("401")) {
        markKeyThrottled(service, key, if (msg.contains("429")) 429 else 403)
      }
      throw t
    }
  }

  fun <T> executeWithCseFailover(
    pairs: List<Pair<String, String>>,
    block: (apiKey: String, cx: String) -> T,
  ): T {
    require(pairs.isNotEmpty()) { "No Google CSE key configured" }
    val (key, cx) = pairs.first()
    try {
      val result = block(key, cx)
      markKeyHealthy("GOOGLE_CSE", key)
      return result
    } catch (e: ApiKeyFailoverException) {
      markKeyThrottled("GOOGLE_CSE", key, e.httpCode)
      throw e
    } catch (t: Throwable) {
      val msg = t.message.orEmpty()
      if (msg.contains("429") || msg.contains("403") || msg.contains("401")) {
        markKeyThrottled("GOOGLE_CSE", key, if (msg.contains("429")) 429 else 403)
      }
      throw t
    }
  }

  // Public release safety: private third-party API keys are intentionally excluded from
  // the Android client APK/BuildConfig so they cannot be extracted by decompilation.
  // Live features operate via keyless public endpoints + Firebase AI Logic + App Check + Firestore.
  fun youtubeKeys(): List<String> = emptyList()

  fun mapboxTokens(): List<String> = emptyList()

  fun openRouteServiceKeys(): List<String> = emptyList()

  fun tomTomKeys(): List<String> = emptyList()

  fun openChargeMapKeys(): List<String> = emptyList()

  fun elevationKeys(): List<String> = emptyList()

  fun weatherKeys(): List<String> = emptyList()

  fun tavilyKeys(): List<String> = emptyList()

  fun braveKeys(): List<String> = emptyList()

  fun newsKeys(): List<String> = emptyList()

  fun imgbbKeys(): List<String> = emptyList()

  fun googleCsePairs(): List<Pair<String, String>> = emptyList()

  private fun cooldownId(service: String, key: String): String = "$service:${key.hashCode()}"
}
