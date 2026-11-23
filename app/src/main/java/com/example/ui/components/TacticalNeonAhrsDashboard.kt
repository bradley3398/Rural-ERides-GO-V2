package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LiveTelemetrySnapshot
import com.example.data.MasterSettings
import com.example.ui.theme.LocalTacticalChrome
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val NeonBgObsidian = Color(0xFF04070D)
private val NeonPanelDark = Color(0xFF0A111E)
private val NeonLime = Color(0xFF39FF14)
private val NeonCyan = Color(0xFF00F5FF)
private val NeonAmber = Color(0xFFFFB800)
private val NeonCrimson = Color(0xFFFF2A6D)
private val NeonGridLine = Color(0xFF172A45)

/**
 * High-Contrast 'Tactical Neon' 9-DOF AHRS Telemetry Dashboard.
 * Visualizes live hardware Pitch, Roll, and Yaw (360° Compass) from Android's 9-DOF sensor fusion
 * (Accelerometer + Gyroscope + Magnetometer / Rotation Vector) using spring-animated needle gauges.
 */
@Composable
fun TacticalNeonAhrsDashboard(
  telemetry: LiveTelemetrySnapshot,
  settings: MasterSettings,
  onTareLevel: () -> Unit,
  onResetPeaks: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val stiffness =
    when (settings.ahrsGaugeDamping) {
      "Snappy" -> Spring.StiffnessMedium
      "Smooth" -> Spring.StiffnessVeryLow
      else -> Spring.StiffnessLow
    }
  val damping =
    when (settings.ahrsGaugeDamping) {
      "Snappy" -> Spring.DampingRatioLowBouncy
      "Smooth" -> Spring.DampingRatioNoBouncy
      else -> Spring.DampingRatioMediumBouncy
    }

  val animatedPitch by animateFloatAsState(
    targetValue = telemetry.pitchDeg.coerceIn(-60f, 60f),
    animationSpec = spring(dampingRatio = damping, stiffness = stiffness),
    label = "ahrs_pitch_needle",
  )
  val animatedRoll by animateFloatAsState(
    targetValue = telemetry.rollDeg.coerceIn(-60f, 60f),
    animationSpec = spring(dampingRatio = damping, stiffness = stiffness),
    label = "ahrs_roll_needle",
  )

  // Continuous shortest-angle tracking for 360° Yaw so needle never whips around 359° -> 0°
  var continuousYaw by remember { mutableFloatStateOf(telemetry.yawDeg) }
  val yawTarget = remember(telemetry.yawDeg) {
    val normCur = ((continuousYaw % 360f) + 360f) % 360f
    var delta = telemetry.yawDeg - normCur
    if (delta > 180f) delta -= 360f
    if (delta < -180f) delta += 360f
    continuousYaw += delta
    continuousYaw
  }
  val animatedYaw by animateFloatAsState(
    targetValue = yawTarget,
    animationSpec = spring(dampingRatio = damping, stiffness = stiffness),
    label = "ahrs_yaw_needle",
  )

  val leanExceeded = abs(telemetry.rollDeg) >= settings.ahrsLeanAlertDeg
  val cardinal = headingToCardinal(telemetry.yawDeg)
  val accuracyLabel =
    when (telemetry.sensorAccuracy) {
      3 -> "HIGH"
      2 -> "MED"
      1 -> "LOW"
      else -> "CAL"
    }

  val chrome = LocalTacticalChrome.current
  val themePrimary = MaterialTheme.colorScheme.primary
  val themeSecondary = MaterialTheme.colorScheme.secondary

  Surface(
    color = NeonBgObsidian,
    shape = chrome.cardShape,
    border =
      BorderStroke(
        width = chrome.cardBorderWidth.coerceAtLeast(1.4.dp),
        brush = Brush.linearGradient(listOf(themePrimary.copy(alpha = 0.9f), NeonCyan.copy(alpha = 0.8f), themeSecondary.copy(alpha = 0.75f))),
      ),
    modifier = modifier.fillMaxWidth().testTag("tactical_neon_ahrs_dashboard"),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(
            Brush.verticalGradient(
              listOf(
                Color(0xFF071120),
                NeonBgObsidian,
                Color(0xFF050A14),
              )
            )
          )
          .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // HEADER BAR
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Box(
            modifier =
              Modifier
                .size(30.dp)
                .clip(chrome.smallShape)
                .background(themePrimary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(Icons.Default.Sensors, contentDescription = null, tint = themePrimary, modifier = Modifier.size(18.dp))
          }
          Column {
            Text(
              text = "TACTICAL NEON 9-DOF AHRS",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Black,
              color = NeonCyan,
              letterSpacing = 1.1.sp,
            )
            Text(
              text =
                if (telemetry.ahrsFused9Dof) "9-DOF HARDWARE FUSION • ACCEL + GYRO + MAG"
                else "6-DOF IMU LIVE • ACCEL + MAGNETOMETER",
              style = MaterialTheme.typography.labelSmall,
              color = NeonLime.copy(alpha = 0.88f),
              fontSize = 10.sp,
            )
          }
        }
        Surface(
          color = if (leanExceeded) NeonCrimson.copy(alpha = 0.24f) else NeonLime.copy(alpha = 0.16f),
          shape = chrome.smallShape,
          border = BorderStroke(1.dp, if (leanExceeded) NeonCrimson else NeonLime),
        ) {
          Text(
            text = if (leanExceeded) "LEAN ALERT" else "LIVE • $accuracyLabel",
            color = if (leanExceeded) NeonCrimson else NeonLime,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
      }

      // 3 HIGH-CONTRAST ANIMATED NEEDLE GAUGES (PITCH, ROLL, YAW)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        NeonBipolarNeedleGauge(
          title = "PITCH",
          subtitle = if (animatedPitch >= 1f) "CLIMB" else if (animatedPitch <= -1f) "DIVE" else "LEVEL",
          valueDeg = animatedPitch,
          maxRangeDeg = 45f,
          warnDeg = 28f,
          accentColor = NeonLime,
          peakNegDeg = null,
          peakPosDeg = null,
          modifier = Modifier.weight(1f).testTag("ahrs_pitch_gauge"),
        )
        NeonBipolarNeedleGauge(
          title = "ROLL",
          subtitle = if (animatedRoll >= 1f) "BANK R" else if (animatedRoll <= -1f) "BANK L" else "FLAT",
          valueDeg = animatedRoll,
          maxRangeDeg = 60f,
          warnDeg = settings.ahrsLeanAlertDeg.toFloat(),
          accentColor = NeonCyan,
          peakNegDeg = if (settings.ahrsPeakLeanHold && telemetry.peakLeanLeftDeg > 1f) -telemetry.peakLeanLeftDeg else null,
          peakPosDeg = if (settings.ahrsPeakLeanHold && telemetry.peakLeanRightDeg > 1f) telemetry.peakLeanRightDeg else null,
          modifier = Modifier.weight(1f).testTag("ahrs_roll_gauge"),
        )
        NeonYawCompassNeedleGauge(
          animatedYawDeg = animatedYaw,
          displayYawDeg = telemetry.yawDeg,
          cardinal = cardinal,
          modifier = Modifier.weight(1f).testTag("ahrs_yaw_gauge"),
        )
      }

      // LIVE ARTIFICIAL HORIZON + GYRO ANGULAR VELOCITY + MAG FIELD STRIP
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        NeonMiniHorizonBall(
          pitchDeg = animatedPitch,
          rollDeg = animatedRoll,
          modifier = Modifier.size(64.dp),
        )
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AhrsMetricPill("GYRO P/R/Y", "${"%.0f".format(telemetry.gyroPitchRateDps)}/${"%.0f".format(telemetry.gyroRollRateDps)}/${"%.0f".format(telemetry.gyroYawRateDps)}°/s", NeonCyan, Modifier.weight(1f))
            Spacer(Modifier.width(6.dp))
            AhrsMetricPill("MAG FIELD", "${"%.1f".format(telemetry.magMicroTesla)} µT", NeonAmber, Modifier.weight(0.75f))
          }
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AhrsMetricPill(
              "PEAK LEAN L/R",
              "${"%.0f".format(telemetry.peakLeanLeftDeg)}° / ${"%.0f".format(telemetry.peakLeanRightDeg)}°",
              if (leanExceeded) NeonCrimson else NeonLime,
              Modifier.weight(1f),
            )
            Spacer(Modifier.width(6.dp))
            AhrsMetricPill("LAT / PEAK G", "${"%.2f".format(telemetry.lateralG)} / ${"%.2f".format(telemetry.peakG)}G", NeonLime, Modifier.weight(0.85f))
          }
        }
      }

      // LIVE CALIBRATION & PEAK RESET CONTROLS
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Surface(
          onClick = onTareLevel,
          color = NeonCyan.copy(alpha = 0.14f),
          shape = chrome.buttonShape,
          border = BorderStroke(chrome.buttonBorderWidth, NeonCyan.copy(alpha = 0.65f)),
          modifier = Modifier.weight(1f).height(48.dp).testTag("ahrs_tare_zero_button"),
        ) {
          Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Default.CompassCalibration, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("ZERO / TARE LEVEL", color = NeonCyan, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
          }
        }
        Surface(
          onClick = onResetPeaks,
          color = NeonAmber.copy(alpha = 0.14f),
          shape = chrome.buttonShape,
          border = BorderStroke(chrome.buttonBorderWidth, NeonAmber.copy(alpha = 0.65f)),
          modifier = Modifier.weight(0.85f).height(48.dp).testTag("ahrs_reset_peaks_button"),
        ) {
          Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Default.RestartAlt, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("RESET PEAKS", color = NeonAmber, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
          }
        }
      }
    }
  }
}

