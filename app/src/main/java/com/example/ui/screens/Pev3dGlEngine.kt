package com.example.ui.screens

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal enum class PevTexKind {
  CARBON,
  METAL,
  TIRE,
  BATTERY,
  ROTOR,
  TFT,
  FLOOR,
  HEADLIGHT,
  TAILLIGHT,
  AMBER_SIGNAL,
}

private class GlMeshPart(
  val partKey: String,
  val texKind: PevTexKind,
  val baseRgb: FloatArray,
  val metallic: Float,
  val roughness: Float,
  val emissive: Float,
  val isShell: Boolean,
  val spinAxleCenter: Vec3? = null,
  val explodeDir: Vec3 = Vec3(0f, 0f, 0f),
  val isTurnLeft: Boolean = false,
  val isTurnRight: Boolean = false,
  vertexData: FloatArray,
  triIndices: ShortArray,
  wireIndices: ShortArray,
) {
  val triCount = triIndices.size
  val wireCount = wireIndices.size
  val vbo: FloatBuffer =
    ByteBuffer.allocateDirect(vertexData.size * 4)
      .order(ByteOrder.nativeOrder())
      .asFloatBuffer()
      .apply { put(vertexData); position(0) }
  val ibo: ShortBuffer =
    ByteBuffer.allocateDirect(triIndices.size * 2)
      .order(ByteOrder.nativeOrder())
      .asShortBuffer()
      .apply { put(triIndices); position(0) }
  val wbo: ShortBuffer =
    ByteBuffer.allocateDirect(wireIndices.size * 2)
      .order(ByteOrder.nativeOrder())
      .asShortBuffer()
      .apply { put(wireIndices); position(0) }
}

