package com.example.core

import android.util.Log
import com.example.data.YouTubeVideoItem
import java.io.StringReader
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

internal object YouTubeApiClient {
  private const val TAG = "YouTubeApiClient"
  private val httpClient get() = LiveCloudServices.httpClient

  private data class CachedYoutubeFeed(
    val videos: List<YouTubeVideoItem>,
    val source: String,
    val fetchedAtMs: Long,
  )

  private const val YT_FRESH_CACHE_TTL_MS = 15L * 60L * 1000L
  private const val YT_RATE_LIMIT_COOLDOWN_MS = 5L * 60L * 1000L
  private const val YT_BROWSER_UA =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

  private val youtubeFeedCache = ConcurrentHashMap<String, CachedYoutubeFeed>()
  @Volatile private var youtubeApiV3CooldownUntilMs: Long = 0L
  @Volatile private var youtubeRssCooldownUntilMs: Long = 0L
  @Volatile private var youtubeWebCooldownUntilMs: Long = 0L
  private val youtubeMirrorCooldowns = ConcurrentHashMap<String, Long>()

  private val youtubeMirrorHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(7, TimeUnit.SECONDS)
      .readTimeout(10, TimeUnit.SECONDS)
      .build()
  }

  private fun isRateLimitOrQuotaHttp(code: Int): Boolean =
    code == 429 || code == 403 || code == 503

  private fun getFreshYoutubeCache(cacheKey: String): Pair<List<YouTubeVideoItem>, String>? {
    val cached = youtubeFeedCache[cacheKey] ?: return null
    val age = System.currentTimeMillis() - cached.fetchedAtMs
    return if (age <= YT_FRESH_CACHE_TTL_MS && cached.videos.isNotEmpty()) {
      cached.videos to "${cached.source}_CACHED"
    } else null
  }

  private fun getStaleYoutubeCache(cacheKey: String): Pair<List<YouTubeVideoItem>, String>? {
    val cached = youtubeFeedCache[cacheKey] ?: return null
    return if (cached.videos.isNotEmpty()) cached.videos to "STALE_CACHE_RECOVERY" else null
  }

  private fun putYoutubeCache(cacheKey: String, videos: List<YouTubeVideoItem>, source: String) {
    if (videos.isNotEmpty()) {
      youtubeFeedCache[cacheKey] = CachedYoutubeFeed(videos, source, System.currentTimeMillis())
    }
  }

  suspend fun fetchYouTubeVideos(
    query: String = "",
    channelOnly: Boolean = true,
  ): Result<Pair<List<YouTubeVideoItem>, String>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val cleanQ = query.trim()
        val cacheKey = "yt:${if (channelOnly) "ch" else "all"}:${cleanQ.lowercase()}"
        getFreshYoutubeCache(cacheKey)?.let { return@runCatching it }

        val now = System.currentTimeMillis()
        var lastError: String? = null
        val channelId = LiveCloudServices.OFFICIAL_CHANNEL_ID

        // TIER 1: YouTube Data API v3 with Automatic Redundant Key Pool (_1 -> _2 -> _3) + 1-unit playlistItems
        val ytKeys = ApiKeyRing.youtubeKeys()
        if (ytKeys.isNotEmpty() && ApiKeyRing.hasHealthyKey("YOUTUBE_V3", ytKeys) && now >= youtubeApiV3CooldownUntilMs) {
          runCatching {
            val items =
              ApiKeyRing.executeWithFailover("YOUTUBE_V3", ytKeys) { rawKey ->
                val key = URLEncoder.encode(rawKey, "UTF-8")
                if (channelOnly && cleanQ.isBlank()) {
                  val uploadsPlaylistId =
                    if (channelId.startsWith("UC")) "UU" + channelId.drop(2)
                    else channelId
                  val playlistUrl =
                    "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&playlistId=$uploadsPlaylistId&maxResults=16&key=$key"
                  executeYoutubePlaylistItems(playlistUrl, rawKey)
                } else {
                  val encodedQ = URLEncoder.encode(cleanQ.ifBlank { "PEV electric scooter euc" }, "UTF-8")
                  val url =
                    if (channelOnly) {
                      "https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=$channelId&q=$encodedQ&maxResults=16&order=relevance&type=video&videoEmbeddable=true&key=$key"
                    } else {
                      "https://www.googleapis.com/youtube/v3/search?part=snippet&q=$encodedQ&maxResults=16&order=relevance&type=video&videoEmbeddable=true&key=$key"
                    }
                  executeYoutubeSearch(url, rawKey)
                }
              }
            if (items.isNotEmpty()) {
              putYoutubeCache(cacheKey, items, "LIVE_API_V3")
              return@runCatching items to "LIVE_API_V3"
            }
          }.onFailure { err ->
            lastError = err.message
            if (err.message?.contains("429") == true || err.message?.contains("403") == true) {
              youtubeApiV3CooldownUntilMs = System.currentTimeMillis() + YT_RATE_LIMIT_COOLDOWN_MS
              Log.w(TAG, "All YouTube Data API v3 keys throttled (${err.message}); rotating to keyless live fallback.")
            }
          }
        }

        // TIER 2: Official YouTube Channel Atom RSS Feed (with browser UA & backoff retry on 429)
        if (channelOnly && System.currentTimeMillis() >= youtubeRssCooldownUntilMs) {
          runCatching {
            val rssVideos = fetchYoutubeChannelRssWithRetry()
            val filtered =
              rssVideos.filter {
                cleanQ.isBlank() ||
                  it.title.contains(cleanQ, ignoreCase = true) ||
                  it.description.contains(cleanQ, ignoreCase = true)
              }
            if (filtered.isNotEmpty() || cleanQ.isBlank()) {
              putYoutubeCache(cacheKey, filtered, "CHANNEL_RSS_LIVE")
              return@runCatching filtered to "CHANNEL_RSS_LIVE"
            }
          }.onFailure { err ->
            lastError = err.message
            if (err.message?.contains("429") == true) {
              youtubeRssCooldownUntilMs = System.currentTimeMillis() + YT_RATE_LIMIT_COOLDOWN_MS
            }
          }
        }

        // TIER 3: Live YouTube Web ytInitialData JSON fallback (zero API quota, immune to API v3 429)
        if (System.currentTimeMillis() >= youtubeWebCooldownUntilMs) {
          val webQuery =
            when {
              channelOnly && cleanQ.isBlank() -> ""
              channelOnly -> "Rural ERides $cleanQ"
              else -> cleanQ.ifBlank { "PEV electric scooter euc review" }
            }
          runCatching {
            val webVideos =
              if (channelOnly && cleanQ.isBlank()) {
                fetchYoutubeWebVideos("https://www.youtube.com/channel/$channelId/videos")
              } else {
                val enc = URLEncoder.encode(webQuery, "UTF-8")
                fetchYoutubeWebVideos("https://www.youtube.com/results?search_query=$enc")
              }
            if (webVideos.isNotEmpty()) {
              putYoutubeCache(cacheKey, webVideos, "YOUTUBE_WEB_LIVE")
              return@runCatching webVideos to "YOUTUBE_WEB_LIVE"
            }
          }.onFailure { err ->
            lastError = err.message
            if (err.message?.contains("429") == true) {
              youtubeWebCooldownUntilMs = System.currentTimeMillis() + YT_RATE_LIMIT_COOLDOWN_MS
            }
          }
        }

        // TIER 4: Rotating Public Piped / Invidious Mirrors
        val mirrorQuery =
          when {
            channelOnly && cleanQ.isBlank() -> "Rural ERides"
            channelOnly -> "Rural ERides $cleanQ"
            else -> cleanQ.ifBlank { "PEV electric scooter euc" }
          }
        val mirrorVideos = fetchFromYoutubeMirrors(mirrorQuery)
        if (mirrorVideos.isNotEmpty()) {
          putYoutubeCache(cacheKey, mirrorVideos, "YOUTUBE_MIRROR_LIVE")
          return@runCatching mirrorVideos to "YOUTUBE_MIRROR_LIVE"
        }

        // TIER 5: Stale in-session cache recovery (never break UI on transient 429)
        getStaleYoutubeCache(cacheKey)?.let { return@runCatching it }
        if (channelOnly) {
          getStaleYoutubeCache("yt:ch:")?.let { (cachedVids, src) ->
            val filtered =
              cachedVids.filter {
                cleanQ.isBlank() || it.title.contains(cleanQ, true) || it.description.contains(cleanQ, true)
              }
            if (filtered.isNotEmpty()) return@runCatching filtered to src
          }
        }

        error(
          if (lastError?.contains("429") == true) {
            "YouTube temporarily rate-limited requests (429). Wait a few seconds and tap Refresh."
          } else {
            lastError ?: "YouTube feed temporarily busy. Tap Refresh to retry."
          }
        )
      }
    }

  suspend fun fetchYouTubeMusicMixes(
    category: String = "Synthwave",
    customQuery: String = "",
  ): Result<Pair<List<YouTubeVideoItem>, String>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val searchSeed =
          if (customQuery.isNotBlank()) customQuery.trim()
          else
            when (category.lowercase()) {
              "synthwave" -> "synthwave night drive mix music"
              "lo-fi", "lofi" -> "lofi chill beats ride mix music"
              "rock" -> "electronic rock trail riding mix"
              "hip-hop", "hiphop" -> "instrumental hip hop beats mix"
              "ambient" -> "ambient focus electronic ride soundtrack"
              "drum & bass", "dnb" -> "liquid drum and bass mix music"
              else -> "$category riding music mix"
            }
        val cacheKey = "ytm:${searchSeed.lowercase()}"
        getFreshYoutubeCache(cacheKey)?.let { return@runCatching it }

        var lastError: String? = null

        // TIER 1: YouTube Data API v3 Music Search with Automatic Redundant Key Pool (_1 -> _2 -> _3)
        val ytKeys = ApiKeyRing.youtubeKeys()
        if (ytKeys.isNotEmpty() && ApiKeyRing.hasHealthyKey("YOUTUBE_V3", ytKeys) && System.currentTimeMillis() >= youtubeApiV3CooldownUntilMs) {
          runCatching {
            val tracks =
              ApiKeyRing.executeWithFailover("YOUTUBE_V3", ytKeys) { rawKey ->
                val key = URLEncoder.encode(rawKey, "UTF-8")
                val encodedQ = URLEncoder.encode(searchSeed, "UTF-8")
                val url =
                  "https://www.googleapis.com/youtube/v3/search?part=snippet&q=$encodedQ&type=video&videoCategoryId=10&videoEmbeddable=true&maxResults=15&order=relevance&key=$key"
                executeYoutubeSearch(url, rawKey)
              }
            if (tracks.isNotEmpty()) {
              putYoutubeCache(cacheKey, tracks, "YOUTUBE_MUSIC_V3")
              return@runCatching tracks to "YOUTUBE_MUSIC_V3"
            }
          }.onFailure { err ->
            lastError = err.message
            if (err.message?.contains("429") == true || err.message?.contains("403") == true) {
              youtubeApiV3CooldownUntilMs = System.currentTimeMillis() + YT_RATE_LIMIT_COOLDOWN_MS
              Log.w(TAG, "All YouTube Music v3 keys throttled (${err.message}); rotating to keyless music fallback.")
            }
          }
        }

        // TIER 2: Live YouTube Web ytInitialData Music Search (keyless, immune to v3 429)
        if (System.currentTimeMillis() >= youtubeWebCooldownUntilMs) {
          runCatching {
            val enc = URLEncoder.encode(searchSeed, "UTF-8")
            val webTracks = fetchYoutubeWebVideos("https://www.youtube.com/results?search_query=$enc")
            if (webTracks.isNotEmpty()) {
              putYoutubeCache(cacheKey, webTracks, "YOUTUBE_MUSIC_LIVE")
              return@runCatching webTracks to "YOUTUBE_MUSIC_LIVE"
            }
          }.onFailure { err ->
            lastError = err.message
            if (err.message?.contains("429") == true) {
              youtubeWebCooldownUntilMs = System.currentTimeMillis() + YT_RATE_LIMIT_COOLDOWN_MS
            }
          }
        }

        // TIER 3: Rotating Piped / Invidious Music Search Mirrors
        val mirrorTracks = fetchFromYoutubeMirrors(searchSeed)
        if (mirrorTracks.isNotEmpty()) {
          putYoutubeCache(cacheKey, mirrorTracks, "MUSIC_MIRROR_LIVE")
          return@runCatching mirrorTracks to "MUSIC_MIRROR_LIVE"
        }

        // TIER 4: Stale Cache Recovery
        getStaleYoutubeCache(cacheKey)?.let { return@runCatching it }

        error(
          if (lastError?.contains("429") == true) {
            "YouTube Music temporarily rate-limited (429). Tap another genre or retry in a few seconds."
          } else {
            lastError ?: "Music feed temporarily busy. Tap Search or choose another genre."
          }
        )
      }
    }

  private suspend fun fetchYoutubeChannelRssWithRetry(): List<YouTubeVideoItem> {
    val rssUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=${LiveCloudServices.OFFICIAL_CHANNEL_ID}"
    var lastCode = 0
    for (attempt in 0..1) {
      val req =
        Request.Builder()
          .url(rssUrl)
          .header("User-Agent", YT_BROWSER_UA)
          .header("Accept", "application/atom+xml,application/xml,text/xml;q=0.9,*/*;q=0.8")
          .header("Accept-Language", "en-US,en;q=0.9")
          .get()
          .build()
      val (code, body) =
        httpClient.newCall(req).execute().use { resp ->
          resp.code to (if (resp.isSuccessful) resp.body?.string().orEmpty() else "")
        }
      if (code in 200..299 && body.isNotBlank()) {
        return parseYoutubeAtomFeed(body)
      }
      lastCode = code
      if (code == 429 && attempt == 0) {
        delay(900L)
      } else {
        break
      }
    }
    error("YouTube RSS HTTP $lastCode")
  }

  private fun executeYoutubePlaylistItems(url: String, activeKey: String): List<YouTubeVideoItem> {
    val body =
      httpClient.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
        if (ApiKeyRing.isFailoverCode(resp.code)) {
          throw ApiKeyFailoverException(resp.code, "YouTube Data API v3 HTTP ${resp.code}")
        }
        if (!resp.isSuccessful) error("YouTube Data API v3 HTTP ${resp.code}")
        resp.body?.string().orEmpty()
      }
    val items = JSONObject(body).optJSONArray("items")
    val videoIds = mutableListOf<String>()
    val draftList = mutableListOf<YouTubeVideoItem>()
    if (items != null) {
      for (i in 0 until items.length()) {
        val item = items.optJSONObject(i) ?: continue
        val snip = item.optJSONObject("snippet") ?: continue
        val vid = snip.optJSONObject("resourceId")?.optString("videoId").orEmpty()
        if (vid.isBlank()) continue
        videoIds += vid
        val thumb =
          snip.optJSONObject("thumbnails")?.optJSONObject("high")?.optString("url")
            ?: "https://i.ytimg.com/vi/$vid/hqdefault.jpg"
        draftList +=
          YouTubeVideoItem(
            videoId = vid,
            title = snip.optString("title"),
            description = snip.optString("description"),
            channelTitle = snip.optString("channelTitle").ifBlank { "Rural ERides" },
            publishedAt = snip.optString("publishedAt").take(10),
            thumbnailUrl = thumb,
          )
      }
    }
    return enrichYoutubeStatsBestEffort(draftList, videoIds, activeKey)
  }

  private fun executeYoutubeSearch(url: String, activeKey: String): List<YouTubeVideoItem> {
    val body =
      httpClient.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
        if (ApiKeyRing.isFailoverCode(resp.code)) {
          throw ApiKeyFailoverException(resp.code, "YouTube Data API v3 HTTP ${resp.code}")
        }
        if (!resp.isSuccessful) error("YouTube Data API v3 HTTP ${resp.code}")
        resp.body?.string().orEmpty()
      }
    val items = JSONObject(body).optJSONArray("items")
    val videoIds = mutableListOf<String>()
    val draftList = mutableListOf<YouTubeVideoItem>()
    if (items != null) {
      for (i in 0 until items.length()) {
        val item = items.optJSONObject(i) ?: continue
        val vid = item.optJSONObject("id")?.optString("videoId").orEmpty()
        val snip = item.optJSONObject("snippet") ?: continue
        if (vid.isBlank()) continue
        videoIds += vid
        val thumb =
          snip.optJSONObject("thumbnails")?.optJSONObject("high")?.optString("url")
            ?: "https://i.ytimg.com/vi/$vid/hqdefault.jpg"
        draftList +=
          YouTubeVideoItem(
            videoId = vid,
            title = snip.optString("title"),
            description = snip.optString("description"),
            channelTitle = snip.optString("channelTitle"),
            publishedAt = snip.optString("publishedAt").take(10),
            thumbnailUrl = thumb,
          )
      }
    }
    return enrichYoutubeStatsBestEffort(draftList, videoIds, activeKey)
  }

  private fun enrichYoutubeStatsBestEffort(
    draftList: List<YouTubeVideoItem>,
    videoIds: List<String>,
    activeKey: String,
  ): List<YouTubeVideoItem> {
    if (videoIds.isEmpty() || activeKey.isBlank()) return draftList
    return runCatching {
      val key = URLEncoder.encode(activeKey, "UTF-8")
      val statsUrl =
        "https://www.googleapis.com/youtube/v3/videos?part=statistics,contentDetails&id=${videoIds.joinToString(",")}&key=$key"
      val statsBody =
        httpClient.newCall(Request.Builder().url(statsUrl).get().build()).execute().use {
          if (it.isSuccessful) it.body?.string().orEmpty() else ""
        }
      if (statsBody.isBlank()) return@runCatching draftList
      val sItems = JSONObject(statsBody).optJSONArray("items") ?: return@runCatching draftList
      val statsMap = mutableMapOf<String, Pair<String, Pair<String, String>>>()
      for (i in 0 until sItems.length()) {
        val sObj = sItems.optJSONObject(i) ?: continue
        val id = sObj.optString("id")
        val st = sObj.optJSONObject("statistics")
        val cd = sObj.optJSONObject("contentDetails")
        statsMap[id] =
          tripleCompat(
            st?.optString("viewCount", "--") ?: "--",
            st?.optString("likeCount", "--") ?: "--",
            parseIsoDuration(cd?.optString("duration").orEmpty()),
          )
      }
      draftList.map { v ->
        val s = statsMap[v.videoId]
        if (s != null) {
          v.copy(
            viewCount = s.first,
            likeCount = s.second.first,
            durationLabel = s.second.second,
          )
        } else v
      }
    }.getOrDefault(draftList)
  }

  private fun fetchYoutubeWebVideos(url: String): List<YouTubeVideoItem> {
    val req =
      Request.Builder()
        .url(url)
        .header("User-Agent", YT_BROWSER_UA)
        .header("Accept-Language", "en-US,en;q=0.9")
        .header("Cookie", "CONSENT=YES+cb.20230531-07-p0.en+FX+908")
        .get()
        .build()
    val html =
      youtubeMirrorHttpClient.newCall(req).execute().use { resp ->
        if (resp.code == 429) error("YouTube Web HTTP 429")
        if (!resp.isSuccessful) error("YouTube Web HTTP ${resp.code}")
        resp.body?.string().orEmpty()
      }
    val jsonStr = extractYtInitialDataJson(html) ?: return emptyList()
    val root = runCatching { JSONObject(jsonStr) }.getOrNull() ?: return emptyList()
    val collected = mutableListOf<YouTubeVideoItem>()
    val seenIds = mutableSetOf<String>()
    collectVideoRenderersRecursive(root, collected, seenIds, maxItems = 16)
    return collected
  }

  private fun extractYtInitialDataJson(html: String): String? {
    val markers = listOf("var ytInitialData = ", "window[\"ytInitialData\"] = ", "ytInitialData = ")
    for (marker in markers) {
      val idx = html.indexOf(marker)
      if (idx < 0) continue
      val start = html.indexOf('{', idx + marker.length)
      if (start < 0) continue
      var depth = 0
      var inString = false
      var escaped = false
      for (i in start until html.length) {
        val ch = html[i]
        if (inString) {
          if (escaped) {
            escaped = false
          } else if (ch == '\\') {
            escaped = true
          } else if (ch == '"') {
            inString = false
          }
        } else {
          when (ch) {
            '"' -> inString = true
            '{' -> depth++
            '}' -> {
              depth--
              if (depth == 0) return html.substring(start, i + 1)
            }
          }
        }
      }
    }
    return null
  }

  private fun collectVideoRenderersRecursive(
    node: Any?,
    out: MutableList<YouTubeVideoItem>,
    seenIds: MutableSet<String>,
    maxItems: Int,
  ) {
    if (out.size >= maxItems || node == null) return
    when (node) {
      is JSONObject -> {
        val renderer =
          node.optJSONObject("videoRenderer")
            ?: node.optJSONObject("compactVideoRenderer")
            ?: node.optJSONObject("gridVideoRenderer")
        if (renderer != null) {
          val vid = renderer.optString("videoId").trim()
          if (vid.length == 11 && seenIds.add(vid)) {
            val title = extractYtRunsText(renderer.optJSONObject("title"))
            if (title.isNotBlank()) {
              val channel =
                extractYtRunsText(renderer.optJSONObject("ownerText"))
                  .ifBlank { extractYtRunsText(renderer.optJSONObject("longBylineText")) }
                  .ifBlank { extractYtRunsText(renderer.optJSONObject("shortBylineText")) }
                  .ifBlank { "YouTube" }
              val published =
                extractYtRunsText(renderer.optJSONObject("publishedTimeText"))
              val duration =
                extractYtRunsText(renderer.optJSONObject("lengthText")).ifBlank { "--:--" }
              val views =
                extractYtRunsText(renderer.optJSONObject("shortViewCountText"))
                  .ifBlank { extractYtRunsText(renderer.optJSONObject("viewCountText")) }
                  .ifBlank { "--" }
              out +=
                YouTubeVideoItem(
                  videoId = vid,
                  title = title,
                  description = "",
                  channelTitle = channel,
                  publishedAt = published,
                  thumbnailUrl = "https://i.ytimg.com/vi/$vid/hqdefault.jpg",
                  viewCount = views,
                  durationLabel = duration,
                )
            }
          }
        }
        val keys = node.keys()
        while (keys.hasNext() && out.size < maxItems) {
          val k = keys.next()
          collectVideoRenderersRecursive(node.opt(k), out, seenIds, maxItems)
        }
      }
      is JSONArray -> {
        for (i in 0 until node.length()) {
          if (out.size >= maxItems) break
          collectVideoRenderersRecursive(node.opt(i), out, seenIds, maxItems)
        }
      }
    }
  }

  private fun extractYtRunsText(obj: JSONObject?): String {
    if (obj == null) return ""
    val simple = obj.optString("simpleText")
    if (simple.isNotBlank()) return simple
    val runs = obj.optJSONArray("runs") ?: return ""
    val sb = StringBuilder()
    for (i in 0 until runs.length()) {
      sb.append(runs.optJSONObject(i)?.optString("text").orEmpty())
    }
    return sb.toString().trim()
  }

  private fun fetchFromYoutubeMirrors(query: String): List<YouTubeVideoItem> {
    val enc = URLEncoder.encode(query, "UTF-8")
    val now = System.currentTimeMillis()
    val mirrors =
      listOf(
        "piped_kavin" to "https://pipedapi.kavin.rocks/search?q=$enc&filter=videos",
        "piped_adminforge" to "https://pipedapi.adminforge.de/search?q=$enc&filter=videos",
        "inv_nadeko" to "https://inv.nadeko.net/api/v1/search?q=$enc&type=video",
      )
    for ((mirrorId, mirrorUrl) in mirrors) {
      val cooldownUntil = youtubeMirrorCooldowns[mirrorId] ?: 0L
      if (now < cooldownUntil) continue
      val parsed =
        runCatching {
          val req =
            Request.Builder()
              .url(mirrorUrl)
              .header("User-Agent", YT_BROWSER_UA)
              .header("Accept", "application/json")
              .get()
              .build()
          youtubeMirrorHttpClient.newCall(req).execute().use { resp ->
            if (isRateLimitOrQuotaHttp(resp.code)) {
              youtubeMirrorCooldowns[mirrorId] = System.currentTimeMillis() + YT_RATE_LIMIT_COOLDOWN_MS
              return@use emptyList()
            }
            if (!resp.isSuccessful) return@use emptyList()
            val raw = resp.body?.string().orEmpty()
            if (mirrorId.startsWith("piped_")) parsePipedSearchJson(raw) else parseInvidiousSearchJson(raw)
          }
        }.getOrDefault(emptyList())
      if (parsed.isNotEmpty()) return parsed
    }
    return emptyList()
  }

  private fun parsePipedSearchJson(raw: String): List<YouTubeVideoItem> {
    val items = JSONObject(raw).optJSONArray("items") ?: return emptyList()
    val out = mutableListOf<YouTubeVideoItem>()
    for (i in 0 until items.length().coerceAtMost(16)) {
      val obj = items.optJSONObject(i) ?: continue
      val urlPath = obj.optString("url")
      val vid = Regex("v=([A-Za-z0-9_-]{11})").find(urlPath)?.groupValues?.get(1).orEmpty()
      if (vid.isBlank()) continue
      val durSec = obj.optLong("duration", 0L)
      val durLabel =
        if (durSec > 0L) {
          val h = durSec / 3600L
          val m = (durSec % 3600L) / 60L
          val s = durSec % 60L
          if (h > 0L) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
        } else "--:--"
      out +=
        YouTubeVideoItem(
          videoId = vid,
          title = obj.optString("title"),
          description = obj.optString("shortDescription"),
          channelTitle = obj.optString("uploaderName").ifBlank { "YouTube" },
          publishedAt = obj.optString("uploadedDate"),
          thumbnailUrl = "https://i.ytimg.com/vi/$vid/hqdefault.jpg",
          viewCount = obj.optLong("views", -1L).takeIf { it >= 0L }?.toString() ?: "--",
          durationLabel = durLabel,
        )
    }
    return out
  }

  private fun parseInvidiousSearchJson(raw: String): List<YouTubeVideoItem> {
    val arr = JSONArray(raw)
    val out = mutableListOf<YouTubeVideoItem>()
    for (i in 0 until arr.length().coerceAtMost(16)) {
      val obj = arr.optJSONObject(i) ?: continue
      val vid = obj.optString("videoId").trim()
      if (vid.length != 11) continue
      val durSec = obj.optLong("lengthSeconds", 0L)
      val durLabel =
        if (durSec > 0L) {
          val h = durSec / 3600L
          val m = (durSec % 3600L) / 60L
          val s = durSec % 60L
          if (h > 0L) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
        } else "--:--"
      out +=
        YouTubeVideoItem(
          videoId = vid,
          title = obj.optString("title"),
          description = obj.optString("description"),
          channelTitle = obj.optString("author").ifBlank { "YouTube" },
          publishedAt = obj.optString("publishedText"),
          thumbnailUrl = "https://i.ytimg.com/vi/$vid/hqdefault.jpg",
          viewCount = obj.optLong("viewCount", -1L).takeIf { it >= 0L }?.toString() ?: "--",
          durationLabel = durLabel,
        )
    }
    return out
  }

  private fun tripleCompat(a: String, b: String, c: String): Pair<String, Pair<String, String>> =
    a to (b to c)

  private fun parseIsoDuration(iso: String): String {
    if (!iso.startsWith("PT")) return "--:--"
    val mins = Regex("(\\d+)M").find(iso)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    val secs = Regex("(\\d+)S").find(iso)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    val hrs = Regex("(\\d+)H").find(iso)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    return if (hrs > 0) "%d:%02d:%02d".format(hrs, mins, secs) else "%d:%02d".format(mins, secs)
  }

  private fun parseYoutubeAtomFeed(xml: String): List<YouTubeVideoItem> {
    val result = mutableListOf<YouTubeVideoItem>()
    val parser = XmlPullParserFactory.newInstance().newPullParser()
    parser.setInput(StringReader(xml))
    var inEntry = false
    var vid = ""
    var title = ""
    var published = ""
    var desc = ""
    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
      when (event) {
        XmlPullParser.START_TAG -> {
          when (parser.name) {
            "entry" -> {
              inEntry = true
              vid = ""
              title = ""
              published = ""
              desc = ""
            }
            "yt:videoId", "videoId" -> if (inEntry) vid = parser.nextText()
            "title" -> if (inEntry && title.isBlank()) title = parser.nextText()
            "published" -> if (inEntry) published = parser.nextText().take(10)
            "media:description", "description" -> if (inEntry) desc = parser.nextText()
          }
        }
        XmlPullParser.END_TAG -> {
          if (parser.name == "entry" && inEntry && vid.isNotBlank()) {
            result +=
              YouTubeVideoItem(
                videoId = vid,
                title = title,
                description = desc,
                channelTitle = "Rural ERides",
                publishedAt = published,
                thumbnailUrl = "https://i.ytimg.com/vi/$vid/hqdefault.jpg",
              )
            inEntry = false
          }
        }
      }
      event = parser.next()
    }
    return result
  }
}
