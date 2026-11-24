package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Reusable debounced click modifier with tactile press visual feedback to prevent rapid double-tapping and multiple trigger events.
 */
fun Modifier.debouncedClickable(
  debounceTimeMs: Long = 400L,
  onClick: () -> Unit,
): Modifier = composed {
  var lastClickTime by remember { mutableLongStateOf(0L) }
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  this
    .graphicsLayer {
      scaleX = if (isPressed) 0.97f else 1.0f
      scaleY = if (isPressed) 0.97f else 1.0f
      alpha = if (isPressed) 0.85f else 1.0f
    }
    .clickable(
      interactionSource = interactionSource,
      indication = ripple(),
    ) {
      val now = System.currentTimeMillis()
      if (now - lastClickTime >= debounceTimeMs) {
        lastClickTime = now
        onClick()
      }
    }
}

/**
 * Debounced Button with built-in duplicate click protection and tactile press scale feedback.
 */
@Composable
fun DebouncedButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  debounceTimeMs: Long = 400L,
  colors: ButtonColors = ButtonDefaults.buttonColors(),
  contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
  content: @Composable RowScope.() -> Unit,
) {
  var lastClickTime by remember { mutableLongStateOf(0L) }
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  Button(
    onClick = {
      val now = System.currentTimeMillis()
      if (now - lastClickTime >= debounceTimeMs) {
        lastClickTime = now
        onClick()
      }
    },
    modifier = modifier.graphicsLayer {
      scaleX = if (isPressed) 0.96f else 1.0f
      scaleY = if (isPressed) 0.96f else 1.0f
    },
    enabled = enabled,
    interactionSource = interactionSource,
    colors = colors,
    contentPadding = contentPadding,
    content = content,
  )
}

/**
 * Debounced IconButton with duplicate click protection and state-based visual feedback.
 */
@Composable
fun DebouncedIconButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  debounceTimeMs: Long = 400L,
  content: @Composable () -> Unit,
) {
  var lastClickTime by remember { mutableLongStateOf(0L) }
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  IconButton(
    onClick = {
      val now = System.currentTimeMillis()
      if (now - lastClickTime >= debounceTimeMs) {
        lastClickTime = now
        onClick()
      }
    },
    modifier = modifier.graphicsLayer {
      scaleX = if (isPressed) 0.92f else 1.0f
      scaleY = if (isPressed) 0.92f else 1.0f
      alpha = if (isPressed) 0.7f else 1.0f
    },
    enabled = enabled,
    interactionSource = interactionSource,
    content = content,
  )
}
