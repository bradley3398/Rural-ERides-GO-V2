package com.example.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
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
      motifTitle = "Midnight Fireworks, Gold Stars & Confetti",
      trimLabel = "NEW YEAR",
      ornamentColor = Color(0xFFFFD700),
      sparkleColor = Color(0xFF38BDF8),
      tertiaryAccent = Color(0xFFFDF2B3),
      ornamentKind = HolidayOrnamentKind.FIREWORK_SPARKLE,
    )
    VisualStyleMode.HOLIDAY_VALENTINES -> HolidayDecorationSpec(
      mode = mode,
      motifTitle = "Crimson Hearts, Lace & Silk Ribbons",
      trimLabel = "LOVE",
      ornamentColor = Color(0xFFFF4D8D),
      sparkleColor = Color(0xFFFFB3C6),
      tertiaryAccent = Color(0xFFFB7185),
      ornamentKind = HolidayOrnamentKind.HEART_RIBBON,
    )
    VisualStyleMode.HOLIDAY_ST_PATRICKS -> HolidayDecorationSpec(
      mode = mode,
      motifTitle = "Emerald Shamrocks, Gold Coins & Garland",
      trimLabel = "CLOVER",
      ornamentColor = Color(0xFF22C55E),
      sparkleColor = Color(0xFFFACC15),
      tertiaryAccent = Color(0xFF4ADE80),
      ornamentKind = HolidayOrnamentKind.SHAMROCK_CLOVER,
    )
    VisualStyleMode.HOLIDAY_EASTER -> HolidayDecorationSpec(
      mode = mode,
      motifTitle = "Painted Easter Eggs, Pastel Bunting & Blooms",
      trimLabel = "SPRING",
      ornamentColor = Color(0xFFF472B6),
      sparkleColor = Color(0xFF67E8F9),
      tertiaryAccent = Color(0xFFFDE047),
      ornamentKind = HolidayOrnamentKind.PAINTED_EGG,
    )
    VisualStyleMode.HOLIDAY_MEMORIAL_DAY -> HolidayDecorationSpec(
      mode = mode,
      motifTitle = "Patriotic Bunting, Poppies & Honor Stars",
      trimLabel = "HONOR",
      ornamentColor = Color(0xFFEF4444),
      sparkleColor = Color(0xFFF8FAFC),
      tertiaryAccent = Color(0xFF3B82F6),
      ornamentKind = HolidayOrnamentKind.MEMORIAL_POPPY_STAR,
    )
    VisualStyleMode.HOLIDAY_FOURTH_JULY -> HolidayDecorationSpec(
      mode = mode,
      motifTitle = "Independence Starbursts, Bunting & Stripes",
      trimLabel = "LIBERTY",
      ornamentColor = Color(0xFFEF4444),
      sparkleColor = Color(0xFF60A5FA),
      tertiaryAccent = Color(0xFFF8FAFC),
      ornamentKind = HolidayOrnamentKind.LIBERTY_STARBURST,
    )
    VisualStyleMode.HOLIDAY_LABOR_DAY -> HolidayDecorationSpec(
      mode = mode,
      motifTitle = "Heritage Gears, Bronze Swags & Gold Stars",
      trimLabel = "CRAFT",
      ornamentColor = Color(0xFFF59E0B),
      sparkleColor = Color(0xFF38BDF8),
      tertiaryAccent = Color(0xFFE2E8F0),
      ornamentKind = HolidayOrnamentKind.HERITAGE_GEAR,
    )
    VisualStyleMode.HOLIDAY_HALLOWEEN -> HolidayDecorationSpec(
      mode = mode,
      motifTitle = "Jack-O'-Lanterns, Spiderwebs, Bats & String Lights",
      trimLabel = "SPOOKY",
      ornamentColor = Color(0xFFFF6D00),
      sparkleColor = Color(0xFFA855F7),
      tertiaryAccent = Color(0xFFFDE047),
      ornamentKind = HolidayOrnamentKind.JACK_O_LANTERN,
    )
    VisualStyleMode.HOLIDAY_VETERANS_DAY -> HolidayDecorationSpec(
      mode = mode,
      motifTitle = "Valor Chevrons, Gold Star Medals & Ribbon",
      trimLabel = "VALOR",
      ornamentColor = Color(0xFFFACC15),
      sparkleColor = Color(0xFF60A5FA),
      tertiaryAccent = Color(0xFFDC2626),
      ornamentKind = HolidayOrnamentKind.VALOR_CHEVRON,
    )
    VisualStyleMode.HOLIDAY_THANKSGIVING -> HolidayDecorationSpec(
      mode = mode,
      motifTitle = "Autumn Maple Garland, Harvest Pumpkins & Lights",
      trimLabel = "HARVEST",
      ornamentColor = Color(0xFFEA580C),
      sparkleColor = Color(0xFFF59E0B),
      tertiaryAccent = Color(0xFFFACC15),
      ornamentKind = HolidayOrnamentKind.MAPLE_HARVEST,
    )
    VisualStyleMode.HOLIDAY_CHRISTMAS -> HolidayDecorationSpec(
      mode = mode,
      motifTitle = "Evergreen Garland, Glass Baubles, Fairy Lights & Snow",
      trimLabel = "NOEL",
      ornamentColor = Color(0xFFEF4444),
      sparkleColor = Color(0xFF22C55E),
      tertiaryAccent = Color(0xFFFACC15),
      ornamentKind = HolidayOrnamentKind.EVERGREEN_BAUBLE,
    )
    else -> null
  }

