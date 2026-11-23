package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.core.PevBatteryPhysics
import com.example.data.MasterSettings
import com.example.data.PevVehicle
import com.example.data.RideSession
import com.example.ui.components.applyThemeToMapView
import com.example.ui.components.createSafeMapView
import com.example.ui.components.configureSmoothMapPanning
import com.example.ui.components.defaultMapLayerId
import com.example.ui.components.distUnitLabel
import com.example.ui.components.formatDistValue
import com.example.ui.components.formatSpeedValue
import com.example.ui.components.resolveMapTileSource
import com.example.ui.components.speedUnitLabel
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

internal fun formatElevationValue(meters: Double, distanceUnit: String): Double =
  if (distanceUnit == "km") meters else meters * 3.28084

internal fun elevationUnitLabel(distanceUnit: String): String =
  if (distanceUnit == "km") "m" else "ft"

internal fun haversineMeters(a: GeoPoint, b: GeoPoint): Double {
  val r = 6371000.0
  val dLat = (b.latitude - a.latitude) * PI / 180.0
  val dLng = (b.longitude - a.longitude) * PI / 180.0
  val lat1 = a.latitude * PI / 180.0
  val lat2 = b.latitude * PI / 180.0
  val h = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLng / 2) * sin(dLng / 2)
  return 2 * r * atan2(sqrt(h), sqrt(1 - h))
}

internal fun buildRideSpeedProfile(ride: RideSession, distanceUnit: String): List<Double> {
  val maxSpd = formatSpeedValue(ride.maxSpeedKmh.coerceAtLeast(0.0), distanceUnit)
  val avgSpd = formatSpeedValue(ride.avgSpeedKmh.coerceAtLeast(0.0), distanceUnit).coerceAtMost(maxSpd)
  if (maxSpd <= 0.1 && avgSpd <= 0.1) return List(16) { 0.0 }

  val pts = parseRideTrackPoints(ride)
  val buckets = 16
  val rawWeights =
    if (pts.size >= 4) {
      val segDists = (1 until pts.size).map { i -> haversineMeters(pts[i - 1], pts[i]) }
      val maxSeg = segDists.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
      List(buckets) { idx ->
        val frac = idx.toDouble() / (buckets - 1)
        val segIdx = (frac * (segDists.size - 1)).toInt().coerceIn(0, segDists.lastIndex)
        val envelope = sin(frac * PI).coerceIn(0.18, 1.0)
        val segRatio = (segDists[segIdx] / maxSeg).coerceIn(0.25, 1.0)
        (0.55 * envelope + 0.45 * segRatio).coerceIn(0.15, 1.0)
      }
    } else {
      val seedPhase = ((ride.id.hashCode().toLong() and 0xFFL) / 255.0) * 1.4
      List(buckets) { idx ->
        val frac = idx.toDouble() / (buckets - 1)
        val ramp = sin(frac * PI).coerceIn(0.15, 1.0)
        val cadence = 0.85 + 0.15 * sin(frac * 3.0 * PI + seedPhase)
        (ramp * cadence).coerceIn(0.12, 1.0)
      }
    }

  val peakIdx = rawWeights.indices.maxByOrNull { rawWeights[it] } ?: (buckets / 2)
  return rawWeights.mapIndexed { idx, w ->
    when (idx) {
      0 -> (avgSpd * 0.25).coerceAtMost(maxSpd)
      buckets - 1 -> (avgSpd * 0.20).coerceAtMost(maxSpd)
      peakIdx -> maxSpd
      else -> (avgSpd * 0.65 + (maxSpd - avgSpd * 0.65) * w).coerceIn(0.0, maxSpd)
    }
  }
}

internal fun buildRideElevationProfile(ride: RideSession, distanceUnit: String): List<Double> {
  val totalGain = formatElevationValue(ride.elevationGainM.coerceAtLeast(0.0), distanceUnit)
  val buckets = 16
  if (totalGain <= 0.1) return List(buckets) { 0.0 }

  val pts = parseRideTrackPoints(ride)
  val increments =
    if (pts.size >= 4) {
      val segDists = (1 until pts.size).map { i -> haversineMeters(pts[i - 1], pts[i]).coerceAtLeast(0.5) }
      List(buckets - 1) { idx ->
        val segIdx = (idx.toDouble() / (buckets - 1) * segDists.size).toInt().coerceIn(0, segDists.lastIndex)
        val gradeWave = 0.6 + 0.4 * abs(sin((idx + 1) * 0.7))
        segDists[segIdx] * gradeWave
      }
    } else {
      val seed = ((ride.id.hashCode().toLong() and 0x7FL) / 127.0) * 1.8
      List(buckets - 1) { idx ->
        val frac = (idx + 1).toDouble() / buckets
        (0.45 + 0.55 * sin(frac * 2.2 * PI + seed) * sin(frac * 2.2 * PI + seed)).coerceAtLeast(0.15)
      }
    }

  val sumInc = increments.sum().coerceAtLeast(0.001)
  val result = ArrayList<Double>(buckets)
  var acc = 0.0
  result.add(0.0)
  increments.forEach { inc ->
    acc += (inc / sumInc) * totalGain
    result.add(acc.coerceIn(0.0, totalGain))
  }
  return result
}

internal fun computeRideEnergySplit(
  ride: RideSession,
  primary: Color,
  secondary: Color,
  tertiary: Color,
): List<PieSliceData> {
  val totalWh = ride.whUsed.coerceAtLeast(1.0)
  val climbWhRaw = (100.0 * 9.81 * ride.elevationGainM.coerceAtLeast(0.0)) / (3600.0 * 0.78)
  val climbWh = climbWhRaw.coerceIn(0.0, totalWh * 0.55)
  val remaining = (totalWh - climbWh).coerceAtLeast(0.0)
  val speedRatio = (ride.avgSpeedKmh / 45.0).coerceIn(0.15, 0.75)
  val aeroWh = remaining * speedRatio
  val rollingWh = (remaining - aeroWh).coerceAtLeast(0.1)
  return listOf(
    PieSliceData("Rolling Resistance", rollingWh, primary),
    PieSliceData("Aero & Speed Surge", aeroWh.coerceAtLeast(0.1), secondary),
    PieSliceData("Hill Climb Work", climbWh.coerceAtLeast(0.0), tertiary),
  ).filter { it.value > 0.05 }
}

/**
 * ANIMATED NEON DONUT / PIE CHART WITH POLAR HOVER + TAP TOOLTIPS
 */
