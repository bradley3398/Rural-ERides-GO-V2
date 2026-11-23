package com.example.ui.screens

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import com.example.BuildConfig
import com.example.core.DiagnosticCheckResult
import com.example.data.ActiveSessionUser
import com.example.data.AuthProviderType
import com.example.data.LiveTelemetrySnapshot
import com.example.hardware.VoiceAudioManager
import com.example.ui.components.TacticalCard
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HardwareDiagnosticSnapshot(
  val deviceLabel: String,
  val androidRelease: String,
  val abiLabel: String,
  val hasRotationVector: Boolean,
  val hasAccelerometer: Boolean,
  val hasGyroscope: Boolean,
  val hasMagnetometer: Boolean,
  val hasBarometer: Boolean,
  val hasLightSensor: Boolean,
  val hasProximity: Boolean,
  val hasLinearAccel: Boolean,
  val hasStepCounter: Boolean,
  val hasAmbientTemp: Boolean,
  val totalSensorCount: Int,
  val batteryPct: Int,
  val batteryTempC: Float,
  val batteryVoltageMv: Int,
  val batteryHealthLabel: String,
  val batteryChargeState: String,
  val networkTransport: String,
  val networkValidated: Boolean,
  val downKbps: Int,
  val thermalStatus: String,
  val ramUsedMb: Long,
  val ramTotalMb: Long,
  val storageFreeGb: Double,
  val storageTotalGb: Double,
)

fun collectHardwareDiagnostics(ctx: Context): HardwareDiagnosticSnapshot {
  val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
  val allSensors = sm?.getSensorList(Sensor.TYPE_ALL).orEmpty()
  val hasRotVec = sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
  val hasAccel = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
  val hasGyro = sm?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
  val hasMag = sm?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
  val hasBaro = sm?.getDefaultSensor(Sensor.TYPE_PRESSURE) != null
  val hasLight = sm?.getDefaultSensor(Sensor.TYPE_LIGHT) != null
  val hasProx = sm?.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null
  val hasLinAccel = sm?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null
  val hasSteps = sm?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
  val hasAmbTemp = sm?.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null

  val batIntent = runCatching { ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) }.getOrNull()
  val level = batIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
  val scale = batIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100)?.coerceAtLeast(1) ?: 100
  val batPct = if (level >= 0) ((level * 100) / scale) else 0
  val batTempC = (batIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
  val batMv = batIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
  val batHealth =
    when (batIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)) {
      BatteryManager.BATTERY_HEALTH_GOOD -> "GOOD"
      BatteryManager.BATTERY_HEALTH_OVERHEAT -> "OVERHEAT"
      BatteryManager.BATTERY_HEALTH_DEAD -> "DEAD"
      BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "OVER-VOLTAGE"
      BatteryManager.BATTERY_HEALTH_COLD -> "COLD"
      else -> "NOMINAL"
    }
  val batStatus =
    when (batIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)) {
      BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
      BatteryManager.BATTERY_STATUS_FULL -> "Full"
      BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
      else -> "Idle"
    }

  val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
  val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
  val transport =
    when {
      caps == null -> "Offline"
      caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
      caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
      caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
      caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
      else -> "Connected"
    }
  val netValidated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
  val downKbps = caps?.linkDownstreamBandwidthKbps ?: 0

  val pm = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager
  val thermalLabel =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && pm != null) {
      when (pm.currentThermalStatus) {
        PowerManager.THERMAL_STATUS_NONE -> "COOL (NORMAL)"
        PowerManager.THERMAL_STATUS_LIGHT -> "WARM (LIGHT)"
        PowerManager.THERMAL_STATUS_MODERATE -> "MODERATE"
        PowerManager.THERMAL_STATUS_SEVERE -> "HOT (THROTTLING)"
        PowerManager.THERMAL_STATUS_CRITICAL -> "CRITICAL"
        PowerManager.THERMAL_STATUS_EMERGENCY -> "EMERGENCY"
        else -> "NORMAL"
      }
    } else "NORMAL"

  val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
  val memInfo = ActivityManager.MemoryInfo()
  am?.getMemoryInfo(memInfo)
  val totalMb = (memInfo.totalMem / (1024L * 1024L)).coerceAtLeast(1L)
  val availMb = (memInfo.availMem / (1024L * 1024L)).coerceAtLeast(0L)
  val usedMb = (totalMb - availMb).coerceAtLeast(0L)

  val stat = runCatching { StatFs(Environment.getDataDirectory().path) }.getOrNull()
  val totalGb = (stat?.totalBytes ?: 0L) / (1024.0 * 1024.0 * 1024.0)
  val freeGb = (stat?.availableBytes ?: 0L) / (1024.0 * 1024.0 * 1024.0)

  return HardwareDiagnosticSnapshot(
    deviceLabel = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}",
    androidRelease = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    abiLabel = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
    hasRotationVector = hasRotVec,
    hasAccelerometer = hasAccel,
    hasGyroscope = hasGyro,
    hasMagnetometer = hasMag,
    hasBarometer = hasBaro,
    hasLightSensor = hasLight,
    hasProximity = hasProx,
    hasLinearAccel = hasLinAccel,
    hasStepCounter = hasSteps,
    hasAmbientTemp = hasAmbTemp,
    totalSensorCount = allSensors.size,
    batteryPct = batPct,
    batteryTempC = batTempC,
    batteryVoltageMv = batMv,
    batteryHealthLabel = batHealth,
    batteryChargeState = batStatus,
    networkTransport = transport,
    networkValidated = netValidated,
    downKbps = downKbps,
    thermalStatus = thermalLabel,
    ramUsedMb = usedMb,
    ramTotalMb = totalMb,
    storageFreeGb = freeGb,
    storageTotalGb = totalGb,
  )
}

