package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.PevExpansionVaultData
import com.example.ui.components.SocialSharePayload
import com.example.ui.components.TacticalCard
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

private data class CalcEntry(val id: Int, val label: String, val category: String)

/**
 * 20 Interactive PEV Engineering & Ride Calculators inside PEV Pro Suite, organized by Category.
 */
@Composable
fun PevProCalculatorsHub(
  onShareResult: (SocialSharePayload) -> Unit,
) {
  val allCalcs =
    remember {
      listOf(
        CalcEntry(0, "Pack Builder", "Battery & Power"),
        CalcEntry(1, "Charge 80%", "Battery & Power"),
        CalcEntry(7, "AWG & Plugs", "Battery & Power"),
        CalcEntry(17, "Cell IR", "Battery & Power"),
        CalcEntry(3, "Hill & Watts", "Drivetrain & Speed"),
        CalcEntry(6, "Gear / Speed", "Drivetrain & Speed"),
        CalcEntry(12, "PWM Safety", "Drivetrain & Speed"),
        CalcEntry(18, "Dual Split", "Drivetrain & Speed"),
        CalcEntry(2, "Tire PSI", "Brakes & Chassis"),
        CalcEntry(5, "Braking & Rotor", "Brakes & Chassis"),
        CalcEntry(8, "Suspension Sag", "Brakes & Chassis"),
        CalcEntry(13, "Torque Matrix", "Brakes & Chassis"),
        CalcEntry(16, "Wobble Tune", "Brakes & Chassis"),
        CalcEntry(4, "Cold & Wind", "Range & Trip"),
        CalcEntry(9, "Commute ROI", "Range & Trip"),
        CalcEntry(10, "Unit Pro", "Range & Trip"),
        CalcEntry(11, "Trip Legs", "Range & Trip"),
        CalcEntry(14, "Fire Audit", "Safety & Audit"),
        CalcEntry(15, "IP Decoder", "Safety & Audit"),
        CalcEntry(19, "PEV Passport", "Safety & Audit"),
      )
    }
  val categories = remember { listOf("All", "Battery & Power", "Drivetrain & Speed", "Brakes & Chassis", "Range & Trip", "Safety & Audit") }
  var selectedCategory by remember { mutableStateOf("All") }
  var selectedCalc by remember { mutableIntStateOf(0) }
  val visibleCalcs = remember(selectedCategory) {
    if (selectedCategory == "All") allCalcs.sortedBy { it.id } else allCalcs.filter { it.category == selectedCategory }
  }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      "20 LIVE PEV ENGINEERING CALCULATORS BY CATEGORY",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.primary,
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(categories) { cat ->
        val count = if (cat == "All") allCalcs.size else allCalcs.count { it.category == cat }
        FilterChip(
          selected = selectedCategory == cat,
          onClick = {
            selectedCategory = cat
            val firstMatch = if (cat == "All") allCalcs.minByOrNull { it.id } else allCalcs.firstOrNull { it.category == cat }
            if (firstMatch != null && (cat == "All" || allCalcs.firstOrNull { it.id == selectedCalc }?.category != cat)) {
              selectedCalc = firstMatch.id
            }
          },
          label = { Text("$cat ($count)", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
        )
      }
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(visibleCalcs, key = { it.id }) { entry ->
        FilterChip(
          selected = selectedCalc == entry.id,
          onClick = { selectedCalc = entry.id },
          label = { Text("${entry.id + 1}. ${entry.label}", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
        )
      }
    }

    when (selectedCalc) {
      0 -> BatteryPackBuilderCalc(onShareResult)
      1 -> ChargeLongevityCalc()
      2 -> TirePsiOptimizerCalc()
      3 -> HillClimbWattsCalc()
      4 -> ColdWindRangeCalc()
      5 -> BrakingRotorCalc()
      6 -> GearSpeedRatioCalc()
      7 -> AwgWireDropCalc()
      8 -> SuspensionSagCalc()
      9 -> CommuteRoiCalc(onShareResult)
      10 -> UnitConverterProCalc()
      11 -> MultiLegTripCalc()
      12 -> PwmCutoutMarginCalc()
      13 -> BoltTorqueMatrixTool()
      14 -> LiIonFireAuditTool()
      15 -> IpRatingDecoderTool()
      16 -> SpeedWobbleAdvisorTool()
      17 -> CellIrHealthTool()
      18 -> DualMotorSplitTool()
      else -> StolenPevPassportTool(onShareResult)
    }
  }
}

@Composable
private fun BatteryPackBuilderCalc(onShareResult: (SocialSharePayload) -> Unit) {
  var cellIdx by remember { mutableIntStateOf(0) }
  var seriesStr by remember { mutableStateOf("20") }
  var parallelStr by remember { mutableStateOf("6") }
  val cell = PevExpansionVaultData.BATTERY_CELLS[cellIdx]
  val s = (seriesStr.toIntOrNull() ?: 20).coerceIn(4, 48)
  val p = (parallelStr.toIntOrNull() ?: 6).coerceIn(1, 20)
  val chem =
    when {
      cell.nominalV <= 3.35 -> "LFP"
      cell.nominalV <= 3.64 -> "NMC_36"
      else -> "NMC"
    }
  val estCellIrMohm =
    when {
      cell.maxContA >= 40.0 -> 13.0 // P45B / 50S tabless-grade low IR
      cell.maxContA >= 25.0 -> 18.0
      cell.nominalV <= 3.35 -> 24.0
      else -> 28.0 // High-capacity 50E / 35E
    }
  val nomV = s * cell.nominalV
  val fullV = s * cell.fullV
  val cutV = s * cell.cutoffV
  val ah = p * cell.capacityAh
  val wh = nomV * ah
  val maxA = p * cell.maxContA
  val peakKw = (nomV * maxA) / 1000.0
  val wtLbs = (s * p * cell.weightGrams * 1.12) / 453.592
  val cruiseAmps = (maxA * 0.40).coerceAtLeast(15.0)
  val simCruise =
    com.example.core.PevBatteryPhysics.simulateUnderLoadDischarge(
      seriesCells = s,
      parallelCells = p.toDouble(),
      cellCapacityAh = cell.capacityAh,
      loadAmps = cruiseAmps,
      socPct = 50.0,
      baseCellIrMohm = estCellIrMohm,
      chemistry = chem,
    )
  val simMax =
    com.example.core.PevBatteryPhysics.simulateUnderLoadDischarge(
      seriesCells = s,
      parallelCells = p.toDouble(),
      cellCapacityAh = cell.capacityAh,
      loadAmps = maxA * 0.75,
      socPct = 50.0,
      baseCellIrMohm = estCellIrMohm,
      chemistry = chem,
    )
  val recBmsAmps = ((maxA * 0.85) / 10.0).roundToInt() * 10

  TacticalCard(highlight = true) {
    Text("1. CUSTOM PEV BATTERY PACK BUILDER (${s}S${p}P)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(6.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(PevExpansionVaultData.BATTERY_CELLS.indices.toList()) { i ->
        FilterChip(
          selected = cellIdx == i,
          onClick = { cellIdx = i },
          label = { Text(PevExpansionVaultData.BATTERY_CELLS[i].name, style = MaterialTheme.typography.labelSmall) },
        )
      }
    }
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(value = seriesStr, onValueChange = { seriesStr = it }, label = { Text("Series (S)") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = parallelStr, onValueChange = { parallelStr = it }, label = { Text("Parallel (P)") }, singleLine = true, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    val summary =
      "${s}S${p}P ${cell.name} (${s * p} cells)\n" +
        "Voltage: ${"%.1f".format(nomV)}V Nom | ${"%.1f".format(fullV)}V Full | ${"%.1f".format(cutV)}V Empty\n" +
        "Energy: ${"%.1f".format(ah)} Ah | ${wh.roundToInt()} Wh Nameplate -> ${simCruise.usableWh.roundToInt()} Wh Usable @ ${cruiseAmps.roundToInt()}A (${"%.0f".format(simCruise.peukertRetentionPct)}% Peukert)\n" +
        "Max Continuous: ${maxA.roundToInt()} A (${"%.1f".format(peakKw)} kW) • Rec Smart BMS: ${recBmsAmps}A\n" +
        "50% SoC Sag @ ${"%.0f".format(maxA * 0.75)}A: -${"%.1f".format(simMax.sagVolts)}V (Load ${"%.1f".format(simMax.underLoadPackV)}V) | Weight: ${"%.1f".format(wtLbs)} lb"
    Text(summary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(6.dp))
    OutlinedButton(onClick = { onShareResult(SocialSharePayload("Custom ${s}S${p}P PEV Battery Build", summary)) }) {
      Text("Share Pack Spec")
    }
  }
}

@Composable
private fun ChargeLongevityCalc() {
  var whStr by remember { mutableStateOf("2400") }
  var voltStr by remember { mutableStateOf("72") }
  var ampStr by remember { mutableStateOf("5") }
  var startPct by remember { mutableStateOf("20") }
  var targetPct by remember { mutableStateOf("80") }
  var kwhRate by remember { mutableStateOf("0.15") }
  var chemistry by remember { mutableStateOf("NMC") }

  val wh = (whStr.toDoubleOrNull() ?: 2400.0).coerceAtLeast(100.0)
  val v = (voltStr.toDoubleOrNull() ?: 72.0).coerceAtLeast(24.0)
  val a = (ampStr.toDoubleOrNull() ?: 5.0).coerceAtLeast(0.5)
  val sPct = (startPct.toDoubleOrNull() ?: 20.0).coerceIn(0.0, 99.0)
  val tPct = (targetPct.toDoubleOrNull() ?: 80.0).coerceIn(sPct + 1.0, 100.0)
  val rate = (kwhRate.toDoubleOrNull() ?: 0.15)

  val profile = com.example.core.PevBatteryPhysics.packProfile(v, chemistry)
  val chargerSpec = com.example.core.PevBatteryPhysics.estimateChargerWatts(v, a, (sPct + tPct) / 2.0, chemistry)
  val hours =
    com.example.core.PevBatteryPhysics.estimateChargeTimeHours(
      batteryWh = wh,
      ratedVoltage = v,
      chargerAmps = a,
      startSocPct = sPct,
      targetSocPct = tPct,
      chemistry = chemistry,
    )
  val costUsd =
    com.example.core.PevBatteryPhysics.estimateChargeCostUsd(
      batteryWh = wh,
      startSocPct = sPct,
      targetSocPct = tPct,
      kwhRateUsd = rate,
    )
  val targetVolts = com.example.core.PevBatteryPhysics.voltageForSeriesSoc(profile.seriesCells, tPct, chemistry)
  val packAh = com.example.core.PevBatteryPhysics.computePackAh(wh, v, chemistry)
  val chgCRate = a / packAh.coerceAtLeast(1.0)
  val cycleBoost = when {
    tPct <= 80.0 -> "2.8x–3.5x Cycle Life (Best Daily Target!)"
    tPct <= 90.0 -> "1.8x–2.2x Cycle Life"
    else -> "1.0x Standard (Full ${"%.1f".format(profile.fullChargeV)}V balance)"
  }

  TacticalCard(highlight = true) {
    Text("2. CHARGE TIME & 80% LONGEVITY PLANNER", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(6.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(listOf("NMC" to "NMC 3.7V", "NMC_36" to "50S/50E 3.6V", "LFP" to "LiFePO4 3.2V")) { (chemId, label) ->
        FilterChip(selected = chemistry == chemId, onClick = { chemistry = chemId }, label = { Text(label, maxLines = 1, softWrap = false) })
      }
    }
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      OutlinedTextField(value = whStr, onValueChange = { whStr = it }, label = { Text("Pack Wh") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = voltStr, onValueChange = { voltStr = it }, label = { Text("Volts") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = ampStr, onValueChange = { ampStr = it }, label = { Text("Chg A") }, singleLine = true, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      OutlinedTextField(value = startPct, onValueChange = { startPct = it }, label = { Text("Start %") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = targetPct, onValueChange = { targetPct = it }, label = { Text("Stop %") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = kwhRate, onValueChange = { kwhRate = it }, label = { Text("$/kWh") }, singleLine = true, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Text(
      "CC+CV Time (${"%.2f".format(chgCRate)}C): ${"%.1f".format(hours)} hrs (${(hours * 60).roundToInt()} min) • Stop @ ${"%.1f".format(targetVolts)}V (${profile.seriesCount}S ${profile.chemistry})",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.primary,
    )
    Text(
      "Charger Power: ~${chargerSpec.activeDcWatts.roundToInt()}W avg / ${chargerSpec.peakDcWatts.roundToInt()}W peak (${chargerSpec.wallAcWatts.roundToInt()}W AC @ ${"%.1f".format(chargerSpec.wallAmps120v)}A) • Wall Cost: $${"%.2f".format(costUsd)} | $cycleBoost",
      style = MaterialTheme.typography.bodySmall,
    )
  }
}

@Composable
private fun TirePsiOptimizerCalc() {
  var pType by remember { mutableStateOf("SCOOTER") }
  var surface by remember { mutableStateOf("DRY") }
  var riderLbsStr by remember { mutableStateOf("180") }
  val lbs = (riderLbsStr.toDoubleOrNull() ?: 180.0).coerceIn(80.0, 380.0)
  val baseFront =
    when (pType) {
      "SCOOTER" -> 38.0 + (lbs - 160.0) * 0.08
      "EBIKE_FAT" -> 18.0 + (lbs - 160.0) * 0.05
      "EUC" -> 28.0 + (lbs - 160.0) * 0.09
      "ONEWHEEL" -> lbs / 10.0
      "EMOTO" -> 22.0 + (lbs - 160.0) * 0.04
      else -> 36.0
    }
  val surfDelta = when (surface) {
    "WET" -> -3.5
    "GRAVEL" -> -5.0
    "SAND_SNOW" -> -8.0
    else -> 0.0
  }
  val recF = (baseFront + surfDelta).coerceAtLeast(8.0)
  val recR = (recF + if (pType == "SCOOTER" || pType == "EBIKE_FAT") 3.0 else 1.5)

  TacticalCard(highlight = true) {
    Text("3. TIRE PSI & TRACTION OPTIMIZER", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(6.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(listOf("SCOOTER", "EBIKE_FAT", "EUC", "ONEWHEEL", "EMOTO")) { t ->
        FilterChip(selected = pType == t, onClick = { pType = t }, label = { Text(t, style = MaterialTheme.typography.labelSmall) })
      }
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(listOf("DRY", "WET", "GRAVEL", "SAND_SNOW")) { s ->
        FilterChip(selected = surface == s, onClick = { surface = s }, label = { Text(s, style = MaterialTheme.typography.labelSmall) })
      }
    }
    OutlinedTextField(value = riderLbsStr, onValueChange = { riderLbsStr = it }, label = { Text("Rider + Gear Weight (lbs)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    Text(
      if (pType == "EUC" || pType == "ONEWHEEL") "Recommended Tire Pressure: ${recF.roundToInt()} PSI (${"%.2f".format(recF * 0.06895)} BAR)"
      else "Front: ${recF.roundToInt()} PSI (${"%.2f".format(recF * 0.06895)} BAR) | Rear: ${recR.roundToInt()} PSI (${"%.2f".format(recR * 0.06895)} BAR)",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.primary,
    )
    Text("Always check cold PSI before riding; never exceed molded sidewall max.", style = MaterialTheme.typography.labelSmall)
  }
}

@Composable
private fun HillClimbWattsCalc() {
  var gradeStr by remember { mutableStateOf("12") }
  var totalLbsStr by remember { mutableStateOf("260") }
  var mphStr by remember { mutableStateOf("18") }
  var voltStr by remember { mutableStateOf("60") }

  val grade = (gradeStr.toDoubleOrNull() ?: 12.0).coerceIn(0.0, 45.0) / 100.0
  val theta = atan(grade)
  val massKg = (totalLbsStr.toDoubleOrNull() ?: 260.0).coerceAtLeast(60.0) * 0.453592
  val speedMs = (mphStr.toDoubleOrNull() ?: 18.0).coerceIn(1.0, 75.0) * 0.44704
  val volts = (voltStr.toDoubleOrNull() ?: 60.0).coerceAtLeast(24.0)

  val gravityW = massKg * 9.81 * sin(theta) * speedMs
  val rollW = massKg * 9.81 * cos(theta) * 0.015 * speedMs
  val aeroW = 0.5 * 1.225 * 0.58 * speedMs.pow(3)
  val totalW = (gravityW + rollW + aeroW) / 0.84
  // Solve quadratic for battery amps under sag: I * (V_nom - I * R_pack) = totalW
  val profile = com.example.core.PevBatteryPhysics.packProfile(volts, "NMC")
  val rPackOhms = (profile.seriesCells * 0.022 * 1.12) / 5.0
  val estVLoad = (profile.nominalPackV - (totalW / profile.nominalPackV) * rPackOhms).coerceAtLeast(profile.cutoffEmptyV)
  val ampsNominal = totalW / profile.nominalPackV
  val ampsSagged = totalW / estVLoad
  val thermal = when {
    totalW > 4500 -> "CRITICAL HEAT — Limit climb to <2 min or use dual-motor / mid-drive"
    totalW > 2200 -> "MODERATE LOAD — Monitor motor stator & controller temps"
    else -> "SAFE CONTINUOUS CLIMB — Within typical PEV thermal envelope"
  }

  TacticalCard(highlight = true) {
    Text("4. HILL CLIMB WATTS & MOTOR THERMAL ESTIMATOR", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      OutlinedTextField(value = gradeStr, onValueChange = { gradeStr = it }, label = { Text("Grade %") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = totalLbsStr, onValueChange = { totalLbsStr = it }, label = { Text("Total lb") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = mphStr, onValueChange = { mphStr = it }, label = { Text("MPH") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = voltStr, onValueChange = { voltStr = it }, label = { Text("Volts") }, singleLine = true, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Text(
      "Power: ${totalW.roundToInt()} W • Under-Load Draw: ${"%.1f".format(ampsSagged)} A @ ${"%.1f".format(estVLoad)}V sagged (${"%.1f".format(ampsNominal)} A @ ${"%.1f".format(profile.nominalPackV)}V nom)",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.primary,
    )
    Text("Thermal Advisory: $thermal", style = MaterialTheme.typography.bodySmall)
  }
}

@Composable
private fun ColdWindRangeCalc() {
  var baseMiStr by remember { mutableStateOf("45") }
  var tempFStr by remember { mutableStateOf("38") }
  var headwindMphStr by remember { mutableStateOf("14") }
  var chem by remember { mutableStateOf("NMC") }

  val baseMi = (baseMiStr.toDoubleOrNull() ?: 45.0).coerceAtLeast(5.0)
  val tempF = (tempFStr.toDoubleOrNull() ?: 38.0)
  val tempC = (tempF - 32.0) * 5.0 / 9.0
  val wind = (headwindMphStr.toDoubleOrNull() ?: 14.0).coerceAtLeast(0.0)
  // Continuous chemistry-weighted electrolyte cold retention from PevBatteryPhysics
  val coldRetention = com.example.core.PevBatteryPhysics.coldCapacityFactor(tempC, chem)
  val coldPen = (1.0 - coldRetention).coerceIn(0.0, 0.58)
  // Real aero+rolling Wh/mi power ratio vs calm 22 mph baseline
  val refMph = 22.0
  val refAeroPower = 0.5 * 1.225 * 0.55 * (refMph * 0.44704).pow(3.0)
  val headAeroPower = 0.5 * 1.225 * 0.55 * ((refMph + wind) * 0.44704).pow(2.0) * (refMph * 0.44704)
  val refTotalW = refAeroPower + 125.0 // 125W baseline rolling + drivetrain
  val windTotalW = headAeroPower + 125.0
  val windScale = (refTotalW / windTotalW.coerceAtLeast(1.0)).coerceIn(0.35, 1.0)
  val windPen = (1.0 - windScale).coerceIn(0.0, 0.65)
  val netMi = baseMi * coldRetention * windScale

  TacticalCard(highlight = true) {
    Text("5. COLD WEATHER & HEADWIND RANGE DERATE", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      FilterChip(selected = chem == "NMC", onClick = { chem = "NMC" }, label = { Text("NMC/NCA Li-Ion") })
      FilterChip(selected = chem == "LFP", onClick = { chem = "LFP" }, label = { Text("LiFePO4 (LFP)") })
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      OutlinedTextField(value = baseMiStr, onValueChange = { baseMiStr = it }, label = { Text("70°F Mi") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = tempFStr, onValueChange = { tempFStr = it }, label = { Text("Temp °F") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = headwindMphStr, onValueChange = { headwindMphStr = it }, label = { Text("Wind MPH") }, singleLine = true, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Text(
      "Real-World Adjusted Range: ${"%.1f".format(netMi)} mi (Cold -${(coldPen * 100).roundToInt()}% • Aero Wind -${(windPen * 100).roundToInt()}%)",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.primary,
    )
    Text(
      if (tempF <= 32.0) "WARNING: Below freezing (${"%.1f".format(tempC)}°C) — NEVER charge battery until warmed above 45°F!"
      else "Safe charging window (${"%.1f".format(tempC)}°C • IR x${"%.2f".format(com.example.core.PevBatteryPhysics.cellIrMultiplier(60.0, tempC, chem))}).",
      style = MaterialTheme.typography.bodySmall,
    )
  }
}

@Composable
private fun BrakingRotorCalc() {
  var mphStr by remember { mutableStateOf("35") }
  var lbsStr by remember { mutableStateOf("270") }
  var surface by remember { mutableStateOf("DRY") }
  val vMs = (mphStr.toDoubleOrNull() ?: 35.0) * 0.44704
  val massKg = (lbsStr.toDoubleOrNull() ?: 270.0) * 0.453592
  val mu = when (surface) { "WET" -> 0.42; "GRAVEL" -> 0.32; else -> 0.72 }
  val reactFt = vMs * 0.75 * 3.28084
  val brakeFt = (vMs * vMs) / (2.0 * mu * 9.81) * 3.28084
  val joules = 0.5 * massKg * vMs * vMs
  val recRotor = when {
    joules > 16000 -> "203mm–220mm (2.3mm thick) 4-Piston Sintered"
    joules > 8000 -> "180mm–203mm Hydraulic 4-Piston"
    else -> "140mm–160mm Hydraulic Disc"
  }

  TacticalCard(highlight = true) {
    Text("6. STOPPING DISTANCE & ROTOR SIZING", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      listOf("DRY", "WET", "GRAVEL").forEach { s ->
        FilterChip(selected = surface == s, onClick = { surface = s }, label = { Text(s) })
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      OutlinedTextField(value = mphStr, onValueChange = { mphStr = it }, label = { Text("Speed MPH") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = lbsStr, onValueChange = { lbsStr = it }, label = { Text("Total lbs") }, singleLine = true, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Text("Total Stopping Distance: ${(reactFt + brakeFt).roundToInt()} ft (React ${reactFt.roundToInt()} ft + Brake ${brakeFt.roundToInt()} ft)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    Text("Kinetic Energy: ${(joules / 1000.0).let { "%.1f".format(it) }} kJ • Rec Rotor: $recRotor", style = MaterialTheme.typography.bodySmall)
  }
}

@Composable
private fun GearSpeedRatioCalc() {
  var kvStr by remember { mutableStateOf("190") }
  var voltStr by remember { mutableStateOf("50.4") }
  var motorTeethStr by remember { mutableStateOf("15") }
  var wheelTeethStr by remember { mutableStateOf("40") }
  var wheelInStr by remember { mutableStateOf("6.5") }

  val kv = (kvStr.toDoubleOrNull() ?: 190.0).coerceAtLeast(5.0)
  val v = (voltStr.toDoubleOrNull() ?: 50.4).coerceAtLeast(12.0)
  val mt = (motorTeethStr.toDoubleOrNull() ?: 15.0).coerceAtLeast(1.0)
  val wt = (wheelTeethStr.toDoubleOrNull() ?: 40.0).coerceAtLeast(1.0)
  val diaIn = (wheelInStr.toDoubleOrNull() ?: 6.5).coerceAtLeast(2.0)

  val ratio = wt / mt
  val motorRpm = kv * v
  val wheelRpm = motorRpm / ratio
  val freeMph = (wheelRpm * PI * diaIn * 60.0) / 63360.0
  // Real loaded speed accounts for ~6% pack sag at peak back-EMF + 7% belt/air-gap slip (87.4% net vs 0.85 arbitrary)
  val sagRatio = (1.0 - com.example.core.PevBatteryPhysics.estimateVoltageSagVolts(45.0, v) / v.coerceAtLeast(12.0)).coerceIn(0.84, 0.96)
  val loadedMph = freeMph * sagRatio * 0.93

  TacticalCard(highlight = true) {
    Text("7. GEAR / BELT RATIO & TOP SPEED (eMOTO / eSKATE)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      OutlinedTextField(value = kvStr, onValueChange = { kvStr = it }, label = { Text("Motor KV") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = voltStr, onValueChange = { voltStr = it }, label = { Text("Volts") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = wheelInStr, onValueChange = { wheelInStr = it }, label = { Text("Tire in") }, singleLine = true, modifier = Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      OutlinedTextField(value = motorTeethStr, onValueChange = { motorTeethStr = it }, label = { Text("Motor T") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = wheelTeethStr, onValueChange = { wheelTeethStr = it }, label = { Text("Wheel T") }, singleLine = true, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Text(
      "Ratio: ${"%.2f".format(ratio)}:1 | Est Loaded Top Speed: ${"%.1f".format(loadedMph)} MPH (Free-Spin ${"%.1f".format(freeMph)} MPH • ${motorRpm.roundToInt()} Motor RPM)",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.primary,
    )
  }
}

@Composable
private fun AwgWireDropCalc() {
  var ampsStr by remember { mutableStateOf("80") }
  var lenFtStr by remember { mutableStateOf("3") }
  var voltsStr by remember { mutableStateOf("72") }
  var awg by remember { mutableIntStateOf(10) }
  val amps = (ampsStr.toDoubleOrNull() ?: 80.0).coerceAtLeast(1.0)
  val lenFt = (lenFtStr.toDoubleOrNull() ?: 3.0).coerceAtLeast(0.5)
  val volts = (voltsStr.toDoubleOrNull() ?: 72.0).coerceAtLeast(12.0)
  val res = com.example.core.PevBatteryPhysics.estimateWireVoltageDrop(
    amps = amps,
    oneWayFeet = lenFt,
    awg = awg,
    systemVolts = volts,
    wireTempC = 75.0,
    matedConnectorPairs = 1,
  )

  TacticalCard(highlight = true) {
    Text("8. AWG SILICONE WIRE DROP & CONNECTOR SIZER (75°C LOADED)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(listOf(6, 8, 10, 12, 14, 16)) { g ->
        FilterChip(selected = awg == g, onClick = { awg = g }, label = { Text("${g} AWG") })
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      OutlinedTextField(value = ampsStr, onValueChange = { ampsStr = it }, label = { Text("Load Amps") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = lenFtStr, onValueChange = { lenFtStr = it }, label = { Text("1-Way ft") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = voltsStr, onValueChange = { voltsStr = it }, label = { Text("Pack V") }, singleLine = true, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Text(
      "Drop w/ 1 Connector Pair: ${"%.3f".format(res.voltageDropV)} V (${"%.2f".format(res.dropPct)}%) • Heat Loss: ${"%.1f".format(res.heatLossWatts)} W",
      style = MaterialTheme.typography.bodyMedium,
      color = if (res.isDropWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
    )
    Text("Recommended Connector: ${res.recommendedConnector} • ASTM B3 copper @ 75°C", style = MaterialTheme.typography.bodySmall)
  }
}

@Composable
private fun SuspensionSagCalc() {
  var riderLbsStr by remember { mutableStateOf("185") }
  var travelMmStr by remember { mutableStateOf("120") }
  var targetSagPct by remember { mutableStateOf("25") }
  val lbs = (riderLbsStr.toDoubleOrNull() ?: 185.0)
  val travel = (travelMmStr.toDoubleOrNull() ?: 120.0)
  val sagPct = (targetSagPct.toDoubleOrNull() ?: 25.0).coerceIn(10.0, 40.0)
  val sagMm = travel * (sagPct / 100.0)
  val coilSpringRate = (lbs * 3.8).roundToInt()

  TacticalCard(highlight = true) {
    Text("9. SUSPENSION SAG & SPRING RATE CALCULATOR", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      OutlinedTextField(value = riderLbsStr, onValueChange = { riderLbsStr = it }, label = { Text("Rider lb") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = travelMmStr, onValueChange = { travelMmStr = it }, label = { Text("Travel mm") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = targetSagPct, onValueChange = { targetSagPct = it }, label = { Text("Sag %") }, singleLine = true, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Text("Target Static Sag: ${"%.1f".format(sagMm)} mm (${sagPct.roundToInt()}% of ${travel.roundToInt()} mm)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    Text("Est Scooter/eMoto Coil Spring: ~$coilSpringRate lb/in (Use 20% street, 25–30% dirt)", style = MaterialTheme.typography.bodySmall)
  }
}

@Composable
private fun CommuteRoiCalc(onShareResult: (SocialSharePayload) -> Unit) {
  var dailyMiStr by remember { mutableStateOf("14") }
  var gasStr by remember { mutableStateOf("3.65") }
  var mpgStr by remember { mutableStateOf("24") }
  var pevPriceStr by remember { mutableStateOf("1899") }

  val dailyMi = (dailyMiStr.toDoubleOrNull() ?: 14.0)
  val gas = (gasStr.toDoubleOrNull() ?: 3.65)
  val mpg = (mpgStr.toDoubleOrNull() ?: 24.0).coerceAtLeast(5.0)
  val pevCost = (pevPriceStr.toDoubleOrNull() ?: 1899.0).coerceAtLeast(200.0)
  val annualMi = dailyMi * 240.0
  val annualCarCost = (annualMi / mpg) * gas + (annualMi * 0.12)
  val annualPevCost = (annualMi * 0.030) * 0.15 + 120.0
  val annualSave = (annualCarCost - annualPevCost).coerceAtLeast(10.0)
  val paybackMonths = (pevCost / (annualSave / 12.0))
  val co2Kg = annualMi * 0.404

  TacticalCard(highlight = true) {
    Text("10. GAS CAR VS PEV COMMUTE ROI & CO₂ PAYBACK", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      OutlinedTextField(value = dailyMiStr, onValueChange = { dailyMiStr = it }, label = { Text("Mi/Day") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = gasStr, onValueChange = { gasStr = it }, label = { Text("Gas $/g") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = mpgStr, onValueChange = { mpgStr = it }, label = { Text("MPG") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = pevPriceStr, onValueChange = { pevPriceStr = it }, label = { Text("PEV $") }, singleLine = true, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    val res =
      "Annual Savings: $${annualSave.roundToInt()}/yr | PEV Payback: ${"%.1f".format(paybackMonths)} months\n" +
        "CO₂ Eliminated: ${co2Kg.roundToInt()} kg/yr (${(co2Kg / 21.7).roundToInt()} trees equivalent)"
    Text(res, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(4.dp))
    OutlinedButton(onClick = { onShareResult(SocialSharePayload("My PEV Commute ROI", res)) }) { Text("Share My Savings") }
  }
}

@Composable
private fun UnitConverterProCalc() {
  var inputStr by remember { mutableStateOf("35") }
  val v = inputStr.toDoubleOrNull() ?: 35.0
  TacticalCard(highlight = true) {
    Text("11. PEV UNIVERSAL UNIT CONVERTER PRO", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    OutlinedTextField(value = inputStr, onValueChange = { inputStr = it }, label = { Text("Enter Value") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(6.dp))
    Text("• $v MPH = ${"%.1f".format(v * 1.60934)} KM/H  |  $v KM/H = ${"%.1f".format(v * 0.621371)} MPH", style = MaterialTheme.typography.bodySmall)
    Text("• $v PSI = ${"%.2f".format(v * 0.0689476)} BAR (${(v * 6.89476).roundToInt()} kPa)", style = MaterialTheme.typography.bodySmall)
    Text("• $v Nm = ${"%.1f".format(v * 0.73756)} ft-lb  |  $v lbs = ${"%.1f".format(v * 0.453592)} kg", style = MaterialTheme.typography.bodySmall)
    Text("• $v °F = ${"%.1f".format((v - 32.0) * 5.0 / 9.0)} °C  |  $v °C = ${"%.1f".format(v * 9.0 / 5.0 + 32.0)} °F", style = MaterialTheme.typography.bodySmall)
  }
}

@Composable
private fun MultiLegTripCalc() {
  var whStr by remember { mutableStateOf("2000") }
  var burnStr by remember { mutableStateOf("32") }
  var totalMiStr by remember { mutableStateOf("68") }
  var pitMinStr by remember { mutableStateOf("45") }
  var pitWattsStr by remember { mutableStateOf("600") }
  val wh = (whStr.toDoubleOrNull() ?: 2000.0).coerceAtLeast(100.0)
  // Reserve 8% usable-energy buffer for Peukert / LVC knee protection
  val usableStartWh = wh * 0.92
  val burn = (burnStr.toDoubleOrNull() ?: 32.0).coerceAtLeast(8.0)
  val totalMi = (totalMiStr.toDoubleOrNull() ?: 68.0).coerceAtLeast(1.0)
  val pitW = (pitWattsStr.toDoubleOrNull() ?: 600.0).coerceAtLeast(50.0)
  // Net DC cell acceptance (~90% average due to CC-CV taper & BMS thermal throttling)
  val pitWh = ((pitMinStr.toDoubleOrNull() ?: 45.0).coerceAtLeast(0.0) / 60.0) * pitW * 0.90
  val neededWh = totalMi * burn
  val marginWh = (usableStartWh + pitWh) - neededWh

  TacticalCard(highlight = true) {
    Text("12. MULTI-LEG TRIP & PIT-STOP RANGE PLANNER", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      OutlinedTextField(value = whStr, onValueChange = { whStr = it }, label = { Text("Pack Wh") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = burnStr, onValueChange = { burnStr = it }, label = { Text("Wh/mi") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = totalMiStr, onValueChange = { totalMiStr = it }, label = { Text("Trip Mi") }, singleLine = true, modifier = Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      OutlinedTextField(value = pitMinStr, onValueChange = { pitMinStr = it }, label = { Text("Pit Min") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = pitWattsStr, onValueChange = { pitWattsStr = it }, label = { Text("Chg Watts") }, singleLine = true, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(6.dp))
    Text(
      if (marginWh >= 0) "GO! Arrive with +${marginWh.roundToInt()} Wh usable reserve (${"%.1f".format(marginWh / burn)} mi buffer above LVC)"
      else "NO-GO! Short by ${(-marginWh).roundToInt()} Wh — add ${((-marginWh) / (pitW * 0.90) * 60.0).roundToInt()} min charging!",
      style = MaterialTheme.typography.bodyMedium,
      color = if (marginWh >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
    )
  }
}

@Composable
private fun PwmCutoutMarginCalc() {
  var freeSpinMphStr by remember { mutableStateOf("62") }
  var battPctStr by remember { mutableStateOf("55") }
  var rideMphStr by remember { mutableStateOf("38") }
  var packVoltsStr by remember { mutableStateOf("100.8") }
  var chemistry by remember { mutableStateOf("NMC") }
  val freeMph = (freeSpinMphStr.toDoubleOrNull() ?: 62.0).coerceAtLeast(15.0)
  val pct = (battPctStr.toDoubleOrNull() ?: 55.0).coerceIn(5.0, 100.0)
  val rideMph = (rideMphStr.toDoubleOrNull() ?: 38.0).coerceAtLeast(0.0)
  val packVolts = (packVoltsStr.toDoubleOrNull() ?: 100.8).coerceAtLeast(36.0)
  // Real back-EMF ceiling scales with OCV(SoC, chemistry) minus rider load IR sag
  val estLoadAmps = (22.0 + rideMph * 0.85).coerceIn(15.0, 90.0)
  val pwm =
    com.example.core.PevBatteryPhysics.estimatePwmHeadroom(
      freeSpinMph = freeMph,
      socPct = pct,
      rideMph = rideMph,
      ratedVoltage = packVolts,
      loadAmps = estLoadAmps,
      parallelCells = 4,
      cellIrMohm = 20.0,
      chemistry = chemistry,
    )

  TacticalCard(highlight = true) {
    Text("13. EUC / ONEWHEEL PWM CUTOUT HEADROOM CALCULATOR", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      listOf("NMC" to "NMC 3.7V", "NMC_36" to "50S/50E 3.6V", "LFP" to "LFP").forEach { (id, lbl) ->
        FilterChip(selected = chemistry == id, onClick = { chemistry = id }, label = { Text(lbl, maxLines = 1, softWrap = false) })
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      OutlinedTextField(value = freeSpinMphStr, onValueChange = { freeSpinMphStr = it }, label = { Text("Freespin") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = battPctStr, onValueChange = { battPctStr = it }, label = { Text("Batt %") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = rideMphStr, onValueChange = { rideMphStr = it }, label = { Text("Ride MPH") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = packVoltsStr, onValueChange = { packVoltsStr = it }, label = { Text("Pack V") }, singleLine = true, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(6.dp))
    Text(
      "Est PWM Load: ${pwm.estPwmPct.roundToInt()}% | Safe 78% Ceiling @ ${pct.roundToInt()}% SoC (${"%.1f".format(pwm.underLoadPackV)}V loaded): ${"%.1f".format(pwm.safeCeilingMph)} MPH",
      style = MaterialTheme.typography.bodyMedium,
      color = if (pwm.isHighRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
    )
    Text(
      "Under ${estLoadAmps.roundToInt()}A rider load: ${"%.1f".format(pwm.restingPackV)}V OCV - ${"%.1f".format(pwm.sagVolts)}V sag • Effective Freespin ${"%.1f".format(pwm.freeSpinAtSocMph)} MPH",
      style = MaterialTheme.typography.bodySmall,
    )
  }
}

@Composable
private fun BoltTorqueMatrixTool() {
  val specs =
    listOf(
      "Hub Motor Axle Nuts (M12/M14)" to "45–60 Nm",
      "Scooter Split-Rim Bolts (M5/M6)" to "7–9 Nm + Blue 243",
      "Disc Brake Rotor Bolts (T25)" to "6.2 Nm + Blue 243",
      "Hydraulic Caliper Mount Bolts" to "8–10 Nm",
      "Stem Faceplate / Handlebar Clamp" to "5–6 Nm (Cross pattern)",
      "Double-Crown Fork Pinch Bolts" to "10–12 Nm",
      "Suspension Eye / Linkage Bolts" to "16–22 Nm + Blue 243",
      "eBike Crank Arm / Pedals" to "35–40 Nm",
      "EUC Pedal Hanger Bolts" to "25–30 Nm + Blue 243",
    )
  TacticalCard(highlight = true) {
    Text("14. PEV CRITICAL BOLT TORQUE MATRIX", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(4.dp))
    specs.forEach { (k, v) ->
      Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(k, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(v, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun LiIonFireAuditTool() {
  val checks =
    listOf(
      "Using genuine OEM or UL-listed smart charger matching exact pack voltage",
      "Charging away from bedroom / apartment exit doors on non-flammable surface",
      "Never charging pack below 32°F (0°C) or immediately after hot hard ride",
      "Smoke detector installed directly above charging zone",
      "Pack inspected for dents, swelling, water intrusion, or scorched XT/GX pins",
      "Unplugging after charge completes (no unattended overnight charging)",
    )
  var checked by remember { mutableStateOf(setOf<Int>()) }
  TacticalCard(highlight = true) {
    Text("15. LI-ION CHARGING FIRE SAFETY AUDIT (${checked.size}/${checks.size})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    checks.forEachIndexed { idx, txt ->
      Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = idx in checked, onCheckedChange = { checked = if (idx in checked) checked - idx else checked + idx })
        Text(txt, style = MaterialTheme.typography.bodySmall)
      }
    }
  }
}

@Composable
private fun IpRatingDecoderTool() {
  val levels =
    listOf(
      "IPX4" to "Light splashes only — avoid puddles & rain.",
      "IP54 / IP55" to "Dust-protected + low-pressure water jets; wipe dry after wet roads.",
      "IPX6" to "Heavy rain & strong road spray resistant (still avoid submerging hubs!).",
      "IP67" to "Dust-tight + 1m temporary submersion rated (common on sealed battery boxes).",
      "IP69K" to "High-pressure hot washdown sealed (Stark VARG / industrial packs).",
    )
  TacticalCard(highlight = true) {
    Text("16. PEV IP WATER INGRESS DECODER", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    levels.forEach { (code, desc) ->
      Text("• $code — $desc", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
    }
  }
}

@Composable
private fun SpeedWobbleAdvisorTool() {
  TacticalCard(highlight = true) {
    Text("17. HIGH-SPEED WOBBLE ELIMINATION CHECKLIST", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Text(
      "1. Relax white-knuckle grip & bend knees — stiff arms feed oscillation.\n" +
        "2. Shift weight slightly back over rear axle on hyper-scooters.\n" +
        "3. Verify front/rear PSI within 2 PSI & statically balance front wheel.\n" +
        "4. Torque headset bearings so zero fore-aft play exists.\n" +
        "5. Add CNC 24-click steering damper for sustained 40+ MPH stability.",
      style = MaterialTheme.typography.bodySmall,
    )
  }
}

@Composable
private fun CellIrHealthTool() {
  var vRestStr by remember { mutableStateOf("82.0") }
  var vLoadStr by remember { mutableStateOf("78.4") }
  var ampsStr by remember { mutableStateOf("50") }
  var seriesStr by remember { mutableStateOf("20") }
  var parallelStr by remember { mutableStateOf("6") }
  val vRest = vRestStr.toDoubleOrNull() ?: 82.0
  val vLoad = vLoadStr.toDoubleOrNull() ?: 78.4
  val amps = (ampsStr.toDoubleOrNull() ?: 50.0).coerceAtLeast(1.0)
  val s = (seriesStr.toIntOrNull() ?: 20).coerceIn(1, 60)
  val p = (parallelStr.toIntOrNull() ?: 6).coerceIn(1, 24)
  val diag = com.example.core.PevBatteryPhysics.diagnosePackIr(vRest, vLoad, amps, s, p)
  TacticalCard(highlight = !diag.isHealthy) {
    Text("18. PACK INTERNAL RESISTANCE (IR) DIAGNOSTIC", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      OutlinedTextField(value = vRestStr, onValueChange = { vRestStr = it }, label = { Text("Rest V") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = vLoadStr, onValueChange = { vLoadStr = it }, label = { Text("Load V") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = ampsStr, onValueChange = { ampsStr = it }, label = { Text("Amps") }, singleLine = true, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      OutlinedTextField(value = seriesStr, onValueChange = { seriesStr = it }, label = { Text("Series S") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = parallelStr, onValueChange = { parallelStr = it }, label = { Text("Parallel P") }, singleLine = true, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(6.dp))
    Text(
      "Pack IR: ${diag.packIrMohm.roundToInt()} mΩ (${diag.perCellIrMohm?.let { "${"%.1f".format(it)} mΩ/cell equiv" } ?: ""}) — ${diag.verdict}",
      style = MaterialTheme.typography.bodyMedium,
      color = if (diag.isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
    )
    Text(
      "Formula: R_cell = R_pack × P ÷ S (${s}S${p}P). High-drain 21700 cells test 12–22 mΩ; >45 mΩ indicates aging or weak nickel welds.",
      style = MaterialTheme.typography.bodySmall,
    )
  }
}

@Composable
private fun DualMotorSplitTool() {
  var totalWStr by remember { mutableStateOf("6000") }
  var rearPctStr by remember { mutableStateOf("60") }
  val totalW = totalWStr.toDoubleOrNull() ?: 6000.0
  val rPct = (rearPctStr.toDoubleOrNull() ?: 60.0).coerceIn(30.0, 90.0)
  val fPct = 100.0 - rPct
  TacticalCard(highlight = true) {
    Text("19. DUAL-MOTOR TORQUE SPLIT TUNER", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      OutlinedTextField(value = totalWStr, onValueChange = { totalWStr = it }, label = { Text("Total W") }, singleLine = true, modifier = Modifier.weight(1f))
      OutlinedTextField(value = rearPctStr, onValueChange = { rearPctStr = it }, label = { Text("Rear %") }, singleLine = true, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(6.dp))
    Text("Front Motor: ${(totalW * fPct / 100).roundToInt()}W (${fPct.roundToInt()}%) | Rear Motor: ${(totalW * rPct / 100).roundToInt()}W (${rPct.roundToInt()}%)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    Text("Tip: 40/60 Front/Rear split maximizes corner-exit traction without front wheel spinout.", style = MaterialTheme.typography.bodySmall)
  }
}

@Composable
private fun StolenPevPassportTool(onShareResult: (SocialSharePayload) -> Unit) {
  var model by remember { mutableStateOf("") }
  var frameVin by remember { mutableStateOf("") }
  var motorSerial by remember { mutableStateOf("") }
  var markings by remember { mutableStateOf("") }
  var lastSeen by remember { mutableStateOf("") }

  TacticalCard(highlight = true) {
    Text("20. STOLEN PEV RECOVERY PASSPORT & ALERT", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
    Text("Record frame & motor serial numbers and blast a 1-tap recovery alert across Facebook, X, Instagram, Reddit & local groups.", style = MaterialTheme.typography.labelSmall)
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Make / Model / Color") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = frameVin, onValueChange = { frameVin = it }, label = { Text("Frame Serial / VIN") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = motorSerial, onValueChange = { motorSerial = it }, label = { Text("Motor / Battery Serial") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = markings, onValueChange = { markings = it }, label = { Text("Custom Mods / Stickers / Marks") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = lastSeen, onValueChange = { lastSeen = it }, label = { Text("Last Seen Location / Reward") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(6.dp))
    OutlinedButton(
      onClick = {
        val alert =
          "STOLEN PEV ALERT!\nModel: ${model.ifBlank { "Electric PEV" }}\nFrame VIN: ${frameVin.ifBlank { "N/A" }}\nMotor/Batt Serial: ${motorSerial.ifBlank { "N/A" }}\nIdentifying Marks: ${markings.ifBlank { "N/A" }}\nLast Seen: ${lastSeen.ifBlank { "Local Area" }}\nPlease share & DM if spotted!"
        onShareResult(SocialSharePayload("STOLEN PEV ALERT: ${model.ifBlank { "Please Share" }}", alert))
      },
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text("Broadcast Stolen PEV Alert to Social Media")
    }
  }
}
