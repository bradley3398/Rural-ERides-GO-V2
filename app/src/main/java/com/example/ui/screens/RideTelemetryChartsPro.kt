package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.PevBatteryPhysics
import com.example.data.MasterSettings
import com.example.data.PevVehicle
import com.example.data.RideSession
import com.example.ui.components.distUnitLabel
import com.example.ui.components.formatDistValue
import com.example.ui.components.formatSpeedValue
import com.example.ui.components.speedUnitLabel
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * 8. POWER (WATTS) & BATTERY PACK AMPS DRAW GRAPH (Zero-Recomposition Draw Phase)
 */
@Composable
internal fun HistoricalPowerAmpsChart(
  rides: List<RideSession>,
  activeVehicle: PevVehicle?,
  settings: MasterSettings,
  chartHeight: Dp = 170.dp,
  modifier: Modifier = Modifier,
  onTap: (() -> Unit)? = null,
) {
  val series = remember(rides) { rides.take(14).reversed() }
  if (series.isEmpty()) return
  val triggerKey = remember(series) { "${series.size}_${series.first().id}_${series.last().id}" }
  val entranceState = rememberChartEntranceState(triggerKey = triggerKey, reducedMotion = settings.reducedMotion, durationMs = 600)

  var selectedIdx by remember(series.size) { mutableIntStateOf(series.lastIndex) }
  var anchorXFrac by remember(series.size) { mutableFloatStateOf(1f) }
  var showTooltip by remember(series.size) { mutableStateOf(false) }
  val pulseState = rememberNeonPulseAlphaState(active = showTooltip, reducedMotion = settings.reducedMotion)

  val packV = (activeVehicle?.voltage ?: 52.0).coerceAtLeast(24.0)
  val avgWatts =
    remember(series) {
      series.map { r ->
        val hrs = (r.durationSec.toDouble() / 3600.0).coerceAtLeast(0.02)
        (r.whUsed / hrs).coerceIn(40.0, 6500.0)
      }
    }
  val estAmps = remember(avgWatts, packV) { avgWatts.map { (it / packV).coerceIn(1.0, 120.0) } }
  val maxW = max(350.0, (avgWatts.maxOrNull() ?: 600.0) * 1.18)
  val maxA = max(10.0, (estAmps.maxOrNull() ?: 15.0) * 1.2)

  val wattColor = Color(0xFFFF6B35)
  val ampColor = Color(0xFF00F0FF)
  val gridC = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
  val safeIdx = selectedIdx.coerceIn(0, series.lastIndex)
  val inspected = series[safeIdx]

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text(
        "Avg Motor Load (W) & Pack Current (A @ ${packV.toInt()}V)",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("■ Watts", style = MaterialTheme.typography.labelSmall, color = wattColor, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        Text("● Amps", style = MaterialTheme.typography.labelSmall, color = ampColor, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
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
              onTap = onTap,
            ),
      ) {
        val entrance = entranceState.value
        val pulseAlpha = pulseState.value
        val w = size.width
        val h = size.height
        if (w < 24f || h < 24f) return@Canvas
        for (g in 0..3) drawLine(gridC, Offset(0f, h * g / 3f), Offset(w, h * g / 3f), strokeWidth = 1f)

        val slotW = w / series.size.coerceAtLeast(1)
        val barW = (slotW * 0.52f).coerceIn(6f, 34f)
        val ampPts = ArrayList<Offset>(series.size)
        val maxBarH = (h - 4f).coerceAtLeast(4f)
        val maxAmpY = (h - 6f).coerceAtLeast(6f)

        avgWatts.forEachIndexed { idx, watts ->
          val stagger = ((entrance * 1.3f) - (idx.toFloat() / (series.size + 2)) * 0.3f).coerceIn(0f, 1f)
          val rawBar = ((watts / maxW).toFloat() * h * stagger).let { if (it.isFinite()) it else 4f }
          val bH = rawBar.coerceIn(4f, maxBarH)
          val xLeft = idx * slotW + (slotW - barW) / 2f
          val isSel = idx == safeIdx
          if (isSel && showTooltip) {
            drawRoundRect(
              color = wattColor.copy(alpha = 0.24f * pulseAlpha),
              topLeft = Offset(xLeft - 3.dp.toPx(), (h - bH - 3.dp.toPx()).coerceAtLeast(0f)),
              size = Size(barW + 6.dp.toPx(), bH + 3.dp.toPx()),
              cornerRadius = CornerRadius(10f, 10f),
            )
          }
          drawRoundRect(
            brush = Brush.verticalGradient(listOf(wattColor.copy(alpha = if (isSel) 0.95f else 0.62f), wattColor.copy(alpha = 0.18f))),
            topLeft = Offset(xLeft, h - bH),
            size = Size(barW, bH),
            cornerRadius = CornerRadius(8f, 8f),
          )
          val rawAmp = ((estAmps[idx] / maxA).toFloat() * h * stagger).let { if (it.isFinite()) it else 6f }
          val aY = h - rawAmp.coerceIn(6f, maxAmpY)
          ampPts.add(Offset(xLeft + barW / 2f, aY))
        }

        clipRect(0f, 0f, w * entrance, h) {
          if (ampPts.size > 1) {
            val spline = buildSmoothSplinePath(ampPts)
            drawPath(spline, ampColor.copy(alpha = 0.22f), style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round))
            drawPath(spline, ampColor, style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round))
          }
          ampPts.forEachIndexed { idx, pt ->
            if (idx == safeIdx) {
              drawCircle(ampColor.copy(alpha = 0.30f * pulseAlpha), radius = 9.dp.toPx(), center = pt)
            }
            drawCircle(ampColor, radius = if (idx == safeIdx) 5.dp.toPx() else 3.dp.toPx(), center = pt)
          }
        }
      }

      NeonChartTooltipOverlay(
        visible = showTooltip,
        anchorXFraction = anchorXFrac,
        title = inspected.title.ifBlank { "Trail Load" },
        subtitle = inspected.vehicleName,
        accentColor = wattColor,
        metrics =
          listOf(
            NeonTooltipMetric("POWER", "${avgWatts[safeIdx].toInt()} W", wattColor),
            NeonTooltipMetric("DRAW", "${"%.1f".format(estAmps[safeIdx])} A", ampColor),
          ),
        modifier = Modifier.align(Alignment.TopStart).padding(top = 2.dp),
      )
    }

    Surface(
      color = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(10.dp),
      border = BorderStroke(1.dp, wattColor.copy(alpha = 0.30f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Avg Load: ${avgWatts[safeIdx].toInt()} W", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = wattColor, maxLines = 1, softWrap = false)
        Text("Pack Current: ${"%.1f".format(estAmps[safeIdx])} A", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ampColor, maxLines = 1, softWrap = false)
      }
    }
  }
}

