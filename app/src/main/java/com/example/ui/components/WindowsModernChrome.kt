package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.data.MasterSettings
import com.example.data.VisualStyleMode
import com.example.ui.theme.LocalTacticalSettings
import com.example.ui.theme.TacticalColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

fun isWindows10Active(settings: MasterSettings): Boolean =
  settings.visualStyle == VisualStyleMode.WINDOWS_10 &&
    !settings.nightVisionMode &&
    !settings.sunVisibilityMode

fun isWindows11Active(settings: MasterSettings): Boolean =
  settings.visualStyle == VisualStyleMode.WINDOWS_11 &&
    !settings.nightVisionMode &&
    !settings.sunVisibilityMode

fun isAnyWindowsDesktopActive(settings: MasterSettings): Boolean =
  (settings.visualStyle == VisualStyleMode.WINDOWS_CLASSIC ||
    settings.visualStyle == VisualStyleMode.WINDOWS_2000 ||
    settings.visualStyle == VisualStyleMode.WINDOWS_XP ||
    settings.visualStyle == VisualStyleMode.WINDOWS_7 ||
    settings.visualStyle == VisualStyleMode.WINDOWS_10 ||
    settings.visualStyle == VisualStyleMode.WINDOWS_11) &&
    !settings.nightVisionMode &&
    !settings.sunVisibilityMode

@Composable
private fun rememberModernTrayClock(): Pair<String, String> {
  var timeText by remember { mutableStateOf("") }
  var dateText by remember { mutableStateOf("") }
  LaunchedEffect(Unit) {
    val tFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
    val dFmt = SimpleDateFormat("M/d/yyyy", Locale.getDefault())
    while (true) {
      val now = Date()
      timeText = tFmt.format(now)
      dateText = dFmt.format(now)
      delay(15_000L)
    }
  }
  val now = Date()
  return Pair(
    timeText.ifBlank { SimpleDateFormat("h:mm a", Locale.getDefault()).format(now) },
    dateText.ifBlank { SimpleDateFormat("M/d/yyyy", Locale.getDefault()).format(now) },
  )
}

/**
 * Windows 10 perspective monochrome 4-pane logo.
 */
@Composable
fun Windows10PerspectiveEmblem(
  size: Dp = 16.dp,
  tint: Color = Color.White,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val gap = w * 0.08f
    val leftW = w * 0.42f
    val rightX = leftW + gap
    val midY = h * 0.5f

    // Top-left trapezoid
    val tl = Path().apply {
      moveTo(0f, h * 0.14f)
      lineTo(leftW, h * 0.07f)
      lineTo(leftW, midY - gap / 2f)
      lineTo(0f, midY - gap / 2f)
      close()
    }
    // Top-right trapezoid
    val tr = Path().apply {
      moveTo(rightX, h * 0.06f)
      lineTo(w, 0f)
      lineTo(w, midY - gap / 2f)
      lineTo(rightX, midY - gap / 2f)
      close()
    }
    // Bottom-left trapezoid
    val bl = Path().apply {
      moveTo(0f, midY + gap / 2f)
      lineTo(leftW, midY + gap / 2f)
      lineTo(leftW, h * 0.93f)
      lineTo(0f, h * 0.86f)
      close()
    }
    // Bottom-right trapezoid
    val br = Path().apply {
      moveTo(rightX, midY + gap / 2f)
      lineTo(w, midY + gap / 2f)
      lineTo(w, h)
      lineTo(rightX, h * 0.94f)
      close()
    }
    drawPath(tl, tint)
    drawPath(tr, tint)
    drawPath(bl, tint)
    drawPath(br, tint)
  }
}

/**
 * Authentic Windows 11 crisp 4-square azure gradient Start logo.
 */
@Composable
fun Windows11SquareEmblem(
  size: Dp = 18.dp,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val gap = w * 0.09f
    val pw = (w - gap) / 2f
    val ph = (h - gap) / 2f
    val r = CornerRadius(w * 0.06f, w * 0.06f)
    drawRoundRect(
      brush = Brush.linearGradient(listOf(Color(0xFF60CDFF), Color(0xFF33A6F7))),
      topLeft = Offset(0f, 0f),
      size = Size(pw, ph),
      cornerRadius = r,
    )
    drawRoundRect(
      brush = Brush.linearGradient(listOf(Color(0xFF4EB8FF), Color(0xFF1F92EC))),
      topLeft = Offset(pw + gap, 0f),
      size = Size(pw, ph),
      cornerRadius = r,
    )
    drawRoundRect(
      brush = Brush.linearGradient(listOf(Color(0xFF38A4F5), Color(0xFF147CD6))),
      topLeft = Offset(0f, ph + gap),
      size = Size(pw, ph),
      cornerRadius = r,
    )
    drawRoundRect(
      brush = Brush.linearGradient(listOf(Color(0xFF238EE8), Color(0xFF0067C0))),
      topLeft = Offset(pw + gap, ph + gap),
      size = Size(pw, ph),
      cornerRadius = r,
    )
  }
}

