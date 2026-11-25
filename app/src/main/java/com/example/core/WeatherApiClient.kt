package com.example.core

import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import com.example.data.DailyForecast
import com.example.data.HourlyForecast
import com.example.data.WeatherGeoLocation
import com.example.data.WeatherMatrixData

internal object WeatherApiClient {
  private val httpClient get() = LiveCloudServices.httpClient

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
      if (LiveCloudServices.hasMapboxKey()) {
        val mb =
          runCatching {
            val token = URLEncoder.encode(LiveCloudServices.mapboxToken(), "UTF-8")
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
        if (LiveCloudServices.hasWeatherKey()) {
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
}