internal data class RiderRadarScores(
  val latest: List<Float>,
  val fleetAvg: List<Float>,
  val axisLabels: List<String>,
  val peakLabel: String,
  val avgPctLabel: String,
  val minLabel: String,
)

internal fun computeRiderRadarScores(rides: List<RideSession>): RiderRadarScores {
  val axes = listOf("Speed", "Range", "Climb", "Efficiency", "Endurance")
  if (rides.isEmpty()) {
    return RiderRadarScores(
      latest = emptyList(),
      fleetAvg = emptyList(),
      axisLabels = axes,
      peakLabel = "0%",
      avgPctLabel = "0%",
      minLabel = "0%",
    )
  }
  val latest = rides.first()
  val maxSpd = max(25.0, rides.maxOfOrNull { it.maxSpeedKmh.takeIf { v -> v.isFinite() } ?: 0.0 } ?: 45.0)
  val maxDist = max(5.0, rides.maxOfOrNull { it.distanceKm.takeIf { v -> v.isFinite() } ?: 0.0 } ?: 15.0)
  val maxElev = max(40.0, rides.maxOfOrNull { it.elevationGainM.takeIf { v -> v.isFinite() } ?: 0.0 } ?: 120.0)
  val maxDur = max(600.0, rides.maxOfOrNull { it.durationSec.toDouble() } ?: 1800.0)

  fun safeRatio(num: Double, den: Double): Float {
    val r = if (den > 0.0 && num.isFinite()) num / den else 0.25
    return (if (r.isFinite()) r.toFloat() else 0.25f).coerceIn(0.15f, 1f)
  }

  fun effScore(r: RideSession): Float {
    val whKm = PevBatteryPhysics.whPerKm(r.whUsed, r.distanceKm).let { if (it.isFinite()) it else 25.0 }
    return ((42.0 - whKm) / 30.0).toFloat().coerceIn(0.18f, 1f)
  }

  val lScores = listOf(
    safeRatio(latest.maxSpeedKmh, maxSpd),
    safeRatio(latest.distanceKm, maxDist),
    safeRatio(latest.elevationGainM, maxElev),
    effScore(latest),
    safeRatio(latest.durationSec.toDouble(), maxDur),
  )
  val fScores = listOf(
    safeRatio(rides.map { it.maxSpeedKmh }.filter { it.isFinite() }.average(), maxSpd),
    safeRatio(rides.map { it.distanceKm }.filter { it.isFinite() }.average(), maxDist),
    safeRatio(rides.map { it.elevationGainM }.filter { it.isFinite() }.average(), maxElev),
    rides.map { effScore(it).toDouble() }.average().let { if (it.isFinite()) it.toFloat() else 0.5f }.coerceIn(0.15f, 1f),
    safeRatio(rides.map { it.durationSec.toDouble() }.average(), maxDur),
  )

  val maxIdx = lScores.indices.maxByOrNull { lScores[it] } ?: 0
  val minIdx = lScores.indices.minByOrNull { lScores[it] } ?: 0
  val avgPct = (lScores.average() * 100.0).let { if (it.isFinite()) it else 0.0 }
  return RiderRadarScores(
    latest = lScores,
    fleetAvg = fScores,
    axisLabels = axes,
    peakLabel = "${(lScores[maxIdx] * 100f).toInt()}% ${axes[maxIdx]}",
    avgPctLabel = "${"%.1f".format(avgPct)}% Overall",
    minLabel = "${(lScores[minIdx] * 100f).toInt()}% ${axes[minIdx]}",
  )
}