private class MeshAccum(
  val partKey: String,
  val texKind: PevTexKind,
  val baseRgb: FloatArray,
  val metallic: Float,
  val roughness: Float,
  val emissive: Float = 0f,
  val isShell: Boolean = false,
  val spinAxleCenter: Vec3? = null,
  val explodeDir: Vec3 = Vec3(0f, 0f, 0f),
  val isTurnLeft: Boolean = false,
  val isTurnRight: Boolean = false,
) {
  private val verts = ArrayList<Float>(1024)
  private val tris = ArrayList<Short>(512)
  private val wires = ArrayList<Short>(512)
  private var vCount = 0

  private fun addVert(p: Vec3, n: Vec3, u: Float, v: Float): Short {
    verts += p.x; verts += p.y; verts += p.z
    verts += n.x; verts += n.y; verts += n.z
    verts += u; verts += v
    return (vCount++).toShort()
  }

  private fun normVec(v: Vec3): Vec3 {
    val len = sqrt(v.x * v.x + v.y * v.y + v.z * v.z).coerceAtLeast(1e-5f)
    return Vec3(v.x / len, v.y / len, v.z / len)
  }

  fun addBox(cx: Float, cy: Float, cz: Float, hx: Float, hy: Float, hz: Float, uvScale: Float = 1f) {
    val p000 = Vec3(cx - hx, cy - hy, cz - hz)
    val p100 = Vec3(cx + hx, cy - hy, cz - hz)
    val p110 = Vec3(cx + hx, cy + hy, cz - hz)
    val p010 = Vec3(cx - hx, cy + hy, cz - hz)
    val p001 = Vec3(cx - hx, cy - hy, cz + hz)
    val p101 = Vec3(cx + hx, cy - hy, cz + hz)
    val p111 = Vec3(cx + hx, cy + hy, cz + hz)
    val p011 = Vec3(cx - hx, cy + hy, cz + hz)

    addQuad(p001, p101, p111, p011, Vec3(0f, 0f, 1f), uvScale)
    addQuad(p100, p000, p010, p110, Vec3(0f, 0f, -1f), uvScale)
    addQuad(p101, p100, p110, p111, Vec3(1f, 0f, 0f), uvScale)
    addQuad(p000, p001, p011, p010, Vec3(-1f, 0f, 0f), uvScale)
    addQuad(p011, p111, p110, p010, Vec3(0f, 1f, 0f), uvScale)
    addQuad(p000, p100, p101, p001, Vec3(0f, -1f, 0f), uvScale)
  }

  fun addQuad(a: Vec3, b: Vec3, c: Vec3, d: Vec3, n: Vec3, uvScale: Float = 1f) {
    val nn = normVec(n)
    val i0 = addVert(a, nn, 0f, uvScale)
    val i1 = addVert(b, nn, uvScale, uvScale)
    val i2 = addVert(c, nn, uvScale, 0f)
    val i3 = addVert(d, nn, 0f, 0f)
    tris += i0; tris += i1; tris += i2
    tris += i0; tris += i2; tris += i3
    wires += i0; wires += i1; wires += i1; wires += i2; wires += i2; wires += i3; wires += i3; wires += i0
  }

  fun addTube(a: Vec3, b: Vec3, radius: Float, segs: Int = 12) {
    val axis = b - a
    val len = sqrt(axis.x * axis.x + axis.y * axis.y + axis.z * axis.z)
    if (len < 1e-4f) return
    val dir = Vec3(axis.x / len, axis.y / len, axis.z / len)
    val up = if (kotlin.math.abs(dir.y) < 0.9f) Vec3(0f, 1f, 0f) else Vec3(1f, 0f, 0f)
    val side = normVec(Vec3(dir.y * up.z - dir.z * up.y, dir.z * up.x - dir.x * up.z, dir.x * up.y - dir.y * up.x))
    val binorm = normVec(Vec3(dir.y * side.z - dir.z * side.y, dir.z * side.x - dir.x * side.z, dir.x * side.y - dir.y * side.x))

    val startIdx = IntArray(segs)
    val endIdx = IntArray(segs)
    for (i in 0 until segs) {
      val ang = (i.toFloat() / segs) * (2f * PI.toFloat())
      val ca = cos(ang); val sa = sin(ang)
      val radial = side * ca + binorm * sa
      val u = (i.toFloat() / segs) * 2f
      startIdx[i] = addVert(a + radial * radius, radial, u, 0f).toInt()
      endIdx[i] = addVert(b + radial * radius, radial, u, (len * 2.5f).coerceAtLeast(1f)).toInt()
    }
    for (i in 0 until segs) {
      val ni = (i + 1) % segs
      val a0 = startIdx[i].toShort(); val a1 = startIdx[ni].toShort()
      val b0 = endIdx[i].toShort(); val b1 = endIdx[ni].toShort()
      tris += a0; tris += a1; tris += b1
      tris += a0; tris += b1; tris += b0
      wires += a0; wires += a1; wires += a0; wires += b0
    }
    // End caps
    val ca = addVert(a, dir * -1f, 0.5f, 0.5f)
    val cb = addVert(b, dir, 0.5f, 0.5f)
    for (i in 0 until segs) {
      val ni = (i + 1) % segs
      tris += ca; tris += startIdx[ni].toShort(); tris += startIdx[i].toShort()
      tris += cb; tris += endIdx[i].toShort(); tris += endIdx[ni].toShort()
    }
  }

  fun addDisc(cx: Float, cy: Float, cz: Float, radius: Float, thickness: Float, segs: Int = 24) {
    val hz = thickness * 0.5f
    val cF = addVert(Vec3(cx, cy, cz + hz), Vec3(0f, 0f, 1f), 0.5f, 0.5f)
    val cB = addVert(Vec3(cx, cy, cz - hz), Vec3(0f, 0f, -1f), 0.5f, 0.5f)
    val fRing = ShortArray(segs)
    val bRing = ShortArray(segs)
    for (i in 0 until segs) {
      val ang = (i.toFloat() / segs) * (2f * PI.toFloat())
      val ca = cos(ang); val sa = sin(ang)
      val x = cx + radius * ca
      val y = cy + radius * sa
      val u = 0.5f + 0.5f * ca
      val v = 0.5f + 0.5f * sa
      fRing[i] = addVert(Vec3(x, y, cz + hz), Vec3(0f, 0f, 1f), u, v)
      bRing[i] = addVert(Vec3(x, y, cz - hz), Vec3(0f, 0f, -1f), u, v)
    }
    for (i in 0 until segs) {
      val ni = (i + 1) % segs
      tris += cF; tris += fRing[i]; tris += fRing[ni]
      tris += cB; tris += bRing[ni]; tris += bRing[i]
      tris += fRing[i]; tris += bRing[i]; tris += bRing[ni]
      tris += fRing[i]; tris += bRing[ni]; tris += fRing[ni]
      wires += fRing[i]; wires += fRing[ni]; wires += fRing[i]; wires += bRing[i]
    }
  }

  fun addWheelTire(cx: Float, cy: Float, cz: Float, outerR: Float, innerR: Float, width: Float, segs: Int = 28) {
    val hw = width * 0.5f
    val midR = (outerR + innerR) * 0.5f
    val tubeR = (outerR - innerR) * 0.5f
    val profileSteps = 10
    val grid = Array(segs) { ShortArray(profileSteps) }
    for (i in 0 until segs) {
      val theta = (i.toFloat() / segs) * (2f * PI.toFloat())
      val ct = cos(theta); val st = sin(theta)
      for (j in 0 until profileSteps) {
        val phi = (j.toFloat() / profileSteps) * (2f * PI.toFloat())
        val cp = cos(phi); val sp = sin(phi)
        val r = midR + tubeR * cp
        val x = cx + r * ct
        val y = cy + r * st
        val z = cz + hw * sp
        val nx = cp * ct
        val ny = cp * st
        val nz = sp
        grid[i][j] = addVert(Vec3(x, y, z), normVec(Vec3(nx, ny, nz)), (i / segs.toFloat()) * 4f, (j / profileSteps.toFloat()) * 2f)
      }
    }
    for (i in 0 until segs) {
      val ni = (i + 1) % segs
      for (j in 0 until profileSteps) {
        val nj = (j + 1) % profileSteps
        val a = grid[i][j]; val b = grid[ni][j]; val c = grid[ni][nj]; val d = grid[i][nj]
        tris += a; tris += b; tris += c
        tris += a; tris += c; tris += d
        if (j % 2 == 0) {
          wires += a; wires += b
          if (i % 2 == 0) { wires += a; wires += d }
        }
      }
    }
  }

  fun build(): GlMeshPart =
    GlMeshPart(
      partKey = partKey,
      texKind = texKind,
      baseRgb = baseRgb,
      metallic = metallic,
      roughness = roughness,
      emissive = emissive,
      isShell = isShell,
      spinAxleCenter = spinAxleCenter,
      explodeDir = explodeDir,
      isTurnLeft = isTurnLeft,
      isTurnRight = isTurnRight,
      vertexData = verts.toFloatArray(),
      triIndices = tris.toShortArray(),
      wireIndices = wires.toShortArray(),
    )
}