@Composable
internal fun TelemetryDonutPieChart(
  slices: List<PieSliceData>,
  centerValue: String,
  centerLabel: String,
  reducedMotion: Boolean = false,
  chartSize: Dp = 136.dp,
  modifier: Modifier = Modifier,
) {
  if (slices.isEmpty()) {
    Text(
      "Not enough telemetry recorded for this chart yet.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = modifier,
    )
    return
  }
  val total = slices.sumOf { it.value }.coerceAtLeast(0.01)
  val slicesKey = "${slices.size}_${slices.first().label}_${total.toInt()}"
  val sweepProgressState = rememberChartEntranceState(triggerKey = slicesKey, reducedMotion = reducedMotion, durationMs = 700)
  var hoveredSliceIdx by remember(slicesKey) { mutableStateOf<Int?>(null) }
  val pulseAlphaState = rememberNeonPulseAlphaState(active = hoveredSliceIdx != null, reducedMotion = reducedMotion)

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    val activeSlice = hoveredSliceIdx?.let { slices.getOrNull(it) }
    NeonChartTooltipOverlay(
      visible = activeSlice != null,
      anchorXFraction = 0.35f,
      title = activeSlice?.label.orEmpty(),
      accentColor = activeSlice?.color ?: MaterialTheme.colorScheme.primary,
      metrics =
        if (activeSlice != null) {
          val pct = ((activeSlice.value / total) * 100.0).toInt()
          listOf(
            NeonTooltipMetric("SHARE", "$pct%", activeSlice.color),
            NeonTooltipMetric("VALUE", "${"%.1f".format(activeSlice.value)} $centerLabel", MaterialTheme.colorScheme.onSurface),
          )
        } else emptyList(),
    )

    Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(modifier = Modifier.size(chartSize), contentAlignment = Alignment.Center) {
        Canvas(
          modifier =
            Modifier
              .fillMaxSize()
              .chartPointer2D(
                key = slicesKey,
                onPointerAt = { pos, w, h, _ ->
                  val cx = w / 2f
                  val cy = h / 2f
                  val dx = pos.x - cx
                  val dy = pos.y - cy
                  val dist = hypot(dx, dy)
                  val maxR = minOf(w, h) / 2f
                  if (dist in (maxR * 0.28f)..(maxR * 1.05f)) {
                    var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                    if (angleDeg < 0f) angleDeg += 360f
                    var acc = 0f
                    var found = 0
                    slices.forEachIndexed { idx, s ->
                      val sweep = ((s.value / total) * 360.0).toFloat().coerceAtLeast(4f)
                      if (angleDeg in acc..(acc + sweep)) found = idx
                      acc += sweep
                    }
                    if (hoveredSliceIdx != found) hoveredSliceIdx = found
                  }
                },
                onHoverEnded = { hoveredSliceIdx = null },
              ),
        ) {
          val sweepProgress = sweepProgressState.value
          val pulseAlpha = pulseAlphaState.value
          var startAngle = -90f
          val baseStroke = (chartSize.toPx() * 0.20f).coerceIn(18f, 34f)
          val arcSize = Size(size.width - baseStroke * 1.25f, size.height - baseStroke * 1.25f)
          val topLeft = Offset((size.width - arcSize.width) / 2f, (size.height - arcSize.height) / 2f)

          slices.forEachIndexed { idx, s ->
            val rawSweep = ((s.value / total) * 360.0).toFloat().coerceAtLeast(4f)
            val animSweep = (rawSweep * sweepProgress - 1.5f).coerceAtLeast(0.5f)
            val isFocused = hoveredSliceIdx == idx
            val strokeW = if (isFocused) baseStroke * 1.18f else baseStroke
            if (isFocused) {
              drawArc(
                color = s.color.copy(alpha = 0.28f * pulseAlpha),
                startAngle = startAngle,
                sweepAngle = animSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeW + 10.dp.toPx(), cap = StrokeCap.Butt),
              )
            }
            drawArc(
              color = if (hoveredSliceIdx == null || isFocused) s.color else s.color.copy(alpha = 0.42f),
              startAngle = startAngle,
              sweepAngle = animSweep,
              useCenter = false,
              topLeft = topLeft,
              size = arcSize,
              style = Stroke(width = strokeW, cap = StrokeCap.Butt),
            )
            startAngle += rawSweep * sweepProgress
          }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = if (activeSlice != null) "${((activeSlice.value / total) * 100.0).toInt()}%" else centerValue,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = activeSlice?.color ?: MaterialTheme.colorScheme.primary,
            maxLines = 1,
            softWrap = false,
          )
          Text(
            text = if (activeSlice != null) activeSlice.label.take(10) else centerLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
          )
        }
      }
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        slices.forEachIndexed { idx, s ->
          val pct = ((s.value / total) * 100.0).toInt()
          val isFocused = hoveredSliceIdx == idx
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier =
              Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(if (isFocused) s.color.copy(alpha = 0.16f) else Color.Transparent)
                .clickable { hoveredSliceIdx = if (isFocused) null else idx }
                .padding(horizontal = 4.dp, vertical = 2.dp),
          ) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(s.color))
            Text(
              text = s.label,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = if (isFocused) FontWeight.Black else FontWeight.Normal,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            Text("$pct%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = s.color, maxLines = 1, softWrap = false)
          }
        }
      }
    }
  }
}

/**
 * 1. HISTORICAL SPEED PROFILES GRAPH (Animated Neon Dual-Spline Area + Floating Hover-Tooltip)
 */
@Composable
internal fun HistoricalSpeedProfileChart(
  rides: List<RideSession>,
  settings: MasterSettings,
  chartHeight: Dp = 170.dp,
  modifier: Modifier = Modifier,
) {
  val series = remember(rides) { rides.take(16).reversed() }
  if (series.isEmpty()) return
  val seriesKey = "${series.size}_${series.first().id}_${series.last().id}"
  val entranceState = rememberChartEntranceState(triggerKey = seriesKey, reducedMotion = settings.reducedMotion, durationMs = 680)

  var selectedIdx by remember(seriesKey) { mutableIntStateOf(series.lastIndex) }
  var anchorXFrac by remember(seriesKey) { mutableFloatStateOf(1f) }
  var showTooltip by remember(seriesKey) { mutableStateOf(false) }
  val pulseAlphaState = rememberNeonPulseAlphaState(active = showTooltip, reducedMotion = settings.reducedMotion)

  val spdUnit = speedUnitLabel(settings.distanceUnit)
  val distUnit = distUnitLabel(settings.distanceUnit)
  val maxSpdSeries = remember(series, settings.distanceUnit) { series.map { formatSpeedValue(it.maxSpeedKmh, settings.distanceUnit) } }
  val avgSpdSeries = remember(series, settings.distanceUnit) { series.map { formatSpeedValue(it.avgSpeedKmh, settings.distanceUnit) } }
  val yMax = max(10.0, (maxSpdSeries.maxOrNull() ?: 20.0) * 1.18)

  val primaryC = MaterialTheme.colorScheme.primary
  val secondaryC = MaterialTheme.colorScheme.secondary
  val gridC = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
  val inspected = series[selectedIdx.coerceIn(0, series.lastIndex)]

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text(
        "Tap or scrub to inspect • ${inspected.title.take(18)}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("● Peak", style = MaterialTheme.typography.labelSmall, color = primaryC, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        Text("● Avg", style = MaterialTheme.typography.labelSmall, color = secondaryC, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
      }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
      Canvas(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(chartHeight)
            .chartHoverAndScrub(
              itemCount = series.size,
              useSlots = false,
              onPointFocused = { idx, frac, _ ->
                selectedIdx = idx
                anchorXFrac = frac
                showTooltip = true
              },
              onHoverEnded = { showTooltip = false },
            ),
      ) {
        val entrance = entranceState.value
        val pulseAlpha = pulseAlphaState.value
        val w = size.width
        val h = size.height
        for (g in 0..3) {
          drawLine(gridC, Offset(0f, h * g / 3f), Offset(w, h * g / 3f), strokeWidth = 1f)
        }
        clipRect(left = 0f, top = 0f, right = w * entrance, bottom = h) {
          if (series.size == 1) {
            val y1 = h - ((maxSpdSeries[0] / yMax).toFloat() * h * entrance).coerceIn(8f, h - 8f)
            val y2 = h - ((avgSpdSeries[0] / yMax).toFloat() * h * entrance).coerceIn(8f, h - 8f)
            drawCircle(primaryC.copy(alpha = 0.28f * pulseAlpha), radius = 13.dp.toPx(), center = Offset(w / 2f, y1))
            drawCircle(primaryC, radius = 7.dp.toPx(), center = Offset(w / 2f, y1))
            drawCircle(secondaryC, radius = 5.dp.toPx(), center = Offset(w / 2f, y2))
          } else {
            val stepX = w / (series.size - 1).coerceAtLeast(1)
            val maxPts = maxSpdSeries.mapIndexed { i, v -> Offset(i * stepX, h - ((v / yMax).toFloat() * h * entrance).coerceIn(6f, h - 6f)) }
            val avgPts = avgSpdSeries.mapIndexed { i, v -> Offset(i * stepX, h - ((v / yMax).toFloat() * h * entrance).coerceIn(6f, h - 6f)) }

            val maxPath = buildSmoothSplinePath(maxPts)
            val fillPath = Path().apply {
              addPath(maxPath)
              lineTo(maxPts.last().x, h)
              lineTo(maxPts.first().x, h)
              close()
            }
            val avgPath = buildSmoothSplinePath(avgPts)

            drawPath(fillPath, brush = Brush.verticalGradient(listOf(primaryC.copy(alpha = 0.34f), Color.Transparent)))
            // Neon outer glow strokes
            drawPath(maxPath, primaryC.copy(alpha = 0.22f), style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
            drawPath(avgPath, secondaryC.copy(alpha = 0.18f), style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
            // Core crisp neon lines
            drawPath(maxPath, primaryC, style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round))
            drawPath(avgPath, secondaryC, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))

            val selIdx = selectedIdx.coerceIn(0, maxPts.lastIndex)
            val selPt = maxPts[selIdx]
            val selAvg = avgPts[selIdx]
            drawLine(
              color = primaryC.copy(alpha = if (showTooltip) 0.80f else 0.45f),
              start = Offset(selPt.x, 0f),
              end = Offset(selPt.x, h),
              strokeWidth = 1.5.dp.toPx(),
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            )
            // Pulsing neon beacon halo on focused node
            drawCircle(primaryC.copy(alpha = 0.32f * pulseAlpha), radius = 11.dp.toPx(), center = selPt)
            drawCircle(secondaryC.copy(alpha = 0.25f * pulseAlpha), radius = 8.dp.toPx(), center = selAvg)

            maxPts.forEachIndexed { idx, pt ->
              drawCircle(primaryC, radius = if (idx == selIdx) 5.5.dp.toPx() else 3.dp.toPx(), center = pt)
            }
            avgPts.forEachIndexed { idx, pt ->
              drawCircle(secondaryC, radius = if (idx == selIdx) 4.5.dp.toPx() else 2.5.dp.toPx(), center = pt)
            }
          }
        }
      }

      NeonChartTooltipOverlay(
        visible = showTooltip,
        anchorXFraction = anchorXFrac,
        title = inspected.title.ifBlank { "Trail Ride" },
        subtitle = "${inspected.vehicleName} • ${"%.2f".format(formatDistValue(inspected.distanceKm, settings.distanceUnit))} $distUnit",
        accentColor = primaryC,
        metrics =
          listOf(
            NeonTooltipMetric("PEAK", "${"%.1f".format(formatSpeedValue(inspected.maxSpeedKmh, settings.distanceUnit))} $spdUnit", primaryC),
            NeonTooltipMetric("AVG", "${"%.1f".format(formatSpeedValue(inspected.avgSpeedKmh, settings.distanceUnit))} $spdUnit", secondaryC),
            NeonTooltipMetric("TIME", "${inspected.durationSec / 60}m", MaterialTheme.colorScheme.onSurface),
          ),
        modifier = Modifier.align(Alignment.TopStart).padding(top = 2.dp),
      )
    }

    Surface(
      color = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(10.dp),
      border = BorderStroke(1.dp, primaryC.copy(alpha = 0.30f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Peak: ${"%.1f".format(formatSpeedValue(inspected.maxSpeedKmh, settings.distanceUnit))} $spdUnit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = primaryC, maxLines = 1, softWrap = false)
        Text("Avg: ${"%.1f".format(formatSpeedValue(inspected.avgSpeedKmh, settings.distanceUnit))} $spdUnit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = secondaryC, maxLines = 1, softWrap = false)
        Text("${inspected.durationSec / 60}m • ${inspected.vehicleName.take(12)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
      }
    }
  }
}

