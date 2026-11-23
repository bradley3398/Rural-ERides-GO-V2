package com.example.ui.screens

import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.ai.GeminiModelCatalog
import com.example.core.AppLocale
import com.example.core.ReminderScheduler
import com.example.data.AiVoiceProfile
import com.example.data.AlertTonePreset
import com.example.data.AppThemePreset
import com.example.data.BgTexture
import com.example.data.ColorModeOverride
import com.example.data.FontPreset
import com.example.data.HudStyle
import com.example.data.MasterSettings
import com.example.data.NavRouteProfile
import com.example.data.PevLegalClass
import com.example.data.PevWallpaper
import com.example.data.RingtonePreset
import com.example.data.VibrationPatternPreset
import com.example.data.VisualStyleMode
import com.example.hardware.VoiceAudioManager
import com.example.ui.components.RuralPevIcons
import com.example.ui.components.TacticalButton
import com.example.ui.components.TacticalCard
import com.example.ui.components.TacticalDialog
import com.example.ui.components.TacticalFilterChip
import com.example.ui.components.TacticalOutlinedButton
import com.example.ui.components.TacticalSlider
import com.example.ui.components.tacticalOutlinedTextFieldColors
import com.example.ui.theme.LocalTacticalChrome
import com.example.viewmodel.SessionViewModel

@Composable
fun AndroidSettingsCategoryRow(
  icon: ImageVector,
  iconTint: Color,
  iconBg: Color,
  title: String,
  summary: String,
  testTagName: String,
  onClick: () -> Unit,
) {
  val haptics = LocalHapticFeedback.current
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clickable {
          haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onClick()
        }
        .padding(horizontal = 16.dp, vertical = 16.dp)
        .testTag(testTagName),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Box(
      modifier = Modifier.size(44.dp).clip(CircleShape).background(iconBg),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = iconTint,
        modifier = Modifier.size(22.dp),
      )
    }
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = summary,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Icon(
      imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
      contentDescription = "Open $title",
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(22.dp),
    )
  }
}

@Composable
fun AndroidSettingsGroupHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.labelLarge,
    fontWeight = FontWeight.Bold,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 6.dp),
  )
}

@Composable
fun SettingsSubCategoryBar(
  subCategories: List<String>,
  selected: String,
  onSelect: (String) -> Unit,
) {
  val haptics = LocalHapticFeedback.current
  Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
    Text(
      "Sub-Categories",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(subCategories) { sub ->
        TacticalFilterChip(
          selected = selected == sub,
          onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onSelect(sub)
          },
          label = sub,
          modifier = Modifier.testTag("settings_subcat_${sub.lowercase().replace(' ', '_')}"),
        )
      }
    }
  }
}

