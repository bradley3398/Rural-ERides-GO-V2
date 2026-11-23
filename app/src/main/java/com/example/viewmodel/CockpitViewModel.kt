package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.BatteryPredictionResult
import com.example.ai.GeminiAiRepository
import com.example.ai.GeminiModelCatalog
import com.example.ai.PevAutoSpecResult
import com.example.core.LiveCloudServices
import com.example.core.PevBatteryPhysics
import com.example.data.AppDatabase
import com.example.data.LiveTelemetrySnapshot
import com.example.data.LocalRiderVault
import com.example.data.MaintenanceTask
import com.example.data.MaintenanceWearState
import com.example.data.MasterSettings
import com.example.data.OfflineRideEntity
import com.example.data.PevVehicle
import com.example.data.RideSession
import com.example.data.RiderRepository
import com.example.data.ServiceLogRecord
import com.example.hardware.TelemetryEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

private const val TAG = "CockpitViewModel"

class CockpitViewModel(application: Application) : AndroidViewModel(application) {

  private val ctx = application.applicationContext
  private val riderRepo = RiderRepository(ctx)
  private val localVault = LocalRiderVault(ctx)
  private val aiRepo = GeminiAiRepository()
  private val rideDao = AppDatabase.get(ctx).offlineRideDao()

  val telemetryEngine = TelemetryEngine(ctx)

  val telemetry: StateFlow<LiveTelemetrySnapshot> = telemetryEngine.telemetry

  private val _vehicles = MutableStateFlow<List<PevVehicle>>(emptyList())
  val vehicles: StateFlow<List<PevVehicle>> = _vehicles.asStateFlow()

  private val _rides = MutableStateFlow<List<RideSession>>(emptyList())
  val rides: StateFlow<List<RideSession>> = _rides.asStateFlow()

  private val _offlineRides = MutableStateFlow<List<OfflineRideEntity>>(emptyList())
  val offlineRides: StateFlow<List<OfflineRideEntity>> = _offlineRides.asStateFlow()

  private val _maintenance = MutableStateFlow<List<MaintenanceTask>>(emptyList())
  val maintenance: StateFlow<List<MaintenanceTask>> = _maintenance.asStateFlow()

  private val _wearStates = MutableStateFlow<List<MaintenanceWearState>>(emptyList())
  val wearStates: StateFlow<List<MaintenanceWearState>> = _wearStates.asStateFlow()

  private val _serviceLogs = MutableStateFlow<List<ServiceLogRecord>>(emptyList())
  val serviceLogs: StateFlow<List<ServiceLogRecord>> = _serviceLogs.asStateFlow()

  private val _preflightChecked = MutableStateFlow<Set<String>>(emptySet())
  val preflightChecked: StateFlow<Set<String>> = _preflightChecked.asStateFlow()

  private val _trailKitChecked = MutableStateFlow<Set<String>>(emptySet())
  val trailKitChecked: StateFlow<Set<String>> = _trailKitChecked.asStateFlow()

  private val _statusBanner = MutableStateFlow("")
  val statusBanner: StateFlow<String> = _statusBanner.asStateFlow()

  private val _debriefBusyId = MutableStateFlow<String?>(null)
  val debriefBusyId: StateFlow<String?> = _debriefBusyId.asStateFlow()

  private val _fleetDebrief = MutableStateFlow("")
  val fleetDebrief: StateFlow<String> = _fleetDebrief.asStateFlow()

  private val _fleetDebriefBusy = MutableStateFlow(false)
  val fleetDebriefBusy: StateFlow<Boolean> = _fleetDebriefBusy.asStateFlow()

  private val _aiSafetyBrief = MutableStateFlow("")
  val aiSafetyBrief: StateFlow<String> = _aiSafetyBrief.asStateFlow()

  private val _aiSafetyBusy = MutableStateFlow(false)
  val aiSafetyBusy: StateFlow<Boolean> = _aiSafetyBusy.asStateFlow()

  private val _activeRideStartBatteryPct = MutableStateFlow(100)
  val activeRideStartBatteryPct: StateFlow<Int> = _activeRideStartBatteryPct.asStateFlow()

  private var collectorsJob: Job? = null
  private var boundKey: String? = null
  private var boundFirebaseAuth: Boolean = false

