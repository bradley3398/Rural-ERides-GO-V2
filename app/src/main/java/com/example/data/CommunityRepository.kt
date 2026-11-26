package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CommunityRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUid(): String =
    auth.currentUser?.uid ?: error("User must sign in with Google first.")

  private fun isAuthenticPilot(
    userId: String,
    callsign: String,
    avatarSource: String = "",
    docId: String? = null,
    requireFirebaseUid: Boolean = false,
  ): Boolean =
    PilotAuthenticityGuard.isAuthenticCallsignAndUid(
      userId = userId,
      callsign = callsign,
      avatarSource = avatarSource,
      docId = docId,
      requireFirebaseUid = requireFirebaseUid,
      currentUid = auth.currentUser?.uid,
    )

  // --------------------------------------------------------------------------
  // PUBLIC PILOT PROFILES & STANDINGS (100% REAL AUTHENTICATED RIDERS ONLY)
  // --------------------------------------------------------------------------
  fun observePublicPilots(): Flow<List<PublicPilotProfile>> =
    combine(
      db.collection("user_profiles").whereEqualTo("visibility", "PUBLIC").snapshots(),
      db.collection("radar_riders").whereEqualTo("visibility", "PUBLIC").snapshots(),
    ) { profileSnap, radarSnap ->
      val currentUid = auth.currentUser?.uid
      val nowSec = System.currentTimeMillis() / 1000L
      val fromProfiles =
        profileSnap
          .toObjects(
            PublicPilotProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          .filter { p ->
            PilotAuthenticityGuard.isDisplayablePilotProfile(
              p = p,
              currentUid = currentUid,
              requireFirebaseUid = false,
              maxStaleDays = 90L,
              nowSec = nowSec,
            )
          }

      val nowMs = System.currentTimeMillis()
      val radarList =
        radarSnap
          .toObjects(
            RadarRider::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          .filter { r ->
            r.userId.isNotBlank() &&
              isAuthenticPilot(
                userId = r.userId,
                callsign = r.callsign,
                docId = r.id,
                requireFirebaseUid = false,
              )
          }
      val radarByUid = radarList.associateBy { it.userId }
      val mergedProfiles =
        fromProfiles.map { p ->
          val r = radarByUid[p.userId]
          val radarFresh =
            r != null &&
              !r.statusTag.equals("OFFLINE", ignoreCase = true) &&
              ((r.updatedAt?.seconds ?: 0L) > 0L && (nowSec - (r.updatedAt?.seconds ?: 0L)) <= 360L)
          val effectiveOnline = p.isLiveOnline(nowMs) || radarFresh || (currentUid != null && p.userId == currentUid)
          val effectiveSeenMs =
            maxOf(
              p.lastSeenMs,
              (p.updatedAt?.seconds ?: 0L) * 1000L,
              (r?.updatedAt?.seconds ?: 0L) * 1000L,
            )
          p.copy(online = effectiveOnline, lastSeenMs = effectiveSeenMs)
        }

      val existingUids = mergedProfiles.map { it.userId }.toSet()
      // Only surface a radar-only beacon if it is actively pinging RIGHT NOW (<= 5 min) or is self;
      // never resurrect stale orphaned or Pre-Launch test radar_riders docs that lack a real user_profiles record.
      val fromRadar =
        radarList
          .filter { r ->
            if (r.userId in existingUids) return@filter false
            val isSelf = currentUid != null && r.userId == currentUid
            val updatedSec = r.updatedAt?.seconds ?: 0L
            val activelyPingingNow =
              !r.statusTag.equals("OFFLINE", ignoreCase = true) &&
                updatedSec > 0L &&
                (nowSec - updatedSec) <= 300L
            isSelf || activelyPingingNow
          }
          .map { r ->
            val updatedSec = r.updatedAt?.seconds ?: 0L
            val radarFresh =
              !r.statusTag.equals("OFFLINE", ignoreCase = true) &&
                updatedSec > 0L &&
                (nowSec - updatedSec) <= 300L
            PublicPilotProfile(
              id = r.userId,
              userId = r.userId,
              callsign = r.callsign.trim(),
              rankBadge = "Live Radar Pilot",
              xpPoints = 25L,
              totalKm = 0.0,
              ridesCount = 0L,
              verified = false,
              banned = false,
              trustScore = 100L,
              vehicleSummary = r.vehicleName.ifBlank { "PEV Rig" },
              visibility = "PUBLIC",
              online = radarFresh,
              lastSeenMs = updatedSec * 1000L,
              createdAt = r.createdAt,
              updatedAt = r.updatedAt,
            )
          }

      (mergedProfiles + fromRadar)
        .distinctBy { it.userId }
        .sortedWith(
          compareByDescending<PublicPilotProfile> { it.online }
            .thenByDescending { it.lastSeenMs }
            .thenByDescending { it.xpPoints }
        )
    }

  // --------------------------------------------------------------------------
  // COMMUNITY BOARD POSTS, COMMENTS & VOLTS (100% REAL RIDERS ONLY)
  // --------------------------------------------------------------------------
  fun observeBoardPosts(): Flow<List<BoardPost>> =
    db.collection("board_posts").whereEqualTo("visibility", "PUBLIC").snapshots().map { snap ->
      snap
        .toObjects(BoardPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .filter { p -> isAuthenticPilot(userId = p.userId, callsign = p.authorCallsign) }
        .sortedByDescending { it.createdAt?.seconds ?: 0L }
    }

  suspend fun fetchBoardPostsFromServer(): Result<List<BoardPost>> =
    try {
      requireUid()
      val snap =
        db.collection("board_posts")
          .whereEqualTo("visibility", "PUBLIC")
          .get(com.google.firebase.firestore.Source.SERVER)
          .await()
      val posts =
        snap
          .toObjects(BoardPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .filter { p -> isAuthenticPilot(userId = p.userId, callsign = p.authorCallsign) }
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      Result.success(posts)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, "board_posts")
      Result.failure(e)
    }

  fun observePostComments(postId: String): Flow<List<BoardComment>> =
    db.collection("board_posts")
      .document(postId)
      .collection("comments")
      .whereEqualTo("visibility", "PUBLIC")
      .snapshots()
      .map { snap ->
        snap
          .toObjects(BoardComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedBy { it.createdAt?.seconds ?: 0L }
      }

  suspend fun createBoardPost(
    authorCallsign: String,
    authorBadge: String,
    category: String,
    title: String,
    body: String,
    imageUrl: String = "",
    pollQuestion: String = "",
    pollOptions: List<String> = emptyList(),
    priceUsd: Double = 0.0,
    locationLabel: String = "",
    telemetrySummary: String = "",
    helpNeeded: Boolean = false,
    speedTrap: Boolean = false,
    stolenAlert: Boolean = false,
  ): Result<String> =
    try {
      val uid = requireUid()
      val postId = "post_${UUID.randomUUID().toString().take(10)}"
      val validCat =
        if (
          category in
            listOf(
              "UNIVERSAL",
              "CREW",
              "GROUP_RIDES",
              "MARKETPLACE",
              "CONFESSIONS",
              "GARAGE",
              "SOS",
              "STOLEN",
              "VIDEO_REQUEST",
              "EVENTS",
              "BUILDS",
              "RIDE_REPORTS",
              "CHARGERS",
              "REVIEWS",
              "ALERTS",
            )
        ) {
          category
        } else "UNIVERSAL"

      db.collection("board_posts")
        .document(postId)
        .set(
          mapOf(
            "userId" to uid,
            "authorCallsign" to authorCallsign.take(32).ifBlank { "PILOT" },
            "authorBadge" to authorBadge.take(40).ifBlank { "Rookie Pilot" },
            "category" to validCat,
            "title" to title.trim().take(120).ifBlank { "Transmission" },
            "body" to body.trim().take(3000),
            "imageUrl" to imageUrl.take(175_000),
            "pollQuestion" to pollQuestion.trim().take(160),
            "pollOptions" to pollOptions.take(6),
            "priceUsd" to priceUsd.coerceIn(0.0, 100000.0),
            "locationLabel" to locationLabel.trim().take(100),
            "telemetrySummary" to telemetrySummary.trim().take(200),
            "helpNeeded" to helpNeeded,
            "speedTrap" to speedTrap,
            "stolenAlert" to stolenAlert,
            "likesCount" to 0L,
            "commentsCount" to 0L,
            "flaggedCount" to 0L,
            "visibility" to "PUBLIC",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(postId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "board_posts")
      Result.failure(e)
    }

  suspend fun toggleLikePost(postId: String): Result<Unit> =
    try {
      val uid = requireUid()
      val postRef = db.collection("board_posts").document(postId)
      val likeRef = postRef.collection("likes").document(uid)
      val existingLike = likeRef.get().await()
      if (existingLike.exists()) {
        likeRef.delete().await()
        postRef
          .update(
            mapOf(
              "likesCount" to FieldValue.increment(-1L),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        likeRef
          .set(mapOf("userId" to uid, "createdAt" to FieldValue.serverTimestamp()))
          .await()
        postRef
          .update(
            mapOf(
              "likesCount" to FieldValue.increment(1L),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "board_posts/$postId/likes")
      Result.failure(e)
    }

  suspend fun votePollOption(postId: String, optionIndex: Int, currentOptions: List<String>): Result<Unit> =
    try {
      requireUid()
      val updated =
        currentOptions.mapIndexed { idx, raw ->
          val parts = raw.split("|")
          val label = parts.firstOrNull() ?: raw
          val votes = parts.getOrNull(1)?.toIntOrNull() ?: 0
          val nextVotes = if (idx == optionIndex) votes + 1 else votes
          "$label|$nextVotes"
        }
      db.collection("board_posts")
        .document(postId)
        .update(mapOf("pollOptions" to updated, "updatedAt" to FieldValue.serverTimestamp()))
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "board_posts/$postId")
      Result.failure(e)
    }

  suspend fun addPostComment(postId: String, authorCallsign: String, body: String): Result<Unit> =
    try {
      val uid = requireUid()
      val cid = "cmt_${UUID.randomUUID().toString().take(8)}"
      val postRef = db.collection("board_posts").document(postId)
      postRef
        .collection("comments")
        .document(cid)
        .set(
          mapOf(
            "userId" to uid,
            "authorCallsign" to authorCallsign.take(32).ifBlank { "PILOT" },
            "body" to body.trim().take(1000),
            "visibility" to "PUBLIC",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      postRef
        .update(
          mapOf(
            "commentsCount" to FieldValue.increment(1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "board_posts/$postId/comments")
      Result.failure(e)
    }

  suspend fun flagPost(postId: String): Result<Unit> =
    try {
      requireUid()
      db.collection("board_posts")
        .document(postId)
        .update(
          mapOf(
            "flaggedCount" to FieldValue.increment(1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "board_posts/$postId")
      Result.failure(e)
    }

  suspend fun updateBoardPost(postId: String, title: String, body: String): Result<Unit> =
    try {
      requireUid()
      db.collection("board_posts")
        .document(postId)
        .update(
          mapOf(
            "title" to title.trim().take(120).ifBlank { "Transmission" },
            "body" to body.trim().take(3000),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "board_posts/$postId")
      Result.failure(e)
    }

  suspend fun deletePost(postId: String): Result<Unit> =
    try {
      requireUid()
      db.collection("board_posts").document(postId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, "board_posts/$postId")
      Result.failure(e)
    }

  // --------------------------------------------------------------------------
  // RIDER RADAR BEACONS, MEETUPS, HAZARDS & WAYPOINTS
  // --------------------------------------------------------------------------
  fun observeRadarRiders(): Flow<List<RadarRider>> =
    db.collection("radar_riders").whereEqualTo("visibility", "PUBLIC").snapshots().map { snap ->
      val currentUid = auth.currentUser?.uid
      val nowSec = System.currentTimeMillis() / 1000L
      val freshnessWindowSec = 3600L // Active in the last 1 hour (or self)
      snap
        .toObjects(RadarRider::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .filter { r ->
          val isSelf = currentUid != null && r.userId == currentUid
          val updatedSec = r.updatedAt?.seconds ?: r.createdAt?.seconds ?: 0L
          val isFresh = isSelf || (updatedSec > 0L && (nowSec - updatedSec) <= freshnessWindowSec)
          val isActiveStatus = !r.statusTag.equals("OFFLINE", ignoreCase = true)
          isFresh &&
            isActiveStatus &&
            isAuthenticPilot(
              userId = r.userId,
              callsign = r.callsign,
              docId = r.id,
              requireFirebaseUid = false,
            )
        }
        .distinctBy { it.userId }
    }

  fun observeRadarMeetups(): Flow<List<RadarMeetup>> =
    db.collection("radar_meetups").whereEqualTo("visibility", "PUBLIC").snapshots().map { snap ->
      snap
        .toObjects(RadarMeetup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .filter { m -> isAuthenticPilot(userId = m.userId, callsign = m.creatorCallsign) }
    }

  fun observeRadarHazards(): Flow<List<RadarHazard>> =
    db.collection("radar_hazards").whereEqualTo("visibility", "PUBLIC").snapshots().map { snap ->
      snap.toObjects(RadarHazard::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    }

  fun observeRadarWaypoints(): Flow<List<RadarWaypoint>> =
    db.collection("radar_waypoints").whereEqualTo("visibility", "PUBLIC").snapshots().map { snap ->
      snap.toObjects(RadarWaypoint::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    }

  suspend fun publishMyRadarBeacon(
    callsign: String,
    vehicleName: String,
    lat: Double,
    lng: Double,
    speedKmh: Double,
    headingDeg: Double,
    batteryPct: Int,
    statusTag: String,
    visible: Boolean,
  ): Result<Unit> =
    try {
      val uid = requireUid()
      val ref = db.collection("radar_riders").document(uid)
      val snap = ref.get().await()
      val cleanStatus =
        if (statusTag in listOf("CRUISING", "CHARGING", "SOS", "ASSEMBLE", "OFFLINE")) statusTag
        else "CRUISING"
      val vis = if (visible) "PUBLIC" else "HIDDEN"
      val fbUser = auth.currentUser
      val cleanCs =
        PilotAuthenticityGuard.deriveHumanCallsign(
          preferredCallsign = callsign,
          displayName = fbUser?.displayName.orEmpty(),
          email = fbUser?.email.orEmpty(),
        )
      if (cleanCs.isBlank()) {
        return Result.failure(IllegalStateException("Set a real pilot callsign before broadcasting radar beacon."))
      }
      if (!snap.exists()) {
        ref
          .set(
            mapOf(
              "userId" to uid,
              "callsign" to cleanCs,
              "vehicleName" to vehicleName.take(60),
              "lat" to lat.coerceIn(-90.0, 90.0),
              "lng" to lng.coerceIn(-180.0, 180.0),
              "speedKmh" to speedKmh.coerceIn(0.0, 250.0),
              "headingDeg" to headingDeg.coerceIn(0.0, 360.0),
              "batteryPct" to batteryPct.coerceIn(0, 100).toLong(),
              "statusTag" to cleanStatus,
              "visibility" to vis,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        ref
          .update(
            mapOf(
              "callsign" to cleanCs,
              "vehicleName" to vehicleName.take(60),
              "lat" to lat.coerceIn(-90.0, 90.0),
              "lng" to lng.coerceIn(-180.0, 180.0),
              "speedKmh" to speedKmh.coerceIn(0.0, 250.0),
              "headingDeg" to headingDeg.coerceIn(0.0, 360.0),
              "batteryPct" to batteryPct.coerceIn(0, 100).toLong(),
              "statusTag" to cleanStatus,
              "visibility" to vis,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "radar_riders")
      Result.failure(e)
    }

  suspend fun createMeetup(
    callsign: String,
    title: String,
    description: String,
    scheduledTime: String,
    lat: Double,
    lng: Double,
  ): Result<Unit> =
    try {
      val uid = requireUid()
      val id = "meet_${UUID.randomUUID().toString().take(8)}"
      db.collection("radar_meetups")
        .document(id)
        .set(
          mapOf(
            "userId" to uid,
            "creatorCallsign" to callsign.take(32).ifBlank { "PILOT" },
            "title" to title.trim().take(80).ifBlank { "Group Ride" },
            "description" to description.trim().take(400),
            "scheduledTime" to scheduledTime.take(60),
            "lat" to lat.coerceIn(-90.0, 90.0),
            "lng" to lng.coerceIn(-180.0, 180.0),
            "attendeeCount" to 1L,
            "visibility" to "PUBLIC",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "radar_meetups")
      Result.failure(e)
    }

  suspend fun rsvpMeetup(meetupId: String): Result<Unit> =
    try {
      requireUid()
      db.collection("radar_meetups")
        .document(meetupId)
        .update(
          mapOf(
            "attendeeCount" to FieldValue.increment(1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "radar_meetups/$meetupId")
      Result.failure(e)
    }

  suspend fun createHazard(
    callsign: String,
    hazardType: String,
    description: String,
    lat: Double,
    lng: Double,
  ): Result<Unit> =
    try {
      val uid = requireUid()
      val id = "haz_${UUID.randomUUID().toString().take(8)}"
      val cleanType =
        if (hazardType in listOf("POTHOLE", "GRAVEL", "POLICE", "CLOSURE", "DOG", "FLOOD"))
          hazardType
        else "POTHOLE"
      db.collection("radar_hazards")
        .document(id)
        .set(
          mapOf(
            "userId" to uid,
            "creatorCallsign" to callsign.take(32).ifBlank { "PILOT" },
            "hazardType" to cleanType,
            "description" to description.trim().take(300),
            "lat" to lat.coerceIn(-90.0, 90.0),
            "lng" to lng.coerceIn(-180.0, 180.0),
            "upvotes" to 1L,
            "visibility" to "PUBLIC",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "radar_hazards")
      Result.failure(e)
    }

  suspend fun upvoteHazard(hazardId: String): Result<Unit> =
    try {
      requireUid()
      db.collection("radar_hazards")
        .document(hazardId)
        .update(
          mapOf(
            "upvotes" to FieldValue.increment(1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "radar_hazards/$hazardId")
      Result.failure(e)
    }

  suspend fun createWaypoint(
    callsign: String,
    waypointType: String,
    title: String,
    notes: String,
    lat: Double,
    lng: Double,
  ): Result<Unit> =
    try {
      val uid = requireUid()
      val id = "wp_${UUID.randomUUID().toString().take(8)}"
      val cleanType =
        if (waypointType in listOf("CHARGER", "REPAIR", "WATER", "SCENIC", "MEETSPOT"))
          waypointType
        else "CHARGER"
      db.collection("radar_waypoints")
        .document(id)
        .set(
          mapOf(
            "userId" to uid,
            "creatorCallsign" to callsign.take(32).ifBlank { "PILOT" },
            "waypointType" to cleanType,
            "title" to title.trim().take(80).ifBlank { "Trail Waypoint" },
            "notes" to notes.trim().take(300),
            "lat" to lat.coerceIn(-90.0, 90.0),
            "lng" to lng.coerceIn(-180.0, 180.0),
            "visibility" to "PUBLIC",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "radar_waypoints")
      Result.failure(e)
    }

  // --------------------------------------------------------------------------
  // P2P WALKIE-TALKIE TACTICAL RADIO CHANNELS
  // --------------------------------------------------------------------------
  fun observeChannelPresence(channelId: String): Flow<List<ChannelPresence>> =
    db.collection("channels")
      .document(channelId)
      .collection("presence")
      .whereEqualTo("active", true)
      .snapshots()
      .map { snap ->
        snap
          .toObjects(
            ChannelPresence::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          .filter { p ->
            isAuthenticPilot(
              userId = p.userId,
              callsign = p.callsign,
              docId = p.id,
              requireFirebaseUid = true,
            )
          }
          .distinctBy { it.userId }
      }

  fun observeChannelSpeaker(channelId: String): Flow<ChannelSpeakerState?> =
    db.collection("channels")
      .document(channelId)
      .collection("state")
      .document("currentSpeaker")
      .snapshots()
      .map { snap ->
        if (snap.exists()) {
          snap.toObject(
            ChannelSpeakerState::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        } else null
      }

  fun observeWalkieLogs(channelId: String): Flow<List<WalkieVoiceLog>> =
    db.collection("channels")
      .document(channelId)
      .collection("walkie")
      .whereEqualTo("active", true)
      .snapshots()
      .map { snap ->
        snap
          .toObjects(WalkieVoiceLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
          .take(20)
      }

  suspend fun joinChannelPresence(channelId: String, callsign: String, active: Boolean) {
    runCatching {
      val uid = requireUid()
      val ref = db.collection("channels").document(channelId).collection("presence").document(uid)
      if (!active) {
        ref.delete().await()
        return
      }
      val snap = ref.get().await()
      if (!snap.exists()) {
        ref
          .set(
            mapOf(
              "userId" to uid,
              "callsign" to callsign.take(32).ifBlank { "PILOT" },
              "channelId" to channelId,
              "active" to true,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        ref
          .update(
            mapOf(
              "callsign" to callsign.take(32).ifBlank { "PILOT" },
              "active" to true,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
    }
  }

  suspend fun setChannelSpeakerFloor(channelId: String, callsign: String, speaking: Boolean) {
    runCatching {
      val uid = requireUid()
      val ref =
        db.collection("channels").document(channelId).collection("state").document("currentSpeaker")
      val snap = ref.get().await()
      if (!snap.exists()) {
        ref
          .set(
            mapOf(
              "speakerUid" to uid,
              "speakerCallsign" to callsign.take(32).ifBlank { "PILOT" },
              "channelId" to channelId,
              "active" to speaking,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        ref
          .update(
            mapOf(
              "speakerUid" to uid,
              "speakerCallsign" to callsign.take(32).ifBlank { "PILOT" },
              "active" to speaking,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
    }
  }

  suspend fun sendWalkieVoiceLog(
    channelId: String,
    callsign: String,
    audioBase64: String,
    durationMs: Long,
  ): Result<Unit> =
    try {
      val uid = requireUid()
      val mid = "ptt_${UUID.randomUUID().toString().take(8)}"
      db.collection("channels")
        .document(channelId)
        .collection("walkie")
        .document(mid)
        .set(
          mapOf(
            "userId" to uid,
            "senderCallsign" to callsign.take(32).ifBlank { "PILOT" },
            "channelId" to channelId,
            "audioBase64" to audioBase64.take(340000),
            "durationMs" to durationMs.coerceIn(100L, 30000L),
            "active" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "channels/$channelId/walkie")
      Result.failure(e)
    }

  fun observeChannelWebRtcSignals(channelId: String): Flow<List<ChannelWebRtcSignal>> =
    db.collection("channels")
      .document(channelId)
      .collection("webrtc_signals")
      .whereEqualTo("active", true)
      .snapshots()
      .map { snap ->
        snap
          .toObjects(ChannelWebRtcSignal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedBy { it.createdAt?.seconds ?: 0L }
      }

  suspend fun sendChannelWebRtcSignal(
    channelId: String,
    callsign: String,
    targetUid: String,
    signalType: String,
    payload: String,
  ): Result<Unit> =
    try {
      val uid = requireUid()
      val sid = "rtc_${UUID.randomUUID().toString().take(10)}"
      val cleanType =
        if (signalType in listOf("JOIN", "OFFER", "ANSWER", "ICE", "LEAVE")) signalType else "ICE"
      db.collection("channels")
        .document(channelId)
        .collection("webrtc_signals")
        .document(sid)
        .set(
          mapOf(
            "senderId" to uid,
            "senderCallsign" to callsign.take(32).ifBlank { "PILOT" },
            "targetUid" to targetUid.take(128),
            "channelId" to channelId,
            "signalType" to cleanType,
            "payload" to payload.take(19500),
            "active" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "channels/$channelId/webrtc_signals")
      Result.failure(e)
    }

  suspend fun clearMyChannelWebRtcSignals(channelId: String) {
    runCatching {
      val uid = requireUid()
      val docs =
        db.collection("channels")
          .document(channelId)
          .collection("webrtc_signals")
          .whereEqualTo("active", true)
          .get()
          .await()
          .documents
      docs.filter { it.getString("senderId") == uid }.forEach { it.reference.delete() }
    }
  }

  // --------------------------------------------------------------------------
  // DIRECT MESSAGES & AUDIO/VIDEO CALL SIGNALING
  // --------------------------------------------------------------------------
  fun observeMyDmThreads(userId: String): Flow<List<DmThread>> =
    db.collection("direct_messages")
      .whereArrayContains("participantUids", userId)
      .snapshots()
      .map { snap ->
        snap
          .toObjects(DmThread::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.updatedAt?.seconds ?: 0L }
      }

  fun observeDmMessages(threadId: String, userId: String): Flow<List<DmMessage>> =
    db.collection("direct_messages")
      .document(threadId)
      .collection("messages")
      .whereArrayContains("participantUids", userId)
      .snapshots()
      .map { snap ->
        snap
          .toObjects(DmMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedBy { it.createdAt?.seconds ?: 0L }
      }

  suspend fun sendDirectMessage(
    myCallsign: String,
    peerUid: String,
    peerCallsign: String,
    text: String,
    imageUrl: String = "",
    voiceBase64: String = "",
    selfDestructSec: Long = 0L,
  ): Result<String> =
    try {
      val uid = requireUid()
      val sorted = listOf(uid, peerUid).sorted()
      val threadId = "${sorted[0]}_${sorted[1]}"
      val threadRef = db.collection("direct_messages").document(threadId)
      val threadSnap = threadRef.get().await()
      val preview = text.ifBlank { if (voiceBase64.isNotBlank()) "Voice Transmission" else "Attachment" }
      if (!threadSnap.exists()) {
        threadRef
          .set(
            mapOf(
              "participantUids" to sorted,
              "participantCallsigns" to listOf(myCallsign.take(32), peerCallsign.take(32)),
              "lastMessage" to preview.take(300),
              "lastSenderId" to uid,
              "selfDestructSec" to selfDestructSec.coerceIn(0L, 604800L),
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        threadRef
          .update(
            mapOf(
              "lastMessage" to preview.take(300),
              "lastSenderId" to uid,
              "selfDestructSec" to selfDestructSec.coerceIn(0L, 604800L),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }

      val msgId = "msg_${UUID.randomUUID().toString().take(8)}"
      threadRef
        .collection("messages")
        .document(msgId)
        .set(
          mapOf(
            "participantUids" to sorted,
            "senderId" to uid,
            "senderCallsign" to myCallsign.take(32).ifBlank { "PILOT" },
            "text" to text.take(2000),
            "imageUrl" to imageUrl.take(175_000),
            "voiceBase64" to voiceBase64.take(340000),
            "selfDestructSec" to selfDestructSec.coerceIn(0L, 604800L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(threadId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "direct_messages")
      Result.failure(e)
    }

  suspend fun deleteDmMessage(threadId: String, msgId: String) {
    runCatching {
      requireUid()
      db.collection("direct_messages")
        .document(threadId)
        .collection("messages")
        .document(msgId)
        .delete()
        .await()
    }
  }

  fun observeMyCallSessions(userId: String): Flow<List<CallSession>> =
    combine(
      db.collection("call_sessions").whereEqualTo("calleeUid", userId).snapshots(),
      db.collection("call_sessions").whereEqualTo("callerUid", userId).snapshots(),
    ) { asCalleeSnap, asCallerSnap ->
      val calleeCalls =
        asCalleeSnap.toObjects(
          CallSession::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      val callerCalls =
        asCallerSnap.toObjects(
          CallSession::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      (calleeCalls + callerCalls)
        .distinctBy { it.id }
        .filter { s -> s.status != "ENDED" }
        .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
    }

  fun observeRecentCallHistory(userId: String): Flow<List<CallSession>> =
    combine(
      db.collection("call_sessions").whereEqualTo("calleeUid", userId).snapshots(),
      db.collection("call_sessions").whereEqualTo("callerUid", userId).snapshots(),
    ) { asCalleeSnap, asCallerSnap ->
      val calleeCalls =
        asCalleeSnap.toObjects(
          CallSession::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      val callerCalls =
        asCallerSnap.toObjects(
          CallSession::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      (calleeCalls + callerCalls)
        .distinctBy { it.id }
        .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
        .take(30)
    }

  suspend fun startCallSession(
    callerCallsign: String,
    calleeUid: String,
    calleeCallsign: String,
    callType: String,
  ): Result<String> =
    try {
      val uid = requireUid()
      val sid = "call_${UUID.randomUUID().toString().take(8)}"
      db.collection("call_sessions")
        .document(sid)
        .set(
          mapOf(
            "callerUid" to uid,
            "callerCallsign" to callerCallsign.take(32).ifBlank { "PILOT" },
            "calleeUid" to calleeUid,
            "calleeCallsign" to calleeCallsign.take(32).ifBlank { "PILOT" },
            "callType" to if (callType == "VIDEO") "VIDEO" else "AUDIO",
            "status" to "RINGING",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(sid)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "call_sessions")
      Result.failure(e)
    }

  suspend fun updateCallStatus(sessionId: String, nextStatus: String): Result<Unit> =
    try {
      requireUid()
      val cleanStatus = if (nextStatus in listOf("RINGING", "ACTIVE", "ENDED")) nextStatus else "ENDED"
      db.collection("call_sessions")
        .document(sessionId)
        .update(mapOf("status" to cleanStatus, "updatedAt" to FieldValue.serverTimestamp()))
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "call_sessions/$sessionId")
      Result.failure(e)
    }

  suspend fun updateCallType(sessionId: String, nextCallType: String): Result<Unit> =
    try {
      requireUid()
      val cleanType = if (nextCallType == "VIDEO") "VIDEO" else "AUDIO"
      db.collection("call_sessions")
        .document(sessionId)
        .update(mapOf("callType" to cleanType, "updatedAt" to FieldValue.serverTimestamp()))
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "call_sessions/$sessionId")
      Result.failure(e)
    }

  suspend fun sendCallSignal(
    sessionId: String,
    signalType: String,
    payload: String,
  ): Result<Unit> =
    try {
      val uid = requireUid()
      val cleanType = if (signalType in listOf("OFFER", "ANSWER", "ICE", "CHAT", "PING")) signalType else "ICE"
      val sigId = "sig_${UUID.randomUUID().toString().take(10)}"
      db.collection("call_sessions")
        .document(sessionId)
        .collection("signals")
        .document(sigId)
        .set(
          mapOf(
            "senderId" to uid,
            "signalType" to cleanType,
            "payload" to payload.take(19500),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "call_sessions/$sessionId/signals")
      Result.failure(e)
    }

  fun observeCallSignals(sessionId: String): Flow<List<CallSignal>> =
    db.collection("call_sessions")
      .document(sessionId)
      .collection("signals")
      .snapshots()
      .map { snap ->
        snap
          .toObjects(CallSignal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedBy { (it.createdAt?.seconds ?: 0L) * 1000L + ((it.createdAt?.nanoseconds ?: 0) / 1_000_000L) }
      }

  fun observeCallSession(sessionId: String): Flow<CallSession?> =
    db.collection("call_sessions")
      .document(sessionId)
      .snapshots()
      .map { snap ->
        if (!snap.exists()) null
        else snap.toObject(CallSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }

  suspend fun publishCallStreamFrame(
    sessionId: String,
    kind: String,
    seq: Long,
    active: Boolean,
    payload: String,
  ): Result<Unit> =
    try {
      val uid = requireUid()
      val cleanKind = if (kind == "AUDIO") "AUDIO" else "VIDEO"
      val streamId = if (cleanKind == "AUDIO") "a_$uid" else "v_$uid"
      val safePayload = payload.take(94_500)
      db.collection("call_sessions")
        .document(sessionId)
        .collection("streams")
        .document(streamId)
        .set(
          mapOf(
            "senderId" to uid,
            "kind" to cleanKind,
            "seq" to seq.coerceIn(0L, 100_000_000L),
            "active" to active,
            "payload" to safePayload,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "call_sessions/$sessionId/streams")
      Result.failure(e)
    }

  fun observePeerCallStream(
    sessionId: String,
    peerUid: String,
    kind: String,
  ): Flow<CallStreamFrame?> {
    val cleanKind = if (kind == "AUDIO") "AUDIO" else "VIDEO"
    val streamId = if (cleanKind == "AUDIO") "a_$peerUid" else "v_$peerUid"
    return db.collection("call_sessions")
      .document(sessionId)
      .collection("streams")
      .document(streamId)
      .snapshots()
      .map { snap ->
        if (!snap.exists()) null
        else snap.toObject(CallStreamFrame::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
  }

  // --------------------------------------------------------------------------
  // EMERGENCY BROADCASTS (READ-ONLY) & YOUTUBE COMMUNITY ENGAGEMENT
  // --------------------------------------------------------------------------
  fun observeBroadcasts(): Flow<List<EmergencyBroadcast>> =
    db.collection("broadcasts").whereEqualTo("active", true).snapshots().map { snap ->
      snap
        .toObjects(
          EmergencyBroadcast::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
        .sortedByDescending { it.createdAt?.seconds ?: 0L }
    }

  fun observeYoutubeComments(videoId: String): Flow<List<YoutubeComment>> =
    db.collection("youtube_engagements")
      .document(videoId)
      .collection("comments")
      .whereEqualTo("visibility", "PUBLIC")
      .snapshots()
      .map { snap ->
        snap
          .toObjects(YoutubeComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }

  fun observeYoutubeEngagement(videoId: String): Flow<Long> =
    db.collection("youtube_engagements").document(videoId).snapshots().map { snap ->
      snap.getLong("voltsCount") ?: 0L
    }

  suspend fun voltYoutubeVideo(videoId: String): Result<Unit> =
    try {
      requireUid()
      val safeId = videoId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64)
      val ref = db.collection("youtube_engagements").document(safeId)
      val snap = ref.get().await()
      if (!snap.exists()) {
        ref
          .set(
            mapOf(
              "videoId" to safeId,
              "voltsCount" to 1L,
              "visibility" to "PUBLIC",
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        ref
          .update(
            mapOf(
              "voltsCount" to FieldValue.increment(1L),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "youtube_engagements/$videoId")
      Result.failure(e)
    }

  suspend fun commentYoutubeVideo(videoId: String, callsign: String, text: String): Result<Unit> =
    try {
      val uid = requireUid()
      val safeId = videoId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64)
      val cid = "yc_${UUID.randomUUID().toString().take(8)}"
      db.collection("youtube_engagements")
        .document(safeId)
        .collection("comments")
        .document(cid)
        .set(
          mapOf(
            "userId" to uid,
            "authorCallsign" to callsign.take(32).ifBlank { "PILOT" },
            "text" to text.trim().take(1000),
            "visibility" to "PUBLIC",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "youtube_engagements/$videoId/comments")
      Result.failure(e)
    }
}