@Composable
fun AndroidSettingSwitch(
  label: String,
  subtitle: String = "",
  checked: Boolean,
  onChecked: (Boolean) -> Unit,
) {
  val haptics = LocalHapticFeedback.current
  Row(
    Modifier.fillMaxWidth().padding(vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
      if (subtitle.isNotBlank()) {
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    Switch(
      checked = checked,
      onCheckedChange = { v ->
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onChecked(v)
      },
    )
  }
}

@Composable
fun DisplayThemesSettingsContent(
  settings: MasterSettings,
  sessionVm: SessionViewModel,
  selectedSubCategory: String? = null,
  onSelectSubCategory: (String) -> Unit = {},
  onReplayStartupIntro: () -> Unit = {},
) {
  if (selectedSubCategory == null) {
    AndroidSettingsSubCategoryMenu(
      sectionHeader = "Display & Visual Style",
      items =
        listOf(
          AndroidSubCategorySpec(
            id = "Themes & Wallpapers",
            title = "Live Theme Preview & Wallpapers",
            summary = "${settings.visualStyle.label} • ${settings.pevWallpaper.label} • ${settings.colorModeOverride.label}",
            icon = RuralPevIcons.ActionThemeStudio,
            accentColor = Color(0xFF00C2FF),
          ),
          AndroidSubCategorySpec(
            id = "Windows & Icons",
            title = "Windows Desktop & Icon Studio",
            summary = "Win 95–11 Chrome • ${settings.iconStylePack} Icons • Glass ${(settings.winGlassIntensity * 100).toInt()}%",
            icon = Icons.Default.DesktopWindows,
            accentColor = Color(0xFF3B82F6),
          ),
          AndroidSubCategorySpec(
            id = "Layout & Gauges",
            title = "Layout, Menus & Speedometer",
            summary = "${settings.hudStyle.label} • ${settings.fontPreset.label} • ${if (settings.spaciousLayoutMode) "Spacious" else "Compact"}",
            icon = Icons.Default.Dashboard,
            accentColor = Color(0xFF39FF14),
          ),
          AndroidSubCategorySpec(
            id = "Startup & Screen",
            title = "Startup Intro & Screen Comfort",
            summary = "${if (settings.showStartupAnimation) "Intro ${settings.splashDurationSec}s" else "Intro Off"} • ${settings.maxFps}Hz • ${if (settings.keepScreenAwakeRiding) "Keep Awake" else "Standard"}",
            icon = Icons.Default.Smartphone,
            accentColor = Color(0xFFFF9F1C),
          ),
        ),
      secondaryHeader = "Charts & Night Vision",
      secondaryItems =
        listOf(
          AndroidSubCategorySpec(
            id = "Telemetry Charts & HUD",
            title = "Telemetry Charts & HUD Precision",
            summary = "${if (settings.chartNeonGlowEnabled) "Neon Glow On" else "Flat"} • Anim ${settings.chartEntranceAnimMs}ms • ${settings.odometerDecimals} dec",
            icon = Icons.Default.ShowChart,
            accentColor = Color(0xFFA855F7),
          ),
        ),
      onSelect = onSelectSubCategory,
    )
    return
  }

  val subCat = selectedSubCategory
  if (subCat == "Windows & Icons") {
    WindowsDesktopIconStudioSettingsPage(settings = settings, sessionVm = sessionVm)
    return
  }
  if (subCat == "Telemetry Charts & HUD") {
    TelemetryChartsHudSettingsPage(settings = settings, sessionVm = sessionVm)
    return
  }

  if (subCat == "All" || subCat == "Themes & Wallpapers") {
    var themeFilter by remember { mutableStateOf("All") }
    LiveThemePreviewStudioCard(
      settings = settings,
      sessionVm = sessionVm,
      onOpenWindowsStudio = { onSelectSubCategory("Windows & Icons") },
    )
    Spacer(Modifier.height(12.dp))
    val filteredStyles =
      remember(themeFilter) {
        when (themeFilter) {
          "Windows OS" -> VisualStyleMode.entries.filter { it.name.startsWith("WINDOWS_") }
          "Rural Country" -> VisualStyleMode.entries.filter { it.name.startsWith("RURAL_") || it == VisualStyleMode.PEV_GRAVEL_DUST }
          "PEV Racing" -> VisualStyleMode.entries.filter { it.name.startsWith("PEV_") }
          "Modern & Dark" -> VisualStyleMode.entries.filter { !it.name.startsWith("WINDOWS_") && !it.name.startsWith("RURAL_") && !it.name.startsWith("PEV_") }
          else -> VisualStyleMode.entries
        }
      }
    TacticalCard(highlight = true, title = "Themes & Wallpapers") {
      Text("App Look & Style (${filteredStyles.size} of ${VisualStyleMode.entries.size} Themes)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
      Spacer(Modifier.height(6.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(listOf("All", "Windows OS", "Rural Country", "PEV Racing", "Modern & Dark")) { catLabel ->
          com.example.ui.components.TacticalFilterChip(
            selected = themeFilter == catLabel,
            onClick = { themeFilter = catLabel },
            label = catLabel,
            modifier = Modifier.testTag("theme_filter_${catLabel.lowercase().replace(" ", "_")}"),
          )
        }
      }
      Spacer(Modifier.height(6.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(filteredStyles) { mode ->
          com.example.ui.components.TacticalFilterChip(
            selected = settings.visualStyle == mode,
            onClick = {
              val isWin = mode.name.startsWith("WINDOWS_")
              sessionVm.updateSettings {
                it.copy(
                  visualStyle = mode,
                  navBarStyle = if (isWin) "classic_taskbar" else it.navBarStyle,
                  showClassicTitleBars = if (isWin) true else it.showClassicTitleBars,
                )
              }
            },
            label = mode.label,
            modifier = Modifier.testTag("style_mode_${mode.id}"),
          )
        }
      }
      Spacer(Modifier.height(10.dp))
      Text("Background Wallpaper (${PevWallpaper.entries.size} Scenes)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(PevWallpaper.entries) { wp ->
          com.example.ui.components.TacticalFilterChip(
            selected = settings.pevWallpaper == wp,
            onClick = { sessionVm.updateSettings { it.copy(pevWallpaper = wp) } },
            label = wp.label,
            modifier = Modifier.testTag("pev_wallpaper_${wp.id}"),
          )
        }
      }
      Text("Wallpaper Brightness (${(settings.wallpaperOpacity * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall)
      TacticalSlider(
        value = settings.wallpaperOpacity,
        onValueChange = { v -> sessionVm.updateSettings { it.copy(wallpaperOpacity = v) } },
        valueRange = 0.0f..0.75f,
      )
      HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
      Text("Light / Dark Mode & Accent Color", style = MaterialTheme.typography.labelSmall)
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ColorModeOverride.entries.forEach { ov ->
          com.example.ui.components.TacticalFilterChip(
            selected = settings.colorModeOverride == ov,
            onClick = { sessionVm.updateSettings { it.copy(colorModeOverride = ov) } },
            label = ov.label,
          )
        }
      }
      Spacer(Modifier.height(4.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(AppThemePreset.entries) { preset ->
          com.example.ui.components.TacticalFilterChip(
            selected = settings.themePreset == preset,
            onClick = { sessionVm.updateSettings { it.copy(themePreset = preset) } },
            label = preset.label,
          )
        }
      }
      Spacer(Modifier.height(6.dp))
      Text("Background Pattern (${BgTexture.entries.size} Patterns)", style = MaterialTheme.typography.labelSmall)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(BgTexture.entries) { tex ->
          com.example.ui.components.TacticalFilterChip(
            selected = settings.bgTexture == tex,
            onClick = { sessionVm.updateSettings { it.copy(bgTexture = tex) } },
            label = tex.label,
          )
        }
      }
    }
    Spacer(Modifier.height(12.dp))
  }

  if (subCat == "All" || subCat == "Layout & Gauges") {
    TacticalCard(title = "Layout, Menus & Speedometer") {
      Text("LAYOUT, MENUS & SPEEDOMETER", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
      Spacer(Modifier.height(6.dp))
      AndroidSettingSwitch(
        label = "Spacious Friendly Card Layout",
        subtitle = "Adds extra breathing room and padding across every screen",
        checked = settings.spaciousLayoutMode,
      ) { v -> sessionVm.updateSettings { it.copy(spaciousLayoutMode = v) } }
      Spacer(Modifier.height(4.dp))
      Text("Navigation Bar Style (${com.example.ui.components.ALL_NAV_BAR_STYLES.size} Layouts)", style = MaterialTheme.typography.labelSmall)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(com.example.ui.components.ALL_NAV_BAR_STYLES) { opt ->
          com.example.ui.components.TacticalFilterChip(
            selected = settings.navBarStyle == opt.id,
            onClick = { sessionVm.updateSettings { it.copy(navBarStyle = opt.id) } },
            label = "${opt.label} [${opt.badge}]",
            modifier = Modifier.testTag("nav_bar_style_${opt.id}"),
          )
        }
      }
      Spacer(Modifier.height(6.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("full" to "Full Detail", "compact" to "Balanced", "minimal" to "Simple").forEach { (densId, densLabel) ->
          com.example.ui.components.TacticalFilterChip(
            selected = settings.dashboardDensity == densId,
            onClick = { sessionVm.updateSettings { it.copy(dashboardDensity = densId) } },
            label = densLabel,
            modifier = Modifier.testTag("dashboard_density_$densId"),
          )
        }
      }
      Spacer(Modifier.height(6.dp))
      Text("Speedometer Gauge Look (${HudStyle.entries.size} HUDs)", style = MaterialTheme.typography.labelSmall)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(HudStyle.entries) { style ->
          com.example.ui.components.TacticalFilterChip(
            selected = settings.hudStyle == style,
            onClick = { sessionVm.updateSettings { it.copy(hudStyle = style) } },
            label = style.label,
          )
        }
      }
      Spacer(Modifier.height(4.dp))
      Text("Font Style (${FontPreset.entries.size} Fonts)", style = MaterialTheme.typography.labelSmall)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(FontPreset.entries) { f ->
          TacticalFilterChip(
            selected = settings.fontPreset == f,
            onClick = { sessionVm.updateSettings { it.copy(fontPreset = f) } },
            label = f.label,
          )
        }
      }
      Text("Language", style = MaterialTheme.typography.labelSmall)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(AppLocale.entries) { loc ->
          TacticalFilterChip(
            selected = settings.locale == loc.code,
            onClick = { sessionVm.updateSettings { it.copy(locale = loc.code) } },
            label = loc.label,
          )
        }
      }
      Text("Card Transparency (${(settings.cardOpacity * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall)
      TacticalSlider(value = settings.cardOpacity, onValueChange = { v -> sessionVm.updateSettings { it.copy(cardOpacity = v) } }, valueRange = 0.5f..1.0f)
      Text("Edge Glow (${(settings.borderGlowIntensity * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall)
      TacticalSlider(value = settings.borderGlowIntensity, onValueChange = { v -> sessionVm.updateSettings { it.copy(borderGlowIntensity = v) } }, valueRange = 0f..1f)
      Text("Speedometer Dial Width: ${settings.dialSweepDeg}°", style = MaterialTheme.typography.labelSmall)
      TacticalSlider(value = settings.dialSweepDeg.toFloat(), onValueChange = { v -> sessionVm.updateSettings { it.copy(dialSweepDeg = v.toInt()) } }, valueRange = 180f..300f)
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("rounded", "sharp", "pill").forEach { cs ->
          TacticalFilterChip(selected = settings.cornerStyle == cs, onClick = { sessionVm.updateSettings { it.copy(cornerStyle = cs) } }, label = cs.replaceFirstChar { it.uppercase() })
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("compact" to "Small Text", "normal" to "Normal Text", "large" to "Large Text").forEach { (sc, scLabel) ->
          TacticalFilterChip(selected = settings.uiScale == sc, onClick = { sessionVm.updateSettings { it.copy(uiScale = sc) } }, label = scLabel)
        }
      }
    }
    Spacer(Modifier.height(12.dp))
  }

  if (subCat == "All" || subCat == "Startup & Screen") {
    TacticalCard(title = "Startup Intro & Screen Comfort") {
      Text("STARTUP INTRO, LIGHT SENSOR & SCREEN", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
      Spacer(Modifier.height(6.dp))
      AndroidSettingSwitch(
        label = "Animated Logo Startup Sequence",
        subtitle = "Plays the animated Rural ERides GO artwork & logo intro when launching the app",
        checked = settings.showStartupAnimation,
      ) { v -> sessionVm.updateSettings { it.copy(showStartupAnimation = v) } }
      Spacer(Modifier.height(4.dp))
      Text("Startup Splash Display Length (${settings.splashDurationSec} seconds)", style = MaterialTheme.typography.labelSmall)
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(4, 7, 9, 12).forEach { sec ->
          TacticalFilterChip(
            selected = settings.splashDurationSec == sec,
            onClick = { sessionVm.updateSettings { it.copy(splashDurationSec = sec) } },
            label = "${sec}s",
            modifier = Modifier.testTag("settings_splash_duration_$sec"),
          )
        }
      }
      Spacer(Modifier.height(6.dp))
      TacticalOutlinedButton(
        onClick = onReplayStartupIntro,
        modifier = Modifier.fillMaxWidth().testTag("settings_replay_splash_button"),
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Replay Startup Artwork Intro")
      }
      Spacer(Modifier.height(6.dp))
      AndroidSettingSwitch(
        label = "Keep Screen Awake While Riding / Navigating",
        subtitle = "Prevents screen timeout during active ride recording or live turn-by-turn GPS",
        checked = settings.keepScreenAwakeRiding,
      ) { v -> sessionVm.updateSettings { it.copy(keepScreenAwakeRiding = v) } }
      AndroidSettingSwitch(
        label = "Auto Night Vision from Light Sensor",
        subtitle = "Automatically switches to dark-safe red mode when riding in low light",
        checked = settings.autoNightFromLightSensor,
      ) { v -> sessionVm.updateSettings { it.copy(autoNightFromLightSensor = v) } }
      AndroidSettingSwitch(
        label = "Keep Screen Bright During Rides",
        subtitle = "Prevents screen dimming while recording your ride",
        checked = settings.keepScreenBrightOnRide,
      ) { v -> sessionVm.updateSettings { it.copy(keepScreenBrightOnRide = v) } }
      AndroidSettingSwitch("Night Vision Red Mode", checked = settings.nightVisionMode) { v ->
        sessionVm.updateSettings { it.copy(nightVisionMode = v) }
      }
      AndroidSettingSwitch("Bright Sunlight High-Contrast", checked = settings.sunVisibilityMode) { v ->
        sessionVm.updateSettings { it.copy(sunVisibilityMode = v) }
      }
      AndroidSettingSwitch("3D Speedometer Tilt Effect", checked = settings.hud3dTilt) { v ->
        sessionVm.updateSettings { it.copy(hud3dTilt = v) }
      }
      AndroidSettingSwitch("High-Contrast Icons", checked = settings.highContrastIcons) { v ->
        sessionVm.updateSettings { it.copy(highContrastIcons = v) }
      }
      AndroidSettingSwitch("Classic Window Title Bars", checked = settings.showClassicTitleBars) { v ->
        sessionVm.updateSettings { it.copy(showClassicTitleBars = v) }
      }
      AndroidSettingSwitch("Reduce Screen Animations", checked = settings.reducedMotion) { v ->
        sessionVm.updateSettings { it.copy(reducedMotion = v) }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(60, 90, 120).forEach { fps ->
          TacticalFilterChip(selected = settings.maxFps == fps, onClick = { sessionVm.updateSettings { it.copy(maxFps = fps) } }, label = "${fps}Hz")
        }
      }
      Spacer(Modifier.height(6.dp))
      Text(
        "Android Material You App Icon: Matched to your original Rural ERides GO logo (zero watermark) so Android home screen Color Match tints your real emblem.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
      )
    }
  }
}

@Composable
fun AhrsCockpitBatterySettingsContent(
  settings: MasterSettings,
  sessionVm: SessionViewModel,
  selectedSubCategory: String? = null,
  onSelectSubCategory: (String) -> Unit = {},
) {
  if (selectedSubCategory == null) {
    AndroidSettingsSubCategoryMenu(
      sectionHeader = "Telemetry & Hardware Sensors",
      items =
        listOf(
          AndroidSubCategorySpec(
            id = "GPS & Live Sensors",
            title = "GPS Satellites & Live Sensors",
            summary = "${if (settings.showSatelliteStatusOnDash) "GNSS Sats On" else "Sats Hidden"} • ${if (settings.showEnvironmentSensorsOnDash) "Barometer & Lux" else "Basic"} • ${if (settings.showRideChartsOnDash) "Charts On" else "Off"}",
            icon = Icons.Default.SatelliteAlt,
            accentColor = Color(0xFF00C2FF),
          ),
          AndroidSubCategorySpec(
            id = "Tilt & Motion",
            title = "Tilt, Lean & Motion Gauges",
            summary = "${if (settings.showAhrsTelemetryCard) "9-DOF AHRS On" else "Hidden"} • Lean ±${settings.ahrsLeanAlertDeg}° • ${settings.imuPollingRateHz}Hz",
            icon = Icons.Default.Explore,
            accentColor = Color(0xFF39FF14),
          ),
        ),
      secondaryHeader = "Power, Drivetrain & Solar Barn",
      secondaryItems =
        listOf(
          AndroidSubCategorySpec(
            id = "Battery & Units",
            title = "Units, Battery & Ride Goals",
            summary = "${if (settings.distanceUnit == "mi") "MPH / °${settings.tempUnit.uppercase()}" else "KM/H / °${settings.tempUnit.uppercase()}"} • ${settings.batteryChemistry} • Low ${settings.lowBatteryWarningPct}%",
            icon = Icons.Default.BatteryChargingFull,
            accentColor = Color(0xFFFF9F1C),
          ),
          AndroidSubCategorySpec(
            id = "Drivetrain & Barn Charging",
            title = "Drivetrain, Solar & Barn Charging",
            summary = "Regen ${settings.regenStrengthPreset} • ${settings.barnBreakerLimitAmps}A Breaker • Solar Target ${settings.solarChargeTargetPct}%",
            icon = Icons.Default.WbSunny,
            accentColor = Color(0xFF14B8A6),
          ),
        ),
      onSelect = onSelectSubCategory,
    )
    return
  }

  val subCat = selectedSubCategory
  if (subCat == "Drivetrain & Barn Charging") {
    DrivetrainBarnSolarSettingsPage(settings = settings, sessionVm = sessionVm)
    return
  }

  if (subCat == "All" || subCat == "GPS & Live Sensors") {
    TacticalCard(highlight = true, title = "GPS Satellites & Live Sensors") {
      Text("GPS SATELLITES, AR & ENVIRONMENT SENSORS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
      Spacer(Modifier.height(6.dp))
      AndroidSettingSwitch(
        label = "Show Connected GPS Satellites on Dashboard",
        subtitle = "Displays how many GPS/GLONASS/Galileo satellites are locked & signal strength",
        checked = settings.showSatelliteStatusOnDash,
      ) { v -> sessionVm.updateSettings { it.copy(showSatelliteStatusOnDash = v) } }
      AndroidSettingSwitch(
        label = "Show Live Environment & Road Sensors",
        subtitle = "Shows live barometer air pressure, ambient light (lux), compass flux & road bumps",
        checked = settings.showEnvironmentSensorsOnDash,
      ) { v -> sessionVm.updateSettings { it.copy(showEnvironmentSensorsOnDash = v) } }
      AndroidSettingSwitch(
        label = "Show Satellites & Sensors in AR Camera",
        subtitle = "Overlays live satellite lock count, barometer & road bump meter on AR views",
        checked = settings.arShowSatelliteOverlay,
      ) { v -> sessionVm.updateSettings { it.copy(arShowSatelliteOverlay = v) } }
      AndroidSettingSwitch(
        label = "Show Ride Charts & Track Maps",
        subtitle = "Displays speed charts, pie charts & GPS route maps in Ride History",
        checked = settings.showRideChartsOnDash,
      ) { v -> sessionVm.updateSettings { it.copy(showRideChartsOnDash = v) } }
      AndroidSettingSwitch(
        label = "Auto-Fetch Ride Photo & Brand Logo",
        subtitle = "Automatically downloads your vehicle's photo & official logo when adding a ride",
        checked = settings.autoFetchPevPhoto,
      ) { v -> sessionVm.updateSettings { it.copy(autoFetchPevPhoto = v) } }
    }
    Spacer(Modifier.height(12.dp))
  }

  if (subCat == "All" || subCat == "Tilt & Motion") {
    TacticalCard(title = "Tilt, Lean & Motion Gauges") {
      Text("TILT, LEAN & MOTION GAUGES", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
      AndroidSettingSwitch(
        label = "Show Live Tilt & Lean Gauges on Dashboard",
        subtitle = "Animated Pitch, Roll & 360° Compass gauges with Artificial Horizon",
        checked = settings.showAhrsTelemetryCard,
      ) { v -> sessionVm.updateSettings { it.copy(showAhrsTelemetryCard = v) } }
      AndroidSettingSwitch(
        label = "Remember Max Left/Right Lean Angles",
        subtitle = "Holds your deepest corner lean angles until you tap Reset",
        checked = settings.ahrsPeakLeanHold,
      ) { v -> sessionVm.updateSettings { it.copy(ahrsPeakLeanHold = v) } }
      AndroidSettingSwitch(
        label = "True North Compass Correction",
        subtitle = "Uses GPS declination to align compass headings with True North",
        checked = settings.compassTrueNorth,
      ) { v -> sessionVm.updateSettings { it.copy(compassTrueNorth = v) } }
      Text("Lean Angle Warning: ±${settings.ahrsLeanAlertDeg}°", style = MaterialTheme.typography.labelSmall)
      TacticalSlider(
        value = settings.ahrsLeanAlertDeg.toFloat().coerceIn(20f, 55f),
        onValueChange = { v -> sessionVm.updateSettings { it.copy(ahrsLeanAlertDeg = v.toInt()) } },
        valueRange = 20f..55f,
      )
      Text("Gauge Needle Smoothness: ${settings.ahrsGaugeDamping}", style = MaterialTheme.typography.labelSmall)
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("Snappy", "Balanced", "Smooth").forEach { mode ->
          TacticalFilterChip(
            selected = settings.ahrsGaugeDamping == mode,
            onClick = { sessionVm.updateSettings { it.copy(ahrsGaugeDamping = mode) } },
            label = mode,
          )
        }
      }
      Text("Motion Sensor Speed: ${settings.imuPollingRateHz} Hz", style = MaterialTheme.typography.labelSmall)
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(10, 20, 50).forEach { hz ->
          TacticalFilterChip(
            selected = settings.imuPollingRateHz == hz,
            onClick = { sessionVm.updateSettings { it.copy(imuPollingRateHz = hz) } },
            label = "${hz}Hz",
          )
        }
      }
    }
    Spacer(Modifier.height(12.dp))
  }

  if (subCat == "All" || subCat == "Battery & Units") {
    TacticalCard(title = "Units, Battery & Ride Goals") {
      Text("UNITS, BATTERY & RIDE PREFERENCES", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
      Spacer(Modifier.height(6.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TacticalFilterChip(selected = settings.distanceUnit == "mi", onClick = { sessionVm.updateSettings { it.copy(distanceUnit = "mi") } }, label = "Miles / MPH")
        TacticalFilterChip(selected = settings.distanceUnit == "km", onClick = { sessionVm.updateSettings { it.copy(distanceUnit = "km") } }, label = "Km / KM/H")
        TacticalFilterChip(selected = settings.tempUnit == "f", onClick = { sessionVm.updateSettings { it.copy(tempUnit = "f") } }, label = "°F")
        TacticalFilterChip(selected = settings.tempUnit == "c", onClick = { sessionVm.updateSettings { it.copy(tempUnit = "c") } }, label = "°C")
      }
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("NMC", "LFP").forEach { chem ->
          TacticalFilterChip(selected = settings.batteryChemistry == chem, onClick = { sessionVm.updateSettings { it.copy(batteryChemistry = chem) } }, label = "Battery: $chem")
        }
        listOf("Linear", "Adaptive", "Aggressive").forEach { reg ->
          TacticalFilterChip(selected = settings.regenAlgorithm == reg, onClick = { sessionVm.updateSettings { it.copy(regenAlgorithm = reg) } }, label = "Regen: $reg")
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("Percentage", "Voltage", "Dual").forEach { mode ->
          TacticalFilterChip(selected = settings.voltDisplayMode == mode, onClick = { sessionVm.updateSettings { it.copy(voltDisplayMode = mode) } }, label = mode)
        }
      }
      AndroidSettingSwitch("Smart AI Range Estimation", checked = settings.smartBatteryAi) { v -> sessionVm.updateSettings { it.copy(smartBatteryAi = v) } }
      AndroidSettingSwitch("Auto-Start Recording on App Open", checked = settings.autoStartTracking) { v -> sessionVm.updateSettings { it.copy(autoStartTracking = v) } }
      Text("Motor Heat Warning: ${settings.motorTempWarnC}°C", style = MaterialTheme.typography.labelSmall)
      TacticalSlider(value = settings.motorTempWarnC.toFloat(), onValueChange = { v -> sessionVm.updateSettings { it.copy(motorTempWarnC = v.toInt()) } }, valueRange = 65f..115f)
      Text("Gas Savings Calculator: $${"%.2f".format(settings.gasPriceUsd)}/gal • Car ${settings.carMpg.toInt()} MPG", style = MaterialTheme.typography.labelSmall)
      TacticalSlider(value = settings.gasPriceUsd.toFloat(), onValueChange = { v -> sessionVm.updateSettings { it.copy(gasPriceUsd = v.toDouble()) } }, valueRange = 2.0f..7.0f)
      Spacer(Modifier.height(6.dp))
      Text("Default Riding Style", style = MaterialTheme.typography.labelSmall)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(listOf("ECO", "STREET", "SPORT", "SEND")) { mode ->
          TacticalFilterChip(
            selected = settings.rideMode.equals(mode, true),
            onClick = { sessionVm.updateSettings { it.copy(rideMode = mode) } },
            label = mode,
          )
        }
      }
      Text("Rider + Gear Weight: ${settings.riderWeightLbs} lbs", style = MaterialTheme.typography.labelSmall)
      TacticalSlider(value = settings.riderWeightLbs.coerceIn(90, 350).toFloat(), onValueChange = { v -> sessionVm.updateSettings { it.copy(riderWeightLbs = v.toInt()) } }, valueRange = 90f..350f)
      Text("Daily Distance Goal: ${"%.0f".format(settings.dailyDistanceGoalKm)} km", style = MaterialTheme.typography.labelSmall)
      TacticalSlider(value = settings.dailyDistanceGoalKm.coerceIn(5f, 100f), onValueChange = { v -> sessionVm.updateSettings { it.copy(dailyDistanceGoalKm = v) } }, valueRange = 5f..100f)
      Text("Low Battery Alert: ${settings.lowBatteryWarningPct}%", style = MaterialTheme.typography.labelSmall)
      TacticalSlider(value = settings.lowBatteryWarningPct.coerceIn(5, 45).toFloat(), onValueChange = { v -> sessionVm.updateSettings { it.copy(lowBatteryWarningPct = v.toInt()) } }, valueRange = 5f..45f)
      Text("Battery Reserve Return Buffer: ${settings.batteryReserveWarningPct}%", style = MaterialTheme.typography.labelSmall)
      TacticalSlider(value = settings.batteryReserveWarningPct.coerceIn(5, 50).toFloat(), onValueChange = { v -> sessionVm.updateSettings { it.copy(batteryReserveWarningPct = v.toInt()) } }, valueRange = 5f..50f)
      Text("Low Voltage Alarm: ${"%.1f".format(settings.criticalVoltageAlertV)}V", style = MaterialTheme.typography.labelSmall)
      TacticalSlider(value = settings.criticalVoltageAlertV.coerceIn(32f, 110f), onValueChange = { v -> sessionVm.updateSettings { it.copy(criticalVoltageAlertV = v) } }, valueRange = 32f..110f)
      AndroidSettingSwitch("Auto-Pause Ride When Stopped", checked = settings.autoPauseRideWhenStopped) { v -> sessionVm.updateSettings { it.copy(autoPauseRideWhenStopped = v) } }
      AndroidSettingSwitch("Ask Before Stopping Ride", checked = settings.confirmBeforeEndRide) { v -> sessionVm.updateSettings { it.copy(confirmBeforeEndRide = v) } }
      AndroidSettingSwitch("Phone Battery Saver Mode", checked = settings.batterySaverTelemetry) { v -> sessionVm.updateSettings { it.copy(batterySaverTelemetry = v) } }
      AndroidSettingSwitch("Auto-Join Group Ride Invites", checked = settings.autoAcceptConvoyInvites) { v -> sessionVm.updateSettings { it.copy(autoAcceptConvoyInvites = v) } }
      AndroidSettingSwitch("Hide Exact Location (Privacy Mode)", checked = settings.stealthPrivacyMode) { v -> sessionVm.updateSettings { it.copy(stealthPrivacyMode = v) } }
    }
  }
}

@Composable
fun SoundRingtonesNotifySettingsContent(
  settings: MasterSettings,
  sessionVm: SessionViewModel,
  selectedSubCategory: String? = null,
  onSelectSubCategory: (String) -> Unit = {},
  onLaunchSystemRingtonePicker: (Intent) -> Unit,
  onRequestNotifPermission: () -> Unit,
  onStatusMessage: (String) -> Unit,
) {
  val ctx = LocalContext.current
  if (selectedSubCategory == null) {
    AndroidSettingsSubCategoryMenu(
      sectionHeader = "Touch, Ringtones & Vibration",
      items =
        listOf(
          AndroidSubCategorySpec(
            id = "Haptics & Vibration",
            title = "Studio Touch Sounds & Haptics",
            summary = "${if (settings.touchSoundsEnabled) settings.touchSoundPack else "Muted"} • ${if (settings.hapticFeedback) "Haptics ${settings.hapticDurationMs}ms" else "Haptics Off"}",
            icon = Icons.Default.Vibration,
            accentColor = Color(0xFF00C2FF),
          ),
          AndroidSubCategorySpec(
            id = "Ringtones & Alerts",
            title = "Studio Ringtones & Polyphonic Alerts",
            summary = "${settings.callRingtonePreset.label} • Ring ${settings.ringtoneVolumePct}% • ${settings.soundPack} Pack",
            icon = Icons.Default.MusicNote,
            accentColor = Color(0xFFFF9F1C),
          ),
        ),
      secondaryHeader = "Voice, Wind Filter & Reminders",
      secondaryItems =
        listOf(
          AndroidSubCategorySpec(
            id = "Voice & Reminders",
            title = "Spoken Voice & Care Reminders",
            summary = "Speed ${"%.2fx".format(settings.ttsSpeechRate)} • PSI every ${settings.psiReminderDays}d • ${if (settings.remindersEnabled) "Active" else "Off"}",
            icon = Icons.Default.NotificationsActive,
            accentColor = Color(0xFF39FF14),
          ),
          AndroidSubCategorySpec(
            id = "Audio Studio & Wind Filter",
            title = "Audio Studio, Ducking & Wind Filter",
            summary = "Callouts: ${settings.voiceCalloutVerbosity} • ${if (settings.windNoiseFilter) "Wind Filter On" else "Off"} • ${if (settings.audioDuckingOnSpeech) "Ducking On" else "Off"}",
            icon = Icons.Default.GraphicEq,
            accentColor = Color(0xFFA855F7),
          ),
        ),
      onSelect = onSelectSubCategory,
    )
    return
  }

  val subCat = selectedSubCategory
  if (subCat == "Audio Studio & Wind Filter") {
    AudioStudioFilterSettingsPage(settings = settings, sessionVm = sessionVm)
    return
  }

  if (subCat == "All" || subCat == "Haptics & Vibration") {
    TacticalCard(highlight = true, title = "Studio Touch Sounds & Haptics") {
      Text("STUDIO TOUCH SOUNDS & HAPTICS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
      Spacer(Modifier.height(6.dp))
      AndroidSettingSwitch(
        label = "Studio Touch Sounds (Acoustic & Chime)",
        subtitle = "Plays rich 44.1kHz polyphonic acoustic plucks, marimba & chimes on taps (no raw beeps)",
        checked = settings.touchSoundsEnabled,
      ) { v ->
        sessionVm.updateSettings { it.copy(touchSoundsEnabled = v) }
        if (v) {
          sessionVm.voiceAudio.playTouchSound(
            enabled = true,
            pack = settings.touchSoundPack,
            cue = VoiceAudioManager.TouchCue.TOGGLE_ON,
            volumePct = settings.globalVolume,
          )
        }
      }
      Text("Touch Sound Palette (${settings.touchSoundPack})", style = MaterialTheme.typography.labelSmall)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(listOf("Country Acoustic", "Warm Marimba", "Studio Glass", "Soft Tactile", "Silent")) { pack ->
          TacticalFilterChip(
            selected = settings.touchSoundPack == pack,
            onClick = {
              sessionVm.updateSettings { it.copy(touchSoundPack = pack, touchSoundsEnabled = pack != "Silent") }
              if (pack != "Silent") {
                sessionVm.voiceAudio.playTouchSound(
                  enabled = true,
                  pack = pack,
                  cue = VoiceAudioManager.TouchCue.ACTION_CONFIRM,
                  volumePct = settings.globalVolume,
                )
                onStatusMessage("Previewed $pack studio touch sound!")
              }
            },
            label = pack,
            modifier = Modifier.testTag("touch_sound_pack_${pack.lowercase().replace(" ", "_")}"),
          )
        }
      }
      Spacer(Modifier.height(6.dp))
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TacticalOutlinedButton(
          onClick = {
            sessionVm.voiceAudio.playTouchSound(true, settings.touchSoundPack, VoiceAudioManager.TouchCue.BUTTON_TAP, settings.globalVolume)
          },
          modifier = Modifier.weight(1f).testTag("test_touch_tap_button"),
        ) {
          Text("Tap Cue", maxLines = 1, softWrap = false)
        }
        TacticalOutlinedButton(
          onClick = {
            sessionVm.voiceAudio.playTouchSound(true, settings.touchSoundPack, VoiceAudioManager.TouchCue.MESSAGE_SENT, settings.globalVolume)
          },
          modifier = Modifier.weight(1f).testTag("test_touch_send_button"),
        ) {
          Text("Send Cue", maxLines = 1, softWrap = false)
        }
        TacticalOutlinedButton(
          onClick = {
            sessionVm.voiceAudio.playTouchSound(true, settings.touchSoundPack, VoiceAudioManager.TouchCue.RIDE_START, settings.globalVolume)
          },
          modifier = Modifier.weight(1f).testTag("test_touch_ride_button"),
        ) {
          Text("Roll Out", maxLines = 1, softWrap = false)
        }
      }
      HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      AndroidSettingSwitch(
        label = "Master Haptic Feedback",
        subtitle = "Enables crisp physical vibration responses throughout the app",
        checked = settings.hapticFeedback,
      ) { v ->
        sessionVm.updateSettings { it.copy(hapticFeedback = v) }
        if (v) sessionVm.voiceAudio.triggerHapticCue(true, VoiceAudioManager.HapticCue.SUCCESS_DOUBLE)
      }
      AndroidSettingSwitch(
        label = "Button & Tab Tap Haptics",
        subtitle = "Subtle tactile click when switching tabs or tapping quick controls",
        checked = settings.buttonHapticFeedback,
      ) { v -> sessionVm.updateSettings { it.copy(buttonHapticFeedback = v) } }
      AndroidSettingSwitch(
        label = "Ride Start, Stop & AR Capture Haptics",
        subtitle = "Distinct pulse when starting/stopping a ride or snapping an AR photo",
        checked = settings.rideMilestoneHaptics,
      ) { v -> sessionVm.updateSettings { it.copy(rideMilestoneHaptics = v) } }
      Text("Haptic Strength Duration: ${settings.hapticDurationMs} ms", style = MaterialTheme.typography.labelSmall)
      TacticalSlider(
        value = settings.hapticDurationMs.toFloat().coerceIn(10f, 65f),
        onValueChange = { v -> sessionVm.updateSettings { it.copy(hapticDurationMs = v.toInt()) } },
        valueRange = 10f..65f,
      )
      Text("Alert Vibration Pattern (${settings.vibrationPattern.label})", style = MaterialTheme.typography.labelSmall)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(VibrationPatternPreset.entries) { pat ->
          TacticalFilterChip(
            selected = settings.vibrationPattern == pat,
            onClick = {
              sessionVm.updateSettings { it.copy(vibrationPattern = pat) }
              sessionVm.voiceAudio.triggerVibrationPattern(pat)
            },
            leadingIcon = Icons.Default.Vibration,
            label = pat.label,
          )
        }
      }
      Spacer(Modifier.height(8.dp))
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TacticalOutlinedButton(
          onClick = {
            val ok = sessionVm.voiceAudio.triggerHapticCue(true, VoiceAudioManager.HapticCue.SUCCESS_DOUBLE, settings.hapticDurationMs)
            onStatusMessage(if (ok) "Triggered double-click haptic feel!" else "Device has no vibration motor.")
          },
          modifier = Modifier.weight(1f).testTag("test_haptic_double_button"),
        ) {
          Icon(Icons.Default.Vibration, null, Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("Test Tap", maxLines = 1, softWrap = false)
        }
        TacticalOutlinedButton(
          onClick = {
            val ok = sessionVm.voiceAudio.triggerHapticCue(true, VoiceAudioManager.HapticCue.WARNING_PULSE, settings.hapticDurationMs)
            onStatusMessage(if (ok) "Triggered safety warning vibration!" else "Device has no vibration motor.")
          },
          modifier = Modifier.weight(1f).testTag("test_haptic_alert_button"),
        ) {
          Text("Test Alert", maxLines = 1, softWrap = false)
        }
      }
    }
    Spacer(Modifier.height(12.dp))
  }

  if (subCat == "All" || subCat == "Ringtones & Alerts") {
    TacticalCard(title = "Studio Ringtones & Polyphonic Alerts") {
      Text("STUDIO RINGTONES & POLYPHONIC ALERTS (44.1kHz)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
      Spacer(Modifier.height(6.dp))
      Text("Incoming Call Ringtone (${RingtonePreset.entries.size} Studio Melodies)", style = MaterialTheme.typography.labelSmall)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(RingtonePreset.entries) { rt ->
          TacticalFilterChip(
            selected = settings.callRingtonePreset == rt,
            onClick = {
              sessionVm.updateSettings { it.copy(callRingtonePreset = rt) }
              sessionVm.voiceAudio.playRingtonePreset(
                preset = rt,
                customUri = settings.customRingtoneUri,
                volumePct = settings.ringtoneVolumePct,
              )
              onStatusMessage("Selected & previewed ringtone: ${rt.label}")
            },
            label = rt.label,
            modifier = Modifier.testTag("ringtone_preset_${rt.id}"),
          )
        }
      }
      Spacer(Modifier.height(6.dp))
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TacticalButton(
          onClick = {
            sessionVm.voiceAudio.playRingtonePreset(
              preset = settings.callRingtonePreset,
              customUri = settings.customRingtoneUri,
              volumePct = settings.ringtoneVolumePct,
            )
            onStatusMessage("Playing ${settings.callRingtonePreset.label}...")
          },
          modifier = Modifier.weight(1f).testTag("preview_ringtone_button"),
        ) {
          Icon(Icons.Default.PlayArrow, null)
          Spacer(Modifier.width(4.dp))
          Text("Preview")
        }
        TacticalOutlinedButton(
          onClick = {
            sessionVm.voiceAudio.stopRingtone()
            onStatusMessage("Ringtone stopped.")
          },
          modifier = Modifier.testTag("stop_ringtone_button"),
        ) {
          Icon(Icons.Default.Stop, null)
          Spacer(Modifier.width(4.dp))
          Text("Stop")
        }
        TacticalOutlinedButton(
          onClick = {
            val intent =
              Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Call Ringtone")
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
              }
            onLaunchSystemRingtonePicker(intent)
          },
          modifier = Modifier.testTag("pick_system_ringtone_button"),
        ) {
          Icon(Icons.Default.MusicNote, null)
          Spacer(Modifier.width(4.dp))
          Text("Phone")
        }
      }
      Spacer(Modifier.height(6.dp))
      Text("Ringtone Volume: ${settings.ringtoneVolumePct}%", style = MaterialTheme.typography.labelSmall)
      TacticalSlider(value = settings.ringtoneVolumePct.toFloat(), onValueChange = { v -> sessionVm.updateSettings { it.copy(ringtoneVolumePct = v.toInt()) } }, valueRange = 0f..100f)

      Text("Direct Message Sound", style = MaterialTheme.typography.labelSmall)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(AlertTonePreset.entries) { tone ->
          TacticalFilterChip(
            selected = settings.dmAlertTone == tone,
            onClick = {
              sessionVm.updateSettings { it.copy(dmAlertTone = tone) }
              sessionVm.voiceAudio.playAlertTonePreset(tone, settings.globalVolume)
            },
            label = tone.label,
          )
        }
      }
      Text("Walkie-Talkie Roger Beep", style = MaterialTheme.typography.labelSmall)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(AlertTonePreset.entries) { tone ->
          TacticalFilterChip(
            selected = settings.walkieRogerTone == tone,
            onClick = {
              sessionVm.updateSettings { it.copy(walkieRogerTone = tone) }
              sessionVm.voiceAudio.playAlertTonePreset(tone, settings.globalVolume)
            },
            label = tone.label,
          )
        }
      }
      Text("Navigation Turn Chime", style = MaterialTheme.typography.labelSmall)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(AlertTonePreset.entries) { tone ->
          TacticalFilterChip(
            selected = settings.navTurnTone == tone,
            onClick = {
              sessionVm.updateSettings { it.copy(navTurnTone = tone) }
              sessionVm.voiceAudio.playAlertTonePreset(tone, settings.globalVolume)
            },
            label = tone.label,
          )
        }
      }
      Text("Low-Battery Warning Tone", style = MaterialTheme.typography.labelSmall)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(AlertTonePreset.entries) { tone ->
          TacticalFilterChip(
            selected = settings.lowBatteryAlarmTone == tone,
            onClick = {
              sessionVm.updateSettings { it.copy(lowBatteryAlarmTone = tone) }
              sessionVm.voiceAudio.playAlertTonePreset(tone, settings.globalVolume)
            },
            label = tone.label,
          )
        }
      }
      Text("Master App Volume: ${settings.globalVolume}%", style = MaterialTheme.typography.labelSmall)
      TacticalSlider(value = settings.globalVolume.toFloat(), onValueChange = { v -> sessionVm.updateSettings { it.copy(globalVolume = v.toInt()) } }, valueRange = 0f..100f)
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("Tactical", "Aviation", "Retro", "Minimal").forEach { pack ->
          TacticalFilterChip(selected = settings.soundPack == pack, onClick = { sessionVm.updateSettings { it.copy(soundPack = pack) } }, label = pack)
        }
      }
      TacticalOutlinedButton(
        onClick = {
          sessionVm.voiceAudio.playSoundPackPreview(settings.soundPack, settings.globalVolume)
          onStatusMessage("Played ${settings.soundPack} sound preview.")
        },
        modifier = Modifier.fillMaxWidth().testTag("test_sound_pack_button"),
      ) {
        Icon(Icons.Default.VolumeUp, null)
        Spacer(Modifier.width(8.dp))
        Text("Test ${settings.soundPack} Sound Pack")
      }
    }
    Spacer(Modifier.height(12.dp))
  }

  if (subCat == "All" || subCat == "Voice & Reminders") {
    TacticalCard(title = "Spoken Voice & Care Reminders") {
      Text("SPOKEN VOICE & MAINTENANCE REMINDERS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
      Spacer(Modifier.height(6.dp))
      Text("Voice Speed: ${"%.2fx".format(settings.ttsSpeechRate)} • Voice Pitch: ${"%.2f".format(settings.ttsPitch)}", style = MaterialTheme.typography.labelSmall)
      TacticalSlider(value = settings.ttsSpeechRate, onValueChange = { v -> sessionVm.updateSettings { it.copy(ttsSpeechRate = v) } }, valueRange = 0.6f..1.6f)
      TacticalSlider(value = settings.ttsPitch, onValueChange = { v -> sessionVm.updateSettings { it.copy(ttsPitch = v) } }, valueRange = 0.6f..1.5f)
      TacticalOutlinedButton(
        onClick = {
          sessionVm.voiceAudio.speakWithVoiceProfile(
            text = "Rural E Rides Go spoken voice and directions are ready.",
            profile = settings.aiVoiceProfile,
            volumePct = settings.globalVolume,
            speechRateMult = settings.ttsSpeechRate,
            pitchMult = settings.ttsPitch,
          )
        },
        modifier = Modifier.fillMaxWidth().testTag("test_tts_voice_button"),
      ) {
        Icon(Icons.Default.RecordVoiceOver, null)
        Spacer(Modifier.width(8.dp))
        Text("Test Spoken Voice Speed & Pitch")
      }
      Spacer(Modifier.height(6.dp))
      AndroidSettingSwitch("Classic Retro Sound Effects", checked = settings.retroAudioCuesEnabled) { v -> sessionVm.updateSettings { it.copy(retroAudioCuesEnabled = v) } }
      AndroidSettingSwitch("Mute Non-Urgent Alerts While Riding", checked = settings.autoMuteAlertsWhileRiding) { v -> sessionVm.updateSettings { it.copy(autoMuteAlertsWhileRiding = v) } }
      AndroidSettingSwitch("Regenerative Braking Chime", checked = settings.regenBrakingChime) { v -> sessionVm.updateSettings { it.copy(regenBrakingChime = v) } }
      AndroidSettingSwitch("Direct Message Alert Sound", checked = settings.dmChime) { v -> sessionVm.updateSettings { it.copy(dmChime = v) } }
      AndroidSettingSwitch("Incoming Call Ringtone", checked = settings.callRingtoneEnabled) { v -> sessionVm.updateSettings { it.copy(callRingtoneEnabled = v) } }
      AndroidSettingSwitch("Mute Alerts During Videos", checked = settings.autoMuteOnVideo) { v -> sessionVm.updateSettings { it.copy(autoMuteOnVideo = v) } }
      AndroidSettingSwitch("Scheduled Maintenance Reminders", checked = settings.remindersEnabled) { v ->
        if (v && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !ReminderScheduler.hasNotificationPermission(ctx)) {
          onRequestNotifPermission()
        } else {
          sessionVm.updateSettings { it.copy(remindersEnabled = v) }
        }
      }
      AndroidSettingSwitch("Severe Weather Warnings", checked = settings.weatherAlertsEnabled) { v -> sessionVm.updateSettings { it.copy(weatherAlertsEnabled = v) } }
      AndroidSettingSwitch("Quiet Hours (${settings.quietStartHour}:00 - ${settings.quietEndHour}:00)", checked = settings.quietHoursEnabled) { v -> sessionVm.updateSettings { it.copy(quietHoursEnabled = v) } }
      Text("Tire Pressure Check Reminder: Every ${settings.psiReminderDays} days", style = MaterialTheme.typography.labelSmall)
      TacticalSlider(value = settings.psiReminderDays.toFloat(), onValueChange = { v -> sessionVm.updateSettings { it.copy(psiReminderDays = v.toInt()) } }, valueRange = 1f..14f)
      Text("Service Reminder Distance: Every ${settings.maintReminderKm} km", style = MaterialTheme.typography.labelSmall)
      TacticalSlider(value = settings.maintReminderKm.toFloat(), onValueChange = { v -> sessionVm.updateSettings { it.copy(maintReminderKm = (v.toInt() / 50) * 50) } }, valueRange = 50f..1000f)
      TacticalOutlinedButton(
        onClick = {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !ReminderScheduler.hasNotificationPermission(ctx)) {
            onRequestNotifPermission()
          } else {
            val posted =
              ReminderScheduler.postNotification(
                ctx,
                "Rural ERides Test Reminder",
                "Tire pressure & safety check scheduled every ${settings.psiReminderDays} days.",
              )
            onStatusMessage(if (posted) "Test reminder notification sent!" else "Enable notifications in Android Settings.")
          }
        },
        modifier = Modifier.fillMaxWidth().testTag("test_reminder_notification_button"),
      ) {
        Icon(Icons.Default.NotificationsActive, null)
        Spacer(Modifier.width(8.dp))
        Text("Send Test Reminder")
      }
    }
  }
}