private fun DrawScope.drawFivePointStar(
  center: Offset,
  outerR: Float,
  innerR: Float,
  color: Color,
  filled: Boolean = false,
  strokeW: Float = 1.6f,
) {
  val path = Path()
  for (i in 0 until 10) {
    val ang = -PI / 2.0 + i * PI / 5.0
    val r = if (i % 2 == 0) outerR else innerR
    val px = center.x + (cos(ang) * r).toFloat()
    val py = center.y + (sin(ang) * r).toFloat()
    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
  }
  path.close()
  if (filled) drawPath(path, color)
  else drawPath(path, color, style = Stroke(width = strokeW, join = StrokeJoin.Round))
}

private fun DrawScope.drawHeartShape(
  center: Offset,
  size: Float,
  color: Color,
  filled: Boolean = false,
  strokeW: Float = 1.8f,
) {
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
  if (filled) drawPath(path, color)
  else drawPath(path, color, style = Stroke(width = strokeW, join = StrokeJoin.Round))
}

private fun DrawScope.drawShamrock(center: Offset, radius: Float, color: Color, stemColor: Color) {
  val leafOffset = radius * 0.46f
  val leafR = radius * 0.44f
  val angles = listOf(-90.0, 150.0, 30.0)
  angles.forEach { deg ->
    val rad = Math.toRadians(deg)
    val lx = center.x + (cos(rad) * leafOffset).toFloat()
    val ly = center.y + (sin(rad) * leafOffset).toFloat()
    drawCircle(color, radius = leafR, center = Offset(lx, ly))
  }
  val stemPath = Path().apply {
    moveTo(center.x, center.y)
    quadraticTo(center.x + radius * 0.14f, center.y + radius * 0.65f, center.x - radius * 0.25f, center.y + radius * 0.96f)
  }
  drawPath(stemPath, stemColor, style = Stroke(width = (radius * 0.18f).coerceAtLeast(1.8f), cap = StrokeCap.Round))
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
  drawLine(
    veinColor,
    Offset(center.x, center.y - r * 0.58f),
    Offset(center.x, center.y + r * 0.90f),
    strokeWidth = (r * 0.12f).coerceAtLeast(1.4f),
    cap = StrokeCap.Round,
  )
}

private fun DrawScope.drawPumpkin(
  center: Offset,
  r: Float,
  bodyColor: Color,
  stemColor: Color,
  jackFaceColor: Color? = null,
) {
  val w = r * 1.15f
  val h = r * 0.92f
  drawOval(bodyColor, topLeft = Offset(center.x - w, center.y - h), size = Size(w * 2f, h * 2f))
  drawOval(
    stemColor.copy(alpha = 0.28f),
    topLeft = Offset(center.x - w * 0.56f, center.y - h),
    size = Size(w * 1.12f, h * 2f),
    style = Stroke(width = (r * 0.12f).coerceAtLeast(1.4f)),
  )
  drawLine(
    stemColor,
    Offset(center.x, center.y - h),
    Offset(center.x + r * 0.20f, center.y - h * 1.42f),
    strokeWidth = (r * 0.22f).coerceAtLeast(2.2f),
    cap = StrokeCap.Round,
  )
  if (jackFaceColor != null) {
    val eyeY = center.y - h * 0.16f
    drawCircle(jackFaceColor, radius = r * 0.18f, center = Offset(center.x - r * 0.38f, eyeY))
    drawCircle(jackFaceColor, radius = r * 0.18f, center = Offset(center.x + r * 0.38f, eyeY))
    drawArc(
      color = jackFaceColor,
      startAngle = 15f,
      sweepAngle = 150f,
      useCenter = false,
      topLeft = Offset(center.x - r * 0.55f, center.y - h * 0.05f),
      size = Size(r * 1.1f, h * 0.72f),
      style = Stroke(width = (r * 0.18f).coerceAtLeast(1.8f), cap = StrokeCap.Round),
    )
  }
}

