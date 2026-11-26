package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LiveTelemetrySnapshot
import com.example.data.MasterSettings
import com.example.ui.theme.LocalTacticalChrome
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

fun headingCardinalText(bearingDeg: Float): String {
  val norm = ((bearingDeg % 360f) + 360f) % 360f
  return when {
    norm < 22.5f || norm >= 337.5f -> "N"
    norm < 67.5f -> "NE"
    norm < 112.5f -> "E"
    norm < 157.5f -> "SE"
    norm < 202.5f -> "S"
    norm < 247.5f -> "SW"
    norm < 292.5f -> "W"
    else -> "NW"
  }
}

/**
 * Live scrolling magnetic + GPS heading compass ribbon with optional pitch/roll attitude indicator embedded across all HUD modes.
 */
@Composable
fun LiveHudCompassTape(
  bearingDeg: Float,
  accent: Color,
  secondary: Color,
  modifier: Modifier = Modifier,
  pitchDeg: Float = 0f,
  rollDeg: Float = 0f,
) {
  val norm = ((bearingDeg % 360f) + 360f) % 360f
  val degInt = norm.roundToInt() % 360
  val cardinal = headingCardinalText(norm)
  val chrome = LocalTacticalChrome.current
  val onSurface = MaterialTheme.colorScheme.onSurface
  val surfaceTint = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
  val borderTint = accent.copy(alpha = 0.45f)

  Surface(
    color = surfaceTint,
    shape = chrome.smallShape,
    border = BorderStroke(chrome.buttonBorderWidth, borderTint),
    modifier = modifier.fillMaxWidth().height(36.dp).testTag("hud_live_compass_tape"),
  ) {
    Row(
      modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // Digital heading degree & cardinal pill
      Surface(
        color = accent.copy(alpha = 0.2f),
        shape = chrome.smallShape,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.65f)),
      ) {
        Text(
          text = "%03d° %s".format(degInt, cardinal),
          fontSize = 10.sp,
          fontWeight = FontWeight.Black,
          color = accent,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          maxLines = 1,
          softWrap = false,
        )
      }

      // Live scrolling horizontal compass tape
      Canvas(modifier = Modifier.weight(1f).height(26.dp)) {
        val cx = size.width / 2f
        val pxPerDeg = size.width / 130f
        val textPaint =
          android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 9.5.sp.toPx()
            isFakeBoldText = true
          }

        // Lubber triangle at center top
        val tri =
          Path().apply {
            moveTo(cx, 6f)
            lineTo(cx - 5f, 0f)
            lineTo(cx + 5f, 0f)
            close()
          }
        drawPath(tri, color = accent)

        val baseDeg = norm.toInt()
        for (offset in -65..65) {
          val deg = ((baseDeg + offset) % 360 + 360) % 360
          if (deg % 5 != 0) continue
          val x = cx + (offset - (norm - baseDeg)) * pxPerDeg
          if (x < 4f || x > size.width - 4f) continue

          val isCardinal = deg % 90 == 0
          val isInter = deg % 45 == 0
          val isMajor = deg % 15 == 0

          val tickTop = if (isCardinal) 7f else if (isInter) 9f else if (isMajor) 12f else 15f
          val tickColor =
            when {
              deg == 0 -> Color(0xFFFF4D6D) // North highlighted crisp red
              isCardinal -> accent
              isInter -> secondary
              else -> onSurface.copy(alpha = 0.45f)
            }
          drawLine(
            color = tickColor,
            start = Offset(x, tickTop),
            end = Offset(x, size.height - 9f),
            strokeWidth = if (isCardinal) 2.2f else if (isMajor) 1.5f else 1f,
          )

          val lbl =
            when (deg) {
              0 -> "N"
              45 -> "NE"
              90 -> "E"
              135 -> "SE"
              180 -> "S"
              225 -> "SW"
              270 -> "W"
              315 -> "NW"
              else -> if (deg % 30 == 0) "$deg" else null
            }
          if (lbl != null) {
            textPaint.color =
              when {
                deg == 0 -> Color(0xFFFF4D6D).toArgb()
                isCardinal -> accent.toArgb()
                else -> onSurface.copy(alpha = 0.85f).toArgb()
              }
            drawContext.canvas.nativeCanvas.drawText(lbl, x, size.height - 1f, textPaint)
          }
        }
      }

      // Live micro Pitch / Roll Attitude Bank Pill
      Surface(
        color = secondary.copy(alpha = 0.16f),
        shape = chrome.smallShape,
        border = BorderStroke(0.8.dp, secondary.copy(alpha = 0.5f)),
      ) {
        Text(
          text = "R${"%.0f".format(rollDeg)}°",
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Black,
          color = secondary,
          modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
          maxLines = 1,
          softWrap = false,
        )
      }
    }
  }
}

