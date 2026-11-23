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

internal enum class Pev3dLightingPreset(val label: String) {
  STUDIO("Studio"),
  SUNSET("Sunset"),
  NEON("Neon Dyno"),
}

internal sealed interface Pev3dAssetLoadState {
  data object Loading : Pev3dAssetLoadState
  data class Loaded(val fileName: String, val envSource: String) : Pev3dAssetLoadState
  data class MissingGlb(
    val expectedFileName: String,
    val expectedAssetPath: String,
    val presentGlbFiles: Set<String>,
    val envSource: String,
  ) : Pev3dAssetLoadState
  data class VulkanUnavailable(val reason: String) : Pev3dAssetLoadState
}

internal class Pev3dVulkanState {
  @Volatile var archetype: Pev3dArchetype = Pev3dArchetype.EMOTO
  @Volatile var renderMode: Pev3dRenderMode = Pev3dRenderMode.SOLID
  @Volatile var lightingPreset: Pev3dLightingPreset = Pev3dLightingPreset.STUDIO
  @Volatile var modelName: String = "Sur-Ron Ultra Bee eMoto"
  @Volatile var modelCategory: String = "Electric Dirt Moped"
  @Volatile var modelVoltage: Int = 74
  @Volatile var yawDeg: Float = -28f
  @Volatile var pitchDeg: Float = 16f
  @Volatile var zoom: Float = 1.05f
  @Volatile var explodePct: Float = 0f
  @Volatile var wheelSpinDeg: Float = 0f
  @Volatile var lightsOn: Boolean = true
  @Volatile var ssaoEnabled: Boolean = true
  @Volatile var turnSignalMode: Pev3dTurnSignalMode = Pev3dTurnSignalMode.OFF
  @Volatile var selectedPartKey: String? = "battery"
  @Volatile var accentRgb: FloatArray = floatArrayOf(0.80f, 0.64f, 0.28f)
  @Volatile var onWireframeReady: ((List<PevWireGroup>) -> Unit)? = null
  @Volatile var onAssetStateChanged: ((Pev3dAssetLoadState) -> Unit)? = null
}

private object FilamentBoot {
  @Volatile private var initialized = false

  fun ensureInit() {
    if (initialized) return
    synchronized(this) {
      if (initialized) return
      Filament.init()
      Gltfio.init()
      Utils.init()
      initialized = true
    }
  }
}

/**
 * Pure native Android Vulkan PBR renderer powered by Google Filament 1.81.0.
 * Zero OpenGL / GLES2 / GLES3 code paths.
 */
