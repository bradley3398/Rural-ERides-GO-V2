package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.ElectricScooter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.PevBatteryPhysics
import com.example.data.LiveTelemetrySnapshot
import com.example.data.MasterSettings
import com.example.data.PevVehicle
import com.example.data.UiState
import com.example.data.WeatherMatrixData
import com.example.ui.components.RuralPevIcons
import com.example.ui.components.TacticalButton
import com.example.ui.components.TacticalCard
import com.example.ui.components.TacticalOutlinedButton
import com.example.ui.components.distUnitLabel
import com.example.ui.components.formatDistValue
import com.example.ui.components.formatSpeedValue
import com.example.ui.components.isWindowsClassicActive
import com.example.ui.components.speedUnitLabel
import com.example.ui.components.windowsClassicBevel
import com.example.ui.theme.TacticalColors
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class PevRangeProjection(
  val seriesCount: Int,
  val perCellVolts: Double,
  val socPct: Double,
  val remainingWh: Double,
  val burnWhPerKm: Double,
  val burnWhPerMi: Double,
  val estimatedRangeKm: Double,
  val safeTurnaroundKm: Double,
  val sagWarning: Boolean,
  val tempDeratePct: Int,
)

internal object PevRangeMath {
  fun inferSeriesCount(nominalVoltage: Double, chemistry: String = "NMC"): Int =
    PevBatteryPhysics.resolveSeriesCount(nominalVoltage, chemistry)

  fun estimateVoltageSocPct(packVoltage: Double, seriesCount: Int, chemistry: String = "NMC"): Double =
    PevBatteryPhysics.socForSeriesVoltage(seriesCount, packVoltage, chemistry)

  fun computeRangeProjection(
    batteryWh: Double,
    measuredVoltage: Double,
    nominalVoltage: Double,
    chemistry: String,
    cruiseMph: Double,
    riderPlusGearLbs: Double,
    ambientTempC: Double,
    windKmh: Double,
    regenPct: Int,
  ): PevRangeProjection {
    val profile = PevBatteryPhysics.packProfile(nominalVoltage, chemistry)
    val series = profile.seriesCount
    val soc = PevBatteryPhysics.socForVoltage(nominalVoltage, measuredVoltage, chemistry)
    val perCell = measuredVoltage / series.coerceAtLeast(1)

    // Continuous chemistry-weighted cold/heat capacity retention (NMC @ -2C = 26% derate)
    val coldRetention = PevBatteryPhysics.coldCapacityFactor(ambientTempC, chemistry)
    val tempDerate = ((1.0 - coldRetention) * 100.0).roundToInt().coerceIn(0, 58)

    // Realistic PEV energy consumption curve (aerodynamic v^2 drag + rolling resistance + wind + regen)
    val clampedSpd = cruiseMph.coerceIn(8.0, 65.0)
    val baseWhPerMi = 14.0 + (clampedSpd * 0.48) + ((clampedSpd / 20.0) * (clampedSpd / 20.0) * 6.2)
    val weightMult = 1.0 + ((riderPlusGearLbs - 175.0) * 0.0022).coerceIn(-0.15, 0.45)
    val windPenalty = 1.0 + (windKmh * 0.0045).coerceIn(0.0, 0.25)
    val regenSavings = (1.0 - (regenPct.coerceIn(0, 20) / 100.0))
    val burnWhPerMi = (baseWhPerMi * weightMult * windPenalty * regenSavings).coerceAtLeast(12.0)
    val burnWhPerKm = burnWhPerMi * 0.621371

    // Estimate cruise draw amps for Peukert + LVC discharge efficiency factor
    val estCruiseWatts = burnWhPerMi * clampedSpd
    val estAmps = estCruiseWatts / profile.nominalPackV.coerceAtLeast(24.0)
    val packAh = PevBatteryPhysics.computePackAh(batteryWh, nominalVoltage, chemistry).coerceAtLeast(4.0)
    val pParallel = (packAh / 4.8).coerceIn(1.0, 16.0)
    val sim =
      PevBatteryPhysics.simulateUnderLoadDischarge(
        seriesCells = series,
        parallelCells = pParallel,
        cellCapacityAh = packAh / pParallel,
        loadAmps = estAmps,
        socPct = soc,
        ambientTempC = ambientTempC,
        chemistry = chemistry,
      )
    val curveFactor = (sim.peukertRetentionPct / 100.0).coerceIn(0.35, 1.0)
    val usableWh = batteryWh * (soc / 100.0) * curveFactor
    val rangeMi = (usableWh / burnWhPerMi).coerceAtLeast(0.0)
    val rangeKm = rangeMi * 1.60934
    val safeTurnKm = rangeKm * 0.45

    val sagThreshold = if (PevBatteryPhysics.isLfp(chemistry)) 3.05 else 3.35
    return PevRangeProjection(
      seriesCount = series,
      perCellVolts = perCell,
      socPct = soc,
      remainingWh = usableWh,
      burnWhPerKm = burnWhPerKm,
      burnWhPerMi = burnWhPerMi,
      estimatedRangeKm = rangeKm,
      safeTurnaroundKm = safeTurnKm,
      sagWarning = perCell in 0.5..sagThreshold || sim.hitsLvc,
      tempDeratePct = tempDerate,
    )
  }
}

