package com.example.data

import android.content.Context
import android.content.Intent
import kotlin.math.roundToInt

data class DecodedRigMatchSpec(
  val displayRigName: String,
  val paceTier: RiderPaceTier,
  val terrainPref: RiderTerrainPref,
  val usableRangeMi: Int,
  val scheduleVibe: RiderScheduleVibe,
  val zipOrTown: String = "",
)

data class RiderMatchScore(
  val scorePct: Int,
  val paceMatched: Boolean,
  val terrainMatched: Boolean,
  val rangeCompatible: Boolean,
  val vibeMatched: Boolean,
  val estimatedLiveRangeMi: Int,
  val tags: List<String>,
  val warningText: String = "",
)

object RiderMatchEngine {

  /**
   * Encodes active vehicle name + Rider Match specs strictly within <= 60 characters for
   * RadarRider.vehicleName, preserving 100% compatibility with Firestore schema rules and Supabase.
   */
  fun encodeRadarRigSpec(
    vehicleName: String,
    pace: RiderPaceTier,
    terrain: RiderTerrainPref,
    usableRangeMi: Int,
    vibe: RiderScheduleVibe,
  ): String {
    val cleanPace = if (pace == RiderPaceTier.ANY) RiderPaceTier.CRUISE else pace
    val cleanTerrain = if (terrain == RiderTerrainPref.ANY) RiderTerrainPref.MIXED else terrain
    val cleanVibe = if (vibe == RiderScheduleVibe.ANY) RiderScheduleVibe.GROUP_CONVOY else vibe
    val rng = usableRangeMi.coerceIn(5, 250)
    val suffix = " |${cleanPace.code}|${cleanTerrain.code}|$rng|${cleanVibe.code}"
    val cleanName = vehicleName.substringBefore(" |").trim().ifBlank { "PEV Rig" }
    val maxNameLen = (60 - suffix.length).coerceAtLeast(6)
    return cleanName.take(maxNameLen) + suffix
  }

  /**
   * Formats PublicPilotProfile.vehicleSummary (<= 100 chars) with human-readable rig + match tags.
   */
  fun encodePilotMatchSummary(
    vehicleName: String,
    batteryWh: Long,
    pace: RiderPaceTier,
    terrain: RiderTerrainPref,
    usableRangeMi: Int,
    vibe: RiderScheduleVibe,
    zipOrTown: String,
  ): String {
    val cleanPace = if (pace == RiderPaceTier.ANY) RiderPaceTier.CRUISE else pace
    val cleanTerrain = if (terrain == RiderTerrainPref.ANY) RiderTerrainPref.MIXED else terrain
    val cleanVibe = if (vibe == RiderScheduleVibe.ANY) RiderScheduleVibe.GROUP_CONVOY else vibe
    val zipClean = zipOrTown.trim().take(18)
    val suffix = buildString {
      append(" |${cleanPace.code}|${cleanTerrain.code}|${usableRangeMi.coerceIn(5, 250)}|${cleanVibe.code}")
      if (zipClean.isNotBlank()) append("|$zipClean")
    }
    val base = "${vehicleName.substringBefore(" |").trim().ifBlank { "PEV" }} (${batteryWh}Wh)"
    val maxBase = (100 - suffix.length).coerceAtLeast(10)
    return base.take(maxBase) + suffix
  }

