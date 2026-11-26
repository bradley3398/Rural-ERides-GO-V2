package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BoardPost
import com.example.data.MasterSettings
import com.example.data.PublicPilotProfile
import com.example.data.RiderMatchEngine
import com.example.data.RiderPaceTier
import com.example.data.RiderTerrainPref
import com.example.hardware.VoiceAudioManager
import com.example.ui.components.PilotAvatarThumb
import com.example.ui.components.SocialShareDialog
import com.example.ui.components.SocialSharePayload
import com.example.ui.components.TacticalButton
import com.example.ui.components.TacticalCard
import com.example.ui.components.TacticalDialog
import com.example.ui.components.TacticalOutlinedButton
import com.example.ui.components.dispatchSocialShare
import com.example.ui.compartments.OverlayCompartment
import com.example.ui.compartments.PrimaryTabCompartment
import com.example.viewmodel.CockpitViewModel
import com.example.viewmodel.CopilotSocialViewModel
import com.example.viewmodel.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardScreen(
  sessionVm: SessionViewModel,
  cockpitVm: CockpitViewModel,
  socialVm: CopilotSocialViewModel,
  onSelectTab: (PrimaryTabCompartment) -> Unit = {},
  onOpenOverlay: (OverlayCompartment) -> Unit = {},
  onOpen3dLab: () -> Unit = {},
) {
  val ctx = LocalContext.current
  val activeUser by sessionVm.activeUser.collectAsStateWithLifecycle()
  val profile by sessionVm.profile.collectAsStateWithLifecycle()
  val settings by sessionVm.settings.collectAsStateWithLifecycle()
  val pilots by sessionVm.publicPilots.collectAsStateWithLifecycle()
  val telemetry by cockpitVm.telemetry.collectAsStateWithLifecycle()
  val vehicles by cockpitVm.vehicles.collectAsStateWithLifecycle()

  val posts by socialVm.boardPosts.collectAsStateWithLifecycle()
  val comments by socialVm.selectedPostComments.collectAsStateWithLifecycle()
  val bookmarkedIds by socialVm.bookmarkedIds.collectAsStateWithLifecycle()
  val mutedPeerIds by socialVm.mutedPeerIds.collectAsStateWithLifecycle()
  val followedPilotIds by socialVm.followedPilotIds.collectAsStateWithLifecycle()
  val sosTriageMap by socialVm.sosTriageMap.collectAsStateWithLifecycle()
  val sosTriageBusyId by socialVm.sosTriageBusyId.collectAsStateWithLifecycle()
  val socialNotice by socialVm.socialNotice.collectAsStateWithLifecycle()
  val activeVideo by socialVm.activeVideo.collectAsStateWithLifecycle()

  // 0=Live Feed, 1=Following, 2=Social 100+, 3=Saved, 4=Standings
  var activeSubTab by remember { mutableIntStateOf(0) }
  var selectedCategory by remember(settings.defaultFeedCategory) { mutableStateOf(settings.defaultFeedCategory) }
  var sortMode by remember { mutableStateOf("Newest") }
  var maxMarketPrice by remember { mutableDoubleStateOf(0.0) }
  var urgentOnly by remember { mutableStateOf(false) }
  var hasPollOnly by remember { mutableStateOf(false) }
  var withPhotoOnly by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var showSearchBar by remember { mutableStateOf(false) }
  var showFilterPanel by remember { mutableStateOf(false) }

  var showComposer by remember { mutableStateOf(false) }
  var composerCat by remember { mutableStateOf("UNIVERSAL") }
  var composerTitle by remember { mutableStateOf("") }
  var composerBody by remember { mutableStateOf("") }
  var composerAttachTel by remember { mutableStateOf(false) }
  var composerHelp by remember { mutableStateOf(false) }
  var composerStolen by remember { mutableStateOf(false) }

  var editingPost by remember { mutableStateOf<BoardPost?>(null) }
  var showSlangDecoder by remember { mutableStateOf(false) }
  var showDms by remember { mutableStateOf(false) }
  var dmInitialPeer by remember { mutableStateOf<PublicPilotProfile?>(null) }
  val requestedDm by socialVm.openDmSheetRequested.collectAsStateWithLifecycle()
  LaunchedEffect(requestedDm) {
    if (requestedDm != null) {
      showDms = true
      socialVm.clearOpenDmSheetRequest()
    }
  }
  var activeCommentPost by remember { mutableStateOf<BoardPost?>(null) }
  var inspectedPilot by remember { mutableStateOf<PublicPilotProfile?>(null) }
  var lightboxImageUrl by remember { mutableStateOf<String?>(null) }
  var translatedPosts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
  var socialSharePayload by remember { mutableStateOf<SocialSharePayload?>(null) }
  var localNotice by remember { mutableStateOf("") }
  var showFiftyFeatures by remember { mutableStateOf(false) }
  var activeStory by remember { mutableStateOf<TimelineStoryItem?>(null) }

  val feedListState = rememberLazyListState()
  val fabExpanded by remember { derivedStateOf { feedListState.firstVisibleItemIndex == 0 } }

  val myUid = activeUser?.uid.orEmpty()
  val cloudSyncReady = activeUser != null
  val callsign = profile?.callsign?.ifBlank { "PILOT" } ?: "PILOT"
  val myAvatar = remember(pilots, myUid) { pilots.firstOrNull { it.userId == myUid }?.avatarBase64.orEmpty() }
  val activeVehicle = vehicles.firstOrNull { it.id == profile?.activeVehicleId } ?: vehicles.firstOrNull()
  val activeVehicleSummary =
    activeVehicle?.let { "${it.name} (${it.category} • ${it.voltage.toInt()}V • ${it.batteryWh}Wh)" }.orEmpty()
  val liveTelemetrySummary =
    "Live Speed ${"%.1f".format(com.example.ui.components.formatSpeedValue(telemetry.speedKmh, settings.distanceUnit))} ${com.example.ui.components.speedUnitLabel(settings.distanceUnit)} • Trip ${"%.2f".format(com.example.ui.components.formatDistValue(telemetry.tripDistanceKm, settings.distanceUnit))} ${com.example.ui.components.distUnitLabel(settings.distanceUnit)} • Bat ${telemetry.batteryPct}%"

  LaunchedEffect(
    activeUser?.uid,
    activeVehicle?.name,
    activeVehicle?.batteryWh,
    settings.matchPaceTier,
    settings.matchTerrainPref,
    settings.matchMinRangeMi,
    settings.matchScheduleVibe,
    settings.matchHomeZipOrTown,
  ) {
    if (activeUser != null) {
      sessionVm.syncPilotMatchProfile(
        vehicleName = activeVehicle?.name ?: "PEV Rig",
        batteryWh = activeVehicle?.batteryWh ?: 1500L,
        matchSettings = settings,
      )
    }
  }

  val categories = remember { listOf("ALL") + LIVE_BOARD_CATEGORIES }
  val pilotsByUid = remember(pilots) { pilots.associateBy { it.userId } }

  val urgentPosts =
    remember(posts) {
      posts.filter { it.helpNeeded || it.speedTrap || it.stolenAlert || it.category == "SOS" || it.category == "STOLEN" }
    }
  val marketCount = remember(posts) { posts.count { it.category == "MARKETPLACE" } }

  val activeFilterCount =
    remember(selectedCategory, sortMode, urgentOnly, hasPollOnly, withPhotoOnly, maxMarketPrice) {
      var c = 0
      if (selectedCategory != "ALL") c++
      if (sortMode != "Newest") c++
      if (urgentOnly) c++
      if (hasPollOnly) c++
      if (withPhotoOnly) c++
      if (maxMarketPrice > 0.0) c++
      c
    }

  val filteredPosts =
    remember(
      posts,
      selectedCategory,
      searchQuery,
      sortMode,
      maxMarketPrice,
      urgentOnly,
      hasPollOnly,
      withPhotoOnly,
      mutedPeerIds,
      followedPilotIds,
      activeSubTab,
      bookmarkedIds,
      settings.boardPostLimit,
    ) {
      posts
        .filter { p ->
          !mutedPeerIds.contains(p.userId) &&
            (activeSubTab != 1 || followedPilotIds.contains(p.userId)) &&
            (activeSubTab != 3 || bookmarkedIds.contains(p.id)) &&
            (selectedCategory == "ALL" || p.category == selectedCategory) &&
            (!urgentOnly || p.helpNeeded || p.speedTrap || p.stolenAlert || p.category == "SOS" || p.category == "STOLEN") &&
            (!hasPollOnly || p.pollQuestion.isNotBlank()) &&
            (!withPhotoOnly || p.imageUrl.isNotBlank()) &&
            (maxMarketPrice <= 0.0 || (p.priceUsd > 0.0 && p.priceUsd <= maxMarketPrice)) &&
            (searchQuery.isBlank() ||
              p.title.contains(searchQuery, true) ||
              p.body.contains(searchQuery, true) ||
              p.authorCallsign.contains(searchQuery, true) ||
              p.locationLabel.contains(searchQuery, true))
        }
        .let { list ->
          when (sortMode) {
            "Most Volted" -> list.sortedByDescending { it.likesCount }
            "Most Discussed" -> list.sortedByDescending { it.commentsCount }
            else -> list
          }
        }
        .take(settings.boardPostLimit.coerceAtLeast(25))
    }

  val savedCount = remember(posts, bookmarkedIds) { posts.count { bookmarkedIds.contains(it.id) } }
  val followingPostCount = remember(posts, followedPilotIds) { posts.count { followedPilotIds.contains(it.userId) } }

  fun launchComposer(
    cat: String = if (selectedCategory == "ALL") "UNIVERSAL" else selectedCategory,
    title: String = "",
    body: String = "",
    attachTel: Boolean = false,
    help: Boolean = false,
    stolen: Boolean = false,
  ) {
    composerCat = cat
    composerTitle = title
    composerBody = body
    composerAttachTel = attachTel
    composerHelp = help
    composerStolen = stolen
    showComposer = true
  }

  fun playCue(cue: VoiceAudioManager.TouchCue) {
    sessionVm.voiceAudio.playTouchSound(
      enabled = settings.touchSoundsEnabled,
      pack = settings.touchSoundPack,
      cue = cue,
      volumePct = settings.soundVolumePct,
    )
  }

  Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
      // 1. CLEAN SPACIOUS TOP HEADER — TITLE NEVER TRUNCATES, ZERO OVERLAP
      Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          Box(
            modifier =
              Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (cloudSyncReady) Color(0xFF22C55E) else MaterialTheme.colorScheme.primary),
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "PORCH TALK",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${posts.size} live posts • ${pilots.size} riders",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Surface(
            onClick = { launchComposer() },
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.heightIn(min = 42.dp).testTag("compose_board_post_fab"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(Icons.Default.Add, contentDescription = "New Live Transmission", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
              Text("Post", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary, maxLines = 1, softWrap = false)
            }
          }

          Surface(
            onClick = {
              dmInitialPeer = null
              showDms = true
            },
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
            modifier = Modifier.heightIn(min = 42.dp).testTag("open_dms_button"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(Icons.Default.Mail, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
              Text("DMs", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, maxLines = 1, softWrap = false)
            }
          }
        }
      }

      // 1B. CLEAN UTILITY ACTION STRIP (SEARCH, FILTER, SLANG, REFRESH — NO CRUSHING)
      LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (activeSubTab == 0 || activeSubTab == 1 || activeSubTab == 3) {
          item {
            FilterChip(
              selected = showSearchBar || searchQuery.isNotBlank(),
              onClick = { showSearchBar = !showSearchBar },
              leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(15.dp)) },
              label = { Text("Search", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
              modifier = Modifier.heightIn(min = 40.dp).testTag("toggle_board_search_button"),
            )
          }
          item {
            FilterChip(
              selected = showFilterPanel || activeFilterCount > 0,
              onClick = { showFilterPanel = !showFilterPanel },
              leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(15.dp)) },
              label = {
                Text(
                  if (activeFilterCount > 0) "Filters ($activeFilterCount)" else "Filters",
                  style = MaterialTheme.typography.labelSmall,
                  maxLines = 1,
                  softWrap = false,
                )
              },
              modifier = Modifier.heightIn(min = 40.dp).testTag("toggle_board_filters_button"),
            )
          }
        }
        item {
          FilterChip(
            selected = showSlangDecoder,
            onClick = { showSlangDecoder = true },
            leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(15.dp)) },
            label = { Text("PEV Slang", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
            modifier = Modifier.heightIn(min = 40.dp).testTag("open_slang_decoder_button"),
          )
        }
        item {
          FilterChip(
            selected = false,
            onClick = { socialVm.refreshSupabaseBoard() },
            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp)) },
            label = { Text("Refresh", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
            modifier = Modifier.heightIn(min = 40.dp).testTag("refresh_live_board_button"),
          )
        }
      }

      // 2. COMPACT PILL NAVIGATION STRIP (ONLY ~40dp)
      LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        item {
          BoardTabPill(
            label = "Porch Feed (${posts.size})",
            icon = Icons.Default.DynamicFeed,
            selected = activeSubTab == 0,
            onClick = { playCue(VoiceAudioManager.TouchCue.TAB_SWITCH); activeSubTab = 0 },
          )
        }
        item {
          BoardTabPill(
            label = "Trail Crew ($followingPostCount)",
            icon = Icons.Default.Group,
            selected = activeSubTab == 1,
            onClick = { playCue(VoiceAudioManager.TouchCue.TAB_SWITCH); activeSubTab = 1 },
          )
        }
        item {
          BoardTabPill(
            label = "Saved ($savedCount)",
            icon = Icons.Default.Bookmark,
            selected = activeSubTab == 3,
            onClick = { playCue(VoiceAudioManager.TouchCue.TAB_SWITCH); activeSubTab = 3 },
          )
        }
        item {
          BoardTabPill(
            label = "Top Riders",
            icon = Icons.Default.EmojiEvents,
            selected = activeSubTab == 4,
            onClick = { playCue(VoiceAudioManager.TouchCue.TAB_SWITCH); activeSubTab = 4 },
          )
        }
        item {
          BoardTabPill(
            label = "Barn Tools",
            icon = Icons.Default.Hub,
            selected = activeSubTab == 2,
            onClick = { playCue(VoiceAudioManager.TouchCue.TAB_SWITCH); activeSubTab = 2 },
          )
        }
      }

      // COLLAPSIBLE SEARCH BAR (HIDDEN BY DEFAULT FOR MAXIMUM FEED AREA)
      AnimatedVisibility(visible = (showSearchBar || searchQuery.isNotBlank()) && (activeSubTab == 0 || activeSubTab == 1 || activeSubTab == 3)) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          leadingIcon = { Icon(Icons.Default.Search, null) },
          trailingIcon = {
            IconButton(onClick = {
              searchQuery = ""
              showSearchBar = false
            }) { Icon(Icons.Default.Close, "Close Search") }
          },
          placeholder = { Text("Search posts, #hashtags, callsigns, parts...") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp).testTag("board_search_input"),
        )
      }

      // COLLAPSIBLE SORT & FILTER DRAWER (HIDDEN BY DEFAULT)
      AnimatedVisibility(visible = showFilterPanel && (activeSubTab == 0 || activeSubTab == 1 || activeSubTab == 3)) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text("FEED SORT & FILTERS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
              if (activeFilterCount > 0) {
                TextButton(
                  onClick = {
                    selectedCategory = "ALL"
                    sortMode = "Newest"
                    urgentOnly = false
                    hasPollOnly = false
                    withPhotoOnly = false
                    maxMarketPrice = 0.0
                  },
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) { Text("Reset All") }
              }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(listOf("Newest", "Most Volted", "Most Discussed")) { sm ->
                FilterChip(selected = sortMode == sm, onClick = { sortMode = sm }, label = { Text(sm) })
              }
              item { FilterChip(selected = urgentOnly, onClick = { urgentOnly = !urgentOnly }, label = { Text("SOS/Alerts") }) }
              item { FilterChip(selected = hasPollOnly, onClick = { hasPollOnly = !hasPollOnly }, label = { Text("Polls") }) }
              item { FilterChip(selected = withPhotoOnly, onClick = { withPhotoOnly = !withPhotoOnly }, label = { Text("Photos") }) }
              if (selectedCategory == "MARKETPLACE" || marketCount > 0) {
                items(listOf(0.0 to "Any $", 500.0 to "<$500", 1500.0 to "<$1500", 3000.0 to "<$3000")) { (cap, lbl) ->
                  FilterChip(selected = maxMarketPrice == cap, onClick = { maxMarketPrice = cap }, label = { Text(lbl) })
                }
              }
            }
          }
        }
      }

      val trendingTags =
        remember(posts) {
          val rx = Regex("#[A-Za-z0-9_]{2,28}")
          posts
            .flatMap { p -> rx.findAll("${p.title} ${p.body}").map { it.value }.distinct().toList() }
            .groupingBy { it.lowercase() }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(8)
            .map { it.key to it.value }
        }
      val activeCrew =
        remember(pilots, myUid) {
          pilots
            .filter { p -> p.userId.isNotBlank() }
            .distinctBy { it.userId }
            .sortedWith(compareByDescending<PublicPilotProfile> { it.userId != myUid }.thenByDescending { it.totalKm })
            .take(25)
        }

      // 3. MAIN FULL-VIEWPORT CONTENT (POST FEED OWNS 85-90% OF SCREEN!)
      when (activeSubTab) {
        2 -> {
          BoardSocialHubTab(
            posts = posts,
            pilots = pilots,
            followedPilotIds = followedPilotIds,
            myUid = myUid,
            myCallsign = callsign,
            cloudSyncReady = cloudSyncReady,
            settings = settings,
            telemetrySummary = liveTelemetrySummary,
            aiModelId = settings.aiModelId,
            socialVm = socialVm,
            onOpenComposer = { cat, title, body, attachTel, help, stolen ->
              launchComposer(cat, title, body, attachTel, help, stolen)
            },
            onApplyFeedAction = { act ->
              when {
                act.startsWith("SORT:") -> {
                  sortMode = act.removePrefix("SORT:")
                  activeSubTab = 0
                }
                act.startsWith("CAT:") -> {
                  selectedCategory = act.removePrefix("CAT:")
                  urgentOnly = false
                  activeSubTab = 0
                }
                act.startsWith("PRICE:") -> {
                  selectedCategory = "MARKETPLACE"
                  maxMarketPrice = act.removePrefix("PRICE:").toDoubleOrNull() ?: 0.0
                  activeSubTab = 0
                }
                act.startsWith("SEARCH:") -> {
                  searchQuery = act.removePrefix("SEARCH:")
                  showSearchBar = true
                  activeSubTab = 0
                }
                act == "FILTER:POLLS" -> {
                  hasPollOnly = true
                  activeSubTab = 0
                }
                act == "FILTER:PHOTOS" -> {
                  withPhotoOnly = true
                  activeSubTab = 0
                }
                act == "FILTER:URGENT" -> {
                  urgentOnly = true
                  activeSubTab = 0
                }
                act == "TAB:FOLLOWING" -> activeSubTab = 1
                act == "TAB:SAVED" -> activeSubTab = 3
                act == "TAB:STANDINGS" -> activeSubTab = 4
                act == "TAB:FEED" -> activeSubTab = 0
                act == "SLANG" -> showSlangDecoder = true
                else -> activeSubTab = 0
              }
            },
            onOpenDmWithPilot = { peer ->
              dmInitialPeer = peer
              showDms = true
            },
            onOpenShareDialog = { socialSharePayload = it },
            onDirectPlatformShare = { plat, text ->
              dispatchSocialShare(ctx = ctx, platform = plat, title = "Rural ERides GO", fullText = text, url = "")
            },
            onOpenFiftyFeatures = { showFiftyFeatures = true },
            onInviteLocalRiders = {
              val msg =
                RiderMatchEngine.buildLocalRiderInviteMessage(
                  callsign = callsign,
                  sectorOrZip = settings.matchHomeZipOrTown.ifBlank { settings.customWeatherCity },
                  vehicleName = activeVehicle?.name ?: "PEV Rig",
                  pace = settings.matchPaceTier,
                  terrain = settings.matchTerrainPref,
                  usableRangeMi = settings.matchMinRangeMi,
                  vibe = settings.matchScheduleVibe,
                  lat = telemetry.lat,
                  lng = telemetry.lng,
                )
              RiderMatchEngine.launchShareInvite(ctx, "Ride with @$callsign on Rural ERides GO", msg)
            },
          )
        }

        4 -> {
          val activeRealPilots =
            remember(pilots, myUid) {
              pilots
                .filter { p -> p.userId.isNotBlank() }
                .distinctBy { it.userId }
            }
          BoardStandingsTab(
            pilots = activeRealPilots,
            myUid = myUid,
            settings = settings,
            followedPilotIds = followedPilotIds,
            bottomPad = if (activeVideo != null) 156.dp else 96.dp,
            onInspectPilot = { inspectedPilot = it },
            onToggleFollow = { uid -> socialVm.toggleFollowPilot(myUid, uid) },
            onInviteLocalRiders = {
              val msg =
                RiderMatchEngine.buildLocalRiderInviteMessage(
                  callsign = callsign,
                  sectorOrZip = settings.matchHomeZipOrTown.ifBlank { settings.customWeatherCity },
                  vehicleName = activeVehicle?.name ?: "PEV Rig",
                  pace = settings.matchPaceTier,
                  terrain = settings.matchTerrainPref,
                  usableRangeMi = settings.matchMinRangeMi,
                  vibe = settings.matchScheduleVibe,
                  lat = telemetry.lat,
                  lng = telemetry.lng,
                )
              RiderMatchEngine.launchShareInvite(ctx, "Ride with @$callsign on Rural ERides GO", msg)
            },
            onHostGroupRide = {
              launchComposer(
                cat = "GROUP_RIDES",
                title = "Public Group Ride • ${settings.matchPaceTier.label}",
                body =
                  "Open local convoy hosted by @$callsign!\n" +
                    "Pace: ${settings.matchPaceTier.label} | Terrain: ${settings.matchTerrainPref.label} | Min Pack: ${settings.matchMinRangeMi}mi | Vibe: ${settings.matchScheduleVibe.label}\n" +
                    "#GroupRide #RuralERidesGO",
                attachTel = true,
              )
            },
          )
        }

        else -> {
          LazyColumn(
            state = feedListState,
            modifier = Modifier.fillMaxSize().testTag("board_post_feed_list"),
            contentPadding = PaddingValues(top = 10.dp, bottom = if (activeVideo != null) 120.dp else 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            // NOTICE BANNER (SCROLLS WITH FEED)
            val bannerMsg = socialNotice.ifBlank { localNotice }
            if (bannerMsg.isNotBlank()) {
              item(key = "board_notice") {
                TacticalCard(highlight = true, modifier = Modifier.padding(horizontal = 14.dp)) {
                  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(bannerMsg, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                      socialVm.clearNotice()
                      localNotice = ""
                    }) { Text("OK") }
                  }
                }
              }
            }

            if (!cloudSyncReady) {
              item(key = "board_signin_notice") {
                TacticalCard(highlight = true, modifier = Modifier.padding(horizontal = 14.dp)) {
                  Text("Live social sync active — sign in via Settings to post, DM, or place WebRTC calls.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }

            // INLINE LIVE 24H TIMELINE STORIES + QUICK COMPOSER & 35 RURAL TEMPLATES (ONLY ON MAIN FEED)
            if (activeSubTab == 0) {
              item(key = "board_live_stories_bar") {
                val liveStories =
                  remember(posts, pilotsByUid) {
                    val cutoffSec = (System.currentTimeMillis() / 1000L) - 86_400L
                    posts
                      .filter { p ->
                        val createdSec = p.createdAt?.seconds ?: 0L
                        val isRecent = createdSec == 0L || createdSec >= cutoffSec
                        isRecent && (p.body.contains("#TrailStory", ignoreCase = true) || p.imageUrl.isNotBlank() || p.telemetrySummary.isNotBlank())
                      }
                      .take(15)
                      .map { p ->
                        TimelineStoryItem(
                          id = p.id,
                          userId = p.userId,
                          authorCallsign = p.authorCallsign.ifBlank { "PILOT" },
                          avatarBase64 = pilotsByUid[p.userId]?.avatarBase64.orEmpty(),
                          mediaUrl = p.imageUrl,
                          caption = p.body.replace("#TrailStory", "").trim().ifBlank { p.title },
                          batteryStatus = p.telemetrySummary,
                          timestampEpoch = p.createdAt?.seconds ?: (System.currentTimeMillis() / 1000L),
                        )
                      }
                  }
                TimelineStoriesBar(
                  myCallsign = callsign,
                  myAvatar = myAvatar,
                  stories = liveStories,
                  cloudSyncReady = cloudSyncReady,
                  onAddStory = { caption, photoBmp, mediaUrl ->
                    socialVm.createPost(
                      callsign = callsign,
                      badge = settings.pilotRankBadge,
                      category = "RIDE_REPORTS",
                      title = "Trail Story • @$callsign",
                      body = "$caption #TrailStory",
                      bitmap = photoBmp,
                      pollQuestion = "",
                      pollOptions = emptyList(),
                      priceUsd = 0.0,
                      location = telemetry.roadName,
                      telemetrySummary = liveTelemetrySummary,
                      helpNeeded = false,
                      speedTrap = false,
                      stolenAlert = false,
                      externalImageUrl = mediaUrl,
                      onDone = { localNotice = "Live 24h Trail Story published to Firestore!" },
                    )
                  },
                  onViewStory = { story -> activeStory = story },
                  modifier = Modifier.padding(horizontal = 14.dp),
                )
              }
              item(key = "board_rural_templates_strip") {
                BoardRuralTemplatesStrip(
                  onLaunchTemplate = { tpl ->
                    playCue(VoiceAudioManager.TouchCue.BUTTON_TAP)
                    launchComposer(
                      cat = tpl.category,
                      title = tpl.titleTemplate,
                      body = tpl.bodyTemplate,
                      attachTel = tpl.category == "RIDE_REPORTS" || tpl.category == "SOS",
                      help = tpl.category == "SOS",
                      stolen = tpl.category == "STOLEN",
                    )
                  },
                )
              }
              item(key = "quick_composer_strip") {
                TacticalCard(modifier = Modifier.padding(horizontal = 14.dp)) {
                  Row(
                    modifier =
                      Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .clickable { launchComposer() }
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                        .testTag("inline_composer_prompt"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    PilotAvatarThumb(avatarSource = myAvatar, callsign = callsign, size = 34.dp)
                    Text(
                      text = "What's happening on the trail, $callsign?",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f),
                    )
                  }
                  Spacer(Modifier.height(8.dp))
                  LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                      QuickComposeChip("Photo", Icons.Default.PhotoCamera, MaterialTheme.colorScheme.primary) {
                        launchComposer(cat = "GARAGE")
                      }
                    }
                    item {
                      QuickComposeChip("Ride Log", Icons.Default.Speed, MaterialTheme.colorScheme.secondary) {
                        launchComposer(cat = "RIDE_REPORTS", attachTel = true)
                      }
                    }
                    item {
                      QuickComposeChip("Poll", Icons.Default.Poll, MaterialTheme.colorScheme.tertiary) {
                        launchComposer(cat = "UNIVERSAL")
                      }
                    }
                    item {
                      QuickComposeChip("Sell Rig", Icons.Default.Sell, MaterialTheme.colorScheme.tertiary) {
                        launchComposer(cat = "MARKETPLACE")
                      }
                    }
                    item {
                      QuickComposeChip("SOS Alert", Icons.Default.Sos, MaterialTheme.colorScheme.error) {
                        launchComposer(cat = "SOS", attachTel = true, help = true)
                      }
                    }
                  }
                  Spacer(Modifier.height(6.dp))
                  // RURAL BACKROAD TRAIL CONDITIONS QUICK-POST / FILTER BAR
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                  ) {
                    Icon(
                      com.example.ui.components.RuralPevIcons.PineForest,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.secondary,
                      modifier = Modifier.size(14.dp),
                    )
                    Text(
                      "RURAL TRAIL REPORTS",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Black,
                      color = MaterialTheme.colorScheme.secondary,
                    )
                  }
                  Spacer(Modifier.height(4.dp))
                  val ruralTags =
                    remember {
                      listOf(
                        Triple("Muddy Trail", "RIDE_REPORTS", "#Muddy"),
                        Triple("Fresh Gravel", "RIDE_REPORTS", "#Gravel"),
                        Triple("Creek High", "ALERTS", "#Creek"),
                        Triple("Wildlife Seen", "ALERTS", "#Wildlife"),
                        Triple("Charger Live", "CHARGERS", "#Charger"),
                        Triple("Barn Build", "BUILDS", "#BarnBuild"),
                        Triple("Group Ride", "GROUP_RIDES", "#GroupRide"),
                      )
                    }
                  LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ruralTags) { (label, cat, tag) ->
                      val isFiltering = searchQuery.equals(tag, ignoreCase = true)
                      Surface(
                        onClick = {
                          playCue(VoiceAudioManager.TouchCue.BUTTON_TAP)
                          if (isFiltering) {
                            searchQuery = ""
                          } else {
                            launchComposer(
                              cat = cat,
                              title = "$label Report",
                              body = "$tag — Backroad update from $callsign: ",
                              attachTel = cat == "RIDE_REPORTS" || cat == "ALERTS" || cat == "CHARGERS",
                            )
                          }
                        },
                        color = if (isFiltering) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                        modifier = Modifier.testTag("rural_trail_tag_${label.lowercase().replace(" ", "_")}"),
                      ) {
                        Row(
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                          Icon(
                            com.example.ui.components.RuralPevIcons.forRuralCondition(label),
                            contentDescription = null,
                            tint = if (isFiltering) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(13.dp),
                          )
                          Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isFiltering) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                          )
                        }
                      }
                    }
                  }
                  Spacer(Modifier.height(6.dp))
                  // LIVE RURAL BOARD PULSE BAR (REAL COUNTS FROM FIRESTORE POSTS)
                  val chargerCount = remember(posts) { posts.count { it.category == "CHARGERS" || it.body.contains("#Charger", true) } }
                  val groupRideCount = remember(posts) { posts.count { it.category == "GROUP_RIDES" || it.category == "EVENTS" } }
                  val buildCount = remember(posts) { posts.count { it.category == "BUILDS" || it.category == "GARAGE" } }
                  LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                      FilterChip(
                        selected = selectedCategory == "CHARGERS",
                        onClick = { playCue(VoiceAudioManager.TouchCue.BUTTON_TAP); selectedCategory = if (selectedCategory == "CHARGERS") "ALL" else "CHARGERS" },
                        label = { Text("Chargers ($chargerCount)", maxLines = 1, softWrap = false) },
                      )
                    }
                    item {
                      FilterChip(
                        selected = selectedCategory == "GROUP_RIDES",
                        onClick = { playCue(VoiceAudioManager.TouchCue.BUTTON_TAP); selectedCategory = if (selectedCategory == "GROUP_RIDES") "ALL" else "GROUP_RIDES" },
                        label = { Text("Group Rides ($groupRideCount)", maxLines = 1, softWrap = false) },
                      )
                    }
                    item {
                      FilterChip(
                        selected = selectedCategory == "BUILDS",
                        onClick = { playCue(VoiceAudioManager.TouchCue.BUTTON_TAP); selectedCategory = if (selectedCategory == "BUILDS") "ALL" else "BUILDS" },
                        label = { Text("Barn Builds ($buildCount)", maxLines = 1, softWrap = false) },
                      )
                    }
                    item {
                      FilterChip(
                        selected = selectedCategory == "MARKETPLACE",
                        onClick = { playCue(VoiceAudioManager.TouchCue.BUTTON_TAP); selectedCategory = if (selectedCategory == "MARKETPLACE") "ALL" else "MARKETPLACE" },
                        label = { Text("Swap Meet ($marketCount)", maxLines = 1, softWrap = false) },
                      )
                    }
                  }
                }
              }

              // LIVE TRENDING #HASHTAGS & ACTIVE CREW STRIP (100% REAL FIRESTORE POSTS & PILOTS)
              if (trendingTags.isNotEmpty() || activeCrew.isNotEmpty()) {
                item(key = "board_trending_crew_strip") {
                  TacticalCard(modifier = Modifier.padding(horizontal = 14.dp).testTag("board_trending_crew_strip")) {
                    if (trendingTags.isNotEmpty()) {
                      Text(
                        "TRENDING LIVE TAGS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                      )
                      Spacer(Modifier.height(6.dp))
                      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(trendingTags) { (tag, count) ->
                          val activeTag = searchQuery.equals(tag, ignoreCase = true)
                          FilterChip(
                            selected = activeTag,
                            onClick = {
                              playCue(VoiceAudioManager.TouchCue.BUTTON_TAP)
                              searchQuery = if (activeTag) "" else tag
                              showSearchBar = searchQuery.isNotBlank()
                            },
                            label = { Text("$tag ($count)", maxLines = 1, softWrap = false) },
                          )
                        }
                      }
                    }
                    if (activeCrew.isNotEmpty()) {
                      if (trendingTags.isNotEmpty()) Spacer(Modifier.height(8.dp))
                      Text(
                        "ACTIVE TRAIL CREW — TAP TO FRIEND, DM OR CALL (${activeCrew.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary,
                      )
                      Spacer(Modifier.height(6.dp))
                      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(activeCrew, key = { "crew_${it.userId}" }) { pilot ->
                          val isMe = pilot.userId == myUid
                          val isFollowing = followedPilotIds.contains(pilot.userId)
                          val isOnline = pilot.isLiveOnline() || isMe
                          val distText = com.example.ui.components.formatDistanceLabel(pilot.totalKm, settings.distanceUnit)
                          Surface(
                            onClick = {
                              playCue(VoiceAudioManager.TouchCue.BUTTON_TAP)
                              inspectedPilot = pilot
                            },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                              if (isOnline) 1.2.dp else 0.8.dp,
                              when {
                                isOnline -> Color(0xFF22C55E)
                                isFollowing -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                              },
                            ),
                          ) {
                            Row(
                              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                              verticalAlignment = Alignment.CenterVertically,
                              horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                              PilotAvatarThumb(avatarSource = pilot.avatarBase64, callsign = pilot.callsign, size = 28.dp)
                              Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                  Box(
                                    Modifier
                                      .size(8.dp)
                                      .clip(CircleShape)
                                      .background(if (isOnline) Color(0xFF22C55E) else MaterialTheme.colorScheme.outlineVariant)
                                  )
                                  Text(
                                    if (isMe) "${pilot.callsign.ifBlank { "PILOT" }} (YOU)" else pilot.callsign.ifBlank { "PILOT" },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                  )
                                }
                                Text(
                                  "${if (isOnline) "ONLINE NOW" else "OFFLINE"} • ${pilot.ridesCount} rides • $distText",
                                  style = MaterialTheme.typography.labelSmall,
                                  color = if (isOnline) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurfaceVariant,
                                  fontWeight = if (isOnline) FontWeight.Bold else FontWeight.Normal,
                                  maxLines = 1,
                                  softWrap = false,
                                )
                              }
                              if (!isMe) {
                                Surface(
                                  onClick = {
                                    playCue(VoiceAudioManager.TouchCue.BUTTON_TAP)
                                    socialVm.toggleFollowPilot(myUid, pilot.userId, callsign, pilot.callsign)
                                  },
                                  color = if (isFollowing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                  shape = RoundedCornerShape(8.dp),
                                ) {
                                  Text(
                                    if (isFollowing) "✓ Friend" else "+ Friend",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFollowing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                  )
                                }
                                IconButton(
                                  onClick = {
                                    dmInitialPeer = pilot
                                    showDms = true
                                  },
                                  modifier = Modifier.size(28.dp),
                                ) {
                                  Icon(Icons.Default.Mail, contentDescription = "DM ${pilot.callsign}", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                  onClick = {
                                    socialVm.startOutgoingCall(myUid, callsign, pilot.userId, pilot.callsign, "AUDIO")
                                  },
                                  modifier = Modifier.size(28.dp),
                                ) {
                                  Icon(Icons.Default.Call, contentDescription = "Call ${pilot.callsign}", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
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
            } else if (activeSubTab == 1 && activeCrew.isNotEmpty()) {
              item(key = "board_trail_crew_directory") {
                TacticalCard(modifier = Modifier.padding(horizontal = 14.dp).testTag("board_trail_crew_directory")) {
                  val onlineCount = activeCrew.count { it.isLiveOnline() || it.userId == myUid }
                  Text(
                    "DISCOVER & FRIEND RIDERS (${activeCrew.size} REAL • $onlineCount ONLINE NOW)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                  )
                  Spacer(Modifier.height(6.dp))
                  LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(activeCrew, key = { "trail_crew_${it.userId}" }) { pilot ->
                      val isMe = pilot.userId == myUid
                      val isFollowing = followedPilotIds.contains(pilot.userId)
                      val isOnline = pilot.isLiveOnline() || isMe
                      Surface(
                        onClick = { inspectedPilot = pilot },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                          if (isOnline) 1.2.dp else 0.8.dp,
                          if (isOnline) Color(0xFF22C55E) else if (isFollowing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        ),
                      ) {
                        Row(
                          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                          PilotAvatarThumb(avatarSource = pilot.avatarBase64, callsign = pilot.callsign, size = 28.dp)
                          Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                              Box(
                                Modifier
                                  .size(8.dp)
                                  .clip(CircleShape)
                                  .background(if (isOnline) Color(0xFF22C55E) else MaterialTheme.colorScheme.outlineVariant)
                              )
                              Text(
                                if (isMe) "${pilot.callsign.ifBlank { "PILOT" }} (YOU)" else pilot.callsign.ifBlank { "PILOT" },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                              )
                            }
                            Text(
                              if (isOnline) "ONLINE • ${pilot.vehicleSummary.ifBlank { "PEV Pilot" }.take(18)}" else pilot.vehicleSummary.ifBlank { "PEV Pilot" }.take(22),
                              style = MaterialTheme.typography.labelSmall,
                              color = if (isOnline) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                          }
                          if (!isMe) {
                            Surface(
                              onClick = { socialVm.toggleFollowPilot(myUid, pilot.userId, callsign, pilot.callsign) },
                              color = if (isFollowing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                              shape = RoundedCornerShape(8.dp),
                            ) {
                              Text(
                                if (isFollowing) "✓ Friend" else "+ Friend",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isFollowing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                              )
                            }
                            IconButton(
                              onClick = {
                                dmInitialPeer = pilot
                                showDms = true
                              },
                              modifier = Modifier.size(28.dp),
                            ) {
                              Icon(Icons.Default.Mail, contentDescription = "DM ${pilot.callsign}", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }

            // URGENT SOS TICKER (SCROLLS WITH FEED)
            if (urgentPosts.isNotEmpty() && !urgentOnly && selectedCategory != "SOS" && activeSubTab == 0) {
              val topUrgent = urgentPosts.first()
              item(key = "urgent_sos_ticker") {
                Surface(
                  onClick = { urgentOnly = true },
                  color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                  shape = RoundedCornerShape(12.dp),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                      Icon(Icons.Default.Sos, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                      Spacer(Modifier.width(8.dp))
                      Text(
                        text = "${topUrgent.authorCallsign}: ${topUrgent.title}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("VIEW (${urgentPosts.size})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                  }
                }
              }
            }

            // SINGLE-ROW CATEGORY CHIPS (INSIDE LAZYCOLUMN SO THEY SCROLL AWAY CLEANLY!)
            item(key = "category_chips_row") {
              LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                items(categories) { cat ->
                  val selected = selectedCategory == cat
                  Surface(
                    onClick = { selectedCategory = cat },
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(20.dp),
                    border = if (selected) null else BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
                  ) {
                    Text(
                      text = cat.replace('_', ' '),
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                      color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                      softWrap = false,
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                  }
                }
              }
            }

            // EMPTY STATE
            if (filteredPosts.isEmpty()) {
              item(key = "empty_feed_state") {
                TacticalCard(modifier = Modifier.padding(horizontal = 14.dp)) {
                  Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Icon(Icons.Default.DynamicFeed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    Text(
                      text =
                        when (activeSubTab) {
                          1 -> "Your Following feed is quiet. Follow verified riders on posts or Standings to see their updates here."
                          3 -> "No saved posts yet. Tap the bookmark icon on any post to save it here."
                          else -> "No live transmissions match this filter yet."
                        },
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (activeFilterCount > 0 || searchQuery.isNotBlank()) {
                      OutlinedButton(onClick = {
                        selectedCategory = "ALL"
                        sortMode = "Newest"
                        urgentOnly = false
                        hasPollOnly = false
                        withPhotoOnly = false
                        maxMarketPrice = 0.0
                        searchQuery = ""
                      }) { Text("Clear Filters") }
                    } else {
                      Button(onClick = { launchComposer() }) {
                        Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Publish First Post")
                      }
                    }
                  }
                }
              }
            }

            // FULL-SIZED SPACIOUS POST FEED CARDS
            items(filteredPosts, key = { it.id }) { post ->
              Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                BoardPostFeedCard(
                  post = post,
                  authorPilot = pilotsByUid[post.userId],
                  myUid = myUid,
                  cloudSyncReady = cloudSyncReady,
                  isSaved = bookmarkedIds.contains(post.id),
                  isFollowing = followedPilotIds.contains(post.userId),
                  translatedSummary = translatedPosts[post.id],
                  sosTriage = sosTriageMap[post.id],
                  sosTriageBusy = sosTriageBusyId == post.id,
                  searchQuery = searchQuery,
                  onInspectPilot = { inspectedPilot = it },
                  onToggleFollow = { socialVm.toggleFollowPilot(myUid, post.userId) },
                  onToggleBookmark = { socialVm.toggleBookmark(myUid, post.id) },
                  onToggleMute = { socialVm.toggleMutePeer(myUid, post.userId) },
                  onTranslate = {
                    socialVm.translateText(post.body, "concise English summary", settings.aiModelId) { tr ->
                      translatedPosts = translatedPosts + (post.id to tr)
                    }
                  },
                  onEdit = { editingPost = post },
                  onFlag = { socialVm.flagPost(post.id) },
                  onDelete = { socialVm.deletePost(post.id) },
                  onHashtagClick = { tag ->
                    searchQuery = if (searchQuery.equals(tag, true)) "" else tag
                    showSearchBar = searchQuery.isNotBlank()
                  },
                  onVotePoll = { idx -> socialVm.votePoll(post.id, idx, post.pollOptions) },
                  onVolt = { socialVm.toggleLikePost(post.id) },
                  onOpenComments = {
                    activeCommentPost = post
                    socialVm.openPostComments(post.id)
                  },
                  onBoost = { socialVm.repostToBoard(post, callsign, settings.pilotRankBadge) },
                  onQuoteBoost = { note -> socialVm.repostToBoard(post, callsign, settings.pilotRankBadge, note) },
                  onOpenDm = {
                    dmInitialPeer =
                      pilotsByUid[post.userId]
                        ?: PublicPilotProfile(userId = post.userId, callsign = post.authorCallsign, rankBadge = post.authorBadge)
                    showDms = true
                  },
                  onStartCall = { callType ->
                    socialVm.startOutgoingCall(myUid, callsign, post.userId, post.authorCallsign, callType)
                  },
                  onRunSosTriage = { socialVm.runSosAiTriage(post, settings.aiModelId) },
                  onOpenShare = { socialSharePayload = it },
                  onInspectImage = { url -> lightboxImageUrl = url },
                  onCopyNotice = { msg -> localNotice = msg },
                )
              }
            }
          }
        }
      }
    }

  }

  // DIALOGS & SHEETS
  if (showComposer) {
    PostComposerDialog(
      socialVm = socialVm,
      aiModelId = settings.aiModelId,
      telemetrySummary = liveTelemetrySummary,
      initialCategory = composerCat,
      initialTitle = composerTitle,
      initialBody = composerBody,
      initialAttachTelemetry = composerAttachTel,
      initialHelpNeeded = composerHelp,
      initialStolenAlert = composerStolen,
      onDismiss = { showComposer = false },
      onPublish = { cat, title, body, bmp, pollQ, pollOpts, price, loc, tel, help, trap, stolen ->
        socialVm.createPost(
          callsign = callsign,
          badge = settings.pilotRankBadge,
          category = cat,
          title = title,
          body = body,
          bitmap = bmp,
          pollQuestion = pollQ,
          pollOptions = pollOpts,
          priceUsd = price,
          location = loc,
          telemetrySummary = tel,
          helpNeeded = help,
          speedTrap = trap,
          stolenAlert = stolen,
          onDone = { showComposer = false },
        )
      },
    )
  }

  editingPost?.let { ep ->
    EditPostDialog(
      post = ep,
      onDismiss = { editingPost = null },
      onSave = { nextTitle, nextBody ->
        socialVm.editBoardPost(ep.id, nextTitle, nextBody) { editingPost = null }
      },
    )
  }

  if (showSlangDecoder) {
    PevSlangDecoderDialog(onDismiss = { showSlangDecoder = false })
  }

  lightboxImageUrl?.let { url ->
    PostImageLightboxDialog(imageUrl = url, onDismiss = { lightboxImageUrl = null })
  }

  activeCommentPost?.let { p ->
    var cText by remember { mutableStateOf("") }
    var aiReplies by remember { mutableStateOf<List<String>>(emptyList()) }
    ModalBottomSheet(onDismissRequest = { activeCommentPost = null }) {
      Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text("COMMENTS • ${p.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
          IconButton(onClick = { activeCommentPost = null }) { Icon(Icons.Default.Close, contentDescription = "Close Comments") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          OutlinedButton(onClick = {
            socialVm.suggestDmReplies("${p.title}: ${p.body.take(200)}", settings.aiModelId) { aiReplies = it }
          }) {
            Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("AI Reply Ideas")
          }
        }
        if (aiReplies.isNotEmpty()) {
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(aiReplies) { idea ->
              FilterChip(selected = false, onClick = { cText = idea }, label = { Text(idea) })
            }
          }
        }
        LazyColumn(modifier = Modifier.height(260.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          if (comments.isEmpty()) {
            item {
              Text("No comments yet — be the first rider to reply live.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          items(comments, key = { it.id }) { c ->
            TacticalCard {
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(c.authorCallsign, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(formatRelativeTimestamp(c.createdAt?.seconds), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Spacer(Modifier.height(2.dp))
              Text(c.body, style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          OutlinedTextField(
            value = cText,
            onValueChange = { cText = it },
            placeholder = { Text("Write a live reply...") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f),
          )
          Spacer(Modifier.width(8.dp))
          Button(onClick = {
            if (cText.isNotBlank()) {
              socialVm.commentOnPost(p.id, callsign, cText)
              cText = ""
            }
          }) { Text("Post") }
        }
      }
    }
  }

  inspectedPilot?.let { p ->
    val isMuted = mutedPeerIds.contains(p.userId)
    val isFollowing = followedPilotIds.contains(p.userId)
    TacticalDialog(
      onDismissRequest = { inspectedPilot = null },
      title = p.callsign,
      subtitle = p.rankBadge.ifBlank { "Rural ERides Rider" },
      badgeText = if (p.verified) "VERIFIED" else "PILOT",
      confirmButton = if (p.userId != myUid) {
        {
          TacticalButton(onClick = {
            inspectedPilot = null
            dmInitialPeer = p
            showDms = true
          }) { Text("Direct Message") }
        }
      } else null,
      dismissButton = {
        TacticalOutlinedButton(onClick = { inspectedPilot = null }) { Text("Close") }
      },
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          PilotAvatarThumb(avatarSource = p.avatarBase64, callsign = p.callsign, size = 44.dp)
          Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(p.callsign, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
              if (p.verified) Icon(Icons.Default.Verified, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
            }
            Text(
              "XP: ${p.xpPoints} • Rides: ${p.ridesCount} • ${com.example.ui.components.formatDistanceLabel(p.totalKm, settings.distanceUnit)}",
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }
        Text(
          "${if (p.isLiveOnline() || p.userId == myUid) "🟢 ONLINE NOW • " else ""}Trust Score: ${p.trustScore}/100 • Fleet: ${p.vehicleSummary.ifBlank { "PEV Rider" }}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (p.userId != myUid && p.userId.isNotBlank()) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TacticalOutlinedButton(
              onClick = { socialVm.toggleFollowPilot(myUid, p.userId, callsign, p.callsign) },
              modifier = Modifier.weight(1f),
            ) { Text(if (isFollowing) "Unfollow" else "Follow") }
            TacticalOutlinedButton(
              onClick = { socialVm.toggleMutePeer(myUid, p.userId) },
              modifier = Modifier.weight(1f),
            ) { Text(if (isMuted) "Unmute" else "Mute") }
          }
          if (cloudSyncReady) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              TacticalOutlinedButton(
                onClick = {
                  inspectedPilot = null
                  socialVm.startOutgoingCall(myUid, callsign, p.userId, p.callsign, "AUDIO")
                },
                modifier = Modifier.weight(1f),
              ) {
                Icon(Icons.Default.Call, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Audio")
              }
              TacticalOutlinedButton(
                onClick = {
                  inspectedPilot = null
                  socialVm.startOutgoingCall(myUid, callsign, p.userId, p.callsign, "VIDEO")
                },
                modifier = Modifier.weight(1f),
              ) {
                Icon(Icons.Default.Videocam, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Video")
              }
            }
          }
        }
      }
    }
  }

  if (showDms) {
    DirectMessagesSheet(
      sessionVm = sessionVm,
      socialVm = socialVm,
      initialPeer = dmInitialPeer,
      telemetryLat = telemetry.lat,
      telemetryLng = telemetry.lng,
      activeVehicleSummary = activeVehicleSummary,
      liveTelemetrySummary = liveTelemetrySummary,
      onDismiss = { showDms = false },
    )
  }

  socialSharePayload?.let { payload ->
    SocialShareDialog(payload = payload, onDismiss = { socialSharePayload = null })
  }

  activeStory?.let { story ->
    StoryViewerDialog(
      story = story,
      onDismiss = { activeStory = null },
    )
  }

  if (showFiftyFeatures) {
    BoardFiftyFeaturesScreen(
      settings = settings,
      onExecuteFeatureRoute = { route ->
        when {
          route == "TAB:COCKPIT" -> onSelectTab(PrimaryTabCompartment.COCKPIT)
          route == "TAB:RADAR" -> onSelectTab(PrimaryTabCompartment.RADAR)
          route == "TAB:MECHANIC" -> onSelectTab(PrimaryTabCompartment.MECHANIC)
          route == "TAB:COPILOT" -> onSelectTab(PrimaryTabCompartment.COPILOT)
          route == "TAB:CHANNEL" -> onSelectTab(PrimaryTabCompartment.CHANNEL)
          route == "OVERLAY:SETTINGS" -> onOpenOverlay(OverlayCompartment.SETTINGS)
          route == "OVERLAY:INFO" -> onOpenOverlay(OverlayCompartment.INFO)
          route == "OVERLAY:TELEMETRY_STUDIO" -> onOpenOverlay(OverlayCompartment.TELEMETRY_STUDIO)
          route == "OVERLAY:CREATOR_HUB" -> onOpenOverlay(OverlayCompartment.CREATOR_HUB)
          route == "OVERLAY:VOICE_STUDIO" -> onOpenOverlay(OverlayCompartment.VOICE_STUDIO)
          route == "POPUP:3D_LAB" -> onOpen3dLab()
          route == "BOARD:COMPOSE_STOLEN" -> launchComposer(cat = "STOLEN", stolen = true)
          route.startsWith("BOARD:COMPOSE:") -> launchComposer(cat = route.removePrefix("BOARD:COMPOSE:"))
          route.startsWith("BOARD:CAT:") -> {
            selectedCategory = route.removePrefix("BOARD:CAT:")
            urgentOnly = false
            activeSubTab = 0
          }
          route.startsWith("BOARD:SEARCH:") -> {
            searchQuery = route.removePrefix("BOARD:SEARCH:")
            showSearchBar = true
            activeSubTab = 0
          }
          route == "BOARD:FILTER:POLLS" -> {
            hasPollOnly = true
            activeSubTab = 0
          }
          route == "BOARD:TAB:STANDINGS" -> activeSubTab = 4
          route == "BOARD:SLANG" -> showSlangDecoder = true
          route == "BOARD:DM_HUB" -> {
            dmInitialPeer = null
            showDms = true
          }
        }
      },
      onNavigateBack = { showFiftyFeatures = false },
    )
  }
}

@Composable
private fun BoardTabPill(
  label: String,
  icon: ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    shape = RoundedCornerShape(20.dp),
    border = if (selected) null else BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Icon(
        icon,
        contentDescription = null,
        tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(15.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Composable
private fun QuickComposeChip(
  label: String,
  icon: ImageVector,
  accent: Color,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    color = accent.copy(alpha = 0.12f),
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(0.8.dp, accent.copy(alpha = 0.35f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
      Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = accent)
    }
  }
}

@Composable
private fun BoardStandingsTab(
  pilots: List<PublicPilotProfile>,
  myUid: String,
  settings: MasterSettings,
  followedPilotIds: Set<String>,
  bottomPad: androidx.compose.ui.unit.Dp,
  onInspectPilot: (PublicPilotProfile) -> Unit,
  onToggleFollow: (String) -> Unit,
  onInviteLocalRiders: () -> Unit = {},
  onHostGroupRide: () -> Unit = {},
) {
  var sortByMatch by remember { mutableStateOf(false) }
  var paceFilter by remember { mutableStateOf(RiderPaceTier.ANY) }
  var terrainFilter by remember { mutableStateOf(RiderTerrainPref.ANY) }

  val filteredAndSorted =
    remember(pilots, settings, sortByMatch, paceFilter, terrainFilter) {
      pilots
        .map { p ->
          val spec = RiderMatchEngine.decodeRigMatchSpec(p.vehicleSummary)
          val score = RiderMatchEngine.scorePilotMatch(settings, p)
          Triple(p, spec, score)
        }
        .filter { (_, spec, _) ->
          (paceFilter == RiderPaceTier.ANY || spec.paceTier == paceFilter) &&
            (terrainFilter == RiderTerrainPref.ANY || spec.terrainPref == terrainFilter)
        }
        .let { list ->
          if (sortByMatch) list.sortedByDescending { it.third.scorePct }
          else list.sortedByDescending { it.first.xpPoints }
        }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = bottomPad),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // RURAL NETWORK SEED CARD
    item(key = "rural_network_seed_standings") {
      TacticalCard(highlight = true) {
        Text(
          "GROW YOUR LOCAL RURAL CREW",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          "Seed your ZIP code (${settings.matchHomeZipOrTown.ifBlank { "Local Sector" }}) by inviting local riders or posting a public Group Ride.",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = onInviteLocalRiders,
            modifier = Modifier.weight(1f).testTag("standings_invite_local_button"),
          ) {
            Icon(Icons.Default.Share, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Invite Local", maxLines = 1, softWrap = false)
          }
          Button(
            onClick = onHostGroupRide,
            modifier = Modifier.weight(1f).testTag("standings_host_group_ride_button"),
          ) {
            Icon(Icons.Default.Group, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Host Ride", maxLines = 1, softWrap = false)
          }
        }
      }
    }

    // RIDER MATCH FILTER STRIP
    item(key = "standings_match_filters") {
      TacticalCard {
        Text("RIDER MATCH & COMPATIBILITY FILTERS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          item {
            FilterChip(
              selected = !sortByMatch,
              onClick = { sortByMatch = false },
              label = { Text("Top XP", maxLines = 1, softWrap = false) },
            )
          }
          item {
            FilterChip(
              selected = sortByMatch,
              onClick = { sortByMatch = true },
              label = { Text("Best Match %", maxLines = 1, softWrap = false) },
            )
          }
          items(RiderPaceTier.entries) { p ->
            FilterChip(
              selected = paceFilter == p,
              onClick = { paceFilter = p },
              label = { Text(if (p == RiderPaceTier.ANY) "Any Pace" else p.name, maxLines = 1, softWrap = false) },
            )
          }
          items(RiderTerrainPref.entries) { t ->
            FilterChip(
              selected = terrainFilter == t,
              onClick = { terrainFilter = t },
              label = { Text(if (t == RiderTerrainPref.ANY) "Any Terrain" else t.label.substringBefore(" "), maxLines = 1, softWrap = false) },
            )
          }
        }
      }
    }

    // TOP 3 PODIUM CARD
    if (filteredAndSorted.isNotEmpty()) {
      item {
        TacticalCard(highlight = true) {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
              if (sortByMatch) "TOP MATCHED RIDERS" else "LIVE SECTOR LEADERBOARD",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.primary,
            )
            Text("${filteredAndSorted.size} real riders", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
          }
          Spacer(Modifier.height(10.dp))
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filteredAndSorted.take(3).forEachIndexed { index, (pilot, _, score) ->
              val medalColor =
                when (index) {
                  0 -> Color(0xFFFFD700)
                  1 -> Color(0xFFC0C0C0)
                  else -> Color(0xFFCD7F32)
                }
              Surface(
                onClick = { onInspectPilot(pilot) },
                color = medalColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, medalColor.copy(alpha = 0.55f)),
                modifier = Modifier.weight(1f),
              ) {
                Column(
                  modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Text("#${index + 1}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = medalColor)
                  Box(
                    modifier = Modifier.clip(CircleShape).border(2.dp, medalColor, CircleShape).padding(2.dp),
                  ) {
                    PilotAvatarThumb(avatarSource = pilot.avatarBase64, callsign = pilot.callsign, size = 40.dp)
                  }
                  Text(
                    text = pilot.callsign,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    if (sortByMatch) "${score.scorePct}% Match" else "${pilot.xpPoints} XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }
          }
        }
      }
    } else {
      item {
        TacticalCard {
          Text(
            "No live pilots match these filters yet. Reset Pace/Terrain filters or tap Invite Local above to grow your area.",
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
    }

    itemsIndexed(filteredAndSorted, key = { _, triple -> triple.first.userId }) { index, (pilot, spec, score) ->
      val isFollowing = followedPilotIds.contains(pilot.userId)
      TacticalCard(highlight = pilot.userId == myUid, onClick = { onInspectPilot(pilot) }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Box(
            modifier =
              Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                  Brush.linearGradient(
                    listOf(
                      MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                      MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                    )
                  )
                ),
            contentAlignment = Alignment.Center,
          ) {
            Text("${index + 1}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
          }
          PilotAvatarThumb(avatarSource = pilot.avatarBase64, callsign = pilot.callsign, size = 38.dp)
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(pilot.callsign, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
              if (pilot.verified) Icon(Icons.Default.Verified, contentDescription = "Verified", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(15.dp))
              if (pilot.userId != myUid) {
                Surface(
                  color =
                    if (score.scorePct >= 75) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                  shape = RoundedCornerShape(8.dp),
                ) {
                  Text(
                    "Match ${score.scorePct}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                  )
                }
              }
            }
            Text(
              "${pilot.rankBadge} • ${"%.1f".format(pilot.totalKm)} km • ${pilot.ridesCount} rides",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              "${spec.paceTier.label} • ${spec.terrainPref.label} • ~${spec.usableRangeMi}mi • ${spec.scheduleVibe.label}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.secondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (pilot.userId != myUid && !score.rangeCompatible && score.warningText.isNotBlank()) {
              Text(
                score.warningText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
          Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${pilot.xpPoints} XP", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.tertiary)
            if (pilot.userId != myUid && pilot.userId.isNotBlank()) {
              Surface(
                onClick = { onToggleFollow(pilot.userId) },
                color = if (isFollowing) MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                shape = RoundedCornerShape(14.dp),
              ) {
                Text(
                  text = if (isFollowing) "Following" else "Follow",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (isFollowing) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}