  private fun defaultWearList(): List<MaintenanceWearState> =
    listOf(
      MaintenanceWearState("front_tire", "Front Tire Tread", 2500.0, 0.0),
      MaintenanceWearState("rear_tire", "Rear Tire Tread", 2000.0, 0.0),
      MaintenanceWearState("brake_pads", "Hydraulic / Disc Brake Pads", 1200.0, 0.0),
      MaintenanceWearState("chain_belt", "Drive Belt / Chain / Bearings", 3000.0, 0.0),
      MaintenanceWearState("suspension_seals", "Fork & Shock Seals", 4000.0, 0.0),
    )

  fun bindUser(uid: String?, firebaseAuthenticated: Boolean = true) {
    val effectiveUid = uid?.ifBlank { "local_pilot" } ?: "local_pilot"
    val effectiveFb = firebaseAuthenticated && !uid.isNullOrBlank()
    val nextKey = "$effectiveUid:$effectiveFb"
    if (nextKey == boundKey) return
    boundKey = nextKey
    boundFirebaseAuth = effectiveFb
    collectorsJob?.cancel()
    collectorsJob =
      viewModelScope.launch {
        launch {
          val loadedWear = localVault.loadWearStates(effectiveUid).ifEmpty { defaultWearList() }
          _wearStates.value = loadedWear
          _serviceLogs.value = localVault.loadServiceLogs(effectiveUid)
          val allPreflight = localVault.loadPreflightChecklist(effectiveUid)
          _preflightChecked.value = allPreflight.filterNot { it.startsWith("kit_") }.toSet()
          _trailKitChecked.value = allPreflight.filter { it.startsWith("kit_") }.toSet()
        }
        launch {
          rideDao.observeRides(effectiveUid).collect { offlineList ->
            _offlineRides.value = offlineList
            if (!effectiveFb) {
              _rides.value =
                offlineList.map { r ->
                  RideSession(
                    id = r.id,
                    userId = r.userId,
                    vehicleName = r.vehicleName,
                    title = r.title,
                    durationSec = r.durationSec,
                    distanceKm = r.distanceKm,
                    maxSpeedKmh = r.maxSpeedKmh,
                    avgSpeedKmh = r.avgSpeedKmh,
                    elevationGainM = r.elevationGainM,
                    whUsed = r.whUsed,
                    polylinePoints =
                      r.polylineCsv.split(";").filter { it.isNotBlank() },
                  )
                }
            }
          }
        }
        if (effectiveFb) {
          launch {
            riderRepo
              .observeVehicles(effectiveUid)
              .catch { e ->
                if (e is CancellationException) throw e
                Log.w(TAG, "Vehicles stream error: ${e.message}")
              }
              .collect { _vehicles.value = it }
          }
          launch {
            riderRepo
              .observeRides(effectiveUid)
              .catch { e ->
                if (e is CancellationException) throw e
                Log.w(TAG, "Rides stream error: ${e.message}")
              }
              .collect { _rides.value = it }
          }
          launch {
            riderRepo
              .observeMaintenance(effectiveUid)
              .catch { e ->
                if (e is CancellationException) throw e
                Log.w(TAG, "Maintenance stream error: ${e.message}")
              }
              .collect { _maintenance.value = it }
          }
        } else {
          _vehicles.value = localVault.loadVehicles(effectiveUid)
          _maintenance.value = localVault.loadTasks(effectiveUid)
        }
      }
  }

