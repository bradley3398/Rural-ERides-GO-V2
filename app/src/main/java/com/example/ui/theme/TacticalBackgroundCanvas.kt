package com.example.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.BgTexture
import com.example.data.PevWallpaper
import com.example.data.VisualStyleMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun resolveWallpaperDrawableRes(wallpaper: PevWallpaper, style: VisualStyleMode): Int? =
  when (wallpaper) {
    PevWallpaper.NONE -> null
    PevWallpaper.HYPER_TRACK -> R.drawable.img_pev_wp_hyper_track_1795253903325
    PevWallpaper.VESC_CARBON -> R.drawable.img_pev_wp_vesc_carbon_1795253903051
    PevWallpaper.ALPINE_RIDGE -> R.drawable.img_pev_wp_alpine_ridge_1795253902717
    PevWallpaper.CYBER_CITY_EUC -> R.drawable.img_pev_wp_cyber_city_euc_1795253903156
    PevWallpaper.FOREST_EUC -> R.drawable.img_pev_bg_forest_euc_1795212595148
    PevWallpaper.NEON_SCOOTER -> R.drawable.img_pev_bg_neon_scooter_1795212595219
    PevWallpaper.CANYON_EMOTO -> R.drawable.img_pev_bg_canyon_emoto_1795212594140
    PevWallpaper.COASTAL_ONEWHEEL -> R.drawable.img_pev_bg_coastal_onewheel_1795212595108
    PevWallpaper.MOUNTAIN_TRAIL -> R.drawable.img_pev_bg_wallpaper_1795208256664
    PevWallpaper.COCKPIT_HERO -> R.drawable.img_pev_cockpit_hero_1795208257433
    PevWallpaper.BOARD_CONVOY -> R.drawable.img_pev_board_banner_1795208257759
    PevWallpaper.CYBER_ALLEY -> R.drawable.img_pev_wp_cyber_alley_1795392499323
    PevWallpaper.GRAVEL_SUNSET -> R.drawable.img_pev_wp_gravel_sunset_1795392500845
    PevWallpaper.BARN_WORKBENCH -> R.drawable.img_pev_wp_barn_workbench_1795392500349
    PevWallpaper.SYNTHWAVE_GRID -> R.drawable.img_pev_wp_synthwave_grid_1795392500461
    PevWallpaper.ALPINE_GLACIER -> R.drawable.img_pev_wp_alpine_glacier_1795392501170
    PevWallpaper.AUTO_THEME ->
      when (style) {
        VisualStyleMode.PEV_TACTICAL_NEON -> R.drawable.img_pev_wp_vesc_carbon_1795253903051
        VisualStyleMode.PEV_APEX_TRACK -> R.drawable.img_pev_wp_hyper_track_1795253903325
        VisualStyleMode.PEV_GRAVEL_DUST -> R.drawable.img_pev_wp_gravel_sunset_1795392500845
        VisualStyleMode.PEV_TITANIUM_PRO -> R.drawable.img_pev_wp_vesc_carbon_1795253903051
        VisualStyleMode.PEV_LITHIUM_LAB -> R.drawable.img_pev_wp_vesc_carbon_1795253903051
        VisualStyleMode.PEV_NIGHT_CONVOY -> R.drawable.img_pev_wp_cyber_alley_1795392499323
        VisualStyleMode.PEV_TRAIL_FOREST -> R.drawable.img_pev_bg_forest_euc_1795212595148
        VisualStyleMode.PEV_MIDNIGHT_NEON -> R.drawable.img_pev_wp_cyber_alley_1795392499323
        VisualStyleMode.PEV_DESERT_ENDURO -> R.drawable.img_pev_bg_canyon_emoto_1795212594140
        VisualStyleMode.PEV_SUNSET_CRUISE -> R.drawable.img_pev_bg_coastal_onewheel_1795212595108
        VisualStyleMode.PEV_VOLT_RACER -> R.drawable.img_pev_wp_hyper_track_1795253903325
        VisualStyleMode.PEV_CYBER_CARBON -> R.drawable.img_pev_wp_vesc_carbon_1795253903051
        VisualStyleMode.PEV_ARCTIC_ICE -> R.drawable.img_pev_wp_alpine_glacier_1795392501170
        VisualStyleMode.RURAL_BARNWOOD -> R.drawable.img_pev_wp_barn_workbench_1795392500349
        VisualStyleMode.RURAL_SUNSET_GRAVEL -> R.drawable.img_pev_wp_gravel_sunset_1795392500845
        VisualStyleMode.RURAL_PINE_HOLLER -> R.drawable.img_pev_bg_forest_euc_1795212595148
        VisualStyleMode.RURAL_PRAIRIE_GOLD -> R.drawable.img_pev_wp_gravel_sunset_1795392500845
        VisualStyleMode.RURAL_CREEK_MIST -> R.drawable.img_pev_wp_alpine_glacier_1795392501170
        VisualStyleMode.RURAL_HARVEST_IRON -> R.drawable.img_pev_wp_barn_workbench_1795392500349
        VisualStyleMode.VAPORWAVE_RETRO -> R.drawable.img_pev_wp_synthwave_grid_1795392500461
        VisualStyleMode.HOLIDAY_THANKSGIVING,
        VisualStyleMode.HOLIDAY_LABOR_DAY -> R.drawable.img_pev_wp_barn_workbench_1795392500349
        VisualStyleMode.HOLIDAY_CHRISTMAS,
        VisualStyleMode.HOLIDAY_ST_PATRICKS -> R.drawable.img_pev_bg_forest_euc_1795212595148
        VisualStyleMode.HOLIDAY_NEW_YEARS,
        VisualStyleMode.HOLIDAY_FOURTH_JULY,
        VisualStyleMode.HOLIDAY_HALLOWEEN -> R.drawable.img_pev_wp_cyber_alley_1795392499323
        VisualStyleMode.HOLIDAY_MEMORIAL_DAY,
        VisualStyleMode.HOLIDAY_VETERANS_DAY -> R.drawable.img_pev_wp_alpine_ridge_1795253902717
        VisualStyleMode.HOLIDAY_VALENTINES,
        VisualStyleMode.HOLIDAY_EASTER -> R.drawable.img_pev_bg_coastal_onewheel_1795212595108
        VisualStyleMode.BRUTALIST_HIGH_VIS, VisualStyleMode.DARK_STUDIO -> R.drawable.img_pev_wp_cyber_alley_1795392499323
        else -> R.drawable.img_pev_bg_wallpaper_1795208256664
      }
  }