/**
 * 9. RIDER 5-AXIS PERFORMANCE SPIDER / RADAR PENTAGON CHART (Latest Ride vs Fleet Average)
 */
@Composable
internal fun RiderPerformanceRadarChart(
  rides: List<RideSession>,
  settings: MasterSettings,
  chartHeight: Dp = 195.dp,
  modifier: Modifier = Modifier,
  onTap: (() -> Unit)? = null,
) {
  if (rides.isEmpty()) return
  val latest = rides.first()
  val triggerKey = remember(rides.size, latest.id) { "radar_${rides.size}_${latest.id}" }
  val entranceState = rememberChartEntranceState(triggerKey = triggerKey, reducedMotion = settings.reducedMotion, durationMs = 640)

  val radar = remember(rides) { computeRiderRadarScores(rides) }
  val axes = radar.axisLabels
  val latestScores = radar.latest
  val fleetScores = radar.fleetAvg

  val primaryC = MaterialTheme.colorScheme.primary
  val secondaryC = MaterialTheme.colorScheme.secondary
  val gridC = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text("Latest Ride vs Fleet Benchmark", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("◆ Latest", style = MaterialTheme.typography.labelSmall, color = primaryC, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        Text("◆ Fleet Avg", style = MaterialTheme.typography.labelSmall, color = secondaryC, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
      }
    }

    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(chartHeight)
        .then(if (onTap != null) Modifier.clickable { onTap() } else Modifier),
    ) {
      val entrance = entranceState.value
      if (size.width < 24f || size.height < 24f) return@Canvas
      val cx = size.width / 2f
      val cy = size.height / 2f
      val maxR = minOf(size.width, size.height) * 0.42f
      val n = axes.size

      fun polyPath(scores: List<Float>, scale: Float): Path {
        val p = Path()
        for (i in 0 until n) {
          val ang = -PI / 2.0 + (2.0 * PI * i / n)
          val r = maxR * scores[i] * scale
          val x = cx + (cos(ang) * r).toFloat()
          val y = cy + (sin(ang) * r).toFloat()
          if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        return p
      }

      // Concentric pentagon rings
      listOf(0.33f, 0.66f, 1f).forEach { ring ->
        drawPath(polyPath(List(n) { ring }, 1f), gridC, style = Stroke(width = 1.dp.toPx()))
      }
      for (i in 0 until n) {
        val ang = -PI / 2.0 + (2.0 * PI * i / n)
        drawLine(
          gridC,
          Offset(cx, cy),
          Offset(cx + (cos(ang) * maxR).toFloat(), cy + (sin(ang) * maxR).toFloat()),
          strokeWidth = 1.dp.toPx(),
        )
      }

      val fleetPath = polyPath(fleetScores, entrance)
      drawPath(fleetPath, secondaryC.copy(alpha = 0.22f))
      drawPath(fleetPath, secondaryC, style = Stroke(width = 2.dp.toPx()))

      val latestPath = polyPath(latestScores, entrance)
      drawPath(latestPath, primaryC.copy(alpha = 0.28f))
      drawPath(latestPath, primaryC, style = Stroke(width = 2.6.dp.toPx()))

      for (i in 0 until n) {
        val ang = -PI / 2.0 + (2.0 * PI * i / n)
        val r = maxR * latestScores[i] * entrance
        drawCircle(primaryC, radius = 4.dp.toPx(), center = Offset(cx + (cos(ang) * r).toFloat(), cy + (sin(ang) * r).toFloat()))
      }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      axes.forEachIndexed { idx, lbl ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
          Text(lbl, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
          Text(
            "${(latestScores[idx] * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = primaryC,
            maxLines = 1,
            softWrap = false,
          )
        }
      }
    }
  }
}

