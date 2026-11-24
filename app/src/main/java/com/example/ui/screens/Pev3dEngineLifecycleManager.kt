package com.example.ui.screens

import android.util.Log

/**
 * Resource Lifecycle Manager for VoltTrail's multi-engine 3D viewports
 * (OpenGL ES 3.0 GGX PBR, Chromium WebGL 2.0, and Compose GPU Vector Mesh).
 * Centralizes cleanup hooks so native GL/WebGL surfaces pause cleanly and release GPU buffers.
 */
class Pev3dEngineLifecycleManager {
  companion object {
    private const val TAG = "Pev3dEngineLifecycleMgr"
  }

  private val disposeHooks = ArrayList<() -> Unit>()

  fun registerDisposeHook(hook: () -> Unit) {
    disposeHooks += hook
  }

  /**
   * Explicitly invokes all registered viewport cleanup hooks and clears native resource references.
   */
  fun destroyAll() {
    try {
      if (disposeHooks.isEmpty()) return
      Log.d(TAG, "Disposing ${disposeHooks.size} 3D viewport resource hook(s)...")
      disposeHooks.forEach { hook ->
        runCatching { hook() }
      }
      disposeHooks.clear()
    } catch (t: Throwable) {
      Log.e(TAG, "Error destroying 3D viewport resources: ${t.message}", t)
    }
  }
}
