package com.example.ui.screens

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Fence
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.example.data.PublicPilotProfile
import com.example.data.RiderMatchEngine
import com.example.data.UiState
import com.example.ui.components.RuralPevIcons
import com.example.ui.components.TacticalButton
import com.example.ui.components.TacticalCard
import com.example.ui.components.TacticalIconButton
import com.example.ui.components.TacticalOutlinedButton
import com.example.ui.components.applyThemeToMapView
import com.example.ui.components.createSafeMapView
import com.example.ui.components.availableMapLayers
import com.example.ui.components.buildMapCirclePolygon
import com.example.ui.components.configureSmoothMapPanning
import com.example.ui.components.defaultMapLayerId
import com.example.ui.components.distUnitLabel
import com.example.ui.components.ensureOsmdroidConfigured
import com.example.ui.components.formatDistValue
import com.example.ui.components.formatSpeedValue
import com.example.ui.components.resolveMapTileSource
import com.example.ui.components.speedUnitLabel
import com.example.viewmodel.CockpitViewModel
import com.example.viewmodel.CopilotSocialViewModel
import com.example.viewmodel.RadarViewModel
import com.example.viewmodel.SessionViewModel
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay

@Composable
fun RiderRadarScreen(
  sessionVm: SessionViewModel,
  cockpitVm: CockpitViewModel,
  radarVm: RadarViewModel,
  socialVm: CopilotSocialViewModel,
) {
  val ctx = LocalContext.current
  val activeUser by sessionVm.activeUser.collectAsStateWithLifecycle()
  val profile by sessionVm.profile.collectAsStateWithLifecycle()
  val settings by sessionVm.settings.collectAsStateWithLifecycle()
  val pilots by sessionVm.publicPilots.collectAsStateWithLifecycle()
  val weatherState by sessionVm.weatherState.collectAsStateWithLifecycle()
  val telemetry by cockpitVm.telemetry.collectAsStateWithLifecycle()
  val riders by radarVm.riders.collectAsStateWithLifecycle()
  val meetups by radarVm.meetups.collectAsStateWithLifecycle()
  val hazards by radarVm.hazards.collectAsStateWithLifecycle()
  val waypoints by radarVm.waypoints.collectAsStateWithLifecycle()
  val elevation by radarVm.elevationProfile.collectAsStateWithLifecycle()
  val lockedUid by radarVm.lockedTargetUid.collectAsStateWithLifecycle()
  val rulerAnchor by radarVm.rulerAnchor.collectAsStateWithLifecycle()
  val geoFenceCenter by radarVm.geoFenceCenter.collectAsStateWithLifecycle()
  val geoFenceRadiusM by radarVm.geoFenceRadiusM.collectAsStateWithLifecycle()
  val geoFenceBreached by radarVm.geoFenceBreached.collectAsStateWithLifecycle()
  val activeNavRoute by radarVm.activeNavRoute.collectAsStateWithLifecycle()
  val navSearching by radarVm.navSearching.collectAsStateWithLifecycle()
  val navRouting by radarVm.navRouting.collectAsStateWithLifecycle()
  val navSearchResults by radarVm.navSearchResults.collectAsStateWithLifecycle()
  val navStepIndex by radarVm.navCurrentStepIndex.collectAsStateWithLifecycle()
  val navDistToTurnM by radarVm.navDistanceToTurnM.collectAsStateWithLifecycle()
  val navRemainingKm by radarVm.navRemainingDistKm.collectAsStateWithLifecycle()
  val navRemainingSec by radarVm.navRemainingSec.collectAsStateWithLifecycle()
  val navOffRoute by radarVm.navOffRouteAlert.collectAsStateWithLifecycle()
  val navStatusMsg by radarVm.navStatusMsg.collectAsStateWithLifecycle()
  val liveChargers by radarVm.liveChargers.collectAsStateWithLifecycle()
  val chargersLoading by radarVm.chargersLoading.collectAsStateWithLifecycle()
  val socialNotice by socialVm.socialNotice.collectAsStateWithLifecycle()

  var radarVisible by remember { mutableStateOf(settings.publicProfile) }
  var statusTag by remember { mutableStateOf("CRUISING") }
  var mapLayer by remember { mutableStateOf(defaultMapLayerId()) }
  var dopplerOn by remember { mutableStateOf(false) }
  var selectedTab by remember { mutableIntStateOf(0) }
  var showWalkie by remember { mutableStateOf(false) }
  var showCreateDialog by remember { mutableStateOf<String?>(null) }
  var dmPeer by remember { mutableStateOf<PublicPilotProfile?>(null) }
  var centeredOnFallback by remember { mutableStateOf(false) }
  var centeredOnRealGps by remember { mutableStateOf(false) }
  var userPannedMap by remember { mutableStateOf(false) }
  var mapViewRef by remember { mutableStateOf<MapView?>(null) }
  var hasLocPerm by remember { mutableStateOf(cockpitVm.telemetryEngine.hasLocationPermission()) }
  val vehicles by cockpitVm.vehicles.collectAsStateWithLifecycle()
  val activeVehicle = remember(vehicles, profile?.activeVehicleId) { vehicles.firstOrNull { it.id == profile?.activeVehicleId } ?: vehicles.firstOrNull() }
  var radarLayers by remember { mutableStateOf(RadarLayerVisibility()) }
  var riderSort by remember { mutableStateOf(RadarRiderSort.NEAREST) }
  var selectedFenceRadiusM by remember { mutableIntStateOf(800) }
  var hazardTypeFilter by remember { mutableStateOf("ALL") }
  var waypointTypeFilter by remember { mutableStateOf("ALL") }
  val lifecycleOwner = LocalLifecycleOwner.current

  val locPermLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
      hasLocPerm = result.values.any { it } || cockpitVm.telemetryEngine.hasLocationPermission()
      if (hasLocPerm) {
        cockpitVm.refreshGpsFix(settings)
      }
    }

  LaunchedEffect(Unit) {
    ensureOsmdroidConfigured(ctx)
    hasLocPerm = cockpitVm.telemetryEngine.hasLocationPermission()
    if (hasLocPerm) {
      cockpitVm.refreshGpsFix(settings)
    }
  }

  DisposableEffect(lifecycleOwner, mapViewRef) {
    val map = mapViewRef
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

  val myUid = activeUser?.uid.orEmpty()
  val cloudSyncReady = activeUser != null
  val hasGpsFix = telemetry.lat != 0.0 || telemetry.lng != 0.0
  val callsign = profile?.callsign?.ifBlank { "PILOT" } ?: "PILOT"
  val myUsableRangeMi =
    remember(activeVehicle, telemetry.batteryPct) {
      RiderMatchEngine.estimateUsableRangeMiles(activeVehicle, telemetry.batteryPct)
    }
  val encodedRigSpec =
    remember(
      activeVehicle?.name,
      settings.matchPaceTier,
      settings.matchTerrainPref,
      myUsableRangeMi,
      settings.matchScheduleVibe,
    ) {
      RiderMatchEngine.encodeRadarRigSpec(
        vehicleName = activeVehicle?.name ?: "PEV Rig",
        pace = settings.matchPaceTier,
        terrain = settings.matchTerrainPref,
        usableRangeMi = myUsableRangeMi,
        vibe = settings.matchScheduleVibe,
      )
    }

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

  LaunchedEffect(telemetry.lat, telemetry.lng, telemetry.speedKmh, statusTag, radarVisible, encodedRigSpec) {
    radarVm.publishBeaconIfDue(
      callsign = callsign,
      vehicleName = encodedRigSpec,
      lat = telemetry.lat,
      lng = telemetry.lng,
      speedKmh = telemetry.speedKmh,
      headingDeg = telemetry.bearingDeg.toDouble(),
      batteryPct = telemetry.batteryPct,
      statusTag = statusTag,
      visible = radarVisible,
    )
    if (telemetry.trailPoints.size >= 4) {
      radarVm.refreshElevationSparkline(telemetry.trailPoints)
    }
  }

  val mutedPeerIds by socialVm.mutedPeerIds.collectAsStateWithLifecycle()
  val pilotsByUid = remember(pilots) { pilots.associateBy { it.userId } }
  val radiusFilteredRiders =
    remember(riders, mutedPeerIds, myUid, telemetry.lat, telemetry.lng, settings.radarRadiusMiles, hasGpsFix) {
      val maxKm = if (settings.radarRadiusMiles >= 500) 99_999.0 else settings.radarRadiusMiles * 1.60934
      riders.filter { r ->
        val isSelf = myUid.isNotBlank() && r.userId == myUid
        val authentic =
          isSelf ||
            com.example.data.PilotAuthenticityGuard.isAuthenticCallsignAndUid(
              userId = r.userId,
              callsign = r.callsign,
              currentUid = myUid.takeIf { it.isNotBlank() },
            )
        authentic &&
          (isSelf || !mutedPeerIds.contains(r.userId)) &&
          (!hasGpsFix || RadarViewModel.haversineKm(telemetry.lat, telemetry.lng, r.lat, r.lng) <= maxKm)
      }
    }
  val nearbyRidersWithMatch =
    remember(radiusFilteredRiders, pilotsByUid, settings, telemetry.lat, telemetry.lng, riderSort, hasGpsFix) {
      radiusFilteredRiders
        .map { r ->
          val peerProfile = pilotsByUid[r.userId]
          val rawSpecSource =
            when {
              r.vehicleName.contains(" |") -> r.vehicleName
              peerProfile != null && peerProfile.vehicleSummary.contains(" |") -> peerProfile.vehicleSummary
              else -> peerProfile?.vehicleSummary?.ifBlank { r.vehicleName } ?: r.vehicleName
            }
          val peerSpec = RiderMatchEngine.decodeRigMatchSpec(rawSpecSource, r.speedKmh, r.batteryPct)
          val score = RiderMatchEngine.scoreRiderMatch(settings, r, peerProfile)
          Triple(r, peerSpec, score)
        }
        .filter { (_, peerSpec, score) ->
          RiderMatchEngine.matchesActiveFilters(settings, peerSpec, score)
        }
        .let { list ->
          when (riderSort) {
            RadarRiderSort.BEST_MATCH -> list.sortedByDescending { it.third.scorePct }
            RadarRiderSort.NEAREST ->
              if (hasGpsFix) list.sortedBy { RadarViewModel.haversineKm(telemetry.lat, telemetry.lng, it.first.lat, it.first.lng) }
              else list
            RadarRiderSort.FASTEST -> list.sortedByDescending { it.first.speedKmh }
            RadarRiderSort.LOWEST_BATTERY -> list.sortedBy { it.first.batteryPct }
          }
        }
    }
  val nearbyRiders = remember(nearbyRidersWithMatch) { nearbyRidersWithMatch.map { it.first } }

  val nearestPeer =
    remember(nearbyRiders, myUid, telemetry.lat, telemetry.lng, hasGpsFix) {
      if (!hasGpsFix) nearbyRiders.firstOrNull { it.userId != myUid }
      else nearbyRiders.filter { it.userId != myUid }.minByOrNull {
        RadarViewModel.haversineKm(telemetry.lat, telemetry.lng, it.lat, it.lng)
      }
    }

  val nearestChargerKm =
    remember(waypoints, telemetry.lat, telemetry.lng, hasGpsFix) {
      if (!hasGpsFix) null
      else waypoints.minOfOrNull { RadarViewModel.haversineKm(telemetry.lat, telemetry.lng, it.lat, it.lng) }
    }

  val lockedTarget = nearbyRiders.firstOrNull { it.userId == lockedUid }

  Box(Modifier.fillMaxSize()) {
    val pad = if (settings.spaciousLayoutMode) 16.dp else 12.dp
    val gap = if (settings.spaciousLayoutMode) 16.dp else 10.dp
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = pad, end = pad, top = pad, bottom = 104.dp),
      verticalArrangement = Arrangement.spacedBy(gap),
    ) {
      item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Column(Modifier.weight(1f)) {
            Text("Map & Nearby Riders", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(
              "${nearbyRiders.size} riders within ${settings.radarRadiusMiles} mi • ${telemetry.satellitesUsed}/${telemetry.satellitesVisible} GPS sats" +
                if (settings.legalComplianceMode) " • ${settings.pevLegalClass.label}" else "",
              style = MaterialTheme.typography.labelSmall,
            )
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Visible", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.width(4.dp))
            Switch(
              checked = radarVisible,
              onCheckedChange = {
                radarVisible = it
                radarVm.publishBeaconIfDue(
                  callsign,
                  encodedRigSpec,
                  telemetry.lat,
                  telemetry.lng,
                  telemetry.speedKmh,
                  telemetry.bearingDeg.toDouble(),
                  telemetry.batteryPct,
                  statusTag,
                  it,
                  force = true,
                )
              },
            )
          }
        }
      }

      if (socialNotice.isNotBlank()) {
        item {
          TacticalCard(highlight = true) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text(socialNotice, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
              TextButton(onClick = { socialVm.clearNotice() }) { Text("OK") }
            }
          }
        }
      }

      if (!cloudSyncReady) {
        item {
          TacticalCard(highlight = true, title = "CONVOY_LINK.SYS") {
            Text(
              "Local GPS map, RainViewer storm radar, GeoFence, and Ruler work offline. Sign in via Settings (Email or Google) to broadcast live beacons, Walkie PTT audio, and P2P calls.",
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }
      }

      // LIVE TURN-BY-TURN GPS NAVIGATION BANNER
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
              if (hasGpsFix) {
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

      // NEAREST-TARGET TACTICAL HUD STRIP & 1-TAP SOS FLARE
      item {
        val wData = (weatherState as? UiState.Success)?.data
        val nearestRiderLabel =
          if (nearestPeer != null && hasGpsFix) {
            val d = formatDistValue(RadarViewModel.haversineKm(telemetry.lat, telemetry.lng, nearestPeer.lat, nearestPeer.lng), settings.distanceUnit)
            "${nearestPeer.callsign} (${"%.1f".format(d)}${distUnitLabel(settings.distanceUnit)})"
          } else {
            nearestPeer?.callsign ?: "None in sector"
          }
        val chargerLabel =
          if (nearestChargerKm != null) "${"%.1f".format(formatDistValue(nearestChargerKm, settings.distanceUnit))}${distUnitLabel(settings.distanceUnit)}"
          else "${waypoints.size} pins"

        val activeRigWh = 1500.0
        val estRemKm = ((activeRigWh * (telemetry.batteryPct / 100.0)) / 24.0).coerceAtLeast(0.0)
        val chargerReachStatus =
          if (nearestChargerKm != null) {
            if (nearestChargerKm <= estRemKm * 0.85) "REACH SAFE" else "LOW MARGIN"
          } else "NO PIN"
        TacticalCard(highlight = statusTag == "SOS", title = "SECTOR_HUD.SYS") {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
              Text(
                "Nearest Pilot: $nearestRiderLabel • Hazards: ${hazards.size}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                "Waypoint: $chargerLabel ($chargerReachStatus)" +
                  (wData?.let { " • Wind ${"%.0f".format(formatSpeedValue(it.windSpeedKmh, settings.distanceUnit))} ${speedUnitLabel(settings.distanceUnit)} (${it.conditionLabel})" } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            TacticalButton(
              onClick = {
                val nextTag = if (statusTag == "SOS") "CRUISING" else "SOS"
                statusTag = nextTag
                radarVisible = true
                radarVm.publishBeaconIfDue(
                  callsign = callsign,
                  vehicleName = encodedRigSpec,
                  lat = telemetry.lat,
                  lng = telemetry.lng,
                  speedKmh = telemetry.speedKmh,
                  headingDeg = telemetry.bearingDeg.toDouble(),
                  batteryPct = telemetry.batteryPct,
                  statusTag = nextTag,
                  visible = true,
                  force = true,
                )
              },
              colors =
                if (statusTag == "SOS") ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)),
              isDestructive = true,
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
              modifier = Modifier.testTag("radar_sos_flare_button"),
            ) {
              Icon(Icons.Default.Sos, null, Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text(if (statusTag == "SOS") "SOS ACTIVE" else "SOS Flare", maxLines = 1, softWrap = false)
            }
          }
        }
      }

      if (geoFenceBreached) {
        item {
          Surface(
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              "GEO-FENCE PERIMETER BREACH ALERT (> ${geoFenceRadiusM}m)",
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.labelLarge,
              modifier = Modifier.padding(10.dp),
            )
          }
        }
      }

      // MAP LAYERS, AUTO-FOLLOW, SPEED ZOOM & DOPPLER
      item {
        val mapLayers = remember { availableMapLayers() }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(mapLayers, key = { it.id }) { layer ->
            FilterChip(
              selected = mapLayer == layer.id,
              onClick = { mapLayer = layer.id },
              label = { Text(layer.label, maxLines = 1, softWrap = false) },
              modifier = Modifier.testTag("radar_layer_${layer.id.lowercase()}"),
            )
          }
          item {
            FilterChip(
              selected = settings.radarAutoFollow,
              onClick = { sessionVm.updateSettings { it.copy(radarAutoFollow = !it.radarAutoFollow) } },
              leadingIcon = { Icon(Icons.Default.GpsFixed, null, Modifier.size(16.dp)) },
              label = { Text("Auto-Follow", maxLines = 1, softWrap = false) },
            )
          }
          item {
            FilterChip(
              selected = settings.radarSpeedZoom,
              onClick = { sessionVm.updateSettings { it.copy(radarSpeedZoom = !it.radarSpeedZoom) } },
              leadingIcon = { Icon(Icons.Default.ZoomIn, null, Modifier.size(16.dp)) },
              label = { Text("Speed Zoom", maxLines = 1, softWrap = false) },
            )
          }
          item {
            FilterChip(selected = dopplerOn, onClick = { dopplerOn = !dopplerOn }, label = { Text("Doppler Storm", maxLines = 1, softWrap = false) })
          }
          item {
            FilterChip(
              selected = geoFenceCenter != null,
              onClick = {
                if (geoFenceCenter == null && hasGpsFix) {
                  radarVm.setGeoFence(telemetry.lat, telemetry.lng, selectedFenceRadiusM)
                } else radarVm.clearGeoFence()
              },
              label = { Text(if (geoFenceCenter == null) "Arm Fence (${selectedFenceRadiusM}m)" else "Clear Fence", maxLines = 1, softWrap = false) },
              leadingIcon = { Icon(Icons.Default.Fence, null, Modifier.size(16.dp)) },
            )
          }
          items(listOf(250, 800, 2000)) { radM ->
            FilterChip(
              selected = selectedFenceRadiusM == radM,
              onClick = {
                selectedFenceRadiusM = radM
                geoFenceCenter?.let { (gLat, gLng) -> radarVm.setGeoFence(gLat, gLng, radM) }
              },
              label = { Text(if (radM >= 1000) "${radM / 1000}km" else "${radM}m", maxLines = 1, softWrap = false) },
              modifier = Modifier.testTag("fence_radius_$radM"),
            )
          }
          item {
            FilterChip(
              selected = rulerAnchor != null,
              onClick = {
                if (rulerAnchor == null && hasGpsFix) radarVm.setRulerAnchor(telemetry.lat, telemetry.lng)
                else radarVm.clearRulerAnchor()
              },
              label = { Text("Ruler", maxLines = 1, softWrap = false) },
              leadingIcon = { Icon(Icons.Default.Straighten, null, Modifier.size(16.dp)) },
            )
          }
        }
      }

      // LIVE MAPBOX / OSMDROID TACTICAL MAP
      item {
        TacticalCard(title = "SECTOR_MAP.EXE") {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(Modifier.weight(1f)) {
              Text(
                if (hasGpsFix) {
                  "GPS LOCK: ${"%.5f".format(telemetry.lat)}, ${"%.5f".format(telemetry.lng)} (±${telemetry.gpsAccuracyM.toInt()}m)"
                } else if (!hasLocPerm) {
                  "Location permission required for true GPS fix"
                } else {
                  "Acquiring live GPS / Network fix..."
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (hasGpsFix) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
              )
            }
            if (!hasLocPerm) {
              TacticalButton(
                onClick = {
                  locPermLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                  )
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("radar_grant_gps_button"),
              ) { Text("Grant GPS", maxLines = 1, softWrap = false) }
            } else {
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (hasGpsFix) {
                  TacticalIconButton(
                    onClick = {
                      val mapsUrl = "https://maps.google.com/?q=${"%.6f".format(telemetry.lat)},${"%.6f".format(telemetry.lng)}"
                      val msg = "$callsign live PEV location: $mapsUrl (±${telemetry.gpsAccuracyM.toInt()}m)"
                      runCatching {
                        ctx.startActivity(
                          Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                              type = "text/plain"
                              putExtra(Intent.EXTRA_SUBJECT, "Live GPS Pin from $callsign")
                              putExtra(Intent.EXTRA_TEXT, msg)
                            },
                            "Share My GPS Pin",
                          )
                        )
                      }
                    },
                    modifier = Modifier.testTag("radar_share_gps_pin_button"),
                  ) {
                    Icon(Icons.Default.Share, contentDescription = "Share My GPS Pin", modifier = Modifier.size(18.dp))
                  }
                }
                TacticalOutlinedButton(
                  onClick = {
                    cockpitVm.refreshGpsFix(settings)
                    userPannedMap = false
                    centeredOnRealGps = false
                    if (hasGpsFix) {
                      mapViewRef?.controller?.animateTo(GeoPoint(telemetry.lat, telemetry.lng), 15.5, 500L)
                    }
                  },
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                  modifier = Modifier.testTag("radar_locate_me_button"),
                ) {
                  Icon(Icons.Default.MyLocation, null, Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Locate Me", maxLines = 1, softWrap = false)
                }
              }
            }
          }
          Spacer(Modifier.height(6.dp))
          val radarPrimaryArgb = MaterialTheme.colorScheme.primary.toArgb()
          val radarSecondaryArgb = MaterialTheme.colorScheme.secondary.toArgb()
          val radarErrorArgb = MaterialTheme.colorScheme.error.toArgb()
          val radarSurfaceArgb = MaterialTheme.colorScheme.surface.toArgb()
          val radarIsDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
          val estRangeMeters = (((activeVehicle?.batteryWh ?: 1500L).toDouble() * (telemetry.batteryPct / 100.0)) / 24.0).coerceIn(1.0, 120.0) * 1000.0

          // Cache RainViewer TilesOverlay once instead of leaking MapTileProviderBasic threads on every recomposition
          val cachedRainViewerOverlay = remember(ctx) {
            val rainViewerSource =
              object : OnlineTileSourceBase(
                "RainViewer",
                1,
                12,
                256,
                ".png",
                arrayOf("https://tilecache.rainviewer.com/v2/radar/nowcast_0/256/"),
              ) {
                override fun getTileURLString(pMapTileIndex: Long): String {
                  val z = MapTileIndex.getZoom(pMapTileIndex)
                  val x = MapTileIndex.getX(pMapTileIndex)
                  val y = MapTileIndex.getY(pMapTileIndex)
                  return "${baseUrl}$z/$x/$y/2/1_1.png"
                }
              }
            val provider = org.osmdroid.tileprovider.MapTileProviderBasic(ctx.applicationContext, rainViewerSource)
            TilesOverlay(provider, ctx.applicationContext)
          }
          var lastRadarOverlaySig by remember { mutableStateOf("") }
          val qRadarLat = (telemetry.lat * 10000.0).toInt()
          val qRadarLng = (telemetry.lng * 10000.0).toInt()
          val radarOverlaySig =
            "$mapLayer|$dopplerOn|$hasGpsFix|$qRadarLat|$qRadarLng|${telemetry.trailPoints.size}|" +
              "${estRangeMeters.toInt()}|${radarLayers.hashCode()}|${geoFenceCenter?.first}|${geoFenceRadiusM}|$geoFenceBreached|" +
              "${rulerAnchor?.first}|${activeNavRoute?.destinationName.orEmpty()}|${liveChargers.size}|${settings.navShowChargersOnMap}|" +
              "${nearbyRiders.size}|${meetups.size}|${hazards.size}|${waypoints.size}|$radarPrimaryArgb|$radarIsDark|${settings.themePreset.id}"

          Box(modifier = Modifier.fillMaxWidth().height(290.dp)) {
            AndroidView(
              factory = { ctxMap ->
                createSafeMapView(ctxMap).also { map ->
                  mapViewRef = map
                  configureSmoothMapPanning(map) { userPannedMap = true }
                  map.setTileSource(resolveMapTileSource(mapLayer))
                  runCatching { map.onResume() }
                }
              },
              update = { map ->
                val desiredTileSource = resolveMapTileSource(mapLayer)
                if (map.tileProvider.tileSource.name() != desiredTileSource.name()) {
                  map.setTileSource(desiredTileSource)
                }
                val desiredOrient = if (radarLayers.trackUpOrientation && hasGpsFix) -telemetry.bearingDeg else 0f
                if (kotlin.math.abs(map.mapOrientation - desiredOrient) > 2f) {
                  map.mapOrientation = desiredOrient
                }

                if (hasGpsFix) {
                  val realPt = GeoPoint(telemetry.lat, telemetry.lng)
                  if ((settings.radarAutoFollow && !userPannedMap) || !centeredOnRealGps) {
                    map.controller.setCenter(realPt)
                    if (!centeredOnRealGps) {
                      map.controller.setZoom(14.8)
                    } else if (settings.radarSpeedZoom) {
                      val targetZoom =
                        when {
                          telemetry.speedKmh > 45.0 -> 12.2
                          telemetry.speedKmh > 25.0 -> 13.6
                          telemetry.speedKmh > 10.0 -> 14.8
                          else -> 15.5
                        }
                      map.controller.setZoom(targetZoom)
                    }
                    centeredOnRealGps = true
                  }
                } else if (!centeredOnFallback && !userPannedMap) {
                  val fallbackPt =
                    when {
                      nearbyRiders.isNotEmpty() -> GeoPoint(nearbyRiders.first().lat, nearbyRiders.first().lng)
                      meetups.isNotEmpty() -> GeoPoint(meetups.first().lat, meetups.first().lng)
                      else -> null
                    }
                  if (fallbackPt != null) {
                    map.controller.setCenter(fallbackPt)
                    map.controller.setZoom(12.0)
                    centeredOnFallback = true
                  }
                }

                if (lastRadarOverlaySig == radarOverlaySig) return@AndroidView
                lastRadarOverlaySig = radarOverlaySig
                applyThemeToMapView(map, settings, radarPrimaryArgb, radarSurfaceArgb, radarIsDark, mapLayer)
                map.overlays.clear()

                if (dopplerOn) {
                  map.overlays.add(cachedRainViewerOverlay)
                }

              // Themed PEV Pack Range Radius Ring
              if (hasGpsFix && radarLayers.showRangeRing) {
                map.overlays.add(
                  buildMapCirclePolygon(
                    map = map,
                    center = GeoPoint(telemetry.lat, telemetry.lng),
                    radiusMeters = estRangeMeters,
                    strokeArgb = radarPrimaryArgb,
                    fillAlpha = 26,
                  )
                )
              }

              // Themed GeoFence Perimeter Ring
              geoFenceCenter?.let { (gLat, gLng) ->
                map.overlays.add(
                  buildMapCirclePolygon(
                    map = map,
                    center = GeoPoint(gLat, gLng),
                    radiusMeters = geoFenceRadiusM.toDouble(),
                    strokeArgb = if (geoFenceBreached) radarErrorArgb else radarSecondaryArgb,
                    fillAlpha = if (geoFenceBreached) 52 else 32,
                  )
                )
              }

              // Themed Live Breadcrumb Trail Polyline
              if (radarLayers.showBreadcrumb && telemetry.trailPoints.size > 1) {
                map.overlays.add(
                  Polyline().apply {
                    outlinePaint.color = radarPrimaryArgb
                    outlinePaint.strokeWidth = 7f
                    setPoints(telemetry.trailPoints.map { GeoPoint(it.first, it.second) })
                  }
                )
              }

              // Active Turn-by-Turn GPS Navigation Polyline & Destination Pin
              activeNavRoute?.let { route ->
                if (route.polylinePoints.size > 1) {
                  map.overlays.add(
                    Polyline().apply {
                      outlinePaint.color = android.graphics.Color.CYAN
                      outlinePaint.strokeWidth = 10f
                      setPoints(route.polylinePoints.map { GeoPoint(it.first, it.second) })
                    }
                  )
                }
                map.overlays.add(
                  Marker(map).apply {
                    position = GeoPoint(route.destLat, route.destLng)
                    title = "DESTINATION: ${route.destinationName}"
                  }
                )
              }

              // Live Charging Stations on Map (OpenChargeMap / Overpass OSM)
              if (settings.navShowChargersOnMap && liveChargers.isNotEmpty()) {
                liveChargers.forEach { st ->
                  map.overlays.add(
                    Marker(map).apply {
                      position = GeoPoint(st.lat, st.lng)
                      title = "CHARGER: ${st.title} (${st.powerKw} • ${st.operatorName})"
                    }
                  )
                }
              }

              if (hasGpsFix) {
                map.overlays.add(Marker(map).apply {
                  position = GeoPoint(telemetry.lat, telemetry.lng)
                  title = "YOU ($callsign)"
                })
              }
              geoFenceCenter?.let { (gLat, gLng) ->
                map.overlays.add(Marker(map).apply {
                  position = GeoPoint(gLat, gLng)
                  title = "GEOFENCE ANCHOR (${geoFenceRadiusM}m)"
                })
              }
              rulerAnchor?.let { (rLat, rLng) ->
                map.overlays.add(Marker(map).apply {
                  position = GeoPoint(rLat, rLng)
                  title = "RULER ANCHOR"
                })
              }
              if (radarLayers.showRiders) {
                nearbyRiders.forEach { r ->
                  map.overlays.add(Marker(map).apply {
                    position = GeoPoint(r.lat, r.lng)
                    title = "${r.callsign} • ${r.statusTag} • ${r.batteryPct}%"
                  })
                }
              }
              if (radarLayers.showMeetups) {
                meetups.forEach { m ->
                  map.overlays.add(Marker(map).apply {
                    position = GeoPoint(m.lat, m.lng)
                    title = "MEETUP: ${m.title}"
                  })
                }
              }
              if (radarLayers.showHazards) {
                hazards.forEach { h ->
                  map.overlays.add(Marker(map).apply {
                    position = GeoPoint(h.lat, h.lng)
                    title = "HAZARD [${h.hazardType}]: ${h.description}"
                  })
                }
              }
              if (radarLayers.showWaypoints) {
                waypoints.forEach { w ->
                  map.overlays.add(Marker(map).apply {
                    position = GeoPoint(w.lat, w.lng)
                    title = "${w.waypointType}: ${w.title}"
                  })
                }
              }
              map.invalidate()
            },
            onRelease = { map ->
              if (mapViewRef === map) mapViewRef = null
              runCatching { map.onPause() }
              runCatching { map.onDetach() }
            },
            modifier = Modifier.fillMaxSize().testTag("radar_live_map_view"),
          )

            // Top-left live Compass Bearing & Elevation pill
            Surface(
              color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                Icon(RuralPevIcons.TrailCompass, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                val elevDisplay = if (settings.distanceUnit == "mi") "${(telemetry.altitudeM * 3.28084).toInt()} ft" else "${telemetry.altitudeM.toInt()} m"
                Text(
                  "HDG ${telemetry.bearingDeg.toInt()}° • $elevDisplay",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                )
              }
            }

            // Right-edge smooth Zoom + / - & Re-Center GPS buttons
            Column(
              modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
              horizontalAlignment = Alignment.End,
            ) {
              if (userPannedMap && hasGpsFix) {
                Surface(
                  onClick = {
                    userPannedMap = false
                    mapViewRef?.controller?.animateTo(GeoPoint(telemetry.lat, telemetry.lng), 15.2, 450L)
                  },
                  color = MaterialTheme.colorScheme.primary,
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier.testTag("radar_recenter_floating_pill"),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Re-Center GPS", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(15.dp))
                    Text("RE-CENTER", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimary)
                  }
                }
              }
              Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                shape = RoundedCornerShape(12.dp),
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  IconButton(
                    onClick = { mapViewRef?.controller?.zoomIn() },
                    modifier = Modifier.size(48.dp).testTag("radar_zoom_in_btn"),
                  ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", modifier = Modifier.size(22.dp))
                  }
                  IconButton(
                    onClick = { mapViewRef?.controller?.zoomOut() },
                    modifier = Modifier.size(48.dp).testTag("radar_zoom_out_btn"),
                  ) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", modifier = Modifier.size(22.dp))
                  }
                }
              }
            }
          }

          // Rural Trail Quick Waypoint Drop Bar + ETA Battery-at-Arrival Pill
          Spacer(Modifier.height(8.dp))
          if (activeNavRoute != null) {
            val arrivalBatt = (telemetry.batteryPct - activeNavRoute!!.estimatedBatteryPctNeeded).coerceIn(0, 100)
            Surface(
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                  Icon(RuralPevIcons.BatteryCharge, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                  Text(
                    "Est. Battery @ Arrival: $arrivalBatt%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (arrivalBatt <= 15) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                  )
                }
                Text(
                  "Climb: +${activeNavRoute!!.elevationGainM.toInt()}m",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.secondary,
                )
              }
            }
            Spacer(Modifier.height(6.dp))
          }
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val ruralPins =
              listOf(
                Triple("GENERAL_STORE", "Country Store", RuralPevIcons.GeneralStore),
                Triple("AIR_PUMP", "Air Pump", RuralPevIcons.AirPumpStop),
                Triple("TRAILHEAD", "Pine Trailhead", RuralPevIcons.PineForest),
                Triple("CREEK_CROSSING", "Creek Ford", RuralPevIcons.CreekWater),
                Triple("SOLAR_SHELTER", "Solar Stop", RuralPevIcons.OffGridSolar),
                Triple("BARN_STOP", "Barn Shop", RuralPevIcons.BarnShop),
              )
            items(ruralPins) { (pinType, pinLabel, pinIcon) ->
              FilterChip(
                selected = false,
                enabled = hasGpsFix,
                onClick = {
                  radarVm.createWaypoint(
                    callsign = callsign,
                    type = pinType,
                    title = pinLabel,
                    notes = "Rural GPS Pin dropped at ${"%.5f".format(telemetry.lat)}, ${"%.5f".format(telemetry.lng)}",
                    lat = telemetry.lat,
                    lng = telemetry.lng,
                  )
                },
                leadingIcon = { Icon(pinIcon, contentDescription = null, modifier = Modifier.size(15.dp)) },
                label = { Text("+ $pinLabel", style = MaterialTheme.typography.labelSmall) },
              )
            }
          }
        }
      }

      // LIVE TURN-BY-TURN GPS NAVIGATION PLANNER & LIVE CHARGER FINDER
      item {
        GpsNavigationPlannerCard(
          hasGpsFix = hasGpsFix,
          myLat = telemetry.lat,
          myLng = telemetry.lng,
          launchLat = telemetry.launchLat,
          launchLng = telemetry.launchLng,
          navSearching = navSearching,
          navRouting = navRouting,
          searchResults = navSearchResults,
          liveChargers = liveChargers,
          chargersLoading = chargersLoading,
          navStatusMsg = navStatusMsg,
          settings = settings,
          onSearchDestinations = { q -> radarVm.searchNavDestinations(q, telemetry.lat, telemetry.lng) },
          onSelectProfile = { prof -> sessionVm.updateSettings { it.copy(navDefaultProfile = prof) } },
          onStartRoute = { dName, dLat, dLng, prof ->
            radarVm.startGpsNavigation(
              startLat = telemetry.lat,
              startLng = telemetry.lng,
              destLat = dLat,
              destLng = dLng,
              destName = dName,
              profile = prof,
              settings = settings,
              vehicleWh = activeVehicle?.batteryWh ?: 1500L,
            )
            mapViewRef?.controller?.animateTo(GeoPoint(dLat, dLng), 13.5, 600L)
          },
          onScanLiveChargers = {
            if (hasGpsFix) radarVm.loadLiveChargingStations(telemetry.lat, telemetry.lng)
          },
          hasActiveRoute = activeNavRoute != null,
          onCancelNav = {
            radarVm.stopGpsNavigation(
              announce = settings.navVoiceGuidance,
              volumePct = settings.globalVolume,
            )
          },
        )
      }

      // 20+ LIVE RADAR PRO CONTROL SUITE (COMPASS, WIND VECTOR, LAYER TOGGLES, CONVOY ANALYTICS, 1-TAP DROPS, DMS COORDS)
      item {
        RadarProControlDeck(
          telemetry = telemetry,
          activeVehicle = activeVehicle,
          settings = settings,
          weather = (weatherState as? UiState.Success)?.data,
          nearbyRiders = nearbyRiders,
          meetups = meetups,
          hazards = hazards,
          waypoints = waypoints,
          layers = radarLayers,
          riderSort = riderSort,
          hasGpsFix = hasGpsFix,
          callsign = callsign,
          onLayersChange = { radarLayers = it },
          onSectorRadiusChange = { miles -> sessionVm.updateSettings { it.copy(radarRadiusMiles = miles) } },
          onRiderSortChange = { riderSort = it },
          onQuickDropHazard = { type, desc ->
            radarVm.createHazard(callsign, type, desc, telemetry.lat, telemetry.lng)
          },
          onQuickDropWaypoint = { type, title ->
            radarVm.createWaypoint(callsign, type, title, "Pinned via 1-tap Radar Hotbar", telemetry.lat, telemetry.lng)
          },
          onFlyToPoint = { lat, lng, _ ->
            sessionVm.updateSettings { it.copy(radarAutoFollow = false) }
            userPannedMap = true
            mapViewRef?.controller?.animateTo(GeoPoint(lat, lng), 15.8, 500L)
          },
        )
      }

      // RURAL BAILOUT RTB & DAYLIGHT SUNSET CUTOFF
      item(key = "rural_bailout_and_daylight_card") {
        RuralBailoutAndDaylightCard(
          telemetry = telemetry,
          activeVehicle = activeVehicle,
          settings = settings,
          weather = (weatherState as? UiState.Success)?.data,
          hasGpsFix = hasGpsFix,
          onRouteHome = { hLat, hLng ->
            radarVm.startGpsNavigation(
              startLat = telemetry.lat,
              startLng = telemetry.lng,
              destLat = hLat,
              destLng = hLng,
              destName = "Launch Base (RTB)",
              profile = settings.navDefaultProfile,
              settings = settings,
              vehicleWh = activeVehicle?.batteryWh ?: 1500L,
            )
            mapViewRef?.controller?.animateTo(GeoPoint(hLat, hLng), 14.2, 500L)
          },
        )
      }

      // RIDER MATCH COMPATIBILITY FILTERS & PROFILE TUNER
      item(key = "rider_match_filter_deck_card") {
        RiderMatchFilterDeck(
          settings = settings,
          totalPilotsCount = radiusFilteredRiders.size,
          matchedPilotsCount = nearbyRiders.size,
          onUpdateMatchSettings = { transform -> sessionVm.updateSettings(transform) },
        )
      }

      // RURAL NETWORK SEEDING (INVITE LOCAL RIDERS & 1-TAP GROUP RIDE HOST)
      item(key = "rural_network_seed_card") {
        RuralNetworkSeedCard(
          callsign = callsign,
          settings = settings,
          activeVehicleName = activeVehicle?.name ?: "PEV Rig",
          usableRangeMi = myUsableRangeMi,
          hasGpsFix = hasGpsFix,
          lat = telemetry.lat,
          lng = telemetry.lng,
          onHostPublicGroupRide = { rTitle, rTime, rDetails ->
            val tagBlock = "[${settings.matchPaceTier.label} • ${settings.matchTerrainPref.label} • Min ${settings.matchMinRangeMi}mi • ${settings.matchScheduleVibe.label}]"
            radarVm.createMeetup(
              callsign = callsign,
              title = rTitle,
              desc = "$tagBlock $rDetails",
              time = rTime,
              lat = telemetry.lat,
              lng = telemetry.lng,
            )
            socialVm.createPost(
              callsign = callsign,
              badge = settings.pilotRankBadge,
              category = "GROUP_RIDES",
              title = "$rTitle ($rTime)",
              body = "$tagBlock\n$rDetails\nDrop into Rider Radar Meetups to RSVP live! #GroupRide #RuralERidesGO",
              bitmap = null,
              pollQuestion = "",
              pollOptions = emptyList(),
              priceUsd = 0.0,
              location = settings.matchHomeZipOrTown.ifBlank { "Live GPS Sector" },
              telemetrySummary = "Staging GPS: ${"%.4f".format(telemetry.lat)}, ${"%.4f".format(telemetry.lng)}",
              helpNeeded = false,
              speedTrap = false,
              stolenAlert = false,
              onDone = {},
            )
            selectedTab = 1
            android.widget.Toast.makeText(ctx, "Published Group Ride Meetup & Board Invite!", android.widget.Toast.LENGTH_SHORT).show()
          },
        )
      }

      // TARGET LOCK & RULER HUD
      if (lockedTarget != null && hasGpsFix) {
        val distKm = RadarViewModel.haversineKm(telemetry.lat, telemetry.lng, lockedTarget.lat, lockedTarget.lng)
        val brg = RadarViewModel.bearingDeg(telemetry.lat, telemetry.lng, lockedTarget.lat, lockedTarget.lng)
        val closingKmh = telemetry.speedKmh.coerceAtLeast(15.0)
        val etaMin = (distKm / closingKmh) * 60.0
        item {
          TacticalCard(highlight = true) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Column(Modifier.weight(1f)) {
                Text("TARGET LOCKED: ${lockedTarget.callsign}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(
                  "Range ${"%.2f".format(formatDistValue(distKm, settings.distanceUnit))} ${distUnitLabel(settings.distanceUnit)} • Bearing ${brg.toInt()}° • ETA ~${etaMin.toInt().coerceAtLeast(1)} min",
                  style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                  "Speed ${"%.1f".format(formatSpeedValue(lockedTarget.speedKmh, settings.distanceUnit))} ${speedUnitLabel(settings.distanceUnit)} • Pack ${lockedTarget.batteryPct}%",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.secondary,
                )
              }
              OutlinedButton(onClick = { radarVm.toggleTargetLock(null) }) { Text("Unlock") }
            }
          }
        }
      }

      if (rulerAnchor != null && hasGpsFix) {
        val dKm = RadarViewModel.haversineKm(rulerAnchor!!.first, rulerAnchor!!.second, telemetry.lat, telemetry.lng)
        item {
          TacticalCard {
            Text(
              "RULER DISTANCE FROM ANCHOR: ${"%.3f".format(formatDistValue(dKm, settings.distanceUnit))} ${distUnitLabel(settings.distanceUnit)}",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.secondary,
            )
          }
        }
      }

      // ELEVATION SPARKLINE
      if (elevation.size >= 2) {
        val totalGainM =
          elevation.zipWithNext { a, b -> (b - a).coerceAtLeast(0.0) }.sum()
        val maxElevM = elevation.maxOrNull() ?: 0.0
        val minElevM = elevation.minOrNull() ?: 0.0
        item {
          TacticalCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text("TRAIL ELEVATION PROFILE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
              Text(
                "+${totalGainM.toInt()}m gain • ${minElevM.toInt()}–${maxElevM.toInt()}m",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
              )
            }
            Spacer(Modifier.height(6.dp))
            val accent = MaterialTheme.colorScheme.primary
            Canvas(Modifier.fillMaxWidth().height(48.dp)) {
              val minE = elevation.minOrNull() ?: 0.0
              val maxE = (elevation.maxOrNull() ?: 1.0).coerceAtLeast(minE + 1.0)
              val path = Path()
              elevation.forEachIndexed { i, e ->
                val x = (i.toFloat() / (elevation.size - 1)) * size.width
                val y = size.height - (((e - minE) / (maxE - minE)).toFloat() * (size.height - 8f) + 4f)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
              }
              drawPath(path, accent, style = Stroke(width = 3f))
            }
          }
        }
      }

      // STATUS TAG & PIN CREATORS
      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(listOf("CRUISING", "FAST_GROUP", "CHARGING", "LOW_BATT", "FLAT_TIRE", "SOS", "ASSEMBLE", "OFFLINE")) { tag ->
            FilterChip(
              selected = statusTag == tag,
              onClick = { statusTag = tag },
              label = { Text(tag.replace("_", " "), maxLines = 1, softWrap = false) },
            )
          }
        }
        Spacer(Modifier.height(6.dp))
        LazyRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          item {
            TacticalOutlinedButton(
              onClick = { showCreateDialog = "MEETUP" },
              enabled = cloudSyncReady && hasGpsFix,
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
              modifier = Modifier.widthIn(min = 108.dp).testTag("radar_add_meetup_button"),
            ) {
              Text("+ Meetup", maxLines = 1, softWrap = false)
            }
          }
          item {
            TacticalOutlinedButton(
              onClick = { showCreateDialog = "HAZARD" },
              enabled = cloudSyncReady && hasGpsFix,
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
              modifier = Modifier.widthIn(min = 108.dp).testTag("radar_add_hazard_button"),
            ) {
              Text("+ Hazard", maxLines = 1, softWrap = false)
            }
          }
          item {
            TacticalOutlinedButton(
              onClick = { showCreateDialog = "WAYPOINT" },
              enabled = cloudSyncReady && hasGpsFix,
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
              modifier = Modifier.widthIn(min = 116.dp).testTag("radar_add_waypoint_button"),
            ) {
              Text("+ Waypoint", maxLines = 1, softWrap = false)
            }
          }
        }
      }

      // RURAL HAZARD DISPATCH (30 PRESETS) & OFFLINE BAILOUT CARD
      item(key = "radar_rural_trail_intel_deck") {
        RadarRuralTrailIntelDeck(
          telemetry = telemetry,
          hasGpsFix = hasGpsFix,
          onDispatchHazard = { preset ->
            if (cloudSyncReady && hasGpsFix) {
              radarVm.createHazard(callsign, preset.firestoreType, "${preset.label}: ${preset.description}", telemetry.lat, telemetry.lng)
              android.widget.Toast.makeText(ctx, "Dispatched live pin: ${preset.label}", android.widget.Toast.LENGTH_SHORT).show()
            } else {
              android.widget.Toast.makeText(ctx, "Requires cloud sign-in & live GPS fix to broadcast pin.", android.widget.Toast.LENGTH_SHORT).show()
            }
          },
        )
      }

      // RADAR LIST TABS
      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          val tabs = listOf("Pilots (${nearbyRiders.size})", "Meetups (${meetups.size})", "Hazards (${hazards.size})", "Waypoints (${waypoints.size})")
          items(tabs.size) { idx ->
            FilterChip(
              selected = selectedTab == idx,
              onClick = { selectedTab = idx },
              label = { Text(tabs[idx], maxLines = 1, softWrap = false) },
            )
          }
        }
      }

      when (selectedTab) {
        0 -> {
          if (nearbyRidersWithMatch.isEmpty()) {
            item {
              TacticalCard {
                Text(
                  "No riders match your current Radar & Rider Match filters. Adjust filters above or tap Invite Riders to seed your area.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
          items(nearbyRidersWithMatch, key = { it.first.userId }) { (r, peerSpec, matchScore) ->
            val distInfo =
              if (hasGpsFix) {
                val dKm = RadarViewModel.haversineKm(telemetry.lat, telemetry.lng, r.lat, r.lng)
                val brg = RadarViewModel.bearingDeg(telemetry.lat, telemetry.lng, r.lat, r.lng).toInt()
                " • ${"%.1f".format(formatDistValue(dKm, settings.distanceUnit))}${distUnitLabel(settings.distanceUnit)} @ ${brg}°"
              } else ""
            val spdInfo = "${"%.0f".format(formatSpeedValue(r.speedKmh, settings.distanceUnit))} ${speedUnitLabel(settings.distanceUnit)}"
            TacticalCard(highlight = lockedUid == r.userId || r.statusTag == "SOS") {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      Text(
                        "${r.callsign} [${r.statusTag}]",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (r.statusTag == "SOS") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      if (r.userId != myUid) {
                        Surface(
                          color =
                            if (matchScore.scorePct >= 75) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                          shape = RoundedCornerShape(8.dp),
                          border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        ) {
                          Text(
                            "Match ${matchScore.scorePct}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                          )
                        }
                      }
                    }
                    Text("${peerSpec.displayRigName} • $spdInfo • BAT ${r.batteryPct}%$distInfo", style = MaterialTheme.typography.labelSmall)
                    Text(
                      "${peerSpec.paceTier.label} • ${peerSpec.terrainPref.label} • ~${matchScore.estimatedLiveRangeMi}mi left • ${peerSpec.scheduleVibe.label}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.secondary,
                    )
                    if (r.userId != myUid && matchScore.warningText.isNotBlank()) {
                      Text(matchScore.warningText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                  }
                }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  if (cloudSyncReady && r.userId != myUid && r.userId.isNotBlank()) {
                    TacticalIconButton(
                      onClick = { dmPeer = PublicPilotProfile(userId = r.userId, callsign = r.callsign, vehicleSummary = r.vehicleName) },
                      modifier = Modifier.testTag("radar_dm_${r.userId}"),
                    ) {
                      Icon(Icons.Default.Mail, contentDescription = "DM ${r.callsign}", modifier = Modifier.size(18.dp))
                    }
                    TacticalIconButton(
                      onClick = { socialVm.startOutgoingCall(myUid, callsign, r.userId, r.callsign, "AUDIO") },
                      modifier = Modifier.testTag("radar_audio_call_${r.userId}"),
                    ) {
                      Icon(Icons.Default.Call, contentDescription = "Audio Call ${r.callsign}", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    TacticalIconButton(
                      onClick = { socialVm.startOutgoingCall(myUid, callsign, r.userId, r.callsign, "VIDEO") },
                      modifier = Modifier.testTag("radar_video_call_${r.userId}"),
                    ) {
                      Icon(Icons.Default.Videocam, contentDescription = "Video Call ${r.callsign}", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    }
                  }
                  TacticalIconButton(
                    onClick = {
                      userPannedMap = true
                      sessionVm.updateSettings { it.copy(radarAutoFollow = false) }
                      mapViewRef?.controller?.animateTo(GeoPoint(r.lat, r.lng), 15.8, 500L)
                    },
                  ) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = "Center Map on ${r.callsign}", modifier = Modifier.size(18.dp))
                  }
                  Spacer(Modifier.weight(1f))
                  TacticalOutlinedButton(onClick = { radarVm.toggleTargetLock(r.userId) }) {
                    Icon(Icons.Default.MyLocation, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (lockedUid == r.userId) "Unlock" else "Lock", maxLines = 1, softWrap = false)
                  }
                }
              }
            }
          }
        }
        1 -> items(meetups, key = { it.id }) { m ->
          val distInfo =
            if (hasGpsFix) {
              val dKm = RadarViewModel.haversineKm(telemetry.lat, telemetry.lng, m.lat, m.lng)
              val brg = RadarViewModel.bearingDeg(telemetry.lat, telemetry.lng, m.lat, m.lng).toInt()
              " • ${"%.1f".format(formatDistValue(dKm, settings.distanceUnit))}${distUnitLabel(settings.distanceUnit)} (${brg}°)"
            } else ""
          TacticalCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                  Text(m.title, style = MaterialTheme.typography.titleMedium)
                  Text("${m.creatorCallsign} • ${m.scheduledTime} • ${m.attendeeCount} going$distInfo", style = MaterialTheme.typography.labelSmall)
                  if (m.description.isNotBlank()) Text(m.description, style = MaterialTheme.typography.bodyMedium)
                }
                TacticalButton(onClick = { radarVm.rsvpMeetup(m.id) }) {
                  Icon(Icons.Default.Group, null, Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("RSVP", maxLines = 1, softWrap = false)
                }
              }
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TacticalOutlinedButton(
                  onClick = {
                    userPannedMap = true
                    sessionVm.updateSettings { it.copy(radarAutoFollow = false) }
                    mapViewRef?.controller?.animateTo(GeoPoint(m.lat, m.lng), 15.6, 500L)
                  },
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                ) {
                  Icon(Icons.Default.CenterFocusStrong, null, Modifier.size(15.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Center Map", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                }
                TacticalOutlinedButton(
                  onClick = {
                    if (hasGpsFix) {
                      radarVm.startGpsNavigation(
                        startLat = telemetry.lat,
                        startLng = telemetry.lng,
                        destLat = m.lat,
                        destLng = m.lng,
                        destName = m.title,
                        profile = settings.navDefaultProfile,
                        settings = settings,
                        vehicleWh = activeVehicle?.batteryWh ?: 1500L,
                      )
                      mapViewRef?.controller?.animateTo(GeoPoint(m.lat, m.lng), 14.5, 500L)
                    } else {
                      android.widget.Toast.makeText(ctx, "Waiting for GPS lock to calculate route.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                  },
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                ) {
                  Icon(Icons.Default.Navigation, null, Modifier.size(15.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Route Here", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                }
                TacticalOutlinedButton(
                  onClick = {
                    val shareTxt =
                      "Group Ride Meetup: ${m.title} (${m.scheduledTime})\nHost: @${m.creatorCallsign}\n${m.description}\nStaging Pin: https://maps.google.com/?q=${"%.5f".format(m.lat)},${"%.5f".format(m.lng)}"
                    RiderMatchEngine.launchShareInvite(ctx, "PEV Group Ride: ${m.title}", shareTxt)
                  },
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                ) {
                  Icon(Icons.Default.Share, null, Modifier.size(15.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Share", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                }
              }
            }
          }
        }
        2 -> {
          item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(listOf("ALL", "POTHOLE", "GRAVEL", "POLICE", "CLOSURE", "DOG", "FLOOD")) { ht ->
                FilterChip(
                  selected = hazardTypeFilter == ht,
                  onClick = { hazardTypeFilter = ht },
                  label = { Text(ht, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
                )
              }
            }
          }
          val shownHazards = hazards.filter { hazardTypeFilter == "ALL" || it.hazardType.equals(hazardTypeFilter, true) }
          items(shownHazards, key = { it.id }) { h ->
            val distInfo =
              if (hasGpsFix) {
                val dKm = RadarViewModel.haversineKm(telemetry.lat, telemetry.lng, h.lat, h.lng)
                val brg = RadarViewModel.bearingDeg(telemetry.lat, telemetry.lng, h.lat, h.lng).toInt()
                " • ${"%.1f".format(formatDistValue(dKm, settings.distanceUnit))}${distUnitLabel(settings.distanceUnit)} (${brg}°)"
              } else ""
            TacticalCard {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Column(Modifier.weight(1f)) {
                    Text("${h.hazardType} • ${h.creatorCallsign}$distInfo", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    Text(h.description, style = MaterialTheme.typography.bodyMedium)
                  }
                  TacticalOutlinedButton(onClick = { radarVm.upvoteHazard(h.id) }) {
                    Icon(Icons.Default.ThumbUp, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${h.upvotes}", maxLines = 1, softWrap = false)
                  }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  TacticalOutlinedButton(
                    onClick = {
                      userPannedMap = true
                      sessionVm.updateSettings { it.copy(radarAutoFollow = false) }
                      mapViewRef?.controller?.animateTo(GeoPoint(h.lat, h.lng), 16.0, 500L)
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  ) {
                    Icon(Icons.Default.CenterFocusStrong, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Center Map", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                  }
                  TacticalOutlinedButton(
                    onClick = {
                      val shareTxt = "PEV Hazard Alert [${h.hazardType}] by @${h.creatorCallsign}: ${h.description}\nLocation: https://maps.google.com/?q=${"%.5f".format(h.lat)},${"%.5f".format(h.lng)}"
                      RiderMatchEngine.launchShareInvite(ctx, "Trail Hazard Alert", shareTxt)
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  ) {
                    Icon(Icons.Default.Share, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                  }
                }
              }
            }
          }
        }
        else -> {
          item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(listOf("ALL", "CHARGER", "REPAIR", "WATER", "SCENIC", "MEETSPOT")) { wt ->
                FilterChip(
                  selected = waypointTypeFilter == wt,
                  onClick = { waypointTypeFilter = wt },
                  label = { Text(wt, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
                )
              }
            }
          }
          val shownWaypoints = waypoints.filter { waypointTypeFilter == "ALL" || it.waypointType.equals(waypointTypeFilter, true) }
          items(shownWaypoints, key = { it.id }) { w ->
            val distInfo =
              if (hasGpsFix) {
                val dKm = RadarViewModel.haversineKm(telemetry.lat, telemetry.lng, w.lat, w.lng)
                val brg = RadarViewModel.bearingDeg(telemetry.lat, telemetry.lng, w.lat, w.lng).toInt()
                " • ${"%.1f".format(formatDistValue(dKm, settings.distanceUnit))}${distUnitLabel(settings.distanceUnit)} (${brg}°)"
              } else ""
            TacticalCard {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.AddLocationAlt, null, tint = MaterialTheme.colorScheme.primary)
                  Spacer(Modifier.width(8.dp))
                  Column {
                    Text("${w.waypointType}: ${w.title}", style = MaterialTheme.typography.titleMedium)
                    Text("Added by ${w.creatorCallsign}$distInfo • ${w.notes}", style = MaterialTheme.typography.bodyMedium)
                  }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  TacticalOutlinedButton(
                    onClick = {
                      userPannedMap = true
                      sessionVm.updateSettings { it.copy(radarAutoFollow = false) }
                      mapViewRef?.controller?.animateTo(GeoPoint(w.lat, w.lng), 15.8, 500L)
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  ) {
                    Icon(Icons.Default.CenterFocusStrong, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Center Map", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                  }
                  TacticalOutlinedButton(
                    onClick = {
                      if (hasGpsFix) {
                        radarVm.startGpsNavigation(
                          startLat = telemetry.lat,
                          startLng = telemetry.lng,
                          destLat = w.lat,
                          destLng = w.lng,
                          destName = w.title,
                          profile = settings.navDefaultProfile,
                          settings = settings,
                          vehicleWh = activeVehicle?.batteryWh ?: 1500L,
                        )
                        mapViewRef?.controller?.animateTo(GeoPoint(w.lat, w.lng), 14.5, 500L)
                      } else {
                        android.widget.Toast.makeText(ctx, "Waiting for GPS lock to calculate route.", android.widget.Toast.LENGTH_SHORT).show()
                      }
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  ) {
                    Icon(Icons.Default.Navigation, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Route Here", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                  }
                  TacticalOutlinedButton(
                    onClick = {
                      val shareTxt = "PEV Waypoint [${w.waypointType}]: ${w.title}\n${w.notes}\nLocation: https://maps.google.com/?q=${"%.5f".format(w.lat)},${"%.5f".format(w.lng)}"
                      RiderMatchEngine.launchShareInvite(ctx, "PEV Waypoint: ${w.title}", shareTxt)
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  ) {
                    Icon(Icons.Default.Share, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                  }
                }
              }
            }
          }
        }
      }
    }

    FloatingActionButton(
      onClick = { showWalkie = true },
      modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).testTag("open_walkie_fab"),
    ) {
      Icon(Icons.Default.Radio, contentDescription = "Open Walkie-Talkie")
    }

    if (showWalkie) {
      WalkieTalkieSheet(
        sessionVm = sessionVm,
        radarVm = radarVm,
        onClose = { showWalkie = false },
      )
    }

    dmPeer?.let { peer ->
      DirectMessagesSheet(
        sessionVm = sessionVm,
        socialVm = socialVm,
        initialPeer = peer,
        telemetryLat = telemetry.lat,
        telemetryLng = telemetry.lng,
        onDismiss = { dmPeer = null },
      )
    }

    showCreateDialog?.let { mode ->
      RadarPinCreateDialog(
        mode = mode,
        onDismiss = { showCreateDialog = null },
        onSubmit = { typeOrTitle, sub, desc ->
          if (hasGpsFix) {
            when (mode) {
              "MEETUP" -> radarVm.createMeetup(callsign, typeOrTitle, desc, sub.ifBlank { "Tonight 19:00" }, telemetry.lat, telemetry.lng)
              "HAZARD" -> radarVm.createHazard(callsign, typeOrTitle, desc, telemetry.lat, telemetry.lng)
              "WAYPOINT" -> radarVm.createWaypoint(callsign, typeOrTitle, sub, desc, telemetry.lat, telemetry.lng)
            }
          }
          showCreateDialog = null
        },
      )
    }
  }
}

@Composable
private fun RadarPinCreateDialog(
  mode: String,
  onDismiss: () -> Unit,
  onSubmit: (primary: String, secondary: String, desc: String) -> Unit,
) {
  var field1 by remember { mutableStateOf(if (mode == "HAZARD") "POTHOLE" else if (mode == "WAYPOINT") "CHARGER" else "") }
  var field2 by remember { mutableStateOf("") }
  var desc by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Drop Live $mode Pin") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (mode == "HAZARD") {
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf("POTHOLE", "GRAVEL", "POLICE", "CLOSURE", "DOG", "FLOOD")) { t ->
              FilterChip(selected = field1 == t, onClick = { field1 = t }, label = { Text(t) })
            }
          }
        } else if (mode == "WAYPOINT") {
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf("CHARGER", "REPAIR", "WATER", "SCENIC", "MEETSPOT")) { t ->
              FilterChip(selected = field1 == t, onClick = { field1 = t }, label = { Text(t) })
            }
          }
          OutlinedTextField(value = field2, onValueChange = { field2 = it }, label = { Text("Waypoint Title") })
        } else {
          OutlinedTextField(value = field1, onValueChange = { field1 = it }, label = { Text("Meetup Title") })
          OutlinedTextField(value = field2, onValueChange = { field2 = it }, label = { Text("Scheduled Time") })
        }
        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Details / Notes") })
      }
    },
    confirmButton = {
      Button(onClick = { onSubmit(field1, field2, desc) }) { Text("Broadcast Pin") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