/**
 * 10. CUMULATIVE ODOMETER & GAS SAVINGS ($) GROWTH CURVE
 */
@Composable
internal fun CumulativeOdometerRoiChart(
  rides: List<RideSession>,
  settings: MasterSettings,
  chartHeight: Dp = 155.dp,
  modifier: Modifier = Modifier,
  onTap: (() -> Unit)? = null,
) {
  val ordered = remember(rides) { rides.take(24).reversed() }
  if (ordered.isEmpty()) return
  val triggerKey = remember(ordered) { "odo_${ordered.size}_${ordered.last().id}" }
  val entranceState = rememberChartEntranceState(triggerKey = triggerKey, reducedMotion = settings.reducedMotion, durationMs = 620)

  val distUnit = distUnitLabel(settings.distanceUnit)
  val cumulativeDist =
    remember(ordered, settings.distanceUnit) {
      var acc = 0.0
      ordered.map { r ->
        acc += formatDistValue(r.distanceKm, settings.distanceUnit)
        acc
      }
    }
  val cumulativeUsdSaved =
    remember(ordered, settings.gasPriceUsd, settings.carMpg, settings.kwhRateUsd) {
      var acc = 0.0
      ordered.map { r ->
        val miles = r.distanceKm * 0.621371
        val gasCost = (miles / settings.carMpg.coerceAtLeast(8.0)) * settings.gasPriceUsd
        val elecCost = (r.whUsed / 1000.0) * settings.kwhRateUsd
        acc += (gasCost - elecCost).coerceAtLeast(0.0)
        acc
      }
    }

  var selectedIdx by remember(ordered.size) { mutableIntStateOf(ordered.lastIndex) }
  var anchorXFrac by remember(ordered.size) { mutableFloatStateOf(1f) }
  var showTooltip by remember(ordered.size) { mutableStateOf(false) }

  val maxDist = max(1.0, (cumulativeDist.lastOrNull() ?: 5.0) * 1.15)
  val accentC = Color(0xFF22C55E)
  val gridC = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
  val safeIdx = selectedIdx.coerceIn(0, ordered.lastIndex)

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text("Cumulative Miles & Fuel Savings", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
      Text("+$${"%.2f".format(cumulativeUsdSaved.lastOrNull() ?: 0.0)} Saved", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = accentC, maxLines = 1, softWrap = false)
    }

    Box(modifier = Modifier.fillMaxWidth()) {
      Canvas(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(chartHeight)
            .chartHoverAndScrub(
              itemCount = ordered.size,
              useSlots = false,
              onPointFocused = { idx, frac, _ ->
                selectedIdx = idx
                anchorXFrac = frac
                showTooltip = true
              },
              onHoverEnded = { showTooltip = false },
              onTap = onTap,
            ),
      ) {
        val entrance = entranceState.value
        val w = size.width
        val h = size.height
        if (w < 24f || h < 24f) return@Canvas
        for (g in 0..3) drawLine(gridC, Offset(0f, h * g / 3f), Offset(w, h * g / 3f), strokeWidth = 1f)
        val maxYBound = (h - 8f).coerceAtLeast(8f)
        clipRect(0f, 0f, w * entrance, h) {
          if (ordered.size == 1) {
            val y = h * 0.35f
            drawCircle(accentC, radius = 6.dp.toPx(), center = Offset(w / 2f, y))
          } else {
            val stepX = w / (ordered.size - 1).coerceAtLeast(1)
            val pts = cumulativeDist.mapIndexed { i, d ->
              val rawY = ((d / maxDist).toFloat() * h).let { if (it.isFinite()) it else 8f }
              Offset(i * stepX, h - rawY.coerceIn(8f, maxYBound))
            }
            val spline = buildSmoothSplinePath(pts)
            val fill = Path().apply {
              addPath(spline)
              lineTo(pts.last().x, h)
              lineTo(pts.first().x, h)
              close()
            }
            drawPath(fill, brush = Brush.verticalGradient(listOf(accentC.copy(alpha = 0.35f), Color.Transparent)))
            drawPath(spline, accentC, style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round))
            pts.getOrNull(safeIdx)?.let { selPt ->
              drawCircle(accentC, radius = 5.5.dp.toPx(), center = selPt)
            }
          }
        }
      }

      NeonChartTooltipOverlay(
        visible = showTooltip,
        anchorXFraction = anchorXFrac,
        title = "Ride #${safeIdx + 1}: ${ordered[safeIdx].title.take(18)}",
        accentColor = accentC,
        metrics =
          listOf(
            NeonTooltipMetric("TOTAL", "${"%.1f".format(cumulativeDist[safeIdx])} $distUnit", accentC),
            NeonTooltipMetric("SAVED", "$${"%.2f".format(cumulativeUsdSaved[safeIdx])}", MaterialTheme.colorScheme.primary),
          ),
        modifier = Modifier.align(Alignment.TopStart).padding(top = 2.dp),
      )
    }
  }
}

