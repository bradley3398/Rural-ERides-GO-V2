package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.WbIncandescent
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LiveTelemetrySnapshot
import com.example.data.MasterSettings
import kotlin.math.roundToInt

private fun deriveGearLevel(rideMode: String, speedKmh: Double): Int =
  when {
    rideMode.equals("ECO", ignoreCase = true) -> if (speedKmh > 18.0) 2 else 1
    rideMode.equals("SPORT", ignoreCase = true) || rideMode.equals("TURBO", ignoreCase = true) ->
      if (speedKmh > 30.0) 5 else 4
    else -> if (speedKmh > 25.0) 3 else 2
  }

private fun estPowerWatts(telemetry: LiveTelemetrySnapshot): Int {
  if (telemetry.powerWatts > 10.0) return telemetry.powerWatts.roundToInt().coerceIn(0, 18000)
  val speedTerm = telemetry.speedKmh * 18.5
  val gradeBoost = telemetry.pitchDeg.coerceIn(0f, 25f) * 32.0
  return if (telemetry.speedKmh < 1.0) 0 else (speedTerm + gradeBoost).roundToInt().coerceIn(45, 8500)
}

private fun estControllerTempC(telemetry: LiveTelemetrySnapshot): Double {
  val ambient = telemetry.ambientTempC?.toDouble() ?: 22.0
  val loadRise = (estPowerWatts(telemetry) / 280.0).coerceIn(2.0, 42.0)
  return ambient + loadRise
}

// Segment bitmask: A(top)=1, B(topRight)=2, C(botRight)=4, D(bot)=8, E(botLeft)=16, F(topLeft)=32, G(mid)=64
private fun sevenSegMask(ch: Char): Int =
  when (ch) {
    '0' -> 1 or 2 or 4 or 8 or 16 or 32
    '1' -> 2 or 4
    '2' -> 1 or 2 or 64 or 16 or 8
    '3' -> 1 or 2 or 64 or 4 or 8
    '4' -> 32 or 64 or 2 or 4
    '5' -> 1 or 32 or 64 or 4 or 8
    '6' -> 1 or 32 or 64 or 4 or 8 or 16
    '7' -> 1 or 2 or 4
    '8' -> 1 or 2 or 4 or 8 or 16 or 32 or 64
    '9' -> 1 or 2 or 4 or 8 or 32 or 64
    '-' -> 64
    'E' -> 1 or 32 or 64 or 16 or 8
    'S' -> 1 or 32 or 64 or 4 or 8
    'P' -> 1 or 2 or 32 or 64 or 16
    else -> 0
  }

private fun DrawScope.drawBeveledHorizontalSeg(
  x: Float,
  y: Float,
  len: Float,
  thick: Float,
  slantX: Float,
  color: Color,
) {
  val half = thick * 0.5f
  val path = Path().apply {
    moveTo(x + slantX, y)
    lineTo(x + half + slantX, y - half)
    lineTo(x + len - half + slantX, y - half)
    lineTo(x + len + slantX, y)
    lineTo(x + len - half + slantX, y + half)
    lineTo(x + half + slantX, y + half)
    close()
  }
  drawPath(path, color)
}

private fun DrawScope.drawBeveledVerticalSeg(
  x: Float,
  y: Float,
  len: Float,
  thick: Float,
  slantTop: Float,
  slantBot: Float,
  color: Color,
) {
  val half = thick * 0.5f
  val path = Path().apply {
    moveTo(x + slantTop, y)
    lineTo(x + half + slantTop, y + half)
    lineTo(x + half + slantBot, y + len - half)
    lineTo(x + slantBot, y + len)
    lineTo(x - half + slantBot, y + len - half)
    lineTo(x - half + slantTop, y + half)
    close()
  }
  drawPath(path, color)
}

/**
 * True physical hardware 7-segment LCD / LED vector renderer with beveled hexagonal bars,
 * authentic 7° italic slant, unlit liquid-crystal / smoked-acrylic ghost segments, and optional LED glow.
 */
@Composable
private fun HardwareSevenSegmentRow(
  text: String,
  digitHeight: Dp,
  activeColor: Color,
  ghostColor: Color,
  ledGlow: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val clean = text.trim()
  val digitCount = clean.count { it != '.' }
  val hasDot = clean.contains('.')
  val digitW = digitHeight * 0.56f
  val gapW = digitHeight * 0.14f
  val dotW = if (hasDot) digitHeight * 0.18f else 0.dp
  val totalW = digitW * digitCount.coerceAtLeast(1) + gapW * (digitCount - 1).coerceAtLeast(0) + dotW + 4.dp

  Canvas(modifier = modifier.width(totalW).height(digitHeight)) {
    val h = size.height
    val dw = digitW.toPx()
    val gw = gapW.toPx()
    val thick = (h * 0.125f).coerceAtLeast(2.2f)
    val gap = (thick * 0.24f).coerceAtLeast(1.2f)
    val slant = h * 0.085f
    var cursorX = 2f

    for (ch in clean) {
      if (ch == '.') {
        val r = thick * 0.62f
        if (ledGlow) {
          drawCircle(activeColor.copy(alpha = 0.28f), radius = r * 2.1f, center = Offset(cursorX + r * 0.5f, h - r * 1.25f))
        }
        drawCircle(activeColor, radius = r, center = Offset(cursorX + r * 0.5f, h - r * 1.25f))
        cursorX += dotW.toPx()
        continue
      }

      val mask = sevenSegMask(ch)
      val x0 = cursorX + thick * 0.55f
      val segH = dw - thick * 1.15f
      val segV = (h - thick * 1.2f) * 0.5f
      val topY = thick * 0.55f
      val midY = h * 0.5f
      val botY = h - thick * 0.55f

      fun segCol(bit: Int): Color = if ((mask and bit) != 0) activeColor else ghostColor
      fun drawPass(colFn: (Int) -> Color, extraThick: Float = 0f) {
        val th = thick + extraThick
        // A - top
        drawBeveledHorizontalSeg(x0 + gap, topY, segH - gap * 2f, th, slant, colFn(1))
        // G - mid
        drawBeveledHorizontalSeg(x0 + gap, midY, segH - gap * 2f, th, slant * 0.5f, colFn(64))
        // D - bot
        drawBeveledHorizontalSeg(x0 + gap, botY, segH - gap * 2f, th, 0f, colFn(8))
        // F - top left
        drawBeveledVerticalSeg(x0, topY + gap, segV - gap * 2f, th, slant, slant * 0.5f, colFn(32))
        // B - top right
        drawBeveledVerticalSeg(x0 + segH, topY + gap, segV - gap * 2f, th, slant, slant * 0.5f, colFn(2))
        // E - bot left
        drawBeveledVerticalSeg(x0, midY + gap, segV - gap * 2f, th, slant * 0.5f, 0f, colFn(16))
        // C - bot right
        drawBeveledVerticalSeg(x0 + segH, midY + gap, segV - gap * 2f, th, slant * 0.5f, 0f, colFn(4))
      }

      if (ledGlow) {
        drawPass(
          colFn = { bit -> if ((mask and bit) != 0) activeColor.copy(alpha = 0.22f) else Color.Transparent },
          extraThick = thick * 0.72f,
        )
      }
      drawPass(colFn = ::segCol, extraThick = 0f)
      cursorX += dw + gw
    }
  }
}