@Composable
private fun NeonBipolarNeedleGauge(
  title: String,
  subtitle: String,
  valueDeg: Float,
  maxRangeDeg: Float,
  warnDeg: Float,
  accentColor: Color,
  peakNegDeg: Float?,
  peakPosDeg: Float?,
  modifier: Modifier = Modifier,
) {
  val isAlert = abs(valueDeg) >= warnDeg
  val activeColor = if (isAlert) NeonCrimson else accentColor

  Surface(
    color = NeonPanelDark,
    shape = LocalTacticalChrome.current.cardShape,
    border = BorderStroke(1.dp, activeColor.copy(alpha = 0.45f)),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = activeColor,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
      )
      Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
          val cx = size.width / 2f
          val cy = size.height / 2f
          val radius = size.minDimension / 2f - 4.dp.toPx()
          val arcTopLeft = Offset(cx - radius, cy - radius)
          val arcSize = Size(radius * 2f, radius * 2f)

          // Outer subtle ring
          drawCircle(
            color = NeonGridLine,
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5.dp.toPx()),
          )

          // 240-degree tactical scale (-120° to +120° around top 270°)
          drawArc(
            color = Color(0xFF1A2942),
            startAngle = 150f,
            sweepAngle = 240f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
          )

          // Danger zones on both ends
          val warnFrac = (warnDeg / maxRangeDeg).coerceIn(0.4f, 0.95f)
          val dangerSweep = 120f * (1f - warnFrac)
          drawArc(
            color = NeonCrimson.copy(alpha = 0.6f),
            startAngle = 150f,
            sweepAngle = dangerSweep,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Butt),
          )
          drawArc(
            color = NeonCrimson.copy(alpha = 0.6f),
            startAngle = 390f - dangerSweep,
            sweepAngle = dangerSweep,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Butt),
          )

          // Active deflection sweep from 12-o'clock (270°)
          val clampedNorm = (valueDeg / maxRangeDeg).coerceIn(-1f, 1f)
          val sweep = clampedNorm * 120f
          if (abs(sweep) > 0.5f) {
            // Outer neon halo
            drawArc(
              color = activeColor.copy(alpha = 0.25f),
              startAngle = 270f,
              sweepAngle = sweep,
              useCenter = false,
              topLeft = arcTopLeft,
              size = arcSize,
              style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round),
            )
            // Crisp neon core arc
            drawArc(
              color = activeColor,
              startAngle = 270f,
              sweepAngle = sweep,
              useCenter = false,
              topLeft = arcTopLeft,
              size = arcSize,
              style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
            )
          }

          // Graduated ticks (-120° to +120° every 20°)
          for (tick in -6..6) {
            val angleDeg = 270f + tick * 20f
            val rad = angleDeg * PI.toFloat() / 180f
            val isMajor = tick % 3 == 0
            val innerR = radius - (if (isMajor) 10.dp.toPx() else 6.dp.toPx())
            val outerR = radius - 1.dp.toPx()
            drawLine(
              color = if (tick == 0) NeonAmber else Color(0xFF94A3B8).copy(alpha = if (isMajor) 0.9f else 0.45f),
              start = Offset(cx + cos(rad) * innerR, cy + sin(rad) * innerR),
              end = Offset(cx + cos(rad) * outerR, cy + sin(rad) * outerR),
              strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
              cap = StrokeCap.Round,
            )
          }

          // Optional Peak-Hold markers
          listOfNotNull(peakNegDeg, peakPosDeg).forEach { pk ->
            val pkNorm = (pk / maxRangeDeg).coerceIn(-1f, 1f)
            val pkRad = (270f + pkNorm * 120f) * PI.toFloat() / 180f
            drawCircle(
              color = NeonAmber,
              radius = 3.dp.toPx(),
              center = Offset(cx + cos(pkRad) * (radius - 2.dp.toPx()), cy + sin(pkRad) * (radius - 2.dp.toPx())),
            )
          }

          // Animated Tapered Neon Needle
          val needleAngleDeg = clampedNorm * 120f
          rotate(degrees = needleAngleDeg, pivot = Offset(cx, cy)) {
            val needleLen = radius - 10.dp.toPx()
            val tailLen = 9.dp.toPx()
            val needlePath =
              Path().apply {
                moveTo(cx, cy - needleLen)
                lineTo(cx + 3.dp.toPx(), cy + tailLen)
                lineTo(cx - 3.dp.toPx(), cy + tailLen)
                close()
              }
            // Glow behind needle
            drawLine(
              color = activeColor.copy(alpha = 0.35f),
              start = Offset(cx, cy + tailLen * 0.6f),
              end = Offset(cx, cy - needleLen),
              strokeWidth = 7.dp.toPx(),
              cap = StrokeCap.Round,
            )
            drawPath(path = needlePath, color = activeColor)
          }

          // Center hub
          drawCircle(color = NeonBgObsidian, radius = 6.dp.toPx(), center = Offset(cx, cy))
          drawCircle(color = activeColor, radius = 6.dp.toPx(), center = Offset(cx, cy), style = Stroke(2.dp.toPx()))
          drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(cx, cy))
        }
      }
      Text(
        text = "${if (valueDeg >= 0f) "+" else ""}${"%.1f".format(valueDeg)}°",
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Black,
        fontSize = 15.sp,
        color = Color.White,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = activeColor.copy(alpha = 0.85f),
        fontSize = 10.sp,
      )
    }
  }
}

