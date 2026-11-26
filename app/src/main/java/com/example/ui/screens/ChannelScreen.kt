package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.UiState
import com.example.data.YouTubeVideoItem
import com.example.hardware.VoiceAudioManager
import com.example.ui.components.TacticalButton
import com.example.ui.components.TacticalCard
import com.example.ui.components.TacticalIconButton
import com.example.ui.components.TacticalOutlinedButton
import com.example.viewmodel.CopilotSocialViewModel
import com.example.viewmodel.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelScreen(
  sessionVm: SessionViewModel,
  socialVm: CopilotSocialViewModel,
) {
  val activeUser by sessionVm.activeUser.collectAsStateWithLifecycle()
  val profile by sessionVm.profile.collectAsStateWithLifecycle()
  val settings by sessionVm.settings.collectAsStateWithLifecycle()
  val ytState by socialVm.youtubeState.collectAsStateWithLifecycle()
  val musicMixesState by socialVm.musicMixesState.collectAsStateWithLifecycle()
  val activeMusicCategory by socialVm.activeMusicCategory.collectAsStateWithLifecycle()
  val activeVideo by socialVm.activeVideo.collectAsStateWithLifecycle()
  val savedVideos by socialVm.savedVideos.collectAsStateWithLifecycle()
  val recentlyPlayed by socialVm.recentlyPlayedVideos.collectAsStateWithLifecycle()
  val sleepTimerSec by socialVm.sleepTimerRemainingSec.collectAsStateWithLifecycle()
  val socialNotice by socialVm.socialNotice.collectAsStateWithLifecycle()

  var searchQuery by remember { mutableStateOf("") }
  var feedMode by remember { mutableIntStateOf(0) } // 0 = Rural ERides, 1 = Global PEV, 2 = Music Mixes
  var sortMode by remember { mutableStateOf("DEFAULT") } // DEFAULT, SHORTEST, LONGEST
  var showWatchlist by remember { mutableStateOf(false) }
  var showRecentHistory by remember { mutableStateOf(false) }
  var showRequestDialog by remember { mutableStateOf(false) }

  val ctx = androidx.compose.ui.platform.LocalContext.current
  val effectiveUid = activeUser?.uid?.ifBlank { "local_pilot" } ?: "local_pilot"
  val callsign = profile?.callsign?.ifBlank { "PILOT" } ?: "PILOT"
  fun playCue(cue: VoiceAudioManager.TouchCue) {
    sessionVm.voiceAudio.playTouchSound(
      enabled = settings.touchSoundsEnabled,
      pack = settings.touchSoundPack,
      cue = cue,
      volumePct = settings.soundVolumePct,
    )
  }
  val musicGenres =
    listOf(
      "Country Folk Trail",
      "Outlaw Country",
      "Bluegrass Ride",
      "Americana Acoustic",
      "Synthwave",
      "Lo-Fi",
      "Phonk",
      "Drum & Bass",
      "Cyberpunk",
      "Rock",
      "Hip-Hop",
      "Deep House",
      "Ambient",
      "Dubstep",
      "Retrowave 80s",
      "Acoustic",
    )
  val activeFeedState = if (feedMode == 2) musicMixesState else ytState

  fun durationToSeconds(label: String): Int {
    val parts = label.split(":").mapNotNull { it.trim().toIntOrNull() }
    return when (parts.size) {
      3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
      2 -> parts[0] * 60 + parts[1]
      else -> 0
    }
  }

  val pad = if (settings.spaciousLayoutMode) 18.dp else 14.dp
  val gap = if (settings.spaciousLayoutMode) 16.dp else 12.dp
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = pad, end = pad, top = pad, bottom = if (activeVideo != null) 116.dp else 32.dp),
    verticalArrangement = Arrangement.spacedBy(gap),
  ) {
    if (socialNotice.isNotBlank()) {
      item {
        TacticalCard(highlight = true) {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(socialNotice, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            TextButton(onClick = { socialVm.clearNotice() }) { Text("OK", maxLines = 1, softWrap = false) }
          }
        }
      }
    }

    item {
      TacticalCard(highlight = true, title = "Country Trail TV & Radio") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
              "Trail TV, Barn Builds & Radio",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.primary,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
            )
            val modeTxt = (activeFeedState as? UiState.Success)?.data?.second ?: "Loading"
            Text("In-App Theater • $modeTxt", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          TacticalOutlinedButton(
            onClick = { playCue(VoiceAudioManager.TouchCue.BUTTON_TAP); showWatchlist = true },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.testTag("open_watchlist_button"),
          ) {
            Icon(Icons.Default.VideoLibrary, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Vault (${savedVideos.size})", maxLines = 1, softWrap = false)
          }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          label = { Text(if (feedMode == 2) "Search artist, mix, or riding mood..." else "Search videos or global PEV reviews") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("youtube_search_input"),
        )
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          item {
            FilterChip(selected = feedMode == 0, onClick = { feedMode = 0 }, label = { Text("Rural ERides", maxLines = 1, softWrap = false) })
          }
          item {
            FilterChip(selected = feedMode == 1, onClick = { feedMode = 1 }, label = { Text("Global PEV", maxLines = 1, softWrap = false) })
          }
          item {
            FilterChip(
              selected = feedMode == 2,
              onClick = {
                feedMode = 2
                if (musicMixesState !is UiState.Success) {
                  socialVm.loadMusicMixes(activeMusicCategory, searchQuery)
                }
              },
              label = { Text("Music Mixes", maxLines = 1, softWrap = false) },
              leadingIcon = { Icon(Icons.Default.MusicNote, null, Modifier.size(16.dp)) },
            )
          }
          item {
            TacticalOutlinedButton(
              onClick = { showRequestDialog = true },
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
              modifier = Modifier.widthIn(min = 132.dp).testTag("request_video_button"),
            ) {
              Text("Request Video", maxLines = 1, softWrap = false)
            }
          }
        }

        if (feedMode == 2) {
          Spacer(Modifier.height(6.dp))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(musicGenres) { genre ->
              FilterChip(
                selected = activeMusicCategory.equals(genre, ignoreCase = true),
                onClick = { socialVm.loadMusicMixes(genre, searchQuery) },
                label = { Text(genre, maxLines = 1, softWrap = false) },
              )
            }
          }
        } else {
          val pevPresets =
            listOf(
              "Rural Gravel Rides",
              "Barn PEV Builds",
              "Off-Grid Solar Charging",
              "Country Backroad Trails",
              "Mud & Trail Tires",
              "Homestead eMoto",
              "Scooter Reviews",
              "EUC Trail",
              "eBike Range",
              "Onewheel Tricks",
              "Sur-Ron & eMoto",
              "VESC Tuning",
              "Tire & Brake Repair",
              "Battery Builds",
              "Range Tests",
              "PEV Laws",
            )
          Spacer(Modifier.height(6.dp))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(pevPresets) { tag ->
              FilterChip(
                selected = searchQuery.equals(tag, ignoreCase = true),
                onClick = {
                  searchQuery = tag
                  feedMode = 1
                  socialVm.loadYouTubeFeed(tag, channelOnly = false)
                },
                label = { Text(tag, maxLines = 1, softWrap = false) },
                modifier = Modifier.testTag("channel_preset_${tag.lowercase().replace(" ", "_")}"),
              )
            }
          }
        }

        // AUTO-DJ, RECENT HISTORY, SLEEP TIMER & SORT BAR
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          item {
            AssistChip(
              onClick = {
                feedMode = 2
                socialVm.launchAutoDjPevMix()
              },
              leadingIcon = { Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) },
              label = { Text("Auto-DJ Mix", maxLines = 1, softWrap = false) },
              modifier = Modifier.testTag("channel_auto_dj_button"),
            )
          }
          item {
            AssistChip(
              onClick = { showRecentHistory = true },
              leadingIcon = { Icon(Icons.Default.History, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary) },
              label = { Text("Recent (${recentlyPlayed.size})", maxLines = 1, softWrap = false) },
              modifier = Modifier.testTag("channel_recent_history_button"),
            )
          }
          item {
            val nextSleepMin =
              when {
                sleepTimerSec <= 0 -> 15
                sleepTimerSec <= 15 * 60 -> 30
                sleepTimerSec <= 30 * 60 -> 45
                sleepTimerSec <= 45 * 60 -> 60
                else -> 0
              }
            val sleepLabel =
              if (sleepTimerSec > 0) "Sleep ${(sleepTimerSec + 59) / 60}m" else "Sleep Off"
            FilterChip(
              selected = sleepTimerSec > 0,
              onClick = { socialVm.setSleepTimerMinutes(nextSleepMin) },
              leadingIcon = { Icon(Icons.Default.Bedtime, null, Modifier.size(16.dp)) },
              label = { Text(sleepLabel, maxLines = 1, softWrap = false) },
              modifier = Modifier.testTag("channel_sleep_timer_chip"),
            )
          }
          items(listOf("DEFAULT" to "Default", "SHORTEST" to "Shortest", "LONGEST" to "Longest")) { (mode, lbl) ->
            FilterChip(
              selected = sortMode == mode,
              onClick = { sortMode = mode },
              label = { Text("Sort: $lbl", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
            )
          }
        }

        Spacer(Modifier.height(8.dp))
        val listNow = (activeFeedState as? UiState.Success)?.data?.first.orEmpty()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          TacticalButton(
            onClick = {
              playCue(VoiceAudioManager.TouchCue.BUTTON_TAP)
              if (feedMode == 2) socialVm.loadMusicMixes(activeMusicCategory, searchQuery)
              else socialVm.loadYouTubeFeed(searchQuery, channelOnly = feedMode == 0)
            },
            modifier = Modifier.weight(1f).testTag("search_youtube_button"),
          ) {
            Icon(Icons.Default.Search, null, Modifier.size(17.dp))
            Spacer(Modifier.width(5.dp))
            Text(if (feedMode == 2) "Load Radio" else "Fetch Feed", maxLines = 1, softWrap = false)
          }
          if (listNow.isNotEmpty()) {
            TacticalOutlinedButton(
              onClick = {
                playCue(VoiceAudioManager.TouchCue.BUTTON_TAP)
                val shuffled = listNow.shuffled()
                socialVm.playInMiniPlayer(shuffled.first(), shuffled, expandTheater = false)
              },
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
              modifier = Modifier.testTag("shuffle_channel_queue_button"),
            ) {
              Icon(Icons.Default.Shuffle, contentDescription = "Shuffle Play", modifier = Modifier.size(17.dp))
              Spacer(Modifier.width(4.dp))
              Text("Shuffle", maxLines = 1, softWrap = false)
            }
            TacticalOutlinedButton(
              onClick = {
                playCue(VoiceAudioManager.TouchCue.BUTTON_TAP)
                socialVm.playInMiniPlayer(listNow.first(), listNow, expandTheater = false)
              },
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
              modifier = Modifier.testTag("play_all_channel_queue_button"),
            ) {
              Icon(Icons.Default.PlaylistPlay, contentDescription = "Queue All", modifier = Modifier.size(17.dp))
              Spacer(Modifier.width(4.dp))
              Text("Queue", maxLines = 1, softWrap = false)
            }
          }
        }
        if (savedVideos.isNotEmpty()) {
          Spacer(Modifier.height(8.dp))
          Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                "Saved Watch-Later Queue (${savedVideos.size} items)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
              TextButton(
                onClick = {
                  playCue(VoiceAudioManager.TouchCue.BUTTON_TAP)
                  val savedItems =
                    savedVideos.map { sv ->
                      YouTubeVideoItem(sv.videoId, sv.title, "", sv.channelTitle, sv.publishedAt, sv.thumbnailUrl)
                    }
                  socialVm.playInMiniPlayer(savedItems.first(), savedItems, expandTheater = false)
                },
                modifier = Modifier.testTag("play_saved_vault_queue_btn"),
              ) {
                Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                Spacer(Modifier.width(3.dp))
                Text("Play Saved", maxLines = 1, softWrap = false)
              }
            }
          }
        }
      }
    }

    item(key = "channel_rural_discovery_deck") {
      ChannelRuralDiscoveryDeck(
        onLaunchSession = { session ->
          playCue(VoiceAudioManager.TouchCue.BUTTON_TAP)
          searchQuery = session.query
          if (session.isMusicMix) {
            feedMode = 2
            socialVm.loadMusicMixes(activeMusicCategory, session.query)
          } else {
            feedMode = 1
            socialVm.loadYouTubeFeed(session.query, channelOnly = false)
          }
        },
      )
    }

    when (val st = activeFeedState) {
      is UiState.Loading -> item {
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.Center) {
          CircularProgressIndicator()
        }
      }
      is UiState.Error -> item {
        TacticalCard {
          Text(st.message, color = MaterialTheme.colorScheme.error)
          Spacer(Modifier.height(8.dp))
          Button(onClick = {
            if (feedMode == 2) socialVm.loadMusicMixes(activeMusicCategory, searchQuery)
            else socialVm.loadYouTubeFeed(searchQuery, channelOnly = feedMode == 0)
          }) {
            Text("Retry In-App YouTube Feed")
          }
        }
      }
      is UiState.Success -> {
        val rawFeed = st.data.first
        val list =
          when (sortMode) {
            "SHORTEST" -> rawFeed.sortedBy { durationToSeconds(it.durationLabel) }
            "LONGEST" -> rawFeed.sortedByDescending { durationToSeconds(it.durationLabel) }
            else -> rawFeed
          }
        if (list.isEmpty()) {
          item {
            TacticalCard {
              Text("No embeddable videos matched that query. Try a broader term.", style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
        items(list, key = { "${feedMode}_${it.videoId}" }) { video ->
          val isSaved = savedVideos.any { it.videoId == video.videoId }
          val isPlayingNow = activeVideo?.videoId == video.videoId
          TacticalCard(
            highlight = isPlayingNow,
            onClick = { socialVm.playInMiniPlayer(video, list, expandTheater = true) },
          ) {
            Box(Modifier.fillMaxWidth()) {
              AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(175.dp).clip(RoundedCornerShape(10.dp)),
              )
              Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
              ) {
                Text(
                  text = if (isPlayingNow) "ACTIVE IN PLAYER" else video.durationLabel,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Column(Modifier.weight(1f)) {
                Text(video.title, style = MaterialTheme.typography.titleMedium)
                Text("${video.channelTitle} • ${video.publishedAt} • ${video.durationLabel}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
              }
              TacticalIconButton(
                onClick = {
                  playCue(VoiceAudioManager.TouchCue.BUTTON_TAP)
                  socialVm.enqueuePlayNext(video)
                },
                modifier = Modifier.testTag("queue_next_${video.videoId}"),
              ) {
                Icon(Icons.Default.QueueMusic, contentDescription = "Play Next in Queue", tint = MaterialTheme.colorScheme.secondary)
              }
              TacticalIconButton(
                onClick = {
                  playCue(VoiceAudioManager.TouchCue.BUTTON_TAP)
                  socialVm.playInMiniPlayer(video, list, expandTheater = false)
                },
                modifier = Modifier.testTag("dock_play_${video.videoId}"),
              ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play in Mini-Player", tint = MaterialTheme.colorScheme.primary)
              }
              TacticalIconButton(onClick = {
                playCue(if (isSaved) VoiceAudioManager.TouchCue.TOGGLE_OFF else VoiceAudioManager.TouchCue.TOGGLE_ON)
                socialVm.toggleSaveVideo(effectiveUid, video)
              }) {
                Icon(
                  if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                  contentDescription = "Save Video",
                  tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
              }
              TacticalIconButton(
                onClick = {
                  val send =
                    android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                      type = "text/plain"
                      putExtra(android.content.Intent.EXTRA_SUBJECT, video.title)
                      putExtra(android.content.Intent.EXTRA_TEXT, "${video.title}\nhttps://youtu.be/${video.videoId}")
                    }
                  runCatching { ctx.startActivity(android.content.Intent.createChooser(send, "Share PEV Video")) }
                },
                modifier = Modifier.testTag("share_video_${video.videoId}"),
              ) {
                Icon(
                  Icons.Default.Share,
                  contentDescription = "Share Video",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    }
  }

  if (showWatchlist) {
    ModalBottomSheet(onDismissRequest = { showWatchlist = false }) {
      Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text("WATCHLIST & FIELD NOTES (${savedVideos.size})", style = MaterialTheme.typography.titleLarge)
          IconButton(onClick = { showWatchlist = false }) { Icon(Icons.Default.Close, contentDescription = "Close Vault") }
        }
        LazyColumn(modifier = Modifier.height(340.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          items(savedVideos, key = { it.videoId }) { sv ->
            var note by remember(sv.videoId) { mutableStateOf(sv.personalNote) }
            val item =
              YouTubeVideoItem(
                videoId = sv.videoId,
                title = sv.title,
                description = "",
                channelTitle = sv.channelTitle,
                publishedAt = sv.publishedAt,
                thumbnailUrl = sv.thumbnailUrl,
              )
            TacticalCard {
              Text(sv.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
              Text("${sv.channelTitle} • ${sv.categoryTag}", style = MaterialTheme.typography.labelSmall)
              Spacer(Modifier.height(6.dp))
              OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Timestamp / Tech Note") }, modifier = Modifier.fillMaxWidth())
              Spacer(Modifier.height(6.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                  showWatchlist = false
                  socialVm.playInMiniPlayer(item, listOf(item), expandTheater = true)
                }) { Text("Play") }
                OutlinedButton(onClick = {
                  playCue(VoiceAudioManager.TouchCue.SUCCESS_CHIME)
                  socialVm.updateSavedVideoNote(effectiveUid, sv, sv.categoryTag, note)
                }) { Text("Save Note", maxLines = 1, softWrap = false) }
                OutlinedButton(onClick = {
                  playCue(VoiceAudioManager.TouchCue.TOGGLE_OFF)
                  socialVm.toggleSaveVideo(effectiveUid, item)
                }) { Text("Remove", maxLines = 1, softWrap = false) }
              }
            }
          }
        }
      }
    }
  }

  if (showRecentHistory) {
    ModalBottomSheet(onDismissRequest = { showRecentHistory = false }) {
      Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text("RECENTLY PLAYED (${recentlyPlayed.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
          IconButton(onClick = { showRecentHistory = false }) {
            Icon(Icons.Default.Close, contentDescription = "Close Recent History", tint = MaterialTheme.colorScheme.onSurface)
          }
        }
        LazyColumn(modifier = Modifier.height(330.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          if (recentlyPlayed.isEmpty()) {
            item {
              TacticalCard {
                Text("Play any PEV video or trail music track to build your quick-replay history here.", style = MaterialTheme.typography.bodySmall)
              }
            }
          }
          items(recentlyPlayed, key = { "recent_${it.videoId}" }) { rv ->
            TacticalCard(onClick = {
              showRecentHistory = false
              socialVm.playInMiniPlayer(rv, recentlyPlayed, expandTheater = true)
            }) {
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                  Text(rv.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, maxLines = 2)
                  Text("${rv.channelTitle} • ${rv.durationLabel}", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = { socialVm.enqueuePlayNext(rv) }) {
                  Icon(Icons.Default.QueueMusic, contentDescription = "Queue Next", tint = MaterialTheme.colorScheme.secondary)
                }
              }
            }
          }
        }
      }
    }
  }

  if (showRequestDialog) {
    var topic by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    com.example.ui.components.TacticalDialog(
      onDismissRequest = { showRequestDialog = false },
      title = "Submit Video Topic Request",
      subtitle = "Trail TV Creator Queue",
      badgeText = "REQUEST",
      confirmButton = {
        com.example.ui.components.TacticalButton(
          onClick = {
            if (topic.isNotBlank()) {
              socialVm.submitVideoRequestToBoard(callsign, settings.pilotRankBadge, topic, details)
              showRequestDialog = false
            }
          },
          enabled = topic.isNotBlank(),
        ) { Text("Post Request") }
      },
      dismissButton = {
        com.example.ui.components.TacticalOutlinedButton(onClick = { showRequestDialog = false }) { Text("Cancel") }
      },
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = topic,
          onValueChange = { topic = it },
          label = { Text("Vehicle / Topic") },
          colors = com.example.ui.components.tacticalOutlinedTextFieldColors(),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = details,
          onValueChange = { details = it },
          label = { Text("What should Rural ERides test?") },
          colors = com.example.ui.components.tacticalOutlinedTextFieldColors(),
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }
}
