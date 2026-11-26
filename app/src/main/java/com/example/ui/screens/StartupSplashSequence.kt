package com.example.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.hardware.VoiceAudioManager
import com.example.ui.theme.LocalTacticalSettings
import com.example.ui.theme.drawHolidayBackgroundArt
import com.example.ui.theme.drawHolidayTopCanopy
import com.example.ui.theme.holidayDecorationSpec
import com.example.ui.theme.isHolidayTheme
import com.example.ui.theme.sanitizeVisualStyleForSeason
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class DetectedSensorInventory(
  val totalHardwareSensors: Int,
  val subsystemTags: List<String>,
)

/**
 * Full-screen upgraded pre-login startup showcase featuring:
 * - Scenic rural backroad driving-horizon artwork with continuous forward-motion road & parallax animation
 * - Dynamic background & accent colors matched to the active Material 3 Theme & VisualStyleMode
 * - Full Holiday assets (top garlands, twinkling string lights, ornaments, and festive particles) when active
 * - Real Android hardware SensorManager enumeration with progressive "X / Y Sensors Loaded" readout & chips
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StartupSplashSequence(
  hapticsEnabled: Boolean,
  voiceAudio: VoiceAudioManager,
  activeSensorCount: Int,
  satellitesVisible: Int,
  durationSec: Int = 7,
  onFinished: () -> Unit,
) {
  val ctx = LocalContext.current
  val settings = LocalTacticalSettings.current
  val effectiveStyle = remember(settings.visualStyle) { sanitizeVisualStyleForSeason(settings.visualStyle) }
  val isHoliday = effectiveStyle.isHolidayTheme() && !settings.nightVisionMode && !settings.sunVisibilityMode
  val holidaySpec = remember(effectiveStyle, isHoliday) { if (isHoliday) holidayDecorationSpec(effectiveStyle) else null }

  // Real Android hardware sensor inventory
  val sensorInventory =
    remember(ctx, activeSensorCount) {
      val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
      val allSensors = runCatching { sm?.getSensorList(Sensor.TYPE_ALL).orEmpty() }.getOrDefault(emptyList())
      val tags = mutableListOf<String>()
      if (sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) tags += "ACCEL"
      if (sm?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) tags += "GYRO"
      if (sm?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) tags += "MAG"
      if (sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null) tags += "9-DOF"
      if (sm?.getDefaultSensor(Sensor.TYPE_PRESSURE) != null) tags += "BARO"
      if (sm?.getDefaultSensor(Sensor.TYPE_LIGHT) != null) tags += "LIGHT"
      if (sm?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null) tags += "STEP"
      tags += "GNSS"
      val total = maxOf(allSensors.size, activeSensorCount, tags.size)
      DetectedSensorInventory(totalHardwareSensors = total, subsystemTags = tags.take(6))
    }

  val totalMs = durationSec.coerceIn(4, 12) * 1000L
  val logoScale = remember { Animatable(0.48f) }
  val logoAlpha = remember { Animatable(0f) }
  val progressAnim = remember { Animatable(0f) }
  val overlayAlpha = remember { Animatable(1f) }
  val bgKenBurnsScale = remember { Animatable(1.04f) }
  var stageIndex by remember { mutableIntStateOf(0) }
  var titleVisible by remember { mutableStateOf(false) }
  var badgesVisible by remember { mutableStateOf(false) }
  var finished by remember { mutableStateOf(false) }

  val infinite = rememberInfiniteTransition(label = "splash_driving_anim")
  // Continuous forward driving road travel loop (0..1)
  val roadTravel by
    infinite.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(920, easing = LinearEasing), RepeatMode.Restart),
      label = "road_travel",
    )
  // Gentle forward steering sway simulating riding down a winding country road
  val steerSway by
    infinite.animateFloat(
      initialValue = -1f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
      label = "steer_sway",
    )
  // Subtle road-suspension vertical bounce
  val roadBounce by
    infinite.animateFloat(
      initialValue = -1f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(520, easing = FastOutSlowInEasing), RepeatMode.Reverse),
      label = "road_bounce",
    )
  val outerSweep by
    infinite.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec = infiniteRepeatable(tween(4400, easing = LinearEasing), RepeatMode.Restart),
      label = "outer_sweep",
    )
  val innerCounterSweep by
    infinite.animateFloat(
      initialValue = 360f,
      targetValue = 0f,
      animationSpec = infiniteRepeatable(tween(6800, easing = LinearEasing), RepeatMode.Restart),
      label = "inner_counter_sweep",
    )
  val pulseGlow by
    infinite.animateFloat(
      initialValue = 0.84f,
      targetValue = 1.16f,
      animationSpec = infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
      label = "pulse_glow",
    )
  val gleamPhase by
    infinite.animateFloat(
      initialValue = -0.35f,
      targetValue = 1.35f,
      animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Restart),
      label = "gleam_phase",
    )
  val holidayTwinklePhase by
    infinite.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(3400, easing = LinearEasing), RepeatMode.Restart),
      label = "holiday_twinkle_splash",
    )
  val eqWave by
    infinite.animateFloat(
      initialValue = 0f,
      targetValue = (2.0 * PI).toFloat(),
      animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
      label = "eq_wave",
    )

  // Dynamic colors strictly matched to current Theme & Holiday palette
  val cs = MaterialTheme.colorScheme
  val primary = cs.primary
  val secondary = holidaySpec?.ornamentColor ?: cs.secondary
  val tertiary = cs.tertiary
  val themeBg = cs.background
  val themeSurface = cs.surface

  val loadedSensorsCount =
    ((sensorInventory.totalHardwareSensors * progressAnim.value).roundToInt())
      .coerceIn(0, sensorInventory.totalHardwareSensors)
  val liveTelemetryArmed = maxOf(activeSensorCount, loadedSensorsCount)

  val bootStages =
    remember(loadedSensorsCount, sensorInventory.totalHardwareSensors, satellitesVisible) {
      listOf(
        "Igniting Rural ERides GO Avionics...",
        "Loading Hardware Sensors ($loadedSensorsCount / ${sensorInventory.totalHardwareSensors} Online)...",
        if (satellitesVisible > 0) "Locking $satellitesVisible GNSS Satellites Overhead..." else "Scanning GPS, GLONASS & Galileo Satellites...",
        "Arming 3D PBR Rig Lab, Rider Radar & Gemini AI Mechanic...",
        "All ${sensorInventory.totalHardwareSensors} Sensors Loaded — Ready to Roll!",
      )
    }

  LaunchedEffect(totalMs) {
    voiceAudio.triggerHapticCue(hapticsEnabled, VoiceAudioManager.HapticCue.BOOT_SWEEP)
    launch {
      bgKenBurnsScale.animateTo(1.22f, animationSpec = tween(totalMs.toInt(), easing = LinearEasing))
    }
    launch {
      logoAlpha.animateTo(1f, animationSpec = tween(640, easing = FastOutSlowInEasing))
    }
    launch {
      logoScale.animateTo(
        1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessVeryLow),
      )
    }

    val holdTailMs = 1050L
    val fadeOutMs = 480L
    val progressDurationMs = (totalMs - holdTailMs - fadeOutMs).coerceAtLeast(2200L)
    val stageStepMs = progressDurationMs / 5L

    launch {
      progressAnim.animateTo(1f, animationSpec = tween(progressDurationMs.toInt(), easing = FastOutSlowInEasing))
    }

    delay(380L)
    titleVisible = true
    delay((stageStepMs - 380L).coerceAtLeast(240L))
    stageIndex = 1
    badgesVisible = true
    delay(stageStepMs)
    stageIndex = 2
    delay(stageStepMs)
    stageIndex = 3
    delay(stageStepMs)
    stageIndex = 4
    voiceAudio.triggerHapticCue(hapticsEnabled, VoiceAudioManager.HapticCue.SUCCESS_DOUBLE)
    delay(holdTailMs)
    if (!finished) {
      overlayAlpha.animateTo(0f, animationSpec = tween(fadeOutMs.toInt()))
      finished = true
      onFinished()
    }
  }

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .alpha(overlayAlpha.value)
        .background(themeBg)
        .testTag("startup_splash_overlay"),
  ) {
    // 1. Upgraded Rural Driving-Horizon Scenic Artwork with forward driving zoom, sway & subtle suspension feel
    Image(
      painter = painterResource(id = R.drawable.img_splash_driving_horizon_1795717550413),
      contentDescription = "Rural ERides GO Driving Horizon Trail Artwork",
      contentScale = ContentScale.Crop,
      modifier =
        Modifier
          .fillMaxSize()
          .graphicsLayer {
            scaleX = bgKenBurnsScale.value
            scaleY = bgKenBurnsScale.value
            translationX = steerSway * 22f
            translationY = roadBounce * 3.5f
          },
    )

    // 2. Dynamic Theme Color Wash & Atmospheric Vignette matching current Theme / Holiday palette
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colorStops =
                arrayOf(
                  0.0f to themeBg.copy(alpha = 0.78f),
                  0.28f to primary.copy(alpha = 0.16f),
                  0.52f to themeBg.copy(alpha = 0.22f),
                  0.78f to themeSurface.copy(alpha = 0.62f),
                  1.0f to themeBg.copy(alpha = 0.94f),
                )
            )
          )
    )

    // 3. Moving Driving-Forward Perspective Road, Headlight Beams, Speed Streaks & Theme Compass Rings
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      val horizonY = h * 0.56f
      val vanishX = w * 0.5f + steerSway * (w * 0.055f)

      // Twin LED PEV Headlight Beam Cones casting forward onto the rushing road
      val headlightPath =
        Path().apply {
          moveTo(vanishX - w * 0.05f, horizonY)
          lineTo(vanishX + w * 0.05f, horizonY)
          lineTo(w * 0.96f, h)
          lineTo(w * 0.04f, h)
          close()
        }
      drawPath(
        path = headlightPath,
        brush =
          Brush.verticalGradient(
            colors =
              listOf(
                primary.copy(alpha = 0.05f),
                primary.copy(alpha = 0.20f * pulseGlow),
                secondary.copy(alpha = 0.14f),
              ),
            startY = horizonY,
            endY = h,
          ),
      )

      // Perspective Road Shoulder Guide Lines converging into the horizon
      drawLine(
        color = primary.copy(alpha = 0.45f),
        start = Offset(vanishX - w * 0.025f, horizonY),
        end = Offset(w * 0.06f, h),
        strokeWidth = 3.2f,
      )
      drawLine(
        color = primary.copy(alpha = 0.45f),
        start = Offset(vanishX + w * 0.025f, horizonY),
        end = Offset(w * 0.94f, h),
        strokeWidth = 3.2f,
      )

      // Rushing perspective center dashes & roadside fence/milepost markers creating crisp forward driving motion
      val numSegments = 9
      for (i in 0 until numSegments) {
        val rawT = (i.toFloat() / numSegments + roadTravel) % 1f
        val depth = rawT.pow(2.35f) // nonlinear perspective acceleration toward camera
        val depthNext = ((rawT + 0.045f).coerceAtMost(1f)).pow(2.35f)
        val y1 = horizonY + depth * (h - horizonY)
        val y2 = horizonY + depthNext * (h - horizonY)
        val segAlpha = (rawT * 1.15f).coerceIn(0f, 0.88f) * (1f - (rawT - 0.85f).coerceAtLeast(0f) * 5f).coerceIn(0f, 1f)

        // Rushing glowing center lane dash
        val dashW = 2.2f + depth * 9.5f
        val cx1 = vanishX + (w * 0.5f - vanishX) * depth
        val cx2 = vanishX + (w * 0.5f - vanishX) * depthNext
        drawLine(
          color = primary.copy(alpha = segAlpha * 0.85f),
          start = Offset(cx1, y1),
          end = Offset(cx2, y2),
          strokeWidth = dashW,
          cap = StrokeCap.Round,
        )

        // Passing left & right roadside trail reflectors / fence posts flying past
        val halfRoadW = w * (0.03f + depth * 0.47f)
        val postH = 8f + depth * 42f
        val postW = 2f + depth * 5.5f
        val leftX = cx1 - halfRoadW
        val rightX = cx1 + halfRoadW

        drawLine(
          color = secondary.copy(alpha = segAlpha * 0.72f),
          start = Offset(leftX, y1),
          end = Offset(leftX - depth * 6f, y1 - postH),
          strokeWidth = postW,
          cap = StrokeCap.Round,
        )
        drawCircle(
          color = primary.copy(alpha = segAlpha * 0.95f),
          radius = 2f + depth * 4.5f,
          center = Offset(leftX - depth * 4f, y1 - postH * 0.82f),
        )
        drawLine(
          color = secondary.copy(alpha = segAlpha * 0.72f),
          start = Offset(rightX, y1),
          end = Offset(rightX + depth * 6f, y1 - postH),
          strokeWidth = postW,
          cap = StrokeCap.Round,
        )
        drawCircle(
          color = primary.copy(alpha = segAlpha * 0.95f),
          radius = 2f + depth * 4.5f,
          center = Offset(rightX + depth * 4f, y1 - postH * 0.82f),
        )
      }

      // Radial peripheral driving speed-wind streaks
      for (s in 0 until 14) {
        val side = if (s % 2 == 0) -1f else 1f
        val phase = (roadTravel + s * 0.17f) % 1f
        val angle = (if (side < 0) 2.35f else 0.78f) + ((s % 5) - 2) * 0.14f
        val rStart = w * (0.18f + phase * 0.45f)
        val rEnd = rStart + w * (0.08f + phase * 0.14f)
        val sx1 = vanishX + cos(angle) * rStart
        val sy1 = horizonY + sin(angle) * rStart * 0.65f
        val sx2 = vanishX + cos(angle) * rEnd
        val sy2 = horizonY + sin(angle) * rEnd * 0.65f
        val a = sin(phase * PI.toFloat()).coerceIn(0f, 1f) * 0.34f
        drawLine(
          color = (if (s % 3 == 0) secondary else primary).copy(alpha = a),
          start = Offset(sx1, sy1),
          end = Offset(sx2, sy2),
          strokeWidth = 1.8f + phase * 2.2f,
          cap = StrokeCap.Round,
        )
      }

      // Theme-Matched Aurora Glow & Counter-Rotating Compass Rings framing the logo medallion
      val emblemCenter = Offset(w / 2f, h * 0.30f)
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                primary.copy(alpha = 0.32f),
                secondary.copy(alpha = 0.16f),
                Color.Transparent,
              ),
            center = emblemCenter,
            radius = (w * 0.42f) * pulseGlow,
          ),
        radius = (w * 0.42f) * pulseGlow,
        center = emblemCenter,
      )

      val outerRadius = (size.minDimension * 0.28f).coerceIn(102f, 174f)
      val innerRadius = outerRadius * 0.85f
      for (deg in 0 until 360 step 10) {
        val rad = Math.toRadians(deg.toDouble())
        val major = deg % 30 == 0
        val rStart = outerRadius + if (major) 5f else 9f
        val rEnd = outerRadius + 16f
        drawLine(
          color = (if (major) primary else Color.White).copy(alpha = if (major) 0.72f else 0.26f),
          start = Offset(emblemCenter.x + cos(rad).toFloat() * rStart, emblemCenter.y + sin(rad).toFloat() * rStart),
          end = Offset(emblemCenter.x + cos(rad).toFloat() * rEnd, emblemCenter.y + sin(rad).toFloat() * rEnd),
          strokeWidth = if (major) 2.4f else 1.2f,
          cap = StrokeCap.Round,
        )
      }
      drawArc(
        color = primary.copy(alpha = 0.94f),
        startAngle = outerSweep,
        sweepAngle = 115f,
        useCenter = false,
        topLeft = Offset(emblemCenter.x - outerRadius, emblemCenter.y - outerRadius),
        size = Size(outerRadius * 2f, outerRadius * 2f),
        style = Stroke(width = 4.4f, cap = StrokeCap.Round),
      )
      drawArc(
        color = secondary.copy(alpha = 0.86f),
        startAngle = outerSweep + 185f,
        sweepAngle = 62f,
        useCenter = false,
        topLeft = Offset(emblemCenter.x - outerRadius, emblemCenter.y - outerRadius),
        size = Size(outerRadius * 2f, outerRadius * 2f),
        style = Stroke(width = 3.4f, cap = StrokeCap.Round),
      )
      drawArc(
        color = tertiary.copy(alpha = 0.82f),
        startAngle = innerCounterSweep,
        sweepAngle = 88f,
        useCenter = false,
        topLeft = Offset(emblemCenter.x - innerRadius, emblemCenter.y - innerRadius),
        size = Size(innerRadius * 2f, innerRadius * 2f),
        style = Stroke(width = 3.0f, cap = StrokeCap.Round),
      )
    }

    // 4. Active Holiday Background Particles & Ornaments (Snow, Leaves, Fireworks, Hearts, Clovers, Stars)
    if (isHoliday) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        drawHolidayBackgroundArt(
          mode = effectiveStyle,
          accent = primary,
          secondary = secondary,
          twinklePhase = holidayTwinklePhase,
        )
      }
    }

    // 5. Foreground HUD Layout (status bar & nav bar safe)
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
          .padding(horizontal = 22.dp, vertical = 14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      if (isHoliday) {
        Spacer(Modifier.height(18.dp))
      }

      // Top bar: Version + Active Theme/Holiday Badge + Skip Intro
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          Surface(
            color = themeSurface.copy(alpha = 0.82f),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, primary.copy(alpha = 0.65f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = primary, modifier = Modifier.size(14.dp))
              Text(
                text = "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = cs.onSurface,
              )
            }
          }
          Surface(
            color = secondary.copy(alpha = 0.22f),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, secondary.copy(alpha = 0.6f)),
          ) {
            Text(
              text = effectiveStyle.name.replace("HOLIDAY_", "").replace("RURAL_", "").replace("_", " "),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
          }
        }

        Surface(
          color = themeSurface.copy(alpha = 0.78f),
          shape = RoundedCornerShape(50),
          border = BorderStroke(1.dp, cs.onSurface.copy(alpha = 0.28f)),
        ) {
          TextButton(
            onClick = {
              if (!finished) {
                finished = true
                voiceAudio.triggerHapticCue(hapticsEnabled, VoiceAudioManager.HapticCue.LIGHT_TICK)
                onFinished()
              }
            },
            modifier = Modifier.testTag("skip_startup_splash_button"),
          ) {
            Text("Skip Intro", color = cs.onSurface, fontWeight = FontWeight.SemiBold)
          }
        }
      }

      Spacer(Modifier.weight(0.32f))

      // Center Brand Medallion framed inside theme-matched compass rings
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier
            .size(158.dp)
            .scale(logoScale.value)
            .alpha(logoAlpha.value),
      ) {
        Surface(
          shape = CircleShape,
          color = themeBg,
          border = BorderStroke(3.dp, primary.copy(alpha = 0.95f)),
          shadowElevation = 28.dp,
          modifier = Modifier.size(148.dp),
        ) {
          Box(Modifier.fillMaxSize().clip(CircleShape)) {
            Image(
              painter = painterResource(id = R.drawable.rural_erides_icon),
              contentDescription = "Rural ERides GO Logo",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
              val bandW = size.width * 0.36f
              val startX = gleamPhase * (size.width + bandW) - bandW
              drawRect(
                brush =
                  Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.38f), Color.Transparent),
                    start = Offset(startX, 0f),
                    end = Offset(startX + bandW, size.height),
                  )
              )
            }
          }
        }
      }

      Spacer(Modifier.height(18.dp))

      // Glassmorphic Title + Live Hardware Sensor Counter & Armed Sensor Chips
      AnimatedVisibility(
        visible = titleVisible,
        enter = fadeIn(tween(520)),
        exit = fadeOut(),
      ) {
        Surface(
          color = themeSurface.copy(alpha = 0.80f),
          shape = RoundedCornerShape(22.dp),
          border = BorderStroke(1.2.dp, (holidaySpec?.ornamentColor ?: primary).copy(alpha = 0.55f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(
              text = "RURAL ERIDES GO",
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.Black,
              letterSpacing = 2.4.sp,
              color = cs.onSurface,
              textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
              text = "LIVE GPS • 9-DOF SENSORS • 3D LAB • GEMINI AI",
              style = MaterialTheme.typography.labelSmall,
              letterSpacing = 1.1.sp,
              color = primary,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center,
            )

            AnimatedVisibility(visible = badgesVisible, enter = fadeIn(tween(420))) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 10.dp),
              ) {
                Row(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Surface(
                    color = primary.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, primary.copy(alpha = 0.62f)),
                    modifier = Modifier.testTag("splash_sensors_loaded_badge"),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                      Icon(Icons.Default.Sensors, contentDescription = null, tint = primary, modifier = Modifier.size(14.dp))
                      Text(
                        text = "$liveTelemetryArmed / ${sensorInventory.totalHardwareSensors} Sensors Loaded",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                      )
                    }
                  }
                  Surface(
                    color = secondary.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, secondary.copy(alpha = 0.58f)),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                      Icon(Icons.Default.SatelliteAlt, contentDescription = null, tint = secondary, modifier = Modifier.size(14.dp))
                      Text(
                        text = if (satellitesVisible > 0) "$satellitesVisible Sats Lock" else "GNSS Ready",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurface,
                        fontWeight = FontWeight.Bold,
                      )
                    }
                  }
                }

                // Live detected hardware sensor chips lighting up progressively
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  sensorInventory.subsystemTags.forEachIndexed { idx, tag ->
                    val armed = progressAnim.value >= (idx.toFloat() / sensorInventory.subsystemTags.size.coerceAtLeast(1))
                    Surface(
                      color = if (armed) primary.copy(alpha = 0.22f) else themeBg.copy(alpha = 0.45f),
                      shape = RoundedCornerShape(8.dp),
                      border = BorderStroke(0.9.dp, if (armed) primary.copy(alpha = 0.72f) else cs.outline.copy(alpha = 0.35f)),
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                      ) {
                        if (armed) {
                          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = primary, modifier = Modifier.size(10.dp))
                        }
                        Text(
                          text = tag,
                          fontSize = 10.sp,
                          fontWeight = FontWeight.Bold,
                          color = if (armed) cs.onSurface else cs.onSurface.copy(alpha = 0.55f),
                        )
                      }
                    }
                  }
                }

                // Live 7-bar theme-matched equalizer
                Row(
                  horizontalArrangement = Arrangement.spacedBy(5.dp),
                  verticalAlignment = Alignment.Bottom,
                  modifier = Modifier.height(16.dp),
                ) {
                  for (bar in 0 until 7) {
                    val hFrac = (0.30f + 0.70f * ((sin(eqWave + bar * 0.78f) + 1f) / 2f)).coerceIn(0.24f, 1f)
                    Box(
                      modifier =
                        Modifier
                          .width(5.dp)
                          .height((16f * hFrac).dp)
                          .clip(RoundedCornerShape(3.dp))
                          .background(if (bar % 2 == 0) primary else secondary)
                    )
                  }
                }
              }
            }
          }
        }
      }

      Spacer(Modifier.weight(0.68f))

      // Bottom boot progress panel themed to match current palette
      Surface(
        color = themeSurface.copy(alpha = 0.86f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, primary.copy(alpha = 0.36f)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          LinearProgressIndicator(
            progress = { progressAnim.value },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50)),
            color = primary,
            trackColor = cs.onSurface.copy(alpha = 0.16f),
          )
          Spacer(Modifier.height(9.dp))
          Text(
            text = bootStages.getOrElse(stageIndex) { bootStages.last() },
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurface,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
          )
          Spacer(Modifier.height(4.dp))
          Text(
            text = "Created by Lord Bradley Callison • KEEP IT RURAL Y'ALL",
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 0.7.sp,
            color = cs.onSurface.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
          )
        }
      }
    }

    // 6. Active Holiday Top Canopy (garlands, twinkling string lights & hanging ornaments)
    if (isHoliday) {
      Canvas(
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .fillMaxWidth()
            .height(34.dp),
      ) {
        drawHolidayTopCanopy(mode = effectiveStyle, twinklePhase = holidayTwinklePhase)
      }
    }
  }
}
