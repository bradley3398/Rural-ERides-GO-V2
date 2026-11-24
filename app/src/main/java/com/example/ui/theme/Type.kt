package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.data.FontPreset
import com.example.data.TypographyScaleMode

fun buildTacticalTypography(
    preset: FontPreset = FontPreset.TACTICAL_MONO,
    scaleMode: TypographyScaleMode = TypographyScaleMode.STANDARD,
): Typography {
    val family =
        when (preset) {
            FontPreset.TACTICAL_MONO, FontPreset.JETBRAINS_MONO -> FontFamily.Monospace
            FontPreset.CLASSIC_SERIF -> FontFamily.Serif
            else -> FontFamily.SansSerif
        }
    val s = scaleMode.multiplier
    return Typography(
        displayLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Black, fontSize = (38 * s).sp),
        displayMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Black, fontSize = (32 * s).sp),
        displaySmall = TextStyle(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = (26 * s).sp),
        headlineLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = (24 * s).sp),
        headlineMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = (20 * s).sp),
        headlineSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = (18 * s).sp),
        titleLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = (18 * s).sp),
        titleMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = (15 * s).sp),
        titleSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = (13.5f * s).sp),
        bodyLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = (15 * s).sp),
        bodyMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = (13.5f * s).sp),
        bodySmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = (12 * s).sp),
        labelLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = (13 * s).sp),
        labelMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = (11.5f * s).sp),
        labelSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = (10 * s).sp),
    )
}

fun buildTacticalTypography(preset: String, scaleMode: Float): Typography =
    buildTacticalTypography(FontPreset.TACTICAL_MONO, TypographyScaleMode.STANDARD)
