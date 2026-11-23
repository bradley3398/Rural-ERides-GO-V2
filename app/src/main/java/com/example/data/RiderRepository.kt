package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class RiderRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUid(expectedUserId: String? = null): String {
    val uid = auth.currentUser?.uid ?: error("User must sign in with Google first.")
    if (expectedUserId != null) check(uid == expectedUserId) { "Cross-user write blocked." }
    return uid
  }

  // --------------------------------------------------------------------------
  // PROFILE
  // --------------------------------------------------------------------------
  fun observeProfile(userId: String): Flow<RiderProfile?> =
    db.collection("users").document(userId).snapshots().map { snap ->
      if (snap.exists()) {
        snap.toObject(RiderProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      } else null
    }

  suspend fun upsertProfileAndPublicIdentity(
    userId: String,
    displayName: String,
    callsign: String,
    email: String,
    unitSystem: String = "IMPERIAL",
    emergencyContactName: String = "",
    sosContactNumber: String = "",
    medicalProfile: String = "",
    acceptedLegal: Boolean = true,
    activeVehicleId: String = "",
  ): Result<Unit> =
    try {
      requireUid(userId)
      val cleanCallsign = callsign.trim().take(32).ifBlank { "PILOT_${userId.take(5)}" }
      val userRef = db.collection("users").document(userId)
      val existingUser = userRef.get().await()
      val existingCallsign = existingUser.getString("callsign").orEmpty().trim()
      val lockedCallsign = if (existingCallsign.isNotBlank()) existingCallsign else cleanCallsign
      if (!existingUser.exists()) {
        userRef
          .set(
            mapOf(
              "userId" to userId,
              "displayName" to displayName.trim().take(60).ifBlank { lockedCallsign },
              "callsign" to lockedCallsign,
              "email" to email.trim().take(120).ifBlank { "pilot@ruralerides.app" },
              "activeVehicleId" to activeVehicleId.take(128),
              "unitSystem" to if (unitSystem == "METRIC") "METRIC" else "IMPERIAL",
              "emergencyContactName" to emergencyContactName.trim().take(60),
              "sosContactNumber" to sosContactNumber.trim().take(30),
              "medicalProfile" to medicalProfile.trim().take(200),
              "acceptedLegal" to acceptedLegal,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        userRef
          .update(
            mapOf(
              "displayName" to displayName.trim().take(60).ifBlank { lockedCallsign },
              "activeVehicleId" to activeVehicleId.take(128),
              "unitSystem" to if (unitSystem == "METRIC") "METRIC" else "IMPERIAL",
              "emergencyContactName" to emergencyContactName.trim().take(60),
              "sosContactNumber" to sosContactNumber.trim().take(30),
              "medicalProfile" to medicalProfile.trim().take(200),
              "acceptedLegal" to acceptedLegal,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }

      val pubRef = db.collection("user_profiles").document(userId)
      val existingPub = pubRef.get().await()
      if (!existingPub.exists()) {
        // Keep brand-new 0-ride accounts PRIVATE until the rider completes a real ride
        // so automated Google Play Test Lab crawlers never pollute the public Riders directory.
        pubRef
          .set(
            mapOf(
              "userId" to userId,
              "callsign" to lockedCallsign,
              "rankBadge" to "Rookie Pilot",
              "xpPoints" to 0L,
              "totalKm" to 0.0,
              "ridesCount" to 0L,
              "verified" to false,
              "banned" to false,
              "trustScore" to 100L,
              "vehicleSummary" to "PEV Unit",
              "visibility" to "PRIVATE",
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        pubRef
          .update(
            mapOf(
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "users/$userId")
      Result.failure(e)
    }

  suspend fun setActiveVehicle(userId: String, vehicleId: String): Result<Unit> =
    try {
      requireUid(userId)
      db.collection("users")
        .document(userId)
        .update(mapOf("activeVehicleId" to vehicleId, "updatedAt" to FieldValue.serverTimestamp()))
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "users/$userId")
      Result.failure(e)
    }

  suspend fun updateRiderWeightAndAvatar(
    userId: String,
    riderWeightLbs: Double,
    avatarBase64: String,
  ): Result<Unit> =
    try {
      requireUid(userId)
      val cleanWeight = riderWeightLbs.coerceIn(0.0, 600.0)
      val cleanAvatar = avatarBase64.take(62_000)
      db.collection("users")
        .document(userId)
        .update(
          mapOf(
            "riderWeightLbs" to cleanWeight,
            "avatarBase64" to cleanAvatar,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      runCatching {
        db.collection("user_profiles")
          .document(userId)
          .update(
            mapOf(
              "avatarBase64" to cleanAvatar,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "users/$userId")
      Result.failure(e)
    }

  suspend fun updatePublicVehicleSummary(userId: String, summary: String): Result<Unit> =
    try {
      requireUid(userId)
      db.collection("user_profiles")
        .document(userId)
        .update(
          mapOf(
            "vehicleSummary" to summary.trim().take(100),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "user_profiles/$userId")
      Result.failure(e)
    }

  // --------------------------------------------------------------------------
  // GARAGE VEHICLES
  // --------------------------------------------------------------------------
  fun observeVehicles(userId: String): Flow<List<PevVehicle>> =
    db.collection("users").document(userId).collection("vehicles").snapshots().map { snap ->
      snap.toObjects(PevVehicle::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
    }

  suspend fun saveVehicle(userId: String, vehicle: PevVehicle): Result<String> =
    try {
      requireUid(userId)
      val vid = vehicle.id.ifBlank { "veh_${UUID.randomUUID().toString().take(8)}" }
      val ref = db.collection("users").document(userId).collection("vehicles").document(vid)
      val snap = ref.get().await()
      val cleanCategory =
        if (
          vehicle.category in
            listOf(
              "EUC", "ONEWHEEL", "SCOOTER", "EBIKE", "EMOTO", "ESKATE",
              "SURRON", "EMOPED", "ETRIKE", "ECARGO", "EFATBIKE",
              "EFOLDING", "EDIRTBIKE", "EGOKART", "EATV", "OTHER",
            )
        ) {
          vehicle.category
        } else "SCOOTER"
      val computedAh =
        if (vehicle.batteryAh > 0.0) vehicle.batteryAh.coerceIn(0.0, 500.0)
        else if (vehicle.voltage > 0.0) (vehicle.batteryWh / vehicle.voltage).coerceIn(0.0, 500.0)
        else 0.0
      if (!snap.exists()) {
        ref
          .set(
            mapOf(
              "userId" to userId,
              "name" to vehicle.name.trim().take(60).ifBlank { "Custom PEV" },
              "category" to cleanCategory,
              "batteryWh" to vehicle.batteryWh.coerceIn(50L, 15000L),
              "batteryAh" to computedAh,
              "voltage" to vehicle.voltage.coerceIn(12.0, 250.0),
              "motorWatts" to vehicle.motorWatts.coerceIn(0, 50000),
              "topSpeedKmh" to vehicle.topSpeedKmh.coerceIn(0.0, 250.0),
              "vehicleWeightLbs" to vehicle.vehicleWeightLbs.coerceIn(0.0, 1000.0),
              "targetPsi" to vehicle.targetPsi.coerceIn(5.0, 100.0),
              "odometerKm" to vehicle.odometerKm.coerceIn(0.0, 500000.0),
              "serviceIntervalKm" to vehicle.serviceIntervalKm.coerceIn(25.0, 50000.0),
              "tireSize" to vehicle.tireSize.trim().take(40).ifBlank { "10x3" },
              "photoUri" to vehicle.photoUri.take(175_000),
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        ref
          .update(
            mapOf(
              "name" to vehicle.name.trim().take(60).ifBlank { "Custom PEV" },
              "category" to cleanCategory,
              "batteryWh" to vehicle.batteryWh.coerceIn(50L, 15000L),
              "batteryAh" to computedAh,
              "voltage" to vehicle.voltage.coerceIn(12.0, 250.0),
              "motorWatts" to vehicle.motorWatts.coerceIn(0, 50000),
              "topSpeedKmh" to vehicle.topSpeedKmh.coerceIn(0.0, 250.0),
              "vehicleWeightLbs" to vehicle.vehicleWeightLbs.coerceIn(0.0, 1000.0),
              "targetPsi" to vehicle.targetPsi.coerceIn(5.0, 100.0),
              "odometerKm" to vehicle.odometerKm.coerceIn(0.0, 500000.0),
              "serviceIntervalKm" to vehicle.serviceIntervalKm.coerceIn(25.0, 50000.0),
              "tireSize" to vehicle.tireSize.trim().take(40).ifBlank { "10x3" },
              "photoUri" to vehicle.photoUri.take(175_000),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      // Update public vehicle summary
      runCatching {
        db.collection("user_profiles")
          .document(userId)
          .update(
            mapOf(
              "vehicleSummary" to "${vehicle.name.take(40)} (${vehicle.voltage.toInt()}V)",
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(vid)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "users/$userId/vehicles")
      Result.failure(e)
    }

  suspend fun deleteVehicle(userId: String, vehicleId: String): Result<Unit> =
    try {
      requireUid(userId)
      db.collection("users")
        .document(userId)
        .collection("vehicles")
        .document(vehicleId)
        .delete()
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, "users/$userId/vehicles/$vehicleId")
      Result.failure(e)
    }

  // --------------------------------------------------------------------------
  // MISSION RIDE LOGS
  // --------------------------------------------------------------------------
  fun observeRides(userId: String): Flow<List<RideSession>> =
    db.collection("users")
      .document(userId)
      .collection("rides")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .snapshots()
      .map { snap ->
        snap.toObjects(RideSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }

  suspend fun saveRideSession(userId: String, ride: RideSession): Result<String> =
    try {
      requireUid(userId)
      val rid = ride.id.ifBlank { "ride_${UUID.randomUUID().toString().take(8)}" }
      db.collection("users")
        .document(userId)
        .collection("rides")
        .document(rid)
        .set(
          mapOf(
            "userId" to userId,
            "vehicleId" to ride.vehicleId.take(128),
            "vehicleName" to ride.vehicleName.take(60).ifBlank { "PEV Unit" },
            "title" to ride.title.take(80).ifBlank { "Tactical Trail Run" },
            "durationSec" to ride.durationSec.coerceIn(1L, 864000L),
            "distanceKm" to ride.distanceKm.coerceIn(0.0, 3000.0),
            "maxSpeedKmh" to ride.maxSpeedKmh.coerceIn(0.0, 250.0),
            "avgSpeedKmh" to ride.avgSpeedKmh.coerceIn(0.0, 250.0),
            "elevationGainM" to ride.elevationGainM.coerceIn(0.0, 25000.0),
            "whUsed" to ride.whUsed.coerceIn(0.0, 25000.0),
            "startBatteryPct" to ride.startBatteryPct.coerceIn(-1, 100),
            "endBatteryPct" to ride.endBatteryPct.coerceIn(-1, 100),
            "weatherSummary" to ride.weatherSummary.take(160),
            "aiDebrief" to ride.aiDebrief.take(2000),
            "polylinePoints" to ride.polylinePoints.take(250),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()

      // Increment public stats & promote real rider to PUBLIC visibility after completing a real ride
      val xpGain = (ride.distanceKm * 25.0).toLong().coerceAtLeast(15L)
      runCatching {
        val updates =
          mutableMapOf<String, Any>(
            "xpPoints" to FieldValue.increment(xpGain),
            "totalKm" to FieldValue.increment(ride.distanceKm),
            "ridesCount" to FieldValue.increment(1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        if (ride.distanceKm >= 0.1 || ride.durationSec >= 30L) {
          updates["visibility"] = "PUBLIC"
        }
        db.collection("user_profiles")
          .document(userId)
          .update(updates)
          .await()
      }
      if (ride.vehicleId.isNotBlank()) {
        runCatching {
          db.collection("users")
            .document(userId)
            .collection("vehicles")
            .document(ride.vehicleId)
            .update(
              mapOf(
                "odometerKm" to FieldValue.increment(ride.distanceKm),
                "updatedAt" to FieldValue.serverTimestamp(),
              )
            )
            .await()
        }
      }
      Result.success(rid)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "users/$userId/rides")
      Result.failure(e)
    }

  suspend fun updateRideDebrief(userId: String, rideId: String, debrief: String): Result<Unit> =
    try {
      requireUid(userId)
      db.collection("users")
        .document(userId)
        .collection("rides")
        .document(rideId)
        .update(
          mapOf("aiDebrief" to debrief.take(2000), "updatedAt" to FieldValue.serverTimestamp())
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "users/$userId/rides/$rideId")
      Result.failure(e)
    }

  suspend fun deleteRideSession(userId: String, rideId: String): Result<Unit> =
    try {
      requireUid(userId)
      db.collection("users")
        .document(userId)
        .collection("rides")
        .document(rideId)
        .delete()
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, "users/$userId/rides/$rideId")
      Result.failure(e)
    }

  // --------------------------------------------------------------------------
  // MAINTENANCE TASKS
  // --------------------------------------------------------------------------
  fun observeMaintenance(userId: String): Flow<List<MaintenanceTask>> =
    db.collection("users").document(userId).collection("maintenance").snapshots().map { snap ->
      snap
        .toObjects(MaintenanceTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedBy { it.completed }
    }

  suspend fun upsertMaintenance(userId: String, task: MaintenanceTask): Result<String> =
    try {
      requireUid(userId)
      val tid = task.id.ifBlank { "maint_${UUID.randomUUID().toString().take(8)}" }
      val ref = db.collection("users").document(userId).collection("maintenance").document(tid)
      val snap = ref.get().await()
      if (!snap.exists()) {
        ref
          .set(
            mapOf(
              "userId" to userId,
              "vehicleId" to task.vehicleId.take(128).ifBlank { "default" },
              "vehicleName" to task.vehicleName.take(60).ifBlank { "PEV Unit" },
              "title" to task.title.trim().take(80).ifBlank { "Inspection" },
              "notes" to task.notes.trim().take(400),
              "dueOdometerKm" to task.dueOdometerKm.coerceIn(0.0, 500000.0),
              "completed" to task.completed,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        ref
          .update(
            mapOf(
              "title" to task.title.trim().take(80).ifBlank { "Inspection" },
              "notes" to task.notes.trim().take(400),
              "dueOdometerKm" to task.dueOdometerKm.coerceIn(0.0, 500000.0),
              "completed" to task.completed,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(tid)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "users/$userId/maintenance")
      Result.failure(e)
    }

  suspend fun deleteMaintenance(userId: String, taskId: String): Result<Unit> =
    try {
      requireUid(userId)
      db.collection("users")
        .document(userId)
        .collection("maintenance")
        .document(taskId)
        .delete()
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, "users/$userId/maintenance/$taskId")
      Result.failure(e)
    }

  // --------------------------------------------------------------------------
  // SAVED YOUTUBE VIDEOS & NOTES
  // --------------------------------------------------------------------------
  fun observeSavedVideos(userId: String): Flow<List<SavedVideo>> =
    db.collection("users").document(userId).collection("saved_videos").snapshots().map { snap ->
      snap
        .toObjects(SavedVideo::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
    }

  suspend fun saveVideoToVault(
    userId: String,
    video: YouTubeVideoItem,
    categoryTag: String = "PEV Tech",
    personalNote: String = "",
  ): Result<Unit> =
    try {
      requireUid(userId)
      val safeId = video.videoId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64)
      val ref = db.collection("users").document(userId).collection("saved_videos").document(safeId)
      val snap = ref.get().await()
      if (!snap.exists()) {
        ref
          .set(
            mapOf(
              "userId" to userId,
              "videoId" to safeId,
              "title" to video.title.take(180).ifBlank { "YouTube Video" },
              "channelTitle" to video.channelTitle.take(100).ifBlank { "Rural ERides" },
              "thumbnailUrl" to video.thumbnailUrl.take(500),
              "publishedAt" to video.publishedAt.take(40),
              "categoryTag" to categoryTag.take(40).ifBlank { "PEV Tech" },
              "personalNote" to personalNote.take(1000),
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        ref
          .update(
            mapOf(
              "categoryTag" to categoryTag.take(40).ifBlank { "PEV Tech" },
              "personalNote" to personalNote.take(1000),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "users/$userId/saved_videos")
      Result.failure(e)
    }

  suspend fun removeSavedVideo(userId: String, videoId: String): Result<Unit> =
    try {
      requireUid(userId)
      db.collection("users")
        .document(userId)
        .collection("saved_videos")
        .document(videoId)
        .delete()
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, "users/$userId/saved_videos/$videoId")
      Result.failure(e)
    }

  // --------------------------------------------------------------------------
  // RIDER'S PRAYER JOURNAL & NOTIFICATIONS
  // --------------------------------------------------------------------------
  fun observePrayers(userId: String): Flow<List<PrayerJournalEntry>> =
    db.collection("users")
      .document(userId)
      .collection("prayers")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .snapshots()
      .map { snap ->
        snap.toObjects(
          PrayerJournalEntry::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      }

  suspend fun savePrayerEntry(userId: String, title: String, body: String, category: String): Result<Unit> =
    try {
      requireUid(userId)
      val pid = "pr_${UUID.randomUUID().toString().take(8)}"
      db.collection("users")
        .document(userId)
        .collection("prayers")
        .document(pid)
        .set(
          mapOf(
            "userId" to userId,
            "title" to title.trim().take(100).ifBlank { "Trail Prayer" },
            "body" to body.trim().take(2000),
            "category" to category.take(40).ifBlank { "Trail Safety" },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "users/$userId/prayers")
      Result.failure(e)
    }

  suspend fun deletePrayerEntry(userId: String, prayerId: String): Result<Unit> =
    try {
      requireUid(userId)
      db.collection("users")
        .document(userId)
        .collection("prayers")
        .document(prayerId)
        .delete()
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, "users/$userId/prayers/$prayerId")
      Result.failure(e)
    }

  fun observeNotifications(userId: String): Flow<List<UserNotification>> =
    db.collection("users")
      .document(userId)
      .collection("notifications")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .snapshots()
      .map { snap ->
        snap.toObjects(
          UserNotification::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      }

  suspend fun markNotificationRead(userId: String, notifId: String): Result<Unit> =
    try {
      requireUid(userId)
      db.collection("users")
        .document(userId)
        .collection("notifications")
        .document(notifId)
        .update(mapOf("read" to true, "updatedAt" to FieldValue.serverTimestamp()))
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "users/$userId/notifications/$notifId")
      Result.failure(e)
    }

  // --------------------------------------------------------------------------
  // CLOUD BACKUP SNAPSHOT SAVE, RESTORE & EXPORT (FOR JSON VAULT + FIRESTORE)
  // --------------------------------------------------------------------------
  suspend fun exportCloudBackupJson(userId: String, settingsJson: String): Result<String> =
    try {
      requireUid(userId)
      val vehicles =
        db.collection("users").document(userId).collection("vehicles").get().await().documents.map {
          (it.data ?: emptyMap()).toMutableMap().apply { put("id", it.id) }
        }
      val maintenance =
        db.collection("users").document(userId).collection("maintenance").get().await().documents.map {
          (it.data ?: emptyMap()).toMutableMap().apply { put("id", it.id) }
        }
      val videos =
        db.collection("users").document(userId).collection("saved_videos").get().await().documents.map {
          (it.data ?: emptyMap()).toMutableMap().apply { put("id", it.id) }
        }
      val prayers =
        db.collection("users").document(userId).collection("prayers").get().await().documents.map {
          (it.data ?: emptyMap()).toMutableMap().apply { put("id", it.id) }
        }
      val rides =
        db.collection("users").document(userId).collection("rides").get().await().documents.size
      val root =
        JSONObject().apply {
          put("version", 2)
          put("exportedAt", System.currentTimeMillis())
          put("userId", userId)
          put("settings", runCatching { JSONObject(settingsJson) }.getOrDefault(JSONObject()))
          put("vehicleCount", vehicles.size)
          put("rideCount", rides)
          put("vehicles", JSONArray(vehicles.map { JSONObject(it) }))
          put("maintenance", JSONArray(maintenance.map { JSONObject(it) }))
          put("savedVideos", JSONArray(videos.map { JSONObject(it) }))
          put("prayers", JSONArray(prayers.map { JSONObject(it) }))
        }
      Result.success(root.toString())
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, "users/$userId")
      Result.failure(e)
    }

  suspend fun saveFirestoreCloudBackup(
    userId: String,
    payloadJson: String,
    summary: String,
  ): Result<Long> =
    try {
      requireUid(userId)
      val ref = db.collection("users").document(userId).collection("backups").document("latest")
      val existing = ref.get().await()
      val boundedJson = payloadJson.take(840_000)
      val cleanSummary = summary.trim().take(200)
      if (!existing.exists()) {
        ref
          .set(
            mapOf(
              "userId" to userId,
              "provider" to "FIRESTORE",
              "summary" to cleanSummary,
              "payloadJson" to boundedJson,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        ref
          .update(
            mapOf(
              "provider" to "FIRESTORE",
              "summary" to cleanSummary,
              "payloadJson" to boundedJson,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(System.currentTimeMillis())
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "users/$userId/backups/latest")
      Result.failure(e)
    }

  suspend fun loadFirestoreCloudBackup(userId: String): Result<Pair<String, Long>> =
    try {
      requireUid(userId)
      val snap =
        db.collection("users").document(userId).collection("backups").document("latest").get().await()
      if (!snap.exists()) {
        Result.failure(IllegalStateException("No Firestore cloud backup saved yet."))
      } else {
        val payload = snap.getString("payloadJson").orEmpty()
        val ts = (snap.getTimestamp("updatedAt")?.seconds ?: 0L) * 1000L
        if (payload.isBlank()) Result.failure(IllegalStateException("Cloud backup payload is empty."))
        else Result.success(payload to ts)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, "users/$userId/backups/latest")
      Result.failure(e)
    }

  suspend fun restoreFirestoreFromBackupJson(userId: String, rawJson: String): Result<Int> =
    try {
      requireUid(userId)
      val root = JSONObject(rawJson)
      var restoredCount = 0
      root.optJSONArray("vehicles")?.let { arr ->
        for (i in 0 until arr.length()) {
          val o = arr.optJSONObject(i) ?: continue
          val veh =
            PevVehicle(
              id = o.optString("id"),
              userId = userId,
              name = o.optString("name", "Restored PEV"),
              category = o.optString("category", "SCOOTER"),
              batteryWh = o.optLong("batteryWh", 1000L),
              voltage = o.optDouble("voltage", 52.0),
              targetPsi = o.optDouble("targetPsi", 36.0),
              odometerKm = o.optDouble("odometerKm", 0.0),
              serviceIntervalKm = o.optDouble("serviceIntervalKm", 500.0),
              tireSize = o.optString("tireSize", "10x3"),
              photoUri = o.optString("photoUri"),
            )
          if (saveVehicle(userId, veh).isSuccess) restoredCount++
        }
      }
      root.optJSONArray("maintenance")?.let { arr ->
        for (i in 0 until arr.length()) {
          val o = arr.optJSONObject(i) ?: continue
          val task =
            MaintenanceTask(
              id = o.optString("id"),
              userId = userId,
              vehicleId = o.optString("vehicleId", "default"),
              vehicleName = o.optString("vehicleName", "PEV Unit"),
              title = o.optString("title", "Inspection"),
              notes = o.optString("notes"),
              dueOdometerKm = o.optDouble("dueOdometerKm", 0.0),
              completed = o.optBoolean("completed", false),
            )
          if (upsertMaintenance(userId, task).isSuccess) restoredCount++
        }
      }
      Result.success(restoredCount)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "users/$userId")
      Result.failure(e)
    }
}
