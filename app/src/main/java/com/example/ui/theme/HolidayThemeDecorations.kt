package com.example.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.VisualStyleMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class HolidayDecorationSpec(
    val mode: VisualStyleMode,
    val trimLabel: String,
    val ornamentColor: Color,
    val secondaryOrnamentColor: Color,
    val bannerEmoji: String,
)

fun holidayDecorationSpec(mode: VisualStyleMode): HolidayDecorationSpec? =
    when (mode) {
        VisualStyleMode.HOLIDAY_CHRISTMAS ->
            HolidayDecorationSpec(
                mode = mode,
                trimLabel = "🎄 HOLLY & PINE",
                ornamentColor = Color(0xFFFF1744),
                secondaryOrnamentColor = Color(0xFF00E676),
                bannerEmoji = "🎄",
            )
        VisualStyleMode.HOLIDAY_HALLOWEEN ->
            HolidayDecorationSpec(
                mode = mode,
                trimLabel = "🎃 HALLOWS EVE",
                ornamentColor = Color(0xFFFF6D00),
                secondaryOrnamentColor = Color(0xFFD500F9),
                bannerEmoji = "🎃",
            )
        VisualStyleMode.HOLIDAY_THANKSGIVING ->
            HolidayDecorationSpec(
                mode = mode,
                trimLabel = "🍂 HARVEST GOLD",
                ornamentColor = Color(0xFFFF8F00),
                secondaryOrnamentColor = Color(0xFFD84315),
                bannerEmoji = "🦃",
            )
        VisualStyleMode.HOLIDAY_FOURTH_JULY ->
            HolidayDecorationSpec(
                mode = mode,
                trimLabel = "🎆 LIBERTY 1776",
                ornamentColor = Color(0xFFFF1744),
                secondaryOrnamentColor = Color(0xFF2979FF),
                bannerEmoji = "🇺🇸",
            )
        VisualStyleMode.HOLIDAY_MEMORIAL_DAY ->
            HolidayDecorationSpec(
                mode = mode,
                trimLabel = "🎖️ HONOR & REMEMBRANCE",
                ornamentColor = Color(0xFFD32F2F),
                secondaryOrnamentColor = Color(0xFF1976D2),
                bannerEmoji = "🎖️",
            )
        VisualStyleMode.HOLIDAY_VETERANS_DAY ->
            HolidayDecorationSpec(
                mode = mode,
                trimLabel = "⭐ VETERANS VALOR",
                ornamentColor = Color(0xFFFFD54F),
                secondaryOrnamentColor = Color(0xFFC62828),
                bannerEmoji = "⭐",
            )
        VisualStyleMode.HOLIDAY_NEW_YEARS ->
            HolidayDecorationSpec(
                mode = mode,
                trimLabel = "🥂 NEW YEAR SPARK",
                ornamentColor = Color(0xFFFFD700),
                secondaryOrnamentColor = Color(0xFF00E5FF),
                bannerEmoji = "🎆",
            )
        VisualStyleMode.HOLIDAY_VALENTINES ->
            HolidayDecorationSpec(
                mode = mode,
                trimLabel = "💘 CRIMSON PULSE",
                ornamentColor = Color(0xFFFF4081),
                secondaryOrnamentColor = Color(0xFFFF80AB),
                bannerEmoji = "💘",
            )
        VisualStyleMode.HOLIDAY_ST_PATRICKS ->
            HolidayDecorationSpec(
                mode = mode,
                trimLabel = "☘️ EMERALD CLOVER",
                ornamentColor = Color(0xFF00E676),
                secondaryOrnamentColor = Color(0xFFFFD54F),
                bannerEmoji = "☘️",
            )
        VisualStyleMode.HOLIDAY_EASTER ->
            HolidayDecorationSpec(
                mode = mode,
                trimLabel = "🌸 SPRING BLOOM",
                ornamentColor = Color(0xFFF48FB1),
                secondaryOrnamentColor = Color(0xFF80CBC4),
                bannerEmoji = "🐣",
            )
        VisualStyleMode.HOLIDAY_LABOR_DAY ->
            HolidayDecorationSpec(
                mode = mode,
                trimLabel = "⚒️ IRON & CRAFT",
                ornamentColor = Color(0xFFFFB300),
                secondaryOrnamentColor = Color(0xFF42A5F5),
                bannerEmoji = "⚒️",
            )
        else -> null
    }

