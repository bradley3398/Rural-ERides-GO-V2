package com.example.core

import android.content.Context
import com.example.BuildConfig
import com.example.data.ActiveNavRoute
import com.example.data.BoardComment
import com.example.data.BoardPost
import com.example.data.CallSession
import com.example.data.CallSignal
import com.example.data.ChannelPresence
import com.example.data.ChannelSpeakerState
import com.example.data.ChannelWebRtcSignal
import com.example.data.DmMessage
import com.example.data.DmThread
import com.example.data.LiveChargingStation
import com.example.data.NavRouteProfile
import com.example.data.NewsArticleItem
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
import com.google.firebase.Timestamp
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

data class DiagnosticCheckResult(
  val name: String,
  val category: String,
  val ok: Boolean,
  val latencyMs: Long,
  val detail: String,
)

/**
 * Facade delegating live HTTP integrations to focused modular clients inside `com.example.core`:
 * - [WeatherApiClient]
 * - [MapsSpeedLimitClient]
 * - [YouTubeApiClient]
 * - [SupabaseCloudSyncClient]
 * - [NewsAndSearchClient]
 * - [ProductAndStoreClient]
 */
object LiveCloudServices {
  const val OFFICIAL_CHANNEL_ID = "UCJloGuR4YwCVIcAh3gjTbTg"