@Composable
private fun BezelCornerScrew(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier.size(7.dp)) {
    drawCircle(Color(0xFF27303F))
    drawCircle(Color(0xFF475569), style = Stroke(width = 1.dp.toPx()))
    drawLine(Color(0xFF0F172A), Offset(size.width * 0.25f, size.height * 0.25f), Offset(size.width * 0.75f, size.height * 0.75f), strokeWidth = 1.1f)
  }
}

@Composable
private fun OemBatterySegments(
  batteryPct: Int,
  activeColor: Color,
  inactiveColor: Color,
  segments: Int = 5,
  vertical: Boolean = false,
) {
  val filled = ((batteryPct.coerceIn(0, 100) * segments + 50) / 100).coerceIn(0, segments)
  if (vertical) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(1.5.dp),
    ) {
      Box(Modifier.width(9.dp).height(2.5.dp).background(activeColor.copy(alpha = 0.9f), RoundedCornerShape(1.dp)))
      Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .border(1.4.dp, activeColor.copy(alpha = 0.85f), RoundedCornerShape(3.dp))
          .padding(3.dp),
      ) {
        for (i in segments downTo 1) {
          val segCol =
            when {
              i > filled -> inactiveColor
              i == 1 && filled == 1 -> Color(0xFFEF4444)
              else -> activeColor
            }
          Box(
            modifier =
              Modifier
                .width(18.dp)
                .height(6.dp)
                .background(segCol, RoundedCornerShape(1.dp)),
          )
        }
      }
    }
  } else {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
      Row(
        modifier =
          Modifier
            .border(1.4.dp, activeColor.copy(alpha = 0.88f), RoundedCornerShape(2.5.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        for (i in 1..segments) {
          val segCol =
            when {
              i > filled -> inactiveColor
              i == 1 && filled == 1 -> Color(0xFFEF4444)
              else -> activeColor
            }
          Box(
            modifier =
              Modifier
                .width(if (segments > 6) 5.dp else 7.dp)
                .height(10.dp)
                .background(segCol, RoundedCornerShape(1.dp)),
          )
        }
      }
      Box(Modifier.width(3.dp).height(6.dp).background(activeColor.copy(alpha = 0.88f), RoundedCornerShape(1.dp)))
    }
  }
}

@Composable
private fun OemBezelButton(
  label: String,
  accent: Color = Color(0xFF64748B),
  modifier: Modifier = Modifier,
) {
  Surface(
    color = Color(0xFF181C24),
    shape = RoundedCornerShape(5.dp),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
    modifier = modifier,
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
      Text(
        text = label,
        fontFamily = FontFamily.Monospace,
        fontSize = 8.sp,
        fontWeight = FontWeight.Black,
        color = Color(0xFFCBD5E1),
        maxLines = 1,
      )
    }
  }
}

/**
 * 1. Authentic S866 Reflective Monochrome Olive-Gray STN E-Bike Handlebar LCD
 * True physical reflective liquid-crystal substrate (#A0B49B -> #859A82) with jet-black LCD ink segments,
 * faint unlit 88.8 crystal ghosting, LCD segment divider hairlines, and left 3-button rubber switch pod.
 */
