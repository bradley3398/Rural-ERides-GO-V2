package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.PevBatteryPhysics
import com.example.data.MasterSettings
import com.example.data.PevVehicle
import com.example.data.RideSession
import com.example.hardware.VoiceAudioManager
import com.example.ui.compartments.GraphInspectChartType
import com.example.ui.compartments.GraphInspectData
import com.example.ui.compartments.GraphInspectSlice
import com.example.ui.components.TacticalCard
import com.example.ui.components.formatDistValue
import com.example.ui.components.formatSpeedValue
import kotlin.math.max

/**
 * Crash-Proof Ride Inspector (focused single-module alternative to monolithic stacked Per-Ride Deep Dive).
 * Parses ride GPS polylines ONCE per ride and mounts only ONE lightweight module at a time,
 * eliminating multi-Canvas OOM/recomposition spikes and zero-width clip crashes.
 */
@Composable
internal fun RideInspectorDeck(
  ride: RideSession,
  activeVehicle: PevVehicle?,
  settings: MasterSettings,
  distUnit: String,
  spdUnit: String,
  elevUnit: String,
  onPlayCue: (VoiceAudioManager.TouchCue) -> Unit,
  onLaunchInspectModal: (GraphInspectData) -> Unit,
) {
  var activeModule by remember(ride.id) { mutableIntStateOf(0) }
  var pieSubMode by remember(ride.id) { mutableIntStateOf(0) }
  var replayProgress by remember(ride.id) { mutableFloatStateOf(0f) }

  val safeWh = ride.whUsed.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
  val safeDist = ride.distanceKm.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
  val safeMaxSpd = ride.maxSpeedKmh.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
  val safeAvgSpd = ride.avgSpeedKmh.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
  val safeElev = ride.elevationGainM.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
  val safeDurMin = (ride.durationSec.coerceAtLeast(0L) / 60L)
  val effVal =
    (if (settings.distanceUnit == "km") PevBatteryPhysics.whPerKm(safeWh, safeDist)
    else PevBatteryPhysics.whPerMile(safeWh, safeDist)).takeIf { it.isFinite() } ?: 0.0
  val startBatt = ride.startBatteryPct.coerceIn(0, 100)
  val endBatt = ride.endBatteryPct.coerceIn(0, 100)
  val battDelta = (startBatt - endBatt).coerceAtLeast(0)

  // Parse GPS polyline ONCE per ride (prevents 5x synchronous parsing on main thread)
  val parsedTrack = remember(ride.id, ride.polylinePoints.size) { parseRideTrackPoints(ride) }
  val gpsCoords = remember(parsedTrack) { parsedTrack.map { it.latitude to it.longitude } }
  val speedCurve = remember(ride.id, settings.distanceUnit, parsedTrack.size) {
    buildRideSpeedProfile(ride, settings.distanceUnit, preParsedPoints = parsedTrack)
      .map { v -> v.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0 }
  }
  val elevCurve = remember(ride.id, settings.distanceUnit, parsedTrack.size) {
    buildRideElevationProfile(ride, settings.distanceUnit, preParsedPoints = parsedTrack)
      .map { v -> v.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0 }
  }

  val nomV = (activeVehicle?.voltage ?: 52.0).takeIf { it.isFinite() && it > 10.0 } ?: 52.0
  val startV = PevBatteryPhysics.voltageForSoc(nomV, startBatt, settings.batteryChemistry)
  val endV = PevBatteryPhysics.voltageForSoc(nomV, endBatt, settings.batteryChemistry)
  val energySlices = remember(ride.id, safeWh, safeElev) {
    computeRideEnergySplit(ride, Color(0xFF6366F1), Color(0xFF38BDF8), Color(0xFFF59E0B))
  }

  val inspectorModules = remember {
    listOf(
      "Speed Curve" to Icons.AutoMirrored.Filled.ShowChart,
      "Climb Profile" to Icons.Default.Terrain,
      "Energy & Pack" to Icons.Default.PieChart,
      "GPS Route" to Icons.Default.Map,
      "Replay & AI" to Icons.Default.PlayArrow,
    )
  }

  Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.testTag("ride_inspector_deck")) {
    // 1. COMPACT RIDE TELEMETRY SCORECARD
    TacticalCard(highlight = true, title = "${ride.title.ifBlank { "Trail Ride" }} • ${ride.vehicleName.ifBlank { "PEV Rig" }}") {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          InspectorKpiTile("DISTANCE", "${"%.2f".format(formatDistValue(safeDist, settings.distanceUnit))} $distUnit", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
          InspectorKpiTile("PEAK", "${"%.1f".format(formatSpeedValue(safeMaxSpd, settings.distanceUnit))} $spdUnit", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
          InspectorKpiTile("AVG", "${"%.1f".format(formatSpeedValue(safeAvgSpd, settings.distanceUnit))} $spdUnit", Color(0xFF38BDF8), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          InspectorKpiTile("CLIMB", "+${formatElevationValue(safeElev, settings.distanceUnit).toInt()} $elevUnit", Color(0xFF22C55E), Modifier.weight(1f))
          InspectorKpiTile("EFFICIENCY", "${"%.1f".format(effVal)} Wh/$distUnit", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
          InspectorKpiTile("PACK", "$startBatt%→$endBatt% (-$battDelta%)", Color(0xFFF97316), Modifier.weight(1f))
        }
        Text(
          text = "Duration: ${safeDurMin}m • Energy: ${safeWh.toInt()} Wh • Voltage: ${"%.1f".format(startV)}V → ${"%.1f".format(endV)}V • GPS Pts: ${parsedTrack.size}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    // 2. FOCUSED 5-MODULE INSPECTOR SWITCHER (ONE LIGHTWEIGHT MODULE AT A TIME)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      itemsIndexed(inspectorModules, key = { idx, item -> "insp_mod_${idx}_${item.first}" }) { idx, (label, icon) ->
        FilterChip(
          selected = activeModule == idx,
          onClick = {
            onPlayCue(VoiceAudioManager.TouchCue.TAB_SWITCH)
            activeModule = idx
          },
          leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp)) },
          label = { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
          modifier = Modifier.heightIn(min = 48.dp).testTag("ride_inspector_module_$idx"),
        )
      }
    }

    // 3. ACTIVE INSPECTOR MODULE CARD
    when (activeModule) {
      0 -> {
        val peakVal = speedCurve.maxOrNull() ?: formatSpeedValue(safeMaxSpd, settings.distanceUnit)
        val avgVal = speedCurve.average().takeIf { it.isFinite() } ?: formatSpeedValue(safeAvgSpd, settings.distanceUnit)
        TacticalCard(title = "RIDE SPEED CURVE ($spdUnit)") {
          SingleCurveAreaChart(
            triggerKey = "${ride.id}_spd_${settings.distanceUnit}",
            values = speedCurve,
            metricLabel = "Peak ${"%.1f".format(peakVal)} $spdUnit • Avg ${"%.1f".format(avgVal)} $spdUnit",
            unitLabel = spdUnit,
            accentColor = MaterialTheme.colorScheme.primary,
            referenceLineValue = avgVal,
            referenceColor = MaterialTheme.colorScheme.secondary,
            reducedMotion = settings.reducedMotion,
            height = 205.dp,
          )
          Spacer(Modifier.height(10.dp))
          OutlinedButton(
            onClick = {
              onPlayCue(VoiceAudioManager.TouchCue.ACTION_CONFIRM)
              onLaunchInspectModal(
                GraphInspectData(
                  title = "${ride.title.ifBlank { "Ride" }} — Speed Curve",
                  subtitle = "${ride.vehicleName} • ${"%.2f".format(formatDistValue(safeDist, settings.distanceUnit))} $distUnit",
                  chartType = GraphInspectChartType.SPEED,
                  icon = Icons.AutoMirrored.Filled.ShowChart,
                  primaryColor = Color(0xFF6366F1),
                  unitLabel = spdUnit,
                  values = speedCurve,
                  peakMetric = "%.1f %s".format(peakVal, spdUnit),
                  avgMetric = "%.1f %s".format(avgVal, spdUnit),
                  referenceValue = avgVal,
                  gpsPoints = gpsCoords,
                ),
              )
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("inspector_open_speed_analytics"),
          ) {
            Icon(Icons.Default.OpenInFull, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Open Analytics Window (SMA / Trend / Z-Score)")
          }
        }
      }

      1 -> {
        val totalGain = formatElevationValue(safeElev, settings.distanceUnit)
        val maxElev = elevCurve.maxOrNull() ?: totalGain
        val climbRate = if (safeDist > 0.05) totalGain / formatDistValue(safeDist, settings.distanceUnit).coerceAtLeast(0.05) else 0.0
        TacticalCard(title = "ELEVATION & CLIMB PROFILE ($elevUnit)") {
          SingleCurveAreaChart(
            triggerKey = "${ride.id}_elev_${settings.distanceUnit}",
            values = elevCurve,
            metricLabel = "Total +${totalGain.toInt()} $elevUnit • Rate ${climbRate.toInt()} $elevUnit/$distUnit",
            unitLabel = elevUnit,
            accentColor = Color(0xFF38BDF8),
            referenceLineValue = null,
            referenceColor = Color(0xFF22C55E),
            reducedMotion = settings.reducedMotion,
            height = 205.dp,
          )
          Spacer(Modifier.height(10.dp))
          OutlinedButton(
            onClick = {
              onPlayCue(VoiceAudioManager.TouchCue.ACTION_CONFIRM)
              onLaunchInspectModal(
                GraphInspectData(
                  title = "${ride.title.ifBlank { "Ride" }} — Climb Profile",
                  subtitle = "Total Gain +${totalGain.toInt()} $elevUnit",
                  chartType = GraphInspectChartType.ELEVATION,
                  icon = Icons.Default.Terrain,
                  primaryColor = Color(0xFF38BDF8),
                  unitLabel = elevUnit,
                  values = elevCurve,
                  peakMetric = "+%.0f %s".format(maxElev, elevUnit),
                  totalMetric = "+%.0f %s".format(totalGain, elevUnit),
                  gpsPoints = gpsCoords,
                ),
              )
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("inspector_open_elev_analytics"),
          ) {
            Icon(Icons.Default.OpenInFull, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Open Analytics Window (Climb Slope & AI)")
          }
        }
      }

      2 -> {
        val battSlices = remember(startBatt, endBatt, battDelta) {
          val remaining = endBatt.coerceAtLeast(0).toDouble()
          val consumed = battDelta.toDouble().coerceAtLeast(1.0)
          val preUsed = (100 - startBatt).coerceAtLeast(0).toDouble()
          listOf(
            PieSliceData("Used This Ride", consumed, Color(0xFFEC4899)),
            PieSliceData("Pack Remaining", remaining, Color(0xFF22C55E)),
            PieSliceData("Used Before Ride", preUsed, Color(0xFF64748B)),
          ).filter { it.value > 0.0 }
        }
        val activeSlices = if (pieSubMode == 0) battSlices else energySlices
        val centerVal = if (pieSubMode == 0) "-$battDelta%" else "${safeWh.toInt()}"
        val centerLbl = if (pieSubMode == 0) "pack drain" else "Wh used"

        TacticalCard(title = "ENERGY & BATTERY PACK BREAKDOWN") {
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
              selected = pieSubMode == 0,
              onClick = { onPlayCue(VoiceAudioManager.TouchCue.TAB_SWITCH); pieSubMode = 0 },
              leadingIcon = { Icon(Icons.Default.ElectricBolt, null, Modifier.size(14.dp)) },
              label = { Text("Pack Drain %", style = MaterialTheme.typography.labelSmall) },
            )
            FilterChip(
              selected = pieSubMode == 1,
              onClick = { onPlayCue(VoiceAudioManager.TouchCue.TAB_SWITCH); pieSubMode = 1 },
              leadingIcon = { Icon(Icons.Default.PieChart, null, Modifier.size(14.dp)) },
              label = { Text("Physics Wh Split", style = MaterialTheme.typography.labelSmall) },
            )
          }
          Spacer(Modifier.height(6.dp))
          TelemetryDonutPieChart(
            slices = activeSlices,
            centerValue = centerVal,
            centerLabel = centerLbl,
            reducedMotion = settings.reducedMotion,
            chartSize = 158.dp,
          )
          Spacer(Modifier.height(10.dp))
          OutlinedButton(
            onClick = {
              onPlayCue(VoiceAudioManager.TouchCue.ACTION_CONFIRM)
              onLaunchInspectModal(
                GraphInspectData(
                  title = "${ride.title.ifBlank { "Ride" }} — Energy & Pack",
                  subtitle = "${"%.1f".format(startV)}V → ${"%.1f".format(endV)}V • ${safeWh.toInt()} Wh",
                  chartType = GraphInspectChartType.PIE_DONUT,
                  icon = Icons.Default.PieChart,
                  primaryColor = Color(0xFFEC4899),
                  unitLabel = centerLbl,
                  peakMetric = "$centerVal $centerLbl",
                  pieSlices = activeSlices.map { GraphInspectSlice(it.label, it.value, it.color) },
                  gpsPoints = gpsCoords,
                ),
              )
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("inspector_open_energy_analytics"),
          ) {
            Icon(Icons.Default.OpenInFull, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Inspect Energy Breakdown")
          }
        }
      }

      3 -> {
        TacticalCard(title = "INTERACTIVE GPS TRAIL ROUTE (${parsedTrack.size} PTS)") {
          ComposeVectorRideMap(
            ride = ride,
            allOverlayRides = emptyList(),
            settings = settings,
            height = 250.dp,
          )
        }
      }

      else -> {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          InteractiveRidePlaybackScrubber(
            ride = ride,
            activeVehicle = activeVehicle,
            settings = settings,
            externalProgress = replayProgress,
            onProgressChange = { replayProgress = it },
          )
          if (ride.aiDebrief.isNotBlank()) {
            TacticalCard(title = "RIDGE-TO-RIDGE AI DEBRIEF") {
              Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(ride.aiDebrief, style = MaterialTheme.typography.bodySmall)
              }
            }
          } else {
            TacticalCard(title = "RIDE AI DEBRIEF") {
              Text(
                "No AI debrief cached for this ride yet. Tap \"AI Debrief\" on this ride in the Trailhead Ride Logbook to run a live Gemini review.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun InspectorKpiTile(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.32f)),
    shape = RoundedCornerShape(10.dp),
    modifier = modifier,
  ) {
    Column(Modifier.padding(horizontal = 8.dp, vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
      Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
  }
}
