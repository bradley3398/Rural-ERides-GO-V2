package com.example.core

import android.util.Log
import com.example.data.BoardComment
import com.example.data.BoardPost
import com.example.data.CallSession
import com.example.data.CallSignal
import com.example.data.ChannelPresence
import com.example.data.ChannelSpeakerState
import com.example.data.ChannelWebRtcSignal
import com.example.data.DmMessage
import com.example.data.DmThread
import com.example.data.PilotAuthenticityGuard
import com.example.data.PublicPilotProfile
import com.example.data.RadarHazard
import com.example.data.RadarMeetup
import com.example.data.RadarRider
import com.example.data.RadarWaypoint
import com.example.data.SupabaseAuthService
import com.example.data.WalkieVoiceLog
import com.example.data.YoutubeComment
import com.google.firebase.Timestamp
import java.net.URLEncoder
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

internal object SupabaseCloudSyncClient {
  private const val TAG = "SupabaseCloudSync"

  private val supabaseRestClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
      .retryOnConnectionFailure(true)
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .build()
  }

  private fun supabaseRestBaseUrl(): String =
    SupabaseAuthService.sanitizeBaseUrl(com.example.BuildConfig.SUPABASE_URL)

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
    val key = SupabaseAuthService.sanitizeAnonKey(com.example.BuildConfig.SUPABASE_ANON_KEY)
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
      delay(500L * (attempt + 1))
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
    PilotAuthenticityGuard.isAuthenticCallsignAndUid(
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
    if (!LiveCloudServices.hasSupabaseConfig()) error("Supabase not configured")
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
    if (!LiveCloudServices.hasSupabaseConfig()) error("Supabase not configured")
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
        if (!LiveCloudServices.hasSupabaseConfig()) error("Supabase not configured")
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
            PilotAuthenticityGuard.isDisplayablePilotProfile(
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
        val cleanCs = pilot.callsign.trim().take(32)
        if (cleanCs.isBlank() || com.example.data.PilotAuthenticityGuard.isAutoGeneratedBotCallsign(cleanCs)) {
          error("Real pilot callsign required")
        }
        val payload =
          JSONObject().apply {
            put("user_id", uid)
            put("callsign", cleanCs)
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
}
