package com.example.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.VisualStyleMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class HolidayOrnamentKind {
  FIREWORK_SPARKLE,
  HEART_RIBBON,
  SHAMROCK_CLOVER,
  PAINTED_EGG,
  MEMORIAL_POPPY_STAR,
  LIBERTY_STARBURST,
  HERITAGE_GEAR,
  JACK_O_LANTERN,
  VALOR_CHEVRON,
  MAPLE_HARVEST,
  EVERGREEN_BAUBLE,
}

data class HolidayDecorationSpec(
  val mode: VisualStyleMode,
  val motifTitle: String,
  val trimLabel: String,
  val ornamentColor: Color,
  val sparkleColor: Color,
  val tertiaryAccent: Color,
  val ornamentKind: HolidayOrnamentKind,
)

fun holidayDecorationSpec(mode: VisualStyleMode): HolidayDecorationSpec? =
  when (mode) {
    VisualStyleMode.HOLIDAY_NEW_YEARS -> HolidayDecorationSpec(
      mode = mode,
      motifTitle = "Midnight Fireworks & Confetti",
      trimLabel = "NEW YEAR",
      ornamentColor = Color(0xFFFFD700),
      sparkleColor = Color(0xFF7DD3FC),
      tertiaryAccent = Color(0xFFFDF2B3),
      ornamentKind = HolidayOrnamentKind.FIREWORK_SPARKLE,
    )
    VisualStyleMode.HOLIDAY_VALENTINES -> HolidayDecorationSpec(
      mode = mode,
      motifTitle = "Crimson Hearts & Silk Ribbons",
      trimLabel = "LOVE",
      ornamentColor = Color(0xFFFF4D8D),
      sparkleColor = Color(0xFFFFB3C6),
      tertiaryAccent = Color(0xFFFB7185),
      ornamentKind = HolidayOrnamentKind.HEART_RIBBON,
    )
    VisualStyleMode.HOLIDAY_ST_PATRICKS -> HolidayDecorationSpec(
      mode = mode,
      motifTitle = "Emerald Shamrocks & Celtic Gold",
      trimLabel = "CLOVER",
      ornamentColor = Color(0xFF16A34A),
      sparkleColor = Color(0xFFFACC15),
      tertiaryAccent = Color(0xFF4ADE80),
      ornamentKind = HolidayOrnamentKind.SHAMROCK_CLOVER,
    )
    VisualStyleMode.HOLIDAY_EASTER -> HolidayDecorationSpec(
      mode = mode,
      motifTitle = "Painted Easter Eggs & Spring Tulips",
      trimLabel = "SPRING",
      ornamentColor = Color(0xFFF472B6),
      sparkleColor = Color(0xFF67E8F9),
      tertiaryAccent = Color(0xFFFDE047),
      ornamentKind = HolidayOrnamentKind.PAINTED_EGG,
    )
    VisualStyleMode.HOLIDAY_MEMORIAL_DAY -> HolidayDecorationSpec(
      mode = mode,
      motifTitle = "Patriotic Bunting, Poppies & Stars",
      trimLabel = "HONOR",
      ornamentColor = Color(0xFFEF4444),
      sparkleColor = Color(0xFFF8FAFC),
      tertiaryAccent = Color(0xFF3B82F6),
      ornamentKind = HolidayOrnamentKind.MEMORIAL_POPPY_STAR,
    )
    VisualStyleMode.HOLIDAY_FOURTH_JULY -> HolidayDecorationSpec(
      mode = mode,
      motifTitle = "Independence Starbursts & Stripes",
      trimLabel = "LIBERTY",
      ornamentColor = Color(0xFFEF4444),
      sparkleColor = Color(0xFF60A5FA),
      tertiaryAccent = Color(0xFFF8FAFC),
      ornamentKind = HolidayOrnamentKind.LIBERTY_STARBURST,
    )
    VisualStyleMode.HOLIDAY_LABOR_DAY -> HolidayDecorationSpec(
      mode = mode,
      motifTitle = "Iron Gears, Oak & Gold Wheat",
      trimLabel = "CRAFT",
      ornamentColor = Color(0xFFF59E0B),
      sparkleColor = Color(0xFF38BDF8),
      tertiaryAccent = Color(0xFFE2E8F0),
      ornamentKind = HolidayOrnamentKind.HERITAGE_GEAR,
    )
    VisualStyleMode.HOLIDAY_HALLOWEEN -> HolidayDecorationSpec(
      mode = mode,
      motifTitle = "Jack-O'-Lanterns, Cobwebs & Bats",
      trimLabel = "SPOOKY",
      ornamentColor = Color(0xFFFF6D00),
      sparkleColor = Color(0xFFA855F7),
      tertiaryAccent = Color(0xFFFDE047),
      ornamentKind = HolidayOrnamentKind.JACK_O_LANTERN,
    )
    VisualStyleMode.HOLIDAY_VETERANS_DAY -> HolidayDecorationSpec(
      mode = mode,
      motifTitle = "Valor Chevrons & Gold Star Medal",
      trimLabel = "VALOR",
      ornamentColor = Color(0xFFFACC15),
      sparkleColor = Color(0xFF60A5FA),
      tertiaryAccent = Color(0xFFDC2626),
      ornamentKind = HolidayOrnamentKind.VALOR_CHEVRON,
    )
    VisualStyleMode.HOLIDAY_THANKSGIVING -> HolidayDecorationSpec(
      mode = mode,
      motifTitle = "Autumn Maple Leaves, Wheat & Pumpkins",
      trimLabel = "HARVEST",
      ornamentColor = Color(0xFFEA580C),
      sparkleColor = Color(0xFFF59E0B),
      tertiaryAccent = Color(0xFFB45309),
      ornamentKind = HolidayOrnamentKind.MAPLE_HARVEST,
    )
    VisualStyleMode.HOLIDAY_CHRISTMAS -> HolidayDecorationSpec(
      mode = mode,
      motifTitle = "Evergreen Garland, Baubles & Snowflakes",
      trimLabel = "NOEL",
      ornamentColor = Color(0xFFDC2626),
      sparkleColor = Color(0xFF22C55E),
      tertiaryAccent = Color(0xFFFACC15),
      ornamentKind = HolidayOrnamentKind.EVERGREEN_BAUBLE,
    )
    else -> null
  }

