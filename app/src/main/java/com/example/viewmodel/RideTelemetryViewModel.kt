package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.PevBatteryPhysics
import com.example.data.MasterSettings
import com.example.data.RideSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class RideTelemetryUiState(
    val rides: List<RideSession> = emptyList(),
    val filteredRides: List<RideSession> = emptyList(),
    val vehicleFilter: String = "ALL",
    val windowLimit: Int = 0, // 0 = all, 5, 10, 25
    val vehicleOptions: List<String> = listOf("ALL"),
    val studioMode: Int = 0, // 0 = Fleet Graphs, 1 = Per-Ride Deep Dive
    val selectedRideId: String = "",
    val totalKm: Double = 0.0,
    val topSpeedKmh: Double = 0.0,
    val totalElevM: Double = 0.0,
    val totalWh: Double = 0.0,
    val totalMins: Int = 0,
    val avgEff: Double = 0.0,
    val isLoading: Boolean = false,
)

class RideTelemetryViewModel(application: Application) : AndroidViewModel(application) {
    private val _vehicleFilter = MutableStateFlow("ALL")
    private val _windowLimit = MutableStateFlow(0)
    private val _studioMode = MutableStateFlow(0)
    private val _selectedRideId = MutableStateFlow("")

    private val _rawRides = MutableStateFlow<List<RideSession>>(emptyList())
    private val _settings = MutableStateFlow(MasterSettings())

    fun updateRides(rides: List<RideSession>) {
        _rawRides.value = rides
        if (_selectedRideId.value.isBlank() && rides.isNotEmpty()) {
            _selectedRideId.value = rides.first().id
        }
    }

    fun updateSettings(settings: MasterSettings) {
        _settings.value = settings
    }

    fun setVehicleFilter(filter: String) {
        _vehicleFilter.value = filter
    }

    fun setWindowLimit(limit: Int) {
        _windowLimit.value = limit
    }

    fun setStudioMode(mode: Int) {
        _studioMode.value = mode
    }

    fun setSelectedRideId(id: String) {
        _selectedRideId.value = id
    }

    private data class FirstThree(val rides: List<RideSession>, val filter: String, val limit: Int)
    private data class FirstFour(val first: FirstThree, val mode: Int)
    private data class FirstFive(val first: FirstFour, val rideId: String)

    val uiState: StateFlow<RideTelemetryUiState> = combine(
        _rawRides,
        _vehicleFilter,
        _windowLimit,
    ) { rawRides, vehicleFilter, windowLimit ->
        FirstThree(rawRides, vehicleFilter, windowLimit)
    }.combine(_studioMode) { first, studioMode ->
        FirstFour(first, studioMode)
    }.combine(_selectedRideId) { first4, selectedRideId ->
        FirstFive(first4, selectedRideId)
    }.combine(_settings) { p, settings ->
        val rawRides = p.first.first.rides
        val vehicleFilter = p.first.first.filter
        val windowLimit = p.first.first.limit
        val studioMode = p.first.mode
        val selectedRideId = p.rideId

        val vehicleOptions = listOf("ALL") + rawRides.map { it.vehicleName }.distinct().filter { it.isNotBlank() }
        val filtered = run {
            val base = if (vehicleFilter == "ALL") rawRides else rawRides.filter { it.vehicleName == vehicleFilter }
            if (windowLimit > 0) base.take(windowLimit) else base
        }

        val totalKm = filtered.sumOf { it.distanceKm.takeIf { v -> v.isFinite() } ?: 0.0 }
        val topSpeedKmh = filtered.maxOfOrNull { it.maxSpeedKmh.takeIf { v -> v.isFinite() } ?: 0.0 } ?: 0.0
        val totalElevM = filtered.sumOf { it.elevationGainM.takeIf { v -> v.isFinite() } ?: 0.0 }
        val totalWh = filtered.sumOf { it.whUsed.takeIf { v -> v.isFinite() } ?: 0.0 }
        val totalMins = (filtered.sumOf { it.durationSec.coerceAtLeast(0L) } / 60).toInt()
        val avgEff = if (totalKm > 0.0) {
            (if (settings.distanceUnit == "km") PevBatteryPhysics.whPerKm(totalWh, totalKm)
            else PevBatteryPhysics.whPerMile(totalWh, totalKm)).takeIf { it.isFinite() } ?: 0.0
        } else 0.0

        val resolvedRideId = if (selectedRideId.isBlank() && rawRides.isNotEmpty()) {
            rawRides.first().id
        } else {
            selectedRideId
        }

        RideTelemetryUiState(
            rides = rawRides,
            filteredRides = filtered,
            vehicleFilter = vehicleFilter,
            windowLimit = windowLimit,
            vehicleOptions = vehicleOptions,
            studioMode = studioMode,
            selectedRideId = resolvedRideId,
            totalKm = totalKm,
            topSpeedKmh = topSpeedKmh,
            totalElevM = totalElevM,
            totalWh = totalWh,
            totalMins = totalMins,
            avgEff = avgEff,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RideTelemetryUiState(isLoading = true)
    )
}
