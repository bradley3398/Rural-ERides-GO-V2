package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.MasterSettings
import com.example.data.VisualStyleMode

enum class TacticalChromeFamily {
  CYBER_HUD,
  BRUTALIST,
  VAPORWAVE,
  AERO_METAL,
  RURAL_TIMBER,
  EDITORIAL,
  KINETIC_VIBRANT,
  OLED_PURE,
  WINDOWS_ERA,
}

@Immutable
data class TacticalChromeSpec(
  val family: TacticalChromeFamily,
  val visualStyle: VisualStyleMode,
  val cardShape: CornerBasedShape,
  val buttonShape: CornerBasedShape,
  val smallShape: CornerBasedShape,
  val dialogShape: CornerBasedShape,
  val cardBorderWidth: Dp,
  val buttonBorderWidth: Dp,
  val hasCornerBrackets: Boolean,
  val hasLeftAccentRail: Boolean,
  val hasBrutalistShadow: Boolean,
  val hasGradientBorder: Boolean,
  val hasSpecularRim: Boolean,
  val sliderTrackHeight: Dp,
  val uppercaseHeaders: Boolean,
)

fun resolveTacticalChromeFamily(settings: MasterSettings): TacticalChromeFamily {
  if (settings.nightVisionMode) return TacticalChromeFamily.CYBER_HUD
  if (settings.sunVisibilityMode) return TacticalChromeFamily.BRUTALIST
  return when (sanitizeVisualStyleForSeason(settings.visualStyle)) {
    VisualStyleMode.WINDOWS_CLASSIC,
    VisualStyleMode.WINDOWS_2000,
    VisualStyleMode.WINDOWS_XP,
    VisualStyleMode.WINDOWS_7,
    VisualStyleMode.WINDOWS_10,
    VisualStyleMode.WINDOWS_11 -> TacticalChromeFamily.WINDOWS_ERA

    VisualStyleMode.BRUTALIST_HIGH_VIS -> TacticalChromeFamily.BRUTALIST
    VisualStyleMode.VAPORWAVE_RETRO -> TacticalChromeFamily.VAPORWAVE
    VisualStyleMode.OLED_STEALTH -> TacticalChromeFamily.OLED_PURE
    VisualStyleMode.WARM_EDITORIAL -> TacticalChromeFamily.EDITORIAL

    VisualStyleMode.VIBRANT_CARDS,
    VisualStyleMode.PEV_SUNSET_CRUISE,
    VisualStyleMode.PEV_NIGHT_CONVOY,
    VisualStyleMode.HOLIDAY_NEW_YEARS,
    VisualStyleMode.HOLIDAY_VALENTINES,
    VisualStyleMode.HOLIDAY_EASTER,
    VisualStyleMode.HOLIDAY_FOURTH_JULY -> TacticalChromeFamily.KINETIC_VIBRANT

    VisualStyleMode.PEV_TITANIUM_PRO,
    VisualStyleMode.PEV_ARCTIC_ICE,
    VisualStyleMode.M3_DYNAMIC -> TacticalChromeFamily.AERO_METAL

    VisualStyleMode.RURAL_BARNWOOD,
    VisualStyleMode.RURAL_SUNSET_GRAVEL,
    VisualStyleMode.RURAL_PINE_HOLLER,
    VisualStyleMode.RURAL_PRAIRIE_GOLD,
    VisualStyleMode.RURAL_CREEK_MIST,
    VisualStyleMode.RURAL_HARVEST_IRON,
    VisualStyleMode.PEV_GRAVEL_DUST,
    VisualStyleMode.PEV_DESERT_ENDURO,
    VisualStyleMode.PEV_TRAIL_FOREST,
    VisualStyleMode.HOLIDAY_ST_PATRICKS,
    VisualStyleMode.HOLIDAY_MEMORIAL_DAY,
    VisualStyleMode.HOLIDAY_VETERANS_DAY,
    VisualStyleMode.HOLIDAY_THANKSGIVING,
    VisualStyleMode.HOLIDAY_CHRISTMAS -> TacticalChromeFamily.RURAL_TIMBER

    VisualStyleMode.DARK_STUDIO,
    VisualStyleMode.PEV_CYBER_CARBON,
    VisualStyleMode.PEV_VOLT_RACER,
    VisualStyleMode.PEV_MIDNIGHT_NEON,
    VisualStyleMode.PEV_TACTICAL_NEON,
    VisualStyleMode.PEV_APEX_TRACK,
    VisualStyleMode.PEV_LITHIUM_LAB,
    VisualStyleMode.HOLIDAY_LABOR_DAY,
    VisualStyleMode.HOLIDAY_HALLOWEEN -> TacticalChromeFamily.CYBER_HUD
  }
}