@Composable
fun S866LcdHud(
  dispSpeed: Double,
  unitLbl: String,
  telemetry: LiveTelemetrySnapshot,
  settings: MasterSettings,
) {
  val lcdGlassTop = Color(0xFFA7B9A2)
  val lcdGlassBottom = Color(0xFF879B84)
  val lcdInk = Color(0xFF101914)
  val lcdGhost = Color(0xFF101914).copy(alpha = 0.10f)
  val pas = deriveGearLevel(settings.rideMode, telemetry.speedKmh)
  val watts = estPowerWatts(telemetry)
  val tripDisp = formatDistValue(telemetry.tripDistanceKm, settings.distanceUnit)
  val distLbl = distUnitLabel(settings.distanceUnit).uppercase()

  Row(
    modifier = Modifier.fillMaxWidth().height(206.dp).padding(horizontal = 4.dp).testTag("hud_s866_lcd"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    // Left Handlebar 3-Button Molded Rubber Remote Pod (+ / M / -)
    Surface(
      color = Color(0xFF14171D),
      shape = RoundedCornerShape(10.dp),
      border = BorderStroke(1.5.dp, Color(0xFF2D3644)),
      modifier = Modifier.width(34.dp).height(146.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(4.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        OemBezelButton("+", Color(0xFF64748B), Modifier.fillMaxWidth())
        OemBezelButton("M", Color(0xFF94A3B8), Modifier.fillMaxWidth())
        OemBezelButton("-", Color(0xFF64748B), Modifier.fillMaxWidth())
      }
    }

    // Main S866 Matte Black ABS Bezel + Reflective Monochrome Liquid-Crystal Glass
    Surface(
      color = Color(0xFF13161C),
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(2.5.dp, Color(0xFF2E3745)),
      modifier = Modifier.weight(1f).fillMaxHeight(),
    ) {
      Box(Modifier.fillMaxSize().padding(8.dp)) {
        Column(
          modifier =
            Modifier
              .fillMaxSize()
              .clip(RoundedCornerShape(8.dp))
              .background(Brush.verticalGradient(listOf(lcdGlassTop, lcdGlassBottom)))
              .border(2.dp, Color(0xFF475547), RoundedCornerShape(8.dp))
              .drawBehind {
                // Polarizer glass highlight + subtle pixel matrix scan
                val glare = Path().apply {
                  moveTo(0f, 0f)
                  lineTo(size.width * 0.45f, 0f)
                  lineTo(0f, size.height * 0.55f)
                  close()
                }
                drawPath(glare, Color.White.copy(alpha = 0.14f))
              }
              .padding(horizontal = 10.dp, vertical = 7.dp),
          verticalArrangement = Arrangement.SpaceBetween,
        ) {
          // Top LCD Status Band: 5-Bar Battery Icon | Voltage | Headlight + Model Stamp
          Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
              OemBatterySegments(telemetry.batteryPct, lcdInk, lcdGhost, segments = 5)
              Text(
                "${telemetry.batteryPct}%",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                color = lcdInk,
              )
            }
            Text(
              "VOL ${"%.1f".format(telemetry.estimatedVoltage)}V",
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Black,
              fontSize = 11.5.sp,
              color = lcdInk,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Icon(Icons.Default.WbIncandescent, contentDescription = null, tint = lcdInk, modifier = Modifier.size(13.dp))
              Text("S866", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 9.5.sp, color = lcdInk)
            }
          }

          HorizontalDivider(color = lcdInk.copy(alpha = 0.30f), thickness = 1.dp)

          // Center Monochrome 7-Segment Speed & Boxed PAS Level
          Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .border(1.4.dp, lcdInk.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
              Text("PAS", fontFamily = FontFamily.Monospace, fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = lcdInk)
              Spacer(Modifier.height(2.dp))
              HardwareSevenSegmentRow(
                text = "$pas",
                digitHeight = 28.dp,
                activeColor = lcdInk,
                ghostColor = lcdGhost,
              )
            }

            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
              HardwareSevenSegmentRow(
                text = "%4.1f".format(dispSpeed),
                digitHeight = 52.dp,
                activeColor = lcdInk,
                ghostColor = lcdGhost,
              )
              Column(modifier = Modifier.padding(bottom = 4.dp)) {
                Text("CUR", fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = lcdInk.copy(alpha = 0.72f))
                Text(unitLbl, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 11.sp, color = lcdInk)
              }
            }

            Column(horizontalAlignment = Alignment.End) {
              Text("${watts}W", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = lcdInk)
              Text(
                "TM %02d:%02d".format(telemetry.elapsedSec / 60, telemetry.elapsedSec % 60),
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = lcdInk.copy(alpha = 0.85f),
              )
            }
          }

          HorizontalDivider(color = lcdInk.copy(alpha = 0.30f), thickness = 1.dp)

          // Bottom LCD Register Strip (TRIP | MAX | AVG)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              "TRIP ${"%.2f".format(tripDisp)} $distLbl",
              fontFamily = FontFamily.Monospace,
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Black,
              color = lcdInk,
            )
            Text(
              "MAX ${"%.1f".format(formatSpeedValue(telemetry.maxSpeedKmh, settings.distanceUnit))} $unitLbl",
              fontFamily = FontFamily.Monospace,
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Black,
              color = lcdInk,
            )
            Text(
              "AVG ${"%.1f".format(formatSpeedValue(telemetry.avgSpeedKmh, settings.distanceUnit))}",
              fontFamily = FontFamily.Monospace,
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Black,
              color = lcdInk,
            )
          }
        }
      }
    }
  }
}

/**
 * 2. Authentic QS-S4 / TF-100 Round Finger-Throttle Scooter LCD
 * Circular matte-black CNC throttle bezel, right-hand index-finger trigger lever,
 * perimeter segmented bar, crisp hardware 7-segment speed numerals, and physical PWR / MODE buttons.
 */