/**
 * Classic Windows 95/2000/XP Menu Strip ("File  Edit  View  Tools  Help") rendered on desktop windows
 * when settings.winShowMenuBar is enabled.
 */
@Composable
fun ClassicWindowsMenuStrip(
  isXp: Boolean = false,
  is2k: Boolean = false,
) {
  val bg =
    when {
      isXp -> Color(0xFFECE9D8)
      is2k -> TacticalColors.Win2KPutty
      else -> TacticalColors.Win95Silver
    }
  val dividerColor = if (isXp) Color(0xFFD8D2BD) else TacticalColors.Win95Shadow
  Column(modifier = Modifier.fillMaxWidth().background(bg)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      listOf("File", "Edit", "View", "Tools", "Help").forEach { item ->
        Text(
          text = item,
          fontSize = 10.sp,
          color = Color(0xFF111111),
          fontWeight = FontWeight.Normal,
          maxLines = 1,
          softWrap = false,
        )
      }
    }
    HorizontalDivider(thickness = 0.8.dp, color = dividerColor)
  }
}

// ============================================================================
// WINDOWS 10 FLUENT CHROME (WINDOW CARD, TITLE BAR, BUTTONS, TASKBAR)
// ============================================================================

@Composable
fun Windows10FluentTitleBar(
  title: String,
  icon: ImageVector? = null,
  trailingBadge: String? = null,
) {
  val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
  val barBg = if (isDark) Color(0xFF161616) else TacticalColors.Win10FluentBlue
  Row(
    modifier = Modifier.fillMaxWidth().background(barBg).padding(start = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f).padding(vertical = 6.dp),
    ) {
      if (icon != null) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(7.dp))
      } else {
        Windows10PerspectiveEmblem(size = 12.dp, tint = Color.White)
        Spacer(Modifier.width(7.dp))
      }
      Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (!trailingBadge.isNullOrBlank()) {
        Box(
          modifier =
            Modifier
              .padding(end = 6.dp)
              .background(Color(0x33FFFFFF), RectangleShape)
              .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
          Text(trailingBadge, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
        }
      }
      listOf("—", "▢", "✕").forEach { glyph ->
        val isClose = glyph == "✕"
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier
              .size(width = 30.dp, height = 28.dp)
              .background(if (isClose) Color(0xFFE81123) else Color.Transparent),
        ) {
          Text(
            text = glyph,
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
            color = Color.White,
          )
        }
      }
    }
  }
}

@Composable
fun Windows10FluentWindowCard(
  modifier: Modifier = Modifier,
  highlight: Boolean = false,
  title: String? = null,
  showTitleBar: Boolean = true,
  content: @Composable ColumnScope.() -> Unit,
) {
  val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
  val borderCol =
    if (highlight) TacticalColors.Win10FluentBlue
    else if (isDark) Color(0xFF3E3E42) else Color(0xFF0078D7).copy(alpha = 0.65f)
  val hasTitle = !title.isNullOrBlank() && showTitleBar
  Column(
    modifier =
      modifier
        .shadow(elevation = if (highlight) 8.dp else 4.dp, shape = RectangleShape)
        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f), RectangleShape)
        .border(1.2.dp, borderCol, RectangleShape),
  ) {
    if (hasTitle) {
      Windows10FluentTitleBar(title = title!!, trailingBadge = if (highlight) "LIVE" else null)
    } else if (highlight) {
      Box(Modifier.fillMaxWidth().height(2.5.dp).background(TacticalColors.Win10FluentBlue))
    }
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), content = content)
  }
}

@Composable
fun Windows10FluentButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  isPrimary: Boolean = true,
  isDestructive: Boolean = false,
  contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
  content: @Composable RowScope.() -> Unit,
) {
  val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val bg =
    when {
      !enabled -> if (isDark) Color(0xFF333333) else Color(0xFFCCCCCC)
      isDestructive -> if (isPressed) Color(0xFFB50E1B) else Color(0xFFE81123)
      isPrimary -> if (isPressed) Color(0xFF005A9E) else TacticalColors.Win10FluentBlue
      else -> if (isPressed) Color(0xFF555555) else if (isDark) Color(0xFF333333) else Color(0xFFE1E1E1)
    }
  val borderCol =
    when {
      !enabled -> Color.Transparent
      isDestructive -> Color(0xFFFF6673)
      isPrimary -> Color(0x66FFFFFF)
      else -> if (isDark) Color(0xFF666666) else Color(0xFFADADAD)
    }
  val fg =
    when {
      !enabled -> Color(0xFF888888)
      isPrimary || isDestructive -> Color.White
      else -> if (isDark) Color.White else Color(0xFF111111)
    }
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
        .background(bg, RectangleShape)
        .border(1.5.dp, borderCol, RectangleShape)
        .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
        .padding(contentPadding),
  ) {
    CompositionLocalProvider(LocalContentColor provides fg) {
      ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, color = fg)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, content = content)
      }
    }
  }
}