  fun applyTelemetryConfig(
    settings: MasterSettings,
    activeVehicle: PevVehicle?,
    riderWeightLbs: Double = settings.riderWeightLbs.toDouble(),
  ) {
    telemetryEngine.updateConfig(settings.gpsAccuracyLimitM, settings.crashSensitivityG)
    val snap = telemetry.value
    val nomV = activeVehicle?.voltage?.takeIf { it >= 12.0 } ?: 48.0
    val chemistry = activeVehicle?.batteryChemistry?.ifBlank { settings.batteryChemistry } ?: settings.batteryChemistry
    val packWh =
      when {
        activeVehicle != null && activeVehicle.batteryWh >= 180L -> activeVehicle.batteryWh.toDouble()
        activeVehicle != null && activeVehicle.batteryAh > 1.0 ->
          PevBatteryPhysics.computePackWh(nomV, activeVehicle.batteryAh, chemistry)
        else -> 960.0
      }
    val hudPack =
      PevBatteryPhysics.computeLiveHudPackState(
        ratedVoltage = nomV,
        packWh = packWh,
        chemistry = chemistry,
        startBatteryPct = _activeRideStartBatteryPct.value,
        tripDistanceKm = snap.tripDistanceKm,
        elevationGainM = snap.elevationGainM,
        speedKmh = snap.speedKmh,
        avgSpeedKmh = snap.avgSpeedKmh,
        pitchDeg = snap.pitchDeg.toDouble(),
        riderWeightLbs = riderWeightLbs,
        vehicleWeightLbs = activeVehicle?.vehicleWeightLbs ?: 58.0,
        rideMode = settings.rideMode,
        ambientTempC = snap.ambientTempC?.toDouble() ?: 22.0,
        motorWatts = activeVehicle?.motorWatts ?: 0,
      )
    telemetryEngine.updateBatteryEstimate(hudPack.socPctInt, hudPack.underLoadVoltageV)
  }

  fun ahrsUiThrottleMs(damping: String): Long =
    when (damping) {
      "Snappy" -> 50L
      "Ultra Smooth" -> 120L
      else -> 80L
    }

  fun startHardwareFeeds(settings: MasterSettings) {
    telemetryEngine.startSensors(settings.imuPollingRateHz, ahrsUiThrottleMs(settings.ahrsGaugeDamping))
    val interval = (1000L / settings.telemetryRateHz.coerceIn(1, 5))
    telemetryEngine.startLocationUpdates(interval)
  }

  fun startSensorsOnly(settings: MasterSettings) {
    telemetryEngine.startSensors(settings.imuPollingRateHz, ahrsUiThrottleMs(settings.ahrsGaugeDamping))
  }

  fun refreshGpsFix(settings: MasterSettings) {
    startHardwareFeeds(settings)
    telemetryEngine.refreshLocationNow()
  }

  fun calibrateZeroPitchRoll() {
    telemetryEngine.calibrateLevelZero()
    _statusBanner.value = "9-DOF AHRS pitch & roll horizon zeroed."
  }

  fun calibrateAhrsAllAxes() {
    telemetryEngine.calibrateAhrsAllAxes()
    _statusBanner.value = "9-DOF AHRS Pitch, Roll & Yaw zeroed."
  }

  fun resetPeakLeanAngles() {
    telemetryEngine.resetPeakLeanAngles()
    _statusBanner.value = "Peak lean markers cleared."
  }

  fun startRide(
    startBatteryPct: Int = 100,
    activeVehicle: PevVehicle? = null,
    settings: MasterSettings = MasterSettings(),
  ) {
    val cleanStart = startBatteryPct.coerceIn(0, 100)
    _activeRideStartBatteryPct.value = cleanStart
    telemetryEngine.startRideRecording()
    val voltage = activeVehicle?.voltage?.takeIf { it >= 12.0 } ?: 48.0
    val chem = activeVehicle?.batteryChemistry?.ifBlank { settings.batteryChemistry } ?: settings.batteryChemistry
    val exactStartV = PevBatteryPhysics.voltageForSoc(voltage, cleanStart.toDouble(), chem)
    telemetryEngine.updateBatteryEstimate(cleanStart, exactStartV)
    _statusBanner.value = "Trailhead Ride Recording Active ($cleanStart% • ${"%.1f".format(exactStartV)}V)"
  }

