package com.example.core

import android.content.Context
import android.graphics.BitmapFactory
import android.location.LocationManager
import android.util.Log
import com.example.BuildConfig
import com.example.R
import com.example.ui.components.bitmapToCloudBase64DataUri
import com.example.data.ActiveNavRoute
import com.example.data.BoardComment
import com.example.data.BoardPost
import com.example.data.CallSession
import com.example.data.CallSignal
import com.example.data.ChannelPresence
import com.example.data.ChannelSpeakerState
import com.example.data.ChannelWebRtcSignal
import com.example.data.DailyForecast
import com.example.data.DmMessage
import com.example.data.DmThread
import com.example.data.HourlyForecast
import com.example.data.LiveChargingStation
import com.example.data.NavManeuverStep
import com.example.data.NavRouteProfile
import com.example.data.NewsArticleItem
import com.example.data.PevStoreDirectory
import com.example.data.PublicPilotProfile
import com.example.data.RadarHazard
import com.example.data.RadarMeetup
import com.example.data.RadarRider
import com.example.data.RadarWaypoint
import com.example.data.SupabaseAuthService
import com.example.data.WalkieVoiceLog
import com.example.data.WeatherGeoLocation
import com.example.data.WeatherMatrixData
import com.example.data.YouTubeVideoItem
import com.example.data.YoutubeComment
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.io.StringReader
import java.net.URI
import java.net.URLEncoder
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

private const val TAG = "NetworkClients"

data class DiagnosticCheckResult(
  val name: String,
  val category: String,
  val ok: Boolean,
  val latencyMs: Long,
  val detail: String,
)

object LiveCloudServices {
  const val OFFICIAL_CHANNEL_ID = "UCJloGuR4YwCVIcAh3gjTbTg"

