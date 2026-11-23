package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ColorModeOverride
import com.example.data.MasterSettings
import com.example.data.VisualStyleMode
import com.example.hardware.VoiceAudioManager
import com.example.ui.components.RuralPevIcons
import com.example.ui.components.TacticalButton
import com.example.ui.components.TacticalCard
import com.example.ui.components.TacticalOutlinedButton
import com.example.ui.components.UpgradedIconBadge
import com.example.ui.components.Windows10PerspectiveEmblem
import com.example.ui.components.Windows11SquareEmblem
import com.example.ui.components.WindowsFourPaneEmblem
import com.example.ui.components.isAnyWindowsDesktopActive
import com.example.ui.theme.RuralERidesTheme
import com.example.ui.theme.TacticalBackgroundCanvas
import com.example.viewmodel.SessionViewModel

private data class WindowsEraPreset(
  val shortLabel: String,
  val mode: VisualStyleMode,
  val accentHex: Color,
)

private val WINDOWS_ERA_PRESETS =
  listOf(
    WindowsEraPreset("Win 95", VisualStyleMode.WINDOWS_CLASSIC, Color(0xFF008080)),
    WindowsEraPreset("Win 2000", VisualStyleMode.WINDOWS_2000, Color(0xFF3A6EA5)),
    WindowsEraPreset("Win XP", VisualStyleMode.WINDOWS_XP, Color(0xFF245EDC)),
    WindowsEraPreset("Win 7", VisualStyleMode.WINDOWS_7, Color(0xFF1F7ED6)),
    WindowsEraPreset("Win 10", VisualStyleMode.WINDOWS_10, Color(0xFF0078D7)),
    WindowsEraPreset("Win 11", VisualStyleMode.WINDOWS_11, Color(0xFF0067C0)),
  )

/**
 * Interactive LIVE Theme & Windows Desktop Preview Studio.
 * Renders a real working miniature preview of the active Rural ERides GO theme,
 * wallpaper, window chrome, upgraded icon pack, button bevel/ripple, and taskbar.
 */
