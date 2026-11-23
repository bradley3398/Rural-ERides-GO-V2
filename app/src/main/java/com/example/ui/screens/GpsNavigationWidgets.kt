package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.TurnSlightLeft
import androidx.compose.material.icons.filled.TurnSlightRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RoundaboutLeft
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Straight
import androidx.compose.material.icons.filled.UTurnLeft
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import android.widget.Toast
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ActiveNavRoute
import com.example.data.LiveChargingStation
import com.example.data.MasterSettings
import com.example.data.NavRouteProfile
import com.example.data.WeatherGeoLocation
import com.example.ui.components.TacticalButton
import com.example.ui.components.TacticalCard
import com.example.ui.components.TacticalFilterChip
import com.example.ui.components.TacticalOutlinedButton
import com.example.ui.components.distUnitLabel
import com.example.ui.components.formatDistValue
import com.example.ui.components.tacticalOutlinedTextFieldColors
import com.example.ui.theme.LocalTacticalChrome

private fun maneuverIconFor(type: String): ImageVector =
  when (type) {
    "left", "sharp_left" -> Icons.Default.TurnLeft
    "right", "sharp_right" -> Icons.Default.TurnRight
    "slight_left" -> Icons.Default.TurnSlightLeft
    "slight_right" -> Icons.Default.TurnSlightRight
    "uturn" -> Icons.Default.UTurnLeft
    "roundabout" -> Icons.Default.RoundaboutLeft
    "arrive" -> Icons.Default.Flag
    else -> Icons.Default.Straight
  }

private fun formatStepDistance(meters: Double, distanceUnit: String): String {
  return if (distanceUnit == "mi") {
    val feet = meters * 3.28084
    if (feet < 950.0) "${feet.toInt()} ft" else "${"%.1f".format(meters / 1609.34)} mi"
  } else {
    if (meters < 950.0) "${meters.toInt()} m" else "${"%.1f".format(meters / 1000.0)} km"
  }
}

private fun formatEtaMinutes(seconds: Long): String {
  val mins = (seconds / 60L).coerceAtLeast(1L)
  return if (mins < 60L) "${mins} min" else "${mins / 60L}h ${mins % 60L}m"
}

