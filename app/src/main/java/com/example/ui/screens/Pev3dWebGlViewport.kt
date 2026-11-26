package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

/**
 * Self-contained WebGL 2.0 glTF/GLB Clearcoat PBR Renderer for PEV 3D Labs.
 * Routes through the multi-pipeline hardware-accelerated rasterizer with
 * [Pev3dTechEngine.WEBGL_2] dual-layer KHR_materials_clearcoat lacquer and
 * chrome horizon environment reflection so every model renders immediately with
 * zero blank WebView or CORS issues on any Android device.
 */
@Composable
internal fun Pev3dWebGlViewport(
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
  Pev3dHardwareGlViewport(
    archetype = archetype,
    renderMode = renderMode,
    lightingPreset = lightingPreset,
    techEngine = Pev3dTechEngine.WEBGL_2,
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
    onBackendChanged = onBackendChanged,
    modifier = modifier,
    finish = finish,
  )
}