@Composable
fun Tf11LcdHud(
  dispSpeed: Double,
  unitLbl: String,
  progress: Float,
  telemetry: LiveTelemetrySnapshot,
  settings: MasterSettings,
) {
  val gear = ((deriveGearLevel(settings.rideMode, telemetry.speedKmh) + 1) / 2).coerceIn(1, 3)
  val cyanGlow = Color(0xFF42F2FF)
  val amberGlow = Color(0xFFFFB020)
  val redGlow = Color(0xFFFF3B5C)
  val watts = estPowerWatts(telemetry)

  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier.size(216.dp).testTag("hud_tf11_lcd"),
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = size.width / 2f
      val cy = size.height / 2f
      val outerR = size.minDimension * 0.46f

      // Right-hand index-finger trigger throttle paddle silhouette
      val triggerPath = Path().apply {
        moveTo(cx + outerR * 0.72f, cy - outerR * 0.14f)
        lineTo(cx + outerR * 1.07f, cy + outerR * 0.28f)
        lineTo(cx + outerR * 0.90f, cy + outerR * 0.58f)
        lineTo(cx + outerR * 0.50f, cy + outerR * 0.34f)
        close()
      }
      drawPath(triggerPath, Color(0xFF1F2633))
      drawPath(triggerPath, Color(0xFF3E4A5E), style = Stroke(1.5.dp.toPx()))

      // Molded ABS outer bezel & recessed circular LCD lens
      drawCircle(color = Color(0xFF131720), radius = outerR, center = Offset(cx, cy))
      drawCircle(color = Color(0xFF394456), radius = outerR, center = Offset(cx, cy), style = Stroke(4.5.dp.toPx()))
      drawCircle(color = Color(0xFF06111F), radius = outerR - 8.dp.toPx(), center = Offset(cx, cy))

      // Segmented perimeter tachometer blocks (real QS-S4 LCD)
      val arcR = outerR - 15.dp.toPx()
      val totalBlocks = 22
      for (i in 0 until totalBlocks) {
        val frac = i.toFloat() / totalBlocks
        val angle = 140f + frac * 260f
        val lit = frac <= progress
        val segColor =
          when {
            !lit -> Color(0xFF10253A)
            frac > 0.78f -> redGlow
            frac > 0.48f -> amberGlow
            else -> cyanGlow
          }
        drawArc(
          color = segColor,
          startAngle = angle,
          sweepAngle = 8.5f,
          useCenter = false,
          topLeft = Offset(cx - arcR, cy - arcR),
          size = Size(arcR * 2f, arcR * 2f),
          style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Butt),
        )
      }
    }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(3.dp),
      modifier = Modifier.padding(20.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("QS-S4", fontFamily = FontFamily.Monospace, fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = cyanGlow.copy(alpha = 0.8f))
        Surface(
          color = amberGlow.copy(alpha = 0.16f),
          shape = RoundedCornerShape(4.dp),
          border = BorderStroke(1.dp, amberGlow),
        ) {
          Text(
            "GEAR $gear",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = amberGlow,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
          )
        }
      }

      HardwareSevenSegmentRow(
        text = "%4.1f".format(dispSpeed),
        digitHeight = 38.dp,
        activeColor = Color(0xFFEAFBFF),
        ghostColor = Color(0xFF153247),
        ledGlow = true,
      )

      Text(
        "$unitLbl • ${watts}W",
        fontFamily = FontFamily.Monospace,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        color = cyanGlow,
      )

      OemBatterySegments(telemetry.batteryPct, cyanGlow, Color(0xFF132D3E), segments = 6)

      Text(
        text = "${"%.1f".format(telemetry.estimatedVoltage)}V • ${"%.1f".format(formatDistValue(telemetry.tripDistanceKm, settings.distanceUnit))}${distUnitLabel(settings.distanceUnit).uppercase()}",
        fontFamily = FontFamily.Monospace,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = amberGlow,
      )

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 1.dp)) {
        OemBezelButton("⏻ PWR", cyanGlow)
        OemBezelButton("MODE", amberGlow)
      }
    }
  }
}

/**
 * 3. Authentic Bafang DPC-18 Full-Color IPS TFT E-Bike Cockpit Display
 * Features the unmistakable DPC-18 top sweeping speed arc gauge, bright IPS speed numerals nestled inside,
 * SPORT/ECO pill, live WATT meter, pack voltage, and crisp matte-black IPS data tiles.
 */
