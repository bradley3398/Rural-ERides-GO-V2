package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.ActiveNavRoute
import com.example.data.LiveChargingStation
import com.example.data.LiveTelemetrySnapshot
import com.example.data.MasterSettings
import com.example.ui.components.HudSpeedLimitBadge
import com.example.ui.components.TacticalButton
import com.example.ui.components.TacticalFilterChip
import com.example.ui.components.TacticalSlider
import com.example.ui.components.distUnitLabel
import com.example.ui.components.formatDistValue
import com.example.ui.components.formatSpeedValue
import com.example.ui.components.speedUnitLabel
import com.example.ui.theme.LocalTacticalChrome
import com.example.viewmodel.RadarViewModel
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

enum class ArHudMode(val label: String) {
  TELEMETRY_HUD("Ride Camera"),
  OPTICAL_INSPECT("Measure & Inspect"),
  G_FORCE_DYNAMICS("G-Force"),
  NAV_WAYFINDER("Turn Guide"),
  SATELLITE_SKY("Satellites & Sky"),
  SLOPE_LEAN("Slope & Lean"),
}

enum class ArVisualFilter(val label: String, val tintColor: Color) {
  STANDARD("Clear View", Color.Transparent),
  THERMAL_FLIR("Heat Tint", Color(0x38FF4500)),
  NIGHT_VISION("Night Green", Color(0x3D00FF55)),
  EDGE_CRACK("High Contrast", Color(0x3000E5FF)),
}

