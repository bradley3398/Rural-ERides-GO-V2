package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MasterSettings
import com.example.data.VisualStyleMode
import com.example.ui.theme.LocalTacticalChrome
import com.example.ui.theme.LocalTacticalSettings
import com.example.ui.theme.TacticalChromeFamily
import com.example.ui.theme.TacticalColors

data class TacticalNavTabItem(
  val label: String,
  val icon: ImageVector,
  val testTag: String,
)

data class NavBarStyleOption(
  val id: String,
  val label: String,
  val badge: String,
)

val ALL_NAV_BAR_STYLES: List<NavBarStyleOption> =
  listOf(
    NavBarStyleOption("pill", "Bottom Bar", "HUD"),
    NavBarStyleOption("rural_timber", "Rural Timber", "WOOD"),
    NavBarStyleOption("dock", "Floating Dock", "FLOAT"),
    NavBarStyleOption("compact_capsule", "Icon Capsule", "MINI"),
    NavBarStyleOption("top_ribbon", "Top Bar", "TOP"),
    NavBarStyleOption("cyber_deck", "Cyber Deck", "LIVE"),
    NavBarStyleOption("neon_underbar", "Neon Underbar", "GLOW"),
    NavBarStyleOption("split_command", "Split AI Dock", "AI+DOCK"),
    NavBarStyleOption("brutal_strip", "High-Vis Strip", "SUN"),
    NavBarStyleOption("rail", "Left Side", "RAIL L"),
    NavBarStyleOption("right_rail", "Right Side", "RAIL R"),
    NavBarStyleOption("classic_taskbar", "Classic Taskbar", "WIN OS"),
  )

fun navBarMiniPlayerBottomOffset(
  styleKey: String,
  useLeftRail: Boolean,
  useRightRail: Boolean,
  useTopRibbon: Boolean,
  useClassicTaskbar: Boolean,
): Dp =
  when {
    useLeftRail || useRightRail || useTopRibbon -> 8.dp
    useClassicTaskbar -> 60.dp
    styleKey == "compact_capsule" -> 68.dp
    styleKey == "dock" -> 76.dp
    styleKey == "split_command" -> 78.dp
    styleKey == "cyber_deck" -> 92.dp
    styleKey == "neon_underbar" -> 72.dp
    styleKey == "brutal_strip" -> 76.dp
    styleKey == "rural_timber" -> 74.dp
    else -> 74.dp
  }

