package com.example.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.data.AppThemePreset
import com.example.data.ColorModeOverride
import com.example.data.MasterSettings
import com.example.data.VisualStyleMode

object TacticalColors {
  val Obsidian = Color(0xFF06090F)
  val SlateDark = Color(0xFF0E1420)
  val SlatePanel = Color(0xFF151E2E)
  val TextPrimary = Color(0xFFF8FAFC)
  val TextSecondary = Color(0xFFCBD5E1)
  val DangerRed = Color(0xFFFF4D6D)
  val WarningAmber = Color(0xFFFBBF24)
  val SafeGreen = Color(0xFF22C55E)

  // Tactical Neon Accents
  val NeonLime = Color(0xFF39FF14)
  val CyberCyan = Color(0xFF22D3EE)
  val TrailEmerald = Color(0xFF34D399)
  val SolarAmber = Color(0xFFFBBF24)
  val CrimsonPulse = Color(0xFFFB7185)
  val VoidPurple = Color(0xFFC084FC)
  val VoltYellow = Color(0xFFFACC15)
  val MotorOrange = Color(0xFFFF7A1A)
  val RegenTeal = Color(0xFF2DD4BF)
  val PlasmaMagenta = Color(0xFFF472B6)
  val StealthWhite = Color(0xFFFFFFFF)

  // Windows 95 Classic Chrome Palette
  val Win95DesktopTeal = Color(0xFF008080)
  val Win95Silver = Color(0xFFC0C0C0)
  val Win95LightEdge = Color(0xFFFFFFFF)
  val Win95DarkEdge = Color(0xFF808080)
  val Win95DeepShadow = Color(0xFF000000)
  val Win95TitleNavy = Color(0xFF000080)
  val Win95TitleGradientEnd = Color(0xFF1084D0)
  val Win95SurfaceWhite = Color(0xFFFFFFFF)
  val Win95BlackInk = Color(0xFF000000)

  // Windows 2000 Professional Palette
  val Win2KDesktopSlate = Color(0xFF3A6EA5)
  val Win2KPutty = Color(0xFFD4D0C8)
  val Win2KTitleNavy = Color(0xFF0A246A)
  val Win2KTitleSky = Color(0xFFA6CAF0)
  val Win2KDarkEdge = Color(0xFF808080)

  // Windows XP Luna Palette
  val WinXPBlissSky = Color(0xFF245EDC)
  val WinXPBlissHill = Color(0xFF53A318)
  val WinXPLunaBlueTop = Color(0xFF0058E6)
  val WinXPLunaBlueBottom = Color(0xFF3A93FF)
  val WinXPStartGreen = Color(0xFF3C9B2A)
  val WinXPWindowCream = Color(0xFFECE9D8)
  val WinXPLunaBorder = Color(0xFF0054E3)

  // Windows 7 Aero Glass Palette
  val Win7DesktopDeep = Color(0xFF0B355C)
  val Win7DesktopGlow = Color(0xFF2A8FD4)
  val Win7AeroGlass = Color(0xFFEAF3FC)
  val Win7AeroTitleTop = Color(0xFF3C84C6)
  val Win7AeroTitleBottom = Color(0xFF1D5288)
  val Win7AeroBorder = Color(0xFF5FA3DC)
  val Win7OrbBlue = Color(0xFF1A7AD4)

  // Retro Windows Chrome Aliases (Classic bevels, title gradients & status indicators)
  val Win95Highlight = Win95LightEdge
  val Win95Shadow = Win95DarkEdge
  val Win95DarkShadow = Win95DeepShadow
  val Win95SafeGreen = Color(0xFF008000)
  val Win95TooltipYellow = Color(0xFFFFFFE1)
  val Win2KTitleStart = Win2KTitleNavy
  val Win2KTitleEnd = Win2KTitleSky
  val WinXPLunaBlueEnd = WinXPLunaBlueBottom
  val WinXPCloseRed = Color(0xFFE0422A)
  val WinXPBorderBlue = WinXPLunaBorder

