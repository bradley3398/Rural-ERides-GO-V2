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

/**
 * Pure Jetpack Compose + Skia GPU Triangle-Rasterized 3D Showroom Engine.
 * Transforms, back-face culls, depth-sorts, and Gouraud-shades the full sculpted
 * Pev3dGltfBuilder geometry with metallic specular reflections, studio rim light,
 * ground turntable + soft AO shadow, X-Ray shell transparency, FLIR Thermal IR,
 * Carbon Gloss clearcoat contrast, and Wireframe CAD overlay.
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
) {
  val builtScene = remember(archetype, finish, lightsOn) {
    Pev3dGltfBuilder.buildSceneMeshes(
      archetype = archetype,
      finish = finish,
      lightsOn = lightsOn,
    )
  }
  val buffers = remember { ComposeTriSortBuffers() }
  val paint = remember { AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG) }

  val isBlueprintMode = renderMode == Pev3dRenderMode.BLUEPRINT
  val bgBrush = remember(lightingPreset, isBlueprintMode) {
    if (isBlueprintMode) {
      Brush.radialGradient(
        listOf(Color(0xFF0E2B5C), Color(0xFF081C40), Color(0xFF040F26)),
      )
    } else {
      when (lightingPreset) {
        Pev3dLightingPreset.STUDIO -> Brush.radialGradient(
          listOf(Color(0xFF192231), Color(0xFF0E141E), Color(0xFF070A0F)),
        )
        Pev3dLightingPreset.SUNSET -> Brush.radialGradient(
          listOf(Color(0xFF2B181A), Color(0xFF191015), Color(0xFF0A080C)),
        )
        Pev3dLightingPreset.NEON -> Brush.radialGradient(
          listOf(Color(0xFF0F1D3A), Color(0xFF0A1024), Color(0xFF050711)),
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

    Pev3dEngine.buildCameraViewProjection(w, h, yawDeg, pitchDeg, zoom, vp, camPos)

    // Studio contact shadow & turntable ring directly under the bike
    val centerIn = floatArrayOf(0f, -0.77f, 0f, 1f)
    val centerOut = FloatArray(4)
    Mat4Math.multiplyMV(centerOut, vp, centerIn)
    if (centerOut[3] > 0.1f) {
      val cx = (centerOut[0] / centerOut[3] * 0.5f + 0.5f) * w
      val cy = (0.5f - centerOut[1] / centerOut[3] * 0.5f) * h
      val shadowR = min(w, h) * 0.34f * zoom.coerceIn(0.7f, 1.5f)
      drawOval(
        brush = Brush.radialGradient(
          listOf(Color(0xAA000000), Color(0x44000000), Color.Transparent),
          center = Offset(cx, cy),
          radius = shadowR,
        ),
        topLeft = Offset(cx - shadowR, cy - shadowR * 0.36f),
        size = androidx.compose.ui.geometry.Size(shadowR * 2f, shadowR * 0.72f),
      )
      drawOval(
        color = if (isBlueprintMode) Color(0x6638BDF8) else Color(0x3338BDF8),
        topLeft = Offset(cx - shadowR * 0.96f, cy - shadowR * 0.34f),
        size = androidx.compose.ui.geometry.Size(shadowR * 1.92f, shadowR * 0.68f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6f),
      )
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

    // Light rig setup matching lightingPreset
    val lightCfg = when (lightingPreset) {
      Pev3dLightingPreset.STUDIO -> floatArrayOf(0.45f, 0.85f, 0.35f, 1.0f, 0.99f, 0.97f)
      Pev3dLightingPreset.SUNSET -> floatArrayOf(-0.78f, 0.42f, 0.45f, 1.08f, 0.72f, 0.45f)
      Pev3dLightingPreset.NEON -> floatArrayOf(0.28f, 0.68f, -0.68f, 0.55f, 0.92f, 1.15f)
    }
    val kx = lightCfg[0]; val ky = lightCfg[1]; val kz = lightCfg[2]
    val kr = lightCfg[3]; val kg = lightCfg[4]; val kb = lightCfg[5]
    val kLen = sqrt(kx * kx + ky * ky + kz * kz).coerceAtLeast(1e-5f)
    val ldx = kx / kLen; val ldy = ky / kLen; val ldz = kz / kLen

    val totalMaxTris = builtScene.parts.sumOf { if (it.partKey == "floor") 0 else it.indices.size / 3 }
    if (totalMaxTris == 0) return@Canvas
    buffers.ensure(totalMaxTris)

    val isXray = renderMode == Pev3dRenderMode.XRAY
    val isCarbon = renderMode == Pev3dRenderMode.CARBON_SHOWROOM
    val isThermal = renderMode == Pev3dRenderMode.THERMAL_IR
    val isBlueprint = renderMode == Pev3dRenderMode.BLUEPRINT
    val ambientBoost = ((ambientTempC - 15f) / 40f).coerceIn(0f, 0.25f)
    val throttleHeat = throttlePct.coerceIn(0f, 1f) * 0.68f
    val flashPhase = ((wheelSpinDeg / 45f).toInt() % 2 == 0)
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

      // Base color & material adjustments
      var br = part.baseRgb[0]
      var bg = part.baseRgb[1]
      var bb = part.baseRgb[2]
      var emR = part.emissiveRgb[0]
      var emG = part.emissiveRgb[1]
      var emB = part.emissiveRgb[2]
      if (part.nodeName.contains("signal_left") && leftActive) {
        emR = 1.0f; emG = 0.65f; emB = 0.05f
      } else if (part.nodeName.contains("signal_right") && rightActive) {
        emR = 1.0f; emG = 0.65f; emB = 0.05f
      }
      val isSel = selectedPartKey != null && part.partKey == selectedPartKey
      val specPow = if (isCarbon || part.clearcoat > 0.2f) 42f else 18f
      val specWeight = if (isCarbon) 0.72f else (0.18f + part.metallic * 0.48f + part.clearcoat * 0.25f)
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
        br *= 0.42f; bg *= 0.42f; bb *= 0.45f
      }

      for (vi in 0 until vCount) {
        val vo = vi * 8
        val px = vArr[vo]; val py = vArr[vo + 1]; val pz = vArr[vo + 2]
        val nx0 = vArr[vo + 3]; val ny0 = vArr[vo + 4]; val nz0 = vArr[vo + 5]

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
        val fresnel = (1f - nDotV).pow(2.3f)

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

        val diff = (nx * ldx + ny * ldy + nz * ldz).coerceAtLeast(0f)
        var hx = ldx + vx; var hy = ldy + vy; var hz = ldz + vz
        val hInv = 1f / sqrt(hx * hx + hy * hy + hz * hz).coerceAtLeast(1e-5f)
        hx *= hInv; hy *= hInv; hz *= hInv
        val nDotH = (nx * hx + ny * hy + nz * hz).coerceAtLeast(0f)
        val spec = nDotH.pow(specPow) * specWeight
        val hemi = 0.24f + 0.18f * (0.5f + 0.5f * ny)

        var r = br * (hemi + diff * kr * 0.85f) + spec * kr + fresnel * 0.20f + emR * 0.75f
        var g = bg * (hemi + diff * kg * 0.85f) + spec * kg + fresnel * 0.24f + emG * 0.75f
        var b = bb * (hemi + diff * kb * 0.85f) + spec * kb + fresnel * 0.32f + emB * 0.75f
        if (isSel) {
          r += 0.12f + 0.22f * fresnel
          g += 0.26f + 0.38f * fresnel
          b += 0.42f + 0.52f * fresnel
        }
        vertColors[vi] = packArgb(alpha, r, g, b)
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
        // Backface cull opaque meshes in screen space (CCW winding -> cross <= 0 is front-facing in flipped Y screen coords)
        val cross2d = (x1 - x0) * (y2 - y0) - (y1 - y0) * (x2 - x0)
        if (!isXray && cross2d > 0f) continue

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
      canvas.nativeCanvas.drawVertices(
        AndroidCanvas.VertexMode.TRIANGLES,
        triCount * 6,
        dstVerts,
        0,
        null,
        0,
        dstCols,
        0,
        null,
        0,
        0,
        paint,
      )
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
