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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MasterSettings
import com.example.data.VisualStyleMode
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun isWindowsXpActive(settings: MasterSettings): Boolean =
  settings.visualStyle == VisualStyleMode.WINDOWS_XP &&
    !settings.nightVisionMode &&
    !settings.sunVisibilityMode

fun isWindows7Active(settings: MasterSettings): Boolean =
  settings.visualStyle == VisualStyleMode.WINDOWS_7 &&
    !settings.nightVisionMode &&
    !settings.sunVisibilityMode

@Composable
private fun rememberLiveTrayClock(): String {
  var clockText by remember { mutableStateOf("") }
  LaunchedEffect(Unit) {
    val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
    while (true) {
      clockText = fmt.format(Date())
      delay(15_000L)
    }
  }
  return clockText.ifBlank { SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) }
}

/**
 * Crisp 4-pane colorful desktop quad emblem used inside the Windows XP green Start button
 * and Windows 7 glowing Aero Start Orb.
 */
@Composable
fun WindowsFourPaneEmblem(
  size: Dp = 16.dp,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val gap = w * 0.08f
    val pw = (w - gap) / 2f
    val ph = (h - gap) / 2f
    val r = CornerRadius(w * 0.08f, w * 0.08f)

    // Top-left (Vibrant Orange-Red)
    drawRoundRect(
      brush = Brush.verticalGradient(listOf(Color(0xFFFF6D42), Color(0xFFE03A17))),
      topLeft = Offset(0f, h * 0.04f),
      size = Size(pw, ph),
      cornerRadius = r,
    )
    // Top-right (Vibrant Emerald Green)
    drawRoundRect(
      brush = Brush.verticalGradient(listOf(Color(0xFF9CE848), Color(0xFF53B814))),
      topLeft = Offset(pw + gap, 0f),
      size = Size(pw, ph),
      cornerRadius = r,
    )
    // Bottom-left (Sky Azure Blue)
    drawRoundRect(
      brush = Brush.verticalGradient(listOf(Color(0xFF56C2FF), Color(0xFF1585E8))),
      topLeft = Offset(0f, ph + gap + h * 0.02f),
      size = Size(pw, ph * 0.94f),
      cornerRadius = r,
    )
    // Bottom-right (Warm Solar Yellow)
    drawRoundRect(
      brush = Brush.verticalGradient(listOf(Color(0xFFFFE559), Color(0xFFF3B30B))),
      topLeft = Offset(pw + gap, ph + gap - h * 0.02f),
      size = Size(pw, ph * 0.94f),
      cornerRadius = r,
    )
  }
}

// ============================================================================
// AUTHENTIC WINDOWS XP LUNA BLUE CHROME (TITLE BAR, WINDOW CARD, BUTTONS)
// ============================================================================