  fun stopAndSaveRide(
    uid: String?,
    activeVehicle: PevVehicle?,
    title: String,
    weatherSummary: String,
    autoCloudSync: Boolean,
    startBatteryPct: Int = _activeRideStartBatteryPct.value,
    endBatteryPct: Int = telemetry.value.batteryPct,
    batteryChemistry: String = "NMC",
  ) {
    val effectiveUid = uid?.ifBlank { "local_pilot" } ?: "local_pilot"
    val snap = telemetryEngine.stopRideRecording()
    val vehName = activeVehicle?.name?.ifBlank { "PEV Rig" } ?: "PEV Rig"
    val cleanStart = startBatteryPct.coerceIn(0, 100)
    val cleanEnd = endBatteryPct.coerceIn(0, 100)
    val chem = activeVehicle?.batteryChemistry?.ifBlank { batteryChemistry } ?: batteryChemistry
    val packWh = (activeVehicle?.batteryWh ?: 960L).toDouble().coerceAtLeast(180.0)
    val estWh =
      if (cleanStart > cleanEnd) packWh * ((cleanStart - cleanEnd) / 100.0)
      else (snap.tripDistanceKm * 22.0)
    val rid = "ride_${UUID.randomUUID().toString().take(8)}"
    val polyStrings = snap.trailPoints.map { "${"%.5f".format(it.first)},${"%.5f".format(it.second)}" }
    _activeRideStartBatteryPct.value = cleanEnd
    val endVolts = PevBatteryPhysics.voltageForSoc(activeVehicle?.voltage?.takeIf { it >= 12.0 } ?: 48.0, cleanEnd.toDouble(), chem)
    telemetryEngine.updateBatteryEstimate(cleanEnd, endVolts)

    viewModelScope.launch {
      run {
        val synced = autoCloudSync && boundFirebaseAuth && effectiveUid != "local_pilot"
        rideDao.upsert(
          OfflineRideEntity(
            id = rid,
            userId = effectiveUid,
            vehicleName = vehName,
            title = title.ifBlank { "Backroad Loop" },
            durationSec = snap.elapsedSec.coerceAtLeast(1L),
            distanceKm = snap.tripDistanceKm,
            maxSpeedKmh = snap.maxSpeedKmh,
            avgSpeedKmh = snap.avgSpeedKmh,
            elevationGainM = snap.elevationGainM,
            whUsed = estWh,
            polylineCsv = polyStrings.joinToString(";"),
            syncedToCloud = synced,
            createdAtMs = System.currentTimeMillis(),
          )
        )
        if (synced) {
          val rideObj =
            RideSession(
              id = rid,
              userId = effectiveUid,
              vehicleId = activeVehicle?.id.orEmpty(),
              vehicleName = vehName,
              title = title.ifBlank { "Backroad Loop" },
              durationSec = snap.elapsedSec.coerceAtLeast(1L),
              distanceKm = snap.tripDistanceKm,
              maxSpeedKmh = snap.maxSpeedKmh,
              avgSpeedKmh = snap.avgSpeedKmh,
              elevationGainM = snap.elevationGainM,
              whUsed = estWh,
              startBatteryPct = cleanStart,
              endBatteryPct = cleanEnd,
              weatherSummary = weatherSummary,
              polylinePoints = polyStrings,
            )
          riderRepo.saveRideSession(effectiveUid, rideObj)
        }
      }
      val usedPct = (cleanStart - cleanEnd).coerceAtLeast(0)
      _statusBanner.value =
        "Ride saved (${"%.2f".format(snap.tripDistanceKm * 0.621371)} mi • $cleanStart% → $cleanEnd% / ${"%.1f".format(endVolts)}V, -${estWh.toInt()} Wh)."
    }
  }

  fun lookupPevSpecs(
    makeAndModel: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_5_FLASH_LITE,
    onResult: (PevAutoSpecResult) -> Unit,
    onError: (String) -> Unit,
  ) {
    viewModelScope.launch {
      aiRepo
        .autoLookupVehicleSpecs(makeAndModel, modelId)
        .onSuccess(onResult)
        .onFailure { onError(it.message ?: "Unable to auto-fetch specs.") }
    }
  }

  fun searchPevModelCandidates(
    query: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_5_FLASH_LITE,
    onResult: (List<PevAutoSpecResult>) -> Unit,
    onError: (String) -> Unit,
  ) {
    viewModelScope.launch {
      aiRepo
        .searchPevModelCandidates(query, modelId)
        .onSuccess(onResult)
        .onFailure { onError(it.message ?: "Deep model search failed.") }
    }
  }

  fun fetchPevPhotoBase64(
    makeAndModel: String,
    onResult: (String) -> Unit,
  ) {
    viewModelScope.launch {
      val b64 = LiveCloudServices.fetchPevProductImageBase64(makeAndModel)
      onResult(b64)
    }
  }