// ============================================================================
// 7 PRO NEW PEV HUDS (SUPERBIKE, F1, FIGHTER JET, COMPASS ROSE, DUAL TWIN, RETRO SYNTH, EUC LEAN)
// ============================================================================

@Composable
fun SuperbikeTachHud(
  dispSpeed: Double,
  unitLbl: String,
  progress: Float,
  telemetry: LiveTelemetrySnapshot,
  settings: MasterSettings,
  accent: Color,
  secondary: Color,
  surfaceBg: Color,
) {
  val chrome = LocalTacticalChrome.current
  val peakFrac = ((formatSpeedValue(telemetry.maxSpeedKmh, settings.distanceUnit)) / 60.0).toFloat().coerceIn(0f, 1f)
  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    // Top Shift-Light LED Strip + Lean Pill
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        for (led in 0 until 8) {
          val lit = (led + 1) / 8f <= progress
          val c =
            when {
              !lit -> surfaceBg
              led >= 6 -> Color(0xFFFF2E55)
              led >= 4 -> Color(0xFFFFB020)
              else -> accent
            }
          Box(
            modifier =
              Modifier
                .size(width = 12.dp, height = 6.dp)
                .background(c, chrome.smallShape),
          )
        }
      }
      Surface(
        color = secondary.copy(alpha = 0.16f),
        shape = chrome.smallShape,
        border = BorderStroke(1.dp, secondary.copy(alpha = 0.55f)),
      ) {
        Text(
          "LEAN ${"%.0f".format(abs(telemetry.rollDeg))}° • ${"%.1f".format(telemetry.peakG)}G",
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Black,
          color = secondary,
          modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
        )
      }
    }

    // Sweeping MotoGP curved tachometer bar + peak marker
    Box(modifier = Modifier.fillMaxWidth().height(138.dp), contentAlignment = Alignment.BottomCenter) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val segs = 28
        val startX = 10f
        val endX = size.width - 10f
        val totalW = endX - startX
        for (i in 0 until segs) {
          val frac = i.toFloat() / (segs - 1)
          val active = frac <= progress
          val isPeakBar = abs(frac - peakFrac) < (1f / segs) && peakFrac > 0.08f
          val curveY =
            if (frac < 0.35f) {
              val t = frac / 0.35f
              size.height * (0.86f - t * 0.62f)
            } else {
              val t = (frac - 0.35f) / 0.65f
              size.height * (0.24f - t * 0.14f)
            }
          val barH = size.height * (0.14f + frac * 0.16f)
          val x = startX + frac * totalW
          val col =
            when {
              isPeakBar -> Color(0xFFFFEA00)
              !active -> surfaceBg.copy(alpha = 0.7f)
              frac > 0.82f -> Color(0xFFFF2E55)
              frac > 0.65f -> Color(0xFFFFB020)
              else -> accent
            }
          drawRoundRect(
            color = col,
            topLeft = Offset(x - 4f, curveY),
            size = Size((totalW / segs) * 0.72f, barH),
            cornerRadius = CornerRadius(3f, 3f),
          )
        }
      }
      Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
      ) {
        Column {
          Text(
            text = "%.${settings.decimalPrecision.coerceIn(0, 1)}f".format(dispSpeed),
            fontSize = 46.sp,
            lineHeight = 46.sp,
            fontWeight = FontWeight.Black,
            color = accent,
          )
          Text(
            "$unitLbl • MODE ${settings.rideMode}",
            style = MaterialTheme.typography.labelMedium,
            color = secondary,
            fontWeight = FontWeight.Bold,
          )
        }
        Column(horizontalAlignment = Alignment.End) {
          Text("BAT ${telemetry.batteryPct}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = accent)
          Text("${"%.1f".format(telemetry.estimatedVoltage)}V", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("PEAK ${"%.1f".format(formatSpeedValue(telemetry.maxSpeedKmh, settings.distanceUnit))}", style = MaterialTheme.typography.labelSmall, color = secondary)
        }
      }
    }
  }
}