  internal val httpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(12, TimeUnit.SECONDS)
      .readTimeout(18, TimeUnit.SECONDS)
      .build()
  }

  fun hasGeminiKey(): Boolean = ApiKeyRing.geminiKeys().isNotEmpty()
  fun hasYoutubeKey(): Boolean = ApiKeyRing.youtubeKeys().isNotEmpty()
  fun hasSupabaseConfig(): Boolean =
    SupabaseAuthService.isConfiguredValues(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY)
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
  // 1. WEATHER & ELEVATION
  // --------------------------------------------------------------------------
  internal fun parseWeatherLocationsJson(raw: String): List<WeatherGeoLocation> =
    WeatherApiClient.parseWeatherLocationsJson(raw)

  suspend fun searchWeatherLocations(query: String): Result<List<WeatherGeoLocation>> =
    WeatherApiClient.searchWeatherLocations(query)

  suspend fun reverseGeocodeCityLabel(lat: Double, lng: Double): String =
    WeatherApiClient.reverseGeocodeCityLabel(lat, lng)

  suspend fun fetchWeatherMatrix(
    lat: Double,
    lng: Double,
    explicitLabel: String = "",
    isGpsSource: Boolean = true,
  ): Result<WeatherMatrixData> =
    WeatherApiClient.fetchWeatherMatrix(lat, lng, explicitLabel, isGpsSource)

  suspend fun fetchElevationSparkline(points: List<Pair<Double, Double>>): List<Double> =
    WeatherApiClient.fetchElevationSparkline(points)

  // --------------------------------------------------------------------------
  // 2. MAPS, SPEED LIMITS, ROUTING & CHARGERS
  // --------------------------------------------------------------------------
  data class RoadSignInfo(
    val roadName: String,
    val speedLimitMph: Int?,
    val inSchoolZone: Boolean,
    val limitSource: String = "",
  )

  internal fun parseRoadSpeedLimitMph(rawTag: String, defaultAsKmh: Boolean = true): Int? =
    MapsSpeedLimitClient.parseRoadSpeedLimitMph(rawTag, defaultAsKmh)

  suspend fun fetchLiveRoadAndSpeedLimit(lat: Double, lng: Double): Result<RoadSignInfo> =
    MapsSpeedLimitClient.fetchLiveRoadAndSpeedLimit(lat, lng)

  suspend fun searchNavDestinations(
    query: String,
    nearLat: Double = 0.0,
    nearLng: Double = 0.0,
  ): Result<List<WeatherGeoLocation>> =
    MapsSpeedLimitClient.searchNavDestinations(query, nearLat, nearLng)

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
    MapsSpeedLimitClient.fetchTurnByTurnRoute(
      startLat = startLat,
      startLng = startLng,
      endLat = endLat,
      endLng = endLng,
      destinationName = destinationName,
      profile = profile,
      avoidHighways = avoidHighways,
      vehicleWh = vehicleWh,
    )

  suspend fun fetchLiveChargingStations(
    lat: Double,
    lng: Double,
    radiusKm: Int = 15,
  ): Result<List<LiveChargingStation>> =
    MapsSpeedLimitClient.fetchLiveChargingStations(lat, lng, radiusKm)

  // --------------------------------------------------------------------------
  // 3. YOUTUBE & TRAIL TV
  // --------------------------------------------------------------------------
  suspend fun fetchYouTubeVideos(
    query: String = "",
    channelOnly: Boolean = true,
  ): Result<Pair<List<YouTubeVideoItem>, String>> =
    YouTubeApiClient.fetchYouTubeVideos(query, channelOnly)

  suspend fun fetchYouTubeMusicMixes(
    category: String = "Synthwave",
    customQuery: String = "",
  ): Result<Pair<List<YouTubeVideoItem>, String>> =
    YouTubeApiClient.fetchYouTubeMusicMixes(category, customQuery)

  // --------------------------------------------------------------------------
  // 4. OPTIONAL SUPABASE POSTGREST CLOUD SYNC
  // --------------------------------------------------------------------------
  internal fun toAppSbUid(raw: String): String =
    SupabaseCloudSyncClient.toAppSbUid(raw)

  internal fun parseSupabaseTimestamp(iso: String): Timestamp? =
    SupabaseCloudSyncClient.parseSupabaseTimestamp(iso)

  internal fun isAuthenticSupabasePilot(uid: String, callsign: String, selfUid: String = ""): Boolean =
    SupabaseCloudSyncClient.isAuthenticSupabasePilot(uid, callsign, selfUid)

  suspend fun fetchSupabaseBoardPosts(accessToken: String): Result<List<BoardPost>> =
    SupabaseCloudSyncClient.fetchSupabaseBoardPosts(accessToken)

  suspend fun insertSupabaseBoardPost(accessToken: String, post: BoardPost): Result<Unit> =
    SupabaseCloudSyncClient.insertSupabaseBoardPost(accessToken, post)

  suspend fun updateSupabaseBoardPost(accessToken: String, postId: String, title: String, body: String): Result<Unit> =
    SupabaseCloudSyncClient.updateSupabaseBoardPost(accessToken, postId, title, body)

  suspend fun updateSupabaseBoardLikes(accessToken: String, postId: String, nextLikes: Long): Result<Unit> =
    SupabaseCloudSyncClient.updateSupabaseBoardLikes(accessToken, postId, nextLikes)

  suspend fun voteSupabaseBoardPoll(accessToken: String, postId: String, updatedOptions: List<String>): Result<Unit> =
    SupabaseCloudSyncClient.voteSupabaseBoardPoll(accessToken, postId, updatedOptions)

  suspend fun flagSupabaseBoardPost(accessToken: String, postId: String, nextFlagCount: Long): Result<Unit> =
    SupabaseCloudSyncClient.flagSupabaseBoardPost(accessToken, postId, nextFlagCount)

  suspend fun fetchSupabaseBoardComments(accessToken: String, postId: String): Result<List<BoardComment>> =
    SupabaseCloudSyncClient.fetchSupabaseBoardComments(accessToken, postId)

  suspend fun insertSupabaseBoardComment(
    accessToken: String,
    postId: String,
    userId: String,
    callsign: String,
    body: String,
  ): Result<Unit> =
    SupabaseCloudSyncClient.insertSupabaseBoardComment(accessToken, postId, userId, callsign, body)

  suspend fun deleteSupabaseBoardPost(accessToken: String, postId: String): Result<Unit> =
    SupabaseCloudSyncClient.deleteSupabaseBoardPost(accessToken, postId)

  suspend fun fetchSupabasePublicPilots(accessToken: String, selfUid: String = ""): Result<List<PublicPilotProfile>> =
    SupabaseCloudSyncClient.fetchSupabasePublicPilots(accessToken, selfUid)

  suspend fun upsertSupabasePilotProfile(accessToken: String, pilot: PublicPilotProfile): Result<Unit> =
    SupabaseCloudSyncClient.upsertSupabasePilotProfile(accessToken, pilot)

  suspend fun fetchSupabaseRadarRiders(accessToken: String, selfUid: String = ""): Result<List<RadarRider>> =
    SupabaseCloudSyncClient.fetchSupabaseRadarRiders(accessToken, selfUid)

  suspend fun upsertSupabaseRadarRider(accessToken: String, rider: RadarRider): Result<Unit> =
    SupabaseCloudSyncClient.upsertSupabaseRadarRider(accessToken, rider)

  suspend fun fetchSupabaseRadarMeetups(accessToken: String): Result<List<RadarMeetup>> =
    SupabaseCloudSyncClient.fetchSupabaseRadarMeetups(accessToken)

  suspend fun insertSupabaseRadarMeetup(accessToken: String, meetup: RadarMeetup): Result<Unit> =
    SupabaseCloudSyncClient.insertSupabaseRadarMeetup(accessToken, meetup)

  suspend fun rsvpSupabaseRadarMeetup(accessToken: String, meetupId: String, nextCount: Long): Result<Unit> =
    SupabaseCloudSyncClient.rsvpSupabaseRadarMeetup(accessToken, meetupId, nextCount)

  suspend fun fetchSupabaseRadarHazards(accessToken: String): Result<List<RadarHazard>> =
    SupabaseCloudSyncClient.fetchSupabaseRadarHazards(accessToken)

  suspend fun insertSupabaseRadarHazard(accessToken: String, hazard: RadarHazard): Result<Unit> =
    SupabaseCloudSyncClient.insertSupabaseRadarHazard(accessToken, hazard)

  suspend fun upvoteSupabaseRadarHazard(accessToken: String, hazardId: String, nextUpvotes: Long): Result<Unit> =
    SupabaseCloudSyncClient.upvoteSupabaseRadarHazard(accessToken, hazardId, nextUpvotes)

  suspend fun fetchSupabaseRadarWaypoints(accessToken: String): Result<List<RadarWaypoint>> =
    SupabaseCloudSyncClient.fetchSupabaseRadarWaypoints(accessToken)

  suspend fun insertSupabaseRadarWaypoint(accessToken: String, wp: RadarWaypoint): Result<Unit> =
    SupabaseCloudSyncClient.insertSupabaseRadarWaypoint(accessToken, wp)

  suspend fun fetchSupabaseDmThreads(accessToken: String, myUid: String): Result<List<DmThread>> =
    SupabaseCloudSyncClient.fetchSupabaseDmThreads(accessToken, myUid)

  suspend fun fetchSupabaseDmMessages(accessToken: String, threadId: String): Result<List<DmMessage>> =
    SupabaseCloudSyncClient.fetchSupabaseDmMessages(accessToken, threadId)

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
    SupabaseCloudSyncClient.sendSupabaseDm(
      accessToken,
      myUid,
      myCallsign,
      peerUid,
      peerCallsign,
      text,
      imageUrl,
      voiceBase64,
      selfDestructSec,
    )

  suspend fun deleteSupabaseDmMessage(accessToken: String, msgId: String): Result<Unit> =
    SupabaseCloudSyncClient.deleteSupabaseDmMessage(accessToken, msgId)

  suspend fun fetchSupabaseWalkiePresence(accessToken: String, channelId: String): Result<List<ChannelPresence>> =
    SupabaseCloudSyncClient.fetchSupabaseWalkiePresence(accessToken, channelId)

  suspend fun upsertSupabaseWalkiePresence(
    accessToken: String,
    channelId: String,
    uid: String,
    callsign: String,
    active: Boolean,
  ): Result<Unit> =
    SupabaseCloudSyncClient.upsertSupabaseWalkiePresence(accessToken, channelId, uid, callsign, active)

  suspend fun fetchSupabaseWalkieSpeaker(accessToken: String, channelId: String): Result<ChannelSpeakerState?> =
    SupabaseCloudSyncClient.fetchSupabaseWalkieSpeaker(accessToken, channelId)

  suspend fun upsertSupabaseWalkieSpeaker(
    accessToken: String,
    channelId: String,
    uid: String,
    callsign: String,
    speaking: Boolean,
  ): Result<Unit> =
    SupabaseCloudSyncClient.upsertSupabaseWalkieSpeaker(accessToken, channelId, uid, callsign, speaking)

  suspend fun fetchSupabaseWalkieLogs(accessToken: String, channelId: String): Result<List<WalkieVoiceLog>> =
    SupabaseCloudSyncClient.fetchSupabaseWalkieLogs(accessToken, channelId)

  suspend fun insertSupabaseWalkieLog(accessToken: String, log: WalkieVoiceLog): Result<Unit> =
    SupabaseCloudSyncClient.insertSupabaseWalkieLog(accessToken, log)

  suspend fun fetchSupabaseWalkieSignals(accessToken: String, channelId: String): Result<List<ChannelWebRtcSignal>> =
    SupabaseCloudSyncClient.fetchSupabaseWalkieSignals(accessToken, channelId)

  suspend fun insertSupabaseWalkieSignal(accessToken: String, sig: ChannelWebRtcSignal): Result<Unit> =
    SupabaseCloudSyncClient.insertSupabaseWalkieSignal(accessToken, sig)

  suspend fun clearSupabaseWalkieSignals(accessToken: String, channelId: String, myUid: String): Result<Unit> =
    SupabaseCloudSyncClient.clearSupabaseWalkieSignals(accessToken, channelId, myUid)

  suspend fun fetchSupabaseCallSessions(accessToken: String, myUid: String): Result<List<CallSession>> =
    SupabaseCloudSyncClient.fetchSupabaseCallSessions(accessToken, myUid)

  suspend fun insertSupabaseCallSession(accessToken: String, session: CallSession): Result<Unit> =
    SupabaseCloudSyncClient.insertSupabaseCallSession(accessToken, session)

  suspend fun updateSupabaseCallStatus(accessToken: String, sessionId: String, nextStatus: String): Result<Unit> =
    SupabaseCloudSyncClient.updateSupabaseCallStatus(accessToken, sessionId, nextStatus)

  suspend fun updateSupabaseCallType(accessToken: String, sessionId: String, nextType: String): Result<Unit> =
    SupabaseCloudSyncClient.updateSupabaseCallType(accessToken, sessionId, nextType)

  suspend fun fetchSupabaseCallSignals(accessToken: String, sessionId: String): Result<List<CallSignal>> =
    SupabaseCloudSyncClient.fetchSupabaseCallSignals(accessToken, sessionId)

  suspend fun insertSupabaseCallSignal(accessToken: String, sessionId: String, sig: CallSignal): Result<Unit> =
    SupabaseCloudSyncClient.insertSupabaseCallSignal(accessToken, sessionId, sig)

  suspend fun fetchSupabaseYoutubeEngagement(accessToken: String, videoId: String): Result<Long> =
    SupabaseCloudSyncClient.fetchSupabaseYoutubeEngagement(accessToken, videoId)

  suspend fun voltSupabaseYoutubeVideo(accessToken: String, videoId: String): Result<Long> =
    SupabaseCloudSyncClient.voltSupabaseYoutubeVideo(accessToken, videoId)

  suspend fun fetchSupabaseYoutubeComments(accessToken: String, videoId: String): Result<List<YoutubeComment>> =
    SupabaseCloudSyncClient.fetchSupabaseYoutubeComments(accessToken, videoId)

  suspend fun insertSupabaseYoutubeComment(
    accessToken: String,
    videoId: String,
    userId: String,
    callsign: String,
    text: String,
  ): Result<Unit> =
    SupabaseCloudSyncClient.insertSupabaseYoutubeComment(accessToken, videoId, userId, callsign, text)

  // --------------------------------------------------------------------------
  // 5. PEV NEWS, WEB GROUNDING, IMAGE UPLOAD & DIAGNOSTICS
  // --------------------------------------------------------------------------
  internal fun isStrictlyPevArticle(title: String, summary: String): Boolean =
    NewsAndSearchClient.isStrictlyPevArticle(title, summary)

  suspend fun fetchLivePevNews(
    topic: String = "",
    category: String = "ALL PEV",
    locationLabel: String = "",
  ): Result<List<NewsArticleItem>> =
    NewsAndSearchClient.fetchLivePevNews(topic, category, locationLabel)

  suspend fun webSearchGrounding(query: String, deepMode: Boolean = false): String =
    NewsAndSearchClient.webSearchGrounding(query, deepMode)

  suspend fun uploadImageToImgbb(base64Image: String): Result<String> =
    NewsAndSearchClient.uploadImageToImgbb(base64Image)

  suspend fun runLiveConnectionDiagnostics(
    context: Context,
    supabaseService: SupabaseAuthService,
    firebaseSignedIn: Boolean,
    gpsActive: Boolean,
  ): List<DiagnosticCheckResult> =
    NewsAndSearchClient.runLiveConnectionDiagnostics(context, supabaseService, firebaseSignedIn, gpsActive)

  // --------------------------------------------------------------------------
  // 6. PRODUCT IMAGES, BRAND LOGOS & VERIFIED IN-STOCK STORES
  // --------------------------------------------------------------------------
  data class VerifiedStoreOffer(
    val storeId: String,
    val storeName: String,
    val url: String,
    val priceHint: String = "",
    val stockBadge: String = "In Stock Verified",
  )

  suspend fun fetchPevProductImageBase64(makeAndModel: String): String =
    ProductAndStoreClient.fetchPevProductImageBase64(makeAndModel)

  fun resolvePevBrandInfo(brandOrModel: String): Pair<String, String> =
    ProductAndStoreClient.resolvePevBrandInfo(brandOrModel)

  fun cachedPevBrandLogo(brandOrModel: String): String =
    ProductAndStoreClient.cachedPevBrandLogo(brandOrModel)

  suspend fun fetchPevBrandLogoBase64(brandOrModel: String): String =
    ProductAndStoreClient.fetchPevBrandLogoBase64(brandOrModel)

  suspend fun fetchLiveProductImages(query: String, limit: Int = 5): List<String> =
    ProductAndStoreClient.fetchLiveProductImages(query, limit)

  internal fun extractStoreProductTokens(productTitle: String): List<String> =
    ProductAndStoreClient.extractStoreProductTokens(productTitle)

  internal fun scoreProductTitleMatch(candidateTitleLower: String, targetTitle: String, tokens: List<String>): Int =
    ProductAndStoreClient.scoreProductTitleMatch(candidateTitleLower, targetTitle, tokens)

  internal fun extractDirectProductUrlFromHtml(
    html: String,
    baseUrl: String,
    targetTitle: String,
    tokens: List<String>,
  ): String? =
    ProductAndStoreClient.extractDirectProductUrlFromHtml(html, baseUrl, targetTitle, tokens)

  internal fun extractCleanPriceHint(rawHtml: String): String =
    ProductAndStoreClient.extractCleanPriceHint(rawHtml)

  suspend fun findVerifiedInStockStores(
    productTitle: String,
    candidateStores: List<Pair<String, Pair<String, String>>>,
  ): List<VerifiedStoreOffer> =
    ProductAndStoreClient.findVerifiedInStockStores(productTitle, candidateStores)
}
