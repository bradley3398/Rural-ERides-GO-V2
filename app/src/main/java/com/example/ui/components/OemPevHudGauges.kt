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
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
  val speedTerm = telemetry.speedKmh * 18.5
  val gradeBoost = telemetry.pitchDeg.coerceIn(0f, 25f) * 32.0
  return if (telemetry.speedKmh < 1.0) 0 else (speedTerm + gradeBoost).roundToInt().coerceIn(45, 6500)
}

@Composable
private fun OemBatterySegments(
  batteryPct: Int,
  activeColor: Color,
  inactiveColor: Color,
  vertical: Boolean = false,
) {
  val filled = ((batteryPct.coerceIn(0, 100) + 19) / 20).coerceIn(0, 5)
  if (vertical) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      for (i in 5 downTo 1) {
        Box(
          modifier =
            Modifier
              .width(18.dp)
              .height(6.dp)
              .background(if (i <= filled) activeColor else inactiveColor, RoundedCornerShape(1.dp)),
        )
      }
    }
  } else {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
      Row(
        modifier =
          Modifier
            .border(1.2.dp, activeColor.copy(alpha = 0.85f), RoundedCornerShape(2.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        for (i in 1..5) {
          Box(
            modifier =
              Modifier
                .width(6.dp)
                .height(10.dp)
                .background(if (i <= filled) activeColor else inactiveColor, RoundedCornerShape(1.dp)),
          )
        }
      }
      Box(Modifier.width(2.dp).height(6.dp).background(activeColor.copy(alpha = 0.85f)))
    }
  }
}

/**
 * 1. S866 Backlit Monochrome E-Bike LCD
 */