@Composable
fun Windows10FluentTaskbar(
  tabLabels: List<String>,
  tabIcons: List<ImageVector>,
  tabTestTags: List<String>,
  activeTabIdx: Int,
  onSelectTab: (Int) -> Unit,
  startMenuOpen: Boolean,
  onToggleStartMenu: () -> Unit,
  showClock: Boolean = true,
  compact: Boolean = false,
  startMenuContent: @Composable () -> Unit,
) {
  val (timeStr, dateStr) = rememberModernTrayClock()
  val barHeight = if (compact) 46.dp else 52.dp
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .background(Color(0xF5101010))
        .drawBehind {
          drawLine(Color(0x33FFFFFF), Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1f)
        }
        .navigationBarsPadding(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().height(barHeight),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier
              .size(width = 48.dp, height = barHeight)
              .background(if (startMenuOpen) Color(0xFF1F1F1F) else Color.Transparent)
              .clickable(onClick = onToggleStartMenu)
              .testTag("win95_start_button"),
        ) {
          Windows10PerspectiveEmblem(
            size = 18.dp,
            tint = if (startMenuOpen) Color(0xFF42A5F5) else Color.White,
          )
        }
        startMenuContent()
      }

      Row(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        tabLabels.forEachIndexed { idx, lbl ->
          val selected = idx == activeTabIdx
          Box(
            modifier =
              Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (selected) Color(0xFF282828) else Color.Transparent)
                .clickable { onSelectTab(idx) }
                .testTag(tabTestTags[idx]),
          ) {
            Column(
              modifier = Modifier.align(Alignment.Center).padding(horizontal = 2.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
            ) {
              Icon(
                tabIcons[idx],
                contentDescription = lbl,
                tint = if (selected) Color(0xFF60CDFF) else Color(0xFFD6D6D6),
                modifier = Modifier.size(17.dp),
              )
              Text(
                text = lbl,
                fontSize = 8.5.sp,
                lineHeight = 9.5.sp,
                color = if (selected) Color.White else Color(0xFFCCCCCC),
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
              )
            }
            // Iconic Windows 10 bottom accent underline bar
            if (selected) {
              Box(
                modifier =
                  Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.84f)
                    .height(2.5.dp)
                    .background(TacticalColors.Win10FluentBlue),
              )
            }
          }
        }
      }

      if (showClock) {
        Column(
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.Center,
          modifier = Modifier.padding(horizontal = 7.dp),
        ) {
          Text(timeStr, fontSize = 9.5.sp, lineHeight = 11.sp, color = Color.White, maxLines = 1, softWrap = false)
          Text(dateStr, fontSize = 8.sp, lineHeight = 9.5.sp, color = Color(0xFFBDBDBD), maxLines = 1, softWrap = false)
        }
      }
    }
  }
}

// ============================================================================
// WINDOWS 11 MICA / ACRYLIC CHROME (ROUNDED MICA CARDS, CENTERED TASKBAR)
// ============================================================================

@Composable
fun Windows11MicaTitleBar(
  title: String,
  icon: ImageVector? = null,
  trailingBadge: String? = null,
) {
  val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
  val fg = if (isDark) Color(0xFFF3F6FA) else Color(0xFF1A1F29)
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      if (icon != null) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(7.dp))
      } else {
        Windows11SquareEmblem(size = 13.dp)
        Spacer(Modifier.width(7.dp))
      }
      Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = fg,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
      if (!trailingBadge.isNullOrBlank()) {
        Box(
          modifier =
            Modifier
              .padding(end = 4.dp)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), RoundedCornerShape(6.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
          Text(trailingBadge, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, maxLines = 1)
        }
      }
      listOf("—", "▢", "✕").forEach { glyph ->
        val isClose = glyph == "✕"
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier
              .size(width = 26.dp, height = 22.dp)
              .clip(RoundedCornerShape(5.dp))
              .background(if (isClose) Color(0x22E81123) else Color.Transparent),
        ) {
          Text(
            text = glyph,
            fontSize = 9.5.sp,
            color = if (isClose) Color(0xFFE81123) else fg.copy(alpha = 0.78f),
          )
        }
      }
    }
  }
}

