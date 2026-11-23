package com.example.ui.screens

import android.opengl.Matrix
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.google.android.filament.utils.Utils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal enum class Pev3dLightingPreset(val label: String) {
  STUDIO("Studio"),
  SUNSET("Sunset"),
  NEON("Neon Dyno"),
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
  @Volatile var turnSignalMode: Pev3dTurnSignalMode = Pev3dTurnSignalMode.OFF
  @Volatile var selectedPartKey: String? = "battery"
  @Volatile var accentRgb: FloatArray = floatArrayOf(0.22f, 0.85f, 0.96f)
  @Volatile var onWireframeReady: ((List<PevWireGroup>) -> Unit)? = null
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

private class PevVulkanHost(
  private val state: Pev3dVulkanState,
  private val onBackendReady: (String) -> Unit,
  private val onFatalFallback: () -> Unit,
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
  private var indirectLight: IndirectLight? = null
  private var skybox: Skybox? = null
  private var colorGrading: ColorGrading? = null
  private var swapChain: SwapChain? = null
  private var appliedLightingPreset: Pev3dLightingPreset? = null

  private var ubershaderProvider: UbershaderProvider? = null
  private var assetLoader: AssetLoader? = null
  private var resourceLoader: ResourceLoader? = null
  private var currentAsset: FilamentAsset? = null
  private var currentBuilt: PevBuiltGlb? = null
  private var loadedBuildKey: String = ""

  private data class NodeAnimInfo(
    val entity: Int,
    val partKey: String,
    val nodeKey: String,
    val axleCenter: Vec3?,
    val explodeDir: Vec3,
  )

  private var animNodes: List<NodeAnimInfo> = emptyList()
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
      val eng =
        try {
          val vk = Engine.Builder().backend(Engine.Backend.VULKAN).build()
          onBackendReady("VULKAN PBR")
          vk
        } catch (_: Throwable) {
          val gl = Engine.Builder().backend(Engine.Backend.OPENGL).build()
          onBackendReady("FILAMENT GL")
          gl
        }
      engine = eng
      renderer = eng.createRenderer()
      scene = eng.createScene()
      view = eng.createView()

      val em = EntityManager.get()
      cameraEntity = em.create()
      camera = eng.createCamera(cameraEntity)

      setupEnvironmentAndLights(eng, scene!!, view!!)
      view!!.scene = scene
      view!!.camera = camera

      ubershaderProvider = UbershaderProvider(eng)
      assetLoader = AssetLoader(eng, ubershaderProvider!!, em)
      resourceLoader = ResourceLoader(eng)

      val helper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
      helper.renderCallback = object : UiHelper.RendererCallback {
        override fun onNativeWindowChanged(surface: Surface) {
          try {
            swapChain?.let { eng.destroySwapChain(it) }
            swapChain = eng.createSwapChain(surface)
          } catch (_: Throwable) {
            destroy()
            onFatalFallback()
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
    } catch (_: Throwable) {
      destroy()
      onFatalFallback()
    }
  }

  private fun setupEnvironmentAndLights(eng: Engine, scn: Scene, vw: View) {
    val em = EntityManager.get()
    val shadowCfg = LightManager.ShadowOptions().apply {
      mapSize = 2048
      shadowCascades = 2
    }
    keyLightEntity = em.create()
    LightManager.Builder(LightManager.Type.DIRECTIONAL)
      .color(1.0f, 0.96f, 0.90f)
      .intensity(120_000f)
      .direction(-0.46f, -0.80f, -0.38f)
      .castShadows(true)
      .shadowOptions(shadowCfg)
      .build(eng, keyLightEntity)
    scn.addEntity(keyLightEntity)

    rimLightEntity = em.create()
    LightManager.Builder(LightManager.Type.DIRECTIONAL)
      .color(0.32f, 0.78f, 1.0f)
      .intensity(48_000f)
      .direction(0.75f, -0.25f, 0.61f)
      .castShadows(false)
      .build(eng, rimLightEntity)
    scn.addEntity(rimLightEntity)

    fillLightEntity = em.create()
    LightManager.Builder(LightManager.Type.DIRECTIONAL)
      .color(0.95f, 0.72f, 0.56f)
      .intensity(30_000f)
      .direction(-0.42f, -0.20f, 0.88f)
      .castShadows(false)
      .build(eng, fillLightEntity)
    scn.addEntity(fillLightEntity)

    // Physical Vehicle Light Emitters (Headlight Spot Beam + Taillight Crimson Glow + Dynamic Turn Signals + Underglow)
    headlightSpotEntity = em.create()
    LightManager.Builder(LightManager.Type.SPOT)
      .color(0.94f, 0.98f, 1.0f)
      .intensity(95_000f)
      .position(0.70f, 0.48f, 0f)
      .direction(0.92f, -0.39f, 0f)
      .falloff(6.5f)
      .spotLightCone(0.28f, 0.62f)
      .castShadows(false)
      .build(eng, headlightSpotEntity)
    scn.addEntity(headlightSpotEntity)

    taillightPointEntity = em.create()
    LightManager.Builder(LightManager.Type.POINT)
      .color(1.0f, 0.14f, 0.10f)
      .intensity(28_000f)
      .position(-0.82f, 0.25f, 0f)
      .falloff(2.8f)
      .build(eng, taillightPointEntity)
    scn.addEntity(taillightPointEntity)

    leftSignalPointEntity = em.create()
    LightManager.Builder(LightManager.Type.POINT)
      .color(1.0f, 0.55f, 0.04f)
      .intensity(0f)
      .position(0.65f, 0.48f, -0.32f)
      .falloff(2.4f)
      .build(eng, leftSignalPointEntity)
    scn.addEntity(leftSignalPointEntity)

    rightSignalPointEntity = em.create()
    LightManager.Builder(LightManager.Type.POINT)
      .color(1.0f, 0.55f, 0.04f)
      .intensity(0f)
      .position(0.65f, 0.48f, 0.32f)
      .falloff(2.4f)
      .build(eng, rightSignalPointEntity)
    scn.addEntity(rightSignalPointEntity)

    underglowPointEntity = em.create()
    LightManager.Builder(LightManager.Type.POINT)
      .color(0.22f, 0.85f, 0.96f)
      .intensity(18_000f)
      .position(0f, -0.45f, 0f)
      .falloff(2.6f)
      .build(eng, underglowPointEntity)
    scn.addEntity(underglowPointEntity)

    applyLightingPreset(eng, scn, state.lightingPreset)

    // Unreal Engine 5-Caliber Post Processing: ACES Filmic, MSAA 4x + FXAA, High SSAO, HDR Bloom & Vignette
    colorGrading = ColorGrading.Builder()
      .toneMapping(ColorGrading.ToneMapping.ACES)
      .build(eng)
    vw.colorGrading = colorGrading
    vw.sampleCount = 4
    vw.antiAliasing = View.AntiAliasing.FXAA
    vw.dithering = View.Dithering.TEMPORAL
    vw.setShadowingEnabled(true)
    vw.bloomOptions = View.BloomOptions().apply {
      enabled = true
      strength = 0.26f
      levels = 6
      threshold = true
    }
    vw.ambientOcclusionOptions = View.AmbientOcclusionOptions().apply {
      enabled = true
      radius = 0.30f
      intensity = 1.40f
      quality = View.QualityLevel.HIGH
    }
    vw.vignetteOptions = View.VignetteOptions().apply {
      enabled = true
      midPoint = 0.52f
      roundness = 0.65f
      feather = 0.45f
    }
  }

  private fun applyLightingPreset(eng: Engine, scn: Scene, preset: Pev3dLightingPreset) {
    if (appliedLightingPreset == preset) return
    appliedLightingPreset = preset

    indirectLight?.let { eng.destroyIndirectLight(it) }
    iblTexture?.let { eng.destroyTexture(it) }
    skybox?.let { eng.destroySkybox(it) }

    val cubemap = createStudioCubemapTexture(eng, 128, preset)
    iblTexture = cubemap
    val shBands =
      when (preset) {
        Pev3dLightingPreset.STUDIO -> floatArrayOf(
          0.35f, 0.38f, 0.45f,
          0.14f, 0.16f, 0.21f,
          0.25f, 0.27f, 0.30f,
          -0.06f, -0.05f, -0.04f,
          0.04f, 0.05f, 0.06f,
          0.03f, 0.04f, 0.06f,
          0.08f, 0.09f, 0.11f,
          -0.05f, -0.04f, -0.02f,
          0.03f, 0.05f, 0.08f,
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
      .intensity(36_000f)
      .build(eng)
    scn.indirectLight = indirectLight

    skybox =
      when (preset) {
        Pev3dLightingPreset.STUDIO -> Skybox.Builder().color(0.025f, 0.048f, 0.098f, 1.0f).build(eng)
        Pev3dLightingPreset.SUNSET -> Skybox.Builder().color(0.078f, 0.042f, 0.035f, 1.0f).build(eng)
        Pev3dLightingPreset.NEON -> Skybox.Builder().color(0.022f, 0.016f, 0.064f, 1.0f).build(eng)
      }
    scn.skybox = skybox

    val lm = eng.lightManager
    val keyI = lm.getInstance(keyLightEntity)
    val rimI = lm.getInstance(rimLightEntity)
    val fillI = lm.getInstance(fillLightEntity)
    when (preset) {
      Pev3dLightingPreset.STUDIO -> {
        if (keyI != 0) {
          lm.setColor(keyI, 1.0f, 0.96f, 0.90f)
          lm.setIntensity(keyI, 120_000f)
          lm.setDirection(keyI, -0.46f, -0.80f, -0.38f)
        }
        if (rimI != 0) {
          lm.setColor(rimI, 0.32f, 0.78f, 1.0f)
          lm.setIntensity(rimI, 48_000f)
        }
        if (fillI != 0) {
          lm.setColor(fillI, 0.95f, 0.72f, 0.56f)
          lm.setIntensity(fillI, 30_000f)
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
    val levels = 8 // 128, 64, 32, 16, 8, 4, 2, 1
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
              0 -> Triple(1f, v, -u)   // +X
              1 -> Triple(-1f, v, u)   // -X
              2 -> Triple(u, 1f, -v)   // +Y
              3 -> Triple(u, -1f, v)   // -Y
              4 -> Triple(u, v, 1f)    // +Z
              else -> Triple(-u, v, -1f) // -Z
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
                r = 0.09f + 0.12f * roughnessBlur
                g = 0.12f + 0.15f * roughnessBlur
                b = 0.19f + 0.19f * roughnessBlur
                val skyFactor = ((ny + 0.25f) / 1.25f).coerceIn(0f, 1f)
                r += skyFactor * roughnessBlur * 0.22f
                g += skyFactor * roughnessBlur * 0.24f
                b += skyFactor * roughnessBlur * 0.28f
                if (ny > 0.28f - 0.35f * roughnessBlur) {
                  val softExpand = 0.18f * roughnessBlur
                  val inZ1 = nz > (0.14f - softExpand) && nz < (0.74f + softExpand)
                  val inZ2 = nz > (-0.74f - softExpand) && nz < (-0.14f + softExpand)
                  val inX = kotlin.math.abs(nx) < (0.64f + softExpand)
                  val sharpWeight = 1f - roughnessBlur * 0.65f
                  val boost = (if (inX && inZ1) 0.94f else 0f) + (if (inX && inZ2) 0.74f else 0f)
                  r += boost * sharpWeight * 0.85f
                  g += boost * sharpWeight * 0.88f
                  b += boost * sharpWeight * 0.94f
                } else if (ny > -0.18f) {
                  val horiz = 1f - kotlin.math.abs(ny) / 0.35f
                  r += horiz * 0.12f
                  g += horiz * 0.28f
                  b += horiz * 0.46f
                } else {
                  r = 0.04f + 0.05f * roughnessBlur
                  g = 0.06f + 0.07f * roughnessBlur
                  b = 0.11f + 0.10f * roughnessBlur
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
                  val h = 1f - kotlin.math.abs(ny) / 0.25f
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
                    // Neon Magenta tube
                    r += 0.92f * w; g += 0.18f * w; b += 0.72f * w
                  }
                  if (nx > (0.12f - soft) && nx < (0.78f + soft)) {
                    // Neon Cyan tube
                    r += 0.12f * w; g += 0.92f * w; b += 0.98f * w
                  }
                } else if (ny > -0.18f) {
                  val horiz = 1f - kotlin.math.abs(ny) / 0.32f
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

  private fun rebuildAssetIfNeeded(eng: Engine, scn: Scene) {
    val mode = state.renderMode
    val isXray = mode == Pev3dRenderMode.XRAY
    val isCarbonGloss = mode == Pev3dRenderMode.CARBON_SHOWROOM
    val key = "${state.archetype}|${state.modelName}|${state.modelVoltage}|${state.lightsOn}|$isXray|$isCarbonGloss"
    if (key == loadedBuildKey && currentAsset != null) return

    currentAsset?.let { old ->
      scn.removeEntities(old.entities)
      assetLoader?.destroyAsset(old)
      currentAsset = null
    }

    val finish = Pev3dEngine.finishForModel(state.modelName, state.modelCategory, state.modelVoltage)
    val built = Pev3dGltfBuilder.buildVehicleGlb(
      archetype = state.archetype,
      finish = finish,
      lightsOn = state.lightsOn,
      xrayShell = isXray,
      carbonGlossMode = isCarbonGloss,
    )
    currentBuilt = built
    state.onWireframeReady?.invoke(built.wireGroups)

    val asset = assetLoader?.createAsset(ByteBuffer.wrap(built.glbBytes)) ?: return
    resourceLoader?.loadResources(asset)
    asset.releaseSourceData()
    scn.addEntities(asset.entities)
    currentAsset = asset
    loadedBuildKey = key

    val parsedNodes = ArrayList<NodeAnimInfo>()
    for (ent in asset.entities) {
      val name = asset.getName(ent) ?: continue
      val parts = name.split("__part_")
      val nodeKey = parts.getOrNull(0) ?: name
      val partKey = parts.getOrNull(1) ?: nodeKey
      val axle = built.wheelAxleCenters[nodeKey]
      val expDir = built.nodeExplodeDirs[partKey] ?: built.nodeExplodeDirs[nodeKey] ?: Vec3(0f, 0f, 0f)
      parsedNodes += NodeAnimInfo(ent, partKey, nodeKey, axle, expDir)
    }
    animNodes = parsedNodes

    indirectLight?.intensity = if (isCarbonGloss) 46_000f else if (mode == Pev3dRenderMode.WIREFRAME) 11_000f else 36_000f
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
    rebuildAssetIfNeeded(eng, scn)

    // Update Camera LookAt matching Pev3dEngine.buildCameraViewProjection
    val dist = (4.65f / state.zoom.coerceIn(0.6f, 2.4f)).coerceIn(1.8f, 8.5f)
    val yr = (state.yawDeg * PI / 180.0).toFloat()
    val pr = (state.pitchDeg * PI / 180.0).toFloat()
    val cosP = cos(pr)
    val sinP = sin(pr)
    val camX = (dist * cosP * sin(yr)).toDouble()
    val camY = (dist * sinP + 0.08f).toDouble()
    val camZ = (dist * cosP * cos(yr)).toDouble()
    cam.lookAt(camX, camY, camZ, 0.0, -0.05, 0.0, 0.0, 1.0, 0.0)

    // Update Node Transforms (Exploded View + Wheel Spin) and Emissive Highlighting
    val tm = eng.transformManager
    val rm = eng.renderableManager
    val explode = state.explodePct
    val spinDeg = state.wheelSpinDeg
    val timeSec = ((frameTimeNanos - startNs) / 1_000_000_000.0).toFloat()
    val pulse = 0.5f + 0.5f * sin(timeSec * 4.5f)
    val flashWave = sin(timeSec * (1.8f * 2f * PI.toFloat()))
    val flashLevel = ((flashWave + 0.22f) * 3.8f).coerceIn(0f, 1f)
    val sigMode = state.turnSignalMode
    val leftActive = sigMode == Pev3dTurnSignalMode.LEFT || sigMode == Pev3dTurnSignalMode.HAZARD
    val rightActive = sigMode == Pev3dTurnSignalMode.RIGHT || sigMode == Pev3dTurnSignalMode.HAZARD
    val lightsEnabled = state.lightsOn
    val selKey = state.selectedPartKey
    val acc = state.accentRgb
    val wireStealth = state.renderMode == Pev3dRenderMode.WIREFRAME
    val matBuf = FloatArray(16)

    // Update dynamic physical Spot & Point Lights on the vehicle
    val lm = eng.lightManager
    val arch = state.archetype
    val hlX = when (arch) {
      Pev3dArchetype.EUC -> 0.42f
      Pev3dArchetype.HYDROFOIL -> 1.10f
      else -> 0.72f
    }
    val hlY = when (arch) {
      Pev3dArchetype.EUC -> 0.28f
      Pev3dArchetype.HYDROFOIL -> 0.20f
      Pev3dArchetype.SCOOTER -> 0.66f
      else -> 0.46f
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
      else -> 0.25f
    }
    lm.getInstance(headlightSpotEntity).takeIf { it != 0 }?.let { i ->
      lm.setPosition(i, hlX, hlY, 0f)
      lm.setIntensity(i, if (lightsEnabled && !wireStealth) 115_000f else 0f)
    }
    lm.getInstance(taillightPointEntity).takeIf { it != 0 }?.let { i ->
      lm.setPosition(i, tailX, tailY, 0f)
      lm.setIntensity(i, if (lightsEnabled && !wireStealth) 32_000f else 0f)
    }
    lm.getInstance(leftSignalPointEntity).takeIf { it != 0 }?.let { i ->
      lm.setPosition(i, hlX * 0.75f, hlY, -0.35f)
      lm.setIntensity(i, if (leftActive && !wireStealth) 34_000f * flashLevel else 0f)
    }
    lm.getInstance(rightSignalPointEntity).takeIf { it != 0 }?.let { i ->
      lm.setPosition(i, hlX * 0.75f, hlY, 0.35f)
      lm.setIntensity(i, if (rightActive && !wireStealth) 34_000f * flashLevel else 0f)
    }
    lm.getInstance(underglowPointEntity).takeIf { it != 0 }?.let { i ->
      lm.setColor(i, acc[0], acc[1], acc[2])
      lm.setIntensity(i, if (lightsEnabled && !wireStealth) 22_000f else 0f)
    }

    for (info in animNodes) {
      val ti = tm.getInstance(info.entity)
      if (ti != 0) {
        Matrix.setIdentityM(matBuf, 0)
        if (explode > 0.005f && info.partKey != "floor") {
          Matrix.translateM(
            matBuf,
            0,
            info.explodeDir.x * explode,
            info.explodeDir.y * explode,
            info.explodeDir.z * explode,
          )
        }
        info.axleCenter?.let { c ->
          Matrix.translateM(matBuf, 0, c.x, c.y, c.z)
          Matrix.rotateM(matBuf, 0, -spinDeg, 0f, 0f, 1f)
          Matrix.translateM(matBuf, 0, -c.x, -c.y, -c.z)
        }
        tm.setTransform(ti, matBuf)
      }

      val ri = rm.getInstance(info.entity)
      if (ri != 0) {
        val primCount = rm.getPrimitiveCount(ri)
        val isSelected = selKey != null && info.partKey == selKey && info.partKey != "floor"
        for (p in 0 until primCount) {
          val mi = rm.getMaterialInstanceAt(ri, p)
          try {
            when {
              wireStealth -> mi.setParameter("emissiveFactor", 0f, 0f, 0f)
              info.nodeKey == "cockpit_light" -> {
                if (lightsEnabled) mi.setParameter("emissiveFactor", 2.6f, 2.8f, 3.1f)
                else mi.setParameter("emissiveFactor", 0.05f, 0.05f, 0.06f)
              }
              info.nodeKey == "frame_tail" -> {
                if (lightsEnabled) mi.setParameter("emissiveFactor", 2.4f, 0.26f, 0.22f)
                else mi.setParameter("emissiveFactor", 0.06f, 0.01f, 0.01f)
              }
              info.nodeKey == "signal_left_front" || info.nodeKey == "signal_left_rear" -> {
                val lv = if (leftActive) 0.10f + 3.2f * flashLevel else 0.06f
                mi.setParameter("emissiveFactor", lv, lv * 0.48f, lv * 0.03f)
              }
              info.nodeKey == "signal_right_front" || info.nodeKey == "signal_right_rear" -> {
                val rv = if (rightActive) 0.10f + 3.2f * flashLevel else 0.06f
                mi.setParameter("emissiveFactor", rv, rv * 0.48f, rv * 0.03f)
              }
              isSelected -> {
                val g = 0.25f + 0.45f * pulse
                mi.setParameter("emissiveFactor", acc[0] * g, acc[1] * g, acc[2] * g)
              }
              info.nodeKey != "cockpit_tft" && info.nodeKey != "floor_ring" -> {
                mi.setParameter("emissiveFactor", 0f, 0f, 0f)
              }
            }
          } catch (_: Throwable) {
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
    currentAsset?.let { a ->
      scn?.removeEntities(a.entities)
      assetLoader?.destroyAsset(a)
      currentAsset = null
    }
    resourceLoader?.destroy()
    assetLoader?.destroy()
    ubershaderProvider?.destroy()
    swapChain?.let { eng.destroySwapChain(it) }
    indirectLight?.let { eng.destroyIndirectLight(it) }
    iblTexture?.let { eng.destroyTexture(it) }
    skybox?.let { eng.destroySkybox(it) }
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
  turnSignalMode: Pev3dTurnSignalMode = Pev3dTurnSignalMode.OFF,
  selectedPartKey: String?,
  accentRgb: FloatArray,
  onBackendChanged: (String) -> Unit,
  onWireframeLinesReady: (List<PevWireGroup>) -> Unit,
  modifier: Modifier = Modifier,
) {
  var useLegacyGlFallback by remember { mutableStateOf(false) }

  if (useLegacyGlFallback) {
    Pev3dGlViewport(
      archetype = archetype,
      renderMode = renderMode,
      modelName = modelName,
      modelCategory = modelCategory,
      modelVoltage = modelVoltage,
      yawDeg = yawDeg,
      pitchDeg = pitchDeg,
      zoom = zoom,
      explodePct = explodePct,
      wheelSpinDeg = wheelSpinDeg,
      lightsOn = lightsOn,
      turnSignalMode = turnSignalMode,
      selectedPartKey = selectedPartKey,
      accentRgb = accentRgb,
      modifier = modifier,
    )
    return
  }

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
  state.turnSignalMode = turnSignalMode
  state.selectedPartKey = selectedPartKey
  state.accentRgb = accentRgb
  state.onWireframeReady = onWireframeLinesReady

  val host = remember {
    PevVulkanHost(
      state = state,
      onBackendReady = onBackendChanged,
      onFatalFallback = {
        onBackendChanged("GLES2 FALLBACK")
        useLegacyGlFallback = true
      },
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
