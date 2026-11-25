package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.BuildCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.PevBatteryPhysics
import com.example.data.AiVoiceProfile
import com.example.data.UiState
import com.example.data.getActiveCustomVoice
import com.example.hardware.VoiceAudioManager
import com.example.ui.components.RuralPevIcons
import com.example.ui.components.SocialShareDialog
import com.example.ui.components.SocialSharePayload
import com.example.ui.components.TacticalButton
import com.example.ui.components.TacticalCard
import com.example.ui.components.TacticalDialog
import com.example.ui.components.TacticalFilterChip
import com.example.ui.components.TacticalOutlinedButton
import com.example.ui.components.decodeSampledBitmap
import com.example.ui.components.formatSpeedValue
import com.example.ui.components.formatTempLabel
import com.example.ui.components.isWindowsClassicActive
import com.example.ui.components.speedUnitLabel
import com.example.ui.components.tacticalOutlinedTextFieldColors
import com.example.ui.theme.LocalTacticalChrome
import com.example.viewmodel.CockpitViewModel
import com.example.viewmodel.CopilotSocialViewModel
import com.example.viewmodel.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopilotScreen(
  sessionVm: SessionViewModel,
  cockpitVm: CockpitViewModel,
  socialVm: CopilotSocialViewModel,
  onOpenVoiceStudio: () -> Unit = {},
) {
  val ctx = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val haptic = LocalHapticFeedback.current
  val activeUser by sessionVm.activeUser.collectAsStateWithLifecycle()
  val profile by sessionVm.profile.collectAsStateWithLifecycle()
  val settings by sessionVm.settings.collectAsStateWithLifecycle()

  fun tapCue(cue: VoiceAudioManager.TouchCue = VoiceAudioManager.TouchCue.BUTTON_TAP) {
    if (settings.hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    socialVm.audio.playTouchSound(
      enabled = settings.touchSoundsEnabled,
      pack = settings.touchSoundPack,
      cue = cue,
      volumePct = settings.globalVolume,
    )
  }
  val telemetry by cockpitVm.telemetry.collectAsStateWithLifecycle()
  val vehicles by cockpitVm.vehicles.collectAsStateWithLifecycle()
  val weatherState by sessionVm.weatherState.collectAsStateWithLifecycle()
  val activeVideo by socialVm.activeVideo.collectAsStateWithLifecycle()

  val sessions by socialVm.chatSessions.collectAsStateWithLifecycle()
  val activeSessionId by socialVm.activeSessionId.collectAsStateWithLifecycle()
  val messages by socialVm.messages.collectAsStateWithLifecycle()
  val streamingText by socialVm.streamingText.collectAsStateWithLifecycle()
  val busy by socialVm.copilotBusy.collectAsStateWithLifecycle()
  val flightCheckState by socialVm.flightCheckState.collectAsStateWithLifecycle()
  val savedBriefings by socialVm.savedBriefings.collectAsStateWithLifecycle()
  val aiSafetyBrief by cockpitVm.aiSafetyBrief.collectAsStateWithLifecycle()
  val chrome = LocalTacticalChrome.current
  val isClassic = isWindowsClassicActive(settings)
  var dismissedVoltReply by rememberSaveable { mutableStateOf("") }
  var lastPlayedCoachTip by rememberSaveable { mutableStateOf("") }

  val effectiveUid = activeUser?.uid?.ifBlank { "local_pilot" } ?: "local_pilot"
  val personas = remember { CopilotProCatalog.personas12 }
  val skills100 = remember { CopilotProCatalog.skills100 }
  val skillCategories = remember { CopilotProCatalog.skillCategories12 }

  val activeVehicle =
    remember(vehicles, profile) {
      vehicles.firstOrNull { it.id == profile?.activeVehicleId } ?: vehicles.firstOrNull()
    }
  val activePack =
    remember(activeVehicle) {
      PevBatteryPhysics.packProfile(
        ratedVoltage = activeVehicle?.voltage ?: 48.0,
        chemistry = activeVehicle?.batteryChemistry ?: "NMC",
      )
    }
  val activeVehicleSummary =
    activeVehicle?.let {
      "${it.name} (${it.category}, ${it.voltage.toInt()}V ${activePack.seriesCells}S ${activePack.chemistry}, Full ${"%.1f".format(activePack.fullChargeV)}V / Cutoff ${"%.1f".format(activePack.cutoffEmptyV)}V, ${it.batteryWh}Wh, ${it.tireSize}, ${it.targetPsi.toInt()}PSI)"
    } ?: "Universal PEV (48V 13S NMC, Full 54.6V / Cutoff 39.0V)"

  var activePersona by remember(settings.copilotPersona) {
    mutableStateOf(personas.firstOrNull { it.name == settings.copilotPersona } ?: personas.first())
  }
  var prompt by rememberSaveable { mutableStateOf("") }
  var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var useWebSearch by remember(settings.aiWebGrounding) { mutableStateOf(settings.aiWebGrounding) }
  var autoSpeak by remember(settings.autoSpeakAiReplies) { mutableStateOf(settings.autoSpeakAiReplies) }
  var verbosity by rememberSaveable { mutableStateOf("Balanced") }
  var customDirective by rememberSaveable { mutableStateOf("") }

  // Collapsible tools panel keeps 75-85% of viewport open for reading & writing chat!
  var showToolsDrawer by remember(settings.aiChatCompactTools) { mutableStateOf(!settings.aiChatCompactTools) }

  // LIVE CONTEXT INJECTOR TOGGLES
  var injectTelemetry by rememberSaveable { mutableStateOf(true) }
  var injectVehicle by rememberSaveable { mutableStateOf(true) }
  var injectWeather by rememberSaveable { mutableStateOf(true) }
  var injectRoadLimit by rememberSaveable { mutableStateOf(true) }

  var showSkillsModal by remember { mutableStateOf(false) }
  var skillCategoryFilter by rememberSaveable { mutableStateOf("All") }
  var skillSearch by rememberSaveable { mutableStateOf("") }
  var showSessionsSheet by remember { mutableStateOf(false) }
  var showSettingsDialog by remember { mutableStateOf(false) }
  var showBriefingsVault by remember { mutableStateOf(false) }
  var showWizardsDialog by remember { mutableStateOf(false) }
  var showCameraSheet by remember { mutableStateOf(false) }
  var showCameraFallbackDialog by remember { mutableStateOf(false) }
  var showMicFallbackDialog by remember { mutableStateOf(false) }
  var showLiveArHud by remember { mutableStateOf(false) }
  var sharePayload by remember { mutableStateOf<SocialSharePayload?>(null) }

  val chatListState = rememberLazyListState()

  val quickSystemCameraLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
      if (bmp != null) {
        attachedBitmap = bmp
        Toast.makeText(ctx, "Photo snapped — tap Analyze or Send", Toast.LENGTH_SHORT).show()
      }
    }

  val camPermLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        showCameraSheet = true
      } else {
        showCameraFallbackDialog = true
      }
    }

  val legacyContentPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
      if (uri != null) {
        val decoded = decodeSampledBitmap(ctx, uri)
        if (decoded != null) {
          attachedBitmap = decoded
          Toast.makeText(ctx, "Photo attached — tap Analyze or Send", Toast.LENGTH_SHORT).show()
        } else {
          Toast.makeText(ctx, "Could not load selected image — try another photo", Toast.LENGTH_SHORT).show()
        }
      }
    }

  val photoPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        val decoded = decodeSampledBitmap(ctx, uri)
        if (decoded != null) {
          attachedBitmap = decoded
          Toast.makeText(ctx, "Photo attached — tap Analyze or Send", Toast.LENGTH_SHORT).show()
        } else {
          Toast.makeText(ctx, "Could not load selected image — try another photo", Toast.LENGTH_SHORT).show()
        }
      }
    }

  fun openPhotoGallery() {
    runCatching {
      photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }.onFailure {
      runCatching { legacyContentPicker.launch("image/*") }.onFailure {
        Toast.makeText(ctx, "No gallery app available on this device", Toast.LENGTH_SHORT).show()
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

  val remainingWh = ((activeVehicle?.batteryWh ?: 1000).toDouble() * (telemetry.batteryPct / 100.0))
  val baseTelemetryCtx =
    "Speed=${"%.1f".format(formatSpeedValue(telemetry.speedKmh, settings.distanceUnit))} ${speedUnitLabel(settings.distanceUnit)}, " +
      "Bat=${telemetry.batteryPct}% (${"%.0f".format(remainingWh)}Wh left), " +
      "Pack=${activePack.seriesCells}S ${activePack.chemistry} Volts=${"%.1f".format(telemetry.estimatedVoltage)}V (Full=${"%.1f".format(activePack.fullChargeV)}V, Low20%=${"%.1f".format(activePack.lowWarningV)}V, Cutoff=${"%.1f".format(activePack.cutoffEmptyV)}V), " +
      "Trip=${"%.2f".format(telemetry.tripDistanceKm)}km, Pitch=${"%.1f".format(telemetry.pitchDeg)}°, " +
      "Roll=${"%.1f".format(telemetry.rollDeg)}°, Yaw=${telemetry.yawDeg.toInt()}°, Road=${telemetry.roadName}"
  val fullInjectedTelemetry =
    buildList {
      if (injectTelemetry) add(baseTelemetryCtx)
      if (injectVehicle) add("Vehicle=$activeVehicleSummary")
      if (injectRoadLimit && telemetry.speedLimitMph != null) add("RoadLimit=${telemetry.speedLimitMph}mph")
    }.joinToString(" | ").ifBlank { "Telemetry paused" }

  val weatherCtx =
    if (!injectWeather) "Weather context muted"
    else (weatherState as? UiState.Success)?.data?.let {
      "${it.conditionLabel}, ${formatTempLabel(it.tempC, settings.tempUnit)}, Wind ${it.windSpeedKmh}km/h, Rain ${it.precipProbPct}%"
    } ?: "Weather syncing"

  fun submitPrompt(textToSend: String) {
    val clean = textToSend.trim()
    if (clean.isBlank() && attachedBitmap == null) {
      Toast.makeText(ctx, "Type a question, tap a skill, or attach a photo first", Toast.LENGTH_SHORT).show()
      return
    }
    tapCue(VoiceAudioManager.TouchCue.MESSAGE_SENT)
    val bmp = attachedBitmap
    attachedBitmap = null
    prompt = ""
    showToolsDrawer = false
    val ruralFlavorDirective =
      buildString {
        if (settings.ruralPersonaMode) {
          append("Keep a warm, neighborly, down-home rural tone that makes the rider feel welcomed and grounded on every road or trail. Voice style: ${settings.aiVoiceProfile.label}. ")
        }
        if (customDirective.isNotBlank()) append(customDirective)
      }
    socialVm.sendCopilotPrompt(
      uid = effectiveUid,
      prompt = clean.ifBlank { "Analyze attached PEV photo" },
      bitmap = bmp,
      personaName = activePersona.name,
      personaPrompt = activePersona.systemPrompt,
      telemetryContext = fullInjectedTelemetry,
      weatherContext = weatherCtx,
      useWebSearch = useWebSearch,
      verbosity = verbosity,
      customDirective = ruralFlavorDirective,
      autoSpeak = autoSpeak,
      volumePct = settings.globalVolume,
      modelId = settings.aiModelId,
      temperature = settings.aiCreativity,
      thinkingLevel = settings.aiThinkingLevel,
      detailLevel = settings.aiDetailLevel,
      voiceProfile = settings.aiVoiceProfile,
    )
  }

  // Smooth auto-scroll whenever messages arrive or stream
  LaunchedEffect(messages.size, streamingText.length, busy, settings.aiChatAutoScroll) {
    if (settings.aiChatAutoScroll) {
      val target = (messages.size + if (streamingText.isNotBlank() || busy) 1 else 0).coerceAtLeast(1) - 1
      if (target >= 0) {
        runCatching { chatListState.animateScrollToItem(target) }
      }
    }
  }

  Box(Modifier.fillMaxSize()) {
  Column(Modifier.fillMaxSize().padding(bottom = if (activeVideo != null) 74.dp else 0.dp)) {
    // COMPACT TACTICAL HEADER WITH FULL-WIDTH SCROLLABLE ACTION BAR (NO CRAMPED/CLIPPED ICONS)
    Surface(
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Row(
          Modifier.fillMaxWidth().padding(horizontal = 14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(Modifier.weight(1f)) {
            Text(
              text = "BACKROAD CO-PILOT • ${activePersona.name}",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Black,
              color = MaterialTheme.colorScheme.primary,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
            )
            val satPart = if (telemetry.satellitesVisible > 0) " • ${telemetry.satellitesUsed} Sats" else ""
            Text(
              text = "${telemetry.batteryPct}% (${"%.1f".format(telemetry.estimatedVoltage)}V/${"%.1f".format(activePack.fullChargeV)}V)$satPart • ${if (busy) "Writing reply..." else "${messages.size} msgs"}",
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
                socialVm.cancelCopilotPrompt()
                Toast.makeText(ctx, "Stopped active Co-Pilot response", Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier.testTag("copilot_header_stop_button"),
            ) {
              Icon(Icons.Default.Stop, contentDescription = "Stop AI", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
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
            AssistChip(
              onClick = {
                submitPrompt(
                  "Run a live Rural Range & Return Check for my ${activeVehicleSummary} right now at ${"%.1f".format(telemetry.estimatedVoltage)}V (${telemetry.batteryPct}% SoC, ~${"%.0f".format(remainingWh)}Wh remaining; 20% turnaround reserve=${"%.1f".format(activePack.lowWarningV)}V, 0% cutoff=${"%.1f".format(activePack.cutoffEmptyV)}V). Factor in today's wind/temperature ($weatherCtx) and give exact safe outbound vs return miles on pavement vs gravel."
                )
              },
              leadingIcon = { Icon(Icons.Default.Route, contentDescription = "Rural Range & Return Check", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp)) },
              label = { Text("Range Check", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
              modifier = Modifier.testTag("copilot_range_check_button"),
            )
          }
          item {
            TacticalFilterChip(
              selected = showToolsDrawer,
              onClick = {
                tapCue()
                showToolsDrawer = !showToolsDrawer
              },
              leadingIcon = if (showToolsDrawer) Icons.Default.ExpandLess else Icons.Default.Tune,
              label = if (showToolsDrawer) "Hide Tools" else "Personas",
              modifier = Modifier.testTag("copilot_tools_toggle_button"),
            )
          }
          item {
            AssistChip(
              onClick = {
                tapCue()
                showWizardsDialog = true
              },
              leadingIcon = { Icon(Icons.Default.BuildCircle, contentDescription = "Interactive Wizards", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(15.dp)) },
              label = { Text("Wizards", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
              modifier = Modifier.testTag("copilot_wizards_button"),
            )
          }
          item {
            AssistChip(
              onClick = {
                tapCue()
                showSkillsModal = true
              },
              leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = "100 Skill Matrix", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp)) },
              label = { Text("100 Skills", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
              modifier = Modifier.testTag("copilot_skills_button"),
            )
          }
          item {
            AssistChip(
              onClick = {
                tapCue()
                showBriefingsVault = true
              },
              leadingIcon = { Icon(Icons.Default.Bookmarks, contentDescription = "Saved AI Briefings Vault", modifier = Modifier.size(15.dp)) },
              label = { Text("Vault (${savedBriefings.size})", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
              modifier = Modifier.testTag("copilot_vault_button"),
            )
          }
          item {
            AssistChip(
              onClick = {
                tapCue()
                showSessionsSheet = true
              },
              leadingIcon = { Icon(Icons.Default.History, contentDescription = "Chat History", modifier = Modifier.size(15.dp)) },
              label = { Text("History", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
              modifier = Modifier.testTag("copilot_sessions_button"),
            )
          }
          item {
            AssistChip(
              onClick = {
                tapCue()
                showSettingsDialog = true
              },
              leadingIcon = { Icon(Icons.Default.Settings, contentDescription = "Co-Pilot Calibration", modifier = Modifier.size(15.dp)) },
              label = { Text("Calibration", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
              modifier = Modifier.testTag("copilot_settings_button"),
            )
          }
          item {
            AssistChip(
              onClick = {
                tapCue()
                onOpenVoiceStudio()
              },
              leadingIcon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = "Custom Voice Studio", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary) },
              label = { Text("Voice Studio", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
              modifier = Modifier.testTag("copilot_header_voice_studio_button"),
            )
          }
        }
      }
    }

    // COLLAPSIBLE PERSONAS, CONTEXT INJECTORS & LIVE QUICK ACTIONS DRAWER
    AnimatedVisibility(visible = showToolsDrawer) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          // 20 EXPERT & RURAL PEV PERSONAS
          LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(personas, key = { it.id }) { p ->
              TacticalFilterChip(
                selected = activePersona.id == p.id,
                onClick = {
                  activePersona = p
                  sessionVm.updateSettings { it.copy(copilotPersona = p.name) }
                  Toast.makeText(ctx, "Persona: ${p.name}", Toast.LENGTH_SHORT).show()
                },
                label = p.name,
              )
            }
          }

          // 8 AI VOICE TYPES + RURAL FEEL TOGGLE + CUSTOM VOICE STUDIO
          val activeCustomVoice = settings.getActiveCustomVoice()
          LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
              TacticalFilterChip(
                selected = settings.ruralPersonaMode,
                onClick = {
                  val next = !settings.ruralPersonaMode
                  sessionVm.updateSettings { it.copy(ruralPersonaMode = next) }
                  Toast.makeText(ctx, if (next) "Rural hospitality voice ON" else "Standard voice mode", Toast.LENGTH_SHORT).show()
                },
                leadingIcon = RuralPevIcons.BarnShop,
                label = if (settings.ruralPersonaMode) "Rural Feel ✓" else "Rural Feel",
              )
            }
            if (activeCustomVoice != null) {
              item {
                TacticalFilterChip(
                  selected = true,
                  onClick = {
                    val ok = socialVm.audio.previewCustomVoice(activeCustomVoice, settings.globalVolume)
                    Toast.makeText(ctx, if (ok) "Custom voice: ${activeCustomVoice.name}" else "Custom voice set", Toast.LENGTH_SHORT).show()
                  },
                  leadingIcon = Icons.Default.RecordVoiceOver,
                  label = "Custom: ${activeCustomVoice.name} ✓",
                )
              }
            }
            items(AiVoiceProfile.entries, key = { it.id }) { vp ->
              TacticalFilterChip(
                selected = activeCustomVoice == null && settings.aiVoiceProfile == vp,
                onClick = {
                  sessionVm.updateSettings { it.copy(aiVoiceProfile = vp, activeCustomVoiceId = "") }
                  val ok = socialVm.audio.previewVoiceProfile(vp, settings.globalVolume)
                  Toast.makeText(
                    ctx,
                    if (ok) "Voice set: ${vp.label}" else "Voice set: ${vp.label} (check device volume)",
                    Toast.LENGTH_SHORT,
                  ).show()
                },
                leadingIcon = Icons.Default.RecordVoiceOver,
                label = vp.label,
              )
            }
            item {
              AssistChip(
                onClick = {
                  socialVm.audio.stopSpeaking()
                  Toast.makeText(ctx, "Voice playback stopped", Toast.LENGTH_SHORT).show()
                },
                leadingIcon = { Icon(Icons.Default.StopCircle, null, Modifier.size(14.dp)) },
                label = { Text("Stop Voice", style = MaterialTheme.typography.labelSmall) },
              )
            }
          }

          // LIVE CONTEXT INJECTOR CHIPS
          LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
              TacticalFilterChip(
                selected = injectTelemetry,
                onClick = {
                  injectTelemetry = !injectTelemetry
                  Toast.makeText(ctx, if (injectTelemetry) "Live 9-DOF & GPS context ON" else "Live 9-DOF & GPS context OFF", Toast.LENGTH_SHORT).show()
                },
                label = "9-DOF/GPS",
              )
            }
            item {
              TacticalFilterChip(
                selected = injectVehicle,
                onClick = {
                  injectVehicle = !injectVehicle
                  Toast.makeText(ctx, if (injectVehicle) "Active PEV specs attached" else "Active PEV specs muted", Toast.LENGTH_SHORT).show()
                },
                label = "Active PEV",
              )
            }
            item {
              TacticalFilterChip(
                selected = injectWeather,
                onClick = {
                  injectWeather = !injectWeather
                  Toast.makeText(ctx, if (injectWeather) "Live Weather context ON" else "Live Weather context OFF", Toast.LENGTH_SHORT).show()
                },
                label = "Weather",
              )
            }
            item {
              TacticalFilterChip(
                selected = injectRoadLimit,
                onClick = {
                  injectRoadLimit = !injectRoadLimit
                  Toast.makeText(ctx, if (injectRoadLimit) "Road Speed Limit context ON" else "Road Speed Limit context OFF", Toast.LENGTH_SHORT).show()
                },
                label = "Road Limit",
              )
            }
            item {
              TacticalFilterChip(
                selected = autoSpeak,
                onClick = {
                  autoSpeak = !autoSpeak
                  sessionVm.updateSettings { it.copy(autoSpeakAiReplies = autoSpeak) }
                  Toast.makeText(ctx, if (autoSpeak) "Auto-Speak AI replies ON" else "Auto-Speak AI replies OFF", Toast.LENGTH_SHORT).show()
                },
                label = if (autoSpeak) "Auto-Speak ✓" else "Auto-Speak",
              )
            }
            item {
              AssistChip(
                onClick = {
                  socialVm.createCopilotSession(effectiveUid, "New Tactical Session", activePersona.name)
                  Toast.makeText(ctx, "Started fresh Co-Pilot thread", Toast.LENGTH_SHORT).show()
                },
                leadingIcon = { Icon(Icons.Default.AddComment, null, Modifier.size(14.dp)) },
                label = { Text("New Chat", style = MaterialTheme.typography.labelSmall) },
              )
            }
          }

          // 8 LIVE AI QUICK ACTION PILLS (EVERY TAP STREAMS IMMEDIATELY)
          val proAiQuickActions =
            remember {
              listOf(
                "Route & Range AI" to "Calculate a safe round-trip route & charging margin for my active PEV given my current battery % and today's wind/temperature.",
                "VESC Amp Tune" to "Suggest safe VESC battery amps, motor phase amps, and 85% duty cycle cutoff limits for my active PEV.",
                "Used PEV Appraisal" to "Estimate fair market value, depreciation checks, and red flags when buying or selling a used version of my active PEV.",
                "Cell Health Audit" to "Analyze my pack voltage and nominal specs to give a cell-balance, storage voltage, and cycle-life preservation plan.",
                "Trail Legal Check" to "Summarize e-scooter, e-bike Class 1/2/3, and EUC trail/road legality, wattage limits, and helmet rules to watch for.",
                "Tire PSI & Cold Prep" to "Give exact cold tire PSI and cold-weather battery warm-up steps for today's temperature and road conditions.",
                "Convoy Ride Plan" to "Draft a group PEV convoy ride plan with sweep/lead roles, charge-stop spacing, and hand-signal safety briefing.",
                "Efficiency Coach" to "Coach me on throttle modulation, regen braking strength, tire PSI, and aero tuck to maximize Wh/mile efficiency.",
              )
            }
          LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(proAiQuickActions) { (label, promptText) ->
              AssistChip(
                onClick = { submitPrompt(promptText) },
                leadingIcon = { Icon(Icons.Default.AutoAwesome, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                label = { Text(label, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.testTag("copilot_pro_ai_${label.lowercase().replace(" ", "_")}"),
              )
            }
          }
        }
      }
    }

    val latestAssistantTip =
      messages.lastOrNull { !it.isUser }?.text?.takeIf { it.isNotBlank() }
        ?: aiSafetyBrief.takeIf { it.isNotBlank() }
    LaunchedEffect(latestAssistantTip, isClassic, settings.retroAudioCuesEnabled) {
      if (isClassic && !latestAssistantTip.isNullOrBlank() &&
        latestAssistantTip != dismissedVoltReply &&
        latestAssistantTip != lastPlayedCoachTip
      ) {
        lastPlayedCoachTip = latestAssistantTip
        socialVm.audio.playRetroAudioCue(
          enabled = settings.retroAudioCuesEnabled,
          volumePct = settings.globalVolume,
          cueType = VoiceAudioManager.RetroCueType.COACH_BRIEF,
        )
      }
    }

    // EXPANSIVE CHAT FEED (~75-85% OF SCREEN)
    LazyColumn(
      state = chatListState,
      modifier = Modifier.weight(1f).fillMaxWidth().testTag("copilot_chat_feed"),
      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      item(key = "copilot_flight_check_card") {
        TacticalCard(highlight = flightCheckState is UiState.Success, title = "Live AI Pre-Ride Flight Check") {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(Modifier.weight(1f)) {
              Text(
                "Structured Gemini readiness score for $activeVehicleSummary",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Spacer(Modifier.width(8.dp))
            TacticalButton(
              onClick = {
                tapCue(VoiceAudioManager.TouchCue.ACTION_CONFIRM)
                socialVm.runPreRideFlightCheck(
                  vehicleSummary = activeVehicleSummary,
                  telemetrySummary = fullInjectedTelemetry,
                  weatherSummary = weatherCtx,
                  riderGoal = prompt.trim().ifBlank { "Rural & street ride safety audit" },
                  modelId = settings.aiModelId,
                )
              },
              enabled = flightCheckState !is UiState.Loading,
              modifier = Modifier.testTag("copilot_run_flight_check_button"),
            ) {
              Text(if (flightCheckState is UiState.Loading) "Scanning..." else "Run Check", maxLines = 1, softWrap = false)
            }
          }
          when (val fc = flightCheckState) {
            is UiState.Loading -> {
              Spacer(Modifier.height(8.dp))
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("Analyzing pack voltage, wind, and gear readiness...", style = MaterialTheme.typography.labelSmall)
              }
            }
            is UiState.Error -> {
              Spacer(Modifier.height(6.dp))
              Text(fc.message, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
            is UiState.Success -> {
              val res = fc.data
              Spacer(Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  "READINESS ${res.readinessScore}/100",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Black,
                  color = if (res.readinessScore >= 80) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
                TacticalOutlinedButton(onClick = { socialVm.clearFlightCheck() }) {
                  Text("Dismiss", style = MaterialTheme.typography.labelSmall)
                }
              }
              Text(res.verdict, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
              Spacer(Modifier.height(4.dp))
              Text("Weather & Grip: ${res.weatherRisk}", style = MaterialTheme.typography.bodySmall)
              Text("Battery Strategy: ${res.batteryStrategy}", style = MaterialTheme.typography.bodySmall)
              Text("Gear: ${res.gearChecklist.joinToString(" • ")}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
              Text("Route Tip: ${res.routeTip}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            null -> Unit
          }
        }
      }

      if (isClassic && !latestAssistantTip.isNullOrBlank() && latestAssistantTip != dismissedVoltReply) {
        item {
          VoltTacticalCoachBubble(
            title = "VOLT • ${activePersona.name.uppercase()}",
            message = latestAssistantTip,
            onDismiss = { dismissedVoltReply = latestAssistantTip },
          )
        }
      }

      if (messages.isEmpty() && streamingText.isBlank() && !busy) {
        item {
          TacticalCard(highlight = true) {
            Text(
              "CO-PILOT LIVE • ${activePersona.name.uppercase()}",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Black,
              color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
              activePersona.roleSummary,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
              "Live telemetry wired: $activeVehicleSummary • ${telemetry.batteryPct}% • ${"%.1f".format(telemetry.estimatedVoltage)}V",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(10.dp))
            Text("INSTANT LIVE PROMPTS (TAP TO RUN):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(skills100.take(8)) { sk ->
                AssistChip(
                  onClick = { submitPrompt(sk.promptTemplate) },
                  leadingIcon = { Icon(Icons.Default.AutoAwesome, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) },
                  label = { Text(sk.title, style = MaterialTheme.typography.labelSmall) },
                )
              }
            }
          }
        }
      }

      items(messages, key = { it.id }) { m ->
        TacticalCard(highlight = !m.isUser) {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
              if (m.isUser) "YOU (RIDER)" else m.persona.uppercase(),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Black,
              color = if (m.isUser) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            if (!m.isUser) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = {
                    tapCue(VoiceAudioManager.TouchCue.ACTION_CONFIRM)
                    socialVm.saveCopilotBriefing(effectiveUid, m.text.lineSequence().firstOrNull().orEmpty().take(55), m.persona, m.text)
                    Toast.makeText(ctx, "Saved briefing to Vault", Toast.LENGTH_SHORT).show()
                  },
                  modifier = Modifier.size(48.dp).testTag("copilot_save_msg_${m.id}"),
                ) { Icon(Icons.Default.BookmarkAdd, contentDescription = "Save Briefing", modifier = Modifier.size(20.dp)) }
                IconButton(
                  onClick = {
                    tapCue(VoiceAudioManager.TouchCue.ACTION_CONFIRM)
                    clipboard.setText(AnnotatedString(m.text))
                    Toast.makeText(ctx, "Copied Co-Pilot reply", Toast.LENGTH_SHORT).show()
                  },
                  modifier = Modifier.size(48.dp).testTag("copilot_copy_msg_${m.id}"),
                ) { Icon(Icons.Default.ContentCopy, contentDescription = "Copy Reply", modifier = Modifier.size(20.dp)) }
                IconButton(
                  onClick = {
                    tapCue()
                    val customVoice = settings.getActiveCustomVoice()
                    val ok = socialVm.audio.speakWithActiveSettings(m.text, settings)
                    Toast.makeText(
                      ctx,
                      if (ok) "Speaking (${customVoice?.name ?: settings.aiVoiceProfile.label})..." else "Global Volume is 0% — raise volume in Settings",
                      Toast.LENGTH_SHORT,
                    ).show()
                  },
                  modifier = Modifier.size(48.dp).testTag("copilot_speak_msg_${m.id}"),
                ) { Icon(Icons.Default.VolumeUp, contentDescription = "Speak Reply", modifier = Modifier.size(21.dp)) }
                IconButton(
                  onClick = {
                    tapCue(VoiceAudioManager.TouchCue.TOGGLE_OFF)
                    socialVm.audio.stopSpeaking()
                    Toast.makeText(ctx, "Stopped voice", Toast.LENGTH_SHORT).show()
                  },
                  modifier = Modifier.size(48.dp).testTag("copilot_stop_voice_${m.id}"),
                ) { Icon(Icons.Default.StopCircle, contentDescription = "Stop Speaking", modifier = Modifier.size(20.dp)) }
                IconButton(
                  onClick = {
                    tapCue()
                    sharePayload = SocialSharePayload(title = "Co-Pilot Briefing (${m.persona})", text = m.text)
                  },
                  modifier = Modifier.size(48.dp).testTag("copilot_share_msg_${m.id}"),
                ) { Icon(Icons.Default.Share, contentDescription = "Share Briefing", modifier = Modifier.size(20.dp)) }
              }
            }
          }
          Spacer(Modifier.height(4.dp))
          Text(m.text, style = MaterialTheme.typography.bodyLarge)

          if (!m.isUser && !busy) {
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              item {
                AssistChip(
                  onClick = { submitPrompt("Summarize your last answer into 3 ultra-short helmet bullet points:\n${m.text.take(600)}") },
                  label = { Text("Simplify", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                )
              }
              item {
                AssistChip(
                  onClick = { submitPrompt("Go deeper with exact torque specs, voltages, or engineering math on your last answer:\n${m.text.take(600)}") },
                  label = { Text("Deep Dive", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                )
              }
              item {
                AssistChip(
                  onClick = {
                    val idx = messages.indexOfFirst { it.id == m.id }
                    val matchingUserPrompt =
                      (if (idx > 0) messages.subList(0, idx).lastOrNull { it.isUser }?.text else null)
                        ?: messages.lastOrNull { it.isUser }?.text
                        ?: "Provide a fresh, detailed alternative answer to this topic:\n${m.text.take(400)}"
                    submitPrompt(matchingUserPrompt)
                  },
                  leadingIcon = { Icon(Icons.Default.Refresh, null, Modifier.size(14.dp)) },
                  label = { Text("Regenerate", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                )
              }
            }
          }

          when (m.widgetTag) {
            "TELEMETRY" -> {
              Spacer(Modifier.height(6.dp))
              Text("LIVE HUD -> $fullInjectedTelemetry", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            "WEATHER" -> {
              Spacer(Modifier.height(6.dp))
              Text("LIVE METEO -> $weatherCtx", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
            "SOS" -> {
              Spacer(Modifier.height(6.dp))
              TacticalButton(isDestructive = true, onClick = { sessionVm.dispatchSosSms(telemetry.lat, telemetry.lng) }) {
                Text("Dispatch Emergency SOS SMS")
              }
            }
          }
        }
      }

      if (streamingText.isNotBlank()) {
        item(key = "copilot_streaming_bubble") {
          TacticalCard(highlight = true) {
            Text("${activePersona.name.uppercase()} • STREAMING LIVE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(streamingText, style = MaterialTheme.typography.bodyLarge)
          }
        }
      } else if (busy) {
        item(key = "copilot_thinking_indicator") {
          TacticalCard(highlight = true) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.5.dp)
              Text("${activePersona.name} analyzing live telemetry...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
          }
        }
      }
    }

    // STAGED PHOTO PREVIEW STRIP (VISIBLE THUMBNAIL + 1-TAP ANALYZE + REMOVE)
    attachedBitmap?.let { stagedBmp ->
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
        shape = chrome.cardShape,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Image(
            bitmap = stagedBmp.asImageBitmap(),
            contentDescription = "Attached Co-Pilot Photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(54.dp).clip(chrome.smallShape),
          )
          Column(Modifier.weight(1f)) {
            Text("Photo Attached for Vision AI", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Tap Analyze Now or type a question below", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          TacticalButton(
            onClick = { submitPrompt(prompt.ifBlank { "Examine this PEV photo in detail and tell me what you see, any wear/issues, and recommendations." }) },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier.testTag("copilot_analyze_photo_now_button"),
          ) {
            Text("Analyze", style = MaterialTheme.typography.labelSmall)
          }
          IconButton(
            onClick = {
              tapCue(VoiceAudioManager.TouchCue.TOGGLE_OFF)
              attachedBitmap = null
              Toast.makeText(ctx, "Removed attached photo", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(48.dp).testTag("copilot_remove_photo_button"),
          ) {
            Icon(Icons.Default.Close, contentDescription = "Remove Photo", modifier = Modifier.size(20.dp))
          }
        }
      }
    }

    // HIGH-CONTRAST SPACIOUS MULTI-LINE CHAT COMPOSER BAR (ZERO CLIPPING)
    Surface(
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      tonalElevation = 6.dp,
      border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
      modifier = Modifier.fillMaxWidth().imePadding(),
    ) {
      Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (useWebSearch) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
            Text(
              "Live Web Grounding ON • Replies include real-time search context",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
            )
          }
        }
        // Fitted 4-slot tool strip — every button fits 100% cleanly with reliable click targets
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          CopilotComposerActionPill(
            icon = Icons.Default.CameraAlt,
            label = "Camera",
            selected = showCameraSheet || showCameraFallbackDialog,
            modifier = Modifier.weight(1f).testTag("copilot_camera_button"),
          ) {
            tapCue(VoiceAudioManager.TouchCue.PHOTO_SHUTTER)
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
              showCameraSheet = true
            } else {
              runCatching { camPermLauncher.launch(Manifest.permission.CAMERA) }
                .onFailure { showCameraFallbackDialog = true }
            }
          }

          CopilotComposerActionPill(
            icon = Icons.Default.PhotoLibrary,
            label = if (attachedBitmap != null) "Photo ✓" else "Photo",
            selected = attachedBitmap != null,
            modifier = Modifier.weight(1f).testTag("copilot_photo_upload_button"),
          ) {
            tapCue()
            openPhotoGallery()
          }

          CopilotComposerActionPill(
            icon = Icons.Default.ViewInAr,
            label = "AR Eye",
            selected = showLiveArHud,
            modifier = Modifier.weight(1f).testTag("copilot_ar_eye_button"),
          ) {
            tapCue()
            showLiveArHud = true
          }

          CopilotComposerActionPill(
            icon = Icons.Default.Public,
            label = if (useWebSearch) "Web ON" else "Web",
            selected = useWebSearch,
            modifier = Modifier.weight(1f).testTag("copilot_web_toggle_button"),
          ) {
            useWebSearch = !useWebSearch
            tapCue(if (useWebSearch) VoiceAudioManager.TouchCue.TOGGLE_ON else VoiceAudioManager.TouchCue.TOGGLE_OFF)
            Toast.makeText(ctx, if (useWebSearch) "Live Web Grounding ON" else "Live Web Grounding OFF", Toast.LENGTH_SHORT).show()
          }
        }

        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            placeholder = { Text("Ask ${activePersona.name} anything about your rig, route, voltage, or weather...") },
            trailingIcon = {
              IconButton(
                onClick = {
                  tapCue()
                  val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to ${activePersona.name}")
                  }
                  runCatching { speechLauncher.launch(intent) }.onFailure {
                    showMicFallbackDialog = true
                  }
                },
                modifier = Modifier.size(48.dp).testTag("copilot_voice_mic_button"),
              ) {
                Icon(Icons.Default.Mic, contentDescription = "Voice Dictation", tint = MaterialTheme.colorScheme.primary)
              }
            },
            minLines = 2,
            maxLines = 5,
            colors = tacticalOutlinedTextFieldColors(),
            shape = chrome.cardShape,
            modifier = Modifier.weight(1f).testTag("copilot_prompt_input"),
          )
          TacticalButton(
            onClick = {
              if (busy) {
                tapCue(VoiceAudioManager.TouchCue.TOGGLE_OFF)
                socialVm.cancelCopilotPrompt()
                Toast.makeText(ctx, "Stopped Co-Pilot response", Toast.LENGTH_SHORT).show()
              } else if (prompt.isBlank() && attachedBitmap == null) {
                tapCue()
                showMicFallbackDialog = true
              } else {
                submitPrompt(prompt)
              }
            },
            isDestructive = busy,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(56.dp).testTag("send_copilot_button"),
          ) {
            if (busy) {
              Icon(Icons.Default.Stop, contentDescription = "Stop Co-Pilot")
            } else {
              Icon(Icons.Default.Send, contentDescription = "Send Co-Pilot")
            }
          }
        }
      }
    }
  }
  }

  if (showSkillsModal) {
    BackHandler { showSkillsModal = false }
    val filteredSkills = remember(skillCategoryFilter, skillSearch, skills100) {
      skills100.filter { sk ->
        (skillCategoryFilter == "All" || sk.category == skillCategoryFilter) &&
          (skillSearch.isBlank() || sk.title.contains(skillSearch, true) || sk.promptTemplate.contains(skillSearch, true) || sk.category.contains(skillSearch, true))
      }
    }
    ModalBottomSheet(onDismissRequest = { showSkillsModal = false }) {
      Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text("100 LIVE CO-PILOT SKILLS", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
          IconButton(onClick = { showSkillsModal = false }) { Icon(Icons.Default.Close, contentDescription = "Close Skills") }
        }
        OutlinedTextField(
          value = skillSearch,
          onValueChange = { skillSearch = it },
          leadingIcon = { Icon(Icons.Default.Search, null) },
          placeholder = { Text("Search 100 PEV skills (VESC, EUC, gravel, 72V, video)...") },
          singleLine = true,
          colors = tacticalOutlinedTextFieldColors(),
          shape = chrome.buttonShape,
          modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(skillCategories) { cat ->
            TacticalFilterChip(selected = skillCategoryFilter == cat, onClick = { skillCategoryFilter = cat }, label = cat)
          }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.height(360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          items(filteredSkills, key = { it.id }) { sk ->
            TacticalCard(onClick = {
              showSkillsModal = false
              submitPrompt(sk.promptTemplate)
            }) {
              Text("[${sk.category}] ${sk.title}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
              Text(sk.promptTemplate, style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
    }
  }

  if (showBriefingsVault) {
    BackHandler { showBriefingsVault = false }
    ModalBottomSheet(onDismissRequest = { showBriefingsVault = false }) {
      Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text("SAVED AI BRIEFINGS VAULT (${savedBriefings.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
          IconButton(onClick = { showBriefingsVault = false }) { Icon(Icons.Default.Close, "Close Vault") }
        }
        LazyColumn(modifier = Modifier.height(340.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          if (savedBriefings.isEmpty()) {
            item {
              TacticalCard {
                Text("No saved AI briefings yet. Tap the bookmark icon on any Co-Pilot reply to save it offline here.", style = MaterialTheme.typography.bodySmall)
              }
            }
          }
          items(savedBriefings, key = { it.id }) { br ->
            TacticalCard {
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${br.persona} • ${br.title}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                Row {
                  IconButton(
                    onClick = {
                      val customVoice = settings.getActiveCustomVoice()
                      val ok = socialVm.audio.speakWithActiveSettings(br.content, settings)
                      Toast.makeText(
                        ctx,
                        if (ok) "Reading briefing (${customVoice?.name ?: settings.aiVoiceProfile.label})..." else "Raise Global Volume in Settings to hear briefing",
                        Toast.LENGTH_SHORT,
                      ).show()
                    },
                  ) {
                    Icon(Icons.Default.VolumeUp, "Speak Briefing", Modifier.size(18.dp))
                  }
                  IconButton(
                    onClick = {
                      socialVm.deleteCopilotBriefing(effectiveUid, br.id)
                      Toast.makeText(ctx, "Deleted saved briefing", Toast.LENGTH_SHORT).show()
                    },
                  ) {
                    Icon(Icons.Default.Delete, "Delete Briefing", Modifier.size(18.dp))
                  }
                }
              }
              Text(br.content, style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
    }
  }

  if (showWizardsDialog) {
    CopilotInteractiveWizardsDialog(
      activeVehicleSummary = activeVehicleSummary,
      telemetryCtx = fullInjectedTelemetry,
      weatherCtx = weatherCtx,
      onDismiss = { showWizardsDialog = false },
      onLaunchPrompt = { submitPrompt(it) },
    )
  }

  if (showSessionsSheet) {
    BackHandler { showSessionsSheet = false }
    var sessionSearch by remember { mutableStateOf("") }
    val filteredSessions =
      remember(sessions, sessionSearch) {
        sessions.filter { s ->
          sessionSearch.isBlank() ||
            s.title.contains(sessionSearch, true) ||
            s.persona.contains(sessionSearch, true)
        }
      }
    ModalBottomSheet(onDismissRequest = { showSessionsSheet = false }) {
      Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text("CHAT HISTORY (${sessions.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (messages.isNotEmpty()) {
              IconButton(
                onClick = {
                  val transcript =
                    buildString {
                      appendLine("=== RURAL ERIDES GO CO-PILOT TRANSCRIPT ===")
                      appendLine("Persona: ${activePersona.name}")
                      appendLine()
                      messages.forEach { msg ->
                        appendLine(if (msg.isUser) "[PILOT]: ${msg.text}" else "[${msg.persona.uppercase()}]: ${msg.text}")
                        appendLine()
                      }
                    }
                  val shareIntent =
                    Intent(Intent.ACTION_SEND).apply {
                      type = "text/plain"
                      putExtra(Intent.EXTRA_SUBJECT, "Rural ERides GO Co-Pilot Transcript")
                      putExtra(Intent.EXTRA_TEXT, transcript)
                    }
                  runCatching { ctx.startActivity(Intent.createChooser(shareIntent, "Export Co-Pilot Chat")) }
                },
                modifier = Modifier.testTag("export_copilot_chat_button"),
              ) {
                Icon(Icons.Default.Share, contentDescription = "Export Active Chat", tint = MaterialTheme.colorScheme.secondary)
              }
            }
            TacticalButton(
              onClick = {
                socialVm.createCopilotSession(effectiveUid, "New Tactical Session", activePersona.name)
                showSessionsSheet = false
              },
              modifier = Modifier.testTag("new_copilot_session_button"),
            ) {
              Icon(Icons.Default.AddComment, null, Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("New")
            }
          }
        }
        OutlinedTextField(
          value = sessionSearch,
          onValueChange = { sessionSearch = it },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          placeholder = { Text("Search Co-Pilot chat history by title or persona...") },
          singleLine = true,
          colors = tacticalOutlinedTextFieldColors(),
          shape = chrome.buttonShape,
          modifier = Modifier.fillMaxWidth().testTag("copilot_sessions_search_input"),
        )
        LazyColumn(modifier = Modifier.height(290.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          items(filteredSessions, key = { it.id }) { s ->
            TacticalCard(highlight = s.id == activeSessionId, onClick = {
              socialVm.selectCopilotSession(s.id)
              showSessionsSheet = false
            }) {
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                  Text(s.title, style = MaterialTheme.typography.titleMedium)
                  Text(s.persona, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { socialVm.deleteCopilotSession(s.id) }) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete Session", tint = MaterialTheme.colorScheme.onSurface)
                }
              }
            }
          }
        }
      }
    }
  }

  if (showSettingsDialog) {
    TacticalDialog(
      onDismissRequest = { showSettingsDialog = false },
      title = "Co-Pilot Calibration",
      subtitle = "Tune answer depth, toolbar layout & riding directive",
      icon = Icons.Default.Tune,
      confirmButton = { TacticalButton(onClick = { showSettingsDialog = false }) { Text("Done") } },
    ) {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text("Compact Tools Drawer")
          Switch(
            checked = settings.aiChatCompactTools,
            onCheckedChange = { v ->
              sessionVm.updateSettings { it.copy(aiChatCompactTools = v) }
              showToolsDrawer = !v
            },
          )
        }
        Text("Response Depth", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("Concise", "Balanced", "Deep Tech").forEach { v ->
            TacticalFilterChip(selected = verbosity == v, onClick = { verbosity = v }, label = v)
          }
        }
        OutlinedTextField(
          value = customDirective,
          onValueChange = { customDirective = it },
          label = { Text("Custom Vehicle / Riding Directive") },
          colors = tacticalOutlinedTextFieldColors(),
          shape = chrome.buttonShape,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }

  if (showCameraSheet) {
    CameraXCaptureSheet(
      onDismiss = { showCameraSheet = false },
      onCaptured = { bmp ->
        attachedBitmap = bmp
        showCameraSheet = false
        Toast.makeText(ctx, "Camera frame attached — tap Analyze or Send", Toast.LENGTH_SHORT).show()
      },
    )
  }

  if (showCameraFallbackDialog) {
    TacticalDialog(
      onDismissRequest = { showCameraFallbackDialog = false },
      title = "Camera Access & Capture Options",
      subtitle = "Snap a live photo or pick from your gallery",
      icon = Icons.Default.CameraAlt,
      confirmButton = {
        TacticalButton(
          onClick = {
            showCameraFallbackDialog = false
            runCatching { quickSystemCameraLauncher.launch(null) }.onFailure {
              openPhotoGallery()
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
          "In-app CameraX requires Camera permission in Android Settings, or you can capture right now using your System Camera or Gallery.",
          style = MaterialTheme.typography.bodySmall,
        )
        TacticalOutlinedButton(
          onClick = {
            showCameraFallbackDialog = false
            openPhotoGallery()
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(Icons.Default.PhotoLibrary, null, Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("Pick Photo from Gallery")
        }
      }
    }
  }

  if (showMicFallbackDialog) {
    val quickVoicePrompts = listOf(
      "How many safe miles do I have left on gravel at this voltage?",
      "Check wind, temperature, and storm risk for the next 2 hours.",
      "Should I slow down or lower pas/regen for battery health right now?",
      "Plan a safe turnaround point & charging stop for my active PEV.",
    )
    TacticalDialog(
      onDismissRequest = { showMicFallbackDialog = false },
      title = "Quick Ask ${activePersona.name}",
      subtitle = "Tap any instant tactical question to run right now",
      icon = Icons.Default.AutoAwesome,
      confirmButton = { TacticalButton(onClick = { showMicFallbackDialog = false }) { Text("Close") } },
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        quickVoicePrompts.forEach { q ->
          Surface(
            onClick = {
              showMicFallbackDialog = false
              submitPrompt(q)
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

  if (showLiveArHud) {
    LiveArHudOverlay(
      telemetry = telemetry,
      settings = settings,
      initialMode = ArHudMode.OPTICAL_INSPECT,
      onCaptureForVisionAi = { bmp ->
        attachedBitmap = bmp
        showLiveArHud = false
        Toast.makeText(ctx, "AR frame attached to Co-Pilot — tap Analyze or Send", Toast.LENGTH_SHORT).show()
      },
      onDismiss = { showLiveArHud = false },
    )
  }

  sharePayload?.let { payload ->
    SocialShareDialog(payload = payload, onDismiss = { sharePayload = null })
  }
}

@Composable
private fun CopilotComposerActionPill(
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
    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
    border = BorderStroke(
      width = if (selected) 1.5.dp else 1.dp,
      color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
    ),
    modifier = modifier.height(48.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier.padding(horizontal = 4.dp),
    ) {
      Icon(
        icon,
        contentDescription = label,
        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(16.dp),
      )
      Spacer(Modifier.width(4.dp))
      Text(
        text = label,
        fontSize = 10.5.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
      )
    }
  }
}