@Composable
fun LiveTurnByTurnNavBanner(
  route: ActiveNavRoute,
  currentStepIdx: Int,
  distanceToTurnM: Double,
  remainingDistKm: Double,
  remainingSec: Long,
  offRouteAlert: Boolean,
  currentBatteryPct: Int,
  settings: MasterSettings,
  onToggleVoice: () -> Unit,
  onReroute: () -> Unit,
  onStopNav: () -> Unit,
) {
  var showAllSteps by remember { mutableStateOf(false) }
  var showCancelConfirm by remember { mutableStateOf(false) }
  val haptics = LocalHapticFeedback.current
  val ctx = LocalContext.current
  val activeStep = route.steps.getOrNull(currentStepIdx) ?: route.steps.firstOrNull()
  val nextStep = route.steps.getOrNull(currentStepIdx + 1)
  val arrivalBatteryPct = (currentBatteryPct - route.estimatedBatteryPctNeeded).coerceIn(0, 100)
  val lowRangeWarn = route.estimatedBatteryPctNeeded > currentBatteryPct
  val requestCancelNav = {
    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    if (settings.confirmCancelNavigation) {
      showCancelConfirm = true
    } else {
      onStopNav()
      Toast.makeText(ctx, "Navigation cancelled", Toast.LENGTH_SHORT).show()
    }
  }

  TacticalCard(
    highlight = true,
    title = "LIVE_GPS_NAV.SYS",
    modifier = Modifier.fillMaxWidth().testTag("live_gps_nav_banner"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Surface(
        color = if (offRouteAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        shape = LocalTacticalChrome.current.smallShape,
        modifier = Modifier.size(48.dp),
      ) {
        Icon(
          maneuverIconFor(activeStep?.maneuverType ?: "straight"),
          contentDescription = "Next maneuver",
          tint = if (offRouteAlert) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.padding(10.dp),
        )
      }
      Column(Modifier.weight(1f)) {
        Text(
          if (offRouteAlert) "OFF-ROUTE — RECALCULATING"
          else formatStepDistance(distanceToTurnM, settings.distanceUnit),
          style = MaterialTheme.typography.titleMedium,
          color = if (offRouteAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Black,
        )
        Text(
          activeStep?.instruction ?: "Follow route to ${route.destinationName}",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        if (nextStep != null) {
          Text(
            "Then: ${nextStep.instruction}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      IconButton(
        onClick = onToggleVoice,
        modifier = Modifier.testTag("nav_toggle_voice_button"),
      ) {
        Icon(
          if (settings.navVoiceGuidance) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
          contentDescription = "Toggle voice guidance",
          tint = if (settings.navVoiceGuidance) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      IconButton(
        onClick = requestCancelNav,
        modifier = Modifier.testTag("nav_stop_button"),
      ) {
        Icon(Icons.Default.Close, contentDescription = "Cancel GPS Navigation", tint = MaterialTheme.colorScheme.error)
      }
    }

    Spacer(Modifier.height(8.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text(
          "DEST: ${route.destinationName}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.secondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          "${formatEtaMinutes(remainingSec)} • ${"%.1f".format(formatDistValue(remainingDistKm, settings.distanceUnit))} ${distUnitLabel(settings.distanceUnit)} • Arrival Batt: ${if (lowRangeWarn) "LOW! (<0%)" else "$arrivalBatteryPct%"}",
          style = MaterialTheme.typography.labelSmall,
          color = if (lowRangeWarn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          "Profile: ${route.profile.label} • Engine: ${route.providerSource}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(onClick = onReroute, modifier = Modifier.testTag("nav_reroute_button")) {
          Icon(Icons.Default.Refresh, contentDescription = "Reroute", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = { showAllSteps = !showAllSteps }, modifier = Modifier.testTag("nav_steps_toggle_button")) {
          Icon(
            if (showAllSteps) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = "Toggle turn list",
            tint = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }

    Spacer(Modifier.height(10.dp))
    TacticalButton(
      onClick = requestCancelNav,
      isDestructive = true,
      modifier = Modifier.fillMaxWidth().height(48.dp).testTag("nav_cancel_full_button"),
    ) {
      Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(8.dp))
      Text("Cancel Navigation", fontWeight = FontWeight.Bold)
    }

    AnimatedVisibility(visible = showAllSteps) {
      Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HorizontalDivider()
        Text("TURN-BY-TURN MANEUVERS (${route.steps.size})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        route.steps.take(16).forEachIndexed { idx, step ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              maneuverIconFor(step.maneuverType),
              contentDescription = null,
              tint = if (idx == currentStepIdx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
            Text(
              step.instruction,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = if (idx == currentStepIdx) FontWeight.Bold else FontWeight.Normal,
              modifier = Modifier.weight(1f),
            )
            Text(
              formatStepDistance(step.distanceM, settings.distanceUnit),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.secondary,
            )
          }
        }
      }
    }
  }

  if (showCancelConfirm) {
    com.example.ui.components.TacticalDialog(
      onDismissRequest = { showCancelConfirm = false },
      title = "Cancel GPS Navigation?",
      subtitle = route.destinationName,
      icon = Icons.Default.Close,
      badgeText = "ROUTE",
      confirmButton = {
        com.example.ui.components.TacticalButton(
          onClick = {
            showCancelConfirm = false
            onStopNav()
            Toast.makeText(ctx, "Navigation cancelled", Toast.LENGTH_SHORT).show()
          },
          isDestructive = true,
          modifier = Modifier.testTag("nav_cancel_confirm_yes"),
        ) {
          Text("Cancel Navigation")
        }
      },
      dismissButton = {
        com.example.ui.components.TacticalOutlinedButton(onClick = { showCancelConfirm = false }) { Text("Keep Navigating") }
      },
    ) {
      Text(
        "Stop live turn-by-turn guidance to ${route.destinationName}?",
        style = MaterialTheme.typography.bodyMedium,
      )
    }
  }
}

@Composable
fun GpsNavigationPlannerCard(
  hasGpsFix: Boolean,
  myLat: Double,
  myLng: Double,
  launchLat: Double?,
  launchLng: Double?,
  navSearching: Boolean,
  navRouting: Boolean,
  searchResults: List<WeatherGeoLocation>,
  liveChargers: List<LiveChargingStation>,
  chargersLoading: Boolean,
  navStatusMsg: String,
  settings: MasterSettings,
  onSearchDestinations: (String) -> Unit,
  onSelectProfile: (NavRouteProfile) -> Unit,
  onStartRoute: (String, Double, Double, NavRouteProfile) -> Unit,
  onScanLiveChargers: () -> Unit,
  hasActiveRoute: Boolean = false,
  onCancelNav: () -> Unit = {},
) {
  var destQuery by remember { mutableStateOf("") }
  var selectedProfile by remember(settings.navDefaultProfile) { mutableStateOf(settings.navDefaultProfile) }
  val haptics = LocalHapticFeedback.current
  val ctx = LocalContext.current

  TacticalCard(highlight = true, title = "GPS_NAV_PLANNER.EXE") {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Navigation, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text("TURN-BY-TURN PEV GPS NAVIGATION", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
      }
      if (navRouting || navSearching) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
      }
    }
    Spacer(Modifier.height(6.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      OutlinedTextField(
        value = destQuery,
        onValueChange = { destQuery = it },
        placeholder = { Text("Enter address, trailhead, park, or city...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        singleLine = true,
        shape = LocalTacticalChrome.current.buttonShape,
        colors = tacticalOutlinedTextFieldColors(),
        modifier = Modifier.weight(1f).testTag("gps_nav_destination_input"),
      )
      TacticalButton(
        onClick = { onSearchDestinations(destQuery) },
        enabled = destQuery.trim().length >= 2 && !navSearching,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        modifier = Modifier.testTag("gps_nav_search_button"),
      ) {
        Icon(Icons.Default.Directions, contentDescription = "Find route", modifier = Modifier.size(18.dp))
      }
    }

    Spacer(Modifier.height(6.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(NavRouteProfile.entries) { prof ->
        TacticalFilterChip(
          selected = selectedProfile == prof,
          onClick = {
            selectedProfile = prof
            onSelectProfile(prof)
          },
          label = prof.label,
          modifier = Modifier.testTag("nav_profile_${prof.id}"),
        )
      }
    }

    Spacer(Modifier.height(6.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      TacticalOutlinedButton(
        onClick = onScanLiveChargers,
        enabled = hasGpsFix && !chargersLoading,
        modifier = Modifier.weight(1f).testTag("scan_live_chargers_button"),
      ) {
        Icon(Icons.Default.EvStation, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(if (chargersLoading) "Scanning..." else "Find Live Chargers", maxLines = 1, softWrap = false)
      }
      if (launchLat != null && launchLng != null && hasGpsFix) {
        TacticalOutlinedButton(
          onClick = { onStartRoute("Launch Point (Home Base)", launchLat, launchLng, selectedProfile) },
          enabled = !navRouting,
          modifier = Modifier.weight(1f).testTag("nav_return_to_launch_button"),
        ) {
          Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Route to Launch", maxLines = 1, softWrap = false)
        }
      }
    }

    if (hasActiveRoute || navRouting) {
      Spacer(Modifier.height(8.dp))
      TacticalButton(
        onClick = {
          haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          onCancelNav()
          Toast.makeText(ctx, "Navigation cancelled", Toast.LENGTH_SHORT).show()
        },
        isDestructive = true,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("nav_planner_cancel_button"),
      ) {
        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
          if (navRouting && !hasActiveRoute) "Cancel Route Calculation" else "Cancel Active Navigation",
          fontWeight = FontWeight.Bold,
        )
      }
    }

    if (navStatusMsg.isNotBlank()) {
      Spacer(Modifier.height(6.dp))
      Text(navStatusMsg, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
    }

    if (searchResults.isNotEmpty()) {
      Spacer(Modifier.height(8.dp))
      Text("SELECT DESTINATION TO START LIVE GUIDANCE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
      Spacer(Modifier.height(4.dp))
      searchResults.forEach { loc ->
        Surface(
          onClick = { onStartRoute(loc.displayLabel(), loc.lat, loc.lng, selectedProfile) },
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
          shape = LocalTacticalChrome.current.smallShape,
          modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).testTag("nav_result_row"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(Modifier.weight(1f)) {
              Text(loc.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              Text(loc.displayLabel(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.Navigation, contentDescription = "Start Navigation", tint = MaterialTheme.colorScheme.primary)
          }
        }
      }
    }

    if (liveChargers.isNotEmpty()) {
      Spacer(Modifier.height(8.dp))
      Text("LIVE NEARBY CHARGING STATIONS (${liveChargers.size})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
      Spacer(Modifier.height(4.dp))
      liveChargers.take(5).forEach { st ->
        Surface(
          onClick = { onStartRoute(st.title, st.lat, st.lng, selectedProfile) },
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
          shape = LocalTacticalChrome.current.smallShape,
          modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(Modifier.weight(1f)) {
              Text(st.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
              Text(
                "${"%.1f".format(formatDistValue(st.distanceKm, settings.distanceUnit))} ${distUnitLabel(settings.distanceUnit)} • ${st.powerKw}${if (st.isFree) " • FREE" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Icon(Icons.Default.EvStation, contentDescription = "Navigate to charger", tint = MaterialTheme.colorScheme.tertiary)
          }
        }
      }
    }
  }
}
