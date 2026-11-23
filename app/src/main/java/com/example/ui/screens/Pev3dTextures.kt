package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.opengl.GLES20
import android.opengl.GLUtils
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal enum class PevPbrMaterialId {
  CARBON_WEAVE,
  BRUSHED_METAL,
  TIRE_RUBBER,
  BATTERY_PACK,
  BRAKE_ROTOR,
  QUILTED_LEATHER,
  TFT_SCREEN,
  DYNO_FLOOR,
  RACING_LIVERY,
  HEADLIGHT_OPTIC,
  TAILLIGHT_OPTIC,
  AMBER_SIGNAL_OPTIC,
}

internal data class PevPbrTextureBundle(
  val albedoPng: ByteArray,
  val normalPng: ByteArray,
  val metallicRoughnessPng: ByteArray,
  val occlusionPng: ByteArray,
)

internal object Pev3dPbrTextures {
  private val cache = HashMap<PevPbrMaterialId, PevPbrTextureBundle>()

  fun getBundle(id: PevPbrMaterialId): PevPbrTextureBundle {
    synchronized(cache) {
      return cache.getOrPut(id) { buildBundle(id) }
    }
  }

  private fun buildBundle(id: PevPbrMaterialId): PevPbrTextureBundle {
    return when (id) {
      PevPbrMaterialId.CARBON_WEAVE -> buildCarbonPbrBundle(512)
      PevPbrMaterialId.BRUSHED_METAL -> buildBrushedMetalPbrBundle(512)
      PevPbrMaterialId.TIRE_RUBBER -> buildTireTreadPbrBundle(512)
      PevPbrMaterialId.BATTERY_PACK -> buildBatteryPackPbrBundle(512)
      PevPbrMaterialId.BRAKE_ROTOR -> buildBrakeRotorPbrBundle(512)
      PevPbrMaterialId.QUILTED_LEATHER -> buildQuiltedLeatherPbrBundle(512)
      PevPbrMaterialId.TFT_SCREEN -> buildTftPbrBundle(512)
      PevPbrMaterialId.DYNO_FLOOR -> buildDynoFloorPbrBundle(1024)
      PevPbrMaterialId.RACING_LIVERY -> buildRacingLiveryPbrBundle(512)
      PevPbrMaterialId.HEADLIGHT_OPTIC -> buildHeadlightOpticPbrBundle(512)
      PevPbrMaterialId.TAILLIGHT_OPTIC -> buildTaillightOpticPbrBundle(512)
      PevPbrMaterialId.AMBER_SIGNAL_OPTIC -> buildAmberSignalPbrBundle(256)
    }
  }

  private fun bitmapToPng(bmp: Bitmap): ByteArray {
    val out = ByteArrayOutputStream(48 * 1024)
    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
    bmp.recycle()
    return out.toByteArray()
  }

