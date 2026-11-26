package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CallSession
import com.example.hardware.NativeCallCameraPreview
import com.example.hardware.NativeCallVoiceEngine
import com.example.hardware.decodeCallVideoFrame
import com.example.ui.components.ConfigureEdgeToEdgeDialogWindow
import com.example.ui.components.TacticalCard
import com.example.ui.components.rememberHostWindowSafeInsets
import com.example.viewmodel.CopilotSocialViewModel
import com.example.viewmodel.SessionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LiveCallOverlayHost(
  sessionVm: SessionViewModel,
  socialVm: CopilotSocialViewModel,
) {
  val activeUser by sessionVm.activeUser.collectAsStateWithLifecycle()
  val profile by sessionVm.profile.collectAsStateWithLifecycle()
  val settings by sessionVm.settings.collectAsStateWithLifecycle()
  val incomingCall by socialVm.incomingCallSession.collectAsStateWithLifecycle()
  val activeCall by socialVm.activeCallSession.collectAsStateWithLifecycle()

  val myUid = activeUser?.uid.orEmpty()
  val myCallsign = profile?.callsign?.ifBlank { "PILOT" } ?: "PILOT"

  LaunchedEffect(incomingCall?.id, activeCall?.id) {
    if (incomingCall != null && activeCall == null) {
      socialVm.audio.playIncomingCallChime(
        enabled = settings.callRingtoneEnabled,
        volumePct = settings.ringtoneVolumePct,
        ringtonePreset = settings.callRingtonePreset,
        customUri = settings.customRingtoneUri,
        vibrationPattern = settings.vibrationPattern,
        hapticsEnabled = settings.hapticFeedback,
      )
    } else {
      socialVm.audio.stopRingtone()
    }
  }

  DisposableEffect(Unit) {
    onDispose { socialVm.audio.stopRingtone() }
  }

  incomingCall?.let { incoming ->
    if (activeCall == null) {
      IncomingCallBanner(
        session = incoming,
        onAccept = {
          socialVm.audio.stopRingtone()
          socialVm.acceptIncomingCall(incoming)
        },
        onDecline = {
          socialVm.audio.stopRingtone()
          socialVm.declineOrEndCall(incoming)
        },
        onQuickDeclineDm = { msg ->
          socialVm.audio.stopRingtone()
          socialVm.quickDeclineCallWithDm(incoming, myCallsign, msg)
        },
      )
    }
  }

  activeCall?.let { active ->
    ActiveNativeCallScreen(
      session = active,
      myUid = myUid,
      myCallsign = myCallsign,
      socialVm = socialVm,
    )
  }
}