  fun decodeRigMatchSpec(
    rawVehicleText: String,
    liveSpeedKmh: Double = 0.0,
    batteryPct: Long = 100L,
  ): DecodedRigMatchSpec {
    val idx = rawVehicleText.indexOf(" |")
    if (idx >= 0) {
      val display = rawVehicleText.substring(0, idx).trim().ifBlank { "PEV Rig" }
      val parts = rawVehicleText.substring(idx + 2).split("|")
      val pCode = parts.getOrNull(0).orEmpty()
      val tCode = parts.getOrNull(1).orEmpty()
      val rngVal = parts.getOrNull(2)?.toIntOrNull()
      val vCode = parts.getOrNull(3).orEmpty()
      val zipVal = parts.getOrNull(4).orEmpty()

      val pace = RiderPaceTier.entries.firstOrNull { it.code.equals(pCode, true) } ?: RiderPaceTier.CRUISE
      val terrain = RiderTerrainPref.entries.firstOrNull { it.code.equals(tCode, true) } ?: RiderTerrainPref.MIXED
      val vibe = RiderScheduleVibe.entries.firstOrNull { it.code.equals(vCode, true) } ?: RiderScheduleVibe.GROUP_CONVOY
      val baseRange = rngVal ?: inferRangeFromText(rawVehicleText)
      return DecodedRigMatchSpec(
        displayRigName = display,
        paceTier = pace,
        terrainPref = terrain,
        usableRangeMi = baseRange,
        scheduleVibe = vibe,
        zipOrTown = zipVal,
      )
    }

    val liveMph = liveSpeedKmh * 0.621371
    val inferredPace =
      when {
        liveMph >= 40.0 || rawVehicleText.contains("surron", true) || rawVehicleText.contains("emoto", true) || rawVehicleText.contains("72v", true) -> RiderPaceTier.HYPER
        liveMph >= 28.0 || rawVehicleText.contains("pro", true) || rawVehicleText.contains("gt", true) || rawVehicleText.contains("60v", true) -> RiderPaceTier.FAST
        liveMph >= 18.0 -> RiderPaceTier.CRUISE
        liveMph > 3.0 -> RiderPaceTier.CHILL
        else -> RiderPaceTier.CRUISE
      }
    val inferredTerrain =
      when {
        rawVehicleText.contains("dirt", true) || rawVehicleText.contains("surron", true) || rawVehicleText.contains("trail", true) -> RiderTerrainPref.SINGLETRACK
        rawVehicleText.contains("gravel", true) || rawVehicleText.contains("offroad", true) || rawVehicleText.contains("all-terrain", true) -> RiderTerrainPref.GRAVEL
        rawVehicleText.contains("road", true) || rawVehicleText.contains("street", true) || rawVehicleText.contains("skate", true) -> RiderTerrainPref.PAVED
        else -> RiderTerrainPref.MIXED
      }
    val inferredRange = inferRangeFromText(rawVehicleText)
    val liveRange = ((inferredRange * (batteryPct.coerceIn(5L, 100L) / 100.0)).roundToInt()).coerceAtLeast(3)
    return DecodedRigMatchSpec(
      displayRigName = rawVehicleText.ifBlank { "PEV Rig" },
      paceTier = inferredPace,
      terrainPref = inferredTerrain,
      usableRangeMi = liveRange,
      scheduleVibe = RiderScheduleVibe.GROUP_CONVOY,
      zipOrTown = "",
    )
  }

  private fun inferRangeFromText(text: String): Int {
    val whMatch = Regex("""(\d{3,5})\s*wh""", RegexOption.IGNORE_CASE).find(text)
    val wh = whMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull()
    if (wh != null && wh >= 200.0) {
      return (wh / 32.0).roundToInt().coerceIn(8, 160)
    }
    return when {
      text.contains("max", true) || text.contains("ultra", true) || text.contains("euc", true) -> 42
      text.contains("pro", true) || text.contains("surron", true) -> 32
      else -> 24
    }
  }

  fun estimateUsableRangeMiles(vehicle: PevVehicle?, batteryPct: Int): Int {
    val wh = (vehicle?.batteryWh ?: 1200L).toDouble().coerceAtLeast(250.0)
    val usableWh = wh * (batteryPct.coerceIn(5, 100) / 100.0)
    val whPerMile =
      when (vehicle?.category?.uppercase()) {
        "EMOTO", "SURRON", "EDIRTBIKE", "EATV" -> 42.0
        "EBIKE", "EFATBIKE", "ECARGO" -> 22.0
        "ESKATE", "ONEWHEEL" -> 26.0
        else -> 30.0
      }
    return (usableWh / whPerMile).roundToInt().coerceIn(4, 180)
  }

