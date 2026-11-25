package com.example.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.ColorModeOverride
import com.example.data.MasterSettings
import com.example.data.VisualStyleMode

enum class TacticalChromeFamily {
    CYBER_HUD,
    BRUTALIST,
    RURAL_TIMBER,
    AERO_METAL,
    OLED_PURE,
    VAPORWAVE,
    KINETIC_VIBRANT,
    EDITORIAL,
}

data class TacticalChromeSpec(
    val family: TacticalChromeFamily = TacticalChromeFamily.CYBER_HUD,
    val cardShape: Shape = RoundedCornerShape(14.dp),
    val buttonShape: Shape = RoundedCornerShape(10.dp),
    val smallShape: Shape = RoundedCornerShape(8.dp),
    val dialogShape: Shape = RoundedCornerShape(18.dp),
    val cardBorderWidth: Dp = 1.2.dp,
    val buttonBorderWidth: Dp = 1.2.dp,
    val sliderTrackHeight: Dp = 6.dp,
    val hasCornerBrackets: Boolean = true,
    val hasLeftAccentRail: Boolean = false,
    val hasSpecularRim: Boolean = false,
    val hasBrutalistShadow: Boolean = false,
    val hasGradientBorder: Boolean = false,
    val uppercaseHeaders: Boolean = true,
)

val LocalTacticalSettings = staticCompositionLocalOf { MasterSettings() }
val LocalTacticalChrome = staticCompositionLocalOf { TacticalChromeSpec() }

fun resolveChromeSpec(settings: MasterSettings): TacticalChromeSpec {
    val r = settings.cardCornerRadiusDp.coerceIn(0, 28).dp
    val style = sanitizeVisualStyleForSeason(settings.visualStyle)
    return when (style) {
        VisualStyleMode.BRUTALIST_HIGH_VIS ->
            TacticalChromeSpec(
                family = TacticalChromeFamily.BRUTALIST,
                cardShape = RectangleShape,
                buttonShape = RectangleShape,
                smallShape = RectangleShape,
                dialogShape = RectangleShape,
                cardBorderWidth = 2.2.dp,
                buttonBorderWidth = 2.dp,
                sliderTrackHeight = 8.dp,
                hasCornerBrackets = false,
                hasBrutalistShadow = true,
                uppercaseHeaders = true,
            )
        VisualStyleMode.OLED_STEALTH ->
            TacticalChromeSpec(
                family = TacticalChromeFamily.OLED_PURE,
                cardShape = CutCornerShape(8.dp),
                buttonShape = CutCornerShape(6.dp),
                smallShape = CutCornerShape(4.dp),
                dialogShape = CutCornerShape(12.dp),
                cardBorderWidth = 1.dp,
                buttonBorderWidth = 1.dp,
                hasCornerBrackets = true,
                uppercaseHeaders = true,
            )
        VisualStyleMode.VAPORWAVE_RETRO,
        VisualStyleMode.PEV_MIDNIGHT_NEON ->
            TacticalChromeSpec(
                family = TacticalChromeFamily.VAPORWAVE,
                cardShape = CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp),
                buttonShape = CutCornerShape(8.dp),
                smallShape = CutCornerShape(6.dp),
                dialogShape = CutCornerShape(16.dp),
                cardBorderWidth = 1.4.dp,
                buttonBorderWidth = 1.3.dp,
                hasCornerBrackets = true,
                hasGradientBorder = true,
                uppercaseHeaders = true,
            )
        VisualStyleMode.RURAL_BARNWOOD,
        VisualStyleMode.RURAL_PINE_HOLLER,
        VisualStyleMode.RURAL_PRAIRIE_GOLD,
        VisualStyleMode.RURAL_HARVEST_IRON,
        VisualStyleMode.HOLIDAY_THANKSGIVING,
        VisualStyleMode.HOLIDAY_LABOR_DAY ->
            TacticalChromeSpec(
                family = TacticalChromeFamily.RURAL_TIMBER,
                cardShape = RoundedCornerShape(r),
                buttonShape = RoundedCornerShape(10.dp),
                smallShape = RoundedCornerShape(8.dp),
                dialogShape = RoundedCornerShape(16.dp),
                cardBorderWidth = 1.4.dp,
                buttonBorderWidth = 1.2.dp,
                hasCornerBrackets = false,
                hasLeftAccentRail = true,
                uppercaseHeaders = false,
            )
        VisualStyleMode.PEV_TITANIUM_PRO,
        VisualStyleMode.PEV_ARCTIC_ICE,
        VisualStyleMode.CUPERTINO_FROST ->
            TacticalChromeSpec(
                family = TacticalChromeFamily.AERO_METAL,
                cardShape = RoundedCornerShape(r),
                buttonShape = RoundedCornerShape(12.dp),
                smallShape = RoundedCornerShape(8.dp),
                dialogShape = RoundedCornerShape(20.dp),
                cardBorderWidth = 1.dp,
                buttonBorderWidth = 1.dp,
                hasCornerBrackets = false,
                hasSpecularRim = true,
                uppercaseHeaders = false,
            )
        VisualStyleMode.MATERIAL_EXPRESSIVE,
        VisualStyleMode.PEV_APEX_TRACK,
        VisualStyleMode.PEV_VOLT_RACER,
        VisualStyleMode.HOLIDAY_CHRISTMAS,
        VisualStyleMode.HOLIDAY_FOURTH_JULY,
        VisualStyleMode.HOLIDAY_NEW_YEARS,
        VisualStyleMode.HOLIDAY_HALLOWEEN,
        VisualStyleMode.HOLIDAY_VALENTINES,
        VisualStyleMode.HOLIDAY_ST_PATRICKS,
        VisualStyleMode.HOLIDAY_EASTER,
        VisualStyleMode.HOLIDAY_MEMORIAL_DAY,
        VisualStyleMode.HOLIDAY_VETERANS_DAY ->
            TacticalChromeSpec(
                family = TacticalChromeFamily.KINETIC_VIBRANT,
                cardShape = RoundedCornerShape(r),
                buttonShape = RoundedCornerShape(12.dp),
                smallShape = RoundedCornerShape(8.dp),
                dialogShape = RoundedCornerShape(18.dp),
                cardBorderWidth = 1.4.dp,
                buttonBorderWidth = 1.2.dp,
                hasCornerBrackets = true,
                hasGradientBorder = style.isHolidayTheme(),
                hasSpecularRim = true,
                uppercaseHeaders = true,
            )
        else ->
            TacticalChromeSpec(
                family = TacticalChromeFamily.CYBER_HUD,
                cardShape = RoundedCornerShape(r),
                buttonShape = RoundedCornerShape(10.dp),
                smallShape = RoundedCornerShape(8.dp),
                dialogShape = RoundedCornerShape(16.dp),
                cardBorderWidth = 1.2.dp,
                buttonBorderWidth = 1.2.dp,
                hasCornerBrackets = true,
                uppercaseHeaders = true,
            )
    }
}