@Composable
fun LiveThemePreviewStudioCard(
  settings: MasterSettings,
  sessionVm: SessionViewModel,
  onOpenWindowsStudio: (() -> Unit)? = null,
) {
  val haptics = LocalHapticFeedback.current
  var testTaps by remember { mutableIntStateOf(0) }
  var previewActiveTabIdx by remember { mutableIntStateOf(0) }

  TacticalCard(highlight = true, title = "LIVE THEME & WINDOWS PREVIEW STUDIO") {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.weight(1f),
      ) {
        UpgradedIconBadge(
          icon = RuralPevIcons.ActionThemeStudio,
          contentDescription = "Theme Studio",
          tint = MaterialTheme.colorScheme.primary,
          containerSize = 34.dp,
          iconSize = 18.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = settings.visualStyle.label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = settings.visualStyle.description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      if (onOpenWindowsStudio != null) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
          modifier =
            Modifier
              .clip(RoundedCornerShape(10.dp))
              .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onOpenWindowsStudio()
              }
              .testTag("preview_open_windows_studio_btn"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
              "Studio",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              maxLines = 1,
              softWrap = false,
            )
          }
        }
      }
    }

    Spacer(Modifier.height(10.dp))

    // LIVE INTERACTIVE VIEWPORT WRAPPED IN REAL APP THEME + REAL WALLPAPER/DESKTOP CANVAS
    RuralERidesTheme(settings = settings) {
      val previewFrameShape =
        if (settings.visualStyle == VisualStyleMode.WINDOWS_CLASSIC || settings.visualStyle == VisualStyleMode.WINDOWS_2000) {
          RectangleShape
        } else {
          RoundedCornerShape(16.dp)
        }

      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .clip(previewFrameShape)
            .border(
              width = 1.5.dp,
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
              shape = previewFrameShape,
            )
            .testTag("live_theme_preview_viewport"),
      ) {
        TacticalBackgroundCanvas(
          texture = settings.bgTexture,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              // Authentic mini window card rendered using the live TacticalCard chrome!
              TacticalCard(
                highlight = true,
                title = "Rural ERides GO — ${settings.visualStyle.label}",
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    UpgradedIconBadge(
                      icon = RuralPevIcons.NavTrailheadGauge,
                      contentDescription = "Preview Speedometer",
                      tint = MaterialTheme.colorScheme.primary,
                      containerSize = 34.dp,
                      iconSize = 18.dp,
                    )
                    Column {
                      Text(
                        text = if (settings.distanceUnit == "mi") "26.4 MPH" else "42.5 KM/H",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false,
                      )
                      Text(
                        text = "54.6V • 100% Full • ${settings.iconStylePack}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                  }
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      Icon(
                        Icons.Default.BatteryChargingFull,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                      )
                      Text(
                        text = "READY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        softWrap = false,
                      )
                    }
                  }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  TacticalButton(
                    onClick = {
                      testTaps++
                      haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                      sessionVm.voiceAudio.playTouchSound(
                        enabled = settings.touchSoundsEnabled,
                        pack = settings.touchSoundPack,
                        cue = VoiceAudioManager.TouchCue.ACTION_CONFIRM,
                        volumePct = settings.globalVolume,
                      )
                    },
                    modifier = Modifier.weight(1f).testTag("preview_test_button"),
                  ) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                      text = if (testTaps == 0) "Test Button" else "Tested ${testTaps}x",
                      maxLines = 1,
                      softWrap = false,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  TacticalOutlinedButton(
                    onClick = {
                      haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                      val nextMode =
                        when (settings.colorModeOverride) {
                          ColorModeOverride.DARK -> ColorModeOverride.LIGHT
                          ColorModeOverride.LIGHT -> ColorModeOverride.SYSTEM
                          ColorModeOverride.SYSTEM -> ColorModeOverride.DARK
                        }
                      sessionVm.updateSettings { it.copy(colorModeOverride = nextMode) }
                    },
                    modifier = Modifier.weight(1f).testTag("preview_flip_mode_button"),
                  ) {
                    Icon(
                      imageVector = if (settings.colorModeOverride == ColorModeOverride.LIGHT) Icons.Default.LightMode else Icons.Default.DarkMode,
                      contentDescription = null,
                      modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                      text = settings.colorModeOverride.label,
                      maxLines = 1,
                      softWrap = false,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }
            }

            // LIVE MINIATURE TASKBAR / BOTTOM NAV PREVIEW MATCHING THE ACTIVE THEME!
            MiniThemePreviewTaskbar(
              settings = settings,
              activeTabIdx = previewActiveTabIdx,
              onSelectTab = { idx ->
                previewActiveTabIdx = idx
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                sessionVm.voiceAudio.playTouchSound(
                  enabled = settings.touchSoundsEnabled,
                  pack = settings.touchSoundPack,
                  cue = VoiceAudioManager.TouchCue.TAB_SWITCH,
                  volumePct = settings.globalVolume,
                )
              },
            )
          }
        }
      }
    }

    Spacer(Modifier.height(10.dp))

    // 1-TAP AUTHENTIC WINDOWS OS ERA SWITCHER
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Default.DesktopWindows, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Text(
          text = "1-Tap Windows Desktop Themes",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
          maxLines = 1,
          softWrap = false,
        )
      }
      if (isAnyWindowsDesktopActive(settings)) {
        Text(
          text = "ACTIVE",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Black,
          color = MaterialTheme.colorScheme.tertiary,
          maxLines = 1,
          softWrap = false,
        )
      }
    }

    Spacer(Modifier.height(6.dp))

    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(WINDOWS_ERA_PRESETS) { era ->
        val selected = settings.visualStyle == era.mode
        FilterChip(
          selected = selected,
          onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            sessionVm.updateSettings {
              it.copy(
                visualStyle = era.mode,
                navBarStyle = "classic_taskbar",
                showClassicTitleBars = true,
                nightVisionMode = false,
                sunVisibilityMode = false,
              )
            }
            sessionVm.voiceAudio.playTouchSound(
              enabled = settings.touchSoundsEnabled,
              pack = settings.touchSoundPack,
              cue = VoiceAudioManager.TouchCue.ACTION_CONFIRM,
              volumePct = settings.globalVolume,
            )
          },
          leadingIcon = {
            if (selected) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
            } else {
              Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(era.accentHex))
            }
          },
          label = { Text(era.shortLabel, maxLines = 1, softWrap = false) },
          modifier = Modifier.testTag("quick_win_era_${era.mode.id}"),
        )
      }
    }
  }
}