private class PevVulkanHost(
  private val appContext: Context,
  private val state: Pev3dVulkanState,
  private val onBackendReady: (String) -> Unit,
) {
  private var engine: Engine? = null
  private var renderer: Renderer? = null
  private var scene: Scene? = null
  private var view: View? = null
  private var camera: Camera? = null
  private var cameraEntity: Int = 0
  private var keyLightEntity: Int = 0
  private var rimLightEntity: Int = 0
  private var fillLightEntity: Int = 0
  private var headlightSpotEntity: Int = 0
  private var taillightPointEntity: Int = 0
  private var leftSignalPointEntity: Int = 0
  private var rightSignalPointEntity: Int = 0
  private var underglowPointEntity: Int = 0

  private var iblTexture: Texture? = null
  private var skyboxTexture: Texture? = null
  private var indirectLight: IndirectLight? = null
  private var skybox: Skybox? = null
  private var colorGrading: ColorGrading? = null
  private var swapChain: SwapChain? = null
  private var appliedLightingPreset: Pev3dLightingPreset? = null
  private var appliedSsaoEnabled: Boolean? = null
  private var activeEnvSource: String = "Procedural Studio IBL"

  private var ubershaderProvider: UbershaderProvider? = null
  private var assetLoader: AssetLoader? = null
  private var resourceLoader: ResourceLoader? = null
  private var floorAsset: FilamentAsset? = null
  private var vehicleAsset: FilamentAsset? = null
  private var loadedGlbKey: String = ""
  private var lastStyleKey: String = ""
  private var vehicleBaseTransform: FloatArray = FloatArray(16).also { Mat4Math.setIdentity(it) }

  private enum class GlbPartRole {
    BODY_PAINT,
    SECONDARY_TRIM,
    SUSPENSION_ACCENT,
    CALIPER_ACCENT,
    TIRE_RUBBER,
    BATTERY,
    MOTOR,
    CONTROLLER,
    COCKPIT,
    HEADLIGHT,
    TAILLIGHT,
    SIGNAL_LEFT,
    SIGNAL_RIGHT,
    GENERIC_METAL,
  }

  private data class GlbNodeBinding(
    val entity: Int,
    val partKey: String,
    val role: GlbPartRole,
    val baseLocalMatrix: FloatArray,
    val isWheelNode: Boolean,
    val explodeDir: Vec3,
  )

  private var nodeBindings: List<GlbNodeBinding> = emptyList()
  private var uiHelper: UiHelper? = null
  private var started = false
  private var destroyed = false
  private var startNs = System.nanoTime()

  private val choreographer = Choreographer.getInstance()
  private val frameCallback = object : Choreographer.FrameCallback {
    override fun doFrame(frameTimeNanos: Long) {
      if (destroyed) return
      if (started) {
        choreographer.postFrameCallback(this)
        renderFrame(frameTimeNanos)
      }
    }
  }

  fun attach(surfaceView: SurfaceView) {
    try {
      FilamentBoot.ensureInit()
      // Native Android VULKAN backend exclusively (no OpenGL / GLES fallback)
      val eng = Engine.Builder().backend(Engine.Backend.VULKAN).build()
      onBackendReady("VULKAN • FILAMENT PBR")
      engine = eng
      renderer = eng.createRenderer()
      scene = eng.createScene()
      view = eng.createView()

      val em = EntityManager.get()
      cameraEntity = em.create()
      camera = eng.createCamera(cameraEntity)

      ubershaderProvider = UbershaderProvider(eng)
      assetLoader = AssetLoader(eng, ubershaderProvider!!, em)
      resourceLoader = ResourceLoader(eng)

      setupEnvironmentAndLights(eng, scene!!, view!!)
      loadShowroomFloorPlane(eng, scene!!)
      view!!.scene = scene
      view!!.camera = camera

      val helper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
      helper.renderCallback = object : UiHelper.RendererCallback {
        override fun onNativeWindowChanged(surface: Surface) {
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
          swapChain?.let {
            eng.destroySwapChain(it)
            swapChain = null
          }
        }

        override fun onResized(width: Int, height: Int) {
          val w = width.coerceAtLeast(1)
          val h = height.coerceAtLeast(1)
          view?.viewport = Viewport(0, 0, w, h)
          camera?.setProjection(38.0, w.toDouble() / h.toDouble(), 0.25, 40.0, Camera.Fov.VERTICAL)
        }
      }
      helper.attachTo(surfaceView)
      uiHelper = helper
      resume()
    } catch (t: Throwable) {
      state.onAssetStateChanged?.invoke(
        Pev3dAssetLoadState.VulkanUnavailable(t.message ?: "Native Vulkan 1.1 GPU backend unavailable on this device")
      )
      destroy()
    }
  }

  private fun setupEnvironmentAndLights(eng: Engine, scn: Scene, vw: View) {
    val em = EntityManager.get()
    val shadowCfg = LightManager.ShadowOptions().apply {
      mapSize = 2048
      shadowCascades = 2
      shadowBulbRadius = 0.04f
    }
    keyLightEntity = em.create()
    LightManager.Builder(LightManager.Type.DIRECTIONAL)
      .color(1.0f, 0.98f, 0.95f)
      .intensity(115_000f)
      .direction(-0.46f, -0.80f, -0.38f)
      .castShadows(true)
      .shadowOptions(shadowCfg)
      .build(eng, keyLightEntity)
    scn.addEntity(keyLightEntity)

    rimLightEntity = em.create()
    LightManager.Builder(LightManager.Type.DIRECTIONAL)
      .color(0.86f, 0.91f, 0.98f)
      .intensity(44_000f)
      .direction(0.75f, -0.25f, 0.61f)
      .castShadows(false)
      .build(eng, rimLightEntity)
    scn.addEntity(rimLightEntity)

    fillLightEntity = em.create()
    LightManager.Builder(LightManager.Type.DIRECTIONAL)
      .color(0.96f, 0.92f, 0.88f)
      .intensity(32_000f)
      .direction(-0.42f, -0.20f, 0.88f)
      .castShadows(false)
      .build(eng, fillLightEntity)
    scn.addEntity(fillLightEntity)

    // Physical Vehicle Light Emitters (Headlight Spot + Taillight + Turn Signals + Optional Dyno Underglow)
    headlightSpotEntity = em.create()
    LightManager.Builder(LightManager.Type.SPOT)
      .color(0.96f, 0.98f, 1.0f)
      .intensity(0f)
      .position(0.68f, 0.44f, 0f)
      .direction(0.92f, -0.39f, 0f)
      .falloff(6.5f)
      .spotLightCone(0.28f, 0.62f)
      .castShadows(false)
      .build(eng, headlightSpotEntity)
    scn.addEntity(headlightSpotEntity)

    taillightPointEntity = em.create()
    LightManager.Builder(LightManager.Type.POINT)
      .color(1.0f, 0.14f, 0.10f)
      .intensity(0f)
      .position(-0.84f, 0.27f, 0f)
      .falloff(2.8f)
      .build(eng, taillightPointEntity)
    scn.addEntity(taillightPointEntity)

    leftSignalPointEntity = em.create()
    LightManager.Builder(LightManager.Type.POINT)
      .color(1.0f, 0.55f, 0.04f)
      .intensity(0f)
      .position(0.65f, 0.46f, -0.34f)
      .falloff(2.4f)
      .build(eng, leftSignalPointEntity)
    scn.addEntity(leftSignalPointEntity)

    rightSignalPointEntity = em.create()
    LightManager.Builder(LightManager.Type.POINT)
      .color(1.0f, 0.55f, 0.04f)
      .intensity(0f)
      .position(0.65f, 0.46f, 0.34f)
      .falloff(2.4f)
      .build(eng, rightSignalPointEntity)
    scn.addEntity(rightSignalPointEntity)

    underglowPointEntity = em.create()
    LightManager.Builder(LightManager.Type.POINT)
      .color(0.22f, 0.85f, 0.96f)
      .intensity(0f)
      .position(0f, -0.45f, 0f)
      .falloff(2.6f)
      .build(eng, underglowPointEntity)
    scn.addEntity(underglowPointEntity)

    applyLightingPreset(eng, scn, state.lightingPreset)

    // Full UE5-Style Filament 1.81.0 Post-Processing Pipeline:
    // ACES Filmic ColorGrading, 4x MSAA + FXAA, High SSAO, Subtle HDR Bloom, PCF Shadows & DynamicLightingOptions
    colorGrading = ColorGrading.Builder()
      .toneMapping(ColorGrading.ToneMapping.ACES)
      .build(eng)
    vw.colorGrading = colorGrading
    vw.multiSampleAntiAliasingOptions = View.MultiSampleAntiAliasingOptions().apply {
      enabled = true
      sampleCount = 4
    }
    vw.antiAliasing = View.AntiAliasing.FXAA
    vw.dithering = View.Dithering.TEMPORAL
    vw.setShadowingEnabled(true)
    vw.setShadowType(View.ShadowType.PCF)
    vw.setDynamicLightingOptions(0.25f, 40.0f)
    vw.bloomOptions = View.BloomOptions().apply {
      enabled = true
      strength = 0.16f
      levels = 5
      threshold = true
    }
    applySsaoIfNeeded(vw)
    vw.vignetteOptions = View.VignetteOptions().apply {
      enabled = true
      midPoint = 0.54f
      roundness = 0.68f
      feather = 0.42f
    }
  }

  private fun applySsaoIfNeeded(vw: View) {
    val desired = state.ssaoEnabled
    if (appliedSsaoEnabled == desired) return
    appliedSsaoEnabled = desired
    vw.ambientOcclusionOptions = if (desired) {
      View.AmbientOcclusionOptions().apply {
        enabled = true
        radius = 0.28f
        intensity = 1.45f
        power = 1.25f
        bias = 0.0005f
        resolution = 1.0f
        quality = View.QualityLevel.HIGH
        bilateralThreshold = 0.05f
        bentNormals = true
      }
    } else {
      View.AmbientOcclusionOptions().apply {
        enabled = false
      }
    }
  }

  /**
   * Builds and loads a dedicated dark satin automotive showroom floor plane
   * that receives real-time PCF contact shadows from any loaded .glb vehicle.
   */
  private fun loadShowroomFloorPlane(eng: Engine, scn: Scene) {
    try {
      val glbBytes = buildShowroomFloorGlb()
      val asset = assetLoader?.createAsset(ByteBuffer.wrap(glbBytes)) ?: return
      resourceLoader?.loadResources(asset)
      asset.releaseSourceData()
      val rm = eng.renderableManager
      for (ent in asset.entities) {
        val ri = rm.getInstance(ent)
        if (ri != 0) {
          rm.setCastShadows(ri, false)
          rm.setReceiveShadows(ri, true)
        }
      }
      scn.addEntities(asset.entities)
      floorAsset = asset
    } catch (_: Throwable) {
    }
  }

  private fun buildShowroomFloorGlb(): ByteArray {
    val floorBundle = Pev3dPbrTextures.getBundle(PevPbrMaterialId.DYNO_FLOOR)
    val r = 2.85f
    val y = -0.78f
    val positions = floatArrayOf(-r, y, r, r, y, r, r, y, -r, -r, y, -r)
    val normals = floatArrayOf(0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f)
    val tangents = floatArrayOf(1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f)
    val uvs = floatArrayOf(0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f)
    val indices = shortArrayOf(0, 1, 2, 0, 2, 3)

    val bin = ByteArrayOutputStream(64 * 1024)
    fun pad4() { while (bin.size() % 4 != 0) bin.write(0) }
    fun writeFloats(arr: FloatArray): Pair<Int, Int> {
      pad4()
      val off = bin.size()
      val bb = ByteBuffer.allocate(arr.size * 4).order(ByteOrder.LITTLE_ENDIAN)
      arr.forEach { bb.putFloat(it) }
      bin.write(bb.array())
      return off to arr.size * 4
    }
    fun writeShorts(arr: ShortArray): Pair<Int, Int> {
      pad4()
      val off = bin.size()
      val bb = ByteBuffer.allocate(arr.size * 2).order(ByteOrder.LITTLE_ENDIAN)
      arr.forEach { bb.putShort(it) }
      bin.write(bb.array())
      return off to arr.size * 2
    }
    fun writeBytes(arr: ByteArray): Pair<Int, Int> {
      pad4()
      val off = bin.size()
      bin.write(arr)
      return off to arr.size
    }

    val (posOff, posLen) = writeFloats(positions)
    val (nrmOff, nrmLen) = writeFloats(normals)
    val (tanOff, tanLen) = writeFloats(tangents)
    val (uvOff, uvLen) = writeFloats(uvs)
    val (idxOff, idxLen) = writeShorts(indices)
    val (albOff, albLen) = writeBytes(floorBundle.albedoPng)
    val (nTexOff, nTexLen) = writeBytes(floorBundle.normalPng)
    val (mrOff, mrLen) = writeBytes(floorBundle.metallicRoughnessPng)
    pad4()
    val binBytes = bin.toByteArray()

    val json = """
      {
        "asset":{"version":"2.0","generator":"RuralERides-VulkanShowroomFloor"},
        "scene":0,
        "scenes":[{"nodes":[0]}],
        "nodes":[{"name":"showroom_floor","mesh":0}],
        "meshes":[{"primitives":[{"attributes":{"POSITION":0,"NORMAL":1,"TANGENT":2,"TEXCOORD_0":3},"indices":4,"material":0}]}],
        "materials":[{
          "name":"showroom_satin_floor",
          "pbrMetallicRoughness":{
            "baseColorFactor":[0.82,0.84,0.88,1.0],
            "baseColorTexture":{"index":0},
            "metallicFactor":0.18,
            "roughnessFactor":0.34,
            "metallicRoughnessTexture":{"index":2}
          },
          "normalTexture":{"index":1,"scale":0.45}
        }],
        "samplers":[{"magFilter":9729,"minFilter":9987,"wrapS":10497,"wrapT":10497}],
        "textures":[{"sampler":0,"source":0},{"sampler":0,"source":1},{"sampler":0,"source":2}],
        "images":[
          {"bufferView":5,"mimeType":"image/png"},
          {"bufferView":6,"mimeType":"image/png"},
          {"bufferView":7,"mimeType":"image/png"}
        ],
        "accessors":[
          {"bufferView":0,"componentType":5126,"count":4,"type":"VEC3","min":[-$r,$y,-$r],"max":[$r,$y,$r]},
          {"bufferView":1,"componentType":5126,"count":4,"type":"VEC3"},
          {"bufferView":2,"componentType":5126,"count":4,"type":"VEC4"},
          {"bufferView":3,"componentType":5126,"count":4,"type":"VEC2"},
          {"bufferView":4,"componentType":5123,"count":6,"type":"SCALAR"}
        ],
        "bufferViews":[
          {"buffer":0,"byteOffset":$posOff,"byteLength":$posLen,"target":34962},
          {"buffer":0,"byteOffset":$nrmOff,"byteLength":$nrmLen,"target":34962},
          {"buffer":0,"byteOffset":$tanOff,"byteLength":$tanLen,"target":34962},
          {"buffer":0,"byteOffset":$uvOff,"byteLength":$uvLen,"target":34962},
          {"buffer":0,"byteOffset":$idxOff,"byteLength":$idxLen,"target":34963},
          {"buffer":0,"byteOffset":$albOff,"byteLength":$albLen},
          {"buffer":0,"byteOffset":$nTexOff,"byteLength":$nTexLen},
          {"buffer":0,"byteOffset":$mrOff,"byteLength":$mrLen}
        ],
        "buffers":[{"byteLength":${binBytes.size}}]
      }
    """.trimIndent()

    val jsonRaw = json.toByteArray(Charsets.UTF_8)
    val jsonPad = (4 - (jsonRaw.size % 4)) % 4
    val jsonChunkLen = jsonRaw.size + jsonPad
    val totalLen = 12 + 8 + jsonChunkLen + 8 + binBytes.size
    val glb = ByteBuffer.allocate(totalLen).order(ByteOrder.LITTLE_ENDIAN)
    glb.putInt(0x46546C67)
    glb.putInt(2)
    glb.putInt(totalLen)
    glb.putInt(jsonChunkLen)
    glb.putInt(0x4E4F534A)
    glb.put(jsonRaw)
    repeat(jsonPad) { glb.put(0x20.toByte()) }
    glb.putInt(binBytes.size)
    glb.putInt(0x004E4942)
    glb.put(binBytes)
    return glb.array()
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

  private fun applyLightingPreset(eng: Engine, scn: Scene, preset: Pev3dLightingPreset) {
    if (appliedLightingPreset == preset) return
    appliedLightingPreset = preset

    indirectLight?.let { eng.destroyIndirectLight(it) }
    iblTexture?.let { eng.destroyTexture(it) }
    skybox?.let { eng.destroySkybox(it) }
    skyboxTexture?.let { eng.destroyTexture(it) }
    indirectLight = null
    iblTexture = null
    skybox = null
    skyboxTexture = null

    val presetPrefix = preset.name.lowercase()
    val assets = appContext.assets
    val ktxIblBuf = readAssetBytes(assets, "envs/${presetPrefix}_ibl.ktx")
      ?: readAssetBytes(assets, "envs/studio_ibl.ktx")
    val ktxSkyBuf = readAssetBytes(assets, "envs/${presetPrefix}_skybox.ktx")
      ?: readAssetBytes(assets, "envs/studio_skybox.ktx")

    // Prefer real cmgen .ktx HDRI environment from assets/envs/; otherwise synthesize studio SH + softbox cubemap
    val loadedKtxIbl = ktxIblBuf?.let { buf ->
      try {
        KTX1Loader.createIndirectLight(eng, buf).indirectLight?.also { il ->
          il.intensity = 38_000f
          indirectLight = il
          scn.indirectLight = il
          activeEnvSource = "Studio HDRI (.ktx)"
        }
      } catch (_: Throwable) {
        null
      }
    }

    if (loadedKtxIbl == null) {
      val cubemap = createStudioCubemapTexture(eng, 128, preset)
      iblTexture = cubemap
      val shBands =
        when (preset) {
          Pev3dLightingPreset.STUDIO -> floatArrayOf(
            0.38f, 0.39f, 0.41f,
            0.14f, 0.14f, 0.15f,
            0.26f, 0.26f, 0.27f,
            -0.05f, -0.05f, -0.04f,
            0.04f, 0.04f, 0.05f,
            0.03f, 0.03f, 0.04f,
            0.08f, 0.08f, 0.09f,
            -0.04f, -0.04f, -0.03f,
            0.04f, 0.04f, 0.05f,
          )
          Pev3dLightingPreset.SUNSET -> floatArrayOf(
            0.42f, 0.32f, 0.25f,
            0.18f, 0.12f, 0.08f,
            0.22f, 0.18f, 0.16f,
            -0.04f, -0.03f, -0.02f,
            0.06f, 0.04f, 0.02f,
            0.05f, 0.04f, 0.03f,
            0.10f, 0.07f, 0.04f,
            -0.03f, -0.02f, -0.01f,
            0.06f, 0.04f, 0.02f,
          )
          Pev3dLightingPreset.NEON -> floatArrayOf(
            0.28f, 0.30f, 0.42f,
            0.20f, 0.08f, 0.24f,
            0.12f, 0.26f, 0.32f,
            -0.07f, -0.04f, -0.05f,
            0.06f, 0.03f, 0.08f,
            0.04f, 0.06f, 0.09f,
            0.09f, 0.05f, 0.12f,
            -0.04f, -0.02f, -0.05f,
            0.06f, 0.08f, 0.12f,
          )
        }
      indirectLight = IndirectLight.Builder()
        .reflections(cubemap)
        .irradiance(3, shBands)
        .intensity(38_000f)
        .build(eng)
      scn.indirectLight = indirectLight
      activeEnvSource = "Procedural Studio IBL"
    }

    val loadedKtxSky = ktxSkyBuf?.let { buf ->
      try {
        KTX1Loader.createSkybox(eng, buf).skybox?.also { sb ->
          skybox = sb
          scn.skybox = sb
        }
      } catch (_: Throwable) {
        null
      }
    }
    if (loadedKtxSky == null) {
      val skyTex = createCycloramaSkyboxTexture(eng, 128, preset)
      skyboxTexture = skyTex
      skybox = Skybox.Builder().environment(skyTex).build(eng)
      scn.skybox = skybox
    }

    val lm = eng.lightManager
    val keyI = lm.getInstance(keyLightEntity)
    val rimI = lm.getInstance(rimLightEntity)
    val fillI = lm.getInstance(fillLightEntity)
    when (preset) {
      Pev3dLightingPreset.STUDIO -> {
        if (keyI != 0) {
          lm.setColor(keyI, 1.0f, 0.98f, 0.95f)
          lm.setIntensity(keyI, 115_000f)
          lm.setDirection(keyI, -0.46f, -0.80f, -0.38f)
        }
        if (rimI != 0) {
          lm.setColor(rimI, 0.86f, 0.91f, 0.98f)
          lm.setIntensity(rimI, 44_000f)
        }
        if (fillI != 0) {
          lm.setColor(fillI, 0.96f, 0.92f, 0.88f)
          lm.setIntensity(fillI, 32_000f)
        }
      }
      Pev3dLightingPreset.SUNSET -> {
        if (keyI != 0) {
          lm.setColor(keyI, 1.0f, 0.68f, 0.36f)
          lm.setIntensity(keyI, 130_000f)
          lm.setDirection(keyI, -0.68f, -0.34f, -0.64f)
        }
        if (rimI != 0) {
          lm.setColor(rimI, 0.38f, 0.54f, 0.96f)
          lm.setIntensity(rimI, 42_000f)
        }
        if (fillI != 0) {
          lm.setColor(fillI, 1.0f, 0.52f, 0.30f)
          lm.setIntensity(fillI, 36_000f)
        }
      }
      Pev3dLightingPreset.NEON -> {
        if (keyI != 0) {
          lm.setColor(keyI, 0.74f, 0.94f, 1.0f)
          lm.setIntensity(keyI, 105_000f)
          lm.setDirection(keyI, -0.36f, -0.85f, -0.38f)
        }
        if (rimI != 0) {
          lm.setColor(rimI, 1.0f, 0.22f, 0.78f)
          lm.setIntensity(rimI, 66_000f)
        }
        if (fillI != 0) {
          lm.setColor(fillI, 0.14f, 0.96f, 0.84f)
          lm.setIntensity(fillI, 52_000f)
        }
      }
    }
  }

  private fun createStudioCubemapTexture(
    eng: Engine,
    baseSize: Int,
    preset: Pev3dLightingPreset,
  ): Texture {
    val levels = 8
    val tex = Texture.Builder()
      .width(baseSize)
      .height(baseSize)
      .levels(levels)
      .sampler(Texture.Sampler.SAMPLER_CUBEMAP)
      .format(Texture.InternalFormat.RGBA8)
      .build(eng)

    for (level in 0 until levels) {
      val size = (baseSize shr level).coerceAtLeast(1)
      val roughnessBlur = level / (levels - 1f)
      val faceBytes = size * size * 4
      for (face in 0 until 6) {
        val faceBuf = ByteBuffer.allocateDirect(faceBytes).order(ByteOrder.LITTLE_ENDIAN)
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
            val nx = dx * inv
            val ny = dy * inv
            val nz = dz * inv

            var r: Float
            var g: Float
            var b: Float
            when (preset) {
              Pev3dLightingPreset.STUDIO -> {
                r = 0.13f + 0.14f * roughnessBlur
                g = 0.135f + 0.145f * roughnessBlur
                b = 0.145f + 0.155f * roughnessBlur
                val skyFactor = ((ny + 0.25f) / 1.25f).coerceIn(0f, 1f)
                r += skyFactor * roughnessBlur * 0.24f
                g += skyFactor * roughnessBlur * 0.24f
                b += skyFactor * roughnessBlur * 0.25f
                if (ny > 0.26f - 0.35f * roughnessBlur) {
                  val softExpand = 0.18f * roughnessBlur
                  val inZ1 = nz > (0.14f - softExpand) && nz < (0.76f + softExpand)
                  val inZ2 = nz > (-0.76f - softExpand) && nz < (-0.14f + softExpand)
                  val inX = abs(nx) < (0.68f + softExpand)
                  val sharpWeight = 1f - roughnessBlur * 0.62f
                  val boost = (if (inX && inZ1) 0.96f else 0f) + (if (inX && inZ2) 0.78f else 0f)
                  r += boost * sharpWeight * 0.95f
                  g += boost * sharpWeight * 0.96f
                  b += boost * sharpWeight * 0.98f
                } else if (ny > -0.18f) {
                  val horiz = (1f - abs(ny) / 0.32f).coerceIn(0f, 1f)
                  r += horiz * 0.20f
                  g += horiz * 0.21f
                  b += horiz * 0.23f
                } else {
                  r = 0.05f + 0.05f * roughnessBlur
                  g = 0.052f + 0.052f * roughnessBlur
                  b = 0.058f + 0.058f * roughnessBlur
                }
              }
              Pev3dLightingPreset.SUNSET -> {
                r = 0.14f + 0.12f * roughnessBlur
                g = 0.09f + 0.09f * roughnessBlur
                b = 0.11f + 0.10f * roughnessBlur
                val sunAlign = ((nx * -0.6f + ny * 0.3f + nz * -0.7f) * 0.5f + 0.5f).coerceIn(0f, 1f)
                val sunDisk = if (sunAlign > 0.84f - 0.25f * roughnessBlur) (sunAlign - 0.6f) * 2.2f else 0f
                r += sunDisk * 0.98f
                g += sunDisk * 0.56f
                b += sunDisk * 0.22f
                if (ny > -0.16f && ny < 0.25f) {
                  val h = 1f - abs(ny) / 0.25f
                  r += h * 0.48f
                  g += h * 0.22f
                  b += h * 0.10f
                } else if (ny <= -0.16f) {
                  r *= 0.45f; g *= 0.45f; b *= 0.55f
                }
              }
              Pev3dLightingPreset.NEON -> {
                r = 0.06f + 0.08f * roughnessBlur
                g = 0.05f + 0.07f * roughnessBlur
                b = 0.14f + 0.12f * roughnessBlur
                if (ny > 0.24f - 0.32f * roughnessBlur) {
                  val soft = 0.16f * roughnessBlur
                  val w = 1f - roughnessBlur * 0.62f
                  if (nx < (-0.12f + soft) && nx > (-0.78f - soft)) {
                    r += 0.92f * w; g += 0.18f * w; b += 0.72f * w
                  }
                  if (nx > (0.12f - soft) && nx < (0.78f + soft)) {
                    r += 0.12f * w; g += 0.92f * w; b += 0.98f * w
                  }
                } else if (ny > -0.18f) {
                  val horiz = 1f - abs(ny) / 0.32f
                  r += horiz * 0.22f
                  g += horiz * 0.34f
                  b += horiz * 0.56f
                } else {
                  r = 0.03f; g = 0.03f; b = 0.08f
                }
              }
            }
            faceBuf.put((r.coerceIn(0f, 1f) * 255f).toInt().toByte())
            faceBuf.put((g.coerceIn(0f, 1f) * 255f).toInt().toByte())
            faceBuf.put((b.coerceIn(0f, 1f) * 255f).toInt().toByte())
            faceBuf.put(255.toByte())
          }
        }
        faceBuf.flip()
        tex.setImage(
          eng,
          level,
          0,
          0,
          face,
          size,
          size,
          1,
          Texture.PixelBufferDescriptor(faceBuf, Texture.Format.RGBA, Texture.Type.UBYTE),
        )
      }
    }
    return tex
  }

  private fun createCycloramaSkyboxTexture(
    eng: Engine,
    size: Int,
    preset: Pev3dLightingPreset,
  ): Texture {
    val tex = Texture.Builder()
      .width(size)
      .height(size)
      .levels(1)
      .sampler(Texture.Sampler.SAMPLER_CUBEMAP)
      .format(Texture.InternalFormat.RGBA8)
      .build(eng)

    val faceBytes = size * size * 4
    for (face in 0 until 6) {
      val faceBuf = ByteBuffer.allocateDirect(faceBytes).order(ByteOrder.LITTLE_ENDIAN)
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
          val horizonBand = (1f - abs(ny + 0.05f) * 2.2f).coerceIn(0f, 1f)
          val zenith = ny.coerceIn(0f, 1f)

          val (r, g, b) = when (preset) {
            Pev3dLightingPreset.STUDIO -> {
              Triple(
                0.055f + 0.088f * horizonBand + 0.025f * zenith,
                0.060f + 0.092f * horizonBand + 0.026f * zenith,
                0.070f + 0.100f * horizonBand + 0.030f * zenith,
              )
            }
            Pev3dLightingPreset.SUNSET -> {
              Triple(
                0.085f + 0.21f * horizonBand + 0.03f * zenith,
                0.055f + 0.105f * horizonBand + 0.025f * zenith,
                0.068f + 0.045f * horizonBand + 0.055f * zenith,
              )
            }
            Pev3dLightingPreset.NEON -> {
              Triple(
                0.035f + 0.055f * horizonBand + 0.02f * zenith,
                0.038f + 0.068f * horizonBand + 0.02f * zenith,
                0.078f + 0.115f * horizonBand + 0.04f * zenith,
              )
            }
          }
          faceBuf.put((r.coerceIn(0f, 1f) * 255f).toInt().toByte())
          faceBuf.put((g.coerceIn(0f, 1f) * 255f).toInt().toByte())
          faceBuf.put((b.coerceIn(0f, 1f) * 255f).toInt().toByte())
          faceBuf.put(255.toByte())
        }
      }
      faceBuf.flip()
      tex.setImage(
        eng,
        0,
        0,
        0,
        face,
        size,
        size,
        1,
        Texture.PixelBufferDescriptor(faceBuf, Texture.Format.RGBA, Texture.Type.UBYTE),
      )
    }
    return tex
  }

  private fun listPresentGlbFiles(): Set<String> {
    return try {
      appContext.assets.list("models")?.filter { it.endsWith(".glb", ignoreCase = true) }?.toSet() ?: emptySet()
    } catch (_: Throwable) {
      emptySet()
    }
  }

  private fun classifyNode(nodeNameRaw: String): Triple<String, GlbPartRole, Boolean> {
    val n = nodeNameRaw.lowercase()
    val isWheel = n.contains("wheel") || n.contains("tire") || n.contains("tyre") || n.contains("rim") || n.contains("spoke") || n.contains("rotor")
    return when {
      n.contains("signal") && n.contains("left") -> Triple("cockpit", GlbPartRole.SIGNAL_LEFT, false)
      n.contains("signal") && n.contains("right") -> Triple("cockpit", GlbPartRole.SIGNAL_RIGHT, false)
      n.contains("headlight") || n.contains("head_light") || n.contains("lamp") || n.contains("drl") -> Triple("cockpit", GlbPartRole.HEADLIGHT, false)
      n.contains("taillight") || n.contains("tail_light") || n.contains("brake_light") -> Triple("frame", GlbPartRole.TAILLIGHT, false)
      n.contains("caliper") -> Triple("brakes", GlbPartRole.CALIPER_ACCENT, false)
      n.contains("brake") || n.contains("rotor") || n.contains("disc") -> Triple("brakes", GlbPartRole.GENERIC_METAL, isWheel)
      n.contains("fork") || n.contains("shock") || n.contains("susp") || n.contains("spring") || n.contains("stanchion") -> Triple("suspension", GlbPartRole.SUSPENSION_ACCENT, false)
      n.contains("battery") || n.contains("pack") || n.contains("cell") -> Triple("battery", GlbPartRole.BATTERY, false)
      n.contains("controller") || n.contains("inverter") || n.contains("esc") || n.contains("foc") -> Triple("controller", GlbPartRole.CONTROLLER, false)
      n.contains("motor") || n.contains("hub") || n.contains("stator") || n.contains("drive") -> Triple("motor", GlbPartRole.MOTOR, isWheel)
      n.contains("tire") || n.contains("tyre") || n.contains("rubber") -> Triple("motor", GlbPartRole.TIRE_RUBBER, true)
      n.contains("cockpit") || n.contains("dash") || n.contains("tft") || n.contains("screen") || n.contains("bar") || n.contains("handle") || n.contains("stem") -> Triple("cockpit", GlbPartRole.COCKPIT, false)
      n.contains("seat") || n.contains("saddle") || n.contains("pad") || n.contains("grip") || n.contains("fender") -> Triple("frame", GlbPartRole.SECONDARY_TRIM, false)
      n.contains("frame") || n.contains("body") || n.contains("fairing") || n.contains("shroud") || n.contains("tank") || n.contains("chassis") || n.contains("deck") || n.contains("shell") || n.contains("hull") -> Triple("frame", GlbPartRole.BODY_PAINT, false)
      else -> Triple("frame", GlbPartRole.BODY_PAINT, isWheel)
    }
  }

  private fun srgbToLinear(c: Float): Float = c.coerceIn(0f, 1f).toDouble().pow(2.2).toFloat()

  /**
   * Loads the real .glb file mapped to the current archetype from app/src/main/assets/models/.
   * Never falls back to procedural Minecraft-style boxes if the .glb is missing.
   */
  private fun loadVehicleGlbIfNeeded(eng: Engine, scn: Scene) {
    val fileName = Pev3dEngine.glbAssetFileName(state.archetype)
    val glbKey = "${state.archetype}|$fileName"
    if (glbKey == loadedGlbKey) return
    loadedGlbKey = glbKey
    lastStyleKey = ""

    vehicleAsset?.let { old ->
      scn.removeEntities(old.entities)
      assetLoader?.destroyAsset(old)
      vehicleAsset = null
    }
    nodeBindings = emptyList()
    state.onWireframeReady?.invoke(emptyList())

    val glbPath = "models/$fileName"
    val glbBuffer = readAssetBytes(appContext.assets, glbPath)
    if (glbBuffer == null) {
      state.onAssetStateChanged?.invoke(
        Pev3dAssetLoadState.MissingGlb(
          expectedFileName = fileName,
          expectedAssetPath = "app/src/main/assets/models/$fileName",
          presentGlbFiles = listPresentGlbFiles(),
          envSource = activeEnvSource,
        )
      )
      return
    }

    try {
      val asset = assetLoader?.createAsset(glbBuffer)
      if (asset == null) {
        state.onAssetStateChanged?.invoke(
          Pev3dAssetLoadState.MissingGlb(
            expectedFileName = fileName,
            expectedAssetPath = "app/src/main/assets/models/$fileName",
            presentGlbFiles = listPresentGlbFiles(),
            envSource = activeEnvSource,
          )
        )
        return
      }
      resourceLoader?.loadResources(asset)
      asset.releaseSourceData()

      // Normalize bounding box to center cleanly on the showroom floor at y = -0.78f
      val box = asset.boundingBox
      val center = box.center
      val half = box.halfExtent
      val maxExtent = max(half[0] * 2f, max(half[1] * 2f, half[2] * 2f)).coerceAtLeast(0.001f)
      val normScale = 2.15f / maxExtent
      val baseMat = FloatArray(16)
      Mat4Math.setIdentity(baseMat)
      val bottomY = (center[1] - half[1]) * normScale
      Mat4Math.translate(baseMat, -center[0] * normScale, -0.78f - bottomY, -center[2] * normScale)
      Mat4Math.scale(baseMat, normScale, normScale, normScale)
      vehicleBaseTransform = baseMat

      val tm = eng.transformManager
      val rootTi = tm.getInstance(asset.root)
      if (rootTi != 0) {
        tm.setTransform(rootTi, baseMat)
      }

      val rm = eng.renderableManager
      val hsMap = Pev3dEngine.hotspotsForArchetype(state.archetype).associateBy { it.partKey }
      val bindings = ArrayList<GlbNodeBinding>()
      for (ent in asset.entities) {
        val ri = rm.getInstance(ent)
        if (ri != 0) {
          rm.setCastShadows(ri, true)
          rm.setReceiveShadows(ri, true)
        }
        val name = asset.getName(ent) ?: ""
        val (partKey, role, isWheel) = classifyNode(name)
        val localMat = FloatArray(16)
        val ti = tm.getInstance(ent)
        if (ti != 0) {
          tm.getTransform(ti, localMat)
        } else {
          Mat4Math.setIdentity(localMat)
        }
        val expDir = hsMap[partKey]?.explodeDir ?: Vec3(0f, 0.15f, 0f)
        bindings += GlbNodeBinding(ent, partKey, role, localMat, isWheel, expDir)
      }
      nodeBindings = bindings
      scn.addEntities(asset.entities)
      vehicleAsset = asset
      state.onAssetStateChanged?.invoke(Pev3dAssetLoadState.Loaded(fileName, activeEnvSource))
    } catch (_: Throwable) {
      state.onAssetStateChanged?.invoke(
        Pev3dAssetLoadState.MissingGlb(
          expectedFileName = fileName,
          expectedAssetPath = "app/src/main/assets/models/$fileName",
          presentGlbFiles = listPresentGlbFiles(),
          envSource = activeEnvSource,
        )
      )
    }
  }

  /**
   * Dynamically applies per-model PBR MaterialInstance overrides for all 58 PEV profiles
   * (frame metallic/clearcoat paint, Carbon Gloss mode, anodized fork/caliper accents, X-Ray & CAD Wire).
   */
  private fun applyPerModelMaterialOverridesIfNeeded(eng: Engine) {
    val asset = vehicleAsset ?: return
    val styleKey = "${state.modelName}|${state.modelVoltage}|${state.renderMode}|${state.lightsOn}"
    if (styleKey == lastStyleKey) return
    lastStyleKey = styleKey

    val finish = Pev3dEngine.finishForModel(state.modelName, state.modelCategory, state.modelVoltage)
    val isCarbonGloss = state.renderMode == Pev3dRenderMode.CARBON_SHOWROOM
    val isXray = state.renderMode == Pev3dRenderMode.XRAY
    val isWire = state.renderMode == Pev3dRenderMode.WIREFRAME

    val rm = eng.renderableManager
    for (binding in nodeBindings) {
      val ri = rm.getInstance(binding.entity)
      if (ri == 0) continue
      val primCount = rm.getPrimitiveCount(ri)
      for (p in 0 until primCount) {
        val mi = rm.getMaterialInstanceAt(ri, p)
        val matName = mi.name.lowercase()
        val effectiveRole =
          if (binding.role == GlbPartRole.BODY_PAINT && matName.isNotBlank()) {
            classifyNode(matName).second
          } else {
            binding.role
          }

        try {
          when (effectiveRole) {
            GlbPartRole.BODY_PAINT -> {
              if (isCarbonGloss) {
                mi.setParameter("baseColorFactor", 0.045f, 0.050f, 0.062f, if (isXray) 0.25f else 1f)
                mi.setParameter("metallicFactor", 0.35f)
                mi.setParameter("roughnessFactor", 0.14f)
              } else if (isXray) {
                mi.setParameter("baseColorFactor", 0.08f, 0.32f, 0.56f, 0.24f)
                mi.setParameter("metallicFactor", 0.12f)
                mi.setParameter("roughnessFactor", 0.28f)
              } else if (isWire) {
                mi.setParameter("baseColorFactor", 0.06f, 0.08f, 0.11f, 1f)
                mi.setParameter("metallicFactor", 0.20f)
                mi.setParameter("roughnessFactor", 0.65f)
              } else {
                mi.setParameter(
                  "baseColorFactor",
                  srgbToLinear(finish.bodyRgb[0]),
                  srgbToLinear(finish.bodyRgb[1]),
                  srgbToLinear(finish.bodyRgb[2]),
                  1f,
                )
                mi.setParameter("metallicFactor", finish.paintMetallic)
                mi.setParameter("roughnessFactor", 0.22f)
              }
            }
            GlbPartRole.SUSPENSION_ACCENT -> {
              mi.setParameter(
                "baseColorFactor",
                srgbToLinear(finish.accentRgb[0]),
                srgbToLinear(finish.accentRgb[1]),
                srgbToLinear(finish.accentRgb[2]),
                1f,
              )
              mi.setParameter("metallicFactor", 0.90f)
              mi.setParameter("roughnessFactor", 0.20f)
            }
            GlbPartRole.CALIPER_ACCENT -> {
              mi.setParameter(
                "baseColorFactor",
                srgbToLinear(finish.caliperRgb[0]),
                srgbToLinear(finish.caliperRgb[1]),
                srgbToLinear(finish.caliperRgb[2]),
                1f,
              )
              mi.setParameter("metallicFactor", 0.80f)
              mi.setParameter("roughnessFactor", 0.24f)
            }
            GlbPartRole.SECONDARY_TRIM -> {
              mi.setParameter(
                "baseColorFactor",
                srgbToLinear(finish.secondaryRgb[0]),
                srgbToLinear(finish.secondaryRgb[1]),
                srgbToLinear(finish.secondaryRgb[2]),
                1f,
              )
              mi.setParameter("metallicFactor", 0.12f)
              mi.setParameter("roughnessFactor", 0.68f)
            }
            else -> Unit
          }
          if (isCarbonGloss && effectiveRole == GlbPartRole.BODY_PAINT) {
            try {
              mi.setParameter("clearcoatFactor", 1.0f)
              mi.setParameter("clearcoatRoughnessFactor", 0.08f)
            } catch (_: Throwable) {
            }
          }
        } catch (_: Throwable) {
        }
      }
    }

    indirectLight?.intensity =
      when (state.renderMode) {
        Pev3dRenderMode.CARBON_SHOWROOM -> 46_000f
        Pev3dRenderMode.WIREFRAME -> 14_000f
        else -> 38_000f
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

    applyLightingPreset(eng, scn, state.lightingPreset)
    applySsaoIfNeeded(vw)
    loadVehicleGlbIfNeeded(eng, scn)
    applyPerModelMaterialOverridesIfNeeded(eng)

    // Camera orbital LookAt matching Pev3dEngine.buildCameraViewProjection
    val dist = (4.65f / state.zoom.coerceIn(0.6f, 2.4f)).coerceIn(1.8f, 8.5f)
    val yr = (state.yawDeg * PI / 180.0).toFloat()
    val pr = (state.pitchDeg * PI / 180.0).toFloat()
    val cosP = cos(pr)
    val sinP = sin(pr)
    val camX = (dist * cosP * sin(yr)).toDouble()
    val camY = (dist * sinP + 0.08f).toDouble()
    val camZ = (dist * cosP * cos(yr)).toDouble()
    cam.lookAt(camX, camY, camZ, 0.0, -0.05, 0.0, 0.0, 1.0, 0.0)

    val timeSec = ((frameTimeNanos - startNs) / 1_000_000_000.0).toFloat()
    val pulse = 0.5f + 0.5f * sin(timeSec * 4.5f)
    val flashWave = sin(timeSec * (1.8f * 2f * PI.toFloat()))
    val flashLevel = ((flashWave + 0.22f) * 3.8f).coerceIn(0f, 1f)
    val sigMode = state.turnSignalMode
    val leftActive = sigMode == Pev3dTurnSignalMode.LEFT || sigMode == Pev3dTurnSignalMode.HAZARD
    val rightActive = sigMode == Pev3dTurnSignalMode.RIGHT || sigMode == Pev3dTurnSignalMode.HAZARD
    val lightsEnabled = state.lightsOn && vehicleAsset != null
    val wireStealth = state.renderMode == Pev3dRenderMode.WIREFRAME
    val selKey = state.selectedPartKey
    val acc = state.accentRgb

    // Dynamic vehicle spot/point lights
    val lm = eng.lightManager
    val arch = state.archetype
    val hlX = when (arch) {
      Pev3dArchetype.EUC -> 0.42f
      Pev3dArchetype.HYDROFOIL -> 1.10f
      Pev3dArchetype.SCOOTER -> 0.72f
      else -> 0.68f
    }
    val hlY = when (arch) {
      Pev3dArchetype.EUC -> 0.28f
      Pev3dArchetype.HYDROFOIL -> 0.20f
      Pev3dArchetype.SCOOTER -> 0.66f
      else -> 0.44f
    }
    val tailX = when (arch) {
      Pev3dArchetype.EUC -> -0.44f
      Pev3dArchetype.HYDROFOIL -> -1.10f
      else -> -0.84f
    }
    val tailY = when (arch) {
      Pev3dArchetype.EUC -> 0.25f
      Pev3dArchetype.HYDROFOIL -> 0.16f
      Pev3dArchetype.SCOOTER -> -0.17f
      else -> 0.27f
    }
    lm.getInstance(headlightSpotEntity).takeIf { it != 0 }?.let { i ->
      lm.setPosition(i, hlX, hlY, 0f)
      lm.setIntensity(i, if (lightsEnabled && !wireStealth) 110_000f else 0f)
    }
    lm.getInstance(taillightPointEntity).takeIf { it != 0 }?.let { i ->
      lm.setPosition(i, tailX, tailY, 0f)
      lm.setIntensity(i, if (lightsEnabled && !wireStealth) 30_000f else 0f)
    }
    lm.getInstance(leftSignalPointEntity).takeIf { it != 0 }?.let { i ->
      lm.setPosition(i, hlX * 0.75f, hlY, -0.35f)
      lm.setIntensity(i, if (leftActive && lightsEnabled && !wireStealth) 32_000f * flashLevel else 0f)
    }
    lm.getInstance(rightSignalPointEntity).takeIf { it != 0 }?.let { i ->
      lm.setPosition(i, hlX * 0.75f, hlY, 0.35f)
      lm.setIntensity(i, if (rightActive && lightsEnabled && !wireStealth) 32_000f * flashLevel else 0f)
    }
    lm.getInstance(underglowPointEntity).takeIf { it != 0 }?.let { i ->
      lm.setColor(i, acc[0], acc[1], acc[2])
      val showUnderglow = state.lightingPreset == Pev3dLightingPreset.NEON && lightsEnabled && !wireStealth
      lm.setIntensity(i, if (showUnderglow) 14_000f else 0f)
    }

    // Animate loaded .glb nodes (wheel dyno spin + exploded teardown + LED & selected subsystem glow)
    if (vehicleAsset != null) {
      val tm = eng.transformManager
      val rm = eng.renderableManager
      val explode = state.explodePct
      val spinDeg = state.wheelSpinDeg
      val matBuf = FloatArray(16)
      val deltaBuf = FloatArray(16)

      for (binding in nodeBindings) {
        val ti = tm.getInstance(binding.entity)
        if (ti != 0 && (explode > 0.005f || binding.isWheelNode)) {
          Mat4Math.setIdentity(deltaBuf)
          if (explode > 0.005f) {
            Mat4Math.translate(
              deltaBuf,
              binding.explodeDir.x * explode,
              binding.explodeDir.y * explode,
              binding.explodeDir.z * explode,
            )
          }
          if (binding.isWheelNode && spinDeg != 0f) {
            Mat4Math.rotateAxis(deltaBuf, -spinDeg, 0f, 0f, 1f)
          }
          Mat4Math.multiplyMM(matBuf, binding.baseLocalMatrix, deltaBuf)
          tm.setTransform(ti, matBuf)
        }

        val ri = rm.getInstance(binding.entity)
        if (ri != 0) {
          val primCount = rm.getPrimitiveCount(ri)
          val isSelected = selKey != null && binding.partKey == selKey
          for (p in 0 until primCount) {
            val mi = rm.getMaterialInstanceAt(ri, p)
            try {
              when {
                wireStealth -> mi.setParameter("emissiveFactor", 0f, 0f, 0f)
                binding.role == GlbPartRole.HEADLIGHT -> {
                  if (lightsEnabled) mi.setParameter("emissiveFactor", 2.6f, 2.8f, 3.1f)
                  else mi.setParameter("emissiveFactor", 0.04f, 0.04f, 0.05f)
                }
                binding.role == GlbPartRole.TAILLIGHT -> {
                  if (lightsEnabled) mi.setParameter("emissiveFactor", 2.4f, 0.24f, 0.20f)
                  else mi.setParameter("emissiveFactor", 0.05f, 0.01f, 0.01f)
                }
                binding.role == GlbPartRole.SIGNAL_LEFT -> {
                  val lv = if (leftActive) 0.10f + 3.0f * flashLevel else 0.04f
                  mi.setParameter("emissiveFactor", lv, lv * 0.48f, lv * 0.03f)
                }
                binding.role == GlbPartRole.SIGNAL_RIGHT -> {
                  val rv = if (rightActive) 0.10f + 3.0f * flashLevel else 0.04f
                  mi.setParameter("emissiveFactor", rv, rv * 0.48f, rv * 0.03f)
                }
                isSelected -> {
                  val g = 0.22f + 0.42f * pulse
                  mi.setParameter("emissiveFactor", acc[0] * g, acc[1] * g, acc[2] * g)
                }
                else -> mi.setParameter("emissiveFactor", 0f, 0f, 0f)
              }
            } catch (_: Throwable) {
            }
          }
        }
      }
    }

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
    vehicleAsset?.let { a ->
      scn?.removeEntities(a.entities)
      assetLoader?.destroyAsset(a)
      vehicleAsset = null
    }
    floorAsset?.let { a ->
      scn?.removeEntities(a.entities)
      assetLoader?.destroyAsset(a)
      floorAsset = null
    }
    resourceLoader?.destroy()
    assetLoader?.destroy()
    ubershaderProvider?.destroy()
    swapChain?.let { eng.destroySwapChain(it) }
    indirectLight?.let { eng.destroyIndirectLight(it) }
    iblTexture?.let { eng.destroyTexture(it) }
    skybox?.let { eng.destroySkybox(it) }
    skyboxTexture?.let { eng.destroyTexture(it) }
    colorGrading?.let { eng.destroyColorGrading(it) }
    listOf(
      keyLightEntity,
      rimLightEntity,
      fillLightEntity,
      headlightSpotEntity,
      taillightPointEntity,
      leftSignalPointEntity,
      rightSignalPointEntity,
      underglowPointEntity,
    ).forEach { ent ->
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
internal fun Pev3dVulkanViewport(
  archetype: Pev3dArchetype,
  renderMode: Pev3dRenderMode,
  lightingPreset: Pev3dLightingPreset = Pev3dLightingPreset.STUDIO,
  modelName: String,
  modelCategory: String,
  modelVoltage: Int,
  yawDeg: Float,
  pitchDeg: Float,
  zoom: Float,
  explodePct: Float,
  wheelSpinDeg: Float,
  lightsOn: Boolean,
  ssaoEnabled: Boolean = true,
  turnSignalMode: Pev3dTurnSignalMode = Pev3dTurnSignalMode.OFF,
  selectedPartKey: String?,
  accentRgb: FloatArray,
  onBackendChanged: (String) -> Unit,
  onWireframeLinesReady: (List<PevWireGroup>) -> Unit,
  onAssetStateChanged: (Pev3dAssetLoadState) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val state = remember { Pev3dVulkanState() }
  state.archetype = archetype
  state.renderMode = renderMode
  state.lightingPreset = lightingPreset
  state.modelName = modelName
  state.modelCategory = modelCategory
  state.modelVoltage = modelVoltage
  state.yawDeg = yawDeg
  state.pitchDeg = pitchDeg
  state.zoom = zoom
  state.explodePct = explodePct
  state.wheelSpinDeg = wheelSpinDeg
  state.lightsOn = lightsOn
  state.ssaoEnabled = ssaoEnabled
  state.turnSignalMode = turnSignalMode
  state.selectedPartKey = selectedPartKey
  state.accentRgb = accentRgb
  state.onWireframeReady = onWireframeLinesReady
  state.onAssetStateChanged = onAssetStateChanged

  val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
  val host = remember {
    PevVulkanHost(
      appContext = context,
      state = state,
      onBackendReady = onBackendChanged,
    )
  }

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