@Composable
fun buildTacticalColorScheme(
    settings: MasterSettings,
    systemDark: Boolean,
    context: Context?,
): ColorScheme {
    if (settings.nightVisionMode) {
        return darkColorScheme(
            primary = Color(0xFFFF1744),
            onPrimary = Color.Black,
            secondary = Color(0xFFD50000),
            tertiary = Color(0xFFFF5252),
            background = Color(0xFF050000),
            surface = Color(0xFF120000),
            surfaceVariant = Color(0xFF1F0303),
            onBackground = Color(0xFFFF8A80),
            onSurface = Color(0xFFFF8A80),
            onSurfaceVariant = Color(0xFFFF5252),
            outline = Color(0xFF8E0000),
        )
    }
    if (settings.sunVisibilityMode) {
        return lightColorScheme(
            primary = Color(0xFF004D40),
            onPrimary = Color.White,
            secondary = Color(0xFFD84315),
            tertiary = Color(0xFF1565C0),
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFF4F6F8),
            surfaceVariant = Color(0xFFE1E6EB),
            onBackground = Color(0xFF000000),
            onSurface = Color(0xFF000000),
            onSurfaceVariant = Color(0xFF263238),
            outline = Color(0xFF000000),
        )
    }

    val useDark =
        when (settings.colorModeOverride) {
            ColorModeOverride.SYSTEM -> systemDark
            ColorModeOverride.DARK -> true
            ColorModeOverride.LIGHT -> false
        }

    val style = sanitizeVisualStyleForSeason(settings.visualStyle)
    if (style == VisualStyleMode.MATERIAL_EXPRESSIVE && settings.useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context != null) {
        return if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }

    val (primary, secondary, bgDark) =
        when (style) {
            VisualStyleMode.HOLIDAY_CHRISTMAS -> Triple(Color(0xFF00E676), Color(0xFFFF1744), Color(0xFF07170E))
            VisualStyleMode.HOLIDAY_HALLOWEEN -> Triple(Color(0xFFFF6D00), Color(0xFFD500F9), Color(0xFF100718))
            VisualStyleMode.HOLIDAY_THANKSGIVING -> Triple(Color(0xFFFF8F00), Color(0xFFD84315), Color(0xFF180E06))
            VisualStyleMode.HOLIDAY_FOURTH_JULY -> Triple(Color(0xFFFF1744), Color(0xFF2979FF), Color(0xFF060E1F))
            VisualStyleMode.HOLIDAY_MEMORIAL_DAY -> Triple(Color(0xFF42A5F5), Color(0xFFEF5350), Color(0xFF081221))
            VisualStyleMode.HOLIDAY_VETERANS_DAY -> Triple(Color(0xFFFFD54F), Color(0xFFE53935), Color(0xFF0B131E))
            VisualStyleMode.HOLIDAY_NEW_YEARS -> Triple(Color(0xFFFFD700), Color(0xFF00E5FF), Color(0xFF080914))
            VisualStyleMode.HOLIDAY_VALENTINES -> Triple(Color(0xFFFF4081), Color(0xFFFF80AB), Color(0xFF1A0812))
            VisualStyleMode.HOLIDAY_ST_PATRICKS -> Triple(Color(0xFF00E676), Color(0xFFFFD54F), Color(0xFF061A0E))
            VisualStyleMode.HOLIDAY_EASTER -> Triple(Color(0xFFF48FB1), Color(0xFF80CBC4), Color(0xFF12101B))
            VisualStyleMode.HOLIDAY_LABOR_DAY -> Triple(Color(0xFFFFB300), Color(0xFF42A5F5), Color(0xFF0F141C))
            VisualStyleMode.PEV_APEX_TRACK -> Triple(Color(0xFFFF3D00), Color(0xFFFFD600), Color(0xFF0E0B0E))
            VisualStyleMode.PEV_GRAVEL_DUST -> Triple(Color(0xFFFFB300), Color(0xFF8D6E63), Color(0xFF14110E))
            VisualStyleMode.PEV_TITANIUM_PRO -> Triple(Color(0xFF40C4FF), Color(0xFFB0BEC5), Color(0xFF0D131A))
            VisualStyleMode.PEV_LITHIUM_LAB -> Triple(Color(0xFF00E5FF), Color(0xFF76FF03), Color(0xFF071218))
            VisualStyleMode.PEV_MIDNIGHT_NEON,
            VisualStyleMode.VAPORWAVE_RETRO -> Triple(Color(0xFFFF007F), Color(0xFF00F0FF), Color(0xFF0D051A))
            VisualStyleMode.OLED_STEALTH -> Triple(Color(0xFF00FF66), Color(0xFF00E5FF), Color(0xFF000000))
            else -> Triple(Color(0xFF00E676), Color(0xFFFFB300), Color(0xFF090E14))
        }

    return if (useDark || style == VisualStyleMode.OLED_STEALTH) {
        val surfaceCol = if (style == VisualStyleMode.OLED_STEALTH) Color(0xFF05070A) else bgDark.copy(alpha = 1f)
        darkColorScheme(
            primary = primary,
            onPrimary = Color(0xFF041008),
            primaryContainer = primary.copy(alpha = 0.22f),
            onPrimaryContainer = primary,
            secondary = secondary,
            onSecondary = Color.Black,
            secondaryContainer = secondary.copy(alpha = 0.22f),
            onSecondaryContainer = secondary,
            tertiary = secondary,
            background = bgDark,
            surface = surfaceCol,
            surfaceVariant = Color(0xFF18222D),
            onBackground = Color(0xFFECEFF4),
            onSurface = Color(0xFFECEFF4),
            onSurfaceVariant = Color(0xFFB0BEC5),
            outline = primary.copy(alpha = 0.45f),
        )
    } else {
        lightColorScheme(
            primary = primary.copy(red = (primary.red * 0.7f), green = (primary.green * 0.7f), blue = (primary.blue * 0.7f)),
            onPrimary = Color.White,
            secondary = secondary,
            tertiary = secondary,
            background = Color(0xFFF5F7FA),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFE4E9F0),
            onBackground = Color(0xFF10161F),
            onSurface = Color(0xFF10161F),
            onSurfaceVariant = Color(0xFF455A64),
            outline = Color(0xFF90A4AE),
        )
    }
}