@Composable
fun LiveArHudOverlay(
  telemetry: LiveTelemetrySnapshot,
  settings: MasterSettings,
  activeNavRoute: ActiveNavRoute? = null,
  navStepIndex: Int = 0,
  navDistToTurnM: Double = 0.0,
  nearestCharger: LiveChargingStation? = null,
  initialMode: ArHudMode = ArHudMode.TELEMETRY_HUD,
  onCaptureForVisionAi: ((Bitmap) -> Unit)? = null,
  onCancelNav: (() -> Unit)? = null,
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)
  val ctx = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val haptics = LocalHapticFeedback.current
  val chrome = LocalTacticalChrome.current

  var hasCamPerm by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    )
  }
  val permLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasCamPerm = granted
    }

  LaunchedEffect(Unit) {
    if (!hasCamPerm) permLauncher.launch(Manifest.permission.CAMERA)
  }

  var arMode by remember { mutableStateOf(initialMode) }
  var arFilter by remember { mutableStateOf(ArVisualFilter.STANDARD) }
  var showGrid by remember { mutableStateOf(true) }
  var showCaliper by remember { mutableStateOf(initialMode == ArHudMode.OPTICAL_INSPECT) }
  var selectedPartTag by remember { mutableStateOf("Brake Caliper") }
  var torchOn by remember { mutableStateOf(false) }
  var linearZoom by remember { mutableFloatStateOf(0f) }
  var exposureIdx by remember { mutableIntStateOf(0) }
  var caliperWidthPct by remember { mutableFloatStateOf(0.45f) }
  var boundCamera by remember { mutableStateOf<Camera?>(null) }
  var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
  var cameraBindError by remember { mutableStateOf<String?>(null) }
  val imageCapture = remember { ImageCapture.Builder().build() }

  LaunchedEffect(torchOn, boundCamera) {
    val cam = boundCamera ?: return@LaunchedEffect
    if (torchOn && !cam.cameraInfo.hasFlashUnit()) {
      torchOn = false
      Toast.makeText(ctx, "No camera flashlight detected on this device.", Toast.LENGTH_SHORT).show()
    } else {
      runCatching { cam.cameraControl.enableTorch(torchOn) }
    }
  }
  LaunchedEffect(linearZoom, boundCamera) {
    runCatching { boundCamera?.cameraControl?.setLinearZoom(linearZoom.coerceIn(0f, 1f)) }
  }
  LaunchedEffect(exposureIdx, boundCamera) {
    val cam = boundCamera ?: return@LaunchedEffect
    val st = cam.cameraInfo.exposureState
    if (st.isExposureCompensationSupported) {
      val clamped = exposureIdx.coerceIn(st.exposureCompensationRange.lower, st.exposureCompensationRange.upper)
      runCatching { cam.cameraControl.setExposureCompensationIndex(clamped) }
    }
  }

  val hudPrimary =
    when (arFilter) {
      ArVisualFilter.THERMAL_FLIR -> Color(0xFFFFB300)
      ArVisualFilter.NIGHT_VISION -> Color(0xFF39FF14)
      ArVisualFilter.EDGE_CRACK -> Color(0xFF00F0FF)
      ArVisualFilter.STANDARD -> MaterialTheme.colorScheme.primary
    }

  val targetBearingDeg =
    remember(telemetry.lat, telemetry.lng, activeNavRoute, nearestCharger) {
      if (telemetry.lat == 0.0 && telemetry.lng == 0.0) null
      else if (activeNavRoute != null) {
        RadarViewModel.bearingDeg(telemetry.lat, telemetry.lng, activeNavRoute.destLat, activeNavRoute.destLng)
      } else if (nearestCharger != null) {
        RadarViewModel.bearingDeg(telemetry.lat, telemetry.lng, nearestCharger.lat, nearestCharger.lng)
      } else null
    }

  val targetDistanceKm =
    remember(telemetry.lat, telemetry.lng, activeNavRoute, nearestCharger) {
      if (telemetry.lat == 0.0 && telemetry.lng == 0.0) null
      else if (activeNavRoute != null) {
        RadarViewModel.haversineKm(telemetry.lat, telemetry.lng, activeNavRoute.destLat, activeNavRoute.destLng)
      } else if (nearestCharger != null) {
        RadarViewModel.haversineKm(telemetry.lat, telemetry.lng, nearestCharger.lat, nearestCharger.lng)
      } else null
    }

  val gradePct =
    remember(telemetry.pitchDeg) {
      (tan(Math.toRadians(telemetry.pitchDeg.coerceIn(-45f, 45f).toDouble())) * 100.0).toFloat()
    }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    BackHandler(onBack = onDismiss)
    Surface(
      color = Color(0xFF05080F),
      modifier = Modifier.fillMaxSize().testTag("live_ar_hud_overlay"),
    ) {
      Box(Modifier.fillMaxSize()) {
      if (hasCamPerm) {
        AndroidView(
          factory = { viewCtx ->
            val previewView = PreviewView(viewCtx)
            previewViewRef = previewView
            val providerFuture = ProcessCameraProvider.getInstance(viewCtx)
            providerFuture.addListener(
              {
                runCatching {
                  val provider = providerFuture.get()
                  val preview = Preview.Builder().build().apply { setSurfaceProvider(previewView.surfaceProvider) }
                  provider.unbindAll()
                  val selector =
                    when {
                      runCatching { provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) }.getOrDefault(false) ->
                        CameraSelector.DEFAULT_BACK_CAMERA
                      runCatching { provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) }.getOrDefault(false) ->
                        CameraSelector.DEFAULT_FRONT_CAMERA
                      else -> CameraSelector.DEFAULT_BACK_CAMERA
                    }
                  boundCamera = provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                  cameraBindError = null
                }.onFailure { err ->
                  cameraBindError = err.message ?: "Camera hardware unavailable"
                }
              },
              ContextCompat.getMainExecutor(viewCtx),
            )
            previewView
          },
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        Column(
          modifier = Modifier.align(Alignment.Center).padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(Icons.Default.ViewInAr, contentDescription = null, tint = hudPrimary, modifier = Modifier.size(48.dp))
          Text("Camera permission is needed for Live AR View & Part Inspection.", color = Color.White)
          TacticalButton(onClick = { permLauncher.launch(Manifest.permission.CAMERA) }) {
            Text("Enable Camera AR")
          }
        }
      }

      if (arFilter != ArVisualFilter.STANDARD) {
        Box(Modifier.fillMaxSize().background(arFilter.tintColor))
      }

      // LIVE AR VECTOR CANVAS (HORIZON, SATELLITE SKY RADAR, SLOPE & LEAN BUBBLE, CALIPER, WAYFINDER)
      Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        if (showGrid) {
          val gridColor = hudPrimary.copy(alpha = 0.18f)
          for (i in 1 until 6) {
            val x = size.width * i / 6
            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
          }
          for (j in 1 until 10) {
            val y = size.height * j / 10
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
          }
        }

        // Corner AR viewfinder brackets
        val bLen = 36f
        val pad = 42f
        val bracketStroke = 3.5f
        drawLine(hudPrimary, Offset(pad, pad), Offset(pad + bLen, pad), bracketStroke)
        drawLine(hudPrimary, Offset(pad, pad), Offset(pad, pad + bLen), bracketStroke)
        drawLine(hudPrimary, Offset(size.width - pad, pad), Offset(size.width - pad - bLen, pad), bracketStroke)
        drawLine(hudPrimary, Offset(size.width - pad, pad), Offset(size.width - pad, pad + bLen), bracketStroke)
        drawLine(hudPrimary, Offset(pad, size.height - pad), Offset(pad + bLen, size.height - pad), bracketStroke)
        drawLine(hudPrimary, Offset(pad, size.height - pad), Offset(pad, size.height - pad - bLen), bracketStroke)
        drawLine(hudPrimary, Offset(size.width - pad, size.height - pad), Offset(size.width - pad - bLen, size.height - pad), bracketStroke)
        drawLine(hudPrimary, Offset(size.width - pad, size.height - pad), Offset(size.width - pad, size.height - pad - bLen), bracketStroke)

        when (arMode) {
          ArHudMode.TELEMETRY_HUD, ArHudMode.NAV_WAYFINDER -> {
            val pitchOffsetPx = (telemetry.pitchDeg.coerceIn(-45f, 45f) / 45f) * (size.height * 0.16f)
            rotate(degrees = -telemetry.rollDeg.coerceIn(-60f, 60f), pivot = Offset(cx, cy)) {
              val hy = cy + pitchOffsetPx
              drawLine(hudPrimary, Offset(cx - 150f, hy), Offset(cx - 40f, hy), strokeWidth = 3f)
              drawLine(hudPrimary, Offset(cx + 40f, hy), Offset(cx + 150f, hy), strokeWidth = 3f)
              for (step in listOf(-2, -1, 1, 2)) {
                val py = hy + step * 48f
                val halfW = if (abs(step) == 1) 70f else 48f
                drawLine(
                  color = hudPrimary.copy(alpha = 0.65f),
                  start = Offset(cx - halfW, py),
                  end = Offset(cx + halfW, py),
                  strokeWidth = 2f,
                  pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
                )
              }
            }
            drawCircle(hudPrimary, radius = 14f, center = Offset(cx, cy), style = Stroke(width = 2.5f))
            drawLine(hudPrimary, Offset(cx - 26f, cy), Offset(cx - 14f, cy), strokeWidth = 2.5f)
            drawLine(hudPrimary, Offset(cx + 14f, cy), Offset(cx + 26f, cy), strokeWidth = 2.5f)

            val arcRadius = minOf(size.width, size.height) * 0.34f
            val arcTopLeft = Offset(cx - arcRadius, cy - arcRadius)
            val arcSize = Size(arcRadius * 2f, arcRadius * 2f)
            val speedFraction = (telemetry.speedKmh / 85.0).toFloat().coerceIn(0f, 1f)
            drawArc(hudPrimary.copy(alpha = 0.25f), 140f, 80f, false, arcTopLeft, arcSize, style = Stroke(width = 8f))
            drawArc(hudPrimary, 220f - 80f * speedFraction, 80f * speedFraction, false, arcTopLeft, arcSize, style = Stroke(width = 8f, cap = StrokeCap.Round))

            val battFraction = (telemetry.batteryPct / 100f).coerceIn(0f, 1f)
            drawArc(Color.White.copy(alpha = 0.25f), -40f, 80f, false, arcTopLeft, arcSize, style = Stroke(width = 8f))
            drawArc(
              if (telemetry.batteryPct <= 20) Color(0xFFFF3B30) else Color(0xFF00E5FF),
              40f - 80f * battFraction,
              80f * battFraction,
              false,
              arcTopLeft,
              arcSize,
              style = Stroke(width = 8f, cap = StrokeCap.Round),
            )
          }
          ArHudMode.SATELLITE_SKY -> {
            val skyR = minOf(size.width, size.height) * 0.33f
            drawCircle(Color(0xFF00E5FF).copy(alpha = 0.35f), skyR, Offset(cx, cy), style = Stroke(width = 2.5f))
            drawCircle(Color(0xFF00E5FF).copy(alpha = 0.25f), skyR * 0.66f, Offset(cx, cy), style = Stroke(width = 2f))
            drawCircle(Color(0xFF00E5FF).copy(alpha = 0.25f), skyR * 0.33f, Offset(cx, cy), style = Stroke(width = 2f))
            drawLine(Color(0xFF00E5FF).copy(alpha = 0.3f), Offset(cx - skyR, cy), Offset(cx + skyR, cy), strokeWidth = 1.5f)
            drawLine(Color(0xFF00E5FF).copy(alpha = 0.3f), Offset(cx, cy - skyR), Offset(cx, cy + skyR), strokeWidth = 1.5f)
            // Rotate sky dome by live magnetic heading
            val satCount = telemetry.satellitesVisible.coerceAtMost(24)
            rotate(degrees = -telemetry.bearingDeg, pivot = Offset(cx, cy)) {
              // North pointer on sky dome
              drawLine(Color(0xFFFF3B30), Offset(cx, cy - skyR), Offset(cx, cy - skyR - 22f), strokeWidth = 5f, cap = StrokeCap.Round)
              // Plot live connected vs visible satellite indicators around sky rings
              for (i in 0 until satCount) {
                val angleRad = Math.toRadians((i * (360.0 / maxOf(satCount, 1))) + 18.0)
                val ringFactor = 0.28f + ((i % 3) * 0.26f)
                val sx = cx + (cos(angleRad) * skyR * ringFactor).toFloat()
                val sy = cy + (sin(angleRad) * skyR * ringFactor).toFloat()
                val used = i < telemetry.satellitesUsed
                drawCircle(
                  color = if (used) Color(0xFF39FF14) else Color(0xFFFFB300),
                  radius = if (used) 10f else 7f,
                  center = Offset(sx, sy),
                )
              }
            }
          }
          ArHudMode.SLOPE_LEAN -> {
            val bubbleR = minOf(size.width, size.height) * 0.30f
            drawCircle(hudPrimary.copy(alpha = 0.4f), bubbleR, Offset(cx, cy), style = Stroke(width = 4f))
            drawCircle(hudPrimary.copy(alpha = 0.25f), bubbleR * 0.5f, Offset(cx, cy), style = Stroke(width = 2f))
            // Lean angle arc
            val leanSweep = telemetry.rollDeg.coerceIn(-60f, 60f)
            drawArc(
              color = if (abs(leanSweep) > settings.ahrsLeanAlertDeg) Color(0xFFFF3B30) else Color(0xFF39FF14),
              startAngle = -90f,
              sweepAngle = leanSweep,
              useCenter = false,
              topLeft = Offset(cx - bubbleR, cy - bubbleR),
              size = Size(bubbleR * 2f, bubbleR * 2f),
              style = Stroke(width = 12f, cap = StrokeCap.Round),
            )
            // Live inclinometer bubble
            val bx = cx + (telemetry.rollDeg.coerceIn(-45f, 45f) / 45f) * (bubbleR * 0.72f)
            val by = cy + (telemetry.pitchDeg.coerceIn(-45f, 45f) / 45f) * (bubbleR * 0.72f)
            drawCircle(Color(0xFFFFEA00), radius = 16f, center = Offset(bx, by))
            drawCircle(Color.Black, radius = 5f, center = Offset(bx, by))
          }
          ArHudMode.G_FORCE_DYNAMICS -> {
            val circleR = minOf(size.width, size.height) * 0.28f
            drawCircle(hudPrimary.copy(alpha = 0.22f), circleR, Offset(cx, cy), style = Stroke(2.5f))
            drawCircle(hudPrimary.copy(alpha = 0.18f), circleR * 0.66f, Offset(cx, cy), style = Stroke(1.8f))
            drawCircle(hudPrimary.copy(alpha = 0.14f), circleR * 0.33f, Offset(cx, cy), style = Stroke(1.5f))
            drawLine(hudPrimary.copy(alpha = 0.28f), Offset(cx - circleR, cy), Offset(cx + circleR, cy), strokeWidth = 1.5f)
            drawLine(hudPrimary.copy(alpha = 0.28f), Offset(cx, cy - circleR), Offset(cx, cy + circleR), strokeWidth = 1.5f)
            val gx = cx + (telemetry.lateralG / 1.5f).coerceIn(-1f, 1f) * circleR
            val gy = cy - (telemetry.longitudinalG / 1.5f).coerceIn(-1f, 1f) * circleR
            drawLine(hudPrimary.copy(alpha = 0.7f), Offset(cx, cy), Offset(gx, gy), strokeWidth = 3f, cap = StrokeCap.Round)
            drawCircle(Color(0xFFFFEA00).copy(alpha = 0.35f), radius = 16f, center = Offset(gx, gy))
            drawCircle(if (telemetry.peakG >= 1.0f) Color(0xFFFF3B30) else Color(0xFF00F0FF), radius = 8f, center = Offset(gx, gy))
          }
          ArHudMode.OPTICAL_INSPECT -> Unit
        }

        if (showCaliper || arMode == ArHudMode.OPTICAL_INSPECT) {
          val halfSpan = (size.width * 0.42f) * caliperWidthPct.coerceIn(0.15f, 0.95f)
          val topY = cy - 90f
          val botY = cy + 90f
          drawLine(Color(0xFFFFEA00), Offset(cx - halfSpan, topY), Offset(cx - halfSpan, botY), strokeWidth = 4f)
          drawLine(Color(0xFFFFEA00), Offset(cx + halfSpan, topY), Offset(cx + halfSpan, botY), strokeWidth = 4f)
          drawLine(
            color = Color(0xFFFFEA00),
            start = Offset(cx - halfSpan, cy),
            end = Offset(cx + halfSpan, cy),
            strokeWidth = 2.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f)),
          )
          drawCircle(Color(0xFFFFEA00), radius = 6f, center = Offset(cx - halfSpan, cy))
          drawCircle(Color(0xFFFFEA00), radius = 6f, center = Offset(cx + halfSpan, cy))
        }

        if (targetBearingDeg != null && arMode != ArHudMode.SATELLITE_SKY) {
          val relDeg = (((targetBearingDeg - telemetry.bearingDeg) + 540.0) % 360.0) - 180.0
          val rad = Math.toRadians(relDeg)
          val arrowR = minOf(size.width, size.height) * 0.24f
          val tipX = cx + (sin(rad) * arrowR).toFloat()
          val tipY = cy - (cos(rad) * arrowR).toFloat()
          val chevron =
            Path().apply {
              moveTo(tipX, tipY)
              lineTo(tipX - 18f, tipY + 26f)
              lineTo(tipX + 18f, tipY + 26f)
              close()
            }
          rotate(degrees = relDeg.toFloat(), pivot = Offset(tipX, tipY)) {
            drawPath(chevron, color = Color(0xFF00F0FF))
          }
        }
      }

      // TOP HUD BAR
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Surface(color = Color(0xCC070D17), shape = chrome.cardShape) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(Modifier.weight(1f)) {
              Text(
                "LIVE AR • ${arMode.label.uppercase()}",
                style = MaterialTheme.typography.labelMedium,
                color = hudPrimary,
                fontWeight = FontWeight.Black,
              )
              Text(
                if (arMode == ArHudMode.G_FORCE_DYNAMICS) {
                  "Lat ${"%.2f".format(telemetry.lateralG)}G • Long ${"%.2f".format(telemetry.longitudinalG)}G • Peak ${"%.2f".format(telemetry.peakG)}G"
                } else if (arMode == ArHudMode.OPTICAL_INSPECT) {
                  "Focus: $selectedPartTag • Bump ${"%.2f".format(telemetry.roadRoughnessG)}G"
                } else {
                  "Heading ${telemetry.bearingDeg.toInt()}° • Lean ${"%.1f".format(telemetry.rollDeg)}° • Slope ${"%.0f".format(gradePct)}%"
                },
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFE2E8F0),
              )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
              IconButton(
                onClick = {
                  if (settings.hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  torchOn = !torchOn
                },
                modifier = Modifier.testTag("ar_torch_button"),
              ) {
                Icon(
                  if (torchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                  contentDescription = "Toggle Flashlight",
                  tint = if (torchOn) Color(0xFFFFEA00) else Color.White,
                )
              }
              IconButton(onClick = { showGrid = !showGrid }) {
                Icon(Icons.Default.GridOn, contentDescription = "Toggle Alignment Grid", tint = if (showGrid) hudPrimary else Color.White)
              }
              IconButton(onClick = { showCaliper = !showCaliper }) {
                Icon(Icons.Default.Straighten, contentDescription = "Toggle Ruler", tint = if (showCaliper) Color(0xFFFFEA00) else Color.White)
              }
              IconButton(onClick = onDismiss, modifier = Modifier.testTag("ar_close_button")) {
                Icon(Icons.Default.Close, contentDescription = "Close AR View", tint = Color.White)
              }
            }
          }
        }

        // Mode & Visual Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(ArHudMode.entries) { m ->
            TacticalFilterChip(
              selected = arMode == m,
              onClick = {
                if (settings.hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                arMode = m
                if (m == ArHudMode.OPTICAL_INSPECT) showCaliper = true
              },
              label = m.label,
            )
          }
          items(ArVisualFilter.entries) { f ->
            TacticalFilterChip(
              selected = arFilter == f,
              onClick = {
                if (settings.hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                arFilter = f
              },
              label = f.label,
            )
          }
        }

        // Quick AR Part Inspection Tags & Caliper Reference Presets
        if (arMode == ArHudMode.OPTICAL_INSPECT || showCaliper) {
          val partTags = listOf("Brake Caliper", "Tire Tread", "Stem Hinge", "Battery/BMS", "Hall Plug")
          val presetsMm = listOf(140f to "140mm", 160f to "160mm", 180f to "180mm", 203f to "203mm", 100f to "100mm Tire")
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(partTags) { tag ->
              TacticalFilterChip(
                selected = selectedPartTag == tag,
                onClick = { selectedPartTag = tag },
                label = tag,
              )
            }
            items(presetsMm) { (mm, lbl) ->
              val targetPct = ((mm * (1f + linearZoom * 2.5f)) / 180f).coerceIn(0.15f, 0.95f)
              TacticalFilterChip(
                selected = abs(caliperWidthPct - targetPct) < 0.04f,
                onClick = { caliperWidthPct = targetPct },
                label = lbl,
              )
            }
          }
        }

        // Live GPS Satellites & Environmental Sensor Strip inside AR
        if (settings.arShowSatelliteOverlay || arMode == ArHudMode.SATELLITE_SKY) {
          Surface(color = Color(0xCC0A1626), shape = chrome.smallShape) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(Icons.Default.SatelliteAlt, contentDescription = null, tint = Color(0xFF39FF14), modifier = Modifier.size(18.dp))
              val satText =
                if (telemetry.satellitesVisible > 0) {
                  "GPS Satellites: ${telemetry.satellitesUsed} Locked / ${telemetry.satellitesVisible} Visible (${"%.0f".format(telemetry.avgSatelliteSnrDb)} dB)"
                } else if (telemetry.lat != 0.0) {
                  "GPS Active (±${telemetry.gpsAccuracyM.toInt()}m) • Scanning Satellites"
                } else {
                  "Scanning GPS Satellites..."
                }
              val envExtras =
                buildList {
                  if (telemetry.barometerHpa > 0f) add("${"%.0f".format(telemetry.barometerHpa)} hPa")
                  if (telemetry.ambientLightLux >= 0f) add("${telemetry.ambientLightLux.toInt()} lux")
                  if (telemetry.roadRoughnessG > 0.01f) add("Bump ${"%.2f".format(telemetry.roadRoughnessG)}G")
                }.joinToString(" • ")
              Column(Modifier.weight(1f)) {
                Text(satText, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                if (envExtras.isNotBlank() || telemetry.satelliteConstellations.isNotBlank()) {
                  Text(
                    listOf(telemetry.satelliteConstellations, envExtras).filter { it.isNotBlank() }.joinToString(" • "),
                    color = Color(0xFF93C5FD),
                    style = MaterialTheme.typography.labelSmall,
                  )
                }
              }
            }
          }
        }

        // Turn Guide or Nearest Charger Banner
        if (activeNavRoute != null) {
          val step = activeNavRoute.steps.getOrNull(navStepIndex)
          Surface(color = Color(0xD90A1626), shape = RoundedCornerShape(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(Icons.Default.Navigation, contentDescription = null, tint = Color(0xFF00F0FF))
              Column(Modifier.weight(1f)) {
                Text(
                  "Next Turn: ${step?.instruction ?: activeNavRoute.destinationName}",
                  color = Color.White,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  "In ${navDistToTurnM.toInt()}m • Destination ${targetDistanceKm?.let { "${"%.2f".format(formatDistValue(it, settings.distanceUnit))} ${distUnitLabel(settings.distanceUnit)}" } ?: "--"}",
                  color = Color(0xFF00F0FF),
                  style = MaterialTheme.typography.labelSmall,
                )
              }
              if (onCancelNav != null) {
                Surface(
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCancelNav()
                    Toast.makeText(ctx, "Navigation cancelled", Toast.LENGTH_SHORT).show()
                  },
                  color = Color(0xFFE11D48),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.testTag("ar_cancel_nav_button"),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel Navigation", tint = Color.White, modifier = Modifier.size(16.dp))
                    Text("Cancel", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        } else if (nearestCharger != null && targetDistanceKm != null) {
          Surface(color = Color(0xCC0A1626), shape = RoundedCornerShape(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = Color(0xFF39FF14))
              Text(
                "Nearest Charger: ${nearestCharger.title} (${"%.2f".format(formatDistValue(targetDistanceKm, settings.distanceUnit))} ${distUnitLabel(settings.distanceUnit)})",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
              )
            }
          }
        }
      }

      // BOTTOM AR READOUT + CONTROLS
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Surface(color = Color(0xCC070D17), shape = chrome.cardShape) {
          Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  "${"%.1f".format(formatSpeedValue(telemetry.speedKmh, settings.distanceUnit))} ${speedUnitLabel(settings.distanceUnit)}",
                  style = MaterialTheme.typography.headlineSmall,
                  color = hudPrimary,
                  fontWeight = FontWeight.Black,
                )
                Text("LIVE SPEED", style = MaterialTheme.typography.labelSmall, color = Color(0xFFCBD5E1))
              }
              HudSpeedLimitBadge(telemetry = telemetry, settings = settings)
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  "${"%.1f".format(telemetry.estimatedVoltage)}V",
                  style = MaterialTheme.typography.titleLarge,
                  color = Color(0xFFFFEA00),
                  fontWeight = FontWeight.Bold,
                )
                Text("VOLTS", style = MaterialTheme.typography.labelSmall, color = Color(0xFFCBD5E1))
              }
              Column(horizontalAlignment = Alignment.End) {
                Text(
                  "${telemetry.batteryPct}%",
                  style = MaterialTheme.typography.headlineSmall,
                  color = if (telemetry.batteryPct <= 20) Color(0xFFFF3B30) else Color(0xFF00F0FF),
                  fontWeight = FontWeight.Black,
                )
                Text("BATTERY", style = MaterialTheme.typography.labelSmall, color = Color(0xFFCBD5E1))
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Text("Zoom ${(1f + linearZoom * 4f).let { "%.1fx".format(it) }}", style = MaterialTheme.typography.labelSmall, color = Color.White)
              TacticalSlider(value = linearZoom, onValueChange = { linearZoom = it }, valueRange = 0f..1f, modifier = Modifier.weight(1f))
            }

            val expState = boundCamera?.cameraInfo?.exposureState
            if (expState?.isExposureCompensationSupported == true) {
              val minEv = expState.exposureCompensationRange.lower.toFloat()
              val maxEv = expState.exposureCompensationRange.upper.toFloat()
              if (maxEv > minEv) {
                val evSteps = (maxEv - minEv).roundToInt().coerceAtLeast(1)
                val evVal = exposureIdx * expState.exposureCompensationStep.toFloat()
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Text("EV ${"%+.1f".format(evVal)}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF93C5FD))
                  TacticalSlider(
                    value = exposureIdx.toFloat().coerceIn(minEv, maxEv),
                    onValueChange = { exposureIdx = it.roundToInt() },
                    valueRange = minEv..maxEv,
                    steps = (evSteps - 1).coerceAtLeast(0),
                    modifier = Modifier.weight(1f),
                  )
                }
              }
            }

            if (showCaliper || arMode == ArHudMode.OPTICAL_INSPECT) {
              val approxMm = (caliperWidthPct * 180f / (1f + linearZoom * 2.5f)).toInt()
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ruler ~${approxMm}mm", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFEA00))
                TacticalSlider(value = caliperWidthPct, onValueChange = { caliperWidthPct = it }, valueRange = 0.15f..0.95f, modifier = Modifier.weight(1f))
              }
            }

            if (onCaptureForVisionAi != null && hasCamPerm) {
              TacticalButton(
                onClick = {
                  if (settings.hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                  if (cameraBindError != null && previewViewRef?.bitmap == null) {
                    Toast.makeText(ctx, "Camera unavailable ($cameraBindError). Use Upload Photo instead.", Toast.LENGTH_SHORT).show()
                    return@TacticalButton
                  }
                  imageCapture.takePicture(
                    ContextCompat.getMainExecutor(ctx),
                    object : ImageCapture.OnImageCapturedCallback() {
                      override fun onCaptureSuccess(image: ImageProxy) {
                        val bmp =
                          runCatching { image.toBitmap() }.getOrNull()
                            ?: runCatching {
                              val buf = image.planes[0].buffer
                              val bytes = ByteArray(buf.remaining())
                              buf.get(bytes)
                              BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            }.getOrNull()
                            ?: previewViewRef?.bitmap
                        image.close()
                        if (bmp != null) {
                          onCaptureForVisionAi(bmp)
                          onDismiss()
                        } else {
                          Toast.makeText(ctx, "Could not capture camera frame — try again", Toast.LENGTH_SHORT).show()
                        }
                      }

                      override fun onError(exception: ImageCaptureException) {
                        val fallback = previewViewRef?.bitmap
                        if (fallback != null) {
                          onCaptureForVisionAi(fallback)
                          onDismiss()
                        } else {
                          Toast.makeText(ctx, "AR capture failed: ${exception.message ?: "Camera busy"}", Toast.LENGTH_SHORT).show()
                        }
                      }
                    },
                  )
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("ar_capture_vision_button"),
              ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Snap $selectedPartTag for AI Inspection")
              }
            }
          }
        }
      }
    }
    }
  }
}