  fun predictEndRideBattery(
    activeVehicle: PevVehicle?,
    startBatteryPct: Int,
    riderWeightLbs: Double,
    modelId: String = GeminiModelCatalog.GEMINI_3_5_FLASH_LITE,
    onResult: (BatteryPredictionResult) -> Unit,
  ) {
    val snap = telemetry.value
    val packWh = (activeVehicle?.batteryWh ?: 1200L).toDouble().coerceAtLeast(200.0)
    val allRides = _rides.value
    val matchingRides =
      allRides
        .filter { r ->
          r.distanceKm >= 0.25 &&
            (activeVehicle == null || r.vehicleId == activeVehicle.id || r.vehicleName.equals(activeVehicle.name, ignoreCase = true))
        }
        .ifEmpty { allRides.filter { it.distanceKm >= 0.25 } }

    // 1. Compute immediate learned Wh/km baseline from past ride records
    val learnedRates =
      matchingRides.mapNotNull { r ->
        val usedWh =
          when {
            r.startBatteryPct >= 0 && r.endBatteryPct >= 0 && r.startBatteryPct > r.endBatteryPct ->
              packWh * ((r.startBatteryPct - r.endBatteryPct) / 100.0)
            r.whUsed > 1.0 -> r.whUsed
            else -> null
          }
        usedWh?.let { (it / r.distanceKm.coerceAtLeast(0.25)).coerceIn(5.0, 150.0) }
      }
    val weightScale = if (riderWeightLbs > 60.0) (0.78 + (riderWeightLbs / 175.0) * 0.22).coerceIn(0.8, 1.5) else 1.0
    val baseWhPerKm =
      if (learnedRates.isNotEmpty()) learnedRates.average()
      else (22.0 * weightScale)
    val elevExtraWh = snap.elevationGainM * 0.14 * weightScale
    val estWhUsed = (snap.tripDistanceKm * baseWhPerKm) + elevExtraWh
    val localPredictedPct =
      (startBatteryPct - ((estWhUsed / packWh) * 100.0)).toInt().coerceIn(0, startBatteryPct.coerceIn(0, 100))

    val localFallback =
      BatteryPredictionResult(
        predictedRemainingPct = localPredictedPct,
        learnedWhPerKm = baseWhPerKm,
        confidence = if (learnedRates.size >= 3) "HIGH" else if (learnedRates.isNotEmpty()) "MEDIUM" else "CALIBRATING",
        reasoning =
          if (learnedRates.isNotEmpty()) {
            "Learned ${"%.1f".format(baseWhPerKm)} Wh/km across ${learnedRates.size} past ride(s) + ${"%.0f".format(snap.elevationGainM)}m climb."
          } else {
            "Calibrating from vehicle pack (${packWh.toInt()}Wh) and rider weight (${riderWeightLbs.toInt()} lbs)."
          },
        ridesAnalyzed = matchingRides.size,
      )
    // Emit immediate learned baseline, then refine with Gemini AI
    onResult(localFallback)

    viewModelScope.launch {
      aiRepo
        .predictRemainingBatteryFromHistory(
          rides = allRides,
          vehicle = activeVehicle,
          startBatteryPct = startBatteryPct,
          currentTripKm = snap.tripDistanceKm,
          currentElevGainM = snap.elevationGainM,
          avgSpeedKmh = snap.avgSpeedKmh,
          riderWeightLbs = riderWeightLbs,
          modelId = modelId,
        )
        .onSuccess(onResult)
    }
  }

  fun requestRideDebrief(
    uid: String,
    ride: RideSession,
    weatherSummary: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_5_FLASH_LITE,
    vehicle: PevVehicle? = null,
    batteryChemistry: String = "NMC",
  ) {
    val effectiveUid = uid.ifBlank { "local_pilot" }
    viewModelScope.launch {
      _debriefBusyId.value = ride.id
      val matchedVeh =
        vehicle
          ?: _vehicles.value.firstOrNull { it.id == ride.vehicleId || it.name.equals(ride.vehicleName, ignoreCase = true) }
          ?: _vehicles.value.firstOrNull()
      val res = aiRepo.generateRideDebrief(ride, weatherSummary, modelId, matchedVeh, batteryChemistry)
      res.onSuccess { text ->
        _rides.value = _rides.value.map { if (it.id == ride.id) it.copy(aiDebrief = text) else it }
        if (boundFirebaseAuth && effectiveUid != "local_pilot") {
          riderRepo.updateRideDebrief(effectiveUid, ride.id, text)
        }
        _statusBanner.value = "Ridge-to-Ridge AI Debrief ready for ${ride.title}."
      }
      res.onFailure { _statusBanner.value = it.message ?: "AI Debrief error" }
      _debriefBusyId.value = null
    }
  }

