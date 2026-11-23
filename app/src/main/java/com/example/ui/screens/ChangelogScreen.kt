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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.MasterSettings
import com.example.ui.components.TacticalTopBar
import com.example.ui.theme.LocalTacticalChrome

data class BuildChangelogEntry(
  val versionCode: Int,
  val versionName: String,
  val date: String,
  val title: String,
  val highlights: List<String>,
)

@Composable
internal fun ChangelogScreen(
  settings: MasterSettings,
  onNavigateBack: () -> Unit,
) {
  val builds = remember {
    listOf(
      BuildChangelogEntry(
        versionCode = 21,
        versionName = "21.0",
        date = "November 2026",
        title = "Optical Morse Transceiver & Telemetry Studio Pro",
        highlights = listOf(
          "Added Morse Code Flashlight TX: Type any trail message and pulse rear LED or visual preview.",
          "Added Camera Morse Code RX: Live CameraX Y-plane luminance frame analyzer with target reticle to decode Morse signals from other riders.",
          "Upgraded Ride Telemetry Charts Pro with 10 interactive Neon graphs, synchronized timeline playback scrubber, and fleet benchmark spider chart.",
          "Eliminated graph freeze bugs by optimizing canvas render cycles, touch scrubbing bounds, and state memoization.",
          "Added 50+ granular settings, theme presets, and robust offline data persistence.",
        ),
      ),
      BuildChangelogEntry(
        versionCode = 20,
        versionName = "20.0",
        date = "October 2026",
        title = "Cloud Persistence & Zero-Trust Firebase Firestore",
        highlights = listOf(
          "Configured real Firebase Firestore database and Authentication via Jetpack Credential Manager.",
          "Added live multiplayer rider radar, shared route waypoints, and direct team messaging.",
          "Implemented offline fallback cache and encrypted local data synchronization.",
        ),
      ),
      BuildChangelogEntry(
        versionCode = 19,
        versionName = "19.0",
        date = "September 2026",
        title = "Cockpit HUD & Offline Osmdroid Map Engine",
        highlights = listOf(
          "Upgraded HUD with 14 switchable skins (Cyber Orb, F1 Telemetry, Fighter Jet, Analog Dial, etc.).",
          "Added RainViewer live doppler radar overlay and open-source vector map tile caching.",
          "Integrated voice command hotwords and automated battery sag compensation.",
        ),
      ),
      BuildChangelogEntry(
        versionCode = 18,
        versionName = "18.0",
        date = "August 2026",
        title = "Rural ERides GO Initial Architecture & Pev Physics",
        highlights = listOf(
          "Core Jetpack Compose Material 3 architecture setup.",
          "PEV battery chemistry simulation (LiFePO4, NMC, Solid-State) and motor thermals.",
          "Diagnostic wrench toolkit, bolt torque specs, and gear ratio calculator.",
        ),
      ),
    )
  }

  Scaffold(
    topBar = {
      TacticalTopBar(
        title = "Build Changelog & History",
        onBack = onNavigateBack,
        icon = Icons.Default.Build,
        badgeText = "${builds.size} RELEASES",
      )
    },
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      items(builds) { build ->
        BuildCard(build = build)
      }
    }
  }
}

@Composable
private fun BuildCard(build: BuildChangelogEntry) {
  val chrome = LocalTacticalChrome.current
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
    shape = chrome.cardShape,
    border = BorderStroke(chrome.cardBorderWidth, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
          Text("v${build.versionName} (Code ${build.versionCode})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        }
        Text(build.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }

      Text(build.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

      Spacer(Modifier.height(2.dp))

      build.highlights.forEach { highlight ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(16.dp).padding(top = 2.dp))
          Text(highlight, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        }
      }
    }
  }
}