@Composable
fun WindowsXpTitleBar(
  title: String,
  icon: ImageVector? = null,
  trailingBadge: String? = null,
  active: Boolean = true,
) {
  val titleBarBrush =
    if (active) {
      Brush.verticalGradient(
        colorStops =
          arrayOf(
            0.00f to Color(0xFF4CA0FF),
            0.12f to Color(0xFF1B73F5),
            0.35f to Color(0xFF0055EA),
            0.78f to Color(0xFF0045D6),
            0.93f to Color(0xFF0037B8),
            1.00f to Color(0xFF0A4ED1),
          )
      )
    } else {
      Brush.verticalGradient(listOf(Color(0xFF9BB4E4), Color(0xFF7B98D1), Color(0xFF6A87C4)))
    }
  val shape = RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp)
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .background(titleBarBrush, shape)
        .padding(horizontal = 8.dp, vertical = 5.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      if (icon != null) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
      } else {
        WindowsFourPaneEmblem(size = 13.dp)
        Spacer(Modifier.width(5.dp))
      }
      Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        style =
          MaterialTheme.typography.labelMedium.copy(
            shadow = Shadow(color = Color(0xAA002375), offset = Offset(1f, 1.5f), blurRadius = 1.5f)
          ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
      if (!trailingBadge.isNullOrBlank()) {
        Box(
          modifier =
            Modifier
              .background(Color(0xFF3E9D31), RoundedCornerShape(3.dp))
              .border(0.8.dp, Color(0xCCFFFFFF), RoundedCornerShape(3.dp))
              .padding(horizontal = 5.dp, vertical = 1.dp),
        ) {
          Text(trailingBadge, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
        }
      }
      listOf("_", "□", "×").forEach { glyph ->
        val isClose = glyph == "×"
        val btnBrush =
          if (isClose) {
            Brush.verticalGradient(
              listOf(Color(0xFFFF9B82), Color(0xFFE74F33), Color(0xFFCC2C14), Color(0xFFDF4126))
            )
          } else {
            Brush.verticalGradient(
              listOf(Color(0xFF7BB8FF), Color(0xFF2874F0), Color(0xFF1656D6), Color(0xFF246CE6))
            )
          }
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier
              .size(19.dp)
              .clip(RoundedCornerShape(3.dp))
              .background(btnBrush)
              .border(1.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(3.dp)),
        ) {
          Text(
            text = glyph,
            fontSize = 11.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
          )
        }
      }
    }
  }
}

@Composable
fun WindowsXpWindowCard(
  modifier: Modifier = Modifier,
  highlight: Boolean = false,
  title: String? = null,
  showTitleBar: Boolean = true,
  content: @Composable ColumnScope.() -> Unit,
) {
  val outerShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 2.dp, bottomEnd = 2.dp)
  val hasExplicitTitle = !title.isNullOrBlank() && showTitleBar
  Column(
    modifier =
      modifier
        .shadow(elevation = if (highlight) 6.dp else 3.dp, shape = outerShape)
        .clip(outerShape)
        .background(
          Brush.verticalGradient(listOf(Color(0xFF0055EA), Color(0xFF0047CF), Color(0xFF003BB3))),
          outerShape,
        )
        .border(1.dp, Color(0xFF003299), outerShape)
        .padding(start = 3.dp, end = 3.dp, bottom = 3.dp, top = if (hasExplicitTitle) 0.dp else 3.dp),
  ) {
    if (hasExplicitTitle) {
      WindowsXpTitleBar(
        title = title!!,
        trailingBadge = if (highlight) "LIVE" else null,
        active = true,
      )
      if (com.example.ui.theme.LocalTacticalSettings.current.winShowMenuBar) {
        ClassicWindowsMenuStrip(isXp = true)
      }
    } else if (highlight) {
      // Subtle glossy Luna accent bar for highlighted cards without a window title
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(
              Brush.verticalGradient(listOf(Color(0xFF5CA8FF), Color(0xFF0055EA), Color(0xFF003BB3))),
              RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp),
            )
      )
    }
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(Color(0xFFECE9D8))
          .border(1.dp, Color(0xFFACA899))
          .padding(12.dp),
      content = content,
    )
  }
}

