package com.example.ui.compartments

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai.GeminiAiRepository
import com.example.core.LiveCloudServices
import com.example.data.WeatherMatrixData
import com.example.ui.screens.chartHoverAndScrub
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Supported chart categories for the compartmentalized Graph Inspection Pop-up.
 */
enum class GraphInspectChartType {
  SPEED,
  ELEVATION,
  EFFICIENCY_VOLTAGE,
  DISTANCE_ENERGY,
  SCATTER,
  HISTOGRAM,
  POWER_AMPS,
  RADAR_5AXIS,
  PIE_DONUT,
}

/**
 * Data slice for pie/donut charts in GraphInspectModal.
 */
data class GraphInspectSlice(
  val label: String,
  val value: Double,
  val color: Color,
)

/**
 * Universal payload passed into GraphInspectModal when any graph is clicked.
 */
data class GraphInspectData(
  val title: String,
  val subtitle: String = "",
  val chartType: GraphInspectChartType = GraphInspectChartType.SPEED,
  val icon: ImageVector = Icons.AutoMirrored.Filled.ShowChart,
  val primaryColor: Color = Color(0xFF39FF14),
  val secondaryColor: Color = Color(0xFF00C2FF),
  val tertiaryColor: Color = Color(0xFFFFB300),
  val unitLabel: String = "",
  val values: List<Double> = emptyList(),
  val secondaryValues: List<Double> = emptyList(),
  val pieSlices: List<GraphInspectSlice> = emptyList(),
  val peakMetric: String = "",
  val avgMetric: String = "",
  val minMetric: String = "",
  val totalMetric: String = "",
  val referenceValue: Double? = null,
  val rideId: String? = null,
  val vehicleName: String? = null,
  val timestamps: List<String> = emptyList(),
  val gpsPoints: List<Pair<Double, Double>> = emptyList(),
)

/**
 * Compartmentalized Graph Inspection Modal supporting:
 * - Compose Vector GPU path & shader rendering
 * - Multi-Method statistical analysis: Raw Series, SMA-3 + OLS Trendline, Z-Score σ Bands, Δ Derivative, Quartiles + CDF
 * - Multi-Service live cloud telemetry: Gemini AI Data Scientist + Open-Meteo 30m DEM & Live Weather Correlation
 */
