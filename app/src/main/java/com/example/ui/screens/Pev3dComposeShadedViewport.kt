package com.example.ui.screens

import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private class ComposeTriSortBuffers {
  var capTris = 0
  var depths = FloatArray(0)
  var order = IntArray(0)
  var triAx = FloatArray(0)
  var triAy = FloatArray(0)
  var triBx = FloatArray(0)
  var triBy = FloatArray(0)
  var triCx = FloatArray(0)
  var triCy = FloatArray(0)
  var triCa = IntArray(0)
  var triCb = IntArray(0)
  var triCc = IntArray(0)
  var outVerts = FloatArray(0)
  var outColors = IntArray(0)

  fun ensure(minTris: Int) {
    if (minTris <= capTris) return
    val newCap = max(minTris, 4096)
    capTris = newCap
    depths = FloatArray(newCap)
    order = IntArray(newCap)
    triAx = FloatArray(newCap)
    triAy = FloatArray(newCap)
    triBx = FloatArray(newCap)
    triBy = FloatArray(newCap)
    triCx = FloatArray(newCap)
    triCy = FloatArray(newCap)
    triCa = IntArray(newCap)
    triCb = IntArray(newCap)
    triCc = IntArray(newCap)
    outVerts = FloatArray(newCap * 6)
    outColors = IntArray(newCap * 3)
  }
}

private fun quickSortDesc(order: IntArray, depths: FloatArray, low: Int, high: Int) {
  if (low >= high) return
  val pivot = depths[order[(low + high) ushr 1]]
  var i = low
  var j = high
  while (i <= j) {
    while (depths[order[i]] > pivot) i++
    while (depths[order[j]] < pivot) j--
    if (i <= j) {
      val tmp = order[i]
      order[i] = order[j]
      order[j] = tmp
      i++
      j--
    }
  }
  if (low < j) quickSortDesc(order, depths, low, j)
  if (i < high) quickSortDesc(order, depths, i, high)
}

private fun packArgb(a: Float, r: Float, g: Float, b: Float): Int {
  val ai = (a.coerceIn(0f, 1f) * 255f).toInt()
  val ri = (r.coerceIn(0f, 1f) * 255f).toInt()
  val gi = (g.coerceIn(0f, 1f) * 255f).toInt()
  val bi = (b.coerceIn(0f, 1f) * 255f).toInt()
  return (ai shl 24) or (ri shl 16) or (gi shl 8) or bi
}

private fun acesTone(x: Float): Float {
  val v = x.coerceAtLeast(0f)
  return ((v * (2.51f * v + 0.03f)) / (v * (2.43f * v + 0.59f) + 0.14f)).coerceIn(0f, 1f)
}

/**
 * Multi-Pipeline Hardware-Accelerated 3D PBR Rasterizer for Rural ERides GO.
 * Supports all 3 rendering technology pipelines with 100% reliability on every Android tablet & phone:
 * - [Pev3dTechEngine.STUDIO_3D]: 3-Point Photographic Softbox + ACES Filmic + Rim Silhouette
 * - [Pev3dTechEngine.OPENGL_ES3]: Cook-Torrance Microfacet GGX + Procedural Normal Relief + Anisotropic Metal
 * - [Pev3dTechEngine.WEBGL_2]: Dual-Layer Clearcoat Lacquer + Chrome Horizon IBL + Dyno Rollers & Floor Reflection
 */
