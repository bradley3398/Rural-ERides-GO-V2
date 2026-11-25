package com.example.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricScooter
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.TacticalNeonAhrsDashboard
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.MissionCardShareUtil
import com.example.data.HudStyle
import com.example.data.MaintenanceTask
import com.example.data.PevLegalClass
import com.example.data.PevVehicle
import com.example.data.UiState
import com.example.data.VisualStyleMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.example.ai.BatteryPredictionResult
import com.example.R
import com.example.hardware.VoiceAudioManager
import com.example.ui.components.CloudImage
import com.example.ui.components.PevScreenHeroBanner
import com.example.ui.components.PilotAvatarThumb
import com.example.ui.components.RiderLevelBadge
import com.example.ui.components.RoadSpeedSignWidget
import com.example.ui.components.RuralPevIcons
import com.example.ui.components.SpeedometerHudGauge
import com.example.ui.components.TacticalButton
import com.example.ui.components.TacticalCard
import com.example.ui.components.TacticalDialog
import com.example.ui.components.TacticalFilterChip
import com.example.ui.components.TacticalIconButton
import com.example.ui.components.TacticalMapLayer
import com.example.ui.components.TacticalOutlinedButton
import com.example.ui.components.applyThemeToMapView
import com.example.ui.components.createSafeMapView
import com.example.ui.components.availableMapLayers
import com.example.ui.components.configureSmoothMapPanning
import com.example.ui.components.defaultMapLayerId
import com.example.ui.components.distUnitLabel
import com.example.ui.components.ensureOsmdroidConfigured
import com.example.ui.components.formatDistValue
import com.example.ui.components.formatSpeedValue
import com.example.ui.components.isWindowsClassicActive
import com.example.ui.components.resolveMapTileSource
import com.example.ui.components.speedUnitLabel
import com.example.ui.components.windowsClassicBevel
import com.example.ui.theme.LocalTacticalChrome
import com.example.ui.theme.LocalTacticalSettings
import com.example.viewmodel.CockpitViewModel
import com.example.viewmodel.CopilotSocialViewModel
import com.example.viewmodel.RadarViewModel
import com.example.viewmodel.SessionViewModel
import kotlinx.coroutines.delay
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun CockpitScreen(
  sessionVm: SessionViewModel,
  cockpitVm: CockpitViewModel,
  radarVm: RadarViewModel,
  socialVm: CopilotSocialViewModel,
  onOpenRadar: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenInfo: () -> Unit,
  onOpenTelemetryStudio: (String?) -> Unit = {},
  onOpenCreatorHub: () -> Unit = {},
  onOpen3dLab: () -> Unit = {},
) {
  val ctx = LocalContext.current
  val activeUser by sessionVm.activeUser.collectAsStateWithLifecycle()
  val profile by sessionVm.profile.collectAsStateWithLifecycle()
  val settings by sessionVm.settings.collectAsStateWithLifecycle()
  val weatherState by sessionVm.weatherState.collectAsStateWithLifecycle()
  val newsState by sessionVm.newsState.collectAsStateWithLifecycle()
  val pilots by sessionVm.publicPilots.collectAsStateWithLifecycle()

  val telemetry by cockpitVm.telemetry.collectAsStateWithLifecycle()
  val vehicles by cockpitVm.vehicles.collectAsStateWithLifecycle()
  val rides by cockpitVm.rides.collectAsStateWithLifecycle()
  val maintenance by cockpitVm.maintenance.collectAsStateWithLifecycle()
  val wearStates by cockpitVm.wearStates.collectAsStateWithLifecycle()
  val serviceLogs by cockpitVm.serviceLogs.collectAsStateWithLifecycle()
  val preflightChecked by cockpitVm.preflightChecked.collectAsStateWithLifecycle()
  val trailKitChecked by cockpitVm.trailKitChecked.collectAsStateWithLifecycle()
  val debriefBusyId by cockpitVm.debriefBusyId.collectAsStateWithLifecycle()
  val fleetDebrief by cockpitVm.fleetDebrief.collectAsStateWithLifecycle()
  val fleetDebriefBusy by cockpitVm.fleetDebriefBusy.collectAsStateWithLifecycle()
  val aiSafetyBrief by cockpitVm.aiSafetyBrief.collectAsStateWithLifecycle()
  val aiSafetyBusy by cockpitVm.aiSafetyBusy.collectAsStateWithLifecycle()
  val effectiveUid = activeUser?.uid?.ifBlank { "local_pilot" } ?: "local_pilot"
  val statusBanner by cockpitVm.statusBanner.collectAsStateWithLifecycle()
  val cloudBackupBusy by sessionVm.cloudBackupBusy.collectAsStateWithLifecycle()
  val cloudBackupMessage by sessionVm.cloudBackupMessage.collectAsStateWithLifecycle()
  val activeVideo by socialVm.activeVideo.collectAsStateWithLifecycle()
  val activeRideStartBatteryPct by cockpitVm.activeRideStartBatteryPct.collectAsStateWithLifecycle()

  val activeVehicle =
    remember(vehicles, profile) {
      vehicles.firstOrNull { it.id == profile?.activeVehicleId } ?: vehicles.firstOrNull()
    }
  val myPilot = remember(pilots, activeUser) { pilots.firstOrNull { it.userId == activeUser?.uid } }
  val effectiveRiderWeightLbs =
    remember(profile?.riderWeightLbs, settings.riderWeightLbs) {
      val pW = profile?.riderWeightLbs ?: 0.0
      if (pW > 10.0) pW else settings.riderWeightLbs.toDouble().coerceAtLeast(100.0)
    }

  var showVehicleDialog by remember { mutableStateOf<PevVehicle?>(null) }
  var showAddVehicle by remember { mutableStateOf(false) }
  var showPilotProfileDialog by remember { mutableStateOf(false) }
  var showStartBatteryDialog by remember { mutableStateOf(false) }
  var showSaveTitleDialog by remember { mutableStateOf(false) }
  var missionTitle by remember { mutableStateOf("") }
  var manualSosCountdown by remember { mutableStateOf<Int?>(null) }
  var lastOverspeedToneMs by remember { mutableLongStateOf(0L) }
  var dismissedVoltMsg by remember { mutableStateOf("") }
  var shareStatusNotice by remember { mutableStateOf("") }
  var showTrailToolkit by remember { mutableStateOf(false) }
  var showLiveArHud by remember { mutableStateOf(false) }
  var showMissionMusicDialog by remember { mutableStateOf(false) }
  var preferredArMode by remember { mutableStateOf(ArHudMode.TELEMETRY_HUD) }
  val activeNavRoute by radarVm.activeNavRoute.collectAsStateWithLifecycle()
  val navStepIndex by radarVm.navCurrentStepIndex.collectAsStateWithLifecycle()
  val navDistToTurnM by radarVm.navDistanceToTurnM.collectAsStateWithLifecycle()
  val navRemainingKm by radarVm.navRemainingDistKm.collectAsStateWithLifecycle()
  val navRemainingSec by radarVm.navRemainingSec.collectAsStateWithLifecycle()
  val navOffRoute by radarVm.navOffRouteAlert.collectAsStateWithLifecycle()
  val liveChargers by radarVm.liveChargers.collectAsStateWithLifecycle()
  val isClassic = isWindowsClassicActive(settings)

  val packWhTotal = (activeVehicle?.batteryWh?.takeIf { it >= 180L } ?: 960L).toDouble()
  val remWhEst = (packWhTotal * (telemetry.batteryPct / 100.0)).coerceAtLeast(0.0)
  val totalMassKg = (effectiveRiderWeightLbs + (activeVehicle?.vehicleWeightLbs?.takeIf { it > 10.0 } ?: 58.0)) * 0.45359237
  val estWhPerKm = (18.5 * (totalMassKg / 105.0).coerceIn(0.65, 1.85)).coerceAtLeast(12.0)
  val estRemRangeKm = (remWhEst / estWhPerKm).coerceAtLeast(0.0)
  val rtbVector =
    remember(telemetry.lat, telemetry.lng, telemetry.launchLat, telemetry.launchLng, telemetry.bearingDeg, estRemRangeKm) {
      val bLat = telemetry.launchLat
      val bLng = telemetry.launchLng
      if (bLat != null && bLng != null && (telemetry.lat != 0.0 || telemetry.lng != 0.0)) {
        PevRtbMath.computeRtbVector(
          curLat = telemetry.lat,
          curLng = telemetry.lng,
          baseLat = bLat,
          baseLng = bLng,
          curHeadingDeg = telemetry.bearingDeg,
          estimatedRemainingRangeKm = estRemRangeKm,
        )
      } else null
    }

  val currentSpeedMph = telemetry.speedKmh * 0.621371
  val classCapMph =
    if (settings.legalComplianceMode && settings.pevLegalClass != PevLegalClass.OFFROAD_UNLIMITED) {
      settings.pevLegalClass.maxMph
    } else null
  val effectiveLimitMph =
    listOfNotNull(
      if (telemetry.inSchoolZone && settings.schoolZoneStrict) minOf(telemetry.speedLimitMph ?: 20, 20)
      else telemetry.speedLimitMph,
      classCapMph,
    ).minOrNull()
  val isOverspeeding = effectiveLimitMph != null && currentSpeedMph > (effectiveLimitMph + settings.speedOffsetTolerance)

  val voltCoachAlert =
    remember(isClassic, telemetry.crashDetected, isOverspeeding, rtbVector?.turnBackWarning, aiSafetyBrief) {
      when {
        !isClassic -> null
        telemetry.crashDetected ->
          "VOLT ALERT • CRASH TRIGGERED" to
            "High-G impact detected! Tap 'I'm Safe' below or cancel the SOS timer if you are unharmed."
        rtbVector?.turnBackWarning == true ->
          "VOLT ALERT • TURN BACK TO BASE" to
            "Pack range (${"%.1f".format(formatDistValue(estRemRangeKm, settings.distanceUnit))} ${distUnitLabel(settings.distanceUnit)}) is within 1.25x of your ${"%.2f".format(formatDistValue(rtbVector.distanceKm, settings.distanceUnit))} ${distUnitLabel(settings.distanceUnit)} RTB distance!"
        isOverspeeding ->
          "VOLT COACH • SPEED CAP EXCEEDED" to
            "You are riding above the active ${effectiveLimitMph} MPH limit on ${telemetry.roadName}. Ease throttle."
        aiSafetyBrief.isNotBlank() ->
          "VOLT • TACTICAL COACH BRIEFING" to aiSafetyBrief
        else -> null
      }
    }

  LaunchedEffect(voltCoachAlert?.second, isClassic, settings.retroAudioCuesEnabled) {
    val msg = voltCoachAlert?.second
    if (isClassic && !msg.isNullOrBlank() && msg != dismissedVoltMsg) {
      val cue =
        if (telemetry.crashDetected || rtbVector?.turnBackWarning == true || isOverspeeding) {
          VoiceAudioManager.RetroCueType.ALERT_WARNING
        } else {
          VoiceAudioManager.RetroCueType.COACH_BRIEF
        }
      sessionVm.voiceAudio.playRetroAudioCue(
        enabled = settings.retroAudioCuesEnabled,
        volumePct = settings.globalVolume,
        cueType = cue,
      )
    }
  }

  LaunchedEffect(currentSpeedMph, effectiveLimitMph, settings.legalComplianceMode, settings.legalAudioAlert) {
    val lim = effectiveLimitMph
    if (settings.legalComplianceMode && settings.legalAudioAlert && lim != null && currentSpeedMph > (lim + settings.speedOffsetTolerance)) {
      val now = System.currentTimeMillis()
      if (now - lastOverspeedToneMs > 6000L) {
        lastOverspeedToneMs = now
        sessionVm.voiceAudio.playSquelchChirp(isEnd = false, volumePct = settings.globalVolume)
      }
    }
  }

  val locPermLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
      cockpitVm.refreshGpsFix(settings)
    }

  var miniMapLayer by remember { mutableStateOf(defaultMapLayerId()) }
  var miniMapViewRef by remember { mutableStateOf<MapView?>(null) }
  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(lifecycleOwner, miniMapViewRef) {
    val map = miniMapViewRef
    val observer =
      LifecycleEventObserver { _, event ->
        when (event) {
          Lifecycle.Event.ON_RESUME -> map?.onResume()
          Lifecycle.Event.ON_PAUSE -> map?.onPause()
          else -> Unit
        }
      }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  val exportCsvLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
      if (uri != null) {
        val csv = cockpitVm.exportRidesAsCsv(rides)
        ctx.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
      }
    }

  var pendingGpxText by remember { mutableStateOf("") }
  val exportGpxLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/gpx+xml")) { uri ->
      if (uri != null && pendingGpxText.isNotBlank()) {
        ctx.contentResolver.openOutputStream(uri)?.use { it.write(pendingGpxText.toByteArray()) }
      }
    }

  LaunchedEffect(settings.telemetryRateHz, settings.imuPollingRateHz, settings.ahrsGaugeDamping) {
    if (cockpitVm.telemetryEngine.hasLocationPermission()) {
      cockpitVm.startHardwareFeeds(settings)
    } else {
      cockpitVm.startSensorsOnly(settings)
    }
  }

  // Continuously recompute HUD resting/loaded pack voltage whenever rig, chemistry, SoC, trip, climb, or speed changes
  LaunchedEffect(
    activeVehicle,
    settings.batteryChemistry,
    settings.rideMode,
    effectiveRiderWeightLbs,
    activeRideStartBatteryPct,
    telemetry.tripDistanceKm,
    telemetry.elevationGainM,
    telemetry.speedKmh,
    telemetry.avgSpeedKmh,
    telemetry.pitchDeg,
    telemetry.ambientTempC,
  ) {
    cockpitVm.applyTelemetryConfig(settings, activeVehicle, effectiveRiderWeightLbs)
  }

  LaunchedEffect(telemetry.lat, telemetry.lng, settings.autoWeatherSync) {
    if (telemetry.lat != 0.0) {
      if (settings.autoWeatherSync) {
        sessionVm.refreshWeatherIfNeeded(telemetry.lat, telemetry.lng)
      }
      sessionVm.pollRoadSpeedLimit(telemetry.lat, telemetry.lng) { road, limit, school ->
        cockpitVm.telemetryEngine.updateSpeedLimitMeta(road, limit, school)
      }
    }
  }

  LaunchedEffect(telemetry.crashDetected, manualSosCountdown) {
    val activeCountdown =
      if (telemetry.crashDetected && manualSosCountdown == null) settings.sosCancelWindowSec
      else manualSosCountdown
    if (activeCountdown != null && activeCountdown > 0) {
      delay(1000L)
      manualSosCountdown = activeCountdown - 1
    } else if (activeCountdown == 0) {
      manualSosCountdown = null
      cockpitVm.telemetryEngine.dismissCrashAlert()
      sessionVm.dispatchSosSms(telemetry.lat, telemetry.lng)
    }
  }

  val dashPadH = if (settings.spaciousLayoutMode) 16.dp else 14.dp
  val dashGap = if (settings.spaciousLayoutMode) 16.dp else 12.dp
  Box(Modifier.fillMaxSize()) {
    if (telemetry.recording) {
      // LOCKED NON-SCROLLING RIDE MISSION HUD (GPS Speedometer + Speed Limit + Essential Telemetry + YouTube Music)
      val cockpitView = LocalView.current
      DisposableEffect(settings.keepScreenBrightOnRide, telemetry.recording) {
        if (settings.keepScreenBrightOnRide && telemetry.recording) {
          cockpitView.keepScreenOn = true
        }
        onDispose {
          cockpitView.keepScreenOn = false
        }
      }

      val packWh = (activeVehicle?.batteryWh ?: 1500L).toDouble()
      val remWh = (packWh * (telemetry.batteryPct / 100.0)).coerceAtLeast(0.0)
      val estRemKm = (remWh / 24.0).coerceAtLeast(0.0)
      val dispDist = formatDistValue(telemetry.tripDistanceKm, settings.distanceUnit)
      val distUnit = distUnitLabel(settings.distanceUnit)
      val spdUnit = speedUnitLabel(settings.distanceUnit)
      val ytPlaying by socialVm.playerPlaying.collectAsStateWithLifecycle()
      val musicQueue by socialVm.playerQueue.collectAsStateWithLifecycle()
      val musicMixes by socialVm.musicMixesState.collectAsStateWithLifecycle()
      val cs = MaterialTheme.colorScheme
      val chrome = LocalTacticalChrome.current

      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .padding(
              start = dashPadH,
              end = dashPadH,
              top = 10.dp,
              bottom = if (activeVideo != null) 96.dp else 10.dp,
            )
            .testTag("locked_mission_hud"),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        // 1. COMPACT LOCKED MISSION STATUS BAR
        Surface(
          color = cs.surface.copy(alpha = 0.92f),
          shape = chrome.smallShape,
          border = BorderStroke(1.3.dp, if (isOverspeeding) cs.error else cs.primary.copy(alpha = 0.65f)),
          modifier = Modifier.fillMaxWidth().testTag("mission_lock_header"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.weight(1f),
            ) {
              Icon(Icons.Default.Lock, contentDescription = "Mission Locked", tint = cs.primary, modifier = Modifier.size(16.dp))
              Text(
                text = "MISSION LOCKED • ${activeVehicle?.name?.take(16) ?: "PEV"}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = cs.primary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              if (rtbVector != null) {
                Surface(
                  color = (if (rtbVector.turnBackWarning) cs.error else cs.secondary).copy(alpha = 0.18f),
                  shape = chrome.smallShape,
                ) {
                  Text(
                    text = "RTB ${"%.1f".format(formatDistValue(rtbVector.distanceKm, settings.distanceUnit))}$distUnit",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = if (rtbVector.turnBackWarning) cs.error else cs.secondary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    maxLines = 1,
                    softWrap = false,
                  )
                }
              }
              Text(
                text = "${telemetry.batteryPct}% • ${"%.1f".format(telemetry.estimatedVoltage)}V",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface,
                maxLines = 1,
                softWrap = false,
              )
            }
          }
        }

        // 2. CONDITIONAL ESSENTIAL TURN-BY-TURN NAV STRIP (IF NAVIGATION ROUTE ACTIVE)
        activeNavRoute?.let { route ->
          val activeStep = route.steps.getOrNull(navStepIndex) ?: route.steps.firstOrNull()
          Surface(
            color = if (navOffRoute) cs.errorContainer else cs.surfaceVariant.copy(alpha = 0.85f),
            shape = chrome.smallShape,
            border = BorderStroke(1.dp, if (navOffRoute) cs.error else cs.secondary.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(Icons.Default.Navigation, contentDescription = null, tint = if (navOffRoute) cs.error else cs.secondary, modifier = Modifier.size(18.dp))
              Column(Modifier.weight(1f)) {
                Text(
                  text = if (navOffRoute) "OFF-ROUTE • REROUTING" else activeStep?.instruction ?: route.destinationName,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "Turn in ${navDistToTurnM.toInt()}m • Rem ${"%.1f".format(formatDistValue(navRemainingKm, settings.distanceUnit))} $distUnit",
                  style = MaterialTheme.typography.labelSmall,
                  color = cs.onSurfaceVariant,
                  maxLines = 1,
                )
              }
              TextButton(
                onClick = { radarVm.stopGpsNavigation(announce = settings.navVoiceGuidance, volumePct = settings.globalVolume) },
                modifier = Modifier.heightIn(min = 48.dp),
              ) { Text("End Nav", style = MaterialTheme.typography.labelSmall) }
            }
          }
        }

        // 3. LOCKED SPEEDOMETER + GPS SPEED LIMIT SIGN CORE HUD
        TacticalCard(
          highlight = true,
          modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            SpeedometerHudGauge(telemetry = telemetry, settings = settings)

            RoadSpeedSignWidget(
              speedLimitMph = telemetry.speedLimitMph,
              currentSpeedMph = currentSpeedMph,
              roadName = telemetry.roadName,
              inSchoolZone = telemetry.inSchoolZone,
              toleranceOffset = settings.speedOffsetTolerance,
              schoolStrict = settings.schoolZoneStrict,
              legalComplianceMode = settings.legalComplianceMode,
              pevLegalClass = settings.pevLegalClass,
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
              StatPill("TRIP", "${"%.2f".format(dispDist)} $distUnit")
              StatPill("TIME", "%02d:%02d".format(telemetry.elapsedSec / 60, telemetry.elapsedSec % 60))
              StatPill("AVG", "${"%.1f".format(formatSpeedValue(telemetry.avgSpeedKmh, settings.distanceUnit))} $spdUnit")
              StatPill("RANGE", "${"%.1f".format(formatDistValue(estRemKm, settings.distanceUnit))} $distUnit")
            }
          }
        }

        // 4. ESSENTIAL YOUTUBE MUSIC MISSION BAR (ALWAYS AVAILABLE EVEN WHEN DASHBOARD LOCKED)
        Surface(
          color = cs.surfaceVariant.copy(alpha = 0.88f),
          shape = chrome.smallShape,
          border = BorderStroke(1.dp, if (activeVideo != null) cs.primary.copy(alpha = 0.65f) else cs.outline.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth().testTag("mission_youtube_music_bar"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = cs.primary, modifier = Modifier.size(18.dp))
            Column(
              modifier =
                Modifier
                  .weight(1f)
                  .heightIn(min = 48.dp)
                  .clickable { showMissionMusicDialog = true },
              verticalArrangement = Arrangement.Center,
            ) {
              Text(
                text = activeVideo?.title ?: "YouTube Music Ready",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = activeVideo?.channelTitle ?: "Tap to search songs or start Auto-DJ",
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            if (activeVideo != null) {
              IconButton(
                onClick = { socialVm.togglePlayerPlaying() },
                modifier = Modifier.size(48.dp).testTag("mission_music_play_pause_btn"),
              ) {
                Icon(
                  if (ytPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                  contentDescription = if (ytPlaying) "Pause Music" else "Play Music",
                  tint = cs.primary,
                )
              }
              if (musicQueue.isNotEmpty()) {
                IconButton(
                  onClick = { socialVm.playNextInQueue() },
                  modifier = Modifier.size(48.dp).testTag("mission_music_next_btn"),
                ) {
                  Icon(Icons.Default.SkipNext, contentDescription = "Next Track", tint = cs.secondary)
                }
              }
            } else {
              IconButton(
                onClick = { socialVm.launchAutoDjPevMix() },
                modifier = Modifier.size(48.dp).testTag("mission_music_autodj_btn"),
              ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Auto-DJ PEV Mix", tint = cs.secondary)
              }
            }
            IconButton(
              onClick = { showMissionMusicDialog = true },
              modifier = Modifier.size(48.dp).testTag("mission_music_search_btn"),
            ) {
              Icon(Icons.Default.Search, contentDescription = "Search YouTube Music", tint = cs.primary)
            }
          }
        }

        // 5. ESSENTIAL RIDE MISSION CONTROLS ROW
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TacticalButton(
            onClick = {
              sessionVm.voiceAudio.triggerHapticCue(
                enabled = settings.hapticFeedback && settings.rideMilestoneHaptics,
                cue = VoiceAudioManager.HapticCue.HEAVY_THUD,
              )
              showSaveTitleDialog = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = cs.error),
            isDestructive = true,
            modifier = Modifier.weight(1f).height(50.dp).testTag("toggle_ride_recording_button"),
          ) {
            Icon(Icons.Default.Stop, null)
            Spacer(Modifier.width(6.dp))
            Text("Stop & Save Ride", maxLines = 1, softWrap = false)
          }
          TacticalOutlinedButton(
            onClick = {
              sessionVm.voiceAudio.triggerHapticCue(settings.hapticFeedback, VoiceAudioManager.HapticCue.MEDIUM_CLICK)
              preferredArMode = if (activeNavRoute != null) ArHudMode.NAV_WAYFINDER else ArHudMode.TELEMETRY_HUD
              showLiveArHud = true
            },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier.height(50.dp).testTag("cockpit_ar_hud_button"),
          ) {
            Icon(RuralPevIcons.ActionArReticle, contentDescription = "AR HUD", tint = cs.secondary)
          }
          TacticalOutlinedButton(
            onClick = {
              sessionVm.voiceAudio.triggerHapticCue(settings.hapticFeedback, VoiceAudioManager.HapticCue.MEDIUM_CLICK)
              cockpitVm.calibrateZeroPitchRoll()
            },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier.height(50.dp).testTag("calibrate_imu_button"),
          ) {
            Icon(Icons.Default.CompassCalibration, contentDescription = "Zero Tilt Level")
          }
          TacticalOutlinedButton(
            onClick = {
              sessionVm.voiceAudio.triggerHapticCue(settings.hapticFeedback, VoiceAudioManager.HapticCue.WARNING_PULSE)
              manualSosCountdown = settings.sosCancelWindowSec
            },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier.height(50.dp).testTag("manual_sos_button"),
          ) {
            Icon(Icons.Default.Sos, contentDescription = "Emergency SOS", tint = cs.error)
          }
        }
      }

      if (showMissionMusicDialog) {
        var missionMusicQuery by remember { mutableStateOf("") }
        val activeGenre by socialVm.activeMusicCategory.collectAsStateWithLifecycle()
        val quickGenres = remember { listOf("Synthwave", "Lo-Fi", "Phonk", "Drum & Bass", "Rock", "Hip-Hop", "Ambient") }
        TacticalDialog(
          onDismissRequest = { showMissionMusicDialog = false },
          title = "YOUTUBE MISSION AUDIO",
          subtitle = "Search or start mixes without leaving locked HUD",
          icon = Icons.Default.MusicNote,
          badgeText = "LIVE",
          confirmButton = {
            TacticalOutlinedButton(
              onClick = { showMissionMusicDialog = false },
              modifier = Modifier.heightIn(min = 48.dp),
            ) { Text("Done") }
          },
        ) {
          Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
              OutlinedTextField(
                value = missionMusicQuery,
                onValueChange = { missionMusicQuery = it },
                placeholder = { Text("Song, artist, or mix...") },
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
              Button(
                onClick = {
                  val q = missionMusicQuery.trim()
                  if (q.isNotBlank()) socialVm.loadMusicMixes("Custom: $q", customQuery = q)
                },
                enabled = missionMusicQuery.isNotBlank(),
                modifier = Modifier.heightIn(min = 52.dp),
              ) { Icon(Icons.Default.Search, contentDescription = "Search Music") }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              item {
                FilterChip(
                  selected = false,
                  onClick = { socialVm.launchAutoDjPevMix(); showMissionMusicDialog = false },
                  label = { Text("Auto-DJ Mix") },
                  modifier = Modifier.heightIn(min = 48.dp),
                )
              }
              items(quickGenres) { genre ->
                FilterChip(
                  selected = activeGenre.equals(genre, ignoreCase = true),
                  onClick = { socialVm.loadMusicMixes(genre) },
                  label = { Text(genre) },
                  modifier = Modifier.heightIn(min = 48.dp),
                )
              }
            }
            when (val st = musicMixes) {
              is UiState.Loading -> Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(22.dp)) }
              is UiState.Error -> Text(st.message, style = MaterialTheme.typography.bodySmall, color = cs.error)
              is UiState.Success -> {
                val tracks = st.data.first
                if (tracks.isEmpty()) {
                  Text("Search a track or tap a genre to load live YouTube streams.", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                } else {
                  LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    items(tracks.take(10), key = { it.videoId }) { track ->
                      Surface(
                        onClick = {
                          socialVm.playInMiniPlayer(track, tracks, expandTheater = false)
                          showMissionMusicDialog = false
                        },
                        color = if (activeVideo?.videoId == track.videoId) cs.primaryContainer else cs.surfaceVariant,
                        shape = chrome.smallShape,
                        modifier = Modifier.fillMaxWidth(),
                      ) {
                        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                          Column(Modifier.weight(1f)) {
                            Text(track.title, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${track.channelTitle} • ${track.durationLabel}", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, maxLines = 1)
                          }
                          IconButton(
                            onClick = { socialVm.enqueuePlayNext(track) },
                            modifier = Modifier.size(48.dp),
                          ) {
                            Icon(Icons.Default.QueueMusic, contentDescription = "Queue Play Next")
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
    } else {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = dashPadH, end = dashPadH, top = 16.dp, bottom = if (activeVideo != null) 116.dp else 32.dp),
      verticalArrangement = Arrangement.spacedBy(dashGap),
    ) {
      // 1. SPACIOUS TWO-ROW PILOT HEADER + UPGRADED ICON QUICK-ACTION BAR + LIVE READINESS SCORE
      item {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.weight(1f).clickable { showPilotProfileDialog = true }.testTag("cockpit_pilot_profile_button"),
            ) {
              PilotAvatarThumb(
                avatarSource = profile?.avatarBase64.orEmpty(),
                callsign = profile?.callsign?.ifBlank { "PILOT" } ?: "PILOT",
                size = 46.dp,
              )
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Text(
                    text = profile?.callsign?.ifBlank { "PILOT" } ?: "PILOT",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                  )
                  Surface(
                    shape = LocalTacticalChrome.current.smallShape,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                  ) {
                    Text(
                      text = "${effectiveRiderWeightLbs.toInt()} lbs",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSecondaryContainer,
                      fontWeight = FontWeight.SemiBold,
                      maxLines = 1,
                      softWrap = false,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                  }
                }
                RiderLevelBadge(
                  xpPoints = myPilot?.xpPoints ?: 50L,
                  rankLabel = myPilot?.rankBadge ?: settings.pilotRankBadge,
                )
              }
            }
            Row(
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              TacticalIconButton(onClick = onOpenSettings, modifier = Modifier.testTag("cockpit_settings_button")) {
                com.example.ui.components.UpgradedIconBadge(
                  icon = RuralPevIcons.ActionSettingsCog,
                  contentDescription = "Settings",
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }

          if (settings.showQuickActionBarOnDash) {
            @Composable
            fun QuickActionTile(
              icon: androidx.compose.ui.graphics.vector.ImageVector,
              label: String,
              tint: Color,
              tag: String,
              modifier: Modifier = Modifier,
              onClick: () -> Unit,
            ) {
              Surface(
                onClick = onClick,
                shape = LocalTacticalChrome.current.buttonShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                border = BorderStroke(LocalTacticalChrome.current.buttonBorderWidth, tint.copy(alpha = 0.48f)),
                modifier = modifier.height(52.dp).testTag(tag),
              ) {
                Row(
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 6.dp),
                ) {
                  Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(5.dp))
                  Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }

            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              // ROW 1: CORE FLIGHT & ANALYTICS TOOLS
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                QuickActionTile(
                  icon = RuralPevIcons.ActionArReticle,
                  label = "AR HUD",
                  tint = MaterialTheme.colorScheme.secondary,
                  tag = "cockpit_ar_hud_button",
                  modifier = Modifier.weight(1f),
                ) {
                  sessionVm.voiceAudio.triggerHapticCue(settings.hapticFeedback && settings.buttonHapticFeedback, VoiceAudioManager.HapticCue.MEDIUM_CLICK)
                  preferredArMode = if (activeNavRoute != null) ArHudMode.NAV_WAYFINDER else ArHudMode.TELEMETRY_HUD
                  showLiveArHud = true
                }

                QuickActionTile(
                  icon = RuralPevIcons.AnalyticsLine,
                  label = "Graphs",
                  tint = Color(0xFFA855F7),
                  tag = "cockpit_quick_studio_button",
                  modifier = Modifier.weight(1f),
                ) {
                  onOpenTelemetryStudio(null)
                }

                QuickActionTile(
                  icon = Icons.Default.FlashlightOn,
                  label = "Trail Kit",
                  tint = Color(0xFFF59E0B),
                  tag = "cockpit_trail_toolkit_button",
                  modifier = Modifier.weight(1f),
                ) {
                  sessionVm.voiceAudio.triggerHapticCue(settings.hapticFeedback && settings.buttonHapticFeedback, VoiceAudioManager.HapticCue.LIGHT_TICK)
                  showTrailToolkit = true
                }
              }

              // ROW 2: AI LAB, 3D LAB, PRAYER & HEALTH
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                QuickActionTile(
                  icon = Icons.Default.AutoAwesome,
                  label = "AI Lab",
                  tint = Color(0xFF00C2FF),
                  tag = "cockpit_quick_creator_hub_button",
                  modifier = Modifier.weight(1f),
                ) {
                  onOpenCreatorHub()
                }

                QuickActionTile(
                  icon = Icons.Default.ViewInAr,
                  label = "3D Lab",
                  tint = Color(0xFFA855F7),
                  tag = "cockpit_3d_lab_button",
                  modifier = Modifier.weight(1f),
                ) {
                  onOpen3dLab()
                }

                QuickActionTile(
                  icon = RuralPevIcons.NavCopilotCore,
                  label = "Prayer",
                  tint = MaterialTheme.colorScheme.tertiary,
                  tag = "cockpit_prayer_button",
                  modifier = Modifier.weight(1f),
                ) {
                  sessionVm.openPrayerOverlay(true)
                }

                QuickActionTile(
                  icon = RuralPevIcons.ActionHealthPulse,
                  label = "Health",
                  tint = MaterialTheme.colorScheme.primary,
                  tag = "cockpit_info_button",
                  modifier = Modifier.weight(1f),
                ) {
                  onOpenInfo()
                }
              }
            }
          }

          if (settings.showRideReadinessScore) {
            val battPts = (telemetry.batteryPct.coerceIn(0, 100) * 0.40).toInt()
            val gpsPts = if (telemetry.lat != 0.0) 25 else if (telemetry.satellitesVisible > 0) 14 else 5
            val liveWeather = (weatherState as? UiState.Success)?.data
            val weatherPts =
              when {
                liveWeather == null -> 16
                liveWeather.precipProbPct >= 70 || liveWeather.windGustKmh >= 45.0 -> 8
                liveWeather.precipProbPct >= 35 || liveWeather.windGustKmh >= 28.0 -> 14
                else -> 20
              }
            val pendingMaint = maintenance.count { !it.completed }
            val maintPts = (15 - pendingMaint * 4).coerceIn(3, 15)
            val readinessPct = (battPts + gpsPts + weatherPts + maintPts).coerceIn(0, 100)
            val readinessCol =
              when {
                readinessPct >= 80 -> Color(0xFF22C55E)
                readinessPct >= 55 -> Color(0xFFF59E0B)
                else -> MaterialTheme.colorScheme.error
              }
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
              border = BorderStroke(1.dp, readinessCol.copy(alpha = 0.48f)),
              modifier = Modifier.fillMaxWidth().testTag("cockpit_ride_readiness_bar"),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                  Icon(RuralPevIcons.HighVoltage, contentDescription = null, tint = readinessCol, modifier = Modifier.size(16.dp))
                  Text(
                    text = "TRAIL READINESS $readinessPct%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = readinessCol,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
                Text(
                  text = "${"%.1f".format(telemetry.estimatedVoltage)}V • ${telemetry.satellitesUsed} sats • ${if (pendingMaint == 0) "Rig Clear" else "$pendingMaint due"}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }

      // LIVE TURN-BY-TURN GPS NAVIGATION BANNER ON DASHBOARD WHEN ROUTE IS ACTIVE
      activeNavRoute?.let { route ->
        item {
          LiveTurnByTurnNavBanner(
            route = route,
            currentStepIdx = navStepIndex,
            distanceToTurnM = navDistToTurnM,
            remainingDistKm = navRemainingKm,
            remainingSec = navRemainingSec,
            offRouteAlert = navOffRoute,
            currentBatteryPct = telemetry.batteryPct,
            settings = settings,
            onToggleVoice = { sessionVm.updateSettings { it.copy(navVoiceGuidance = !it.navVoiceGuidance) } },
            onReroute = {
              if (telemetry.lat != 0.0 || telemetry.lng != 0.0) {
                radarVm.startGpsNavigation(
                  startLat = telemetry.lat,
                  startLng = telemetry.lng,
                  destLat = route.destLat,
                  destLng = route.destLng,
                  destName = route.destinationName,
                  profile = route.profile,
                  settings = settings,
                  vehicleWh = activeVehicle?.batteryWh ?: 1500L,
                )
              }
            },
            onStopNav = {
              radarVm.stopGpsNavigation(
                announce = settings.navVoiceGuidance,
                volumePct = settings.globalVolume,
              )
            },
          )
        }
      }

      // RIDE DASHBOARD HERO BANNER
      item {
        PevScreenHeroBanner(
          imageRes = R.drawable.img_pev_cockpit_hero_1795208257433,
          badgeLabel = if (telemetry.recording) "RECORDING RIDE • START ${activeRideStartBatteryPct}%" else "LIVE RIDE DASHBOARD",
          title = activeVehicle?.name ?: "Rural ERides GO",
          subtitle =
            if (activeVehicle != null) {
              val ahLabel = if (activeVehicle.batteryAh > 0.5) " • ${"%.1f".format(activeVehicle.batteryAh)}Ah" else ""
              val motorLabel = if (activeVehicle.motorWatts > 0) " • ${activeVehicle.motorWatts}W" else ""
              "${activeVehicle.category} • ${activeVehicle.voltage.toInt()}V$ahLabel • ${activeVehicle.batteryWh}Wh$motorLabel"
            } else "Live Speedometer, GPS Satellites, AR View & Smart AI Helper",
          icon = Icons.Default.ElectricScooter,
          accentColor = MaterialTheme.colorScheme.primary,
          metrics =
            listOf(
              "VOLTS" to "${"%.1f".format(telemetry.estimatedVoltage)}V",
              "BATTERY" to "${telemetry.batteryPct}%",
              "SATS" to if (telemetry.satellitesVisible > 0) "${telemetry.satellitesUsed}/${telemetry.satellitesVisible}" else if (telemetry.lat != 0.0) "GPS" else "--",
            ),
          height = 124.dp,
        )
      }

      // QUICK SHORTCUTS & THEME BAR
      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (settings.quickThemeBarEnabled) {
            item {
              TacticalFilterChip(
                selected = settings.visualStyle == VisualStyleMode.WINDOWS_CLASSIC,
                onClick = {
                  sessionVm.voiceAudio.triggerHapticCue(settings.hapticFeedback && settings.buttonHapticFeedback, VoiceAudioManager.HapticCue.LIGHT_TICK)
                  val all = com.example.ui.theme.availableVisualStyles()
                  val idx = all.indexOf(settings.visualStyle)
                  val next = all[if (idx >= 0) (idx + 1) % all.size else 0]
                  sessionVm.updateSettings { it.copy(visualStyle = next) }
                },
                leadingIcon = Icons.Default.Palette,
                label = "Theme: ${settings.visualStyle.label}",
                modifier = Modifier.testTag("quick_theme_cycle_chip"),
              )
            }
          }
          item {
            TacticalFilterChip(
              selected = settings.nightVisionMode,
              onClick = { sessionVm.updateSettings { it.copy(nightVisionMode = !it.nightVisionMode, sunVisibilityMode = false) } },
              leadingIcon = Icons.Default.DarkMode,
              label = "Night Mode",
            )
          }
          item {
            TacticalFilterChip(
              selected = settings.sunVisibilityMode,
              onClick = { sessionVm.updateSettings { it.copy(sunVisibilityMode = !it.sunVisibilityMode, nightVisionMode = false) } },
              leadingIcon = Icons.Default.WbSunny,
              label = "Sunlight Boost",
            )
          }
          item {
            TacticalFilterChip(
              selected = settings.legalComplianceMode,
              onClick = {
                val allCls = PevLegalClass.entries
                val nextCls = allCls[(allCls.indexOf(settings.pevLegalClass) + 1) % allCls.size]
                sessionVm.updateSettings { it.copy(legalComplianceMode = true, pevLegalClass = nextCls) }
              },
              leadingIcon = Icons.Default.Gavel,
              label = settings.pevLegalClass.label.substringBefore(" ("),
            )
          }
          item {
            TacticalFilterChip(
              selected = false,
              onClick = { if (!cloudBackupBusy) sessionVm.backupAllToCloud() },
              leadingIcon = Icons.Default.CloudUpload,
              label = if (cloudBackupBusy) "Backing Up..." else "Cloud Backup",
            )
          }
        }
      }

      if (isClassic && voltCoachAlert != null && voltCoachAlert.second != dismissedVoltMsg) {
        item {
          VoltTacticalCoachBubble(
            title = voltCoachAlert.first,
            message = voltCoachAlert.second,
            onDismiss = { dismissedVoltMsg = voltCoachAlert.second },
          )
        }
      }

      if (statusBanner.isNotBlank() || cloudBackupMessage.isNotBlank() || shareStatusNotice.isNotBlank()) {
        item {
          TacticalCard(highlight = true, title = "SYSTEM_ALERT.SYS") {
            if (statusBanner.isNotBlank()) {
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(statusBanner, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                TextButton(onClick = { cockpitVm.clearBanner() }) { Text("OK") }
              }
            }
            if (cloudBackupMessage.isNotBlank()) {
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(cloudBackupMessage, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                TextButton(onClick = { sessionVm.clearCloudBackupMessage() }) { Text("OK") }
              }
            }
            if (shareStatusNotice.isNotBlank()) {
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(shareStatusNotice, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f))
                TextButton(onClick = { shareStatusNotice = "" }) { Text("OK") }
              }
            }
          }
        }
      }

      // 2. GPS PERMISSION CTA IF NEEDED
      if (!cockpitVm.telemetryEngine.hasLocationPermission()) {
        item {
          TacticalCard(highlight = true, title = "GPS_ARMING.EXE") {
            Text("ENABLE LIVE GPS TELEMETRY", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text("Grant location permission for live speedometer, road speed limit signs, Open-Meteo weather, and trail recording.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            TacticalButton(
              onClick = {
                locPermLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
              },
              modifier = Modifier.testTag("grant_gps_button"),
            ) { Text("Arm Live GPS Telemetry", maxLines = 1, softWrap = false) }
          }
        }
      }

      // 2B. BACKROAD LIVE COMMAND & RURAL FIELD CALCULATOR DECK
      item(key = "cockpit_rural_live_command_deck") {
        val ambientF =
          (weatherState as? UiState.Success)?.data?.tempC?.let { (it * 9.0 / 5.0) + 32.0 } ?: 62.0
        CockpitRuralLiveCommandDeck(
          telemetry = telemetry,
          activeVehicle = activeVehicle,
          settings = settings,
          ambientTempF = ambientF,
        )
      }

      // 3. GARAGE SWITCHER & ACTIVE RIG SPEC STRIP
      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          items(vehicles, key = { it.id }) { veh ->
            val selected = activeVehicle?.id == veh.id
            val chrome = LocalTacticalChrome.current
            Surface(
              onClick = { activeUser?.uid?.let { cockpitVm.selectActiveVehicle(it, veh.id) } },
              shape = chrome.smallShape,
              color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
              border = BorderStroke(if (selected) 1.4.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
              ) {
                val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                Icon(com.example.ui.components.pevCategoryIcon(veh.category), null, tint = fg, modifier = Modifier.size(16.dp))
                Text(
                  "${veh.name} (${veh.voltage.toInt()}V)",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  color = fg,
                  maxLines = 1,
                  softWrap = false,
                )
                IconButton(onClick = { showVehicleDialog = veh }, modifier = Modifier.size(48.dp)) {
                  Icon(Icons.Default.Edit, contentDescription = "Edit Vehicle", tint = fg, modifier = Modifier.size(16.dp))
                }
              }
            }
          }
          item {
            TacticalOutlinedButton(onClick = { showAddVehicle = true }, modifier = Modifier.testTag("add_vehicle_button")) {
              Icon(Icons.Default.ElectricScooter, null, Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Add PEV", maxLines = 1, softWrap = false)
            }
          }
        }
        activeVehicle?.let { veh ->
          if (settings.dashboardDensity != "minimal") {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              com.example.ui.components.PevBrandLogoBadge(
                brandOrModel = veh.name,
                logoDataOrUrl = veh.brandLogoUri,
                size = 34.dp,
              )
              if (veh.photoUri.isNotBlank()) {
                Surface(shape = LocalTacticalChrome.current.smallShape) {
                  CloudImage(
                    source = veh.photoUri,
                    contentDescription = veh.name,
                    modifier = Modifier.size(40.dp),
                  )
                }
              }
              val ahPart = if (veh.batteryAh > 0.5) " / ${"%.1f".format(veh.batteryAh)}Ah" else ""
              val motorPart = if (veh.motorWatts > 0) " • ${veh.motorWatts}W" else ""
              Text(
                "ACTIVE RIG: ${veh.name} • ${veh.batteryWh}Wh / ${veh.voltage.toInt()}V$ahPart$motorPart • Target ${veh.targetPsi.toInt()} PSI (${veh.tireSize}) • Rider ${effectiveRiderWeightLbs.toInt()} lbs",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }

      // 4. SPEEDOMETER HUD + HUD STYLE STRIP + LIVE PHYSICS MATRIX + RIDE CONTROLS
      item {
        val packWh = (activeVehicle?.batteryWh ?: 1500L).toDouble()
        val remWh = (packWh * (telemetry.batteryPct / 100.0)).coerceAtLeast(0.0)
        val estRemKm = (remWh / 24.0).coerceAtLeast(0.0)
        val dispDist = formatDistValue(telemetry.tripDistanceKm, settings.distanceUnit)
        val estUsedWh = telemetry.tripDistanceKm * 22.0
        val effStr =
          if (dispDist > 0.05) "${"%.1f".format(estUsedWh / dispDist)} Wh/${distUnitLabel(settings.distanceUnit)}"
          else "-- Wh/${distUnitLabel(settings.distanceUnit)}"

        TacticalCard(highlight = telemetry.recording, title = "COCKPIT_HUD.EXE") {
          val cockpitView = LocalView.current
          DisposableEffect(settings.keepScreenBrightOnRide, telemetry.recording) {
            if (settings.keepScreenBrightOnRide && telemetry.recording) {
              cockpitView.keepScreenOn = true
            }
            onDispose {
              cockpitView.keepScreenOn = false
            }
          }
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
              TacticalFilterChip(
                selected = settings.showAhrsTelemetryCard,
                onClick = { sessionVm.updateSettings { it.copy(showAhrsTelemetryCard = !it.showAhrsTelemetryCard) } },
                label = "Tilt & Lean",
                leadingIcon = Icons.Default.CompassCalibration,
                modifier = Modifier.testTag("toggle_ahrs_dashboard_chip"),
              )
            }
            items(HudStyle.entries) { style ->
              TacticalFilterChip(
                selected = settings.hudStyle == style,
                onClick = { sessionVm.updateSettings { it.copy(hudStyle = style) } },
                label = style.label,
              )
            }
          }
          Spacer(Modifier.height(6.dp))
          SpeedometerHudGauge(telemetry = telemetry, settings = settings)
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatPill("TRIP", "${"%.2f".format(dispDist)} ${distUnitLabel(settings.distanceUnit)}")
            StatPill("AVG", "${"%.1f".format(formatSpeedValue(telemetry.avgSpeedKmh, settings.distanceUnit))} ${speedUnitLabel(settings.distanceUnit)}")
            StatPill("RANGE", "${"%.1f".format(formatDistValue(estRemKm, settings.distanceUnit))} ${distUnitLabel(settings.distanceUnit)}")
            StatPill("TIME", "%02d:%02d".format(telemetry.elapsedSec / 60, telemetry.elapsedSec % 60))
          }
          if (settings.dashboardDensity != "minimal") {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
              StatPill("EFFICIENCY", effStr)
              StatPill("ENERGY LEFT", "${remWh.toInt()} Wh")
              StatPill("LEAN / G", "${telemetry.rollDeg.toInt()}° / ${"%.1f".format(telemetry.peakG)}G")
              StatPill("GPS SATS", if (telemetry.satellitesVisible > 0) "${telemetry.satellitesUsed}/${telemetry.satellitesVisible}" else "±${telemetry.gpsAccuracyM.toInt()}m")
            }
          }
          if (settings.legalComplianceMode) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.Gavel, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
              Text(
                "Speed Class: ${settings.pevLegalClass.label}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
          Spacer(Modifier.height(8.dp))
          RtbHomingMiniWidget(
            telemetry = telemetry,
            estRemainingRangeKm = estRemKm,
            settings = settings,
          )
          Spacer(Modifier.height(12.dp))
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TacticalButton(
              onClick = {
                sessionVm.voiceAudio.triggerHapticCue(
                  enabled = settings.hapticFeedback && settings.rideMilestoneHaptics,
                  cue = if (telemetry.recording) VoiceAudioManager.HapticCue.HEAVY_THUD else VoiceAudioManager.HapticCue.SUCCESS_DOUBLE,
                )
                if (telemetry.recording) {
                  showSaveTitleDialog = true
                } else {
                  showStartBatteryDialog = true
                }
              },
              colors =
                if (telemetry.recording) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                else ButtonDefaults.buttonColors(),
              isDestructive = telemetry.recording,
              modifier = Modifier.weight(1f).height(50.dp).testTag("toggle_ride_recording_button"),
            ) {
              Icon(if (telemetry.recording) Icons.Default.Stop else Icons.Default.PlayArrow, null)
              Spacer(Modifier.width(6.dp))
              Text(if (telemetry.recording) "Stop & Save Ride" else "Start Ride", maxLines = 1, softWrap = false)
            }
            TacticalOutlinedButton(
              onClick = {
                sessionVm.voiceAudio.triggerHapticCue(settings.hapticFeedback, VoiceAudioManager.HapticCue.MEDIUM_CLICK)
                cockpitVm.calibrateZeroPitchRoll()
              },
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
              modifier = Modifier.height(50.dp).testTag("calibrate_imu_button"),
            ) {
              Icon(Icons.Default.CompassCalibration, contentDescription = "Zero Tilt Level")
            }
            TacticalOutlinedButton(
              onClick = {
                sessionVm.voiceAudio.triggerHapticCue(settings.hapticFeedback, VoiceAudioManager.HapticCue.WARNING_PULSE)
                manualSosCountdown = settings.sosCancelWindowSec
              },
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
              modifier = Modifier.height(50.dp).testTag("manual_sos_button"),
            ) {
              Icon(Icons.Default.Sos, contentDescription = "Emergency SOS", tint = MaterialTheme.colorScheme.error)
            }
          }
        }
      }

      // 4A. LIVE GPS SATELLITES & PHONE SENSORS HUB CARD
      if (settings.showSatelliteStatusOnDash || settings.showEnvironmentSensorsOnDash) {
        item(key = "live_satellites_sensors_card") {
          TacticalCard(highlight = telemetry.satellitesUsed >= 4, title = "GPS Satellites & Live Sensors") {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.SatelliteAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                  Text("GPS SATELLITES & LIVE SENSORS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                  Text(
                    if (telemetry.satellitesVisible > 0) {
                      "${telemetry.satellitesUsed} Connected / ${telemetry.satellitesVisible} Visible • Signal ${"%.0f".format(telemetry.avgSatelliteSnrDb)} dB"
                    } else if (telemetry.lat != 0.0) {
                      "GPS Active (±${telemetry.gpsAccuracyM.toInt()}m) • Scanning Satellites"
                    } else {
                      "Waiting for clear sky GPS signal..."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Surface(
                color =
                  when {
                    telemetry.satellitesUsed >= 6 -> MaterialTheme.colorScheme.primaryContainer
                    telemetry.satellitesUsed >= 3 || telemetry.lat != 0.0 -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                  },
                shape = LocalTacticalChrome.current.smallShape,
              ) {
                Text(
                  text =
                    when {
                      telemetry.satellitesUsed >= 6 -> "STRONG"
                      telemetry.satellitesUsed >= 3 || telemetry.lat != 0.0 -> "LOCKED"
                      else -> "SEARCHING"
                    },
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Black,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }

            if (settings.showSatelliteStatusOnDash && telemetry.satelliteConstellations.isNotBlank()) {
              Spacer(Modifier.height(6.dp))
              Text(
                "Constellations: ${telemetry.satelliteConstellations} • Accuracy ±${telemetry.gpsAccuracyM.toInt()}m",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
              )
            }

            if (settings.showEnvironmentSensorsOnDash) {
              Spacer(Modifier.height(10.dp))
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatPill(
                  "BAROMETER",
                  if (telemetry.barometerHpa > 0f) "${"%.0f".format(telemetry.barometerHpa)} hPa" else "N/A",
                )
                StatPill(
                  "LIGHT",
                  if (telemetry.ambientLightLux >= 0f) "${telemetry.ambientLightLux.toInt()} lux" else "N/A",
                )
                StatPill(
                  "ROAD BUMP",
                  "${"%.2f".format(telemetry.roadRoughnessG)} G",
                )
                StatPill(
                  "COMPASS",
                  if (telemetry.magMicroTesla > 0f) "${telemetry.magMicroTesla.toInt()} µT" else "${telemetry.bearingDeg.toInt()}°",
                )
              }
              if (telemetry.activeSensorsList.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Icon(Icons.Default.Sensors, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                  Text(
                    "Active Hardware (${telemetry.activeSensorsList.size}): ${telemetry.activeSensorsList.joinToString(" • ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }

            Spacer(Modifier.height(10.dp))
            TacticalOutlinedButton(
              onClick = {
                sessionVm.voiceAudio.triggerHapticCue(settings.hapticFeedback, VoiceAudioManager.HapticCue.MEDIUM_CLICK)
                preferredArMode = ArHudMode.SATELLITE_SKY
                showLiveArHud = true
              },
              modifier = Modifier.fillMaxWidth().testTag("open_ar_satellites_button"),
            ) {
              Icon(Icons.Default.ViewInAr, null, Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Satellites AR", maxLines = 1, softWrap = false)
            }
          }
        }
      }

      // 4B. HIGH-CONTRAST TILT & LEAN MOTION DASHBOARD
      if (settings.showAhrsTelemetryCard) {
        item(key = "tactical_neon_ahrs_dashboard") {
          TacticalNeonAhrsDashboard(
            telemetry = telemetry,
            settings = settings,
            onTareLevel = {
              sessionVm.voiceAudio.triggerHapticCue(settings.hapticFeedback, VoiceAudioManager.HapticCue.MEDIUM_CLICK)
              cockpitVm.calibrateAhrsAllAxes()
            },
            onResetPeaks = {
              sessionVm.voiceAudio.triggerHapticCue(settings.hapticFeedback, VoiceAudioManager.HapticCue.LIGHT_TICK)
              cockpitVm.resetPeakLeanAngles()
            },
          )
        }
      }

      // 5. ROAD SPEED LIMIT SIGN & AI RIDE & SENSOR SAFETY CHECK
      item {
        RoadSpeedSignWidget(
          speedLimitMph = telemetry.speedLimitMph,
          currentSpeedMph = currentSpeedMph,
          roadName = telemetry.roadName,
          inSchoolZone = telemetry.inSchoolZone,
          toleranceOffset = settings.speedOffsetTolerance,
          schoolStrict = settings.schoolZoneStrict,
          legalComplianceMode = settings.legalComplianceMode,
          pevLegalClass = settings.pevLegalClass,
        )
      }

      item {
        TacticalCard {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
              Text("AI RIDE & SENSOR SAFETY CHECK", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
              Text("Checks live speed, satellites, sensors & weather", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TacticalOutlinedButton(
              onClick = {
                sessionVm.voiceAudio.triggerHapticCue(settings.hapticFeedback, VoiceAudioManager.HapticCue.LIGHT_TICK)
                val wSummary =
                  (weatherState as? UiState.Success)?.data?.let { "${it.conditionLabel} ${it.tempC}C, wind ${it.windSpeedKmh}km/h" } ?: "Clear"
                cockpitVm.requestTelemetrySafetyBrief(effectiveLimitMph, wSummary, settings.aiModelId)
              },
              enabled = !aiSafetyBusy,
              modifier = Modifier.testTag("ai_telemetry_safety_button"),
            ) {
              if (aiSafetyBusy) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
              } else {
                Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Check Now", maxLines = 1, softWrap = false)
              }
            }
          }
          if (aiSafetyBrief.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(aiSafetyBrief, style = MaterialTheme.typography.bodyMedium)
          }
        }
      }

      if (telemetry.lat != 0.0 || telemetry.lng != 0.0) {
        item {
          val mapLayers = remember { availableMapLayers() }
          TacticalCard(title = "Live Ride Map") {
            var miniMapUserPanned by remember { mutableStateOf(false) }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(Modifier.weight(1f)) {
                Text("LIVE RIDE MAP", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(
                  "${"%.5f".format(telemetry.lat)}, ${"%.5f".format(telemetry.lng)} • ±${telemetry.gpsAccuracyM.toInt()}m",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              TacticalIconButton(
                onClick = {
                  miniMapUserPanned = false
                  cockpitVm.refreshGpsFix(settings)
                  miniMapViewRef?.controller?.animateTo(GeoPoint(telemetry.lat, telemetry.lng), 15.8, 400L)
                },
                modifier = Modifier.testTag("cockpit_recenter_gps_button"),
              ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Re-Center GPS")
              }
            }
            if (mapLayers.size > 1) {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(mapLayers, key = { it.id }) { layer: TacticalMapLayer ->
                  TacticalFilterChip(
                    selected = miniMapLayer == layer.id,
                    onClick = { miniMapLayer = layer.id },
                    label = layer.label,
                  )
                }
              }
            }
            Spacer(Modifier.height(6.dp))
            val mapPrimaryArgb = MaterialTheme.colorScheme.primary.toArgb()
            val mapSurfaceArgb = MaterialTheme.colorScheme.surface.toArgb()
            val mapIsDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
            var lastMiniMapSig by remember { mutableStateOf("") }
            val qLat = (telemetry.lat * 10000.0).toInt()
            val qLng = (telemetry.lng * 10000.0).toInt()
            var miniMapCenteredOnce by remember { mutableStateOf(false) }
            val miniMapSig =
              "$qLat|$qLng|${telemetry.trailPoints.size}|${activeNavRoute?.destinationName.orEmpty()}|${liveChargers.size}|${settings.navShowChargersOnMap}|$miniMapLayer|$mapPrimaryArgb|$mapIsDark|${settings.themePreset.id}|${profile?.callsign.orEmpty()}"

            Box(modifier = Modifier.fillMaxWidth().height(215.dp)) {
              AndroidView(
                factory = { mapCtx ->
                  createSafeMapView(mapCtx).also { map ->
                    miniMapViewRef = map
                    configureSmoothMapPanning(map) { miniMapUserPanned = true }
                    map.setUseDataConnection(true)
                    map.setTileSource(resolveMapTileSource(miniMapLayer))
                    runCatching { map.onResume() }
                  }
                },
                update = { map ->
                  val desiredSource = resolveMapTileSource(miniMapLayer)
                  if (map.tileProvider.tileSource.name() != desiredSource.name()) {
                    map.setTileSource(desiredSource)
                  }
                  val pos = GeoPoint(telemetry.lat, telemetry.lng)
                  if (!miniMapCenteredOnce) {
                    map.controller.setCenter(pos)
                    map.controller.setZoom(15.8)
                    miniMapCenteredOnce = true
                  } else if (!miniMapUserPanned) {
                    map.controller.setCenter(pos)
                  }
                  if (lastMiniMapSig == miniMapSig) return@AndroidView
                  lastMiniMapSig = miniMapSig
                  applyThemeToMapView(map, settings, mapPrimaryArgb, mapSurfaceArgb, mapIsDark, miniMapLayer)
                  map.overlays.clear()
                  if (telemetry.trailPoints.size > 1) {
                    map.overlays.add(
                      Polyline().apply {
                        outlinePaint.color = mapPrimaryArgb
                        outlinePaint.strokeWidth = 6f
                        setPoints(telemetry.trailPoints.map { GeoPoint(it.first, it.second) })
                      }
                    )
                  }
                  activeNavRoute?.let { route ->
                    if (route.polylinePoints.size > 1) {
                      map.overlays.add(
                        Polyline().apply {
                          outlinePaint.color = android.graphics.Color.CYAN
                          outlinePaint.strokeWidth = 8f
                          setPoints(route.polylinePoints.map { GeoPoint(it.first, it.second) })
                        }
                      )
                    }
                    map.overlays.add(
                      Marker(map).apply {
                        position = GeoPoint(route.destLat, route.destLng)
                        title = "DEST: ${route.destinationName}"
                      }
                    )
                  }
                  if (settings.navShowChargersOnMap && liveChargers.isNotEmpty()) {
                    liveChargers.take(12).forEach { st ->
                      map.overlays.add(
                        Marker(map).apply {
                          position = GeoPoint(st.lat, st.lng)
                          title = "CHARGER: ${st.title}"
                        }
                      )
                    }
                  }
                  map.overlays.add(
                    Marker(map).apply {
                      position = pos
                      title = "YOU (${profile?.callsign?.ifBlank { "PILOT" } ?: "PILOT"})"
                    }
                  )
                  map.invalidate()
                },
                onRelease = { map ->
                  if (miniMapViewRef === map) miniMapViewRef = null
                  runCatching { map.onPause() }
                  runCatching { map.onDetach() }
                },
                modifier = Modifier.fillMaxSize().testTag("cockpit_live_map_view"),
              )

              Row(
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                if (miniMapUserPanned) {
                  Surface(
                    onClick = {
                      miniMapUserPanned = false
                      miniMapViewRef?.controller?.animateTo(GeoPoint(telemetry.lat, telemetry.lng), 15.8, 350L)
                    },
                    color = MaterialTheme.colorScheme.primary,
                    shape = LocalTacticalChrome.current.smallShape,
                  ) {
                    Text(
                      "LOCK GPS",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.ExtraBold,
                      color = MaterialTheme.colorScheme.onPrimary,
                      maxLines = 1,
                      softWrap = false,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    )
                  }
                }
                Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), shape = LocalTacticalChrome.current.smallShape) {
                  Row {
                    IconButton(onClick = { miniMapViewRef?.controller?.zoomIn() }, modifier = Modifier.size(48.dp)) {
                      Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { miniMapViewRef?.controller?.zoomOut() }, modifier = Modifier.size(48.dp)) {
                      Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
                    }
                  }
                }
              }
            }
          }
        }
      }

      // 6. RURAL BACKCOUNTRY READINESS, PEV POWER/MODES STRIP, VOLTAGE-SOC RANGE PLANNER, LAUNCH/BRAKE BOX, WEATHER MATRIX, FINANCIAL ROI, MAINTENANCE, LOGS, MUSIC & NEWS
      item {
        RuralTrailReadinessCard(
          telemetry = telemetry,
          vehicle = activeVehicle,
          weatherState = weatherState,
          settings = settings,
        )
      }
      item {
        PevTacticalPowerStripCard(
          telemetry = telemetry,
          vehicle = activeVehicle,
          weatherState = weatherState,
          settings = settings,
          onSelectRideMode = { mode -> sessionVm.updateSettings { it.copy(rideMode = mode) } },
        )
      }
      item {
        PevRangeAndChargePlannerCard(
          vehicle = activeVehicle,
          weatherState = weatherState,
          settings = settings,
        )
      }
      item {
        PevAccelBrakeTimerCard(
          telemetry = telemetry,
          settings = settings,
        )
      }
      item {
        WeatherMatrixCard(
          weatherState = weatherState,
          settings = settings,
          sessionVm = sessionVm,
          liveLat = telemetry.lat,
          liveLng = telemetry.lng,
          onRefresh = { sessionVm.refreshWeatherIfNeeded(telemetry.lat, telemetry.lng, force = true) },
        )
      }
      item {
        FinancialRoiCard(
          rides = rides,
          settings = settings,
          onUpdateSettings = { tf -> sessionVm.updateSettings(tf) },
        )
      }
      item {
        PredictiveMaintenanceCard(
          vehicle = activeVehicle,
          tasks = maintenance,
          wearStates = wearStates,
          serviceLogs = serviceLogs,
          preflightChecked = preflightChecked,
          trailKitChecked = trailKitChecked,
          settings = settings,
          voiceAudio = sessionVm.voiceAudio,
          onAddTask = { title, notes, due ->
            cockpitVm.upsertMaintenance(
              effectiveUid,
              MaintenanceTask(
                userId = effectiveUid,
                vehicleId = activeVehicle?.id.orEmpty(),
                vehicleName = activeVehicle?.name.orEmpty(),
                title = title,
                notes = notes,
                dueOdometerKm = due,
              ),
            )
          },
          onToggleDone = { task ->
            cockpitVm.upsertMaintenance(effectiveUid, task.copy(completed = !task.completed))
          },
          onDeleteTask = { id -> cockpitVm.deleteMaintenance(effectiveUid, id) },
          onResetWear = { itemId ->
            cockpitVm.resetWearItem(effectiveUid, itemId, activeVehicle?.odometerKm ?: 0.0)
          },
          onTogglePreflight = { itemKey ->
            cockpitVm.togglePreflightItem(effectiveUid, itemKey)
          },
          onResetPreflight = {
            cockpitVm.resetPreflightChecklist(effectiveUid)
          },
          onToggleTrailKit = { itemKey ->
            cockpitVm.toggleTrailKitItem(effectiveUid, itemKey)
          },
          onResetTrailKit = {
            cockpitVm.resetTrailKit(effectiveUid)
          },
          onAddServiceLog = { comp, notes, cost ->
            cockpitVm.addServiceLog(effectiveUid, comp, notes, cost, activeVehicle?.odometerKm ?: 0.0)
          },
          onDeleteServiceLog = { logId ->
            cockpitVm.deleteServiceLog(effectiveUid, logId)
          },
        )
      }
      item {
        MissionLogsCard(
          rides = rides,
          activeVehicle = activeVehicle,
          settings = settings,
          debriefBusyId = debriefBusyId,
          fleetDebrief = fleetDebrief,
          fleetDebriefBusy = fleetDebriefBusy,
          voiceAudio = sessionVm.voiceAudio,
          onRequestFleetDebrief = {
            cockpitVm.requestFleetLogbookDebrief(
              activeVehicle = activeVehicle,
              batteryChemistry = settings.batteryChemistry,
              modelId = settings.aiModelId,
            )
          },
          onRequestDebrief = { ride ->
            val wSummary =
              (weatherState as? UiState.Success)?.data?.let { "${it.conditionLabel} ${it.tempC}°C" } ?: "Live GPS"
            cockpitVm.requestRideDebrief(
              uid = effectiveUid,
              ride = ride,
              weatherSummary = wSummary,
              modelId = settings.aiModelId,
              vehicle = activeVehicle,
              batteryChemistry = settings.batteryChemistry,
            )
          },
          onExportGpx = { ride ->
            pendingGpxText = cockpitVm.exportRideAsGpx(ride)
            exportGpxLauncher.launch("${ride.title.replace(" ", "_")}.gpx")
          },
          onShareCard = { ride ->
            val res = MissionCardShareUtil.shareMissionCard(ctx, ride, settings)
            shareStatusNotice =
              res.fold(
                onSuccess = { "Ride Card image rendered & share sheet opened." },
                onFailure = { "Unable to share Ride Card: ${it.message}" },
              )
          },
          onOpenTelemetryStudio = onOpenTelemetryStudio,
          onExportAllCsv = { exportCsvLauncher.launch("rural_erides_rides.csv") },
          onDeleteRide = { id -> cockpitVm.deleteRide(effectiveUid, id) },
        )
      }
      item {
        YoutubeMusicAndNewsDeck(
          sessionVm = sessionVm,
          socialVm = socialVm,
          newsState = newsState,
          onRefreshNews = { sessionVm.refreshNews() },
        )
      }
    }
    }

    // SOS CRASH COUNTDOWN DIALOG
    val count = manualSosCountdown
    if (count != null) {
      TacticalDialog(
        onDismissRequest = {
          manualSosCountdown = null
          cockpitVm.telemetryEngine.dismissCrashAlert()
        },
        title = "EMERGENCY SOS BEACON ($count s)",
        subtitle = "Active Crash / Distress Dispatch",
        icon = Icons.Default.Sos,
        badgeText = "SOS",
        confirmButton = {
          TacticalButton(
            onClick = {
              manualSosCountdown = null
              cockpitVm.telemetryEngine.dismissCrashAlert()
              sessionVm.dispatchSosSms(telemetry.lat, telemetry.lng)
            },
            isDestructive = true,
          ) { Text("Send SOS Now") }
        },
        dismissButton = {
          TacticalOutlinedButton(onClick = {
            manualSosCountdown = null
            cockpitVm.telemetryEngine.dismissCrashAlert()
          }) { Text("I'm Safe (Cancel)") }
        },
      ) {
        Text(
          "Crash or manual SOS triggered at ${"%.5f".format(telemetry.lat)}, ${"%.5f".format(telemetry.lng)}. Dispatching SOS SMS and emergency dialer when timer hits 0.",
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    }

    if (showStartBatteryDialog) {
      StartRideBatteryDialog(
        vehicle = activeVehicle,
        initialPct = telemetry.batteryPct,
        onDismiss = { showStartBatteryDialog = false },
        onConfirmStart = { startPct ->
          cockpitVm.startRide(
            startBatteryPct = startPct,
            activeVehicle = activeVehicle,
            settings = settings,
          )
          sessionVm.voiceAudio.playTouchSound(
            enabled = settings.touchSoundsEnabled,
            pack = settings.touchSoundPack,
            cue = VoiceAudioManager.TouchCue.RIDE_START,
            volumePct = settings.globalVolume,
          )
          if (isClassic) {
            sessionVm.voiceAudio.playRetroAudioCue(
              enabled = settings.retroAudioCuesEnabled,
              volumePct = settings.globalVolume,
              cueType = VoiceAudioManager.RetroCueType.MISSION_START,
            )
          }
          showStartBatteryDialog = false
        },
      )
    }

    if (showSaveTitleDialog) {
      var aiBatteryPred by remember { mutableStateOf<BatteryPredictionResult?>(null) }
      var aiPredBusy by remember { mutableStateOf(false) }
      EndRideBatteryDialog(
        vehicle = activeVehicle,
        startBatteryPct = activeRideStartBatteryPct,
        estimatedRemainingPct = telemetry.batteryPct,
        missionTitle = missionTitle,
        onTitleChange = { missionTitle = it },
        aiPrediction = aiBatteryPred,
        predictionBusy = aiPredBusy,
        onRequestAiPrediction = {
          aiPredBusy = true
          cockpitVm.predictEndRideBattery(
            activeVehicle = activeVehicle,
            startBatteryPct = activeRideStartBatteryPct,
            riderWeightLbs = effectiveRiderWeightLbs,
            modelId = settings.aiModelId,
          ) { pred ->
            aiBatteryPred = pred
            aiPredBusy = false
          }
        },
        onDismiss = { showSaveTitleDialog = false },
        onConfirmSave = { endPct ->
          val wLabel = (weatherState as? UiState.Success)?.data?.conditionLabel ?: "Live Telemetry"
          cockpitVm.stopAndSaveRide(
            uid = effectiveUid,
            activeVehicle = activeVehicle,
            title = missionTitle,
            weatherSummary = wLabel,
            autoCloudSync = settings.autoCloudSyncOnFinish,
            startBatteryPct = activeRideStartBatteryPct,
            endBatteryPct = endPct,
            batteryChemistry = settings.batteryChemistry,
          )
          sessionVm.voiceAudio.playTouchSound(
            enabled = settings.touchSoundsEnabled,
            pack = settings.touchSoundPack,
            cue = VoiceAudioManager.TouchCue.RIDE_STOP,
            volumePct = settings.globalVolume,
          )
          if (isClassic) {
            sessionVm.voiceAudio.playRetroAudioCue(
              enabled = settings.retroAudioCuesEnabled,
              volumePct = settings.globalVolume,
              cueType = VoiceAudioManager.RetroCueType.MISSION_STOP,
            )
          }
          missionTitle = ""
          showSaveTitleDialog = false
        },
      )
    }

    if (showAddVehicle || showVehicleDialog != null) {
      PevVehicleProfileDialog(
        initial = showVehicleDialog,
        currentRiderWeightLbs = effectiveRiderWeightLbs,
        currentAvatarBase64 = profile?.avatarBase64.orEmpty(),
        onLookupSpecs = { query, onOk, onErr ->
          cockpitVm.lookupPevSpecs(query, settings.aiModelId, onOk, onErr)
        },
        onFetchPevPhoto = { query, onOk ->
          cockpitVm.fetchPevPhotoBase64(query, onOk)
        },
        onDeepSearchModels = { query, onOk, onErr ->
          cockpitVm.searchPevModelCandidates(query, settings.aiModelId, onOk, onErr)
        },
        onDismiss = {
          showAddVehicle = false
          showVehicleDialog = null
        },
        onSave = { veh, nextWeightLbs, nextAvatarBase64 ->
          activeUser?.uid?.let { cockpitVm.saveVehicle(it, veh) }
          sessionVm.updateRiderProfileDetails(nextWeightLbs, nextAvatarBase64) { msg ->
            shareStatusNotice = msg
          }
          showAddVehicle = false
          showVehicleDialog = null
        },
      )
    }

    if (showPilotProfileDialog) {
      PilotProfileDialog(
        profile = profile,
        fallbackWeightLbs = settings.riderWeightLbs,
        onDismiss = { showPilotProfileDialog = false },
        onSave = { nextWeightLbs, nextAvatarBase64 ->
          sessionVm.updateRiderProfileDetails(nextWeightLbs, nextAvatarBase64) { msg ->
            shareStatusNotice = msg
          }
          showPilotProfileDialog = false
        },
      )
    }

    if (showTrailToolkit) {
      TrailPowerToolkitSheet(
        liveSpeedKmh = telemetry.speedKmh,
        settings = settings,
        voiceAudio = sessionVm.voiceAudio,
        onDismiss = { showTrailToolkit = false },
      )
    }

    if (showLiveArHud) {
      LiveArHudOverlay(
        telemetry = telemetry,
        settings = settings,
        activeNavRoute = activeNavRoute,
        navStepIndex = navStepIndex,
        navDistToTurnM = navDistToTurnM,
        nearestCharger = liveChargers.firstOrNull(),
        initialMode = preferredArMode,
        onCancelNav = {
          radarVm.stopGpsNavigation(
            announce = settings.navVoiceGuidance,
            volumePct = settings.globalVolume,
          )
        },
        onDismiss = { showLiveArHud = false },
      )
    }
  }
}

@Composable
private fun StatPill(label: String, value: String) {
  val settings = LocalTacticalSettings.current
  val isClassic = isWindowsClassicActive(settings)
  if (isClassic) {
    Column(
      modifier =
        Modifier
          .background(MaterialTheme.colorScheme.surfaceVariant)
          .windowsClassicBevel(sunken = true)
          .padding(horizontal = 8.dp, vertical = 5.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
    return
  }
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    border = BorderStroke(LocalTacticalChrome.current.buttonBorderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.42f)),
    shape = LocalTacticalChrome.current.smallShape,
  ) {
    Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
  }
}
