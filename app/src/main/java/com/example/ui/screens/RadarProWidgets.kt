package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.LiveTelemetrySnapshot
import com.example.data.MasterSettings
import com.example.data.PevVehicle
import com.example.data.RadarHazard
import com.example.data.RadarMeetup
import com.example.data.RadarRider
import com.example.data.RadarWaypoint
import com.example.data.RiderMatchEngine
import com.example.data.RiderPaceTier
import com.example.data.RiderScheduleVibe
import com.example.data.RiderTerrainPref
import com.example.data.WeatherMatrixData
import com.example.ui.components.TacticalCard
import com.example.ui.components.distUnitLabel
import com.example.ui.components.formatDistValue
import com.example.ui.components.formatSpeedValue
import com.example.ui.components.speedUnitLabel
import com.example.viewmodel.RadarViewModel
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor

data class RadarLayerVisibility(
  val showRiders: Boolean = true,
  val showMeetups: Boolean = true,
  val showHazards: Boolean = true,
  val showWaypoints: Boolean = true,
  val showBreadcrumb: Boolean = true,
  val showRangeRing: Boolean = true,
  val trackUpOrientation: Boolean = false,
)

enum class RadarRiderSort(val label: String) {
  BEST_MATCH("Best Match"),
  NEAREST("Nearest"),
  FASTEST("Fastest"),
  LOWEST_BATTERY("Low Bat"),
}

fun headingToCardinal(deg: Float): String {
  val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
  val idx = (((deg % 360f) + 360f) % 360f / 45f + 0.5f).toInt() % 8
  return dirs[idx]
}

fun formatDmsCoord(value: Double, isLat: Boolean): String {
  val hemi = if (isLat) (if (value >= 0) "N" else "S") else (if (value >= 0) "E" else "W")
  val absVal = abs(value)
  val d = floor(absVal).toInt()
  val minFull = (absVal - d) * 60.0
  val m = floor(minFull).toInt()
  val s = (minFull - m) * 60.0
  return "%d°%02d'%04.1f\"%s".format(d, m, s, hemi)
}

