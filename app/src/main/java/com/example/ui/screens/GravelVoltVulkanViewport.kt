package com.example.ui.screens

import android.content.Context
import android.content.res.AssetManager
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.filament.Camera
import com.google.android.filament.ColorGrading
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.Filament
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.Skybox
import com.google.android.filament.SwapChain
import com.google.android.filament.Texture
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import com.google.android.filament.utils.KTX1Loader
import com.google.android.filament.utils.Utils
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

internal data class GameEntityRenderItem(
  val lane: Int,
  val z: Float, // 0 = horizon, 1 = player
  val kindCode: Int, // 0=HAY, 1=CREEK, 2=TRACTOR, 3=VOLT_CELL, 4=SOLAR_ORB
)

internal class GravelVoltVulkanState {
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
}

private object GameFilamentBoot {
  @Volatile private var ready = false
  fun ensureInit() {
    if (ready) return
    synchronized(this) {
      if (ready) return
      Filament.init()
      Gltfio.init()
      Utils.init()
      ready = true
    }
  }
}

/**
 * Pure native Android Vulkan 3D PBR game renderer for Gravel Volt Runner
 * powered by Google Filament 1.81.0 (Engine.Backend.VULKAN, ACES Filmic, 4x MSAA, FXAA, SSAO, Bloom, PCF Shadows).
 * Loads the player's real .glb PEV rig from app/src/main/assets/models/ and renders a real 3D twilight wet backroad.
 */
