package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBike
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.ElectricMoped
import androidx.compose.material.icons.filled.ElectricScooter
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Skateboarding
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.EmergencyBroadcast
import com.example.data.HudStyle
import com.example.data.LiveTelemetrySnapshot
import com.example.data.MasterSettings
import com.example.data.PevLegalClass
import com.example.data.VisualStyleMode
import com.example.ui.theme.LocalTacticalChrome
import com.example.ui.theme.LocalTacticalSettings
import com.example.ui.theme.TacticalChromeFamily
import com.example.ui.theme.TacticalColors
import com.example.ui.theme.drawHolidayCardOrnament
import com.example.ui.theme.holidayDecorationSpec
import com.example.ui.theme.isHolidayTheme
import com.example.ui.theme.sanitizeVisualStyleForSeason
import com.example.ui.theme.tacticalBorder
import com.example.ui.theme.tacticalCardColors
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

fun formatSpeedValue(kmh: Double, unit: String): Double =
  if (unit == "km") kmh else kmh * 0.621371

fun speedUnitLabel(unit: String): String = if (unit == "km") "KM/H" else "MPH"

fun formatDistValue(km: Double, unit: String): Double =
  if (unit == "km") km else km * 0.621371

fun distUnitLabel(unit: String): String = if (unit == "km") "km" else "mi"

fun formatTempLabel(tempC: Double, tempUnit: String): String =
  if (tempUnit == "c") "${"%.1f".format(tempC)}°C" else "${"%.1f".format(tempC * 9.0 / 5.0 + 32.0)}°F"

fun decodeSampledBitmap(context: Context, uri: Uri, maxDim: Int = 1024): Bitmap? =
  runCatching {
    context.contentResolver.openInputStream(uri)?.use { stream ->
      val raw = BitmapFactory.decodeStream(stream) ?: return@use null
      val scale = minOf(maxDim.toFloat() / raw.width, maxDim.toFloat() / raw.height, 1f)
      if (scale < 1f) {
        Bitmap.createScaledBitmap(raw, (raw.width * scale).toInt(), (raw.height * scale).toInt(), true)
      } else raw
    }
  }.getOrNull()

fun isWindowsClassicActive(settings: MasterSettings): Boolean =
  (settings.visualStyle == VisualStyleMode.WINDOWS_CLASSIC ||
    settings.visualStyle == VisualStyleMode.WINDOWS_2000 ||
    settings.visualStyle == VisualStyleMode.WINDOWS_XP ||
    settings.visualStyle == VisualStyleMode.WINDOWS_7 ||
    settings.visualStyle == VisualStyleMode.WINDOWS_10 ||
    settings.visualStyle == VisualStyleMode.WINDOWS_11) &&
    !settings.nightVisionMode &&
    !settings.sunVisibilityMode

fun Modifier.windowsClassicBevel(sunken: Boolean = false): Modifier =
  this.drawBehind {
    val outerTopLeft = if (sunken) TacticalColors.Win95Shadow else TacticalColors.Win95Highlight
    val outerBottomRight = if (sunken) TacticalColors.Win95Highlight else TacticalColors.Win95DarkShadow
    val innerTopLeft = if (sunken) TacticalColors.Win95DarkShadow else TacticalColors.Win95LightEdge
    val innerBottomRight = if (sunken) TacticalColors.Win95LightEdge else TacticalColors.Win95Shadow
    val t1 = 1.6.dp.toPx()
    val t2 = 1.2.dp.toPx()
    // Outer top + left
    drawLine(outerTopLeft, Offset(0f, t1 / 2f), Offset(size.width, t1 / 2f), strokeWidth = t1)
    drawLine(outerTopLeft, Offset(t1 / 2f, 0f), Offset(t1 / 2f, size.height), strokeWidth = t1)
    // Outer bottom + right
    drawLine(outerBottomRight, Offset(0f, size.height - t1 / 2f), Offset(size.width, size.height - t1 / 2f), strokeWidth = t1)
    drawLine(outerBottomRight, Offset(size.width - t1 / 2f, 0f), Offset(size.width - t1 / 2f, size.height), strokeWidth = t1)
    // Inner top + left
    drawLine(innerTopLeft, Offset(t1, t1 + t2 / 2f), Offset(size.width - t1, t1 + t2 / 2f), strokeWidth = t2)
    drawLine(innerTopLeft, Offset(t1 + t2 / 2f, t1), Offset(t1 + t2 / 2f, size.height - t1), strokeWidth = t2)
    // Inner bottom + right
    drawLine(innerBottomRight, Offset(t1, size.height - t1 - t2 / 2f), Offset(size.width - t1, size.height - t1 - t2 / 2f), strokeWidth = t2)
    drawLine(innerBottomRight, Offset(size.width - t1 - t2 / 2f, t1), Offset(size.width - t1 - t2 / 2f, size.height - t1), strokeWidth = t2)
  }

@Composable
fun ClassicWindowTitleBar(
  title: String,
  icon: ImageVector? = null,
  trailingBadge: String? = null,
  active: Boolean = true,
) {
  val settings = LocalTacticalSettings.current
  if (isWindowsXpActive(settings)) {
    WindowsXpTitleBar(title = title, icon = icon, trailingBadge = trailingBadge, active = active)
    return
  }
  if (isWindows7Active(settings)) {
    Windows7AeroTitleBar(title = title, icon = icon, trailingBadge = trailingBadge, active = active)
    return
  }
  if (isWindows10Active(settings)) {
    Windows10FluentTitleBar(title = title, icon = icon, trailingBadge = trailingBadge)
    return
  }
  if (isWindows11Active(settings)) {
    Windows11MicaTitleBar(title = title, icon = icon, trailingBadge = trailingBadge)
    return
  }
  val style = settings.visualStyle
  val isXp = style == VisualStyleMode.WINDOWS_XP
  val isWin7 = style == VisualStyleMode.WINDOWS_7
  val is2k = style == VisualStyleMode.WINDOWS_2000
  val bgBrush =
    if (!active) {
      Brush.horizontalGradient(listOf(TacticalColors.Win95Shadow, TacticalColors.Win95Silver))
    } else when (style) {
      VisualStyleMode.WINDOWS_2000 ->
        Brush.horizontalGradient(listOf(TacticalColors.Win2KTitleStart, TacticalColors.Win2KTitleEnd))
      VisualStyleMode.WINDOWS_XP ->
        Brush.verticalGradient(listOf(TacticalColors.WinXPLunaBlueEnd, TacticalColors.WinXPLunaBlueTop, Color(0xFF003EC2)))
      VisualStyleMode.WINDOWS_7 ->
        Brush.verticalGradient(listOf(Color(0xCC9AD0FF), Color(0xCC3C84C6), Color(0xCC1E5792)))
      else ->
        Brush.horizontalGradient(listOf(TacticalColors.Win95TitleNavy, TacticalColors.Win95TitleGradientEnd))
    }
  val titleBarShape =
    when {
      isXp -> RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)
      isWin7 -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
      else -> RectangleShape
    }
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .background(bgBrush, titleBarShape)
        .padding(horizontal = 7.dp, vertical = if (isXp || isWin7) 5.dp else 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      if (icon != null) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(5.dp))
      }
      Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
      if (!trailingBadge.isNullOrBlank()) {
        Box(
          modifier =
            Modifier
              .background(if (is2k) TacticalColors.Win2KPutty else TacticalColors.Win95Silver)
              .windowsClassicBevel(sunken = false)
              .padding(horizontal = 5.dp, vertical = 1.dp),
        ) {
          Text(
            text = trailingBadge,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = TacticalColors.Win95BlackInk,
            maxLines = 1,
            softWrap = false,
          )
        }
      }
      listOf("_", "□", "×").forEach { glyph ->
        val isClose = glyph == "×"
        val btnBg =
          when {
            isXp && isClose -> TacticalColors.WinXPCloseRed
            isXp -> TacticalColors.WinXPLunaBlueTop
            isWin7 && isClose -> Color(0xFFD6453A)
            isWin7 -> Color(0x66FFFFFF)
            is2k -> TacticalColors.Win2KPutty
            else -> TacticalColors.Win95Silver
          }
        val btnFg = if (isXp || isWin7) Color.White else TacticalColors.Win95BlackInk
        val btnShape = if (isXp || isWin7) RoundedCornerShape(3.dp) else RectangleShape
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier
              .size(if (isWin7 && isClose) 20.dp else 16.dp, 16.dp)
              .background(btnBg, btnShape)
              .then(if (isXp || isWin7) Modifier.border(0.8.dp, Color.White.copy(alpha = 0.7f), btnShape) else Modifier.windowsClassicBevel(sunken = false)),
        ) {
          Text(
            text = glyph,
            fontSize = 10.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.Black,
            color = btnFg,
          )
        }
      }
    }
  }
}

