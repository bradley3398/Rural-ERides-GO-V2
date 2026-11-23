package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hardware.VoiceAudioManager
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private enum class VoltGamePhase { TITLE, PLAYING, PAUSED, GAME_OVER }

private enum class TrackEntityKind { HAY_BALE, CREEK_WASHOUT, TRACTOR, VOLT_CELL, SOLAR_ORB }

private data class TrackEntity(
  val id: Int,
  val lane: Int, // -1, 0, +1
  var z: Float, // 0f = horizon, 1f = player line
  val kind: TrackEntityKind,
  var collected: Boolean = false,
)

private data class GameParticle(
  var x: Float,
  var y: Float,
  var vx: Float,
  var vy: Float,
  var life: Float,
  val color: Color,
  val radius: Float,
)

private data class RoadIntersection(
  val id: Int,
  var z: Float,
  val leftStreet: String,
  val rightStreet: String,
  var turned: Boolean = false,
)

private enum class VoltMissionType(
  val title: String,
  val description: String,
  val target: Int,
  val rewardPts: Int,
) {
  COLLECT_CELLS("Supply Run", "Grab 21700 Volt Cells", 5, 500),
  EXPLORE_STREETS("Street Explorer", "Turn onto new streets", 3, 650),
  HOP_HAZARDS("Trail Hopper", "Hop or smash hazards", 4, 550),
  BOOST_BLITZ("Overdrive Blitz", "Fire 72V Volt Boosts", 2, 600),
  PATROL_ORBS("Solar Relay", "Collect Solar Orbs", 3, 700),
}

private val STREET_NAMES = listOf(
  "Oak Ridge Pkwy",
  "Cedar Creek Rd",
  "Voltage Way",
  "Pinecrest Blvd",
  "Old Mill Ln",
  "Covered Bridge Rd",
  "Gravel Switch Ave",
  "Valley View Pike",
  "Copperhead Trail",
  "Redbarn Cutoff",
)

private val COMPASS_HEADINGS = listOf("N", "E", "S", "W")

/**
 * Full-Viewport 60fps Unreal-Styled Rural PEV Arcade Runner: "GRAVEL VOLT RUNNER".
 * Features twilight atmospheric volumetric sky, wet specular road & streetlamp cones,
 * PEV dual-projector headlight cone & turn signals, open-street 4-way intersections
 * ("turn on any street"), and a rotating live Mission system.
 */