private fun DrawScope.drawOrnamentBauble(
  anchorTop: Offset,
  radius: Float,
  baubleColor: Color,
  capColor: Color,
  sparkleColor: Color,
  cordLength: Float = radius * 0.9f,
) {
  val center = Offset(anchorTop.x, anchorTop.y + cordLength + radius)
  // Metallic hanging string + ornament crown cap
  drawLine(capColor, anchorTop, Offset(anchorTop.x, center.y - radius), strokeWidth = (radius * 0.14f).coerceAtLeast(1.4f))
  drawRect(
    capColor,
    topLeft = Offset(center.x - radius * 0.30f, center.y - radius - radius * 0.24f),
    size = Size(radius * 0.60f, radius * 0.30f),
  )
  // Subtle glow halo behind bauble
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(baubleColor.copy(alpha = baubleColor.alpha * 0.45f), Color.Transparent),
      center = center,
      radius = radius * 1.7f,
    ),
    radius = radius * 1.7f,
    center = center,
  )
  // Glass sphere body
  drawCircle(baubleColor, radius = radius, center = center)
  // Decorative equatorial band
  drawLine(
    capColor.copy(alpha = (capColor.alpha * 0.9f).coerceIn(0f, 1f)),
    Offset(center.x - radius * 0.92f, center.y),
    Offset(center.x + radius * 0.92f, center.y),
    strokeWidth = (radius * 0.16f).coerceAtLeast(1.4f),
  )
  // Specular glass highlight arc
  drawArc(
    color = sparkleColor,
    startAngle = 195f,
    sweepAngle = 70f,
    useCenter = false,
    topLeft = Offset(center.x - radius * 0.68f, center.y - radius * 0.68f),
    size = Size(radius * 1.36f, radius * 1.36f),
    style = Stroke(width = (radius * 0.18f).coerceAtLeast(1.8f), cap = StrokeCap.Round),
  )
}