  fun requestFleetLogbookDebrief(
    activeVehicle: PevVehicle?,
    batteryChemistry: String = "NMC",
    modelId: String = GeminiModelCatalog.GEMINI_3_5_FLASH_LITE,
  ) {
    val list = _rides.value
    if (list.isEmpty()) {
      _fleetDebrief.value = "Record at least one ride first so your Barn Fleet AI can analyze real efficiency, voltage drop, and maintenance pacing."
      return
    }
    viewModelScope.launch {
      _fleetDebriefBusy.value = true
      val res = aiRepo.generateFleetLogbookDebrief(list, activeVehicle, batteryChemistry, modelId)
      _fleetDebrief.value = res.getOrElse { it.message ?: "Fleet briefing unavailable right now." }
      _fleetDebriefBusy.value = false
    }
  }

  fun requestTelemetrySafetyBrief(
    limitMph: Int?,
    weatherSummary: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_5_FLASH_LITE,
  ) {
    viewModelScope.launch {
      _aiSafetyBusy.value = true
      val t = telemetry.value
      val estMotorTemp = (36 + (t.speedKmh * 0.45) + (t.tripDistanceKm * 0.4)).toInt().coerceIn(28, 105)
      val sensorContext =
        buildString {
          append(weatherSummary)
          append(" | GPS Satellites: ${t.satellitesUsed} locked / ${t.satellitesVisible} visible (±${t.gpsAccuracyM.toInt()}m)")
          if (t.barometerHpa > 0f) append(" | Barometer: ${"%.1f".format(t.barometerHpa)} hPa")
          if (t.ambientLightLux >= 0f) append(" | Ambient Light: ${t.ambientLightLux.toInt()} lux")
          if (t.roadRoughnessG > 0.01f) append(" | Road Vibration: ${"%.2f".format(t.roadRoughnessG)}G")
          append(" | Lean: ${"%.1f".format(t.rollDeg)}° | Pitch: ${"%.1f".format(t.pitchDeg)}°")
        }
      val res =
        aiRepo.analyzeTelemetrySafety(
          speedMph = t.speedKmh * 0.621371,
          limitMph = limitMph,
          batteryPct = t.batteryPct,
          voltage = t.estimatedVoltage,
          motorTempC = estMotorTemp,
          weatherSummary = sensorContext,
          modelId = modelId,
        )
      _aiSafetyBrief.value = res.getOrElse { it.message ?: "AI safety check unavailable." }
      _aiSafetyBusy.value = false
    }
  }

  fun deleteRide(uid: String, rideId: String) {
    viewModelScope.launch {
      if (boundFirebaseAuth) {
        riderRepo.deleteRideSession(uid, rideId)
      } else {
        _rides.value = _rides.value.filterNot { it.id == rideId }
      }
    }
  }

