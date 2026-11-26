package com.example.data

/**
 * Single unified authenticity guard shared by Firebase Firestore, Supabase PostgREST,
 * Direct Messages, Porch Talk Board, and Rider Radar so zero crawler, Play Pre-Launch Lab,
 * or seeded bot accounts (e.g. ELIPILOT) ever appear on any screen while every real
 * authenticated rider (including brand-new accounts with 0 rides) is always discoverable.
 */
object PilotAuthenticityGuard {

  private val FIREBASE_UID_REGEX = Regex("^[A-Za-z0-9]{20,40}$")
  private val SUPABASE_UUID_REGEX =
    Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

  private val blockedPrefixes =
    listOf(
      "mock", "demo", "seed", "fake", "test_", "bot_", "sample_", "guest_", "anon_",
      "placeholder", "sim_", "npc_", "dummy", "ai_",
      "virtual_", "synth_", "ghost_", "rosie_", "robo_",
      "playtest", "prelaunch", "crawler", "qa_",
    )

  private val blockedSuffixes =
    listOf(
      "_bot", "_demo", "_test", "_mock", "_fake",
    )

  private val blockedNormalizedCallsigns =
    setOf(
      "test", "demo", "guest", "anonymous", "bot", "unknown",
      "nighthawk", "voltrider", "voltpilot", "voltscooter", "ghostrider", "rosiepilot",
      "pilotone", "eagleone", "ironhorse", "trailhawk",
      "cybervolt", "neonrider", "ridgerunner", "gravelking", "sparkrider",
      "voltking", "ruralbot", "demorider", "fakepilot",
      "testpilot", "testrider", "playtest", "crawler", "robottest",
      "elipilot", "demopilot", "botpilot", "autopilot", "prelaunch", "robolab",
      "firebasecrawler", "appium", "monkey", "pixelbot", "emulator",
    )

  private val crawlerPattern =
    Regex(
      "^(rosie|test|demo|robo|bot|guest|anon|elipilot|playtest|prelaunch|sim_|qa_)[a-z_\\-]*\\d*$"
    )

  private val blockedTokens =
    listOf(
      "mockup", "placeholder", "fake_pilot", "demo_pilot", "test_pilot",
      "volt_pilot", "voltpilot", "voltrider", "ghostrider", "rosie_pilot",
      "elipilot", "eli_pilot", "pilotone", "eagleone", "voltscooter", "bot_rider",
      "dummy_rider", "npc_pilot", "stock_pilot", "seeded", "synthetic",
      "fake_rider", "sample_pilot", "testuser", "robolab", "prelaunch", "playtest",
    )

  private val stockAvatarHosts =
    listOf(
      "pravatar", "randomuser", "dicebear", "ui-avatars", "picsum",
      "robohash", "thispersondoesnotexist", "unsplash.com/photo-", "pexels.com",
    )

  fun normalizeUid(uid: String): String = uid.trim().removePrefix("sb_")

  fun isValidFirebaseUid(uid: String): Boolean {
    val trimmed = uid.trim()
    return FIREBASE_UID_REGEX.matches(trimmed) &&
      trimmed.any { it.isLetter() }
  }

  fun isValidRealAuthUid(uid: String, currentUid: String? = null): Boolean {
    val trimmed = uid.trim()
    if (!currentUid.isNullOrBlank() && normalizeUid(trimmed) == normalizeUid(currentUid)) return true
    val strippedSb = normalizeUid(trimmed)
    if (trimmed.length !in 20..44 && !SUPABASE_UUID_REGEX.matches(strippedSb)) return false
    return isValidFirebaseUid(trimmed) || SUPABASE_UUID_REGEX.matches(strippedSb)
  }

  fun isAuthenticCallsignAndUid(
    userId: String,
    callsign: String,
    avatarSource: String = "",
    docId: String? = null,
    requireFirebaseUid: Boolean = false,
    currentUid: String? = null,
  ): Boolean {
    val uid = userId.trim()
    val isSelf = !currentUid.isNullOrBlank() && normalizeUid(uid) == normalizeUid(currentUid)
    if (isSelf) return true

    if (docId != null && docId.isNotBlank() && normalizeUid(docId) != normalizeUid(uid)) return false
    val uidLower = uid.lowercase()
    val cs = callsign.trim().lowercase()
    if (cs.isBlank()) return false

    if (requireFirebaseUid) {
      if (!isValidFirebaseUid(uid)) return false
    } else if (!isValidRealAuthUid(uid, currentUid)) {
      return false
    }

    if (blockedPrefixes.any { uidLower.startsWith(it) || cs.startsWith(it) }) return false
    if (blockedSuffixes.any { uidLower.endsWith(it) || cs.endsWith(it) }) return false

    val csNorm = cs.replace(Regex("[_\\-\\s]+"), "").replace(Regex("\\d+$"), "")
    if (cs in blockedNormalizedCallsigns || csNorm in blockedNormalizedCallsigns) return false
    if (crawlerPattern.matches(cs) || crawlerPattern.matches(csNorm)) return false
    if (blockedTokens.any { uidLower.contains(it) || cs.contains(it) || csNorm.contains(it) }) return false

    val avatarLower = avatarSource.trim().lowercase()
    if (avatarLower.isNotBlank() && stockAvatarHosts.any { avatarLower.contains(it) }) return false
    return true
  }

  fun isLikelyBotProfile(p: PublicPilotProfile): Boolean {
    val csLower = p.callsign.trim().lowercase()
    if (csLower in blockedNormalizedCallsigns) return true
    if (blockedTokens.any { csLower.contains(it) }) return true
    return false
  }

  fun isDisplayablePilotProfile(
    p: PublicPilotProfile,
    currentUid: String? = null,
    requireFirebaseUid: Boolean = false,
    maxStaleDays: Long = 90L,
    nowSec: Long = System.currentTimeMillis() / 1000L,
  ): Boolean {
    val isSelf = !currentUid.isNullOrBlank() && normalizeUid(p.userId) == normalizeUid(currentUid)
    if (p.banned) return false
    if (
      !isAuthenticCallsignAndUid(
        userId = p.userId,
        callsign = p.callsign,
        avatarSource = p.avatarBase64,
        docId = p.id.ifBlank { null },
        requireFirebaseUid = requireFirebaseUid,
        currentUid = currentUid,
      )
    ) {
      return false
    }
    if (isSelf) return true

    if (isLikelyBotProfile(p)) return false

    val updatedSec = p.updatedAt?.seconds ?: p.createdAt?.seconds ?: 0L
    val maxStaleSec = maxStaleDays * 86400L
    if (updatedSec > 0L && (nowSec - updatedSec) > maxStaleSec) {
      return false
    }
    return true
  }
}
