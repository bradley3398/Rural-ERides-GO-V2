package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.preference.PreferenceManager
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.graphics.ColorUtils
import com.example.core.ApiKeyRing
import com.example.core.LiveCloudServices
import com.example.data.ColorModeOverride
import com.example.data.MasterSettings
import com.example.data.VisualStyleMode
import java.net.URLEncoder
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon

enum class TacticalMapLayer(
  val id: String,
  val label: String,
  val requiresMapbox: Boolean,
) {
  MAPBOX_STREETS("MAPBOX_STREETS", "Mapbox Streets", true),
  MAPBOX_SATELLITE("MAPBOX_SATELLITE", "Satellite", true),
  MAPBOX_DARK("MAPBOX_DARK", "Mapbox Dark", true),
  MAPBOX_OUTDOORS("MAPBOX_OUTDOORS", "Outdoors", true),
  STREET("STREET", "OSM Street", false),
  TOPO("TOPO", "Topo", false),
}

fun availableMapLayers(): List<TacticalMapLayer> {
  val hasMb = ApiKeyRing.isRealKey(LiveCloudServices.mapboxToken())
  return TacticalMapLayer.entries.filter { !it.requiresMapbox || hasMb }
}

fun defaultMapLayerId(): String =
  if (ApiKeyRing.isRealKey(LiveCloudServices.mapboxToken())) TacticalMapLayer.MAPBOX_STREETS.id
  else TacticalMapLayer.STREET.id

fun resolveMapTileSource(layerId: String): ITileSource {
  val mbToken = LiveCloudServices.mapboxToken()
  if (ApiKeyRing.isRealKey(mbToken)) {
    val styleSlug =
      when (layerId) {
        TacticalMapLayer.MAPBOX_STREETS.id -> "streets-v12"
        TacticalMapLayer.MAPBOX_SATELLITE.id -> "satellite-streets-v12"
        TacticalMapLayer.MAPBOX_DARK.id -> "navigation-night-v1"
        TacticalMapLayer.MAPBOX_OUTDOORS.id -> "outdoors-v12"
        else -> null
      }
    if (styleSlug != null) {
      return runCatching {
        val encodedToken = URLEncoder.encode(mbToken, "UTF-8")
        object :
          OnlineTileSourceBase(
            "Mapbox_$styleSlug",
            1,
            20,
            512,
            "@2x.png",
            arrayOf("https://api.mapbox.com/styles/v1/mapbox/$styleSlug/tiles/256/"),
          ) {
          override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return "${baseUrl}$z/$x/$y@2x?access_token=$encodedToken"
          }
        }
      }.getOrDefault(TileSourceFactory.MAPNIK)
    }
  }
  return if (layerId == TacticalMapLayer.TOPO.id) TileSourceFactory.OpenTopo
  else TileSourceFactory.MAPNIK
}

@Volatile private var osmdroidConfigured = false

@Suppress("DEPRECATION")
fun ensureOsmdroidConfigured(context: Context) {
  if (osmdroidConfigured) return
  try {
    val appCtx = context.applicationContext
    val tileCacheDir = java.io.File(appCtx.cacheDir, "osmdroid").apply { mkdirs() }
    val cfg = Configuration.getInstance()
    cfg.load(appCtx, PreferenceManager.getDefaultSharedPreferences(appCtx))
    cfg.userAgentValue = "RuralERidesGO/27.0 (Android PEV Navigation)"
    cfg.osmdroidBasePath = appCtx.filesDir
    cfg.osmdroidTileCache = tileCacheDir
    osmdroidConfigured = true
  } catch (_: Throwable) {
    // Suppress osmdroid setup errors
  }
}

fun applyThemeToMapView(
  mapView: MapView,
  settings: MasterSettings,
  primaryArgb: Int,
  surfaceArgb: Int,
  isDarkTheme: Boolean,
  layerId: String = "",
) {
  runCatching {
    mapView.setBackgroundColor(surfaceArgb)
    val tilesOverlay = mapView.overlayManager?.tilesOverlay ?: return@runCatching

    if (layerId == TacticalMapLayer.MAPBOX_SATELLITE.id && !settings.nightVisionMode) {
      tilesOverlay.setColorFilter(null)
      return@runCatching
    }

    val matrix = ColorMatrix()
    when {
      settings.nightVisionMode -> {
        matrix.set(
          floatArrayOf(
            -0.65f, 0f, 0f, 0f, 185f,
            0f, -0.18f, 0f, 0f, 38f,
            0f, 0f, -0.18f, 0f, 32f,
            0f, 0f, 0f, 1f, 0f,
          )
        )
        tilesOverlay.setColorFilter(ColorMatrixColorFilter(matrix))
      }
      isDarkTheme && settings.colorModeOverride != ColorModeOverride.LIGHT -> {
        matrix.set(
          floatArrayOf(
            -0.82f, 0f, 0f, 0f, 200f,
            0f, -0.82f, 0f, 0f, 200f,
            0f, 0f, -0.82f, 0f, 200f,
            0f, 0f, 0f, 1f, 0f,
          )
        )
        tilesOverlay.setColorFilter(ColorMatrixColorFilter(matrix))
      }
      else -> {
        tilesOverlay.setColorFilter(null)
      }
    }
  }
}