@Composable
fun tacticalCardColors(): CardColors {
    val settings = LocalTacticalSettings.current
    val cs = MaterialTheme.colorScheme
    val alpha = settings.glassAlpha.coerceIn(0.45f, 1.0f)
    return CardDefaults.cardColors(
        containerColor = cs.surface.copy(alpha = alpha),
        contentColor = cs.onSurface,
    )
}

@Composable
fun tacticalBorder(highlight: Boolean = false): BorderStroke {
    val settings = LocalTacticalSettings.current
    val chrome = LocalTacticalChrome.current
    val cs = MaterialTheme.colorScheme
    val width = if (highlight) (chrome.cardBorderWidth + 0.6.dp) else chrome.cardBorderWidth
    val effectiveStyle = sanitizeVisualStyleForSeason(settings.visualStyle)
    val holidaySpec = if (effectiveStyle.isHolidayTheme()) holidayDecorationSpec(effectiveStyle) else null
    val col =
        when {
            highlight -> cs.primary
            holidaySpec != null -> holidaySpec.ornamentColor.copy(alpha = 0.65f)
            else -> cs.outline.copy(alpha = 0.48f)
        }
    return BorderStroke(width, col)
}

@Composable
fun RuralERidesTheme(
    settings: MasterSettings = MasterSettings(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val sysDark = isSystemInDarkTheme()
    val colorScheme = buildTacticalColorScheme(settings, sysDark, context)
    val chromeSpec = resolveChromeSpec(settings)
    val typography = buildTacticalTypography(settings.fontPreset, settings.typographyScaleMode)

    CompositionLocalProvider(
        LocalTacticalSettings provides settings,
        LocalTacticalChrome provides chromeSpec,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}