fun resolveTacticalChromeSpec(settings: MasterSettings): TacticalChromeSpec {
  val family = resolveTacticalChromeFamily(settings)
  val overrideSharp = settings.cornerStyle == "sharp"
  val overridePill = settings.cornerStyle == "pill"
  return when (family) {
    TacticalChromeFamily.WINDOWS_ERA -> {
      val classic =
        settings.visualStyle == VisualStyleMode.WINDOWS_CLASSIC ||
          settings.visualStyle == VisualStyleMode.WINDOWS_2000 ||
          settings.visualStyle == VisualStyleMode.WINDOWS_10
      val r =
        when (settings.visualStyle) {
          VisualStyleMode.WINDOWS_CLASSIC, VisualStyleMode.WINDOWS_2000, VisualStyleMode.WINDOWS_10 -> 0.dp
          VisualStyleMode.WINDOWS_XP -> 6.dp
          VisualStyleMode.WINDOWS_7 -> 9.dp
          VisualStyleMode.WINDOWS_11 -> 12.dp
          else -> 8.dp
        }
      TacticalChromeSpec(
        family = family,
        visualStyle = settings.visualStyle,
        cardShape = RoundedCornerShape(r),
        buttonShape = RoundedCornerShape(if (classic) 0.dp else 6.dp),
        smallShape = RoundedCornerShape(if (classic) 0.dp else 4.dp),
        dialogShape = RoundedCornerShape(r),
        cardBorderWidth = 1.5.dp,
        buttonBorderWidth = 1.dp,
        hasCornerBrackets = false,
        hasLeftAccentRail = false,
        hasBrutalistShadow = false,
        hasGradientBorder = false,
        hasSpecularRim = settings.visualStyle == VisualStyleMode.WINDOWS_7,
        sliderTrackHeight = 6.dp,
        uppercaseHeaders = false,
      )
    }

    TacticalChromeFamily.CYBER_HUD -> {
      val cut = if (overridePill) RoundedCornerShape(22.dp) else CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp, topEnd = 4.dp, bottomStart = 4.dp)
      val btn = if (overridePill) RoundedCornerShape(24.dp) else CutCornerShape(8.dp)
      val sm = if (overridePill) RoundedCornerShape(14.dp) else CutCornerShape(6.dp)
      TacticalChromeSpec(
        family = family,
        visualStyle = settings.visualStyle,
        cardShape = cut,
        buttonShape = btn,
        smallShape = sm,
        dialogShape = cut,
        cardBorderWidth = settings.borderWidthDp.coerceIn(0.8f, 2.5f).dp,
        buttonBorderWidth = 1.2.dp,
        hasCornerBrackets = true,
        hasLeftAccentRail = false,
        hasBrutalistShadow = false,
        hasGradientBorder = false,
        hasSpecularRim = false,
        sliderTrackHeight = 6.dp,
        uppercaseHeaders = true,
      )
    }

    TacticalChromeFamily.BRUTALIST ->
      TacticalChromeSpec(
        family = family,
        visualStyle = settings.visualStyle,
        cardShape = RoundedCornerShape(0.dp),
        buttonShape = RoundedCornerShape(0.dp),
        smallShape = RoundedCornerShape(0.dp),
        dialogShape = RoundedCornerShape(0.dp),
        cardBorderWidth = 2.5.dp,
        buttonBorderWidth = 2.dp,
        hasCornerBrackets = false,
        hasLeftAccentRail = false,
        hasBrutalistShadow = true,
        hasGradientBorder = false,
        hasSpecularRim = false,
        sliderTrackHeight = 10.dp,
        uppercaseHeaders = true,
      )

    TacticalChromeFamily.VAPORWAVE -> {
      val shape = if (overrideSharp) CutCornerShape(4.dp) else CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp, topEnd = 2.dp, bottomStart = 2.dp)
      TacticalChromeSpec(
        family = family,
        visualStyle = settings.visualStyle,
        cardShape = shape,
        buttonShape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp, topEnd = 2.dp, bottomStart = 2.dp),
        smallShape = CutCornerShape(6.dp),
        dialogShape = shape,
        cardBorderWidth = 1.5.dp,
        buttonBorderWidth = 1.2.dp,
        hasCornerBrackets = false,
        hasLeftAccentRail = false,
        hasBrutalistShadow = false,
        hasGradientBorder = true,
        hasSpecularRim = false,
        sliderTrackHeight = 7.dp,
        uppercaseHeaders = true,
      )
    }

    TacticalChromeFamily.AERO_METAL ->
      TacticalChromeSpec(
        family = family,
        visualStyle = settings.visualStyle,
        cardShape = RoundedCornerShape(if (overrideSharp) 6.dp else 16.dp),
        buttonShape = RoundedCornerShape(if (overrideSharp) 6.dp else 24.dp),
        smallShape = RoundedCornerShape(if (overrideSharp) 4.dp else 10.dp),
        dialogShape = RoundedCornerShape(if (overrideSharp) 8.dp else 20.dp),
        cardBorderWidth = 1.dp,
        buttonBorderWidth = 1.dp,
        hasCornerBrackets = false,
        hasLeftAccentRail = false,
        hasBrutalistShadow = false,
        hasGradientBorder = false,
        hasSpecularRim = true,
        sliderTrackHeight = 7.dp,
        uppercaseHeaders = false,
      )

    TacticalChromeFamily.RURAL_TIMBER ->
      TacticalChromeSpec(
        family = family,
        visualStyle = settings.visualStyle,
        cardShape = RoundedCornerShape(if (overridePill) 20.dp else 10.dp),
        buttonShape = RoundedCornerShape(if (overridePill) 24.dp else 10.dp),
        smallShape = RoundedCornerShape(8.dp),
        dialogShape = RoundedCornerShape(14.dp),
        cardBorderWidth = 1.4.dp,
        buttonBorderWidth = 1.4.dp,
        hasCornerBrackets = false,
        hasLeftAccentRail = true,
        hasBrutalistShadow = false,
        hasGradientBorder = false,
        hasSpecularRim = false,
        sliderTrackHeight = 8.dp,
        uppercaseHeaders = true,
      )

    TacticalChromeFamily.EDITORIAL ->
      TacticalChromeSpec(
        family = family,
        visualStyle = settings.visualStyle,
        cardShape = RoundedCornerShape(12.dp),
        buttonShape = RoundedCornerShape(22.dp),
        smallShape = RoundedCornerShape(8.dp),
        dialogShape = RoundedCornerShape(16.dp),
        cardBorderWidth = 1.dp,
        buttonBorderWidth = 1.dp,
        hasCornerBrackets = false,
        hasLeftAccentRail = false,
        hasBrutalistShadow = false,
        hasGradientBorder = false,
        hasSpecularRim = false,
        sliderTrackHeight = 4.dp,
        uppercaseHeaders = false,
      )

    TacticalChromeFamily.KINETIC_VIBRANT ->
      TacticalChromeSpec(
        family = family,
        visualStyle = settings.visualStyle,
        cardShape = RoundedCornerShape(22.dp),
        buttonShape = RoundedCornerShape(26.dp),
        smallShape = RoundedCornerShape(14.dp),
        dialogShape = RoundedCornerShape(24.dp),
        cardBorderWidth = 1.3.dp,
        buttonBorderWidth = 1.dp,
        hasCornerBrackets = false,
        hasLeftAccentRail = false,
        hasBrutalistShadow = false,
        hasGradientBorder = true,
        hasSpecularRim = true,
        sliderTrackHeight = 8.dp,
        uppercaseHeaders = false,
      )

    TacticalChromeFamily.OLED_PURE ->
      TacticalChromeSpec(
        family = family,
        visualStyle = settings.visualStyle,
        cardShape = CutCornerShape(6.dp),
        buttonShape = CutCornerShape(6.dp),
        smallShape = CutCornerShape(4.dp),
        dialogShape = CutCornerShape(8.dp),
        cardBorderWidth = 1.dp,
        buttonBorderWidth = 1.dp,
        hasCornerBrackets = false,
        hasLeftAccentRail = false,
        hasBrutalistShadow = false,
        hasGradientBorder = false,
        hasSpecularRim = false,
        sliderTrackHeight = 4.dp,
        uppercaseHeaders = true,
      )
  }
}

