package com.example.ui.screens

import android.opengl.Matrix
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class Pev3dArchetype {
  EMOTO,
  SCOOTER,
  EUC,
  EBIKE,
  HYDROFOIL,
}

enum class Pev3dRenderMode(val label: String, val shaderId: Int) {
  SOLID("Vulkan PBR", 0),
  CARBON_SHOWROOM("Carbon Gloss", 3),
  WIREFRAME("CAD Wire", 1),
  XRAY("X-Ray Shader", 2),
}

enum class Pev3dTurnSignalMode(val label: String) {
  OFF("Signals Off"),
  LEFT("Left <"),
  RIGHT("Right >"),
  HAZARD("Hazards"),
}

data class Vec3(val x: Float, val y: Float, val z: Float) {
  operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
  operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
  operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
}

data class Pev3dHotspot(
  val partKey: String,
  val label: String,
  val worldPos: Vec3,
  val explodeDir: Vec3,
  val accent: Color,
)

data class ProjectedHotspot(
  val partKey: String,
  val label: String,
  val screenPos: Offset,
  val depth: Float,
  val accent: Color,
)

data class Pev3dModelFinish(
  val bodyRgb: FloatArray,
  val accentRgb: FloatArray,
  val caliperRgb: FloatArray,
  val wheelScale: Float = 1.0f,
  val bodyLengthScale: Float = 1.0f,
)

internal object Pev3dEngine {
  fun archetypeForCategory(category: String): Pev3dArchetype {
    val c = category.lowercase()
    return when {
      c.contains("unicycle") || c.contains("euc") -> Pev3dArchetype.EUC
      c.contains("scooter") -> Pev3dArchetype.SCOOTER
      c.contains("hydrofoil") || c.contains("efoil") -> Pev3dArchetype.HYDROFOIL
      c.contains("e-bike") || c.contains("moped") -> Pev3dArchetype.EBIKE
      else -> Pev3dArchetype.EMOTO
    }
  }

  fun finishForModel(name: String, category: String, voltage: Int): Pev3dModelFinish {
    val n = name.lowercase()
    val c = category.lowercase()
    val bodyRgb =
      when {
        n.contains("rion") || n.contains("stealth") || n.contains("black") -> floatArrayOf(0.11f, 0.13f, 0.17f)
        n.contains("stark") || n.contains("red") || n.contains("ducati") -> floatArrayOf(0.84f, 0.14f, 0.16f)
        n.contains("bmw") || n.contains("ryvid") || n.contains("apollo") -> floatArrayOf(0.85f, 0.89f, 0.94f)
        n.contains("ultra bee") || n.contains("kaabo") || n.contains("green") -> floatArrayOf(0.16f, 0.72f, 0.34f)
        n.contains("storm bee") || n.contains("talaria") || n.contains("amber") -> floatArrayOf(0.92f, 0.45f, 0.12f)
        n.contains("fliteboard") || n.contains("inmotion") -> floatArrayOf(0.08f, 0.54f, 0.78f)
        n.contains("sherman") || n.contains("patton") || n.contains("lynx") -> floatArrayOf(0.24f, 0.27f, 0.33f)
        n.contains("onyx") || n.contains("cake") || n.contains("volcon") -> floatArrayOf(0.74f, 0.56f, 0.24f)
        voltage >= 100 -> floatArrayOf(0.18f, 0.22f, 0.30f)
        else -> floatArrayOf(0.22f, 0.32f, 0.46f)
      }
    val accentRgb =
      when {
        n.contains("ultra bee") || n.contains("kaabo") -> floatArrayOf(0.45f, 0.95f, 0.22f)
        n.contains("lynx") || n.contains("sherman") || n.contains("cake") -> floatArrayOf(0.92f, 0.72f, 0.24f)
        n.contains("stark") || n.contains("dualtron") || n.contains("nami") -> floatArrayOf(0.96f, 0.28f, 0.26f)
        n.contains("fliteboard") || n.contains("segway") -> floatArrayOf(0.18f, 0.86f, 0.96f)
        else -> floatArrayOf(0.22f, 0.78f, 0.98f)
      }
    val caliperRgb =
      when {
        n.contains("stark") || n.contains("dualtron") -> floatArrayOf(0.94f, 0.18f, 0.22f)
        n.contains("lynx") || n.contains("storm") -> floatArrayOf(0.96f, 0.74f, 0.18f)
        else -> floatArrayOf(0.20f, 0.74f, 0.98f)
      }
    val wheelScale =
      when {
        c.contains("utility") || c.contains("fat") -> 1.10f
        n.contains("master pro") || n.contains("v13") || n.contains("sherman") -> 1.07f
        n.contains("climber") || n.contains("p100s") -> 0.90f
        else -> 1.0f
      }
    val bodyLengthScale =
      when {
        n.contains("storm") || n.contains("stark") || n.contains("metacycle") -> 1.06f
        n.contains("climber") -> 0.92f
        else -> 1.0f
      }
    return Pev3dModelFinish(bodyRgb, accentRgb, caliperRgb, wheelScale, bodyLengthScale)
  }

