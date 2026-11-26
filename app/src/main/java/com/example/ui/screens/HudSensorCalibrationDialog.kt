package com.example.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.LiveTelemetrySnapshot
import com.example.ui.components.TacticalDialog
import kotlin.math.abs

private data class HardwareSensorDetail(
  val name: String,
  val vendor: String,
  val category: String,
  val powerMa: Float,
  val maxRange: Float,
  val isLiveSubscribed: Boolean,
)

/**
 * Interactive HUD Hardware Sensor Info & Multi-Axis Calibration Dialog.
 * Displays live hardware sensor inventory from Android SensorManager, real-time 9-DOF AHRS
 * spirit-level bubble, GNSS & environmental readouts, and one-tap level/3-axis/factory calibration.
 */
@Composable
fun HudSensorCalibrationDialog(
  telemetry: LiveTelemetrySnapshot,
  onZeroPitchRoll: () -> Unit,
  onZeroAllAxes: () -> Unit,
  onResetPeakLean: () -> Unit,
  onFactoryResetOffsets: () -> Unit,
  onRefreshGps: () -> Unit,
  onDismiss: () -> Unit,
) {
  val ctx = LocalContext.current
  val cs = MaterialTheme.colorScheme

  var calibrationFeedback by remember { mutableStateOf("") }
  var showFullSensorList by remember { mutableStateOf(false) }

  val hardwareSensors =
    remember(telemetry.activeSensorsList) {
      val sm = runCatching { ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }.getOrNull()
      val all = runCatching { sm?.getSensorList(Sensor.TYPE_ALL).orEmpty() }.getOrDefault(emptyList())
      val activeNames = telemetry.activeSensorsList.map { it.lowercase() }.toSet()
      all.map { s ->
        val category =
          when (s.type) {
            Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_LINEAR_ACCELERATION, Sensor.TYPE_GRAVITY -> "Accelerometer / IMU"
            Sensor.TYPE_GYROSCOPE, Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> "Gyroscope"
            Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GAME_ROTATION_VECTOR, Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> "9-DOF Fusion"
            Sensor.TYPE_MAGNETIC_FIELD, Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> "Magnetometer / Compass"
            Sensor.TYPE_PRESSURE -> "Barometer"
            Sensor.TYPE_LIGHT -> "Ambient Light"
            Sensor.TYPE_PROXIMITY -> "Proximity"
            Sensor.TYPE_AMBIENT_TEMPERATURE -> "Temperature"
            Sensor.TYPE_STEP_COUNTER, Sensor.TYPE_STEP_DETECTOR -> "Pedometer"
            else -> s.stringType.substringAfterLast('.').replace('_', ' ').uppercase().ifBlank { "Hardware Sensor" }
          }
        val subscribed =
          activeNames.any { act ->
            category.lowercase().contains(act.take(5)) || s.name.lowercase().contains(act.take(5))
          } ||
            s.type in
            setOf(
              Sensor.TYPE_ROTATION_VECTOR,
              Sensor.TYPE_ACCELEROMETER,
              Sensor.TYPE_LINEAR_ACCELERATION,
              Sensor.TYPE_GRAVITY,
              Sensor.TYPE_GYROSCOPE,
              Sensor.TYPE_MAGNETIC_FIELD,
              Sensor.TYPE_PRESSURE,
              Sensor.TYPE_LIGHT,
              Sensor.TYPE_PROXIMITY,
              Sensor.TYPE_AMBIENT_TEMPERATURE,
              Sensor.TYPE_STEP_COUNTER,
            )
        HardwareSensorDetail(
          name = s.name.ifBlank { category },
          vendor = s.vendor.ifBlank { "System" },
          category = category,
          powerMa = s.power,
          maxRange = s.maximumRange,
          isLiveSubscribed = subscribed,
        )
      }
    }

  val isLevel = abs(telemetry.pitchDeg) <= 1.8f && abs(telemetry.rollDeg) <= 1.8f

  TacticalDialog(
    onDismissRequest = onDismiss,
    title = "SENSOR INFO & CALIBRATION",
    subtitle = "${telemetry.activeSensorsList.size} Active Telemetry Feeds • ${hardwareSensors.size} Hardware Sensors",
    icon = Icons.Default.Sensors,
    badgeText = if (isLevel) "LEVEL 0°" else "LIVE IMU",
    confirmButton = {
      OutlinedButton(
        onClick = onDismiss,
        modifier = Modifier.heightIn(min = 48.dp).testTag("close_sensor_calibration_dialog"),
      ) {
        Text("Done")
      }
    },
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      contentPadding = PaddingValues(bottom = 6.dp),
    ) {
      // 1. LIVE SPIRIT-LEVEL BUBBLE & 9-DOF ATTITUDE MONITOR
      item(key = "level_bubble") {
        Surface(
          color = cs.surfaceVariant.copy(alpha = 0.55f),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Box(
              modifier =
                Modifier
                  .size(86.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF0A121C))
                  .border(
                    width = 2.dp,
                    color = if (isLevel) cs.primary else cs.secondary,
                    shape = CircleShape,
                  ),
              contentAlignment = Alignment.Center,
            ) {
              Canvas(modifier = Modifier.size(86.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val r = size.minDimension / 2f
                // Crosshairs & inner level target ring
                drawLine(Color.White.copy(alpha = 0.22f), Offset(0f, center.y), Offset(size.width, center.y), 1.5f)
                drawLine(Color.White.copy(alpha = 0.22f), Offset(center.x, 0f), Offset(center.x, size.height), 1.5f)
                drawCircle(
                  color = if (isLevel) Color(0xFF39FF14) else Color(0xFF38BDF8),
                  radius = r * 0.32f,
                  center = center,
                  style = Stroke(width = 2f),
                )
                // Live tilt bubble clamped within circle
                val dx = (telemetry.rollDeg / 45f).coerceIn(-1f, 1f) * (r * 0.68f)
                val dy = (telemetry.pitchDeg / 45f).coerceIn(-1f, 1f) * (r * 0.68f)
                drawCircle(
                  color = if (isLevel) Color(0xFF39FF14) else Color(0xFFFFB703),
                  radius = 10f,
                  center = Offset(center.x + dx, center.y + dy),
                )
              }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = if (isLevel) "MOUNT LEVEL VERIFIED" else "HOLD RIG UPRIGHT TO CALIBRATE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = if (isLevel) cs.primary else cs.secondary,
              )
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SensorMetricCell("PITCH", "${"%.1f".format(telemetry.pitchDeg)}°")
                SensorMetricCell("ROLL", "${"%.1f".format(telemetry.rollDeg)}°")
                SensorMetricCell("YAW", "${"%.0f".format(telemetry.yawDeg)}°")
              }
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SensorMetricCell("G-FORCE", "${"%.2f".format(telemetry.gForceTotal)}G")
                SensorMetricCell("PEAK L/R", "${telemetry.peakLeanLeftDeg.toInt()}°/${telemetry.peakLeanRightDeg.toInt()}°")
                SensorMetricCell("BUMP", "${"%.2f".format(telemetry.roadRoughnessG)}G")
              }
            }
          }
        }
      }

      if (calibrationFeedback.isNotBlank()) {
        item(key = "cal_feedback") {
          Surface(
            color = cs.primaryContainer.copy(alpha = 0.85f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = cs.primary, modifier = Modifier.size(18.dp))
              Text(
                calibrationFeedback,
                style = MaterialTheme.typography.labelMedium,
                color = cs.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }

      // 2. ONE-TAP CALIBRATION CONTROLS
      item(key = "cal_actions") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            "CALIBRATION ACTIONS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = cs.primary,
          )
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = {
                onZeroPitchRoll()
                calibrationFeedback = "Pitch & Roll zeroed to current mount angle."
              },
              modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("cal_zero_pitch_roll_btn"),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            ) {
              Icon(Icons.Default.CompassCalibration, null, Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("Zero Level", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(
              onClick = {
                onZeroAllAxes()
                calibrationFeedback = "All 3 AHRS axes (Pitch, Roll & Heading) calibrated."
              },
              colors = ButtonDefaults.buttonColors(containerColor = cs.secondary),
              modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("cal_zero_all_axes_btn"),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            ) {
              Icon(Icons.Default.Explore, null, Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("3-Axis + Yaw", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = {
                onResetPeakLean()
                calibrationFeedback = "Peak left & right lean angle memory cleared."
              },
              modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("cal_reset_peaks_btn"),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            ) {
              Icon(Icons.Default.Tune, null, Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Clear Peaks", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
              onClick = {
                onFactoryResetOffsets()
                calibrationFeedback = "Restored raw factory hardware sensor offsets."
              },
              modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("cal_factory_reset_btn"),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            ) {
              Icon(Icons.Default.RestartAlt, null, Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Factory Zero", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
        }
      }

      // 3. LIVE GNSS & ENVIRONMENTAL TELEMETRY READOUTS
      item(key = "env_gnss") {
        Surface(
          color = cs.surfaceVariant.copy(alpha = 0.45f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.SatelliteAlt, null, tint = cs.primary, modifier = Modifier.size(16.dp))
                Text("GNSS & ENVIRONMENT SENSORS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = cs.primary)
              }
              OutlinedButton(
                onClick = {
                  onRefreshGps()
                  calibrationFeedback = "Requested fresh GNSS satellite lock & sensor poll."
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.heightIn(min = 36.dp).testTag("cal_refresh_gps_btn"),
              ) {
                Icon(Icons.Default.Refresh, null, Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Re-Lock GPS", style = MaterialTheme.typography.labelSmall)
              }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              SensorMetricCell(
                "SATELLITES",
                "${telemetry.satellitesUsed}/${telemetry.satellitesVisible}",
              )
              SensorMetricCell(
                "GPS ACC",
                if (telemetry.gpsAccuracyM > 0f) "±${telemetry.gpsAccuracyM.toInt()}m" else "Scanning",
              )
              SensorMetricCell(
                "BAROMETER",
                if (telemetry.barometerHpa > 0f) "${"%.1f".format(telemetry.barometerHpa)} hPa" else "N/A",
              )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              SensorMetricCell(
                "MAGNETIC",
                if (telemetry.magMicroTesla > 0f) "${telemetry.magMicroTesla.toInt()} µT" else "N/A",
              )
              SensorMetricCell(
                "LIGHT",
                if (telemetry.ambientLightLux >= 0f) "${telemetry.ambientLightLux.toInt()} lux" else "N/A",
              )
              SensorMetricCell(
                "ALTITUDE",
                "${telemetry.altitudeM.toInt()} m",
              )
            }
            if (telemetry.activeSensorsList.isNotEmpty()) {
              Text(
                "Active Cockpit Feeds (${telemetry.activeSensorsList.size}): ${telemetry.activeSensorsList.joinToString(" • ")}",
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
              )
            }
          }
        }
      }

      // 4. EXPANDABLE FULL HARDWARE SENSOR INVENTORY
      item(key = "inventory_header") {
        Row(
          Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            "DEVICE HARDWARE SENSORS (${hardwareSensors.size})",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = cs.secondary,
          )
          OutlinedButton(
            onClick = { showFullSensorList = !showFullSensorList },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.heightIn(min = 38.dp).testTag("toggle_full_sensor_inventory_btn"),
          ) {
            Text(
              if (showFullSensorList) "Show Core (${hardwareSensors.count { it.isLiveSubscribed }})" else "Show All (${hardwareSensors.size})",
              style = MaterialTheme.typography.labelSmall,
            )
          }
        }
      }

      val displayedSensors =
        if (showFullSensorList) hardwareSensors
        else hardwareSensors.filter { it.isLiveSubscribed }.ifEmpty { hardwareSensors.take(8) }

      items(displayedSensors) { sensor ->
        Surface(
          color = cs.surface.copy(alpha = 0.85f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                sensor.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                "${sensor.category} • ${sensor.vendor}",
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                if (sensor.isLiveSubscribed) "ACTIVE" else "READY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = if (sensor.isLiveSubscribed) cs.primary else cs.onSurfaceVariant,
              )
              Text(
                "${"%.2f".format(sensor.powerMa)} mA",
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SensorMetricCell(label: String, value: String) {
  Column {
    Text(
      label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      value,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.Black,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}