@Composable
fun TacticalButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  colors: ButtonColors = ButtonDefaults.buttonColors(),
  contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
  isDestructive: Boolean = false,
  content: @Composable RowScope.() -> Unit,
) {
  var lastClickTime by remember { mutableStateOf(0L) }
  val debouncedOnClick = {
    val now = System.currentTimeMillis()
    if (now - lastClickTime >= 400L) {
      lastClickTime = now
      onClick()
    }
  }
  val settings = LocalTacticalSettings.current
  val chrome = LocalTacticalChrome.current
  if (isWindowsXpActive(settings)) {
    WindowsXpStyledButton(
      onClick = debouncedOnClick,
      modifier = modifier,
      enabled = enabled,
      isPrimary = true,
      isDestructive = isDestructive,
      contentPadding = contentPadding,
      content = content,
    )
    return
  }
  if (isWindows7Active(settings)) {
    Windows7AeroStyledButton(
      onClick = debouncedOnClick,
      modifier = modifier,
      enabled = enabled,
      isPrimary = true,
      isDestructive = isDestructive,
      contentPadding = contentPadding,
      content = content,
    )
    return
  }
  if (isWindows10Active(settings)) {
    Windows10FluentButton(
      onClick = debouncedOnClick,
      modifier = modifier,
      enabled = enabled,
      isPrimary = true,
      isDestructive = isDestructive,
      contentPadding = contentPadding,
      content = content,
    )
    return
  }
  if (isWindows11Active(settings)) {
    Windows11MicaButton(
      onClick = debouncedOnClick,
      modifier = modifier,
      enabled = enabled,
      isPrimary = true,
      isDestructive = isDestructive,
      contentPadding = contentPadding,
      content = content,
    )
    return
  }
  val isClassic = isWindowsClassicActive(settings)
  if (!isClassic) {
    val cs = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val basePrimary = if (isDestructive) cs.error else cs.primary
    val baseOnPrimary = if (isDestructive) cs.onError else cs.onPrimary
    val bgBrush =
      when {
        !enabled -> Brush.horizontalGradient(listOf(cs.surfaceVariant.copy(alpha = 0.55f), cs.surfaceVariant.copy(alpha = 0.45f)))
        chrome.family == TacticalChromeFamily.VAPORWAVE ->
          Brush.horizontalGradient(listOf(cs.primary, cs.secondary))
        chrome.family == TacticalChromeFamily.KINETIC_VIBRANT ->
          Brush.linearGradient(listOf(basePrimary, cs.secondary))
        chrome.family == TacticalChromeFamily.AERO_METAL ->
          Brush.verticalGradient(listOf(basePrimary.copy(alpha = 0.92f), basePrimary, basePrimary.copy(alpha = 0.85f)))
        chrome.family == TacticalChromeFamily.CYBER_HUD ->
          Brush.linearGradient(listOf(basePrimary, basePrimary.copy(alpha = 0.84f)))
        else -> Brush.horizontalGradient(listOf(basePrimary, basePrimary))
      }
    val borderColor =
      when {
        !enabled -> cs.outline.copy(alpha = 0.25f)
        chrome.family == TacticalChromeFamily.BRUTALIST -> cs.outline
        chrome.family == TacticalChromeFamily.CYBER_HUD -> basePrimary.copy(alpha = 0.95f)
        chrome.family == TacticalChromeFamily.VAPORWAVE -> cs.secondary.copy(alpha = 0.8f)
        chrome.family == TacticalChromeFamily.RURAL_TIMBER -> cs.tertiary.copy(alpha = 0.7f)
        else -> Color.White.copy(alpha = 0.22f)
      }
    val fgColor =
      when {
        !enabled -> cs.onSurface.copy(alpha = 0.42f)
        chrome.family == TacticalChromeFamily.OLED_PURE && !isDestructive -> basePrimary
        else -> baseOnPrimary
      }
    val shadowMod =
      if (chrome.hasBrutalistShadow && enabled) {
        val shadowCol = if (cs.primary.luminance() > 0.5f) cs.outline else cs.primary
        Modifier.drawBehind {
          val off = 3.dp.toPx()
          drawRect(color = shadowCol.copy(alpha = 0.9f), topLeft = Offset(off, off), size = size)
        }
      } else Modifier
    val effectiveFillModifier =
      if (chrome.family == TacticalChromeFamily.OLED_PURE && enabled) {
        Modifier
          .clip(chrome.buttonShape)
          .background(basePrimary.copy(alpha = if (isPressed) 0.26f else 0.16f), chrome.buttonShape)
          .border(1.3.dp, basePrimary, chrome.buttonShape)
      } else {
        Modifier
          .clip(chrome.buttonShape)
          .background(bgBrush, chrome.buttonShape)
          .border(chrome.buttonBorderWidth, borderColor, chrome.buttonShape)
      }
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        modifier
          .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
          .graphicsLayer {
            val s = if (isPressed && enabled) 0.97f else 1f
            scaleX = s
            scaleY = s
          }
          .then(shadowMod)
          .then(effectiveFillModifier)
          .clickable(enabled = enabled, onClick = debouncedOnClick)
          .padding(contentPadding),
    ) {
      CompositionLocalProvider(LocalContentColor provides fgColor) {
        ProvideTextStyle(
          MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            color = fgColor,
          )
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content,
          )
        }
      }
    }
    return
  }
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val bgColor =
    when {
      !enabled -> TacticalColors.Win95Silver.copy(alpha = 0.75f)
      isDestructive -> MaterialTheme.colorScheme.error
      else -> TacticalColors.Win95Silver
    }
  val fgColor =
    when {
      !enabled -> TacticalColors.Win95Shadow
      isDestructive -> Color.White
      else -> TacticalColors.Win95BlackInk
    }
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
        .background(bgColor, RectangleShape)
        .windowsClassicBevel(sunken = isPressed)
        .clickable(
          enabled = enabled,
          interactionSource = interactionSource,
          indication = null,
          onClick = debouncedOnClick,
        )
        .padding(contentPadding),
  ) {
    CompositionLocalProvider(LocalContentColor provides fgColor) {
      ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = fgColor)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
          content = content,
        )
      }
    }
  }
}

@Composable
fun TacticalOutlinedButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
  content: @Composable RowScope.() -> Unit,
) {
  var lastClickTime by remember { mutableStateOf(0L) }
  val debouncedOnClick = {
    val now = System.currentTimeMillis()
    if (now - lastClickTime >= 400L) {
      lastClickTime = now
      onClick()
    }
  }
  val settings = LocalTacticalSettings.current
  val chrome = LocalTacticalChrome.current
  if (isWindowsXpActive(settings)) {
    WindowsXpStyledButton(
      onClick = debouncedOnClick,
      modifier = modifier,
      enabled = enabled,
      isPrimary = false,
      contentPadding = contentPadding,
      content = content,
    )
    return
  }
  if (isWindows7Active(settings)) {
    Windows7AeroStyledButton(
      onClick = debouncedOnClick,
      modifier = modifier,
      enabled = enabled,
      isPrimary = false,
      contentPadding = contentPadding,
      content = content,
    )
    return
  }
  if (isWindows10Active(settings)) {
    Windows10FluentButton(
      onClick = debouncedOnClick,
      modifier = modifier,
      enabled = enabled,
      isPrimary = false,
      contentPadding = contentPadding,
      content = content,
    )
    return
  }
  if (isWindows11Active(settings)) {
    Windows11MicaButton(
      onClick = debouncedOnClick,
      modifier = modifier,
      enabled = enabled,
      isPrimary = false,
      contentPadding = contentPadding,
      content = content,
    )
    return
  }
  val isClassic = isWindowsClassicActive(settings)
  if (!isClassic) {
    val cs = MaterialTheme.colorScheme
    val fgColor = if (enabled) cs.primary else cs.onSurface.copy(alpha = 0.4f)
    val bgColor =
      when {
        chrome.family == TacticalChromeFamily.OLED_PURE -> Color(0xFF04060A)
        chrome.family == TacticalChromeFamily.BRUTALIST -> cs.surface
        else -> cs.surfaceVariant.copy(alpha = 0.36f)
      }
    val borderBrush =
      if (chrome.hasGradientBorder && enabled) {
        Brush.horizontalGradient(listOf(cs.primary, cs.secondary))
      } else {
        Brush.horizontalGradient(
          listOf(
            if (enabled) cs.primary.copy(alpha = 0.7f) else cs.outline.copy(alpha = 0.25f),
            if (enabled) cs.outline.copy(alpha = 0.85f) else cs.outline.copy(alpha = 0.25f),
          )
        )
      }
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        modifier
          .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
          .clip(chrome.buttonShape)
          .background(bgColor, chrome.buttonShape)
          .border(chrome.buttonBorderWidth, borderBrush, chrome.buttonShape)
          .clickable(enabled = enabled, onClick = debouncedOnClick)
          .padding(contentPadding),
    ) {
      CompositionLocalProvider(LocalContentColor provides fgColor) {
        ProvideTextStyle(
          MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.SemiBold,
            color = fgColor,
          )
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content,
          )
        }
      }
    }
    return
  }
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val fgColor = if (enabled) TacticalColors.Win95BlackInk else TacticalColors.Win95Shadow
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
        .background(TacticalColors.Win95Silver, RectangleShape)
        .windowsClassicBevel(sunken = isPressed)
        .clickable(
          enabled = enabled,
          interactionSource = interactionSource,
          indication = null,
          onClick = debouncedOnClick,
        )
        .padding(contentPadding),
  ) {
    CompositionLocalProvider(LocalContentColor provides fgColor) {
      ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, color = fgColor)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
          content = content,
        )
      }
    }
  }
}