/**
 * 2. HISTORICAL ELEVATION GAINS & CLIMB RATE GRAPH (Staggered Rising Bars + Neon Hover-Tooltip)
 */
@Composable
internal fun HistoricalElevationGainChart(
  rides: List<RideSession>,
  settings: MasterSettings,
  chartHeight: Dp = 165.dp,
  modifier: Modifier = Modifier,
) {
  val series = remember(rides) { rides.take(16).reversed() }
  if (series.isEmpty()) return
  val seriesKey = "${series.size}_${series.first().id}_${series.last().id}"
  val entranceState = rememberChartEntranceState(triggerKey = seriesKey, reducedMotion = settings.reducedMotion, durationMs = 700)

  var selectedIdx by remember(seriesKey) { mutableIntStateOf(series.lastIndex) }
  var anchorXFrac by remember(seriesKey) { mutableFloatStateOf(1f) }
  var showTooltip by remember(seriesKey) { mutableStateOf(false) }
  val pulseAlphaState = rememberNeonPulseAlphaState(active = showTooltip, reducedMotion = settings.reducedMotion)

  val elevUnit = elevationUnitLabel(settings.distanceUnit)
  val distUnit = distUnitLabel(settings.distanceUnit)
  val elevVals = remember(series, settings.distanceUnit) { series.map { formatElevationValue(it.elevationGainM, settings.distanceUnit) } }
  val climbRates =
    remember(series, settings.distanceUnit) {
      series.map { r ->
        val d = formatDistValue(r.distanceKm, settings.distanceUnit)
        if (d > 0.05) formatElevationValue(r.elevationGainM, settings.distanceUnit) / d else 0.0
      }
    }
  val maxElev = max(20.0, (elevVals.maxOrNull() ?: 50.0) * 1.18)
  val maxRate = max(10.0, (climbRates.maxOrNull() ?: 25.0) * 1.18)
  val elevColor = Color(0xFF38BDF8)
  val rateColor = Color(0xFFF59E0B)
  val gridC = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
  val inspected = series[selectedIdx.coerceIn(0, series.lastIndex)]
  val inspectedElev = elevVals[selectedIdx.coerceIn(0, elevVals.lastIndex)]
  val inspectedRate = climbRates[selectedIdx.coerceIn(0, climbRates.lastIndex)]

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("Elevation Gain (+$elevUnit) & Climb Rate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("■ Climb", style = MaterialTheme.typography.labelSmall, color = elevColor, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        Text("● Rate", style = MaterialTheme.typography.labelSmall, color = rateColor, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
      }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
      Canvas(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(chartHeight)
            .chartHoverAndScrub(
              itemCount = series.size,
              useSlots = true,
              onPointFocused = { idx, frac, _ ->
                selectedIdx = idx
                anchorXFrac = frac
                showTooltip = true
              },
              onHoverEnded = { showTooltip = false },
            ),
      ) {
        val entrance = entranceState.value
        val pulseAlpha = pulseAlphaState.value
        val w = size.width
        val h = size.height
        for (g in 0..3) drawLine(gridC, Offset(0f, h * g / 3f), Offset(w, h * g / 3f), strokeWidth = 1f)

        val slotW = w / series.size.coerceAtLeast(1)
        val barW = (slotW * 0.56f).coerceIn(8f, 38f)
        val ratePts = ArrayList<Offset>(series.size)

        elevVals.forEachIndexed { idx, ev ->
          val stagger = ((entrance * 1.35f) - (idx.toFloat() / (series.size + 2)) * 0.35f).coerceIn(0f, 1f)
          val barH = ((ev / maxElev).toFloat() * h * stagger).coerceIn(3f, h - 4f)
          val xLeft = idx * slotW + (slotW - barW) / 2f
          val isSel = idx == selectedIdx
          if (isSel && showTooltip) {
            drawRoundRect(
              color = elevColor.copy(alpha = 0.22f * pulseAlpha),
              topLeft = Offset(xLeft - 3.dp.toPx(), h - barH - 3.dp.toPx()),
              size = Size(barW + 6.dp.toPx(), barH + 3.dp.toPx()),
              cornerRadius = CornerRadius(10f, 10f),
            )
          }
          drawRoundRect(
            brush = Brush.verticalGradient(listOf(elevColor.copy(alpha = if (isSel) 0.96f else 0.62f), elevColor.copy(alpha = 0.18f))),
            topLeft = Offset(xLeft, h - barH),
            size = Size(barW, barH),
            cornerRadius = CornerRadius(8f, 8f),
          )
          val rY = h - ((climbRates[idx] / maxRate).toFloat() * h * stagger).coerceIn(6f, h - 6f)
          ratePts.add(Offset(xLeft + barW / 2f, rY))
        }
        clipRect(0f, 0f, w * entrance, h) {
          if (ratePts.size > 1) {
            val path = buildSmoothSplinePath(ratePts)
            drawPath(path, rateColor.copy(alpha = 0.22f), style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round))
            drawPath(path, rateColor, style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round))
          }
          ratePts.forEachIndexed { idx, pt ->
            if (idx == selectedIdx) {
              drawCircle(rateColor.copy(alpha = 0.30f * pulseAlpha), radius = 9.dp.toPx(), center = pt)
            }
            drawCircle(rateColor, radius = if (idx == selectedIdx) 5.dp.toPx() else 3.dp.toPx(), center = pt)
          }
        }
      }

      NeonChartTooltipOverlay(
        visible = showTooltip,
        anchorXFraction = anchorXFrac,
        title = inspected.title.ifBlank { "Trail Climb" },
        subtitle = inspected.vehicleName,
        accentColor = elevColor,
        metrics =
          listOf(
            NeonTooltipMetric("GAIN", "+${inspectedElev.toInt()} $elevUnit", elevColor),
            NeonTooltipMetric("RATE", "${"%.0f".format(inspectedRate)} $elevUnit/$distUnit", rateColor),
          ),
        modifier = Modifier.align(Alignment.TopStart).padding(top = 2.dp),
      )
    }

    Surface(
      color = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(10.dp),
      border = BorderStroke(1.dp, elevColor.copy(alpha = 0.30f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("${inspected.title.take(14)}: +${inspectedElev.toInt()} $elevUnit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = elevColor, maxLines = 1, softWrap = false)
        Text("Climb Rate: ${"%.0f".format(inspectedRate)} $elevUnit/$distUnit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = rateColor, maxLines = 1, softWrap = false)
      }
    }
  }
}

/**
 * 3. ENERGY EFFICIENCY (Wh/mi or Wh/km) & EXACT PACK VOLTAGE DRAIN CHART
 */
@Composable
internal fun HistoricalEfficiencyVoltageChart(
  rides: List<RideSession>,
  activeVehicle: PevVehicle?,
  settings: MasterSettings,
  chartHeight: Dp = 160.dp,
  modifier: Modifier = Modifier,
) {
  val series = remember(rides) { rides.take(14).reversed() }
  if (series.isEmpty()) return
  val seriesKey = "${series.size}_${series.first().id}_${series.last().id}"
  val entranceState = rememberChartEntranceState(triggerKey = seriesKey, reducedMotion = settings.reducedMotion, durationMs = 680)

  var selectedIdx by remember(seriesKey) { mutableIntStateOf(series.lastIndex) }
  var anchorXFrac by remember(seriesKey) { mutableFloatStateOf(1f) }
  var showTooltip by remember(seriesKey) { mutableStateOf(false) }
  val pulseAlphaState = rememberNeonPulseAlphaState(active = showTooltip, reducedMotion = settings.reducedMotion)

  val nomV = activeVehicle?.voltage ?: 48.0
  val distUnit = distUnitLabel(settings.distanceUnit)
  val effSeries =
    remember(series, settings.distanceUnit) {
      series.map { r ->
        if (settings.distanceUnit == "km") PevBatteryPhysics.whPerKm(r.whUsed, r.distanceKm)
        else PevBatteryPhysics.whPerMile(r.whUsed, r.distanceKm)
      }
    }
  val drainSeries =
    remember(series) {
      series.map { r ->
        if (r.startBatteryPct >= 0 && r.endBatteryPct >= 0) (r.startBatteryPct - r.endBatteryPct).coerceAtLeast(0).toDouble()
        else (r.whUsed / ((activeVehicle?.batteryWh ?: 1500L).toDouble().coerceAtLeast(200.0)) * 100.0).coerceIn(0.0, 100.0)
      }
    }
  val maxEff = max(20.0, (effSeries.maxOrNull() ?: 35.0) * 1.2)
  val effColor = MaterialTheme.colorScheme.tertiary
  val drainColor = Color(0xFFEF4444)
  val gridC = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
  val inspected = series[selectedIdx.coerceIn(0, series.lastIndex)]
  val startV = if (inspected.startBatteryPct >= 0) PevBatteryPhysics.voltageForSoc(nomV, inspected.startBatteryPct.toDouble(), settings.batteryChemistry) else 0.0
  val endV = if (inspected.endBatteryPct >= 0) PevBatteryPhysics.voltageForSoc(nomV, inspected.endBatteryPct.toDouble(), settings.batteryChemistry) else 0.0

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text("Efficiency (Wh/$distUnit) & Pack Drain (%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("● Wh/$distUnit", style = MaterialTheme.typography.labelSmall, color = effColor, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        Text("■ Drain%", style = MaterialTheme.typography.labelSmall, color = drainColor, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
      }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
      Canvas(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(chartHeight)
            .chartHoverAndScrub(
              itemCount = series.size,
              useSlots = true,
              onPointFocused = { idx, frac, _ ->
                selectedIdx = idx
                anchorXFrac = frac
                showTooltip = true
              },
              onHoverEnded = { showTooltip = false },
            ),
      ) {
        val entrance = entranceState.value
        val pulseAlpha = pulseAlphaState.value
        val w = size.width
        val h = size.height
        for (g in 0..3) drawLine(gridC, Offset(0f, h * g / 3f), Offset(w, h * g / 3f), strokeWidth = 1f)
        val slotW = w / series.size.coerceAtLeast(1)
        val barW = (slotW * 0.42f).coerceIn(6f, 28f)
        val pts = ArrayList<Offset>(series.size)

        series.indices.forEach { i ->
          val stagger = ((entrance * 1.35f) - (i.toFloat() / (series.size + 2)) * 0.35f).coerceIn(0f, 1f)
          val dH = ((drainSeries[i] / 100.0).toFloat() * h * stagger).coerceIn(3f, h - 4f)
          val xLeft = i * slotW + (slotW - barW) / 2f
          drawRoundRect(
            color = drainColor.copy(alpha = if (i == selectedIdx) 0.78f else 0.32f),
            topLeft = Offset(xLeft, h - dH),
            size = Size(barW, dH),
            cornerRadius = CornerRadius(6f, 6f),
          )
          val y = h - ((effSeries[i] / maxEff).toFloat() * h * stagger).coerceIn(6f, h - 6f)
          pts.add(Offset(xLeft + barW / 2f, y))
        }
        clipRect(0f, 0f, w * entrance, h) {
          if (pts.size > 1) {
            val path = buildSmoothSplinePath(pts)
            drawPath(path, effColor.copy(alpha = 0.24f), style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round))
            drawPath(path, effColor, style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round))
          }
          pts.forEachIndexed { idx, pt ->
            if (idx == selectedIdx) {
              drawCircle(effColor.copy(alpha = 0.34f * pulseAlpha), radius = 10.dp.toPx(), center = pt)
            }
            drawCircle(effColor, radius = if (idx == selectedIdx) 5.dp.toPx() else 3.dp.toPx(), center = pt)
          }
        }
      }

      NeonChartTooltipOverlay(
        visible = showTooltip,
        anchorXFraction = anchorXFrac,
        title = inspected.title.ifBlank { "Pack Drain" },
        subtitle = if (startV > 0.0 && endV > 0.0) "${"%.1f".format(startV)}V → ${"%.1f".format(endV)}V" else "${inspected.whUsed.toInt()} Wh total",
        accentColor = effColor,
        metrics =
          listOf(
            NeonTooltipMetric("EFF", "${"%.1f".format(effSeries[selectedIdx.coerceIn(0, effSeries.lastIndex)])} Wh/$distUnit", effColor),
            NeonTooltipMetric("DRAIN", "-${drainSeries[selectedIdx].toInt()}%", drainColor),
          ),
        modifier = Modifier.align(Alignment.TopStart).padding(top = 2.dp),
      )
    }

    Surface(
      color = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(10.dp),
      border = BorderStroke(1.dp, effColor.copy(alpha = 0.25f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("${"%.1f".format(effSeries[selectedIdx.coerceIn(0, effSeries.lastIndex)])} Wh/$distUnit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = effColor, maxLines = 1, softWrap = false)
        Text(
          if (startV > 0.0 && endV > 0.0) "${"%.1f".format(startV)}V → ${"%.1f".format(endV)}V (-${drainSeries[selectedIdx].toInt()}%)"
          else "${inspected.whUsed.toInt()} Wh used",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = drainColor,
          maxLines = 1,
          softWrap = false,
        )
      }
    }
  }
}

