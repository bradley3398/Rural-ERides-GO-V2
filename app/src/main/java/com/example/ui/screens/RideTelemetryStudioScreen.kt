package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.compartments.ComposeVectorMethodChartPreview
import com.example.ui.compartments.GraphAnalysisMethod
import com.example.ui.compartments.GraphInspectChartType
import com.example.ui.compartments.GraphInspectData
import com.example.ui.compartments.GraphInspectModal
import com.example.ui.compartments.GraphInspectSlice
import com.example.ui.compartments.applicableMethodsFor
import com.example.ui.compartments.computeGraphAnalytics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.PevBatteryPhysics
import com.example.data.MasterSettings
import com.example.data.PevVehicle
import com.example.data.RideSession
import com.example.hardware.VoiceAudioManager
import com.example.ui.components.ConfigureEdgeToEdgeDialogWindow
import com.example.ui.components.TacticalCard
import com.example.ui.components.distUnitLabel
import com.example.ui.components.formatDistValue
import com.example.ui.components.formatSpeedValue
import com.example.ui.components.rememberHostWindowSafeInsets
import com.example.ui.components.speedUnitLabel
import com.example.viewmodel.CockpitViewModel
import com.example.viewmodel.RideTelemetryViewModel
import com.example.viewmodel.SessionViewModel
import kotlin.math.max

