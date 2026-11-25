package com.example.core

import android.graphics.BitmapFactory
import com.example.data.PevStoreDirectory
import com.example.ui.components.bitmapToCloudBase64DataUri
import java.net.URI
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

internal object ProductAndStoreClient {
  private val httpClient get() = LiveCloudServices.httpClient

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

      // 2. Keyless Multi-Engine Live Product Image Search
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

  private val brandLogoCache = ConcurrentHashMap<String, String>()

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
        candidateUrls += "https://www.google.com/s2/favicons?domain=${URLEncoder.encode(domain, "UTF-8")}&sz=128"
        candidateUrls += "https://icons.duckduckgo.com/ip3/$domain.ico"
      }

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

      // 1. Google Custom Search Image API
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

      // 2. Brave Image Search API
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

      // 3. Tavily Search API with include_images=true
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

      // 4. Keyless Live Bing Image Search
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

      // 5. Keyless Live Shopify PEV Dealer Product Photography
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

      // 6. Keyless DuckDuckGo Live Image Search
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

  suspend fun findVerifiedInStockStores(
    productTitle: String,
    candidateStores: List<Pair<String, Pair<String, String>>>,
  ): List<LiveCloudServices.VerifiedStoreOffer> =
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

      fun verifyHtmlOffer(storeId: String, storeName: String, url: String, allowSecondHop: Boolean = true): LiveCloudServices.VerifiedStoreOffer? {
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

        return LiveCloudServices.VerifiedStoreOffer(
          storeId = storeId,
          storeName = storeName,
          url = finalUrl,
          priceHint = priceMatch,
          stockBadge = if (isDirectProductPage) "Direct Product • In Stock" else "In Stock Listing",
        )
      }

      fun tryResolveShopifySuggest(storeId: String, storeName: String, originHost: String): Pair<Boolean, LiveCloudServices.VerifiedStoreOffer?> {
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

        var bestOffer: LiveCloudServices.VerifiedStoreOffer? = null
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
                LiveCloudServices.VerifiedStoreOffer(
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

      suspend fun checkCandidate(storeId: String, storeName: String, url: String): LiveCloudServices.VerifiedStoreOffer? =
        runCatching {
          val storeEntry = PevStoreDirectory.KNOWN_PEV_STORES.firstOrNull { it.id == storeId }
          val urlHost = runCatching { URI(url).host.orEmpty() }.getOrDefault("")
          if ((storeEntry?.isShopify == true) && urlHost.isNotBlank()) {
            val (handledJson, shopifyOffer) = tryResolveShopifySuggest(storeId, storeName, urlHost)
            if (shopifyOffer != null) return@runCatching shopifyOffer
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

      val discoveredOffers =
        runCatching {
          val webCtx = NewsAndSearchClient.webSearchGrounding("\"$cleanTitle\" buy price \"in stock\"", deepMode = false)
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
}
