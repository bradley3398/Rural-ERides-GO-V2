package com.example.ui.compartments

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.components.RuralPevIcons

/**
 * Defined functional compartments of the Rural ERides GO PEV platform.
 * Compartmentalization provides strong decoupling between hardware HUD, navigation,
 * AI diagnostics, social networks, 3D CAD visualization, and telemetry analytics.
 */
enum class AppCompartment(val label: String, val description: String) {
  COCKPIT("Cockpit Compartment", "Primary HUD, AHRS gauges, speed telemetry, and trip recording"),
  RADAR("Radar Compartment", "GPS navigation, offline trail layers, charger telemetry, and group tracking"),
  MECHANIC("Mechanic Compartment", "AI vision inspection, e-code database, battery physics, and parts catalog"),
  COPILOT("Copilot Compartment", "Voice AI copilot, synthesized custom voice studio, and safety advisories"),
  COMMUNITY("Community Compartment", "Porch board, tactical dispatch channels, direct messages, and walkie-talkie"),
  LAB_3D("3D CAD Lab Compartment", "Isolated 3D OpenGL ES / WebGL / Compose CAD garage viewer and dyno simulation pop-up"),
  TELEMETRY_STUDIO("Telemetry Studio Compartment", "Reactive ride logs, historical analytics, and deep dive graph pop-ups"),
}

/**
 * Primary navigation tab compartments rendered inside the main scaffold.
 */
enum class PrimaryTabCompartment(
  val compartment: AppCompartment,
  val icon: ImageVector,
  val testTag: String,
) {
  COCKPIT(AppCompartment.COCKPIT, RuralPevIcons.NavTrailheadGauge, "nav_cockpit"),
  RADAR(AppCompartment.RADAR, RuralPevIcons.NavRadarDish, "nav_radar"),
  MECHANIC(AppCompartment.MECHANIC, RuralPevIcons.NavBarnMechanic, "nav_mechanic"),
  COPILOT(AppCompartment.COPILOT, RuralPevIcons.NavCopilotCore, "nav_copilot"),
  CHANNEL(AppCompartment.COMMUNITY, RuralPevIcons.NavChannelBroadcast, "nav_channel"),
  BOARD(AppCompartment.COMMUNITY, RuralPevIcons.NavPorchBoard, "nav_board"),
}

/**
 * Full-screen overlay compartments isolated from the bottom navigation scaffold.
 */
enum class OverlayCompartment {
  NONE,
  SETTINGS,
  INFO,
  TELEMETRY_STUDIO,
  CREATOR_HUB,
  VOICE_STUDIO,
}

/**
 * State holder for compartmentalized pop-ups across Rural ERides GO.
 */
@Stable
class CompartmentPopupsState {
  var activeGraphInspect by mutableStateOf<GraphInspectData?>(null)
    private set

  var is3dLabOpen by mutableStateOf(false)
    private set

  fun openGraphInspect(data: GraphInspectData) {
    activeGraphInspect = data
  }

  fun closeGraphInspect() {
    activeGraphInspect = null
  }

  fun open3dLab() {
    is3dLabOpen = true
  }

  fun close3dLab() {
    is3dLabOpen = false
  }
}

val LocalCompartmentPopups = staticCompositionLocalOf<CompartmentPopupsState?> { null }

@Composable
fun rememberCompartmentPopupsState(): CompartmentPopupsState =
  remember { CompartmentPopupsState() }
