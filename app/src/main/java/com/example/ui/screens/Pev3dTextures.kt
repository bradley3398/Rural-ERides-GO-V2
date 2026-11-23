package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.round
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

  private fun hash2d(x: Int, y: Int): Float {
    var n = x * 374761393 + y * 668265263
    n = (n xor (n ushr 13)) * 1274126177
    n = n xor (n ushr 16)
    return (n and 0x7fffffff) / 2147483647f
  }

  private fun smoothNoise(x: Float, y: Float): Float {
    val xi = floor(x).toInt()
    val yi = floor(y).toInt()
    val xf = x - xi
    val yf = y - yi
    val u = xf * xf * (3f - 2f * xf)
    val v = yf * yf * (3f - 2f * yf)
    val a = hash2d(xi, yi)
    val b = hash2d(xi + 1, yi)
    val c = hash2d(xi, yi + 1)
    val d = hash2d(xi + 1, yi + 1)
    return (a * (1f - u) + b * u) * (1f - v) + (c * (1f - u) + d * u) * v
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
    floorMin: Float = 0.42f,
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
    // glTF 2.0 MetallicRoughness map: G = roughness (0..1), B = metallic (0..1), R = 255
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

  /**
   * Fine 3K 2x2 diagonal twill carbon fiber (6px pitch at 512px) with realistic satin-gloss clearcoat
   * and anisotropic strand sheen — zero blocky checkerboard artifacts.
   */
  private fun buildCarbonPbrBundle(size: Int): PevPbrTextureBundle {
    val albedoBmp = Pev3dTextureBank.buildCarbonWeaveBitmap(size)
    val heights = FloatArray(size * size)
    val mrPixels = IntArray(size * size)
    val cell = 6
    for (y in 0 until size) {
      val ty = y / cell
      val fy = (y % cell) / cell.toFloat()
      for (x in 0 until size) {
        val tx = x / cell
        val fx = (x % cell) / cell.toFloat()
        val twill = (tx + ty) and 3
        val isWarp = twill < 2
        val h =
          if (isWarp) sin(fy * PI).toFloat() * (0.76f + 0.24f * sin(fx * PI).toFloat())
          else sin(fx * PI).toFloat() * (0.68f + 0.24f * sin(fy * PI).toFloat())
        heights[y * size + x] = h
        val rough = if (isWarp) 0.16f + (1f - h) * 0.12f else 0.24f + (1f - h) * 0.14f
        val metal = 0.22f
        mrPixels[y * size + x] = Color.rgb(255, (rough * 255f).toInt().coerceIn(24, 255), (metal * 255f).toInt())
      }
    }
    val normalBmp = heightFieldToNormalBitmap(heights, size, 3.0f)
    val aoBmp = heightFieldToOcclusionBitmap(heights, size, radiusPx = 2, strength = 0.72f, floorMin = 0.62f)
    val mrBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
      setPixels(mrPixels, 0, size, 0, 0, size, size)
    }
    return PevPbrTextureBundle(bitmapToPng(albedoBmp), bitmapToPng(normalBmp), bitmapToPng(mrBmp), bitmapToPng(aoBmp))
  }

  /**
   * CNC-turned / brushed 6061-T6 aluminum & anodized titanium grain with realistic anisotropic highlights.
   */
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
      val seam = if (y % 128 < 2) -0.35f else 0f
      for (x in 0 until size) {
        val micro = (hash2d(x, y) - 0.5f) * 0.06f
        heights[y * size + x] = rowBias[y] * 0.28f + 0.05f * sin(x * 0.16f) + seam + micro
      }
    }
    val normalBmp = heightFieldToNormalBitmap(heights, size, 2.2f)
    val aoBmp = heightFieldToOcclusionBitmap(heights, size, radiusPx = 2, strength = 0.95f, floorMin = 0.68f)
    val mrBmp = buildMrBitmap(size) { x, y ->
      val streak = abs(rowBias[y]) * 0.11f
      val seam = if (y % 128 < 2) 0.22f else 0f
      val metal = if (y % 128 < 2) 0.78f else 0.94f
      val rough = (0.17f + streak + seam + 0.025f * sin(x * 0.09f)).coerceIn(0.12f, 0.48f)
      metal to rough
    }
    return PevPbrTextureBundle(bitmapToPng(albedoBmp), bitmapToPng(normalBmp), bitmapToPng(mrBmp), bitmapToPng(aoBmp))
  }

  /**
   * High-density vulcanized all-terrain / street-sport motorcycle tire tread:
   * directional V-chevron tread blocks, circumferential water channels, fine sipes,
   * and micro-rubber pebble grain in normal & roughness maps.
   */
  private fun buildTireTreadPbrBundle(size: Int): PevPbrTextureBundle {
    val albedoBmp = Pev3dTextureBank.buildTireTreadBitmap(size)
    val heights = FloatArray(size * size)
    val mrPixels = IntArray(size * size)
    val s = size.toFloat()
    for (y in 0 until size) {
      val v = y / s // across tire profile (0..1)
      // Tire crown lives around center bands; sidewalls near 0 & 1 are smoother rubber
      val distFromCenter = abs(v - 0.5f) * 2f // 0 at crown, 1 at bead
      val inTreadZone = distFromCenter < 0.76f
      for (x in 0 until size) {
        val u = x / s // around tire circumference
        val microRubber = (hash2d(x, y) - 0.5f) * 0.09f + (smoothNoise(x * 0.18f, y * 0.18f) - 0.5f) * 0.08f
        val h: Float
        val rough: Float
        if (inTreadZone) {
          // Center circumferential drainage channel + 2 shoulder channels
          val centerGroove = abs(v - 0.5f) < 0.018f
          val midGroove = abs(abs(v - 0.5f) - 0.16f) < 0.014f
          // Angled V-chevron tread lugs repeating 18 times around circumference
          val chevronPhase = (u * 18f + abs(v - 0.5f) * 5.2f)
          val chevronFrac = chevronPhase - floor(chevronPhase)
          val lateralGroove = chevronFrac < 0.18f
          val microSipe = abs(chevronFrac - 0.58f) < 0.028f && distFromCenter in 0.08f..0.62f
          h =
            when {
              centerGroove || midGroove -> 0.05f + microRubber * 0.4f
              lateralGroove -> 0.12f + microRubber * 0.5f
              microSipe -> 0.42f + microRubber * 0.5f
              else -> 0.88f + microRubber
            }
          rough = if (centerGroove || midGroove || lateralGroove) 0.92f else 0.78f + (microRubber * 0.35f)
        } else {
          // Smooth vulcanized rubber sidewall with subtle concentric rib ring
          val rib = if (abs(distFromCenter - 0.84f) < 0.025f) 0.22f else 0.0f
          h = 0.32f + rib + microRubber * 0.6f
          rough = 0.82f + microRubber * 0.25f
        }
        heights[y * size + x] = h
        mrPixels[y * size + x] = Color.rgb(255, (rough.coerceIn(0.62f, 0.96f) * 255f).toInt(), 6)
      }
    }
    val normalBmp = heightFieldToNormalBitmap(heights, size, 4.4f)
    val aoBmp = heightFieldToOcclusionBitmap(heights, size, radiusPx = 4, strength = 1.55f, floorMin = 0.36f)
    val mrBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
      setPixels(mrPixels, 0, size, 0, 0, size, size)
    }
    return PevPbrTextureBundle(bitmapToPng(albedoBmp), bitmapToPng(normalBmp), bitmapToPng(mrBmp), bitmapToPng(aoBmp))
  }

  /**
   * Realistic OEM anodized dark-graphite extruded aluminum lithium battery enclosure
   * with machined cooling ribs, recessed fastener bolts, and brushed metal spec plate.
   */
  private fun buildBatteryPackPbrBundle(size: Int): PevPbrTextureBundle {
    val albedoBmp = Pev3dTextureBank.buildBatteryPackBitmap(size)
    val heights = FloatArray(size * size)
    val mrPixels = IntArray(size * size)
    val s = size.toFloat()
    for (y in 0 until size) {
      val yf = y / s
      for (x in 0 until size) {
        val xf = x / s
        // Extruded horizontal structural ribs across aluminum casing
        val ribWave = sin(yf * PI * 24.0).toFloat()
        val isPlate = xf in 0.14f..0.86f && yf in 0.34f..0.66f
        val isSeam = abs(xf - 0.06f) < 0.008f || abs(xf - 0.94f) < 0.008f || abs(yf - 0.08f) < 0.008f || abs(yf - 0.92f) < 0.008f
        val isCopperBar = yf in 0.14f..0.185f && xf in 0.16f..0.84f
        val grain = (hash2d(x, y) - 0.5f) * 0.06f
        val h =
          when {
            isSeam -> -0.38f
            isCopperBar -> 0.52f + grain * 0.4f
            isPlate -> 0.30f + grain * 0.5f
            else -> 0.22f + ribWave * 0.18f + grain
          }
        heights[y * size + x] = h
        val metal = when {
          isCopperBar -> 0.92f
          isPlate -> 0.86f
          else -> 0.72f
        }
        val rough = when {
          isCopperBar -> 0.22f
          isPlate -> 0.26f
          isSeam -> 0.65f
          else -> 0.38f + abs(ribWave) * 0.08f
        }
        mrPixels[y * size + x] = Color.rgb(255, (rough * 255f).toInt(), (metal * 255f).toInt())
      }
    }
    val normalBmp = heightFieldToNormalBitmap(heights, size, 3.0f)
    val aoBmp = heightFieldToOcclusionBitmap(heights, size, radiusPx = 3, strength = 1.25f, floorMin = 0.46f)
    val mrBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
      setPixels(mrPixels, 0, size, 0, 0, size, size)
    }
    return PevPbrTextureBundle(bitmapToPng(albedoBmp), bitmapToPng(normalBmp), bitmapToPng(mrBmp), bitmapToPng(aoBmp))
  }

  /**
   * Floating slotted & cross-drilled stainless brake rotor with dark anodized inner carrier and bobbins.
   */
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
        val groove = 0.06f * sin(r * 190f)
        val ang = kotlin.math.atan2(dy, dx)
        var hole = false
        for (band in 0..2) {
          val rad = 0.54f + band * 0.13f
          val phase = ((ang + band * 0.13f) / (2.0 * PI) * 24.0)
          val nearest = round(phase) * (2.0 * PI / 24.0) - band * 0.13f
          val hx = cx + (rad * size * 0.5f) * cos(nearest).toFloat()
          val hy = cy + (rad * size * 0.5f) * sin(nearest).toFloat()
          if (sqrt((x - hx) * (x - hx) + (y - hy) * (y - hy)) < size * 0.014f) {
            hole = true
            break
          }
        }
        val carrierBand = r in 0.20f..0.46f
        val bobbinAngle = round(ang / (2.0 * PI) * 8.0) * (2.0 * PI / 8.0)
        val bx = cx + (0.47f * size * 0.5f) * cos(bobbinAngle).toFloat()
        val by = cy + (0.47f * size * 0.5f) * sin(bobbinAngle).toFloat()
        val isBobbin = sqrt((x - bx) * (x - bx) + (y - by) * (y - by)) < size * 0.026f
        heights[y * size + x] = when {
          hole -> -0.82f
          isBobbin -> 0.40f
          carrierBand -> -0.20f
          else -> groove
        }
        val metal = if (hole) 0.12f else if (isBobbin) 0.95f else if (carrierBand) 0.80f else 0.95f
        val rough = if (hole) 0.88f else if (carrierBand) 0.30f else (0.15f + abs(groove))
        mrPixels[y * size + x] = Color.rgb(255, (rough * 255f).toInt(), (metal * 255f).toInt())
      }
    }
    val normalBmp = heightFieldToNormalBitmap(heights, size, 3.2f)
    val aoBmp = heightFieldToOcclusionBitmap(heights, size, radiusPx = 3, strength = 1.40f, floorMin = 0.36f)
    val mrBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
      setPixels(mrPixels, 0, size, 0, 0, size, size)
    }
    return PevPbrTextureBundle(bitmapToPng(albedoBmp), bitmapToPng(normalBmp), bitmapToPng(mrBmp), bitmapToPng(aoBmp))
  }

  /**
   * Motorsport ribbed gripper-seat vinyl / perforated Alcantara with fine pebble leather grain.
   */
  private fun buildQuiltedLeatherPbrBundle(size: Int): PevPbrTextureBundle {
    val albedoPixels = IntArray(size * size)
    val heights = FloatArray(size * size)
    val mrPixels = IntArray(size * size)
    val ribPeriod = size / 14f
    for (y in 0 until size) {
      val ribPhase = (y / ribPeriod)
      val ribFrac = ribPhase - floor(ribPhase)
      val ribBump = sin(ribFrac * PI).toFloat()
      val stitchLine = abs(ribFrac - 0.05f) < 0.028f
      for (x in 0 until size) {
        val pebble = smoothNoise(x * 0.35f, y * 0.35f) * 0.14f + (hash2d(x, y) - 0.5f) * 0.04f
        val perfHole = ((x % 14) in 5..7) && ((y % 14) in 5..7) && !stitchLine
        val lum =
          when {
            stitchLine -> 0.11f
            perfHole -> 0.09f
            else -> 0.15f + ribBump * 0.07f + pebble * 0.25f
          }
        val c = (lum.coerceIn(0.06f, 0.30f) * 255f).toInt()
        albedoPixels[y * size + x] = Color.rgb(c, c, c)
        heights[y * size + x] =
          when {
            stitchLine -> 0.05f
            perfHole -> 0.14f
            else -> 0.32f + ribBump * 0.42f + pebble
          }
        val rough = if (stitchLine || perfHole) 0.90f else 0.72f - ribBump * 0.08f
        mrPixels[y * size + x] = Color.rgb(255, (rough * 255f).toInt(), 4)
      }
    }
    val albedoBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
      setPixels(albedoPixels, 0, size, 0, 0, size, size)
    }
    val normalBmp = heightFieldToNormalBitmap(heights, size, 3.2f)
    val aoBmp = heightFieldToOcclusionBitmap(heights, size, radiusPx = 3, strength = 1.35f, floorMin = 0.44f)
    val mrBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
      setPixels(mrPixels, 0, size, 0, 0, size, size)
    }
    return PevPbrTextureBundle(bitmapToPng(albedoBmp), bitmapToPng(normalBmp), bitmapToPng(mrBmp), bitmapToPng(aoBmp))
  }

  private fun buildTftPbrBundle(size: Int): PevPbrTextureBundle {
    val albedoBmp = Pev3dTextureBank.buildTftCockpitBitmap(size)
    val heights = FloatArray(size * size) { 0.5f }
    val normalBmp = heightFieldToNormalBitmap(heights, size, 0.8f)
    val aoBmp = heightFieldToOcclusionBitmap(heights, size, radiusPx = 2, strength = 0.4f, floorMin = 0.88f)
    val mrBmp = buildMrBitmap(size) { _, _ -> 0.06f to 0.07f }
    return PevPbrTextureBundle(bitmapToPng(albedoBmp), bitmapToPng(normalBmp), bitmapToPng(mrBmp), bitmapToPng(aoBmp))
  }

  /**
   * Realistic automotive photo-studio polished dark concrete & brushed steel showroom turntable floor.
   */
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
        val steelRing = abs(r - 0.72f) < 0.012f
        val innerRing = abs(r - 0.695f) < 0.004f
        val concrete = (smoothNoise(x * 0.035f, y * 0.035f) - 0.5f) * 0.22f +
          (smoothNoise(x * 0.14f, y * 0.14f) - 0.5f) * 0.10f +
          (hash2d(x, y) - 0.5f) * 0.04f
        heights[y * size + x] = if (innerRing) -0.22f else if (steelRing) 0.18f else concrete
        val rough =
          when {
            steelRing -> 0.24f
            r < 0.70f -> (0.34f + concrete * 0.35f).coerceIn(0.24f, 0.54f)
            else -> (0.48f + concrete * 0.38f).coerceIn(0.36f, 0.68f)
          }
        val metal = if (steelRing) 0.86f else 0.06f
        mrPixels[y * size + x] = Color.rgb(255, (rough * 255f).toInt(), (metal * 255f).toInt())
      }
    }
    val normalBmp = heightFieldToNormalBitmap(heights, size, 1.8f)
    val aoBmp = heightFieldToOcclusionBitmap(heights, size, radiusPx = 3, strength = 1.0f, floorMin = 0.55f)
    val mrBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
      setPixels(mrPixels, 0, size, 0, 0, size, size)
    }
    return PevPbrTextureBundle(bitmapToPng(albedoBmp), bitmapToPng(normalBmp), bitmapToPng(mrBmp), bitmapToPng(aoBmp))
  }

  /**
   * Photorealistic Metallic Automotive Clearcoat Paint & Molded Fairing Panel Texture.
   * Uses a neutral high-key metallic micro-flake base so glTF linear baseColorFactor renders
   * authentic OEM paint colors (Corsa Red, Satin Olive, Pearl White, Gunmetal, Burnt Orange)
   * with realistic stamped vents, subtle panel seams, and precision stainless hardware.
   */
  private fun buildRacingLiveryPbrBundle(size: Int): PevPbrTextureBundle {
    val pixels = IntArray(size * size)
    val heights = FloatArray(size * size)
    val mrPixels = IntArray(size * size)
    val s = size.toFloat()

    for (y in 0 until size) {
      val yf = y / s
      for (x in 0 until size) {
        val xf = x / s
        // Subtle automotive metallic flake shimmer + gentle soft tonal gradient
        val flake = (hash2d(x, y) - 0.5f) * 14f + (smoothNoise(x * 0.12f, y * 0.12f) - 0.5f) * 10f
        val grad = (1f - yf * 0.10f) * 234f

        // Subtle satin-charcoal lower belly/intake accent contour (diagonal aerodynamic split)
        val diagVal = xf * 0.58f + yf * 0.82f
        val isAccentPin = abs(diagVal - 0.86f) < 0.012f
        val isCarbonInset = diagVal > 0.89f
        // Stamped fairing panel parting seam
        val isPanelSeam = abs(yf - 0.12f) < 0.004f || abs(yf - 0.88f) < 0.004f || abs(xf - 0.04f) < 0.004f || abs(xf - 0.96f) < 0.004f
        // Recessed air-intake louver slots near lower rear quarter
        val inVentArea = xf in 0.64f..0.88f && yf in 0.46f..0.68f
        val ventPhase = ((xf - 0.64f) / 0.055f)
        val isVentSlot = inVentArea && (ventPhase - floor(ventPhase)) < 0.36f

        val baseVal: Int
        val h: Float
        val rough: Float
        val metal: Float
        when {
          isVentSlot -> {
            baseVal = 36
            h = -0.62f
            rough = 0.72f
            metal = 0.18f
          }
          isPanelSeam -> {
            baseVal = 72
            h = -0.40f
            rough = 0.58f
            metal = 0.25f
          }
          isAccentPin -> {
            baseVal = 246
            h = 0.22f
            rough = 0.14f
            metal = 0.55f
          }
          isCarbonInset -> {
            // Subtle woven dark satin inset panel
            val weave = sin(x * 0.9f) * sin(y * 0.9f) * 14f
            baseVal = (96f + weave).toInt().coerceIn(64, 132)
            h = 0.06f + (weave / 120f)
            rough = 0.30f
            metal = 0.28f
          }
          else -> {
            baseVal = (grad + flake).toInt().coerceIn(195, 250)
            h = 0.18f + (flake / 320f)
            rough = (0.14f + abs(flake) * 0.004f).coerceIn(0.11f, 0.24f)
            metal = 0.54f
          }
        }
        pixels[y * size + x] = Color.rgb(baseVal, baseVal, baseVal)
        heights[y * size + x] = h
        mrPixels[y * size + x] = Color.rgb(255, (rough * 255f).toInt(), (metal * 255f).toInt())
      }
    }

    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
      setPixels(pixels, 0, size, 0, 0, size, size)
    }
    // Crisp machined stainless Allen/Torx fairing bolts in corners
    val canvas = Canvas(bmp)
    val recessPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(42, 46, 52) }
    val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(206, 212, 218) }
    val hexPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(68, 72, 78) }
    listOf(0.08f to 0.17f, 0.92f to 0.17f, 0.08f to 0.83f, 0.92f to 0.83f).forEach { (bx, by) ->
      canvas.drawCircle(s * bx, s * by, s * 0.015f, recessPaint)
      canvas.drawCircle(s * bx, s * by, s * 0.011f, headPaint)
      canvas.drawCircle(s * bx, s * by, s * 0.005f, hexPaint)
    }

    val normalBmp = heightFieldToNormalBitmap(heights, size, 3.0f)
    val aoBmp = heightFieldToOcclusionBitmap(heights, size, radiusPx = 3, strength = 1.25f, floorMin = 0.45f)
    val mrBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
      setPixels(mrPixels, 0, size, 0, 0, size, size)
    }
    return PevPbrTextureBundle(bitmapToPng(bmp), bitmapToPng(normalBmp), bitmapToPng(mrBmp), bitmapToPng(aoBmp))
  }

  /**
   * Realistic dark-smoked bi-LED projector headlight optic with chrome reflector cups & crisp DRL ring.
   */
  private fun buildHeadlightOpticPbrBundle(size: Int): PevPbrTextureBundle {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val s = size.toFloat()
    canvas.drawColor(Color.rgb(14, 17, 22))

    // Subtle smoked chrome reflector backing
    val reflectorBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = RadialGradient(
        s * 0.5f,
        s * 0.5f,
        s * 0.48f,
        intArrayOf(Color.rgb(46, 52, 62), Color.rgb(20, 24, 30), Color.rgb(10, 12, 16)),
        floatArrayOf(0f, 0.65f, 1f),
        Shader.TileMode.CLAMP,
      )
    }
    canvas.drawRect(0f, 0f, s, s, reflectorBg)

    // Precision LED Daytime Running Light (DRL) Perimeter Ring
    val drlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = s * 0.036f
      color = Color.rgb(240, 246, 255)
    }
    canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.42f, drlPaint)

    // Central High/Low Bi-LED Glass Projector Dome
    val lensGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = RadialGradient(
        s * 0.5f,
        s * 0.5f,
        s * 0.28f,
        intArrayOf(Color.rgb(255, 254, 248), Color.rgb(225, 238, 252), Color.rgb(48, 60, 74)),
        floatArrayOf(0f, 0.58f, 1f),
        Shader.TileMode.CLAMP,
      )
    }
    canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.28f, lensGlow)
    val bezelRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = s * 0.022f
      color = Color.rgb(175, 185, 198)
    }
    canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.29f, bezelRing)

    val heights = FloatArray(size * size)
    val mrPixels = IntArray(size * size)
    for (y in 0 until size) {
      val ny = y / s - 0.5f
      for (x in 0 until size) {
        val nx = x / s - 0.5f
        val d = sqrt(nx * nx + ny * ny)
        val dome = (0.30f - d).coerceAtLeast(0f) * 3.8f
        heights[y * size + x] = dome
        mrPixels[y * size + x] = Color.rgb(255, 14, 40) // Crystal-clear optical polycarbonate
      }
    }
    val normalBmp = heightFieldToNormalBitmap(heights, size, 3.6f)
    val aoBmp = heightFieldToOcclusionBitmap(heights, size, radiusPx = 3, strength = 0.7f, floorMin = 0.65f)
    val mrBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
      setPixels(mrPixels, 0, size, 0, 0, size, size)
    }
    return PevPbrTextureBundle(bitmapToPng(bmp), bitmapToPng(normalBmp), bitmapToPng(mrBmp), bitmapToPng(aoBmp))
  }

  private fun buildTaillightOpticPbrBundle(size: Int): PevPbrTextureBundle {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val s = size.toFloat()
    canvas.drawColor(Color.rgb(36, 5, 8))

    val bladePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(235, 28, 34) }
    val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 145, 130) }
    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = s * 0.036f
      color = Color.rgb(238, 42, 46)
    }
    canvas.drawRoundRect(s * 0.06f, s * 0.16f, s * 0.94f, s * 0.84f, s * 0.12f, s * 0.12f, border)
    for (col in 0 until 8) {
      val bx = s * (0.12f + col * 0.10f)
      canvas.drawRoundRect(bx, s * 0.28f, bx + s * 0.062f, s * 0.72f, 6f, 6f, bladePaint)
      canvas.drawRoundRect(bx + s * 0.015f, s * 0.36f, bx + s * 0.047f, s * 0.64f, 4f, 4f, corePaint)
    }

    val heights = FloatArray(size * size)
    for (y in 0 until size) {
      val yf = y / s
      for (x in 0 until size) {
        val xf = x / s
        val inFin = yf in 0.26f..0.74f && ((xf - 0.12f) / 0.10f).let { it - floor(it) } < 0.62f
        heights[y * size + x] = if (inFin) 0.42f else 0.05f
      }
    }
    val normalBmp = heightFieldToNormalBitmap(heights, size, 2.8f)
    val aoBmp = heightFieldToOcclusionBitmap(heights, size, radiusPx = 3, strength = 1.0f, floorMin = 0.52f)
    val mrBmp = buildMrBitmap(size) { _, _ -> 0.12f to 0.07f }
    return PevPbrTextureBundle(bitmapToPng(bmp), bitmapToPng(normalBmp), bitmapToPng(mrBmp), bitmapToPng(aoBmp))
  }

  private fun buildAmberSignalPbrBundle(size: Int): PevPbrTextureBundle {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val s = size.toFloat()
    canvas.drawColor(Color.rgb(44, 20, 4))

    val amber = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(245, 152, 16) }
    val hotCore = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 218, 108) }
    for (i in 0 until 5) {
      val cx = s * (0.14f + i * 0.17f)
      canvas.drawRoundRect(cx, s * 0.24f, cx + s * 0.105f, s * 0.76f, 5f, 5f, amber)
      canvas.drawRoundRect(cx + s * 0.024f, s * 0.34f, cx + s * 0.082f, s * 0.66f, 3f, 3f, hotCore)
    }

    val heights = FloatArray(size * size)
    for (y in 0 until size) {
      for (x in 0 until size) {
        heights[y * size + x] = 0.22f * sin(x * 0.38f) * cos(y * 0.38f)
      }
    }
    val normalBmp = heightFieldToNormalBitmap(heights, size, 2.6f)
    val aoBmp = heightFieldToOcclusionBitmap(heights, size, radiusPx = 2, strength = 0.9f, floorMin = 0.58f)
    val mrBmp = buildMrBitmap(size) { _, _ -> 0.10f to 0.07f }
    return PevPbrTextureBundle(bitmapToPng(bmp), bitmapToPng(normalBmp), bitmapToPng(mrBmp), bitmapToPng(aoBmp))
  }
}