@Composable
fun S866LcdHud(
  dispSpeed: Double,
  unitLbl: String,
  telemetry: LiveTelemetrySnapshot,
  settings: MasterSettings,
) {
  val lcdBg = Color(0xFF0A2D5C)
  val lcdInk = Color(0xFFD8F3FF)
  val lcdDim = Color(0xFF28558A)
  val pas = deriveGearLevel(settings.rideMode, telemetry.speedKmh)
  val watts = estPowerWatts(telemetry)
  val tripDisp = formatDistValue(telemetry.tripDistanceKm, settings.distanceUnit)
  val distLbl = distUnitLabel(settings.distanceUnit)

  Surface(
    color = Color(0xFF14181F),
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(2.dp, Color(0xFF334155)),
    modifier = Modifier.fillMaxWidth().height(198.dp).padding(horizontal = 6.dp).testTag("hud_s866_lcd"),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(8.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Brush.verticalGradient(listOf(Color(0xFF0D3A75), lcdBg)))
          .border(1.5.dp, Color(0xFF4BA3E3).copy(alpha = 0.55f), RoundedCornerShape(8.dp))
          .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          OemBatterySegments(telemetry.batteryPct, lcdInk, lcdDim)
          Text("${telemetry.batteryPct}%", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = lcdInk)
        }
        Text("VOL ${"%.1f".format(telemetry.estimatedVoltage)}V", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = lcdInk)
        Text("S866", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 10.sp, color = lcdInk.copy(alpha = 0.72f))
      }

      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.Start) {
          Text("PAS", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = lcdInk.copy(alpha = 0.78f))
          Text("$pas", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 32.sp, color = lcdInk)
          Text(settings.rideMode.uppercase().take(5), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = lcdInk.copy(alpha = 0.8f))
        }
        Row(verticalAlignment = Alignment.Bottom) {
          Text(
            text = "%.1f".format(dispSpeed),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            fontSize = 52.sp,
            color = lcdInk,
            maxLines = 1,
            softWrap = false,
          )
          Spacer(Modifier.width(4.dp))
          Text(unitLbl, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = lcdInk, modifier = Modifier.padding(bottom = 10.dp))
        }
        Column(horizontalAlignment = Alignment.End) {
          Text("PWR", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = lcdInk.copy(alpha = 0.75f))
          Text("${watts}W", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = lcdInk)
          Text("TIME %02d:%02d".format(telemetry.elapsedSec / 60, telemetry.elapsedSec % 60), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = lcdInk.copy(alpha = 0.8f))
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().border(1.dp, lcdInk.copy(alpha = 0.25f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text("TRIP ${"%.2f".format(tripDisp)}$distLbl", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = lcdInk)
        Text("MAX ${"%.1f".format(formatSpeedValue(telemetry.maxSpeedKmh, settings.distanceUnit))}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = lcdInk)
        Text("AVG ${"%.1f".format(formatSpeedValue(telemetry.avgSpeedKmh, settings.distanceUnit))}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = lcdInk)
      }
    }
  }
}

/**
 * 2. TF-100 / TF11 / QS-S4 Round E-Scooter Throttle LCD
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
  val cyanGlow = Color(0xFF47E5FF)
  val amberGlow = Color(0xFFFFB020)

  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier.size(202.dp).testTag("hud_tf11_lcd"),
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = size.width / 2f
      val cy = size.height / 2f
      val outerR = size.minDimension * 0.48f
      drawCircle(color = Color(0xFF151922), radius = outerR, center = Offset(cx, cy))
      drawCircle(color = Color(0xFF334155), radius = outerR, center = Offset(cx, cy), style = Stroke(4.dp.toPx()))
      drawCircle(color = Color(0xFF061624), radius = outerR - 8.dp.toPx(), center = Offset(cx, cy))
      val arcR = outerR - 14.dp.toPx()
      drawArc(
        color = Color(0xFF12324A),
        startAngle = 140f,
        sweepAngle = 260f,
        useCenter = false,
        topLeft = Offset(cx - arcR, cy - arcR),
        size = Size(arcR * 2f, arcR * 2f),
        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
      )
      drawArc(
        brush = Brush.sweepGradient(listOf(cyanGlow, amberGlow, Color(0xFFFF4D6D))),
        startAngle = 140f,
        sweepAngle = 260f * progress,
        useCenter = false,
        topLeft = Offset(cx - arcR, cy - arcR),
        size = Size(arcR * 2f, arcR * 2f),
        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
      )
    }
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(3.dp),
      modifier = Modifier.padding(18.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("TF-11", fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = cyanGlow.copy(alpha = 0.7f))
        Text("GEAR $gear", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Black, color = amberGlow)
      }
      Text(
        text = "%.1f".format(dispSpeed),
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Black,
        fontSize = 40.sp,
        color = Color.White,
        maxLines = 1,
        softWrap = false,
      )
      Text(unitLbl, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = cyanGlow)
      OemBatterySegments(telemetry.batteryPct, cyanGlow, Color(0xFF17374A))
      Text(
        text = "${"%.1f".format(telemetry.estimatedVoltage)}V • ${"%.1f".format(formatDistValue(telemetry.tripDistanceKm, settings.distanceUnit))}${distUnitLabel(settings.distanceUnit)}",
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = amberGlow,
      )
    }
  }
}

/**
 * 3. Bafang DPC18 Color Mid-Drive TFT Matrix
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
  val modeCol = if (isSport) Color(0xFFFF6B35) else Color(0xFF22C55E)
  val watts = estPowerWatts(telemetry)
  val wattFrac = (watts / 1500f).coerceIn(0f, 1f)

  Surface(
    color = Color(0xFF0C1018),
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(2.dp, Color(0xFF334155)),
    modifier = Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 8.dp).testTag("hud_bafang_dpc18"),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(12.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Surface(color = modeCol.copy(alpha = 0.22f), shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, modeCol)) {
          Text(
            text = if (isSport) "SPORT" else "ECO",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = modeCol,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
          )
        }
        Text("BAFANG DPC18", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          OemBatterySegments(telemetry.batteryPct, modeCol, Color(0xFF1E293B))
          Text("${telemetry.batteryPct}%", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
      }

      Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(68.dp)) {
          Text("ASSIST", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
          Text("$pas", fontSize = 30.sp, fontWeight = FontWeight.Black, color = modeCol)
          Text("${watts}W", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("%.1f".format(dispSpeed), fontSize = 46.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1)
          Text(unitLbl, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
        }

        Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(72.dp)) {
          Text("VOLTS", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
          Text("${"%.1f".format(telemetry.estimatedVoltage)}V", fontFamily = FontFamily.Monospace, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFFFBBF24))
          Text("TRIP ${"%.1f".format(formatDistValue(telemetry.tripDistanceKm, settings.distanceUnit))}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White)
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("OUTPUT POWER", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
          Text("PEAK ${"%.1f".format(formatSpeedValue(telemetry.maxSpeedKmh, settings.distanceUnit))} $unitLbl", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
        }
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .height(10.dp)
              .clip(RoundedCornerShape(5.dp))
              .background(Color(0xFF1E293B)),
        ) {
          Box(
            modifier =
              Modifier
                .fillMaxWidth((wattFrac.coerceAtLeast(progress)).coerceIn(0.04f, 1f))
                .fillMaxHeight()
                .background(Brush.horizontalGradient(listOf(Color(0xFF22C55E), Color(0xFFF59E0B), Color(0xFFEF4444)))),
          )
        }
      }
    }
  }
}

/**
 * 4. iSinwheel Colorful Sport TFT Cluster
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
      settings.rideMode.equals("ECO", ignoreCase = true) -> "ECO"
      settings.rideMode.equals("SPORT", ignoreCase = true) || settings.rideMode.equals("TURBO", ignoreCase = true) -> "S"
      else -> "D"
    }
  val modeTint = if (modeLbl == "S") Color(0xFFFF3B5C) else if (modeLbl == "D") Color(0xFF00E5FF) else Color(0xFF22C55E)

  Surface(
    color = Color(0xFF090D16),
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(2.dp, modeTint.copy(alpha = 0.65f)),
    modifier = Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 8.dp).testTag("hud_isinwheel_tft"),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(14.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Surface(color = modeTint, shape = CircleShape, modifier = Modifier.size(26.dp)) {
            Box(contentAlignment = Alignment.Center) {
              Text(modeLbl, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.Black)
            }
          }
          Icon(Icons.Default.WbIncandescent, contentDescription = "Headlight Indicator", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
          Icon(Icons.Default.Bluetooth, contentDescription = "Telemetry Active", tint = Color(0xFF60A5FA), modifier = Modifier.size(16.dp))
        }
        Text("iSINWHEEL TFT", fontWeight = FontWeight.Black, fontSize = 10.sp, color = Color(0xFF94A3B8))
        Text("${telemetry.batteryPct}%", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFF22C55E))
      }

      Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
        Text(
          text = "%.0f".format(dispSpeed),
          fontSize = 58.sp,
          fontWeight = FontWeight.Black,
          color = Color.White,
          maxLines = 1,
        )
        Spacer(Modifier.width(6.dp))
        Text(unitLbl, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = modeTint, modifier = Modifier.padding(bottom = 12.dp))
      }

      // Signature rainbow curved-feeling power/speed strip
      Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
        val segs = 18
        val gap = 5f
        val segW = (size.width - gap * (segs - 1)) / segs
        for (i in 0 until segs) {
          val f = i.toFloat() / segs
          val col =
            when {
              f >= progress -> Color(0xFF1E293B)
              f < 0.35f -> Color(0xFF00E5FF)
              f < 0.7f -> Color(0xFF22C55E)
              else -> Color(0xFFFF3B5C)
            }
          drawRoundRect(
            color = col,
            topLeft = Offset(i * (segW + gap), 0f),
            size = Size(segW, size.height),
            cornerRadius = CornerRadius(4f, 4f),
          )
        }
      }

      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("TRIP ${"%.2f".format(formatDistValue(telemetry.tripDistanceKm, settings.distanceUnit))} ${distUnitLabel(settings.distanceUnit)}", fontSize = 10.sp, color = Color(0xFFCBD5E1), fontWeight = FontWeight.Bold)
        Text("${"%.1f".format(telemetry.estimatedVoltage)}V PACK", fontSize = 10.sp, color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold)
      }
    }
  }
}

/**
 * 5. Segway Ninebot Monolithic LED Dot-Matrix Bezel
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
  val modeColor = if (mode == "S") Color(0xFFFF3B30) else Color.White

  Surface(
    color = Color(0xFF040507),
    shape = RoundedCornerShape(28.dp),
    border = BorderStroke(2.dp, Color(0xFF27272A)),
    modifier = Modifier.width(236.dp).height(202.dp).testTag("hud_ninebot_matrix"),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Bluetooth, contentDescription = "Connected", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
        Text("NINEBOT", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF71717A))
        Surface(color = modeColor.copy(alpha = 0.2f), shape = CircleShape, border = BorderStroke(1.dp, modeColor)) {
          Text(
            text = mode,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = modeColor,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
          )
        }
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "%.0f".format(dispSpeed).padStart(2, '0'),
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Black,
          fontSize = 64.sp,
          color = Color.White,
          maxLines = 1,
        )
        Text(unitLbl, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA1A1AA))
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        OemBatterySegments(telemetry.batteryPct, if (telemetry.batteryPct < 20) Color(0xFFFF3B30) else Color.White, Color(0xFF27272A))
        Text("${telemetry.batteryPct}% • ${"%.1f".format(telemetry.estimatedVoltage)}V", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFFA1A1AA))
      }
    }
  }
}

/**
 * 6. SW900 Wide High-Contrast LCD Display
 */
@Composable
fun Sw900LcdHud(
  dispSpeed: Double,
  unitLbl: String,
  telemetry: LiveTelemetrySnapshot,
  settings: MasterSettings,
) {
  val lcdBg = Color(0xFF102A2E)
  val lcdFg = Color(0xFFB8FFF4)
  val pas = deriveGearLevel(settings.rideMode, telemetry.speedKmh)
  val watts = estPowerWatts(telemetry)

  Surface(
    color = Color(0xFF11151B),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(2.dp, Color(0xFF3F4E5A)),
    modifier = Modifier.fillMaxWidth().height(198.dp).padding(horizontal = 6.dp).testTag("hud_sw900_lcd"),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(8.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(lcdBg)
          .border(1.dp, lcdFg.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
          .padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        OemBatterySegments(telemetry.batteryPct, lcdFg, Color(0xFF1C464D), vertical = true)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("PAS", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = lcdFg.copy(alpha = 0.75f))
          Text("$pas", fontFamily = FontFamily.Monospace, fontSize = 24.sp, fontWeight = FontWeight.Black, color = lcdFg)
        }
      }

      Column(
        modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("SW900", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = lcdFg.copy(alpha = 0.7f))
          Text("VOLT ${"%.1f".format(telemetry.estimatedVoltage)}V (${telemetry.batteryPct}%)", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = lcdFg)
        }

        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "%.1f".format(dispSpeed),
            fontFamily = FontFamily.Monospace,
            fontSize = 54.sp,
            fontWeight = FontWeight.Black,
            color = lcdFg,
            maxLines = 1,
          )
          Spacer(Modifier.width(6.dp))
          Text(unitLbl, fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = lcdFg, modifier = Modifier.padding(bottom = 10.dp))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("DST ${"%.2f".format(formatDistValue(telemetry.tripDistanceKm, settings.distanceUnit))}${distUnitLabel(settings.distanceUnit)}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = lcdFg, maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text("PWR ${watts}W", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = lcdFg, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

/**
 * 7. Vsett / Minimotors EY3 Throttle Display
 */
@Composable
fun VsettEy3Hud(
  dispSpeed: Double,
  unitLbl: String,
  progress: Float,
  telemetry: LiveTelemetrySnapshot,
  settings: MasterSettings,
) {
  val eyBg = Color(0xFF06252B)
  val eyInk = Color(0xFF7CFCE2)
  val gear = ((deriveGearLevel(settings.rideMode, telemetry.speedKmh) + 1) / 2).coerceIn(1, 3)

  Surface(
    color = Color(0xFF14181E),
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(2.dp, eyInk.copy(alpha = 0.55f)),
    modifier = Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 10.dp).testTag("hud_vsett_ey3"),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(8.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(eyBg)
          .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(Icons.Default.Bolt, contentDescription = "Motor Power", tint = eyInk, modifier = Modifier.size(16.dp))
          Text("GEAR $gear", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 12.sp, color = eyInk)
        }
        Text("VSETT • EY3", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = eyInk.copy(alpha = 0.72f))
        Text("${"%.1f".format(telemetry.estimatedVoltage)}V", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFFFBBF24))
      }

      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
          Text("VOLT %", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = eyInk.copy(alpha = 0.7f))
          Text("${telemetry.batteryPct}%", fontFamily = FontFamily.Monospace, fontSize = 20.sp, fontWeight = FontWeight.Black, color = eyInk)
        }
        Row(verticalAlignment = Alignment.Bottom) {
          Text("%.1f".format(dispSpeed), fontFamily = FontFamily.Monospace, fontSize = 50.sp, fontWeight = FontWeight.Black, color = eyInk, maxLines = 1)
          Spacer(Modifier.width(4.dp))
          Text(unitLbl, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = eyInk, modifier = Modifier.padding(bottom = 10.dp))
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
          modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF0E3D45)),
        ) {
          Box(modifier = Modifier.fillMaxWidth(progress.coerceIn(0.03f, 1f)).fillMaxHeight().background(eyInk))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("TRIP ${"%.2f".format(formatDistValue(telemetry.tripDistanceKm, settings.distanceUnit))} ${distUnitLabel(settings.distanceUnit)}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = eyInk)
          Text("TIME %02d:%02d".format(telemetry.elapsedSec / 60, telemetry.elapsedSec % 60), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = eyInk)
        }
      }
    }
  }
}
