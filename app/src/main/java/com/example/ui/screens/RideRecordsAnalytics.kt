package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.PevBatteryPhysics
import com.example.data.MasterSettings
import com.example.data.PevVehicle
import com.example.data.RideSession
import com.example.ui.compartments.GraphInspectChartType
import com.example.ui.compartments.GraphInspectData
import com.example.ui.compartments.GraphInspectModal
import com.example.ui.compartments.GraphInspectSlice
import com.example.ui.components.RuralPevIcons
import com.example.ui.components.distUnitLabel
import com.example.ui.components.formatDistValue
import com.example.ui.components.formatSpeedValue
import com.example.ui.components.speedUnitLabel
import org.osmdroid.util.GeoPoint

internal data class PieSliceData(
  val label: String,
  val value: Double,
  val color: Color,
)

internal fun parseRideTrackPoints(ride: RideSession): List<GeoPoint> =
  ride.polylinePoints.mapNotNull { raw ->
    val parts = raw.split(",")
    if (parts.size < 2) return@mapNotNull null
    val lat = parts[0].trim().toDoubleOrNull()
    val lng = parts[1].trim().toDoubleOrNull()
    if (lat != null && lng != null &&
      lat.isFinite() && lng.isFinite() &&
      lat in -85.0..85.0 && lng in -180.0..180.0 &&
      (lat != 0.0 || lng != 0.0)
    ) {
      GeoPoint(lat, lng)
    } else {
      null
    }
  }