@Composable
fun F1TelemetryWheelHud(
  dispSpeed: Double,
  unitLbl: String,
  progress: Float,
  telemetry: LiveTelemetrySnapshot,
  settings: MasterSettings,
  accent: Color,
  secondary: Color,
  surfaceBg: Color,
) {
  val chrome = LocalTacticalChrome.current
  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // 15 Sequential F1 Shift LEDs (5 Green -> 5 Red -> 5 Blue)
    Canvas(modifier = Modifier.fillMaxWidth().height(18.dp)) {
      val leds = 15
      val spacing = size.width / leds
      for (i in 0 until leds) {
        val active = i.toFloat() / leds < progress
        val ledColor =
          when {
            !active -> surfaceBg
            i < 5 -> Color(0xFF22C55E)
            i < 10 -> Color(0xFFFF3B30)
            else -> Color(0xFF38BDF8)
          }
        drawCircle(
          color = ledColor,
          radius = 6.5.dp.toPx(),
          center = Offset(spacing * i + spacing / 2f, size.height / 2f),
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Left Pit Telemetry Pod with ERS bar
      Surface(
        color = surfaceBg.copy(alpha = 0.65f),
        shape = chrome.smallShape,
        border = BorderStroke(chrome.buttonBorderWidth, accent.copy(alpha = 0.42f)),
        modifier = Modifier.weight(1f),
      ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("ERS / SOC", fontSize = 9.sp, color = secondary, fontWeight = FontWeight.Bold)
          Text("${telemetry.batteryPct}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = accent)
          Box(
            Modifier
              .fillMaxWidth()
              .height(5.dp)
              .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), chrome.smallShape),
          ) {
            Box(
              Modifier
                .fillMaxWidth((telemetry.batteryPct / 100f).coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(if (telemetry.batteryPct <= 20) Color(0xFFFF3B30) else accent, chrome.smallShape),
            )
          }
          Text("${"%.1f".format(telemetry.estimatedVoltage)}V", style = MaterialTheme.typography.labelSmall)
        }
      }

      // Center Giant Speed Pod
      Surface(
        color = Color(0xDD060A12),
        shape = chrome.cardShape,
        border = BorderStroke(1.6.dp, accent),
        modifier = Modifier.weight(1.45f),
      ) {
        Column(
          modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(settings.rideMode, fontSize = 10.sp, fontWeight = FontWeight.Black, color = secondary)
          Text(
            text = "%.${settings.decimalPrecision.coerceIn(0, 1)}f".format(dispSpeed),
            fontSize = 44.sp,
            lineHeight = 46.sp,
            fontWeight = FontWeight.Black,
            color = accent,
          )
          Text(unitLbl, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
      }

      // Right G-Force Ball & Sector Pod
      Surface(
        color = surfaceBg.copy(alpha = 0.65f),
        shape = chrome.smallShape,
        border = BorderStroke(chrome.buttonBorderWidth, secondary.copy(alpha = 0.42f)),
        modifier = Modifier.weight(1f),
      ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp), horizontalAlignment = Alignment.End) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Canvas(modifier = Modifier.size(22.dp)) {
              val c = Offset(size.width / 2f, size.height / 2f)
              val r = size.minDimension * 0.44f
              drawCircle(secondary.copy(alpha = 0.4f), r, c, style = Stroke(1.2.dp.toPx()))
              val gx = c.x + (telemetry.lateralG / 1.5f).coerceIn(-1f, 1f) * r * 0.75f
              val gy = c.y + (telemetry.longitudinalG / 1.5f).coerceIn(-1f, 1f) * r * 0.75f
              drawCircle(accent, 3.dp.toPx(), Offset(gx, gy))
            }
            Text("${"%.2f".format(telemetry.peakG)}G", fontSize = 10.sp, color = secondary, fontWeight = FontWeight.Black)
          }
          Text(
            "%.1f".format(formatDistValue(telemetry.tripDistanceKm, settings.distanceUnit)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
          )
          Text("${distUnitLabel(settings.distanceUnit).uppercase()} • MAX ${"%.0f".format(formatSpeedValue(telemetry.maxSpeedKmh, settings.distanceUnit))}", style = MaterialTheme.typography.labelSmall, color = accent)
        }
      }
    }
  }
}

