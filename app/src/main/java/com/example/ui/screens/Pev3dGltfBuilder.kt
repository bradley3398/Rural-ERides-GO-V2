package com.example.ui.screens

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal data class PevMaterialKey(
  val partKey: String,
  val texId: PevPbrMaterialId,
  val baseRgb: FloatArray,
  val metallic: Float,
  val roughness: Float,
  val emissiveRgb: FloatArray,
  val clearcoat: Float,
  val isShell: Boolean,
  val isScreen: Boolean = false,
  val useEmissiveTexture: Boolean = false,
)

internal data class PevWireGroup(
  val partKey: String,
  val axleCenter: Vec3?,
  val explodeDir: Vec3,
  val lines: FloatArray,
)

internal class PevBuiltGlb(
  val glbBytes: ByteArray,
  val wheelAxleCenters: Map<String, Vec3>,
  val nodeExplodeDirs: Map<String, Vec3>,
  val wireframeLines: FloatArray,
  val wireGroups: List<PevWireGroup> = emptyList(),
)

private class MeshBucket(
  val nodeName: String,
  val matKey: PevMaterialKey,
) {
  val positions = ArrayList<Float>(1024)
  val normals = ArrayList<Float>(1024)
  val uvs = ArrayList<Float>(768)
  val indices = ArrayList<Int>(1024)
  val wirePositions = ArrayList<Float>(512)
  private var vertCount = 0

  var minX = Float.POSITIVE_INFINITY
  var minY = Float.POSITIVE_INFINITY
  var minZ = Float.POSITIVE_INFINITY
  var maxX = Float.NEGATIVE_INFINITY
  var maxY = Float.NEGATIVE_INFINITY
  var maxZ = Float.NEGATIVE_INFINITY

  private fun normVec(v: Vec3): Vec3 {
    val len = sqrt(v.x * v.x + v.y * v.y + v.z * v.z).coerceAtLeast(1e-5f)
    return Vec3(v.x / len, v.y / len, v.z / len)
  }

  private fun addVert(p: Vec3, n: Vec3, u: Float, v: Float): Int {
    val nn = normVec(n)
    positions += p.x; positions += p.y; positions += p.z
    normals += nn.x; normals += nn.y; normals += nn.z
    uvs += u; uvs += v
    if (p.x < minX) minX = p.x
    if (p.y < minY) minY = p.y
    if (p.z < minZ) minZ = p.z
    if (p.x > maxX) maxX = p.x
    if (p.y > maxY) maxY = p.y
    if (p.z > maxZ) maxZ = p.z
    return vertCount++
  }

  private fun addWireEdge(a: Vec3, b: Vec3) {
    wirePositions += a.x; wirePositions += a.y; wirePositions += a.z
    wirePositions += b.x; wirePositions += b.y; wirePositions += b.z
  }

  fun addQuad(a: Vec3, b: Vec3, c: Vec3, d: Vec3, n: Vec3, uvScaleU: Float = 1f, uvScaleV: Float = 1f, wire: Boolean = true) {
    val i0 = addVert(a, n, 0f, uvScaleV)
    val i1 = addVert(b, n, uvScaleU, uvScaleV)
    val i2 = addVert(c, n, uvScaleU, 0f)
    val i3 = addVert(d, n, 0f, 0f)
    indices += i0; indices += i1; indices += i2
    indices += i0; indices += i2; indices += i3
    if (wire) {
      addWireEdge(a, b); addWireEdge(b, c); addWireEdge(c, d); addWireEdge(d, a)
    }
  }

  fun addChamferedBox(
    cx: Float,
    cy: Float,
    cz: Float,
    hx: Float,
    hy: Float,
    hz: Float,
    bevel: Float = 0.018f,
    uvScale: Float = 1f,
  ) {
    val b = minOf(bevel, hx * 0.35f, hy * 0.35f, hz * 0.35f)
    if (b < 0.002f) {
      addBox(cx, cy, cz, hx, hy, hz, uvScale)
      return
    }
    val ix = hx - b
    val iy = hy - b
    val iz = hz - b
    // Core 6 faces
    addQuad(Vec3(cx - ix, cy - iy, cz + hz), Vec3(cx + ix, cy - iy, cz + hz), Vec3(cx + ix, cy + iy, cz + hz), Vec3(cx - ix, cy + iy, cz + hz), Vec3(0f, 0f, 1f), uvScale, uvScale)
    addQuad(Vec3(cx + ix, cy - iy, cz - hz), Vec3(cx - ix, cy - iy, cz - hz), Vec3(cx - ix, cy + iy, cz - hz), Vec3(cx + ix, cy + iy, cz - hz), Vec3(0f, 0f, -1f), uvScale, uvScale)
    addQuad(Vec3(cx + hx, cy - iy, cz + iz), Vec3(cx + hx, cy - iy, cz - iz), Vec3(cx + hx, cy + iy, cz - iz), Vec3(cx + hx, cy + iy, cz + iz), Vec3(1f, 0f, 0f), uvScale, uvScale)
    addQuad(Vec3(cx - hx, cy - iy, cz - iz), Vec3(cx - hx, cy - iy, cz + iz), Vec3(cx - hx, cy + iy, cz + iz), Vec3(cx - hx, cy + iy, cz - iz), Vec3(-1f, 0f, 0f), uvScale, uvScale)
    addQuad(Vec3(cx - ix, cy + hy, cz + iz), Vec3(cx + ix, cy + hy, cz + iz), Vec3(cx + ix, cy + hy, cz - iz), Vec3(cx - ix, cy + hy, cz - iz), Vec3(0f, 1f, 0f), uvScale, uvScale)
    addQuad(Vec3(cx - ix, cy - hy, cz - iz), Vec3(cx + ix, cy - hy, cz - iz), Vec3(cx + ix, cy - hy, cz + iz), Vec3(cx - ix, cy - hy, cz + iz), Vec3(0f, -1f, 0f), uvScale, uvScale)

    // 12 beveled edges
    addQuad(Vec3(cx - ix, cy + iy, cz + hz), Vec3(cx + ix, cy + iy, cz + hz), Vec3(cx + ix, cy + hy, cz + iz), Vec3(cx - ix, cy + hy, cz + iz), Vec3(0f, 0.707f, 0.707f), uvScale, 0.25f, false)
    addQuad(Vec3(cx + ix, cy + iy, cz - hz), Vec3(cx - ix, cy + iy, cz - hz), Vec3(cx - ix, cy + hy, cz - iz), Vec3(cx + ix, cy + hy, cz - iz), Vec3(0f, 0.707f, -0.707f), uvScale, 0.25f, false)
    addQuad(Vec3(cx - ix, cy - hy, cz + iz), Vec3(cx + ix, cy - hy, cz + iz), Vec3(cx + ix, cy - iy, cz + hz), Vec3(cx - ix, cy - iy, cz + hz), Vec3(0f, -0.707f, 0.707f), uvScale, 0.25f, false)
    addQuad(Vec3(cx + ix, cy - hy, cz - iz), Vec3(cx - ix, cy - hy, cz - iz), Vec3(cx - ix, cy - iy, cz - hz), Vec3(cx + ix, cy - iy, cz - hz), Vec3(0f, -0.707f, -0.707f), uvScale, 0.25f, false)

    addQuad(Vec3(cx + ix, cy - iy, cz + hz), Vec3(cx + hx, cy - iy, cz + iz), Vec3(cx + hx, cy + iy, cz + iz), Vec3(cx + ix, cy + iy, cz + hz), Vec3(0.707f, 0f, 0.707f), uvScale, 0.25f, false)
    addQuad(Vec3(cx - hx, cy - iy, cz + iz), Vec3(cx - ix, cy - iy, cz + hz), Vec3(cx - ix, cy + iy, cz + hz), Vec3(cx - hx, cy + iy, cz + iz), Vec3(-0.707f, 0f, 0.707f), uvScale, 0.25f, false)
    addQuad(Vec3(cx + hx, cy - iy, cz - iz), Vec3(cx + ix, cy - iy, cz - hz), Vec3(cx + ix, cy + iy, cz - hz), Vec3(cx + hx, cy + iy, cz - iz), Vec3(0.707f, 0f, -0.707f), uvScale, 0.25f, false)
    addQuad(Vec3(cx - ix, cy - iy, cz - hz), Vec3(cx - hx, cy - iy, cz - iz), Vec3(cx - hx, cy + iy, cz - iz), Vec3(cx - ix, cy + iy, cz - hz), Vec3(-0.707f, 0f, -0.707f), uvScale, 0.25f, false)

    addQuad(Vec3(cx + hx, cy + iy, cz + iz), Vec3(cx + hx, cy + iy, cz - iz), Vec3(cx + ix, cy + hy, cz - iz), Vec3(cx + ix, cy + hy, cz + iz), Vec3(0.707f, 0.707f, 0f), uvScale, 0.25f, false)
    addQuad(Vec3(cx - hx, cy + iy, cz - iz), Vec3(cx - hx, cy + iy, cz + iz), Vec3(cx - ix, cy + hy, cz + iz), Vec3(cx - ix, cy + hy, cz - iz), Vec3(-0.707f, 0.707f, 0f), uvScale, 0.25f, false)
    addQuad(Vec3(cx + ix, cy - hy, cz + iz), Vec3(cx + ix, cy - hy, cz - iz), Vec3(cx + hx, cy - iy, cz - iz), Vec3(cx + hx, cy - iy, cz + iz), Vec3(0.707f, -0.707f, 0f), uvScale, 0.25f, false)
    addQuad(Vec3(cx - hx, cy - iy, cz + iz), Vec3(cx - hx, cy - iy, cz - iz), Vec3(cx - ix, cy - hy, cz - iz), Vec3(cx - ix, cy - hy, cz + iz), Vec3(-0.707f, -0.707f, 0f), uvScale, 0.25f, false)

    // 8 chamfer corner triangles
    fun tri(p0: Vec3, p1: Vec3, p2: Vec3, n: Vec3) {
      val a = addVert(p0, n, 0f, 0f)
      val c1 = addVert(p1, n, 0.2f, 0f)
      val c2 = addVert(p2, n, 0.1f, 0.2f)
      indices += a; indices += c1; indices += c2
    }
    tri(Vec3(cx + ix, cy + iy, cz + hz), Vec3(cx + hx, cy + iy, cz + iz), Vec3(cx + ix, cy + hy, cz + iz), Vec3(1f, 1f, 1f))
    tri(Vec3(cx - ix, cy + iy, cz + hz), Vec3(cx - ix, cy + hy, cz + iz), Vec3(cx - hx, cy + iy, cz + iz), Vec3(-1f, 1f, 1f))
    tri(Vec3(cx + ix, cy + hy, cz - iz), Vec3(cx + hx, cy + iy, cz - iz), Vec3(cx + ix, cy + iy, cz - hz), Vec3(1f, 1f, -1f))
    tri(Vec3(cx - ix, cy + hy, cz - iz), Vec3(cx - ix, cy + iy, cz - hz), Vec3(cx - hx, cy + iy, cz - iz), Vec3(-1f, 1f, -1f))
    tri(Vec3(cx + ix, cy - hy, cz + iz), Vec3(cx + hx, cy - iy, cz + iz), Vec3(cx + ix, cy - iy, cz + hz), Vec3(1f, -1f, 1f))
    tri(Vec3(cx - ix, cy - hy, cz + iz), Vec3(cx - ix, cy - iy, cz + hz), Vec3(cx - hx, cy - iy, cz + iz), Vec3(-1f, -1f, 1f))
    tri(Vec3(cx + ix, cy - iy, cz - hz), Vec3(cx + hx, cy - iy, cz - iz), Vec3(cx + ix, cy - hy, cz - iz), Vec3(1f, -1f, -1f))
    tri(Vec3(cx - ix, cy - iy, cz - hz), Vec3(cx - ix, cy - hy, cz - iz), Vec3(cx - hx, cy - iy, cz - iz), Vec3(-1f, -1f, -1f))
  }

  fun addBox(cx: Float, cy: Float, cz: Float, hx: Float, hy: Float, hz: Float, uvScale: Float = 1f) {
    addQuad(Vec3(cx - hx, cy - hy, cz + hz), Vec3(cx + hx, cy - hy, cz + hz), Vec3(cx + hx, cy + hy, cz + hz), Vec3(cx - hx, cy + hy, cz + hz), Vec3(0f, 0f, 1f), uvScale, uvScale)
    addQuad(Vec3(cx + hx, cy - hy, cz - hz), Vec3(cx - hx, cy - hy, cz - hz), Vec3(cx - hx, cy + hy, cz - hz), Vec3(cx + hx, cy + hy, cz - hz), Vec3(0f, 0f, -1f), uvScale, uvScale)
    addQuad(Vec3(cx + hx, cy - hy, cz + hz), Vec3(cx + hx, cy - hy, cz - hz), Vec3(cx + hx, cy + hy, cz - hz), Vec3(cx + hx, cy + hy, cz + hz), Vec3(1f, 0f, 0f), uvScale, uvScale)
    addQuad(Vec3(cx - hx, cy - hy, cz - hz), Vec3(cx - hx, cy - hy, cz + hz), Vec3(cx - hx, cy + hy, cz + hz), Vec3(cx - hx, cy + hy, cz - hz), Vec3(-1f, 0f, 0f), uvScale, uvScale)
    addQuad(Vec3(cx - hx, cy + hy, cz + hz), Vec3(cx + hx, cy + hy, cz + hz), Vec3(cx + hx, cy + hy, cz - hz), Vec3(cx - hx, cy + hy, cz - hz), Vec3(0f, 1f, 0f), uvScale, uvScale)
    addQuad(Vec3(cx - hx, cy - hy, cz - hz), Vec3(cx + hx, cy - hy, cz - hz), Vec3(cx + hx, cy - hy, cz + hz), Vec3(cx - hx, cy - hy, cz + hz), Vec3(0f, -1f, 0f), uvScale, uvScale)
  }

  fun addFinnedBlock(cx: Float, cy: Float, cz: Float, hx: Float, hy: Float, hz: Float, finCount: Int = 7) {
    addChamferedBox(cx, cy, cz, hx * 0.92f, hy * 0.85f, hz * 0.82f, 0.012f, 1.5f)
    val step = (hx * 1.7f) / (finCount - 1).coerceAtLeast(1)
    for (i in 0 until finCount) {
      val fx = cx - hx * 0.85f + i * step
      addChamferedBox(fx, cy, cz, 0.006f, hy, hz, 0.002f, 0.6f)
    }
  }

  fun addSweptTube(points: List<Vec3>, radii: List<Float>, segs: Int = 14, capEnds: Boolean = true) {
    if (points.size < 2) return
    val rings = Array(points.size) { IntArray(segs) }
    var prevSide: Vec3? = null
    for (pIdx in points.indices) {
      val cur = points[pIdx]
      val tangent =
        when (pIdx) {
          0 -> points[1] - points[0]
          points.lastIndex -> points[pIdx] - points[pIdx - 1]
          else -> points[pIdx + 1] - points[pIdx - 1]
        }
      val dir = normVec(tangent)
      val ref = prevSide ?: if (abs(dir.y) < 0.9f) Vec3(0f, 1f, 0f) else Vec3(1f, 0f, 0f)
      val side =
        if (prevSide == null) {
          normVec(Vec3(dir.y * ref.z - dir.z * ref.y, dir.z * ref.x - dir.x * ref.z, dir.x * ref.y - dir.y * ref.x))
        } else {
          // Parallel transport frame to eliminate twisting!
          val dot = ref.x * dir.x + ref.y * dir.y + ref.z * dir.z
          normVec(Vec3(ref.x - dir.x * dot, ref.y - dir.y * dot, ref.z - dir.z * dot))
        }
      prevSide = side
      val binorm = normVec(Vec3(dir.y * side.z - dir.z * side.y, dir.z * side.x - dir.x * side.z, dir.x * side.y - dir.y * side.x))
      val r = radii[pIdx.coerceAtMost(radii.lastIndex)]
      val vCoord = pIdx.toFloat() * 0.65f
      for (i in 0 until segs) {
        val ang = (i.toFloat() / segs) * (2f * PI.toFloat())
        val radial = side * cos(ang) + binorm * sin(ang)
        rings[pIdx][i] = addVert(cur + radial * r, radial, (i.toFloat() / segs) * 2f, vCoord)
      }
    }
    for (pIdx in 0 until points.lastIndex) {
      for (i in 0 until segs) {
        val ni = (i + 1) % segs
        val a0 = rings[pIdx][i]; val a1 = rings[pIdx][ni]
        val b0 = rings[pIdx + 1][i]; val b1 = rings[pIdx + 1][ni]
        indices += a0; indices += a1; indices += b1
        indices += a0; indices += b1; indices += b0
        if (i % 2 == 0) {
          addWireEdge(
            Vec3(positions[a0 * 3], positions[a0 * 3 + 1], positions[a0 * 3 + 2]),
            Vec3(positions[b0 * 3], positions[b0 * 3 + 1], positions[b0 * 3 + 2]),
          )
        }
      }
    }
    if (capEnds) {
      val dirStart = normVec(points[0] - points[1])
      val dirEnd = normVec(points.last() - points[points.lastIndex - 1])
      val cs = addVert(points.first(), dirStart, 0.5f, 0.5f)
      val ce = addVert(points.last(), dirEnd, 0.5f, 0.5f)
      for (i in 0 until segs) {
        val ni = (i + 1) % segs
        indices += cs; indices += rings.first()[ni]; indices += rings.first()[i]
        indices += ce; indices += rings.last()[i]; indices += rings.last()[ni]
      }
    }
  }

  fun addTube(a: Vec3, b: Vec3, radius: Float, segs: Int = 14) {
    addSweptTube(listOf(a, b), listOf(radius, radius), segs, capEnds = true)
  }

  fun addHelicalSpring(a: Vec3, b: Vec3, coilRadius: Float, wireRadius: Float, turns: Float = 6.5f) {
    val axis = b - a
    val len = sqrt(axis.x * axis.x + axis.y * axis.y + axis.z * axis.z).coerceAtLeast(1e-4f)
    val dir = Vec3(axis.x / len, axis.y / len, axis.z / len)
    val up = if (abs(dir.y) < 0.9f) Vec3(0f, 1f, 0f) else Vec3(1f, 0f, 0f)
    val side = normVec(Vec3(dir.y * up.z - dir.z * up.y, dir.z * up.x - dir.x * up.z, dir.x * up.y - dir.y * up.x))
    val binorm = normVec(Vec3(dir.y * side.z - dir.z * side.y, dir.z * side.x - dir.x * side.z, dir.x * side.y - dir.y * side.x))
    val steps = (turns * 10f).toInt().coerceIn(24, 72)
    val pts = ArrayList<Vec3>(steps + 1)
    val rads = ArrayList<Float>(steps + 1)
    for (i in 0..steps) {
      val t = i / steps.toFloat()
      val ang = t * turns * (2f * PI.toFloat())
      val pos = a + axis * t + (side * cos(ang) + binorm * sin(ang)) * coilRadius
      pts += pos
      rads += wireRadius
    }
    addSweptTube(pts, rads, segs = 7, capEnds = true)
  }

  fun addDisc(cx: Float, cy: Float, cz: Float, radius: Float, thickness: Float, segs: Int = 32) {
    val hz = thickness * 0.5f
    val cF = addVert(Vec3(cx, cy, cz + hz), Vec3(0f, 0f, 1f), 0.5f, 0.5f)
    val cB = addVert(Vec3(cx, cy, cz - hz), Vec3(0f, 0f, -1f), 0.5f, 0.5f)
    val fRing = IntArray(segs)
    val bRing = IntArray(segs)
    val sideF = IntArray(segs)
    val sideB = IntArray(segs)
    for (i in 0 until segs) {
      val ang = (i.toFloat() / segs) * (2f * PI.toFloat())
      val ca = cos(ang); val sa = sin(ang)
      val x = cx + radius * ca
      val y = cy + radius * sa
      val u = 0.5f + 0.5f * ca
      val v = 0.5f + 0.5f * sa
      fRing[i] = addVert(Vec3(x, y, cz + hz), Vec3(0f, 0f, 1f), u, v)
      bRing[i] = addVert(Vec3(x, y, cz - hz), Vec3(0f, 0f, -1f), u, v)
      sideF[i] = addVert(Vec3(x, y, cz + hz), Vec3(ca, sa, 0f), i / segs.toFloat(), 1f)
      sideB[i] = addVert(Vec3(x, y, cz - hz), Vec3(ca, sa, 0f), i / segs.toFloat(), 0f)
    }
    for (i in 0 until segs) {
      val ni = (i + 1) % segs
      indices += cF; indices += fRing[i]; indices += fRing[ni]
      indices += cB; indices += bRing[ni]; indices += bRing[i]
      indices += sideF[i]; indices += sideB[i]; indices += sideB[ni]
      indices += sideF[i]; indices += sideB[ni]; indices += sideF[ni]
      addWireEdge(Vec3(positions[fRing[i] * 3], positions[fRing[i] * 3 + 1], cz + hz), Vec3(positions[fRing[ni] * 3], positions[fRing[ni] * 3 + 1], cz + hz))
    }
  }

  fun addHorizontalDisc(cx: Float, cy: Float, cz: Float, radius: Float, thickness: Float, segs: Int = 40) {
    val hy = thickness * 0.5f
    val cT = addVert(Vec3(cx, cy + hy, cz), Vec3(0f, 1f, 0f), 0.5f, 0.5f)
    val cB = addVert(Vec3(cx, cy - hy, cz), Vec3(0f, -1f, 0f), 0.5f, 0.5f)
    val tRing = IntArray(segs)
    val bRing = IntArray(segs)
    val sideT = IntArray(segs)
    val sideB = IntArray(segs)
    for (i in 0 until segs) {
      val ang = (i.toFloat() / segs) * (2f * PI.toFloat())
      val ca = cos(ang); val sa = sin(ang)
      val x = cx + radius * ca
      val z = cz + radius * sa
      val u = 0.5f + 0.5f * ca
      val v = 0.5f + 0.5f * sa
      tRing[i] = addVert(Vec3(x, cy + hy, z), Vec3(0f, 1f, 0f), u, v)
      bRing[i] = addVert(Vec3(x, cy - hy, z), Vec3(0f, -1f, 0f), u, v)
      sideT[i] = addVert(Vec3(x, cy + hy, z), Vec3(ca, 0f, sa), i / segs.toFloat(), 1f)
      sideB[i] = addVert(Vec3(x, cy - hy, z), Vec3(ca, 0f, sa), i / segs.toFloat(), 0f)
    }
    for (i in 0 until segs) {
      val ni = (i + 1) % segs
      indices += cT; indices += tRing[ni]; indices += tRing[i]
      indices += cB; indices += bRing[i]; indices += bRing[ni]
      indices += sideT[i]; indices += sideT[ni]; indices += sideB[ni]
      indices += sideT[i]; indices += sideB[ni]; indices += sideB[i]
    }
  }

  fun addWheelTireWithKnobs(cx: Float, cy: Float, cz: Float, outerR: Float, innerR: Float, width: Float, segs: Int = 36) {
    val hw = width * 0.5f
    val midR = (outerR + innerR) * 0.5f
    val tubeR = (outerR - innerR) * 0.5f
    val profileSteps = 14
    val grid = Array(segs) { IntArray(profileSteps) }
    for (i in 0 until segs) {
      val theta = (i.toFloat() / segs) * (2f * PI.toFloat())
      val ct = cos(theta); val st = sin(theta)
      for (j in 0 until profileSteps) {
        val phi = (j.toFloat() / profileSteps) * (2f * PI.toFloat())
        val cp = cos(phi); val sp = sin(phi)
        // Flatten crown slightly for realistic motorcycle/scooter tire profile
        val crownFactor = if (cp > 0.3f) 0.92f else 1.0f
        val r = midR + tubeR * cp * crownFactor
        val x = cx + r * ct
        val y = cy + r * st
        val z = cz + hw * sp
        grid[i][j] = addVert(Vec3(x, y, z), Vec3(cp * ct, cp * st, sp), (i / segs.toFloat()) * 6f, (j / profileSteps.toFloat()) * 2f)
      }
    }
    for (i in 0 until segs) {
      val ni = (i + 1) % segs
      for (j in 0 until profileSteps) {
        val nj = (j + 1) % profileSteps
        val a = grid[i][j]; val b = grid[ni][j]; val c = grid[ni][nj]; val d = grid[i][nj]
        indices += a; indices += b; indices += c
        indices += a; indices += c; indices += d
        if (j % 3 == 0 && i % 2 == 0) {
          addWireEdge(
            Vec3(positions[a * 3], positions[a * 3 + 1], positions[a * 3 + 2]),
            Vec3(positions[b * 3], positions[b * 3 + 1], positions[b * 3 + 2]),
          )
        }
      }
    }
    // 3D sculpted knobby tread blocks staggered left/center/right around the tire crown
    val lugCount = 24
    for (k in 0 until lugCount) {
      val theta = (k.toFloat() / lugCount) * (2f * PI.toFloat())
      val ct = cos(theta); val st = sin(theta)
      val rCrown = outerR - 0.004f
      val zShift = when (k % 3) {
        0 -> 0f
        1 -> hw * 0.44f
        else -> -hw * 0.44f
      }
      val lugH = 0.018f
      val lugW = hw * 0.34f
      val lx = cx + rCrown * ct
      val ly = cy + rCrown * st
      // Oriented small knobby block along radial direction
      val tx = -st * 0.022f
      val ty = ct * 0.022f
      val nx = ct * lugH
      val ny = st * lugH
      val p0 = Vec3(lx - tx, ly - ty, cz + zShift - lugW)
      val p1 = Vec3(lx + tx, ly + ty, cz + zShift - lugW)
      val p2 = Vec3(lx + tx, ly + ty, cz + zShift + lugW)
      val p3 = Vec3(lx - tx, ly - ty, cz + zShift + lugW)
      val q0 = Vec3(p0.x + nx, p0.y + ny, p0.z)
      val q1 = Vec3(p1.x + nx, p1.y + ny, p1.z)
      val q2 = Vec3(p2.x + nx, p2.y + ny, p2.z)
      val q3 = Vec3(p3.x + nx, p3.y + ny, p3.z)
      addQuad(q0, q1, q2, q3, Vec3(ct, st, 0f), 0.5f, 0.5f, false)
      addQuad(p0, p1, q1, q0, Vec3(0f, 0f, -1f), 0.3f, 0.3f, false)
      addQuad(p2, p3, q3, q2, Vec3(0f, 0f, 1f), 0.3f, 0.3f, false)
      addQuad(p1, p2, q2, q1, Vec3(-st, ct, 0f), 0.3f, 0.3f, false)
      addQuad(p3, p0, q0, q3, Vec3(st, -ct, 0f), 0.3f, 0.3f, false)
    }
  }

  fun addCurvedFender(cx: Float, cy: Float, radius: Float, startDeg: Float, endDeg: Float, halfW: Float, thickness: Float = 0.022f, steps: Int = 14) {
    val gridOut = Array(steps + 1) { IntArray(3) }
    val gridIn = Array(steps + 1) { IntArray(3) }
    for (i in 0..steps) {
      val t = i / steps.toFloat()
      val deg = startDeg + (endDeg - startDeg) * t
      val rad = (deg * PI / 180.0).toFloat()
      val cr = cos(rad); val sr = sin(rad)
      val rOut = radius + thickness
      val zArr = floatArrayOf(-halfW, 0f, halfW)
      val crownDrop = floatArrayOf(-0.016f, 0.008f, -0.016f)
      for (k in 0..2) {
        val rrOut = rOut + crownDrop[k]
        val rrIn = radius + crownDrop[k]
        gridOut[i][k] = addVert(Vec3(cx + rrOut * cr, cy + rrOut * sr, zArr[k]), Vec3(cr, sr, (k - 1) * 0.25f), t * 2f, k * 0.5f)
        gridIn[i][k] = addVert(Vec3(cx + rrIn * cr, cy + rrIn * sr, zArr[k]), Vec3(-cr, -sr, (k - 1) * 0.25f), t * 2f, k * 0.5f)
      }
    }
    for (i in 0 until steps) {
      for (k in 0 until 2) {
        indices += gridOut[i][k]; indices += gridOut[i + 1][k]; indices += gridOut[i + 1][k + 1]
        indices += gridOut[i][k]; indices += gridOut[i + 1][k + 1]; indices += gridOut[i][k + 1]
        indices += gridIn[i][k + 1]; indices += gridIn[i + 1][k + 1]; indices += gridIn[i + 1][k]
        indices += gridIn[i][k + 1]; indices += gridIn[i + 1][k]; indices += gridIn[i][k]
      }
    }
  }

  /**
   * Generates NACA-0012 symmetric hydrofoil / aero wing lofted along Z with sweep & taper.
   */
  fun addAirfoilWing(
    cx: Float,
    cy: Float,
    cz: Float,
    chord: Float,
    thickness: Float,
    halfSpan: Float,
    sweepBack: Float = 0.08f,
    segsChord: Int = 12,
    segsSpan: Int = 8,
  ) {
    val profilePts = segsChord * 2
    val slices = segsSpan * 2 + 1
    val grid = Array(slices) { IntArray(profilePts) }
    for (s in 0 until slices) {
      val spanNorm = (s - segsSpan) / segsSpan.toFloat() // -1..+1
      val absSpan = abs(spanNorm)
      val taper = 1f - 0.48f * absSpan * absSpan
      val localChord = chord * taper
      val localThick = thickness * (1f - 0.40f * absSpan)
      val xSweep = cx - absSpan * sweepBack
      val zPos = cz + spanNorm * halfSpan
      val yDihedral = cy + absSpan * 0.028f

      for (p in 0 until profilePts) {
        val isUpper = p < segsChord
        val t = if (isUpper) p / segsChord.toFloat() else (profilePts - p) / segsChord.toFloat()
        // NACA 00xx half-thickness equation
        val yt = 5f * localThick * (
          0.2969f * sqrt(t.coerceAtLeast(1e-5f)) -
            0.1260f * t -
            0.3516f * t * t +
            0.2843f * t * t * t -
            0.1015f * t * t * t * t
          )
        val xPos = xSweep + (0.5f - t) * localChord
        val yPos = yDihedral + if (isUpper) yt else -yt * 0.72f
        val ny = if (isUpper) 1f else -1f
        val nx = if (t < 0.12f) 0.65f else 0f
        grid[s][p] = addVert(Vec3(xPos, yPos, zPos), Vec3(nx, ny, spanNorm * 0.18f), t, (spanNorm + 1f) * 0.5f)
      }
    }
    for (s in 0 until slices - 1) {
      for (p in 0 until profilePts) {
        val np = (p + 1) % profilePts
        val a = grid[s][p]; val b = grid[s + 1][p]; val c = grid[s + 1][np]; val d = grid[s][np]
        indices += a; indices += b; indices += c
        indices += a; indices += c; indices += d
        if (s % 2 == 0 && p % 3 == 0) {
          addWireEdge(
            Vec3(positions[a * 3], positions[a * 3 + 1], positions[a * 3 + 2]),
            Vec3(positions[b * 3], positions[b * 3 + 1], positions[b * 3 + 2]),
          )
        }
      }
    }
  }

  /**
   * Lofted hydrodynamic surf/eFoil deck hull with tapered nose rocker, wide stance pad, and swallow tail.
   */
  fun addSculptedDeckHull(
    cx: Float,
    cy: Float,
    cz: Float,
    halfLen: Float,
    halfThick: Float,
    halfWidth: Float,
    lengthSteps: Int = 16,
    ringSegs: Int = 14,
  ) {
    val rings = Array(lengthSteps + 1) { IntArray(ringSegs) }
    for (i in 0..lengthSteps) {
      val t = i / lengthSteps.toFloat() // 0=tail .. 1=nose
      val xNorm = t * 2f - 1f // -1..+1
      val widthFactor = sqrt((1f - xNorm * xNorm * 0.88f).coerceAtLeast(0.08f))
      val thickFactor = if (xNorm > 0.6f) (1f - (xNorm - 0.6f) * 1.1f).coerceAtLeast(0.32f) else 1f
      val rockerY = if (xNorm > 0.35f) (xNorm - 0.35f) * (xNorm - 0.35f) * 0.12f else 0f
      val x = cx + xNorm * halfLen
      val w = halfWidth * widthFactor
      val h = halfThick * thickFactor
      for (j in 0 until ringSegs) {
        val ang = (j / ringSegs.toFloat()) * (2f * PI.toFloat())
        val ca = cos(ang)
        val sa = sin(ang)
        // Flatter deck on top, slightly V-chined hull below
        val ySign = if (sa >= 0f) sa * 0.85f else sa * 1.05f
        val y = cy + rockerY + h * ySign
        val z = cz + w * ca
        rings[i][j] = addVert(Vec3(x, y, z), Vec3(xNorm * 0.18f, ySign, ca), t * 2.5f, j / ringSegs.toFloat())
      }
    }
    for (i in 0 until lengthSteps) {
      for (j in 0 until ringSegs) {
        val nj = (j + 1) % ringSegs
        val a = rings[i][j]; val b = rings[i + 1][j]; val c = rings[i + 1][nj]; val d = rings[i][nj]
        indices += a; indices += b; indices += c
        indices += a; indices += c; indices += d
      }
    }
    val tailC = addVert(Vec3(cx - halfLen, cy, cz), Vec3(-1f, 0f, 0f), 0f, 0.5f)
    val noseC = addVert(Vec3(cx + halfLen, cy + 0.05f, cz), Vec3(1f, 0.2f, 0f), 1f, 0.5f)
    for (j in 0 until ringSegs) {
      val nj = (j + 1) % ringSegs
      indices += tailC; indices += rings[0][nj]; indices += rings[0][j]
      indices += noseC; indices += rings[lengthSteps][j]; indices += rings[lengthSteps][nj]
    }
  }

  /**
   * CNC Rear Sprocket / Front Chainring with perimeter teeth lugs.
   */
  fun addSprocketWithTeeth(cx: Float, cy: Float, cz: Float, radius: Float, thickness: Float, teethCount: Int = 22) {
    addDisc(cx, cy, cz, radius * 0.88f, thickness, 24)
    val toothH = radius * 0.16f
    val toothW = (2f * PI.toFloat() * radius / teethCount) * 0.28f
    val hz = thickness * 0.45f
    for (i in 0 until teethCount) {
      val ang = (i / teethCount.toFloat()) * (2f * PI.toFloat())
      val ca = cos(ang); val sa = sin(ang)
      val rBase = radius * 0.86f
      val rTip = rBase + toothH
      val tx = -sa * toothW; val ty = ca * toothW
      val p0 = Vec3(cx + rBase * ca - tx, cy + rBase * sa - ty, cz - hz)
      val p1 = Vec3(cx + rBase * ca + tx, cy + rBase * sa + ty, cz - hz)
      val p2 = Vec3(cx + rBase * ca + tx, cy + rBase * sa + ty, cz + hz)
      val p3 = Vec3(cx + rBase * ca - tx, cy + rBase * sa - ty, cz + hz)
      val q0 = Vec3(cx + rTip * ca - tx * 0.6f, cy + rTip * sa - ty * 0.6f, cz - hz)
      val q1 = Vec3(cx + rTip * ca + tx * 0.6f, cy + rTip * sa + ty * 0.6f, cz - hz)
      val q2 = Vec3(cx + rTip * ca + tx * 0.6f, cy + rTip * sa + ty * 0.6f, cz + hz)
      val q3 = Vec3(cx + rTip * ca - tx * 0.6f, cy + rTip * sa - ty * 0.6f, cz + hz)
      addQuad(q0, q1, q2, q3, Vec3(ca, sa, 0f), 0.3f, 0.3f, false)
      addQuad(p0, p1, q1, q0, Vec3(0f, 0f, -1f), 0.2f, 0.2f, false)
      addQuad(p2, p3, q3, q2, Vec3(0f, 0f, 1f), 0.2f, 0.2f, false)
    }
  }

  /**
   * Continuous drive chain loop wrapping between front motor/crank sprocket and rear wheel sprocket.
   */
  fun addChainDriveLoop(frontAxle: Vec3, rearAxle: Vec3, rFront: Float, rRear: Float, zOffset: Float) {
    val fTop = Vec3(frontAxle.x, frontAxle.y + rFront, zOffset)
    val fBot = Vec3(frontAxle.x, frontAxle.y - rFront, zOffset)
    val rTop = Vec3(rearAxle.x, rearAxle.y + rRear, zOffset)
    val rBot = Vec3(rearAxle.x, rearAxle.y - rRear, zOffset)
    addTube(fTop, rTop, 0.0085f, 6)
    addTube(rBot, fBot, 0.0085f, 6)
  }

  /**
   * Computes MikkTSpace-compatible orthogonalized VEC4 tangents from mesh triangles and UVs.
   */
  fun computeTangentsVec4(): ArrayList<Float> {
    val tan1X = FloatArray(vertCount)
    val tan1Y = FloatArray(vertCount)
    val tan1Z = FloatArray(vertCount)
    val tan2X = FloatArray(vertCount)
    val tan2Y = FloatArray(vertCount)
    val tan2Z = FloatArray(vertCount)

    var t = 0
    while (t + 2 < indices.size) {
      val i0 = indices[t]
      val i1 = indices[t + 1]
      val i2 = indices[t + 2]
      t += 3

      val x0 = positions[i0 * 3]; val y0 = positions[i0 * 3 + 1]; val z0 = positions[i0 * 3 + 2]
      val x1 = positions[i1 * 3]; val y1 = positions[i1 * 3 + 1]; val z1 = positions[i1 * 3 + 2]
      val x2 = positions[i2 * 3]; val y2 = positions[i2 * 3 + 1]; val z2 = positions[i2 * 3 + 2]

      val u0 = uvs[i0 * 2]; val v0 = uvs[i0 * 2 + 1]
      val u1 = uvs[i1 * 2]; val v1 = uvs[i1 * 2 + 1]
      val u2 = uvs[i2 * 2]; val v2 = uvs[i2 * 2 + 1]

      val dx1 = x1 - x0; val dy1 = y1 - y0; val dz1 = z1 - z0
      val dx2 = x2 - x0; val dy2 = y2 - y0; val dz2 = z2 - z0
      val du1 = u1 - u0; val dv1 = v1 - v0
      val du2 = u2 - u0; val dv2 = v2 - v0

      val denom = du1 * dv2 - du2 * dv1
      if (abs(denom) > 1e-7f) {
        val inv = 1f / denom
        val sx = (dv2 * dx1 - dv1 * dx2) * inv
        val sy = (dv2 * dy1 - dv1 * dy2) * inv
        val sz = (dv2 * dz1 - dv1 * dz2) * inv
        val bx = (-du2 * dx1 + du1 * dx2) * inv
        val by = (-du2 * dy1 + du1 * dy2) * inv
        val bz = (-du2 * dz1 + du1 * dz2) * inv

        tan1X[i0] += sx; tan1Y[i0] += sy; tan1Z[i0] += sz
        tan1X[i1] += sx; tan1Y[i1] += sy; tan1Z[i1] += sz
        tan1X[i2] += sx; tan1Y[i2] += sy; tan1Z[i2] += sz

        tan2X[i0] += bx; tan2Y[i0] += by; tan2Z[i0] += bz
        tan2X[i1] += bx; tan2Y[i1] += by; tan2Z[i1] += bz
        tan2X[i2] += bx; tan2Y[i2] += by; tan2Z[i2] += bz
      }
    }

    val out = ArrayList<Float>(vertCount * 4)
    for (v in 0 until vertCount) {
      val nx = normals[v * 3]; val ny = normals[v * 3 + 1]; val nz = normals[v * 3 + 2]
      var tx = tan1X[v]; var ty = tan1Y[v]; var tz = tan1Z[v]
      val dotNt = nx * tx + ny * ty + nz * tz
      tx -= nx * dotNt
      ty -= ny * dotNt
      tz -= nz * dotNt
      val lenSq = tx * tx + ty * ty + tz * tz
      if (lenSq < 1e-6f) {
        // Fallback orthonormal tangent
        val refX = if (abs(ny) < 0.9f) 0f else 1f
        val refY = if (abs(ny) < 0.9f) 1f else 0f
        val refZ = 0f
        tx = ny * refZ - nz * refY
        ty = nz * refX - nx * refZ
        tz = nx * refY - ny * refX
      }
      val invL = 1f / sqrt((tx * tx + ty * ty + tz * tz).coerceAtLeast(1e-7f))
      tx *= invL; ty *= invL; tz *= invL

      val cx = ny * tz - nz * ty
      val cy = nz * tx - nx * tz
      val cz = nx * ty - ny * tx
      val hand = if ((cx * tan2X[v] + cy * tan2Y[v] + cz * tan2Z[v]) < 0f) -1f else 1f

      out += tx; out += ty; out += tz; out += hand
    }
    return out
  }
}