@Composable
fun RideRecordsAnalyticsSuite(
  rides: List<RideSession>,
  settings: MasterSettings,
  activeVehicle: PevVehicle? = null,
  focusedTrackRideId: String? = null,
  onOpenTelemetryStudio: (String?) -> Unit = {},
) {
  if (rides.isEmpty()) return

  var chartTab by remember(focusedTrackRideId) {
    mutableIntStateOf(if (focusedTrackRideId != null) 5 else 0)
  }
  var pieMode by remember { mutableIntStateOf(0) } // 0 = Distance by PEV, 1 = Speed Zones, 2 = Energy by Ride
  var selectedMapRideId by remember(rides, focusedTrackRideId) {
    mutableStateOf(
      focusedTrackRideId
        ?: rides.firstOrNull { parseRideTrackPoints(it).size >= 2 }?.id
        ?: rides.first().id
    )
  }
  var compareLeftIdx by remember(rides) { mutableIntStateOf(0) }
  var compareRightIdx by remember(rides) { mutableIntStateOf(if (rides.size > 1) 1 else 0) }
  var activeInspectData by remember { mutableStateOf<GraphInspectData?>(null) }
  val fleetGpsPoints = remember(rides) {
    rides.firstOrNull { parseRideTrackPoints(it).size >= 2 }
      ?.let { r -> parseRideTrackPoints(r).map { it.latitude to it.longitude } }
      .orEmpty()
  }

  val primaryThemeColor = MaterialTheme.colorScheme.primary
  val secondaryThemeColor = MaterialTheme.colorScheme.secondary

  val totalKm = remember(rides) { rides.sumOf { it.distanceKm } }
  val topSpeedKmh = remember(rides) { rides.maxOfOrNull { it.maxSpeedKmh } ?: 0.0 }
  val totalWh = remember(rides) { rides.sumOf { it.whUsed } }
  val totalElevM = remember(rides) { rides.sumOf { it.elevationGainM } }
  val totalDurationSec = remember(rides) { rides.sumOf { it.durationSec }.coerceAtLeast(1L) }
  val avgWhPerUnit =
    remember(totalKm, totalWh, settings.distanceUnit) {
      val dist = formatDistValue(totalKm, settings.distanceUnit)
      if (dist > 0.05) totalWh / dist else 0.0
    }
  val distUnit = distUnitLabel(settings.distanceUnit)
  val spdUnit = speedUnitLabel(settings.distanceUnit)

  val activeNomV = activeVehicle?.voltage ?: 48.0
  val activePackSpec = remember(activeNomV, settings.batteryChemistry) {
    PevBatteryPhysics.packProfile(activeNomV, settings.batteryChemistry)
  }
  val fleetAvgWatts = remember(totalWh, totalDurationSec) { totalWh / (totalDurationSec / 3600.0) }
  val fleetAvgAmps = remember(fleetAvgWatts, activePackSpec.nominalPackV) { fleetAvgWatts / activePackSpec.nominalPackV.coerceAtLeast(12.0) }
  val fleetPackWh = (activeVehicle?.batteryWh ?: 960L).toDouble().coerceAtLeast(250.0)
  val estPackCycles = remember(totalWh, fleetPackWh) { totalWh / fleetPackWh }
  val avgClimbGradePct = remember(totalElevM, totalKm) {
    val meters = (totalKm * 1000.0).coerceAtLeast(100.0)
    (totalElevM / meters) * 100.0
  }
  val avgClimbRateMphHr = remember(totalElevM, totalDurationSec) {
    totalElevM / (totalDurationSec / 3600.0)
  }
  val fleetWhPerMi = remember(totalWh, totalKm) { PevBatteryPhysics.whPerMile(totalWh, totalKm) }
  val effGradeLabel = when {
    fleetWhPerMi <= 0.1 -> "LOGGING"
    fleetWhPerMi < 22.0 -> "S-TIER ECO"
    fleetWhPerMi < 36.0 -> "BALANCED"
    fleetWhPerMi < 52.0 -> "SPORT"
    else -> "FULL SEND"
  }

  val palette =
    listOf(
      MaterialTheme.colorScheme.primary,
      MaterialTheme.colorScheme.secondary,
      MaterialTheme.colorScheme.tertiary,
      Color(0xFF38BDF8),
      Color(0xFFF97316),
      Color(0xFFA855F7),
    )

  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag("ride_records_analytics_suite"),
  ) {
    Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(RuralPevIcons.AnalyticsLine, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
          Text(
            "TELEMETRY GRAPHS & TRACKS",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        OutlinedButton(
          onClick = { onOpenTelemetryStudio(null) },
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
          modifier = Modifier.testTag("open_full_telemetry_studio_button"),
        ) {
          Icon(Icons.Default.OpenInFull, contentDescription = "Open Full Telemetry Studio", modifier = Modifier.size(14.dp))
          Spacer(Modifier.width(4.dp))
          Text("Full Studio", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
        }
      }

      // LIFETIME RECORDS KPI STRIP
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AnalyticsKpiPill("TOTAL DIST", "${"%.1f".format(formatDistValue(totalKm, settings.distanceUnit))} $distUnit", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        AnalyticsKpiPill("TOP SPEED", "${"%.1f".format(formatSpeedValue(topSpeedKmh, settings.distanceUnit))} $spdUnit", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
        AnalyticsKpiPill("EFFICIENCY", "${"%.1f".format(avgWhPerUnit)} Wh/$distUnit", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
        AnalyticsKpiPill("CLIMB", "+${formatElevationValue(totalElevM, settings.distanceUnit).toInt()}${elevationUnitLabel(settings.distanceUnit)}", Color(0xFF38BDF8), Modifier.weight(1f))
      }

      // DEEP FLEET ENGINEERING TELEMETRY MATRIX
      Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().testTag("ride_records_deep_matrix"),
      ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
              "DEEP PACK & DRIVETRAIN MATRIX (${activePackSpec.seriesCells}S ${activePackSpec.chemistry} • ${"%.1f".format(activePackSpec.fullChargeV)}V FULL)",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Black,
              color = MaterialTheme.colorScheme.secondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
              Text(
                effGradeLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Avg Draw: ${fleetAvgWatts.toInt()}W (${"%.1f".format(fleetAvgAmps)}A)", style = MaterialTheme.typography.labelSmall)
            Text("Cycles Used: ${"%.2f".format(estPackCycles)}x", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
          }
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Avg Climb Grade: ${"%.1f".format(avgClimbGradePct)}%", style = MaterialTheme.typography.labelSmall)
            Text("Climb Rate: ${avgClimbRateMphHr.toInt()} m/hr • ${totalDurationSec / 60}m saddle", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }

      // VIEW SWITCHER CHIPS (EXPANDED TO 8 ANALYTICS VIEWS)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
          FilterChip(
            selected = chartTab == 0,
            onClick = { chartTab = 0 },
            leadingIcon = { Icon(Icons.Default.PieChart, null, Modifier.size(15.dp)) },
            label = { Text("Pie Charts", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
            modifier = Modifier.testTag("ride_analytics_pie_tab"),
          )
        }
        item {
          FilterChip(
            selected = chartTab == 1,
            onClick = { chartTab = 1 },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ShowChart, null, Modifier.size(15.dp)) },
            label = { Text("Speed Profile", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
            modifier = Modifier.testTag("ride_analytics_line_tab"),
          )
        }
        item {
          FilterChip(
            selected = chartTab == 2,
            onClick = { chartTab = 2 },
            leadingIcon = { Icon(Icons.Default.Terrain, null, Modifier.size(15.dp)) },
            label = { Text("Elevation", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
            modifier = Modifier.testTag("ride_analytics_elevation_tab"),
          )
        }
        item {
          FilterChip(
            selected = chartTab == 3,
            onClick = { chartTab = 3 },
            leadingIcon = { Icon(Icons.Default.ElectricBolt, null, Modifier.size(15.dp)) },
            label = { Text("Wh & Volts", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
            modifier = Modifier.testTag("ride_analytics_efficiency_tab"),
          )
        }
        item {
          FilterChip(
            selected = chartTab == 4,
            onClick = { chartTab = 4 },
            leadingIcon = { Icon(Icons.Default.BarChart, null, Modifier.size(15.dp)) },
            label = { Text("Distance/Wh", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
            modifier = Modifier.testTag("ride_analytics_bar_tab"),
          )
        }
        item {
          FilterChip(
            selected = chartTab == 5,
            onClick = { chartTab = 5 },
            leadingIcon = { Icon(Icons.Default.Map, null, Modifier.size(15.dp)) },
            label = { Text("Track Map", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
            modifier = Modifier.testTag("ride_analytics_map_tab"),
          )
        }
        item {
          FilterChip(
            selected = chartTab == 6,
            onClick = { chartTab = 6 },
            leadingIcon = { Icon(Icons.Default.BubbleChart, null, Modifier.size(15.dp)) },
            label = { Text("Pace Scatter", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
            modifier = Modifier.testTag("ride_analytics_scatter_tab"),
          )
        }
        item {
          FilterChip(
            selected = chartTab == 7,
            onClick = { chartTab = 7 },
            leadingIcon = { Icon(Icons.Default.Schedule, null, Modifier.size(15.dp)) },
            label = { Text("Duration", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
            modifier = Modifier.testTag("ride_analytics_duration_tab"),
          )
        }
        item {
          FilterChip(
            selected = chartTab == 9,
            onClick = { chartTab = 9 },
            leadingIcon = { Icon(Icons.Default.Speed, null, Modifier.size(15.dp)) },
            label = { Text("Watts & Amps", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
            modifier = Modifier.testTag("ride_analytics_power_tab"),
          )
        }
        item {
          FilterChip(
            selected = chartTab == 10,
            onClick = { chartTab = 10 },
            leadingIcon = { Icon(Icons.Default.Radar, null, Modifier.size(15.dp)) },
            label = { Text("5-Axis Radar", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
            modifier = Modifier.testTag("ride_analytics_radar_tab"),
          )
        }
        if (rides.size >= 2) {
          item {
            FilterChip(
              selected = chartTab == 8,
              onClick = { chartTab = 8 },
              leadingIcon = { Icon(Icons.Default.CompareArrows, null, Modifier.size(15.dp)) },
              label = { Text("Compare", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
              modifier = Modifier.testTag("ride_analytics_compare_tab"),
            )
          }
        }
      }

      when (chartTab) {
        // 1. INTERACTIVE PIE / DONUT CHARTS
        0 -> {
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("By Vehicle", "Speed Zones", "Energy Wh").forEachIndexed { idx, lbl ->
              item {
                FilterChip(
                  selected = pieMode == idx,
                  onClick = { pieMode = idx },
                  label = { Text(lbl, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                )
              }
            }
          }
          val slices: List<PieSliceData> =
            remember(rides, pieMode, palette) {
              when (pieMode) {
                0 -> {
                  rides
                    .groupBy { it.vehicleName.ifBlank { "PEV Unit" } }
                    .entries
                    .mapIndexed { idx, (name, list) ->
                      PieSliceData(name, list.sumOf { it.distanceKm }.coerceAtLeast(0.01), palette[idx % palette.size])
                    }
                    .sortedByDescending { it.value }
                    .take(6)
                }
                1 -> {
                  val cruise = rides.count { it.maxSpeedKmh < 25.0 }.toDouble()
                  val brisk = rides.count { it.maxSpeedKmh in 25.0..45.0 }.toDouble()
                  val hyper = rides.count { it.maxSpeedKmh > 45.0 }.toDouble()
                  listOf(
                    PieSliceData("Trail Cruise (<15mph)", cruise, palette[0]),
                    PieSliceData("Country Pace (15-28mph)", brisk, palette[1]),
                    PieSliceData("Hyper Run (28+ mph)", hyper, palette[2]),
                  ).filter { it.value > 0.0 }
                }
                else -> {
                  rides.take(6).mapIndexed { idx, r ->
                    PieSliceData(r.title.ifBlank { "Ride ${idx + 1}" }, r.whUsed.coerceAtLeast(1.0), palette[idx % palette.size])
                  }
                }
              }
            }
          fun openPieInspect() {
            activeInspectData = GraphInspectData(
              title = "Fleet Energy & Distance Breakdown",
              subtitle = "Sector Distribution (${rides.size} total rides)",
              chartType = GraphInspectChartType.PIE_DONUT,
              icon = Icons.Default.PieChart,
              primaryColor = palette[0],
              unitLabel = when (pieMode) { 0 -> distUnit; 1 -> "rides"; else -> "Wh" },
              peakMetric = when (pieMode) { 0 -> "%.1f %s".format(formatDistValue(totalKm, settings.distanceUnit), distUnit); 1 -> "${rides.size} rides"; else -> "${totalWh.toInt()} Wh" },
              totalMetric = "${totalWh.toInt()} Wh",
              pieSlices = slices.map { GraphInspectSlice(it.label, it.value, it.color) },
              gpsPoints = fleetGpsPoints,
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Tap chart to inspect", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(
              onClick = { openPieInspect() },
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.heightIn(min = 48.dp),
            ) {
              Icon(Icons.Default.OpenInFull, contentDescription = "Pop up Pie Chart", modifier = Modifier.size(14.dp))
              Spacer(Modifier.width(4.dp))
              Text("Pop up Graph", style = MaterialTheme.typography.labelSmall)
            }
          }
          TelemetryDonutPieChart(
            slices = slices,
            centerValue =
              when (pieMode) {
                0 -> "%.1f".format(formatDistValue(totalKm, settings.distanceUnit))
                1 -> "${rides.size}"
                else -> "${totalWh.toInt()}"
              },
            centerLabel =
              when (pieMode) {
                0 -> distUnit
                1 -> "rides"
                else -> "Wh"
              },
            reducedMotion = settings.reducedMotion,
            chartSize = 132.dp,
            onTap = { openPieInspect() },
          )
        }

        // 2. INTERACTIVE SPEED PROFILES SPLINE GRAPH
        1 -> {
          val series = remember(rides) { rides.take(16).reversed() }
          fun openSpeedInspect() {
            val maxSpd = series.map { formatSpeedValue(it.maxSpeedKmh, settings.distanceUnit) }
            val avgSpd = series.map { formatSpeedValue(it.avgSpeedKmh, settings.distanceUnit) }
            activeInspectData = GraphInspectData(
              title = "Historical Speed Profiles",
              subtitle = "Peak & Average Speed Progression (${series.size} rides)",
              chartType = GraphInspectChartType.SPEED,
              icon = Icons.AutoMirrored.Filled.ShowChart,
              primaryColor = primaryThemeColor,
              secondaryColor = secondaryThemeColor,
              unitLabel = spdUnit,
              values = maxSpd,
              secondaryValues = avgSpd,
              peakMetric = "%.1f %s".format(maxSpd.maxOrNull() ?: 0.0, spdUnit),
              avgMetric = "%.1f %s".format(avgSpd.average().takeIf { !it.isNaN() } ?: 0.0, spdUnit),
              minMetric = "%.1f %s".format(maxSpd.minOrNull() ?: 0.0, spdUnit),
              referenceValue = avgSpd.average().takeIf { !it.isNaN() },
              timestamps = series.map { it.title },
              gpsPoints = fleetGpsPoints,
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Tap chart to inspect", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(
              onClick = { openSpeedInspect() },
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            ) {
              Icon(Icons.Default.OpenInFull, contentDescription = "Pop up Speed Profile", modifier = Modifier.size(14.dp))
              Spacer(Modifier.width(4.dp))
              Text("Pop up Graph", style = MaterialTheme.typography.labelSmall)
            }
          }
          HistoricalSpeedProfileChart(
            rides = rides,
            settings = settings,
            chartHeight = 160.dp,
            onTap = { openSpeedInspect() },
          )
        }

        // 3. ELEVATION GAINS & CLIMB RATE GRAPH
        2 -> {
          val series = remember(rides) { rides.take(16).reversed() }
          val elevUnit = if (settings.distanceUnit == "mi") "ft" else "m"
          fun openElevInspect() {
            val elevVals = series.map { formatElevationValue(it.elevationGainM, settings.distanceUnit) }
            activeInspectData = GraphInspectData(
              title = "Elevation Gain & Climb Dynamics",
              subtitle = "Cumulative Ascent (${series.size} rides)",
              chartType = GraphInspectChartType.ELEVATION,
              icon = Icons.Default.Terrain,
              primaryColor = Color(0xFF00E5FF),
              unitLabel = elevUnit,
              values = elevVals,
              peakMetric = "+%.0f %s".format(elevVals.maxOrNull() ?: 0.0, elevUnit),
              avgMetric = "+%.0f %s".format(elevVals.average().takeIf { !it.isNaN() } ?: 0.0, elevUnit),
              totalMetric = "+%.0f %s".format(elevVals.sum(), elevUnit),
              referenceValue = elevVals.average().takeIf { !it.isNaN() },
              timestamps = series.map { it.title },
              gpsPoints = fleetGpsPoints,
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Tap chart to inspect", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(
              onClick = { openElevInspect() },
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            ) {
              Icon(Icons.Default.OpenInFull, contentDescription = "Pop up Elevation Graph", modifier = Modifier.size(14.dp))
              Spacer(Modifier.width(4.dp))
              Text("Pop up Graph", style = MaterialTheme.typography.labelSmall)
            }
          }
          HistoricalElevationGainChart(
            rides = rides,
            settings = settings,
            chartHeight = 160.dp,
            onTap = { openElevInspect() },
          )
        }

        // 4. EFFICIENCY (Wh/mi) & PACK VOLTAGE DRAIN GRAPH
        3 -> {
          val series = remember(rides) { rides.take(16).reversed() }
          fun openEffInspect() {
            val effVals = series.map { r ->
              val dist = formatDistValue(r.distanceKm, settings.distanceUnit)
              if (dist > 0.05) r.whUsed / dist else 0.0
            }
            activeInspectData = GraphInspectData(
              title = "Energy Efficiency & Pack Drain",
              subtitle = "Wh per $distUnit consumption rate",
              chartType = GraphInspectChartType.EFFICIENCY_VOLTAGE,
              icon = Icons.Default.ElectricBolt,
              primaryColor = Color(0xFFFFB300),
              unitLabel = "Wh/$distUnit",
              values = effVals,
              peakMetric = "%.1f Wh/%s".format(effVals.maxOrNull() ?: 0.0, distUnit),
              avgMetric = "%.1f Wh/%s".format(effVals.average().takeIf { !it.isNaN() } ?: 0.0, distUnit),
              minMetric = "%.1f Wh/%s".format(effVals.minOrNull() ?: 0.0, distUnit),
              referenceValue = effVals.average().takeIf { !it.isNaN() },
              timestamps = series.map { it.title },
              gpsPoints = fleetGpsPoints,
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Tap chart to inspect", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(
              onClick = { openEffInspect() },
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            ) {
              Icon(Icons.Default.OpenInFull, contentDescription = "Pop up Efficiency Graph", modifier = Modifier.size(14.dp))
              Spacer(Modifier.width(4.dp))
              Text("Pop up Graph", style = MaterialTheme.typography.labelSmall)
            }
          }
          HistoricalEfficiencyVoltageChart(
            rides = rides,
            activeVehicle = activeVehicle,
            settings = settings,
            chartHeight = 160.dp,
            onTap = { openEffInspect() },
          )
        }

        // 5. DISTANCE & ENERGY BAR CHART
        4 -> {
          val series = remember(rides) { rides.take(16).reversed() }
          fun openDistInspect() {
            val distVals = series.map { formatDistValue(it.distanceKm, settings.distanceUnit) }
            activeInspectData = GraphInspectData(
              title = "Distance & Energy Distribution",
              subtitle = "Trip lengths across recent logs",
              chartType = GraphInspectChartType.DISTANCE_ENERGY,
              icon = Icons.Default.BarChart,
              primaryColor = Color(0xFF39FF14),
              unitLabel = distUnit,
              values = distVals,
              peakMetric = "%.1f %s".format(distVals.maxOrNull() ?: 0.0, distUnit),
              avgMetric = "%.1f %s".format(distVals.average().takeIf { !it.isNaN() } ?: 0.0, distUnit),
              totalMetric = "%.1f %s".format(distVals.sum(), distUnit),
              timestamps = series.map { it.title },
              gpsPoints = fleetGpsPoints,
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Tap chart to inspect", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(
              onClick = { openDistInspect() },
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            ) {
              Icon(Icons.Default.OpenInFull, contentDescription = "Pop up Distance Graph", modifier = Modifier.size(14.dp))
              Spacer(Modifier.width(4.dp))
              Text("Pop up Graph", style = MaterialTheme.typography.labelSmall)
            }
          }
          HistoricalDistanceEnergyChart(
            rides = rides,
            settings = settings,
            chartHeight = 148.dp,
            onTap = { openDistInspect() },
          )
        }

        // 6. RECORDED GPS TRACKS ON A SMOOTH-PANNING, LIFECYCLE-SAFE MAP
        5 -> {
          val selectedRide = rides.firstOrNull { it.id == selectedMapRideId } ?: rides.first()

          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(rides.take(10), key = { it.id }) { r ->
              val ptsCount = remember(r.id, r.polylinePoints.size) { parseRideTrackPoints(r).size }
              FilterChip(
                selected = selectedRide.id == r.id,
                onClick = { selectedMapRideId = r.id },
                label = { Text("${r.title.take(18)} ($ptsCount pts)", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
              )
            }
          }

          RideTrackMapView(
            ride = selectedRide,
            settings = settings,
            height = 225.dp,
          )
        }

        // 7. PACE VS EFFICIENCY SCATTER PLOT
        6 -> {
          val series = remember(rides) { rides.take(16).reversed() }
          fun openScatterInspect() {
            val spdVals = series.map { formatSpeedValue(it.avgSpeedKmh, settings.distanceUnit) }
            val whVals = series.map { r ->
              val dist = formatDistValue(r.distanceKm, settings.distanceUnit)
              if (dist > 0.05) r.whUsed / dist else 0.0
            }
            activeInspectData = GraphInspectData(
              title = "Pace vs Efficiency Scatter",
              subtitle = "Speed ($spdUnit) vs Consumption (Wh/$distUnit)",
              chartType = GraphInspectChartType.SCATTER,
              icon = Icons.Default.BubbleChart,
              primaryColor = Color(0xFF39FF14),
              secondaryColor = Color(0xFF00C2FF),
              unitLabel = "Wh/$distUnit",
              values = spdVals,
              secondaryValues = whVals,
              peakMetric = "%.1f %s".format(spdVals.maxOrNull() ?: 0.0, spdUnit),
              avgMetric = "%.1f Wh/%s".format(whVals.average().takeIf { !it.isNaN() } ?: 0.0, distUnit),
              timestamps = series.map { it.title },
              gpsPoints = fleetGpsPoints,
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Tap chart to inspect", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(
              onClick = { openScatterInspect() },
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            ) {
              Icon(Icons.Default.OpenInFull, contentDescription = "Pop up Scatter Graph", modifier = Modifier.size(14.dp))
              Spacer(Modifier.width(4.dp))
              Text("Pop up Graph", style = MaterialTheme.typography.labelSmall)
            }
          }
          SpeedVsEfficiencyScatterChart(
            rides = rides,
            settings = settings,
            chartHeight = 160.dp,
            onTap = { openScatterInspect() },
          )
        }

        // 8. RIDE DURATION DISTRIBUTION HISTOGRAM
        7 -> {
          val series = remember(rides) { rides.take(16).reversed() }
          fun openDurationInspect() {
            val durationsMin = series.map { (it.durationSec / 60.0).coerceAtLeast(1.0) }
            activeInspectData = GraphInspectData(
              title = "Ride Duration Histogram",
              subtitle = "Distribution of ride time in minutes",
              chartType = GraphInspectChartType.HISTOGRAM,
              icon = Icons.Default.Schedule,
              primaryColor = Color(0xFFA855F7),
              unitLabel = "mins",
              values = durationsMin,
              peakMetric = "%.0f mins".format(durationsMin.maxOrNull() ?: 0.0),
              avgMetric = "%.0f mins".format(durationsMin.average().takeIf { !it.isNaN() } ?: 0.0),
              totalMetric = "%.0f mins".format(durationsMin.sum()),
              timestamps = series.map { it.title },
              gpsPoints = fleetGpsPoints,
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Tap chart to inspect", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(
              onClick = { openDurationInspect() },
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            ) {
              Icon(Icons.Default.OpenInFull, contentDescription = "Pop up Duration Graph", modifier = Modifier.size(14.dp))
              Spacer(Modifier.width(4.dp))
              Text("Pop up Graph", style = MaterialTheme.typography.labelSmall)
            }
          }
          RideDurationHistogramChart(
            rides = rides,
            reducedMotion = settings.reducedMotion,
            chartHeight = 130.dp,
            onTap = { openDurationInspect() },
          )
        }

        // 9. POWER (WATTS) & PACK AMPS DRAW
        9 -> {
          val series = remember(rides) { rides.take(16).reversed() }
          fun openPowerInspect() {
            val wattsSeries = series.map { r ->
              val h = (r.durationSec / 3600.0).coerceAtLeast(0.01)
              r.whUsed / h
            }
            activeInspectData = GraphInspectData(
              title = "Power (Watts) & Current Demand",
              subtitle = "Continuous power demand across fleet",
              chartType = GraphInspectChartType.POWER_AMPS,
              icon = Icons.Default.Speed,
              primaryColor = Color(0xFFFF5252),
              unitLabel = "Watts",
              values = wattsSeries,
              peakMetric = "%.0f W".format(wattsSeries.maxOrNull() ?: 0.0),
              avgMetric = "%.0f W".format(wattsSeries.average().takeIf { !it.isNaN() } ?: 0.0),
              minMetric = "%.0f W".format(wattsSeries.minOrNull() ?: 0.0),
              timestamps = series.map { it.title },
              gpsPoints = fleetGpsPoints,
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Tap chart to inspect", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(
              onClick = { openPowerInspect() },
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            ) {
              Icon(Icons.Default.OpenInFull, contentDescription = "Pop up Power Graph", modifier = Modifier.size(14.dp))
              Spacer(Modifier.width(4.dp))
              Text("Pop up Graph", style = MaterialTheme.typography.labelSmall)
            }
          }
          HistoricalPowerAmpsChart(
            rides = rides,
            activeVehicle = activeVehicle,
            settings = settings,
            chartHeight = 160.dp,
            onTap = { openPowerInspect() },
          )
        }

        // 10. 5-AXIS PERFORMANCE RADAR
        10 -> {
          fun openRadarInspect() {
            val radar = computeRiderRadarScores(rides)
            activeInspectData = GraphInspectData(
              title = "5-Axis Rider Performance Radar",
              subtitle = "Latest Ride vs Fleet Benchmark (${rides.size} logs)",
              chartType = GraphInspectChartType.RADAR_5AXIS,
              icon = Icons.Default.Radar,
              primaryColor = Color(0xFF00E5FF),
              unitLabel = "%",
              values = radar.latest.map { it.toDouble() },
              secondaryValues = radar.fleetAvg.map { it.toDouble() },
              peakMetric = radar.peakLabel,
              avgMetric = radar.avgPctLabel,
              minMetric = radar.minLabel,
              timestamps = radar.axisLabels,
              gpsPoints = fleetGpsPoints,
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Tap chart to inspect", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(
              onClick = { openRadarInspect() },
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            ) {
              Icon(Icons.Default.OpenInFull, contentDescription = "Pop up Radar Graph", modifier = Modifier.size(14.dp))
              Spacer(Modifier.width(4.dp))
              Text("Pop up Graph", style = MaterialTheme.typography.labelSmall)
            }
          }
          RiderPerformanceRadarChart(
            rides = rides,
            settings = settings,
            chartHeight = 195.dp,
            onTap = { openRadarInspect() },
          )
        }

        // 11. SIDE-BY-SIDE 2-RIDE COMPARISON
        else -> {
          val left = rides[compareLeftIdx.coerceIn(0, rides.lastIndex)]
          val right = rides[compareRightIdx.coerceIn(0, rides.lastIndex)]
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
              selected = true,
              onClick = { compareLeftIdx = (compareLeftIdx + 1) % rides.size },
              label = { Text("A: ${left.title.take(14)}", style = MaterialTheme.typography.labelSmall) },
              modifier = Modifier.weight(1f),
            )
            FilterChip(
              selected = true,
              onClick = { compareRightIdx = (compareRightIdx + 1) % rides.size },
              label = { Text("B: ${right.title.take(14)}", style = MaterialTheme.typography.labelSmall) },
              modifier = Modifier.weight(1f),
            )
          }
          CompareMetricRow(
            "Distance",
            "${"%.2f".format(formatDistValue(left.distanceKm, settings.distanceUnit))} $distUnit",
            "${"%.2f".format(formatDistValue(right.distanceKm, settings.distanceUnit))} $distUnit",
          )
          CompareMetricRow(
            "Max Speed",
            "${"%.1f".format(formatSpeedValue(left.maxSpeedKmh, settings.distanceUnit))} $spdUnit",
            "${"%.1f".format(formatSpeedValue(right.maxSpeedKmh, settings.distanceUnit))} $spdUnit",
          )
          CompareMetricRow(
            "Avg Speed",
            "${"%.1f".format(formatSpeedValue(left.avgSpeedKmh, settings.distanceUnit))} $spdUnit",
            "${"%.1f".format(formatSpeedValue(right.avgSpeedKmh, settings.distanceUnit))} $spdUnit",
          )
          CompareMetricRow(
            "Energy / Climb",
            "${left.whUsed.toInt()} Wh • +${left.elevationGainM.toInt()}m",
            "${right.whUsed.toInt()} Wh • +${right.elevationGainM.toInt()}m",
          )
        }
      }
    }
  }

  if (activeInspectData != null) {
    GraphInspectModal(
      data = activeInspectData!!,
      onDismiss = { activeInspectData = null },
    )
  }
}

@Composable
private fun AnalyticsKpiPill(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(10.dp),
    modifier = modifier,
  ) {
    Column(Modifier.padding(horizontal = 6.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
      Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
  }
}

@Composable
private fun CompareMetricRow(label: String, valA: String, valB: String) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
    Text(valA, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(valB, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
  }
}
