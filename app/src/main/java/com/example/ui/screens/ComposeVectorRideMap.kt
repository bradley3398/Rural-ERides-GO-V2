package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MasterSettings
import com.example.data.RideSession
import com.example.ui.components.distUnitLabel
import com.example.ui.components.formatDistValue
import com.example.ui.components.formatSpeedValue
import com.example.ui.components.speedUnitLabel
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

internal data class VectorProjectionBounds(
  val minX: Double,
  val maxX: Double,
  val minY: Double,
  val maxY: Double,
  val cosLat: Double,
) {
  fun project(lat: Double, lng: Double): Pair<Float, Float> {
    val spanX = max(maxX - minX, 1e-5)
    val spanY = max(maxY - minY, 1e-5)
    val maxSpan = max(spanX, spanY)
    val xPad = (maxSpan - spanX) * 0.5
    val yPad = (maxSpan - spanY) * 0.5
    val rawX = lng * cosLat
    val u = (((rawX - minX) + xPad) / maxSpan).toFloat().coerceIn(0f, 1f)
    // Screen Y increases downward, so higher latitude maps toward 0f (top)
    val v = (1.0 - (((lat - minY) + yPad) / maxSpan)).toFloat().coerceIn(0f, 1f)
    return u to v
  }
}

internal fun computeVectorProjectionBounds(points: List<Pair<Double, Double>>): VectorProjectionBounds {
  val valid = points.filter { (lat, lng) -> lat.isFinite() && lng.isFinite() }
  if (valid.isEmpty()) return VectorProjectionBounds(0.0, 1.0, 0.0, 1.0, 1.0)
  val midLat = valid.sumOf { it.first } / valid.size.toDouble()
  val cosLat = cos(midLat * PI / 180.0).coerceIn(0.25, 1.0)
  var minX = Double.POSITIVE_INFINITY
  var maxX = Double.NEGATIVE_INFINITY
  var minY = Double.POSITIVE_INFINITY
  var maxY = Double.NEGATIVE_INFINITY
  valid.forEach { (lat, lng) ->
    val x = lng * cosLat
    if (x < minX) minX = x
    if (x > maxX) maxX = x
    if (lat < minY) minY = lat
    if (lat > maxY) maxY = lat
  }
  return VectorProjectionBounds(
    minX = minX,
    maxX = maxX,
    minY = minY,
    maxY = maxY,
    cosLat = cosLat,
  )
}

/**
 * Pure cosine-corrected geospatial normalizer that maps any valid lat/lng list
 * into aspect-preserving unit coordinates [0f..1f] for 100% Compose Vector route rendering.
 */
internal fun normalizeGeoTrackToUnitBox(points: List<Pair<Double, Double>>): List<Pair<Float, Float>> {
  val bounds = computeVectorProjectionBounds(points)
  return points.mapNotNull { (lat, lng) ->
    if (lat.isFinite() && lng.isFinite()) bounds.project(lat, lng) else null
  }
}

private fun downsampleLatLng(points: List<Pair<Double, Double>>, maxPoints: Int = 320): List<Pair<Double, Double>> {
  if (points.size <= maxPoints) return points
  val step = (points.size - 1).toDouble() / (maxPoints - 1).toDouble()
  return List(maxPoints) { idx ->
    points[(idx * step).toInt().coerceIn(0, points.lastIndex)]
  }
}

/**
 * 100% JETPACK COMPOSE VECTOR ROUTE MAP
 * Eliminates native WebView/MapView lifecycle crashes inside LazyColumn Deep Dive
 * while providing interactive pinch/pan/zoom, topographic contours, and live replay beacon sync.
 */