  fun saveVehicle(uid: String, vehicle: PevVehicle, setActive: Boolean = true) {
    viewModelScope.launch {
      var savedId = vehicle.id
      if (boundFirebaseAuth) {
        val res = riderRepo.saveVehicle(uid, vehicle)
        res.onSuccess { id ->
          savedId = id
          if (setActive) riderRepo.setActiveVehicle(uid, id)
          _statusBanner.value = "Vehicle ${vehicle.name} saved to garage."
        }
      } else {
        val vid = vehicle.id.ifBlank { "veh_${UUID.randomUUID().toString().take(8)}" }
        savedId = vid
        val stored = vehicle.copy(id = vid, userId = uid)
        val next = listOf(stored) + _vehicles.value.filterNot { it.id == vid }
        localVault.saveVehicles(uid, next)
        _vehicles.value = next
        _statusBanner.value = "Vehicle ${vehicle.name} saved to local vault."
      }

      // Automatically retrieve and set real PEV photo + official brand logo if blank
      val needsPhoto = vehicle.photoUri.isBlank() && vehicle.name.trim().length >= 3 && savedId.isNotBlank()
      val needsBrandLogo = vehicle.brandLogoUri.isBlank() && vehicle.name.trim().length >= 2 && savedId.isNotBlank()
      if (needsPhoto || needsBrandLogo) {
        val fetchedPhoto = if (needsPhoto) LiveCloudServices.fetchPevProductImageBase64(vehicle.name) else vehicle.photoUri
        val fetchedLogo = if (needsBrandLogo) LiveCloudServices.fetchPevBrandLogoBase64(vehicle.name) else vehicle.brandLogoUri
        if (fetchedPhoto.isNotBlank() || fetchedLogo.isNotBlank()) {
          val enriched =
            vehicle.copy(
              id = savedId,
              userId = uid,
              photoUri = fetchedPhoto.ifBlank { vehicle.photoUri },
              brandLogoUri = fetchedLogo.ifBlank { vehicle.brandLogoUri },
            )
          if (boundFirebaseAuth && fetchedPhoto.isNotBlank() && needsPhoto) {
            riderRepo.saveVehicle(uid, enriched)
          }
          val next = listOf(enriched) + _vehicles.value.filterNot { it.id == savedId }
          localVault.saveVehicles(uid, next)
          _vehicles.value = next
          _statusBanner.value =
            when {
              needsPhoto && fetchedPhoto.isNotBlank() && fetchedLogo.isNotBlank() ->
                "PEV photo & brand logo auto-fetched for ${vehicle.name}."
              needsPhoto && fetchedPhoto.isNotBlank() ->
                "PEV photo auto-fetched & set for ${vehicle.name}."
              fetchedLogo.isNotBlank() ->
                "Official brand logo fetched for ${vehicle.name}."
              else -> _statusBanner.value
            }
        }
      }
    }
  }

  fun selectActiveVehicle(uid: String, vehicleId: String) {
    viewModelScope.launch {
      if (boundFirebaseAuth) {
        riderRepo.setActiveVehicle(uid, vehicleId)
      }
    }
  }

  fun deleteVehicle(uid: String, vehicleId: String) {
    viewModelScope.launch {
      if (boundFirebaseAuth) {
        riderRepo.deleteVehicle(uid, vehicleId)
      } else {
        val next = _vehicles.value.filterNot { it.id == vehicleId }
        localVault.saveVehicles(uid, next)
        _vehicles.value = next
      }
    }
  }

  fun upsertMaintenance(uid: String, task: MaintenanceTask) {
    viewModelScope.launch {
      if (boundFirebaseAuth) {
        riderRepo.upsertMaintenance(uid, task)
      } else {
        val tid = task.id.ifBlank { "maint_${UUID.randomUUID().toString().take(8)}" }
        val stored = task.copy(id = tid, userId = uid)
        val next = listOf(stored) + _maintenance.value.filterNot { it.id == tid }
        localVault.saveTasks(uid, next)
        _maintenance.value = next
      }
    }
  }

  fun deleteMaintenance(uid: String, taskId: String) {
    viewModelScope.launch {
      if (boundFirebaseAuth) {
        riderRepo.deleteMaintenance(uid, taskId)
      } else {
        val next = _maintenance.value.filterNot { it.id == taskId }
        localVault.saveTasks(uid, next)
        _maintenance.value = next
      }
    }
  }

  fun resetWearItem(uid: String, itemId: String, currentOdometerKm: Double) {
    viewModelScope.launch {
      val updated =
        _wearStates.value.map { w ->
          if (w.itemId == itemId) w.copy(lastResetKm = currentOdometerKm) else w
        }
      _wearStates.value = updated
      localVault.saveWearStates(uid, updated)
      val resetLabel = updated.firstOrNull { it.itemId == itemId }?.label ?: "Component"
      val newLog =
        ServiceLogRecord(
          id = "srv_${UUID.randomUUID().toString().take(8)}",
          dateLabel = SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(Date()),
          component = resetLabel,
          notes = "Wear counter reset at ${"%.1f".format(currentOdometerKm)} km",
          costUsd = 0.0,
          odometerKm = currentOdometerKm,
        )
      val nextLogs = listOf(newLog) + _serviceLogs.value
      _serviceLogs.value = nextLogs
      localVault.saveServiceLogs(uid, nextLogs)
      _statusBanner.value = "$resetLabel wear counter reset & logged."
    }
  }