@Composable
fun SettingsAccountAndSecurityCard(
  activeUser: ActiveSessionUser?,
  onLinkGoogleAccount: () -> Unit,
  onRefreshSupabaseSession: () -> Unit,
) {
  val ctx = LocalContext.current
  TacticalCard(highlight = true) {
    Text(
      "SIGNED-IN ACCOUNT & SECURITY",
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(6.dp))
    Text(
      when (activeUser?.provider) {
        AuthProviderType.GOOGLE -> "Signed in with Google + Firebase Cloud (${activeUser.email.ifBlank { activeUser.uid }})"
        AuthProviderType.SUPABASE -> "Signed in with Supabase (${activeUser.email.ifBlank { activeUser.uid }})"
        null -> "Not signed in"
      },
      style = MaterialTheme.typography.bodyMedium,
    )
    if (activeUser?.provider == AuthProviderType.SUPABASE) {
      Spacer(Modifier.height(8.dp))
      OutlinedButton(
        onClick = onRefreshSupabaseSession,
        modifier = Modifier.fillMaxWidth().testTag("refresh_supabase_jwt_button"),
      ) {
        Icon(Icons.Default.Refresh, null)
        Spacer(Modifier.width(8.dp))
        Text("Refresh Cloud Login Token")
      }
    }
    if (activeUser != null && !activeUser.firebaseAuthenticated) {
      Spacer(Modifier.height(8.dp))
      val activity = ctx as? Activity
      Button(
        onClick = { if (activity != null) onLinkGoogleAccount() },
        modifier = Modifier.fillMaxWidth().testTag("link_google_account_button"),
      ) {
        Icon(Icons.Default.Link, null)
        Spacer(Modifier.width(8.dp))
        Text("Link Google Account for Community & Backup")
      }
    }
  }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun SettingsDiagnosticsAndCacheSection(
  diagnosticResults: List<DiagnosticCheckResult>,
  diagnosticsBusy: Boolean,
  onRunDiagnostics: () -> Unit,
  onStatusMessage: (String) -> Unit,
  telemetry: LiveTelemetrySnapshot? = null,
  voiceAudio: VoiceAudioManager? = null,
) {
  val ctx = LocalContext.current
  val scope = rememberCoroutineScope()
  var cacheBytes by remember { mutableLongStateOf(calculateCacheBytes(ctx)) }
  var clearing by remember { mutableStateOf(false) }
  var hwSnap by remember { mutableStateOf(collectHardwareDiagnostics(ctx)) }

  // 1. LIVE GPS SATELLITES, SENSORS & PHONE HEALTH
  TacticalCard(highlight = true) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(Modifier.weight(1f)) {
        Text(
          "PHONE HEALTH, SATELLITES & SENSORS",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Black,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          "${hwSnap.deviceLabel} • ${hwSnap.androidRelease} (${hwSnap.abiLabel})",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      OutlinedButton(
        onClick = {
          hwSnap = collectHardwareDiagnostics(ctx)
          onStatusMessage("Refreshed live hardware, satellite, and sensor readings.")
        },
        modifier = Modifier.testTag("refresh_hardware_diag_button"),
      ) {
        Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Scan")
      }
    }

    Spacer(Modifier.height(12.dp))

    // Live GPS Satellite Status Row
    if (telemetry != null) {
      val satSummary =
        if (telemetry.satellitesVisible > 0) {
          "${telemetry.satellitesUsed} Connected / ${telemetry.satellitesVisible} Visible • Signal ${"%.1f".format(telemetry.avgSatelliteSnrDb)} dB-Hz" +
            if (telemetry.satelliteConstellations.isNotBlank()) " (${telemetry.satelliteConstellations})" else ""
        } else if (telemetry.lat != 0.0 || telemetry.lng != 0.0) {
          "GPS Fix Active (±${telemetry.gpsAccuracyM.toInt()}m) • Waiting for satellite list"
        } else {
          "No GPS lock yet — step outdoors or enable Location permission"
        }
      DiagRow(
        icon = Icons.Default.SatelliteAlt,
        title = "GPS Satellites Connected",
        value = satSummary,
        ok = telemetry.satellitesUsed >= 4 || telemetry.lat != 0.0,
      )
      HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

      val liveEnvText =
        buildList {
          if (telemetry.barometerHpa > 0f) add("Pressure: ${"%.1f".format(telemetry.barometerHpa)} hPa (${telemetry.baroAltitudeM.toInt()}m)")
          if (telemetry.ambientLightLux >= 0f) add("Light: ${telemetry.ambientLightLux.toInt()} lux")
          add("Road Bump: ${"%.2f".format(telemetry.roadRoughnessG)}G")
          if (telemetry.magMicroTesla > 0f) add("Compass Field: ${telemetry.magMicroTesla.toInt()} µT")
          if (telemetry.ambientTempC != null) add("Air Temp: ${"%.1f".format(telemetry.ambientTempC)}°C")
          if (telemetry.stepCountSession > 0) add("Steps: ${telemetry.stepCountSession}")
        }.joinToString(" • ")
      DiagRow(
        icon = Icons.Default.Sensors,
        title = "Live Environment & Road Sensors",
        value = liveEnvText,
        ok = telemetry.activeSensorsList.isNotEmpty(),
      )
      HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    }

    // Motion & Environment Hardware Sensor Checklist
    DiagRow(
      icon = Icons.Default.CompassCalibration,
      title = "Motion & Tilt Sensors (${hwSnap.totalSensorCount} total on phone)",
      value =
        listOf(
          "3D Tilt:${if (hwSnap.hasRotationVector) "OK" else "No"}",
          "Accel:${if (hwSnap.hasAccelerometer) "OK" else "No"}",
          "Gyro:${if (hwSnap.hasGyroscope) "OK" else "No"}",
          "Compass:${if (hwSnap.hasMagnetometer) "OK" else "No"}",
          "Vibration:${if (hwSnap.hasLinearAccel) "OK" else "No"}",
        ).joinToString(" • "),
      ok = hwSnap.hasRotationVector || (hwSnap.hasAccelerometer && hwSnap.hasMagnetometer),
    )
    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

    DiagRow(
      icon = Icons.Default.Sensors,
      title = "Environment Hardware Sensors",
      value =
        listOf(
          "Barometer:${if (hwSnap.hasBarometer) "OK" else "No"}",
          "Light:${if (hwSnap.hasLightSensor) "OK" else "No"}",
          "Proximity:${if (hwSnap.hasProximity) "OK" else "No"}",
          "Steps:${if (hwSnap.hasStepCounter) "OK" else "No"}",
          "AirTemp:${if (hwSnap.hasAmbientTemp) "OK" else "No"}",
        ).joinToString(" • "),
      ok = hwSnap.hasBarometer || hwSnap.hasLightSensor || hwSnap.hasProximity,
    )
    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

    // Battery & Power Health
    DiagRow(
      icon = Icons.Default.BatteryChargingFull,
      title = "Phone Battery & Temperature",
      value = "${hwSnap.batteryPct}% (${hwSnap.batteryChargeState}) • ${"%.1f".format(hwSnap.batteryTempC)}°C • ${"%.2f".format(hwSnap.batteryVoltageMv / 1000f)}V • ${hwSnap.batteryHealthLabel}",
      ok = hwSnap.batteryHealthLabel == "GOOD" || hwSnap.batteryHealthLabel == "NOMINAL",
    )
    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

    // Network Transport
    DiagRow(
      icon = Icons.Default.NetworkCheck,
      title = "Internet Connection",
      value = "${hwSnap.networkTransport} • ${if (hwSnap.networkValidated) "Online" else "Checking"} • ${hwSnap.downKbps / 1000} Mbps",
      ok = hwSnap.networkValidated,
    )
    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

    // Thermal & Memory
    DiagRow(
      icon = Icons.Default.Thermostat,
      title = "Phone Heat Level",
      value = hwSnap.thermalStatus,
      ok = hwSnap.thermalStatus.startsWith("COOL") || hwSnap.thermalStatus.startsWith("WARM") || hwSnap.thermalStatus == "NORMAL",
    )
    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

    DiagRow(
      icon = Icons.Default.Memory,
      title = "Memory & Free Storage",
      value = "RAM: ${hwSnap.ramUsedMb}/${hwSnap.ramTotalMb} MB • Free Space: ${"%.1f".format(hwSnap.storageFreeGb)}/${"%.1f".format(hwSnap.storageTotalGb)} GB",
      ok = hwSnap.storageFreeGb > 1.0,
    )

    if (voiceAudio != null) {
      Spacer(Modifier.height(12.dp))
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = {
            val ok = voiceAudio.triggerHapticCue(true, VoiceAudioManager.HapticCue.BOOT_SWEEP)
            onStatusMessage(if (ok) "Haptic motor test passed!" else "No vibration motor detected on this device.")
          },
          modifier = Modifier.weight(1f).testTag("diag_test_haptics_button"),
        ) {
          Icon(Icons.Default.Vibration, null, Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("Test Haptics", maxLines = 1, softWrap = false)
        }
        OutlinedButton(
          onClick = {
            val sats = telemetry?.satellitesUsed ?: 0
            val spoken =
              voiceAudio.speakText(
                "Rural E Rides Go diagnostics check. Phone battery is ${hwSnap.batteryPct} percent. $sats GPS satellites connected. Speakers working.",
                volumePct = 90,
              )
            onStatusMessage(if (spoken) "Playing speaker & voice test..." else "Voice engine busy — try again.")
          },
          modifier = Modifier.weight(1f).testTag("diag_test_voice_button"),
        ) {
          Icon(Icons.Default.RecordVoiceOver, null, Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("Test Speaker", maxLines = 1, softWrap = false)
        }
      }
    }

    Spacer(Modifier.height(10.dp))
    OutlinedButton(
      onClick = {
        val report =
          buildString {
            appendLine("=== RURAL ERIDES GO v${BuildConfig.VERSION_NAME} DIAGNOSTIC REPORT ===")
            appendLine("Developer: Lord Bradley Callison")
            appendLine("Device: ${hwSnap.deviceLabel} | ${hwSnap.androidRelease} (${hwSnap.abiLabel})")
            if (telemetry != null) {
              appendLine("GPS Satellites: ${telemetry.satellitesUsed} locked / ${telemetry.satellitesVisible} visible (${telemetry.satelliteConstellations}) | Avg SNR=${"%.1f".format(telemetry.avgSatelliteSnrDb)} dB")
              appendLine("Environment: Baro=${"%.1f".format(telemetry.barometerHpa)}hPa | Light=${telemetry.ambientLightLux.toInt()}lux | RoadBump=${"%.2f".format(telemetry.roadRoughnessG)}G")
            }
            appendLine("Sensors (${hwSnap.totalSensorCount}): RotVec=${hwSnap.hasRotationVector}, Accel=${hwSnap.hasAccelerometer}, Gyro=${hwSnap.hasGyroscope}, Mag=${hwSnap.hasMagnetometer}, Baro=${hwSnap.hasBarometer}, Light=${hwSnap.hasLightSensor}, Prox=${hwSnap.hasProximity}")
            appendLine("Battery: ${hwSnap.batteryPct}% (${hwSnap.batteryChargeState}), ${hwSnap.batteryTempC}°C, ${hwSnap.batteryVoltageMv}mV, ${hwSnap.batteryHealthLabel}")
            appendLine("Network: ${hwSnap.networkTransport} (validated=${hwSnap.networkValidated}, ${hwSnap.downKbps}kbps)")
            appendLine("Thermal: ${hwSnap.thermalStatus}")
            appendLine("Memory: ${hwSnap.ramUsedMb}/${hwSnap.ramTotalMb} MB | Disk Free: ${"%.1f".format(hwSnap.storageFreeGb)} GB")
            if (diagnosticResults.isNotEmpty()) {
              appendLine("\n--- Cloud & Online Services ---")
              diagnosticResults.forEach { r ->
                appendLine("[${if (r.ok) "PASS" else "FAIL"}] ${r.name} (${r.category}) ${r.latencyMs}ms: ${r.detail}")
              }
            }
          }
        val intent =
          Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Rural ERides GO v${BuildConfig.VERSION_NAME} Diagnostic Report")
            putExtra(Intent.EXTRA_TEXT, report)
          }
        runCatching { ctx.startActivity(Intent.createChooser(intent, "Share Diagnostic Report")) }
      },
      modifier = Modifier.fillMaxWidth().testTag("export_diagnostics_report_button"),
    ) {
      Icon(Icons.Default.Share, null, Modifier.size(16.dp))
      Spacer(Modifier.width(8.dp))
      Text("Share Full Health Report")
    }
  }

  Spacer(Modifier.height(14.dp))

  // 2. LIVE ONLINE SERVICES & CLOUD CHECK
  TacticalCard(highlight = true) {
    Text(
      "ONLINE SERVICES & CLOUD CONNECTION TEST",
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Black,
      color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(8.dp))
    Button(
      onClick = onRunDiagnostics,
      enabled = !diagnosticsBusy,
      modifier = Modifier.fillMaxWidth().testTag("run_live_diagnostics_button"),
    ) {
      if (diagnosticsBusy) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(8.dp))
        Text("Testing Online Services...")
      } else {
        Icon(Icons.Default.NetworkCheck, null)
        Spacer(Modifier.width(8.dp))
        Text("Run Live Connection Check")
      }
    }
    if (diagnosticResults.isNotEmpty()) {
      val passCount = diagnosticResults.count { it.ok }
      Spacer(Modifier.height(8.dp))
      Text(
        "SERVICES ONLINE: $passCount / ${diagnosticResults.size}",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = if (passCount == diagnosticResults.size) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
      )
      Spacer(Modifier.height(6.dp))
      diagnosticResults.forEach { res ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(
              if (res.ok) Icons.Default.CheckCircle else Icons.Default.Error,
              contentDescription = null,
              tint = if (res.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
              modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Column {
              Text("${res.name} (${res.category})", style = MaterialTheme.typography.labelLarge)
              Text(res.detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          if (res.latencyMs > 0L) {
            Text("${res.latencyMs}ms", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
          }
        }
        Spacer(Modifier.height(4.dp))
      }
    }
  }

  Spacer(Modifier.height(14.dp))

  // 3. STORAGE & TEMPORARY FILES
  TacticalCard {
    Text(
      "STORAGE & CLEAR TEMPORARY FILES",
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(6.dp))
    Text(
      "Saved Map Tiles & Image Cache: ${"%.2f".format(cacheBytes / (1024.0 * 1024.0))} MB",
      style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
      onClick = {
        clearing = true
        scope.launch {
          clearAppCaches(ctx)
          cacheBytes = calculateCacheBytes(ctx)
          clearing = false
          onStatusMessage("Cleared cached images and map tiles.")
        }
      },
      enabled = !clearing,
      modifier = Modifier.fillMaxWidth().testTag("clear_cache_button"),
    ) {
      Icon(Icons.Default.CleaningServices, null)
      Spacer(Modifier.width(8.dp))
      Text(if (clearing) "Clearing..." else "Clear Image & Map Cache")
    }
  }
}

@Composable
private fun DiagRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  value: String,
  ok: Boolean,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
      modifier = Modifier.size(20.dp),
    )
    Column(Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
      Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Icon(
      imageVector = if (ok) Icons.Default.CheckCircle else Icons.Default.Error,
      contentDescription = if (ok) "OK" else "Check",
      tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
      modifier = Modifier.size(18.dp),
    )
  }
}

private fun calculateCacheBytes(ctx: Context): Long {
  val dirs = listOfNotNull(ctx.cacheDir, ctx.externalCacheDir)
  return dirs.sumOf { dirSize(it) }
}

private fun dirSize(file: File): Long {
  if (!file.exists()) return 0L
  if (file.isFile) return file.length()
  return file.listFiles()?.sumOf { dirSize(it) } ?: 0L
}

@OptIn(ExperimentalCoilApi::class)
private suspend fun clearAppCaches(ctx: Context) =
  withContext(Dispatchers.IO) {
    runCatching {
      val loader = Coil.imageLoader(ctx)
      loader.memoryCache?.clear()
      loader.diskCache?.clear()
    }
    runCatching {
      ctx.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
      ctx.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
    }
  }
