package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AiVoiceProfile
import com.example.data.CustomVoiceProfile
import com.example.data.VoiceDspProfile
import com.example.data.parseCustomVoiceProfiles
import com.example.data.serializeCustomVoiceProfiles
import com.example.hardware.VoiceAudioManager
import com.example.viewmodel.SessionViewModel
import java.io.InputStream
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val GEMINI_NEURAL_VOICES =
  listOf("Achird", "Zephyr", "Orus", "Fenrir", "Enceladus", "Puck", "Kore", "Aoede", "Charon", "Leda")

private val VoiceDspProfile.displayName: String
  get() = when (this) {
    VoiceDspProfile.WARM_PORCH -> "Warm Porch"
    VoiceDspProfile.DEEP_BARN -> "Deep Barn"
    VoiceDspProfile.BRIGHT_DISPATCH -> "Bright Dispatch"
    VoiceDspProfile.TACTICAL_CRISP -> "Tactical Crisp"
    VoiceDspProfile.WARM_SCOUT -> "Warm Scout"
    VoiceDspProfile.OUTBACK_AU -> "Outback AU"
    VoiceDspProfile.HIGHLAND_UK -> "Highland UK"
    VoiceDspProfile.CB_RADIO -> "CB Radio"
  }

private val AiVoiceProfile.genderLabel: String
  get() = if (preferFemale) "Female Voice" else "Male Voice"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomVoiceStudioScreen(
  sessionVm: SessionViewModel,
  onBack: () -> Unit,
) {
  val ctx = LocalContext.current
  val scope = rememberCoroutineScope()
  val settings by sessionVm.settings.collectAsStateWithLifecycle()
  val voiceAudio = sessionVm.voiceAudio

  var selectedTab by remember { mutableIntStateOf(0) }
  val customProfiles = remember(settings.customVoiceProfilesJson) {
    parseCustomVoiceProfiles(settings.customVoiceProfilesJson)
  }
  val activeCustom = settings.getActiveCustomVoice()

  // Form State for Creating or Editing a Custom Voice Profile
  var showEditorCard by remember { mutableStateOf(false) }
  var editingProfileId by remember { mutableStateOf<String?>(null) }
  var formName by remember { mutableStateOf("") }
  var formDescription by remember { mutableStateOf("") }
  var formPitch by remember { mutableFloatStateOf(1.0f) }
  var formRate by remember { mutableFloatStateOf(1.0f) }
  var formBaseProfileId by remember { mutableStateOf(AiVoiceProfile.WARM_RURAL_PORCH.id) }
  var formGeminiVoice by remember { mutableStateOf("Achird") }
  var formDspProfile by remember { mutableStateOf(VoiceDspProfile.WARM_PORCH) }
  var formDirective by remember { mutableStateOf("Speak warmly and conversationally in a friendly rural tone with clear trail guidance.") }
  var formAudioBase64 by remember { mutableStateOf("") }
  var formAudioFileName by remember { mutableStateOf("") }
  var formAudioDurationMs by remember { mutableStateOf(0L) }
  var isRecordingSample by remember { mutableStateOf(false) }
  var bufferTestActive by remember { mutableStateOf(false) }

  BackHandler {
    voiceAudio.stopSpeaking()
    onBack()
  }

  // Audio file picker launcher (Zero-permission storage compliance via ActivityResultContracts.GetContent)
  val audioFileLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
      if (uri != null) {
        runCatching {
          val inputStream: InputStream? = ctx.contentResolver.openInputStream(uri)
          val bytes = inputStream?.readBytes() ?: ByteArray(0)
          inputStream?.close()
          if (bytes.isNotEmpty()) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "custom_voice_sample.wav"
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            formAudioBase64 = b64
            formAudioFileName = fileName
            // Approximate duration from byte count (assuming 16-bit mono 16kHz to 44.1kHz)
            formAudioDurationMs = ((bytes.size / 32000.0) * 1000.0).toLong().coerceIn(400L, 120000L)
            voiceAudio.playTouchSound(settings.touchSoundsEnabled, settings.touchSoundPack, VoiceAudioManager.TouchCue.SUCCESS_CHIME)
            Toast.makeText(ctx, "Imported voice audio sample: $fileName", Toast.LENGTH_SHORT).show()
          } else {
            Toast.makeText(ctx, "Selected audio file was empty", Toast.LENGTH_SHORT).show()
          }
        }.onFailure { err ->
          Toast.makeText(ctx, "Failed to read audio file: ${err.message}", Toast.LENGTH_SHORT).show()
        }
      }
    }

  // Mic permission for in-app voice sample recording
  val micPermLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        isRecordingSample = true
        voiceAudio.startPttRecording(volumePct = settings.globalVolume)
        scope.launch {
          delay(5000L)
          if (isRecordingSample) {
            val result = voiceAudio.stopPttRecordingAndEncode(volumePct = settings.globalVolume)
            isRecordingSample = false
            if (result != null && result.first.isNotBlank()) {
              formAudioBase64 = result.first
              formAudioFileName = "mic_sample_${System.currentTimeMillis() % 10000}.pcm"
              formAudioDurationMs = result.second
              voiceAudio.playTouchSound(settings.touchSoundsEnabled, settings.touchSoundPack, VoiceAudioManager.TouchCue.SUCCESS_CHIME)
              Toast.makeText(ctx, "Recorded 5s custom voice sample", Toast.LENGTH_SHORT).show()
            }
          }
        }
      } else {
        Toast.makeText(ctx, "Microphone permission required to record sample", Toast.LENGTH_SHORT).show()
      }
    }

  fun resetForm() {
    editingProfileId = null
    formName = ""
    formDescription = ""
    formPitch = 1.0f
    formRate = 1.0f
    formBaseProfileId = AiVoiceProfile.WARM_RURAL_PORCH.id
    formGeminiVoice = "Achird"
    formDspProfile = VoiceDspProfile.WARM_PORCH
    formDirective = "Speak warmly and conversationally in a friendly rural tone with clear trail guidance."
    formAudioBase64 = ""
    formAudioFileName = ""
    formAudioDurationMs = 0L
    isRecordingSample = false
  }

  fun populateFormFromProfile(profile: CustomVoiceProfile) {
    editingProfileId = profile.id
    formName = profile.name
    formDescription = profile.description
    formPitch = profile.pitch
    formRate = profile.speechRate
    formBaseProfileId = profile.baseProfileId
    formGeminiVoice = profile.geminiVoiceName
    formDspProfile = profile.dspProfile
    formDirective = profile.styleDirective
    formAudioBase64 = profile.audioSampleBase64
    formAudioFileName = profile.audioSampleFileName
    formAudioDurationMs = profile.audioSampleDurationMs
    showEditorCard = true
  }

  fun saveCustomVoice() {
    val cleanName = formName.trim()
    if (cleanName.isBlank()) {
      Toast.makeText(ctx, "Please enter a voice name", Toast.LENGTH_SHORT).show()
      return
    }
    val profileId = editingProfileId ?: UUID.randomUUID().toString()
    val updated =
      CustomVoiceProfile(
        id = profileId,
        name = cleanName,
        description = formDescription.trim(),
        pitch = formPitch,
        speechRate = formRate,
        baseProfileId = formBaseProfileId,
        geminiVoiceName = formGeminiVoice,
        dspProfile = formDspProfile,
        styleDirective = formDirective.trim(),
        audioSampleBase64 = formAudioBase64,
        audioSampleFileName = formAudioFileName,
        audioSampleDurationMs = formAudioDurationMs,
        isCustomUploaded = formAudioBase64.isNotBlank(),
        createdAtMs = System.currentTimeMillis(),
      )
    val list = customProfiles.toMutableList()
    val existingIdx = list.indexOfFirst { it.id == profileId }
    if (existingIdx >= 0) {
      list[existingIdx] = updated
    } else {
      list.add(0, updated)
    }
    val json = serializeCustomVoiceProfiles(list)
    sessionVm.updateSettings {
      it.copy(
        customVoiceProfilesJson = json,
        activeCustomVoiceId = profileId,
      )
    }
    voiceAudio.playTouchSound(settings.touchSoundsEnabled, settings.touchSoundPack, VoiceAudioManager.TouchCue.ACTION_CONFIRM)
    Toast.makeText(ctx, "Custom voice saved & activated: ${updated.name}", Toast.LENGTH_SHORT).show()
    showEditorCard = false
    resetForm()
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Custom AI Voice Studio",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "Synthesized Neural Voices & Latency Buffer",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = {
              voiceAudio.stopSpeaking()
              onBack()
            },
            modifier = Modifier.size(48.dp).testTag("voice_studio_back_button"),
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(
            onClick = {
              voiceAudio.stopSpeaking()
              Toast.makeText(ctx, "Audio stopped", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(48.dp).testTag("voice_studio_stop_audio_button"),
          ) {
            Icon(Icons.Default.Stop, contentDescription = "Stop Audio", tint = MaterialTheme.colorScheme.error)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
        ),
      )
    },
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
    ) {
      // TABS
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = {
            voiceAudio.triggerHapticCue(settings.hapticFeedback, VoiceAudioManager.HapticCue.LIGHT_TICK)
            selectedTab = 0
          },
          text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("Custom Voices", style = MaterialTheme.typography.labelMedium)
              if (customProfiles.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                  Text("${customProfiles.size}", color = MaterialTheme.colorScheme.onPrimary)
                }
              }
            }
          },
          modifier = Modifier.testTag("tab_custom_voices"),
        )
        Tab(
          selected = selectedTab == 1,
          onClick = {
            voiceAudio.triggerHapticCue(settings.hapticFeedback, VoiceAudioManager.HapticCue.LIGHT_TICK)
            selectedTab = 1
          },
          text = { Text("Presets (8)", style = MaterialTheme.typography.labelMedium) },
          modifier = Modifier.testTag("tab_presets"),
        )
        Tab(
          selected = selectedTab == 2,
          onClick = {
            voiceAudio.triggerHapticCue(settings.hapticFeedback, VoiceAudioManager.HapticCue.LIGHT_TICK)
            selectedTab = 2
          },
          text = { Text("Pre-load Buffer", style = MaterialTheme.typography.labelMedium) },
          modifier = Modifier.testTag("tab_preload_buffer"),
        )
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // ACTIVE VOICE STATUS BANNER (SHOWS ACROSS ALL TABS)
        item {
          Card(
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
              ) {
                Box(
                  modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                  )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                  Text(
                    text = if (activeCustom != null) "ACTIVE: ${activeCustom.name} (Custom)" else "ACTIVE: ${settings.aiVoiceProfile.label} (Preset)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                  Text(
                    text = if (activeCustom != null)
                      "Neural: ${activeCustom.geminiVoiceName} • DSP: ${activeCustom.dspProfile.name} • Speed ${"%.2f".format(activeCustom.speechRate)}x / Pitch ${"%.2f".format(activeCustom.pitch)}x"
                    else
                      "Neural Base: ${settings.aiVoiceProfile.geminiVoiceName} • DSP: ${settings.aiVoiceProfile.dspProfile.name} • ${settings.aiVoiceProfile.genderLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
              if (activeCustom != null) {
                OutlinedButton(
                  onClick = {
                    sessionVm.updateSettings { it.copy(activeCustomVoiceId = "") }
                    voiceAudio.playTouchSound(settings.touchSoundsEnabled, settings.touchSoundPack, VoiceAudioManager.TouchCue.BUTTON_TAP)
                    Toast.makeText(ctx, "Switched back to preset ${settings.aiVoiceProfile.label}", Toast.LENGTH_SHORT).show()
                  },
                  modifier = Modifier.testTag("button_switch_to_presets"),
                ) {
                  Text("Use Preset", style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }
        }

        // TAB 0: CUSTOM VOICES
        if (selectedTab == 0) {
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "YOUR CUSTOM SYNTHESIZED VOICES",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
              Button(
                onClick = {
                  if (showEditorCard) {
                    showEditorCard = false
                    resetForm()
                  } else {
                    resetForm()
                    showEditorCard = true
                  }
                },
                modifier = Modifier.testTag("button_toggle_create_custom_voice"),
              ) {
                Icon(if (showEditorCard) Icons.Default.Close else Icons.Default.Add, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (showEditorCard) "Cancel" else "Create / Upload Voice")
              }
            }
          }

          // EDITOR / UPLOAD FORM CARD
          if (showEditorCard) {
            item {
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("custom_voice_editor_card"),
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Text(
                    text = if (editingProfileId != null) "Edit Custom Voice" else "Synthesize New Voice Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "Configure neural Gemini base, real-time DSP filter, pitch/rate, and attach audio samples.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )

                  OutlinedTextField(
                    value = formName,
                    onValueChange = { formName = it },
                    label = { Text("Voice Name (e.g. Appalachian Co-Pilot)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_voice_name"),
                  )

                  OutlinedTextField(
                    value = formDescription,
                    onValueChange = { formDescription = it },
                    label = { Text("Description & Notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_voice_description"),
                  )

                  // BASE NEURAL GEMINI VOICE SELECTOR
                  Text("Base Neural Gemini Voice", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                  LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(GEMINI_NEURAL_VOICES) { vName ->
                      FilterChip(
                        selected = formGeminiVoice == vName,
                        onClick = { formGeminiVoice = vName },
                        label = { Text(vName, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = if (formGeminiVoice == vName) {
                          { Icon(Icons.Default.Check, null, Modifier.size(14.dp)) }
                        } else null,
                        modifier = Modifier.testTag("chip_neural_voice_$vName"),
                      )
                    }
                  }

                  // DSP AUDIO SIGNATURE SELECTOR
                  Text("DSP Audio Signature & Filter", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                  LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(VoiceDspProfile.entries) { dsp ->
                      FilterChip(
                        selected = formDspProfile == dsp,
                        onClick = { formDspProfile = dsp },
                        label = { Text(dsp.displayName, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = if (formDspProfile == dsp) {
                          { Icon(Icons.Default.Audiotrack, null, Modifier.size(14.dp)) }
                        } else null,
                        modifier = Modifier.testTag("chip_dsp_profile_${dsp.name}"),
                      )
                    }
                  }

                  // PITCH SLIDER
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text("Pitch Multiplier: ${"%.2f".format(formPitch)}x", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    OutlinedButton(
                      onClick = { formPitch = 1.0f },
                      modifier = Modifier.height(28.dp),
                      contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                      Text("Reset 1.0x", style = MaterialTheme.typography.labelSmall)
                    }
                  }
                  Slider(
                    value = formPitch,
                    onValueChange = { formPitch = it },
                    valueRange = 0.65f..1.45f,
                    modifier = Modifier.fillMaxWidth().testTag("slider_voice_pitch"),
                  )

                  // SPEECH RATE SLIDER
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text("Speech Rate: ${"%.2f".format(formRate)}x", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    OutlinedButton(
                      onClick = { formRate = 1.0f },
                      modifier = Modifier.height(28.dp),
                      contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                      Text("Reset 1.0x", style = MaterialTheme.typography.labelSmall)
                    }
                  }
                  Slider(
                    value = formRate,
                    onValueChange = { formRate = it },
                    valueRange = 0.70f..1.50f,
                    modifier = Modifier.fillMaxWidth().testTag("slider_voice_rate"),
                  )

                  OutlinedTextField(
                    value = formDirective,
                    onValueChange = { formDirective = it },
                    label = { Text("Voice Style & Tone Directive") },
                    modifier = Modifier.fillMaxWidth().testTag("input_voice_directive"),
                  )

                  // REFERENCE AUDIO SAMPLE (FILE UPLOAD OR MIC RECORDING)
                  HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                  Text("Reference Audio Sample (Optional)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                  if (formAudioBase64.isNotBlank()) {
                    Surface(
                      color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier.fillMaxWidth(),
                    ) {
                      Row(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                      ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                          Icon(Icons.Default.Audiotrack, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                          Spacer(Modifier.width(8.dp))
                          Column {
                            Text(formAudioFileName.ifBlank { "Voice Sample" }, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("${formAudioDurationMs / 1000}s duration attached", style = MaterialTheme.typography.bodySmall)
                          }
                        }
                        Row {
                          IconButton(
                            onClick = { voiceAudio.playPcmBase64(formAudioBase64, settings.globalVolume) },
                            modifier = Modifier.size(36.dp),
                          ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play Sample")
                          }
                          IconButton(
                            onClick = {
                              formAudioBase64 = ""
                              formAudioFileName = ""
                              formAudioDurationMs = 0L
                            },
                            modifier = Modifier.size(36.dp),
                          ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove Sample")
                          }
                        }
                      }
                    }
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    OutlinedButton(
                      onClick = { audioFileLauncher.launch("audio/*") },
                      modifier = Modifier.weight(1f).testTag("button_upload_audio_file"),
                    ) {
                      Icon(Icons.Default.Upload, null, Modifier.size(16.dp))
                      Spacer(Modifier.width(4.dp))
                      Text("Pick Audio File", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                      onClick = {
                        if (isRecordingSample) {
                          val res = voiceAudio.stopPttRecordingAndEncode(settings.globalVolume)
                          isRecordingSample = false
                          if (res != null && res.first.isNotBlank()) {
                            formAudioBase64 = res.first
                            formAudioFileName = "mic_sample_${System.currentTimeMillis() % 10000}.pcm"
                            formAudioDurationMs = res.second
                            Toast.makeText(ctx, "Sample attached", Toast.LENGTH_SHORT).show()
                          }
                        } else {
                          micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                      },
                      colors = if (isRecordingSample) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors(),
                      modifier = Modifier.weight(1f).testTag("button_record_voice_sample"),
                    ) {
                      Icon(Icons.Default.Mic, null, Modifier.size(16.dp))
                      Spacer(Modifier.width(4.dp))
                      Text(if (isRecordingSample) "Stop Rec" else "Record 5s", style = MaterialTheme.typography.labelSmall)
                    }
                  }

                  // PREVIEW & SAVE BUTTONS
                  HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    OutlinedButton(
                      onClick = {
                        val tempProfile = CustomVoiceProfile(
                          name = formName.ifBlank { "Preview" },
                          pitch = formPitch,
                          speechRate = formRate,
                          baseProfileId = formBaseProfileId,
                          geminiVoiceName = formGeminiVoice,
                          dspProfile = formDspProfile,
                          styleDirective = formDirective,
                        )
                        voiceAudio.previewCustomVoice(tempProfile, settings.globalVolume)
                      },
                      modifier = Modifier.weight(1f).testTag("button_preview_custom_voice"),
                    ) {
                      Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                      Spacer(Modifier.width(4.dp))
                      Text("Preview Voice")
                    }

                    Button(
                      onClick = { saveCustomVoice() },
                      modifier = Modifier.weight(1f).testTag("button_save_custom_voice"),
                    ) {
                      Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                      Spacer(Modifier.width(4.dp))
                      Text("Save & Activate")
                    }
                  }
                }
              }
            }
          }

          // CUSTOM PROFILES LIST
          if (customProfiles.isEmpty()) {
            item {
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center,
                ) {
                  Icon(Icons.Default.RecordVoiceOver, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                  Spacer(Modifier.height(8.dp))
                  Text("No Custom Synthesized Voices Yet", fontWeight = FontWeight.Bold)
                  Spacer(Modifier.height(4.dp))
                  Text(
                    "Tap 'Create / Upload Voice' to customize pitch, speech rate, DSP signatures, or upload custom audio samples.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          } else {
            items(customProfiles, key = { it.id }) { cp ->
              val isCurrent = settings.activeCustomVoiceId == cp.id
              Card(
                colors = CardDefaults.cardColors(
                  containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("custom_voice_card_${cp.id}"),
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(Modifier.weight(1f)) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(cp.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (isCurrent) {
                          Spacer(Modifier.width(8.dp))
                          Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(6.dp),
                          ) {
                            Text(
                              "ACTIVE",
                              style = MaterialTheme.typography.labelSmall,
                              color = MaterialTheme.colorScheme.onPrimary,
                              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                          }
                        }
                      }
                      if (cp.description.isNotBlank()) {
                        Text(cp.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                    }
                  }

                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surface) {
                      Text("Neural: ${cp.geminiVoiceName}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surface) {
                      Text("DSP: ${cp.dspProfile.name}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surface) {
                      Text("${"%.2f".format(cp.speechRate)}x / ${"%.2f".format(cp.pitch)}x", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                    }
                    if (cp.audioSampleBase64.isNotBlank()) {
                      Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text("Sample attached", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                      }
                    }
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    if (!isCurrent) {
                      Button(
                        onClick = {
                          sessionVm.updateSettings { it.copy(activeCustomVoiceId = cp.id) }
                          voiceAudio.playTouchSound(settings.touchSoundsEnabled, settings.touchSoundPack, VoiceAudioManager.TouchCue.ACTION_CONFIRM)
                          Toast.makeText(ctx, "Active voice: ${cp.name}", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).testTag("button_activate_voice_${cp.id}"),
                      ) {
                        Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Set Active", style = MaterialTheme.typography.labelSmall)
                      }
                    }

                    OutlinedButton(
                      onClick = { voiceAudio.previewCustomVoice(cp, settings.globalVolume) },
                      modifier = Modifier.weight(1f).testTag("button_preview_voice_${cp.id}"),
                    ) {
                      Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                      Spacer(Modifier.width(4.dp))
                      Text("Preview", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                      onClick = { populateFormFromProfile(cp) },
                      modifier = Modifier.testTag("button_edit_voice_${cp.id}"),
                    ) {
                      Icon(Icons.Default.Edit, contentDescription = "Edit Voice", Modifier.size(16.dp))
                    }

                    OutlinedButton(
                      onClick = {
                        val remaining = customProfiles.filter { it.id != cp.id }
                        val json = serializeCustomVoiceProfiles(remaining)
                        sessionVm.updateSettings {
                          it.copy(
                            customVoiceProfilesJson = json,
                            activeCustomVoiceId = if (it.activeCustomVoiceId == cp.id) "" else it.activeCustomVoiceId,
                          )
                        }
                        Toast.makeText(ctx, "Deleted voice ${cp.name}", Toast.LENGTH_SHORT).show()
                      },
                      modifier = Modifier.testTag("button_delete_voice_${cp.id}"),
                    ) {
                      Icon(Icons.Default.Delete, contentDescription = "Delete Voice", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                  }
                }
              }
            }
          }
        }

        // TAB 1: PRESET NEURAL VOICES
        if (selectedTab == 1) {
          item {
            Text(
              text = "BUILT-IN NEURAL VOICE PROFILES (8 PRESETS)",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          }

          items(AiVoiceProfile.entries) { vp ->
            val isCurrentPreset = activeCustom == null && settings.aiVoiceProfile == vp
            Card(
              colors = CardDefaults.cardColors(
                containerColor = if (isCurrentPreset) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              ),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("preset_voice_card_${vp.id}"),
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(vp.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                      if (isCurrentPreset) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                          color = MaterialTheme.colorScheme.primary,
                          shape = RoundedCornerShape(6.dp),
                        ) {
                          Text(
                            "CURRENT PRESET",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                          )
                        }
                      }
                    }
                    Text(vp.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                  Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surface) {
                    Text("Gemini: ${vp.geminiVoiceName}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                  }
                  Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surface) {
                    Text("DSP: ${vp.dspProfile.name}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                  }
                  Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surface) {
                    Text(vp.genderLabel, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                  }
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  if (!isCurrentPreset) {
                    Button(
                      onClick = {
                        sessionVm.updateSettings {
                          it.copy(
                            aiVoiceProfile = vp,
                            activeCustomVoiceId = "",
                          )
                        }
                        voiceAudio.playTouchSound(settings.touchSoundsEnabled, settings.touchSoundPack, VoiceAudioManager.TouchCue.ACTION_CONFIRM)
                        Toast.makeText(ctx, "Preset activated: ${vp.label}", Toast.LENGTH_SHORT).show()
                      },
                      modifier = Modifier.weight(1f).testTag("button_activate_preset_${vp.id}"),
                    ) {
                      Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                      Spacer(Modifier.width(4.dp))
                      Text("Set Active", style = MaterialTheme.typography.labelSmall)
                    }
                  }

                  OutlinedButton(
                    onClick = { voiceAudio.previewVoiceProfile(vp, settings.globalVolume) },
                    modifier = Modifier.weight(1f).testTag("button_preview_preset_${vp.id}"),
                  ) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Preview", style = MaterialTheme.typography.labelSmall)
                  }

                  OutlinedButton(
                    onClick = {
                      selectedTab = 0
                      editingProfileId = null
                      formName = "${vp.label} (Custom Copy)"
                      formDescription = vp.description
                      formPitch = vp.pitch
                      formRate = vp.speechRate
                      formBaseProfileId = vp.id
                      formGeminiVoice = vp.geminiVoiceName
                      formDspProfile = vp.dspProfile
                      formDirective = "Tuned variation of ${vp.label} for rural PEV navigation"
                      showEditorCard = true
                    },
                    modifier = Modifier.testTag("button_clone_preset_${vp.id}"),
                  ) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = "Clone as Custom", Modifier.size(16.dp))
                  }
                }
              }
            }
          }
        }

        // TAB 2: ZERO-LATENCY PRE-LOAD BUFFER & DSP ENGINE
        if (selectedTab == 2) {
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Text(
                  text = "STREAMING TTS PRE-LOAD BUFFER",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = "Instead of waiting 3–7 seconds for Gemini to generate its entire answer, the streaming sentence buffer detects punctuation boundaries on the fly. The very first sentence plays within ~80–120ms, while sentences 2 and 3 pre-synthesize concurrently in background workers for seamless zero-gap playback.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(Modifier.weight(1f)) {
                    Text("Enable Streaming Pre-load Buffer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Pipes early sentence chunks into the speech queue during LLM streaming", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                  Switch(
                    checked = settings.ttsPreloadBufferEnabled,
                    onCheckedChange = { v -> sessionVm.updateSettings { it.copy(ttsPreloadBufferEnabled = v) } },
                    modifier = Modifier.testTag("switch_preload_buffer_enabled"),
                  )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text("Pre-load Buffer Depth: ${settings.ttsPreloadBufferDepth} Sentences", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                  Text(
                    text = if (settings.ttsPreloadBufferDepth <= 2) "Lowest Latency (~85ms)" else "Higher Headroom",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
                Slider(
                  value = settings.ttsPreloadBufferDepth.toFloat(),
                  onValueChange = { sessionVm.updateSettings { s -> s.copy(ttsPreloadBufferDepth = it.toInt().coerceIn(1, 5)) } },
                  valueRange = 1f..5f,
                  steps = 3,
                  modifier = Modifier.fillMaxWidth().testTag("slider_preload_buffer_depth"),
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // LIVE BENCHMARK TEST
                Button(
                  onClick = {
                    bufferTestActive = true
                    scope.launch {
                      val testFragments = listOf(
                        "Sentence one starts instantly from the pre-load buffer without waiting for the full paragraph.",
                        "Sentence two was synthesized in the background worker while sentence one was already speaking.",
                        "Sentence three completes the buffer demonstration with zero perceived latency and smooth DSP filtering.",
                      )
                      val effectiveProfile = activeCustom?.resolveEffectiveBase() ?: settings.aiVoiceProfile
                      val session = voiceAudio.startStreamingSpeechSession(
                        profile = effectiveProfile,
                        customVoice = activeCustom,
                        volumePct = settings.globalVolume,
                        bufferDepth = settings.ttsPreloadBufferDepth,
                      )
                      for (frag in testFragments) {
                        session.enqueueTextFragment(frag)
                        delay(600L)
                      }
                      session.finish()
                      bufferTestActive = false
                    }
                  },
                  modifier = Modifier.fillMaxWidth().testTag("button_test_buffer_stream"),
                ) {
                  Icon(Icons.Default.Speed, null, Modifier.size(16.dp))
                  Spacer(Modifier.width(6.dp))
                  Text(if (bufferTestActive) "Playing Buffer Stream..." else "Test Streaming Pre-load Buffer")
                }

                OutlinedButton(
                  onClick = {
                    voiceAudio.clearPcmCache()
                    voiceAudio.playTouchSound(settings.touchSoundsEnabled, settings.touchSoundPack, VoiceAudioManager.TouchCue.BUTTON_TAP)
                    Toast.makeText(ctx, "PCM synthesis fragment cache cleared", Toast.LENGTH_SHORT).show()
                  },
                  modifier = Modifier.fillMaxWidth().testTag("button_clear_pcm_cache"),
                ) {
                  Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                  Spacer(Modifier.width(6.dp))
                  Text("Clear Synthesized Audio Cache")
                }
              }
            }
          }

          // ARCHITECTURE SPECIFICATIONS
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text("DSP & AUDIO ENGINE SPECIFICATIONS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("• Output AudioTrack: 16-bit Mono PCM, Dynamic Sample Rate (16kHz / 24kHz / 44.1kHz)", style = MaterialTheme.typography.bodySmall)
                Text("• Stream Mode: USAGE_ASSISTANT / CONTENT_TYPE_SPEECH (Hardware Ducking Enabled)", style = MaterialTheme.typography.bodySmall)
                Text("• Boundary Splitter: Unicode sentence punctuation with decimal protection (e.g. 72.4V won't split)", style = MaterialTheme.typography.bodySmall)
                Text("• DSP Pipeline: Biquad lowpass + bass boost saturation + dynamic squelch limiter", style = MaterialTheme.typography.bodySmall)
                Text("• Playback Delay: ~85ms first token audio start (Zero-blocking coroutine channel)", style = MaterialTheme.typography.bodySmall)
              }
            }
          }
        }
      }
    }
  }
}