private fun DrawScope.drawHollyCluster(center: Offset, leafLen: Float, leafColor: Color, berryColor: Color) {
  val leftLeaf = Path().apply {
    moveTo(center.x, center.y)
    quadraticTo(center.x - leafLen * 0.65f, center.y - leafLen * 0.55f, center.x - leafLen, center.y - leafLen * 0.12f)
    quadraticTo(center.x - leafLen * 0.55f, center.y + leafLen * 0.42f, center.x, center.y)
    close()
  }
  val rightLeaf = Path().apply {
    moveTo(center.x, center.y)
    quadraticTo(center.x + leafLen * 0.65f, center.y - leafLen * 0.55f, center.x + leafLen, center.y - leafLen * 0.12f)
    quadraticTo(center.x + leafLen * 0.55f, center.y + leafLen * 0.42f, center.x, center.y)
    close()
  }
  drawPath(leftLeaf, leafColor)
  drawPath(rightLeaf, leafColor)
  val bR = leafLen * 0.22f
  drawCircle(berryColor, radius = bR, center = Offset(center.x, center.y - bR * 0.55f))
  drawCircle(berryColor, radius = bR, center = Offset(center.x - bR * 0.85f, center.y + bR * 0.55f))
  drawCircle(berryColor, radius = bR, center = Offset(center.x + bR * 0.85f, center.y + bR * 0.55f))
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
  drawOval(shellColor.copy(alpha = (shellColor.alpha * 0.35f).coerceIn(0f, 1f)), topLeft = Offset(center.x - rx, center.y - ry), size = Size(rx * 2f, ry * 2f))
  drawOval(shellColor, topLeft = Offset(center.x - rx, center.y - ry), size = Size(rx * 2f, ry * 2f), style = Stroke(width = (rx * 0.20f).coerceAtLeast(1.8f)))
  drawLine(stripeColor, Offset(center.x - rx * 0.85f, center.y - ry * 0.24f), Offset(center.x + rx * 0.85f, center.y - ry * 0.24f), strokeWidth = (rx * 0.20f).coerceAtLeast(1.8f))
  drawLine(stripeColor, Offset(center.x - rx * 0.85f, center.y + ry * 0.24f), Offset(center.x + rx * 0.85f, center.y + ry * 0.24f), strokeWidth = (rx * 0.20f).coerceAtLeast(1.8f))
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

private fun DrawScope.drawSnowflake(center: Offset, arm: Float, color: Color) {
  val sw = (arm * 0.16f).coerceAtLeast(1.3f)
  drawLine(color, Offset(center.x - arm, center.y), Offset(center.x + arm, center.y), strokeWidth = sw, cap = StrokeCap.Round)
  drawLine(color, Offset(center.x, center.y - arm), Offset(center.x, center.y + arm), strokeWidth = sw, cap = StrokeCap.Round)
  val diag = arm * 0.72f
  drawLine(color, Offset(center.x - diag, center.y - diag), Offset(center.x + diag, center.y + diag), strokeWidth = sw * 0.85f, cap = StrokeCap.Round)
  drawLine(color, Offset(center.x + diag, center.y - diag), Offset(center.x - diag, center.y + diag), strokeWidth = sw * 0.85f, cap = StrokeCap.Round)
  drawCircle(color, radius = arm * 0.18f, center = center)
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
          strokeWidth = (radius * 0.18f).coerceAtLeast(1.8f),
          cap = StrokeCap.Round,
        )
      }
      drawCircle(tertiary, radius = radius * 0.28f, center = center)
    }
    HolidayOrnamentKind.HEART_RIBBON -> drawHeartShape(center, radius * 1.8f, primary, filled = true)
    HolidayOrnamentKind.SHAMROCK_CLOVER -> drawShamrock(center, radius, primary, secondary)
    HolidayOrnamentKind.PAINTED_EGG -> drawEasterEgg(center, radius * 0.74f, radius, primary, secondary)
    HolidayOrnamentKind.MEMORIAL_POPPY_STAR -> {
      drawCircle(primary, radius = radius * 0.86f, center = center)
      drawFivePointStar(center, radius * 0.54f, radius * 0.24f, secondary, filled = true)
    }
    HolidayOrnamentKind.LIBERTY_STARBURST -> drawFivePointStar(center, radius, radius * 0.44f, tertiary, filled = true)
    HolidayOrnamentKind.HERITAGE_GEAR -> drawGearIcon(center, radius, primary)
    HolidayOrnamentKind.JACK_O_LANTERN -> drawPumpkin(center, radius * 0.92f, primary, secondary, tertiary)
    HolidayOrnamentKind.VALOR_CHEVRON -> {
      val path = Path().apply {
        moveTo(center.x - radius * 0.85f, center.y - radius * 0.2f)
        lineTo(center.x, center.y + radius * 0.48f)
        lineTo(center.x + radius * 0.85f, center.y - radius * 0.2f)
      }
      drawPath(path, primary, style = Stroke(width = (radius * 0.28f).coerceAtLeast(2.4f), cap = StrokeCap.Round, join = StrokeJoin.Round))
      drawFivePointStar(Offset(center.x, center.y - radius * 0.36f), radius * 0.46f, radius * 0.20f, primary, filled = true)
    }
    HolidayOrnamentKind.MAPLE_HARVEST -> drawMapleLeaf(center, radius, primary, tertiary)
    HolidayOrnamentKind.EVERGREEN_BAUBLE ->
      drawOrnamentBauble(
        anchorTop = Offset(center.x, center.y - radius * 1.15f),
        radius = radius * 0.76f,
        baubleColor = primary,
        capColor = tertiary,
        sparkleColor = Color.White.copy(alpha = 0.85f),
        cordLength = radius * 0.45f,
      )
  }
}

/**
 * Crisp DP-scaled top-of-screen festive holiday canopy: scalloped evergreen/bunting garland,
 * glowing fairy string lights, and hanging bespoke holiday ornaments across all 11 holiday themes.
 */