private object Pev3dMeshFactory {
  fun buildVehicleMeshes(
    archetype: Pev3dArchetype,
    finish: Pev3dModelFinish,
    lightsOn: Boolean,
  ): List<GlMeshPart> {
    val parts = ArrayList<GlMeshPart>(18)
    val hsMap = Pev3dEngine.hotspotsForArchetype(archetype).associateBy { it.partKey }
    fun exp(key: String) = hsMap[key]?.explodeDir ?: Vec3(0f, 0f, 0f)

    val darkTire = floatArrayOf(0.16f, 0.18f, 0.21f)
    val metalSilver = floatArrayOf(0.76f, 0.80f, 0.86f)
    val gunMetal = floatArrayOf(0.32f, 0.36f, 0.43f)
    val battGreen = floatArrayOf(0.14f, 0.72f, 0.36f)
    val motorCopper = floatArrayOf(0.92f, 0.42f, 0.16f)
    val focMagenta = floatArrayOf(0.86f, 0.22f, 0.56f)
    val tftWhite = floatArrayOf(0.92f, 0.96f, 1.0f)

    // Dyno Showroom Floor Pad
    MeshAccum("floor", PevTexKind.FLOOR, floatArrayOf(0.90f, 0.94f, 1.0f), metallic = 0.25f, roughness = 0.55f).apply {
      addQuad(Vec3(-1.85f, -0.78f, 1.85f), Vec3(1.85f, -0.78f, 1.85f), Vec3(1.85f, -0.78f, -1.85f), Vec3(-1.85f, -0.78f, -1.85f), Vec3(0f, 1f, 0f), 1f)
      parts += build()
    }

    fun addWheelAssembly(
      cx: Float,
      cy: Float,
      cz: Float,
      outerR: Float,
      width: Float,
      motorKey: String,
      isPowered: Boolean,
    ) {
      val innerR = outerR * 0.63f
      MeshAccum(motorKey, PevTexKind.TIRE, darkTire, metallic = 0.08f, roughness = 0.86f, spinAxleCenter = Vec3(cx, cy, cz), explodeDir = exp(motorKey)).apply {
        addWheelTire(cx, cy, cz, outerR, innerR, width, 28)
        parts += build()
      }
      val rimRgb = if (isPowered) finish.accentRgb else metalSilver
      MeshAccum(motorKey, PevTexKind.METAL, rimRgb, metallic = 0.85f, roughness = 0.24f, spinAxleCenter = Vec3(cx, cy, cz), explodeDir = exp(motorKey)).apply {
        addDisc(cx, cy, cz, innerR * 0.55f, width * 0.72f, 20)
        for (s in 0 until 8) {
          val a = (s / 8f) * (2f * PI.toFloat())
          addTube(Vec3(cx, cy, cz), Vec3(cx + innerR * 1.02f * cos(a), cy + innerR * 1.02f * sin(a), cz), 0.017f, 8)
        }
        parts += build()
      }
      // Cross-drilled spinning steel brake rotor + fixed 4-piston caliper
      val bz = cz + width * 0.52f + 0.025f
      MeshAccum("brakes", PevTexKind.ROTOR, metalSilver, metallic = 0.92f, roughness = 0.18f, spinAxleCenter = Vec3(cx, cy, cz), explodeDir = exp("brakes")).apply {
        addDisc(cx, cy, bz, outerR * 0.48f, 0.022f, 24)
        parts += build()
      }
      MeshAccum("brakes", PevTexKind.METAL, finish.caliperRgb, metallic = 0.78f, roughness = 0.25f, explodeDir = exp("brakes")).apply {
        addBox(cx + outerR * 0.36f, cy + outerR * 0.26f, bz, 0.075f, 0.065f, 0.042f)
        parts += build()
      }
    }

    val ws = finish.wheelScale
    val ls = finish.bodyLengthScale

    when (archetype) {
      Pev3dArchetype.SCOOTER -> {
        addWheelAssembly(-0.92f * ls, -0.45f, 0f, 0.30f * ws, 0.24f, "motor", true)
        addWheelAssembly(0.92f * ls, -0.45f, 0f, 0.30f * ws, 0.24f, "suspension", true)
        MeshAccum("frame", PevTexKind.CARBON, finish.bodyRgb, metallic = 0.45f, roughness = 0.32f, isShell = true, explodeDir = exp("frame")).apply {
          addBox(0f, -0.28f, 0f, 0.72f * ls, 0.075f, 0.28f, 2f)
          addTube(Vec3(0.64f * ls, -0.20f, -0.075f), Vec3(0.42f * ls, 0.88f, -0.075f), 0.032f, 12)
          addTube(Vec3(0.64f * ls, -0.20f, 0.075f), Vec3(0.42f * ls, 0.88f, 0.075f), 0.032f, 12)
          parts += build()
        }
        MeshAccum("battery", PevTexKind.BATTERY, battGreen, metallic = 0.50f, roughness = 0.35f, emissive = 0.14f, explodeDir = exp("battery")).apply {
          addBox(0f, -0.24f, 0f, 0.50f * ls, 0.058f, 0.20f, 2f)
          parts += build()
        }
        MeshAccum("controller", PevTexKind.METAL, focMagenta, metallic = 0.82f, roughness = 0.22f, emissive = 0.10f, explodeDir = exp("controller")).apply {
          addBox(0.42f * ls, -0.23f, 0f, 0.14f, 0.06f, 0.20f)
          parts += build()
        }
        MeshAccum("suspension", PevTexKind.METAL, finish.accentRgb, metallic = 0.88f, roughness = 0.20f, explodeDir = exp("suspension")).apply {
          addTube(Vec3(-0.70f * ls, -0.28f, -0.14f), Vec3(-0.92f * ls, -0.45f, -0.14f), 0.030f, 10)
          addTube(Vec3(-0.70f * ls, -0.28f, 0.14f), Vec3(-0.92f * ls, -0.45f, 0.14f), 0.030f, 10)
          addTube(Vec3(0.68f * ls, -0.20f, -0.14f), Vec3(0.92f * ls, -0.45f, -0.14f), 0.032f, 10)
          addTube(Vec3(0.68f * ls, -0.20f, 0.14f), Vec3(0.92f * ls, -0.45f, 0.14f), 0.032f, 10)
          parts += build()
        }
        MeshAccum("cockpit", PevTexKind.METAL, gunMetal, metallic = 0.75f, roughness = 0.3f, explodeDir = exp("cockpit")).apply {
          addTube(Vec3(0.42f * ls, 0.88f, -0.46f), Vec3(0.42f * ls, 0.88f, 0.46f), 0.024f, 10)
          parts += build()
        }
        MeshAccum("cockpit", PevTexKind.TFT, tftWhite, metallic = 0.15f, roughness = 0.12f, emissive = 0.72f, explodeDir = exp("cockpit")).apply {
          addBox(0.45f * ls, 0.94f, 0f, 0.05f, 0.045f, 0.105f)
          parts += build()
        }
      }
      Pev3dArchetype.EUC -> {
        addWheelAssembly(0f, -0.12f, 0f, 0.56f * ws, 0.24f, "motor", true)
        MeshAccum("battery", PevTexKind.BATTERY, battGreen, metallic = 0.45f, roughness = 0.34f, emissive = 0.16f, explodeDir = exp("battery")).apply {
          addBox(0f, 0.08f, 0.24f, 0.33f, 0.28f, 0.085f, 2f)
          addBox(0f, 0.08f, -0.24f, 0.33f, 0.28f, 0.085f, 2f)
          parts += build()
        }
        MeshAccum("frame", PevTexKind.CARBON, finish.bodyRgb, metallic = 0.45f, roughness = 0.28f, isShell = true, explodeDir = exp("frame")).apply {
          addBox(0f, 0.09f, 0.29f, 0.36f, 0.30f, 0.045f, 2f)
          addBox(0f, 0.09f, -0.29f, 0.36f, 0.30f, 0.045f, 2f)
          addBox(0f, -0.30f, 0.35f, 0.22f, 0.025f, 0.12f)
          addBox(0f, -0.30f, -0.35f, 0.22f, 0.025f, 0.12f)
          addTube(Vec3(-0.34f, 0.44f, -0.28f), Vec3(0.34f, 0.44f, -0.28f), 0.026f, 10)
          addTube(Vec3(-0.34f, 0.44f, 0.28f), Vec3(0.34f, 0.44f, 0.28f), 0.026f, 10)
          parts += build()
        }
        MeshAccum("suspension", PevTexKind.METAL, finish.accentRgb, metallic = 0.88f, roughness = 0.20f, explodeDir = exp("suspension")).apply {
          addTube(Vec3(-0.22f, -0.18f, 0.26f), Vec3(-0.22f, 0.48f, 0.26f), 0.036f, 12)
          addTube(Vec3(-0.22f, -0.18f, -0.26f), Vec3(-0.22f, 0.48f, -0.26f), 0.036f, 12)
          parts += build()
        }
        MeshAccum("controller", PevTexKind.METAL, focMagenta, metallic = 0.80f, roughness = 0.24f, emissive = 0.12f, explodeDir = exp("controller")).apply {
          addBox(0f, 0.48f, 0f, 0.25f, 0.08f, 0.19f)
          parts += build()
        }
        MeshAccum("cockpit", PevTexKind.TFT, tftWhite, metallic = 0.2f, roughness = 0.14f, emissive = 0.72f, explodeDir = exp("cockpit")).apply {
          addBox(0.05f, 0.60f, 0f, 0.18f, 0.04f, 0.12f)
          parts += build()
        }
      }
      Pev3dArchetype.HYDROFOIL -> {
        MeshAccum("frame", PevTexKind.CARBON, finish.bodyRgb, metallic = 0.55f, roughness = 0.24f, isShell = true, explodeDir = exp("frame")).apply {
          addBox(0f, 0.14f, 0f, 1.02f, 0.065f, 0.36f, 2f)
          parts += build()
        }
        MeshAccum("battery", PevTexKind.BATTERY, battGreen, metallic = 0.5f, roughness = 0.32f, emissive = 0.18f, explodeDir = exp("battery")).apply {
          addBox(-0.10f, 0.19f, 0f, 0.38f, 0.052f, 0.22f, 2f)
          parts += build()
        }
        MeshAccum("controller", PevTexKind.METAL, focMagenta, metallic = 0.85f, roughness = 0.20f, explodeDir = exp("controller")).apply {
          addBox(0.36f, 0.18f, 0f, 0.14f, 0.048f, 0.16f)
          parts += build()
        }
        MeshAccum("suspension", PevTexKind.CARBON, finish.accentRgb, metallic = 0.65f, roughness = 0.22f, explodeDir = exp("suspension")).apply {
          addTube(Vec3(0f, 0.12f, 0f), Vec3(0f, -0.76f, 0f), 0.045f, 14)
          parts += build()
        }
        MeshAccum("motor", PevTexKind.METAL, motorCopper, metallic = 0.90f, roughness = 0.22f, emissive = 0.12f, explodeDir = exp("motor")).apply {
          addTube(Vec3(-0.46f, -0.76f, 0f), Vec3(0.46f, -0.76f, 0f), 0.052f, 16)
          parts += build()
        }
        MeshAccum("brakes", PevTexKind.CARBON, finish.caliperRgb, metallic = 0.6f, roughness = 0.25f, explodeDir = exp("brakes")).apply {
          addBox(0.24f, -0.76f, 0f, 0.13f, 0.018f, 0.48f, 2f)
          addBox(-0.38f, -0.76f, 0f, 0.08f, 0.018f, 0.25f, 1.5f)
          parts += build()
        }
        MeshAccum("cockpit", PevTexKind.TFT, tftWhite, metallic = 0.25f, roughness = 0.15f, emissive = 0.65f, explodeDir = exp("cockpit")).apply {
          addBox(0.75f, 0.28f, 0f, 0.07f, 0.03f, 0.045f)
          parts += build()
        }
      }
      else -> {
        // EMOTO & EBIKE
        val isBike = archetype == Pev3dArchetype.EBIKE
        val wheelR = (if (isBike) 0.42f else 0.40f) * ws
        addWheelAssembly(-0.92f * ls, -0.34f, 0f, wheelR, 0.22f, if (isBike) "motor" else "frame", isBike)
        addWheelAssembly(0.94f * ls, -0.34f, 0f, wheelR, 0.20f, "brakes", false)

        MeshAccum("frame", PevTexKind.CARBON, finish.bodyRgb, metallic = 0.62f, roughness = 0.28f, isShell = true, explodeDir = exp("frame")).apply {
          addTube(Vec3(-0.32f * ls, 0.26f, 0f), Vec3(0.46f * ls, 0.36f, 0f), 0.048f, 12)
          addTube(Vec3(-0.22f * ls, -0.14f, 0f), Vec3(0.44f * ls, 0.30f, 0f), 0.046f, 12)
          addTube(Vec3(-0.32f * ls, 0.26f, 0f), Vec3(-0.22f * ls, -0.14f, 0f), 0.042f, 12)
          addTube(Vec3(-0.24f * ls, -0.14f, -0.11f), Vec3(-0.92f * ls, -0.34f, -0.11f), 0.032f, 10)
          addTube(Vec3(-0.24f * ls, -0.14f, 0.11f), Vec3(-0.92f * ls, -0.34f, 0.11f), 0.032f, 10)
          // Saddle
          addBox(-0.38f * ls, 0.32f, 0f, 0.30f, 0.062f, 0.135f, 1.5f)
          parts += build()
        }
        MeshAccum("suspension", PevTexKind.METAL, finish.accentRgb, metallic = 0.90f, roughness = 0.18f, explodeDir = exp("suspension")).apply {
          addTube(Vec3(0.48f * ls, 0.58f, -0.115f), Vec3(0.94f * ls, -0.34f, -0.115f), 0.036f, 12)
          addTube(Vec3(0.48f * ls, 0.58f, 0.115f), Vec3(0.94f * ls, -0.34f, 0.115f), 0.036f, 12)
          addTube(Vec3(-0.26f * ls, 0.24f, 0f), Vec3(-0.56f * ls, -0.22f, 0f), 0.042f, 12)
          parts += build()
        }
        MeshAccum("battery", PevTexKind.BATTERY, battGreen, metallic = 0.45f, roughness = 0.32f, emissive = 0.16f, explodeDir = exp("battery")).apply {
          addBox(0.08f * ls, 0.14f, 0f, 0.24f, 0.22f, 0.14f, 2f)
          parts += build()
        }
        MeshAccum("motor", PevTexKind.METAL, motorCopper, metallic = 0.88f, roughness = 0.24f, emissive = 0.10f, explodeDir = exp("motor")).apply {
          addTube(Vec3(-0.14f * ls, -0.22f, -0.14f), Vec3(-0.14f * ls, -0.22f, 0.14f), 0.13f, 16)
          parts += build()
        }
        MeshAccum("controller", PevTexKind.METAL, focMagenta, metallic = 0.84f, roughness = 0.22f, emissive = 0.12f, explodeDir = exp("controller")).apply {
          addBox(0.34f * ls, -0.02f, 0f, 0.10f, 0.12f, 0.11f)
          parts += build()
        }
        MeshAccum("cockpit", PevTexKind.METAL, gunMetal, metallic = 0.78f, roughness = 0.28f, explodeDir = exp("cockpit")).apply {
          addTube(Vec3(0.50f * ls, 0.64f, -0.44f), Vec3(0.50f * ls, 0.64f, 0.44f), 0.024f, 10)
          parts += build()
        }
        MeshAccum("cockpit", PevTexKind.TFT, tftWhite, metallic = 0.18f, roughness = 0.12f, emissive = 0.75f, explodeDir = exp("cockpit")).apply {
          addBox(0.54f * ls, 0.68f, 0f, 0.06f, 0.042f, 0.095f)
          parts += build()
        }
      }
    }

    // Headlight + Glowing Taillight + 4-corner Stalk Turn Signals (matching Vulkan GLB rig)
    if (archetype != Pev3dArchetype.HYDROFOIL) {
      val hlX = if (archetype == Pev3dArchetype.EUC) 0.34f else 0.66f * ls
      val hlY = if (archetype == Pev3dArchetype.EUC) 0.26f else 0.46f
      val tailX = if (archetype == Pev3dArchetype.EUC) -0.36f else -0.78f * ls
      val tailY = if (archetype == Pev3dArchetype.EUC) 0.24f else 0.24f
      val frontSigX = if (archetype == Pev3dArchetype.EUC) 0.28f else 0.56f * ls
      val frontSigY = if (archetype == Pev3dArchetype.EUC) 0.26f else 0.48f
      val rearSigX = if (archetype == Pev3dArchetype.EUC) -0.34f else -0.82f * ls
      val rearSigY = if (archetype == Pev3dArchetype.EUC) 0.22f else 0.24f
      val frontSigZ = if (archetype == Pev3dArchetype.EUC) 0.31f else 0.24f
      val rearSigZ = if (archetype == Pev3dArchetype.EUC) 0.31f else 0.22f

      MeshAccum("cockpit", PevTexKind.HEADLIGHT, floatArrayOf(1.0f, 0.96f, 0.80f), metallic = 0.1f, roughness = 0.08f, emissive = if (lightsOn) 1.0f else 0.15f, explodeDir = exp("cockpit")).apply {
        addBox(hlX, hlY, 0f, 0.048f, 0.056f, 0.095f)
        parts += build()
      }
      MeshAccum("frame", PevTexKind.TAILLIGHT, floatArrayOf(1.0f, 0.16f, 0.16f), metallic = 0.2f, roughness = 0.15f, emissive = if (lightsOn) 0.95f else 0.12f, explodeDir = exp("frame")).apply {
        addBox(tailX, tailY, 0f, 0.036f, 0.042f, 0.098f)
        parts += build()
      }
      // 4-Corner Stalk Turn Signals (Left / Right)
      MeshAccum("cockpit", PevTexKind.AMBER_SIGNAL, floatArrayOf(1.0f, 0.62f, 0.08f), metallic = 0.25f, roughness = 0.18f, emissive = 0.15f, explodeDir = exp("cockpit"), isTurnLeft = true).apply {
        addBox(frontSigX, frontSigY, frontSigZ, 0.034f, 0.022f, 0.038f)
        parts += build()
      }
      MeshAccum("cockpit", PevTexKind.AMBER_SIGNAL, floatArrayOf(1.0f, 0.62f, 0.08f), metallic = 0.25f, roughness = 0.18f, emissive = 0.15f, explodeDir = exp("cockpit"), isTurnRight = true).apply {
        addBox(frontSigX, frontSigY, -frontSigZ, 0.034f, 0.022f, 0.038f)
        parts += build()
      }
      MeshAccum("frame", PevTexKind.AMBER_SIGNAL, floatArrayOf(1.0f, 0.62f, 0.08f), metallic = 0.25f, roughness = 0.18f, emissive = 0.15f, explodeDir = exp("frame"), isTurnLeft = true).apply {
        addBox(rearSigX, rearSigY, rearSigZ, 0.032f, 0.022f, 0.036f)
        parts += build()
      }
      MeshAccum("frame", PevTexKind.AMBER_SIGNAL, floatArrayOf(1.0f, 0.62f, 0.08f), metallic = 0.25f, roughness = 0.18f, emissive = 0.15f, explodeDir = exp("frame"), isTurnRight = true).apply {
        addBox(rearSigX, rearSigY, -rearSigZ, 0.032f, 0.022f, 0.036f)
        parts += build()
      }
    }

    return parts
  }
}

