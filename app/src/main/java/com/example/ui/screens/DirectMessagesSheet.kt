package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DmMessage
import com.example.data.PilotAuthenticityGuard
import com.example.data.PublicPilotProfile
import com.example.data.RiderMatchEngine
import com.example.hardware.VoiceAudioManager
import com.example.ui.components.CloudImage
import com.example.ui.components.PilotAvatarThumb
import com.example.ui.components.TacticalCard
import com.example.ui.components.decodeSampledBitmap
import com.example.viewmodel.CopilotSocialViewModel
import com.example.viewmodel.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectMessagesSheet(
  sessionVm: SessionViewModel,
  socialVm: CopilotSocialViewModel,
  initialPeer: PublicPilotProfile? = null,
  telemetryLat: Double = 0.0,
  telemetryLng: Double = 0.0,
  activeVehicleSummary: String = "",
  liveTelemetrySummary: String = "",
  onDismiss: () -> Unit,
) {
  val ctx = LocalContext.current
  val haptics = LocalHapticFeedback.current
  val scope = rememberCoroutineScope()
  val activeUser by sessionVm.activeUser.collectAsStateWithLifecycle()
  val profile by sessionVm.profile.collectAsStateWithLifecycle()
  val settings by sessionVm.settings.collectAsStateWithLifecycle()
  val pilots by sessionVm.publicPilots.collectAsStateWithLifecycle()

  val threads by socialVm.dmThreads.collectAsStateWithLifecycle()
  val messages by socialVm.dmMessages.collectAsStateWithLifecycle()
  val mutedPeerIds by socialVm.mutedPeerIds.collectAsStateWithLifecycle()
  val pinnedThreadIds by socialVm.pinnedDmThreadIds.collectAsStateWithLifecycle()
  val callHistory by socialVm.callHistory.collectAsStateWithLifecycle()

  val myUid = activeUser?.uid.orEmpty()
  val cloudSyncReady = activeUser != null
  val myCallsign = profile?.callsign?.ifBlank { "PILOT" } ?: "PILOT"

  var inboxTab by remember { mutableIntStateOf(0) } // 0=Chats, 1=Pins, 2=Riders, 3=Calls
  var inboxQuery by remember { mutableStateOf("") }
  var riderFilterCat by remember { mutableStateOf("All") } // All | Best Match | Verified
  var callFilterCat by remember { mutableStateOf("All") } // All | Audio | Video | Incoming | Outgoing
  var selectedPeer by remember(initialPeer) { mutableStateOf(initialPeer) }
  var activeThreadId by remember { mutableStateOf("") }
  var draftText by remember { mutableStateOf("") }
  var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var selfDestructSec by remember { mutableLongStateOf(0L) }
  var recordingVoice by remember { mutableStateOf(false) }
  var translations by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
  var showThreadSearch by remember { mutableStateOf(false) }
  var threadSearchQuery by remember { mutableStateOf("") }
  var replyTarget by remember { mutableStateOf<DmMessage?>(null) }
  var aiSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
  var statusToast by remember { mutableStateOf("") }
  var activeToolCategory by remember { mutableStateOf("GPS & Rig") } // GPS & Rig | Safety & Burn | AI Replies | Quick Chips
  var quickReplyCategory by remember { mutableStateOf("Convoy") } // Convoy | Charging | Hazards
  var lightboxImageUrl by remember { mutableStateOf<String?>(null) }

  val authenticThreads =
    remember(threads, myUid, mutedPeerIds) {
      threads.filter { th ->
        val otherIdx = th.participantUids.indexOfFirst { it != myUid }
        if (otherIdx < 0) return@filter true
        val otherUid = th.participantUids.getOrElse(otherIdx) { "" }
        val otherCs = th.participantCallsigns.getOrElse(otherIdx) { "" }
        !mutedPeerIds.contains(otherUid) &&
          PilotAuthenticityGuard.isAuthenticCallsignAndUid(
            userId = otherUid,
            callsign = otherCs,
            currentUid = myUid.takeIf { it.isNotBlank() },
          )
      }
    }

  val authenticRiders =
    remember(pilots, myUid, mutedPeerIds) {
      pilots
        .filter { p ->
          p.userId != myUid &&
            !mutedPeerIds.contains(p.userId) &&
            PilotAuthenticityGuard.isDisplayablePilotProfile(
              p = p,
              currentUid = myUid.takeIf { it.isNotBlank() },
              requireFirebaseUid = false,
            )
        }
        .distinctBy { it.userId }
        .distinctBy { it.callsign.trim().uppercase() }
    }

  val authenticCalls =
    remember(callHistory, myUid, mutedPeerIds) {
      callHistory.filter { c ->
        val peerUid = if (c.callerUid == myUid) c.calleeUid else c.callerUid
        val peerCs = if (c.callerUid == myUid) c.calleeCallsign else c.callerCallsign
        !mutedPeerIds.contains(peerUid) &&
          (peerUid.isBlank() ||
            PilotAuthenticityGuard.isAuthenticCallsignAndUid(
              userId = peerUid,
              callsign = peerCs,
              currentUid = myUid.takeIf { it.isNotBlank() },
            ))
      }
    }

  val photoPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) attachedBitmap = decodeSampledBitmap(ctx, uri)
    }

  val speechLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
      val spoken = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
      if (!spoken.isNullOrBlank()) {
        draftText = if (draftText.isBlank()) spoken else "$draftText $spoken"
      }
    }

  val audioPermLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        recordingVoice = socialVm.audio.startPttCapture(scope, 10_000L) { b64, _ ->
          recordingVoice = false
          val peer = selectedPeer ?: return@startPttCapture
          socialVm.sendDm(myCallsign, peer.userId, peer.callsign, "", null, b64, selfDestructSec) { tid ->
            activeThreadId = tid
          }
        }
      }
    }

  LaunchedEffect(selectedPeer, myUid) {
    val peer = selectedPeer
    if (peer != null && myUid.isNotBlank() && cloudSyncReady) {
      val sorted = listOf(myUid, peer.userId).sorted()
      val tid = "${sorted[0]}_${sorted[1]}"
      activeThreadId = tid
      replyTarget = null
      aiSuggestions = emptyList()
      socialVm.openDmThread(tid, myUid)
    }
  }

  val quickRepliesByCategory =
    remember(settings.quickReplies) {
      val custom = settings.quickReplies.split(",").map { it.trim() }.filter { it.isNotBlank() }
      mapOf(
        "Convoy" to (custom + listOf("Rolling out in 5 min", "Copy — standing by", "Meet at staging lot", "Heading to meetup spot")).distinct(),
        "Charging" to listOf("Stopped to fast-charge", "Battery < 25% — slowing pace", "240V / 120V charger open here", "Ready at 100% SOC"),
        "Hazards" to listOf("Road clear ahead", "Gravel / debris ahead!", "High creek crossing — use caution", "Sending live GPS pin now"),
      )
    }

  val visibleMessages =
    remember(messages, threadSearchQuery) {
      if (threadSearchQuery.isBlank()) messages
      else messages.filter { it.text.contains(threadSearchQuery, true) || it.senderCallsign.contains(threadSearchQuery, true) }
    }

  fun playCue(cue: VoiceAudioManager.TouchCue) {
    sessionVm.voiceAudio.playTouchSound(
      enabled = settings.touchSoundsEnabled,
      pack = settings.touchSoundPack,
      cue = cue,
      volumePct = settings.soundVolumePct,
    )
  }

  val focusManager = LocalFocusManager.current

  fun handleSafeBack() {
    runCatching { focusManager.clearFocus(force = true) }
    runCatching { playCue(VoiceAudioManager.TouchCue.BACK_NAV) }
    if (lightboxImageUrl != null) {
      lightboxImageUrl = null
    } else if (selectedPeer != null) {
      selectedPeer = null
      replyTarget = null
      attachedBitmap = null
      showThreadSearch = false
      threadSearchQuery = ""
    } else {
      onDismiss()
    }
  }

  Dialog(
    onDismissRequest = { handleSafeBack() },
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnBackPress = false,
      dismissOnClickOutside = false,
      decorFitsSystemWindows = false,
    ),
  ) {
    BackHandler(enabled = true) {
      handleSafeBack()
    }
    Surface(
      color = MaterialTheme.colorScheme.background,
      modifier = Modifier.fillMaxSize(),
    ) {
      Column(
        Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
          .imePadding()
          .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        // TOP HEADER WITH ALWAYS-VISIBLE 48DP BACK BUTTON
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            IconButton(
              onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                handleSafeBack()
              },
              modifier = Modifier.size(48.dp).testTag("dm_back_button"),
            ) {
              Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = if (selectedPeer != null) "Back to Inbox" else "Back",
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                selectedPeer?.let { "@${it.callsign}" } ?: "PORCH DMs & LIVE CALLS",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Black,
              color = MaterialTheme.colorScheme.primary,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              selectedPeer?.let { p ->
                val spec = RiderMatchEngine.decodeRigMatchSpec(p.vehicleSummary)
                "${p.rankBadge.ifBlank { "Active Pilot" }} • ${spec.displayRigName}"
              } ?: "Zero Bots • ${authenticRiders.size} real riders • ${authenticThreads.size} threads",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          selectedPeer?.let { peer ->
            if (activeThreadId.isNotBlank()) {
              val isPinned = pinnedThreadIds.contains(activeThreadId)
              IconButton(onClick = {
                playCue(VoiceAudioManager.TouchCue.TOGGLE_ON)
                socialVm.togglePinDmThread(myUid, activeThreadId)
              }) {
                Icon(
                  Icons.Default.PushPin,
                  contentDescription = if (isPinned) "Unpin Thread" else "Pin Thread",
                  tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            IconButton(onClick = { showThreadSearch = !showThreadSearch }) {
              Icon(Icons.Default.Search, contentDescription = "Search Thread")
            }
            if (cloudSyncReady && peer.userId != myUid) {
              IconButton(
                onClick = {
                  socialVm.startOutgoingCall(myUid, myCallsign, peer.userId, peer.callsign, "AUDIO")
                },
                modifier = Modifier.testTag("dm_audio_call_button"),
              ) {
                Icon(Icons.Default.Call, contentDescription = "Audio Call Pilot", tint = MaterialTheme.colorScheme.primary)
              }
              IconButton(
                onClick = {
                  socialVm.startOutgoingCall(myUid, myCallsign, peer.userId, peer.callsign, "VIDEO")
                },
                modifier = Modifier.testTag("dm_video_call_button"),
              ) {
                Icon(Icons.Default.Videocam, contentDescription = "Video Call Pilot", tint = MaterialTheme.colorScheme.secondary)
              }
              val isMuted = mutedPeerIds.contains(peer.userId)
              IconButton(onClick = {
                socialVm.toggleMutePeer(myUid, peer.userId)
                selectedPeer = null
                statusToast = if (isMuted) "Unblocked @${peer.callsign}" else "Blocked & hidden @${peer.callsign}"
              }) {
                Icon(
                  Icons.Default.Block,
                  contentDescription = if (isMuted) "Unblock Peer" else "Block & Hide Peer",
                  tint = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
          IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close DMs") }
        }
      }

      if (statusToast.isNotBlank()) {
        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            statusToast,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          )
        }
      }

      if (!cloudSyncReady) {
        TacticalCard(highlight = true) {
          Text("Sign in with Email or Google in Settings to enable live encrypted DMs, voice notes, and WebRTC calls.", style = MaterialTheme.typography.bodySmall)
        }
      }

      if (selectedPeer == null) {
        val dmTabs =
          listOf(
            Triple(0, "Chats", authenticThreads.size),
            Triple(1, "Pins", authenticThreads.count { pinnedThreadIds.contains(it.id) }),
            Triple(2, "Riders", authenticRiders.size),
            Triple(3, "Calls", authenticCalls.size),
          )
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          dmTabs.forEach { (idx, title, count) ->
            val selected = inboxTab == idx
            Surface(
              onClick = {
                playCue(VoiceAudioManager.TouchCue.TAB_SWITCH)
                inboxTab = idx
              },
              color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
              border =
                BorderStroke(
                  width = if (selected) 1.5.dp else 1.dp,
                  color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.30f),
                ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("dm_inbox_tab_$idx"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = if (count > 0) "$title $count" else title,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                  color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }

        if (inboxTab != 3) {
          OutlinedTextField(
            value = inboxQuery,
            onValueChange = { inboxQuery = it },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
              if (inboxQuery.isNotBlank()) {
                IconButton(onClick = { inboxQuery = "" }) { Icon(Icons.Default.Close, "Clear search") }
              }
            },
            placeholder = { Text("Search real callsigns, rigs, or messages...") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("dm_inbox_search"),
          )
        }

        when (inboxTab) {
          3 -> {
            // CATEGORIZED CALL HISTORY SUMMARY & FILTERS
            val videoCount = authenticCalls.count { it.callType == "VIDEO" }
            val audioCount = authenticCalls.count { it.callType != "VIDEO" }
            val filteredCalls =
              remember(authenticCalls, callFilterCat, myUid) {
                authenticCalls.filter { c ->
                  val outgoing = c.callerUid == myUid
                  when (callFilterCat) {
                    "Audio" -> c.callType != "VIDEO"
                    "Video" -> c.callType == "VIDEO"
                    "Outgoing" -> outgoing
                    "Incoming" -> !outgoing
                    else -> true
                  }
                }
              }
            TacticalCard {
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("LIVE CALL DISPATCH LOG", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                Text("${authenticCalls.size} Total • $audioCount Audio • $videoCount Video", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
              }
              Spacer(Modifier.height(6.dp))
              LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf("All", "Audio", "Video", "Incoming", "Outgoing")) { cat ->
                  FilterChip(selected = callFilterCat == cat, onClick = { callFilterCat = cat }, label = { Text(cat) })
                }
              }
            }
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              if (filteredCalls.isEmpty()) {
                item {
                  TacticalCard {
                    Text("No live audio or video calls match \"$callFilterCat\" yet. Tap Call or Video next to any real rider to start an encrypted WebRTC call.", style = MaterialTheme.typography.bodySmall)
                  }
                }
              }
              itemsIndexed(filteredCalls, key = { idx, c -> "call_${idx}_${c.id}" }) { _, call ->
                val outgoing = call.callerUid == myUid
                val peerUid = if (outgoing) call.calleeUid else call.callerUid
                val peerName = (if (outgoing) call.calleeCallsign else call.callerCallsign).ifBlank { "Pilot" }
                val isVideoCall = call.callType == "VIDEO"
                TacticalCard {
                  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                      Surface(
                        color = (if (isVideoCall) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary).copy(alpha = 0.16f),
                        shape = CircleShape,
                        modifier = Modifier.size(38.dp),
                      ) {
                        Box(contentAlignment = Alignment.Center) {
                          Icon(
                            if (isVideoCall) Icons.Default.Videocam else Icons.Default.Call,
                            contentDescription = null,
                            tint = if (isVideoCall) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                          )
                        }
                      }
                      Column(Modifier.weight(1f)) {
                        Text(peerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                          "${if (outgoing) "Outgoing" else "Incoming"} ${call.callType} • ${call.status} • ${formatRelativeTimestamp(call.updatedAt?.seconds ?: call.createdAt?.seconds)}",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                    }
                    if (cloudSyncReady && peerUid.isNotBlank() && peerUid != myUid) {
                      Row {
                        IconButton(onClick = {
                          selectedPeer = authenticRiders.firstOrNull { it.userId == peerUid } ?: PublicPilotProfile(userId = peerUid, callsign = peerName)
                        }) {
                          Icon(Icons.Default.Send, contentDescription = "Message $peerName", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { socialVm.startOutgoingCall(myUid, myCallsign, peerUid, peerName, "AUDIO") }) {
                          Icon(Icons.Default.Call, contentDescription = "Redial Audio", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { socialVm.startOutgoingCall(myUid, myCallsign, peerUid, peerName, "VIDEO") }) {
                          Icon(Icons.Default.Videocam, contentDescription = "Redial Video", tint = MaterialTheme.colorScheme.secondary)
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          2 -> {
            val filteredRiders =
              remember(authenticRiders, inboxQuery, riderFilterCat, settings) {
                authenticRiders
                  .map { p ->
                    val spec = RiderMatchEngine.decodeRigMatchSpec(p.vehicleSummary)
                    val score = RiderMatchEngine.scorePilotMatch(settings, p)
                    Triple(p, spec, score)
                  }
                  .filter { (p, spec, _) ->
                    (riderFilterCat != "Verified" || p.verified) &&
                      (inboxQuery.isBlank() ||
                        p.callsign.contains(inboxQuery, true) ||
                        spec.displayRigName.contains(inboxQuery, true) ||
                        p.rankBadge.contains(inboxQuery, true))
                  }
                  .let { list ->
                    if (riderFilterCat == "Best Match") list.sortedByDescending { it.third.scorePct }
                    else list.sortedByDescending { it.first.xpPoints }
                  }
              }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                items(listOf("All", "Best Match", "Verified")) { cat ->
                  FilterChip(selected = riderFilterCat == cat, onClick = { riderFilterCat = cat }, label = { Text(cat) })
                }
              }
              Surface(
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(Icons.Default.Shield, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(13.dp))
                  Text("0 Bots", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
              }
            }
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              if (filteredRiders.isEmpty()) {
                item {
                  TacticalCard {
                    Text(
                      if (inboxQuery.isNotBlank()) "No real riders match \"$inboxQuery\"."
                      else "No other verified or ride-active real riders found yet. Zero crawler, pre-launch test, or 0-ride accounts are shown.",
                      style = MaterialTheme.typography.bodySmall,
                    )
                  }
                }
              }
              itemsIndexed(filteredRiders, key = { idx, r -> "pilot_${idx}_${r.first.userId}" }) { _, (p, spec, score) ->
                val isOnlineNow = (System.currentTimeMillis() / 1000L - (p.updatedAt?.seconds ?: p.createdAt?.seconds ?: 0L)) in 0..900
                TacticalCard(onClick = { selectedPeer = p }) {
                  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                      PilotAvatarThumb(avatarSource = p.avatarBase64, callsign = p.callsign, size = 40.dp)
                      Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                          Text(
                            p.callsign,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                          )
                          if (isOnlineNow) {
                            Surface(
                              color = Color(0xFF10B981).copy(alpha = 0.2f),
                              shape = RoundedCornerShape(6.dp),
                            ) {
                              Text(
                                "ONLINE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                              )
                            }
                          }
                          if (p.verified) {
                            Icon(Icons.Default.Verified, "Verified", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                          }
                          Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp),
                          ) {
                            Text(
                              "${score.scorePct}% Match",
                              style = MaterialTheme.typography.labelSmall,
                              fontWeight = FontWeight.Bold,
                              color = MaterialTheme.colorScheme.primary,
                              modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            )
                          }
                        }
                        Text(
                          "${p.rankBadge} • ${spec.displayRigName} • ${p.ridesCount} rides (${com.example.ui.components.formatDistLabel(p.totalKm, settings.distanceUnit, decimals = 1)})",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                      }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      if (cloudSyncReady) {
                        IconButton(onClick = { socialVm.startOutgoingCall(myUid, myCallsign, p.userId, p.callsign, "AUDIO") }) {
                          Icon(Icons.Default.Call, contentDescription = "Audio Call ${p.callsign}", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { socialVm.startOutgoingCall(myUid, myCallsign, p.userId, p.callsign, "VIDEO") }) {
                          Icon(Icons.Default.Videocam, contentDescription = "Video Call ${p.callsign}", tint = MaterialTheme.colorScheme.secondary)
                        }
                      }
                      IconButton(onClick = {
                        socialVm.toggleMutePeer(myUid, p.userId)
                        statusToast = "Hid & blocked @${p.callsign} from DMs and Radar."
                      }) {
                        Icon(Icons.Default.Block, contentDescription = "Hide & Block ${p.callsign}", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                      }
                    }
                  }
                }
              }
            }
          }

          else -> {
            val orderedThreads =
              authenticThreads
                .filter { th ->
                  (inboxTab != 1 || pinnedThreadIds.contains(th.id)) &&
                    (inboxQuery.isBlank() ||
                      th.lastMessage.contains(inboxQuery, true) ||
                      th.participantCallsigns.any { it.contains(inboxQuery, true) })
                }
                .sortedByDescending { if (pinnedThreadIds.contains(it.id)) 1 else 0 }

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              if (orderedThreads.isEmpty()) {
                item {
                  TacticalCard {
                    Text(
                      if (inboxTab == 1) "No pinned threads yet. Open any chat and tap the pin icon to keep it here."
                      else "No live DM threads yet. Switch to Riders tab to message or call any verified active rider.",
                      style = MaterialTheme.typography.bodySmall,
                    )
                  }
                }
              }
              itemsIndexed(orderedThreads, key = { idx, th -> "thread_${idx}_${th.id}" }) { _, th ->
                val otherIdx = th.participantUids.indexOfFirst { it != myUid }.coerceAtLeast(0)
                val otherUid = th.participantUids.getOrElse(otherIdx) { "" }
                val otherCs = th.participantCallsigns.getOrElse(otherIdx) { "Pilot" }
                val peerProfile = authenticRiders.firstOrNull { it.userId == otherUid }
                val isPinned = pinnedThreadIds.contains(th.id)
                TacticalCard(
                  highlight = isPinned,
                  onClick = {
                    selectedPeer = peerProfile ?: PublicPilotProfile(userId = otherUid, callsign = otherCs)
                  },
                ) {
                  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                      PilotAvatarThumb(avatarSource = peerProfile?.avatarBase64.orEmpty(), callsign = otherCs, size = 40.dp)
                      Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                          Text(
                            otherCs,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                          )
                          if (isPinned) {
                            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), shape = RoundedCornerShape(6.dp)) {
                              Text("PINNED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                            }
                          }
                          if (th.selfDestructSec > 0L) {
                            Surface(color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f), shape = RoundedCornerShape(6.dp)) {
                              Text("BURN ${th.selfDestructSec}s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                            }
                          }
                        }
                        Text(th.lastMessage.ifBlank { "Encrypted channel open" }, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(formatRelativeTimestamp(th.updatedAt?.seconds), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      if (cloudSyncReady && otherUid.isNotBlank()) {
                        IconButton(onClick = { socialVm.startOutgoingCall(myUid, myCallsign, otherUid, otherCs, "AUDIO") }) {
                          Icon(Icons.Default.Call, contentDescription = "Call $otherCs", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                      }
                      IconButton(onClick = { socialVm.togglePinDmThread(myUid, th.id) }) {
                        Icon(
                          Icons.Default.PushPin,
                          contentDescription = if (isPinned) "Unpin" else "Pin",
                          tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      } else {
        if (showThreadSearch) {
          OutlinedTextField(
            value = threadSearchQuery,
            onValueChange = { threadSearchQuery = it },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            placeholder = { Text("Search inside this conversation...") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
          )
        }

        // CATEGORIZED DM TOOLS BAR (GPS & RIG | SAFETY & BURN | AI REPLIES | QUICK CHIPS)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(listOf("GPS & Rig", "Safety & Burn", "AI Replies", "Quick Chips")) { cat ->
            FilterChip(
              selected = activeToolCategory == cat,
              onClick = { activeToolCategory = cat },
              label = { Text(cat, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
            )
          }
        }

        when (activeToolCategory) {
          "GPS & Rig" -> {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              if (cloudSyncReady && telemetryLat != 0.0) {
                item {
                  AssistChip(
                    onClick = {
                      val peer = selectedPeer ?: return@AssistChip
                      val pinMsg = "LIVE GPS PIN: ${"%.5f".format(telemetryLat)}, ${"%.5f".format(telemetryLng)} (https://maps.google.com/?q=$telemetryLat,$telemetryLng)"
                      socialVm.sendDm(myCallsign, peer.userId, peer.callsign, pinMsg, null, "", selfDestructSec) { tid -> activeThreadId = tid }
                    },
                    leadingIcon = { Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp)) },
                    label = { Text("Share GPS Pin") },
                  )
                }
              }
              if (cloudSyncReady && activeVehicleSummary.isNotBlank()) {
                item {
                  AssistChip(
                    onClick = {
                      val peer = selectedPeer ?: return@AssistChip
                      socialVm.sendDm(myCallsign, peer.userId, peer.callsign, "MY PEV RIG: $activeVehicleSummary", null, "", selfDestructSec) { tid -> activeThreadId = tid }
                    },
                    leadingIcon = { Icon(Icons.Default.DirectionsBike, null, Modifier.size(16.dp)) },
                    label = { Text("Send PEV Card") },
                  )
                }
              }
              if (cloudSyncReady && liveTelemetrySummary.isNotBlank()) {
                item {
                  AssistChip(
                    onClick = {
                      val peer = selectedPeer ?: return@AssistChip
                      socialVm.sendDm(myCallsign, peer.userId, peer.callsign, "LIVE TELEMETRY: $liveTelemetrySummary", null, "", selfDestructSec) { tid -> activeThreadId = tid }
                    },
                    leadingIcon = { Icon(Icons.Default.Speed, null, Modifier.size(16.dp)) },
                    label = { Text("Send Live Telemetry") },
                  )
                }
              }
            }
          }
          "Safety & Burn" -> {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              val timers = listOf(0L to "Persist", 60L to "Burn 1m", 300L to "Burn 5m", 3600L to "Burn 1h", 86400L to "Burn 24h")
              items(timers.size) { idx ->
                val (sec, lbl) = timers[idx]
                FilterChip(
                  selected = selfDestructSec == sec,
                  onClick = { selfDestructSec = sec },
                  leadingIcon = if (sec > 0L) { { Icon(Icons.Default.Security, null, Modifier.size(14.dp)) } } else null,
                  label = { Text(lbl) },
                )
              }
              if (cloudSyncReady) {
                item {
                  AssistChip(
                    onClick = {
                      val peer = selectedPeer ?: return@AssistChip
                      val locText = if (telemetryLat != 0.0) "${"%.5f".format(telemetryLat)}, ${"%.5f".format(telemetryLng)}" else "current sector"
                      socialVm.sendDm(myCallsign, peer.userId, peer.callsign, "STEALTH SOS PING from $myCallsign at $locText", null, "", selfDestructSec) { tid -> activeThreadId = tid }
                    },
                    leadingIcon = { Icon(Icons.Default.Sos, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error) },
                    label = { Text("SOS Ping") },
                  )
                }
              }
            }
          }
          "AI Replies" -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                  onClick = {
                    val lastIncoming = messages.lastOrNull { it.senderId != myUid }?.text ?: messages.lastOrNull()?.text.orEmpty()
                    socialVm.suggestDmReplies(lastIncoming, settings.aiModelId) { aiSuggestions = it }
                  },
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                ) {
                  Icon(Icons.Default.AutoAwesome, null, Modifier.size(15.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Generate AI Replies", style = MaterialTheme.typography.labelSmall)
                }
              }
              if (aiSuggestions.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  items(aiSuggestions) { sug ->
                    FilterChip(selected = false, onClick = { draftText = sug }, label = { Text("AI: $sug") })
                  }
                }
              }
            }
          }
          else -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf("Convoy", "Charging", "Hazards")) { cat ->
                  FilterChip(selected = quickReplyCategory == cat, onClick = { quickReplyCategory = cat }, label = { Text(cat, style = MaterialTheme.typography.labelSmall) })
                }
              }
              LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(quickRepliesByCategory[quickReplyCategory].orEmpty()) { qr ->
                  AssistChip(
                    onClick = {
                      haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                      draftText = qr
                    },
                    label = { Text(qr) },
                  )
                }
              }
            }
          }
        }

        // UPGRADED MESSENGER CHAT BUBBLES
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          if (visibleMessages.isEmpty()) {
            item {
              TacticalCard {
                Text("Encrypted live channel ready. Send a text, voice note, photo, GPS pin, or PEV rig card.", style = MaterialTheme.typography.bodySmall)
              }
            }
          }
          itemsIndexed(visibleMessages, key = { idx, msg -> "msg_${idx}_${msg.id}" }) { _, msg ->
            val isMine = msg.senderId == myUid
            val bubbleShape =
              if (isMine) RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
              else RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
            val bubbleColor =
              if (isMine) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
              else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
            val isGpsPin = msg.text.startsWith("LIVE GPS PIN:")
            val isSos = msg.text.startsWith("STEALTH SOS PING")
            val isRigCard = msg.text.startsWith("MY PEV RIG:") || msg.text.startsWith("LIVE TELEMETRY:")

            Box(
              modifier = Modifier.fillMaxWidth(),
              contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
              Surface(
                color = if (isSos) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.88f) else bubbleColor,
                shape = bubbleShape,
                border =
                  BorderStroke(
                    width = 1.dp,
                    color =
                      when {
                        isSos -> MaterialTheme.colorScheme.error.copy(alpha = 0.65f)
                        isMine -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)
                      },
                  ),
                modifier = Modifier.widthIn(min = 140.dp, max = 320.dp),
              ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      buildString {
                        append(if (isMine) "You (${msg.senderCallsign})" else msg.senderCallsign)
                        append(" • ${formatRelativeTimestamp(msg.createdAt?.seconds)}")
                        if (msg.selfDestructSec > 0L) append(" • Burn ${msg.selfDestructSec}s")
                      },
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = if (isSos) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                      modifier = Modifier.weight(1f),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      IconButton(
                        onClick = {
                          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                          replyTarget = msg
                          statusToast = "Replying to ${msg.senderCallsign}"
                        },
                        modifier = Modifier.size(32.dp),
                      ) { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "Reply Quote", modifier = Modifier.size(16.dp)) }
                      if (msg.text.isNotBlank()) {
                        IconButton(
                          onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("DM", msg.text))
                            statusToast = "Copied message."
                          },
                          modifier = Modifier.size(32.dp),
                        ) { Icon(Icons.Default.ContentCopy, contentDescription = "Copy Message", modifier = Modifier.size(15.dp)) }
                        IconButton(
                          onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            statusToast = "Translating message..."
                            socialVm.translateText(msg.text, "concise English summary", settings.aiModelId) { tr ->
                              translations = translations + (msg.id to tr)
                              statusToast = "Translation ready."
                            }
                          },
                          modifier = Modifier.size(32.dp),
                        ) { Icon(Icons.Default.Translate, contentDescription = "Translate DM", modifier = Modifier.size(16.dp)) }
                      }
                      if (isMine && activeThreadId.isNotBlank()) {
                        IconButton(
                          onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            socialVm.deleteDmMessage(activeThreadId, msg.id)
                            statusToast = "Deleted message."
                          },
                          modifier = Modifier.size(32.dp),
                        ) { Icon(Icons.Default.Delete, contentDescription = "Delete Message", modifier = Modifier.size(16.dp)) }
                      }
                    }
                  }

                  if (msg.imageUrl.isNotBlank()) {
                    CloudImage(
                      source = msg.imageUrl,
                      contentDescription = "DM Attachment",
                      contentScale = ContentScale.Crop,
                      modifier =
                        Modifier
                          .fillMaxWidth()
                          .height(165.dp)
                          .clip(RoundedCornerShape(12.dp))
                          .clickable { lightboxImageUrl = msg.imageUrl },
                    )
                  }

                  if (msg.voiceBase64.isNotBlank()) {
                    Surface(
                      onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        socialVm.audio.playBase64Pcm(scope, msg.voiceBase64)
                        statusToast = "Playing voice transmission..."
                      },
                      color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                      shape = RoundedCornerShape(12.dp),
                      border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                      modifier = Modifier.fillMaxWidth(),
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                      ) {
                        Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Icon(Icons.Default.GraphicEq, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                        Text("Play Encrypted Voice Note", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                      }
                    }
                  }

                  if (msg.text.isNotBlank()) {
                    val lines = msg.text.lines()
                    val quotedLine = lines.firstOrNull()?.takeIf { it.startsWith("> ") }
                    val mainBody = if (quotedLine != null) lines.drop(1).joinToString("\n") else msg.text

                    if (quotedLine != null) {
                      Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth(),
                      ) {
                        Text(
                          quotedLine.removePrefix("> "),
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                      }
                    }

                    if (isGpsPin) {
                      val mapsUrl = Regex("https://maps\\.google\\.com/\\?q=[-0-9.,]+").find(mainBody)?.value
                      Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth(),
                      ) {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                            Text("LIVE GPS COORDINATE PIN", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                          }
                          Text(mainBody, style = MaterialTheme.typography.bodySmall)
                          if (!mapsUrl.isNullOrBlank()) {
                            OutlinedButton(
                              onClick = { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl))) } },
                              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                              Icon(Icons.Default.LocationOn, null, Modifier.size(14.dp))
                              Spacer(Modifier.width(4.dp))
                              Text("Open in Maps", style = MaterialTheme.typography.labelSmall)
                            }
                          }
                        }
                      }
                    } else if (isRigCard) {
                      Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth(),
                      ) {
                        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                          Icon(Icons.Default.DirectionsBike, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                          Text(mainBody, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                      }
                    } else if (mainBody.isNotBlank()) {
                      Text(mainBody, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                  }

                  translations[msg.id]?.let { tr ->
                    Surface(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp)) {
                      Text(
                        "AI Summary: $tr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      )
                    }
                  }
                }
              }
            }
          }
        }

        // REPLY QUOTE PREVIEW BAR
        replyTarget?.let { rt ->
          TacticalCard(highlight = true) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text(
                "Replying to ${rt.senderCallsign}: ${rt.text.take(60)}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
              )
              IconButton(onClick = { replyTarget = null }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Close, "Cancel Reply", Modifier.size(20.dp))
              }
            }
          }
        }

        // STAGED PHOTO PREVIEW
        attachedBitmap?.let { bmp ->
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Attached photo preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)),
              )
              Text(
                "Photo attached — add a message or tap Send",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
              )
              IconButton(
                onClick = {
                  attachedBitmap = null
                  statusToast = "Removed attached photo."
                },
                modifier = Modifier.size(48.dp),
              ) {
                Icon(Icons.Default.Close, contentDescription = "Remove Photo", modifier = Modifier.size(20.dp))
              }
            }
          }
        }

        // HIGH-VISIBILITY SPACIOUS 2-ROW DM CHAT COMPOSER (ALWAYS CRISP & CLEAR OVER KEYBOARD)
        Surface(
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          shape = RoundedCornerShape(18.dp),
          border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
          shadowElevation = 6.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedTextField(
              value = draftText,
              onValueChange = { draftText = it },
              label = { Text("Message @${selectedPeer?.callsign ?: "rider"}", maxLines = 1, softWrap = false) },
              placeholder = {
                Text(
                  if (attachedBitmap != null) "Photo ready — type a caption..."
                  else "Type your live DM here...",
                )
              },
              minLines = 2,
              maxLines = 5,
              textStyle = MaterialTheme.typography.bodyLarge,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().testTag("dm_message_input"),
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    playCue(VoiceAudioManager.TouchCue.BUTTON_TAP)
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                  },
                  modifier = Modifier.size(48.dp).testTag("dm_attach_photo_button"),
                ) {
                  Icon(Icons.Default.PhotoLibrary, contentDescription = "Attach Photo", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    playCue(VoiceAudioManager.TouchCue.BUTTON_TAP)
                    runCatching {
                      val intent =
                        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                          putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                          putExtra(RecognizerIntent.EXTRA_PROMPT, "Dictate DM message")
                        }
                      speechLauncher.launch(intent)
                    }.onFailure { statusToast = "Voice dictation unavailable on this device." }
                  },
                  modifier = Modifier.size(48.dp).testTag("dm_dictate_button"),
                ) {
                  Icon(Icons.Default.KeyboardVoice, contentDescription = "Voice Dictation", tint = MaterialTheme.colorScheme.secondary)
                }
                IconButton(
                  onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    playCue(VoiceAudioManager.TouchCue.BUTTON_TAP)
                    if (!cloudSyncReady) {
                      statusToast = "Sign in with Email or Google in Settings to send live voice notes."
                      return@IconButton
                    }
                    if (recordingVoice) {
                      recordingVoice = false
                      val (b64, _) = socialVm.audio.finishPttCapture()
                      val peer = selectedPeer ?: return@IconButton
                      if (b64.isNotBlank()) {
                        playCue(VoiceAudioManager.TouchCue.MESSAGE_SENT)
                        socialVm.sendDm(myCallsign, peer.userId, peer.callsign, "", null, b64, selfDestructSec) { tid ->
                          activeThreadId = tid
                        }
                        statusToast = "Voice note sent!"
                      } else {
                        statusToast = "Voice recording was too short — hold or speak longer."
                      }
                    } else if (socialVm.audio.hasRecordPermission()) {
                      recordingVoice = socialVm.audio.startPttCapture(scope, 10_000L) { b64, _ ->
                        recordingVoice = false
                        val peer = selectedPeer ?: return@startPttCapture
                        if (b64.isNotBlank()) {
                          playCue(VoiceAudioManager.TouchCue.MESSAGE_SENT)
                          socialVm.sendDm(myCallsign, peer.userId, peer.callsign, "", null, b64, selfDestructSec) { tid ->
                            activeThreadId = tid
                          }
                          statusToast = "Voice note sent!"
                        }
                      }
                      if (recordingVoice) statusToast = "Recording voice note... tap Stop to send."
                    } else {
                      audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                  },
                  modifier = Modifier.size(48.dp).testTag("dm_record_voice_button"),
                ) {
                  Icon(
                    if (recordingVoice) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (recordingVoice) "Stop Voice Note" else "Record Voice Note",
                    tint = if (recordingVoice) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                  )
                }
              }
              Button(
                onClick = {
                  haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  if (!cloudSyncReady) {
                    playCue(VoiceAudioManager.TouchCue.ERROR_THUD)
                    statusToast = "Sign in with Email or Google in Settings to send live DMs."
                    return@Button
                  }
                  val peer = selectedPeer ?: return@Button
                  val quoted =
                    replyTarget?.let { "> ${it.senderCallsign}: ${it.text.take(80)}\n" }.orEmpty()
                  val txt = (quoted + draftText).trim()
                  val bmp = attachedBitmap
                  if (txt.isBlank() && bmp == null) {
                    playCue(VoiceAudioManager.TouchCue.ERROR_THUD)
                    statusToast = "Type a message or attach a photo first."
                    return@Button
                  }
                  playCue(VoiceAudioManager.TouchCue.MESSAGE_SENT)
                  draftText = ""
                  attachedBitmap = null
                  replyTarget = null
                  socialVm.sendDm(myCallsign, peer.userId, peer.callsign, txt, bmp, "", selfDestructSec) { tid ->
                    activeThreadId = tid
                  }
                  statusToast = "Message sent to ${peer.callsign}."
                },
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                modifier = Modifier.height(48.dp).testTag("send_dm_button"),
              ) {
                Icon(Icons.Default.Send, contentDescription = "Send DM", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Send", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
              }
            }
          }
        }
      }
    }
  }
  }

  lightboxImageUrl?.let { url ->
    PostImageLightboxDialog(imageUrl = url, onDismiss = { lightboxImageUrl = null })
  }
}
