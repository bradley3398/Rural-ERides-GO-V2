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
 * Crash-proof Native Android OpenGL ES 3D Twilight Road & PEV Runner Renderer.
 * Renders animated 3D perspective twilight asphalt, sunset horizon glow, lane stripes,
 * roadside pine silhouettes & solar street lamps, intersection cross-roads, collectibles,
 * hazards, headlight ground beam, and blueprint-accurate leaning PEV rigs with spinning wheels.
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

  private val viewMatrix = FloatArray(16)
  private val projMatrix = FloatArray(16)
  private val modelMatrix = FloatArray(16)
  private val mvMatrix = FloatArray(16)
  private val mvpMatrix = FloatArray(16)
  private val startNs = System.nanoTime()

  override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    val glVersion = GLES20.glGetString(GLES20.GL_VERSION).orEmpty()
    val tag = if (glVersion.contains("3.")) "OPENGL ES 3.0 • 3D GLSL" else "OPENGL ES 2.0 • 3D GLSL"
    state.onBackendReady?.invoke(tag)
    state.onAssetStateChanged?.invoke(Pev3dAssetLoadState.Ready(state.vehicleName, "Twilight 3D Shader"))

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
        vWorldNorm = normalize(mat3(uModel) * aNorm);
        gl_Position = uMvp * vec4(aPos, 1.0);
      }
      """.trimIndent()

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
        float spec = pow(max(dot(N, H), 0.0), 32.0);
        float fresnel = pow(1.0 - max(dot(N, V), 0.0), 2.5);
        float fog = clamp((-vWorldPos.z - 12.0) / 52.0, 0.0, 0.85);
        vec3 lit = uColor.rgb * (0.30 + 0.70 * diff) + vec3(0.45) * spec + uColor.rgb * fresnel * 0.28;
        lit = mix(lit, uColor.rgb * 1.45, clamp(uEmissive, 0.0, 1.0));
        vec3 skyFog = vec3(0.035, 0.055, 0.12);
        gl_FragColor = vec4(mix(lit, skyFog, fog), uColor.a);
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
    buildCylinderMesh(20)
  }

  override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    val w = width.coerceAtLeast(1)
    val h = height.coerceAtLeast(1)
    GLES20.glViewport(0, 0, w, h)
    Matrix.perspectiveM(projMatrix, 0, 52f, w.toFloat() / h.toFloat(), 0.25f, 120f)
  }

  override fun onDrawFrame(gl: GL10?) {
    GLES20.glClearColor(0.035f, 0.055f, 0.12f, 1f)
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
    if (programId == 0) return

    GLES20.glUseProgram(programId)
    val t = ((System.nanoTime() - startNs) / 1_000_000_000.0).toFloat()

    val laneX = state.lanePos * 2.15f
    val shake = state.shakeAmount
    val shakeX = if (shake > 0.01f) sin(t * 58f) * 0.22f * shake else 0f
    val shakeY = if (shake > 0.01f) cos(t * 47f) * 0.16f * shake else 0f
    val camX = laneX * 0.42f + shakeX
    val camY = 3.2f + state.jumpHeight * 0.35f + shakeY
    val camZ = 5.4f
    Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, laneX * 0.55f, 0.9f, -18.0f, 0f, 1f, 0f)

    GLES20.glUniform3f(uLightDirLoc, 0.40f, 0.85f, 0.32f)
    GLES20.glUniform3f(uCamPosLoc, camX, camY, camZ)

    // 1. Wide twilight ground, sunset horizon glow & asphalt road slab
    drawBox(0f, -0.35f, -28f, 80f, 0.2f, 84f, 0.05f, 0.09f, 0.07f, 1f, 0f)
    drawBox(0f, 3.2f, -66f, 96f, 6.5f, 0.6f, 0.36f, 0.14f, 0.24f, 0.85f, 0.55f)
    drawBox(0f, 0.9f, -65.5f, 84f, 2.2f, 0.5f, 0.85f, 0.32f, 0.16f, 0.72f, 0.70f)
    drawBox(0f, -0.18f, -28f, 7.6f, 0.18f, 84f, 0.09f, 0.11f, 0.15f, 1f, 0f)
    // Edge gravel shoulders
    drawBox(-4.2f, -0.14f, -28f, 0.85f, 0.14f, 84f, 0.20f, 0.18f, 0.14f, 1f, 0f)
    drawBox(4.2f, -0.14f, -28f, 0.85f, 0.14f, 84f, 0.20f, 0.18f, 0.14f, 1f, 0f)

    // 2. Scrolling dashed lane dividers, roadside pine silhouettes & solar lamps
    val scrollOff = (state.roadScroll % 1f) * 4.5f
    for (i in 0..15) {
      val zPos = 4f - i * 4.5f + scrollOff
      drawBox(-1.15f, -0.05f, zPos, 0.12f, 0.04f, 1.9f, 0.95f, 0.80f, 0.22f, 0.9f, 0.5f)
      drawBox(1.15f, -0.05f, zPos, 0.12f, 0.04f, 1.9f, 0.95f, 0.80f, 0.22f, 0.9f, 0.5f)
      if (i % 2 == 0) {
        drawBox(-4.5f, 1.4f, zPos, 0.12f, 2.9f, 0.12f, 0.30f, 0.35f, 0.42f, 1f, 0f)
        drawBox(4.5f, 1.4f, zPos, 0.12f, 2.9f, 0.12f, 0.30f, 0.35f, 0.42f, 1f, 0f)
        drawBox(-4.1f, 2.85f, zPos, 0.45f, 0.15f, 0.35f, 0.25f, 0.85f, 1.0f, 1f, 0.9f)
        drawBox(4.1f, 2.85f, zPos, 0.45f, 0.15f, 0.35f, 0.25f, 0.85f, 1.0f, 1f, 0.9f)
      } else {
        drawBox(-7.8f, 1.3f, zPos, 1.15f, 2.7f, 1.15f, 0.06f, 0.24f, 0.13f, 1f, 0.05f)
        drawBox(7.8f, 1.3f, zPos, 1.15f, 2.7f, 1.15f, 0.06f, 0.24f, 0.13f, 1f, 0.05f)
      }
    }

    // 3. Intersections
    val inters = state.intersectionZs
    for (iz in inters) {
      val worldZ = -58f + iz * 60f
      drawBox(0f, -0.07f, worldZ, 26f, 0.10f, 3.8f, 0.14f, 0.17f, 0.24f, 1f, 0.05f)
    }

    // 4. Hazards & Collectibles
    val items = state.entities
    for (item in items) {
      val ex = item.lane * 2.15f
      val ez = -58f + item.z * 58f
      when (item.kindCode) {
        0 -> { // HAY BALE
          drawBox(ex, 0.45f, ez, 1.25f, 0.85f, 0.95f, 0.86f, 0.65f, 0.18f, 1f, 0.15f)
        }
        1 -> { // CREEK WASHOUT
          drawBox(ex, -0.01f, ez, 1.85f, 0.12f, 2.2f, 0.12f, 0.62f, 0.95f, 0.88f, 0.55f)
        }
        2 -> { // TRACTOR
          drawBox(ex, 0.72f, ez, 1.35f, 1.25f, 1.85f, 0.20f, 0.72f, 0.28f, 1f, 0.2f)
          drawBox(ex, 1.55f, ez - 0.15f, 1.05f, 0.65f, 1.0f, 0.88f, 0.78f, 0.22f, 1f, 0.35f)
        }
        3 -> { // VOLT CELL (glowing cyan battery)
          val bob = sin(t * 6f + ez) * 0.15f
          drawBox(ex, 0.68f + bob, ez, 0.52f, 0.72f, 0.42f, 0.15f, 0.95f, 0.88f, 1f, 0.85f)
        }
        4 -> { // SOLAR ORB (glowing gold)
          val bob = sin(t * 7f + ez) * 0.18f
          drawCyl(ex, 0.75f + bob, ez, 0.42f, 0.42f, 0.42f, 0f, 1.0f, 0.82f, 0.16f, 1f, 0.95f)
        }
      }
    }

    // 5. Blueprint-Accurate Player PEV Rig + Headlight Ground Pool + Spinning Wheels
    val px = laneX
    val py = 0.42f + state.jumpHeight * 2.2f
    val pz = 0f
    val bankRad = state.turnBankDeg
    val finish = Pev3dEngine.finishForModel(state.vehicleName, "", 72)
    val bCol = finish.bodyRgb
    val aCol = finish.accentRgb
    val boostCol = if (state.boostActive) floatArrayOf(0.15f, 0.92f, 1.0f) else floatArrayOf(0.95f, 0.32f, 0.14f)

    // Projected forward headlight beam cone on asphalt road ahead
    drawBox(px, -0.05f, pz - 4.4f, 1.9f, 0.03f, 6.2f, 0.82f, 0.93f, 1.0f, 0.14f, 0.9f)

    when (finish.blueprintType) {
      Pev3dBlueprintType.EUC_ARMORED_ROLLCAGE,
      Pev3dBlueprintType.EUC_AERO_TRAIL, -> {
        drawPartRotated(px, py + 0.05f, pz, 0.24f, 0.44f, 0.44f, state.wheelSpinDeg, bankRad, 0.10f, 0.11f, 0.13f, 0f, isCyl = true)
        drawPartRotated(px - 0.16f, py + 0.20f, pz, 0.12f, 0.62f, 0.56f, 0f, bankRad, bCol[0], bCol[1], bCol[2], 0.15f)
        drawPartRotated(px + 0.16f, py + 0.20f, pz, 0.12f, 0.62f, 0.56f, 0f, bankRad, bCol[0], bCol[1], bCol[2], 0.15f)
        drawPartRotated(px, py + 0.56f, pz, 0.36f, 0.12f, 0.44f, 0f, bankRad, aCol[0], aCol[1], aCol[2], 0.3f)
        drawPartRotated(px, py + 0.44f, pz - 0.32f, 0.20f, 0.14f, 0.08f, 0f, bankRad, 0.96f, 0.98f, 1.0f, 1.0f)
        drawPartRotated(px, py + 0.40f, pz + 0.32f, 0.22f, 0.10f, 0.08f, 0f, bankRad, boostCol[0], boostCol[1], boostCol[2], 1.0f)
      }
      Pev3dBlueprintType.SCOOTER_DUAL_STEM_OFFROAD,
      Pev3dBlueprintType.SCOOTER_TUBULAR_RACING,
      Pev3dBlueprintType.SCOOTER_MONOCOQUE_AERO, -> {
        drawPartRotated(px, py - 0.22f, pz - 0.64f, 0.16f, 0.24f, 0.24f, state.wheelSpinDeg, bankRad, 0.10f, 0.11f, 0.13f, 0f, isCyl = true)
        drawPartRotated(px, py - 0.22f, pz + 0.64f, 0.16f, 0.24f, 0.24f, state.wheelSpinDeg, bankRad, 0.10f, 0.11f, 0.13f, 0f, isCyl = true)
        drawPartRotated(px, py - 0.10f, pz, 0.46f, 0.16f, 1.18f, 0f, bankRad, bCol[0], bCol[1], bCol[2], 0.18f)
        if (finish.blueprintType == Pev3dBlueprintType.SCOOTER_DUAL_STEM_OFFROAD) {
          drawPartRotated(px - 0.11f, py + 0.54f, pz - 0.48f, 0.08f, 1.10f, 0.08f, 0f, bankRad, aCol[0], aCol[1], aCol[2], 0.2f)
          drawPartRotated(px + 0.11f, py + 0.54f, pz - 0.48f, 0.08f, 1.10f, 0.08f, 0f, bankRad, aCol[0], aCol[1], aCol[2], 0.2f)
        } else {
          drawPartRotated(px, py + 0.54f, pz - 0.48f, 0.12f, 1.10f, 0.12f, 0f, bankRad, bCol[0], bCol[1], bCol[2], 0.2f)
        }
        drawPartRotated(px, py + 1.08f, pz - 0.48f, 0.82f, 0.07f, 0.09f, 0f, bankRad, 0.90f, 0.92f, 0.96f, 0.4f)
        drawPartRotated(px, py + 0.34f, pz - 0.56f, 0.20f, 0.12f, 0.08f, 0f, bankRad, 0.96f, 0.98f, 1.0f, 1.0f)
        drawPartRotated(px, py - 0.02f, pz + 0.62f, 0.26f, 0.09f, 0.08f, 0f, bankRad, boostCol[0], boostCol[1], boostCol[2], 1.0f)
      }
      Pev3dBlueprintType.HYDROFOIL_EFOIL -> {
        drawPartRotated(px, py + 0.06f, pz, 0.72f, 0.12f, 1.55f, 0f, bankRad, bCol[0], bCol[1], bCol[2], 0.22f)
        drawPartRotated(px, py - 0.22f, pz + 0.15f, 0.08f, 0.52f, 0.20f, 0f, bankRad, aCol[0], aCol[1], aCol[2], 0.25f)
        drawPartRotated(px, py - 0.44f, pz + 0.10f, 0.82f, 0.06f, 0.22f, 0f, bankRad, 0.20f, 0.85f, 0.95f, 0.45f)
        drawPartRotated(px, py + 0.16f, pz - 0.68f, 0.22f, 0.08f, 0.08f, 0f, bankRad, 0.96f, 0.98f, 1.0f, 1.0f)
      }
      else -> {
        drawPartRotated(px, py - 0.15f, pz - 0.68f, 0.20f, 0.34f, 0.34f, state.wheelSpinDeg, bankRad, 0.10f, 0.11f, 0.13f, 0f, isCyl = true)
        drawPartRotated(px, py - 0.15f, pz + 0.68f, 0.20f, 0.34f, 0.34f, state.wheelSpinDeg, bankRad, 0.10f, 0.11f, 0.13f, 0f, isCyl = true)
        drawPartRotated(px, py + 0.12f, pz, 0.34f, 0.28f, 1.35f, 0f, bankRad, bCol[0], bCol[1], bCol[2], 0.15f)
        drawPartRotated(px, py + 0.26f, pz - 0.08f, 0.38f, 0.26f, 0.58f, 0f, bankRad, 0.18f, 0.88f, 0.48f, 0.35f)
        drawPartRotated(px, py + 0.52f, pz - 0.46f, 0.14f, 0.56f, 0.14f, 0f, bankRad, aCol[0], aCol[1], aCol[2], 0.25f)
        drawPartRotated(px, py + 0.78f, pz - 0.46f, 0.78f, 0.08f, 0.10f, 0f, bankRad, 0.88f, 0.92f, 0.96f, 0.4f)
        drawPartRotated(px, py + 0.58f, pz - 0.62f, 0.18f, 0.14f, 0.08f, 0f, bankRad, 0.95f, 0.98f, 1.0f, 1.0f)
        drawPartRotated(px, py + 0.26f, pz + 0.72f, 0.24f, 0.10f, 0.08f, 0f, bankRad, boostCol[0], boostCol[1], boostCol[2], 1.0f)
      }
    }
    if (state.boostActive) {
      drawPartRotated(px, py + 0.22f, pz + 1.05f, 0.22f, 0.22f, 0.55f, 0f, bankRad, 0.15f, 0.95f, 1.0f, 1.0f)
    }

    if (state.turnSignalActive && state.turnSignalDir != 0) {
      val sx = if (state.turnSignalDir < 0) px - 0.44f else px + 0.44f
      drawPartRotated(sx, py + 0.74f, pz - 0.46f, 0.12f, 0.12f, 0.12f, 0f, bankRad, 1.0f, 0.68f, 0.05f, 1.0f)
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

  private fun drawCyl(
    x: Float,
    y: Float,
    z: Float,
    sx: Float,
    sy: Float,
    sz: Float,
    spinDeg: Float,
    r: Float,
    g: Float,
    b: Float,
    a: Float,
    emissive: Float,
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    if (spinDeg != 0f) Matrix.rotateM(modelMatrix, 0, spinDeg, 1f, 0f, 0f)
    Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
    submitBuffer(cylBuf, cylVertCount, r, g, b, a, emissive)
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
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    if (bankDeg != 0f) Matrix.rotateM(modelMatrix, 0, -bankDeg, 0f, 0f, 1f)
    if (spinDeg != 0f) Matrix.rotateM(modelMatrix, 0, -spinDeg, 1f, 0f, 0f)
    if (isCyl) Matrix.rotateM(modelMatrix, 0, 90f, 0f, 1f, 0f)
    Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
    submitBuffer(if (isCyl) cylBuf else cubeBuf, if (isCyl) cylVertCount else cubeVertCount, r, g, b, 1f, emissive)
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
      val x0 = cos(a0)
      val y0 = sin(a0)
      val x1 = cos(a1)
      val y1 = sin(a1)
      verts.addAll(listOf(x0, y0, 0.5f, x0, y0, 0f, x1, y1, 0.5f, x1, y1, 0f, x1, y1, -0.5f, x1, y1, 0f))
      verts.addAll(listOf(x0, y0, 0.5f, x0, y0, 0f, x1, y1, -0.5f, x1, y1, 0f, x0, y0, -0.5f, x0, y0, 0f))
      verts.addAll(listOf(0f, 0f, 0.5f, 0f, 0f, 1f, x0, y0, 0.5f, 0f, 0f, 1f, x1, y1, 0.5f, 0f, 0f, 1f))
      verts.addAll(listOf(0f, 0f, -0.5f, 0f, 0f, -1f, x1, y1, -0.5f, 0f, 0f, -1f, x0, y0, -0.5f, 0f, 0f, -1f))
    }
    cylVertCount = verts.size / 6
    cylBuf = allocBuffer(verts.toFloatArray())
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