  // Windows 10 Fluent & Windows 11 Mica Palettes
  val Win10FluentBlue = Color(0xFF0078D7)
  val Win10TaskbarBlack = Color(0xFF101010)
  val Win10DesktopHero = Color(0xFF0A2A52)
  val Win10SurfaceLight = Color(0xFFF3F3F3)
  val Win10SurfaceDark = Color(0xFF1F1F1F)
  val Win11MicaAccent = Color(0xFF0067C0)
  val Win11MicaAccentLight = Color(0xFF60CDFF)
  val Win11BloomDeep = Color(0xFF08204A)
  val Win11MicaSurfaceLight = Color(0xF5F9FAFC)
  val Win11MicaSurfaceDark = Color(0xEE20242C)
}

fun presetAccentColor(preset: AppThemePreset): Color =
  when (preset) {
    AppThemePreset.LIME -> TacticalColors.NeonLime
    AppThemePreset.CYAN -> TacticalColors.CyberCyan
    AppThemePreset.EMERALD -> TacticalColors.TrailEmerald
    AppThemePreset.AMBER -> TacticalColors.SolarAmber
    AppThemePreset.ROSE -> TacticalColors.CrimsonPulse
    AppThemePreset.PURPLE -> TacticalColors.VoidPurple
    AppThemePreset.VOLT_YELLOW -> TacticalColors.VoltYellow
    AppThemePreset.MOTOR_ORANGE -> TacticalColors.MotorOrange
    AppThemePreset.REGEN_TEAL -> TacticalColors.RegenTeal
    AppThemePreset.PLASMA_MAGENTA -> TacticalColors.PlasmaMagenta
    AppThemePreset.VOID -> TacticalColors.StealthWhite
  }

/**
 * Helper for high-contrast dark color schemes so icons, FilterChips, NavigationBarItems,
 * and secondary containers NEVER render dark-on-dark.
 */
private fun buildHighContrastDarkScheme(
  primary: Color,
  secondary: Color,
  tertiary: Color,
  background: Color,
  surface: Color,
  surfaceVariant: Color,
  outline: Color,
  highContrastIcons: Boolean,
): ColorScheme {
  val iconInk = if (highContrastIcons) Color(0xFFF8FAFC) else Color(0xFFE2E8F0)
  val subtleInk = if (highContrastIcons) Color(0xFFD6E2F0) else Color(0xFFCBD5E1)
  return darkColorScheme(
    primary = primary,
    onPrimary = Color(0xFF04070B),
    primaryContainer = primary.copy(alpha = 0.24f),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = secondary,
    onSecondary = Color(0xFF04070B),
    secondaryContainer = secondary.copy(alpha = 0.24f),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = tertiary,
    onTertiary = Color(0xFF0B0A00),
    tertiaryContainer = tertiary.copy(alpha = 0.24f),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background = background,
    onBackground = iconInk,
    surface = surface,
    onSurface = iconInk,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = subtleInk,
    outline = outline,
    outlineVariant = outline.copy(alpha = 0.65f),
    error = TacticalColors.DangerRed,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF5A1120),
    onErrorContainer = Color(0xFFFFD9DF),
  )
}