@Composable
fun FighterJetHud(
  dispSpeed: Double,
  unitLbl: String,
  telemetry: LiveTelemetrySnapshot,
  settings: MasterSettings,
  accent: Color,
) {
  val chrome = LocalTacticalChrome.current
  val hudGreen = if (settings.nightVisionMode) accent else Color(0xFF39FF14)
  Box(
    modifier =
      Modifier
        .fillMaxWidth()
        .height(176.dp)
        .background(Color(0xE0040B07), chrome.cardShape),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
      val cx = size.width / 2f
      val cy = size.height / 2f

      // Top bank scale arc (-45° to +45°) & sky roll pointer
      val bankR = size.minDimension * 0.44f
      drawArc(
        color = hudGreen.copy(alpha = 0.5f),
        startAngle = 225f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(cx - bankR, cy - bankR + 12f),
        size = Size(bankR * 2f, bankR * 2f),
        style = Stroke(1.6f),
      )
      for (bDeg in listOf(-45, -30, -15, 0, 15, 30, 45)) {
        val bRad = Math.toRadians((270 + bDeg).toDouble())
        val tx1 = cx + (bankR * cos(bRad)).toFloat()
        val ty1 = cy + 12f + (bankR * sin(bRad)).toFloat()
        val tx2 = cx + ((bankR + 7f) * cos(bRad)).toFloat()
        val ty2 = cy + 12f + ((bankR + 7f) * sin(bRad)).toFloat()
        drawLine(hudGreen, Offset(tx1, ty1), Offset(tx2, ty2), strokeWidth = if (bDeg == 0) 2.5f else 1.4f)
      }

      // Pitch ladder rotated by roll
      rotate(degrees = -telemetry.rollDeg.coerceIn(-45f, 45f), pivot = Offset(cx, cy)) {
        val pitchShift = (telemetry.pitchDeg * 2.2f).coerceIn(-45f, 45f)
        drawLine(hudGreen, Offset(cx - 72f, cy + pitchShift), Offset(cx - 20f, cy + pitchShift), strokeWidth = 2.4f)
        drawLine(hudGreen, Offset(cx + 20f, cy + pitchShift), Offset(cx + 72f, cy + pitchShift), strokeWidth = 2.4f)
        listOf(-30f, 30f).forEach { dy ->
          drawLine(hudGreen.copy(alpha = 0.65f), Offset(cx - 42f, cy + pitchShift + dy), Offset(cx - 16f, cy + pitchShift + dy), strokeWidth = 1.6f)
          drawLine(hudGreen.copy(alpha = 0.65f), Offset(cx + 16f, cy + pitchShift + dy), Offset(cx + 42f, cy + pitchShift + dy), strokeWidth = 1.6f)
        }
      }

      // Flight-path vector reticle in center
      drawCircle(hudGreen, radius = 9f, center = Offset(cx, cy), style = Stroke(2f))
      drawLine(hudGreen, Offset(cx - 22f, cy), Offset(cx - 9f, cy), strokeWidth = 2f)
      drawLine(hudGreen, Offset(cx + 9f, cy), Offset(cx + 22f, cy), strokeWidth = 2f)
      drawLine(hudGreen, Offset(cx, cy - 18f), Offset(cx, cy - 9f), strokeWidth = 2f)

      // Left speed tape vertical rail & ticks
      val leftX = 64f
      drawLine(hudGreen, Offset(leftX, 16f), Offset(leftX, size.height - 16f), strokeWidth = 2f)
      for (i in 0..6) {
        val y = 20f + i * ((size.height - 40f) / 6f)
        drawLine(hudGreen.copy(alpha = 0.7f), Offset(leftX - 8f, y), Offset(leftX, y), strokeWidth = 1.5f)
      }

      // Right battery/alt tape vertical rail & ticks
      val rightX = size.width - 64f
      drawLine(hudGreen, Offset(rightX, 16f), Offset(rightX, size.height - 16f), strokeWidth = 2f)
      for (i in 0..6) {
        val y = 20f + i * ((size.height - 40f) / 6f)
        drawLine(hudGreen.copy(alpha = 0.7f), Offset(rightX, y), Offset(rightX + 8f, y), strokeWidth = 1.5f)
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(color = Color(0xCC06140A), border = BorderStroke(1.2.dp, hudGreen), shape = chrome.smallShape) {
        Column(Modifier.padding(horizontal = 7.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
          Text("%.1f".format(dispSpeed), fontSize = 20.sp, fontWeight = FontWeight.Black, color = hudGreen)
          Text(unitLbl, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = hudGreen)
        }
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 98.dp)) {
        val altVal = if (settings.distanceUnit == "km") "${telemetry.altitudeM.toInt()}m" else "${(telemetry.altitudeM * 3.28084).roundToInt()}ft"
        Text(
          "HDG ${"%03d".format(((telemetry.bearingDeg.toInt() % 360) + 360) % 360)}° • ALT $altVal • P ${"%.0f".format(telemetry.pitchDeg)}° R ${"%.0f".format(telemetry.rollDeg)}°",
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Bold,
          color = hudGreen,
        )
      }

      Surface(color = Color(0xCC06140A), border = BorderStroke(1.2.dp, hudGreen), shape = chrome.smallShape) {
        Column(Modifier.padding(horizontal = 7.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
          Text("${telemetry.batteryPct}%", fontSize = 18.sp, fontWeight = FontWeight.Black, color = hudGreen)
          Text("${"%.1f".format(telemetry.estimatedVoltage)}V", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = hudGreen)
        }
      }
    }
  }
}

@Composable
fun TacticalCompassRoseHud(
  dispSpeed: Double,
  unitLbl: String,
  progress: Float,
  telemetry: LiveTelemetrySnapshot,
  settings: MasterSettings,
  accent: Color,
  secondary: Color,
  surfaceBg: Color,
) {
  val norm = ((telemetry.bearingDeg % 360f) + 360f) % 360f
  val onSurface = MaterialTheme.colorScheme.onSurface
  val peakFrac = ((formatSpeedValue(telemetry.maxSpeedKmh, settings.distanceUnit)) / 60.0).toFloat().coerceIn(0f, 1f)

  Box(contentAlignment = Alignment.Center, modifier = Modifier.size(186.dp)) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = size.width / 2f
      val cy = size.height / 2f
      val outerR = size.minDimension * 0.47f
      val innerR = size.minDimension * 0.34f

      val lubber =
        Path().apply {
          moveTo(cx, 2f)
          lineTo(cx - 6f, 12f)
          lineTo(cx + 6f, 12f)
          close()
        }
      drawPath(lubber, Color(0xFFFF4D6D))

      rotate(degrees = -norm, pivot = Offset(cx, cy)) {
        drawCircle(surfaceBg.copy(alpha = 0.65f), radius = outerR, style = Stroke(2.5f))
        val paint =
          android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 10.sp.toPx()
            isFakeBoldText = true
          }
        for (deg in 0 until 360 step 15) {
          val rad = Math.toRadians((deg - 90).toDouble())
          val isCard = deg % 90 == 0
          val is30 = deg % 30 == 0
          val tickLen = if (isCard) 12f else if (is30) 9f else 5f
          val x1 = cx + ((outerR - tickLen) * cos(rad)).toFloat()
          val y1 = cy + ((outerR - tickLen) * sin(rad)).toFloat()
          val x2 = cx + (outerR * cos(rad)).toFloat()
          val y2 = cy + (outerR * sin(rad)).toFloat()
          val col = if (deg == 0) Color(0xFFFF4D6D) else if (isCard) accent else onSurface.copy(alpha = 0.5f)
          drawLine(col, Offset(x1, y1), Offset(x2, y2), strokeWidth = if (isCard) 2.8f else 1.4f)

          if (isCard || deg % 60 == 0) {
            val lbl = when (deg) { 0 -> "N"; 90 -> "E"; 180 -> "S"; 270 -> "W"; else -> "$deg" }
            paint.color = if (deg == 0) Color(0xFFFF4D6D).toArgb() else if (isCard) accent.toArgb() else onSurface.copy(alpha = 0.72f).toArgb()
            val tx = cx + ((outerR - 22f) * cos(rad)).toFloat()
            val ty = cy + ((outerR - 22f) * sin(rad)).toFloat() + 4f
            drawContext.canvas.nativeCanvas.drawText(lbl, tx, ty, paint)
          }
        }
      }

      drawArc(
        color = surfaceBg,
        startAngle = 135f,
        sweepAngle = 270f,
        useCenter = false,
        topLeft = Offset(cx - innerR, cy - innerR),
        size = Size(innerR * 2f, innerR * 2f),
        style = Stroke(width = 8f, cap = StrokeCap.Round),
      )
      drawArc(
        brush = Brush.sweepGradient(listOf(accent, secondary, accent)),
        startAngle = 135f,
        sweepAngle = 270f * progress,
        useCenter = false,
        topLeft = Offset(cx - innerR, cy - innerR),
        size = Size(innerR * 2f, innerR * 2f),
        style = Stroke(width = 8f, cap = StrokeCap.Round),
      )
      if (peakFrac > 0.08f) {
        val pkRad = Math.toRadians((135f + 270f * peakFrac).toDouble())
        drawCircle(
          color = Color(0xFFFFEA00),
          radius = 4.dp.toPx(),
          center = Offset(cx + (innerR * cos(pkRad)).toFloat(), cy + (innerR * sin(pkRad)).toFloat()),
        )
      }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = "%.${settings.decimalPrecision.coerceIn(0, 1)}f".format(dispSpeed),
        fontSize = 32.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Black,
        color = accent,
      )
      Text(unitLbl, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(
        "${norm.roundToInt()}° ${headingCardinalText(norm)}",
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = secondary,
      )
    }
  }
}