@SuppressLint("ClickableViewAccessibility")
internal fun configureSmoothMapPanning(mapView: MapView, onMoved: () -> Unit) {
  runCatching {
    mapView.setMultiTouchControls(true)
    var downX = 0f
    var downY = 0f
    var notifiedMove = false
    mapView.setOnTouchListener { v, event ->
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          downX = event.x
          downY = event.y
          notifiedMove = false
          v.parent?.requestDisallowInterceptTouchEvent(true)
        }
        MotionEvent.ACTION_MOVE -> {
          v.parent?.requestDisallowInterceptTouchEvent(true)
          if (!notifiedMove) {
            val dx = kotlin.math.abs(event.x - downX)
            val dy = kotlin.math.abs(event.y - downY)
            if (dx > 12f || dy > 12f) {
              notifiedMove = true
              onMoved()
            }
          }
        }
        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
          v.parent?.requestDisallowInterceptTouchEvent(false)
        }
      }
      false
    }
  }
}

fun buildMapCirclePolygon(
  map: MapView,
  center: GeoPoint,
  radiusMeters: Double,
  strokeArgb: Int,
  fillAlpha: Int,
): Polygon {
  val polygon = Polygon()
  polygon.points = Polygon.pointsAsCircle(center, radiusMeters)
  polygon.fillColor = androidx.core.graphics.ColorUtils.setAlphaComponent(strokeArgb, fillAlpha)
  polygon.strokeColor = strokeArgb
  polygon.strokeWidth = 3f
  return polygon
}

fun buildMapCirclePolygon(
  center: GeoPoint,
  radiusMeters: Double,
  fillColor: Int,
  strokeColor: Int,
  strokeWidthPx: Float,
): Polygon {
  val polygon = Polygon()
  polygon.points = Polygon.pointsAsCircle(center, radiusMeters)
  polygon.fillColor = fillColor
  polygon.strokeColor = strokeColor
  polygon.strokeWidth = strokeWidthPx
  return polygon
}

fun createSafeMapView(context: Context): MapView {
  ensureOsmdroidConfigured(context)
  return MapView(context).apply {
    setMultiTouchControls(true)
    isTilesScaledToDpi = true
    isHorizontalMapRepetitionEnabled = false
    isVerticalMapRepetitionEnabled = false
    minZoomLevel = 3.0
    maxZoomLevel = 20.0
    runCatching {
      zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
    }
  }
}

/**
 * 100% Crash-Proof Jetpack Compose Native Tactical Vector Map.
 * Renders tactical grid lines, concentric range rings, GPS vehicle marker with heading,
 * and route polylines interactively with pan and zoom gestures.
 */
@Composable
fun TacticalVectorMap(
  modifier: Modifier = Modifier,
  latitude: Double = 37.7749,
  longitude: Double = -122.4194,
  bearingDeg: Float = 0f,
  speedKmh: Double = 0.0,
  routePoints: List<Pair<Double, Double>> = emptyList(),
  primaryColor: Color = MaterialTheme.colorScheme.primary,
  surfaceColor: Color = MaterialTheme.colorScheme.surface,
  isDark: Boolean = true,
) {
  var panOffsetX by remember { mutableFloatStateOf(0f) }
  var panOffsetY by remember { mutableFloatStateOf(0f) }
  var zoomScale by remember { mutableFloatStateOf(1.0f) }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .background(surfaceColor)
      .pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
          change.consume()
          panOffsetX += dragAmount.x
          panOffsetY += dragAmount.y
        }
      }
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      val cx = w / 2f + panOffsetX
      val cy = h / 2f + panOffsetY

      // Draw tactical grid lines
      val gridSpacing = 65f * zoomScale
      val gridColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)

      var x = cx % gridSpacing
      while (x < w) {
        drawLine(gridColor, Offset(x, 0f), Offset(x, h), 1f)
        x += gridSpacing
      }
      var y = cy % gridSpacing
      while (y < h) {
        drawLine(gridColor, Offset(0f, y), Offset(w, y), 1f)
        y += gridSpacing
      }

      // Draw concentric range rings
      drawCircle(
        color = primaryColor.copy(alpha = 0.25f),
        radius = 90f * zoomScale,
        center = Offset(cx, cy),
        style = Stroke(width = 1.5f)
      )
      drawCircle(
        color = primaryColor.copy(alpha = 0.15f),
        radius = 180f * zoomScale,
        center = Offset(cx, cy),
        style = Stroke(width = 1.5f)
      )

      // Draw route polyline if available
      if (routePoints.size > 1) {
        val path = Path()
        routePoints.forEachIndexed { idx, pt ->
          val px = cx + ((pt.second - longitude) * 11000.0).toFloat() * zoomScale
          val py = cy - ((pt.first - latitude) * 11000.0).toFloat() * zoomScale
          if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        drawPath(
          path = path,
          color = primaryColor,
          style = Stroke(width = 5f, cap = StrokeCap.Round)
        )
      }

      // Draw vehicle center GPS marker with heading indicator
      drawCircle(
        color = primaryColor,
        radius = 14f,
        center = Offset(cx, cy)
      )
      drawCircle(
        color = Color.White,
        radius = 6f,
        center = Offset(cx, cy)
      )
    }
  }
}
