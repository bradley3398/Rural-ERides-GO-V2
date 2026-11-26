package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import android.view.View
import android.view.ViewParent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Resolved system safe-area insets combining local Compose WindowInsets with the host
 * Activity decorView's root WindowInsetsCompat so Dialog windows (including Samsung One UI
 * / Android 14-16) never collapse status/navigation/cutout/IME padding to 0.dp.
 */
data class HostWindowSafeInsets(
  val top: Dp,
  val bottom: Dp,
  val start: Dp,
  val end: Dp,
  val imeBottom: Dp,
)

private tailrec fun Context.findActivityOrNull(): Activity? =
  when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivityOrNull()
    else -> null
  }

@Composable
fun ConfigureEdgeToEdgeDialogWindow(fullScreen: Boolean = true) {
  val view = LocalView.current
  SideEffect {
    var parent: ViewParent? = view.parent
    var dialogWindow: android.view.Window? = null
    while (parent != null) {
      if (parent is DialogWindowProvider) {
        dialogWindow = parent.window
        break
      }
      parent = parent.parent
    }
    dialogWindow?.let { win ->
      WindowCompat.setDecorFitsSystemWindows(win, false)
      if (fullScreen) {
        @Suppress("DEPRECATION")
        runCatching {
          win.statusBarColor = AndroidColor.TRANSPARENT
          win.navigationBarColor = AndroidColor.TRANSPARENT
        }
      }
    }
  }
}

@Composable
fun rememberHostWindowSafeInsets(): HostWindowSafeInsets {
  val view = LocalView.current
  val density = LocalDensity.current
  val layoutDir = LocalLayoutDirection.current

  val localStatusTop = WindowInsets.statusBars.getTop(density)
  val localCutoutTop = WindowInsets.displayCutout.getTop(density)
  val localNavBottom = WindowInsets.navigationBars.getBottom(density)
  val localSafeLeft = WindowInsets.safeDrawing.getLeft(density, layoutDir)
  val localSafeRight = WindowInsets.safeDrawing.getRight(density, layoutDir)
  val localImeBottom = WindowInsets.ime.getBottom(density)

  val rootDecorView: View? =
    remember(view) {
      view.context.findActivityOrNull()?.window?.decorView ?: view.rootView
    }
  val rootCompat = rootDecorView?.let { ViewCompat.getRootWindowInsets(it) }
  val rootSys = rootCompat?.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
  val rootNav = rootCompat?.getInsets(WindowInsetsCompat.Type.navigationBars())
  val rootIme = rootCompat?.getInsets(WindowInsetsCompat.Type.ime())

  val topPx = maxOf(localStatusTop, localCutoutTop, rootSys?.top ?: 0)
  val bottomPx = maxOf(localNavBottom, rootNav?.bottom ?: 0)
  val leftPx = maxOf(localSafeLeft, rootSys?.left ?: 0)
  val rightPx = maxOf(localSafeRight, rootSys?.right ?: 0)
  val imePx = maxOf(localImeBottom, rootIme?.bottom ?: 0)

  val isRtl = layoutDir == androidx.compose.ui.unit.LayoutDirection.Rtl
  val startPx = if (isRtl) rightPx else leftPx
  val endPx = if (isRtl) leftPx else rightPx

  return with(density) {
    HostWindowSafeInsets(
      top = topPx.toDp(),
      bottom = bottomPx.toDp(),
      start = startPx.toDp(),
      end = endPx.toDp(),
      imeBottom = imePx.toDp(),
    )
  }
}
