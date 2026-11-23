package com.example.ui.screens

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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.hardware.VoiceAudioManager
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen cinematic startup showcase featuring bespoke rural electric trail artwork
 * with live Ken Burns parallax motion, drifting golden/neon embers, aurora breathing glow,
 * counter-rotating precision compass rings framing the Rural ERides GO brand logo,
 * diagonal glossy emblem sheen, live 7-bar power equalizer, and generous viewing duration (default 7s).
 */
@Composable
fun StartupSplashSequence(
  hapticsEnabled: Boolean,
  voiceAudio: VoiceAudioManager,
  activeSensorCount: Int,
  satellitesVisible: Int,
  durationSec: Int = 7,
  onFinished: () -> Unit,
) {
  val totalMs = durationSec.coerceIn(4, 12) * 1000L
  val logoScale = remember { Animatable(0.45f) }
  val logoAlpha = remember { Animatable(0f) }
  val progressAnim = remember { Animatable(0f) }
  val overlayAlpha = remember { Animatable(1f) }
  val bgKenBurnsScale = remember { Animatable(1.02f) }
  var stageIndex by remember { mutableIntStateOf(0) }
  var titleVisible by remember { mutableStateOf(false) }
  var badgesVisible by remember { mutableStateOf(false) }
  var finished by remember { mutableStateOf(false) }

  val infinite = rememberInfiniteTransition(label = "splash_artwork_anim")
  val outerSweep by
    infinite.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec = infiniteRepeatable(tween(4600, easing = LinearEasing), RepeatMode.Restart),
      label = "outer_sweep",
    )
  val innerCounterSweep by
    infinite.animateFloat(
      initialValue = 360f,
      targetValue = 0f,
      animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart),
      label = "inner_counter_sweep",
    )
  val pulseGlow by
    infinite.animateFloat(
      initialValue = 0.80f,
      targetValue = 1.18f,
      animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
      label = "pulse_glow",
    )
  val bgDriftX by
    infinite.animateFloat(
      initialValue = -16f,
      targetValue = 16f,
      animationSpec = infiniteRepeatable(tween(8000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
      label = "bg_drift_x",
    )
  val gleamPhase by
    infinite.animateFloat(
      initialValue = -0.35f,
      targetValue = 1.35f,
      animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Restart),
      label = "gleam_phase",
    )
  val emberPhase by
    infinite.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(6200, easing = LinearEasing), RepeatMode.Restart),
      label = "ember_phase",
    )
  val eqWave by
    infinite.animateFloat(
      initialValue = 0f,
      targetValue = (2.0 * PI).toFloat(),
      animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
      label = "eq_wave",
    )

  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary
  val tertiary = MaterialTheme.colorScheme.tertiary
  val amberGold = Color(0xFFFFB703)
  val auroraTeal = Color(0xFF00F0FF)

  val bootStages =
    remember(activeSensorCount, satellitesVisible) {
      listOf(
        "Welcome to Rural ERides GO...",
        "Calibrating 9-DOF Tilt, Compass & Barometer ($activeSensorCount live sensors)...",
        if (satellitesVisible > 0) "Locking $satellitesVisible GNSS Satellites Overhead..." else "Scanning GPS, GLONASS & Galileo Satellites...",
        "Warming Up Live AR Camera, AI Helper & Repair Shop...",
        "All Systems Green — Have a Safe & Scenic Ride!",
      )
    }

  LaunchedEffect(totalMs) {
    voiceAudio.triggerHapticCue(hapticsEnabled, VoiceAudioManager.HapticCue.BOOT_SWEEP)
    launch {
      bgKenBurnsScale.animateTo(1.13f, animationSpec = tween(totalMs.toInt(), easing = FastOutSlowInEasing))
    }
    launch {
      logoAlpha.animateTo(1f, animationSpec = tween(680, easing = FastOutSlowInEasing))
    }
    launch {
      logoScale.animateTo(
        1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessVeryLow),
      )
    }

    val holdTailMs = 1100L
    val fadeOutMs = 520L
    val progressDurationMs = (totalMs - holdTailMs - fadeOutMs).coerceAtLeast(2400L)
    val stageStepMs = progressDurationMs / 5L

    launch {
      progressAnim.animateTo(1f, animationSpec = tween(progressDurationMs.toInt(), easing = FastOutSlowInEasing))
    }

    delay(420L)
    titleVisible = true
    delay((stageStepMs - 420L).coerceAtLeast(280L))
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
        .background(Color(0xFF050B10))
        .testTag("startup_splash_overlay"),
  ) {
    // 1. Bespoke Rural Electric Trail Artwork with animated Ken Burns zoom & gentle drift
    Image(
      painter = painterResource(id = R.drawable.img_splash_rural_trail_1795300562169),
      contentDescription = "Rural ERides GO Scenic Electric Trail Artwork",
      contentScale = ContentScale.Crop,
      modifier =
        Modifier
          .fillMaxSize()
          .graphicsLayer {
            scaleX = bgKenBurnsScale.value
            scaleY = bgKenBurnsScale.value
            translationX = bgDriftX
          },
    )

    // 2. Atmospheric cinema vignette & aurora tint overlay for crisp foreground readability
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colorStops =
                arrayOf(
                  0.0f to Color(0x9903090E),
                  0.30f to Color(0x38051015),
                  0.66f to Color(0x4A061412),
                  1.0f to Color(0xE0040B0D),
                )
            )
          )
    )

    // 3. Live Animated Overlay Canvas: breathing aurora aura, twinkling stars, drifting firefly embers, compass rings
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      val emblemCenter = Offset(w / 2f, h * 0.345f)

      // Aurora glow halo pulsing behind the logo medallion
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                primary.copy(alpha = 0.30f),
                auroraTeal.copy(alpha = 0.14f),
                Color.Transparent,
              ),
            center = emblemCenter,
            radius = (w * 0.44f) * pulseGlow,
          ),
        radius = (w * 0.44f) * pulseGlow,
        center = emblemCenter,
      )

      // Animated breathing glow along the winding lower trail
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                auroraTeal.copy(alpha = 0.16f * pulseGlow),
                primary.copy(alpha = 0.08f),
                Color.Transparent,
              ),
            center = Offset(w * 0.50f, h * 0.78f),
            radius = w * 0.52f,
          ),
        radius = w * 0.52f,
        center = Offset(w * 0.50f, h * 0.78f),
      )

      // Twinkling upper-sky stars
      for (i in 0 until 26) {
        val sx = ((i * 197 + 41) % 100) / 100f * w
        val sy = ((i * 89 + 13) % 34) / 100f * h
        val twinkle = 0.18f + 0.65f * ((sin(eqWave + i * 0.85f) + 1f) / 2f)
        drawCircle(
          color = Color.White.copy(alpha = twinkle),
          radius = if (i % 5 == 0) 2.5f else 1.4f,
          center = Offset(sx, sy),
        )
      }

      // 24 drifting warm golden firefly & neon trail embers rising smoothly
      for (p in 0 until 24) {
        val seedX = ((p * 73 + 17) % 100) / 100f
        val progressY = (emberPhase + p * 0.11f) % 1f
        val px = seedX * w + sin(progressY * 2f * PI.toFloat() + p * 1.1f) * 30f
        val py = h * 0.94f - progressY * (h * 0.82f)
        val alpha = sin(progressY * PI.toFloat()).coerceIn(0f, 1f) * 0.88f
        val emberColor =
          when (p % 3) {
            0 -> amberGold
            1 -> primary
            else -> auroraTeal
          }
        drawCircle(
          color = emberColor.copy(alpha = alpha * 0.34f),
          radius = 9f,
          center = Offset(px, py),
        )
        drawCircle(
          color = emberColor.copy(alpha = alpha),
          radius = 2.8f,
          center = Offset(px, py),
        )
      }

      // Precision compass ticks + counter-rotating glowing arcs centered on the logo
      val outerRadius = (size.minDimension * 0.32f).coerceIn(112f, 192f)
      val innerRadius = outerRadius * 0.85f

      for (deg in 0 until 360 step 10) {
        val rad = Math.toRadians(deg.toDouble())
        val major = deg % 30 == 0
        val rStart = outerRadius + if (major) 6f else 10f
        val rEnd = outerRadius + 18f
        drawLine(
          color = (if (major) primary else Color.White).copy(alpha = if (major) 0.68f else 0.24f),
          start = Offset(emblemCenter.x + cos(rad).toFloat() * rStart, emblemCenter.y + sin(rad).toFloat() * rStart),
          end = Offset(emblemCenter.x + cos(rad).toFloat() * rEnd, emblemCenter.y + sin(rad).toFloat() * rEnd),
          strokeWidth = if (major) 2.5f else 1.2f,
          cap = StrokeCap.Round,
        )
      }

      drawArc(
        color = primary.copy(alpha = 0.92f),
        startAngle = outerSweep,
        sweepAngle = 110f,
        useCenter = false,
        topLeft = Offset(emblemCenter.x - outerRadius, emblemCenter.y - outerRadius),
        size = Size(outerRadius * 2f, outerRadius * 2f),
        style = Stroke(width = 4.6f, cap = StrokeCap.Round),
      )
      drawArc(
        color = amberGold.copy(alpha = 0.82f),
        startAngle = outerSweep + 185f,
        sweepAngle = 60f,
        useCenter = false,
        topLeft = Offset(emblemCenter.x - outerRadius, emblemCenter.y - outerRadius),
        size = Size(outerRadius * 2f, outerRadius * 2f),
        style = Stroke(width = 3.4f, cap = StrokeCap.Round),
      )
      drawArc(
        color = auroraTeal.copy(alpha = 0.85f),
        startAngle = innerCounterSweep,
        sweepAngle = 86f,
        useCenter = false,
        topLeft = Offset(emblemCenter.x - innerRadius, emblemCenter.y - innerRadius),
        size = Size(innerRadius * 2f, innerRadius * 2f),
        style = Stroke(width = 3.0f, cap = StrokeCap.Round),
      )
      drawArc(
        color = tertiary.copy(alpha = 0.60f),
        startAngle = innerCounterSweep + 165f,
        sweepAngle = 55f,
        useCenter = false,
        topLeft = Offset(emblemCenter.x - innerRadius, emblemCenter.y - innerRadius),
        size = Size(innerRadius * 2f, innerRadius * 2f),
        style = Stroke(width = 2.4f, cap = StrokeCap.Round),
      )
    }

    // 4. Foreground layout (status bar & nav bar safe)
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
          .padding(horizontal = 24.dp, vertical = 14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Top row: Version badge + Skip Intro button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color = Color(0x99061216),
          shape = RoundedCornerShape(50),
          border = BorderStroke(1.dp, primary.copy(alpha = 0.55f)),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = primary, modifier = Modifier.size(14.dp))
            Text(
              text = "v${BuildConfig.VERSION_NAME}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = Color.White,
            )
          }
        }

        Surface(
          color = Color(0x88061216),
          shape = RoundedCornerShape(50),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
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
            Text("Skip Intro", color = Color.White, fontWeight = FontWeight.SemiBold)
          }
        }
      }

      Spacer(Modifier.weight(0.55f))

      // Center Brand Medallion sitting cleanly inside the celestial circle
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier
            .size(176.dp)
            .scale(logoScale.value)
            .alpha(logoAlpha.value),
      ) {
        Surface(
          shape = CircleShape,
          color = Color(0xFF071210),
          border = BorderStroke(3.dp, primary.copy(alpha = 0.95f)),
          shadowElevation = 28.dp,
          modifier = Modifier.size(166.dp),
        ) {
          Box(Modifier.fillMaxSize().clip(CircleShape)) {
            Image(
              painter = painterResource(id = R.drawable.rural_erides_icon),
              contentDescription = "Rural ERides GO Logo",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
            // Sweeping glossy gleam across the brand emblem
            Canvas(modifier = Modifier.fillMaxSize()) {
              val bandW = size.width * 0.36f
              val startX = gleamPhase * (size.width + bandW) - bandW
              drawRect(
                brush =
                  Brush.linearGradient(
                    colors =
                      listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.36f),
                        Color.Transparent,
                      ),
                    start = Offset(startX, 0f),
                    end = Offset(startX + bandW, size.height),
                  )
              )
            }
          }
        }
      }

      Spacer(Modifier.height(24.dp))

      // Glassmorphic Title & Live Telemetry Card over the scenic trail
      AnimatedVisibility(
        visible = titleVisible,
        enter = fadeIn(tween(580)),
        exit = fadeOut(),
      ) {
        Surface(
          color = Color(0xA6061217),
          shape = RoundedCornerShape(22.dp),
          border = BorderStroke(1.dp, primary.copy(alpha = 0.42f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(
              text = "RURAL ERIDES GO",
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.Black,
              letterSpacing = 2.6.sp,
              color = Color.White,
              textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(5.dp))
            Text(
              text = "LIVE GPS • 9-DOF SENSORS • AR CAMERA • AI HELPER",
              style = MaterialTheme.typography.labelMedium,
              letterSpacing = 1.1.sp,
              color = primary,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center,
            )
            AnimatedVisibility(visible = badgesVisible, enter = fadeIn(tween(480))) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 12.dp),
              ) {
                Row(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Surface(
                    color = primary.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, primary.copy(alpha = 0.50f)),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      Icon(Icons.Default.Sensors, contentDescription = null, tint = primary, modifier = Modifier.size(14.dp))
                      Text(
                        text = "$activeSensorCount Live Sensors",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                      )
                    }
                  }
                  Surface(
                    color = auroraTeal.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, auroraTeal.copy(alpha = 0.50f)),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      Icon(Icons.Default.SatelliteAlt, contentDescription = null, tint = auroraTeal, modifier = Modifier.size(14.dp))
                      Text(
                        text = if (satellitesVisible > 0) "$satellitesVisible Sats Ready" else "GNSS Scanning",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                      )
                    }
                  }
                }

                // Live 7-bar power/signal equalizer
                Row(
                  horizontalArrangement = Arrangement.spacedBy(5.dp),
                  verticalAlignment = Alignment.Bottom,
                  modifier = Modifier.height(20.dp),
                ) {
                  for (bar in 0 until 7) {
                    val hFrac = (0.32f + 0.68f * ((sin(eqWave + bar * 0.78f) + 1f) / 2f)).coerceIn(0.25f, 1f)
                    Box(
                      modifier =
                        Modifier
                          .width(5.dp)
                          .height((20f * hFrac).dp)
                          .clip(RoundedCornerShape(3.dp))
                          .background(if (bar % 2 == 0) primary else auroraTeal)
                    )
                  }
                }
              }
            }
          }
        }
      }

      Spacer(Modifier.weight(1.0f))

      // Bottom boot progress panel over the glowing rural trail
      Surface(
        color = Color(0xBF050F14),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          LinearProgressIndicator(
            progress = { progressAnim.value },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50)),
            color = primary,
            trackColor = Color.White.copy(alpha = 0.18f),
          )
          Spacer(Modifier.height(10.dp))
          Text(
            text = bootStages.getOrElse(stageIndex) { bootStages.last() },
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
          )
          Spacer(Modifier.height(6.dp))
          Text(
            text = "Created by Lord Bradley Callison",
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 0.8.sp,
            color = Color.White.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
          )
        }
      }
    }
  }
}