@Composable
internal fun ComposeVectorRideMap(
  ride: RideSession,
  allOverlayRides: List<RideSession> = emptyList(),
  settings: MasterSettings,
  playbackProgress: Float? = null,
  height: Dp = 220.dp,
  modifier: Modifier = Modifier,
) {
  val primaryLatLng = remember(ride.id, ride.polylinePoints.size) {
    downsampleLatLng(parseRideTrackPoints(ride).map { it.latitude to it.longitude })
  }
  val overlayTracks = remember(allOverlayRides.size, allOverlayRides.firstOrNull()?.id) {
    allOverlayRides.take(16).mapNotNull { r ->
      val pts = downsampleLatLng(parseRideTrackPoints(r).map { it.latitude to it.longitude }, maxPoints = 140)
      if (pts.size >= 2) r to pts else null
    }
  }

  val distUnit = distUnitLabel(settings.distanceUnit)
  val spdUnit = speedUnitLabel(settings.distanceUnit)

  if (primaryLatLng.isEmpty() && overlayTracks.isEmpty()) {
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
      modifier = modifier.fillMaxWidth().testTag("ride_track_map_empty"),
    ) {
      Text(
        "No GPS polyline points saved for \"${ride.title}\". Record a trail with GPS lock to view your Compose Vector route track.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(12.dp),
      )
    }
    return
  }

  val allFitPts = remember(primaryLatLng, overlayTracks) {
    if (overlayTracks.isNotEmpty()) overlayTracks.flatMap { it.second } else primaryLatLng
  }
  val bounds = remember(allFitPts) { computeVectorProjectionBounds(allFitPts) }

  var zoomScale by remember(ride.id, overlayTracks.size) { mutableFloatStateOf(1f) }
  var panOffset by remember(ride.id, overlayTracks.size) { mutableStateOf(Offset.Zero) }

  val primaryC = MaterialTheme.colorScheme.primary
  val secondaryC = MaterialTheme.colorScheme.secondary
  val tertiaryC = MaterialTheme.colorScheme.tertiary
  val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
  val bgTop = if (isDark) Color(0xFF07111A) else Color(0xFFEEF5FA)
  val bgBot = if (isDark) Color(0xFF0C1B28) else Color(0xFFE0ECF4)
  val gridC = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
  val contourC = primaryC.copy(alpha = if (isDark) 0.14f else 0.18f)
  val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f) }

  val overlayColors = remember(primaryC, secondaryC, tertiaryC) {
    listOf(primaryC, secondaryC, Color(0xFF38BDF8), tertiaryC, Color(0xFFA855F7), Color(0xFF22C55E))
  }

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .height(height)
        .clip(RoundedCornerShape(12.dp))
        .background(Brush.verticalGradient(listOf(bgTop, bgBot)))
        .pointerInput(ride.id, overlayTracks.size) {
          detectTransformGestures { _, pan, zoom, _ ->
            zoomScale = (zoomScale * zoom).coerceIn(0.8f, 6.0f)
            val maxPan = 420f * zoomScale
            panOffset = Offset(
              x = (panOffset.x + pan.x).coerceIn(-maxPan, maxPan),
              y = (panOffset.y + pan.y).coerceIn(-maxPan, maxPan),
            )
          }
        }
        .testTag("ride_track_map_view"),
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      if (w < 24f || h < 24f) return@Canvas

      clipRect {
        val pad = minOf(w, h) * 0.12f
        val plotSize = (minOf(w, h) - pad * 2f).coerceAtLeast(16f)
        val cx = w / 2f
        val cy = h / 2f

        val safeZoom = if (zoomScale.isFinite()) zoomScale.coerceIn(0.8f, 6.0f) else 1f
        val safePanX = if (panOffset.x.isFinite()) panOffset.x else 0f
        val safePanY = if (panOffset.y.isFinite()) panOffset.y else 0f
        fun toCanvas(uv: Pair<Float, Float>): Offset {
          val u = if (uv.first.isFinite()) uv.first.coerceIn(0f, 1f) else 0.5f
          val v = if (uv.second.isFinite()) uv.second.coerceIn(0f, 1f) else 0.5f
          val baseX = cx + (u - 0.5f) * plotSize
          val baseY = cy + (v - 0.5f) * plotSize
          return Offset(
            x = cx + (baseX - cx) * safeZoom + safePanX,
            y = cy + (baseY - cy) * safeZoom + safePanY,
          )
        }

        // Vector grid
        val gridCols = 8
        for (i in 0..gridCols) {
          val gx = (w / gridCols) * i
          val gy = (h / gridCols) * i
          drawLine(gridC, Offset(gx, 0f), Offset(gx, h), strokeWidth = 1f)
          drawLine(gridC, Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
        }

        // Topographic elevation vector rings centered on terrain focus
        val terrainCenter = toCanvas(0.46f to 0.52f)
        for (ring in 1..4) {
          val r = plotSize * 0.16f * ring * zoomScale.coerceAtLeast(0.75f)
          drawCircle(
            color = contourC,
            radius = r,
            center = terrainCenter,
            style = Stroke(width = 1.4f, pathEffect = if (ring % 2 == 0) dashEffect else null),
          )
        }

        if (overlayTracks.isNotEmpty()) {
          overlayTracks.forEachIndexed { idx, (_, pts) ->
            val col = overlayColors[idx % overlayColors.size]
            val path = Path()
            val mapped = pts.map { (lat, lng) -> toCanvas(bounds.project(lat, lng)) }
            mapped.forEachIndexed { pIdx, pt ->
              if (pIdx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            drawPath(path, col.copy(alpha = 0.25f), style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path, col, style = Stroke(width = if (idx == 0) 5f else 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            mapped.firstOrNull()?.let { startPt ->
              drawCircle(col, radius = 5f, center = startPt)
            }
          }
        } else if (primaryLatLng.isNotEmpty()) {
          val mapped = primaryLatLng.map { (lat, lng) -> toCanvas(bounds.project(lat, lng)) }
          if (mapped.size >= 2) {
            val routePath = Path()
            mapped.forEachIndexed { i, pt ->
              if (i == 0) routePath.moveTo(pt.x, pt.y) else routePath.lineTo(pt.x, pt.y)
            }
            // Glow halo
            drawPath(
              path = routePath,
              color = primaryC.copy(alpha = 0.24f),
              style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            // Primary route vector brush
            drawPath(
              path = routePath,
              brush = Brush.linearGradient(listOf(Color(0xFF22C55E), primaryC, secondaryC)),
              style = Stroke(width = 5.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )

            // Directional chevrons along the track
            val chevronIndices = listOf(mapped.size / 3, (mapped.size * 2) / 3).distinct()
            chevronIndices.forEach { ci ->
              val prev = mapped[(ci - 1).coerceAtLeast(0)]
              val curr = mapped[ci.coerceAtMost(mapped.lastIndex)]
              val dx = curr.x - prev.x
              val dy = curr.y - prev.y
              if (dx * dx + dy * dy > 4f) {
                val ang = atan2(dy.toDouble(), dx.toDouble())
                val len = 9f
                val leftAng = ang + Math.toRadians(145.0)
                val rightAng = ang - Math.toRadians(145.0)
                drawLine(
                  color = Color.White,
                  start = curr,
                  end = Offset(curr.x + (cos(leftAng) * len).toFloat(), curr.y + (sin(leftAng) * len).toFloat()),
                  strokeWidth = 2.4f,
                  cap = StrokeCap.Round,
                )
                drawLine(
                  color = Color.White,
                  start = curr,
                  end = Offset(curr.x + (cos(rightAng) * len).toFloat(), curr.y + (sin(rightAng) * len).toFloat()),
                  strokeWidth = 2.4f,
                  cap = StrokeCap.Round,
                )
              }
            }
          }

          // Start pin (green)
          val startPt = mapped.first()
          drawCircle(Color(0xFF22C55E).copy(alpha = 0.28f), radius = 12f, center = startPt)
          drawCircle(Color(0xFF22C55E), radius = 6.5f, center = startPt)
          drawCircle(Color.White, radius = 2.5f, center = startPt)

          // Finish pin (coral/secondary)
          val endPt = mapped.last()
          if (mapped.size >= 2) {
            drawCircle(Color(0xFFF43F5E).copy(alpha = 0.28f), radius = 12f, center = endPt)
            drawCircle(Color(0xFFF43F5E), radius = 6.5f, center = endPt)
            drawCircle(Color.White, radius = 2.5f, center = endPt)
          }

          // Live Scrubbing / Playback Beacon synced with InteractiveRidePlaybackScrubber
          if (playbackProgress != null && mapped.size >= 2) {
            val clamped = playbackProgress.coerceIn(0f, 1f)
            val exactIdx = clamped * (mapped.size - 1)
            val lo = exactIdx.toInt().coerceIn(0, mapped.lastIndex)
            val hi = (lo + 1).coerceAtMost(mapped.lastIndex)
            val frac = exactIdx - lo
            val beacon = Offset(
              x = mapped[lo].x + (mapped[hi].x - mapped[lo].x) * frac,
              y = mapped[lo].y + (mapped[hi].y - mapped[lo].y) * frac,
            )
            drawCircle(Color(0xFF38BDF8).copy(alpha = 0.34f), radius = 17f, center = beacon)
            drawCircle(Color(0xFF38BDF8), radius = 8f, center = beacon)
            drawCircle(Color.White, radius = 3.2f, center = beacon)
          }
        }

        // Vector North Compass Rose in top-right corner
        val compassCenter = Offset(w - 28.dp.toPx(), 28.dp.toPx())
        val compassR = 14.dp.toPx()
        drawCircle(gridC.copy(alpha = 0.28f), radius = compassR, center = compassCenter, style = Stroke(width = 1.6f))
        drawLine(
          color = Color(0xFFF43F5E),
          start = compassCenter,
          end = Offset(compassCenter.x, compassCenter.y - compassR * 0.8f),
          strokeWidth = 2.4f,
          cap = StrokeCap.Round,
        )
        drawLine(
          color = if (isDark) Color.White.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.55f),
          start = compassCenter,
          end = Offset(compassCenter.x, compassCenter.y + compassR * 0.7f),
          strokeWidth = 2.0f,
          cap = StrokeCap.Round,
        )
      }
    }

    // Top-left ride telemetry badge
    Surface(
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
      shape = RoundedCornerShape(10.dp),
      border = BorderStroke(1.dp, primaryC.copy(alpha = 0.35f)),
      modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
    ) {
      Text(
        if (overlayTracks.isNotEmpty()) {
          "COMPOSE VECTOR • ${overlayTracks.size} GPS TRAILS"
        } else {
          "${ride.vehicleName} • ${"%.2f".format(formatDistValue(ride.distanceKm, settings.distanceUnit))} $distUnit • Max ${"%.1f".format(formatSpeedValue(ride.maxSpeedKmh, settings.distanceUnit))} $spdUnit"
        },
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      )
    }

    // Bottom-left Start / Finish legend pill
    Surface(
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
      shape = RoundedCornerShape(8.dp),
      modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text("● START", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF22C55E), maxLines = 1, softWrap = false)
        Text("● END", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFFF43F5E), maxLines = 1, softWrap = false)
        if (playbackProgress != null) {
          Text("● REPLAY", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF38BDF8), maxLines = 1, softWrap = false)
        }
      }
    }

    // Bottom-right vector map fit & zoom controls (>= 48dp touch targets)
    Column(
      modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
      horizontalAlignment = Alignment.End,
    ) {
      Surface(
        onClick = {
          zoomScale = 1f
          panOffset = Offset.Zero
        },
        color = primaryC,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.heightIn(min = 48.dp),
      ) {
        Row(
          Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(Icons.Default.MyLocation, contentDescription = "Fit Track", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
          Text("FIT TRACK", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary, maxLines = 1, softWrap = false)
        }
      }
      Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f), shape = RoundedCornerShape(12.dp)) {
        Row {
          IconButton(
            onClick = { zoomScale = (zoomScale * 1.35f).coerceIn(0.8f, 6.0f) },
            modifier = Modifier.size(48.dp),
          ) {
            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
          }
          IconButton(
            onClick = { zoomScale = (zoomScale / 1.35f).coerceIn(0.8f, 6.0f) },
            modifier = Modifier.size(48.dp),
          ) {
            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
          }
        }
      }
    }
  }
}