  fun hotspotsForArchetype(archetype: Pev3dArchetype): List<Pev3dHotspot> =
    when (archetype) {
      Pev3dArchetype.SCOOTER ->
        listOf(
          Pev3dHotspot("battery", "Battery & BMS", Vec3(0f, -0.26f, 0f), Vec3(0f, 0.38f, 0f), Color(0xFF22C55E)),
          Pev3dHotspot("motor", "Dual Hub Motors", Vec3(-0.92f, -0.45f, 0f), Vec3(-0.35f, -0.1f, 0f), Color(0xFFFF6B35)),
          Pev3dHotspot("brakes", "4-Piston Brakes", Vec3(0.92f, -0.45f, 0.2f), Vec3(0.25f, -0.15f, 0.35f), Color(0xFF38BDF8)),
          Pev3dHotspot("suspension", "C-Arm Shocks", Vec3(0.75f, -0.22f, 0f), Vec3(0.25f, 0.25f, 0f), Color(0xFF6366F1)),
          Pev3dHotspot("controller", "FOC Inverter", Vec3(0.32f, -0.24f, 0f), Vec3(0f, -0.35f, 0.25f), Color(0xFFEC4899)),
          Pev3dHotspot("cockpit", "TFT Cockpit", Vec3(0.46f, 0.96f, 0f), Vec3(0.15f, 0.35f, 0f), Color(0xFFF59E0B)),
          Pev3dHotspot("frame", "Stem & Deck", Vec3(-0.32f, -0.18f, 0f), Vec3(0f, 0.12f, -0.25f), Color(0xFFA855F7)),
        )
      Pev3dArchetype.EUC ->
        listOf(
          Pev3dHotspot("battery", "Twin Cell Packs", Vec3(0f, 0.14f, 0.26f), Vec3(0f, 0.2f, 0.45f), Color(0xFF22C55E)),
          Pev3dHotspot("motor", "Hollow-Bore Hub", Vec3(0f, -0.12f, 0f), Vec3(0f, -0.25f, 0f), Color(0xFFFF6B35)),
          Pev3dHotspot("brakes", "E-Brake & Rotor", Vec3(0.24f, -0.24f, 0.15f), Vec3(0.32f, -0.18f, 0.25f), Color(0xFF38BDF8)),
          Pev3dHotspot("suspension", "CNC Stanchions", Vec3(-0.22f, 0.18f, 0.22f), Vec3(-0.35f, 0.28f, 0f), Color(0xFF6366F1)),
          Pev3dHotspot("controller", "Mainboard & Mosfets", Vec3(0f, 0.46f, 0f), Vec3(0f, 0.45f, 0f), Color(0xFFEC4899)),
          Pev3dHotspot("cockpit", "Trolley & HUD", Vec3(0.22f, 0.56f, 0f), Vec3(0.25f, 0.35f, 0f), Color(0xFFF59E0B)),
          Pev3dHotspot("frame", "CNC Pedals & Cage", Vec3(0f, -0.28f, 0.34f), Vec3(0f, -0.1f, 0.38f), Color(0xFFA855F7)),
        )
      Pev3dArchetype.HYDROFOIL ->
        listOf(
          Pev3dHotspot("battery", "Waterproof Pack", Vec3(-0.1f, 0.18f, 0f), Vec3(0f, 0.4f, 0f), Color(0xFF22C55E)),
          Pev3dHotspot("motor", "Foil Pod Motor", Vec3(-0.2f, -0.78f, 0f), Vec3(-0.25f, -0.25f, 0f), Color(0xFFFF6B35)),
          Pev3dHotspot("brakes", "Foil Wing Stabilizer", Vec3(0.25f, -0.78f, 0f), Vec3(0.3f, -0.2f, 0f), Color(0xFF38BDF8)),
          Pev3dHotspot("suspension", "Carbon Mast", Vec3(0f, -0.30f, 0f), Vec3(0f, 0f, 0.2f), Color(0xFF6366F1)),
          Pev3dHotspot("controller", "Watercooled ESC", Vec3(0.22f, 0.16f, 0f), Vec3(0.25f, 0.35f, 0f), Color(0xFFEC4899)),
          Pev3dHotspot("cockpit", "Handheld Remote", Vec3(0.75f, 0.28f, 0f), Vec3(0.2f, 0.3f, 0f), Color(0xFFF59E0B)),
          Pev3dHotspot("frame", "Carbon Board Hull", Vec3(0f, 0.12f, 0f), Vec3(0f, 0.18f, -0.22f), Color(0xFFA855F7)),
        )
      else -> // EMOTO & EBIKE
        listOf(
          Pev3dHotspot("battery", "High-V Pack", Vec3(0.08f, 0.14f, 0f), Vec3(0f, 0.44f, 0f), Color(0xFF22C55E)),
          Pev3dHotspot("motor", "Mid/Hub Drive", Vec3(-0.18f, -0.22f, 0f), Vec3(0f, -0.36f, 0f), Color(0xFFFF6B35)),
          Pev3dHotspot("brakes", "4-Piston Rotors", Vec3(0.95f, -0.35f, 0.14f), Vec3(0.28f, -0.15f, 0.35f), Color(0xFF38BDF8)),
          Pev3dHotspot("suspension", "USD Inverted Fork", Vec3(0.72f, 0.22f, 0f), Vec3(0.35f, 0.2f, 0f), Color(0xFF6366F1)),
          Pev3dHotspot("controller", "FOC Inverter", Vec3(0.34f, -0.04f, 0f), Vec3(0.25f, -0.22f, 0.25f), Color(0xFFEC4899)),
          Pev3dHotspot("cockpit", "TFT & Bars", Vec3(0.55f, 0.68f, 0f), Vec3(0.2f, 0.35f, 0f), Color(0xFFF59E0B)),
          Pev3dHotspot("frame", "6061 Chassis", Vec3(-0.35f, 0.18f, 0f), Vec3(-0.22f, 0.2f, -0.2f), Color(0xFFA855F7)),
        )
    }

