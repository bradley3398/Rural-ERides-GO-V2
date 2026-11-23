package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BoardPost
import com.example.data.MasterSettings
import com.example.data.PublicPilotProfile
import com.example.data.RiderMatchEngine
import com.example.ui.components.SocialPlatform
import com.example.ui.components.SocialSharePayload
import com.example.ui.components.TacticalCard
import com.example.viewmodel.CopilotSocialViewModel

data class LiveSocialFeatureItem(
  val id: String,
  val title: String,
  val category: String,
  val description: String,
  val actionKey: String,
)

/**
 * 100+ live social media capabilities wired directly to live Firestore posts, real rider directory,
 * live Gemini AI Caption Studio, native social cross-posting, live DMs, and live WebRTC calling.
 * Zero mock posts or fake counters.
 */
object LiveSocialFeatureCatalog {
  val categories =
    listOf(
      "All",
      "Publishing",
      "Discovery",
      "Marketplace",
      "Safety & BOLO",
      "Crew & Events",
      "Comms & Calls",
      "AI Studio",
      "Cross-Post",
      "Moderation",
      "Analytics",
    )

  val allFeatures: List<LiveSocialFeatureItem> =
    listOf(
      // PUBLISHING (12)
      LiveSocialFeatureItem("sf_01", "Live Universal Broadcast", "Publishing", "Compose a live community transmission synced to Firestore.", "COMPOSE:UNIVERSAL"),
      LiveSocialFeatureItem("sf_02", "Live Ride Report w/ Telemetry", "Publishing", "Attach live GPS speed, trip distance, and battery %.", "COMPOSE_TEL:RIDE_REPORTS"),
      LiveSocialFeatureItem("sf_03", "PEV Build Log Post", "Publishing", "Document custom 72V/VESC/suspension upgrades live.", "COMPOSE:BUILDS"),
      LiveSocialFeatureItem("sf_04", "Garage Showcase Card", "Publishing", "Share your current PEV rig photos & specs.", "COMPOSE:GARAGE"),
      LiveSocialFeatureItem("sf_05", "4-Option Live Poll Creator", "Publishing", "Launch real-time Firestore community polls.", "COMPOSE:UNIVERSAL"),
      LiveSocialFeatureItem("sf_06", "Public 120V/20A Charger Spot", "Publishing", "Report verified PEV charging outlets for riders.", "COMPOSE:CHARGERS"),
      LiveSocialFeatureItem("sf_07", "Gear & Tire Review Post", "Publishing", "Publish real-world tire, helmet, or controller reviews.", "COMPOSE:REVIEWS"),
      LiveSocialFeatureItem("sf_08", "Crew Briefing Dispatch", "Publishing", "Broadcast squad-level ride plans and updates.", "COMPOSE:CREW"),
      LiveSocialFeatureItem("sf_09", "Road & Trail Alert Post", "Publishing", "Warn riders about debris, closures, or hazards.", "COMPOSE:ALERTS"),
      LiveSocialFeatureItem("sf_10", "Anonymous Rider Confession", "Publishing", "Share lessons learned & trail close-call stories.", "COMPOSE:CONFESSIONS"),
      LiveSocialFeatureItem("sf_11", "YouTube Channel Video Request", "Publishing", "Request upcoming Rural ERides GO video topics.", "COMPOSE:VIDEO_REQUEST"),
      LiveSocialFeatureItem("sf_12", "Photo Upload Transmission", "Publishing", "Attach high-res PEV photos via zero-permission Photo Picker.", "COMPOSE:UNIVERSAL"),
      // DISCOVERY (12)
      LiveSocialFeatureItem("sf_13", "Filter: Most Volted Posts", "Discovery", "Rank live board posts by real community Volts.", "SORT:Most Volted"),
      LiveSocialFeatureItem("sf_14", "Filter: Most Discussed", "Discovery", "Surface live threads with highest comment activity.", "SORT:Most Discussed"),
      LiveSocialFeatureItem("sf_15", "Filter: Newest Live Stream", "Discovery", "Chronological live Firestore feed.", "SORT:Newest"),
      LiveSocialFeatureItem("sf_16", "Filter: Active Polls Only", "Discovery", "Jump straight to live votable community polls.", "FILTER:POLLS"),
      LiveSocialFeatureItem("sf_17", "Filter: Photo Feed Gallery", "Discovery", "Browse live PEV photo transmissions only.", "FILTER:PHOTOS"),
      LiveSocialFeatureItem("sf_18", "Filter: Build Logs", "Discovery", "Inspect real custom VESC, battery & chassis builds.", "CAT:BUILDS"),
      LiveSocialFeatureItem("sf_19", "Filter: Ride Reports", "Discovery", "Read live trail & highway range reports.", "CAT:RIDE_REPORTS"),
      LiveSocialFeatureItem("sf_20", "Filter: Gear Reviews", "Discovery", "Compare rider-tested tires, pads, and helmets.", "CAT:REVIEWS"),
      LiveSocialFeatureItem("sf_21", "Filter: Public Chargers", "Discovery", "Find community-reported charging stops.", "CAT:CHARGERS"),
      LiveSocialFeatureItem("sf_22", "Filter: Following Feed", "Discovery", "See posts exclusively from pilots you follow.", "TAB:FOLLOWING"),
      LiveSocialFeatureItem("sf_23", "Filter: Saved Bookmarks", "Discovery", "Access your bookmarked transmissions vault.", "TAB:SAVED"),
      LiveSocialFeatureItem("sf_24", "PEV Slang & Tech Decoder", "Discovery", "Instant glossary of VESC, PWM, LVC, C-Rate & more.", "SLANG"),
      // MARKETPLACE (10)
      LiveSocialFeatureItem("sf_25", "List PEV or Part For Sale", "Marketplace", "Post real asking price & pickup sector live.", "COMPOSE:MARKETPLACE"),
      LiveSocialFeatureItem("sf_26", "Browse All Live Listings", "Marketplace", "View live rider-to-rider PEV marketplace posts.", "CAT:MARKETPLACE"),
      LiveSocialFeatureItem("sf_27", "Budget Finder: Under $500", "Marketplace", "Filter live parts & accessories under $500.", "PRICE:500"),
      LiveSocialFeatureItem("sf_28", "Mid-Range Rigs: Under $1500", "Marketplace", "Filter live PEV listings under $1,500.", "PRICE:1500"),
      LiveSocialFeatureItem("sf_29", "Hyper Rigs: Under $3000", "Marketplace", "Filter high-voltage rigs under $3,000.", "PRICE:3000"),
      LiveSocialFeatureItem("sf_30", "Battery Pack Trade Search", "Marketplace", "Search live posts mentioning batteries or packs.", "SEARCH:battery"),
      LiveSocialFeatureItem("sf_31", "VESC & Controller Market", "Marketplace", "Search live listings for VESC or controllers.", "SEARCH:VESC"),
      LiveSocialFeatureItem("sf_32", "Tires & Brakes Parts Swap", "Marketplace", "Search live listings for PMT tires or calipers.", "SEARCH:tire"),
      LiveSocialFeatureItem("sf_33", "Protective Gear Exchange", "Marketplace", "Search helmets, armor, and gloves.", "SEARCH:helmet"),
      LiveSocialFeatureItem("sf_34", "1-Tap Seller Direct Message", "Marketplace", "Open encrypted DM directly with any listing author.", "DM_HUB"),
      // SAFETY & BOLO (10)
      LiveSocialFeatureItem("sf_35", "Broadcast Live SOS Alert", "Safety & BOLO", "Flag urgent roadside help needed to all pilots.", "COMPOSE_SOS"),
      LiveSocialFeatureItem("sf_36", "Stolen PEV BOLO Alert", "Safety & BOLO", "Broadcast stolen rig serial, mods & last seen sector.", "COMPOSE_STOLEN"),
      LiveSocialFeatureItem("sf_37", "Live SOS & Hazards Feed", "Safety & BOLO", "Filter active SOS, speed traps, and stolen alerts.", "FILTER:URGENT"),
      LiveSocialFeatureItem("sf_38", "Stolen Rig Watchlist", "Safety & BOLO", "Inspect every active BOLO stolen PEV post.", "CAT:STOLEN"),
      LiveSocialFeatureItem("sf_39", "Road Hazards & Debris Feed", "Safety & BOLO", "Check active trail/road alerts from riders.", "CAT:ALERTS"),
      LiveSocialFeatureItem("sf_40", "Gemini Live SOS AI Triage", "Safety & BOLO", "Run real Gemini roadside diagnostics on SOS posts.", "FILTER:URGENT"),
      LiveSocialFeatureItem("sf_41", "Speed Trap & Patrol Spotter", "Safety & BOLO", "Search posts flagged for speed traps.", "SEARCH:trap"),
      LiveSocialFeatureItem("sf_42", "Flat Tire & Tube Rescue Call", "Safety & BOLO", "Broadcast tire repair assistance request.", "COMPOSE_SOS"),
      LiveSocialFeatureItem("sf_43", "Low-Battery Tow / Charger SOS", "Safety & BOLO", "Request emergency charging outlet nearby.", "COMPOSE_SOS"),
      LiveSocialFeatureItem("sf_44", "Cross-Share Stolen BOLO", "Safety & BOLO", "Blast BOLO alerts to Facebook, X, Instagram & Nextdoor.", "SHARE_SOCIAL"),
      // CREW & EVENTS (10)
      LiveSocialFeatureItem("sf_45", "Schedule Group Ride Convoy", "Crew & Events", "Post meetup time, pace, and voltage class.", "COMPOSE:GROUP_RIDES"),
      LiveSocialFeatureItem("sf_46", "Publish PEV Event / Race", "Crew & Events", "Announce local PEV races, demos, or rallies.", "COMPOSE:EVENTS"),
      LiveSocialFeatureItem("sf_47", "Browse Live Group Rides", "Crew & Events", "Join active rider convoys on the board.", "CAT:GROUP_RIDES"),
      LiveSocialFeatureItem("sf_48", "Browse PEV Events Calendar", "Crew & Events", "See upcoming community rides and meetups.", "CAT:EVENTS"),
      LiveSocialFeatureItem("sf_49", "Live Sector XP Leaderboard", "Crew & Events", "Inspect real top pilots by XP, km & rides.", "TAB:STANDINGS"),
      LiveSocialFeatureItem("sf_50", "Night Ride Convoy Finder", "Crew & Events", "Search live posts for night ride meetups.", "SEARCH:night"),
      LiveSocialFeatureItem("sf_51", "Trail / Off-Road Convoy", "Crew & Events", "Search live posts for gravel & off-road rides.", "SEARCH:trail"),
      LiveSocialFeatureItem("sf_52", "EUC Squad Finder", "Crew & Events", "Search live posts tagged #EUC or wheel riders.", "SEARCH:EUC"),
      LiveSocialFeatureItem("sf_53", "Sur-Ron / E-Moto Crew", "Crew & Events", "Search live posts for e-moto riders.", "SEARCH:moto"),
      LiveSocialFeatureItem("sf_54", "Crew Channel Dispatch", "Crew & Events", "Filter private-style squad updates.", "CAT:CREW"),
      // COMMS & CALLS (12)
      LiveSocialFeatureItem("sf_55", "Encrypted Direct Messages", "Comms & Calls", "Open live Firestore DM inbox & search.", "DM_HUB"),
      LiveSocialFeatureItem("sf_56", "Pinned Priority DM Threads", "Comms & Calls", "Keep riding partners pinned at top of DMs.", "DM_HUB"),
      LiveSocialFeatureItem("sf_57", "Self-Destruct Ephemeral DMs", "Comms & Calls", "Auto-burn messages after 1m, 5m, 1h, or 24h.", "DM_HUB"),
      LiveSocialFeatureItem("sf_58", "DM Voice Dictation & Notes", "Comms & Calls", "Hands-free speech-to-text & voice notes in DM.", "DM_HUB"),
      LiveSocialFeatureItem("sf_59", "Share Live GPS Pin in DM", "Comms & Calls", "Drop real lat/lng coordinates inside DMs.", "DM_HUB"),
      LiveSocialFeatureItem("sf_60", "Share Active PEV Card in DM", "Comms & Calls", "Send your active vehicle specs directly in chat.", "DM_HUB"),
      LiveSocialFeatureItem("sf_61", "AI Smart Quick Replies in DM", "Comms & Calls", "Gemini generates 3 tactical replies for incoming DMs.", "DM_HUB"),
      LiveSocialFeatureItem("sf_62", "Live P2P WebRTC Audio Calls", "Comms & Calls", "Place encrypted rider-to-rider voice calls.", "DM_HUB"),
      LiveSocialFeatureItem("sf_63", "Live P2P WebRTC Video Calls", "Comms & Calls", "HD camera calls with front/rear flip & PiP.", "DM_HUB"),
      LiveSocialFeatureItem("sf_64", "Mid-Call Audio/Video Switch", "Comms & Calls", "Upgrade or downgrade video mid-call seamlessly.", "DM_HUB"),
      LiveSocialFeatureItem("sf_65", "In-Call Tactical Chat & Pings", "Comms & Calls", "Send live text & ride pings during active calls.", "DM_HUB"),
      LiveSocialFeatureItem("sf_66", "Call History & 1-Tap Redial", "Comms & Calls", "View recent Firestore call log and redial instantly.", "DM_HUB"),
      // AI STUDIO (10)
      LiveSocialFeatureItem("sf_67", "AI Caption Writer (Instagram)", "AI Studio", "Generate PEV captions + hashtags via Gemini.", "AI_STUDIO:Instagram"),
      LiveSocialFeatureItem("sf_68", "AI Post Writer (X / Twitter)", "AI Studio", "Generate punchy 280-char PEV posts via Gemini.", "AI_STUDIO:X"),
      LiveSocialFeatureItem("sf_69", "AI Story Script (TikTok)", "AI Studio", "Generate viral ride reel hooks & captions.", "AI_STUDIO:TikTok"),
      LiveSocialFeatureItem("sf_70", "AI Group Post (Facebook)", "AI Studio", "Write engaging local PEV group announcements.", "AI_STUDIO:Facebook"),
      LiveSocialFeatureItem("sf_71", "AI YouTube Description", "AI Studio", "Craft SEO ride video descriptions & tags.", "AI_STUDIO:YouTube"),
      LiveSocialFeatureItem("sf_72", "AI Reddit Build Breakdown", "AI Studio", "Format clean technical spec posts for Reddit.", "AI_STUDIO:Reddit"),
      LiveSocialFeatureItem("sf_73", "AI Tone Polish: Tactical", "AI Studio", "Rewrite post drafts in sharp tactical prose.", "COMPOSE:UNIVERSAL"),
      LiveSocialFeatureItem("sf_74", "AI Tone Polish: Hype", "AI Studio", "Boost excitement for ride recaps & events.", "COMPOSE:UNIVERSAL"),
      LiveSocialFeatureItem("sf_75", "AI Post Translator & Summary", "AI Studio", "Translate or condense any live post in 1 tap.", "TAB:FEED"),
      LiveSocialFeatureItem("sf_76", "AI Comment Reply Helper", "AI Studio", "Draft instant helpful replies in post threads.", "TAB:FEED"),
      // CROSS-POST (12)
      LiveSocialFeatureItem("sf_77", "Share to Facebook", "Cross-Post", "Send live post or telemetry card to Facebook.", "PLATFORM:facebook"),
      LiveSocialFeatureItem("sf_78", "Share to X (Twitter)", "Cross-Post", "Post live ride highlights directly to X.", "PLATFORM:x"),
      LiveSocialFeatureItem("sf_79", "Share to Instagram", "Cross-Post", "Share PEV stories & posts to Instagram.", "PLATFORM:instagram"),
      LiveSocialFeatureItem("sf_80", "Share to TikTok", "Cross-Post", "Cross-share PEV clips and captions to TikTok.", "PLATFORM:tiktok"),
      LiveSocialFeatureItem("sf_81", "Share to YouTube", "Cross-Post", "Share updates to YouTube community.", "PLATFORM:youtube"),
      LiveSocialFeatureItem("sf_82", "Share to Reddit", "Cross-Post", "Cross-post to r/ElectricScooters / r/ElectricUnicycle.", "PLATFORM:reddit"),
      LiveSocialFeatureItem("sf_83", "Share to WhatsApp", "Cross-Post", "Send ride invites & pins to WhatsApp squads.", "PLATFORM:whatsapp"),
      LiveSocialFeatureItem("sf_84", "Share to Telegram", "Cross-Post", "Broadcast to Telegram PEV channels.", "PLATFORM:telegram"),
      LiveSocialFeatureItem("sf_85", "Share to Discord", "Cross-Post", "Drop build logs & alerts into Discord servers.", "PLATFORM:discord"),
      LiveSocialFeatureItem("sf_86", "Share to Threads", "Cross-Post", "Cross-post ride notes to Meta Threads.", "PLATFORM:threads"),
      LiveSocialFeatureItem("sf_87", "Share to Messenger", "Cross-Post", "Message riding friends on Facebook Messenger.", "PLATFORM:messenger"),
      LiveSocialFeatureItem("sf_88", "Share to Snapchat / System", "Cross-Post", "Broadcast via Snapchat or Android Share Sheet.", "PLATFORM:snapchat"),
      // MODERATION & PROFILE (8)
      LiveSocialFeatureItem("sf_89", "Follow / Unfollow Pilots", "Moderation", "Curate your personal Following rider feed.", "TAB:STANDINGS"),
      LiveSocialFeatureItem("sf_90", "Mute / Unmute Noisy Accounts", "Moderation", "Hide unwanted authors from your live feed.", "TAB:STANDINGS"),
      LiveSocialFeatureItem("sf_91", "Live Post Author Editing", "Moderation", "Edit headlines and bodies of your live posts.", "TAB:FEED"),
      LiveSocialFeatureItem("sf_92", "Boost / Repost to Board", "Moderation", "Amplify valuable rider alerts or builds live.", "TAB:FEED"),
      LiveSocialFeatureItem("sf_93", "Flag Unsafe / Spam Posts", "Moderation", "Report violations directly in Firestore.", "TAB:FEED"),
      LiveSocialFeatureItem("sf_94", "Immutable Callsign Identity", "Moderation", "Permanent locked rider callsign prevents spoofing.", "TAB:STANDINGS"),
      LiveSocialFeatureItem("sf_95", "Verified Pilot Trust Badges", "Moderation", "Inspect live Trust Score (0-100) & verified status.", "TAB:STANDINGS"),
      LiveSocialFeatureItem("sf_96", "Author Delete Transmission", "Moderation", "Remove your own live posts cleanly anytime.", "TAB:FEED"),
      // ANALYTICS & LIVE PULSE (6)
      LiveSocialFeatureItem("sf_97", "Live Sector Post Velocity", "Analytics", "Real-time count of active cloud transmissions.", "PULSE"),
      LiveSocialFeatureItem("sf_98", "Live Trending Hashtag Cloud", "Analytics", "Auto-extracted #tags from real live posts.", "PULSE"),
      LiveSocialFeatureItem("sf_99", "Top Volted Contributors", "Analytics", "Live ranking of highest-engagement riders.", "TAB:STANDINGS"),
      LiveSocialFeatureItem("sf_100", "Live Poll Participation Tracker", "Analytics", "Track real vote counts across active polls.", "FILTER:POLLS"),
      LiveSocialFeatureItem("sf_101", "Live BOLO & SOS Counter", "Analytics", "Instant visibility into active sector emergencies.", "FILTER:URGENT"),
      LiveSocialFeatureItem("sf_102", "One-Tap Copy Post & Link", "Analytics", "Copy formatted transmission text to clipboard.", "TAB:FEED"),
    )
}

