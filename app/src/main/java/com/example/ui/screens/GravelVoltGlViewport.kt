package com.example.ui.screens

import android.content.Context
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
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal data class GameEntityRenderItem(
  val lane: Int,
  val z: Float, // 0 = horizon, 1 = player
  val kindCode: Int, // 0=HAY, 1=CREEK, 2=TRACTOR, 3=VOLT_CELL, 4=SOLAR_ORB
)

internal class GravelVoltGlState {
  @Volatile var vehicleName: String = "Sur-Ron Ultra Bee eMoto"
  @Volatile var lanePos: Float = 0f
  @Volatile var jumpHeight: Float = 0f
  @Volatile var turnBankDeg: Float = 0f
  @Volatile var roadScroll: Float = 0f
  @Volatile var boostActive: Boolean = false
  @Volatile var shakeAmount: Float = 0f
  @Volatile var turnSignalDir: Int = 0
  @Volatile var turnSignalActive: Boolean = false
  @Volatile var wheelSpinDeg: Float = 0f
  @Volatile var intersectionZs: FloatArray = FloatArray(0)
  @Volatile var entities: List<GameEntityRenderItem> = emptyList()
  @Volatile var onAssetStateChanged: ((Pev3dAssetLoadState) -> Unit)? = null
  @Volatile var onBackendReady: ((String) -> Unit)? = null
}

/**
 * Photorealistic Native Android OpenGL ES 3D Rural Dirt Trail, Forest & Procedural Wildflower Renderer.
 * Renders a cambered earthen dirt & gravel country road with twin worn tire ruts, rolling green meadow hills,
 * layered Blue Ridge mountain peaks, golden sun + drifting 3D volumetric clouds, multi-tiered 3D pine & oak trees,
 * swaying procedural roadside wildflowers (lupines, black-eyed Susans, poppies, daisies, ferns),
 * round rolled hay bales, rocky creek crossings, and sculpted multi-part 3D PEV rigs.
 */
