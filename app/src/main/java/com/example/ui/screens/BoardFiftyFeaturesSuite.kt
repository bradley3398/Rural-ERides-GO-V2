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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
  val status: String,
)

@Composable
internal fun BoardFiftyFeaturesScreen(
  settings: MasterSettings,
  onNavigateBack: () -> Unit,
) {
  val chrome = LocalTacticalChrome.current
  val features = remember {
    listOf(
      BoardExtraFeature(1, "Diagnostics", "AI Fleet Battery Health Clinic", "Deep cycle impedance testing and state-of-health cell balancing analysis.", "Active"),
      BoardExtraFeature(2, "Diagnostics", "Motor Hall Sensor Phase Test", "Real-time oscilloscope inspection of brushless motor commutation signals.", "Ready"),
      BoardExtraFeature(3, "Diagnostics", "Controller PWM Frequency Tuner", "Adjust motor controller pulse width modulation for silent or torque-heavy drive.", "Optimized"),
      BoardExtraFeature(4, "Diagnostics", "BMS Cell Voltage Delta Monitor", "Live per-cell voltage discrepancy warning and thermal runaway guard.", "Monitoring"),
      BoardExtraFeature(5, "Diagnostics", "Voltage Sag Dynamic Predictor", "Calculates internal pack resistance under heavy hill climbs.", "Calibrated"),
      BoardExtraFeature(6, "Diagnostics", "Regenerative Braking Profiler", "Fine-tunes regen torque curve to protect MOSFETs during high-speed decel.", "Active"),
      BoardExtraFeature(7, "Diagnostics", "Hub Motor Thermal Runaway Alarm", "Audible and haptic alert when stator temperature exceeds 85C.", "Armed"),
      BoardExtraFeature(8, "Diagnostics", "Charger Efficiency & Ripple Tester", "Measures AC-DC charger ripple and output wattage stability.", "Ready"),
      BoardExtraFeature(9, "Diagnostics", "Suspension Sag & Rebound Calculator", "Calculates preload settings based on pilot weight and riding terrain.", "Ready"),
      BoardExtraFeature(10, "Diagnostics", "Tire Pressure & Puncture Predictor", "Real-time PSI telemetry logging with slow leak detection algorithms.", "Monitoring"),
      BoardExtraFeature(11, "Navigation", "Offline Vector Map Tile Cacher", "Downloads regional topographic and county road grids for zero-signal rides.", "Cached"),
      BoardExtraFeature(12, "Navigation", "RainViewer Doppler Radar Stream", "Live meteorological precipitation overlays projected directly onto map routes.", "Live"),
      BoardExtraFeature(13, "Navigation", "GPS Geofence Perimeter Alarm", "Triggers siren and SOS beacon if PEV moves outside secure parking zone.", "Armed"),
      BoardExtraFeature(14, "Navigation", "Anti-Theft Motion & Shock Beacon", "Uses IMU accelerometer to detect unauthorized tampering or towing.", "Armed"),
      BoardExtraFeature(15, "Navigation", "Turn-by-Turn Audio Navigation", "Synthesized voice cues for backcountry trail intersections and charging stops.", "Enabled"),
      BoardExtraFeature(16, "Navigation", "Live Multiplayer Rider Radar", "Peer-to-peer telemetry mesh showing nearby community pilots in real-time.", "Online"),
      BoardExtraFeature(17, "Navigation", "Elevation Profile Grade Predictor", "Analyzes upcoming trail incline to recommend optimal throttle/assist torque.", "Active"),
      BoardExtraFeature(18, "Navigation", "Emergency SOS Satellite Relay", "Automated crash detection with GPS coordinate broadcast via cellular/mesh.", "Standby"),
      BoardExtraFeature(19, "Navigation", "Charger Station Availability Finder", "Live status of rural 240V RV pedestals, NEMA outlets, and fast chargers.", "Synced"),
      BoardExtraFeature(20, "Navigation", "GPX Route Importer & Exporter", "Import custom backcountry GPX tracks and export recorded rides.", "Ready"),
      BoardExtraFeature(21, "Community", "Convoy Voice Walkie-Talkie Channel", "Push-to-talk encrypted audio communication with riding group members.", "Ready"),
      BoardExtraFeature(22, "Community", "Stolen PEV Recovery Broadcast", "Instant community alert with serial number and last known GPS ping.", "Ready"),
      BoardExtraFeature(23, "Community", "Trail Hazard Crowdsourced Reporter", "Report washed-out bridges, fallen trees, and gravel traps to community feed.", "Active"),
      BoardExtraFeature(24, "Community", "Leaderboard Trophy & XP Hall", "Global and regional rankings for distance, elevation climb, and energy efficiency.", "Updated"),
      BoardExtraFeature(25, "Community", "Classifieds PEV Marketplace", "Buy, sell, and trade electric unicycles, e-bikes, scooters, and parts.", "Active"),
      BoardExtraFeature(26, "Community", "Lost & Found Gear Tracker", "Community bulletin board for dropped chargers, helmets, and accessories.", "Active"),
      BoardExtraFeature(27, "Community", "Group Ride Dispatcher & RSVP", "Organize weekend backcountry group rides with route maps and safety briefings.", "Ready"),
      BoardExtraFeature(28, "Community", "Community Polls & Feedback Hub", "Vote on new trail access rights, accessory reviews, and feature requests.", "Open"),
      BoardExtraFeature(29, "Community", "Slang & Technical Acronym Decoder", "Quick reference guide for PEV terminology (FOC, ESC, BMS, VESC, Wh/mi).", "Loaded"),
      BoardExtraFeature(30, "Community", "Direct Messaging & Encrypted Chat", "Private peer-to-peer messaging with riding buddies and trail mechanics.", "Connected"),
      BoardExtraFeature(31, "Toolkit", "Bolt Torque Quick Reference", "Essential torque specs for axle nuts, handlebars, brake calipers, and stems.", "Loaded"),
      BoardExtraFeature(32, "Toolkit", "Gear Ratio & Top Speed Calculator", "Calculates theoretical top speed based on pole pairs, KV rating, and wheel size.", "Ready"),
      BoardExtraFeature(33, "Toolkit", "Morse Code Flashlight Transceiver", "Flashlight encoder and camera Y-plane decoder for optical emergency signaling.", "Ready"),
      BoardExtraFeature(34, "Toolkit", "First Aid & Wilderness Survival Guide", "Emergency medical reference for trail injuries, tourniquets, and snakebites.", "Loaded"),
      BoardExtraFeature(35, "Toolkit", "Campfire Weather & Wind Index", "Real-time barometric pressure, wind gusts, and humidity for backcountry camps.", "Live"),
      BoardExtraFeature(36, "Toolkit", "Custom Horn & Soundboard", "Acoustic warning sounds and digital air-horn generator for trail safety.", "Ready"),
      BoardExtraFeature(37, "Toolkit", "HUD Neon Color Customizer", "Personalize cockpit accent hues, glowing borders, and neon opacity.", "Applied"),
      BoardExtraFeature(38, "Toolkit", "Ride Timeline Replay Scrubber", "Scrub through recorded ride speed, elevation, and power curves interactively.", "Ready"),
      BoardExtraFeature(39, "Toolkit", "Firmware Flasher & Version Log", "Changelog and release notes tracker for controller and display firmware.", "Updated"),
      BoardExtraFeature(40, "Toolkit", "Export CSV Telemetry Logs", "Export raw ride logs for spreadsheet analysis and mechanical auditing.", "Ready"),
      BoardExtraFeature(41, "Pro Suite", "AI Trail Chaplain Mechanic Copilot", "Gemini-powered diagnostic assistant for electrical faults and error codes.", "Online"),
      BoardExtraFeature(42, "Pro Suite", "Autonomous Trail Follower Assist", "Predictive trajectory smoothing for rough off-road terrain.", "Calibrated"),
      BoardExtraFeature(43, "Pro Suite", "Crash Detection G-Force Calibrator", "Adjusts accelerometer sensitivity to prevent false alarms during drop-offs.", "Optimized"),
      BoardExtraFeature(44, "Pro Suite", "Offline Bluetooth Mesh Sync", "Synchronize ride records and chat messages via BLE mesh when offline.", "Ready"),
      BoardExtraFeature(45, "Pro Suite", "Dynamic Speed Limiter Unlocker", "Trail vs street geofenced speed profile switching for legal compliance.", "Compliant"),
      BoardExtraFeature(46, "Pro Suite", "Multi-Vehicle Garage Fleet Manager", "Manage specs, batteries, and maintenance logs for multiple PEVs.", "Active"),
      BoardExtraFeature(47, "Pro Suite", "Night Vision HUD Monochrome Mode", "Reduces eye strain with high-contrast red/green monochrome cockpit themes.", "Ready"),
      BoardExtraFeature(48, "Pro Suite", "Energy Economy Efficiency Optimizer", "Real-time Wh/mi coaching to maximize range on long backcountry loops.", "Active"),
      BoardExtraFeature(49, "Pro Suite", "Backup & Restore Cloud Snapshot", "Cloud sync of all rides, vehicle profiles, and custom preferences.", "Synced"),
      BoardExtraFeature(50, "Pro Suite", "Advanced Developer Telemetry Debugger", "Raw CAN-bus packet viewer and CPU thermal monitor for developers.", "Ready"),
    )
  }

  Scaffold(
    topBar = {
      TacticalTopBar(
        title = "50 Advanced Board Features Suite",
        onBack = onNavigateBack,
        icon = Icons.Default.ElectricBolt,
        badgeText = "50 MODULES",
      )
    },
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      itemsIndexed(features) { idx, feat ->
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
          shape = chrome.cardShape,
          border = BorderStroke(chrome.cardBorderWidth, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text("#${feat.id}. ${feat.title}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
              }
              Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                shape = chrome.smallShape,
              ) {
                Text(
                  feat.status,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
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