@Composable
fun PevRangeAndChargePlannerCard(
  vehicle: PevVehicle?,
  weatherState: UiState<WeatherMatrixData>,
  settings: MasterSettings,
) {
  val nomV = vehicle?.voltage ?: 48.0
  val packWh = (vehicle?.batteryWh ?: 1200L).toDouble()
  val packProfile = remember(nomV, settings.batteryChemistry) { PevBatteryPhysics.packProfile(nomV, settings.batteryChemistry) }
  val series = packProfile.seriesCount
  val defaultFullV = packProfile.fullChargeV

  var measuredVStr by remember(vehicle?.id, series, settings.batteryChemistry) { mutableStateOf("%.1f".format(defaultFullV)) }
  var cruiseMphStr by remember { mutableStateOf("22") }
  var riderLbsStr by remember { mutableStateOf(settings.riderWeightLbs.coerceIn(90, 350).toString()) }
  var chargerAmpsStr by remember { mutableStateOf("4.0") }
  var regenPct by remember { mutableIntStateOf(6) }
  var showVoltTable by remember { mutableStateOf(false) }

  val weather = (weatherState as? UiState.Success)?.data
  val ambientC = weather?.tempC ?: 20.0
  val windKmh = weather?.windSpeedKmh ?: 8.0

  val measuredV = measuredVStr.toDoubleOrNull() ?: defaultFullV
  val cruiseMph = cruiseMphStr.toDoubleOrNull() ?: 22.0
  val riderLbs = riderLbsStr.toDoubleOrNull() ?: 180.0
  val chargerA = (chargerAmpsStr.toDoubleOrNull() ?: 4.0).coerceIn(0.5, 40.0)

  val proj =
    PevRangeMath.computeRangeProjection(
      batteryWh = packWh,
      measuredVoltage = measuredV,
      nominalVoltage = nomV,
      chemistry = settings.batteryChemistry,
      cruiseMph = cruiseMph,
      riderPlusGearLbs = riderLbs,
      ambientTempC = ambientC,
      windKmh = windKmh,
      regenPct = regenPct,
    )

  val chargerSpec = PevBatteryPhysics.estimateChargerWatts(nomV, chargerA, proj.socPct, settings.batteryChemistry)
  val activeChargeV = ((measuredV.coerceIn(packProfile.cutoffEmptyV, packProfile.fullChargeV) + packProfile.fullChargeV) / 2.0)
  val activeChargeWatts = activeChargeV * chargerA
  val addedWhIn30Min = activeChargeWatts * 0.5 * 0.94
  val addedKm30Min = addedWhIn30Min / proj.burnWhPerKm.coerceAtLeast(8.0)
  val hoursToFull =
    PevBatteryPhysics.estimateChargeTimeHours(
      batteryWh = packWh,
      ratedVoltage = nomV,
      chargerAmps = chargerA,
      startSocPct = proj.socPct,
      targetSocPct = 100.0,
      chemistry = settings.batteryChemistry,
    )
  val hoursTo80 =
    PevBatteryPhysics.estimateChargeTimeHours(
      batteryWh = packWh,
      ratedVoltage = nomV,
      chargerAmps = chargerA,
      startSocPct = proj.socPct,
      targetSocPct = 80.0,
      chemistry = settings.batteryChemistry,
    )

  TacticalCard(highlight = proj.sagWarning, title = "Pack Volt & Range Gauge") {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Icon(Icons.Default.BatteryChargingFull, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(6.dp))
        Text("PACK VOLT, SOC & BARN CHARGE PLANNER", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1)
      }
      Text("${packProfile.seriesCount}S ${packProfile.chemistry}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
    }
    Spacer(Modifier.height(4.dp))
    Text(
      "100% Full: ${"%.1f".format(packProfile.fullChargeV)}V • Nominal: ${"%.1f".format(packProfile.nominalV)}V • Storage: ${"%.1f".format(packProfile.storageV)}V • 0% Empty: ${"%.1f".format(packProfile.cutoffEmptyV)}V",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.secondary,
    )
    Spacer(Modifier.height(6.dp))

    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      item {
        FilterChip(
          selected = cruiseMphStr == "18",
          onClick = { cruiseMphStr = "18" },
          label = { Text("Eco 18mph", maxLines = 1, softWrap = false) },
        )
      }
      item {
        FilterChip(
          selected = cruiseMphStr == "25",
          onClick = { cruiseMphStr = "25" },
          label = { Text("Trail 25mph", maxLines = 1, softWrap = false) },
        )
      }
      item {
        FilterChip(
          selected = cruiseMphStr == "40",
          onClick = { cruiseMphStr = "40" },
          label = { Text("Send 40mph", maxLines = 1, softWrap = false) },
        )
      }
      items(listOf(0, 6, 12)) { r ->
        FilterChip(
          selected = regenPct == r,
          onClick = { regenPct = r },
          label = { Text("Regen $r%", maxLines = 1, softWrap = false) },
        )
      }
    }

    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      OutlinedTextField(
        value = measuredVStr,
        onValueChange = { measuredVStr = it },
        label = { Text("Pack Volts") },
        singleLine = true,
        modifier = Modifier.weight(1f).testTag("pev_measured_volts_input"),
      )
      OutlinedTextField(
        value = cruiseMphStr,
        onValueChange = { cruiseMphStr = it },
        label = { Text("Cruise MPH") },
        singleLine = true,
        modifier = Modifier.weight(1f),
      )
      OutlinedTextField(
        value = riderLbsStr,
        onValueChange = { riderLbsStr = it },
        label = { Text("Rider lbs") },
        singleLine = true,
        modifier = Modifier.weight(1f),
      )
      OutlinedTextField(
        value = chargerAmpsStr,
        onValueChange = { chargerAmpsStr = it },
        label = { Text("Chg A") },
        singleLine = true,
        modifier = Modifier.weight(1f),
      )
    }

    Spacer(Modifier.height(8.dp))
    LinearProgressIndicator(
      progress = { (proj.socPct / 100.0).toFloat().coerceIn(0f, 1f) },
      modifier = Modifier.fillMaxWidth().height(7.dp),
    )
    Spacer(Modifier.height(6.dp))

    val distUnit = distUnitLabel(settings.distanceUnit)
    val estDist = formatDistValue(proj.estimatedRangeKm, settings.distanceUnit)
    val safeReturn = formatDistValue(proj.safeTurnaroundKm, settings.distanceUnit)
    val boost30 = formatDistValue(addedKm30Min, settings.distanceUnit)
    val burnPerDist =
      if (settings.distanceUnit == "km") "${"%.1f".format(proj.burnWhPerKm)} Wh/km"
      else "${"%.1f".format(proj.burnWhPerMi)} Wh/mi"

    Text(
      "True SOC: ${"%.0f".format(proj.socPct)}% (${"%.2f".format(proj.perCellVolts)}V/cell) • Usable: ${proj.remainingWh.toInt()} Wh • $burnPerDist",
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.primary,
    )
    Text(
      "Weather-Adjusted Range: ${"%.1f".format(estDist)} $distUnit • Safe Return Radius: ${"%.1f".format(safeReturn)} $distUnit" +
        if (proj.tempDeratePct > 0) " (-${proj.tempDeratePct}% cold/heat derate)" else "",
      style = MaterialTheme.typography.bodyMedium,
    )
    Text(
      "30m @ ${chargerA}A Pit Stop: +${"%.1f".format(boost30)} $distUnit (~${activeChargeWatts.toInt()}W avg / ${chargerSpec.peakDcWatts.toInt()}W max) • To 80%: ${"%.1f".format(hoursTo80)}h • Full 100% CC+CV: ${"%.1f".format(hoursToFull)}h",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.secondary,
    )
    Spacer(Modifier.height(6.dp))
    TacticalOutlinedButton(
      onClick = { showVoltTable = !showVoltTable },
      modifier = Modifier.fillMaxWidth().testTag("toggle_voltage_chart_button"),
    ) {
      Text(
        if (showVoltTable) "Hide Exact ${packProfile.seriesCount}S Voltage ↔ % Chart" else "Show Exact ${packProfile.seriesCount}S Voltage ↔ % Chart",
        maxLines = 1,
        softWrap = false,
      )
    }
    if (showVoltTable) {
      Spacer(Modifier.height(6.dp))
      val rows = remember(nomV, settings.batteryChemistry) { PevBatteryPhysics.socVoltageTable(nomV, settings.batteryChemistry) }
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(
            "EXACT ${packProfile.nominalLabel} (${packProfile.seriesCount}S ${packProfile.chemistry}) RESTING VOLTAGE CHART",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
          )
          rows.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              pair.forEach { (socInt, vVal) ->
                Text(
                  text = "$socInt% = ${"%.1f".format(vVal)}V",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (socInt == 100 || socInt == 50 || socInt == 0) FontWeight.Bold else FontWeight.Normal,
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }
        }
      }
    }

    if (proj.sagWarning) {
      Spacer(Modifier.height(6.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
          "LOW CELL CUTOFF WARNING (${"%.2f".format(proj.perCellVolts)}V/cell): Ease throttle to prevent BMS undervoltage cutout!",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.error,
        )
      }
    }
  }
}