@Composable
fun WindowsXpStyledButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  isPrimary: Boolean = true,
  isDestructive: Boolean = false,
  contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
  content: @Composable RowScope.() -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val shape = RoundedCornerShape(4.dp)
  val bgBrush =
    when {
      !enabled -> Brush.verticalGradient(listOf(Color(0xFFF5F4EA), Color(0xFFE5E3D5)))
      isDestructive ->
        if (isPressed) Brush.verticalGradient(listOf(Color(0xFFB3240F), Color(0xFFD63B20)))
        else Brush.verticalGradient(listOf(Color(0xFFF68265), Color(0xFFDD4326), Color(0xFFC22A14)))
      isPressed ->
        Brush.verticalGradient(listOf(Color(0xFFDCD9CA), Color(0xFFE9E6D9), Color(0xFFF2F0E6)))
      else ->
        Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF7F6F1), Color(0xFFEBE8DC), Color(0xFFDFDCCD)))
    }
  val outerBorder =
    when {
      !enabled -> Color(0xFFC9C7BA)
      isDestructive -> Color(0xFF8C1C0A)
      else -> Color(0xFF003C74)
    }
  val innerGlow =
    when {
      !enabled -> Color.Transparent
      isDestructive -> Color(0x66FFFFFF)
      isPrimary -> Color(0xFF69A8EB) // Classic XP default button blue inner rim
      else -> Color(0x88F6B94D) // Classic XP warm amber accent rim
    }
  val fgColor =
    when {
      !enabled -> Color(0xFF9E9B8E)
      isDestructive -> Color.White
      else -> Color(0xFF000000)
    }

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
        .clip(shape)
        .background(bgBrush, shape)
        .border(1.2.dp, outerBorder, shape)
        .padding(1.5.dp)
        .border(1.2.dp, innerGlow, RoundedCornerShape(2.5.dp))
        .clickable(
          enabled = enabled,
          interactionSource = interactionSource,
          indication = null,
          onClick = onClick,
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

// ============================================================================
// AUTHENTIC WINDOWS 7 AERO GLASS CHROME (TITLE BAR, GLASS WINDOW, GLOSS BUTTONS)
// ============================================================================

@Composable
fun Windows7AeroTitleBar(
  title: String,
  icon: ImageVector? = null,
  trailingBadge: String? = null,
  active: Boolean = true,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(start = 8.dp, end = 6.dp, top = 3.dp, bottom = 5.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      if (icon != null) {
        Icon(icon, contentDescription = null, tint = Color(0xFF0B2948), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
      } else {
        WindowsFourPaneEmblem(size = 13.dp)
        Spacer(Modifier.width(5.dp))
      }
      // Authentic Windows 7 white text glow halo behind crisp black title text
      Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (active) Color(0xFF061626) else Color(0xFF334B63),
        style =
          MaterialTheme.typography.labelMedium.copy(
            shadow = Shadow(color = Color(0xEEFFFFFF), offset = Offset(0f, 0f), blurRadius = 8f)
          ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      if (!trailingBadge.isNullOrBlank()) {
        Box(
          modifier =
            Modifier
              .background(Color(0xCC1E7AD8), RoundedCornerShape(3.dp))
              .border(0.8.dp, Color(0xCCFFFFFF), RoundedCornerShape(3.dp))
              .padding(horizontal = 5.dp, vertical = 1.dp),
        ) {
          Text(trailingBadge, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
        }
      }
      // Iconic Windows 7 joined 3-button Aero glass caption cluster
      Row(
        modifier =
          Modifier
            .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp, topStart = 2.dp, topEnd = 2.dp))
            .border(
              0.9.dp,
              Color(0x99183452),
              RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp, topStart = 2.dp, topEnd = 2.dp),
            ),
      ) {
        listOf("_", "□", "×").forEachIndexed { i, glyph ->
          val isClose = glyph == "×"
          val splitBrush =
            if (isClose) {
              Brush.verticalGradient(
                colorStops =
                  arrayOf(
                    0.00f to Color(0xFFF29688),
                    0.48f to Color(0xFFE05C4B),
                    0.50f to Color(0xFFC42B1C),
                    1.00f to Color(0xFFD64B38),
                  )
              )
            } else {
              Brush.verticalGradient(
                colorStops =
                  arrayOf(
                    0.00f to Color(0xAAFFFFFF),
                    0.48f to Color(0x66DCEEFF),
                    0.50f to Color(0x557CAAD4),
                    1.00f to Color(0x77A6D2F7),
                  )
              )
            }
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier
                .size(width = if (isClose) 30.dp else 21.dp, height = 18.dp)
                .background(splitBrush)
                .then(
                  if (i < 2) Modifier.drawBehind {
                    drawLine(Color(0x88203F60), Offset(size.width - 0.5f, 0f), Offset(size.width - 0.5f, size.height), strokeWidth = 1f)
                  } else Modifier
                ),
          ) {
            Text(
              text = glyph,
              fontSize = 10.sp,
              lineHeight = 10.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              style =
                MaterialTheme.typography.labelSmall.copy(
                  shadow = Shadow(color = Color(0xAA000000), offset = Offset(0f, 1f), blurRadius = 2f)
                ),
            )
          }
        }
      }
    }
  }
}

