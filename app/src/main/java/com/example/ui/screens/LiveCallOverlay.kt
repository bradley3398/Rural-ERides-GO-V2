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
  BackHandler(enabled = !minimized) { socialVm.setCallMinimized(true) }

  val ctx = LocalContext.current
  val signals by socialVm.callSignals.collectAsStateWithLifecycle()

  val isCaller = session.callerUid == myUid
  val isVideo = session.callType == "VIDEO"
  val peerCallsign = if (isCaller) session.calleeCallsign else session.callerCallsign

  var micMuted by remember(session.id) { mutableStateOf(false) }
  var camEnabled by remember(session.id) { mutableStateOf(isVideo) }
  var speakerOn by remember(session.id) { mutableStateOf(isVideo) }
  var lowBandwidthMode by remember(session.id) { mutableStateOf(false) }
  var pttHelmetMode by remember(session.id) { mutableStateOf(false) }
  var pttTalking by remember(session.id) { mutableStateOf(false) }
  var nightDimmer by remember(session.id) { mutableStateOf(false) }
  var showInCallChat by remember(session.id) { mutableStateOf(false) }
  var inCallDraft by remember(session.id) { mutableStateOf("") }
  var webStatus by remember(session.id) {
    mutableStateOf(if (session.status == "ACTIVE") "Establishing WebRTC P2P..." else "Ringing $peerCallsign...")
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
    remember(isVideo) {
      if (isVideo) arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
      else arrayOf(Manifest.permission.RECORD_AUDIO)
    }

  var hasMediaPerms by remember(session.id) {
    mutableStateOf(
      requiredPerms.all {
        ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED
      }
    )
  }

  val permLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
      hasMediaPerms = result.values.all { it }
    }

  LaunchedEffect(session.id) {
    if (!hasMediaPerms) permLauncher.launch(requiredPerms)
    socialVm.audio.setCallSpeakerphone(speakerOn)
  }

  LaunchedEffect(speakerOn) {
    socialVm.audio.setCallSpeakerphone(speakerOn)
  }

  LaunchedEffect(pttHelmetMode, pttTalking, micMuted) {
    val micOn = if (pttHelmetMode) pttTalking else !micMuted
    webViewRef?.evaluateJavascript("window.setMicEnabled && window.setMicEnabled($micOn);", null)
  }

  LaunchedEffect(session.status) {
    if (session.status == "ACTIVE") {
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

  LaunchedEffect(signals, webReady, session.status, hasMediaPerms) {
    val wv = webViewRef ?: return@LaunchedEffect
    if (!webReady || !hasMediaPerms) return@LaunchedEffect
    if (session.status == "ACTIVE" && isCaller) {
      wv.evaluateJavascript("window.startCallerOfferIfNeeded && window.startCallerOfferIfNeeded();", null)
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

  // Keep WebView mounted even when minimized so live WebRTC media never disconnects!
  Box(
    modifier = if (minimized) Modifier.fillMaxWidth() else Modifier.fillMaxSize(),
  ) {
    if (hasMediaPerms) {
      AndroidView(
        modifier = if (minimized) Modifier.size(1.dp) else Modifier.fillMaxSize(),
        factory = { context ->
          WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            setBackgroundColor(android.graphics.Color.parseColor("#0D131F"))
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
      Surface(
        color = Color(if (nightDimmer) 0xEE030508 else 0xB806090F),
        modifier = Modifier.fillMaxSize().testTag("active_call_overlay"),
      ) {
        Column(
          modifier = Modifier.fillMaxSize().padding(14.dp),
          verticalArrangement = Arrangement.SpaceBetween,
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          // TOP HUD BAR
          Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
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
                  "${session.callType} P2P • ${if (lowBandwidthMode) "LOW-BW" else "HD"}",
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
                    camEnabled = nextType == "VIDEO"
                    webViewRef?.evaluateJavascript("window.setCamEnabled && window.setCamEnabled($camEnabled);", null)
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

          // CENTER STAGE: PERMISSION PROMPT OR IN-CALL CHAT OVERLAY
          Box(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
          ) {
            if (!hasMediaPerms) {
              TacticalCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                  Text(
                    "Microphone${if (isVideo) " & Camera" else ""} permission needed for live P2P call",
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
          Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
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
              onCheckedChange = { speakerOn = it },
              modifier = Modifier.size(52.dp).testTag("call_speaker_button"),
            ) {
              Icon(Icons.Default.VolumeUp, contentDescription = "Toggle Speakerphone")
            }

            if (isVideo) {
              FilledIconToggleButton(
                checked = camEnabled,
                onCheckedChange = { enabled ->
                  camEnabled = enabled
                  webViewRef?.evaluateJavascript("window.setCamEnabled && window.setCamEnabled($enabled);", null)
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

fun buildWebRtcCallHtml(isCaller: Boolean, isVideo: Boolean): String =
  """
<!doctype html>
<html>
<head>
<meta name="viewport" content="width=device-width,initial-scale=1">
<style>
  html, body { margin:0; padding:0; width:100%; height:100%; background:#0D131F; color:#E2E8F0; font-family:sans-serif; overflow:hidden; }
  .stage { position:relative; width:100%; height:100%; display:flex; align-items:center; justify-content:center; }
  #remoteVideo { width:100%; height:100%; object-fit:cover; background:#090E18; }
  #localVideo { position:absolute; right:12px; bottom:110px; width:112px; height:156px; border-radius:10px; object-fit:cover; border:2px solid #39FF14; background:#141D2E; z-index:4; }
  .audioCard { text-align:center; padding:24px; }
  .pulse { width:92px; height:92px; margin:0 auto 14px; border-radius:50%; background:rgba(57,255,20,0.16); border:2px solid #39FF14; display:flex; align-items:center; justify-content:center; font-weight:700; color:#39FF14; }
</style>
</head>
<body>
<div class="stage">
  ${if (isVideo) """<video id="remoteVideo" autoplay playsinline></video><video id="localVideo" autoplay muted playsinline></video>""" else """<audio id="remoteAudio" autoplay></audio><div class="audioCard"><div class="pulse">VOICE</div><div id="statusLabel">Initializing WebRTC Audio...</div></div>"""}
</div>
<script>
const IS_CALLER = $isCaller;
const IS_VIDEO = $isVideo;
let pc = null;
let localStream = null;
let offerSent = false;
let currentFacing = 'user';
const pendingIce = [];

function notifyState(txt) {
  const lbl = document.getElementById('statusLabel');
  if (lbl) lbl.textContent = txt;
  if (window.CallBridge) window.CallBridge.onState(txt);
}

async function ensureReady() {
  if (!localStream) {
    try {
      localStream = await navigator.mediaDevices.getUserMedia({
        audio: true,
        video: IS_VIDEO ? { facingMode: currentFacing } : false
      });
      const lv = document.getElementById('localVideo');
      if (lv) lv.srcObject = localStream;
    } catch (e) {
      notifyState('Media access error: ' + e.message);
      return null;
    }
  }
  if (!pc) {
    pc = new RTCPeerConnection({ iceServers: [{ urls: 'stun:stun.l.google.com:19302' }] });
    localStream.getTracks().forEach(t => pc.addTrack(t, localStream));
    pc.onicecandidate = ev => {
      if (ev.candidate && window.CallBridge) {
        window.CallBridge.sendSignal('ICE', JSON.stringify(ev.candidate));
      }
    };
    pc.ontrack = ev => {
      const rv = document.getElementById('remoteVideo') || document.getElementById('remoteAudio');
      if (rv && ev.streams && ev.streams[0]) rv.srcObject = ev.streams[0];
      notifyState('P2P Connected');
    };
    pc.onconnectionstatechange = () => {
      if (pc.connectionState === 'connected') notifyState('P2P Connected');
      else if (pc.connectionState === 'connecting') notifyState('Connecting P2P...');
      else if (pc.connectionState === 'failed') notifyState('ICE Retry Needed');
    };
  }
  return pc;
}

window.startCallerOfferIfNeeded = async () => {
  if (!IS_CALLER || offerSent) return;
  const conn = await ensureReady();
  if (!conn) return;
  offerSent = true;
  const offer = await conn.createOffer();
  await conn.setLocalDescription(offer);
  window.CallBridge.sendSignal('OFFER', JSON.stringify(offer));
  notifyState('Offer Sent — Negotiating...');
};

window.onRemoteSignal = async (type, payloadStr) => {
  const conn = await ensureReady();
  if (!conn) return;
  const data = JSON.parse(payloadStr);
  if (type === 'OFFER') {
    await conn.setRemoteDescription(new RTCSessionDescription(data));
    const answer = await conn.createAnswer();
    await conn.setLocalDescription(answer);
    window.CallBridge.sendSignal('ANSWER', JSON.stringify(answer));
    while (pendingIce.length) await conn.addIceCandidate(new RTCIceCandidate(pendingIce.shift()));
    notifyState('Connected');
  } else if (type === 'ANSWER') {
    await conn.setRemoteDescription(new RTCSessionDescription(data));
    while (pendingIce.length) await conn.addIceCandidate(new RTCIceCandidate(pendingIce.shift()));
    notifyState('Connected');
  } else if (type === 'ICE') {
    if (conn.remoteDescription) await conn.addIceCandidate(new RTCIceCandidate(data));
    else pendingIce.push(data);
  }
};

window.setMicEnabled = (enabled) => {
  if (!localStream) return;
  localStream.getAudioTracks().forEach(t => t.enabled = enabled);
};

window.setCamEnabled = (enabled) => {
  if (!localStream) return;
  localStream.getVideoTracks().forEach(t => t.enabled = enabled);
};

window.flipCamera = async () => {
  if (!IS_VIDEO || !localStream || !pc) return;
  try {
    currentFacing = (currentFacing === 'user') ? 'environment' : 'user';
    const nextStream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: currentFacing }, audio: false });
    const newTrack = nextStream.getVideoTracks()[0];
    if (!newTrack) return;
    const sender = pc.getSenders().find(s => s.track && s.track.kind === 'video');
    if (sender) await sender.replaceTrack(newTrack);
    localStream.getVideoTracks().forEach(t => { t.stop(); localStream.removeTrack(t); });
    localStream.addTrack(newTrack);
    const lv = document.getElementById('localVideo');
    if (lv) lv.srcObject = localStream;
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