@Composable
fun GravelVoltGameScreen(
  callsign: String,
  vehicleName: String,
  voiceAudio: VoiceAudioManager,
  onShareHighScore: (score: Int, miles: Double) -> Unit = { _, _ -> },
  onBack: () -> Unit,
) {
  val haptics = LocalHapticFeedback.current
  var phase by remember { mutableStateOf(VoltGamePhase.TITLE) }
  var muted by remember { mutableStateOf(false) }

  var score by remember { mutableIntStateOf(0) }
  var bestScore by remember { mutableIntStateOf(0) }
  var distanceMiles by remember { mutableFloatStateOf(0f) }
  var batteryPct by remember { mutableFloatStateOf(100f) }
  var comboCount by remember { mutableIntStateOf(0) }
  var lives by remember { mutableIntStateOf(3) }

  // Open-Street Navigation & Turn-Any-Street State
  var currentStreet by remember { mutableStateOf(STREET_NAMES[0]) }
  var compassIdx by remember { mutableIntStateOf(0) } // 0=N,1=E,2=S,3=W
  var streetsExplored by remember { mutableIntStateOf(1) }
  var intersectionTimer by remember { mutableFloatStateOf(5.5f) }
  var nextIntersectionId by remember { mutableIntStateOf(1) }
  var turnBankDeg by remember { mutableFloatStateOf(0f) }
  var turnSignalTimer by remember { mutableFloatStateOf(0f) }
  var turnSignalDir by remember { mutableIntStateOf(0) } // -1 left, 1 right
  var bannerMessage by remember { mutableStateOf("") }
  var bannerTimer by remember { mutableFloatStateOf(0f) }

  // Interactive Mission State
  var selectedStartMissionIdx by remember { mutableIntStateOf(0) }
  var missionIdx by remember { mutableIntStateOf(0) }
  var missionProgress by remember { mutableIntStateOf(0) }
  var missionsCompleted by remember { mutableIntStateOf(0) }

  // Player kinematics
  var targetLane by remember { mutableIntStateOf(0) } // -1, 0, 1
  var lanePos by remember { mutableFloatStateOf(0f) }
  var jumpHeight by remember { mutableFloatStateOf(0f) }
  var jumpVel by remember { mutableFloatStateOf(0f) }
  var boostTimer by remember { mutableFloatStateOf(0f) }
  var shakeTimer by remember { mutableFloatStateOf(0f) }
  var hitstopTimer by remember { mutableFloatStateOf(0f) }
  var hitFlash by remember { mutableFloatStateOf(0f) }
  var roadScroll by remember { mutableFloatStateOf(0f) }
  var worldClock by remember { mutableFloatStateOf(0f) }
  var spawnTimer by remember { mutableFloatStateOf(0.8f) }
  var nextEntityId by remember { mutableIntStateOf(1) }

  val entities = remember { mutableStateListOf<TrackEntity>() }
  val intersections = remember { mutableStateListOf<RoadIntersection>() }
  val particles = remember { mutableStateListOf<GameParticle>() }

  BackHandler {
    when (phase) {
      VoltGamePhase.PLAYING -> phase = VoltGamePhase.PAUSED
      else -> onBack()
    }
  }

  val activeMission = VoltMissionType.entries[missionIdx % VoltMissionType.entries.size]

  fun advanceMissionProgress(type: VoltMissionType, amount: Int = 1) {
    if (activeMission != type) return
    val next = missionProgress + amount
    if (next >= activeMission.target) {
      score += activeMission.rewardPts
      batteryPct = (batteryPct + 15f).coerceAtMost(100f)
      missionsCompleted++
      bannerMessage = "MISSION COMPLETE: ${activeMission.title} +${activeMission.rewardPts} PTS!"
      bannerTimer = 2.6f
      missionIdx = (missionIdx + 1) % VoltMissionType.entries.size
      missionProgress = 0
      if (!muted) voiceAudio.playTouchSound(true, "Country Acoustic", VoiceAudioManager.TouchCue.ACTION_CONFIRM, 95)
    } else {
      missionProgress = next
    }
  }

  fun resetRun() {
    score = 0
    distanceMiles = 0f
    batteryPct = 100f
    comboCount = 0
    lives = 3
    targetLane = 0
    lanePos = 0f
    jumpHeight = 0f
    jumpVel = 0f
    boostTimer = 0f
    shakeTimer = 0f
    hitstopTimer = 0f
    hitFlash = 0f
    spawnTimer = 0.65f
    currentStreet = STREET_NAMES[0]
    compassIdx = 0
    streetsExplored = 1
    intersectionTimer = 4.5f
    turnBankDeg = 0f
    turnSignalTimer = 0f
    turnSignalDir = 0
    missionIdx = selectedStartMissionIdx
    missionProgress = 0
    missionsCompleted = 0
    bannerMessage = "ACTIVE MISSION: ${VoltMissionType.entries[selectedStartMissionIdx].title}"
    bannerTimer = 2.2f
    entities.clear()
    intersections.clear()
    particles.clear()
    phase = VoltGamePhase.PLAYING
    if (!muted) {
      voiceAudio.playTouchSound(true, "Country Acoustic", VoiceAudioManager.TouchCue.RIDE_START, 85)
    }
  }

  fun spawnBurst(nx: Float, ny: Float, color: Color, count: Int = 12) {
    if (particles.size > 90) {
      repeat(count.coerceAtMost(particles.size)) { particles.removeAt(0) }
    }
    repeat(count) {
      val ang = Random.nextFloat() * 2f * PI.toFloat()
      val spd = 0.14f + Random.nextFloat() * 0.44f
      particles +=
        GameParticle(
          x = nx,
          y = ny,
          vx = cos(ang) * spd,
          vy = sin(ang) * spd - 0.16f,
          life = 1f,
          color = color,
          radius = 3f + Random.nextFloat() * 5.5f,
        )
    }
  }

  fun steer(delta: Int) {
    if (phase != VoltGamePhase.PLAYING) return
    val next = (targetLane + delta).coerceIn(-1, 1)
    turnSignalDir = delta
    turnSignalTimer = 0.85f
    if (next != targetLane) {
      targetLane = next
      haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      if (!muted) voiceAudio.playTouchSound(true, "Soft Tactile", VoiceAudioManager.TouchCue.TAB_SWITCH, 65)
    }
  }

  val activeTurnIntersection = intersections.firstOrNull { !it.turned && it.z in 0.35f..0.96f }

  fun turnOntoStreet(direction: Int) {
    if (phase != VoltGamePhase.PLAYING) return
    val inter = activeTurnIntersection ?: return
    inter.turned = true
    val chosenStreet = if (direction < 0) inter.leftStreet else inter.rightStreet
    currentStreet = chosenStreet
    compassIdx = (compassIdx + (if (direction < 0) 3 else 1)) % 4
    streetsExplored++
    turnBankDeg = direction * 18f
    turnSignalDir = direction
    turnSignalTimer = 1.2f
    score += 200
    comboCount++
    bannerMessage = "TURNED ${if (direction < 0) "LEFT" else "RIGHT"} -> $chosenStreet (+200)"
    bannerTimer = 2.0f
    // Clear immediate hazards in front after turning onto new street
    entities.removeAll { it.z > 0.35f }
    spawnBurst(0.5f + lanePos * 0.22f, 0.78f, Color(0xFF38BDF8), 16)
    advanceMissionProgress(VoltMissionType.EXPLORE_STREETS)
    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    if (!muted) voiceAudio.playTouchSound(true, "Studio Glass", VoiceAudioManager.TouchCue.ACTION_CONFIRM, 88)
  }

  fun triggerJump() {
    if (phase != VoltGamePhase.PLAYING) return
    if (jumpHeight <= 0.04f) {
      jumpVel = 2.48f
      haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      if (!muted) voiceAudio.playTouchSound(true, "Warm Marimba", VoiceAudioManager.TouchCue.BUTTON_TAP, 75)
    }
  }

  fun triggerBoost() {
    if (phase != VoltGamePhase.PLAYING) return
    if (batteryPct >= 14f && boostTimer <= 0.2f) {
      boostTimer = 2.2f
      batteryPct = (batteryPct - 12f).coerceAtLeast(0f)
      advanceMissionProgress(VoltMissionType.BOOST_BLITZ)
      haptics.performHapticFeedback(HapticFeedbackType.LongPress)
      if (!muted) voiceAudio.playTouchSound(true, "Studio Glass", VoiceAudioManager.TouchCue.ACTION_CONFIRM, 90)
    }
  }

  // 60fps simulation loop
  LaunchedEffect(phase) {
    var lastNs = 0L
    while (true) {
      withFrameNanos { now ->
        val dt = if (lastNs == 0L) 0.016f else ((now - lastNs) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastNs = now
        worldClock += dt

        if (phase == VoltGamePhase.TITLE) {
          roadScroll = (roadScroll + dt * 0.55f) % 1f
          return@withFrameNanos
        }
        if (phase != VoltGamePhase.PLAYING) return@withFrameNanos

        if (hitstopTimer > 0f) {
          hitstopTimer = (hitstopTimer - dt).coerceAtLeast(0f)
          return@withFrameNanos
        }

        val difficultyRamp = (1f + distanceMiles * 0.14f).coerceAtMost(2.35f)
        val speedFactor = difficultyRamp * (if (boostTimer > 0f) 1.55f else 1f)
        roadScroll = (roadScroll + dt * 1.18f * speedFactor) % 1f
        distanceMiles += dt * 0.042f * speedFactor
        score += (dt * 42f * speedFactor * (1f + comboCount * 0.08f)).toInt()

        // Smooth lane damping & horizon banking recovery after turns
        val damp = 1f - kotlin.math.exp(-15f * dt)
        lanePos += (targetLane.toFloat() - lanePos) * damp
        turnBankDeg *= kotlin.math.exp(-5.5f * dt)
        if (turnSignalTimer > 0f) turnSignalTimer = (turnSignalTimer - dt).coerceAtLeast(0f)
        if (bannerTimer > 0f) bannerTimer = (bannerTimer - dt).coerceAtLeast(0f)

        // Jump arc physics
        if (jumpHeight > 0f || jumpVel > 0f) {
          jumpHeight += jumpVel * dt
          jumpVel -= 6.8f * dt
          if (jumpHeight <= 0f) {
            jumpHeight = 0f
            jumpVel = 0f
            spawnBurst(0.5f + lanePos * 0.22f, 0.80f, Color(0xFFD97706), 7)
          }
        }

        if (boostTimer > 0f) boostTimer = (boostTimer - dt).coerceAtLeast(0f)
        if (shakeTimer > 0f) shakeTimer = (shakeTimer - dt).coerceAtLeast(0f)
        if (hitFlash > 0f) hitFlash = (hitFlash - dt * 3.5f).coerceAtLeast(0f)

        batteryPct = (batteryPct - dt * 1.38f).coerceAtLeast(0f)
        if (batteryPct <= 0f) {
          bestScore = maxOf(bestScore, score)
          phase = VoltGamePhase.GAME_OVER
          return@withFrameNanos
        }

        // Spawn intersections periodically so players can turn onto any street
        intersectionTimer -= dt * speedFactor
        if (intersectionTimer <= 0f) {
          intersectionTimer = 6.2f + Random.nextFloat() * 3.0f
          val candidates = STREET_NAMES.filter { it != currentStreet }.shuffled()
          intersections += RoadIntersection(
            id = nextIntersectionId++,
            z = 0.05f,
            leftStreet = candidates.getOrElse(0) { "Valley View Pike" },
            rightStreet = candidates.getOrElse(1) { "Copperhead Trail" },
          )
        }

        val interIter = intersections.listIterator()
        while (interIter.hasNext()) {
          val inter = interIter.next()
          inter.z += dt * 0.56f * speedFactor
          if (inter.z > 1.15f) interIter.remove()
        }

        // Spawn hazards & collectibles
        spawnTimer -= dt * speedFactor
        if (spawnTimer <= 0f) {
          spawnTimer = (0.68f - difficultyRamp * 0.14f).coerceAtLeast(0.30f) + Random.nextFloat() * 0.25f
          val lane = Random.nextInt(-1, 2)
          val roll = Random.nextFloat()
          val kind =
            when {
              roll < 0.26f -> TrackEntityKind.VOLT_CELL
              roll < 0.42f -> TrackEntityKind.SOLAR_ORB
              roll < 0.65f -> TrackEntityKind.HAY_BALE
              roll < 0.85f -> TrackEntityKind.CREEK_WASHOUT
              else -> TrackEntityKind.TRACTOR
            }
          entities += TrackEntity(id = nextEntityId++, lane = lane, z = 0.04f, kind = kind)
        }

        // Advance entities & collision detection
        val iter = entities.listIterator()
        while (iter.hasNext()) {
          val e = iter.next()
          e.z += dt * 0.68f * speedFactor
          if (!e.collected && e.z in 0.84f..0.98f && abs(lanePos - e.lane) < 0.44f) {
            val px = 0.5f + lanePos * 0.22f
            when (e.kind) {
              TrackEntityKind.VOLT_CELL -> {
                e.collected = true
                batteryPct = (batteryPct + 18f).coerceAtMost(100f)
                score += 150
                comboCount++
                spawnBurst(px, 0.78f, Color(0xFF39FF14), 16)
                advanceMissionProgress(VoltMissionType.COLLECT_CELLS)
                if (!muted) voiceAudio.playTouchSound(true, "Studio Glass", VoiceAudioManager.TouchCue.MESSAGE_SENT, 80)
              }
              TrackEntityKind.SOLAR_ORB -> {
                e.collected = true
                score += 250 + comboCount * 25
                comboCount++
                spawnBurst(px, 0.78f, Color(0xFFFACC15), 18)
                advanceMissionProgress(VoltMissionType.PATROL_ORBS)
                if (!muted) voiceAudio.playTouchSound(true, "Country Acoustic", VoiceAudioManager.TouchCue.ACTION_CONFIRM, 85)
              }
              TrackEntityKind.HAY_BALE, TrackEntityKind.CREEK_WASHOUT -> {
                val clearedByJump = jumpHeight > 0.22f
                if (clearedByJump) {
                  e.collected = true
                  score += 90
                  advanceMissionProgress(VoltMissionType.HOP_HAZARDS)
                } else {
                  e.collected = true
                  if (boostTimer > 0f) {
                    score += 180
                    hitstopTimer = 0.045f
                    shakeTimer = 0.22f
                    spawnBurst(px, 0.78f, Color(0xFF22D3EE), 20)
                    advanceMissionProgress(VoltMissionType.HOP_HAZARDS)
                  } else {
                    lives--
                    comboCount = 0
                    hitstopTimer = 0.06f
                    shakeTimer = 0.35f
                    hitFlash = 1f
                    spawnBurst(px, 0.78f, Color(0xFFEF4444), 18)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (lives <= 0) {
                      bestScore = maxOf(bestScore, score)
                      phase = VoltGamePhase.GAME_OVER
                    }
                  }
                }
              }
              TrackEntityKind.TRACTOR -> {
                e.collected = true
                lives--
                comboCount = 0
                hitstopTimer = 0.07f
                shakeTimer = 0.42f
                hitFlash = 1f
                spawnBurst(px, 0.78f, Color(0xFFEF4444), 22)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                if (lives <= 0) {
                  bestScore = maxOf(bestScore, score)
                  phase = VoltGamePhase.GAME_OVER
                }
              }
            }
          }
          if (e.z > 1.15f || e.collected) {
            iter.remove()
          }
        }

        // Advance particles
        val pIter = particles.listIterator()
        while (pIter.hasNext()) {
          val p = pIter.next()
          p.x += p.vx * dt
          p.y += p.vy * dt
          p.vy += 0.9f * dt
          p.life -= dt * 2.1f
          if (p.life <= 0f) pIter.remove()
        }
      }
    }
  }

  // FULL-VIEWPORT GAME SHELL
  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(Color(0xFF040812))
        .pointerInput(phase) {
          var dragAccum = 0f
          detectHorizontalDragGestures(
            onDragEnd = { dragAccum = 0f },
            onHorizontalDrag = { _, dragAmount ->
              dragAccum += dragAmount
              if (dragAccum > 48f) {
                steer(1)
                dragAccum = 0f
              } else if (dragAccum < -48f) {
                steer(-1)
                dragAccum = 0f
              }
            },
          )
        }
        .testTag("gravel_volt_game_viewport"),
  ) {
    // 60FPS UNREAL-STYLED VOLUMETRIC TWILIGHT & WET SPECULAR BACKROAD CANVAS
    Canvas(modifier = Modifier.fillMaxSize()) {
      val shakeX = if (shakeTimer > 0f) (Random.nextFloat() - 0.5f) * 18f * (shakeTimer / 0.35f) else 0f
      val shakeY = if (shakeTimer > 0f) (Random.nextFloat() - 0.5f) * 14f * (shakeTimer / 0.35f) else 0f
      val w = size.width
      val h = size.height
      val horizonY = h * 0.38f + shakeY + turnBankDeg * 0.45f
      val roadTopW = w * 0.13f
      val roadBotW = w * 0.98f
      val cx = w * 0.5f + shakeX - turnBankDeg * 4.5f

      // 1) Volumetric Twilight Sky Shader
      drawRect(
        brush = Brush.verticalGradient(
          listOf(Color(0xFF020617), Color(0xFF0F172A), Color(0xFF3B1D5A), Color(0xFFC2410C), Color(0xFFF59E0B)),
          startY = 0f,
          endY = horizonY,
        ),
        size = Size(w, horizonY),
      )

      // Atmospheric bloom sun + twilight star glints
      drawCircle(
        brush = Brush.radialGradient(
          listOf(Color(0xFFFEF08A), Color(0xBBFB923C), Color(0x447C3AED), Color.Transparent),
          center = Offset(cx, horizonY - 28f),
          radius = w * 0.34f,
        ),
        center = Offset(cx, horizonY - 28f),
        radius = w * 0.34f,
      )
      for (s in 0..16) {
        val sx = ((s * 97 + compassIdx * 61) % 100) / 100f * w
        val sy = ((s * 43) % 24) / 100f * h
        val alpha = 0.25f + 0.35f * abs(sin(worldClock * 2.2f + s))
        drawCircle(Color.White.copy(alpha = alpha), radius = 1.6f, center = Offset(sx, sy))
      }

      // Layered Appalachian Ridges + Silhouetted Pines (depth parallax)
      val farRidge = Path().apply {
        moveTo(0f, horizonY)
        lineTo(w * 0.14f, horizonY - 62f)
        lineTo(w * 0.31f, horizonY - 26f)
        lineTo(w * 0.52f, horizonY - 78f)
        lineTo(w * 0.74f, horizonY - 36f)
        lineTo(w * 0.90f, horizonY - 66f)
        lineTo(w, horizonY)
        close()
      }
      drawPath(farRidge, color = Color(0xFF1E1B3A).copy(alpha = 0.85f))

      val nearRidge = Path().apply {
        moveTo(0f, horizonY)
        lineTo(w * 0.18f, horizonY - 34f)
        lineTo(w * 0.38f, horizonY - 12f)
        lineTo(w * 0.62f, horizonY - 40f)
        lineTo(w * 0.84f, horizonY - 16f)
        lineTo(w, horizonY - 24f)
        lineTo(w, horizonY)
        close()
      }
      drawPath(nearRidge, color = Color(0xFF0A1416))

      // Volumetric Horizon Mist Band
      drawRect(
        brush = Brush.verticalGradient(listOf(Color.Transparent, Color(0x55F97316), Color(0x220EA5E9), Color.Transparent)),
        topLeft = Offset(0f, horizonY - 22f),
        size = Size(w, 44f),
      )

      // 2) Moody Wet Rural Meadow Ground
      drawRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF0D1D1A), Color(0xFF060E0D)), startY = horizonY, endY = h),
        topLeft = Offset(0f, horizonY),
        size = Size(w, h - horizonY),
      )

      // 3) Wet Specular Asphalt/Gravel Roadway
      val roadPath = Path().apply {
        moveTo(cx - roadTopW * 0.5f, horizonY)
        lineTo(cx + roadTopW * 0.5f, horizonY)
        lineTo(cx + roadBotW * 0.5f, h)
        lineTo(cx - roadBotW * 0.5f, h)
        close()
      }
      drawPath(
        roadPath,
        brush = Brush.verticalGradient(listOf(Color(0xFF28242C), Color(0xFF171A21), Color(0xFF0D1117)), startY = horizonY, endY = h),
      )

      // Sunset Sky Reflection Pool down center of wet road (UE5 SSR effect)
      val reflectionPath = Path().apply {
        moveTo(cx - roadTopW * 0.18f, horizonY)
        lineTo(cx + roadTopW * 0.18f, horizonY)
        lineTo(cx + roadBotW * 0.16f, h)
        lineTo(cx - roadBotW * 0.16f, h)
        close()
      }
      drawPath(
        reflectionPath,
        brush = Brush.verticalGradient(listOf(Color(0x3AF59E0B), Color(0x1838BDF8), Color(0x08000000)), startY = horizonY, endY = h),
      )

      // Scrolling Open-Street Intersections (Crossroads!)
      intersections.forEach { inter ->
        val p = (inter.z * inter.z).coerceIn(0f, 1.2f)
        val iy = horizonY + (h - horizonY) * p
        val crossH = 12f + 78f * p
        drawRect(
          brush = Brush.horizontalGradient(listOf(Color(0xFF141820), Color(0xFF272B36), Color(0xFF141820))),
          topLeft = Offset(0f, iy - crossH * 0.5f),
          size = Size(w, crossH),
        )
        drawLine(Color(0x8838BDF8), Offset(0f, iy - crossH * 0.5f), Offset(w, iy - crossH * 0.5f), strokeWidth = 2f)
        drawLine(Color(0x88FACC15), Offset(0f, iy), Offset(w, iy), strokeWidth = 1.5f + 2f * p)
      }

      // Streetlamps + Volumetric Light Cones + Dashed Lane Lines
      for (i in 0..10) {
        val tRaw = ((i / 11f) + roadScroll) % 1f
        val p = tRaw * tRaw
        val y = horizonY + (h - horizonY) * p
        val halfW = (roadTopW + (roadBotW - roadTopW) * p) * 0.5f
        val postH = 14f + 74f * p
        val lx = cx - halfW - 10f * p
        val rx = cx + halfW + 10f * p
        drawLine(Color(0xFF475569), Offset(lx, y), Offset(lx, y - postH), strokeWidth = 2f + 4f * p)
        drawLine(Color(0xFF475569), Offset(rx, y), Offset(rx, y - postH), strokeWidth = 2f + 4f * p)

        // Glowing warm/cyan lamp heads & wet ground light pools every 2nd post
        if (i % 2 == 0) {
          drawCircle(Color(0xCCFFE08A), radius = 3f + 5f * p, center = Offset(lx, y - postH))
          drawCircle(Color(0xCC38BDF8), radius = 3f + 5f * p, center = Offset(rx, y - postH))
          drawOval(
            brush = Brush.radialGradient(listOf(Color(0x33FDE047), Color.Transparent), center = Offset(lx + 14f * p, y), radius = 36f * p + 4f),
            topLeft = Offset(lx - 18f * p, y - 8f * p),
            size = Size(64f * p, 20f * p),
          )
        }

        // Illuminated dashed highway lane markers
        val laneOffset = halfW * 0.34f
        drawLine(Color(0x88FBBF24), Offset(cx - laneOffset, y), Offset(cx - laneOffset * 1.06f, y + 14f * p), strokeWidth = 1.5f + 2.8f * p)
        drawLine(Color(0x88FBBF24), Offset(cx + laneOffset, y), Offset(cx + laneOffset * 1.06f, y + 14f * p), strokeWidth = 1.5f + 2.8f * p)
      }

      // Roadside Neon Guard Rails
      drawLine(Color(0x9922D3EE), Offset(cx - roadTopW * 0.5f, horizonY), Offset(cx - roadBotW * 0.5f, h), strokeWidth = 3f)
      drawLine(Color(0x9939FF14), Offset(cx + roadTopW * 0.5f, horizonY), Offset(cx + roadBotW * 0.5f, h), strokeWidth = 3f)

      // 4) Draw Oncoming Entities with Dynamic Ground Light/Shadow Reflections
      entities.sortedBy { it.z }.forEach { e ->
        val p = (e.z * e.z).coerceIn(0f, 1.3f)
        val y = horizonY + (h - horizonY) * p
        val halfW = (roadTopW + (roadBotW - roadTopW) * p) * 0.5f
        val ex = cx + e.lane * halfW * 0.64f
        val sz = (14f + 70f * p)

        when (e.kind) {
          TrackEntityKind.VOLT_CELL -> {
            val bob = sin(worldClock * 7f + e.id) * 6f * p
            drawOval(Color(0x4439FF14), topLeft = Offset(ex - sz * 0.6f, y - sz * 0.14f), size = Size(sz * 1.2f, sz * 0.28f))
            drawCircle(
              brush = Brush.radialGradient(listOf(Color(0x7739FF14), Color.Transparent), center = Offset(ex, y - sz * 0.5f + bob), radius = sz * 0.9f),
              center = Offset(ex, y - sz * 0.5f + bob),
              radius = sz * 0.9f,
            )
            drawRoundRect(
              color = Color(0xFF16A34A),
              topLeft = Offset(ex - sz * 0.28f, y - sz * 0.92f + bob),
              size = Size(sz * 0.56f, sz * 0.85f),
              cornerRadius = CornerRadius(6f, 6f),
            )
            drawRect(Color(0xFF39FF14), topLeft = Offset(ex - sz * 0.15f, y - sz * 1.02f + bob), size = Size(sz * 0.30f, sz * 0.12f))
          }
          TrackEntityKind.SOLAR_ORB -> {
            val bob = sin(worldClock * 6f + e.id) * 7f * p
            drawCircle(
              brush = Brush.radialGradient(listOf(Color(0x99FDE047), Color.Transparent), center = Offset(ex, y - sz * 0.5f + bob), radius = sz * 0.85f),
              center = Offset(ex, y - sz * 0.5f + bob),
              radius = sz * 0.85f,
            )
            drawCircle(Color(0xFFFEF08A), radius = sz * 0.35f, center = Offset(ex, y - sz * 0.5f + bob))
            drawCircle(Color(0x9938BDF8), radius = sz * 0.52f, center = Offset(ex, y - sz * 0.5f + bob), style = Stroke(2.2f))
          }
          TrackEntityKind.HAY_BALE -> {
            drawOval(Color(0x77000000), topLeft = Offset(ex - sz * 0.60f, y - sz * 0.14f), size = Size(sz * 1.20f, sz * 0.28f))
            drawRoundRect(
              brush = Brush.verticalGradient(listOf(Color(0xFFF59E0B), Color(0xFF92400E))),
              topLeft = Offset(ex - sz * 0.52f, y - sz * 0.72f),
              size = Size(sz * 1.04f, sz * 0.72f),
              cornerRadius = CornerRadius(14f, 14f),
            )
            drawRoundRect(
              color = Color(0xFFFDE68A),
              topLeft = Offset(ex - sz * 0.52f, y - sz * 0.72f),
              size = Size(sz * 1.04f, sz * 0.72f),
              cornerRadius = CornerRadius(14f, 14f),
              style = Stroke(2.2f),
            )
          }
          TrackEntityKind.CREEK_WASHOUT -> {
            drawOval(Color(0xFF0369A1), topLeft = Offset(ex - sz * 0.72f, y - sz * 0.24f), size = Size(sz * 1.44f, sz * 0.40f))
            drawOval(Color(0xBB38BDF8), topLeft = Offset(ex - sz * 0.48f, y - sz * 0.18f), size = Size(sz * 0.96f, sz * 0.18f))
          }
          TrackEntityKind.TRACTOR -> {
            drawOval(Color(0x88000000), topLeft = Offset(ex - sz * 0.72f, y - sz * 0.15f), size = Size(sz * 1.44f, sz * 0.28f))
            drawRoundRect(Color(0xFF15803D), topLeft = Offset(ex - sz * 0.55f, y - sz * 1.05f), size = Size(sz * 1.10f, sz * 0.92f), cornerRadius = CornerRadius(10f, 10f))
            drawRect(Color(0xFFFACC15), topLeft = Offset(ex - sz * 0.44f, y - sz * 0.85f), size = Size(sz * 0.88f, sz * 0.24f))
            // Oncoming tractor twin warm headlights!
            drawCircle(Color(0xFFFEF08A), radius = sz * 0.12f, center = Offset(ex - sz * 0.32f, y - sz * 0.52f))
            drawCircle(Color(0xFFFEF08A), radius = sz * 0.12f, center = Offset(ex + sz * 0.32f, y - sz * 0.52f))
            drawCircle(Color(0xFF111827), radius = sz * 0.24f, center = Offset(ex - sz * 0.54f, y - sz * 0.24f))
            drawCircle(Color(0xFF111827), radius = sz * 0.24f, center = Offset(ex + sz * 0.54f, y - sz * 0.24f))
          }
        }
      }

      // 5) Draw Player PEV with Dynamic Volumetric Headlight Beam Cone, Turn Signals & Taillight Trail
      val playerYBase = h * 0.83f + shakeY
      val playerX = cx + lanePos * (roadBotW * 0.28f)
      val jumpPx = jumpHeight * h * 0.24f
      val stretch = if (jumpHeight > 0.05f) 1.14f else 1.0f

      // Forward volumetric twin headlight beam cone sweeping the road ahead
      val beamPath = Path().apply {
        moveTo(playerX - 18f, playerYBase - 40f - jumpPx)
        lineTo(playerX + 18f, playerYBase - 40f - jumpPx)
        lineTo(playerX + 135f, horizonY + 32f)
        lineTo(playerX - 135f, horizonY + 32f)
        close()
      }
      drawPath(
        beamPath,
        brush = Brush.verticalGradient(
          listOf(Color(0x0538BDF8), Color(0x36FFFFFF), Color(0x55E0F2FE)),
          startY = horizonY + 32f,
          endY = playerYBase - 36f - jumpPx,
        ),
      )

      // Ground contact shadow + neon underglow
      drawOval(
        color = Color.Black.copy(alpha = (0.68f - jumpHeight * 0.38f).coerceIn(0.15f, 0.68f)),
        topLeft = Offset(playerX - 40f, playerYBase - 9f),
        size = Size(80f, 20f),
      )
      drawOval(
        color = if (boostTimer > 0f) Color(0x7722D3EE) else Color(0x4439FF14),
        topLeft = Offset(playerX - 32f, playerYBase - 6f),
        size = Size(64f, 14f),
      )

      if (boostTimer > 0f) {
        drawCircle(
          brush = Brush.radialGradient(listOf(Color(0xAA22D3EE), Color.Transparent), center = Offset(playerX, playerYBase - 44f - jumpPx), radius = 98f),
          center = Offset(playerX, playerYBase - 44f - jumpPx),
          radius = 98f,
        )
        // Speedwarp radial lines during Boost
        for (sl in 0..7) {
          val ang = (sl / 8f) * 2f * PI.toFloat() + worldClock * 4f
          val r1 = w * 0.25f
          val r2 = w * 0.55f
          drawLine(
            color = Color(0x5522D3EE),
            start = Offset(w * 0.5f + cos(ang) * r1, h * 0.45f + sin(ang) * r1),
            end = Offset(w * 0.5f + cos(ang) * r2, h * 0.45f + sin(ang) * r2),
            strokeWidth = 2.5f,
          )
        }
      }

      // Taillight ribbon trails behind player
      drawLine(Color(0x88EF4444), Offset(playerX - 12f, playerYBase - 36f - jumpPx), Offset(playerX - 18f, h), strokeWidth = 5f)
      drawLine(Color(0x88EF4444), Offset(playerX + 12f, playerYBase - 36f - jumpPx), Offset(playerX + 18f, h), strokeWidth = 5f)

      // Rear knobby tire + deck + glowing tailbar + blinking amber turn signals
      drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(playerX - 16f, playerYBase - 38f * stretch - jumpPx),
        size = Size(32f, 40f * stretch),
        cornerRadius = CornerRadius(10f, 10f),
      )
      drawRoundRect(
        color = if (boostTimer > 0f) Color(0xFF22D3EE) else Color(0xFF39FF14),
        topLeft = Offset(playerX - 28f, playerYBase - 48f * stretch - jumpPx),
        size = Size(56f, 14f),
        cornerRadius = CornerRadius(6f, 6f),
      )
      drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(playerX - 18f, playerYBase - 98f * stretch - jumpPx),
        size = Size(36f, 52f * stretch),
        cornerRadius = CornerRadius(14f, 14f),
      )
      drawCircle(Color(0xFF38BDF8), radius = 16f, center = Offset(playerX, playerYBase - 112f * stretch - jumpPx))
      drawLine(Color(0xFFFF1E1E), Offset(playerX - 15f, playerYBase - 41f * stretch - jumpPx), Offset(playerX + 15f, playerYBase - 41f * stretch - jumpPx), strokeWidth = 5.5f)

      // Active Rear Turn Signal Blinkers on player PEV
      val blinkerLit = turnSignalTimer > 0f && sin(worldClock * 16f) > 0f
      if (blinkerLit && turnSignalDir < 0) {
        drawCircle(Color(0xFFFFB020), radius = 7f, center = Offset(playerX - 24f, playerYBase - 41f * stretch - jumpPx))
      }
      if (blinkerLit && turnSignalDir > 0) {
        drawCircle(Color(0xFFFFB020), radius = 7f, center = Offset(playerX + 24f, playerYBase - 41f * stretch - jumpPx))
      }

      // 6) Pooled particles
      particles.forEach { pt ->
        drawCircle(
          color = pt.color.copy(alpha = pt.life.coerceIn(0f, 1f)),
          radius = pt.radius * pt.life.coerceAtLeast(0.2f),
          center = Offset(pt.x * w, pt.y * h),
        )
      }

      if (hitFlash > 0.01f) {
        drawRect(Color.Red.copy(alpha = hitFlash * 0.34f), size = size)
      }
    }

    // TOP COMPACT HUD + STREET COMPASS + ACTIVE MISSION STRIP
    Column(
      modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(shape = CircleShape, color = Color(0xBB0B132B), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))) {
          IconButton(onClick = { if (phase == VoltGamePhase.PLAYING) phase = VoltGamePhase.PAUSED else onBack() }, modifier = Modifier.testTag("game_back_pause_btn")) {
            Icon(
              imageVector = if (phase == VoltGamePhase.PLAYING) Icons.Default.Pause else Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back or Pause",
              tint = Color.White,
            )
          }
        }

        if (phase == VoltGamePhase.PLAYING || phase == VoltGamePhase.PAUSED) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xCC0B132B),
            border = BorderStroke(1.dp, Color(0xFF39FF14).copy(alpha = 0.55f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("$score PTS", color = Color(0xFF39FF14), fontWeight = FontWeight.Black, fontSize = 12.sp, maxLines = 1, softWrap = false)
              Text("${"%.2f".format(distanceMiles)} MI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, softWrap = false)
              Text("${batteryPct.toInt()}%⚡", color = if (batteryPct < 25f) Color(0xFFEF4444) else Color(0xFF38BDF8), fontWeight = FontWeight.Black, fontSize = 11.sp, maxLines = 1, softWrap = false)
              Text("♥".repeat(lives.coerceAtLeast(0)), color = Color(0xFFF43F5E), fontWeight = FontWeight.Black, fontSize = 11.sp, maxLines = 1, softWrap = false)
            }
          }
        }

        Surface(shape = CircleShape, color = Color(0xBB0B132B), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))) {
          IconButton(onClick = { muted = !muted }, modifier = Modifier.testTag("game_mute_btn")) {
            Icon(
              imageVector = if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
              contentDescription = "Toggle Game Audio",
              tint = if (muted) Color(0xFF94A3B8) else Color(0xFF39FF14),
            )
          }
        }
      }

      // Street Compass + Live Mission Objective HUD Bar (visible while playing)
      if (phase == VoltGamePhase.PLAYING) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xCC0F172A),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.55f)),
            modifier = Modifier.weight(1f),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Icon(Icons.Default.Explore, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
              Text(
                "${COMPASS_HEADINGS[compassIdx]} • $currentStreet",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xCC0F172A),
            border = BorderStroke(1.dp, Color(0xFFFACC15).copy(alpha = 0.6f)),
            modifier = Modifier.weight(1.15f),
          ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                  "🎯 ${activeMission.title}",
                  color = Color(0xFFFACC15),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Black,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text("$missionProgress/${activeMission.target}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
              }
              LinearProgressIndicator(
                progress = { (missionProgress.toFloat() / activeMission.target.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = Color(0xFFFACC15),
                trackColor = Color(0xFF1E293B),
              )
            }
          }
        }

        // Dynamic Turn-On-Any-Street Intersection Prompt!
        activeTurnIntersection?.let { inter ->
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xDD0B132B),
            border = BorderStroke(1.5.dp, Color(0xFF22D3EE)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                onClick = { turnOntoStreet(-1) },
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0E7490),
                modifier = Modifier.weight(1f).testTag("game_turn_left_street_btn"),
              ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  Icon(Icons.Default.KeyboardArrowLeft, null, tint = Color.White, modifier = Modifier.size(16.dp))
                  Text(inter.leftStreet, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
              }
              Text("CROSSING", color = Color(0xFF38BDF8), fontSize = 9.sp, fontWeight = FontWeight.Black)
              Surface(
                onClick = { turnOntoStreet(1) },
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0E7490),
                modifier = Modifier.weight(1f).testTag("game_turn_right_street_btn"),
              ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                  Text(inter.rightStreet, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                  Spacer(Modifier.width(4.dp))
                  Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }

        if (bannerTimer > 0f && bannerMessage.isNotBlank()) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xCC16A34A),
            modifier = Modifier.align(Alignment.CenterHorizontally),
          ) {
            Text(
              bannerMessage,
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
          }
        }
      }
    }

    // BOTTOM ARCADE CONTROLS (ONLY WHILE PLAYING)
    if (phase == VoltGamePhase.PLAYING) {
      Row(
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Surface(
            onClick = { steer(-1) },
            shape = CircleShape,
            color = Color(0xCC0F172A),
            border = BorderStroke(1.5.dp, Color(0xFF39FF14).copy(alpha = 0.65f)),
            modifier = Modifier.size(62.dp).testTag("game_steer_left_btn"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Steer Left", tint = Color.White, modifier = Modifier.size(34.dp))
            }
          }
          Surface(
            onClick = { steer(1) },
            shape = CircleShape,
            color = Color(0xCC0F172A),
            border = BorderStroke(1.5.dp, Color(0xFF39FF14).copy(alpha = 0.65f)),
            modifier = Modifier.size(62.dp).testTag("game_steer_right_btn"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Steer Right", tint = Color.White, modifier = Modifier.size(34.dp))
            }
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Surface(
            onClick = { triggerJump() },
            shape = CircleShape,
            color = Color(0xCCF59E0B),
            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.7f)),
            modifier = Modifier.size(62.dp).testTag("game_jump_btn"),
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
              Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Bunny Hop", tint = Color.Black, modifier = Modifier.size(26.dp))
              Text("HOP", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black)
            }
          }
          Surface(
            onClick = { triggerBoost() },
            shape = CircleShape,
            color = if (batteryPct >= 14f) Color(0xCC22D3EE) else Color(0x66475569),
            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.7f)),
            modifier = Modifier.size(62.dp).testTag("game_boost_btn"),
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
              Icon(Icons.Default.Bolt, contentDescription = "72V Boost", tint = Color.Black, modifier = Modifier.size(24.dp))
              Text("BOOST", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black)
            }
          }
        }
      }
    }

    // TITLE SCREEN OVERLAY WITH MISSION PICKER
    if (phase == VoltGamePhase.TITLE) {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xE60B132B),
        border = BorderStroke(1.5.dp, Color(0xFF39FF14)),
        modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.90f),
      ) {
        Column(
          modifier = Modifier.padding(22.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
          Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF39FF14), modifier = Modifier.size(42.dp))
          Text(
            "GRAVEL VOLT RUNNER",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = Color.White,
            textAlign = TextAlign.Center,
          )
          Text(
            "Pilot $callsign • $vehicleName",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF38BDF8),
            fontWeight = FontWeight.Bold,
          )
          Text(
            "Turn onto any rural street at intersections • Complete rotating Trail Missions • Fire 72V Boost with volumetric headlights!",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFCBD5E1),
            textAlign = TextAlign.Center,
          )
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.Flag, null, tint = Color(0xFFFACC15), modifier = Modifier.size(14.dp))
            Text("SELECT STARTING MISSION", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFFACC15))
          }
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(VoltMissionType.entries.indices.toList()) { idx ->
              val m = VoltMissionType.entries[idx]
              FilterChip(
                selected = selectedStartMissionIdx == idx,
                onClick = { selectedStartMissionIdx = idx },
                label = { Text("${m.title} (+${m.rewardPts})", fontSize = 10.sp, maxLines = 1, softWrap = false) },
              )
            }
          }
          if (bestScore > 0) {
            Text("Personal Best: $bestScore PTS", color = Color(0xFFFACC15), fontWeight = FontWeight.Black)
          }
          Spacer(Modifier.height(4.dp))
          Button(
            onClick = { resetRun() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39FF14), contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("game_start_cta"),
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("START TRAIL RUN", fontWeight = FontWeight.Black)
          }
        }
      }
    }

    // PAUSE / GAME OVER MODAL
    if (phase == VoltGamePhase.PAUSED || phase == VoltGamePhase.GAME_OVER) {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xEE0B132B),
        border = BorderStroke(1.5.dp, if (phase == VoltGamePhase.GAME_OVER) Color(0xFFF43F5E) else Color(0xFF38BDF8)),
        modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.88f),
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            Icons.Default.EmojiEvents,
            contentDescription = null,
            tint = Color(0xFFFACC15),
            modifier = Modifier.size(40.dp),
          )
          Text(
            if (phase == VoltGamePhase.GAME_OVER) "RUN COMPLETE!" else "PAUSED ON $currentStreet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = Color.White,
            textAlign = TextAlign.Center,
          )
          Text(
            "Score: $score PTS • ${"%.2f".format(distanceMiles)} MI • $streetsExplored Streets",
            color = Color(0xFF39FF14),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
          )
          Text(
            "Missions Cleared: $missionsCompleted • Best: ${maxOf(bestScore, score)} PTS",
            color = Color(0xFFFACC15),
            style = MaterialTheme.typography.labelLarge,
          )
          Spacer(Modifier.height(4.dp))
          if (phase == VoltGamePhase.PAUSED) {
            Button(
              onClick = { phase = VoltGamePhase.PLAYING },
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("game_resume_btn"),
            ) {
              Icon(Icons.Default.PlayArrow, contentDescription = null)
              Spacer(Modifier.width(6.dp))
              Text("Resume Run")
            }
          }
          Button(
            onClick = { resetRun() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39FF14), contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("game_restart_btn"),
          ) {
            Icon(Icons.Default.Replay, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Ride Again", fontWeight = FontWeight.Black)
          }
          if (phase == VoltGamePhase.GAME_OVER && score > 0) {
            OutlinedButton(
              onClick = {
                onShareHighScore(score, distanceMiles.toDouble())
              },
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("game_share_board_btn"),
            ) {
              Icon(Icons.Default.Share, contentDescription = null)
              Spacer(Modifier.width(6.dp))
              Text("Post High Score to Porch Board")
            }
          }
          OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("game_exit_btn"),
          ) {
            Text("Return to Studio")
          }
        }
      }
    }
  }
}
