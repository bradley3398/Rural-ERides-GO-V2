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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.MasterSettings
import com.example.ui.components.RuralPevIcons
import com.example.ui.components.TacticalFilterChip
import com.example.ui.components.TacticalSlider
import com.example.ui.components.UpgradedIconBadge
import com.example.ui.theme.LocalTacticalChrome
import com.example.viewmodel.SessionViewModel

data class AndroidSubCategorySpec(
  val id: String,
  val title: String,
  val summary: String,
  val icon: ImageVector,
  val accentColor: Color,
)

/**
 * Renders an authentic Android Settings (Pixel / One UI style) grouped sub-category list
 * with tinted circular icons, live preference summaries, hairline dividers, and right chevrons.
 */
@Composable
fun AndroidSettingsSubCategoryMenu(
  sectionHeader: String,
  items: List<AndroidSubCategorySpec>,
  onSelect: (String) -> Unit,
  secondaryHeader: String = "",
  secondaryItems: List<AndroidSubCategorySpec> = emptyList(),
) {
  val chrome = LocalTacticalChrome.current
  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    AndroidSettingsGroupHeader(sectionHeader)
    ElevatedCard(
      shape = chrome.cardShape,
      colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
      modifier = Modifier.fillMaxWidth(),
    ) {
      items.forEachIndexed { idx, spec ->
        AndroidSettingsCategoryRow(
          icon = spec.icon,
          iconTint = spec.accentColor,
          iconBg = spec.accentColor.copy(alpha = 0.16f),
          title = spec.title,
          summary = spec.summary,
          testTagName = "settings_subrow_${spec.id.lowercase().replace(' ', '_')}",
          onClick = { onSelect(spec.id) },
        )
        if (idx < items.lastIndex) {
          HorizontalDivider(
            modifier = Modifier.padding(start = 74.dp, end = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
          )
        }
      }
    }

    if (secondaryItems.isNotEmpty()) {
      AndroidSettingsGroupHeader(secondaryHeader.ifBlank { "Additional Preferences" })
      ElevatedCard(
        shape = chrome.cardShape,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
      ) {
        secondaryItems.forEachIndexed { idx, spec ->
          AndroidSettingsCategoryRow(
            icon = spec.icon,
            iconTint = spec.accentColor,
            iconBg = spec.accentColor.copy(alpha = 0.16f),
            title = spec.title,
            summary = spec.summary,
            testTagName = "settings_subrow_${spec.id.lowercase().replace(' ', '_')}",
            onClick = { onSelect(spec.id) },
          )
          if (idx < secondaryItems.lastIndex) {
            HorizontalDivider(
              modifier = Modifier.padding(start = 74.dp, end = 16.dp),
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
          }
        }
      }
    }
  }
}

/**
 * Clean native Android Settings grouped card container for preference controls.
 */
@Composable
fun AndroidSettingsPreferenceCard(
  header: String,
  content: @Composable () -> Unit,
) {
  val chrome = LocalTacticalChrome.current
  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    AndroidSettingsGroupHeader(header)
    ElevatedCard(
      shape = chrome.cardShape,
      colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        content()
      }
    }
  }
}

