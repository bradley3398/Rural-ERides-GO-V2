package com.example.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
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
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal class PevOpenGlState {
  @Volatile var scene: PevBuiltScene? = null
  @Volatile var sceneKey: Int = 0
  @Volatile var renderMode: Pev3dRenderMode = Pev3dRenderMode.SOLID
  @Volatile var lightingPreset: Pev3dLightingPreset = Pev3dLightingPreset.STUDIO
  @Volatile var yawDeg: Float = -28f
  @Volatile var pitchDeg: Float = 16f
  @Volatile var zoom: Float = 1.05f
  @Volatile var explodePct: Float = 0f
  @Volatile var wheelSpinDeg: Float = 0f
  @Volatile var throttlePct: Float = 0.35f
  @Volatile var lightsOn: Boolean = true
  @Volatile var turnSignalMode: Pev3dTurnSignalMode = Pev3dTurnSignalMode.OFF
  @Volatile var selectedPartKey: String? = "battery"
  @Volatile var ambientTempC: Float = 22f
}

private class GlUploadedPart(
  val part: PevSceneMeshPart,
  val vboId: Int,
  val iboId: Int,
  val indexCount: Int,
  val wireBuf: FloatBuffer?,
  val wireVertCount: Int,
)

private class GlMaterialTextures(
  val albedoTexId: Int,
  val normalTexId: Int,
  val mrTexId: Int,
)

/**
 * Native Hardware-Accelerated OpenGL ES Cook-Torrance GGX PBR Engine.
 * Renders full sculpted Pev3dGltfBuilder CAD geometry with 256x256 mipmapped PBR
 * Albedo + Normal + MetallicRoughness maps, Studio Softbox IBL reflections, Clearcoat,
 * ACES Filmic tone mapping, animated LED optics, X-Ray cutaway pass, and FLIR Thermal IR.
 */
