package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.viewmodel.CopilotSocialViewModel
import com.example.viewmodel.SessionViewModel
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import java.net.URLEncoder

@Composable
fun YoutubeMiniPlayerHost(
  sessionVm: SessionViewModel,
  socialVm: CopilotSocialViewModel,
) {
  val activeVideo by socialVm.activeVideo.collectAsStateWithLifecycle()
  val queue by socialVm.playerQueue.collectAsStateWithLifecycle()
  val playing by socialVm.playerPlaying.collectAsStateWithLifecycle()
  val expanded by socialVm.playerExpanded.collectAsStateWithLifecycle()
  val muted by socialVm.playerMuted.collectAsStateWithLifecycle()
  val shuffle by socialVm.playerShuffle.collectAsStateWithLifecycle()
  val repeatMode by socialVm.playerRepeatMode.collectAsStateWithLifecycle()
  val playbackSpeed by socialVm.playerSpeed.collectAsStateWithLifecycle()
  val sleepRemainingSec by socialVm.sleepTimerRemainingSec.collectAsStateWithLifecycle()
  val playerError by socialVm.playerError.collectAsStateWithLifecycle()
  val savedVideos by socialVm.savedVideos.collectAsStateWithLifecycle()
  val volts by socialVm.videoVolts.collectAsStateWithLifecycle()
  val comments by socialVm.videoComments.collectAsStateWithLifecycle()
  val aiAnswer by socialVm.videoAiAnswer.collectAsStateWithLifecycle()
  val aiBusy by socialVm.videoAiBusy.collectAsStateWithLifecycle()
  val activeUser by sessionVm.activeUser.collectAsStateWithLifecycle()
  val profile by sessionVm.profile.collectAsStateWithLifecycle()
  val settings by sessionVm.settings.collectAsStateWithLifecycle()
  val isClassic = isWindowsClassicActive(settings)
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  val video = activeVideo ?: return
  val callsign = profile?.callsign?.ifBlank { "PILOT" } ?: "PILOT"
  val isSaved = savedVideos.any { it.videoId == video.videoId }

  val currentVideoState = rememberUpdatedState(video)
  val currentPlayingState = rememberUpdatedState(playing)
  val currentMutedState = rememberUpdatedState(muted)
  val currentSpeedState = rememberUpdatedState(playbackSpeed)

  var playerViewRef by remember { mutableStateOf<YouTubePlayerView?>(null) }
  var ytPlayerRef by remember { mutableStateOf<YouTubePlayer?>(null) }
  val playerTracker = remember { YouTubePlayerTracker() }
  var currentSecondRef by remember { mutableFloatStateOf(0f) }
  var loadedVideoId by remember { mutableStateOf("") }
  var videoFrameReady by remember { mutableStateOf(false) }
  val mixesState by socialVm.musicMixesState.collectAsStateWithLifecycle()
  var aiQuestion by remember { mutableStateOf("") }
  var commentText by remember { mutableStateOf("") }
  var miniSearchOpen by remember { mutableStateOf(false) }
  var miniSearchQuery by remember { mutableStateOf("") }

  fun seekRelativeSeconds(deltaSec: Int) {
    val player = ytPlayerRef ?: return
    val baseSecond = if (playerTracker.currentSecond > 0f) playerTracker.currentSecond else currentSecondRef
    val targetSec = (baseSecond + deltaSec.toFloat()).coerceAtLeast(0f)
    currentSecondRef = targetSec
    player.seekTo(targetSec)
  }

  if (expanded) {
    BackHandler { socialVm.togglePlayerExpanded(false) }
  }

  DisposableEffect(lifecycleOwner, playerViewRef) {
    val view = playerViewRef
    if (view != null) {
      lifecycleOwner.lifecycle.addObserver(view)
    }
    onDispose {
      if (view != null) {
        lifecycleOwner.lifecycle.removeObserver(view)
      }
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      playerViewRef?.release()
      playerViewRef = null
      ytPlayerRef = null
    }
  }

  LaunchedEffect(video.videoId, ytPlayerRef) {
    val player = ytPlayerRef ?: return@LaunchedEffect
    val safeId = sanitizeYoutubeVideoId(video.videoId)
    if (safeId.isNotBlank() && loadedVideoId != safeId) {
      loadedVideoId = safeId
      currentSecondRef = 0f
      videoFrameReady = false
      socialVm.setPlayerError("")
      if (currentPlayingState.value) {
        player.loadVideo(safeId, 0f)
      } else {
        player.cueVideo(safeId, 0f)
      }
      if (currentMutedState.value) player.mute() else player.unMute()
      player.setPlaybackRate(mapPlaybackRate(currentSpeedState.value))
    }
  }

  LaunchedEffect(playing, ytPlayerRef) {
    val player = ytPlayerRef ?: return@LaunchedEffect
    if (playing) {
      if (playerTracker.state != PlayerConstants.PlayerState.PLAYING &&
        playerTracker.state != PlayerConstants.PlayerState.BUFFERING
      ) {
        player.play()
      }
    } else {
      if (playerTracker.state == PlayerConstants.PlayerState.PLAYING ||
        playerTracker.state == PlayerConstants.PlayerState.BUFFERING
      ) {
        player.pause()
      }
    }
  }

  LaunchedEffect(muted, ytPlayerRef) {
    val player = ytPlayerRef ?: return@LaunchedEffect
    if (muted) player.mute() else player.unMute()
  }

  LaunchedEffect(playbackSpeed, ytPlayerRef, loadedVideoId) {
    val player = ytPlayerRef ?: return@LaunchedEffect
    player.setPlaybackRate(mapPlaybackRate(playbackSpeed))
  }

  // Persistent movable native YouTubePlayerView keeps playback alive seamlessly between docked mini-player & expanded theater
  val persistentYouTubePlayer =
    remember {
      movableContentOf { mod: Modifier ->
        AndroidView(
          factory = { ctx ->
            val embedOrigin = youtubeEmbedOriginForPackage(ctx.packageName)
            val iFrameOptions =
              IFramePlayerOptions.Builder(ctx)
                .controls(0)
                .rel(0)
                .ivLoadPolicy(3)
                .origin(embedOrigin)
                .build()

            YouTubePlayerView(ctx).apply {
              layoutParams =
                ViewGroup.LayoutParams(
                  ViewGroup.LayoutParams.MATCH_PARENT,
                  ViewGroup.LayoutParams.MATCH_PARENT,
                )
              enableAutomaticInitialization = false
              val listener =
                object : AbstractYouTubePlayerListener() {
                  override fun onReady(youTubePlayer: YouTubePlayer) {
                    youTubePlayer.addListener(playerTracker)
                    ytPlayerRef = youTubePlayer
                    val safeId = sanitizeYoutubeVideoId(currentVideoState.value.videoId)
                    if (safeId.isNotBlank()) {
                      loadedVideoId = safeId
                      currentSecondRef = 0f
                      socialVm.setPlayerError("")
                      if (currentPlayingState.value) {
                        youTubePlayer.loadVideo(safeId, 0f)
                      } else {
                        youTubePlayer.cueVideo(safeId, 0f)
                      }
                      if (currentMutedState.value) youTubePlayer.mute() else youTubePlayer.unMute()
                      youTubePlayer.setPlaybackRate(mapPlaybackRate(currentSpeedState.value))
                    }
                  }

                  override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                    currentSecondRef = second
                  }

                  override fun onStateChange(
                    youTubePlayer: YouTubePlayer,
                    state: PlayerConstants.PlayerState,
                  ) {
                    when (state) {
                      PlayerConstants.PlayerState.PLAYING -> {
                        videoFrameReady = true
                        socialVm.setPlayerError("")
                        socialVm.setPlayerPlayingFromWeb(true)
                      }
                      PlayerConstants.PlayerState.PAUSED -> {
                        videoFrameReady = true
                        socialVm.setPlayerPlayingFromWeb(false)
                      }
                      PlayerConstants.PlayerState.BUFFERING,
                      PlayerConstants.PlayerState.VIDEO_CUED -> {
                        videoFrameReady = true
                      }
                      PlayerConstants.PlayerState.ENDED -> {
                        socialVm.playNextInQueue(fromAutoEnded = true)
                      }
                      else -> Unit
                    }
                  }

                  override fun onError(
                    youTubePlayer: YouTubePlayer,
                    error: PlayerConstants.PlayerError,
                  ) {
                    val msg =
                      when (error) {
                        PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER ->
                          "Uploader disabled embedding for this video. Tap Open in YouTube App or Next."
                        PlayerConstants.PlayerError.VIDEO_NOT_FOUND ->
                          "Video unavailable or removed on YouTube. Tap Open in YouTube App or Next."
                        PlayerConstants.PlayerError.HTML_5_PLAYER ->
                          "Playback engine error. Tap Open in YouTube App or Next."
                        PlayerConstants.PlayerError.INVALID_PARAMETER_IN_REQUEST ->
                          "Invalid video ID. Tap Open in YouTube App or Next."
                        else ->
                          "Playback restricted (${error.name}). Tap Open in YouTube App or Next."
                      }
                    socialVm.setPlayerError(msg)
                  }
                }
              initialize(listener, iFrameOptions)
              playerViewRef = this
            }
          },
          modifier = mod,
        )
      }
    }

  val isDarkBg = MaterialTheme.colorScheme.background.luminance() < 0.5f
  val theaterHeaderTextColor = if (isDarkBg) Color.White else MaterialTheme.colorScheme.onBackground
  val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
  val miniBarTitleColor = if (isDarkSurface) Color.White else MaterialTheme.colorScheme.onSurface

  if (expanded) {
    Surface(
      color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
      contentColor = theaterHeaderTextColor,
      modifier = Modifier.fillMaxSize().statusBarsPadding(),
    ) {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        item {
          Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            TacticalIconButton(
              onClick = { socialVm.togglePlayerExpanded(false) },
              modifier = Modifier.testTag("collapse_mini_player_button"),
            ) {
              Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Minimize Player",
                tint = theaterHeaderTextColor,
              )
            }
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
              Text(
                "IN-APP MEDIA THEATER",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium,
                color = theaterHeaderTextColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("media_theater_header_title"),
              )
            }
            TacticalIconButton(
              onClick = { miniSearchOpen = !miniSearchOpen },
              modifier = Modifier.testTag("theater_search_toggle_button"),
            ) {
              Icon(
                Icons.Default.Search,
                contentDescription = "Search Music or Videos",
                tint = if (miniSearchOpen) MaterialTheme.colorScheme.primary else theaterHeaderTextColor,
              )
            }
            TacticalIconButton(
              onClick = { socialVm.closeMiniPlayer() },
              modifier = Modifier.testTag("close_expanded_player_button"),
            ) {
              Icon(
                Icons.Default.Close,
                contentDescription = "Close Player",
                tint = theaterHeaderTextColor,
              )
            }
          }
        }

        if (miniSearchOpen) {
          item {
            TacticalCard(highlight = true, title = "SEARCH_MUSIC_MEDIA.EXE") {
              Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                  value = miniSearchQuery,
                  onValueChange = { miniSearchQuery = it },
                  placeholder = { Text("Search songs, artists, podcasts, videos...") },
                  singleLine = true,
                  modifier = Modifier.weight(1f).testTag("theater_search_input"),
                )
                Button(
                  onClick = {
                    val q = miniSearchQuery.trim()
                    if (q.isNotBlank()) socialVm.loadMusicMixes("Custom: $q", customQuery = q)
                  },
                  enabled = miniSearchQuery.isNotBlank(),
                ) {
                  Icon(Icons.Default.Search, contentDescription = "Search")
                }
              }
              Spacer(Modifier.height(6.dp))
              when (val st = mixesState) {
                is com.example.data.UiState.Loading -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                is com.example.data.UiState.Error -> Text(st.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                is com.example.data.UiState.Success -> {
                  val resList = st.data.first
                  LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(resList.take(12), key = { it.videoId }) { item ->
                      Surface(
                        onClick = {
                          socialVm.playInMiniPlayer(item, resList, expandTheater = true)
                          miniSearchOpen = false
                        },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.width(190.dp),
                      ) {
                        Column(Modifier.padding(8.dp)) {
                          Text(item.title, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                          Text(item.channelTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }

        item {
          Box(
            modifier =
              Modifier
                .fillMaxWidth()
                .height(215.dp)
                .clip(RoundedCornerShape(if (isClassic) 0.dp else 14.dp))
                .background(Color.Black),
          ) {
            AsyncImage(
              model = video.thumbnailUrl.ifBlank { "https://i.ytimg.com/vi/${video.videoId}/hqdefault.jpg" },
              contentDescription = video.title,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
            persistentYouTubePlayer(Modifier.fillMaxSize())
          }
          if (playerError.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Row(
              Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                playerError,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
              )
              TacticalOutlinedButton(
                onClick = { openInYouTubeApp(context, video.videoId) },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("error_open_youtube_app_button"),
              ) { Text("Open in YouTube App", maxLines = 1, softWrap = false) }
              if (queue.size > 1) {
                TacticalOutlinedButton(
                  onClick = { socialVm.playNextInQueue() },
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                ) { Text("Next", maxLines = 1, softWrap = false) }
              }
            }
          }
        }

        item {
          TacticalCard(highlight = true, title = "MEDIA_PLAYER.EXE") {
            Text(
              video.title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              "${video.channelTitle} • ${video.durationLabel}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(10.dp))
            Row(
              Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              TacticalIconButton(
                onClick = { socialVm.playPrevInQueue() },
                enabled = queue.size > 1,
                modifier = Modifier.testTag("player_prev_button"),
              ) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Track")
              }
              TacticalIconButton(
                onClick = { seekRelativeSeconds(-10) },
                modifier = Modifier.testTag("player_seek_back_10"),
              ) {
                Icon(Icons.Default.Replay10, contentDescription = "Rewind 10 Seconds")
              }
              TacticalButton(
                onClick = { socialVm.togglePlayerPlaying() },
                modifier = Modifier.testTag("expanded_play_pause_button"),
              ) {
                Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(4.dp))
                Text(if (playing) "Pause" else "Play", maxLines = 1, softWrap = false)
              }
              TacticalIconButton(
                onClick = { seekRelativeSeconds(10) },
                modifier = Modifier.testTag("player_seek_fwd_10"),
              ) {
                Icon(Icons.Default.Forward10, contentDescription = "Forward 10 Seconds")
              }
              TacticalIconButton(
                onClick = { socialVm.playNextInQueue() },
                enabled = queue.size > 1,
                modifier = Modifier.testTag("player_next_button"),
              ) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next Track")
              }
              TacticalIconButton(
                onClick = { socialVm.togglePlayerMuted() },
                modifier = Modifier.testTag("player_mute_button"),
              ) {
                Icon(
                  if (muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                  contentDescription = "Toggle Mute",
                )
              }
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              item {
                FilterChip(
                  selected = shuffle,
                  onClick = {
                    socialVm.togglePlayerShuffle()
                    sessionVm.updateSettings { it.copy(ytShuffleEnabled = !shuffle) }
                  },
                  leadingIcon = { Icon(Icons.Default.Shuffle, null, Modifier.size(16.dp)) },
                  label = { Text(if (shuffle) "Shuffle On" else "Shuffle") },
                  modifier = Modifier.testTag("player_shuffle_chip"),
                )
              }
              item {
                FilterChip(
                  selected = repeatMode != "off",
                  onClick = {
                    socialVm.cyclePlayerRepeatMode()
                    val nextRep = when (repeatMode) { "off" -> "all"; "all" -> "one"; else -> "off" }
                    sessionVm.updateSettings { it.copy(ytRepeatMode = nextRep) }
                  },
                  leadingIcon = {
                    Icon(
                      if (repeatMode == "one") Icons.Default.RepeatOne else Icons.Default.Repeat,
                      null,
                      Modifier.size(16.dp),
                    )
                  },
                  label = { Text("Repeat: ${repeatMode.uppercase()}") },
                  modifier = Modifier.testTag("player_repeat_chip"),
                )
              }
              item {
                FilterChip(
                  selected = playbackSpeed != 1.0f,
                  onClick = {
                    socialVm.cyclePlayerSpeed()
                    val nextSpd = when (playbackSpeed) { 0.75f -> 1.0f; 1.0f -> 1.25f; 1.25f -> 1.5f; 1.5f -> 2.0f; else -> 0.75f }
                    sessionVm.updateSettings { it.copy(ytPlaybackSpeed = nextSpd) }
                  },
                  leadingIcon = { Icon(Icons.Default.Speed, null, Modifier.size(16.dp)) },
                  label = { Text("${playbackSpeed}x") },
                  modifier = Modifier.testTag("player_speed_chip"),
                )
              }
              item {
                val sleepLabel =
                  if (sleepRemainingSec > 0) "Sleep %02d:%02d".format(sleepRemainingSec / 60, sleepRemainingSec % 60)
                  else "Sleep Timer"
                FilterChip(
                  selected = sleepRemainingSec > 0,
                  onClick = {
                    val nextMin =
                      when {
                        sleepRemainingSec <= 0 -> 15
                        sleepRemainingSec > 45 * 60 -> 0
                        sleepRemainingSec > 20 * 60 -> 60
                        else -> 30
                      }
                    socialVm.setSleepTimerMinutes(nextMin)
                  },
                  leadingIcon = { Icon(Icons.Default.Timer, null, Modifier.size(16.dp)) },
                  label = { Text(sleepLabel) },
                  modifier = Modifier.testTag("player_sleep_chip"),
                )
              }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = { socialVm.voltVideo(video.videoId) },
                modifier = Modifier.weight(1f).testTag("player_volt_button"),
              ) {
                Icon(Icons.Default.ElectricBolt, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Volt ($volts)", maxLines = 1, softWrap = false)
              }
              OutlinedButton(
                onClick = { activeUser?.uid?.let { socialVm.toggleSaveVideo(it, video) } },
                modifier = Modifier.weight(1f).testTag("player_save_vault_button"),
              ) {
                Icon(if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (isSaved) "Saved" else "Save Vault", maxLines = 1, softWrap = false)
              }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = { openInYouTubeApp(context, video.videoId) },
                modifier = Modifier.weight(1.3f).testTag("open_in_youtube_app_button"),
              ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("YouTube App", maxLines = 1, softWrap = false)
              }
              OutlinedButton(
                onClick = {
                  val send =
                    android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                      type = "text/plain"
                      putExtra(android.content.Intent.EXTRA_SUBJECT, video.title)
                      putExtra(android.content.Intent.EXTRA_TEXT, "${video.title}\nhttps://youtu.be/${video.videoId}")
                    }
                  runCatching { context.startActivity(android.content.Intent.createChooser(send, "Share PEV Video")) }
                },
                modifier = Modifier.weight(1f).testTag("player_share_video_button"),
              ) {
                Icon(Icons.Default.Share, null, Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("Share", maxLines = 1, softWrap = false)
              }
            }
          }
        }

        if (queue.size > 1) {
          item {
            TacticalCard {
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("UP NEXT IN QUEUE (${queue.size})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                TextButton(
                  onClick = { socialVm.clearQueue() },
                  modifier = Modifier.testTag("clear_player_queue_button"),
                ) {
                  Icon(Icons.Default.DeleteSweep, null, Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Clear")
                }
              }
              Spacer(Modifier.height(6.dp))
              queue.take(8).forEach { track ->
                val isCurrent = track.videoId == video.videoId
                Surface(
                  onClick = { socialVm.playInMiniPlayer(track, queue, expandTheater = true) },
                  color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                  shape = MaterialTheme.shapes.small,
                  modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                ) {
                  Row(Modifier.padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                      Text(track.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                      Text("${track.channelTitle} • ${track.durationLabel}", style = MaterialTheme.typography.labelSmall)
                    }
                    if (isCurrent) {
                      Text("PLAYING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 6.dp))
                    } else {
                      IconButton(onClick = { socialVm.removeFromQueue(track.videoId) }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove from queue", modifier = Modifier.size(16.dp))
                      }
                    }
                  }
                }
              }
            }
          }
        }

        item {
          TacticalCard(highlight = true) {
            Text(
              "GEMINI FLASH-LITE VIDEO INTERROGATION",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
              value = aiQuestion,
              onValueChange = { aiQuestion = it },
              label = { Text("Ask AI about specs, teardown, or track details...") },
              modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Button(
              onClick = {
                if (aiQuestion.isNotBlank()) {
                  socialVm.askVideoAi(video, aiQuestion, "PEV Engineer", "Technical", settings.aiModelId)
                }
              },
              enabled = !aiBusy,
            ) {
              if (aiBusy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
              else {
                Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Interrogate Video")
              }
            }
            if (aiAnswer.isNotBlank()) {
              Spacer(Modifier.height(6.dp))
              Text(aiAnswer, style = MaterialTheme.typography.bodyMedium)
            }
          }
        }

        item {
          Text(
            "COMMUNITY VIDEO COMMENTS (${comments.size})",
            style = MaterialTheme.typography.labelMedium,
            color = theaterHeaderTextColor,
          )
          Spacer(Modifier.height(4.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
              value = commentText,
              onValueChange = { commentText = it },
              label = { Text("Post comment...") },
              modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(6.dp))
            Button(onClick = {
              if (commentText.isNotBlank()) {
                socialVm.postVideoComment(video.videoId, callsign, commentText)
                commentText = ""
              }
            }) { Text("Post") }
          }
        }

        items(comments, key = { it.id }) { c ->
          TacticalCard {
            Text(c.authorCallsign, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(c.text, style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
    }
  } else {
    val barModifier =
      if (isClassic) {
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 4.dp)
          .background(MaterialTheme.colorScheme.surface)
          .windowsClassicBevel(sunken = false)
          .clickable { socialVm.togglePlayerExpanded(true) }
          .testTag("youtube_mini_player_bar")
      } else {
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 6.dp)
          .clickable { socialVm.togglePlayerExpanded(true) }
          .testTag("youtube_mini_player_bar")
      }
    val subStatus =
      when {
        playerError.isNotBlank() -> playerError
        sleepRemainingSec > 0 -> "${video.channelTitle} • ${playbackSpeed}x • Sleep ${sleepRemainingSec / 60}m"
        else -> "${video.channelTitle} • ${playbackSpeed}x • In-App Player"
      }

    Surface(
      color = if (isClassic) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
      contentColor = miniBarTitleColor,
      border = if (isClassic) null else BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
      shape = if (isClassic) RoundedCornerShape(0.dp) else RoundedCornerShape(14.dp),
      shadowElevation = if (isClassic) 0.dp else 8.dp,
      modifier = barModifier,
    ) {
      Column(Modifier.fillMaxWidth()) {
        if (isClassic && settings.showClassicTitleBars) {
          ClassicWindowTitleBar(
            title = "MINI_PLAYER.EXE",
            trailingBadge = if (playing) "PLAY" else "PAUSE",
            active = true,
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth().padding(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier
                .width(128.dp)
                .height(72.dp)
                .clip(RoundedCornerShape(if (isClassic) 0.dp else 8.dp))
                .clipToBounds()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
          ) {
            AsyncImage(
              model = video.thumbnailUrl.ifBlank { "https://i.ytimg.com/vi/${video.videoId}/hqdefault.jpg" },
              contentDescription = video.title,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
            persistentYouTubePlayer(Modifier.fillMaxSize())
            if (!videoFrameReady) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
          Spacer(Modifier.width(10.dp))
          Column(Modifier.weight(1f)) {
            Text(
              text = video.title,
              style = MaterialTheme.typography.labelLarge,
              color = miniBarTitleColor,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = subStatus,
              style = MaterialTheme.typography.labelSmall,
              color = if (playerError.isNotBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (playerError.isNotBlank()) {
              Text(
                text = "Tap to open in YouTube App",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clickable { openInYouTubeApp(context, video.videoId) },
              )
            }
          }
          Surface(
            onClick = { socialVm.cyclePlayerSpeed() },
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("mini_player_speed_cycle_chip"),
          ) {
            Text(
              text = "${if (playbackSpeed % 1f == 0f) playbackSpeed.toInt().toString() else "%.2g".format(playbackSpeed)}x",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Black,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            )
          }
          TacticalIconButton(
            onClick = { miniSearchOpen = !miniSearchOpen },
            modifier = Modifier.testTag("mini_player_search_button"),
          ) {
            Icon(
              Icons.Default.Search,
              contentDescription = "Search Music in Mini-Player",
              tint = if (miniSearchOpen) MaterialTheme.colorScheme.primary else miniBarTitleColor,
            )
          }
          TacticalIconButton(
            onClick = { socialVm.togglePlayerPlaying() },
            modifier = Modifier.testTag("mini_player_play_pause_button"),
          ) {
            Icon(
              if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = if (playing) "Pause" else "Play",
              tint = miniBarTitleColor,
            )
          }
          if (queue.size > 1) {
            TacticalIconButton(onClick = { socialVm.playNextInQueue() }) {
              Icon(Icons.Default.SkipNext, contentDescription = "Next Track", tint = miniBarTitleColor)
            }
          }
          TacticalIconButton(onClick = { socialVm.togglePlayerExpanded(true) }) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Expand Player", tint = miniBarTitleColor)
          }
          TacticalIconButton(
            onClick = { socialVm.closeMiniPlayer() },
            modifier = Modifier.testTag("mini_player_close_button"),
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close Mini-Player", tint = miniBarTitleColor)
          }
        }
        if (miniSearchOpen) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              OutlinedTextField(
                value = miniSearchQuery,
                onValueChange = { miniSearchQuery = it },
                placeholder = { Text("Search music, artists, or videos...") },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("mini_player_search_input"),
              )
              Button(
                onClick = {
                  val q = miniSearchQuery.trim()
                  if (q.isNotBlank()) socialVm.loadMusicMixes("Custom: $q", customQuery = q)
                },
                enabled = miniSearchQuery.isNotBlank(),
                modifier = Modifier.testTag("mini_player_search_go_button"),
              ) { Text("Go") }
            }
            when (val st = mixesState) {
              is com.example.data.UiState.Loading -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                  CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                }
              }
              is com.example.data.UiState.Error -> {
                Text(st.message, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
              }
              is com.example.data.UiState.Success -> {
                val results = st.data.first
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  items(results.take(10), key = { it.videoId }) { track ->
                    Surface(
                      onClick = {
                        socialVm.playInMiniPlayer(track, results, expandTheater = false)
                        miniSearchOpen = false
                      },
                      color = MaterialTheme.colorScheme.surfaceVariant,
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.width(176.dp),
                    ) {
                      Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                          Text(track.title, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                          Text(track.channelTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        IconButton(
                          onClick = { socialVm.enqueuePlayNext(track) },
                          modifier = Modifier.size(48.dp),
                        ) {
                          Icon(Icons.Default.QueueMusic, contentDescription = "Queue Next", modifier = Modifier.size(14.dp))
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

private fun sanitizeYoutubeVideoId(videoId: String): String =
  videoId.replace(Regex("[^a-zA-Z0-9_\\-]"), "")

private fun mapPlaybackRate(speed: Float): PlayerConstants.PlaybackRate =
  when {
    speed <= 0.6f -> PlayerConstants.PlaybackRate.RATE_0_5
    speed <= 0.85f -> PlayerConstants.PlaybackRate.RATE_0_75
    speed <= 1.15f -> PlayerConstants.PlaybackRate.RATE_1
    speed <= 1.35f -> PlayerConstants.PlaybackRate.RATE_1_25
    speed <= 1.75f -> PlayerConstants.PlaybackRate.RATE_1_5
    else -> PlayerConstants.PlaybackRate.RATE_2
  }

private fun openInYouTubeApp(context: Context, videoId: String) {
  val safeId = sanitizeYoutubeVideoId(videoId)
  if (safeId.isBlank()) return
  val watchUri = Uri.parse("https://www.youtube.com/watch?v=$safeId")
  val intent = Intent(Intent.ACTION_VIEW, watchUri).apply {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  }
  runCatching { context.startActivity(intent) }
}

internal fun youtubeEmbedOriginForPackage(packageName: String): String {
  val cleanPkg = packageName.trim().ifBlank { "com.aistudio.volttrail.kqrmvx" }
  return "https://$cleanPkg"
}

internal fun buildYoutubeIframeHtml(
  videoId: String,
  origin: String = youtubeEmbedOriginForPackage("com.aistudio.volttrail.kqrmvx"),
): String {
  val safeId = sanitizeYoutubeVideoId(videoId)
  val encodedOrigin = URLEncoder.encode(origin, "UTF-8")
  val iframeSrc =
    "https://www.youtube.com/embed/$safeId?enablejsapi=1&playsinline=1&rel=0&modestbranding=1&fs=0&origin=$encodedOrigin&widget_referrer=$encodedOrigin"
  return """
    <!DOCTYPE html>
    <html>
      <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
        <meta name="referrer" content="strict-origin-when-cross-origin">
        <style>
          html, body { margin: 0; padding: 0; width: 100%; height: 100%; background: #000; overflow: hidden; }
          #player { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; border: 0; }
        </style>
      </head>
      <body>
        <iframe
          id="player"
          src="$iframeSrc"
          frameborder="0"
          referrerpolicy="strict-origin-when-cross-origin"
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
          allowfullscreen>
        </iframe>
        <script src="https://www.youtube.com/iframe_api"></script>
        <script>
          var ytPlayer;
          function onYouTubeIframeAPIReady() {
            ytPlayer = new YT.Player('player', {
              events: {
                'onReady': function(e) {
                  if (e.target && e.target.playVideo) e.target.playVideo();
                },
                'onStateChange': function(e) {
                  if (window.AndroidPlayer) AndroidPlayer.onStateChanged(e.data);
                },
                'onError': function(e) {
                  if (window.AndroidPlayer) AndroidPlayer.onPlayerError(e.data);
                }
              }
            });
          }
        </script>
      </body>
    </html>
  """.trimIndent()
}
