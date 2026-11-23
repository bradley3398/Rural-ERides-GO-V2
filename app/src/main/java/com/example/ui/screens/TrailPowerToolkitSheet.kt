package com.example.ui.screens

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.MasterSettings
import com.example.hardware.VoiceAudioManager
import com.example.ui.components.formatSpeedValue
import com.example.ui.components.speedUnitLabel
import kotlinx.coroutines.delay
import kotlin.math.PI

private enum class BeaconMode(val label: String) {
  OFF("Off"),
  STEADY("Headlight"),
  STROBE("4Hz Strobe"),
  SOS("SOS Morse"),
}

private data class LapSplit(
  val lapNumber: Int,
  val splitMs: Long,
  val totalMs: Long,
  val speedKmh: Double,
)

private data class TorqueSpecItem(
  val component: String,
  val boltSize: String,
  val rangeNm: String,
  val rangeInLb: String,
  val note: String,
)

private val PEV_TORQUE_SPECS =
  listOf(
    TorqueSpecItem("Stem Clamp / Handlebar Bolts", "M5 / M6 (4–5mm Hex)", "5 – 7 Nm", "44 – 62 in-lb", "Tighten in X-pattern; Blue 242 Loctite"),
    TorqueSpecItem("Folding Stem Latch & Collar", "M6 / M8 High-Tensile", "12 – 18 Nm", "106 – 160 in-lb", "Inspect weekly for gravel play"),
    TorqueSpecItem("Disc Brake Rotor Bolts", "M5 T25 Torx (6-Bolt)", "5 – 6 Nm", "44 – 53 in-lb", "Star sequence; Blue Loctite mandatory"),
    TorqueSpecItem("Brake Caliper Mount Bolts", "M6 (5mm Hex)", "8 – 10 Nm", "71 – 88 in-lb", "Center pads before final torque"),
    TorqueSpecItem("Hub Motor Axle Nuts (12–14mm)", "M12 / M14 Flange", "45 – 65 Nm", "400 – 575 in-lb", "Always verify torque arm engagement"),
    TorqueSpecItem("Swingarm & Suspension Pivot", "M8 / M10", "18 – 28 Nm", "160 – 248 in-lb", "Use nylon locknut or Blue 242"),
    TorqueSpecItem("Split-Rim Wheel Halves", "M5 / M6", "6 – 9 Nm", "53 – 80 in-lb", "Deflate tube fully before loosening!"),
    TorqueSpecItem("Kickstand & Deck Plate Screws", "M5 / M6 Countersunk", "5 – 8 Nm", "44 – 71 in-lb", "Anti-seize on stainless-to-aluminum"),
    TorqueSpecItem("E-Bike Crank Arm Pinch Bolts", "M6 / M8 Square or Hollow", "12 – 14 Nm (Pinch) / 38 Nm", "106 – 124 / 336 in-lb", "Re-check after first 25 miles"),
    TorqueSpecItem("Seatpost Collar & Rail Clamp", "M5 / M6", "6 – 9 Nm", "53 – 80 in-lb", "Carbon posts max 5 Nm + grip paste"),
  )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrailPowerToolkitSheet(
  liveSpeedKmh: Double,
  settings: MasterSettings,
  voiceAudio: VoiceAudioManager? = null,
  onDismiss: () -> Unit,
) {
  BackHandler { onDismiss() }

  val ctx = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var activeTab by remember { mutableIntStateOf(0) }
  var beaconSubMode by remember { mutableIntStateOf(1) } // 0=Quick Torch, 1=Morse TX, 2=Camera RX

  // 1. REAL ANDROID CAMERA TORCH BEACON STATE
  var beaconMode by remember { mutableStateOf(BeaconMode.OFF) }
  var torchError by remember { mutableStateOf<String?>(null) }
  val cameraManager = remember(ctx) { ctx.getSystemService(Context.CAMERA_SERVICE) as? CameraManager }
  val flashCameraId =
    remember(cameraManager) {
      runCatching {
        cameraManager?.cameraIdList?.firstOrNull { id ->
          cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
      }.getOrNull()
    }

  fun setHardwareTorch(on: Boolean) {
    val mgr = cameraManager
    val id = flashCameraId
    if (mgr == null || id == null) {
      torchError = "No LED flash hardware detected on this device."
      return
    }
    runCatching {
      mgr.setTorchMode(id, on)
      torchError = null
    }.onFailure { err ->
      torchError = err.message ?: "Unable to toggle flashlight right now."
    }
  }

  LaunchedEffect(beaconMode) {
    when (beaconMode) {
      BeaconMode.OFF -> setHardwareTorch(false)
      BeaconMode.STEADY -> setHardwareTorch(true)
      BeaconMode.STROBE -> {
        while (true) {
          setHardwareTorch(true)
          delay(125L)
          setHardwareTorch(false)
          delay(125L)
        }
      }
      BeaconMode.SOS -> {
        val sosPattern =
          listOf(
            // S: ...
            180L to 180L, 180L to 180L, 180L to 420L,
            // O: ---
            520L to 180L, 520L to 180L, 520L to 420L,
            // S: ...
            180L to 180L, 180L to 180L, 180L to 1100L,
          )
        while (true) {
          for ((onMs, offMs) in sosPattern) {
            setHardwareTorch(true)
            delay(onMs)
            setHardwareTorch(false)
            delay(offMs)
          }
        }
      }
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      if (flashCameraId != null) {
        runCatching { cameraManager?.setTorchMode(flashCameraId, false) }
      }
    }
  }

  // 2. LIVE SPLIT-LAP STOPWATCH STATE
  var timerRunning by remember { mutableStateOf(false) }
  var elapsedMs by remember { mutableLongStateOf(0L) }
  var lastLapTotalMs by remember { mutableLongStateOf(0L) }
  val laps = remember { mutableStateListOf<LapSplit>() }

  LaunchedEffect(timerRunning) {
    if (!timerRunning) return@LaunchedEffect
    var lastTick = System.currentTimeMillis()
    while (timerRunning) {
      delay(50L)
      val now = System.currentTimeMillis()
      elapsedMs += (now - lastTick).coerceAtLeast(0L)
      lastTick = now
    }
  }

  // 3. GEAR RATIO & TOP-SPEED CALCULATOR STATE
  var directHubDrive by remember { mutableStateOf(false) }
  var tireDiameterIn by remember { mutableFloatStateOf(11f) }
  var motorLoadedRpm by remember { mutableFloatStateOf(1450f) }
  var frontTeeth by remember { mutableFloatStateOf(14f) }
  var rearTeeth by remember { mutableFloatStateOf(54f) }

  // 4. TORQUE SPECS SEARCH STATE
  var torqueQuery by remember { mutableStateOf("") }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 16.dp)
          .padding(bottom = 16.dp)
          .testTag("trail_power_toolkit_sheet"),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // HEADER
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(Modifier.weight(1f)) {
          Text(
            "TRAIL POWER & SAFETY TOOLKIT",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            "Morse Torch TX/RX • Lap Timer • Gear Calc • Torque Specs",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(48.dp).testTag("trail_toolkit_close_button"),
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close Toolkit")
        }
      }

      // ZERO-CLIP 4-SLOT FITTED TAB BAR (48dp TALL EACH)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        ToolkitSegmentTab(
          icon = if (beaconMode == BeaconMode.OFF) Icons.Default.FlashlightOff else Icons.Default.FlashlightOn,
          label = "Beacon",
          selected = activeTab == 0,
          onClick = { activeTab = 0 },
          modifier = Modifier.weight(1f).testTag("toolkit_tab_beacon"),
        )
        ToolkitSegmentTab(
          icon = Icons.Default.Timer,
          label = "Laps",
          selected = activeTab == 1,
          onClick = { activeTab = 1 },
          modifier = Modifier.weight(1f).testTag("toolkit_tab_laps"),
        )
        ToolkitSegmentTab(
          icon = Icons.Default.SettingsInputComponent,
          label = "Gearing",
          selected = activeTab == 2,
          onClick = { activeTab = 2 },
          modifier = Modifier.weight(1f).testTag("toolkit_tab_gear"),
        )
        ToolkitSegmentTab(
          icon = Icons.Default.Build,
          label = "Torque",
          selected = activeTab == 3,
          onClick = { activeTab = 3 },
          modifier = Modifier.weight(1f).testTag("toolkit_tab_torque"),
        )
      }

      when (activeTab) {
        // TAB 0: QUICK TORCH + MORSE CODE FLASHLIGHT TX + CAMERA MORSE RX
        0 -> {
          Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              FilterChip(
                selected = beaconSubMode == 0,
                onClick = { beaconSubMode = 0 },
                label = { Text("Quick Torch", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                modifier = Modifier.weight(1f).testTag("beacon_sub_torch"),
              )
              FilterChip(
                selected = beaconSubMode == 1,
                onClick = {
                  beaconMode = BeaconMode.OFF
                  beaconSubMode = 1
                },
                label = { Text("Morse TX", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                modifier = Modifier.weight(1f).testTag("beacon_sub_morse_tx"),
              )
              FilterChip(
                selected = beaconSubMode == 2,
                onClick = {
                  beaconMode = BeaconMode.OFF
                  setHardwareTorch(false)
                  beaconSubMode = 2
                },
                label = { Text("Camera RX", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                modifier = Modifier.weight(1f).testTag("beacon_sub_morse_rx"),
              )
            }

            when (beaconSubMode) {
              1 -> {
                MorseFlashlightTransmitterPanel(
                  hasFlashHardware = flashCameraId != null,
                  torchError = torchError,
                  setHardwareTorch = ::setHardwareTorch,
                )
              }
              2 -> {
                MorseCameraReceiverPanel(
                  settings = settings,
                  voiceAudio = voiceAudio,
                )
              }
              else -> {
                Surface(
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                  shape = RoundedCornerShape(14.dp),
                  border =
                    BorderStroke(
                      1.dp,
                      if (beaconMode == BeaconMode.OFF) MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                      else MaterialTheme.colorScheme.primary,
                    ),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                      Icon(
                        imageVector = if (beaconMode == BeaconMode.OFF) Icons.Default.FlashlightOff else Icons.Default.FlashlightOn,
                        contentDescription = null,
                        tint = if (beaconMode == BeaconMode.OFF) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp),
                      )
                      Column(Modifier.weight(1f)) {
                        Text(
                          "Status: ${beaconMode.label.uppercase()}",
                          style = MaterialTheme.typography.titleSmall,
                          fontWeight = FontWeight.Black,
                          color = if (beaconMode == BeaconMode.OFF) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                        )
                        Text(
                          if (flashCameraId != null) "Uses device rear LED flash via Android CameraManager."
                          else "No rear LED flash detected on this device.",
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                    }

                    if (torchError != null) {
                      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Text(torchError.orEmpty(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                      }
                    }

                    // 2x2 SPACIOUS 48dp MODE BUTTONS
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      BeaconModeButton(
                        mode = BeaconMode.STEADY,
                        active = beaconMode == BeaconMode.STEADY,
                        onClick = { beaconMode = BeaconMode.STEADY },
                        modifier = Modifier.weight(1f),
                      )
                      BeaconModeButton(
                        mode = BeaconMode.STROBE,
                        active = beaconMode == BeaconMode.STROBE,
                        onClick = { beaconMode = BeaconMode.STROBE },
                        modifier = Modifier.weight(1f),
                      )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      BeaconModeButton(
                        mode = BeaconMode.SOS,
                        active = beaconMode == BeaconMode.SOS,
                        onClick = { beaconMode = BeaconMode.SOS },
                        modifier = Modifier.weight(1f),
                      )
                      OutlinedButton(
                        onClick = { beaconMode = BeaconMode.OFF },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("beacon_off_button"),
                      ) {
                        Icon(Icons.Default.FlashlightOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Turn Off", maxLines = 1, softWrap = false)
                      }
                    }
                  }
                }
              }
            }
          }
        }

        // TAB 1: LIVE SPLIT-LAP STOPWATCH
        1 -> {
          val spdUnit = speedUnitLabel(settings.distanceUnit)
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text(
                formatStopwatchMs(elapsedMs),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false,
              )
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                Text(
                  "Live Speed: ${"%.1f".format(formatSpeedValue(liveSpeedKmh, settings.distanceUnit))} $spdUnit • ${laps.size} Laps",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.secondary,
                  fontWeight = FontWeight.Bold,
                )
              }
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                  onClick = { timerRunning = !timerRunning },
                  modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("lap_timer_toggle_button"),
                ) {
                  Icon(if (timerRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(6.dp))
                  Text(if (timerRunning) "Pause" else "Start", maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                  onClick = {
                    val split = (elapsedMs - lastLapTotalMs).coerceAtLeast(0L)
                    lastLapTotalMs = elapsedMs
                    laps.add(0, LapSplit(laps.size + 1, split, elapsedMs, liveSpeedKmh))
                  },
                  enabled = elapsedMs > 0L,
                  modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("lap_timer_split_button"),
                ) {
                  Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(6.dp))
                  Text("Lap", maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                  onClick = {
                    timerRunning = false
                    elapsedMs = 0L
                    lastLapTotalMs = 0L
                    laps.clear()
                  },
                  modifier = Modifier.heightIn(min = 48.dp).testTag("lap_timer_reset_button"),
                ) {
                  Icon(Icons.Default.Refresh, contentDescription = "Reset Timer", modifier = Modifier.size(18.dp))
                }
              }
            }
          }

          if (laps.isNotEmpty()) {
            LazyColumn(
              modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              items(laps, key = { it.lapNumber }) { lap ->
                Surface(
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text("Lap ${lap.lapNumber}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Text("+${formatStopwatchMs(lap.splitMs)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("${"%.1f".format(formatSpeedValue(lap.speedKmh, settings.distanceUnit))} $spdUnit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text(formatStopwatchMs(lap.totalMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }
              }
            }
          }
        }

        // TAB 2: GEAR RATIO, SPROCKET & TIRE TOP-SPEED CALCULATOR
        2 -> {
          val gearRatio = if (directHubDrive) 1.0 else (rearTeeth.toDouble() / frontTeeth.toDouble().coerceAtLeast(1.0))
          val wheelRpm = motorLoadedRpm.toDouble() / gearRatio.coerceAtLeast(0.1)
          val wheelCircumferenceMeters = tireDiameterIn.toDouble() * 0.0254 * PI
          val topSpeedKmh = (wheelRpm * wheelCircumferenceMeters * 60.0) / 1000.0
          val topSpeedMph = topSpeedKmh * 0.621371

          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                  Text("ESTIMATED TOP SPEED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text(
                    "${"%.1f".format(topSpeedMph)} mph  (${"%.1f".format(topSpeedKmh)} km/h)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
                Column(horizontalAlignment = Alignment.End) {
                  Text("RATIO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text(
                    if (directHubDrive) "1.00:1 Hub" else "${"%.2f".format(gearRatio)}:1",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary,
                  )
                }
              }

              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Direct-Drive Hub Motor (No Chain/Belt)", style = MaterialTheme.typography.labelMedium)
                Switch(checked = directHubDrive, onCheckedChange = { directHubDrive = it })
              }

              Text("Tire Outer Diameter: ${"%.1f".format(tireDiameterIn)}\" (${(tireDiameterIn * 25.4).toInt()}mm)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              com.example.ui.components.TacticalSlider(value = tireDiameterIn, onValueChange = { tireDiameterIn = it }, valueRange = 8f..29f)

              Text(
                if (directHubDrive) "Loaded Hub RPM: ${motorLoadedRpm.toInt()} RPM"
                else "Loaded Motor Shaft RPM: ${motorLoadedRpm.toInt()} RPM",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
              )
              com.example.ui.components.TacticalSlider(value = motorLoadedRpm, onValueChange = { motorLoadedRpm = it }, valueRange = 400f..6500f)

              if (!directHubDrive) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                  Column(Modifier.weight(1f)) {
                    Text("Front: ${frontTeeth.toInt()}T", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    com.example.ui.components.TacticalSlider(value = frontTeeth, onValueChange = { frontTeeth = it }, valueRange = 10f..56f)
                  }
                  Column(Modifier.weight(1f)) {
                    Text("Rear: ${rearTeeth.toInt()}T", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    com.example.ui.components.TacticalSlider(value = rearTeeth, onValueChange = { rearTeeth = it }, valueRange = 14f..72f)
                  }
                }
              }
            }
          }
        }

        // TAB 3: SEARCHABLE PEV BOLT TORQUE REFERENCE
        else -> {
          val filteredSpecs =
            remember(torqueQuery) {
              val q = torqueQuery.trim().lowercase()
              if (q.isEmpty()) PEV_TORQUE_SPECS
              else PEV_TORQUE_SPECS.filter {
                it.component.lowercase().contains(q) || it.boltSize.lowercase().contains(q) || it.note.lowercase().contains(q)
              }
            }
          OutlinedTextField(
            value = torqueQuery,
            onValueChange = { torqueQuery = it },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Filter bolt, rotor, stem, axle...") },
            modifier = Modifier.fillMaxWidth().testTag("torque_spec_search_input"),
          )
          LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 290.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
          ) {
            items(filteredSpecs, key = { it.component }) { spec ->
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      spec.component,
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.Black,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                      spec.rangeNm,
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Black,
                      color = MaterialTheme.colorScheme.primary,
                      maxLines = 1,
                      softWrap = false,
                    )
                  }
                  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(spec.boltSize, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text(spec.rangeInLb, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                  Text(spec.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ToolkitSegmentTab(
  icon: ImageVector,
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    border =
      BorderStroke(
        width = if (selected) 1.5.dp else 1.dp,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
      ),
    shape = RoundedCornerShape(12.dp),
    modifier = modifier.heightIn(min = 52.dp),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp),
      )
      Spacer(Modifier.height(2.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun BeaconModeButton(
  mode: BeaconMode,
  active: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Button(
    onClick = onClick,
    colors =
      if (active) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
      else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
    modifier = modifier.heightIn(min = 48.dp).testTag("beacon_mode_${mode.name.lowercase()}"),
  ) {
    Icon(Icons.Default.FlashlightOn, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(6.dp))
    Text(mode.label, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
  }
}

private fun formatStopwatchMs(ms: Long): String {
  val totalTenths = ms / 100L
  val tenths = totalTenths % 10L
  val totalSec = totalTenths / 10L
  val sec = totalSec % 60L
  val min = (totalSec / 60L) % 60L
  return "%02d:%02d.%d".format(min, sec, tenths)
}