@Composable
fun Windows7AeroWindowCard(
  modifier: Modifier = Modifier,
  highlight: Boolean = false,
  title: String? = null,
  showTitleBar: Boolean = true,
  content: @Composable ColumnScope.() -> Unit,
) {
  val outerShape = RoundedCornerShape(9.dp)
  val innerShape = RoundedCornerShape(3.dp)
  val hasExplicitTitle = !title.isNullOrBlank() && showTitleBar
  val glassAlpha = com.example.ui.theme.LocalTacticalSettings.current.winGlassIntensity.coerceIn(0.45f, 1.0f)

  Box(
    modifier =
      modifier
        .shadow(elevation = if (highlight) 10.dp else 6.dp, shape = outerShape)
        .clip(outerShape)
        .background(
          Brush.verticalGradient(
            listOf(
              Color(0xFFB8DDFB).copy(alpha = (0.86f * glassAlpha).coerceIn(0.42f, 0.94f)),
              Color(0xFF86BCEB).copy(alpha = (0.80f * glassAlpha).coerceIn(0.38f, 0.90f)),
              Color(0xFF6BA6DC).copy(alpha = (0.82f * glassAlpha).coerceIn(0.40f, 0.92f)),
              Color(0xFF9ACAF4).copy(alpha = (0.86f * glassAlpha).coerceIn(0.42f, 0.94f)),
            )
          ),
          outerShape,
        )
        // Signature Windows 7 diagonal translucent white glass sheen streaks
        .drawBehind {
          val w = size.width
          val h = size.height
          val streak1 =
            Path().apply {
              moveTo(w * 0.08f, 0f)
              lineTo(w * 0.28f, 0f)
              lineTo(w * 0.08f, h)
              lineTo(-w * 0.06f, h)
              close()
            }
          val streak2 =
            Path().apply {
              moveTo(w * 0.36f, 0f)
              lineTo(w * 0.44f, 0f)
              lineTo(w * 0.24f, h)
              lineTo(w * 0.16f, h)
              close()
            }
          val streak3 =
            Path().apply {
              moveTo(w * 0.68f, 0f)
              lineTo(w * 0.85f, 0f)
              lineTo(w * 0.62f, h)
              lineTo(w * 0.46f, h)
              close()
            }
          drawPath(streak1, Color(0x36FFFFFF))
          drawPath(streak2, Color(0x26FFFFFF))
          drawPath(streak3, Color(0x20FFFFFF))
        }
        .border(1.dp, Color(0xBB183A5E), outerShape)
        .padding(1.dp)
        .border(1.dp, Color(0xCCFFFFFF), RoundedCornerShape(8.dp))
        .padding(start = 5.dp, end = 5.dp, bottom = 5.dp, top = if (hasExplicitTitle) 1.dp else 5.dp),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      if (hasExplicitTitle) {
        Windows7AeroTitleBar(
          title = title!!,
          trailingBadge = if (highlight) "LIVE" else null,
          active = true,
        )
      }
      // Crisp bright white client interior separated from frosted Aero glass bezel
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .clip(innerShape)
            .background(Color(0xFAFFFFFF))
            .border(1.dp, Color(0x99355678), innerShape)
            .padding(12.dp),
        content = content,
      )
    }
  }
}