/**
 * 4. DISTANCE VS ENERGY (Wh) GROUPED BAR CHART (Animated + Floating Neon Hover-Tooltip)
 */
@Composable
internal fun HistoricalDistanceEnergyChart(
  rides: List<RideSession>,
  settings: MasterSettings,
  chartHeight: Dp = 155.dp,
  modifier: Modifier = Modifier,
) {
  val bars = remember(rides) { rides.take(10).reversed() }
  if (bars.isEmpty()) return
  val barsKey = "${bars.size}_${bars.first().id}_${bars.last().id}"
  val entranceState = rememberChartEntranceState(triggerKey = barsKey, reducedMotion = settings.reducedMotion, durationMs = 680)

  var selectedIdx by remember(barsKey) { mutableIntStateOf(bars.lastIndex) }
  var anchorXFrac by remember(barsKey) { mutableFloatStateOf(1f) }
  var showTooltip by remember(barsKey) { mutableStateOf(false) }
  val pulseAlphaState = rememberNeonPulseAlphaState(active = showTooltip, reducedMotion = settings.reducedMotion)

  val distUnit = distUnitLabel(settings.distanceUnit)
  val maxDist = max(0.5, bars.maxOfOrNull { formatDistValue(it.distanceKm, settings.distanceUnit) } ?: 1.0)
  val maxWh = max(50.0, bars.maxOfOrNull { it.whUsed } ?: 100.0)
  val distC = MaterialTheme.colorScheme.primary
  val whC = MaterialTheme.colorScheme.tertiary
  val gridC = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
  val inspected = bars[selectedIdx.coerceIn(0, bars.lastIndex)]

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text("Distance ($distUnit) vs Energy (Wh)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("■ Dist", style = MaterialTheme.typography.labelSmall, color = distC, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        Text("■ Wh", style = MaterialTheme.typography.labelSmall, color = whC, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
      }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
      Canvas(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(chartHeight)
            .chartHoverAndScrub(
              itemCount = bars.size,
              useSlots = true,
              onPointFocused = { idx, frac, _ ->
                selectedIdx = idx
                anchorXFrac = frac
                showTooltip = true
              },
              onHoverEnded = { showTooltip = false },
            ),
      ) {
        val entrance = entranceState.value
        val pulseAlpha = pulseAlphaState.value
        val h = size.height
        for (g in 0..3) drawLine(gridC, Offset(0f, h * g / 3f), Offset(size.width, h * g / 3f), strokeWidth = 1f)
        val groupW = size.width / bars.size.coerceAtLeast(1)
        val barW = (groupW * 0.34f).coerceIn(6f, 26f)
        bars.forEachIndexed { idx, ride ->
          val stagger = ((entrance * 1.35f) - (idx.toFloat() / (bars.size + 2)) * 0.35f).coerceIn(0f, 1f)
          val dNorm = (formatDistValue(ride.distanceKm, settings.distanceUnit) / maxDist).toFloat().coerceIn(0.05f, 1f)
          val wNorm = (ride.whUsed / maxWh).toFloat().coerceIn(0.05f, 1f)
          val xBase = idx * groupW + (groupW - (barW * 2f + 4f)) / 2f
          val dH = h * dNorm * stagger
          val wH = h * wNorm * stagger
          val isSel = idx == selectedIdx
          if (isSel && showTooltip) {
            drawRoundRect(
              color = distC.copy(alpha = 0.14f * pulseAlpha),
              topLeft = Offset(xBase - 4f, 0f),
              size = Size(barW * 2f + 12f, h),
              cornerRadius = CornerRadius(10f, 10f),
            )
          }
          drawRoundRect(
            color = if (isSel || !showTooltip) distC else distC.copy(alpha = 0.55f),
            topLeft = Offset(xBase, h - dH),
            size = Size(barW, dH),
            cornerRadius = CornerRadius(6f, 6f),
          )
          drawRoundRect(
            color = if (isSel || !showTooltip) whC else whC.copy(alpha = 0.55f),
            topLeft = Offset(xBase + barW + 4f, h - wH),
            size = Size(barW, wH),
            cornerRadius = CornerRadius(6f, 6f),
          )
        }
      }

      NeonChartTooltipOverlay(
        visible = showTooltip,
        anchorXFraction = anchorXFrac,
        title = inspected.title.ifBlank { "Trail Ride" },
        subtitle = inspected.vehicleName,
        accentColor = distC,
        metrics =
          listOf(
            NeonTooltipMetric("DIST", "${"%.2f".format(formatDistValue(inspected.distanceKm, settings.distanceUnit))} $distUnit", distC),
            NeonTooltipMetric("ENERGY", "${inspected.whUsed.toInt()} Wh", whC),
          ),
        modifier = Modifier.align(Alignment.TopStart).padding(top = 2.dp),
      )
    }

    Text(
      "Longest: ${"%.2f".format(bars.maxOfOrNull { formatDistValue(it.distanceKm, settings.distanceUnit) } ?: 0.0)} $distUnit • Peak Draw: ${(bars.maxOfOrNull { it.whUsed } ?: 0.0).toInt()} Wh",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

/**
 * 5. SPEED VS EFFICIENCY 2D SCATTER PLOT (Animated Radial Pop-In + Nearest-Bubble Hover-Tooltip)
 */
@Composable
internal fun SpeedVsEfficiencyScatterChart(
  rides: List<RideSession>,
  settings: MasterSettings,
  chartHeight: Dp = 165.dp,
  modifier: Modifier = Modifier,
) {
  if (rides.isEmpty()) return
  val scatterKey = "${rides.size}_${rides.first().id}"
  val entranceState = rememberChartEntranceState(triggerKey = scatterKey, reducedMotion = settings.reducedMotion, durationMs = 660)

  val spdUnit = speedUnitLabel(settings.distanceUnit)
  val distUnit = distUnitLabel(settings.distanceUnit)
  val primaryC = MaterialTheme.colorScheme.primary
  val secondaryC = MaterialTheme.colorScheme.secondary
  val tertiaryC = MaterialTheme.colorScheme.tertiary
  val gridC = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

  val points =
    remember(rides, settings.distanceUnit) {
      rides.take(24).map { r ->
        val xSpd = formatSpeedValue(r.avgSpeedKmh, settings.distanceUnit)
        val yEff =
          if (settings.distanceUnit == "km") PevBatteryPhysics.whPerKm(r.whUsed, r.distanceKm)
          else PevBatteryPhysics.whPerMile(r.whUsed, r.distanceKm)
        Triple(r, xSpd, yEff)
      }
    }
  val maxX = max(15.0, (points.maxOfOrNull { it.second } ?: 25.0) * 1.15)
  val maxY = max(25.0, (points.maxOfOrNull { it.third } ?: 40.0) * 1.20)
  val maxDist = max(1.0, points.maxOfOrNull { it.first.distanceKm } ?: 5.0)

  var focusedIdx by remember(scatterKey) { mutableStateOf<Int?>(null) }
  var anchorXFrac by remember { mutableFloatStateOf(0.5f) }
  val pulseAlphaState = rememberNeonPulseAlphaState(active = focusedIdx != null, reducedMotion = settings.reducedMotion)
  val focusedPt = focusedIdx?.let { points.getOrNull(it) }

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text("X: Avg Speed ($spdUnit) • Y: Wh/$distUnit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
      Text("Tap Bubble", style = MaterialTheme.typography.labelSmall, color = primaryC, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
    }

    Box(modifier = Modifier.fillMaxWidth()) {
      Canvas(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(chartHeight)
            .chartPointer2D(
              key = scatterKey,
              onPointerAt = { pos, w, h, _ ->
                var bestIdx = -1
                var bestDist = Float.MAX_VALUE
                points.forEachIndexed { idx, (_, xSpd, yEff) ->
                  val cx = ((xSpd / maxX).toFloat() * (w - 24f) + 12f).coerceIn(12f, w - 12f)
                  val cy = (h - ((yEff / maxY).toFloat() * (h - 24f)) - 12f).coerceIn(12f, h - 12f)
                  val d = hypot(pos.x - cx, pos.y - cy)
                  if (d < bestDist) {
                    bestDist = d
                    bestIdx = idx
                  }
                }
                if (bestIdx >= 0 && bestDist < 95f) {
                  if (focusedIdx != bestIdx) focusedIdx = bestIdx
                  anchorXFrac = (pos.x / w).coerceIn(0f, 1f)
                }
              },
              onHoverEnded = { focusedIdx = null },
            ),
      ) {
        val entrance = entranceState.value
        val pulseAlpha = pulseAlphaState.value
        val w = size.width
        val h = size.height
        for (g in 0..3) {
          drawLine(gridC, Offset(0f, h * g / 3f), Offset(w, h * g / 3f), strokeWidth = 1f)
          drawLine(gridC, Offset(w * g / 3f, 0f), Offset(w * g / 3f, h), strokeWidth = 1f)
        }
        drawRect(
          color = primaryC.copy(alpha = 0.08f * entrance),
          topLeft = Offset(w * 0.25f, h * 0.55f),
          size = Size(w * 0.50f, h * 0.45f),
        )
        points.forEachIndexed { idx, (ride, xSpd, yEff) ->
          val stagger = ((entrance * 1.4f) - (idx.toFloat() / (points.size + 4)) * 0.4f).coerceIn(0f, 1f)
          val cx = ((xSpd / maxX).toFloat() * (w - 24f) + 12f).coerceIn(12f, w - 12f)
          val cy = (h - ((yEff / maxY).toFloat() * (h - 24f)) - 12f).coerceIn(12f, h - 12f)
          val radius = ((5.dp.toPx() + (ride.distanceKm / maxDist).toFloat() * 9.dp.toPx()).coerceIn(5.dp.toPx(), 15.dp.toPx())) * stagger
          val col =
            when {
              yEff < maxY * 0.40 -> primaryC
              yEff < maxY * 0.70 -> secondaryC
              else -> tertiaryC
            }
          val isFocused = focusedIdx == idx
          if (isFocused) {
            drawLine(col.copy(alpha = 0.45f), Offset(cx, 0f), Offset(cx, h), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
            drawLine(col.copy(alpha = 0.45f), Offset(0f, cy), Offset(w, cy), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
            drawCircle(col.copy(alpha = 0.34f * pulseAlpha), radius = radius + 7.dp.toPx(), center = Offset(cx, cy))
          }
          drawCircle(col.copy(alpha = if (isFocused) 0.58f else 0.34f), radius = radius, center = Offset(cx, cy))
          drawCircle(col, radius = radius, center = Offset(cx, cy), style = Stroke(width = if (isFocused) 2.8.dp.toPx() else 2.dp.toPx()))
        }
      }

      NeonChartTooltipOverlay(
        visible = focusedPt != null,
        anchorXFraction = anchorXFrac,
        title = focusedPt?.first?.title?.ifBlank { "Ride" }.orEmpty(),
        subtitle = focusedPt?.first?.vehicleName.orEmpty(),
        accentColor = primaryC,
        metrics =
          if (focusedPt != null) {
            listOf(
              NeonTooltipMetric("AVG", "${"%.1f".format(focusedPt.second)} $spdUnit", secondaryC),
              NeonTooltipMetric("EFF", "${"%.1f".format(focusedPt.third)} Wh/$distUnit", primaryC),
              NeonTooltipMetric("DIST", "${"%.1f".format(formatDistValue(focusedPt.first.distanceKm, settings.distanceUnit))} $distUnit", tertiaryC),
            )
          } else emptyList(),
        modifier = Modifier.align(Alignment.TopStart).padding(top = 2.dp),
      )
    }

    Text(
      "Lower-right dots = faster pace with lower Wh/$distUnit (your PEV's optimal cruise sweet spot).",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

/**
 * 6. RIDE DURATION DISTRIBUTION HISTOGRAM (Staggered Neon Bars + Hover-Tooltip)
 */
@Composable
internal fun RideDurationHistogramChart(
  rides: List<RideSession>,
  reducedMotion: Boolean = false,
  chartHeight: Dp = 130.dp,
  modifier: Modifier = Modifier,
) {
  if (rides.isEmpty()) return
  val histKey = "${rides.size}_${rides.first().id}"
  val entranceState = rememberChartEntranceState(triggerKey = histKey, reducedMotion = reducedMotion, durationMs = 660)

  val buckets =
    remember(rides) {
      listOf(
        "<15m" to rides.count { it.durationSec < 900 },
        "15–30m" to rides.count { it.durationSec in 900 until 1800 },
        "30–60m" to rides.count { it.durationSec in 1800 until 3600 },
        "60m+" to rides.count { it.durationSec >= 3600 },
      )
    }
  val maxCount = max(1, buckets.maxOf { it.second })
  val colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, Color(0xFF38BDF8), MaterialTheme.colorScheme.tertiary)
  var hoveredIdx by remember { mutableStateOf<Int?>(null) }
  var anchorXFrac by remember { mutableFloatStateOf(0.5f) }
  val pulseAlphaState = rememberNeonPulseAlphaState(active = hoveredIdx != null, reducedMotion = reducedMotion)

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Box(modifier = Modifier.fillMaxWidth()) {
      Canvas(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(chartHeight)
            .chartHoverAndScrub(
              itemCount = buckets.size,
              useSlots = true,
              onPointFocused = { idx, frac, _ ->
                hoveredIdx = idx
                anchorXFrac = frac
              },
              onHoverEnded = { hoveredIdx = null },
            ),
      ) {
        val entrance = entranceState.value
        val pulseAlpha = pulseAlphaState.value
        val slotW = size.width / buckets.size
        val barW = (slotW * 0.56f).coerceIn(16f, 64f)
        buckets.forEachIndexed { idx, (_, count) ->
          val stagger = ((entrance * 1.3f) - idx * 0.1f).coerceIn(0f, 1f)
          val frac = (count.toFloat() / maxCount.toFloat()).coerceIn(0.06f, 1f)
          val bH = size.height * frac * stagger
          val xLeft = idx * slotW + (slotW - barW) / 2f
          val isFocused = hoveredIdx == idx
          if (isFocused) {
            drawRoundRect(
              color = colors[idx].copy(alpha = 0.24f * pulseAlpha),
              topLeft = Offset(xLeft - 4.dp.toPx(), size.height - bH - 4.dp.toPx()),
              size = Size(barW + 8.dp.toPx(), bH + 4.dp.toPx()),
              cornerRadius = CornerRadius(12f, 12f),
            )
          }
          drawRoundRect(
            brush = Brush.verticalGradient(listOf(colors[idx], colors[idx].copy(alpha = 0.25f))),
            topLeft = Offset(xLeft, size.height - bH),
            size = Size(barW, bH),
            cornerRadius = CornerRadius(10f, 10f),
          )
        }
      }

      val hIdx = hoveredIdx
      NeonChartTooltipOverlay(
        visible = hIdx != null,
        anchorXFraction = anchorXFrac,
        title = if (hIdx != null) "Duration ${buckets[hIdx].first}" else "",
        accentColor = if (hIdx != null) colors[hIdx] else MaterialTheme.colorScheme.primary,
        metrics =
          if (hIdx != null) {
            val cnt = buckets[hIdx].second
            val pct = ((cnt.toDouble() / rides.size.coerceAtLeast(1)) * 100.0).toInt()
            listOf(
              NeonTooltipMetric("RIDES", "$cnt", colors[hIdx]),
              NeonTooltipMetric("SHARE", "$pct%", MaterialTheme.colorScheme.onSurface),
            )
          } else emptyList(),
        modifier = Modifier.align(Alignment.TopStart).padding(top = 2.dp),
      )
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
      buckets.forEachIndexed { idx, (label, count) ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("$count", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = colors[idx], maxLines = 1, softWrap = false)
          Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
        }
      }
    }
  }
}

private fun downsampleTrackPoints(points: List<GeoPoint>, maxPoints: Int = 320): List<GeoPoint> {
  if (points.size <= maxPoints) return points
  val step = (points.size - 1).toDouble() / (maxPoints - 1).toDouble()
  return List(maxPoints) { idx ->
    points[(idx * step).toInt().coerceIn(0, points.lastIndex)]
  }
}

private fun safeFitMapToPoints(map: MapView, pts: List<GeoPoint>, animated: Boolean) {
  if (pts.isEmpty()) return
  if (pts.size >= 2) {
    runCatching {
      val bb = BoundingBox.fromGeoPoints(pts)
      if (bb.latitudeSpan > 0.0002 || bb.longitudeSpan > 0.0002) {
        map.post { runCatching { map.zoomToBoundingBox(bb, animated, 64) } }
      } else {
        if (animated) map.controller.animateTo(pts.first(), 15.5, 400L)
        else {
          map.controller.setCenter(pts.first())
          map.controller.setZoom(15.5)
        }
      }
    }.onFailure {
      map.controller.setCenter(pts.first())
      map.controller.setZoom(14.5)
    }
  } else {
    if (animated) map.controller.animateTo(pts.first(), 15.0, 400L)
    else {
      map.controller.setCenter(pts.first())
      map.controller.setZoom(15.0)
    }
  }
}

/**
 * SINGLE-RIDE INTERACTIVE OSMDROID TRACK MAP (Zero-Freeze Memoized Overlays + Lifecycle Safe)
 */
@Composable
internal fun RideTrackMapView(
  ride: RideSession,
  allOverlayRides: List<RideSession> = emptyList(),
  settings: MasterSettings,
  height: Dp = 220.dp,
  modifier: Modifier = Modifier,
) {
  val trackPts = remember(ride.id, ride.polylinePoints.size) { downsampleTrackPoints(parseRideTrackPoints(ride)) }
  val overlayTracks =
    remember(allOverlayRides.size, allOverlayRides.firstOrNull()?.id) {
      allOverlayRides.take(16).map { r -> r to downsampleTrackPoints(parseRideTrackPoints(r), 140) }.filter { it.second.size >= 2 }
    }
  val distUnit = distUnitLabel(settings.distanceUnit)
  val spdUnit = speedUnitLabel(settings.distanceUnit)

  if (trackPts.isEmpty() && overlayTracks.isEmpty()) {
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
      modifier = modifier.fillMaxWidth(),
    ) {
      Text(
        "No GPS polyline points saved for \"${ride.title}\". Record a trail with GPS lock to view your route track on the map.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(12.dp),
      )
    }
    return
  }

  var mapRef by remember { mutableStateOf<MapView?>(null) }
  var fittedKey by remember { mutableStateOf("") }
  var lastOverlaySig by remember { mutableStateOf("") }
  var userPanned by remember { mutableStateOf(false) }
  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(lifecycleOwner, mapRef) {
    val map = mapRef
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> map?.onResume()
        Lifecycle.Event.ON_PAUSE -> map?.onPause()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  val mapLayer = remember { defaultMapLayerId() }
  val primaryArgb = MaterialTheme.colorScheme.primary.toArgb()
  val secondaryArgb = MaterialTheme.colorScheme.secondary.toArgb()
  val surfaceArgb = MaterialTheme.colorScheme.surface.toArgb()
  val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

  val fitPoints = remember(trackPts, overlayTracks) {
    if (overlayTracks.isNotEmpty()) overlayTracks.flatMap { it.second } else trackPts
  }
  val fitKey = if (overlayTracks.isNotEmpty()) "ALL_${overlayTracks.size}" else ride.id
  val overlaySig = "$fitKey|${trackPts.size}|$mapLayer|$primaryArgb|$secondaryArgb|$isDark|${settings.themePreset.id}|${settings.distanceUnit}"

  Box(modifier = modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(12.dp))) {
    AndroidView(
      factory = { ctxMap ->
        createSafeMapView(ctxMap).also { map ->
          mapRef = map
          configureSmoothMapPanning(map) { userPanned = true }
        }
      },
      update = { map ->
        val desiredSource = resolveMapTileSource(mapLayer)
        if (map.tileProvider.tileSource.name() != desiredSource.name()) {
          map.setTileSource(desiredSource)
        }
        if (fittedKey != fitKey && !userPanned) {
          fittedKey = fitKey
          safeFitMapToPoints(map, fitPoints, animated = false)
        }
        if (lastOverlaySig == overlaySig) return@AndroidView
        lastOverlaySig = overlaySig
        applyThemeToMapView(map, settings, primaryArgb, surfaceArgb, isDark, mapLayer)
        map.overlays.clear()
        if (overlayTracks.isNotEmpty()) {
          overlayTracks.forEachIndexed { idx, (_, pts) ->
            map.overlays.add(
              Polyline().apply {
                outlinePaint.color = if (idx == 0) primaryArgb else secondaryArgb
                outlinePaint.strokeWidth = if (idx == 0) 7f else 5f
                setPoints(pts)
              }
            )
          }
        } else if (trackPts.size > 1) {
          map.overlays.add(
            Polyline().apply {
              outlinePaint.color = primaryArgb
              outlinePaint.strokeWidth = 7f
              setPoints(trackPts)
            }
          )
          map.overlays.add(
            Marker(map).apply {
              position = trackPts.first()
              title = "START • ${ride.title}"
            }
          )
          map.overlays.add(
            Marker(map).apply {
              position = trackPts.last()
              title = "FINISH • ${ride.title}"
            }
          )
        }
        map.invalidate()
      },
      onRelease = { map ->
        if (mapRef === map) mapRef = null
        runCatching { map.onPause() }
        runCatching { map.onDetach() }
      },
      modifier = Modifier.fillMaxSize().testTag("ride_track_map_view"),
    )

    Surface(
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
    ) {
      Text(
        if (overlayTracks.isNotEmpty()) "ALL TRAILS (${overlayTracks.size} GPS tracks)"
        else "${ride.vehicleName} • ${"%.2f".format(formatDistValue(ride.distanceKm, settings.distanceUnit))} $distUnit • Max ${"%.1f".format(formatSpeedValue(ride.maxSpeedKmh, settings.distanceUnit))} $spdUnit",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      )
    }

    Column(
      modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
      horizontalAlignment = Alignment.End,
    ) {
      Surface(
        onClick = {
          userPanned = false
          mapRef?.let { m -> safeFitMapToPoints(m, fitPoints, animated = true) }
        },
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.heightIn(min = 44.dp),
      ) {
        Row(
          Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(Icons.Default.MyLocation, contentDescription = "Fit Track", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
          Text("FIT TRACK", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary, maxLines = 1, softWrap = false)
        }
      }
      Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f), shape = RoundedCornerShape(12.dp)) {
        Row {
          IconButton(onClick = { mapRef?.controller?.zoomIn() }, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
          }
          IconButton(onClick = { mapRef?.controller?.zoomOut() }, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
          }
        }
      }
    }
  }
}

/**
 * DEDICATED PER-RIDE-LOG VISUAL SUITE (Animated Speed Graph, Elevation Graph, Pie Charts & Track Map!)
 */
@Composable
fun PerRideTelemetryPanel(
  ride: RideSession,
  activeVehicle: PevVehicle?,
  settings: MasterSettings,
  showAllStacked: Boolean = false,
  onOpenFullStudio: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  var subTab by remember(ride.id) { mutableIntStateOf(0) }
  var pieSubMode by remember(ride.id) { mutableIntStateOf(0) }

  val spdUnit = speedUnitLabel(settings.distanceUnit)
  val distUnit = distUnitLabel(settings.distanceUnit)
  val elevUnit = elevationUnitLabel(settings.distanceUnit)
  val nomV = activeVehicle?.voltage ?: 48.0

  val speedCurve = remember(ride.id, settings.distanceUnit) { buildRideSpeedProfile(ride, settings.distanceUnit) }
  val elevCurve = remember(ride.id, settings.distanceUnit) { buildRideElevationProfile(ride, settings.distanceUnit) }
  val primaryC = MaterialTheme.colorScheme.primary
  val secondaryC = MaterialTheme.colorScheme.secondary
  val tertiaryC = MaterialTheme.colorScheme.tertiary
  val elevC = Color(0xFF38BDF8)

  val energySlices = remember(ride.id, primaryC, secondaryC, tertiaryC) { computeRideEnergySplit(ride, primaryC, secondaryC, tertiaryC) }
  val usedPct =
    if (ride.startBatteryPct >= 0 && ride.endBatteryPct >= 0) (ride.startBatteryPct - ride.endBatteryPct).coerceAtLeast(1)
    else ((ride.whUsed / (activeVehicle?.batteryWh ?: 1500L).toDouble().coerceAtLeast(200.0)) * 100.0).toInt().coerceIn(1, 99)
  val remPct = (if (ride.endBatteryPct >= 0) ride.endBatteryPct else (100 - usedPct)).coerceIn(0, 100)
  val batterySlices =
    remember(usedPct, remPct, primaryC, tertiaryC) {
      listOf(
        PieSliceData("Pack Used ($usedPct%)", usedPct.toDouble(), tertiaryC),
        PieSliceData("Pack Reserve ($remPct%)", remPct.coerceAtLeast(1).toDouble(), primaryC),
      )
    }

  val startV = if (ride.startBatteryPct >= 0) PevBatteryPhysics.voltageForSoc(nomV, ride.startBatteryPct.toDouble(), settings.batteryChemistry) else 0.0
  val endV = if (ride.endBatteryPct >= 0) PevBatteryPhysics.voltageForSoc(nomV, ride.endBatteryPct.toDouble(), settings.batteryChemistry) else 0.0

  Surface(
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
    border = BorderStroke(1.dp, primaryC.copy(alpha = 0.35f)),
    shape = RoundedCornerShape(12.dp),
    modifier = modifier.fillMaxWidth().testTag("per_ride_telemetry_${ride.id}"),
  ) {
    Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      if (!showAllStacked) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text(
            "RIDE TELEMETRY GRAPHS & MAP",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = primaryC,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          if (onOpenFullStudio != null) {
            OutlinedButton(
              onClick = onOpenFullStudio,
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier.heightIn(min = 38.dp).testTag("open_ride_studio_${ride.id}"),
            ) {
              Icon(Icons.Default.OpenInFull, contentDescription = "Full Telemetry Studio", modifier = Modifier.size(14.dp))
              Spacer(Modifier.width(4.dp))
              Text("Full Studio", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
            }
          }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          item {
            FilterChip(
              selected = subTab == 0,
              onClick = { subTab = 0 },
              leadingIcon = { Icon(Icons.AutoMirrored.Filled.ShowChart, null, Modifier.size(15.dp)) },
              label = { Text("Speed", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
            )
          }
          item {
            FilterChip(
              selected = subTab == 1,
              onClick = { subTab = 1 },
              leadingIcon = { Icon(Icons.Default.Terrain, null, Modifier.size(15.dp)) },
              label = { Text("Elevation", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
            )
          }
          item {
            FilterChip(
              selected = subTab == 2,
              onClick = { subTab = 2 },
              leadingIcon = { Icon(Icons.Default.PieChart, null, Modifier.size(15.dp)) },
              label = { Text("Pie Chart", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
            )
          }
          item {
            FilterChip(
              selected = subTab == 3,
              onClick = { subTab = 3 },
              leadingIcon = { Icon(Icons.Default.Map, null, Modifier.size(15.dp)) },
              label = { Text("Track Map", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
            )
          }
        }
      }

      if (showAllStacked || subTab == 0) {
        if (showAllStacked) SectionSubHeader(Icons.AutoMirrored.Filled.ShowChart, "SPEED PROFILE ($spdUnit)", primaryC)
        SingleCurveAreaChart(
          triggerKey = "${ride.id}_speed",
          values = speedCurve,
          metricLabel = "SPEED",
          unitLabel = spdUnit,
          accentColor = primaryC,
          referenceLineValue = formatSpeedValue(ride.avgSpeedKmh, settings.distanceUnit),
          referenceColor = secondaryC,
          reducedMotion = settings.reducedMotion,
          height = if (showAllStacked) 155.dp else 125.dp,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Peak: ${"%.1f".format(formatSpeedValue(ride.maxSpeedKmh, settings.distanceUnit))} $spdUnit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = primaryC, maxLines = 1, softWrap = false)
          Text("Avg: ${"%.1f".format(formatSpeedValue(ride.avgSpeedKmh, settings.distanceUnit))} $spdUnit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = secondaryC, maxLines = 1, softWrap = false)
          Text("Time: ${ride.durationSec / 60}m ${ride.durationSec % 60}s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
        }
      }

      if (showAllStacked || subTab == 1) {
        if (showAllStacked) {
          Spacer(Modifier.height(4.dp))
          SectionSubHeader(Icons.Default.Terrain, "ELEVATION GAIN PROFILE (+$elevUnit)", elevC)
        }
        SingleCurveAreaChart(
          triggerKey = "${ride.id}_elev",
          values = elevCurve,
          metricLabel = "CLIMB",
          unitLabel = elevUnit,
          accentColor = elevC,
          referenceLineValue = null,
          referenceColor = elevC,
          reducedMotion = settings.reducedMotion,
          height = if (showAllStacked) 145.dp else 120.dp,
        )
        val distVal = formatDistValue(ride.distanceKm, settings.distanceUnit)
        val totalElevVal = formatElevationValue(ride.elevationGainM, settings.distanceUnit)
        val climbRate = if (distVal > 0.05) totalElevVal / distVal else 0.0
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Total Gain: +${totalElevVal.toInt()} $elevUnit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = elevC, maxLines = 1, softWrap = false)
          Text("Climb Rate: ${"%.0f".format(climbRate)} $elevUnit/$distUnit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
        }
      }

      if (showAllStacked || subTab == 2) {
        if (showAllStacked) {
          Spacer(Modifier.height(4.dp))
          SectionSubHeader(Icons.Default.PieChart, "ENERGY & BATTERY PIE BREAKDOWN", tertiaryC)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          FilterChip(
            selected = pieSubMode == 0,
            onClick = { pieSubMode = 0 },
            label = { Text("Battery Pack", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
          )
          FilterChip(
            selected = pieSubMode == 1,
            onClick = { pieSubMode = 1 },
            label = { Text("Energy Split (Wh)", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
          )
        }
        if (pieSubMode == 0) {
          TelemetryDonutPieChart(
            slices = batterySlices,
            centerValue = "-$usedPct%",
            centerLabel = "${ride.whUsed.toInt()} Wh",
            reducedMotion = settings.reducedMotion,
            chartSize = if (showAllStacked) 136.dp else 112.dp,
          )
          if (startV > 0.0 && endV > 0.0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Start: ${ride.startBatteryPct}% (${"%.1f".format(startV)}V)", style = MaterialTheme.typography.labelSmall, color = primaryC, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
              Text("End: ${ride.endBatteryPct}% (${"%.1f".format(endV)}V)", style = MaterialTheme.typography.labelSmall, color = tertiaryC, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }
          }
        } else {
          TelemetryDonutPieChart(
            slices = energySlices,
            centerValue = "${ride.whUsed.toInt()}",
            centerLabel = "Wh Total",
            reducedMotion = settings.reducedMotion,
            chartSize = if (showAllStacked) 136.dp else 112.dp,
          )
        }
      }

      if (showAllStacked || subTab == 3) {
        if (showAllStacked) {
          Spacer(Modifier.height(4.dp))
          SectionSubHeader(Icons.Default.Map, "GPS ROUTE TRACK MAP", primaryC)
        }
        RideTrackMapView(
          ride = ride,
          settings = settings,
          height = if (showAllStacked) 250.dp else 195.dp,
        )
      }
    }
  }
}

@Composable
private fun SectionSubHeader(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  accent: Color,
) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
    Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
  }
}

@Composable
private fun SingleCurveAreaChart(
  triggerKey: Any?,
  values: List<Double>,
  metricLabel: String,
  unitLabel: String,
  accentColor: Color,
  referenceLineValue: Double?,
  referenceColor: Color,
  reducedMotion: Boolean,
  height: Dp,
) {
  if (values.isEmpty()) return
  val entranceState = rememberChartEntranceState(triggerKey = triggerKey, reducedMotion = reducedMotion, durationMs = 660)
  val maxV = max(5.0, (values.maxOrNull() ?: 10.0) * 1.15)
  val gridC = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)

  var hoveredIdx by remember(triggerKey) { mutableStateOf<Int?>(null) }
  var anchorXFrac by remember { mutableFloatStateOf(0.5f) }
  val pulseAlphaState = rememberNeonPulseAlphaState(active = hoveredIdx != null, reducedMotion = reducedMotion)

  Box(modifier = Modifier.fillMaxWidth()) {
    Canvas(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(height)
          .chartHoverAndScrub(
            itemCount = values.size,
            useSlots = false,
            onPointFocused = { idx, frac, _ ->
              hoveredIdx = idx
              anchorXFrac = frac
            },
            onHoverEnded = { hoveredIdx = null },
          ),
    ) {
      val entrance = entranceState.value
      val pulseAlpha = pulseAlphaState.value
      val w = size.width
      val h = size.height
      for (g in 0..3) drawLine(gridC, Offset(0f, h * g / 3f), Offset(w, h * g / 3f), strokeWidth = 1f)
      if (referenceLineValue != null && referenceLineValue > 0.0) {
        val ry = h - ((referenceLineValue / maxV).toFloat() * h).coerceIn(6f, h - 6f)
        drawLine(
          color = referenceColor.copy(alpha = 0.7f * entrance),
          start = Offset(0f, ry),
          end = Offset(w, ry),
          strokeWidth = 1.6.dp.toPx(),
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
        )
      }
      clipRect(0f, 0f, w * entrance, h) {
        if (values.size > 1) {
          val stepX = w / (values.size - 1)
          val pts = values.mapIndexed { i, v -> Offset(i * stepX, h - ((v / maxV).toFloat() * h * entrance).coerceIn(6f, h - 6f)) }
          val linePath = buildSmoothSplinePath(pts)
          val fillPath = Path().apply {
            addPath(linePath)
            lineTo(pts.last().x, h)
            lineTo(pts.first().x, h)
            close()
          }
          drawPath(fillPath, brush = Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.36f), Color.Transparent)))
          drawPath(linePath, accentColor.copy(alpha = 0.24f), style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round))
          drawPath(linePath, accentColor, style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round))

          val focusPt = hoveredIdx?.let { pts.getOrNull(it) } ?: pts.minByOrNull { it.y }
          if (focusPt != null) {
            if (hoveredIdx != null) {
              drawLine(
                color = accentColor.copy(alpha = 0.75f),
                start = Offset(focusPt.x, 0f),
                end = Offset(focusPt.x, h),
                strokeWidth = 1.4.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
              )
            }
            drawCircle(accentColor.copy(alpha = 0.32f * pulseAlpha), radius = 10.dp.toPx(), center = focusPt)
            drawCircle(accentColor, radius = 4.8.dp.toPx(), center = focusPt)
          }
        }
      }
    }

    val hIdx = hoveredIdx
    NeonChartTooltipOverlay(
      visible = hIdx != null,
      anchorXFraction = anchorXFrac,
      title = if (hIdx != null) "Progress ${((hIdx.toFloat() / (values.size - 1).coerceAtLeast(1)) * 100).toInt()}%" else "",
      accentColor = accentColor,
      metrics =
        if (hIdx != null) {
          listOf(
            NeonTooltipMetric(metricLabel, "${"%.1f".format(values[hIdx])} $unitLabel", accentColor),
          )
        } else emptyList(),
      modifier = Modifier.align(Alignment.TopStart).padding(top = 2.dp),
    )
  }
}

internal fun buildSmoothSplinePath(points: List<Offset>): Path {
  val path = Path()
  if (points.isEmpty()) return path
  path.moveTo(points.first().x, points.first().y)
  for (i in 0 until points.lastIndex) {
    val p0 = points[i]
    val p1 = points[i + 1]
    val midX = (p0.x + p1.x) / 2f
    path.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
  }
  return path
}