private fun DrawScope.drawFivePointStar(center: Offset, outerRadius: Float, innerRadius: Float, color: Color) {
    val path = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val angle = -PI / 2.0 + i * (PI / 5.0)
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

fun DrawScope.drawHolidayBackgroundArt(mode: VisualStyleMode, accent: Color, secondary: Color) {
    val w = size.width
    val h = size.height
    when (mode) {
        VisualStyleMode.HOLIDAY_CHRISTMAS -> {
            // Festive string lights garland along the top + snowflakes
            val bulbs = 12
            val step = w / bulbs
            for (i in 0..bulbs) {
                val x = i * step
                val sagY = 18f + sin(i * 0.9f) * 8f
                val bulbColor =
                    when (i % 3) {
                        0 -> Color(0xFFFF1744)
                        1 -> Color(0xFF00E676)
                        else -> Color(0xFFFFD700)
                    }
                drawCircle(color = bulbColor.copy(alpha = 0.25f), radius = 14f, center = Offset(x, sagY))
                drawCircle(color = bulbColor.copy(alpha = 0.75f), radius = 5.5f, center = Offset(x, sagY))
            }
            for (i in 1..18) {
                val sx = ((i * 173) % 100) / 100f * w
                val sy = ((i * 257) % 100) / 100f * h
                drawCircle(color = Color.White.copy(alpha = 0.16f), radius = (2 + (i % 4)).toFloat(), center = Offset(sx, sy))
            }
        }
        VisualStyleMode.HOLIDAY_HALLOWEEN -> {
            // Glowing harvest moon + eerie embers
            drawCircle(color = Color(0xFFFF6D00).copy(alpha = 0.22f), radius = 78f, center = Offset(w * 0.84f, h * 0.16f))
            drawCircle(color = Color(0xFFFFAB00).copy(alpha = 0.32f), radius = 44f, center = Offset(w * 0.84f, h * 0.16f))
            for (i in 1..14) {
                val ex = ((i * 131) % 100) / 100f * w
                val ey = ((i * 293) % 100) / 100f * h
                val col = if (i % 2 == 0) Color(0xFFFF6D00) else Color(0xFFD500F9)
                drawCircle(color = col.copy(alpha = 0.22f), radius = 4.5f, center = Offset(ex, ey))
            }
        }
        VisualStyleMode.HOLIDAY_THANKSGIVING -> {
            // Warm autumn harvest glows and falling leaf motifs
            drawCircle(color = Color(0xFFFF8F00).copy(alpha = 0.18f), radius = 72f, center = Offset(w * 0.16f, h * 0.18f))
            drawCircle(color = Color(0xFFD84315).copy(alpha = 0.16f), radius = 58f, center = Offset(w * 0.85f, h * 0.72f))
            for (i in 1..12) {
                val lx = ((i * 157) % 100) / 100f * w
                val ly = ((i * 223) % 100) / 100f * h
                val leafCol = if (i % 2 == 0) Color(0xFFFF8F00) else Color(0xFFD84315)
                drawOval(color = leafCol.copy(alpha = 0.22f), topLeft = Offset(lx, ly), size = Size(14f, 8f))
            }
        }
        VisualStyleMode.HOLIDAY_FOURTH_JULY -> {
            // Red, white & blue firework bursts + stars
            val bursts = listOf(
                Triple(Offset(w * 0.22f, h * 0.18f), Color(0xFFFF1744), 52f),
                Triple(Offset(w * 0.78f, h * 0.22f), Color(0xFF2979FF), 58f),
                Triple(Offset(w * 0.50f, h * 0.14f), Color.White, 44f),
            )
            bursts.forEach { (center, col, rad) ->
                for (ray in 0 until 12) {
                    val ang = ray * (PI / 6.0)
                    val end = Offset(center.x + (rad * cos(ang)).toFloat(), center.y + (rad * sin(ang)).toFloat())
                    drawLine(col.copy(alpha = 0.28f), center, end, strokeWidth = 2.4f)
                    drawCircle(col.copy(alpha = 0.45f), radius = 3.2f, center = end)
                }
            }
            for (i in 1..10) {
                val sx = ((i * 149) % 100) / 100f * w
                val sy = ((i * 199) % 100) / 100f * (h * 0.65f)
                drawFivePointStar(Offset(sx, sy), 9f, 4f, Color.White.copy(alpha = 0.22f))
            }
        }
        VisualStyleMode.HOLIDAY_MEMORIAL_DAY,
        VisualStyleMode.HOLIDAY_VETERANS_DAY -> {
            // Patriotic stars & honor stripes ribbon across top
            val stripeH = 6f
            drawRect(Color(0xFFC62828).copy(alpha = 0.30f), topLeft = Offset(0f, 0f), size = Size(w, stripeH))
            drawRect(Color.White.copy(alpha = 0.22f), topLeft = Offset(0f, stripeH), size = Size(w, stripeH))
            drawRect(Color(0xFF1565C0).copy(alpha = 0.32f), topLeft = Offset(0f, stripeH * 2), size = Size(w, stripeH))
            for (i in 1..9) {
                val sx = (w / 10f) * i
                val sy = h * 0.12f + (i % 2) * 18f
                val starCol = if (mode == VisualStyleMode.HOLIDAY_VETERANS_DAY) Color(0xFFFFD54F) else Color.White
                drawFivePointStar(Offset(sx, sy), 11f, 4.8f, starCol.copy(alpha = 0.26f))
            }
        }
        VisualStyleMode.HOLIDAY_NEW_YEARS -> {
            // Golden champagne bubbles & midnight firework sparkles
            for (i in 1..20) {
                val bx = ((i * 137) % 100) / 100f * w
                val by = ((i * 241) % 100) / 100f * h
                val col = if (i % 3 == 0) Color(0xFF00E5FF) else Color(0xFFFFD700)
                drawCircle(col.copy(alpha = 0.24f), radius = (3 + (i % 6)).toFloat(), center = Offset(bx, by), style = Stroke(width = 1.6f))
            }
            drawFivePointStar(Offset(w * 0.82f, h * 0.16f), 16f, 6f, Color(0xFFFFD700).copy(alpha = 0.35f))
            drawFivePointStar(Offset(w * 0.18f, h * 0.22f), 13f, 5f, Color(0xFF00E5FF).copy(alpha = 0.30f))
        }
        VisualStyleMode.HOLIDAY_VALENTINES -> {
            drawCircle(color = Color(0xFFFF4081).copy(alpha = 0.20f), radius = 64f, center = Offset(w * 0.82f, h * 0.18f))
            drawCircle(color = Color(0xFFFF80AB).copy(alpha = 0.16f), radius = 48f, center = Offset(w * 0.18f, h * 0.76f))
        }
        VisualStyleMode.HOLIDAY_ST_PATRICKS -> {
            drawCircle(color = Color(0xFF00E676).copy(alpha = 0.20f), radius = 64f, center = Offset(w * 0.20f, h * 0.18f))
            drawCircle(color = Color(0xFFFFD54F).copy(alpha = 0.18f), radius = 48f, center = Offset(w * 0.82f, h * 0.76f))
        }
        VisualStyleMode.HOLIDAY_EASTER -> {
            drawCircle(color = Color(0xFFF48FB1).copy(alpha = 0.18f), radius = 56f, center = Offset(w * 0.22f, h * 0.26f))
            drawCircle(color = Color(0xFF80CBC4).copy(alpha = 0.18f), radius = 56f, center = Offset(w * 0.78f, h * 0.72f))
        }
        VisualStyleMode.HOLIDAY_LABOR_DAY -> {
            drawCircle(color = Color(0xFFFFB300).copy(alpha = 0.18f), radius = 56f, center = Offset(w * 0.50f, h * 0.22f))
        }
        else -> {
            drawCircle(color = accent.copy(alpha = 0.1f), radius = 50f, center = Offset(w * 0.5f, h * 0.3f))
        }
    }
}

fun DrawScope.drawHolidayCardOrnament(mode: VisualStyleMode, highlight: Boolean) {
    val spec = holidayDecorationSpec(mode) ?: return
    val w = size.width
    val h = size.height
    val primary = spec.ornamentColor
    val secondary = spec.secondaryOrnamentColor

    // Festive top garland / trim line along card top edge
    drawLine(
        color = primary.copy(alpha = if (highlight) 0.85f else 0.55f),
        start = Offset(0f, 2.5f),
        end = Offset(w * 0.5f, 2.5f),
        strokeWidth = 3.5f,
    )
    drawLine(
        color = secondary.copy(alpha = if (highlight) 0.85f else 0.55f),
        start = Offset(w * 0.5f, 2.5f),
        end = Offset(w, 2.5f),
        strokeWidth = 3.5f,
    )

    // Corner ornament bauble / star in top-right and bottom-right corners
    val cornerCenter = Offset(w - 14f, h - 14f)
    when (mode) {
        VisualStyleMode.HOLIDAY_FOURTH_JULY,
        VisualStyleMode.HOLIDAY_MEMORIAL_DAY,
        VisualStyleMode.HOLIDAY_VETERANS_DAY,
        VisualStyleMode.HOLIDAY_NEW_YEARS -> {
            drawFivePointStar(cornerCenter, 8.5f, 3.8f, primary.copy(alpha = 0.75f))
            drawFivePointStar(Offset(14f, h - 14f), 6.5f, 2.8f, secondary.copy(alpha = 0.55f))
        }
        else -> {
            drawCircle(color = primary.copy(alpha = 0.35f), radius = 9.5f, center = cornerCenter)
            drawCircle(color = primary.copy(alpha = 0.85f), radius = 5.2f, center = cornerCenter)
            drawCircle(color = secondary.copy(alpha = 0.75f), radius = 3.8f, center = Offset(w - 26f, h - 12f))
        }
    }
}

fun DrawScope.drawHolidayNavOrnament(mode: VisualStyleMode) {
    val spec = holidayDecorationSpec(mode) ?: return
    val w = size.width
    // Alternating festive lights / stars along the navigation bar top edge
    val count = 10
    val step = w / count
    for (i in 0..count) {
        val x = i * step
        val col = if (i % 2 == 0) spec.ornamentColor else spec.secondaryOrnamentColor
        drawCircle(color = col.copy(alpha = 0.85f), radius = 3.8f, center = Offset(x, 3.5f))
        drawCircle(color = col.copy(alpha = 0.28f), radius = 7.5f, center = Offset(x, 3.5f))
    }
    drawLine(
        color = spec.ornamentColor.copy(alpha = 0.7f),
        start = Offset(0f, 2.5f),
        end = Offset(w, 2.5f),
        strokeWidth = 2.5f,
    )
}
