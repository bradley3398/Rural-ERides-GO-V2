package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CallSession
import com.example.ui.components.TacticalCard
import com.example.viewmodel.CopilotSocialViewModel
import com.example.viewmodel.SessionViewModel
import kotlinx.coroutines.delay
import org.json.JSONObject

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
    ActiveWebRtcCallScreen(
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
  androidx.compose.ui.window.Dialog(
    onDismissRequest = {},
    properties =
      androidx.compose.ui.window.DialogProperties(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = false,
      ),
  ) {
    Box(
      modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 14.dp, vertical = 24.dp),
      contentAlignment = Alignment.TopCenter,
    ) {
    TacticalCard(
      highlight = true,
      title = "INCOMING ${session.callType} DISPATCH",
      modifier = Modifier.fillMaxWidth().testTag("incoming_call_banner"),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            "INCOMING ${session.callType} CALL",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            session.callerCallsign.ifBlank { "Pilot" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            "Encrypted P2P WebRTC Audio/Video Link",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = onDecline,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.testTag("decline_call_button"),
          ) {
            Icon(Icons.Default.CallEnd, contentDescription = "Decline Call", modifier = Modifier.size(18.dp))
          }
          Button(
            onClick = onAccept,
            modifier = Modifier.testTag("accept_call_button"),
          ) {
            Icon(
              if (session.callType == "VIDEO") Icons.Default.Videocam else Icons.Default.Call,
              contentDescription = "Accept Call",
              modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text("Answer")
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
            label = { Text("Decline: ${reply.take(22)}") },
          )
        }
      }
    }
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ActiveWebRtcCallScreen(
  session: CallSession,
  myUid: String,
  myCallsign: String,
  socialVm: CopilotSocialViewModel,
) {
  val minimized by socialVm.callMinimized.collectAsStateWithLifecycle()

  val ctx = LocalContext.current
  val signals by socialVm.callSignals.collectAsStateWithLifecycle()

  val isCaller = session.callerUid == myUid
  val isVideo = session.callType == "VIDEO"
  val peerCallsign = if (isCaller) session.calleeCallsign else session.callerCallsign

  var micMuted by remember(session.id) { mutableStateOf(false) }
  var camEnabled by remember(session.id, isVideo) { mutableStateOf(isVideo) }
  var speakerOn by remember(session.id) { mutableStateOf(true) }
  var lowBandwidthMode by remember(session.id) { mutableStateOf(false) }
  var pttHelmetMode by remember(session.id) { mutableStateOf(false) }
  var pttTalking by remember(session.id) { mutableStateOf(false) }
  var nightDimmer by remember(session.id) { mutableStateOf(false) }
  var showInCallChat by remember(session.id) { mutableStateOf(false) }
  var inCallDraft by remember(session.id) { mutableStateOf("") }
  var webStatus by remember(session.id) {
    mutableStateOf(if (session.status == "ACTIVE") "Connecting Live Call..." else "Ringing $peerCallsign...")
  }
  var elapsedSec by remember(session.id) { mutableIntStateOf(0) }
  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var webReady by remember(session.id) { mutableStateOf(false) }
  val processedRtcSignalIds = remember(session.id) { mutableSetOf<String>() }

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
      if (hasMicPerm) {
        webViewRef?.evaluateJavascript("window.retryMediaInit && window.retryMediaInit(${isVideo || camEnabled});", null)
      }
    }

  LaunchedEffect(session.id, isVideo) {
    if (!hasMediaPerms) permLauncher.launch(requiredPerms)
    socialVm.audio.setCallSpeakerphone(speakerOn)
  }

  LaunchedEffect(speakerOn, webReady) {
    socialVm.audio.setCallSpeakerphone(speakerOn)
    if (webReady) {
      webViewRef?.evaluateJavascript("window.setSpeakerBoost && window.setSpeakerBoost($speakerOn);", null)
    }
  }

  LaunchedEffect(pttHelmetMode, pttTalking, micMuted, webReady) {
    val micOn = if (pttHelmetMode) pttTalking else !micMuted
    if (webReady) {
      webViewRef?.evaluateJavascript("window.setMicEnabled && window.setMicEnabled($micOn);", null)
    }
  }

  LaunchedEffect(isVideo, webReady, hasCamPerm) {
    if (webReady) {
      val enableVideo = isVideo && hasCamPerm && !lowBandwidthMode
      camEnabled = enableVideo
      webViewRef?.evaluateJavascript("window.setCamEnabled && window.setCamEnabled($enableVideo);", null)
    }
  }

  LaunchedEffect(session.status, webReady) {
    if (webReady) {
      val isActive = session.status == "ACTIVE"
      webViewRef?.evaluateJavascript("window.setCallActive && window.setCallActive($isActive);", null)
    }
    if (session.status == "ACTIVE") {
      socialVm.audio.setCallSpeakerphone(speakerOn)
      while (true) {
        delay(1000L)
        elapsedSec++
      }
    }
  }

  DisposableEffect(session.id) {
    onDispose {
      socialVm.audio.resetCallAudioMode()
      webViewRef?.destroy()
    }
  }

  LaunchedEffect(signals, webReady, session.status, hasMicPerm) {
    val wv = webViewRef ?: return@LaunchedEffect
    if (!webReady || !hasMicPerm) return@LaunchedEffect
    if (session.status == "ACTIVE") {
      wv.evaluateJavascript("window.setCallActive && window.setCallActive(true);", null)
      if (isCaller) {
        wv.evaluateJavascript("window.startCallerOfferIfNeeded && window.startCallerOfferIfNeeded(false);", null)
      }
    }
    signals
      .filter {
        it.senderId != myUid &&
          it.signalType in listOf("OFFER", "ANSWER", "ICE") &&
          processedRtcSignalIds.add(it.id)
      }
      .forEach { sig ->
        val safeType = JSONObject.quote(sig.signalType)
        val safePayload = JSONObject.quote(sig.payload)
        wv.evaluateJavascript("window.onRemoteSignal && window.onRemoteSignal($safeType, $safePayload);", null)
      }
  }

  val timerLabel = "%02d:%02d".format(elapsedSec / 60, elapsedSec % 60)

  // Keep WebView mounted as soon as microphone permission is available so live audio/video never drops
  if (!minimized) {
    androidx.activity.compose.BackHandler {
      if (showInCallChat) {
        showInCallChat = false
      } else {
        socialVm.setCallMinimized(true)
      }
    }
  }

  Box(
    modifier = if (minimized) Modifier.fillMaxWidth() else Modifier.fillMaxSize(),
  ) {
    if (hasMicPerm) {
      AndroidView(
        modifier = if (minimized) Modifier.size(1.dp) else Modifier.fillMaxSize(),
        factory = { context ->
          WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            setBackgroundColor(android.graphics.Color.parseColor("#090E18"))
            webViewClient =
              object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                  webReady = true
                }
              }
            webChromeClient =
              object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest?) {
                  request?.grant(request.resources)
                }

                override fun getDefaultVideoPoster(): android.graphics.Bitmap {
                  return android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
                }
              }
            addJavascriptInterface(
              object {
                @JavascriptInterface
                fun sendSignal(type: String, payload: String) {
                  socialVm.sendWebRtcSignal(session.id, type, payload)
                }

                @JavascriptInterface
                fun onState(stateText: String) {
                  webStatus = stateText
                }
              },
              "CallBridge",
            )
            loadDataWithBaseURL(
              "https://ruralerides.app/",
              buildWebRtcCallHtml(isCaller = isCaller, isVideo = isVideo),
              "text/html",
              "utf-8",
              null,
            )
            webViewRef = this
          }
        },
      )
    }

    if (minimized) {
      // FLOATING PiP CALL PILL (USER CAN BROWSE COCKPIT / RADAR / BOARD WHILE TALKING)
      Surface(
        color = Color(0xEE0B1420),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 8.dp,
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
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
              Text(webStatus, color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
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
    } else {
      // Transparent center stage when nightDimmer is off so WebView video (#remoteVideo / #relayVideo / #localVideo) is 100% visible!
      Surface(
        color = Color(if (nightDimmer) 0xE6030508 else 0x00000000),
        modifier = Modifier.fillMaxSize().testTag("active_call_overlay"),
      ) {
        Column(
          modifier = Modifier.fillMaxSize().padding(14.dp),
          verticalArrangement = Arrangement.SpaceBetween,
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          // TOP HUD BAR (Translucent pill so live video shines behind it)
          Surface(
            color = Color(0xB3060A12),
            shape = RoundedCornerShape(16.dp),
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
                  modifier = Modifier.testTag("call_minimize_button"),
                ) {
                  Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize Call to PiP", tint = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                  Text(
                    "${session.callType} • ${if (speakerOn) "SPEAKER ON" else "EARPIECE"} • ${if (lowBandwidthMode) "LOW-BW" else "HD"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
                Row {
                  IconButton(onClick = { nightDimmer = !nightDimmer }) {
                    Icon(
                      Icons.Default.Brightness2,
                      contentDescription = "Night Dimmer",
                      tint = if (nightDimmer) MaterialTheme.colorScheme.primary else Color.White,
                    )
                  }
                  IconButton(
                    onClick = { showInCallChat = !showInCallChat },
                    modifier = Modifier.testTag("call_chat_toggle_button"),
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
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
              )
              Text(
                if (session.status == "ACTIVE") "$webStatus • $timerLabel" else webStatus,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1),
              )

              // LIVE CALL MODE CHIPS
              Spacer(Modifier.height(6.dp))
              LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                  FilterChip(
                    selected = pttHelmetMode,
                    onClick = {
                      pttHelmetMode = !pttHelmetMode
                      pttTalking = false
                    },
                    leadingIcon = { Icon(Icons.Default.RecordVoiceOver, null, Modifier.size(16.dp)) },
                    label = { Text("PTT Helmet") },
                  )
                }
                item {
                  FilterChip(
                    selected = lowBandwidthMode,
                    onClick = {
                      lowBandwidthMode = !lowBandwidthMode
                      if (lowBandwidthMode) {
                        camEnabled = false
                        webViewRef?.evaluateJavascript("window.setCamEnabled && window.setCamEnabled(false);", null)
                      }
                    },
                    leadingIcon = { Icon(Icons.Default.SignalCellularAlt, null, Modifier.size(16.dp)) },
                    label = { Text("Rural Low-BW") },
                  )
                }
                item {
                  FilterChip(
                    selected = isVideo,
                    onClick = {
                      val nextType = if (isVideo) "AUDIO" else "VIDEO"
                      socialVm.switchActiveCallType(nextType)
                      val wantCam = nextType == "VIDEO"
                      camEnabled = wantCam
                      if (wantCam && !hasCamPerm) {
                        permLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA))
                      } else {
                        webViewRef?.evaluateJavascript("window.setCamEnabled && window.setCamEnabled($wantCam);", null)
                      }
                    },
                    leadingIcon = { Icon(Icons.Default.Videocam, null, Modifier.size(16.dp)) },
                    label = { Text(if (isVideo) "Switch to Audio" else "Switch to Video") },
                  )
                }
              }

              latestPing?.let { lp ->
                val parts = lp.payload.split("|", limit = 2)
                val who = parts.firstOrNull().orEmpty()
                val code = parts.getOrNull(1) ?: lp.payload
                Spacer(Modifier.height(6.dp))
                Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), shape = RoundedCornerShape(10.dp)) {
                  Text(
                    "LIVE PING • $who: $code",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  )
                }
              }
            }
          }

          // CENTER STAGE: PERMISSION PROMPT OR IN-CALL CHAT OVERLAY
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
                  Button(onClick = { permLauncher.launch(requiredPerms) }) {
                    Text("Grant Call Permissions")
                  }
                }
              }
            } else if (showInCallChat) {
              Surface(
                color = Color(0xE60B111A),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxSize(),
              ) {
                Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("IN-CALL TACTICAL CHAT & PINGS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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
                        Text("Send a quick ride ping or live text message during the call.", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
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
                    }) { Icon(Icons.Default.Send, "Send") }
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
            color = Color(0xB3060A12),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 8.dp),
              horizontalArrangement = Arrangement.SpaceEvenly,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              FilledIconToggleButton(
                checked = micMuted,
                onCheckedChange = { muted ->
                  micMuted = muted
                  if (!pttHelmetMode) {
                    webViewRef?.evaluateJavascript("window.setMicEnabled && window.setMicEnabled(${!muted});", null)
                  }
                },
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
                  webViewRef?.evaluateJavascript("window.setSpeakerBoost && window.setSpeakerBoost($enabled);", null)
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
                    } else {
                      webViewRef?.evaluateJavascript("window.setCamEnabled && window.setCamEnabled($enabled);", null)
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
                    webViewRef?.evaluateJavascript("window.flipCamera && window.flipCamera();", null)
                  },
                  modifier = Modifier.size(52.dp).testTag("call_flip_camera_button"),
                ) {
                  Icon(Icons.Default.Cameraswitch, contentDescription = "Flip Front/Rear Camera")
                }
              }

              Button(
                onClick = { socialVm.declineOrEndCall(session) },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.size(60.dp).testTag("end_call_button"),
              ) {
                Icon(Icons.Default.CallEnd, contentDescription = "End Call", tint = Color.White)
              }
            }
          }
        }
      }
    }
  }
}