@Composable
private fun NeonYawCompassNeedleGauge(
  animatedYawDeg: Float,
  displayYawDeg: Float,
  cardinal: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = NeonPanelDark,
    shape = LocalTacticalChrome.current.cardShape,
    border = BorderStroke(1.dp, NeonAmber.copy(alpha = 0.5f)),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(Icons.Default.Explore, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(12.dp))
        Text(
          text = "YAW",
          style = MaterialTheme.typography.labelSmall,
          color = NeonAmber,
          fontWeight = FontWeight.Black,
          letterSpacing = 1.sp,
        )
      }
      Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
          val cx = size.width / 2f
          val cy = size.height / 2f
          val radius = size.minDimension / 2f - 4.dp.toPx()

          // Compass bezel ring
          drawCircle(
            color = Color(0xFF1A2942),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 4.dp.toPx()),
          )
          drawCircle(
            color = NeonAmber.copy(alpha = 0.3f),
            radius = radius - 3.dp.toPx(),
            center = Offset(cx, cy),
            style = Stroke(width = 1.dp.toPx()),
          )

          // 24 compass ticks (every 15°, cardinal N/E/S/W highlighted)
          for (i in 0 until 24) {
            val angleDeg = i * 15f - 90f
            val rad = angleDeg * PI.toFloat() / 180f
            val isCardinal = i % 6 == 0
            val innerR = radius - (if (isCardinal) 10.dp.toPx() else 5.dp.toPx())
            val outerR = radius - 1.dp.toPx()
            val col =
              when (i) {
                0 -> NeonCrimson // North tick
                6, 12, 18 -> NeonAmber
                else -> Color(0xFF94A3B8).copy(alpha = 0.4f)
              }
            drawLine(
              color = col,
              start = Offset(cx + cos(rad) * innerR, cy + sin(rad) * innerR),
              end = Offset(cx + cos(rad) * outerR, cy + sin(rad) * outerR),
              strokeWidth = if (isCardinal) 2.2.dp.toPx() else 1.dp.toPx(),
              cap = StrokeCap.Round,
            )
          }

          // Animated 360° Yaw Heading Needle
          rotate(degrees = animatedYawDeg, pivot = Offset(cx, cy)) {
            val tipLen = radius - 9.dp.toPx()
            val tailLen = radius * 0.45f

            // Glow trail
            drawLine(
              color = NeonAmber.copy(alpha = 0.3f),
              start = Offset(cx, cy),
              end = Offset(cx, cy - tipLen),
              strokeWidth = 7.dp.toPx(),
              cap = StrokeCap.Round,
            )

            // Forward Neon Amber/Crimson Heading Tip
            val frontNeedle =
              Path().apply {
                moveTo(cx, cy - tipLen)
                lineTo(cx + 4.dp.toPx(), cy)
                lineTo(cx - 4.dp.toPx(), cy)
                close()
              }
            drawPath(frontNeedle, color = NeonAmber)

            // Counterweight Tail
            val rearNeedle =
              Path().apply {
                moveTo(cx, cy + tailLen)
                lineTo(cx + 3.dp.toPx(), cy)
                lineTo(cx - 3.dp.toPx(), cy)
                close()
              }
            drawPath(rearNeedle, color = NeonCyan.copy(alpha = 0.65f))
          }

          drawCircle(color = NeonBgObsidian, radius = 5.5.dp.toPx(), center = Offset(cx, cy))
          drawCircle(color = NeonAmber, radius = 5.5.dp.toPx(), center = Offset(cx, cy), style = Stroke(1.8.dp.toPx()))
          drawCircle(color = Color.White, radius = 1.8.dp.toPx(), center = Offset(cx, cy))
        }
      }
      Text(
        text = "${"%03.0f".format(((displayYawDeg % 360f) + 360f) % 360f)}°",
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Black,
        fontSize = 15.sp,
        color = Color.White,
      )
      Text(
        text = cardinal,
        style = MaterialTheme.typography.labelSmall,
        color = NeonAmber,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 10.sp,
      )
    }
  }
}