@Composable
fun PevAccelBrakeTimerCard(
  telemetry: LiveTelemetrySnapshot,
  settings: MasterSettings,
) {
  var armed by remember { mutableStateOf(false) }
  var launchStartMs by remember { mutableLongStateOf(0L) }
  var split15Sec by remember { mutableDoubleStateOf(0.0) }
  var split20Sec by remember { mutableDoubleStateOf(0.0) }
  var split30Sec by remember { mutableDoubleStateOf(0.0) }
  var peakRunMph by remember { mutableDoubleStateOf(0.0) }

  var brakeActive by remember { mutableStateOf(false) }
  var brakeStartMs by remember { mutableLongStateOf(0L) }
  var brakeStartMph by remember { mutableDoubleStateOf(0.0) }
  var lastStopSec by remember { mutableDoubleStateOf(0.0) }
  var lastPeakBrakeG by remember { mutableDoubleStateOf(0.0) }

  val curMph = telemetry.speedKmh * 0.621371

  LaunchedEffect(armed, curMph, telemetry.peakG) {
    if (!armed) return@LaunchedEffect
    val now = System.currentTimeMillis()
    if (launchStartMs == 0L && curMph >= 1.5) {
      launchStartMs = now
      peakRunMph = curMph
    }
    if (launchStartMs > 0L) {
      if (curMph > peakRunMph) peakRunMph = curMph
      val elapsed = (now - launchStartMs) / 1000.0
      if (split15Sec <= 0.0 && curMph >= 15.0) split15Sec = elapsed
      if (split20Sec <= 0.0 && curMph >= 20.0) split20Sec = elapsed
      if (split30Sec <= 0.0 && curMph >= 30.0) split30Sec = elapsed

      // Detect rapid braking phase from >= 15 mph down to <= 2 mph
      if (!brakeActive && peakRunMph >= 15.0 && (peakRunMph - curMph) >= 4.0) {
        brakeActive = true
        brakeStartMs = now
        brakeStartMph = curMph
        lastPeakBrakeG = abs(telemetry.peakG.toDouble())
      }
      if (brakeActive) {
        val curG = abs(telemetry.peakG.toDouble())
        if (curG > lastPeakBrakeG) lastPeakBrakeG = curG
        if (curMph <= 2.0 && lastStopSec <= 0.0) {
          lastStopSec = ((now - brakeStartMs) / 1000.0).coerceAtLeast(0.1)
          brakeActive = false
        }
      }
    }
  }

  TacticalCard(highlight = armed, title = "Acceleration & Braking Timer") {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(6.dp))
        Text("GPS + IMU PERFORMANCE BENCHMARK", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
      }
      Text(
        when {
          !armed -> "STANDBY"
          launchStartMs == 0L -> "ARMED (WAIT MOVE)"
          else -> "MEASURING"
        },
        style = MaterialTheme.typography.labelSmall,
        color = if (armed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Spacer(Modifier.height(6.dp))
    val spdLabel = speedUnitLabel(settings.distanceUnit)
    val t15Label = if (settings.distanceUnit == "km") "0–24" else "0–15"
    val t20Label = if (settings.distanceUnit == "km") "0–32" else "0–20"
    val t30Label = if (settings.distanceUnit == "km") "0–48" else "0–30"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Column {
        Text("$t15Label $spdLabel", style = MaterialTheme.typography.labelSmall)
        Text(if (split15Sec > 0.0) "${"%.2f".format(split15Sec)}s" else "--", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
      }
      Column {
        Text("$t20Label $spdLabel", style = MaterialTheme.typography.labelSmall)
        Text(if (split20Sec > 0.0) "${"%.2f".format(split20Sec)}s" else "--", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
      }
      Column {
        Text("$t30Label $spdLabel", style = MaterialTheme.typography.labelSmall)
        Text(if (split30Sec > 0.0) "${"%.2f".format(split30Sec)}s" else "--", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
      }
      Column {
        Text("Brake Stop", style = MaterialTheme.typography.labelSmall)
        Text(
          if (lastStopSec > 0.0) "${"%.2f".format(lastStopSec)}s" else "--",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.secondary,
        )
      }
    }
    Spacer(Modifier.height(4.dp))
    Text(
      "Peak Run: ${"%.1f".format(formatSpeedValue(peakRunMph / 0.621371, settings.distanceUnit))} $spdLabel • Peak Braking: ${"%.2f".format(lastPeakBrakeG)}G (Live GPS + IMU)",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      TacticalButton(
        onClick = {
          if (!armed) {
            launchStartMs = 0L
            split15Sec = 0.0
            split20Sec = 0.0
            split30Sec = 0.0
            peakRunMph = 0.0
            lastStopSec = 0.0
            lastPeakBrakeG = 0.0
            brakeActive = false
            armed = true
          } else {
            armed = false
          }
        },
        modifier = Modifier.weight(1f).testTag("arm_drag_timer_button"),
      ) {
        Icon(if (armed) Icons.Default.Flag else Icons.Default.PlayArrow, null, Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(if (armed) "Disarm Box" else "Arm Launch Timer", maxLines = 1, softWrap = false)
      }
      TacticalOutlinedButton(
        onClick = {
          armed = false
          launchStartMs = 0L
          split15Sec = 0.0
          split20Sec = 0.0
          split30Sec = 0.0
          peakRunMph = 0.0
          lastStopSec = 0.0
          lastPeakBrakeG = 0.0
          brakeActive = false
        },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
      ) {
        Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Reset", maxLines = 1, softWrap = false)
      }
    }
  }
}

data class RtbVector(
  val distanceKm: Double,
  val bearingToBaseDeg: Float,
  val relativeArrowDeg: Float,
  val cardinal: String,
  val turnBackWarning: Boolean,
)

internal object PevRtbMath {
  private val cardinals = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

  fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a =
      sin(dLat / 2.0) * sin(dLat / 2.0) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2.0) * sin(dLng / 2.0)
    val c = 2.0 * atan2(sqrt(a.coerceIn(0.0, 1.0)), sqrt((1.0 - a).coerceIn(0.0, 1.0)))
    return r * c
  }

  fun initialBearingDeg(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val dLng = Math.toRadians(lng2 - lng1)
    val y = sin(dLng) * cos(phi2)
    val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLng)
    return ((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0).toFloat()
  }

  fun cardinalLabel(bearingDeg: Float): String {
    val norm = ((bearingDeg % 360f) + 360f) % 360f
    val idx = ((norm + 22.5f) / 45f).toInt() % 8
    return cardinals[idx]
  }

  fun computeRtbVector(
    curLat: Double,
    curLng: Double,
    baseLat: Double,
    baseLng: Double,
    curHeadingDeg: Float,
    estimatedRemainingRangeKm: Double,
  ): RtbVector {
    val distKm = haversineKm(curLat, curLng, baseLat, baseLng)
    val bearing = if (distKm < 0.005) 0f else initialBearingDeg(curLat, curLng, baseLat, baseLng)
    val rel = ((bearing - curHeadingDeg + 360f) % 360f)
    val turnBack = distKm > 0.05 && estimatedRemainingRangeKm <= distKm * 1.25
    return RtbVector(
      distanceKm = distKm,
      bearingToBaseDeg = bearing,
      relativeArrowDeg = rel,
      cardinal = cardinalLabel(bearing),
      turnBackWarning = turnBack,
    )
  }
}

@Composable
fun RtbHomingMiniWidget(
  telemetry: LiveTelemetrySnapshot,
  estRemainingRangeKm: Double,
  settings: MasterSettings,
) {
  val isClassic = isWindowsClassicActive(settings)
  val baseLat = telemetry.launchLat
  val baseLng = telemetry.launchLng
  val hasBase = baseLat != null && baseLng != null && (telemetry.lat != 0.0 || telemetry.lng != 0.0)

  val rtb =
    remember(telemetry.lat, telemetry.lng, baseLat, baseLng, telemetry.bearingDeg, estRemainingRangeKm) {
      if (baseLat != null && baseLng != null && (telemetry.lat != 0.0 || telemetry.lng != 0.0)) {
        PevRtbMath.computeRtbVector(
          curLat = telemetry.lat,
          curLng = telemetry.lng,
          baseLat = baseLat,
          baseLng = baseLng,
          curHeadingDeg = telemetry.bearingDeg,
          estimatedRemainingRangeKm = estRemainingRangeKm,
        )
      } else null
    }

  val containerMod =
    if (isClassic) {
      Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
        .windowsClassicBevel(sunken = true)
        .padding(horizontal = 10.dp, vertical = 8.dp)
        .testTag("rtb_homing_widget")
    } else {
      Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
        .border(
          width = 1.dp,
          color =
            if (rtb?.turnBackWarning == true) TacticalColors.SolarAmber
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
          shape = MaterialTheme.shapes.small,
        )
        .padding(horizontal = 10.dp, vertical = 8.dp)
        .testTag("rtb_homing_widget")
    }

  Row(
    modifier = containerMod,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier
            .size(34.dp)
            .background(
              color =
                when {
                  rtb == null -> MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
                  rtb.turnBackWarning -> TacticalColors.SolarAmber.copy(alpha = 0.22f)
                  else -> TacticalColors.TrailEmerald.copy(alpha = 0.20f)
                },
              shape = if (isClassic) RectangleShape else MaterialTheme.shapes.small,
            ),
      ) {
        Icon(
          imageVector = Icons.Default.Navigation,
          contentDescription = "Return to Base Compass Arrow",
          tint =
            when {
              rtb == null -> MaterialTheme.colorScheme.onSurfaceVariant
              rtb.turnBackWarning -> TacticalColors.SolarAmber
              isClassic -> TacticalColors.Win95SafeGreen
              else -> TacticalColors.TrailEmerald
            },
          modifier = Modifier.size(20.dp).rotate(rtb?.relativeArrowDeg ?: 0f),
        )
      }
      Spacer(Modifier.width(8.dp))
      Column {
        Text(
          text =
            if (rtb == null) "RTB HOMING BEACON • STANDBY"
            else {
              val d = formatDistValue(rtb.distanceKm, settings.distanceUnit)
              "RTB HOME: ${"%.2f".format(d)} ${distUnitLabel(settings.distanceUnit)} • ${rtb.cardinal} ${rtb.bearingToBaseDeg.toInt()}°"
            },
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          softWrap = false,
        )
        Text(
          text =
            if (rtb == null) "Offline launch GPS locks automatically on Start Mission"
            else "100% Offline Haversine • Est Range ${"%.1f".format(formatDistValue(estRemainingRangeKm, settings.distanceUnit))} ${distUnitLabel(settings.distanceUnit)}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
        )
      }
    }

    Spacer(Modifier.width(6.dp))
    val badgeBg =
      when {
        rtb == null -> MaterialTheme.colorScheme.surface
        rtb.turnBackWarning -> if (estRemainingRangeKm <= rtb.distanceKm) TacticalColors.CrimsonPulse else TacticalColors.SolarAmber
        isClassic -> TacticalColors.Win95SafeGreen
        else -> TacticalColors.TrailEmerald
      }
    val badgeFg =
      when {
        rtb == null -> MaterialTheme.colorScheme.onSurfaceVariant
        rtb.turnBackWarning && estRemainingRangeKm > rtb.distanceKm -> TacticalColors.Win95BlackInk
        else -> TacticalColors.StealthWhite
      }
    Box(
      modifier =
        Modifier
          .background(badgeBg, if (isClassic) RectangleShape else MaterialTheme.shapes.small)
          .padding(horizontal = 8.dp, vertical = 4.dp)
          .testTag("rtb_safety_badge"),
    ) {
      Text(
        text =
          when {
            rtb == null -> "OFFLINE"
            rtb.turnBackWarning -> "TURN BACK"
            else -> "SAFE RETURN"
          },
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        color = badgeFg,
        maxLines = 1,
        softWrap = false,
      )
    }
  }
}