@Composable
fun Windows11MicaWindowCard(
  modifier: Modifier = Modifier,
  highlight: Boolean = false,
  title: String? = null,
  showTitleBar: Boolean = true,
  content: @Composable ColumnScope.() -> Unit,
) {
  val settings = LocalTacticalSettings.current
  val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
  val shape = RoundedCornerShape(12.dp)
  val alpha = settings.winGlassIntensity.coerceIn(0.75f, 0.96f)
  val bg =
    if (isDark) Color(0xFF20242D).copy(alpha = alpha)
    else Color(0xFFF9FBFE).copy(alpha = alpha)
  val borderCol =
    when {
      highlight -> MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
      isDark -> Color(0x44FFFFFF)
      else -> Color(0x28000000)
    }
  val hasTitle = !title.isNullOrBlank() && showTitleBar
  Column(
    modifier =
      modifier
        .shadow(elevation = if (highlight) 10.dp else 5.dp, shape = shape)
        .clip(shape)
        .background(bg, shape)
        .border(1.dp, borderCol, shape),
  ) {
    if (hasTitle) {
      Windows11MicaTitleBar(title = title!!, trailingBadge = if (highlight) "LIVE" else null)
      HorizontalDivider(thickness = 0.6.dp, color = if (isDark) Color(0x22FFFFFF) else Color(0x18000000))
    }
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), content = content)
  }
}

@Composable
fun Windows11MicaButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  isPrimary: Boolean = true,
  isDestructive: Boolean = false,
  contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
  content: @Composable RowScope.() -> Unit,
) {
  val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val shape = RoundedCornerShape(6.dp)
  val bg =
    when {
      !enabled -> if (isDark) Color(0xFF2D313A) else Color(0xFFE5E7EB)
      isDestructive -> if (isPressed) Color(0xFFA82115) else Color(0xFFC42B1C)
      isPrimary ->
        if (isDark) {
          if (isPressed) Color(0xFF4FB8E8) else TacticalColors.Win11MicaAccentLight
        } else {
          if (isPressed) Color(0xFF00559E) else TacticalColors.Win11MicaAccent
        }
      else -> if (isDark) Color(0xFF2D323C) else Color(0xFFFFFFFF)
    }
  val fg =
    when {
      !enabled -> Color(0xFF8A92A0)
      isDestructive -> Color.White
      isPrimary -> if (isDark) Color(0xFF001B2E) else Color.White
      else -> if (isDark) Color(0xFFF3F6FA) else Color(0xFF181C24)
    }
  val borderCol =
    when {
      isPrimary || isDestructive -> Color(0x33FFFFFF)
      isDark -> Color(0x33FFFFFF)
      else -> Color(0x22000000)
    }
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
        .clip(shape)
        .background(bg, shape)
        .border(1.dp, borderCol, shape)
        .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
        .padding(contentPadding),
  ) {
    CompositionLocalProvider(LocalContentColor provides fg) {
      ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, color = fg)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, content = content)
      }
    }
  }
}

@Composable
fun Windows11MicaTaskbar(
  tabLabels: List<String>,
  tabIcons: List<ImageVector>,
  tabTestTags: List<String>,
  activeTabIdx: Int,
  onSelectTab: (Int) -> Unit,
  startMenuOpen: Boolean,
  onToggleStartMenu: () -> Unit,
  showClock: Boolean = true,
  compact: Boolean = false,
  startMenuContent: @Composable () -> Unit,
) {
  val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
  val (timeStr, _) = rememberModernTrayClock()
  val barBg = if (isDark) Color(0xEC1B2028) else Color(0xECF0F5FA)
  val fg = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
  val barHeight = if (compact) 48.dp else 54.dp

  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .background(barBg)
        .drawBehind {
          drawLine(
            if (isDark) Color(0x30FFFFFF) else Color(0x22000000),
            Offset(0f, 0f),
            Offset(size.width, 0f),
            strokeWidth = 1f,
          )
        }
        .navigationBarsPadding(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().height(barHeight).padding(horizontal = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      // Centered-feel Windows 11 Start button
      Box {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier
              .size(44.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(
                if (startMenuOpen) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                else Color.Transparent
              )
              .clickable(onClick = onToggleStartMenu)
              .testTag("win95_start_button"),
        ) {
          Windows11SquareEmblem(size = 20.dp)
        }
        startMenuContent()
      }

      Row(
        modifier = Modifier.weight(1f).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        tabLabels.forEachIndexed { idx, lbl ->
          val selected = idx == activeTabIdx
          Box(
            modifier =
              Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(7.dp))
                .background(
                  if (selected) {
                    if (isDark) Color(0x2AFFFFFF) else Color(0xCCFFFFFF)
                  } else Color.Transparent
                )
                .then(
                  if (selected) Modifier.border(0.8.dp, if (isDark) Color(0x30FFFFFF) else Color(0x20000000), RoundedCornerShape(7.dp))
                  else Modifier
                )
                .clickable { onSelectTab(idx) }
                .testTag(tabTestTags[idx]),
          ) {
            Column(
              modifier = Modifier.align(Alignment.Center).padding(horizontal = 2.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
            ) {
              Icon(
                tabIcons[idx],
                contentDescription = lbl,
                tint = if (selected) MaterialTheme.colorScheme.primary else fg.copy(alpha = 0.78f),
                modifier = Modifier.size(17.dp),
              )
              Text(
                text = lbl,
                fontSize = 8.5.sp,
                lineHeight = 9.5.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else fg.copy(alpha = 0.8f),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
              )
            }
            // Iconic Windows 11 glowing pill indicator underneath active app
            if (selected) {
              Box(
                modifier =
                  Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .width(16.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary),
              )
            }
          }
        }
      }

      if (showClock) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.padding(end = 2.dp),
        ) {
          Icon(Icons.Default.Wifi, contentDescription = null, tint = fg.copy(alpha = 0.72f), modifier = Modifier.size(12.dp))
          Text(
            text = timeStr,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Medium,
            color = fg,
            maxLines = 1,
            softWrap = false,
          )
        }
      }
    }
  }
}

