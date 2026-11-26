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
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WbIncandescent
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LiveTelemetrySnapshot
import com.example.data.MasterSettings
import kotlin.math.abs
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

/**
 * Authentic 7-segment style digital readout with faint unlit `88.8` LCD/LED segment ghosting behind the active digits.
 */
@Composable
private fun SevenSegmentReadout(
  valueText: String,
  ghostText: String,
  fontSize: TextUnit,
  activeColor: Color,
  ghostColor: Color = activeColor.copy(alpha = 0.11f),
  modifier: Modifier = Modifier,
) {
  Box(contentAlignment = Alignment.CenterEnd, modifier = modifier) {
    Text(
      text = ghostText,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Black,
      fontSize = fontSize,
      color = ghostColor,
      maxLines = 1,
      softWrap = false,
    )
    Text(
      text = valueText,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Black,
      fontSize = fontSize,
      color = activeColor,
      maxLines = 1,
      softWrap = false,
    )
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
      verticalArrangement = Arrangement.spacedBy(2.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .border(1.2.dp, activeColor.copy(alpha = 0.78f), RoundedCornerShape(3.dp))
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
              .width(16.dp)
              .height(5.dp)
              .background(segCol, RoundedCornerShape(1.dp)),
        )
      }
    }
  } else {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
      Row(
        modifier =
          Modifier
            .border(1.3.dp, activeColor.copy(alpha = 0.85f), RoundedCornerShape(2.5.dp))
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
                .width(if (segments > 6) 4.dp else 6.dp)
                .height(10.dp)
                .background(segCol, RoundedCornerShape(1.dp)),
          )
        }
      }
      Box(Modifier.width(2.5.dp).height(6.dp).background(activeColor.copy(alpha = 0.85f), RoundedCornerShape(1.dp)))
    }
  }
}

/**
 * Physical hardware button pill for OEM handlebar display bezels (`POWER`, `MODE`, `+`, `-`).
 */