@Composable
fun Windows7AeroStyledButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  isPrimary: Boolean = true,
  isDestructive: Boolean = false,
  contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
  content: @Composable RowScope.() -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val shape = RoundedCornerShape(4.dp)

  // Signature Windows 7 50/50 split glossy glass button gradient
  val bgBrush =
    when {
      !enabled -> Brush.verticalGradient(listOf(Color(0xFFF4F4F4), Color(0xFFEAEAEA)))
      isDestructive ->
        Brush.verticalGradient(
          colorStops =
            arrayOf(
              0.00f to Color(0xFFF5A396),
              0.48f to Color(0xFFE36452),
              0.50f to Color(0xFFC42D1E),
              1.00f to Color(0xFFD94A38),
            )
        )
      isPressed ->
        Brush.verticalGradient(
          colorStops =
            arrayOf(
              0.00f to Color(0xFFE5F4FD),
              0.48f to Color(0xFFC4E5F6),
              0.50f to Color(0xFF98D1EF),
              1.00f to Color(0xFF68B3DB),
            )
        )
      isPrimary ->
        Brush.verticalGradient(
          colorStops =
            arrayOf(
              0.00f to Color(0xFFFAFDFF),
              0.47f to Color(0xFFEBF4FC),
              0.50f to Color(0xFFD2E6F7),
              1.00f to Color(0xFFBBD9F2),
            )
        )
      else ->
        Brush.verticalGradient(
          colorStops =
            arrayOf(
              0.00f to Color(0xFFF8FAFC),
              0.47f to Color(0xFFEBEFF4),
              0.50f to Color(0xFFDCE2EA),
              1.00f to Color(0xFFCFD6E0),
            )
        )
    }

  val borderCol =
    when {
      !enabled -> Color(0xFFADB2B5)
      isDestructive -> Color(0xFF8C1C10)
      isPressed || isPrimary -> Color(0xFF3C7FB1) // Authentic Win7 Aero sky-blue button border
      else -> Color(0xFF707070)
    }

  val fgColor =
    when {
      !enabled -> Color(0xFF838383)
      isDestructive -> Color.White
      else -> Color(0xFF0A1420)
    }

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
        .clip(shape)
        .background(bgBrush, shape)
        .border(1.dp, borderCol, shape)
        .padding(1.dp)
        .border(0.9.dp, if (isPrimary && enabled) Color(0xCCBEE6FD) else Color(0xAAFFFFFF), RoundedCornerShape(3.dp))
        .clickable(
          enabled = enabled,
          interactionSource = interactionSource,
          indication = null,
          onClick = onClick,
        )
        .padding(contentPadding),
  ) {
    CompositionLocalProvider(LocalContentColor provides fgColor) {
      ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium, color = fgColor)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
          content = content,
        )
      }
    }
  }
}

// ============================================================================
// AUTHENTIC WINDOWS XP LUNA TASKBAR & WINDOWS 7 AERO SUPERBAR
// ============================================================================