val LocalTacticalSettings = staticCompositionLocalOf { MasterSettings() }
val LocalTacticalChrome = staticCompositionLocalOf { resolveTacticalChromeSpec(MasterSettings()) }

@Composable
fun RuralERidesTheme(
  settings: MasterSettings = MasterSettings(),
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val systemDark = isSystemInDarkTheme()
  val colorScheme =
    buildTacticalColorScheme(
      settings = settings,
      systemDark = systemDark,
      context = context,
    )
  val typography =
    buildTacticalTypography(
      preset = settings.fontPreset,
      scaleMode = settings.uiScale,
    )
  val chromeSpec = remember(settings) { resolveTacticalChromeSpec(settings) }
  val shapes =
    Shapes(
      small = chromeSpec.smallShape,
      medium = chromeSpec.cardShape,
      large = chromeSpec.dialogShape,
    )

  CompositionLocalProvider(
    LocalTacticalSettings provides settings,
    LocalTacticalChrome provides chromeSpec,
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = typography,
      shapes = shapes,
      content = content,
    )
  }
}

@Composable
fun tacticalCardColors(): CardColors {
  val settings = LocalTacticalSettings.current
  val chrome = LocalTacticalChrome.current
  val isRetroSolid =
    (settings.visualStyle == VisualStyleMode.WINDOWS_CLASSIC ||
      settings.visualStyle == VisualStyleMode.WINDOWS_2000 ||
      settings.visualStyle == VisualStyleMode.WINDOWS_XP) &&
      !settings.nightVisionMode &&
      !settings.sunVisibilityMode
  val isWin7 = settings.visualStyle == VisualStyleMode.WINDOWS_7 && !settings.nightVisionMode && !settings.sunVisibilityMode
  val isWin10 = settings.visualStyle == VisualStyleMode.WINDOWS_10 && !settings.nightVisionMode && !settings.sunVisibilityMode
  val isWin11 = settings.visualStyle == VisualStyleMode.WINDOWS_11 && !settings.nightVisionMode && !settings.sunVisibilityMode
  val alpha =
    when {
      isRetroSolid -> 1f
      isWin10 -> 0.96f
      isWin11 -> settings.winGlassIntensity.coerceIn(0.72f, 0.96f)
      isWin7 -> settings.winGlassIntensity.coerceIn(0.76f, 0.96f)
      settings.sunVisibilityMode -> 0.98f
      chrome.family == TacticalChromeFamily.OLED_PURE -> 0.98f
      chrome.family == TacticalChromeFamily.BRUTALIST -> 0.98f
      else -> settings.cardOpacity.coerceIn(0.45f, 1f)
    }
  val container =
    if (chrome.family == TacticalChromeFamily.OLED_PURE) Color(0xFF000000).copy(alpha = alpha)
    else MaterialTheme.colorScheme.surface.copy(alpha = alpha)
  return CardDefaults.cardColors(
    containerColor = container,
    contentColor = MaterialTheme.colorScheme.onSurface,
  )
}