@Composable
fun TacticalIconButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  content: @Composable () -> Unit,
) {
  var lastClickTime by remember { mutableStateOf(0L) }
  val debouncedOnClick = {
    val now = System.currentTimeMillis()
    if (now - lastClickTime >= 350L) {
      lastClickTime = now
      onClick()
    }
  }
  val settings = LocalTacticalSettings.current
  val chrome = LocalTacticalChrome.current
  if (isWindowsXpActive(settings)) {
    WindowsXpStyledButton(
      onClick = debouncedOnClick,
      modifier = modifier.size(48.dp),
      enabled = enabled,
      isPrimary = false,
      contentPadding = PaddingValues(0.dp),
    ) {
      content()
    }
    return
  }
  if (isWindows7Active(settings)) {
    Windows7AeroStyledButton(
      onClick = debouncedOnClick,
      modifier = modifier.size(48.dp),
      enabled = enabled,
      isPrimary = false,
      contentPadding = PaddingValues(0.dp),
    ) {
      content()
    }
    return
  }
  if (isWindows10Active(settings)) {
    Windows10FluentButton(
      onClick = debouncedOnClick,
      modifier = modifier.size(48.dp),
      enabled = enabled,
      isPrimary = false,
      contentPadding = PaddingValues(0.dp),
    ) {
      content()
    }
    return
  }
  if (isWindows11Active(settings)) {
    Windows11MicaButton(
      onClick = debouncedOnClick,
      modifier = modifier.size(48.dp),
      enabled = enabled,
      isPrimary = false,
      contentPadding = PaddingValues(0.dp),
    ) {
      content()
    }
    return
  }
  val isClassic = isWindowsClassicActive(settings)
  if (!isClassic) {
    val cs = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        modifier
          .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
          .graphicsLayer {
            scaleX = if (isPressed) 0.94f else 1f
            scaleY = if (isPressed) 0.94f else 1f
          }
          .clip(chrome.smallShape)
          .background(cs.surfaceVariant.copy(alpha = 0.42f), chrome.smallShape)
          .border(1.dp, cs.outline.copy(alpha = 0.45f), chrome.smallShape)
          .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = ripple(),
            onClick = debouncedOnClick,
          ),
    ) {
      CompositionLocalProvider(LocalContentColor provides if (enabled) cs.primary else cs.onSurface.copy(alpha = 0.4f)) {
        content()
      }
    }
    return
  }
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val fgColor = if (enabled) TacticalColors.Win95BlackInk else TacticalColors.Win95Shadow
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .size(48.dp)
        .background(TacticalColors.Win95Silver, RectangleShape)
        .windowsClassicBevel(sunken = isPressed)
        .clickable(
          enabled = enabled,
          interactionSource = interactionSource,
          indication = null,
          onClick = debouncedOnClick,
        ),
  ) {
    CompositionLocalProvider(LocalContentColor provides fgColor, content = content)
  }
}