@Composable
private fun MiniThemePreviewTaskbar(
  settings: MasterSettings,
  activeTabIdx: Int,
  onSelectTab: (Int) -> Unit,
) {
  val tabs = listOf("Dash", "Radar", "Shop", "AI")
  when (settings.visualStyle) {
    VisualStyleMode.WINDOWS_CLASSIC, VisualStyleMode.WINDOWS_2000 -> {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(Color(0xFFC0C0C0))
            .border(1.dp, Color.White)
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          modifier =
            Modifier
              .background(Color(0xFFC0C0C0))
              .border(1.dp, Color(0xFF404040))
              .padding(horizontal = 6.dp, vertical = 3.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          WindowsFourPaneEmblem(size = 12.dp)
          Text("Start", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black, maxLines = 1, softWrap = false)
        }
        tabs.forEachIndexed { idx, label ->
          val sel = idx == activeTabIdx
          Box(
            modifier =
              Modifier
                .weight(1f)
                .height(24.dp)
                .background(if (sel) Color(0xFFE0E0E0) else Color(0xFFC0C0C0))
                .border(1.dp, if (sel) Color(0xFF404040) else Color.White)
                .clickable { onSelectTab(idx) },
            contentAlignment = Alignment.Center,
          ) {
            Text(label, fontSize = 9.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, color = Color.Black, maxLines = 1, softWrap = false)
          }
        }
        if (settings.winShowTaskbarClock) {
          Box(
            modifier = Modifier.background(Color(0xFFB8B8B8)).border(1.dp, Color(0xFF808080)).padding(horizontal = 5.dp, vertical = 3.dp),
          ) {
            Text("12:45 PM", fontSize = 9.sp, color = Color.Black, maxLines = 1, softWrap = false)
          }
        }
      }
    }

    VisualStyleMode.WINDOWS_XP -> {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(Brush.verticalGradient(listOf(Color(0xFF3E84EA), Color(0xFF245EDC), Color(0xFF1941A5)))),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier =
            Modifier
              .height(36.dp)
              .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
              .background(Brush.verticalGradient(listOf(Color(0xFF72C963), Color(0xFF3B9A32), Color(0xFF287822))))
              .padding(horizontal = 9.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          WindowsFourPaneEmblem(size = 13.dp)
          Text("start", fontSize = 11.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, color = Color.White, maxLines = 1, softWrap = false)
        }
        Row(
          modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
          tabs.forEachIndexed { idx, label ->
            val sel = idx == activeTabIdx
            Box(
              modifier =
                Modifier
                  .weight(1f)
                  .height(26.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(if (sel) Color(0xFF1A46A8) else Color(0xFF3B82EE))
                  .clickable { onSelectTab(idx) },
              contentAlignment = Alignment.Center,
            ) {
              Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, softWrap = false)
            }
          }
        }
        if (settings.winShowTaskbarClock) {
          Box(
            modifier = Modifier.height(36.dp).background(Color(0xFF1393E4)).padding(horizontal = 7.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text("12:45", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, softWrap = false)
          }
        }
      }
    }

    VisualStyleMode.WINDOWS_7 -> {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(
              Brush.verticalGradient(
                listOf(
                  Color(0xFF2E5D8A).copy(alpha = settings.winGlassIntensity.coerceIn(0.6f, 0.96f)),
                  Color(0xFF0E2942).copy(alpha = (settings.winGlassIntensity + 0.08f).coerceAtMost(0.98f)),
                )
              )
            )
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Box(
          modifier =
            Modifier
              .size(26.dp)
              .clip(CircleShape)
              .background(Brush.radialGradient(listOf(Color(0xFF66D1FF), Color(0xFF125A9C), Color(0xFF0A2239))))
              .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          WindowsFourPaneEmblem(size = 13.dp)
        }
        tabs.forEachIndexed { idx, label ->
          val sel = idx == activeTabIdx
          Box(
            modifier =
              Modifier
                .weight(1f)
                .height(26.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (sel) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f))
                .border(0.8.dp, Color.White.copy(alpha = if (sel) 0.65f else 0.2f), RoundedCornerShape(4.dp))
                .clickable { onSelectTab(idx) },
            contentAlignment = Alignment.Center,
          ) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, softWrap = false)
          }
        }
        if (settings.winShowTaskbarClock) {
          Text("12:45 PM", fontSize = 9.sp, color = Color.White, maxLines = 1, softWrap = false)
        }
      }
    }

    VisualStyleMode.WINDOWS_10 -> {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(Color(0xFF101010))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
          Windows10PerspectiveEmblem(size = 14.dp, tint = Color.White)
        }
        tabs.forEachIndexed { idx, label ->
          val sel = idx == activeTabIdx
          Column(
            modifier =
              Modifier
                .weight(1f)
                .height(34.dp)
                .background(if (sel) Color(0xFF282828) else Color.Transparent)
                .clickable { onSelectTab(idx) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Color.White, maxLines = 1, softWrap = false)
            Box(
              modifier = Modifier.fillMaxWidth(0.7f).height(2.dp).background(if (sel) Color(0xFF4CC2FF) else Color.Transparent),
            )
          }
        }
        if (settings.winShowTaskbarClock) {
          Text("12:45 PM", fontSize = 9.sp, color = Color.White.copy(alpha = 0.9f), maxLines = 1, softWrap = false)
        }
      }
    }

    VisualStyleMode.WINDOWS_11 -> {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(Color(0xFF1D232E).copy(alpha = settings.winGlassIntensity.coerceIn(0.68f, 0.96f)))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        Box(
          modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.08f)),
          contentAlignment = Alignment.Center,
        ) {
          Windows11SquareEmblem(size = 14.dp)
        }
        Spacer(Modifier.width(6.dp))
        tabs.forEachIndexed { idx, label ->
          val sel = idx == activeTabIdx
          Column(
            modifier =
              Modifier
                .padding(horizontal = 2.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (sel) Color.White.copy(alpha = 0.14f) else Color.Transparent)
                .clickable { onSelectTab(idx) }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(label, fontSize = 9.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, color = Color.White, maxLines = 1, softWrap = false)
            Spacer(Modifier.height(2.dp))
            Box(
              modifier =
                Modifier
                  .width(if (sel) 14.dp else 0.dp)
                  .height(2.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF60CDFF)),
            )
          }
        }
        if (settings.winShowTaskbarClock) {
          Spacer(Modifier.width(8.dp))
          Text("12:45", fontSize = 9.sp, color = Color.White.copy(alpha = 0.9f), maxLines = 1, softWrap = false)
        }
      }
    }

    else -> {
      // Tactical Android Pill Navigation Preview
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        val navIcons =
          listOf(
            RuralPevIcons.NavTrailheadGauge to "Dash",
            RuralPevIcons.NavRadarDish to "Radar",
            RuralPevIcons.NavBarnMechanic to "Shop",
            RuralPevIcons.NavCopilotCore to "AI",
          )
        navIcons.forEachIndexed { idx, (icon, label) ->
          val sel = idx == activeTabIdx
          Row(
            modifier =
              Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (sel) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                .clickable { onSelectTab(idx) }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = icon,
              contentDescription = label,
              modifier = Modifier.size(14.dp),
              tint = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = label,
              fontSize = 10.sp,
              fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
              color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              softWrap = false,
            )
          }
        }
      }
    }
  }
}
