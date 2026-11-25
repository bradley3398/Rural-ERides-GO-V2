package com.example.ui.screens

import android.graphics.Color as AndroidColor
import android.view.Choreographer
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import com.google.android.filament.MaterialInstance
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * GOOGLE FILAMENT 1.56.0 OPENGL ES PBR ENGINE (ZERO VULKAN!)
 *
 * Loads high-resolution procedural glTF 2.0 (.glb) PEV rigs with embedded 512x512 Albedo,
 * Normal, MetallicRoughness, Occlusion textures + KHR_materials_clearcoat + MikkTSpace tangents
 * using Filament's explicit OpenGL ES backend (`Engine.Backend.OPENGL`).
 *
 * Features:
 * - Procedural Studio Softbox IBL Reflections Cubemap + 3-Band Spherical Harmonics IndirectLight
 * - 3-Point Studio LightManager Rig (Shadow-Casting Directional Key, Cool Fill, Silhouette Rim)
 * - ACES Filmic ColorGrading + FXAA + Temporal Dithering + Screen-Space Ambient Occlusion (SSAO)
 * - Live TransformManager wheel rotation & exploded CAD teardown
 * - Live MaterialInstance overrides for turn signals, part selection highlight & Thermal IR
 * - Graceful automatic fallback to Pev3dOpenGlViewport if native Filament cannot initialize
 */
private object FilamentNativeBootstrap {
  private val initialized = AtomicBoolean(false)
  @Volatile var available: Boolean = false
    private set

  fun ensureInitialized(): Boolean {
    if (initialized.get()) return available
    synchronized(this) {
      if (initialized.get()) return available
      available = runCatching {
        Filament.init()
        Gltfio.init()
        Utils.init()
        true
      }.getOrDefault(false)
      initialized.set(true)
      return available
    }
  }
}

private object PevFilamentGlbCache {
  private val cache = object : LinkedHashMap<String, PevBuiltGlb>(6, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, PevBuiltGlb>?): Boolean = size > 5
  }

  fun getOrBuild(
    archetype: Pev3dArchetype,
    finish: Pev3dModelFinish,
    lightsOn: Boolean,
    xrayShell: Boolean,
    carbonGlossMode: Boolean,
  ): PevBuiltGlb {
    val key = buildString {
      append(archetype.name).append('|')
      append(finish.blueprintType.name).append('|')
      append((finish.bodyRgb.getOrElse(0) { 0f } * 255).toInt()).append(',')
      append((finish.bodyRgb.getOrElse(1) { 0f } * 255).toInt()).append(',')
      append((finish.bodyRgb.getOrElse(2) { 0f } * 255).toInt()).append('|')
      append((finish.accentRgb.getOrElse(0) { 0f } * 255).toInt()).append(',')
      append((finish.accentRgb.getOrElse(1) { 0f } * 255).toInt()).append('|')
      append((finish.wheelScale * 100).toInt()).append('|')
      append((finish.bodyLengthScale * 100).toInt()).append('|')
      append(lightsOn).append('|').append(xrayShell).append('|').append(carbonGlossMode)
    }
    synchronized(cache) {
      return cache.getOrPut(key) {
        Pev3dGltfBuilder.buildVehicleGlb(
          archetype = archetype,
          finish = finish,
          lightsOn = lightsOn,
          xrayShell = xrayShell,
          carbonGlossMode = carbonGlossMode,
        )
      }
    }
  }
}

private data class FilamentPartMeta(
  val entity: Int,
  val nodeName: String,
  val partKey: String,
  val matInstance: MaterialInstance?,
  val origBaseColor: FloatArray,
  val origEmissive: FloatArray,
  val isFloor: Boolean,
  val isTurnSignalLeft: Boolean,
  val isTurnSignalRight: Boolean,
)