@Composable
fun NavRadarLegalSettingsContent(
  settings: MasterSettings,
  sessionVm: SessionViewModel,
  selectedSubCategory: String? = null,
  onSelectSubCategory: (String) -> Unit = {},
) {
  if (selectedSubCategory == null) {
    AndroidSettingsSubCategoryMenu(
      sectionHeader = "Navigation & Legal Governor",
      items =
        listOf(
          AndroidSubCategorySpec(
            id = "Turn-by-Turn Nav",
            title = "Turn-by-Turn GPS Directions",
            summary = "${settings.navDefaultProfile.label} • ${if (settings.navAvoidHighways) "Avoid Highways" else "All Roads"} • ${if (settings.navVoiceGuidance) "Voice On" else "Silent"}",
            icon = Icons.Default.Navigation,
            accentColor = Color(0xFF3B82F6),
          ),
          AndroidSubCategorySpec(
            id = "Speed Rules & Privacy",
            title = "Speed Rules, GPS & Privacy",
            summary = "${settings.pevLegalClass.label.substringBefore(" (")} • Radar ${settings.radarRadiusMiles}mi • Privacy ${settings.privacyZoneMeters}m",
            icon = Icons.Default.Speed,
            accentColor = Color(0xFFF43F5E),
          ),
        ),
      secondaryHeader = "Backroad & Rural Trail Intelligence",
      secondaryItems =
        listOf(
          AndroidSubCategorySpec(
            id = "Rural Trails & Wildlife",
            title = "Rural Roads, Wildlife & Convoy GPX",
            summary = "Wildlife ${settings.wildlifeAlertRadiusMi}mi • ${if (settings.farmEquipmentAlertEnabled) "Farm Alerts" else "Off"} • Beacon ${settings.convoyBeaconIntervalSec}s",
            icon = Icons.Default.Terrain,
            accentColor = Color(0xFF39FF14),
          ),
        ),
      onSelect = onSelectSubCategory,
    )
    return
  }

  val subCat = selectedSubCategory
  if (subCat == "Rural Trails & Wildlife") {
    RuralTrailsWildlifeSettingsPage(settings = settings, sessionVm = sessionVm)
    return
  }

  if (subCat == "All" || subCat == "Turn-by-Turn Nav") {
    TacticalCard(highlight = true, title = "Turn-by-Turn GPS Directions") {
      Text("TURN-BY-TURN GPS DIRECTIONS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
      Spacer(Modifier.height(6.dp))
      Text("Preferred Route Type", style = MaterialTheme.typography.labelSmall)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(NavRouteProfile.entries) { prof ->
          TacticalFilterChip(
            selected = settings.navDefaultProfile == prof,
            onClick = { sessionVm.updateSettings { it.copy(navDefaultProfile = prof) } },
            label = prof.label,
          )
        }
      }
      AndroidSettingSwitch("Spoken Turn-by-Turn Directions", checked = settings.navVoiceGuidance) { v -> sessionVm.updateSettings { it.copy(navVoiceGuidance = v) } }
      AndroidSettingSwitch("Auto-Reroute If Off Course", checked = settings.navAutoReroute) { v -> sessionVm.updateSettings { it.copy(navAutoReroute = v) } }
      AndroidSettingSwitch("Avoid Highways (Safer Electric Routes)", checked = settings.navAvoidHighways) { v -> sessionVm.updateSettings { it.copy(navAvoidHighways = v) } }
      AndroidSettingSwitch("Show Nearby Charging Stations on Map", checked = settings.navShowChargersOnMap) { v -> sessionVm.updateSettings { it.copy(navShowChargersOnMap = v) } }
      AndroidSettingSwitch("Rotate Map in Direction of Travel", checked = settings.navHeadingUpMode) { v -> sessionVm.updateSettings { it.copy(navHeadingUpMode = v) } }
      AndroidSettingSwitch("Auto-Follow Map Heading", subtitle = "Keeps the map smoothly oriented with your travel bearing", checked = settings.mapAutoFollowHeading) { v -> sessionVm.updateSettings { it.copy(mapAutoFollowHeading = v) } }
      AndroidSettingSwitch("Show Live Road Speed Limit Sign", checked = settings.showSpeedLimitOverlay) { v -> sessionVm.updateSettings { it.copy(showSpeedLimitOverlay = v) } }
      AndroidSettingSwitch("Show Floating Speed Limit Badge", subtitle = "Displays a compact road limit pill on map and cockpit views", checked = settings.showSpeedLimitBadge) { v -> sessionVm.updateSettings { it.copy(showSpeedLimitBadge = v) } }
      AndroidSettingSwitch("Confirm Before Cancelling Navigation", subtitle = "Asks once before ending an active turn-by-turn GPS route", checked = settings.confirmCancelNavigation) { v -> sessionVm.updateSettings { it.copy(confirmCancelNavigation = v) } }
      AndroidSettingSwitch("Auto-End Navigation on Arrival", subtitle = "Automatically finishes guidance after announcing arrival", checked = settings.navAutoEndOnArrival) { v -> sessionVm.updateSettings { it.copy(navAutoEndOnArrival = v) } }
    }
    Spacer(Modifier.height(12.dp))
  }

  if (subCat == "All" || subCat == "Speed Rules & Privacy") {
    TacticalCard(title = "Speed Rules, GPS & Privacy") {
      Text("SPEED RULES, GPS & PRIVACY", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
      AndroidSettingSwitch("Speed Limit Alerts & Class Cap", checked = settings.legalComplianceMode) { v -> sessionVm.updateSettings { it.copy(legalComplianceMode = v) } }
      Text("Electric Vehicle Speed Class", style = MaterialTheme.typography.labelSmall)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(PevLegalClass.entries) { cls ->
          TacticalFilterChip(selected = settings.pevLegalClass == cls, onClick = { sessionVm.updateSettings { it.copy(pevLegalClass = cls) } }, label = cls.label)
        }
      }
      AndroidSettingSwitch("Strict School Zone Cap (20 MPH)", checked = settings.schoolZoneStrict) { v -> sessionVm.updateSettings { it.copy(schoolZoneStrict = v) } }
      AndroidSettingSwitch("Play Beep When Over Speed Limit", checked = settings.legalAudioAlert) { v -> sessionVm.updateSettings { it.copy(legalAudioAlert = v) } }
      AndroidSettingSwitch("Flash Speedometer at Max Speed", checked = settings.hudBlinkMax) { v -> sessionVm.updateSettings { it.copy(hudBlinkMax = v) } }
      Text("Speed Alert Buffer: +${settings.speedOffsetTolerance} MPH", style = MaterialTheme.typography.labelSmall)
      TacticalSlider(value = settings.speedOffsetTolerance.toFloat(), onValueChange = { v -> sessionVm.updateSettings { it.copy(speedOffsetTolerance = v.toInt()) } }, valueRange = 0f..15f)
      AndroidSettingSwitch("Auto-Follow My Position on Map", checked = settings.radarAutoFollow) { v -> sessionVm.updateSettings { it.copy(radarAutoFollow = v) } }
      AndroidSettingSwitch("Auto-Zoom Map Based on Speed", checked = settings.radarSpeedZoom) { v -> sessionVm.updateSettings { it.copy(radarSpeedZoom = v) } }
      AndroidSettingSwitch("Show My Profile on Local Map", checked = settings.publicProfile) { v -> sessionVm.updateSettings { it.copy(publicProfile = v) } }
      AndroidSettingSwitch("Include Me on Community Leaderboard", checked = settings.shareLeaderboard) { v -> sessionVm.updateSettings { it.copy(shareLeaderboard = v) } }
      AndroidSettingSwitch("Auto-Refresh Local Weather", checked = settings.autoWeatherSync) { v -> sessionVm.updateSettings { it.copy(autoWeatherSync = v) } }
      Text("Nearby Map Search Radius: ${settings.radarRadiusMiles} mi", style = MaterialTheme.typography.labelSmall)
      TacticalSlider(value = settings.radarRadiusMiles.toFloat(), onValueChange = { v -> sessionVm.updateSettings { it.copy(radarRadiusMiles = v.toInt()) } }, valueRange = 5f..100f)
      Text("GPS Refresh Rate: ${settings.telemetryRateHz}x/sec • Accuracy Filter: ${settings.gpsAccuracyLimitM}m", style = MaterialTheme.typography.labelSmall)
      TacticalSlider(value = settings.telemetryRateHz.toFloat(), onValueChange = { v -> sessionVm.updateSettings { it.copy(telemetryRateHz = v.toInt()) } }, valueRange = 1f..5f)
      Text("Home Address Privacy Radius: ${settings.privacyZoneMeters}m", style = MaterialTheme.typography.labelSmall)
      TacticalSlider(value = settings.privacyZoneMeters.toFloat(), onValueChange = { v -> sessionVm.updateSettings { it.copy(privacyZoneMeters = (v.toInt() / 50) * 50) } }, valueRange = 0f..1000f)
    }
  }
}

@Composable
fun AiCoreChatSettingsContent(
  settings: MasterSettings,
  sessionVm: SessionViewModel,
  selectedSubCategory: String? = null,
  onSelectSubCategory: (String) -> Unit = {},
) {
  if (selectedSubCategory == null) {
    AndroidSettingsSubCategoryMenu(
      sectionHeader = "AI Co-Pilot & Barn Mechanic",
      items =
        listOf(
          AndroidSubCategorySpec(
            id = "AI Brain",
            title = "AI Engine, Depth & Web Search",
            summary = "${settings.aiModelId.substringBefore("-preview")} • Thinking: ${settings.aiThinkingLevel} • ${if (settings.aiWebGrounding) "Live Web On" else "Offline"}",
            icon = Icons.Default.AutoAwesome,
            accentColor = Color(0xFFA855F7),
          ),
          AndroidSubCategorySpec(
            id = "Voice & Rural Warmth",
            title = "AI Voice & Rural Hospitality",
            summary = "${settings.aiVoiceProfile.label} • ${if (settings.ruralPersonaMode) "Warm Rural Tone" else "Standard"}",
            icon = Icons.Default.RecordVoiceOver,
            accentColor = Color(0xFFFF9F1C),
          ),
          AndroidSubCategorySpec(
            id = "Chat Comfort",
            title = "Chat Comfort & Inspection Depth",
            summary = "Mechanic: ${settings.mechanicDefaultDepth} • ${if (settings.autoSpeakAiReplies) "Auto-Speak On" else "Tap to Speak"}",
            icon = Icons.Default.Chat,
            accentColor = Color(0xFF00C2FF),
          ),
        ),
      onSelect = onSelectSubCategory,
    )
    return
  }

  val subCat = selectedSubCategory
  if (subCat == "All" || subCat == "AI Brain") {
    TacticalCard(highlight = true, title = "AI Helper Brain & Answers") {
      Text("AI MODEL & ANSWER STYLE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
      Spacer(Modifier.height(6.dp))
      Text("Active AI Engine", style = MaterialTheme.typography.labelSmall)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(GeminiModelCatalog.availableModels) { (modelId, label) ->
          TacticalFilterChip(
            selected = settings.aiModelId == modelId,
            onClick = { sessionVm.updateSettings { it.copy(aiModelId = modelId) } },
            label = label,
          )
        }
      }
      Spacer(Modifier.height(6.dp))
      Text("Thinking Depth", style = MaterialTheme.typography.labelSmall)
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("low" to "Fast", "medium" to "Balanced", "high" to "Deep").forEach { (lvl, lbl) ->
          TacticalFilterChip(selected = settings.aiThinkingLevel == lvl, onClick = { sessionVm.updateSettings { it.copy(aiThinkingLevel = lvl) } }, label = lbl)
        }
      }
      Text("Answer Length", style = MaterialTheme.typography.labelSmall)
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("concise" to "Short", "standard" to "Normal", "exhaustive" to "Detailed").forEach { (det, lbl) ->
          TacticalFilterChip(selected = settings.aiDetailLevel == det, onClick = { sessionVm.updateSettings { it.copy(aiDetailLevel = det) } }, label = lbl)
        }
      }
      Text("Creativity Level: ${"%.2f".format(settings.aiCreativity)}", style = MaterialTheme.typography.labelSmall)
      TacticalSlider(value = settings.aiCreativity, onValueChange = { v -> sessionVm.updateSettings { it.copy(aiCreativity = v) } }, valueRange = 0.1f..1.2f)
      AndroidSettingSwitch(
        label = "Use Live Web Search in AI Helper",
        subtitle = "Looks up up-to-date local trails, laws, and parts online",
        checked = settings.aiWebGrounding,
      ) { v -> sessionVm.updateSettings { it.copy(aiWebGrounding = v) } }
    }
    Spacer(Modifier.height(12.dp))
  }

  if (subCat == "All" || subCat == "Voice & Rural Warmth") {
    TacticalCard(title = "AI Voice & Rural Hospitality") {
      Text("AI VOICE & RURAL HOSPITALITY", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
      Spacer(Modifier.height(6.dp))
      AndroidSettingSwitch(
        label = "Warm Rural Hospitality Tone",
        subtitle = "Keeps your AI Helper & Repair Shop friendly, neighborly, and down-to-earth everywhere you go",
        checked = settings.ruralPersonaMode,
      ) { v -> sessionVm.updateSettings { it.copy(ruralPersonaMode = v) } }
      HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
      Text(
        "AI Voice Type (${AiVoiceProfile.entries.size} Voices — ${settings.aiVoiceProfile.label})",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
      )
      Text(settings.aiVoiceProfile.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(Modifier.height(6.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(AiVoiceProfile.entries) { vp ->
          TacticalFilterChip(
            selected = settings.aiVoiceProfile == vp,
            onClick = {
              sessionVm.updateSettings { it.copy(aiVoiceProfile = vp) }
              sessionVm.voiceAudio.previewVoiceProfile(vp, settings.globalVolume)
            },
            label = vp.label,
            modifier = Modifier.testTag("settings_ai_voice_${vp.id}"),
          )
        }
      }
      Spacer(Modifier.height(8.dp))
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TacticalOutlinedButton(
          onClick = { sessionVm.voiceAudio.previewVoiceProfile(settings.aiVoiceProfile, settings.globalVolume) },
          modifier = Modifier.weight(1f).testTag("settings_preview_ai_voice_button"),
        ) {
          Icon(Icons.Default.RecordVoiceOver, null, Modifier.size(16.dp))
          Spacer(Modifier.width(6.dp))
          Text("Preview Voice")
        }
        TacticalOutlinedButton(
          onClick = { sessionVm.voiceAudio.stopSpeaking() },
          modifier = Modifier.testTag("settings_stop_ai_voice_button"),
        ) {
          Icon(Icons.Default.Stop, null, Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Stop")
        }
      }
    }
    Spacer(Modifier.height(12.dp))
  }

  if (subCat == "All" || subCat == "Chat Comfort") {
    TacticalCard(title = "Chat Window Comfort") {
      Text("CHAT WINDOW COMFORT", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
      AndroidSettingSwitch(
        label = "Auto-Speak AI Helper & Mechanic Replies",
        subtitle = "Automatically reads out new AI responses using your chosen voice profile",
        checked = settings.autoSpeakAiReplies,
      ) { v -> sessionVm.updateSettings { it.copy(autoSpeakAiReplies = v) } }
      AndroidSettingSwitch(
        label = "Auto-Scroll to Newest Reply",
        subtitle = "Follows live AI replies automatically in AI Helper & Repair Shop",
        checked = settings.aiChatAutoScroll,
      ) { v -> sessionVm.updateSettings { it.copy(aiChatAutoScroll = v) } }
      AndroidSettingSwitch(
        label = "Spacious Chat Box (Hide Extra Toolbars)",
        subtitle = "Gives you a bigger, easier-to-read conversation view on phones",
        checked = settings.aiChatCompactTools,
      ) { v -> sessionVm.updateSettings { it.copy(aiChatCompactTools = v) } }
      Spacer(Modifier.height(6.dp))
      Text("Default Repair Shop Inspection Depth", style = MaterialTheme.typography.labelSmall)
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("QUICK" to "Quick", "STANDARD" to "Standard", "DEEP" to "Deep Tear-Down").forEach { (depthId, depthLbl) ->
          TacticalFilterChip(
            selected = settings.mechanicDefaultDepth.equals(depthId, ignoreCase = true),
            onClick = { sessionVm.updateSettings { it.copy(mechanicDefaultDepth = depthId) } },
            label = depthLbl,
            modifier = Modifier.testTag("settings_mech_depth_${depthId.lowercase()}"),
          )
        }
      }
    }
  }
}

@Composable
fun SafetyCommunitySettingsContent(
  settings: MasterSettings,
  sessionVm: SessionViewModel,
  selectedSubCategory: String? = null,
  onSelectSubCategory: (String) -> Unit = {},
) {
  if (selectedSubCategory == null) {
    AndroidSettingsSubCategoryMenu(
      sectionHeader = "Emergency & Rider Security",
      items =
        listOf(
          AndroidSubCategorySpec(
            id = "Crash & SOS",
            title = "Crash Detection, Lock & SOS",
            summary = "Impact ${"%.1f".format(settings.crashSensitivityG.coerceIn(6f, 15f))}G • Cancel ${settings.sosCancelWindowSec}s • ${if (settings.biometricAppLock) "Biometric Lock" else "Unlocked"}",
            icon = Icons.Default.Shield,
            accentColor = Color(0xFFF43F5E),
          ),
          AndroidSubCategorySpec(
            id = "Community Feed",
            title = "Porch Board & Quick Replies",
            summary = "Default: ${settings.defaultFeedCategory} • ${settings.boardPostLimit} posts",
            icon = Icons.Default.Forum,
            accentColor = Color(0xFF14B8A6),
          ),
        ),
      onSelect = onSelectSubCategory,
    )
    return
  }

  val subCat = selectedSubCategory
  val chrome = LocalTacticalChrome.current
  val tfColors = tacticalOutlinedTextFieldColors()
  if (subCat == "All" || subCat == "Crash & SOS") {
    TacticalCard(highlight = true, title = "Crash Detection & Emergency SOS") {
      Text("CRASH DETECTION, SECURITY & SOS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
      Spacer(Modifier.height(6.dp))
      AndroidSettingSwitch("Fingerprint / Face Unlock Lock Screen", checked = settings.biometricAppLock) { v -> sessionVm.updateSettings { it.copy(biometricAppLock = v) } }
      AndroidSettingSwitch("Keep Phone Screen Awake While Open", checked = settings.wakeLockEnabled) { v -> sessionVm.updateSettings { it.copy(wakeLockEnabled = v) } }
      val clampedCrashG = settings.crashSensitivityG.coerceIn(6.0f, 15.0f)
      Text("Crash Impact Sensitivity: ${"%.1f".format(clampedCrashG)}G", style = MaterialTheme.typography.labelSmall)
      TacticalSlider(value = clampedCrashG, onValueChange = { v -> sessionVm.updateSettings { it.copy(crashSensitivityG = v) } }, valueRange = 6.0f..15.0f)
      Text("Countdown Before Sending SOS Text: ${settings.sosCancelWindowSec} seconds", style = MaterialTheme.typography.labelSmall)
      TacticalSlider(value = settings.sosCancelWindowSec.toFloat(), onValueChange = { v -> sessionVm.updateSettings { it.copy(sosCancelWindowSec = v.toInt()) } }, valueRange = 5f..30f)
      OutlinedTextField(
        value = settings.sosTemplate,
        onValueChange = { txt -> sessionVm.updateSettings { it.copy(sosTemplate = txt) } },
        label = { Text("Emergency Text Message") },
        colors = tfColors,
        shape = chrome.smallShape,
        modifier = Modifier.fillMaxWidth(),
      )
    }
    Spacer(Modifier.height(12.dp))
  }

  if (subCat == "All" || subCat == "Community Feed") {
    TacticalCard(title = "Community & Quick Replies") {
      Text("COMMUNITY FEED & QUICK REPLIES", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
      Spacer(Modifier.height(6.dp))
      Text("Default Community Tab", style = MaterialTheme.typography.labelSmall)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(listOf("ALL", "UNIVERSAL", "GROUP_RIDES", "MARKETPLACE", "SOS")) { cat ->
          TacticalFilterChip(selected = settings.defaultFeedCategory == cat, onClick = { sessionVm.updateSettings { it.copy(defaultFeedCategory = cat) } }, label = cat.replace('_', ' '))
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(25, 50, 100).forEach { lim ->
          TacticalFilterChip(selected = settings.boardPostLimit == lim, onClick = { sessionVm.updateSettings { it.copy(boardPostLimit = lim) } }, label = "$lim Posts")
        }
      }
      Spacer(Modifier.height(6.dp))
      OutlinedTextField(
        value = settings.quickReplies,
        onValueChange = { txt -> sessionVm.updateSettings { it.copy(quickReplies = txt) } },
        label = { Text("Quick Message Replies (comma-separated)") },
        colors = tfColors,
        shape = chrome.smallShape,
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
fun AboutDeveloperSettingsContent() {
  val ctx = LocalContext.current
  val chrome = LocalTacticalChrome.current
  val clipboard = LocalClipboardManager.current
  var tapCount by remember { mutableIntStateOf(0) }
  var showEasterEgg by remember { mutableStateOf(false) }

  TacticalCard(highlight = true, title = "About Rural ERides GO") {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Icon(Icons.Default.VerifiedUser, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
      Column {
        Text("Rural ERides GO", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text(
          "Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.clickable {
            tapCount++
            if (tapCount >= 7) {
              tapCount = 0
              showEasterEgg = true
            }
          }.testTag("about_version_tap_target"),
        )
      }
    }
    Spacer(Modifier.height(12.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Icon(Icons.Default.Code, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(26.dp))
      Column {
        Text("APP DEVELOPER & CREATOR", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
        Text("Lord Bradley Callison", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text("Founder, Systems Architect & Lead Android Engineer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    Spacer(Modifier.height(12.dp))
    Surface(
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
      shape = chrome.cardShape,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("Package ID: ${BuildConfig.APPLICATION_ID}", style = MaterialTheme.typography.labelSmall)
        Text("Device: ${Build.MANUFACTURER.uppercase()} ${Build.MODEL}", style = MaterialTheme.typography.labelSmall)
        Text("System: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", style = MaterialTheme.typography.labelSmall)
        Text("Powered by: Kotlin • Jetpack Compose • Live GPS & GNSS Satellites • 9-DOF Sensors • Live AR Camera • Firebase AI & Cloud Sync", style = MaterialTheme.typography.labelSmall)
      }
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
      TacticalOutlinedButton(
        onClick = {
          clipboard.setText(
            AnnotatedString("Rural ERides GO v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}) — Developed by Lord Bradley Callison (${BuildConfig.APPLICATION_ID})")
          )
          Toast.makeText(ctx, "Copied developer info", Toast.LENGTH_SHORT).show()
        },
        modifier = Modifier.weight(1f).testTag("settings_copy_dev_info_button"),
      ) {
        Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Copy Info")
      }
      TacticalOutlinedButton(
        onClick = {
          val intent =
            Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(Intent.EXTRA_SUBJECT, "Rural ERides GO v${BuildConfig.VERSION_NAME}")
              putExtra(
                Intent.EXTRA_TEXT,
                "Rural ERides GO v${BuildConfig.VERSION_NAME} — Built by Lord Bradley Callison. Electric Ride Dashboard, Live GPS Satellites, AR Camera, Repair Shop & AI Helper.",
              )
            }
          runCatching { ctx.startActivity(Intent.createChooser(intent, "Share App Info")) }
        },
        modifier = Modifier.weight(1f).testTag("settings_share_dev_info_button"),
      ) {
        Icon(Icons.Default.Share, null, Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Share")
      }
    }
  }

  if (showEasterEgg) {
    TacticalDialog(
      onDismissRequest = { showEasterEgg = false },
      title = "SECRET EASTER EGG UNLOCKED!",
      subtitle = "Rider Matrix Core",
      icon = Icons.Default.AutoAwesome,
      badgeText = "MATRIX",
      confirmButton = {
        TacticalButton(onClick = { showEasterEgg = false }) {
          Text("Awesome!")
        }
      },
    ) {
      Text(
        "Welcome to Lord Bradley Callison's Secret Rider Matrix Mode!\n\nAll PEV telemetry limiters have been unlocked. You have discovered the ultimate backdoor into the Rural ERides GO core architecture.",
        style = MaterialTheme.typography.bodyMedium,
      )
    }
  }
}