@Composable
fun tacticalBorder(highlight: Boolean = false): BorderStroke {
  val settings = LocalTacticalSettings.current
  val chrome = LocalTacticalChrome.current
  val isRetro = chrome.family == TacticalChromeFamily.WINDOWS_ERA
  if (isRetro) {
    val borderCol =
      when {
        highlight -> MaterialTheme.colorScheme.primary
        settings.visualStyle == VisualStyleMode.WINDOWS_XP -> TacticalColors.WinXPBorderBlue
        settings.visualStyle == VisualStyleMode.WINDOWS_7 -> TacticalColors.Win7AeroBorder
        settings.visualStyle == VisualStyleMode.WINDOWS_10 -> TacticalColors.Win10FluentBlue
        settings.visualStyle == VisualStyleMode.WINDOWS_11 -> MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
        else -> TacticalColors.Win95DarkShadow
      }
    return BorderStroke(width = if (settings.visualStyle == VisualStyleMode.WINDOWS_11) 1.dp else 1.5.dp, color = borderCol)
  }
  if (chrome.family == TacticalChromeFamily.BRUTALIST) {
    val col = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    return BorderStroke(2.5.dp, col)
  }
  val glowAlpha =
    if (settings.sunVisibilityMode) 0.9f
    else if (highlight) 0.88f
    else (0.24f + settings.borderGlowIntensity * 0.42f).coerceIn(0.18f, 0.92f)
  val color: Color =
    if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha)
    else MaterialTheme.colorScheme.outline.copy(alpha = glowAlpha)
  return BorderStroke(chrome.cardBorderWidth, color)
}