// ============================================================================
// AUTHENTIC ERA-ACCURATE WINDOWS START MENU POPUP (95 / 2000 / XP / 7 / 10 / 11)
// ============================================================================

@Composable
fun AuthenticWindowsStartMenuPopup(
  expanded: Boolean,
  visualStyle: VisualStyleMode,
  pilotCallsign: String,
  tabLabels: List<String>,
  tabIcons: List<ImageVector>,
  onDismiss: () -> Unit,
  onSelectTab: (Int) -> Unit,
  onCycleTheme: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenInfo: () -> Unit,
  onOpenTelemetryStudio: () -> Unit,
  onOpenPrayer: () -> Unit,
  onSignOut: () -> Unit,
) {
  if (!expanded) return
  Popup(
    alignment = Alignment.BottomStart,
    onDismissRequest = onDismiss,
    properties = PopupProperties(focusable = true),
  ) {
    val isXp = visualStyle == VisualStyleMode.WINDOWS_XP
    val isWin7 = visualStyle == VisualStyleMode.WINDOWS_7
    val isWin10 = visualStyle == VisualStyleMode.WINDOWS_10
    val isWin11 = visualStyle == VisualStyleMode.WINDOWS_11
    val is2k = visualStyle == VisualStyleMode.WINDOWS_2000

    when {
      isXp -> {
        // Authentic 2-column Windows XP Luna Start Menu
        Column(
          modifier =
            Modifier
              .padding(start = 4.dp, bottom = 54.dp)
              .width(310.dp)
              .shadow(12.dp, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
              .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
              .border(2.dp, Color(0xFF0054E3), RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp)),
        ) {
          // Glossy Blue User Header
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF4CA0FF), Color(0xFF1968E6), Color(0xFF0E4CC4))))
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier =
                Modifier
                  .size(34.dp)
                  .clip(RoundedCornerShape(5.dp))
                  .background(Color(0xFFD6E8FF))
                  .border(1.5.dp, Color.White, RoundedCornerShape(5.dp)),
            ) {
              WindowsFourPaneEmblem(size = 20.dp)
            }
            Text(
              text = pilotCallsign.ifBlank { "Rural Rider" },
              color = Color.White,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFFFF8C00)))
          // Two-Column XP Body
          Row(modifier = Modifier.fillMaxWidth()) {
            // Left White Programs Column
            Column(
              modifier = Modifier.weight(1.1f).background(Color.White).padding(6.dp),
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              tabLabels.forEachIndexed { idx, lbl ->
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(7.dp),
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(4.dp))
                      .clickable {
                        onSelectTab(idx)
                        onDismiss()
                      }
                      .padding(horizontal = 6.dp, vertical = 7.dp),
                ) {
                  Icon(tabIcons[idx], contentDescription = null, tint = Color(0xFF0055EA), modifier = Modifier.size(16.dp))
                  Text(lbl, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
              }
            }
            // Right Soft Sky-Blue Places Column
            Column(
              modifier =
                Modifier
                  .weight(0.95f)
                  .background(Color(0xFFD3E5FA))
                  .border(0.8.dp, Color(0xFF95BDEE))
                  .padding(6.dp),
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              XpStartRightItem("Control Panel", Icons.Default.Settings) { onOpenSettings(); onDismiss() }
              XpStartRightItem("Telemetry Lab", Icons.Default.ShowChart) { onOpenTelemetryStudio(); onDismiss() }
              XpStartRightItem("Next Theme", Icons.Default.Palette) { onCycleTheme(); onDismiss() }
              XpStartRightItem("Rider's Prayer", Icons.Default.AutoAwesome) { onOpenPrayer(); onDismiss() }
              XpStartRightItem("System Info", Icons.Default.Info) { onOpenInfo(); onDismiss() }
            }
          }
          // Glossy XP Footer Bar
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF3E89F0), Color(0xFF1C5BD6))))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
              modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { onDismiss(); onSignOut() }.padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE0422A)).border(1.dp, Color.White, RoundedCornerShape(4.dp)),
              ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
              }
              Text("Log Off / Sign Out", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
          }
        }
      }

      isWin7 -> {
        // Authentic Windows 7 Frosted Sapphire Aero Glass Start Menu
        Column(
          modifier =
            Modifier
              .padding(start = 6.dp, bottom = 56.dp)
              .width(316.dp)
              .shadow(14.dp, RoundedCornerShape(10.dp))
              .clip(RoundedCornerShape(10.dp))
              .background(Brush.verticalGradient(listOf(Color(0xEB1D4E7D), Color(0xF00E2E52), Color(0xF50B2440))))
              .border(1.2.dp, Color(0xCC9AD4FF), RoundedCornerShape(10.dp))
              .padding(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Left White Programs List
            Column(
              modifier =
                Modifier
                  .weight(1.15f)
                  .clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFAFFFFFF))
                  .border(1.dp, Color(0x88355E85), RoundedCornerShape(6.dp))
                  .padding(6.dp),
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              tabLabels.forEachIndexed { idx, lbl ->
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(7.dp),
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(4.dp))
                      .clickable { onSelectTab(idx); onDismiss() }
                      .padding(horizontal = 6.dp, vertical = 7.dp),
                ) {
                  Icon(tabIcons[idx], contentDescription = null, tint = Color(0xFF1A7AD4), modifier = Modifier.size(16.dp))
                  Text(lbl, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
              }
            }
            // Right Frosted Dark-Glass Controls
            Column(
              modifier = Modifier.weight(0.9f).padding(vertical = 2.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(pilotCallsign.ifBlank { "Rider" }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
              HorizontalDivider(color = Color(0x44FFFFFF))
              Win7StartGlassLink("Control Panel") { onOpenSettings(); onDismiss() }
              Win7StartGlassLink("Telemetry Studio") { onOpenTelemetryStudio(); onDismiss() }
              Win7StartGlassLink("Switch Theme") { onCycleTheme(); onDismiss() }
              Win7StartGlassLink("Rider's Prayer") { onOpenPrayer(); onDismiss() }
              Win7StartGlassLink("System Health") { onOpenInfo(); onDismiss() }
            }
          }
          // Bottom Aero Search & Shut Down Bar
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier =
                Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(4.dp))
                  .background(Color.White)
                  .border(1.dp, Color(0xFF4F8CC9), RoundedCornerShape(4.dp))
                  .clickable { onOpenSettings(); onDismiss() }
                  .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
              Text("Search settings...", fontSize = 10.5.sp, color = Color(0xFF64748B), modifier = Modifier.weight(1f), maxLines = 1)
              Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF1A7AD4), modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(8.dp))
            Box(
              modifier =
                Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x3370B8F0), Color(0x55225688))))
                  .border(1.dp, Color(0xAAFFFFFF), RoundedCornerShape(4.dp))
                  .clickable { onDismiss(); onSignOut() }
                  .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
              Text("Sign Out", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
          }
        }
      }

      isWin10 -> {
        // Authentic Windows 10 Dark Fluent Start Menu with Pinned Tiles
        Column(
          modifier =
            Modifier
              .padding(start = 2.dp, bottom = 54.dp)
              .width(310.dp)
              .shadow(14.dp, RectangleShape)
              .background(Color(0xF61A1A1A), RectangleShape)
              .border(1.dp, Color(0xFF3A3A3A), RectangleShape)
              .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Windows10PerspectiveEmblem(size = 16.dp, tint = Color(0xFF3AA0FF))
            Text("Rural ERides GO • $pilotCallsign", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          // 2x3 Live Fluent Tile Grid
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            tabLabels.chunked(3).forEachIndexed { rowIdx, rowLabels ->
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                rowLabels.forEachIndexed { colIdx, lbl ->
                  val idx = rowIdx * 3 + colIdx
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier =
                      Modifier
                        .weight(1f)
                        .height(60.dp)
                        .background(if (idx % 2 == 0) Color(0xFF0078D7) else Color(0xFF005A9E))
                        .clickable { onSelectTab(idx); onDismiss() }
                        .padding(4.dp),
                  ) {
                    Icon(tabIcons[idx], contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.height(3.dp))
                    Text(lbl, fontSize = 9.5.sp, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  }
                }
              }
            }
          }
          HorizontalDivider(color = Color(0xFF333333))
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            Win10ActionTile("Settings", Icons.Default.Settings, Modifier.weight(1f)) { onOpenSettings(); onDismiss() }
            Win10ActionTile("Next Theme", Icons.Default.Palette, Modifier.weight(1f)) { onCycleTheme(); onDismiss() }
            Win10ActionTile("Sign Out", Icons.AutoMirrored.Filled.Logout, Modifier.weight(1f)) { onDismiss(); onSignOut() }
          }
        }
      }

      isWin11 -> {
        // Authentic Windows 11 Floating Rounded Mica Start Menu
        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        val bg = if (isDark) Color(0xF520242E) else Color(0xF7F4F8FD)
        val fg = if (isDark) Color(0xFFF8FAFC) else Color(0xFF141820)
        Column(
          modifier =
            Modifier
              .padding(start = 10.dp, bottom = 58.dp)
              .width(316.dp)
              .shadow(16.dp, RoundedCornerShape(14.dp))
              .clip(RoundedCornerShape(14.dp))
              .background(bg)
              .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x22000000), RoundedCornerShape(14.dp))
              .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          // Win11 Pill Search Box
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier =
              Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(if (isDark) Color(0xFF2C313D) else Color.White)
                .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x22000000), RoundedCornerShape(20.dp))
                .clickable { onOpenSettings(); onDismiss() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
          ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Text("Search settings, rigs, or trails", fontSize = 11.5.sp, color = fg.copy(alpha = 0.68f), maxLines = 1)
          }
          Text("Pinned", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            tabLabels.chunked(3).forEachIndexed { rowIdx, rowLabels ->
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                rowLabels.forEachIndexed { colIdx, lbl ->
                  val idx = rowIdx * 3 + colIdx
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier =
                      Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0x16FFFFFF) else Color(0x99FFFFFF))
                        .clickable { onSelectTab(idx); onDismiss() }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                  ) {
                    Icon(tabIcons[idx], contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.height(3.dp))
                    Text(lbl, fontSize = 9.5.sp, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  }
                }
              }
            }
          }
          HorizontalDivider(color = if (isDark) Color(0x22FFFFFF) else Color(0x18000000))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Windows11SquareEmblem(size = 15.dp)
              Text(pilotCallsign.ifBlank { "Rider" }, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Surface(
                onClick = { onCycleTheme(); onDismiss() },
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
              ) {
                Text("Theme", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
              }
              Surface(
                onClick = { onOpenSettings(); onDismiss() },
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary,
              ) {
                Text("Settings", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
              }
            }
          }
        }
      }

      else -> {
        // Authentic Windows 95 / Windows 2000 Start Menu with Vertical Left Banner
        val menuBg = if (is2k) TacticalColors.Win2KPutty else TacticalColors.Win95Silver
        val sidebarBrush =
          if (is2k) Brush.verticalGradient(listOf(TacticalColors.Win2KTitleSky, TacticalColors.Win2KTitleNavy))
          else Brush.verticalGradient(listOf(Color(0xFF000080), Color(0xFF000040)))
        Row(
          modifier =
            Modifier
              .padding(start = 3.dp, bottom = 52.dp)
              .width(262.dp)
              .shadow(8.dp, RectangleShape)
              .background(menuBg, RectangleShape)
              .windowsClassicBevel(sunken = false)
              .padding(3.dp),
        ) {
          // Iconic Vertical Windows 95 / 2000 Sidebar Strip
          Box(
            contentAlignment = Alignment.BottomCenter,
            modifier =
              Modifier
                .width(28.dp)
                .height(338.dp)
                .background(sidebarBrush)
                .padding(bottom = 12.dp),
          ) {
            Text(
              text = if (is2k) "ERides 2000" else "ERides 95",
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.rotate(-90f),
            )
          }
          Column(
            modifier = Modifier.weight(1f).padding(vertical = 2.dp),
          ) {
            tabLabels.forEachIndexed { idx, lbl ->
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .clickable { onSelectTab(idx); onDismiss() }
                    .padding(horizontal = 8.dp, vertical = 7.dp),
              ) {
                Icon(tabIcons[idx], contentDescription = null, tint = TacticalColors.Win95TitleNavy, modifier = Modifier.size(16.dp))
                Text("${lbl}.exe", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Black, maxLines = 1)
              }
            }
            HorizontalDivider(thickness = 1.dp, color = TacticalColors.Win95Shadow, modifier = Modifier.padding(vertical = 3.dp))
            Win95MenuRow("Control Panel (Settings)", Icons.Default.Settings) { onOpenSettings(); onDismiss() }
            Win95MenuRow("Switch Theme", Icons.Default.Palette) { onCycleTheme(); onDismiss() }
            Win95MenuRow("Telemetry Studio", Icons.Default.ShowChart) { onOpenTelemetryStudio(); onDismiss() }
            Win95MenuRow("Rider's Prayer", Icons.Default.AutoAwesome) { onOpenPrayer(); onDismiss() }
            HorizontalDivider(thickness = 1.dp, color = TacticalColors.Win95Shadow, modifier = Modifier.padding(vertical = 3.dp))
            Win95MenuRow("Shut Down / Sign Out...", Icons.AutoMirrored.Filled.Logout) { onDismiss(); onSignOut() }
          }
        }
      }
    }
  }
}