  private fun heightFieldToNormalBitmap(heights: FloatArray, size: Int, strength: Float): Bitmap {
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
      val ym = (y - 1 + size) % size
      val yp = (y + 1) % size
      for (x in 0 until size) {
        val xm = (x - 1 + size) % size
        val xp = (x + 1) % size
        val dx = (heights[y * size + xp] - heights[y * size + xm]) * strength
        val dy = (heights[yp * size + x] - heights[ym * size + x]) * strength
        var nx = -dx
        var ny = -dy
        var nz = 1f
        val inv = 1f / sqrt(nx * nx + ny * ny + nz * nz)
        nx *= inv
        ny *= inv
        nz *= inv
        val r = ((nx * 0.5f + 0.5f) * 255f).toInt().coerceIn(0, 255)
        val g = ((ny * 0.5f + 0.5f) * 255f).toInt().coerceIn(0, 255)
        val b = ((nz * 0.5f + 0.5f) * 255f).toInt().coerceIn(0, 255)
        pixels[y * size + x] = Color.rgb(r, g, b)
      }
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
      setPixels(pixels, 0, size, 0, 0, size, size)
    }
  }

  private fun heightFieldToOcclusionBitmap(
    heights: FloatArray,
    size: Int,
    radiusPx: Int = 3,
    strength: Float = 1.45f,
    floorMin: Float = 0.38f,
  ): Bitmap {
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
      val ym = (y - radiusPx + size) % size
      val yp = (y + radiusPx) % size
      for (x in 0 until size) {
        val xm = (x - radiusPx + size) % size
        val xp = (x + radiusPx) % size
        val centerH = heights[y * size + x]
        val avgNeighbor =
          (heights[ym * size + x] + heights[yp * size + x] + heights[y * size + xm] + heights[y * size + xp]) * 0.25f
        val cavity = (avgNeighbor - centerH).coerceAtLeast(0f)
        val ao = (1f - cavity * strength).coerceIn(floorMin, 1f)
        val c = (ao * 255f).toInt().coerceIn(0, 255)
        pixels[y * size + x] = Color.rgb(c, c, c)
      }
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
      setPixels(pixels, 0, size, 0, 0, size, size)
    }
  }

  private fun buildMrBitmap(size: Int, compute: (x: Int, y: Int) -> Pair<Float, Float>): Bitmap {
    // glTF 2.0 MetallicRoughness map: G = roughness (0..1), B = metallic (0..1), R = occlusion/unused (255)
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
      for (x in 0 until size) {
        val (metallic, roughness) = compute(x, y)
        val g = (roughness.coerceIn(0.04f, 1f) * 255f).toInt()
        val b = (metallic.coerceIn(0f, 1f) * 255f).toInt()
        pixels[y * size + x] = Color.rgb(255, g, b)
      }
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
      setPixels(pixels, 0, size, 0, 0, size, size)
    }
  }

  private fun buildCarbonPbrBundle(size: Int): PevPbrTextureBundle {
    val albedoBmp = Pev3dTextureBank.buildCarbonWeaveBitmap(size)
    val heights = FloatArray(size * size)
    val mrPixels = IntArray(size * size)
    val cell = 16
    for (y in 0 until size) {
      val ty = y / cell
      val fy = (y % cell) / cell.toFloat()
      for (x in 0 until size) {
        val tx = x / cell
        val fx = (x % cell) / cell.toFloat()
        val twill = (tx + ty) and 3
        val isWarp = twill < 2
        val h =
          if (isWarp) sin(fy * PI).toFloat() * (0.75f + 0.25f * sin(fx * PI).toFloat())
          else sin(fx * PI).toFloat() * (0.65f + 0.25f * sin(fy * PI).toFloat())
        heights[y * size + x] = h
        val rough = if (isWarp) 0.14f + (1f - h) * 0.14f else 0.22f + (1f - h) * 0.16f
        val metal = 0.18f
        mrPixels[y * size + x] = Color.rgb(255, (rough * 255f).toInt().coerceIn(20, 255), (metal * 255f).toInt())
      }
    }
    val normalBmp = heightFieldToNormalBitmap(heights, size, 3.8f)
    val aoBmp = heightFieldToOcclusionBitmap(heights, size, radiusPx = 3, strength = 0.85f, floorMin = 0.55f)
    val mrBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
      setPixels(mrPixels, 0, size, 0, 0, size, size)
    }
    return PevPbrTextureBundle(bitmapToPng(albedoBmp), bitmapToPng(normalBmp), bitmapToPng(mrBmp), bitmapToPng(aoBmp))
  }

  private fun buildBrushedMetalPbrBundle(size: Int): PevPbrTextureBundle {
    val albedoBmp = Pev3dTextureBank.buildBrushedMetalBitmap(size)
    val heights = FloatArray(size * size)
    var seed = 91823
    val rowBias = FloatArray(size)
    for (y in 0 until size) {
      seed = (seed * 1103515245 + 12345) and 0x7fffffff
      rowBias[y] = (seed / 2147483647f - 0.5f)
    }
    for (y in 0 until size) {
      val seam = if (y % 128 < 3) -0.48f else 0f
      for (x in 0 until size) {
        heights[y * size + x] = rowBias[y] * 0.35f + 0.08f * sin(x * 0.14f) + seam
      }
    }
    val normalBmp = heightFieldToNormalBitmap(heights, size, 2.5f)
    val aoBmp = heightFieldToOcclusionBitmap(heights, size, radiusPx = 2, strength = 1.15f, floorMin = 0.58f)
    val mrBmp = buildMrBitmap(size) { x, y ->
      val streak = abs(rowBias[y]) * 0.12f
      val seam = if (y % 128 < 3) 0.25f else 0f
      val metal = if (y % 128 < 3) 0.72f else 0.94f
      val rough = (0.16f + streak + seam + 0.03f * sin(x * 0.08f)).coerceIn(0.11f, 0.55f)
      metal to rough
    }
    return PevPbrTextureBundle(bitmapToPng(albedoBmp), bitmapToPng(normalBmp), bitmapToPng(mrBmp), bitmapToPng(aoBmp))
  }

  private fun buildTireTreadPbrBundle(size: Int): PevPbrTextureBundle {
    val albedoBmp = Pev3dTextureBank.buildTireTreadBitmap(size)
    val heights = FloatArray(size * size)
    val mrPixels = IntArray(size * size)
    val rows = 8
    val cols = 6
    val cellH = size / rows.toFloat()
    val cellW = size / cols.toFloat()
    for (y in 0 until size) {
      val r = (y / cellH).toInt().coerceIn(0, rows - 1)
      val ly = (y - r * cellH) / cellH
      val stagger = if (r % 2 == 0) 0f else 0.5f
      for (x in 0 until size) {
        val fx = (x / cellW + stagger)
        val lx = fx - kotlin.math.floor(fx)
        val inKnob = lx in 0.14f..0.86f && ly in 0.14f..0.86f
        val sipe = abs(ly - 0.5f) < 0.045f && lx in 0.2f..0.8f
        val edgeDist = minOf(lx - 0.14f, 0.86f - lx, ly - 0.14f, 0.86f - ly)
        val h =
          when {
            sipe -> 0.15f
            inKnob -> (edgeDist * 8f).coerceIn(0.2f, 1.0f)
            else -> 0.0f
          }
        val micro = (((x * 29 + y * 53) and 15) / 15f) * 0.06f
        heights[y * size + x] = h + micro
        val rough = if (inKnob && !sipe) 0.70f else 0.92f
        mrPixels[y * size + x] = Color.rgb(255, (rough * 255f).toInt(), 8)
      }
    }
    val normalBmp = heightFieldToNormalBitmap(heights, size, 5.2f)
    val aoBmp = heightFieldToOcclusionBitmap(heights, size, radiusPx = 5, strength = 1.65f, floorMin = 0.34f)
    val mrBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
      setPixels(mrPixels, 0, size, 0, 0, size, size)
    }
    return PevPbrTextureBundle(bitmapToPng(albedoBmp), bitmapToPng(normalBmp), bitmapToPng(mrBmp), bitmapToPng(aoBmp))
  }

  private fun buildBatteryPackPbrBundle(size: Int): PevPbrTextureBundle {
    val albedoBmp = Pev3dTextureBank.buildBatteryPackBitmap(size)
    val heights = FloatArray(size * size)
    val mrPixels = IntArray(size * size)
    val step = size / 10f
    for (y in 0 until size) {
      val row = (y / (step * 0.86f)).toInt()
      val cy = (row + 0.5f) * (step * 0.86f)
      val offset = if (row % 2 == 0) 0f else step * 0.5f
      for (x in 0 until size) {
        val col = ((x - offset) / step).toInt()
        val cx = col * step + step * 0.5f + offset
        val d = sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy)) / (step * 0.42f)
        val bus = abs(y - size * 0.25f) < size * 0.025f || abs(y - size * 0.75f) < size * 0.025f
        val h =
          when {
            bus -> 0.85f
            d < 0.78f -> 0.55f
            d < 0.98f -> 0.25f
            else -> 0.05f
          }
        heights[y * size + x] = h
        val metallic = if (bus) 0.94f else if (d < 0.95f) 0.52f else 0.20f
        val rough = if (bus) 0.16f else if (d < 0.95f) 0.32f else 0.65f
        mrPixels[y * size + x] = Color.rgb(255, (rough * 255f).toInt(), (metallic * 255f).toInt())
      }
    }
    val normalBmp = heightFieldToNormalBitmap(heights, size, 4.0f)
    val aoBmp = heightFieldToOcclusionBitmap(heights, size, radiusPx = 4, strength = 1.40f, floorMin = 0.42f)
    val mrBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
      setPixels(mrPixels, 0, size, 0, 0, size, size)
    }
    return PevPbrTextureBundle(bitmapToPng(albedoBmp), bitmapToPng(normalBmp), bitmapToPng(mrBmp), bitmapToPng(aoBmp))
  }

  private fun buildBrakeRotorPbrBundle(size: Int): PevPbrTextureBundle {
    val albedoBmp = Pev3dTextureBank.buildBrakeRotorBitmap(size)
    val heights = FloatArray(size * size)
    val mrPixels = IntArray(size * size)
    val cx = size * 0.5f
    val cy = size * 0.5f
    for (y in 0 until size) {
      for (x in 0 until size) {
        val dx = x - cx
        val dy = y - cy
        val r = sqrt(dx * dx + dy * dy) / (size * 0.5f)
        val groove = 0.08f * sin(r * 160f)
        val ang = kotlin.math.atan2(dy, dx)
        var hole = false
        for (band in 0..2) {
          val rad = 0.52f + band * 0.14f
          val phase = ((ang + band * 0.14f) / (2.0 * PI) * 24.0)
          val nearest = kotlin.math.round(phase) * (2.0 * PI / 24.0) - band * 0.14f
          val hx = cx + (rad * size * 0.5f) * cos(nearest).toFloat()
          val hy = cy + (rad * size * 0.5f) * sin(nearest).toFloat()
          if (sqrt((x - hx) * (x - hx) + (y - hy) * (y - hy)) < size * 0.015f) {
            hole = true
            break
          }
        }
        val carrierBand = r in 0.22f..0.44f
        val bobbinAngle = kotlin.math.round(ang / (2.0 * PI) * 8.0) * (2.0 * PI / 8.0)
        val bx = cx + (0.45f * size * 0.5f) * cos(bobbinAngle).toFloat()
        val by = cy + (0.45f * size * 0.5f) * sin(bobbinAngle).toFloat()
        val isBobbin = sqrt((x - bx) * (x - bx) + (y - by) * (y - by)) < size * 0.028f
        heights[y * size + x] = when {
          hole -> -0.82f
          isBobbin -> 0.42f
          carrierBand -> -0.18f
          else -> groove
        }
        val metal = if (hole) 0.15f else if (isBobbin) 0.96f else 0.94f
        val rough = if (hole) 0.85f else if (carrierBand) 0.24f else (0.14f + abs(groove))
        mrPixels[y * size + x] = Color.rgb(255, (rough * 255f).toInt(), (metal * 255f).toInt())
      }
    }
    val normalBmp = heightFieldToNormalBitmap(heights, size, 3.4f)
    val aoBmp = heightFieldToOcclusionBitmap(heights, size, radiusPx = 3, strength = 1.50f, floorMin = 0.32f)
    val mrBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
      setPixels(mrPixels, 0, size, 0, 0, size, size)
    }
    return PevPbrTextureBundle(bitmapToPng(albedoBmp), bitmapToPng(normalBmp), bitmapToPng(mrBmp), bitmapToPng(aoBmp))
  }

  private fun buildQuiltedLeatherPbrBundle(size: Int): PevPbrTextureBundle {
    val albedoPixels = IntArray(size * size)
    val heights = FloatArray(size * size)
    val mrPixels = IntArray(size * size)
    val period = 36f
    for (y in 0 until size) {
      for (x in 0 until size) {
        val u = (x + y) / period
        val v = (x - y + size * 2) / period
        val du = abs(u - kotlin.math.round(u))
        val dv = abs(v - kotlin.math.round(v))
        val stitch = minOf(du, dv)
        val seam = (stitch < 0.075f)
        val puff = sqrt((du * dv).coerceAtLeast(0f)) * 0.68f
        val grain = (((x * 19 + y * 41) and 7) - 3) * 0.012f
        val lum = if (seam) 0.09f else (0.15f + puff * 0.08f + grain)
        val c = (lum.coerceIn(0.06f, 0.32f) * 255f).toInt()
        albedoPixels[y * size + x] = Color.rgb(c, c, (c * 1.06f).toInt().coerceAtMost(255))
        heights[y * size + x] = if (seam) 0.04f else 0.35f + puff + grain * 2f
        val rough = if (seam) 0.88f else 0.64f
        mrPixels[y * size + x] = Color.rgb(255, (rough * 255f).toInt(), 5)
      }
    }
    val albedoBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
      setPixels(albedoPixels, 0, size, 0, 0, size, size)
    }
    val normalBmp = heightFieldToNormalBitmap(heights, size, 3.4f)
    val aoBmp = heightFieldToOcclusionBitmap(heights, size, radiusPx = 4, strength = 1.60f, floorMin = 0.38f)
    val mrBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
      setPixels(mrPixels, 0, size, 0, 0, size, size)
    }
    return PevPbrTextureBundle(bitmapToPng(albedoBmp), bitmapToPng(normalBmp), bitmapToPng(mrBmp), bitmapToPng(aoBmp))
  }

  private fun buildTftPbrBundle(size: Int): PevPbrTextureBundle {
    val albedoBmp = Pev3dTextureBank.buildTftCockpitBitmap(size)
    val heights = FloatArray(size * size) { 0.5f }
    val normalBmp = heightFieldToNormalBitmap(heights, size, 1.0f)
    val aoBmp = heightFieldToOcclusionBitmap(heights, size, radiusPx = 2, strength = 0.5f, floorMin = 0.85f)
    val mrBmp = buildMrBitmap(size) { _, _ -> 0.08f to 0.08f }
    return PevPbrTextureBundle(bitmapToPng(albedoBmp), bitmapToPng(normalBmp), bitmapToPng(mrBmp), bitmapToPng(aoBmp))
  }

  private fun buildDynoFloorPbrBundle(size: Int): PevPbrTextureBundle {
    val albedoBmp = Pev3dTextureBank.buildDynoFloorBitmap(size)
    val heights = FloatArray(size * size)
    val mrPixels = IntArray(size * size)
    val cx = size * 0.5f
    val cy = size * 0.5f
    for (y in 0 until size) {
      for (x in 0 until size) {
        val dx = x - cx
        val dy = y - cy
        val r = sqrt(dx * dx + dy * dy) / (size * 0.5f)
        val ring1 = abs(r - 0.72f) < 0.012f
        val ring2 = abs(r - 0.88f) < 0.008f
        val grid = (x % (size / 16) < 2) || (y % (size / 16) < 2)
        val grain = (((x * 13 + y * 31) and 15) / 15f - 0.5f) * 0.08f
        heights[y * size + x] = (if (ring1 || ring2 || grid) -0.25f else 0.1f) + grain
        val rough = if (ring1 || ring2) 0.16f else if (r < 0.70f) 0.30f else 0.50f
        val metal = if (ring1 || ring2) 0.82f else 0.24f
        mrPixels[y * size + x] = Color.rgb(255, (rough * 255f).toInt(), (metal * 255f).toInt())
      }
    }
    val normalBmp = heightFieldToNormalBitmap(heights, size, 2.2f)
    val aoBmp = heightFieldToOcclusionBitmap(heights, size, radiusPx = 3, strength = 1.25f, floorMin = 0.45f)
    val mrBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
      setPixels(mrPixels, 0, size, 0, 0, size, size)
    }
    return PevPbrTextureBundle(bitmapToPng(albedoBmp), bitmapToPng(normalBmp), bitmapToPng(mrBmp), bitmapToPng(aoBmp))
  }

  private fun buildRacingLiveryPbrBundle(size: Int): PevPbrTextureBundle {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    // Satin forged-carbon & metallic clearcoat livery base
    canvas.drawColor(Color.rgb(225, 232, 240))
    val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(178, 190, 206) }
    val darkGraphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(26, 32, 44) }
    val accentStripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 255, 255) }
    val ventPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(14, 18, 26) }
    val seamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(36, 44, 58)
      style = Paint.Style.STROKE
      strokeWidth = size / 128f
    }

    // Precision aero speed chevrons & contrast livery panels
    val s = size.toFloat()
    val pathDark = android.graphics.Path().apply {
      moveTo(0f, s * 0.62f)
      lineTo(s * 0.56f, 0f)
      lineTo(s * 0.84f, 0f)
      lineTo(s * 0.22f, s)
      lineTo(0f, s)
      close()
    }
    canvas.drawPath(pathDark, darkGraphPaint)

    val pathAccent = android.graphics.Path().apply {
      moveTo(s * 0.24f, s)
      lineTo(s * 0.86f, 0f)
      lineTo(s * 0.94f, 0f)
      lineTo(s * 0.34f, s)
      close()
    }
    canvas.drawPath(pathAccent, accentStripePaint)

    // Side cooling louver slats
    for (i in 0..4) {
      val vx = s * (0.58f + i * 0.068f)
      val vyTop = s * (0.42f + i * 0.03f)
      canvas.drawRoundRect(vx, vyTop, vx + s * 0.036f, vyTop + s * 0.26f, 6f, 6f, ventPaint)
    }

    // Stencil spec callout graphics + racing number roundel
    canvas.drawCircle(s * 0.24f, s * 0.26f, s * 0.14f, panelPaint)
    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(18, 24, 34)
      textSize = s * 0.075f
      isFakeBoldText = true
      textAlign = Paint.Align.CENTER
    }
    canvas.drawText("EV-PRO", s * 0.24f, s * 0.28f, badgePaint)
    badgePaint.color = Color.rgb(240, 246, 255)
    badgePaint.textSize = s * 0.044f
    canvas.drawText("VOLT // APEX 74", s * 0.68f, s * 0.86f, badgePaint)

    // Panel seam lines & stainless fastener bolts
    canvas.drawRoundRect(s * 0.04f, s * 0.04f, s * 0.96f, s * 0.96f, s * 0.05f, s * 0.05f, seamPaint)
    canvas.drawLine(s * 0.04f, s * 0.50f, s * 0.96f, s * 0.50f, seamPaint)
    val boltCoords = listOf(0.08f to 0.08f, 0.92f to 0.08f, 0.08f to 0.92f, 0.92f to 0.92f, 0.50f to 0.08f, 0.50f to 0.92f)
    val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(210, 220, 232) }
    boltCoords.forEach { (bx, by) ->
      canvas.drawCircle(s * bx, s * by, s * 0.018f, ventPaint)
      canvas.drawCircle(s * bx, s * by, s * 0.012f, boltPaint)
    }

    val heights = FloatArray(size * size)
    val mrPixels = IntArray(size * size)
    for (y in 0 until size) {
      val yf = y / s
      for (x in 0 until size) {
        val xf = x / s
        val edge = xf < 0.045f || xf > 0.955f || yf < 0.045f || yf > 0.955f || abs(yf - 0.50f) < 0.008f
        val inVent = xf in 0.57f..0.92f && yf in 0.42f..0.78f && (((xf - 0.58f) / 0.068f) % 1f < 0.55f)
        val weave = sin(x * 0.4f) * sin(y * 0.4f) * 0.06f
        val h = when {
          edge -> -0.45f
          inVent -> -0.65f
          else -> 0.12f + weave
        }
        heights[y * size + x] = h
        val rough = if (inVent) 0.72f else if (edge) 0.55f else 0.14f
        val metal = if (inVent) 0.20f else 0.42f
        mrPixels[y * size + x] = Color.rgb(255, (rough * 255f).toInt(), (metal * 255f).toInt())
      }
    }
    val normalBmp = heightFieldToNormalBitmap(heights, size, 3.6f)
    val aoBmp = heightFieldToOcclusionBitmap(heights, size, radiusPx = 4, strength = 1.50f, floorMin = 0.36f)
    val mrBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
      setPixels(mrPixels, 0, size, 0, 0, size, size)
    }
    return PevPbrTextureBundle(bitmapToPng(bmp), bitmapToPng(normalBmp), bitmapToPng(mrBmp), bitmapToPng(aoBmp))
  }

  private fun buildHeadlightOpticPbrBundle(size: Int): PevPbrTextureBundle {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val s = size.toFloat()
    canvas.drawColor(Color.rgb(16, 22, 32))

    // Chrome inner reflector bowl & hex-mesh surround
    val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(42, 54, 72)
      strokeWidth = s / 160f
    }
    for (i in 0..16) {
      val p = i * (s / 16f)
      canvas.drawLine(p, 0f, p, s, gridPaint)
      canvas.drawLine(0f, p, s, p, gridPaint)
    }

    // Outer C-shaped Neon DRL Halo Perimeter
    val drlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = s * 0.048f
      color = Color.rgb(230, 248, 255)
    }
    canvas.drawRoundRect(s * 0.08f, s * 0.12f, s * 0.92f, s * 0.88f, s * 0.14f, s * 0.14f, drlPaint)

    // Twin Projector LED Lenses (left + right)
    val centers = listOf(s * 0.31f to s * 0.50f, s * 0.69f to s * 0.50f)
    centers.forEach { (cx, cy) ->
      val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
          cx,
          cy,
          s * 0.18f,
          intArrayOf(Color.rgb(255, 255, 250), Color.rgb(185, 232, 255), Color.rgb(28, 42, 62)),
          floatArrayOf(0f, 0.52f, 1f),
          Shader.TileMode.CLAMP,
        )
      }
      canvas.drawCircle(cx, cy, s * 0.18f, glow)
      val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = s * 0.022f
        color = Color.rgb(255, 255, 255)
      }
      canvas.drawCircle(cx, cy, s * 0.14f, ring)
    }

    val heights = FloatArray(size * size)
    val mrPixels = IntArray(size * size)
    for (y in 0 until size) {
      val ny = y / s
      for (x in 0 until size) {
        val nx = x / s
        val dL = sqrt((nx - 0.31f) * (nx - 0.31f) + (ny - 0.50f) * (ny - 0.50f))
        val dR = sqrt((nx - 0.69f) * (nx - 0.69f) + (ny - 0.50f) * (ny - 0.50f))
        val dome = maxOf((0.18f - dL).coerceAtLeast(0f), (0.18f - dR).coerceAtLeast(0f)) * 4.5f
        heights[y * size + x] = dome
        mrPixels[y * size + x] = Color.rgb(255, 16, 48) // Mirror-clear polycarbonate lens (rough=0.06)
      }
    }
    val normalBmp = heightFieldToNormalBitmap(heights, size, 3.8f)
    val aoBmp = heightFieldToOcclusionBitmap(heights, size, radiusPx = 3, strength = 0.8f, floorMin = 0.60f)
    val mrBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
      setPixels(mrPixels, 0, size, 0, 0, size, size)
    }
    return PevPbrTextureBundle(bitmapToPng(bmp), bitmapToPng(normalBmp), bitmapToPng(mrBmp), bitmapToPng(aoBmp))
  }

  private fun buildTaillightOpticPbrBundle(size: Int): PevPbrTextureBundle {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val s = size.toFloat()
    canvas.drawColor(Color.rgb(42, 6, 10))

    // Smoked ruby diffuser border & horizontal LED light-blades
    val bladePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 44, 52) }
    val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 165, 150) }
    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = s * 0.04f
      color = Color.rgb(255, 62, 68)
    }
    canvas.drawRoundRect(s * 0.06f, s * 0.14f, s * 0.94f, s * 0.86f, s * 0.10f, s * 0.10f, border)
    for (col in 0 until 8) {
      val bx = s * (0.12f + col * 0.10f)
      canvas.drawRoundRect(bx, s * 0.26f, bx + s * 0.065f, s * 0.74f, 6f, 6f, bladePaint)
      canvas.drawRoundRect(bx + s * 0.015f, s * 0.34f, bx + s * 0.050f, s * 0.66f, 4f, 4f, corePaint)
    }

    val heights = FloatArray(size * size)
    for (y in 0 until size) {
      val yf = y / s
      for (x in 0 until size) {
        val xf = x / s
        val inFin = yf in 0.24f..0.76f && ((xf - 0.12f) / 0.10f).let { it - kotlin.math.floor(it) } < 0.65f
        heights[y * size + x] = if (inFin) 0.45f else 0.05f
      }
    }
    val normalBmp = heightFieldToNormalBitmap(heights, size, 3.0f)
    val aoBmp = heightFieldToOcclusionBitmap(heights, size, radiusPx = 3, strength = 1.1f, floorMin = 0.50f)
    val mrBmp = buildMrBitmap(size) { _, _ -> 0.14f to 0.08f }
    return PevPbrTextureBundle(bitmapToPng(bmp), bitmapToPng(normalBmp), bitmapToPng(mrBmp), bitmapToPng(aoBmp))
  }

  private fun buildAmberSignalPbrBundle(size: Int): PevPbrTextureBundle {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val s = size.toFloat()
    canvas.drawColor(Color.rgb(52, 24, 4))

    // Sequential amber chevron LED cells
    val amber = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 168, 18) }
    val hotCore = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 224, 120) }
    for (i in 0 until 5) {
      val cx = s * (0.14f + i * 0.17f)
      canvas.drawRoundRect(cx, s * 0.22f, cx + s * 0.11f, s * 0.78f, 5f, 5f, amber)
      canvas.drawRoundRect(cx + s * 0.025f, s * 0.32f, cx + s * 0.085f, s * 0.68f, 3f, 3f, hotCore)
    }

    val heights = FloatArray(size * size)
    for (y in 0 until size) {
      for (x in 0 until size) {
        heights[y * size + x] = 0.25f * sin(x * 0.35f) * cos(y * 0.35f)
      }
    }
    val normalBmp = heightFieldToNormalBitmap(heights, size, 2.8f)
    val aoBmp = heightFieldToOcclusionBitmap(heights, size, radiusPx = 2, strength = 1.0f, floorMin = 0.55f)
    val mrBmp = buildMrBitmap(size) { _, _ -> 0.12f to 0.07f }
    return PevPbrTextureBundle(bitmapToPng(bmp), bitmapToPng(normalBmp), bitmapToPng(mrBmp), bitmapToPng(aoBmp))
  }
}