  private val httpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(12, TimeUnit.SECONDS)
      .readTimeout(18, TimeUnit.SECONDS)
      .build()
  }

  private fun isRealKey(value: String): Boolean = ApiKeyRing.isRealKey(value)

  fun hasYoutubeKey(): Boolean = ApiKeyRing.youtubeKeys().isNotEmpty()

  fun hasSupabaseConfig(): Boolean = false

  fun hasWeatherKey(): Boolean = ApiKeyRing.weatherKeys().isNotEmpty()
  fun hasTavilyKey(): Boolean = ApiKeyRing.tavilyKeys().isNotEmpty()
  fun hasBraveKey(): Boolean = ApiKeyRing.braveKeys().isNotEmpty()
  fun hasTomTomKey(): Boolean = ApiKeyRing.tomTomKeys().isNotEmpty()
  fun hasMapboxKey(): Boolean = ApiKeyRing.mapboxTokens().isNotEmpty()
  fun mapboxToken(): String = ApiKeyRing.orderedKeys("MAPBOX", ApiKeyRing.mapboxTokens()).firstOrNull().orEmpty()
  fun hasOpenRouteServiceKey(): Boolean = ApiKeyRing.openRouteServiceKeys().isNotEmpty()
  fun hasOpenChargeMapKey(): Boolean = ApiKeyRing.openChargeMapKeys().isNotEmpty()
  fun hasElevationKey(): Boolean = ApiKeyRing.elevationKeys().isNotEmpty()
  fun hasNewsKey(): Boolean = ApiKeyRing.newsKeys().isNotEmpty()
  fun hasImgbbKey(): Boolean = ApiKeyRing.imgbbKeys().isNotEmpty()
  fun hasCseKeys(): Boolean = ApiKeyRing.googleCsePairs().isNotEmpty()

  // --------------------------------------------------------------------------
  // 1. OPEN-METEO HYPERLOCAL WEATHER MATRIX (+ OPTIONAL OPENWEATHER ENRICHMENT)
  // --------------------------------------------------------------------------
  internal fun parseWeatherLocationsJson(raw: String): List<WeatherGeoLocation> =
    runCatching {
      val arr = JSONObject(raw).optJSONArray("results") ?: return emptyList()
      (0 until arr.length()).mapNotNull { i ->
        val o = arr.optJSONObject(i) ?: return@mapNotNull null
        val name = o.optString("name").trim()
        if (name.isBlank()) return@mapNotNull null
        WeatherGeoLocation(
          name = name,
          admin1 = o.optString("admin1").trim(),
          country = o.optString("country_code").ifBlank { o.optString("country") }.trim(),
          lat = o.optDouble("latitude", 0.0),
          lng = o.optDouble("longitude", 0.0),
        )
      }
    }.getOrDefault(emptyList())

  suspend fun searchWeatherLocations(query: String): Result<List<WeatherGeoLocation>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val clean = query.trim()
        if (clean.length < 2) return@runCatching emptyList()
        val encoded = URLEncoder.encode(clean, "UTF-8")
        val url =
          "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=6&language=en&format=json"
        val req = Request.Builder().url(url).get().build()
        val raw =
          httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("Geocoding HTTP ${resp.code}")
            resp.body?.string().orEmpty()
          }
        parseWeatherLocationsJson(raw)
      }
    }

  suspend fun reverseGeocodeCityLabel(lat: Double, lng: Double): String =
    withContext(Dispatchers.IO) {
      if (lat == 0.0 && lng == 0.0) return@withContext ""
      // 1. Mapbox reverse geocoding if configured
      if (hasMapboxKey()) {
        val mb =
          runCatching {
            val token = URLEncoder.encode(mapboxToken(), "UTF-8")
            val mbUrl =
              "https://api.mapbox.com/geocoding/v5/mapbox.places/$lng,$lat.json?types=place,locality,region&limit=1&access_token=$token"
            httpClient.newCall(Request.Builder().url(mbUrl).get().build()).execute().use { resp ->
              if (!resp.isSuccessful) return@use ""
              val feat = JSONObject(resp.body?.string().orEmpty()).optJSONArray("features")?.optJSONObject(0)
              feat?.optString("place_name")?.split(",")?.take(2)?.joinToString(",")?.trim().orEmpty()
            }
          }.getOrDefault("")
        if (mb.isNotBlank()) return@withContext mb
      }
      // 2. Nominatim fallback
      runCatching {
        val nomUrl = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=$lat&lon=$lng&zoom=10"
        val req =
          Request.Builder().url(nomUrl).header("User-Agent", "RuralERidesGO/2.0 Android").get().build()
        httpClient.newCall(req).execute().use { resp ->
          if (!resp.isSuccessful) return@use ""
          val addr = JSONObject(resp.body?.string().orEmpty()).optJSONObject("address") ?: return@use ""
          val city =
            addr.optString("city")
              .ifBlank { addr.optString("town") }
              .ifBlank { addr.optString("village") }
              .ifBlank { addr.optString("county") }
          val state = addr.optString("state")
          listOf(city, state).filter { it.isNotBlank() }.joinToString(", ")
        }
      }.getOrDefault("")
    }

  suspend fun fetchWeatherMatrix(
    lat: Double,
    lng: Double,
    explicitLabel: String = "",
    isGpsSource: Boolean = true,
  ): Result<WeatherMatrixData> =
    withContext(Dispatchers.IO) {
      runCatching {
        val url =
          "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lng" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m" +
            "&hourly=temperature_2m,precipitation_probability,wind_speed_10m" +
            "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,wind_speed_10m_max,uv_index_max,sunrise,sunset" +
            "&forecast_days=10&timezone=auto"
        val req = Request.Builder().url(url).get().build()
        val body =
          httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("Open-Meteo HTTP ${resp.code}")
            resp.body?.string().orEmpty()
          }
        val json = JSONObject(body)
        val cur = json.getJSONObject("current")
        val hourlyJson = json.getJSONObject("hourly")
        val hTimes = hourlyJson.getJSONArray("time")
        val hTemps = hourlyJson.getJSONArray("temperature_2m")
        val hPrecips = hourlyJson.getJSONArray("precipitation_probability")
        val hWinds = hourlyJson.getJSONArray("wind_speed_10m")

        val hourlyList =
          (0 until minOf(12, hTimes.length())).map { i ->
            HourlyForecast(
              timeLabel = hTimes.optString(i).takeLast(5),
              tempC = hTemps.optDouble(i),
              windKmh = hWinds.optDouble(i),
              precipPct = hPrecips.optInt(i),
            )
          }

        val dailyJson = json.getJSONObject("daily")
        val dDates = dailyJson.getJSONArray("time")
        val dMin = dailyJson.getJSONArray("temperature_2m_min")
        val dMax = dailyJson.getJSONArray("temperature_2m_max")
        val dPrecip = dailyJson.getJSONArray("precipitation_probability_max")
        val dWind = dailyJson.getJSONArray("wind_speed_10m_max")
        val dUv = dailyJson.getJSONArray("uv_index_max")
        val dSunrise = dailyJson.getJSONArray("sunrise")
        val dSunset = dailyJson.getJSONArray("sunset")

        val dailyList =
          (0 until minOf(10, dDates.length())).map { i ->
            DailyForecast(
              dateLabel = dDates.optString(i).takeLast(5),
              minTempC = dMin.optDouble(i),
              maxTempC = dMax.optDouble(i),
              precipPct = dPrecip.optInt(i),
              windMaxKmh = dWind.optDouble(i),
            )
          }

        val wCode = cur.optInt("weather_code", 0)
        var label = wmoCodeLabel(wCode)
        var resolvedPlace = explicitLabel.trim()
        if (hasWeatherKey()) {
          fetchOpenWeatherDescription(lat, lng)?.let { owDesc ->
            label = "$label • $owDesc"
            if (resolvedPlace.isBlank() && owDesc.contains(" @ ")) {
              resolvedPlace = owDesc.substringAfter(" @ ").trim()
            }
          }
        }
        if (resolvedPlace.isBlank()) {
          resolvedPlace = reverseGeocodeCityLabel(lat, lng).ifBlank {
            "${"%.3f".format(lat)}, ${"%.3f".format(lng)}"
          }
        }

        WeatherMatrixData(
          tempC = cur.optDouble("temperature_2m", 18.0),
          feelsLikeC = cur.optDouble("apparent_temperature", 18.0),
          windSpeedKmh = cur.optDouble("wind_speed_10m", 0.0),
          windGustKmh = cur.optDouble("wind_gusts_10m", 0.0),
          windDirDeg = cur.optInt("wind_direction_10m", 0),
          precipProbPct = dailyList.firstOrNull()?.precipPct ?: 0,
          uvIndex = if (dUv.length() > 0) dUv.optDouble(0, 0.0) else 0.0,
          pressureHpa = cur.optDouble("surface_pressure", 1013.0),
          humidityPct = cur.optInt("relative_humidity_2m", 50),
          weatherCode = wCode,
          conditionLabel = label,
          locationLabel = resolvedPlace,
          lat = lat,
          lng = lng,
          isGpsSource = isGpsSource,
          sunrise = if (dSunrise.length() > 0) dSunrise.optString(0).takeLast(5) else "--:--",
          sunset = if (dSunset.length() > 0) dSunset.optString(0).takeLast(5) else "--:--",
          hourly = hourlyList,
          daily = dailyList,
        )
      }
    }

  private fun fetchOpenWeatherDescription(lat: Double, lng: Double): String? {
    val keys = ApiKeyRing.weatherKeys()
    if (keys.isEmpty()) return null
    return runCatching {
      ApiKeyRing.executeWithFailover("OPENWEATHER", keys) { rawKey ->
        val key = URLEncoder.encode(rawKey, "UTF-8")
        val url =
          "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lng&appid=$key&units=metric"
        httpClient.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
          if (ApiKeyRing.isFailoverCode(resp.code)) throw ApiKeyFailoverException(resp.code, "OpenWeather HTTP ${resp.code}")
          if (!resp.isSuccessful) return@executeWithFailover null
          val json = JSONObject(resp.body?.string().orEmpty())
          val desc =
            json.optJSONArray("weather")?.optJSONObject(0)?.optString("description").orEmpty()
          val station = json.optString("name")
          listOf(desc.replaceFirstChar { it.uppercase() }, station)
            .filter { it.isNotBlank() }
            .joinToString(" @ ")
            .takeIf { it.isNotBlank() }
        }
      }
    }.getOrNull()
  }

  private fun wmoCodeLabel(code: Int): String =
    when (code) {
      0 -> "Clear Sky"
      1, 2 -> "Partly Cloudy"
      3 -> "Overcast"
      45, 48 -> "Fog / Mist"
      51, 53, 55 -> "Light Drizzle"
      61, 63, 65 -> "Active Rain"
      71, 73, 75 -> "Snowfall"
      80, 81, 82 -> "Rain Showers"
      95, 96, 99 -> "Thunderstorm Alert"
      else -> "Conditions Live ($code)"
    }

  // --------------------------------------------------------------------------
  // 2. OVERPASS OSM & NOMINATIM SPEED LIMIT + ROAD GEOCODING (KEYLESS + TOMTOM)
  // --------------------------------------------------------------------------
  data class RoadSignInfo(
    val roadName: String,
    val speedLimitMph: Int?,
    val inSchoolZone: Boolean,
  )

  private val firstSpeedTokenRegex = Regex("""\b(\d{1,3})\b""")
  private val digitUnitBoundaryRegex = Regex("""(?<=\d)(?=[a-zA-Z])""")

  internal fun parseRoadSpeedLimitMph(rawTag: String, defaultAsKmh: Boolean = true): Int? {
    val cleaned = rawTag.trim()
    if (cleaned.isBlank()) return null
    val normalized = cleaned.replace(digitUnitBoundaryRegex, " ")
    val token = firstSpeedTokenRegex.find(normalized)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return null
    val isExplicitMph = cleaned.contains("mph", ignoreCase = true)
    val isExplicitKmh =
      cleaned.contains("kph", ignoreCase = true) ||
        cleaned.contains("km/h", ignoreCase = true) ||
        cleaned.contains("kmh", ignoreCase = true)
    val parseAsKmh = !isExplicitMph && (isExplicitKmh || defaultAsKmh)
    return if (parseAsKmh) {
      if (token in 10..140) kotlin.math.round(token * 0.621371).toInt().coerceIn(5, 85) else null
    } else {
      if (token in 5..85) token else null
    }
  }

  suspend fun fetchLiveRoadAndSpeedLimit(lat: Double, lng: Double): Result<RoadSignInfo> =
    withContext(Dispatchers.IO) {
      runCatching {
        var road = ""
        var limitMph: Int? = null
        var schoolNearby = false

        val ttKeys = ApiKeyRing.tomTomKeys()
        if (ttKeys.isNotEmpty()) {
          runCatching {
            val respBody =
              ApiKeyRing.executeWithFailover("TOMTOM", ttKeys) { rawKey ->
                val encodedKey = URLEncoder.encode(rawKey, "UTF-8")
                val ttUrl =
                  "https://api.tomtom.com/search/2/reverseGeocode/$lat,$lng.json?key=$encodedKey&returnSpeedLimit=true"
                val req = Request.Builder().url(ttUrl).get().build()
                httpClient.newCall(req).execute().use { resp ->
                  if (ApiKeyRing.isFailoverCode(resp.code)) throw ApiKeyFailoverException(resp.code, "TomTom HTTP ${resp.code}")
                  if (resp.isSuccessful) resp.body?.string().orEmpty() else ""
                }
              }
            if (respBody.isNotBlank()) {
              val addr =
                JSONObject(respBody).optJSONArray("addresses")?.optJSONObject(0)?.optJSONObject("address")
              val street =
                addr?.optString("streetName").orEmpty().ifBlank {
                  addr?.optString("freeformAddress").orEmpty()
                }
              if (street.isNotBlank()) road = street
              val limitStr = addr?.optString("speedLimit").orEmpty()
              val mph = parseRoadSpeedLimitMph(limitStr, defaultAsKmh = false)
              if (mph != null) limitMph = mph
            }
          }
        }

        // Query Overpass OSM for speed limits & school zones (plus road name if missing)
        runCatching {
          val query =
            """[out:json][timeout:6];(way(around:45,$lat,$lng)["highway"];node(around:120,$lat,$lng)["amenity"="school"];way(around:120,$lat,$lng)["amenity"="school"];);out tags 6;"""
          val encoded = URLEncoder.encode(query, "UTF-8")
          val overpassUrl = "https://overpass-api.de/api/interpreter?data=$encoded"
          val req =
            Request.Builder()
              .url(overpassUrl)
              .header("User-Agent", "RuralERidesGO/2.0 Android")
              .get()
              .build()
          val body =
            httpClient.newCall(req).execute().use { resp ->
              if (resp.isSuccessful) resp.body?.string().orEmpty() else ""
            }
          if (body.isNotBlank()) {
            val elements = JSONObject(body).optJSONArray("elements")
            if (elements != null) {
              for (i in 0 until elements.length()) {
                val tags = elements.optJSONObject(i)?.optJSONObject("tags") ?: continue
                if (tags.optString("amenity") == "school" || tags.optString("school") != "") {
                  schoolNearby = true
                }
                if (tags.has("name") && road.isBlank()) {
                  road = tags.optString("name", "")
                }
                if (tags.has("maxspeed") && limitMph == null) {
                  val ms = tags.optString("maxspeed")
                  val parsedMph = parseRoadSpeedLimitMph(ms, defaultAsKmh = true)
                  if (parsedMph != null) {
                    limitMph = parsedMph
                  }
                }
              }
            }
          }
        }

        // Enrich street/place name via Mapbox Geocoding if still blank
        if (road.isBlank() && hasMapboxKey()) {
          runCatching {
            val token = URLEncoder.encode(mapboxToken(), "UTF-8")
            val mbUrl =
              "https://api.mapbox.com/geocoding/v5/mapbox.places/$lng,$lat.json?types=address,street,place,locality&limit=1&access_token=$token"
            val body =
              httpClient.newCall(Request.Builder().url(mbUrl).get().build()).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string().orEmpty() else ""
              }
            if (body.isNotBlank()) {
              val feat = JSONObject(body).optJSONArray("features")?.optJSONObject(0)
              val place =
                feat?.optString("text").orEmpty().ifBlank { feat?.optString("place_name").orEmpty() }
              if (place.isNotBlank()) road = place
            }
          }
        }

        RoadSignInfo(
          roadName = road.ifBlank { "Rural Sector Trail" },
          speedLimitMph = limitMph,
          inSchoolZone = schoolNearby,
        )
      }
    }

  suspend fun fetchElevationSparkline(points: List<Pair<Double, Double>>): List<Double> =
    withContext(Dispatchers.IO) {
      if (points.isEmpty()) return@withContext emptyList()
      runCatching {
        val sample = if (points.size > 12) points.chunked(points.size / 10).map { it.first() } else points
        val lats = sample.joinToString(",") { "%.5f".format(it.first) }
        val lngs = sample.joinToString(",") { "%.5f".format(it.second) }
        val url = "https://api.open-meteo.com/v1/elevation?latitude=$lats&longitude=$lngs"
        val body =
          httpClient.newCall(Request.Builder().url(url).get().build()).execute().use {
            it.body?.string().orEmpty()
          }
        val arr = JSONObject(body).optJSONArray("elevation") ?: return@runCatching emptyList()
        (0 until arr.length()).map { arr.optDouble(it, 0.0) }
      }.getOrDefault(emptyList())
    }

  // --------------------------------------------------------------------------
  // 3. YOUTUBE DATA API V3 + ANTI-429 MULTI-SOURCE RESILIENCE ENGINE
  // --------------------------------------------------------------------------
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

        // TIER 1: YouTube Data API v3 with Automatic Redundant Key Pool (_1 -> _2 -> _3) + 1-unit playlistItems
        val ytKeys = ApiKeyRing.youtubeKeys()
        if (ytKeys.isNotEmpty() && ApiKeyRing.hasHealthyKey("YOUTUBE_V3", ytKeys) && now >= youtubeApiV3CooldownUntilMs) {
          runCatching {
            val items =
              ApiKeyRing.executeWithFailover("YOUTUBE_V3", ytKeys) { rawKey ->
                val key = URLEncoder.encode(rawKey, "UTF-8")
                if (channelOnly && cleanQ.isBlank()) {
                  val uploadsPlaylistId =
                    if (OFFICIAL_CHANNEL_ID.startsWith("UC")) "UU" + OFFICIAL_CHANNEL_ID.drop(2)
                    else OFFICIAL_CHANNEL_ID
                  val playlistUrl =
                    "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&playlistId=$uploadsPlaylistId&maxResults=16&key=$key"
                  executeYoutubePlaylistItems(playlistUrl, rawKey)
                } else {
                  val encodedQ = URLEncoder.encode(cleanQ.ifBlank { "PEV electric scooter euc" }, "UTF-8")
                  val url =
                    if (channelOnly) {
                      "https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=$OFFICIAL_CHANNEL_ID&q=$encodedQ&maxResults=16&order=relevance&type=video&videoEmbeddable=true&key=$key"
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
                fetchYoutubeWebVideos("https://www.youtube.com/channel/$OFFICIAL_CHANNEL_ID/videos")
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
    val rssUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=$OFFICIAL_CHANNEL_ID"
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
          TripleCompat(
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

  private fun TripleCompat(a: String, b: String, c: String): Pair<String, Pair<String, String>> =
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

  // --------------------------------------------------------------------------
  // 4. OPTIONAL SUPABASE POSTGREST BOARD FALLBACK (FOR SUPABASE-ONLY SESSIONS)
  // --------------------------------------------------------------------------
  private val supabaseRestClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
      .retryOnConnectionFailure(true)
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .build()
  }

  private fun supabaseRestBaseUrl(): String = ""

  private fun isMissingPostgrestSchemaOrTable(code: Int, bodyStr: String): Boolean {
    if (code == 404) return true
    if (code != 400) return false
    return bodyStr.contains("PGRST205", ignoreCase = true) ||
      bodyStr.contains("PGRST106", ignoreCase = true) ||
      bodyStr.contains("42P01", ignoreCase = true) ||
      bodyStr.contains("Could not find the table", ignoreCase = true) ||
      bodyStr.contains("does not exist", ignoreCase = true)
  }

  private fun Request.Builder.withSupabaseRestHeaders(accessToken: String): Request.Builder {
    val key = ""
    header("apikey", key)
    header("Accept", "application/json")
    header("User-Agent", "RuralERidesGO/11.0 (Android)")
    val bearer = accessToken.trim().ifBlank { key }
    if (SupabaseAuthService.isJwtToken(bearer)) {
      header("Authorization", "Bearer $bearer")
    }
    return this
  }

  private suspend fun executeSupabaseRestWithRetry(buildReq: () -> Request): okhttp3.Response {
    var lastIo: java.io.IOException? = null
    for (attempt in 0 until 4) {
      try {
        val baseReq = buildReq()
        val req =
          if (attempt > 0) baseReq.newBuilder().header("Connection", "close").build()
          else baseReq
        val resp = supabaseRestClient.newCall(req).execute()
        if (resp.code !in listOf(520, 502, 503, 504) || attempt == 3) {
          return resp
        }
        Log.w(TAG, "Supabase REST HTTP ${resp.code} on attempt ${attempt + 1}; evicting pool & retrying...")
        resp.close()
        supabaseRestClient.connectionPool.evictAll()
      } catch (e: CancellationException) {
        throw e
      } catch (e: java.io.IOException) {
        lastIo = e
        if (attempt == 3) throw e
        supabaseRestClient.connectionPool.evictAll()
      }
      kotlinx.coroutines.delay(500L * (attempt + 1))
    }
    throw lastIo ?: java.io.IOException("Supabase REST unavailable")
  }

  private val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

  internal fun toAppSbUid(raw: String): String {
    val clean = raw.trim()
    if (clean.isBlank()) return ""
    if (clean.startsWith("sb_")) return clean
    return if (uuidRegex.matches(clean)) "sb_$clean" else clean
  }

  internal fun parseSupabaseTimestamp(iso: String): Timestamp? {
    val clean = iso.trim()
    if (clean.isBlank()) return null
    return runCatching {
      val instant = Instant.parse(clean)
      Timestamp(instant.epochSecond, instant.nano)
    }.getOrNull()
  }

  internal fun isAuthenticSupabasePilot(
    uid: String,
    callsign: String,
    selfUid: String = "",
  ): Boolean =
    com.example.data.PilotAuthenticityGuard.isAuthenticCallsignAndUid(
      userId = toAppSbUid(uid),
      callsign = callsign,
      requireFirebaseUid = false,
      currentUid = selfUid.takeIf { it.isNotBlank() }?.let { toAppSbUid(it) },
    )

  private fun parseJsonStringList(arr: JSONArray?): List<String> {
    if (arr == null) return emptyList()
    return (0 until arr.length()).mapNotNull { idx ->
      arr.optString(idx).takeIf { it.isNotBlank() }
    }
  }

  private suspend fun supabaseRestGetArray(
    accessToken: String,
    primaryPathAndQuery: String,
    fallbackPathAndQuery: String? = null,
  ): JSONArray {
    if (!hasSupabaseConfig()) error("Supabase not configured")
    val base = supabaseRestBaseUrl()
    val (statusCode, bodyStr) =
      executeSupabaseRestWithRetry {
        Request.Builder()
          .url("$base$primaryPathAndQuery")
          .withSupabaseRestHeaders(accessToken)
          .get()
          .build()
      }.use { resp -> resp.code to resp.body?.string().orEmpty() }

    val recoveredPair =
      if (statusCode == 520 && accessToken.isNotBlank()) {
        executeSupabaseRestWithRetry {
          Request.Builder()
            .url("$base$primaryPathAndQuery")
            .withSupabaseRestHeaders("")
            .header("Connection", "close")
            .get()
            .build()
        }.use { respAnon -> respAnon.code to respAnon.body?.string().orEmpty() }
      } else {
        statusCode to bodyStr
      }

    val finalPair =
      if (fallbackPathAndQuery != null && recoveredPair.first == 400 && !isMissingPostgrestSchemaOrTable(recoveredPair.first, recoveredPair.second)) {
        executeSupabaseRestWithRetry {
          Request.Builder()
            .url("$base$fallbackPathAndQuery")
            .withSupabaseRestHeaders(if (statusCode == 520) "" else accessToken)
            .get()
            .build()
        }.use { resp2 -> resp2.code to resp2.body?.string().orEmpty() }
      } else {
        recoveredPair
      }

    if (isMissingPostgrestSchemaOrTable(finalPair.first, finalPair.second)) {
      Log.w(TAG, "Supabase table unavailable for $primaryPathAndQuery (HTTP ${finalPair.first})")
      return JSONArray()
    }
    if (finalPair.first !in 200..299) {
      error(SupabaseAuthService.parseSupabaseError(finalPair.second, finalPair.first))
    }
    return runCatching { JSONArray(finalPair.second) }.getOrDefault(JSONArray())
  }

  private suspend fun supabaseRestWrite(
    accessToken: String,
    method: String,
    pathAndQuery: String,
    payloadJson: String? = null,
    preferHeader: String = "return=minimal",
  ) {
    if (!hasSupabaseConfig()) error("Supabase not configured")
    val base = supabaseRestBaseUrl()
    val reqBody = payloadJson?.toRequestBody("application/json".toMediaType())
    executeSupabaseRestWithRetry {
      val builder =
        Request.Builder()
          .url("$base$pathAndQuery")
          .withSupabaseRestHeaders(accessToken)
      if (payloadJson != null) {
        builder.header("Content-Type", "application/json")
      }
      if (preferHeader.isNotBlank()) {
        builder.header("Prefer", preferHeader)
      }
      when (method.uppercase()) {
        "POST" -> builder.post(reqBody ?: "".toRequestBody("application/json".toMediaType()))
        "PATCH" -> builder.patch(reqBody ?: "{}".toRequestBody("application/json".toMediaType()))
        "DELETE" -> builder.delete()
        else -> builder.get()
      }.build()
    }.use { resp ->
      val bodyStr = resp.body?.string().orEmpty()
      if (!resp.isSuccessful) {
        if (isMissingPostgrestSchemaOrTable(resp.code, bodyStr)) {
          error("Supabase tables not created yet. Open Settings -> Cloud Backup -> Copy Supabase SQL Schema.")
        }
        error(SupabaseAuthService.parseSupabaseError(bodyStr, resp.code))
      }
    }
  }

  private fun parseSupabaseBoardPostJson(o: JSONObject, fallbackIdx: Int): BoardPost =
    BoardPost(
      id = o.optString("id", "sb_$fallbackIdx"),
      userId = toAppSbUid(o.optString("user_id").ifBlank { o.optString("userId") }),
      authorCallsign = o.optString("author_callsign").ifBlank { o.optString("authorCallsign", "Pilot") },
      authorBadge = o.optString("author_badge").ifBlank { o.optString("authorBadge", "Supabase Pilot") },
      category = o.optString("category", "UNIVERSAL"),
      title = o.optString("title"),
      body = o.optString("body"),
      imageUrl = o.optString("image_url").ifBlank { o.optString("imageUrl") },
      pollQuestion = o.optString("poll_question").ifBlank { o.optString("pollQuestion") },
      pollOptions = parseJsonStringList(o.optJSONArray("poll_options") ?: o.optJSONArray("pollOptions")),
      priceUsd = o.optDouble("price_usd", o.optDouble("priceUsd", 0.0)),
      locationLabel = o.optString("location_label").ifBlank { o.optString("locationLabel") },
      telemetrySummary = o.optString("telemetry_summary").ifBlank { o.optString("telemetrySummary") },
      helpNeeded = o.optBoolean("help_needed", o.optBoolean("helpNeeded", false)),
      speedTrap = o.optBoolean("speed_trap", o.optBoolean("speedTrap", false)),
      stolenAlert = o.optBoolean("stolen_alert", o.optBoolean("stolenAlert", false)),
      likesCount = o.optLong("likes_count", o.optLong("likesCount", 0L)),
      commentsCount = o.optLong("comments_count", o.optLong("commentsCount", 0L)),
      flaggedCount = o.optLong("flagged_count", o.optLong("flaggedCount", 0L)),
      createdAt = parseSupabaseTimestamp(o.optString("created_at")),
    )

  suspend fun fetchSupabaseBoardPosts(accessToken: String): Result<List<BoardPost>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val arr =
          supabaseRestGetArray(
            accessToken = accessToken,
            primaryPathAndQuery = "/rest/v1/board_posts?select=*&order=created_at.desc&limit=50",
            fallbackPathAndQuery = "/rest/v1/board_posts?select=*&limit=50",
          )
        (0 until arr.length()).mapNotNull { i ->
          val o = arr.optJSONObject(i) ?: return@mapNotNull null
          val post = parseSupabaseBoardPostJson(o, i)
          if (isAuthenticSupabasePilot(post.userId, post.authorCallsign)) post else null
        }
      }
    }

  suspend fun insertSupabaseBoardPost(accessToken: String, post: BoardPost): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        if (!hasSupabaseConfig()) error("Supabase not configured")
        val base = supabaseRestBaseUrl()
        val normalizedUid = toAppSbUid(post.userId)
        val postId = post.id.ifBlank { java.util.UUID.randomUUID().toString() }
        val fullPayload =
          JSONObject().apply {
            put("id", postId)
            if (normalizedUid.isNotBlank()) put("user_id", normalizedUid)
            put("author_callsign", post.authorCallsign)
            put("author_badge", post.authorBadge)
            put("category", post.category)
            put("title", post.title)
            put("body", post.body)
            put("image_url", post.imageUrl)
            put("poll_question", post.pollQuestion)
            put("poll_options", JSONArray(post.pollOptions))
            put("location_label", post.locationLabel)
            put("telemetry_summary", post.telemetrySummary)
            put("price_usd", post.priceUsd)
            put("help_needed", post.helpNeeded)
            put("speed_trap", post.speedTrap)
            put("stolen_alert", post.stolenAlert)
          }.toString()

        val (firstCode, firstBody) =
          executeSupabaseRestWithRetry {
            Request.Builder()
              .url("$base/rest/v1/board_posts")
              .withSupabaseRestHeaders(accessToken)
              .header("Content-Type", "application/json")
              .header("Prefer", "return=minimal")
              .post(fullPayload.toRequestBody("application/json".toMediaType()))
              .build()
          }.use { resp -> resp.code to resp.body?.string().orEmpty() }

        if (firstCode in 200..299) return@runCatching
        if (isMissingPostgrestSchemaOrTable(firstCode, firstBody)) {
          error("Supabase board_posts table not created yet. Open Settings -> Cloud Backup -> Copy Supabase SQL Schema to create it.")
        }
        if (firstCode == 400) {
          val minimalPayload =
            JSONObject().apply {
              put("id", postId)
              if (normalizedUid.isNotBlank()) put("user_id", normalizedUid)
              put("author_callsign", post.authorCallsign)
              put("category", post.category)
              put("title", post.title)
              put("body", post.body)
            }.toString()
          supabaseRestWrite(accessToken, "POST", "/rest/v1/board_posts", minimalPayload)
        } else {
          error(SupabaseAuthService.parseSupabaseError(firstBody, firstCode))
        }
      }
    }

  suspend fun updateSupabaseBoardPost(accessToken: String, postId: String, title: String, body: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val payload =
          JSONObject().apply {
            put("title", title.trim().take(120))
            put("body", body.trim().take(3000))
          }.toString()
        supabaseRestWrite(
          accessToken = accessToken,
          method = "PATCH",
          pathAndQuery = "/rest/v1/board_posts?id=eq.${URLEncoder.encode(postId, "UTF-8")}",
          payloadJson = payload,
          preferHeader = "",
        )
      }
    }

  suspend fun updateSupabaseBoardLikes(accessToken: String, postId: String, nextLikes: Long): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val payloadStr = JSONObject().apply { put("likes_count", nextLikes) }.toString()
        supabaseRestWrite(
          accessToken = accessToken,
          method = "PATCH",
          pathAndQuery = "/rest/v1/board_posts?id=eq.${URLEncoder.encode(postId, "UTF-8")}",
          payloadJson = payloadStr,
          preferHeader = "",
        )
      }
    }

  suspend fun voteSupabaseBoardPoll(accessToken: String, postId: String, updatedOptions: List<String>): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val payloadStr = JSONObject().apply { put("poll_options", JSONArray(updatedOptions)) }.toString()
        supabaseRestWrite(
          accessToken = accessToken,
          method = "PATCH",
          pathAndQuery = "/rest/v1/board_posts?id=eq.${URLEncoder.encode(postId, "UTF-8")}",
          payloadJson = payloadStr,
          preferHeader = "",
        )
      }
    }

  suspend fun flagSupabaseBoardPost(accessToken: String, postId: String, nextFlagCount: Long): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val payloadStr = JSONObject().apply { put("flagged_count", nextFlagCount) }.toString()
        supabaseRestWrite(
          accessToken = accessToken,
          method = "PATCH",
          pathAndQuery = "/rest/v1/board_posts?id=eq.${URLEncoder.encode(postId, "UTF-8")}",
          payloadJson = payloadStr,
          preferHeader = "",
        )
      }
    }

  suspend fun fetchSupabaseBoardComments(accessToken: String, postId: String): Result<List<BoardComment>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val encodedId = URLEncoder.encode(postId, "UTF-8")
        val arr =
          supabaseRestGetArray(
            accessToken = accessToken,
            primaryPathAndQuery = "/rest/v1/board_comments?post_id=eq.$encodedId&order=created_at.asc",
            fallbackPathAndQuery = "/rest/v1/board_comments?post_id=eq.$encodedId",
          )
        (0 until arr.length()).mapNotNull { i ->
          val o = arr.optJSONObject(i) ?: return@mapNotNull null
          BoardComment(
            id = o.optString("id", "sbc_$i"),
            userId = toAppSbUid(o.optString("user_id").ifBlank { o.optString("userId") }),
            authorCallsign = o.optString("author_callsign").ifBlank { o.optString("authorCallsign", "Pilot") },
            body = o.optString("body"),
            createdAt = parseSupabaseTimestamp(o.optString("created_at")),
          )
        }
      }
    }

  suspend fun insertSupabaseBoardComment(
    accessToken: String,
    postId: String,
    userId: String,
    callsign: String,
    body: String,
  ): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val normalizedUid = toAppSbUid(userId)
        val payloadStr =
          JSONObject().apply {
            put("post_id", postId)
            if (normalizedUid.isNotBlank()) put("user_id", normalizedUid)
            put("author_callsign", callsign)
            put("body", body)
          }.toString()
        supabaseRestWrite(accessToken, "POST", "/rest/v1/board_comments", payloadStr)
      }
    }

  suspend fun deleteSupabaseBoardPost(accessToken: String, postId: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        supabaseRestWrite(
          accessToken = accessToken,
          method = "DELETE",
          pathAndQuery = "/rest/v1/board_posts?id=eq.${URLEncoder.encode(postId, "UTF-8")}",
          preferHeader = "",
        )
      }
    }

  // --------------------------------------------------------------------------
  // 4b. SUPABASE PILOT PROFILES, RADAR, DMS, WALKIE PTT & CALL SIGNALING
  // --------------------------------------------------------------------------
  suspend fun fetchSupabasePublicPilots(
    accessToken: String,
    selfUid: String = "",
  ): Result<List<PublicPilotProfile>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val arr =
          supabaseRestGetArray(
            accessToken = accessToken,
            primaryPathAndQuery = "/rest/v1/pilot_profiles?visibility=eq.PUBLIC&order=xp_points.desc&limit=100",
            fallbackPathAndQuery = "/rest/v1/pilot_profiles?select=*&limit=100",
          )
        val normSelf = toAppSbUid(selfUid)
        (0 until arr.length()).mapNotNull { i ->
          val o = arr.optJSONObject(i) ?: return@mapNotNull null
          val uid = toAppSbUid(o.optString("user_id"))
          val cs = o.optString("callsign").trim()
          val banned = o.optBoolean("banned", false)
          val profile =
            PublicPilotProfile(
              id = uid,
              userId = uid,
              callsign = cs,
              rankBadge = o.optString("rank_badge", "Rookie Pilot"),
              xpPoints = o.optLong("xp_points", 0L),
              totalKm = o.optDouble("total_km", 0.0),
              ridesCount = o.optLong("rides_count", 0L),
              verified = o.optBoolean("verified", false),
              banned = banned,
              trustScore = o.optLong("trust_score", 100L),
              vehicleSummary = o.optString("vehicle_summary"),
              visibility = o.optString("visibility", "PUBLIC"),
              avatarBase64 = o.optString("avatar_base64"),
              updatedAt = parseSupabaseTimestamp(o.optString("updated_at")),
            )
          val valid =
            com.example.data.PilotAuthenticityGuard.isDisplayablePilotProfile(
              p = profile,
              currentUid = normSelf.takeIf { it.isNotBlank() },
              requireFirebaseUid = false,
            )
          if (valid) profile else null
        }
          .sortedByDescending { it.xpPoints }
          .distinctBy { it.userId }
          .distinctBy { it.callsign.trim().uppercase() }
      }
    }

  suspend fun upsertSupabasePilotProfile(accessToken: String, pilot: PublicPilotProfile): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val uid = toAppSbUid(pilot.userId.ifBlank { pilot.id })
        if (uid.isBlank()) error("Missing Supabase pilot user_id")
        val payload =
          JSONObject().apply {
            put("user_id", uid)
            put("callsign", pilot.callsign.trim().take(32).ifBlank { "PILOT" })
            put("rank_badge", pilot.rankBadge.take(40).ifBlank { "Rookie Pilot" })
            put("xp_points", pilot.xpPoints.coerceAtLeast(0L))
            put("total_km", pilot.totalKm.coerceAtLeast(0.0))
            put("rides_count", pilot.ridesCount.coerceAtLeast(0L))
            put("verified", pilot.verified)
            put("banned", false)
            put("trust_score", pilot.trustScore.coerceIn(0L, 1000L))
            put("vehicle_summary", pilot.vehicleSummary.trim().take(100))
            put("visibility", pilot.visibility.ifBlank { "PUBLIC" })
            put("avatar_base64", pilot.avatarBase64.take(62_000))
            put("updated_at", Instant.now().toString())
          }.toString()
        supabaseRestWrite(
          accessToken = accessToken,
          method = "POST",
          pathAndQuery = "/rest/v1/pilot_profiles?on_conflict=user_id",
          payloadJson = payload,
          preferHeader = "resolution=merge-duplicates,return=minimal",
        )
      }
    }

  suspend fun fetchSupabaseRadarRiders(accessToken: String, selfUid: String = ""): Result<List<RadarRider>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val arr =
          supabaseRestGetArray(
            accessToken = accessToken,
            primaryPathAndQuery = "/rest/v1/radar_riders?visibility=eq.PUBLIC&order=updated_at.desc&limit=80",
            fallbackPathAndQuery = "/rest/v1/radar_riders?select=*&limit=80",
          )
        val normSelf = toAppSbUid(selfUid)
        val nowSec = System.currentTimeMillis() / 1000L
        (0 until arr.length()).mapNotNull { i ->
          val o = arr.optJSONObject(i) ?: return@mapNotNull null
          val uid = toAppSbUid(o.optString("user_id"))
          val cs = o.optString("callsign").trim()
          val status = o.optString("status_tag", "CRUISING")
          val lat = o.optDouble("lat", 0.0)
          val lng = o.optDouble("lng", 0.0)
          val updatedTs = parseSupabaseTimestamp(o.optString("updated_at"))
          val isSelf = normSelf.isNotBlank() && uid == normSelf
          val isRecent = updatedTs == null || (nowSec - updatedTs.seconds) <= 7200L
          if (status == "OFFLINE" || (lat == 0.0 && lng == 0.0)) return@mapNotNull null
          if (!isSelf && (!isRecent || !isAuthenticSupabasePilot(uid, cs))) return@mapNotNull null
          RadarRider(
            id = uid,
            userId = uid,
            callsign = cs,
            vehicleName = o.optString("vehicle_name", "PEV Rig"),
            lat = lat,
            lng = lng,
            speedKmh = o.optDouble("speed_kmh", 0.0),
            headingDeg = o.optDouble("heading_deg", 0.0),
            batteryPct = o.optLong("battery_pct", 100L),
            statusTag = status,
            visibility = o.optString("visibility", "PUBLIC"),
            updatedAt = updatedTs,
          )
        }
      }
    }

  suspend fun upsertSupabaseRadarRider(accessToken: String, rider: RadarRider): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val uid = toAppSbUid(rider.userId.ifBlank { rider.id })
        if (uid.isBlank()) error("Missing radar rider user_id")
        val payload =
          JSONObject().apply {
            put("user_id", uid)
            put("callsign", rider.callsign.trim().take(32).ifBlank { "PILOT" })
            put("vehicle_name", rider.vehicleName.trim().take(60).ifBlank { "PEV Rig" })
            put("lat", rider.lat.coerceIn(-90.0, 90.0))
            put("lng", rider.lng.coerceIn(-180.0, 180.0))
            put("speed_kmh", rider.speedKmh.coerceIn(0.0, 250.0))
            put("heading_deg", rider.headingDeg.coerceIn(0.0, 360.0))
            put("battery_pct", rider.batteryPct.coerceIn(0L, 100L))
            put("status_tag", rider.statusTag.take(24).ifBlank { "CRUISING" })
            put("visibility", rider.visibility.ifBlank { "PUBLIC" })
            put("updated_at", Instant.now().toString())
          }.toString()
        supabaseRestWrite(
          accessToken = accessToken,
          method = "POST",
          pathAndQuery = "/rest/v1/radar_riders?on_conflict=user_id",
          payloadJson = payload,
          preferHeader = "resolution=merge-duplicates,return=minimal",
        )
      }
    }

  suspend fun fetchSupabaseRadarMeetups(accessToken: String): Result<List<RadarMeetup>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val arr =
          supabaseRestGetArray(
            accessToken = accessToken,
            primaryPathAndQuery = "/rest/v1/radar_meetups?visibility=eq.PUBLIC&order=created_at.desc&limit=40",
            fallbackPathAndQuery = "/rest/v1/radar_meetups?select=*&limit=40",
          )
        (0 until arr.length()).mapNotNull { i ->
          val o = arr.optJSONObject(i) ?: return@mapNotNull null
          val uid = toAppSbUid(o.optString("user_id"))
          val cs = o.optString("creator_callsign")
          if (!isAuthenticSupabasePilot(uid, cs)) return@mapNotNull null
          RadarMeetup(
            id = o.optString("id", "meet_$i"),
            userId = uid,
            creatorCallsign = cs,
            title = o.optString("title", "Group Ride"),
            description = o.optString("description"),
            scheduledTime = o.optString("scheduled_time", "Soon"),
            lat = o.optDouble("lat", 0.0),
            lng = o.optDouble("lng", 0.0),
            attendeeCount = o.optLong("attendee_count", 1L),
            visibility = o.optString("visibility", "PUBLIC"),
            createdAt = parseSupabaseTimestamp(o.optString("created_at")),
          )
        }
      }
    }

  suspend fun insertSupabaseRadarMeetup(accessToken: String, meetup: RadarMeetup): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val uid = toAppSbUid(meetup.userId)
        val mid = meetup.id.ifBlank { "meet_${java.util.UUID.randomUUID().toString().take(8)}" }
        val payload =
          JSONObject().apply {
            put("id", mid)
            put("user_id", uid)
            put("creator_callsign", meetup.creatorCallsign.take(32).ifBlank { "PILOT" })
            put("title", meetup.title.trim().take(100).ifBlank { "Group Ride" })
            put("description", meetup.description.trim().take(400))
            put("scheduled_time", meetup.scheduledTime.trim().take(60).ifBlank { "Soon" })
            put("lat", meetup.lat)
            put("lng", meetup.lng)
            put("attendee_count", meetup.attendeeCount.coerceAtLeast(1L))
            put("visibility", "PUBLIC")
          }.toString()
        supabaseRestWrite(accessToken, "POST", "/rest/v1/radar_meetups", payload)
      }
    }

  suspend fun rsvpSupabaseRadarMeetup(accessToken: String, meetupId: String, nextCount: Long): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val payload = JSONObject().apply { put("attendee_count", nextCount.coerceAtLeast(1L)) }.toString()
        supabaseRestWrite(
          accessToken = accessToken,
          method = "PATCH",
          pathAndQuery = "/rest/v1/radar_meetups?id=eq.${URLEncoder.encode(meetupId, "UTF-8")}",
          payloadJson = payload,
          preferHeader = "",
        )
      }
    }

  suspend fun fetchSupabaseRadarHazards(accessToken: String): Result<List<RadarHazard>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val arr =
          supabaseRestGetArray(
            accessToken = accessToken,
            primaryPathAndQuery = "/rest/v1/radar_hazards?visibility=eq.PUBLIC&order=created_at.desc&limit=50",
            fallbackPathAndQuery = "/rest/v1/radar_hazards?select=*&limit=50",
          )
        (0 until arr.length()).mapNotNull { i ->
          val o = arr.optJSONObject(i) ?: return@mapNotNull null
          val uid = toAppSbUid(o.optString("user_id"))
          val cs = o.optString("creator_callsign")
          if (!isAuthenticSupabasePilot(uid, cs)) return@mapNotNull null
          RadarHazard(
            id = o.optString("id", "haz_$i"),
            userId = uid,
            creatorCallsign = cs,
            hazardType = o.optString("hazard_type", "POTHOLE"),
            description = o.optString("description"),
            lat = o.optDouble("lat", 0.0),
            lng = o.optDouble("lng", 0.0),
            upvotes = o.optLong("upvotes", 1L),
            visibility = o.optString("visibility", "PUBLIC"),
            createdAt = parseSupabaseTimestamp(o.optString("created_at")),
          )
        }
      }
    }

  suspend fun insertSupabaseRadarHazard(accessToken: String, hazard: RadarHazard): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val uid = toAppSbUid(hazard.userId)
        val hid = hazard.id.ifBlank { "haz_${java.util.UUID.randomUUID().toString().take(8)}" }
        val payload =
          JSONObject().apply {
            put("id", hid)
            put("user_id", uid)
            put("creator_callsign", hazard.creatorCallsign.take(32).ifBlank { "PILOT" })
            put("hazard_type", hazard.hazardType.take(24))
            put("description", hazard.description.trim().take(300))
            put("lat", hazard.lat)
            put("lng", hazard.lng)
            put("upvotes", hazard.upvotes.coerceAtLeast(1L))
            put("visibility", "PUBLIC")
          }.toString()
        supabaseRestWrite(accessToken, "POST", "/rest/v1/radar_hazards", payload)
      }
    }

  suspend fun upvoteSupabaseRadarHazard(accessToken: String, hazardId: String, nextUpvotes: Long): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val payload = JSONObject().apply { put("upvotes", nextUpvotes.coerceAtLeast(1L)) }.toString()
        supabaseRestWrite(
          accessToken = accessToken,
          method = "PATCH",
          pathAndQuery = "/rest/v1/radar_hazards?id=eq.${URLEncoder.encode(hazardId, "UTF-8")}",
          payloadJson = payload,
          preferHeader = "",
        )
      }
    }

  suspend fun fetchSupabaseRadarWaypoints(accessToken: String): Result<List<RadarWaypoint>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val arr =
          supabaseRestGetArray(
            accessToken = accessToken,
            primaryPathAndQuery = "/rest/v1/radar_waypoints?visibility=eq.PUBLIC&order=created_at.desc&limit=50",
            fallbackPathAndQuery = "/rest/v1/radar_waypoints?select=*&limit=50",
          )
        (0 until arr.length()).mapNotNull { i ->
          val o = arr.optJSONObject(i) ?: return@mapNotNull null
          val uid = toAppSbUid(o.optString("user_id"))
          val cs = o.optString("creator_callsign")
          if (!isAuthenticSupabasePilot(uid, cs)) return@mapNotNull null
          RadarWaypoint(
            id = o.optString("id", "wp_$i"),
            userId = uid,
            creatorCallsign = cs,
            waypointType = o.optString("waypoint_type", "CHARGER"),
            title = o.optString("title", "Trail Waypoint"),
            notes = o.optString("notes"),
            lat = o.optDouble("lat", 0.0),
            lng = o.optDouble("lng", 0.0),
            visibility = o.optString("visibility", "PUBLIC"),
            createdAt = parseSupabaseTimestamp(o.optString("created_at")),
          )
        }
      }
    }

  suspend fun insertSupabaseRadarWaypoint(accessToken: String, wp: RadarWaypoint): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val uid = toAppSbUid(wp.userId)
        val wid = wp.id.ifBlank { "wp_${java.util.UUID.randomUUID().toString().take(8)}" }
        val payload =
          JSONObject().apply {
            put("id", wid)
            put("user_id", uid)
            put("creator_callsign", wp.creatorCallsign.take(32).ifBlank { "PILOT" })
            put("waypoint_type", wp.waypointType.take(24))
            put("title", wp.title.trim().take(80).ifBlank { "Trail Waypoint" })
            put("notes", wp.notes.trim().take(300))
            put("lat", wp.lat)
            put("lng", wp.lng)
            put("visibility", "PUBLIC")
          }.toString()
        supabaseRestWrite(accessToken, "POST", "/rest/v1/radar_waypoints", payload)
      }
    }

  suspend fun fetchSupabaseDmThreads(accessToken: String, myUid: String): Result<List<DmThread>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val normMe = toAppSbUid(myUid)
        if (normMe.isBlank()) return@runCatching emptyList()
        val arr =
          supabaseRestGetArray(
            accessToken = accessToken,
            primaryPathAndQuery = "/rest/v1/dm_threads?select=*&order=updated_at.desc&limit=60",
            fallbackPathAndQuery = "/rest/v1/dm_threads?select=*&limit=60",
          )
        (0 until arr.length()).mapNotNull { i ->
          val o = arr.optJSONObject(i) ?: return@mapNotNull null
          val uids = parseJsonStringList(o.optJSONArray("participant_uids")).map { toAppSbUid(it) }
          if (normMe !in uids) return@mapNotNull null
          val callsigns = parseJsonStringList(o.optJSONArray("participant_callsigns"))
          val otherIdx = uids.indexOfFirst { it != normMe }
          if (otherIdx >= 0 && !isAuthenticSupabasePilot(uids[otherIdx], callsigns.getOrElse(otherIdx) { "" })) {
            return@mapNotNull null
          }
          DmThread(
            id = o.optString("id"),
            participantUids = uids,
            participantCallsigns = callsigns,
            lastMessage = o.optString("last_message"),
            selfDestructSec = o.optLong("self_destruct_sec", 0L),
            updatedAt = parseSupabaseTimestamp(o.optString("updated_at")),
          )
        }
      }
    }

  suspend fun fetchSupabaseDmMessages(accessToken: String, threadId: String): Result<List<DmMessage>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val encTid = URLEncoder.encode(threadId, "UTF-8")
        val arr =
          supabaseRestGetArray(
            accessToken = accessToken,
            primaryPathAndQuery = "/rest/v1/dm_messages?thread_id=eq.$encTid&order=created_at.asc&limit=150",
            fallbackPathAndQuery = "/rest/v1/dm_messages?thread_id=eq.$encTid&limit=150",
          )
        (0 until arr.length()).mapNotNull { i ->
          val o = arr.optJSONObject(i) ?: return@mapNotNull null
          DmMessage(
            id = o.optString("id", "msg_$i"),
            participantUids = parseJsonStringList(o.optJSONArray("participant_uids")).map { toAppSbUid(it) },
            senderId = toAppSbUid(o.optString("sender_id")),
            senderCallsign = o.optString("sender_callsign", "PILOT"),
            text = o.optString("text"),
            imageUrl = o.optString("image_url"),
            voiceBase64 = o.optString("voice_base64"),
            selfDestructSec = o.optLong("self_destruct_sec", 0L),
            createdAt = parseSupabaseTimestamp(o.optString("created_at")),
          )
        }
      }
    }

  suspend fun sendSupabaseDm(
    accessToken: String,
    myUid: String,
    myCallsign: String,
    peerUid: String,
    peerCallsign: String,
    text: String,
    imageUrl: String = "",
    voiceBase64: String = "",
    selfDestructSec: Long = 0L,
  ): Result<String> =
    withContext(Dispatchers.IO) {
      runCatching {
        val me = toAppSbUid(myUid)
        val peer = toAppSbUid(peerUid)
        val sorted = listOf(me, peer).sorted()
        val sortedCallsigns =
          if (sorted[0] == me) listOf(myCallsign.take(32), peerCallsign.take(32))
          else listOf(peerCallsign.take(32), myCallsign.take(32))
        val threadId = "${sorted[0]}_${sorted[1]}"
        val preview = text.ifBlank { if (voiceBase64.isNotBlank()) "Voice Transmission" else "Attachment" }
        val threadPayload =
          JSONObject().apply {
            put("id", threadId)
            put("participant_uids", JSONArray(sorted))
            put("participant_callsigns", JSONArray(sortedCallsigns))
            put("last_message", preview.take(300))
            put("self_destruct_sec", selfDestructSec.coerceIn(0L, 604800L))
            put("updated_at", Instant.now().toString())
          }.toString()
        supabaseRestWrite(
          accessToken = accessToken,
          method = "POST",
          pathAndQuery = "/rest/v1/dm_threads?on_conflict=id",
          payloadJson = threadPayload,
          preferHeader = "resolution=merge-duplicates,return=minimal",
        )
        val msgId = "msg_${java.util.UUID.randomUUID().toString().take(8)}"
        val msgPayload =
          JSONObject().apply {
            put("id", msgId)
            put("thread_id", threadId)
            put("participant_uids", JSONArray(sorted))
            put("sender_id", me)
            put("sender_callsign", myCallsign.take(32).ifBlank { "PILOT" })
            put("text", text.take(2000))
            put("image_url", imageUrl.take(175_000))
            put("voice_base64", voiceBase64.take(340_000))
            put("self_destruct_sec", selfDestructSec.coerceIn(0L, 604800L))
          }.toString()
        supabaseRestWrite(accessToken, "POST", "/rest/v1/dm_messages", msgPayload)
        threadId
      }
    }

  suspend fun deleteSupabaseDmMessage(accessToken: String, msgId: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        supabaseRestWrite(
          accessToken = accessToken,
          method = "DELETE",
          pathAndQuery = "/rest/v1/dm_messages?id=eq.${URLEncoder.encode(msgId, "UTF-8")}",
          preferHeader = "",
        )
      }
    }

  suspend fun fetchSupabaseWalkiePresence(accessToken: String, channelId: String): Result<List<ChannelPresence>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val encCh = URLEncoder.encode(channelId, "UTF-8")
        val arr =
          supabaseRestGetArray(
            accessToken = accessToken,
            primaryPathAndQuery = "/rest/v1/walkie_presence?channel_id=eq.$encCh&active=eq.true",
          )
        (0 until arr.length()).mapNotNull { i ->
          val o = arr.optJSONObject(i) ?: return@mapNotNull null
          val uid = toAppSbUid(o.optString("user_id"))
          val cs = o.optString("callsign")
          if (!isAuthenticSupabasePilot(uid, cs)) return@mapNotNull null
          ChannelPresence(
            id = o.optString("id"),
            userId = uid,
            callsign = cs,
            channelId = o.optString("channel_id", channelId),
            active = o.optBoolean("active", true),
            updatedAt = parseSupabaseTimestamp(o.optString("updated_at")),
          )
        }
      }
    }

  suspend fun upsertSupabaseWalkiePresence(
    accessToken: String,
    channelId: String,
    uid: String,
    callsign: String,
    active: Boolean,
  ): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val normUid = toAppSbUid(uid)
        val docId = "${channelId}_$normUid"
        val payload =
          JSONObject().apply {
            put("id", docId)
            put("user_id", normUid)
            put("callsign", callsign.take(32).ifBlank { "PILOT" })
            put("channel_id", channelId)
            put("active", active)
            put("updated_at", Instant.now().toString())
          }.toString()
        supabaseRestWrite(
          accessToken = accessToken,
          method = "POST",
          pathAndQuery = "/rest/v1/walkie_presence?on_conflict=id",
          payloadJson = payload,
          preferHeader = "resolution=merge-duplicates,return=minimal",
        )
      }
    }

  suspend fun fetchSupabaseWalkieSpeaker(accessToken: String, channelId: String): Result<ChannelSpeakerState?> =
    withContext(Dispatchers.IO) {
      runCatching {
        val encCh = URLEncoder.encode(channelId, "UTF-8")
        val arr =
          supabaseRestGetArray(
            accessToken = accessToken,
            primaryPathAndQuery = "/rest/v1/walkie_channel_state?channel_id=eq.$encCh&limit=1",
          )
        val o = arr.optJSONObject(0) ?: return@runCatching null
        ChannelSpeakerState(
          id = channelId,
          speakerUid = toAppSbUid(o.optString("speaker_uid")),
          speakerCallsign = o.optString("speaker_callsign"),
          channelId = o.optString("channel_id", channelId),
          active = o.optBoolean("active", false),
          updatedAt = parseSupabaseTimestamp(o.optString("updated_at")),
        )
      }
    }

  suspend fun upsertSupabaseWalkieSpeaker(
    accessToken: String,
    channelId: String,
    uid: String,
    callsign: String,
    speaking: Boolean,
  ): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val payload =
          JSONObject().apply {
            put("channel_id", channelId)
            put("speaker_uid", if (speaking) toAppSbUid(uid) else "")
            put("speaker_callsign", if (speaking) callsign.take(32) else "")
            put("active", speaking)
            put("updated_at", Instant.now().toString())
          }.toString()
        supabaseRestWrite(
          accessToken = accessToken,
          method = "POST",
          pathAndQuery = "/rest/v1/walkie_channel_state?on_conflict=channel_id",
          payloadJson = payload,
          preferHeader = "resolution=merge-duplicates,return=minimal",
        )
      }
    }

  suspend fun fetchSupabaseWalkieLogs(accessToken: String, channelId: String): Result<List<WalkieVoiceLog>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val encCh = URLEncoder.encode(channelId, "UTF-8")
        val arr =
          supabaseRestGetArray(
            accessToken = accessToken,
            primaryPathAndQuery = "/rest/v1/walkie_logs?channel_id=eq.$encCh&active=eq.true&order=created_at.desc&limit=25",
            fallbackPathAndQuery = "/rest/v1/walkie_logs?channel_id=eq.$encCh&limit=25",
          )
        (0 until arr.length()).mapNotNull { i ->
          val o = arr.optJSONObject(i) ?: return@mapNotNull null
          WalkieVoiceLog(
            id = o.optString("id", "wl_$i"),
            userId = toAppSbUid(o.optString("user_id")),
            senderCallsign = o.optString("sender_callsign", "PILOT"),
            channelId = o.optString("channel_id", channelId),
            audioBase64 = o.optString("audio_base64"),
            durationMs = o.optLong("duration_ms", 1000L),
            active = o.optBoolean("active", true),
            createdAt = parseSupabaseTimestamp(o.optString("created_at")),
          )
        }
      }
    }

  suspend fun insertSupabaseWalkieLog(accessToken: String, log: WalkieVoiceLog): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val payload =
          JSONObject().apply {
            put("id", log.id.ifBlank { "ptt_${java.util.UUID.randomUUID().toString().take(8)}" })
            put("user_id", toAppSbUid(log.userId))
            put("sender_callsign", log.senderCallsign.take(32).ifBlank { "PILOT" })
            put("channel_id", log.channelId)
            put("audio_base64", log.audioBase64.take(350_000))
            put("duration_ms", log.durationMs.coerceIn(150L, 12_000L))
            put("active", true)
          }.toString()
        supabaseRestWrite(accessToken, "POST", "/rest/v1/walkie_logs", payload)
      }
    }

  suspend fun fetchSupabaseWalkieSignals(accessToken: String, channelId: String): Result<List<ChannelWebRtcSignal>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val encCh = URLEncoder.encode(channelId, "UTF-8")
        val arr =
          supabaseRestGetArray(
            accessToken = accessToken,
            primaryPathAndQuery = "/rest/v1/walkie_signals?channel_id=eq.$encCh&active=eq.true&order=created_at.asc&limit=80",
            fallbackPathAndQuery = "/rest/v1/walkie_signals?channel_id=eq.$encCh&limit=80",
          )
        (0 until arr.length()).mapNotNull { i ->
          val o = arr.optJSONObject(i) ?: return@mapNotNull null
          ChannelWebRtcSignal(
            id = o.optString("id", "wsig_$i"),
            senderId = toAppSbUid(o.optString("sender_id")),
            senderCallsign = o.optString("sender_callsign", "PILOT"),
            targetUid = toAppSbUid(o.optString("target_uid")),
            channelId = o.optString("channel_id", channelId),
            signalType = o.optString("signal_type", "ICE"),
            payload = o.optString("payload"),
            active = o.optBoolean("active", true),
            createdAt = parseSupabaseTimestamp(o.optString("created_at")),
          )
        }
      }
    }

  suspend fun insertSupabaseWalkieSignal(accessToken: String, sig: ChannelWebRtcSignal): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val cleanType = if (sig.signalType in listOf("JOIN", "OFFER", "ANSWER", "ICE", "LEAVE")) sig.signalType else "ICE"
        val target = if (sig.targetUid == "ALL" || sig.targetUid.isBlank()) "ALL" else toAppSbUid(sig.targetUid)
        val payload =
          JSONObject().apply {
            put("id", sig.id.ifBlank { "rtc_${java.util.UUID.randomUUID().toString().take(10)}" })
            put("sender_id", toAppSbUid(sig.senderId))
            put("sender_callsign", sig.senderCallsign.take(32).ifBlank { "PILOT" })
            put("target_uid", target)
            put("channel_id", sig.channelId)
            put("signal_type", cleanType)
            put("payload", sig.payload.take(19_500))
            put("active", true)
          }.toString()
        supabaseRestWrite(accessToken, "POST", "/rest/v1/walkie_signals", payload)
      }
    }

  suspend fun clearSupabaseWalkieSignals(accessToken: String, channelId: String, myUid: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val encCh = URLEncoder.encode(channelId, "UTF-8")
        val encUid = URLEncoder.encode(toAppSbUid(myUid), "UTF-8")
        supabaseRestWrite(
          accessToken = accessToken,
          method = "DELETE",
          pathAndQuery = "/rest/v1/walkie_signals?channel_id=eq.$encCh&sender_id=eq.$encUid",
          preferHeader = "",
        )
      }
    }

  suspend fun fetchSupabaseCallSessions(accessToken: String, myUid: String): Result<List<CallSession>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val me = toAppSbUid(myUid)
        if (me.isBlank()) return@runCatching emptyList()
        val arr =
          supabaseRestGetArray(
            accessToken = accessToken,
            primaryPathAndQuery = "/rest/v1/call_sessions?select=*&order=updated_at.desc&limit=50",
            fallbackPathAndQuery = "/rest/v1/call_sessions?select=*&limit=50",
          )
        (0 until arr.length()).mapNotNull { i ->
          val o = arr.optJSONObject(i) ?: return@mapNotNull null
          val caller = toAppSbUid(o.optString("caller_uid"))
          val callee = toAppSbUid(o.optString("callee_uid"))
          val callerCs = o.optString("caller_callsign", "PILOT")
          val calleeCs = o.optString("callee_callsign", "PILOT")
          val participants = parseJsonStringList(o.optJSONArray("participant_uids")).map { toAppSbUid(it) }
          if (me != caller && me != callee && me !in participants) return@mapNotNull null
          val otherUid = if (me == caller) callee else caller
          val otherCs = if (me == caller) calleeCs else callerCs
          if (otherUid.isNotBlank() && !isAuthenticSupabasePilot(otherUid, otherCs, me)) return@mapNotNull null
          CallSession(
            id = o.optString("id"),
            callerUid = caller,
            callerCallsign = callerCs,
            calleeUid = callee,
            calleeCallsign = calleeCs,
            callType = o.optString("call_type", "AUDIO"),
            status = o.optString("status", "ENDED"),
            createdAt = parseSupabaseTimestamp(o.optString("created_at")),
            updatedAt = parseSupabaseTimestamp(o.optString("updated_at")),
          )
        }
      }
    }

  suspend fun insertSupabaseCallSession(accessToken: String, session: CallSession): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val caller = toAppSbUid(session.callerUid)
        val callee = toAppSbUid(session.calleeUid)
        val sid = session.id.ifBlank { "call_${java.util.UUID.randomUUID().toString().take(8)}" }
        val payload =
          JSONObject().apply {
            put("id", sid)
            put("caller_uid", caller)
            put("caller_callsign", session.callerCallsign.take(32).ifBlank { "PILOT" })
            put("callee_uid", callee)
            put("callee_callsign", session.calleeCallsign.take(32).ifBlank { "PILOT" })
            put("participant_uids", JSONArray(listOf(caller, callee)))
            put("call_type", if (session.callType == "VIDEO") "VIDEO" else "AUDIO")
            put("status", session.status.ifBlank { "RINGING" })
            put("updated_at", Instant.now().toString())
          }.toString()
        supabaseRestWrite(accessToken, "POST", "/rest/v1/call_sessions", payload)
      }
    }

  suspend fun updateSupabaseCallStatus(accessToken: String, sessionId: String, nextStatus: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val clean = if (nextStatus in listOf("RINGING", "ACTIVE", "ENDED")) nextStatus else "ENDED"
        val payload =
          JSONObject().apply {
            put("status", clean)
            put("updated_at", Instant.now().toString())
          }.toString()
        supabaseRestWrite(
          accessToken = accessToken,
          method = "PATCH",
          pathAndQuery = "/rest/v1/call_sessions?id=eq.${URLEncoder.encode(sessionId, "UTF-8")}",
          payloadJson = payload,
          preferHeader = "",
        )
      }
    }

  suspend fun updateSupabaseCallType(accessToken: String, sessionId: String, nextType: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val clean = if (nextType == "VIDEO") "VIDEO" else "AUDIO"
        val payload =
          JSONObject().apply {
            put("call_type", clean)
            put("updated_at", Instant.now().toString())
          }.toString()
        supabaseRestWrite(
          accessToken = accessToken,
          method = "PATCH",
          pathAndQuery = "/rest/v1/call_sessions?id=eq.${URLEncoder.encode(sessionId, "UTF-8")}",
          payloadJson = payload,
          preferHeader = "",
        )
      }
    }

  suspend fun fetchSupabaseCallSignals(accessToken: String, sessionId: String): Result<List<CallSignal>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val encSid = URLEncoder.encode(sessionId, "UTF-8")
        val arr =
          supabaseRestGetArray(
            accessToken = accessToken,
            primaryPathAndQuery = "/rest/v1/call_signals?session_id=eq.$encSid&order=created_at.asc&limit=120",
            fallbackPathAndQuery = "/rest/v1/call_signals?session_id=eq.$encSid&limit=120",
          )
        (0 until arr.length()).mapNotNull { i ->
          val o = arr.optJSONObject(i) ?: return@mapNotNull null
          CallSignal(
            id = o.optString("id", "csig_$i"),
            senderId = toAppSbUid(o.optString("sender_id")),
            signalType = o.optString("signal_type", "ICE"),
            payload = o.optString("payload"),
            createdAt = parseSupabaseTimestamp(o.optString("created_at")),
          )
        }
      }
    }

  suspend fun insertSupabaseCallSignal(accessToken: String, sessionId: String, sig: CallSignal): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val cleanType = if (sig.signalType in listOf("OFFER", "ANSWER", "ICE", "CHAT", "PING")) sig.signalType else "ICE"
        val payload =
          JSONObject().apply {
            put("id", sig.id.ifBlank { "sig_${java.util.UUID.randomUUID().toString().take(10)}" })
            put("session_id", sessionId)
            put("sender_id", toAppSbUid(sig.senderId))
            put("signal_type", cleanType)
            put("payload", sig.payload.take(19_500))
          }.toString()
        supabaseRestWrite(accessToken, "POST", "/rest/v1/call_signals", payload)
      }
    }

  suspend fun fetchSupabaseYoutubeEngagement(accessToken: String, videoId: String): Result<Long> =
    withContext(Dispatchers.IO) {
      runCatching {
        val safeId = videoId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64)
        val arr =
          supabaseRestGetArray(
            accessToken = accessToken,
            primaryPathAndQuery = "/rest/v1/youtube_engagement?video_id=eq.${URLEncoder.encode(safeId, "UTF-8")}&limit=1",
          )
        arr.optJSONObject(0)?.optLong("volts_count", 0L) ?: 0L
      }
    }

  suspend fun voltSupabaseYoutubeVideo(accessToken: String, videoId: String): Result<Long> =
    withContext(Dispatchers.IO) {
      runCatching {
        val safeId = videoId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64)
        val current = fetchSupabaseYoutubeEngagement(accessToken, safeId).getOrDefault(0L)
        val next = current + 1L
        val payload =
          JSONObject().apply {
            put("video_id", safeId)
            put("volts_count", next)
            put("updated_at", Instant.now().toString())
          }.toString()
        supabaseRestWrite(
          accessToken = accessToken,
          method = "POST",
          pathAndQuery = "/rest/v1/youtube_engagement?on_conflict=video_id",
          payloadJson = payload,
          preferHeader = "resolution=merge-duplicates,return=minimal",
        )
        next
      }
    }

  suspend fun fetchSupabaseYoutubeComments(accessToken: String, videoId: String): Result<List<YoutubeComment>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val safeId = videoId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64)
        val arr =
          supabaseRestGetArray(
            accessToken = accessToken,
            primaryPathAndQuery = "/rest/v1/youtube_comments?video_id=eq.${URLEncoder.encode(safeId, "UTF-8")}&order=created_at.desc&limit=60",
            fallbackPathAndQuery = "/rest/v1/youtube_comments?video_id=eq.${URLEncoder.encode(safeId, "UTF-8")}&limit=60",
          )
        (0 until arr.length()).mapNotNull { i ->
          val o = arr.optJSONObject(i) ?: return@mapNotNull null
          YoutubeComment(
            id = o.optString("id", "yc_$i"),
            userId = toAppSbUid(o.optString("user_id")),
            authorCallsign = o.optString("author_callsign", "PILOT"),
            text = o.optString("text"),
            createdAt = parseSupabaseTimestamp(o.optString("created_at")),
          )
        }
      }
    }

  suspend fun insertSupabaseYoutubeComment(
    accessToken: String,
    videoId: String,
    userId: String,
    callsign: String,
    text: String,
  ): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val safeId = videoId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64)
        val payload =
          JSONObject().apply {
            put("video_id", safeId)
            put("user_id", toAppSbUid(userId))
            put("author_callsign", callsign.take(32).ifBlank { "PILOT" })
            put("text", text.trim().take(1000))
          }.toString()
        supabaseRestWrite(accessToken, "POST", "/rest/v1/youtube_comments", payload)
      }
    }

  // --------------------------------------------------------------------------
  // 5. STRICTLY PEV-ONLY & LOCATION-AWARE NEWS FEED (RSS + OPTIONAL NEWSAPI)
  // --------------------------------------------------------------------------
  private val pevIncludeTerms =
    listOf(
      "e-bike", "ebike", "electric bike", "electric bicycle",
      "e-scooter", "escooter", "electric scooter", "kickscooter",
      "electric unicycle", "euc", "onewheel", "floatwheel",
      "surron", "sur-ron", "talaria", "e-ride pro", "eride pro",
      "e-moto", "emoto", "electric dirt bike", "electric moped", "e-moped",
      "electric skateboard", "eskate", "e-skate", "e-trike", "etrike", "electric trike",
      "personal electric vehicle", "pev", "micromobility", "bike lane",
      "segway", "ninebot", "dualtron", "kaabo", "nami burn", "inmotion",
      "begode", "leaperkim", "kingsong", "aventon", "rad power", "lectric",
      "super73", "apollo scooter", "vsett", "vesc", "ul 2849", "ul 2272",
    )

  private val autoExcludeTerms =
    listOf(
      "tesla", "cybertruck", "model y", "model 3", "model s", "model x",
      "rivian", "lucid", "ford f-150", "mustang mach-e", "chevy equinox",
      "hyundai ioniq", "kia ev6", "kia ev9", "polestar", "waymo", "robotaxi",
      "supercharger", "nacs", "byd seal", "volkswagen id.", "bmw i4", "mercedes eq",
      "porsche taycan", "pickup truck", "sedan", "suv",
    )

  internal fun isStrictlyPevArticle(title: String, summary: String): Boolean {
    val combined = "${title.lowercase()} ${summary.lowercase()}"
    val hasPev = pevIncludeTerms.any { term -> combined.contains(term) }
    if (!hasPev) return false
    val titleLower = title.lowercase()
    val titleHasPev = pevIncludeTerms.any { term -> titleLower.contains(term) }
    val hasCarOnly = autoExcludeTerms.any { term -> titleLower.contains(term) }
    return titleHasPev || !hasCarOnly
  }

  private fun buildPevNewsQuery(category: String, locationLabel: String, customTopic: String): String {
    val baseCat =
      when (category.uppercase()) {
        "E-BIKES" -> "(ebike OR \"electric bike\" OR \"e-bike\")"
        "E-SCOOTERS" -> "(\"electric scooter\" OR escooter OR \"e-scooter\")"
        "EUCS & ONEWHEEL" -> "(\"electric unicycle\" OR onewheel OR floatwheel OR EUC)"
        "E-MOTOS" -> "(Sur-Ron OR Talaria OR \"electric dirt bike\" OR \"e-moto\")"
        "LAWS & TRAILS" -> "(ebike OR \"electric scooter\" OR PEV) (law OR regulation OR trail OR helmet OR UL2849)"
        else -> "(\"e-bike\" OR \"electric scooter\" OR \"electric unicycle\" OR onewheel OR micromobility OR Sur-Ron)"
      }
    val parts = mutableListOf(baseCat)
    if (customTopic.isNotBlank()) parts += customTopic.trim()
    val cleanLoc = locationLabel.trim().substringBefore(",").trim()
    if (cleanLoc.isNotBlank() && !cleanLoc.equals("Global", ignoreCase = true) && !cleanLoc.startsWith("Awaiting")) {
      parts += "\"$cleanLoc\""
    }
    return parts.joinToString(" ")
  }

  suspend fun fetchLivePevNews(
    topic: String = "",
    category: String = "ALL PEV",
    locationLabel: String = "",
  ): Result<List<NewsArticleItem>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val rawArticles = mutableListOf<NewsArticleItem>()
        val query = buildPevNewsQuery(category, locationLabel, topic)

        // 1. Optional NewsAPI query (strictly PEV scoped, redundant key failover)
        val newsKeys = ApiKeyRing.newsKeys()
        if (newsKeys.isNotEmpty()) {
          runCatching {
            val q = URLEncoder.encode(query, "UTF-8")
            ApiKeyRing.executeWithFailover("NEWSAPI", newsKeys) { rawKey ->
              val key = URLEncoder.encode(rawKey, "UTF-8")
              val url =
                "https://newsapi.org/v2/everything?q=$q&language=en&sortBy=publishedAt&pageSize=20&apiKey=$key"
              httpClient.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
                if (ApiKeyRing.isFailoverCode(resp.code)) throw ApiKeyFailoverException(resp.code, "NewsAPI HTTP ${resp.code}")
                if (resp.isSuccessful) {
                  val arr = JSONObject(resp.body?.string().orEmpty()).optJSONArray("articles")
                  if (arr != null) {
                    for (i in 0 until arr.length()) {
                      val o = arr.optJSONObject(i) ?: continue
                      rawArticles +=
                        NewsArticleItem(
                          title = o.optString("title"),
                          link = o.optString("url"),
                          source = o.optJSONObject("source")?.optString("name") ?: "NewsAPI",
                          publishedAt = o.optString("publishedAt").take(10),
                          summary = o.optString("description"),
                        )
                    }
                  }
                }
              }
            }
          }
        }

        // 2. Keyless Google News RSS strictly scoped to PEV category + optional user city/state
        runCatching {
          val encodedQ = URLEncoder.encode(query, "UTF-8")
          val gnUrl = "https://news.google.com/rss/search?q=$encodedQ&hl=en-US&gl=US&ceid=US:en"
          httpClient.newCall(
            Request.Builder().url(gnUrl).header("User-Agent", "RuralERidesGO/2.0").get().build()
          ).execute().use { resp ->
            if (resp.isSuccessful) {
              val xml = resp.body?.string().orEmpty()
              rawArticles += parseStandardRss(xml, if (locationLabel.isNotBlank()) "Local PEV Wire" else "PEV Wire").take(18)
            }
          }
        }

        // 3. Dedicated Electrek e-Bike / Micromobility RSS (not main EV-car feed)
        if (rawArticles.size < 6) {
          runCatching {
            val rssUrl = "https://electrek.co/guides/ebike/feed/"
            httpClient.newCall(
              Request.Builder().url(rssUrl).header("User-Agent", "RuralERidesGO/2.0").get().build()
            ).execute().use { resp ->
              if (resp.isSuccessful) {
                rawArticles += parseStandardRss(resp.body?.string().orEmpty(), "Electrek eBike").take(15)
              }
            }
          }
        }

        // Strictly filter to PEV-only articles & deduplicate
        val strictlyPev =
          rawArticles
            .filter { isStrictlyPevArticle(it.title, it.summary) }
            .distinctBy { it.title.trim().lowercase().take(55) }

        if (strictlyPev.isNotEmpty()) {
          strictlyPev.take(16)
        } else if (locationLabel.isNotBlank()) {
          // If hyperlocal city had zero PEV hits, retry without city so feed stays PEV-only and never empty
          val globalQ = URLEncoder.encode(buildPevNewsQuery(category, "", topic), "UTF-8")
          val gnFallback = "https://news.google.com/rss/search?q=$globalQ&hl=en-US&gl=US&ceid=US:en"
          val fallbackList =
            runCatching {
              httpClient.newCall(
                Request.Builder().url(gnFallback).header("User-Agent", "RuralERidesGO/2.0").get().build()
              ).execute().use { resp ->
                if (resp.isSuccessful) parseStandardRss(resp.body?.string().orEmpty(), "PEV Wire")
                else emptyList()
              }
            }.getOrDefault(emptyList())
          fallbackList.filter { isStrictlyPevArticle(it.title, it.summary) }.take(12)
        } else {
          emptyList()
        }
      }
    }

  private fun parseStandardRss(xml: String, sourceLabel: String): List<NewsArticleItem> {
    val list = mutableListOf<NewsArticleItem>()
    val parser = XmlPullParserFactory.newInstance().newPullParser()
    parser.setInput(StringReader(xml))
    var inItem = false
    var title = ""
    var link = ""
    var pubDate = ""
    var desc = ""
    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
      when (event) {
        XmlPullParser.START_TAG -> {
          when (parser.name) {
            "item" -> {
              inItem = true
              title = ""
              link = ""
              pubDate = ""
              desc = ""
            }
            "title" -> if (inItem) title = parser.nextText()
            "link" -> if (inItem && link.isBlank()) link = parser.nextText()
            "pubDate" -> if (inItem) pubDate = parser.nextText().take(16)
            "description" -> if (inItem) desc = parser.nextText().replace(Regex("<[^>]*>"), "").take(220)
          }
        }
        XmlPullParser.END_TAG -> {
          if (parser.name == "item" && inItem && title.isNotBlank()) {
            list += NewsArticleItem(title, link, sourceLabel, pubDate, desc)
            inItem = false
          }
        }
      }
      event = parser.next()
    }
    return list
  }

  // --------------------------------------------------------------------------
  // 6. LIVE WEB GROUNDING & IMAGE HOSTING
  // --------------------------------------------------------------------------
  suspend fun webSearchGrounding(query: String, deepMode: Boolean = false): String =
    withContext(Dispatchers.IO) {
      val combined = mutableListOf<String>()
      val maxPerEngine = if (deepMode) 8 else 5
      try {
        val tavilyKeys = ApiKeyRing.tavilyKeys()
        if (tavilyKeys.isNotEmpty()) {
          runCatching {
            ApiKeyRing.executeWithFailover("TAVILY", tavilyKeys) { rawKey ->
              val payload =
                JSONObject().apply {
                  put("api_key", rawKey)
                  put("query", query)
                  put("search_depth", if (deepMode) "advanced" else "basic")
                  put("max_results", maxPerEngine)
                }
              val req =
                Request.Builder()
                  .url("https://api.tavily.com/search")
                  .post(payload.toString().toRequestBody("application/json".toMediaType()))
                  .build()
              httpClient.newCall(req).execute().use { resp ->
                if (ApiKeyRing.isFailoverCode(resp.code)) throw ApiKeyFailoverException(resp.code, "Tavily HTTP ${resp.code}")
                if (resp.isSuccessful) {
                  val results = JSONObject(resp.body?.string().orEmpty()).optJSONArray("results")
                  if (results != null) {
                    for (i in 0 until results.length()) {
                      val r = results.getJSONObject(i)
                      combined += "- [Tavily] ${r.optString("title")}: ${r.optString("content").take(260)} (${r.optString("url")})"
                    }
                  }
                }
              }
            }
          }
        }
        val braveKeys = ApiKeyRing.braveKeys()
        if (braveKeys.isNotEmpty() && (deepMode || combined.isEmpty())) {
          val q = URLEncoder.encode(query, "UTF-8")
          runCatching {
            ApiKeyRing.executeWithFailover("BRAVE", braveKeys) { rawKey ->
              val req =
                Request.Builder()
                  .url("https://api.search.brave.com/res/v1/web/search?q=$q&count=$maxPerEngine")
                  .header("X-Subscription-Token", rawKey)
                  .get()
                  .build()
              httpClient.newCall(req).execute().use { resp ->
                if (ApiKeyRing.isFailoverCode(resp.code)) throw ApiKeyFailoverException(resp.code, "Brave HTTP ${resp.code}")
                if (resp.isSuccessful) {
                  val results =
                    JSONObject(resp.body?.string().orEmpty())
                      .optJSONObject("web")
                      ?.optJSONArray("results")
                  if (results != null) {
                    for (i in 0 until results.length()) {
                      val r = results.getJSONObject(i)
                      combined += "- [Brave] ${r.optString("title")}: ${r.optString("description").take(260)} (${r.optString("url")})"
                    }
                  }
                }
              }
            }
          }
        }
        val csePairs = ApiKeyRing.googleCsePairs()
        if (csePairs.isNotEmpty() && (deepMode || combined.isEmpty())) {
          val q = URLEncoder.encode(query, "UTF-8")
          runCatching {
            ApiKeyRing.executeWithCseFailover(csePairs) { rawKey, rawCx ->
              val cseUrl =
                "https://www.googleapis.com/customsearch/v1?key=${URLEncoder.encode(rawKey, "UTF-8")}&cx=${URLEncoder.encode(rawCx, "UTF-8")}&q=$q&num=${maxPerEngine.coerceAtMost(8)}"
              httpClient.newCall(Request.Builder().url(cseUrl).get().build()).execute().use { resp ->
                if (ApiKeyRing.isFailoverCode(resp.code)) throw ApiKeyFailoverException(resp.code, "Google CSE HTTP ${resp.code}")
                if (resp.isSuccessful) {
                  val items = JSONObject(resp.body?.string().orEmpty()).optJSONArray("items")
                  if (items != null) {
                    for (i in 0 until items.length()) {
                      val it = items.getJSONObject(i)
                      combined += "- [Google] ${it.optString("title")}: ${it.optString("snippet").take(240)} (${it.optString("link")})"
                    }
                  }
                }
              }
            }
          }
        }
        // 4. Keyless Live Bing Web Search RSS (works with zero API keys; rich PEV specs, parts, prices & URLs)
        if (combined.size < maxPerEngine || deepMode) {
          runCatching {
            val q = URLEncoder.encode(query.take(140), "UTF-8")
            val rssUrl = "https://www.bing.com/search?q=$q&format=rss&cc=US&setlang=en-US"
            val req =
              Request.Builder()
                .url(rssUrl)
                .header(
                  "User-Agent",
                  "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36",
                )
                .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
              if (resp.isSuccessful) {
                val xml = resp.body?.string().orEmpty()
                val items = parseStandardRss(xml, "BingWeb")
                items.take(maxPerEngine).forEach { it ->
                  val cleanLink = it.link.trim()
                  if (cleanLink.startsWith("http")) {
                    combined += "- [Web] ${it.title.trim()}: ${it.summary.trim().take(240)} ($cleanLink)"
                  }
                }
              }
            }
          }
        }

        // 5. Keyless Live DuckDuckGo HTML Search (broad independent PEV dealer & forum grounding)
        if (combined.size < (if (deepMode) 8 else 4)) {
          runCatching {
            val q = URLEncoder.encode(query.take(140), "UTF-8")
            val ddgUrl = "https://html.duckduckgo.com/html/?q=$q"
            val req =
              Request.Builder()
                .url(ddgUrl)
                .header(
                  "User-Agent",
                  "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36",
                )
                .header("Accept", "text/html,application/xhtml+xml")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
              if (resp.isSuccessful) {
                val html = resp.body?.string().orEmpty()
                val titleRe = Regex("""<a[^>]*class="result__a"[^>]*href="([^"]+)"[^>]*>(.*?)</a>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                val snipRe = Regex("""class="result__snippet"[^>]*>(.*?)</(?:a|div|span)>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                val titles = titleRe.findAll(html).toList()
                val snips = snipRe.findAll(html).map { m -> m.groupValues[1].replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim() }.toList()
                titles.take(maxPerEngine).forEachIndexed { idx, m ->
                  val rawHref = m.groupValues[1].replace("&amp;", "&")
                  val resolvedUrl =
                    Regex("""[?&]uddg=([^&]+)""").find(rawHref)?.groupValues?.get(1)?.let { enc ->
                      runCatching { java.net.URLDecoder.decode(enc, "UTF-8") }.getOrDefault("")
                    }.orEmpty().ifBlank { if (rawHref.startsWith("http")) rawHref else "" }
                  val cleanTitle = m.groupValues[2].replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
                  val snippet = snips.getOrElse(idx) { "" }.take(220)
                  if (cleanTitle.isNotBlank() && resolvedUrl.startsWith("http")) {
                    combined += "- [DDG] $cleanTitle: $snippet ($resolvedUrl)"
                  }
                }
              }
            }
          }
        }

        // 6. Keyless Wikipedia OpenSearch fallback so deep grounding always returns factual context
        if (combined.size < 3) {
          val q = URLEncoder.encode(query.take(80), "UTF-8")
          val wikiUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$q&srlimit=4&format=json"
          runCatching {
            httpClient.newCall(
              Request.Builder().url(wikiUrl).header("User-Agent", "RuralERidesGO/11.0").get().build()
            ).execute().use { resp ->
              if (resp.isSuccessful) {
                val arr = JSONObject(resp.body?.string().orEmpty()).optJSONObject("query")?.optJSONArray("search")
                if (arr != null) {
                  for (i in 0 until arr.length()) {
                    val w = arr.getJSONObject(i)
                    val clean = w.optString("snippet").replace(Regex("<[^>]+>"), "")
                    combined += "- [Wiki] ${w.optString("title")}: ${clean.take(220)}"
                  }
                }
              }
            }
          }
        }
      } catch (e: Exception) {
        Log.w(TAG, "Optional web search skipped: ${e.message}")
      }
      combined.distinct().take(if (deepMode) 18 else 8).joinToString("\n")
    }

  suspend fun uploadImageToImgbb(base64Image: String): Result<String> =
    withContext(Dispatchers.IO) {
      runCatching {
        val imgbbKeys = ApiKeyRing.imgbbKeys()
        if (imgbbKeys.isEmpty()) error("External image upload disabled on public builds; using Firestore media storage.")
        ApiKeyRing.executeWithFailover("IMGBB", imgbbKeys) { rawKey ->
          val formBody =
            okhttp3.FormBody.Builder()
              .add("key", rawKey)
              .add("image", base64Image)
              .build()
          val req = Request.Builder().url("https://api.imgbb.com/1/upload").post(formBody).build()
          val body =
            httpClient.newCall(req).execute().use { resp ->
              if (ApiKeyRing.isFailoverCode(resp.code)) throw ApiKeyFailoverException(resp.code, "ImgBB HTTP ${resp.code}")
              if (!resp.isSuccessful) error("ImgBB HTTP ${resp.code}")
              resp.body?.string().orEmpty()
            }
          val url = JSONObject(body).optJSONObject("data")?.optString("url").orEmpty()
          if (url.isBlank()) error("ImgBB did not return an image URL.")
          url
        }
      }
    }

  // --------------------------------------------------------------------------
  // 7. LIVE API & CONNECTION DIAGNOSTICS (TIMED HEALTH PINGS, ZERO SECRETS LEAKED)
  // --------------------------------------------------------------------------
  suspend fun runLiveConnectionDiagnostics(
    context: Context,
    supabaseService: SupabaseAuthService,
    firebaseSignedIn: Boolean,
    gpsActive: Boolean,
  ): List<DiagnosticCheckResult> =
    withContext(Dispatchers.IO) {
      val results = mutableListOf<DiagnosticCheckResult>()

      // 1. Active Cloud Auth (Firebase Auth + optional Supabase GoTrue when configured)
      val firebaseProjectId =
        runCatching { FirebaseApp.getInstance().options.projectId }
          .getOrNull()
          .orEmpty()
          .ifBlank { "Firebase" }
      results +=
        DiagnosticCheckResult(
          name = "Firebase Google Auth",
          category = "AUTH",
          ok = firebaseSignedIn,
          latencyMs = 0L,
          detail = if (firebaseSignedIn) "Authenticated ($firebaseProjectId)" else "Sign in with Google to enable cloud sync",
        )
      if (supabaseService.isConfigured()) {
        val sbHealth = supabaseService.checkSupabaseHealth()
        results +=
          DiagnosticCheckResult(
            name = "Supabase GoTrue Auth",
            category = "AUTH",
            ok = sbHealth.healthy,
            latencyMs = sbHealth.latencyMs,
            detail = sbHealth.detail,
          )
      }

      // 2. Firebase Firestore (active provisioned project — real Server round-trip)
      val fsStart = System.currentTimeMillis()
      val fsResult =
        if (!firebaseSignedIn) {
          DiagnosticCheckResult(
            name = "Firestore Cloud DB ($firebaseProjectId)",
            category = "CLOUD DB",
            ok = false,
            latencyMs = 0L,
            detail = "Requires Google Sign-In to authorize live Firestore listeners",
          )
        } else {
          try {
            val dbId = context.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(dbId)
            db.collection("board_posts").whereEqualTo("visibility", "PUBLIC").limit(1).get(Source.SERVER).await()
            DiagnosticCheckResult(
              name = "Firestore Cloud DB ($firebaseProjectId)",
              category = "CLOUD DB",
              ok = true,
              latencyMs = System.currentTimeMillis() - fsStart,
              detail = "Connected live to $firebaseProjectId",
            )
          } catch (e: CancellationException) {
            throw e
          } catch (e: Exception) {
            DiagnosticCheckResult(
              name = "Firestore Cloud DB ($firebaseProjectId)",
              category = "CLOUD DB",
              ok = false,
              latencyMs = System.currentTimeMillis() - fsStart,
              detail = e.localizedMessage ?: "Firestore unreachable",
            )
          }
        }
      results += fsResult

      // 3. YouTube Live Feed (Real HTTP ping)
      results +=
        timedHttpPing(
          name = "YouTube Live Channel Feed",
          category = "MEDIA",
          configured = true,
          missingMsg = "",
          url = "https://www.youtube.com/feeds/videos.xml?channel_id=$OFFICIAL_CHANNEL_ID",
        )

      // 4. Open-Meteo Primary Weather (Real HTTP ping)
      results +=
        timedHttpPing(
          name = "Open-Meteo Weather",
          category = "WEATHER",
          configured = true,
          missingMsg = "",
          url = "https://api.open-meteo.com/v1/forecast?latitude=37.77&longitude=-122.41&current=temperature_2m",
        )

      // 5. Doppler Storm Radar (Real RainViewer HTTP ping)
      results +=
        timedHttpPing(
          name = "Doppler Storm Radar (RainViewer)",
          category = "WEATHER",
          configured = true,
          missingMsg = "",
          url = "https://api.rainviewer.com/public/weather-maps.json",
        )

      // 6. OpenStreetMap / Nominatim Geocoding (Real HTTP ping)
      results +=
        timedHttpPing(
          name = "OpenStreetMap Geocoding",
          category = "MAPS",
          configured = true,
          missingMsg = "",
          url = "https://nominatim.openstreetmap.org/status.php?format=json",
        )

      // 7. Overpass OSM Road Speeds & Chargers (Real HTTP ping)
      results +=
        timedHttpPing(
          name = "Overpass OSM Roads & Chargers",
          category = "ROADS",
          configured = true,
          missingMsg = "",
          url = "https://overpass-api.de/api/status",
        )

      // 8. OSRM PEV Turn-by-Turn Navigation (Real HTTP ping)
      results +=
        timedHttpPing(
          name = "OSRM PEV Navigation",
          category = "NAVIGATION",
          configured = true,
          missingMsg = "",
          url = "https://router.project-osrm.org/nearest/v1/driving/-122.4194,37.7749",
        )

      // 9. Open-Elevation Terrain API (Real HTTP ping)
      results +=
        timedHttpPing(
          name = "Open-Elevation Terrain API",
          category = "ELEVATION",
          configured = true,
          missingMsg = "",
          url = "https://api.open-elevation.com/api/v1/lookup?locations=37.7749,-122.4194",
        )

      // 10. Live PEV News Feed (Real Electrek RSS HTTP ping)
      results +=
        timedHttpPing(
          name = "PEV News Feed (Electrek RSS)",
          category = "NEWS",
          configured = true,
          missingMsg = "",
          url = "https://electrek.co/feed/",
        )

      // 11. Firebase AI Logic & App Check
      val appCheckReady = runCatching { FirebaseApp.getInstance().options.applicationId.isNotBlank() }.getOrDefault(false)
      results +=
        DiagnosticCheckResult(
          name = "Firebase AI Logic & App Check",
          category = "AI",
          ok = appCheckReady,
          latencyMs = 0L,
          detail = if (appCheckReady) "App Check attested Gemini AI active ($firebaseProjectId)" else "Firebase app not initialized",
        )

      // 8. Hardware Sensors
      val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
      val providerReady =
        runCatching {
          lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        }.getOrDefault(false)
      results +=
        DiagnosticCheckResult(
          name = "GPS & IMU Telemetry Engine",
          category = "HARDWARE",
          ok = gpsActive || providerReady,
          latencyMs = 0L,
          detail =
            when {
              gpsActive -> "Live GPS + 9-DOF IMU fix active"
              providerReady -> "GPS hardware enabled (awaiting satellite fix)"
              else -> "GPS provider disabled in device settings"
            },
        )

      results
    }

  // --------------------------------------------------------------------------
  // 9. AUTO-RETRIEVE PEV PHOTO AS COMPACT CLOUD BASE64 DATA URI
  // --------------------------------------------------------------------------
  suspend fun fetchPevProductImageBase64(makeAndModel: String): String =
    withContext(Dispatchers.IO) {
      val clean = makeAndModel.trim()
      if (clean.length < 2) return@withContext ""
      val candidateUrls = mutableListOf<String>()

      // 1. Optional Google CSE Image Search with Redundant Key Failover
      val csePairs = ApiKeyRing.googleCsePairs()
      if (csePairs.isNotEmpty()) {
        runCatching {
          val q = URLEncoder.encode("$clean electric vehicle PEV", "UTF-8")
          ApiKeyRing.executeWithCseFailover(csePairs) { rawKey, rawCx ->
            val key = URLEncoder.encode(rawKey, "UTF-8")
            val cx = URLEncoder.encode(rawCx, "UTF-8")
            val url = "https://www.googleapis.com/customsearch/v1?q=$q&cx=$cx&key=$key&searchType=image&num=3&imgSize=medium"
            httpClient.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
              if (ApiKeyRing.isFailoverCode(resp.code)) throw ApiKeyFailoverException(resp.code, "Google CSE HTTP ${resp.code}")
              if (resp.isSuccessful) {
                val arr = JSONObject(resp.body?.string().orEmpty()).optJSONArray("items")
                if (arr != null) {
                  for (i in 0 until arr.length()) {
                    val link = arr.optJSONObject(i)?.optString("link").orEmpty()
                    if (link.startsWith("https://")) candidateUrls += link
                  }
                }
              }
            }
          }
        }
      }

      // 2. Keyless Multi-Engine Live Product Image Search (Bing Images + Shopify PEV Dealers + DDG)
      val liveUrls = runCatching { fetchLiveProductImages("$clean electric vehicle", limit = 6) }.getOrDefault(emptyList())
      candidateUrls += liveUrls.filter { it.startsWith("https://") }

      // 3. Keyless Wikipedia PageImages Search
      if (candidateUrls.size < 3) {
        runCatching {
          val q = URLEncoder.encode(clean, "UTF-8")
          val url =
            "https://en.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch=$q&gsrlimit=4&prop=pageimages&pithumbsize=520&format=json"
          val req = Request.Builder().url(url).header("User-Agent", "RuralERidesGO/10.0 Android").get().build()
          httpClient.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) {
              val pages = JSONObject(resp.body?.string().orEmpty()).optJSONObject("query")?.optJSONObject("pages")
              if (pages != null) {
                val keys = pages.keys()
                while (keys.hasNext()) {
                  val p = pages.optJSONObject(keys.next()) ?: continue
                  val src = p.optJSONObject("thumbnail")?.optString("source").orEmpty()
                  if (src.startsWith("https://")) candidateUrls += src
                }
              }
            }
          }
        }
      }

      // 4. Keyless Wikimedia Commons Image Search
      if (candidateUrls.size < 3) {
        runCatching {
          val q = URLEncoder.encode("$clean electric", "UTF-8")
          val url =
            "https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrnamespace=6&gsrsearch=$q&gsrlimit=4&prop=imageinfo&iiprop=url&iiurlwidth=520&format=json"
          val req = Request.Builder().url(url).header("User-Agent", "RuralERidesGO/14.0 Android").get().build()
          httpClient.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) {
              val pages = JSONObject(resp.body?.string().orEmpty()).optJSONObject("query")?.optJSONObject("pages")
              if (pages != null) {
                val keys = pages.keys()
                while (keys.hasNext()) {
                  val p = pages.optJSONObject(keys.next()) ?: continue
                  val info = p.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
                  val src = info.optString("thumburl").ifBlank { info.optString("url") }
                  if (src.startsWith("https://") && !src.endsWith(".svg", ignoreCase = true)) {
                    candidateUrls += src
                  }
                }
              }
            }
          }
        }
      }

      // 5. Keyless Openverse Creative Commons Image Search
      if (candidateUrls.isEmpty()) {
        runCatching {
          val q = URLEncoder.encode("$clean electric scooter bike", "UTF-8")
          val url = "https://api.openverse.org/v1/images/?q=$q&page_size=4"
          val req = Request.Builder().url(url).header("User-Agent", "RuralERidesGO/14.0 Android").get().build()
          httpClient.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) {
              val arr = JSONObject(resp.body?.string().orEmpty()).optJSONArray("results")
              if (arr != null) {
                for (i in 0 until arr.length()) {
                  val obj = arr.optJSONObject(i) ?: continue
                  val src = obj.optString("thumbnail").ifBlank { obj.optString("url") }
                  if (src.startsWith("https://")) candidateUrls += src
                }
              }
            }
          }
        }
      }

      // Download first valid raster image and convert directly to cloud-safe JPEG Base64
      for (imgUrl in candidateUrls.distinct().take(10)) {
        val encoded =
          runCatching {
            val req =
              Request.Builder()
                .url(imgUrl)
                .header(
                  "User-Agent",
                  "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36",
                )
                .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
              if (!resp.isSuccessful) return@use ""
              val bytes = resp.body?.bytes() ?: return@use ""
              if (bytes.isEmpty() || bytes.size > 6_000_000) return@use ""
              val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@use ""
              val b64 = bitmapToCloudBase64DataUri(bmp, maxDimension = 480, initialQuality = 64, maxChars = 140_000)
              bmp.recycle()
              b64
            }
          }.getOrDefault("")
        if (encoded.startsWith("data:image/")) return@withContext encoded
      }
      ""
    }

  // --------------------------------------------------------------------------
  // 9B. OFFICIAL PEV BRAND LOGO FETCHER (PEV PROFILE + SPEC ENGINE)
  // --------------------------------------------------------------------------
  private val brandLogoCache = java.util.concurrent.ConcurrentHashMap<String, String>()

  private val knownPevBrandDomains: List<Pair<List<String>, Pair<String, String>>> =
    listOf(
      listOf("segway", "ninebot") to ("Segway" to "segway.com"),
      listOf("sur-ron", "surron", "light bee", "ultra bee", "storm bee") to ("Sur-Ron" to "sur-ronusa.com"),
      listOf("talaria", "sting", "komodo") to ("Talaria" to "talariaebikes.com"),
      listOf("dualtron", "minimotors", "bluetran", "speedway") to ("Dualtron" to "minimotorsusa.com"),
      listOf("kaabo", "wolf warrior", "wolf king", "mantis") to ("Kaabo" to "kaabousa.com"),
      listOf("nami", "burn-e", "klima", "blast") to ("NAMI" to "namielectric.com"),
      listOf("vsett") to ("VSETT" to "vsett.com"),
      listOf("inmotion") to ("InMotion" to "inmotionworld.com"),
      listOf("begode", "gotway", "extreme bull") to ("Begode" to "begode.com"),
      listOf("leaperkim", "veteran", "sherman", "lynx", "patton") to ("Veteran" to "leaperkim.com"),
      listOf("kingsong", "king song") to ("KingSong" to "kingsong.com"),
      listOf("nosfet") to ("Nosfet" to "nosfet.com"),
      listOf("apollo") to ("Apollo" to "apolloscooters.co"),
      listOf("emove", "voro", "roadrunner") to ("EMOVE" to "voromotors.com"),
      listOf("super73", "super 73") to ("Super73" to "super73.com"),
      listOf("rad power", "radrunner", "radrover", "radwagon", "radcity", "radexpand") to ("Rad Power" to "radpowerbikes.com"),
      listOf("aventon", "aventure", "level.2", "abound", "sinch", "soltera") to ("Aventon" to "aventon.com"),
      listOf("lectric", "xp 3.0", "xpedition", "xpeak") to ("Lectric" to "lectricebikes.com"),
      listOf("stark", "varg") to ("Stark Future" to "starkfuture.com"),
      listOf("e-ride pro", "eride pro", "e ride pro") to ("E-Ride Pro" to "eridepros.com"),
      listOf("onewheel", "future motion") to ("Onewheel" to "onewheel.com"),
      listOf("hiboy") to ("Hiboy" to "hiboy.com"),
      listOf("gotrax") to ("Gotrax" to "gotrax.com"),
      listOf("niu") to ("NIU" to "niu.com"),
      listOf("kukirin", "kugoo") to ("KuKirin" to "kugookirin.com"),
      listOf("mukuta") to ("Mukuta" to "mukutariding.com"),
      listOf("yume") to ("YUME" to "yumescooter.com"),
      listOf("teewing") to ("Teewing" to "teewing.com"),
      listOf("nanrobot") to ("Nanrobot" to "nanrobot.com"),
      listOf("varla") to ("Varla" to "varlascooter.com"),
      listOf("solar") to ("Solar" to "solarscooters.com"),
      listOf("juiced") to ("Juiced" to "juicedbikes.com"),
      listOf("ariel rider", "grizzly", "x-class") to ("Ariel Rider" to "arielrider.com"),
      listOf("ride1up", "revv1", "revv 1") to ("Ride1Up" to "ride1up.com"),
      listOf("specialized", "turbo levo", "turbo vado", "globe haul") to ("Specialized" to "specialized.com"),
      listOf("trek") to ("Trek" to "trekbikes.com"),
      listOf("cannondale") to ("Cannondale" to "cannondale.com"),
      listOf("giant", "momentum") to ("Giant" to "giant-bicycles.com"),
      listOf("santa cruz") to ("Santa Cruz" to "santacruzbicycles.com"),
      listOf("tern") to ("Tern" to "ternbicycles.com"),
      listOf("gazelle") to ("Gazelle" to "gazellebikes.com"),
      listOf("riese", "muller") to ("Riese & Müller" to "r-m.de"),
      listOf("velotric") to ("Velotric" to "velotricbike.com"),
      listOf("mokwheel") to ("Mokwheel" to "mokwheel.com"),
      listOf("heybike") to ("Heybike" to "heybike.com"),
      listOf("engwe") to ("Engwe" to "engwe-bikes.com"),
      listOf("fiido") to ("Fiido" to "fiido.com"),
      listOf("himiway") to ("Himiway" to "himiwaybike.com"),
      listOf("quietkat") to ("QuietKat" to "quietkat.com"),
      listOf("bakcou") to ("Bakcou" to "bakcou.com"),
      listOf("cyrusher") to ("Cyrusher" to "cyrusher.com"),
      listOf("zooz") to ("Zooz" to "zoozbikes.com"),
      listOf("onyx") to ("ONYX" to "onyxmotorbikes.com"),
      listOf("zero") to ("Zero" to "zeromotorcycles.com"),
      listOf("livewire") to ("LiveWire" to "livewire.com"),
      listOf("altis", "sigma") to ("Altis" to "altispowersports.com"),
      listOf("rawrr", "mantis x") to ("Rawrr" to "riderawrr.com"),
      listOf("79bike", "falcon") to ("79Bike" to "79bike.com"),
      listOf("meepo") to ("Meepo" to "meepoboard.com"),
      listOf("evolve") to ("Evolve" to "evolveskateboardsusa.com"),
      listOf("backfire") to ("Backfire" to "backfireboards.com"),
      listOf("exway") to ("Exway" to "exwayboard.com"),
      listOf("acedeck") to ("Acedeck" to "acedeckboards.com"),
      listOf("propel") to ("Propel" to "ridepropel.com"),
      listOf("tynee") to ("Tynee" to "tyneeboard.com"),
      listOf("wowgo") to ("WowGo" to "wowgoboard.com"),
      listOf("xiaomi") to ("Xiaomi" to "mi.com"),
      listOf("unagi") to ("Unagi" to "unagiscooters.com"),
      listOf("fluidfreeride", "mosquito") to ("FluidFreeRide" to "fluidfreeride.com"),
      listOf("inokim") to ("Inokim" to "inokim.com"),
      listOf("honda", "motocompacto") to ("Honda" to "honda.com"),
      listOf("yamaha") to ("Yamaha" to "yamahabicycles.com"),
      listOf("kawasaki") to ("Kawasaki" to "kawasaki.com"),
      listOf("ktm") to ("KTM" to "ktm.com"),
      listOf("husqvarna") to ("Husqvarna" to "husqvarna-motorcycles.com"),
      listOf("bmw") to ("BMW" to "bmwmotorcycles.com"),
    )

  fun resolvePevBrandInfo(brandOrModel: String): Pair<String, String> {
    val clean = brandOrModel.trim()
    val lower = clean.lowercase()
    for ((tokens, brandDomain) in knownPevBrandDomains) {
      if (tokens.any { tok -> lower.contains(tok) }) {
        return brandDomain
      }
    }
    val firstWord =
      clean
        .split(Regex("[\\s\\-/]+"))
        .firstOrNull { it.length >= 2 }
        ?.replace(Regex("[^A-Za-z0-9]"), "")
        .orEmpty()
    val guessedDomain = if (firstWord.length >= 3) "${firstWord.lowercase()}.com" else ""
    return (firstWord.ifBlank { clean.take(12) }) to guessedDomain
  }

  fun cachedPevBrandLogo(brandOrModel: String): String {
    val (brandName, _) = resolvePevBrandInfo(brandOrModel)
    val key = brandName.lowercase().trim()
    return if (key.isNotBlank()) brandLogoCache[key].orEmpty() else ""
  }

  suspend fun fetchPevBrandLogoBase64(brandOrModel: String): String =
    withContext(Dispatchers.IO) {
      val clean = brandOrModel.trim()
      if (clean.length < 2) return@withContext ""
      val (brandName, domain) = resolvePevBrandInfo(clean)
      val cacheKey = brandName.lowercase().trim()
      brandLogoCache[cacheKey]?.let { if (it.isNotBlank()) return@withContext it }

      val candidateUrls = mutableListOf<String>()
      if (domain.isNotBlank()) {
        // High-res official brand favicon/emblem endpoints
        candidateUrls += "https://www.google.com/s2/favicons?domain=${URLEncoder.encode(domain, "UTF-8")}&sz=128"
        candidateUrls += "https://icons.duckduckgo.com/ip3/$domain.ico"
      }

      // Wikimedia Commons brand logo search (non-SVG raster PNG/JPG)
      runCatching {
        val q = URLEncoder.encode("$brandName logo", "UTF-8")
        val url =
          "https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrnamespace=6&gsrsearch=$q&gsrlimit=3&prop=imageinfo&iiprop=url&iiurlwidth=256&format=json"
        val req = Request.Builder().url(url).header("User-Agent", "RuralERidesGO/14.0 Android").get().build()
        httpClient.newCall(req).execute().use { resp ->
          if (resp.isSuccessful) {
            val pages = JSONObject(resp.body?.string().orEmpty()).optJSONObject("query")?.optJSONObject("pages")
            if (pages != null) {
              val keys = pages.keys()
              while (keys.hasNext()) {
                val p = pages.optJSONObject(keys.next()) ?: continue
                val info = p.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
                val src = info.optString("thumburl").ifBlank { info.optString("url") }
                if (src.startsWith("https://") && !src.endsWith(".svg", ignoreCase = true)) {
                  candidateUrls += src
                }
              }
            }
          }
        }
      }

      for (logoUrl in candidateUrls.distinct().take(6)) {
        val encoded =
          runCatching {
            val req = Request.Builder().url(logoUrl).header("User-Agent", "RuralERidesGO/14.0 Android").get().build()
            httpClient.newCall(req).execute().use { resp ->
              if (!resp.isSuccessful) return@use ""
              val bytes = resp.body?.bytes() ?: return@use ""
              if (bytes.size < 120 || bytes.size > 1_500_000) return@use ""
              val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@use ""
              if (bmp.width < 16 || bmp.height < 16) {
                bmp.recycle()
                return@use ""
              }
              val out = java.io.ByteArrayOutputStream()
              val scaled =
                if (bmp.width > 128 || bmp.height > 128) {
                  val ratio = minOf(128f / bmp.width, 128f / bmp.height)
                  android.graphics.Bitmap.createScaledBitmap(
                    bmp,
                    (bmp.width * ratio).toInt().coerceAtLeast(16),
                    (bmp.height * ratio).toInt().coerceAtLeast(16),
                    true,
                  )
                } else bmp
              scaled.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, out)
              if (scaled !== bmp) scaled.recycle()
              bmp.recycle()
              val pngBytes = out.toByteArray()
              if (pngBytes.size > 48_000) "" else "data:image/png;base64," + android.util.Base64.encodeToString(pngBytes, android.util.Base64.NO_WRAP)
            }
          }.getOrDefault("")
        if (encoded.startsWith("data:image/")) {
          if (cacheKey.isNotBlank()) brandLogoCache[cacheKey] = encoded
          return@withContext encoded
        }
      }
      ""
    }

  // --------------------------------------------------------------------------
  // 10. LIVE TURN-BY-TURN GPS NAVIGATION & PUBLIC CHARGER LOCATOR
  //     Uses dedicated API keys (MAPBOX_ACCESS_TOKEN / OPENROUTESERVICE_API_KEY /
  //     TOMTOM_API_KEY / OPENCHARGEMAP_API_KEY) with keyless OSRM/Nominatim/OSM fallbacks.
  // --------------------------------------------------------------------------
  suspend fun searchNavDestinations(
    query: String,
    nearLat: Double = 0.0,
    nearLng: Double = 0.0,
  ): Result<List<WeatherGeoLocation>> =
    withContext(Dispatchers.IO) {
      runCatching {
        val clean = query.trim()
        if (clean.length < 2) return@runCatching emptyList()
        val encoded = URLEncoder.encode(clean, "UTF-8")

        // 1. Mapbox Geocoding API (dedicated MAPBOX_ACCESS_TOKEN + _2 failover)
        val mbTokens = ApiKeyRing.mapboxTokens()
        if (mbTokens.isNotEmpty()) {
          val mbRes =
            runCatching {
              val prox = if (nearLat != 0.0 || nearLng != 0.0) "&proximity=$nearLng,$nearLat" else ""
              ApiKeyRing.executeWithFailover("MAPBOX", mbTokens) { rawTok ->
                val tok = URLEncoder.encode(rawTok, "UTF-8")
                val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/$encoded.json?limit=6$prox&access_token=$tok"
                httpClient.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
                  if (ApiKeyRing.isFailoverCode(resp.code)) throw ApiKeyFailoverException(resp.code, "Mapbox HTTP ${resp.code}")
                  if (!resp.isSuccessful) return@executeWithFailover emptyList()
                  val feats = JSONObject(resp.body?.string().orEmpty()).optJSONArray("features") ?: return@executeWithFailover emptyList()
                  (0 until feats.length()).mapNotNull { i ->
                    val f = feats.optJSONObject(i) ?: return@mapNotNull null
                    val center = f.optJSONArray("center") ?: return@mapNotNull null
                    val place = f.optString("place_name").trim()
                    if (place.isBlank() || center.length() < 2) return@mapNotNull null
                    val parts = place.split(",").map { it.trim() }
                    WeatherGeoLocation(
                      name = parts.firstOrNull().orEmpty(),
                      admin1 = parts.drop(1).take(2).joinToString(", "),
                      country = parts.lastOrNull().orEmpty(),
                      lat = center.optDouble(1, 0.0),
                      lng = center.optDouble(0, 0.0),
                    )
                  }
                }
              }
            }.getOrDefault(emptyList())
          if (mbRes.isNotEmpty()) return@runCatching mbRes
        }

        // 2. TomTom Search API (dedicated TOMTOM_API_KEY + _2 failover)
        val ttKeys = ApiKeyRing.tomTomKeys()
        if (ttKeys.isNotEmpty()) {
          val ttRes =
            runCatching {
              val bias = if (nearLat != 0.0 || nearLng != 0.0) "&lat=$nearLat&lon=$nearLng" else ""
              ApiKeyRing.executeWithFailover("TOMTOM", ttKeys) { rawKey ->
                val key = URLEncoder.encode(rawKey, "UTF-8")
                val url = "https://api.tomtom.com/search/2/search/$encoded.json?limit=6$bias&key=$key"
                httpClient.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
                  if (ApiKeyRing.isFailoverCode(resp.code)) throw ApiKeyFailoverException(resp.code, "TomTom HTTP ${resp.code}")
                  if (!resp.isSuccessful) return@executeWithFailover emptyList()
                  val arr = JSONObject(resp.body?.string().orEmpty()).optJSONArray("results") ?: return@executeWithFailover emptyList()
                  (0 until arr.length()).mapNotNull { i ->
                    val o = arr.optJSONObject(i) ?: return@mapNotNull null
                    val pos = o.optJSONObject("position") ?: return@mapNotNull null
                    val addr = o.optJSONObject("address")
                    val poi = o.optJSONObject("poi")?.optString("name").orEmpty()
                    val label = poi.ifBlank { addr?.optString("freeformAddress").orEmpty() }
                    if (label.isBlank()) return@mapNotNull null
                    WeatherGeoLocation(
                      name = label,
                      admin1 = addr?.optString("municipality").orEmpty(),
                      country = addr?.optString("countryCode").orEmpty(),
                      lat = pos.optDouble("lat", 0.0),
                      lng = pos.optDouble("lon", 0.0),
                    )
                  }
                }
              }
            }.getOrDefault(emptyList())
          if (ttRes.isNotEmpty()) return@runCatching ttRes
        }

        // 3. Keyless Nominatim OpenStreetMap Place/Address Search
        val nomUrl = "https://nominatim.openstreetmap.org/search?format=jsonv2&q=$encoded&limit=6"
        val req = Request.Builder().url(nomUrl).header("User-Agent", "RuralERidesGO/11.0 Android").get().build()
        val nomRes =
          httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@use emptyList()
            val arr = JSONArray(resp.body?.string().orEmpty())
            (0 until arr.length()).mapNotNull { i ->
              val o = arr.optJSONObject(i) ?: return@mapNotNull null
              val display = o.optString("display_name").trim()
              val lat = o.optString("lat").toDoubleOrNull() ?: return@mapNotNull null
              val lng = o.optString("lon").toDoubleOrNull() ?: return@mapNotNull null
              val parts = display.split(",").map { it.trim() }
              WeatherGeoLocation(
                name = parts.firstOrNull().orEmpty(),
                admin1 = parts.drop(1).take(2).joinToString(", "),
                country = parts.lastOrNull().orEmpty(),
                lat = lat,
                lng = lng,
              )
            }
          }
        if (nomRes.isNotEmpty()) nomRes else searchWeatherLocations(clean).getOrDefault(emptyList())
      }
    }

  suspend fun fetchTurnByTurnRoute(
    startLat: Double,
    startLng: Double,
    endLat: Double,
    endLng: Double,
    destinationName: String,
    profile: NavRouteProfile,
    avoidHighways: Boolean = true,
    vehicleWh: Long = 1500L,
  ): Result<ActiveNavRoute> =
    withContext(Dispatchers.IO) {
      runCatching {
        val whSafe = vehicleWh.coerceAtLeast(350L).toDouble()

        fun estBattPct(distKm: Double): Int {
          val miles = distKm * 0.621371
          val whNeeded = miles * 26.0
          return ((whNeeded / whSafe) * 100.0).toInt().coerceIn(1, 999)
        }

        // 1. Dedicated Mapbox Directions API (redundant token failover)
        val mbTokens = ApiKeyRing.mapboxTokens()
        if (mbTokens.isNotEmpty()) {
          val mb =
            runCatching {
              val exclude = if (avoidHighways && profile == NavRouteProfile.FAST_STREET) "&exclude=motorway" else ""
              ApiKeyRing.executeWithFailover("MAPBOX", mbTokens) { rawTok ->
                val tok = URLEncoder.encode(rawTok, "UTF-8")
                val url =
                  "https://api.mapbox.com/directions/v5/mapbox/${profile.mapboxProfile}/$startLng,$startLat;$endLng,$endLat?geometries=geojson&steps=true&overview=full$exclude&access_token=$tok"
                httpClient.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
                  if (ApiKeyRing.isFailoverCode(resp.code)) throw ApiKeyFailoverException(resp.code, "Mapbox HTTP ${resp.code}")
                  if (!resp.isSuccessful) return@executeWithFailover null
                  val route = JSONObject(resp.body?.string().orEmpty()).optJSONArray("routes")?.optJSONObject(0) ?: return@executeWithFailover null
                  val distKm = route.optDouble("distance", 0.0) / 1000.0
                  val durSec = route.optDouble("duration", 0.0).toLong()
                  val coords = route.optJSONObject("geometry")?.optJSONArray("coordinates") ?: JSONArray()
                  val poly =
                    (0 until coords.length()).mapNotNull { idx ->
                      val pt = coords.optJSONArray(idx) ?: return@mapNotNull null
                      if (pt.length() >= 2) pt.optDouble(1) to pt.optDouble(0) else null
                    }
                  val steps = mutableListOf<NavManeuverStep>()
                  val legs = route.optJSONArray("legs")
                  if (legs != null && legs.length() > 0) {
                    val sArr = legs.optJSONObject(0)?.optJSONArray("steps") ?: JSONArray()
                    for (i in 0 until sArr.length()) {
                      val st = sArr.optJSONObject(i) ?: continue
                      val man = st.optJSONObject("maneuver")
                      val loc = man?.optJSONArray("location")
                      val instr = man?.optString("instruction").orEmpty().ifBlank { "Continue on route" }
                      val mType = mapManeuverType(man?.optString("type").orEmpty(), man?.optString("modifier").orEmpty())
                      steps +=
                        NavManeuverStep(
                          instruction = instr,
                          streetName = st.optString("name").orEmpty(),
                          distanceM = st.optDouble("distance", 0.0),
                          durationSec = st.optDouble("duration", 0.0),
                          maneuverType = mType,
                          lat = loc?.optDouble(1, endLat) ?: endLat,
                          lng = loc?.optDouble(0, endLng) ?: endLng,
                        )
                    }
                  }
                  if (poly.size < 2) return@executeWithFailover null
                  ActiveNavRoute(
                    destinationName = destinationName,
                    destLat = endLat,
                    destLng = endLng,
                    profile = profile,
                    totalDistanceKm = distKm,
                    totalDurationSec = durSec,
                    polylinePoints = poly,
                    steps = steps,
                    providerSource = "Mapbox Directions (API Key)",
                    estimatedBatteryPctNeeded = estBattPct(distKm),
                  )
                }
              }
            }.getOrNull()
          if (mb != null) return@runCatching mb
        }

        // 2. Dedicated OpenRouteService Directions API (PEV / Electric Cycling specialist, redundant key failover)
        val orsKeys = ApiKeyRing.openRouteServiceKeys()
        if (orsKeys.isNotEmpty()) {
          val ors =
            runCatching {
              val url = "https://api.openrouteservice.org/v2/directions/${profile.orsProfile}/geojson"
              val bodyJson =
                JSONObject()
                  .put(
                    "coordinates",
                    JSONArray()
                      .put(JSONArray().put(startLng).put(startLat))
                      .put(JSONArray().put(endLng).put(endLat)),
                  )
                  .put("instructions", true)
                  .put("elevation", true)
                  .toString()
              ApiKeyRing.executeWithFailover("OPENROUTESERVICE", orsKeys) { rawKey ->
                val req =
                  Request.Builder()
                    .url(url)
                    .header("Authorization", rawKey)
                    .header("Content-Type", "application/json")
                    .post(bodyJson.toRequestBody("application/json".toMediaType()))
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                  if (ApiKeyRing.isFailoverCode(resp.code)) throw ApiKeyFailoverException(resp.code, "ORS HTTP ${resp.code}")
                  if (!resp.isSuccessful) return@executeWithFailover null
                  val feat = JSONObject(resp.body?.string().orEmpty()).optJSONArray("features")?.optJSONObject(0) ?: return@executeWithFailover null
                  val props = feat.optJSONObject("properties") ?: return@executeWithFailover null
                  val summary = props.optJSONObject("summary") ?: return@executeWithFailover null
                  val distKm = summary.optDouble("distance", 0.0) / 1000.0
                  val durSec = summary.optDouble("duration", 0.0).toLong()
                  val ascent = summary.optDouble("ascent", 0.0)
                  val coords = feat.optJSONObject("geometry")?.optJSONArray("coordinates") ?: JSONArray()
                  val poly =
                    (0 until coords.length()).mapNotNull { idx ->
                      val pt = coords.optJSONArray(idx) ?: return@mapNotNull null
                      if (pt.length() >= 2) pt.optDouble(1) to pt.optDouble(0) else null
                    }
                  val steps = mutableListOf<NavManeuverStep>()
                  val segs = props.optJSONArray("segments")
                  if (segs != null && segs.length() > 0) {
                    val sArr = segs.optJSONObject(0)?.optJSONArray("steps") ?: JSONArray()
                    for (i in 0 until sArr.length()) {
                      val st = sArr.optJSONObject(i) ?: continue
                      val wpIdx = st.optJSONArray("way_points")?.optInt(0, 0) ?: 0
                      val wp = poly.getOrNull(wpIdx) ?: (endLat to endLng)
                      steps +=
                        NavManeuverStep(
                          instruction = st.optString("instruction").ifBlank { "Continue" },
                          streetName = st.optString("name").orEmpty(),
                          distanceM = st.optDouble("distance", 0.0),
                          durationSec = st.optDouble("duration", 0.0),
                          maneuverType = mapOrsType(st.optInt("type", 11)),
                          lat = wp.first,
                          lng = wp.second,
                        )
                    }
                  }
                  if (poly.size < 2) return@executeWithFailover null
                  ActiveNavRoute(
                    destinationName = destinationName,
                    destLat = endLat,
                    destLng = endLng,
                    profile = profile,
                    totalDistanceKm = distKm,
                    totalDurationSec = durSec,
                    elevationGainM = ascent,
                    polylinePoints = poly,
                    steps = steps,
                    providerSource = "OpenRouteService (API Key)",
                    estimatedBatteryPctNeeded = estBattPct(distKm),
                  )
                }
              }
            }.getOrNull()
          if (ors != null) return@runCatching ors
        }

        // 3. Keyless Live OSRM Public Turn-by-Turn Routing Server
        val osrmUrl =
          "https://router.project-osrm.org/route/v1/${profile.osrmProfile}/$startLng,$startLat;$endLng,$endLat?overview=full&geometries=geojson&steps=true"
        val req = Request.Builder().url(osrmUrl).header("User-Agent", "RuralERidesGO/11.0 Android").get().build()
        httpClient.newCall(req).execute().use { resp ->
          if (!resp.isSuccessful) error("Routing service HTTP ${resp.code}")
          val route = JSONObject(resp.body?.string().orEmpty()).optJSONArray("routes")?.optJSONObject(0)
            ?: error("No drivable/ridable route found to destination")
          val distKm = route.optDouble("distance", 0.0) / 1000.0
          val durSec = route.optDouble("duration", 0.0).toLong()
          val coords = route.optJSONObject("geometry")?.optJSONArray("coordinates") ?: JSONArray()
          val poly =
            (0 until coords.length()).mapNotNull { idx ->
              val pt = coords.optJSONArray(idx) ?: return@mapNotNull null
              if (pt.length() >= 2) pt.optDouble(1) to pt.optDouble(0) else null
            }
          val steps = mutableListOf<NavManeuverStep>()
          val legs = route.optJSONArray("legs")
          if (legs != null && legs.length() > 0) {
            val sArr = legs.optJSONObject(0)?.optJSONArray("steps") ?: JSONArray()
            for (i in 0 until sArr.length()) {
              val st = sArr.optJSONObject(i) ?: continue
              val man = st.optJSONObject("maneuver")
              val loc = man?.optJSONArray("location")
              val mTypeRaw = man?.optString("type").orEmpty()
              val mMod = man?.optString("modifier").orEmpty()
              val road = st.optString("name").orEmpty()
              val mType = mapManeuverType(mTypeRaw, mMod)
              val instr = buildHumanInstruction(mTypeRaw, mMod, road)
              steps +=
                NavManeuverStep(
                  instruction = instr,
                  streetName = road,
                  distanceM = st.optDouble("distance", 0.0),
                  durationSec = st.optDouble("duration", 0.0),
                  maneuverType = mType,
                  lat = loc?.optDouble(1, endLat) ?: endLat,
                  lng = loc?.optDouble(0, endLng) ?: endLng,
                )
            }
          }
          if (poly.size < 2) error("Empty route geometry returned")
          ActiveNavRoute(
            destinationName = destinationName,
            destLat = endLat,
            destLng = endLng,
            profile = profile,
            totalDistanceKm = distKm,
            totalDurationSec = durSec,
            polylinePoints = poly,
            steps = steps,
            providerSource = "OSRM Live Routing",
            estimatedBatteryPctNeeded = estBattPct(distKm),
          )
        }
      }
    }

  suspend fun fetchLiveChargingStations(
    lat: Double,
    lng: Double,
    radiusKm: Int = 15,
  ): Result<List<LiveChargingStation>> =
    withContext(Dispatchers.IO) {
      runCatching {
        if (lat == 0.0 && lng == 0.0) return@runCatching emptyList()

        // 1. Dedicated OpenChargeMap API (OPENCHARGEMAP_API_KEY + _2 failover)
        val ocmKeys = ApiKeyRing.openChargeMapKeys()
        if (ocmKeys.isNotEmpty()) {
          val ocm =
            runCatching {
              ApiKeyRing.executeWithFailover("OPENCHARGEMAP", ocmKeys) { rawKey ->
                val key = URLEncoder.encode(rawKey, "UTF-8")
                val url =
                  "https://api.openchargemap.io/v3/poi/?output=json&latitude=$lat&longitude=$lng&distance=$radiusKm&distanceunit=KM&maxresults=15&compact=true&verbose=false&key=$key"
                httpClient.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
                  if (ApiKeyRing.isFailoverCode(resp.code)) throw ApiKeyFailoverException(resp.code, "OpenChargeMap HTTP ${resp.code}")
                  if (!resp.isSuccessful) return@executeWithFailover emptyList()
                  val arr = JSONArray(resp.body?.string().orEmpty())
                  (0 until arr.length()).mapNotNull { i ->
                    val o = arr.optJSONObject(i) ?: return@mapNotNull null
                    val addr = o.optJSONObject("AddressInfo") ?: return@mapNotNull null
                    val title = addr.optString("Title").trim()
                    val sLat = addr.optDouble("Latitude", 0.0)
                    val sLng = addr.optDouble("Longitude", 0.0)
                    if (title.isBlank() || (sLat == 0.0 && sLng == 0.0)) return@mapNotNull null
                    val conns = o.optJSONArray("Connections")
                    val kw = conns?.optJSONObject(0)?.optDouble("PowerKW", 0.0) ?: 0.0
                    LiveChargingStation(
                      id = "ocm_${o.optInt("ID", i)}",
                      title = title,
                      address = listOf(addr.optString("AddressLine1"), addr.optString("Town")).filter { it.isNotBlank() }.joinToString(", "),
                      lat = sLat,
                      lng = sLng,
                      distanceKm = addr.optDouble("Distance", 0.0),
                      powerKw = if (kw > 0.0) "${"%.1f".format(kw)} kW" else "AC Outlet / EVSE",
                      operatorName = "OpenChargeMap",
                      isFree = o.optString("UsageCost").contains("free", ignoreCase = true),
                    )
                  }
                }
              }
            }.getOrDefault(emptyList())
          if (ocm.isNotEmpty()) return@runCatching ocm
        }

        // 2. Keyless Overpass OpenStreetMap Live Charging Station Query
        val radM = (radiusKm * 1000).coerceIn(2000, 25000)
        val query = "[out:json][timeout:10];node[\"amenity\"=\"charging_station\"](around:$radM,$lat,$lng);out 15;"
        val encodedQ = URLEncoder.encode(query, "UTF-8")
        val url = "https://overpass-api.de/api/interpreter?data=$encodedQ"
        val req = Request.Builder().url(url).header("User-Agent", "RuralERidesGO/11.0 Android").get().build()
        httpClient.newCall(req).execute().use { resp ->
          if (!resp.isSuccessful) return@use emptyList()
          val elems = JSONObject(resp.body?.string().orEmpty()).optJSONArray("elements") ?: return@use emptyList()
          (0 until elems.length()).mapNotNull { i ->
            val el = elems.optJSONObject(i) ?: return@mapNotNull null
            val sLat = el.optDouble("lat", 0.0)
            val sLng = el.optDouble("lon", 0.0)
            if (sLat == 0.0 && sLng == 0.0) return@mapNotNull null
            val tags = el.optJSONObject("tags")
            val name = tags?.optString("name").orEmpty().ifBlank { tags?.optString("operator").orEmpty() }.ifBlank { "Public EV/PEV Charger" }
            val fee = tags?.optString("fee").orEmpty()
            val dLat = Math.toRadians(sLat - lat)
            val dLng = Math.toRadians(sLng - lng)
            val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
              kotlin.math.cos(Math.toRadians(lat)) * kotlin.math.cos(Math.toRadians(sLat)) *
              kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)
            val distKm = 6371.0 * 2.0 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
            LiveChargingStation(
              id = "osm_${el.optLong("id", i.toLong())}",
              title = name,
              address = tags?.optString("addr:street").orEmpty().ifBlank { "OpenStreetMap Public Station" },
              lat = sLat,
              lng = sLng,
              distanceKm = distKm,
              powerKw = tags?.optString("socket:type2:output").orEmpty().ifBlank { "Public AC/EV" },
              operatorName = tags?.optString("operator").orEmpty().ifBlank { "OSM Live" },
              isFree = fee.equals("no", ignoreCase = true),
            )
          }.sortedBy { it.distanceKm }
        }
      }
    }

  // --------------------------------------------------------------------------
  // 12. MULTI-IMAGE LIVE PRODUCT GALLERY FETCHER (SPEC ENGINE & PARTS FINDER)
  // --------------------------------------------------------------------------
  suspend fun fetchLiveProductImages(
    query: String,
    limit: Int = 5,
  ): List<String> =
    withContext(Dispatchers.IO) {
      val clean = query.trim()
      if (clean.length < 2) return@withContext emptyList()
      val urls = linkedSetOf<String>()
      val enc = URLEncoder.encode(clean, "UTF-8")

      fun acceptRasterUrl(raw: String): Boolean {
        val u = raw.trim()
        val low = u.lowercase()
        if (!low.startsWith("http://") && !low.startsWith("https://")) return false
        if (low.contains(".svg") || low.contains("logo") || low.contains("icon") || low.contains("favicon")) return false
        return true
      }

      // 1. Google Custom Search Image API (redundant key failover)
      val csePairs = ApiKeyRing.googleCsePairs()
      if (urls.size < limit && csePairs.isNotEmpty()) {
        runCatching {
          ApiKeyRing.executeWithCseFailover(csePairs) { rawKey, rawCx ->
            val key = URLEncoder.encode(rawKey, "UTF-8")
            val cx = URLEncoder.encode(rawCx, "UTF-8")
            val url = "https://www.googleapis.com/customsearch/v1?key=$key&cx=$cx&searchType=image&num=${limit.coerceIn(3, 8)}&safe=active&q=$enc"
            httpClient.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
              if (ApiKeyRing.isFailoverCode(resp.code)) throw ApiKeyFailoverException(resp.code, "Google CSE HTTP ${resp.code}")
              if (resp.isSuccessful) {
                val arr = JSONObject(resp.body?.string().orEmpty()).optJSONArray("items")
                if (arr != null) {
                  for (i in 0 until arr.length()) {
                    val link = arr.optJSONObject(i)?.optString("link").orEmpty()
                    if (acceptRasterUrl(link)) urls += link
                    if (urls.size >= limit) break
                  }
                }
              }
            }
          }
        }
      }

      // 2. Brave Image Search API (redundant key failover)
      val braveKeys = ApiKeyRing.braveKeys()
      if (urls.size < limit && braveKeys.isNotEmpty()) {
        runCatching {
          val url = "https://api.search.brave.com/res/v1/images/search?q=$enc&count=${limit.coerceIn(3, 8)}&safesearch=strict"
          ApiKeyRing.executeWithFailover("BRAVE", braveKeys) { rawKey ->
            val req =
              Request.Builder()
                .url(url)
                .header("X-Subscription-Token", rawKey)
                .header("Accept", "application/json")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
              if (ApiKeyRing.isFailoverCode(resp.code)) throw ApiKeyFailoverException(resp.code, "Brave HTTP ${resp.code}")
              if (resp.isSuccessful) {
                val arr = JSONObject(resp.body?.string().orEmpty()).optJSONArray("results")
                if (arr != null) {
                  for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val propUrl = o.optJSONObject("properties")?.optString("url").orEmpty()
                    val thumbUrl = o.optJSONObject("thumbnail")?.optString("src").orEmpty()
                    val pick = if (acceptRasterUrl(propUrl)) propUrl else thumbUrl
                    if (acceptRasterUrl(pick)) urls += pick
                    if (urls.size >= limit) break
                  }
                }
              }
            }
          }
        }
      }

      // 3. Tavily Search API with include_images=true (redundant key failover)
      val tavilyKeys = ApiKeyRing.tavilyKeys()
      if (urls.size < limit && tavilyKeys.isNotEmpty()) {
        runCatching {
          ApiKeyRing.executeWithFailover("TAVILY", tavilyKeys) { rawKey ->
            val payload =
              JSONObject().apply {
                put("api_key", rawKey)
                put("query", "$clean product photo")
                put("search_depth", "basic")
                put("include_images", true)
                put("max_results", 5)
              }.toString()
            val req =
              Request.Builder()
                .url("https://api.tavily.com/search")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            httpClient.newCall(req).execute().use { resp ->
              if (ApiKeyRing.isFailoverCode(resp.code)) throw ApiKeyFailoverException(resp.code, "Tavily HTTP ${resp.code}")
              if (resp.isSuccessful) {
                val imgs = JSONObject(resp.body?.string().orEmpty()).optJSONArray("images")
                if (imgs != null) {
                  for (i in 0 until imgs.length()) {
                    val link = imgs.optString(i).orEmpty()
                    if (acceptRasterUrl(link)) urls += link
                    if (urls.size >= limit) break
                  }
                }
              }
            }
          }
        }
      }

      val browserUa =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

      // 4. Keyless Live Bing Image Search (parses real HTTPS product photos & fast Bing CDN thumbnails)
      if (urls.size < limit) {
        runCatching {
          val bingUrl = "https://www.bing.com/images/search?q=$enc&form=HDRSC2&first=1&safeSearch=Strict"
          val req =
            Request.Builder()
              .url(bingUrl)
              .header("User-Agent", browserUa)
              .header("Accept", "text/html,application/xhtml+xml")
              .header("Accept-Language", "en-US,en;q=0.9")
              .get()
              .build()
          httpClient.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) {
              val html = resp.body?.string().orEmpty()
              // Prefer reliable CDN thumbnails (turl -> tse*.mm.bing.net) interleaved with direct murl sources
              val turlRe = Regex("""(?:&quot;|")turl(?:&quot;|")\s*:\s*(?:&quot;|")(https?:\\?/\\?/[^"&]+)(?:&quot;|")""")
              val murlRe = Regex("""(?:&quot;|")murl(?:&quot;|")\s*:\s*(?:&quot;|")(https?:\\?/\\?/[^"&]+)(?:&quot;|")""")
              val turls =
                turlRe.findAll(html).map { m ->
                  m.groupValues[1].replace("\\/", "/").replace("&amp;", "&")
                }.filter { it.startsWith("https://") && acceptRasterUrl(it) }.distinct().toList()
              val murls =
                murlRe.findAll(html).map { m ->
                  m.groupValues[1].replace("\\/", "/").replace("&amp;", "&")
                }.filter { it.startsWith("https://") && acceptRasterUrl(it) }.distinct().toList()

              val maxPairs = maxOf(turls.size, murls.size)
              for (idx in 0 until maxPairs) {
                if (idx < turls.size && urls.size < limit) urls += turls[idx]
                if (idx < murls.size && urls.size < limit) urls += murls[idx]
                if (urls.size >= limit) break
              }
            }
          }
        }
      }

      // 5. Keyless Live Shopify PEV Dealer Product Photography (official OEM/dealer product CDN photos)
      if (urls.size < limit) {
        val dealerHosts =
          listOf(
            "www.voromotors.com",
            "alienrides.com",
            "www.revrides.com",
            "ewheels.com",
            "chargedcycleworks.com",
            "gritshift.com",
            "minimotorsusa.com",
          )
        val shortQ = URLEncoder.encode(clean.take(64), "UTF-8")
        runCatching {
          coroutineScope {
            dealerHosts.map { host ->
              async {
                runCatching {
                  val sUrl = "https://$host/search/suggest.json?q=$shortQ&resources[type]=product&resources[limit]=3"
                  val req =
                    Request.Builder()
                      .url(sUrl)
                      .header("User-Agent", browserUa)
                      .header("Accept", "application/json")
                      .get()
                      .build()
                  httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use emptyList<String>()
                    val arr =
                      JSONObject(resp.body?.string().orEmpty())
                        .optJSONObject("resources")
                        ?.optJSONObject("results")
                        ?.optJSONArray("products")
                        ?: return@use emptyList<String>()
                    val found = mutableListOf<String>()
                    for (i in 0 until arr.length()) {
                      val p = arr.optJSONObject(i) ?: continue
                      val rawImg =
                        p.optString("image").ifBlank {
                          p.optJSONObject("featured_image")?.optString("url").orEmpty()
                        }.trim()
                      val normalized =
                        when {
                          rawImg.startsWith("//") -> "https:$rawImg"
                          rawImg.startsWith("http://") -> rawImg.replaceFirst("http://", "https://")
                          else -> rawImg
                        }
                      if (normalized.startsWith("https://") && acceptRasterUrl(normalized)) {
                        found += normalized
                      }
                    }
                    found
                  }
                }.getOrDefault(emptyList())
              }
            }.awaitAll().flatten()
          }.forEach { u ->
            if (urls.size < limit) urls += u
          }
        }
      }

      // 6. Keyless DuckDuckGo Live Image Search (vqd token + i.js JSON API)
      if (urls.size < limit) {
        runCatching {
          val seedUrl = "https://duckduckgo.com/?q=$enc&iax=images&ia=images"
          val seedReq = Request.Builder().url(seedUrl).header("User-Agent", browserUa).get().build()
          val vqd =
            httpClient.newCall(seedReq).execute().use { resp ->
              if (!resp.isSuccessful) ""
              else {
                val body = resp.body?.string().orEmpty()
                Regex("""vqd=["']?([0-9a-zA-Z_-]+)["']?""").find(body)?.groupValues?.get(1).orEmpty()
              }
            }
          if (vqd.isNotBlank()) {
            val ijsUrl = "https://duckduckgo.com/i.js?l=us-en&o=json&q=$enc&vqd=${URLEncoder.encode(vqd, "UTF-8")}&f=,,,&p=1"
            val ijsReq =
              Request.Builder()
                .url(ijsUrl)
                .header("User-Agent", browserUa)
                .header("Referer", "https://duckduckgo.com/")
                .header("Accept", "application/json")
                .get()
                .build()
            httpClient.newCall(ijsReq).execute().use { resp ->
              if (resp.isSuccessful) {
                val arr = JSONObject(resp.body?.string().orEmpty()).optJSONArray("results")
                if (arr != null) {
                  for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val thumb = o.optString("thumbnail").trim()
                    val full = o.optString("image").trim()
                    if (thumb.startsWith("https://") && acceptRasterUrl(thumb) && urls.size < limit) urls += thumb
                    if (full.startsWith("https://") && acceptRasterUrl(full) && urls.size < limit) urls += full
                    if (urls.size >= limit) break
                  }
                }
              }
            }
          }
        }
      }

      // 7. Keyless Openverse Creative Commons Image Search
      if (urls.size < limit) {
        runCatching {
          val ovUrl = "https://api.openverse.org/v1/images/?q=$enc&page_size=${limit.coerceIn(3, 8)}"
          val req = Request.Builder().url(ovUrl).header("User-Agent", "RuralERidesGO/32.0 Android").get().build()
          httpClient.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) {
              val arr = JSONObject(resp.body?.string().orEmpty()).optJSONArray("results")
              if (arr != null) {
                for (i in 0 until arr.length()) {
                  val o = arr.optJSONObject(i) ?: continue
                  val pick = o.optString("thumbnail").ifBlank { o.optString("url") }
                  if (acceptRasterUrl(pick)) urls += pick
                  if (urls.size >= limit) break
                }
              }
            }
          }
        }
      }

      // 8. Keyless Wikimedia Commons & Wikipedia Multi-Image Fallback
      if (urls.size < limit) {
        runCatching {
          val wikiUrl =
            "https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrsearch=$enc&gsrnamespace=6&gsrlimit=${limit + 3}&prop=imageinfo&iiprop=url|mime&iiurlwidth=720&format=json"
          val req = Request.Builder().url(wikiUrl).header("User-Agent", "RuralERidesGO/11.0 Android").get().build()
          httpClient.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) {
              val pages = JSONObject(resp.body?.string().orEmpty()).optJSONObject("query")?.optJSONObject("pages")
              if (pages != null) {
                val keys = pages.keys()
                while (keys.hasNext() && urls.size < limit) {
                  val p = pages.optJSONObject(keys.next()) ?: continue
                  val info = p.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
                  val mime = info.optString("mime").lowercase()
                  if (!mime.startsWith("image/") || mime.contains("svg")) continue
                  val thumb = info.optString("thumburl").ifBlank { info.optString("url") }
                  if (acceptRasterUrl(thumb)) urls += thumb
                }
              }
            }
          }
        }
      }

      if (urls.size < limit) {
        runCatching {
          val wpUrl =
            "https://en.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch=$enc&gsrlimit=6&prop=pageimages&piprop=thumbnail&pithumbsize=720&format=json"
          val req = Request.Builder().url(wpUrl).header("User-Agent", "RuralERidesGO/11.0 Android").get().build()
          httpClient.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) {
              val pages = JSONObject(resp.body?.string().orEmpty()).optJSONObject("query")?.optJSONObject("pages")
              if (pages != null) {
                val keys = pages.keys()
                while (keys.hasNext() && urls.size < limit) {
                  val p = pages.optJSONObject(keys.next()) ?: continue
                  val src = p.optJSONObject("thumbnail")?.optString("source").orEmpty()
                  if (acceptRasterUrl(src)) urls += src
                }
              }
            }
          }
        }
      }

      urls.toList().take(limit)
    }

  private fun mapManeuverType(type: String, modifier: String): String {
    val t = type.lowercase()
    val m = modifier.lowercase()
    return when {
      t == "arrive" -> "arrive"
      t == "depart" -> "depart"
      t.contains("roundabout") || t == "rotary" -> "roundabout"
      m.contains("uturn") -> "uturn"
      m.contains("sharp left") -> "sharp_left"
      m.contains("sharp right") -> "sharp_right"
      m.contains("slight left") -> "slight_left"
      m.contains("slight right") -> "slight_right"
      m.contains("left") -> "left"
      m.contains("right") -> "right"
      else -> "straight"
    }
  }

  private fun mapOrsType(code: Int): String =
    when (code) {
      0 -> "left"
      1 -> "right"
      2 -> "sharp_left"
      3 -> "sharp_right"
      4 -> "slight_left"
      5 -> "slight_right"
      7, 8 -> "roundabout"
      9 -> "uturn"
      10 -> "arrive"
      11 -> "depart"
      else -> "straight"
    }

  private fun buildHumanInstruction(type: String, modifier: String, road: String): String {
    val onRoad = if (road.isNotBlank()) " onto $road" else ""
    return when (type.lowercase()) {
      "depart" -> if (road.isNotBlank()) "Head out on $road" else "Depart along route"
      "arrive" -> "Arrive at your destination"
      "roundabout", "rotary" -> "Enter roundabout$onRoad"
      "end of road" -> "At end of road, turn ${modifier.ifBlank { "ahead" }}$onRoad"
      "fork" -> "Keep ${modifier.ifBlank { "straight" }} at fork$onRoad"
      "merge" -> "Merge ${modifier.ifBlank { "" }}$onRoad".trim()
      "turn", "new name", "continue" ->
        if (modifier.isBlank() || modifier.equals("straight", true)) "Continue straight$onRoad"
        else "Turn $modifier$onRoad"
      else -> if (modifier.isNotBlank()) "Bear $modifier$onRoad" else "Continue$onRoad"
    }
  }

  data class VerifiedStoreOffer(
    val storeId: String,
    val storeName: String,
    val url: String,
    val priceHint: String = "",
    val stockBadge: String = "In Stock Verified",
  )

  private val productStopWords =
    setOf("electric", "part", "parts", "kit", "with", "for", "and", "the", "pev", "set", "pair", "pack", "for", "new", "edition")

  internal fun extractStoreProductTokens(productTitle: String): List<String> {
    val cleaned =
      productTitle
        .replace(Regex("""\((?:Scooter|EUC|Onewheel|eSkate|Sur-Ron|CNC Billet)[^)]*\)""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""\bw/\s*""", RegexOption.IGNORE_CASE), " ")
    val rawParts =
      cleaned
        .split(Regex("""[\s,/|()]+"""))
        .map { it.trim().trimEnd('.', '-', ':') }
        .filter { it.isNotBlank() }

    val tokens = linkedSetOf<String>()
    var prevAlnum = ""
    rawParts.forEach { raw ->
      val lower = raw.lowercase()
      val isSingleSuffix =
        raw.length == 1 && (raw[0].isUpperCase() || raw[0].isDigit())
      if (isSingleSuffix && prevAlnum.isNotBlank()) {
        tokens += "$prevAlnum $lower"
      } else if (lower.length >= 2 && lower !in productStopWords) {
        tokens += lower
        // Also preserve base alphanumeric model stem when suffix has punctuation like "aventure.2" -> "aventure"
        if (lower.contains(".")) {
          lower.split(".").filter { it.length >= 2 }.forEach { tokens += it }
        }
      }
      if (lower.length >= 2 && lower !in productStopWords) {
        prevAlnum = lower
      }
    }
    return tokens.toList().take(7)
  }

  private fun containsToken(haystackLower: String, token: String): Boolean {
    if (token.isBlank()) return false
    return if (token.length <= 3 || token.contains(" ")) {
      Regex("""(?:^|[^a-z0-9])${Regex.escape(token)}(?:$|[^a-z0-9])""").containsMatchIn(haystackLower)
    } else {
      haystackLower.contains(token)
    }
  }

  internal fun scoreProductTitleMatch(candidateTitleLower: String, targetTitle: String, tokens: List<String>): Int {
    if (tokens.isEmpty() || candidateTitleLower.isBlank()) return 0
    val targetLower = targetTitle.lowercase()
    val accessoryNoise = listOf("t-shirt", "shirt", "sticker", "decal", "keychain", "hat ", "cap ", "gift card", "manual", "poster")
    if (accessoryNoise.any { noise -> candidateTitleLower.contains(noise) && !targetLower.contains(noise.trim()) }) {
      return -10
    }
    var score = 0
    tokens.forEachIndexed { idx, tok ->
      if (containsToken(candidateTitleLower, tok)) {
        score += if (idx < 2 || tok.any { it.isDigit() }) 3 else 2
      }
    }
    // Bonus if the brand/first token AND numeric model token both appear
    val numericTokens = tokens.filter { t -> t.any { it.isDigit() } }
    if (numericTokens.isNotEmpty() && numericTokens.none { containsToken(candidateTitleLower, it) }) {
      score -= 3
    }
    return score
  }

  internal fun extractDirectProductUrlFromHtml(
    html: String,
    baseUrl: String,
    targetTitle: String,
    tokens: List<String>,
  ): String? {
    val baseUri = runCatching { URI(baseUrl) }.getOrNull() ?: return null
    val baseHost = baseUri.host?.lowercase()?.removePrefix("www.").orEmpty()
    if (baseHost.isBlank()) return null

    val linkRegex = Regex("""<a\s+[^>]*href\s*=\s*["']([^"'#]+)["'][^>]*>(.*?)</a>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    val ignoredPathFragments = listOf("/cart", "/checkout", "/account", "/login", "/search", "/collections/", "/collection/", "/category/", "/categories/", "/tag/", "/blog", "/pages/", "/policies/", "mailto:", "javascript:")

    var bestUrl: String? = null
    var bestScore = 0
    val minRequired = if (tokens.size <= 1) 2 else 4

    linkRegex.findAll(html.take(150_000)).forEach { match ->
      val rawHref = match.groupValues[1].trim()
      if (rawHref.isBlank() || rawHref.startsWith("javascript:", true) || rawHref.startsWith("mailto:", true)) return@forEach
      val resolved = runCatching { baseUri.resolve(rawHref) }.getOrNull() ?: return@forEach
      val resolvedHost = resolved.host?.lowercase()?.removePrefix("www.").orEmpty()
      if (resolvedHost != baseHost && !resolvedHost.endsWith(".$baseHost")) return@forEach
      val absUrl = resolved.toString()
      val pathLower = resolved.path?.lowercase().orEmpty()
      if (pathLower.isBlank() || pathLower == "/" || ignoredPathFragments.any { pathLower.contains(it) }) return@forEach

      val looksLikeProductPath =
        pathLower.contains("/products/") ||
          pathLower.contains("/product/") ||
          pathLower.contains("/itm/") ||
          pathLower.contains("/dp/") ||
          pathLower.contains("/ip/") ||
          pathLower.contains("/p/") ||
          (pathLower.endsWith(".html") && pathLower.count { it == '/' } >= 1)
      if (!looksLikeProductPath) return@forEach

      val anchorText = match.groupValues[2].replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim().lowercase()
      val slugText = pathLower.replace(Regex("""[-_/]+"""), " ").lowercase()
      val combined = "$slugText $anchorText"
      val score = scoreProductTitleMatch(combined, targetTitle, tokens)
      if (score >= minRequired && score > bestScore) {
        bestScore = score
        bestUrl = absUrl.substringBefore("#")
      }
    }
    return bestUrl
  }

  internal fun extractCleanPriceHint(rawHtml: String): String {
    val snippet = rawHtml.take(120_000)
    val metaPrice =
      Regex("""(?:property|name)\s*=\s*["'](?:og:price:amount|product:price:amount)["'][^>]*content\s*=\s*["'](\d+(?:[.,]\d{1,2})?)["']""", RegexOption.IGNORE_CASE)
        .find(snippet)
        ?.groupValues
        ?.getOrNull(1)
        ?.replace(",", "")
        ?.toDoubleOrNull()
    if (metaPrice != null && metaPrice >= 5.0) {
      return formatUsdPrice(metaPrice)
    }
    val jsonPrice =
      Regex(""""price"\s*:\s*"?(\d{1,5}(?:\.\d{2})?)"?""", RegexOption.IGNORE_CASE)
        .find(snippet)
        ?.groupValues
        ?.getOrNull(1)
        ?.toDoubleOrNull()
    if (jsonPrice != null && jsonPrice >= 5.0) {
      return formatUsdPrice(jsonPrice)
    }
    val dollarRegex = Regex("""\$\s?(\d{1,2}(?:,\d{3})+|\d{2,4}(?:\.\d{2})?|\d\.\d{2})""")
    val match = dollarRegex.find(snippet)?.value?.replace(" ", "").orEmpty()
    return match
  }

  private fun formatUsdPrice(value: Double): String {
    if (value <= 0.0) return ""
    val whole = value.toLong()
    val hasCents = kotlin.math.abs(value - whole.toDouble()) >= 0.005
    return if (hasCents) String.format(java.util.Locale.US, "$%,.2f", value)
    else String.format(java.util.Locale.US, "$%,d", whole)
  }

  private fun isSearchLikeUrl(url: String): Boolean {
    val u = url.lowercase()
    return u.contains("/search") ||
      u.contains("?q=") ||
      u.contains("&q=") ||
      u.contains("?s=") ||
      u.contains("?k=") ||
      u.contains("_nkw=") ||
      u.contains("search_query=") ||
      u.contains("searchterm=") ||
      u.contains("catalogsearch")
  }

  /**
   * Live HTTP stock verifier & direct product destination resolver:
   * 1. Queries Shopify `/search/suggest.json` for Shopify storefronts to resolve the exact `/products/<handle>` URL, price, and `available` stock.
   * 2. Validates direct product URLs (rejecting homepage redirects, bot walls, out-of-stock pages, and non-matching titles).
   * 3. Extracts & verifies direct product URLs from non-Shopify search pages when present.
   * 4. Falls back to live web grounding discovery if `< 2` verified stores are found and web keys are configured.
   */
  suspend fun findVerifiedInStockStores(
    productTitle: String,
    candidateStores: List<Pair<String, Pair<String, String>>>,
  ): List<VerifiedStoreOffer> =
    withContext(Dispatchers.IO) {
      val cleanTitle = productTitle.trim()
      val tokens = extractStoreProductTokens(cleanTitle)
      if (tokens.isEmpty()) return@withContext emptyList()

      val stockClient =
        httpClient
          .newBuilder()
          .connectTimeout(5, TimeUnit.SECONDS)
          .readTimeout(7, TimeUnit.SECONDS)
          .followRedirects(true)
          .build()

      val outOfStockPhrases =
        listOf(
          "out of stock",
          "sold out",
          "currently unavailable",
          "temporarily out of stock",
          "backordered",
          "discontinued",
          "no results found",
          "0 results",
          "nothing found",
          "no products were found",
          "did not match any products",
          "we couldn't find any results",
          "page not found",
          "404 not found",
          "\"availability\":\"http://schema.org/outofstock\"",
          "\"availability\":\"https://schema.org/outofstock\"",
          "\"available\":false",
        )

      val inStockSignals =
        listOf(
          "\"availability\":\"http://schema.org/instock\"",
          "\"availability\":\"https://schema.org/instock\"",
          "\"available\":true",
          "add to cart",
          "add to bag",
          "buy now",
          "in stock",
          "ships today",
          "ready to ship",
          "add-to-cart",
          "product-price",
        )

      fun executeGet(targetUrl: String, acceptHeader: String = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"): Pair<String, String>? {
        val req =
          Request.Builder()
            .url(targetUrl)
            .header(
              "User-Agent",
              "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36",
            )
            .header("Accept", acceptHeader)
            .header("Accept-Language", "en-US,en;q=0.9")
            .get()
            .build()
        return stockClient.newCall(req).execute().use { resp ->
          if (!resp.isSuccessful) return@use null
          val finalUrl = resp.request.url.toString()
          val bodyStr = resp.body?.string().orEmpty()
          finalUrl to bodyStr
        }
      }

      fun verifyHtmlOffer(storeId: String, storeName: String, url: String, allowSecondHop: Boolean = true): VerifiedStoreOffer? {
        if (!url.startsWith("http") || PevStoreDirectory.isHomepageUrl(url) || PevStoreDirectory.isGenericGoogleSearchUrl(url)) {
          return null
        }
        val (finalUrl, bodyStr) = executeGet(url) ?: return null
        if (PevStoreDirectory.isHomepageUrl(finalUrl) || PevStoreDirectory.isGenericGoogleSearchUrl(finalUrl)) {
          return null
        }
        if (bodyStr.length < 400) return null
        val lower = bodyStr.take(160_000).lowercase()

        if (
          lower.contains("enter the characters you see below") ||
            lower.contains("robot check") ||
            (lower.contains("captcha") && lower.length < 18_000)
        ) {
          return null
        }

        val hasZeroResults =
          lower.contains("no results found") ||
            lower.contains("0 results") ||
            lower.contains("nothing found") ||
            lower.contains("no products were found") ||
            lower.contains("did not match any products") ||
            lower.contains("we couldn't find any results") ||
            lower.contains("page not found")
        if (hasZeroResults) return null

        val isListing = isSearchLikeUrl(finalUrl)
        if (isListing && allowSecondHop) {
          val resolvedProductUrl = extractDirectProductUrlFromHtml(bodyStr, finalUrl, cleanTitle, tokens)
          if (resolvedProductUrl != null && resolvedProductUrl != finalUrl) {
            verifyHtmlOffer(storeId, storeName, resolvedProductUrl, allowSecondHop = false)?.let { directOffer ->
              return directOffer
            }
          }
        }

        // Strip nav/header/footer noise where possible so global navbar links don't false-match
        val titleBlock =
          Regex("""<title[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(lower)
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()
        val mainBodyLower =
          lower
            .replace(Regex("""<footer\b.*?</footer>""", RegexOption.DOT_MATCHES_ALL), " ")
            .replace(Regex("""<nav\b.*?</nav>""", RegexOption.DOT_MATCHES_ALL), " ")
        val searchableText = "$titleBlock $mainBodyLower"

        val matchedTokens = tokens.count { tok -> containsToken(searchableText, tok) }
        val requiredTokens = if (tokens.size <= 1) tokens.size else 2.coerceAtMost(tokens.size)
        if (matchedTokens < requiredTokens) return null

        val isDirectProductPage = !isSearchLikeUrl(finalUrl)
        if (isDirectProductPage) {
          val hasSoldOut =
            outOfStockPhrases.any { phrase -> lower.contains(phrase) } &&
              !lower.contains("\"availability\":\"https://schema.org/instock\"") &&
              !lower.contains("\"availability\":\"http://schema.org/instock\"")
          if (hasSoldOut) return null
        }

        val hasPositiveSignal = inStockSignals.any { sig -> lower.contains(sig) }
        val priceMatch = extractCleanPriceHint(bodyStr)
        if (!hasPositiveSignal && priceMatch.isBlank()) return null

        return VerifiedStoreOffer(
          storeId = storeId,
          storeName = storeName,
          url = finalUrl,
          priceHint = priceMatch,
          stockBadge = if (isDirectProductPage) "Direct Product • In Stock" else "In Stock Listing",
        )
      }

      fun tryResolveShopifySuggest(storeId: String, storeName: String, originHost: String): Pair<Boolean, VerifiedStoreOffer?> {
        val encQ = URLEncoder.encode(cleanTitle.take(72), "UTF-8")
        val suggestUrl =
          "https://$originHost/search/suggest.json?q=$encQ&resources[type]=product&resources[limit]=6&resources[options][unavailable_products]=hide"
        val (_, jsonStr) = executeGet(suggestUrl, "application/json") ?: return false to null
        if (!jsonStr.trimStart().startsWith("{")) return false to null
        val parsed = runCatching { JSONObject(jsonStr) }.getOrNull() ?: return false to null
        val products =
          parsed
            .optJSONObject("resources")
            ?.optJSONObject("results")
            ?.optJSONArray("products")
            ?: return false to null

        var bestOffer: VerifiedStoreOffer? = null
        var bestScore = 0
        val minScore = if (tokens.size <= 1) 2 else 4

        for (i in 0 until products.length()) {
          val p = products.optJSONObject(i) ?: continue
          if (p.has("available") && !p.optBoolean("available", true)) continue
          val title = p.optString("title").trim()
          val vendor = p.optString("vendor").trim()
          val relUrl = p.optString("url").trim()
          if (title.isBlank() || relUrl.isBlank()) continue

          val score = scoreProductTitleMatch("${title.lowercase()} ${vendor.lowercase()}", cleanTitle, tokens)
          if (score >= minScore && score > bestScore) {
            val cleanPath = relUrl.substringBefore("?").let { if (it.startsWith("/")) it else "/$it" }
            val destUrl =
              if (relUrl.startsWith("http", true)) relUrl.substringBefore("?")
              else "https://$originHost$cleanPath"
            if (!PevStoreDirectory.isHomepageUrl(destUrl)) {
              val priceNum = p.optString("price").replace(",", "").toDoubleOrNull() ?: 0.0
              val priceStr = if (priceNum >= 5.0) formatUsdPrice(priceNum) else ""
              bestScore = score
              bestOffer =
                VerifiedStoreOffer(
                  storeId = storeId,
                  storeName = storeName,
                  url = destUrl,
                  priceHint = priceStr,
                  stockBadge = "Direct Product • In Stock",
                )
            }
          }
        }
        return true to bestOffer
      }

      suspend fun checkCandidate(storeId: String, storeName: String, url: String): VerifiedStoreOffer? =
        runCatching {
          val storeEntry = PevStoreDirectory.KNOWN_PEV_STORES.firstOrNull { it.id == storeId }
          val urlHost = runCatching { URI(url).host.orEmpty() }.getOrDefault("")
          if ((storeEntry?.isShopify == true) && urlHost.isNotBlank()) {
            val (handledJson, shopifyOffer) = tryResolveShopifySuggest(storeId, storeName, urlHost)
            if (shopifyOffer != null) return@runCatching shopifyOffer
            // If direct product URL was given (e.g. verified:: or built-in deep URL), still verify it directly
            if (handledJson && isSearchLikeUrl(url)) return@runCatching null
          }
          verifyHtmlOffer(storeId, storeName, url, allowSecondHop = true)
        }.getOrNull()

      val primaryOffers =
        coroutineScope {
          candidateStores
            .distinctBy { it.first }
            .take(8)
            .map { (storeId, pair) ->
              val (storeName, url) = pair
              async { checkCandidate(storeId, storeName, url) }
            }
            .awaitAll()
            .filterNotNull()
        }

      if (primaryOffers.size >= 2) {
        return@withContext primaryOffers
          .sortedByDescending { it.stockBadge.startsWith("Direct Product") }
      }

      // Live Web Auto-Discovery Fallback (works keyless via Bing RSS + DuckDuckGo HTML)
      val discoveredOffers =
        runCatching {
          val webCtx = webSearchGrounding("\"$cleanTitle\" buy price \"in stock\"", deepMode = false)
          val alreadyVerifiedHosts =
            primaryOffers
              .mapNotNull { runCatching { URI(it.url).host?.lowercase()?.removePrefix("www.") }.getOrNull() }
              .toSet()
          val blockedDomains =
            setOf("youtube.com", "youtu.be", "reddit.com", "wikipedia.org", "facebook.com", "instagram.com", "tiktok.com", "pinterest.com", "google.com")
          val urlMatches =
            Regex("""https?://[^\s\)\]"'<>]+""")
              .findAll(webCtx)
              .map { it.value.trimEnd('.', ',', ';', ')') }
              .filter { u ->
                !PevStoreDirectory.isHomepageUrl(u) &&
                  !PevStoreDirectory.isGenericGoogleSearchUrl(u)
              }
              .distinctBy { u ->
                runCatching { URI(u).host?.lowercase()?.removePrefix("www.") }.getOrNull() ?: u
              }
              .filter { u ->
                val host = runCatching { URI(u).host?.lowercase()?.removePrefix("www.").orEmpty() }.getOrDefault("")
                host.isNotBlank() &&
                  host !in alreadyVerifiedHosts &&
                  blockedDomains.none { b -> host == b || host.endsWith(".$b") }
              }
              .take(3)
              .toList()

          coroutineScope {
            urlMatches.map { u ->
              async {
                val matchedEntry = PevStoreDirectory.findStoreByUrl(u)
                val hostLabel =
                  runCatching {
                    URI(u).host?.lowercase()?.removePrefix("www.")?.substringBefore(".")?.replaceFirstChar { it.uppercase() }
                  }.getOrNull().orEmpty()
                val sId = matchedEntry?.id ?: "web_${hostLabel.lowercase()}"
                val sName = matchedEntry?.name ?: hostLabel.ifBlank { "Online Store" }
                verifyHtmlOffer(sId, sName, u, allowSecondHop = false)
              }
            }.awaitAll().filterNotNull()
          }
        }.getOrDefault(emptyList())

      (primaryOffers + discoveredOffers)
        .distinctBy { it.storeId }
        .sortedByDescending { it.stockBadge.startsWith("Direct Product") }
    }

  private fun timedHttpPing(
    name: String,
    category: String,
    configured: Boolean,
    missingMsg: String,
    url: String,
  ): DiagnosticCheckResult {
    if (!configured) {
      return DiagnosticCheckResult(name, category, false, 0L, missingMsg)
    }
    val start = System.currentTimeMillis()
    return try {
      httpClient.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
        val elapsed = System.currentTimeMillis() - start
        DiagnosticCheckResult(
          name = name,
          category = category,
          ok = resp.isSuccessful,
          latencyMs = elapsed,
          detail = if (resp.isSuccessful) "HTTP ${resp.code} OK" else "HTTP ${resp.code} Error",
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      DiagnosticCheckResult(
        name = name,
        category = category,
        ok = false,
        latencyMs = System.currentTimeMillis() - start,
        detail = e.localizedMessage ?: "Connection error",
      )
    }
  }
}