fun launchExternalNavigation(context: Context, lat: Double, lng: Double, label: String) {
  runCatching {
    val uri = Uri.parse("google.navigation:q=$lat,$lng")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    context.startActivity(intent)
  }.onFailure {
    runCatching {
      val geo = Uri.parse("https://maps.google.com/?q=$lat,$lng(${Uri.encode(label)})")
      context.startActivity(Intent(Intent.ACTION_VIEW, geo).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }
  }
}

@Composable
fun RadarProControlDeck(
  telemetry: LiveTelemetrySnapshot,
  activeVehicle: PevVehicle?,
  settings: MasterSettings,
  weather: WeatherMatrixData?,
  nearbyRiders: List<RadarRider>,
  meetups: List<RadarMeetup>,
  hazards: List<RadarHazard>,
  waypoints: List<RadarWaypoint>,
  layers: RadarLayerVisibility,
  riderSort: RadarRiderSort,
  hasGpsFix: Boolean,
  callsign: String,
  onLayersChange: (RadarLayerVisibility) -> Unit,
  onSectorRadiusChange: (Int) -> Unit,
  onRiderSortChange: (RadarRiderSort) -> Unit,
  onQuickDropHazard: (hazardType: String, description: String) -> Unit,
  onQuickDropWaypoint: (waypointType: String, title: String) -> Unit,
  onFlyToPoint: (lat: Double, lng: Double, label: String) -> Unit,
) {
  val ctx = LocalContext.current
  var expandedTools by remember { mutableStateOf(true) }
  var actionToast by remember { mutableStateOf("") }

  // Compute live Convoy & Range telemetry
  val packWh = (activeVehicle?.batteryWh ?: 1500L).toDouble().coerceAtLeast(300.0)
  val estRangeKm = ((packWh * (telemetry.batteryPct / 100.0)) / 24.0).coerceIn(1.0, 180.0)
  val avgPackSpeedKmh = remember(nearbyRiders) { if (nearbyRiders.isNotEmpty()) nearbyRiders.map { it.speedKmh }.average() else 0.0 }
  val lowestBatRider = remember(nearbyRiders) { nearbyRiders.minByOrNull { it.batteryPct } }
  val sosCount = remember(nearbyRiders) { nearbyRiders.count { it.statusTag == "SOS" } }
  val convoySpreadKm =
    remember(nearbyRiders, telemetry.lat, telemetry.lng, hasGpsFix) {
      if (!hasGpsFix || nearbyRiders.isEmpty()) 0.0
      else nearbyRiders.maxOfOrNull { RadarViewModel.haversineKm(telemetry.lat, telemetry.lng, it.lat, it.lng) } ?: 0.0
    }

  // Headwind / Tailwind calculation from live weather windDirDeg vs rider bearingDeg
  val windRelativeText =
    remember(weather, telemetry.bearingDeg, settings.distanceUnit) {
      if (weather == null) {
        "Wind: Syncing"
      } else {
        val diffRad = Math.toRadians(((weather.windDirDeg - telemetry.bearingDeg + 540.0) % 360.0) - 180.0)
        val componentKmh = weather.windSpeedKmh * cos(diffRad)
        val spd = formatSpeedValue(abs(componentKmh), settings.distanceUnit)
        val u = speedUnitLabel(settings.distanceUnit)
        when {
          componentKmh > 3.5 -> "Headwind ${"%.0f".format(spd)} $u"
          componentKmh < -3.5 -> "Tailwind +${"%.0f".format(spd)} $u"
          else -> "Crosswind ${"%.0f".format(formatSpeedValue(weather.windSpeedKmh, settings.distanceUnit))} $u"
        }
      }
    }

  val nearestCharger =
    remember(waypoints, telemetry.lat, telemetry.lng, hasGpsFix) {
      if (!hasGpsFix) waypoints.firstOrNull { it.waypointType == "CHARGER" }
      else waypoints.filter { it.waypointType == "CHARGER" }.minByOrNull {
        RadarViewModel.haversineKm(telemetry.lat, telemetry.lng, it.lat, it.lng)
      }
    }
  val nearestRepair =
    remember(waypoints, telemetry.lat, telemetry.lng, hasGpsFix) {
      if (!hasGpsFix) waypoints.firstOrNull { it.waypointType == "REPAIR" }
      else waypoints.filter { it.waypointType == "REPAIR" }.minByOrNull {
        RadarViewModel.haversineKm(telemetry.lat, telemetry.lng, it.lat, it.lng)
      }
    }
  val nearestWater =
    remember(waypoints, telemetry.lat, telemetry.lng, hasGpsFix) {
      if (!hasGpsFix) waypoints.firstOrNull { it.waypointType == "WATER" }
      else waypoints.filter { it.waypointType == "WATER" }.minByOrNull {
        RadarViewModel.haversineKm(telemetry.lat, telemetry.lng, it.lat, it.lng)
      }
    }

  // Live Radar Sweep Angle animation (real tactical scope)
  var radarSweepDeg by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
  androidx.compose.runtime.LaunchedEffect(Unit) {
    var lastNs = 0L
    while (true) {
      androidx.compose.runtime.withFrameNanos { ns ->
        if (lastNs != 0L) {
          val dt = ((ns - lastNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
          radarSweepDeg = (radarSweepDeg + dt * 78f) % 360f
        }
        lastNs = ns
      }
    }
  }

  TacticalCard(title = "Radar Pro Scope & Tools", modifier = Modifier.testTag("radar_pro_suite_card")) {
    // 0. ANIMATED LIVE TACTICAL RADAR SWEEP SCOPE (100% REAL FIRESTORE & GPS BEARINGS)
    val radarScopeRadiusKm = (settings.radarRadiusMiles.coerceIn(1, 100) * 1.60934)
    val primaryCol = MaterialTheme.colorScheme.primary
    val errorCol = MaterialTheme.colorScheme.error
    val secondaryCol = MaterialTheme.colorScheme.secondary
    val tertiaryCol = MaterialTheme.colorScheme.tertiary
    Surface(
      color = androidx.compose.ui.graphics.Color(0xFF040B14),
      border = BorderStroke(1.dp, primaryCol.copy(alpha = 0.45f)),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("radar_tactical_scope_canvas"),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(116.dp)) {
          val r = size.minDimension * 0.46f
          val center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.5f)
          // Concentric range rings
          listOf(0.33f, 0.66f, 1.0f).forEach { frac ->
            drawCircle(
              color = primaryCol.copy(alpha = 0.28f),
              radius = r * frac,
              center = center,
              style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f),
            )
          }
          // Crosshairs
          drawLine(primaryCol.copy(alpha = 0.22f), androidx.compose.ui.geometry.Offset(center.x - r, center.y), androidx.compose.ui.geometry.Offset(center.x + r, center.y), strokeWidth = 1.2f)
          drawLine(primaryCol.copy(alpha = 0.22f), androidx.compose.ui.geometry.Offset(center.x, center.y - r), androidx.compose.ui.geometry.Offset(center.x, center.y + r), strokeWidth = 1.2f)
          // Rotating Phosphor Sweep Sector
          drawArc(
            brush = androidx.compose.ui.graphics.Brush.sweepGradient(
              listOf(androidx.compose.ui.graphics.Color.Transparent, primaryCol.copy(alpha = 0.35f)),
              center = center,
            ),
            startAngle = radarSweepDeg - 45f,
            sweepAngle = 45f,
            useCenter = true,
            topLeft = androidx.compose.ui.geometry.Offset(center.x - r, center.y - r),
            size = androidx.compose.ui.geometry.Size(r * 2f, r * 2f),
          )
          // Heading Needle
          val hRad = Math.toRadians((telemetry.bearingDeg - 90f).toDouble())
          drawLine(
            color = androidx.compose.ui.graphics.Color(0xFF38BDF8),
            start = center,
            end = androidx.compose.ui.geometry.Offset(center.x + (r * 0.38f * kotlin.math.cos(hRad)).toFloat(), center.y + (r * 0.38f * kotlin.math.sin(hRad)).toFloat()),
            strokeWidth = 3f,
          )
          // Plot Real Targets when GPS locked
          if (hasGpsFix) {
            fun plotPin(lat: Double, lng: Double, pinColor: androidx.compose.ui.graphics.Color, dotR: Float) {
              val dKm = RadarViewModel.haversineKm(telemetry.lat, telemetry.lng, lat, lng)
              val brgRad = Math.toRadians(RadarViewModel.bearingDeg(telemetry.lat, telemetry.lng, lat, lng) - 90.0)
              val norm = (dKm / radarScopeRadiusKm).toFloat().coerceIn(0.05f, 0.96f)
              val px = center.x + (r * norm * kotlin.math.cos(brgRad)).toFloat()
              val py = center.y + (r * norm * kotlin.math.sin(brgRad)).toFloat()
              drawCircle(pinColor, radius = dotR, center = androidx.compose.ui.geometry.Offset(px, py))
            }
            waypoints.forEach { w -> plotPin(w.lat, w.lng, tertiaryCol, 3.8f) }
            hazards.forEach { h -> plotPin(h.lat, h.lng, errorCol, 4.5f) }
            nearbyRiders.forEach { rdr -> plotPin(rdr.lat, rdr.lng, if (rdr.statusTag == "SOS") errorCol else primaryCol, 5.2f) }
          }
          // Self center reticle
          drawCircle(androidx.compose.ui.graphics.Color.White, radius = 4.5f, center = center)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(
            "SECTOR SCOPE (${settings.radarRadiusMiles} MI)",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = primaryCol,
          )
          Text(
            "● ${nearbyRiders.size} Live Riders  ▲ ${hazards.size} Hazards",
            style = MaterialTheme.typography.labelSmall,
            color = androidx.compose.ui.graphics.Color.White,
          )
          Text(
            "◆ ${waypoints.size} Waypoints  ★ ${meetups.size} Meetups",
            style = MaterialTheme.typography.labelSmall,
            color = secondaryCol,
          )
          val closestHaz = remember(hazards, telemetry.lat, telemetry.lng, hasGpsFix) {
            if (!hasGpsFix) null
            else hazards.minByOrNull { RadarViewModel.haversineKm(telemetry.lat, telemetry.lng, it.lat, it.lng) }
          }
          if (closestHaz != null && hasGpsFix) {
            val hKm = RadarViewModel.haversineKm(telemetry.lat, telemetry.lng, closestHaz.lat, closestHaz.lng)
            val hBrg = RadarViewModel.bearingDeg(telemetry.lat, telemetry.lng, closestHaz.lat, closestHaz.lng).toFloat()
            Text(
              "Nearest Hazard: ${closestHaz.hazardType} @ ${"%.1f".format(formatDistValue(hKm, settings.distanceUnit))}${distUnitLabel(settings.distanceUnit)} ${headingToCardinal(hBrg)}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = errorCol,
            )
          } else {
            Text(
              if (hasGpsFix) "Sector sweep clear of nearby hazards" else "Acquiring GPS for vector plot...",
              style = MaterialTheme.typography.labelSmall,
              color = androidx.compose.ui.graphics.Color(0xFF94A3B8),
            )
          }
        }
      }
    }

    // 1. LIVE COMPASS HEADING + WIND VECTOR + PACK RANGE HUD
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Default.Explore, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(
          text = "HDG ${telemetry.bearingDeg.toInt()}° ${headingToCardinal(telemetry.bearingDeg)} • Alt ${telemetry.altitudeM.toInt()}m",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
        )
      }
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(Icons.Default.Air, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
        Text(windRelativeText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
      }
    }

    Spacer(Modifier.height(6.dp))

    // 2. CONVOY TELEMETRY ANALYTICS STRIP (Avg Speed, Spread, Low-Bat Pilot, SOS, Est Range)
    Surface(
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(
            "Pack Avg: ${"%.1f".format(formatSpeedValue(avgPackSpeedKmh, settings.distanceUnit))} ${speedUnitLabel(settings.distanceUnit)}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            "Spread: ${"%.1f".format(formatDistValue(convoySpreadKm, settings.distanceUnit))} ${distUnitLabel(settings.distanceUnit)}",
            style = MaterialTheme.typography.labelSmall,
          )
          Text(
            "Range Ring: ${"%.0f".format(formatDistValue(estRangeKm, settings.distanceUnit))}${distUnitLabel(settings.distanceUnit)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
          )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(
            text =
              if (lowestBatRider != null) "Lowest Pack: ${lowestBatRider.callsign} (${lowestBatRider.batteryPct}%)"
              else "Convoy Standby (${nearbyRiders.size} active)",
            style = MaterialTheme.typography.labelSmall,
            color = if ((lowestBatRider?.batteryPct ?: 100L) <= 25L) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "SOS: $sosCount • Pins: ${meetups.size + hazards.size + waypoints.size}",
            style = MaterialTheme.typography.labelSmall,
            color = if (sosCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
          )
        }
      }
    }

    if (lowestBatRider != null && lowestBatRider.batteryPct <= 25L) {
      val lowSpec = RiderMatchEngine.decodeRigMatchSpec(lowestBatRider.vehicleName, lowestBatRider.speedKmh, lowestBatRider.batteryPct)
      val estLeftMi = ((lowSpec.usableRangeMi * (lowestBatRider.batteryPct.coerceIn(5L, 100L) / 100.0))).toInt().coerceAtLeast(1)
      Spacer(Modifier.height(6.dp))
      Surface(
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().testTag("convoy_low_pack_sweep_alert"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            Text(
              "SWEEP ALERT: ${lowestBatRider.callsign} at ${lowestBatRider.batteryPct}% (~${estLeftMi}mi left)",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.error,
            )
          }
          TextButton(
            onClick = { onFlyToPoint(lowestBatRider.lat, lowestBatRider.lng, lowestBatRider.callsign) },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          ) { Text("Locate", style = MaterialTheme.typography.labelSmall) }
        }
      }
    }

    Spacer(Modifier.height(8.dp))

    // 3. MAP OVERLAY VISIBILITY & TRACK-UP ORIENTATION TOGGLES
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      Icon(Icons.Default.Layers, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
      Text("MAP LAYERS & ORIENTATION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
      item {
        FilterChip(
          selected = layers.trackUpOrientation,
          onClick = { onLayersChange(layers.copy(trackUpOrientation = !layers.trackUpOrientation)) },
          leadingIcon = { Icon(Icons.Default.Navigation, null, Modifier.size(14.dp)) },
          label = { Text(if (layers.trackUpOrientation) "Track-Up" else "North-Up") },
        )
      }
      item {
        FilterChip(
          selected = layers.showRangeRing,
          onClick = { onLayersChange(layers.copy(showRangeRing = !layers.showRangeRing)) },
          leadingIcon = { Icon(Icons.Default.Radar, null, Modifier.size(14.dp)) },
          label = { Text("Range Ring") },
        )
      }
      item {
        FilterChip(
          selected = layers.showBreadcrumb,
          onClick = { onLayersChange(layers.copy(showBreadcrumb = !layers.showBreadcrumb)) },
          leadingIcon = { Icon(Icons.Default.Route, null, Modifier.size(14.dp)) },
          label = { Text("Trail Line") },
        )
      }
      item {
        FilterChip(
          selected = layers.showRiders,
          onClick = { onLayersChange(layers.copy(showRiders = !layers.showRiders)) },
          label = { Text("Riders (${nearbyRiders.size})") },
        )
      }
      item {
        FilterChip(
          selected = layers.showHazards,
          onClick = { onLayersChange(layers.copy(showHazards = !layers.showHazards)) },
          label = { Text("Hazards (${hazards.size})") },
        )
      }
      item {
        FilterChip(
          selected = layers.showWaypoints,
          onClick = { onLayersChange(layers.copy(showWaypoints = !layers.showWaypoints)) },
          label = { Text("Waypoints (${waypoints.size})") },
        )
      }
      item {
        FilterChip(
          selected = layers.showMeetups,
          onClick = { onLayersChange(layers.copy(showMeetups = !layers.showMeetups)) },
          label = { Text("Meetups (${meetups.size})") },
        )
      }
    }

    // 4. SECTOR RADIUS QUICK-SELECT + PILOT SORT
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
      items(listOf(5, 15, 50, 100, 500)) { miles ->
        FilterChip(
          selected = settings.radarRadiusMiles == miles,
          onClick = { onSectorRadiusChange(miles) },
          label = { Text(if (miles >= 500) "Global" else "${miles}mi") },
        )
      }
      items(RadarRiderSort.entries) { sort ->
        FilterChip(
          selected = riderSort == sort,
          onClick = { onRiderSortChange(sort) },
          leadingIcon = { Icon(Icons.Default.Groups, null, Modifier.size(14.dp)) },
          label = { Text("Sort: ${sort.label}") },
        )
      }
    }

    Spacer(Modifier.height(6.dp))

    // 5. NEAREST SUPPORT JUMP BAR (CHARGER / REPAIR / WATER) + TURN-BY-TURN NAV
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      nearestCharger?.let { wp ->
        item {
          AssistChip(
            onClick = {
              onFlyToPoint(wp.lat, wp.lng, wp.title)
              actionToast = "Centered on charger: ${wp.title}"
            },
            leadingIcon = { Icon(Icons.Default.BatteryChargingFull, null, Modifier.size(15.dp)) },
            label = { Text("Jump Charger") },
          )
        }
        item {
          AssistChip(
            onClick = { launchExternalNavigation(ctx, wp.lat, wp.lng, wp.title) },
            leadingIcon = { Icon(Icons.Default.Navigation, null, Modifier.size(15.dp)) },
            label = { Text("Nav Charger") },
          )
        }
      }
      nearestRepair?.let { wp ->
        item {
          AssistChip(
            onClick = {
              onFlyToPoint(wp.lat, wp.lng, wp.title)
              actionToast = "Centered on repair spot: ${wp.title}"
            },
            leadingIcon = { Icon(Icons.Default.Build, null, Modifier.size(15.dp)) },
            label = { Text("Jump Repair") },
          )
        }
      }
      nearestWater?.let { wp ->
        item {
          AssistChip(
            onClick = {
              onFlyToPoint(wp.lat, wp.lng, wp.title)
              actionToast = "Centered on water stop: ${wp.title}"
            },
            leadingIcon = { Icon(Icons.Default.WaterDrop, null, Modifier.size(15.dp)) },
            label = { Text("Jump Water") },
          )
        }
      }
      if (hasGpsFix) {
        item {
          AssistChip(
            onClick = {
              val dms = "${formatDmsCoord(telemetry.lat, true)} ${formatDmsCoord(telemetry.lng, false)}"
              val dec = "${"%.6f".format(telemetry.lat)}, ${"%.6f".format(telemetry.lng)}"
              val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
              cm?.setPrimaryClip(ClipData.newPlainText("PEV Grid Coords", "$callsign Grid: $dec | $dms | Alt ${telemetry.altitudeM.toInt()}m"))
              actionToast = "Copied Grid DMS & Decimal Coords"
            },
            leadingIcon = { Icon(Icons.Default.ContentCopy, null, Modifier.size(15.dp)) },
            label = { Text("Copy Grid DMS") },
          )
        }
      }
      item {
        AssistChip(
          onClick = { expandedTools = !expandedTools },
          label = { Text(if (expandedTools) "Hide Quick-Drops" else "Show Quick-Drops") },
        )
      }
    }

    if (expandedTools) {
      Spacer(Modifier.height(6.dp))
      // 6. 1-TAP INSTANT HAZARD HOTBAR (6 LIVE HAZARD TYPES)
      Text("1-TAP INSTANT HAZARD DROP (LIVE GPS)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 3.dp)) {
        val quickHazards =
          listOf(
            "POTHOLE" to "Deep Pothole Ahead",
            "GRAVEL" to "Loose Gravel / Sand",
            "POLICE" to "Speed Trap / Patrol",
            "CLOSURE" to "Trail / Road Closed",
            "DOG" to "Loose Dog Warning",
            "FLOOD" to "Standing Water / Mud",
          )
        items(quickHazards) { (type, desc) ->
          AssistChip(
            onClick = {
              if (!hasGpsFix) {
                actionToast = "Waiting for GPS lock before dropping $type pin."
              } else {
                onQuickDropHazard(type, desc)
                actionToast = "Dropped live $type hazard pin!"
              }
            },
            leadingIcon = { Icon(Icons.Default.Warning, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error) },
            label = { Text(type) },
          )
        }
      }

      // 7. 1-TAP INSTANT WAYPOINT HOTBAR (5 LIVE WAYPOINT TYPES)
      Text("1-TAP INSTANT WAYPOINT DROP (LIVE GPS)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 3.dp)) {
        val quickWaypoints =
          listOf(
            "CHARGER" to "Public Outlet / Charger",
            "REPAIR" to "Air Pump / Tool Station",
            "WATER" to "Water Refill / Restroom",
            "SCENIC" to "Scenic Overlook Stop",
            "MEETSPOT" to "Pack Rally Staging Spot",
          )
        items(quickWaypoints) { (type, title) ->
          AssistChip(
            onClick = {
              if (!hasGpsFix) {
                actionToast = "Waiting for GPS lock before dropping $type pin."
              } else {
                onQuickDropWaypoint(type, title)
                actionToast = "Pinned live $type waypoint!"
              }
            },
            leadingIcon = { Icon(Icons.Default.Place, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary) },
            label = { Text(type) },
          )
        }
      }

      if (hasGpsFix) {
        Text(
          text = "DMS Grid: ${formatDmsCoord(telemetry.lat, true)}  ${formatDmsCoord(telemetry.lng, false)} • Acc ±${telemetry.gpsAccuracyM.toInt()}m",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    if (actionToast.isNotBlank()) {
      Spacer(Modifier.height(4.dp))
      Text(actionToast, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
  }
}

internal fun parseSunsetMinutesRemaining(
  sunsetStr: String,
  nowLocal: LocalTime = LocalTime.now(),
): Int? {
  val trimmed = sunsetStr.trim()
  if (trimmed.isBlank() || trimmed == "--:--") return null
  val timePart = if (trimmed.contains('T')) trimmed.substringAfter('T').take(5) else trimmed
  val patterns = listOf("H:mm", "HH:mm", "HH:mm:ss", "h:mm a", "hh:mm a", "h:mma")
  var parsed: LocalTime? = null
  for (p in patterns) {
    parsed = runCatching { LocalTime.parse(timePart.uppercase(Locale.US), DateTimeFormatter.ofPattern(p, Locale.US)) }.getOrNull()
    if (parsed != null) break
  }
  if (parsed == null) return null
  return Duration.between(nowLocal, parsed).toMinutes().toInt()
}

@Composable
fun RiderMatchFilterDeck(
  settings: MasterSettings,
  totalPilotsCount: Int,
  matchedPilotsCount: Int,
  onUpdateMatchSettings: ((MasterSettings) -> MasterSettings) -> Unit,
) {
  var editingMyProfile by remember { mutableStateOf(false) }
  var zipDraft by remember(settings.matchHomeZipOrTown) { mutableStateOf(settings.matchHomeZipOrTown) }
  var minRangeDraft by remember(settings.matchMinRangeMi) { mutableIntStateOf(settings.matchMinRangeMi) }

  TacticalCard(title = "Rider Match ($matchedPilotsCount/$totalPilotsCount Compatible)", modifier = Modifier.testTag("rider_match_filter_deck")) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        "My Spec: ${settings.matchPaceTier.label} • ${settings.matchTerrainPref.label} • Min ${settings.matchMinRangeMi}mi",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.weight(1f),
      )
      OutlinedButton(
        onClick = { editingMyProfile = !editingMyProfile },
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.testTag("rider_match_edit_profile_button"),
      ) {
        Icon(Icons.Default.Tune, null, Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(if (editingMyProfile) "Done" else "My Profile", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
      }
    }

    Spacer(Modifier.height(6.dp))
    // Pace Filter Row
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(RiderPaceTier.entries) { tier ->
        FilterChip(
          selected = settings.matchPaceFilter == tier,
          onClick = { onUpdateMatchSettings { it.copy(matchPaceFilter = tier) } },
          label = { Text(if (tier == RiderPaceTier.ANY) "Pace: Any" else tier.label, maxLines = 1, softWrap = false) },
        )
      }
    }
    // Terrain Filter Row
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(RiderTerrainPref.entries) { terr ->
        FilterChip(
          selected = settings.matchTerrainFilter == terr,
          onClick = { onUpdateMatchSettings { it.copy(matchTerrainFilter = terr) } },
          label = { Text(if (terr == RiderTerrainPref.ANY) "Terrain: Any" else terr.label, maxLines = 1, softWrap = false) },
        )
      }
    }
    // Vibe & Min Match % Row
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(listOf(0 to "Match: Any", 50 to "50%+", 70 to "70%+", 85 to "85%+")) { (pct, lbl) ->
        FilterChip(
          selected = settings.matchMinScorePct == pct,
          onClick = { onUpdateMatchSettings { it.copy(matchMinScorePct = pct) } },
          label = { Text(lbl, maxLines = 1, softWrap = false) },
        )
      }
      items(RiderScheduleVibe.entries) { vibe ->
        FilterChip(
          selected = settings.matchVibeFilter == vibe,
          onClick = { onUpdateMatchSettings { it.copy(matchVibeFilter = vibe) } },
          label = { Text(if (vibe == RiderScheduleVibe.ANY) "Vibe: Any" else vibe.label, maxLines = 1, softWrap = false) },
        )
      }
    }

    AnimatedVisibility(visible = editingMyProfile) {
      Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("MY CRUISING PACE TIER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(RiderPaceTier.entries.filter { it != RiderPaceTier.ANY }) { p ->
            FilterChip(
              selected = settings.matchPaceTier == p,
              onClick = { onUpdateMatchSettings { it.copy(matchPaceTier = p) } },
              label = { Text(p.label, maxLines = 1, softWrap = false) },
            )
          }
        }
        Text("MY PREFERRED TERRAIN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(RiderTerrainPref.entries.filter { it != RiderTerrainPref.ANY }) { t ->
            FilterChip(
              selected = settings.matchTerrainPref == t,
              onClick = { onUpdateMatchSettings { it.copy(matchTerrainPref = t) } },
              label = { Text(t.label, maxLines = 1, softWrap = false) },
            )
          }
        }
        Text("MY SCHEDULE & VIBE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(RiderScheduleVibe.entries.filter { it != RiderScheduleVibe.ANY }) { v ->
            FilterChip(
              selected = settings.matchScheduleVibe == v,
              onClick = { onUpdateMatchSettings { it.copy(matchScheduleVibe = v) } },
              label = { Text(v.label, maxLines = 1, softWrap = false) },
            )
          }
        }
        Text("Minimum Safe Group Range: $minRangeDraft miles", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Slider(
          value = minRangeDraft.toFloat(),
          onValueChange = { minRangeDraft = it.toInt() },
          onValueChangeFinished = { onUpdateMatchSettings { it.copy(matchMinRangeMi = minRangeDraft) } },
          valueRange = 5f..80f,
        )
        OutlinedTextField(
          value = zipDraft,
          onValueChange = { zipDraft = it },
          label = { Text("Home ZIP Code or Rural Town") },
          singleLine = true,
          trailingIcon = {
            TextButton(onClick = { onUpdateMatchSettings { it.copy(matchHomeZipOrTown = zipDraft.trim()) } }) {
              Text("Save")
            }
          },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }
}