@Composable
fun BafangDpc18Hud(
  dispSpeed: Double,
  unitLbl: String,
  progress: Float,
  telemetry: LiveTelemetrySnapshot,
  settings: MasterSettings,
) {
  val pas = deriveGearLevel(settings.rideMode, telemetry.speedKmh)
  val isSport = settings.rideMode.equals("SPORT", ignoreCase = true) || settings.rideMode.equals("TURBO", ignoreCase = true)
  val modeCol = if (isSport) Color(0xFFFF5722) else Color(0xFF10B981)
  val watts = estPowerWatts(telemetry)
  val amps = if (telemetry.estimatedVoltage > 10.0) watts / telemetry.estimatedVoltage else 0.0
  val wattFrac = (watts / 2500f).coerceIn(0f, 1f)
  val tempLbl = formatTempLabel(estControllerTempC(telemetry), settings.tempUnit)
  val distLbl = distUnitLabel(settings.distanceUnit).uppercase()

  Surface(
    color = Color(0xFF12161E),
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(3.dp, Color(0xFF334155)),
    modifier = Modifier.fillMaxWidth().height(216.dp).padding(horizontal = 6.dp).testTag("hud_bafang_dpc18"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(5.dp)
        .clip(RoundedCornerShape(13.dp))
        .background(Color(0xFF06090F))
        .padding(horizontal = 11.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top IPS Status Strip: Mode | Clock/Model/Temp | Battery %
      Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
          Surface(color = modeCol, shape = RoundedCornerShape(4.dp)) {
            Text(
              text = settings.rideMode.uppercase().take(5),
              fontSize = 9.sp,
              fontWeight = FontWeight.Black,
              color = Color.Black,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp),
            )
          }
          Icon(Icons.Default.WbIncandescent, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
        }

        Text(
          "BAFANG DPC-18 • $tempLbl",
          fontFamily = FontFamily.Monospace,
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF94A3B8),
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          OemBatterySegments(telemetry.batteryPct, Color(0xFF22C55E), Color(0xFF1E293B), segments = 5)
          Text("${telemetry.batteryPct}%", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
      }

      // Signature DPC-18 Sweeping Top Speed Arc + Crisp Center Speed + Left Assist / Right Watt Pods
      Row(
        Modifier.fillMaxWidth().weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .width(66.dp)
            .background(Color(0xFF0E1522), RoundedCornerShape(8.dp))
            .border(1.dp, modeCol.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp),
        ) {
          Text("PAS", fontSize = 8.5.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
          HardwareSevenSegmentRow(
            text = "$pas",
            digitHeight = 28.dp,
            activeColor = modeCol,
            ghostColor = modeCol.copy(alpha = 0.14f),
            ledGlow = true,
          )
          Text("${"%.0f".format(amps)}A", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
        }

        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 4.dp),
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val arcDiameter = minOf(size.width * 0.92f, size.height * 1.48f)
            val topLeft = Offset((size.width - arcDiameter) * 0.5f, size.height * 0.10f)
            val arcSize = Size(arcDiameter, arcDiameter)
            drawArc(
              color = Color(0xFF1E293B),
              startAngle = 195f,
              sweepAngle = 150f,
              useCenter = false,
              topLeft = topLeft,
              size = arcSize,
              style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
              brush = Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF38BDF8), Color(0xFFF59E0B), Color(0xFFEF4444))),
              startAngle = 195f,
              sweepAngle = 150f * progress.coerceIn(0.03f, 1f),
              useCenter = false,
              topLeft = topLeft,
              size = arcSize,
              style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
            )
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 18.dp)) {
            HardwareSevenSegmentRow(
              text = "%4.1f".format(dispSpeed),
              digitHeight = 40.dp,
              activeColor = Color.White,
              ghostColor = Color(0xFF1A2232),
              ledGlow = true,
            )
            Text(unitLbl, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF38BDF8))
          }
        }

        Column(
          horizontalAlignment = Alignment.End,
          modifier = Modifier
            .width(74.dp)
            .background(Color(0xFF0E1522), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF283548), RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 6.dp),
        ) {
          Text("POWER", fontSize = 8.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
          Text("${watts}W", fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFFBBF24))
          Text("${"%.1f".format(telemetry.estimatedVoltage)}V", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
          Text("SAG -${"%.1f".format(telemetry.voltageSagV)}", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color(0xFF94A3B8))
        }
      }

      // Bottom IPS Watt Bar + Crisp Trip Computer Grid
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
          modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF1A2233)),
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth((wattFrac.coerceAtLeast(progress)).coerceIn(0.04f, 1f))
              .fillMaxHeight()
              .background(Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF38BDF8), Color(0xFFF59E0B), Color(0xFFEF4444)))),
          )
        }
        Row(
          Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1623), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("TRIP ${"%.2f".format(formatDistValue(telemetry.tripDistanceKm, settings.distanceUnit))}$distLbl", fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
          Text("MAX ${"%.1f".format(formatSpeedValue(telemetry.maxSpeedKmh, settings.distanceUnit))} $unitLbl", fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
          Text("TM %02d:%02d".format(telemetry.elapsedSec / 60, telemetry.elapsedSec % 60), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFF94A3B8))
        }
      }
    }
  }
}

/**
 * 4. Authentic Nami Burn-E / Kaabo Wolf Matrix IPS TFT Hyper-Scooter Cockpit
 * Waterproof CNC bezel with corner screws, DUAL AWD/SINGLE badge, dual F-ESC/R-ESC watt readouts,
 * glowing 7-segment speed numerals, and 20-bar color power curve.
 */