@Composable
fun TacticalCard(
  modifier: Modifier = Modifier,
  highlight: Boolean = false,
  title: String? = null,
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val settings = LocalTacticalSettings.current
  val chrome = LocalTacticalChrome.current
  val mod = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
  if (isWindowsXpActive(settings)) {
    WindowsXpWindowCard(
      modifier = mod,
      highlight = highlight,
      title = title,
      showTitleBar = settings.showClassicTitleBars,
      content = content,
    )
    return
  }
  if (isWindows7Active(settings)) {
    Windows7AeroWindowCard(
      modifier = mod,
      highlight = highlight,
      title = title,
      showTitleBar = settings.showClassicTitleBars,
      content = content,
    )
    return
  }
  if (isWindows10Active(settings)) {
    Windows10FluentWindowCard(
      modifier = mod,
      highlight = highlight,
      title = title,
      showTitleBar = settings.showClassicTitleBars,
      content = content,
    )
    return
  }
  if (isWindows11Active(settings)) {
    Windows11MicaWindowCard(
      modifier = mod,
      highlight = highlight,
      title = title,
      showTitleBar = settings.showClassicTitleBars,
      content = content,
    )
    return
  }
  val isClassic = isWindowsClassicActive(settings)
  if (isClassic) {
    val is2k = settings.visualStyle == VisualStyleMode.WINDOWS_2000
    Column(
      modifier =
        mod
          .background(MaterialTheme.colorScheme.surface)
          .windowsClassicBevel(sunken = false)
          .padding(3.dp),
    ) {
      if (!title.isNullOrBlank() && settings.showClassicTitleBars) {
        ClassicWindowTitleBar(
          title = title,
          trailingBadge = if (highlight) "LIVE" else null,
          active = true,
        )
        if (settings.winShowMenuBar) {
          ClassicWindowsMenuStrip(isXp = false, is2k = is2k)
        }
      }
      Column(modifier = Modifier.padding(12.dp), content = content)
    }
    return
  }

  val cs = MaterialTheme.colorScheme
  val colors = tacticalCardColors()
  val cardPadding = if (settings.spaciousLayoutMode) 18.dp else 14.dp
  val shadowMod =
    when {
      chrome.hasBrutalistShadow -> {
        val shCol = if (highlight) cs.primary else cs.outline
        Modifier.padding(end = 4.dp, bottom = 4.dp).drawBehind {
          val off = 4.dp.toPx()
          drawRect(color = shCol.copy(alpha = 0.9f), topLeft = Offset(off, off), size = size)
        }
      }
      chrome.family == TacticalChromeFamily.EDITORIAL || chrome.family == TacticalChromeFamily.KINETIC_VIBRANT ->
        Modifier.shadow(elevation = if (highlight) 8.dp else 3.dp, shape = chrome.cardShape)
      else -> Modifier
    }

  val surfaceBrush =
    when (chrome.family) {
      TacticalChromeFamily.AERO_METAL ->
        Brush.verticalGradient(
          listOf(
            colors.containerColor.copy(alpha = (colors.containerColor.alpha + 0.06f).coerceAtMost(1f)),
            colors.containerColor,
            cs.surfaceVariant.copy(alpha = (colors.containerColor.alpha * 0.72f).coerceIn(0.35f, 0.95f)),
          )
        )
      TacticalChromeFamily.KINETIC_VIBRANT ->
        Brush.linearGradient(
          listOf(
            colors.containerColor,
            cs.primaryContainer.copy(alpha = if (highlight) 0.42f else 0.22f),
          )
        )
      TacticalChromeFamily.VAPORWAVE ->
        Brush.verticalGradient(
          listOf(
            colors.containerColor,
            cs.primaryContainer.copy(alpha = 0.20f),
          )
        )
      else -> Brush.verticalGradient(listOf(colors.containerColor, colors.containerColor))
    }

  val borderStroke =
    if (chrome.hasGradientBorder && highlight) {
      BorderStroke(chrome.cardBorderWidth, Brush.horizontalGradient(listOf(cs.primary, cs.secondary, cs.tertiary)))
    } else if (chrome.hasGradientBorder) {
      BorderStroke(chrome.cardBorderWidth, Brush.horizontalGradient(listOf(cs.primary.copy(alpha = 0.65f), cs.secondary.copy(alpha = 0.55f))))
    } else {
      tacticalBorder(highlight)
    }

  val effectiveStyle = sanitizeVisualStyleForSeason(settings.visualStyle)
  val holidaySpec =
    if (!settings.nightVisionMode && !settings.sunVisibilityMode && effectiveStyle.isHolidayTheme()) {
      holidayDecorationSpec(effectiveStyle)
    } else null

  val chromeDecorationMod =
    Modifier.drawBehind {
      val w = size.width
      val h = size.height
      if (chrome.hasCornerBrackets) {
        val len = 10.dp.toPx()
        val stroke = if (highlight) 2.2.dp.toPx() else 1.5.dp.toPx()
        val col = if (highlight) cs.primary else cs.primary.copy(alpha = 0.65f)
        // Top-right reticle
        drawLine(col, Offset(w - len, 2f), Offset(w - 2f, 2f), strokeWidth = stroke)
        drawLine(col, Offset(w - 2f, 2f), Offset(w - 2f, len), strokeWidth = stroke)
        // Bottom-left reticle
        drawLine(col, Offset(2f, h - len), Offset(2f, h - 2f), strokeWidth = stroke)
        drawLine(col, Offset(2f, h - 2f), Offset(len, h - 2f), strokeWidth = stroke)
      }
      if (chrome.hasLeftAccentRail) {
        val railW = 4.5.dp.toPx()
        drawRect(
          brush = Brush.verticalGradient(listOf(cs.primary, cs.tertiary)),
          topLeft = Offset(0f, 0f),
          size = Size(railW, h),
        )
        // Brass/iron corner rivets
        val rivetCol = cs.tertiary.copy(alpha = 0.7f)
        drawCircle(rivetCol, radius = 2.dp.toPx(), center = Offset(w - 8.dp.toPx(), 8.dp.toPx()))
        drawCircle(rivetCol, radius = 2.dp.toPx(), center = Offset(w - 8.dp.toPx(), h - 8.dp.toPx()))
      }
      if (chrome.hasSpecularRim) {
        drawLine(
          color = Color.White.copy(alpha = if (highlight) 0.42f else 0.24f),
          start = Offset(12.dp.toPx(), 1.dp.toPx()),
          end = Offset(w - 12.dp.toPx(), 1.dp.toPx()),
          strokeWidth = 1.2.dp.toPx(),
        )
      }
    }

  val holidayOverlayMod =
    if (holidaySpec != null) {
      Modifier.drawWithContent {
        drawContent()
        drawHolidayCardOrnament(mode = effectiveStyle, highlight = highlight)
      }
    } else Modifier

  Box(
    modifier =
      mod
        .then(shadowMod)
        .clip(chrome.cardShape)
        .background(surfaceBrush, chrome.cardShape)
        .border(borderStroke, chrome.cardShape)
        .then(chromeDecorationMod)
        .then(holidayOverlayMod),
  ) {
    CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
      Column(modifier = Modifier.fillMaxWidth()) {
        if (!title.isNullOrBlank()) {
          val headerText = if (chrome.uppercaseHeaders) title.uppercase() else title
          val headerBg =
            when (chrome.family) {
              TacticalChromeFamily.BRUTALIST -> cs.primary.copy(alpha = 0.18f)
              TacticalChromeFamily.CYBER_HUD -> cs.primary.copy(alpha = if (highlight) 0.16f else 0.08f)
              TacticalChromeFamily.VAPORWAVE -> cs.secondary.copy(alpha = 0.12f)
              TacticalChromeFamily.RURAL_TIMBER -> cs.primary.copy(alpha = 0.12f)
              else -> Color.Transparent
            }
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .background(headerBg)
                .padding(
                  start = if (chrome.hasLeftAccentRail) cardPadding + 4.dp else cardPadding,
                  end = cardPadding,
                  top = 10.dp,
                  bottom = 6.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = headerText,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.ExtraBold,
              color = if (highlight) cs.primary else cs.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
              if (holidaySpec != null) {
                Box(
                  modifier =
                    Modifier
                      .padding(start = 6.dp)
                      .background(holidaySpec.ornamentColor.copy(alpha = 0.22f), chrome.smallShape)
                      .border(1.dp, holidaySpec.ornamentColor.copy(alpha = 0.72f), chrome.smallShape)
                      .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                  Text(
                    text = holidaySpec.trimLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = holidaySpec.ornamentColor,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
              }
              if (highlight) {
                Box(
                  modifier =
                    Modifier
                      .padding(start = if (holidaySpec == null) 8.dp else 0.dp)
                      .background(cs.primary.copy(alpha = 0.2f), chrome.smallShape)
                      .border(0.8.dp, cs.primary.copy(alpha = 0.7f), chrome.smallShape)
                      .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                  Text(
                    text = "LIVE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = cs.primary,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
              }
            }
          }
          HorizontalDivider(
            thickness = if (chrome.family == TacticalChromeFamily.BRUTALIST) 1.5.dp else 0.8.dp,
            color = cs.outline.copy(alpha = 0.35f),
          )
        }
        Column(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(
                start = if (chrome.hasLeftAccentRail) cardPadding + 4.dp else cardPadding,
                end = cardPadding,
                top = if (!title.isNullOrBlank()) 10.dp else cardPadding,
                bottom = cardPadding,
              ),
          content = content,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TacticalSlider(
  value: Float,
  onValueChange: (Float) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
  steps: Int = 0,
  onValueChangeFinished: (() -> Unit)? = null,
) {
  val settings = LocalTacticalSettings.current
  val chrome = LocalTacticalChrome.current
  val cs = MaterialTheme.colorScheme
  val isClassicWin =
    (settings.visualStyle == VisualStyleMode.WINDOWS_CLASSIC || settings.visualStyle == VisualStyleMode.WINDOWS_2000) &&
      !settings.nightVisionMode &&
      !settings.sunVisibilityMode
  val isWinXp = isWindowsXpActive(settings)
  val isWin7 = isWindows7Active(settings)
  val isWin10 = isWindows10Active(settings)
  val isWin11 = isWindows11Active(settings)

  Slider(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    enabled = enabled,
    valueRange = valueRange,
    steps = steps,
    onValueChangeFinished = onValueChangeFinished,
    thumb = {
      when {
        isClassicWin -> {
          Box(
            modifier =
              Modifier
                .size(width = 14.dp, height = 24.dp)
                .background(TacticalColors.Win95Silver, RectangleShape)
                .windowsClassicBevel(sunken = false),
          )
        }
        isWinXp -> {
          Box(
            modifier =
              Modifier
                .size(width = 14.dp, height = 24.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFECE9D8), Color(0xFFD6D2C2))))
                .border(1.2.dp, Color(0xFF003C74), RoundedCornerShape(3.dp))
                .drawBehind {
                  drawRect(Color(0xFF3E9D31), topLeft = Offset(0f, size.height - 4.dp.toPx()), size = Size(size.width, 4.dp.toPx()))
                },
          )
        }
        isWin7 -> {
          Box(
            modifier =
              Modifier
                .size(width = 15.dp, height = 24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFD7ECFD), Color(0xFF9ACDF7))))
                .border(1.2.dp, Color(0xFF3C7FB1), RoundedCornerShape(4.dp)),
          )
        }
        isWin10 -> {
          Box(
            modifier =
              Modifier
                .size(width = 10.dp, height = 24.dp)
                .background(cs.primary, RectangleShape)
                .border(1.dp, Color.White.copy(alpha = 0.7f), RectangleShape),
          )
        }
        isWin11 -> {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier
                .size(22.dp)
                .shadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.5.dp, cs.outline.copy(alpha = 0.5f), CircleShape),
          ) {
            Box(Modifier.size(11.dp).clip(CircleShape).background(cs.primary))
          }
        }
        chrome.family == TacticalChromeFamily.BRUTALIST -> {
          Box(
            modifier =
              Modifier
                .size(width = 16.dp, height = 26.dp)
                .background(cs.primary, RectangleShape)
                .border(2.dp, cs.outline, RectangleShape),
          )
        }
        chrome.family == TacticalChromeFamily.CYBER_HUD || chrome.family == TacticalChromeFamily.VAPORWAVE -> {
          val thumbShape = CutCornerShape(5.dp)
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier
                .size(22.dp)
                .clip(thumbShape)
                .background(Brush.linearGradient(listOf(cs.primary, cs.secondary)))
                .border(1.2.dp, Color.White.copy(alpha = 0.85f), thumbShape),
          ) {
            Box(Modifier.size(6.dp).background(cs.onPrimary, CircleShape))
          }
        }
        chrome.family == TacticalChromeFamily.RURAL_TIMBER -> {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier
                .size(width = 18.dp, height = 24.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Brush.verticalGradient(listOf(cs.tertiary, cs.primary)))
                .border(1.5.dp, cs.outline, RoundedCornerShape(5.dp)),
          ) {
            Box(Modifier.size(width = 2.dp, height = 10.dp).background(cs.onPrimary.copy(alpha = 0.85f)))
          }
        }
        chrome.family == TacticalChromeFamily.AERO_METAL -> {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier
                .size(22.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color.White, cs.primary.copy(alpha = 0.85f), cs.primary)))
                .border(1.2.dp, Color.White.copy(alpha = 0.9f), CircleShape),
          ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.85f)))
          }
        }
        chrome.family == TacticalChromeFamily.OLED_PURE -> {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier
                .size(20.dp)
                .clip(CutCornerShape(4.dp))
                .background(Color.Black)
                .border(1.8.dp, cs.primary, CutCornerShape(4.dp)),
          ) {
            Box(Modifier.size(6.dp).background(cs.primary, CircleShape))
          }
        }
        else -> {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier
                .size(22.dp)
                .shadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(cs.primary, cs.secondary)))
                .border(2.dp, cs.surface, CircleShape),
          ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(cs.onPrimary))
          }
        }
      }
    },
    track = { sliderState ->
      val span = (sliderState.valueRange.endInclusive - sliderState.valueRange.start).coerceAtLeast(0.0001f)
      val fraction = ((sliderState.value - sliderState.valueRange.start) / span).coerceIn(0f, 1f)
      val trackShape: Shape =
        when {
          isClassicWin || isWin10 || chrome.family == TacticalChromeFamily.BRUTALIST -> RectangleShape
          chrome.family == TacticalChromeFamily.CYBER_HUD || chrome.family == TacticalChromeFamily.OLED_PURE -> CutCornerShape(3.dp)
          else -> RoundedCornerShape(99.dp)
        }
      val inactiveColor =
        when {
          isClassicWin -> TacticalColors.Win95Silver
          isWinXp -> Color(0xFFDAD5C5)
          isWin7 -> Color(0xFFBBD2E6)
          chrome.family == TacticalChromeFamily.OLED_PURE -> Color(0xFF141820)
          chrome.family == TacticalChromeFamily.BRUTALIST -> cs.surfaceVariant
          else -> cs.surfaceVariant.copy(alpha = 0.72f)
        }
      val activeBrush =
        when {
          isClassicWin -> Brush.horizontalGradient(listOf(TacticalColors.Win95TitleNavy, TacticalColors.Win95TitleNavy))
          chrome.family == TacticalChromeFamily.VAPORWAVE -> Brush.horizontalGradient(listOf(cs.primary, cs.secondary, cs.tertiary))
          chrome.family == TacticalChromeFamily.CYBER_HUD -> Brush.horizontalGradient(listOf(cs.primary, cs.secondary))
          chrome.family == TacticalChromeFamily.RURAL_TIMBER -> Brush.horizontalGradient(listOf(cs.primary, cs.tertiary))
          chrome.family == TacticalChromeFamily.KINETIC_VIBRANT -> Brush.horizontalGradient(listOf(cs.primary, cs.secondary))
          else -> Brush.horizontalGradient(listOf(cs.primary, cs.primary))
        }
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(chrome.sliderTrackHeight)
            .clip(trackShape)
            .background(inactiveColor, trackShape)
            .then(
              if (isClassicWin) Modifier.windowsClassicBevel(sunken = true)
              else Modifier.border(
                width = if (chrome.family == TacticalChromeFamily.BRUTALIST) 1.5.dp else 0.8.dp,
                color = cs.outline.copy(alpha = 0.45f),
                shape = trackShape,
              )
            ),
      ) {
        Box(
          modifier =
            Modifier
              .fillMaxWidth(fraction)
              .height(chrome.sliderTrackHeight)
              .clip(trackShape)
              .background(activeBrush, trackShape),
        )
      }
    },
  )
}