private object Pev3dGlShaders {
  const val LIT_VERTEX = """
    uniform mat4 uMvpMatrix;
    uniform mat4 uModelMatrix;
    attribute vec3 aPosition;
    attribute vec3 aNormal;
    attribute vec2 aTexCoord;
    varying vec3 vWorldPos;
    varying vec3 vNormal;
    varying vec2 vTexCoord;
    void main() {
      vec4 wp = uModelMatrix * vec4(aPosition, 1.0);
      vWorldPos = wp.xyz;
      vNormal = normalize(mat3(uModelMatrix) * aNormal);
      vTexCoord = aTexCoord;
      gl_Position = uMvpMatrix * vec4(aPosition, 1.0);
    }
  """

  const val LIT_FRAGMENT = """
    precision mediump float;
    uniform sampler2D uTexture;
    uniform vec3 uCameraPos;
    uniform vec3 uBaseColor;
    uniform vec3 uAccentColor;
    uniform float uMetallic;
    uniform float uRoughness;
    uniform float uEmissive;
    uniform float uSelectedPulse;
    uniform float uAlpha;
    uniform int uShaderMode;
    uniform int uIsShell;
    varying vec3 vWorldPos;
    varying vec3 vNormal;
    varying vec2 vTexCoord;

    void main() {
      vec3 N = normalize(vNormal);
      vec3 V = normalize(uCameraPos - vWorldPos);
      vec4 texel = texture2D(uTexture, vTexCoord);
      vec3 albedo = texel.rgb * uBaseColor;

      // Carbon Gloss mode amplifies specular & weave depth
      float effRough = (uShaderMode == 3) ? max(0.08, uRoughness * 0.62) : uRoughness;
      float effMetal = (uShaderMode == 3) ? min(1.0, uMetallic + 0.24) : uMetallic;

      // 3-point studio lighting + hemisphere bounce
      vec3 L1 = normalize(vec3(0.55, 0.90, 0.50));
      vec3 L2 = normalize(vec3(-0.75, 0.35, -0.55));
      vec3 L3 = normalize(vec3(0.45, 0.25, -0.82));

      float diff1 = max(dot(N, L1), 0.0);
      float diff2 = max(dot(N, L2), 0.0);
      float diff3 = max(dot(N, L3), 0.0);

      vec3 hemi = mix(vec3(0.07, 0.10, 0.16), vec3(0.24, 0.30, 0.40), clamp(N.y * 0.5 + 0.5, 0.0, 1.0));
      vec3 diffuse = albedo * (hemi + diff1 * vec3(1.05, 1.02, 0.96) + diff2 * vec3(0.25, 0.58, 0.85) + diff3 * vec3(0.45, 0.38, 0.32));

      float shininess = mix(118.0, 12.0, effRough);
      vec3 H1 = normalize(L1 + V);
      vec3 H2 = normalize(L2 + V);
      float spec1 = pow(max(dot(N, H1), 0.0), shininess) * mix(0.32, 1.25, effMetal);
      float spec2 = pow(max(dot(N, H2), 0.0), shininess * 0.75) * 0.45;
      vec3 specTint = mix(vec3(1.0), albedo, effMetal * 0.65);

      float fresnel = pow(1.0 - max(abs(dot(N, V)), 0.0), 2.8);
      vec3 rimGlow = mix(vec3(0.25, 0.72, 1.0), uAccentColor, 0.45) * fresnel * 0.55;

      vec3 color = diffuse + specTint * (spec1 + spec2) + rimGlow + albedo * uEmissive;

      // Selected subsystem emissive pulse highlight
      if (uSelectedPulse > 0.01) {
        vec3 selCol = mix(uAccentColor, vec3(1.0), 0.35);
        color = mix(color, selCol, 0.22 * uSelectedPulse) + selCol * (0.35 + 0.65 * fresnel) * uSelectedPulse;
      }

      float outAlpha = uAlpha;
      if (uShaderMode == 2) {
        // X-Ray Holographic Shader
        float xRim = pow(1.0 - max(abs(dot(N, V)), 0.0), 1.85);
        if (uIsShell == 1) {
          color = mix(vec3(0.06, 0.22, 0.38), vec3(0.35, 0.88, 1.0), xRim) + uAccentColor * (0.3 * uSelectedPulse);
          outAlpha = clamp(0.14 + 0.62 * xRim + 0.20 * uSelectedPulse, 0.14, 0.90);
        } else {
          color = color * 1.15 + uAccentColor * (0.28 * xRim);
          outAlpha = 0.94;
        }
      } else if (uShaderMode == 1) {
        // Dark occluded fill under CAD neon wireframe
        color *= 0.24;
      }

      gl_FragColor = vec4(clamp(color, 0.0, 1.0), outAlpha);
    }
  """