/**
 * LIVE INTERACTIVE RIDE PLAYBACK SCRUBBER (Synchronized Speed, Elevation, Pack Volts, Grade & Watts)
 */
@Composable
internal fun InteractiveRidePlaybackScrubber(
  ride: RideSession,
  activeVehicle: PevVehicle?,
  settings: MasterSettings,
  externalProgress: Float? = null,
  onProgressChange: ((Float) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val speedCurve = remember(ride.id, settings.distanceUnit) { buildRideSpeedProfile(ride, settings.distanceUnit) }
  val elevCurve = remember(ride.id, settings.distanceUnit) { buildRideElevationProfile(ride, settings.distanceUnit) }
  var internalProgress by remember(ride.id) { mutableFloatStateOf(0f) }
  val progress = (externalProgress ?: internalProgress).coerceIn(0f, 1f)
  fun setProgress(value: Float) {
    val clamped = value.coerceIn(0f, 1f)
    internalProgress = clamped
    onProgressChange?.invoke(clamped)
  }
  var playing by remember(ride.id) { mutableStateOf(false) }

  LaunchedEffect(playing, ride.id) {
    while (playing) {
      delay(65L)
      val next = (externalProgress ?: internalProgress) + 0.015f
      if (next >= 1f) {
        setProgress(1f)
        playing = false
      } else {
        setProgress(next)
      }
    }
  }

  val idx = if (speedCurve.isEmpty()) 0 else ((progress * (speedCurve.size - 1)).toInt()).coerceIn(0, speedCurve.lastIndex)
  val spdVal = speedCurve.getOrElse(idx) { 0.0 }.takeIf { it.isFinite() } ?: 0.0
  val elevVal = elevCurve.getOrElse(idx) { 0.0 }.takeIf { it.isFinite() } ?: 0.0
  val spdUnit = speedUnitLabel(settings.distanceUnit)
  val elevUnit = elevationUnitLabel(settings.distanceUnit)
  val safeDuration = ride.durationSec.coerceAtLeast(0L)
  val elapsedAtPosSec = (safeDuration * progress).toInt()

  val nomV = activeVehicle?.voltage?.takeIf { it.isFinite() && it >= 12.0 } ?: 52.0
  val startPct = if (ride.startBatteryPct >= 0) ride.startBatteryPct.toDouble() else 95.0
  val endPct = if (ride.endBatteryPct >= 0) ride.endBatteryPct.toDouble() else (startPct - 20.0).coerceAtLeast(15.0)
  val estSoc = (startPct + (endPct - startPct) * progress).coerceIn(0.0, 100.0)
  val estVolts = PevBatteryPhysics.voltageForSoc(nomV, estSoc, settings.batteryChemistry).takeIf { it.isFinite() } ?: nomV
  val safeWh = ride.whUsed.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
  val maxSpdPeak = (speedCurve.maxOrNull()?.takeIf { it.isFinite() && it >= 1.0 } ?: 1.0)
  val estWattsRaw = (safeWh / (safeDuration / 3600.0).coerceAtLeast(0.02)) * (0.45 + 0.9 * (spdVal / maxSpdPeak))
  val estWatts = (if (estWattsRaw.isFinite()) estWattsRaw.toInt() else 0).coerceIn(0, 8000)

  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
    shape = RoundedCornerShape(14.dp),
    modifier = modifier.fillMaxWidth().testTag("ride_playback_scrubber"),
  ) {
    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
          Text("RIDE TIMELINE REPLAY SCRUBBER", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
          Text(
            "T+%02d:%02d • ${(progress * 100).toInt()}% complete".format(elapsedAtPosSec / 60, elapsedAtPosSec % 60),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = {
              if (progress >= 0.99f) setProgress(0f)
              playing = !playing
            },
            modifier = Modifier.size(48.dp).testTag("ride_replay_play_pause_btn"),
          ) {
            Icon(
              if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = if (playing) "Pause Replay" else "Play Replay",
              tint = MaterialTheme.colorScheme.primary,
            )
          }
          IconButton(
            onClick = {
              playing = false
              setProgress(0f)
            },
            modifier = Modifier.size(48.dp).testTag("ride_replay_reset_btn"),
          ) {
            Icon(Icons.Default.Replay, contentDescription = "Reset Replay", tint = MaterialTheme.colorScheme.secondary)
          }
        }
      }

      com.example.ui.components.TacticalSlider(
        value = progress,
        onValueChange = {
          playing = false
          setProgress(it)
        },
        modifier = Modifier.fillMaxWidth(),
      )

      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ReplayMetricCell("SPEED", "${"%.1f".format(spdVal)} $spdUnit", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        ReplayMetricCell("CLIMB", "+${elevVal.toInt()} $elevUnit", Color(0xFF38BDF8), Modifier.weight(1f))
        ReplayMetricCell("PACK", "${"%.1f".format(estVolts)}V", Color(0xFF22C55E), Modifier.weight(1f))
        ReplayMetricCell("LOAD", "${estWatts}W", Color(0xFFFF6B35), Modifier.weight(1f))
      }
    }
  }
}

@Composable
private fun ReplayMetricCell(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
    modifier = modifier,
  ) {
    Column(Modifier.padding(horizontal = 6.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
      Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = accent, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
    }
  }
}