@Composable
fun RuralBailoutAndDaylightCard(
  telemetry: LiveTelemetrySnapshot,
  activeVehicle: PevVehicle?,
  settings: MasterSettings,
  weather: WeatherMatrixData?,
  hasGpsFix: Boolean,
  onRouteHome: (lat: Double, lng: Double) -> Unit,
) {
  val lLat = telemetry.launchLat
  val lLng = telemetry.launchLng
  val hasLaunch = hasGpsFix && lLat != null && lLng != null && (lLat != 0.0 || lLng != 0.0)
  val rtbDistKm = if (hasLaunch) RadarViewModel.haversineKm(telemetry.lat, telemetry.lng, lLat!!, lLng!!) else 0.0
  val rtbBearing = if (hasLaunch) RadarViewModel.bearingDeg(telemetry.lat, telemetry.lng, lLat!!, lLng!!).toInt() else 0

  val packWh = (activeVehicle?.batteryWh ?: 1500L).toDouble().coerceAtLeast(300.0)
  val windPenalty =
    weather?.let { w ->
      val diffRad = Math.toRadians(((w.windDirDeg - rtbBearing + 540.0) % 360.0) - 180.0)
      val headKmh = (w.windSpeedKmh * cos(diffRad)).coerceAtLeast(0.0)
      1.0 + (headKmh / 65.0)
    } ?: 1.0
  val estRemKm = (((packWh * (telemetry.batteryPct / 100.0)) / 24.0) / windPenalty).coerceAtLeast(0.0)
  val marginLabel =
    when {
      !hasLaunch -> "AWAITING LAUNCH"
      estRemKm >= rtbDistKm * 1.35 -> "SAFE RETURN"
      estRemKm >= rtbDistKm * 1.05 -> "TIGHT MARGIN"
      else -> "TURN BACK NOW"
    }
  val marginError = marginLabel == "TURN BACK NOW"
  val sunsetMin = remember(weather?.sunset) { parseSunsetMinutesRemaining(weather?.sunset.orEmpty()) }

  TacticalCard(highlight = marginError, title = "Return-to-Base & Daylight") {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text =
            if (hasLaunch) "RTB Launch: ${"%.1f".format(formatDistValue(rtbDistKm, settings.distanceUnit))}${distUnitLabel(settings.distanceUnit)} @ $rtbBearing° ($marginLabel)"
            else "Launch anchor sets automatically on first GPS move",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = if (marginError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
        Text(
          text = "Wind-Adjusted Rem: ~${"%.1f".format(formatDistValue(estRemKm, settings.distanceUnit))}${distUnitLabel(settings.distanceUnit)} (${telemetry.batteryPct}%)",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (hasLaunch) {
        OutlinedButton(
          onClick = { onRouteHome(lLat!!, lLng!!) },
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier.testTag("radar_rtb_route_home_button"),
        ) {
          Icon(Icons.Default.Home, null, Modifier.size(15.dp))
          Spacer(Modifier.width(4.dp))
          Text("RTB Route", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
        }
      }
    }

    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      Icon(Icons.Default.WbTwilight, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
      val daylightTxt =
        when {
          sunsetMin == null -> "Sunset: ${weather?.sunset?.ifBlank { "--:--" } ?: "--:--"} (Sync weather for cutoff)"
          sunsetMin <= 0 -> "After sunset (${weather?.sunset}) — run high-beam headlight & tail beacon"
          sunsetMin <= 45 -> "DUSK CUTOFF IN ${sunsetMin}m (${weather?.sunset}) — plan return leg"
          else -> "Daylight remaining: ${sunsetMin / 60}h ${sunsetMin % 60}m (Sunset ${weather?.sunset})"
        }
      Text(
        text = daylightTxt,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (sunsetMin != null && sunsetMin <= 45) FontWeight.Bold else FontWeight.Normal,
        color = if (sunsetMin != null && sunsetMin <= 45) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun RuralNetworkSeedCard(
  callsign: String,
  settings: MasterSettings,
  activeVehicleName: String,
  usableRangeMi: Int,
  hasGpsFix: Boolean,
  lat: Double,
  lng: Double,
  onHostPublicGroupRide: (title: String, timeLabel: String, details: String) -> Unit,
) {
  val ctx = LocalContext.current
  var showHostDialog by remember { mutableStateOf(false) }
  var rideTitle by remember { mutableStateOf("Rural Backroad Group Ride") }
  var rideTime by remember { mutableStateOf("Sat 10:00 AM") }
  var rideDetails by remember { mutableStateOf("All PEVs welcome — bring helmet & charger.") }

  TacticalCard(title = "Regional Rider Network") {
    Text(
      "Build your local riding crew in ${settings.matchHomeZipOrTown.ifBlank { settings.customWeatherCity.ifBlank { "your area" } }} — share a rider invite or drop a public Group Ride beacon + Board post.",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedButton(
        onClick = {
          val msg =
            RiderMatchEngine.buildLocalRiderInviteMessage(
              callsign = callsign,
              sectorOrZip = settings.matchHomeZipOrTown.ifBlank { settings.customWeatherCity },
              vehicleName = activeVehicleName,
              pace = settings.matchPaceTier,
              terrain = settings.matchTerrainPref,
              usableRangeMi = usableRangeMi,
              vibe = settings.matchScheduleVibe,
              lat = lat,
              lng = lng,
            )
          RiderMatchEngine.launchShareInvite(ctx, "Join @$callsign on Rural ERides GO", msg)
        },
        modifier = Modifier.weight(1f).testTag("radar_invite_local_riders_button"),
      ) {
        Icon(Icons.Default.Share, null, Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Invite Riders", maxLines = 1, softWrap = false)
      }
      Button(
        onClick = { showHostDialog = true },
        enabled = hasGpsFix,
        modifier = Modifier.weight(1f).testTag("radar_host_group_ride_button"),
      ) {
        Icon(Icons.Default.Groups, null, Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Host Group Ride", maxLines = 1, softWrap = false)
      }
    }
    if (!hasGpsFix) {
      Spacer(Modifier.height(4.dp))
      Text("Waiting for GPS lock to anchor public Group Ride staging pin.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
    }
  }

  if (showHostDialog) {
    AlertDialog(
      onDismissRequest = { showHostDialog = false },
      title = { Text("Host Public Group Ride") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            "Publishes a live Radar Meetup pin + a synced GROUP_RIDES Board post tagged [${settings.matchPaceTier.label} • ${settings.matchTerrainPref.label} • Min ${settings.matchMinRangeMi}mi].",
            style = MaterialTheme.typography.labelSmall,
          )
          OutlinedTextField(value = rideTitle, onValueChange = { rideTitle = it }, label = { Text("Ride Title") })
          OutlinedTextField(value = rideTime, onValueChange = { rideTime = it }, label = { Text("Meetup Time") })
          OutlinedTextField(value = rideDetails, onValueChange = { rideDetails = it }, label = { Text("Route / Staging Details") })
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (rideTitle.isNotBlank()) {
              onHostPublicGroupRide(rideTitle.trim(), rideTime.ifBlank { "This Weekend" }, rideDetails.trim())
              showHostDialog = false
            }
          },
        ) { Text("Broadcast Ride") }
      },
      dismissButton = { TextButton(onClick = { showHostDialog = false }) { Text("Cancel") } },
    )
  }
}