private fun DrawScope.drawFivePointStar(center: Offset, outerR: Float, innerR: Float, color: Color, filled: Boolean = false, strokeW: Float = 1.8f) {
  val path = Path()
  for (i in 0 until 10) {
    val ang = -PI / 2.0 + i * PI / 5.0
    val r = if (i % 2 == 0) outerR else innerR
    val px = center.x + (cos(ang) * r).toFloat()
    val py = center.y + (sin(ang) * r).toFloat()
    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
  }
  path.close()
  if (filled) drawPath(path, color) else drawPath(path, color, style = Stroke(width = strokeW, join = StrokeJoin.Round))
}

private fun DrawScope.drawHeartShape(center: Offset, size: Float, color: Color, filled: Boolean = false, strokeW: Float = 1.8f) {
  val w = size
  val h = size
  val path = Path().apply {
    moveTo(center.x, center.y + h * 0.44f)
    cubicTo(
      center.x - w * 0.56f, center.y + h * 0.10f,
      center.x - w * 0.50f, center.y - h * 0.42f,
      center.x, center.y - h * 0.16f,
    )
    cubicTo(
      center.x + w * 0.50f, center.y - h * 0.42f,
      center.x + w * 0.56f, center.y + h * 0.10f,
      center.x, center.y + h * 0.44f,
    )
    close()
  }
  if (filled) drawPath(path, color) else drawPath(path, color, style = Stroke(width = strokeW, join = StrokeJoin.Round))
}

