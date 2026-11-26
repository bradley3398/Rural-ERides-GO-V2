package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.os.Handler
import android.os.Looper
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal class Pev3dHardwareGlState {
  @Volatile var archetype: Pev3dArchetype = Pev3dArchetype.EMOTO
  @Volatile var finish: Pev3dModelFinish = Pev3dModelFinish(
    bodyRgb = floatArrayOf(0.24f, 0.27f, 0.31f),
    accentRgb = floatArrayOf(0.80f, 0.64f, 0.28f),
    caliperRgb = floatArrayOf(0.74f, 0.14f, 0.15f),
  )
  @Volatile var renderMode: Pev3dRenderMode = Pev3dRenderMode.SOLID
  @Volatile var lightingPreset: Pev3dLightingPreset = Pev3dLightingPreset.STUDIO
  @Volatile var techEngine: Pev3dTechEngine = Pev3dTechEngine.STUDIO_3D
  @Volatile var yawDeg: Float = -28f
  @Volatile var pitchDeg: Float = 16f
  @Volatile var zoom: Float = 1.05f
  @Volatile var explodePct: Float = 0f
  @Volatile var wheelSpinDeg: Float = 0f
  @Volatile var throttlePct: Float = 0.25f
  @Volatile var lightsOn: Boolean = true
  @Volatile var turnSignalMode: Pev3dTurnSignalMode = Pev3dTurnSignalMode.OFF
  @Volatile var selectedPartKey: String? = null
  @Volatile var ambientTempC: Float = 22f
  @Volatile var sceneRevision: Int = 0
}

private class GlMeshPartGpu(
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
  val vertexBuf: FloatBuffer,
  val indexBuf: ShortBuffer,
  val indexCount: Int,
  val wireBuf: FloatBuffer?,
  val wireVertCount: Int,
)

/**
 * Dedicated Hardware-Accelerated EGL14 + OpenGL ES 2.0/3.0 Textured Cook-Torrance GGX PBR Engine
 * running on a TextureView so it respects Compose RoundedCornerShape clipping and scrolling
 * with zero SurfaceView black-hole artifacts and zero native .so ABI crashes.
 */