@Composable
fun TacticalFilterChip(
  selected: Boolean,
  onClick: () -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  leadingIcon: ImageVector? = null,
) {
  val settings = LocalTacticalSettings.current
  val chrome = LocalTacticalChrome.current
  val cs = MaterialTheme.colorScheme
  val isClassic =
    (settings.visualStyle == VisualStyleMode.WINDOWS_CLASSIC || settings.visualStyle == VisualStyleMode.WINDOWS_2000) &&
      !settings.nightVisionMode &&
      !settings.sunVisibilityMode
  val bg =
    when {
      isClassic -> if (selected) TacticalColors.Win95TitleNavy else TacticalColors.Win95Silver
      selected && chrome.family == TacticalChromeFamily.OLED_PURE -> cs.primary.copy(alpha = 0.20f)
      selected -> cs.primary
      else -> cs.surfaceVariant.copy(alpha = 0.50f)
    }
  val fg =
    when {
      isClassic -> if (selected) Color.White else TacticalColors.Win95BlackInk
      selected && chrome.family == TacticalChromeFamily.OLED_PURE -> cs.primary
      selected -> cs.onPrimary
      else -> cs.onSurface
    }
  val borderCol =
    when {
      isClassic -> TacticalColors.Win95DarkShadow
      selected -> cs.primary
      else -> cs.outline.copy(alpha = 0.55f)
    }
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp),
    modifier =
      modifier
        .heightIn(min = 36.dp)
        .clip(chrome.smallShape)
        .then(
          if (isClassic) Modifier.background(bg, RectangleShape).windowsClassicBevel(sunken = selected)
          else Modifier.background(bg, chrome.smallShape).border(if (selected) 1.3.dp else 1.dp, borderCol, chrome.smallShape)
        )
        .clickable(enabled = enabled, onClick = onClick)
        .padding(horizontal = 11.dp, vertical = 7.dp),
  ) {
    if (leadingIcon != null) {
      Icon(leadingIcon, contentDescription = null, tint = fg, modifier = Modifier.size(15.dp))
    }
    Text(
      text = if (chrome.uppercaseHeaders && selected) label.uppercase() else label,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      color = fg,
      maxLines = 1,
      softWrap = false,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
fun tacticalOutlinedTextFieldColors(): TextFieldColors {
  val cs = MaterialTheme.colorScheme
  return OutlinedTextFieldDefaults.colors(
    focusedBorderColor = cs.primary,
    unfocusedBorderColor = cs.outline.copy(alpha = 0.65f),
    focusedLabelColor = cs.primary,
    unfocusedLabelColor = cs.onSurfaceVariant,
    focusedTextColor = cs.onSurface,
    unfocusedTextColor = cs.onSurface,
    cursorColor = cs.primary,
    focusedContainerColor = cs.surfaceVariant.copy(alpha = 0.24f),
    unfocusedContainerColor = cs.surfaceVariant.copy(alpha = 0.14f),
  )
}

@Composable
fun TacticalDialog(
  onDismissRequest: () -> Unit,
  title: String,
  subtitle: String? = null,
  icon: ImageVector? = null,
  badgeText: String? = null,
  confirmButton: (@Composable () -> Unit)? = null,
  dismissButton: (@Composable () -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val settings = LocalTacticalSettings.current
  val chrome = LocalTacticalChrome.current
  val cs = MaterialTheme.colorScheme

  Dialog(
    onDismissRequest = onDismissRequest,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    val dialogMod =
      Modifier
        .fillMaxWidth(0.94f)
        .widthIn(max = 560.dp)
        .heightIn(max = 680.dp)
        .padding(vertical = 12.dp)

    val isClassicWin = isWindowsClassicActive(settings) &&
      !isWindowsXpActive(settings) &&
      !isWindows7Active(settings) &&
      !isWindows10Active(settings) &&
      !isWindows11Active(settings)

    when {
      isWindowsXpActive(settings) -> {
        val xpShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        Column(
          modifier =
            dialogMod
              .shadow(14.dp, xpShape)
              .clip(xpShape)
              .background(Brush.verticalGradient(listOf(Color(0xFF0055EA), Color(0xFF003BB3))), xpShape)
              .border(1.5.dp, Color(0xFF003299), xpShape)
              .padding(start = 3.dp, end = 3.dp, bottom = 3.dp),
        ) {
          WindowsXpTitleBar(title = title, icon = icon, trailingBadge = badgeText)
          Column(
            modifier =
              Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .background(Color(0xFFECE9D8))
                .padding(14.dp),
            content = content,
          )
          if (confirmButton != null || dismissButton != null) {
            Row(
              modifier = Modifier.fillMaxWidth().background(Color(0xFFE3DFCC)).padding(horizontal = 12.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              dismissButton?.invoke()
              confirmButton?.invoke()
            }
          }
        }
      }

      isWindows7Active(settings) -> {
        val aeroShape = RoundedCornerShape(10.dp)
        Column(
          modifier =
            dialogMod
              .shadow(16.dp, aeroShape)
              .clip(aeroShape)
              .background(Brush.verticalGradient(listOf(Color(0xE8B8DDFB), Color(0xE86BA6DC))), aeroShape)
              .border(1.2.dp, Color(0xCCFFFFFF), aeroShape)
              .padding(5.dp),
        ) {
          Windows7AeroTitleBar(title = title, icon = icon, trailingBadge = badgeText)
          Column(
            modifier =
              Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFAFFFFFF))
                .border(1.dp, Color(0x99355678), RoundedCornerShape(4.dp))
                .padding(14.dp),
            content = content,
          )
          if (confirmButton != null || dismissButton != null) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(top = 8.dp, end = 4.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              dismissButton?.invoke()
              confirmButton?.invoke()
            }
          }
        }
      }

      isWindows10Active(settings) -> {
        Column(
          modifier =
            dialogMod
              .shadow(16.dp, RectangleShape)
              .background(cs.surface, RectangleShape)
              .border(1.5.dp, TacticalColors.Win10FluentBlue, RectangleShape),
        ) {
          Windows10FluentTitleBar(title = title, icon = icon, trailingBadge = badgeText)
          Column(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false).padding(14.dp),
            content = content,
          )
          if (confirmButton != null || dismissButton != null) {
            HorizontalDivider(color = cs.outline.copy(alpha = 0.3f))
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              dismissButton?.invoke()
              confirmButton?.invoke()
            }
          }
        }
      }

      isWindows11Active(settings) -> {
        val micaShape = RoundedCornerShape(14.dp)
        Column(
          modifier =
            dialogMod
              .shadow(18.dp, micaShape)
              .clip(micaShape)
              .background(cs.surface, micaShape)
              .border(1.dp, cs.outline.copy(alpha = 0.55f), micaShape),
        ) {
          Windows11MicaTitleBar(title = title, icon = icon, trailingBadge = badgeText)
          HorizontalDivider(color = cs.outline.copy(alpha = 0.25f))
          Column(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false).padding(14.dp),
            content = content,
          )
          if (confirmButton != null || dismissButton != null) {
            HorizontalDivider(color = cs.outline.copy(alpha = 0.2f))
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              dismissButton?.invoke()
              confirmButton?.invoke()
            }
          }
        }
      }

      isClassicWin -> {
        Column(
          modifier =
            dialogMod
              .background(cs.surface, RectangleShape)
              .windowsClassicBevel(sunken = false)
              .padding(3.dp),
        ) {
          ClassicWindowTitleBar(title = title, icon = icon, trailingBadge = badgeText)
          Column(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false).padding(14.dp),
            content = content,
          )
          if (confirmButton != null || dismissButton != null) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              dismissButton?.invoke()
              confirmButton?.invoke()
            }
          }
        }
      }

      else -> {
        val surfaceColor =
          if (chrome.family == TacticalChromeFamily.OLED_PURE) Color(0xFF020408)
          else cs.surface.copy(alpha = if (settings.sunVisibilityMode) 1f else 0.98f)
        val borderBrush =
          if (chrome.hasGradientBorder) Brush.horizontalGradient(listOf(cs.primary, cs.secondary, cs.tertiary))
          else Brush.horizontalGradient(listOf(cs.primary.copy(alpha = 0.78f), cs.outline.copy(alpha = 0.85f)))
        val headerBrush =
          when (chrome.family) {
            TacticalChromeFamily.VAPORWAVE ->
              Brush.horizontalGradient(listOf(cs.primary.copy(alpha = 0.26f), cs.secondary.copy(alpha = 0.18f)))
            TacticalChromeFamily.BRUTALIST ->
              Brush.horizontalGradient(listOf(cs.primary.copy(alpha = 0.28f), cs.primary.copy(alpha = 0.14f)))
            TacticalChromeFamily.KINETIC_VIBRANT ->
              Brush.horizontalGradient(listOf(cs.primaryContainer.copy(alpha = 0.72f), cs.secondaryContainer.copy(alpha = 0.52f)))
            TacticalChromeFamily.RURAL_TIMBER ->
              Brush.horizontalGradient(listOf(cs.primary.copy(alpha = 0.18f), cs.tertiary.copy(alpha = 0.08f)))
            else ->
              Brush.horizontalGradient(listOf(cs.primary.copy(alpha = 0.16f), cs.surfaceVariant.copy(alpha = 0.45f)))
          }
        val brutalMod =
          if (chrome.hasBrutalistShadow) {
            Modifier.padding(end = 5.dp, bottom = 5.dp).drawBehind {
              val off = 5.dp.toPx()
              drawRect(color = cs.primary, topLeft = Offset(off, off), size = size)
            }
          } else {
            Modifier.shadow(18.dp, chrome.dialogShape)
          }

        Column(
          modifier =
            dialogMod
              .then(brutalMod)
              .clip(chrome.dialogShape)
              .background(surfaceColor, chrome.dialogShape)
              .border(chrome.cardBorderWidth.coerceAtLeast(1.2.dp), borderBrush, chrome.dialogShape),
        ) {
          // Themed Dialog Header
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .background(headerBrush)
                .padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.weight(1f),
            ) {
              if (icon != null) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier =
                    Modifier
                      .size(32.dp)
                      .clip(chrome.smallShape)
                      .background(cs.primary.copy(alpha = 0.18f))
                      .border(1.dp, cs.primary.copy(alpha = 0.55f), chrome.smallShape),
                ) {
                  Icon(icon, contentDescription = null, tint = cs.primary, modifier = Modifier.size(18.dp))
                }
              }
              Column(Modifier.weight(1f)) {
                Text(
                  text = if (chrome.uppercaseHeaders) title.uppercase() else title,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Black,
                  color = cs.onSurface,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                  Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              if (!badgeText.isNullOrBlank()) {
                Box(
                  modifier =
                    Modifier
                      .background(cs.secondary.copy(alpha = 0.18f), chrome.smallShape)
                      .border(0.8.dp, cs.secondary.copy(alpha = 0.65f), chrome.smallShape)
                      .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                  Text(badgeText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = cs.secondary, maxLines = 1)
                }
              }
              IconButton(onClick = onDismissRequest, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = cs.onSurfaceVariant)
              }
            }
          }
          HorizontalDivider(
            thickness = if (chrome.family == TacticalChromeFamily.BRUTALIST) 2.dp else 1.dp,
            color = cs.primary.copy(alpha = 0.35f),
          )
          // Flexible Body
          Column(
            modifier =
              Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            content = content,
          )
          if (confirmButton != null || dismissButton != null) {
            HorizontalDivider(thickness = 0.8.dp, color = cs.outline.copy(alpha = 0.3f))
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .background(cs.surfaceVariant.copy(alpha = 0.22f))
                  .padding(horizontal = 14.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              dismissButton?.invoke()
              confirmButton?.invoke()
            }
          }
        }
      }
    }
  }
}