private class PevOpenGlRenderer(
  private val state: PevOpenGlState,
  private val onGlBackendReady: (String) -> Unit,
) : GLSurfaceView.Renderer {

  private var pbrProgram = 0
  private var wireProgram = 0
  private var shadowProgram = 0

  // PBR shader uniforms & attributes
  private var uMvpLoc = 0
  private var uModelLoc = 0
  private var uCamPosLoc = 0
  private var uKeyDirLoc = 0
  private var uKeyColLoc = 0
  private var uFillColLoc = 0
  private var uRimColLoc = 0
  private var uBaseRgbLoc = 0
  private var uEmissiveRgbLoc = 0
  private var uMetallicLoc = 0
  private var uRoughnessLoc = 0
  private var uClearcoatLoc = 0
  private var uModeLoc = 0
  private var uHeatLoc = 0
  private var uSelectedLoc = 0
  private var uAlphaLoc = 0
  private var uTimeLoc = 0
  private var uTexAlbedoLoc = 0
  private var uTexNormalLoc = 0
  private var uTexMrLoc = 0
  private var aPosLoc = 0
  private var aNormLoc = 0
  private var aUvLoc = 0

  // Wireframe shader handles
  private var wMvpLoc = 0
  private var wColLoc = 0
  private var wPosLoc = 0

  // Shadow / studio turntable quad handles
  private var sMvpLoc = 0
  private var sLightsOnLoc = 0
  private var sBlueprintLoc = 0
  private var sPosLoc = 0
  private var sUvLoc = 0
  private var shadowBuf: FloatBuffer? = null

  private val matTextures = HashMap<PevPbrMaterialId, GlMaterialTextures>()
  private var uploadedSceneKey = -1
  private val uploadedParts = ArrayList<GlUploadedPart>()

  private val viewMatrix = FloatArray(16)
  private val projMatrix = FloatArray(16)
  private val modelMatrix = FloatArray(16)
  private val mvMatrix = FloatArray(16)
  private val mvpMatrix = FloatArray(16)
  private val startNs = System.nanoTime()

  override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    val glVersion = GLES20.glGetString(GLES20.GL_VERSION).orEmpty()
    val glesTag = if (glVersion.contains("3.")) "OPENGL ES 3.0 • GGX PBR" else "OPENGL ES 2.0 • GGX PBR"
    onGlBackendReady(glesTag)

    GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    GLES20.glEnable(GLES20.GL_BLEND)
    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

    buildPrograms()
    uploadAllPbrTextures()
    buildContactShadowQuad()
    uploadedSceneKey = -1
  }

  private fun buildPrograms() {
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
        vWorldNorm = normalize(mat3(uModel) * aNorm);
        vUv = aUv;
        gl_Position = uMvp * vec4(aPos, 1.0);
      }
      """.trimIndent()

    val fragSrc =
      """
      precision highp float;
      uniform sampler2D uTexAlbedo;
      uniform sampler2D uTexNormal;
      uniform sampler2D uTexMr;
      uniform vec3 uCamPos;
      uniform vec3 uKeyDir;
      uniform vec3 uKeyCol;
      uniform vec3 uFillCol;
      uniform vec3 uRimCol;
      uniform vec3 uBaseRgb;
      uniform vec3 uEmissiveRgb;
      uniform float uMetallic;
      uniform float uRoughness;
      uniform float uClearcoat;
      uniform int uMode;
      uniform float uHeat;
      uniform float uSelected;
      uniform float uAlpha;
      uniform float uTime;
      varying vec3 vWorldPos;
      varying vec3 vWorldNorm;
      varying vec2 vUv;

      const float PI = 3.14159265;

      vec3 srgbToLin(vec3 c) {
        return pow(max(c, vec3(0.0)), vec3(2.2));
      }

      vec3 acesFilmic(vec3 x) {
        return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
      }

      float D_GGX(float NdotH, float rough) {
        float a = rough * rough;
        float a2 = a * a;
        float d = (NdotH * NdotH) * (a2 - 1.0) + 1.0;
        return a2 / max(PI * d * d, 0.0001);
      }

      float G_SchlickGGX(float NdotX, float rough) {
        float r = rough + 1.0;
        float k = (r * r) / 8.0;
        return NdotX / max(NdotX * (1.0 - k) + k, 0.0001);
      }

      vec3 studioEnv(vec3 R, float rough, vec3 rimCol) {
        float skyMix = smoothstep(-0.25, 0.75, R.y);
        vec3 floorCol = vec3(0.035, 0.042, 0.055);
        vec3 horizonCol = mix(vec3(0.11, 0.14, 0.20), rimCol * 0.45, 0.45);
        vec3 zenithCol = vec3(0.24, 0.29, 0.36);
        vec3 env = mix(floorCol, mix(horizonCol, zenithCol, skyMix), smoothstep(-0.45, 0.15, R.y));
        // Overhead photographic softbox strips for crisp metallic & clearcoat reflections
        float strip1 = smoothstep(0.62, 0.94, R.y) * smoothstep(0.25, 0.85, 1.0 - abs(R.z));
        float strip2 = smoothstep(0.32, 0.80, abs(R.x)) * smoothstep(-0.05, 0.35, R.y) * smoothstep(0.55, 0.0, R.y);
        float glossFade = mix(1.0, 0.18, rough);
        env += vec3(1.15, 1.18, 1.24) * strip1 * glossFade;
        env += rimCol * 0.75 * strip2 * glossFade;
        return env;
      }

      vec3 thermalPalette(float t) {
        float x = clamp(t, 0.0, 1.0);
        vec3 cold = vec3(0.06, 0.10, 0.48);
        vec3 mid = vec3(0.86, 0.14, 0.62);
        vec3 warm = vec3(1.0, 0.56, 0.06);
        vec3 hot = vec3(1.0, 0.98, 0.72);
        if (x < 0.34) return mix(cold, mid, x / 0.34);
        if (x < 0.70) return mix(mid, warm, (x - 0.34) / 0.36);
        return mix(warm, hot, (x - 0.70) / 0.30);
      }

      void main() {
        vec3 geomN = normalize(vWorldNorm);
        vec3 V = normalize(uCamPos - vWorldPos);
        float pulse = 0.5 + 0.5 * sin(uTime * 5.0);

        if (uMode == 2) {
          // X-Ray Cutaway Holographic Shader
          float fres = pow(1.0 - max(dot(geomN, V), 0.0), 2.1);
          vec3 xCol = mix(vec3(0.06, 0.52, 0.92), vec3(0.18, 1.0, 0.72), fres);
          if (uSelected > 0.5) xCol = mix(xCol, vec3(1.0, 0.86, 0.25), 0.5 + 0.35 * pulse);
          float alpha = clamp(0.16 + fres * 0.74 + uSelected * 0.18, 0.15, 0.92);
          gl_FragColor = vec4(xCol, alpha * uAlpha);
          return;
        }

        if (uMode == 4) {
          // FLIR Thermal IR Heat-Map Shader
          float fres = pow(1.0 - max(dot(geomN, V), 0.0), 2.0);
          float diff = max(dot(geomN, normalize(uKeyDir)), 0.0);
          float tVal = clamp(uHeat + fres * 0.14 + diff * 0.10, 0.04, 1.0);
          vec3 tCol = thermalPalette(tVal);
          if (uSelected > 0.5) tCol += vec3(0.24 * pulse);
          gl_FragColor = vec4(tCol, 1.0);
          return;
        }

        if (uMode == 5) {
          // Blueprint CAD Engineering Shader
          float NdotV_bp = max(dot(geomN, V), 0.0);
          float fres = pow(1.0 - NdotV_bp, 1.9);
          float diff = max(dot(geomN, normalize(uKeyDir)), 0.0);
          vec3 gx = abs(fract(vWorldPos * 8.0) - 0.5);
          float gridLine = 1.0 - smoothstep(0.45, 0.495, max(gx.x, max(gx.y, gx.z)));
          vec3 bpBase = mix(vec3(0.04, 0.16, 0.40), vec3(0.10, 0.36, 0.70), 0.30 + 0.70 * diff);
          vec3 bpEdge = vec3(0.32, 0.90, 1.0) * fres * 1.15;
          vec3 colBp = bpBase + bpEdge + vec3(0.08, 0.32, 0.52) * gridLine * 0.35;
          if (uSelected > 0.5) colBp = mix(colBp, vec3(0.98, 0.92, 0.35), 0.45 + 0.35 * pulse);
          gl_FragColor = vec4(colBp, uAlpha);
          return;
        }

        // Tangent-free world-space frame for normal map perturbation
        vec3 upVec = abs(geomN.y) < 0.98 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0);
        vec3 T = normalize(cross(upVec, geomN));
        vec3 B = normalize(cross(geomN, T));
        vec3 mapN = texture2D(uTexNormal, vUv).rgb * 2.0 - 1.0;
        vec3 N = normalize(geomN + (T * mapN.x + B * mapN.y) * 0.42);

        vec3 texAlb = srgbToLin(texture2D(uTexAlbedo, vUv).rgb);
        vec3 albedo = texAlb * srgbToLin(uBaseRgb);
        vec3 mr = texture2D(uTexMr, vUv).rgb;
        float metallic = clamp(uMetallic * (0.45 + 0.55 * mr.b), 0.0, 1.0);
        float rough = clamp(uRoughness * (0.55 + 0.45 * mr.g), 0.06, 0.96);
        float clearcoat = uClearcoat;

        if (uMode == 3) {
          // Carbon Gloss Showroom boost
          float weave = 0.80 + 0.20 * step(0.0, sin(vWorldPos.x * 58.0) * sin((vWorldPos.y + vWorldPos.z) * 58.0));
          albedo *= mix(0.75, 0.98, weave);
          rough = clamp(rough * 0.52, 0.06, 0.65);
          clearcoat = max(clearcoat, 0.88);
        }

        float NdotV = max(dot(N, V), 0.001);
        vec3 F0 = mix(vec3(0.04), albedo, metallic);

        // 3-Point Studio Light Accumulation (Key + Fill + Back Rim)
        vec3 L1 = normalize(uKeyDir);
        vec3 L2 = normalize(vec3(-uKeyDir.x * 0.9, 0.35, -uKeyDir.z * 0.6));
        vec3 L3 = normalize(vec3(0.15, 0.45, -1.0));

        vec3 Lo = vec3(0.0);
        for (int i = 0; i < 3; i++) {
          vec3 L = (i == 0) ? L1 : ((i == 1) ? L2 : L3);
          vec3 rad = (i == 0) ? uKeyCol : ((i == 1) ? uFillCol : uRimCol);
          vec3 H = normalize(V + L);
          float NdotL = max(dot(N, L), 0.0);
          float NdotH = max(dot(N, H), 0.0);
          float HdotV = max(dot(H, V), 0.0);
          vec3 F = F0 + (1.0 - F0) * pow(1.0 - HdotV, 5.0);
          float NDF = D_GGX(NdotH, rough);
          float G = G_SchlickGGX(NdotV, rough) * G_SchlickGGX(max(NdotL, 0.001), rough);
          vec3 spec = (NDF * G * F) / max(4.0 * NdotV * max(NdotL, 0.001), 0.001);
          vec3 kD = (vec3(1.0) - F) * (1.0 - metallic);
          vec3 diff = kD * albedo / PI;
          float cc = clearcoat * D_GGX(NdotH, 0.075) * 0.24 * pow(1.0 - HdotV, 3.0);
          Lo += (diff + spec + vec3(cc)) * rad * NdotL;
        }

        // Studio IBL Diffuse + Specular Softbox Reflection
        vec3 R = reflect(-V, N);
        vec3 kS = F0 + (max(vec3(1.0 - rough), F0) - F0) * pow(1.0 - NdotV, 5.0);
        vec3 kD_env = (1.0 - kS) * (1.0 - metallic);
        vec3 irr = mix(vec3(0.07, 0.08, 0.11), vec3(0.24, 0.27, 0.32), 0.5 + 0.5 * N.y);
        vec3 envRefl = studioEnv(R, rough, uRimCol);
        vec3 ccRefl = studioEnv(R, 0.07, uRimCol) * clearcoat * (0.04 + 0.72 * pow(1.0 - NdotV, 4.0));
        vec3 ambient = kD_env * albedo * irr + envRefl * (F0 + kS * 0.45) + ccRefl;

        vec3 color = ambient + Lo + uEmissiveRgb;
        if (uSelected > 0.5) {
          float selRim = pow(1.0 - NdotV, 1.8);
          color += vec3(0.22, 0.62, 1.0) * (0.14 + 0.55 * selRim) * (0.55 + 0.45 * pulse);
        }

        color = acesFilmic(color * 1.12);
        color = pow(color, vec3(1.0 / 2.2));
        gl_FragColor = vec4(color, uAlpha);
      }
      """.trimIndent()

    val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertSrc)
    val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragSrc)
    pbrProgram = GLES20.glCreateProgram().also { p ->
      GLES20.glAttachShader(p, vs)
      GLES20.glAttachShader(p, fs)
      GLES20.glLinkProgram(p)
    }
    uMvpLoc = GLES20.glGetUniformLocation(pbrProgram, "uMvp")
    uModelLoc = GLES20.glGetUniformLocation(pbrProgram, "uModel")
    uCamPosLoc = GLES20.glGetUniformLocation(pbrProgram, "uCamPos")
    uKeyDirLoc = GLES20.glGetUniformLocation(pbrProgram, "uKeyDir")
    uKeyColLoc = GLES20.glGetUniformLocation(pbrProgram, "uKeyCol")
    uFillColLoc = GLES20.glGetUniformLocation(pbrProgram, "uFillCol")
    uRimColLoc = GLES20.glGetUniformLocation(pbrProgram, "uRimCol")
    uBaseRgbLoc = GLES20.glGetUniformLocation(pbrProgram, "uBaseRgb")
    uEmissiveRgbLoc = GLES20.glGetUniformLocation(pbrProgram, "uEmissiveRgb")
    uMetallicLoc = GLES20.glGetUniformLocation(pbrProgram, "uMetallic")
    uRoughnessLoc = GLES20.glGetUniformLocation(pbrProgram, "uRoughness")
    uClearcoatLoc = GLES20.glGetUniformLocation(pbrProgram, "uClearcoat")
    uModeLoc = GLES20.glGetUniformLocation(pbrProgram, "uMode")
    uHeatLoc = GLES20.glGetUniformLocation(pbrProgram, "uHeat")
    uSelectedLoc = GLES20.glGetUniformLocation(pbrProgram, "uSelected")
    uAlphaLoc = GLES20.glGetUniformLocation(pbrProgram, "uAlpha")
    uTimeLoc = GLES20.glGetUniformLocation(pbrProgram, "uTime")
    uTexAlbedoLoc = GLES20.glGetUniformLocation(pbrProgram, "uTexAlbedo")
    uTexNormalLoc = GLES20.glGetUniformLocation(pbrProgram, "uTexNormal")
    uTexMrLoc = GLES20.glGetUniformLocation(pbrProgram, "uTexMr")
    aPosLoc = GLES20.glGetAttribLocation(pbrProgram, "aPos")
    aNormLoc = GLES20.glGetAttribLocation(pbrProgram, "aNorm")
    aUvLoc = GLES20.glGetAttribLocation(pbrProgram, "aUv")

    val wVs = compileShader(
      GLES20.GL_VERTEX_SHADER,
      "uniform mat4 uMvp; attribute vec3 aPos; void main(){ gl_Position = uMvp * vec4(aPos,1.0); }",
    )
    val wFs = compileShader(
      GLES20.GL_FRAGMENT_SHADER,
      "precision mediump float; uniform vec4 uCol; void main(){ gl_FragColor = uCol; }",
    )
    wireProgram = GLES20.glCreateProgram().also { p ->
      GLES20.glAttachShader(p, wVs)
      GLES20.glAttachShader(p, wFs)
      GLES20.glLinkProgram(p)
    }
    wMvpLoc = GLES20.glGetUniformLocation(wireProgram, "uMvp")
    wColLoc = GLES20.glGetUniformLocation(wireProgram, "uCol")
    wPosLoc = GLES20.glGetAttribLocation(wireProgram, "aPos")

    val sVs = compileShader(
      GLES20.GL_VERTEX_SHADER,
      "uniform mat4 uMvp; attribute vec3 aPos; attribute vec2 aUv; varying vec2 vUv; void main(){ vUv=aUv; gl_Position=uMvp*vec4(aPos,1.0); }",
    )
    val sFs = compileShader(
      GLES20.GL_FRAGMENT_SHADER,
      """
      precision mediump float;
      uniform float uLightsOn;
      uniform float uBlueprint;
      varying vec2 vUv;
      void main() {
        vec2 centered = vUv - vec2(0.5);
        vec2 d = centered * vec2(1.45, 2.4);
        float dist = length(d);
        float shadowAlpha = smoothstep(0.92, 0.12, dist) * 0.65;
        float ringDist = length(centered * vec2(1.0, 1.75));
        float outerRing = smoothstep(0.016, 0.0, abs(ringDist - 0.46));
        float innerRing = smoothstep(0.012, 0.0, abs(ringDist - 0.31));
        vec3 ringCol = mix(vec3(0.22, 0.68, 0.95), vec3(0.45, 0.92, 1.0), uBlueprint);
        float beam = 0.0;
        if (uLightsOn > 0.5 && centered.x > 0.06) {
          float cone = smoothstep(0.32, 0.02, abs(centered.y) / max(centered.x * 0.85, 0.06));
          beam = cone * smoothstep(0.49, 0.08, centered.x) * 0.28;
        }
        vec3 col = mix(vec3(0.01, 0.015, 0.025), ringCol, clamp(outerRing * 0.75 + innerRing * 0.5, 0.0, 1.0));
        col = mix(col, vec3(0.86, 0.95, 1.0), beam);
        float a = clamp(max(shadowAlpha, max((outerRing * 0.42 + innerRing * 0.25), beam)), 0.0, 0.78);
        gl_FragColor = vec4(col, a);
      }
      """.trimIndent(),
    )
    shadowProgram = GLES20.glCreateProgram().also { p ->
      GLES20.glAttachShader(p, sVs)
      GLES20.glAttachShader(p, sFs)
      GLES20.glLinkProgram(p)
    }
    sMvpLoc = GLES20.glGetUniformLocation(shadowProgram, "uMvp")
    sLightsOnLoc = GLES20.glGetUniformLocation(shadowProgram, "uLightsOn")
    sBlueprintLoc = GLES20.glGetUniformLocation(shadowProgram, "uBlueprint")
    sPosLoc = GLES20.glGetAttribLocation(shadowProgram, "aPos")
    sUvLoc = GLES20.glGetAttribLocation(shadowProgram, "aUv")
  }

  private fun uploadAllPbrTextures() {
    matTextures.clear()
    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
    fun uploadPng(bytes: ByteArray): Int {
      val texIds = IntArray(1)
      GLES20.glGenTextures(1, texIds, 0)
      val tid = texIds[0]
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tid)
      GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
      GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
      GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
      GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
      val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
      if (bmp != null) {
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        bmp.recycle()
      }
      return tid
    }
    for (id in PevPbrMaterialId.entries) {
      val b = Pev3dPbrTextures.getBundle(id)
      matTextures[id] = GlMaterialTextures(
        albedoTexId = uploadPng(b.albedoPng),
        normalTexId = uploadPng(b.normalPng),
        mrTexId = uploadPng(b.metallicRoughnessPng),
      )
    }
  }

  private fun buildContactShadowQuad() {
    val verts = floatArrayOf(
      -1.28f, -0.774f, 0.72f, 0f, 0f,
      1.28f, -0.774f, 0.72f, 1f, 0f,
      1.28f, -0.774f, -0.72f, 1f, 1f,
      -1.28f, -0.774f, 0.72f, 0f, 0f,
      1.28f, -0.774f, -0.72f, 1f, 1f,
      -1.28f, -0.774f, -0.72f, 0f, 1f,
    )
    shadowBuf = ByteBuffer.allocateDirect(verts.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
      put(verts)
      position(0)
    }
  }

  private fun syncSceneIfNeeded() {
    val scene = state.scene ?: return
    val targetKey = state.sceneKey
    if (uploadedSceneKey == targetKey && uploadedParts.isNotEmpty()) return

    if (uploadedParts.isNotEmpty()) {
      val oldBufs = IntArray(uploadedParts.size * 2)
      uploadedParts.forEachIndexed { idx, up ->
        oldBufs[idx * 2] = up.vboId
        oldBufs[idx * 2 + 1] = up.iboId
      }
      GLES20.glDeleteBuffers(oldBufs.size, oldBufs, 0)
      uploadedParts.clear()
    }

    for (part in scene.parts) {
      val bufs = IntArray(2)
      GLES20.glGenBuffers(2, bufs, 0)
      val vbo = bufs[0]
      val ibo = bufs[1]

      val vb = ByteBuffer.allocateDirect(part.vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        put(part.vertices)
        position(0)
      }
      GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
      GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, part.vertices.size * 4, vb, GLES20.GL_STATIC_DRAW)

      val ib = ByteBuffer.allocateDirect(part.indices.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer().apply {
        put(part.indices)
        position(0)
      }
      GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo)
      GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, part.indices.size * 2, ib, GLES20.GL_STATIC_DRAW)

      GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
      GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)

      val wireBuf = if (part.wireframeLines.size >= 6) {
        ByteBuffer.allocateDirect(part.wireframeLines.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
          put(part.wireframeLines)
          position(0)
        }
      } else null

      uploadedParts += GlUploadedPart(
        part = part,
        vboId = vbo,
        iboId = ibo,
        indexCount = part.indices.size,
        wireBuf = wireBuf,
        wireVertCount = part.wireframeLines.size / 3,
      )
    }
    uploadedSceneKey = targetKey
  }

  override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    val w = width.coerceAtLeast(1)
    val h = height.coerceAtLeast(1)
    GLES20.glViewport(0, 0, w, h)
    Matrix.perspectiveM(projMatrix, 0, 38f, w.toFloat() / h.toFloat(), 0.25f, 40f)
  }

  override fun onDrawFrame(gl: GL10?) {
    if (state.renderMode == Pev3dRenderMode.BLUEPRINT) {
      GLES20.glClearColor(0.028f, 0.105f, 0.245f, 1f)
    } else {
      when (state.lightingPreset) {
        Pev3dLightingPreset.STUDIO -> GLES20.glClearColor(0.055f, 0.072f, 0.102f, 1f)
        Pev3dLightingPreset.SUNSET -> GLES20.glClearColor(0.115f, 0.068f, 0.068f, 1f)
        Pev3dLightingPreset.NEON -> GLES20.glClearColor(0.032f, 0.042f, 0.092f, 1f)
      }
    }
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
    if (pbrProgram == 0) return

    syncSceneIfNeeded()
    if (uploadedParts.isEmpty()) return

    val dist = (4.65f / state.zoom.coerceIn(0.6f, 2.4f)).coerceIn(1.8f, 8.5f)
    val yr = (state.yawDeg * PI / 180.0).toFloat()
    val pr = (state.pitchDeg * PI / 180.0).toFloat()
    val camX = dist * cos(pr) * sin(yr)
    val camY = dist * sin(pr) + 0.08f
    val camZ = dist * cos(pr) * cos(yr)
    Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, 0f, -0.05f, 0f, 0f, 1f, 0f)

    val timeSec = ((System.nanoTime() - startNs) / 1_000_000_000.0).toFloat()
    val flashOn = sin(timeSec * 8.5f) > 0f
    val leftActive = (state.turnSignalMode == Pev3dTurnSignalMode.LEFT || state.turnSignalMode == Pev3dTurnSignalMode.HAZARD) && flashOn
    val rightActive = (state.turnSignalMode == Pev3dTurnSignalMode.RIGHT || state.turnSignalMode == Pev3dTurnSignalMode.HAZARD) && flashOn

    val isWire = state.renderMode == Pev3dRenderMode.WIREFRAME
    val isXray = state.renderMode == Pev3dRenderMode.XRAY
    val isBlueprint = state.renderMode == Pev3dRenderMode.BLUEPRINT

    // Pass 1: Draw Wireframe mode if active
    if (isWire && wireProgram != 0) {
      drawWireframeScene(timeSec, alphaScale = 0.85f)
      return
    }

    // PBR shader setup
    GLES20.glUseProgram(pbrProgram)
    when (state.lightingPreset) {
      Pev3dLightingPreset.STUDIO -> {
        GLES20.glUniform3f(uKeyDirLoc, 0.45f, 0.85f, 0.35f)
        GLES20.glUniform3f(uKeyColLoc, 2.7f, 2.68f, 2.65f)
        GLES20.glUniform3f(uFillColLoc, 0.95f, 1.05f, 1.25f)
        GLES20.glUniform3f(uRimColLoc, 0.55f, 0.78f, 1.15f)
      }
      Pev3dLightingPreset.SUNSET -> {
        GLES20.glUniform3f(uKeyDirLoc, -0.78f, 0.42f, 0.45f)
        GLES20.glUniform3f(uKeyColLoc, 3.1f, 1.85f, 1.05f)
        GLES20.glUniform3f(uFillColLoc, 0.65f, 0.60f, 1.05f)
        GLES20.glUniform3f(uRimColLoc, 1.25f, 0.58f, 0.38f)
      }
      Pev3dLightingPreset.NEON -> {
        GLES20.glUniform3f(uKeyDirLoc, 0.28f, 0.68f, -0.68f)
        GLES20.glUniform3f(uKeyColLoc, 1.35f, 2.45f, 3.0f)
        GLES20.glUniform3f(uFillColLoc, 2.1f, 0.55f, 1.65f)
        GLES20.glUniform3f(uRimColLoc, 0.35f, 1.65f, 2.2f)
      }
    }
    GLES20.glUniform3f(uCamPosLoc, camX, camY, camZ)
    GLES20.glUniform1i(uModeLoc, state.renderMode.shaderId)
    GLES20.glUniform1f(uTimeLoc, timeSec)
    GLES20.glUniform1i(uTexAlbedoLoc, 0)
    GLES20.glUniform1i(uTexNormalLoc, 1)
    GLES20.glUniform1i(uTexMrLoc, 2)

    val ambientBoost = ((state.ambientTempC - 15f) / 40f).coerceIn(0f, 0.25f)
    val throttleHeat = state.throttlePct.coerceIn(0f, 1f) * 0.68f

    // Solid opaque pass first
    GLES20.glDepthMask(true)
    for (up in uploadedParts) {
      if (isXray && up.part.isShell) continue
      drawPbrPart(up, leftActive, rightActive, throttleHeat, ambientBoost, alpha = 1f)
    }

    // Contact Ground Shadow + Studio Turntable Rings underneath the vehicle
    drawContactShadow(isBlueprint = isBlueprint)

    // Blueprint CAD sharp technical wireframe overlay
    if (isBlueprint && wireProgram != 0) {
      drawWireframeScene(timeSec, alphaScale = 0.52f)
    }

    // Translucent X-Ray shell pass last
    if (isXray) {
      GLES20.glDepthMask(false)
      for (up in uploadedParts) {
        if (!up.part.isShell) continue
        drawPbrPart(up, leftActive, rightActive, throttleHeat, ambientBoost, alpha = 0.72f)
      }
      GLES20.glDepthMask(true)
    }
  }

  private fun computeModelMatrixForPart(part: PevSceneMeshPart) {
    Matrix.setIdentityM(modelMatrix, 0)
    if (part.partKey != "floor" && state.explodePct > 0.002f) {
      Matrix.translateM(
        modelMatrix,
        0,
        part.explodeDir.x * state.explodePct,
        part.explodeDir.y * state.explodePct,
        part.explodeDir.z * state.explodePct,
      )
    }
    part.axleCenter?.let { c ->
      Matrix.translateM(modelMatrix, 0, c.x, c.y, c.z)
      Matrix.rotateM(modelMatrix, 0, -state.wheelSpinDeg, 0f, 0f, 1f)
      Matrix.translateM(modelMatrix, 0, -c.x, -c.y, -c.z)
    }
    Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0)
    Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvMatrix, 0)
  }

  private fun drawPbrPart(
    up: GlUploadedPart,
    leftSignalActive: Boolean,
    rightSignalActive: Boolean,
    throttleHeat: Float,
    ambientBoost: Float,
    alpha: Float,
  ) {
    val part = up.part
    computeModelMatrixForPart(part)
    GLES20.glUniformMatrix4fv(uMvpLoc, 1, false, mvpMatrix, 0)
    GLES20.glUniformMatrix4fv(uModelLoc, 1, false, modelMatrix, 0)

    val tex = matTextures[part.texId] ?: matTextures[PevPbrMaterialId.BRUSHED_METAL]
    if (tex != null) {
      GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex.albedoTexId)
      GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex.normalTexId)
      GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex.mrTexId)
    }

    var emR = part.emissiveRgb[0]
    var emG = part.emissiveRgb[1]
    var emB = part.emissiveRgb[2]
    if (part.nodeName.contains("signal_left") && leftSignalActive) {
      emR = 2.6f; emG = 1.45f; emB = 0.08f
    } else if (part.nodeName.contains("signal_right") && rightSignalActive) {
      emR = 2.6f; emG = 1.45f; emB = 0.08f
    } else if (part.nodeName.contains("headlight")) {
      if (state.lightsOn) { emR = 2.4f; emG = 2.5f; emB = 2.7f } else { emR = 0.03f; emG = 0.03f; emB = 0.04f }
    } else if (part.nodeName.contains("tail")) {
      if (state.lightsOn) { emR = 2.2f; emG = 0.22f; emB = 0.18f } else { emR = 0.04f; emG = 0.01f; emB = 0.01f }
    }

    val partHeatFactor = when (part.partKey) {
      "motor" -> 0.92f
      "controller" -> 0.88f
      "battery" -> 0.72f
      "brakes" -> 0.65f
      else -> 0.22f
    }
    val heat = (0.12f + partHeatFactor * throttleHeat + ambientBoost).coerceIn(0.05f, 1f)
    val selected = if (part.partKey != "floor" && state.selectedPartKey == part.partKey) 1f else 0f

    GLES20.glUniform3f(uBaseRgbLoc, part.baseRgb[0], part.baseRgb[1], part.baseRgb[2])
    GLES20.glUniform3f(uEmissiveRgbLoc, emR, emG, emB)
    GLES20.glUniform1f(uMetallicLoc, part.metallic)
    GLES20.glUniform1f(uRoughnessLoc, part.roughness)
    GLES20.glUniform1f(uClearcoatLoc, part.clearcoat)
    GLES20.glUniform1f(uHeatLoc, heat)
    GLES20.glUniform1f(uSelectedLoc, selected)
    GLES20.glUniform1f(uAlphaLoc, alpha)

    val stride = 8 * 4
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, up.vboId)
    GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, up.iboId)
    GLES20.glEnableVertexAttribArray(aPosLoc)
    GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, stride, 0)
    GLES20.glEnableVertexAttribArray(aNormLoc)
    GLES20.glVertexAttribPointer(aNormLoc, 3, GLES20.GL_FLOAT, false, stride, 3 * 4)
    GLES20.glEnableVertexAttribArray(aUvLoc)
    GLES20.glVertexAttribPointer(aUvLoc, 2, GLES20.GL_FLOAT, false, stride, 6 * 4)

    GLES20.glDrawElements(GLES20.GL_TRIANGLES, up.indexCount, GLES20.GL_UNSIGNED_SHORT, 0)

    GLES20.glDisableVertexAttribArray(aPosLoc)
    GLES20.glDisableVertexAttribArray(aNormLoc)
    GLES20.glDisableVertexAttribArray(aUvLoc)
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
  }

  private fun drawContactShadow(isBlueprint: Boolean) {
    val sb = shadowBuf ?: return
    if (shadowProgram == 0) return
    GLES20.glUseProgram(shadowProgram)
    GLES20.glDepthMask(false)
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0)
    Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvMatrix, 0)
    GLES20.glUniformMatrix4fv(sMvpLoc, 1, false, mvpMatrix, 0)
    val showLightPool = state.lightsOn && state.renderMode != Pev3dRenderMode.THERMAL_IR && !isBlueprint
    GLES20.glUniform1f(sLightsOnLoc, if (showLightPool) 1f else 0f)
    GLES20.glUniform1f(sBlueprintLoc, if (isBlueprint) 1f else 0f)
    val stride = 5 * 4
    sb.position(0)
    GLES20.glEnableVertexAttribArray(sPosLoc)
    GLES20.glVertexAttribPointer(sPosLoc, 3, GLES20.GL_FLOAT, false, stride, sb)
    sb.position(3)
    GLES20.glEnableVertexAttribArray(sUvLoc)
    GLES20.glVertexAttribPointer(sUvLoc, 2, GLES20.GL_FLOAT, false, stride, sb)
    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
    GLES20.glDisableVertexAttribArray(sPosLoc)
    GLES20.glDisableVertexAttribArray(sUvLoc)
    GLES20.glDepthMask(true)
    GLES20.glUseProgram(pbrProgram)
  }

  private fun drawWireframeScene(timeSec: Float, alphaScale: Float = 0.85f) {
    val pulse = 0.5f + 0.5f * sin(timeSec * 5f)
    GLES20.glUseProgram(wireProgram)
    for (up in uploadedParts) {
      val wb = up.wireBuf ?: continue
      if (up.wireVertCount < 2) continue
      computeModelMatrixForPart(up.part)
      GLES20.glUniformMatrix4fv(wMvpLoc, 1, false, mvpMatrix, 0)
      val isSel = up.part.partKey != "floor" && state.selectedPartKey == up.part.partKey
      if (isSel) {
        GLES20.glUniform4f(wColLoc, 0.85f + 0.15f * pulse, 0.96f, 1.0f, 0.98f)
      } else {
        GLES20.glUniform4f(wColLoc, 0.24f, 0.88f, 1.0f, alphaScale)
      }
      wb.position(0)
      GLES20.glEnableVertexAttribArray(wPosLoc)
      GLES20.glVertexAttribPointer(wPosLoc, 3, GLES20.GL_FLOAT, false, 3 * 4, wb)
      GLES20.glDrawArrays(GLES20.GL_LINES, 0, up.wireVertCount)
      GLES20.glDisableVertexAttribArray(wPosLoc)
    }
  }

  private fun compileShader(type: Int, src: String): Int {
    val shader = GLES20.glCreateShader(type)
    GLES20.glShaderSource(shader, src)
    GLES20.glCompileShader(shader)
    return shader
  }
}

@Composable
internal fun Pev3dOpenGlViewport(
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
  onBackendChanged: (String) -> Unit,
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
  val sceneKey = remember(builtScene) { System.identityHashCode(builtScene) }

  val state = remember { PevOpenGlState() }
  state.scene = builtScene
  state.sceneKey = sceneKey
  state.renderMode = renderMode
  state.lightingPreset = lightingPreset
  state.yawDeg = yawDeg
  state.pitchDeg = pitchDeg
  state.zoom = zoom
  state.explodePct = explodePct
  state.wheelSpinDeg = wheelSpinDeg
  state.throttlePct = throttlePct
  state.lightsOn = lightsOn
  state.turnSignalMode = turnSignalMode
  state.selectedPartKey = selectedPartKey
  state.ambientTempC = ambientTempC

  val renderer = remember { PevOpenGlRenderer(state, onBackendChanged) }
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
