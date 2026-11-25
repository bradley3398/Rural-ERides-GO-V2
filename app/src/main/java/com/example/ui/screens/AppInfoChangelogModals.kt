package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.core.ApiKeyRing
import com.example.core.LiveCloudServices
import com.example.ui.components.TacticalCard
import com.example.ui.components.TacticalOutlinedButton
import com.example.ui.components.TacticalTopBar
import com.example.viewmodel.CockpitViewModel
import com.example.viewmodel.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInfoAndChangelogScreen(
  sessionVm: SessionViewModel,
  cockpitVm: CockpitViewModel,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)
  val ctx = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val telemetry by cockpitVm.telemetry.collectAsStateWithLifecycle()
  val diagnosticResults by sessionVm.diagnosticResults.collectAsStateWithLifecycle()
  val diagnosticsBusy by sessionVm.diagnosticsBusy.collectAsStateWithLifecycle()

  var versionTapCount by remember { mutableIntStateOf(0) }
  var showConsole by remember { mutableStateOf(false) }
  var cmdInput by remember { mutableStateOf("") }
  var consoleOutput by remember {
    mutableStateOf("Rural ERides Kernel Shell v${BuildConfig.VERSION_NAME} ready.\nType: help | dev | sysinfo | ahrs | sensors | battery | memory | telemetry | keys | clear")
  }
  var hwSnap by remember { mutableStateOf(collectHardwareDiagnostics(ctx)) }
  var notice by remember { mutableStateOf("") }

  val changelog =
    remember {
      listOf(
        "v52.0 (Build 52) — Android 14 Tablet Multi-Layer Google Sign-In Fix" to
          "• Android 14 Tablet Google Sign-In Recovery: Fixed silent-vs-interactive Credential Manager race conditions, upgraded androidx.credentials to 1.5.0, preserved MainActivity across tablet config/orientation changes, and added automatic fallback to the native Play Services Google Account Picker.\n" +
          "• Dedicated Google Account Picker Button: Added direct Play Services account picker login alongside hardened Google Browser Sign-In (Custom Tabs <queries> + ON_RESUME pending OAuth recovery).",
        "v51.0 (Build 51) — Direct GitHub Repo Build Notifications (Play Creator Compliant)" to
          "• 1-Tap Notification to GitHub Repo Page: Tapping a new-build Android notification opens the official GitHub repository release page directly in your browser—no restricted install permissions, 100% Google Play Creator Program compliant.\n" +
          "• GitHub Repo Default Update Channel: Settings -> App Updates & New Builds defaults to opening the GitHub Repo Page (with Google Play also available anytime).",
        "v50.0 (Build 50) — Live New-Build Notifications & In-App Update Center" to
          "• Automatic New-Build Notifications: Checks live GitHub Releases on launch and in the background via WorkManager, sending a high-priority Android notification whenever a new build is published.\n" +
          "• Settings -> App Updates & New Builds: Dedicated update center with 1-tap Check Now, Auto-Check toggle, New Build Alert toggle, Test Notification button, check cadence (6h/12h/24h), Beta/Pre-Release opt-in, and preferred update source (Google Play or GitHub Releases).\n" +
          "• In-App New-Build Banner & Update Dialog: Tapping a build notification or banner shows full release notes and jumps directly to Google Play or the official GitHub Release in your browser.",
        "v49.0 (Build 49) — Zero-Crash Studio 3D Lab, Non-Overlapping Layouts & Google Sign-In Recovery" to
          "• Visible Build Stamp on Launch: Displays live v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}) directly on the login screen and in About & Diagnostics so every new build is easy to verify.\n" +
          "• Zero-Crash Studio 3D PBR Rig Lab: Runs smoothly in-window with 3-point studio lighting, ACES Filmic tone mapping, and hardened OpenGL ES PBR & WebGL 2.0 automatic fallbacks.\n" +
          "• Non-Overlapping Screen Layout Overhaul: Added safe-area status and navigation bar padding across all 6 primary tabs, redesigned Porch Talk header without truncation or overlapping FABs, and added roomy spacing.\n" +
          "• Restored Google Sign-In Default + Browser Backup: Restored Google + Firebase as the default login tab with Activity-scoped Credential Manager sign-in and a built-in Google Browser Sign-In fallback.",
        "v47.0 Studio 3D PBR Lab, Crash-Proof Ride Inspector & Holiday Ornaments" to
          "• Zero-Crash Studio 3D PBR Rig Lab: Replaced Filament/Vulkan native crashes with an in-window Studio 3D PBR engine (3-point studio lighting, ACES Filmic tone mapping, softbox specular sheen) plus hardened OpenGL ES PBR and WebGL 2.0 GLB viewports with automatic safe fallback.\n" +
          "• Upgraded Telemetry Studio & Crash-Proof Ride Inspector: Replaced the legacy stacked Per-Ride Deep Dive with a focused 5-module Ride Inspector (Speed Curve, Climb Profile, Energy & Pack, GPS Route, Replay & AI), added a 1-tap Personal Records Ribbon, live KPI subtitles on all 11 Fleet Graph Windows, and NaN/zero-width Canvas crash guards.\n" +
          "• Holiday Themes Ornaments & Decorations: Unlocked all 11 Holiday themes year-round with DP-scaled top-canopy garlands, twinkling string lights, hanging ornaments, foreground card corner emblems, and festive nav bar trim.\n" +
          "• Duplicate Button Cleanup & Official GitHub Links: Removed redundant UI buttons across 3D Lab, Ride Logbook, and Graph modals, updated all links to github.com/bradley3398/Rural-Erides-GO, and restored GitHub Pages & Release Workflows.",
        "v44.0 Live Command Suite, Timeline Stories & Real Connection Probes" to
          "• 50-Module Interactive Command Directory: Replaced static feature list with live search, category filters, and real 1-tap routing across Cockpit, Radar, Mechanic, Co-Pilot, Board, Telemetry Studio, 3D CAD Lab, and Settings.\n" +
          "• Live 24h Rider Timeline Stories: Restored live Firestore-backed 24-hour trail stories on the Porch Board with zero-permission Photo Picker uploads and live battery telemetry badges.\n" +
          "• Real Live Server Sync & HTTP Diagnostics: Upgraded Board Refresh to pull fresh server snapshots from Firestore and upgraded Live Diagnostics to run genuine timed HTTP pings across Open-Meteo, RainViewer, Nominatim, Overpass, OSRM, Open-Elevation, YouTube RSS, and Electrek News.\n" +
          "• Unified Complete Changelog (v7.0–v44.0): Consolidated full release history into About & Diagnostics with direct access from Settings.",
        "v39.0–v43.0 Compartmentalized Architecture, 3D CAD Lab & Play Compliance" to
          "• Compartmentalized Host Architecture: Isolated Cockpit, Radar, Mechanic, Co-Pilot, Community, 3D CAD Lab, and Telemetry Studio compartments for rock-solid lifecycle stability.\n" +
          "• Creator Arcade Hub & Gravel Volt 3D Runner: Live Firebase AI Logic Image Studio, Voice Live, Search/Maps Grounding, Trail Music Lab, and hardware-accelerated 3D arcade runner.\n" +
          "• Zero Client-Side Secret Exposure: Keyless App Check attested Gemini AI Logic + Firebase Auth/Firestore + open-data live telemetry endpoints.",
        "v38.0 Custom Synthesized Voice Studio & Zero-Latency TTS Buffer" to
          "• Custom AI Voice Studio: Synthesize bespoke Co-Pilot voices with reference audio clips, mic recording, pitch/rate tuning, 10 Gemini neural bases, and 8 DSP filters.\n" +
          "• Zero-Latency Streaming Pre-Load Buffer: Splits sentences as Gemini streams into an async PCM buffer for instant gapless playback.",
        "v37.0 Tri-Channel Push Notifications & Tactile Accessibility Audit" to
          "• Tri-Channel Notifications: Deep-linked alerts for AI Co-Pilot/Mechanic, Community Board dispatches, and Direct Messages.\n" +
          "• Global UI Debounce & Accessible 48dp Touch Targets across all Tactical controls.",
        "v36.0 Multi-Engine 3D Viewport Lifecycle & Reactive Telemetry Studio" to
          "• 3D Viewport Lifecycle Manager: Clean GPU surface pause/release across OpenGL ES, WebGL, and Compose 3D viewports.\n" +
          "• Reactive RideTelemetryViewModel driving 10+ Neon analytics charts with zero-crash bounds.",
        "v21.0 Optical Morse Transceiver & Telemetry Studio Pro" to
          "• Torch Morse Code TX & CameraX Y-plane luminance Morse RX inside Trail Power Toolkit.\n" +
          "• 10 interactive Neon ride graphs with synchronized timeline playback scrubber.",
        "v18.0–v20.0 Zero-Trust Firestore Cloud Sync, 14 HUD Skins & PEV Physics" to
          "• Google Credential Manager sign-in with zero-trust Firestore security rules, multiplayer Rider Radar, and offline Room vault.\n" +
          "• 14 switchable Cockpit HUD skins, RainViewer Doppler storm radar, and LiFePO4/NMC battery physics engine.",
        "v14.0 Rural Heritage, Live Bench Vision, Brand Logos & Ride Analytics" to
          "• Original Rural ERides GO Launcher Icon Restored: Authentic olive-hill pine & electric scooter adaptive icon across all Android densities.\n" +
          "• Live Maintenance Bench Camera ('Bench Eyes') in AI Mechanic: Embedded live CameraX workbench viewer with optical reticle and 1-tap 'Inspect What I'm Working On' Gemini Vision AI diagnosis, plus 6 new rural/roadside workshop presets.\n" +
          "• AI Co-Pilot Camera & Photo Upload + 20 Rural/PEV Personas & 8 Voice Types: Capture live photos or upload from gallery directly in Co-Pilot, pick from 8 warm rural/country personas (Country Trail Boss, Barn Shop Grandpa, Backroad Rancher Guide, Gravel Creek Scout, Homestead Solar Tech & more), toggle Warm Rural Hospitality mode, and choose from 8 distinct spoken TTS voice profiles.\n" +
          "• Automatic PEV Photo & Official Brand Logo Fetching: Creating or retrieving a PEV automatically fetches both its real vehicle photo AND official brand logo emblem — shown on PEV Profiles, Cockpit Garage, Spec Engine cards, and Spec Engine Detail Sheets.\n" +
          "• Ride Records Analytics Suite: Interactive Jetpack Compose Canvas Pie/Donut charts (By Vehicle / Speed Zones / Energy Wh), Peak vs Avg Speed line graph, Distance & Wh bar chart, 2-Ride side-by-side comparison, and smooth-panning GPS Track Map.\n" +
          "• Smooth Panning Fixed Across ALL Maps: Touch-scroll interception, fling inertia, floating Zoom +/-, and RE-CENTER / LOCK GPS controls on Rider Radar, Cockpit Breadcrumb Map, and Ride Track Maps, plus Rural Trail Quick Waypoint Drop Bar & ETA Battery-at-Arrival.\n" +
          "• 6 New Rural Visual Themes (26 Total) & 8 Navigation Panel Layouts: Barnwood, Sunset Gravel, Pine Holler, Prairie Gold, Creek Mist, and Harvest Iron themes + Bottom Pill, Rural Timber, Floating Dock, Icon Capsule, Top Ribbon, Left Rail, Right Rail, and Win95 Taskbar.\n" +
          "• Zero Fake/Stock-Face Pilot Profiles: Hardened pilot authenticity verification strips synthetic IDs and stock-face avatars everywhere; upgraded Board with Rural Trail Condition tags, Channel with Rural PEV/Country Music presets & 1-tap Share, and Mini-Player with quick speed cycling.\n" +
          "• Created & Architected by Lord Bradley Callison.",
        "v12.0 Tactical Neon & System Architecture Overhaul" to
          "• High-Contrast 'Tactical Neon' 9-DOF AHRS Telemetry Dashboard in Jetpack Compose with spring-animated Pitch, Roll (peak lean hold + lean-angle alert), and 360° Yaw needle gauges + live Artificial Horizon & All-Axis Tare.\n" +
          "• Real 9-DOF Sensor Fusion Engine (Rotation Vector + Gyroscope + Accelerometer + Magnetometer) with live °/s angular rates & µT magnetic flux.\n" +
          "• OS-Level Material You Themed App Icon (Android 13+ monochrome adaptive layer matching system wallpaper & accent theme).\n" +
          "• 6 New PEV Visual Themes (Tactical Neon, Apex Track, Gravel Dust, Titanium Pro, Lithium Lab, Night Convoy) & 4 New HD PEV Wallpapers (Night Hyper-Track, Carbon VESC Circuit, Alpine Twilight eMTB, Cyber City EUC).\n" +
          "• Zero Fake Data Policy: Strict authentic-pilot verification across the Board — zero mock/demo/test pilots or placeholder posts.\n" +
          "• AI Mechanic & AI Co-Pilot Major Upgrade: Spacious full-screen auto-scrolling chat viewports, roomy multi-line input bars with Voice Dictation, collapsible toolbars, and 100% live-wired buttons (instant session creation & 1-tap live preset diagnostics).\n" +
          "• Android System Settings Category Matrix: Reorganized Settings into native Android Settings categories with live summaries, global instant search, AHRS/Chat/Display controls, and Developer Info crediting Lord Bradley Callison.\n" +
          "• Expanded Hardware & Cloud Diagnostics Suite: Live 9-DOF IMU inventory, battery temp/voltage/health, network throughput, SoC thermal envelope, RAM/storage, and 1-tap shareable diagnostic reports.",
        "v11.0 Authentic Retro OS Themes, Expanded HUDs & Board Redesign" to
          "• Authentic Windows XP Luna & Windows 7 Aero glass visual engines with faithful window chrome, glossy title bars, and retro sound cues.\n" +
          "• Expanded Cockpit HUD Suite with integrated live Magnetic/GPS Compass rose across HUD gauges.\n" +
          "• Welcoming Rider Login Command Gate with Google Credential Manager + Supabase GoTrue password authentication & password reset.\n" +
          "• Redesigned Spacious Community Board feed with full-bleed photos, polls, marketplace pricing, SOS triage, threaded comments, DMs, and WebRTC calls.",
        "v10.0 Pro Navigation, Radar & P2P Tactical Comms" to
          "• Dedicated PEV Turn-by-Turn GPS Navigation (Mapbox + OpenRouteService + OSRM fallback) with live voice guidance and elevation profiling.\n" +
          "• Live RainViewer & OpenWeather Doppler Storm Radar overlays, OpenChargeMap / Overpass EV & PEV public charger locator, Geo-Fence perimeter alarms, and Target Lock bearing.\n" +
          "• Real-time PCM Push-to-Talk (PTT) Squad Walkie-Talkie and WebRTC Voice/Video rider calling.",
        "v9.0 AI Vision AR Mechanic, 17-Bay Lab & Co-Pilot Pro" to
          "• CameraX + Android Photo Picker Vision AR Hardware Diagnostics with FLIR thermal filter, anomaly bounding boxes, wiring harness pinout auto-mapper, interactive repair checklists, and OEM E-Code decoder.\n" +
          "• 17-Bay PEV Engineering Calculator Lab, 4-Model Spec Comparison Engine, and 48+ Store Parts BOM Finder.\n" +
          "• Grounded AI Co-Pilot powered by Firebase AI Logic (Gemini) with 12 expert PEV personas, 100 live skills, 5 interactive wizards, web grounding, and offline Saved Briefings Vault.",
        "v8.0 Telemetry Cockpit, Safety SOS & Weather Intel" to
          "• 1Hz–5Hz FusedLocation GPS speedometer, 10Hz–50Hz IMU crash detection with auto-cancelable SOS SMS & emergency siren, Return-to-Base homing beacon, and legal class speed governor.\n" +
          "• Live Road Speed Limit detection (TomTom + Overpass OSM) with school-zone strict alerts.\n" +
          "• 6-Tab Open-Meteo PEV Meteorology (wind headwind impact, rain timeline, road surface grip, visibility, and cold-battery range derate).",
        "v7.0 Garage Fleet, Maintenance Logbook & Media Hub" to
          "• Multi-PEV Garage manager with AI Spec Lookup, learned Wh/km end-of-ride battery prediction, component wear meters, and persistent maintenance/service logbooks.\n" +
          "• Integrated YouTube PEV Video & Music Mini-Player with keyless RSS/web resilience, PEV News Wire, and offline Mission Vault JSON export/import.",
      )
    }

  Scaffold(
    topBar = {
      TacticalTopBar(
        title = "ABOUT, DIAGNOSTICS & CHANGELOG",
        onBack = onBack,
        backTestTag = "info_back_button",
        icon = Icons.Default.VerifiedUser,
        badgeText = "v${BuildConfig.VERSION_NAME}",
      )
    }
  ) { pad ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(pad),
      contentPadding = PaddingValues(14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (notice.isNotBlank()) {
        item {
          TacticalCard(highlight = true) {
            Text(notice, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
          }
        }
      }

      // 1. DEVELOPER & APP IDENTITY CARD
      item(key = "developer_info_card") {
        TacticalCard(
          highlight = true,
          onClick = {
            versionTapCount++
            if (versionTapCount >= 5) showConsole = true
          },
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
              Text(
                "RURAL ERIDES GO • v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                "Native Android PEV Telemetry, AI & Tactical Command Suite",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Spacer(Modifier.height(10.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
          Spacer(Modifier.height(10.dp))
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            Column {
              Text("DEVELOPER & CREATOR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
              Text("Lord Bradley Callison", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
              Text(
                "Founder, Lead Architect & Principal Engineer — Rural ERides GO",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Spacer(Modifier.height(8.dp))
          Text(
            "Package: ${BuildConfig.APPLICATION_ID}\nGitHub: https://github.com/bradley3398/Rural-Erides-GO\nArchitecture: Kotlin • Jetpack Compose Material 3 • Studio 3D PBR / OpenGL ES / WebGL 2.0 • 9-DOF AHRS • Firebase AI Logic • Cloud Firestore • CameraX • OSMDroid",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(Modifier.height(8.dp))
          TacticalOutlinedButton(
            onClick = {
              runCatching {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/bradley3398/Rural-Erides-GO")))
              }
            },
            modifier = Modifier.fillMaxWidth().testTag("info_open_github_repo_button"),
          ) {
            Icon(Icons.Default.Code, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("GitHub: bradley3398/Rural-Erides-GO", maxLines = 1)
          }
          Spacer(Modifier.height(8.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TacticalOutlinedButton(
              onClick = {
                clipboard.setText(
                  AnnotatedString(
                    "Rural ERides GO v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}) — Developed by Lord Bradley Callison — https://github.com/bradley3398/Rural-Erides-GO (${BuildConfig.APPLICATION_ID})"
                  )
                )
                Toast.makeText(ctx, "Copied developer & GitHub info", Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier.weight(1f).testTag("copy_developer_info_button"),
            ) {
              Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("Copy Info", maxLines = 1)
            }
            TacticalOutlinedButton(
              onClick = {
                val shareIntent =
                  Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Rural ERides GO v${BuildConfig.VERSION_NAME}")
                    putExtra(
                      Intent.EXTRA_TEXT,
                      "Rural ERides GO v${BuildConfig.VERSION_NAME} — Tactical PEV Telemetry, Studio 3D PBR Lab, 9-DOF AHRS, Radar, Vision AR Mechanic & AI Co-Pilot. Created by Lord Bradley Callison.\nhttps://github.com/bradley3398/Rural-Erides-GO",
                    )
                  }
                runCatching { ctx.startActivity(Intent.createChooser(shareIntent, "Share Rural ERides GO")) }
              },
              modifier = Modifier.weight(1f).testTag("share_app_info_button"),
            ) {
              Icon(Icons.Default.Share, null, Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("Share App", maxLines = 1)
            }
          }
          Spacer(Modifier.height(6.dp))
          Text(
            if (showConsole) "Kernel Diagnostic Console Unlocked ✓" else "Tap card 5x to unlock Kernel Diagnostic Shell ($versionTapCount/5)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // 2. LIVE 9-DOF AHRS & TELEMETRY PROBE
      item(key = "live_ahrs_probe_card") {
        TacticalCard {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.Build, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
              Text("LIVE 9-DOF AHRS & GPS PROBE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(
              onClick = {
                hwSnap = collectHardwareDiagnostics(ctx)
                notice = "Hardware & 9-DOF probe refreshed."
              },
            ) {
              Icon(Icons.Default.Refresh, contentDescription = "Refresh Probe", modifier = Modifier.size(18.dp))
            }
          }
          Text(
            "Pitch: ${"%+.1f".format(telemetry.pitchDeg)}°  •  Roll: ${"%+.1f".format(telemetry.rollDeg)}°  •  Yaw: ${telemetry.yawDeg.toInt()}°\n" +
              "Gyro (P/R/Y): ${"%+.0f".format(telemetry.gyroPitchRateDps)} / ${"%+.0f".format(telemetry.gyroRollRateDps)} / ${"%+.0f".format(telemetry.gyroYawRateDps)} °/s\n" +
              "Mag Flux: ${telemetry.magMicroTesla.toInt()} µT  •  9-DOF Fused: ${if (telemetry.ahrsFused9Dof) "ACTIVE" else "FALLBACK"}  •  Peak G: ${"%.2f".format(telemetry.peakG)}G\n" +
              "GPS: ${"%.5f".format(telemetry.lat)}, ${"%.5f".format(telemetry.lng)} (±${telemetry.gpsAccuracyM.toInt()}m)  •  Alt: ${telemetry.altitudeM.toInt()}m",
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }

      // 3. FULL LIVE HARDWARE, CLOUD API & CACHE DIAGNOSTICS
      item(key = "full_diagnostics_panel") {
        SettingsDiagnosticsAndCacheSection(
          diagnosticResults = diagnosticResults,
          diagnosticsBusy = diagnosticsBusy,
          onRunDiagnostics = { sessionVm.runLiveDiagnostics(gpsActive = telemetry.lat != 0.0 || telemetry.lng != 0.0) },
          onStatusMessage = { notice = it },
          telemetry = telemetry,
          voiceAudio = sessionVm.voiceAudio,
        )
      }

      // 4. KERNEL DIAGNOSTIC SHELL (EASTER EGG / PRO TOOL)
      if (showConsole) {
        item(key = "kernel_shell_card") {
          TacticalCard(highlight = true) {
            Text("KERNEL DIAGNOSTIC SHELL", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(consoleOutput, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = cmdInput,
                onValueChange = { cmdInput = it },
                label = { Text("Command (ahrs, sensors, battery, dev...)") },
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
              Button(onClick = {
                val c = cmdInput.trim().lowercase()
                hwSnap = collectHardwareDiagnostics(ctx)
                consoleOutput =
                  when (c) {
                    "help" -> "Commands: dev, sysinfo, ahrs, sensors, battery, memory, telemetry, keys, clear"
                    "dev" -> "Rural ERides GO v${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE}) — Developer: Lord Bradley Callison"
                    "sysinfo" -> "${hwSnap.deviceLabel} | ${hwSnap.androidRelease} | ABI=${hwSnap.abiLabel} | Thermal=${hwSnap.thermalStatus}"
                    "ahrs" -> "9-DOF=${telemetry.ahrsFused9Dof} Pitch=${"%.1f".format(telemetry.pitchDeg)}° Roll=${"%.1f".format(telemetry.rollDeg)}° Yaw=${"%.1f".format(telemetry.yawDeg)}° Mag=${telemetry.magMicroTesla.toInt()}µT"
                    "sensors" -> "Total=${hwSnap.totalSensorCount} | RotVec=${hwSnap.hasRotationVector} Accel=${hwSnap.hasAccelerometer} Gyro=${hwSnap.hasGyroscope} Mag=${hwSnap.hasMagnetometer} Baro=${hwSnap.hasBarometer}"
                    "battery" -> "Phone=${hwSnap.batteryPct}% (${hwSnap.batteryChargeState}) ${hwSnap.batteryTempC}°C ${hwSnap.batteryVoltageMv}mV Health=${hwSnap.batteryHealthLabel}"
                    "memory" -> "RAM=${hwSnap.ramUsedMb}/${hwSnap.ramTotalMb}MB | FreeDisk=${"%.2f".format(hwSnap.storageFreeGb)}/${"%.2f".format(hwSnap.storageTotalGb)}GB"
                    "telemetry" -> "Lat=${telemetry.lat}, Lng=${telemetry.lng}, Spd=${"%.1f".format(telemetry.speedKmh)}kmh, Acc=±${telemetry.gpsAccuracyM.toInt()}m, Pack=${telemetry.batteryPct}%"
                    "keys" -> "YT=${LiveCloudServices.hasYoutubeKey()}, TT=${LiveCloudServices.hasTomTomKey()}, MB=${ApiKeyRing.mapboxTokens().isNotEmpty()}, ORS=${LiveCloudServices.hasOpenRouteServiceKey()}, OCM=${LiveCloudServices.hasOpenChargeMapKey()}, TV=${LiveCloudServices.hasTavilyKey()}, IMGBB=${LiveCloudServices.hasImgbbKey()}"
                    "clear" -> ""
                    else -> "Unknown cmd: $c (type 'help')"
                  }
                cmdInput = ""
              }) { Text("Run") }
            }
          }
        }
      }

      // 5. FULL FEATURE CHANGELOG HEADER
      item(key = "changelog_header") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
          Icon(Icons.Default.NewReleases, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
          Text("RELEASE CHANGELOG & FEATURE LEDGER", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        }
      }

      items(changelog, key = { it.first }) { (ver, desc) ->
        TacticalCard {
          Text(ver, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
          Spacer(Modifier.height(6.dp))
          Text(desc, style = MaterialTheme.typography.bodySmall)
        }
      }
    }
  }
}