@Composable
fun VoltTacticalCoachBubble(
  title: String,
  message: String,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth().testTag("win95_volt_coach_bubble")) {
    Surface(
      color = TacticalColors.Win95TooltipYellow,
      shape = RectangleShape,
      border = BorderStroke(1.dp, TacticalColors.Win95BlackInk),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Top,
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier
              .size(28.dp)
              .background(TacticalColors.Win95TitleNavy, RectangleShape)
              .border(1.dp, TacticalColors.Win95BlackInk, RectangleShape),
        ) {
          Icon(
            imageVector = Icons.Default.ElectricBolt,
            contentDescription = "Volt Tactical Coach",
            tint = TacticalColors.Win95TooltipYellow,
            modifier = Modifier.size(16.dp),
          )
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
              imageVector = Icons.Default.ElectricScooter,
              contentDescription = null,
              tint = TacticalColors.Win95BlackInk,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = title,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = TacticalColors.Win95BlackInk,
              maxLines = 1,
              softWrap = false,
            )
          }
          Spacer(Modifier.height(2.dp))
          Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = TacticalColors.Win95BlackInk,
          )
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(48.dp).testTag("dismiss_volt_bubble_button"),
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Dismiss Volt tip",
            tint = TacticalColors.Win95BlackInk,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }
    // Authentic Win95 speech bubble pointer tail
    Canvas(modifier = Modifier.padding(start = 24.dp).size(width = 18.dp, height = 10.dp)) {
      val tail =
        Path().apply {
          moveTo(0f, 0f)
          lineTo(size.width, 0f)
          lineTo(0f, size.height)
          close()
        }
      drawPath(path = tail, color = TacticalColors.Win95TooltipYellow)
      drawLine(
        color = TacticalColors.Win95BlackInk,
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(0f, size.height),
        strokeWidth = 1.dp.toPx(),
      )
      drawLine(
        color = TacticalColors.Win95BlackInk,
        start = androidx.compose.ui.geometry.Offset(0f, size.height),
        end = androidx.compose.ui.geometry.Offset(size.width, 0f),
        strokeWidth = 1.dp.toPx(),
      )
    }
  }
}