@Composable
fun IsinwheelTftHud(
  dispSpeed: Double,
  unitLbl: String,
  progress: Float,
  telemetry: LiveTelemetrySnapshot,
  settings: MasterSettings,
) {
  val modeLbl =
    when {
      settings.rideMode.equals("ECO", ignoreCase = true) -> "E"
      settings.rideMode.equals("SPORT", ignoreCase = true) || settings.rideMode.equals("TURBO", ignoreCase = true) -> "S"
      else -> "D"
    }
  val modeTint = if (modeLbl == "S") Color(0xFFFF2E55) else if (modeLbl == "D") Color(0xFF00E5FF) else Color(0xFF22C55E)
  val watts = estPowerWatts(telemetry)
  val tempLbl = formatTempLabel(estControllerTempC(telemetry), settings.tempUnit)
  val distLbl = distUnitLabel(settings.distanceUnit).uppercase()

  Surface(
    color = Color(0xFF10131A),
    shape = RoundedCornerShape(20.dp),
    border = BorderStroke(2.5.dp, Color(0xFF3B475C)),
    modifier = Modifier.fillMaxWidth().height(212.dp).padding(horizontal = 6.dp).testTag("hud_isinwheel_tft"),
  ) {
    Box(Modifier.fillMaxSize().padding(6.dp)) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .clip(RoundedCornerShape(14.dp))
          .background(Color(0xFF05080F))
          .border(1.2.dp, modeTint.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
          .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(color = modeTint, shape = CircleShape, modifier = Modifier.size(24.dp)) {
              Box(contentAlignment = Alignment.Center) {
                Text(modeLbl, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.Black)
              }
            }
            Surface(
              color = modeTint.copy(alpha = 0.16f),
              shape = RoundedCornerShape(4.dp),
              border = BorderStroke(1.dp, modeTint.copy(alpha = 0.65f)),
            ) {
              Text(
                if (modeLbl == "E") "SINGLE ECO" else "DUAL AWD",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                color = modeTint,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp),
              )
            }
            Icon(Icons.Default.WbIncandescent, contentDescription = "Headlight Indicator", tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
            Icon(Icons.Default.Bluetooth, contentDescription = "Telemetry Active", tint = Color(0xFF60A5FA), modifier = Modifier.size(14.dp))
          }

          Text("MATRIX TFT • $tempLbl", fontWeight = FontWeight.Black, fontSize = 9.sp, color = Color(0xFF94A3B8))

          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OemBatterySegments(telemetry.batteryPct, Color(0xFF22C55E), Color(0xFF1E293B), segments = 5)
            Text("${telemetry.batteryPct}%", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color(0xFF22C55E))
          }
        }

        Row(
          Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(horizontalAlignment = Alignment.Start) {
            Text("F-ESC", fontFamily = FontFamily.Monospace, fontSize = 8.5.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
            Text("${watts / 2}W", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Black)
            Text("SINEWAVE", fontFamily = FontFamily.Monospace, fontSize = 7.5.sp, color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
          }

          Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            HardwareSevenSegmentRow(
              text = "%2.0f".format(dispSpeed).padStart(2, '0'),
              digitHeight = 56.dp,
              activeColor = Color.White,
              ghostColor = Color(0xFF162032),
              ledGlow = true,
            )
            Text(
              unitLbl,
              fontSize = 12.sp,
              fontWeight = FontWeight.Black,
              color = modeTint,
              modifier = Modifier.padding(bottom = 6.dp),
            )
          }

          Column(horizontalAlignment = Alignment.End) {
            Text("R-ESC", fontFamily = FontFamily.Monospace, fontSize = 8.5.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
            Text("${(watts + 1) / 2}W", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = modeTint, fontWeight = FontWeight.Black)
            Text("MAX ${"%.0f".format(formatSpeedValue(telemetry.maxSpeedKmh, settings.distanceUnit))}", fontFamily = FontFamily.Monospace, fontSize = 8.5.sp, color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold)
          }
        }

        Canvas(modifier = Modifier.fillMaxWidth().height(13.dp)) {
          val segs = 20
          val gap = 4.5f
          val segW = (size.width - gap * (segs - 1)) / segs
          for (i in 0 until segs) {
            val f = i.toFloat() / segs
            val col =
              when {
                f >= progress.coerceAtLeast(0.05f) -> Color(0xFF182233)
                f < 0.35f -> Color(0xFF00E5FF)
                f < 0.70f -> Color(0xFF22C55E)
                else -> Color(0xFFFF2E55)
              }
            drawRoundRect(
              color = col,
              topLeft = Offset(i * (segW + gap), 0f),
              size = Size(segW, size.height),
              cornerRadius = CornerRadius(3.5f, 3.5f),
            )
          }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(
            "TRIP ${"%.2f".format(formatDistValue(telemetry.tripDistanceKm, settings.distanceUnit))} $distLbl",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.5.sp,
            color = Color(0xFFCBD5E1),
            fontWeight = FontWeight.Bold,
          )
          Text(
            "${"%.1f".format(telemetry.estimatedVoltage)}V (SAG -${"%.1f".format(telemetry.voltageSagV)}V)",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.5.sp,
            color = Color(0xFFFBBF24),
            fontWeight = FontWeight.Bold,
          )
          Text(
            "TM %02d:%02d".format(telemetry.elapsedSec / 60, telemetry.elapsedSec % 60),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.5.sp,
            color = Color(0xFF94A3B8),
            fontWeight = FontWeight.Bold,
          )
        }
      }
      BezelCornerScrew(Modifier.align(Alignment.TopStart).padding(2.dp))
      BezelCornerScrew(Modifier.align(Alignment.TopEnd).padding(2.dp))
      BezelCornerScrew(Modifier.align(Alignment.BottomStart).padding(2.dp))
      BezelCornerScrew(Modifier.align(Alignment.BottomEnd).padding(2.dp))
    }
  }
}

/**
 * 5. Authentic Segway Ninebot Max G30 / G2 Flush Smoked-Acrylic Stem LED Matrix
 * Glossy black smoked-acrylic vertical pill stem lens with pure glowing white 7-segment LED speed digits,
 * illuminated Bluetooth/headlight/mode LEDs, signature 5-bar horizontal LED battery pylon, and flush power button.
 */
