package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import com.example.data.MasterSettings
import com.example.ui.components.TacticalTopBar
import com.example.ui.theme.LocalTacticalChrome

data class BoardExtraFeature(
  val id: Int,
  val category: String,
  val title: String,
  val description: String,
  val actionLabel: String,
  val routeKey: String,
)

@Composable
internal fun BoardFiftyFeaturesScreen(
  settings: MasterSettings,
  onExecuteFeatureRoute: (String) -> Unit,
  onNavigateBack: () -> Unit,
) {
  BackHandler(onBack = onNavigateBack)
  val chrome = LocalTacticalChrome.current
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("All") }

  val categories = remember { listOf("All", "Diagnostics", "Navigation", "Community", "Toolkit", "Pro Suite") }

  val features = remember {
    listOf(
      BoardExtraFeature(1, "Diagnostics", "AI Fleet Battery Health Clinic", "Open live AI Mechanic battery physics, pack impedance & cell balancing diagnostics.", "OPEN BENCH", "TAB:MECHANIC"),
      BoardExtraFeature(2, "Diagnostics", "Motor Hall Sensor Phase Test", "Inspect brushless motor commutation faults and wiring pinout maps in AI Mechanic.", "OPEN BENCH", "TAB:MECHANIC"),
      BoardExtraFeature(3, "Diagnostics", "Controller PWM & FOC Spec Matrix", "Compare sine-wave/VESC controller specs, phase limits & wiring in Rig Specs.", "OPEN BENCH", "TAB:MECHANIC"),
      BoardExtraFeature(4, "Diagnostics", "BMS Cell Voltage Delta Monitor", "Inspect live NMC/LiFePO4 voltage curves, cutoff & storage targets in Mechanic.", "OPEN BENCH", "TAB:MECHANIC"),
      BoardExtraFeature(5, "Diagnostics", "Voltage Sag Dynamic Predictor", "Monitor live under-load voltage sag and pack reserve directly on your Cockpit HUD.", "OPEN COCKPIT", "TAB:COCKPIT"),
      BoardExtraFeature(6, "Diagnostics", "Regenerative Braking Profiler", "Configure drivetrain, cold-pack & regen settings inside Dashboard Settings.", "OPEN SETTINGS", "OVERLAY:SETTINGS"),
      BoardExtraFeature(7, "Diagnostics", "Hub Motor Thermal Runaway Guide", "Run live thermal camera overlay & stator heat inspection inside Vision AR Bench.", "OPEN BENCH", "TAB:MECHANIC"),
      BoardExtraFeature(8, "Diagnostics", "Charger Speed & Cost Calculator", "Calculate CC+CV charge hours, wattage & electricity cost in Shop Calc & Rig Specs.", "OPEN BENCH", "TAB:MECHANIC"),
      BoardExtraFeature(9, "Diagnostics", "Suspension Sag & Rider Weight Tuner", "Tune rider payload weight & suspension preload guidance in Co-Pilot or Settings.", "OPEN COPILOT", "TAB:COPILOT"),
      BoardExtraFeature(10, "Diagnostics", "Tire Pressure & PSI Maintenance", "Track tire wear intervals, PSI reminders & torque logs inside Cockpit Maintenance.", "OPEN COCKPIT", "TAB:COCKPIT"),
      BoardExtraFeature(11, "Navigation", "Offline Vector Map Tile Cacher", "Load OSMDroid topographic & county road maps on Rider Radar.", "OPEN RADAR", "TAB:RADAR"),
      BoardExtraFeature(12, "Navigation", "RainViewer Doppler Radar Stream", "Toggle live RainViewer precipitation overlays directly on the Radar map.", "OPEN RADAR", "TAB:RADAR"),
      BoardExtraFeature(13, "Navigation", "GPS Geofence Perimeter Alarm", "Drop a live GPS perimeter fence with breach warnings on Rider Radar.", "OPEN RADAR", "TAB:RADAR"),
      BoardExtraFeature(14, "Navigation", "Anti-Theft Motion & Crash Guard", "Configure IMU G-force crash sensitivity & SOS countdown in Safety Settings.", "OPEN SETTINGS", "OVERLAY:SETTINGS"),
      BoardExtraFeature(15, "Navigation", "Turn-by-Turn Audio Navigation", "Launch live OSRM PEV turn-by-turn routing with spoken voice guidance on Radar.", "OPEN RADAR", "TAB:RADAR"),
      BoardExtraFeature(16, "Navigation", "Live Multiplayer Rider Radar", "Broadcast & track real authenticated community riders in real time on Radar.", "OPEN RADAR", "TAB:RADAR"),
      BoardExtraFeature(17, "Navigation", "Elevation Profile Grade Predictor", "Inspect live Open-Elevation route grade profiles on Rider Radar.", "OPEN RADAR", "TAB:RADAR"),
      BoardExtraFeature(18, "Navigation", "Emergency SOS Coordinate Relay", "Trigger or configure GPS SOS emergency contact alerts in Cockpit & Safety.", "OPEN COCKPIT", "TAB:COCKPIT"),
      BoardExtraFeature(19, "Navigation", "Live Public Charger Locator", "Query live Overpass OSM EV & PEV charging stops around your GPS fix.", "OPEN RADAR", "TAB:RADAR"),
      BoardExtraFeature(20, "Navigation", "Ride Track GPX & Telemetry Studio", "Inspect recorded GPS tracks, speed zones & energy graphs in Telemetry Studio.", "OPEN STUDIO", "OVERLAY:TELEMETRY_STUDIO"),
      BoardExtraFeature(21, "Community", "Convoy Voice Walkie-Talkie Channel", "Open live Firestore PTT squad walkie-talkie channels on Rider Radar.", "OPEN RADAR", "TAB:RADAR"),
      BoardExtraFeature(22, "Community", "Stolen PEV Recovery Broadcast", "Publish an instant Stolen PEV BOLO alert with rig details to the live Board.", "POST BOLO", "BOARD:COMPOSE_STOLEN"),
      BoardExtraFeature(23, "Community", "Trail Hazard Crowdsourced Reporter", "Post live road debris, washout, or creek hazards straight to the Porch Board.", "POST ALERT", "BOARD:COMPOSE:ALERTS"),
      BoardExtraFeature(24, "Community", "Leaderboard Trophy & XP Hall", "Inspect real verified rider XP, total distance & ride counts on Top Riders.", "TOP RIDERS", "BOARD:TAB:STANDINGS"),
      BoardExtraFeature(25, "Community", "Classifieds PEV Marketplace", "Browse live rider-to-rider PEV & parts listings on the Porch Board.", "MARKETPLACE", "BOARD:CAT:MARKETPLACE"),
      BoardExtraFeature(26, "Community", "Lost & Found / Gear Exchange", "Search live Board posts for gear, chargers, helmets, and trail parts.", "SEARCH FEED", "BOARD:SEARCH:charger"),
      BoardExtraFeature(27, "Community", "Group Ride Dispatcher & Convoy", "Browse or host live rural group rides and local convoys on the Board.", "GROUP RIDES", "BOARD:CAT:GROUP_RIDES"),
      BoardExtraFeature(28, "Community", "Community Polls & Voting Hub", "Filter active votable live Firestore polls on the Porch Board.", "OPEN POLLS", "BOARD:FILTER:POLLS"),
      BoardExtraFeature(29, "Community", "Slang & Technical Acronym Decoder", "Open the instant PEV glossary (FOC, ESC, BMS, VESC, PWM, Wh/mi).", "OPEN GLOSSARY", "BOARD:SLANG"),
      BoardExtraFeature(30, "Community", "Direct Messaging & WebRTC Calls", "Open live encrypted peer-to-peer DMs, voice notes & audio/video calls.", "OPEN DMs", "BOARD:DM_HUB"),
      BoardExtraFeature(31, "Toolkit", "Bolt Torque & Trail Power Toolkit", "Open Cockpit Trail Power Toolkit for bolt torque charts, horn & survival tools.", "OPEN COCKPIT", "TAB:COCKPIT"),
      BoardExtraFeature(32, "Toolkit", "Gear Ratio & Top Speed Calculator", "Compute motor KV, gear ratios, Wh/mi & speed envelopes in Shop Calc.", "OPEN BENCH", "TAB:MECHANIC"),
      BoardExtraFeature(33, "Toolkit", "Morse Code Flashlight Transceiver", "Use Torch Morse TX & CameraX luminance Morse RX via Cockpit Trail Toolkit.", "OPEN COCKPIT", "TAB:COCKPIT"),
      BoardExtraFeature(34, "Toolkit", "First Aid & Backcountry Safety", "Access rural safety checklists and AI emergency briefings in Co-Pilot.", "OPEN COPILOT", "TAB:COPILOT"),
      BoardExtraFeature(35, "Toolkit", "Live Weather & Wind Headwind Index", "View real-time Open-Meteo wind, rain, barometric & range derate on Cockpit.", "OPEN COCKPIT", "TAB:COCKPIT"),
      BoardExtraFeature(36, "Toolkit", "Trail TV, Radio & Music Mixes", "Stream live YouTube Rural ERides videos and riding music mixes in Channel.", "OPEN CHANNEL", "TAB:CHANNEL"),
      BoardExtraFeature(37, "Toolkit", "HUD Skins & Theme Preview Studio", "Switch between 26 visual themes, ${com.example.data.HudStyle.entries.size} HUD skins & nav layouts in Settings.", "OPEN SETTINGS", "OVERLAY:SETTINGS"),
      BoardExtraFeature(38, "Toolkit", "Ride Timeline Replay Scrubber", "Scrub through recorded ride speed, elevation & power curves in Telemetry Studio.", "OPEN STUDIO", "OVERLAY:TELEMETRY_STUDIO"),
      BoardExtraFeature(39, "Toolkit", "Release Notes & System Diagnostics", "Inspect full release notes, 9-DOF sensor probe & live service status in About.", "OPEN ABOUT", "OVERLAY:INFO"),
      BoardExtraFeature(40, "Toolkit", "Export JSON Vault & Cloud Backup", "Export or restore full garage, rides & settings JSON in Cloud Vault Settings.", "OPEN SETTINGS", "OVERLAY:SETTINGS"),
      BoardExtraFeature(41, "Pro Suite", "AI Trail Mechanic & Co-Pilot", "Chat with 20 live Gemini personas with real telemetry & weather context.", "OPEN COPILOT", "TAB:COPILOT"),
      BoardExtraFeature(42, "Pro Suite", "Creator Studio & AI Image Lab", "Generate/edit trail art, compose music & run Maps/Web grounding in Creator Hub.", "CREATOR HUB", "OVERLAY:CREATOR_HUB"),
      BoardExtraFeature(43, "Pro Suite", "Custom Synthesized Voice Studio", "Create custom Gemini neural Co-Pilot voices with DSP filters & mic samples.", "VOICE STUDIO", "OVERLAY:VOICE_STUDIO"),
      BoardExtraFeature(44, "Pro Suite", "3D CAD Garage & Dyno Viewer", "Inspect 88 PEV rigs in OpenGL ES / WebGL / CAD Blueprint 3D Lab.", "OPEN 3D LAB", "POPUP:3D_LAB"),
      BoardExtraFeature(45, "Pro Suite", "Legal Class Speed Governor", "Configure Class 1/2/3/Off-Road speed compliance & school zones in Settings.", "OPEN SETTINGS", "OVERLAY:SETTINGS"),
      BoardExtraFeature(46, "Pro Suite", "Multi-Vehicle Garage Fleet Manager", "Add, switch & track multiple PEV rigs with auto specs & logos on Cockpit.", "OPEN COCKPIT", "TAB:COCKPIT"),
      BoardExtraFeature(47, "Pro Suite", "Night Vision & Red-Shift Mode", "Toggle high-contrast night riding & red-shift themes inside Appearance Settings.", "OPEN SETTINGS", "OVERLAY:SETTINGS"),
      BoardExtraFeature(48, "Pro Suite", "Energy Economy Range Simulator", "Run weight, grade, temperature & surface range simulations in Rig Specs.", "OPEN BENCH", "TAB:MECHANIC"),
      BoardExtraFeature(49, "Pro Suite", "Firestore Cloud Sync & Restore", "Sync all vehicles, rides, settings & Board data with your Google account.", "OPEN SETTINGS", "OVERLAY:SETTINGS"),
      BoardExtraFeature(50, "Pro Suite", "Hardware, Satellite & API Diagnostics", "Run live GNSS satellite, 9-DOF sensor, battery & HTTP endpoint diagnostics.", "OPEN ABOUT", "OVERLAY:INFO"),
    )
  }

  val filteredFeatures = remember(searchQuery, selectedCategory, features) {
    features.filter { f ->
      (selectedCategory == "All" || f.category == selectedCategory) &&
        (searchQuery.isBlank() ||
          f.title.contains(searchQuery, ignoreCase = true) ||
          f.description.contains(searchQuery, ignoreCase = true) ||
          f.category.contains(searchQuery, ignoreCase = true) ||
          f.actionLabel.contains(searchQuery, ignoreCase = true))
    }
  }

  Scaffold(
    topBar = {
      TacticalTopBar(
        title = "50 Live Command Modules",
        subtitle = "Tap any module to launch live",
        onBack = onNavigateBack,
        backTestTag = "fifty_features_back_button",
        icon = Icons.Default.ElectricBolt,
        badgeText = "${filteredFeatures.size} LIVE",
      )
    },
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding).imePadding(),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(modifier = Modifier.fillMaxSize().widthIn(max = 860.dp)) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
          if (searchQuery.isNotBlank()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Default.Close, contentDescription = "Clear search")
            }
          }
        },
        placeholder = { Text("Search 50 live tools (radar, 3D lab, BOLO, Morse, voice)...") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag("fifty_features_search_input"),
      )

      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(categories) { cat ->
          FilterChip(
            selected = selectedCategory == cat,
            onClick = { selectedCategory = cat },
            label = { Text(cat) },
            modifier = Modifier.testTag("fifty_cat_${cat.lowercase().replace(" ", "_")}"),
          )
        }
      }

      Spacer(Modifier.height(6.dp))

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(filteredFeatures, key = { it.id }) { feat ->
          Surface(
            onClick = {
              onNavigateBack()
              onExecuteFeatureRoute(feat.routeKey)
            },
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            shape = chrome.cardShape,
            border = BorderStroke(chrome.cardBorderWidth, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth().testTag("fifty_feature_card_${feat.id}"),
          ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f),
                ) {
                  Icon(
                    Icons.Default.ElectricBolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                  )
                  Text(
                    "#${feat.id}. ${feat.title}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                }
                Surface(
                  color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                  shape = chrome.smallShape,
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Text(
                      feat.actionLabel,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                      Icons.AutoMirrored.Filled.OpenInNew,
                      contentDescription = "Launch ${feat.title}",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(12.dp),
                    )
                  }
                }
              }
              Text(feat.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("Category: ${feat.category}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
          }
        }
      }
    }
    }
  }
}