private class FilamentGlHost(
  private val textureView: TextureView,
  private val onBackendLabel: (String) -> Unit,
  private val onFatalInitError: () -> Unit,
) {
  private var engine: Engine? = null
  private var renderer: Renderer? = null
  private var scene: Scene? = null
  private var view: View? = null
  private var camera: Camera? = null
  private var cameraEntity: Int = 0
  private var swapChain: SwapChain? = null
  private var colorGrading: ColorGrading? = null
  private var materialProvider: UbershaderProvider? = null
  private var assetLoader: AssetLoader? = null
  private var resourceLoader: ResourceLoader? = null

  private var keyLightEntity: Int = 0
  private var fillLightEntity: Int = 0
  private var rimLightEntity: Int = 0
  private var indirectLight: IndirectLight? = null
  private var reflectionCubemap: Texture? = null
  private var skybox: Skybox? = null

  private var currentAsset: FilamentAsset? = null
  private var currentBuiltGlb: PevBuiltGlb? = null
  private var partMetas: List<FilamentPartMeta> = emptyList()
  private var appliedLightingPreset: Pev3dLightingPreset? = null

  private var uiHelper: UiHelper? = null
  private val choreographer = Choreographer.getInstance()
  private var isRunning = false
  private var isDestroyed = false
  private var viewportWidth = 1
  private var viewportHeight = 1
  private val scratchMat = FloatArray(16)

  // Live render parameters updated from Compose
  @Volatile var yawDeg: Float = -28f
  @Volatile var pitchDeg: Float = 16f
  @Volatile var zoom: Float = 1.05f
  @Volatile var explodePct: Float = 0f
  @Volatile var wheelSpinDeg: Float = 0f
  @Volatile var throttlePct: Float = 0f
  @Volatile var lightsOn: Boolean = true
  @Volatile var turnSignalMode: Pev3dTurnSignalMode = Pev3dTurnSignalMode.OFF
  @Volatile var selectedPartKey: String? = null
  @Volatile var renderMode: Pev3dRenderMode = Pev3dRenderMode.SOLID
  @Volatile var lightingPreset: Pev3dLightingPreset = Pev3dLightingPreset.STUDIO
  @Volatile var ambientTempC: Double = 22.0
  @Volatile var accentRgb: FloatArray = floatArrayOf(0.22f, 0.74f, 0.97f)

  private val frameCallback = object : Choreographer.FrameCallback {
    override fun doFrame(frameTimeNanos: Long) {
      if (!isRunning || isDestroyed) return
      choreographer.postFrameCallback(this)
      renderFrame(frameTimeNanos)
    }
  }

  fun initialize(): Boolean {
    return runCatching {
      // Explicitly select OpenGL ES backend (NO Vulkan!)
      val eng = Engine.Builder()
        .backend(Engine.Backend.OPENGL)
        .build()
      engine = eng
      val rend = eng.createRenderer()
      renderer = rend
      val scn = eng.createScene()
      scene = scn
      val vw = eng.createView()
      view = vw
      cameraEntity = EntityManager.get().create()
      val cam = eng.createCamera(cameraEntity)
      camera = cam

      vw.scene = scn
      vw.camera = cam
      vw.antiAliasing = View.AntiAliasing.FXAA
      vw.dithering = View.Dithering.TEMPORAL
      vw.setShadowingEnabled(true)
      vw.ambientOcclusionOptions = View.AmbientOcclusionOptions().apply {
        enabled = true
        radius = 0.24f
        intensity = 1.15f
        bias = 0.0008f
      }

      val cg = ColorGrading.Builder()
        .toneMapping(ColorGrading.ToneMapping.ACES)
        .contrast(1.07f)
        .saturation(1.08f)
        .exposure(0.18f)
        .build(eng)
      colorGrading = cg
      vw.colorGrading = cg

      val mp = UbershaderProvider(eng)
      materialProvider = mp
      assetLoader = AssetLoader(eng, mp, EntityManager.get())
      resourceLoader = ResourceLoader(eng, true)

      setupLightsAndEnvironment(eng, scn, lightingPreset)

      val helper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
      helper.isOpaque = true
      helper.renderCallback = object : UiHelper.RendererCallback {
        override fun onNativeWindowChanged(surface: Surface) {
          if (isDestroyed) return
          val e = engine ?: return
          swapChain?.let { runCatching { e.destroySwapChain(it) } }
          swapChain = runCatching { e.createSwapChain(surface) }.getOrNull()
        }

        override fun onDetachedFromSurface() {
          val e = engine ?: return
          swapChain?.let { sc ->
            runCatching { e.destroySwapChain(sc) }
            runCatching { e.flushAndWait() }
          }
          swapChain = null
        }

        override fun onResized(width: Int, height: Int) {
          viewportWidth = width.coerceAtLeast(1)
          viewportHeight = height.coerceAtLeast(1)
          view?.viewport = Viewport(0, 0, viewportWidth, viewportHeight)
          updateCameraProjection()
        }
      }
      helper.attachTo(textureView)
      uiHelper = helper

      onBackendLabel("Filament GL-PBR • ACES + IBL")
      startLoop()
      true
    }.getOrElse {
      destroy()
      onFatalInitError()
      false
    }
  }

  fun startLoop() {
    if (isDestroyed || isRunning) return
    isRunning = true
    choreographer.postFrameCallback(frameCallback)
  }

  fun pauseLoop() {
    isRunning = false
    choreographer.removeFrameCallback(frameCallback)
  }

  fun loadBuiltGlb(built: PevBuiltGlb) {
    if (isDestroyed) return
    val eng = engine ?: return
    val scn = scene ?: return
    val loader = assetLoader ?: return
    val resLoader = resourceLoader ?: return

    currentAsset?.let { old ->
      runCatching {
        scn.removeEntities(old.entities)
        loader.destroyAsset(old)
      }
    }
    currentAsset = null
    partMetas = emptyList()
    currentBuiltGlb = built

    runCatching {
      val directBuf = ByteBuffer.allocateDirect(built.glbBytes.size).order(ByteOrder.LITTLE_ENDIAN)
      directBuf.put(built.glbBytes)
      directBuf.flip()

      val asset = loader.createAsset(directBuf) ?: return@runCatching
      resLoader.loadResources(asset)
      asset.releaseSourceData()
      scn.addEntities(asset.entities)
      currentAsset = asset

      val rm = eng.renderableManager
      val metas = ArrayList<FilamentPartMeta>(asset.entities.size)
      for (entity in asset.entities) {
        val rawName = asset.getName(entity).orEmpty()
        val splitIdx = rawName.indexOf("__part_")
        val nodeName = if (splitIdx >= 0) rawName.substring(0, splitIdx) else rawName
        val partKey = if (splitIdx >= 0) rawName.substring(splitIdx + 7) else "frame"
        val isFloor = partKey == "floor" || nodeName.startsWith("floor")
        val ri = rm.getInstance(entity)
        val mi = if (ri != 0) {
          // Cast shadows from vehicle parts onto the floor; floor receives shadows cleanly
          rm.setCastShadows(ri, !isFloor)
          rm.setReceiveShadows(ri, true)
          runCatching { rm.getMaterialInstanceAt(ri, 0) }.getOrNull()
        } else {
          null
        }
        metas += FilamentPartMeta(
          entity = entity,
          nodeName = nodeName,
          partKey = partKey,
          matInstance = mi,
          origBaseColor = floatArrayOf(1f, 1f, 1f, 1f),
          origEmissive = floatArrayOf(0f, 0f, 0f),
          isFloor = isFloor,
          isTurnSignalLeft = nodeName == "signal_left_front" || nodeName == "signal_left_rear",
          isTurnSignalRight = nodeName == "signal_right_front" || nodeName == "signal_right_rear",
        )
      }
      partMetas = metas
      applyTransforms()
      applyMaterialOverrides(0f)
    }
  }

  private fun updateCameraProjection() {
    val cam = camera ?: return
    val aspect = viewportWidth.toDouble() / viewportHeight.coerceAtLeast(1).toDouble()
    cam.setProjection(38.0, aspect.coerceAtLeast(0.4), 0.25, 40.0, Camera.Fov.VERTICAL)
  }

  private fun updateCameraPose() {
    val cam = camera ?: return
    val dist = (4.65f / zoom.coerceIn(0.6f, 2.4f)).coerceIn(1.8f, 8.5f)
    val yr = (yawDeg * PI / 180.0)
    val pr = (pitchDeg * PI / 180.0)
    val cosP = cos(pr)
    val sinP = sin(pr)
    val camX = dist * cosP * sin(yr)
    val camY = dist * sinP + 0.08
    val camZ = dist * cosP * cos(yr)
    cam.lookAt(camX, camY, camZ, 0.0, -0.05, 0.0, 0.0, 1.0, 0.0)
  }

  private fun applyTransforms() {
    val eng = engine ?: return
    val built = currentBuiltGlb ?: return
    val tm = eng.transformManager
    val exp = explodePct.coerceIn(0f, 1f)
    val spin = wheelSpinDeg

    tm.openLocalTransformTransaction()
    try {
      for (meta in partMetas) {
        val ti = tm.getInstance(meta.entity)
        if (ti == 0) continue
        Mat4Math.setIdentity(scratchMat)
        if (!meta.isFloor && exp > 0.004f) {
          val dir = built.nodeExplodeDirs[meta.partKey] ?: built.nodeExplodeDirs[meta.nodeName]
          if (dir != null) {
            Mat4Math.translate(scratchMat, dir.x * exp, dir.y * exp, dir.z * exp)
          }
        }
        val axle = built.wheelAxleCenters[meta.nodeName]
        if (axle != null) {
          Mat4Math.translate(scratchMat, axle.x, axle.y, axle.z)
          Mat4Math.rotateAxis(scratchMat, -spin, 0f, 0f, 1f)
          Mat4Math.translate(scratchMat, -axle.x, -axle.y, -axle.z)
        }
        tm.setTransform(ti, scratchMat)
      }
    } finally {
      tm.commitLocalTransformTransaction()
    }
  }

  private fun applyMaterialOverrides(blinkPhase: Float) {
    val mode = renderMode
    val selKey = selectedPartKey
    val sig = turnSignalMode
    val throttle = throttlePct.coerceIn(0f, 1f)
    val tempNorm = ((ambientTempC.toFloat() - 10f) / 35f).coerceIn(0f, 1f)
    val blinkOn = blinkPhase > 0.45f
    val leftActive = (sig == Pev3dTurnSignalMode.LEFT || sig == Pev3dTurnSignalMode.HAZARD) && blinkOn
    val rightActive = (sig == Pev3dTurnSignalMode.RIGHT || sig == Pev3dTurnSignalMode.HAZARD) && blinkOn
    val acc = accentRgb

    for (meta in partMetas) {
      val mi = meta.matInstance ?: continue
      val mat = mi.material
      if (!mat.hasParameter("emissiveFactor")) continue

      var er = 0f
      var eg = 0f
      var eb = 0f

      when {
        meta.isTurnSignalLeft -> {
          if (leftActive) {
            er = 3.2f; eg = 1.65f; eb = 0.08f
          } else {
            er = 0.06f; eg = 0.03f; eb = 0.005f
          }
        }
        meta.isTurnSignalRight -> {
          if (rightActive) {
            er = 3.2f; eg = 1.65f; eb = 0.08f
          } else {
            er = 0.06f; eg = 0.03f; eb = 0.005f
          }
        }
        meta.nodeName == "cockpit_light" -> {
          if (lightsOn) {
            er = 2.6f; eg = 2.75f; eb = 3.0f
          } else {
            er = 0.04f; eg = 0.04f; eb = 0.05f
          }
        }
        meta.nodeName == "frame_tail" -> {
          if (lightsOn) {
            er = 2.4f; eg = 0.22f; eb = 0.18f
          } else {
            er = 0.05f; eg = 0.01f; eb = 0.01f
          }
        }
        meta.nodeName == "cockpit_tft" -> {
          er = 0.52f; eg = 0.62f; eb = 0.72f
        }
        mode == Pev3dRenderMode.THERMAL_IR && !meta.isFloor -> {
          val heat = when (meta.partKey) {
            "motor" -> (0.58f + throttle * 0.42f + tempNorm * 0.1f).coerceIn(0f, 1f)
            "controller" -> (0.48f + throttle * 0.44f + tempNorm * 0.1f).coerceIn(0f, 1f)
            "battery" -> (0.34f + throttle * 0.34f + tempNorm * 0.15f).coerceIn(0f, 1f)
            "brakes" -> (0.38f + throttle * 0.25f).coerceIn(0f, 1f)
            else -> 0.12f
          }
          er = (heat * 2.2f).coerceIn(0.05f, 2.2f)
          eg = ((1f - kotlin.math.abs(heat - 0.55f) * 2f).coerceAtLeast(0f) * 1.15f)
          eb = ((1f - heat) * 0.85f).coerceIn(0.05f, 0.9f)
        }
        mode == Pev3dRenderMode.BLUEPRINT && !meta.isFloor -> {
          er = 0.04f; eg = 0.22f; eb = 0.48f
        }
        mode == Pev3dRenderMode.WIREFRAME && !meta.isFloor -> {
          er = 0.02f; eg = 0.10f; eb = 0.16f
        }
        selKey != null && meta.partKey == selKey && !meta.isFloor -> {
          val pulse = 0.35f + 0.25f * sin(blinkPhase * PI.toFloat())
          er = acc.getOrElse(0) { 0.22f } * pulse
          eg = acc.getOrElse(1) { 0.74f } * pulse
          eb = acc.getOrElse(2) { 0.97f } * pulse
        }
      }
      runCatching { mi.setParameter("emissiveFactor", er, eg, eb) }
    }
  }

  private fun setupLightsAndEnvironment(eng: Engine, scn: Scene, preset: Pev3dLightingPreset) {
    appliedLightingPreset = preset

    // Clean up previous environment
    if (keyLightEntity != 0) {
      scn.removeEntity(keyLightEntity)
      eng.destroyEntity(keyLightEntity)
      EntityManager.get().destroy(keyLightEntity)
      keyLightEntity = 0
    }
    if (fillLightEntity != 0) {
      scn.removeEntity(fillLightEntity)
      eng.destroyEntity(fillLightEntity)
      EntityManager.get().destroy(fillLightEntity)
      fillLightEntity = 0
    }
    if (rimLightEntity != 0) {
      scn.removeEntity(rimLightEntity)
      eng.destroyEntity(rimLightEntity)
      EntityManager.get().destroy(rimLightEntity)
      rimLightEntity = 0
    }
    indirectLight?.let {
      scn.indirectLight = null
      eng.destroyIndirectLight(it)
      indirectLight = null
    }
    reflectionCubemap?.let {
      eng.destroyTexture(it)
      reflectionCubemap = null
    }
    skybox?.let {
      scn.skybox = null
      eng.destroySkybox(it)
      skybox = null
    }

    val em = EntityManager.get()
    keyLightEntity = em.create()
    fillLightEntity = em.create()
    rimLightEntity = em.create()

    val (keyCol, keyInt, fillCol, fillInt, rimCol, rimInt, skyCol, shIrradiance) = when (preset) {
      Pev3dLightingPreset.STUDIO -> StudioLightingConfig(
        keyCol = floatArrayOf(1.0f, 0.98f, 0.95f),
        keyInt = 112_000f,
        fillCol = floatArrayOf(0.76f, 0.87f, 1.0f),
        fillInt = 44_000f,
        rimCol = floatArrayOf(0.88f, 0.94f, 1.0f),
        rimInt = 66_000f,
        skyCol = floatArrayOf(0.055f, 0.068f, 0.095f, 1.0f),
        sh = floatArrayOf(
          0.74f, 0.78f, 0.86f,
          0.36f, 0.38f, 0.42f,
          0.16f, 0.18f, 0.22f,
          -0.18f, -0.16f, -0.14f,
          0.06f, 0.07f, 0.09f,
          0.04f, 0.05f, 0.06f,
          0.14f, 0.15f, 0.17f,
          -0.05f, -0.04f, -0.03f,
          0.08f, 0.09f, 0.11f,
        ),
      )
      Pev3dLightingPreset.SUNSET -> StudioLightingConfig(
        keyCol = floatArrayOf(1.0f, 0.72f, 0.42f),
        keyInt = 122_000f,
        fillCol = floatArrayOf(0.55f, 0.65f, 0.95f),
        fillInt = 38_000f,
        rimCol = floatArrayOf(1.0f, 0.56f, 0.26f),
        rimInt = 78_000f,
        skyCol = floatArrayOf(0.10f, 0.06f, 0.085f, 1.0f),
        sh = floatArrayOf(
          0.76f, 0.60f, 0.54f,
          0.32f, 0.24f, 0.28f,
          0.26f, 0.18f, 0.12f,
          -0.22f, -0.14f, -0.08f,
          0.08f, 0.05f, 0.04f,
          0.05f, 0.04f, 0.03f,
          0.12f, 0.09f, 0.08f,
          -0.06f, -0.04f, -0.02f,
          0.10f, 0.07f, 0.05f,
        ),
      )
      Pev3dLightingPreset.NEON -> StudioLightingConfig(
        keyCol = floatArrayOf(0.45f, 0.90f, 1.0f),
        keyInt = 104_000f,
        fillCol = floatArrayOf(0.88f, 0.34f, 0.96f),
        fillInt = 52_000f,
        rimCol = floatArrayOf(0.28f, 0.98f, 0.82f),
        rimInt = 82_000f,
        skyCol = floatArrayOf(0.035f, 0.045f, 0.095f, 1.0f),
        sh = floatArrayOf(
          0.54f, 0.66f, 0.88f,
          0.22f, 0.30f, 0.42f,
          0.20f, 0.12f, 0.30f,
          -0.24f, 0.10f, 0.22f,
          0.06f, 0.08f, 0.12f,
          0.04f, 0.06f, 0.10f,
          0.10f, 0.12f, 0.18f,
          -0.06f, 0.04f, 0.08f,
          0.08f, 0.10f, 0.16f,
        ),
      )
    }

    LightManager.Builder(LightManager.Type.DIRECTIONAL)
      .color(keyCol[0], keyCol[1], keyCol[2])
      .intensity(keyInt)
      .direction(-0.46f, -0.78f, -0.42f)
      .castShadows(true)
      .shadowOptions(
        LightManager.ShadowOptions().apply {
          mapSize = 1024
          constantBias = 0.002f
          normalBias = 1.2f
        },
      )
      .build(eng, keyLightEntity)
    scn.addEntity(keyLightEntity)

    LightManager.Builder(LightManager.Type.DIRECTIONAL)
      .color(fillCol[0], fillCol[1], fillCol[2])
      .intensity(fillInt)
      .direction(0.68f, -0.32f, 0.56f)
      .castShadows(false)
      .build(eng, fillLightEntity)
    scn.addEntity(fillLightEntity)

    LightManager.Builder(LightManager.Type.DIRECTIONAL)
      .color(rimCol[0], rimCol[1], rimCol[2])
      .intensity(rimInt)
      .direction(0.24f, -0.20f, 0.94f)
      .castShadows(false)
      .build(eng, rimLightEntity)
    scn.addEntity(rimLightEntity)

    // Procedural studio softbox reflections cubemap (gives realistic metallic/clearcoat specular highlights)
    val cubemap = buildStudioReflectionCubemap(eng, preset)
    reflectionCubemap = cubemap

    val iblBuilder = IndirectLight.Builder()
      .irradiance(3, shIrradiance)
      .intensity(34_000f)
    if (cubemap != null) {
      iblBuilder.reflections(cubemap)
    }
    indirectLight = runCatching { iblBuilder.build(eng) }.getOrElse {
      IndirectLight.Builder().irradiance(3, shIrradiance).intensity(34_000f).build(eng)
    }
    scn.indirectLight = indirectLight

    skybox = Skybox.Builder()
      .color(skyCol[0], skyCol[1], skyCol[2], skyCol[3])
      .build(eng)
    scn.skybox = skybox
  }

  private fun buildStudioReflectionCubemap(eng: Engine, preset: Pev3dLightingPreset): Texture? {
    return runCatching {
      val baseSize = 64
      val levels = 7 // Full mip chain: 64 -> 32 -> 16 -> 8 -> 4 -> 2 -> 1 (required by Filament IndirectLight)
      val tex = Texture.Builder()
        .width(baseSize)
        .height(baseSize)
        .levels(levels)
        .sampler(Texture.Sampler.SAMPLER_CUBEMAP)
        .format(Texture.InternalFormat.RGBA8)
        .build(eng)

      for (level in 0 until levels) {
        val faceSize = (baseSize shr level).coerceAtLeast(1)
        val roughness = level.toFloat() / (levels - 1).toFloat()
        val faceBytes = faceSize * faceSize * 4
        val allFaces = ByteBuffer.allocateDirect(faceBytes * 6).order(ByteOrder.nativeOrder())
        val offsets = IntArray(6) { it * faceBytes }

        for (face in 0 until 6) {
          for (y in 0 until faceSize) {
            val v = 2f * (y + 0.5f) / faceSize - 1f
            for (x in 0 until faceSize) {
              val u = 2f * (x + 0.5f) / faceSize - 1f
              val (dx, dy, dz) = cubemapDir(face, u, v)
              val invLen = 1f / sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(1e-5f)
              val nx = dx * invLen
              val ny = dy * invLen
              val nz = dz * invLen

              val (sr, sg, sb) = sampleStudioSoftboxEnv(nx, ny, nz, preset)
              // Blend toward soft studio ambient as mip roughness increases for realistic GGX IBL
              val ambR = 0.22f
              val ambG = 0.25f
              val ambB = 0.30f
              val blur = roughness * 0.72f
              val r = sr * (1f - blur) + ambR * blur
              val g = sg * (1f - blur) + ambG * blur
              val b = sb * (1f - blur) + ambB * blur
              allFaces.put((r.coerceIn(0f, 1f) * 255f).toInt().toByte())
              allFaces.put((g.coerceIn(0f, 1f) * 255f).toInt().toByte())
              allFaces.put((b.coerceIn(0f, 1f) * 255f).toInt().toByte())
              allFaces.put(0xFF.toByte())
            }
          }
        }
        allFaces.flip()
        val pbd = Texture.PixelBufferDescriptor(
          allFaces,
          Texture.Format.RGBA,
          Texture.Type.UBYTE,
        )
        tex.setImage(eng, level, pbd, offsets)
      }
      tex
    }.getOrNull()
  }

  private fun cubemapDir(face: Int, u: Float, v: Float): Triple<Float, Float, Float> =
    when (face) {
      0 -> Triple(1f, -v, -u)   // +X
      1 -> Triple(-1f, -v, u)   // -X
      2 -> Triple(u, 1f, v)     // +Y
      3 -> Triple(u, -1f, -v)   // -Y
      4 -> Triple(u, -v, 1f)    // +Z
      else -> Triple(-u, -v, -1f) // -Z
    }

  private fun sampleStudioSoftboxEnv(nx: Float, ny: Float, nz: Float, preset: Pev3dLightingPreset): Triple<Float, Float, Float> {
    // Ground bounce vs studio dome gradient
    val up = ((ny + 1f) * 0.5f).coerceIn(0f, 1f)
    var r = 0.08f + up * 0.14f
    var g = 0.09f + up * 0.16f
    var b = 0.12f + up * 0.22f

    // Overhead twin automotive studio softbox strips (gives authentic crisp clearcoat car/bike reflections!)
    val topSoftbox = if (ny > 0.58f && kotlin.math.abs(nz) < 0.42f) {
      ((ny - 0.58f) / 0.42f).pow(1.4f)
    } else 0f
    // Key side studio strip light
    val sideStrip = max(0f, nx * 0.65f + ny * 0.55f + nz * 0.52f).pow(6f)
    // Rim back studio strip light
    val rimStrip = max(0f, -nx * 0.45f + ny * 0.35f - nz * 0.82f).pow(5f)
    // Subtle horizon cyclorama glow band
    val horizonBand = (1f - kotlin.math.abs(ny)).pow(4f) * 0.28f

    when (preset) {
      Pev3dLightingPreset.STUDIO -> {
        r += topSoftbox * 0.92f + sideStrip * 0.88f + rimStrip * 0.62f + horizonBand * 0.72f
        g += topSoftbox * 0.94f + sideStrip * 0.90f + rimStrip * 0.72f + horizonBand * 0.78f
        b += topSoftbox * 1.00f + sideStrip * 0.92f + rimStrip * 0.88f + horizonBand * 0.90f
      }
      Pev3dLightingPreset.SUNSET -> {
        r += topSoftbox * 0.96f + sideStrip * 1.00f + rimStrip * 0.86f + horizonBand * 0.95f
        g += topSoftbox * 0.74f + sideStrip * 0.66f + rimStrip * 0.48f + horizonBand * 0.52f
        b += topSoftbox * 0.62f + sideStrip * 0.40f + rimStrip * 0.68f + horizonBand * 0.46f
      }
      Pev3dLightingPreset.NEON -> {
        r += topSoftbox * 0.42f + sideStrip * 0.32f + rimStrip * 0.92f + horizonBand * 0.52f
        g += topSoftbox * 0.86f + sideStrip * 0.94f + rimStrip * 0.28f + horizonBand * 0.62f
        b += topSoftbox * 1.00f + sideStrip * 1.00f + rimStrip * 0.98f + horizonBand * 0.96f
      }
    }
    if (ny < -0.05f) {
      // Dark reflective studio concrete floor under horizon
      val floorFade = ((-ny - 0.05f) / 0.95f).coerceIn(0f, 1f)
      val keep = 1f - floorFade * 0.78f
      r *= keep; g *= keep; b *= keep
    }
    return Triple(r, g, b)
  }

  private fun renderFrame(frameTimeNanos: Long) {
    val eng = engine ?: return
    val rend = renderer ?: return
    val vw = view ?: return
    val sc = swapChain ?: return
    val scn = scene ?: return
    if (uiHelper?.isReadyToRender != true) return

    if (appliedLightingPreset != lightingPreset) {
      setupLightsAndEnvironment(eng, scn, lightingPreset)
    }

    updateCameraPose()
    applyTransforms()
    val blinkPhase = ((frameTimeNanos / 1_000_000L) % 900L) / 900f
    applyMaterialOverrides(blinkPhase)

    runCatching {
      if (rend.beginFrame(sc, frameTimeNanos)) {
        rend.render(vw)
        rend.endFrame()
      }
    }
  }

  fun destroy() {
    if (isDestroyed) return
    isDestroyed = true
    pauseLoop()

    val eng = engine ?: return
    runCatching { uiHelper?.detach() }
    uiHelper = null

    runCatching {
      currentAsset?.let { asset ->
        scene?.removeEntities(asset.entities)
        assetLoader?.destroyAsset(asset)
      }
      currentAsset = null
    }
    runCatching { resourceLoader?.destroy() }
    runCatching { assetLoader?.destroy() }
    runCatching { materialProvider?.destroyMaterials() }
    runCatching { materialProvider?.destroy() }

    runCatching {
      if (keyLightEntity != 0) {
        scene?.removeEntity(keyLightEntity)
        eng.destroyEntity(keyLightEntity)
        EntityManager.get().destroy(keyLightEntity)
      }
      if (fillLightEntity != 0) {
        scene?.removeEntity(fillLightEntity)
        eng.destroyEntity(fillLightEntity)
        EntityManager.get().destroy(fillLightEntity)
      }
      if (rimLightEntity != 0) {
        scene?.removeEntity(rimLightEntity)
        eng.destroyEntity(rimLightEntity)
        EntityManager.get().destroy(rimLightEntity)
      }
    }
    runCatching { indirectLight?.let { eng.destroyIndirectLight(it) } }
    runCatching { reflectionCubemap?.let { eng.destroyTexture(it) } }
    runCatching { skybox?.let { eng.destroySkybox(it) } }
    runCatching { colorGrading?.let { eng.destroyColorGrading(it) } }
    runCatching { swapChain?.let { eng.destroySwapChain(it) } }
    runCatching { renderer?.let { eng.destroyRenderer(it) } }
    runCatching { view?.let { eng.destroyView(it) } }
    runCatching { scene?.let { eng.destroyScene(it) } }
    runCatching {
      if (cameraEntity != 0) {
        eng.destroyCameraComponent(cameraEntity)
        EntityManager.get().destroy(cameraEntity)
      }
    }
    runCatching { eng.destroy() }

    engine = null
    renderer = null
    scene = null
    view = null
    camera = null
    swapChain = null
    colorGrading = null
    assetLoader = null
    resourceLoader = null
    materialProvider = null
  }

  private data class StudioLightingConfig(
    val keyCol: FloatArray,
    val keyInt: Float,
    val fillCol: FloatArray,
    val fillInt: Float,
    val rimCol: FloatArray,
    val rimInt: Float,
    val skyCol: FloatArray,
    val sh: FloatArray,
  )
}