@Composable
fun DualTwinClusterHud(
  dispSpeed: Double,
  unitLbl: String,
  progress: Float,
  telemetry: LiveTelemetrySnapshot,
  settings: MasterSettings,
  accent: Color,
  secondary: Color,
  surfaceBg: Color,
) {
  val batFrac = (telemetry.batteryPct / 100f).coerceIn(0f, 1f)
  val tickTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // Left Speedometer Dial with graduated ticks
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(146.dp)) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension * 0.42f
        for (t in 0..10) {
          val td = 135f + t * 27f
          val rad = Math.toRadians(td.toDouble())
          drawLine(
            color = tickTint,
            start = Offset(c.x + ((r - 6f) * cos(rad)).toFloat(), c.y + ((r - 6f) * sin(rad)).toFloat()),
            end = Offset(c.x + ((r + 4f) * cos(rad)).toFloat(), c.y + ((r + 4f) * sin(rad)).toFloat()),
            strokeWidth = if (t % 2 == 0) 2.2f else 1.2f,
          )
        }
        drawArc(surfaceBg, 135f, 270f, false, style = Stroke(11f, cap = StrokeCap.Round))
        drawArc(accent, 135f, 270f * progress, false, style = Stroke(11f, cap = StrokeCap.Round))
        val rad = Math.toRadians((135f + 270f * progress).toDouble())
        val nr = size.minDimension * 0.34f
        drawLine(Color(0xFFFF4D6D), c, Offset(c.x + (nr * cos(rad)).toFloat(), c.y + (nr * sin(rad)).toFloat()), strokeWidth = 4f, cap = StrokeCap.Round)
        drawCircle(accent, radius = 4f, center = c)
      }
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("%.1f".format(dispSpeed), fontSize = 26.sp, fontWeight = FontWeight.Black, color = accent)
        Text(unitLbl, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }

    // Center Mini Gear / Mode Indicator
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(settings.rideMode, fontSize = 10.sp, fontWeight = FontWeight.Black, color = secondary)
      Text("TRIP (${distUnitLabel(settings.distanceUnit).uppercase()})", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text("%.1f".format(formatDistValue(telemetry.tripDistanceKm, settings.distanceUnit)), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }

    // Right Battery & Voltage Dial with low-battery warning arc
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(146.dp)) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        drawArc(surfaceBg, 135f, 270f, false, style = Stroke(11f, cap = StrokeCap.Round))
        drawArc(Color(0x66FF4D6D), 135f, 54f, false, style = Stroke(11f, cap = StrokeCap.Butt))
        val bCol = if (telemetry.batteryPct <= 20) Color(0xFFFF4D6D) else secondary
        drawArc(bCol, 135f, 270f * batFrac, false, style = Stroke(11f, cap = StrokeCap.Round))
      }
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${telemetry.batteryPct}%", fontSize = 24.sp, fontWeight = FontWeight.Black, color = secondary)
        Text("${"%.1f".format(telemetry.estimatedVoltage)}V", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

@Composable
fun RetroSynthWedgeHud(
  dispSpeed: Double,
  unitLbl: String,
  progress: Float,
  telemetry: LiveTelemetrySnapshot,
  settings: MasterSettings,
  accent: Color,
  secondary: Color,
  surfaceBg: Color,
) {
  val chrome = LocalTacticalChrome.current
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .background(Color(0xE00D061A), chrome.cardShape)
        .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
      Row(verticalAlignment = Alignment.Bottom) {
        Text(
          text = "%.1f".format(dispSpeed),
          fontSize = 44.sp,
          lineHeight = 44.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFFFF4FD8),
        )
        Spacer(Modifier.width(6.dp))
        Text(unitLbl, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF), modifier = Modifier.padding(bottom = 6.dp))
      }
      Column(horizontalAlignment = Alignment.End) {
        Text("PACK ${telemetry.batteryPct}%", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF00F0FF))
        Text("${"%.1f".format(telemetry.estimatedVoltage)}V • ${settings.rideMode}", fontSize = 10.sp, color = Color(0xFFFFD84D))
      }
    }

    // 1980s rising staircase neon wedge speed bars over synth perspective grid
    Canvas(modifier = Modifier.fillMaxWidth().height(54.dp)) {
      drawLine(Color(0x66FF4FD8), Offset(0f, size.height - 2f), Offset(size.width, size.height - 2f), strokeWidth = 1.5f)
      drawLine(Color(0x3300F0FF), Offset(0f, size.height * 0.5f), Offset(size.width, size.height * 0.5f), strokeWidth = 1f)
      val bars = 20
      val gap = 4.dp.toPx()
      val barW = (size.width - gap * (bars - 1)) / bars
      for (i in 0 until bars) {
        val frac = (i + 1).toFloat() / bars
        val active = frac <= progress + 0.03f
        val barH = size.height * (0.28f + 0.72f * frac)
        val col =
          when {
            !active -> surfaceBg.copy(alpha = 0.25f)
            frac > 0.8f -> Color(0xFFFF2E63)
            frac > 0.45f -> Color(0xFFFF4FD8)
            else -> Color(0xFF00F0FF)
          }
        drawRoundRect(
          color = col,
          topLeft = Offset(i * (barW + gap), size.height - barH),
          size = Size(barW, barH),
          cornerRadius = CornerRadius(2f, 2f),
        )
      }
    }
  }
}

