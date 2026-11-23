package com.example.data

/**
 * Single unified authenticity guard shared by Firebase Firestore, Supabase PostgREST,
 * Direct Messages, Porch Talk Board, and Rider Radar so zero crawler, Play Pre-Launch Lab,
 * seeded, or 0-ride fake pilot accounts (e.g. ELIPILOT) ever appear on any screen.
 */
object PilotAuthenticityGuard {

  private val FIREBASE_UID_REGEX = Regex("^[A-Za-z0-9]{24,36}$")
  private val SUPABASE_UUID_REGEX =
    Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

  private val blockedPrefixes =
    listOf(
      "mock", "demo", "seed", "fake", "test", "bot", "sample", "guest", "anon",
      "placeholder", "sim_", "pilot_", "rider_", "npc_", "dummy", "user_", "ai_",
      "auto_", "virtual_", "synth_", "ghost_", "alpha_", "bravo_", "charlie_",
      "delta_", "echo_", "foxtrot_", "omega_", "rover_", "scout_", "rosie_", "robo_",
      "eli_", "playtest", "prelaunch", "crawler", "qa_",
    )

  private val blockedSuffixes =
    listOf(
      "_pilot", "_rider", "_bot", "_demo", "_test", "_mock", "_fake",
      "pilot", "rider", "bot", "demo", "test", "mock", "fake",
    )

  private val blockedNormalizedCallsigns =
    setOf(
      "pilot", "rider", "test", "demo", "guest", "anonymous", "bot", "unknown",
      "nighthawk", "voltrider", "voltpilot", "voltscooter", "ghostrider", "rosiepilot",
      "falcon", "pilotone", "eagleone", "ironhorse", "apex", "trailhawk", "coyote",
      "cybervolt", "shadow", "blaze", "strider", "ghost", "phantom", "drifter",
      "viper", "neonrider", "ridgerunner", "gravelking", "sparkrider",
      "voltking", "trailblazer", "ruralbot", "demorider", "fakepilot",
      "alpha", "bravo", "charlie", "delta", "echo", "foxtrot", "scout", "rover",
      "testpilot", "testrider", "playtest", "crawler", "robottest", "qa",
      "elipilot", "demopilot", "botpilot", "autopilot", "prelaunch", "robolab",
      "firebasecrawler", "appium", "monkey", "pixelbot", "emulator",
    )

  private val crawlerPattern =
    Regex(
      "^(volt|pilot|rider|ghost|falcon|eagle|rosie|scout|rover|test|demo|robo|bot|user|guest|anon|eli|play|auto|sim|qa)[a-z_\\-]*\\d*$"
    )

  private val genericRolePattern =
    Regex("^[a-z]{2,8}(pilot|rider|bot)\\d*$")

  private val blockedTokens =
    listOf(
      "mockup", "placeholder", "fake_pilot", "demo_pilot", "test_pilot", "_pilot_",
      "_rider_", "volt_pilot", "voltpilot", "voltrider", "ghostrider", "rosie_pilot",
      "elipilot", "eli_pilot", "pilotone", "eagleone", "voltscooter", "bot_rider",
      "example", "dummy_rider", "npc_pilot", "stock_pilot", "seeded", "synthetic",
      "fake_rider", "sample_pilot", "testuser", "robolab", "prelaunch", "playtest",
    )

  private val stockAvatarHosts =
    listOf(
      "pravatar", "randomuser", "dicebear", "ui-avatars", "picsum",
      "robohash", "thispersondoesnotexist", "unsplash.com/photo-", "pexels.com",
    )

  private val defaultBotVehicles =
    setOf(
      "", "pev unit", "pev rig", "pev rider", "custom pev (60v)",
      "voltscooter (60v)", "nami (60v)", "unknown",
    )

  fun normalizeUid(uid: String): String = uid.trim().removePrefix("sb_")

  fun isValidFirebaseUid(uid: String): Boolean {
    val trimmed = uid.trim()
    return FIREBASE_UID_REGEX.matches(trimmed) &&
      trimmed.any { it.isLetter() } &&
      trimmed.any { it.isDigit() }
  }

  fun isValidRealAuthUid(uid: String, currentUid: String? = null): Boolean {
    val trimmed = uid.trim()
    if (!currentUid.isNullOrBlank() && normalizeUid(trimmed) == normalizeUid(currentUid)) return true
    val strippedSb = normalizeUid(trimmed)
    if (trimmed.length !in 24..40 && !SUPABASE_UUID_REGEX.matches(strippedSb)) return false
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
    if (cs.length < 2) return false

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
    if (genericRolePattern.matches(csNorm)) return false
    if (blockedTokens.any { uidLower.contains(it) || cs.contains(it) || csNorm.contains(it) }) return false

    val avatarLower = avatarSource.trim().lowercase()
    if (avatarLower.isNotBlank() && stockAvatarHosts.any { avatarLower.contains(it) }) return false
    return true
  }

  fun isLikelyBotProfile(p: PublicPilotProfile): Boolean {
    val rawRig = p.vehicleSummary.substringBefore(" |").trim().lowercase()
    val isDefaultVehicle = rawRig in defaultBotVehicles
    val isRookie = p.rankBadge.trim().equals("Rookie Pilot", ignoreCase = true)
    if (!p.verified && p.ridesCount <= 0L && p.totalKm < 0.5) return true
    if (!p.verified && isRookie && isDefaultVehicle && p.ridesCount <= 1L && p.totalKm < 1.0) return true
    return false
  }

  fun isDisplayablePilotProfile(
    p: PublicPilotProfile,
    currentUid: String? = null,
    requireFirebaseUid: Boolean = false,
    maxStaleDays: Long = 30L,
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
    val hasRealActivity = p.verified || (p.ridesCount > 0L && p.totalKm >= 0.5)
    if (!hasRealActivity) return false

    val updatedSec = p.updatedAt?.seconds ?: p.createdAt?.seconds ?: 0L
    val maxStaleSec = maxStaleDays * 86400L
    if (requireFirebaseUid) {
      if (updatedSec <= 0L || (nowSec - updatedSec) > maxStaleSec) return false
    } else if (updatedSec > 0L && (nowSec - updatedSec) > maxStaleSec) {
      return false
    }
    return true
  }
}
