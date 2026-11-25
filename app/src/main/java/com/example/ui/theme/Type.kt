package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.FontPreset

val OrbitronFont = FontFamily(Font(R.font.orbitron, FontWeight.Normal))
val JetBrainsMonoFont = FontFamily(Font(R.font.jetbrains_mono, FontWeight.Normal))
val RajdhaniFont = FontFamily(Font(R.font.rajdhani, FontWeight.Normal))
val SpaceGroteskFont = FontFamily(Font(R.font.space_grotesk, FontWeight.Normal))
val PlusJakartaFont = FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.Normal))
val AudiowideFont = FontFamily(Font(R.font.audiowide, FontWeight.Normal))
val ShareTechMonoFont = FontFamily(Font(R.font.share_tech_mono, FontWeight.Normal))
val FiraCodeFont = FontFamily(Font(R.font.fira_code, FontWeight.Normal))
val ChakraPetchFont = FontFamily(Font(R.font.chakra_petch, FontWeight.Normal))
val OxaniumFont = FontFamily(Font(R.font.oxanium, FontWeight.Normal))
val BebasNeueFont = FontFamily(Font(R.font.bebas_neue, FontWeight.Normal))
val RussoOneFont = FontFamily(Font(R.font.russo_one, FontWeight.Normal))
val PlayfairDisplayFont = FontFamily(Font(R.font.playfair_display, FontWeight.Normal))
val InterFont = FontFamily(Font(R.font.inter, FontWeight.Normal))
val Vt323Font = FontFamily(Font(R.font.vt323, FontWeight.Normal))
val SilkscreenFont = FontFamily(Font(R.font.silkscreen, FontWeight.Normal))

fun resolveFontFamily(preset: FontPreset): FontFamily =
  when (preset) {
    FontPreset.ORBITRON -> OrbitronFont
    FontPreset.JETBRAINS_MONO -> JetBrainsMonoFont
    FontPreset.RAJDHANI -> RajdhaniFont
    FontPreset.SPACE_GROTESK -> SpaceGroteskFont
    FontPreset.PLUS_JAKARTA -> PlusJakartaFont
    FontPreset.AUDIOWIDE -> AudiowideFont
    FontPreset.SHARE_TECH -> ShareTechMonoFont
    FontPreset.FIRA_CODE -> FiraCodeFont
    FontPreset.CHAKRA_PETCH -> ChakraPetchFont
    FontPreset.OXANIUM -> OxaniumFont
    FontPreset.BEBAS_NEUE -> BebasNeueFont
    FontPreset.RUSSO_ONE -> RussoOneFont
    FontPreset.PLAYFAIR_DISPLAY -> PlayfairDisplayFont
    FontPreset.INTER -> InterFont
    FontPreset.VT323 -> Vt323Font
    FontPreset.SILKSCREEN -> SilkscreenFont
  }

fun resolveBodyCompanionFont(preset: FontPreset): FontFamily =
  when (preset) {
    FontPreset.BEBAS_NEUE,
    FontPreset.RUSSO_ONE,
    FontPreset.AUDIOWIDE,
    FontPreset.ORBITRON -> SpaceGroteskFont
    FontPreset.PLAYFAIR_DISPLAY -> PlusJakartaFont
    FontPreset.VT323,
    FontPreset.SILKSCREEN -> ShareTechMonoFont
    FontPreset.PLUS_JAKARTA -> PlusJakartaFont
    else -> resolveFontFamily(preset)
  }

fun resolveLabelFont(preset: FontPreset): FontFamily =
  when (preset) {
    FontPreset.PLAYFAIR_DISPLAY -> InterFont
    FontPreset.BEBAS_NEUE -> ChakraPetchFont
    FontPreset.RUSSO_ONE -> OxaniumFont
    FontPreset.PLUS_JAKARTA -> InterFont
    else -> resolveFontFamily(preset)
  }

fun buildTacticalTypography(preset: FontPreset, scaleMode: String): Typography {
  val displayFont = resolveFontFamily(preset)
  val bodyFont = resolveBodyCompanionFont(preset)
  val labelFont = resolveLabelFont(preset)
  val factor =
    when (scaleMode) {
      "compact" -> 0.90f
      "large" -> 1.14f
      else -> 1.0f
    }

  return Typography(
    displayLarge =
      TextStyle(
        fontFamily = displayFont,
        fontWeight = FontWeight.Black,
        fontSize = (52f * factor).sp,
        lineHeight = (56f * factor).sp,
        letterSpacing = (-0.5).sp,
      ),
    displaySmall =
      TextStyle(
        fontFamily = displayFont,
        fontWeight = FontWeight.Bold,
        fontSize = (34f * factor).sp,
        lineHeight = (40f * factor).sp,
      ),
    headlineMedium =
      TextStyle(
        fontFamily = displayFont,
        fontWeight = FontWeight.Bold,
        fontSize = (24f * factor).sp,
        lineHeight = (30f * factor).sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = displayFont,
        fontWeight = FontWeight.Bold,
        fontSize = (20f * factor).sp,
        lineHeight = (26f * factor).sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = displayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = (16f * factor).sp,
        lineHeight = (22f * factor).sp,
      ),
    titleSmall =
      TextStyle(
        fontFamily = displayFont,
        fontWeight = FontWeight.Bold,
        fontSize = (14f * factor).sp,
        lineHeight = (19f * factor).sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = bodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = (15f * factor).sp,
        lineHeight = (21f * factor).sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = bodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = (13.5f * factor).sp,
        lineHeight = (19f * factor).sp,
      ),
    bodySmall =
      TextStyle(
        fontFamily = bodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = (12f * factor).sp,
        lineHeight = (16.5f * factor).sp,
      ),
    labelLarge =
      TextStyle(
        fontFamily = labelFont,
        fontWeight = FontWeight.Bold,
        fontSize = (13f * factor).sp,
        lineHeight = (17f * factor).sp,
        letterSpacing = 0.6.sp,
      ),
    labelMedium =
      TextStyle(
        fontFamily = labelFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = (11f * factor).sp,
        lineHeight = (15f * factor).sp,
        letterSpacing = 0.7.sp,
      ),
    labelSmall =
      TextStyle(
        fontFamily = labelFont,
        fontWeight = FontWeight.Medium,
        fontSize = (10f * factor).sp,
        lineHeight = (13f * factor).sp,
        letterSpacing = 0.8.sp,
      ),
  )
}