@Composable
internal fun Pev3dFilamentViewport(
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
  ambientTempC: Double,
  onBackendChanged: (String) -> Unit,
  modifier: Modifier = Modifier,
  finish: Pev3dModelFinish = Pev3dModelFinish(bodyRgb, accentRgb, caliperRgb),
) {
  val nativeAvailable = remember { FilamentNativeBootstrap.ensureInitialized() }
  var fallbackToOpenGl by remember { mutableStateOf(!nativeAvailable) }

  if (fallbackToOpenGl) {
    Pev3dOpenGlViewport(
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
      ambientTempC = ambientTempC.toFloat(),
      onBackendChanged = { onBackendChanged("OpenGL ES PBR (Fallback)") },
      modifier = modifier,
      finish = finish,
    )
    return
  }

  val xrayShell = renderMode == Pev3dRenderMode.XRAY
  val carbonGloss = renderMode == Pev3dRenderMode.CARBON_SHOWROOM
  var builtGlb by remember { mutableStateOf<PevBuiltGlb?>(null) }

  LaunchedEffect(
    archetype,
    finish.blueprintType,
    finish.bodyRgb.contentHashCode(),
    finish.accentRgb.contentHashCode(),
    finish.caliperRgb.contentHashCode(),
    finish.wheelScale,
    finish.bodyLengthScale,
    lightsOn,
    xrayShell,
    carbonGloss,
  ) {
    builtGlb = withContext(Dispatchers.Default) {
      PevFilamentGlbCache.getOrBuild(
        archetype = archetype,
        finish = finish,
        lightsOn = lightsOn,
        xrayShell = xrayShell,
        carbonGlossMode = carbonGloss,
      )
    }
  }

  var hostRef by remember { mutableStateOf<FilamentGlHost?>(null) }
  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(lifecycleOwner, hostRef) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> hostRef?.startLoop()
        Lifecycle.Event.ON_PAUSE -> hostRef?.pauseLoop()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      hostRef?.destroy()
      hostRef = null
    }
  }

  LaunchedEffect(builtGlb, hostRef) {
    val b = builtGlb ?: return@LaunchedEffect
    hostRef?.loadBuiltGlb(b)
  }

  // Push live interactive state to Filament host without triggering view rebuilds
  hostRef?.let { host ->
    host.yawDeg = yawDeg
    host.pitchDeg = pitchDeg
    host.zoom = zoom
    host.explodePct = explodePct
    host.wheelSpinDeg = wheelSpinDeg
    host.throttlePct = throttlePct
    host.lightsOn = lightsOn
    host.turnSignalMode = turnSignalMode
    host.selectedPartKey = selectedPartKey
    host.renderMode = renderMode
    host.lightingPreset = lightingPreset
    host.ambientTempC = ambientTempC
    host.accentRgb = accentRgb
  }

  Box(modifier = modifier) {
    AndroidView(
      factory = { context ->
        TextureView(context).apply {
          isOpaque = true
          setBackgroundColor(AndroidColor.rgb(12, 16, 24))
          val host = FilamentGlHost(
            textureView = this,
            onBackendLabel = onBackendChanged,
            onFatalInitError = { fallbackToOpenGl = true },
          )
          if (host.initialize()) {
            hostRef = host
            builtGlb?.let { host.loadBuiltGlb(it) }
          }
        }
      },
      modifier = Modifier.fillMaxSize(),
    )

    // Razor-sharp 3D CAD Wireframe & Technical Blueprint edge overlay synchronized to Filament camera
    if (renderMode == Pev3dRenderMode.WIREFRAME || renderMode == Pev3dRenderMode.BLUEPRINT) {
      val wireGroups = builtGlb?.wireGroups.orEmpty()
      if (wireGroups.isNotEmpty()) {
        val isBlueprint = renderMode == Pev3dRenderMode.BLUEPRINT
        val lineCol = if (isBlueprint) Color(0xFF7DD3FC).copy(alpha = 0.78f) else Color(0xFF22D3EE).copy(alpha = 0.84f)
        val selCol = Color(0xFFFDE047)
        Canvas(modifier = Modifier.fillMaxSize()) {
          if (size.width < 16f || size.height < 16f) return@Canvas
          val (normalSegs, selSegs) = Pev3dEngine.projectWireframeGroups(
            wireGroups = wireGroups,
            widthPx = size.width,
            heightPx = size.height,
            yawDeg = yawDeg,
            pitchDeg = pitchDeg,
            zoom = zoom,
            explodePct = explodePct,
            wheelSpinDeg = wheelSpinDeg,
            selectedPartKey = selectedPartKey,
          )
          var i = 0
          while (i + 3 < normalSegs.size) {
            drawLine(
              color = lineCol,
              start = Offset(normalSegs[i], normalSegs[i + 1]),
              end = Offset(normalSegs[i + 2], normalSegs[i + 3]),
              strokeWidth = if (isBlueprint) 1.35f else 1.5f,
              cap = StrokeCap.Round,
            )
            i += 4
          }
          var j = 0
          while (j + 3 < selSegs.size) {
            drawLine(
              color = selCol,
              start = Offset(selSegs[j], selSegs[j + 1]),
              end = Offset(selSegs[j + 2], selSegs[j + 3]),
              strokeWidth = 2.4f,
              cap = StrokeCap.Round,
            )
            j += 4
          }
        }
      }
    }
  }
}
