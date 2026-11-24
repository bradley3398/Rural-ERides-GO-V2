package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

internal sealed interface Pev3dAssetLoadState {
  data object Loading : Pev3dAssetLoadState
  data class Ready(val assetName: String, val envName: String) : Pev3dAssetLoadState
  data class MissingGlb(val expectedAssetPath: String) : Pev3dAssetLoadState
  data class EngineUnavailable(val reason: String) : Pev3dAssetLoadState
}

/**
 * Legacy viewport delegate — routes cleanly to hardware-accelerated OpenGL ES 3D.
 */
@Composable
internal fun Pev3dVulkanViewport(
  archetype: Pev3dArchetype,
  renderMode: Pev3dRenderMode,
  lightingPreset: Pev3dLightingPreset,
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
  turnSignalMode: Pev3dTurnSignalMode,
  selectedPartKey: String?,
  accentRgb: FloatArray,
  onBackendChanged: (String) -> Unit,
  onWireframeLinesReady: (List<PevWireGroup>) -> Unit,
  onAssetStateChanged: (Pev3dAssetLoadState) -> Unit,
  modifier: Modifier = Modifier,
) {
  val finish = androidx.compose.runtime.remember(modelName, modelCategory, modelVoltage) {
    Pev3dEngine.finishForModel(modelName, modelCategory, modelVoltage)
  }
  LaunchedEffect(modelName) {
    onAssetStateChanged(Pev3dAssetLoadState.Ready(modelName, "OpenGL ES 3D"))
  }
  Pev3dOpenGlViewport(
    archetype = archetype,
    renderMode = renderMode,
    lightingPreset = lightingPreset,
    yawDeg = yawDeg,
    pitchDeg = pitchDeg,
    zoom = zoom,
    explodePct = explodePct,
    wheelSpinDeg = wheelSpinDeg,
    throttlePct = 0.35f,
    lightsOn = lightsOn,
    turnSignalMode = turnSignalMode,
    selectedPartKey = selectedPartKey,
    accentRgb = finish.accentRgb,
    bodyRgb = finish.bodyRgb,
    caliperRgb = finish.caliperRgb,
    onBackendChanged = onBackendChanged,
    modifier = modifier,
    finish = finish,
  )
}