internal class Pev3dTextureBank(
  val carbonWeave: Int,
  val brushedMetal: Int,
  val tireRubber: Int,
  val batteryPack: Int,
  val brakeRotor: Int,
  val tftScreen: Int,
  val dynoFloor: Int,
) {
  fun release() {
    val ids = intArrayOf(carbonWeave, brushedMetal, tireRubber, batteryPack, brakeRotor, tftScreen, dynoFloor)
    GLES20.glDeleteTextures(ids.size, ids, 0)
  }

  companion object {
    fun createOnGlThread(): Pev3dTextureBank {
      return Pev3dTextureBank(
        carbonWeave = uploadBitmapTexture(buildCarbonWeaveBitmap(512)),
        brushedMetal = uploadBitmapTexture(buildBrushedMetalBitmap(512)),
        tireRubber = uploadBitmapTexture(buildTireTreadBitmap(512)),
        batteryPack = uploadBitmapTexture(buildBatteryPackBitmap(512)),
        brakeRotor = uploadBitmapTexture(buildBrakeRotorBitmap(512)),
        tftScreen = uploadBitmapTexture(buildTftCockpitBitmap(512)),
        dynoFloor = uploadBitmapTexture(buildDynoFloorBitmap(1024)),
      )
    }

    private fun uploadBitmapTexture(bmp: Bitmap): Int {
      val ids = IntArray(1)
      GLES20.glGenTextures(1, ids, 0)
      val texId = ids[0]
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
      GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
      GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
      GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
      GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
      GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
      GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
      bmp.recycle()
      return texId
    }

    internal fun buildCarbonWeaveBitmap(size: Int): Bitmap {
      val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
      val pixels = IntArray(size * size)
      val cell = 16 // 2x2 twill repeat every 32px
      for (y in 0 until size) {
        val ty = y / cell
        val fy = (y % cell) / cell.toFloat()
        for (x in 0 until size) {
          val tx = x / cell
          val fx = (x % cell) / cell.toFloat()
          val twill = (tx + ty) and 3
          val isWarp = twill < 2
          val fiberWave =
            if (isWarp) {
              sin(fy * PI).toFloat() * (0.78f + 0.22f * cos(fx * 2f * PI).toFloat())
            } else {
              sin(fx * PI).toFloat() * (0.62f + 0.20f * cos(fy * 2f * PI).toFloat())
            }
          val baseLum = if (isWarp) 0.24f + 0.34f * fiberWave else 0.14f + 0.22f * fiberWave
          val micro = (((x * 37 + y * 17) and 7) - 3) * 0.004f
          val v = ((baseLum + micro).coerceIn(0.08f, 0.72f) * 255f).toInt()
          val b = (v * 1.07f).toInt().coerceIn(0, 255)
          pixels[y * size + x] = Color.rgb(v, v, b)
        }
      }
      bmp.setPixels(pixels, 0, size, 0, 0, size, size)
      return bmp
    }

    internal fun buildBrushedMetalBitmap(size: Int): Bitmap {
      val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
      val pixels = IntArray(size * size)
      var seed = 48271
      val rowBias = FloatArray(size)
      for (y in 0 until size) {
        seed = (seed * 1103515245 + 12345) and 0x7fffffff
        rowBias[y] = (seed / 2147483647f - 0.5f) * 0.16f
      }
      for (y in 0 until size) {
        val streak = rowBias[y] + 0.04f * sin(y * 0.35f)
        for (x in 0 until size) {
          val grain = 0.035f * sin((x * 0.09f) + (y * 0.02f)) + 0.02f * cos(x * 0.25f)
          val seam = if (y % 128 < 2) -0.14f else 0f
          val lum = (0.74f + streak + grain + seam).coerceIn(0.42f, 0.96f)
          val c = (lum * 255f).toInt()
          val b = ((lum * 1.04f).coerceIn(0f, 1f) * 255f).toInt()
          pixels[y * size + x] = Color.rgb(c, c, b)
        }
      }
      bmp.setPixels(pixels, 0, size, 0, 0, size, size)
      return bmp
    }

    internal fun buildTireTreadBitmap(size: Int): Bitmap {
      val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bmp)
      canvas.drawColor(Color.rgb(26, 30, 36))

      val knobDark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(15, 17, 21) }
      val knobTop = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(48, 54, 64) }
      val sipePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(10, 12, 16)
        strokeWidth = 3f
      }

      val rows = 8
      val cols = 6
      val cellH = size / rows.toFloat()
      val cellW = size / cols.toFloat()
      for (r in 0 until rows) {
        val stagger = if (r % 2 == 0) 0f else cellW * 0.5f
        for (c in -1..cols) {
          val cx = c * cellW + stagger
          val cy = r * cellH
          canvas.drawRoundRect(cx + 7f, cy + 6f, cx + cellW - 7f, cy + cellH - 6f, 8f, 8f, knobDark)
          canvas.drawRoundRect(cx + 11f, cy + 9f, cx + cellW - 11f, cy + cellH - 11f, 5f, 5f, knobTop)
          canvas.drawLine(cx + 14f, cy + cellH * 0.5f, cx + cellW - 14f, cy + cellH * 0.5f, sipePaint)
        }
      }
      return bmp
    }

    internal fun buildBatteryPackBitmap(size: Int): Bitmap {
      val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bmp)
      val bgPaint = Paint().apply {
        shader = LinearGradient(
          0f,
          0f,
          size.toFloat(),
          size.toFloat(),
          intArrayOf(Color.rgb(22, 36, 30), Color.rgb(16, 48, 36), Color.rgb(20, 30, 28)),
          null,
          Shader.TileMode.CLAMP,
        )
      }
      canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

      val cellRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.argb(145, 74, 222, 128)
      }
      val cellCore = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(115, 16, 185, 129)
      }
      val step = size / 10f
      var row = 0
      var y = step * 0.45f
      while (y < size) {
        val offset = if (row % 2 == 0) 0f else step * 0.5f
        var x = step * 0.5f + offset
        while (x < size) {
          canvas.drawCircle(x, y, step * 0.34f, cellCore)
          canvas.drawCircle(x, y, step * 0.34f, cellRing)
          x += step
        }
        y += step * 0.86f
        row++
      }

      val busbar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(210, 245, 158, 11)
        strokeWidth = (size / 48f).coerceAtLeast(4f)
      }
      canvas.drawLine(0f, size * 0.25f, size.toFloat(), size * 0.25f, busbar)
      canvas.drawLine(0f, size * 0.75f, size.toFloat(), size * 0.75f, busbar)
      return bmp
    }

    internal fun buildBrakeRotorBitmap(size: Int): Bitmap {
      val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bmp)
      val cx = size * 0.5f
      val cy = size * 0.5f
      canvas.drawColor(Color.rgb(172, 184, 198))

      val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (size / 140f).coerceAtLeast(2f)
      }
      val step = (size / 64).coerceAtLeast(4)
      for (r in size / 16 until size / 2 step step) {
        val tone = 160 + ((r * 13) % 65)
        ringPaint.color = Color.rgb(tone, tone + 6, tone + 14)
        canvas.drawCircle(cx, cy, r.toFloat(), ringPaint)
      }
      val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(22, 28, 36) }
      for (a in 0 until 24) {
        val ang = (a / 24.0) * 2.0 * PI
        for (band in 0..2) {
          val rad = size * (0.24f + band * 0.09f)
          val x = cx + (rad * cos(ang + band * 0.14)).toFloat()
          val y = cy + (rad * sin(ang + band * 0.14)).toFloat()
          canvas.drawCircle(x, y, size * 0.014f, holePaint)
        }
      }
      return bmp
    }

    internal fun buildTftCockpitBitmap(size: Int): Bitmap {
      val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bmp)
      canvas.drawColor(Color.rgb(9, 15, 30))

      val scale = size / 512f
      val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f * scale
        color = Color.rgb(56, 189, 248)
      }
      canvas.drawRoundRect(14f * scale, 14f * scale, size - 14f * scale, size - 14f * scale, 24f * scale, 24f * scale, border)

      val arcBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 26f * scale
        strokeCap = Paint.Cap.ROUND
        color = Color.rgb(30, 41, 59)
      }
      canvas.drawArc(70f * scale, 70f * scale, size - 70f * scale, size - 70f * scale, 140f, 260f, false, arcBg)

      val arcFg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 26f * scale
        strokeCap = Paint.Cap.ROUND
        color = Color.rgb(34, 211, 238)
      }
      canvas.drawArc(70f * scale, 70f * scale, size - 70f * scale, size - 70f * scale, 140f, 195f, false, arcFg)

      val speedText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 112f * scale
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
      }
      canvas.drawText("74V", size * 0.5f, size * 0.49f, speedText)

      val subText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(74, 222, 128)
        textSize = 36f * scale
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
      }
      canvas.drawText("FOC SPORT+ • 98%", size * 0.5f, size * 0.63f, subText)
      val auxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(245, 158, 11)
        textSize = 26f * scale
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
      }
      canvas.drawText("BMS 48°C • 240A PK", size * 0.5f, size * 0.75f, auxPaint)
      return bmp
    }

    internal fun buildDynoFloorBitmap(size: Int): Bitmap {
      val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bmp)
      val cx = size * 0.5f
      val cy = size * 0.5f

      val bg = Paint().apply {
        shader = RadialGradient(
          cx,
          cy,
          size * 0.56f,
          intArrayOf(Color.rgb(26, 40, 68), Color.rgb(13, 21, 38), Color.rgb(5, 8, 16)),
          floatArrayOf(0f, 0.54f, 1f),
          Shader.TileMode.CLAMP,
        )
      }
      canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bg)

      val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(62, 56, 189, 248)
        strokeWidth = (size / 512f).coerceAtLeast(1.5f)
      }
      val step = size / 24
      for (i in 0..24) {
        val p = i * step.toFloat()
        canvas.drawLine(p, 0f, p, size.toFloat(), gridPaint)
        canvas.drawLine(0f, p, size.toFloat(), p, gridPaint)
      }

      val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (size / 220f).coerceAtLeast(2.5f)
        color = Color.argb(170, 34, 211, 238)
      }
      canvas.drawCircle(cx, cy, size * 0.36f, ringPaint)
      ringPaint.strokeWidth = (size / 420f).coerceAtLeast(1.5f)
      ringPaint.color = Color.argb(95, 56, 189, 248)
      canvas.drawCircle(cx, cy, size * 0.44f, ringPaint)
      canvas.drawCircle(cx, cy, size * 0.22f, ringPaint)

      // 15-degree radial degree protractor tick marks around turntable ring
      val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(165, 125, 211, 252)
        strokeWidth = (size / 400f).coerceAtLeast(1.5f)
      }
      for (deg in 0 until 360 step 15) {
        val r = (deg * PI / 180.0).toFloat()
        val inner = size * (if (deg % 45 == 0) 0.34f else 0.352f)
        val outer = size * 0.378f
        canvas.drawLine(
          cx + inner * cos(r),
          cy + inner * sin(r),
          cx + outer * cos(r),
          cy + outer * sin(r),
          tickPaint,
        )
      }

      // Dual Steel Dyno Drum Track Markers
      val drumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (size / 180f).coerceAtLeast(3f)
        color = Color.argb(180, 245, 158, 11)
      }
      canvas.drawRoundRect(cx - size * 0.24f, cy - size * 0.055f, cx - size * 0.14f, cy + size * 0.055f, 8f, 8f, drumPaint)
      canvas.drawRoundRect(cx + size * 0.14f, cy - size * 0.055f, cx + size * 0.24f, cy + size * 0.055f, 8f, 8f, drumPaint)
      return bmp
    }
  }
}