@Composable
fun RideTelemetryStudioScreen(
  sessionVm: SessionViewModel,
  cockpitVm: CockpitViewModel,
  initialRideId: String? = null,
  onBack: () -> Unit,
) {
  BackHandler { onBack() }

  val ctx = LocalContext.current
  val telemetryVm = androidx.lifecycle.viewmodel.compose.viewModel<RideTelemetryViewModel>()
  val settings by sessionVm.settings.collectAsStateWithLifecycle()
  val rides by cockpitVm.rides.collectAsStateWithLifecycle()
  val vehicles by cockpitVm.vehicles.collectAsStateWithLifecycle()
  val profile by sessionVm.profile.collectAsStateWithLifecycle()
  val fleetDebrief by cockpitVm.fleetDebrief.collectAsStateWithLifecycle()
  val fleetDebriefBusy by cockpitVm.fleetDebriefBusy.collectAsStateWithLifecycle()

  androidx.compose.runtime.LaunchedEffect(rides, settings) {
    telemetryVm.updateRides(rides)
    telemetryVm.updateSettings(settings)
  }

  androidx.compose.runtime.LaunchedEffect(initialRideId) {
    if (initialRideId != null) {
      telemetryVm.setSelectedRideId(initialRideId)
      telemetryVm.setStudioMode(1)
    }
  }

  val uiState by telemetryVm.uiState.collectAsStateWithLifecycle()
  val studioMode = uiState.studioMode
  val vehicleFilter = uiState.vehicleFilter
  val windowLimit = uiState.windowLimit
  val selectedRideId = uiState.selectedRideId
  val filteredRides = uiState.filteredRides
  val vehicleOptions = uiState.vehicleOptions
  val totalKm = uiState.totalKm
  val topSpeedKmh = uiState.topSpeedKmh
  val totalElevM = uiState.totalElevM
  val totalWh = uiState.totalWh
  val totalMins = uiState.totalMins
  val avgEff = uiState.avgEff

  val activeVehicle =
    remember(vehicles, profile?.activeVehicleId) {
      vehicles.firstOrNull { it.id == profile?.activeVehicleId } ?: vehicles.firstOrNull()
    }

  var pieMode by remember { mutableIntStateOf(0) } // 0=Vehicle, 1=Speed Zones, 2=Energy Wh, 3=Elevation, 4=Battery Drain
  var overlayAllTracks by remember { mutableStateOf(false) }
  var compareLeftIdx by remember(rides) { mutableIntStateOf(0) }
  var compareRightIdx by remember(rides) { mutableIntStateOf(if (rides.size > 1) 1 else 0) }
  var activeGraphTab by remember { mutableStateOf<Int?>(null) }
  var fullInspectData by remember { mutableStateOf<GraphInspectData?>(null) }
  var selectedStudioMethod by remember { mutableStateOf(GraphAnalysisMethod.RAW_SERIES) }

  val studioGraphMeta = remember {
    listOf(
      Triple("1. Speed Profile", Icons.AutoMirrored.Filled.ShowChart, Color(0xFF6366F1)),
      Triple("2. Elevation Gain", Icons.Default.Terrain, Color(0xFF38BDF8)),
      Triple("3. Efficiency & Voltage", Icons.Default.ElectricBolt, Color(0xFFEC4899)),
      Triple("4. Distance vs Energy", Icons.Default.BarChart, Color(0xFF10B981)),
      Triple("5. Fleet Pie Slices", Icons.Default.PieChart, Color(0xFF6366F1)),
      Triple("6. Pace vs Efficiency", Icons.Default.BubbleChart, Color(0xFFA855F7)),
      Triple("7. Ride Duration", Icons.Default.Schedule, Color(0xFFF97316)),
      Triple("8. Motor Watts & Amps", Icons.Default.Speed, Color(0xFFF59E0B)),
      Triple("9. Performance Radar", Icons.Default.Radar, Color(0xFF10B981)),
      Triple("10. Gas Savings ROI", Icons.AutoMirrored.Filled.TrendingUp, Color(0xFF22C55E)),
      Triple("11. AI Fleet Coach", Icons.Default.AutoAwesome, Color(0xFFEC4899)),
    )
  }

  fun playCue(cue: VoiceAudioManager.TouchCue = VoiceAudioManager.TouchCue.BUTTON_TAP) {
    sessionVm.voiceAudio.triggerHapticCue(
      enabled = settings.hapticFeedback && settings.buttonHapticFeedback,
      cue = VoiceAudioManager.HapticCue.LIGHT_TICK,
      durationMs = settings.hapticDurationMs,
    )
    sessionVm.voiceAudio.playTouchSound(
      enabled = settings.touchSoundsEnabled,
      pack = settings.touchSoundPack,
      cue = cue,
      volumePct = settings.globalVolume,
    )
  }

  val distUnit = distUnitLabel(settings.distanceUnit)
  val spdUnit = speedUnitLabel(settings.distanceUnit)
  val elevUnit = elevationUnitLabel(settings.distanceUnit)

  val liveCardSummaries = remember(filteredRides, totalKm, topSpeedKmh, totalElevM, totalWh, totalMins, avgEff, fleetDebrief, settings.distanceUnit, settings.carMpg, settings.gasPriceUsd, settings.kwhRateUsd) {
    val milesTotal = totalKm * 0.621371
    val gasSavedUsd = ((milesTotal / settings.carMpg.coerceAtLeast(8.0)) * settings.gasPriceUsd - (totalWh / 1000.0) * settings.kwhRateUsd).coerceAtLeast(0.0)
    val avgDurationMin = if (filteredRides.isNotEmpty()) totalMins / filteredRides.size else 0L
    val avgWatts = if (filteredRides.isNotEmpty()) {
      filteredRides.map { r -> r.whUsed / (r.durationSec / 3600.0).coerceAtLeast(0.02) }.average().takeIf { it.isFinite() } ?: 0.0
    } else 0.0
    val vehicleCount = filteredRides.map { it.vehicleName }.distinct().size
    listOf(
      "Peak ${"%.1f".format(formatSpeedValue(topSpeedKmh, settings.distanceUnit))} $spdUnit across ${filteredRides.size} logs",
      "Total +${formatElevationValue(totalElevM, settings.distanceUnit).toInt()} $elevUnit climbed",
      "Avg ${"%.1f".format(avgEff)} Wh/$distUnit • Pack drain curve",
      "${"%.1f".format(formatDistValue(totalKm, settings.distanceUnit))} $distUnit • ${totalWh.toInt()} Wh consumed",
      "$vehicleCount active rig${if (vehicleCount == 1) "" else "s"} • 5 live pie slices",
      "Pace vs Wh/$distUnit correlation scatter",
      "Avg $avgDurationMin min/ride • $totalMins min total",
      "Avg ${avgWatts.toInt()} W continuous drivetrain load",
      "5-axis latest ride vs fleet benchmark",
      "$${"%.2f".format(gasSavedUsd)} net fuel savings • Odometer growth",
      if (fleetDebrief.isNotBlank()) "Live Gemini debrief ready • Tap to inspect" else "Run live Gemini engineering review",
    )
  }

  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = Modifier.fillMaxSize().testTag("ride_telemetry_studio_screen"),
  ) {
    Box(
      modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding(),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier.fillMaxSize().widthIn(max = 860.dp),
      ) {
      // TOP HEADER BAR
      Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          IconButton(
            onClick = {
              playCue(VoiceAudioManager.TouchCue.BACK_NAV)
              onBack()
            },
            modifier = Modifier.size(48.dp).testTag("telemetry_studio_back_button"),
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
          }
          Column(Modifier.weight(1f)) {
            Text(
              "BACKROAD TELEMETRY STUDIO",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Black,
              color = MaterialTheme.colorScheme.primary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Icon(Icons.Default.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(13.dp))
              Text(
                "Firestore Live • ${rides.size} Ride Logs",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                softWrap = false,
              )
            }
          }
          if (rides.isNotEmpty()) {
            IconButton(
              onClick = {
                playCue()
                val report = buildString {
                  appendLine("Rural ERides GO — Fleet Telemetry Summary")
                  appendLine("Rides Analyzed: ${filteredRides.size} ($vehicleFilter)")
                  appendLine("Total Distance: ${"%.2f".format(formatDistValue(totalKm, settings.distanceUnit))} $distUnit")
                  appendLine("Top Speed: ${"%.1f".format(formatSpeedValue(topSpeedKmh, settings.distanceUnit))} $spdUnit")
                  appendLine("Total Elevation Gain: +${formatElevationValue(totalElevM, settings.distanceUnit).toInt()} $elevUnit")
                  appendLine("Avg Efficiency: ${"%.1f".format(avgEff)} Wh/$distUnit (${totalWh.toInt()} Wh total)")
                  appendLine("Total Saddle Time: $totalMins min")
                }
                val share = Intent(Intent.ACTION_SEND).apply {
                  type = "text/plain"
                  putExtra(Intent.EXTRA_SUBJECT, "PEV Telemetry Report")
                  putExtra(Intent.EXTRA_TEXT, report)
                }
                runCatching { ctx.startActivity(Intent.createChooser(share, "Share Telemetry Summary")) }
              },
              modifier = Modifier.size(48.dp).testTag("telemetry_studio_share_button"),
            ) {
              Icon(Icons.Default.Share, contentDescription = "Share Telemetry Report", tint = MaterialTheme.colorScheme.secondary)
            }
          }
        }
      }

      if (rides.isEmpty()) {
        // HONEST EMPTY STATE (NO MOCK DATA)
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
          TacticalCard(title = "NO FIRESTORE RIDE LOGS YET") {
            Column(
              modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
              Text(
                "Ready for Live Trail Telemetry",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                "Start a live ride from Trailhead and save it to Firestore. Your speed profiles, elevation gain curves, Wh/mi efficiency graphs, pie breakdowns, and GPS track maps will appear here automatically.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
        return@Column
      }

      // PRIMARY MODE SWITCHER
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = studioMode == 0,
          onClick = { playCue(VoiceAudioManager.TouchCue.TAB_SWITCH); telemetryVm.setStudioMode(0) },
          leadingIcon = { Icon(Icons.AutoMirrored.Filled.ShowChart, null, Modifier.size(16.dp)) },
          label = { Text("Fleet Graphs (${studioGraphMeta.size})", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
          modifier = Modifier.weight(1f).testTag("studio_mode_fleet"),
        )
        FilterChip(
          selected = studioMode == 1,
          onClick = { playCue(VoiceAudioManager.TouchCue.TAB_SWITCH); telemetryVm.setStudioMode(1) },
          leadingIcon = { Icon(Icons.Default.Speed, null, Modifier.size(16.dp)) },
          label = { Text("Ride Inspector", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
          modifier = Modifier.weight(1f).testTag("studio_mode_single"),
        )
      }

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (studioMode == 0) {
          // VEHICLE & WINDOW FILTERS
          item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(listOf(0 to "All Rides", 5 to "Last 5", 10 to "Last 10", 25 to "Last 25")) { (lim, lbl) ->
                FilterChip(
                  selected = windowLimit == lim,
                  onClick = { playCue(VoiceAudioManager.TouchCue.TAB_SWITCH); telemetryVm.setWindowLimit(lim) },
                  label = { Text(lbl, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                )
              }
              items(vehicleOptions) { vName ->
                FilterChip(
                  selected = vehicleFilter == vName,
                  onClick = { playCue(VoiceAudioManager.TouchCue.TAB_SWITCH); telemetryVm.setVehicleFilter(vName) },
                  label = { Text(vName, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                )
              }
            }
          }

          // KPI SUMMARY GRID (2 ROWS x 3 PILLS)
          item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StudioKpiTile("TOTAL DIST", "${"%.1f".format(formatDistValue(totalKm, settings.distanceUnit))} $distUnit", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                StudioKpiTile("PEAK SPEED", "${"%.1f".format(formatSpeedValue(topSpeedKmh, settings.distanceUnit))} $spdUnit", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                StudioKpiTile("TOTAL CLIMB", "+${formatElevationValue(totalElevM, settings.distanceUnit).toInt()} $elevUnit", Color(0xFF38BDF8), Modifier.weight(1f))
              }
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StudioKpiTile("EFFICIENCY", "${"%.1f".format(avgEff)} Wh/$distUnit", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                StudioKpiTile("ENERGY USED", "${totalWh.toInt()} Wh", Color(0xFFF97316), Modifier.weight(1f))
                StudioKpiTile("SADDLE TIME", "${totalMins} min", Color(0xFFA855F7), Modifier.weight(1f))
              }
            }
          }

          // LIVE PERSONAL RECORDS RIBBON (1-TAP JUMP TO RIDE INSPECTOR)
          if (filteredRides.isNotEmpty()) {
            item(key = "fleet_personal_records_ribbon") {
              val fastestRide = remember(filteredRides) { filteredRides.maxByOrNull { it.maxSpeedKmh.takeIf { v -> v.isFinite() } ?: 0.0 } }
              val longestRide = remember(filteredRides) { filteredRides.maxByOrNull { it.distanceKm.takeIf { v -> v.isFinite() } ?: 0.0 } }
              val biggestClimb = remember(filteredRides) { filteredRides.maxByOrNull { it.elevationGainM.takeIf { v -> v.isFinite() } ?: 0.0 } }
              val bestEffRide = remember(filteredRides) {
                filteredRides.filter { it.distanceKm > 0.25 && it.whUsed > 0.5 }
                  .minByOrNull { it.whUsed / it.distanceKm }
              }
              val records = remember(fastestRide, longestRide, biggestClimb, bestEffRide, settings.distanceUnit) {
                buildList {
                  fastestRide?.let { r ->
                    add(Triple("FASTEST PEAK", "${"%.1f".format(formatSpeedValue(r.maxSpeedKmh, settings.distanceUnit))} $spdUnit", r))
                  }
                  longestRide?.let { r ->
                    add(Triple("LONGEST TRIP", "${"%.1f".format(formatDistValue(r.distanceKm, settings.distanceUnit))} $distUnit", r))
                  }
                  biggestClimb?.let { r ->
                    add(Triple("BIGGEST CLIMB", "+${formatElevationValue(r.elevationGainM, settings.distanceUnit).toInt()} $elevUnit", r))
                  }
                  bestEffRide?.let { r ->
                    val eff = if (settings.distanceUnit == "km") PevBatteryPhysics.whPerKm(r.whUsed, r.distanceKm)
                    else PevBatteryPhysics.whPerMile(r.whUsed, r.distanceKm)
                    add(Triple("BEST EFFICIENCY", "${"%.1f".format(eff)} Wh/$distUnit", r))
                  }
                }
              }
              if (records.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(
                    "PERSONAL RECORDS (TAP TO INSPECT RIDE)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                  )
                  LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(records, key = { idx, item -> "pr_${idx}_${item.first}" }) { idx, (label, metricVal, rideObj) ->
                      val accent = listOf(Color(0xFF6366F1), Color(0xFF10B981), Color(0xFF38BDF8), Color(0xFFEC4899))[idx % 4]
                      Surface(
                        onClick = {
                          playCue(VoiceAudioManager.TouchCue.ACTION_CONFIRM)
                          telemetryVm.setSelectedRideId(rideObj.id)
                          telemetryVm.setStudioMode(1)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
                        modifier = Modifier.widthIn(min = 144.dp).testTag("studio_pr_card_$idx"),
                      ) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                          Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black, color = accent, maxLines = 1, softWrap = false)
                          Text(metricVal, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                          Text(
                            rideObj.title.ifBlank { rideObj.vehicleName }.take(20),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          // INDEPENDENT GRAPH WINDOW MATRIX (EACH GRAPH IN ITS OWN WINDOW)
          item {
            Text(
              "TELEMETRY WINDOW MATRIX (TAP ANY GRAPH TO OPEN DEDICATED ANALYTICS WINDOW)",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          }

          items(studioGraphMeta.indices.toList()) { idx ->
            val (title, icon, accent) = studioGraphMeta[idx]
            val subtitle = liveCardSummaries.getOrElse(idx) { "Launch dedicated chart window" }
            Surface(
              onClick = {
                playCue(VoiceAudioManager.TouchCue.BUTTON_TAP)
                activeGraphTab = idx
              },
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
              modifier = Modifier.fillMaxWidth().testTag("graph_window_card_$idx"),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = accent.copy(alpha = 0.15f),
                  modifier = Modifier.size(38.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                  }
                }
                Column(Modifier.weight(1f)) {
                  Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                  Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = accent,
                  modifier = Modifier.padding(2.dp),
                ) {
                  Text("OPEN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
              }
            }
          }



          // INTERACTIVE MULTI-RIDE & SINGLE-RIDE GPS TRACK MAP
          val mapRide = filteredRides.firstOrNull { it.id == selectedRideId } ?: filteredRides.firstOrNull()
          if (mapRide != null) {
            item {
              StudioGraphCard(icon = Icons.Default.Map, title = "GPS TRAIL TRACKS MAP", accent = MaterialTheme.colorScheme.primary) {
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                  selected = overlayAllTracks,
                  onClick = { playCue(VoiceAudioManager.TouchCue.TAB_SWITCH); overlayAllTracks = !overlayAllTracks },
                  leadingIcon = { Icon(Icons.Default.Layers, null, Modifier.size(15.dp)) },
                  label = { Text(if (overlayAllTracks) "Overlaying All Tracks" else "Overlay All Tracks", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                )
              }
              if (!overlayAllTracks) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  itemsIndexed(filteredRides.take(12), key = { idx, r -> "map_${r.id}_$idx" }) { _, r ->
                    val pts = remember(r.id, r.polylinePoints.size) { parseRideTrackPoints(r).size }
                    FilterChip(
                      selected = mapRide.id == r.id,
                      onClick = { playCue(VoiceAudioManager.TouchCue.TAB_SWITCH); telemetryVm.setSelectedRideId(r.id) },
                      label = { Text("${r.title.take(16)} ($pts)", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                    )
                  }
                }
              }
              RideTrackMapView(
                ride = mapRide,
                allOverlayRides = if (overlayAllTracks) filteredRides else emptyList(),
                settings = settings,
                height = 250.dp,
              )
            }
          }
        }

          // SIDE-BY-SIDE 2-RIDE COMPARISON
          if (filteredRides.size >= 2) {
            item {
              val left = filteredRides[compareLeftIdx.coerceIn(0, filteredRides.lastIndex)]
              val right = filteredRides[compareRightIdx.coerceIn(0, filteredRides.lastIndex)]
              StudioGraphCard(icon = Icons.Default.CompareArrows, title = "SIDE-BY-SIDE RIDE COMPARISON", accent = MaterialTheme.colorScheme.secondary) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  FilterChip(
                    selected = true,
                    onClick = { playCue(); compareLeftIdx = (compareLeftIdx + 1) % filteredRides.size },
                    label = { Text("A: ${left.title.take(14)}", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                    modifier = Modifier.weight(1f),
                  )
                  FilterChip(
                    selected = true,
                    onClick = { playCue(); compareRightIdx = (compareRightIdx + 1) % filteredRides.size },
                    label = { Text("B: ${right.title.take(14)}", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                    modifier = Modifier.weight(1f),
                  )
                }
                StudioCompareRow("Distance", "${"%.2f".format(formatDistValue(left.distanceKm, settings.distanceUnit))} $distUnit", "${"%.2f".format(formatDistValue(right.distanceKm, settings.distanceUnit))} $distUnit")
                StudioCompareRow("Peak Speed", "${"%.1f".format(formatSpeedValue(left.maxSpeedKmh, settings.distanceUnit))} $spdUnit", "${"%.1f".format(formatSpeedValue(right.maxSpeedKmh, settings.distanceUnit))} $spdUnit")
                StudioCompareRow("Avg Speed", "${"%.1f".format(formatSpeedValue(left.avgSpeedKmh, settings.distanceUnit))} $spdUnit", "${"%.1f".format(formatSpeedValue(right.avgSpeedKmh, settings.distanceUnit))} $spdUnit")
                StudioCompareRow("Elevation Gain", "+${formatElevationValue(left.elevationGainM, settings.distanceUnit).toInt()} $elevUnit", "+${formatElevationValue(right.elevationGainM, settings.distanceUnit).toInt()} $elevUnit")
                StudioCompareRow("Energy Used", "${left.whUsed.toInt()} Wh", "${right.whUsed.toInt()} Wh")
              }
            }
          }
        } else {
          // MODE 1: CRASH-PROOF FOCUSED RIDE INSPECTOR (REPLACES MONOLITHIC STACKED DEEP DIVE)
          item(key = "ride_inspector_picker") {
            Text(
              "SELECT A FIRESTORE RIDE LOG (${rides.size})",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Black,
              color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              itemsIndexed(rides, key = { idx, r -> "pick_${idx}_${r.id}" }) { idx, r ->
                val dVal = formatDistValue(r.distanceKm.takeIf { it.isFinite() } ?: 0.0, settings.distanceUnit)
                FilterChip(
                  selected = selectedRideId == r.id || (selectedRideId == null && idx == 0),
                  onClick = { playCue(VoiceAudioManager.TouchCue.TAB_SWITCH); telemetryVm.setSelectedRideId(r.id) },
                  label = { Text("${r.title.ifBlank { "Ride ${idx + 1}" }.take(18)} • ${"%.1f".format(dVal)}$distUnit", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                  modifier = Modifier.testTag("studio_pick_ride_${r.id}"),
                )
              }
            }
          }

          val activeIdx = rides.indexOfFirst { it.id == selectedRideId }.takeIf { it >= 0 } ?: 0
          val activeRide = rides.getOrNull(activeIdx)
          if (activeRide != null) {
            item(key = "ride_inspector_body_${activeRide.id}_$activeIdx") {
              RideInspectorDeck(
                ride = activeRide,
                activeVehicle = activeVehicle,
                settings = settings,
                distUnit = distUnit,
                spdUnit = spdUnit,
                elevUnit = elevUnit,
                onPlayCue = { cue -> playCue(cue) },
                onLaunchInspectModal = { data -> fullInspectData = data },
              )
            }
          } else {
            item {
              TacticalCard(title = "NO RIDE LOGS") {
                Text("No ride logs available. Record or sync a ride to inspect live telemetry.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
              }
            }
          }
        }

        item { Spacer(Modifier.height(16.dp)) }
      }
    }
    }
  }

  // DEDICATED GRAPH WINDOW POP-UP FOR TELEMETRY MATRIX CARDS (0..10)
  activeGraphTab?.let { tabIdx ->
    val safeTab = tabIdx.coerceIn(0, studioGraphMeta.lastIndex)
    val (winTitle, winIcon, winAccent) = studioGraphMeta[safeTab]
    val palette = listOf(
      MaterialTheme.colorScheme.primary,
      MaterialTheme.colorScheme.secondary,
      MaterialTheme.colorScheme.tertiary,
      Color(0xFF38BDF8),
      Color(0xFFF97316),
      Color(0xFFA855F7),
    )

    fun buildInspectForTab(idx: Int): GraphInspectData? {
      val series = filteredRides.take(16).reversed()
      if (series.isEmpty()) return null
      val seriesGps =
        series.lastOrNull { parseRideTrackPoints(it).size >= 2 }
          ?.let { r -> parseRideTrackPoints(r).map { it.latitude to it.longitude } }
          .orEmpty()
      return when (idx) {
        0 -> {
          val maxSpd = series.map { formatSpeedValue(it.maxSpeedKmh, settings.distanceUnit) }
          val avgSpd = series.map { formatSpeedValue(it.avgSpeedKmh, settings.distanceUnit) }
          GraphInspectData(
            title = "Speed Profile Matrix",
            subtitle = "Peak & Average across ${series.size} logs",
            chartType = GraphInspectChartType.SPEED,
            icon = Icons.AutoMirrored.Filled.ShowChart,
            primaryColor = winAccent,
            unitLabel = spdUnit,
            values = maxSpd,
            secondaryValues = avgSpd,
            peakMetric = "%.1f %s".format(maxSpd.maxOrNull() ?: 0.0, spdUnit),
            avgMetric = "%.1f %s".format(avgSpd.average().takeIf { !it.isNaN() } ?: 0.0, spdUnit),
            referenceValue = avgSpd.average().takeIf { !it.isNaN() },
            timestamps = series.map { it.title },
            gpsPoints = seriesGps,
          )
        }
        1 -> {
          val elevVals = series.map { formatElevationValue(it.elevationGainM, settings.distanceUnit) }
          GraphInspectData(
            title = "Elevation Gain Matrix",
            subtitle = "Total Climb: +${elevVals.sum().toInt()} $elevUnit",
            chartType = GraphInspectChartType.ELEVATION,
            icon = Icons.Default.Terrain,
            primaryColor = winAccent,
            unitLabel = elevUnit,
            values = elevVals,
            peakMetric = "+%.0f %s".format(elevVals.maxOrNull() ?: 0.0, elevUnit),
            avgMetric = "+%.0f %s".format(elevVals.average().takeIf { !it.isNaN() } ?: 0.0, elevUnit),
            totalMetric = "+%.0f %s".format(elevVals.sum(), elevUnit),
            referenceValue = elevVals.average().takeIf { !it.isNaN() },
            timestamps = series.map { it.title },
            gpsPoints = seriesGps,
          )
        }
        2 -> {
          val effVals = series.map { r ->
            if (settings.distanceUnit == "km") PevBatteryPhysics.whPerKm(r.whUsed, r.distanceKm)
            else PevBatteryPhysics.whPerMile(r.whUsed, r.distanceKm)
          }
          GraphInspectData(
            title = "Efficiency & Pack Drain",
            subtitle = "Wh per $distUnit consumption",
            chartType = GraphInspectChartType.EFFICIENCY_VOLTAGE,
            icon = Icons.Default.ElectricBolt,
            primaryColor = winAccent,
            unitLabel = "Wh/$distUnit",
            values = effVals,
            peakMetric = "%.1f Wh/%s".format(effVals.maxOrNull() ?: 0.0, distUnit),
            avgMetric = "%.1f Wh/%s".format(effVals.average().takeIf { !it.isNaN() } ?: 0.0, distUnit),
            referenceValue = effVals.average().takeIf { !it.isNaN() },
            timestamps = series.map { it.title },
            gpsPoints = seriesGps,
          )
        }
        3 -> {
          val distVals = series.map { formatDistValue(it.distanceKm, settings.distanceUnit) }
          GraphInspectData(
            title = "Distance vs Energy",
            subtitle = "Trip length distribution (${series.size} rides)",
            chartType = GraphInspectChartType.DISTANCE_ENERGY,
            icon = Icons.Default.BarChart,
            primaryColor = winAccent,
            unitLabel = distUnit,
            values = distVals,
            peakMetric = "%.1f %s".format(distVals.maxOrNull() ?: 0.0, distUnit),
            totalMetric = "%.1f %s".format(distVals.sum(), distUnit),
            timestamps = series.map { it.title },
            gpsPoints = seriesGps,
          )
        }
        4 -> {
          val (slices, totalStr, lbl) = buildFleetPieSlices(filteredRides, pieMode, palette, settings.distanceUnit)
          GraphInspectData(
            title = "Fleet Pie Breakdown",
            subtitle = "${filteredRides.size} rides • $totalStr $lbl",
            chartType = GraphInspectChartType.PIE_DONUT,
            icon = Icons.Default.PieChart,
            primaryColor = winAccent,
            unitLabel = lbl,
            peakMetric = "$totalStr $lbl",
            pieSlices = slices.map { GraphInspectSlice(it.label, it.value, it.color) },
            gpsPoints = seriesGps,
          )
        }
        5 -> {
          val spdVals = series.map { formatSpeedValue(it.avgSpeedKmh, settings.distanceUnit) }
          val effVals = series.map { r ->
            if (settings.distanceUnit == "km") PevBatteryPhysics.whPerKm(r.whUsed, r.distanceKm)
            else PevBatteryPhysics.whPerMile(r.whUsed, r.distanceKm)
          }
          GraphInspectData(
            title = "Pace vs Efficiency Scatter",
            subtitle = "Avg Speed ($spdUnit) vs Wh/$distUnit",
            chartType = GraphInspectChartType.SCATTER,
            icon = Icons.Default.BubbleChart,
            primaryColor = winAccent,
            unitLabel = "Wh/$distUnit",
            values = spdVals,
            secondaryValues = effVals,
            peakMetric = "%.1f %s".format(spdVals.maxOrNull() ?: 0.0, spdUnit),
            avgMetric = "%.1f Wh/%s".format(effVals.average().takeIf { !it.isNaN() } ?: 0.0, distUnit),
            timestamps = series.map { it.title },
            gpsPoints = seriesGps,
          )
        }
        6 -> {
          val mins = series.map { (it.durationSec / 60.0).coerceAtLeast(1.0) }
          GraphInspectData(
            title = "Ride Duration Distribution",
            subtitle = "Minutes per ride",
            chartType = GraphInspectChartType.HISTOGRAM,
            icon = Icons.Default.Schedule,
            primaryColor = winAccent,
            unitLabel = "mins",
            values = mins,
            peakMetric = "%.0f mins".format(mins.maxOrNull() ?: 0.0),
            totalMetric = "%.0f mins".format(mins.sum()),
            timestamps = series.map { it.title },
            gpsPoints = seriesGps,
          )
        }
        7 -> {
          val watts = series.map { r ->
            val hrs = (r.durationSec / 3600.0).coerceAtLeast(0.02)
            r.whUsed / hrs
          }
          GraphInspectData(
            title = "Motor Watts & Pack Current",
            subtitle = "Average continuous load",
            chartType = GraphInspectChartType.POWER_AMPS,
            icon = Icons.Default.Speed,
            primaryColor = winAccent,
            unitLabel = "W",
            values = watts,
            peakMetric = "%.0f W".format(watts.maxOrNull() ?: 0.0),
            avgMetric = "%.0f W".format(watts.average().takeIf { !it.isNaN() } ?: 0.0),
            timestamps = series.map { it.title },
            gpsPoints = seriesGps,
          )
        }
        8 -> {
          val radar = computeRiderRadarScores(filteredRides)
          GraphInspectData(
            title = "5-Axis Performance Radar",
            subtitle = "Latest Ride vs Fleet Benchmark (${filteredRides.size} logs)",
            chartType = GraphInspectChartType.RADAR_5AXIS,
            icon = Icons.Default.Radar,
            primaryColor = winAccent,
            unitLabel = "%",
            values = radar.latest.map { it.toDouble() },
            secondaryValues = radar.fleetAvg.map { it.toDouble() },
            peakMetric = radar.peakLabel,
            avgMetric = radar.avgPctLabel,
            minMetric = radar.minLabel,
            timestamps = radar.axisLabels,
            gpsPoints = seriesGps,
          )
        }
        9 -> {
          var acc = 0.0
          val cum = series.map { r ->
            acc += formatDistValue(r.distanceKm, settings.distanceUnit)
            acc
          }
          GraphInspectData(
            title = "Cumulative Odometer Growth",
            subtitle = "Total: ${"%.1f".format(acc)} $distUnit",
            chartType = GraphInspectChartType.SPEED,
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            primaryColor = winAccent,
            unitLabel = distUnit,
            values = cum,
            peakMetric = "%.1f %s".format(acc, distUnit),
            totalMetric = "${filteredRides.size} rides",
            timestamps = series.map { it.title },
            gpsPoints = seriesGps,
          )
        }
        else -> null
      }
    }

    fun launchFullInspectForTab(idx: Int) {
      fullInspectData = buildInspectForTab(idx)
    }

    BackHandler { activeGraphTab = null }
    Dialog(
      onDismissRequest = { activeGraphTab = null },
      properties = DialogProperties(
        usePlatformDefaultWidth = false,
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        decorFitsSystemWindows = false,
      ),
    ) {
      ConfigureEdgeToEdgeDialogWindow(fullScreen = false)
      val safeInsets = rememberHostWindowSafeInsets()
      BoxWithConstraints(
        modifier = Modifier
          .fillMaxSize()
          .padding(
            start = safeInsets.start + 10.dp,
            end = safeInsets.end + 10.dp,
            top = safeInsets.top + 10.dp,
            bottom = maxOf(safeInsets.bottom, safeInsets.imeBottom) + 10.dp,
          ),
        contentAlignment = Alignment.Center,
      ) {
      val computedMaxHeight = (maxHeight - 8.dp).coerceIn(280.dp, 780.dp)
      Surface(
        modifier = Modifier
          .fillMaxWidth(0.96f)
          .widthIn(max = 700.dp)
          .heightIn(max = computedMaxHeight),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, winAccent.copy(alpha = 0.65f)),
        tonalElevation = 12.dp,
        shadowElevation = 24.dp,
      ) {
        Column(Modifier.fillMaxWidth()) {
          // POP-UP TOP HEADER WITH PREV/NEXT/CLOSE
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(
                Brush.horizontalGradient(
                  listOf(winAccent.copy(alpha = 0.24f), MaterialTheme.colorScheme.surfaceVariant),
                ),
              )
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(winIcon, contentDescription = null, tint = winAccent, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
              Text(
                winTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                "${filteredRides.size} Filtered Logs • $vehicleFilter",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            IconButton(
              onClick = {
                playCue(VoiceAudioManager.TouchCue.TAB_SWITCH)
                activeGraphTab = (safeTab - 1 + studioGraphMeta.size) % studioGraphMeta.size
              },
              modifier = Modifier.size(48.dp).testTag("studio_window_prev_btn"),
            ) {
              Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Graph Window")
            }
            IconButton(
              onClick = {
                playCue(VoiceAudioManager.TouchCue.TAB_SWITCH)
                activeGraphTab = (safeTab + 1) % studioGraphMeta.size
              },
              modifier = Modifier.size(48.dp).testTag("studio_window_next_btn"),
            ) {
              Icon(Icons.Default.ChevronRight, contentDescription = "Next Graph Window")
            }
            IconButton(
              onClick = {
                playCue(VoiceAudioManager.TouchCue.BACK_NAV)
                activeGraphTab = null
              },
              modifier = Modifier.size(48.dp).testTag("studio_window_close_btn"),
            ) {
              Icon(Icons.Default.Close, contentDescription = "Close Graph Window")
            }
          }

          HorizontalDivider(color = winAccent.copy(alpha = 0.35f))

          Column(
            modifier = Modifier
              .weight(1f, fill = false)
              .verticalScroll(rememberScrollState())
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            if (filteredRides.isEmpty() && safeTab != 10) {
              Text(
                "No rides match filter \"$vehicleFilter\". Select \"All\" in the filter strip to visualize telemetry.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            } else {
              val tabChartType = when (safeTab) {
                0 -> GraphInspectChartType.SPEED
                1 -> GraphInspectChartType.ELEVATION
                2 -> GraphInspectChartType.EFFICIENCY_VOLTAGE
                3 -> GraphInspectChartType.DISTANCE_ENERGY
                4 -> GraphInspectChartType.PIE_DONUT
                5 -> GraphInspectChartType.SCATTER
                6 -> GraphInspectChartType.HISTOGRAM
                7 -> GraphInspectChartType.POWER_AMPS
                8 -> GraphInspectChartType.RADAR_5AXIS
                else -> GraphInspectChartType.SPEED
              }
              val tabApplicableMethods = remember(tabChartType) { applicableMethodsFor(tabChartType) }
              val effectiveStudioMethod = if (selectedStudioMethod in tabApplicableMethods) selectedStudioMethod else tabApplicableMethods.first()

              if (safeTab in 0..9 && tabApplicableMethods.size > 1) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  items(tabApplicableMethods) { method ->
                    FilterChip(
                      selected = effectiveStudioMethod == method,
                      onClick = { playCue(VoiceAudioManager.TouchCue.TAB_SWITCH); selectedStudioMethod = method },
                      modifier = Modifier.heightIn(min = 48.dp).testTag("studio_method_${method.name.lowercase()}"),
                      label = { Text(method.label, style = MaterialTheme.typography.labelSmall) },
                    )
                  }
                }
              }

              val customPreviewPayload =
                if (safeTab in 0..9 && effectiveStudioMethod != GraphAnalysisMethod.RAW_SERIES) {
                  buildInspectForTab(safeTab)
                } else null

              if (customPreviewPayload != null) {
                val previewStats = remember(customPreviewPayload.values) { computeGraphAnalytics(customPreviewPayload.values) }
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(235.dp),
                ) {
                  ComposeVectorMethodChartPreview(
                    data = customPreviewPayload,
                    stats = previewStats,
                    method = effectiveStudioMethod,
                    showGrid = true,
                    showRefLine = true,
                    showDataPoints = true,
                    onScrubPoint = { _, _ -> },
                    onHoverEnd = {},
                  )
                }
              } else {
              when (safeTab) {
                0 -> HistoricalSpeedProfileChart(
                  rides = filteredRides,
                  settings = settings,
                  chartHeight = 230.dp,
                  onTap = { launchFullInspectForTab(0) },
                )
                1 -> HistoricalElevationGainChart(
                  rides = filteredRides,
                  settings = settings,
                  chartHeight = 230.dp,
                  onTap = { launchFullInspectForTab(1) },
                )
                2 -> HistoricalEfficiencyVoltageChart(
                  rides = filteredRides,
                  activeVehicle = activeVehicle,
                  settings = settings,
                  chartHeight = 230.dp,
                  onTap = { launchFullInspectForTab(2) },
                )
                3 -> HistoricalDistanceEnergyChart(
                  rides = filteredRides,
                  settings = settings,
                  chartHeight = 220.dp,
                  onTap = { launchFullInspectForTab(3) },
                )
                4 -> {
                  LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf(0 to "By Vehicle", 1 to "Speed Zones", 2 to "Energy Wh", 3 to "Elevation", 4 to "Pack Drain")) { (mIdx, mLbl) ->
                      FilterChip(
                        selected = pieMode == mIdx,
                        onClick = { playCue(VoiceAudioManager.TouchCue.TAB_SWITCH); pieMode = mIdx },
                        label = { Text(mLbl, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                      )
                    }
                  }
                  val (slices, totalStr, unitLbl) = buildFleetPieSlices(filteredRides, pieMode, palette, settings.distanceUnit)
                  TelemetryDonutPieChart(
                    slices = slices,
                    centerValue = totalStr,
                    centerLabel = unitLbl,
                    reducedMotion = settings.reducedMotion,
                    chartSize = 165.dp,
                    onTap = { launchFullInspectForTab(4) },
                  )
                }
                5 -> SpeedVsEfficiencyScatterChart(
                  rides = filteredRides,
                  settings = settings,
                  chartHeight = 230.dp,
                  onTap = { launchFullInspectForTab(5) },
                )
                6 -> RideDurationHistogramChart(
                  rides = filteredRides,
                  reducedMotion = settings.reducedMotion,
                  chartHeight = 210.dp,
                  onTap = { launchFullInspectForTab(6) },
                )
                7 -> HistoricalPowerAmpsChart(
                  rides = filteredRides,
                  activeVehicle = activeVehicle,
                  settings = settings,
                  chartHeight = 230.dp,
                  onTap = { launchFullInspectForTab(7) },
                )
                8 -> RiderPerformanceRadarChart(
                  rides = filteredRides,
                  settings = settings,
                  chartHeight = 240.dp,
                  onTap = { launchFullInspectForTab(8) },
                )
                9 -> {
                  CumulativeOdometerRoiChart(
                    rides = filteredRides,
                    settings = settings,
                    chartHeight = 230.dp,
                    onTap = { launchFullInspectForTab(9) },
                  )
                  val milesTotal = totalKm * 0.621371
                  val gasSavedUsd = ((milesTotal / settings.carMpg.coerceAtLeast(8.0)) * settings.gasPriceUsd - (totalWh / 1000.0) * settings.kwhRateUsd).coerceAtLeast(0.0)
                  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudioKpiTile("TOTAL ODO", "${"%.1f".format(formatDistValue(totalKm, settings.distanceUnit))} $distUnit", winAccent, Modifier.weight(1f))
                    StudioKpiTile("GAS SAVED", "$${"%.2f".format(gasSavedUsd)}", Color(0xFF22C55E), Modifier.weight(1f))
                  }
                }
                else -> {
                  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                      onClick = {
                        playCue(VoiceAudioManager.TouchCue.ACTION_CONFIRM)
                        cockpitVm.requestFleetLogbookDebrief(
                          activeVehicle = activeVehicle,
                          batteryChemistry = settings.batteryChemistry,
                        )
                      },
                      enabled = !fleetDebriefBusy,
                      modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("studio_ai_coach_run_btn"),
                    ) {
                      if (fleetDebriefBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                      } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                      }
                      Spacer(Modifier.width(8.dp))
                      Text(if (fleetDebriefBusy) "Analyzing Live Fleet Telemetry..." else "Generate Live AI Fleet Debrief")
                    }
                    if (fleetDebrief.isNotBlank()) {
                      Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, winAccent.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth(),
                      ) {
                        Text(
                          text = fleetDebrief,
                          style = MaterialTheme.typography.bodyMedium,
                          modifier = Modifier.padding(12.dp),
                        )
                      }
                    } else {
                      Text(
                        "Tap above to run a live Gemini telemetry engineering review across your ${filteredRides.size} recorded rides.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                }
              }
              }
            }
          }

          if (safeTab in 0..9 && filteredRides.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.End,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              OutlinedButton(
                onClick = { launchFullInspectForTab(safeTab) },
                modifier = Modifier.heightIn(min = 48.dp).testTag("studio_window_inspect_btn"),
              ) {
                Icon(Icons.Default.OpenInFull, contentDescription = "Deep Inspect Graph", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Inspect & Analytics")
              }
            }
          }
        }
      }
      }
    }
  }

  fullInspectData?.let { inspectData ->
    GraphInspectModal(
      data = inspectData,
      initialMethod = selectedStudioMethod,
      onDismiss = { fullInspectData = null },
    )
  }
}

private fun buildFleetPieSlices(
  rides: List<RideSession>,
  pieMode: Int,
  palette: List<Color>,
  distanceUnit: String,
): Triple<List<PieSliceData>, String, String> {
  val distUnit = distUnitLabel(distanceUnit)
  val elevUnit = elevationUnitLabel(distanceUnit)
  return when (pieMode) {
    0 -> {
      val slices =
        rides
          .groupBy { it.vehicleName.ifBlank { "PEV Unit" } }
          .entries
          .mapIndexed { idx, (name, list) ->
            PieSliceData(name, list.sumOf { it.distanceKm }.coerceAtLeast(0.01), palette[idx % palette.size])
          }
          .sortedByDescending { it.value }
          .take(6)
      Triple(slices, "%.1f".format(formatDistValue(rides.sumOf { it.distanceKm }, distanceUnit)), distUnit)
    }
    1 -> {
      val cruise = rides.count { it.maxSpeedKmh < 25.0 }.toDouble()
      val brisk = rides.count { it.maxSpeedKmh in 25.0..45.0 }.toDouble()
      val hyper = rides.count { it.maxSpeedKmh > 45.0 }.toDouble()
      val slices =
        listOf(
          PieSliceData("Trail Cruise (<15mph)", cruise, palette[0]),
          PieSliceData("Country Pace (15–28mph)", brisk, palette[1]),
          PieSliceData("Hyper Run (28+ mph)", hyper, palette[2]),
        ).filter { it.value > 0.0 }
      Triple(slices, "${rides.size}", "rides")
    }
    2 -> {
      val slices =
        rides.take(6).mapIndexed { idx, r ->
          PieSliceData(r.title.ifBlank { "Ride ${idx + 1}" }, r.whUsed.coerceAtLeast(1.0), palette[idx % palette.size])
        }
      Triple(slices, "${rides.sumOf { it.whUsed }.toInt()}", "Wh")
    }
    3 -> {
      val slices =
        rides.take(6).mapIndexed { idx, r ->
          PieSliceData(r.title.ifBlank { "Ride ${idx + 1}" }, r.elevationGainM.coerceAtLeast(1.0), palette[idx % palette.size])
        }
      Triple(slices, "+${formatElevationValue(rides.sumOf { it.elevationGainM }, distanceUnit).toInt()}", elevUnit)
    }
    else -> {
      val light = rides.count { (it.startBatteryPct - it.endBatteryPct).coerceAtLeast(0) < 15 }.toDouble()
      val medium = rides.count { (it.startBatteryPct - it.endBatteryPct).coerceAtLeast(0) in 15..39 }.toDouble()
      val deep = rides.count { (it.startBatteryPct - it.endBatteryPct).coerceAtLeast(0) >= 40 }.toDouble()
      val slices =
        listOf(
          PieSliceData("Light Drain (<15%)", light, palette[0]),
          PieSliceData("Mid Drain (15–39%)", medium, palette[1]),
          PieSliceData("Deep Run (40%+)", deep, palette[2]),
        ).filter { it.value > 0.0 }
      Triple(slices, "${rides.size}", "packs")
    }
  }
}

@Composable
private fun StudioGraphCard(
  icon: ImageVector,
  title: String,
  accent: Color,
  reducedMotion: Boolean = false,
  enterDelayMs: Int = 0,
  content: @Composable () -> Unit,
) {
  NeonAnimatedChartContainer(
    triggerKey = title,
    reducedMotion = reducedMotion,
    delayMs = enterDelayMs,
  ) {
    Surface(
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
      border = BorderStroke(1.dp, accent.copy(alpha = 0.48f)),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(Modifier.fillMaxWidth()) {
        // Neon top energy accent strip
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .height(2.dp)
              .background(
                Brush.horizontalGradient(
                  listOf(accent.copy(alpha = 0.1f), accent.copy(alpha = 0.9f), accent.copy(alpha = 0.1f)),
                ),
              ),
        )
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          content()
        }
      }
    }
  }
}

@Composable
private fun StudioKpiTile(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
    shape = RoundedCornerShape(12.dp),
    modifier = modifier,
  ) {
    Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
      Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
  }
}

@Composable
private fun StudioCompareRow(label: String, valA: String, valB: String) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
    Text(valA, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, maxLines = 1, softWrap = false)
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
    Text(valB, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, maxLines = 1, softWrap = false)
  }
}