private class GravelVoltVulkanHost(
  private val appContext: Context,
  private val state: GravelVoltVulkanState,
) {
  private var engine: Engine? = null
  private var renderer: Renderer? = null
  private var scene: Scene? = null
  private var view: View? = null
  private var camera: Camera? = null
  private var cameraEntity: Int = 0
  private var sunLightEntity: Int = 0
  private var skyRimLightEntity: Int = 0
  private var headlightSpotEntity: Int = 0
  private var taillightPointEntity: Int = 0
  private var boostPointEntity: Int = 0

  private var iblTexture: Texture? = null
  private var skyboxTexture: Texture? = null
  private var indirectLight: IndirectLight? = null
  private var skybox: Skybox? = null
  private var colorGrading: ColorGrading? = null
  private var swapChain: SwapChain? = null
  private var envSource: String = "Procedural Twilight IBL"

  private var ubershader: UbershaderProvider? = null
  private var assetLoader: AssetLoader? = null
  private var resourceLoader: ResourceLoader? = null

  private var roadAsset: FilamentAsset? = null
  private var playerAsset: FilamentAsset? = null
  private var loadedVehicleKey: String = ""
  private var playerBaseMatrix = FloatArray(16).also { Mat4Math.setIdentity(it) }
  private var playerWheelEntities: List<Pair<Int, FloatArray>> = emptyList()

  // Pooled 3D Vulkan PBR world prop assets (road stripes, intersections, collectibles, hazards)
  private val stripeAssets = ArrayList<FilamentAsset>()
  private val lampAssets = ArrayList<FilamentAsset>()
  private val crossRoadAssets = ArrayList<FilamentAsset>()
  private val voltCellPool = ArrayList<FilamentAsset>()
  private val solarOrbPool = ArrayList<FilamentAsset>()
  private val hayBalePool = ArrayList<FilamentAsset>()
  private val creekPool = ArrayList<FilamentAsset>()
  private val tractorPool = ArrayList<FilamentAsset>()

  private var uiHelper: UiHelper? = null
  private var started = false
  private var destroyed = false
  private var startNs = System.nanoTime()

  private fun toDirectBuffer(bytes: ByteArray): ByteBuffer =
    ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).apply {
      put(bytes)
      flip()
    }

  private val choreographer = Choreographer.getInstance()
  private val frameCallback = object : Choreographer.FrameCallback {
    override fun doFrame(frameTimeNanos: Long) {
      if (destroyed) return
      if (started) {
        choreographer.postFrameCallback(this)
        try {
          renderFrame(frameTimeNanos)
        } catch (t: Throwable) {
          state.onAssetStateChanged?.invoke(
            Pev3dAssetLoadState.VulkanUnavailable(t.message ?: "Vulkan 3D render error")
          )
          destroy()
        }
      }
    }
  }

  fun attach(surfaceView: SurfaceView) {
    try {
      GameFilamentBoot.ensureInit()
      val eng = Engine.Builder().backend(Engine.Backend.VULKAN).build()
      engine = eng
      renderer = eng.createRenderer()
      scene = eng.createScene()
      view = eng.createView()

      val em = EntityManager.get()
      cameraEntity = em.create()
      camera = eng.createCamera(cameraEntity)

      ubershader = UbershaderProvider(eng)
      assetLoader = AssetLoader(eng, ubershader!!, em)
      resourceLoader = ResourceLoader(eng)

      setupLightingAndPostProcessing(eng, scene!!, view!!)
      build3dWorldAndPools(eng, scene!!)

      view!!.scene = scene
      view!!.camera = camera

      val helper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
      helper.renderCallback = object : UiHelper.RendererCallback {
        override fun onNativeWindowChanged(surface: Surface) {
          if (destroyed || !surface.isValid) return
          try {
            swapChain?.let { eng.destroySwapChain(it) }
            swapChain = eng.createSwapChain(surface)
          } catch (t: Throwable) {
            state.onAssetStateChanged?.invoke(
              Pev3dAssetLoadState.VulkanUnavailable(t.message ?: "Vulkan surface swapchain error")
            )
            destroy()
          }
        }

        override fun onDetachedFromSurface() {
          if (destroyed) return
          runCatching {
            swapChain?.let { eng.destroySwapChain(it); swapChain = null }
          }
        }

        override fun onResized(width: Int, height: Int) {
          if (destroyed) return
          runCatching {
            val w = width.coerceAtLeast(1)
            val h = height.coerceAtLeast(1)
            view?.viewport = Viewport(0, 0, w, h)
            camera?.setProjection(52.0, w.toDouble() / h.toDouble(), 0.25, 140.0, Camera.Fov.VERTICAL)
          }
        }
      }
      helper.attachTo(surfaceView)
      uiHelper = helper
      resume()
    } catch (t: Throwable) {
      state.onAssetStateChanged?.invoke(
        Pev3dAssetLoadState.VulkanUnavailable(t.message ?: "Native Vulkan 1.1 GPU required")
      )
      destroy()
    }
  }

  private fun readAssetBytes(assets: AssetManager, path: String): ByteBuffer? {
    return try {
      assets.open(path).use { input ->
        val bytes = input.readBytes()
        ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).apply {
          put(bytes)
          flip()
        }
      }
    } catch (_: Throwable) {
      null
    }
  }

  private fun setupLightingAndPostProcessing(eng: Engine, scn: Scene, vw: View) {
    val em = EntityManager.get()
    val shadowCfg = LightManager.ShadowOptions().apply {
      mapSize = 1024
      shadowCascades = 1
      shadowBulbRadius = 0.05f
    }
    sunLightEntity = em.create()
    LightManager.Builder(LightManager.Type.DIRECTIONAL)
      .color(1.0f, 0.72f, 0.42f)
      .intensity(105_000f)
      .direction(-0.22f, -0.52f, 0.82f)
      .castShadows(true)
      .shadowOptions(shadowCfg)
      .build(eng, sunLightEntity)
    scn.addEntity(sunLightEntity)

    skyRimLightEntity = em.create()
    LightManager.Builder(LightManager.Type.DIRECTIONAL)
      .color(0.45f, 0.66f, 0.96f)
      .intensity(36_000f)
      .direction(0.35f, -0.35f, -0.86f)
      .castShadows(false)
      .build(eng, skyRimLightEntity)
    scn.addEntity(skyRimLightEntity)

    // Physical Player Headlight Spot Beam + Taillight + Boost Point Light
    headlightSpotEntity = em.create()
    LightManager.Builder(LightManager.Type.SPOT)
      .color(0.95f, 0.98f, 1.0f)
      .intensity(240_000f)
      .position(0f, 1.0f, -0.5f)
      .direction(0f, -0.18f, -0.98f)
      .falloff(38f)
      .spotLightCone(0.28f, 0.72f)
      .castShadows(false)
      .build(eng, headlightSpotEntity)
    scn.addEntity(headlightSpotEntity)

    taillightPointEntity = em.create()
    LightManager.Builder(LightManager.Type.POINT)
      .color(1.0f, 0.14f, 0.12f)
      .intensity(42_000f)
      .position(0f, 0.75f, 1.2f)
      .falloff(6f)
      .build(eng, taillightPointEntity)
    scn.addEntity(taillightPointEntity)

    boostPointEntity = em.create()
    LightManager.Builder(LightManager.Type.POINT)
      .color(0.18f, 0.86f, 1.0f)
      .intensity(0f)
      .position(0f, 0.65f, 0.5f)
      .falloff(10f)
      .build(eng, boostPointEntity)
    scn.addEntity(boostPointEntity)

    // Load real .ktx HDRI from assets/envs/ if present; otherwise build twilight HDRI cubemap + skybox
    val assets = appContext.assets
    val ktxIbl = readAssetBytes(assets, "envs/game_ibl.ktx")
      ?: readAssetBytes(assets, "envs/sunset_ibl.ktx")
      ?: readAssetBytes(assets, "envs/studio_ibl.ktx")
    val ktxSky = readAssetBytes(assets, "envs/game_skybox.ktx")
      ?: readAssetBytes(assets, "envs/sunset_skybox.ktx")
      ?: readAssetBytes(assets, "envs/studio_skybox.ktx")

    val loadedIbl = ktxIbl?.let { buf ->
      try {
        KTX1Loader.createIndirectLight(eng, buf).also { il ->
          il.intensity = 34_000f
          indirectLight = il
          scn.indirectLight = il
          envSource = "Twilight HDRI (.ktx)"
        }
      } catch (_: Throwable) {
        null
      }
    }
    if (loadedIbl == null) {
      val sh = floatArrayOf(
        0.34f, 0.28f, 0.30f,
        0.14f, 0.10f, 0.12f,
        0.18f, 0.16f, 0.20f,
        -0.04f, -0.03f, -0.02f,
        0.05f, 0.04f, 0.03f,
        0.04f, 0.03f, 0.04f,
        0.09f, 0.06f, 0.04f,
        -0.03f, -0.02f, -0.01f,
        0.05f, 0.04f, 0.03f,
      )
      val cubemap = runCatching { buildTwilightCubemap(eng, 32, mipmapped = true) }.getOrNull()
      iblTexture = cubemap
      indirectLight = runCatching {
        val builder = IndirectLight.Builder().irradiance(3, sh).intensity(34_000f)
        if (cubemap != null) builder.reflections(cubemap)
        builder.build(eng)
      }.getOrNull()
      scn.indirectLight = indirectLight
      envSource = if (cubemap != null) "Procedural Twilight IBL" else "Twilight SH IBL"
    }

    val loadedSky = ktxSky?.let { buf ->
      try {
        KTX1Loader.createSkybox(eng, buf).also { sb ->
          skybox = sb
          scn.skybox = sb
        }
      } catch (_: Throwable) {
        null
      }
    }
    if (loadedSky == null) {
      val skyTex = runCatching { buildTwilightCubemap(eng, 32, mipmapped = false) }.getOrNull()
      skyboxTexture = skyTex
      skybox = runCatching {
        if (skyTex != null) Skybox.Builder().environment(skyTex).build(eng)
        else Skybox.Builder().color(0.06f, 0.07f, 0.14f, 1f).build(eng)
      }.getOrNull()
      scn.skybox = skybox
    }

    // Mobile-safe Post Processing Pipeline on Filament 1.81.0
    runCatching {
      colorGrading = ColorGrading.Builder()
        .toneMapping(ColorGrading.ToneMapping.ACES)
        .build(eng)
      vw.colorGrading = colorGrading
    }
    runCatching {
      vw.multiSampleAntiAliasingOptions = View.MultiSampleAntiAliasingOptions().apply {
        enabled = true
        sampleCount = 2
      }
    }
    runCatching { vw.antiAliasing = View.AntiAliasing.FXAA }
    runCatching { vw.dithering = View.Dithering.TEMPORAL }
    runCatching {
      vw.setShadowingEnabled(true)
      vw.setShadowType(View.ShadowType.PCF)
      vw.setDynamicLightingOptions(0.25f, 120.0f)
    }
    runCatching {
      vw.bloomOptions = View.BloomOptions().apply {
        enabled = true
        strength = 0.18f
        levels = 4
        threshold = true
      }
    }
    runCatching {
      vw.ambientOcclusionOptions = View.AmbientOcclusionOptions().apply {
        enabled = true
        radius = 0.32f
        intensity = 1.15f
        quality = View.QualityLevel.MEDIUM
      }
    }
    runCatching {
      vw.vignetteOptions = View.VignetteOptions().apply {
        enabled = true
        midPoint = 0.56f
        roundness = 0.70f
        feather = 0.40f
      }
    }
  }

  private fun buildTwilightCubemap(eng: Engine, baseSize: Int, mipmapped: Boolean): Texture {
    var levels = 1
    if (mipmapped) {
      var s = baseSize
      while (s > 1) {
        s = s shr 1
        levels++
      }
    }
    val tex = Texture.Builder()
      .width(baseSize)
      .height(baseSize)
      .levels(levels)
      .sampler(Texture.Sampler.SAMPLER_CUBEMAP)
      .format(Texture.InternalFormat.RGBA8)
      .build(eng)

    for (level in 0 until levels) {
      val size = (baseSize shr level).coerceAtLeast(1)
      val blur = if (levels > 1) level / (levels - 1f) else 0f
      for (face in 0 until 6) {
        val buf = ByteBuffer.allocateDirect(size * size * 4).order(ByteOrder.nativeOrder())
        for (y in 0 until size) {
          val v = 1f - 2f * ((y + 0.5f) / size)
          for (x in 0 until size) {
            val u = 2f * ((x + 0.5f) / size) - 1f
            val (dx, dy, dz) = when (face) {
              0 -> Triple(1f, v, -u)
              1 -> Triple(-1f, v, u)
              2 -> Triple(u, 1f, -v)
              3 -> Triple(u, -1f, v)
              4 -> Triple(u, v, 1f)
              else -> Triple(-u, v, -1f)
            }
            val inv = 1f / sqrt(dx * dx + dy * dy + dz * dz)
            val ny = dy * inv
            val nz = dz * inv
            val horizon = (1f - abs(ny - 0.04f) * 3.0f).coerceIn(0f, 1f)
            val sunGlow = ((-nz).coerceIn(0f, 1f).pow(3f)) * horizon
            var r = 0.05f + 0.16f * horizon + 0.65f * sunGlow + 0.05f * blur
            var g = 0.06f + 0.09f * horizon + 0.34f * sunGlow + 0.04f * blur
            var b = 0.14f + 0.10f * horizon + 0.12f * sunGlow + 0.06f * blur
            if (ny < -0.05f) {
              r *= 0.25f; g *= 0.28f; b *= 0.32f
            }
            buf.put((r.coerceIn(0f, 1f) * 255f).toInt().toByte())
            buf.put((g.coerceIn(0f, 1f) * 255f).toInt().toByte())
            buf.put((b.coerceIn(0f, 1f) * 255f).toInt().toByte())
            buf.put(255.toByte())
          }
        }
        buf.rewind()
        tex.setImage(
          eng, level, 0, 0, face, size, size, 1,
          Texture.PixelBufferDescriptor(buf, Texture.Format.RGBA, Texture.Type.UBYTE),
        )
      }
    }
    return tex
  }

  private fun createPrimitiveGlb(
    positions: FloatArray,
    normals: FloatArray,
    indices: ShortArray,
    baseColor: FloatArray,
    metallic: Float,
    roughness: Float,
    emissive: FloatArray = floatArrayOf(0f, 0f, 0f),
  ): ByteArray {
    var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var minZ = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
    var i = 0
    while (i + 2 < positions.size) {
      val x = positions[i]; val y = positions[i + 1]; val z = positions[i + 2]
      if (x < minX) minX = x; if (x > maxX) maxX = x
      if (y < minY) minY = y; if (y > maxY) maxY = y
      if (z < minZ) minZ = z; if (z > maxZ) maxZ = z
      i += 3
    }
    val bin = ByteArrayOutputStream(4096)
    fun pad4() { while (bin.size() % 4 != 0) bin.write(0) }
    fun putFloats(arr: FloatArray): Pair<Int, Int> {
      pad4()
      val off = bin.size()
      val bb = ByteBuffer.allocate(arr.size * 4).order(ByteOrder.LITTLE_ENDIAN)
      arr.forEach { bb.putFloat(it) }
      bin.write(bb.array())
      return off to arr.size * 4
    }
    fun putShorts(arr: ShortArray): Pair<Int, Int> {
      pad4()
      val off = bin.size()
      val bb = ByteBuffer.allocate(arr.size * 2).order(ByteOrder.LITTLE_ENDIAN)
      arr.forEach { bb.putShort(it) }
      bin.write(bb.array())
      return off to arr.size * 2
    }
    val (posOff, posLen) = putFloats(positions)
    val (nrmOff, nrmLen) = putFloats(normals)
    val (idxOff, idxLen) = putShorts(indices)
    pad4()
    val binBytes = bin.toByteArray()
    val vCount = positions.size / 3
    val json = """
      {
        "asset":{"version":"2.0"},
        "scene":0,
        "scenes":[{"nodes":[0]}],
        "nodes":[{"mesh":0}],
        "meshes":[{"primitives":[{"attributes":{"POSITION":0,"NORMAL":1},"indices":2,"material":0}]}],
        "materials":[{
          "pbrMetallicRoughness":{
            "baseColorFactor":[${baseColor[0]},${baseColor[1]},${baseColor[2]},1.0],
            "metallicFactor":$metallic,
            "roughnessFactor":$roughness
          },
          "emissiveFactor":[${emissive[0]},${emissive[1]},${emissive[2]}]
        }],
        "accessors":[
          {"bufferView":0,"componentType":5126,"count":$vCount,"type":"VEC3","min":[$minX,$minY,$minZ],"max":[$maxX,$maxY,$maxZ]},
          {"bufferView":1,"componentType":5126,"count":$vCount,"type":"VEC3"},
          {"bufferView":2,"componentType":5123,"count":${indices.size},"type":"SCALAR"}
        ],
        "bufferViews":[
          {"buffer":0,"byteOffset":$posOff,"byteLength":$posLen,"target":34962},
          {"buffer":0,"byteOffset":$nrmOff,"byteLength":$nrmLen,"target":34962},
          {"buffer":0,"byteOffset":$idxOff,"byteLength":$idxLen,"target":34963}
        ],
        "buffers":[{"byteLength":${binBytes.size}}]
      }
    """.trimIndent()
    val jRaw = json.toByteArray(Charsets.UTF_8)
    val jPad = (4 - jRaw.size % 4) % 4
    val jLen = jRaw.size + jPad
    val total = 12 + 8 + jLen + 8 + binBytes.size
    val glb = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN)
    glb.putInt(0x46546C67); glb.putInt(2); glb.putInt(total)
    glb.putInt(jLen); glb.putInt(0x4E4F534A); glb.put(jRaw); repeat(jPad) { glb.put(0x20.toByte()) }
    glb.putInt(binBytes.size); glb.putInt(0x004E4942); glb.put(binBytes)
    return glb.array()
  }

  private fun buildBoxGeom(hx: Float, hy: Float, hz: Float): Triple<FloatArray, FloatArray, ShortArray> {
    val p = arrayListOf<Float>(); val n = arrayListOf<Float>(); val idx = arrayListOf<Short>()
    fun face(v0: FloatArray, v1: FloatArray, v2: FloatArray, v3: FloatArray, nrm: FloatArray) {
      val base = (p.size / 3).toShort()
      listOf(v0, v1, v2, v3).forEach { v -> p += v[0]; p += v[1]; p += v[2]; n += nrm[0]; n += nrm[1]; n += nrm[2] }
      idx += base; idx += (base + 1).toShort(); idx += (base + 2).toShort()
      idx += base; idx += (base + 2).toShort(); idx += (base + 3).toShort()
    }
    face(floatArrayOf(-hx, -hy, hz), floatArrayOf(hx, -hy, hz), floatArrayOf(hx, hy, hz), floatArrayOf(-hx, hy, hz), floatArrayOf(0f, 0f, 1f))
    face(floatArrayOf(hx, -hy, -hz), floatArrayOf(-hx, -hy, -hz), floatArrayOf(-hx, hy, -hz), floatArrayOf(hx, hy, -hz), floatArrayOf(0f, 0f, -1f))
    face(floatArrayOf(hx, -hy, hz), floatArrayOf(hx, -hy, -hz), floatArrayOf(hx, hy, -hz), floatArrayOf(hx, hy, hz), floatArrayOf(1f, 0f, 0f))
    face(floatArrayOf(-hx, -hy, -hz), floatArrayOf(-hx, -hy, hz), floatArrayOf(-hx, hy, hz), floatArrayOf(-hx, hy, -hz), floatArrayOf(-1f, 0f, 0f))
    face(floatArrayOf(-hx, hy, hz), floatArrayOf(hx, hy, hz), floatArrayOf(hx, hy, -hz), floatArrayOf(-hx, hy, -hz), floatArrayOf(0f, 1f, 0f))
    face(floatArrayOf(-hx, -hy, -hz), floatArrayOf(hx, -hy, -hz), floatArrayOf(hx, -hy, hz), floatArrayOf(-hx, -hy, hz), floatArrayOf(0f, -1f, 0f))
    return Triple(p.toFloatArray(), n.toFloatArray(), idx.toShortArray())
  }

  private fun buildCylinderGeom(radius: Float, halfHeight: Float, segs: Int = 18): Triple<FloatArray, FloatArray, ShortArray> {
    val p = arrayListOf<Float>(); val n = arrayListOf<Float>(); val idx = arrayListOf<Short>()
    for (i in 0 until segs) {
      val a0 = (i.toFloat() / segs) * 2f * PI.toFloat()
      val a1 = ((i + 1).toFloat() / segs) * 2f * PI.toFloat()
      val c0 = cos(a0); val s0 = sin(a0); val c1 = cos(a1); val s1 = sin(a1)
      val b = (p.size / 3).toShort()
      p += radius * c0; p += -halfHeight; p += radius * s0; n += c0; n += 0f; n += s0
      p += radius * c1; p += -halfHeight; p += radius * s1; n += c1; n += 0f; n += s1
      p += radius * c1; p += halfHeight; p += radius * s1; n += c1; n += 0f; n += s1
      p += radius * c0; p += halfHeight; p += radius * s0; n += c0; n += 0f; n += s0
      idx += b; idx += (b + 1).toShort(); idx += (b + 2).toShort()
      idx += b; idx += (b + 2).toShort(); idx += (b + 3).toShort()
    }
    val topCenter = (p.size / 3).toShort()
    p += 0f; p += halfHeight; p += 0f; n += 0f; n += 1f; n += 0f
    for (i in 0 until segs) {
      val a = (i.toFloat() / segs) * 2f * PI.toFloat()
      p += radius * cos(a); p += halfHeight; p += radius * sin(a); n += 0f; n += 1f; n += 0f
    }
    for (i in 0 until segs) {
      val n0 = (topCenter + 1 + i).toShort()
      val n1 = (topCenter + 1 + (i + 1) % segs).toShort()
      idx += topCenter; idx += n1; idx += n0
    }
    return Triple(p.toFloatArray(), n.toFloatArray(), idx.toShortArray())
  }

  private fun buildSphereGeom(radius: Float, stacks: Int = 10, slices: Int = 14): Triple<FloatArray, FloatArray, ShortArray> {
    val p = arrayListOf<Float>(); val n = arrayListOf<Float>(); val idx = arrayListOf<Short>()
    for (st in 0..stacks) {
      val phi = PI.toFloat() * st / stacks
      val sp = sin(phi); val cp = cos(phi)
      for (sl in 0..slices) {
        val th = 2f * PI.toFloat() * sl / slices
        val nx = sp * cos(th); val ny = cp; val nz = sp * sin(th)
        p += radius * nx; p += radius * ny; p += radius * nz
        n += nx; n += ny; n += nz
      }
    }
    val stride = slices + 1
    for (st in 0 until stacks) {
      for (sl in 0 until slices) {
        val a = (st * stride + sl).toShort()
        val b = ((st + 1) * stride + sl).toShort()
        val c = ((st + 1) * stride + sl + 1).toShort()
        val d = (st * stride + sl + 1).toShort()
        idx += a; idx += b; idx += c; idx += a; idx += c; idx += d
      }
    }
    return Triple(p.toFloatArray(), n.toFloatArray(), idx.toShortArray())
  }

  private fun spawnPoolAsset(eng: Engine, scn: Scene, glb: ByteArray, castShadows: Boolean): FilamentAsset? {
    val asset = assetLoader?.createAsset(toDirectBuffer(glb)) ?: return null
    resourceLoader?.loadResources(asset)
    asset.releaseSourceData()
    val rm = eng.renderableManager
    for (ent in asset.entities) {
      val ri = rm.getInstance(ent)
      if (ri != 0) {
        rm.setCastShadows(ri, castShadows)
        rm.setReceiveShadows(ri, true)
      }
    }
    scn.addEntities(asset.entities)
    hideAsset(eng, asset)
    return asset
  }

  private fun hideAsset(eng: Engine, asset: FilamentAsset) {
    val tm = eng.transformManager
    val ti = tm.getInstance(asset.root)
    if (ti != 0) {
      val m = FloatArray(16)
      Mat4Math.setIdentity(m)
      Mat4Math.translate(m, 0f, -50f, 50f)
      tm.setTransform(ti, m)
    }
  }

  private fun placeAsset(eng: Engine, asset: FilamentAsset, x: Float, y: Float, z: Float, rotYDeg: Float = 0f, rotZDeg: Float = 0f) {
    val tm = eng.transformManager
    val ti = tm.getInstance(asset.root)
    if (ti != 0) {
      val m = FloatArray(16)
      Mat4Math.setIdentity(m)
      Mat4Math.translate(m, x, y, z)
      if (rotYDeg != 0f) Mat4Math.rotateAxis(m, rotYDeg, 0f, 1f, 0f)
      if (rotZDeg != 0f) Mat4Math.rotateAxis(m, rotZDeg, 0f, 0f, 1f)
      tm.setTransform(ti, m)
    }
  }

  private fun build3dWorldAndPools(eng: Engine, scn: Scene) {
    // 1) Wet satin asphalt road + dark twilight rural meadow shoulders
    val (rp, rn, ri) = buildBoxGeom(5.2f, 0.08f, 68f)
    val roadGlb = createPrimitiveGlb(rp, rn, ri, floatArrayOf(0.055f, 0.060f, 0.072f), metallic = 0.22f, roughness = 0.28f)
    roadAsset = spawnPoolAsset(eng, scn, roadGlb, castShadows = false)?.also {
      placeAsset(eng, it, 0f, -0.08f, -52f)
    }

    // 2) Scrolling highway lane dashes + roadside lamp beacons
    val (sp, sn, si) = buildBoxGeom(0.09f, 0.015f, 1.8f)
    val stripeGlb = createPrimitiveGlb(sp, sn, si, floatArrayOf(0.95f, 0.72f, 0.14f), 0.1f, 0.4f, floatArrayOf(0.8f, 0.55f, 0.08f))
    repeat(16) { spawnPoolAsset(eng, scn, stripeGlb, false)?.let { stripeAssets += it } }

    val (lp, ln, li) = buildCylinderGeom(0.09f, 2.2f, 10)
    val lampGlb = createPrimitiveGlb(lp, ln, li, floatArrayOf(0.28f, 0.32f, 0.38f), 0.85f, 0.25f, floatArrayOf(0.35f, 0.45f, 0.55f))
    repeat(12) { spawnPoolAsset(eng, scn, lampGlb, true)?.let { lampAssets += it } }

    // 3) Cross-street intersection ribbons
    val (cp, cn, ci) = buildBoxGeom(24f, 0.02f, 3.2f)
    val crossGlb = createPrimitiveGlb(cp, cn, ci, floatArrayOf(0.08f, 0.09f, 0.11f), 0.20f, 0.30f, floatArrayOf(0.05f, 0.14f, 0.20f))
    repeat(3) { spawnPoolAsset(eng, scn, crossGlb, false)?.let { crossRoadAssets += it } }

    // 4) Collectibles & hazards pools (smooth cylinders/spheres/vehicles — crisp Vulkan PBR!)
    val (vp, vn, vi) = buildCylinderGeom(0.32f, 0.52f, 16)
    val voltGlb = createPrimitiveGlb(vp, vn, vi, floatArrayOf(0.08f, 0.72f, 0.24f), 0.55f, 0.22f, floatArrayOf(0.22f, 1.9f, 0.18f))
    repeat(8) { spawnPoolAsset(eng, scn, voltGlb, true)?.let { voltCellPool += it } }

    val (op, on, oi) = buildSphereGeom(0.46f)
    val orbGlb = createPrimitiveGlb(op, on, oi, floatArrayOf(0.98f, 0.84f, 0.18f), 0.30f, 0.15f, floatArrayOf(2.2f, 1.6f, 0.25f))
    repeat(6) { spawnPoolAsset(eng, scn, orbGlb, false)?.let { solarOrbPool += it } }

    val (hp, hn, hi) = buildCylinderGeom(0.68f, 0.58f, 16)
    val hayGlb = createPrimitiveGlb(hp, hn, hi, floatArrayOf(0.68f, 0.44f, 0.12f), 0.05f, 0.88f)
    repeat(8) { spawnPoolAsset(eng, scn, hayGlb, true)?.let { hayBalePool += it } }

    val (wp, wn, wi) = buildCylinderGeom(1.05f, 0.04f, 20)
    val creekGlb = createPrimitiveGlb(wp, wn, wi, floatArrayOf(0.04f, 0.32f, 0.58f), 0.15f, 0.08f, floatArrayOf(0.04f, 0.22f, 0.38f))
    repeat(6) { spawnPoolAsset(eng, scn, creekGlb, false)?.let { creekPool += it } }

    val (tp, tn, ti) = buildBoxGeom(0.95f, 0.85f, 1.35f)
    val tractorGlb = createPrimitiveGlb(tp, tn, ti, floatArrayOf(0.10f, 0.38f, 0.14f), 0.45f, 0.32f, floatArrayOf(0.15f, 0.12f, 0.02f))
    repeat(5) { spawnPoolAsset(eng, scn, tractorGlb, true)?.let { tractorPool += it } }
  }

  private fun loadPlayerGlbIfNeeded(eng: Engine, scn: Scene) {
    val vName = state.vehicleName
    val arch = Pev3dEngine.archetypeForCategory(vName)
    val fileName = Pev3dEngine.glbAssetFileName(arch)
    val key = "$vName|$fileName"
    if (key == loadedVehicleKey) return
    loadedVehicleKey = key

    playerAsset?.let { old ->
      scn.removeEntities(old.entities)
      assetLoader?.destroyAsset(old)
      playerAsset = null
    }
    playerWheelEntities = emptyList()

    val presentFiles = try {
      appContext.assets.list("models")?.filter { it.endsWith(".glb", ignoreCase = true) }?.toSet() ?: emptySet()
    } catch (_: Throwable) {
      emptySet()
    }

    val buf = readAssetBytes(appContext.assets, "models/$fileName")
    if (buf == null) {
      state.onAssetStateChanged?.invoke(
        Pev3dAssetLoadState.MissingGlb(
          expectedFileName = fileName,
          expectedAssetPath = "app/src/main/assets/models/$fileName",
          presentGlbFiles = presentFiles,
          envSource = envSource,
        )
      )
      return
    }

    try {
      val asset = assetLoader?.createAsset(buf) ?: return
      resourceLoader?.loadResources(asset)
      asset.releaseSourceData()

      val box = asset.boundingBox
      val center = box.center
      val half = box.halfExtent
      val maxExtent = max(half[0] * 2f, max(half[1] * 2f, half[2] * 2f)).coerceAtLeast(0.001f)
      val scale = 2.0f / maxExtent
      val base = FloatArray(16)
      Mat4Math.setIdentity(base)
      // Orient vehicle facing forward (-Z down the road) and sitting cleanly on y = 0
      val bottomY = (center[1] - half[1]) * scale
      Mat4Math.translate(base, -center[0] * scale, -bottomY, -center[2] * scale)
      Mat4Math.rotateAxis(base, 90f, 0f, 1f, 0f)
      Mat4Math.scale(base, scale, scale, scale)
      playerBaseMatrix = base

      val finish = Pev3dEngine.finishForModel(vName, vName, 72)
      val rm = eng.renderableManager
      val tm = eng.transformManager
      val wheels = ArrayList<Pair<Int, FloatArray>>()
      for (ent in asset.entities) {
        val ri = rm.getInstance(ent)
        if (ri != 0) {
          rm.setCastShadows(ri, true)
          rm.setReceiveShadows(ri, true)
          val prims = rm.getPrimitiveCount(ri)
          val eName = (asset.getName(ent) ?: "").lowercase()
          for (p in 0 until prims) {
            val mi = rm.getMaterialInstanceAt(ri, p)
            try {
              if (eName.contains("frame") || eName.contains("body") || eName.contains("fairing") || eName.contains("shell")) {
                mi.setParameter(
                  "baseColorFactor",
                  finish.bodyRgb[0].toDouble().pow(2.2).toFloat(),
                  finish.bodyRgb[1].toDouble().pow(2.2).toFloat(),
                  finish.bodyRgb[2].toDouble().pow(2.2).toFloat(),
                  1f,
                )
                mi.setParameter("metallicFactor", finish.paintMetallic)
                mi.setParameter("roughnessFactor", 0.20f)
              } else if (eName.contains("headlight")) {
                mi.setParameter("emissiveFactor", 3.0f, 3.1f, 3.4f)
              } else if (eName.contains("tail")) {
                mi.setParameter("emissiveFactor", 2.8f, 0.20f, 0.16f)
              }
            } catch (_: Throwable) {
            }
          }
          if (eName.contains("wheel") || eName.contains("tire") || eName.contains("rim")) {
            val ti = tm.getInstance(ent)
            if (ti != 0) {
              val lm = FloatArray(16)
              tm.getTransform(ti, lm)
              wheels += ent to lm
            }
          }
        }
      }
      playerWheelEntities = wheels
      scn.addEntities(asset.entities)
      playerAsset = asset
      state.onAssetStateChanged?.invoke(Pev3dAssetLoadState.Loaded(fileName, envSource))
    } catch (_: Throwable) {
      state.onAssetStateChanged?.invoke(
        Pev3dAssetLoadState.MissingGlb(
          expectedFileName = fileName,
          expectedAssetPath = "app/src/main/assets/models/$fileName",
          presentGlbFiles = presentFiles,
          envSource = envSource,
        )
      )
    }
  }

  private fun renderFrame(frameTimeNanos: Long) {
    val eng = engine ?: return
    val ren = renderer ?: return
    val scn = scene ?: return
    val vw = view ?: return
    val cam = camera ?: return
    val sc = swapChain ?: return
    if (uiHelper?.isReadyToRender != true) return

    loadPlayerGlbIfNeeded(eng, scn)

    val timeSec = ((frameTimeNanos - startNs) / 1_000_000_000.0).toFloat()
    val laneX = state.lanePos * 2.4f
    val jumpY = state.jumpHeight * 2.4f
    val bank = state.turnBankDeg
    val shake = state.shakeAmount
    val shakeX = if (shake > 0.01f) sin(timeSec * 65f) * 0.25f * shake else 0f
    val shakeY = if (shake > 0.01f) cos(timeSec * 58f) * 0.18f * shake else 0f

    // Chase Camera behind player PEV looking down the 3D twilight backroad (-Z)
    val camX = (laneX * 0.62f + shakeX).toDouble()
    val camY = (2.85f + jumpY * 0.28f + shakeY).toDouble()
    val camZ = (if (state.boostActive) 5.2f else 5.8f).toDouble()
    val bankRad = (bank * 0.28f * PI / 180.0).toFloat()
    cam.lookAt(
      camX, camY, camZ,
      (laneX * 0.45f - bank * 0.08f).toDouble(), 0.75, -18.0,
      sin(bankRad).toDouble(), cos(bankRad).toDouble(), 0.0,
    )

    // Update dynamic vehicle headlight spot beam, taillight & boost glow
    val lm = eng.lightManager
    lm.getInstance(headlightSpotEntity).takeIf { it != 0 }?.let { i ->
      lm.setPosition(i, laneX, 1.05f + jumpY, -0.6f)
      lm.setIntensity(i, if (state.boostActive) 340_000f else 240_000f)
    }
    lm.getInstance(taillightPointEntity).takeIf { it != 0 }?.let { i ->
      lm.setPosition(i, laneX, 0.75f + jumpY, 1.15f)
    }
    lm.getInstance(boostPointEntity).takeIf { it != 0 }?.let { i ->
      lm.setPosition(i, laneX, 0.65f + jumpY, 0.5f)
      lm.setIntensity(i, if (state.boostActive) 95_000f else 0f)
    }

    // Update Player .glb Transform (lane lean + bunny hop + turn banking)
    playerAsset?.let { pAsset ->
      val tm = eng.transformManager
      val rootTi = tm.getInstance(pAsset.root)
      if (rootTi != 0) {
        val worldMat = FloatArray(16)
        Mat4Math.setIdentity(worldMat)
        Mat4Math.translate(worldMat, laneX, jumpY, 0f)
        Mat4Math.rotateAxis(worldMat, -bank * 0.55f, 0f, 0f, 1f)
        if (jumpY > 0.05f) Mat4Math.rotateAxis(worldMat, 12f * state.jumpHeight, 1f, 0f, 0f)
        val finalMat = FloatArray(16)
        Mat4Math.multiplyMM(finalMat, worldMat, playerBaseMatrix)
        tm.setTransform(rootTi, finalMat)
      }
      if (playerWheelEntities.isNotEmpty()) {
        val rotMat = FloatArray(16)
        val combined = FloatArray(16)
        Mat4Math.setIdentity(rotMat)
        Mat4Math.rotateAxis(rotMat, -state.wheelSpinDeg, 0f, 0f, 1f)
        for ((wEnt, baseLocal) in playerWheelEntities) {
          val wTi = tm.getInstance(wEnt)
          if (wTi != 0) {
            Mat4Math.multiplyMM(combined, baseLocal, rotMat)
            tm.setTransform(wTi, combined)
          }
        }
      }
    }

    // Scroll highway lane markers & roadside poles along -Z -> +Z
    val scrollZ = state.roadScroll * 10f
    for (idx in 0 until stripeAssets.size) {
      val pair = idx / 2
      val side = if (idx % 2 == 0) -1.25f else 1.25f
      val z = -72f + pair * 10f + scrollZ
      placeAsset(eng, stripeAssets[idx], side, 0.015f, z)
    }
    for (idx in 0 until lampAssets.size) {
      val pair = idx / 2
      val side = if (idx % 2 == 0) -5.4f else 5.4f
      val z = -75f + pair * 14f + (state.roadScroll * 14f)
      placeAsset(eng, lampAssets[idx], side, 2.2f, z)
    }

    // Cross-street intersections
    val interZs = state.intersectionZs
    for (idx in crossRoadAssets.indices) {
      if (idx < interZs.size) {
        val worldZ = -68f + interZs[idx] * 68f
        placeAsset(eng, crossRoadAssets[idx], 0f, 0.008f, worldZ)
      } else {
        hideAsset(eng, crossRoadAssets[idx])
      }
    }

    // Place pooled 3D obstacles & collectibles
    var vIdx = 0; var oIdx = 0; var hIdx = 0; var cIdx = 0; var tIdx = 0
    for (item in state.entities) {
      val wx = item.lane * 2.4f
      val wz = -68f + item.z * 68f
      when (item.kindCode) {
        3 -> if (vIdx < voltCellPool.size) {
          val bob = 0.65f + 0.16f * sin(timeSec * 5f + vIdx)
          placeAsset(eng, voltCellPool[vIdx++], wx, bob, wz, rotYDeg = timeSec * 120f)
        }
        4 -> if (oIdx < solarOrbPool.size) {
          val bob = 0.85f + 0.20f * sin(timeSec * 4.2f + oIdx)
          placeAsset(eng, solarOrbPool[oIdx++], wx, bob, wz, rotYDeg = timeSec * 90f)
        }
        0 -> if (hIdx < hayBalePool.size) {
          placeAsset(eng, hayBalePool[hIdx++], wx, 0.58f, wz, rotZDeg = 90f)
        }
        1 -> if (cIdx < creekPool.size) {
          placeAsset(eng, creekPool[cIdx++], wx, 0.04f, wz)
        }
        2 -> if (tIdx < tractorPool.size) {
          placeAsset(eng, tractorPool[tIdx++], wx, 0.85f, wz)
        }
      }
    }
    while (vIdx < voltCellPool.size) hideAsset(eng, voltCellPool[vIdx++])
    while (oIdx < solarOrbPool.size) hideAsset(eng, solarOrbPool[oIdx++])
    while (hIdx < hayBalePool.size) hideAsset(eng, hayBalePool[hIdx++])
    while (cIdx < creekPool.size) hideAsset(eng, creekPool[cIdx++])
    while (tIdx < tractorPool.size) hideAsset(eng, tractorPool[tIdx++])

    if (ren.beginFrame(sc, frameTimeNanos)) {
      ren.render(vw)
      ren.endFrame()
    }
  }

  fun resume() {
    if (destroyed || started) return
    started = true
    choreographer.postFrameCallback(frameCallback)
  }

  fun pause() {
    started = false
    choreographer.removeFrameCallback(frameCallback)
  }

  fun destroy() {
    if (destroyed) return
    destroyed = true
    pause()
    val eng = engine ?: return
    val scn = scene
    uiHelper?.detach()
    uiHelper = null

    fun destroyAssetSafe(a: FilamentAsset?) {
      a?.let {
        scn?.removeEntities(it.entities)
        assetLoader?.destroyAsset(it)
      }
    }
    destroyAssetSafe(playerAsset); playerAsset = null
    destroyAssetSafe(roadAsset); roadAsset = null
    stripeAssets.forEach { destroyAssetSafe(it) }; stripeAssets.clear()
    lampAssets.forEach { destroyAssetSafe(it) }; lampAssets.clear()
    crossRoadAssets.forEach { destroyAssetSafe(it) }; crossRoadAssets.clear()
    voltCellPool.forEach { destroyAssetSafe(it) }; voltCellPool.clear()
    solarOrbPool.forEach { destroyAssetSafe(it) }; solarOrbPool.clear()
    hayBalePool.forEach { destroyAssetSafe(it) }; hayBalePool.clear()
    creekPool.forEach { destroyAssetSafe(it) }; creekPool.clear()
    tractorPool.forEach { destroyAssetSafe(it) }; tractorPool.clear()

    resourceLoader?.destroy()
    assetLoader?.destroy()
    ubershader?.destroy()
    swapChain?.let { eng.destroySwapChain(it) }
    indirectLight?.let { eng.destroyIndirectLight(it) }
    iblTexture?.let { eng.destroyTexture(it) }
    skybox?.let { eng.destroySkybox(it) }
    skyboxTexture?.let { eng.destroyTexture(it) }
    colorGrading?.let { eng.destroyColorGrading(it) }

    listOf(sunLightEntity, skyRimLightEntity, headlightSpotEntity, taillightPointEntity, boostPointEntity).forEach { ent ->
      if (ent != 0) {
        scn?.removeEntity(ent)
        eng.destroyEntity(ent)
        EntityManager.get().destroy(ent)
      }
    }
    view?.let { eng.destroyView(it) }
    scn?.let { eng.destroyScene(it) }
    renderer?.let { eng.destroyRenderer(it) }
    if (cameraEntity != 0) {
      eng.destroyCameraComponent(cameraEntity)
      EntityManager.get().destroy(cameraEntity)
    }
    eng.destroy()
    engine = null
  }
}

@Composable
internal fun GravelVoltVulkanViewport(
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
  modifier: Modifier = Modifier,
) {
  val state = remember { GravelVoltVulkanState() }
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

  val appContext = LocalContext.current.applicationContext
  val host = remember { GravelVoltVulkanHost(appContext, state) }
  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> host.resume()
        Lifecycle.Event.ON_PAUSE -> host.pause()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      host.destroy()
    }
  }

  AndroidView(
    modifier = modifier,
    factory = { ctx ->
      SurfaceView(ctx).apply {
        host.attach(this)
      }
    },
  )
}