  /**
   * Computes OpenGL camera position & ViewProjection matrix matching Pev3dGlRenderer
   * so 2D screen hotspots and touch hit-testing align pixel-for-pixel with the 3D shaders.
   */
  fun buildCameraViewProjection(
    widthPx: Float,
    heightPx: Float,
    yawDeg: Float,
    pitchDeg: Float,
    zoom: Float,
    outVpMatrix: FloatArray,
    outCameraPos: FloatArray? = null,
  ) {
    val aspect = (widthPx / heightPx.coerceAtLeast(1f)).coerceAtLeast(0.4f)
    val proj = FloatArray(16)
    val view = FloatArray(16)
    Matrix.perspectiveM(proj, 0, 38f, aspect, 0.25f, 40f)

    val dist = (4.65f / zoom.coerceIn(0.6f, 2.4f)).coerceIn(1.8f, 8.5f)
    val yr = (yawDeg * PI / 180.0).toFloat()
    val pr = (pitchDeg * PI / 180.0).toFloat()
    val cosP = cos(pr)
    val sinP = sin(pr)
    val camX = dist * cosP * sin(yr)
    val camY = dist * sinP + 0.08f
    val camZ = dist * cosP * cos(yr)

    outCameraPos?.let {
      it[0] = camX
      it[1] = camY
      it[2] = camZ
    }
    Matrix.setLookAtM(
      view,
      0,
      camX,
      camY,
      camZ,
      0f,
      -0.05f,
      0f,
      0f,
      1f,
      0f,
    )
    Matrix.multiplyMM(outVpMatrix, 0, proj, 0, view, 0)
  }

  fun projectHotspots(
    archetype: Pev3dArchetype,
    widthPx: Float,
    heightPx: Float,
    yawDeg: Float,
    pitchDeg: Float,
    zoom: Float,
    explodePct: Float,
  ): List<ProjectedHotspot> {
    if (widthPx <= 1f || heightPx <= 1f) return emptyList()
    val vp = FloatArray(16)
    buildCameraViewProjection(widthPx, heightPx, yawDeg, pitchDeg, zoom, vp)
    val inVec = FloatArray(4)
    val outVec = FloatArray(4)
    return hotspotsForArchetype(archetype).map { hs ->
      val wp = hs.worldPos + hs.explodeDir * explodePct
      inVec[0] = wp.x
      inVec[1] = wp.y
      inVec[2] = wp.z
      inVec[3] = 1f
      Matrix.multiplyMV(outVec, 0, vp, 0, inVec, 0)
      val w = outVec[3].coerceAtLeast(0.01f)
      val ndcX = outVec[0] / w
      val ndcY = outVec[1] / w
      val sx = (ndcX * 0.5f + 0.5f) * widthPx
      val sy = (0.5f - ndcY * 0.5f) * heightPx
      ProjectedHotspot(
        partKey = hs.partKey,
        label = hs.label,
        screenPos = Offset(sx, sy),
        depth = w,
        accent = hs.accent,
      )
    }
  }