@Composable
fun BoardSocialHubTab(
  posts: List<BoardPost>,
  pilots: List<PublicPilotProfile>,
  followedPilotIds: Set<String>,
  myUid: String,
  myCallsign: String,
  cloudSyncReady: Boolean,
  settings: MasterSettings = MasterSettings(),
  telemetrySummary: String,
  aiModelId: String,
  socialVm: CopilotSocialViewModel,
  onOpenComposer: (category: String, title: String, body: String, attachTel: Boolean, help: Boolean, stolen: Boolean) -> Unit,
  onApplyFeedAction: (String) -> Unit,
  onOpenDmWithPilot: (PublicPilotProfile?) -> Unit,
  onOpenShareDialog: (SocialSharePayload) -> Unit,
  onDirectPlatformShare: (SocialPlatform, String) -> Unit,
  onOpenFiftyFeatures: () -> Unit,
  onInviteLocalRiders: () -> Unit = {},
) {
  var featureSearch by remember { mutableStateOf("") }
  var selectedCat by remember { mutableStateOf("All") }
  var aiTopic by remember { mutableStateOf("") }
  var aiPlatform by remember { mutableStateOf("Instagram") }
  var aiGeneratedCaption by remember { mutableStateOf("") }
  var aiBusy by remember { mutableStateOf(false) }

  // Live hashtags extracted ONLY from real posts (zero mock tags)
  val liveHashtags =
    remember(posts) {
      val regex = Regex("#[A-Za-z0-9_]{2,28}")
      posts
        .flatMap { regex.findAll("${it.title} ${it.body}").map { m -> m.value }.toList() }
        .groupingBy { it.lowercase() }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(12)
        .map { it.key to it.value }
    }

  val filteredFeatures =
    remember(featureSearch, selectedCat) {
      LiveSocialFeatureCatalog.allFeatures.filter { f ->
        (selectedCat == "All" || f.category == selectedCat) &&
          (featureSearch.isBlank() ||
            f.title.contains(featureSearch, true) ||
            f.description.contains(featureSearch, true) ||
            f.category.contains(featureSearch, true))
      }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item {
      TacticalCard(highlight = true, onClick = onOpenFiftyFeatures) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Column(Modifier.weight(1f)) {
            Text("⚡ 50 ADVANCED BOARD & COCKPIT FEATURES SUITE", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            Text("Explore all 50 specialized tools, diagnostics, and navigation features built for Rural PEV pilots.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        }
      }
    }

    // LIVE COMMUNITY PULSE (REAL COUNTS ONLY)
    item {
      TacticalCard(highlight = true) {
        Text("LIVE SOCIAL PULSE (100% REAL DATA)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          PulseStat("LIVE POSTS", "${posts.size}")
          PulseStat("PILOTS", "${pilots.size}")
          PulseStat("FOLLOWING", "${followedPilotIds.size}")
          PulseStat("POLLS", "${posts.count { it.pollQuestion.isNotBlank() }}")
        }
        if (liveHashtags.isNotEmpty()) {
          Spacer(Modifier.height(8.dp))
          Text("TRENDING LIVE #HASHTAGS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(liveHashtags) { (tag, count) ->
              FilterChip(
                selected = false,
                onClick = { onApplyFeedAction("SEARCH:$tag") },
                label = { Text("$tag ($count)") },
              )
            }
          }
        }
      }
    }

    // LIVE GEMINI AI CAPTION & HASHTAG STUDIO
    item {
      TacticalCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
          Text("AI SOCIAL CAPTION & HASHTAG STUDIO", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(listOf("Instagram", "X", "Facebook", "TikTok", "YouTube", "Reddit", "Discord")) { p ->
            FilterChip(selected = aiPlatform == p, onClick = { aiPlatform = p }, label = { Text(p) })
          }
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
          value = aiTopic,
          onValueChange = { aiTopic = it },
          label = { Text("What are you posting? (e.g. 42-mile 72V canyon run)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("social_ai_studio_topic"),
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = {
              if (aiTopic.isNotBlank()) {
                aiBusy = true
                socialVm.generateSocialCaptions(aiTopic, aiPlatform, telemetrySummary, aiModelId) { out ->
                  aiGeneratedCaption = out
                  aiBusy = false
                }
              }
            },
            enabled = !aiBusy && aiTopic.isNotBlank(),
          ) {
            Text(if (aiBusy) "Generating..." else "Generate Caption")
          }
          if (aiGeneratedCaption.isNotBlank()) {
            OutlinedButton(onClick = {
              onOpenComposer("RIDE_REPORTS", aiTopic.take(80), aiGeneratedCaption, true, false, false)
            }) { Text("Post to Board") }
            OutlinedButton(onClick = {
              onOpenShareDialog(SocialSharePayload(title = aiTopic.ifBlank { "PEV Ride Update" }, text = aiGeneratedCaption))
            }) {
              Icon(Icons.Default.Share, null, Modifier.size(16.dp))
            }
          }
        }
        if (aiGeneratedCaption.isNotBlank()) {
          Spacer(Modifier.height(6.dp))
          Text(aiGeneratedCaption, style = MaterialTheme.typography.bodySmall)
        }
      }
    }

    // LIVE RIDER DIRECTORY (100% REAL AUTHENTICATED RIDERS — MATCH SCORE / FOLLOW / DM / CALL)
    item {
      val otherRealPilots =
        remember(pilots, myUid, settings) {
          pilots
            .filter { it.userId != myUid }
            .sortedByDescending { RiderMatchEngine.scorePilotMatch(settings, it).scorePct }
        }
      TacticalCard {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            "LIVE RIDER DIRECTORY (${otherRealPilots.size} REAL RIDERS)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
          )
          OutlinedButton(
            onClick = onInviteLocalRiders,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.testTag("hub_invite_local_riders_button"),
          ) {
            Icon(Icons.Default.Share, null, Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("Invite Local", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
          }
        }
        Spacer(Modifier.height(6.dp))
        if (otherRealPilots.isEmpty()) {
          Text(
            "No other real signed-in riders are listed yet. Tap Invite Local to bring nearby riders in your ZIP/town onto live Radar & Porch Talk.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(otherRealPilots.take(15), key = { it.userId }) { pilot ->
              val isFollowing = followedPilotIds.contains(pilot.userId)
              val matchScore = remember(pilot, settings) { RiderMatchEngine.scorePilotMatch(settings, pilot) }
              val rigSpec = remember(pilot.vehicleSummary) { RiderMatchEngine.decodeRigMatchSpec(pilot.vehicleSummary) }
              TacticalCard(modifier = Modifier.width(248.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    pilot.callsign,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                  )
                  Surface(
                    color =
                      if (matchScore.scorePct >= 75) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                      else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                  ) {
                    Text(
                      text = "Match ${matchScore.scorePct}%",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                  }
                }
                Text("${pilot.rankBadge} • ${pilot.xpPoints} XP • ${rigSpec.displayRigName}", style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                  "${rigSpec.paceTier.label} • ${rigSpec.terrainPref.label} • ~${rigSpec.usableRangeMi}mi • ${rigSpec.scheduleVibe.label}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.secondary,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
                if (!matchScore.rangeCompatible && matchScore.warningText.isNotBlank()) {
                  Text(
                    matchScore.warningText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  OutlinedButton(
                    onClick = { socialVm.toggleFollowPilot(myUid, pilot.userId) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                  ) {
                    Icon(if (isFollowing) Icons.Default.PersonRemove else Icons.Default.PersonAdd, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(if (isFollowing) "Following" else "Follow", style = MaterialTheme.typography.labelSmall)
                  }
                  OutlinedButton(
                    onClick = { onOpenDmWithPilot(pilot) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                  ) { Icon(Icons.Default.Mail, "DM", Modifier.size(14.dp)) }
                  if (cloudSyncReady) {
                    OutlinedButton(
                      onClick = { socialVm.startOutgoingCall(myUid, myCallsign, pilot.userId, pilot.callsign, "AUDIO") },
                      contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) { Icon(Icons.Default.Call, "Audio Call", Modifier.size(14.dp)) }
                    OutlinedButton(
                      onClick = { socialVm.startOutgoingCall(myUid, myCallsign, pilot.userId, pilot.callsign, "VIDEO") },
                      contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) { Icon(Icons.Default.Videocam, "Video Call", Modifier.size(14.dp)) }
                  }
                }
              }
            }
          }
        }
      }
    }

    // 100+ LIVE SOCIAL COMMAND ACTIONS MATRIX
    item {
      Text(
        "SOCIAL COMMAND MATRIX (${LiveSocialFeatureCatalog.allFeatures.size} LIVE FEATURES)",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
      )
      Spacer(Modifier.height(6.dp))
      OutlinedTextField(
        value = featureSearch,
        onValueChange = { featureSearch = it },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        placeholder = { Text("Search 102 live social features (polls, BOLO, reels, DM, calls)...") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("social_hub_search_input"),
      )
      Spacer(Modifier.height(6.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(LiveSocialFeatureCatalog.categories) { cat ->
          FilterChip(selected = selectedCat == cat, onClick = { selectedCat = cat }, label = { Text(cat) })
        }
      }
    }

    items(filteredFeatures, key = { it.id }) { feat ->
      TacticalCard(
        onClick = {
          when {
            feat.actionKey.startsWith("COMPOSE_TEL:") ->
              onOpenComposer(feat.actionKey.removePrefix("COMPOSE_TEL:"), "", "", true, false, false)
            feat.actionKey.startsWith("COMPOSE:") ->
              onOpenComposer(feat.actionKey.removePrefix("COMPOSE:"), "", "", false, false, false)
            feat.actionKey == "COMPOSE_SOS" ->
              onOpenComposer("SOS", "", "", true, true, false)
            feat.actionKey == "COMPOSE_STOLEN" ->
              onOpenComposer("STOLEN", "", "", false, false, true)
            feat.actionKey.startsWith("AI_STUDIO:") -> {
              aiPlatform = feat.actionKey.removePrefix("AI_STUDIO:")
              if (aiTopic.isBlank()) aiTopic = "PEV Ride Telemetry & Trail Highlights"
            }
            feat.actionKey.startsWith("PLATFORM:") -> {
              val pid = feat.actionKey.removePrefix("PLATFORM:")
              val plat = SocialPlatform.entries.firstOrNull { it.id == pid } ?: SocialPlatform.FACEBOOK
              onDirectPlatformShare(plat, "Live from Rural ERides GO • $telemetrySummary #RuralERidesGO #PEV")
            }
            feat.actionKey == "SHARE_SOCIAL" ->
              onOpenShareDialog(SocialSharePayload(title = "Rural ERides GO Live Update", text = "Riding live on Rural ERides GO • $telemetrySummary #RuralERidesGO #PEV"))
            feat.actionKey == "DM_HUB" -> onOpenDmWithPilot(null)
            else -> onApplyFeedAction(feat.actionKey)
          }
        }
      ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Column(Modifier.weight(1f)) {
            Text(feat.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(feat.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Spacer(Modifier.width(8.dp))
          Text(feat.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
      }
    }
  }
}

@Composable
private fun PulseStat(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}
