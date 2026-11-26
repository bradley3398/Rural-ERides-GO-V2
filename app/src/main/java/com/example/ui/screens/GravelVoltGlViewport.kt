package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
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
 * Hardware-Accelerated OpenGL ES 2.0/3.0 Textured Cook-Torrance GGX PBR + ACES Filmic
 * Rural Dirt Trail, Forest & Wildflower 3D Renderer for Gravel Volt Runner.
 *
 * Features:
 * - True Cook-Torrance GGX microfacet specular, Schlick Fresnel, Dual-Layer Clearcoat,
 *   golden-hour hemisphere sky/ground IBL, and ACES Filmic tone mapping.
 * - Procedural mipmapped GPU textures for the player's sculpted 3D PEV rig (knobby tires,
 *   3K carbon weave, CNC brushed alloy, metallic livery, glowing LED optics, TFT dash)
 *   plus a scrolling crushed-gravel & sienna clay rural road surface texture.
 * - Multi-tiered 3D pines & oaks, swaying roadside wildflowers, golden sun corona,
 *   drifting volumetric clouds, Blue Ridge mountain peaks, rolled hay bales, rocky creek fords,
 *   and dynamic gravel dust rooster-tails.
 */
private class GravelVoltGlRenderer(
  private val state: GravelVoltGlState,
) : GLSurfaceView.Renderer {

  private var programId = 0
  private var uMvpLoc = -1
  private var uModelLoc = -1
  private var uLightDirLoc = -1
  private var uCamPosLoc = -1
  private var uColorLoc = -1
  private var uEmissiveLoc = -1
  private var uMatParamsLoc = -1 // metallic, roughness, clearcoat, useTex
  private var uUvScaleScrollLoc = -1 // scaleU, scaleV, scrollU, scrollV
  private var uTexLoc = -1
  private var aPosLoc = -1
  private var aNormLoc = -1
  private var aUvLoc = -1

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

  private val vehicleTextures = HashMap<PevPbrMaterialId, Int>()
  private var dirtRoadTexId = 0
  private var whiteTexId = 0

  private class GlSculptedPart(
    val buf: FloatBuffer,
    val vertCount: Int,
    val texId: PevPbrMaterialId,
    val baseRgb: FloatArray,
    val metallic: Float,
    val roughness: Float,
    val clearcoat: Float,
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
      val out = FloatArray(idxArr.size * 8)
      var o = 0
      for (k in idxArr.indices) {
        val vi = (idxArr[k].toInt() and 0xFFFF) * 8
        if (vi + 7 >= srcVerts.size) continue
        val px = srcVerts[vi]
        val py = srcVerts[vi + 1]
        val pz = srcVerts[vi + 2]
        if (!px.isFinite() || !py.isFinite() || !pz.isFinite()) continue
        if (py < minY) minY = py
        val ax = abs(px)
        if (ax > maxExtent) maxExtent = ax
        out[o] = px
        out[o + 1] = py
        out[o + 2] = pz
        out[o + 3] = srcVerts[vi + 3]
        out[o + 4] = srcVerts[vi + 4]
        out[o + 5] = srcVerts[vi + 5]
        out[o + 6] = srcVerts[vi + 6]
        out[o + 7] = srcVerts[vi + 7]
        o += 8
      }
      if (o < 24) continue
      val em = ((part.emissiveRgb[0] + part.emissiveRgb[1] + part.emissiveRgb[2]) / 3f).coerceIn(0f, 1.4f)
      val calibratedRgb = when (part.texId) {
        PevPbrMaterialId.TIRE_RUBBER,
        PevPbrMaterialId.QUILTED_LEATHER,
        PevPbrMaterialId.BATTERY_PACK,
        PevPbrMaterialId.TFT_SCREEN,
        PevPbrMaterialId.HEADLIGHT_OPTIC,
        PevPbrMaterialId.TAILLIGHT_OPTIC,
        PevPbrMaterialId.AMBER_SIGNAL_OPTIC -> floatArrayOf(1f, 1f, 1f)
        else -> part.baseRgb.copyOf()
      }
      glParts.add(
        GlSculptedPart(
          buf = allocBuffer(if (o == out.size) out else out.copyOf(o)),
          vertCount = o / 8,
          texId = part.texId,
          baseRgb = calibratedRgb,
          metallic = part.metallic.coerceIn(0f, 1f),
          roughness = part.roughness.coerceIn(0.08f, 0.96f),
          clearcoat = part.clearcoat.coerceIn(0f, 1f),
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
    runCatching {
      val glVersion = GLES20.glGetString(GLES20.GL_VERSION).orEmpty()
      val tag = if (glVersion.contains("3.")) {
        "OPENGL ES 3.0 • GGX PBR + ACES"
      } else {
        "OPENGL ES 2.0 • GGX PBR + ACES"
      }
      state.onBackendReady?.invoke(tag)
      state.onAssetStateChanged?.invoke(
        Pev3dAssetLoadState.Ready(state.vehicleName, "Hardware GGX PBR Rural Trail & Sculpted Rig")
      )

      GLES20.glEnable(GLES20.GL_DEPTH_TEST)
      GLES20.glDepthFunc(GLES20.GL_LEQUAL)
      GLES20.glEnable(GLES20.GL_BLEND)
      GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

      val vertSrc =
        """
        uniform mat4 uMvp;
        uniform mat4 uModel;
        attribute vec3 aPos;
        attribute vec3 aNorm;
        attribute vec2 aUv;
        varying vec3 vWorldPos;
        varying vec3 vWorldNorm;
        varying vec2 vUv;
        void main() {
          vec4 wp = uModel * vec4(aPos, 1.0);
          vWorldPos = wp.xyz;
          vWorldNorm = normalize((uModel * vec4(aNorm, 0.0)).xyz);
          vUv = aUv;
          gl_Position = uMvp * vec4(aPos, 1.0);
        }
        """.trimIndent()

      // True Cook-Torrance GGX PBR + Dual-Layer Clearcoat + Procedural Textures + ACES Filmic
      val fragSrc =
        """
        precision mediump float;
        uniform vec3 uLightDir;
        uniform vec3 uCamPos;
        uniform vec4 uColor;
        uniform float uEmissive;
        uniform vec4 uMatParams;      // x=metallic, y=roughness, z=clearcoat, w=useTex
        uniform vec4 uUvScaleScroll;  // xy=scale, zw=scroll
        uniform sampler2D uTex;
        varying vec3 vWorldPos;
        varying vec3 vWorldNorm;
        varying vec2 vUv;

        const float PI_VAL = 3.14159265;

        vec3 acesToneMap(vec3 x) {
          return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
        }

        float ggxD(float NdotH, float r) {
          float a = r * r;
          float a2 = a * a;
          float d = NdotH * NdotH * (a2 - 1.0) + 1.0;
          return a2 / max(PI_VAL * d * d, 0.0008);
        }

        void main() {
          vec3 N = normalize(vWorldNorm);
          vec3 L = normalize(uLightDir);
          vec3 V = normalize(uCamPos - vWorldPos);
          vec3 H = normalize(L + V);

          vec2 uv = vUv * uUvScaleScroll.xy + uUvScaleScroll.zw;
          vec3 texSample = mix(vec3(1.0), texture2D(uTex, uv).rgb, clamp(uMatParams.w, 0.0, 1.0));

          // Organic procedural micro-grain for untextured dirt berms, bark & meadow foliage
          float grainA = sin(vWorldPos.x * 13.7 + cos(vWorldPos.z * 9.3)) * cos(vWorldPos.z * 14.1);
          float grainB = sin(vWorldPos.x * 37.0 - vWorldPos.z * 29.0);
          float terrainMask = 1.0 - clamp(abs(vWorldPos.y + 0.12) * 2.5, 0.0, 1.0);
          float microVar = (grainA * 0.048 + grainB * 0.028) * (1.0 - uMatParams.w * 0.65) * (0.40 + 0.60 * terrainMask);

          vec3 albedo = clamp(uColor.rgb * texSample + vec3(microVar), 0.0, 1.0);
          float metallic = clamp(uMatParams.x, 0.0, 1.0);
          float rough = clamp(uMatParams.y, 0.08, 0.96);
          float clearcoat = clamp(uMatParams.z, 0.0, 1.0);

          float NdotL = max(dot(N, L), 0.0);
          float NdotV = max(dot(N, V), 0.001);
          float NdotH = max(dot(N, H), 0.0);
          float VdotH = max(dot(V, H), 0.0);

          // Cook-Torrance GGX Specular + Schlick Fresnel
          vec3 F0 = mix(vec3(0.04), albedo, metallic);
          vec3 F = F0 + (1.0 - F0) * pow(1.0 - VdotH, 5.0);
          float D = ggxD(NdotH, rough);
          float k = (rough + 1.0) * (rough + 1.0) * 0.125;
          float G = (NdotV / (NdotV * (1.0 - k) + k)) * (NdotL / (NdotL * (1.0 - k) + k));
          vec3 spec = (D * G * F) / max(4.0 * NdotV * NdotL, 0.002);

          // Dual-layer automotive clearcoat glint for painted PEV body panels & water
          float Dcoat = ggxD(NdotH, 0.11);
          float Fcoat = 0.04 + 0.96 * pow(1.0 - VdotH, 5.0);
          vec3 coatSpec = vec3(0.28 * clearcoat * Dcoat * Fcoat);

          // Golden-hour sunlight + sky/earth hemisphere IBL reflection
          vec3 sunRad = vec3(1.52, 1.32, 1.04);
          vec3 kD = (1.0 - F) * (1.0 - metallic);
          vec3 direct = (kD * albedo / PI_VAL + spec) * sunRad * NdotL + coatSpec * sunRad * NdotL;

          float hemi = 0.5 + 0.5 * N.y;
          vec3 groundAmb = vec3(0.26, 0.21, 0.16);
          vec3 skyAmb = vec3(0.46, 0.62, 0.84);
          vec3 ambientDiff = albedo * mix(groundAmb, skyAmb, hemi) * (1.0 - metallic * 0.55) * 0.72;

          // Procedural Golden-Hour Environment Reflection (sky dome + warm sun horizon band)
          vec3 R = reflect(-V, N);
          float horizonBand = exp(-abs(R.y) * 5.0);
          vec3 envColor = mix(vec3(0.20, 0.17, 0.14), vec3(0.45, 0.68, 0.96), clamp(R.y * 0.5 + 0.5, 0.0, 1.0));
          envColor += vec3(0.95, 0.68, 0.36) * horizonBand * 0.55;
          float specFade = pow(1.0 - rough, 1.8);
          vec3 envSpec = envColor * (F0 + (1.0 - F0) * pow(1.0 - NdotV, 4.0)) * specFade * (0.35 + 0.65 * metallic + 0.45 * clearcoat);

          vec3 hdr = direct + ambientDiff + envSpec;
          vec3 lit = acesToneMap(hdr * 1.15);

          // Emissive optics, sun disc, voltage cells & solar orbs
          lit = mix(lit, clamp(albedo * 1.42, 0.0, 1.0), clamp(uEmissive, 0.0, 1.0));

          // Atmospheric golden-hour distance scattering
          float distFog = clamp((-vWorldPos.z - 18.0) / 58.0, 0.0, 0.80);
          float horizonBlend = clamp((vWorldPos.y + 1.0) / 14.0, 0.0, 1.0);
          vec3 horizonFog = mix(vec3(0.84, 0.63, 0.43), vec3(0.30, 0.53, 0.78), horizonBlend);
          gl_FragColor = vec4(mix(lit, horizonFog, distFog * (1.0 - clamp(uEmissive, 0.0, 1.0) * 0.72)), uColor.a);
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
      uMatParamsLoc = GLES20.glGetUniformLocation(programId, "uMatParams")
      uUvScaleScrollLoc = GLES20.glGetUniformLocation(programId, "uUvScaleScroll")
      uTexLoc = GLES20.glGetUniformLocation(programId, "uTex")
      aPosLoc = GLES20.glGetAttribLocation(programId, "aPos")
      aNormLoc = GLES20.glGetAttribLocation(programId, "aNorm")
      aUvLoc = GLES20.glGetAttribLocation(programId, "aUv")

      uploadProceduralTextures()

      buildCubeMesh()
      buildCylinderMesh(24)
      buildUprightCylinderMesh(20)
      buildConeMesh(22)
      buildSphereMesh(12, 18)
    }
  }

  private fun uploadProceduralTextures() {
    whiteTexId = uploadBitmapTex(
      Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply { eraseColor(AndroidColor.WHITE) },
      repeat = true,
    )
    dirtRoadTexId = uploadBitmapTex(buildDirtGravelRoadBitmap(256), repeat = true)

    vehicleTextures.clear()
    vehicleTextures[PevPbrMaterialId.CARBON_WEAVE] = uploadBitmapTex(buildCarbonBitmap(128), repeat = true)
    vehicleTextures[PevPbrMaterialId.BRUSHED_METAL] = uploadBitmapTex(buildBrushedMetalBitmap(128), repeat = true)
    vehicleTextures[PevPbrMaterialId.TIRE_RUBBER] = uploadBitmapTex(buildTireTreadBitmap(128), repeat = true)
    vehicleTextures[PevPbrMaterialId.BATTERY_PACK] = uploadBitmapTex(buildBatteryPackBitmap(128), repeat = true)
    vehicleTextures[PevPbrMaterialId.BRAKE_ROTOR] = uploadBitmapTex(buildBrakeRotorBitmap(128), repeat = true)
    vehicleTextures[PevPbrMaterialId.QUILTED_LEATHER] = uploadBitmapTex(buildQuiltedSeatBitmap(128), repeat = true)
    vehicleTextures[PevPbrMaterialId.TFT_SCREEN] = uploadBitmapTex(buildTftDashBitmap(128), repeat = false)
    vehicleTextures[PevPbrMaterialId.RACING_LIVERY] = uploadBitmapTex(buildMetallicFlakeBitmap(128), repeat = true)
    vehicleTextures[PevPbrMaterialId.HEADLIGHT_OPTIC] = uploadBitmapTex(buildHeadlightOpticBitmap(128), repeat = false)
    vehicleTextures[PevPbrMaterialId.TAILLIGHT_OPTIC] = uploadBitmapTex(buildTaillightOpticBitmap(128), repeat = false)
    vehicleTextures[PevPbrMaterialId.AMBER_SIGNAL_OPTIC] = uploadBitmapTex(buildAmberOpticBitmap(128), repeat = false)
    vehicleTextures[PevPbrMaterialId.DYNO_FLOOR] = dirtRoadTexId
  }

  private fun uploadBitmapTex(bmp: Bitmap, repeat: Boolean): Int {
    val ids = IntArray(1)
    GLES20.glGenTextures(1, ids, 0)
    val id = ids[0]
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)
    val wrap = if (repeat) GLES20.GL_REPEAT else GLES20.GL_CLAMP_TO_EDGE
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, wrap)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, wrap)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
    GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
    bmp.recycle()
    return id
  }

  private fun buildDirtGravelRoadBitmap(size: Int): Bitmap {
    val px = IntArray(size * size)
    for (y in 0 until size) {
      val yf = y.toFloat() / size
      for (x in 0 until size) {
        val xf = x.toFloat() / size
        // 3 subtle worn tire ruts across the 3-lane country trail
        val rutL = kotlin.math.exp(-((xf - 0.21f) * (xf - 0.21f)) / 0.0045f)
        val rutC = kotlin.math.exp(-((xf - 0.50f) * (xf - 0.50f)) / 0.0045f)
        val rutR = kotlin.math.exp(-((xf - 0.79f) * (xf - 0.79f)) / 0.0045f)
        val rut = (rutL + rutC + rutR).coerceAtMost(1f)

        val h1 = (((x * 374761393 + y * 668265263) xor (x * 19 + y * 31)) and 0xFF) / 255f
        val h2 = ((((x / 2) * 1274126177 + (y / 2) * 374761393) xor (x * 7)) and 0xFF) / 255f
        val pebble = if (h1 > 0.89f) 0.16f else if (h1 < 0.09f) -0.14f else (h2 - 0.5f) * 0.10f
        val wave = sin((yf * 6f + xf * 2f) * PI).toFloat() * 0.03f

        val baseR = 0.54f - rut * 0.16f + pebble + wave
        val baseG = 0.40f - rut * 0.13f + pebble * 0.88f + wave * 0.7f
        val baseB = 0.26f - rut * 0.09f + pebble * 0.72f
        val r = (baseR * 255f).toInt().coerceIn(38, 225)
        val g = (baseG * 255f).toInt().coerceIn(28, 195)
        val b = (baseB * 255f).toInt().coerceIn(18, 155)
        px[y * size + x] = AndroidColor.rgb(r, g, b)
      }
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
      setPixels(px, 0, size, 0, 0, size, size)
    }
  }

  private fun buildCarbonBitmap(size: Int): Bitmap {
    val px = IntArray(size * size)
    val cell = (size / 16).coerceAtLeast(4)
    for (y in 0 until size) {
      for (x in 0 until size) {
        val phase = ((x / cell) + (y / cell)) % 4
        val warp = phase < 2
        val lx = (x % cell).toFloat() / cell
        val ly = (y % cell).toFloat() / cell
        val dome = sin((if (warp) lx else ly) * PI).toFloat()
        val lum = ((if (warp) 34f else 22f) + dome * 34f).toInt().coerceIn(14, 96)
        px[y * size + x] = AndroidColor.rgb(lum, (lum + 2).coerceAtMost(100), (lum + 6).coerceAtMost(108))
      }
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply { setPixels(px, 0, size, 0, 0, size, size) }
  }

  private fun buildBrushedMetalBitmap(size: Int): Bitmap {
    val px = IntArray(size * size)
    for (y in 0 until size) {
      val streak = (((y * 2654435761L) ushr 16) and 0x1FL).toInt() - 16
      for (x in 0 until size) {
        val grain = (((x * 1103515245 + y * 12345) ushr 18) and 0x0F) - 8
        val v = (212 + streak + grain).coerceIn(162, 246)
        px[y * size + x] = AndroidColor.rgb(v, v, (v + 3).coerceAtMost(250))
      }
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply { setPixels(px, 0, size, 0, 0, size, size) }
  }

  private fun buildTireTreadBitmap(size: Int): Bitmap {
    val px = IntArray(size * size)
    for (y in 0 until size) {
      val v = y.toFloat() / size
      for (x in 0 until size) {
        val u = x.toFloat() / size
        val chev = (v * 14f + abs(u - 0.5f) * 4.5f).let { it - floor(it) }
        val knob = chev in 0.16f..0.78f && abs(u - 0.5f) > 0.055f
        val lum = if (knob) 46 else 22
        px[y * size + x] = AndroidColor.rgb(lum, lum, (lum + 3).coerceAtMost(60))
      }
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply { setPixels(px, 0, size, 0, 0, size, size) }
  }

  private fun buildBatteryPackBitmap(size: Int): Bitmap {
    val px = IntArray(size * size)
    for (y in 0 until size) {
      val rib = (y % 14) < 5
      val stripe = y in (size * 44 / 100)..(size * 50 / 100)
      for (x in 0 until size) {
        px[y * size + x] = when {
          stripe -> AndroidColor.rgb(22, 212, 162)
          rib -> AndroidColor.rgb(52, 62, 74)
          else -> AndroidColor.rgb(26, 32, 40)
        }
      }
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply { setPixels(px, 0, size, 0, 0, size, size) }
  }

  private fun buildBrakeRotorBitmap(size: Int): Bitmap {
    val px = IntArray(size * size)
    val c = size * 0.5f
    for (y in 0 until size) {
      for (x in 0 until size) {
        val dx = (x - c) / c
        val dy = (y - c) / c
        val rad = kotlin.math.sqrt(dx * dx + dy * dy)
        val ang = kotlin.math.atan2(dy.toDouble(), dx.toDouble()).toFloat()
        val vent = rad in 0.45f..0.88f && sin(ang * 12f + rad * 6f) > 0.80f
        val v = when {
          vent -> 36
          rad < 0.38f -> 82
          else -> 208
        }
        px[y * size + x] = AndroidColor.rgb(v, v, (v + 4).coerceAtMost(230))
      }
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply { setPixels(px, 0, size, 0, 0, size, size) }
  }

  private fun buildQuiltedSeatBitmap(size: Int): Bitmap {
    val px = IntArray(size * size)
    val ribP = size / 10f
    for (y in 0 until size) {
      val f = (y / ribP).let { it - floor(it) }
      val bump = sin(f * PI).toFloat()
      val lum = (36 + (bump * 24f).toInt()).coerceIn(22, 70)
      for (x in 0 until size) {
        px[y * size + x] = AndroidColor.rgb(lum, lum, lum + 2)
      }
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply { setPixels(px, 0, size, 0, 0, size, size) }
  }

  private fun buildTftDashBitmap(size: Int): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    c.drawColor(AndroidColor.rgb(8, 16, 28))
    val p = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(34, 211, 238) }
    c.drawRect(size * 0.10f, size * 0.14f, size * 0.90f, size * 0.24f, p)
    p.color = AndroidColor.rgb(16, 185, 129)
    c.drawRect(size * 0.12f, size * 0.68f, size * 0.84f, size * 0.82f, p)
    return bmp
  }

  private fun buildMetallicFlakeBitmap(size: Int): Bitmap {
    val px = IntArray(size * size)
    for (y in 0 until size) {
      val yf = y.toFloat() / size
      for (x in 0 until size) {
        val xf = x.toFloat() / size
        val flake = (((x * 374761393 + y * 668265263) xor (x * 13)) and 0xFF) / 255f * 16f - 8f
        val diag = xf * 0.6f + yf * 0.8f
        val v = if (abs(diag - 0.86f) < 0.015f) 248 else (232f - yf * 18f + flake).toInt().coerceIn(194, 250)
        px[y * size + x] = AndroidColor.rgb(v, v, v)
      }
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply { setPixels(px, 0, size, 0, 0, size, size) }
  }

  private fun buildHeadlightOpticBitmap(size: Int): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    val s = size.toFloat()
    c.drawColor(AndroidColor.rgb(18, 24, 32))
    val p = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(245, 252, 255) }
    c.drawCircle(s * 0.5f, s * 0.5f, s * 0.34f, p)
    return bmp
  }

  private fun buildTaillightOpticBitmap(size: Int): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    val s = size.toFloat()
    c.drawColor(AndroidColor.rgb(45, 6, 10))
    val p = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(255, 42, 42) }
    c.drawRoundRect(s * 0.12f, s * 0.24f, s * 0.88f, s * 0.76f, 8f, 8f, p)
    return bmp
  }

  private fun buildAmberOpticBitmap(size: Int): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    val s = size.toFloat()
    c.drawColor(AndroidColor.rgb(48, 22, 4))
    val p = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(255, 174, 22) }
    c.drawRoundRect(s * 0.14f, s * 0.24f, s * 0.86f, s * 0.76f, 8f, 8f, p)
    return bmp
  }

  override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    val w = width.coerceAtLeast(1)
    val h = height.coerceAtLeast(1)
    GLES20.glViewport(0, 0, w, h)
    Matrix.perspectiveM(projMatrix, 0, 52f, w.toFloat() / h.toFloat(), 0.25f, 130f)
  }

  override fun onDrawFrame(gl: GL10?) {
    runCatching { drawFrameInternal() }
  }

  private fun drawFrameInternal() {
    // Rich golden-hour rural sky dome backdrop color
    GLES20.glClearColor(0.28f, 0.48f, 0.74f, 1f)
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
    if (programId == 0) return

    GLES20.glUseProgram(programId)
    GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
    GLES20.glUniform1i(uTexLoc, 0)
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
      drawSphere(mx + 4.5f, -0.4f, -59f, 14f, 4.6f, 8f, 0.18f, 0.33f, 0.22f, 1f, 0.05f)
    }

    // =========================================================================
    // 2. TEXTURED CAMBERED RURAL DIRT & GRAVEL ROAD, TWIN TIRE RUTS & MEADOW SHOULDERS
    // =========================================================================
    drawBox(0f, -0.34f, -28f, 95f, 0.22f, 88f, 0.17f, 0.35f, 0.15f, 1f, 0f)
    drawBox(-6.6f, -0.22f, -28f, 4.8f, 0.18f, 88f, 0.22f, 0.43f, 0.18f, 1f, 0f)
    drawBox(6.6f, -0.22f, -28f, 4.8f, 0.18f, 88f, 0.22f, 0.43f, 0.18f, 1f, 0f)
    drawBox(-4.15f, -0.17f, -28f, 1.15f, 0.16f, 88f, 0.56f, 0.43f, 0.28f, 1f, 0f)
    drawBox(4.15f, -0.17f, -28f, 1.15f, 0.16f, 88f, 0.56f, 0.43f, 0.28f, 1f, 0f)

    // Main compacted sienna dirt & crushed-gravel country roadbed with scrolling procedural gravel texture
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, 0f, -0.16f, -28f)
    Matrix.scaleM(modelMatrix, 0, 7.6f, 0.16f, 88f)
    submitBuffer(
      buf = cubeBuf,
      vertCount = cubeVertCount,
      r = 0.96f,
      g = 0.92f,
      b = 0.86f,
      a = 1f,
      emissive = 0f,
      metallic = 0.02f,
      roughness = 0.88f,
      clearcoat = 0.0f,
      texId = dirtRoadTexId,
      useTex = 1f,
      uvScaleU = 1.0f,
      uvScaleV = 14.0f,
      uvScrollV = -(state.roadScroll % 1f) * 2.0f,
    )

    // Raised center gravel & wild grass crowns between tire tracks
    drawBox(-1.10f, -0.068f, -28f, 0.40f, 0.025f, 88f, 0.50f, 0.37f, 0.24f, 1f, 0f)
    drawBox(1.10f, -0.068f, -28f, 0.40f, 0.025f, 88f, 0.50f, 0.37f, 0.24f, 1f, 0f)

    // =========================================================================
    // 3. MULTI-TIERED 3D PINE/OAK FOREST TREES & PROCEDURAL ROADSIDE WILDFLOWERS
    // =========================================================================
    val scrollOff = (state.roadScroll % 1f) * 4.5f
    for (i in 0..15) {
      val zPos = 4f - i * 4.5f + scrollOff
      val seed = i * 37

      val pebbleOffset = ((i % 3) - 1) * 1.1f
      drawSphere(pebbleOffset, -0.06f, zPos, 0.55f, 0.03f, 1.35f, 0.58f, 0.45f, 0.31f, 1f, 0f)
      drawSphere(-3.75f, -0.06f, zPos + 1.1f, 0.45f, 0.04f, 0.95f, 0.62f, 0.48f, 0.33f, 1f, 0f)
      drawSphere(3.75f, -0.06f, zPos - 0.8f, 0.45f, 0.04f, 0.95f, 0.62f, 0.48f, 0.33f, 1f, 0f)

      val hillScale = 1.0f + (i % 3) * 0.32f
      drawSphere(-13.5f - (i % 2) * 3f, -0.35f, zPos, 7.5f * hillScale, 1.8f * hillScale, 5.2f, 0.20f, 0.40f, 0.17f, 1f, 0f)
      drawSphere(13.5f + (i % 2) * 3f, -0.35f, zPos, 7.5f * hillScale, 1.8f * hillScale, 5.2f, 0.20f, 0.40f, 0.17f, 1f, 0f)

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

      val deepScale = 1.12f + (i % 3) * 0.18f
      drawRealisticPineTree(-12.2f - (i % 2) * 2.2f, zPos - 2.1f, deepScale)
      drawRealisticPineTree(12.2f + (i % 2) * 2.2f, zPos + 1.8f, deepScale)

      if (i % 2 == 0) {
        drawUprightCyl(-5.15f, 0.35f, zPos, 0.16f, 0.95f, 0.16f, 0.42f, 0.29f, 0.18f, 1f, 0f)
        drawUprightCyl(5.15f, 0.35f, zPos, 0.16f, 0.95f, 0.16f, 0.42f, 0.29f, 0.18f, 1f, 0f)
        drawBox(-5.15f, 0.56f, zPos - 2.25f, 0.08f, 0.09f, 4.5f, 0.46f, 0.32f, 0.20f, 1f, 0f)
        drawBox(5.15f, 0.56f, zPos - 2.25f, 0.08f, 0.09f, 4.5f, 0.46f, 0.32f, 0.20f, 1f, 0f)
      }

      val windSway = sin(t * 2.7f + zPos * 0.45f) * 0.07f
      drawProceduralFloralCluster(-4.55f - (i % 2) * 0.45f, zPos + 0.35f, seed % 4, windSway)
      drawProceduralFloralCluster(4.55f + ((i + 1) % 2) * 0.45f, zPos - 0.35f, (seed + 2) % 4, -windSway)
      drawProceduralFloralCluster(-6.05f - (i % 3) * 0.35f, zPos - 1.4f, (seed + 1) % 4, windSway * 1.2f)
      drawProceduralFloralCluster(6.05f + (i % 3) * 0.35f, zPos + 1.4f, (seed + 3) % 4, -windSway * 1.2f)
    }

    // =========================================================================
    // 4. RURAL CROSS-TRAIL INTERSECTIONS
    // =========================================================================
    val inters = state.intersectionZs
    for (iz in inters) {
      val worldZ = -58f + iz * 60f
      drawBox(0f, -0.08f, worldZ, 30f, 0.11f, 4.0f, 0.48f, 0.35f, 0.22f, 1f, 0.02f)
      drawBox(0f, -0.065f, worldZ, 30f, 0.02f, 1.6f, 0.36f, 0.25f, 0.15f, 1f, 0f)
      drawUprightCyl(-4.8f, 0.95f, worldZ - 1.8f, 0.18f, 2.1f, 0.18f, 0.40f, 0.27f, 0.16f, 1f, 0f)
      drawBox(-4.4f, 1.72f, worldZ - 1.8f, 1.15f, 0.28f, 0.08f, 0.16f, 0.48f, 0.26f, 1f, 0.25f)
      drawUprightCyl(4.8f, 0.95f, worldZ - 1.8f, 0.18f, 2.1f, 0.18f, 0.40f, 0.27f, 0.16f, 1f, 0f)
      drawBox(4.4f, 1.72f, worldZ - 1.8f, 1.15f, 0.28f, 0.08f, 0.16f, 0.48f, 0.26f, 1f, 0.25f)
    }

    // =========================================================================
    // 5. ORGANIC 3D TRAIL HAZARDS & COLLECTIBLES
    // =========================================================================
    val items = state.entities
    for (item in items) {
      val ex = item.lane * 2.15f
      val ez = -58f + item.z * 58f
      when (item.kindCode) {
        0 -> { // ROLLED ROUND HAY BALE WITH TWINE BINDINGS
          drawPartRotated(ex, 0.52f, ez, 1.18f, 1.04f, 1.04f, 0f, 0f, 0.85f, 0.66f, 0.22f, 0.04f, isCyl = true, roughness = 0.92f)
          drawPartRotated(ex, 0.52f, ez, 1.22f, 0.56f, 0.56f, 0f, 0f, 0.72f, 0.53f, 0.16f, 0.02f, isCyl = true, roughness = 0.94f)
          drawPartRotated(ex - 0.34f, 0.52f, ez, 0.06f, 1.07f, 1.07f, 0f, 0f, 0.52f, 0.36f, 0.14f, 0.02f, isCyl = true, roughness = 0.90f)
          drawPartRotated(ex + 0.34f, 0.52f, ez, 0.06f, 1.07f, 1.07f, 0f, 0f, 0.52f, 0.36f, 0.14f, 0.02f, isCyl = true, roughness = 0.90f)
        }
        1 -> { // SPARKLING ROCKY CREEK CROSSING WITH REFLECTIVE WATER CLEARCOAT
          drawSphere(ex, -0.02f, ez, 2.25f, 0.10f, 2.35f, 0.14f, 0.62f, 0.92f, 0.88f, 0.28f, metallic = 0.15f, roughness = 0.12f, clearcoat = 0.95f)
          val ripple = sin(t * 5f + ez) * 0.12f
          drawSphere(ex + ripple, 0.02f, ez - 0.25f, 1.45f, 0.05f, 0.45f, 0.85f, 0.97f, 1.0f, 0.78f, 0.65f, roughness = 0.18f)
          drawSphere(ex - 0.88f, 0.12f, ez + 0.35f, 0.46f, 0.28f, 0.42f, 0.45f, 0.48f, 0.50f, 1f, 0f, roughness = 0.78f)
          drawSphere(ex + 0.86f, 0.14f, ez - 0.30f, 0.52f, 0.32f, 0.44f, 0.42f, 0.46f, 0.48f, 1f, 0f, roughness = 0.78f)
        }
        2 -> { // GLOSSY CLEARCOAT RURAL FARM TRACTOR WITH LUGGED WHEELS
          drawSphere(ex, 0.74f, ez + 0.28f, 0.96f, 0.72f, 1.25f, 0.14f, 0.62f, 0.20f, 1f, 0.06f, metallic = 0.35f, roughness = 0.24f, clearcoat = 0.85f)
          drawBox(ex, 1.38f, ez - 0.28f, 1.02f, 0.78f, 0.92f, 0.14f, 0.56f, 0.18f, 1f, 0.05f, metallic = 0.30f, roughness = 0.26f, clearcoat = 0.80f)
          drawBox(ex, 1.82f, ez - 0.28f, 1.14f, 0.12f, 1.04f, 0.94f, 0.82f, 0.18f, 1f, 0.14f, metallic = 0.20f, roughness = 0.28f, clearcoat = 0.65f)
          drawUprightCyl(ex + 0.36f, 1.38f, ez + 0.45f, 0.11f, 0.85f, 0.11f, 0.24f, 0.26f, 0.30f, 1f, 0f, metallic = 0.85f, roughness = 0.30f)
          drawPartRotated(ex - 0.66f, 0.56f, ez - 0.32f, 0.34f, 1.12f, 1.12f, 0f, 0f, 0.12f, 0.13f, 0.15f, 0f, isCyl = true, roughness = 0.85f)
          drawPartRotated(ex + 0.66f, 0.56f, ez - 0.32f, 0.34f, 1.12f, 1.12f, 0f, 0f, 0.12f, 0.13f, 0.15f, 0f, isCyl = true, roughness = 0.85f)
          drawPartRotated(ex - 0.68f, 0.56f, ez - 0.32f, 0.36f, 0.58f, 0.58f, 0f, 0f, 0.95f, 0.80f, 0.15f, 0.10f, isCyl = true, metallic = 0.45f, roughness = 0.26f)
          drawPartRotated(ex + 0.68f, 0.56f, ez - 0.32f, 0.36f, 0.58f, 0.58f, 0f, 0f, 0.95f, 0.80f, 0.15f, 0.10f, isCyl = true, metallic = 0.45f, roughness = 0.26f)
          drawPartRotated(ex - 0.56f, 0.34f, ez + 0.68f, 0.24f, 0.68f, 0.68f, 0f, 0f, 0.12f, 0.13f, 0.15f, 0f, isCyl = true, roughness = 0.85f)
          drawPartRotated(ex + 0.56f, 0.34f, ez + 0.68f, 0.24f, 0.68f, 0.68f, 0f, 0f, 0.12f, 0.13f, 0.15f, 0f, isCyl = true, roughness = 0.85f)
        }
        3 -> { // 21700 LITHIUM VOLT CELL (METALLIC + GLOWING CYAN WRAP)
          val bob = sin(t * 6f + ez) * 0.15f
          drawUprightCyl(ex, 0.68f + bob, ez, 0.46f, 0.76f, 0.46f, 0.10f, 0.94f, 0.88f, 1f, 0.75f, metallic = 0.55f, roughness = 0.20f, clearcoat = 0.85f)
          drawUprightCyl(ex, 1.09f + bob, ez, 0.26f, 0.10f, 0.26f, 0.92f, 0.96f, 1.0f, 1f, 0.85f, metallic = 0.90f, roughness = 0.18f)
          drawSphere(ex, 0.68f + bob, ez, 0.68f, 0.22f, 0.68f, 0.22f, 1.0f, 0.55f, 0.55f, 1.0f)
        }
        4 -> { // SOLAR ORB (GLOWING GOLDEN SPHERE + CORONA)
          val bob = sin(t * 7f + ez) * 0.18f
          drawSphere(ex, 0.78f + bob, ez, 0.58f, 0.58f, 0.58f, 1.0f, 0.86f, 0.18f, 1f, 0.98f, metallic = 0.70f, roughness = 0.14f)
          drawSphere(ex, 0.78f + bob, ez, 0.84f, 0.84f, 0.84f, 1.0f, 0.64f, 0.12f, 0.38f, 0.95f)
        }
      }
    }

    // =========================================================================
    // 6. HIGH-POLY TEXTURED PBR 3D PLAYER PEV RIG (SHARED WITH 3D LABS ENGINE)
    // =========================================================================
    val px = laneX
    val groundY = -0.06f
    val py = groundY + state.jumpHeight * 2.2f
    val pz = 0f
    val bankRad = state.turnBankDeg
    ensureVehicleMeshLoaded(state.vehicleName)

    val shadowScale = (1f - state.jumpHeight * 0.45f).coerceIn(0.45f, 1f)
    drawSphere(px, -0.055f, pz, 0.95f * shadowScale, 0.02f, 2.05f * shadowScale, 0.10f, 0.08f, 0.05f, 0.64f, 0f, roughness = 0.95f)
    drawSphere(px, -0.05f, pz - 4.4f, 2.3f, 0.03f, 6.4f, 0.98f, 0.95f, 0.82f, 0.20f, 0.92f)

    // Render every sculpted high-poly mesh part with real Cook-Torrance GGX PBR + UV Material Textures
    val vehScale = cachedVehicleScale
    val liftY = py - cachedVehicleMinY * vehScale
    for (part in cachedVehicleParts) {
      Matrix.setIdentityM(modelMatrix, 0)
      Matrix.translateM(modelMatrix, 0, px, liftY, pz)
      if (bankRad != 0f) {
        Matrix.rotateM(modelMatrix, 0, -bankRad * 1.15f, 0f, 0f, 1f)
      }
      Matrix.rotateM(modelMatrix, 0, 90f, 0f, 1f, 0f)
      Matrix.scaleM(modelMatrix, 0, vehScale, vehScale, vehScale)
      if (part.isWheel && part.axleCenter != null) {
        val ac = part.axleCenter
        Matrix.translateM(modelMatrix, 0, ac.x, ac.y, ac.z)
        Matrix.rotateM(modelMatrix, 0, -state.wheelSpinDeg, 0f, 0f, 1f)
        Matrix.translateM(modelMatrix, 0, -ac.x, -ac.y, -ac.z)
      }
      val tex = vehicleTextures[part.texId] ?: whiteTexId
      submitBuffer(
        buf = part.buf,
        vertCount = part.vertCount,
        r = part.baseRgb[0],
        g = part.baseRgb[1],
        b = part.baseRgb[2],
        a = 1f,
        emissive = part.emissiveBoost,
        metallic = part.metallic,
        roughness = part.roughness,
        clearcoat = part.clearcoat,
        texId = tex,
        useTex = 1f,
      )
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

  private fun drawRealisticPineTree(x: Float, z: Float, scale: Float) {
    drawUprightCyl(x, 0.55f * scale, z, 0.34f * scale, 1.35f * scale, 0.34f * scale, 0.36f, 0.23f, 0.13f, 1f, 0f, roughness = 0.92f)
    drawCone(x, 1.25f * scale, z, 2.25f * scale, 1.45f * scale, 2.25f * scale, 0.10f, 0.29f, 0.14f, 1f, 0.02f)
    drawCone(x, 2.05f * scale, z, 1.85f * scale, 1.35f * scale, 1.85f * scale, 0.13f, 0.35f, 0.17f, 1f, 0.03f)
    drawCone(x, 2.78f * scale, z, 1.40f * scale, 1.20f * scale, 1.40f * scale, 0.16f, 0.41f, 0.19f, 1f, 0.04f)
    drawCone(x, 3.42f * scale, z, 0.92f * scale, 0.98f * scale, 0.92f * scale, 0.20f, 0.47f, 0.22f, 1f, 0.06f)
  }

  private fun drawRealisticOakTree(x: Float, z: Float, scale: Float) {
    drawUprightCyl(x, 0.68f * scale, z, 0.42f * scale, 1.55f * scale, 0.42f * scale, 0.38f, 0.25f, 0.15f, 1f, 0f, roughness = 0.92f)
    drawSphere(x, 2.05f * scale, z, 2.25f * scale, 1.75f * scale, 2.25f * scale, 0.15f, 0.38f, 0.16f, 1f, 0.03f)
    drawSphere(x - 0.68f * scale, 1.78f * scale, z + 0.35f * scale, 1.55f * scale, 1.35f * scale, 1.55f * scale, 0.12f, 0.33f, 0.14f, 1f, 0.02f)
    drawSphere(x + 0.72f * scale, 1.85f * scale, z - 0.30f * scale, 1.60f * scale, 1.40f * scale, 1.60f * scale, 0.22f, 0.46f, 0.18f, 1f, 0.05f)
    drawSphere(x, 2.72f * scale, z, 1.55f * scale, 1.25f * scale, 1.55f * scale, 0.26f, 0.52f, 0.20f, 1f, 0.06f)
  }

  private fun drawProceduralFloralCluster(baseX: Float, baseZ: Float, variant: Int, sway: Float) {
    drawSphere(baseX, -0.02f, baseZ, 0.72f, 0.26f, 0.72f, 0.18f, 0.46f, 0.18f, 1f, 0.02f)
    drawCone(baseX - 0.22f + sway * 0.5f, 0.18f, baseZ + 0.15f, 0.16f, 0.45f, 0.16f, 0.22f, 0.54f, 0.20f, 1f, 0.04f)
    drawCone(baseX + 0.22f + sway * 0.5f, 0.16f, baseZ - 0.15f, 0.16f, 0.42f, 0.16f, 0.20f, 0.50f, 0.18f, 1f, 0.04f)

    for (f in -1..1) {
      val fx = baseX + f * 0.26f + sway
      val fz = baseZ + (f * 0.19f)
      val stemH = 0.42f + (f + 1) * 0.08f
      drawUprightCyl(fx, stemH * 0.5f - 0.04f, fz, 0.045f, stemH, 0.045f, 0.22f, 0.55f, 0.20f, 1f, 0.02f)

      when (variant) {
        0 -> {
          drawSphere(fx, stemH, fz, 0.28f, 0.09f, 0.28f, 1.0f, 0.80f, 0.10f, 1f, 0.35f)
          drawSphere(fx, stemH + 0.04f, fz, 0.11f, 0.08f, 0.11f, 0.26f, 0.14f, 0.06f, 1f, 0.05f)
        }
        1 -> {
          drawSphere(fx, stemH - 0.04f, fz, 0.17f, 0.14f, 0.17f, 0.56f, 0.26f, 0.92f, 1f, 0.32f)
          drawSphere(fx + sway * 0.3f, stemH + 0.08f, fz, 0.13f, 0.13f, 0.13f, 0.68f, 0.40f, 0.98f, 1f, 0.38f)
          drawSphere(fx + sway * 0.5f, stemH + 0.18f, fz, 0.09f, 0.11f, 0.09f, 0.82f, 0.58f, 1.0f, 1f, 0.45f)
        }
        2 -> {
          drawSphere(fx, stemH, fz, 0.26f, 0.08f, 0.26f, 0.98f, 0.98f, 1.0f, 1f, 0.35f)
          drawSphere(fx, stemH + 0.035f, fz, 0.10f, 0.07f, 0.10f, 0.98f, 0.78f, 0.12f, 1f, 0.40f)
        }
        else -> {
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
    metallic: Float = 0.04f,
    roughness: Float = 0.82f,
    clearcoat: Float = 0.0f,
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
    submitBuffer(cubeBuf, cubeVertCount, r, g, b, a, emissive, metallic, roughness, clearcoat)
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
    metallic: Float = 0.05f,
    roughness: Float = 0.82f,
    clearcoat: Float = 0.0f,
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
    submitBuffer(uprightCylBuf, uprightCylVertCount, r, g, b, a, emissive, metallic, roughness, clearcoat)
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
    submitBuffer(coneBuf, coneVertCount, r, g, b, a, emissive, 0.02f, 0.86f, 0.0f)
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
    metallic: Float = 0.04f,
    roughness: Float = 0.78f,
    clearcoat: Float = 0.0f,
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
    submitBuffer(sphereBuf, sphereVertCount, r, g, b, a, emissive, metallic, roughness, clearcoat)
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
    metallic: Float = 0.05f,
    roughness: Float = 0.80f,
    clearcoat: Float = 0.0f,
  ) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, z)
    if (bankDeg != 0f) Matrix.rotateM(modelMatrix, 0, -bankDeg, 0f, 0f, 1f)
    if (spinDeg != 0f) Matrix.rotateM(modelMatrix, 0, -spinDeg, 1f, 0f, 0f)
    if (isCyl) Matrix.rotateM(modelMatrix, 0, 90f, 0f, 1f, 0f)
    Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
    when {
      isSphere -> submitBuffer(sphereBuf, sphereVertCount, r, g, b, 1f, emissive, metallic, roughness, clearcoat)
      isCyl -> submitBuffer(cylBuf, cylVertCount, r, g, b, 1f, emissive, metallic, roughness, clearcoat)
      else -> submitBuffer(cubeBuf, cubeVertCount, r, g, b, 1f, emissive, metallic, roughness, clearcoat)
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
    metallic: Float = 0.05f,
    roughness: Float = 0.80f,
    clearcoat: Float = 0.0f,
    texId: Int = whiteTexId,
    useTex: Float = 0f,
    uvScaleU: Float = 1f,
    uvScaleV: Float = 1f,
    uvScrollU: Float = 0f,
    uvScrollV: Float = 0f,
  ) {
    Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0)
    Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvMatrix, 0)
    GLES20.glUniformMatrix4fv(uMvpLoc, 1, false, mvpMatrix, 0)
    GLES20.glUniformMatrix4fv(uModelLoc, 1, false, modelMatrix, 0)
    GLES20.glUniform4f(uColorLoc, r, g, b, a)
    GLES20.glUniform1f(uEmissiveLoc, emissive)
    GLES20.glUniform4f(uMatParamsLoc, metallic, roughness, clearcoat, useTex)
    GLES20.glUniform4f(uUvScaleScrollLoc, uvScaleU, uvScaleV, uvScrollU, uvScrollV)
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)

    val stride = 8 * 4
    buf.position(0)
    GLES20.glEnableVertexAttribArray(aPosLoc)
    GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, stride, buf)
    buf.position(3)
    GLES20.glEnableVertexAttribArray(aNormLoc)
    GLES20.glVertexAttribPointer(aNormLoc, 3, GLES20.GL_FLOAT, false, stride, buf)
    if (aUvLoc >= 0) {
      buf.position(6)
      GLES20.glEnableVertexAttribArray(aUvLoc)
      GLES20.glVertexAttribPointer(aUvLoc, 2, GLES20.GL_FLOAT, false, stride, buf)
    }
    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertCount)
    GLES20.glDisableVertexAttribArray(aPosLoc)
    GLES20.glDisableVertexAttribArray(aNormLoc)
    if (aUvLoc >= 0) GLES20.glDisableVertexAttribArray(aUvLoc)
  }

  private fun compileShader(type: Int, src: String): Int {
    val shader = GLES20.glCreateShader(type)
    GLES20.glShaderSource(shader, src)
    GLES20.glCompileShader(shader)
    return shader
  }

  private fun buildCubeMesh() {
    val d = floatArrayOf(
      // Front (+Z)
      -0.5f, -0.5f, 0.5f, 0f, 0f, 1f, 0f, 1f, 0.5f, -0.5f, 0.5f, 0f, 0f, 1f, 1f, 1f, 0.5f, 0.5f, 0.5f, 0f, 0f, 1f, 1f, 0f,
      -0.5f, -0.5f, 0.5f, 0f, 0f, 1f, 0f, 1f, 0.5f, 0.5f, 0.5f, 0f, 0f, 1f, 1f, 0f, -0.5f, 0.5f, 0.5f, 0f, 0f, 1f, 0f, 0f,
      // Back (-Z)
      0.5f, -0.5f, -0.5f, 0f, 0f, -1f, 0f, 1f, -0.5f, -0.5f, -0.5f, 0f, 0f, -1f, 1f, 1f, -0.5f, 0.5f, -0.5f, 0f, 0f, -1f, 1f, 0f,
      0.5f, -0.5f, -0.5f, 0f, 0f, -1f, 0f, 1f, -0.5f, 0.5f, -0.5f, 0f, 0f, -1f, 1f, 0f, 0.5f, 0.5f, -0.5f, 0f, 0f, -1f, 0f, 0f,
      // Top (+Y) — U across X, V along Z for scrolling dirt trail
      -0.5f, 0.5f, 0.5f, 0f, 1f, 0f, 0f, 1f, 0.5f, 0.5f, 0.5f, 0f, 1f, 0f, 1f, 1f, 0.5f, 0.5f, -0.5f, 0f, 1f, 0f, 1f, 0f,
      -0.5f, 0.5f, 0.5f, 0f, 1f, 0f, 0f, 1f, 0.5f, 0.5f, -0.5f, 0f, 1f, 0f, 1f, 0f, -0.5f, 0.5f, -0.5f, 0f, 1f, 0f, 0f, 0f,
      // Bottom (-Y)
      -0.5f, -0.5f, -0.5f, 0f, -1f, 0f, 0f, 1f, 0.5f, -0.5f, -0.5f, 0f, -1f, 0f, 1f, 1f, 0.5f, -0.5f, 0.5f, 0f, -1f, 0f, 1f, 0f,
      -0.5f, -0.5f, -0.5f, 0f, -1f, 0f, 0f, 1f, 0.5f, -0.5f, 0.5f, 0f, -1f, 0f, 1f, 0f, -0.5f, -0.5f, 0.5f, 0f, -1f, 0f, 0f, 0f,
      // Right (+X)
      0.5f, -0.5f, 0.5f, 1f, 0f, 0f, 0f, 1f, 0.5f, -0.5f, -0.5f, 1f, 0f, 0f, 1f, 1f, 0.5f, 0.5f, -0.5f, 1f, 0f, 0f, 1f, 0f,
      0.5f, -0.5f, 0.5f, 1f, 0f, 0f, 0f, 1f, 0.5f, 0.5f, -0.5f, 1f, 0f, 0f, 1f, 0f, 0.5f, 0.5f, 0.5f, 1f, 0f, 0f, 0f, 0f,
      // Left (-X)
      -0.5f, -0.5f, -0.5f, -1f, 0f, 0f, 0f, 1f, -0.5f, -0.5f, 0.5f, -1f, 0f, 0f, 1f, 1f, -0.5f, 0.5f, 0.5f, -1f, 0f, 0f, 1f, 0f,
      -0.5f, -0.5f, -0.5f, -1f, 0f, 0f, 0f, 1f, -0.5f, 0.5f, 0.5f, -1f, 0f, 0f, 1f, 0f, -0.5f, 0.5f, -0.5f, -1f, 0f, 0f, 0f, 0f,
    )
    cubeVertCount = d.size / 8
    cubeBuf = allocBuffer(d)
  }

  private fun buildCylinderMesh(segs: Int) {
    val verts = ArrayList<Float>(segs * 12 * 8)
    for (i in 0 until segs) {
      val a0 = (i * 2.0 * PI / segs).toFloat()
      val a1 = ((i + 1) * 2.0 * PI / segs).toFloat()
      val u0 = i.toFloat() / segs
      val u1 = (i + 1).toFloat() / segs
      val x0 = cos(a0) * 0.5f
      val y0 = sin(a0) * 0.5f
      val x1 = cos(a1) * 0.5f
      val y1 = sin(a1) * 0.5f
      val nx0 = cos(a0)
      val ny0 = sin(a0)
      val nx1 = cos(a1)
      val ny1 = sin(a1)
      verts.addAll(listOf(x0, y0, 0.5f, nx0, ny0, 0f, u0, 1f, x1, y1, 0.5f, nx1, ny1, 0f, u1, 1f, x1, y1, -0.5f, nx1, ny1, 0f, u1, 0f))
      verts.addAll(listOf(x0, y0, 0.5f, nx0, ny0, 0f, u0, 1f, x1, y1, -0.5f, nx1, ny1, 0f, u1, 0f, x0, y0, -0.5f, nx0, ny0, 0f, u0, 0f))
      verts.addAll(listOf(0f, 0f, 0.5f, 0f, 0f, 1f, 0.5f, 0.5f, x0, y0, 0.5f, 0f, 0f, 1f, 0.5f + x0, 0.5f + y0, x1, y1, 0.5f, 0f, 0f, 1f, 0.5f + x1, 0.5f + y1))
      verts.addAll(listOf(0f, 0f, -0.5f, 0f, 0f, -1f, 0.5f, 0.5f, x1, y1, -0.5f, 0f, 0f, -1f, 0.5f + x1, 0.5f + y1, x0, y0, -0.5f, 0f, 0f, -1f, 0.5f + x0, 0.5f + y0))
    }
    cylVertCount = verts.size / 8
    cylBuf = allocBuffer(verts.toFloatArray())
  }

  private fun buildUprightCylinderMesh(segs: Int) {
    val verts = ArrayList<Float>(segs * 12 * 8)
    for (i in 0 until segs) {
      val a0 = (i * 2.0 * PI / segs).toFloat()
      val a1 = ((i + 1) * 2.0 * PI / segs).toFloat()
      val u0 = i.toFloat() / segs
      val u1 = (i + 1).toFloat() / segs
      val nx0 = cos(a0)
      val nz0 = sin(a0)
      val nx1 = cos(a1)
      val nz1 = sin(a1)
      val x0 = nx0 * 0.5f
      val z0 = nz0 * 0.5f
      val x1 = nx1 * 0.5f
      val z1 = nz1 * 0.5f
      verts.addAll(listOf(x0, 0.5f, z0, nx0, 0f, nz0, u0, 1f, x1, 0.5f, z1, nx1, 0f, nz1, u1, 1f, x1, -0.5f, z1, nx1, 0f, nz1, u1, 0f))
      verts.addAll(listOf(x0, 0.5f, z0, nx0, 0f, nz0, u0, 1f, x1, -0.5f, z1, nx1, 0f, nz1, u1, 0f, x0, -0.5f, z0, nx0, 0f, nz0, u0, 0f))
      verts.addAll(listOf(0f, 0.5f, 0f, 0f, 1f, 0f, 0.5f, 0.5f, x0, 0.5f, z0, 0f, 1f, 0f, 0.5f + x0, 0.5f + z0, x1, 0.5f, z1, 0f, 1f, 0f, 0.5f + x1, 0.5f + z1))
      verts.addAll(listOf(0f, -0.5f, 0f, 0f, -1f, 0f, 0.5f, 0.5f, x1, -0.5f, z1, 0f, -1f, 0f, 0.5f + x1, 0.5f + z1, x0, -0.5f, z0, 0f, -1f, 0f, 0.5f + x0, 0.5f + z0))
    }
    uprightCylVertCount = verts.size / 8
    uprightCylBuf = allocBuffer(verts.toFloatArray())
  }

  private fun buildConeMesh(segs: Int) {
    val verts = ArrayList<Float>(segs * 6 * 8)
    val slopeY = 0.4472136f
    val slopeR = 0.8944272f
    for (i in 0 until segs) {
      val a0 = (i * 2.0 * PI / segs).toFloat()
      val a1 = ((i + 1) * 2.0 * PI / segs).toFloat()
      val u0 = i.toFloat() / segs
      val u1 = (i + 1).toFloat() / segs
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
      verts.addAll(
        listOf(
          0f, 0.5f, 0f, nxMid, slopeY, nzMid, (u0 + u1) * 0.5f, 1f,
          x0, -0.5f, z0, nx0, slopeY, nz0, u0, 0f,
          x1, -0.5f, z1, nx1, slopeY, nz1, u1, 0f,
        )
      )
      verts.addAll(
        listOf(
          0f, -0.5f, 0f, 0f, -1f, 0f, 0.5f, 0.5f,
          x1, -0.5f, z1, 0f, -1f, 0f, 0.5f + x1, 0.5f + z1,
          x0, -0.5f, z0, 0f, -1f, 0f, 0.5f + x0, 0.5f + z0,
        )
      )
    }
    coneVertCount = verts.size / 8
    coneBuf = allocBuffer(verts.toFloatArray())
  }

  private fun buildSphereMesh(latBands: Int, lonBands: Int) {
    val verts = ArrayList<Float>(latBands * lonBands * 6 * 8)
    for (lat in 0 until latBands) {
      val v0 = lat.toFloat() / latBands
      val v1 = (lat + 1).toFloat() / latBands
      val theta0 = (lat * PI / latBands).toFloat()
      val theta1 = ((lat + 1) * PI / latBands).toFloat()
      val sinT0 = sin(theta0)
      val cosT0 = cos(theta0)
      val sinT1 = sin(theta1)
      val cosT1 = cos(theta1)
      for (lon in 0 until lonBands) {
        val u0 = lon.toFloat() / lonBands
        val u1 = (lon + 1).toFloat() / lonBands
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
            nx00 * 0.5f, ny00 * 0.5f, nz00 * 0.5f, nx00, ny00, nz00, u0, v0,
            nx01 * 0.5f, ny01 * 0.5f, nz01 * 0.5f, nx01, ny01, nz01, u0, v1,
            nx11 * 0.5f, ny11 * 0.5f, nz11 * 0.5f, nx11, ny11, nz11, u1, v1,
            nx00 * 0.5f, ny00 * 0.5f, nz00 * 0.5f, nx00, ny00, nz00, u0, v0,
            nx11 * 0.5f, ny11 * 0.5f, nz11 * 0.5f, nx11, ny11, nz11, u1, v1,
            nx10 * 0.5f, ny10 * 0.5f, nz10 * 0.5f, nx10, ny10, nz10, u1, v0,
          )
        )
      }
    }
    sphereVertCount = verts.size / 8
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
        runCatching { setEGLConfigChooser(8, 8, 8, 8, 16, 0) }
        setRenderer(renderer)
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY)
        glViewHolder[0] = this
      }
    },
  )
}