@Composable
fun TelemetryChartsHudSettingsPage(
  settings: MasterSettings,
  sessionVm: SessionViewModel,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    AndroidSettingsPreferenceCard("Telemetry Graphs & Animations") {
      AndroidSettingSwitch(
        label = "Neon Glow & Pulse on Charts",
        subtitle = "Renders animated luminous aura and pulsing beacons on speed & elevation curves",
        checked = settings.chartNeonGlowEnabled,
      ) { v -> sessionVm.updateSettings { it.copy(chartNeonGlowEnabled = v) } }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      AndroidSettingSwitch(
        label = "Interactive Hover & Scrub Tooltips",
        subtitle = "Displays floating HUD telemetry readout when hovering or dragging across any chart",
        checked = settings.chartHoverTooltipsEnabled,
      ) { v -> sessionVm.updateSettings { it.copy(chartHoverTooltipsEnabled = v) } }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      Text(
        "Chart Entrance Animation Speed: ${settings.chartEntranceAnimMs} ms",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
      )
      TacticalSlider(
        value = settings.chartEntranceAnimMs.toFloat().coerceIn(250f, 1800f),
        onValueChange = { v -> sessionVm.updateSettings { it.copy(chartEntranceAnimMs = (v.toInt() / 50) * 50) } },
        valueRange = 250f..1800f,
      )
    }

    AndroidSettingsPreferenceCard("Cockpit Readout Precision") {
      AndroidSettingSwitch(
        label = "Compact High-Contrast Numeric HUD",
        subtitle = "Prioritizes oversized crisp digits for quick handlebar glances on rough backroads",
        checked = settings.compactNumericHud,
      ) { v -> sessionVm.updateSettings { it.copy(compactNumericHud = v) } }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      Text("Odometer Decimal Places: ${settings.odometerDecimals}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0 to "Whole (12)", 1 to "Tenths (12.4)", 2 to "Hundredths (12.45)").forEach { (dec, lbl) ->
          TacticalFilterChip(
            selected = settings.odometerDecimals == dec,
            onClick = { sessionVm.updateSettings { it.copy(odometerDecimals = dec) } },
            label = lbl,
            modifier = Modifier.testTag("settings_odo_dec_$dec"),
          )
        }
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      Text(
        "Auto-Pause Threshold Speed: ${"%.1f".format(settings.autoPauseSpeedKmh)} km/h",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
      )
      TacticalSlider(
        value = settings.autoPauseSpeedKmh.coerceIn(0.5f, 8.0f),
        onValueChange = { v -> sessionVm.updateSettings { it.copy(autoPauseSpeedKmh = v) } },
        valueRange = 0.5f..8.0f,
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      Text(
        "Night Vision Red-Shift Intensity: ${(settings.nightRedShiftIntensity * 100).toInt()}%",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
      )
      TacticalSlider(
        value = settings.nightRedShiftIntensity.coerceIn(0.25f, 1.0f),
        onValueChange = { v -> sessionVm.updateSettings { it.copy(nightRedShiftIntensity = v) } },
        valueRange = 0.25f..1.0f,
      )
    }
  }
}

