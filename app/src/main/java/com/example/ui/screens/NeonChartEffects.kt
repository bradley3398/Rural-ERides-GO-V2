package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Animated 0f -> 1f chart entrance progress returned as a State<Float> so callers
 * can read `.value` inside Canvas { ... } or graphicsLayer { ... } with ZERO
 * Compose tree recompositions per frame!
 */
@Composable
internal fun rememberChartEntranceState(
  triggerKey: Any?,
  reducedMotion: Boolean = false,
  durationMs: Int = 580,
  delayMs: Int = 0,
): State<Float> {
  val anim = remember(triggerKey) { Animatable(if (reducedMotion) 1f else 0f) }
  LaunchedEffect(triggerKey, reducedMotion) {
    if (reducedMotion) {
      anim.snapTo(1f)
    } else {
      if (anim.value < 0.99f) {
        anim.animateTo(
          targetValue = 1f,
          animationSpec = tween(durationMillis = durationMs, delayMillis = delayMs, easing = FastOutSlowInEasing),
        )
      }
    }
  }
  return anim.asState()
}

/**
 * Backwards-compatible helper (prefer rememberChartEntranceState for zero-recomposition draw phase).
 */
@Composable
internal fun rememberChartEntranceProgress(
  triggerKey: Any?,
  reducedMotion: Boolean = false,
  durationMs: Int = 580,
  delayMs: Int = 0,
): Float = rememberChartEntranceState(triggerKey, reducedMotion, durationMs, delayMs).value

/**
 * Subtle neon beacon pulse (0.45f -> 1f) returned as State<Float> AND only active when
 * [active] is true so idle charts in a LazyColumn consume ZERO background animation frames!
 */