  fun projectWireframeSegments(
    wireframeLines: FloatArray,
    widthPx: Float,
    heightPx: Float,
    yawDeg: Float,
    pitchDeg: Float,
    zoom: Float,
  ): FloatArray {
    if (widthPx <= 1f || heightPx <= 1f || wireframeLines.size < 6) return FloatArray(0)
    val vp = FloatArray(16)
    buildCameraViewProjection(widthPx, heightPx, yawDeg, pitchDeg, zoom, vp)
    val segCount = wireframeLines.size / 6
    val out = FloatArray(segCount * 4)
    val inA = FloatArray(4)
    val inB = FloatArray(4)
    val outA = FloatArray(4)
    val outB = FloatArray(4)
    var oi = 0
    for (i in 0 until segCount) {
      val bi = i * 6
      inA[0] = wireframeLines[bi]; inA[1] = wireframeLines[bi + 1]; inA[2] = wireframeLines[bi + 2]; inA[3] = 1f
      inB[0] = wireframeLines[bi + 3]; inB[1] = wireframeLines[bi + 4]; inB[2] = wireframeLines[bi + 5]; inB[3] = 1f
      Matrix.multiplyMV(outA, 0, vp, 0, inA, 0)
      Matrix.multiplyMV(outB, 0, vp, 0, inB, 0)
      if (outA[3] <= 0.1f || outB[3] <= 0.1f) continue
      out[oi++] = ((outA[0] / outA[3]) * 0.5f + 0.5f) * widthPx
      out[oi++] = (0.5f - (outA[1] / outA[3]) * 0.5f) * heightPx
      out[oi++] = ((outB[0] / outB[3]) * 0.5f + 0.5f) * widthPx
      out[oi++] = (0.5f - (outB[1] / outB[3]) * 0.5f) * heightPx
    }
    return if (oi == out.size) out else out.copyOf(oi)
  }

  fun projectWireframeGroups(
    wireGroups: List<PevWireGroup>,
    widthPx: Float,
    heightPx: Float,
    yawDeg: Float,
    pitchDeg: Float,
    zoom: Float,
    explodePct: Float,
    wheelSpinDeg: Float,
    selectedPartKey: String?,
  ): Pair<FloatArray, FloatArray> {
    if (widthPx <= 1f || heightPx <= 1f || wireGroups.isEmpty()) return FloatArray(0) to FloatArray(0)
    val vp = FloatArray(16)
    val modelMat = FloatArray(16)
    val mvp = FloatArray(16)
    buildCameraViewProjection(widthPx, heightPx, yawDeg, pitchDeg, zoom, vp)

    val normalOut = ArrayList<Float>(4096)
    val selectedOut = ArrayList<Float>(1024)
    val inA = FloatArray(4)
    val inB = FloatArray(4)
    val outA = FloatArray(4)
    val outB = FloatArray(4)

    for (g in wireGroups) {
      Matrix.setIdentityM(modelMat, 0)
      if (explodePct > 0.005f && g.partKey != "floor") {
        Matrix.translateM(modelMat, 0, g.explodeDir.x * explodePct, g.explodeDir.y * explodePct, g.explodeDir.z * explodePct)
      }
      g.axleCenter?.let { c ->
        Matrix.translateM(modelMat, 0, c.x, c.y, c.z)
        Matrix.rotateM(modelMat, 0, -wheelSpinDeg, 0f, 0f, 1f)
        Matrix.translateM(modelMat, 0, -c.x, -c.y, -c.z)
      }
      Matrix.multiplyMM(mvp, 0, vp, 0, modelMat, 0)

      val target = if (selectedPartKey != null && g.partKey == selectedPartKey && g.partKey != "floor") selectedOut else normalOut
      val lines = g.lines
      val segCount = lines.size / 6
      for (i in 0 until segCount) {
        val bi = i * 6
        inA[0] = lines[bi]; inA[1] = lines[bi + 1]; inA[2] = lines[bi + 2]; inA[3] = 1f
        inB[0] = lines[bi + 3]; inB[1] = lines[bi + 4]; inB[2] = lines[bi + 5]; inB[3] = 1f
        Matrix.multiplyMV(outA, 0, mvp, 0, inA, 0)
        Matrix.multiplyMV(outB, 0, mvp, 0, inB, 0)
        if (outA[3] <= 0.1f || outB[3] <= 0.1f) continue
        target += ((outA[0] / outA[3]) * 0.5f + 0.5f) * widthPx
        target += (0.5f - (outA[1] / outA[3]) * 0.5f) * heightPx
        target += ((outB[0] / outB[3]) * 0.5f + 0.5f) * widthPx
        target += (0.5f - (outB[1] / outB[3]) * 0.5f) * heightPx
      }
    }
    return normalOut.toFloatArray() to selectedOut.toFloatArray()
  }
}