@Composable
fun TacticalTopRibbonBar(
  tabs: List<TacticalNavTabItem>,
  activeTabIdx: Int,
  onSelectTab: (Int) -> Unit,
) {
  val chrome = LocalTacticalChrome.current
  val cs = MaterialTheme.colorScheme
  val borderBrush =
    if (chrome.hasGradientBorder) Brush.horizontalGradient(listOf(cs.primary, cs.secondary, cs.tertiary))
    else Brush.horizontalGradient(listOf(cs.primary.copy(alpha = 0.55f), cs.outline.copy(alpha = 0.45f)))

  Surface(
    color = if (chrome.family == TacticalChromeFamily.OLED_PURE) Color(0xFF000000) else cs.surface.copy(alpha = 0.97f),
    border = BorderStroke(chrome.cardBorderWidth.coerceAtLeast(1.dp), borderBrush),
    shadowElevation = 6.dp,
    modifier = Modifier.fillMaxWidth().statusBarsPadding().testTag("top_ribbon_nav_bar"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(3.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      tabs.forEachIndexed { idx, tab ->
        val selected = activeTabIdx == idx
        val itemShape: Shape =
          when (chrome.family) {
            TacticalChromeFamily.BRUTALIST -> RectangleShape
            TacticalChromeFamily.CYBER_HUD, TacticalChromeFamily.VAPORWAVE, TacticalChromeFamily.OLED_PURE -> CutCornerShape(6.dp)
            else -> chrome.smallShape
          }
        val itemBg =
          when {
            selected && chrome.family == TacticalChromeFamily.OLED_PURE -> cs.primary.copy(alpha = 0.22f)
            selected -> cs.primary
            else -> cs.surfaceVariant.copy(alpha = 0.45f)
          }
        val fg =
          when {
            selected && chrome.family == TacticalChromeFamily.OLED_PURE -> cs.primary
            selected -> cs.onPrimary
            else -> cs.onSurfaceVariant
          }
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .clip(itemShape)
              .background(itemBg, itemShape)
              .border(
                width = if (selected) 1.2.dp else 0.7.dp,
                color = if (selected) cs.primary else cs.outline.copy(alpha = 0.30f),
                shape = itemShape,
              )
              .clickable { onSelectTab(idx) }
              .padding(horizontal = 2.dp, vertical = 4.dp)
              .testTag(tab.testTag),
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(tab.icon, contentDescription = tab.label, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(1.dp))
            Text(
              text = tab.label,
              fontSize = 8.5.sp,
              lineHeight = 10.sp,
              letterSpacing = (-0.4).sp,
              fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
              color = fg,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@Composable
fun TacticalSideNavigationRail(
  tabs: List<TacticalNavTabItem>,
  activeTabIdx: Int,
  isRightSide: Boolean,
  onSelectTab: (Int) -> Unit,
) {
  val chrome = LocalTacticalChrome.current
  val cs = MaterialTheme.colorScheme
  val railBg = if (chrome.family == TacticalChromeFamily.OLED_PURE) Color(0xFF000000) else cs.surface.copy(alpha = 0.96f)

  NavigationRail(
    containerColor = railBg,
    contentColor = cs.onSurface,
    modifier =
      Modifier
        .then(if (isRightSide) Modifier.testTag("right_ergonomic_nav_rail") else Modifier.testTag("left_tactical_nav_rail"))
        .drawBehind {
          val x = if (isRightSide) 0f else size.width - 1.5.dp.toPx()
          drawLine(
            color = cs.primary.copy(alpha = 0.45f),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1.5.dp.toPx(),
          )
        },
  ) {
    Spacer(Modifier.height(8.dp))
    tabs.forEachIndexed { idx, tab ->
      val selected = activeTabIdx == idx
      val shape = chrome.smallShape
      val bg =
        when {
          selected && chrome.family == TacticalChromeFamily.OLED_PURE -> cs.primary.copy(alpha = 0.22f)
          selected -> cs.primaryContainer
          else -> Color.Transparent
        }
      val fg = if (selected) cs.primary else cs.onSurfaceVariant
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .width(68.dp)
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(bg, shape)
            .then(
              if (selected) Modifier.border(1.2.dp, cs.primary.copy(alpha = 0.78f), shape)
              else Modifier
            )
            .clickable { onSelectTab(idx) }
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag(tab.testTag),
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
          Icon(tab.icon, contentDescription = tab.label, tint = fg, modifier = Modifier.size(20.dp))
          Spacer(Modifier.height(2.dp))
          Text(
            text = tab.label,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            letterSpacing = (-0.3).sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
            color = fg,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
fun TacticalBottomNavigationHost(
  styleKey: String,
  tabs: List<TacticalNavTabItem>,
  activeTabIdx: Int,
  telemetryTicker: String,
  copilotTabIdx: Int = 3,
  onSelectTab: (Int) -> Unit,
) {
  val chrome = LocalTacticalChrome.current
  val settings = LocalTacticalSettings.current
  val cs = MaterialTheme.colorScheme

  when (styleKey) {
    "rural_timber" -> {
      val containerShape = if (chrome.family == TacticalChromeFamily.BRUTALIST) RectangleShape else RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
      Surface(
        color = cs.surface.copy(alpha = 0.98f),
        shape = containerShape,
        border = BorderStroke(1.6.dp, Brush.horizontalGradient(listOf(cs.primary, cs.tertiary, cs.primary))),
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth().testTag("rural_timber_nav_bar"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 5.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(3.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          tabs.forEachIndexed { idx, tab ->
            val selected = activeTabIdx == idx
            val plankShape = RoundedCornerShape(9.dp)
            Box(
              contentAlignment = Alignment.Center,
              modifier =
                Modifier
                  .weight(1f)
                  .height(54.dp)
                  .clip(plankShape)
                  .background(
                    if (selected) Brush.verticalGradient(listOf(cs.primaryContainer.copy(alpha = 0.95f), cs.primary.copy(alpha = 0.20f)))
                    else Brush.verticalGradient(listOf(cs.surfaceVariant.copy(alpha = 0.45f), cs.surfaceVariant.copy(alpha = 0.25f))),
                    plankShape,
                  )
                  .border(
                    width = if (selected) 1.5.dp else 0.7.dp,
                    color = if (selected) cs.primary else cs.outline.copy(alpha = 0.35f),
                    shape = plankShape,
                  )
                  .clickable { onSelectTab(idx) }
                  .padding(horizontal = 2.dp)
                  .testTag(tab.testTag),
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(
                  tab.icon,
                  contentDescription = tab.label,
                  tint = if (selected) cs.primary else cs.onSurfaceVariant,
                  modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                  text = tab.label,
                  fontSize = 8.5.sp,
                  lineHeight = 10.sp,
                  letterSpacing = (-0.4).sp,
                  fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                  color = if (selected) cs.primary else cs.onSurfaceVariant,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis,
                )
                if (selected) {
                  Spacer(Modifier.height(2.dp))
                  Box(
                    Modifier
                      .width(20.dp)
                      .height(3.dp)
                      .background(Brush.horizontalGradient(listOf(cs.primary, cs.tertiary)), RoundedCornerShape(2.dp))
                  )
                }
              }
            }
          }
        }
      }
    }

    "compact_capsule" -> {
      val capsuleShape =
        when (chrome.family) {
          TacticalChromeFamily.BRUTALIST -> RectangleShape
          TacticalChromeFamily.CYBER_HUD, TacticalChromeFamily.VAPORWAVE, TacticalChromeFamily.OLED_PURE -> CutCornerShape(16.dp)
          else -> RoundedCornerShape(30.dp)
        }
      val itemShape =
        when (chrome.family) {
          TacticalChromeFamily.BRUTALIST -> RectangleShape
          TacticalChromeFamily.CYBER_HUD, TacticalChromeFamily.VAPORWAVE, TacticalChromeFamily.OLED_PURE -> CutCornerShape(10.dp)
          else -> CircleShape
        }
      Box(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
      ) {
        Surface(
          color = if (chrome.family == TacticalChromeFamily.OLED_PURE) Color(0xFF000000) else cs.surface.copy(alpha = 0.96f),
          shape = capsuleShape,
          border = BorderStroke(chrome.cardBorderWidth.coerceAtLeast(1.2.dp), cs.primary.copy(alpha = 0.65f)),
          shadowElevation = 12.dp,
          modifier = Modifier.testTag("compact_capsule_nav_bar"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            tabs.forEachIndexed { idx, tab ->
              val selected = activeTabIdx == idx
              Box(
                contentAlignment = Alignment.Center,
                modifier =
                  Modifier
                    .size(48.dp)
                    .clip(itemShape)
                    .background(if (selected) cs.primary else Color.Transparent, itemShape)
                    .clickable { onSelectTab(idx) }
                    .testTag(tab.testTag),
              ) {
                Icon(
                  tab.icon,
                  contentDescription = tab.label,
                  tint = if (selected) cs.onPrimary else cs.onSurfaceVariant,
                  modifier = Modifier.size(21.dp),
                )
              }
            }
          }
        }
      }
    }

    "dock" -> {
      val dockShape =
        when (chrome.family) {
          TacticalChromeFamily.BRUTALIST -> RectangleShape
          TacticalChromeFamily.CYBER_HUD, TacticalChromeFamily.VAPORWAVE, TacticalChromeFamily.OLED_PURE -> CutCornerShape(14.dp)
          else -> RoundedCornerShape(24.dp)
        }
      val borderBrush =
        if (chrome.hasGradientBorder) Brush.horizontalGradient(listOf(cs.primary, cs.secondary, cs.tertiary))
        else Brush.horizontalGradient(listOf(cs.primary.copy(alpha = 0.6f), cs.outline.copy(alpha = 0.45f)))
      Box(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
      ) {
        Surface(
          color = if (chrome.family == TacticalChromeFamily.OLED_PURE) Color(0xFF000000) else cs.surface.copy(alpha = 0.96f),
          shape = dockShape,
          border = BorderStroke(chrome.cardBorderWidth.coerceAtLeast(1.dp), borderBrush),
          shadowElevation = 12.dp,
          modifier = Modifier.fillMaxWidth().testTag("floating_dock_nav_bar"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            tabs.forEachIndexed { idx, tab ->
              val selected = activeTabIdx == idx
              val pillShape = chrome.smallShape
              val pillBg =
                when {
                  selected && chrome.family == TacticalChromeFamily.OLED_PURE -> cs.primary.copy(alpha = 0.2f)
                  selected -> cs.primaryContainer
                  else -> Color.Transparent
                }
              val fg = if (selected) cs.primary else cs.onSurfaceVariant
              Box(
                contentAlignment = Alignment.Center,
                modifier =
                  Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(pillShape)
                    .background(pillBg, pillShape)
                    .then(if (selected) Modifier.border(1.dp, cs.primary.copy(alpha = 0.5f), pillShape) else Modifier)
                    .clickable { onSelectTab(idx) }
                    .padding(horizontal = 2.dp)
                    .testTag(tab.testTag),
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                  Icon(tab.icon, contentDescription = tab.label, tint = fg, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.height(1.dp))
                  Text(
                    text = tab.label,
                    fontSize = 8.5.sp,
                    lineHeight = 10.sp,
                    letterSpacing = (-0.4).sp,
                    fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                    color = fg,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        }
      }
    }

    "cyber_deck" -> {
      val deckShape = CutCornerShape(topStart = 14.dp, topEnd = 14.dp)
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .shadow(14.dp, deckShape)
            .clip(deckShape)
            .background(Color(0xF5050A14))
            .border(1.3.dp, Brush.horizontalGradient(listOf(cs.primary, cs.secondary, cs.primary)), deckShape)
            .navigationBarsPadding()
            .padding(horizontal = 6.dp, vertical = 5.dp)
            .testTag("cyber_deck_nav_bar"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        // Live telemetry header strip inside Cyber Deck
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .background(cs.primary.copy(alpha = 0.12f), CutCornerShape(4.dp))
              .padding(horizontal = 8.dp, vertical = 2.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = telemetryTicker,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = cs.primary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          Text(
            text = "SYS // ${tabs.getOrNull(activeTabIdx)?.label?.uppercase().orEmpty()}",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = cs.secondary,
            maxLines = 1,
            softWrap = false,
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(3.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          tabs.forEachIndexed { idx, tab ->
            val selected = activeTabIdx == idx
            val keyShape = CutCornerShape(topStart = 7.dp, bottomEnd = 7.dp)
            val fg = if (selected) cs.primary else Color(0xFF94A3B8)
            Box(
              contentAlignment = Alignment.Center,
              modifier =
                Modifier
                  .weight(1f)
                  .height(48.dp)
                  .clip(keyShape)
                  .background(if (selected) cs.primary.copy(alpha = 0.22f) else Color(0xFF0C1526), keyShape)
                  .border(
                    width = if (selected) 1.4.dp else 0.7.dp,
                    color = if (selected) cs.primary else cs.outline.copy(alpha = 0.35f),
                    shape = keyShape,
                  )
                  .clickable { onSelectTab(idx) }
                  .padding(horizontal = 2.dp)
                  .testTag(tab.testTag),
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(tab.icon, contentDescription = tab.label, tint = fg, modifier = Modifier.size(18.dp))
                Text(
                  text = tab.label.uppercase(),
                  fontSize = 7.8.sp,
                  lineHeight = 9.5.sp,
                  letterSpacing = (-0.4).sp,
                  fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                  color = fg,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }
    }

    "neon_underbar" -> {
      Surface(
        color = if (chrome.family == TacticalChromeFamily.OLED_PURE) Color(0xFF000000) else cs.surface.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, cs.primary.copy(alpha = 0.35f)),
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth().testTag("neon_underbar_nav_bar"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(2.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          tabs.forEachIndexed { idx, tab ->
            val selected = activeTabIdx == idx
            val fg = if (selected) cs.primary else cs.onSurfaceVariant
            Box(
              modifier =
                Modifier
                  .weight(1f)
                  .height(58.dp)
                  .then(
                    if (selected) {
                      Modifier.background(
                        Brush.verticalGradient(listOf(cs.primary.copy(alpha = 0.22f), Color.Transparent))
                      )
                    } else Modifier
                  )
                  .drawBehind {
                    if (selected) {
                      drawRect(
                        brush = Brush.horizontalGradient(listOf(cs.secondary, cs.primary, cs.secondary)),
                        topLeft = Offset(4.dp.toPx(), 0f),
                        size = Size(size.width - 8.dp.toPx(), 3.dp.toPx()),
                      )
                    }
                  }
                  .clickable { onSelectTab(idx) }
                  .padding(horizontal = 2.dp, vertical = 4.dp)
                  .testTag(tab.testTag),
              contentAlignment = Alignment.Center,
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(tab.icon, contentDescription = tab.label, tint = fg, modifier = Modifier.size(19.dp))
                Spacer(Modifier.height(2.dp))
                Text(
                  text = tab.label,
                  fontSize = 8.5.sp,
                  lineHeight = 10.sp,
                  letterSpacing = (-0.4).sp,
                  fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                  color = fg,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }
    }

    "split_command" -> {
      val coreTab = tabs.getOrNull(copilotTabIdx)
      val dockTabs = tabs.mapIndexedNotNull { idx, item -> if (idx == copilotTabIdx) null else idx to item }
      val dockShape =
        if (chrome.family == TacticalChromeFamily.BRUTALIST) RectangleShape
        else if (chrome.family == TacticalChromeFamily.CYBER_HUD || chrome.family == TacticalChromeFamily.VAPORWAVE) CutCornerShape(14.dp)
        else RoundedCornerShape(24.dp)
      val orbShape =
        if (chrome.family == TacticalChromeFamily.BRUTALIST) RectangleShape
        else if (chrome.family == TacticalChromeFamily.CYBER_HUD || chrome.family == TacticalChromeFamily.OLED_PURE) CutCornerShape(14.dp)
        else CircleShape

      Row(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp).testTag("split_command_nav_bar"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color = if (chrome.family == TacticalChromeFamily.OLED_PURE) Color(0xFF000000) else cs.surface.copy(alpha = 0.96f),
          shape = dockShape,
          border = BorderStroke(1.2.dp, cs.primary.copy(alpha = 0.5f)),
          shadowElevation = 10.dp,
          modifier = Modifier.weight(1f),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            dockTabs.forEach { (origIdx, tab) ->
              val selected = activeTabIdx == origIdx
              val fg = if (selected) cs.primary else cs.onSurfaceVariant
              val itemShape = chrome.smallShape
              Box(
                contentAlignment = Alignment.Center,
                modifier =
                  Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(itemShape)
                    .background(if (selected) cs.primaryContainer else Color.Transparent, itemShape)
                    .clickable { onSelectTab(origIdx) }
                    .padding(horizontal = 2.dp)
                    .testTag(tab.testTag),
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                  Icon(tab.icon, contentDescription = tab.label, tint = fg, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.height(1.dp))
                  Text(
                    text = tab.label,
                    fontSize = 8.2.sp,
                    lineHeight = 9.5.sp,
                    letterSpacing = (-0.45).sp,
                    fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                    color = fg,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        }

        if (coreTab != null) {
          val coreSelected = activeTabIdx == copilotTabIdx
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier
                .size(58.dp)
                .shadow(12.dp, orbShape)
                .clip(orbShape)
                .background(
                  if (coreSelected) Brush.linearGradient(listOf(cs.primary, cs.secondary))
                  else Brush.linearGradient(listOf(cs.surface, cs.primaryContainer)),
                  orbShape,
                )
                .border(2.dp, if (coreSelected) Color.White.copy(alpha = 0.9f) else cs.primary, orbShape)
                .clickable { onSelectTab(copilotTabIdx) }
                .testTag(coreTab.testTag),
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
              Icon(
                coreTab.icon,
                contentDescription = coreTab.label,
                tint = if (coreSelected) cs.onPrimary else cs.primary,
                modifier = Modifier.size(21.dp),
              )
              Text(
                text = "AI",
                fontSize = 8.5.sp,
                lineHeight = 9.5.sp,
                fontWeight = FontWeight.Black,
                color = if (coreSelected) cs.onPrimary else cs.primary,
                maxLines = 1,
                softWrap = false,
              )
            }
          }
        }
      }
    }

    "brutal_strip" -> {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .background(cs.background)
            .border(width = 2.dp, color = cs.outline, shape = RectangleShape)
            .navigationBarsPadding()
            .padding(horizontal = 4.dp, vertical = 5.dp)
            .testTag("brutal_strip_nav_bar"),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        tabs.forEachIndexed { idx, tab ->
          val selected = activeTabIdx == idx
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier
                .weight(1f)
                .height(52.dp)
                .then(
                  if (selected) {
                    Modifier.drawBehind {
                      drawRect(cs.outline, topLeft = Offset(2.dp.toPx(), 2.dp.toPx()), size = size)
                    }
                  } else Modifier
                )
                .background(if (selected) cs.primary else cs.surface, RectangleShape)
                .border(if (selected) 2.dp else 1.2.dp, cs.outline, RectangleShape)
                .clickable { onSelectTab(idx) }
                .padding(horizontal = 2.dp)
                .testTag(tab.testTag),
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
              Icon(
                tab.icon,
                contentDescription = tab.label,
                tint = if (selected) cs.onPrimary else cs.onSurface,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.height(1.dp))
              Text(
                text = tab.label.uppercase(),
                fontSize = 7.8.sp,
                lineHeight = 9.5.sp,
                letterSpacing = (-0.45).sp,
                fontWeight = FontWeight.Black,
                color = if (selected) cs.onPrimary else cs.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }

    else -> {
      // Upgraded default "pill" bottom navigation bar: zero clipped labels ("Porch Talk" always fits),
      // full theme chrome adaptation across all 9 chrome families!
      val barBg =
        if (chrome.family == TacticalChromeFamily.OLED_PURE) Color(0xFF000000)
        else cs.surface.copy(alpha = if (settings.sunVisibilityMode) 1f else 0.96f)
      val barBorder =
        if (chrome.hasGradientBorder) Brush.horizontalGradient(listOf(cs.primary.copy(alpha = 0.7f), cs.secondary.copy(alpha = 0.6f)))
        else Brush.horizontalGradient(listOf(cs.primary.copy(alpha = 0.42f), cs.outline.copy(alpha = 0.35f)))

      Surface(
        color = barBg,
        border = BorderStroke(chrome.cardBorderWidth.coerceAtLeast(1.dp), barBorder),
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth().testTag("tactical_pill_nav_bar"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 4.dp, vertical = 5.dp),
          horizontalArrangement = Arrangement.spacedBy(2.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          tabs.forEachIndexed { idx, tab ->
            val selected = activeTabIdx == idx
            val pillShape =
              when (chrome.family) {
                TacticalChromeFamily.BRUTALIST -> RectangleShape
                TacticalChromeFamily.CYBER_HUD, TacticalChromeFamily.VAPORWAVE, TacticalChromeFamily.OLED_PURE -> CutCornerShape(8.dp)
                else -> RoundedCornerShape(16.dp)
              }
            val indicatorBg =
              when {
                selected && chrome.family == TacticalChromeFamily.OLED_PURE -> cs.primary.copy(alpha = 0.22f)
                selected -> cs.primaryContainer
                else -> Color.Transparent
              }
            val fg = if (selected) cs.primary else cs.onSurfaceVariant

            Box(
              contentAlignment = Alignment.Center,
              modifier =
                Modifier
                  .weight(1f)
                  .heightIn(min = 54.dp)
                  .clip(pillShape)
                  .background(indicatorBg, pillShape)
                  .then(
                    if (selected && chrome.family == TacticalChromeFamily.CYBER_HUD) {
                      Modifier.border(1.dp, cs.primary.copy(alpha = 0.8f), pillShape)
                    } else if (selected && chrome.family == TacticalChromeFamily.BRUTALIST) {
                      Modifier.border(1.8.dp, cs.outline, pillShape)
                    } else Modifier
                  )
                  .clickable { onSelectTab(idx) }
                  .padding(horizontal = 2.dp, vertical = 5.dp)
                  .testTag(tab.testTag),
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(tab.icon, contentDescription = tab.label, tint = fg, modifier = Modifier.size(19.dp))
                Spacer(Modifier.height(2.dp))
                Text(
                  text = tab.label,
                  fontSize = 8.8.sp,
                  lineHeight = 10.5.sp,
                  letterSpacing = (-0.45).sp,
                  fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                  color = fg,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun TacticalTabStrip(
  tabs: List<String>,
  selectedIndex: Int,
  onSelectTab: (Int) -> Unit,
  modifier: Modifier = Modifier,
  scrollable: Boolean = tabs.size > 4,
  testTagPrefix: String = "tab",
) {
  val settings = LocalTacticalSettings.current
  val chrome = LocalTacticalChrome.current
  val cs = MaterialTheme.colorScheme
  val isClassic95 =
    (settings.visualStyle == VisualStyleMode.WINDOWS_CLASSIC || settings.visualStyle == VisualStyleMode.WINDOWS_2000) &&
      !settings.nightVisionMode &&
      !settings.sunVisibilityMode

  val rowModifier =
    if (scrollable) {
      modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 6.dp)
    } else {
      modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)
    }

  Row(
    modifier = rowModifier,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    tabs.forEachIndexed { idx, label ->
      val selected = selectedIndex == idx
      val shape: Shape = if (isClassic95) RectangleShape else chrome.smallShape
      val bgColor =
        when {
          isClassic95 -> if (selected) TacticalColors.Win95TitleNavy else TacticalColors.Win95Silver
          selected && chrome.family == TacticalChromeFamily.OLED_PURE -> cs.primary.copy(alpha = 0.22f)
          selected -> cs.primaryContainer
          else -> cs.surfaceVariant.copy(alpha = 0.48f)
        }
      val fgColor =
        when {
          isClassic95 -> if (selected) Color.White else TacticalColors.Win95BlackInk
          selected -> cs.primary
          else -> cs.onSurfaceVariant
        }
      val borderColor =
        when {
          isClassic95 -> TacticalColors.Win95DarkShadow
          selected -> cs.primary
          else -> cs.outline.copy(alpha = 0.35f)
        }
      val itemBaseMod =
        if (scrollable) Modifier.heightIn(min = 48.dp)
        else Modifier.weight(1f).heightIn(min = 48.dp)

      Box(
        contentAlignment = Alignment.Center,
        modifier =
          itemBaseMod
            .clip(shape)
            .then(
              if (isClassic95) Modifier.background(bgColor, RectangleShape).windowsClassicBevel(sunken = selected)
              else Modifier.background(bgColor, shape).border(if (selected) 1.5.dp else 1.dp, borderColor, shape)
            )
            .clickable { onSelectTab(idx) }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("${testTagPrefix}_$idx"),
      ) {
        Text(
          text = if (chrome.uppercaseHeaders && selected) label.uppercase() else label,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
          color = fgColor,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
fun TacticalTopBar(
  title: String,
  subtitle: String? = null,
  onBack: (() -> Unit)? = null,
  backTestTag: String = "back_button",
  icon: ImageVector? = null,
  badgeText: String? = null,
  actions: @Composable RowScope.() -> Unit = {},
) {
  val settings = LocalTacticalSettings.current
  val chrome = LocalTacticalChrome.current
  val cs = MaterialTheme.colorScheme
  val isClassicWin = isWindowsClassicActive(settings)

  if (isClassicWin) {
    Column(modifier = Modifier.fillMaxWidth().background(cs.surface).statusBarsPadding()) {
      ClassicWindowTitleBar(title = title, icon = icon, trailingBadge = badgeText)
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag(backTestTag)) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = cs.onSurface)
            }
          }
          if (!subtitle.isNullOrBlank()) {
            Text(
              text = subtitle,
              style = MaterialTheme.typography.labelMedium,
              color = cs.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
      }
      HorizontalDivider(color = cs.outline.copy(alpha = 0.45f))
    }
    return
  }

  val headerBrush =
    when (chrome.family) {
      TacticalChromeFamily.VAPORWAVE ->
        Brush.horizontalGradient(listOf(cs.surface, cs.primary.copy(alpha = 0.16f), cs.secondary.copy(alpha = 0.12f)))
      TacticalChromeFamily.CYBER_HUD ->
        Brush.horizontalGradient(listOf(cs.surface, cs.primary.copy(alpha = 0.12f)))
      TacticalChromeFamily.BRUTALIST ->
        Brush.horizontalGradient(listOf(cs.surface, cs.primary.copy(alpha = 0.14f)))
      TacticalChromeFamily.OLED_PURE ->
        Brush.horizontalGradient(listOf(Color(0xFF000000), Color(0xFF050A12)))
      else -> Brush.horizontalGradient(listOf(cs.surface.copy(alpha = 0.98f), cs.surface.copy(alpha = 0.95f)))
    }

  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .background(headerBrush)
        .statusBarsPadding(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.weight(1f),
      ) {
        if (onBack != null) {
          IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag(backTestTag)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = cs.onSurface)
          }
        } else if (icon != null) {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier
                .size(36.dp)
                .clip(chrome.smallShape)
                .background(cs.primary.copy(alpha = 0.16f))
                .border(1.dp, cs.primary.copy(alpha = 0.55f), chrome.smallShape),
          ) {
            Icon(icon, contentDescription = null, tint = cs.primary, modifier = Modifier.size(20.dp))
          }
        }
        Column(Modifier.weight(1f)) {
          Text(
            text = if (chrome.uppercaseHeaders) title.uppercase() else title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = cs.onSurface,
            maxLines = 1,
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
                .background(cs.primary.copy(alpha = 0.16f), chrome.smallShape)
                .border(0.9.dp, cs.primary.copy(alpha = 0.6f), chrome.smallShape)
                .padding(horizontal = 7.dp, vertical = 3.dp),
          ) {
            Text(
              text = badgeText,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Black,
              color = cs.primary,
              maxLines = 1,
              softWrap = false,
            )
          }
        }
        actions()
      }
    }
    HorizontalDivider(
      thickness = if (chrome.family == TacticalChromeFamily.BRUTALIST) 2.dp else 1.dp,
      color = cs.primary.copy(alpha = 0.32f),
    )
  }
}