private fun DrawScope.drawShamrock(center: Offset, radius: Float, color: Color, stemColor: Color) {
  val leafOffset = radius * 0.45f
  val leafR = radius * 0.42f
  val angles = listOf(-90.0, 150.0, 30.0)
  angles.forEach { deg ->
    val rad = Math.toRadians(deg)
    val lx = center.x + (cos(rad) * leafOffset).toFloat()
    val ly = center.y + (sin(rad) * leafOffset).toFloat()
    drawCircle(color, radius = leafR, center = Offset(lx, ly))
  }
  val stemPath = Path().apply {
    moveTo(center.x, center.y)
    quadraticTo(center.x + radius * 0.12f, center.y + radius * 0.65f, center.x - radius * 0.25f, center.y + radius * 0.95f)
  }
  drawPath(stemPath, stemColor, style = Stroke(width = (radius * 0.16f).coerceAtLeast(1.8f), cap = StrokeCap.Round))
}

private fun DrawScope.drawMapleLeaf(center: Offset, r: Float, color: Color, veinColor: Color) {
  val path = Path().apply {
    moveTo(center.x, center.y - r)
    lineTo(center.x + r * 0.22f, center.y - r * 0.42f)
    lineTo(center.x + r * 0.85f, center.y - r * 0.45f)
    lineTo(center.x + r * 0.45f, center.y + r * 0.04f)
    lineTo(center.x + r * 0.72f, center.y + r * 0.42f)
    lineTo(center.x + r * 0.16f, center.y + r * 0.36f)
    lineTo(center.x, center.y + r * 0.62f)
    lineTo(center.x - r * 0.16f, center.y + r * 0.36f)
    lineTo(center.x - r * 0.72f, center.y + r * 0.42f)
    lineTo(center.x - r * 0.45f, center.y + r * 0.04f)
    lineTo(center.x - r * 0.85f, center.y - r * 0.45f)
    lineTo(center.x - r * 0.22f, center.y - r * 0.42f)
    close()
  }
  drawPath(path, color)
  drawLine(veinColor, Offset(center.x, center.y - r * 0.6f), Offset(center.x, center.y + r * 0.88f), strokeWidth = 1.6f, cap = StrokeCap.Round)
}

private fun DrawScope.drawPumpkin(center: Offset, r: Float, bodyColor: Color, stemColor: Color, jackFaceColor: Color? = null) {
  val w = r * 1.15f
  val h = r * 0.92f
  drawOval(bodyColor, topLeft = Offset(center.x - w, center.y - h), size = Size(w * 2f, h * 2f))
  drawOval(stemColor.copy(alpha = 0.25f), topLeft = Offset(center.x - w * 0.56f, center.y - h), size = Size(w * 1.12f, h * 2f), style = Stroke(width = 1.5f))
  drawLine(stemColor, Offset(center.x, center.y - h), Offset(center.x + r * 0.18f, center.y - h * 1.38f), strokeWidth = (r * 0.2f).coerceAtLeast(2f), cap = StrokeCap.Round)
  if (jackFaceColor != null) {
    val eyeY = center.y - h * 0.18f
    drawCircle(jackFaceColor, radius = r * 0.16f, center = Offset(center.x - r * 0.38f, eyeY))
    drawCircle(jackFaceColor, radius = r * 0.16f, center = Offset(center.x + r * 0.38f, eyeY))
    drawArc(
      color = jackFaceColor,
      startAngle = 15f,
      sweepAngle = 150f,
      useCenter = false,
      topLeft = Offset(center.x - r * 0.55f, center.y - h * 0.05f),
      size = Size(r * 1.1f, h * 0.72f),
      style = Stroke(width = (r * 0.16f).coerceAtLeast(1.8f), cap = StrokeCap.Round),
    )
  }
}

private fun DrawScope.drawOrnamentBauble(anchorTop: Offset, radius: Float, baubleColor: Color, capColor: Color, sparkleColor: Color) {
  val cordLen = radius * 0.8f
  val center = Offset(anchorTop.x, anchorTop.y + cordLen + radius)
  drawLine(capColor, anchorTop, Offset(anchorTop.x, center.y - radius), strokeWidth = 1.5f)
  drawRect(capColor, topLeft = Offset(center.x - radius * 0.28f, center.y - radius - radius * 0.22f), size = Size(radius * 0.56f, radius * 0.28f))
  drawCircle(baubleColor, radius = radius, center = center)
  drawArc(
    color = sparkleColor,
    startAngle = 195f,
    sweepAngle = 70f,
    useCenter = false,
    topLeft = Offset(center.x - radius * 0.65f, center.y - radius * 0.65f),
    size = Size(radius * 1.3f, radius * 1.3f),
    style = Stroke(width = 2f, cap = StrokeCap.Round),
  )
}

