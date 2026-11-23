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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.TacticalCard
import com.example.viewmodel.RadarViewModel
import com.example.viewmodel.SessionViewModel
import org.json.JSONObject

private val TACTICAL_CHANNELS =
  listOf(
    "SECTOR_01_OPEN" to "CH-01 OPEN",
    "GROUP_CONVOY" to "CH-02 CONVOY",
    "TRAIL_SCOUTS" to "CH-03 SCOUTS",
    "NIGHT_RIDERS" to "CH-04 NIGHT",
    "CHARGE_NET" to "CH-05 CHARGE",
    "EMERGENCY_SOS" to "CH-09 SOS",
  )

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WalkieTalkieSheet(
  sessionVm: SessionViewModel,
  radarVm: RadarViewModel,
  onClose: () -> Unit,
) {
  BackHandler(onBack = onClose)

  val ctx = LocalContext.current
  val activeUser by sessionVm.activeUser.collectAsStateWithLifecycle()
  val profile by sessionVm.profile.collectAsStateWithLifecycle()
  val settings by sessionVm.settings.collectAsStateWithLifecycle()
  val activeChannel by radarVm.activeChannelId.collectAsStateWithLifecycle()
  val presence by radarVm.presence.collectAsStateWithLifecycle()
  val speaker by radarVm.speaker.collectAsStateWithLifecycle()
  val logs by radarVm.walkieLogs.collectAsStateWithLifecycle()
  val webRtcSignals by radarVm.webRtcSignals.collectAsStateWithLifecycle()
  val transmitting by radarVm.isTransmitting.collectAsStateWithLifecycle()

  val myUid = activeUser?.uid.orEmpty()
  val myCallsign = profile?.callsign?.ifBlank { "PILOT" } ?: "PILOT"

  var hasMic by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  val micLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasMic = granted
    }

  // WebRTC Instant Walkie-Talkie live controls
  var handsFreeOpenMic by remember { mutableStateOf(false) }
  var speakerMuted by remember { mutableStateOf(false) }
  var speakerphoneOn by remember { mutableStateOf(true) }
  var rtcStatus by remember(activeChannel) { mutableStateOf("Connecting WebRTC Instant Mesh...") }
  var connectedPeerCount by remember(activeChannel) { mutableIntStateOf(0) }
  var livePttHeld by remember(activeChannel) { mutableStateOf(false) }
  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var webReady by remember(activeChannel) { mutableStateOf(false) }
  val processedSigIds = remember(activeChannel) { mutableSetOf<String>() }

  LaunchedEffect(activeChannel, hasMic, myUid) {
    radarVm.joinWalkiePresence(myCallsign, true)
    if (hasMic && myUid.isNotBlank()) {
      radarVm.audio.setCallSpeakerphone(speakerphoneOn)
      radarVm.sendWalkieWebRtcSignal(
        callsign = myCallsign,
        targetUid = "ALL",
        signalType = "JOIN",
        payload = myCallsign,
      )
    }
  }

  LaunchedEffect(speakerphoneOn) {
    radarVm.audio.setCallSpeakerphone(speakerphoneOn)
  }

  LaunchedEffect(livePttHeld, handsFreeOpenMic, webReady) {
    val micLive = livePttHeld || handsFreeOpenMic
    if (webReady) {
      webViewRef?.evaluateJavascript("window.setWalkieMicEnabled && window.setWalkieMicEnabled($micLive);", null)
    }
  }

  LaunchedEffect(speakerMuted, webReady) {
    if (webReady) {
      webViewRef?.evaluateJavascript("window.setWalkieSpeakerMuted && window.setWalkieSpeakerMuted($speakerMuted);", null)
    }
  }

  // Deliver remote WebRTC mesh signals into the hidden low-latency WebRTC audio engine
  LaunchedEffect(webRtcSignals, webReady, hasMic, myUid) {
    val wv = webViewRef ?: return@LaunchedEffect
    if (!webReady || !hasMic || myUid.isBlank()) return@LaunchedEffect
    webRtcSignals
      .filter { sig ->
        sig.senderId != myUid &&
          (sig.targetUid.isBlank() || sig.targetUid == "ALL" || sig.targetUid == myUid) &&
          processedSigIds.add(sig.id)
      }
      .forEach { sig ->
        val sFrom = JSONObject.quote(sig.senderId)
        val sCall = JSONObject.quote(sig.senderCallsign)
        val sType = JSONObject.quote(sig.signalType)
        val sPayload = JSONObject.quote(sig.payload)
        wv.evaluateJavascript(
          "window.onWalkieSignal && window.onWalkieSignal($sFrom, $sCall, $sType, $sPayload);",
          null,
        )
      }
  }

  DisposableEffect(activeChannel) {
    onDispose {
      radarVm.setLiveChannelSpeaker(myCallsign, false, settings.globalVolume)
      radarVm.sendWalkieWebRtcSignal(myCallsign, "ALL", "LEAVE", myCallsign)
      radarVm.clearMyWalkieWebRtcSignals()
      radarVm.joinWalkiePresence(myCallsign, false)
      radarVm.audio.resetCallAudioMode()
      webViewRef?.destroy()
      webViewRef = null
    }
  }

  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Hidden 1dp WebRTC P2P Multi-Peer Instant Audio Mesh Engine (STUN + Firestore Signaling)
      if (hasMic && myUid.isNotBlank()) {
        AndroidView(
          modifier = Modifier.size(1.dp),
          factory = { context ->
            WebView(context).apply {
              this.settings.javaScriptEnabled = true
              this.settings.domStorageEnabled = true
              this.settings.mediaPlaybackRequiresUserGesture = false
              this.settings.cacheMode = WebSettings.LOAD_NO_CACHE
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
                  fun sendSignal(targetUid: String, type: String, payload: String) {
                    radarVm.sendWalkieWebRtcSignal(myCallsign, targetUid, type, payload)
                  }

                  @JavascriptInterface
                  fun onMeshStatus(statusText: String, peerCount: Int) {
                    rtcStatus = statusText
                    connectedPeerCount = peerCount
                  }
                },
                "WalkieRtcBridge",
              )
              loadDataWithBaseURL(
                "https://ruralerides.app/walkie/$activeChannel",
                buildWalkieWebRtcMeshHtml(myUid = myUid, myCallsign = myCallsign),
                "text/html",
                "utf-8",
                null,
              )
              webViewRef = this
            }
          },
          update = { view ->
            if (view.tag != activeChannel) {
              view.tag = activeChannel
              webReady = false
              processedSigIds.clear()
              view.loadDataWithBaseURL(
                "https://ruralerides.app/walkie/$activeChannel",
                buildWalkieWebRtcMeshHtml(myUid = myUid, myCallsign = myCallsign),
                "text/html",
                "utf-8",
                null,
              )
            }
          },
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Default.Radio, null, tint = MaterialTheme.colorScheme.primary)
          Column {
            Text("WEBRTC INSTANT WALKIE-TALKIE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text(
              "$activeChannel • ${presence.size} on freq • $connectedPeerCount WebRTC peer(s)",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
        IconButton(onClick = onClose, modifier = Modifier.testTag("walkie_close_button")) {
          Icon(Icons.Default.Close, contentDescription = "Close Radio")
        }
      }

      LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        items(TACTICAL_CHANNELS) { (id, label) ->
          FilterChip(
            selected = activeChannel == id,
            onClick = {
              livePttHeld = false
              radarVm.clearMyWalkieWebRtcSignals()
              radarVm.switchWalkieChannel(id, myCallsign, true)
            },
            label = { Text(label) },
          )
        }
      }

      Spacer(Modifier.height(8.dp))

      // INSTANT WEBRTC RADIO STATUS & AUDIO CONTROLS
      TacticalCard(
        highlight = livePttHeld || handsFreeOpenMic || speaker?.active == true,
        modifier = Modifier.padding(horizontal = 14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.GraphicEq, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
              Text(
                text =
                  when {
                    livePttHeld -> "WEBRTC LIVE TX (INSTANT)"
                    handsFreeOpenMic -> "OPEN-MIC VOX LIVE"
                    speaker?.active == true -> "RX: ${speaker?.speakerCallsign} SPEAKING LIVE"
                    else -> "WEBRTC INSTANT STANDBY"
                  },
                style = MaterialTheme.typography.labelLarge,
                color = if (livePttHeld || handsFreeOpenMic) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
              )
            }
            Text(
              text = "$rtcStatus • Peers: ${presence.joinToString { it.callsign }.ifBlank { "Scanning..." }}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          item {
            FilterChip(
              selected = handsFreeOpenMic,
              onClick = {
                if (!hasMic) {
                  micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                  handsFreeOpenMic = !handsFreeOpenMic
                  radarVm.setLiveChannelSpeaker(myCallsign, handsFreeOpenMic, settings.globalVolume)
                }
              },
              leadingIcon = { Icon(Icons.Default.HeadsetMic, null, Modifier.size(16.dp)) },
              label = { Text(if (handsFreeOpenMic) "Open-Mic ON" else "Hands-Free Open-Mic") },
              modifier = Modifier.testTag("walkie_open_mic_chip"),
            )
          }
          item {
            FilterChip(
              selected = speakerphoneOn,
              onClick = { speakerphoneOn = !speakerphoneOn },
              leadingIcon = { Icon(Icons.Default.VolumeUp, null, Modifier.size(16.dp)) },
              label = { Text(if (speakerphoneOn) "Speakerphone" else "Earpiece / BT") },
            )
          }
          item {
            FilterChip(
              selected = speakerMuted,
              onClick = { speakerMuted = !speakerMuted },
              leadingIcon = {
                Icon(if (speakerMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, null, Modifier.size(16.dp))
              },
              label = { Text(if (speakerMuted) "RX Muted" else "RX Live") },
            )
          }
        }
      }

      Spacer(Modifier.height(10.dp))

      // INSTANT WEBRTC HOLD-TO-TALK + VOICE CLIP BACKUP BUTTON
      Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        if (!hasMic) {
          Button(onClick = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
            Icon(Icons.Default.Mic, null)
            Spacer(Modifier.width(8.dp))
            Text("Grant Microphone for WebRTC Instant Radio")
          }
        } else {
          Surface(
            shape = CircleShape,
            color =
              when {
                livePttHeld || handsFreeOpenMic -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
              },
            border = BorderStroke(4.dp, MaterialTheme.colorScheme.outline),
            modifier =
              Modifier
                .size(138.dp)
                .testTag("walkie_ptt_button")
                .pointerInput(activeChannel, myCallsign, settings.globalVolume) {
                  detectTapGestures(
                    onPress = {
                      livePttHeld = true
                      radarVm.setLiveChannelSpeaker(myCallsign, true, settings.globalVolume)
                      tryAwaitRelease()
                      livePttHeld = false
                      if (!handsFreeOpenMic) {
                        radarVm.setLiveChannelSpeaker(myCallsign, false, settings.globalVolume)
                      }
                    }
                  )
                },
          ) {
            Column(
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Icon(
                if (livePttHeld || handsFreeOpenMic) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = "Hold for Instant WebRTC Push to Talk",
                modifier = Modifier.size(38.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
              )
              Text(
                if (livePttHeld) "LIVE TX" else if (handsFreeOpenMic) "OPEN MIC" else "HOLD TO TALK",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimary,
              )
              Text(
                "WebRTC Zero-Delay",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
              )
            }
          }
        }
      }

      // Secondary option: send recorded voice clip to channel log for offline peers
      if (hasMic) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.Center,
        ) {
          FilterChip(
            selected = transmitting && !livePttHeld && !handsFreeOpenMic,
            onClick = {
              if (!transmitting) {
                radarVm.beginPtt(myCallsign, settings.globalVolume)
              } else {
                radarVm.endPtt(myCallsign, settings.globalVolume)
              }
            },
            label = {
              Text(
                if (transmitting && !livePttHeld && !handsFreeOpenMic) "Stop & Post Voice Clip Log"
                else "Record Async Voice Note for Offline Peers"
              )
            },
          )
        }
      }

      Text(
        "CHANNEL VOICE ARCHIVE (${logs.size})",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
      )

      LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (logs.isEmpty()) {
          item {
            TacticalCard {
              Text(
                "WebRTC Instant Voice Mesh is active on $activeChannel. Hold the button above or toggle Open-Mic to speak in real time with every pilot tuned to this channel.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
        items(logs, key = { it.id }) { log ->
          TacticalCard {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(log.senderCallsign, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("${"%.1f".format(log.durationMs / 1000.0)}s voice packet", style = MaterialTheme.typography.labelSmall)
              }
              Button(onClick = { radarVm.playWalkieClip(log, settings.globalVolume) }) {
                Icon(Icons.Default.PlayArrow, null)
                Text("Play")
              }
            }
          }
        }
      }
    }
  }
}