@Composable
private fun IncomingCallBanner(
  session: CallSession,
  onAccept: () -> Unit,
  onDecline: () -> Unit,
  onQuickDeclineDm: (String) -> Unit,
) {
  Dialog(
    onDismissRequest = {},
    properties =
      DialogProperties(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false,
      ),
  ) {
    ConfigureEdgeToEdgeDialogWindow(fullScreen = true)
    val safeInsets = rememberHostWindowSafeInsets()
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.55f))
          .padding(
            start = safeInsets.start + 14.dp,
            end = safeInsets.end + 14.dp,
            top = safeInsets.top + 16.dp,
            bottom = safeInsets.bottom + 16.dp,
          ),
      contentAlignment = Alignment.TopCenter,
    ) {
      TacticalCard(
        highlight = true,
        title = "INCOMING ${session.callType} DISPATCH",
        modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp).testTag("incoming_call_banner"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              "INCOMING ${session.callType} CALL",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              session.callerCallsign.ifBlank { "Pilot" },
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              "Native Hardware Camera & Live Voice Link",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(
              onClick = onDecline,
              colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color(0xFFDC2626),
                contentColor = Color.White,
              ),
              modifier = Modifier.size(48.dp).testTag("decline_call_button"),
            ) {
              Icon(Icons.Default.CallEnd, contentDescription = "Decline Call", modifier = Modifier.size(22.dp))
            }
            Button(
              onClick = onAccept,
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
              modifier = Modifier.heightIn(min = 48.dp).testTag("accept_call_button"),
            ) {
              Icon(
                if (session.callType == "VIDEO") Icons.Default.Videocam else Icons.Default.Call,
                contentDescription = "Accept Call",
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(4.dp))
              Text("Answer", maxLines = 1, softWrap = false)
            }
          }
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(
            listOf(
              "Riding now — will call back at next stop",
              "Battery low — sending DM instead",
              "At charger — give me 5 mins",
            )
          ) { reply ->
            AssistChip(
              onClick = { onQuickDeclineDm(reply) },
              label = { Text("Decline: ${reply.take(22)}", maxLines = 1, softWrap = false) },
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveNativeCallScreen(
  session: CallSession,
  myUid: String,
  myCallsign: String,
  socialVm: CopilotSocialViewModel,
) {
  val ctx = LocalContext.current
  val scope = rememberCoroutineScope()
  val minimized by socialVm.callMinimized.collectAsStateWithLifecycle()
  val signals by socialVm.callSignals.collectAsStateWithLifecycle()
  val remoteVideoFrame by socialVm.remoteVideoStream.collectAsStateWithLifecycle()
  val remoteAudioFrame by socialVm.remoteAudioStream.collectAsStateWithLifecycle()

  val isCaller = session.callerUid == myUid
  val isVideo = session.callType == "VIDEO"
  val peerCallsign = if (isCaller) session.calleeCallsign else session.callerCallsign
  val isCallActive = session.status == "ACTIVE"

  var micMuted by remember(session.id) { mutableStateOf(false) }
  var camEnabled by remember(session.id, isVideo) { mutableStateOf(isVideo) }
  var useFrontCamera by remember(session.id) { mutableStateOf(true) }
  var speakerOn by remember(session.id) { mutableStateOf(true) }
  var lowBandwidthMode by remember(session.id) { mutableStateOf(false) }
  var pttHelmetMode by remember(session.id) { mutableStateOf(false) }
  var pttTalking by remember(session.id) { mutableStateOf(false) }
  var nightDimmer by remember(session.id) { mutableStateOf(false) }
  var showInCallChat by remember(session.id) { mutableStateOf(false) }
  var inCallDraft by remember(session.id) { mutableStateOf("") }
  var cameraStatus by remember(session.id) { mutableStateOf(if (isVideo) "Starting Camera..." else "Voice Only") }
  var elapsedSec by remember(session.id) { mutableIntStateOf(0) }
  var remoteBitmap by remember(session.id) { mutableStateOf<Bitmap?>(null) }
  var lastDecodedVideoSeq by remember(session.id) { mutableLongStateOf(-1L) }
  var rxVideoCount by remember(session.id) { mutableIntStateOf(0) }
  var txVideoCount by remember(session.id) { mutableIntStateOf(0) }
  var swapPreviewMain by remember(session.id) { mutableStateOf(false) }

  // Native hardware voice transceiver stays alive across expanded <-> minimized PiP transitions
  val voiceEngine = remember(session.id) { NativeCallVoiceEngine(ctx.applicationContext) }
  val micLevel by voiceEngine.micLevel.collectAsStateWithLifecycle()
  val voiceRxActive by voiceEngine.voiceRxActive.collectAsStateWithLifecycle()

  val chatAndPingSignals =
    remember(signals) {
      signals.filter { it.signalType == "CHAT" || it.signalType == "PING" }
    }
  val latestPing = remember(chatAndPingSignals) { chatAndPingSignals.lastOrNull { it.signalType == "PING" } }

  val requiredPerms =
    remember(isVideo, camEnabled) {
      if (isVideo || camEnabled) arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
      else arrayOf(Manifest.permission.RECORD_AUDIO)
    }

  var hasMicPerm by remember(session.id) {
    mutableStateOf(
      ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    )
  }
  var hasCamPerm by remember(session.id) {
    mutableStateOf(
      ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    )
  }
  val hasMediaPerms = if (isVideo || camEnabled) (hasMicPerm && hasCamPerm) else hasMicPerm

  val permLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
      hasMicPerm =
        result[Manifest.permission.RECORD_AUDIO]
          ?: (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
      hasCamPerm =
        result[Manifest.permission.CAMERA]
          ?: (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

  LaunchedEffect(session.id, isVideo) {
    if (!hasMediaPerms) {
      permLauncher.launch(requiredPerms)
    }
    socialVm.audio.setCallSpeakerphone(speakerOn)
  }

  // Keep voice engine parameters synchronized in real time
  LaunchedEffect(isCallActive, micMuted, pttHelmetMode, pttTalking, speakerOn) {
    val micOn = if (pttHelmetMode) pttTalking else !micMuted
    voiceEngine.isCallActive = isCallActive
    voiceEngine.isMicEnabled = micOn
    voiceEngine.isSpeakerBoost = speakerOn
    socialVm.audio.setCallSpeakerphone(speakerOn)
  }

  // Start native AudioRecord + AudioTrack as soon as mic permission is granted
  LaunchedEffect(session.id, hasMicPerm) {
    if (hasMicPerm) {
      voiceEngine.start { seq, active, b64 ->
        socialVm.publishCallAudioChunk(session.id, seq, active, b64)
      }
    }
  }

  // Feed remote peer audio stream frames to AudioTrack immediately
  LaunchedEffect(remoteAudioFrame?.seq, remoteAudioFrame?.active) {
    val af = remoteAudioFrame ?: return@LaunchedEffect
    if (af.active && af.payload.isNotBlank()) {
      voiceEngine.enqueueRemoteAudio(af.seq, af.payload)
    }
  }

  // Decode remote peer JPEG video frames on Dispatchers.Default
  LaunchedEffect(remoteVideoFrame?.seq, remoteVideoFrame?.active) {
    val vf = remoteVideoFrame ?: return@LaunchedEffect
    if (!vf.active || vf.payload.isBlank()) {
      remoteBitmap = null
      return@LaunchedEffect
    }
    if (vf.seq == lastDecodedVideoSeq) return@LaunchedEffect
    lastDecodedVideoSeq = vf.seq
    val decoded = withContext(Dispatchers.Default) { decodeCallVideoFrame(vf.payload) }
    if (decoded != null) {
      remoteBitmap = decoded
      rxVideoCount++
    }
  }

  // Publish camera paused marker when user disables camera
  LaunchedEffect(session.id, camEnabled, isCallActive) {
    if (isCallActive && !camEnabled) {
      socialVm.publishCallVideoFrame(
        sessionId = session.id,
        seq = System.currentTimeMillis() % 90_000_000L,
        active = false,
        jpegBase64 = "",
      )
    }
  }

  // Call duration timer once answered
  LaunchedEffect(session.id, isCallActive) {
    if (isCallActive) {
      socialVm.audio.setCallSpeakerphone(speakerOn)
      while (true) {
        delay(1000L)
        elapsedSec++
      }
    }
  }

  DisposableEffect(session.id) {
    onDispose {
      voiceEngine.release()
      socialVm.audio.resetCallAudioMode()
    }
  }

  val timerLabel = "%02d:%02d".format(elapsedSec / 60, elapsedSec % 60)
  val liveLinkStatus =
    when {
      !hasMediaPerms -> "Grant ${if (isVideo || camEnabled) "Camera & Mic" else "Mic"} Permission"
      !isCallActive -> "Ringing ${peerCallsign.ifBlank { "Pilot" }}..."
      remoteBitmap != null && voiceRxActive -> "Live HD Video & Voice Connected"
      remoteBitmap != null -> "Live Video Stream Connected"
      voiceRxActive -> "Live Voice Connected"
      txVideoCount > 0 -> "Live Video Sending • Waiting for ${peerCallsign.ifBlank { "Peer" }}"
      else -> "Live Call Connected • $cameraStatus"
    }

  if (minimized) {
    val safeInsets = rememberHostWindowSafeInsets()
    // FLOATING PiP CALL PILL (USER CAN BROWSE COCKPIT / RADAR / BOARD WHILE LIVE VOICE CONTINUES)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(
          start = safeInsets.start + 12.dp,
          end = safeInsets.end + 12.dp,
          top = safeInsets.top + 8.dp,
        ),
      contentAlignment = Alignment.TopCenter,
    ) {
      Surface(
        color = Color(0xF20B1420),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)),
        tonalElevation = 8.dp,
        modifier =
          Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .clickable { socialVm.setCallMinimized(false) }
            .testTag("minimized_call_pill"),
      ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(Modifier.width(8.dp))
          Column {
            Text(
              "${peerCallsign.ifBlank { "Pilot" }} • ${session.callType} $timerLabel",
              color = Color.White,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
            Text(liveLinkStatus, color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          IconButton(onClick = { micMuted = !micMuted }) {
            Icon(
              if (micMuted) Icons.Default.MicOff else Icons.Default.Mic,
              contentDescription = "Mute",
              tint = if (micMuted) MaterialTheme.colorScheme.error else Color.White,
            )
          }
          IconButton(onClick = { speakerOn = !speakerOn }) {
            Icon(
              Icons.Default.VolumeUp,
              contentDescription = "Speakerphone",
              tint = if (speakerOn) MaterialTheme.colorScheme.primary else Color.White,
            )
          }
          IconButton(onClick = { socialVm.setCallMinimized(false) }) {
            Icon(Icons.Default.OpenInFull, contentDescription = "Expand Call", tint = MaterialTheme.colorScheme.primary)
          }
          IconButton(onClick = { socialVm.declineOrEndCall(session) }) {
            Icon(Icons.Default.CallEnd, contentDescription = "End Call", tint = MaterialTheme.colorScheme.error)
          }
        }
        }
      }
    }
    return
  }

  // FULL-SCREEN TOP-LAYER DIALOG SO ACTIVE VIDEO/AUDIO CALL ALWAYS APPEARS ABOVE DMs, RADAR & ALL MODALS!
  Dialog(
    onDismissRequest = {
      if (showInCallChat) showInCallChat = false
      else socialVm.setCallMinimized(true)
    },
    properties =
      DialogProperties(
        usePlatformDefaultWidth = false,
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        decorFitsSystemWindows = false,
      ),
  ) {
    ConfigureEdgeToEdgeDialogWindow(fullScreen = true)
    val safeInsets = rememberHostWindowSafeInsets()
    val effectiveBottomInset = maxOf(safeInsets.bottom, safeInsets.imeBottom)
    val pipBottomClearance = effectiveBottomInset + (if (pttHelmetMode) 156.dp else 98.dp)

    BackHandler(enabled = true) {
      if (showInCallChat) {
        showInCallChat = false
      } else {
        socialVm.setCallMinimized(true)
      }
    }

    val showLocalCamera = (isVideo || camEnabled) && camEnabled && hasCamPerm && !lowBandwidthMode
    val peerVideoBmp = remoteBitmap
    val localIsFullScreen = showLocalCamera && (peerVideoBmp == null || swapPreviewMain)

    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(Color(0xFF070C14))
          .testTag("active_call_overlay"),
    ) {
      // 1. BACKGROUND FULL-SCREEN VIDEO STAGE (REMOTE PEER VIDEO OR LOCAL CAMERA PREVIEW)
      if (peerVideoBmp != null && !swapPreviewMain) {
        Image(
          bitmap = peerVideoBmp.asImageBitmap(),
          contentDescription = "Live video from $peerCallsign",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      } else if (!localIsFullScreen) {
        // Stylized Voice / Waiting-for-Video Stage
        Box(
          modifier =
            Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  listOf(Color(0xFF0A1424), Color(0xFF070C14), Color(0xFF0B1928))
                )
              ),
          contentAlignment = Alignment.Center,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val pulseScale = (1f + micLevel * 0.25f).coerceIn(1f, 1.3f)
            Box(
              modifier =
                Modifier
                  .size(116.dp)
                  .scale(pulseScale)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                  .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
              contentAlignment = Alignment.Center,
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                  if (isVideo) Icons.Default.Videocam else Icons.Default.GraphicEq,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(34.dp),
                )
                Text(
                  if (isVideo) "VIDEO" else "VOICE",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Black,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
            Spacer(Modifier.height(14.dp))
            Text(
              peerCallsign.ifBlank { "Pilot" },
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold,
              color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
              liveLinkStatus,
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF94A3B8),
            )
          }
        }
      }

      // 2. SINGLE NATIVE CAMERAX VIEW (EITHER FULL-SCREEN WHILE RINGING/WAITING OR ROUNDED PiP OVERLAY WHEN PEER VIDEO IS LIVE)
      if (showLocalCamera) {
        Box(
          modifier =
            if (localIsFullScreen) {
              Modifier.fillMaxSize()
            } else {
              Modifier
                .align(Alignment.BottomEnd)
                .padding(end = safeInsets.end + 14.dp, bottom = pipBottomClearance)
                .size(width = 112.dp, height = 156.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                .clickable { swapPreviewMain = !swapPreviewMain }
            },
        ) {
          NativeCallCameraPreview(
            modifier = Modifier.fillMaxSize(),
            useFrontCamera = useFrontCamera,
            streamingEnabled = isCallActive && camEnabled && !lowBandwidthMode,
            lowBandwidth = lowBandwidthMode,
            onStatusChange = { st -> cameraStatus = st },
            onEncodedFrame = { seq, b64 ->
              val ok = socialVm.publishCallVideoFrame(session.id, seq, true, b64)
              if (ok) txVideoCount++
            },
          )
          if (!localIsFullScreen) {
            Surface(
              color = Color(0xAA060A12),
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
            ) {
              Text(
                "YOU • ${if (useFrontCamera) "FRONT" else "REAR"}",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }
      }

      // If swapped (local full-screen while peer video is live), show peer video in PiP corner
      if (swapPreviewMain && peerVideoBmp != null) {
        Box(
          modifier =
            Modifier
              .align(Alignment.BottomEnd)
              .padding(end = safeInsets.end + 14.dp, bottom = pipBottomClearance)
              .size(width = 112.dp, height = 156.dp)
              .clip(RoundedCornerShape(16.dp))
              .border(2.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(16.dp))
              .clickable { swapPreviewMain = false },
        ) {
          Image(
            bitmap = peerVideoBmp.asImageBitmap(),
            contentDescription = "Remote peer video",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }

      // Optional Night Dimmer veil
      if (nightDimmer) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xC8030508)))
      }

      // 3. TRANSLUCENT HUD OVERLAY (TOP STATUS + CENTER PERMS/CHAT + BOTTOM CONTROLS)
      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .padding(
              start = safeInsets.start + 12.dp,
              end = safeInsets.end + 12.dp,
              top = safeInsets.top + 8.dp,
              bottom = effectiveBottomInset + 12.dp,
            ),
        contentAlignment = Alignment.Center,
      ) {
      Column(
        modifier = Modifier.fillMaxSize().widthIn(max = 580.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // TOP HUD BAR
        Surface(
          color = Color(0xBA060A12),
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              IconButton(
                onClick = { socialVm.setCallMinimized(true) },
                modifier = Modifier.size(48.dp).testTag("call_minimize_button"),
              ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize Call to PiP", tint = Color.White)
              }
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.weight(1f),
              ) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text(
                  "${session.callType} • ${if (speakerOn) "SPEAKER" else "EARPIECE"} • ${if (lowBandwidthMode) "LOW-BW" else "HD"}",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Row {
                IconButton(onClick = { nightDimmer = !nightDimmer }, modifier = Modifier.size(48.dp)) {
                  Icon(
                    Icons.Default.Brightness2,
                    contentDescription = "Night Dimmer",
                    tint = if (nightDimmer) MaterialTheme.colorScheme.primary else Color.White,
                  )
                }
                IconButton(
                  onClick = { showInCallChat = !showInCallChat },
                  modifier = Modifier.size(48.dp).testTag("call_chat_toggle_button"),
                ) {
                  Icon(
                    Icons.Default.Chat,
                    contentDescription = "In-Call Chat & Pings",
                    tint = if (showInCallChat) MaterialTheme.colorScheme.primary else Color.White,
                  )
                }
              }
            }

            Text(
              peerCallsign.ifBlank { "Pilot" },
              style = MaterialTheme.typography.titleLarge,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              if (isCallActive) "$liveLinkStatus • $timerLabel" else liveLinkStatus,
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFCBD5E1),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(6.dp))
            FlowRow(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              FilterChip(
                selected = pttHelmetMode,
                onClick = {
                  pttHelmetMode = !pttHelmetMode
                  pttTalking = false
                },
                leadingIcon = { Icon(Icons.Default.RecordVoiceOver, null, Modifier.size(15.dp)) },
                label = { Text("PTT", maxLines = 1, softWrap = false) },
              )
              FilterChip(
                selected = lowBandwidthMode,
                onClick = { lowBandwidthMode = !lowBandwidthMode },
                leadingIcon = { Icon(Icons.Default.SignalCellularAlt, null, Modifier.size(15.dp)) },
                label = { Text("Low-BW", maxLines = 1, softWrap = false) },
              )
              FilterChip(
                selected = isVideo,
                onClick = {
                  val nextType = if (isVideo) "AUDIO" else "VIDEO"
                  socialVm.switchActiveCallType(nextType)
                  val wantCam = nextType == "VIDEO"
                  camEnabled = wantCam
                  if (wantCam && !hasCamPerm) {
                    permLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA))
                  }
                },
                leadingIcon = { Icon(Icons.Default.Videocam, null, Modifier.size(15.dp)) },
                label = { Text(if (isVideo) "Audio" else "Video", maxLines = 1, softWrap = false) },
              )
            }

            latestPing?.let { lp ->
              val parts = lp.payload.split("|", limit = 2)
              val who = parts.firstOrNull().orEmpty()
              val code = parts.getOrNull(1) ?: lp.payload
              Spacer(Modifier.height(6.dp))
              Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), shape = RoundedCornerShape(10.dp)) {
                Text(
                  "LIVE PING • $who: $code",
                  color = Color.White,
                  style = MaterialTheme.typography.labelMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }
            }
          }
        }

        // CENTER STAGE: PERMISSION CARD OR IN-CALL CHAT OVERLAY
        Box(
          modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp),
          contentAlignment = Alignment.Center,
        ) {
          if (!hasMediaPerms) {
            TacticalCard {
              Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                  "Microphone${if (isVideo || camEnabled) " & Camera" else ""} permission needed for live call",
                  style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(10.dp))
                Button(
                  onClick = { permLauncher.launch(requiredPerms) },
                  modifier = Modifier.testTag("grant_call_permissions_button"),
                ) {
                  Text("Grant Call Permissions")
                }
              }
            }
          } else if (showInCallChat) {
            Surface(
              color = Color(0xEB0B111A),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxSize(),
            ) {
              Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                  Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    "IN-CALL TACTICAL CHAT & PINGS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                  )
                  IconButton(onClick = { showInCallChat = false }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, "Close Chat", tint = Color.White)
                  }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  items(listOf("SLOW DOWN", "CHARGER STOP", "ROAD HAZARD", "PULLING OVER", "LOW BATTERY", "ALL CLEAR")) { ping ->
                    AssistChip(
                      onClick = { socialVm.sendInCallPing(session.id, myCallsign, ping) },
                      label = { Text(ping) },
                    )
                  }
                }
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  if (chatAndPingSignals.isEmpty()) {
                    item {
                      Text(
                        "Send a quick ride ping or live text message during the call.",
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.bodySmall,
                      )
                    }
                  }
                  items(chatAndPingSignals, key = { it.id }) { sig ->
                    val parts = sig.payload.split("|", limit = 2)
                    val sender = parts.firstOrNull().orEmpty()
                    val msg = parts.getOrNull(1) ?: sig.payload
                    TacticalCard(highlight = sig.signalType == "PING") {
                      Text(
                        if (sig.signalType == "PING") "PING • $sender" else sender,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                      )
                      Text(msg, style = MaterialTheme.typography.bodySmall)
                    }
                  }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                  OutlinedTextField(
                    value = inCallDraft,
                    onValueChange = { inCallDraft = it },
                    placeholder = { Text("In-call message...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                  )
                  Spacer(Modifier.width(6.dp))
                  Button(onClick = {
                    if (inCallDraft.isNotBlank()) {
                      socialVm.sendInCallChat(session.id, myCallsign, inCallDraft)
                      inCallDraft = ""
                    }
                  }) {
                    Icon(Icons.Default.Send, "Send")
                  }
                }
              }
            }
          }
        }

        // PTT HELMET BUTTON WHEN ACTIVE
        if (pttHelmetMode) {
          Button(
            onClick = { pttTalking = !pttTalking },
            colors =
              ButtonDefaults.buttonColors(
                containerColor = if (pttTalking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
              ),
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(bottom = 8.dp),
          ) {
            Icon(if (pttTalking) Icons.Default.Mic else Icons.Default.MicOff, null)
            Spacer(Modifier.width(8.dp))
            Text(if (pttTalking) "TRANSMITTING LIVE (TAP TO MUTE)" else "PTT HELMET STANDBY (TAP TO TALK)")
          }
        }

        // BOTTOM PRIMARY CONTROLS BAR
        Surface(
          color = Color(0xC8060A12),
          shape = RoundedCornerShape(26.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            FilledIconToggleButton(
              checked = micMuted,
              onCheckedChange = { muted -> micMuted = muted },
              modifier = Modifier.size(52.dp).testTag("call_mute_button"),
            ) {
              Icon(
                if (micMuted) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = if (micMuted) "Unmute Microphone" else "Mute Microphone",
              )
            }

            FilledIconToggleButton(
              checked = speakerOn,
              onCheckedChange = { enabled ->
                speakerOn = enabled
                socialVm.audio.setCallSpeakerphone(enabled)
              },
              modifier = Modifier.size(52.dp).testTag("call_speaker_button"),
            ) {
              Icon(Icons.Default.VolumeUp, contentDescription = "Toggle Speakerphone")
            }

            if (isVideo || camEnabled) {
              FilledIconToggleButton(
                checked = camEnabled,
                onCheckedChange = { enabled ->
                  camEnabled = enabled
                  if (enabled && !hasCamPerm) {
                    permLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA))
                  } else if (!enabled && isCallActive) {
                    scope.launch {
                      socialVm.publishCallVideoFrame(session.id, System.currentTimeMillis() % 90_000_000L, false, "")
                    }
                  }
                },
                modifier = Modifier.size(52.dp).testTag("call_camera_button"),
              ) {
                Icon(
                  if (camEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                  contentDescription = "Toggle Camera",
                )
              }

              FilledIconToggleButton(
                checked = false,
                onCheckedChange = {
                  useFrontCamera = !useFrontCamera
                },
                modifier = Modifier.size(52.dp).testTag("call_flip_camera_button"),
              ) {
                Icon(Icons.Default.Cameraswitch, contentDescription = "Flip Front/Rear Camera")
              }
            }

            FilledIconButton(
              onClick = { socialVm.declineOrEndCall(session) },
              colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color(0xFFDC2626),
                contentColor = Color.White,
              ),
              modifier = Modifier.size(56.dp).testTag("end_call_button"),
            ) {
              Icon(
                Icons.Default.CallEnd,
                contentDescription = "End Call",
                modifier = Modifier.size(26.dp),
              )
            }
          }
        }
      }
      }
    }
  }
}

/**
 * Legacy WebRTC HTML blueprint retained for diagnostic compatibility tests.
 * Active live video/audio calls now use NativeCallCameraPreview + NativeCallVoiceEngine
 * with fixed-slot Firestore streams so calls work reliably across all Android devices & NATs.
 */
fun buildWebRtcCallHtml(isCaller: Boolean, isVideo: Boolean): String =
  """
<!doctype html>
<html>
<head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
<body>
<script>
const IS_CALLER = $isCaller;
const IS_VIDEO = $isVideo;
const pc = new RTCPeerConnection({ iceServers: [{ urls: 'stun:stun.l.google.com:19302' }] });
</script>
</body>
</html>
  """.trimIndent()