  const val WIRE_VERTEX = """
    uniform mat4 uMvpMatrix;
    attribute vec3 aPosition;
    void main() {
      gl_Position = uMvpMatrix * vec4(aPosition, 1.0);
    }
  """

  const val WIRE_FRAGMENT = """
    precision mediump float;
    uniform vec4 uWireColor;
    void main() {
      gl_FragColor = uWireColor;
    }
  """

  fun compileProgram(vertSrc: String, fragSrc: String): Int {
    val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertSrc)
    val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragSrc)
    val prog = GLES20.glCreateProgram()
    GLES20.glAttachShader(prog, vs)
    GLES20.glAttachShader(prog, fs)
    GLES20.glLinkProgram(prog)
    return prog
  }

  private fun compileShader(type: Int, src: String): Int {
    val s = GLES20.glCreateShader(type)
    GLES20.glShaderSource(s, src)
    GLES20.glCompileShader(s)
    return s
  }
}

internal class Pev3dGlRenderer : GLSurfaceView.Renderer {
  @Volatile var archetype: Pev3dArchetype = Pev3dArchetype.EMOTO
  @Volatile var renderMode: Pev3dRenderMode = Pev3dRenderMode.SOLID
  @Volatile var modelName: String = "Sur-Ron Ultra Bee eMoto"
  @Volatile var modelCategory: String = "Electric Dirt Moped"
  @Volatile var modelVoltage: Int = 74
  @Volatile var yawDeg: Float = -28f
  @Volatile var pitchDeg: Float = 16f
  @Volatile var zoom: Float = 1.05f
  @Volatile var explodePct: Float = 0f
  @Volatile var wheelSpinDeg: Float = 0f
  @Volatile var lightsOn: Boolean = true
  @Volatile var turnSignalMode: Pev3dTurnSignalMode = Pev3dTurnSignalMode.OFF
  @Volatile var selectedPartKey: String? = "battery"
  @Volatile var accentRgb: FloatArray = floatArrayOf(0.22f, 0.85f, 0.96f)