@Composable
fun NinebotMatrixHud(
  dispSpeed: Double,
  unitLbl: String,
  telemetry: LiveTelemetrySnapshot,
  settings: MasterSettings,
) {
  val mode =
    when {
      settings.rideMode.equals("ECO", ignoreCase = true) -> "ECO"
      settings.rideMode.equals("SPORT", ignoreCase = true) || settings.rideMode.equals("TURBO", ignoreCase = true) -> "S"
      else -> "D"
    }
  val modeColor = if (mode == "S") Color(0xFFFF3B30) else if (mode == "D") Color(0xFFFFFFFF) else Color(0xFF22C55E)
  val filledBars = ((telemetry.batteryPct.coerceIn(0, 100) * 5 + 50) / 100).coerceIn(0, 5)
  val distLbl = distUnitLabel(settings.distanceUnit).uppercase()

  Surface(
    color = Color(0xFF06070A),
    shape = RoundedCornerShape(42.dp),
    border = BorderStroke(3.dp, Color(0xFF272A30)),
    modifier = Modifier.width(236.dp).height(216.dp).testTag("hud_ninebot_matrix"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(Brush.verticalGradient(listOf(Color(0xFF10131A), Color(0xFF040507), Color(0xFF0A0C10))))
        .drawBehind {
          // Subtle smoked acrylic glossy reflection band
          drawRect(
            brush = Brush.linearGradient(
              listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
              start = Offset(0f, 0f),
              end = Offset(size.width * 0.6f, size.height * 0.4f),
            )
          )
        }
        .padding(horizontal = 18.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top Smoked-Lens LED Status Icons (Bluetooth | Headlight | ECO/D/S)
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Bluetooth, contentDescription = "Connected", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
        Icon(Icons.Default.WbIncandescent, contentDescription = "Headlight", tint = Color(0xFF22C55E), modifier = Modifier.size(16.dp))
        Surface(
          color = if (mode == "S") Color(0xFFFF3B30) else Color.Transparent,
          shape = RoundedCornerShape(4.dp),
          border = BorderStroke(1.2.dp, modeColor),
        ) {
          Text(
            text = mode,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = if (mode == "S") Color.White else modeColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
          )
        }
      }

      // Giant Glowing White LED 7-Segment Speed Digits
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HardwareSevenSegmentRow(
          text = "%.0f".format(dispSpeed).padStart(2, '0'),
          digitHeight = 64.dp,
          activeColor = Color.White,
          ghostColor = Color(0xFF171A21),
          ledGlow = true,
        )
        Spacer(Modifier.height(2.dp))
        Text(
          "$unitLbl • ${"%.1f".format(formatDistValue(telemetry.tripDistanceKm, settings.distanceUnit))} $distLbl",
          fontFamily = FontFamily.Monospace,
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF94A3B8),
        )
      }

      // Signature Ninebot 5-Bar LED Battery Strip + Flush Circular Power Button
      Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          for (i in 1..5) {
            val active = i <= filledBars
            val barCol =
              when {
                !active -> Color(0xFF1D2028)
                i == 1 && filledBars == 1 -> Color(0xFFFF3B30)
                else -> Color.White
              }
            Box(
              Modifier
                .width(24.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(barCol),
            )
          }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            "${telemetry.batteryPct}% • ${"%.1f".format(telemetry.estimatedVoltage)}V",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = Color(0xFF94A3B8),
          )
          Surface(
            color = Color(0xFF161920),
            shape = CircleShape,
            border = BorderStroke(1.2.dp, Color(0xFF3F4654)),
            modifier = Modifier.size(22.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text("⏻", fontSize = 10.sp, color = Color(0xFFCBD5E1))
            }
          }
        }
      }
    }
  }
}

/**
 * 6. Authentic SW900 / KT-LCD3 Wide Backlit Cobalt-Blue Monochrome LCD Display
 * Signature cobalt-blue STN backlit glass, 4 corner bezel screws, vertical 5-bar battery tower,
 * boxed PAS 1-5 level, and crisp white-cyan 7-segment speed numerals.
 */
@Composable
fun Sw900LcdHud(
  dispSpeed: Double,
  unitLbl: String,
  telemetry: LiveTelemetrySnapshot,
  settings: MasterSettings,
) {
  val lcdTop = Color(0xFF174994)
  val lcdBot = Color(0xFF0C2B5E)
  val lcdFg = Color(0xFFE6FAFF)
  val lcdGhost = Color(0xFF285EA6)
  val pas = deriveGearLevel(settings.rideMode, telemetry.speedKmh)
  val watts = estPowerWatts(telemetry)
  val tempLbl = formatTempLabel(estControllerTempC(telemetry), settings.tempUnit)
  val distLbl = distUnitLabel(settings.distanceUnit).uppercase()

  Surface(
    color = Color(0xFF12161D),
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(2.5.dp, Color(0xFF3C4A5C)),
    modifier = Modifier.fillMaxWidth().height(206.dp).padding(horizontal = 6.dp).testTag("hud_sw900_lcd"),
  ) {
    Box(Modifier.fillMaxSize().padding(8.dp)) {
      Row(
        modifier =
          Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.verticalGradient(listOf(lcdTop, lcdBot)))
            .border(1.5.dp, Color(0xFF60A5FA).copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .padding(11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Left Column: Vertical Battery Tower + Boxed PAS Frame
        Column(
          modifier = Modifier.fillMaxHeight(),
          verticalArrangement = Arrangement.SpaceBetween,
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          OemBatterySegments(telemetry.batteryPct, lcdFg, lcdGhost, segments = 5, vertical = true)
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .border(1.2.dp, lcdFg.copy(alpha = 0.70f), RoundedCornerShape(4.dp))
              .padding(horizontal = 8.dp, vertical = 3.dp),
          ) {
            Text("PAS", fontFamily = FontFamily.Monospace, fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = lcdFg)
            HardwareSevenSegmentRow(
              text = "$pas",
              digitHeight = 24.dp,
              activeColor = lcdFg,
              ghostColor = lcdGhost.copy(alpha = 0.55f),
            )
          }
        }

        // Right Column: Header Voltage + Giant 7-Segment Speed + Footer Registers
        Column(
          modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 12.dp),
          verticalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("SW900 • $tempLbl", fontFamily = FontFamily.Monospace, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = lcdFg.copy(alpha = 0.85f))
            Text(
              "VOLT ${"%.1f".format(telemetry.estimatedVoltage)}V (${telemetry.batteryPct}%)",
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp,
              fontWeight = FontWeight.Black,
              color = lcdFg,
            )
          }

          Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            HardwareSevenSegmentRow(
              text = "%4.1f".format(dispSpeed),
              digitHeight = 54.dp,
              activeColor = lcdFg,
              ghostColor = lcdGhost.copy(alpha = 0.45f),
            )
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.padding(bottom = 5.dp)) {
              Text("MAX ${"%.0f".format(formatSpeedValue(telemetry.maxSpeedKmh, settings.distanceUnit))}", fontFamily = FontFamily.Monospace, fontSize = 8.5.sp, color = lcdFg.copy(alpha = 0.8f))
              Text(unitLbl, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Black, color = lcdFg)
            }
          }

          Row(
            Modifier
              .fillMaxWidth()
              .border(1.dp, lcdFg.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
              .padding(horizontal = 7.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              "DST ${"%.2f".format(formatDistValue(telemetry.tripDistanceKm, settings.distanceUnit))} $distLbl",
              fontFamily = FontFamily.Monospace,
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
              color = lcdFg,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text("PWR ${watts}W", fontFamily = FontFamily.Monospace, fontSize = 9.5.sp, color = lcdFg, fontWeight = FontWeight.Black)
            Text("TM %02d:%02d".format(telemetry.elapsedSec / 60, telemetry.elapsedSec % 60), fontFamily = FontFamily.Monospace, fontSize = 9.5.sp, color = lcdFg)
          }
        }
      }
      BezelCornerScrew(Modifier.align(Alignment.TopStart).padding(1.dp))
      BezelCornerScrew(Modifier.align(Alignment.TopEnd).padding(1.dp))
      BezelCornerScrew(Modifier.align(Alignment.BottomStart).padding(1.dp))
      BezelCornerScrew(Modifier.align(Alignment.BottomEnd).padding(1.dp))
    }
  }
}