@Composable
fun WindowsXpLunaTaskbar(
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
  val liveClock = rememberLiveTrayClock()
  val barHeight = if (compact) 44.dp else 50.dp
  val barBrush =
    Brush.verticalGradient(
      colorStops =
        arrayOf(
          0.00f to Color(0xFF56A0F0),
          0.09f to Color(0xFF327BE2),
          0.22f to Color(0xFF225CD8),
          0.85f to Color(0xFF1941A5),
          1.00f to Color(0xFF1F54C8),
        )
    )
  val startBtnBrush =
    if (startMenuOpen) {
      Brush.verticalGradient(listOf(Color(0xFF28751E), Color(0xFF39942D), Color(0xFF47AA39)))
    } else {
      Brush.verticalGradient(
        colorStops =
          arrayOf(
            0.00f to Color(0xFF8BE07B),
            0.14f to Color(0xFF54B647),
            0.48f to Color(0xFF3C9C30),
            0.88f to Color(0xFF29781F),
            1.00f to Color(0xFF46A839),
          )
      )
    }
  val trayBrush =
    Brush.verticalGradient(
      colorStops =
        arrayOf(
          0.00f to Color(0xFF47BAFA),
          0.12f to Color(0xFF1697EA),
          0.85f to Color(0xFF0E7AD1),
          1.00f to Color(0xFF1898ED),
        )
    )

  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .background(barBrush)
        .navigationBarsPadding(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().height(barHeight),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Authentic Windows XP Curved Emerald Green "start" Button
      Box {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier
              .fillMaxHeight()
              .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
              .background(startBtnBrush)
              .border(
                width = 1.dp,
                color = Color(0x881D5216),
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
              )
              .clickable(onClick = onToggleStartMenu)
              .padding(start = 8.dp, end = 12.dp)
              .testTag("win95_start_button"),
        ) {
          WindowsFourPaneEmblem(size = 16.dp)
          Spacer(Modifier.width(5.dp))
          Text(
            text = "start",
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            style =
              MaterialTheme.typography.labelLarge.copy(
                shadow = Shadow(color = Color(0xAA103A0A), offset = Offset(1f, 1.5f), blurRadius = 2f)
              ),
            maxLines = 1,
            softWrap = false,
          )
        }
        startMenuContent()
      }

      Spacer(Modifier.width(4.dp))

      // XP Glossy Blue Rounded Program Buttons
      Row(
        modifier = Modifier.weight(1f).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        tabLabels.forEachIndexed { idx, lbl ->
          val selected = idx == activeTabIdx
          val tabBrush =
            if (selected) {
              Brush.verticalGradient(listOf(Color(0xFF173D96), Color(0xFF1B4AB3), Color(0xFF163D94)))
            } else {
              Brush.verticalGradient(listOf(Color(0xFF579CF7), Color(0xFF3C81F3), Color(0xFF286BE0)))
            }
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier =
              Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(tabBrush)
                .border(
                  0.8.dp,
                  if (selected) Color(0xFF0E2B6E) else Color(0x9989BFFF),
                  RoundedCornerShape(4.dp),
                )
                .clickable { onSelectTab(idx) }
                .padding(horizontal = 2.dp)
                .testTag(tabTestTags[idx]),
          ) {
            Icon(tabIcons[idx], contentDescription = lbl, tint = Color.White, modifier = Modifier.size(15.dp))
            Text(
              text = lbl,
              fontSize = 8.5.sp,
              lineHeight = 9.5.sp,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
              color = Color.White,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Clip,
            )
          }
        }
      }

      Spacer(Modifier.width(3.dp))

      // Authentic Light-Cyan XP System Notification Tray with Live Clock
      if (showClock) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier
              .fillMaxHeight()
              .background(trayBrush)
              .drawBehind {
                drawLine(Color(0xFF0A4DA2), Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 1.5f)
                drawLine(Color(0x8884D4FF), Offset(1.5f, 0f), Offset(1.5f, size.height), strokeWidth = 1.5f)
              }
              .padding(horizontal = 6.dp),
        ) {
          Text(
            text = liveClock,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            softWrap = false,
          )
        }
      }
    }
  }
}