private fun DrawScope.drawGearIcon(center: Offset, outerR: Float, color: Color) {
  val innerR = outerR * 0.68f
  drawCircle(color, radius = innerR, center = center, style = Stroke(width = (outerR * 0.26f).coerceAtLeast(2f)))
  drawCircle(color, radius = outerR * 0.26f, center = center)
  for (i in 0 until 8) {
    val ang = i * PI / 4.0
    drawLine(
      color = color,
      start = Offset(center.x + (cos(ang) * innerR).toFloat(), center.y + (sin(ang) * innerR).toFloat()),
      end = Offset(center.x + (cos(ang) * outerR).toFloat(), center.y + (sin(ang) * outerR).toFloat()),
      strokeWidth = (outerR * 0.24f).coerceAtLeast(2.2f),
      cap = StrokeCap.Round,
    )
  }
}

private fun DrawScope.drawEasterEgg(center: Offset, rx: Float, ry: Float, shellColor: Color, stripeColor: Color) {
  drawOval(shellColor, topLeft = Offset(center.x - rx, center.y - ry), size = Size(rx * 2f, ry * 2f), style = Stroke(width = 2.0f))
  drawLine(stripeColor, Offset(center.x - rx * 0.85f, center.y - ry * 0.22f), Offset(center.x + rx * 0.85f, center.y - ry * 0.22f), strokeWidth = 2f)
  drawLine(stripeColor, Offset(center.x - rx * 0.85f, center.y + ry * 0.22f), Offset(center.x + rx * 0.85f, center.y + ry * 0.22f), strokeWidth = 2f)
}

private fun DrawScope.drawBatSilhouette(center: Offset, wingSpan: Float, color: Color) {
  val path = Path().apply {
    moveTo(center.x - wingSpan, center.y)
    quadraticTo(center.x - wingSpan * 0.55f, center.y - wingSpan * 0.42f, center.x - wingSpan * 0.18f, center.y - wingSpan * 0.14f)
    lineTo(center.x - wingSpan * 0.08f, center.y - wingSpan * 0.28f)
    lineTo(center.x, center.y - wingSpan * 0.16f)
    lineTo(center.x + wingSpan * 0.08f, center.y - wingSpan * 0.28f)
    lineTo(center.x + wingSpan * 0.18f, center.y - wingSpan * 0.14f)
    quadraticTo(center.x + wingSpan * 0.55f, center.y - wingSpan * 0.42f, center.x + wingSpan, center.y)
    quadraticTo(center.x + wingSpan * 0.45f, center.y + wingSpan * 0.14f, center.x, center.y + wingSpan * 0.22f)
    quadraticTo(center.x - wingSpan * 0.45f, center.y + wingSpan * 0.14f, center.x - wingSpan, center.y)
    close()
  }
  drawPath(path, color)
}

