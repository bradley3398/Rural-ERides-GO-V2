package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * Reusable debounced click modifier to prevent rapid double-tapping and multiple trigger events.
 */
fun Modifier.debouncedClickable(
  debounceTimeMs: Long = 400L,
  onClick: () -> Unit,
): Modifier = composed {
  var lastClickTime by remember { mutableLongStateOf(0L) }
  this.clickable {
    val now = System.currentTimeMillis()
    if (now - lastClickTime >= debounceTimeMs) {
      lastClickTime = now
      onClick()
    }
  }
}
