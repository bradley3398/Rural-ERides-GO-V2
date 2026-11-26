package com.example.core

import android.content.Context
import android.location.LocationManager
import android.util.Log
import com.example.R
import com.example.data.NewsArticleItem
import com.example.data.SupabaseAuthService
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.io.StringReader
import java.net.URLEncoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

internal object NewsAndSearchClient {
  private const val TAG = "NewsAndSearchClient"
  private val httpClient get() = LiveCloudServices.httpClient

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
        // 4. Keyless Live Bing Web Search RSS
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

        // 5. Keyless Live DuckDuckGo HTML Search
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

        // 6. Keyless Wikipedia OpenSearch fallback
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

  suspend fun runLiveConnectionDiagnostics(
    context: Context,
    supabaseService: SupabaseAuthService,
    firebaseSignedIn: Boolean,
    gpsActive: Boolean,
  ): List<DiagnosticCheckResult> =
    withContext(Dispatchers.IO) {
      val results = mutableListOf<DiagnosticCheckResult>()

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

      results +=
        timedHttpPing(
          name = "YouTube Live Channel Feed",
          category = "MEDIA",
          configured = true,
          missingMsg = "",
          url = "https://www.youtube.com/feeds/videos.xml?channel_id=${LiveCloudServices.OFFICIAL_CHANNEL_ID}",
        )

      results +=
        timedHttpPing(
          name = "Open-Meteo Weather",
          category = "WEATHER",
          configured = true,
          missingMsg = "",
          url = "https://api.open-meteo.com/v1/forecast?latitude=37.77&longitude=-122.41&current=temperature_2m",
        )

      results +=
        timedHttpPing(
          name = "Doppler Storm Radar (RainViewer)",
          category = "WEATHER",
          configured = true,
          missingMsg = "",
          url = "https://api.rainviewer.com/public/weather-maps.json",
        )

      results +=
        timedHttpPing(
          name = "OpenStreetMap Geocoding",
          category = "MAPS",
          configured = true,
          missingMsg = "",
          url = "https://nominatim.openstreetmap.org/status.php?format=json",
        )

      results +=
        timedHttpPing(
          name = "Overpass OSM Roads & Chargers",
          category = "ROADS",
          configured = true,
          missingMsg = "",
          url = "https://overpass-api.de/api/status",
        )

      results +=
        timedHttpPing(
          name = "OSRM PEV Navigation",
          category = "NAVIGATION",
          configured = true,
          missingMsg = "",
          url = "https://router.project-osrm.org/nearest/v1/driving/-122.4194,37.7749",
        )

      results +=
        timedHttpPing(
          name = "Open-Elevation Terrain API",
          category = "ELEVATION",
          configured = true,
          missingMsg = "",
          url = "https://api.open-elevation.com/api/v1/lookup?locations=37.7749,-122.4194",
        )

      results +=
        timedHttpPing(
          name = "PEV News Feed (Electrek RSS)",
          category = "NEWS",
          configured = true,
          missingMsg = "",
          url = "https://electrek.co/feed/",
        )

      val (appCheckOk, appCheckMs, appCheckDetail) = AppCheckHealthManager.probeLiveAppCheck(context)
      results +=
        DiagnosticCheckResult(
          name = "Firebase AI Logic & App Check",
          category = "AI",
          ok = appCheckOk,
          latencyMs = appCheckMs,
          detail = "$appCheckDetail ($firebaseProjectId)",
        )

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