fun buildWebRtcCallHtml(isCaller: Boolean, isVideo: Boolean): String =
  """
<!doctype html>
<html>
<head>
<meta name="viewport" content="width=device-width,initial-scale=1">
<style>
  html, body { margin:0; padding:0; width:100%; height:100%; background:#090E18; color:#E2E8F0; font-family:sans-serif; overflow:hidden; }
  .stage { position:relative; width:100%; height:100%; display:flex; align-items:center; justify-content:center; background:#090E18; }
  #remoteVideo { position:absolute; inset:0; width:100%; height:100%; object-fit:cover; background:#090E18; z-index:1; display:none; }
  #relayVideo { position:absolute; inset:0; width:100%; height:100%; object-fit:cover; background:#090E18; z-index:2; display:none; }
  #localVideo { position:absolute; right:14px; bottom:108px; width:116px; height:162px; border-radius:12px; object-fit:cover; border:2px solid #39FF14; background:#141D2E; z-index:6; box-shadow:0 6px 20px rgba(0,0,0,0.6); display:${if (isVideo) "block" else "none"}; }
  .audioCard { position:relative; z-index:3; text-align:center; padding:24px; background:rgba(13,19,31,0.78); border:1px solid rgba(57,255,20,0.3); border-radius:20px; backdrop-filter:blur(6px); }
  .pulse { width:96px; height:96px; margin:0 auto 14px; border-radius:50%; background:rgba(57,255,20,0.16); border:2px solid #39FF14; display:flex; align-items:center; justify-content:center; font-weight:800; color:#39FF14; letter-spacing:1px; transition:transform 0.12s ease; }
  #levelText { font-size:12px; color:#94A3B8; margin-top:6px; }
</style>
</head>
<body>
<div class="stage">
  <video id="remoteVideo" autoplay playsinline></video>
  <img id="relayVideo" alt="" />
  <video id="localVideo" autoplay muted playsinline></video>
  <audio id="remoteAudio" autoplay playsinline></audio>
  <div id="audioOverlay" class="audioCard">
    <div id="voicePulse" class="pulse">${if (isVideo) "VIDEO" else "VOICE"}</div>
    <div id="statusLabel">Initializing Live Audio${if (isVideo) " & Video" else ""}...</div>
    <div id="levelText">Speakerphone Boost Active</div>
  </div>
</div>
<script>
const IS_CALLER = $isCaller;
let wantVideo = $isVideo;
let camOn = $isVideo;
let micOn = true;
let callActive = false;
let speakerBoost = true;
let pc = null;
let localStream = null;
let initPromise = null;
let offerSent = false;
let p2pConnected = false;
let p2pVideoActive = false;
let currentFacing = 'user';
const pendingIce = [];

let audioCtx = null;
let remoteGainNode = null;
let nextRelayPlayTime = 0;
let relayAudioProcessor = null;
let relayVideoTimer = null;
let relayCanvas = null;

function ensureAudioContext() {
  try {
    if (!audioCtx) {
      const AC = window.AudioContext || window.webkitAudioContext;
      if (AC) {
        audioCtx = new AC();
        remoteGainNode = audioCtx.createGain();
        remoteGainNode.gain.value = speakerBoost ? 2.4 : 1.25;
        remoteGainNode.connect(audioCtx.destination);
      }
    }
    if (audioCtx && audioCtx.state === 'suspended') {
      audioCtx.resume().catch(() => {});
    }
  } catch (e) {}
  return audioCtx;
}

function notifyState(txt) {
  const lbl = document.getElementById('statusLabel');
  if (lbl) lbl.textContent = txt;
  if (window.CallBridge) window.CallBridge.onState(txt);
}

function updateVideoStageVisibility() {
  const rv = document.getElementById('remoteVideo');
  const rimg = document.getElementById('relayVideo');
  const lv = document.getElementById('localVideo');
  const card = document.getElementById('audioOverlay');
  if (lv) lv.style.display = (camOn && localStream && localStream.getVideoTracks().length > 0) ? 'block' : 'none';
  if (p2pVideoActive && rv) {
    rv.style.display = 'block';
    if (rimg) rimg.style.display = 'none';
    if (card) card.style.display = 'none';
  }
}

async function acquireMediaStream(withVideo) {
  const audioConstraints = {
    echoCancellation: true,
    noiseSuppression: true,
    autoGainControl: true
  };
  if (withVideo) {
    try {
      return await navigator.mediaDevices.getUserMedia({
        audio: audioConstraints,
        video: { facingMode: currentFacing, width: { ideal: 640 }, height: { ideal: 480 } }
      });
    } catch (e1) {
      try {
        return await navigator.mediaDevices.getUserMedia({ audio: true, video: true });
      } catch (e2) {
        return await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
      }
    }
  } else {
    return await navigator.mediaDevices.getUserMedia({ audio: audioConstraints, video: false });
  }
}

function startLiveCloudMediaRelay() {
  ensureAudioContext();
  if (!localStream) return;

  // 1. Live Voice Audio Relay (8kHz Int8 PCM slices when speaking & P2P not yet directly connected)
  if (audioCtx && !relayAudioProcessor && localStream.getAudioTracks().length > 0) {
    try {
      const src = audioCtx.createMediaStreamSource(localStream);
      const proc = audioCtx.createScriptProcessor(4096, 1, 1);
      const silentGain = audioCtx.createGain();
      silentGain.gain.value = 0;
      let pcmAccumulator = [];
      let energySum = 0;
      let lastAudioRelayMs = 0;
      const inRate = audioCtx.sampleRate || 48000;
      const outRate = 8000;
      const step = inRate / outRate;

      proc.onaudioprocess = (ev) => {
        if (!micOn) {
          pcmAccumulator = [];
          energySum = 0;
          return;
        }
        const input = ev.inputBuffer.getChannelData(0);
        for (let i = 0; i < input.length; i += step) {
          const idx = Math.min(input.length - 1, Math.floor(i));
          const s = Math.max(-1, Math.min(1, input[idx]));
          energySum += s * s;
          pcmAccumulator.push(Math.round(s * 127));
        }
        if (pcmAccumulator.length >= 4400) {
          const slice = pcmAccumulator.splice(0, 4400);
          const rms = Math.sqrt(energySum / slice.length);
          energySum = 0;
          const pulse = document.getElementById('voicePulse');
          if (pulse) {
            const scale = 1 + Math.min(0.35, rms * 2.5);
            pulse.style.transform = 'scale(' + scale.toFixed(2) + ')';
          }
          const now = Date.now();
          if (callActive && !p2pConnected && rms > 0.006 && (now - lastAudioRelayMs) >= 480) {
            lastAudioRelayMs = now;
            let bin = '';
            for (let j = 0; j < slice.length; j++) {
              bin += String.fromCharCode((slice[j] + 128) & 0xFF);
            }
            const b64 = btoa(bin);
            if (window.CallBridge) {
              window.CallBridge.sendSignal('ICE', JSON.stringify({ __relayPcm: b64, rate: outRate }));
            }
          }
        }
      };
      src.connect(proc);
      proc.connect(silentGain);
      silentGain.connect(audioCtx.destination);
      relayAudioProcessor = proc;
    } catch (e) {}
  }

  // 2. Live Camera Video Relay (compact 160x120 JPEG frames every 420ms whenever camOn & P2P video not actively decoding)
  if (!relayVideoTimer) {
    relayCanvas = document.createElement('canvas');
    relayCanvas.width = 160;
    relayCanvas.height = 120;
    relayVideoTimer = setInterval(() => {
      if (!callActive || !camOn) return;
      const rv = document.getElementById('remoteVideo');
      const hasDecodedP2pVideo = p2pVideoActive && rv && rv.videoWidth > 0 && rv.readyState >= 2 && !rv.paused;
      if (hasDecodedP2pVideo) return;
      const lv = document.getElementById('localVideo');
      if (!lv || !lv.videoWidth || !lv.videoHeight) return;
      try {
        const ctx2d = relayCanvas.getContext('2d');
        ctx2d.drawImage(lv, 0, 0, relayCanvas.width, relayCanvas.height);
        const dataUrl = relayCanvas.toDataURL('image/jpeg', 0.34);
        if (dataUrl && dataUrl.length < 18500 && window.CallBridge) {
          window.CallBridge.sendSignal('ICE', JSON.stringify({ __relayVideo: dataUrl }));
        }
      } catch (e) {}
    }, 420);
  }
}

function playRelayPcm(b64, rate) {
  const ctx = ensureAudioContext();
  if (!ctx || !remoteGainNode) return;
  try {
    const bin = atob(b64);
    const len = bin.length;
    if (!len) return;
    const buf = ctx.createBuffer(1, len, rate || 8000);
    const ch = buf.getChannelData(0);
    for (let i = 0; i < len; i++) {
      ch[i] = ((bin.charCodeAt(i) & 0xFF) - 128) / 127.0;
    }
    const src = ctx.createBufferSource();
    src.buffer = buf;
    src.connect(remoteGainNode);
    const now = ctx.currentTime;
    if (nextRelayPlayTime < now || nextRelayPlayTime > now + 1.5) {
      nextRelayPlayTime = now + 0.03;
    }
    src.start(nextRelayPlayTime);
    nextRelayPlayTime += buf.duration;
    const lvl = document.getElementById('levelText');
    if (lvl) lvl.textContent = 'Live Two-Way Audio Active' + (speakerBoost ? ' (Speaker Boost)' : '');
  } catch (e) {}
}

function showRelayVideoFrame(dataUrl) {
  const rv = document.getElementById('remoteVideo');
  const hasLiveP2p = p2pVideoActive && rv && rv.videoWidth > 0 && rv.readyState >= 2 && !rv.paused;
  if (hasLiveP2p) return;
  const rimg = document.getElementById('relayVideo');
  const card = document.getElementById('audioOverlay');
  if (rimg && dataUrl) {
    rimg.src = dataUrl;
    rimg.style.display = 'block';
    if (rv) rv.style.display = 'none';
    if (card) card.style.display = 'none';
  }
}

function attachRemoteStream(stream) {
  ensureAudioContext();
  const rv = document.getElementById('remoteVideo');
  const ra = document.getElementById('remoteAudio');
  if (ra) {
    ra.srcObject = stream;
    ra.volume = 1.0;
    ra.play().catch(() => {});
  }
  if (rv) {
    rv.srcObject = stream;
    rv.play().catch(() => {});
    const checkDecoded = () => {
      if (rv.videoWidth > 0 && rv.readyState >= 2 && pc && (pc.connectionState === 'connected' || pc.iceConnectionState === 'connected' || pc.iceConnectionState === 'completed')) {
        p2pVideoActive = true;
        rv.style.display = 'block';
        const rimg = document.getElementById('relayVideo');
        if (rimg) rimg.style.display = 'none';
        updateVideoStageVisibility();
      }
    };
    rv.onloadedmetadata = () => {
      rv.play().catch(() => {});
      checkDecoded();
    };
    rv.onplaying = checkDecoded;
    rv.onresize = checkDecoded;
  }
  if (audioCtx && remoteGainNode && stream.getAudioTracks().length > 0) {
    try {
      const remoteSrc = audioCtx.createMediaStreamSource(stream);
      remoteSrc.connect(remoteGainNode);
    } catch (e) {}
  }
}

async function ensureReady() {
  if (initPromise) return initPromise;
  initPromise = (async () => {
    ensureAudioContext();
    if (!localStream) {
      try {
        localStream = await acquireMediaStream(wantVideo);
        const lv = document.getElementById('localVideo');
        if (lv) {
          lv.srcObject = localStream;
          lv.muted = true;
          lv.play().catch(() => {});
        }
        updateVideoStageVisibility();
        startLiveCloudMediaRelay();
      } catch (e) {
        notifyState('Mic/Camera error: ' + e.message);
        initPromise = null;
        return null;
      }
    }
    if (!pc) {
      pc = new RTCPeerConnection({
        iceServers: [
          { urls: 'stun:stun.l.google.com:19302' },
          { urls: 'stun:stun1.l.google.com:19302' },
          { urls: 'stun:stun2.l.google.com:19302' }
        ]
      });
      localStream.getTracks().forEach(t => pc.addTrack(t, localStream));
      pc.onicecandidate = ev => {
        if (ev.candidate && window.CallBridge) {
          window.CallBridge.sendSignal('ICE', JSON.stringify(ev.candidate));
        }
      };
      pc.ontrack = ev => {
        const stream = (ev.streams && ev.streams[0]) ? ev.streams[0] : new MediaStream([ev.track]);
        attachRemoteStream(stream);
        if (pc && (pc.connectionState === 'connected' || pc.iceConnectionState === 'connected' || pc.iceConnectionState === 'completed')) {
          p2pConnected = true;
          notifyState('Live P2P Connected');
        }
      };
      pc.onconnectionstatechange = () => {
        if (pc.connectionState === 'connected') {
          p2pConnected = true;
          const rv = document.getElementById('remoteVideo');
          if (rv && rv.videoWidth > 0 && rv.readyState >= 2) {
            p2pVideoActive = true;
            updateVideoStageVisibility();
          }
          notifyState('Live P2P Connected');
        } else if (pc.connectionState === 'connecting') {
          notifyState('Live Call Active');
        } else if (pc.connectionState === 'failed' || pc.connectionState === 'disconnected') {
          p2pConnected = false;
          p2pVideoActive = false;
          notifyState('Live Cloud Relay Active');
        }
      };
      // Notify peer that our WebRTC engine + media tracks are ready
      if (window.CallBridge) {
        window.CallBridge.sendSignal('ICE', JSON.stringify({ __webrtcReady: true }));
      }
    }
    return pc;
  })();
  return initPromise;
}

window.retryMediaInit = async (withVideo) => {
  wantVideo = !!withVideo;
  camOn = !!withVideo;
  initPromise = null;
  await ensureReady();
  if (IS_CALLER && callActive) {
    window.startCallerOfferIfNeeded(true);
  }
};

window.setCallActive = (active) => {
  callActive = !!active;
  ensureAudioContext();
  if (callActive) {
    notifyState(p2pConnected ? 'Live P2P Connected' : 'Live Call Active');
  }
};

window.setSpeakerBoost = (enabled) => {
  speakerBoost = !!enabled;
  ensureAudioContext();
  if (remoteGainNode && audioCtx) {
    remoteGainNode.gain.setTargetAtTime(speakerBoost ? 2.4 : 1.15, audioCtx.currentTime, 0.05);
  }
  const lvl = document.getElementById('levelText');
  if (lvl) lvl.textContent = speakerBoost ? 'Speakerphone Boost Active' : 'Earpiece / Headset Output';
};

window.startCallerOfferIfNeeded = async (forceResend) => {
  if (!IS_CALLER) return;
  if (offerSent && !forceResend) return;
  if (p2pConnected) return;
  const conn = await ensureReady();
  if (!conn) return;
  try {
    offerSent = true;
    const offer = await conn.createOffer({
      offerToReceiveAudio: true,
      offerToReceiveVideo: true
    });
    await conn.setLocalDescription(offer);
    if (window.CallBridge) {
      window.CallBridge.sendSignal('OFFER', JSON.stringify(offer));
    }
    notifyState('Live Call Active');
  } catch (e) {
    offerSent = false;
  }
};

window.onRemoteSignal = async (type, payloadStr) => {
  ensureAudioContext();
  let data = null;
  try {
    data = JSON.parse(payloadStr);
  } catch (e) {
    return;
  }
  if (!data) return;

  // Handle instant Cloud Media Relay frames immediately even before RTCPeerConnection completes
  if (type === 'ICE') {
    if (data.__relayPcm) {
      callActive = true;
      playRelayPcm(data.__relayPcm, data.rate || 8000);
      if (!p2pConnected) notifyState('Live Call Active');
      return;
    }
    if (data.__relayVideo) {
      callActive = true;
      showRelayVideoFrame(data.__relayVideo);
      if (!p2pConnected) notifyState('Live Video Active');
      return;
    }
    if (data.__webrtcReady) {
      callActive = true;
      if (IS_CALLER && !p2pConnected) {
        window.startCallerOfferIfNeeded(true);
      }
      return;
    }
  }

  const conn = await ensureReady();
  if (!conn) return;
  try {
    if (type === 'OFFER') {
      callActive = true;
      await conn.setRemoteDescription(new RTCSessionDescription(data));
      const answer = await conn.createAnswer();
      await conn.setLocalDescription(answer);
      if (window.CallBridge) {
        window.CallBridge.sendSignal('ANSWER', JSON.stringify(answer));
      }
      while (pendingIce.length) {
        const c = pendingIce.shift();
        try { await conn.addIceCandidate(new RTCIceCandidate(c)); } catch (e) {}
      }
      notifyState('Live Call Connected');
    } else if (type === 'ANSWER') {
      callActive = true;
      await conn.setRemoteDescription(new RTCSessionDescription(data));
      while (pendingIce.length) {
        const c = pendingIce.shift();
        try { await conn.addIceCandidate(new RTCIceCandidate(c)); } catch (e) {}
      }
      notifyState('Live Call Connected');
    } else if (type === 'ICE' && data.candidate) {
      if (conn.remoteDescription && conn.remoteDescription.type) {
        try { await conn.addIceCandidate(new RTCIceCandidate(data)); } catch (e) {}
      } else {
        pendingIce.push(data);
      }
    }
  } catch (e) {}
};

window.setMicEnabled = (enabled) => {
  micOn = !!enabled;
  if (!localStream) return;
  localStream.getAudioTracks().forEach(t => t.enabled = micOn);
};

window.setCamEnabled = async (enabled) => {
  camOn = !!enabled;
  wantVideo = camOn;
  if (!localStream) return;
  const vTracks = localStream.getVideoTracks();
  if (camOn && vTracks.length === 0) {
    try {
      const vStream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: currentFacing, width: { ideal: 640 }, height: { ideal: 480 } },
        audio: false
      });
      const vt = vStream.getVideoTracks()[0];
      if (vt) {
        localStream.addTrack(vt);
        const lv = document.getElementById('localVideo');
        if (lv) {
          lv.srcObject = localStream;
          lv.play().catch(() => {});
        }
        if (pc) {
          const sender = pc.getSenders().find(s => s.track && s.track.kind === 'video');
          if (sender) await sender.replaceTrack(vt);
          else pc.addTrack(vt, localStream);
        }
      }
    } catch (e) {}
  } else {
    vTracks.forEach(t => t.enabled = camOn);
  }
  updateVideoStageVisibility();
};

window.flipCamera = async () => {
  if (!localStream) return;
  try {
    currentFacing = (currentFacing === 'user') ? 'environment' : 'user';
    const nextStream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: currentFacing, width: { ideal: 640 }, height: { ideal: 480 } },
      audio: false
    });
    const newTrack = nextStream.getVideoTracks()[0];
    if (!newTrack) return;
    if (pc) {
      const sender = pc.getSenders().find(s => s.track && s.track.kind === 'video');
      if (sender) await sender.replaceTrack(newTrack);
    }
    localStream.getVideoTracks().forEach(t => { t.stop(); localStream.removeTrack(t); });
    localStream.addTrack(newTrack);
    const lv = document.getElementById('localVideo');
    if (lv) {
      lv.srcObject = localStream;
      lv.play().catch(() => {});
    }
    notifyState('Camera: ' + (currentFacing === 'user' ? 'Front' : 'Rear'));
  } catch (e) {
    notifyState('Camera switch unavailable');
  }
};

ensureReady();
</script>
</body>
</html>
  """.trimIndent()