@Composable
internal fun rememberNeonPulseAlphaState(
  active: Boolean = false,
  reducedMotion: Boolean = false,
): State<Float> {
  if (reducedMotion || !active) {
    return remember { mutableFloatStateOf(0.78f) }
  }
  val transition = rememberInfiniteTransition(label = "neon_chart_pulse")
  return transition.animateFloat(
    initialValue = 0.45f,
    targetValue = 1f,
    animationSpec =
      infiniteRepeatable(
        animation = tween(durationMillis = 1050, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse,
      ),
    label = "neon_pulse_alpha",
  )
}

/**
 * Staggered card entrance wrapper that applies alpha/translation strictly inside
 * `graphicsLayer {}` (layer phase), preventing full-card recomposition during entrance.
 */
@Composable
internal fun NeonAnimatedChartContainer(
  triggerKey: Any?,
  reducedMotion: Boolean,
  delayMs: Int = 0,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  val progressState =
    rememberChartEntranceState(
      triggerKey = triggerKey,
      reducedMotion = reducedMotion,
      durationMs = 380,
      delayMs = delayMs.coerceAtMost(220),
    )
  Box(
    modifier =
      modifier.graphicsLayer {
        val p = progressState.value
        alpha = p
        translationY = (1f - p) * 20f
        scaleX = 0.98f + 0.02f * p
        scaleY = 0.98f + 0.02f * p
      },
  ) {
    content()
  }
}

/**
 * Non-blocking pointer Hover (mouse/trackpad/stylus) + Touch Tap + Horizontal Scrub detector.
 *
 * Key anti-freeze design:
 * 1) Never consumes vertical scroll gestures so LazyColumn stays 60fps silky smooth.
 * 2) Deduplicates index emissions (`idx != lastIdx`) so Compose doesn't recompose on sub-pixel jitter.
 * 3) Uses `rememberUpdatedState` so pointerInput never restarts mid-gesture when callbacks change.
 */
@Composable
internal fun Modifier.chartHoverAndScrub(
  itemCount: Int,
  useSlots: Boolean = false,
  onPointFocused: (index: Int, xFrac: Float, isHovering: Boolean) -> Unit,
  onHoverEnded: () -> Unit = {},
  onTap: (() -> Unit)? = null,
): Modifier {
  val latestOnPointFocused by rememberUpdatedState(onPointFocused)
  val latestOnHoverEnded by rememberUpdatedState(onHoverEnded)
  val latestOnTap by rememberUpdatedState(onTap)

  return pointerInput(itemCount, useSlots) {
    if (itemCount <= 0) return@pointerInput
    val tapSlop = viewConfiguration.touchSlop.coerceAtLeast(24f)
    awaitPointerEventScope {
      var lastIdx = -1
      var pressOrigin: Offset? = null
      var pressTime = 0L
      var horizontalScrubLocked = false

      fun emitForX(x: Float, width: Float) {
        if (!x.isFinite() || !width.isFinite() || width <= 0f) return
        val rawFrac = (x / width).coerceIn(0f, 1f)
        val idx =
          if (useSlots || itemCount == 1) {
            (rawFrac * itemCount).toInt().coerceIn(0, itemCount - 1)
          } else {
            (rawFrac * (itemCount - 1)).roundToInt().coerceIn(0, itemCount - 1)
          }
        if (idx != lastIdx) {
          lastIdx = idx
          val snappedFrac =
            when {
              itemCount == 1 -> 0.5f
              useSlots -> (idx + 0.5f) / itemCount.toFloat()
              else -> idx.toFloat() / (itemCount - 1).toFloat()
            }
          latestOnPointFocused(idx, snappedFrac, true)
        }
      }

      while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull() ?: continue
        val w = size.width.toFloat().coerceAtLeast(1f)
        val isMouseOrStylus = change.type == PointerType.Mouse || change.type == PointerType.Stylus

        when (event.type) {
          PointerEventType.Enter -> {
            if (isMouseOrStylus) emitForX(change.position.x, w)
          }
          PointerEventType.Press -> {
            pressOrigin = change.position
            pressTime = System.currentTimeMillis()
            horizontalScrubLocked = false
            lastIdx = -1
            emitForX(change.position.x, w)
          }
          PointerEventType.Move -> {
            if (isMouseOrStylus && !change.pressed) {
              emitForX(change.position.x, w)
            } else if (change.pressed) {
              val origin = pressOrigin
              if (origin != null) {
                val dx = abs(change.position.x - origin.x)
                val dy = abs(change.position.y - origin.y)
                // Only track horizontal scrub if user clearly moves horizontally; otherwise let LazyColumn scroll freely!
                if (!horizontalScrubLocked && dx > 14f && dx > dy * 1.35f) {
                  horizontalScrubLocked = true
                }
                if (horizontalScrubLocked) {
                  emitForX(change.position.x, w)
                }
              }
            }
          }
          PointerEventType.Release -> {
            val origin = pressOrigin
            val tapCb = latestOnTap
            if (origin != null && !horizontalScrubLocked && tapCb != null) {
              val dx = abs(change.position.x - origin.x)
              val dy = abs(change.position.y - origin.y)
              val dt = System.currentTimeMillis() - pressTime
              if (dx <= tapSlop && dy <= tapSlop && dt < 500L) {
                tapCb()
              }
            }
            pressOrigin = null
            horizontalScrubLocked = false
          }
          PointerEventType.Exit -> {
            lastIdx = -1
            pressOrigin = null
            horizontalScrubLocked = false
            latestOnHoverEnded()
          }
          else -> Unit
        }
      }
    }
  }
}

/**
 * Non-blocking 2D pointer Hover + Tap detector for scatter plots and polar pie/donut charts.
 * Throttles move emissions so vertical scrolling over charts never stutters or freezes.
 */