@Composable
fun DrivetrainBarnSolarSettingsPage(
  settings: MasterSettings,
  sessionVm: SessionViewModel,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    AndroidSettingsPreferenceCard("Drivetrain, Regen & Cold Protection") {
      Text("Regenerative Braking Strength: ${settings.regenStrengthPreset}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Off", "Light", "Medium", "Strong").forEach { lvl ->
          TacticalFilterChip(
            selected = settings.regenStrengthPreset == lvl,
            onClick = { sessionVm.updateSettings { it.copy(regenStrengthPreset = lvl) } },
            label = lvl,
            modifier = Modifier.testTag("settings_regen_${lvl.lowercase()}"),
          )
        }
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      AndroidSettingSwitch(
        label = "Cold-Weather Lithium Charge Guard",
        subtitle = "Warns against charging below 32°F (0°C) and adjusts rural winter range estimates",
        checked = settings.coldWeatherBatteryGuard,
      ) { v -> sessionVm.updateSettings { it.copy(coldWeatherBatteryGuard = v) } }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      Text(
        "Chain / Belt Lube & Inspection Interval: ${settings.chainLubeIntervalKm} km",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
      )
      TacticalSlider(
        value = settings.chainLubeIntervalKm.toFloat().coerceIn(50f, 1000f),
        onValueChange = { v -> sessionVm.updateSettings { it.copy(chainLubeIntervalKm = (v.toInt() / 25) * 25) } },
        valueRange = 50f..1000f,
      )
    }

    AndroidSettingsPreferenceCard("Barn Outlet & Off-Grid Solar Charging") {
      Text("Barn / Shop Circuit Breaker Limit: ${settings.barnBreakerLimitAmps}A (120V)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(15, 20, 30, 50).forEach { amps ->
          TacticalFilterChip(
            selected = settings.barnBreakerLimitAmps == amps,
            onClick = { sessionVm.updateSettings { it.copy(barnBreakerLimitAmps = amps) } },
            label = "${amps}A",
            modifier = Modifier.testTag("settings_breaker_${amps}a"),
          )
        }
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      Text(
        "Daily Solar / Longevity Charge Target: ${settings.solarChargeTargetPct}%",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
      )
      TacticalSlider(
        value = settings.solarChargeTargetPct.toFloat().coerceIn(60f, 100f),
        onValueChange = { v -> sessionVm.updateSettings { it.copy(solarChargeTargetPct = (v.toInt() / 5) * 5) } },
        valueRange = 60f..100f,
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      AndroidSettingSwitch(
        label = "Dusk Headlight & Taillight Reminder",
        subtitle = "Alerts you before sunset so you stay visible on unlit county & gravel roads",
        checked = settings.headlightDuskReminder,
      ) { v -> sessionVm.updateSettings { it.copy(headlightDuskReminder = v) } }
      Text(
        "Remind Before Sunset: ${settings.duskAutoHeadlightReminderMin} minutes",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      TacticalSlider(
        value = settings.duskAutoHeadlightReminderMin.toFloat().coerceIn(10f, 90f),
        onValueChange = { v -> sessionVm.updateSettings { it.copy(duskAutoHeadlightReminderMin = (v.toInt() / 5) * 5) } },
        valueRange = 10f..90f,
      )
    }
  }
}

@Composable
fun RuralTrailsWildlifeSettingsPage(
  settings: MasterSettings,
  sessionVm: SessionViewModel,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    AndroidSettingsPreferenceCard("Rural Road, Wildlife & Farm Alerts") {
      AndroidSettingSwitch(
        label = "Farm Equipment & Tractor Crossing Alerts",
        subtitle = "Flags slow-moving combines, hay wagons, and blind farm lane pull-outs",
        checked = settings.farmEquipmentAlertEnabled,
      ) { v -> sessionVm.updateSettings { it.copy(farmEquipmentAlertEnabled = v) } }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      AndroidSettingSwitch(
        label = "Low-Water Creek & Washout Warnings",
        subtitle = "Highlights flood-prone rural crossings and IP-rating safety depths",
        checked = settings.creekDepthWarningEnabled,
      ) { v -> sessionVm.updateSettings { it.copy(creekDepthWarningEnabled = v) } }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      AndroidSettingSwitch(
        label = "Trail Surface Auto-Detection (IMU Bump Sensor)",
        subtitle = "Classifies pavement vs washboard gravel vs dirt trail from live accelerometer RMS",
        checked = settings.trailSurfaceAutoDetect,
      ) { v -> sessionVm.updateSettings { it.copy(trailSurfaceAutoDetect = v) } }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      AndroidSettingSwitch(
        label = "Rural Road Sign & Unpaved Transition Alerts",
        subtitle = "Announces speed drops, one-lane bridges, and gravel starts ahead",
        checked = settings.ruralRoadSignAlerts,
      ) { v -> sessionVm.updateSettings { it.copy(ruralRoadSignAlerts = v) } }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      Text(
        "Wildlife / Loose Dog Alert Radius: ${settings.wildlifeAlertRadiusMi} miles",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
      )
      TacticalSlider(
        value = settings.wildlifeAlertRadiusMi.toFloat().coerceIn(1f, 25f),
        onValueChange = { v -> sessionVm.updateSettings { it.copy(wildlifeAlertRadiusMi = v.toInt()) } },
        valueRange = 1f..25f,
      )
    }

    AndroidSettingsPreferenceCard("Convoy Telemetry & GPX Track Export") {
      Text(
        "Convoy Radar Beacon Interval: every ${settings.convoyBeaconIntervalSec} sec",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
      )
      TacticalSlider(
        value = settings.convoyBeaconIntervalSec.toFloat().coerceIn(5f, 60f),
        onValueChange = { v -> sessionVm.updateSettings { it.copy(convoyBeaconIntervalSec = (v.toInt() / 5) * 5) } },
        valueRange = 5f..60f,
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      AndroidSettingSwitch(
        label = "Include Elevation & Battery Metadata in GPX",
        subtitle = "Embeds barometric climb profiles and pack Wh data in exported trail logs",
        checked = settings.gpxIncludeElevation,
      ) { v -> sessionVm.updateSettings { it.copy(gpxIncludeElevation = v) } }
    }
  }
}

@Composable
fun AudioStudioFilterSettingsPage(
  settings: MasterSettings,
  sessionVm: SessionViewModel,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    AndroidSettingsPreferenceCard("Riding Audio Studio & Wind Protection") {
      Text("Voice Callout Verbosity: ${settings.voiceCalloutVerbosity}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Minimal", "Balanced", "Detailed").forEach { mode ->
          TacticalFilterChip(
            selected = settings.voiceCalloutVerbosity == mode,
            onClick = { sessionVm.updateSettings { it.copy(voiceCalloutVerbosity = mode) } },
            label = mode,
            modifier = Modifier.testTag("settings_callout_${mode.lowercase()}"),
          )
        }
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      AndroidSettingSwitch(
        label = "Duck Music Volume During AI / Nav Speech",
        subtitle = "Smoothly lowers riding audio while Co-Pilot or turn-by-turn speaks",
        checked = settings.audioDuckingOnSpeech,
      ) { v -> sessionVm.updateSettings { it.copy(audioDuckingOnSpeech = v) } }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      AndroidSettingSwitch(
        label = "High-Speed Wind Noise Voice Filter",
        subtitle = "Applies noise-reduction priority when recording walkie or voice queries above 20 MPH",
        checked = settings.windNoiseFilter,
      ) { v -> sessionVm.updateSettings { it.copy(windNoiseFilter = v) } }
      Spacer(Modifier.height(2.dp))
    }
  }
}