  fun scoreRiderMatch(
    mySettings: MasterSettings,
    peerRider: RadarRider,
    peerProfile: PublicPilotProfile? = null,
  ): RiderMatchScore {
    val rigSpec =
      if (peerRider.vehicleName.contains(" |")) {
        decodeRigMatchSpec(peerRider.vehicleName, peerRider.speedKmh, peerRider.batteryPct)
      } else if (peerProfile != null && peerProfile.vehicleSummary.contains(" |")) {
        decodeRigMatchSpec(peerProfile.vehicleSummary, peerRider.speedKmh, peerRider.batteryPct)
      } else {
        decodeRigMatchSpec(
          peerProfile?.vehicleSummary?.ifBlank { peerRider.vehicleName } ?: peerRider.vehicleName,
          peerRider.speedKmh,
          peerRider.batteryPct,
        )
      }
    val liveRangeMi =
      ((rigSpec.usableRangeMi * (peerRider.batteryPct.coerceIn(5L, 100L) / 100.0)).roundToInt())
        .coerceAtLeast(2)

    return computeCompatibilityScore(
      mySettings = mySettings,
      peerSpec = rigSpec,
      liveRangeMi = liveRangeMi,
      peerBatteryPct = peerRider.batteryPct.toInt(),
    )
  }

  fun scorePilotMatch(
    mySettings: MasterSettings,
    pilot: PublicPilotProfile,
  ): RiderMatchScore {
    val rigSpec = decodeRigMatchSpec(pilot.vehicleSummary, 0.0, 100L)
    return computeCompatibilityScore(
      mySettings = mySettings,
      peerSpec = rigSpec,
      liveRangeMi = rigSpec.usableRangeMi,
      peerBatteryPct = 100,
    )
  }

  private fun computeCompatibilityScore(
    mySettings: MasterSettings,
    peerSpec: DecodedRigMatchSpec,
    liveRangeMi: Int,
    peerBatteryPct: Int,
  ): RiderMatchScore {
    val myPace = if (mySettings.matchPaceTier == RiderPaceTier.ANY) RiderPaceTier.CRUISE else mySettings.matchPaceTier
    val myTerrain = if (mySettings.matchTerrainPref == RiderTerrainPref.ANY) RiderTerrainPref.MIXED else mySettings.matchTerrainPref
    val myVibe = if (mySettings.matchScheduleVibe == RiderScheduleVibe.ANY) RiderScheduleVibe.GROUP_CONVOY else mySettings.matchScheduleVibe
    val minReqRange = mySettings.matchMinRangeMi.coerceIn(5, 150)

    // 1. Pace Compatibility (30 pts)
    val paceOrdinalDiff = kotlin.math.abs(myPace.ordinal - peerSpec.paceTier.ordinal)
    val pacePts =
      when (paceOrdinalDiff) {
        0 -> 30
        1 -> 20
        2 -> 8
        else -> 0
      }
    val paceMatched = paceOrdinalDiff <= 1

    // 2. Terrain Compatibility (28 pts)
    val terrainMatched =
      myTerrain == peerSpec.terrainPref ||
        myTerrain == RiderTerrainPref.MIXED ||
        peerSpec.terrainPref == RiderTerrainPref.MIXED
    val terrainPts =
      when {
        myTerrain == peerSpec.terrainPref -> 28
        terrainMatched -> 20
        (myTerrain == RiderTerrainPref.GRAVEL && peerSpec.terrainPref == RiderTerrainPref.SINGLETRACK) ||
          (myTerrain == RiderTerrainPref.SINGLETRACK && peerSpec.terrainPref == RiderTerrainPref.GRAVEL) -> 22
        else -> 6
      }

    // 3. Usable Battery Range Compatibility (27 pts) — prevents mid-ride stranding
    val rangeCompatible = liveRangeMi >= minReqRange
    val rangePts =
      when {
        liveRangeMi >= minReqRange + 8 -> 27
        liveRangeMi >= minReqRange -> 23
        liveRangeMi >= (minReqRange * 0.75).roundToInt() -> 12
        else -> 3
      }

    // 4. Schedule / Vibe Compatibility (15 pts)
    val vibeMatched = myVibe == peerSpec.scheduleVibe || peerSpec.scheduleVibe == RiderScheduleVibe.GROUP_CONVOY
    val vibePts =
      when {
        myVibe == peerSpec.scheduleVibe -> 15
        vibeMatched -> 11
        else -> 5
      }

    val total = (pacePts + terrainPts + rangePts + vibePts).coerceIn(10, 100)
    val tags = buildList {
      add(peerSpec.paceTier.label)
      add(peerSpec.terrainPref.label)
      add("~${liveRangeMi}mi left")
      add(peerSpec.scheduleVibe.label)
    }
    val warning =
      when {
        peerBatteryPct <= 20 -> "Low pack ($peerBatteryPct% / ~${liveRangeMi}mi left)"
        !rangeCompatible -> "Range short (~${liveRangeMi}mi vs ${minReqRange}mi min)"
        else -> ""
      }

    return RiderMatchScore(
      scorePct = total,
      paceMatched = paceMatched,
      terrainMatched = terrainMatched,
      rangeCompatible = rangeCompatible,
      vibeMatched = vibeMatched,
      estimatedLiveRangeMi = liveRangeMi,
      tags = tags,
      warningText = warning,
    )
  }