@Composable
private fun OemBezelButton(
  label: String,
  accent: Color = Color(0xFF64748B),
  modifier: Modifier = Modifier,
) {
  Surface(
    color = Color(0xFF1A202C),
    shape = RoundedCornerShape(5.dp),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
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
 * 1. Authentic S866 Backlit Monochrome E-Bike & Scooter Handlebar LCD
 * Complete with left 3-button tactile rubber remote pod (+ / ⏻ / -), LCD segment framing,
 * PAS 1-5 box, ghosted 88.8 7-segment speed numerals, live phase watts/amps, and ODO/TRIP/MAX/TM registers.
 */
@Composable
fun S866LcdHud(
  dispSpeed: Double,
  unitLbl: String,
  telemetry: LiveTelemetrySnapshot,
  settings: MasterSettings,
) {
  val lcdTop = Color(0xFF0E3F82)
  val lcdBottom = Color(0xFF072552)
  val lcdInk = Color(0xFFE0F7FF)
  val lcdDim = Color(0xFF214F88)
  val pas = deriveGearLevel(settings.rideMode, telemetry.speedKmh)
  val watts = estPowerWatts(telemetry)
  val amps = if (telemetry.estimatedVoltage > 10.0) watts / telemetry.estimatedVoltage else 0.0
  val tripDisp = formatDistValue(telemetry.tripDistanceKm, settings.distanceUnit)
  val distLbl = distUnitLabel(settings.distanceUnit).uppercase()
  val tempLbl = formatTempLabel(estControllerTempC(telemetry), settings.tempUnit)

  Row(
    modifier = Modifier.fillMaxWidth().height(206.dp).padding(horizontal = 4.dp).testTag("hud_s866_lcd"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    // Authentic S866 Left Handlebar 3-Button Rubber Switch Pod (+ / ⏻ / -)
    Surface(
      color = Color(0xFF141820),
      shape = RoundedCornerShape(10.dp),
      border = BorderStroke(1.5.dp, Color(0xFF334155)),
      modifier = Modifier.width(32.dp).height(142.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxSize().padding(4.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        OemBezelButton("+", Color(0xFF475569), Modifier.fillMaxWidth())
        OemBezelButton("⏻", Color(0xFFEF4444), Modifier.fillMaxWidth())
        OemBezelButton("-", Color(0xFF475569), Modifier.fillMaxWidth())
      }
    }

    // Main S866 ABS Bezel & Backlit Blue STN LCD Glass
    Surface(
      color = Color(0xFF11151C),
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(2.5.dp, Color(0xFF3A4659)),
      modifier = Modifier.weight(1f).fillMaxHeight(),
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .padding(7.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Brush.verticalGradient(listOf(lcdTop, lcdBottom)))
            .border(1.5.dp, Color(0xFF5AB2F2).copy(alpha = 0.65f), RoundedCornerShape(9.dp))
            .drawBehind {
              // Subtle diagonal anti-glare LCD polarizer reflection
              val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width * 0.42f, 0f)
                lineTo(0f, size.height * 0.65f)
                close()
              }
              drawPath(path, Color.White.copy(alpha = 0.035f))
            }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        // Top Status Bar: 6-Segment Battery Outline + Pack Voltage + Sag + Headlight/Temp
        Row(
          Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            OemBatterySegments(telemetry.batteryPct, lcdInk, lcdDim, segments = 6)
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
            fontSize = 12.sp,
            color = lcdInk,
          )
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.WbIncandescent, contentDescription = null, tint = lcdInk, modifier = Modifier.size(13.dp))
            Text(
              "S866 • $tempLbl",
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              fontSize = 9.5.sp,
              color = lcdInk.copy(alpha = 0.85f),
            )
          }
        }

        // Center Row: Boxed PAS 1-5 Gear Frame | Giant 7-Segment Speed | Phase Watts & Current
        Row(
          Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .border(1.2.dp, lcdInk.copy(alpha = 0.65f), RoundedCornerShape(5.dp))
              .padding(horizontal = 8.dp, vertical = 4.dp),
          ) {
            Text("PAS", fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = lcdInk.copy(alpha = 0.85f))
            SevenSegmentReadout(
              valueText = "$pas",
              ghostText = "8",
              fontSize = 28.sp,
              activeColor = lcdInk,
            )
            Text(
              settings.rideMode.uppercase().take(5),
              fontFamily = FontFamily.Monospace,
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Bold,
              color = lcdInk.copy(alpha = 0.85f),
            )
          }

          Row(verticalAlignment = Alignment.Bottom) {
            SevenSegmentReadout(
              valueText = "%4.1f".format(dispSpeed),
              ghostText = "88.8",
              fontSize = 48.sp,
              activeColor = lcdInk,
            )
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
              Text(
                "SPD",
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                color = lcdInk.copy(alpha = 0.7f),
              )
              Text(
                unitLbl,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 11.5.sp,
                color = lcdInk,
              )
            }
          }

          Column(horizontalAlignment = Alignment.End) {
            Text(
              "${watts}W",
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Black,
              fontSize = 14.sp,
              color = lcdInk,
            )
            Text(
              "${"%.1f".format(amps)}A",
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp,
              color = lcdInk.copy(alpha = 0.88f),
            )
            Text(
              "TM %02d:%02d".format(telemetry.elapsedSec / 60, telemetry.elapsedSec % 60),
              fontFamily = FontFamily.Monospace,
              fontSize = 9.sp,
              color = lcdInk.copy(alpha = 0.8f),
            )
          }
        }

        // Bottom Multi-Field Register Strip (TRIP | MAX | AVG | SAG)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, lcdInk.copy(alpha = 0.38f), RoundedCornerShape(4.dp))
            .background(Color(0x22000000), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            "TRIP ${"%.2f".format(tripDisp)} $distLbl",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = lcdInk,
          )
          Text(
            "MAX ${"%.1f".format(formatSpeedValue(telemetry.maxSpeedKmh, settings.distanceUnit))} $unitLbl",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = lcdInk,
          )
          Text(
            "AVG ${"%.1f".format(formatSpeedValue(telemetry.avgSpeedKmh, settings.distanceUnit))}",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = lcdInk,
          )
        }
      }
    }
  }
}

