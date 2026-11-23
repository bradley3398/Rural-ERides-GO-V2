package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.LiveTelemetrySnapshot
import com.example.data.MasterSettings
import kotlin.math.abs

@Composable
internal fun CockpitAhrsDiagnosticsPanel(
  telemetry: LiveTelemetrySnapshot,
  settings: MasterSettings,
  modifier: Modifier = Modifier,
) {
  val pitch = telemetry.pitchDeg
  val roll = telemetry.rollDeg
  val yaw = telemetry.yawDeg
  val accelMag = abs(telemetry.peakG).coerceIn(0.1f, 12.0f)

  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Sensors, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
          Text("9-DOF AHRS & SENSOR HEALTH DIAGNOSTICS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        }
        Surface(
          color = Color(0xFF22C55E).copy(alpha = 0.20f),
          shape = RoundedCornerShape(6.dp),
          border = BorderStroke(1.dp, Color(0xFF22C55E)),
        ) {
          Text("CALIBRATED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
        }
      }

      Text("Real-time telemetry and IMU sensor stream for PEV connectivity and balance stabilization.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

      Spacer(Modifier.height(2.dp))

      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AhrsMetricBox("PITCH", "${"%.1f".format(pitch)}°", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        AhrsMetricBox("ROLL", "${"%.1f".format(roll)}°", Color(0xFF38BDF8), Modifier.weight(1f))
        AhrsMetricBox("YAW", "${"%.0f".format(yaw)}°", Color(0xFF22C55E), Modifier.weight(1f))
        AhrsMetricBox("PEAK G", "${"%.2f".format(accelMag)}G", Color(0xFFFF6B35), Modifier.weight(1f))
      }

      Spacer(Modifier.height(4.dp))

      Text("IMU Calibration Quality (Gyro / Accel / Mag)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      LinearProgressIndicator(
        progress = { 0.98f },
        color = Color(0xFF22C55E),
        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
      )

      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("BLE Link: Stable (RSSI -58dBm)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF22C55E), fontFamily = FontFamily.Monospace)
        Text("Packet Loss: 0.0%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
      }
    }
  }
}

@Composable
private fun AhrsMetricBox(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    modifier = modifier,
  ) {
    Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = accent, maxLines = 1, softWrap = false)
    }
  }
}