/**
 * 7. Authentic Minimotors Dualtron / VSETT EYE3 (EY3) Emerald-Green Backlit Monochrome LCD
 * True physical EYE3 appearance: luminous emerald-green backlit STN substrate (#6AE8A6 -> #44BF7F)
 * with razor-sharp dark jet-black liquid-crystal 7-segment numerals (#072013), 7-bar battery outline,
 * TIME / PAS / GEAR / TRIP / VOL registers, and molded rubber POWER & MODE buttons below the glass.
 */
@Composable
fun VsettEy3Hud(
  dispSpeed: Double,
  unitLbl: String,
  progress: Float,
  telemetry: LiveTelemetrySnapshot,
  settings: MasterSettings,
) {
  val eyGlassTop = Color(0xFF72EEAE)
  val eyGlassBottom = Color(0xFF42C482)
  val eyInk = Color(0xFF071F12)
  val eyGhost = Color(0xFF071F12).copy(alpha = 0.10f)
  val gear = ((deriveGearLevel(settings.rideMode, telemetry.speedKmh) + 1) / 2).coerceIn(1, 3)
  val distLbl = distUnitLabel(settings.distanceUnit).uppercase()
  val watts = estPowerWatts(telemetry)

  Surface(
    color = Color(0xFF141820),
    shape = RoundedCornerShape(24.dp),
    border = BorderStroke(2.5.dp, Color(0xFF334155)),
    modifier = Modifier.fillMaxWidth().height(210.dp).padding(horizontal = 8.dp).testTag("hud_vsett_ey3"),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(8.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Iconic Bright Green Backlit EYE3 Monochrome LCD Window with Black Crystal Segments
      Column(
        modifier =
          Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(eyGlassTop, eyGlassBottom)))
            .border(1.8.dp, Color(0xFF1D6E45), RoundedCornerShape(14.dp))
            .drawBehind {
              val glare = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width * 0.42f, 0f)
                lineTo(0f, size.height * 0.6f)
                close()
              }
              drawPath(glare, Color.White.copy(alpha = 0.16f))
            }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            "TIME %02d:%02d".format(telemetry.elapsedSec / 60, telemetry.elapsedSec % 60),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            fontSize = 10.sp,
            color = eyInk,
          )
          Text(
            "MINIMOTORS • EYE3",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            fontSize = 9.5.sp,
            color = eyInk.copy(alpha = 0.85f),
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier
              .border(1.3.dp, eyInk, RoundedCornerShape(4.dp))
              .padding(horizontal = 6.dp, vertical = 1.dp),
          ) {
            Icon(Icons.Default.Bolt, contentDescription = null, tint = eyInk, modifier = Modifier.size(12.dp))
            Text(
              "GEAR $gear",
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Black,
              fontSize = 10.sp,
              color = eyInk,
            )
          }
        }

        Row(
          Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
              "BAT ${telemetry.batteryPct}%",
              fontFamily = FontFamily.Monospace,
              fontSize = 13.sp,
              fontWeight = FontWeight.Black,
              color = eyInk,
            )
            OemBatterySegments(telemetry.batteryPct, eyInk, eyGhost, segments = 7)
            Text(
              "${"%.1f".format(telemetry.estimatedVoltage)}V • ${watts}W",
              fontFamily = FontFamily.Monospace,
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Black,
              color = eyInk,
            )
          }

          Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            HardwareSevenSegmentRow(
              text = "%4.1f".format(dispSpeed),
              digitHeight = 50.dp,
              activeColor = eyInk,
              ghostColor = eyGhost,
            )
            Text(
              unitLbl,
              fontFamily = FontFamily.Monospace,
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              color = eyInk,
              modifier = Modifier.padding(bottom = 5.dp),
            )
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Box(
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(2.dp)).background(eyGhost),
          ) {
            Box(
              modifier = Modifier.fillMaxWidth(progress.coerceIn(0.04f, 1f)).fillMaxHeight().background(eyInk),
            )
          }
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
              "TRIP ${"%.2f".format(formatDistValue(telemetry.tripDistanceKm, settings.distanceUnit))} $distLbl",
              fontFamily = FontFamily.Monospace,
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Black,
              color = eyInk,
            )
            Text(
              "MAX ${"%.1f".format(formatSpeedValue(telemetry.maxSpeedKmh, settings.distanceUnit))} $unitLbl",
              fontFamily = FontFamily.Monospace,
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Black,
              color = eyInk,
            )
            Text("E-ABS", fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Black, color = eyInk)
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 5.dp, start = 12.dp, end = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OemBezelButton("⏻ POWER", Color(0xFF4ADE80))
        Text(
          "EY3 SMART THROTTLE",
          fontFamily = FontFamily.Monospace,
          fontSize = 8.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF64748B),
        )
        OemBezelButton("• MODE", Color(0xFF4ADE80))
      }
    }
  }
}