private fun DrawScope.drawOrnamentByKind(
  kind: HolidayOrnamentKind,
  center: Offset,
  radius: Float,
  primary: Color,
  secondary: Color,
  tertiary: Color,
) {
  when (kind) {
    HolidayOrnamentKind.FIREWORK_SPARKLE -> {
      for (r in 0 until 8) {
        val ang = r * PI / 4.0
        drawLine(
          color = if (r % 2 == 0) primary else secondary,
          start = center,
          end = Offset(center.x + (cos(ang) * radius).toFloat(), center.y + (sin(ang) * radius).toFloat()),
          strokeWidth = 2f,
          cap = StrokeCap.Round,
        )
      }
      drawCircle(tertiary, radius = radius * 0.24f, center = center)
    }
    HolidayOrnamentKind.HEART_RIBBON -> drawHeartShape(center, radius * 1.7f, primary, filled = true)
    HolidayOrnamentKind.SHAMROCK_CLOVER -> drawShamrock(center, radius, primary, secondary)
    HolidayOrnamentKind.PAINTED_EGG -> drawEasterEgg(center, radius * 0.72f, radius, primary, secondary)
    HolidayOrnamentKind.MEMORIAL_POPPY_STAR -> {
      drawCircle(primary, radius = radius * 0.82f, center = center)
      drawFivePointStar(center, radius * 0.52f, radius * 0.24f, secondary, filled = true)
    }
    HolidayOrnamentKind.LIBERTY_STARBURST -> drawFivePointStar(center, radius, radius * 0.44f, tertiary, filled = true)
    HolidayOrnamentKind.HERITAGE_GEAR -> drawGearIcon(center, radius, primary)
    HolidayOrnamentKind.JACK_O_LANTERN -> drawPumpkin(center, radius * 0.9f, primary, secondary, tertiary)
    HolidayOrnamentKind.VALOR_CHEVRON -> {
      val path = Path().apply {
        moveTo(center.x - radius * 0.85f, center.y - radius * 0.2f)
        lineTo(center.x, center.y + radius * 0.45f)
        lineTo(center.x + radius * 0.85f, center.y - radius * 0.2f)
      }
      drawPath(path, primary, style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
      drawFivePointStar(Offset(center.x, center.y - radius * 0.35f), radius * 0.42f, radius * 0.18f, primary, filled = true)
    }
    HolidayOrnamentKind.MAPLE_HARVEST -> drawMapleLeaf(center, radius, primary, tertiary)
    HolidayOrnamentKind.EVERGREEN_BAUBLE -> drawOrnamentBauble(Offset(center.x, center.y - radius * 1.1f), radius * 0.7f, primary, tertiary, Color.White.copy(alpha = 0.7f))
  }
}

/**
 * Full-screen holiday wallpaper vector ornament art — unique, bespoke motifs per holiday.
 */
fun DrawScope.drawHolidayBackgroundArt(
  mode: VisualStyleMode,
  accent: Color,
  secondary: Color,
) {
  val spec = holidayDecorationSpec(mode) ?: return
  val w = size.width
  val h = size.height
  val c1 = spec.ornamentColor.copy(alpha = 0.20f)
  val c2 = spec.sparkleColor.copy(alpha = 0.18f)
  val c3 = spec.tertiaryAccent.copy(alpha = 0.16f)

  when (mode) {
    VisualStyleMode.HOLIDAY_NEW_YEARS -> {
      val bursts = listOf(Offset(w * 0.20f, h * 0.18f), Offset(w * 0.78f, h * 0.24f), Offset(w * 0.50f, h * 0.72f))
      bursts.forEachIndexed { idx, center ->
        val rad = 68f + idx * 18f
        for (ray in 0 until 16) {
          val ang = (PI * 2.0 * ray) / 16.0
          val col = if (ray % 2 == 0) c1 else c2
          drawLine(col, center, Offset(center.x + (cos(ang) * rad).toFloat(), center.y + (sin(ang) * rad).toFloat()), strokeWidth = 1.8f)
          if (ray % 3 == 0) {
            drawCircle(c3, radius = 3.2f, center = Offset(center.x + (cos(ang) * rad * 1.15f).toFloat(), center.y + (sin(ang) * rad * 1.15f).toFloat()))
          }
        }
      }
      // Rising champagne bubble stream
      for (i in 0..14) {
        val bx = w * 0.12f + ((i * 197) % 100) / 100f * w * 0.76f
        val by = h - ((i * 239 + 60) % 100) / 100f * h * 0.8f
        drawCircle(c3, radius = 4f + (i % 4) * 3.5f, center = Offset(bx, by), style = Stroke(width = 1.3f))
      }
    }
    VisualStyleMode.HOLIDAY_VALENTINES -> {
      // Flowing ribbon garland
      val ribbon = Path().apply {
        moveTo(0f, h * 0.14f)
        cubicTo(w * 0.3f, h * 0.22f, w * 0.7f, h * 0.06f, w, h * 0.16f)
      }
      drawPath(ribbon, c2, style = Stroke(width = 2.4f))
      for (i in 0..12) {
        val hx = ((i * 193 + 53) % 1000) / 1000f * w
        val hy = ((i * 263 + 89) % 1000) / 1000f * h
        val sz = 18f + (i % 4) * 8f
        drawHeartShape(Offset(hx, hy), sz, if (i % 2 == 0) c1 else c2, filled = i % 3 == 0)
      }
    }
    VisualStyleMode.HOLIDAY_ST_PATRICKS -> {
      for (i in 0..11) {
        val sx = ((i * 211 + 47) % 1000) / 1000f * w
        val sy = ((i * 283 + 61) % 1000) / 1000f * h
        drawShamrock(Offset(sx, sy), 14f + (i % 3) * 6f, c1, c2)
      }
      // Celtic interlocking corner rings
      for (ring in 1..3) {
        drawCircle(c2, radius = 34f * ring, center = Offset(w * 0.88f, h * 0.14f), style = Stroke(width = 1.6f))
      }
    }
    VisualStyleMode.HOLIDAY_EASTER -> {
      // Soft rolling hillside
      val hill = Path().apply {
        moveTo(0f, h * 0.88f)
        quadraticTo(w * 0.35f, h * 0.78f, w * 0.72f, h * 0.86f)
        quadraticTo(w * 0.88f, h * 0.90f, w, h * 0.83f)
      }
      drawPath(hill, c2, style = Stroke(width = 2.4f))
      for (i in 0..11) {
        val ex = ((i * 227 + 43) % 1000) / 1000f * w
        val ey = ((i * 307 + 79) % 1000) / 1000f * h * 0.85f
        val rx = 12f + (i % 3) * 4f
        drawEasterEgg(Offset(ex, ey), rx, rx * 1.38f, if (i % 2 == 0) c1 else c2, c3)
      }
    }
    VisualStyleMode.HOLIDAY_MEMORIAL_DAY -> {
      // Draped bunting swags across top
      for (s in 0..3) {
        val x0 = s * (w / 4f)
        val x1 = (s + 1) * (w / 4f)
        val swag = Path().apply {
          moveTo(x0, 0f)
          quadraticTo((x0 + x1) * 0.5f, 44f, x1, 0f)
        }
        drawPath(swag, if (s % 2 == 0) c1 else c3, style = Stroke(width = 2.6f))
      }
      for (i in 0..12) {
        val sx = ((i * 199 + 37) % 1000) / 1000f * w
        val sy = ((i * 277 + 83) % 1000) / 1000f * h
        if (i % 2 == 0) {
          drawFivePointStar(Offset(sx, sy), 13f + (i % 3) * 5f, 6f + (i % 3) * 2f, c2, filled = false)
        } else {
          drawCircle(c1, radius = 10f + (i % 3) * 4f, center = Offset(sx, sy))
          drawCircle(Color.Black.copy(alpha = 0.14f), radius = 3.5f, center = Offset(sx, sy))
        }
      }
    }
    VisualStyleMode.HOLIDAY_FOURTH_JULY -> {
      val bursts = listOf(Offset(w * 0.22f, h * 0.20f), Offset(w * 0.80f, h * 0.26f), Offset(w * 0.45f, h * 0.74f))
      bursts.forEachIndexed { idx, c ->
        val rad = 74f + idx * 14f
        for (ray in 0 until 14) {
          val ang = (PI * 2.0 * ray) / 14.0
          val col = when (ray % 3) {
            0 -> c1
            1 -> c2
            else -> c3
          }
          drawLine(col, c, Offset(c.x + (cos(ang) * rad).toFloat(), c.y + (sin(ang) * rad).toFloat()), strokeWidth = 2f)
        }
      }
      for (i in 0..10) {
        val sx = ((i * 239 + 61) % 1000) / 1000f * w
        val sy = ((i * 313 + 47) % 1000) / 1000f * h
        drawFivePointStar(Offset(sx, sy), 14f, 6.2f, if (i % 2 == 0) c3 else c2, filled = true)
      }
    }
    VisualStyleMode.HOLIDAY_LABOR_DAY -> {
      for (i in 0..8) {
        val gx = ((i * 241 + 59) % 1000) / 1000f * w
        val gy = ((i * 317 + 71) % 1000) / 1000f * h
        drawGearIcon(Offset(gx, gy), 18f + (i % 3) * 8f, if (i % 2 == 0) c1 else c2)
      }
    }
    VisualStyleMode.HOLIDAY_HALLOWEEN -> {
      // Corner spiderweb arcs
      val webOrigin = Offset(0f, h * 0.14f)
      for (r in 1..5) {
        drawCircle(c2, radius = r * 44f, center = webOrigin, style = Stroke(width = 1.2f))
      }
      for (spoke in 0..6) {
        val ang = Math.toRadians((spoke * 15).toDouble() - 15.0)
        drawLine(c2, webOrigin, Offset(webOrigin.x + (cos(ang) * 230f).toFloat(), webOrigin.y + (sin(ang) * 230f).toFloat()), strokeWidth = 1.1f)
      }
      drawCircle(c1, radius = w * 0.18f, center = Offset(w * 0.86f, h * 0.16f))
      drawBatSilhouette(Offset(w * 0.72f, h * 0.22f), 32f, c2)
      drawBatSilhouette(Offset(w * 0.24f, h * 0.44f), 26f, c2)
      drawPumpkin(Offset(w * 0.82f, h * 0.78f), 28f, c1, c2, c3)
      drawPumpkin(Offset(w * 0.18f, h * 0.74f), 22f, c1, c2, c3)
    }
    VisualStyleMode.HOLIDAY_VETERANS_DAY -> {
      // Valor chevrons + stars
      for (band in 0..2) {
        val yBase = h * (0.20f + band * 0.28f)
        val chev = Path().apply {
          moveTo(w * 0.1f, yBase)
          lineTo(w * 0.5f, yBase + 46f)
          lineTo(w * 0.9f, yBase)
        }
        drawPath(chev, if (band % 2 == 0) c1 else c2, style = Stroke(width = 2.6f))
      }
      for (i in 0..7) {
        drawFivePointStar(Offset(w * (0.12f + i * 0.11f), h * 0.14f), 11f, 5f, c1, filled = true)
      }
    }
    VisualStyleMode.HOLIDAY_THANKSGIVING -> {
      // Scattered detailed autumn maple leaves + harvest pumpkins
      for (i in 0..14) {
        val lx = ((i * 193 + 57) % 1000) / 1000f * w
        val ly = ((i * 271 + 83) % 1000) / 1000f * h
        drawMapleLeaf(Offset(lx, ly), 14f + (i % 4) * 5f, if (i % 2 == 0) c1 else c2, c3)
      }
      drawPumpkin(Offset(w * 0.85f, h * 0.82f), 26f, c1, c3)
      drawPumpkin(Offset(w * 0.16f, h * 0.80f), 20f, c2, c3)
    }
    VisualStyleMode.HOLIDAY_CHRISTMAS -> {
      // Top evergreen garland swag + hanging ornaments + crystalline snowflakes
      val garland = Path().apply {
        moveTo(0f, 18f)
        quadraticTo(w * 0.25f, 58f, w * 0.50f, 20f)
        quadraticTo(w * 0.75f, 58f, w, 18f)
      }
      drawPath(garland, c2, style = Stroke(width = 4.5f, cap = StrokeCap.Round))
      listOf(w * 0.22f, w * 0.50f, w * 0.78f).forEachIndexed { idx, bx ->
        drawOrnamentBauble(
          anchorTop = Offset(bx, if (idx == 1) 20f else 38f),
          radius = 12f,
          baubleColor = if (idx % 2 == 0) c1 else c3,
          capColor = c3,
          sparkleColor = Color.White.copy(alpha = 0.28f),
        )
      }
      val snowCol = Color.White.copy(alpha = 0.18f)
      for (i in 0..24) {
        val sx = ((i * 179 + 31) % 1000) / 1000f * w
        val sy = ((i * 257 + 67) % 1000) / 1000f * h
        val arm = 6f + (i % 3) * 3.5f
        drawLine(snowCol, Offset(sx - arm, sy), Offset(sx + arm, sy), strokeWidth = 1.4f)
        drawLine(snowCol, Offset(sx, sy - arm), Offset(sx, sy + arm), strokeWidth = 1.4f)
        drawLine(snowCol, Offset(sx - arm * 0.7f, sy - arm * 0.7f), Offset(sx + arm * 0.7f, sy + arm * 0.7f), strokeWidth = 1.1f)
        drawLine(snowCol, Offset(sx + arm * 0.7f, sy - arm * 0.7f), Offset(sx - arm * 0.7f, sy + arm * 0.7f), strokeWidth = 1.1f)
      }
    }
    else -> Unit
  }
}

/**
 * Renders festive garland trim and corner ornament badge directly onto TacticalCard frames.
 */
fun DrawScope.drawHolidayCardOrnament(
  mode: VisualStyleMode,
  highlight: Boolean,
) {
  val spec = holidayDecorationSpec(mode) ?: return
  val w = size.width
  val h = size.height
  val alpha = if (highlight) 0.55f else 0.34f
  val p = spec.ornamentColor.copy(alpha = alpha)
  val s = spec.sparkleColor.copy(alpha = alpha)
  val t = spec.tertiaryAccent.copy(alpha = alpha)

  // Top decorative garland swag / bunting line
  val steps = ((w / 48f).toInt()).coerceIn(3, 12)
  val stepW = w / steps
  val garlandPath = Path().apply {
    moveTo(0f, 3f)
    for (i in 0 until steps) {
      val xMid = (i + 0.5f) * stepW
      val xEnd = (i + 1f) * stepW
      quadraticTo(xMid, 11f, xEnd, 3f)
    }
  }
  drawPath(garlandPath, s, style = Stroke(width = 1.8f, cap = StrokeCap.Round))

  // Twinkling ornament dots along garland scallops
  for (i in 1 until steps) {
    drawCircle(
      color = if (i % 2 == 0) p else t,
      radius = 2.6f,
      center = Offset(i * stepW, 4f),
    )
  }

  // Bespoke holiday corner ornament in top-right + subtle bottom-left accent
  drawOrnamentByKind(
    kind = spec.ornamentKind,
    center = Offset(w - 16f, 16f),
    radius = 8.5f,
    primary = p,
    secondary = s,
    tertiary = t,
  )
  drawOrnamentByKind(
    kind = spec.ornamentKind,
    center = Offset(14f, h - 14f),
    radius = 6.5f,
    primary = s,
    secondary = p,
    tertiary = t,
  )
}

/**
 * Renders subtle seasonal holiday trim and hanging ornaments on top border of active Nav Bars.
 */
fun DrawScope.drawHolidayNavOrnament(mode: VisualStyleMode) {
  val spec = holidayDecorationSpec(mode) ?: return
  val w = size.width
  val p = spec.ornamentColor.copy(alpha = 0.72f)
  val s = spec.sparkleColor.copy(alpha = 0.72f)
  val t = spec.tertiaryAccent.copy(alpha = 0.72f)

  val segments = 8
  val segW = w / segments
  val swag = Path().apply {
    moveTo(0f, 2f)
    for (i in 0 until segments) {
      quadraticTo((i + 0.5f) * segW, 9f, (i + 1f) * segW, 2f)
    }
  }
  drawPath(swag, s, style = Stroke(width = 1.6f))
  for (i in 1 until segments) {
    val cx = i * segW
    if (i == 1 || i == segments - 1) {
      drawOrnamentByKind(spec.ornamentKind, Offset(cx, 8f), 5.2f, p, s, t)
    } else {
      drawCircle(if (i % 2 == 0) p else t, radius = 2.8f, center = Offset(cx, 4.5f))
    }
  }
}
