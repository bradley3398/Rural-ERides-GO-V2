package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.UiState
import com.example.ui.components.TacticalButton
import com.example.ui.components.TacticalCard
import com.example.ui.components.TacticalFilterChip
import com.example.ui.components.TacticalOutlinedButton
import com.example.ui.components.tacticalOutlinedTextFieldColors
import com.example.ui.theme.LocalTacticalChrome
import com.example.viewmodel.MechanicViewModel
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

private data class EngBayEntry(val id: Int, val name: String, val category: String)

@Composable
fun EngineeringLabTab(
  mechanicVm: MechanicViewModel,
  aiModelId: String = com.example.ai.GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
  activeRigSummary: String = "72V 40Ah Performance PEV",
  currentOdomKm: Double = 0.0,
  distanceUnit: String = "mi",
) {
  val bayEntries =
    remember {
      listOf(
        EngBayEntry(0, "Error Decoder", "Diagnostics & AI"),
        EngBayEntry(17, "Fitment AI", "Diagnostics & AI"),
        EngBayEntry(1, "Battery & Sag", "Battery & Power"),
        EngBayEntry(2, "Pack Builder", "Battery & Power"),
        EngBayEntry(10, "Charge & Storage", "Battery & Power"),
        EngBayEntry(15, "Dual Charger & $", "Battery & Power"),
        EngBayEntry(16, "Regen Recovery", "Battery & Power"),
        EngBayEntry(3, "Powertrain & Gear", "Drivetrain & Speed"),
        EngBayEntry(8, "VESC / FOC", "Drivetrain & Speed"),
        EngBayEntry(11, "Speedo & Rollout", "Drivetrain & Speed"),
        EngBayEntry(4, "Braking & Heat", "Chassis & Aero"),
        EngBayEntry(5, "Chassis & PSI", "Chassis & Aero"),
        EngBayEntry(6, "Aero & Climb", "Chassis & Aero"),
        EngBayEntry(12, "Torque & Loctite", "Chassis & Aero"),
        EngBayEntry(7, "Wiring & Fuses", "Wiring & Legal"),
        EngBayEntry(9, "Unit Converter", "Wiring & Legal"),
        EngBayEntry(13, "Connectors & AWG", "Wiring & Legal"),
        EngBayEntry(14, "Legal Class", "Wiring & Legal"),
        EngBayEntry(18, "Maintenance", "Diagnostics & AI"),
      )
    }
  val categories = remember { listOf("All", "Diagnostics & AI", "Battery & Power", "Drivetrain & Speed", "Chassis & Aero", "Wiring & Legal") }
  var selectedCategory by rememberSaveable { mutableStateOf("All") }
  var selectedBay by rememberSaveable { mutableIntStateOf(0) }
  val visibleBays = remember(selectedCategory) {
    if (selectedCategory == "All") bayEntries.sortedBy { it.id } else bayEntries.filter { it.category == selectedCategory }
  }

  Column(Modifier.fillMaxSize()) {
    LazyRow(
      contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 2.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      items(categories) { cat ->
        val count = if (cat == "All") bayEntries.size else bayEntries.count { it.category == cat }
        TacticalFilterChip(
          selected = selectedCategory == cat,
          onClick = {
            selectedCategory = cat
            val first = if (cat == "All") bayEntries.minByOrNull { it.id } else bayEntries.firstOrNull { it.category == cat }
            if (first != null && (cat == "All" || bayEntries.firstOrNull { it.id == selectedBay }?.category != cat)) {
              selectedBay = first.id
            }
          },
          label = "$cat ($count)",
        )
      }
    }
    LazyRow(
      contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      items(visibleBays, key = { it.id }) { bay ->
        TacticalFilterChip(
          selected = selectedBay == bay.id,
          onClick = { selectedBay = bay.id },
          label = bay.name,
          modifier = Modifier.testTag("eng_bay_${bay.id}"),
        )
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      when (selectedBay) {
        0 -> item { ErrorCodeLiveDecoderCard(mechanicVm, aiModelId) }
        1 -> item { BatteryAndSagBay() }
        2 -> item { PackBuilderBay() }
        3 -> item { PowertrainBay() }
        4 -> item { BrakingThermalBay() }
        5 -> item { ChassisTireBay() }
        6 -> item { AeroClimbBay() }
        7 -> item { ElectricalWiringBay() }
        8 -> item { VescFocBay() }
        9 -> item { UnitConverterBay() }
        10 -> item { ChargeStorageSocBay() }
        11 -> item { SpeedoRolloutCalBay() }
        12 -> item { TorqueAndLoctiteBay() }
        13 -> item { ConnectorAndAwgBay() }
        14 -> item { PevLegalClassVerifierBay() }
        15 -> item { ChargeCostAndDualChargerBay() }
        16 -> item { RegenDownhillRecoveryBay() }
        17 -> item { UpgradeCompatibilityAdvisorCard(mechanicVm, activeRigSummary, aiModelId) }
        18 -> item { MaintenanceScheduleBay(mechanicVm, currentOdomKm = currentOdomKm, distanceUnit = distanceUnit) }
      }
    }
  }
}

@Composable
private fun ErrorCodeLiveDecoderCard(
  mechanicVm: MechanicViewModel,
  aiModelId: String,
) {
  val ctx = LocalContext.current
  val errorState by mechanicVm.errorCodeState.collectAsStateWithLifecycle()
  var code by rememberSaveable { mutableStateOf("") }
  var brand by rememberSaveable { mutableStateOf("Dualtron / Minimotors EY3") }
  val quickCodes =
    remember {
      listOf(
        "E07" to "Dualtron / Minimotors EY3",
        "E09" to "Dualtron / Minimotors EY3",
        "E10" to "Kaabo / Minimotors EY3",
        "Error 14" to "Segway Ninebot",
        "Error 21" to "Bafang / Ninebot BMS",
        "DRV_FAULT" to "VESC / Flipsky",
        "Code 105" to "Sur-Ron / Talaria",
      )
    }

  TacticalCard(highlight = true) {
    Text("LIVE E-CODE DIAGNOSTIC DECODER", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Text("Model: $aiModelId • Tap a quick code or enter yours below", style = MaterialTheme.typography.labelSmall)
    Spacer(Modifier.height(6.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(quickCodes) { (qc, qb) ->
        AssistChip(
          onClick = {
            code = qc
            brand = qb
            mechanicVm.lookupLiveErrorCode(qc, qb, aiModelId)
          },
          label = { Text(qc, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
        )
      }
    }
    val tfColors = tacticalOutlinedTextFieldColors()
    val chrome = LocalTacticalChrome.current
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Controller / OEM Brand") }, colors = tfColors, shape = chrome.smallShape, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Error Code (e.g. E07, E10, Error 21, ABS)") }, colors = tfColors, shape = chrome.smallShape, modifier = Modifier.fillMaxWidth().testTag("ecode_input"))
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      TacticalButton(
        onClick = {
          val cleanCode = code.trim()
          if (cleanCode.isBlank()) {
            Toast.makeText(ctx, "Enter an error code or tap a quick code above (e.g. E07)", Toast.LENGTH_SHORT).show()
          } else {
            mechanicVm.lookupLiveErrorCode(cleanCode, brand.trim().ifBlank { "Universal PEV" }, aiModelId)
          }
        },
        modifier = Modifier.weight(1f).testTag("decode_ecode_button"),
      ) { Text("Decode Live via Gemini + Web") }
      if (errorState != null || code.isNotBlank()) {
        TacticalOutlinedButton(
          onClick = {
            code = ""
            mechanicVm.clearErrorCodeResult()
          },
        ) { Text("Clear") }
      }
    }
    Spacer(Modifier.height(8.dp))
    when (val st = errorState) {
      is UiState.Loading -> CircularProgressIndicator()
      is UiState.Error -> Text(st.message, color = MaterialTheme.colorScheme.error)
      is UiState.Success -> {
        val d = st.data
        Text("Fault: ${d.fault}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text("Component: ${d.component}", style = MaterialTheme.typography.labelLarge)
        Text("Symptoms: ${d.symptoms}", style = MaterialTheme.typography.bodyMedium)
        Text("Multimeter Check: ${d.voltageCheck}", style = MaterialTheme.typography.bodyMedium)
        Text("Fix Protocol: ${d.fix}", style = MaterialTheme.typography.bodyMedium)
      }
      null -> Unit
    }
  }
}

@Composable
private fun BatteryAndSagBay() {
  var series by remember { mutableStateOf("16") }
  var parallel by remember { mutableStateOf("6") }
  var cellAh by remember { mutableStateOf("5.0") }
  var battAmps by remember { mutableStateOf("60") }
  var packIrMohm by remember { mutableStateOf("45") }
  var chargerAmps by remember { mutableStateOf("5.0") }
  var socStr by remember { mutableStateOf("80") }
  var tempCStr by remember { mutableStateOf("20") }
  var chemMode by remember { mutableStateOf("NMC_37") }

  val s = (series.toIntOrNull() ?: 16).coerceIn(4, 108)
  val p = (parallel.toDoubleOrNull() ?: 6.0).coerceAtLeast(1.0)
  val cAh = (cellAh.toDoubleOrNull() ?: 5.0).coerceAtLeast(0.5)
  val soc = (socStr.toDoubleOrNull() ?: 80.0).coerceIn(0.0, 100.0)
  val tempC = (tempCStr.toDoubleOrNull() ?: 20.0).coerceIn(-30.0, 60.0)
  val loadAmps = (battAmps.toDoubleOrNull() ?: 60.0).coerceAtLeast(0.0)
  val overrideIrMohm = (packIrMohm.toDoubleOrNull() ?: 45.0).coerceAtLeast(1.0)
  val sim =
    com.example.core.PevBatteryPhysics.simulateUnderLoadDischarge(
      seriesCells = s,
      parallelCells = p,
      cellCapacityAh = cAh,
      loadAmps = loadAmps,
      socPct = soc,
      ambientTempC = tempC,
      chemistry = chemMode,
      packIrOverrideMohm = overrideIrMohm,
    )
  val nomCell = when (chemMode) {
    "LFP" -> 3.2
    "NMC_36" -> 3.6
    else -> 3.7
  }
  val maxV = s * (if (chemMode == "LFP") 3.65 else 4.20)
  val cutoffV = s * (if (chemMode == "LFP" || chemMode == "NMC_36") 2.80 else 3.00)
  val nomV = s * nomCell
  val totalAh = p * cAh
  val chgA = (chargerAmps.toDoubleOrNull() ?: 5.0).coerceAtLeast(0.5)
  val chargeHrs =
    com.example.core.PevBatteryPhysics.estimateChargeTimeHours(
      batteryWh = sim.nameplateWh,
      ratedVoltage = nomV,
      chargerAmps = chgA,
      startSocPct = 0.0,
      targetSocPct = 100.0,
      chemistry = chemMode,
    )
  val cRateChg = chgA / totalAh.coerceAtLeast(1.0)

  TacticalCard(highlight = sim.hitsLvc) {
    Text("BAY 1: BATTERY TOPOLOGY, SAG & DISCHARGE CURVE", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      listOf("NMC_37" to "NMC 3.7V", "NMC_36" to "50S 3.6V", "LFP" to "LFP 3.2V").forEach { (id, lbl) ->
        TacticalFilterChip(selected = chemMode == id, onClick = { chemMode = id }, label = lbl)
      }
    }
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(value = series, onValueChange = { series = it }, label = { Text("Series (S)") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = parallel, onValueChange = { parallel = it }, label = { Text("Parallel (P)") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = cellAh, onValueChange = { cellAh = it }, label = { Text("Cell Ah") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = socStr, onValueChange = { socStr = it }, label = { Text("SoC %") }, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(value = battAmps, onValueChange = { battAmps = it }, label = { Text("Load A") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = packIrMohm, onValueChange = { packIrMohm = it }, label = { Text("IR (mΩ)") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = tempCStr, onValueChange = { tempCStr = it }, label = { Text("Temp °C") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = chargerAmps, onValueChange = { chargerAmps = it }, label = { Text("Chg A") }, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(10.dp))
    Text("Nominal ${"%.1f".format(nomV)}V | Full ${"%.1f".format(maxV)}V | LVC Cutoff ${"%.1f".format(cutoffV)}V", style = MaterialTheme.typography.titleMedium)
    Text(
      "Nameplate: ${"%.1f".format(totalAh)} Ah (${"%.0f".format(sim.nameplateWh)} Wh) -> Usable @ ${"%.2f".format(sim.cRate)}C & ${tempC.toInt()}°C: ${"%.0f".format(sim.usableWh)} Wh (${"%.1f".format(sim.peukertRetentionPct)}%, LVC @ ${"%.0f".format(sim.lvcCutoffSocPct)}% SoC)",
      style = MaterialTheme.typography.bodyMedium,
    )
    Text(
      "Resting @ ${soc.toInt()}%: ${"%.1f".format(sim.restingPackV)}V | Effective IR: ${"%.0f".format(sim.effectivePackIrMohm)} mΩ | Sag: -${"%.2f".format(sim.sagVolts)}V -> ${"%.1f".format(sim.underLoadPackV)}V (${"%.0f".format(sim.jouleHeatWatts)}W pack heat)${if (sim.hitsLvc) " — BMS CUTOFF RISK!" else ""}",
      color = if (sim.hitsLvc) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
    )
    Text("0–100% CC+CV Charge Time: ${"%.1f".format(chargeHrs)}h at ${"%.2f".format(cRateChg)}C (${"%.1f".format(sim.dischargeEfficiencyPct)}% terminal eff)", style = MaterialTheme.typography.bodyMedium)
  }
}

@Composable
private fun PackBuilderBay() {
  var sStr by remember { mutableStateOf("20") }
  var pStr by remember { mutableStateOf("8") }
  var cellWh by remember { mutableStateOf("18.0") }
  var whPerMi by remember { mutableStateOf("42.0") }

  val totalCells = (sStr.toIntOrNull() ?: 20) * (pStr.toIntOrNull() ?: 8)
  val totalWh = totalCells * (cellWh.toDoubleOrNull() ?: 18.0)
  val weightKg = totalCells * 0.069 * 1.18
  val rangeMi = totalWh / (whPerMi.toDoubleOrNull() ?: 42.0).coerceAtLeast(10.0)

  TacticalCard(highlight = true) {
    Text("BAY 2: CUSTOM 21700 PACK BUILDER", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(value = sStr, onValueChange = { sStr = it }, label = { Text("Series S") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = pStr, onValueChange = { pStr = it }, label = { Text("Parallel P") }, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(value = cellWh, onValueChange = { cellWh = it }, label = { Text("Wh/Cell") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = whPerMi, onValueChange = { whPerMi = it }, label = { Text("Burn Wh/mi") }, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Text("Total Cells: $totalCells | Energy: ${"%.0f".format(totalWh)} Wh", style = MaterialTheme.typography.titleMedium)
    Text("Est. Pack Mass (w/ nickel & BMS): ${"%.2f".format(weightKg)} kg (${"%.1f".format(weightKg * 2.205)} lb)", style = MaterialTheme.typography.bodyMedium)
    Text("Projected Real Trail Range: ${"%.1f".format(rangeMi)} miles", color = MaterialTheme.colorScheme.primary)
  }
}

@Composable
private fun PowertrainBay() {
  var volts by remember { mutableStateOf("72") }
  var kv by remember { mutableStateOf("45") }
  var motorTeeth by remember { mutableStateOf("14") }
  var wheelTeeth by remember { mutableStateOf("54") }
  var tireDiaIn by remember { mutableStateOf("19.0") }
  var polePairs by remember { mutableStateOf("15") }

  val v = volts.toDoubleOrNull() ?: 72.0
  val kvVal = kv.toDoubleOrNull() ?: 45.0
  val ratio = (wheelTeeth.toDoubleOrNull() ?: 54.0) / (motorTeeth.toDoubleOrNull() ?: 14.0).coerceAtLeast(1.0)
  val motorRpm = v * kvVal
  val wheelRpm = motorRpm / ratio
  val mph = wheelRpm * Math.PI * (tireDiaIn.toDoubleOrNull() ?: 19.0) * 60.0 / 63360.0
  val erpm = motorRpm * (polePairs.toDoubleOrNull() ?: 15.0)
  val kt = 9.549 / kvVal.coerceAtLeast(1.0)

  TacticalCard(highlight = true) {
    Text("BAY 3: POWERTRAIN, GEAR RATIO & TORQUE CONSTANT", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(value = volts, onValueChange = { volts = it }, label = { Text("Volts") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = kv, onValueChange = { kv = it }, label = { Text("Motor KV") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = tireDiaIn, onValueChange = { tireDiaIn = it }, label = { Text("Tire in") }, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(value = motorTeeth, onValueChange = { motorTeeth = it }, label = { Text("Pinion T") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = wheelTeeth, onValueChange = { wheelTeeth = it }, label = { Text("Sprocket T") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = polePairs, onValueChange = { polePairs = it }, label = { Text("Pole Pairs") }, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Text("Reduction ${"%.2f".format(ratio)}:1 | No-Load Speed ${"%.1f".format(mph)} MPH (${"%.1f".format(mph * 1.609)} km/h)", style = MaterialTheme.typography.titleMedium)
    Text("Motor ${motorRpm.toInt()} RPM | ERPM ${erpm.toInt()} | Torque Const Kt=${"%.3f".format(kt)} Nm/A", style = MaterialTheme.typography.bodyMedium)
  }
}

@Composable
private fun BrakingThermalBay() {
  var totalMassKg by remember { mutableStateOf("115") }
  var speedMph by remember { mutableStateOf("45") }
  var rotorMassG by remember { mutableStateOf("160") }

  val mass = totalMassKg.toDoubleOrNull() ?: 115.0
  val velMs = (speedMph.toDoubleOrNull() ?: 45.0) * 0.44704
  val keJoules = 0.5 * mass * velMs * velMs
  val rotorKg = ((rotorMassG.toDoubleOrNull() ?: 160.0) * 2.0) / 1000.0
  val deltaTempC = keJoules / (rotorKg * 500.0).coerceAtLeast(10.0)

  TacticalCard(highlight = true) {
    Text("BAY 4: KINETIC BRAKING & ROTOR THERMAL RISE", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(value = totalMassKg, onValueChange = { totalMassKg = it }, label = { Text("Rider+PEV kg") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = speedMph, onValueChange = { speedMph = it }, label = { Text("Stop MPH") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = rotorMassG, onValueChange = { rotorMassG = it }, label = { Text("Rotor g") }, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Text("Kinetic Energy Dissipated: ${"%.1f".format(keJoules / 1000.0)} kJ", style = MaterialTheme.typography.titleMedium)
    Text("Dual-Rotor Temp Spike: +${"%.1f".format(deltaTempC)}°C per hard stop", color = MaterialTheme.colorScheme.secondary)
  }
}

@Composable
private fun ChassisTireBay() {
  var riderLbs by remember { mutableStateOf("185") }
  var pevLbs by remember { mutableStateOf("82") }
  var tireWidthIn by remember { mutableStateOf("3.5") }

  val totalLbs = (riderLbs.toDoubleOrNull() ?: 185.0) + (pevLbs.toDoubleOrNull() ?: 82.0)
  val recPsi = (22.0 + (totalLbs - 180.0) * 0.08 - (tireWidthIn.toDoubleOrNull() ?: 3.5) * 1.4).coerceIn(18.0, 58.0)

  TacticalCard(highlight = true) {
    Text("BAY 5: TIRE PSI, LOAD DISTRIBUTION & SUSPENSION SAG", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(value = riderLbs, onValueChange = { riderLbs = it }, label = { Text("Rider lb") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = pevLbs, onValueChange = { pevLbs = it }, label = { Text("PEV lb") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = tireWidthIn, onValueChange = { tireWidthIn = it }, label = { Text("Tire W in") }, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Text("Target Cold Tire Pressure: ${"%.1f".format(recPsi)} PSI (${"%.2f".format(recPsi * 0.0689)} bar)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Text("Target Static Suspension Sag: 22–28% of stroke travel", style = MaterialTheme.typography.bodyMedium)
  }
}

@Composable
private fun AeroClimbBay() {
  var speedMph by remember { mutableStateOf("35") }
  var gradePct by remember { mutableStateOf("8") }
  var totalKg by remember { mutableStateOf("110") }
  var cda by remember { mutableStateOf("0.55") }

  val v = (speedMph.toDoubleOrNull() ?: 35.0) * 0.44704
  val m = totalKg.toDoubleOrNull() ?: 110.0
  val slope = ((gradePct.toDoubleOrNull() ?: 8.0) / 100.0).coerceIn(-0.5, 0.8)
  val theta = atan(slope)
  val aeroW = 0.5 * 1.225 * (cda.toDoubleOrNull() ?: 0.55) * v.pow(3.0)
  val rollW = 0.014 * m * 9.81 * cos(theta) * v
  val climbW = m * 9.81 * sin(theta) * v

  TacticalCard(highlight = true) {
    Text("BAY 6: AERO CdA & GRADE CLIMB WATTAGE", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(value = speedMph, onValueChange = { speedMph = it }, label = { Text("MPH") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = gradePct, onValueChange = { gradePct = it }, label = { Text("Grade %") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = totalKg, onValueChange = { totalKg = it }, label = { Text("Mass kg") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = cda, onValueChange = { cda = it }, label = { Text("CdA") }, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Text("Total Mechanical Demand: ${(aeroW + rollW + climbW).toInt()} W", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Text("Aero Drag: ${aeroW.toInt()}W | Rolling: ${rollW.toInt()}W | Grade Climb: ${climbW.toInt()}W", style = MaterialTheme.typography.bodyMedium)
  }
}

@Composable
private fun ElectricalWiringBay() {
  var peakAmps by remember { mutableStateOf("80") }
  var wireLenM by remember { mutableStateOf("1.5") }
  var voltsStr by remember { mutableStateOf("72") }

  val amps = (peakAmps.toDoubleOrNull() ?: 80.0).coerceAtLeast(0.0)
  val lenM = (wireLenM.toDoubleOrNull() ?: 1.5).coerceAtLeast(0.1)
  val volts = (voltsStr.toDoubleOrNull() ?: 72.0).coerceAtLeast(12.0)
  val awg =
    when {
      amps <= 20 -> 14
      amps <= 45 -> 12
      amps <= 85 -> 10
      amps <= 140 -> 8
      else -> 6
    }
  val dropRes =
    com.example.core.PevBatteryPhysics.estimateWireVoltageDrop(
      amps = amps,
      oneWayFeet = lenM * 3.28084,
      awg = awg,
      systemVolts = volts,
      wireTempC = 75.0,
      matedConnectorPairs = 1,
    )
  val breakerA = (amps * 1.25).toInt()

  TacticalCard(highlight = dropRes.isDropWarning) {
    Text("BAY 7: AWG AMPACITY, FUSE SIZING & THROTTLE ADC", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(value = peakAmps, onValueChange = { peakAmps = it }, label = { Text("Peak Amps") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = wireLenM, onValueChange = { wireLenM = it }, label = { Text("Run Length m") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = voltsStr, onValueChange = { voltsStr = it }, label = { Text("Pack V") }, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Text("Recommended Gauge & Plug: ${awg} AWG • ${dropRes.recommendedConnector}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Text(
      "Inline DC Fuse / Breaker: ${breakerA}A MIDI/ANL • 75°C Round-Trip Drop: ${"%.2f".format(dropRes.voltageDropV)}V (${"%.2f".format(dropRes.dropPct)}%, ${"%.1f".format(dropRes.heatLossWatts)}W heat)",
      style = MaterialTheme.typography.bodyMedium,
      color = if (dropRes.isDropWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
    )
    Text("Hall Throttle Curve: Idle 0.80V (Deadband 0.92V) -> WOT 4.15V", style = MaterialTheme.typography.labelSmall)
  }
}

@Composable
private fun VescFocBay() {
  var battMaxA by remember { mutableStateOf("80") }
  var phaseMaxA by remember { mutableStateOf("160") }
  var fwAmps by remember { mutableStateOf("25") }

  val bA = (battMaxA.toDoubleOrNull() ?: 80.0).coerceAtLeast(1.0)
  val pA = (phaseMaxA.toDoubleOrNull() ?: 160.0).coerceAtLeast(1.0)
  val fw = (fwAmps.toDoubleOrNull() ?: 25.0).coerceIn(0.0, pA * 0.65)
  val ratio = pA / bA
  // Field-weakening headroom saturates at ~38% speed boost due to d-axis copper heating & back-EMF ceiling
  val fwBoostPct = ((fw / pA) * 42.0).coerceIn(0.0, 38.0)
  val fwHeatMultiplier = 1.0 + (fw / pA).pow(2.0)

  TacticalCard(highlight = fw > pA * 0.35) {
    Text("BAY 8: VESC / FOC PHASE RATIO & FIELD WEAKENING", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(value = battMaxA, onValueChange = { battMaxA = it }, label = { Text("Batt Max A") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = phaseMaxA, onValueChange = { phaseMaxA = it }, label = { Text("Motor Phase A") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = fwAmps, onValueChange = { fwAmps = it }, label = { Text("FW Amps") }, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Text("Phase/Battery Ratio: ${"%.2f".format(ratio)}x • Recommended Duty Cutoff: 85%–90%", style = MaterialTheme.typography.titleMedium)
    Text(
      "Field Weakening Top-End Gain: +${"%.1f".format(fwBoostPct)}% Speed Headroom (Stator Heat x${"%.2f".format(fwHeatMultiplier)})",
      color = if (fw > pA * 0.35) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
    )
  }
}

@Composable
private fun UnitConverterBay() {
  var valStr by remember { mutableStateOf("50") }
  val v = valStr.toDoubleOrNull() ?: 0.0

  TacticalCard(highlight = true) {
    Text("BAY 9: UNIVERSAL PEV UNIT CONVERTER", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(value = valStr, onValueChange = { valStr = it }, label = { Text("Input Value") }, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    Text("${v} km/h = ${"%.2f".format(v * 0.621371)} mph  |  ${v} mph = ${"%.2f".format(v * 1.60934)} km/h", style = MaterialTheme.typography.bodyMedium)
    Text("${v} Nm = ${"%.2f".format(v * 0.73756)} ft-lb  |  ${v} PSI = ${"%.2f".format(v * 0.06895)} bar", style = MaterialTheme.typography.bodyMedium)
    Text("${v} °C = ${"%.1f".format(v * 9.0 / 5.0 + 32.0)} °F  |  ${v} °F = ${"%.1f".format((v - 32.0) * 5.0 / 9.0)} °C", style = MaterialTheme.typography.bodyMedium)
  }
}

@Composable
private fun ChargeStorageSocBay() {
  var seriesStr by remember { mutableStateOf("20") }
  var packAhStr by remember { mutableStateOf("35.0") }
  var chargerAmpsStr by remember { mutableStateOf("8.0") }
  var targetPct by remember { mutableIntStateOf(80) }
  var chemistry by remember { mutableStateOf("NMC") }

  val s = (seriesStr.toIntOrNull() ?: 20).coerceIn(6, 48)
  val ah = (packAhStr.toDoubleOrNull() ?: 35.0).coerceAtLeast(1.0)
  val chgA = (chargerAmpsStr.toDoubleOrNull() ?: 8.0).coerceAtLeast(0.5)

  val nomCellV = when {
    chemistry == "LFP" -> 3.20
    chemistry == "NMC_36" -> 3.60
    else -> 3.70
  }
  val targetCellV = com.example.core.PevBatteryPhysics.cellVoltageForSoc(targetPct.toDouble(), chemistry)
  val targetPackV = com.example.core.PevBatteryPhysics.voltageForSeriesSoc(s, targetPct.toDouble(), chemistry)
  val maxV = s * (if (chemistry == "LFP") 3.65 else 4.20)
  val nomPackV = s * nomCellV
  val packWh = nomPackV * ah
  val acWallWatts = (maxV * chgA) / 0.88
  val acWallAmps120v = acWallWatts / 120.0
  val estHours =
    com.example.core.PevBatteryPhysics.estimateChargeTimeHours(
      batteryWh = packWh,
      ratedVoltage = nomPackV,
      chargerAmps = chgA,
      startSocPct = 0.0,
      targetSocPct = targetPct.toDouble(),
      chemistry = chemistry,
    )

  TacticalCard(highlight = true) {
    Text("BAY 10: CHARGE TARGET, STORAGE SOC & BREAKER LOAD", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(6.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(listOf("NMC" to "NMC 3.7V", "NMC_36" to "50S 3.6V", "LFP" to "LFP")) { (chemId, lbl) ->
        TacticalFilterChip(selected = chemistry == chemId, onClick = { chemistry = chemId }, label = lbl)
      }
      items(listOf(50, 80, 90, 100)) { pct ->
        TacticalFilterChip(
          selected = targetPct == pct,
          onClick = { targetPct = pct },
          label = if (pct == 50) "50% Store" else "$pct%",
        )
      }
    }
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(value = seriesStr, onValueChange = { seriesStr = it }, label = { Text("Series S") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = packAhStr, onValueChange = { packAhStr = it }, label = { Text("Pack Ah") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = chargerAmpsStr, onValueChange = { chargerAmpsStr = it }, label = { Text("Charger A") }, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Text(
      "Cutoff Voltage ($targetPct%): ${"%.1f".format(targetPackV)}V (${"%.2f".format(targetCellV)}V/cell)",
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.primary,
    )
    Text(
      "Wall Draw: ${acWallWatts.toInt()}W (${"%.1f".format(acWallAmps120v)}A @ 120V AC)" +
        if (acWallAmps120v > 12.5) " — HIGH BREAKER LOAD!" else " — Safe on 15A outlet",
      style = MaterialTheme.typography.bodyMedium,
      color = if (acWallAmps120v > 12.5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
    )
    Text("0 -> $targetPct% Est. CC+CV Time: ${"%.1f".format(estHours)}h (Peak DC ${(maxV * chgA).toInt()}W)", style = MaterialTheme.typography.labelSmall)
  }
}

@Composable
private fun SpeedoRolloutCalBay() {
  var tireDiaStr by remember { mutableStateOf("11.0") }
  var dashSpeedStr by remember { mutableStateOf("38.0") }
  var gpsSpeedStr by remember { mutableStateOf("33.5") }

  val tireIn = (tireDiaStr.toDoubleOrNull() ?: 11.0).coerceIn(4.0, 32.0)
  val dashSpd = (dashSpeedStr.toDoubleOrNull() ?: 38.0).coerceAtLeast(1.0)
  val gpsSpd = (gpsSpeedStr.toDoubleOrNull() ?: 33.5).coerceAtLeast(1.0)
  val ratio = gpsSpd / dashSpd
  val errorPct = ((dashSpd - gpsSpd) / gpsSpd) * 100.0
  val effectiveDiaIn = tireIn * ratio
  val trueRolloutMm = effectiveDiaIn * 25.4 * Math.PI

  TacticalCard(highlight = true) {
    Text("BAY 11: WHEEL ROLLOUT & CONTROLLER SPEEDO CALIBRATION", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(value = tireDiaStr, onValueChange = { tireDiaStr = it }, label = { Text("Set Tire in") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = dashSpeedStr, onValueChange = { dashSpeedStr = it }, label = { Text("Dash Spd") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = gpsSpeedStr, onValueChange = { gpsSpeedStr = it }, label = { Text("GPS Spd") }, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Text(
      "Controller Optimism: ${if (errorPct >= 0) "+" else ""}${"%.1f".format(errorPct)}% • Calibrate P06/Tire to ${"%.2f".format(effectiveDiaIn)}\"",
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.primary,
    )
    Text("True Loaded Tire Rollout: ${trueRolloutMm.toInt()} mm (circumference under rider weight)", style = MaterialTheme.typography.bodyMedium)
  }
}

@Composable
private fun UpgradeCompatibilityAdvisorCard(
  mechanicVm: MechanicViewModel,
  activeRigSummary: String,
  aiModelId: String,
) {
  val ctx = LocalContext.current
  val chrome = LocalTacticalChrome.current
  val tfColors = tacticalOutlinedTextFieldColors()
  val upgradeState by mechanicVm.upgradeCheckState.collectAsStateWithLifecycle()
  var rigSpec by remember(activeRigSummary) { mutableStateOf(activeRigSummary) }
  var plannedUpgrade by remember { mutableStateOf("Torp TC1000 / 72V 100A FOC Controller + 203mm Floating Rotors") }
  var ridingGoal by remember { mutableStateOf("Steep gravel climbs, cooler controller temps & trail reliability") }

  val quickPresets = remember {
    listOf(
      "Torp TC1000 100A FOC" to "Steep climbs & smoother low-end modulation",
      "203mm Floating Rotors + 4-Piston" to "Fade-free steep downhill braking",
      "PMT 100/55R6.5 Radial Slicks" to "High-speed asphalt cornering grip",
      "QS8-S Anti-Spark + 8AWG Harness" to "Eliminate connector melt under 120A bursts",
      "84V / 20S Molicel P45B Pack" to "Zero voltage sag & +35% trail range",
    )
  }

  TacticalCard(highlight = true) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
      Column(Modifier.weight(1f)) {
        Text(
          "BAY 17: AI UPGRADE & FITMENT ADVISOR",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Black,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          "Live Cloud Console Gemini 3.1 Flash-Lite schema analysis for voltage, amps, connectors, braking & clearance",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    Spacer(Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(quickPresets) { (presetUpgrade, presetGoal) ->
        TacticalFilterChip(
          selected = plannedUpgrade == presetUpgrade,
          onClick = {
            plannedUpgrade = presetUpgrade
            ridingGoal = presetGoal
          },
          label = presetUpgrade,
        )
      }
    }

    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
      value = rigSpec,
      onValueChange = { rigSpec = it },
      label = { Text("Active Rig Voltage / Battery / Tire Spec") },
      colors = tfColors,
      shape = chrome.smallShape,
      modifier = Modifier.fillMaxWidth().testTag("fitment_rig_input"),
    )
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
      value = plannedUpgrade,
      onValueChange = { plannedUpgrade = it },
      label = { Text("Planned Part / Controller / Pack Upgrade") },
      colors = tfColors,
      shape = chrome.smallShape,
      modifier = Modifier.fillMaxWidth().testTag("fitment_upgrade_input"),
    )
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
      value = ridingGoal,
      onValueChange = { ridingGoal = it },
      label = { Text("Riding Goal & Terrain") },
      colors = tfColors,
      shape = chrome.smallShape,
      modifier = Modifier.fillMaxWidth().testTag("fitment_goal_input"),
    )

    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      TacticalButton(
        onClick = {
          val upgradeClean = plannedUpgrade.trim()
          if (upgradeClean.isBlank()) {
            Toast.makeText(ctx, "Enter a planned upgrade part or pick a preset above", Toast.LENGTH_SHORT).show()
          } else {
            mechanicVm.checkUpgradeCompatibility(
              vehicleSummary = rigSpec.trim().ifBlank { activeRigSummary },
              plannedUpgrade = upgradeClean,
              ridingGoal = ridingGoal.trim().ifBlank { "Trail reliability & balanced range" },
              modelId = aiModelId,
            )
          }
        },
        modifier = Modifier.weight(1f).testTag("fitment_analyze_button"),
      ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Analyze Live Fitment")
      }
      if (upgradeState != null) {
        TacticalOutlinedButton(
          onClick = { mechanicVm.clearUpgradeCheck() },
          modifier = Modifier.testTag("fitment_clear_button"),
        ) {
          Text("Reset")
        }
      }
    }

    Spacer(Modifier.height(10.dp))
    when (val st = upgradeState) {
      is UiState.Loading -> {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          CircularProgressIndicator(strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
          Text("Running Gemini 3.1 Flash-Lite electrical & fitment check...", style = MaterialTheme.typography.bodySmall)
        }
      }
      is UiState.Error -> {
        Text(st.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
      }
      is UiState.Success -> {
        val r = st.data
        val scorePct = (r.compatibilityScore.coerceIn(0, 100) / 100f)
        val verdictColor =
          when {
            r.compatibilityScore >= 80 -> MaterialTheme.colorScheme.primary
            r.compatibilityScore >= 55 -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.error
          }
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          shape = chrome.smallShape,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text(
                r.verdict,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = verdictColor,
                modifier = Modifier.weight(1f),
              )
              Text(
                "${r.compatibilityScore}/100",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = verdictColor,
              )
            }
            LinearProgressIndicator(
              progress = { scorePct },
              color = verdictColor,
              trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
              modifier = Modifier.fillMaxWidth().height(6.dp),
            )
            Text("Voltage & Amps: ${r.voltageAndAmpsCheck}", style = MaterialTheme.typography.bodySmall)
            Text("Thermal & Range: ${r.thermalAndRangeImpact}", style = MaterialTheme.typography.bodySmall)
            if (r.requiredSupportingMods.isNotEmpty()) {
              Text("Required Supporting Mods:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
              r.requiredSupportingMods.forEach { mod ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp).padding(top = 2.dp))
                  Text(mod, style = MaterialTheme.typography.bodySmall)
                }
              }
            }
            if (r.warnings.isNotEmpty()) {
              Text("Safety Warnings:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
              r.warnings.forEach { warn ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                  Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp).padding(top = 2.dp))
                  Text(warn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
              }
            }
          }
        }
      }
      null -> Unit
    }
  }
}

@Composable
private fun MaintenanceScheduleBay(
  mechanicVm: MechanicViewModel,
  currentOdomKm: Double,
  distanceUnit: String,
) {
  val ctx = LocalContext.current
  val maintenanceItems by mechanicVm.maintenanceItems.collectAsStateWithLifecycle()
  val chrome = LocalTacticalChrome.current
  val useMiles = distanceUnit != "km"
  val odomDisplay = if (useMiles) currentOdomKm * 0.621371 else currentOdomKm
  val unitLbl = if (useMiles) "mi" else "km"

  TacticalCard(highlight = true, title = "PROACTIVE PEV MAINTENANCE SCHEDULE & ALERTS") {
    Text(
      "Live odometer: ${"%.1f".format(odomDisplay)} $unitLbl • Track wear intervals against your active rig mileage.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(10.dp))

    maintenanceItems.forEach { item ->
      val kmSinceService = (currentOdomKm - item.lastServicedKm).coerceAtLeast(0.0)
      val progress = (kmSinceService / item.intervalKm).coerceIn(0.0, 1.0).toFloat()
      val isDue = kmSinceService >= item.intervalKm
      val sinceDisplay = if (useMiles) kmSinceService * 0.621371 else kmSinceService
      val intervalDisplay = if (useMiles) item.intervalKm * 0.621371 else item.intervalKm
      val remainingDisplay = ((item.intervalKm - kmSinceService).coerceAtLeast(0.0)) * (if (useMiles) 0.621371 else 1.0)

      Surface(
        color = if (isDue) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = chrome.smallShape,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
      ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            AssistChip(
              onClick = {
                mechanicVm.markMaintenanceServiced(item.key, currentOdomKm)
                Toast.makeText(ctx, "Marked '${item.title}' as serviced!", Toast.LENGTH_SHORT).show()
              },
              label = { Text(if (isDue) "SERVICE DUE" else "Mark Serviced", fontWeight = FontWeight.Bold) },
            )
          }
          Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          LinearProgressIndicator(
            progress = { progress },
            color = if (isDue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().height(6.dp),
          )
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Logged: ${"%.1f".format(sinceDisplay)} / ${intervalDisplay.toInt()} $unitLbl", style = MaterialTheme.typography.labelSmall)
            Text(if (isDue) "OVERDUE" else "${remainingDisplay.toInt()} $unitLbl remaining", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (isDue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
          }
        }
      }
    }
  }
}