private class GravelVoltGlRenderer(
  private val state: GravelVoltGlState,
) : GLSurfaceView.Renderer {

  private var programId = 0
  private var uMvpLoc = 0
  private var uModelLoc = 0
  private var uLightDirLoc = 0
  private var uCamPosLoc = 0
  private var uColorLoc = 0
  private var uEmissiveLoc = 0
  private var aPosLoc = 0
  private var aNormLoc = 0

  private lateinit var cubeBuf: FloatBuffer
  private var cubeVertCount = 0
  private lateinit var cylBuf: FloatBuffer
  private var cylVertCount = 0
  private lateinit var uprightCylBuf: FloatBuffer
  private var uprightCylVertCount = 0
  private lateinit var coneBuf: FloatBuffer
  private var coneVertCount = 0
  private lateinit var sphereBuf: FloatBuffer
  private var sphereVertCount = 0

  private class GlSculptedPart(
    val buf: FloatBuffer,
    val vertCount: Int,
    val baseRgb: FloatArray,
    val emissiveBoost: Float,
    val isWheel: Boolean,
    val axleCenter: Vec3?,
  )

  private var cachedVehicleKey = ""
  private var cachedVehicleParts: List<GlSculptedPart> = emptyList()
  private var cachedVehicleMinY = -0.45f
  private var cachedVehicleScale = 1.05f

  private val viewMatrix = FloatArray(16)
  private val projMatrix = FloatArray(16)
  private val modelMatrix = FloatArray(16)
  private val mvMatrix = FloatArray(16)
  private val mvpMatrix = FloatArray(16)
  private val startNs = System.nanoTime()

  private fun ensureVehicleMeshLoaded(vehicleName: String) {
    if (vehicleName == cachedVehicleKey && cachedVehicleParts.isNotEmpty()) return
    cachedVehicleKey = vehicleName
    val finish = Pev3dEngine.finishForModel(vehicleName, "", 72)
    val archetype =
      when (finish.blueprintType) {
        Pev3dBlueprintType.EUC_ARMORED_ROLLCAGE,
        Pev3dBlueprintType.EUC_AERO_TRAIL -> Pev3dArchetype.EUC
        Pev3dBlueprintType.SCOOTER_DUAL_STEM_OFFROAD,
        Pev3dBlueprintType.SCOOTER_TUBULAR_RACING,
        Pev3dBlueprintType.SCOOTER_MONOCOQUE_AERO -> Pev3dArchetype.SCOOTER
        Pev3dBlueprintType.EBIKE_SCRAMBLER_MOPED -> Pev3dArchetype.EBIKE
        Pev3dBlueprintType.HYDROFOIL_EFOIL -> Pev3dArchetype.HYDROFOIL
        else -> Pev3dArchetype.EMOTO
      }
    val scene = Pev3dGltfBuilder.buildSceneMeshes(
      archetype = archetype,
      finish = finish,
      lightsOn = true,
    )
    var minY = Float.POSITIVE_INFINITY
    var maxExtent = 0.5f
    val glParts = ArrayList<GlSculptedPart>(scene.parts.size)
    for (part in scene.parts) {
      val idxArr = part.indices
      val srcVerts = part.vertices
      if (idxArr.isEmpty() || srcVerts.isEmpty()) continue
      val out = FloatArray(idxArr.size * 6)
      var o = 0
      for (k in idxArr.indices) {
        val vi = (idxArr[k].toInt() and 0xFFFF) * 8
        if (vi + 5 >= srcVerts.size) continue
        val px = srcVerts[vi]
        val py = srcVerts[vi + 1]
        val pz = srcVerts[vi + 2]
        if (py < minY) minY = py
        val ax = kotlin.math.abs(px)
        if (ax > maxExtent) maxExtent = ax
        out[o] = px
        out[o + 1] = py
        out[o + 2] = pz
        out[o + 3] = srcVerts[vi + 3]
        out[o + 4] = srcVerts[vi + 4]
        out[o + 5] = srcVerts[vi + 5]
        o += 6
      }
      val em = ((part.emissiveRgb[0] + part.emissiveRgb[1] + part.emissiveRgb[2]) / 3f).coerceIn(0f, 1f)
      glParts.add(
        GlSculptedPart(
          buf = allocBuffer(out),
          vertCount = o / 6,
          baseRgb = part.baseRgb.copyOf(),
          emissiveBoost = em,
          isWheel = part.nodeName.startsWith("wheel_") && part.axleCenter != null,
          axleCenter = part.axleCenter,
        )
      )
    }
    cachedVehicleMinY = if (minY.isFinite()) minY else -0.45f
    cachedVehicleScale = (0.95f / maxExtent.coerceAtLeast(0.45f)).coerceIn(0.75f, 1.35f)
    cachedVehicleParts = glParts
  }

  override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    val glVersion = GLES20.glGetString(GLES20.GL_VERSION).orEmpty()
    val tag = if (glVersion.contains("3.")) "OPENGL ES 3.0 • RURAL TRAIL PBR" else "OPENGL ES 2.0 • RURAL TRAIL PBR"
    state.onBackendReady?.invoke(tag)
    state.onAssetStateChanged?.invoke(Pev3dAssetLoadState.Ready(state.vehicleName, "Rural Dirt Trail & Floral 3D Shader"))

    GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    GLES20.glEnable(GLES20.GL_BLEND)
    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

    val vertSrc =
      """
      uniform mat4 uMvp;
      uniform mat4 uModel;
      attribute vec3 aPos;
      attribute vec3 aNorm;
      varying vec3 vWorldPos;
      varying vec3 vWorldNorm;
      void main() {
        vec4 wp = uModel * vec4(aPos, 1.0);
        vWorldPos = wp.xyz;
        vWorldNorm = normalize((uModel * vec4(aNorm, 0.0)).xyz);
        gl_Position = uMvp * vec4(aPos, 1.0);
      }
      """.trimIndent()

    // Realistic golden-hour rural lighting with procedural soil/gravel/foliage grain & warm horizon haze
    val fragSrc =
      """
      precision mediump float;
      uniform vec3 uLightDir;
      uniform vec3 uCamPos;
      uniform vec4 uColor;
      uniform float uEmissive;
      varying vec3 vWorldPos;
      varying vec3 vWorldNorm;
      void main() {
        vec3 N = normalize(vWorldNorm);
        vec3 L = normalize(uLightDir);
        vec3 V = normalize(uCamPos - vWorldPos);
        vec3 H = normalize(L + V);
        float diff = max(dot(N, L), 0.0);
        float hemi = 0.5 + 0.5 * N.y;
        float spec = pow(max(dot(N, H), 0.0), 28.0);
        float fresnel = pow(1.0 - max(dot(N, V), 0.0), 2.4);

        // Organic micro-grain for dirt road, gravel ruts, bark, and meadow foliage
        float grainA = sin(vWorldPos.x * 13.7 + cos(vWorldPos.z * 9.3)) * cos(vWorldPos.z * 14.1);
        float grainB = sin(vWorldPos.x * 37.0 - vWorldPos.z * 29.0);
        float terrainMask = 1.0 - clamp(abs(vWorldPos.y + 0.12) * 2.5, 0.0, 1.0);
        float microVariation = (grainA * 0.055 + grainB * 0.035) * (0.45 + 0.55 * terrainMask);

        vec3 baseCol = clamp(uColor.rgb + vec3(microVariation), 0.0, 1.0);
        vec3 sunTint = vec3(1.05, 0.94, 0.80);
        vec3 skyAmbient = mix(vec3(0.24, 0.20, 0.16), vec3(0.42, 0.56, 0.72), hemi);
        vec3 lit = baseCol * (skyAmbient * 0.62 + sunTint * diff * 0.78) + vec3(0.32, 0.29, 0.24) * spec * (1.0 - terrainMask * 0.75) + baseCol * fresnel * 0.22;
        lit = mix(lit, baseCol * 1.38, clamp(uEmissive, 0.0, 1.0));

        // Atmospheric golden-hour horizon haze
        float distFog = clamp((-vWorldPos.z - 16.0) / 56.0, 0.0, 0.82);
        float horizonBlend = clamp((vWorldPos.y + 1.0) / 14.0, 0.0, 1.0);
        vec3 horizonFog = mix(vec3(0.84, 0.62, 0.42), vec3(0.32, 0.54, 0.78), horizonBlend);
        gl_FragColor = vec4(mix(lit, horizonFog, distFog * (1.0 - uEmissive * 0.65)), uColor.a);
      }
      """.trimIndent()

    val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertSrc)
    val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragSrc)
    programId = GLES20.glCreateProgram().also { p ->
      GLES20.glAttachShader(p, vs)
      GLES20.glAttachShader(p, fs)
      GLES20.glLinkProgram(p)
    }

    uMvpLoc = GLES20.glGetUniformLocation(programId, "uMvp")
    uModelLoc = GLES20.glGetUniformLocation(programId, "uModel")
    uLightDirLoc = GLES20.glGetUniformLocation(programId, "uLightDir")
    uCamPosLoc = GLES20.glGetUniformLocation(programId, "uCamPos")
    uColorLoc = GLES20.glGetUniformLocation(programId, "uColor")
    uEmissiveLoc = GLES20.glGetUniformLocation(programId, "uEmissive")
    aPosLoc = GLES20.glGetAttribLocation(programId, "aPos")
    aNormLoc = GLES20.glGetAttribLocation(programId, "aNorm")

    buildCubeMesh()
    buildCylinderMesh(24)
    buildUprightCylinderMesh(20)
    buildConeMesh(22)
    buildSphereMesh(12, 18)
  }

  override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    val w = width.coerceAtLeast(1)
    val h = height.coerceAtLeast(1)
    GLES20.glViewport(0, 0, w, h)
    Matrix.perspectiveM(projMatrix, 0, 52f, w.toFloat() / h.toFloat(), 0.25f, 130f)
  }

  override fun onDrawFrame(gl: GL10?) {
    // Rich golden-hour rural sky dome backdrop color
    GLES20.glClearColor(0.28f, 0.48f, 0.74f, 1f)
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
    if (programId == 0) return

    GLES20.glUseProgram(programId)
    val t = ((System.nanoTime() - startNs) / 1_000_000_000.0).toFloat()

    val laneX = state.lanePos * 2.15f
    val shake = state.shakeAmount
    val shakeX = if (shake > 0.01f) sin(t * 58f) * 0.22f * shake else 0f
    val shakeY = if (shake > 0.01f) cos(t * 47f) * 0.16f * shake else 0f
    val camX = laneX * 0.42f + shakeX
    val camY = 3.15f + state.jumpHeight * 0.35f + shakeY
    val camZ = 5.4f
    Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, laneX * 0.55f, 0.95f, -18.0f, 0f, 1f, 0f)

    // Warm golden-hour sun angle
    GLES20.glUniform3f(uLightDirLoc, -0.38f, 0.78f, 0.48f)
    GLES20.glUniform3f(uCamPosLoc, camX, camY, camZ)

    // =========================================================================
    // 1. GOLDEN-HOUR SKY DOME, SUN DISC, DRIFTING 3D CLOUDS & BLUE RIDGE MOUNTAINS
    // =========================================================================
    // Upper sky & warm golden horizon bands
    drawBox(0f, 18.0f, -78f, 150f, 22.0f, 0.5f, 0.25f, 0.47f, 0.76f, 1f, 0.65f)
    drawBox(0f, 7.2f, -76f, 150f, 10.5f, 0.5f, 0.62f, 0.66f, 0.78f, 0.92f, 0.65f)
    drawBox(0f, 2.4f, -74f, 150f, 5.2f, 0.5f, 0.94f, 0.68f, 0.42f, 0.92f, 0.78f)

    // Glowing Golden Sun Disc & Soft Corona Halo
    drawSphere(-14.5f, 13.5f, -71f, 4.2f, 4.2f, 1.2f, 1.0f, 0.95f, 0.72f, 1f, 1.0f)
    drawSphere(-14.5f, 13.5f, -71.5f, 7.8f, 7.8f, 1.0f, 1.0f, 0.76f, 0.40f, 0.42f, 0.95f)

    // Drifting 3D Volumetric Cloud Clusters in the Sky
    for (c in 0..5) {
      val driftX = ((c * 19f - 45f + t * (0.45f + (c % 3) * 0.18f) + 90f) % 90f) - 45f
      val cy = 11.2f + (c % 3) * 2.4f
      val cz = -62f + (c % 2) * 8f
      drawSphere(driftX, cy, cz, 5.6f, 1.85f, 2.4f, 0.98f, 0.95f, 0.90f, 0.92f, 0.55f)
      drawSphere(driftX - 2.2f, cy - 0.35f, cz + 0.3f, 3.8f, 1.45f, 2.0f, 0.95f, 0.88f, 0.82f, 0.90f, 0.48f)
      drawSphere(driftX + 2.4f, cy - 0.28f, cz + 0.2f, 4.1f, 1.50f, 2.1f, 1.0f, 0.93f, 0.86f, 0.90f, 0.52f)
    }

    // Layered Blue-Ridge & Forest Mountain Peaks on the Horizon
    val mountainPeaks = floatArrayOf(-44f, -31f, -19f, -7f, 6f, 18f, 31f, 44f)
    for (mIdx in mountainPeaks.indices) {
      val mx = mountainPeaks[mIdx]
      val mh = 7.5f + (mIdx % 3) * 2.8f
      val mw = 18f + (mIdx % 2) * 6f
      drawCone(mx, mh * 0.42f - 0.4f, -67f, mw, mh, mw * 0.65f, 0.24f, 0.34f, 0.48f, 1f, 0.15f)
      // Closer forested foothills
      drawSphere(mx + 4.5f, -0.4f, -59f, 14f, 4.6f, 8f, 0.18f, 0.33f, 0.22f, 1f, 0.05f)
    }

    // =========================================================================
    // 2. CAMBERED RURAL DIRT & GRAVEL ROAD, TWIN TIRE RUTS & MEADOW SHOULDERS
    // =========================================================================
    // Lush green rural valley meadow floor
    drawBox(0f, -0.34f, -28f, 95f, 0.22f, 88f, 0.17f, 0.35f, 0.15f, 1f, 0f)
    // Grassy roadside berms
    drawBox(-6.6f, -0.22f, -28f, 4.8f, 0.18f, 88f, 0.22f, 0.43f, 0.18f, 1f, 0f)
    drawBox(6.6f, -0.22f, -28f, 4.8f, 0.18f, 88f, 0.22f, 0.43f, 0.18f, 1f, 0f)
    // Sandy ochre gravel trail shoulders
    drawBox(-4.15f, -0.17f, -28f, 1.15f, 0.16f, 88f, 0.56f, 0.43f, 0.28f, 1f, 0f)
    drawBox(4.15f, -0.17f, -28f, 1.15f, 0.16f, 88f, 0.56f, 0.43f, 0.28f, 1f, 0f)
    // Main compacted sienna dirt & clay country roadbed
    drawBox(0f, -0.16f, -28f, 7.5f, 0.16f, 88f, 0.45f, 0.32f, 0.20f, 1f, 0f)
    // Worn twin/triple dirt tire ruts along the 3 riding lanes (-2.15, 0, +2.15)
    drawBox(-2.15f, -0.075f, -28f, 0.78f, 0.02f, 88f, 0.33f, 0.23f, 0.14f, 1f, 0f)
    drawBox(0.0f, -0.075f, -28f, 0.78f, 0.02f, 88f, 0.35f, 0.24f, 0.15f, 1f, 0f)
    drawBox(2.15f, -0.075f, -28f, 0.78f, 0.02f, 88f, 0.33f, 0.23f, 0.14f, 1f, 0f)
    // Raised center gravel & wild grass crowns between tire tracks
    drawBox(-1.10f, -0.068f, -28f, 0.42f, 0.03f, 88f, 0.52f, 0.39f, 0.25f, 1f, 0f)
    drawBox(1.10f, -0.068f, -28f, 0.42f, 0.03f, 88f, 0.52f, 0.39f, 0.25f, 1f, 0f)

    // =========================================================================
    // 3. MULTI-TIERED 3D PINE/OAK FOREST TREES & PROCEDURAL ROADSIDE WILDFLOWERS
    // =========================================================================
    val scrollOff = (state.roadScroll % 1f) * 4.5f
    for (i in 0..15) {
      val zPos = 4f - i * 4.5f + scrollOff
      val seed = i * 37

      // Crushed gravel patches & rustic center grass tufts scrolling down the dirt road
      val pebbleOffset = ((i % 3) - 1) * 1.1f
      drawSphere(pebbleOffset, -0.06f, zPos, 0.55f, 0.03f, 1.35f, 0.58f, 0.45f, 0.31f, 1f, 0f)
      drawSphere(-3.75f, -0.06f, zPos + 1.1f, 0.45f, 0.04f, 0.95f, 0.62f, 0.48f, 0.33f, 1f, 0f)
      drawSphere(3.75f, -0.06f, zPos - 0.8f, 0.45f, 0.04f, 0.95f, 0.62f, 0.48f, 0.33f, 1f, 0f)

      // Rolling grassy meadow knolls on both sides
      val hillScale = 1.0f + (i % 3) * 0.32f
      drawSphere(-13.5f - (i % 2) * 3f, -0.35f, zPos, 7.5f * hillScale, 1.8f * hillScale, 5.2f, 0.20f, 0.40f, 0.17f, 1f, 0f)
      drawSphere(13.5f + (i % 2) * 3f, -0.35f, zPos, 7.5f * hillScale, 1.8f * hillScale, 5.2f, 0.20f, 0.40f, 0.17f, 1f, 0f)

      // Multi-Tiered 3D Pine/Spruce & Broadleaf Oak Trees on Left & Right Forest Line
      val leftTreeX = -7.4f - (i % 3) * 1.45f
      val rightTreeX = 7.4f + ((i + 1) % 3) * 1.45f
      val treeScale = 0.88f + (i % 4) * 0.16f
      if (i % 3 != 1) {
        drawRealisticPineTree(leftTreeX, zPos, treeScale)
      } else {
        drawRealisticOakTree(leftTreeX - 0.4f, zPos, treeScale * 1.05f)
      }
      if (i % 3 != 2) {
        drawRealisticPineTree(rightTreeX, zPos - 0.6f, treeScale * 0.96f)
      } else {
        drawRealisticOakTree(rightTreeX + 0.4f, zPos - 0.6f, treeScale * 1.04f)
      }

      // Secondary deeper forest row for dense woodland depth
      val deepScale = 1.12f + (i % 3) * 0.18f
      drawRealisticPineTree(-12.2f - (i % 2) * 2.2f, zPos - 2.1f, deepScale)
      drawRealisticPineTree(12.2f + (i % 2) * 2.2f, zPos + 1.8f, deepScale)

      // Split-rail cedar country fence posts & rails along alternating trail sections
      if (i % 2 == 0) {
        drawUprightCyl(-5.15f, 0.35f, zPos, 0.16f, 0.95f, 0.16f, 0.42f, 0.29f, 0.18f, 1f, 0f)
        drawUprightCyl(5.15f, 0.35f, zPos, 0.16f, 0.95f, 0.16f, 0.42f, 0.29f, 0.18f, 1f, 0f)
        drawBox(-5.15f, 0.56f, zPos - 2.25f, 0.08f, 0.09f, 4.5f, 0.46f, 0.32f, 0.20f, 1f, 0f)
        drawBox(5.15f, 0.56f, zPos - 2.25f, 0.08f, 0.09f, 4.5f, 0.46f, 0.32f, 0.20f, 1f, 0f)
      }

      // PROCEDURAL ROADSIDE FLORALS, WILDFLOWERS & SWAYING FERNS
      val windSway = sin(t * 2.7f + zPos * 0.45f) * 0.07f
      drawProceduralFloralCluster(
        baseX = -4.55f - (i % 2) * 0.45f,
        baseZ = zPos + 0.35f,
        variant = seed % 4,
        sway = windSway,
      )
      drawProceduralFloralCluster(
        baseX = 4.55f + ((i + 1) % 2) * 0.45f,
        baseZ = zPos - 0.35f,
        variant = (seed + 2) % 4,
        sway = -windSway,
      )
      // Outer meadow wildflower patches & fern clusters
      drawProceduralFloralCluster(
        baseX = -6.05f - (i % 3) * 0.35f,
        baseZ = zPos - 1.4f,
        variant = (seed + 1) % 4,
        sway = windSway * 1.2f,
      )
      drawProceduralFloralCluster(
        baseX = 6.05f + (i % 3) * 0.35f,
        baseZ = zPos + 1.4f,
        variant = (seed + 3) % 4,
        sway = -windSway * 1.2f,
      )
    }

    // =========================================================================
    // 4. RURAL CROSS-TRAIL INTERSECTIONS
    // =========================================================================
    val inters = state.intersectionZs
    for (iz in inters) {
      val worldZ = -58f + iz * 60f
      // Cross-cutting dirt & gravel country lane with wooden signpost
      drawBox(0f, -0.08f, worldZ, 30f, 0.11f, 4.0f, 0.48f, 0.35f, 0.22f, 1f, 0.02f)
      drawBox(0f, -0.065f, worldZ, 30f, 0.02f, 1.6f, 0.36f, 0.25f, 0.15f, 1f, 0f)
      drawUprightCyl(-4.8f, 0.95f, worldZ - 1.8f, 0.18f, 2.1f, 0.18f, 0.40f, 0.27f, 0.16f, 1f, 0f)
      drawBox(-4.4f, 1.72f, worldZ - 1.8f, 1.15f, 0.28f, 0.08f, 0.16f, 0.48f, 0.26f, 1f, 0.25f)
      drawUprightCyl(4.8f, 0.95f, worldZ - 1.8f, 0.18f, 2.1f, 0.18f, 0.40f, 0.27f, 0.16f, 1f, 0f)
      drawBox(4.4f, 1.72f, worldZ - 1.8f, 1.15f, 0.28f, 0.08f, 0.16f, 0.48f, 0.26f, 1f, 0.25f)
    }

    // =========================================================================
    // 5. ORGANIC 3D TRAIL HAZARDS & COLLECTIBLES (ZERO BLOCKY CUBES)
    // =========================================================================
    val items = state.entities
    for (item in items) {
      val ex = item.lane * 2.15f
      val ez = -58f + item.z * 58f
      when (item.kindCode) {
        0 -> { // ROLLED ROUND HAY BALE WITH TWINE BINDINGS
          drawPartRotated(ex, 0.52f, ez, 1.18f, 1.04f, 1.04f, 0f, 0f, 0.85f, 0.66f, 0.22f, 0.08f, isCyl = true)
          // Inner spiral straw core & twin baling twine straps
          drawPartRotated(ex, 0.52f, ez, 1.22f, 0.56f, 0.56f, 0f, 0f, 0.72f, 0.53f, 0.16f, 0.05f, isCyl = true)
          drawPartRotated(ex - 0.34f, 0.52f, ez, 0.06f, 1.07f, 1.07f, 0f, 0f, 0.52f, 0.36f, 0.14f, 0.05f, isCyl = true)
          drawPartRotated(ex + 0.34f, 0.52f, ez, 0.06f, 1.07f, 1.07f, 0f, 0f, 0.52f, 0.36f, 0.14f, 0.05f, isCyl = true)
        }
        1 -> { // SPARKLING ROCKY CREEK CROSSING WITH RIVER STONES
          drawSphere(ex, -0.02f, ez, 2.25f, 0.10f, 2.35f, 0.14f, 0.65f, 0.92f, 0.88f, 0.45f)
          // Shimmering white water foam ripples
          val ripple = sin(t * 5f + ez) * 0.12f
          drawSphere(ex + ripple, 0.02f, ez - 0.25f, 1.45f, 0.05f, 0.45f, 0.82f, 0.96f, 1.0f, 0.78f, 0.75f)
          // Rounded mossy river stones flanking the creek ford
          drawSphere(ex - 0.88f, 0.12f, ez + 0.35f, 0.46f, 0.28f, 0.42f, 0.45f, 0.48f, 0.50f, 1f, 0f)
          drawSphere(ex + 0.86f, 0.14f, ez - 0.30f, 0.52f, 0.32f, 0.44f, 0.42f, 0.46f, 0.48f, 1f, 0f)
        }
        2 -> { // DETAILED RURAL FARM TRACTOR WITH ROUND LUGGED WHEELS
          // Hood & engine block
          drawSphere(ex, 0.74f, ez + 0.28f, 0.96f, 0.72f, 1.25f, 0.16f, 0.64f, 0.22f, 1f, 0.12f)
          // Cab & roof canopy
          drawBox(ex, 1.38f, ez - 0.28f, 1.02f, 0.78f, 0.92f, 0.16f, 0.58f, 0.20f, 1f, 0.10f)
          drawBox(ex, 1.82f, ez - 0.28f, 1.14f, 0.12f, 1.04f, 0.94f, 0.82f, 0.18f, 1f, 0.25f)
          // Vertical exhaust stack
          drawUprightCyl(ex + 0.36f, 1.38f, ez + 0.45f, 0.11f, 0.85f, 0.11f, 0.18f, 0.20f, 0.22f, 1f, 0f)
          // Large rear tractor tires + yellow rims
          drawPartRotated(ex - 0.66f, 0.56f, ez - 0.32f, 0.34f, 1.12f, 1.12f, 0f, 0f, 0.11f, 0.12f, 0.14f, 0f, isCyl = true)
          drawPartRotated(ex + 0.66f, 0.56f, ez - 0.32f, 0.34f, 1.12f, 1.12f, 0f, 0f, 0.11f, 0.12f, 0.14f, 0f, isCyl = true)
          drawPartRotated(ex - 0.68f, 0.56f, ez - 0.32f, 0.36f, 0.58f, 0.58f, 0f, 0f, 0.95f, 0.80f, 0.15f, 0.2f, isCyl = true)
          drawPartRotated(ex + 0.68f, 0.56f, ez - 0.32f, 0.36f, 0.58f, 0.58f, 0f, 0f, 0.95f, 0.80f, 0.15f, 0.2f, isCyl = true)
          // Front steering tires
          drawPartRotated(ex - 0.56f, 0.34f, ez + 0.68f, 0.24f, 0.68f, 0.68f, 0f, 0f, 0.11f, 0.12f, 0.14f, 0f, isCyl = true)
          drawPartRotated(ex + 0.56f, 0.34f, ez + 0.68f, 0.24f, 0.68f, 0.68f, 0f, 0f, 0.11f, 0.12f, 0.14f, 0f, isCyl = true)
        }
        3 -> { // 21700 LITHIUM VOLT CELL (CYLINDRICAL BATTERY CELL)
          val bob = sin(t * 6f + ez) * 0.15f
          drawUprightCyl(ex, 0.68f + bob, ez, 0.46f, 0.76f, 0.46f, 0.10f, 0.92f, 0.86f, 1f, 0.85f)
          drawUprightCyl(ex, 1.09f + bob, ez, 0.26f, 0.10f, 0.26f, 0.92f, 0.96f, 1.0f, 1f, 0.95f)
          drawSphere(ex, 0.68f + bob, ez, 0.68f, 0.22f, 0.68f, 0.22f, 1.0f, 0.55f, 0.55f, 1.0f)
        }
        4 -> { // SOLAR ORB (GLOWING GOLDEN SPHERE + CORONA)
          val bob = sin(t * 7f + ez) * 0.18f
          drawSphere(ex, 0.78f + bob, ez, 0.58f, 0.58f, 0.58f, 1.0f, 0.86f, 0.18f, 1f, 0.98f)
          drawSphere(ex, 0.78f + bob, ez, 0.84f, 0.84f, 0.84f, 1.0f, 0.64f, 0.12f, 0.38f, 0.95f)
        }
      }
    }

    // =========================================================================
    // 6. HIGH-POLY SCULPTED 3D PLAYER PEV RIG (SHARED WITH 3D LABS ENGINE)
    // =========================================================================
    val px = laneX
    val groundY = -0.06f
    val py = groundY + state.jumpHeight * 2.2f
    val pz = 0f
    val bankRad = state.turnBankDeg
    ensureVehicleMeshLoaded(state.vehicleName)

    // Grounded contact shadow directly underneath the PEV wheels on the dirt trail
    val shadowScale = (1f - state.jumpHeight * 0.45f).coerceIn(0.45f, 1f)
    drawSphere(px, -0.055f, pz, 0.95f * shadowScale, 0.02f, 2.05f * shadowScale, 0.12f, 0.09f, 0.06f, 0.62f, 0f)

    // Soft warm headlight beam pool on dirt road ahead
    drawSphere(px, -0.05f, pz - 4.4f, 2.3f, 0.03f, 6.4f, 0.98f, 0.95f, 0.82f, 0.20f, 0.92f)

    // Render every sculpted high-poly mesh part from Pev3dGltfBuilder
    val vehScale = cachedVehicleScale
    val liftY = py - cachedVehicleMinY * vehScale
    for (part in cachedVehicleParts) {
      Matrix.setIdentityM(modelMatrix, 0)
      Matrix.translateM(modelMatrix, 0, px, liftY, pz)
      if (bankRad != 0f) {
        Matrix.rotateM(modelMatrix, 0, -bankRad * 1.15f, 0f, 0f, 1f)
      }
      // Rotate +X forward (3D Lab frame) into -Z forward (dirt trail heading)
      Matrix.rotateM(modelMatrix, 0, 90f, 0f, 1f, 0f)
      Matrix.scaleM(modelMatrix, 0, vehScale, vehScale, vehScale)
      if (part.isWheel && part.axleCenter != null) {
        val ac = part.axleCenter
        Matrix.translateM(modelMatrix, 0, ac.x, ac.y, ac.z)
        Matrix.rotateM(modelMatrix, 0, -state.wheelSpinDeg, 0f, 0f, 1f)
        Matrix.translateM(modelMatrix, 0, -ac.x, -ac.y, -ac.z)
      }
      val r = part.baseRgb[0]
      val g = part.baseRgb[1]
      val b = part.baseRgb[2]
      submitBuffer(part.buf, part.vertCount, r, g, b, 1f, part.emissiveBoost)
    }

    // Dynamic gravel & dirt dust rooster-tail kicked up behind the rear tire when grounded
    if (state.jumpHeight < 0.08f) {
      for (d in 0..4) {
        val phase = ((t * 5.2f + d * 0.22f) % 1f)
        val dustZ = pz + 0.95f + phase * 2.15f
        val dustY = 0.08f + phase * 0.42f
        val dustX = px + sin(t * 9f + d * 1.7f) * (0.09f + phase * 0.22f)
        val dustSize = 0.16f + phase * 0.34f
        val dustAlpha = ((1f - phase) * 0.45f).coerceIn(0.05f, 0.45f)
        drawSphere(dustX, dustY, dustZ, dustSize, dustSize * 0.72f, dustSize, 0.62f, 0.47f, 0.31f, dustAlpha, 0.05f)
      }
    }

    if (state.boostActive) {
      drawSphere(px, py + 0.45f, pz + 1.18f, 0.30f, 0.26f, 0.72f, 0.15f, 0.95f, 1.0f, 0.85f, 1.0f)
    }

    if (state.turnSignalActive && state.turnSignalDir != 0) {
      val sx = if (state.turnSignalDir < 0) px - 0.44f else px + 0.44f
      drawSphere(sx, py + 0.95f, pz - 0.46f, 0.14f, 0.10f, 0.10f, 1.0f, 0.68f, 0.05f, 1f, 1.0f)
      drawSphere(sx * 0.65f, py + 0.52f, pz + 0.72f, 0.12f, 0.09f, 0.09f, 1.0f, 0.68f, 0.05f, 1f, 1.0f)
    }
  }

  /**
   * Multi-Tiered Realistic Conifer Pine / Spruce Tree with cylindrical timber trunk and 4 overlapping conical boughs.
   */
  private fun drawRealisticPineTree(x: Float, z: Float, scale: Float) {
    // Cylindrical cedar/pine bark trunk
    drawUprightCyl(x, 0.55f * scale, z, 0.34f * scale, 1.35f * scale, 0.34f * scale, 0.36f, 0.23f, 0.13f, 1f, 0f)
    // 4 layered conical evergreen tiers
    drawCone(x, 1.25f * scale, z, 2.25f * scale, 1.45f * scale, 2.25f * scale, 0.10f, 0.29f, 0.14f, 1f, 0.02f)
    drawCone(x, 2.05f * scale, z, 1.85f * scale, 1.35f * scale, 1.85f * scale, 0.13f, 0.35f, 0.17f, 1f, 0.03f)
    drawCone(x, 2.78f * scale, z, 1.40f * scale, 1.20f * scale, 1.40f * scale, 0.16f, 0.41f, 0.19f, 1f, 0.04f)
    drawCone(x, 3.42f * scale, z, 0.92f * scale, 0.98f * scale, 0.92f * scale, 0.20f, 0.47f, 0.22f, 1f, 0.06f)
  }

  /**
   * Realistic Broadleaf Country Oak / Maple Tree with bark trunk and multi-sphere sunlit foliage crown.
   */
  private fun drawRealisticOakTree(x: Float, z: Float, scale: Float) {
    drawUprightCyl(x, 0.68f * scale, z, 0.42f * scale, 1.55f * scale, 0.42f * scale, 0.38f, 0.25f, 0.15f, 1f, 0f)
    drawSphere(x, 2.05f * scale, z, 2.25f * scale, 1.75f * scale, 2.25f * scale, 0.15f, 0.38f, 0.16f, 1f, 0.03f)
    drawSphere(x - 0.68f * scale, 1.78f * scale, z + 0.35f * scale, 1.55f * scale, 1.35f * scale, 1.55f * scale, 0.12f, 0.33f, 0.14f, 1f, 0.02f)
    drawSphere(x + 0.72f * scale, 1.85f * scale, z - 0.30f * scale, 1.60f * scale, 1.40f * scale, 1.60f * scale, 0.22f, 0.46f, 0.18f, 1f, 0.05f)
    drawSphere(x, 2.72f * scale, z, 1.55f * scale, 1.25f * scale, 1.55f * scale, 0.26f, 0.52f, 0.20f, 1f, 0.06f)
  }

  /**
   * Procedural Roadside Wildflower, Lupine, Black-Eyed Susan, Poppy, Daisy & Fern Cluster swaying in the breeze.
   */
  private fun drawProceduralFloralCluster(baseX: Float, baseZ: Float, variant: Int, sway: Float) {
    // Lush green basal fern / meadow grass mound
    drawSphere(baseX, -0.02f, baseZ, 0.72f, 0.26f, 0.72f, 0.18f, 0.46f, 0.18f, 1f, 0.02f)
    drawCone(baseX - 0.22f + sway * 0.5f, 0.18f, baseZ + 0.15f, 0.16f, 0.45f, 0.16f, 0.22f, 0.54f, 0.20f, 1f, 0.04f)
    drawCone(baseX + 0.22f + sway * 0.5f, 0.16f, baseZ - 0.15f, 0.16f, 0.42f, 0.16f, 0.20f, 0.50f, 0.18f, 1f, 0.04f)

    // 3 wildflower stems & blossoms per cluster
    for (f in -1..1) {
      val fx = baseX + f * 0.26f + sway
      val fz = baseZ + (f * 0.19f)
      val stemH = 0.42f + (f + 1) * 0.08f
      drawUprightCyl(fx, stemH * 0.5f - 0.04f, fz, 0.045f, stemH, 0.045f, 0.22f, 0.55f, 0.20f, 1f, 0.02f)

      when (variant) {
        0 -> { // Golden Black-Eyed Susan / Wild Sunflower
          drawSphere(fx, stemH, fz, 0.28f, 0.09f, 0.28f, 1.0f, 0.80f, 0.10f, 1f, 0.35f)
          drawSphere(fx, stemH + 0.04f, fz, 0.11f, 0.08f, 0.11f, 0.26f, 0.14f, 0.06f, 1f, 0.05f)
        }
        1 -> { // Tall Purple & Violet Country Lupine Spire
          drawSphere(fx, stemH - 0.04f, fz, 0.17f, 0.14f, 0.17f, 0.56f, 0.26f, 0.92f, 1f, 0.32f)
          drawSphere(fx + sway * 0.3f, stemH + 0.08f, fz, 0.13f, 0.13f, 0.13f, 0.68f, 0.40f, 0.98f, 1f, 0.38f)
          drawSphere(fx + sway * 0.5f, stemH + 0.18f, fz, 0.09f, 0.11f, 0.09f, 0.82f, 0.58f, 1.0f, 1f, 0.45f)
        }
        2 -> { // White Meadow Daisy with Golden Center
          drawSphere(fx, stemH, fz, 0.26f, 0.08f, 0.26f, 0.98f, 0.98f, 1.0f, 1f, 0.35f)
          drawSphere(fx, stemH + 0.035f, fz, 0.10f, 0.07f, 0.10f, 0.98f, 0.78f, 0.12f, 1f, 0.40f)
        }
        else -> { // Crimson Wild Poppy / Clover Bloom
          drawSphere(fx, stemH, fz, 0.24f, 0.12f, 0.24f, 0.96f, 0.22f, 0.28f, 1f, 0.35f)
          drawSphere(fx, stemH + 0.04f, fz, 0.09f, 0.07f, 0.09f, 0.15f, 0.12f, 0.14f, 1f, 0.05f)
        }
      }
    }
  }

  private fun drawBox(
    x: Float,
    y: Float,
    z: Float,
    sx: Float,
    sy: Float,
    sz: Float,
    r: Float,
    g: Float,
    b: Float,
    a: Float,
    emissive: Float,
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
    submitBuffer(cubeBuf, cubeVertCount, r, g, b, a, emissive)
  }

  private fun drawUprightCyl(
    x: Float,
    y: Float,
    z: Float,
    sx: Float,
    sy: Float,
    sz: Float,
    r: Float,
    g: Float,
    b: Float,
    a: Float,
    emissive: Float,
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
    submitBuffer(uprightCylBuf, uprightCylVertCount, r, g, b, a, emissive)
  }

  private fun drawCone(
    x: Float,
    y: Float,
    z: Float,
    sx: Float,
    sy: Float,
    sz: Float,
    r: Float,
    g: Float,
    b: Float,
    a: Float,
    emissive: Float,
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
    submitBuffer(coneBuf, coneVertCount, r, g, b, a, emissive)
  }

  private fun drawSphere(
    x: Float,
    y: Float,
    z: Float,
    sx: Float,
    sy: Float,
    sz: Float,
    r: Float,
    g: Float,
    b: Float,
    a: Float,
    emissive: Float,
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
    submitBuffer(sphereBuf, sphereVertCount, r, g, b, a, emissive)
  }

  private fun drawPartRotated(
    x: Float,
    y: Float,
    z: Float,
    sx: Float,
    sy: Float,
    sz: Float,
    spinDeg: Float,
    bankDeg: Float,
    r: Float,
    g: Float,
    b: Float,
    emissive: Float,
    isCyl: Boolean = false,
    isSphere: Boolean = false,
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    if (bankDeg != 0f) Matrix.rotateM(modelMatrix, 0, -bankDeg, 0f, 0f, 1f)
    if (spinDeg != 0f) Matrix.rotateM(modelMatrix, 0, -spinDeg, 1f, 0f, 0f)
    if (isCyl) Matrix.rotateM(modelMatrix, 0, 90f, 0f, 1f, 0f)
    Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
    when {
      isSphere -> submitBuffer(sphereBuf, sphereVertCount, r, g, b, 1f, emissive)
      isCyl -> submitBuffer(cylBuf, cylVertCount, r, g, b, 1f, emissive)
      else -> submitBuffer(cubeBuf, cubeVertCount, r, g, b, 1f, emissive)
    }
  }

  private fun submitBuffer(
    buf: FloatBuffer,
    vertCount: Int,
    r: Float,
    g: Float,
    b: Float,
    a: Float,
    emissive: Float,
  ) {
    Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0)
    Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvMatrix, 0)
    GLES20.glUniformMatrix4fv(uMvpLoc, 1, false, mvpMatrix, 0)
    GLES20.glUniformMatrix4fv(uModelLoc, 1, false, modelMatrix, 0)
    GLES20.glUniform4f(uColorLoc, r, g, b, a)
    GLES20.glUniform1f(uEmissiveLoc, emissive)

    val stride = 6 * 4
    buf.position(0)
    GLES20.glEnableVertexAttribArray(aPosLoc)
    GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, stride, buf)
    buf.position(3)
    GLES20.glEnableVertexAttribArray(aNormLoc)
    GLES20.glVertexAttribPointer(aNormLoc, 3, GLES20.GL_FLOAT, false, stride, buf)
    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertCount)
    GLES20.glDisableVertexAttribArray(aPosLoc)
    GLES20.glDisableVertexAttribArray(aNormLoc)
  }

  private fun compileShader(type: Int, src: String): Int {
    val shader = GLES20.glCreateShader(type)
    GLES20.glShaderSource(shader, src)
    GLES20.glCompileShader(shader)
    return shader
  }

  private fun buildCubeMesh() {
    val d = floatArrayOf(
      -0.5f, -0.5f, 0.5f, 0f, 0f, 1f, 0.5f, -0.5f, 0.5f, 0f, 0f, 1f, 0.5f, 0.5f, 0.5f, 0f, 0f, 1f,
      -0.5f, -0.5f, 0.5f, 0f, 0f, 1f, 0.5f, 0.5f, 0.5f, 0f, 0f, 1f, -0.5f, 0.5f, 0.5f, 0f, 0f, 1f,
      0.5f, -0.5f, -0.5f, 0f, 0f, -1f, -0.5f, -0.5f, -0.5f, 0f, 0f, -1f, -0.5f, 0.5f, -0.5f, 0f, 0f, -1f,
      0.5f, -0.5f, -0.5f, 0f, 0f, -1f, -0.5f, 0.5f, -0.5f, 0f, 0f, -1f, 0.5f, 0.5f, -0.5f, 0f, 0f, -1f,
      -0.5f, 0.5f, 0.5f, 0f, 1f, 0f, 0.5f, 0.5f, 0.5f, 0f, 1f, 0f, 0.5f, 0.5f, -0.5f, 0f, 1f, 0f,
      -0.5f, 0.5f, 0.5f, 0f, 1f, 0f, 0.5f, 0.5f, -0.5f, 0f, 1f, 0f, -0.5f, 0.5f, -0.5f, 0f, 1f, 0f,
      -0.5f, -0.5f, -0.5f, 0f, -1f, 0f, 0.5f, -0.5f, -0.5f, 0f, -1f, 0f, 0.5f, -0.5f, 0.5f, 0f, -1f, 0f,
      -0.5f, -0.5f, -0.5f, 0f, -1f, 0f, 0.5f, -0.5f, 0.5f, 0f, -1f, 0f, -0.5f, -0.5f, 0.5f, 0f, -1f, 0f,
      0.5f, -0.5f, 0.5f, 1f, 0f, 0f, 0.5f, -0.5f, -0.5f, 1f, 0f, 0f, 0.5f, 0.5f, -0.5f, 1f, 0f, 0f,
      0.5f, -0.5f, 0.5f, 1f, 0f, 0f, 0.5f, 0.5f, -0.5f, 1f, 0f, 0f, 0.5f, 0.5f, 0.5f, 1f, 0f, 0f,
      -0.5f, -0.5f, -0.5f, -1f, 0f, 0f, -0.5f, -0.5f, 0.5f, -1f, 0f, 0f, -0.5f, 0.5f, 0.5f, -1f, 0f, 0f,
      -0.5f, -0.5f, -0.5f, -1f, 0f, 0f, -0.5f, 0.5f, 0.5f, -1f, 0f, 0f, -0.5f, 0.5f, -0.5f, -1f, 0f, 0f,
    )
    cubeVertCount = d.size / 6
    cubeBuf = allocBuffer(d)
  }

  private fun buildCylinderMesh(segs: Int) {
    val verts = ArrayList<Float>(segs * 12 * 6)
    for (i in 0 until segs) {
      val a0 = (i * 2.0 * PI / segs).toFloat()
      val a1 = ((i + 1) * 2.0 * PI / segs).toFloat()
      val x0 = cos(a0) * 0.5f
      val y0 = sin(a0) * 0.5f
      val x1 = cos(a1) * 0.5f
      val y1 = sin(a1) * 0.5f
      val nx0 = cos(a0)
      val ny0 = sin(a0)
      val nx1 = cos(a1)
      val ny1 = sin(a1)
      verts.addAll(listOf(x0, y0, 0.5f, nx0, ny0, 0f, x1, y1, 0.5f, nx1, ny1, 0f, x1, y1, -0.5f, nx1, ny1, 0f))
      verts.addAll(listOf(x0, y0, 0.5f, nx0, ny0, 0f, x1, y1, -0.5f, nx1, ny1, 0f, x0, y0, -0.5f, nx0, ny0, 0f))
      verts.addAll(listOf(0f, 0f, 0.5f, 0f, 0f, 1f, x0, y0, 0.5f, 0f, 0f, 1f, x1, y1, 0.5f, 0f, 0f, 1f))
      verts.addAll(listOf(0f, 0f, -0.5f, 0f, 0f, -1f, x1, y1, -0.5f, 0f, 0f, -1f, x0, y0, -0.5f, 0f, 0f, -1f))
    }
    cylVertCount = verts.size / 6
    cylBuf = allocBuffer(verts.toFloatArray())
  }

  /**
   * Upright Y-axis cylinder (y in -0.5..+0.5, radius 0.5) for tree trunks, fence posts, and wildflower stems.
   */
  private fun buildUprightCylinderMesh(segs: Int) {
    val verts = ArrayList<Float>(segs * 12 * 6)
    for (i in 0 until segs) {
      val a0 = (i * 2.0 * PI / segs).toFloat()
      val a1 = ((i + 1) * 2.0 * PI / segs).toFloat()
      val nx0 = cos(a0)
      val nz0 = sin(a0)
      val nx1 = cos(a1)
      val nz1 = sin(a1)
      val x0 = nx0 * 0.5f
      val z0 = nz0 * 0.5f
      val x1 = nx1 * 0.5f
      val z1 = nz1 * 0.5f
      verts.addAll(listOf(x0, 0.5f, z0, nx0, 0f, nz0, x1, 0.5f, z1, nx1, 0f, nz1, x1, -0.5f, z1, nx1, 0f, nz1))
      verts.addAll(listOf(x0, 0.5f, z0, nx0, 0f, nz0, x1, -0.5f, z1, nx1, 0f, nz1, x0, -0.5f, z0, nx0, 0f, nz0))
      verts.addAll(listOf(0f, 0.5f, 0f, 0f, 1f, 0f, x0, 0.5f, z0, 0f, 1f, 0f, x1, 0.5f, z1, 0f, 1f, 0f))
      verts.addAll(listOf(0f, -0.5f, 0f, 0f, -1f, 0f, x1, -0.5f, z1, 0f, -1f, 0f, x0, -0.5f, z0, 0f, -1f, 0f))
    }
    uprightCylVertCount = verts.size / 6
    uprightCylBuf = allocBuffer(verts.toFloatArray())
  }

  /**
   * Upright Y-axis cone (base at y = -0.5 with radius 0.5, apex at y = +0.5) for pine tree tiers, ferns, and mountains.
   */
  private fun buildConeMesh(segs: Int) {
    val verts = ArrayList<Float>(segs * 6 * 6)
    val slopeY = 0.4472136f
    val slopeR = 0.8944272f
    for (i in 0 until segs) {
      val a0 = (i * 2.0 * PI / segs).toFloat()
      val a1 = ((i + 1) * 2.0 * PI / segs).toFloat()
      val c0 = cos(a0)
      val s0 = sin(a0)
      val c1 = cos(a1)
      val s1 = sin(a1)
      val x0 = c0 * 0.5f
      val z0 = s0 * 0.5f
      val x1 = c1 * 0.5f
      val z1 = s1 * 0.5f
      val nx0 = c0 * slopeR
      val nz0 = s0 * slopeR
      val nx1 = c1 * slopeR
      val nz1 = s1 * slopeR
      val nxMid = ((nx0 + nx1) * 0.5f)
      val nzMid = ((nz0 + nz1) * 0.5f)
      // Side triangle
      verts.addAll(
        listOf(
          0f, 0.5f, 0f, nxMid, slopeY, nzMid,
          x0, -0.5f, z0, nx0, slopeY, nz0,
          x1, -0.5f, z1, nx1, slopeY, nz1,
        )
      )
      // Base triangle
      verts.addAll(
        listOf(
          0f, -0.5f, 0f, 0f, -1f, 0f,
          x1, -0.5f, z1, 0f, -1f, 0f,
          x0, -0.5f, z0, 0f, -1f, 0f,
        )
      )
    }
    coneVertCount = verts.size / 6
    coneBuf = allocBuffer(verts.toFloatArray())
  }

  /**
   * Smooth UV sphere mesh (radius 0.5) for broadleaf tree canopies, wildflower blooms, clouds, boulders, and sculpted bike tanks.
   */
  private fun buildSphereMesh(latBands: Int, lonBands: Int) {
    val verts = ArrayList<Float>(latBands * lonBands * 6 * 6)
    for (lat in 0 until latBands) {
      val theta0 = (lat * PI / latBands).toFloat()
      val theta1 = ((lat + 1) * PI / latBands).toFloat()
      val sinT0 = sin(theta0)
      val cosT0 = cos(theta0)
      val sinT1 = sin(theta1)
      val cosT1 = cos(theta1)
      for (lon in 0 until lonBands) {
        val phi0 = (lon * 2.0 * PI / lonBands).toFloat()
        val phi1 = ((lon + 1) * 2.0 * PI / lonBands).toFloat()
        val cosP0 = cos(phi0)
        val sinP0 = sin(phi0)
        val cosP1 = cos(phi1)
        val sinP1 = sin(phi1)

        val nx00 = cosP0 * sinT0
        val ny00 = cosT0
        val nz00 = sinP0 * sinT0

        val nx10 = cosP1 * sinT0
        val ny10 = cosT0
        val nz10 = sinP1 * sinT0

        val nx01 = cosP0 * sinT1
        val ny01 = cosT1
        val nz01 = sinP0 * sinT1

        val nx11 = cosP1 * sinT1
        val ny11 = cosT1
        val nz11 = sinP1 * sinT1

        verts.addAll(
          listOf(
            nx00 * 0.5f, ny00 * 0.5f, nz00 * 0.5f, nx00, ny00, nz00,
            nx01 * 0.5f, ny01 * 0.5f, nz01 * 0.5f, nx01, ny01, nz01,
            nx11 * 0.5f, ny11 * 0.5f, nz11 * 0.5f, nx11, ny11, nz11,
            nx00 * 0.5f, ny00 * 0.5f, nz00 * 0.5f, nx00, ny00, nz00,
            nx11 * 0.5f, ny11 * 0.5f, nz11 * 0.5f, nx11, ny11, nz11,
            nx10 * 0.5f, ny10 * 0.5f, nz10 * 0.5f, nx10, ny10, nz10,
          )
        )
      }
    }
    sphereVertCount = verts.size / 6
    sphereBuf = allocBuffer(verts.toFloatArray())
  }

  private fun allocBuffer(arr: FloatArray): FloatBuffer =
    ByteBuffer.allocateDirect(arr.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
      put(arr)
      position(0)
    }
}