/**
 * 2. Authentic QS-S4 / TF-100 / TF-11 Round Finger-Throttle Scooter LCD
 * Includes circular CNC throttle bezel, index-finger trigger lever ear, bottom POWER & MODE buttons,
 * perimeter red/amber RPM tick segments, GEAR 1-3 indicator, and 7-segment speed + voltage/trip register.
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
  val cyanGlow = Color(0xFF38EEFF)
  val amberGlow = Color(0xFFFFA826)
  val redGlow = Color(0xFFFF3B5C)
  val watts = estPowerWatts(telemetry)

  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier.size(214.dp).testTag("hud_tf11_lcd"),
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = size.width / 2f
      val cy = size.height / 2f
      val outerR = size.minDimension * 0.47f

      // Right-hand index-finger trigger throttle ear silhouette
      val triggerPath = Path().apply {
        moveTo(cx + outerR * 0.72f, cy - outerR * 0.12f)
        lineTo(cx + outerR * 1.05f, cy + outerR * 0.28f)
        lineTo(cx + outerR * 0.88f, cy + outerR * 0.56f)
        lineTo(cx + outerR * 0.52f, cy + outerR * 0.35f)
        close()
      }
      drawPath(triggerPath, Color(0xFF1E293B))

      // Outer molded ABS + aluminum ring
      drawCircle(color = Color(0xFF12161F), radius = outerR, center = Offset(cx, cy))
      drawCircle(color = Color(0xFF334155), radius = outerR, center = Offset(cx, cy), style = Stroke(4.5.dp.toPx()))
      drawCircle(color = Color(0xFF050D17), radius = outerR - 8.dp.toPx(), center = Offset(cx, cy))

      // Segmented perimeter tachometer blocks (like real QS-S4 / TF-100 LCD)
      val arcR = outerR - 15.dp.toPx()
      val totalBlocks = 22
      for (i in 0 until totalBlocks) {
        val frac = i.toFloat() / totalBlocks
        val angle = 140f + frac * 260f
        val lit = frac <= progress
        val segColor =
          when {
            !lit -> Color(0xFF102536)
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
      verticalArrangement = Arrangement.spacedBy(2.dp),
      modifier = Modifier.padding(20.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("QS-S4", fontFamily = FontFamily.Monospace, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = cyanGlow.copy(alpha = 0.75f))
        Surface(
          color = amberGlow.copy(alpha = 0.18f),
          shape = RoundedCornerShape(4.dp),
          border = BorderStroke(1.dp, amberGlow),
        ) {
          Text(
            "GEAR $gear",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Black,
            color = amberGlow,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
          )
        }
      }

      SevenSegmentReadout(
        valueText = "%4.1f".format(dispSpeed),
        ghostText = "88.8",
        fontSize = 38.sp,
        activeColor = Color.White,
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
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        color = amberGlow,
      )

      // Physical POWER & MODE rubber buttons on lower bezel
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 2.dp)) {
        OemBezelButton("⏻ PWR", cyanGlow)
        OemBezelButton("MODE", amberGlow)
      }
    }
  }
}

/**
 * 3. Authentic Bafang DPC-18 / Sur-Ron / Super73 Color IPS TFT Cockpit Display
 * Full-color high-luminance automotive IPS dashboard with sweeping power watt/amp bar,
 * SPORT/ECO/RACE drive mode badge, live pack voltage + under-load sag, controller temp, and trip computer.
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
    color = Color(0xFF0A0E17),
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(2.5.dp, Color(0xFF334155)),
    modifier = Modifier.fillMaxWidth().height(210.dp).padding(horizontal = 6.dp).testTag("hud_bafang_dpc18"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(Brush.verticalGradient(listOf(Color(0xFF111827), Color(0xFF070A10))))
        .padding(12.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top OEM IPS TFT Header Bar: Drive Mode Pill | Model & Temp | Battery Bar + %
      Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Surface(
            color = modeCol.copy(alpha = 0.22f),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.2.dp, modeCol),
          ) {
            Text(
              text = settings.rideMode.uppercase(),
              fontSize = 10.sp,
              fontWeight = FontWeight.Black,
              color = modeCol,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
          }
          Icon(Icons.Default.WbIncandescent, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
        }

        Text(
          "BAFANG DPC-18 • $tempLbl",
          fontFamily = FontFamily.Monospace,
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF94A3B8),
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          OemBatterySegments(telemetry.batteryPct, modeCol, Color(0xFF1E293B), segments = 6)
          Text(
            "${telemetry.batteryPct}%",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
          )
        }
      }

      // Center IPS Cluster: Left Assist Level & Amps | Center Giant Speed | Right Pack Voltage & Trip
      Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .width(76.dp)
            .background(Color(0xFF161F30), RoundedCornerShape(10.dp))
            .border(1.dp, modeCol.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        ) {
          Text("ASSIST", fontSize = 8.5.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
          Text("$pas", fontSize = 28.sp, fontWeight = FontWeight.Black, color = modeCol)
          Text(
            "${watts}W • ${"%.0f".format(amps)}A",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = Color(0xFF38BDF8),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
          )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          SevenSegmentReadout(
            valueText = "%4.1f".format(dispSpeed),
            ghostText = "88.8",
            fontSize = 48.sp,
            activeColor = Color.White,
          )
          Text(
            "$unitLbl • LIVE CAN-BUS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8),
          )
        }

        Column(
          horizontalAlignment = Alignment.End,
          modifier = Modifier
            .width(84.dp)
            .background(Color(0xFF161F30), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
            .padding(vertical = 6.dp, horizontal = 6.dp),
        ) {
          Text("PACK VOLTS", fontSize = 8.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
          Text(
            "${"%.1f".format(telemetry.estimatedVoltage)}V",
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFFBBF24),
          )
          Text(
            "SAG -${"%.1f".format(telemetry.voltageSagV)}V",
            fontFamily = FontFamily.Monospace,
            fontSize = 8.5.sp,
            color = if (telemetry.voltageSagV > 3.5) Color(0xFFEF4444) else Color(0xFF94A3B8),
          )
          Text(
            "TRIP ${"%.1f".format(formatDistValue(telemetry.tripDistanceKm, settings.distanceUnit))}$distLbl",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
          )
        }
      }

      // Bottom Graduated Phase Power Output Bar + Peak Speed & Range
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(
            "PHASE POWER (${watts}W)",
            fontSize = 8.5.sp,
            color = Color(0xFF94A3B8),
            fontWeight = FontWeight.Bold,
          )
          Text(
            "MAX ${"%.1f".format(formatSpeedValue(telemetry.maxSpeedKmh, settings.distanceUnit))} $unitLbl • AVG ${"%.1f".format(formatSpeedValue(telemetry.avgSpeedKmh, settings.distanceUnit))} $unitLbl",
            fontSize = 8.5.sp,
            color = Color(0xFFCBD5E1),
            fontWeight = FontWeight.Bold,
          )
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
                .background(Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF38BDF8), Color(0xFFF59E0B), Color(0xFFEF4444)))),
          )
        }
      }
    }
  }
}

/**
 * 4. Authentic Kaabo / Nami Viper / iSinwheel Wide Color Matrix TFT Cluster
 * Features DUAL/SINGLE motor drive indicator, S/D/E neon gear mode badge, dual Front/Rear controller
 * load bars, 20-segment color power curve, and live pack voltage/temperature footer.
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
    color = Color(0xFF070A12),
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(2.2.dp, modeTint.copy(alpha = 0.72f)),
    modifier = Modifier.fillMaxWidth().height(208.dp).padding(horizontal = 6.dp).testTag("hud_isinwheel_tft"),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top Status Ribbon: Gear Mode Circle + DUAL MOTOR Pill + Optics/BLE + Battery
      Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Surface(color = modeTint, shape = CircleShape, modifier = Modifier.size(26.dp)) {
            Box(contentAlignment = Alignment.Center) {
              Text(modeLbl, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.Black)
            }
          }
          Surface(
            color = modeTint.copy(alpha = 0.16f),
            shape = RoundedCornerShape(5.dp),
            border = BorderStroke(1.dp, modeTint.copy(alpha = 0.65f)),
          ) {
            Text(
              if (modeLbl == "E") "SINGLE ECO" else "DUAL AWD",
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Black,
              color = modeTint,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
          Icon(Icons.Default.WbIncandescent, contentDescription = "Headlight Indicator", tint = Color(0xFF38BDF8), modifier = Modifier.size(15.dp))
          Icon(Icons.Default.Bluetooth, contentDescription = "Telemetry Active", tint = Color(0xFF60A5FA), modifier = Modifier.size(15.dp))
        }

        Text("MATRIX TFT • $tempLbl", fontWeight = FontWeight.Black, fontSize = 9.5.sp, color = Color(0xFF94A3B8))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          OemBatterySegments(telemetry.batteryPct, Color(0xFF22C55E), Color(0xFF1E293B), segments = 5)
          Text("${telemetry.batteryPct}%", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFF22C55E))
        }
      }

      // Center Speed + Front/Rear ESC Telemetry Pods
      Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(horizontalAlignment = Alignment.Start) {
          Text("F-ESC", fontFamily = FontFamily.Monospace, fontSize = 8.5.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
          Text("${watts / 2}W", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Black)
          Text("CRUISE RDY", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
        }

        Row(verticalAlignment = Alignment.Bottom) {
          SevenSegmentReadout(
            valueText = "%2.0f".format(dispSpeed).padStart(2, '0'),
            ghostText = "88",
            fontSize = 54.sp,
            activeColor = Color.White,
          )
          Spacer(Modifier.width(6.dp))
          Text(
            unitLbl,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = modeTint,
            modifier = Modifier.padding(bottom = 10.dp),
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Text("R-ESC", fontFamily = FontFamily.Monospace, fontSize = 8.5.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
          Text("${(watts + 1) / 2}W", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = modeTint, fontWeight = FontWeight.Black)
          Text("MAX ${"%.0f".format(formatSpeedValue(telemetry.maxSpeedKmh, settings.distanceUnit))}", fontFamily = FontFamily.Monospace, fontSize = 8.5.sp, color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold)
        }
      }

      // Signature 20-segment curved rainbow power/speed bar
      Canvas(modifier = Modifier.fillMaxWidth().height(14.dp)) {
        val segs = 20
        val gap = 4.5f
        val segW = (size.width - gap * (segs - 1)) / segs
        for (i in 0 until segs) {
          val f = i.toFloat() / segs
          val col =
            when {
              f >= progress.coerceAtLeast(0.05f) -> Color(0xFF1B2434)
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
  }
}

/**
 * 5. Authentic Segway Ninebot Max G2 / GT Smoked Pill Stem LED Dot-Matrix Bezel
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
  val modeColor = if (mode == "S") Color(0xFFFF3B30) else if (mode == "D") Color(0xFF38BDF8) else Color(0xFF22C55E)
  val distLbl = distUnitLabel(settings.distanceUnit).uppercase()
  val tempLbl = formatTempLabel(estControllerTempC(telemetry), settings.tempUnit)

  Surface(
    color = Color(0xFF040507),
    shape = RoundedCornerShape(32.dp),
    border = BorderStroke(2.5.dp, Color(0xFF27272A)),
    modifier = Modifier.width(248.dp).height(210.dp).testTag("hud_ninebot_matrix"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(Brush.verticalGradient(listOf(Color(0xFF0B0E14), Color(0xFF030406))))
        .padding(14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Bluetooth, contentDescription = "Connected", tint = Color(0xFF38BDF8), modifier = Modifier.size(15.dp))
          Icon(Icons.Default.WbIncandescent, contentDescription = "Headlight", tint = Color(0xFF22C55E), modifier = Modifier.size(15.dp))
        }
        Text("NINEBOT OS", fontFamily = FontFamily.Monospace, fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF71717A))
        Surface(color = modeColor.copy(alpha = 0.22f), shape = RoundedCornerShape(6.dp), border = BorderStroke(1.2.dp, modeColor)) {
          Text(
            text = mode,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = modeColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
          )
        }
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SevenSegmentReadout(
          valueText = "%.0f".format(dispSpeed).padStart(2, '0'),
          ghostText = "88",
          fontSize = 60.sp,
          activeColor = Color.White,
        )
        Text(
          "$unitLbl • ${"%.1f".format(formatDistValue(telemetry.tripDistanceKm, settings.distanceUnit))} $distLbl",
          fontFamily = FontFamily.Monospace,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFFA1A1AA),
        )
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OemBatterySegments(
          telemetry.batteryPct,
          if (telemetry.batteryPct < 20) Color(0xFFFF3B30) else Color.White,
          Color(0xFF27272A),
          segments = 5,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Text(
            "${telemetry.batteryPct}% • ${"%.1f".format(telemetry.estimatedVoltage)}V • $tempLbl",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.5.sp,
            color = Color(0xFFA1A1AA),
          )
          OemBezelButton("⏻", Color(0xFF3F3F46))
        }
      }
    }
  }
}

/**
 * 6. Authentic SW900 / KT-LCD3 Wide High-Contrast E-Bike & Stealth Bomber LCD Display
 * Features 4 corner mounting screws, vertical battery & watt tower, boxed PAS 1-5 frame,
 * 7-segment ghosted speed digits, and DST / VOLT / PWR / TEMP registers.
 */