@Composable
private fun NeonMiniHorizonBall(
  pitchDeg: Float,
  rollDeg: Float,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.clip(CircleShape).background(Color(0xFF06101E)),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = size.width / 2f
      val cy = size.height / 2f
      val r = size.minDimension / 2f
      val pitchShiftPx = (pitchDeg.coerceIn(-45f, 45f) / 45f) * (r * 0.55f)

      rotate(degrees = -rollDeg, pivot = Offset(cx, cy)) {
        // Sky upper half
        drawRect(
          color = Color(0xFF0D2D4D),
          topLeft = Offset(-r, -r),
          size = Size(size.width * 2f, cy + pitchShiftPx + r),
        )
        // Ground lower half
        drawRect(
          color = Color(0xFF1B140C),
          topLeft = Offset(-r, cy + pitchShiftPx),
          size = Size(size.width * 2f, size.height * 2f),
        )
        // Glowing neon horizon line
        drawLine(
          color = NeonLime,
          start = Offset(-r, cy + pitchShiftPx),
          end = Offset(size.width + r, cy + pitchShiftPx),
          strokeWidth = 2.dp.toPx(),
        )
      }

      // Fixed aircraft / rider center reticle
      drawLine(
        color = NeonCyan,
        start = Offset(cx - 14.dp.toPx(), cy),
        end = Offset(cx - 5.dp.toPx(), cy),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round,
      )
      drawLine(
        color = NeonCyan,
        start = Offset(cx + 5.dp.toPx(), cy),
        end = Offset(cx + 14.dp.toPx(), cy),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round,
      )
      drawCircle(color = NeonAmber, radius = 2.5.dp.toPx(), center = Offset(cx, cy))
      drawCircle(color = NeonCyan.copy(alpha = 0.8f), radius = r - 1.dp.toPx(), center = Offset(cx, cy), style = Stroke(1.8.dp.toPx()))
    }
  }
}

@Composable
private fun AhrsMetricPill(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = NeonPanelDark,
    shape = LocalTacticalChrome.current.smallShape,
    border = BorderStroke(0.8.dp, accent.copy(alpha = 0.32f)),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
      Text(label, fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
      Text(
        value,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = accent,
        maxLines = 1,
      )
    }
  }
}

private fun headingToCardinal(deg: Float): String {
  val norm = ((deg % 360f) + 360f) % 360f
  val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
  return dirs[((norm + 22.5f) / 45f).toInt() % 8]
}