internal object Pev3dGltfBuilder {
  fun buildVehicleGlb(
    archetype: Pev3dArchetype,
    finish: Pev3dModelFinish,
    lightsOn: Boolean,
    xrayShell: Boolean = false,
    carbonGlossMode: Boolean = false,
  ): PevBuiltGlb {
    val buckets = ArrayList<MeshBucket>()
    val wheelAxleCenters = HashMap<String, Vec3>()
    val hsMap = Pev3dEngine.hotspotsForArchetype(archetype).associateBy { it.partKey }
    val explodeDirs = hsMap.mapValues { it.value.explodeDir }.toMutableMap()

    fun newBucket(
      nodeName: String,
      partKey: String,
      texId: PevPbrMaterialId,
      baseRgb: FloatArray,
      metallic: Float,
      roughness: Float,
      emissiveRgb: FloatArray = floatArrayOf(0f, 0f, 0f),
      clearcoat: Float = 0f,
      isShell: Boolean = false,
      isScreen: Boolean = false,
      useEmissiveTexture: Boolean = false,
      block: MeshBucket.() -> Unit,
    ) {
      val effRough = if (carbonGlossMode) (roughness * 0.65f).coerceAtLeast(0.06f) else roughness
      val effClear = if (carbonGlossMode && (isShell || texId == PevPbrMaterialId.CARBON_WEAVE || texId == PevPbrMaterialId.RACING_LIVERY)) 1.0f else clearcoat
      val bucket = MeshBucket(
        nodeName = nodeName,
        matKey = PevMaterialKey(
          partKey = partKey,
          texId = texId,
          baseRgb = baseRgb,
          metallic = metallic,
          roughness = effRough,
          emissiveRgb = emissiveRgb,
          clearcoat = effClear,
          isShell = isShell,
          isScreen = isScreen,
          useEmissiveTexture = useEmissiveTexture,
        ),
      )
      bucket.block()
      if (bucket.positions.isNotEmpty()) {
        buckets += bucket
      }
    }

    val darkRubber = floatArrayOf(0.88f, 0.90f, 0.94f)
    val cncSilver = floatArrayOf(0.86f, 0.89f, 0.94f)
    val darkAnodized = floatArrayOf(0.28f, 0.32f, 0.38f)
    val saddleCharcoal = floatArrayOf(0.88f, 0.90f, 0.94f)
    val battGreen = floatArrayOf(0.24f, 0.88f, 0.48f)
    val motorCopper = floatArrayOf(0.94f, 0.48f, 0.20f)
    val focCrimson = floatArrayOf(0.90f, 0.24f, 0.56f)
    val tftWhite = floatArrayOf(1.0f, 1.0f, 1.0f)

    // Dyno Studio Turntable Pad
    newBucket(
      nodeName = "floor",
      partKey = "floor",
      texId = PevPbrMaterialId.DYNO_FLOOR,
      baseRgb = floatArrayOf(0.95f, 0.97f, 1.0f),
      metallic = 0.30f,
      roughness = 0.42f,
    ) {
      addQuad(
        Vec3(-2.1f, -0.78f, 2.1f),
        Vec3(2.1f, -0.78f, 2.1f),
        Vec3(2.1f, -0.78f, -2.1f),
        Vec3(-2.1f, -0.78f, -2.1f),
        Vec3(0f, 1f, 0f),
        1f,
        1f,
        wire = false,
      )
      // Beveled cylindrical dyno turntable plinth under the vehicle
      addHorizontalDisc(0f, -0.805f, 0f, 1.48f, 0.044f, 40)
    }
    // Neon Turntable Perimeter Ring
    newBucket(
      nodeName = "floor_ring",
      partKey = "floor",
      texId = PevPbrMaterialId.BRUSHED_METAL,
      baseRgb = finish.accentRgb,
      metallic = 0.90f,
      roughness = 0.16f,
      emissiveRgb = floatArrayOf(finish.accentRgb[0] * 0.42f, finish.accentRgb[1] * 0.42f, finish.accentRgb[2] * 0.42f),
    ) {
      val ringSegs = 40
      val r = 1.48f
      val pts = (0..ringSegs).map { i ->
        val a = (i.toFloat() / ringSegs) * (2f * PI.toFloat())
        Vec3(r * cos(a), -0.772f, r * sin(a))
      }
      addSweptTube(pts, List(pts.size) { 0.012f }, segs = 6, capEnds = false)
    }

    fun addDetailedWheel(
      nodeName: String,
      cx: Float,
      cy: Float,
      cz: Float,
      outerR: Float,
      width: Float,
      motorKey: String,
      isPowered: Boolean,
    ) {
      wheelAxleCenters[nodeName] = Vec3(cx, cy, cz)
      explodeDirs[nodeName] = hsMap[motorKey]?.explodeDir ?: Vec3(0f, 0f, 0f)
      val innerR = outerR * 0.62f

      // Tire + 3D knobby lugs
      newBucket(
        nodeName = nodeName,
        partKey = motorKey,
        texId = PevPbrMaterialId.TIRE_RUBBER,
        baseRgb = darkRubber,
        metallic = 0.06f,
        roughness = 0.84f,
      ) {
        addWheelTireWithKnobs(cx, cy, cz, outerR, innerR, width, 36)
      }

      // Concave forged rim & motor hub + 10 radial spokes
      val rimRgb = if (isPowered) finish.accentRgb else cncSilver
      newBucket(
        nodeName = nodeName,
        partKey = motorKey,
        texId = PevPbrMaterialId.BRUSHED_METAL,
        baseRgb = rimRgb,
        metallic = 0.92f,
        roughness = 0.20f,
        clearcoat = 0.35f,
      ) {
        // Inner hub barrel & dual flanges
        addDisc(cx, cy, cz, innerR * 0.56f, width * 0.78f, 28)
        addDisc(cx, cy, cz, innerR * 0.34f, width * 0.94f, 24)
        // Outer anodized rim lip ring
        val lipPts = (0..32).map { i ->
          val a = (i / 32f) * (2f * PI.toFloat())
          Vec3(cx + innerR * 1.01f * cos(a), cy + innerR * 1.01f * sin(a), cz + width * 0.36f)
        }
        addSweptTube(lipPts, List(lipPts.size) { 0.016f }, 6, false)
        val lipPtsB = (0..32).map { i ->
          val a = (i / 32f) * (2f * PI.toFloat())
          Vec3(cx + innerR * 1.01f * cos(a), cy + innerR * 1.01f * sin(a), cz - width * 0.36f)
        }
        addSweptTube(lipPtsB, List(lipPtsB.size) { 0.016f }, 6, false)

        // Cross-laced dual-side CNC spokes
        for (s in 0 until 10) {
          val a = (s / 10f) * (2f * PI.toFloat())
          val zSide = if (s % 2 == 0) width * 0.26f else -width * 0.26f
          addTube(
            Vec3(cx + innerR * 0.30f * cos(a), cy + innerR * 0.30f * sin(a), cz + zSide),
            Vec3(cx + innerR * 1.00f * cos(a + 0.14f), cy + innerR * 1.00f * sin(a + 0.14f), cz),
            0.013f,
            8,
          )
        }
      }

      // Cross-drilled floating stainless brake rotor (spins with wheel!)
      val bz = cz + width * 0.50f + 0.028f
      newBucket(
        nodeName = nodeName,
        partKey = "brakes",
        texId = PevPbrMaterialId.BRAKE_ROTOR,
        baseRgb = cncSilver,
        metallic = 0.95f,
        roughness = 0.16f,
      ) {
        addDisc(cx, cy, bz, outerR * 0.50f, 0.018f, 32)
      }

      // Fixed 4-Piston CNC Caliper + Mount Carrier (mounted to brakes node)
      newBucket(
        nodeName = "brakes",
        partKey = "brakes",
        texId = PevPbrMaterialId.BRUSHED_METAL,
        baseRgb = finish.caliperRgb,
        metallic = 0.88f,
        roughness = 0.22f,
        clearcoat = 0.6f,
      ) {
        addChamferedBox(cx + outerR * 0.35f, cy + outerR * 0.27f, bz, 0.072f, 0.062f, 0.044f, 0.015f)
        addChamferedBox(cx + outerR * 0.24f, cy + outerR * 0.18f, bz - 0.025f, 0.042f, 0.038f, 0.020f, 0.008f)
      }
    }

    val ws = finish.wheelScale
    val ls = finish.bodyLengthScale

    when (archetype) {
      Pev3dArchetype.SCOOTER -> {
        addDetailedWheel("wheel_rear", -0.92f * ls, -0.45f, 0f, 0.30f * ws, 0.24f, "motor", true)
        addDetailedWheel("wheel_front", 0.92f * ls, -0.45f, 0f, 0.30f * ws, 0.24f, "suspension", true)

        // Sculpted Carbon Deck + Grip Pad + Dual-Stem Gusset + Curved Fenders
        newBucket(
          nodeName = "frame",
          partKey = "frame",
          texId = PevPbrMaterialId.CARBON_WEAVE,
          baseRgb = finish.bodyRgb,
          metallic = 0.24f,
          roughness = 0.28f,
          clearcoat = 0.85f,
          isShell = true,
        ) {
          addChamferedBox(0f, -0.27f, 0f, 0.72f * ls, 0.076f, 0.29f, 0.026f, 2f)
          // Kickplate rear footrest
          addChamferedBox(-0.62f * ls, -0.17f, 0f, 0.14f * ls, 0.055f, 0.24f, 0.018f, 1.5f)
          // Twin Aerospace Down-tubes + Folding Neck Gusset
          addSweptTube(
            listOf(Vec3(0.70f * ls, -0.22f, -0.08f), Vec3(0.62f * ls, 0.05f, -0.075f), Vec3(0.42f * ls, 0.88f, -0.075f)),
            listOf(0.038f, 0.034f, 0.030f),
            14,
          )
          addSweptTube(
            listOf(Vec3(0.70f * ls, -0.22f, 0.08f), Vec3(0.62f * ls, 0.05f, 0.075f), Vec3(0.42f * ls, 0.88f, 0.075f)),
            listOf(0.038f, 0.034f, 0.030f),
            14,
          )
          // Rear & Front Arched Fenders
          addCurvedFender(-0.92f * ls, -0.45f, 0.34f * ws, 55f, 175f, 0.14f)
          addCurvedFender(0.92f * ls, -0.45f, 0.34f * ws, 20f, 130f, 0.14f)
        }
        // Anti-slip rubber deck grip mat
        newBucket("frame_grip", "frame", PevPbrMaterialId.TIRE_RUBBER, darkRubber, 0.05f, 0.86f) {
          addChamferedBox(0.02f * ls, -0.188f, 0f, 0.56f * ls, 0.010f, 0.245f, 0.006f, 2.5f)
        }
        // Deep Racing Livery Side Shrouds + Front Aero Shield + Under-Deck Skid Armor
        newBucket("frame_graphics", "frame", PevPbrMaterialId.RACING_LIVERY, finish.accentRgb, 0.42f, 0.16f, clearcoat = 0.96f, isShell = true) {
          addChamferedBox(0.02f * ls, -0.265f, 0.296f, 0.52f * ls, 0.068f, 0.014f, 0.008f, 1.0f)
          addChamferedBox(0.02f * ls, -0.265f, -0.296f, 0.52f * ls, 0.068f, 0.014f, 0.008f, 1.0f)
          addChamferedBox(0.55f * ls, 0.50f, 0f, 0.018f, 0.22f, 0.105f, 0.008f, 1.0f)
        }

        newBucket("battery", "battery", PevPbrMaterialId.BATTERY_PACK, battGreen, 0.45f, 0.34f, floatArrayOf(0.04f, 0.22f, 0.10f)) {
          addChamferedBox(0f, -0.24f, 0f, 0.50f * ls, 0.056f, 0.21f, 0.014f, 2f)
        }

        newBucket("controller", "controller", PevPbrMaterialId.BRUSHED_METAL, focCrimson, 0.90f, 0.22f, floatArrayOf(0.18f, 0.03f, 0.10f), clearcoat = 0.4f) {
          addFinnedBlock(0.42f * ls, -0.22f, 0f, 0.15f, 0.062f, 0.21f, 7)
        }

        newBucket("suspension", "suspension", PevPbrMaterialId.BRUSHED_METAL, finish.accentRgb, 0.92f, 0.18f, clearcoat = 0.5f) {
          // Anodized Deck Side Protection Rails & CNC Folding Collar
          addTube(Vec3(-0.68f * ls, -0.26f, 0.295f), Vec3(0.68f * ls, -0.26f, 0.295f), 0.015f, 8)
          addTube(Vec3(-0.68f * ls, -0.26f, -0.295f), Vec3(0.68f * ls, -0.26f, -0.295f), 0.015f, 8)
          addChamferedBox(0.61f * ls, 0.08f, 0f, 0.052f, 0.048f, 0.115f, 0.012f)
          // Hydraulic Steering Damper cylinder
          addTube(Vec3(0.56f * ls, -0.10f, 0.12f), Vec3(0.64f * ls, 0.14f, 0.04f), 0.016f, 10)
          // C-Arm CNC Swingarms
          addTube(Vec3(-0.68f * ls, -0.26f, -0.15f), Vec3(-0.92f * ls, -0.45f, -0.15f), 0.030f, 12)
          addTube(Vec3(-0.68f * ls, -0.26f, 0.15f), Vec3(-0.92f * ls, -0.45f, 0.15f), 0.030f, 12)
          addTube(Vec3(0.68f * ls, -0.18f, -0.15f), Vec3(0.92f * ls, -0.45f, -0.15f), 0.032f, 12)
          addTube(Vec3(0.68f * ls, -0.18f, 0.15f), Vec3(0.92f * ls, -0.45f, 0.15f), 0.032f, 12)
          // Coiled Dual Springs
          addHelicalSpring(Vec3(-0.56f * ls, -0.22f, -0.15f), Vec3(-0.82f * ls, -0.40f, -0.15f), 0.036f, 0.008f, 5.5f)
          addHelicalSpring(Vec3(-0.56f * ls, -0.22f, 0.15f), Vec3(-0.82f * ls, -0.40f, 0.15f), 0.036f, 0.008f, 5.5f)
          addHelicalSpring(Vec3(0.62f * ls, -0.16f, 0.15f), Vec3(0.84f * ls, -0.38f, 0.15f), 0.036f, 0.008f, 5.5f)
        }

        // Braided Hydraulic Brake Hoses along stem and swingarms
        newBucket("brakes", "brakes", PevPbrMaterialId.CARBON_WEAVE, finish.caliperRgb, 0.35f, 0.38f) {
          addSweptTube(
            listOf(Vec3(0.45f * ls, 0.86f, 0.24f), Vec3(0.55f * ls, 0.42f, 0.14f), Vec3(0.70f * ls, 0.02f, 0.16f), Vec3(0.94f * ls, -0.35f, 0.15f)),
            listOf(0.008f, 0.008f, 0.008f, 0.008f),
            6,
          )
          addSweptTube(
            listOf(Vec3(0.45f * ls, 0.86f, -0.24f), Vec3(0.52f * ls, -0.16f, -0.14f), Vec3(-0.58f * ls, -0.22f, -0.14f), Vec3(-0.86f * ls, -0.38f, 0.14f)),
            listOf(0.008f, 0.008f, 0.008f, 0.008f),
            6,
          )
        }

        newBucket("cockpit", "cockpit", PevPbrMaterialId.BRUSHED_METAL, darkAnodized, 0.85f, 0.28f) {
          // Swept Ergonomic Handlebars + Levers + Master Cylinders
          addSweptTube(
            listOf(Vec3(0.40f * ls, 0.89f, -0.46f), Vec3(0.43f * ls, 0.88f, -0.18f), Vec3(0.42f * ls, 0.88f, 0f), Vec3(0.43f * ls, 0.88f, 0.18f), Vec3(0.40f * ls, 0.89f, 0.46f)),
            listOf(0.026f, 0.022f, 0.025f, 0.022f, 0.026f),
            12,
          )
          addTube(Vec3(0.44f * ls, 0.88f, -0.28f), Vec3(0.51f * ls, 0.86f, -0.40f), 0.009f, 6)
          addTube(Vec3(0.44f * ls, 0.88f, 0.28f), Vec3(0.51f * ls, 0.86f, 0.40f), 0.009f, 6)
          addChamferedBox(0.44f * ls, 0.90f, -0.26f, 0.022f, 0.016f, 0.026f, 0.004f)
          addChamferedBox(0.44f * ls, 0.90f, 0.26f, 0.022f, 0.016f, 0.026f, 0.004f)
          addChamferedBox(0.45f * ls, 0.94f, 0f, 0.056f, 0.048f, 0.115f, 0.01f)
        }
        newBucket("cockpit_grips", "cockpit", PevPbrMaterialId.TIRE_RUBBER, darkRubber, 0.05f, 0.82f) {
          addTube(Vec3(0.415f * ls, 0.886f, 0.28f), Vec3(0.40f * ls, 0.89f, 0.47f), 0.032f, 10)
          addTube(Vec3(0.415f * ls, 0.886f, -0.28f), Vec3(0.40f * ls, 0.89f, -0.47f), 0.032f, 10)
        }
        newBucket("cockpit_tft", "cockpit", PevPbrMaterialId.TFT_SCREEN, tftWhite, 0.12f, 0.10f, floatArrayOf(0.18f, 0.65f, 0.85f), isScreen = true) {
          addChamferedBox(0.465f * ls, 0.945f, 0f, 0.046f, 0.040f, 0.100f, 0.005f)
        }
      }

      Pev3dArchetype.EUC -> {
        addDetailedWheel("wheel_rear", 0f, -0.12f, 0f, 0.56f * ws, 0.25f, "motor", true)

        newBucket("battery", "battery", PevPbrMaterialId.BATTERY_PACK, battGreen, 0.45f, 0.34f, floatArrayOf(0.04f, 0.24f, 0.12f)) {
          addChamferedBox(0f, 0.08f, 0.24f, 0.33f, 0.28f, 0.085f, 0.024f, 2f)
          addChamferedBox(0f, 0.08f, -0.24f, 0.33f, 0.28f, 0.085f, 0.024f, 2f)
        }

        newBucket("frame", "frame", PevPbrMaterialId.CARBON_WEAVE, finish.bodyRgb, 0.24f, 0.26f, clearcoat = 0.90f, isShell = true) {
          addChamferedBox(0f, 0.09f, 0.30f, 0.36f, 0.30f, 0.045f, 0.022f, 2f)
          addChamferedBox(0f, 0.09f, -0.30f, 0.36f, 0.30f, 0.045f, 0.022f, 2f)
          // CNC Spiked Honeycomb Pedals
          addChamferedBox(0f, -0.30f, 0.36f, 0.23f, 0.024f, 0.125f, 0.01f)
          addChamferedBox(0f, -0.30f, -0.36f, 0.23f, 0.024f, 0.125f, 0.01f)
          // Contoured CNC Kinetic Power Pads (Jump + Brake Hooks)
          addChamferedBox(0.16f, 0.02f, 0.355f, 0.075f, 0.18f, 0.032f, 0.015f)
          addChamferedBox(-0.18f, 0.02f, 0.355f, 0.065f, 0.16f, 0.030f, 0.015f)
          addChamferedBox(0.16f, 0.02f, -0.355f, 0.075f, 0.18f, 0.032f, 0.015f)
          addChamferedBox(-0.18f, 0.02f, -0.355f, 0.065f, 0.16f, 0.030f, 0.015f)
          // Armored Front & Rear Roll-Cage Bumpers + Curved Fender + Rear Kickstand
          addSweptTube(
            listOf(Vec3(-0.36f, -0.14f, -0.28f), Vec3(-0.37f, 0.45f, -0.27f), Vec3(0.37f, 0.45f, -0.27f), Vec3(0.36f, -0.14f, -0.28f)),
            listOf(0.026f, 0.028f, 0.028f, 0.026f),
            12,
          )
          addSweptTube(
            listOf(Vec3(-0.36f, -0.14f, 0.28f), Vec3(-0.37f, 0.45f, 0.27f), Vec3(0.37f, 0.45f, 0.27f), Vec3(0.36f, -0.14f, 0.28f)),
            listOf(0.026f, 0.028f, 0.028f, 0.026f),
            12,
          )
          addSweptTube(
            listOf(Vec3(-0.32f, -0.18f, -0.14f), Vec3(-0.46f, -0.58f, -0.14f), Vec3(-0.46f, -0.58f, 0.14f), Vec3(-0.32f, -0.18f, 0.14f)),
            listOf(0.016f, 0.016f, 0.016f, 0.016f),
            8,
          )
          addCurvedFender(0f, -0.12f, 0.60f * ws, 130f, 225f, 0.15f)
        }
        // Deep Racing Livery Side Fairings on EUC Armored Shells
        newBucket("frame_graphics", "frame", PevPbrMaterialId.RACING_LIVERY, finish.accentRgb, 0.42f, 0.16f, clearcoat = 0.96f, isShell = true) {
          addChamferedBox(0f, 0.14f, 0.348f, 0.28f, 0.16f, 0.014f, 0.008f, 1.0f)
          addChamferedBox(0f, 0.14f, -0.348f, 0.28f, 0.16f, 0.014f, 0.008f, 1.0f)
        }

        // Exposed Copper Hollow-Bore Stator Collar
        newBucket("motor", "motor", PevPbrMaterialId.BRUSHED_METAL, motorCopper, 0.92f, 0.20f) {
          addDisc(0f, -0.12f, 0f, 0.23f * ws, 0.27f, 28)
        }

        newBucket("suspension", "suspension", PevPbrMaterialId.BRUSHED_METAL, finish.accentRgb, 0.92f, 0.18f, clearcoat = 0.5f) {
          addTube(Vec3(-0.22f, -0.18f, 0.26f), Vec3(-0.22f, 0.50f, 0.26f), 0.036f, 14)
          addTube(Vec3(-0.22f, -0.18f, -0.26f), Vec3(-0.22f, 0.50f, -0.26f), 0.036f, 14)
          addHelicalSpring(Vec3(-0.22f, -0.08f, 0.26f), Vec3(-0.22f, 0.34f, 0.26f), 0.048f, 0.009f, 6.5f)
          addHelicalSpring(Vec3(-0.22f, -0.08f, -0.26f), Vec3(-0.22f, 0.34f, -0.26f), 0.048f, 0.009f, 6.5f)
        }

        newBucket("controller", "controller", PevPbrMaterialId.BRUSHED_METAL, focCrimson, 0.88f, 0.22f, floatArrayOf(0.20f, 0.04f, 0.12f)) {
          addFinnedBlock(0f, 0.48f, 0f, 0.25f, 0.082f, 0.19f, 9)
        }

        newBucket("cockpit", "cockpit", PevPbrMaterialId.TFT_SCREEN, tftWhite, 0.14f, 0.12f, floatArrayOf(0.18f, 0.65f, 0.85f), isScreen = true) {
          addChamferedBox(0.05f, 0.60f, 0f, 0.19f, 0.042f, 0.125f, 0.012f)
        }
        // Retractable Anodized Top Trolley Handle
        newBucket("cockpit_trolley", "cockpit", PevPbrMaterialId.BRUSHED_METAL, finish.accentRgb, 0.90f, 0.20f) {
          addSweptTube(
            listOf(Vec3(-0.20f, 0.52f, -0.14f), Vec3(-0.20f, 0.72f, -0.14f), Vec3(-0.16f, 0.72f, 0.14f), Vec3(-0.20f, 0.52f, 0.14f)),
            listOf(0.016f, 0.018f, 0.018f, 0.016f),
            8,
          )
        }
      }

      Pev3dArchetype.HYDROFOIL -> {
        // Lofted Tapered Carbon eFoil Surf Hull
        newBucket("frame", "frame", PevPbrMaterialId.CARBON_WEAVE, finish.bodyRgb, 0.26f, 0.20f, clearcoat = 0.96f, isShell = true) {
          addSculptedDeckHull(0f, 0.14f, 0f, 1.10f, 0.072f, 0.39f, 18, 16)
        }
        // EVA Hydro Traction Pad & Dual Rider Foot Straps
        newBucket("frame_deckpad", "frame", PevPbrMaterialId.TIRE_RUBBER, darkRubber, 0.04f, 0.88f) {
          addChamferedBox(-0.08f, 0.21f, 0f, 0.66f, 0.014f, 0.28f, 0.008f, 2.5f)
          addSweptTube(
            listOf(Vec3(0.28f, 0.22f, -0.12f), Vec3(0.28f, 0.31f, -0.02f), Vec3(0.28f, 0.22f, 0.10f)),
            listOf(0.014f, 0.018f, 0.014f),
            8,
          )
          addSweptTube(
            listOf(Vec3(-0.34f, 0.22f, -0.11f), Vec3(-0.34f, 0.31f, 0f), Vec3(-0.34f, 0.22f, 0.11f)),
            listOf(0.014f, 0.018f, 0.014f),
            8,
          )
        }
        newBucket("battery", "battery", PevPbrMaterialId.BATTERY_PACK, battGreen, 0.45f, 0.32f, floatArrayOf(0.05f, 0.22f, 0.12f)) {
          addChamferedBox(-0.08f, 0.16f, 0f, 0.38f, 0.048f, 0.21f, 0.018f, 2f)
        }
        newBucket("controller", "controller", PevPbrMaterialId.BRUSHED_METAL, focCrimson, 0.90f, 0.20f, floatArrayOf(0.16f, 0.04f, 0.10f)) {
          addFinnedBlock(0.36f, 0.16f, 0f, 0.14f, 0.044f, 0.15f, 7)
        }
        // Carbon Hydrodynamic Foil Mast Strut
        newBucket("suspension", "suspension", PevPbrMaterialId.CARBON_WEAVE, finish.accentRgb, 0.48f, 0.18f, clearcoat = 0.90f) {
          addChamferedBox(-0.04f, -0.32f, 0f, 0.075f, 0.44f, 0.016f, 0.010f)
          addChamferedBox(-0.04f, 0.08f, 0f, 0.16f, 0.024f, 0.085f, 0.008f)
        }
        // Torpedo Pod + Ducted Shroud + 3-Blade Propeller (spins on wheel_rear node)
        newBucket("motor", "motor", PevPbrMaterialId.BRUSHED_METAL, motorCopper, 0.92f, 0.18f, floatArrayOf(0.18f, 0.08f, 0.02f)) {
          addSweptTube(
            listOf(Vec3(-0.56f, -0.76f, 0f), Vec3(-0.22f, -0.76f, 0f), Vec3(0.28f, -0.76f, 0f), Vec3(0.48f, -0.76f, 0f)),
            listOf(0.022f, 0.056f, 0.050f, 0.020f),
            16,
          )
          // Prop Duct Shroud Ring + 3 Blades
          val ductPts = (0..24).map { i ->
            val a = (i / 24f) * (2f * PI.toFloat())
            Vec3(-0.24f, -0.76f + 0.115f * sin(a), 0.115f * cos(a))
          }
          addSweptTube(ductPts, List(ductPts.size) { 0.012f }, 6, false)
          for (b in 0 until 3) {
            val a = (b / 3f) * (2f * PI.toFloat())
            addTube(
              Vec3(-0.24f, -0.76f, 0f),
              Vec3(-0.27f, -0.76f + 0.092f * sin(a), 0.092f * cos(a)),
              0.016f,
              6,
            )
          }
        }
        // Tapered Swept NACA-0012 Carbon Main Wing & Rear Stabilizer
        newBucket("brakes", "brakes", PevPbrMaterialId.CARBON_WEAVE, finish.caliperRgb, 0.40f, 0.20f, clearcoat = 0.88f) {
          addAirfoilWing(0.26f, -0.76f, 0f, chord = 0.24f, thickness = 0.18f, halfSpan = 0.56f, sweepBack = 0.11f)
          addAirfoilWing(-0.46f, -0.76f, 0f, chord = 0.14f, thickness = 0.14f, halfSpan = 0.28f, sweepBack = 0.06f)
        }
        newBucket("cockpit", "cockpit", PevPbrMaterialId.TFT_SCREEN, tftWhite, 0.20f, 0.14f, floatArrayOf(0.18f, 0.65f, 0.85f), isScreen = true) {
          addChamferedBox(0.75f, 0.28f, 0f, 0.07f, 0.032f, 0.045f, 0.008f)
        }
      }

      else -> {
        // EMOTO & EBIKE
        val isBike = archetype == Pev3dArchetype.EBIKE
        val wheelR = (if (isBike) 0.42f else 0.40f) * ws
        addDetailedWheel("wheel_rear", -0.92f * ls, -0.34f, 0f, wheelR, 0.22f, if (isBike) "motor" else "frame", isBike)
        addDetailedWheel("wheel_front", 0.94f * ls, -0.34f, 0f, wheelR, 0.20f, "brakes", false)

        // Sculpted Carbon/6061 Chassis Frame + Side Shrouds + Fenders
        newBucket(
          nodeName = "frame",
          partKey = "frame",
          texId = PevPbrMaterialId.CARBON_WEAVE,
          baseRgb = finish.bodyRgb,
          metallic = 0.28f,
          roughness = 0.26f,
          clearcoat = 0.92f,
          isShell = true,
        ) {
          // Top backbone curved twin spars
          addSweptTube(
            listOf(Vec3(-0.38f * ls, 0.25f, -0.055f), Vec3(0.05f * ls, 0.31f, -0.065f), Vec3(0.46f * ls, 0.36f, -0.045f)),
            listOf(0.036f, 0.044f, 0.042f),
            12,
          )
          addSweptTube(
            listOf(Vec3(-0.38f * ls, 0.25f, 0.055f), Vec3(0.05f * ls, 0.31f, 0.065f), Vec3(0.46f * ls, 0.36f, 0.045f)),
            listOf(0.036f, 0.044f, 0.042f),
            12,
          )
          // Lower cradle & headtube
          addSweptTube(
            listOf(Vec3(0.46f * ls, 0.42f, 0f), Vec3(0.32f * ls, 0.14f, 0f), Vec3(-0.18f * ls, -0.18f, 0f)),
            listOf(0.048f, 0.045f, 0.042f),
            14,
          )
          addTube(Vec3(-0.34f * ls, 0.25f, 0f), Vec3(-0.22f * ls, -0.16f, 0f), 0.040f, 12)
          // Forged Rear Swingarm & Axle Blocks
          addSweptTube(
            listOf(Vec3(-0.24f * ls, -0.15f, -0.11f), Vec3(-0.60f * ls, -0.26f, -0.12f), Vec3(-0.92f * ls, -0.34f, -0.115f)),
            listOf(0.038f, 0.034f, 0.030f),
            12,
          )
          addSweptTube(
            listOf(Vec3(-0.24f * ls, -0.15f, 0.11f), Vec3(-0.60f * ls, -0.26f, 0.12f), Vec3(-0.92f * ls, -0.34f, 0.115f)),
            listOf(0.038f, 0.034f, 0.030f),
            12,
          )
          if (isBike) {
            // EBIKE Crank Arms + CNC Platform Pedals
            addTube(Vec3(-0.24f * ls, -0.22f, -0.12f), Vec3(-0.24f * ls, -0.22f, 0.12f), 0.022f, 10)
            addTube(Vec3(-0.24f * ls, -0.22f, -0.12f), Vec3(-0.14f * ls, -0.38f, -0.15f), 0.016f, 8)
            addTube(Vec3(-0.24f * ls, -0.22f, 0.12f), Vec3(-0.34f * ls, -0.08f, 0.15f), 0.016f, 8)
            addChamferedBox(-0.14f * ls, -0.38f, -0.22f, 0.055f, 0.014f, 0.050f, 0.004f)
            addChamferedBox(-0.34f * ls, -0.08f, 0.22f, 0.055f, 0.014f, 0.050f, 0.004f)
            // Compact commuter fenders
            addCurvedFender(0.94f * ls, -0.34f, wheelR + 0.06f, 52f, 125f, 0.10f)
            addCurvedFender(-0.92f * ls, -0.34f, wheelR + 0.05f, 70f, 170f, 0.11f)
          } else {
            // EMOTO Dropped MX Footpegs + Front Number Plate + High MX Fender
            addTube(Vec3(-0.30f * ls, -0.26f, -0.12f), Vec3(-0.30f * ls, -0.26f, -0.28f), 0.018f, 10)
            addTube(Vec3(-0.30f * ls, -0.26f, 0.12f), Vec3(-0.30f * ls, -0.26f, 0.28f), 0.018f, 10)
            addChamferedBox(-0.30f * ls, -0.26f, -0.23f, 0.045f, 0.012f, 0.055f, 0.004f)
            addChamferedBox(-0.30f * ls, -0.26f, 0.23f, 0.045f, 0.012f, 0.055f, 0.004f)
            addChamferedBox(0.56f * ls, 0.50f, 0f, 0.020f, 0.115f, 0.098f, 0.008f)
            addCurvedFender(0.94f * ls, -0.34f, wheelR + 0.15f, 45f, 118f, 0.11f)
            addCurvedFender(-0.92f * ls, -0.34f, wheelR + 0.06f, 70f, 165f, 0.12f)
          }
          // Tank Shroud Side Plates + Billet Under-Chassis Bash Guard + Tail Tidy Bracket
          addChamferedBox(0.24f * ls, 0.28f, 0.145f, 0.18f, 0.09f, 0.022f, 0.014f)
          addChamferedBox(0.24f * ls, 0.28f, -0.145f, 0.18f, 0.09f, 0.022f, 0.014f)
          addChamferedBox(0.05f * ls, -0.22f, 0f, 0.26f, 0.018f, 0.135f, 0.008f)
          addTube(Vec3(-0.68f * ls, 0.25f, 0f), Vec3(-0.92f * ls, 0.14f, 0f), 0.016f, 8)
          addChamferedBox(-0.94f * ls, 0.10f, 0f, 0.008f, 0.055f, 0.075f, 0.004f)
        }

        // Deep High-Gloss Motorsport Racing Livery Graphics Shrouds & Number Panels
        newBucket("frame_graphics", "frame", PevPbrMaterialId.RACING_LIVERY, finish.accentRgb, 0.44f, 0.14f, clearcoat = 0.98f, isShell = true) {
          addChamferedBox(0.22f * ls, 0.26f, 0.172f, 0.21f, 0.115f, 0.012f, 0.008f, 1.0f)
          addChamferedBox(0.22f * ls, 0.26f, -0.172f, 0.21f, 0.115f, 0.012f, 0.008f, 1.0f)
          addChamferedBox(-0.52f * ls, 0.24f, 0.148f, 0.18f, 0.075f, 0.012f, 0.008f, 1.0f)
          addChamferedBox(-0.52f * ls, 0.24f, -0.148f, 0.18f, 0.075f, 0.012f, 0.008f, 1.0f)
          if (!isBike) {
            addChamferedBox(0.58f * ls, 0.50f, 0f, 0.010f, 0.110f, 0.094f, 0.005f, 1.0f)
          }
        }

        // Quilted Grippy Moto Saddle (separate PBR material!)
        newBucket("frame_seat", "frame", PevPbrMaterialId.QUILTED_LEATHER, saddleCharcoal, 0.04f, 0.72f, isShell = true) {
          addChamferedBox(-0.38f * ls, 0.34f, 0f, 0.31f, 0.064f, 0.132f, 0.032f, 2f)
          addChamferedBox(-0.14f * ls, 0.31f, 0f, 0.14f, 0.048f, 0.115f, 0.024f, 1.5f)
        }

        // USD Dual-Crown Forks + Stanchion Guards + Rear Coiled Nitrogen Mono-Shock + Linkage
        newBucket("suspension", "suspension", PevPbrMaterialId.BRUSHED_METAL, finish.accentRgb, 0.94f, 0.16f, clearcoat = 0.5f) {
          // Upper Fork Anodized Tubes + Chrome Lower Stanchions
          addTube(Vec3(0.47f * ls, 0.59f, -0.118f), Vec3(0.70f * ls, 0.14f, -0.118f), 0.040f, 14)
          addTube(Vec3(0.47f * ls, 0.59f, 0.118f), Vec3(0.70f * ls, 0.14f, 0.118f), 0.040f, 14)
          addTube(Vec3(0.70f * ls, 0.14f, -0.118f), Vec3(0.94f * ls, -0.34f, -0.118f), 0.029f, 14)
          addTube(Vec3(0.70f * ls, 0.14f, 0.118f), Vec3(0.94f * ls, -0.34f, 0.118f), 0.029f, 14)
          // Molded Lower Fork Stanchion Guards
          addChamferedBox(0.84f * ls, -0.12f, -0.148f, 0.045f, 0.14f, 0.016f, 0.006f)
          addChamferedBox(0.84f * ls, -0.12f, 0.148f, 0.045f, 0.14f, 0.016f, 0.006f)
          // CNC Triple Clamps
          addChamferedBox(0.50f * ls, 0.52f, 0f, 0.055f, 0.022f, 0.155f, 0.008f)
          addChamferedBox(0.57f * ls, 0.37f, 0f, 0.055f, 0.022f, 0.155f, 0.008f)
          // Rear Mono-shock Damper + Real 3D Helical Coil Spring
          addTube(Vec3(-0.26f * ls, 0.24f, 0f), Vec3(-0.58f * ls, -0.22f, 0f), 0.026f, 12)
          addHelicalSpring(Vec3(-0.30f * ls, 0.18f, 0f), Vec3(-0.54f * ls, -0.16f, 0f), 0.056f, 0.011f, 6.5f)
        }

        // High-Voltage Battery Pack
        newBucket("battery", "battery", PevPbrMaterialId.BATTERY_PACK, battGreen, 0.45f, 0.32f, floatArrayOf(0.04f, 0.24f, 0.10f)) {
          addChamferedBox(0.08f * ls, 0.13f, 0f, 0.24f, 0.21f, 0.142f, 0.022f, 2f)
        }

        // Mid/Hub Drive Motor + CNC Sprocket & Drive Chain + High-V Phase Leads
        newBucket("motor", "motor", PevPbrMaterialId.BRUSHED_METAL, motorCopper, 0.90f, 0.22f, floatArrayOf(0.14f, 0.06f, 0.02f)) {
          addTube(Vec3(-0.14f * ls, -0.22f, -0.145f), Vec3(-0.14f * ls, -0.22f, 0.145f), 0.132f, 20)
          addFinnedBlock(-0.14f * ls, -0.22f, 0f, 0.11f, 0.12f, 0.13f, 6)
          // Orange Armored HV Phase Cable
          addSweptTube(
            listOf(Vec3(0.28f * ls, -0.02f, 0.06f), Vec3(0.12f * ls, -0.14f, 0.11f), Vec3(-0.10f * ls, -0.20f, 0.12f)),
            listOf(0.016f, 0.016f, 0.016f),
            8,
          )
          // Front Pinion + Rear CNC 54T Sprocket + Drive Chain
          val sprocketZ = -0.142f
          addSprocketWithTeeth(-0.14f * ls, -0.22f, sprocketZ, 0.075f, 0.014f, 14)
          addSprocketWithTeeth(-0.92f * ls, -0.34f, sprocketZ, 0.165f * ws, 0.014f, 24)
          addChainDriveLoop(
            frontAxle = Vec3(-0.14f * ls, -0.22f, sprocketZ),
            rearAxle = Vec3(-0.92f * ls, -0.34f, sprocketZ),
            rFront = 0.075f,
            rRear = 0.165f * ws,
            zOffset = sprocketZ,
          )
        }

        // Braided Hydraulic Brake Hoses (Front + Rear Calipers)
        newBucket("brakes", "brakes", PevPbrMaterialId.CARBON_WEAVE, finish.caliperRgb, 0.35f, 0.36f) {
          addSweptTube(
            listOf(Vec3(0.50f * ls, 0.64f, 0.25f), Vec3(0.62f * ls, 0.32f, 0.16f), Vec3(0.76f * ls, 0.02f, 0.14f), Vec3(0.98f * ls, -0.28f, 0.14f)),
            listOf(0.008f, 0.008f, 0.008f, 0.008f),
            6,
          )
          addSweptTube(
            listOf(Vec3(0.50f * ls, 0.64f, -0.25f), Vec3(0.20f * ls, 0.28f, -0.09f), Vec3(-0.34f * ls, -0.16f, 0.12f), Vec3(-0.82f * ls, -0.26f, 0.14f)),
            listOf(0.008f, 0.008f, 0.008f, 0.008f),
            6,
          )
        }

        // Finned Extruded Aluminum FOC Controller
        newBucket("controller", "controller", PevPbrMaterialId.BRUSHED_METAL, focCrimson, 0.88f, 0.22f, floatArrayOf(0.20f, 0.04f, 0.10f), clearcoat = 0.4f) {
          addFinnedBlock(0.34f * ls, -0.02f, 0f, 0.105f, 0.125f, 0.115f, 7)
        }

        // Tapered MX Bars + Levers + Master Cylinders + Grips + Bezel
        newBucket("cockpit", "cockpit", PevPbrMaterialId.BRUSHED_METAL, darkAnodized, 0.85f, 0.28f) {
          addSweptTube(
            listOf(Vec3(0.46f * ls, 0.66f, -0.45f), Vec3(0.51f * ls, 0.65f, -0.18f), Vec3(0.50f * ls, 0.63f, 0f), Vec3(0.51f * ls, 0.65f, 0.18f), Vec3(0.46f * ls, 0.66f, 0.45f)),
            listOf(0.026f, 0.022f, 0.026f, 0.022f, 0.026f),
            12,
          )
          addTube(Vec3(0.51f * ls, 0.65f, -0.26f), Vec3(0.58f * ls, 0.64f, -0.39f), 0.009f, 6)
          addTube(Vec3(0.51f * ls, 0.65f, 0.26f), Vec3(0.58f * ls, 0.64f, 0.39f), 0.009f, 6)
          addChamferedBox(0.51f * ls, 0.67f, -0.25f, 0.022f, 0.016f, 0.026f, 0.004f)
          addChamferedBox(0.51f * ls, 0.67f, 0.25f, 0.022f, 0.016f, 0.026f, 0.004f)
          addChamferedBox(0.54f * ls, 0.68f, 0f, 0.062f, 0.045f, 0.102f, 0.01f)
          // Stealth CNC Bar-End Rearview Mirrors
          addTube(Vec3(0.46f * ls, 0.66f, 0.44f), Vec3(0.44f * ls, 0.77f, 0.48f), 0.010f, 6)
          addTube(Vec3(0.46f * ls, 0.66f, -0.44f), Vec3(0.44f * ls, 0.77f, -0.48f), 0.010f, 6)
          addChamferedBox(0.42f * ls, 0.79f, 0.50f, 0.014f, 0.036f, 0.058f, 0.008f)
          addChamferedBox(0.42f * ls, 0.79f, -0.50f, 0.014f, 0.036f, 0.058f, 0.008f)
        }
        newBucket("cockpit_grips", "cockpit", PevPbrMaterialId.TIRE_RUBBER, darkRubber, 0.05f, 0.82f) {
          addTube(Vec3(0.495f * ls, 0.655f, 0.27f), Vec3(0.46f * ls, 0.66f, 0.46f), 0.032f, 10)
          addTube(Vec3(0.495f * ls, 0.655f, -0.27f), Vec3(0.46f * ls, 0.66f, -0.46f), 0.032f, 10)
        }
        newBucket("cockpit_tft", "cockpit", PevPbrMaterialId.TFT_SCREEN, tftWhite, 0.12f, 0.10f, floatArrayOf(0.18f, 0.65f, 0.85f), isScreen = true) {
          addChamferedBox(0.555f * ls, 0.685f, 0f, 0.050f, 0.038f, 0.090f, 0.004f)
        }
      }
    }

    // Dual-Projector Halo LED Headlight Pod, Crimson Blade Taillight & 4-Corner Sequential Amber Turn Signals
    val hlX = when (archetype) {
      Pev3dArchetype.EUC -> 0.36f
      Pev3dArchetype.HYDROFOIL -> 1.05f
      else -> 0.68f * ls
    }
    val hlY = when (archetype) {
      Pev3dArchetype.EUC -> 0.27f
      Pev3dArchetype.HYDROFOIL -> 0.19f
      Pev3dArchetype.SCOOTER -> 0.66f
      else -> 0.46f
    }
    val tailX = when (archetype) {
      Pev3dArchetype.EUC -> -0.37f
      Pev3dArchetype.HYDROFOIL -> -1.04f
      else -> -0.78f * ls
    }
    val tailY = when (archetype) {
      Pev3dArchetype.EUC -> 0.25f
      Pev3dArchetype.HYDROFOIL -> 0.16f
      Pev3dArchetype.SCOOTER -> -0.17f
      else -> 0.25f
    }

    // Machined Headlight Housing Bezel + Glowing Dual-Projector Optics Lens
    newBucket("cockpit_bezel", "cockpit", PevPbrMaterialId.BRUSHED_METAL, darkAnodized, 0.92f, 0.18f, clearcoat = 0.5f) {
      addChamferedBox(hlX - 0.022f, hlY, 0f, 0.042f, 0.068f, 0.118f, 0.014f)
      addChamferedBox(tailX + 0.016f, tailY, 0f, 0.028f, 0.042f, 0.110f, 0.010f)
    }
    newBucket(
      nodeName = "cockpit_light",
      partKey = "cockpit",
      texId = PevPbrMaterialId.HEADLIGHT_OPTIC,
      baseRgb = floatArrayOf(1.0f, 1.0f, 1.0f),
      metallic = 0.15f,
      roughness = 0.08f,
      emissiveRgb = if (lightsOn) floatArrayOf(2.4f, 2.5f, 2.7f) else floatArrayOf(0.05f, 0.05f, 0.06f),
      clearcoat = 1.0f,
      useEmissiveTexture = true,
    ) {
      // Forward (+X) facing Dual-Projector LED Lens Quad
      val fx = hlX + 0.022f
      addQuad(
        Vec3(fx, hlY - 0.054f, 0.104f),
        Vec3(fx, hlY - 0.054f, -0.104f),
        Vec3(fx, hlY + 0.054f, -0.104f),
        Vec3(fx, hlY + 0.054f, 0.104f),
        Vec3(1f, 0f, 0f),
        1f,
        1f,
        wire = false,
      )
      addChamferedBox(hlX, hlY, 0f, 0.024f, 0.056f, 0.106f, 0.008f)
    }

    // Crimson Sequential LED Taillight Blade
    newBucket(
      nodeName = "frame_tail",
      partKey = "frame",
      texId = PevPbrMaterialId.TAILLIGHT_OPTIC,
      baseRgb = floatArrayOf(1.0f, 0.92f, 0.92f),
      metallic = 0.15f,
      roughness = 0.08f,
      emissiveRgb = if (lightsOn) floatArrayOf(2.2f, 0.25f, 0.22f) else floatArrayOf(0.06f, 0.01f, 0.01f),
      clearcoat = 1.0f,
      useEmissiveTexture = true,
    ) {
      val rx = tailX - 0.018f
      addQuad(
        Vec3(rx, tailY - 0.032f, -0.098f),
        Vec3(rx, tailY - 0.032f, 0.098f),
        Vec3(rx, tailY + 0.032f, 0.098f),
        Vec3(rx, tailY + 0.032f, -0.098f),
        Vec3(-1f, 0f, 0f),
        1f,
        1f,
        wire = false,
      )
      addChamferedBox(tailX, tailY, 0f, 0.018f, 0.034f, 0.098f, 0.006f)
    }

    // 4-Corner CNC Stalk-Mounted Sequential Amber Turn Signal Pods (animated by PevVulkanHost!)
    val fSigX = hlX - 0.03f
    val fSigY = if (archetype == Pev3dArchetype.SCOOTER) hlY + 0.14f else hlY + 0.02f
    val rSigX = tailX + 0.02f
    val rSigY = tailY - 0.01f
    val sigZSpan = if (archetype == Pev3dArchetype.EUC) 0.34f else 0.18f

    fun addTurnSignalPod(nodeName: String, partKey: String, sx: Float, sy: Float, zBase: Float, zTip: Float) {
      // Anodized Stalk
      newBucket("${nodeName}_stalk", partKey, PevPbrMaterialId.BRUSHED_METAL, darkAnodized, 0.90f, 0.22f) {
        addTube(Vec3(sx, sy, zBase), Vec3(sx, sy, zTip), 0.012f, 8)
      }
      // Amber LED Lens Blade
      newBucket(
        nodeName = nodeName,
        partKey = partKey,
        texId = PevPbrMaterialId.AMBER_SIGNAL_OPTIC,
        baseRgb = floatArrayOf(1.0f, 0.72f, 0.16f),
        metallic = 0.12f,
        roughness = 0.08f,
        emissiveRgb = floatArrayOf(0.08f, 0.04f, 0.01f),
        clearcoat = 1.0f,
        useEmissiveTexture = true,
      ) {
        addChamferedBox(sx, sy, zTip, 0.026f, 0.018f, 0.038f, 0.006f)
      }
    }

    addTurnSignalPod("signal_left_front", "cockpit", fSigX, fSigY, -0.10f, -sigZSpan - 0.06f)
    addTurnSignalPod("signal_right_front", "cockpit", fSigX, fSigY, 0.10f, sigZSpan + 0.06f)
    addTurnSignalPod("signal_left_rear", "frame", rSigX, rSigY, -0.10f, -sigZSpan - 0.05f)
    addTurnSignalPod("signal_right_rear", "frame", rSigX, rSigY, 0.10f, sigZSpan + 0.05f)

    return packBucketsToGlb(buckets, wheelAxleCenters, explodeDirs, xrayShell)
  }