@Composable
fun Sw900LcdHud(
  dispSpeed: Double,
  unitLbl: String,
  telemetry: LiveTelemetrySnapshot,
  settings: MasterSettings,
) {
  val lcdBg = Color(0xFF0E292D)
  val lcdFg = Color(0xFFC4FFF7)
  val pas = deriveGearLevel(settings.rideMode, telemetry.speedKmh)
  val watts = estPowerWatts(telemetry)
  val tempLbl = formatTempLabel(estControllerTempC(telemetry), settings.tempUnit)
  val distLbl = distUnitLabel(settings.distanceUnit).uppercase()

  Surface(
    color = Color(0xFF11151B),
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(2.5.dp, Color(0xFF3F4E5A)),
    modifier = Modifier.fillMaxWidth().height(204.dp).padding(horizontal = 6.dp).testTag("hud_sw900_lcd"),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(8.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Brush.verticalGradient(listOf(Color(0xFF13353A), lcdBg)))
          .border(1.3.dp, lcdFg.copy(alpha = 0.50f), RoundedCornerShape(8.dp))
          .padding(11.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Left Column: Vertical Battery Tower + Boxed PAS Assist Level
      Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        OemBatterySegments(telemetry.batteryPct, lcdFg, Color(0xFF1C464D), segments = 5, vertical = true)
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .border(1.dp, lcdFg.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        ) {
          Text("PAS", fontFamily = FontFamily.Monospace, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = lcdFg.copy(alpha = 0.8f))
          SevenSegmentReadout(
            valueText = "$pas",
            ghostText = "8",
            fontSize = 24.sp,
            activeColor = lcdFg,
          )
        }
      }

      // Right Column: Header Voltage + Giant 7-Segment Speed + Footer Registers
      Column(
        modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("SW900 • $tempLbl", fontFamily = FontFamily.Monospace, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = lcdFg.copy(alpha = 0.78f))
          Text(
            "VOLT ${"%.1f".format(telemetry.estimatedVoltage)}V (${telemetry.batteryPct}%)",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Black,
            color = lcdFg,
          )
        }

        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
          SevenSegmentReadout(
            valueText = "%4.1f".format(dispSpeed),
            ghostText = "88.8",
            fontSize = 52.sp,
            activeColor = lcdFg,
          )
          Spacer(Modifier.width(6.dp))
          Column(modifier = Modifier.padding(bottom = 8.dp)) {
            Text("MAX ${"%.0f".format(formatSpeedValue(telemetry.maxSpeedKmh, settings.distanceUnit))}", fontFamily = FontFamily.Monospace, fontSize = 8.5.sp, color = lcdFg.copy(alpha = 0.75f))
            Text(unitLbl, fontFamily = FontFamily.Monospace, fontSize = 12.5.sp, fontWeight = FontWeight.Black, color = lcdFg)
          }
        }

        Row(
          Modifier
            .fillMaxWidth()
            .border(1.dp, lcdFg.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
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
          Text(
            "PWR ${watts}W",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.5.sp,
            color = lcdFg,
            fontWeight = FontWeight.Black,
          )
          Text(
            "TM %02d:%02d".format(telemetry.elapsedSec / 60, telemetry.elapsedSec % 60),
            fontFamily = FontFamily.Monospace,
            fontSize = 9.5.sp,
            color = lcdFg,
          )
        }
      }
    }
  }
}