@Composable
fun BroadcastBanner(
  broadcast: EmergencyBroadcast,
) {
  Surface(
    color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.7f)),
    shape = MaterialTheme.shapes.small,
    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(Icons.Default.Warning, contentDescription = "Broadcast Alert", tint = MaterialTheme.colorScheme.error)
      Spacer(Modifier.width(8.dp))
      Column(Modifier.weight(1f)) {
        Text(
          text = broadcast.category,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.error,
        )
        Text(
          text = broadcast.message,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}

@Composable
fun RiderLevelBadge(xpPoints: Long, rankLabel: String) {
  val settings = LocalTacticalSettings.current
  val chrome = LocalTacticalChrome.current
  val isClassic95 =
    (settings.visualStyle == VisualStyleMode.WINDOWS_CLASSIC || settings.visualStyle == VisualStyleMode.WINDOWS_2000) &&
      !settings.nightVisionMode &&
      !settings.sunVisibilityMode
  val level = ((xpPoints / 250L) + 1L).coerceAtLeast(1L)
  val progress = (xpPoints % 250L) / 250f
  val badgeShape = if (isClassic95) RectangleShape else chrome.smallShape
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      if (isClassic95) {
        Modifier
          .background(TacticalColors.Win95Silver, RectangleShape)
          .windowsClassicBevel(sunken = true)
          .padding(horizontal = 8.dp, vertical = 4.dp)
      } else {
        Modifier
          .background(MaterialTheme.colorScheme.primaryContainer, badgeShape)
          .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), badgeShape)
          .padding(horizontal = 8.dp, vertical = 4.dp)
      },
  ) {
    Icon(
      Icons.Default.ElectricBolt,
      contentDescription = "Rider Level",
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(14.dp),
    )
    Spacer(Modifier.width(4.dp))
    Column {
      Text(
        "LVL $level • $rankLabel",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
      )
      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.width(84.dp).height(3.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
      )
    }
  }
}

@Composable
fun RoadSpeedSignWidget(
  speedLimitMph: Int?,
  currentSpeedMph: Double,
  roadName: String,
  inSchoolZone: Boolean,
  toleranceOffset: Int,
  schoolStrict: Boolean,
  legalComplianceMode: Boolean = false,
  pevLegalClass: PevLegalClass = PevLegalClass.CLASS_2_20,
  roadLimitSource: String = "",
  onRefresh: (() -> Unit)? = null,
) {
  val validRoadLimit = speedLimitMph?.takeIf { it in 5..85 }
  val classCap =
    if (legalComplianceMode && pevLegalClass != PevLegalClass.OFFROAD_UNLIMITED) {
      pevLegalClass.maxMph
    } else {
      null
    }
  val roadOrSchool =
    if (inSchoolZone && schoolStrict) minOf(validRoadLimit ?: 20, 20)
    else validRoadLimit
  val effectiveLimitMph =
    when {
      roadOrSchool != null && classCap != null -> minOf(roadOrSchool, classCap)
      roadOrSchool != null -> roadOrSchool
      else -> classCap
    }
  val speeding = effectiveLimitMph != null && currentSpeedMph > (effectiveLimitMph + toleranceOffset)
  val borderCol by
    animateColorAsState(
      if (speeding) MaterialTheme.colorScheme.error
      else if (inSchoolZone) MaterialTheme.colorScheme.tertiary
      else MaterialTheme.colorScheme.outline
    )

  val settings = LocalTacticalSettings.current
  val chrome = LocalTacticalChrome.current
  val isClassic = isWindowsClassicActive(settings)
  val badgeShape = if (isClassic) RectangleShape else chrome.smallShape
  val isMetric = settings.distanceUnit == "km"
  val displayedLimitText =
    effectiveLimitMph?.let { limMph ->
      if (isMetric) (limMph * 1.60934).roundToInt().toString() else limMph.toString()
    } ?: "--"
  val unitLbl = if (isMetric) "KM/H" else "MPH"
  val sourceLabel =
    when {
      roadOrSchool != null && roadLimitSource.isNotBlank() -> roadLimitSource
      roadOrSchool != null -> "LIVE SIGN"
      classCap != null -> "CLASS CAP"
      else -> "SEEKING"
    }

  Surface(
    shape = if (isClassic) RectangleShape else chrome.cardShape,
    color =
      when {
        inSchoolZone -> if (isClassic) Color(0xFFFFF3CD) else Color(0xFF422006)
        isClassic -> TacticalColors.Win95Silver
        else -> MaterialTheme.colorScheme.surfaceVariant
      },
    border = if (isClassic) null else BorderStroke(2.dp, borderCol),
    modifier =
      (if (isClassic) Modifier.fillMaxWidth().windowsClassicBevel(sunken = false) else Modifier.fillMaxWidth())
        .testTag("speed_limit_sign"),
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text =
              when {
                speeding -> "OVERSPEED ALERT"
                inSchoolZone -> "SCHOOL ZONE ACTIVE (${if (isMetric) "32 KM/H" else "20 MPH"})"
                legalComplianceMode -> "GOV ACTIVE • ${pevLegalClass.label}"
                else -> "LIVE ROAD SECTOR"
              },
            style = MaterialTheme.typography.labelSmall,
            color =
              if (speeding) MaterialTheme.colorScheme.error
              else if (inSchoolZone) MaterialTheme.colorScheme.tertiary
              else MaterialTheme.colorScheme.primary,
          )
          Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            shape = chrome.smallShape,
          ) {
            Text(
              text = sourceLabel,
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
              maxLines = 1,
              softWrap = false,
            )
          }
          if (legalComplianceMode) {
            Icon(Icons.Default.Gavel, contentDescription = "Legal Governor", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
          }
        }
        Text(
          text = roadName.ifBlank { "Acquiring GPS road name..." },
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (onRefresh != null) {
        IconButton(
          onClick = onRefresh,
          modifier = Modifier.size(48.dp).testTag("speed_limit_refresh_button"),
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Sync Speed Limit",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
          )
        }
      }
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier
            .size(width = 60.dp, height = 62.dp)
            .background(Color.White, badgeShape)
            .border(2.dp, if (speeding) Color.Red else Color.Black, badgeShape),
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            if (legalComplianceMode && classCap != null && roadOrSchool == null) "CAP" else "LIMIT",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            maxLines = 1,
            softWrap = false,
          )
          Text(
            text = displayedLimitText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = if (speeding) Color.Red else Color.Black,
            maxLines = 1,
            softWrap = false,
          )
          Text(
            text = unitLbl,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF334155),
            maxLines = 1,
            softWrap = false,
          )
        }
      }
    }
  }
}

fun resolveEffectiveHudSpeedLimitMph(
  telemetry: LiveTelemetrySnapshot,
  settings: MasterSettings,
): Int? {
  val validRoadLimit = telemetry.speedLimitMph?.takeIf { it in 5..85 }
  val classCap =
    if (settings.legalComplianceMode && settings.pevLegalClass != PevLegalClass.OFFROAD_UNLIMITED) {
      settings.pevLegalClass.maxMph
    } else {
      null
    }
  val roadOrSchool =
    if (telemetry.inSchoolZone && settings.schoolZoneStrict) minOf(validRoadLimit ?: 20, 20)
    else validRoadLimit
  return when {
    roadOrSchool != null && classCap != null -> minOf(roadOrSchool, classCap)
    roadOrSchool != null -> roadOrSchool
    else -> classCap
  }
}