@Composable
internal fun GravelVoltGlViewport(
  vehicleName: String,
  lanePos: Float,
  jumpHeight: Float,
  turnBankDeg: Float,
  roadScroll: Float,
  boostActive: Boolean,
  shakeAmount: Float,
  turnSignalDir: Int,
  turnSignalActive: Boolean,
  wheelSpinDeg: Float,
  intersectionZs: FloatArray,
  entities: List<GameEntityRenderItem>,
  onAssetStateChanged: (Pev3dAssetLoadState) -> Unit,
  onBackendReady: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val state = remember { GravelVoltGlState() }
  state.vehicleName = vehicleName
  state.lanePos = lanePos
  state.jumpHeight = jumpHeight
  state.turnBankDeg = turnBankDeg
  state.roadScroll = roadScroll
  state.boostActive = boostActive
  state.shakeAmount = shakeAmount
  state.turnSignalDir = turnSignalDir
  state.turnSignalActive = turnSignalActive
  state.wheelSpinDeg = wheelSpinDeg
  state.intersectionZs = intersectionZs
  state.entities = entities
  state.onAssetStateChanged = onAssetStateChanged
  state.onBackendReady = onBackendReady

  val renderer = remember { GravelVoltGlRenderer(state) }
  val glViewHolder = remember { arrayOfNulls<GLSurfaceView>(1) }
  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> runCatching { glViewHolder[0]?.onResume() }
        Lifecycle.Event.ON_PAUSE -> runCatching { glViewHolder[0]?.onPause() }
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      runCatching { glViewHolder[0]?.onPause() }
      glViewHolder[0] = null
    }
  }

  AndroidView(
    modifier = modifier,
    factory = { ctx: Context ->
      GLSurfaceView(ctx).apply {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY)
        glViewHolder[0] = this
      }
    },
  )
}