/**
 * 7. Authentic Minimotors Dualtron / VSETT EYE3 (EY3) Green-Backlit Throttle LCD
 * Replicates the signature Minimotors emerald-lime backlit LCD substrate (`#0C331E` -> `#7EFFA7`),
 * top TIME + PAS/GEAR 1-3 readout, ghosted `88.8` 7-segment digits, 7-bar battery outline,
 * bottom TRIP/ODO/VOL register, and physical `⏻ POWER` + `• MODE` rubber bezel buttons.
 */
@Composable
fun VsettEy3Hud(
  dispSpeed: Double,
  unitLbl: String,
  progress: Float,
  telemetry: LiveTelemetrySnapshot,
  settings: MasterSettings,
) {
  val eyGlassTop = Color(0xFF103E24)
  val eyGlassBottom = Color(0xFF082616)
  val eyInk = Color(0xFF86FFAC)
  val eyDim = Color(0xFF1C5A37)
  val gear = ((deriveGearLevel(settings.rideMode, telemetry.speedKmh) + 1) / 2).coerceIn(1, 3)
  val distLbl = distUnitLabel(settings.distanceUnit).uppercase()
  val watts = estPowerWatts(telemetry)

  Surface(
    color = Color(0xFF12161D),
    shape = RoundedCornerShape(24.dp),
    border = BorderStroke(2.5.dp, Color(0xFF334155)),
    modifier = Modifier.fillMaxWidth().height(208.dp).padding(horizontal = 8.dp).testTag("hud_vsett_ey3"),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(8.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Backlit Green EY3 LCD Window
      Column(
        modifier =
          Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(Brush.verticalGradient(listOf(eyGlassTop, eyGlassBottom)))
            .border(1.5.dp, eyInk.copy(alpha = 0.55f), RoundedCornerShape(15.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        // Top EY3 LCD Bar: TIME 00:00 | MINIMOTORS EYE3 | PAS / GEAR Circle
        Row(
          Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            "TIME %02d:%02d".format(telemetry.elapsedSec / 60, telemetry.elapsedSec % 60),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = eyInk,
          )
          Text(
            "MINIMOTORS • EYE3",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            fontSize = 9.5.sp,
            color = eyInk.copy(alpha = 0.8f),
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
              .border(1.2.dp, eyInk, RoundedCornerShape(5.dp))
              .padding(horizontal = 6.dp, vertical = 1.dp),
          ) {
            Icon(Icons.Default.Bolt, contentDescription = null, tint = eyInk, modifier = Modifier.size(12.dp))
            Text(
              "GEAR $gear",
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Black,
              fontSize = 10.5.sp,
              color = eyInk,
            )
          }
        }

        // Center EY3 Readout: Battery % + 7-Bar Outline | Giant 7-Segment Speed + Unit
        Row(
          Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
              "BAT ${telemetry.batteryPct}%",
              fontFamily = FontFamily.Monospace,
              fontSize = 14.sp,
              fontWeight = FontWeight.Black,
              color = eyInk,
            )
            OemBatterySegments(telemetry.batteryPct, eyInk, eyDim, segments = 7)
            Text(
              "${"%.1f".format(telemetry.estimatedVoltage)}V • ${watts}W",
              fontFamily = FontFamily.Monospace,
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
              color = eyInk.copy(alpha = 0.9f),
            )
          }

          Row(verticalAlignment = Alignment.Bottom) {
            SevenSegmentReadout(
              valueText = "%4.1f".format(dispSpeed),
              ghostText = "88.8",
              fontSize = 48.sp,
              activeColor = eyInk,
              ghostColor = eyInk.copy(alpha = 0.13f),
            )
            Spacer(Modifier.width(4.dp))
            Text(
              unitLbl,
              fontFamily = FontFamily.Monospace,
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Black,
              color = eyInk,
              modifier = Modifier.padding(bottom = 9.dp),
            )
          }
        }

        // Bottom EY3 throttle bar + TRIP / MAX / E-ABS registers
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Box(
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(eyDim),
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
              fontWeight = FontWeight.Bold,
              color = eyInk,
            )
            Text(
              "MAX ${"%.1f".format(formatSpeedValue(telemetry.maxSpeedKmh, settings.distanceUnit))} $unitLbl",
              fontFamily = FontFamily.Monospace,
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
              color = eyInk,
            )
            Text(
              "E-ABS • DUAL",
              fontFamily = FontFamily.Monospace,
              fontSize = 9.sp,
              fontWeight = FontWeight.Black,
              color = eyInk.copy(alpha = 0.85f),
            )
          }
        }
      }

      // Lower EY3 Physical Bezel Buttons (Power & Mode)
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 5.dp, start = 12.dp, end = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OemBezelButton("⏻ POWER", eyInk)
        Text(
          "EY3 SMART THROTTLE",
          fontFamily = FontFamily.Monospace,
          fontSize = 8.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF64748B),
        )
        OemBezelButton("• MODE", eyInk)
      }
    }
  }
}