  private var textures: Pev3dTextureBank? = null
  private var litProg = 0
  private var wireProg = 0
  private var cachedMeshes: List<GlMeshPart> = emptyList()
  private var cachedKey: String = ""
  private var startNs = System.nanoTime()

  private val vpMatrix = FloatArray(16)
  private val modelMatrix = FloatArray(16)
  private val mvpMatrix = FloatArray(16)
  private val camPos = FloatArray(3)
  private var widthPx = 800
  private var heightPx = 600

  override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
    GLES20.glClearColor(0.025f, 0.045f, 0.095f, 1.0f)
    GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    GLES20.glEnable(GLES20.GL_BLEND)
    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    litProg = Pev3dGlShaders.compileProgram(Pev3dGlShaders.LIT_VERTEX, Pev3dGlShaders.LIT_FRAGMENT)
    wireProg = Pev3dGlShaders.compileProgram(Pev3dGlShaders.WIRE_VERTEX, Pev3dGlShaders.WIRE_FRAGMENT)
    textures = Pev3dTextureBank.createOnGlThread()
    cachedKey = ""
  }

  override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
    widthPx = width.coerceAtLeast(1)
    heightPx = height.coerceAtLeast(1)
    GLES20.glViewport(0, 0, widthPx, heightPx)
  }

  override fun onDrawFrame(unused: GL10?) {
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
    val tex = textures ?: return

    val buildKey = "$archetype|$modelName|$modelVoltage|$lightsOn"
    if (buildKey != cachedKey) {
      val finish = Pev3dEngine.finishForModel(modelName, modelCategory, modelVoltage)
      cachedMeshes = Pev3dMeshFactory.buildVehicleMeshes(archetype, finish, lightsOn)
      cachedKey = buildKey
    }

    Pev3dEngine.buildCameraViewProjection(
      widthPx = widthPx.toFloat(),
      heightPx = heightPx.toFloat(),
      yawDeg = yawDeg,
      pitchDeg = pitchDeg,
      zoom = zoom,
      outVpMatrix = vpMatrix,
      outCameraPos = camPos,
    )

    val timeSec = ((System.nanoTime() - startNs) / 1_000_000_000.0).toFloat()
    val pulse = (0.5f + 0.5f * sin(timeSec * 4.5f))
    val mode = renderMode

    // Render opaque parts first, translucent X-Ray shell parts last
    val sortedParts =
      if (mode == Pev3dRenderMode.XRAY) cachedMeshes.sortedBy { if (it.isShell) 1 else 0 }
      else cachedMeshes

    GLES20.glUseProgram(litProg)
    val aPos = GLES20.glGetAttribLocation(litProg, "aPosition")
    val aNorm = GLES20.glGetAttribLocation(litProg, "aNormal")
    val aTex = GLES20.glGetAttribLocation(litProg, "aTexCoord")

    val uMvp = GLES20.glGetUniformLocation(litProg, "uMvpMatrix")
    val uModel = GLES20.glGetUniformLocation(litProg, "uModelMatrix")
    val uCam = GLES20.glGetUniformLocation(litProg, "uCameraPos")
    val uBase = GLES20.glGetUniformLocation(litProg, "uBaseColor")
    val uAcc = GLES20.glGetUniformLocation(litProg, "uAccentColor")
    val uMet = GLES20.glGetUniformLocation(litProg, "uMetallic")
    val uRough = GLES20.glGetUniformLocation(litProg, "uRoughness")
    val uEmis = GLES20.glGetUniformLocation(litProg, "uEmissive")
    val uSel = GLES20.glGetUniformLocation(litProg, "uSelectedPulse")
    val uAlpha = GLES20.glGetUniformLocation(litProg, "uAlpha")
    val uMode = GLES20.glGetUniformLocation(litProg, "uShaderMode")
    val uShell = GLES20.glGetUniformLocation(litProg, "uIsShell")
    val uTex = GLES20.glGetUniformLocation(litProg, "uTexture")

    GLES20.glUniform3f(uCam, camPos[0], camPos[1], camPos[2])
    GLES20.glUniform3f(uAcc, accentRgb[0], accentRgb[1], accentRgb[2])
    GLES20.glUniform1i(uMode, mode.shaderId)
    GLES20.glUniform1i(uTex, 0)

    val stride = 8 * 4
    sortedParts.forEach { part ->
      Matrix.setIdentityM(modelMatrix, 0)
      if (explodePct > 0.005f && part.partKey != "floor") {
        Matrix.translateM(modelMatrix, 0, part.explodeDir.x * explodePct, part.explodeDir.y * explodePct, part.explodeDir.z * explodePct)
      }
      part.spinAxleCenter?.let { c ->
        Matrix.translateM(modelMatrix, 0, c.x, c.y, c.z)
        Matrix.rotateM(modelMatrix, 0, -wheelSpinDeg, 0f, 0f, 1f)
        Matrix.translateM(modelMatrix, 0, -c.x, -c.y, -c.z)
      }
      Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

      val glTexId =
        when (part.texKind) {
          PevTexKind.CARBON -> tex.carbonWeave
          PevTexKind.METAL -> tex.brushedMetal
          PevTexKind.TIRE -> tex.tireRubber
          PevTexKind.BATTERY -> tex.batteryPack
          PevTexKind.ROTOR -> tex.brakeRotor
          PevTexKind.TFT -> tex.tftScreen
          PevTexKind.FLOOR -> tex.dynoFloor
          PevTexKind.HEADLIGHT, PevTexKind.TAILLIGHT, PevTexKind.AMBER_SIGNAL -> tex.tftScreen
        }
      GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, glTexId)

      val signalPulseOn = sin(timeSec * 2f * PI.toFloat() * 1.8f) > 0f
      val effectiveEmissive =
        when {
          part.isTurnLeft -> {
            val active = turnSignalMode == Pev3dTurnSignalMode.LEFT || turnSignalMode == Pev3dTurnSignalMode.HAZARD
            if (active && signalPulseOn) 1.0f else if (active) 0.12f else 0.08f
          }
          part.isTurnRight -> {
            val active = turnSignalMode == Pev3dTurnSignalMode.RIGHT || turnSignalMode == Pev3dTurnSignalMode.HAZARD
            if (active && signalPulseOn) 1.0f else if (active) 0.12f else 0.08f
          }
          else -> part.emissive
        }

      val isSelected = selectedPartKey != null && part.partKey == selectedPartKey
      GLES20.glUniformMatrix4fv(uMvp, 1, false, mvpMatrix, 0)
      GLES20.glUniformMatrix4fv(uModel, 1, false, modelMatrix, 0)
      GLES20.glUniform3f(uBase, part.baseRgb[0], part.baseRgb[1], part.baseRgb[2])
      GLES20.glUniform1f(uMet, part.metallic)
      GLES20.glUniform1f(uRough, part.roughness)
      GLES20.glUniform1f(uEmis, effectiveEmissive)
      GLES20.glUniform1f(uSel, if (isSelected) 0.55f + 0.45f * pulse else 0f)
      GLES20.glUniform1f(uAlpha, if (part.partKey == "floor") 0.88f else 1f)
      GLES20.glUniform1i(uShell, if (part.isShell) 1 else 0)

      part.vbo.position(0)
      GLES20.glEnableVertexAttribArray(aPos)
      GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, stride, part.vbo)

      part.vbo.position(3)
      GLES20.glEnableVertexAttribArray(aNorm)
      GLES20.glVertexAttribPointer(aNorm, 3, GLES20.GL_FLOAT, false, stride, part.vbo)

      part.vbo.position(6)
      GLES20.glEnableVertexAttribArray(aTex)
      GLES20.glVertexAttribPointer(aTex, 2, GLES20.GL_FLOAT, false, stride, part.vbo)

      if (mode == Pev3dRenderMode.XRAY && part.isShell) {
        GLES20.glDepthMask(false)
      }
      GLES20.glDrawElements(GLES20.GL_TRIANGLES, part.triCount, GLES20.GL_UNSIGNED_SHORT, part.ibo)
      GLES20.glDepthMask(true)
    }

    // Wireframe Pass in CAD Wireframe Mode (or subtle rim wireframe for selected part in X-Ray)
    if (mode == Pev3dRenderMode.WIREFRAME || mode == Pev3dRenderMode.XRAY) {
      GLES20.glUseProgram(wireProg)
      val waPos = GLES20.glGetAttribLocation(wireProg, "aPosition")
      val wuMvp = GLES20.glGetUniformLocation(wireProg, "uMvpMatrix")
      val wuCol = GLES20.glGetUniformLocation(wireProg, "uWireColor")
      sortedParts.forEach { part ->
        val isSelected = selectedPartKey != null && part.partKey == selectedPartKey
        if (mode == Pev3dRenderMode.XRAY && !isSelected) return@forEach

        Matrix.setIdentityM(modelMatrix, 0)
        if (explodePct > 0.005f && part.partKey != "floor") {
          Matrix.translateM(modelMatrix, 0, part.explodeDir.x * explodePct, part.explodeDir.y * explodePct, part.explodeDir.z * explodePct)
        }
        part.spinAxleCenter?.let { c ->
          Matrix.translateM(modelMatrix, 0, c.x, c.y, c.z)
          Matrix.rotateM(modelMatrix, 0, -wheelSpinDeg, 0f, 0f, 1f)
          Matrix.translateM(modelMatrix, 0, -c.x, -c.y, -c.z)
        }
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(wuMvp, 1, false, mvpMatrix, 0)
        if (isSelected) {
          GLES20.glUniform4f(wuCol, 1f, 1f, 1f, 0.95f)
          GLES20.glLineWidth(3.0f)
        } else {
          GLES20.glUniform4f(wuCol, accentRgb[0], accentRgb[1], accentRgb[2], if (part.partKey == "floor") 0.35f else 0.78f)
          GLES20.glLineWidth(1.6f)
        }
        part.vbo.position(0)
        GLES20.glEnableVertexAttribArray(waPos)
        GLES20.glVertexAttribPointer(waPos, 3, GLES20.GL_FLOAT, false, stride, part.vbo)
        GLES20.glDrawElements(GLES20.GL_LINES, part.wireCount, GLES20.GL_UNSIGNED_SHORT, part.wbo)
      }
    }
  }
}

