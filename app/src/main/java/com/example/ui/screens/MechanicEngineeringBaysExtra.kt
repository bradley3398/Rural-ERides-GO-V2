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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.TacticalCard
import com.example.ui.theme.TacticalColors
import kotlin.math.pow

@Composable
fun TorqueAndLoctiteBay() {
  var filter by remember { mutableStateOf("ALL") }
  val categories = listOf("ALL", "WHEEL/AXLE", "BRAKES", "STEERING", "CHASSIS")
  val specs =
    listOf(
      Triple("WHEEL/AXLE", "Hub Motor Axle Nut (M12 / M14)", "45 - 65 Nm • Clean threads + torque arms seated"),
      Triple("WHEEL/AXLE", "Split Rim Perimeter Bolts (M5)", "7 - 9 Nm • Blue 243 Loctite, star pattern"),
      Triple("WHEEL/AXLE", "EUC Pedal Hanger / Motor Bolts", "28 - 35 Nm • Blue 243 Loctite, verify zero play"),
      Triple("BRAKES", "6-Bolt Brake Rotor Screws (T25 M5)", "6.0 - 6.5 Nm • Blue 243, clean rotor w/ IPA"),
      Triple("BRAKES", "Hydraulic Caliper Mount Bolts (M6)", "8 - 10 Nm • Hold lever squeezed while tightening"),
      Triple("BRAKES", "Bleed Port Screw / Banjo Bolt", "3 - 4 Nm / 5 - 6 Nm • Do NOT over-torque aluminum"),
      Triple("STEERING", "Stem Pinch / Triple-Tree Clamp (M6)", "10 - 14 Nm • Alternate bolts evenly"),
      Triple("STEERING", "Handlebar Faceplate Bolts (M5)", "5 - 6 Nm • Cross pattern, even gap top/bottom"),
      Triple("CHASSIS", "Suspension Shock Eyelet Bolts (M8)", "18 - 24 Nm • Blue 243 + grease bushings"),
      Triple("CHASSIS", "eBike Crank Arm Pinch / Bottom Bracket", "12 - 14 Nm / 40 Nm • Re-check after first 50 mi"),
    )
  val shown = specs.filter { filter == "ALL" || it.first == filter }

  TacticalCard(highlight = true) {
    Text("BAY 12: PEV TORQUE & THREADLOCKER SPEC SHEET", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Text("Use Blue 243 (medium) on removable fasteners; Red 271 only for permanent studs (requires heat).", style = MaterialTheme.typography.labelSmall)
    Spacer(Modifier.height(6.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(categories) { c ->
        FilterChip(selected = filter == c, onClick = { filter = c }, label = { Text(c, maxLines = 1, softWrap = false) })
      }
    }
    Spacer(Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      shown.forEach { (cat, part, valText) ->
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
          Column(Modifier.padding(8.dp)) {
            Text("$part [$cat]", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(valText, style = MaterialTheme.typography.bodySmall)
          }
        }
      }
    }
  }
}

@Composable
fun ConnectorAndAwgBay() {
  var ampsStr by remember { mutableStateOf("80") }
  var runFeetStr by remember { mutableStateOf("3.5") }
  var sysVoltsStr by remember { mutableStateOf("72") }
  var awgStr by remember { mutableStateOf("8") }

  val amps = (ampsStr.toDoubleOrNull() ?: 80.0).coerceAtLeast(1.0)
  val feet = (runFeetStr.toDoubleOrNull() ?: 3.5).coerceAtLeast(0.5)
  val volts = (sysVoltsStr.toDoubleOrNull() ?: 72.0).coerceAtLeast(12.0)
  val awg = (awgStr.toIntOrNull() ?: 8).coerceIn(4, 18)

  val res = com.example.core.PevBatteryPhysics.estimateWireVoltageDrop(
    amps = amps,
    oneWayFeet = feet,
    awg = awg,
    systemVolts = volts,
    wireTempC = 75.0,
    matedConnectorPairs = 1,
  )

  TacticalCard(highlight = res.isDropWarning) {
    Text("BAY 13: HIGH-AMP CONNECTOR & AWG DROP LAB", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(value = ampsStr, onValueChange = { ampsStr = it }, label = { Text("Cont Amps") }, modifier = Modifier.weight(1f).testTag("awg_amps_input"))
      OutlinedTextField(value = awgStr, onValueChange = { awgStr = it }, label = { Text("Wire AWG") }, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(value = runFeetStr, onValueChange = { runFeetStr = it }, label = { Text("1-Way Feet") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = sysVoltsStr, onValueChange = { sysVoltsStr = it }, label = { Text("Pack Volts") }, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Text("Recommended Plug: ${res.recommendedConnector}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Text(
      "75°C Drop (w/ 1 plug): ${"%.2f".format(res.voltageDropV)}V (${"%.2f".format(res.dropPct)}%) • Wire Heat Loss: ${"%.1f".format(res.heatLossWatts)}W",
      style = MaterialTheme.typography.bodyMedium,
      color = if (res.isDropWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
    )
    Spacer(Modifier.height(6.dp))
    Text(
      "Reference: XT60 (60A) • XT90-S (90A anti-spark) • QS8-S (110A/180A) • QS10-S (180A/300A) • 200°C Silicone Copper.",
      style = MaterialTheme.typography.labelSmall,
    )
  }
}

@Composable
fun PevLegalClassVerifierBay() {
  var wattsStr by remember { mutableStateOf("750") }
  var topMphStr by remember { mutableStateOf("20") }
  var hasThrottle by remember { mutableStateOf(true) }
  var hasPedals by remember { mutableStateOf(true) }

  val watts = wattsStr.toDoubleOrNull() ?: 750.0
  val mph = topMphStr.toDoubleOrNull() ?: 20.0

  val verdict =
    when {
      !hasPedals && mph <= 20.0 && watts <= 750.0 -> "STREET-LEGAL MICROMOBILITY SCOOTER / PEV (Typical US Local Path Rules)"
      !hasPedals && mph > 20.0 -> "HIGH-POWER PEV / OFF-ROAD OR MOPED CLASS — Check Municipal Speed & Helmet Laws"
      watts <= 750.0 && !hasThrottle && mph <= 20.0 -> "US FEDERAL CLASS 1 eBIKE (Pedal-Assist <= 20 MPH, <= 750W)"
      watts <= 750.0 && hasThrottle && mph <= 20.0 -> "US FEDERAL CLASS 2 eBIKE (Throttle Allowed <= 20 MPH, <= 750W)"
      watts <= 750.0 && !hasThrottle && mph <= 28.0 -> "US FEDERAL CLASS 3 eBIKE (Pedal-Assist <= 28 MPH, <= 750W)"
      else -> "OUT-OF-CLASS / OFF-ROAD eMOTO (>750W or >28 MPH) — Private Trails or Moped Registration Required"
    }

  TacticalCard(highlight = true) {
    Text("BAY 14: PEV & eBIKE LEGAL CLASS VERIFIER", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(value = wattsStr, onValueChange = { wattsStr = it }, label = { Text("Nominal Watts") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = topMphStr, onValueChange = { topMphStr = it }, label = { Text("Assisted MPH") }, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(selected = hasPedals, onClick = { hasPedals = !hasPedals }, label = { Text(if (hasPedals) "Has Pedals: YES" else "Has Pedals: NO") })
      FilterChip(selected = hasThrottle, onClick = { hasThrottle = !hasThrottle }, label = { Text(if (hasThrottle) "Throttle: YES" else "Throttle: NO") })
    }
    Spacer(Modifier.height(8.dp))
    Text(verdict, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Text(
      "Advisory only — verify state/city bike-lane, wattage, and helmet ordinances before riding on public trails.",
      style = MaterialTheme.typography.labelSmall,
    )
  }
}

@Composable
fun ChargeCostAndDualChargerBay() {
  var packWhStr by remember { mutableStateOf("2160") }
  var packVoltsStr by remember { mutableStateOf("72") }
  var charger1AStr by remember { mutableStateOf("5") }
  var charger2AStr by remember { mutableStateOf("5") }
  var kwhRateStr by remember { mutableStateOf("0.16") }
  var whPerMiStr by remember { mutableStateOf("38") }

  val packWh = (packWhStr.toDoubleOrNull() ?: 2160.0).coerceAtLeast(100.0)
  val packV = (packVoltsStr.toDoubleOrNull() ?: 72.0).coerceAtLeast(24.0)
  val totalA = ((charger1AStr.toDoubleOrNull() ?: 5.0) + (charger2AStr.toDoubleOrNull() ?: 0.0)).coerceAtLeast(0.5)
  val rate = (kwhRateStr.toDoubleOrNull() ?: 0.16).coerceAtLeast(0.01)
  val whPerMi = (whPerMiStr.toDoubleOrNull() ?: 38.0).coerceAtLeast(8.0)

  val profile = com.example.core.PevBatteryPhysics.packProfile(packV, "NMC")
  val packAh = com.example.core.PevBatteryPhysics.computePackAh(packWh, packV, "NMC").coerceAtLeast(1.0)
  val chargeC = totalA / packAh
  val etaHrs =
    com.example.core.PevBatteryPhysics.estimateChargeTimeHours(
      batteryWh = packWh,
      ratedVoltage = packV,
      chargerAmps = totalA,
      startSocPct = 15.0,
      targetSocPct = 100.0,
      chemistry = "NMC",
    )
  val wallSpec = com.example.core.PevBatteryPhysics.estimateChargerWatts(packV, totalA, 80.0, "NMC")
  val fullChargeCost = com.example.core.PevBatteryPhysics.estimateChargeCostUsd(packWh, 0.0, 100.0, rate)
  val costPer100Mi = (whPerMi * 100.0 / 1000.0 / 0.88) * rate

  TacticalCard(highlight = wallSpec.wallAmps120v > 12.5) {
    Text("BAY 15: DUAL-CHARGER & ELECTRICITY COST LAB", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(value = packWhStr, onValueChange = { packWhStr = it }, label = { Text("Pack Wh") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = packVoltsStr, onValueChange = { packVoltsStr = it }, label = { Text("Pack V") }, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(value = charger1AStr, onValueChange = { charger1AStr = it }, label = { Text("Charger 1 (A)") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = charger2AStr, onValueChange = { charger2AStr = it }, label = { Text("Charger 2 (A)") }, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(value = kwhRateStr, onValueChange = { kwhRateStr = it }, label = { Text("$/kWh Rate") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = whPerMiStr, onValueChange = { whPerMiStr = it }, label = { Text("Avg Wh/mi") }, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Text(
      "Combined Rate (${profile.seriesCells}S): ${"%.1f".format(totalA)}A (${"%.2f".format(chargeC)}C) • 15–100% CC+CV ETA: ${"%.1f".format(etaHrs)} hrs",
      style = MaterialTheme.typography.titleSmall,
      color = if (chargeC > 0.5) TacticalColors.SolarAmber else MaterialTheme.colorScheme.primary,
    )
    Text(
      "AC Draw: ${wallSpec.wallAcWatts.toInt()}W (${"%.1f".format(wallSpec.wallAmps120v)}A @ 120V)" +
        if (wallSpec.wallAmps120v > 12.5) " — EXCEEDS 15A BREAKER!" else " • Full Charge Cost: $${"%.2f".format(fullChargeCost)} ($${"%.2f".format(costPer100Mi)}/100mi)",
      style = MaterialTheme.typography.bodyMedium,
      color = if (wallSpec.wallAmps120v > 12.5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
fun RegenDownhillRecoveryBay() {
  var totalLbsStr by remember { mutableStateOf("245") }
  var elevDropFtStr by remember { mutableStateOf("1200") }
  var regenEffStr by remember { mutableStateOf("42") }
  var packVoltsStr by remember { mutableStateOf("72") }
  var startSocStr by remember { mutableStateOf("85") }

  val massKg = (totalLbsStr.toDoubleOrNull() ?: 245.0).coerceAtLeast(60.0) * 0.453592
  val dropM = (elevDropFtStr.toDoubleOrNull() ?: 1200.0).coerceAtLeast(0.0) * 0.3048
  val eff = ((regenEffStr.toDoubleOrNull() ?: 42.0) / 100.0).coerceIn(0.05, 0.85)
  val packV = (packVoltsStr.toDoubleOrNull() ?: 72.0).coerceAtLeast(24.0)
  val startSoc = (startSocStr.toDoubleOrNull() ?: 85.0).coerceIn(0.0, 100.0)

  val profile = com.example.core.PevBatteryPhysics.packProfile(packV, "NMC")
  val potentialJoules = massKg * 9.81 * dropM
  val potentialWh = potentialJoules / 3600.0
  val recoveredWh = potentialWh * eff
  val recoveredAh = recoveredWh / profile.nominalPackV.coerceAtLeast(12.0)
  val bonusMiles = recoveredWh / 32.0
  // Estimate peak regen voltage rise: resting V at startSoc + ~20A braking spike * pack IR
  val restV = com.example.core.PevBatteryPhysics.voltageForSeriesSoc(profile.seriesCells, startSoc, "NMC")
  val rPackOhms = (profile.seriesCells * 0.022 * com.example.core.PevBatteryPhysics.cellIrMultiplier(startSoc, 20.0, "NMC")) / 5.0
  val spikeV = restV + 22.0 * rPackOhms
  val ovpRisk = spikeV >= profile.fullChargeV

  TacticalCard(highlight = ovpRisk) {
    Text("BAY 16: REGEN DOWNHILL ENERGY RECOVERY", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Text(
      if (ovpRisk) "OVP CUTOUT HAZARD! Peak regen ${"%.1f".format(spikeV)}V exceeds ${"%.1f".format(profile.fullChargeV)}V full charge — start descent <= 90% SoC!"
      else "Rest ${"%.1f".format(restV)}V -> Peak 22A regen ~${"%.1f".format(spikeV)}V (${"%.1f".format(profile.fullChargeV - spikeV)}V OVP headroom).",
      style = MaterialTheme.typography.labelSmall,
      color = if (ovpRisk) MaterialTheme.colorScheme.error else TacticalColors.SolarAmber,
    )
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(value = totalLbsStr, onValueChange = { totalLbsStr = it }, label = { Text("Rider+PEV lbs") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = elevDropFtStr, onValueChange = { elevDropFtStr = it }, label = { Text("Descent (ft)") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = startSocStr, onValueChange = { startSocStr = it }, label = { Text("Start SoC %") }, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(value = regenEffStr, onValueChange = { regenEffStr = it }, label = { Text("Regen Eff %") }, modifier = Modifier.weight(1f))
      OutlinedTextField(value = packVoltsStr, onValueChange = { packVoltsStr = it }, label = { Text("Pack Volts") }, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Text(
      "Recovered Energy: +${"%.1f".format(recoveredWh)} Wh (+${"%.2f".format(recoveredAh)} Ah) • Bonus Range: +${"%.1f".format(bonusMiles)} mi",
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.primary,
    )
    Text(
      "Brake Rotor Heat Saved: ${"%.0f".format(recoveredWh)} Wh kept off pads & fluid (${profile.seriesCells}S ${profile.chemistry}).",
      style = MaterialTheme.typography.bodySmall,
    )
  }
}