fun DrawScope.drawHolidayTopCanopy(
  mode: VisualStyleMode,
  twinklePhase: Float = 0f,
) {
  val spec = holidayDecorationSpec(mode) ?: return
  val w = size.width
  if (w < 10f) return

  val swagDrop = 24.dp.toPx()
  val topY = 2.dp.toPx()
  val segments = 4
  val segW = w / segments

  // Soft festive top edge glow
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(spec.ornamentColor.copy(alpha = 0.16f), Color.Transparent),
      startY = 0f,
      endY = 48.dp.toPx(),
    ),
    topLeft = Offset.Zero,
    size = Size(w, 48.dp.toPx()),
  )

  // Draped garland / bunting swag
  val swagPath = Path().apply {
    moveTo(0f, topY)
    for (i in 0 until segments) {
      val xMid = (i + 0.5f) * segW
      val xEnd = (i + 1f) * segW
      quadraticTo(xMid, topY + swagDrop, xEnd, topY)
    }
  }
  val garlandCol =
    if (mode == VisualStyleMode.HOLIDAY_CHRISTMAS) Color(0xFF15803D).copy(alpha = 0.90f)
    else spec.sparkleColor.copy(alpha = 0.78f)
  drawPath(swagPath, garlandCol, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
  drawPath(swagPath, spec.tertiaryAccent.copy(alpha = 0.65f), style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round))

  // Twinkling string lights along the garland curve
  val bulbCount = 16
  for (i in 1 until bulbCount) {
    val tGlobal = i.toFloat() / bulbCount.toFloat()
    val segIdx = (tGlobal * segments).toInt().coerceIn(0, segments - 1)
    val localT = (tGlobal * segments) - segIdx
    val bx = tGlobal * w
    // Quadratic Bezier Y on swag: (1-t)^2*y0 + 2(1-t)t*yCtrl + t^2*y1
    val by = topY + 2f * (1f - localT) * localT * swagDrop
    val pulse = 0.65f + 0.35f * sin((twinklePhase * 2f * PI + i * 0.9).toDouble()).toFloat()
    val bulbColor = when (i % 3) {
      0 -> spec.ornamentColor
      1 -> spec.tertiaryAccent
      else -> spec.sparkleColor
    }
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(bulbColor.copy(alpha = 0.50f * pulse), Color.Transparent),
        center = Offset(bx, by + 2.dp.toPx()),
        radius = 8.dp.toPx(),
      ),
      radius = 8.dp.toPx(),
      center = Offset(bx, by + 2.dp.toPx()),
    )
    drawCircle(
      color = bulbColor.copy(alpha = (0.72f + 0.28f * pulse).coerceIn(0f, 1f)),
      radius = 2.6.dp.toPx(),
      center = Offset(bx, by + 2.dp.toPx()),
    )
  }

  // Hanging bespoke ornaments suspended from each garland trough & corner
  for (s in 0 until segments) {
    val hx = (s + 0.5f) * segW
    val hy = topY + swagDrop * 0.52f
    val cordLen = (if (s % 2 == 0) 14.dp else 9.dp).toPx()
    val ornR = (if (s % 2 == 0) 9.5.dp else 8.dp).toPx()
    val pCol = if (s % 2 == 0) spec.ornamentColor.copy(alpha = 0.94f) else spec.tertiaryAccent.copy(alpha = 0.94f)
    val sCol = spec.sparkleColor.copy(alpha = 0.92f)
    val tCol = spec.tertiaryAccent.copy(alpha = 0.95f)

    if (mode == VisualStyleMode.HOLIDAY_CHRISTMAS) {
      drawOrnamentBauble(
        anchorTop = Offset(hx, hy),
        radius = ornR * 0.85f,
        baubleColor = if (s % 2 == 0) Color(0xFFEF4444) else Color(0xFFFACC15),
        capColor = Color(0xFFFDE047),
        sparkleColor = Color.White.copy(alpha = 0.85f),
        cordLength = cordLen,
      )
      if (s < segments - 1) {
        drawHollyCluster(
          center = Offset((s + 1f) * segW, topY + 4.dp.toPx()),
          leafLen = 10.dp.toPx(),
          leafColor = Color(0xFF16A34A),
          berryColor = Color(0xFFEF4444),
        )
      }
    } else {
      drawLine(
        color = spec.tertiaryAccent.copy(alpha = 0.78f),
        start = Offset(hx, hy),
        end = Offset(hx, hy + cordLen),
        strokeWidth = 1.4.dp.toPx(),
      )
      drawOrnamentByKind(
        kind = spec.ornamentKind,
        center = Offset(hx, hy + cordLen + ornR * 0.72f),
        radius = ornR,
        primary = pCol,
        secondary = sCol,
        tertiary = tCol,
      )
    }
  }
}

/**
 * Full-screen holiday wallpaper vector ornament art — DP-scaled, unmistakable motifs per holiday.
 */