@Composable
fun WindowsDesktopIconStudioSettingsPage(
  settings: MasterSettings,
  sessionVm: SessionViewModel,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    LiveThemePreviewStudioCard(
      settings = settings,
      sessionVm = sessionVm,
    )

    AndroidSettingsPreferenceCard("Windows Desktop Chrome (95 / 2000 / XP / 7 / 10 / 11)") {
      AndroidSettingSwitch(
        label = "Show Window Title Bars & Min/Max/Close Controls",
        subtitle = "Renders era-accurate window title bars across cards in Windows themes",
        checked = settings.showClassicTitleBars,
      ) { v -> sessionVm.updateSettings { it.copy(showClassicTitleBars = v) } }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      AndroidSettingSwitch(
        label = "Classic Window Menu Bar (File • Edit • View • Tools • Help)",
        subtitle = "Displays authentic classic menu strip beneath Win95, Win2000, and WinXP title bars",
        checked = settings.winShowMenuBar,
      ) { v -> sessionVm.updateSettings { it.copy(winShowMenuBar = v) } }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      AndroidSettingSwitch(
        label = "System Tray Live Clock on Taskbar",
        subtitle = "Shows real-time clock (and date on Win10/11) inside the Windows notification tray",
        checked = settings.winShowTaskbarClock,
      ) { v -> sessionVm.updateSettings { it.copy(winShowTaskbarClock = v) } }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      AndroidSettingSwitch(
        label = "Compact Taskbar Height",
        subtitle = "Slims down the Windows taskbar to maximize vertical riding telemetry space",
        checked = settings.winTaskbarCompact,
      ) { v -> sessionVm.updateSettings { it.copy(winTaskbarCompact = v) } }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      Text(
        "Aero & Mica Glass Opacity: ${(settings.winGlassIntensity * 100).toInt()}%",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
      )
      TacticalSlider(
        value = settings.winGlassIntensity.coerceIn(0.45f, 1.0f),
        onValueChange = { v -> sessionVm.updateSettings { it.copy(winGlassIntensity = v) } },
        valueRange = 0.45f..1.0f,
        modifier = Modifier.testTag("settings_win_glass_slider"),
      )
    }

    AndroidSettingsPreferenceCard("Upgraded App Icon Pack & Neon Halo") {
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        UpgradedIconBadge(RuralPevIcons.NavTrailheadGauge, "Trailhead Icon", MaterialTheme.colorScheme.primary, containerSize = 40.dp, iconSize = 20.dp)
        UpgradedIconBadge(RuralPevIcons.NavRadarDish, "Radar Icon", Color(0xFF00C2FF), containerSize = 40.dp, iconSize = 20.dp)
        UpgradedIconBadge(RuralPevIcons.NavBarnMechanic, "Barn Mechanic Icon", Color(0xFFFF9F1C), containerSize = 40.dp, iconSize = 20.dp)
        UpgradedIconBadge(RuralPevIcons.NavCopilotCore, "Co-Pilot Icon", Color(0xFFA855F7), containerSize = 40.dp, iconSize = 20.dp)
        UpgradedIconBadge(RuralPevIcons.ActionSettingsCog, "Settings Icon", Color(0xFF39FF14), containerSize = 40.dp, iconSize = 20.dp)
      }
      Spacer(Modifier.height(4.dp))
      Text("Icon Style Pack: ${settings.iconStylePack}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(listOf("Tactical Neon", "Framed Duotone", "Classic Minimal", "Retro Pixel")) { pack ->
          TacticalFilterChip(
            selected = settings.iconStylePack == pack,
            onClick = { sessionVm.updateSettings { it.copy(iconStylePack = pack) } },
            label = pack,
            modifier = Modifier.testTag("settings_icon_pack_${pack.lowercase().replace(' ', '_')}"),
          )
        }
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      AndroidSettingSwitch(
        label = "Neon Halo Behind Active Icons",
        subtitle = "Adds a crisp luminous aura ring behind upgraded app badges & quick action buttons",
        checked = settings.iconNeonGlowEnabled,
      ) { v -> sessionVm.updateSettings { it.copy(iconNeonGlowEnabled = v) } }
    }

    AndroidSettingsPreferenceCard("Trailhead Quick Bar, Readiness & Safety HUD") {
      AndroidSettingSwitch(
        label = "Show 5-Slot Quick Command Bar on Trailhead",
        subtitle = "Displays fitted 1-tap AR HUD, Prayer, Health, Graphs & Theme shortcuts below your header",
        checked = settings.showQuickActionBarOnDash,
      ) { v -> sessionVm.updateSettings { it.copy(showQuickActionBarOnDash = v) } }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      AndroidSettingSwitch(
        label = "1-Tap Quick Theme Switcher Button on Cockpit",
        subtitle = "Lets you cycle Windows & Tactical themes right from the Trailhead command strip",
        checked = settings.quickThemeBarEnabled,
      ) { v -> sessionVm.updateSettings { it.copy(quickThemeBarEnabled = v) } }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      AndroidSettingSwitch(
        label = "Live Trail Readiness Score Meter",
        subtitle = "Calculates real-time launch score from battery %, GPS satellite lock, weather & service log",
        checked = settings.showRideReadinessScore,
      ) { v -> sessionVm.updateSettings { it.copy(showRideReadinessScore = v) } }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      AndroidSettingSwitch(
        label = "Critical Pack Voltage Audible Alarm",
        subtitle = "Sounds an urgent polyphonic warning if live pack voltage sags below your safety floor",
        checked = settings.batteryVoltageAlarmEnabled,
      ) { v -> sessionVm.updateSettings { it.copy(batteryVoltageAlarmEnabled = v) } }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      AndroidSettingSwitch(
        label = "Tactile Haptic Detents on Theme & Slider Swipes",
        subtitle = "Provides crisp micro-ticks when sliding gauges or switching Windows desktop eras",
        checked = settings.hapticScrollDetents,
      ) { v -> sessionVm.updateSettings { it.copy(hapticScrollDetents = v) } }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
      Text(
        "Auto Night HUD Dimming Level: ${settings.autoNightDimHudPct}%",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
      )
      TacticalSlider(
        value = settings.autoNightDimHudPct.toFloat().coerceIn(25f, 100f),
        onValueChange = { v -> sessionVm.updateSettings { it.copy(autoNightDimHudPct = (v.toInt() / 5) * 5) } },
        valueRange = 25f..100f,
        modifier = Modifier.testTag("settings_night_dim_slider"),
      )
    }
  }
}

