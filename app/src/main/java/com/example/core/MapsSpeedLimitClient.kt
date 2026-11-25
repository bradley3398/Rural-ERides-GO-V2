package com.example.core

import com.example.data.ActiveNavRoute
import com.example.data.LiveChargingStation
import com.example.data.NavManeuverStep
import com.example.data.NavRouteProfile
import com.example.data.WeatherGeoLocation
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

internal object MapsSpeedLimitClient {
  private val httpClient get() = LiveCloudServices.httpClient
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

  suspend fun fetchLiveRoadAndSpeedLimit(lat: Double, lng: Double): Result<LiveCloudServices.RoadSignInfo> =
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
        if (road.isBlank() && LiveCloudServices.hasMapboxKey()) {
          runCatching {
            val token = URLEncoder.encode(LiveCloudServices.mapboxToken(), "UTF-8")
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

        LiveCloudServices.RoadSignInfo(
          roadName = road.ifBlank { "Rural Sector Trail" },
          speedLimitMph = limitMph,
          inSchoolZone = schoolNearby,
        )
      }
    }

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
        if (nomRes.isNotEmpty()) nomRes else WeatherApiClient.searchWeatherLocations(clean).getOrDefault(emptyList())
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
}
