package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.AiComposedTrack
import com.example.ai.AudioTranscriptionResult
import com.example.ai.GeminiModelCatalog
import com.example.ai.GeminiStudioEngine
import com.example.ai.GeneratedImageResult
import com.example.ai.GroundedExploreResult
import com.example.ai.VoiceChatTurn
import com.example.data.UiState
import com.example.hardware.TrailAudioStudioEngine
import com.example.ui.components.TacticalButton
import com.example.ui.components.TacticalCard
import com.example.ui.components.TacticalOutlinedButton
import com.example.viewmodel.CockpitViewModel
import com.example.viewmodel.CopilotSocialViewModel
import com.example.viewmodel.SessionViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class StudioHubTab(val label: String, val icon: ImageVector, val accent: Color) {
  IMAGE_STUDIO("AI Image", Icons.Default.Image, Color(0xFF00C2FF)),
  VOICE_LIVE("Voice AI", Icons.Default.Mic, Color(0xFF39FF14)),
  SEARCH_MAPS("Maps & Web", Icons.Default.Public, Color(0xFFF59E0B)),
  MUSIC_LAB("Music Lab", Icons.Default.MusicNote, Color(0xFFEC4899)),
  ARCADE_GAME("Volt Game", Icons.Default.SportsEsports, Color(0xFF10B981)),
  PEV_QUIZ("PEV Test", Icons.Default.School, Color(0xFF38BDF8)),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorArcadeHubScreen(
  sessionVm: SessionViewModel,
  cockpitVm: CockpitViewModel,
  socialVm: CopilotSocialViewModel,
  onBack: () -> Unit,
) {
  val ctx = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val scope = rememberCoroutineScope()

  val settings by sessionVm.settings.collectAsStateWithLifecycle()
  val profile by sessionVm.profile.collectAsStateWithLifecycle()
  val telemetry by cockpitVm.telemetry.collectAsStateWithLifecycle()
  val vehicles by cockpitVm.vehicles.collectAsStateWithLifecycle()
  val weatherState by sessionVm.weatherState.collectAsStateWithLifecycle()

  val activeVehicle = remember(vehicles, profile?.activeVehicleId) {
    vehicles.firstOrNull { it.id == profile?.activeVehicleId } ?: vehicles.firstOrNull()
  }
  val vehicleSummary = activeVehicle?.let { "${it.name} (${it.voltage.toInt()}V ${it.batteryWh}Wh)" } ?: "Rural Electric PEV"
  val liveLocationLabel = (weatherState as? UiState.Success)?.data?.locationLabel?.ifBlank { telemetry.roadName } ?: telemetry.roadName

  val studioAi = remember { GeminiStudioEngine() }
  val audioEngine = remember { TrailAudioStudioEngine() }
  DisposableEffect(Unit) {
    onDispose {
      audioEngine.release()
      sessionVm.voiceAudio.stopSpeaking()
    }
  }

  var activeTab by remember { mutableStateOf(StudioHubTab.IMAGE_STUDIO) }
  var launchFullGame by remember { mutableStateOf(false) }

  // 1) AI Image Studio State
  var imgPrompt by remember { mutableStateOf("") }
  var imgAspect by remember { mutableStateOf("16:9") }
  var sourceEditBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var imgBusy by remember { mutableStateOf(false) }
  var imgError by remember { mutableStateOf("") }
  var imgResult by remember { mutableStateOf<GeneratedImageResult?>(null) }

  val photoPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        scope.launch {
          val bmp =
            withContext(Dispatchers.IO) {
              runCatching {
                ctx.contentResolver.openInputStream(uri)?.use { stream ->
                  val raw = BitmapFactory.decodeStream(stream)
                  if (raw != null && (raw.width > 1280 || raw.height > 1280)) {
                    val scale = 1280f / maxOf(raw.width, raw.height)
                    Bitmap.createScaledBitmap(raw, (raw.width * scale).toInt(), (raw.height * scale).toInt(), true)
                  } else raw
                }
              }.getOrNull()
            }
          if (bmp != null) {
            sourceEditBitmap = bmp
            Toast.makeText(ctx, "Photo loaded for AI editing!", Toast.LENGTH_SHORT).show()
          }
        }
      }
    }

  // 2) Voice Live & Audio Transcribe State
  val isRecording by audioEngine.isRecording.collectAsStateWithLifecycle()
  val micLevel by audioEngine.micLevel.collectAsStateWithLifecycle()
  val recSeconds by audioEngine.recordedDurationSec.collectAsStateWithLifecycle()
  var voiceModeTranscribeOnly by remember { mutableStateOf(false) }
  var selectedVoiceModel by remember { mutableStateOf(GeminiModelCatalog.GEMINI_3_8_FLASH) }
  var voicePersona by remember { mutableStateOf("Rural Trail Navigator") }
  var autoSpeakVoiceReply by remember { mutableStateOf(true) }
  var voiceTextInput by remember { mutableStateOf("") }
  var voiceBusy by remember { mutableStateOf(false) }
  var voiceError by remember { mutableStateOf("") }
  var lastTranscription by remember { mutableStateOf<AudioTranscriptionResult?>(null) }
  val voiceHistory = remember { mutableStateListOf<VoiceChatTurn>() }

  val micPermLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        audioEngine.startMicRecording()
      } else {
        voiceError = "Microphone permission is required to record audio for transcription & live voice."
      }
    }

  // 3) Search & Maps Grounded Explorer State
  var exploreQuery by remember { mutableStateOf("") }
  var useGoogleSearch by remember { mutableStateOf(true) }
  var useMapPlaces by remember { mutableStateOf(true) }
  var exploreBusy by remember { mutableStateOf(false) }
  var exploreError by remember { mutableStateOf("") }
  var exploreResult by remember { mutableStateOf<GroundedExploreResult?>(null) }

  // 4) AI Trail Music Lab State
  var musicPrompt by remember { mutableStateOf("Sunset gravel backroad cruise on a 72V electric rig") }
  var musicGenre by remember { mutableStateOf("Country Acoustic") }
  var musicBusy by remember { mutableStateOf(false) }
  var musicError by remember { mutableStateOf("") }
  var composedTrack by remember { mutableStateOf<AiComposedTrack?>(null) }
  val isPlayingMusic by audioEngine.isPlayingMusic.collectAsStateWithLifecycle()
  val musicProgress by audioEngine.musicProgress.collectAsStateWithLifecycle()

  if (launchFullGame) {
    GravelVoltGameScreen(
      callsign = profile?.callsign?.ifBlank { "PILOT" } ?: "PILOT",
      vehicleName = activeVehicle?.name ?: "Rural Hyper-PEV",
      voiceAudio = sessionVm.voiceAudio,
      onShareHighScore = { pts, mi ->
        socialVm.createPost(
          callsign = profile?.callsign?.ifBlank { "PILOT" } ?: "PILOT",
          badge = settings.pilotRankBadge,
          category = "RIDE_REPORTS",
          title = "Gravel Volt Runner High Score: $pts PTS!",
          body = "Just ran ${"%.2f".format(mi)} miles on Gravel Volt Runner with ${activeVehicle?.name ?: "my rig"} — beat my score of $pts PTS!",
          bitmap = null,
          pollQuestion = "",
          pollOptions = emptyList(),
          priceUsd = 0.0,
          location = liveLocationLabel,
          telemetrySummary = "$pts PTS • ${"%.2f".format(mi)} mi Arcade Run",
          helpNeeded = false,
          speedTrap = false,
          stolenAlert = false,
          onDone = { Toast.makeText(ctx, "High score posted to Porch Board!", Toast.LENGTH_SHORT).show() },
        )
      },
      onBack = { launchFullGame = false },
    )
    return
  }

  BackHandler { onBack() }

  fun stopMicAndProcess() {
    val wav = audioEngine.stopMicRecordingAndGetWav()
    if (wav.size < 256) {
      voiceError = "Recording was too short — hold & speak for at least 1 second."
      return
    }
    voiceBusy = true
    voiceError = ""
    scope.launch {
      if (voiceModeTranscribeOnly) {
        val res = studioAi.transcribeAudioWav(wav, vehicleSummary)
        res.fold(
          onSuccess = { tr ->
            lastTranscription = tr
            voiceTextInput = tr.transcript
          },
          onFailure = { e -> voiceError = e.localizedMessage ?: "Audio transcription failed." },
        )
      } else {
        voiceHistory += VoiceChatTurn(id = UUID.randomUUID().toString(), isUser = true, text = "🎤 Voice Audio (${"%.1f".format(recSeconds)}s)")
        val res =
          studioAi.runVoiceConversationTurn(
            userMessage = "",
            audioWavBytes = wav,
            history = voiceHistory.toList(),
            personaRole = voicePersona,
            modelId = selectedVoiceModel,
            vehicleContext = vehicleSummary,
            locationLabel = liveLocationLabel,
          )
        res.fold(
          onSuccess = { (reply, modelUsed) ->
            voiceHistory += VoiceChatTurn(id = UUID.randomUUID().toString(), isUser = false, text = reply, modelUsed = modelUsed)
            if (autoSpeakVoiceReply) {
              sessionVm.voiceAudio.speak(
                text = reply,
                volumePct = settings.globalVolume,
                speechRate = settings.ttsSpeechRate,
                pitch = settings.ttsPitch,
              )
            }
          },
          onFailure = { e -> voiceError = e.localizedMessage ?: "Live voice turn failed." },
        )
      }
      voiceBusy = false
    }
  }

  Scaffold(
    topBar = {
      com.example.ui.components.TacticalTopBar(
        title = "Rural ERides AI & Arcade Studio",
        subtitle = activeTab.label,
        onBack = onBack,
        backTestTag = "studio_hub_back_btn",
        badgeText = "STUDIO",
      )
    },
  ) { pad ->
    Column(modifier = Modifier.fillMaxSize().padding(pad)) {
      // Fitted 5-tab horizontal studio selector
      LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        items(StudioHubTab.entries) { tab ->
          val selected = activeTab == tab
          com.example.ui.components.TacticalFilterChip(
            selected = selected,
            onClick = { activeTab = tab },
            leadingIcon = tab.icon,
            label = tab.label,
            modifier = Modifier.testTag("studio_tab_${tab.name.lowercase()}"),
          )
        }
      }

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        when (activeTab) {
          StudioHubTab.IMAGE_STUDIO -> {
                item {
                  TacticalCard(highlight = true, title = "AI IMAGE CREATION & PHOTO EDIT (Nano Banana 2.1)") {
                Text(
                  "Create custom rural PEV liveries, trail posters, or upload a photo of your rig to restyle it with Gemini Nano Banana 2.1.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                  value = imgPrompt,
                  onValueChange = { imgPrompt = it },
                  label = { Text(if (sourceEditBitmap != null) "Describe how to edit your photo..." else "Describe the PEV image to create...") },
                  placeholder = { Text("e.g., 72V dual-motor hyper-scooter parked by a red rural barn at golden sunset, neon lime accents") },
                  minLines = 2,
                  maxLines = 4,
                  modifier = Modifier.fillMaxWidth().testTag("studio_img_prompt_input"),
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  val quickIdeas =
                    listOf(
                      "Custom matte-green 72V eMoto on misty pine gravel road at sunset",
                      "Neon lime PEV crew patch emblem with lightning bolt & pine trees",
                      "Retro 1995 garage blueprint poster of a dual-motor off-road scooter",
                    )
                  items(quickIdeas) { idea ->
                    FilterChip(
                      selected = false,
                      onClick = { imgPrompt = idea },
                      label = { Text(idea.take(28) + "…", maxLines = 1, softWrap = false) },
                    )
                  }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  listOf("1:1", "16:9", "9:16").forEach { ratio ->
                    FilterChip(
                      selected = imgAspect == ratio,
                      onClick = { imgAspect = ratio },
                      label = { Text(ratio, maxLines = 1, softWrap = false) },
                      modifier = Modifier.testTag("studio_img_ratio_${ratio.replace(':', '_')}")
                    )
                  }
                  Spacer(Modifier.weight(1f))
                  OutlinedButton(
                    onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.testTag("studio_img_pick_photo_btn"),
                  ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (sourceEditBitmap != null) "Change Photo" else "Edit Photo", maxLines = 1, softWrap = false)
                  }
                }

                sourceEditBitmap?.let { src ->
                  Spacer(Modifier.height(8.dp))
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Image(
                      bitmap = src.asImageBitmap(),
                      contentDescription = "Source photo to edit",
                      contentScale = ContentScale.Crop,
                      modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)),
                    )
                    Text("Source photo attached — Gemini will edit this photo using your prompt.", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { sourceEditBitmap = null }) {
                      Icon(Icons.Default.Close, contentDescription = "Remove photo")
                    }
                  }
                }

                Spacer(Modifier.height(10.dp))
                TacticalButton(
                  onClick = {
                    val p = imgPrompt.trim()
                    if (p.isBlank()) {
                      imgError = "Enter a visual prompt first."
                      return@TacticalButton
                    }
                    imgBusy = true
                    imgError = ""
                    scope.launch {
                      val res = studioAi.generateOrEditImage(p, sourceEditBitmap, imgAspect)
                      res.fold(
                        onSuccess = { imgResult = it },
                        onFailure = { e -> imgError = e.localizedMessage ?: "Image generation failed." },
                      )
                      imgBusy = false
                    }
                  },
                  enabled = !imgBusy,
                  modifier = Modifier.fillMaxWidth().testTag("studio_img_generate_btn"),
                ) {
                  Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(6.dp))
                  Text(if (imgBusy) "Synthesizing (30–60s)..." else if (sourceEditBitmap != null) "Edit Photo with Gemini" else "Generate Image with Gemini")
                }

                if (imgBusy) {
                  Spacer(Modifier.height(10.dp))
                  LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                  Spacer(Modifier.height(4.dp))
                  Text("Rendering high-res 1K artwork via Firebase AI Logic (gemini-nano-banana-2.1)...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                if (imgError.isNotBlank()) {
                  Spacer(Modifier.height(8.dp))
                  Text(imgError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }

                imgResult?.let { generated ->
                  Spacer(Modifier.height(12.dp))
                  Image(
                    bitmap = generated.bitmap.asImageBitmap(),
                    contentDescription = "AI Generated Rural PEV Artwork",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
                  )
                  if (generated.captionText.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(generated.captionText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                  Spacer(Modifier.height(8.dp))
                  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TacticalOutlinedButton(
                      onClick = { sourceEditBitmap = generated.bitmap },
                      modifier = Modifier.weight(1f).testTag("studio_img_refine_btn"),
                    ) {
                      Text("Refine / Edit", maxLines = 1, softWrap = false)
                    }
                    TacticalButton(
                      onClick = {
                        runCatching {
                          val file = File(ctx.cacheDir, "rural_erides_ai_${System.currentTimeMillis()}.png")
                          FileOutputStream(file).use { generated.bitmap.compress(Bitmap.CompressFormat.PNG, 95, it) }
                          val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
                          val share = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                          }
                          ctx.startActivity(Intent.createChooser(share, "Share Rural ERides Artwork"))
                        }.onFailure {
                          Toast.makeText(ctx, "Share failed: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                      },
                      modifier = Modifier.weight(1f).testTag("studio_img_share_btn"),
                    ) {
                      Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(Modifier.width(4.dp))
                      Text("Share Image", maxLines = 1, softWrap = false)
                    }
                  }
                }
              }
            }
          }
          StudioHubTab.VOICE_LIVE -> {
            item {
              TacticalCard(highlight = true, title = "LIVE VOICE CONVERSATION & AUDIO TRANSCRIBER") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  FilterChip(
                    selected = !voiceModeTranscribeOnly,
                    onClick = { voiceModeTranscribeOnly = false },
                    label = { Text("Live Voice Chat", maxLines = 1, softWrap = false) },
                    modifier = Modifier.weight(1f).testTag("studio_voice_mode_chat"),
                  )
                  FilterChip(
                    selected = voiceModeTranscribeOnly,
                    onClick = { voiceModeTranscribeOnly = true },
                    label = { Text("Transcribe Mic Audio", maxLines = 1, softWrap = false) },
                    modifier = Modifier.weight(1f).testTag("studio_voice_mode_transcribe"),
                  )
                }

                Spacer(Modifier.height(6.dp))
                Text("Gemini Intelligence Tier", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  items(GeminiModelCatalog.availableModels) { (modelId, label) ->
                    FilterChip(
                      selected = selectedVoiceModel == modelId,
                      onClick = { selectedVoiceModel = modelId },
                      label = { Text(label, maxLines = 1, softWrap = false) },
                      modifier = Modifier.testTag("studio_voice_model_$modelId"),
                    )
                  }
                }

                if (!voiceModeTranscribeOnly) {
                  Spacer(Modifier.height(6.dp))
                  LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("Rural Trail Navigator", "Barn Shop Master Mechanic", "Battery & VESC Engineer", "Safety Weather Dispatcher")) { role ->
                      FilterChip(
                        selected = voicePersona == role,
                        onClick = { voicePersona = role },
                        label = { Text(role, maxLines = 1, softWrap = false) },
                      )
                    }
                  }
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text("Auto-Speak Gemini Replies Aloud", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = autoSpeakVoiceReply, onCheckedChange = { autoSpeakVoiceReply = it })
                  }
                }

                Spacer(Modifier.height(10.dp))

                // Live Audio RMS Waveform Meter & Record/Stop CTA
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  border = BorderStroke(1.dp, if (isRecording) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(42.dp)) {
                      val bars = 24
                      val barW = size.width / (bars * 1.45f)
                      val gap = barW * 0.45f
                      val level = if (isRecording) micLevel.coerceIn(0.08f, 1f) else 0.06f
                      for (i in 0 until bars) {
                        val wave = kotlin.math.abs(kotlin.math.sin((i * 0.65f) + recSeconds * 11f)).toFloat()
                        val hFrac = (level * (0.35f + 0.65f * wave)).coerceIn(0.1f, 1f)
                        val bh = size.height * hFrac
                        drawRoundRect(
                          color = if (isRecording) Color(0xFF39FF14) else Color(0xFF64748B),
                          topLeft = Offset(i * (barW + gap), (size.height - bh) / 2f),
                          size = Size(barW, bh),
                          cornerRadius = CornerRadius(4f, 4f),
                        )
                      }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                      onClick = {
                        if (isRecording) {
                          stopMicAndProcess()
                        } else {
                          val hasMic = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                          if (hasMic) {
                            audioEngine.startMicRecording { autoWav ->
                              scope.launch {
                                voiceBusy = true
                                val res = studioAi.transcribeAudioWav(autoWav, vehicleSummary)
                                res.onSuccess { lastTranscription = it }
                                voiceBusy = false
                              }
                            }
                          } else {
                            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                          }
                        }
                      },
                      enabled = !voiceBusy,
                      colors = ButtonDefaults.buttonColors(containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                      modifier = Modifier.fillMaxWidth().height(48.dp).testTag("studio_mic_record_btn"),
                    ) {
                      Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null)
                      Spacer(Modifier.width(6.dp))
                      Text(
                        when {
                          voiceBusy -> "Processing with Gemini..."
                          isRecording -> "Stop & Send (${"%.1f".format(recSeconds)}s)"
                          voiceModeTranscribeOnly -> "Tap to Record Audio for Transcription"
                          else -> "Tap to Speak to Live Gemini Co-Pilot"
                        },
                        maxLines = 1,
                        softWrap = false,
                      )
                    }
                  }
                }

                if (voiceError.isNotBlank()) {
                  Spacer(Modifier.height(8.dp))
                  Text(voiceError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }

                lastTranscription?.let { tr ->
                  Spacer(Modifier.height(10.dp))
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("TRANSCRIPT • ${tr.actionCategory}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = {
                          clipboard.setText(AnnotatedString(tr.transcript))
                          Toast.makeText(ctx, "Transcript copied!", Toast.LENGTH_SHORT).show()
                        }) {
                          Icon(Icons.Default.ContentCopy, contentDescription = "Copy transcript", modifier = Modifier.size(18.dp))
                        }
                      }
                      Text(tr.transcript, style = MaterialTheme.typography.bodyMedium)
                      if (tr.summary.isNotBlank()) {
                        Text("Summary: ${tr.summary}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                    }
                  }
                }

                if (!voiceModeTranscribeOnly) {
                  Spacer(Modifier.height(10.dp))
                  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                      value = voiceTextInput,
                      onValueChange = { voiceTextInput = it },
                      placeholder = { Text("Or type a message to Gemini...") },
                      singleLine = true,
                      modifier = Modifier.weight(1f).testTag("studio_voice_text_input"),
                    )
                    IconButton(
                      enabled = !voiceBusy && voiceTextInput.isNotBlank(),
                      onClick = {
                        val msg = voiceTextInput.trim()
                        voiceTextInput = ""
                        voiceHistory += VoiceChatTurn(id = UUID.randomUUID().toString(), isUser = true, text = msg)
                        voiceBusy = true
                        voiceError = ""
                        scope.launch {
                          val res =
                            studioAi.runVoiceConversationTurn(
                              userMessage = msg,
                              audioWavBytes = null,
                              history = voiceHistory.toList(),
                              personaRole = voicePersona,
                              modelId = selectedVoiceModel,
                              vehicleContext = vehicleSummary,
                              locationLabel = liveLocationLabel,
                            )
                          res.fold(
                            onSuccess = { (reply, modelUsed) ->
                              voiceHistory += VoiceChatTurn(id = UUID.randomUUID().toString(), isUser = false, text = reply, modelUsed = modelUsed)
                              if (autoSpeakVoiceReply) {
                                sessionVm.voiceAudio.speak(
                                  text = reply,
                                  volumePct = settings.globalVolume,
                                  speechRate = settings.ttsSpeechRate,
                                  pitch = settings.ttsPitch,
                                )
                              }
                            },
                            onFailure = { e -> voiceError = e.localizedMessage ?: "Chat turn failed." },
                          )
                          voiceBusy = false
                        }
                      },
                      modifier = Modifier.testTag("studio_voice_send_btn"),
                    ) {
                      Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                  }
                }
              }
            }

            if (voiceHistory.isNotEmpty() && !voiceModeTranscribeOnly) {
              items(voiceHistory, key = { it.id }) { turn ->
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = if (turn.isUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                      text = if (turn.isUser) (profile?.callsign ?: "YOU") else "$voicePersona • ${turn.modelUsed}",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Black,
                      color = MaterialTheme.colorScheme.primary,
                    )
                    Text(turn.text, style = MaterialTheme.typography.bodyMedium)
                  }
                }
              }
            }
          }

          StudioHubTab.SEARCH_MAPS -> {
            item {
              TacticalCard(highlight = true, title = "LIVE GOOGLE SEARCH & GPS MAPS SCOUT") {
                Text(
                  "Ask live grounded questions about rural trails, nearby e-bike/PEV shops, public outlets, weather rules, or replacement parts.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                  value = exploreQuery,
                  onValueChange = { exploreQuery = it },
                  label = { Text("Search live trails, shops, chargers, or rules...") },
                  placeholder = { Text("e.g., Best rural rail-trails & bike repair shops near me") },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().testTag("studio_search_input"),
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  FilterChip(
                    selected = useGoogleSearch,
                    onClick = { useGoogleSearch = !useGoogleSearch },
                    label = { Text("Google Search", maxLines = 1, softWrap = false) },
                  )
                  FilterChip(
                    selected = useMapPlaces,
                    onClick = { useMapPlaces = !useMapPlaces },
                    label = { Text("GPS Map Places", maxLines = 1, softWrap = false) },
                  )
                }
                Spacer(Modifier.height(8.dp))
                TacticalButton(
                  onClick = {
                    val q = exploreQuery.trim()
                    if (q.isBlank()) {
                      exploreError = "Enter a search query first."
                      return@TacticalButton
                    }
                    exploreBusy = true
                    exploreError = ""
                    scope.launch {
                      val res =
                        studioAi.exploreWithSearchAndMaps(
                          query = q,
                          lat = telemetry.lat,
                          lng = telemetry.lng,
                          locationLabel = liveLocationLabel,
                          useGoogleSearch = useGoogleSearch,
                          useMapPlaces = useMapPlaces,
                        )
                      res.fold(
                        onSuccess = { exploreResult = it },
                        onFailure = { e -> exploreError = e.localizedMessage ?: "Search failed." },
                      )
                      exploreBusy = false
                    }
                  },
                  enabled = !exploreBusy,
                  modifier = Modifier.fillMaxWidth().testTag("studio_search_go_btn"),
                ) {
                  Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(6.dp))
                  Text(if (exploreBusy) "Searching Live Data..." else "Search Web & Maps")
                }
                if (exploreBusy) {
                  Spacer(Modifier.height(8.dp))
                  LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                if (exploreError.isNotBlank()) {
                  Spacer(Modifier.height(8.dp))
                  Text(exploreError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                exploreResult?.let { res ->
                  Spacer(Modifier.height(10.dp))
                  Text(
                    if (res.usedSearchTool) "GROUNDED WITH GOOGLE SEARCH" else "LIVE TRAIL & MAPS ANSWER",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                  )
                  Spacer(Modifier.height(4.dp))
                  Text(res.answer, style = MaterialTheme.typography.bodyMedium)

                  if (res.places.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text("MAP WAYPOINTS (${res.places.size})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    res.places.forEach { pin ->
                      Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier =
                          Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clickable {
                              runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(pin.url))) }
                            },
                      ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                          Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFEF4444))
                          Column(Modifier.weight(1f)) {
                            Text(pin.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(pin.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                          }
                          Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open in Maps", modifier = Modifier.size(16.dp))
                        }
                      }
                    }
                  }

                  if (res.webSources.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("VERIFIED WEB SOURCES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    res.webSources.forEach { (title, url) ->
                      Row(
                        modifier =
                          Modifier
                            .fillMaxWidth()
                            .clickable { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                      ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                      }
                    }
                  }
                }
              }
            }
          }

          StudioHubTab.MUSIC_LAB -> {
            item {
              TacticalCard(highlight = true, title = "AI TRAIL SOUNDTRACK STUDIO (44.1kHz Synth)") {
                Text(
                  "Describe your ride mood and Gemini composes an original chord progression, bassline, and melody synthesized live at 44.1kHz stereo!",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                  value = musicPrompt,
                  onValueChange = { musicPrompt = it },
                  label = { Text("Ride soundtrack vibe or trail story...") },
                  minLines = 2,
                  maxLines = 3,
                  modifier = Modifier.fillMaxWidth().testTag("studio_music_prompt_input"),
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  items(listOf("Country Acoustic", "Backroad Synthwave", "Cyber PEV Electro", "Gravel Folk Groove")) { g ->
                    FilterChip(
                      selected = musicGenre == g,
                      onClick = { musicGenre = g },
                      label = { Text(g, maxLines = 1, softWrap = false) },
                      modifier = Modifier.testTag("studio_music_genre_${g.lowercase().replace(' ', '_')}"),
                    )
                  }
                }
                Spacer(Modifier.height(10.dp))
                TacticalButton(
                  onClick = {
                    musicBusy = true
                    musicError = ""
                    audioEngine.stopMusic()
                    scope.launch {
                      val res = studioAi.composeTrailSoundtrack(musicPrompt.ifBlank { "Rural evening e-ride" }, musicGenre)
                      res.fold(
                        onSuccess = { trk ->
                          composedTrack = trk
                          audioEngine.playComposedTrack(trk, loopCount = 2, volumePct = settings.globalVolume)
                        },
                        onFailure = { e -> musicError = e.localizedMessage ?: "Music composition failed." },
                      )
                      musicBusy = false
                    }
                  },
                  enabled = !musicBusy,
                  modifier = Modifier.fillMaxWidth().testTag("studio_music_compose_btn"),
                ) {
                  Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(6.dp))
                  Text(if (musicBusy) "Composing Arrangement..." else "Compose & Play Trail Soundtrack")
                }
                if (musicBusy) {
                  Spacer(Modifier.height(8.dp))
                  LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                if (musicError.isNotBlank()) {
                  Spacer(Modifier.height(8.dp))
                  Text(musicError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }

                composedTrack?.let { trk ->
                  Spacer(Modifier.height(12.dp))
                  Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                          Text(trk.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                          Text("${trk.genre} • ${trk.bpm} BPM • ${trk.musicalKey}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                          onClick = {
                            if (isPlayingMusic) audioEngine.stopMusic()
                            else audioEngine.playComposedTrack(trk, loopCount = 2, volumePct = settings.globalVolume)
                          },
                          modifier = Modifier.testTag("studio_music_play_toggle_btn"),
                        ) {
                          Icon(
                            imageVector = if (isPlayingMusic) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "Play or Stop Track",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp),
                          )
                        }
                      }
                      LinearProgressIndicator(progress = { musicProgress }, modifier = Modifier.fillMaxWidth())
                      Text("Chords: ${trk.chords.joinToString("  →  ")}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                      Text(trk.vibeSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      if (trk.lyricHook.isNotBlank()) {
                        Text("“${trk.lyricHook}”", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.secondary)
                      }
                    }
                  }
                }
              }
            }
          }

          StudioHubTab.ARCADE_GAME -> {
            item {
              TacticalCard(highlight = true, title = "GRAVEL VOLT RUNNER • 60FPS PEV ARCADE") {
                Text(
                  "Take your rig down a sunset rural gravel backroad! Weave past crossing tractors, bunny-hop hay bales & creek washouts, fire 72V Volt Boost to smash obstacles, and collect 21700 cells to keep your pack alive.",
                  style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                TacticalButton(
                  onClick = { launchFullGame = true },
                  modifier = Modifier.fillMaxWidth().height(52.dp).testTag("studio_launch_game_btn"),
                ) {
                  Icon(Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(22.dp))
                  Spacer(Modifier.width(8.dp))
                  Text("LAUNCH FULL-SCREEN ARCADE GAME", fontWeight = FontWeight.Black)
                }
              }
            }
          }

          StudioHubTab.PEV_QUIZ -> {
            item {
              PevKnowledgeTestView(
                activeVehicleName = activeVehicle?.name ?: "Sur-Ron Ultra Bee eMoto",
                activeVoltage = activeVehicle?.voltage?.toInt() ?: 72,
                onShareScoreToBoard = { scorePct, rankTitle, summary ->
                  socialVm.createPost(
                    callsign = profile?.callsign?.ifBlank { "PILOT" } ?: "PILOT",
                    badge = rankTitle,
                    category = "TIPS",
                    title = "Earned $rankTitle ($scorePct%) on PEV Knowledge Test!",
                    body = "$summary Tested on ${activeVehicle?.name ?: "my PEV rig"}!",
                    bitmap = null,
                    pollQuestion = "",
                    pollOptions = emptyList(),
                    priceUsd = 0.0,
                    location = liveLocationLabel,
                    telemetrySummary = "$scorePct% PEV Certification • $rankTitle",
                    helpNeeded = false,
                    speedTrap = false,
                    stolenAlert = false,
                    onDone = { Toast.makeText(ctx, "PEV Certification posted to Porch Board!", Toast.LENGTH_SHORT).show() },
                  )
                },
              )
            }
          }
        }
      }
    }
  }
}
