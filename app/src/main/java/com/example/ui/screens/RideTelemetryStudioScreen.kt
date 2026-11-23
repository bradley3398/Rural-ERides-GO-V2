package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.material.icons.filled.Refresh
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.PevBatteryPhysics
import com.example.data.MasterSettings
import com.example.data.PevVehicle
import com.example.data.RideSession
import com.example.hardware.VoiceAudioManager
import com.example.ui.components.TacticalCard
import com.example.ui.components.distUnitLabel
import com.example.ui.components.formatDistValue
import com.example.ui.components.formatSpeedValue
import com.example.ui.components.speedUnitLabel
import com.example.viewmodel.CockpitViewModel
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
  val settings by sessionVm.settings.collectAsStateWithLifecycle()
  val rides by cockpitVm.rides.collectAsStateWithLifecycle()
  val vehicles by cockpitVm.vehicles.collectAsStateWithLifecycle()
  val profile by sessionVm.profile.collectAsStateWithLifecycle()
  val fleetDebrief by cockpitVm.fleetDebrief.collectAsStateWithLifecycle()
  val fleetDebriefBusy by cockpitVm.fleetDebriefBusy.collectAsStateWithLifecycle()

  val activeVehicle =
    remember(vehicles, profile?.activeVehicleId) {
      vehicles.firstOrNull { it.id == profile?.activeVehicleId } ?: vehicles.firstOrNull()
    }

  var studioMode by remember(initialRideId) { mutableIntStateOf(if (initialRideId != null) 1 else 0) } // 0=Fleet Graphs, 1=Per-Ride Deep Dive
  var vehicleFilter by remember { mutableStateOf("ALL") }
  var windowLimit by remember { mutableIntStateOf(0) } // 0=All, 5, 10, 25
  var selectedRideId by remember(rides, initialRideId) {
    mutableStateOf(
      initialRideId?.takeIf { id -> rides.any { it.id == id } }
        ?: rides.firstOrNull()?.id.orEmpty()
    )
  }
  var pieMode by remember { mutableIntStateOf(0) } // 0=Vehicle, 1=Speed Zones, 2=Energy Wh, 3=Elevation, 4=Battery Drain
  var overlayAllTracks by remember { mutableStateOf(false) }
  var compareLeftIdx by remember(rides) { mutableIntStateOf(0) }
  var compareRightIdx by remember(rides) { mutableIntStateOf(if (rides.size > 1) 1 else 0) }
  var activeGraphTab by remember { mutableIntStateOf(0) }

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

  val vehicleOptions = remember(rides) { listOf("ALL") + rides.map { it.vehicleName }.distinct().filter { it.isNotBlank() } }
  val filteredRides =
    remember(rides, vehicleFilter, windowLimit) {
      val base = if (vehicleFilter == "ALL") rides else rides.filter { it.vehicleName == vehicleFilter }
      if (windowLimit > 0) base.take(windowLimit) else base
    }

  val distUnit = distUnitLabel(settings.distanceUnit)
  val spdUnit = speedUnitLabel(settings.distanceUnit)
  val elevUnit = elevationUnitLabel(settings.distanceUnit)

  val totalKm = remember(filteredRides) { filteredRides.sumOf { it.distanceKm } }
  val topSpeedKmh = remember(filteredRides) { filteredRides.maxOfOrNull { it.maxSpeedKmh } ?: 0.0 }
  val totalElevM = remember(filteredRides) { filteredRides.sumOf { it.elevationGainM } }
  val totalWh = remember(filteredRides) { filteredRides.sumOf { it.whUsed } }
  val totalMins = remember(filteredRides) { filteredRides.sumOf { it.durationSec } / 60 }
  val avgEff =
    remember(totalKm, totalWh, settings.distanceUnit) {
      if (settings.distanceUnit == "km") PevBatteryPhysics.whPerKm(totalWh, totalKm)
      else PevBatteryPhysics.whPerMile(totalWh, totalKm)
    }

  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = Modifier.fillMaxSize().testTag("ride_telemetry_studio_screen"),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
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
          onClick = { playCue(VoiceAudioManager.TouchCue.TAB_SWITCH); studioMode = 0 },
          leadingIcon = { Icon(Icons.AutoMirrored.Filled.ShowChart, null, Modifier.size(16.dp)) },
          label = { Text("Fleet Graphs (10)", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
          modifier = Modifier.weight(1f).testTag("studio_mode_fleet"),
        )
        FilterChip(
          selected = studioMode == 1,
          onClick = { playCue(VoiceAudioManager.TouchCue.TAB_SWITCH); studioMode = 1 },
          leadingIcon = { Icon(Icons.Default.Map, null, Modifier.size(16.dp)) },
          label = { Text("Per-Ride Deep Dive", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
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
                  onClick = { playCue(VoiceAudioManager.TouchCue.TAB_SWITCH); windowLimit = lim },
                  label = { Text(lbl, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                )
              }
              items(vehicleOptions) { vName ->
                FilterChip(
                  selected = vehicleFilter == vName,
                  onClick = { playCue(VoiceAudioManager.TouchCue.TAB_SWITCH); vehicleFilter = vName },
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

          // INDEPENDENT GRAPH WINDOW MATRIX (EACH GRAPH IN ITS OWN WINDOW)
          item {
            Text(
              "TELEMETRY WINDOW MATRIX (TAP ANY GRAPH TO OPEN IN DEDICATED WINDOW)",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          }



          val graphMeta = listOf(
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

          items(graphMeta.indices.toList()) { idx ->
            val (title, icon, accent) = graphMeta[idx]
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
                  Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = Color.White)
                  Text("Click to launch independent chart window", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                }
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = accent,
                  modifier = Modifier.padding(2.dp),
                ) {
                  Text("OPEN WINDOW", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
              }
            }
          }

          // DEDICATED FULL-SCREEN INDEPENDENT WINDOW MODAL WHEN A GRAPH IS TAPPED
          if (activeGraphTab >= 0) {
            item {
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
              ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("ACTIVE GRAPH WINDOW: ${graphMeta[activeGraphTab].first}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = { activeGraphTab = -1 }, modifier = Modifier.size(36.dp)) {
                      Icon(Icons.Default.Refresh, contentDescription = "Close Window", tint = Color.White)
                    }
                  }

                  when (activeGraphTab) {
                    0 -> HistoricalSpeedProfileChart(rides = filteredRides, settings = settings, chartHeight = 260.dp)
                    1 -> HistoricalElevationGainChart(rides = filteredRides, settings = settings, chartHeight = 260.dp)
                    2 -> HistoricalEfficiencyVoltageChart(rides = filteredRides, activeVehicle = activeVehicle, settings = settings, chartHeight = 260.dp)
                    3 -> HistoricalDistanceEnergyChart(rides = filteredRides, settings = settings, chartHeight = 260.dp)
                    4 -> {
                      val palette = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary, Color(0xFF38BDF8), Color(0xFFF97316), Color(0xFFA855F7))
                      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("By PEV", "Speed Zones", "Energy Wh", "Elevation", "Battery Drain").forEachIndexed { idx, lbl ->
                          item {
                            FilterChip(
                              selected = pieMode == idx,
                              onClick = { playCue(VoiceAudioManager.TouchCue.TAB_SWITCH); pieMode = idx },
                              label = { Text(lbl, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                            )
                          }
                        }
                      }
                      val (slices, cVal, cLbl) = remember(filteredRides, pieMode, palette, settings.distanceUnit) {
                        buildFleetPieSlices(filteredRides, pieMode, palette, settings.distanceUnit)
                      }
                      TelemetryDonutPieChart(slices = slices, centerValue = cVal, centerLabel = cLbl, reducedMotion = settings.reducedMotion, chartSize = 190.dp)
                    }
                    5 -> SpeedVsEfficiencyScatterChart(rides = filteredRides, settings = settings, chartHeight = 260.dp)
                    6 -> RideDurationHistogramChart(rides = filteredRides, reducedMotion = settings.reducedMotion, chartHeight = 240.dp)
                    7 -> HistoricalPowerAmpsChart(rides = filteredRides, activeVehicle = activeVehicle, settings = settings, chartHeight = 260.dp)
                    8 -> RiderPerformanceRadarChart(rides = filteredRides, settings = settings, chartHeight = 260.dp)
                    9 -> CumulativeOdometerRoiChart(rides = filteredRides, settings = settings, chartHeight = 260.dp)
                    else -> {
                      if (fleetDebrief.isNotBlank()) {
                        Text(fleetDebrief, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                      } else {
                        Text("Run Gemini AI across your ${filteredRides.size} Firestore ride logs.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                      Button(
                        onClick = {
                          playCue()
                          cockpitVm.requestFleetLogbookDebrief(activeVehicle = activeVehicle, batteryChemistry = settings.batteryChemistry, modelId = settings.aiModelId)
                        },
                        enabled = !fleetDebriefBusy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                      ) {
                        Text("Generate AI Fleet Report")
                      }
                    }
                  }
                }
              }
            }
          }

          // INTERACTIVE MULTI-RIDE & SINGLE-RIDE GPS TRACK MAP
          item {
            val mapRide = filteredRides.firstOrNull { it.id == selectedRideId } ?: filteredRides.first()
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
                  items(filteredRides.take(12), key = { it.id }) { r ->
                    val pts = remember(r.id, r.polylinePoints.size) { parseRideTrackPoints(r).size }
                    FilterChip(
                      selected = mapRide.id == r.id,
                      onClick = { playCue(VoiceAudioManager.TouchCue.TAB_SWITCH); selectedRideId = r.id },
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
          // MODE 1: PER-RIDE DEEP DIVE (EVERY RIDE HAS ITS OWN SPEED GRAPH, ELEVATION GRAPH, PIE CHARTS & TRACK MAP)
          item {
            Text(
              "SELECT A FIRESTORE RIDE LOG (${rides.size})",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Black,
              color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(rides, key = { it.id }) { r ->
                FilterChip(
                  selected = selectedRideId == r.id,
                  onClick = { playCue(VoiceAudioManager.TouchCue.TAB_SWITCH); selectedRideId = r.id },
                  label = { Text("${r.title.take(18)} • ${"%.1f".format(formatDistValue(r.distanceKm, settings.distanceUnit))}$distUnit", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                  modifier = Modifier.testTag("studio_pick_ride_${r.id}"),
                )
              }
            }
          }

          val activeRide = rides.firstOrNull { it.id == selectedRideId } ?: rides.first()
          item(key = "deep_dive_${activeRide.id}") {
            val effVal =
              if (settings.distanceUnit == "km") PevBatteryPhysics.whPerKm(activeRide.whUsed, activeRide.distanceKm)
              else PevBatteryPhysics.whPerMile(activeRide.whUsed, activeRide.distanceKm)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              TacticalCard(title = "${activeRide.title} • ${activeRide.vehicleName}") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  StudioKpiTile("DISTANCE", "${"%.2f".format(formatDistValue(activeRide.distanceKm, settings.distanceUnit))} $distUnit", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                  StudioKpiTile("PEAK", "${"%.1f".format(formatSpeedValue(activeRide.maxSpeedKmh, settings.distanceUnit))} $spdUnit", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                  StudioKpiTile("CLIMB", "+${formatElevationValue(activeRide.elevationGainM, settings.distanceUnit).toInt()} $elevUnit", Color(0xFF38BDF8), Modifier.weight(1f))
                  StudioKpiTile("EFF", "${"%.1f".format(effVal)} Wh/$distUnit", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                }
              }

              InteractiveRidePlaybackScrubber(
                ride = activeRide,
                activeVehicle = activeVehicle,
                settings = settings,
              )

              PerRideTelemetryPanel(
                ride = activeRide,
                activeVehicle = activeVehicle,
                settings = settings,
                showAllStacked = true,
              )

              if (activeRide.aiDebrief.isNotBlank()) {
                TacticalCard(title = "RIDGE-TO-RIDGE AI DEBRIEF") {
                  Text(activeRide.aiDebrief, style = MaterialTheme.typography.bodySmall)
                }
              }
            }
          }
        }

        item { Spacer(Modifier.height(16.dp)) }
      }
    }
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