  fun matchesActiveFilters(
    settings: MasterSettings,
    peerSpec: DecodedRigMatchSpec,
    score: RiderMatchScore,
  ): Boolean {
    if (settings.matchPaceFilter != RiderPaceTier.ANY && peerSpec.paceTier != settings.matchPaceFilter) {
      return false
    }
    if (settings.matchTerrainFilter != RiderTerrainPref.ANY && peerSpec.terrainPref != settings.matchTerrainFilter) {
      return false
    }
    if (settings.matchVibeFilter != RiderScheduleVibe.ANY && peerSpec.scheduleVibe != settings.matchVibeFilter) {
      return false
    }
    if (settings.matchMinScorePct > 0 && score.scorePct < settings.matchMinScorePct) {
      return false
    }
    return true
  }

  fun buildLocalRiderInviteMessage(
    callsign: String,
    sectorOrZip: String,
    vehicleName: String,
    pace: RiderPaceTier,
    terrain: RiderTerrainPref,
    usableRangeMi: Int,
    vibe: RiderScheduleVibe,
    lat: Double,
    lng: Double,
  ): String {
    val sectorLabel = sectorOrZip.trim().ifBlank { "our local area" }
    val pinLine =
      if (lat != 0.0 || lng != 0.0) {
        "\nStaging Pin: https://maps.google.com/?q=${"%.5f".format(lat)},${"%.5f".format(lng)}"
      } else ""
    return "Hey! Join me on Rural ERides GO so we can ride together around $sectorLabel.\n" +
      "Pilot: @$callsign • Rig: ${vehicleName.substringBefore(" |").ifBlank { "PEV" }}\n" +
      "Match Profile: ${pace.label} • ${terrain.label} • ~${usableRangeMi}mi range • ${vibe.label}" +
      pinLine +
      "\nSearch for my callsign @$callsign in Rider Radar or Porch Talk!"
  }

  fun launchShareInvite(
    context: Context,
    title: String,
    inviteText: String,
  ) {
    runCatching {
      context.startActivity(
        Intent.createChooser(
          Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, inviteText)
          },
          "Invite Local PEV Riders",
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
      )
    }
  }
}