@Composable
internal fun Modifier.chartPointer2D(
  key: Any?,
  onPointerAt: (position: Offset, width: Float, height: Float, isHovering: Boolean) -> Unit,
  onHoverEnded: () -> Unit = {},
  onTap: (() -> Unit)? = null,
): Modifier {
  val latestOnPointerAt by rememberUpdatedState(onPointerAt)
  val latestOnHoverEnded by rememberUpdatedState(onHoverEnded)
  val latestOnTap by rememberUpdatedState(onTap)

  return pointerInput(key) {
    val tapSlop = viewConfiguration.touchSlop.coerceAtLeast(24f)
    awaitPointerEventScope {
      var lastEmitPos = Offset.Unspecified
      var pressOrigin: Offset? = null
      var pressTime = 0L

      while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull() ?: continue
        val w = size.width.toFloat().coerceAtLeast(1f)
        val h = size.height.toFloat().coerceAtLeast(1f)
        val isMouseOrStylus = change.type == PointerType.Mouse || change.type == PointerType.Stylus
        when (event.type) {
          PointerEventType.Press -> {
            pressOrigin = change.position
            pressTime = System.currentTimeMillis()
            lastEmitPos = change.position
            latestOnPointerAt(change.position, w, h, true)
          }
          PointerEventType.Enter,
          PointerEventType.Move -> {
            if (isMouseOrStylus) {
              val prev = lastEmitPos
              if (prev == Offset.Unspecified || abs(change.position.x - prev.x) > 6f || abs(change.position.y - prev.y) > 6f) {
                lastEmitPos = change.position
                latestOnPointerAt(change.position, w, h, true)
              }
            }
          }
          PointerEventType.Release -> {
            val origin = pressOrigin
            val tapCb = latestOnTap
            if (origin != null && tapCb != null) {
              val dx = abs(change.position.x - origin.x)
              val dy = abs(change.position.y - origin.y)
              val dt = System.currentTimeMillis() - pressTime
              if (dx <= tapSlop && dy <= tapSlop && dt < 500L) {
                tapCb()
              }
            }
            pressOrigin = null
          }
          PointerEventType.Exit -> {
            lastEmitPos = Offset.Unspecified
            pressOrigin = null
            latestOnHoverEnded()
          }
          else -> Unit
        }
      }
    }
  }
}

internal data class NeonTooltipMetric(
  val label: String,
  val value: String,
  val color: Color,
)

/**
 * Floating neon HUD hover-tooltip pill positioned dynamically above the hovered/scrubbed
 * point using lightweight `onSizeChanged` (NO `BoxWithConstraints` subcomposition!) so
 * tooltips stay silky smooth inside LazyColumns.
 */
@Composable
internal fun NeonChartTooltipOverlay(
  visible: Boolean,
  anchorXFraction: Float,
  title: String,
  subtitle: String = "",
  accentColor: Color,
  metrics: List<NeonTooltipMetric>,
  modifier: Modifier = Modifier,
) {
  var containerWidthPx by remember { mutableIntStateOf(0) }
  val density = LocalDensity.current
  val approxTooltipWidthPx = remember(density) { with(density) { 196.dp.toPx() } }

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .onSizeChanged { sz ->
          if (sz.width != containerWidthPx) containerWidthPx = sz.width
        },
  ) {
    val safeAnchor = if (anchorXFraction.isFinite()) anchorXFraction.coerceIn(0f, 1f) else 0.5f
    val maxWidthPx = containerWidthPx.toFloat().coerceAtLeast(approxTooltipWidthPx + 8f)
    val maxClamped = (maxWidthPx - approxTooltipWidthPx - 4f).coerceAtLeast(4f)
    val clampedX =
      (safeAnchor * maxWidthPx - approxTooltipWidthPx / 2f)
        .let { if (it.isFinite()) it else 4f }
        .coerceIn(4f, maxClamped)

    AnimatedVisibility(
      visible = visible && containerWidthPx > 0,
      enter =
        fadeIn(tween(130)) +
          scaleIn(
            initialScale = 0.92f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
          ),
      exit = fadeOut(tween(110)) + scaleOut(targetScale = 0.94f),
      modifier = Modifier.offset { IntOffset(clampedX.roundToInt(), 0) },
    ) {
      Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(11.dp),
        border = BorderStroke(1.25.dp, accentColor.copy(alpha = 0.88f)),
        shadowElevation = 6.dp,
        modifier =
          Modifier
            .widthIn(min = 148.dp, max = 212.dp)
            .testTag("neon_chart_hover_tooltip"),
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Box(
            modifier =
              Modifier
                .fillMaxWidth()
                .height(2.5.dp)
                .background(
                  Brush.horizontalGradient(
                    listOf(accentColor.copy(alpha = 0.15f), accentColor, accentColor.copy(alpha = 0.15f)),
                  ),
                ),
          )
          Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
              Box(Modifier.size(7.dp).clip(CircleShape).background(accentColor))
              Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
            }
            if (subtitle.isNotBlank()) {
              Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              metrics.take(3).forEach { m ->
                Column(modifier = Modifier.weight(1f, fill = false)) {
                  Text(
                    text = m.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                  )
                  Text(
                    text = m.value,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = m.color,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