  fun togglePreflightItem(uid: String, itemKey: String) {
    val effectiveUid = uid.ifBlank { "local_pilot" }
    viewModelScope.launch {
      val next = _preflightChecked.value.toMutableSet()
      if (!next.add(itemKey)) next.remove(itemKey)
      _preflightChecked.value = next
      localVault.savePreflightChecklist(effectiveUid, next + _trailKitChecked.value)
    }
  }

  fun resetPreflightChecklist(uid: String) {
    val effectiveUid = uid.ifBlank { "local_pilot" }
    viewModelScope.launch {
      _preflightChecked.value = emptySet()
      localVault.savePreflightChecklist(effectiveUid, _trailKitChecked.value)
      _statusBanner.value = "Pre-ride check cleared for next rollout."
    }
  }

  fun toggleTrailKitItem(uid: String, itemKey: String) {
    val effectiveUid = uid.ifBlank { "local_pilot" }
    val storageKey = if (itemKey.startsWith("kit_")) itemKey else "kit_$itemKey"
    viewModelScope.launch {
      val next = _trailKitChecked.value.toMutableSet()
      if (!next.add(storageKey)) next.remove(storageKey)
      _trailKitChecked.value = next
      localVault.savePreflightChecklist(effectiveUid, _preflightChecked.value + next)
    }
  }

  fun resetTrailKit(uid: String) {
    val effectiveUid = uid.ifBlank { "local_pilot" }
    viewModelScope.launch {
      _trailKitChecked.value = emptySet()
      localVault.savePreflightChecklist(effectiveUid, _preflightChecked.value)
      _statusBanner.value = "Saddlebag trail kit checklist reset."
    }
  }

  fun addServiceLog(uid: String, component: String, notes: String, costUsd: Double, odometerKm: Double) {
    val effectiveUid = uid.ifBlank { "local_pilot" }
    viewModelScope.launch {
      val entry =
        ServiceLogRecord(
          id = "srv_${UUID.randomUUID().toString().take(8)}",
          dateLabel = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date()),
          component = component.ifBlank { "General Service" },
          notes = notes,
          costUsd = costUsd.coerceAtLeast(0.0),
          odometerKm = odometerKm,
        )
      val next = listOf(entry) + _serviceLogs.value
      _serviceLogs.value = next
      localVault.saveServiceLogs(effectiveUid, next)
      _statusBanner.value = "Service logged: ${entry.component}"
    }
  }

  fun deleteServiceLog(uid: String, logId: String) {
    val effectiveUid = uid.ifBlank { "local_pilot" }
    viewModelScope.launch {
      val next = _serviceLogs.value.filterNot { it.id == logId }
      _serviceLogs.value = next
      localVault.saveServiceLogs(effectiveUid, next)
      _statusBanner.value = "Service log removed."
    }
  }

  fun exportRideAsGpx(ride: RideSession): String {
    val trkpts =
      ride.polylinePoints.joinToString("\n") { pt ->
        val p = pt.split(",")
        val lat = p.getOrNull(0)?.trim() ?: "0.0"
        val lon = p.getOrNull(1)?.trim() ?: "0.0"
        """      <trkpt lat="$lat" lon="$lon"></trkpt>"""
      }
    return """<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="Rural ERides GO">
  <trk>
    <name>${ride.title} (${ride.vehicleName})</name>
    <trkseg>
$trkpts
    </trkseg>
  </trk>
</gpx>"""
  }

  fun exportRidesAsCsv(list: List<RideSession>): String {
    val header = "id,vehicle,title,duration_sec,distance_km,max_kmh,avg_kmh,elev_m,wh_used"
    val rows =
      list.joinToString("\n") { r ->
        "${r.id},\"${r.vehicleName}\",\"${r.title}\",${r.durationSec},${r.distanceKm},${r.maxSpeedKmh},${r.avgSpeedKmh},${r.elevationGainM},${r.whUsed}"
      }
    return "$header\n$rows"
  }

  fun clearBanner() {
    _statusBanner.value = ""
  }

  override fun onCleared() {
    telemetryEngine.shutdown()
    super.onCleared()
  }
}
