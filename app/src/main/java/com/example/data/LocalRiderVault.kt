package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.localVaultStore by preferencesDataStore(name = "volttrail_local_vault")

/**
 * Device-local JSON DataStore vault keyed by user UID so pilots signed in via
 * Supabase-only sessions retain profiles, PEV fleet specs, maintenance logs,
 * prayer journal entries, and saved videos seamlessly without requiring Firebase Auth.
 */
class LocalRiderVault(private val context: Context) {

  private fun profileKey(uid: String) = stringPreferencesKey("profile_$uid")
  private fun vehiclesKey(uid: String) = stringPreferencesKey("vehicles_$uid")
  private fun tasksKey(uid: String) = stringPreferencesKey("tasks_$uid")
  private fun videosKey(uid: String) = stringPreferencesKey("videos_$uid")
  private fun prayersKey(uid: String) = stringPreferencesKey("prayers_$uid")
  private fun bookmarksKey(uid: String) = stringPreferencesKey("bookmarks_$uid")
  private fun mutedPeersKey(uid: String) = stringPreferencesKey("muted_peers_$uid")
  private fun wearStatesKey(uid: String) = stringPreferencesKey("wear_states_$uid")
  private fun serviceLogsKey(uid: String) = stringPreferencesKey("service_logs_$uid")
  private fun checklistKey(uid: String) = stringPreferencesKey("preflight_checklist_$uid")
  private fun followedPilotsKey(uid: String) = stringPreferencesKey("followed_pilots_$uid")
  private fun pinnedDmThreadsKey(uid: String) = stringPreferencesKey("pinned_dm_threads_$uid")
  private fun savedBriefingsKey(uid: String) = stringPreferencesKey("saved_copilot_briefings_$uid")

  suspend fun loadProfile(uid: String): RiderProfile? {
    val raw = context.localVaultStore.data.first()[profileKey(uid)] ?: return null
    return runCatching {
      val o = JSONObject(raw)
      RiderProfile(
        id = o.optString("id", uid),
        userId = o.optString("userId", uid),
        displayName = o.optString("displayName"),
        callsign = o.optString("callsign"),
        email = o.optString("email"),
        activeVehicleId = o.optString("activeVehicleId"),
        unitSystem = o.optString("unitSystem", "IMPERIAL"),
        emergencyContactName = o.optString("emergencyContactName"),
        sosContactNumber = o.optString("sosContactNumber"),
        medicalProfile = o.optString("medicalProfile"),
        acceptedLegal = o.optBoolean("acceptedLegal", false),
        riderWeightLbs = o.optDouble("riderWeightLbs", 0.0),
        avatarBase64 = o.optString("avatarBase64"),
      )
    }.getOrNull()
  }

  suspend fun saveProfile(profile: RiderProfile) {
    val uid = profile.userId.ifBlank { profile.id }
    if (uid.isBlank()) return
    val json =
      JSONObject().apply {
        put("id", profile.id.ifBlank { uid })
        put("userId", uid)
        put("displayName", profile.displayName)
        put("callsign", profile.callsign)
        put("email", profile.email)
        put("activeVehicleId", profile.activeVehicleId)
        put("unitSystem", profile.unitSystem)
        put("emergencyContactName", profile.emergencyContactName)
        put("sosContactNumber", profile.sosContactNumber)
        put("medicalProfile", profile.medicalProfile)
        put("acceptedLegal", profile.acceptedLegal)
        put("riderWeightLbs", profile.riderWeightLbs)
        put("avatarBase64", profile.avatarBase64)
      }
    context.localVaultStore.edit { it[profileKey(uid)] = json.toString() }
  }