@Composable
private fun XpStartRightItem(label: String, icon: ImageVector, onClick: () -> Unit) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp)).clickable(onClick = onClick).padding(horizontal = 5.dp, vertical = 6.dp),
  ) {
    Icon(icon, contentDescription = null, tint = Color(0xFF0A3A8A), modifier = Modifier.size(15.dp))
    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A246A), maxLines = 1, overflow = TextOverflow.Ellipsis)
  }
}

@Composable
private fun Win7StartGlassLink(label: String, onClick: () -> Unit) {
  Text(
    text = label,
    fontSize = 11.sp,
    color = Color(0xFFEBF6FF),
    fontWeight = FontWeight.Medium,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(4.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = 6.dp, vertical = 5.dp),
  )
}

@Composable
private fun Win10ActionTile(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
    modifier = modifier.background(Color(0xFF2B2B2B)).clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 4.dp),
  ) {
    Icon(icon, contentDescription = null, tint = Color(0xFF60CDFF), modifier = Modifier.size(14.dp))
    Spacer(Modifier.width(4.dp))
    Text(label, fontSize = 10.sp, color = Color.White, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
  }
}

@Composable
private fun Win95MenuRow(label: String, icon: ImageVector, onClick: () -> Unit) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp),
  ) {
    Icon(icon, contentDescription = null, tint = Color(0xFF1E293B), modifier = Modifier.size(15.dp))
    Text(label, fontSize = 11.5.sp, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
  }
}