private fun buildWalkieWebRtcMeshHtml(myUid: String, myCallsign: String): String =
  """
<!doctype html>
<html>
<head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
<body style="margin:0;background:#000;">
<div id="audioPool"></div>
<script>
const MY_UID = ${JSONObject.quote(myUid)};
const MY_CALLSIGN = ${JSONObject.quote(myCallsign)};
const peers = {};
const pendingIce = {};
let localStream = null;
let micEnabled = false;
let speakerMuted = false;

function reportStatus(msg) {
  let active = 0;
  Object.values(peers).forEach(p => {
    if (p.connectionState === 'connected' || p.iceConnectionState === 'connected' || p.iceConnectionState === 'completed') active++;
  });
  if (window.WalkieRtcBridge) window.WalkieRtcBridge.onMeshStatus(msg, active);
}

async function ensureLocalAudio() {
  if (localStream) return localStream;
  try {
    localStream = await navigator.mediaDevices.getUserMedia({
      audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true },
      video: false
    });
    localStream.getAudioTracks().forEach(t => t.enabled = micEnabled);
    reportStatus('WebRTC Audio Ready • Hold PTT or Open-Mic');
    return localStream;
  } catch (e) {
    reportStatus('Mic Error: ' + e.message);
    return null;
  }
}

async function getOrCreatePeer(peerUid) {
  if (peers[peerUid]) return peers[peerUid];
  const stream = await ensureLocalAudio();
  const pc = new RTCPeerConnection({
    iceServers: [
      { urls: 'stun:stun.l.google.com:19302' },
      { urls: 'stun:stun1.l.google.com:19302' }
    ]
  });
  if (stream) {
    stream.getTracks().forEach(t => pc.addTrack(t, stream));
  }
  pc.onicecandidate = ev => {
    if (ev.candidate && window.WalkieRtcBridge) {
      window.WalkieRtcBridge.sendSignal(peerUid, 'ICE', JSON.stringify(ev.candidate));
    }
  };
  pc.ontrack = ev => {
    let el = document.getElementById('aud_' + peerUid);
    if (!el) {
      el = document.createElement('audio');
      el.id = 'aud_' + peerUid;
      el.autoplay = true;
      el.playsInline = true;
      document.getElementById('audioPool').appendChild(el);
    }
    el.muted = speakerMuted;
    if (ev.streams && ev.streams[0]) {
      el.srcObject = ev.streams[0];
      el.play().catch(() => {});
    }
    reportStatus('WebRTC Mesh Linked');
  };
  pc.onconnectionstatechange = () => {
    if (pc.connectionState === 'connected') reportStatus('WebRTC Instant Mesh Connected');
    else if (pc.connectionState === 'failed' || pc.connectionState === 'closed') {
      delete peers[peerUid];
      reportStatus('WebRTC Mesh Ready');
    }
  };
  peers[peerUid] = pc;
  return pc;
}

window.onWalkieSignal = async (fromUid, fromCallsign, type, payloadStr) => {
  if (!fromUid || fromUid === MY_UID) return;
  try {
    if (type === 'JOIN') {
      // Deterministic tie-breaker so only one peer initiates the offer
      if (MY_UID > fromUid) {
        const pc = await getOrCreatePeer(fromUid);
        const offer = await pc.createOffer();
        await pc.setLocalDescription(offer);
        window.WalkieRtcBridge.sendSignal(fromUid, 'OFFER', JSON.stringify(offer));
        reportStatus('Linking WebRTC with ' + fromCallsign + '...');
      }
    } else if (type === 'OFFER') {
      const pc = await getOrCreatePeer(fromUid);
      const data = JSON.parse(payloadStr);
      await pc.setRemoteDescription(new RTCSessionDescription(data));
      const answer = await pc.createAnswer();
      await pc.setLocalDescription(answer);
      window.WalkieRtcBridge.sendSignal(fromUid, 'ANSWER', JSON.stringify(answer));
      const q = pendingIce[fromUid] || [];
      while (q.length) await pc.addIceCandidate(new RTCIceCandidate(q.shift()));
      reportStatus('WebRTC Linked: ' + fromCallsign);
    } else if (type === 'ANSWER') {
      const pc = peers[fromUid];
      if (!pc) return;
      const data = JSON.parse(payloadStr);
      await pc.setRemoteDescription(new RTCSessionDescription(data));
      const q = pendingIce[fromUid] || [];
      while (q.length) await pc.addIceCandidate(new RTCIceCandidate(q.shift()));
      reportStatus('WebRTC Linked: ' + fromCallsign);
    } else if (type === 'ICE') {
      const data = JSON.parse(payloadStr);
      const pc = peers[fromUid];
      if (pc && pc.remoteDescription) {
        await pc.addIceCandidate(new RTCIceCandidate(data));
      } else {
        pendingIce[fromUid] = pendingIce[fromUid] || [];
        pendingIce[fromUid].push(data);
      }
    } else if (type === 'LEAVE') {
      if (peers[fromUid]) {
        peers[fromUid].close();
        delete peers[fromUid];
      }
      const el = document.getElementById('aud_' + fromUid);
      if (el) el.remove();
      reportStatus('WebRTC Standby');
    }
  } catch (err) {
    reportStatus('WebRTC Signaling: ' + err.message);
  }
};

window.setWalkieMicEnabled = async (enabled) => {
  micEnabled = !!enabled;
  const s = await ensureLocalAudio();
  if (s) s.getAudioTracks().forEach(t => t.enabled = micEnabled);
};

window.setWalkieSpeakerMuted = (muted) => {
  speakerMuted = !!muted;
  document.querySelectorAll('#audioPool audio').forEach(a => a.muted = speakerMuted);
};

ensureLocalAudio();
</script>
</body>
</html>
  """.trimIndent()
