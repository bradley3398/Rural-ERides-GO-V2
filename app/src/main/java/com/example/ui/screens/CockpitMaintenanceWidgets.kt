package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.PevBatteryPhysics
import com.example.data.MaintenanceTask
import com.example.data.MaintenanceWearState
import com.example.data.MasterSettings
import com.example.data.PevVehicle
import com.example.data.RideSession
import com.example.data.ServiceLogRecord
import com.example.hardware.VoiceAudioManager
import com.example.ui.components.SocialShareDialog
import com.example.ui.components.SocialSharePayload
import com.example.ui.components.TacticalCard
import com.example.ui.components.distUnitLabel
import com.example.ui.components.formatDistValue
import com.example.ui.components.formatSpeedValue
import com.example.ui.components.speedUnitLabel

@Composable
fun PredictiveMaintenanceCard(
  vehicle: PevVehicle?,
  tasks: List<MaintenanceTask>,
  wearStates: List<MaintenanceWearState>,
  serviceLogs: List<ServiceLogRecord>,
  preflightChecked: Set<String>,
  trailKitChecked: Set<String> = emptySet(),
  settings: MasterSettings,
  voiceAudio: VoiceAudioManager? = null,
  onAddTask: (title: String, notes: String, dueKm: Double) -> Unit,
  onToggleDone: (MaintenanceTask) -> Unit,
  onDeleteTask: (String) -> Unit,
  onResetWear: (itemId: String) -> Unit,
  onTogglePreflight: (itemKey: String) -> Unit,
  onResetPreflight: () -> Unit,
  onToggleTrailKit: (itemKey: String) -> Unit = {},
  onResetTrailKit: () -> Unit = {},
  onAddServiceLog: (component: String, notes: String, costUsd: Double) -> Unit,
  onDeleteServiceLog: (logId: String) -> Unit,
) {
  val ctx = LocalContext.current
  var subTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("Wear Gauges", "Pack SOH & V", "Pre-Ride", "Torque Bench", "Saddlebag Kit", "Barn Logbook")

  var showAddTask by remember { mutableStateOf(false) }
  var title by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf("") }
  var intervalStr by remember { mutableStateOf("500") }

  var showAddLog by remember { mutableStateOf(false) }
  var logComp by remember { mutableStateOf("") }
  var logNotes by remember { mutableStateOf("") }
  var logCostStr by remember { mutableStateOf("") }

  val odoKm = vehicle?.odometerKm ?: 0.0

  fun playCue(cue: VoiceAudioManager.TouchCue = VoiceAudioManager.TouchCue.BUTTON_TAP) {
    voiceAudio?.triggerHapticCue(
      enabled = settings.hapticFeedback && settings.buttonHapticFeedback,
      cue = VoiceAudioManager.HapticCue.LIGHT_TICK,
      durationMs = settings.hapticDurationMs,
    )
    voiceAudio?.playTouchSound(
      enabled = settings.touchSoundsEnabled,
      pack = settings.touchSoundPack,
      cue = cue,
      volumePct = settings.globalVolume,
    )
  }

  TacticalCard(title = "Barn & Bench PEV Care") {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Icon(Icons.Default.ElectricalServices, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(
          "BARN & BENCH CARE LAB",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Black,
          color = MaterialTheme.colorScheme.primary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      IconButton(
        onClick = {
          playCue()
          showAddTask = true
        },
        modifier = Modifier.size(48.dp).testTag("add_maintenance_button"),
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add Maintenance Task")
      }
    }
    if (vehicle != null) {
      val packProf = remember(vehicle.voltage, settings.batteryChemistry) {
        PevBatteryPhysics.packProfile(vehicle.voltage, settings.batteryChemistry)
      }
      Text(
        "${vehicle.name} • Odo ${"%.1f".format(formatDistValue(odoKm, settings.distanceUnit))} ${distUnitLabel(settings.distanceUnit)} • ${packProf.seriesCount}S (${"%.1f".format(packProf.fullChargeV)}V Full) • ${vehicle.targetPsi.toInt()} PSI",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Spacer(Modifier.height(6.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(tabs.size) { idx ->
        FilterChip(
          selected = subTab == idx,
          onClick = {
            playCue(VoiceAudioManager.TouchCue.TAB_SWITCH)
            subTab = idx
          },
          label = { Text(tabs[idx], style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
          modifier = Modifier.testTag("maint_subtab_$idx"),
        )
      }
    }
    Spacer(Modifier.height(8.dp))

    when (subTab) {
      0 -> {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          if (wearStates.isEmpty()) {
            Text(
              "Initializing live component wear gauges...",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          wearStates.forEach { w ->
            val used = (odoKm - w.lastResetKm).coerceAtLeast(0.0)
            val pct = (used / w.ratedKm.coerceAtLeast(100.0)).toFloat().coerceIn(0f, 1f)
            val lifePct = (100 - (pct * 100).toInt()).coerceIn(0, 100)
            val remainDist = formatDistValue((w.ratedKm - used).coerceAtLeast(0.0), settings.distanceUnit)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text(w.label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                  Text(
                    "$lifePct% (${"%.0f".format(remainDist)} ${distUnitLabel(settings.distanceUnit)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (lifePct <= 25) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
                Spacer(Modifier.height(3.dp))
                LinearProgressIndicator(progress = { 1f - pct }, modifier = Modifier.fillMaxWidth().height(6.dp))
              }
              Spacer(Modifier.width(8.dp))
              OutlinedButton(
                onClick = {
                  playCue(VoiceAudioManager.TouchCue.ACTION_CONFIRM)
                  onResetWear(w.itemId)
                  Toast.makeText(ctx, "${w.label} reset to 100% & logged in Barn Logbook.", Toast.LENGTH_SHORT).show()
                },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("reset_wear_${w.itemId}"),
              ) {
                Text("Reset", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall)
              }
            }
          }
        }
      }
      1 -> {
        val nomV = vehicle?.voltage ?: 48.0
        val wh = (vehicle?.batteryWh ?: 1200L).toDouble()
        val pack = remember(nomV, settings.batteryChemistry) { PevBatteryPhysics.packProfile(nomV, settings.batteryChemistry) }
        val fullRangeKm = (wh / 22.0).coerceAtLeast(25.0)
        val estCycles = (odoKm / fullRangeKm).toInt()
        val ratedCycles = if (settings.batteryChemistry == "LFP") 2500 else 800
        val sohPct = (100.0 - (estCycles.toDouble() / ratedCycles) * 20.0).coerceIn(70.0, 100.0)
        val ah = PevBatteryPhysics.computePackAh(wh, nomV, settings.batteryChemistry)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            "${pack.nominalLabel} (${pack.seriesCount}S ${pack.chemistry}) • ${wh.toInt()} Wh / ${"%.1f".format(ah)} Ah",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            "100% Full: ${"%.1f".format(pack.fullChargeV)}V (${"%.2f".format(pack.fullCellV)}V/cell) • Nominal: ${"%.1f".format(pack.nominalV)}V",
            style = MaterialTheme.typography.bodySmall,
          )
          Text(
            "Barn Storage (50%): ${"%.1f".format(pack.storageV)}V • 0% Cutoff: ${"%.1f".format(pack.cutoffEmptyV)}V (${"%.2f".format(pack.emptyCellV)}V/cell)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
          )
          Text(
            "Est Charge Cycles: $estCycles / $ratedCycles • Pack SOH: ${"%.1f".format(sohPct)}%",
            style = MaterialTheme.typography.labelSmall,
          )
          LinearProgressIndicator(progress = { (sohPct / 100.0).toFloat() }, modifier = Modifier.fillMaxWidth().height(7.dp))
          OutlinedButton(
            onClick = {
              playCue(VoiceAudioManager.TouchCue.ACTION_CONFIRM)
              onAddServiceLog(
                "Battery Balance & Volt Check",
                "${pack.seriesCount}S ${pack.chemistry}: Full ${"%.1f".format(pack.fullChargeV)}V, Storage ${"%.1f".format(pack.storageV)}V, SOH ${"%.1f".format(sohPct)}%",
                0.0,
              )
              Toast.makeText(ctx, "Battery health check saved to Barn Logbook.", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().testTag("log_battery_health_button"),
          ) {
            Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Log Pack Balance Check", maxLines = 1, softWrap = false)
          }
        }
      }
      2 -> {
        val items = listOf(
          "tires" to "Check Tire PSI (${vehicle?.targetPsi?.toInt() ?: 36} PSI) & Sidewalls",
          "brakes" to "Squeeze Hydraulic/Cable Brakes & Check Pads",
          "stem" to "Test Stem Clamp & Folding Latch Zero-Play",
          "lights" to "Verify Headlight Cutoff, Taillight & Horn",
          "pack" to "Confirm Charge Port Cap Seated & Pack Volts Healthy",
          "gear" to "Full-Face Helmet, Gloves & Armor Secured",
        )
        val allArmed = preflightChecked.size >= items.size
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text(
            if (allArmed) "RIG READY TO ROLL (${preflightChecked.size}/${items.size})" else "${preflightChecked.size}/${items.size} Checked",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (allArmed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
          )
          TextButton(
            onClick = {
              playCue(VoiceAudioManager.TouchCue.TOGGLE_OFF)
              onResetPreflight()
              Toast.makeText(ctx, "Pre-ride check cleared.", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.testTag("reset_preflight_button"),
          ) {
            Icon(Icons.Default.Refresh, null, Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text("Reset", maxLines = 1, softWrap = false)
          }
        }
        items.forEach { (key, label) ->
          val checked = preflightChecked.contains(key)
          Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
              checked = checked,
              onCheckedChange = {
                playCue(if (!checked) VoiceAudioManager.TouchCue.TOGGLE_ON else VoiceAudioManager.TouchCue.TOGGLE_OFF)
                onTogglePreflight(key)
              },
            )
            Text(label, style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
      3 -> {
        val specs = listOf(
          "Axle Motor Nuts (M12/M14)" to "45 - 60 Nm (Blue 243)",
          "Stem Clamp / Triple Tree" to "12 - 15 Nm (Cross pattern)",
          "Brake Rotor Bolts (T25)" to "6.2 Nm (Clean w/ IPA)",
          "Caliper Mount Bolts" to "8 - 10 Nm (Squeeze lever)",
          "Suspension Pivot Bolts" to "18 - 22 Nm",
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          specs.forEach { (part, torque) ->
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(part, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text(torque, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
              }
            }
          }
          OutlinedButton(
            onClick = {
              playCue(VoiceAudioManager.TouchCue.ACTION_CONFIRM)
              onAddServiceLog(
                "Bolt Torque Bench Check",
                "Verified axle nuts (45-60Nm), stem clamp (12-15Nm), rotor bolts (6.2Nm), calipers (8-10Nm), pivots (18-22Nm).",
                0.0,
              )
              Toast.makeText(ctx, "Bolt torque inspection logged in Barn Logbook!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().testTag("log_torque_check_button"),
          ) {
            Icon(Icons.Default.Build, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Log Torque Inspection", maxLines = 1, softWrap = false)
          }
        }
      }
      4 -> {
        val kitItems = listOf(
          "kit_allen" to "Metric Allen / Torx T25 Folding Multi-Tool",
          "kit_wrench" to "18mm / 19mm Stubby Wrench for Motor Axle Nuts",
          "kit_plugs" to "Tubeless Tire Plug Kit + Mini Pump / CO2 Inflator",
          "kit_loctite" to "Blue 243 Threadlocker + Heavy Zip Ties + Tape",
          "kit_firstaid" to "Compact Trail First-Aid Kit & Thermal Blanket",
          "kit_charger" to "Compact Barn Fast Charger & Outlet Adapter",
        )
        val packedCount = kitItems.count { (k, _) -> trailKitChecked.contains(k) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text(
            if (packedCount == kitItems.size) "SADDLEBAG PACKED ($packedCount/${kitItems.size})" else "Saddlebag Packed: $packedCount/${kitItems.size}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (packedCount == kitItems.size) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
          )
          TextButton(
            onClick = {
              playCue(VoiceAudioManager.TouchCue.TOGGLE_OFF)
              onResetTrailKit()
              Toast.makeText(ctx, "Saddlebag checklist reset.", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.testTag("reset_trail_kit_button"),
          ) {
            Icon(Icons.Default.Refresh, null, Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text("Reset", maxLines = 1, softWrap = false)
          }
        }
        kitItems.forEach { (key, item) ->
          val checked = trailKitChecked.contains(key)
          Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
              checked = checked,
              onCheckedChange = {
                playCue(if (!checked) VoiceAudioManager.TouchCue.TOGGLE_ON else VoiceAudioManager.TouchCue.TOGGLE_OFF)
                onToggleTrailKit(key)
              },
            )
            Text(item, style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
      else -> {
        val totalSpend = serviceLogs.sumOf { it.costUsd }
        if (serviceLogs.isNotEmpty()) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
          ) {
            Row(
              Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("Total Barn Service Spend", style = MaterialTheme.typography.labelMedium)
              Text(
                "$${"%.2f".format(totalSpend)} (${serviceLogs.size} logs)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false,
              )
            }
          }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = {
              playCue()
              showAddLog = true
            },
            modifier = Modifier.weight(1f).testTag("add_service_log_button"),
          ) {
            Icon(Icons.Default.Add, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Log Service", maxLines = 1, softWrap = false)
          }
          OutlinedButton(
            onClick = {
              playCue()
              showAddTask = true
            },
            modifier = Modifier.weight(1f).testTag("schedule_maint_task_button"),
          ) {
            Icon(Icons.Default.Build, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Add Task", maxLines = 1, softWrap = false)
          }
        }
        Spacer(Modifier.height(6.dp))
        if (tasks.isEmpty() && serviceLogs.isEmpty()) {
          Text("No tasks or service logs yet. Tap Log Service or Add Task above.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        tasks.forEach { t ->
          Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = {
                playCue(VoiceAudioManager.TouchCue.ACTION_CONFIRM)
                onToggleDone(t)
              },
            ) {
              Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Complete Task",
                tint = if (t.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
              )
            }
            Column(Modifier.weight(1f)) {
              Text(t.title, style = MaterialTheme.typography.titleSmall)
              if (t.notes.isNotBlank()) Text(t.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("Due @ ${"%.0f".format(formatDistValue(t.dueOdometerKm, settings.distanceUnit))} ${distUnitLabel(settings.distanceUnit)}", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(
              onClick = {
                playCue(VoiceAudioManager.TouchCue.TOGGLE_OFF)
                onDeleteTask(t.id)
              },
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Delete Task")
            }
          }
        }
        serviceLogs.take(10).forEach { log ->
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
          ) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
              Column(Modifier.weight(1f)) {
                Text("${log.component} • ${log.dateLabel}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                if (log.notes.isNotBlank()) Text(log.notes, style = MaterialTheme.typography.bodySmall)
                if (log.costUsd > 0.0) Text("Cost: $${"%.2f".format(log.costUsd)}", style = MaterialTheme.typography.labelSmall)
              }
              IconButton(
                onClick = {
                  playCue(VoiceAudioManager.TouchCue.TOGGLE_OFF)
                  onDeleteServiceLog(log.id)
                },
              ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Log")
              }
            }
          }
        }
      }
    }
  }

  if (showAddTask) {
    AlertDialog(
      onDismissRequest = { showAddTask = false },
      title = { Text("Schedule Barn Maintenance") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task (e.g. Hydraulic Pad Swap)") })
          OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Torque / Fluid Spec Notes") })
          OutlinedTextField(value = intervalStr, onValueChange = { intervalStr = it }, label = { Text("Due Odometer (km)") })
        }
      },
      confirmButton = {
        Button(onClick = {
          val due = intervalStr.toDoubleOrNull() ?: (odoKm + 500.0)
          onAddTask(title.ifBlank { "Rig Inspection" }, notes, due)
          playCue(VoiceAudioManager.TouchCue.ACTION_CONFIRM)
          Toast.makeText(ctx, "Maintenance task saved!", Toast.LENGTH_SHORT).show()
          title = ""
          notes = ""
          showAddTask = false
        }) { Text("Save") }
      },
      dismissButton = { TextButton(onClick = { showAddTask = false }) { Text("Cancel") } },
    )
  }

  if (showAddLog) {
    AlertDialog(
      onDismissRequest = { showAddLog = false },
      title = { Text("Record Barn Service Log") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = logComp, onValueChange = { logComp = it }, label = { Text("Component (e.g. Rear Knobby Tire)") })
          OutlinedTextField(value = logNotes, onValueChange = { logNotes = it }, label = { Text("Work Performed / Notes") })
          OutlinedTextField(value = logCostStr, onValueChange = { logCostStr = it }, label = { Text("Parts Cost USD") })
        }
      },
      confirmButton = {
        Button(onClick = {
          onAddServiceLog(logComp.ifBlank { "General Barn Service" }, logNotes, logCostStr.toDoubleOrNull() ?: 0.0)
          playCue(VoiceAudioManager.TouchCue.ACTION_CONFIRM)
          Toast.makeText(ctx, "Service log recorded!", Toast.LENGTH_SHORT).show()
          logComp = ""
          logNotes = ""
          logCostStr = ""
          showAddLog = false
        }) { Text("Log Service") }
      },
      dismissButton = { TextButton(onClick = { showAddLog = false }) { Text("Cancel") } },
    )
  }
}

@Composable
fun MissionLogsCard(
  rides: List<RideSession>,
  activeVehicle: PevVehicle? = null,
  settings: MasterSettings,
  debriefBusyId: String?,
  fleetDebrief: String = "",
  fleetDebriefBusy: Boolean = false,
  voiceAudio: VoiceAudioManager? = null,
  onRequestFleetDebrief: () -> Unit = {},
  onRequestDebrief: (RideSession) -> Unit,
  onExportGpx: (RideSession) -> Unit,
  onShareCard: (RideSession) -> Unit = {},
  onOpenTelemetryStudio: (String?) -> Unit = {},
  onExportAllCsv: () -> Unit,
  onDeleteRide: (String) -> Unit,
) {
  val ctx = LocalContext.current
  var searchQuery by remember { mutableStateOf("") }
  var sortMode by remember { mutableStateOf("Newest") }
  var vehicleFilter by remember { mutableStateOf("ALL") }
  var focusedTrackRideId by remember { mutableStateOf<String?>(null) }
  var expandedTelemetryRideId by remember { mutableStateOf<String?>(null) }
  var sharePayload by remember { mutableStateOf<SocialSharePayload?>(null) }

  val vehicleNames = remember(rides) { listOf("ALL") + rides.map { it.vehicleName }.distinct().filter { it.isNotBlank() } }
  val filtered =
    remember(rides, searchQuery, sortMode, vehicleFilter) {
      rides
        .filter { r ->
          (vehicleFilter == "ALL" || r.vehicleName == vehicleFilter) &&
            (searchQuery.isBlank() || r.title.contains(searchQuery, true) || r.vehicleName.contains(searchQuery, true))
        }
        .let { list ->
          when (sortMode) {
            "Distance" -> list.sortedByDescending { it.distanceKm }
            "Max Speed" -> list.sortedByDescending { it.maxSpeedKmh }
            "Efficient" -> list.sortedBy { if (it.distanceKm > 0.1) it.whUsed / it.distanceKm else 999.0 }
            else -> list
          }
        }
    }

  fun playCue(cue: VoiceAudioManager.TouchCue = VoiceAudioManager.TouchCue.BUTTON_TAP) {
    voiceAudio?.playTouchSound(
      enabled = settings.touchSoundsEnabled,
      pack = settings.touchSoundPack,
      cue = cue,
      volumePct = settings.globalVolume,
    )
  }

  TacticalCard(title = "Trailhead Ride Logbook") {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text(
        "TRAILHEAD RIDE LOGBOOK (${filtered.size}/${rides.size})",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      if (rides.isNotEmpty()) {
        TextButton(onClick = { playCue(); onExportAllCsv() }, modifier = Modifier.testTag("export_rides_csv_button")) {
          Icon(Icons.Default.Download, null, Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("CSV", maxLines = 1, softWrap = false)
        }
      }
    }

    if (rides.isNotEmpty()) {
      // UPGRADED BARN FLEET AI LOGBOOK BRIEFING
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
      ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
              Text("RIDGE-TO-RIDGE FLEET AI BRIEFING", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
              Text("Analyzes your whole logbook for pack health & real Wh/mi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(
              onClick = {
                playCue(VoiceAudioManager.TouchCue.ACTION_CONFIRM)
                onRequestFleetDebrief()
              },
              enabled = !fleetDebriefBusy,
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier.testTag("fleet_ai_briefing_button"),
            ) {
              if (fleetDebriefBusy) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
              } else {
                Icon(Icons.Default.AutoAwesome, null, Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("Analyze Fleet", maxLines = 1, softWrap = false)
              }
            }
          }
          if (fleetDebrief.isNotBlank()) {
            Text(fleetDebrief, style = MaterialTheme.typography.bodySmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              item {
                OutlinedButton(
                  onClick = {
                    playCue()
                    voiceAudio?.speakWithActiveSettings(
                      text = fleetDebrief,
                      settings = settings,
                    )
                  },
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                ) {
                  Icon(Icons.Default.VolumeUp, null, Modifier.size(14.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Read Aloud", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall)
                }
              }
              item {
                OutlinedButton(
                  onClick = {
                    playCue()
                    voiceAudio?.stopSpeaking()
                  },
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                ) {
                  Icon(Icons.Default.Stop, null, Modifier.size(14.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Stop Voice", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall)
                }
              }
              item {
                OutlinedButton(
                  onClick = {
                    playCue()
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Fleet AI Briefing", fleetDebrief))
                    Toast.makeText(ctx, "Fleet AI briefing copied.", Toast.LENGTH_SHORT).show()
                  },
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                ) {
                  Icon(Icons.Default.ContentCopy, null, Modifier.size(14.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Copy", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }
        }
      }

      if (settings.showRideChartsOnDash) {
        RideRecordsAnalyticsSuite(
          rides = filtered.ifEmpty { rides },
          settings = settings,
          activeVehicle = activeVehicle,
          focusedTrackRideId = focusedTrackRideId,
          onOpenTelemetryStudio = onOpenTelemetryStudio,
        )
      }
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
        placeholder = { Text("Search ride logbook...") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("mission_search_input"),
      )
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(listOf("Newest", "Distance", "Max Speed", "Efficient")) { s ->
          FilterChip(
            selected = sortMode == s,
            onClick = { playCue(VoiceAudioManager.TouchCue.TAB_SWITCH); sortMode = s },
            label = { Text("Sort: $s", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
          )
        }
        items(vehicleNames) { vn ->
          FilterChip(
            selected = vehicleFilter == vn,
            onClick = { playCue(VoiceAudioManager.TouchCue.TAB_SWITCH); vehicleFilter = vn },
            label = { Text(vn, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
          )
        }
      }
      Spacer(Modifier.height(6.dp))
    }

    if (filtered.isEmpty()) {
      Text("Tap Roll Out above to record live GPS trails, accurate pack volts, Wh/mi efficiency, and Ridge-to-Ridge AI Debriefs.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
      val nomV = activeVehicle?.voltage ?: 48.0
      filtered.take(12).forEach { ride ->
        val startV = if (ride.startBatteryPct >= 0) PevBatteryPhysics.voltageForSoc(nomV, ride.startBatteryPct.toDouble(), settings.batteryChemistry) else 0.0
        val endV = if (ride.endBatteryPct >= 0) PevBatteryPhysics.voltageForSoc(nomV, ride.endBatteryPct.toDouble(), settings.batteryChemistry) else 0.0
        val whPerMi = PevBatteryPhysics.whPerMile(ride.whUsed, ride.distanceKm)
        val whPerKm = PevBatteryPhysics.whPerKm(ride.whUsed, ride.distanceKm)
        val effLabel = if (settings.distanceUnit == "km") "${"%.1f".format(whPerKm)} Wh/km" else "${"%.1f".format(whPerMi)} Wh/mi"
        val hours = (ride.durationSec.coerceAtLeast(1L) / 3600.0)
        val avgSpeedKmh = if (ride.durationSec > 10L && ride.distanceKm > 0.0) ride.distanceKm / hours else 0.0
        val avgWatts = if (ride.durationSec > 10L && ride.whUsed > 0.0) ride.whUsed / hours else 0.0
        val avgAmps = avgWatts / nomV.coerceAtLeast(12.0)
        val estSagV =
          PevBatteryPhysics.estimateVoltageSagVolts(
            loadAmps = avgAmps,
            ratedVoltage = nomV,
            chemistry = settings.batteryChemistry,
            socPct = ride.endBatteryPct.takeIf { it in 5..100 }?.toDouble() ?: 70.0,
          )
        val climbGradePct = if (ride.distanceKm > 0.05) (ride.elevationGainM / (ride.distanceKm * 1000.0)) * 100.0 else 0.0
        val effBadge =
          when {
            whPerMi <= 0.1 -> "TELEMETRY LOG"
            whPerMi < 22.0 -> "S-TIER ECO"
            whPerMi < 36.0 -> "BALANCED"
            whPerMi < 52.0 -> "SPORT"
            else -> "FULL SEND"
          }

        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) {
          Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Column(Modifier.weight(1f)) {
                Text("${ride.title} • ${ride.vehicleName}", style = MaterialTheme.typography.titleMedium)
                Text(
                  "${"%.2f".format(formatDistValue(ride.distanceKm, settings.distanceUnit))} ${distUnitLabel(settings.distanceUnit)} • Max ${"%.1f".format(formatSpeedValue(ride.maxSpeedKmh, settings.distanceUnit))} ${speedUnitLabel(settings.distanceUnit)} • ${ride.durationSec / 60}m • $effLabel",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.secondary,
                )
                if (ride.startBatteryPct >= 0 && ride.endBatteryPct >= 0) {
                  val usedPct = (ride.startBatteryPct - ride.endBatteryPct).coerceAtLeast(0)
                  Text(
                    "Pack: ${ride.startBatteryPct}% (${"%.1f".format(startV)}V) → ${ride.endBatteryPct}% (${"%.1f".format(endV)}V) • -${usedPct}% / ${ride.whUsed.toInt()} Wh",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
              IconButton(
                onClick = {
                  playCue(VoiceAudioManager.TouchCue.TOGGLE_OFF)
                  onDeleteRide(ride.id)
                },
              ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Ride Log")
              }
            }
            Spacer(Modifier.height(6.dp))
            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth().testTag("ride_deep_telemetry_strip_${ride.id}"),
            ) {
              val chips =
                buildList {
                  add("Tier: $effBadge")
                  if (avgSpeedKmh > 0.5) {
                    add("Avg ${"%.1f".format(formatSpeedValue(avgSpeedKmh, settings.distanceUnit))} ${speedUnitLabel(settings.distanceUnit)}")
                  }
                  if (ride.elevationGainM > 0.5 || climbGradePct > 0.1) {
                    add("Climb +${ride.elevationGainM.toInt()}m (${"%.1f".format(climbGradePct)}%)")
                  }
                  if (avgWatts > 1.0) {
                    add("${avgWatts.toInt()}W / ${"%.1f".format(avgAmps)}A")
                    add("Sag -${"%.2f".format(estSagV)}V")
                  }
                  if (ride.polylinePoints.isNotEmpty()) {
                    add("GPS ${ride.polylinePoints.size} pts")
                  }
                }
              items(chips) { metricLabel ->
                Surface(
                  color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                  border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                  shape = MaterialTheme.shapes.extraSmall,
                ) {
                  Text(
                    text = metricLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  )
                }
              }
            }
            if (ride.aiDebrief.isNotBlank()) {
              Spacer(Modifier.height(6.dp))
              Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text("RIDGE-TO-RIDGE AI DEBRIEF", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                  Text(ride.aiDebrief, style = MaterialTheme.typography.bodySmall)
                  LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                      OutlinedButton(
                        onClick = {
                          playCue()
                          voiceAudio?.speakWithActiveSettings(
                            text = ride.aiDebrief,
                            settings = settings,
                          )
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("speak_debrief_${ride.id}"),
                      ) {
                        Icon(Icons.Default.VolumeUp, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Read Aloud", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall)
                      }
                    }
                    item {
                      OutlinedButton(
                        onClick = {
                          playCue()
                          voiceAudio?.stopSpeaking()
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                      ) {
                        Icon(Icons.Default.Stop, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Stop", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall)
                      }
                    }
                    item {
                      OutlinedButton(
                        onClick = {
                          playCue()
                          val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                          cm.setPrimaryClip(ClipData.newPlainText("Ride Debrief", ride.aiDebrief))
                          Toast.makeText(ctx, "AI Debrief copied.", Toast.LENGTH_SHORT).show()
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                      ) {
                        Icon(Icons.Default.ContentCopy, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Copy", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall)
                      }
                    }
                  }
                }
              }
            }
            Spacer(Modifier.height(6.dp))
            val telemetryExpanded = expandedTelemetryRideId == ride.id
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              item {
                OutlinedButton(
                  onClick = {
                    playCue(VoiceAudioManager.TouchCue.TAB_SWITCH)
                    expandedTelemetryRideId = if (telemetryExpanded) null else ride.id
                  },
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                  modifier = Modifier.testTag("toggle_ride_graphs_${ride.id}"),
                ) {
                  Icon(Icons.Default.Analytics, null, Modifier.size(15.dp))
                  Spacer(Modifier.width(4.dp))
                  Text(if (telemetryExpanded) "Hide Graphs" else "Graphs, Pie & Map", maxLines = 1, softWrap = false)
                }
              }
              item {
                OutlinedButton(
                  onClick = {
                    playCue(VoiceAudioManager.TouchCue.ACTION_CONFIRM)
                    onOpenTelemetryStudio(ride.id)
                  },
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                  modifier = Modifier.testTag("deep_dive_ride_${ride.id}"),
                ) {
                  Text("Ride Inspector", maxLines = 1, softWrap = false)
                }
              }
              item {
                OutlinedButton(
                  onClick = {
                    playCue(VoiceAudioManager.TouchCue.ACTION_CONFIRM)
                    onRequestDebrief(ride)
                  },
                  enabled = debriefBusyId != ride.id,
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                  modifier = Modifier.testTag("debrief_ride_${ride.id}"),
                ) {
                  if (debriefBusyId == ride.id) {
                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                  } else {
                    Icon(Icons.Default.AutoAwesome, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (ride.aiDebrief.isBlank()) "AI Debrief" else "Refresh Debrief", maxLines = 1, softWrap = false)
                  }
                }
              }
              item {
                OutlinedButton(
                  onClick = { playCue(); onShareCard(ride) },
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                  modifier = Modifier.testTag("share_card_${ride.id}"),
                ) {
                  Icon(Icons.Default.Share, null, Modifier.size(15.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Share Card", maxLines = 1, softWrap = false)
                }
              }
              item {
                OutlinedButton(
                  onClick = {
                    playCue()
                    val summary =
                      buildString {
                        append("Rural ERides GO Ride: ${ride.title} (${ride.vehicleName})\n")
                        append("Distance: ${"%.2f".format(formatDistValue(ride.distanceKm, settings.distanceUnit))} ${distUnitLabel(settings.distanceUnit)} | ")
                        append("Max: ${"%.1f".format(formatSpeedValue(ride.maxSpeedKmh, settings.distanceUnit))} ${speedUnitLabel(settings.distanceUnit)} | ")
                        append("Eff: $effLabel | Duration: ${ride.durationSec / 60}m")
                        if (ride.aiDebrief.isNotBlank()) append("\nAI Debrief: ${ride.aiDebrief}")
                      }
                    sharePayload =
                      SocialSharePayload(
                        title = "Ride Log: ${ride.title}",
                        text = summary,
                      )
                  },
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                  modifier = Modifier.testTag("share_text_${ride.id}"),
                ) {
                  Text("Share Social", maxLines = 1, softWrap = false)
                }
              }
              item {
                OutlinedButton(
                  onClick = { playCue(); onExportGpx(ride) },
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                  Text("GPX", maxLines = 1, softWrap = false)
                }
              }
            }

            if (telemetryExpanded) {
              Spacer(Modifier.height(8.dp))
              PerRideTelemetryPanel(
                ride = ride,
                activeVehicle = activeVehicle,
                settings = settings,
                showAllStacked = false,
                onOpenFullStudio = null,
              )
            }
          }
        }
      }
    }
  }

  sharePayload?.let { payload ->
    SocialShareDialog(payload = payload, onDismiss = { sharePayload = null })
  }
}