/**
 * Authentic Windows 95 / Windows 2000 Taskbar with 4-Color Flag Start Button, Sunken System Tray Clock & Sound Icon.
 */
@Composable
fun WindowsClassic95Taskbar(
  is2k: Boolean,
  tabLabels: List<String>,
  tabIcons: List<ImageVector>,
  tabTestTags: List<String>,
  activeTabIdx: Int,
  onSelectTab: (Int) -> Unit,
  startMenuOpen: Boolean,
  onToggleStartMenu: () -> Unit,
  showClock: Boolean = true,
  compact: Boolean = false,
  startMenuContent: @Composable () -> Unit,
) {
  val (timeStr, _) = rememberModernTrayClock()
  val barBg = if (is2k) TacticalColors.Win2KPutty else TacticalColors.Win95Silver
  val btnHeight = if (compact) 42.dp else 48.dp

  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .background(barBg, RectangleShape)
        .windowsClassicBevel(sunken = false)
        .navigationBarsPadding()
        .padding(horizontal = 3.dp, vertical = 3.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      // Classic 3D Beveled Start Button with 4-Color Windows Flag
      Box {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier
              .height(btnHeight)
              .background(barBg, RectangleShape)
              .windowsClassicBevel(sunken = startMenuOpen)
              .clickable(onClick = onToggleStartMenu)
              .padding(horizontal = 7.dp)
              .testTag("win95_start_button"),
        ) {
          WindowsFourPaneEmblem(size = 15.dp)
          Spacer(Modifier.width(4.dp))
          Text(
            text = "Start",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = TacticalColors.Win95BlackInk,
            maxLines = 1,
            softWrap = false,
          )
        }
        startMenuContent()
      }

      // 3D Beveled Program Taskbar Buttons
      Row(
        modifier = Modifier.weight(1f),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        tabLabels.forEachIndexed { idx, lbl ->
          val selected = activeTabIdx == idx
          val tabBg =
            if (selected) Color(0xFFDFDFDF)
            else barBg
          val tabFg = if (selected) TacticalColors.Win95TitleNavy else TacticalColors.Win95BlackInk
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier =
              Modifier
                .weight(1f)
                .height(btnHeight)
                .background(tabBg, RectangleShape)
                .windowsClassicBevel(sunken = selected)
                .clickable { onSelectTab(idx) }
                .padding(horizontal = 2.dp)
                .testTag(tabTestTags[idx]),
          ) {
            Icon(tabIcons[idx], contentDescription = lbl, tint = tabFg, modifier = Modifier.size(15.dp))
            Text(
              text = lbl,
              fontSize = 8.5.sp,
              lineHeight = 9.5.sp,
              letterSpacing = (-0.25).sp,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
              color = tabFg,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Clip,
            )
          }
        }
      }

      // Authentic Sunken System Notification Tray with Speaker + Live Clock
      if (showClock) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(3.dp),
          modifier =
            Modifier
              .height(btnHeight)
              .background(barBg, RectangleShape)
              .windowsClassicBevel(sunken = true)
              .padding(horizontal = 5.dp),
        ) {
          Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF222222), modifier = Modifier.size(11.dp))
          Text(
            text = timeStr,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Medium,
            color = TacticalColors.Win95BlackInk,
            maxLines = 1,
            softWrap = false,
          )
        }
      }
    }
  }
}
