package com.example.ui.screens

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
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

internal class PevSceneMeshPart(
  val nodeName: String,
  val partKey: String,
  val texId: PevPbrMaterialId,
  val baseRgb: FloatArray,
  val metallic: Float,
  val roughness: Float,
  val emissiveRgb: FloatArray,
  val clearcoat: Float,
  val isShell: Boolean,
  val isScreen: Boolean,
  val axleCenter: Vec3?,
  val explodeDir: Vec3,
  val vertices: FloatArray, // interleaved: px,py,pz, nx,ny,nz, u,v (stride = 8 floats)
  val indices: ShortArray,
  val wireframeLines: FloatArray,
)

internal class PevBuiltScene(
  val parts: List<PevSceneMeshPart>,
  val wireGroups: List<PevWireGroup>,
)

/**
 * Parametric cross-section station for smooth lofted automotive/moto organic bodywork.
 * [exp] controls cross-section curvature: 1.0 = smooth ellipse/oval, 0.55 = sculpted chamfered automotive fairing.
 */
private data class LoftStation(
  val x: Float,
  val cy: Float,
  val cz: Float,
  val ry: Float,
  val rz: Float,
  val exp: Float = 0.78f,
  val topFlat: Float = 1.0f,
  val botFlat: Float = 1.0f,
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

  private fun cross(a: Vec3, b: Vec3): Vec3 =
    Vec3(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x)

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

  /**
   * Lofts a smooth, organic closed 3D body along X through arbitrary superellipse cross-sections.
   * Computes true smooth surface normals from longitudinal and circumferential surface derivatives
   * so curved tanks, saddles, EUC shells, fairings, and decks reflect studio light like real molded bodywork.
   */
  fun addLoftedBodyX(
    stations: List<LoftStation>,
    ringSegs: Int = 20,
    uTile: Float = 1.5f,
    vTile: Float = 1.5f,
    capStart: Boolean = true,
    capEnd: Boolean = true,
  ) {
    if (stations.size < 2) return
    val nS = stations.size
    val pts = Array(nS) { Array(ringSegs) { Vec3(0f, 0f, 0f) } }

    for (i in 0 until nS) {
      val st = stations[i]
      val e = st.exp.coerceIn(0.28f, 1.35f)
      for (j in 0 until ringSegs) {
        val ang = (j.toFloat() / ringSegs) * (2f * PI.toFloat())
        val ca = cos(ang)
        val sa = sin(ang)
        val sc = sign(ca) * abs(ca).pow(e)
        val ss = sign(sa) * abs(sa).pow(e)
        val yScale = if (ss >= 0f) st.topFlat else st.botFlat
        pts[i][j] = Vec3(st.x, st.cy + st.ry * ss * yScale, st.cz + st.rz * sc)
      }
    }

    val grid = Array(nS) { IntArray(ringSegs) }
    for (i in 0 until nS) {
      val iPrev = (i - 1).coerceAtLeast(0)
      val iNext = (i + 1).coerceAtMost(nS - 1)
      val uCoord = (i.toFloat() / (nS - 1)) * uTile
      for (j in 0 until ringSegs) {
        val jPrev = (j - 1 + ringSegs) % ringSegs
        val jNext = (j + 1) % ringSegs
        val tU = pts[iNext][j] - pts[iPrev][j]
        val tV = pts[i][jNext] - pts[i][jPrev]
        var n = cross(tV, tU)
        val nLen = sqrt(n.x * n.x + n.y * n.y + n.z * n.z)
        if (nLen < 1e-5f) {
          val ang = (j.toFloat() / ringSegs) * (2f * PI.toFloat())
          n = Vec3(0f, sin(ang), cos(ang))
        }
        val vCoord = (j.toFloat() / ringSegs) * vTile
        grid[i][j] = addVert(pts[i][j], n, uCoord, vCoord)
      }
    }

    for (i in 0 until nS - 1) {
      for (j in 0 until ringSegs) {
        val nj = (j + 1) % ringSegs
        val a = grid[i][j]; val b = grid[i + 1][j]; val c = grid[i + 1][nj]; val d = grid[i][nj]
        indices += a; indices += b; indices += c
        indices += a; indices += c; indices += d
        if (j % 3 == 0) {
          addWireEdge(pts[i][j], pts[i + 1][j])
        }
        if (i % 2 == 0 && j % 2 == 0) {
          addWireEdge(pts[i][j], pts[i][nj])
        }
      }
    }

    if (capStart) {
      val s0 = stations.first()
      val dir = if (stations.last().x >= s0.x) Vec3(-1f, 0f, 0f) else Vec3(1f, 0f, 0f)
      val cIdx = addVert(Vec3(s0.x, s0.cy, s0.cz), dir, 0.5f, 0.5f)
      val capRing = IntArray(ringSegs) { j ->
        val ang = (j.toFloat() / ringSegs) * (2f * PI.toFloat())
        addVert(pts[0][j], dir, 0.5f + 0.48f * cos(ang), 0.5f + 0.48f * sin(ang))
      }
      for (j in 0 until ringSegs) {
        val nj = (j + 1) % ringSegs
        indices += cIdx; indices += capRing[nj]; indices += capRing[j]
      }
    }
    if (capEnd) {
      val sL = stations.last()
      val dir = if (sL.x >= stations.first().x) Vec3(1f, 0f, 0f) else Vec3(-1f, 0f, 0f)
      val cIdx = addVert(Vec3(sL.x, sL.cy, sL.cz), dir, 0.5f, 0.5f)
      val capRing = IntArray(ringSegs) { j ->
        val ang = (j.toFloat() / ringSegs) * (2f * PI.toFloat())
        addVert(pts[nS - 1][j], dir, 0.5f + 0.48f * cos(ang), 0.5f + 0.48f * sin(ang))
      }
      for (j in 0 until ringSegs) {
        val nj = (j + 1) % ringSegs
        indices += cIdx; indices += capRing[j]; indices += capRing[nj]
      }
    }
  }

  /**
   * Sculpted ellipsoid/teardrop pod (for aerodynamic turn signals, mirrors, headlight pods, calipers).
   */
  fun addSculptedPod(
    cx: Float,
    cy: Float,
    cz: Float,
    rx: Float,
    ry: Float,
    rz: Float,
    exp: Float = 0.85f,
    rings: Int = 10,
    segs: Int = 16,
  ) {
    val stations = ArrayList<LoftStation>(rings + 1)
    for (i in 0..rings) {
      val t = i / rings.toFloat() // 0..1
      val phi = (t - 0.5f) * PI.toFloat() // -pi/2..+pi/2
      val x = cx + rx * sin(phi)
      val rFactor = cos(phi).coerceAtLeast(0.06f)
      stations += LoftStation(x = x, cy = cy, cz = cz, ry = ry * rFactor, rz = rz * rFactor, exp = exp)
    }
    addLoftedBodyX(stations, ringSegs = segs, uTile = 1f, vTile = 1f, capStart = true, capEnd = true)
  }

  /**
   * Smooth rounded-edge block built as a superellipse loft (never sharp Minecraft cubes!).
   */
  fun addRoundedBlock(
    cx: Float,
    cy: Float,
    cz: Float,
    hx: Float,
    hy: Float,
    hz: Float,
    uvScale: Float = 1.2f,
    exp: Float = 0.48f,
  ) {
    val bevelX = minOf(hx * 0.22f, hy * 0.35f, hz * 0.35f, 0.025f)
    val stations = listOf(
      LoftStation(cx - hx, cy, cz, hy * 0.72f, hz * 0.72f, exp + 0.14f),
      LoftStation(cx - hx + bevelX, cy, cz, hy, hz, exp),
      LoftStation(cx + hx - bevelX, cy, cz, hy, hz, exp),
      LoftStation(cx + hx, cy, cz, hy * 0.72f, hz * 0.72f, exp + 0.14f),
    )
    addLoftedBodyX(stations, ringSegs = 16, uTile = uvScale, vTile = uvScale)
  }

  /**
   * Finned CNC billet aluminum heatsink housing (for FOC inverter & motor stator cases).
   */
  fun addFinnedBlock(cx: Float, cy: Float, cz: Float, hx: Float, hy: Float, hz: Float, finCount: Int = 8) {
    addRoundedBlock(cx, cy, cz, hx * 0.94f, hy * 0.84f, hz * 0.84f, uvScale = 1.4f, exp = 0.42f)
    val step = (hx * 1.66f) / (finCount - 1).coerceAtLeast(1)
    for (i in 0 until finCount) {
      val fx = cx - hx * 0.83f + i * step
      addRoundedBlock(fx, cy, cz, 0.0055f, hy, hz, uvScale = 0.5f, exp = 0.45f)
    }
  }

  fun addSweptTube(points: List<Vec3>, radii: List<Float>, segs: Int = 16, capEnds: Boolean = true) {
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
          normVec(cross(dir, ref))
        } else {
          val dot = ref.x * dir.x + ref.y * dir.y + ref.z * dir.z
          normVec(Vec3(ref.x - dir.x * dot, ref.y - dir.y * dot, ref.z - dir.z * dot))
        }
      prevSide = side
      val binorm = normVec(cross(dir, side))
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
        if (i % 3 == 0) {
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

  fun addTube(a: Vec3, b: Vec3, radius: Float, segs: Int = 16) {
    addSweptTube(listOf(a, b), listOf(radius, radius), segs, capEnds = true)
  }

  fun addHelicalSpring(a: Vec3, b: Vec3, coilRadius: Float, wireRadius: Float, turns: Float = 6.5f) {
    val axis = b - a
    val len = sqrt(axis.x * axis.x + axis.y * axis.y + axis.z * axis.z).coerceAtLeast(1e-4f)
    val dir = Vec3(axis.x / len, axis.y / len, axis.z / len)
    val up = if (abs(dir.y) < 0.9f) Vec3(0f, 1f, 0f) else Vec3(1f, 0f, 0f)
    val side = normVec(cross(dir, up))
    val binorm = normVec(cross(dir, side))
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
    addSweptTube(pts, rads, segs = 8, capEnds = true)
  }

  fun addDisc(cx: Float, cy: Float, cz: Float, radius: Float, thickness: Float, segs: Int = 36) {
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

  /**
   * Forward-facing circular projector lens disc oriented along +X (or -X) for realistic round/oval headlights.
   */
  fun addXFacingLensDisc(cx: Float, cy: Float, cz: Float, ry: Float, rz: Float, faceDirX: Float = 1f, segs: Int = 28) {
    val n = Vec3(faceDirX, 0f, 0f)
    val center = addVert(Vec3(cx, cy, cz), n, 0.5f, 0.5f)
    val ring = IntArray(segs)
    for (i in 0 until segs) {
      val ang = (i.toFloat() / segs) * (2f * PI.toFloat())
      val ca = cos(ang); val sa = sin(ang)
      // Slight convex lens dome bulge along faceDirX
      val bulge = faceDirX * 0.008f
      val p = Vec3(cx + bulge * 0.1f, cy + ry * sa, cz + rz * ca)
      val nl = normVec(Vec3(faceDirX, sa * 0.25f, ca * 0.25f))
      ring[i] = addVert(p, nl, 0.5f - 0.48f * ca * faceDirX, 0.5f - 0.48f * sa)
    }
    for (i in 0 until segs) {
      val ni = (i + 1) % segs
      if (faceDirX > 0f) {
        indices += center; indices += ring[ni]; indices += ring[i]
      } else {
        indices += center; indices += ring[i]; indices += ring[ni]
      }
    }
  }

  fun addHorizontalDisc(cx: Float, cy: Float, cz: Float, radius: Float, thickness: Float, segs: Int = 48) {
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

  /**
   * High-resolution sculpted pneumatic motorcycle / EUC / superscooter tire with contoured sidewall shoulders,
   * integrated subtle tread-channel crown relief, and clean seam-free UVs — ZERO floating Minecraft cubes!
   */
  fun addSculptedTire(cx: Float, cy: Float, cz: Float, outerR: Float, innerR: Float, width: Float, segs: Int = 48) {
    val hw = width * 0.5f
    val midR = (outerR + innerR) * 0.5f
    val tubeR = (outerR - innerR) * 0.5f
    val profileSteps = 20
    val grid = Array(segs) { IntArray(profileSteps) }
    for (i in 0 until segs) {
      val theta = (i.toFloat() / segs) * (2f * PI.toFloat())
      val ct = cos(theta); val st = sin(theta)
      val u = (i.toFloat() / segs) * 4f
      for (j in 0 until profileSteps) {
        val phi = (j.toFloat() / profileSteps) * (2f * PI.toFloat())
        val cp = cos(phi) // +1 = outer crown, -1 = inner rim bead
        val sp = sin(phi) // +/-1 = left/right sidewalls
        // Realistic motorcycle/all-terrain pneumatic profile: rounded crown, fuller shoulder, tucked rim bead
        val crownShape = when {
          cp > 0.25f -> 0.96f + 0.04f * cos((sp) * PI.toFloat() * 0.5f)
          cp < -0.55f -> 0.94f
          else -> 1.0f
        }
        // Subtle integrated circumferential rain-groove & chevron micro-relief on outer crown only
        val treadRelief = if (cp > 0.48f) {
          val wave = sin(theta * 24f + abs(sp) * 5.0f) * 0.0045f * ((cp - 0.48f) / 0.52f)
          wave
        } else 0f
        val r = midR + tubeR * cp * crownShape + treadRelief
        val sidePinch = if (cp < -0.35f) 0.86f else 1.0f
        val x = cx + r * ct
        val y = cy + r * st
        val z = cz + hw * sp * sidePinch
        // Map V cleanly so center of texture (v=0.5) is the outer tire crown and edges (0/1) are sidewalls
        val vCoord = 0.5f + sp * 0.46f * (if (cp >= 0f) 0.78f else 1.0f)
        grid[i][j] = addVert(Vec3(x, y, z), Vec3(cp * ct, cp * st, sp), u, vCoord)
      }
    }
    for (i in 0 until segs) {
      val ni = (i + 1) % segs
      for (j in 0 until profileSteps) {
        val nj = (j + 1) % profileSteps
        val a = grid[i][j]; val b = grid[ni][j]; val c = grid[ni][nj]; val d = grid[i][nj]
        indices += a; indices += b; indices += c
        indices += a; indices += c; indices += d
        if (j % 4 == 0 && i % 2 == 0) {
          addWireEdge(
            Vec3(positions[a * 3], positions[a * 3 + 1], positions[a * 3 + 2]),
            Vec3(positions[b * 3], positions[b * 3 + 1], positions[b * 3 + 2]),
          )
        }
      }
    }
  }

  /**
   * Deep-dish CNC alloy wheel rim barrel with beveled flanges.
   */
  fun addRimBarrel(cx: Float, cy: Float, cz: Float, rimR: Float, width: Float, segs: Int = 36) {
    val hw = width * 0.40f
    val profile = listOf(
      -hw to rimR * 1.02f,
      -hw * 0.88f to rimR * 0.94f,
      0f to rimR * 0.90f,
      hw * 0.88f to rimR * 0.94f,
      hw to rimR * 1.02f,
    )
    val grid = Array(segs) { IntArray(profile.size) }
    for (i in 0 until segs) {
      val ang = (i.toFloat() / segs) * (2f * PI.toFloat())
      val ca = cos(ang); val sa = sin(ang)
      for (k in profile.indices) {
        val (pz, pr) = profile[k]
        grid[i][k] = addVert(Vec3(cx + pr * ca, cy + pr * sa, cz + pz), Vec3(-ca, -sa, (k - 2) * 0.15f), (i / segs.toFloat()) * 3f, k / 4f)
      }
    }
    for (i in 0 until segs) {
      val ni = (i + 1) % segs
      for (k in 0 until profile.lastIndex) {
        val a = grid[i][k]; val b = grid[ni][k]; val c = grid[ni][k + 1]; val d = grid[i][k + 1]
        indices += a; indices += b; indices += c
        indices += a; indices += c; indices += d
      }
    }
  }

  fun addCurvedFender(cx: Float, cy: Float, radius: Float, startDeg: Float, endDeg: Float, halfW: Float, thickness: Float = 0.020f, steps: Int = 18) {
    val gridOut = Array(steps + 1) { IntArray(5) }
    val gridIn = Array(steps + 1) { IntArray(5) }
    val zFracs = floatArrayOf(-1f, -0.55f, 0f, 0.55f, 1f)
    val crownDrops = floatArrayOf(-0.024f, -0.005f, 0.006f, -0.005f, -0.024f)
    for (i in 0..steps) {
      val t = i / steps.toFloat()
      val deg = startDeg + (endDeg - startDeg) * t
      val rad = (deg * PI / 180.0).toFloat()
      val cr = cos(rad); val sr = sin(rad)
      // Taper fender ends slightly for sculpted moto styling
      val endTaper = 0.72f + 0.28f * sin(t * PI.toFloat())
      val rOut = radius + thickness
      for (k in 0..4) {
        val rrOut = rOut + crownDrops[k]
        val rrIn = radius + crownDrops[k]
        val zz = halfW * zFracs[k] * endTaper
        gridOut[i][k] = addVert(Vec3(cx + rrOut * cr, cy + rrOut * sr, zz), Vec3(cr, sr, zFracs[k] * 0.35f), t * 2f, k * 0.25f)
        gridIn[i][k] = addVert(Vec3(cx + rrIn * cr, cy + rrIn * sr, zz), Vec3(-cr, -sr, zFracs[k] * 0.35f), t * 2f, k * 0.25f)
      }
    }
    for (i in 0 until steps) {
      for (k in 0 until 4) {
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
    segsChord: Int = 14,
    segsSpan: Int = 10,
  ) {
    val profilePts = segsChord * 2
    val slices = segsSpan * 2 + 1
    val grid = Array(slices) { IntArray(profilePts) }
    for (s in 0 until slices) {
      val spanNorm = (s - segsSpan) / segsSpan.toFloat()
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
    lengthSteps: Int = 20,
    ringSegs: Int = 18,
  ) {
    val rings = Array(lengthSteps + 1) { IntArray(ringSegs) }
    for (i in 0..lengthSteps) {
      val t = i / lengthSteps.toFloat()
      val xNorm = t * 2f - 1f
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

  fun addSprocketWithTeeth(cx: Float, cy: Float, cz: Float, radius: Float, thickness: Float, teethCount: Int = 24) {
    addDisc(cx, cy, cz, radius * 0.88f, thickness, 28)
    val toothH = radius * 0.14f
    val toothW = (2f * PI.toFloat() * radius / teethCount) * 0.26f
    val hz = thickness * 0.44f
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
      val q0 = Vec3(cx + rTip * ca - tx * 0.55f, cy + rTip * sa - ty * 0.55f, cz - hz)
      val q1 = Vec3(cx + rTip * ca + tx * 0.55f, cy + rTip * sa + ty * 0.55f, cz - hz)
      val q2 = Vec3(cx + rTip * ca + tx * 0.55f, cy + rTip * sa + ty * 0.55f, cz + hz)
      val q3 = Vec3(cx + rTip * ca - tx * 0.55f, cy + rTip * sa - ty * 0.55f, cz + hz)
      addQuad(q0, q1, q2, q3, Vec3(ca, sa, 0f), 0.3f, 0.3f, false)
      addQuad(p0, p1, q1, q0, Vec3(0f, 0f, -1f), 0.2f, 0.2f, false)
      addQuad(p2, p3, q3, q2, Vec3(0f, 0f, 1f), 0.2f, 0.2f, false)
    }
  }

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
  /**
   * Converts sRGB automotive color swatches into true linear light space required by glTF 2.0 PBR.
   */
  private fun srgbToLinear(c: Float): Double = c.coerceIn(0f, 1f).toDouble().pow(2.2)

  private class RawSceneBuckets(
    val buckets: List<MeshBucket>,
    val wheelAxleCenters: Map<String, Vec3>,
    val explodeDirs: Map<String, Vec3>,
  )

  fun buildVehicleGlb(
    archetype: Pev3dArchetype,
    finish: Pev3dModelFinish,
    lightsOn: Boolean,
    xrayShell: Boolean = false,
    carbonGlossMode: Boolean = false,
  ): PevBuiltGlb {
    val raw = buildRawSceneBuckets(archetype, finish, lightsOn, xrayShell, carbonGlossMode)
    return packBucketsToGlb(raw.buckets, raw.wheelAxleCenters, raw.explodeDirs, xrayShell)
  }

  fun buildSceneMeshes(
    archetype: Pev3dArchetype,
    finish: Pev3dModelFinish,
    lightsOn: Boolean,
  ): PevBuiltScene {
    val raw = buildRawSceneBuckets(archetype, finish, lightsOn, xrayShell = false, carbonGlossMode = false)
    val parts = ArrayList<PevSceneMeshPart>(raw.buckets.size)
    val wireGroups = ArrayList<PevWireGroup>(raw.buckets.size)
    for (b in raw.buckets) {
      val mk = b.matKey
      val axle = raw.wheelAxleCenters[b.nodeName]
      val exp = raw.explodeDirs[mk.partKey] ?: raw.explodeDirs[b.nodeName] ?: Vec3(0f, 0f, 0f)
      val vCount = b.positions.size / 3
      if (vCount > 0 && b.indices.size >= 3) {
        if (vCount <= 32760) {
          val verts = FloatArray(vCount * 8)
          for (i in 0 until vCount) {
            val vo = i * 8
            val po = i * 3
            val uo = i * 2
            verts[vo] = b.positions[po]
            verts[vo + 1] = b.positions[po + 1]
            verts[vo + 2] = b.positions[po + 2]
            verts[vo + 3] = b.normals[po]
            verts[vo + 4] = b.normals[po + 1]
            verts[vo + 5] = b.normals[po + 2]
            verts[vo + 6] = b.uvs[uo]
            verts[vo + 7] = b.uvs[uo + 1]
          }
          val idxArr = ShortArray(b.indices.size) { k -> b.indices[k].toShort() }
          parts += PevSceneMeshPart(
            nodeName = b.nodeName,
            partKey = mk.partKey,
            texId = mk.texId,
            baseRgb = mk.baseRgb,
            metallic = mk.metallic,
            roughness = mk.roughness,
            emissiveRgb = mk.emissiveRgb,
            clearcoat = mk.clearcoat,
            isShell = mk.isShell,
            isScreen = mk.isScreen,
            axleCenter = axle,
            explodeDir = exp,
            vertices = verts,
            indices = idxArr,
            wireframeLines = b.wirePositions.toFloatArray(),
          )
        }
      }
      if (b.wirePositions.isNotEmpty()) {
        wireGroups += PevWireGroup(
          partKey = mk.partKey,
          axleCenter = axle,
          explodeDir = exp,
          lines = b.wirePositions.toFloatArray(),
        )
      }
    }
    return PevBuiltScene(parts = parts, wireGroups = wireGroups)
  }

  private fun buildRawSceneBuckets(
    archetype: Pev3dArchetype,
    finish: Pev3dModelFinish,
    lightsOn: Boolean,
    xrayShell: Boolean,
    carbonGlossMode: Boolean,
  ): RawSceneBuckets {
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

    // Authentic automotive & PEV hardware finishes (sRGB swatches, linearized at glTF packing)
    val rubberNeutral = floatArrayOf(0.95f, 0.95f, 0.96f) // Tire texture already carries deep matte rubber tone
    val cncAluminum = floatArrayOf(0.84f, 0.86f, 0.89f)
    val satinTitanium = floatArrayOf(0.42f, 0.45f, 0.49f)
    val anodizedBlack = floatArrayOf(0.24f, 0.26f, 0.29f)
    val matteCarbon = floatArrayOf(0.46f, 0.48f, 0.51f)
    val saddleVinyl = floatArrayOf(0.94f, 0.94f, 0.95f)
    val batteryAlloy = floatArrayOf(0.92f, 0.94f, 0.96f) // Texture carries realistic dark graphite extruded aluminum
    val motorHousing = floatArrayOf(0.32f, 0.34f, 0.38f) // Satin dark gunmetal motor casing
    val statorBronze = floatArrayOf(0.76f, 0.50f, 0.24f) // Real copper/bronze stator ring accent
    val focHeatsink = floatArrayOf(0.50f, 0.53f, 0.58f) // CNC billet silver-titanium controller heatsink
    val steelChain = floatArrayOf(0.58f, 0.55f, 0.48f)
    val tftWhite = floatArrayOf(1.0f, 1.0f, 1.0f)

    // Automotive Photo-Studio Polished Concrete Showroom Floor + Machined Steel Turntable Plinth
    newBucket(
      nodeName = "floor",
      partKey = "floor",
      texId = PevPbrMaterialId.DYNO_FLOOR,
      baseRgb = floatArrayOf(0.96f, 0.96f, 0.97f),
      metallic = 0.14f,
      roughness = 0.44f,
    ) {
      addQuad(
        Vec3(-2.4f, -0.78f, 2.4f),
        Vec3(2.4f, -0.78f, 2.4f),
        Vec3(2.4f, -0.78f, -2.4f),
        Vec3(-2.4f, -0.78f, -2.4f),
        Vec3(0f, 1f, 0f),
        1f,
        1f,
        wire = false,
      )
      addHorizontalDisc(0f, -0.802f, 0f, 1.50f, 0.040f, 56)
    }
    // Understated Brushed Stainless Steel Showroom Turntable Trim Ring (no neon glow)
    newBucket(
      nodeName = "floor_ring",
      partKey = "floor",
      texId = PevPbrMaterialId.BRUSHED_METAL,
      baseRgb = satinTitanium,
      metallic = 0.90f,
      roughness = 0.24f,
    ) {
      val ringSegs = 56
      val r = 1.50f
      val pts = (0..ringSegs).map { i ->
        val a = (i.toFloat() / ringSegs) * (2f * PI.toFloat())
        Vec3(r * cos(a), -0.775f, r * sin(a))
      }
      addSweptTube(pts, List(pts.size) { 0.009f }, segs = 8, capEnds = false)
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

      // Smooth High-Resolution Sculpted Rubber Tire (48x20 carcass + high-res V-chevron PBR tread)
      newBucket(
        nodeName = nodeName,
        partKey = motorKey,
        texId = PevPbrMaterialId.TIRE_RUBBER,
        baseRgb = rubberNeutral,
        metallic = 0.03f,
        roughness = 0.86f,
      ) {
        addSculptedTire(cx, cy, cz, outerR, innerR, width, 48)
      }

      // Deep-Dish Anodized Alloy Rim Barrel + Hub Motor Shell + 16 Cross-Laced Wire Spokes
      val rimRgb = if (isPowered) anodizedBlack else satinTitanium
      newBucket(
        nodeName = nodeName,
        partKey = motorKey,
        texId = PevPbrMaterialId.BRUSHED_METAL,
        baseRgb = rimRgb,
        metallic = 0.92f,
        roughness = 0.22f,
        clearcoat = 0.30f,
      ) {
        addRimBarrel(cx, cy, cz, innerR, width, 36)
        // Hub motor housing or forged front hub with beveled end caps
        val hubR = if (isPowered) innerR * 0.58f else innerR * 0.32f
        addDisc(cx, cy, cz, hubR, width * 0.72f, 32)
        addDisc(cx, cy, cz, hubR * 0.65f, width * 0.90f, 28)
      }

      // Stainless Wire Spokes + Anodized Rim Pinstripe Accent
      newBucket(
        nodeName = nodeName,
        partKey = motorKey,
        texId = PevPbrMaterialId.BRUSHED_METAL,
        baseRgb = cncAluminum,
        metallic = 0.95f,
        roughness = 0.16f,
      ) {
        val spokeCount = 16
        val hubAnchorR = if (isPowered) innerR * 0.54f else innerR * 0.28f
        for (s in 0 until spokeCount) {
          val a = (s / spokeCount.toFloat()) * (2f * PI.toFloat())
          val zSide = if (s % 2 == 0) width * 0.24f else -width * 0.24f
          val crossAngle = if (s % 2 == 0) 0.18f else -0.18f
          addTube(
            Vec3(cx + hubAnchorR * cos(a), cy + hubAnchorR * sin(a), cz + zSide),
            Vec3(cx + innerR * 0.95f * cos(a + crossAngle), cy + innerR * 0.95f * sin(a + crossAngle), cz),
            0.0065f,
            6,
          )
        }
      }
      // Subtle anodized rim lip pinstripe matching vehicle hardware accent
      newBucket(
        nodeName = nodeName,
        partKey = motorKey,
        texId = PevPbrMaterialId.BRUSHED_METAL,
        baseRgb = finish.accentRgb,
        metallic = 0.90f,
        roughness = 0.18f,
        clearcoat = 0.45f,
      ) {
        val lipPtsF = (0..36).map { i ->
          val a = (i / 36f) * (2f * PI.toFloat())
          Vec3(cx + innerR * 0.99f * cos(a), cy + innerR * 0.99f * sin(a), cz + width * 0.37f)
        }
        addSweptTube(lipPtsF, List(lipPtsF.size) { 0.008f }, 6, false)
        val lipPtsB = (0..36).map { i ->
          val a = (i / 36f) * (2f * PI.toFloat())
          Vec3(cx + innerR * 0.99f * cos(a), cy + innerR * 0.99f * sin(a), cz - width * 0.37f)
        }
        addSweptTube(lipPtsB, List(lipPtsB.size) { 0.008f }, 6, false)
      }

      // Floating Slotted & Cross-Drilled Stainless Steel Brake Rotor (spins with wheel)
      val bz = cz + width * 0.48f + 0.024f
      newBucket(
        nodeName = nodeName,
        partKey = "brakes",
        texId = PevPbrMaterialId.BRAKE_ROTOR,
        baseRgb = cncAluminum,
        metallic = 0.95f,
        roughness = 0.18f,
      ) {
        addDisc(cx, cy, bz, outerR * 0.50f, 0.014f, 36)
      }

      // Sculpted Monobloc Radial 4-Piston Brake Caliper (organic pod + carrier bracket)
      newBucket(
        nodeName = "brakes",
        partKey = "brakes",
        texId = PevPbrMaterialId.BRUSHED_METAL,
        baseRgb = finish.caliperRgb,
        metallic = 0.86f,
        roughness = 0.22f,
        clearcoat = 0.65f,
      ) {
        addSculptedPod(cx + outerR * 0.34f, cy + outerR * 0.26f, bz, 0.068f, 0.056f, 0.038f, exp = 0.55f, rings = 8, segs = 14)
        addRoundedBlock(cx + outerR * 0.23f, cy + outerR * 0.18f, bz - 0.022f, 0.036f, 0.030f, 0.016f, exp = 0.55f)
      }
    }

    val ws = finish.wheelScale
    val ls = finish.bodyLengthScale
    val bp = finish.blueprintType

    when (bp) {
      Pev3dBlueprintType.SCOOTER_DUAL_STEM_OFFROAD,
      Pev3dBlueprintType.SCOOTER_TUBULAR_RACING,
      Pev3dBlueprintType.SCOOTER_MONOCOQUE_AERO -> {
        val isDualStem = bp == Pev3dBlueprintType.SCOOTER_DUAL_STEM_OFFROAD
        val isTubular = bp == Pev3dBlueprintType.SCOOTER_TUBULAR_RACING
        val isAero = bp == Pev3dBlueprintType.SCOOTER_MONOCOQUE_AERO
        val wheelR = (if (isDualStem) 0.32f else if (isAero) 0.28f else 0.30f) * ws
        val wheelW = if (isDualStem) 0.26f else 0.23f
        addDetailedWheel("wheel_rear", -0.92f * ls, -0.45f, 0f, wheelR, wheelW, "motor", true)
        addDetailedWheel("wheel_front", 0.92f * ls, -0.45f, 0f, wheelR, wheelW, "suspension", true)

        newBucket(
          nodeName = "frame",
          partKey = "frame",
          texId = PevPbrMaterialId.CARBON_WEAVE,
          baseRgb = matteCarbon,
          metallic = 0.28f,
          roughness = 0.24f,
          clearcoat = 0.88f,
          isShell = true,
        ) {
          if (isAero) {
            // Smooth continuous organic monocoque fuselage shell (Segway GT / Apollo Pro)
            addLoftedBodyX(
              stations = listOf(
                LoftStation(-0.80f * ls, -0.18f, 0f, 0.036f, 0.14f, exp = 0.62f),
                LoftStation(-0.56f * ls, -0.24f, 0f, 0.072f, 0.23f, exp = 0.56f),
                LoftStation(0.0f, -0.26f, 0f, 0.078f, 0.26f, exp = 0.54f),
                LoftStation(0.56f * ls, -0.22f, 0f, 0.088f, 0.22f, exp = 0.58f),
                LoftStation(0.76f * ls, -0.04f, 0f, 0.115f, 0.14f, exp = 0.68f),
              ),
              ringSegs = 22,
              uTile = 2.6f,
              vTile = 2.2f,
            )
            // Sculpted mono aero blade stem tapering into cockpit
            addLoftedBodyX(
              stations = listOf(
                LoftStation(0.68f * ls, 0.18f, 0f, 0.26f, 0.078f, exp = 0.72f),
                LoftStation(0.56f * ls, 0.50f, 0f, 0.24f, 0.064f, exp = 0.72f),
                LoftStation(0.45f * ls, 0.86f, 0f, 0.12f, 0.056f, exp = 0.72f),
              ),
              ringSegs = 18,
            )
          } else {
            // Wide sculpted carbon deck chassis
            addLoftedBodyX(
              stations = listOf(
                LoftStation(-0.76f * ls, -0.22f, 0f, 0.038f, 0.16f, exp = 0.55f),
                LoftStation(-0.62f * ls, -0.24f, 0f, 0.068f, 0.25f, exp = 0.50f),
                LoftStation(-0.35f * ls, -0.27f, 0f, 0.074f, 0.29f, exp = 0.48f),
                LoftStation(0.35f * ls, -0.27f, 0f, 0.074f, 0.29f, exp = 0.48f),
                LoftStation(0.64f * ls, -0.24f, 0f, 0.064f, 0.23f, exp = 0.52f),
                LoftStation(0.76f * ls, -0.18f, 0f, 0.038f, 0.14f, exp = 0.62f),
              ),
              ringSegs = 20,
              uTile = 2.6f,
              vTile = 2.2f,
            )
            // Steep rear kicktail spoiler
            addLoftedBodyX(
              stations = listOf(
                LoftStation(-0.78f * ls, -0.10f, 0f, 0.022f, 0.18f, exp = 0.58f),
                LoftStation(-0.62f * ls, -0.17f, 0f, 0.032f, 0.22f, exp = 0.55f),
                LoftStation(-0.46f * ls, -0.21f, 0f, 0.024f, 0.23f, exp = 0.58f),
              ),
              ringSegs = 16,
            )
          }

          if (isDualStem) {
            // Massive twin vertical upper fork columns + quad-clamp crown (Kaabo Wolf / Dualtron X)
            for (zSign in listOf(-1f, 1f)) {
              addTube(Vec3(0.78f * ls, -0.18f, zSign * 0.138f), Vec3(0.50f * ls, 0.88f, zSign * 0.135f), 0.038f, 16)
              // Side steel deck crash-guard bumpers
              addSweptTube(
                listOf(
                  Vec3(-0.72f * ls, -0.23f, zSign * 0.27f),
                  Vec3(-0.35f * ls, -0.25f, zSign * 0.32f),
                  Vec3(0.35f * ls, -0.25f, zSign * 0.32f),
                  Vec3(0.70f * ls, -0.21f, zSign * 0.25f),
                ),
                listOf(0.018f, 0.020f, 0.020f, 0.018f),
                10,
              )
            }
            addRoundedBlock(0.54f * ls, 0.72f, 0f, 0.055f, 0.022f, 0.17f, exp = 0.52f)
            addRoundedBlock(0.66f * ls, 0.22f, 0f, 0.055f, 0.022f, 0.17f, exp = 0.52f)
          } else if (isTubular) {
            // Welded tubular trellis goose-neck & under-deck spaceframe rails (Nami / Rion / Teverun)
            for (zSign in listOf(-1f, 1f)) {
              addSweptTube(
                listOf(
                  Vec3(0.68f * ls, -0.12f, zSign * 0.078f),
                  Vec3(0.76f * ls, 0.04f, zSign * 0.088f),
                  Vec3(0.60f * ls, 0.28f, zSign * 0.072f),
                  Vec3(0.43f * ls, 0.88f, zSign * 0.058f),
                ),
                listOf(0.028f, 0.026f, 0.026f, 0.024f),
                14,
              )
              addTube(Vec3(0.56f * ls, -0.22f, zSign * 0.18f), Vec3(0.72f * ls, 0.04f, zSign * 0.09f), 0.022f, 12)
              addTube(Vec3(-0.66f * ls, -0.26f, zSign * 0.295f), Vec3(0.58f * ls, -0.26f, zSign * 0.295f), 0.018f, 10)
            }
            // Cross trellis braces
            addTube(Vec3(0.68f * ls, 0.14f, -0.08f), Vec3(0.68f * ls, 0.14f, 0.08f), 0.018f, 10)
          }

          addCurvedFender(-0.92f * ls, -0.45f, (wheelR + 0.04f), 52f, 175f, wheelW * 0.56f)
          addCurvedFender(0.92f * ls, -0.45f, (wheelR + 0.04f), 18f, 132f, wheelW * 0.56f)
        }

        // Textured Vulcanized Rubber Deck Grip Mat
        newBucket("frame_grip", "frame", PevPbrMaterialId.TIRE_RUBBER, rubberNeutral, 0.03f, 0.88f) {
          addLoftedBodyX(
            stations = listOf(
              LoftStation(-0.56f * ls, -0.194f, 0f, 0.008f, 0.19f, exp = 0.50f),
              LoftStation(0.02f * ls, -0.194f, 0f, 0.009f, 0.235f, exp = 0.45f),
              LoftStation(0.56f * ls, -0.194f, 0f, 0.008f, 0.18f, exp = 0.50f),
            ),
            ringSegs = 14,
            uTile = 2.4f,
            vTile = 1.6f,
          )
        }

        // OEM Metallic Livery Side Fairings + Stem Cowling
        newBucket("frame_graphics", "frame", PevPbrMaterialId.RACING_LIVERY, finish.bodyRgb, finish.paintMetallic, 0.15f, clearcoat = 0.96f, isShell = true) {
          for (zSign in listOf(-1f, 1f)) {
            addLoftedBodyX(
              stations = listOf(
                LoftStation(-0.56f * ls, -0.26f, zSign * 0.282f, 0.034f, 0.016f, exp = 0.65f),
                LoftStation(-0.15f * ls, -0.265f, zSign * 0.292f, 0.056f, 0.020f, exp = 0.60f),
                LoftStation(0.32f * ls, -0.265f, zSign * 0.290f, 0.054f, 0.020f, exp = 0.60f),
                LoftStation(0.58f * ls, -0.25f, zSign * 0.245f, 0.036f, 0.015f, exp = 0.68f),
              ),
              ringSegs = 14,
            )
          }
          if (isAero) {
            addSculptedPod(0.57f * ls, 0.54f, 0f, 0.044f, 0.26f, 0.112f, exp = 0.68f, rings = 10, segs = 16)
          } else {
            addSculptedPod(0.54f * ls, 0.52f, 0f, 0.032f, 0.18f, 0.088f, exp = 0.68f, rings = 10, segs = 16)
          }
        }

        newBucket("battery", "battery", PevPbrMaterialId.BATTERY_PACK, batteryAlloy, 0.68f, 0.34f) {
          addRoundedBlock(0f, -0.25f, 0f, 0.48f * ls, 0.050f, 0.20f, uvScale = 1.8f, exp = 0.45f)
        }
        newBucket("controller", "controller", PevPbrMaterialId.BRUSHED_METAL, focHeatsink, 0.90f, 0.22f, clearcoat = 0.3f) {
          addFinnedBlock(0.42f * ls, -0.23f, 0f, 0.14f, 0.054f, 0.19f, 8)
        }

        // Anodized CNC C-Arm Suspension, Hydraulic Damper & Dual Coiled Shocks
        newBucket("suspension", "suspension", PevPbrMaterialId.BRUSHED_METAL, finish.accentRgb, 0.92f, 0.18f, clearcoat = 0.48f) {
          if (isDualStem) {
            // Anodized triple clamps & tall inverted fork lowers
            addTube(Vec3(0.92f * ls, -0.45f, -0.155f), Vec3(0.68f * ls, 0.16f, -0.145f), 0.034f, 14)
            addTube(Vec3(0.92f * ls, -0.45f, 0.155f), Vec3(0.68f * ls, 0.16f, 0.145f), 0.034f, 14)
            addTube(Vec3(-0.68f * ls, -0.26f, -0.15f), Vec3(-0.92f * ls, -0.45f, -0.15f), 0.030f, 14)
            addTube(Vec3(-0.68f * ls, -0.26f, 0.15f), Vec3(-0.92f * ls, -0.45f, 0.15f), 0.030f, 14)
            addHelicalSpring(Vec3(-0.56f * ls, -0.20f, -0.15f), Vec3(-0.82f * ls, -0.38f, -0.15f), 0.036f, 0.008f, 6.0f)
            addHelicalSpring(Vec3(-0.56f * ls, -0.20f, 0.15f), Vec3(-0.82f * ls, -0.38f, 0.15f), 0.036f, 0.008f, 6.0f)
          } else {
            addSculptedPod(0.61f * ls, 0.08f, 0f, 0.052f, 0.048f, 0.108f, exp = 0.55f)
            addTube(Vec3(0.56f * ls, -0.10f, 0.12f), Vec3(0.64f * ls, 0.14f, 0.04f), 0.015f, 12)
            addTube(Vec3(-0.68f * ls, -0.26f, -0.15f), Vec3(-0.92f * ls, -0.45f, -0.15f), 0.028f, 14)
            addTube(Vec3(-0.68f * ls, -0.26f, 0.15f), Vec3(-0.92f * ls, -0.45f, 0.15f), 0.028f, 14)
            addTube(Vec3(0.68f * ls, -0.18f, -0.15f), Vec3(0.92f * ls, -0.45f, -0.15f), 0.030f, 14)
            addTube(Vec3(0.68f * ls, -0.18f, 0.15f), Vec3(0.92f * ls, -0.45f, 0.15f), 0.030f, 14)
            addHelicalSpring(Vec3(-0.56f * ls, -0.22f, -0.15f), Vec3(-0.82f * ls, -0.40f, -0.15f), 0.034f, 0.008f, 5.5f)
            addHelicalSpring(Vec3(-0.56f * ls, -0.22f, 0.15f), Vec3(-0.82f * ls, -0.40f, 0.15f), 0.034f, 0.008f, 5.5f)
            addHelicalSpring(Vec3(0.62f * ls, -0.16f, 0.15f), Vec3(0.84f * ls, -0.38f, 0.15f), 0.034f, 0.008f, 5.5f)
          }
          if (isTubular) {
            // Exposed CNC rotary & linear steering damper along deck neck
            addTube(Vec3(0.46f * ls, -0.10f, 0.13f), Vec3(0.74f * ls, -0.02f, 0.05f), 0.016f, 12)
          }
        }

        newBucket("brakes", "brakes", PevPbrMaterialId.CARBON_WEAVE, anodizedBlack, 0.35f, 0.42f) {
          addSweptTube(
            listOf(Vec3(0.45f * ls, 0.86f, 0.24f), Vec3(0.55f * ls, 0.42f, 0.14f), Vec3(0.70f * ls, 0.02f, 0.16f), Vec3(0.94f * ls, -0.35f, 0.15f)),
            listOf(0.007f, 0.007f, 0.007f, 0.007f),
            8,
          )
          addSweptTube(
            listOf(Vec3(0.45f * ls, 0.86f, -0.24f), Vec3(0.52f * ls, -0.16f, -0.14f), Vec3(-0.58f * ls, -0.22f, -0.14f), Vec3(-0.86f * ls, -0.38f, 0.14f)),
            listOf(0.007f, 0.007f, 0.007f, 0.007f),
            8,
          )
        }

        newBucket("cockpit", "cockpit", PevPbrMaterialId.BRUSHED_METAL, anodizedBlack, 0.86f, 0.26f) {
          val barW = if (isDualStem) 0.50f else 0.45f
          addSweptTube(
            listOf(Vec3(0.39f * ls, 0.89f, -barW), Vec3(0.43f * ls, 0.88f, -0.18f), Vec3(0.42f * ls, 0.88f, 0f), Vec3(0.43f * ls, 0.88f, 0.18f), Vec3(0.39f * ls, 0.89f, barW)),
            listOf(0.024f, 0.021f, 0.025f, 0.021f, 0.024f),
            14,
          )
          addTube(Vec3(0.44f * ls, 0.88f, -0.28f), Vec3(0.51f * ls, 0.86f, -0.40f), 0.008f, 8)
          addTube(Vec3(0.44f * ls, 0.88f, 0.28f), Vec3(0.51f * ls, 0.86f, 0.40f), 0.008f, 8)
          addSculptedPod(0.44f * ls, 0.90f, -0.26f, 0.022f, 0.016f, 0.026f, exp = 0.65f)
          addSculptedPod(0.44f * ls, 0.90f, 0.26f, 0.022f, 0.016f, 0.026f, exp = 0.65f)
          addRoundedBlock(0.45f * ls, 0.94f, 0f, 0.048f, 0.042f, 0.106f, exp = 0.55f)
        }
        newBucket("cockpit_grips", "cockpit", PevPbrMaterialId.TIRE_RUBBER, rubberNeutral, 0.04f, 0.86f) {
          val barW = if (isDualStem) 0.51f else 0.47f
          addTube(Vec3(0.415f * ls, 0.886f, 0.28f), Vec3(0.39f * ls, 0.89f, barW), 0.030f, 12)
          addTube(Vec3(0.415f * ls, 0.886f, -0.28f), Vec3(0.39f * ls, 0.89f, -barW), 0.030f, 12)
        }
        newBucket("cockpit_tft", "cockpit", PevPbrMaterialId.TFT_SCREEN, tftWhite, 0.08f, 0.08f, floatArrayOf(0.55f, 0.58f, 0.62f), isScreen = true) {
          addQuad(
            Vec3(0.488f * ls, 0.912f, 0.090f),
            Vec3(0.488f * ls, 0.912f, -0.090f),
            Vec3(0.468f * ls, 0.976f, -0.090f),
            Vec3(0.468f * ls, 0.976f, 0.090f),
            Vec3(0.95f, 0.30f, 0f),
            1f,
            1f,
            wire = false,
          )
        }
      }

      Pev3dBlueprintType.EUC_ARMORED_ROLLCAGE,
      Pev3dBlueprintType.EUC_AERO_TRAIL -> {
        val isArmored = bp == Pev3dBlueprintType.EUC_ARMORED_ROLLCAGE
        val wheelR = (if (isArmored) 0.57f else 0.54f) * ws
        addDetailedWheel("wheel_rear", 0f, -0.12f, 0f, wheelR, 0.25f, "motor", true)

        newBucket("battery", "battery", PevPbrMaterialId.BATTERY_PACK, batteryAlloy, 0.68f, 0.34f) {
          for (zSign in listOf(-1f, 1f)) {
            addLoftedBodyX(
              stations = listOf(
                LoftStation(-0.31f, 0.06f, zSign * 0.225f, if (isArmored) 0.21f else 0.17f, 0.068f, exp = 0.68f),
                LoftStation(-0.15f, 0.09f, zSign * 0.235f, if (isArmored) 0.27f else 0.24f, 0.082f, exp = 0.58f),
                LoftStation(0.15f, 0.09f, zSign * 0.235f, if (isArmored) 0.27f else 0.24f, 0.082f, exp = 0.58f),
                LoftStation(0.31f, 0.06f, zSign * 0.225f, if (isArmored) 0.21f else 0.17f, 0.068f, exp = 0.68f),
              ),
              ringSegs = 16,
              uTile = 1.6f,
              vTile = 1.6f,
            )
          }
        }

        newBucket("frame", "frame", PevPbrMaterialId.CARBON_WEAVE, matteCarbon, 0.26f, 0.24f, clearcoat = 0.92f, isShell = true) {
          for (zSign in listOf(-1f, 1f)) {
            addLoftedBodyX(
              stations = listOf(
                LoftStation(-0.37f, 0.04f, zSign * 0.255f, 0.14f, 0.032f, exp = 0.85f),
                LoftStation(-0.26f, 0.10f, zSign * 0.285f, 0.25f, 0.048f, exp = 0.72f),
                LoftStation(0.0f, 0.14f, zSign * 0.305f, 0.30f, 0.056f, exp = 0.68f),
                LoftStation(0.26f, 0.10f, zSign * 0.285f, 0.25f, 0.048f, exp = 0.72f),
                LoftStation(0.37f, 0.04f, zSign * 0.255f, 0.14f, 0.032f, exp = 0.85f),
              ),
              ringSegs = 20,
              uTile = 2.2f,
              vTile = 2.0f,
            )
            addSweptTube(
              listOf(Vec3(-0.35f, -0.06f, zSign * 0.26f), Vec3(-0.34f, 0.36f, zSign * 0.25f), Vec3(0f, 0.48f, zSign * 0.22f), Vec3(0.34f, 0.36f, zSign * 0.25f), Vec3(0.35f, -0.06f, zSign * 0.26f)),
              listOf(0.024f, 0.028f, 0.030f, 0.028f, 0.024f),
              14,
            )
            // Beveled CNC Pedals
            addLoftedBodyX(
              stations = listOf(
                LoftStation(-0.22f, -0.30f, zSign * 0.35f, 0.016f, 0.088f, exp = 0.52f),
                LoftStation(0.0f, -0.30f, zSign * 0.36f, 0.020f, 0.122f, exp = 0.45f),
                LoftStation(0.22f, -0.30f, zSign * 0.35f, 0.016f, 0.088f, exp = 0.52f),
              ),
              ringSegs = 14,
            )
          }

          if (isArmored) {
            // Veteran Sherman / Lynx iconic external tubular steel front + rear roll-cage bars!
            for (endSign in listOf(-1f, 1f)) {
              addSweptTube(
                listOf(
                  Vec3(endSign * 0.33f, -0.10f, -0.25f),
                  Vec3(endSign * 0.43f, 0.24f, -0.23f),
                  Vec3(endSign * 0.40f, 0.52f, -0.16f),
                  Vec3(endSign * 0.40f, 0.52f, 0.16f),
                  Vec3(endSign * 0.43f, 0.24f, 0.23f),
                  Vec3(endSign * 0.33f, -0.10f, 0.25f),
                ),
                listOf(0.020f, 0.022f, 0.022f, 0.022f, 0.022f, 0.020f),
                12,
              )
            }
            // High square MX jump-pad saddle block
            addRoundedBlock(0f, 0.52f, 0f, 0.28f, 0.055f, 0.175f, exp = 0.65f)
          } else {
            // Contoured Top Saddle Pad Crown + Aero Rear Fin
            addLoftedBodyX(
              stations = listOf(
                LoftStation(-0.30f, 0.48f, 0f, 0.040f, 0.15f, exp = 0.78f),
                LoftStation(0.0f, 0.53f, 0f, 0.052f, 0.18f, exp = 0.72f),
                LoftStation(0.30f, 0.48f, 0f, 0.040f, 0.15f, exp = 0.78f),
              ),
              ringSegs = 18,
            )
          }

          addSweptTube(
            listOf(Vec3(-0.32f, -0.18f, -0.14f), Vec3(-0.46f, -0.58f, -0.14f), Vec3(-0.46f, -0.58f, 0.14f), Vec3(-0.32f, -0.18f, 0.14f)),
            listOf(0.015f, 0.015f, 0.015f, 0.015f),
            10,
          )
          addCurvedFender(0f, -0.12f, 0.60f * ws, 130f, 225f, 0.15f)
        }

        newBucket("frame_graphics", "frame", PevPbrMaterialId.RACING_LIVERY, finish.bodyRgb, finish.paintMetallic, 0.15f, clearcoat = 0.96f, isShell = true) {
          for (zSign in listOf(-1f, 1f)) {
            addLoftedBodyX(
              stations = listOf(
                LoftStation(-0.30f, 0.14f, zSign * 0.330f, 0.11f, 0.016f, exp = 0.78f),
                LoftStation(0.0f, 0.18f, zSign * 0.348f, 0.16f, 0.022f, exp = 0.70f),
                LoftStation(0.30f, 0.14f, zSign * 0.330f, 0.11f, 0.016f, exp = 0.78f),
              ),
              ringSegs = 16,
            )
          }
        }

        newBucket("frame_pads", "frame", PevPbrMaterialId.QUILTED_LEATHER, saddleVinyl, 0.04f, 0.78f, isShell = true) {
          for (zSign in listOf(-1f, 1f)) {
            addSweptTube(
              listOf(Vec3(0.22f, -0.14f, zSign * 0.355f), Vec3(0.20f, 0.04f, zSign * 0.368f), Vec3(0.11f, 0.21f, zSign * 0.362f)),
              listOf(0.036f, 0.046f, 0.034f),
              14,
            )
            addSweptTube(
              listOf(Vec3(-0.22f, -0.14f, zSign * 0.355f), Vec3(-0.21f, 0.03f, zSign * 0.366f), Vec3(-0.14f, 0.18f, zSign * 0.360f)),
              listOf(0.032f, 0.042f, 0.030f),
              14,
            )
          }
        }

        newBucket("motor", "motor", PevPbrMaterialId.BRUSHED_METAL, statorBronze, 0.92f, 0.24f) {
          addDisc(0f, -0.12f, 0f, 0.23f * ws, 0.265f, 32)
        }

        newBucket("suspension", "suspension", PevPbrMaterialId.BRUSHED_METAL, finish.accentRgb, 0.93f, 0.18f, clearcoat = 0.45f) {
          if (isArmored) {
            // Vertical Fastace CNC side stanchions
            addTube(Vec3(-0.20f, -0.18f, 0.26f), Vec3(-0.20f, 0.54f, 0.26f), 0.036f, 16)
            addTube(Vec3(-0.20f, -0.18f, -0.26f), Vec3(-0.20f, 0.54f, -0.26f), 0.036f, 16)
            addTube(Vec3(0.20f, -0.18f, 0.26f), Vec3(0.20f, 0.54f, 0.26f), 0.030f, 14)
            addTube(Vec3(0.20f, -0.18f, -0.26f), Vec3(0.20f, 0.54f, -0.26f), 0.030f, 14)
            addHelicalSpring(Vec3(-0.20f, -0.06f, 0.26f), Vec3(-0.20f, 0.36f, 0.26f), 0.046f, 0.0085f, 6.5f)
            addHelicalSpring(Vec3(-0.20f, -0.06f, -0.26f), Vec3(-0.20f, 0.36f, -0.26f), 0.046f, 0.0085f, 6.5f)
          } else {
            // Exposed rear linkage coil shock (S22 / Begode Extreme style)
            addTube(Vec3(-0.22f, -0.14f, 0.23f), Vec3(-0.22f, 0.46f, 0.23f), 0.028f, 14)
            addTube(Vec3(-0.22f, -0.14f, -0.23f), Vec3(-0.22f, 0.46f, -0.23f), 0.028f, 14)
            addTube(Vec3(-0.34f, -0.10f, 0f), Vec3(-0.18f, 0.42f, 0f), 0.034f, 16)
            addHelicalSpring(Vec3(-0.34f, -0.08f, 0f), Vec3(-0.19f, 0.38f, 0f), 0.056f, 0.010f, 6.0f)
          }
        }

        newBucket("controller", "controller", PevPbrMaterialId.BRUSHED_METAL, focHeatsink, 0.90f, 0.22f) {
          addFinnedBlock(0f, 0.46f, 0f, 0.22f, 0.068f, 0.17f, 9)
        }

        newBucket("cockpit", "cockpit", PevPbrMaterialId.TFT_SCREEN, tftWhite, 0.08f, 0.08f, floatArrayOf(0.55f, 0.58f, 0.62f), isScreen = true) {
          addQuad(
            Vec3(-0.10f, 0.585f, 0.085f),
            Vec3(0.14f, 0.585f, 0.085f),
            Vec3(0.14f, 0.585f, -0.085f),
            Vec3(-0.10f, 0.585f, -0.085f),
            Vec3(0f, 1f, 0f),
            1f,
            1f,
            wire = false,
          )
        }
        newBucket("cockpit_trolley", "cockpit", PevPbrMaterialId.BRUSHED_METAL, satinTitanium, 0.90f, 0.20f) {
          addSweptTube(
            listOf(Vec3(-0.20f, 0.50f, -0.14f), Vec3(-0.20f, 0.72f, -0.14f), Vec3(-0.16f, 0.72f, 0.14f), Vec3(-0.20f, 0.50f, 0.14f)),
            listOf(0.015f, 0.017f, 0.017f, 0.015f),
            10,
          )
        }
      }

      Pev3dBlueprintType.HYDROFOIL_EFOIL -> {
        // Lofted Marine Metallic Gelcoat & Carbon eFoil Surf Hull
        newBucket("frame", "frame", PevPbrMaterialId.RACING_LIVERY, finish.bodyRgb, finish.paintMetallic, 0.14f, clearcoat = 0.98f, isShell = true) {
          addSculptedDeckHull(0f, 0.14f, 0f, 1.10f, 0.072f, 0.39f, 22, 20)
        }
        newBucket("frame_deckpad", "frame", PevPbrMaterialId.TIRE_RUBBER, rubberNeutral, 0.03f, 0.88f) {
          addLoftedBodyX(
            stations = listOf(
              LoftStation(-0.68f, 0.21f, 0f, 0.010f, 0.20f, exp = 0.55f),
              LoftStation(-0.08f, 0.214f, 0f, 0.012f, 0.27f, exp = 0.50f),
              LoftStation(0.54f, 0.21f, 0f, 0.010f, 0.19f, exp = 0.55f),
            ),
            ringSegs = 16,
            uTile = 2.4f,
            vTile = 1.6f,
          )
          addSweptTube(
            listOf(Vec3(0.28f, 0.22f, -0.12f), Vec3(0.28f, 0.31f, -0.02f), Vec3(0.28f, 0.22f, 0.10f)),
            listOf(0.014f, 0.018f, 0.014f),
            10,
          )
          addSweptTube(
            listOf(Vec3(-0.34f, 0.22f, -0.11f), Vec3(-0.34f, 0.31f, 0f), Vec3(-0.34f, 0.22f, 0.11f)),
            listOf(0.014f, 0.018f, 0.014f),
            10,
          )
        }
        newBucket("battery", "battery", PevPbrMaterialId.BATTERY_PACK, batteryAlloy, 0.65f, 0.34f) {
          addRoundedBlock(-0.08f, 0.155f, 0f, 0.36f, 0.044f, 0.20f, uvScale = 1.8f, exp = 0.50f)
        }
        newBucket("controller", "controller", PevPbrMaterialId.BRUSHED_METAL, focHeatsink, 0.90f, 0.20f) {
          addFinnedBlock(0.36f, 0.155f, 0f, 0.13f, 0.040f, 0.14f, 7)
        }
        newBucket("suspension", "suspension", PevPbrMaterialId.CARBON_WEAVE, matteCarbon, 0.34f, 0.18f, clearcoat = 0.92f) {
          addLoftedBodyX(
            stations = listOf(
              LoftStation(-0.12f, -0.32f, 0f, 0.44f, 0.006f, exp = 0.80f),
              LoftStation(-0.04f, -0.32f, 0f, 0.44f, 0.018f, exp = 0.72f),
              LoftStation(0.04f, -0.32f, 0f, 0.44f, 0.014f, exp = 0.78f),
              LoftStation(0.09f, -0.32f, 0f, 0.44f, 0.005f, exp = 0.90f),
            ),
            ringSegs = 14,
          )
          addRoundedBlock(-0.04f, 0.08f, 0f, 0.16f, 0.022f, 0.080f, exp = 0.55f)
        }
        newBucket("motor", "motor", PevPbrMaterialId.BRUSHED_METAL, satinTitanium, 0.94f, 0.16f) {
          addSweptTube(
            listOf(Vec3(-0.56f, -0.76f, 0f), Vec3(-0.22f, -0.76f, 0f), Vec3(0.28f, -0.76f, 0f), Vec3(0.48f, -0.76f, 0f)),
            listOf(0.018f, 0.056f, 0.050f, 0.016f),
            20,
          )
          val ductPts = (0..28).map { i ->
            val a = (i / 28f) * (2f * PI.toFloat())
            Vec3(-0.24f, -0.76f + 0.115f * sin(a), 0.115f * cos(a))
          }
          addSweptTube(ductPts, List(ductPts.size) { 0.011f }, 8, false)
          for (b in 0 until 3) {
            val a = (b / 3f) * (2f * PI.toFloat())
            addTube(
              Vec3(-0.24f, -0.76f, 0f),
              Vec3(-0.27f, -0.76f + 0.094f * sin(a), 0.094f * cos(a)),
              0.015f,
              8,
            )
          }
        }
        newBucket("brakes", "brakes", PevPbrMaterialId.CARBON_WEAVE, matteCarbon, 0.34f, 0.18f, clearcoat = 0.92f) {
          addAirfoilWing(0.26f, -0.76f, 0f, chord = 0.24f, thickness = 0.18f, halfSpan = 0.56f, sweepBack = 0.11f)
          addAirfoilWing(-0.46f, -0.76f, 0f, chord = 0.14f, thickness = 0.14f, halfSpan = 0.28f, sweepBack = 0.06f)
        }
        newBucket("cockpit", "cockpit", PevPbrMaterialId.TFT_SCREEN, tftWhite, 0.14f, 0.12f, floatArrayOf(0.55f, 0.58f, 0.62f), isScreen = true) {
          addSculptedPod(0.75f, 0.28f, 0f, 0.068f, 0.028f, 0.042f, exp = 0.65f)
        }
      }

      Pev3dBlueprintType.EBIKE_MOUNTAIN_FULLSUSP -> {
        // Authentic Sloped Enduro e-MTB (Turbo Levo 29"/27.5" Mullet, diamond chassis, dropper saddle, integrated downtube pack)
        val wheelR = 0.44f * ws
        addDetailedWheel("wheel_rear", -0.90f * ls, -0.34f, 0f, wheelR * 0.95f, 0.15f, "motor", false)
        addDetailedWheel("wheel_front", 0.94f * ls, -0.34f, 0f, wheelR, 0.15f, "brakes", false)

        newBucket("frame", "frame", PevPbrMaterialId.RACING_LIVERY, finish.bodyRgb, finish.paintMetallic, 0.20f, clearcoat = 0.88f, isShell = true) {
          // Top tube + seat tube + steep downtube integrated diamond triangle
          addTube(Vec3(0.46f * ls, 0.45f, 0f), Vec3(-0.22f * ls, 0.22f, 0f), 0.034f, 14)
          addTube(Vec3(-0.22f * ls, 0.26f, 0f), Vec3(-0.18f * ls, -0.22f, 0f), 0.032f, 14)
          // Tapered headtube
          addTube(Vec3(0.49f * ls, 0.50f, 0f), Vec3(0.43f * ls, 0.30f, 0f), 0.042f, 14)
          // Rear Horst-link chainstays & seatstays
          for (zSign in listOf(-1f, 1f)) {
            addTube(Vec3(-0.18f * ls, -0.22f, zSign * 0.075f), Vec3(-0.90f * ls, -0.34f, zSign * 0.105f), 0.020f, 10)
            addTube(Vec3(-0.24f * ls, 0.12f, zSign * 0.068f), Vec3(-0.90f * ls, -0.34f, zSign * 0.105f), 0.017f, 10)
          }
          // Seatpost + Dropper stanchion
          addTube(Vec3(-0.22f * ls, 0.26f, 0f), Vec3(-0.30f * ls, 0.50f, 0f), 0.020f, 12)
          // Cranks + Pedals
          addTube(Vec3(-0.18f * ls, -0.22f, -0.11f), Vec3(-0.18f * ls, -0.22f, 0.11f), 0.020f, 10)
          addTube(Vec3(-0.18f * ls, -0.22f, -0.11f), Vec3(-0.08f * ls, -0.38f, -0.14f), 0.014f, 8)
          addTube(Vec3(-0.18f * ls, -0.22f, 0.11f), Vec3(-0.28f * ls, -0.06f, 0.14f), 0.014f, 8)
          addRoundedBlock(-0.08f * ls, -0.38f, -0.20f, 0.048f, 0.011f, 0.042f, exp = 0.55f)
          addRoundedBlock(-0.28f * ls, -0.06f, 0.20f, 0.048f, 0.011f, 0.042f, exp = 0.55f)
          addCurvedFender(0.94f * ls, -0.34f, wheelR + 0.04f, 56f, 118f, 0.082f)
        }

        // Sleek Recessed Integrated Downtube Battery Pack
        newBucket("battery", "battery", PevPbrMaterialId.BATTERY_PACK, batteryAlloy, 0.65f, 0.34f) {
          addSweptTube(
            listOf(Vec3(0.41f * ls, 0.31f, 0f), Vec3(0.14f * ls, 0.04f, 0f), Vec3(-0.12f * ls, -0.18f, 0f)),
            listOf(0.056f, 0.068f, 0.058f),
            16,
          )
        }

        // Narrow Trail Dropper MTB Saddle
        newBucket("frame_seat", "frame", PevPbrMaterialId.QUILTED_LEATHER, saddleVinyl, 0.03f, 0.78f, isShell = true) {
          addLoftedBodyX(
            stations = listOf(
              LoftStation(-0.42f * ls, 0.52f, 0f, 0.024f, 0.088f, exp = 0.80f),
              LoftStation(-0.30f * ls, 0.51f, 0f, 0.028f, 0.072f, exp = 0.82f),
              LoftStation(-0.18f * ls, 0.52f, 0f, 0.018f, 0.036f, exp = 0.90f),
            ),
            ringSegs = 16,
          )
        }

        // Single-Crown MTB Suspension Fork + Inclined Inline Piggyback Rear Shock
        newBucket("suspension", "suspension", PevPbrMaterialId.BRUSHED_METAL, finish.accentRgb, 0.94f, 0.16f, clearcoat = 0.45f) {
          addTube(Vec3(0.45f * ls, 0.32f, -0.095f), Vec3(0.71f * ls, 0.02f, -0.095f), 0.026f, 14)
          addTube(Vec3(0.45f * ls, 0.32f, 0.095f), Vec3(0.71f * ls, 0.02f, 0.095f), 0.026f, 14)
          addRoundedBlock(0.47f * ls, 0.32f, 0f, 0.040f, 0.018f, 0.122f, exp = 0.55f)
          // Inclined Trunnion Rear Trail Shock under Top Tube
          addTube(Vec3(0.18f * ls, 0.30f, 0f), Vec3(-0.14f * ls, 0.16f, 0f), 0.028f, 12)
          addHelicalSpring(Vec3(0.12f * ls, 0.27f, 0f), Vec3(-0.08f * ls, 0.18f, 0f), 0.038f, 0.007f, 5.0f)
        }
        newBucket("suspension_chrome", "suspension", PevPbrMaterialId.BRUSHED_METAL, anodizedBlack, 0.86f, 0.22f) {
          addTube(Vec3(0.71f * ls, 0.02f, -0.095f), Vec3(0.94f * ls, -0.34f, -0.095f), 0.030f, 14)
          addTube(Vec3(0.71f * ls, 0.02f, 0.095f), Vec3(0.94f * ls, -0.34f, 0.095f), 0.030f, 14)
        }

        // Compact Brose/Shimano Trail Mid-Drive Motor + Derailleur Chainline
        newBucket("motor", "motor", PevPbrMaterialId.BRUSHED_METAL, motorHousing, 0.88f, 0.26f) {
          addTube(Vec3(-0.18f * ls, -0.22f, -0.095f), Vec3(-0.18f * ls, -0.22f, 0.095f), 0.094f, 22)
        }
        newBucket("motor_drive", "motor", PevPbrMaterialId.BRUSHED_METAL, steelChain, 0.92f, 0.22f) {
          addSprocketWithTeeth(-0.18f * ls, -0.22f, -0.105f, 0.068f, 0.010f, 14)
          addSprocketWithTeeth(-0.90f * ls, -0.34f, -0.105f, 0.120f * ws, 0.010f, 20)
          addChainDriveLoop(Vec3(-0.18f * ls, -0.22f, -0.105f), Vec3(-0.90f * ls, -0.34f, -0.105f), 0.068f, 0.120f * ws, -0.105f)
        }
        newBucket("brakes", "brakes", PevPbrMaterialId.CARBON_WEAVE, anodizedBlack, 0.35f, 0.42f) {
          addSweptTube(listOf(Vec3(0.50f * ls, 0.62f, 0.24f), Vec3(0.68f * ls, 0.10f, 0.12f), Vec3(0.94f * ls, -0.30f, 0.11f)), listOf(0.006f, 0.006f, 0.006f), 8)
          addSweptTube(listOf(Vec3(0.50f * ls, 0.62f, -0.24f), Vec3(-0.18f * ls, -0.18f, -0.06f), Vec3(-0.84f * ls, -0.30f, 0.10f)), listOf(0.006f, 0.006f, 0.006f), 8)
        }
        newBucket("controller", "controller", PevPbrMaterialId.BRUSHED_METAL, focHeatsink, 0.90f, 0.22f) {
          addFinnedBlock(0.18f * ls, 0.12f, 0f, 0.075f, 0.042f, 0.065f, 5)
        }
        newBucket("cockpit", "cockpit", PevPbrMaterialId.BRUSHED_METAL, anodizedBlack, 0.88f, 0.24f) {
          addSweptTube(
            listOf(Vec3(0.45f * ls, 0.62f, -0.44f), Vec3(0.50f * ls, 0.62f, -0.16f), Vec3(0.49f * ls, 0.60f, 0f), Vec3(0.50f * ls, 0.62f, 0.16f), Vec3(0.45f * ls, 0.62f, 0.44f)),
            listOf(0.020f, 0.018f, 0.022f, 0.018f, 0.020f),
            12,
          )
          addRoundedBlock(0.46f * ls, 0.44f, 0f, 0.040f, 0.025f, 0.056f, exp = 0.55f)
        }
        newBucket("cockpit_grips", "cockpit", PevPbrMaterialId.TIRE_RUBBER, rubberNeutral, 0.04f, 0.86f) {
          addTube(Vec3(0.49f * ls, 0.62f, 0.28f), Vec3(0.45f * ls, 0.62f, 0.45f), 0.027f, 12)
          addTube(Vec3(0.49f * ls, 0.62f, -0.28f), Vec3(0.45f * ls, 0.62f, -0.45f), 0.027f, 12)
        }
        newBucket("cockpit_tft", "cockpit", PevPbrMaterialId.TFT_SCREEN, tftWhite, 0.08f, 0.08f, floatArrayOf(0.55f, 0.58f, 0.62f), isScreen = true) {
          addQuad(Vec3(0.32f * ls, 0.39f, 0.038f), Vec3(0.32f * ls, 0.39f, -0.038f), Vec3(0.24f * ls, 0.34f, -0.038f), Vec3(0.24f * ls, 0.34f, 0.038f), Vec3(0.5f, 0.86f, 0f), 1f, 1f, wire = false)
        }
      }

      Pev3dBlueprintType.EBIKE_COMPACT_FOLDING -> {
        // Low Step-Through Mono-Tube Utility / Folding Chassis (Lectric XP 3.0 / RadRunner 3 Plus)
        val wheelR = 0.34f * ws
        addDetailedWheel("wheel_rear", -0.84f * ls, -0.42f, 0f, wheelR, 0.22f, "motor", true)
        addDetailedWheel("wheel_front", 0.84f * ls, -0.42f, 0f, wheelR, 0.22f, "brakes", false)

        newBucket("frame", "frame", PevPbrMaterialId.RACING_LIVERY, finish.bodyRgb, finish.paintMetallic, 0.18f, clearcoat = 0.90f, isShell = true) {
          // Deep step-through swept mono-tube spar + central folding hinge knuckle
          addSweptTube(
            listOf(
              Vec3(0.50f * ls, 0.22f, 0f),
              Vec3(0.28f * ls, -0.10f, 0f),
              Vec3(0.02f * ls, -0.20f, 0f),
              Vec3(-0.24f * ls, -0.18f, 0f),
            ),
            listOf(0.056f, 0.062f, 0.062f, 0.056f),
            16,
          )
          // Machined folding center clamp hinge block
          addRoundedBlock(0.10f * ls, -0.16f, 0f, 0.065f, 0.068f, 0.072f, exp = 0.45f)
          // Seat tube + Tall folding stem column + headtube
          addTube(Vec3(-0.22f * ls, -0.20f, 0f), Vec3(-0.32f * ls, 0.40f, 0f), 0.038f, 14)
          addTube(Vec3(0.54f * ls, 0.32f, 0f), Vec3(0.46f * ls, 0.06f, 0f), 0.048f, 14)
          addTube(Vec3(0.54f * ls, 0.32f, 0f), Vec3(0.42f * ls, 0.80f, 0f), 0.032f, 14)
          // Heavy-duty Rear Utility Cargo Rack + Pannier Side Rails
          for (zSign in listOf(-1f, 1f)) {
            addTube(Vec3(-0.26f * ls, -0.18f, zSign * 0.08f), Vec3(-0.84f * ls, -0.42f, zSign * 0.12f), 0.024f, 10)
            addTube(Vec3(-0.30f * ls, 0.12f, zSign * 0.12f), Vec3(-0.84f * ls, 0.10f, zSign * 0.12f), 0.018f, 10)
            addTube(Vec3(-0.84f * ls, -0.42f, zSign * 0.12f), Vec3(-0.84f * ls, 0.10f, zSign * 0.12f), 0.018f, 10)
            addTube(Vec3(-0.56f * ls, -0.30f, zSign * 0.12f), Vec3(-0.56f * ls, 0.11f, zSign * 0.12f), 0.015f, 10)
          }
          addRoundedBlock(-0.58f * ls, 0.12f, 0f, 0.28f * ls, 0.018f, 0.135f, exp = 0.50f)
          // Utility Crankset
          addTube(Vec3(-0.20f * ls, -0.20f, -0.11f), Vec3(-0.20f * ls, -0.20f, 0.11f), 0.020f, 10)
          addTube(Vec3(-0.20f * ls, -0.20f, -0.11f), Vec3(-0.10f * ls, -0.36f, -0.14f), 0.014f, 8)
          addTube(Vec3(-0.20f * ls, -0.20f, 0.11f), Vec3(-0.30f * ls, -0.04f, 0.14f), 0.014f, 8)
          addRoundedBlock(-0.10f * ls, -0.36f, -0.20f, 0.048f, 0.011f, 0.042f, exp = 0.55f)
          addRoundedBlock(-0.30f * ls, -0.04f, 0.20f, 0.048f, 0.011f, 0.042f, exp = 0.55f)
          addCurvedFender(0.84f * ls, -0.42f, wheelR + 0.05f, 45f, 130f, 0.118f)
          addCurvedFender(-0.84f * ls, -0.42f, wheelR + 0.05f, 65f, 170f, 0.118f)
        }

        newBucket("frame_seat", "frame", PevPbrMaterialId.QUILTED_LEATHER, saddleVinyl, 0.03f, 0.78f, isShell = true) {
          addLoftedBodyX(
            stations = listOf(
              LoftStation(-0.46f * ls, 0.42f, 0f, 0.044f, 0.125f, exp = 0.76f),
              LoftStation(-0.32f * ls, 0.41f, 0f, 0.048f, 0.135f, exp = 0.74f),
              LoftStation(-0.18f * ls, 0.42f, 0f, 0.034f, 0.075f, exp = 0.85f),
            ),
            ringSegs = 18,
          )
        }

        // Rear-of-seatpost Slide-out Battery + Compact Controller
        newBucket("battery", "battery", PevPbrMaterialId.BATTERY_PACK, batteryAlloy, 0.68f, 0.34f) {
          addRoundedBlock(-0.16f * ls, 0.02f, 0f, 0.085f, 0.21f, 0.085f, exp = 0.48f)
        }
        newBucket("controller", "controller", PevPbrMaterialId.BRUSHED_METAL, focHeatsink, 0.90f, 0.22f) {
          addFinnedBlock(0.14f * ls, -0.18f, 0f, 0.085f, 0.052f, 0.095f, 6)
        }
        newBucket("suspension", "suspension", PevPbrMaterialId.BRUSHED_METAL, finish.accentRgb, 0.92f, 0.18f) {
          addTube(Vec3(0.50f * ls, 0.10f, -0.11f), Vec3(0.84f * ls, -0.42f, -0.11f), 0.028f, 14)
          addTube(Vec3(0.50f * ls, 0.10f, 0.11f), Vec3(0.84f * ls, -0.42f, 0.11f), 0.028f, 14)
        }
        newBucket("motor", "motor", PevPbrMaterialId.BRUSHED_METAL, motorHousing, 0.90f, 0.24f) {
          addDisc(-0.84f * ls, -0.42f, 0f, wheelR * 0.44f, 0.18f, 28)
        }
        newBucket("motor_drive", "motor", PevPbrMaterialId.BRUSHED_METAL, steelChain, 0.92f, 0.22f) {
          addSprocketWithTeeth(-0.20f * ls, -0.20f, -0.115f, 0.075f, 0.010f, 16)
          addSprocketWithTeeth(-0.84f * ls, -0.42f, -0.115f, 0.105f * ws, 0.010f, 18)
          addChainDriveLoop(Vec3(-0.20f * ls, -0.20f, -0.115f), Vec3(-0.84f * ls, -0.42f, -0.115f), 0.075f, 0.105f * ws, -0.115f)
        }
        newBucket("brakes", "brakes", PevPbrMaterialId.CARBON_WEAVE, anodizedBlack, 0.35f, 0.42f) {
          addSweptTube(listOf(Vec3(0.42f * ls, 0.78f, 0.20f), Vec3(0.62f * ls, 0.02f, 0.12f), Vec3(0.84f * ls, -0.36f, 0.12f)), listOf(0.006f, 0.006f, 0.006f), 8)
          addSweptTube(listOf(Vec3(0.42f * ls, 0.78f, -0.20f), Vec3(0.20f * ls, -0.12f, -0.04f), Vec3(-0.78f * ls, -0.36f, 0.12f)), listOf(0.006f, 0.006f, 0.006f), 8)
        }
        newBucket("cockpit", "cockpit", PevPbrMaterialId.BRUSHED_METAL, anodizedBlack, 0.88f, 0.24f) {
          addSweptTube(
            listOf(Vec3(0.39f * ls, 0.82f, -0.38f), Vec3(0.43f * ls, 0.82f, -0.14f), Vec3(0.42f * ls, 0.80f, 0f), Vec3(0.43f * ls, 0.82f, 0.14f), Vec3(0.39f * ls, 0.82f, 0.38f)),
            listOf(0.021f, 0.019f, 0.024f, 0.019f, 0.021f),
            12,
          )
          addRoundedBlock(0.45f * ls, 0.86f, 0f, 0.044f, 0.036f, 0.078f, exp = 0.55f)
        }
        newBucket("cockpit_grips", "cockpit", PevPbrMaterialId.TIRE_RUBBER, rubberNeutral, 0.04f, 0.86f) {
          addTube(Vec3(0.42f * ls, 0.82f, 0.22f), Vec3(0.39f * ls, 0.82f, 0.39f), 0.028f, 12)
          addTube(Vec3(0.42f * ls, 0.82f, -0.22f), Vec3(0.39f * ls, 0.82f, -0.39f), 0.028f, 12)
        }
        newBucket("cockpit_tft", "cockpit", PevPbrMaterialId.TFT_SCREEN, tftWhite, 0.08f, 0.08f, floatArrayOf(0.55f, 0.58f, 0.62f), isScreen = true) {
          addQuad(Vec3(0.495f * ls, 0.845f, 0.068f), Vec3(0.495f * ls, 0.845f, -0.068f), Vec3(0.472f * ls, 0.895f, -0.068f), Vec3(0.472f * ls, 0.895f, 0.068f), Vec3(0.92f, 0.38f, 0f), 1f, 1f, wire = false)
        }
      }

      else -> {
        // EMOTO_MX_DIRT, EMOTO_STREET_NAKED & EBIKE_SCRAMBLER_MOPED
        val isStreet = bp == Pev3dBlueprintType.EMOTO_STREET_NAKED
        val isScrambler = bp == Pev3dBlueprintType.EBIKE_SCRAMBLER_MOPED
        val wheelR = when {
          isScrambler -> 0.41f * ws
          isStreet -> 0.38f * ws
          else -> 0.42f * ws
        }
        val tireW = if (isScrambler) 0.26f else if (isStreet) 0.24f else 0.20f
        addDetailedWheel("wheel_rear", -0.92f * ls, -0.34f, 0f, wheelR, tireW, if (isScrambler) "motor" else "frame", isScrambler)
        addDetailedWheel("wheel_front", 0.94f * ls, -0.34f, 0f, wheelR, if (isScrambler) 0.25f else 0.19f, "brakes", false)

        newBucket(
          nodeName = "frame",
          partKey = "frame",
          texId = PevPbrMaterialId.CARBON_WEAVE,
          baseRgb = matteCarbon,
          metallic = 0.34f,
          roughness = 0.26f,
          clearcoat = 0.85f,
          isShell = true,
        ) {
          for (zSign in listOf(-1f, 1f)) {
            if (isScrambler) {
              // Vintage twin-bar horizontal café/scrambler top frame (Super73 / Onyx RCR)
              addTube(Vec3(0.47f * ls, 0.42f, zSign * 0.055f), Vec3(-0.72f * ls, 0.30f, zSign * 0.088f), 0.028f, 12)
              addTube(Vec3(0.43f * ls, 0.28f, zSign * 0.065f), Vec3(-0.22f * ls, -0.15f, zSign * 0.105f), 0.026f, 12)
            } else {
              addSweptTube(
                listOf(
                  Vec3(0.47f * ls, 0.40f, zSign * 0.042f),
                  Vec3(0.18f * ls, 0.32f, zSign * 0.105f),
                  Vec3(-0.18f * ls, 0.14f, zSign * 0.115f),
                  Vec3(-0.26f * ls, -0.15f, zSign * 0.112f),
                ),
                listOf(0.036f, 0.042f, 0.038f, 0.036f),
                14,
              )
            }
            addSweptTube(
              listOf(
                Vec3(-0.16f * ls, 0.22f, zSign * 0.082f),
                Vec3(-0.46f * ls, 0.26f, zSign * 0.088f),
                Vec3(-0.74f * ls, 0.27f, zSign * 0.055f),
              ),
              listOf(0.024f, 0.022f, 0.018f),
              10,
            )
            addTube(Vec3(-0.24f * ls, -0.12f, zSign * 0.105f), Vec3(-0.58f * ls, 0.25f, zSign * 0.082f), 0.018f, 10)
            addSweptTube(
              listOf(Vec3(-0.24f * ls, -0.15f, zSign * 0.115f), Vec3(-0.58f * ls, -0.25f, zSign * 0.125f), Vec3(-0.92f * ls, -0.34f, zSign * 0.118f)),
              listOf(0.042f, 0.035f, 0.028f),
              14,
            )
          }
          addTube(Vec3(0.49f * ls, 0.50f, 0f), Vec3(0.43f * ls, 0.30f, 0f), 0.046f, 16)
          addSweptTube(
            listOf(Vec3(0.45f * ls, 0.34f, 0f), Vec3(0.32f * ls, 0.06f, 0f), Vec3(0.06f * ls, -0.18f, 0f), Vec3(-0.20f * ls, -0.18f, 0f)),
            listOf(0.044f, 0.042f, 0.040f, 0.040f),
            16,
          )
          addLoftedBodyX(
            stations = listOf(
              LoftStation(-0.22f * ls, -0.22f, 0f, 0.016f, 0.10f, exp = 0.60f),
              LoftStation(0.08f * ls, -0.23f, 0f, 0.018f, 0.12f, exp = 0.55f),
              LoftStation(0.32f * ls, -0.12f, 0f, 0.016f, 0.10f, exp = 0.62f),
            ),
            ringSegs = 14,
          )
          if (isScrambler) {
            // Cranks & Pedals + café fenders
            addTube(Vec3(-0.24f * ls, -0.22f, -0.12f), Vec3(-0.24f * ls, -0.22f, 0.12f), 0.022f, 12)
            addTube(Vec3(-0.24f * ls, -0.22f, -0.12f), Vec3(-0.14f * ls, -0.38f, -0.15f), 0.015f, 10)
            addTube(Vec3(-0.24f * ls, -0.22f, 0.12f), Vec3(-0.34f * ls, -0.08f, 0.15f), 0.015f, 10)
            addRoundedBlock(-0.14f * ls, -0.38f, -0.22f, 0.052f, 0.013f, 0.046f, exp = 0.55f)
            addRoundedBlock(-0.34f * ls, -0.08f, 0.22f, 0.052f, 0.013f, 0.046f, exp = 0.55f)
            addCurvedFender(0.94f * ls, -0.34f, wheelR + 0.06f, 52f, 125f, 0.132f)
            addCurvedFender(-0.92f * ls, -0.34f, wheelR + 0.05f, 70f, 170f, 0.135f)
          } else {
            // CNC Dropped Footpegs + High MX Beak (Dirt) or Low Street Tire Huggers (Street Naked)
            addTube(Vec3(-0.30f * ls, -0.26f, -0.12f), Vec3(-0.30f * ls, -0.26f, -0.27f), 0.016f, 10)
            addTube(Vec3(-0.30f * ls, -0.26f, 0.12f), Vec3(-0.30f * ls, -0.26f, 0.27f), 0.016f, 10)
            addRoundedBlock(-0.30f * ls, -0.26f, -0.23f, 0.042f, 0.011f, 0.050f, exp = 0.50f)
            addRoundedBlock(-0.30f * ls, -0.26f, 0.23f, 0.042f, 0.011f, 0.050f, exp = 0.50f)
            if (isStreet) {
              addCurvedFender(0.94f * ls, -0.34f, wheelR + 0.045f, 52f, 122f, 0.118f)
              addCurvedFender(-0.92f * ls, -0.34f, wheelR + 0.045f, 95f, 175f, 0.125f)
            } else {
              // High-mounted MX front dirt beak
              addCurvedFender(0.84f * ls, -0.12f, wheelR + 0.05f, 45f, 118f, 0.115f)
              addCurvedFender(-0.92f * ls, -0.34f, wheelR + 0.06f, 70f, 165f, 0.125f)
            }
          }
          addTube(Vec3(-0.70f * ls, 0.25f, 0f), Vec3(-0.88f * ls, 0.15f, 0f), 0.014f, 8)
        }

        // OEM Clearcoat Bodywork Fairings differentiated by chassis class
        newBucket("frame_graphics", "frame", PevPbrMaterialId.RACING_LIVERY, finish.bodyRgb, finish.paintMetallic, 0.15f, clearcoat = 0.96f, isShell = true) {
          if (isStreet) {
            // Muscular wide sculpted café/street motorcycle fuel tank & sculpted lower belly pan cowl
            addLoftedBodyX(
              stations = listOf(
                LoftStation(-0.14f * ls, 0.30f, 0f, 0.065f, 0.125f, exp = 0.78f, topFlat = 0.92f),
                LoftStation(0.10f * ls, 0.41f, 0f, 0.125f, 0.205f, exp = 0.72f, topFlat = 0.90f),
                LoftStation(0.32f * ls, 0.42f, 0f, 0.120f, 0.198f, exp = 0.72f, topFlat = 0.92f),
                LoftStation(0.48f * ls, 0.37f, 0f, 0.072f, 0.130f, exp = 0.80f),
              ),
              ringSegs = 22,
              uTile = 1.6f,
              vTile = 1.4f,
            )
            // Aerodynamic lower belly pan fairing
            addLoftedBodyX(
              stations = listOf(
                LoftStation(-0.16f * ls, -0.22f, 0f, 0.052f, 0.135f, exp = 0.65f),
                LoftStation(0.14f * ls, -0.20f, 0f, 0.068f, 0.150f, exp = 0.62f),
                LoftStation(0.36f * ls, -0.10f, 0f, 0.058f, 0.120f, exp = 0.70f),
              ),
              ringSegs = 16,
            )
          } else if (isScrambler) {
            // Vintage retro café/moped top-tube teardrop tank shroud (Onyx RCR / Super73 style)
            addLoftedBodyX(
              stations = listOf(
                LoftStation(-0.02f * ls, 0.44f, 0f, 0.052f, 0.115f, exp = 0.78f),
                LoftStation(0.20f * ls, 0.45f, 0f, 0.072f, 0.142f, exp = 0.72f),
                LoftStation(0.40f * ls, 0.44f, 0f, 0.055f, 0.112f, exp = 0.80f),
              ),
              ringSegs = 20,
            )
          } else {
            // Sharp MX Dirt Tank + Number Plate Shrouds
            addLoftedBodyX(
              stations = listOf(
                LoftStation(-0.14f * ls, 0.30f, 0f, 0.058f, 0.115f, exp = 0.78f, topFlat = 0.92f),
                LoftStation(0.06f * ls, 0.36f, 0f, 0.092f, 0.155f, exp = 0.74f, topFlat = 0.90f),
                LoftStation(0.28f * ls, 0.38f, 0f, 0.098f, 0.162f, exp = 0.72f, topFlat = 0.92f),
                LoftStation(0.45f * ls, 0.37f, 0f, 0.062f, 0.118f, exp = 0.80f),
                LoftStation(0.52f * ls, 0.36f, 0f, 0.032f, 0.068f, exp = 0.90f),
              ),
              ringSegs = 22,
              uTile = 1.6f,
              vTile = 1.4f,
            )
            // High MX Front Number Plate Fairing
            addSculptedPod(0.57f * ls, 0.56f, 0f, 0.034f, 0.105f, 0.100f, exp = 0.65f, rings = 8, segs = 16)
          }
          for (zSign in listOf(-1f, 1f)) {
            addLoftedBodyX(
              stations = listOf(
                LoftStation(-0.04f * ls, 0.20f, zSign * 0.158f, 0.065f, 0.018f, exp = 0.75f),
                LoftStation(0.18f * ls, 0.24f, zSign * 0.174f, 0.115f, 0.026f, exp = 0.68f),
                LoftStation(0.38f * ls, 0.26f, zSign * 0.168f, 0.095f, 0.022f, exp = 0.72f),
                LoftStation(0.50f * ls, 0.29f, zSign * 0.130f, 0.052f, 0.014f, exp = 0.82f),
              ),
              ringSegs = 16,
            )
            addLoftedBodyX(
              stations = listOf(
                LoftStation(-0.76f * ls, 0.27f, zSign * 0.088f, 0.032f, 0.016f, exp = 0.78f),
                LoftStation(-0.56f * ls, 0.25f, zSign * 0.138f, 0.070f, 0.024f, exp = 0.70f),
                LoftStation(-0.34f * ls, 0.23f, zSign * 0.132f, 0.064f, 0.020f, exp = 0.74f),
              ),
              ringSegs = 14,
            )
          }
          addLoftedBodyX(
            stations = listOf(
              LoftStation(-0.84f * ls, 0.28f, 0f, 0.024f, 0.072f, exp = 0.78f),
              LoftStation(-0.70f * ls, 0.29f, 0f, 0.046f, 0.118f, exp = 0.70f),
              LoftStation(-0.52f * ls, 0.29f, 0f, 0.054f, 0.130f, exp = 0.68f),
            ),
            ringSegs = 16,
          )
        }

        // Bench Saddle (Scrambler flat long quilted vs MX stepped vs Street sport sculpted)
        newBucket("frame_seat", "frame", PevPbrMaterialId.QUILTED_LEATHER, saddleVinyl, 0.03f, 0.74f, isShell = true) {
          if (isScrambler) {
            addLoftedBodyX(
              stations = listOf(
                LoftStation(-0.74f * ls, 0.42f, 0f, 0.050f, 0.125f, exp = 0.72f, topFlat = 0.90f),
                LoftStation(-0.42f * ls, 0.42f, 0f, 0.054f, 0.132f, exp = 0.70f, topFlat = 0.90f),
                LoftStation(-0.10f * ls, 0.42f, 0f, 0.048f, 0.118f, exp = 0.74f, topFlat = 0.90f),
              ),
              ringSegs = 20,
            )
          } else {
            addLoftedBodyX(
              stations = listOf(
                LoftStation(-0.70f * ls, 0.34f, 0f, 0.036f, 0.098f, exp = 0.85f, topFlat = 0.85f),
                LoftStation(-0.54f * ls, 0.35f, 0f, 0.058f, 0.128f, exp = 0.76f, topFlat = 0.82f),
                LoftStation(-0.34f * ls, 0.33f, 0f, 0.056f, 0.132f, exp = 0.76f, topFlat = 0.80f),
                LoftStation(-0.16f * ls, 0.33f, 0f, 0.050f, 0.114f, exp = 0.80f, topFlat = 0.85f),
                LoftStation(-0.02f * ls, 0.36f, 0f, 0.034f, 0.088f, exp = 0.88f, topFlat = 0.90f),
              ),
              ringSegs = 20,
              uTile = 2.0f,
              vTile = 1.6f,
            )
          }
        }

        newBucket("suspension", "suspension", PevPbrMaterialId.BRUSHED_METAL, finish.accentRgb, 0.94f, 0.16f, clearcoat = 0.45f) {
          addTube(Vec3(0.47f * ls, 0.59f, -0.118f), Vec3(0.70f * ls, 0.14f, -0.118f), 0.038f, 16)
          addTube(Vec3(0.47f * ls, 0.59f, 0.118f), Vec3(0.70f * ls, 0.14f, 0.118f), 0.038f, 16)
          addRoundedBlock(0.50f * ls, 0.52f, 0f, 0.048f, 0.020f, 0.152f, exp = 0.55f)
          addRoundedBlock(0.57f * ls, 0.37f, 0f, 0.048f, 0.020f, 0.152f, exp = 0.55f)
          addHelicalSpring(Vec3(-0.30f * ls, 0.18f, 0f), Vec3(-0.54f * ls, -0.16f, 0f), 0.052f, 0.010f, 6.5f)
        }
        newBucket("suspension_chrome", "suspension", PevPbrMaterialId.BRUSHED_METAL, cncAluminum, 0.98f, 0.10f) {
          addTube(Vec3(0.70f * ls, 0.14f, -0.118f), Vec3(0.94f * ls, -0.34f, -0.118f), 0.027f, 16)
          addTube(Vec3(0.70f * ls, 0.14f, 0.118f), Vec3(0.94f * ls, -0.34f, 0.118f), 0.027f, 16)
          addTube(Vec3(-0.26f * ls, 0.24f, 0f), Vec3(-0.58f * ls, -0.22f, 0f), 0.022f, 12)
        }
        newBucket("suspension_guards", "suspension", PevPbrMaterialId.CARBON_WEAVE, anodizedBlack, 0.28f, 0.32f) {
          addSculptedPod(0.84f * ls, -0.12f, -0.146f, 0.038f, 0.135f, 0.018f, exp = 0.65f)
          addSculptedPod(0.84f * ls, -0.12f, 0.146f, 0.038f, 0.135f, 0.018f, exp = 0.65f)
        }

        newBucket("battery", "battery", PevPbrMaterialId.BATTERY_PACK, batteryAlloy, 0.68f, 0.34f) {
          addLoftedBodyX(
            stations = listOf(
              LoftStation(-0.14f * ls, 0.10f, 0f, 0.17f, 0.118f, exp = 0.45f),
              LoftStation(0.08f * ls, 0.13f, 0f, 0.20f, 0.132f, exp = 0.42f),
              LoftStation(0.28f * ls, 0.15f, 0f, 0.17f, 0.122f, exp = 0.45f),
            ),
            ringSegs = 16,
            uTile = 1.6f,
            vTile = 1.6f,
          )
        }

        newBucket("motor", "motor", PevPbrMaterialId.BRUSHED_METAL, motorHousing, 0.88f, 0.26f) {
          addTube(Vec3(-0.14f * ls, -0.22f, -0.142f), Vec3(-0.14f * ls, -0.22f, 0.142f), 0.128f, 24)
          addFinnedBlock(-0.14f * ls, -0.22f, 0f, 0.105f, 0.115f, 0.125f, 7)
        }
        newBucket("motor_drive", "motor", PevPbrMaterialId.BRUSHED_METAL, steelChain, 0.92f, 0.22f) {
          addSweptTube(
            listOf(Vec3(0.28f * ls, -0.02f, 0.06f), Vec3(0.12f * ls, -0.14f, 0.11f), Vec3(-0.10f * ls, -0.20f, 0.12f)),
            listOf(0.014f, 0.014f, 0.014f),
            10,
          )
          val sprocketZ = -0.140f
          addSprocketWithTeeth(-0.14f * ls, -0.22f, sprocketZ, 0.072f, 0.012f, 16)
          addSprocketWithTeeth(-0.92f * ls, -0.34f, sprocketZ, 0.162f * ws, 0.012f, 26)
          addChainDriveLoop(Vec3(-0.14f * ls, -0.22f, sprocketZ), Vec3(-0.92f * ls, -0.34f, sprocketZ), 0.072f, 0.162f * ws, sprocketZ)
        }

        newBucket("brakes", "brakes", PevPbrMaterialId.CARBON_WEAVE, anodizedBlack, 0.35f, 0.42f) {
          addSweptTube(
            listOf(Vec3(0.50f * ls, 0.64f, 0.25f), Vec3(0.62f * ls, 0.32f, 0.16f), Vec3(0.76f * ls, 0.02f, 0.14f), Vec3(0.98f * ls, -0.28f, 0.14f)),
            listOf(0.007f, 0.007f, 0.007f, 0.007f),
            8,
          )
          addSweptTube(
            listOf(Vec3(0.50f * ls, 0.64f, -0.25f), Vec3(0.20f * ls, 0.28f, -0.09f), Vec3(-0.34f * ls, -0.16f, 0.12f), Vec3(-0.82f * ls, -0.26f, 0.14f)),
            listOf(0.007f, 0.007f, 0.007f, 0.007f),
            8,
          )
        }

        newBucket("controller", "controller", PevPbrMaterialId.BRUSHED_METAL, focHeatsink, 0.90f, 0.22f, clearcoat = 0.30f) {
          addFinnedBlock(0.34f * ls, -0.02f, 0f, 0.098f, 0.118f, 0.110f, 8)
        }

        newBucket("cockpit", "cockpit", PevPbrMaterialId.BRUSHED_METAL, anodizedBlack, 0.88f, 0.24f) {
          val barY = if (isStreet) 0.58f else 0.66f
          val barZ = if (isStreet) 0.38f else 0.45f
          addSweptTube(
            listOf(Vec3(0.45f * ls, barY, -barZ), Vec3(0.51f * ls, barY - 0.01f, -0.18f), Vec3(0.50f * ls, barY - 0.02f, 0f), Vec3(0.51f * ls, barY - 0.01f, 0.18f), Vec3(0.45f * ls, barY, barZ)),
            listOf(0.023f, 0.020f, 0.025f, 0.020f, 0.023f),
            14,
          )
          addTube(Vec3(0.51f * ls, barY, -0.26f), Vec3(0.58f * ls, barY - 0.02f, -0.37f), 0.008f, 8)
          addTube(Vec3(0.51f * ls, barY, 0.26f), Vec3(0.58f * ls, barY - 0.02f, 0.37f), 0.008f, 8)
          addSculptedPod(0.51f * ls, barY + 0.01f, -0.25f, 0.022f, 0.016f, 0.026f, exp = 0.65f)
          addSculptedPod(0.51f * ls, barY + 0.01f, 0.25f, 0.022f, 0.016f, 0.026f, exp = 0.65f)
          addRoundedBlock(0.54f * ls, barY + 0.02f, 0f, 0.052f, 0.038f, 0.094f, exp = 0.55f)
          addTube(Vec3(0.45f * ls, barY, barZ - 0.01f), Vec3(0.43f * ls, barY + 0.11f, barZ + 0.03f), 0.009f, 8)
          addTube(Vec3(0.45f * ls, barY, -(barZ - 0.01f)), Vec3(0.43f * ls, barY + 0.11f, -(barZ + 0.03f)), 0.009f, 8)
          addSculptedPod(0.42f * ls, barY + 0.13f, barZ + 0.05f, 0.018f, 0.036f, 0.056f, exp = 0.82f)
          addSculptedPod(0.42f * ls, barY + 0.13f, -(barZ + 0.05f), 0.018f, 0.036f, 0.056f, exp = 0.82f)
        }
        newBucket("cockpit_grips", "cockpit", PevPbrMaterialId.TIRE_RUBBER, rubberNeutral, 0.04f, 0.86f) {
          val barY = if (isStreet) 0.58f else 0.66f
          val barZ = if (isStreet) 0.39f else 0.46f
          addTube(Vec3(0.495f * ls, barY - 0.005f, 0.26f), Vec3(0.45f * ls, barY, barZ), 0.030f, 12)
          addTube(Vec3(0.495f * ls, barY - 0.005f, -0.26f), Vec3(0.45f * ls, barY, -barZ), 0.030f, 12)
        }
        newBucket("cockpit_tft", "cockpit", PevPbrMaterialId.TFT_SCREEN, tftWhite, 0.08f, 0.08f, floatArrayOf(0.55f, 0.58f, 0.62f), isScreen = true) {
          val barY = if (isStreet) 0.58f else 0.66f
          addQuad(
            Vec3(0.580f * ls, barY - 0.005f, 0.082f),
            Vec3(0.580f * ls, barY - 0.005f, -0.082f),
            Vec3(0.556f * ls, barY + 0.055f, -0.082f),
            Vec3(0.556f * ls, barY + 0.055f, 0.082f),
            Vec3(0.92f, 0.38f, 0f),
            1f,
            1f,
            wire = false,
          )
        }
      }
    }

    // Sculpted Round/Oval Bi-LED Projector Headlight Pod, Flush Tapered LED Taillight & Teardrop Turn Signals
    val hlX = when (bp) {
      Pev3dBlueprintType.EUC_ARMORED_ROLLCAGE,
      Pev3dBlueprintType.EUC_AERO_TRAIL -> 0.37f
      Pev3dBlueprintType.HYDROFOIL_EFOIL -> 1.04f
      Pev3dBlueprintType.EBIKE_COMPACT_FOLDING -> 0.56f * ls
      Pev3dBlueprintType.EBIKE_MOUNTAIN_FULLSUSP -> 0.58f * ls
      else -> 0.64f * ls
    }
    val hlY = when (bp) {
      Pev3dBlueprintType.EUC_ARMORED_ROLLCAGE,
      Pev3dBlueprintType.EUC_AERO_TRAIL -> 0.28f
      Pev3dBlueprintType.HYDROFOIL_EFOIL -> 0.19f
      Pev3dBlueprintType.SCOOTER_DUAL_STEM_OFFROAD,
      Pev3dBlueprintType.SCOOTER_TUBULAR_RACING,
      Pev3dBlueprintType.SCOOTER_MONOCOQUE_AERO -> 0.66f
      Pev3dBlueprintType.EBIKE_COMPACT_FOLDING -> 0.22f
      Pev3dBlueprintType.EBIKE_MOUNTAIN_FULLSUSP -> 0.50f
      Pev3dBlueprintType.EBIKE_SCRAMBLER_MOPED -> 0.48f
      else -> 0.44f
    }
    val tailX = when (bp) {
      Pev3dBlueprintType.EUC_ARMORED_ROLLCAGE,
      Pev3dBlueprintType.EUC_AERO_TRAIL -> -0.37f
      Pev3dBlueprintType.HYDROFOIL_EFOIL -> -1.03f
      Pev3dBlueprintType.EBIKE_COMPACT_FOLDING -> -0.84f * ls
      else -> -0.82f * ls
    }
    val tailY = when (bp) {
      Pev3dBlueprintType.EUC_ARMORED_ROLLCAGE,
      Pev3dBlueprintType.EUC_AERO_TRAIL -> 0.25f
      Pev3dBlueprintType.HYDROFOIL_EFOIL -> 0.16f
      Pev3dBlueprintType.SCOOTER_DUAL_STEM_OFFROAD,
      Pev3dBlueprintType.SCOOTER_TUBULAR_RACING,
      Pev3dBlueprintType.SCOOTER_MONOCOQUE_AERO -> -0.17f
      Pev3dBlueprintType.EBIKE_COMPACT_FOLDING -> 0.12f
      else -> 0.27f
    }

    // Sculpted Cylindrical/Bullet Anodized Headlight Bucket + Taillight Housing (NO blocky cubes!)
    newBucket("cockpit_bezel", "cockpit", PevPbrMaterialId.BRUSHED_METAL, anodizedBlack, 0.92f, 0.20f, clearcoat = 0.45f) {
      addLoftedBodyX(
        stations = listOf(
          LoftStation(hlX - 0.078f, hlY, 0f, 0.044f, 0.048f, exp = 1.0f),
          LoftStation(hlX - 0.032f, hlY, 0f, 0.072f, 0.082f, exp = 0.92f),
          LoftStation(hlX + 0.016f, hlY, 0f, 0.076f, 0.086f, exp = 0.92f),
        ),
        ringSegs = 24,
        capStart = true,
        capEnd = false,
      )
      // Tapered rear taillight housing bezel
      addSculptedPod(tailX + 0.014f, tailY, 0f, 0.030f, 0.032f, 0.094f, exp = 0.68f, rings = 8, segs = 16)
    }
    // Crisp Optical Glass Bi-LED Projector Lens Face
    newBucket(
      nodeName = "cockpit_light",
      partKey = "cockpit",
      texId = PevPbrMaterialId.HEADLIGHT_OPTIC,
      baseRgb = floatArrayOf(1.0f, 1.0f, 1.0f),
      metallic = 0.10f,
      roughness = 0.06f,
      emissiveRgb = if (lightsOn) floatArrayOf(2.4f, 2.5f, 2.7f) else floatArrayOf(0.05f, 0.05f, 0.06f),
      clearcoat = 1.0f,
      useEmissiveTexture = true,
    ) {
      addXFacingLensDisc(hlX + 0.017f, hlY, 0f, 0.070f, 0.078f, faceDirX = 1f, segs = 28)
    }

    // Flush Curved Ruby LED Taillight Lens
    newBucket(
      nodeName = "frame_tail",
      partKey = "frame",
      texId = PevPbrMaterialId.TAILLIGHT_OPTIC,
      baseRgb = floatArrayOf(1.0f, 0.92f, 0.92f),
      metallic = 0.12f,
      roughness = 0.07f,
      emissiveRgb = if (lightsOn) floatArrayOf(2.2f, 0.24f, 0.20f) else floatArrayOf(0.06f, 0.01f, 0.01f),
      clearcoat = 1.0f,
      useEmissiveTexture = true,
    ) {
      addXFacingLensDisc(tailX - 0.015f, tailY, 0f, 0.028f, 0.086f, faceDirX = -1f, segs = 24)
    }

    // 4-Corner Sculpted Teardrop Bullet Turn Signals on Slim CNC Stalks
    val fSigX = hlX - 0.03f
    val fSigY = if (archetype == Pev3dArchetype.SCOOTER) hlY + 0.14f else hlY + 0.02f
    val rSigX = tailX + 0.02f
    val rSigY = tailY - 0.01f
    val sigZSpan = if (archetype == Pev3dArchetype.EUC) 0.34f else 0.18f

    fun addTurnSignalPod(nodeName: String, partKey: String, sx: Float, sy: Float, zBase: Float, zTip: Float) {
      newBucket("${nodeName}_stalk", partKey, PevPbrMaterialId.BRUSHED_METAL, anodizedBlack, 0.90f, 0.22f) {
        addTube(Vec3(sx, sy, zBase), Vec3(sx, sy, zTip), 0.009f, 8)
      }
      newBucket(
        nodeName = nodeName,
        partKey = partKey,
        texId = PevPbrMaterialId.AMBER_SIGNAL_OPTIC,
        baseRgb = floatArrayOf(1.0f, 0.72f, 0.18f),
        metallic = 0.10f,
        roughness = 0.07f,
        emissiveRgb = floatArrayOf(0.08f, 0.04f, 0.01f),
        clearcoat = 1.0f,
        useEmissiveTexture = true,
      ) {
        addSculptedPod(sx, sy, zTip, 0.026f, 0.017f, 0.034f, exp = 0.88f, rings = 8, segs = 14)
      }
    }

    addTurnSignalPod("signal_left_front", "cockpit", fSigX, fSigY, -0.10f, -sigZSpan - 0.06f)
    addTurnSignalPod("signal_right_front", "cockpit", fSigX, fSigY, 0.10f, sigZSpan + 0.06f)
    addTurnSignalPod("signal_left_rear", "frame", rSigX, rSigY, -0.10f, -sigZSpan - 0.05f)
    addTurnSignalPod("signal_right_rear", "frame", rSigX, rSigY, 0.10f, sigZSpan + 0.05f)

    return RawSceneBuckets(buckets, wheelAxleCenters, explodeDirs)
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
      val alpha = if (isXrayTrans) 0.22 else 1.0
      val pbr = JSONObject()
        .put(
          "baseColorFactor",
          JSONArray()
            .put(if (isXrayTrans) 0.14 else srgbToLinear(mk.baseRgb[0]))
            .put(if (isXrayTrans) 0.62 else srgbToLinear(mk.baseRgb[1]))
            .put(if (isXrayTrans) 0.85 else srgbToLinear(mk.baseRgb[2]))
            .put(alpha),
        )
        .put("baseColorTexture", JSONObject().put("index", texSet.albedo))
        .put("metallicFactor", if (isXrayTrans) 0.15 else mk.metallic.toDouble())
        .put("roughnessFactor", if (isXrayTrans) 0.18 else mk.roughness.toDouble())
        .put("metallicRoughnessTexture", JSONObject().put("index", texSet.mr))

      val mat = JSONObject()
        .put("name", "mat_${mk.partKey}_$bIdx")
        .put("pbrMetallicRoughness", pbr)
        .put("normalTexture", JSONObject().put("index", texSet.normal).put("scale", 1.15))
        .put("occlusionTexture", JSONObject().put("index", texSet.occlusion).put("strength", 0.80))
        .put("doubleSided", isXrayTrans || mk.partKey == "frame")

      if (isXrayTrans) {
        mat.put("alphaMode", "BLEND")
        mat.put("emissiveFactor", JSONArray().put(0.06).put(0.32).put(0.48))
      } else if (mk.isScreen || mk.useEmissiveTexture) {
        mat.put("emissiveTexture", JSONObject().put("index", texSet.albedo))
        mat.put(
          "emissiveFactor",
          JSONArray()
            .put(mk.emissiveRgb[0].coerceAtLeast(if (mk.isScreen) 0.85f else 0.04f).toDouble())
            .put(mk.emissiveRgb[1].coerceAtLeast(if (mk.isScreen) 0.85f else 0.02f).toDouble())
            .put(mk.emissiveRgb[2].coerceAtLeast(if (mk.isScreen) 0.85f else 0.01f).toDouble()),
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
              .put("clearcoatRoughnessFactor", 0.07),
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
      .put("asset", JSONObject().put("version", "2.0").put("generator", "VoltTrail-PBR-GLB"))
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