@Composable
internal fun Pev3dGlViewport(
  archetype: Pev3dArchetype,
  renderMode: Pev3dRenderMode,
  modelName: String,
  modelCategory: String,
  modelVoltage: Int,
  yawDeg: Float,
  pitchDeg: Float,
  zoom: Float,
  explodePct: Float,
  wheelSpinDeg: Float,
  lightsOn: Boolean,
  turnSignalMode: Pev3dTurnSignalMode = Pev3dTurnSignalMode.OFF,
  selectedPartKey: String?,
  accentRgb: FloatArray,
  modifier: Modifier = Modifier,
) {
  val renderer = remember { Pev3dGlRenderer() }
  renderer.archetype = archetype
  renderer.renderMode = renderMode
  renderer.modelName = modelName
  renderer.modelCategory = modelCategory
  renderer.modelVoltage = modelVoltage
  renderer.yawDeg = yawDeg
  renderer.pitchDeg = pitchDeg
  renderer.zoom = zoom
  renderer.explodePct = explodePct
  renderer.wheelSpinDeg = wheelSpinDeg
  renderer.lightsOn = lightsOn
  renderer.turnSignalMode = turnSignalMode
  renderer.selectedPartKey = selectedPartKey
  renderer.accentRgb = accentRgb

  val lifecycleOwner = LocalLifecycleOwner.current
  val glViewHolder = remember { arrayOfNulls<GLSurfaceView>(1) }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> glViewHolder[0]?.onResume()
        Lifecycle.Event.ON_PAUSE -> glViewHolder[0]?.onPause()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      glViewHolder[0]?.onPause()
      glViewHolder[0] = null
    }
  }

  AndroidView(
    modifier = modifier,
    factory = { ctx ->
      GLSurfaceView(ctx).apply {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY)
        glViewHolder[0] = this
      }
    },
    update = { view ->
      glViewHolder[0] = view
    },
  )
}