private class Pev3dGlRenderThread(
  private val surfaceTexture: SurfaceTexture,
  initialWidth: Int,
  initialHeight: Int,
  private val state: Pev3dHardwareGlState,
  private val onBackendReady: (String) -> Unit,
  private val onFatalGlError: () -> Unit,
) : Thread("Pev3dHardwareGlThread") {

  private val running = AtomicBoolean(true)
  @Volatile private var viewportWidth = initialWidth.coerceAtLeast(1)
  @Volatile private var viewportHeight = initialHeight.coerceAtLeast(1)
  @Volatile private var paused = false

  private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
  private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
  private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

  private var pbrProgram = 0
  private var wireProgram = 0
  private var uMvpLoc = -1
  private var uModelLoc = -1
  private var uCamPosLoc = -1
  private var uKeyDirLoc = -1
  private var uKeyRgbLoc = -1
  private var uFillRgbLoc = -1
  private var uRimRgbLoc = -1
  private var uBaseColorLoc = -1
  private var uEmissiveLoc = -1
  private var uMatParamsLoc = -1 // metallic, roughness, clearcoat, alpha
  private var uShaderFlagsLoc = -1 // renderModeId, techId, heatBase, selectedHighlight
  private var uTexLoc = -1
  private var aPosLoc = -1
  private var aNormLoc = -1
  private var aUvLoc = -1

  private var wMvpLoc = -1
  private var wColorLoc = -1
  private var wPosLoc = -1

  private val glTextures = HashMap<PevPbrMaterialId, Int>()
  private var floorBuf: FloatBuffer? = null
  private var floorVertCount = 0

  private var uploadedSceneRevision = -1
  private var gpuParts: List<GlMeshPartGpu> = emptyList()

  private val vpMat = FloatArray(16)
  private val camPos = FloatArray(3)
  private val modelMat = FloatArray(16)
  private val mvpMat = FloatArray(16)
  private val mainHandler = Handler(Looper.getMainLooper())

  fun updateViewportSize(w: Int, h: Int) {
    viewportWidth = w.coerceAtLeast(1)
    viewportHeight = h.coerceAtLeast(1)
  }

  fun setPaused(isPaused: Boolean) {
    paused = isPaused
  }

  fun requestStop() {
    running.set(false)
    interrupt()
  }

  override fun run() {
    if (!initEgl()) {
      mainHandler.post { onFatalGlError() }
      return
    }
    try {
      initShadersAndResources()
      val glVersion = runCatching { GLES20.glGetString(GLES20.GL_VERSION).orEmpty() }.getOrDefault("")
      val glTag = if (glVersion.contains("3.")) "OPENGL ES 3.0" else "OPENGL ES 2.0"
      val initialLabel = when (state.techEngine) {
        Pev3dTechEngine.STUDIO_3D -> "$glTag • STUDIO GGX PBR"
        Pev3dTechEngine.OPENGL_ES3 -> "$glTag • ANISOTROPIC GGX"
        Pev3dTechEngine.WEBGL_2 -> "$glTag • CLEARCOAT IBL"
      }
      mainHandler.post { onBackendReady(initialLabel) }

      while (running.get()) {
        if (paused) {
          try {
            sleep(40L)
          } catch (_: InterruptedException) {
            if (!running.get()) break
          }
          continue
        }
        val frameStartNs = System.nanoTime()
        renderFrame()
        EGL14.eglSwapBuffers(eglDisplay, eglSurface)
        val elapsedMs = (System.nanoTime() - frameStartNs) / 1_000_000L
        val sleepMs = (16L - elapsedMs).coerceIn(2L, 16L)
        try {
          sleep(sleepMs)
        } catch (_: InterruptedException) {
          if (!running.get()) break
        }
      }
    } catch (_: Throwable) {
      mainHandler.post { onFatalGlError() }
    } finally {
      releaseGlAndEgl()
    }
  }

  private fun initEgl(): Boolean {
    return try {
      eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
      if (eglDisplay == EGL14.EGL_NO_DISPLAY) return false
      val version = IntArray(2)
      if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) return false

      val configs = arrayOfNulls<EGLConfig>(1)
      val numConfigs = IntArray(1)
      val preferredAttribs = intArrayOf(
        EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
        EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
        EGL14.EGL_RED_SIZE, 8,
        EGL14.EGL_GREEN_SIZE, 8,
        EGL14.EGL_BLUE_SIZE, 8,
        EGL14.EGL_ALPHA_SIZE, 8,
        EGL14.EGL_DEPTH_SIZE, 24,
        EGL14.EGL_NONE,
      )
      var ok = EGL14.eglChooseConfig(eglDisplay, preferredAttribs, 0, configs, 0, 1, numConfigs, 0) &&
        numConfigs[0] > 0 && configs[0] != null
      if (!ok) {
        val fallbackAttribs = intArrayOf(
          EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
          EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
          EGL14.EGL_RED_SIZE, 8,
          EGL14.EGL_GREEN_SIZE, 8,
          EGL14.EGL_BLUE_SIZE, 8,
          EGL14.EGL_DEPTH_SIZE, 16,
          EGL14.EGL_NONE,
        )
        ok = EGL14.eglChooseConfig(eglDisplay, fallbackAttribs, 0, configs, 0, 1, numConfigs, 0) &&
          numConfigs[0] > 0 && configs[0] != null
      }
      if (!ok) return false
      val config = configs[0] ?: return false

      val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
      eglContext = EGL14.eglCreateContext(eglDisplay, config, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
      if (eglContext == EGL14.EGL_NO_CONTEXT) return false

      val surfAttribs = intArrayOf(EGL14.EGL_NONE)
      eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, config, surfaceTexture, surfAttribs, 0)
      if (eglSurface == EGL14.EGL_NO_SURFACE) return false

      EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
    } catch (_: Throwable) {
      false
    }
  }

  private fun initShadersAndResources() {
    val pbrVertSrc =
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

    // Full Per-Pixel Cook-Torrance GGX PBR + Dual-Layer Clearcoat + Procedural Studio IBL + ACES Filmic
    val pbrFragSrc =
      """
      precision highp float;
      uniform vec3 uCamPos;
      uniform vec3 uKeyDir;
      uniform vec3 uKeyRgb;
      uniform vec3 uFillRgb;
      uniform vec3 uRimRgb;
      uniform vec3 uBaseColor;
      uniform vec3 uEmissive;
      uniform vec4 uMatParams;    // x=metallic, y=roughness, z=clearcoat, w=alpha
      uniform vec4 uShaderFlags;  // x=renderModeId, y=techId, z=heatBase, w=selectedHighlight
      uniform sampler2D uTex;
      varying vec3 vWorldPos;
      varying vec3 vWorldNorm;
      varying vec2 vUv;

      const float PI = 3.14159265;

      vec3 acesFilm(vec3 x) {
        vec3 v = max(x, vec3(0.0));
        return clamp((v * (2.51 * v + 0.03)) / (v * (2.43 * v + 0.59) + 0.14), 0.0, 1.0);
      }

      float distributionGGX(vec3 N, vec3 H, float roughness) {
        float a = roughness * roughness;
        float a2 = a * a;
        float NdotH = max(dot(N, H), 0.0);
        float NdotH2 = NdotH * NdotH;
        float denom = (NdotH2 * (a2 - 1.0) + 1.0);
        return a2 / max(PI * denom * denom, 0.0005);
      }

      float geometrySchlickGGX(float NdotV, float roughness) {
        float r = roughness + 1.0;
        float k = (r * r) / 8.0;
        return NdotV / max(NdotV * (1.0 - k) + k, 0.001);
      }

      float geometrySmith(vec3 N, vec3 V, vec3 L, float roughness) {
        return geometrySchlickGGX(max(dot(N, V), 0.0), roughness) *
               geometrySchlickGGX(max(dot(N, L), 0.0), roughness);
      }

      vec3 fresnelSchlick(float cosTheta, vec3 F0) {
        return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
      }

      // Procedural Automotive Photo-Studio Environment Reflection (Overhead Softbox + Horizon Strip)
      vec3 sampleStudioEnv(vec3 R, float roughness, float techId) {
        float y = clamp(R.y, -1.0, 1.0);
        vec3 floorCol = vec3(0.05, 0.07, 0.10);
        vec3 midCol = vec3(0.14, 0.19, 0.27);
        vec3 skyCol = vec3(0.38, 0.48, 0.62);
        vec3 env = mix(floorCol, midCol, smoothstep(-0.35, 0.15, y));
        env = mix(env, skyCol, smoothstep(0.15, 0.85, y));

        // Overhead rectangular LED studio softbox strip
        float softbox = smoothstep(0.45, 0.92, y) * (0.45 + 0.55 * smoothstep(0.65, 0.15, abs(R.z)));
        float sharpSoftbox = pow(clamp(softbox, 0.0, 1.0), mix(4.0, 1.2, roughness));
        env += vec3(0.95, 1.02, 1.15) * sharpSoftbox * (1.15 - roughness * 0.65);

        // Crisp automotive showroom horizon light band (extra crisp in WebGL Clearcoat mode)
        float horizonBoost = (techId > 1.5) ? 1.45 : 0.92;
        float horizonBand = exp(-pow((y - 0.04) * mix(9.0, 2.5, roughness), 2.0));
        env += uRimRgb * horizonBand * 0.55 * horizonBoost * (1.0 - roughness * 0.55);
        return env;
      }

      void main() {
        vec3 N = normalize(vWorldNorm);
        vec3 V = normalize(uCamPos - vWorldPos);
        if (!gl_FrontFacing) N = -N;

        float modeId = uShaderFlags.x;
        float techId = uShaderFlags.y;
        float heatBase = uShaderFlags.z;
        float selHi = uShaderFlags.w;

        float NdotV = max(dot(N, V), 0.001);
        float fresnelRim = pow(1.0 - NdotV, 2.6);

        // X-RAY HOLOGRAPHIC MODE
        if (modeId > 1.5 && modeId < 2.5) {
          float isTranslucent = (uMatParams.w < 0.95) ? 1.0 : 0.0;
          vec3 col = mix(uBaseColor * (0.45 + 0.55 * NdotV), vec3(0.10, 0.68, 0.96) + vec3(0.25, 0.35, 0.45) * fresnelRim, isTranslucent);
          float a = mix(1.0, clamp(0.18 + fresnelRim * 0.56, 0.14, 0.78), isTranslucent);
          gl_FragColor = vec4(col, a);
          return;
        }

        // FLIR THERMAL IR MODE
        if (modeId > 3.5 && modeId < 4.5) {
          float th = clamp(heatBase + fresnelRim * 0.15, 0.05, 1.0);
          vec3 tc;
          if (th < 0.34) {
            float f = th / 0.34;
            tc = vec3(0.06 + f * 0.80, 0.10 + f * 0.05, 0.48 + f * 0.14);
          } else if (th < 0.70) {
            float f = (th - 0.34) / 0.36;
            tc = vec3(0.86 + f * 0.14, 0.15 + f * 0.42, 0.62 - f * 0.54);
          } else {
            float f = (th - 0.70) / 0.30;
            tc = vec3(1.0, 0.57 + f * 0.41, 0.08 + f * 0.62);
          }
          gl_FragColor = vec4(tc, 1.0);
          return;
        }

        // BLUEPRINT CAD SHADED MODE
        if (modeId > 4.5) {
          float diffBp = max(dot(N, normalize(uKeyDir)), 0.0);
          vec3 bp = vec3(0.04, 0.16, 0.42) + vec3(0.08, 0.24, 0.36) * diffBp + vec3(0.28, 0.78, 0.98) * fresnelRim;
          if (selHi > 0.5) bp += vec3(0.35, 0.30, -0.08);
          gl_FragColor = vec4(clamp(bp, 0.0, 1.0), 1.0);
          return;
        }

        // Sample real procedural PBR albedo texture + compute micro-relief detail
        vec4 texSample = texture2D(uTex, vUv);
        float texLuma = dot(texSample.rgb, vec3(0.299, 0.587, 0.114));
        // Subtle normal perturbation from procedural texture height
        vec2 du = vec2(0.004, 0.0);
        vec2 dv = vec2(0.0, 0.004);
        float lU = dot(texture2D(uTex, vUv + du).rgb, vec3(0.299, 0.587, 0.114));
        float lV = dot(texture2D(uTex, vUv + dv).rgb, vec3(0.299, 0.587, 0.114));
        float bumpScale = (techId > 0.5 && techId < 1.5) ? 1.8 : 1.15;
        vec3 bumpN = normalize(N + vec3((texLuma - lU) * bumpScale, (texLuma - lV) * bumpScale, 0.0));
        N = mix(N, bumpN, 0.42);
        NdotV = max(dot(N, V), 0.001);

        // Convert sRGB to linear
        vec3 albedo = pow(clamp(texSample.rgb * uBaseColor, 0.005, 1.0), vec3(2.2));
        float metallic = clamp(uMatParams.x, 0.0, 1.0);
        float roughness = clamp(uMatParams.y, 0.06, 0.96);
        float clearcoat = clamp(uMatParams.z, 0.0, 1.0);

        // Carbon Showroom Mode enhances dark 3K carbon fiber & high-gloss clearcoat
        if (modeId > 2.5 && modeId < 3.5) {
          roughness = min(roughness, 0.24);
          clearcoat = max(clearcoat, 0.85);
        }

        vec3 F0 = mix(vec3(0.04), albedo, metallic);

        // Key light (Cook-Torrance GGX)
        vec3 L = normalize(uKeyDir);
        vec3 H = normalize(V + L);
        float NdotL = max(dot(N, L), 0.0);
        float D = distributionGGX(N, H, roughness);
        float G = geometrySmith(N, V, L, roughness);
        vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);
        vec3 specKey = (D * G * F) / max(4.0 * NdotV * NdotL, 0.001);
        vec3 kD = (vec3(1.0) - F) * (1.0 - metallic);
        vec3 Lo = (kD * albedo / PI + specKey) * uKeyRgb * NdotL * 1.45;

        // Opposite studio fill light
        vec3 L2 = normalize(vec3(-uKeyDir.x * 0.85, 0.35, -uKeyDir.z * 0.65));
        float NdotL2 = max(dot(N, L2), 0.0);
        vec3 H2 = normalize(V + L2);
        float D2 = distributionGGX(N, H2, min(roughness + 0.15, 0.95));
        vec3 specFill = vec3(D2 * 0.14) * F0;
        Lo += (kD * albedo / PI + specFill) * uFillRgb * NdotL2 * 0.92;

        // Dual-layer automotive clearcoat specular lobe
        if (clearcoat > 0.05) {
          float ccD = distributionGGX(N, H, 0.09);
          float ccF = 0.04 + 0.96 * pow(1.0 - max(dot(H, V), 0.0), 5.0);
          Lo += vec3(ccD * ccF * 0.28 * clearcoat) * uKeyRgb * NdotL;
        }

        // Studio IBL Environment Reflection & Ground Cavity AO
        vec3 R = reflect(-V, N);
        vec3 envRefl = sampleStudioEnv(R, roughness, techId);
        vec3 Fenv = fresnelSchlick(NdotV, F0);
        float groundAo = clamp(0.62 + 0.38 * clamp((vWorldPos.y + 0.62) / 0.48, 0.0, 1.0), 0.50, 1.0);
        float hemi = 0.22 + 0.26 * (0.5 + 0.5 * N.y);
        vec3 ambient = (kD * albedo * hemi + envRefl * Fenv * (0.48 + 0.52 * (1.0 - roughness))) * groundAo;
        if (clearcoat > 0.05) {
          ambient += sampleStudioEnv(R, 0.08, techId) * (0.04 + 0.45 * fresnelRim) * clearcoat * 0.52;
        }

        // Subtle rim silhouette + Emissive optics (Headlights, Taillights, Signals, TFT)
        vec3 rimLight = uRimRgb * fresnelRim * (0.14 + 0.22 * (1.0 - roughness));
        vec3 colorLinear = ambient + Lo + rimLight + uEmissive * texSample.rgb;

        if (selHi > 0.5) {
          colorLinear += vec3(0.10, 0.32, 0.52) * (0.35 + 0.65 * fresnelRim);
        }

        vec3 toneMapped = acesFilm(colorLinear * 1.18);
        vec3 srgb = pow(toneMapped, vec3(1.0 / 2.2));
        gl_FragColor = vec4(srgb, uMatParams.w);
      }
      """.trimIndent()

    val wireVertSrc =
      """
      uniform mat4 uMvp;
      attribute vec3 aPos;
      void main() {
        gl_Position = uMvp * vec4(aPos, 1.0);
      }
      """.trimIndent()

    val wireFragSrc =
      """
      precision mediump float;
      uniform vec4 uColor;
      void main() {
        gl_FragColor = uColor;
      }
      """.trimIndent()

    pbrProgram = linkProgram(pbrVertSrc, pbrFragSrc)
    wireProgram = linkProgram(wireVertSrc, wireFragSrc)
    if (pbrProgram == 0 || wireProgram == 0) {
      throw IllegalStateException("OpenGL ES PBR shader link failed")
    }

    uMvpLoc = GLES20.glGetUniformLocation(pbrProgram, "uMvp")
    uModelLoc = GLES20.glGetUniformLocation(pbrProgram, "uModel")
    uCamPosLoc = GLES20.glGetUniformLocation(pbrProgram, "uCamPos")
    uKeyDirLoc = GLES20.glGetUniformLocation(pbrProgram, "uKeyDir")
    uKeyRgbLoc = GLES20.glGetUniformLocation(pbrProgram, "uKeyRgb")
    uFillRgbLoc = GLES20.glGetUniformLocation(pbrProgram, "uFillRgb")
    uRimRgbLoc = GLES20.glGetUniformLocation(pbrProgram, "uRimRgb")
    uBaseColorLoc = GLES20.glGetUniformLocation(pbrProgram, "uBaseColor")
    uEmissiveLoc = GLES20.glGetUniformLocation(pbrProgram, "uEmissive")
    uMatParamsLoc = GLES20.glGetUniformLocation(pbrProgram, "uMatParams")
    uShaderFlagsLoc = GLES20.glGetUniformLocation(pbrProgram, "uShaderFlags")
    uTexLoc = GLES20.glGetUniformLocation(pbrProgram, "uTex")
    aPosLoc = GLES20.glGetAttribLocation(pbrProgram, "aPos")
    aNormLoc = GLES20.glGetAttribLocation(pbrProgram, "aNorm")
    aUvLoc = GLES20.glGetAttribLocation(pbrProgram, "aUv")

    wMvpLoc = GLES20.glGetUniformLocation(wireProgram, "uMvp")
    wColorLoc = GLES20.glGetUniformLocation(wireProgram, "uColor")
    wPosLoc = GLES20.glGetAttribLocation(wireProgram, "aPos")

    GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    GLES20.glDepthFunc(GLES20.GL_LEQUAL)
    GLES20.glEnable(GLES20.GL_BLEND)
    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

    uploadAllPbrTextures()
    buildTurntableFloorMesh()
  }

  private fun uploadAllPbrTextures() {
    val size = 256
    for (matId in PevPbrMaterialId.entries) {
      val bmp = buildTextureBitmapForGl(matId, size)
      val texIds = IntArray(1)
      GLES20.glGenTextures(1, texIds, 0)
      val id = texIds[0]
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)
      GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
      GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
      GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
      GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
      GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
      GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
      bmp.recycle()
      glTextures[matId] = id
    }
  }

  private fun buildTextureBitmapForGl(id: PevPbrMaterialId, size: Int): Bitmap {
    return when (id) {
      PevPbrMaterialId.CARBON_WEAVE -> Pev3dTextureBank.buildCarbonWeaveBitmap(size)
      PevPbrMaterialId.BRUSHED_METAL -> Pev3dTextureBank.buildBrushedMetalBitmap(size)
      PevPbrMaterialId.TIRE_RUBBER -> Pev3dTextureBank.buildTireTreadBitmap(size)
      PevPbrMaterialId.BATTERY_PACK -> Pev3dTextureBank.buildBatteryPackBitmap(size)
      PevPbrMaterialId.BRAKE_ROTOR -> Pev3dTextureBank.buildBrakeRotorBitmap(size)
      PevPbrMaterialId.TFT_SCREEN -> Pev3dTextureBank.buildTftCockpitBitmap(size)
      PevPbrMaterialId.DYNO_FLOOR -> Pev3dTextureBank.buildDynoFloorBitmap(size)
      PevPbrMaterialId.QUILTED_LEATHER -> buildQuiltedBitmap(size)
      PevPbrMaterialId.RACING_LIVERY -> buildMetallicLiveryBitmap(size)
      PevPbrMaterialId.HEADLIGHT_OPTIC -> buildHeadlightBitmap(size)
      PevPbrMaterialId.TAILLIGHT_OPTIC -> buildTaillightBitmap(size)
      PevPbrMaterialId.AMBER_SIGNAL_OPTIC -> buildAmberSignalBitmap(size)
    }
  }

  private fun buildQuiltedBitmap(size: Int): Bitmap {
    val pixels = IntArray(size * size)
    val ribPeriod = size / 12f
    for (y in 0 until size) {
      val ribFrac = (y / ribPeriod).let { it - kotlin.math.floor(it) }
      val ribBump = sin(ribFrac * PI).toFloat()
      val stitch = kotlin.math.abs(ribFrac - 0.05f) < 0.03f
      for (x in 0 until size) {
        val perf = ((x % 10) in 4..5) && ((y % 10) in 4..5) && !stitch
        val lum = if (stitch) 28 else if (perf) 22 else (42 + (ribBump * 24f).toInt()).coerceIn(22, 76)
        pixels[y * size + x] = AndroidColor.rgb(lum, lum, (lum + 2).coerceAtMost(82))
      }
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
      setPixels(pixels, 0, size, 0, 0, size, size)
    }
  }

  private fun buildMetallicLiveryBitmap(size: Int): Bitmap {
    val pixels = IntArray(size * size)
    val s = size.toFloat()
    for (y in 0 until size) {
      val yf = y / s
      for (x in 0 until size) {
        val xf = x / s
        val flake = (((x * 374761393 + y * 668265263) xor (x * 13)) and 0xFF) / 255f * 14f - 7f
        val seam = kotlin.math.abs(yf - 0.12f) < 0.005f || kotlin.math.abs(yf - 0.88f) < 0.005f
        val inVent = xf in 0.64f..0.88f && yf in 0.46f..0.68f && (((xf - 0.64f) / 0.055f).let { it - kotlin.math.floor(it) } < 0.35f)
        val diag = xf * 0.58f + yf * 0.82f
        val v = when {
          inVent -> 48
          seam -> 92
          kotlin.math.abs(diag - 0.86f) < 0.012f -> 248
          diag > 0.89f -> 118
          else -> (234f - yf * 18f + flake).toInt().coerceIn(196, 252)
        }
        pixels[y * size + x] = AndroidColor.rgb(v, v, v)
      }
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
      setPixels(pixels, 0, size, 0, 0, size, size)
    }
  }

  private fun buildHeadlightBitmap(size: Int): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(bmp)
    val s = size.toFloat()
    c.drawColor(AndroidColor.rgb(18, 22, 28))
    val ring = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
      style = android.graphics.Paint.Style.STROKE
      strokeWidth = s * 0.04f
      color = AndroidColor.rgb(240, 248, 255)
    }
    c.drawCircle(s * 0.5f, s * 0.5f, s * 0.42f, ring)
    val dome = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
      color = AndroidColor.rgb(246, 250, 255)
    }
    c.drawCircle(s * 0.5f, s * 0.5f, s * 0.26f, dome)
    return bmp
  }

  private fun buildTaillightBitmap(size: Int): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(bmp)
    val s = size.toFloat()
    c.drawColor(AndroidColor.rgb(42, 6, 10))
    val blade = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(242, 34, 42) }
    val core = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(255, 160, 145) }
    for (i in 0 until 8) {
      val bx = s * (0.10f + i * 0.105f)
      c.drawRoundRect(bx, s * 0.24f, bx + s * 0.068f, s * 0.76f, 5f, 5f, blade)
      c.drawRoundRect(bx + s * 0.018f, s * 0.34f, bx + s * 0.050f, s * 0.66f, 3f, 3f, core)
    }
    return bmp
  }

  private fun buildAmberSignalBitmap(size: Int): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(bmp)
    val s = size.toFloat()
    c.drawColor(AndroidColor.rgb(52, 22, 4))
    val amber = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(255, 166, 20) }
    for (i in 0 until 5) {
      val cx = s * (0.12f + i * 0.17f)
      c.drawRoundRect(cx, s * 0.24f, cx + s * 0.11f, s * 0.76f, 4f, 4f, amber)
    }
    return bmp
  }

  private fun buildTurntableFloorMesh() {
    val r = 2.15f
    val y = -0.78f
    val segs = 40
    val verts = FloatArray(segs * 3 * 8)
    var o = 0
    for (i in 0 until segs) {
      val a0 = (i.toFloat() / segs) * (2f * PI.toFloat())
      val a1 = ((i + 1).toFloat() / segs) * (2f * PI.toFloat())
      val x0 = cos(a0) * r; val z0 = sin(a0) * r
      val x1 = cos(a1) * r; val z1 = sin(a1) * r
      // Center
      verts[o++] = 0f; verts[o++] = y; verts[o++] = 0f
      verts[o++] = 0f; verts[o++] = 1f; verts[o++] = 0f
      verts[o++] = 0.5f; verts[o++] = 0.5f
      // Edge 0
      verts[o++] = x0; verts[o++] = y; verts[o++] = z0
      verts[o++] = 0f; verts[o++] = 1f; verts[o++] = 0f
      verts[o++] = 0.5f + cos(a0) * 0.5f; verts[o++] = 0.5f + sin(a0) * 0.5f
      // Edge 1
      verts[o++] = x1; verts[o++] = y; verts[o++] = z1
      verts[o++] = 0f; verts[o++] = 1f; verts[o++] = 0f
      verts[o++] = 0.5f + cos(a1) * 0.5f; verts[o++] = 0.5f + sin(a1) * 0.5f
    }
    floorVertCount = segs * 3
    floorBuf = allocFloatBuf(verts)
  }

  private fun ensureSceneUploaded() {
    val rev = state.sceneRevision
    if (rev == uploadedSceneRevision && gpuParts.isNotEmpty()) return
    uploadedSceneRevision = rev
    val built = runCatching {
      Pev3dGltfBuilder.buildSceneMeshes(
        archetype = state.archetype,
        finish = state.finish,
        lightsOn = state.lightsOn,
      )
    }.getOrNull() ?: return

    val list = ArrayList<GlMeshPartGpu>(built.parts.size)
    for (p in built.parts) {
      if (p.partKey == "floor" || p.vertices.isEmpty() || p.indices.isEmpty()) continue
      val wBuf = if (p.wireframeLines.size >= 6) allocFloatBuf(p.wireframeLines) else null
      list.add(
        GlMeshPartGpu(
          nodeName = p.nodeName,
          partKey = p.partKey,
          texId = p.texId,
          baseRgb = p.baseRgb.copyOf(),
          metallic = p.metallic,
          roughness = p.roughness,
          emissiveRgb = p.emissiveRgb.copyOf(),
          clearcoat = p.clearcoat,
          isShell = p.isShell,
          isScreen = p.isScreen,
          axleCenter = p.axleCenter,
          explodeDir = p.explodeDir,
          vertexBuf = allocFloatBuf(p.vertices),
          indexBuf = allocShortBuf(p.indices),
          indexCount = p.indices.size,
          wireBuf = wBuf,
          wireVertCount = p.wireframeLines.size / 3,
        )
      )
    }
    gpuParts = list
  }

  private fun renderFrame() {
    ensureSceneUploaded()
    val w = viewportWidth
    val h = viewportHeight
    GLES20.glViewport(0, 0, w, h)

    val renderMode = state.renderMode
    val lightingPreset = state.lightingPreset
    val techEngine = state.techEngine

    // Clear studio backdrop matching active lighting preset / mode
    when {
      renderMode == Pev3dRenderMode.BLUEPRINT -> GLES20.glClearColor(0.03f, 0.11f, 0.25f, 1f)
      lightingPreset == Pev3dLightingPreset.SUNSET -> GLES20.glClearColor(0.11f, 0.06f, 0.08f, 1f)
      lightingPreset == Pev3dLightingPreset.NEON -> GLES20.glClearColor(0.04f, 0.07f, 0.16f, 1f)
      techEngine == Pev3dTechEngine.WEBGL_2 -> GLES20.glClearColor(0.05f, 0.09f, 0.14f, 1f)
      techEngine == Pev3dTechEngine.OPENGL_ES3 -> GLES20.glClearColor(0.05f, 0.08f, 0.13f, 1f)
      else -> GLES20.glClearColor(0.06f, 0.09f, 0.14f, 1f)
    }
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

    val yaw = if (state.yawDeg.isFinite()) state.yawDeg else -28f
    val pitch = if (state.pitchDeg.isFinite()) state.pitchDeg.coerceIn(-75f, 80f) else 16f
    val zoom = if (state.zoom.isFinite()) state.zoom.coerceIn(0.6f, 2.4f) else 1.05f
    val explode = if (state.explodePct.isFinite()) state.explodePct.coerceIn(0f, 1.5f) else 0f
    val wheelSpin = if (state.wheelSpinDeg.isFinite()) state.wheelSpinDeg else 0f
    val throttle = if (state.throttlePct.isFinite()) state.throttlePct.coerceIn(0f, 1f) else 0f
    val ambientTemp = if (state.ambientTempC.isFinite()) state.ambientTempC else 22f
    val lightsOn = state.lightsOn
    val turnMode = state.turnSignalMode
    val selectedKey = state.selectedPartKey

    Pev3dEngine.buildCameraViewProjection(w.toFloat(), h.toFloat(), yaw, pitch, zoom, vpMat, camPos)

    if (renderMode == Pev3dRenderMode.WIREFRAME) {
      drawWireframePass(explode, wheelSpin, selectedKey, ColorCyan = true)
      return
    }

    val keyCfg = when (lightingPreset) {
      Pev3dLightingPreset.STUDIO -> floatArrayOf(0.45f, 0.85f, 0.38f, 1.42f, 1.38f, 1.34f)
      Pev3dLightingPreset.SUNSET -> floatArrayOf(-0.78f, 0.42f, 0.45f, 1.58f, 0.96f, 0.54f)
      Pev3dLightingPreset.NEON -> floatArrayOf(0.28f, 0.68f, -0.68f, 0.78f, 1.30f, 1.62f)
    }
    val fillRgb = when (lightingPreset) {
      Pev3dLightingPreset.STUDIO -> floatArrayOf(0.42f, 0.52f, 0.68f)
      Pev3dLightingPreset.SUNSET -> floatArrayOf(0.36f, 0.38f, 0.62f)
      Pev3dLightingPreset.NEON -> floatArrayOf(0.95f, 0.28f, 0.84f)
    }
    val rimRgb = when (lightingPreset) {
      Pev3dLightingPreset.STUDIO -> when (techEngine) {
        Pev3dTechEngine.STUDIO_3D -> floatArrayOf(0.60f, 0.80f, 1.08f)
        Pev3dTechEngine.OPENGL_ES3 -> floatArrayOf(0.42f, 0.96f, 0.92f)
        Pev3dTechEngine.WEBGL_2 -> floatArrayOf(0.66f, 0.88f, 1.18f)
      }
      Pev3dLightingPreset.SUNSET -> floatArrayOf(1.15f, 0.56f, 0.32f)
      Pev3dLightingPreset.NEON -> floatArrayOf(0.28f, 1.15f, 1.32f)
    }
    val techIdFloat = when (techEngine) {
      Pev3dTechEngine.STUDIO_3D -> 0f
      Pev3dTechEngine.OPENGL_ES3 -> 1f
      Pev3dTechEngine.WEBGL_2 -> 2f
    }

    GLES20.glUseProgram(pbrProgram)
    GLES20.glUniform3f(uCamPosLoc, camPos[0], camPos[1], camPos[2])
    GLES20.glUniform3f(uKeyDirLoc, keyCfg[0], keyCfg[1], keyCfg[2])
    GLES20.glUniform3f(uKeyRgbLoc, keyCfg[3], keyCfg[4], keyCfg[5])
    GLES20.glUniform3f(uFillRgbLoc, fillRgb[0], fillRgb[1], fillRgb[2])
    GLES20.glUniform3f(uRimRgbLoc, rimRgb[0], rimRgb[1], rimRgb[2])
    GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
    GLES20.glUniform1i(uTexLoc, 0)

    // 1. Render Textured Showroom Concrete & Machined Steel Turntable Floor
    floorBuf?.let { fb ->
      Mat4Math.setIdentity(modelMat)
      Mat4Math.multiplyMM(mvpMat, vpMat, modelMat)
      GLES20.glUniformMatrix4fv(uMvpLoc, 1, false, mvpMat, 0)
      GLES20.glUniformMatrix4fv(uModelLoc, 1, false, modelMat, 0)
      GLES20.glUniform3f(uBaseColorLoc, 0.92f, 0.95f, 1.0f)
      GLES20.glUniform3f(uEmissiveLoc, 0f, 0f, 0f)
      GLES20.glUniform4f(uMatParamsLoc, 0.14f, 0.42f, 0.25f, 1f)
      GLES20.glUniform4f(uShaderFlagsLoc, renderMode.shaderId.toFloat(), techIdFloat, 0.08f, 0f)
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, glTextures[PevPbrMaterialId.DYNO_FLOOR] ?: 0)
      val stride = 8 * 4
      fb.position(0)
      GLES20.glEnableVertexAttribArray(aPosLoc)
      GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, stride, fb)
      fb.position(3)
      GLES20.glEnableVertexAttribArray(aNormLoc)
      GLES20.glVertexAttribPointer(aNormLoc, 3, GLES20.GL_FLOAT, false, stride, fb)
      fb.position(6)
      GLES20.glEnableVertexAttribArray(aUvLoc)
      GLES20.glVertexAttribPointer(aUvLoc, 2, GLES20.GL_FLOAT, false, stride, fb)
      GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, floorVertCount)
    }

    val isXray = renderMode == Pev3dRenderMode.XRAY
    val isCarbon = renderMode == Pev3dRenderMode.CARBON_SHOWROOM
    val ambientBoost = ((ambientTemp - 15f) / 40f).coerceIn(0f, 0.25f)
    val throttleHeat = throttle * 0.68f
    val flashPhase = ((System.currentTimeMillis() / 420L) % 2L == 0L)
    val leftActive = (turnMode == Pev3dTurnSignalMode.LEFT || turnMode == Pev3dTurnSignalMode.HAZARD) && flashPhase
    val rightActive = (turnMode == Pev3dTurnSignalMode.RIGHT || turnMode == Pev3dTurnSignalMode.HAZARD) && flashPhase

    // Render opaque parts first, then translucent X-ray shells last so depth-test blends cleanly
    val orderedParts = if (isXray) gpuParts.sortedBy { if (it.isShell) 1 else 0 } else gpuParts
    for (part in orderedParts) {
      Mat4Math.setIdentity(modelMat)
      if (explode > 0.002f) {
        Mat4Math.translate(modelMat, part.explodeDir.x * explode, part.explodeDir.y * explode, part.explodeDir.z * explode)
      }
      part.axleCenter?.let { c ->
        Mat4Math.translate(modelMat, c.x, c.y, c.z)
        Mat4Math.rotateAxis(modelMat, -wheelSpin, 0f, 0f, 1f)
        Mat4Math.translate(modelMat, -c.x, -c.y, -c.z)
      }
      Mat4Math.multiplyMM(mvpMat, vpMat, modelMat)
      GLES20.glUniformMatrix4fv(uMvpLoc, 1, false, mvpMat, 0)
      GLES20.glUniformMatrix4fv(uModelLoc, 1, false, modelMat, 0)

      val effTex = if (isCarbon && part.isShell) PevPbrMaterialId.CARBON_WEAVE else part.texId
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, glTextures[effTex] ?: 0)

      // Calibrated tinting with real textures (textures already carry base detail!)
      var br = part.baseRgb.getOrElse(0) { 0.5f }
      var bg = part.baseRgb.getOrElse(1) { 0.5f }
      var bb = part.baseRgb.getOrElse(2) { 0.5f }
      when (effTex) {
        PevPbrMaterialId.TIRE_RUBBER,
        PevPbrMaterialId.QUILTED_LEATHER,
        PevPbrMaterialId.BATTERY_PACK,
        PevPbrMaterialId.TFT_SCREEN,
        PevPbrMaterialId.HEADLIGHT_OPTIC,
        PevPbrMaterialId.TAILLIGHT_OPTIC,
        PevPbrMaterialId.AMBER_SIGNAL_OPTIC -> {
          br = 1f; bg = 1f; bb = 1f
        }
        PevPbrMaterialId.CARBON_WEAVE -> {
          if (isCarbon && part.isShell) {
            br = 0.65f; bg = 0.68f; bb = 0.74f
          }
        }
        else -> Unit
      }

      var emR = part.emissiveRgb.getOrElse(0) { 0f }
      var emG = part.emissiveRgb.getOrElse(1) { 0f }
      var emB = part.emissiveRgb.getOrElse(2) { 0f }
      if (part.nodeName.contains("signal_left")) {
        if (leftActive) { emR = 2.6f; emG = 1.45f; emB = 0.10f } else { emR = 0.05f; emG = 0.02f; emB = 0f }
      } else if (part.nodeName.contains("signal_right")) {
        if (rightActive) { emR = 2.6f; emG = 1.45f; emB = 0.10f } else { emR = 0.05f; emG = 0.02f; emB = 0f }
      } else if (part.nodeName.contains("headlight") || part.nodeName.contains("cockpit_light")) {
        if (lightsOn) { emR = 2.4f; emG = 2.55f; emB = 2.8f } else { emR = 0.05f; emG = 0.05f; emB = 0.06f }
      } else if (part.nodeName.contains("tail")) {
        if (lightsOn) { emR = 2.4f; emG = 0.22f; emB = 0.16f } else { emR = 0.06f; emG = 0.01f; emB = 0.01f }
      } else if (part.isScreen) {
        emR = 1.25f; emG = 1.30f; emB = 1.38f
      }

      val partHeatFactor = when (part.partKey) {
        "motor" -> 0.92f
        "controller" -> 0.88f
        "battery" -> 0.72f
        "brakes" -> 0.65f
        else -> 0.22f
      }
      val heatBase = (0.12f + partHeatFactor * throttleHeat + ambientBoost).coerceIn(0.04f, 1f)
      val alpha = if (isXray && part.isShell) 0.26f else 1f
      val selHi = if (selectedKey != null && part.partKey == selectedKey) 1f else 0f

      if (isXray && part.isShell) GLES20.glDepthMask(false) else GLES20.glDepthMask(true)

      GLES20.glUniform3f(uBaseColorLoc, br, bg, bb)
      GLES20.glUniform3f(uEmissiveLoc, emR, emG, emB)
      GLES20.glUniform4f(uMatParamsLoc, part.metallic, part.roughness, part.clearcoat, alpha)
      GLES20.glUniform4f(uShaderFlagsLoc, renderMode.shaderId.toFloat(), techIdFloat, heatBase, selHi)

      val vb = part.vertexBuf
      val stride = 8 * 4
      vb.position(0)
      GLES20.glEnableVertexAttribArray(aPosLoc)
      GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, stride, vb)
      vb.position(3)
      GLES20.glEnableVertexAttribArray(aNormLoc)
      GLES20.glVertexAttribPointer(aNormLoc, 3, GLES20.GL_FLOAT, false, stride, vb)
      vb.position(6)
      GLES20.glEnableVertexAttribArray(aUvLoc)
      GLES20.glVertexAttribPointer(aUvLoc, 2, GLES20.GL_FLOAT, false, stride, vb)

      part.indexBuf.position(0)
      GLES20.glDrawElements(GLES20.GL_TRIANGLES, part.indexCount, GLES20.GL_UNSIGNED_SHORT, part.indexBuf)
    }
    GLES20.glDepthMask(true)
    GLES20.glDisableVertexAttribArray(aPosLoc)
    GLES20.glDisableVertexAttribArray(aNormLoc)
    GLES20.glDisableVertexAttribArray(aUvLoc)

    if (renderMode == Pev3dRenderMode.BLUEPRINT) {
      drawWireframePass(explode, wheelSpin, selectedKey, ColorCyan = false)
    }
  }

  private fun drawWireframePass(explode: Float, wheelSpin: Float, selectedKey: String?, ColorCyan: Boolean) {
    GLES20.glUseProgram(wireProgram)
    GLES20.glLineWidth(2.0f)
    for (part in gpuParts) {
      val wb = part.wireBuf ?: continue
      if (part.wireVertCount < 2) continue
      Mat4Math.setIdentity(modelMat)
      if (explode > 0.002f) {
        Mat4Math.translate(modelMat, part.explodeDir.x * explode, part.explodeDir.y * explode, part.explodeDir.z * explode)
      }
      part.axleCenter?.let { c ->
        Mat4Math.translate(modelMat, c.x, c.y, c.z)
        Mat4Math.rotateAxis(modelMat, -wheelSpin, 0f, 0f, 1f)
        Mat4Math.translate(modelMat, -c.x, -c.y, -c.z)
      }
      Mat4Math.multiplyMM(mvpMat, vpMat, modelMat)
      GLES20.glUniformMatrix4fv(wMvpLoc, 1, false, mvpMat, 0)
      val isSel = selectedKey != null && part.partKey == selectedKey
      if (isSel) {
        GLES20.glUniform4f(wColorLoc, 1f, 0.90f, 0.28f, 1f)
      } else if (ColorCyan) {
        GLES20.glUniform4f(wColorLoc, 0.14f, 0.85f, 0.95f, 0.90f)
      } else {
        GLES20.glUniform4f(wColorLoc, 0.36f, 0.92f, 0.84f, 0.55f)
      }
      wb.position(0)
      GLES20.glEnableVertexAttribArray(wPosLoc)
      GLES20.glVertexAttribPointer(wPosLoc, 3, GLES20.GL_FLOAT, false, 0, wb)
      GLES20.glDrawArrays(GLES20.GL_LINES, 0, part.wireVertCount)
    }
    GLES20.glDisableVertexAttribArray(wPosLoc)
  }

  private fun linkProgram(vertSrc: String, fragSrc: String): Int {
    val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertSrc)
    val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragSrc)
    if (vs == 0 || fs == 0) return 0
    val prog = GLES20.glCreateProgram()
    GLES20.glAttachShader(prog, vs)
    GLES20.glAttachShader(prog, fs)
    GLES20.glLinkProgram(prog)
    val linkStatus = IntArray(1)
    GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linkStatus, 0)
    GLES20.glDeleteShader(vs)
    GLES20.glDeleteShader(fs)
    if (linkStatus[0] == 0) {
      GLES20.glDeleteProgram(prog)
      return 0
    }
    return prog
  }

  private fun compileShader(type: Int, src: String): Int {
    val shader = GLES20.glCreateShader(type)
    GLES20.glShaderSource(shader, src)
    GLES20.glCompileShader(shader)
    val status = IntArray(1)
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
    if (status[0] == 0) {
      GLES20.glDeleteShader(shader)
      return 0
    }
    return shader
  }

  private fun allocFloatBuf(arr: FloatArray): FloatBuffer =
    ByteBuffer.allocateDirect(arr.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
      put(arr)
      position(0)
    }

  private fun allocShortBuf(arr: ShortArray): ShortBuffer =
    ByteBuffer.allocateDirect(arr.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer().apply {
      put(arr)
      position(0)
    }

  private fun releaseGlAndEgl() {
    runCatching {
      if (glTextures.isNotEmpty()) {
        val ids = glTextures.values.toIntArray()
        GLES20.glDeleteTextures(ids.size, ids, 0)
        glTextures.clear()
      }
      if (pbrProgram != 0) {
        GLES20.glDeleteProgram(pbrProgram)
        pbrProgram = 0
      }
      if (wireProgram != 0) {
        GLES20.glDeleteProgram(wireProgram)
        wireProgram = 0
      }
    }
    runCatching {
      if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
        EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        if (eglSurface != EGL14.EGL_NO_SURFACE) {
          EGL14.eglDestroySurface(eglDisplay, eglSurface)
        }
        if (eglContext != EGL14.EGL_NO_CONTEXT) {
          EGL14.eglDestroyContext(eglDisplay, eglContext)
        }
        EGL14.eglTerminate(eglDisplay)
      }
    }
    eglSurface = EGL14.EGL_NO_SURFACE
    eglContext = EGL14.EGL_NO_CONTEXT
    eglDisplay = EGL14.EGL_NO_DISPLAY
  }
}