fun buildTacticalColorScheme(
  settings: MasterSettings,
  systemDark: Boolean,
  context: Context?,
): ColorScheme {
  val accent = presetAccentColor(settings.themePreset)
  val hc = settings.highContrastIcons

  if (settings.nightVisionMode) {
    return buildHighContrastDarkScheme(
      primary = Color(0xFFFF4D4D),
      secondary = Color(0xFFFF7A66),
      tertiary = Color(0xFFFFA8A8),
      background = Color(0xFF070101),
      surface = Color(0xFF140404),
      surfaceVariant = Color(0xFF260909),
      outline = Color(0xFF8C2424),
      highContrastIcons = true,
    )
  }

  if (settings.sunVisibilityMode) {
    return lightColorScheme(
      primary = Color(0xFF0F172A),
      onPrimary = Color.White,
      primaryContainer = Color(0xFFDBEAFE),
      onPrimaryContainer = Color(0xFF0F172A),
      secondary = Color(0xFF0369A1),
      onSecondary = Color.White,
      secondaryContainer = Color(0xFFE0F2FE),
      onSecondaryContainer = Color(0xFF082F49),
      tertiary = Color(0xFF15803D),
      onTertiary = Color.White,
      background = Color(0xFFFFFFFF),
      onBackground = Color(0xFF000000),
      surface = Color(0xFFF8FAFC),
      onSurface = Color(0xFF000000),
      surfaceVariant = Color(0xFFE2E8F0),
      onSurfaceVariant = Color(0xFF1E293B),
      outline = Color(0xFF475569),
      error = Color(0xFFB91C1C),
      onError = Color.White,
    )
  }

  val useDark =
    when (settings.colorModeOverride) {
      ColorModeOverride.DARK -> true
      ColorModeOverride.LIGHT -> false
      ColorModeOverride.SYSTEM -> systemDark
    }

  // Dedicated PEV-Themed Palettes (always crisp high-contrast dark cockpit unless LIGHT override)
  when (settings.visualStyle) {
    VisualStyleMode.PEV_CYBER_CARBON -> {
      return buildHighContrastDarkScheme(
        primary = if (settings.themePreset == AppThemePreset.LIME) Color(0xFF4DFF2B) else accent,
        secondary = Color(0xFF38BDF8),
        tertiary = Color(0xFFFACC15),
        background = Color(0xFF070A0F),
        surface = Color(0xFF101622),
        surfaceVariant = Color(0xFF1B2434),
        outline = Color(0xFF3B4D6B),
        highContrastIcons = hc,
      )
    }

    VisualStyleMode.PEV_TRAIL_FOREST -> {
      return buildHighContrastDarkScheme(
        primary = Color(0xFF34D399),
        secondary = Color(0xFF6EE7B7),
        tertiary = Color(0xFFFBBF24),
        background = Color(0xFF05110C),
        surface = Color(0xFF0C1E16),
        surfaceVariant = Color(0xFF163024),
        outline = Color(0xFF2E5C47),
        highContrastIcons = hc,
      )
    }

    VisualStyleMode.PEV_VOLT_RACER -> {
      return buildHighContrastDarkScheme(
        primary = Color(0xFFFACC15),
        secondary = Color(0xFFFF5A36),
        tertiary = Color(0xFF38BDF8),
        background = Color(0xFF0B0A08),
        surface = Color(0xFF171510),
        surfaceVariant = Color(0xFF26221A),
        outline = Color(0xFF574C32),
        highContrastIcons = hc,
      )
    }

    VisualStyleMode.PEV_MIDNIGHT_NEON -> {
      return buildHighContrastDarkScheme(
        primary = Color(0xFF22D3EE),
        secondary = Color(0xFF4DFF2B),
        tertiary = Color(0xFFF472B6),
        background = Color(0xFF050814),
        surface = Color(0xFF0D1326),
        surfaceVariant = Color(0xFF182240),
        outline = Color(0xFF334C7D),
        highContrastIcons = hc,
      )
    }

    VisualStyleMode.PEV_DESERT_ENDURO -> {
      return buildHighContrastDarkScheme(
        primary = Color(0xFFFF8C24),
        secondary = Color(0xFFFBBF24),
        tertiary = Color(0xFF38BDF8),
        background = Color(0xFF120B08),
        surface = Color(0xFF201510),
        surfaceVariant = Color(0xFF302019),
        outline = Color(0xFF694635),
        highContrastIcons = hc,
      )
    }

    VisualStyleMode.PEV_ARCTIC_ICE -> {
      return buildHighContrastDarkScheme(
        primary = Color(0xFF7DD3FC),
        secondary = Color(0xFF2DD4BF),
        tertiary = Color(0xFFC4B5FD),
        background = Color(0xFF040D17),
        surface = Color(0xFF0B1A2B),
        surfaceVariant = Color(0xFF152C45),
        outline = Color(0xFF335B85),
        highContrastIcons = hc,
      )
    }

    VisualStyleMode.PEV_SUNSET_CRUISE -> {
      return buildHighContrastDarkScheme(
        primary = Color(0xFFFB923C),
        secondary = Color(0xFF2DD4BF),
        tertiary = Color(0xFFF472B6),
        background = Color(0xFF0B0C18),
        surface = Color(0xFF16182B),
        surfaceVariant = Color(0xFF242742),
        outline = Color(0xFF4C507A),
        highContrastIcons = hc,
      )
    }

    VisualStyleMode.PEV_TACTICAL_NEON -> {
      return buildHighContrastDarkScheme(
        primary = Color(0xFF39FF14),
        secondary = Color(0xFF00F5FF),
        tertiary = Color(0xFFFFB800),
        background = Color(0xFF04070D),
        surface = Color(0xFF0B111E),
        surfaceVariant = Color(0xFF131D30),
        outline = Color(0xFF1E4E68),
        highContrastIcons = true,
      )
    }

    VisualStyleMode.PEV_APEX_TRACK -> {
      return buildHighContrastDarkScheme(
        primary = Color(0xFFFF2A55),
        secondary = Color(0xFFF8FAFC),
        tertiary = Color(0xFFFACC15),
        background = Color(0xFF08080A),
        surface = Color(0xFF131318),
        surfaceVariant = Color(0xFF202029),
        outline = Color(0xFF5C2836),
        highContrastIcons = hc,
      )
    }

    VisualStyleMode.PEV_GRAVEL_DUST -> {
      return buildHighContrastDarkScheme(
        primary = Color(0xFFF97316),
        secondary = Color(0xFFEAB308),
        tertiary = Color(0xFF38BDF8),
        background = Color(0xFF100E0C),
        surface = Color(0xFF1C1814),
        surfaceVariant = Color(0xFF2B251F),
        outline = Color(0xFF635240),
        highContrastIcons = hc,
      )
    }

    VisualStyleMode.PEV_TITANIUM_PRO -> {
      return buildHighContrastDarkScheme(
        primary = Color(0xFF38BDF8),
        secondary = Color(0xFFCBD5E1),
        tertiary = Color(0xFF34D399),
        background = Color(0xFF0B0E13),
        surface = Color(0xFF151B24),
        surfaceVariant = Color(0xFF222B38),
        outline = Color(0xFF4B5C75),
        highContrastIcons = hc,
      )
    }

    VisualStyleMode.PEV_LITHIUM_LAB -> {
      return buildHighContrastDarkScheme(
        primary = Color(0xFF22C55E),
        secondary = Color(0xFF2DD4BF),
        tertiary = Color(0xFFA3E635),
        background = Color(0xFF040F0A),
        surface = Color(0xFF0B1E15),
        surfaceVariant = Color(0xFF143123),
        outline = Color(0xFF286146),
        highContrastIcons = hc,
      )
    }

    VisualStyleMode.PEV_NIGHT_CONVOY -> {
      return buildHighContrastDarkScheme(
        primary = Color(0xFFFBBF24),
        secondary = Color(0xFFF472B6),
        tertiary = Color(0xFF22D3EE),
        background = Color(0xFF070614),
        surface = Color(0xFF110F26),
        surfaceVariant = Color(0xFF1E1A3D),
        outline = Color(0xFF4C4280),
        highContrastIcons = hc,
      )
    }

    VisualStyleMode.WINDOWS_CLASSIC -> {
      return lightColorScheme(
        primary = TacticalColors.Win95TitleNavy,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD4D0C8),
        onPrimaryContainer = TacticalColors.Win95BlackInk,
        secondary = TacticalColors.Win95DesktopTeal,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFDCEEEF),
        onSecondaryContainer = TacticalColors.Win95BlackInk,
        tertiary = Color(0xFF006400),
        onTertiary = Color.White,
        background = TacticalColors.Win95DesktopTeal,
        onBackground = Color.White,
        surface = TacticalColors.Win95Silver,
        onSurface = TacticalColors.Win95BlackInk,
        surfaceVariant = TacticalColors.Win95SurfaceWhite,
        onSurfaceVariant = Color(0xFF1E293B),
        outline = TacticalColors.Win95DarkEdge,
        error = Color(0xFFB91C1C),
        onError = Color.White,
      )
    }

    VisualStyleMode.WINDOWS_2000 -> {
      return lightColorScheme(
        primary = TacticalColors.Win2KTitleNavy,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE4E0D8),
        onPrimaryContainer = TacticalColors.Win95BlackInk,
        secondary = TacticalColors.Win2KDesktopSlate,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD9E6F2),
        onSecondaryContainer = TacticalColors.Win95BlackInk,
        tertiary = Color(0xFF1E5C2E),
        onTertiary = Color.White,
        background = TacticalColors.Win2KDesktopSlate,
        onBackground = Color.White,
        surface = TacticalColors.Win2KPutty,
        onSurface = TacticalColors.Win95BlackInk,
        surfaceVariant = TacticalColors.Win95SurfaceWhite,
        onSurfaceVariant = Color(0xFF1E293B),
        outline = TacticalColors.Win2KDarkEdge,
        error = Color(0xFFB91C1C),
        onError = Color.White,
      )
    }

    VisualStyleMode.WINDOWS_XP -> {
      return lightColorScheme(
        primary = TacticalColors.WinXPLunaBlueTop,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD6E4FA),
        onPrimaryContainer = Color(0xFF002D7A),
        secondary = TacticalColors.WinXPStartGreen,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFDDF3D6),
        onSecondaryContainer = Color(0xFF14532D),
        tertiary = Color(0xFFE07A12),
        onTertiary = Color.White,
        background = TacticalColors.WinXPBlissSky,
        onBackground = Color.White,
        surface = TacticalColors.WinXPWindowCream,
        onSurface = Color(0xFF111827),
        surfaceVariant = Color(0xFFFFFFFF),
        onSurfaceVariant = Color(0xFF334155),
        outline = TacticalColors.WinXPLunaBorder,
        error = Color(0xFFC62828),
        onError = Color.White,
      )
    }

    VisualStyleMode.WINDOWS_7 -> {
      return lightColorScheme(
        primary = TacticalColors.Win7OrbBlue,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD4E8FA),
        onPrimaryContainer = Color(0xFF0B355C),
        secondary = TacticalColors.Win7DesktopGlow,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD6EEF9),
        onSecondaryContainer = Color(0xFF0C4A6E),
        tertiary = Color(0xFF0E7490),
        onTertiary = Color.White,
        background = TacticalColors.Win7DesktopDeep,
        onBackground = Color.White,
        surface = TacticalColors.Win7AeroGlass,
        onSurface = Color(0xFF0F172A),
        surfaceVariant = Color(0xFFF4F9FE),
        onSurfaceVariant = Color(0xFF334155),
        outline = TacticalColors.Win7AeroBorder,
        error = Color(0xFFC62828),
        onError = Color.White,
      )
    }

    VisualStyleMode.WINDOWS_10 -> {
      return if (useDark) {
        darkColorScheme(
          primary = Color(0xFF3AA0FF),
          onPrimary = Color.White,
          primaryContainer = Color(0xFF004E8C),
          onPrimaryContainer = Color.White,
          secondary = Color(0xFF00B7C3),
          onSecondary = Color.Black,
          secondaryContainer = Color(0xFF0A3A44),
          onSecondaryContainer = Color(0xFFD6F7FA),
          tertiary = Color(0xFFFFB900),
          onTertiary = Color.Black,
          background = TacticalColors.Win10DesktopHero,
          onBackground = Color.White,
          surface = TacticalColors.Win10SurfaceDark,
          onSurface = Color(0xFFF5F5F5),
          surfaceVariant = Color(0xFF2B2B2B),
          onSurfaceVariant = Color(0xFFCCCCCC),
          outline = Color(0xFF4C4C4C),
          error = Color(0xFFE81123),
          onError = Color.White,
        )
      } else {
        lightColorScheme(
          primary = TacticalColors.Win10FluentBlue,
          onPrimary = Color.White,
          primaryContainer = Color(0xFFDCECFB),
          onPrimaryContainer = Color(0xFF00386B),
          secondary = Color(0xFF008272),
          onSecondary = Color.White,
          secondaryContainer = Color(0xFFD7F2EE),
          onSecondaryContainer = Color(0xFF003B33),
          tertiary = Color(0xFFD83B01),
          onTertiary = Color.White,
          background = TacticalColors.Win10DesktopHero,
          onBackground = Color.White,
          surface = TacticalColors.Win10SurfaceLight,
          onSurface = Color(0xFF111111),
          surfaceVariant = Color(0xFFFFFFFF),
          onSurfaceVariant = Color(0xFF444444),
          outline = Color(0xFFCCCCCC),
          error = Color(0xFFE81123),
          onError = Color.White,
        )
      }
    }

    VisualStyleMode.WINDOWS_11 -> {
      return if (useDark) {
        darkColorScheme(
          primary = TacticalColors.Win11MicaAccentLight,
          onPrimary = Color(0xFF001B2E),
          primaryContainer = Color(0xFF123B61),
          onPrimaryContainer = Color(0xFFE0F4FF),
          secondary = Color(0xFF76B9ED),
          onSecondary = Color(0xFF001E36),
          secondaryContainer = Color(0xFF1B334D),
          onSecondaryContainer = Color(0xFFDCEEFF),
          tertiary = Color(0xFF6CCB5F),
          onTertiary = Color(0xFF082906),
          background = TacticalColors.Win11BloomDeep,
          onBackground = Color.White,
          surface = TacticalColors.Win11MicaSurfaceDark,
          onSurface = Color(0xFFF8FAFC),
          surfaceVariant = Color(0xFF2C313B),
          onSurfaceVariant = Color(0xFFCBD5E1),
          outline = Color(0xFF475060),
          error = Color(0xFFFF6B72),
          onError = Color.Black,
        )
      } else {
        lightColorScheme(
          primary = TacticalColors.Win11MicaAccent,
          onPrimary = Color.White,
          primaryContainer = Color(0xFFDDEEFF),
          onPrimaryContainer = Color(0xFF00315E),
          secondary = Color(0xFF0078D4),
          onSecondary = Color.White,
          secondaryContainer = Color(0xFFE5F1FB),
          onSecondaryContainer = Color(0xFF003B6B),
          tertiary = Color(0xFF0F7B0F),
          onTertiary = Color.White,
          background = Color(0xFFDCEBFB),
          onBackground = Color(0xFF0F172A),
          surface = TacticalColors.Win11MicaSurfaceLight,
          onSurface = Color(0xFF14181F),
          surfaceVariant = Color(0xFFFFFFFF),
          onSurfaceVariant = Color(0xFF4B5563),
          outline = Color(0xFFD0D7E2),
          error = Color(0xFFC42B1C),
          onError = Color.White,
        )
      }
    }

    VisualStyleMode.BRUTALIST_HIGH_VIS -> {
      return if (useDark) {
        buildHighContrastDarkScheme(
          primary = Color(0xFFFFF000),
          secondary = Color(0xFFFF3D00),
          tertiary = Color(0xFF00E5FF),
          background = Color(0xFF090909),
          surface = Color(0xFF141414),
          surfaceVariant = Color(0xFF212121),
          outline = Color(0xFFFFF000),
          highContrastIcons = true,
        )
      } else {
        lightColorScheme(
          primary = Color(0xFF000000),
          onPrimary = Color(0xFFFFF000),
          primaryContainer = Color(0xFFFFF000),
          onPrimaryContainer = Color(0xFF000000),
          secondary = Color(0xFFFF3D00),
          onSecondary = Color.White,
          secondaryContainer = Color(0xFFFFE5DC),
          onSecondaryContainer = Color(0xFF000000),
          tertiary = Color(0xFF0057FF),
          onTertiary = Color.White,
          background = Color(0xFFFAFAFA),
          onBackground = Color.Black,
          surface = Color(0xFFFFFFFF),
          onSurface = Color.Black,
          surfaceVariant = Color(0xFFE4E4E7),
          onSurfaceVariant = Color(0xFF18181B),
          outline = Color.Black,
          error = Color(0xFFD50000),
          onError = Color.White,
        )
      }
    }

    VisualStyleMode.VAPORWAVE_RETRO -> {
      return buildHighContrastDarkScheme(
        primary = Color(0xFFFF4FD8),
        secondary = Color(0xFF00F0FF),
        tertiary = Color(0xFFFFD84D),
        background = Color(0xFF0C0518),
        surface = Color(0xFF170B2B),
        surfaceVariant = Color(0xFF281447),
        outline = Color(0xFF8B47D6),
        highContrastIcons = hc,
      )
    }

    VisualStyleMode.OLED_STEALTH -> {
      return buildHighContrastDarkScheme(
        primary = accent,
        secondary = TacticalColors.CyberCyan,
        tertiary = TacticalColors.SolarAmber,
        background = Color(0xFF000000),
        surface = Color(0xFF080A0F),
        surfaceVariant = Color(0xFF141821),
        outline = Color(0xFF334155),
        highContrastIcons = hc,
      )
    }

    VisualStyleMode.M3_DYNAMIC -> {
      if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val dyn = if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        return if (useDark && hc) {
          dyn.copy(
            onSurface = Color(0xFFF8FAFC),
            onSurfaceVariant = Color(0xFFCBD5E1),
            onPrimaryContainer = Color(0xFFFFFFFF),
            onSecondaryContainer = Color(0xFFFFFFFF),
          )
        } else dyn
      }
    }

    VisualStyleMode.WARM_EDITORIAL -> {
      return if (useDark) {
        buildHighContrastDarkScheme(
          primary = Color(0xFFF5B856),
          secondary = Color(0xFFFB7185),
          tertiary = Color(0xFF34D399),
          background = Color(0xFF14110F),
          surface = Color(0xFF201B18),
          surfaceVariant = Color(0xFF2E2722),
          outline = Color(0xFF5C4E43),
          highContrastIcons = hc,
        )
      } else {
        lightColorScheme(
          primary = Color(0xFFB45309),
          onPrimary = Color.White,
          primaryContainer = Color(0xFFFEF3C7),
          onPrimaryContainer = Color(0xFF431407),
          secondary = Color(0xFF0F766E),
          onSecondary = Color.White,
          secondaryContainer = Color(0xFFCCFBF1),
          onSecondaryContainer = Color(0xFF134E4A),
          tertiary = Color(0xFF9333EA),
          background = Color(0xFFFAF6F0),
          onBackground = Color(0xFF1C1917),
          surface = Color(0xFFFFFDF9),
          onSurface = Color(0xFF1C1917),
          surfaceVariant = Color(0xFFF0E7DA),
          onSurfaceVariant = Color(0xFF44403C),
          outline = Color(0xFFA8A29E),
          error = Color(0xFFDC2626),
        )
      }
    }

    VisualStyleMode.VIBRANT_CARDS -> {
      return if (useDark) {
        buildHighContrastDarkScheme(
          primary = accent,
          secondary = Color(0xFF38BDF8),
          tertiary = Color(0xFFF472B6),
          background = Color(0xFF080716),
          surface = Color(0xFF13112B),
          surfaceVariant = Color(0xFF201D45),
          outline = Color(0xFF4A438A),
          highContrastIcons = hc,
        )
      } else {
        lightColorScheme(
          primary = Color(0xFF2563EB),
          onPrimary = Color.White,
          primaryContainer = Color(0xFFDBEAFE),
          onPrimaryContainer = Color(0xFF1E3A8A),
          secondary = Color(0xFF059669),
          onSecondary = Color.White,
          secondaryContainer = Color(0xFFD1FAE5),
          onSecondaryContainer = Color(0xFF065F46),
          tertiary = Color(0xFFDB2777),
          background = Color(0xFFEEF2FF),
          onBackground = Color(0xFF0F172A),
          surface = Color(0xFFFFFFFF),
          onSurface = Color(0xFF0F172A),
          surfaceVariant = Color(0xFFE0E7FF),
          onSurfaceVariant = Color(0xFF3730A3),
          outline = Color(0xFF818CF8),
          error = Color(0xFFE11D48),
        )
      }
    }

    VisualStyleMode.RURAL_BARNWOOD -> {
      return buildHighContrastDarkScheme(
        primary = Color(0xFFF59E0B),
        secondary = Color(0xFF9AC94B),
        tertiary = Color(0xFF38BDF8),
        background = Color(0xFF14100C),
        surface = Color(0xFF211A14),
        surfaceVariant = Color(0xFF31261D),
        outline = Color(0xFF705842),
        highContrastIcons = hc,
      )
    }

    VisualStyleMode.RURAL_SUNSET_GRAVEL -> {
      return buildHighContrastDarkScheme(
        primary = Color(0xFFFF9233),
        secondary = Color(0xFFFACC15),
        tertiary = Color(0xFF34D399),
        background = Color(0xFF150D0B),
        surface = Color(0xFF241713),
        surfaceVariant = Color(0xFF36221C),
        outline = Color(0xFF7A4B3A),
        highContrastIcons = hc,
      )
    }

    VisualStyleMode.RURAL_PINE_HOLLER -> {
      return buildHighContrastDarkScheme(
        primary = Color(0xFF8FA73F),
        secondary = Color(0xFF4ADE80),
        tertiary = Color(0xFFFBBF24),
        background = Color(0xFF07110A),
        surface = Color(0xFF102016),
        surfaceVariant = Color(0xFF1A3122),
        outline = Color(0xFF3E6648),
        highContrastIcons = hc,
      )
    }

    VisualStyleMode.RURAL_PRAIRIE_GOLD -> {
      return buildHighContrastDarkScheme(
        primary = Color(0xFFEAB308),
        secondary = Color(0xFF38BDF8),
        tertiary = Color(0xFF84CC16),
        background = Color(0xFF0F0E09),
        surface = Color(0xFF1C1A11),
        surfaceVariant = Color(0xFF2B271A),
        outline = Color(0xFF6B5F35),
        highContrastIcons = hc,
      )
    }

    VisualStyleMode.RURAL_CREEK_MIST -> {
      return buildHighContrastDarkScheme(
        primary = Color(0xFF2DD4BF),
        secondary = Color(0xFF7DD3FC),
        tertiary = Color(0xFFA3E635),
        background = Color(0xFF071014),
        surface = Color(0xFF101F26),
        surfaceVariant = Color(0xFF1A313B),
        outline = Color(0xFF3C677A),
        highContrastIcons = hc,
      )
    }

    VisualStyleMode.RURAL_HARVEST_IRON -> {
      return buildHighContrastDarkScheme(
        primary = Color(0xFF65C438),
        secondary = Color(0xFFFF7A1A),
        tertiary = Color(0xFFFACC15),
        background = Color(0xFF0C0E0C),
        surface = Color(0xFF161A16),
        surfaceVariant = Color(0xFF232A23),
        outline = Color(0xFF4A5C48),
        highContrastIcons = hc,
      )
    }

    VisualStyleMode.DARK_STUDIO -> Unit
  }

  if (!useDark) {
    val lightPrimary =
      when (settings.themePreset) {
        AppThemePreset.LIME -> Color(0xFF15803D)
        AppThemePreset.CYAN -> Color(0xFF0284C7)
        AppThemePreset.EMERALD -> Color(0xFF047857)
        AppThemePreset.AMBER -> Color(0xFFB45309)
        AppThemePreset.ROSE -> Color(0xFFBE123C)
        AppThemePreset.PURPLE -> Color(0xFF7E22CE)
        AppThemePreset.VOLT_YELLOW -> Color(0xFFA16207)
        AppThemePreset.MOTOR_ORANGE -> Color(0xFFC2410C)
        AppThemePreset.REGEN_TEAL -> Color(0xFF0F766E)
        AppThemePreset.PLASMA_MAGENTA -> Color(0xFFA21CAF)
        AppThemePreset.VOID -> Color(0xFF0F172A)
      }
    return lightColorScheme(
      primary = lightPrimary,
      onPrimary = Color.White,
      primaryContainer = lightPrimary.copy(alpha = 0.14f),
      onPrimaryContainer = lightPrimary,
      secondary = Color(0xFF0284C7),
      onSecondary = Color.White,
      secondaryContainer = Color(0xFFE0F2FE),
      onSecondaryContainer = Color(0xFF075985),
      tertiary = Color(0xFFB45309),
      background = Color(0xFFF4F6FA),
      onBackground = Color(0xFF0F172A),
      surface = Color(0xFFFFFFFF),
      onSurface = Color(0xFF0F172A),
      surfaceVariant = Color(0xFFE2E8F0),
      onSurfaceVariant = Color(0xFF334155),
      outline = Color(0xFF94A3B8),
      error = Color(0xFFDC2626),
    )
  }

  return buildHighContrastDarkScheme(
    primary = accent,
    secondary = TacticalColors.CyberCyan,
    tertiary = TacticalColors.SolarAmber,
    background = TacticalColors.Obsidian,
    surface = TacticalColors.SlateDark,
    surfaceVariant = TacticalColors.SlatePanel,
    outline = Color(0xFF3B4D6B),
    highContrastIcons = hc,
  )
}