@Composable
fun GraphInspectModal(
  data: GraphInspectData,
  initialMethod: GraphAnalysisMethod = GraphAnalysisMethod.RAW_SERIES,
  onDismiss: () -> Unit,
) {
  val ctx = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val scope = rememberCoroutineScope()
  val geminiRepo = remember { GeminiAiRepository() }
  BackHandler { onDismiss() }

  val applicableMethods = remember(data.chartType) { applicableMethodsFor(data.chartType) }
  var activeMethod by remember(data.title, applicableMethods) {
    mutableStateOf(if (initialMethod in applicableMethods) initialMethod else applicableMethods.first())
  }
  val effectiveMethod = if (activeMethod in applicableMethods) activeMethod else applicableMethods.first()
  var showGrid by remember { mutableStateOf(true) }
  var showRefLine by remember { mutableStateOf(true) }
  var showDataPoints by remember { mutableStateOf(true) }
  var hoveredIdx by remember { mutableStateOf<Int?>(null) }
  var hoveredFrac by remember { mutableFloatStateOf(0.5f) }

  // Live Cloud Services State
  var aiBusy by remember(data.title) { mutableStateOf(false) }
  var aiInsight by remember(data.title) { mutableStateOf("") }
  var geoBusy by remember(data.title) { mutableStateOf(false) }
  var geoDemElevations by remember(data.title) { mutableStateOf<List<Double>>(emptyList()) }
  var geoWeather by remember(data.title) { mutableStateOf<WeatherMatrixData?>(null) }
  var geoStatus by remember(data.title) { mutableStateOf("") }

  val stats = remember(data.values) { computeGraphAnalytics(data.values) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnBackPress = true,
      dismissOnClickOutside = true,
    ),
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .widthIn(max = 700.dp)
        .statusBarsPadding()
        .navigationBarsPadding()
        .heightIn(max = 820.dp)
        .padding(vertical = 12.dp)
        .clip(RoundedCornerShape(20.dp))
        .border(1.5.dp, data.primaryColor.copy(alpha = 0.55f), RoundedCornerShape(20.dp)),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 12.dp,
      shadowElevation = 24.dp,
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // 1. POPUP TOP RIBBON / HEADER
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.horizontalGradient(
                listOf(
                  data.primaryColor.copy(alpha = 0.22f),
                  MaterialTheme.colorScheme.surfaceVariant,
                )
              )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(data.primaryColor.copy(alpha = 0.2f))
                .border(1.dp, data.primaryColor, CircleShape),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = data.icon,
                contentDescription = null,
                tint = data.primaryColor,
                modifier = Modifier.size(20.dp),
              )
            }
            Column {
              Text(
                text = data.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "Compose Vector • ${effectiveMethod.label}" + if (data.subtitle.isNotBlank()) " • ${data.subtitle}" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(48.dp).testTag("graph_inspect_close_btn"),
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close Pop-up")
          }
        }

        HorizontalDivider(color = data.primaryColor.copy(alpha = 0.3f), thickness = 1.dp)

        // 2. SCROLLABLE POPUP BODY
        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          // ANALYTICAL METHOD SWITCHER (only shown when > 1 method is mathematically valid for this chart type)
          if (applicableMethods.size > 1) {
            item {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text = "ANALYTICAL METHOD",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Black,
                  color = data.secondaryColor,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  items(applicableMethods) { method ->
                    FilterChip(
                      selected = effectiveMethod == method,
                      onClick = { activeMethod = method },
                      modifier = Modifier.heightIn(min = 48.dp).testTag("graph_method_${method.name.lowercase()}"),
                      label = { Text(method.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                      colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = data.secondaryColor.copy(alpha = 0.22f),
                        selectedLabelColor = data.secondaryColor,
                      ),
                    )
                  }
                }
                Text(
                  text = effectiveMethod.shortDesc,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          // KEY METRICS BANNER PILLS
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              if (data.peakMetric.isNotBlank()) {
                MetricSummaryCard(
                  label = "PEAK",
                  value = data.peakMetric,
                  accent = data.primaryColor,
                  modifier = Modifier.weight(1f),
                )
              }
              if (data.avgMetric.isNotBlank()) {
                MetricSummaryCard(
                  label = "AVERAGE",
                  value = data.avgMetric,
                  accent = data.secondaryColor,
                  modifier = Modifier.weight(1f),
                )
              }
              if (data.minMetric.isNotBlank()) {
                MetricSummaryCard(
                  label = "MIN / LOW",
                  value = data.minMetric,
                  accent = MaterialTheme.colorScheme.outline,
                  modifier = Modifier.weight(1f),
                )
              }
              if (data.totalMetric.isNotBlank()) {
                MetricSummaryCard(
                  label = "TOTAL",
                  value = data.totalMetric,
                  accent = data.tertiaryColor,
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }

          // COMPUTED STATISTICAL METRICS ROW (ALWAYS LIVE FROM SERIES VALUES)
          if (stats.count > 0) {
            item {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                MetricSummaryCard(
                  label = "OLS SLOPE",
                  value = "${if (stats.olsSlope >= 0) "+" else ""}${"%.2f".format(stats.olsSlope)}",
                  accent = data.tertiaryColor,
                  modifier = Modifier.weight(1f),
                )
                MetricSummaryCard(
                  label = "STD DEV σ",
                  value = "%.2f".format(stats.stdDev),
                  accent = data.secondaryColor,
                  modifier = Modifier.weight(1f),
                )
                MetricSummaryCard(
                  label = "MED / IQR",
                  value = "${"%.1f".format(stats.median)} / ${"%.1f".format(stats.iqr)}",
                  accent = data.primaryColor,
                  modifier = Modifier.weight(1f),
                )
                MetricSummaryCard(
                  label = "Z > 1.5σ",
                  value = "${stats.anomalyIndices.size} pts",
                  accent = if (stats.anomalyIndices.isNotEmpty()) Color(0xFFFF4D6D) else MaterialTheme.colorScheme.outline,
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }

          // LIVE HOVER / SCRUB CALLOUT STRIP
          item {
            val scrubbedValue =
              when {
                hoveredIdx != null && data.chartType == GraphInspectChartType.PIE_DONUT && data.pieSlices.isNotEmpty() -> {
                  val idx = hoveredIdx!!.coerceIn(0, data.pieSlices.lastIndex)
                  val sl = data.pieSlices[idx]
                  val totalPie = data.pieSlices.sumOf { it.value }.coerceAtLeast(0.01)
                  val pct = (sl.value / totalPie) * 100.0
                  "${sl.label}: ${"%.1f".format(sl.value)} ${data.unitLabel} (${"%.1f".format(pct)}%)"
                }
                hoveredIdx != null && data.values.isNotEmpty() -> {
                  val idx = hoveredIdx!!.coerceIn(0, data.values.lastIndex)
                  val v = data.values[idx]
                  val secV = data.secondaryValues.getOrNull(idx)
                  val zVal = stats.zScores.getOrNull(idx) ?: 0.0
                  val deltaVal = stats.deltas.getOrNull(idx) ?: 0.0
                  val timeLabel = data.timestamps.getOrNull(idx) ?: "Point #${idx + 1}"
                  val methodTag =
                    when (effectiveMethod) {
                      GraphAnalysisMethod.ZSCORE_ANOMALY -> " • Z=${"%+.2f".format(zVal)}σ"
                      GraphAnalysisMethod.DERIVATIVE_DELTA -> " • Δ=${"%+.1f".format(deltaVal)}"
                      GraphAnalysisMethod.SMA_TREND -> " • SMA=${"%.1f".format(stats.sma3Values.getOrElse(idx) { v })}"
                      GraphAnalysisMethod.QUARTILE_CDF -> " • Cum=${"%.1f".format(stats.cumulativeValues.getOrElse(idx) { v })}"
                      else -> ""
                    }
                  if (secV != null) {
                    "$timeLabel: ${"%.1f".format(v)} ${data.unitLabel} • Sec: ${"%.1f".format(secV)}$methodTag"
                  } else {
                    "$timeLabel: ${"%.1f".format(v)} ${data.unitLabel}$methodTag"
                  }
                }
                else -> "Scrub across Compose Vector canvas to inspect points & ${effectiveMethod.label}"
              }

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = scrubbedValue,
                style = MaterialTheme.typography.labelSmall,
                color = if (hoveredIdx != null) data.primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (hoveredIdx != null) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
              if (hoveredIdx != null) {
                Text(
                  text = "LOCKED",
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Black,
                  color = data.primaryColor,
                  modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(data.primaryColor.copy(alpha = 0.15f))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                )
              }
            }
          }

          // COMPOSE VECTOR HIGH-DEFINITION CHART VIEWPORT
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0A0F14))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(8.dp),
            ) {
              ComposeVectorMethodChartPreview(
                data = data,
                stats = stats,
                method = effectiveMethod,
                showGrid = showGrid,
                showRefLine = showRefLine,
                showDataPoints = showDataPoints,
                onScrubPoint = { idx, frac ->
                  hoveredIdx = idx
                  hoveredFrac = frac
                },
                onHoverEnd = { hoveredIdx = null },
              )
            }
          }

          // DISPLAY TOGGLE CHIPS (strictly omit non-applicable toggles so no chip is a no-op)
          val showGridChip = data.chartType != GraphInspectChartType.PIE_DONUT && data.chartType != GraphInspectChartType.RADAR_5AXIS
          val showRefChip = data.referenceValue != null && data.referenceValue > 0.0 &&
            effectiveMethod !in setOf(GraphAnalysisMethod.DERIVATIVE_DELTA, GraphAnalysisMethod.QUARTILE_CDF) &&
            data.chartType !in setOf(GraphInspectChartType.PIE_DONUT, GraphInspectChartType.RADAR_5AXIS, GraphInspectChartType.SCATTER, GraphInspectChartType.DISTANCE_ENERGY, GraphInspectChartType.HISTOGRAM)
          val showPointsChip = data.chartType in setOf(
            GraphInspectChartType.SPEED,
            GraphInspectChartType.ELEVATION,
            GraphInspectChartType.EFFICIENCY_VOLTAGE,
            GraphInspectChartType.POWER_AMPS,
          ) && effectiveMethod !in setOf(GraphAnalysisMethod.DERIVATIVE_DELTA, GraphAnalysisMethod.QUARTILE_CDF)

          if (showGridChip || showRefChip || showPointsChip) {
            item {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showGridChip) {
                  item {
                    FilterChip(
                      selected = showGrid,
                      onClick = { showGrid = !showGrid },
                      modifier = Modifier.heightIn(min = 48.dp),
                      leadingIcon = { Icon(Icons.Default.GridOn, null, Modifier.size(14.dp)) },
                      label = { Text("Grid Lines", style = MaterialTheme.typography.labelSmall) },
                      colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = data.primaryColor.copy(alpha = 0.2f),
                        selectedLabelColor = data.primaryColor,
                      ),
                    )
                  }
                }
                if (showRefChip) {
                  item {
                    FilterChip(
                      selected = showRefLine,
                      onClick = { showRefLine = !showRefLine },
                      modifier = Modifier.heightIn(min = 48.dp),
                      leadingIcon = { Icon(Icons.Default.LinearScale, null, Modifier.size(14.dp)) },
                      label = { Text("Avg Guide (${"%.1f".format(data.referenceValue)})", style = MaterialTheme.typography.labelSmall) },
                      colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = data.secondaryColor.copy(alpha = 0.2f),
                        selectedLabelColor = data.secondaryColor,
                      ),
                    )
                  }
                }
                if (showPointsChip) {
                  item {
                    FilterChip(
                      selected = showDataPoints,
                      onClick = { showDataPoints = !showDataPoints },
                      modifier = Modifier.heightIn(min = 48.dp),
                      label = { Text("Points Halo", style = MaterialTheme.typography.labelSmall) },
                    )
                  }
                }
              }
            }
          }

          // LIVE GRAPH CLOUD SERVICES PANEL
          item {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, data.secondaryColor.copy(alpha = 0.35f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                  text = "LIVE CLOUD TELEMETRY SERVICES",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Black,
                  color = data.secondaryColor,
                )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Button(
                    onClick = {
                      if (aiBusy) return@Button
                      aiBusy = true
                      scope.launch {
                        val csv = data.values.takeLast(14).joinToString(", ") { "%.1f".format(it) }
                        val res = geminiRepo.analyzeTelemetryGraphSeries(
                          graphTitle = data.title,
                          unitLabel = data.unitLabel,
                          sampleCount = stats.count,
                          minVal = stats.min,
                          maxVal = stats.max,
                          avgVal = stats.mean,
                          medianVal = stats.median,
                          stdDev = stats.stdDev,
                          slopePerStep = stats.olsSlope,
                          outliersCount = stats.anomalyIndices.size,
                          recentValuesCsv = csv.ifBlank { data.pieSlices.joinToString { "${it.label}:${"%.1f".format(it.value)}" } },
                          methodLabel = "Compose Vector / ${effectiveMethod.label}",
                        )
                        aiInsight = res.getOrElse { "AI Analyst error: ${it.message}" }
                        aiBusy = false
                      }
                    },
                    enabled = !aiBusy,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("graph_service_gemini_btn"),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = data.primaryColor.copy(alpha = 0.22f),
                      contentColor = data.primaryColor,
                    ),
                  ) {
                    if (aiBusy) {
                      CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = data.primaryColor)
                    } else {
                      Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Gemini Analyst", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                  }

                  val curSettings = com.example.ui.theme.LocalTacticalSettings.current
                  OutlinedButton(
                    onClick = {
                      if (geoBusy) return@OutlinedButton
                      if (data.gpsPoints.isEmpty()) {
                        geoStatus = "No GPS track coordinates attached to this summary series. Open a ride with GPS polyline points to query Open-Meteo 30m DEM & Weather."
                        return@OutlinedButton
                      }
                      geoBusy = true
                      val distUnit = curSettings.distanceUnit
                      val tempUnit = curSettings.tempUnit
                      scope.launch {
                        val elevs = LiveCloudServices.fetchElevationSparkline(data.gpsPoints)
                        val anchor = data.gpsPoints.first()
                        val weatherRes = LiveCloudServices.fetchWeatherMatrix(anchor.first, anchor.second)
                        geoDemElevations = elevs
                        geoWeather = weatherRes.getOrNull()
                        val useImperialElev = distUnit.equals("mi", ignoreCase = true)
                        val elevMult = if (useImperialElev) 3.28084 else 1.0
                        val elevUnit = if (useImperialElev) "ft" else "m"
                        geoStatus =
                          buildString {
                            if (elevs.isNotEmpty()) {
                              val gain = elevs.zipWithNext().sumOf { (a, b) -> (b - a).coerceAtLeast(0.0) }
                              append(
                                "Open-Meteo 30m DEM (${elevs.size} pts): Min ${"%.0f".format((elevs.minOrNull() ?: 0.0) * elevMult)}$elevUnit • Peak ${"%.0f".format((elevs.maxOrNull() ?: 0.0) * elevMult)}$elevUnit • Climb +${"%.0f".format(gain * elevMult)}$elevUnit"
                              )
                            }
                            weatherRes.getOrNull()?.let { w ->
                              if (isNotEmpty()) append("\n")
                              val tempStr = com.example.ui.components.formatTempLabel(w.tempC, tempUnit)
                              val windVal = com.example.ui.components.formatSpeedValue(w.windSpeedKmh, distUnit)
                              val windUnit = com.example.ui.components.speedUnitLabel(distUnit)
                              append("${w.locationLabel}: $tempStr • Wind ${"%.1f".format(windVal)} $windUnit @ ${w.windDirDeg}° • ${w.pressureHpa.toInt()} hPa")
                            }
                            if (isEmpty()) append("Elevation/Weather query returned no data for track coordinates.")
                          }
                        geoBusy = false
                      }
                    },
                    enabled = !geoBusy,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("graph_service_dem_btn"),
                  ) {
                    if (geoBusy) {
                      CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                      Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("DEM & Weather", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                  }
                }

                if (aiInsight.isNotBlank()) {
                  Surface(
                    color = Color(0xFF0E161D),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, data.primaryColor.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Text(
                      text = aiInsight,
                      style = MaterialTheme.typography.bodySmall,
                      color = Color(0xFFE2E8F0),
                      modifier = Modifier.padding(10.dp),
                    )
                  }
                }

                if (geoStatus.isNotBlank()) {
                  Surface(
                    color = Color(0xFF0E161D),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, data.secondaryColor.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                      Text(
                        text = geoStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE2E8F0),
                      )
                      if (geoDemElevations.size >= 2) {
                        val demMax = max(1.0, geoDemElevations.maxOrNull() ?: 1.0)
                        val demMin = geoDemElevations.minOrNull() ?: 0.0
                        val demSpan = max(1.0, demMax - demMin)
                        Canvas(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                          val p = Path()
                          val step = size.width / (geoDemElevations.size - 1)
                          geoDemElevations.forEachIndexed { idx, el ->
                            val x = idx * step
                            val y = size.height - (((el - demMin) / demSpan).toFloat() * (size.height - 8f)) - 4f
                            if (idx == 0) p.moveTo(x, y) else p.lineTo(x, y)
                          }
                          drawPath(p, Color(0xFF38BDF8), style = Stroke(width = 3f, cap = StrokeCap.Round))
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          // PIE SLICE LEGEND BREAKDOWN (IF PIE TYPE)
          if (data.chartType == GraphInspectChartType.PIE_DONUT && data.pieSlices.isNotEmpty()) {
            item {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text = "DETAILED SECTOR BREAKDOWN",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val totalPieVal = data.pieSlices.sumOf { it.value }.coerceAtLeast(0.01)
                data.pieSlices.forEach { slice ->
                  val pct = (slice.value / totalPieVal) * 100.0
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(6.dp))
                      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                      .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Box(
                        modifier = Modifier
                          .size(10.dp)
                          .clip(CircleShape)
                          .background(slice.color)
                      )
                      Text(slice.label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                    Text(
                      "${"%.1f".format(slice.value)} (${"%.1f".format(pct)}%)",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = slice.color,
                    )
                  }
                }
              }
            }
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 1.dp)

        // 3. ACTIONS BOTTOM BAR
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = {
                val summary = buildString {
                  append("${data.title} [Compose Vector • ${effectiveMethod.label}]\n")
                  if (data.peakMetric.isNotBlank()) append("Peak: ${data.peakMetric}\n")
                  if (data.avgMetric.isNotBlank()) append("Average: ${data.avgMetric}\n")
                  if (stats.count > 0) {
                    append("Median: ${"%.2f".format(stats.median)} | StdDev: ${"%.2f".format(stats.stdDev)} | OLS Slope: ${"%.3f".format(stats.olsSlope)}\n")
                  }
                  if (aiInsight.isNotBlank()) append("\nAI Analyst:\n$aiInsight\n")
                  append("Samples: ${data.values.size} data points")
                }
                clipboard.setText(AnnotatedString(summary))
                Toast.makeText(ctx, "Graph stats copied to clipboard", Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier.heightIn(min = 48.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
              Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(Modifier.width(6.dp))
              Text("Copy Stats", style = MaterialTheme.typography.labelSmall)
            }

            OutlinedButton(
              onClick = {
                val shareText = buildString {
                  append("${data.title} [Compose Vector • ${effectiveMethod.label}]")
                  if (data.peakMetric.isNotBlank()) append("\nPeak: ${data.peakMetric}")
                  if (data.avgMetric.isNotBlank()) append(" | Avg: ${data.avgMetric}")
                  if (stats.count > 0) append("\nOLS Slope: ${"%.2f".format(stats.olsSlope)} | StdDev: ${"%.2f".format(stats.stdDev)}")
                }
                runCatching {
                  val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, data.title)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                  }
                  ctx.startActivity(Intent.createChooser(send, "Share Telemetry"))
                }.onFailure {
                  clipboard.setText(AnnotatedString(shareText))
                  Toast.makeText(ctx, "Copied to clipboard for sharing", Toast.LENGTH_SHORT).show()
                }
              },
              modifier = Modifier.heightIn(min = 48.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(Modifier.width(6.dp))
              Text("Share", style = MaterialTheme.typography.labelSmall)
            }
          }

          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.heightIn(min = 48.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
          ) {
            Text("Done", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun ComposeVectorMethodChartPreview(
  data: GraphInspectData,
  stats: GraphStatsSummary,
  method: GraphAnalysisMethod,
  showGrid: Boolean,
  showRefLine: Boolean,
  showDataPoints: Boolean,
  onScrubPoint: (Int, Float) -> Unit,
  onHoverEnd: () -> Unit,
) {
  when (data.chartType) {
    GraphInspectChartType.SPEED,
    GraphInspectChartType.ELEVATION,
    GraphInspectChartType.EFFICIENCY_VOLTAGE,
    GraphInspectChartType.POWER_AMPS -> {
      EnlargedSplineCanvas(
        data = data,
        stats = stats,
        method = method,
        showGrid = showGrid,
        showRefLine = showRefLine,
        showDataPoints = showDataPoints,
        onScrubPoint = onScrubPoint,
        onHoverEnd = onHoverEnd,
      )
    }
    GraphInspectChartType.DISTANCE_ENERGY,
    GraphInspectChartType.HISTOGRAM -> {
      EnlargedBarCanvas(
        data = data,
        stats = stats,
        method = method,
        showGrid = showGrid,
        onScrubPoint = onScrubPoint,
        onHoverEnd = onHoverEnd,
      )
    }
    GraphInspectChartType.SCATTER -> {
      EnlargedScatterCanvas(
        data = data,
        stats = stats,
        method = method,
        showGrid = showGrid,
      )
    }
    GraphInspectChartType.RADAR_5AXIS -> EnlargedRadarCanvas(data = data)
    GraphInspectChartType.PIE_DONUT -> EnlargedPieDonutCanvas(data = data)
  }
}

@Composable
private fun MetricSummaryCard(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(accent.copy(alpha = 0.12f))
      .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
      .padding(horizontal = 8.dp, vertical = 6.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      fontSize = 9.sp,
      fontWeight = FontWeight.Bold,
      color = accent,
      maxLines = 1,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.Black,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun EnlargedSplineCanvas(
  data: GraphInspectData,
  stats: GraphStatsSummary,
  method: GraphAnalysisMethod,
  showGrid: Boolean,
  showRefLine: Boolean,
  showDataPoints: Boolean,
  onScrubPoint: (Int, Float) -> Unit,
  onHoverEnd: () -> Unit,
) {
  val values = data.values
  if (values.isEmpty()) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("No telemetry points to plot", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
    }
    return
  }

  val maxV = max(5.0, max(values.maxOrNull() ?: 10.0, stats.mean + stats.stdDev) * 1.15)
  val gridColor = Color.White.copy(alpha = 0.12f)
  val primaryC = data.primaryColor
  val secondaryC = data.secondaryColor
  val tertiaryC = data.tertiaryColor
  val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f) }

  Canvas(
    modifier = Modifier
      .fillMaxSize()
      .chartHoverAndScrub(
        itemCount = values.size,
        useSlots = false,
        onPointFocused = { idx, frac, _ -> onScrubPoint(idx, frac) },
        onHoverEnded = onHoverEnd,
      )
  ) {
    val w = size.width
    val h = size.height
    if (w < 24f || h < 24f) return@Canvas
    val padY = 12f
    val usableH = (h - padY * 2f).coerceAtLeast(16f)
    val maxYBound = (h - padY).coerceAtLeast(padY)
    fun yOf(v: Double): Float {
      val safe = if (v.isFinite()) v else 0.0
      val raw = h - padY - ((safe / maxV).toFloat() * usableH)
      return (if (raw.isFinite()) raw else h - padY).coerceIn(padY, maxYBound)
    }

    if (showGrid) {
      for (g in 0..4) {
        val y = h * g / 4f
        drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
      }
      for (g in 0..6) {
        val x = w * g / 6f
        drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
      }
    }

    if (method == GraphAnalysisMethod.DERIVATIVE_DELTA) {
      val midY = h * 0.52f
      drawLine(Color.White.copy(alpha = 0.4f), Offset(0f, midY), Offset(w, midY), strokeWidth = 1.5f, pathEffect = dashEffect)
      val deltas = stats.deltas
      val maxAbs = max(1.0, deltas.maxOfOrNull { abs(it) } ?: 1.0)
      val slotW = w / deltas.size.coerceAtLeast(1)
      val barW = (slotW * 0.68f).coerceAtLeast(4f)
      deltas.forEachIndexed { i, dv ->
        val bh = ((abs(dv) / maxAbs).toFloat() * (h * 0.38f)).coerceAtLeast(3f)
        val x = i * slotW + (slotW - barW) / 2f
        val y = if (dv >= 0.0) midY - bh else midY
        drawRoundRect(
          color = if (dv >= 0.0) primaryC else tertiaryC,
          topLeft = Offset(x, y),
          size = Size(barW, bh),
          cornerRadius = CornerRadius(4f, 4f),
        )
      }
      return@Canvas
    }

    if (method == GraphAnalysisMethod.QUARTILE_CDF) {
      val p75Y = yOf(stats.p75)
      val p25Y = yOf(stats.p25)
      drawRect(secondaryC.copy(alpha = 0.18f), topLeft = Offset(0f, p75Y), size = Size(w, (p25Y - p75Y).coerceAtLeast(2f)))
      drawLine(tertiaryC, Offset(0f, yOf(stats.median)), Offset(w, yOf(stats.median)), strokeWidth = 2.5f, pathEffect = dashEffect)

      val cumMax = max(1.0, stats.cumulativeValues.lastOrNull() ?: 1.0)
      val cdfPath = Path()
      val stepX = if (stats.cumulativeValues.size > 1) w / (stats.cumulativeValues.size - 1) else w
      stats.cumulativeValues.forEachIndexed { i, cv ->
        val x = i * stepX
        val y = h - padY - ((cv / cumMax).toFloat() * usableH)
        if (i == 0) cdfPath.moveTo(x, y) else cdfPath.lineTo(x, y)
      }
      drawPath(cdfPath, primaryC, style = Stroke(width = 4f, cap = StrokeCap.Round))
      return@Canvas
    }

    if (method == GraphAnalysisMethod.ZSCORE_ANOMALY && stats.stdDev > 0.0) {
      val topY = yOf(stats.mean + stats.stdDev)
      val botY = yOf((stats.mean - stats.stdDev).coerceAtLeast(0.0))
      drawRect(secondaryC.copy(alpha = 0.16f), topLeft = Offset(0f, topY), size = Size(w, (botY - topY).coerceAtLeast(2f)))
      drawLine(secondaryC, Offset(0f, yOf(stats.mean)), Offset(w, yOf(stats.mean)), strokeWidth = 2f, pathEffect = dashEffect)
    }

    if (showRefLine && data.referenceValue != null && data.referenceValue > 0.0) {
      val ry = yOf(data.referenceValue)
      drawLine(
        color = secondaryC,
        start = Offset(0f, ry),
        end = Offset(w, ry),
        strokeWidth = 2f,
        cap = StrokeCap.Round,
        pathEffect = dashEffect,
      )
    }

    val path = Path()
    val fillPath = Path()
    val stepX = if (values.size > 1) w / (values.size - 1) else w

    values.forEachIndexed { i, v ->
      val x = i * stepX
      val y = yOf(v)
      if (i == 0) {
        path.moveTo(x, y)
        fillPath.moveTo(x, h)
        fillPath.lineTo(x, y)
      } else {
        val prevX = (i - 1) * stepX
        val prevY = yOf(values[i - 1])
        val cx = (prevX + x) / 2f
        path.cubicTo(cx, prevY, cx, y, x, y)
        fillPath.cubicTo(cx, prevY, cx, y, x, y)
      }
      if (i == values.lastIndex) {
        fillPath.lineTo(x, h)
        fillPath.close()
      }
    }

    drawPath(
      path = fillPath,
      brush = Brush.verticalGradient(
        listOf(primaryC.copy(alpha = 0.35f), primaryC.copy(alpha = 0.02f)),
        startY = 0f,
        endY = h,
      ),
    )

    drawPath(
      path = path,
      color = if (method == GraphAnalysisMethod.SMA_TREND) primaryC.copy(alpha = 0.42f) else primaryC,
      style = Stroke(width = if (method == GraphAnalysisMethod.SMA_TREND) 2.2f else 3.5f, cap = StrokeCap.Round),
    )

    if (method == GraphAnalysisMethod.SMA_TREND && stats.sma3Values.size == values.size) {
      val smaPath = Path()
      stats.sma3Values.forEachIndexed { i, v ->
        val x = i * stepX
        val y = yOf(v)
        if (i == 0) smaPath.moveTo(x, y) else smaPath.lineTo(x, y)
      }
      drawPath(smaPath, primaryC, style = Stroke(width = 4f, cap = StrokeCap.Round))
      if (stats.olsTrendValues.size >= 2) {
        drawLine(
          color = tertiaryC,
          start = Offset(0f, yOf(stats.olsTrendValues.first())),
          end = Offset(w, yOf(stats.olsTrendValues.last())),
          strokeWidth = 3f,
          pathEffect = dashEffect,
        )
      }
    }

    if (showDataPoints && values.size <= 48) {
      values.forEachIndexed { i, v ->
        val x = i * stepX
        val y = yOf(v)
        drawCircle(primaryC, radius = 4f, center = Offset(x, y))
        drawCircle(Color(0xFF0A0F14), radius = 2f, center = Offset(x, y))
        if (method == GraphAnalysisMethod.ZSCORE_ANOMALY && i in stats.anomalyIndices) {
          drawCircle(Color(0xFFFF4D6D), radius = 9f, center = Offset(x, y), style = Stroke(width = 3f))
        }
      }
    }
  }
}

@Composable
private fun EnlargedBarCanvas(
  data: GraphInspectData,
  stats: GraphStatsSummary,
  method: GraphAnalysisMethod,
  showGrid: Boolean,
  onScrubPoint: (Int, Float) -> Unit,
  onHoverEnd: () -> Unit,
) {
  val values = data.values
  if (values.isEmpty()) return
  val maxV = max(5.0, (values.maxOrNull() ?: 10.0) * 1.15)
  val gridColor = Color.White.copy(alpha = 0.12f)
  val primaryC = data.primaryColor
  val secondaryC = data.secondaryColor
  val tertiaryC = data.tertiaryColor
  val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f) }

  Canvas(
    modifier = Modifier
      .fillMaxSize()
      .chartHoverAndScrub(
        itemCount = values.size,
        useSlots = true,
        onPointFocused = { idx, frac, _ -> onScrubPoint(idx, frac) },
        onHoverEnded = onHoverEnd,
      )
  ) {
    val w = size.width
    val h = size.height
    if (w < 24f || h < 24f) return@Canvas
    if (showGrid) {
      for (g in 0..4) {
        val y = h * g / 4f
        drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
      }
    }

    if (method == GraphAnalysisMethod.DERIVATIVE_DELTA) {
      val midY = h * 0.52f
      drawLine(Color.White.copy(alpha = 0.4f), Offset(0f, midY), Offset(w, midY), strokeWidth = 1.5f, pathEffect = dashEffect)
      val maxAbs = max(1.0, stats.deltas.maxOfOrNull { abs(it) } ?: 1.0)
      val slotW = w / stats.deltas.size.coerceAtLeast(1)
      val barW = (slotW * 0.68f).coerceAtLeast(4f)
      stats.deltas.forEachIndexed { i, dv ->
        val bh = ((abs(dv) / maxAbs).toFloat() * (h * 0.38f)).coerceAtLeast(3f)
        val x = i * slotW + (slotW - barW) / 2f
        val y = if (dv >= 0.0) midY - bh else midY
        drawRect(if (dv >= 0.0) primaryC else tertiaryC, topLeft = Offset(x, y), size = Size(barW, bh))
      }
      return@Canvas
    }

    val slotW = w / values.size
    val barW = (slotW * 0.72f).coerceAtLeast(3f)
    values.forEachIndexed { i, v ->
      val barH = ((v / maxV).toFloat() * h).coerceIn(4f, h)
      val x = i * slotW + (slotW - barW) / 2f
      val y = h - barH
      val barCol = if (method == GraphAnalysisMethod.ZSCORE_ANOMALY && i in stats.anomalyIndices) Color(0xFFFF4D6D) else primaryC
      drawRect(
        brush = Brush.verticalGradient(
          listOf(barCol, barCol.copy(alpha = 0.45f)),
          startY = y,
          endY = h,
        ),
        topLeft = Offset(x, y),
        size = Size(barW, barH),
      )
    }

    if (method == GraphAnalysisMethod.SMA_TREND && stats.sma3Values.size == values.size) {
      val p = Path()
      stats.sma3Values.forEachIndexed { i, v ->
        val x = i * slotW + slotW / 2f
        val y = h - ((v / maxV).toFloat() * h).coerceIn(4f, h)
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
      }
      drawPath(p, secondaryC, style = Stroke(width = 3.5f, cap = StrokeCap.Round))
    }
  }
}

@Composable
private fun EnlargedScatterCanvas(
  data: GraphInspectData,
  stats: GraphStatsSummary,
  method: GraphAnalysisMethod,
  showGrid: Boolean,
) {
  val xs = data.values
  val ys = data.secondaryValues
  if (xs.isEmpty() || ys.isEmpty()) return

  val maxX = max(5.0, (xs.maxOrNull() ?: 10.0) * 1.15)
  val maxY = max(5.0, (ys.maxOrNull() ?: 10.0) * 1.15)
  val primaryC = data.primaryColor
  val secondaryC = data.secondaryColor

  Canvas(Modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height
    if (w < 24f || h < 24f) return@Canvas

    if (showGrid) {
      for (g in 0..4) {
        val y = h * g / 4f
        drawLine(Color.White.copy(alpha = 0.12f), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        val x = w * g / 4f
        drawLine(Color.White.copy(alpha = 0.12f), Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
      }
    }

    val count = min(xs.size, ys.size)
    val maxXBound = (w - 8f).coerceAtLeast(8f)
    val maxYBound = (h - 8f).coerceAtLeast(8f)
    for (i in 0 until count) {
      val vx = xs[i].takeIf { it.isFinite() } ?: 0.0
      val vy = ys[i].takeIf { it.isFinite() } ?: 0.0
      val x = ((vx / maxX).toFloat() * w).coerceIn(8f, maxXBound)
      val y = h - ((vy / maxY).toFloat() * h).coerceIn(8f, maxYBound)
      drawCircle(secondaryC.copy(alpha = 0.3f), radius = 9f, center = Offset(x, y))
      drawCircle(primaryC, radius = 5f, center = Offset(x, y))
      drawCircle(Color.White, radius = 2f, center = Offset(x, y))
      if (method == GraphAnalysisMethod.ZSCORE_ANOMALY && i in stats.anomalyIndices) {
        drawCircle(Color(0xFFFF4D6D), radius = 12f, center = Offset(x, y), style = Stroke(width = 2.5f))
      }
    }
  }
}

@Composable
private fun EnlargedRadarCanvas(data: GraphInspectData) {
  val rawVals = data.values.take(5)
  if (rawVals.isEmpty()) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("No telemetry rides to score radar", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
    }
    return
  }
  val primaryC = data.primaryColor
  val axisLabels = remember(data.timestamps) {
    data.timestamps.ifEmpty { listOf("SPD", "RNG", "CLB", "EFF", "END") }
  }

  Canvas(Modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height
    if (w < 24f || h < 24f) return@Canvas
    val c = Offset(w / 2f, h / 2f)
    val radius = min(w, h) * 0.36f

    for (step in 1..4) {
      val r = radius * (step / 4f)
      val webPath = Path()
      for (i in 0..4) {
        val angle = (i * 2 * PI / 5.0) - (PI / 2.0)
        val p = Offset(c.x + (r * cos(angle)).toFloat(), c.y + (r * sin(angle)).toFloat())
        if (i == 0) webPath.moveTo(p.x, p.y) else webPath.lineTo(p.x, p.y)
      }
      webPath.close()
      drawPath(webPath, Color.White.copy(alpha = 0.15f), style = Stroke(width = 1f))
    }

    val labelPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
      color = 0xFFB0BEC5.toInt()
      textSize = 24f
      textAlign = android.graphics.Paint.Align.CENTER
    }

    for (i in 0..4) {
      val angle = (i * 2 * PI / 5.0) - (PI / 2.0)
      val end = Offset(c.x + (radius * cos(angle)).toFloat(), c.y + (radius * sin(angle)).toFloat())
      drawLine(Color.White.copy(alpha = 0.2f), c, end, strokeWidth = 1f)
      val lx = c.x + ((radius + 24f) * cos(angle)).toFloat()
      val ly = c.y + ((radius + 24f) * sin(angle)).toFloat() + 8f
      drawContext.canvas.nativeCanvas.drawText(
        axisLabels.getOrElse(i) { "" }.take(5).uppercase(),
        lx,
        ly,
        labelPaint,
      )
    }

    val poly = Path()
    for (i in 0..4) {
      val raw = rawVals.getOrElse(i) { 0.0 }.takeIf { it.isFinite() } ?: 0.0
      val v = (if (raw > 1.0) raw / 100.0 else raw).coerceIn(0.1, 1.0)
      val r = radius * v.toFloat()
      val angle = (i * 2 * PI / 5.0) - (PI / 2.0)
      val p = Offset(c.x + (r * cos(angle)).toFloat(), c.y + (r * sin(angle)).toFloat())
      if (i == 0) poly.moveTo(p.x, p.y) else poly.lineTo(p.x, p.y)
    }
    poly.close()

    drawPath(poly, primaryC.copy(alpha = 0.35f), style = Fill)
    drawPath(poly, primaryC, style = Stroke(width = 2.5f))
  }
}

@Composable
private fun EnlargedPieDonutCanvas(data: GraphInspectData) {
  val slices = remember(data.pieSlices) { data.pieSlices.filter { it.value.isFinite() && it.value > 0.0 } }
  if (slices.isEmpty()) return

  val total = slices.sumOf { it.value }.coerceAtLeast(0.01)

  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Canvas(Modifier.size(240.dp)) {
      if (size.width < 24f || size.height < 24f) return@Canvas
      val strokeW = 44f
      var startAngle = -90f
      slices.forEach { s ->
        val rawSweep = ((s.value / total) * 360f).toFloat()
        val sweep = if (rawSweep.isFinite()) rawSweep else 0f
        if (sweep > 0f) {
          drawArc(
            color = s.color,
            startAngle = startAngle,
            sweepAngle = (sweep - 1.5f).coerceAtLeast(1f),
            useCenter = false,
            style = Stroke(width = strokeW),
          )
          startAngle += sweep
        }
      }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = data.peakMetric.ifBlank { "${slices.size} Slices" },
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Black,
      )
      Text(
        text = data.unitLabel.ifBlank { "Breakdown" },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