@Composable
fun PevTacticalPowerStripCard(
  telemetry: LiveTelemetrySnapshot,
  vehicle: PevVehicle?,
  weatherState: UiState<WeatherMatrixData>,
  settings: MasterSettings,
  onSelectRideMode: (String) -> Unit,
) {
  val win95 = isWindowsClassicActive(settings)
  val rideModes = listOf("ECO", "STREET", "SPORT", "SEND")
  val activeMode = settings.rideMode.uppercase().ifBlank { "SPORT" }

  val modeMultiplier =
    when (activeMode) {
      "ECO" -> 0.58
      "STREET" -> 0.82
      "SEND" -> 1.25
      else -> 1.0
    }
  val speedCapHint =
    when (activeMode) {
      "ECO" -> "Max Range • Soft Regen"
      "STREET" -> "Balanced • Legal Cruise"
      "SEND" -> "Full Phase • Max Torque"
      else -> "Crisp Throttle • Active Regen"
    }

  val nomV = (vehicle?.voltage?.takeIf { it >= 12.0 } ?: 48.0).coerceAtLeast(24.0)
  val packChem = vehicle?.batteryChemistry?.ifBlank { settings.batteryChemistry } ?: settings.batteryChemistry
  val packProf = remember(nomV, packChem) { PevBatteryPhysics.packProfile(nomV, packChem) }
  val packWh = (vehicle?.batteryWh?.takeIf { it >= 180L } ?: 960L).toDouble().coerceAtLeast(200.0)
  val packAh = PevBatteryPhysics.computePackAh(packWh, nomV, packChem).coerceAtLeast(5.0)
  val seriesCount = packProf.seriesCount
  val storageVolts = packProf.storageV

  val riderKg = (settings.riderWeightLbs.coerceIn(70, 400) * 0.453592) + ((vehicle?.vehicleWeightLbs ?: 62.0) * 0.453592)
  val speedMs = (telemetry.speedKmh / 3.6).coerceAtLeast(0.0)
  val pitchRad = Math.toRadians(telemetry.pitchDeg.toDouble().coerceIn(-30.0, 30.0))
  val aeroW = 0.5 * 1.225 * 0.65 * speedMs * speedMs * speedMs
  val rollW = 0.015 * riderKg * 9.81 * cos(pitchRad) * speedMs
  val climbW = (riderKg * 9.81 * sin(pitchRad) * speedMs).coerceAtLeast(0.0)
  val estWatts =
    if (telemetry.speedKmh < 1.2) 18.0
    else ((aeroW + rollW + climbW) / 0.84 * modeMultiplier).coerceIn(35.0, 9500.0)
  val activePackV =
    if (telemetry.estimatedVoltage > packProf.cutoffEmptyV * 0.7) {
      telemetry.estimatedVoltage.coerceIn(packProf.cutoffEmptyV, packProf.fullChargeV)
    } else packProf.nominalPackV
  val estAmps = (estWatts / activePackV.coerceAtLeast(12.0)).coerceAtLeast(0.0)
  val estCRate = (estAmps / packAh).coerceAtLeast(0.0)

  val ambientC = (weatherState as? UiState.Success)?.data?.tempC ?: 20.0
  val estMotorTempC = (ambientC + 10.0 + (estWatts / 145.0) + abs(telemetry.pitchDeg.toDouble()) * 0.9).coerceIn(ambientC, 125.0)
  val thermalLabel =
    when {
      estMotorTempC >= 85.0 -> "HOT • EASE THROTTLE"
      estMotorTempC >= 65.0 -> "WARM"
      else -> "COOL"
    }
  val thermalColor =
    when {
      estMotorTempC >= 85.0 -> MaterialTheme.colorScheme.error
      estMotorTempC >= 65.0 -> TacticalColors.SolarAmber
      else -> TacticalColors.NeonLime
    }

  val basePsi = (vehicle?.targetPsi ?: 36.0).coerceIn(15.0, 75.0)
  val coldDeltaPsi = ((ambientC - 20.0) * 0.14).roundToInt()
  val recommendedPsi = (basePsi - coldDeltaPsi).coerceIn(15.0, 75.0)

  var peakWatts by remember { mutableDoubleStateOf(0.0) }
  var peakAmps by remember { mutableDoubleStateOf(0.0) }
  var peakSpeedKmh by remember { mutableDoubleStateOf(0.0) }
  var tripABaseKm by remember { mutableDoubleStateOf(telemetry.tripDistanceKm) }
  var tripBBaseKm by remember { mutableDoubleStateOf(telemetry.tripDistanceKm) }

  LaunchedEffect(estWatts, estAmps, telemetry.speedKmh) {
    if (estWatts > peakWatts) peakWatts = estWatts
    if (estAmps > peakAmps) peakAmps = estAmps
    if (telemetry.speedKmh > peakSpeedKmh) peakSpeedKmh = telemetry.speedKmh
  }

  val tripAKm = (telemetry.tripDistanceKm - tripABaseKm).coerceAtLeast(0.0)
  val tripBKm = (telemetry.tripDistanceKm - tripBBaseKm).coerceAtLeast(0.0)
  val dailyGoalKm = settings.dailyDistanceGoalKm.toDouble().coerceAtLeast(1.0)
  val goalProgress = (telemetry.tripDistanceKm / dailyGoalKm).toFloat().coerceIn(0f, 1f)

  TacticalCard(highlight = estMotorTempC >= 85.0) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(6.dp))
        Text("PEV RIDE MODES & LIVE POWER MONITOR", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
      }
      Text(
        thermalLabel,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = thermalColor,
      )
    }

    Spacer(Modifier.height(6.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(rideModes) { mode ->
        FilterChip(
          selected = activeMode == mode,
          onClick = { onSelectRideMode(mode) },
          label = { Text(mode, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
          modifier = Modifier.testTag("ride_mode_${mode.lowercase()}"),
        )
      }
    }
    Text(
      "$activeMode Profile: $speedCapHint • Storage Target: ${"%.1f".format(storageVolts)}V (${seriesCount}S) • Cold-Comp Tire: ${"%.0f".format(recommendedPsi)} PSI",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = if (win95) RectangleShape else MaterialTheme.shapes.small,
        modifier = Modifier.weight(1f).then(if (win95) Modifier.windowsClassicBevel(sunken = true) else Modifier),
      ) {
        Column(Modifier.padding(8.dp)) {
          Text("EST POWER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("${estWatts.roundToInt()} W", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
          Text("Peak ${peakWatts.roundToInt()} W", style = MaterialTheme.typography.labelSmall)
        }
      }
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = if (win95) RectangleShape else MaterialTheme.shapes.small,
        modifier = Modifier.weight(1f).then(if (win95) Modifier.windowsClassicBevel(sunken = true) else Modifier),
      ) {
        Column(Modifier.padding(8.dp)) {
          Text("DRAW / C-RATE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("${"%.1f".format(estAmps)} A", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
          Text("${"%.2f".format(estCRate)}C • Pk ${"%.0f".format(peakAmps)}A", style = MaterialTheme.typography.labelSmall)
        }
      }
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = if (win95) RectangleShape else MaterialTheme.shapes.small,
        modifier = Modifier.weight(1f).then(if (win95) Modifier.windowsClassicBevel(sunken = true) else Modifier),
      ) {
        Column(Modifier.padding(8.dp)) {
          Text("MOTOR TEMP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("${estMotorTempC.roundToInt()}°C", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = thermalColor)
          Text("Pk ${"%.1f".format(formatSpeedValue(peakSpeedKmh, settings.distanceUnit))} ${speedUnitLabel(settings.distanceUnit)}", style = MaterialTheme.typography.labelSmall)
        }
      }
    }

    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text(
        "DAILY GOAL: ${"%.1f".format(formatDistValue(telemetry.tripDistanceKm, settings.distanceUnit))} / ${"%.1f".format(formatDistValue(dailyGoalKm, settings.distanceUnit))} ${distUnitLabel(settings.distanceUnit)} (${(goalProgress * 100).roundToInt()}%)",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
      )
      Text(
        "Trip A: ${"%.2f".format(formatDistValue(tripAKm, settings.distanceUnit))} • Trip B: ${"%.2f".format(formatDistValue(tripBKm, settings.distanceUnit))}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Spacer(Modifier.height(4.dp))
    LinearProgressIndicator(
      progress = { goalProgress },
      modifier = Modifier.fillMaxWidth().height(6.dp),
    )

    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      TacticalOutlinedButton(
        onClick = { tripABaseKm = telemetry.tripDistanceKm },
        modifier = Modifier.weight(1f).testTag("reset_trip_a_button"),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
      ) {
        Text("Reset A", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall)
      }
      TacticalOutlinedButton(
        onClick = { tripBBaseKm = telemetry.tripDistanceKm },
        modifier = Modifier.weight(1f).testTag("reset_trip_b_button"),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
      ) {
        Text("Reset B", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall)
      }
      TacticalOutlinedButton(
        onClick = {
          peakWatts = 0.0
          peakAmps = 0.0
          peakSpeedKmh = 0.0
        },
        modifier = Modifier.weight(1f).testTag("reset_peaks_button"),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
      ) {
        Text("Clear Peaks", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall)
      }
    }
  }
}

@Composable
fun RuralTrailReadinessCard(
  telemetry: LiveTelemetrySnapshot,
  vehicle: PevVehicle?,
  weatherState: UiState<WeatherMatrixData>,
  settings: MasterSettings,
) {
  val weather = (weatherState as? UiState.Success)?.data
  val precipPct = weather?.precipProbPct ?: 0
  val humidityPct = weather?.humidityPct ?: 50
  val windKmh = weather?.windSpeedKmh ?: 0.0
  val sunsetStr = weather?.sunset?.substringAfter("T", weather.sunset)?.ifBlank { "--:--" } ?: "--:--"
  val sunriseStr = weather?.sunrise?.substringAfter("T", weather.sunrise)?.ifBlank { "--:--" } ?: "--:--"

  // Live rural dirt/gravel traction score (0-100) from real Open-Meteo rain, humidity, wind
  val gripScore =
    (100 - (precipPct * 0.65).roundToInt() - ((humidityPct - 55).coerceAtLeast(0) * 0.35).roundToInt() - (windKmh * 0.25).roundToInt())
      .coerceIn(15, 100)
  val gripLabel =
    when {
      gripScore >= 78 -> "DRY GRAVEL • HIGH GRIP"
      gripScore >= 52 -> "DAMP DIRT • WATCH CORNERS"
      else -> "MUDDY / SLICK • DROP 3 PSI"
    }
  val gripColor =
    when {
      gripScore >= 78 -> TacticalColors.NeonLime
      gripScore >= 52 -> TacticalColors.SolarAmber
      else -> MaterialTheme.colorScheme.error
    }

  // Live off-grid 100W portable solar top-up estimator for active PEV pack
  val packWh = (vehicle?.batteryWh ?: 1200L).toDouble().coerceAtLeast(200.0)
  val missingPct = (100 - telemetry.batteryPct).coerceIn(0, 100)
  val whNeeded = packWh * (missingPct / 100.0)
  val solarEffectiveW = if (precipPct > 50) 35.0 else 78.0
  val solarHrs = if (whNeeded <= 5.0) 0.0 else (whNeeded / solarEffectiveW)
  val cardinal = PevRtbMath.cardinalLabel(telemetry.bearingDeg)
  val altText =
    if (settings.distanceUnit == "mi") "${(telemetry.altitudeM * 3.28084).roundToInt()} ft"
    else "${telemetry.altitudeM.roundToInt()} m"

  TacticalCard(title = "Trail Readiness & Conditions") {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(RuralPevIcons.CountryHills, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text("BACKCOUNTRY & RURAL TRAIL READINESS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
      }
      Text(
        "$cardinal ${telemetry.bearingDeg.toInt()}°",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
      )
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.weight(1f),
      ) {
        Column(Modifier.padding(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(RuralPevIcons.GravelTerrain, contentDescription = null, tint = gripColor, modifier = Modifier.size(14.dp))
            Text("GRAVEL GRIP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Text("$gripScore/100", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = gripColor)
          Text(gripLabel.substringBefore(" •"), style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
      }
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.weight(1f),
      ) {
        Column(Modifier.padding(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(RuralPevIcons.SunsetDaylight, contentDescription = null, tint = TacticalColors.SolarAmber, modifier = Modifier.size(14.dp))
            Text("DAYLIGHT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Text(sunsetStr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TacticalColors.SolarAmber)
          Text("Rise $sunriseStr", style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
      }
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.weight(1f),
      ) {
        Column(Modifier.padding(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(RuralPevIcons.OffGridSolar, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
            Text("100W SOLAR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Text(
            if (solarHrs <= 0.05) "FULL" else "${"%.1f".format(solarHrs)}h",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
          )
          Text("Elev $altText", style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
      }
    }
    Spacer(Modifier.height(6.dp))
    Text(
      "$gripLabel • Active Rig: ${vehicle?.name?.ifBlank { "PEV Unit" } ?: "PEV Unit"} (${packWh.toInt()}Wh)",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}