  private fun packBucketsToGlb(
    buckets: List<MeshBucket>,
    wheelAxleCenters: Map<String, Vec3>,
    explodeDirs: Map<String, Vec3>,
    xrayShell: Boolean,
  ): PevBuiltGlb {
    val binStream = ByteArrayOutputStream(256 * 1024)
    val bufferViews = JSONArray()
    val accessors = JSONArray()
    val images = JSONArray()
    val textures = JSONArray()
    val materials = JSONArray()
    val meshes = JSONArray()
    val nodes = JSONArray()
    val rootNodeIndices = JSONArray()
    val wireAll = ArrayList<Float>(8192)

    fun padBinTo4() {
      while (binStream.size() % 4 != 0) binStream.write(0)
    }

    fun appendBytesView(bytes: ByteArray, target: Int? = null): Int {
      padBinTo4()
      val offset = binStream.size()
      binStream.write(bytes)
      val bv = JSONObject()
        .put("buffer", 0)
        .put("byteOffset", offset)
        .put("byteLength", bytes.size)
      if (target != null) bv.put("target", target)
      val idx = bufferViews.length()
      bufferViews.put(bv)
      return idx
    }

    fun appendFloatArrayView(list: List<Float>, target: Int): Int {
      val bb = ByteBuffer.allocate(list.size * 4).order(ByteOrder.LITTLE_ENDIAN)
      for (f in list) bb.putFloat(f)
      return appendBytesView(bb.array(), target)
    }

    fun appendIntArrayView(list: List<Int>, target: Int): Int {
      val bb = ByteBuffer.allocate(list.size * 4).order(ByteOrder.LITTLE_ENDIAN)
      for (v in list) bb.putInt(v)
      return appendBytesView(bb.array(), target)
    }

    // Embed shared PBR texture sets once per PevPbrMaterialId used (albedo, normal, metallicRoughness, occlusion)
    data class PbrTexSet(val albedo: Int, val normal: Int, val mr: Int, val occlusion: Int)
    val usedTexIds = buckets.map { it.matKey.texId }.distinct()
    val texIndexMap = HashMap<PevPbrMaterialId, PbrTexSet>()
    val samplerJson = JSONArray().put(
      JSONObject()
        .put("magFilter", 9729)
        .put("minFilter", 9987)
        .put("wrapS", 10497)
        .put("wrapT", 10497),
    )

    for (tid in usedTexIds) {
      val bundle = Pev3dPbrTextures.getBundle(tid)
      fun addPngTex(png: ByteArray): Int {
        val bvIdx = appendBytesView(png)
        val imgIdx = images.length()
        images.put(JSONObject().put("bufferView", bvIdx).put("mimeType", "image/png"))
        val texIdx = textures.length()
        textures.put(JSONObject().put("sampler", 0).put("source", imgIdx))
        return texIdx
      }
      texIndexMap[tid] = PbrTexSet(
        albedo = addPngTex(bundle.albedoPng),
        normal = addPngTex(bundle.normalPng),
        mr = addPngTex(bundle.metallicRoughnessPng),
        occlusion = addPngTex(bundle.occlusionPng),
      )
    }

    buckets.forEachIndexed { bIdx, b ->
      wireAll.addAll(b.wirePositions)

      val tangents = b.computeTangentsVec4()
      val posBv = appendFloatArrayView(b.positions, 34962)
      val normBv = appendFloatArrayView(b.normals, 34962)
      val tanBv = appendFloatArrayView(tangents, 34962)
      val uvBv = appendFloatArrayView(b.uvs, 34962)
      val idxBv = appendIntArrayView(b.indices, 34963)

      val vCount = b.positions.size / 3
      val posAcc = accessors.length().also {
        accessors.put(
          JSONObject()
            .put("bufferView", posBv)
            .put("componentType", 5126)
            .put("count", vCount)
            .put("type", "VEC3")
            .put("min", JSONArray().put(b.minX.toDouble()).put(b.minY.toDouble()).put(b.minZ.toDouble()))
            .put("max", JSONArray().put(b.maxX.toDouble()).put(b.maxY.toDouble()).put(b.maxZ.toDouble())),
        )
      }
      val normAcc = accessors.length().also {
        accessors.put(JSONObject().put("bufferView", normBv).put("componentType", 5126).put("count", vCount).put("type", "VEC3"))
      }
      val tanAcc = accessors.length().also {
        accessors.put(JSONObject().put("bufferView", tanBv).put("componentType", 5126).put("count", vCount).put("type", "VEC4"))
      }
      val uvAcc = accessors.length().also {
        accessors.put(JSONObject().put("bufferView", uvBv).put("componentType", 5126).put("count", vCount).put("type", "VEC2"))
      }
      val idxAcc = accessors.length().also {
        accessors.put(JSONObject().put("bufferView", idxBv).put("componentType", 5125).put("count", b.indices.size).put("type", "SCALAR"))
      }

      val texSet = texIndexMap.getValue(b.matKey.texId)
      val mk = b.matKey
      val isXrayTrans = xrayShell && mk.isShell
      val alpha = if (isXrayTrans) 0.24 else 1.0
      val pbr = JSONObject()
        .put(
          "baseColorFactor",
          JSONArray()
            .put(if (isXrayTrans) 0.18 else mk.baseRgb[0].toDouble())
            .put(if (isXrayTrans) 0.78 else mk.baseRgb[1].toDouble())
            .put(if (isXrayTrans) 1.0 else mk.baseRgb[2].toDouble())
            .put(alpha),
        )
        .put("baseColorTexture", JSONObject().put("index", texSet.albedo))
        .put("metallicFactor", if (isXrayTrans) 0.15 else mk.metallic.toDouble())
        .put("roughnessFactor", if (isXrayTrans) 0.18 else mk.roughness.toDouble())
        .put("metallicRoughnessTexture", JSONObject().put("index", texSet.mr))

      val mat = JSONObject()
        .put("name", "mat_${mk.partKey}_$bIdx")
        .put("pbrMetallicRoughness", pbr)
        .put("normalTexture", JSONObject().put("index", texSet.normal).put("scale", 1.22))
        .put("occlusionTexture", JSONObject().put("index", texSet.occlusion).put("strength", 0.85))
        .put("doubleSided", isXrayTrans || mk.partKey == "frame")

      if (isXrayTrans) {
        mat.put("alphaMode", "BLEND")
        mat.put("emissiveFactor", JSONArray().put(0.08).put(0.38).put(0.55))
      } else if (mk.isScreen || mk.useEmissiveTexture) {
        mat.put("emissiveTexture", JSONObject().put("index", texSet.albedo))
        mat.put(
          "emissiveFactor",
          JSONArray()
            .put(mk.emissiveRgb[0].coerceAtLeast(if (mk.isScreen) 1.1f else 0.04f).toDouble())
            .put(mk.emissiveRgb[1].coerceAtLeast(if (mk.isScreen) 1.1f else 0.02f).toDouble())
            .put(mk.emissiveRgb[2].coerceAtLeast(if (mk.isScreen) 1.1f else 0.01f).toDouble()),
        )
      } else if (mk.emissiveRgb[0] > 0.001f || mk.emissiveRgb[1] > 0.001f || mk.emissiveRgb[2] > 0.001f) {
        mat.put(
          "emissiveFactor",
          JSONArray()
            .put(mk.emissiveRgb[0].toDouble())
            .put(mk.emissiveRgb[1].toDouble())
            .put(mk.emissiveRgb[2].toDouble()),
        )
      }

      if (mk.clearcoat > 0.05f && !isXrayTrans) {
        mat.put(
          "extensions",
          JSONObject().put(
            "KHR_materials_clearcoat",
            JSONObject()
              .put("clearcoatFactor", mk.clearcoat.toDouble())
              .put("clearcoatRoughnessFactor", 0.08),
          ),
        )
      }

      val matIdx = materials.length()
      materials.put(mat)

      val prim = JSONObject()
        .put(
          "attributes",
          JSONObject()
            .put("POSITION", posAcc)
            .put("NORMAL", normAcc)
            .put("TANGENT", tanAcc)
            .put("TEXCOORD_0", uvAcc),
        )
        .put("indices", idxAcc)
        .put("material", matIdx)
      val meshIdx = meshes.length()
      meshes.put(JSONObject().put("name", "mesh_${b.nodeName}_$bIdx").put("primitives", JSONArray().put(prim)))

      val nodeIdx = nodes.length()
      nodes.put(JSONObject().put("name", "${b.nodeName}__part_${mk.partKey}").put("mesh", meshIdx))
      rootNodeIndices.put(nodeIdx)
    }

    padBinTo4()
    val binBytes = binStream.toByteArray()

    val root = JSONObject()
      .put("asset", JSONObject().put("version", "2.0").put("generator", "RuralERides-VulkanPBR-GLB"))
      .put("extensionsUsed", JSONArray().put("KHR_materials_clearcoat"))
      .put("scene", 0)
      .put("scenes", JSONArray().put(JSONObject().put("nodes", rootNodeIndices)))
      .put("nodes", nodes)
      .put("meshes", meshes)
      .put("materials", materials)
      .put("textures", textures)
      .put("images", images)
      .put("samplers", samplerJson)
      .put("accessors", accessors)
      .put("bufferViews", bufferViews)
      .put("buffers", JSONArray().put(JSONObject().put("byteLength", binBytes.size)))

    val rawJson = root.toString().toByteArray(Charsets.UTF_8)
    val jsonPad = (4 - (rawJson.size % 4)) % 4
    val jsonLen = rawJson.size + jsonPad
    val totalLen = 12 + 8 + jsonLen + 8 + binBytes.size

    val glb = ByteBuffer.allocate(totalLen).order(ByteOrder.LITTLE_ENDIAN)
    glb.putInt(0x46546C67) // 'glTF'
    glb.putInt(2)
    glb.putInt(totalLen)

    glb.putInt(jsonLen)
    glb.putInt(0x4E4F534A) // 'JSON'
    glb.put(rawJson)
    repeat(jsonPad) { glb.put(0x20.toByte()) }

    glb.putInt(binBytes.size)
    glb.putInt(0x004E4942) // 'BIN\0'
    glb.put(binBytes)

    val wireGroups = buckets.mapNotNull { b ->
      if (b.wirePositions.isEmpty()) null
      else {
        PevWireGroup(
          partKey = b.matKey.partKey,
          axleCenter = wheelAxleCenters[b.nodeName],
          explodeDir = explodeDirs[b.matKey.partKey] ?: explodeDirs[b.nodeName] ?: Vec3(0f, 0f, 0f),
          lines = b.wirePositions.toFloatArray(),
        )
      }
    }

    return PevBuiltGlb(
      glbBytes = glb.array(),
      wheelAxleCenters = wheelAxleCenters,
      nodeExplodeDirs = explodeDirs,
      wireframeLines = wireAll.toFloatArray(),
      wireGroups = wireGroups,
    )
  }
}