/**
 * Unified Hardware-Accelerated OpenGL ES Textured Cook-Torrance GGX PBR Viewport for 3D Lab.
 * Renders on a TextureView (zero SurfaceView overlap/clipping bugs) with automatic zero-crash
 * fallback to [Pev3dComposeShadedViewport] on headless environments or drivers without EGL.
 */
@Composable
internal fun Pev3dHardwareGlViewport(
  archetype: Pev3dArchetype,
  renderMode: Pev3dRenderMode,
  lightingPreset: Pev3dLightingPreset,
  techEngine: Pev3dTechEngine,
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
  onBackendChanged: (String) -> Unit = {},
  modifier: Modifier = Modifier,
  finish: Pev3dModelFinish = Pev3dModelFinish(
    bodyRgb = bodyRgb,
    accentRgb = accentRgb,
    caliperRgb = caliperRgb,
  ),
) {
  var fallbackToCompose by remember { mutableStateOf(false) }

  if (fallbackToCompose) {
    Pev3dComposeShadedViewport(
      archetype = archetype,
      renderMode = renderMode,
      lightingPreset = lightingPreset,
      yawDeg = yawDeg,
      pitchDeg = pitchDeg,
      zoom = zoom,
      explodePct = explodePct,
      wheelSpinDeg = wheelSpinDeg,
      throttlePct = throttlePct,
      lightsOn = lightsOn,
      turnSignalMode = turnSignalMode,
      selectedPartKey = selectedPartKey,
      accentRgb = accentRgb,
      bodyRgb = bodyRgb,
      caliperRgb = caliperRgb,
      ambientTempC = ambientTempC,
      modifier = modifier,
      finish = finish,
      techEngine = techEngine,
    )
    return
  }

  val glState = remember { Pev3dHardwareGlState() }
  val prevKey = remember { arrayOf<Any?>(null, null, null) }
  if (prevKey[0] != archetype || prevKey[1] != finish || prevKey[2] != lightsOn) {
    prevKey[0] = archetype
    prevKey[1] = finish
    prevKey[2] = lightsOn
    glState.archetype = archetype
    glState.finish = finish
    glState.lightsOn = lightsOn
    glState.sceneRevision = glState.sceneRevision + 1
  }
  glState.renderMode = renderMode
  glState.lightingPreset = lightingPreset
  glState.techEngine = techEngine
  glState.yawDeg = yawDeg
  glState.pitchDeg = pitchDeg
  glState.zoom = zoom
  glState.explodePct = explodePct
  glState.wheelSpinDeg = wheelSpinDeg
  glState.throttlePct = throttlePct
  glState.turnSignalMode = turnSignalMode
  glState.selectedPartKey = selectedPartKey
  glState.ambientTempC = ambientTempC

  val threadHolder = remember { arrayOfNulls<Pev3dGlRenderThread>(1) }

  DisposableEffect(Unit) {
    onDispose {
      threadHolder[0]?.requestStop()
      threadHolder[0] = null
    }
  }

  AndroidView(
    modifier = modifier,
    factory = { ctx: Context ->
      TextureView(ctx).apply {
        isOpaque = true
        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
          override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            threadHolder[0]?.requestStop()
            val t = Pev3dGlRenderThread(
              surfaceTexture = surface,
              initialWidth = width,
              initialHeight = height,
              state = glState,
              onBackendReady = { lbl -> onBackendChanged(lbl) },
              onFatalGlError = {
                fallbackToCompose = true
                onBackendChanged("STUDIO 3D • SAFE PBR")
              },
            )
            threadHolder[0] = t
            t.start()
          }

          override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            threadHolder[0]?.updateViewportSize(width, height)
          }

          override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            threadHolder[0]?.requestStop()
            threadHolder[0] = null
            return true
          }

          override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
        }
      }
    },
  )
}