@Composable
fun Windows7AeroTaskbar(
  tabLabels: List<String>,
  tabIcons: List<ImageVector>,
  tabTestTags: List<String>,
  activeTabIdx: Int,
  onSelectTab: (Int) -> Unit,
  startMenuOpen: Boolean,
  onToggleStartMenu: () -> Unit,
  onAeroPeekHome: () -> Unit,
  showClock: Boolean = true,
  compact: Boolean = false,
  startMenuContent: @Composable () -> Unit,
) {
  val liveClock = rememberLiveTrayClock()
  val barHeight = if (compact) 46.dp else 52.dp
  val barBrush =
    Brush.verticalGradient(
      colorStops =
        arrayOf(
          0.00f to Color(0xD8275A8F),
          0.16f to Color(0xD2153E6B),
          0.52f to Color(0xD80D2948),
          1.00f to Color(0xE60B223C),
        )
    )

  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .background(barBrush)
        .drawBehind {
          // Crisp dark top edge + glowing white glass inner top rim + diagonal Aero gloss
          drawLine(Color(0xCC061322), Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.2f)
          drawLine(Color(0x88FFFFFF), Offset(0f, 1.5f), Offset(size.width, 1.5f), strokeWidth = 1.2f)
          val sheen =
            Path().apply {
              moveTo(size.width * 0.14f, 0f)
              lineTo(size.width * 0.34f, 0f)
              lineTo(size.width * 0.24f, size.height)
              lineTo(size.width * 0.04f, size.height)
              close()
            }
          drawPath(sheen, Color(0x1EFFFFFF))
        }
        .navigationBarsPadding(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().height(barHeight).padding(start = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      // Iconic Circular Glowing Windows 7 Start Orb
      Box {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(
                Brush.radialGradient(
                  colors =
                    if (startMenuOpen) {
                      listOf(Color(0xFF74ECFF), Color(0xFF1F92E8), Color(0xFF0A4380), Color(0xFF052142))
                    } else {
                      listOf(Color(0xFF4AD9FF), Color(0xFF1674C8), Color(0xFF0B376B), Color(0xFF061E3D))
                    },
                  center = Offset(55f, 88f),
                  radius = 90f,
                )
              )
              .drawBehind {
                // Top curved white specular glass dome highlight
                drawOval(
                  brush = Brush.verticalGradient(listOf(Color(0xBBFFFFFF), Color(0x11FFFFFF))),
                  topLeft = Offset(size.width * 0.12f, size.height * 0.06f),
                  size = Size(size.width * 0.76f, size.height * 0.42f),
                )
                // Cyan bottom glow arc
                drawCircle(
                  brush = Brush.verticalGradient(listOf(Color.Transparent, Color(0x8838E8FF))),
                  radius = size.minDimension * 0.48f,
                )
              }
              .border(1.3.dp, Color(0xCC98D9FF), CircleShape)
              .clickable(onClick = onToggleStartMenu)
              .testTag("win95_start_button"),
        ) {
          WindowsFourPaneEmblem(size = 19.dp)
        }
        startMenuContent()
      }

      // Windows 7 Aero Superbar Frosted Glass App Tiles
      Row(
        modifier = Modifier.weight(1f).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        tabLabels.forEachIndexed { idx, lbl ->
          val selected = idx == activeTabIdx
          val tileBrush =
            if (selected) {
              Brush.verticalGradient(
                colorStops =
                  arrayOf(
                    0.00f to Color(0x88FFFFFF),
                    0.48f to Color(0x44AEE4FF),
                    0.50f to Color(0x33409CE0),
                    1.00f to Color(0x6658D8FF),
                  )
              )
            } else {
              Brush.verticalGradient(listOf(Color(0x28FFFFFF), Color(0x10FFFFFF)))
            }
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier =
              Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(tileBrush)
                .border(
                  width = if (selected) 1.dp else 0.6.dp,
                  color = if (selected) Color(0xCCFFFFFF) else Color(0x44FFFFFF),
                  shape = RoundedCornerShape(4.dp),
                )
                .clickable { onSelectTab(idx) }
                .padding(horizontal = 2.dp)
                .testTag(tabTestTags[idx]),
          ) {
            Icon(
              tabIcons[idx],
              contentDescription = lbl,
              tint = Color.White,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = lbl,
              fontSize = 8.5.sp,
              lineHeight = 9.5.sp,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
              color = Color.White,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Clip,
            )
          }
        }
      }

      // Live Tray Clock
      if (showClock) {
        Text(
          text = liveClock,
          fontSize = 9.sp,
          fontWeight = FontWeight.Medium,
          color = Color.White,
          maxLines = 1,
          softWrap = false,
          modifier = Modifier.padding(horizontal = 3.dp),
        )
      }

      // Signature Windows 7 Far-Right Translucent Aero Peek "Show Desktop" Glass Sliver
      Box(
        modifier =
          Modifier
            .width(12.dp)
            .fillMaxHeight()
            .background(
              Brush.verticalGradient(
                listOf(Color(0x66FFFFFF), Color(0x22A8D8FF), Color(0x44FFFFFF))
              )
            )
            .border(0.8.dp, Color(0x99FFFFFF))
            .clickable(onClick = onAeroPeekHome)
            .testTag("win7_aero_peek_button"),
      )
    }
  }
}