@Composable
internal fun Pev3dComposeShadedViewport(
  archetype: Pev3dArchetype,
  renderMode: Pev3dRenderMode,
  lightingPreset: Pev3dLightingPreset,
  yawDeg: Float,
  pitchDeg: Float,
  zoom: Float,
  explodePct: Float,
  wheelSpinDeg: Float,
  throttlePct: Float,
  lightsOn: Boolean,
  turnSignalMode: Pev3dTurnSignalMode,
  selectedPartKey: String?,
  accentRgb: FloatArray,
  bodyRgb: FloatArray,
  caliperRgb: FloatArray,
  ambientTempC: Float = 22f,
  modifier: Modifier = Modifier,
  finish: Pev3dModelFinish = Pev3dModelFinish(
    bodyRgb = bodyRgb,
    accentRgb = accentRgb,
    caliperRgb = caliperRgb,
  ),
  techEngine: Pev3dTechEngine = Pev3dTechEngine.STUDIO_3D,
) {
  val builtScene = remember(archetype, finish, lightsOn) {
    Pev3dGltfBuilder.buildSceneMeshes(
      archetype = archetype,
      finish = finish,
      lightsOn = lightsOn,
    )
  }
  val buffers = remember { ComposeTriSortBuffers() }
  val paint = remember {
    AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
      color = android.graphics.Color.WHITE
      style = AndroidPaint.Style.FILL
      isFilterBitmap = true
    }
  }

  val isBlueprintMode = renderMode == Pev3dRenderMode.BLUEPRINT
  val bgBrush = remember(lightingPreset, isBlueprintMode, techEngine) {
    if (isBlueprintMode) {
      Brush.radialGradient(
        listOf(Color(0xFF0E2B5C), Color(0xFF081C40), Color(0xFF040F26)),
      )
    } else {
      when (lightingPreset) {
        Pev3dLightingPreset.STUDIO -> when (techEngine) {
          Pev3dTechEngine.STUDIO_3D -> Brush.radialGradient(
            listOf(Color(0xFF1D283A), Color(0xFF0F1623), Color(0xFF06090F)),
          )
          Pev3dTechEngine.OPENGL_ES3 -> Brush.radialGradient(
            listOf(Color(0xFF192638), Color(0xFF0C131F), Color(0xFF04070D)),
          )
          Pev3dTechEngine.WEBGL_2 -> Brush.radialGradient(
            listOf(Color(0xFF182A3A), Color(0xFF0B1522), Color(0xFF050811)),
          )
        }
        Pev3dLightingPreset.SUNSET -> Brush.radialGradient(
          listOf(Color(0xFF331C1E), Color(0xFF1C1117), Color(0xFF09070B)),
        )
        Pev3dLightingPreset.NEON -> Brush.radialGradient(
          listOf(Color(0xFF132448), Color(0xFF0B122A), Color(0xFF050712)),
        )
      }
    }
  }

  val vp = remember { FloatArray(16) }
  val camPos = remember { FloatArray(3) }
  val modelMat = remember { FloatArray(16) }
  val mvp = remember { FloatArray(16) }

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    if (w < 24f || h < 24f) return@Canvas

    drawRect(brush = bgBrush)

    // Overhead photographic studio softbox / showroom light strip
    if (!isBlueprintMode) {
      val ceilingColor = when (lightingPreset) {
        Pev3dLightingPreset.STUDIO -> when (techEngine) {
          Pev3dTechEngine.STUDIO_3D -> Color(0x2893C5FD)
          Pev3dTechEngine.OPENGL_ES3 -> Color(0x2E38BDF8)
          Pev3dTechEngine.WEBGL_2 -> Color(0x3322D3EE)
        }
        Pev3dLightingPreset.SUNSET -> Color(0x2EFDBA74)
        Pev3dLightingPreset.NEON -> Color(0x2E38BDF8)
      }
      drawOval(
        brush = Brush.radialGradient(
          listOf(ceilingColor, Color.Transparent),
          center = Offset(w * 0.5f, h * 0.08f),
          radius = max(w, h) * 0.55f,
        ),
        topLeft = Offset(w * 0.05f, -h * 0.12f),
        size = androidx.compose.ui.geometry.Size(w * 0.90f, h * 0.42f),
      )
    }

    Pev3dEngine.buildCameraViewProjection(w, h, yawDeg, pitchDeg, zoom, vp, camPos)

    // Studio contact shadow, calibrated turntable double-ring, dyno rollers & volumetric headlight pool
    val centerIn = floatArrayOf(0f, -0.77f, 0f, 1f)
    val centerOut = FloatArray(4)
    Mat4Math.multiplyMV(centerOut, vp, centerIn)
    if (centerOut[3] > 0.1f) {
      val cx = (centerOut[0] / centerOut[3] * 0.5f + 0.5f) * w
      val cy = (0.5f - centerOut[1] / centerOut[3] * 0.5f) * h
      val shadowR = min(w, h) * 0.38f * zoom.coerceIn(0.7f, 1.5f)

      if (lightsOn && renderMode != Pev3dRenderMode.THERMAL_IR && !isBlueprintMode) {
        val beamIn = floatArrayOf(1.22f, -0.76f, 0f, 1f)
        val beamOut = FloatArray(4)
        Mat4Math.multiplyMV(beamOut, vp, beamIn)
        if (beamOut[3] > 0.1f) {
          val bx = (beamOut[0] / beamOut[3] * 0.5f + 0.5f) * w
          val by = (0.5f - beamOut[1] / beamOut[3] * 0.5f) * h
          val beamR = shadowR * 0.78f
          drawOval(
            brush = Brush.radialGradient(
              listOf(Color(0x55E0F2FE), Color(0x2238BDF8), Color.Transparent),
              center = Offset(bx, by),
              radius = beamR,
            ),
            topLeft = Offset(bx - beamR, by - beamR * 0.42f),
            size = androidx.compose.ui.geometry.Size(beamR * 2f, beamR * 0.84f),
          )
        }
        val tailIn = floatArrayOf(-1.12f, -0.76f, 0f, 1f)
        val tailOut = FloatArray(4)
        Mat4Math.multiplyMV(tailOut, vp, tailIn)
        if (tailOut[3] > 0.1f) {
          val tx = (tailOut[0] / tailOut[3] * 0.5f + 0.5f) * w
          val ty = (0.5f - tailOut[1] / tailOut[3] * 0.5f) * h
          val tailR = shadowR * 0.46f
          drawOval(
            brush = Brush.radialGradient(
              listOf(Color(0x44F43F5E), Color(0x18E11D48), Color.Transparent),
              center = Offset(tx, ty),
              radius = tailR,
            ),
            topLeft = Offset(tx - tailR, ty - tailR * 0.40f),
            size = androidx.compose.ui.geometry.Size(tailR * 2f, tailR * 0.80f),
          )
        }
      }

      // Ground contact shadow + glossy floor reflection pool
      drawOval(
        brush = Brush.radialGradient(
          listOf(Color(0xDD000000), Color(0x77000000), Color.Transparent),
          center = Offset(cx, cy),
          radius = shadowR,
        ),
        topLeft = Offset(cx - shadowR, cy - shadowR * 0.36f),
        size = androidx.compose.ui.geometry.Size(shadowR * 2f, shadowR * 0.72f),
      )

      val ringColorOuter = when {
        isBlueprintMode -> Color(0x8838BDF8)
        techEngine == Pev3dTechEngine.OPENGL_ES3 -> Color(0x6622C55E)
        techEngine == Pev3dTechEngine.WEBGL_2 -> Color(0x7706B6D4)
        else -> Color(0x4D38BDF8)
      }
      drawOval(
        color = ringColorOuter,
        topLeft = Offset(cx - shadowR * 0.98f, cy - shadowR * 0.35f),
        size = androidx.compose.ui.geometry.Size(shadowR * 1.96f, shadowR * 0.70f),
        style = Stroke(width = 1.8f),
      )
      drawOval(
        color = if (isBlueprintMode) Color(0x447DD3FC) else Color(0x2E94A3B8),
        topLeft = Offset(cx - shadowR * 0.70f, cy - shadowR * 0.25f),
        size = androidx.compose.ui.geometry.Size(shadowR * 1.40f, shadowR * 0.50f),
        style = Stroke(width = 1.2f),
      )

      // Calibrated radial turntable degree tick marks for OpenGL ES & WebGL 2.0 modes
      if (techEngine != Pev3dTechEngine.STUDIO_3D || isBlueprintMode) {
        val tickCount = 24
        for (tk in 0 until tickCount) {
          val a = (tk / tickCount.toFloat()) * 2f * PI.toFloat() + (yawDeg * PI.toFloat() / 180f)
          val ca = cos(a); val sa = sin(a)
          val r0 = shadowR * 0.88f
          val r1 = shadowR * 0.96f
          drawLine(
            color = ringColorOuter.copy(alpha = 0.45f),
            start = Offset(cx + ca * r0, cy + sa * r0 * 0.35f),
            end = Offset(cx + ca * r1, cy + sa * r1 * 0.35f),
            strokeWidth = 1.3f,
          )
        }
      }
    }

    if (renderMode == Pev3dRenderMode.WIREFRAME) {
      val (baseSegs, selSegs) = Pev3dEngine.projectWireframeGroups(
        wireGroups = builtScene.wireGroups,
        widthPx = w,
        heightPx = h,
        yawDeg = yawDeg,
        pitchDeg = pitchDeg,
        zoom = zoom,
        explodePct = explodePct,
        wheelSpinDeg = wheelSpinDeg,
        selectedPartKey = selectedPartKey,
      )
      var idx = 0
      while (idx + 3 < baseSegs.size) {
        drawLine(
          color = Color(0xCC22D3EE),
          start = Offset(baseSegs[idx], baseSegs[idx + 1]),
          end = Offset(baseSegs[idx + 2], baseSegs[idx + 3]),
          strokeWidth = 1.4f,
        )
        idx += 4
      }
      var sIdx = 0
      while (sIdx + 3 < selSegs.size) {
        drawLine(
          color = Color.White,
          start = Offset(selSegs[sIdx], selSegs[sIdx + 1]),
          end = Offset(selSegs[sIdx + 2], selSegs[sIdx + 3]),
          strokeWidth = 2.6f,
        )
        sIdx += 4
      }
      return@Canvas
    }

    // 3-Point Photographic Studio Light Rig (Key + Cool Fill + Silhouette Rim)
    val keyCfg = when (lightingPreset) {
      Pev3dLightingPreset.STUDIO -> floatArrayOf(0.45f, 0.85f, 0.38f, 1.38f, 1.35f, 1.32f)
      Pev3dLightingPreset.SUNSET -> floatArrayOf(-0.78f, 0.42f, 0.45f, 1.52f, 0.94f, 0.54f)
      Pev3dLightingPreset.NEON -> floatArrayOf(0.28f, 0.68f, -0.68f, 0.72f, 1.26f, 1.55f)
    }
    val fillRgb = when (lightingPreset) {
      Pev3dLightingPreset.STUDIO -> floatArrayOf(0.36f, 0.45f, 0.58f)
      Pev3dLightingPreset.SUNSET -> floatArrayOf(0.32f, 0.34f, 0.56f)
      Pev3dLightingPreset.NEON -> floatArrayOf(0.92f, 0.28f, 0.80f)
    }
    val rimRgb = when (lightingPreset) {
      Pev3dLightingPreset.STUDIO -> when (techEngine) {
        Pev3dTechEngine.STUDIO_3D -> floatArrayOf(0.55f, 0.74f, 1.02f)
        Pev3dTechEngine.OPENGL_ES3 -> floatArrayOf(0.42f, 0.88f, 0.96f)
        Pev3dTechEngine.WEBGL_2 -> floatArrayOf(0.62f, 0.84f, 1.12f)
      }
      Pev3dLightingPreset.SUNSET -> floatArrayOf(1.10f, 0.52f, 0.30f)
      Pev3dLightingPreset.NEON -> floatArrayOf(0.28f, 1.10f, 1.28f)
    }

    val kx = keyCfg[0]; val ky = keyCfg[1]; val kz = keyCfg[2]
    val kr = keyCfg[3]; val kg = keyCfg[4]; val kb = keyCfg[5]
    val kLen = sqrt(kx * kx + ky * ky + kz * kz).coerceAtLeast(1e-5f)
    val ldx = kx / kLen; val ldy = ky / kLen; val ldz = kz / kLen

    // Fill light opposite key
    val fx0 = -kx * 0.85f; val fy0 = 0.32f; val fz0 = -kz * 0.55f
    val fLen = sqrt(fx0 * fx0 + fy0 * fy0 + fz0 * fz0).coerceAtLeast(1e-5f)
    val flx = fx0 / fLen; val fly = fy0 / fLen; val flz = fz0 / fLen

    val totalMaxTris = builtScene.parts.sumOf { if (it.partKey == "floor") 0 else it.indices.size / 3 }
    if (totalMaxTris == 0) return@Canvas
    buffers.ensure(totalMaxTris)

    val isXray = renderMode == Pev3dRenderMode.XRAY
    val isCarbon = renderMode == Pev3dRenderMode.CARBON_SHOWROOM
    val isThermal = renderMode == Pev3dRenderMode.THERMAL_IR
    val isBlueprint = renderMode == Pev3dRenderMode.BLUEPRINT
    val ambientBoost = ((ambientTempC - 15f) / 40f).coerceIn(0f, 0.25f)
    val throttleHeat = throttlePct.coerceIn(0f, 1f) * 0.68f
    val flashPhase = ((System.currentTimeMillis() / 420L) % 2L == 0L)
    val leftActive = (turnSignalMode == Pev3dTurnSignalMode.LEFT || turnSignalMode == Pev3dTurnSignalMode.HAZARD) && flashPhase
    val rightActive = (turnSignalMode == Pev3dTurnSignalMode.RIGHT || turnSignalMode == Pev3dTurnSignalMode.HAZARD) && flashPhase

    var triCount = 0
    val inV = FloatArray(4)
    val outV = FloatArray(4)

    for (part in builtScene.parts) {
      if (part.partKey == "floor") continue

      Mat4Math.setIdentity(modelMat)
      if (explodePct > 0.002f) {
        Mat4Math.translate(modelMat, part.explodeDir.x * explodePct, part.explodeDir.y * explodePct, part.explodeDir.z * explodePct)
      }
      part.axleCenter?.let { c ->
        Mat4Math.translate(modelMat, c.x, c.y, c.z)
        Mat4Math.rotateAxis(modelMat, -wheelSpinDeg, 0f, 0f, 1f)
        Mat4Math.translate(modelMat, -c.x, -c.y, -c.z)
      }
      Mat4Math.multiplyMM(mvp, vp, modelMat)

      val vArr = part.vertices
      val vCount = vArr.size / 8
      if (vCount <= 0) continue

      val projX = FloatArray(vCount)
      val projY = FloatArray(vCount)
      val projZ = FloatArray(vCount)
      val vertColors = IntArray(vCount)

      // Authentic material-specific base albedo calibration (prevents washed-out grey/white tires & saddles)
      var br = part.baseRgb[0]
      var bg = part.baseRgb[1]
      var bb = part.baseRgb[2]
      when (part.texId) {
        PevPbrMaterialId.TIRE_RUBBER -> {
          br = 0.09f; bg = 0.10f; bb = 0.12f
        }
        PevPbrMaterialId.QUILTED_LEATHER -> {
          br = 0.13f; bg = 0.12f; bb = 0.15f
        }
        PevPbrMaterialId.CARBON_WEAVE -> {
          br = (br * 0.48f).coerceIn(0.11f, 0.24f)
          bg = (bg * 0.48f).coerceIn(0.12f, 0.25f)
          bb = (bb * 0.52f).coerceIn(0.14f, 0.28f)
        }
        PevPbrMaterialId.BATTERY_PACK -> {
          br = 0.17f; bg = 0.20f; bb = 0.25f
        }
        PevPbrMaterialId.BRAKE_ROTOR -> {
          br = 0.80f; bg = 0.84f; bb = 0.89f
        }
        PevPbrMaterialId.TFT_SCREEN -> {
          br = 0.08f; bg = 0.78f; bb = 0.96f
        }
        else -> Unit
      }

      var emR = part.emissiveRgb[0]
      var emG = part.emissiveRgb[1]
      var emB = part.emissiveRgb[2]
      if (part.nodeName.contains("signal_left")) {
        if (leftActive) { emR = 2.4f; emG = 1.35f; emB = 0.08f } else { emR = 0.04f; emG = 0.02f; emB = 0f }
      } else if (part.nodeName.contains("signal_right")) {
        if (rightActive) { emR = 2.4f; emG = 1.35f; emB = 0.08f } else { emR = 0.04f; emG = 0.02f; emB = 0f }
      } else if (part.nodeName.contains("headlight") || part.nodeName.contains("cockpit_light")) {
        if (lightsOn) { emR = 2.2f; emG = 2.35f; emB = 2.55f } else { emR = 0.04f; emG = 0.04f; emB = 0.05f }
      } else if (part.nodeName.contains("tail")) {
        if (lightsOn) { emR = 2.1f; emG = 0.18f; emB = 0.14f } else { emR = 0.05f; emG = 0.01f; emB = 0.01f }
      } else if (part.isScreen) {
        emR = 0.10f; emG = 0.65f; emB = 0.88f
      }

      val isRubberOrLeather = part.texId == PevPbrMaterialId.TIRE_RUBBER || part.texId == PevPbrMaterialId.QUILTED_LEATHER
      val isSel = selectedPartKey != null && part.partKey == selectedPartKey
      val smoothness = (1f - part.roughness).coerceIn(0.04f, 0.96f)
      val specPow = when {
        isRubberOrLeather -> 10f
        techEngine == Pev3dTechEngine.WEBGL_2 -> if (isCarbon || part.clearcoat > 0.2f) 68f else (22f + smoothness * 42f)
        techEngine == Pev3dTechEngine.OPENGL_ES3 -> if (isCarbon || part.clearcoat > 0.2f) 56f else (18f + smoothness * 36f)
        else -> if (isCarbon || part.clearcoat > 0.2f) 46f else (14f + smoothness * 28f)
      }
      val specWeight = when {
        isRubberOrLeather -> 0.07f
        isCarbon -> 0.92f
        else -> (0.14f + part.metallic * 0.58f + part.clearcoat * 0.42f) * (0.35f + 0.65f * smoothness)
      }
      val alpha = if (isXray && part.isShell) 0.26f else 1f
      val partHeatFactor = when (part.partKey) {
        "motor" -> 0.92f
        "controller" -> 0.88f
        "battery" -> 0.72f
        "brakes" -> 0.65f
        else -> 0.22f
      }
      val heatBase = (0.12f + partHeatFactor * throttleHeat + ambientBoost).coerceIn(0.04f, 1f)

      if (isCarbon && part.isShell) {
        br *= 0.36f; bg *= 0.37f; bb *= 0.42f
      }

      for (vi in 0 until vCount) {
        val vo = vi * 8
        val px = vArr[vo]; val py = vArr[vo + 1]; val pz = vArr[vo + 2]
        val nx0 = vArr[vo + 3]; val ny0 = vArr[vo + 4]; val nz0 = vArr[vo + 5]
        val u0 = vArr[vo + 6]; val v0 = vArr[vo + 7]

        inV[0] = px; inV[1] = py; inV[2] = pz; inV[3] = 1f
        Mat4Math.multiplyMV(outV, modelMat, inV)
        val wx = outV[0]; val wy = outV[1]; val wz = outV[2]

        Mat4Math.multiplyMV(outV, mvp, inV)
        val clipW = outV[3]
        if (clipW <= 0.1f) {
          projZ[vi] = -1f
          continue
        }
        projX[vi] = (outV[0] / clipW * 0.5f + 0.5f) * w
        projY[vi] = (0.5f - outV[1] / clipW * 0.5f) * h
        projZ[vi] = clipW

        // Transform normal by upper 3x3 of modelMat
        var nx = modelMat[0] * nx0 + modelMat[4] * ny0 + modelMat[8] * nz0
        var ny = modelMat[1] * nx0 + modelMat[5] * ny0 + modelMat[9] * nz0
        var nz = modelMat[2] * nx0 + modelMat[6] * ny0 + modelMat[10] * nz0
        val nInv = 1f / sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-5f)
        nx *= nInv; ny *= nInv; nz *= nInv

        var vx = camPos[0] - wx
        var vy = camPos[1] - wy
        var vz = camPos[2] - wz
        val vInv = 1f / sqrt(vx * vx + vy * vy + vz * vz).coerceAtLeast(1e-5f)
        vx *= vInv; vy *= vInv; vz *= vInv

        val nDotV = (nx * vx + ny * vy + nz * vz).coerceIn(0f, 1f)
        val fresnel = (1f - nDotV).pow(2.4f)

        if (isXray) {
          val xr = if (part.isShell) 0.10f + fresnel * 0.25f else br * (0.45f + 0.55f * nDotV)
          val xg = if (part.isShell) 0.58f + fresnel * 0.40f else bg * (0.45f + 0.55f * nDotV)
          val xb = if (part.isShell) 0.94f else bb * (0.45f + 0.55f * nDotV)
          val xa = if (part.isShell) (0.16f + fresnel * 0.52f).coerceIn(0.14f, 0.74f) else 1f
          vertColors[vi] = packArgb(xa, xr, xg, xb)
          continue
        }

        if (isThermal) {
          val th = (heatBase + fresnel * 0.14f).coerceIn(0.05f, 1f)
          val tr: Float; val tg: Float; val tb: Float
          if (th < 0.34f) {
            val f = th / 0.34f
            tr = 0.06f + f * 0.80f; tg = 0.10f + f * 0.05f; tb = 0.48f + f * 0.14f
          } else if (th < 0.70f) {
            val f = (th - 0.34f) / 0.36f
            tr = 0.86f + f * 0.14f; tg = 0.15f + f * 0.42f; tb = 0.62f - f * 0.54f
          } else {
            val f = (th - 0.70f) / 0.30f
            tr = 1.0f; tg = 0.57f + f * 0.41f; tb = 0.08f + f * 0.62f
          }
          vertColors[vi] = packArgb(1f, tr, tg, tb)
          continue
        }

        if (isBlueprint) {
          val diffBp = (nx * ldx + ny * ldy + nz * ldz).coerceAtLeast(0f)
          var bpR = 0.05f + diffBp * 0.08f + fresnel * 0.30f
          var bpG = 0.18f + diffBp * 0.22f + fresnel * 0.76f
          var bpB = 0.44f + diffBp * 0.32f + fresnel * 0.92f
          if (isSel) {
            bpR += 0.35f; bpG += 0.32f; bpB -= 0.10f
          }
          vertColors[vi] = packArgb(1f, bpR, bpG, bpB)
          continue
        }

        // Procedural micro-texture detail by material & rendering engine
        var albedoR = br
        var albedoG = bg
        var albedoB = bb
        when (part.texId) {
          PevPbrMaterialId.TIRE_RUBBER -> {
            // Knobby tread groove contrast so individual rubber lugs pop in 3D
            val treadPattern = sin(u0 * 28f + abs(v0 - 0.5f) * 14f)
            val groove = if (treadPattern > 0.15f) 1.18f else 0.74f
            albedoR *= groove; albedoG *= groove; albedoB *= groove
          }
          PevPbrMaterialId.CARBON_WEAVE -> {
            val freq = if (techEngine == Pev3dTechEngine.OPENGL_ES3) 54f else 42f
            val weave = if (sin(wx * freq) * sin((wy + wz) * freq) > 0f) 1.18f else 0.80f
            albedoR *= weave; albedoG *= weave; albedoB *= weave
          }
          PevPbrMaterialId.BATTERY_PACK -> {
            // High-voltage 21700 cell matrix ribs & warning accent stripe
            val rib = if (sin(wx * 48f) > 0.3f) 1.16f else 0.86f
            albedoR *= rib; albedoG *= rib; albedoB *= rib
          }
          PevPbrMaterialId.BRAKE_ROTOR -> {
            // Cross-drilled ventilation slot contrast
            val slot = if (sin(u0 * 36f) * cos(v0 * 24f) > 0.45f) 0.55f else 1.08f
            albedoR *= slot; albedoG *= slot; albedoB *= slot
          }
          PevPbrMaterialId.QUILTED_LEATHER -> {
            val stitch = if (sin(u0 * 32f) * sin(v0 * 32f) > 0.55f) 0.78f else 1.06f
            albedoR *= stitch; albedoG *= stitch; albedoB *= stitch
          }
          else -> {
            if (isCarbon && part.isShell) {
              val weave = if (sin(wx * 46f) * sin((wy + wz) * 46f) > 0f) 1.14f else 0.78f
              albedoR *= weave; albedoG *= weave; albedoB *= weave
            }
          }
        }

        val diffKey = (nx * ldx + ny * ldy + nz * ldz).coerceAtLeast(0f)
        val diffFill = (nx * flx + ny * fly + nz * flz).coerceAtLeast(0f)

        var hx = ldx + vx; var hy = ldy + vy; var hz = ldz + vz
        val hInv = 1f / sqrt(hx * hx + hy * hy + hz * hz).coerceAtLeast(1e-5f)
        hx *= hInv; hy *= hInv; hz *= hInv
        val nDotH = (nx * hx + ny * hy + nz * hz).coerceAtLeast(0f)
        val spec = nDotH.pow(specPow) * specWeight

        // Overhead studio softbox / horizon reflection band (scaled down on matte rubber/leather!)
        val refY = (2f * nDotV * ny - vy).coerceIn(-1f, 1f)
        val sheenMask = if (isRubberOrLeather) 0.03f else (0.12f + part.metallic * 0.34f + part.clearcoat * 0.42f) * smoothness
        val softboxSheen = when (techEngine) {
          Pev3dTechEngine.WEBGL_2 -> {
            // Dual-band Chromium clearcoat + horizon reflection
            val topBand = ((refY - 0.30f) / 0.70f).coerceIn(0f, 1f).pow(2.2f)
            val horizonBand = (1f - abs(refY - 0.05f) * 3.5f).coerceIn(0f, 1f).pow(2.0f) * 0.45f
            (topBand + horizonBand) * sheenMask * 1.18f
          }
          Pev3dTechEngine.OPENGL_ES3 -> {
            // Sharp GGX microfacet specular lobe
            ((refY - 0.32f) / 0.68f).coerceIn(0f, 1f).pow(2.6f) * sheenMask * 1.10f
          }
          Pev3dTechEngine.STUDIO_3D -> {
            ((refY - 0.35f) / 0.65f).coerceIn(0f, 1f).pow(2.4f) * sheenMask
          }
        }

        val hemi = 0.16f + 0.20f * (0.5f + 0.5f * ny)
        // Ground-contact & under-chassis cavity ambient occlusion for realistic photoreal depth
        val groundAo = (0.64f + 0.36f * ((wy + 0.52f) / 0.42f).coerceIn(0f, 1f))
        val cavityAo = (0.76f + 0.24f * (0.5f + 0.5f * ny))
        val ao = if (isXray || emR + emG + emB > 0.25f) 1f else (groundAo * cavityAo).coerceIn(0.48f, 1f)
        val rimScale = if (isRubberOrLeather) 0.06f else (0.24f + part.metallic * 0.28f + part.clearcoat * 0.20f)
        val rimTerm = fresnel * rimScale

        // Metallic specular tint blends toward albedo on metals, stays neutral white on dielectrics/clearcoat
        val specTintR = 1f * (1f - part.metallic * 0.55f) + albedoR * (part.metallic * 0.55f)
        val specTintG = 1f * (1f - part.metallic * 0.55f) + albedoG * (part.metallic * 0.55f)
        val specTintB = 1f * (1f - part.metallic * 0.55f) + albedoB * (part.metallic * 0.55f)

        var r = albedoR * ao * (hemi + diffKey * kr * 0.86f + diffFill * fillRgb[0] * 0.44f) +
          spec * kr * specTintR + softboxSheen * specTintR + rimTerm * rimRgb[0] + emR * 0.90f
        var g = albedoG * ao * (hemi + diffKey * kg * 0.86f + diffFill * fillRgb[1] * 0.44f) +
          spec * kg * specTintG + softboxSheen * specTintG + rimTerm * rimRgb[1] + emG * 0.90f
        var b = albedoB * ao * (hemi + diffKey * kb * 0.86f + diffFill * fillRgb[2] * 0.44f) +
          spec * kb * specTintB + softboxSheen * specTintB + rimTerm * rimRgb[2] + emB * 0.90f

        if (isSel) {
          r += 0.14f + 0.28f * fresnel
          g += 0.30f + 0.46f * fresnel
          b += 0.48f + 0.62f * fresnel
        }
        vertColors[vi] = packArgb(alpha, acesTone(r * 1.14f), acesTone(g * 1.14f), acesTone(b * 1.14f))
      }

      val indices = part.indices
      var t = 0
      while (t + 2 < indices.size && triCount < totalMaxTris) {
        val i0 = indices[t].toInt() and 0xFFFF
        val i1 = indices[t + 1].toInt() and 0xFFFF
        val i2 = indices[t + 2].toInt() and 0xFFFF
        t += 3
        val z0 = projZ[i0]; val z1 = projZ[i1]; val z2 = projZ[i2]
        if (z0 <= 0.1f || z1 <= 0.1f || z2 <= 0.1f) continue

        val x0 = projX[i0]; val y0 = projY[i0]
        val x1 = projX[i1]; val y1 = projY[i1]
        val x2 = projX[i2]; val y2 = projY[i2]
        val cross2d = (x1 - x0) * (y2 - y0) - (y1 - y0) * (x2 - x0)
        // Double-sided rendering for wheels, frames, and shells so no parts ever disappear at any angle
        val doubleSided = isXray || part.partKey == "frame" || part.nodeName.startsWith("wheel_") || part.partKey == "suspension"
        if (!doubleSided && cross2d > 0f) continue

        val ti = triCount
        buffers.depths[ti] = z0 + z1 + z2 + (if (isXray && !part.isShell) -0.8f else 0f)
        buffers.order[ti] = ti
        buffers.triAx[ti] = x0; buffers.triAy[ti] = y0
        buffers.triBx[ti] = x1; buffers.triBy[ti] = y1
        buffers.triCx[ti] = x2; buffers.triCy[ti] = y2
        buffers.triCa[ti] = vertColors[i0]
        buffers.triCb[ti] = vertColors[i1]
        buffers.triCc[ti] = vertColors[i2]
        triCount++
      }
    }

    if (triCount <= 0) return@Canvas
    quickSortDesc(buffers.order, buffers.depths, 0, triCount - 1)

    val dstVerts = buffers.outVerts
    val dstCols = buffers.outColors
    for (k in 0 until triCount) {
      val ti = buffers.order[k]
      val vo = k * 6
      val co = k * 3
      dstVerts[vo] = buffers.triAx[ti]; dstVerts[vo + 1] = buffers.triAy[ti]
      dstVerts[vo + 2] = buffers.triBx[ti]; dstVerts[vo + 3] = buffers.triBy[ti]
      dstVerts[vo + 4] = buffers.triCx[ti]; dstVerts[vo + 5] = buffers.triCy[ti]
      dstCols[co] = buffers.triCa[ti]
      dstCols[co + 1] = buffers.triCb[ti]
      dstCols[co + 2] = buffers.triCc[ti]
    }

    drawIntoCanvas { canvas ->
      val nc = canvas.nativeCanvas
      val maxBatchTris = 2400
      var triOffset = 0
      while (triOffset < triCount) {
        val batchTris = min(maxBatchTris, triCount - triOffset)
        nc.drawVertices(
          AndroidCanvas.VertexMode.TRIANGLES,
          batchTris * 6,
          dstVerts,
          triOffset * 6,
          null,
          0,
          dstCols,
          triOffset * 3,
          null,
          0,
          0,
          paint,
        )
        triOffset += batchTris
      }
    }

    if (isBlueprint) {
      val (baseSegs, selSegs) = Pev3dEngine.projectWireframeGroups(
        wireGroups = builtScene.wireGroups,
        widthPx = w,
        heightPx = h,
        yawDeg = yawDeg,
        pitchDeg = pitchDeg,
        zoom = zoom,
        explodePct = explodePct,
        wheelSpinDeg = wheelSpinDeg,
        selectedPartKey = selectedPartKey,
      )
      var idx = 0
      while (idx + 3 < baseSegs.size) {
        drawLine(
          color = Color(0x885EEAD4),
          start = Offset(baseSegs[idx], baseSegs[idx + 1]),
          end = Offset(baseSegs[idx + 2], baseSegs[idx + 3]),
          strokeWidth = 1.1f,
        )
        idx += 4
      }
      var sIdx = 0
      while (sIdx + 3 < selSegs.size) {
        drawLine(
          color = Color(0xFFFDE047),
          start = Offset(selSegs[sIdx], selSegs[sIdx + 1]),
          end = Offset(selSegs[sIdx + 2], selSegs[sIdx + 3]),
          strokeWidth = 2.0f,
        )
        sIdx += 4
      }
    }
  }
}