/**
 * Authentic LeaperKim Veteran Sherman-S / Begode Master / InMotion V13 Top-Deck EUC Display
 * Includes live PWM % duty cycle + Safety Headroom bar, pack voltage & phase amps, board temp,
 * HARD/MED/SOFT pedal stiffness badge, and dual-axis carving lean & pitch horizon.
 */
@Composable
fun EucLeanCarvingHud(
  dispSpeed: Double,
  unitLbl: String,
  progress: Float,
  telemetry: LiveTelemetrySnapshot,
  settings: MasterSettings,
  accent: Color,
  secondary: Color,
  surfaceBg: Color,
) {
  val chrome = LocalTacticalChrome.current
  val pwmUsedPct = (progress * 88f + (telemetry.voltageSagV.toFloat() * 1.8f)).roundToInt().coerceIn(4, 98)
  val pwmSafetyPct = (100 - pwmUsedPct).coerceIn(2, 96)
  val pwmAlertColor =
    when {
      pwmUsedPct >= 80 -> Color(0xFFFF2E55)
      pwmUsedPct >= 70 -> Color(0xFFF59E0B)
      else -> accent
    }
  val boardTempC = (telemetry.ambientTempC?.toDouble() ?: 24.0) + (pwmUsedPct * 0.34)
  val boardTempLbl = formatTempLabel(boardTempC, settings.tempUnit)
  val pedalMode =
    when {
      settings.rideMode.equals("SPORT", ignoreCase = true) || settings.rideMode.equals("TURBO", ignoreCase = true) -> "HARD"
      settings.rideMode.equals("ECO", ignoreCase = true) -> "SOFT"
      else -> "MED"
    }

  Surface(
    color = Color(0xFF070B12),
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(2.dp, pwmAlertColor.copy(alpha = 0.65f)),
    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      // Top EUC Roll-Cage Header: Pack Volts + Sag | EUC Mode | Board Temp + Battery %
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          "${"%.1f".format(telemetry.estimatedVoltage)}V (-${"%.1f".format(telemetry.voltageSagV)}V)",
          fontSize = 10.sp,
          fontWeight = FontWeight.Black,
          color = Color(0xFFFBBF24),
        )
        Surface(
          color = pwmAlertColor.copy(alpha = 0.18f),
          shape = RoundedCornerShape(5.dp),
          border = BorderStroke(1.dp, pwmAlertColor),
        ) {
          Text(
            "PEDAL $pedalMode • ${if (pwmUsedPct >= 80) "TILTBACK!" else "FOC OK"}",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = pwmAlertColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          )
        }
        Text(
          "MOS $boardTempLbl • ${telemetry.batteryPct}%",
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = secondary,
        )
      }

      Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(116.dp)) {
        Canvas(modifier = Modifier.size(196.dp, 114.dp)) {
          val cx = size.width / 2f
          val cy = size.height * 0.88f
          val r = size.height * 0.76f
          drawArc(
            color = surfaceBg,
            startAngle = 205f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = Offset(cx - r, cy - r),
            size = Size(r * 2f, r * 2f),
            style = Stroke(12f, cap = StrokeCap.Round),
          )
          // ±15°, ±30°, ±45° lean scale ticks
          for (degOff in listOf(-45, -30, -15, 0, 15, 30, 45)) {
            val rad = Math.toRadians((270f + (degOff / 45f) * 65f).toDouble())
            drawLine(
              color = if (degOff == 0) accent else secondary.copy(alpha = 0.65f),
              start = Offset(cx + ((r - 9f) * cos(rad)).toFloat(), cy + ((r - 9f) * sin(rad)).toFloat()),
              end = Offset(cx + ((r + 8f) * cos(rad)).toFloat(), cy + ((r + 8f) * sin(rad)).toFloat()),
              strokeWidth = if (degOff == 0) 3f else 1.6f,
            )
          }
          val rollClamped = telemetry.rollDeg.coerceIn(-45f, 45f)
          val rollSweep = (rollClamped / 45f) * 65f
          drawArc(
            color = if (abs(rollClamped) > 32f) Color(0xFFFF4D6D) else secondary,
            startAngle = 270f,
            sweepAngle = rollSweep,
            useCenter = false,
            topLeft = Offset(cx - r, cy - r),
            size = Size(r * 2f, r * 2f),
            style = Stroke(12f, cap = StrokeCap.Round),
          )
          // Peak Left/Right lean markers
          listOf(-telemetry.peakLeanLeftDeg, telemetry.peakLeanRightDeg).forEach { pk ->
            if (abs(pk) > 3f) {
              val pkRad = Math.toRadians((270f + (pk.coerceIn(-45f, 45f) / 45f) * 65f).toDouble())
              drawCircle(
                color = Color(0xFFFFEA00),
                radius = 3.5f,
                center = Offset(cx + (r * cos(pkRad)).toFloat(), cy + (r * sin(pkRad)).toFloat()),
              )
            }
          }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 20.dp)) {
          Text("%.1f".format(dispSpeed), fontSize = 38.sp, lineHeight = 38.sp, fontWeight = FontWeight.Black, color = Color.White)
          Text(
            "$unitLbl • LEAN ${"%.0f".format(telemetry.rollDeg)}° • PITCH ${"%.0f".format(telemetry.pitchDeg)}°",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = secondary,
          )
        }
      }

      // Live EUC PWM % Load & Cutout Safety Headroom Bar
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          "PWM $pwmUsedPct%",
          fontSize = 10.sp,
          fontWeight = FontWeight.Black,
          color = pwmAlertColor,
        )
        Box(
          modifier =
            Modifier
              .weight(1f)
              .height(10.dp)
              .background(surfaceBg, chrome.smallShape),
        ) {
          Box(
            modifier =
              Modifier
                .fillMaxWidth((pwmUsedPct / 100f).coerceIn(0.04f, 1f))
                .fillMaxHeight()
                .background(pwmAlertColor, chrome.smallShape),
          )
        }
        Text(
          "SAFE $pwmSafetyPct%",
          fontSize = 10.sp,
          fontWeight = FontWeight.Black,
          color = if (pwmSafetyPct <= 20) Color(0xFFFF2E55) else accent,
        )
      }
    }
  }
}
