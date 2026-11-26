package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.PevBatteryPhysics
import com.example.data.BoardRuralTemplate
import com.example.data.ChannelRuralSession
import com.example.data.LiveTelemetrySnapshot
import com.example.data.MasterSettings
import com.example.data.PevVehicle
import com.example.data.RuralHazardPreset
import com.example.data.RuralPevMasterSuite250
import com.example.data.RuralPevPhysics
import com.example.ui.components.TacticalCard
import com.example.ui.components.TacticalSlider
import java.util.Calendar
import kotlin.math.abs

// ============================================================================
// 1. COCKPIT / TRAILHEAD TAB — LIVE BACKROAD COMMAND & FIELD CALCULATOR DECK
// ============================================================================
@Composable
fun CockpitRuralLiveCommandDeck(
  telemetry: LiveTelemetrySnapshot,
  activeVehicle: PevVehicle?,
  settings: MasterSettings,
  ambientTempF: Double = 62.0,
) {
  var expanded by remember { mutableStateOf(false) }
  var calcTab by remember { mutableIntStateOf(0) }

  val packNomV = activeVehicle?.voltage?.takeIf { it >= 12.0 } ?: 48.0
  val packWh = activeVehicle?.batteryWh?.toDouble()?.takeIf { it >= 50.0 } ?: 1000.0
  val livePct = telemetry.batteryPct.coerceIn(0, 100)
  val ambientTempC = (ambientTempF - 32.0) * 5.0 / 9.0
  val speedMph = telemetry.speedKmh * 0.621371
  val whPerMile = (22.0 + (speedMph * 0.65)).coerceIn(18.0, 68.0)
  val liveWatts = (speedMph.coerceAtLeast(12.0) * whPerMile).coerceIn(120.0, 12000.0)
  val dischargeSim =
    remember(packNomV, packWh, livePct, ambientTempC, liveWatts, settings.batteryChemistry) {
      val sCells = PevBatteryPhysics.resolveSeriesCount(packNomV, settings.batteryChemistry)
      val packAh = PevBatteryPhysics.computePackAh(packWh, packNomV, settings.batteryChemistry).coerceAtLeast(5.0)
      val pCells = (packAh / 4.5).coerceIn(1.0, 16.0)
      val cAh = packAh / pCells
      val loadAmps = (liveWatts / packNomV.coerceAtLeast(12.0)).coerceIn(2.0, 220.0)
      PevBatteryPhysics.simulateUnderLoadDischarge(
        seriesCells = sCells,
        parallelCells = pCells,
        cellCapacityAh = cAh,
        loadAmps = loadAmps,
        socPct = livePct.toDouble(),
        ambientTempC = ambientTempC,
        chemistry = settings.batteryChemistry,
      )
    }
  val coldFactor =
    remember(ambientTempF, settings.batteryChemistry) {
      RuralPevPhysics.computeColdWeatherRangeFactor(ambientTempF, settings.batteryChemistry)
    }
  val estRemainingWh = dischargeSim.usableWh * (livePct / 100.0)
  val estRangeMi = (estRemainingWh / whPerMile).coerceAtLeast(0.0)
  val liveGradePct = (kotlin.math.tan(Math.toRadians(telemetry.pitchDeg.toDouble().coerceIn(-25.0, 25.0))) * 100.0)
  val totalWeightLbs = (settings.riderWeightLbs + (activeVehicle?.vehicleWeightLbs ?: 65.0)).coerceIn(120.0, 550.0)
  val liveClimb =
    remember(totalWeightLbs, speedMph, liveGradePct, packNomV) {
      RuralPevPhysics.computeHillClimbPower(
        totalLbs = totalWeightLbs,
        speedMph = speedMph.coerceAtLeast(10.0),
        gradePct = liveGradePct.coerceAtLeast(0.0),
        packVoltage = packNomV,
        rollingCrr = 0.022,
      )
    }
  val gasSaved =
    remember(telemetry.tripDistanceKm, settings.kwhRateUsd, settings.gasPriceUsd, settings.carMpg) {
      val tripWh = (telemetry.tripDistanceKm * 0.621371) * whPerMile
      RuralPevPhysics.computeGasSavingsUsd(
        distanceKm = telemetry.tripDistanceKm,
        whUsed = tripWh,
        kwhRateUsd = settings.kwhRateUsd,
        gasPriceUsd = settings.gasPriceUsd,
        mpg = settings.carMpg,
      )
    }
  val fullChargeV =
    remember(packNomV, settings.batteryChemistry) {
      PevBatteryPhysics.packProfile(packNomV, settings.batteryChemistry).fullChargeV
    }
  val duskNote =
    remember(settings.headlightDuskReminder, settings.duskAutoHeadlightReminderMin) {
      val hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
      when {
        hr in 17..19 -> "Dusk Window — Beams ON"
        hr >= 20 || hr < 6 -> "Night Riding — High-Viz"
        else -> "Daylight • Dusk Alert -${settings.duskAutoHeadlightReminderMin}m"
      }
    }

  TacticalCard(highlight = expanded, title = "Backroad Trail Ops") {
    Row(
      modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.testTag("cockpit_rural_deck_toggle"),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(Modifier.weight(1f)) {
        Text(
          "BACKROAD FIELD COMMAND & CALCULATORS",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Black,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          "Est. ${"%.1f".format(estRangeMi)} mi • 100%=${"%.1f".format(fullChargeV)}V • Saved $${"%.2f".format(gasSaved)} • $duskNote",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Icon(
        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
        contentDescription = if (expanded) "Collapse Backroad Deck" else "Expand Backroad Deck",
        tint = MaterialTheme.colorScheme.primary,
      )
    }

    Spacer(Modifier.height(8.dp))
    // Compact 3-pill live physics HUD strip (always live!)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      RuralMetricPill(
        label = "True Range",
        value = "${"%.1f".format(estRangeMi)} mi",
        accent = Color(0xFF00F0FF),
        modifier = Modifier.weight(1f),
      )
      RuralMetricPill(
        label = "Climb Load",
        value = "${liveClimb.requiredWatts.toInt()}W (${"%.0f".format(abs(liveGradePct))}%)",
        accent = Color(0xFF39FF14),
        modifier = Modifier.weight(1f),
      )
      RuralMetricPill(
        label = "Cold Factor",
        value = "${(coldFactor * 100).toInt()}% ${settings.batteryChemistry}",
        accent = Color(0xFFFFB703),
        modifier = Modifier.weight(1f),
      )
    }

    AnimatedVisibility(visible = expanded) {
      Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        val tabs = listOf("Tire PSI (30)", "Barn Breaker", "Solar ETA", "Hill Watts", "Creek Ford")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(tabs.indices.toList()) { idx ->
            FilterChip(
              selected = calcTab == idx,
              onClick = { calcTab = idx },
              label = { Text(tabs[idx], maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
              modifier = Modifier.testTag("rural_calc_tab_$idx"),
            )
          }
        }

        when (calcTab) {
          0 -> {
            var surfaceIdx by remember { mutableIntStateOf(5) }
            val profile = RuralPevMasterSuite250.TERRAIN_PROFILES[surfaceIdx]
            val basePsi = activeVehicle?.targetPsi ?: 42.0
            val recPsi = RuralPevPhysics.computeDynamicTirePsi(basePsi, totalWeightLbs, profile.psiMultiplier)
            Text("Surface: ${profile.surface} (${profile.category})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(RuralPevMasterSuite250.TERRAIN_PROFILES.indices.toList()) { idx ->
                val p = RuralPevMasterSuite250.TERRAIN_PROFILES[idx]
                FilterChip(
                  selected = surfaceIdx == idx,
                  onClick = { surfaceIdx = idx },
                  label = { Text(p.surface, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
                )
              }
            }
            Text(
              "Recommended PSI for ${totalWeightLbs.toInt()} lbs total: ${"%.1f".format(recPsi)} PSI • Max Safe Lean: ±${profile.maxSafeLeanDeg}°",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(profile.ridingTip, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }

          1 -> {
            var chargerW by remember { mutableIntStateOf(420) }
            var rigCount by remember { mutableIntStateOf(2) }
            var breakerA by remember { mutableIntStateOf(settings.barnBreakerLimitAmps) }
            val res = RuralPevPhysics.computeBarnBreakerLoad(chargerW, rigCount, breakerA)
            Text("Charger Output: ${chargerW}W × $rigCount rigs on ${breakerA}A Barn Circuit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            TacticalSlider(value = chargerW.toFloat(), onValueChange = { chargerW = (it.toInt() / 20) * 20 }, valueRange = 100f..1500f)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              listOf(1, 2, 3, 4, 5).forEach { n ->
                FilterChip(selected = rigCount == n, onClick = { rigCount = n }, label = { Text("$n Rigs", maxLines = 1, softWrap = false) })
              }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              listOf(15, 20, 30, 50).forEach { a ->
                FilterChip(selected = breakerA == a, onClick = { breakerA = a }, label = { Text("${a}A", maxLines = 1, softWrap = false) })
              }
            }
            Text(
              "${res.totalWatts}W • ${"%.1f".format(res.lineAmps)}A draw — ${res.advice}",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold,
              color = if (res.isOverloaded) MaterialTheme.colorScheme.error else Color(0xFF39FF14),
            )
            Text("30 Barn & Solar Recipes:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(RuralPevMasterSuite250.BARN_SOLAR_RECIPES.take(10), key = { it.id }) { rec ->
                AssistChip(
                  onClick = {
                    if (rec.watts > 0) chargerW = rec.watts
                    breakerA = rec.breakerAmps
                  },
                  label = { Text(rec.name, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
                )
              }
            }
          }

          2 -> {
            var panelW by remember { mutableIntStateOf(400) }
            var sunPct by remember { mutableIntStateOf(80) }
            val hrs = RuralPevPhysics.computeSolarChargeHours(packWh, livePct, settings.solarChargeTargetPct, panelW, sunPct / 100.0)
            Text("Off-Grid Solar Panel: ${panelW}W • Clear Sky: $sunPct%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            TacticalSlider(value = panelW.toFloat(), onValueChange = { panelW = (it.toInt() / 50) * 50 }, valueRange = 100f..1600f)
            TacticalSlider(value = sunPct.toFloat(), onValueChange = { sunPct = it.toInt() }, valueRange = 20f..100f)
            Text(
              "Pack (${packWh.toInt()}Wh) $livePct% → ${settings.solarChargeTargetPct}% target: ~${"%.1f".format(hrs)} sun-hours",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF00F0FF),
            )
          }

          3 -> {
            var gradePct by remember { mutableDoubleStateOf(12.0) }
            var climbMph by remember { mutableDoubleStateOf(16.0) }
            val res = RuralPevPhysics.computeHillClimbPower(totalWeightLbs, climbMph, gradePct, packNomV, 0.025)
            val climbDisplay =
              if (settings.distanceUnit.equals("mi", ignoreCase = true)) "${"%.0f".format(climbMph)} MPH"
              else "${"%.0f".format(climbMph * 1.60934)} KM/H"
            Text("Grade: ${"%.0f".format(gradePct)}% • Speed: $climbDisplay • Weight: ${totalWeightLbs.toInt()} lbs", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            TacticalSlider(value = gradePct.toFloat(), onValueChange = { gradePct = it.toDouble() }, valueRange = 2f..30f)
            TacticalSlider(value = climbMph.toFloat(), onValueChange = { climbMph = it.toDouble() }, valueRange = 6f..35f)
            Text(
              "Draw: ${res.requiredWatts.toInt()}W (${"%.1f".format(res.batteryAmps)}A @ ${packNomV.toInt()}V) — ${res.thermalRisk}",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold,
              color = if (res.batteryAmps > 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
          }

          else -> {
            var depthIn by remember { mutableDoubleStateOf(3.0) }
            var ipRating by remember { mutableStateOf("IP65") }
            val (safe, msg) = RuralPevPhysics.computeCreekFordSafety(depthIn, ipRating, axleHeightInches = 5.5)
            Text("Creek Depth: ${"%.1f".format(depthIn)} inches • Rig Seal: $ipRating", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            TacticalSlider(value = depthIn.toFloat(), onValueChange = { depthIn = it.toDouble() }, valueRange = 0.5f..12.0f)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              listOf("IP54", "IP65", "IP66", "IP67").forEach { ip ->
                FilterChip(selected = ipRating == ip, onClick = { ipRating = ip }, label = { Text(ip, maxLines = 1, softWrap = false) })
              }
            }
            Text(msg, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (safe) Color(0xFF39FF14) else MaterialTheme.colorScheme.error)
          }
        }
      }
    }
  }
}

@Composable
private fun RuralMetricPill(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = accent.copy(alpha = 0.11f),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    modifier = modifier,
  ) {
    Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
      Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
      Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
  }
}

// ============================================================================
// 2. RADAR TAB — 30-PRESET RURAL HAZARD DISPATCH & OFFLINE BAILOUT DECK
// ============================================================================
@Composable
fun RadarRuralTrailIntelDeck(
  telemetry: LiveTelemetrySnapshot,
  hasGpsFix: Boolean,
  onDispatchHazard: (RuralHazardPreset) -> Unit,
) {
  val ctx = LocalContext.current
  val clipboard = LocalClipboardManager.current
  var expanded by remember { mutableStateOf(false) }
  var selectedGroup by remember { mutableStateOf("Farm & Road") }
  val groups = listOf("Farm & Road", "Water & Surface", "Wildlife & Dusk")
  val filtered = remember(selectedGroup) { RuralPevMasterSuite250.RURAL_HAZARD_PRESETS.filter { it.group == selectedGroup } }

  TacticalCard(highlight = expanded, title = "Trail Dispatch & Check-In") {
    Row(
      modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.testTag("radar_rural_intel_toggle"),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
        Icon(Icons.Default.Terrain, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column {
          Text("RURAL HAZARD DISPATCH & BAILOUT (${RuralPevMasterSuite250.RURAL_HAZARD_PRESETS.size})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
          Text("1-tap Farm, Creek, Mud & Wildlife pins + Offline GPS Rescue Card", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
      Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = "Toggle Trail Dispatch")
    }

    AnimatedVisibility(visible = expanded) {
      Column(Modifier.fillMaxWidth().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          groups.forEach { grp ->
            FilterChip(
              selected = selectedGroup == grp,
              onClick = { selectedGroup = grp },
              label = { Text(grp, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
            )
          }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(filtered, key = { it.id }) { preset ->
            AssistChip(
              onClick = { onDispatchHazard(preset) },
              leadingIcon = { Icon(Icons.Default.Warning, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error) },
              label = { Text(preset.label, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
              modifier = Modifier.testTag("radar_rural_hz_${preset.id}"),
            )
          }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        val bailoutText =
          if (hasGpsFix) {
            "RURAL ERIDES BAILOUT COORDS: ${"%.5f".format(telemetry.lat)}, ${"%.5f".format(telemetry.lng)} | Alt ${telemetry.altitudeM.toInt()}m | Batt ${telemetry.batteryPct}% | Maps: https://maps.google.com/?q=${telemetry.lat},${telemetry.lng}"
          } else {
            "Awaiting live GPS satellite lock for bailout coordinates..."
          }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = {
              clipboard.setText(AnnotatedString(bailoutText))
              Toast.makeText(ctx, "Copied live GPS bailout card!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.weight(1f).testTag("radar_copy_bailout_btn"),
          ) {
            Icon(Icons.Default.ContentCopy, null, Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text("Copy GPS Bailout", maxLines = 1, softWrap = false)
          }
          Button(
            onClick = {
              val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, bailoutText)
              }
              runCatching { ctx.startActivity(Intent.createChooser(intent, "Share Trail Bailout Coords")) }
            },
            modifier = Modifier.weight(1f).testTag("radar_share_bailout_btn"),
          ) {
            Icon(Icons.Default.Share, null, Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text("Share Rescue Pin", maxLines = 1, softWrap = false)
          }
        }
      }
    }
  }
}

// ============================================================================
// 3. MECHANIC TAB — 135-TOOL BARN BENCH SUITE (45 FAULT TREES + 60 TORQUE + 30 PSI)
// ============================================================================
@Composable
fun MechanicBarnDiagnosticSuite(
  onAskMechanicAi: (String) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  var mode by remember { mutableIntStateOf(0) }
  var search by remember { mutableStateOf("") }

  TacticalCard(highlight = expanded, title = "Workshop Quick Tools") {
    Row(
      modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.testTag("mechanic_barn_suite_toggle"),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
        Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column {
          Text("BARN WORKBENCH REFERENCE (135 TOOLS)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
          Text("45 Live Fault Trees • 60 Bolt Torque Specs (Nm/ft-lb) • 30 Surface PSI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
      Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = "Toggle Barn Workbench")
    }

    AnimatedVisibility(visible = expanded) {
      Column(Modifier.fillMaxWidth().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("Fault Tree (45)", "Torque Specs (60)", "Terrain PSI (30)").forEachIndexed { i, lbl ->
            FilterChip(
              selected = mode == i,
              onClick = { mode = i; search = "" },
              label = { Text(lbl, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
              modifier = Modifier.testTag("barn_suite_tab_$i"),
            )
          }
        }
        OutlinedTextField(
          value = search,
          onValueChange = { search = it },
          leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
          placeholder = { Text("Filter symptoms, bolts, or terrain...") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        when (mode) {
          0 -> {
            val matches =
              RuralPevMasterSuite250.FAULT_TREES.filter {
                search.isBlank() || it.symptom.contains(search, true) || it.subsystem.contains(search, true) || it.barnFix.contains(search, true)
              }.take(8)
            matches.forEach { ft ->
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${ft.subsystem}: ${ft.symptom}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onAskMechanicAi(ft.aiPrompt) }) {
                      Icon(Icons.Default.AutoAwesome, null, Modifier.size(14.dp))
                      Spacer(Modifier.width(4.dp))
                      Text("Ask AI", maxLines = 1, softWrap = false)
                    }
                  }
                  Text("Multimeter: ${ft.meterCheck}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                  Text("Barn Fix: ${ft.barnFix}", style = MaterialTheme.typography.bodySmall)
                }
              }
            }
          }

          1 -> {
            val matches =
              RuralPevMasterSuite250.TORQUE_SPECS.filter {
                search.isBlank() || it.component.contains(search, true) || it.group.contains(search, true)
              }.take(10)
            matches.forEach { tq ->
              Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(Modifier.weight(1f)) {
                  Text(tq.component, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                  Text("${tq.group} • Thread: ${tq.threadLock} • ${tq.gravelCheckNote}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp)) {
                  Text(
                    "${"%.1f".format(tq.torqueNm)} Nm / ${"%.1f".format(tq.torqueFtLbs)} ft-lb",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  )
                }
              }
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            }
          }

          else -> {
            val matches =
              RuralPevMasterSuite250.TERRAIN_PROFILES.filter {
                search.isBlank() || it.surface.contains(search, true) || it.category.contains(search, true)
              }.take(10)
            matches.forEach { tp ->
              Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(Modifier.weight(1f)) {
                  Text("${tp.surface} (${tp.category})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                  Text(tp.ridingTip, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                  "×${"%.2f".format(tp.psiMultiplier)} PSI\n±${tp.maxSafeLeanDeg}°",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.secondary,
                )
              }
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            }
          }
        }
      }
    }
  }
}

// ============================================================================
// 4. CHANNEL TAB — 35 CURATED RURAL PEV RADIO & WORKSHOP VIDEO LAUNCHERS
// ============================================================================
@Composable
fun ChannelRuralDiscoveryDeck(
  onLaunchSession: (ChannelRuralSession) -> Unit,
) {
  var selectedGroup by remember { mutableStateOf("Riding Radio") }
  val groups = listOf("Riding Radio", "Barn Repair", "VESC & Tech", "Trail Rides")
  val sessions = remember(selectedGroup) { RuralPevMasterSuite250.CHANNEL_SESSIONS.filter { it.group == selectedGroup } }

  TacticalCard(title = "Featured Ride Channels") {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
      Column {
        Text("RURAL RIDING RADIO & BARN WORKSHOPS (${RuralPevMasterSuite250.CHANNEL_SESSIONS.size})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text("1-tap live YouTube playlists, VESC labs & gravel repair clinics", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    Spacer(Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(groups) { g ->
        FilterChip(
          selected = selectedGroup == g,
          onClick = { selectedGroup = g },
          label = { Text(g, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
        )
      }
    }
    Spacer(Modifier.height(6.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(sessions, key = { it.id }) { session ->
        AssistChip(
          onClick = { onLaunchSession(session) },
          leadingIcon = { Icon(Icons.Default.PlayArrow, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.secondary) },
          label = { Text(session.title, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
          modifier = Modifier.testTag("channel_rural_session_${session.id}"),
        )
      }
    }
  }
}

// ============================================================================
// 5. PORCH BOARD TAB — 35 RURAL COMMUNITY QUICK-POST TEMPLATES
// ============================================================================
@Composable
fun BoardRuralTemplatesStrip(
  onLaunchTemplate: (BoardRuralTemplate) -> Unit,
) {
  var selectedGroup by remember { mutableStateOf("Convoy") }
  val groups = listOf("Convoy", "Swap Meet", "Road Pulse", "Chargers", "Builds")
  val templates = remember(selectedGroup) { RuralPevMasterSuite250.BOARD_TEMPLATES.filter { it.group == selectedGroup } }

  TacticalCard(modifier = Modifier.padding(horizontal = 14.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Icon(Icons.Default.Forum, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
      Column {
        Text("RURAL PORCH POST TEMPLATES (${RuralPevMasterSuite250.BOARD_TEMPLATES.size})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text("Tap to open live post composer pre-filled for county convoys, road reports & swap meets", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    Spacer(Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(groups) { grp ->
        FilterChip(
          selected = selectedGroup == grp,
          onClick = { selectedGroup = grp },
          label = { Text(grp, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
        )
      }
    }
    Spacer(Modifier.height(6.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(templates, key = { it.id }) { tpl ->
        AssistChip(
          onClick = { onLaunchTemplate(tpl) },
          leadingIcon = { Icon(Icons.Default.Explore, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary) },
          label = { Text(tpl.label, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
          modifier = Modifier.testTag("board_rural_tpl_${tpl.id}"),
        )
      }
    }
  }
}