internal object Pev3dTextureBank {
    private fun hashPx(x: Int, y: Int): Float {
      var n = x * 374761393 + y * 668265263
      n = (n xor (n ushr 13)) * 1274126177
      n = n xor (n ushr 16)
      return (n and 0x7fffffff) / 2147483647f
    }

    private fun noise2d(x: Float, y: Float): Float {
      val xi = floor(x).toInt()
      val yi = floor(y).toInt()
      val xf = x - xi
      val yf = y - yi
      val u = xf * xf * (3f - 2f * xf)
      val v = yf * yf * (3f - 2f * yf)
      val a = hashPx(xi, yi)
      val b = hashPx(xi + 1, yi)
      val c = hashPx(xi, yi + 1)
      val d = hashPx(xi + 1, yi + 1)
      return (a * (1f - u) + b * u) * (1f - v) + (c * (1f - u) + d * u) * v
    }

    internal fun buildCarbonWeaveBitmap(size: Int): Bitmap {
      val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
      val pixels = IntArray(size * size)
      val cell = 6 // Fine realistic 3K 2x2 twill carbon weave
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
              sin(fy * PI).toFloat() * (0.76f + 0.24f * cos(fx * 2f * PI).toFloat())
            } else {
              sin(fx * PI).toFloat() * (0.64f + 0.22f * cos(fy * 2f * PI).toFloat())
            }
          // Calibrated neutral graphite values so tinted/untinted carbon stays richly readable
          val baseLum = if (isWarp) 0.44f + 0.34f * fiberWave else 0.28f + 0.24f * fiberWave
          val micro = (hashPx(x, y) - 0.5f) * 0.025f
          val v = ((baseLum + micro).coerceIn(0.18f, 0.84f) * 255f).toInt()
          pixels[y * size + x] = Color.rgb(v, v, (v * 1.02f).toInt().coerceIn(0, 255))
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
        rowBias[y] = (seed / 2147483647f - 0.5f) * 0.13f
      }
      for (y in 0 until size) {
        val streak = rowBias[y] + 0.03f * sin(y * 0.42f)
        for (x in 0 until size) {
          val grain = 0.025f * sin((x * 0.11f) + (y * 0.02f)) + (hashPx(x, y) - 0.5f) * 0.02f
          val seam = if (y % 128 < 2) -0.11f else 0f
          val lum = (0.82f + streak + grain + seam).coerceIn(0.54f, 0.97f)
          val c = (lum * 255f).toInt()
          pixels[y * size + x] = Color.rgb(c, c, c)
        }
      }
      bmp.setPixels(pixels, 0, size, 0, 0, size, size)
      return bmp
    }

    internal fun buildTireTreadBitmap(size: Int): Bitmap {
      val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
      val pixels = IntArray(size * size)
      val s = size.toFloat()
      for (y in 0 until size) {
        val v = y / s
        val distFromCenter = abs(v - 0.5f) * 2f
        val inTreadZone = distFromCenter < 0.76f
        for (x in 0 until size) {
          val u = x / s
          val grain = (hashPx(x, y) - 0.5f) * 7f + (noise2d(x * 0.14f, y * 0.14f) - 0.5f) * 8f
          val base: Float =
            if (inTreadZone) {
              val centerGroove = abs(v - 0.5f) < 0.018f
              val midGroove = abs(abs(v - 0.5f) - 0.16f) < 0.014f
              val chevronPhase = (u * 18f + abs(v - 0.5f) * 5.2f)
              val chevronFrac = chevronPhase - floor(chevronPhase)
              val lateralGroove = chevronFrac < 0.18f
              val microSipe = abs(chevronFrac - 0.58f) < 0.028f && distFromCenter in 0.08f..0.62f
              when {
                centerGroove || midGroove -> 20f
                lateralGroove -> 24f
                microSipe -> 28f
                else -> 44f
              }
            } else {
              val rib = if (abs(distFromCenter - 0.84f) < 0.025f) 7f else 0f
              34f + rib
            }
          val c = (base + grain).toInt().coerceIn(14, 62)
          pixels[y * size + x] = Color.rgb(c, c, (c + 2).coerceAtMost(66))
        }
      }
      bmp.setPixels(pixels, 0, size, 0, 0, size, size)
      return bmp
    }

    internal fun buildBatteryPackBitmap(size: Int): Bitmap {
      val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bmp)
      val s = size.toFloat()
      // Matte anodized dark graphite extruded aluminum case
      val bgPaint = Paint().apply {
        shader = LinearGradient(
          0f,
          0f,
          0f,
          s,
          intArrayOf(Color.rgb(42, 46, 52), Color.rgb(32, 35, 40), Color.rgb(46, 50, 56)),
          null,
          Shader.TileMode.CLAMP,
        )
      }
      canvas.drawRect(0f, 0f, s, s, bgPaint)

      // Extruded cooling fin grooves
      val ribLight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(65, 96, 104, 114)
        strokeWidth = s / 120f
      }
      val ribDark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 14, 16, 20)
        strokeWidth = s / 120f
      }
      for (i in 0 until 24) {
        val ry = (i + 0.5f) * (s / 24f)
        canvas.drawLine(s * 0.06f, ry, s * 0.94f, ry, ribLight)
        canvas.drawLine(s * 0.06f, ry + s / 80f, s * 0.94f, ry + s / 80f, ribDark)
      }

      // Brushed titanium aluminum spec badge plate
      val platePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
          s * 0.14f,
          s * 0.34f,
          s * 0.86f,
          s * 0.66f,
          intArrayOf(Color.rgb(74, 80, 88), Color.rgb(58, 63, 70), Color.rgb(78, 84, 92)),
          null,
          Shader.TileMode.CLAMP,
        )
      }
      canvas.drawRoundRect(s * 0.14f, s * 0.34f, s * 0.86f, s * 0.66f, s * 0.03f, s * 0.03f, platePaint)
      val plateBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = s * 0.008f
        color = Color.rgb(122, 130, 140)
      }
      canvas.drawRoundRect(s * 0.14f, s * 0.34f, s * 0.86f, s * 0.66f, s * 0.03f, s * 0.03f, plateBorder)

      // Subtle copper HV busbar accent trim + crisp laser-etched spec label
      val copperBar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(184, 115, 51) }
      canvas.drawRoundRect(s * 0.16f, s * 0.14f, s * 0.84f, s * 0.185f, s * 0.01f, s * 0.01f, copperBar)

      val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(218, 224, 232)
        textSize = s * 0.062f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
      }
      canvas.drawText("21700 LI-ION PACK", s * 0.5f, s * 0.49f, titlePaint)
      val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(154, 164, 176)
        textSize = s * 0.038f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
      }
      canvas.drawText("SMART BMS • IP67 SEALED", s * 0.5f, s * 0.58f, subPaint)

      // Corner stainless socket bolts
      val boltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(165, 172, 182) }
      listOf(0.09f to 0.10f, 0.91f to 0.10f, 0.09f to 0.90f, 0.91f to 0.90f).forEach { (bx, by) ->
        canvas.drawCircle(s * bx, s * by, s * 0.018f, boltPaint)
      }
      return bmp
    }

    internal fun buildBrakeRotorBitmap(size: Int): Bitmap {
      val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bmp)
      val cx = size * 0.5f
      val cy = size * 0.5f
      canvas.drawColor(Color.rgb(188, 194, 202))

      // Dark anodized aluminum inner carrier spider
      val carrierPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(46, 50, 56) }
      canvas.drawCircle(cx, cy, size * 0.23f, carrierPaint)

      val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (size / 140f).coerceAtLeast(2f)
      }
      val step = (size / 72).coerceAtLeast(4)
      for (r in (size * 0.24f).toInt() until size / 2 step step) {
        val tone = 176 + ((r * 11) % 44)
        ringPaint.color = Color.rgb(tone, tone + 2, tone + 5)
        canvas.drawCircle(cx, cy, r.toFloat(), ringPaint)
      }
      // Brass/titanium floating rotor bobbins
      val bobbinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(198, 164, 92) }
      for (b in 0 until 8) {
        val ang = (b / 8.0) * 2.0 * PI
        val bx = cx + (size * 0.235f * cos(ang)).toFloat()
        val by = cy + (size * 0.235f * sin(ang)).toFloat()
        canvas.drawCircle(bx, by, size * 0.015f, bobbinPaint)
      }
      // Cross-drilled rotor vents
      val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(24, 27, 32) }
      for (a in 0 until 24) {
        val ang = (a / 24.0) * 2.0 * PI
        for (band in 0..2) {
          val rad = size * (0.27f + band * 0.068f)
          val x = cx + (rad * cos(ang + band * 0.13)).toFloat()
          val y = cy + (rad * sin(ang + band * 0.13)).toFloat()
          canvas.drawCircle(x, y, size * 0.012f, holePaint)
        }
      }
      return bmp
    }

    internal fun buildTftCockpitBitmap(size: Int): Bitmap {
      val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bmp)
      canvas.drawColor(Color.rgb(10, 13, 18))

      val scale = size / 512f
      val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f * scale
        color = Color.rgb(68, 76, 88)
      }
      canvas.drawRoundRect(14f * scale, 14f * scale, size - 14f * scale, size - 14f * scale, 22f * scale, 22f * scale, border)

      val arcBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 22f * scale
        strokeCap = Paint.Cap.ROUND
        color = Color.rgb(32, 38, 48)
      }
      canvas.drawArc(74f * scale, 74f * scale, size - 74f * scale, size - 74f * scale, 140f, 260f, false, arcBg)

      val arcFg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 22f * scale
        strokeCap = Paint.Cap.ROUND
        color = Color.rgb(235, 170, 56)
      }
      canvas.drawArc(74f * scale, 74f * scale, size - 74f * scale, size - 74f * scale, 140f, 185f, false, arcFg)

      val speedText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(245, 247, 250)
        textSize = 104f * scale
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
      }
      canvas.drawText("00", size * 0.5f, size * 0.48f, speedText)

      val subText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(134, 218, 148)
        textSize = 34f * scale
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
      }
      canvas.drawText("SPORT • 98%", size * 0.5f, size * 0.62f, subText)
      val auxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(176, 186, 198)
        textSize = 26f * scale
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
      }
      canvas.drawText("MPH • FOC READY", size * 0.5f, size * 0.74f, auxPaint)
      return bmp
    }

    /**
     * Realistic dark polished concrete & machined steel circular automotive photography turntable floor.
     */
    internal fun buildDynoFloorBitmap(size: Int): Bitmap {
      val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
      val pixels = IntArray(size * size)
      val cx = size * 0.5f
      val cy = size * 0.5f
      val invHalf = 1f / (size * 0.5f)
      for (y in 0 until size) {
        val dy = (y - cy) * invHalf
        for (x in 0 until size) {
          val dx = (x - cx) * invHalf
          val r = sqrt(dx * dx + dy * dy)
          // Soft overhead studio spotlight falloff on realistic dark polished concrete
          val spotlight = (1f - (r * 0.85f).coerceIn(0f, 1f)).let { it * it }
          val mottle = (noise2d(x * 0.025f, y * 0.025f) - 0.5f) * 14f +
            (noise2d(x * 0.09f, y * 0.09f) - 0.5f) * 8f +
            (hashPx(x, y) - 0.5f) * 4f
          val inTurntable = r < 0.70f
          val onSteelRing = abs(r - 0.72f) < 0.012f
          val onGroove = abs(r - 0.695f) < 0.005f
          val lum: Float =
            when {
              onGroove -> 16f
              onSteelRing -> 84f + mottle * 0.4f
              inTurntable -> 34f + spotlight * 22f + mottle
              else -> 22f + spotlight * 12f + mottle * 0.7f
            }
          val v = lum.toInt().coerceIn(12, 118)
          pixels[y * size + x] = Color.rgb(v, v + 1, (v + 3).coerceAtMost(124))
        }
      }
      bmp.setPixels(pixels, 0, size, 0, 0, size, size)

      // Subtle machined satin-steel compass ticks along the turntable perimeter
      val canvas = Canvas(bmp)
      val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(90, 180, 186, 194)
        strokeWidth = (size / 512f).coerceAtLeast(1.2f)
      }
      for (deg in 0 until 360 step 15) {
        val rad = (deg * PI / 180.0).toFloat()
        val inner = size * (if (deg % 45 == 0) 0.342f else 0.349f)
        val outer = size * 0.356f
        canvas.drawLine(
          cx + inner * cos(rad),
          cy + inner * sin(rad),
          cx + outer * cos(rad),
          cy + outer * sin(rad),
          tickPaint,
        )
      }
      return bmp
    }
}