@Composable
fun TacticalBackgroundCanvas(
  texture: BgTexture,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  val settings = LocalTacticalSettings.current
  val bg = MaterialTheme.colorScheme.background
  val accent = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary
  val isLightBg = bg.luminance() > 0.5f
  val style = sanitizeVisualStyleForSeason(settings.visualStyle)
  val isWin95 = style == VisualStyleMode.WINDOWS_CLASSIC && !settings.nightVisionMode && !settings.sunVisibilityMode
  val isWin2K = style == VisualStyleMode.WINDOWS_2000 && !settings.nightVisionMode && !settings.sunVisibilityMode
  val isWinXP = style == VisualStyleMode.WINDOWS_XP && !settings.nightVisionMode && !settings.sunVisibilityMode
  val isWin7 = style == VisualStyleMode.WINDOWS_7 && !settings.nightVisionMode && !settings.sunVisibilityMode
  val isWin10 = style == VisualStyleMode.WINDOWS_10 && !settings.nightVisionMode && !settings.sunVisibilityMode
  val isWin11 = style == VisualStyleMode.WINDOWS_11 && !settings.nightVisionMode && !settings.sunVisibilityMode
  val isAnyWindows = isWin95 || isWin2K || isWinXP || isWin7 || isWin10 || isWin11
  val isVaporwave = style == VisualStyleMode.VAPORWAVE_RETRO && !settings.nightVisionMode && !settings.sunVisibilityMode
  val isOled = style == VisualStyleMode.OLED_STEALTH && !settings.nightVisionMode && !settings.sunVisibilityMode

  val wallpaperRes =
    if (isAnyWindows || settings.sunVisibilityMode || settings.nightVisionMode || isOled) null
    else resolveWallpaperDrawableRes(settings.pevWallpaper, style)

  Box(modifier = modifier.fillMaxSize().background(bg)) {
    if (wallpaperRes != null) {
      val clampedOpacity = settings.wallpaperOpacity.coerceIn(0.05f, 0.85f)
      val imgAlpha = if (isLightBg) (clampedOpacity * 0.35f) else clampedOpacity
      Image(
        painter = painterResource(wallpaperRes),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alpha = imgAlpha,
        modifier = Modifier.fillMaxSize(),
      )
      Box(
        modifier =
          Modifier.fillMaxSize().background(
            Brush.verticalGradient(
              colors =
                listOf(
                  bg.copy(alpha = if (isLightBg) 0.72f else (1f - clampedOpacity * 0.72f).coerceIn(0.28f, 0.85f)),
                  bg.copy(alpha = if (isLightBg) 0.58f else (1f - clampedOpacity * 0.50f).coerceIn(0.38f, 0.88f)),
                  bg.copy(alpha = if (isLightBg) 0.82f else 0.86f),
                )
            )
          )
      )
    }

    if (isWin95 || isWin2K) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val desktopBase = if (isWin2K) TacticalColors.Win2KDesktopSlate else TacticalColors.Win95DesktopTeal
        drawRect(color = desktopBase)
        val dot = Color.White.copy(alpha = if (isWin2K) 0.05f else 0.04f)
        var y = 0f
        while (y < h) {
          var x = 0f
          while (x < w) {
            drawCircle(dot, radius = 1f, center = Offset(x, y))
            x += 12f
          }
          y += 12f
        }
      }
    } else if (isWinXP) {
      Image(
        painter = painterResource(id = R.drawable.img_winxp_bliss_bg_1795214889853),
        contentDescription = "Windows XP Bliss Desktop",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    } else if (isWin7) {
      Image(
        painter = painterResource(id = R.drawable.img_win7_aero_bg_1795214889451),
        contentDescription = "Windows 7 Aero Desktop",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    } else if (isWin10) {
      // Authentic Windows 10 Hero luminous blue window-pane lightbeams desktop
      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(
          brush =
            Brush.linearGradient(
              colors = listOf(Color(0xFF040D1C), Color(0xFF0A2A56), Color(0xFF071B36)),
              start = Offset(0f, 0f),
              end = Offset(w, h),
            )
        )
        // Volumetric diagonal light beams radiating from a tilted 4-pane window glow
        val beam1 =
          Path().apply {
            moveTo(w * 0.56f, h * 0.24f)
            lineTo(w * 0.94f, h * 0.14f)
            lineTo(w * 0.28f, h * 0.88f)
            lineTo(-w * 0.08f, h * 0.68f)
            close()
          }
        drawPath(
          beam1,
          brush =
            Brush.linearGradient(
              colors = listOf(Color(0x8832B6FF), Color(0x380078D7), Color.Transparent),
              start = Offset(w * 0.72f, h * 0.22f),
              end = Offset(w * 0.12f, h * 0.80f),
            ),
        )
        val beam2 =
          Path().apply {
            moveTo(w * 0.64f, h * 0.34f)
            lineTo(w * 1.02f, h * 0.44f)
            lineTo(w * 0.52f, h * 1.02f)
            lineTo(w * 0.16f, h * 0.82f)
            close()
          }
        drawPath(
          beam2,
          brush =
            Brush.linearGradient(
              colors = listOf(Color(0x6600BCF2), Color(0x280067C0), Color.Transparent),
              start = Offset(w * 0.76f, h * 0.38f),
              end = Offset(w * 0.30f, h * 0.95f),
            ),
        )
        // Crisp glowing Hero 4-pane silhouette
        val paneGlow = Color(0x5575D6FF)
        drawRect(paneGlow, topLeft = Offset(w * 0.56f, h * 0.22f), size = androidx.compose.ui.geometry.Size(w * 0.14f, h * 0.09f))
        drawRect(paneGlow, topLeft = Offset(w * 0.72f, h * 0.20f), size = androidx.compose.ui.geometry.Size(w * 0.18f, h * 0.11f))
        drawRect(paneGlow, topLeft = Offset(w * 0.56f, h * 0.33f), size = androidx.compose.ui.geometry.Size(w * 0.14f, h * 0.09f))
        drawRect(paneGlow, topLeft = Offset(w * 0.72f, h * 0.33f), size = androidx.compose.ui.geometry.Size(w * 0.18f, h * 0.11f))
      }
    } else if (isWin11) {
      // Authentic Windows 11 Bloom folded-silk radial ribbons desktop
      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val darkBloom = !isLightBg
        drawRect(
          brush =
            Brush.verticalGradient(
              colors =
                if (darkBloom) listOf(Color(0xFF040D21), Color(0xFF0A2454), Color(0xFF040C1D))
                else listOf(Color(0xFFD6E9FD), Color(0xFFEAF4FF), Color(0xFFC5DFF8))
            )
        )
        // Outer translucent sapphire ribbon fold
        drawCircle(
          brush =
            Brush.radialGradient(
              colors =
                if (darkBloom) listOf(Color(0xCC2588F5), Color(0x771055BD), Color.Transparent)
                else listOf(Color(0xBB3B9DFF), Color(0x661268D4), Color.Transparent),
              center = Offset(w * 0.52f, h * 0.44f),
              radius = w * 0.62f,
            ),
          radius = w * 0.62f,
          center = Offset(w * 0.52f, h * 0.44f),
        )
        // Inner luminous cyan-sky petal fold
        drawOval(
          brush =
            Brush.linearGradient(
              colors =
                if (darkBloom) listOf(Color(0xDD62CEFF), Color(0xAA1971E3), Color(0x44062C75))
                else listOf(Color(0xDD7ED8FF), Color(0xBB2382F0), Color(0x550C4CA8)),
              start = Offset(w * 0.20f, h * 0.30f),
              end = Offset(w * 0.84f, h * 0.58f),
            ),
          topLeft = Offset(w * 0.14f, h * 0.30f),
          size = androidx.compose.ui.geometry.Size(w * 0.72f, h * 0.25f),
        )
        // Soft curved highlight fold
        drawOval(
          brush =
            Brush.linearGradient(
              colors = listOf(Color(0xBB9CE8FF), Color(0x66166FE0), Color.Transparent),
              start = Offset(w * 0.35f, h * 0.35f),
              end = Offset(w * 0.70f, h * 0.62f),
            ),
          topLeft = Offset(w * 0.26f, h * 0.37f),
          size = androidx.compose.ui.geometry.Size(w * 0.54f, h * 0.20f),
        )
      }
    } else if (isVaporwave) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val horizonY = h * 0.56f
        val sunColor = Color(0xFFFF9E00).copy(alpha = 0.22f)
        drawCircle(sunColor, radius = w * 0.32f, center = Offset(w * 0.5f, horizonY - w * 0.1f))
        val gridLine = secondary.copy(alpha = 0.16f)
        for (i in 0..10) {
          val y = horizonY + (i * i) * (h * 0.0044f)
          if (y <= h) drawLine(gridLine, Offset(0f, y), Offset(w, y), strokeWidth = 1.4f)
        }
        val cx = w / 2f
        for (i in -6..6) {
          drawLine(accent.copy(alpha = 0.14f), Offset(cx + i * 24f, horizonY), Offset(cx + i * 160f, h), strokeWidth = 1.2f)
        }
      }
    } else if (texture != BgTexture.NONE && !isOled) {
      val lineColor = accent.copy(alpha = if (isLightBg) 0.09f else 0.085f)
      val subtleColor = secondary.copy(alpha = if (isLightBg) 0.07f else 0.065f)
      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        when (texture) {
          BgTexture.NONE -> Unit
          BgTexture.GRID, BgTexture.COUNTY_GRID -> {
            val step = if (texture == BgTexture.COUNTY_GRID) 76f else 48f
            var x = 0f
            while (x < w) {
              drawLine(lineColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
              x += step
            }
            var y = 0f
            while (y < h) {
              drawLine(lineColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
              y += step
            }
          }
          BgTexture.ASPHALT, BgTexture.GRAVEL, BgTexture.TRAIL_DUST -> {
            val spacing = if (texture == BgTexture.ASPHALT) 24f else 34f
            var y = 12f
            var row = 0
            while (y < h) {
              var x = (row % 3) * 11f
              while (x < w) {
                drawCircle(
                  color = if ((x.toInt() + y.toInt()) % 2 == 0) lineColor else subtleColor,
                  radius = if (texture == BgTexture.GRAVEL) 2.2f else 1.3f,
                  center = Offset(x, y),
                )
                x += spacing
              }
              y += spacing
              row++
            }
          }
          BgTexture.CONTOUR, BgTexture.TOPO, BgTexture.WAVES -> {
            var y = 60f
            while (y < h) {
              val path = Path()
              path.moveTo(0f, y)
              var x = 0f
              while (x <= w + 40f) {
                val amp = if (texture == BgTexture.WAVES) 22f else 16f
                val wave = sin((x / 110f) + (y / 90f)) * amp
                path.lineTo(x, y + wave)
                x += 36f
              }
              drawPath(path, lineColor, style = Stroke(width = 1.4f))
              y += 58f
            }
          }
          BgTexture.FOREST, BgTexture.FARMLAND -> {
            val step = 36f
            var x = -h
            while (x < w + h) {
              drawLine(lineColor, Offset(x, 0f), Offset(x + h * 0.45f, h), strokeWidth = 1.2f)
              x += step
            }
          }
          BgTexture.HEX -> {
            val r = 34f
            val dx = r * 1.5f
            val dy = r * 1.732f
            var col = 0
            var x = 0f
            while (x < w + r) {
              var y = if (col % 2 == 0) 0f else dy / 2f
              while (y < h + r) {
                val p = Path()
                for (i in 0..5) {
                  val ang = (PI / 3.0) * i
                  val px = x + (r * cos(ang)).toFloat()
                  val py = y + (r * sin(ang)).toFloat()
                  if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
                }
                p.close()
                drawPath(p, lineColor, style = Stroke(width = 1f))
                y += dy
              }
              x += dx
              col++
            }
          }
          BgTexture.MOUNTAIN -> {
            var base = 180f
            while (base < h + 120f) {
              val p = Path()
              p.moveTo(0f, base)
              var x = 0f
              var up = true
              while (x <= w + 80f) {
                p.lineTo(x, if (up) base - 46f else base + 10f)
                x += 70f
                up = !up
              }
              drawPath(p, lineColor, style = Stroke(width = 1.4f))
              base += 110f
            }
          }
          BgTexture.CIRCUIT -> {
            var y = 40f
            while (y < h) {
              drawLine(lineColor, Offset(0f, y), Offset(w * 0.4f, y), strokeWidth = 1.3f)
              drawLine(lineColor, Offset(w * 0.4f, y), Offset(w * 0.46f, y + 24f), strokeWidth = 1.3f)
              drawLine(lineColor, Offset(w * 0.46f, y + 24f), Offset(w, y + 24f), strokeWidth = 1.3f)
              drawCircle(subtleColor, radius = 3.5f, center = Offset(w * 0.4f, y))
              y += 84f
            }
          }
          BgTexture.CARBON, BgTexture.STEEL_PLATE -> {
            val step = 28f
            var y = 0f
            while (y < h) {
              var x = 0f
              while (x < w) {
                drawLine(lineColor, Offset(x, y), Offset(x + 12f, y + 12f), strokeWidth = 1.6f)
                drawLine(subtleColor, Offset(x + 14f, y + 12f), Offset(x + 26f, y), strokeWidth = 1.6f)
                x += step
              }
              y += step
            }
          }
          BgTexture.RADAR_GRID -> {
            val center = Offset(w / 2f, h * 0.42f)
            for (ring in 1..7) {
              drawCircle(lineColor, radius = ring * 85f, center = center, style = Stroke(width = 1.2f))
            }
            drawLine(subtleColor, Offset(0f, center.y), Offset(w, center.y), strokeWidth = 1f)
            drawLine(subtleColor, Offset(center.x, 0f), Offset(center.x, h), strokeWidth = 1f)
          }
          BgTexture.MATRIX -> {
            var x = 18f
            var col = 0
            while (x < w) {
              var y = ((col * 37) % 90).toFloat()
              while (y < h) {
                drawLine(lineColor, Offset(x, y), Offset(x, y + 18f), strokeWidth = 2f)
                y += 42f
              }
              x += 30f
              col++
            }
          }
          BgTexture.BLUEPRINT -> {
            val major = 96f
            val minor = 24f
            var x = 0f
            while (x < w) {
              val isMaj = (x % major) < 1f
              drawLine(if (isMaj) lineColor.copy(alpha = 0.14f) else subtleColor.copy(alpha = 0.05f), Offset(x, 0f), Offset(x, h), strokeWidth = if (isMaj) 1.4f else 0.7f)
              x += minor
            }
            var y = 0f
            while (y < h) {
              val isMaj = (y % major) < 1f
              drawLine(if (isMaj) lineColor.copy(alpha = 0.14f) else subtleColor.copy(alpha = 0.05f), Offset(0f, y), Offset(w, y), strokeWidth = if (isMaj) 1.4f else 0.7f)
              y += minor
            }
            drawLine(lineColor, Offset(w * 0.12f, h * 0.18f), Offset(w * 0.88f, h * 0.18f), strokeWidth = 1.2f)
          }
          BgTexture.SCANLINES -> {
            var y = 0f
            var idx = 0
            while (y < h) {
              drawLine(
                color = if (idx % 2 == 0) lineColor else subtleColor.copy(alpha = 0.03f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.5f,
              )
              y += 8f
              idx++
            }
          }
          BgTexture.DIAMOND_GRID -> {
            val step = 52f
            var d = -h
            while (d < w + h) {
              drawLine(lineColor, Offset(d, 0f), Offset(d + h, h), strokeWidth = 1.1f)
              drawLine(subtleColor, Offset(d + h, 0f), Offset(d, h), strokeWidth = 1.1f)
              d += step
            }
          }
          BgTexture.STARFIELD -> {
            for (i in 0..85) {
              val sx = ((i * 197 + 43) % 1000) / 1000f * w
              val sy = ((i * 313 + 71) % 1000) / 1000f * h
              val r = if (i % 7 == 0) 2.6f else if (i % 3 == 0) 1.7f else 1.0f
              drawCircle(
                color = if (i % 5 == 0) accent.copy(alpha = 0.25f) else Color.White.copy(alpha = if (isLightBg) 0.12f else 0.22f),
                radius = r,
                center = Offset(sx, sy),
              )
            }
          }
          BgTexture.WOODGRAIN -> {
            var y = 28f
            var plank = 0
            while (y < h) {
              val p = Path()
              p.moveTo(0f, y)
              var x = 0f
              while (x <= w + 40f) {
                val bend = sin((x / 160f) + plank * 1.1f) * 9f
                p.lineTo(x, y + bend)
                x += 32f
              }
              drawPath(p, if (plank % 3 == 0) lineColor else subtleColor, style = Stroke(width = if (plank % 4 == 0) 2.0f else 1.1f))
              y += 32f
              plank++
            }
          }
          BgTexture.ISOMETRIC -> {
            val s = 54f
            val dy = s * 0.866f
            var row = 0
            var y = 0f
            while (y < h + s) {
              var x = if (row % 2 == 0) 0f else s * 0.5f
              while (x < w + s) {
                drawLine(lineColor, Offset(x, y), Offset(x + s * 0.5f, y + dy * 0.5f), strokeWidth = 1f)
                drawLine(lineColor, Offset(x, y), Offset(x - s * 0.5f, y + dy * 0.5f), strokeWidth = 1f)
                drawLine(subtleColor, Offset(x, y), Offset(x, y + dy), strokeWidth = 1f)
                x += s
              }
              y += dy
              row++
            }
          }
        }
      }
    }
    val showHolidayOrnaments = style.isHolidayTheme() && !settings.nightVisionMode && !settings.sunVisibilityMode
    val holidayTwinklePhase =
      if (showHolidayOrnaments && !settings.reducedMotion) {
        val inf = rememberInfiniteTransition(label = "holiday_twinkle")
        val phase by inf.animateFloat(
          initialValue = 0f,
          targetValue = 1f,
          animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
          ),
          label = "holiday_twinkle_phase",
        )
        phase
      } else 0f

    if (showHolidayOrnaments) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        drawHolidayBackgroundArt(
          mode = style,
          accent = accent,
          secondary = secondary,
          twinklePhase = holidayTwinklePhase,
        )
      }
    }
    content()
    if (showHolidayOrnaments) {
      Canvas(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .statusBarsPadding()
          .fillMaxWidth()
          .height(58.dp),
      ) {
        drawHolidayTopCanopy(mode = style, twinklePhase = holidayTwinklePhase)
      }
    }
  }
}