@Composable
fun HudSpeedLimitBadge(
  telemetry: LiveTelemetrySnapshot,
  settings: MasterSettings,
  modifier: Modifier = Modifier,
) {
  val chrome = LocalTacticalChrome.current
  val effectiveLimitMph = resolveEffectiveHudSpeedLimitMph(telemetry, settings)
  val currentSpeedMph = telemetry.speedKmh * 0.621371
  val speeding = effectiveLimitMph != null && currentSpeedMph > (effectiveLimitMph + settings.speedOffsetTolerance)
  val displayLimit =
    effectiveLimitMph?.let { limMph ->
      if (settings.distanceUnit == "km") (limMph * 1.60934).roundToInt().toString() else limMph.toString()
    } ?: "--"
  val unitLbl = if (settings.distanceUnit == "km") "KM/H" else "MPH"
  val borderCol =
    when {
      speeding -> MaterialTheme.colorScheme.error
      telemetry.inSchoolZone -> Color(0xFFF59E0B)
      else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
    }

  Surface(
    color = if (speeding) MaterialTheme.colorScheme.errorContainer else Color.White,
    shape = chrome.smallShape,
    border = BorderStroke(2.dp, borderCol),
    modifier = modifier.testTag("hud_speed_limit_badge"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Text(
        text = if (telemetry.inSchoolZone) "SCHOOL" else if (settings.legalComplianceMode && telemetry.speedLimitMph == null) "CAP" else "LIMIT",
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Black,
        color = if (speeding) MaterialTheme.colorScheme.error else Color.Black,
        maxLines = 1,
        softWrap = false,
      )
      Text(
        text = displayLimit,
        fontSize = 15.sp,
        fontWeight = FontWeight.Black,
        color = if (speeding) MaterialTheme.colorScheme.error else Color.Black,
        maxLines = 1,
        softWrap = false,
      )
      Text(
        text = unitLbl,
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        color = if (speeding) MaterialTheme.colorScheme.error else Color(0xFF334155),
        maxLines = 1,
        softWrap = false,
      )
    }
  }
}

@Composable
fun SpeedometerHudGauge(
  telemetry: LiveTelemetrySnapshot,
  settings: MasterSettings,
  maxGaugeSpeed: Double = 60.0,
) {
  val chrome = LocalTacticalChrome.current
  val dispSpeed = formatSpeedValue(telemetry.speedKmh, settings.distanceUnit)
  val speedMph = telemetry.speedKmh * 0.621371
  val unitLbl = speedUnitLabel(settings.distanceUnit)
  val progress = (dispSpeed / maxGaugeSpeed).toFloat().coerceIn(0f, 1f)
  val effectiveLimitMph = resolveEffectiveHudSpeedLimitMph(telemetry, settings)
  val overRoadLimit = effectiveLimitMph != null && speedMph > (effectiveLimitMph + settings.speedOffsetTolerance)
  val overLegalCap =
    settings.legalComplianceMode &&
      settings.pevLegalClass != PevLegalClass.OFFROAD_UNLIMITED &&
      speedMph > (settings.pevLegalClass.maxMph + settings.speedOffsetTolerance)
  val overMax = (telemetry.speedKmh > 65.0 && settings.hudBlinkMax) || overLegalCap || overRoadLimit
  val accent = if (overMax) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary
  val surfaceBg = MaterialTheme.colorScheme.surfaceVariant
  val peakFrac = (formatSpeedValue(telemetry.maxSpeedKmh, settings.distanceUnit) / maxGaugeSpeed).toFloat().coerceIn(0f, 1f)
  val onSurfaceTint = MaterialTheme.colorScheme.onSurfaceVariant

  val tiltMod =
    if (settings.hud3dTilt && !settings.reducedMotion) {
      Modifier.graphicsLayer {
        rotationX = (telemetry.pitchDeg * 0.35f).coerceIn(-14f, 14f)
        rotationY = (telemetry.rollDeg * 0.35f).coerceIn(-14f, 14f)
        cameraDistance = 16f * density
      }
    } else Modifier

  Column(
    modifier = tiltMod.fillMaxWidth().testTag("speedometer_hud"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    // Live scrolling magnetic + GPS heading compass & roll integrated on EVERY HUD
    LiveHudCompassTape(
      bearingDeg = telemetry.bearingDeg,
      accent = accent,
      secondary = secondary,
      pitchDeg = telemetry.pitchDeg,
      rollDeg = telemetry.rollDeg,
    )

    // Embedded Live Speed Limit & Road/Mode Strip inside the HUD
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      HudSpeedLimitBadge(telemetry = telemetry, settings = settings)
      Text(
        text = telemetry.roadName.ifBlank { "LIVE ROADWAY" },
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = if (overRoadLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
      )
      Surface(
        color = (if (overMax) MaterialTheme.colorScheme.error else accent).copy(alpha = 0.14f),
        shape = chrome.smallShape,
        border = BorderStroke(1.dp, (if (overMax) MaterialTheme.colorScheme.error else accent).copy(alpha = 0.45f)),
      ) {
        Text(
          text = if (overRoadLimit) "OVERSPEED" else settings.hudStyle.label.uppercase(),
          fontSize = 9.sp,
          fontWeight = FontWeight.Black,
          color = if (overMax) MaterialTheme.colorScheme.error else accent,
          modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
          maxLines = 1,
          softWrap = false,
        )
      }
    }

    Box(
      modifier = Modifier.fillMaxWidth().height(206.dp),
      contentAlignment = Alignment.Center,
    ) {
      when (settings.hudStyle) {
        HudStyle.MINIMAL -> {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "%.${settings.decimalPrecision.coerceIn(0, 2)}f".format(dispSpeed),
              style = MaterialTheme.typography.displayLarge,
              color = accent,
            )
            Text(unitLbl, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(
              "PEAK ${"%.1f".format(formatSpeedValue(telemetry.maxSpeedKmh, settings.distanceUnit))} $unitLbl • BAT ${telemetry.batteryPct}% • ${"%.1f".format(telemetry.estimatedVoltage)}V",
              style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
              "LEAN ${"%.0f".format(abs(telemetry.rollDeg))}° • PEAK ${"%.2f".format(telemetry.peakG)}G",
              style = MaterialTheme.typography.labelSmall,
              color = secondary,
            )
          }
        }
        HudStyle.BAR -> {
          Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Row(verticalAlignment = Alignment.Bottom) {
              Text("%.1f".format(dispSpeed), style = MaterialTheme.typography.displayLarge, color = accent)
              Spacer(Modifier.width(6.dp))
              Text(unitLbl, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 10.dp))
            }
            Spacer(Modifier.height(14.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(36.dp)) {
              val segments = 24
              val gap = 6f
              val segW = (size.width - gap * (segments - 1)) / segments
              for (i in 0 until segments) {
                val frac = i.toFloat() / segments
                val active = frac < progress
                val isPeakSeg = peakFrac > 0.08f && abs(frac - peakFrac) < (1f / segments)
                val col =
                  when {
                    isPeakSeg -> Color(0xFFFFEA00)
                    !active -> surfaceBg
                    i > segments * 0.8f -> Color(0xFFF43F5E)
                    i > segments * 0.55f -> secondary
                    else -> accent
                  }
                drawRoundRect(
                  color = col,
                  topLeft = Offset(i * (segW + gap), 0f),
                  size = Size(segW, size.height),
                  cornerRadius = CornerRadius(4f, 4f),
                )
              }
            }
            Spacer(Modifier.height(8.dp))
            Text(
              "TRIP ${"%.2f".format(formatDistValue(telemetry.tripDistanceKm, settings.distanceUnit))} ${distUnitLabel(settings.distanceUnit)} • PEAK ${"%.1f".format(formatSpeedValue(telemetry.maxSpeedKmh, settings.distanceUnit))} $unitLbl",
              style = MaterialTheme.typography.labelMedium,
            )
          }
        }
        HudStyle.AVIATION -> {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.size(196.dp)) {
            AhrsHorizonCanvas(
              pitchDeg = telemetry.pitchDeg,
              rollDeg = telemetry.rollDeg,
              accent = accent,
              modifier = Modifier.fillMaxSize(),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("%.1f".format(dispSpeed), style = MaterialTheme.typography.displaySmall, color = accent, fontWeight = FontWeight.Black)
              Text(unitLbl, style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
          }
        }
        HudStyle.SUPERBIKE -> {
          SuperbikeTachHud(dispSpeed, unitLbl, progress, telemetry, settings, accent, secondary, surfaceBg)
        }
        HudStyle.F1_TELEMETRY -> {
          F1TelemetryWheelHud(dispSpeed, unitLbl, progress, telemetry, settings, accent, secondary, surfaceBg)
        }
        HudStyle.FIGHTER_JET -> {
          FighterJetHud(dispSpeed, unitLbl, telemetry, settings, accent)
        }
        HudStyle.COMPASS_ROSE -> {
          TacticalCompassRoseHud(dispSpeed, unitLbl, progress, telemetry, settings, accent, secondary, surfaceBg)
        }
        HudStyle.DUAL_TWIN -> {
          DualTwinClusterHud(dispSpeed, unitLbl, progress, telemetry, settings, accent, secondary, surfaceBg)
        }
        HudStyle.RETRO_SYNTH -> {
          RetroSynthWedgeHud(dispSpeed, unitLbl, progress, telemetry, settings, accent, secondary, surfaceBg)
        }
        HudStyle.EUC_LEAN -> {
          EucLeanCarvingHud(dispSpeed, unitLbl, progress, telemetry, settings, accent, secondary, surfaceBg)
        }
        HudStyle.S866_LCD -> {
          S866LcdHud(dispSpeed, unitLbl, telemetry, settings)
        }
        HudStyle.TF11_LCD -> {
          Tf11LcdHud(dispSpeed, unitLbl, progress, telemetry, settings)
        }
        HudStyle.BAFANG_DPC18 -> {
          BafangDpc18Hud(dispSpeed, unitLbl, progress, telemetry, settings)
        }
        HudStyle.ISINWHEEL_TFT -> {
          IsinwheelTftHud(dispSpeed, unitLbl, progress, telemetry, settings)
        }
        HudStyle.NINEBOT_MATRIX -> {
          NinebotMatrixHud(dispSpeed, unitLbl, telemetry, settings)
        }
        HudStyle.SW900_LCD -> {
          Sw900LcdHud(dispSpeed, unitLbl, telemetry, settings)
        }
        HudStyle.VSETT_EY3 -> {
          VsettEy3Hud(dispSpeed, unitLbl, progress, telemetry, settings)
        }
        else -> {
          val sweep = settings.dialSweepDeg.toFloat().coerceIn(180f, 300f)
          val startAngle = 90f + (360f - sweep) / 2f
          Canvas(modifier = Modifier.size(200.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outerR = size.minDimension * 0.48f
            val strokeW = if (settings.hudStyle == HudStyle.CYBER) 20f else 15f
            val numPaint =
              android.graphics.Paint().apply {
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 9.sp.toPx()
                isFakeBoldText = true
                color = onSurfaceTint.toArgb()
              }

            // Outer instrument bezel ring
            drawCircle(
              color = onSurfaceTint.copy(alpha = 0.25f),
              radius = outerR,
              center = Offset(cx, cy),
              style = Stroke(width = 2.dp.toPx()),
            )
            // Radial major/minor ticks + numerals
            for (t in 0..12) {
              val frac = t / 12f
              val deg = startAngle + sweep * frac
              val rad = Math.toRadians(deg.toDouble())
              val isMajor = t % 2 == 0
              val innerTick = outerR - (if (isMajor) 12f else 6f)
              drawLine(
                color = if (frac > 0.8f) Color(0xFFF43F5E) else onSurfaceTint.copy(alpha = if (isMajor) 0.85f else 0.4f),
                start = Offset(cx + (innerTick * cos(rad)).toFloat(), cy + (innerTick * sin(rad)).toFloat()),
                end = Offset(cx + (outerR * cos(rad)).toFloat(), cy + (outerR * sin(rad)).toFloat()),
                strokeWidth = if (isMajor) 2.2f else 1.2f,
              )
              if (isMajor) {
                val spdVal = (maxGaugeSpeed * frac).roundToInt()
                val labelR = outerR - 23f
                drawContext.canvas.nativeCanvas.drawText(
                  "$spdVal",
                  cx + (labelR * cos(rad)).toFloat(),
                  cy + (labelR * sin(rad)).toFloat() + 3f,
                  numPaint,
                )
              }
            }

            val arcR = outerR - 14f
            drawArc(
              color = surfaceBg,
              startAngle = startAngle,
              sweepAngle = sweep,
              useCenter = false,
              topLeft = Offset(cx - arcR, cy - arcR),
              size = Size(arcR * 2f, arcR * 2f),
              style = Stroke(width = strokeW, cap = StrokeCap.Round),
            )
            drawArc(
              brush = Brush.sweepGradient(listOf(accent, secondary, accent)),
              startAngle = startAngle,
              sweepAngle = sweep * progress,
              useCenter = false,
              topLeft = Offset(cx - arcR, cy - arcR),
              size = Size(arcR * 2f, arcR * 2f),
              style = Stroke(width = strokeW, cap = StrokeCap.Round),
            )
            if (peakFrac > 0.08f) {
              val pkRad = Math.toRadians((startAngle + sweep * peakFrac).toDouble())
              drawCircle(
                color = Color(0xFFFFEA00),
                radius = 4.dp.toPx(),
                center = Offset(cx + (arcR * cos(pkRad)).toFloat(), cy + (arcR * sin(pkRad)).toFloat()),
              )
            }
            if (settings.hudStyle == HudStyle.ANALOG) {
              val deg = startAngle + sweep * progress
              val rad = Math.toRadians(deg.toDouble())
              val c = Offset(cx, cy)
              val r = arcR * 0.78f
              drawLine(
                color = accent,
                start = c,
                end = Offset(c.x + (r * cos(rad)).toFloat(), c.y + (r * sin(rad)).toFloat()),
                strokeWidth = 5f,
                cap = StrokeCap.Round,
              )
              drawCircle(color = accent, radius = 6f, center = c)
            }
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "%.${settings.decimalPrecision.coerceIn(0, 1)}f".format(dispSpeed),
              style = MaterialTheme.typography.displayLarge,
              color = accent,
            )
            Text(unitLbl, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
              "BAT ${telemetry.batteryPct}% • ${"%.1f".format(telemetry.estimatedVoltage)}V",
              style = MaterialTheme.typography.labelSmall,
              color = secondary,
            )
          }
        }
      }
    }
  }
}

