package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.PevBatteryPhysics
import com.example.data.VisionAnomalyBox
import com.example.hardware.VoiceAudioManager
import com.example.ui.components.TacticalButton
import com.example.ui.components.TacticalCard
import com.example.ui.components.TacticalDialog
import com.example.ui.components.TacticalFilterChip
import com.example.ui.components.TacticalOutlinedButton
import com.example.ui.components.decodeSampledBitmap
import com.example.ui.components.tacticalOutlinedTextFieldColors
import com.example.ui.theme.LocalTacticalChrome
import com.example.viewmodel.CockpitViewModel
import com.example.viewmodel.CopilotSocialViewModel
import com.example.viewmodel.MechanicViewModel
import com.example.viewmodel.RadarViewModel
import com.example.viewmodel.SessionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.data.getActiveCustomVoice

@Composable
fun MechanicScreen(
  sessionVm: SessionViewModel,
  cockpitVm: CockpitViewModel,
  mechanicVm: MechanicViewModel,
  radarVm: RadarViewModel,
  socialVm: CopilotSocialViewModel? = null,
) {
  var activeTab by rememberSaveable { mutableIntStateOf(0) }
  val settings by sessionVm.settings.collectAsStateWithLifecycle()
  val vehicles by cockpitVm.vehicles.collectAsStateWithLifecycle()
  val telemetry by cockpitVm.telemetry.collectAsStateWithLifecycle()
  val currentOdomKm = (vehicles.firstOrNull()?.odometerKm ?: 0.0) + telemetry.tripDistanceKm
  val activeRigSummary =
    remember(vehicles) {
      vehicles.firstOrNull()?.let { "${it.name} (${it.voltage.toInt()}V ${it.batteryChemistry}, ${it.batteryWh}Wh, ${it.tireSize})" }
        ?: "72V 40Ah Performance PEV"
    }
  val haptic = LocalHapticFeedback.current

  fun switchTab(idx: Int) {
    activeTab = idx
    if (settings.hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    sessionVm.voiceAudio.playTouchSound(
      enabled = settings.touchSoundsEnabled,
      pack = settings.touchSoundPack,
      cue = VoiceAudioManager.TouchCue.TAB_SWITCH,
      volumePct = settings.globalVolume,
    )
  }

  Column(Modifier.fillMaxSize()) {
    com.example.ui.components.TacticalTabStrip(
      tabs = listOf("Bench", "Shop Calc", "Rig Specs", "Parts"),
      selectedIndex = activeTab,
      onSelectTab = { switchTab(it) },
      scrollable = false,
      testTagPrefix = "mechanic_tab",
    )

    when (activeTab) {
      0 -> VisionArMechanicTab(sessionVm, cockpitVm, mechanicVm, radarVm, socialVm)
      1 -> EngineeringLabTab(mechanicVm, settings.aiModelId, activeRigSummary, currentOdomKm, settings.distanceUnit)
      2 -> SpecEngineTab(mechanicVm, cockpitVm, sessionVm, socialVm, settings.aiModelId)
      3 -> PartsFinderTab(mechanicVm, cockpitVm, socialVm, settings.aiModelId)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VisionArMechanicTab(
  sessionVm: SessionViewModel,
  cockpitVm: CockpitViewModel,
  mechanicVm: MechanicViewModel,
  radarVm: RadarViewModel,
  socialVm: CopilotSocialViewModel? = null,
) {
  val ctx = LocalContext.current
  val settings by sessionVm.settings.collectAsStateWithLifecycle()
  val vehicles by cockpitVm.vehicles.collectAsStateWithLifecycle()
  val telemetry by cockpitVm.telemetry.collectAsStateWithLifecycle()
  val activeNavRoute by radarVm.activeNavRoute.collectAsStateWithLifecycle()
  val navStepIndex by radarVm.navCurrentStepIndex.collectAsStateWithLifecycle()
  val navDistToTurnM by radarVm.navDistanceToTurnM.collectAsStateWithLifecycle()
  val liveChargers by radarVm.liveChargers.collectAsStateWithLifecycle()
  val diagSessions by mechanicVm.diagSessions.collectAsStateWithLifecycle()
  val activeSessionId by mechanicVm.activeSessionId.collectAsStateWithLifecycle()
  val messages by mechanicVm.diagMessages.collectAsStateWithLifecycle()
  val busy by mechanicVm.diagBusy.collectAsStateWithLifecycle()
  val pinout by mechanicVm.wiringPinout.collectAsStateWithLifecycle()
  val checked by mechanicVm.checkedItems.collectAsStateWithLifecycle()
  val chrome = LocalTacticalChrome.current

  val clipboard = LocalClipboardManager.current
  val haptic = LocalHapticFeedback.current
  val diagListState = rememberLazyListState()

  fun tapCue(cue: VoiceAudioManager.TouchCue = VoiceAudioManager.TouchCue.BUTTON_TAP) {
    if (settings.hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    sessionVm.voiceAudio.playTouchSound(
      enabled = settings.touchSoundsEnabled,
      pack = settings.touchSoundPack,
      cue = cue,
      volumePct = settings.globalVolume,
    )
  }

  var prompt by rememberSaveable { mutableStateOf("") }
  var primaryBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var secondaryBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var opticalScope by rememberSaveable { mutableStateOf("GENERAL") }
  var detailLevel by rememberSaveable(settings.mechanicDefaultDepth) { mutableStateOf(settings.mechanicDefaultDepth) }
  var thermalOverlay by rememberSaveable { mutableStateOf(false) }
  var showBenchDrawer by rememberSaveable(settings.aiChatCompactTools) { mutableStateOf(!settings.aiChatCompactTools) }
  var showLiveBenchCam by remember { mutableStateOf(false) }
  var showCameraSheet by remember { mutableStateOf(false) }
  var showCameraFallbackDialog by remember { mutableStateOf(false) }
  var showMicFallbackDialog by remember { mutableStateOf(false) }
  var showLiveArHud by remember { mutableStateOf(false) }
  var showHistorySheet by remember { mutableStateOf(false) }
  var showEcodeDbDialog by remember { mutableStateOf(false) }
  var showPevProSuite by remember { mutableStateOf(false) }
  var showVoltmeterDialog by remember { mutableStateOf(false) }

  val quickSystemCameraLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
      if (bmp != null) {
        if (primaryBitmap == null) primaryBitmap = bmp else secondaryBitmap = bmp
        Toast.makeText(ctx, "Photo captured — tap Analyze or Send", Toast.LENGTH_SHORT).show()
      }
    }

  val legacyPicker1 =
    rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
      if (uri != null) {
        val decoded = decodeSampledBitmap(ctx, uri)
        if (decoded != null) {
          primaryBitmap = decoded
          Toast.makeText(ctx, "Angle 1 photo staged — tap Analyze or Send", Toast.LENGTH_SHORT).show()
        } else {
          Toast.makeText(ctx, "Could not load selected image — try another photo", Toast.LENGTH_SHORT).show()
        }
      }
    }

  val legacyPicker2 =
    rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
      if (uri != null) {
        val decoded = decodeSampledBitmap(ctx, uri)
        if (decoded != null) {
          secondaryBitmap = decoded
          Toast.makeText(ctx, "Angle 2 photo staged for Vision AR", Toast.LENGTH_SHORT).show()
        } else {
          Toast.makeText(ctx, "Could not load selected image — try another photo", Toast.LENGTH_SHORT).show()
        }
      }
    }

  val primaryPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        val decoded = decodeSampledBitmap(ctx, uri)
        if (decoded != null) {
          primaryBitmap = decoded
          Toast.makeText(ctx, "Angle 1 photo staged — tap Analyze or Send", Toast.LENGTH_SHORT).show()
        } else {
          Toast.makeText(ctx, "Could not load selected image — try another photo", Toast.LENGTH_SHORT).show()
        }
      }
    }
  val secondaryPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        val decoded = decodeSampledBitmap(ctx, uri)
        if (decoded != null) {
          secondaryBitmap = decoded
          Toast.makeText(ctx, "Angle 2 photo staged for Vision AR", Toast.LENGTH_SHORT).show()
        } else {
          Toast.makeText(ctx, "Could not load selected image — try another photo", Toast.LENGTH_SHORT).show()
        }
      }
    }

  fun launchPic1Picker() {
    runCatching {
      primaryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }.onFailure {
      runCatching { legacyPicker1.launch("image/*") }.onFailure {
        Toast.makeText(ctx, "No gallery app available on this device", Toast.LENGTH_SHORT).show()
      }
    }
  }

  fun launchPic2Picker() {
    runCatching {
      secondaryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }.onFailure {
      runCatching { legacyPicker2.launch("image/*") }.onFailure {
        Toast.makeText(ctx, "No gallery app available on this device", Toast.LENGTH_SHORT).show()
      }
    }
  }

  var pendingBenchCamPerm by remember { mutableStateOf(false) }
  val camPermLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        if (pendingBenchCamPerm) {
          showLiveBenchCam = true
          pendingBenchCamPerm = false
        } else {
          showCameraSheet = true
        }
      } else {
        pendingBenchCamPerm = false
        showCameraFallbackDialog = true
      }
    }

  fun openCameraOrFallback(forBenchCam: Boolean) {
    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
      if (forBenchCam) showLiveBenchCam = true else showCameraSheet = true
    } else {
      pendingBenchCamPerm = forBenchCam
      runCatching { camPermLauncher.launch(Manifest.permission.CAMERA) }.onFailure {
        pendingBenchCamPerm = false
        showCameraFallbackDialog = true
      }
    }
  }

  val speechLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
      if (res.resultCode == Activity.RESULT_OK) {
        val spoken = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
          prompt = if (prompt.isBlank()) spoken else "$prompt $spoken"
        }
      }
    }

  val thermalColorFilter =
    remember(thermalOverlay) {
      if (thermalOverlay) {
        ColorFilter.colorMatrix(
          ColorMatrix(
            floatArrayOf(
              1.6f, 0.4f, 0f, 0f, 20f,
              0.2f, 0.9f, -0.3f, 0f, 0f,
              -0.4f, -0.3f, 1.8f, 0f, -20f,
              0f, 0f, 0f, 1f, 0f,
            )
          )
        )
      } else null
    }

  val activeRig = remember(vehicles) { vehicles.firstOrNull() }
  val activePack =
    remember(activeRig) {
      PevBatteryPhysics.packProfile(
        ratedVoltage = activeRig?.voltage ?: 48.0,
        chemistry = activeRig?.batteryChemistry ?: "NMC",
      )
    }
  val fleetSummary =
    remember(vehicles, telemetry, activePack) {
      val rigs =
        vehicles.joinToString("; ") { v ->
          val p = PevBatteryPhysics.packProfile(v.voltage, v.batteryChemistry)
          "${v.name} (${v.category}, ${v.voltage.toInt()}V ${p.seriesCells}S ${p.chemistry}, Full ${"%.1f".format(p.fullChargeV)}V / Cutoff ${"%.1f".format(p.cutoffEmptyV)}V, ${v.batteryWh}Wh, ${v.tireSize})"
        }.ifBlank { "Universal PEV Platform (48V 13S NMC, Full 54.6V / Cutoff 39.0V)" }
      "$rigs | Live Pack=${telemetry.batteryPct}% (${"%.1f".format(telemetry.estimatedVoltage)}V of ${"%.1f".format(activePack.fullChargeV)}V full)"
    }

  fun launchDiagnosis(queryText: String, scopeOverride: String = opticalScope) {
    val clean = queryText.trim()
    val effectivePrimary = primaryBitmap ?: secondaryBitmap
    val effectiveSecondary = if (primaryBitmap != null) secondaryBitmap else null
    if (clean.isBlank() && effectivePrimary == null) {
      Toast.makeText(ctx, "Describe a PEV fault, tap a preset, or attach a photo first", Toast.LENGTH_SHORT).show()
      return
    }
    tapCue(VoiceAudioManager.TouchCue.MESSAGE_SENT)
    prompt = ""
    showBenchDrawer = false
    mechanicVm.runVisionDiagnosis(
      primaryBitmap = effectivePrimary,
      secondaryBitmap = effectiveSecondary,
      query = clean.ifBlank { "Perform a full visual inspection of the attached PEV hardware photo." },
      fleetInfo = fleetSummary,
      unitSystem = if (settings.distanceUnit == "km") "METRIC" else "IMPERIAL",
      scope = scopeOverride,
      detailLevel = detailLevel,
      imageLabels = listOfNotNull(effectivePrimary?.let { "Angle 1" }, effectiveSecondary?.let { "Angle 2" }),
      modelId = settings.aiModelId,
    )
  }

  LaunchedEffect(messages.size, busy, settings.aiChatAutoScroll, settings.autoSpeakAiReplies) {
    if (settings.aiChatAutoScroll) {
      val target = (messages.size + if (busy) 1 else 0).coerceAtLeast(1) - 1
      if (target >= 0) {
        runCatching { diagListState.animateScrollToItem(target) }
      }
    }
    if (!busy && settings.autoSpeakAiReplies) {
      val latestAi = messages.lastOrNull { !it.isUser && it.text.isNotBlank() }
      if (latestAi != null && latestAi.id != mechanicVm.lastAutoSpokenMsgId) {
        mechanicVm.markDiagAutoSpoken(latestAi.id)
        sessionVm.voiceAudio.speakWithActiveSettings(latestAi.text, settings)
      }
    }
  }

  val presetPrompts =
    remember {
      listOf(
        "Controller Burn Check" to "Inspect controller MOSFETs & phase wires for heat scorch, shorted gate drivers, or melted solder and provide multimeter resistance checks.",
        "Brake Caliper Align" to "Diagnose rotor rub, pad wear, hydraulic piston stick, and caliper bolt torque alignment protocol.",
        "Hub Bearing Play" to "Check axle nut torque, motor lip seal, and hub bearing radial play diagnosis steps.",
        "Error Code 07 / Hall" to "Guide Hall sensor 5V rail & signal pin multimeter checks for E07 motor phase faults.",
        "Acoustic Noise AI" to "Diagnose clicking, grinding, or high-pitched whine from my hub motor, belt, or suspension under load.",
        "Wiring Pinout Gen" to "Generate a complete wire-color pinout table for my PEV controller, throttle, brake cutoff, and Hall harness.",
        "Upgrade Fitment AI" to "Verify exact fitment, bolt spacing, voltage limits, and clearances before upgrading tires, rotors, or controller.",
        "Repair Cost & Labor AI" to "Estimate DIY parts cost, shop labor hours, and required specialty tools to fix a PEV controller or tire swap.",
        "Thermal Runaway Audit" to "Perform a Li-Ion BMS cell balance, charger cutoff voltage, and thermal safety inspection checklist.",
        "Used PEV Inspection" to "Give a 15-point pre-purchase used PEV inspection checklist covering frame cracks, battery sag, and water ingress.",
        "Roadside MacGyver Fix" to "Give practical backcountry rural roadside emergency fixes for loose bolts, broken brake levers, or flat tires miles from town.",
        "Multimeter Probe Guide" to "Walk me step-by-step through probing battery pack voltage, charge port fuse, throttle 5V rail, and phase continuity safely.",
        "Chain & Belt Tension" to "Give exact chain slack, belt deflection, sprocket alignment, and mud-cleaning specs for electric dirt bikes and scooters.",
        "Silicone Waterproofing" to "Point out every wire gland, deck seam, display seal, and bearing lip to waterproof for muddy rural trails.",
        "Hydraulic Brake Bleed" to "Step-by-step mineral oil vs DOT hydraulic brake lever bleed, air bubble burp, and pad bedding guide.",
        "Barn Winter Storage" to "Cold-barn winter storage voltage target (50% SOC), tire lift, and corrosion protection checklist.",
      )
    }

  Box(Modifier.fillMaxSize()) {
  Column(Modifier.fillMaxSize()) {
    // COMPACT WORKSHOP HEADER BAR WITH FULL-WIDTH SCROLLABLE ACTION PILLS (NO CLIPPED ICONS)
    Surface(
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          Modifier.fillMaxWidth().padding(horizontal = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(Modifier.weight(1f)) {
            Text(
              "BARN & BENCH AR MECHANIC",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Black,
              color = MaterialTheme.colorScheme.primary,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              "${activePack.seriesCells}S ${activePack.chemistry} (${"%.1f".format(telemetry.estimatedVoltage)}V/${"%.1f".format(activePack.fullChargeV)}V) • $opticalScope • ${if (busy) "Diagnosing live..." else "${messages.size} entries"}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
            )
          }
          if (busy) {
            TextButton(
              onClick = {
                tapCue(VoiceAudioManager.TouchCue.TOGGLE_OFF)
                mechanicVm.cancelVisionDiagnosis()
                Toast.makeText(ctx, "Stopped active diagnosis", Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier.testTag("mechanic_header_stop_button"),
            ) {
              Icon(Icons.Default.Stop, contentDescription = "Stop Diagnosis", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Stop", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
            }
          }
        }
        LazyRow(
          contentPadding = PaddingValues(horizontal = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          item {
            TacticalFilterChip(
              selected = showLiveBenchCam,
              onClick = {
                tapCue(if (showLiveBenchCam) VoiceAudioManager.TouchCue.TOGGLE_OFF else VoiceAudioManager.TouchCue.TOGGLE_ON)
                if (showLiveBenchCam) {
                  showLiveBenchCam = false
                } else {
                  openCameraOrFallback(forBenchCam = true)
                }
              },
              leadingIcon = if (showLiveBenchCam) Icons.Default.VideocamOff else Icons.Default.Videocam,
              label = if (showLiveBenchCam) "Bench ON" else "Bench Cam",
              modifier = Modifier.testTag("mechanic_live_bench_cam_button"),
            )
          }
          item {
            AssistChip(
              onClick = {
                tapCue(VoiceAudioManager.TouchCue.ACTION_CONFIRM)
                showVoltmeterDialog = true
              },
              leadingIcon = { Icon(Icons.Default.Bolt, contentDescription = "Barn Voltmeter Decoder", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp)) },
              label = { Text("Voltmeter", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
              modifier = Modifier.testTag("mechanic_voltmeter_chip"),
            )
          }
          item {
            TacticalFilterChip(
              selected = showBenchDrawer,
              onClick = {
                tapCue()
                showBenchDrawer = !showBenchDrawer
              },
              leadingIcon = if (showBenchDrawer) Icons.Default.ExpandLess else Icons.Default.Tune,
              label = if (showBenchDrawer) "Hide Tools" else "Tools",
              modifier = Modifier.testTag("mechanic_bench_toggle_button"),
            )
          }
          item {
            AssistChip(
              onClick = {
                tapCue()
                showLiveArHud = true
              },
              leadingIcon = { Icon(Icons.Default.ViewInAr, contentDescription = "Live AR Caliper HUD", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(15.dp)) },
              label = { Text("Live AR", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
              modifier = Modifier.testTag("mechanic_live_ar_chip"),
            )
          }
          item {
            AssistChip(
              onClick = {
                tapCue()
                showHistorySheet = true
              },
              leadingIcon = { Icon(Icons.Default.History, contentDescription = "Workshop Chat History", modifier = Modifier.size(15.dp)) },
              label = { Text("History (${diagSessions.size})", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
              modifier = Modifier.testTag("mechanic_chat_history_button"),
            )
          }
          item {
            AssistChip(
              onClick = {
                tapCue(VoiceAudioManager.TouchCue.TOGGLE_OFF)
                primaryBitmap = null
                secondaryBitmap = null
                mechanicVm.clearDiagChat()
                Toast.makeText(ctx, "Started fresh Barn Mechanic bench", Toast.LENGTH_SHORT).show()
              },
              leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Clear Workshop", modifier = Modifier.size(15.dp)) },
              label = { Text("Clear", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
              modifier = Modifier.testTag("mechanic_clear_workshop_button"),
            )
          }
        }
      }
    }

    // LIVE MAINTENANCE BENCH CAMERA ("BENCH EYES" — MECHANIC SEES WHAT YOU'RE WORKING ON)
    AnimatedVisibility(visible = showLiveBenchCam) {
      LiveMaintenanceBenchCameraPane(
        busy = busy,
        onClose = { showLiveBenchCam = false },
        onInspectLiveFrame = { bmp ->
          primaryBitmap = bmp
          val liveQuestion =
            prompt.trim().ifBlank {
              "Look closely at what I am working on right now on my PEV maintenance bench. Identify the exact component, spot any wear/misalignment/safety hazards, and walk me through the exact next repair step."
            }
          prompt = ""
          mechanicVm.runVisionDiagnosis(
            primaryBitmap = bmp,
            secondaryBitmap = secondaryBitmap,
            query = liveQuestion,
            fleetInfo = fleetSummary,
            unitSystem = if (settings.distanceUnit == "km") "METRIC" else "IMPERIAL",
            scope = opticalScope,
            detailLevel = detailLevel,
            imageLabels = listOfNotNull("Live Bench View", secondaryBitmap?.let { "Angle 2" }),
            modelId = settings.aiModelId,
          )
        },
      )
    }

    // COLLAPSIBLE BENCH TOOLS, SCOPES & LIVE 1-TAP DIAGNOSTIC PRESETS
    AnimatedVisibility(visible = showBenchDrawer) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
              AssistChip(
                onClick = { showPevProSuite = true },
                label = { Text("Pro Suite 470+", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.testTag("mechanic_pev_pro_suite_chip"),
              )
            }
            item {
              AssistChip(
                onClick = { showEcodeDbDialog = true },
                label = { Text("OEM E-Codes", style = MaterialTheme.typography.labelSmall) },
              )
            }
            item {
              TacticalFilterChip(
                selected = detailLevel == "DEEP_TECH",
                onClick = {
                  detailLevel = if (detailLevel == "DEEP_TECH") "STANDARD" else "DEEP_TECH"
                  Toast.makeText(ctx, "Mechanic depth: $detailLevel", Toast.LENGTH_SHORT).show()
                },
                label = if (detailLevel == "DEEP_TECH") "Deep Tech ✓" else "Standard Depth",
              )
            }
          }
          LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf("GENERAL", "ELECTRICAL", "BRAKING", "TIRES_CHASSIS", "THERMAL")) { sc ->
              TacticalFilterChip(
                selected = opticalScope == sc,
                onClick = {
                  opticalScope = sc
                  Toast.makeText(ctx, "Inspection scope: $sc", Toast.LENGTH_SHORT).show()
                },
                label = sc,
              )
            }
          }
          LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(presetPrompts) { (lbl, text) ->
              AssistChip(
                onClick = { launchDiagnosis(text) },
                leadingIcon = { Icon(Icons.Default.AutoAwesome, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                label = { Text(lbl, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.testTag("mechanic_preset_${lbl.lowercase().replace(" ", "_")}"),
              )
            }
          }
        }
      }
    }

    // EXPANSIVE DIAGNOSTIC CHAT FEED (~75-82% OF SCREEN)
    LazyColumn(
      state = diagListState,
      modifier = Modifier.weight(1f).fillMaxWidth().testTag("mechanic_chat_feed"),
      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (messages.isEmpty() && !busy && primaryBitmap == null) {
        item {
          TacticalCard(highlight = true) {
            Text(
              "WORKSHOP CHIEF AI READY • GEMINI VISION AR",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Black,
              color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
              "Snap a photo with CameraX, attach a controller/brake image, or tap any live diagnostic below to run instant step-by-step PEV troubleshooting with interactive checklists.",
              style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(presetPrompts.take(5)) { (lbl, text) ->
                AssistChip(
                  onClick = { launchDiagnosis(text) },
                  leadingIcon = { Icon(Icons.Default.Build, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                  label = { Text(lbl, style = MaterialTheme.typography.labelSmall) },
                )
              }
            }
          }
        }
      }

      item(key = "mechanic_barn_diagnostic_suite") {
        MechanicBarnDiagnosticSuite(
          onAskMechanicAi = { prompt -> launchDiagnosis(prompt) },
        )
      }

      if (primaryBitmap != null || secondaryBitmap != null) {
        item {
          val latestBoxes: List<VisionAnomalyBox> =
            messages.lastOrNull { !it.isUser }?.anomalies.orEmpty()
          TacticalCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text("STAGED HARDWARE OPTICS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TacticalFilterChip(
                  selected = thermalOverlay,
                  onClick = { thermalOverlay = !thermalOverlay },
                  label = "FLIR",
                  leadingIcon = Icons.Default.Thermostat,
                )
                IconButton(
                  onClick = {
                    primaryBitmap = null
                    secondaryBitmap = null
                  },
                  modifier = Modifier.testTag("clear_staged_photos_button"),
                ) {
                  Icon(Icons.Default.Close, contentDescription = "Clear Staged Photos", modifier = Modifier.size(18.dp))
                }
              }
            }
            Spacer(Modifier.height(6.dp))
            primaryBitmap?.let { bmp ->
              Box(Modifier.fillMaxWidth().height(210.dp)) {
                Image(
                  bitmap = bmp.asImageBitmap(),
                  contentDescription = "Primary Hardware Frame",
                  contentScale = ContentScale.Fit,
                  colorFilter = thermalColorFilter,
                  modifier = Modifier.fillMaxSize(),
                )
                Canvas(Modifier.fillMaxSize()) {
                  latestBoxes.forEach { box ->
                    val col =
                      if (box.severity == "CRITICAL") Color(0xFFF43F5E)
                      else Color(0xFF39FF14)
                    drawRect(
                      color = col,
                      topLeft = Offset(box.xNorm * size.width, box.yNorm * size.height),
                      size = Size(box.wNorm * size.width, box.hNorm * size.height),
                      style = Stroke(width = 4f),
                    )
                  }
                }
              }
            }
            if (latestBoxes.isNotEmpty()) {
              Spacer(Modifier.height(6.dp))
              Text(
                "DETECTED AR ANOMALIES (TAP TO DEEP-INSPECT):",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
              )
              LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                items(latestBoxes) { box ->
                  TacticalFilterChip(
                    selected = box.severity == "CRITICAL",
                    onClick = {
                      launchDiagnosis(
                        "Inspect the highlighted ${box.severity} anomaly '${box.label}' in the staged hardware frame and give exact repair, multimeter test, and torque steps.",
                      )
                    },
                    label = "${box.severity}: ${box.label}",
                  )
                }
              }
            }
            secondaryBitmap?.let { bmp2 ->
              Spacer(Modifier.height(6.dp))
              Text("Secondary Angle Staged ✓", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
              Image(
                bitmap = bmp2.asImageBitmap(),
                contentDescription = "Secondary Angle",
                contentScale = ContentScale.Fit,
                colorFilter = thermalColorFilter,
                modifier = Modifier.fillMaxWidth().height(120.dp),
              )
            }
            Spacer(Modifier.height(8.dp))
            TacticalButton(
              onClick = {
                launchDiagnosis(
                  prompt.ifBlank {
                    "Perform a full visual inspection of these staged PEV hardware photos ($opticalScope scope). Identify components, wear, alignment, or electrical risks, and give a step-by-step repair checklist."
                  }
                )
              },
              modifier = Modifier.fillMaxWidth().testTag("mechanic_analyze_staged_photo_button"),
            ) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text(if (busy) "Re-Run Vision AI Diagnosis" else "Analyze Staged Photo Now")
            }
          }
        }
      }

      if (pinout.isNotEmpty()) {
        item {
          TacticalCard(highlight = true) {
            Text("AUTO-MAPPED WIRING HARNESS PINOUT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            pinout.forEach { (wire, signal) ->
              Text("• $wire  ->  $signal", style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      }

      items(messages, key = { it.id }) { msg ->
        TacticalCard(highlight = !msg.isUser) {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
              if (msg.isUser) "RIDER QUESTION" else "BARN MECHANIC AI",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Black,
              color = if (msg.isUser) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            if (!msg.isUser) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = {
                    tapCue(VoiceAudioManager.TouchCue.ACTION_CONFIRM)
                    clipboard.setText(AnnotatedString(msg.text))
                    Toast.makeText(ctx, "Copied diagnosis to clipboard", Toast.LENGTH_SHORT).show()
                  },
                  modifier = Modifier.size(48.dp).testTag("mechanic_copy_msg_${msg.id}"),
                ) {
                  Icon(Icons.Default.ContentCopy, contentDescription = "Copy Diagnosis", modifier = Modifier.size(20.dp))
                }
                IconButton(
                  onClick = {
                    tapCue()
                    val customVoice = settings.getActiveCustomVoice()
                    val ok = sessionVm.voiceAudio.speakWithActiveSettings(msg.text, settings)
                    Toast.makeText(
                      ctx,
                      if (ok) "Speaking diagnosis (${customVoice?.name ?: settings.aiVoiceProfile.label})..." else "Global Volume is 0% — raise volume in Settings",
                      Toast.LENGTH_SHORT,
                    ).show()
                  },
                  modifier = Modifier.size(48.dp).testTag("mechanic_speak_msg_${msg.id}"),
                ) {
                  Icon(Icons.Default.VolumeUp, contentDescription = "Speak Diagnosis", modifier = Modifier.size(21.dp))
                }
                IconButton(
                  onClick = {
                    tapCue(VoiceAudioManager.TouchCue.TOGGLE_OFF)
                    sessionVm.voiceAudio.stopSpeaking()
                    Toast.makeText(ctx, "Stopped voice playback", Toast.LENGTH_SHORT).show()
                  },
                  modifier = Modifier.size(48.dp).testTag("mechanic_stop_voice_${msg.id}"),
                ) {
                  Icon(Icons.Default.StopCircle, contentDescription = "Stop Voice", modifier = Modifier.size(20.dp))
                }
                IconButton(
                  onClick = {
                    tapCue()
                    val shareIntent =
                      Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Rural ERides GO Mechanic Diagnosis")
                        putExtra(Intent.EXTRA_TEXT, msg.text)
                      }
                    runCatching { ctx.startActivity(Intent.createChooser(shareIntent, "Share Diagnosis")) }
                  },
                  modifier = Modifier.size(48.dp).testTag("mechanic_share_msg_${msg.id}"),
                ) {
                  Icon(Icons.Default.Share, contentDescription = "Share Diagnosis", modifier = Modifier.size(20.dp))
                }
              }
            }
          }
          Spacer(Modifier.height(4.dp))
          Text(msg.text, style = MaterialTheme.typography.bodyLarge)
          if (msg.checklist.isNotEmpty()) {
            val doneCount = msg.checklist.indices.count { "${msg.id}_$it" in checked }
            Spacer(Modifier.height(6.dp))
            Text(
              "ACTION CHECKLIST ($doneCount/${msg.checklist.size} done):",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
            msg.checklist.forEachIndexed { idx, step ->
              val key = "${msg.id}_$idx"
              Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                  checked = key in checked,
                  onCheckedChange = {
                    tapCue(if (key in checked) VoiceAudioManager.TouchCue.TOGGLE_OFF else VoiceAudioManager.TouchCue.ACTION_CONFIRM)
                    mechanicVm.toggleChecklistItem(key)
                  },
                )
                Text(step, style = MaterialTheme.typography.bodyMedium)
              }
            }
          }
          if (!msg.isUser && !busy) {
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              item {
                AssistChip(
                  onClick = { launchDiagnosis("Give exact multimeter voltage/ohm readings & fastener torque specs for this repair:\n${msg.text.take(500)}") },
                  label = { Text("Specs & Torque", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                )
              }
              item {
                AssistChip(
                  onClick = { launchDiagnosis("List exact replacement parts, connector types, and DIY tool list needed for this fix:\n${msg.text.take(500)}") },
                  label = { Text("Parts & Tools", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                )
              }
            }
          }
        }
      }

      if (busy) {
        item(key = "mechanic_busy_card") {
          TacticalCard(highlight = true) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.5.dp)
              Text("Workshop Chief AI running live Vision AR & hardware diagnosis...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
          }
        }
      }
    }

    // HIGH-CONTRAST SPACIOUS MULTI-LINE CAMERA + VOICE + PROMPT COMPOSER (ZERO CLIPPING)
    Surface(
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      tonalElevation = 6.dp,
      border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // PINNED STAGED OPTICS STRIP (ALWAYS VISIBLE ABOVE COMPOSER WHEN PIC 1 / PIC 2 / SNAP STAGED)
        if (primaryBitmap != null || secondaryBitmap != null) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
            shape = chrome.cardShape,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              primaryBitmap?.let { bmp1 ->
                Image(
                  bitmap = bmp1.asImageBitmap(),
                  contentDescription = "Pic 1 Hardware Photo",
                  contentScale = ContentScale.Crop,
                  colorFilter = thermalColorFilter,
                  modifier = Modifier.size(46.dp).clip(chrome.smallShape),
                )
              }
              secondaryBitmap?.let { bmp2 ->
                Image(
                  bitmap = bmp2.asImageBitmap(),
                  contentDescription = "Pic 2 Hardware Photo",
                  contentScale = ContentScale.Crop,
                  colorFilter = thermalColorFilter,
                  modifier = Modifier.size(46.dp).clip(chrome.smallShape),
                )
              }
              Column(Modifier.weight(1f)) {
                val countLbl =
                  when {
                    primaryBitmap != null && secondaryBitmap != null -> "2 Hardware Angles Staged"
                    primaryBitmap != null -> "Pic 1 Staged for Vision AR"
                    else -> "Pic 2 Staged for Vision AR"
                  }
                Text(countLbl, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Scope: $opticalScope • ${if (thermalOverlay) "FLIR ON" else "RGB Optics"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
              }
              TacticalFilterChip(
                selected = thermalOverlay,
                onClick = {
                  tapCue()
                  thermalOverlay = !thermalOverlay
                },
                label = "FLIR",
                leadingIcon = Icons.Default.Thermostat,
              )
              TacticalButton(
                onClick = {
                  launchDiagnosis(
                    prompt.ifBlank {
                      "Perform a full visual inspection of these staged PEV hardware photos ($opticalScope scope). Identify components, wear, alignment, or electrical risks, and give a step-by-step repair checklist."
                    }
                  )
                },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("mechanic_pinned_analyze_button"),
              ) {
                Text("Analyze", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
              }
              IconButton(
                onClick = {
                  tapCue(VoiceAudioManager.TouchCue.TOGGLE_OFF)
                  primaryBitmap = null
                  secondaryBitmap = null
                  Toast.makeText(ctx, "Cleared staged hardware photos", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(44.dp).testTag("mechanic_pinned_clear_photos_button"),
              ) {
                Icon(Icons.Default.Close, contentDescription = "Clear Staged Photos", modifier = Modifier.size(18.dp))
              }
            }
          }
        }

        // Fitted 3-slot media capture strip — camera & hardware photo attachments
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          MechanicComposerActionPill(
            icon = Icons.Default.CameraAlt,
            label = "Snap",
            selected = showCameraSheet || showCameraFallbackDialog,
            modifier = Modifier.weight(1f).testTag("open_camerax_button"),
          ) {
            tapCue(VoiceAudioManager.TouchCue.PHOTO_SHUTTER)
            openCameraOrFallback(forBenchCam = false)
          }

          MechanicComposerActionPill(
            icon = Icons.Default.PhotoLibrary,
            label = if (primaryBitmap != null) "Pic 1 ✓" else "Pic 1",
            selected = primaryBitmap != null,
            modifier = Modifier.weight(1f).testTag("pick_primary_photo_button"),
          ) {
            tapCue()
            launchPic1Picker()
          }

          MechanicComposerActionPill(
            icon = Icons.Default.PhotoLibrary,
            label = if (secondaryBitmap != null) "Pic 2 ✓" else "Pic 2",
            selected = secondaryBitmap != null,
            modifier = Modifier.weight(1f).testTag("pick_secondary_photo_button"),
          ) {
            tapCue()
            launchPic2Picker()
          }
        }

        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            placeholder = { Text("Describe fault, error code, voltage, or attach photo...") },
            trailingIcon = {
              IconButton(
                onClick = {
                  tapCue()
                  val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe the PEV fault or noise")
                  }
                  runCatching { speechLauncher.launch(intent) }.onFailure {
                    showMicFallbackDialog = true
                  }
                },
                modifier = Modifier.size(48.dp).testTag("mechanic_voice_mic_button"),
              ) {
                Icon(Icons.Default.Mic, contentDescription = "Voice Dictation", tint = MaterialTheme.colorScheme.primary)
              }
            },
            minLines = 2,
            maxLines = 5,
            colors = tacticalOutlinedTextFieldColors(),
            shape = chrome.cardShape,
            modifier = Modifier.weight(1f).testTag("mechanic_prompt_input"),
          )
          TacticalButton(
            onClick = {
              if (busy) {
                tapCue(VoiceAudioManager.TouchCue.TOGGLE_OFF)
                mechanicVm.cancelVisionDiagnosis()
                Toast.makeText(ctx, "Stopped active diagnosis", Toast.LENGTH_SHORT).show()
              } else if (prompt.isBlank() && primaryBitmap == null && secondaryBitmap == null) {
                tapCue()
                showMicFallbackDialog = true
              } else {
                launchDiagnosis(prompt)
              }
            },
            isDestructive = busy,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(56.dp).testTag("run_vision_diag_button"),
          ) {
            if (busy) {
              Icon(Icons.Default.Stop, contentDescription = "Stop Diagnosis")
            } else {
              Icon(Icons.Default.Send, contentDescription = "Run Diagnostic")
            }
          }
        }
      }
    }
  }

  if (showCameraSheet) {
    CameraXCaptureSheet(
      onDismiss = { showCameraSheet = false },
      onCaptured = { bmp ->
        if (primaryBitmap == null) primaryBitmap = bmp else secondaryBitmap = bmp
        showCameraSheet = false
        Toast.makeText(ctx, "Hardware frame staged — tap Analyze or Send", Toast.LENGTH_SHORT).show()
      },
    )
  }

  if (showCameraFallbackDialog) {
    TacticalDialog(
      onDismissRequest = { showCameraFallbackDialog = false },
      title = "Bench Camera & Capture Options",
      subtitle = "Snap a hardware photo or pick from your gallery",
      icon = Icons.Default.CameraAlt,
      confirmButton = {
        TacticalButton(
          onClick = {
            showCameraFallbackDialog = false
            runCatching { quickSystemCameraLauncher.launch(null) }.onFailure {
              launchPic1Picker()
            }
          },
        ) { Text("System Camera") }
      },
      dismissButton = {
        TacticalOutlinedButton(
          onClick = {
            showCameraFallbackDialog = false
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
              data = Uri.fromParts("package", ctx.packageName, null)
            }
            runCatching { ctx.startActivity(intent) }
          },
        ) { Text("App Permissions") }
      },
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          "In-app CameraX requires Camera permission in Android Settings, or you can capture right now using System Camera or Gallery.",
          style = MaterialTheme.typography.bodySmall,
        )
        TacticalOutlinedButton(
          onClick = {
            showCameraFallbackDialog = false
            launchPic1Picker()
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(Icons.Default.PhotoLibrary, null, Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("Pick Pic 1 from Gallery")
        }
      }
    }
  }

  if (showMicFallbackDialog) {
    val quickMechanicPrompts = listOf(
      "Error 07 / Hall sensor check: guide me step by step with a multimeter.",
      "Hydraulic brake rub & squishy lever: give bleed & caliper centering specs.",
      "High voltage sag under throttle load: test pack IR, XT90 plug, and BMS.",
      "Clicking noise or play from rear hub motor axle: check nuts, bearings, and keyway.",
    )
    TacticalDialog(
      onDismissRequest = { showMicFallbackDialog = false },
      title = "Quick Ask Barn Mechanic",
      subtitle = "Tap any instant hardware diagnostic to run now",
      icon = Icons.Default.AutoAwesome,
      confirmButton = { TacticalButton(onClick = { showMicFallbackDialog = false }) { Text("Close") } },
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        quickMechanicPrompts.forEach { q ->
          Surface(
            onClick = {
              showMicFallbackDialog = false
              launchDiagnosis(q)
            },
            shape = chrome.smallShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(q, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
          }
        }
      }
    }
  }

  if (showEcodeDbDialog) {
    var ecodeSearch by remember { mutableStateOf("") }
    val oemDb =
      listOf(
        "Dualtron / EY3: E07" to "Motor phase / Hall fault — inspect 5-pin white JST-SM Hall plug and phase bullets.",
        "Dualtron / EY3: E09" to "Controller overheat / MOSFET short — check phase resistance & thermal paste.",
        "Dualtron / EY3: E10" to "Display-to-controller RX/TX UART drop — inspect stem harness bend.",
        "Segway Ninebot: Error 14" to "Hall Throttle sensor out of 0.8–4.2V range — check thumb lever magnet.",
        "Segway Ninebot: Error 15" to "Brake Hall lever out of range — check e-brake sensor & cable.",
        "Segway Ninebot: Error 21" to "BMS UART comm failure — check yellow/green data wires and pack fuse.",
        "VESC / Flipsky: DRV_FAULT" to "Gate driver over-current / under-voltage — check phase solder bridges.",
        "VESC: ABS_OVER_CURRENT" to "Phase current spike above absolute max — tune motor amps & regen ramp.",
        "Begode / Gotway EUC" to "2/3-beep over-lean tiltback or Hall sensor disconnect — inspect board capacitors.",
        "InMotion / LeaperKim EUC: Overload" to "Board temp >80°C or PWM >85% — reduce speed and check cooling fan.",
        "Kaabo / Sinewave: E06 / E10" to "E06 Low battery cutoff; E10 Display-to-controller RX/TX UART drop.",
        "Bafang Ultra / BBS02: Error 21 / 30" to "Error 21 Speed sensor magnet alignment; Error 30 Controller-display UART timeout.",
        "Bosch / Shimano eBike: 503 / W011" to "Rear spoke speed sensor gap too wide (>10mm) or magnet slipped.",
        "Sur-Ron / Talaria: Code 105 / 206" to "Throttle voltage fault or kickstand/tilt cutoff switch stuck open.",
        "Onewheel: Error 16 / 23" to "Error 16 BMS/Controller bus corruption; Error 23 Incompatible hardware/regen fault.",
      )
    val shownOem =
      oemDb.filter { (t, d) ->
        ecodeSearch.isBlank() || t.contains(ecodeSearch, true) || d.contains(ecodeSearch, true)
      }
    TacticalDialog(
      onDismissRequest = { showEcodeDbDialog = false },
      title = "OEM PEV Error Code Reference (${shownOem.size})",
      subtitle = "Tap any fault code for full multimeter diagnostics",
      icon = Icons.Default.Build,
      confirmButton = {
        TacticalButton(onClick = { showEcodeDbDialog = false }) { Text("Close") }
      },
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = ecodeSearch,
          onValueChange = { ecodeSearch = it },
          label = { Text("Filter code or brand (e.g. E07, Bafang, VESC)") },
          singleLine = true,
          colors = tacticalOutlinedTextFieldColors(),
          shape = chrome.buttonShape,
          modifier = Modifier.fillMaxWidth().testTag("oem_ecode_search_input"),
        )
        LazyColumn(modifier = Modifier.height(270.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          items(shownOem) { (codeTitle, fixDesc) ->
            Surface(
              onClick = {
                showEcodeDbDialog = false
                launchDiagnosis("Full live repair diagnosis and multimeter test sequence for $codeTitle ($fixDesc)", scopeOverride = "ELECTRICAL")
              },
              shape = chrome.smallShape,
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(Modifier.padding(10.dp)) {
                Text(codeTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(fixDesc, style = MaterialTheme.typography.bodySmall)
                Text("Tap to run live AI troubleshooting →", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
              }
            }
          }
        }
      }
    }
  }

  if (showPevProSuite) {
    PevProSuiteModal(onDismiss = { showPevProSuite = false })
  }

  if (showVoltmeterDialog) {
    var ratedPackV by remember { mutableStateOf(activePack.nominalLabelV) }
    var chem by remember { mutableStateOf(activePack.chemistry) }
    var measuredStr by remember { mutableStateOf("%.1f".format(telemetry.estimatedVoltage)) }
    val profile = remember(ratedPackV, chem) { PevBatteryPhysics.packProfile(ratedPackV, chem) }
    val measuredV = measuredStr.toDoubleOrNull() ?: profile.nominalPackV
    val exactSoc = PevBatteryPhysics.socForVoltage(ratedPackV, measuredV, chem)
    val perCellV = measuredV / profile.seriesCells.coerceAtLeast(1)
    val verdict =
      when {
        measuredV > profile.fullChargeV + 0.35 -> "OVER-VOLTAGE WARNING — Check charger cutoff!"
        exactSoc >= 95.0 -> "Full Charge Ready (${profile.fullChargeV}V max)"
        exactSoc >= 45.0 -> "Healthy Operating / Trail Zone"
        exactSoc >= 20.0 -> "Storage / Low Reserve Zone — Plan Recharge"
        measuredV >= profile.cutoffEmptyV -> "Near BMS Cutoff — Crawl Home Gently"
        else -> "BELOW CUTOFF — Do NOT ride; check BMS & cell balance"
      }
    val packOptions = listOf(24.0, 36.0, 48.0, 52.0, 60.0, 72.0, 96.0, 100.8, 126.0, 134.4, 151.2)
    TacticalDialog(
      onDismissRequest = { showVoltmeterDialog = false },
      title = "Barn Voltmeter & Cell Decoder",
      subtitle = "Voltage SoC & BMS cutoff analyzer",
      icon = Icons.Default.Bolt,
      confirmButton = {
        TacticalButton(
          onClick = {
            tapCue(VoiceAudioManager.TouchCue.MESSAGE_SENT)
            showVoltmeterDialog = false
            launchDiagnosis(
              "Barn multimeter test on my ${profile.nominalLabelV}V (${profile.seriesCells}S ${profile.chemistry}) pack reads ${"%.2f".format(measuredV)}V resting (${"%.1f".format(exactSoc)}% SoC, ${"%.2f".format(perCellV)}V/cell; 100% full=${profile.fullChargeV}V, 0% cutoff=${profile.cutoffEmptyV}V). Verdict: $verdict. Check for BMS cell imbalance, charger calibration issues, and tell me exact safe next steps.",
              scopeOverride = "ELECTRICAL",
            )
          },
          modifier = Modifier.testTag("voltmeter_ask_mechanic_button"),
        ) {
          Text("Ask Barn Mechanic", maxLines = 1, softWrap = false)
        }
      },
      dismissButton = {
        TacticalOutlinedButton(onClick = { showVoltmeterDialog = false }) { Text("Close") }
      },
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Select Nominal / Rated Pack Voltage:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(packOptions) { vOpt ->
            TacticalFilterChip(
              selected = kotlin.math.abs(ratedPackV - vOpt) < 0.2,
              onClick = {
                tapCue()
                ratedPackV = vOpt
                measuredStr = "%.1f".format(PevBatteryPhysics.voltageForSoc(vOpt, exactSoc, chem))
              },
              label = "${if (vOpt % 1.0 == 0.0) vOpt.toInt() else vOpt}V",
            )
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("NMC", "LFP").forEach { c ->
            TacticalFilterChip(
              selected = chem == c,
              onClick = {
                tapCue()
                chem = c
              },
              label = if (c == "NMC") "Li-Ion NMC (4.20V)" else "LiFePO4 LFP (3.65V)",
            )
          }
        }
        OutlinedTextField(
          value = measuredStr,
          onValueChange = { measuredStr = it },
          label = { Text("Measured Resting Pack Voltage (V)") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          colors = tacticalOutlinedTextFieldColors(),
          shape = chrome.buttonShape,
          modifier = Modifier.fillMaxWidth().testTag("barn_voltmeter_input"),
        )
        TacticalCard(highlight = true) {
          Text(
            "${profile.seriesCells}S ${profile.chemistry} • ${"%.1f".format(exactSoc)}% SoC (${"%.2f".format(perCellV)}V/cell)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(Modifier.height(4.dp))
          Text(
            "100% Full: ${"%.1f".format(profile.fullChargeV)}V • Storage (~50%): ${"%.1f".format(profile.storageV)}V",
            style = MaterialTheme.typography.bodySmall,
          )
          Text(
            "20% Low Warn: ${"%.1f".format(profile.lowWarningV)}V • 0% Cutoff: ${"%.1f".format(profile.cutoffEmptyV)}V",
            style = MaterialTheme.typography.bodySmall,
          )
          Spacer(Modifier.height(4.dp))
          Text(verdict, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
        }
      }
    }
  }

  if (showLiveArHud) {
    LiveArHudOverlay(
      telemetry = telemetry,
      settings = settings,
      activeNavRoute = activeNavRoute,
      navStepIndex = navStepIndex,
      navDistToTurnM = navDistToTurnM,
      nearestCharger = liveChargers.firstOrNull(),
      initialMode = ArHudMode.OPTICAL_INSPECT,
      onCaptureForVisionAi = { bmp ->
        val prevPrimary = primaryBitmap
        if (prevPrimary == null) primaryBitmap = bmp else secondaryBitmap = bmp
        showLiveArHud = false
        val arPrompt =
          prompt.trim().ifBlank {
            "Analyze this Live AR optical capture for wear, damage, or alignment faults."
          }
        Toast.makeText(ctx, "AR frame captured — running Vision AI diagnosis", Toast.LENGTH_SHORT).show()
        mechanicVm.runVisionDiagnosis(
          primaryBitmap = prevPrimary ?: bmp,
          secondaryBitmap = if (prevPrimary != null) bmp else null,
          query = arPrompt,
          fleetInfo = fleetSummary,
          unitSystem = if (settings.distanceUnit == "km") "METRIC" else "IMPERIAL",
          scope = opticalScope,
          detailLevel = detailLevel,
          imageLabels = if (prevPrimary != null) listOf("Angle 1", "Live AR Frame") else listOf("Live AR Frame"),
          modelId = settings.aiModelId,
        )
      },
      onCancelNav = {
        radarVm.stopGpsNavigation(announce = settings.navVoiceGuidance, volumePct = settings.globalVolume)
      },
      onDismiss = { showLiveArHud = false },
    )
  }
  }

  if (showHistorySheet) {
    BackHandler { showHistorySheet = false }
    var historyQuery by remember { mutableStateOf("") }
    val dateFmt = remember { SimpleDateFormat("MMM d • h:mm a", Locale.US) }
    val filteredSessions =
      remember(diagSessions, historyQuery) {
        diagSessions.filter { s ->
          historyQuery.isBlank() ||
            s.title.contains(historyQuery, true) ||
            s.scope.contains(historyQuery, true) ||
            s.messages.any { it.text.contains(historyQuery, true) }
        }
      }
    ModalBottomSheet(onDismissRequest = { showHistorySheet = false }) {
      Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text("MECHANIC CHAT HISTORY (${diagSessions.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (messages.isNotEmpty()) {
              IconButton(
                onClick = {
                  val transcript = mechanicVm.exportActiveDiagTranscript()
                  val shareIntent =
                    Intent(Intent.ACTION_SEND).apply {
                      type = "text/plain"
                      putExtra(Intent.EXTRA_SUBJECT, "Rural ERides GO Mechanic Log")
                      putExtra(Intent.EXTRA_TEXT, transcript)
                    }
                  runCatching { ctx.startActivity(Intent.createChooser(shareIntent, "Export Mechanic Log")) }
                },
                modifier = Modifier.testTag("export_mechanic_history_button"),
              ) {
                Icon(Icons.Default.Share, contentDescription = "Export Transcript", tint = MaterialTheme.colorScheme.secondary)
              }
            }
            TacticalButton(
              onClick = {
                mechanicVm.startNewDiagSession(scope = opticalScope)
                showHistorySheet = false
              },
              modifier = Modifier.testTag("new_mechanic_session_button"),
            ) {
              Icon(Icons.Default.AddComment, null, Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("New")
            }
          }
        }
        OutlinedTextField(
          value = historyQuery,
          onValueChange = { historyQuery = it },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          placeholder = { Text("Search saved workshop chats & diagnoses...") },
          singleLine = true,
          colors = tacticalOutlinedTextFieldColors(),
          shape = chrome.buttonShape,
          modifier = Modifier.fillMaxWidth().testTag("mechanic_history_search_input"),
        )
        LazyColumn(modifier = Modifier.height(310.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          if (filteredSessions.isEmpty()) {
            item {
              TacticalCard {
                Text("No saved Mechanic workshop sessions yet. Run any AI diagnosis to automatically save it here.", style = MaterialTheme.typography.bodySmall)
              }
            }
          }
          items(filteredSessions, key = { it.id }) { sess ->
            TacticalCard(
              highlight = sess.id == activeSessionId,
              onClick = {
                mechanicVm.selectDiagSession(sess.id)
                showHistorySheet = false
              },
            ) {
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                  Text(sess.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                  Text(
                    "${sess.scope} • ${sess.messages.size} msgs • ${dateFmt.format(Date(sess.updatedAtMs))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                IconButton(onClick = { mechanicVm.deleteDiagSession(sess.id) }) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete Workshop Session", tint = MaterialTheme.colorScheme.onSurface)
                }
              }
            }
          }
        }
      }
    }
  }
}

private fun decodeCapturedImageProxy(image: ImageProxy, previewView: PreviewView?): Bitmap? {
  return runCatching { image.toBitmap() }.getOrNull()
    ?: runCatching {
      val buf = image.planes[0].buffer
      val bytes = ByteArray(buf.remaining())
      buf.get(bytes)
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
    ?: previewView?.bitmap
}

@Composable
private fun LiveMaintenanceBenchCameraPane(
  busy: Boolean,
  onClose: () -> Unit,
  onInspectLiveFrame: (Bitmap) -> Unit,
) {
  val ctx = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val imageCapture = remember { ImageCapture.Builder().build() }
  var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
  var cameraBindError by remember { mutableStateOf<String?>(null) }
  var cameraReady by remember { mutableStateOf(false) }
  val accent = MaterialTheme.colorScheme.primary

  DisposableEffect(lifecycleOwner) {
    onDispose {
      runCatching { ProcessCameraProvider.getInstance(ctx).get().unbindAll() }
    }
  }

  val legacyBenchGalleryFallback =
    rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
      if (uri != null) {
        val bmp = decodeSampledBitmap(ctx, uri)
        if (bmp != null) onInspectLiveFrame(bmp)
        else Toast.makeText(ctx, "Could not decode selected photo", Toast.LENGTH_SHORT).show()
      }
    }

  val benchGalleryFallback =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        val bmp = decodeSampledBitmap(ctx, uri)
        if (bmp != null) onInspectLiveFrame(bmp)
        else Toast.makeText(ctx, "Could not decode selected photo", Toast.LENGTH_SHORT).show()
      }
    }

  Surface(
    color = Color.Black,
    border = BorderStroke(1.5.dp, accent.copy(alpha = 0.7f)),
    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
    shape = RoundedCornerShape(14.dp),
  ) {
    Column(Modifier.fillMaxWidth()) {
      Box(Modifier.fillMaxWidth().height(185.dp)) {
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
                  provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                  cameraBindError = null
                  cameraReady = true
                }.onFailure { err ->
                  cameraBindError = err.message ?: "Camera hardware unavailable"
                  cameraReady = false
                }
              },
              ContextCompat.getMainExecutor(viewCtx),
            )
            previewView
          },
          modifier = Modifier.fillMaxSize(),
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
          val cx = size.width / 2f
          val cy = size.height / 2f
          val r = size.minDimension * 0.24f
          drawCircle(accent.copy(alpha = 0.7f), radius = r, center = Offset(cx, cy), style = Stroke(width = 2.5f))
          drawLine(accent.copy(alpha = 0.7f), Offset(cx - r - 12f, cy), Offset(cx - 10f, cy), strokeWidth = 2f)
          drawLine(accent.copy(alpha = 0.7f), Offset(cx + 10f, cy), Offset(cx + r + 12f, cy), strokeWidth = 2f)
          drawLine(accent.copy(alpha = 0.7f), Offset(cx, cy - r - 12f), Offset(cx, cy - 10f), strokeWidth = 2f)
          drawLine(accent.copy(alpha = 0.7f), Offset(cx, cy + 10f), Offset(cx, cy + r + 12f), strokeWidth = 2f)
        }
        Surface(
          color = Color.Black.copy(alpha = 0.72f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        ) {
          Text(
            text =
              when {
                cameraBindError != null -> "CAMERA UNAVAILABLE — USE GALLERY"
                !cameraReady -> "STARTING LIVE BENCH CAMERA..."
                else -> "LIVE BENCH EYES • POINT AT PART"
              },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = accent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
        IconButton(
          onClick = onClose,
          modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close Bench Camera", tint = Color.White)
        }
      }
      Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TacticalButton(
          enabled = !busy,
          onClick = {
            if (cameraBindError != null || !cameraReady) {
              val fallback = previewViewRef?.bitmap
              if (fallback != null) {
                onInspectLiveFrame(fallback)
              } else {
                Toast.makeText(ctx, "Camera warming up or unavailable — tap Gallery to inspect a photo", Toast.LENGTH_SHORT).show()
              }
              return@TacticalButton
            }
            runCatching {
              imageCapture.takePicture(
                ContextCompat.getMainExecutor(ctx),
                object : ImageCapture.OnImageCapturedCallback() {
                  override fun onCaptureSuccess(image: ImageProxy) {
                    val bmp = decodeCapturedImageProxy(image, previewViewRef)
                    image.close()
                    if (bmp != null) {
                      onInspectLiveFrame(bmp)
                    } else {
                      Toast.makeText(ctx, "Could not capture frame — try Gallery instead", Toast.LENGTH_SHORT).show()
                    }
                  }

                  override fun onError(exception: ImageCaptureException) {
                    val fallback = previewViewRef?.bitmap
                    if (fallback != null) {
                      onInspectLiveFrame(fallback)
                    } else {
                      Toast.makeText(ctx, "Bench capture failed: ${exception.message ?: "try Gallery"}", Toast.LENGTH_SHORT).show()
                    }
                  }
                },
              )
            }.onFailure {
              val fallback = previewViewRef?.bitmap
              if (fallback != null) onInspectLiveFrame(fallback)
              else Toast.makeText(ctx, "Live capture unavailable — tap Gallery to inspect a photo", Toast.LENGTH_SHORT).show()
            }
          },
          modifier = Modifier.weight(1f).testTag("mechanic_inspect_live_bench_button"),
        ) {
          Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text(if (busy) "Inspecting..." else "Inspect Bench", maxLines = 1, softWrap = false)
        }
        TacticalOutlinedButton(
          enabled = !busy,
          onClick = {
            runCatching {
              benchGalleryFallback.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }.onFailure {
              runCatching { legacyBenchGalleryFallback.launch("image/*") }.onFailure {
                Toast.makeText(ctx, "No gallery app available on this device", Toast.LENGTH_SHORT).show()
              }
            }
          },
          modifier = Modifier.testTag("mechanic_bench_gallery_fallback_button"),
        ) {
          Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Gallery", maxLines = 1, softWrap = false)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CameraXCaptureSheet(
  onDismiss: () -> Unit,
  onCaptured: (Bitmap) -> Unit,
) {
  BackHandler(onBack = onDismiss)
  val ctx = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val imageCapture = remember { ImageCapture.Builder().build() }
  var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
  var cameraBindError by remember { mutableStateOf<String?>(null) }
  var cameraReady by remember { mutableStateOf(false) }

  DisposableEffect(lifecycleOwner) {
    onDispose {
      runCatching { ProcessCameraProvider.getInstance(ctx).get().unbindAll() }
    }
  }

  val sysCamFallback =
    rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
      if (bmp != null) onCaptured(bmp)
    }

  val legacyGalleryFallback =
    rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
      if (uri != null) {
        val bmp = decodeSampledBitmap(ctx, uri)
        if (bmp != null) onCaptured(bmp)
        else Toast.makeText(ctx, "Could not decode selected image", Toast.LENGTH_SHORT).show()
      }
    }

  val galleryFallback =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        val bmp = decodeSampledBitmap(ctx, uri)
        if (bmp != null) onCaptured(bmp)
        else Toast.makeText(ctx, "Could not decode selected image", Toast.LENGTH_SHORT).show()
      }
    }

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("CAMERAX HARDWARE OPTICS", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close Camera") }
      }
      Box(Modifier.fillMaxWidth().height(280.dp).background(Color.Black, RoundedCornerShape(12.dp))) {
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
                  provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                  cameraBindError = null
                  cameraReady = true
                }.onFailure { err ->
                  cameraBindError = err.message ?: "Camera hardware unavailable"
                  cameraReady = false
                }
              },
              ContextCompat.getMainExecutor(viewCtx),
            )
            previewView
          },
          modifier = Modifier.fillMaxSize(),
        )
        cameraBindError?.let { errMsg ->
          Text(
            text = "Camera unavailable: $errMsg\nUse Sys Cam or Gallery below.",
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.Center).padding(16.dp),
          )
        }
      }
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TacticalButton(
          onClick = {
            if (cameraBindError != null || !cameraReady) {
              val fallback = previewViewRef?.bitmap
              if (fallback != null) onCaptured(fallback)
              else runCatching { sysCamFallback.launch(null) }.onFailure {
                Toast.makeText(ctx, "Camera warming up or unavailable — tap Gallery", Toast.LENGTH_SHORT).show()
              }
              return@TacticalButton
            }
            runCatching {
              imageCapture.takePicture(
                ContextCompat.getMainExecutor(ctx),
                object : ImageCapture.OnImageCapturedCallback() {
                  override fun onCaptureSuccess(image: ImageProxy) {
                    val bmp = decodeCapturedImageProxy(image, previewViewRef)
                    image.close()
                    if (bmp != null) {
                      onCaptured(bmp)
                    } else {
                      Toast.makeText(ctx, "Frame decode failed — try Sys Cam or Gallery", Toast.LENGTH_SHORT).show()
                    }
                  }

                  override fun onError(exception: ImageCaptureException) {
                    val fallback = previewViewRef?.bitmap
                    if (fallback != null) {
                      onCaptured(fallback)
                    } else {
                      Toast.makeText(ctx, "Capture failed: ${exception.message ?: "try Sys Cam or Gallery"}", Toast.LENGTH_SHORT).show()
                    }
                  }
                },
              )
            }.onFailure {
              val fallback = previewViewRef?.bitmap
              if (fallback != null) onCaptured(fallback)
              else Toast.makeText(ctx, "Capture failed — tap Sys Cam or Gallery", Toast.LENGTH_SHORT).show()
            }
          },
          modifier = Modifier.weight(1f).height(52.dp).testTag("camerax_capture_frame_button"),
        ) {
          Icon(Icons.Default.CameraAlt, null)
          Spacer(Modifier.width(6.dp))
          Text("Capture", maxLines = 1, softWrap = false)
        }
        TacticalOutlinedButton(
          onClick = {
            runCatching { sysCamFallback.launch(null) }.onFailure {
              Toast.makeText(ctx, "System camera unavailable — try Gallery", Toast.LENGTH_SHORT).show()
            }
          },
          modifier = Modifier.height(52.dp).testTag("camerax_syscam_button"),
        ) {
          Icon(Icons.Default.CameraAlt, null, Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Sys Cam", maxLines = 1, softWrap = false)
        }
        TacticalOutlinedButton(
          onClick = {
            runCatching {
              galleryFallback.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }.onFailure {
              runCatching { legacyGalleryFallback.launch("image/*") }
            }
          },
          modifier = Modifier.height(52.dp).testTag("camerax_gallery_fallback_button"),
        ) {
          Icon(Icons.Default.PhotoLibrary, null)
          Spacer(Modifier.width(4.dp))
          Text("Gallery", maxLines = 1, softWrap = false)
        }
      }
      Spacer(Modifier.height(12.dp))
    }
  }
}

@Composable
private fun MechanicComposerActionPill(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  selected: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  val chrome = LocalTacticalChrome.current
  Surface(
    onClick = onClick,
    shape = chrome.smallShape,
    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
    border = BorderStroke(
      width = if (selected) 1.5.dp else 1.dp,
      color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
    ),
    modifier = modifier.height(48.dp),
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(horizontal = 2.dp),
    ) {
      Icon(
        icon,
        contentDescription = label,
        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(16.dp),
      )
      Spacer(Modifier.height(2.dp))
      Text(
        text = label,
        fontSize = 9.5.sp,
        lineHeight = 10.5.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
      )
    }
  }
}