fun DrawScope.drawHolidayBackgroundArt(
  mode: VisualStyleMode,
  accent: Color,
  secondary: Color,
  twinklePhase: Float = 0f,
) {
  val spec = holidayDecorationSpec(mode) ?: return
  val w = size.width
  val h = size.height
  val c1 = spec.ornamentColor.copy(alpha = 0.28f)
  val c2 = spec.sparkleColor.copy(alpha = 0.25f)
  val c3 = spec.tertiaryAccent.copy(alpha = 0.24f)

  // Always draw festive top garland canopy + hanging ornaments
  drawHolidayTopCanopy(mode = mode, twinklePhase = twinklePhase)

  when (mode) {
    VisualStyleMode.HOLIDAY_NEW_YEARS -> {
      val bursts = listOf(Offset(w * 0.20f, h * 0.22f), Offset(w * 0.80f, h * 0.28f), Offset(w * 0.50f, h * 0.72f))
      bursts.forEachIndexed { idx, center ->
        val rad = (44 + idx * 12).dp.toPx()
        for (ray in 0 until 16) {
          val ang = (PI * 2.0 * ray) / 16.0
          val col = if (ray % 2 == 0) c1 else c2
          drawLine(col, center, Offset(center.x + (cos(ang) * rad).toFloat(), center.y + (sin(ang) * rad).toFloat()), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
          if (ray % 3 == 0) {
            drawCircle(c3, radius = 2.8.dp.toPx(), center = Offset(center.x + (cos(ang) * rad * 1.14f).toFloat(), center.y + (sin(ang) * rad * 1.14f).toFloat()))
          }
        }
      }
      for (i in 0..14) {
        val bx = w * 0.10f + ((i * 197) % 100) / 100f * w * 0.80f
        val by = h - ((i * 239 + 60) % 100) / 100f * h * 0.78f
        drawCircle(c3, radius = (3 + (i % 4) * 2).dp.toPx(), center = Offset(bx, by), style = Stroke(width = 1.2.dp.toPx()))
      }
    }
    VisualStyleMode.HOLIDAY_VALENTINES -> {
      val ribbon = Path().apply {
        moveTo(0f, h * 0.16f)
        cubicTo(w * 0.3f, h * 0.24f, w * 0.7f, h * 0.08f, w, h * 0.18f)
      }
      drawPath(ribbon, c2, style = Stroke(width = 2.4.dp.toPx()))
      for (i in 0..13) {
        val hx = ((i * 193 + 53) % 1000) / 1000f * w
        val hy = h * 0.12f + ((i * 263 + 89) % 1000) / 1000f * h * 0.82f
        val sz = (12 + (i % 4) * 5).dp.toPx()
        drawHeartShape(Offset(hx, hy), sz, if (i % 2 == 0) c1 else c2, filled = i % 2 == 0)
      }
    }
    VisualStyleMode.HOLIDAY_ST_PATRICKS -> {
      for (i in 0..12) {
        val sx = ((i * 211 + 47) % 1000) / 1000f * w
        val sy = h * 0.12f + ((i * 283 + 61) % 1000) / 1000f * h * 0.82f
        drawShamrock(Offset(sx, sy), (10 + (i % 3) * 4).dp.toPx(), c1, c2)
      }
      for (ring in 1..3) {
        drawCircle(c2, radius = (22 * ring).dp.toPx(), center = Offset(w * 0.88f, h * 0.18f), style = Stroke(width = 1.5.dp.toPx()))
      }
    }
    VisualStyleMode.HOLIDAY_EASTER -> {
      val hill = Path().apply {
        moveTo(0f, h * 0.88f)
        quadraticTo(w * 0.35f, h * 0.78f, w * 0.72f, h * 0.86f)
        quadraticTo(w * 0.88f, h * 0.90f, w, h * 0.83f)
      }
      drawPath(hill, c2, style = Stroke(width = 2.4.dp.toPx()))
      for (i in 0..12) {
        val ex = ((i * 227 + 43) % 1000) / 1000f * w
        val ey = h * 0.12f + ((i * 307 + 79) % 1000) / 1000f * h * 0.76f
        val rx = (8 + (i % 3) * 3).dp.toPx()
        drawEasterEgg(Offset(ex, ey), rx, rx * 1.38f, if (i % 2 == 0) c1 else c2, c3)
      }
    }
    VisualStyleMode.HOLIDAY_MEMORIAL_DAY -> {
      for (i in 0..13) {
        val sx = ((i * 199 + 37) % 1000) / 1000f * w
        val sy = h * 0.14f + ((i * 277 + 83) % 1000) / 1000f * h * 0.78f
        if (i % 2 == 0) {
          drawFivePointStar(Offset(sx, sy), (9 + (i % 3) * 3).dp.toPx(), (4 + (i % 3) * 1.4f).dp.toPx(), c2, filled = true)
        } else {
          drawCircle(c1, radius = (8 + (i % 3) * 3).dp.toPx(), center = Offset(sx, sy))
          drawCircle(Color.Black.copy(alpha = 0.22f), radius = 3.dp.toPx(), center = Offset(sx, sy))
        }
      }
    }
    VisualStyleMode.HOLIDAY_FOURTH_JULY -> {
      val bursts = listOf(Offset(w * 0.22f, h * 0.24f), Offset(w * 0.80f, h * 0.28f), Offset(w * 0.45f, h * 0.74f))
      bursts.forEachIndexed { idx, c ->
        val rad = (48 + idx * 10).dp.toPx()
        for (ray in 0 until 14) {
          val ang = (PI * 2.0 * ray) / 14.0
          val col = when (ray % 3) {
            0 -> c1
            1 -> c2
            else -> c3
          }
          drawLine(col, c, Offset(c.x + (cos(ang) * rad).toFloat(), c.y + (sin(ang) * rad).toFloat()), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
      }
      for (i in 0..11) {
        val sx = ((i * 239 + 61) % 1000) / 1000f * w
        val sy = h * 0.14f + ((i * 313 + 47) % 1000) / 1000f * h * 0.78f
        drawFivePointStar(Offset(sx, sy), 10.dp.toPx(), 4.5.dp.toPx(), if (i % 2 == 0) c3 else c2, filled = true)
      }
    }
    VisualStyleMode.HOLIDAY_LABOR_DAY -> {
      for (i in 0..9) {
        val gx = ((i * 241 + 59) % 1000) / 1000f * w
        val gy = h * 0.14f + ((i * 317 + 71) % 1000) / 1000f * h * 0.78f
        drawGearIcon(Offset(gx, gy), (12 + (i % 3) * 5).dp.toPx(), if (i % 2 == 0) c1 else c2)
      }
    }
    VisualStyleMode.HOLIDAY_HALLOWEEN -> {
      val webOrigin = Offset(0f, h * 0.15f)
      for (r in 1..5) {
        drawCircle(c2, radius = (r * 24).dp.toPx(), center = webOrigin, style = Stroke(width = 1.2.dp.toPx()))
      }
      for (spoke in 0..6) {
        val ang = Math.toRadians((spoke * 15).toDouble() - 15.0)
        drawLine(c2, webOrigin, Offset(webOrigin.x + (cos(ang) * 130.dp.toPx()).toFloat(), webOrigin.y + (sin(ang) * 130.dp.toPx()).toFloat()), strokeWidth = 1.1.dp.toPx())
      }
      drawCircle(c1, radius = w * 0.16f, center = Offset(w * 0.85f, h * 0.20f))
      drawBatSilhouette(Offset(w * 0.70f, h * 0.25f), 22.dp.toPx(), c2)
      drawBatSilhouette(Offset(w * 0.24f, h * 0.45f), 18.dp.toPx(), c2)
      drawPumpkin(Offset(w * 0.82f, h * 0.78f), 20.dp.toPx(), c1, c2, c3)
      drawPumpkin(Offset(w * 0.18f, h * 0.74f), 16.dp.toPx(), c1, c2, c3)
    }
    VisualStyleMode.HOLIDAY_VETERANS_DAY -> {
      for (band in 0..2) {
        val yBase = h * (0.22f + band * 0.26f)
        val chev = Path().apply {
          moveTo(w * 0.1f, yBase)
          lineTo(w * 0.5f, yBase + 32.dp.toPx())
          lineTo(w * 0.9f, yBase)
        }
        drawPath(chev, if (band % 2 == 0) c1 else c2, style = Stroke(width = 2.4.dp.toPx()))
      }
      for (i in 0..7) {
        drawFivePointStar(Offset(w * (0.12f + i * 0.11f), h * 0.18f), 8.dp.toPx(), 3.6.dp.toPx(), c1, filled = true)
      }
    }
    VisualStyleMode.HOLIDAY_THANKSGIVING -> {
      for (i in 0..14) {
        val lx = ((i * 193 + 57) % 1000) / 1000f * w
        val ly = h * 0.14f + ((i * 271 + 83) % 1000) / 1000f * h * 0.78f
        drawMapleLeaf(Offset(lx, ly), (10 + (i % 4) * 3.5f).dp.toPx(), if (i % 2 == 0) c1 else c2, c3)
      }
      drawPumpkin(Offset(w * 0.84f, h * 0.82f), 20.dp.toPx(), c1, c3)
      drawPumpkin(Offset(w * 0.16f, h * 0.80f), 16.dp.toPx(), c2, c3)
    }
    VisualStyleMode.HOLIDAY_CHRISTMAS -> {
      val snowCol = Color.White.copy(alpha = 0.26f)
      for (i in 0..26) {
        val sx = ((i * 179 + 31) % 1000) / 1000f * w
        val sy = h * 0.12f + ((i * 257 + 67) % 1000) / 1000f * h * 0.84f
        val arm = (4.5f + (i % 3) * 2.5f).dp.toPx()
        drawSnowflake(Offset(sx, sy), arm, snowCol)
      }
    }
    else -> Unit
  }
}

/**
 * Renders festive DP-scaled garland trim, fairy bulbs, and corner ornaments directly onto TacticalCard frames.
 */
fun DrawScope.drawHolidayCardOrnament(
  mode: VisualStyleMode,
  highlight: Boolean,
) {
  val spec = holidayDecorationSpec(mode) ?: return
  val w = size.width
  val h = size.height
  if (w < 20f || h < 20f) return
  val alpha = if (highlight) 0.82f else 0.58f
  val p = spec.ornamentColor.copy(alpha = alpha)
  val s = spec.sparkleColor.copy(alpha = alpha)
  val t = spec.tertiaryAccent.copy(alpha = alpha)

  // Top decorative garland swag / bunting line
  val stepTarget = 44.dp.toPx().coerceAtLeast(24f)
  val steps = ((w / stepTarget).toInt()).coerceIn(3, 10)
  val stepW = w / steps
  val swagY = 2.dp.toPx()
  val dropY = 8.dp.toPx()
  val garlandPath = Path().apply {
    moveTo(0f, swagY)
    for (i in 0 until steps) {
      val xMid = (i + 0.5f) * stepW
      val xEnd = (i + 1f) * stepW
      quadraticTo(xMid, dropY, xEnd, swagY)
    }
  }
  drawPath(garlandPath, s, style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round))

  // Glowing ornament dots along garland scallops
  for (i in 1 until steps) {
    drawCircle(
      color = if (i % 2 == 0) p else t,
      radius = 2.4.dp.toPx(),
      center = Offset(i * stepW, 3.2.dp.toPx()),
    )
  }

  // Bespoke holiday corner ornaments (DP-scaled so clearly visible on high-DPI phones)
  val topRightInset = 15.dp.toPx()
  val bottomLeftInset = 12.dp.toPx()
  drawOrnamentByKind(
    kind = spec.ornamentKind,
    center = Offset(w - topRightInset, topRightInset),
    radius = 8.5.dp.toPx(),
    primary = p,
    secondary = s,
    tertiary = t,
  )
  drawOrnamentByKind(
    kind = spec.ornamentKind,
    center = Offset(bottomLeftInset, h - bottomLeftInset),
    radius = 6.5.dp.toPx(),
    primary = s,
    secondary = p,
    tertiary = t,
  )
}

/**
 * Renders festive DP-scaled seasonal holiday trim, fairy lights, and hanging ornaments on top border of active Nav Bars.
 */
fun DrawScope.drawHolidayNavOrnament(mode: VisualStyleMode) {
  val spec = holidayDecorationSpec(mode) ?: return
  val w = size.width
  if (w < 20f) return
  val p = spec.ornamentColor.copy(alpha = 0.88f)
  val s = spec.sparkleColor.copy(alpha = 0.88f)
  val t = spec.tertiaryAccent.copy(alpha = 0.90f)

  val segments = 8
  val segW = w / segments
  val swag = Path().apply {
    moveTo(0f, 1.5.dp.toPx())
    for (i in 0 until segments) {
      quadraticTo((i + 0.5f) * segW, 8.dp.toPx(), (i + 1f) * segW, 1.5.dp.toPx())
    }
  }
  drawPath(swag, s, style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round))
  for (i in 1 until segments) {
    val cx = i * segW
    if (i == 1 || i == segments / 2 || i == segments - 1) {
      drawOrnamentByKind(spec.ornamentKind, Offset(cx, 8.5.dp.toPx()), 5.8.dp.toPx(), p, s, t)
    } else {
      drawCircle(if (i % 2 == 0) p else t, radius = 2.4.dp.toPx(), center = Offset(cx, 4.dp.toPx()))
    }
  }
}