@Composable
fun AhrsHorizonCanvas(
  pitchDeg: Float,
  rollDeg: Float,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension / 2f
    val circleClip =
      Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(cx - r, cy - r, cx + r, cy + r))
      }
    clipPath(circleClip) {
      rotate(degrees = -rollDeg.coerceIn(-45f, 45f), pivot = Offset(cx, cy)) {
        val yShift = (pitchDeg * 2.2f).coerceIn(-r * 0.6f, r * 0.6f)
        // Sky vs Ground artificial horizon split
        drawRect(
          color = Color(0xFF0A2444),
          topLeft = Offset(cx - r * 2f, cy - r * 2f),
          size = Size(r * 4f, r * 2f + yShift),
        )
        drawRect(
          color = Color(0xFF21160B),
          topLeft = Offset(cx - r * 2f, cy + yShift),
          size = Size(r * 4f, r * 3f),
        )
        drawLine(
          color = accent,
          start = Offset(cx - r * 1.4f, cy + yShift),
          end = Offset(cx + r * 1.4f, cy + yShift),
          strokeWidth = 3f,
        )
        // Pitch ladder ±10° / ±20° rungs
        listOf(-44f, -22f, 22f, 44f).forEachIndexed { idx, dy ->
          val w = if (idx == 1 || idx == 2) r * 0.34f else r * 0.22f
          drawLine(
            color = accent.copy(alpha = 0.6f),
            start = Offset(cx - w, cy + yShift + dy),
            end = Offset(cx + w, cy + yShift + dy),
            strokeWidth = 2f,
          )
        }
      }
    }
    // Fixed center aircraft datum wings + top bank index
    drawLine(Color(0xFFFFEA00), Offset(cx - r * 0.35f, cy), Offset(cx - r * 0.14f, cy), strokeWidth = 2.8f, cap = StrokeCap.Round)
    drawLine(Color(0xFFFFEA00), Offset(cx + r * 0.14f, cy), Offset(cx + r * 0.35f, cy), strokeWidth = 2.8f, cap = StrokeCap.Round)
    val bankIndex =
      Path().apply {
        moveTo(cx, cy - r + 6f)
        lineTo(cx - 6f, cy - r + 15f)
        lineTo(cx + 6f, cy - r + 15f)
        close()
      }
    drawPath(bankIndex, Color(0xFFFFEA00))
    drawCircle(accent.copy(alpha = 0.72f), radius = r - 3f, style = Stroke(3.5f))
  }
}

fun pevCategoryIcon(category: String): ImageVector =
  when (category.uppercase()) {
    "EBIKE", "EFATBIKE", "EFOLDING", "ECARGO", "ETRIKE" -> Icons.Default.ElectricBike
    "SCOOTER" -> Icons.Default.ElectricScooter
    "EMOTO", "SURRON", "EMOPED", "EDIRTBIKE", "EGOKART", "EATV" -> Icons.Default.ElectricMoped
    "EUC" -> Icons.Default.ElectricBolt
    "ONEWHEEL", "ESKATE" -> Icons.Default.Skateboarding
    else -> Icons.Default.ElectricScooter
  }

fun pevCategoryDisplayLabel(category: String): String =
  when (category.uppercase()) {
    "SCOOTER" -> "E-Scooter"
    "EBIKE" -> "E-Bike"
    "EUC" -> "EUC"
    "ONEWHEEL" -> "Onewheel"
    "EMOTO" -> "E-Moto"
    "ESKATE" -> "E-Skate"
    "SURRON" -> "Sur-Ron / Talaria"
    "EMOPED" -> "E-Moped"
    "ETRIKE" -> "E-Trike"
    "ECARGO" -> "E-Cargo"
    "EFATBIKE" -> "Fat Tire E-Bike"
    "EFOLDING" -> "Folding E-Bike"
    "EDIRTBIKE" -> "E-Dirt Bike"
    "EGOKART" -> "E-GoKart"
    "EATV" -> "E-ATV / Quad"
    else -> "Other PEV"
  }


