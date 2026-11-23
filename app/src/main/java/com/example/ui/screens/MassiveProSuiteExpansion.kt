package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.MasterSettings
import com.example.ui.components.TacticalTopBar
import com.example.ui.theme.LocalTacticalChrome

data class ProFeatureItem(
  val title: String,
  val category: String,
  val description: String,
  val specDetail: String,
)

@Composable
internal fun MassiveProSuiteExpansionScreen(
  settings: MasterSettings,
  onNavigateBack: () -> Unit,
) {
  val chrome = LocalTacticalChrome.current
  val items = remember {
    listOf(
      // Direct Messages Pro
      ProFeatureItem("End-to-End Encrypted Trail Channel", "Direct Messages", "Quantum-resistant secure messaging protocol for backcountry riding squads.", "AES-256-GCM Secure"),
      ProFeatureItem("Live GPS Telemetry Ping in Chat", "Direct Messages", "Share live map coordinates and battery state directly inside chat threads.", "Real-Time Mesh"),
      ProFeatureItem("Audio Walkie-Talkie Voice Notes", "Direct Messages", "Push-to-talk voice recording transmission directly across direct message channels.", "Opus 64kbps"),
      ProFeatureItem("Remote Diagnostic Log Sharing", "Direct Messages", "Attach raw CAN-bus error codes and battery logs to mechanic DMs instantly.", "JSON / CSV Sync"),

      // Mechanic & Engineering Bay
      ProFeatureItem("Brushless Motor Stator Impedance Analyzer", "Mechanic Lab", "Measures winding resistance and phase inductance for custom motor rewinds.", "0.01mΩ Precision"),
      ProFeatureItem("ESC MOSFET Thermal Stress Profiler", "Mechanic Lab", "Monitors temperature spikes across dual VESC controllers during heavy hill climbs.", "Real-Time Thermals"),
      ProFeatureItem("Hall Sensor Commutation Oscilloscope", "Mechanic Lab", "Live graphical waveform verification of UVW Hall sensor signals.", "100Hz Sampling"),
      ProFeatureItem("Battery Cell Balancing & IR Diagnostics", "Mechanic Lab", "Identifies degraded P-groups and measures internal resistance per cell block.", "Active Balancing"),

      // Copilot AI Suite
      ProFeatureItem("Gemini AI Trail Riding Coach", "Copilot AI", "Real-time voice and text coaching on throttle management, leaning, and energy economy.", "Gemini 3.8 Flash"),
      ProFeatureItem("Automated Weather Route Risk Assessor", "Copilot AI", "Analyzes barometric pressure drops and wind gusts along planned backcountry tracks.", "Live Radar Sync"),
      ProFeatureItem("Smart Suspension Sag Advisor", "Copilot AI", "Recommends exact preload and rebound clicks based on trail terrain and payload.", "AI Expert System"),
      ProFeatureItem("Dynamic Range & Elevation Estimator", "Copilot AI", "Predicts exact remaining range factoring in headwind, grade, and throttle aggression.", "ML Predictive"),

      // Spec & Engine Parts Tab
      ProFeatureItem("High-Voltage FOC Controller Spec Matrix", "Engine Parts", "Technical specifications for 72V-96V sine-wave and square-wave motor controllers.", "Up to 400A Phase"),
      ProFeatureItem("Custom Battery Chemistry Selector", "Engine Parts", "Parameters for LiFePO4, NMC, and Solid-State cells with custom C-rate limits.", "BMS Integrated"),
      ProFeatureItem("Heavy-Duty Off-Road Axle & Hub Specs", "Engine Parts", "Reinforced axle brackets, torque arms, and custom rim lacing guidelines.", "Grade 12.9 Steel"),
      ProFeatureItem("Regenerative Braking Power Modules", "Engine Parts", "Capacitor bank upgrades and braking energy recovery configuration specs.", "Up to 35kW Regen"),

      // Dashboard & Diagnostics
      ProFeatureItem("Multi-Gauge Custom HUD Pods", "Dashboard", "Configure custom telemetry widgets (volt-amp meters, G-force, incline pitch).", "14 HUD Skins"),
      ProFeatureItem("IMU G-Force & Lean Angle Logger", "Dashboard", "Real-time acceleration, braking Gs, and cornering lean angle telemetry.", "10Hz High-Speed"),
      ProFeatureItem("Geofenced Speed Limiter Profile Switcher", "Dashboard", "Automatically switches between street legal (20mph) and off-road unlimited modes.", "GPS Geofence"),
      ProFeatureItem("Advanced Developer CAN-Bus Inspector", "Diagnostics", "Raw packet capture and filter inspection for custom PEV telemetry buses.", "CAN 2.0B / FD"),
    )
  }

  Scaffold(
    topBar = {
      TacticalTopBar(
        title = "Massive Pro Suite Feature Expansion",
        onBack = onNavigateBack,
        icon = Icons.Default.Engineering,
        badgeText = "${items.size} SPECS",
      )
    },
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      items(items) { item ->
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
          shape = chrome.cardShape,
          border = BorderStroke(chrome.cardBorderWidth, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Engineering, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
              }
              Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                shape = chrome.smallShape,
              ) {
                Text(
                  item.category,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
            }
            Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Spec: ${item.specDetail}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