  suspend fun loadVehicles(uid: String): List<PevVehicle> {
    val raw = context.localVaultStore.data.first()[vehiclesKey(uid)] ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      (0 until arr.length()).mapNotNull { idx ->
        val o = arr.optJSONObject(idx) ?: return@mapNotNull null
        PevVehicle(
          id = o.optString("id"),
          userId = o.optString("userId", uid),
          name = o.optString("name"),
          category = o.optString("category", "SCOOTER"),
          batteryWh = o.optLong("batteryWh", 1000L),
          batteryAh = o.optDouble("batteryAh", 0.0),
          voltage = o.optDouble("voltage", 52.0),
          motorWatts = o.optInt("motorWatts", 0),
          topSpeedKmh = o.optDouble("topSpeedKmh", 0.0),
          vehicleWeightLbs = o.optDouble("vehicleWeightLbs", 0.0),
          targetPsi = o.optDouble("targetPsi", 36.0),
          odometerKm = o.optDouble("odometerKm", 0.0),
          serviceIntervalKm = o.optDouble("serviceIntervalKm", 500.0),
          tireSize = o.optString("tireSize", "10x3.0"),
          photoUri = o.optString("photoUri"),
          brandLogoUri = o.optString("brandLogoUri"),
        )
      }
    }.getOrDefault(emptyList())
  }

  suspend fun saveVehicles(uid: String, list: List<PevVehicle>) {
    val arr = JSONArray()
    list.forEach { v ->
      arr.put(
        JSONObject().apply {
          put("id", v.id)
          put("userId", uid)
          put("name", v.name)
          put("category", v.category)
          put("batteryWh", v.batteryWh)
          put("batteryAh", v.batteryAh)
          put("voltage", v.voltage)
          put("motorWatts", v.motorWatts)
          put("topSpeedKmh", v.topSpeedKmh)
          put("vehicleWeightLbs", v.vehicleWeightLbs)
          put("targetPsi", v.targetPsi)
          put("odometerKm", v.odometerKm)
          put("serviceIntervalKm", v.serviceIntervalKm)
          put("tireSize", v.tireSize)
          put("photoUri", v.photoUri)
          put("brandLogoUri", v.brandLogoUri)
        }
      )
    }
    context.localVaultStore.edit { it[vehiclesKey(uid)] = arr.toString() }
  }

  suspend fun loadTasks(uid: String): List<MaintenanceTask> {
    val raw = context.localVaultStore.data.first()[tasksKey(uid)] ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      (0 until arr.length()).mapNotNull { idx ->
        val o = arr.optJSONObject(idx) ?: return@mapNotNull null
        MaintenanceTask(
          id = o.optString("id"),
          userId = o.optString("userId", uid),
          vehicleId = o.optString("vehicleId"),
          vehicleName = o.optString("vehicleName"),
          title = o.optString("title"),
          notes = o.optString("notes"),
          dueOdometerKm = o.optDouble("dueOdometerKm", 0.0),
          completed = o.optBoolean("completed", false),
        )
      }
    }.getOrDefault(emptyList())
  }

  suspend fun saveTasks(uid: String, list: List<MaintenanceTask>) {
    val arr = JSONArray()
    list.forEach { t ->
      arr.put(
        JSONObject().apply {
          put("id", t.id)
          put("userId", uid)
          put("vehicleId", t.vehicleId)
          put("vehicleName", t.vehicleName)
          put("title", t.title)
          put("notes", t.notes)
          put("dueOdometerKm", t.dueOdometerKm)
          put("completed", t.completed)
        }
      )
    }
    context.localVaultStore.edit { it[tasksKey(uid)] = arr.toString() }
  }

  suspend fun loadSavedVideos(uid: String): List<SavedVideo> {
    val raw = context.localVaultStore.data.first()[videosKey(uid)] ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      (0 until arr.length()).mapNotNull { idx ->
        val o = arr.optJSONObject(idx) ?: return@mapNotNull null
        SavedVideo(
          id = o.optString("id"),
          userId = o.optString("userId", uid),
          videoId = o.optString("videoId"),
          title = o.optString("title"),
          channelTitle = o.optString("channelTitle"),
          thumbnailUrl = o.optString("thumbnailUrl"),
          publishedAt = o.optString("publishedAt"),
          categoryTag = o.optString("categoryTag", "General"),
          personalNote = o.optString("personalNote"),
        )
      }
    }.getOrDefault(emptyList())
  }

  suspend fun saveSavedVideos(uid: String, list: List<SavedVideo>) {
    val arr = JSONArray()
    list.forEach { v ->
      arr.put(
        JSONObject().apply {
          put("id", v.id)
          put("userId", uid)
          put("videoId", v.videoId)
          put("title", v.title)
          put("channelTitle", v.channelTitle)
          put("thumbnailUrl", v.thumbnailUrl)
          put("publishedAt", v.publishedAt)
          put("categoryTag", v.categoryTag)
          put("personalNote", v.personalNote)
        }
      )
    }
    context.localVaultStore.edit { it[videosKey(uid)] = arr.toString() }
  }

  suspend fun loadPrayers(uid: String): List<PrayerJournalEntry> {
    val raw = context.localVaultStore.data.first()[prayersKey(uid)] ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      (0 until arr.length()).mapNotNull { idx ->
        val o = arr.optJSONObject(idx) ?: return@mapNotNull null
        PrayerJournalEntry(
          id = o.optString("id"),
          userId = o.optString("userId", uid),
          title = o.optString("title"),
          body = o.optString("body"),
          category = o.optString("category", "Trail Safety"),
        )
      }
    }.getOrDefault(emptyList())
  }

  suspend fun savePrayers(uid: String, list: List<PrayerJournalEntry>) {
    val arr = JSONArray()
    list.forEach { p ->
      arr.put(
        JSONObject().apply {
          put("id", p.id)
          put("userId", uid)
          put("title", p.title)
          put("body", p.body)
          put("category", p.category)
        }
      )
    }
    context.localVaultStore.edit { it[prayersKey(uid)] = arr.toString() }
  }

  suspend fun loadBookmarkedPostIds(uid: String): Set<String> {
    val raw = context.localVaultStore.data.first()[bookmarksKey(uid)] ?: return emptySet()
    return runCatching {
      val arr = JSONArray(raw)
      (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }.toSet()
    }.getOrDefault(emptySet())
  }

  suspend fun toggleBookmarkedPostId(uid: String, postId: String): Set<String> {
    val current = loadBookmarkedPostIds(uid).toMutableSet()
    if (!current.add(postId)) current.remove(postId)
    val arr = JSONArray()
    current.forEach { arr.put(it) }
    context.localVaultStore.edit { it[bookmarksKey(uid)] = arr.toString() }
    return current
  }

  suspend fun loadMutedPeerIds(uid: String): Set<String> {
    val raw = context.localVaultStore.data.first()[mutedPeersKey(uid)] ?: return emptySet()
    return runCatching {
      val arr = JSONArray(raw)
      (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }.toSet()
    }.getOrDefault(emptySet())
  }

  suspend fun toggleMutedPeerId(uid: String, peerUid: String): Set<String> {
    val current = loadMutedPeerIds(uid).toMutableSet()
    if (!current.add(peerUid)) current.remove(peerUid)
    val arr = JSONArray()
    current.forEach { arr.put(it) }
    context.localVaultStore.edit { it[mutedPeersKey(uid)] = arr.toString() }
    return current
  }

  suspend fun loadFollowedPilotIds(uid: String): Set<String> {
    val raw = context.localVaultStore.data.first()[followedPilotsKey(uid)] ?: return emptySet()
    return runCatching {
      val arr = JSONArray(raw)
      (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }.toSet()
    }.getOrDefault(emptySet())
  }

  suspend fun saveFollowedPilotIds(uid: String, ids: Set<String>) {
    val arr = JSONArray()
    ids.forEach { arr.put(it) }
    context.localVaultStore.edit { it[followedPilotsKey(uid)] = arr.toString() }
  }

  suspend fun toggleFollowedPilotId(uid: String, targetUid: String): Set<String> {
    val current = loadFollowedPilotIds(uid).toMutableSet()
    if (!current.add(targetUid)) current.remove(targetUid)
    saveFollowedPilotIds(uid, current)
    return current
  }

  suspend fun loadPinnedDmThreadIds(uid: String): Set<String> {
    val raw = context.localVaultStore.data.first()[pinnedDmThreadsKey(uid)] ?: return emptySet()
    return runCatching {
      val arr = JSONArray(raw)
      (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }.toSet()
    }.getOrDefault(emptySet())
  }

  suspend fun togglePinnedDmThreadId(uid: String, threadId: String): Set<String> {
    val current = loadPinnedDmThreadIds(uid).toMutableSet()
    if (!current.add(threadId)) current.remove(threadId)
    val arr = JSONArray()
    current.forEach { arr.put(it) }
    context.localVaultStore.edit { it[pinnedDmThreadsKey(uid)] = arr.toString() }
    return current
  }

  suspend fun loadSavedCopilotBriefings(uid: String): List<SavedCopilotBriefing> {
    val raw = context.localVaultStore.data.first()[savedBriefingsKey(uid)] ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      (0 until arr.length()).mapNotNull { i ->
        val o = arr.optJSONObject(i) ?: return@mapNotNull null
        SavedCopilotBriefing(
          id = o.optString("id"),
          title = o.optString("title"),
          persona = o.optString("persona"),
          content = o.optString("content"),
          savedAtMs = o.optLong("savedAtMs", System.currentTimeMillis()),
        )
      }
    }.getOrDefault(emptyList())
  }

  suspend fun saveCopilotBriefings(uid: String, list: List<SavedCopilotBriefing>) {
    val arr = JSONArray()
    list.take(60).forEach { b ->
      arr.put(
        JSONObject().apply {
          put("id", b.id)
          put("title", b.title)
          put("persona", b.persona)
          put("content", b.content)
          put("savedAtMs", b.savedAtMs)
        }
      )
    }
    context.localVaultStore.edit { it[savedBriefingsKey(uid)] = arr.toString() }
  }

  suspend fun loadWearStates(uid: String): List<MaintenanceWearState> {
    val raw = context.localVaultStore.data.first()[wearStatesKey(uid)] ?: return defaultWearStates()
    return runCatching {
      val arr = JSONArray(raw)
      (0 until arr.length()).mapNotNull { i ->
        val o = arr.optJSONObject(i) ?: return@mapNotNull null
        MaintenanceWearState(
          itemId = o.optString("itemId"),
          label = o.optString("label"),
          ratedKm = o.optDouble("ratedKm", 1000.0),
          lastResetKm = o.optDouble("lastResetKm", 0.0),
        )
      }.ifEmpty { defaultWearStates() }
    }.getOrDefault(defaultWearStates())
  }

  suspend fun saveWearStates(uid: String, list: List<MaintenanceWearState>) {
    val arr = JSONArray()
    list.forEach { w ->
      arr.put(
        JSONObject().apply {
          put("itemId", w.itemId)
          put("label", w.label)
          put("ratedKm", w.ratedKm)
          put("lastResetKm", w.lastResetKm)
        }
      )
    }
    context.localVaultStore.edit { it[wearStatesKey(uid)] = arr.toString() }
  }

  suspend fun loadServiceLogs(uid: String): List<ServiceLogRecord> {
    val raw = context.localVaultStore.data.first()[serviceLogsKey(uid)] ?: return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      (0 until arr.length()).mapNotNull { i ->
        val o = arr.optJSONObject(i) ?: return@mapNotNull null
        ServiceLogRecord(
          id = o.optString("id"),
          dateLabel = o.optString("dateLabel"),
          component = o.optString("component"),
          notes = o.optString("notes"),
          costUsd = o.optDouble("costUsd", 0.0),
          odometerKm = o.optDouble("odometerKm", 0.0),
        )
      }
    }.getOrDefault(emptyList())
  }

  suspend fun saveServiceLogs(uid: String, list: List<ServiceLogRecord>) {
    val arr = JSONArray()
    list.forEach { l ->
      arr.put(
        JSONObject().apply {
          put("id", l.id)
          put("dateLabel", l.dateLabel)
          put("component", l.component)
          put("notes", l.notes)
          put("costUsd", l.costUsd)
          put("odometerKm", l.odometerKm)
        }
      )
    }
    context.localVaultStore.edit { it[serviceLogsKey(uid)] = arr.toString() }
  }

  suspend fun loadPreflightChecklist(uid: String): Set<String> {
    val raw = context.localVaultStore.data.first()[checklistKey(uid)] ?: return emptySet()
    return runCatching {
      val arr = JSONArray(raw)
      (0 until arr.length()).map { arr.optString(it) }.toSet()
    }.getOrDefault(emptySet())
  }

  suspend fun savePreflightChecklist(uid: String, checked: Set<String>) {
    val arr = JSONArray()
    checked.forEach { arr.put(it) }
    context.localVaultStore.edit { it[checklistKey(uid)] = arr.toString() }
  }

  private fun defaultWearStates(): List<MaintenanceWearState> =
    listOf(
      MaintenanceWearState("w_front_pads", "Front Brake Pads", 1200.0, 0.0),
      MaintenanceWearState("w_rear_pads", "Rear Brake Pads", 1000.0, 0.0),
      MaintenanceWearState("w_front_tire", "Front Tire Tread", 2400.0, 0.0),
      MaintenanceWearState("w_rear_tire", "Rear Tire Tread", 1800.0, 0.0),
      MaintenanceWearState("w_stem_bolts", "Stem & Folding Latch Torque", 500.0, 0.0),
      MaintenanceWearState("w_bearings", "Wheel Bearings & Seals", 3500.0, 0.0),
    )

  suspend fun exportFullBackupJson(uid: String, settingsJson: String): String {
    val vehicles = loadVehicles(uid)
    val tasks = loadTasks(uid)
    val videos = loadSavedVideos(uid)
    val prayers = loadPrayers(uid)
    val wear = loadWearStates(uid)
    val logs = loadServiceLogs(uid)
    return JSONObject()
      .apply {
        put("version", 2)
        put("exportedAt", System.currentTimeMillis())
        put("userId", uid)
        put("settings", runCatching { JSONObject(settingsJson) }.getOrDefault(JSONObject()))
        put(
          "vehicles",
          JSONArray().apply {
            vehicles.forEach { v ->
              put(
                JSONObject().apply {
                  put("id", v.id)
                  put("name", v.name)
                  put("category", v.category)
                  put("batteryWh", v.batteryWh)
                  put("voltage", v.voltage)
                  put("targetPsi", v.targetPsi)
                  put("odometerKm", v.odometerKm)
                  put("serviceIntervalKm", v.serviceIntervalKm)
                  put("tireSize", v.tireSize)
                  put("photoUri", v.photoUri)
                  put("brandLogoUri", v.brandLogoUri)
                }
              )
            }
          },
        )
        put(
          "maintenance",
          JSONArray().apply {
            tasks.forEach { t ->
              put(
                JSONObject().apply {
                  put("id", t.id)
                  put("vehicleId", t.vehicleId)
                  put("vehicleName", t.vehicleName)
                  put("title", t.title)
                  put("notes", t.notes)
                  put("dueOdometerKm", t.dueOdometerKm)
                  put("completed", t.completed)
                }
              )
            }
          },
        )
        put(
          "savedVideos",
          JSONArray().apply {
            videos.forEach { v ->
              put(
                JSONObject().apply {
                  put("id", v.id)
                  put("videoId", v.videoId)
                  put("title", v.title)
                  put("channelTitle", v.channelTitle)
                  put("thumbnailUrl", v.thumbnailUrl)
                  put("publishedAt", v.publishedAt)
                  put("categoryTag", v.categoryTag)
                  put("personalNote", v.personalNote)
                }
              )
            }
          },
        )
        put(
          "prayers",
          JSONArray().apply {
            prayers.forEach { p ->
              put(
                JSONObject().apply {
                  put("id", p.id)
                  put("title", p.title)
                  put("body", p.body)
                  put("category", p.category)
                }
              )
            }
          },
        )
        put(
          "wearStates",
          JSONArray().apply {
            wear.forEach { w ->
              put(
                JSONObject().apply {
                  put("itemId", w.itemId)
                  put("label", w.label)
                  put("ratedKm", w.ratedKm)
                  put("lastResetKm", w.lastResetKm)
                }
              )
            }
          },
        )
        put(
          "serviceLogs",
          JSONArray().apply {
            logs.forEach { l ->
              put(
                JSONObject().apply {
                  put("id", l.id)
                  put("dateLabel", l.dateLabel)
                  put("component", l.component)
                  put("notes", l.notes)
                  put("costUsd", l.costUsd)
                  put("odometerKm", l.odometerKm)
                }
              )
            }
          },
        )
      }
      .toString()
  }

  suspend fun restoreFullBackupJson(uid: String, rawJson: String): Result<String> =
    runCatching {
      val root = JSONObject(rawJson)
      root.optJSONArray("vehicles")?.let { arr ->
        val list =
          (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            PevVehicle(
              id = o.optString("id"),
              userId = uid,
              name = o.optString("name", "Restored PEV"),
              category = o.optString("category", "SCOOTER"),
              batteryWh = o.optLong("batteryWh", 1000L),
              voltage = o.optDouble("voltage", 52.0),
              targetPsi = o.optDouble("targetPsi", 36.0),
              odometerKm = o.optDouble("odometerKm", 0.0),
              serviceIntervalKm = o.optDouble("serviceIntervalKm", 500.0),
              tireSize = o.optString("tireSize", "10x3.0"),
              photoUri = o.optString("photoUri"),
              brandLogoUri = o.optString("brandLogoUri"),
            )
          }
        if (list.isNotEmpty()) saveVehicles(uid, list)
      }
      root.optJSONArray("maintenance")?.let { arr ->
        val list =
          (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            MaintenanceTask(
              id = o.optString("id"),
              userId = uid,
              vehicleId = o.optString("vehicleId"),
              vehicleName = o.optString("vehicleName"),
              title = o.optString("title"),
              notes = o.optString("notes"),
              dueOdometerKm = o.optDouble("dueOdometerKm", 0.0),
              completed = o.optBoolean("completed", false),
            )
          }
        if (list.isNotEmpty()) saveTasks(uid, list)
      }
      root.optJSONArray("savedVideos")?.let { arr ->
        val list =
          (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            SavedVideo(
              id = o.optString("id"),
              userId = uid,
              videoId = o.optString("videoId"),
              title = o.optString("title"),
              channelTitle = o.optString("channelTitle"),
              thumbnailUrl = o.optString("thumbnailUrl"),
              publishedAt = o.optString("publishedAt"),
              categoryTag = o.optString("categoryTag", "General"),
              personalNote = o.optString("personalNote"),
            )
          }
        if (list.isNotEmpty()) saveSavedVideos(uid, list)
      }
      root.optJSONArray("prayers")?.let { arr ->
        val list =
          (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            PrayerJournalEntry(
              id = o.optString("id"),
              userId = uid,
              title = o.optString("title"),
              body = o.optString("body"),
              category = o.optString("category", "Trail Safety"),
            )
          }
        if (list.isNotEmpty()) savePrayers(uid, list)
      }
      root.optJSONArray("wearStates")?.let { arr ->
        val list =
          (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            MaintenanceWearState(
              itemId = o.optString("itemId"),
              label = o.optString("label"),
              ratedKm = o.optDouble("ratedKm", 1000.0),
              lastResetKm = o.optDouble("lastResetKm", 0.0),
            )
          }
        if (list.isNotEmpty()) saveWearStates(uid, list)
      }
      root.optJSONArray("serviceLogs")?.let { arr ->
        val list =
          (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            ServiceLogRecord(
              id = o.optString("id"),
              dateLabel = o.optString("dateLabel"),
              component = o.optString("component"),
              notes = o.optString("notes"),
              costUsd = o.optDouble("costUsd", 0.0),
              odometerKm = o.optDouble("odometerKm", 0.0),
            )
          }
        if (list.isNotEmpty()) saveServiceLogs(uid, list)
      }
      "Local vault restored"
    }

  suspend fun clearAll() {
    context.localVaultStore.edit { it.clear() }
  }
}
