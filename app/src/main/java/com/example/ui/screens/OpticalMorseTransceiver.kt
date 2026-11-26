package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.MasterSettings
import com.example.hardware.VoiceAudioManager
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

internal object MorseCodec {
  val CHAR_TO_MORSE: Map<Char, String> =
    mapOf(
      'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
      'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
      'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
      'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
      'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
      'Z' to "--..",
      '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
      '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.",
      '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '!' to "-.-.--",
      '/' to "-..-.", '-' to "-....-", '+' to ".-.-.", '=' to "-...-",
    )

  val MORSE_TO_CHAR: Map<String, Char> = CHAR_TO_MORSE.entries.associate { (k, v) -> v to k }

  fun encodeToPreview(message: String): String =
    message
      .uppercase()
      .trim()
      .split(Regex("\\s+"))
      .filter { it.isNotEmpty() }
      .joinToString(" / ") { word ->
        word.mapNotNull { CHAR_TO_MORSE[it] }.joinToString(" ")
      }

  fun decodeSymbol(code: String): String {
    if (code == "...---...") return "SOS"
    MORSE_TO_CHAR[code]?.let { return it.toString() }
    // Long-distance recovery: if a very long sequence merged two characters without enough gap
    if (code.length > 6) {
      val firstHalf = code.take(3)
      val secondHalf = code.drop(3)
      val c1 = MORSE_TO_CHAR[firstHalf]
      val c2 = MORSE_TO_CHAR[secondHalf]
      if (c1 != null && c2 != null) return "$c1$c2"
    }
    return "?"
  }
}

/**
 * Distance sensitivity profile for optical detection:
 * - FAR_DISTANCE: High-gain 3x3 point-source hotspot detector with low Schmitt hysteresis delta (+18 luma)
 * - MID_TRAIL: Balanced hotspot detector (+30 luma)
 * - CLOSE_RANGE: Anti-glare broad spot detector (+48 luma)
 */
internal enum class MorseDistanceMode(
  val label: String,
  val triggerDeltaOn: Int,
  val triggerDeltaOff: Int,
  val minAbsolutePeak: Int,
  val reticleFraction: Float,
) {
  FAR_DISTANCE("Far (40-200m+)", triggerDeltaOn = 18, triggerDeltaOff = 10, minAbsolutePeak = 52, reticleFraction = 0.22f),
  MID_TRAIL("Mid (10-40m)", triggerDeltaOn = 30, triggerDeltaOff = 17, minAbsolutePeak = 82, reticleFraction = 0.26f),
  CLOSE_RANGE("Close (1-10m)", triggerDeltaOn = 48, triggerDeltaOff = 28, minAbsolutePeak = 130, reticleFraction = 0.32f),
}

/**
 * Real-time long-distance optical pulse timing decoder with:
 * - Schmitt-trigger hysteresis (separate turn-on and turn-off thresholds to eliminate distant shimmer)
 * - Rolling temporal dark-floor & peak tracking (AC-coupled pulse detection for faint distant beams)
 * - Auto-adaptive dot/dash timing calibration so speed mismatches or 15fps night camera exposure still decode cleanly
 */
private class OpticalMorseDecoder(
  @Volatile var unitMs: Long = 340L,
  @Volatile var autoSpeedAdapt: Boolean = true,
) {
  private var isLit = false
  private var lastEdgeMs = 0L
  private var pendingEdgeState: Boolean? = null
  private var pendingEdgeStartMs = 0L
  private var currentSymbol = StringBuilder()
  private var decoded = StringBuilder()
  private var wordSpaceEmitted = true

  // Rolling temporal baseline for distant point light sources
  private var rollingDarkFloor = 45f
  private var rollingPeakCeiling = 110f
  private var adaptiveUnitMs = 340f
  private val recentPulseDurations = ArrayDeque<Long>(16)

  @Synchronized
  fun reset() {
    isLit = false
    lastEdgeMs = 0L
    pendingEdgeState = null
    pendingEdgeStartMs = 0L
    currentSymbol.clear()
    decoded.clear()
    wordSpaceEmitted = true
    recentPulseDurations.clear()
    adaptiveUnitMs = unitMs.toFloat()
  }

  @Synchronized
  fun classifyLitState(
    hotspotPeak: Int,
    surroundBg: Int,
    autoThreshold: Boolean,
    manualThreshold: Int,
    distanceMode: MorseDistanceMode,
  ): Triple<Boolean, Int, Int> {
    val spatialContrast = (hotspotPeak - surroundBg).coerceAtLeast(0)
    if (!autoThreshold) {
      val onTh = manualThreshold
      val offTh = (manualThreshold - 12).coerceAtLeast(30)
      val nextLit = if (isLit) hotspotPeak >= offTh else hotspotPeak >= onTh
      return Triple(nextLit, onTh, spatialContrast)
    }

    // Update rolling dark floor when not lit, or slowly decay floor
    if (!isLit && hotspotPeak < rollingDarkFloor + distanceMode.triggerDeltaOn) {
      rollingDarkFloor = rollingDarkFloor * 0.88f + hotspotPeak * 0.12f
    } else {
      rollingDarkFloor = min(rollingDarkFloor, surroundBg.toFloat() + 6f)
    }

    if (hotspotPeak > rollingPeakCeiling) {
      rollingPeakCeiling = rollingPeakCeiling * 0.6f + hotspotPeak * 0.4f
    } else {
      rollingPeakCeiling = max(rollingDarkFloor + distanceMode.triggerDeltaOn + 10f, rollingPeakCeiling * 0.992f)
    }

    val dynamicSpan = (rollingPeakCeiling - rollingDarkFloor).coerceAtLeast(distanceMode.triggerDeltaOn.toFloat())
    val adaptiveOn =
      max(
        distanceMode.minAbsolutePeak,
        min(238, (rollingDarkFloor + max(distanceMode.triggerDeltaOn.toFloat(), dynamicSpan * 0.42f)).roundToInt()),
      )
    val adaptiveOff =
      max(
        distanceMode.minAbsolutePeak - 10,
        (rollingDarkFloor + max(distanceMode.triggerDeltaOff.toFloat(), dynamicSpan * 0.24f)).roundToInt(),
      )

    // Also allow trigger when spatial point-source contrast spike is strong (crucial at 100m+ in twilight/night)
    val spatialOn =
      hotspotPeak >= distanceMode.minAbsolutePeak &&
        spatialContrast >= (distanceMode.triggerDeltaOn + 6)
    val spatialOff =
      hotspotPeak >= (distanceMode.minAbsolutePeak - 8) &&
        spatialContrast >= distanceMode.triggerDeltaOff

    val litNow =
      if (!isLit) {
        hotspotPeak >= adaptiveOn || spatialOn
      } else {
        hotspotPeak >= adaptiveOff || spatialOff
      }
    return Triple(litNow, adaptiveOn, spatialContrast)
  }

  private fun updateAdaptiveTiming(pulseMs: Long) {
    if (pulseMs !in 45L..2200L) return
    if (recentPulseDurations.size >= 14) recentPulseDurations.removeFirst()
    recentPulseDurations.addLast(pulseMs)
    if (!autoSpeedAdapt || recentPulseDurations.size < 3) {
      adaptiveUnitMs = unitMs.toFloat()
      return
    }
    val sorted = recentPulseDurations.sorted()
    val shortest = sorted.take(max(1, sorted.size / 2)).average().toFloat()
    val longest = sorted.takeLast(max(1, sorted.size / 2)).average().toFloat()
    // If we have both dots and dashes (ratio > 1.9x), calibrate unitMs from shortest cluster
    if (longest / shortest.coerceAtLeast(1f) >= 1.85f) {
      val estimatedUnit = shortest.coerceIn(90f, 560f)
      adaptiveUnitMs = adaptiveUnitMs * 0.65f + estimatedUnit * 0.35f
    } else {
      adaptiveUnitMs = unitMs.toFloat()
    }
  }

  @Synchronized
  fun onFrameSample(litRaw: Boolean, nowMs: Long): Pair<String, String> {
    if (lastEdgeMs == 0L) {
      isLit = litRaw
      lastEdgeMs = nowMs
      return currentSymbol.toString() to decoded.toString()
    }

    val effUnit = if (autoSpeedAdapt) adaptiveUnitMs.toLong().coerceIn(100L, 550L) else unitMs
    // 25ms glitch debounce filter prevents single-frame camera noise at long distance from false-triggering
    val debounceMs = 24L
    var litNow = isLit
    if (litRaw != isLit) {
      if (pendingEdgeState != litRaw) {
        pendingEdgeState = litRaw
        pendingEdgeStartMs = nowMs
      } else if (nowMs - pendingEdgeStartMs >= debounceMs) {
        litNow = litRaw
        pendingEdgeState = null
      }
    } else {
      pendingEdgeState = null
    }

    val edgeTimestamp = if (litNow != isLit && pendingEdgeStartMs > 0L) pendingEdgeStartMs else nowMs
    val elapsed = (edgeTimestamp - lastEdgeMs).coerceAtLeast(0L)
    val minPulseMs = (effUnit * 0.22).toLong().coerceIn(32L, 95L)
    val dotDashBoundaryMs = (effUnit * 1.95).toLong()
    val letterGapMs = (effUnit * 2.15).toLong()
    val wordGapMs = (effUnit * 5.1).toLong()

    if (litNow != isLit) {
      if (isLit && !litNow) {
        // Falling edge: light turned OFF -> classify pulse as dot (.) or dash (-)
        if (elapsed >= minPulseMs) {
          // Ignore 1.4s+ aiming preamble lock beam
          if (elapsed < effUnit * 6.8) {
            updateAdaptiveTiming(elapsed)
            val updatedEffUnit = if (autoSpeedAdapt) adaptiveUnitMs.toLong().coerceIn(100L, 550L) else unitMs
            val boundary = (updatedEffUnit * 1.95).toLong()
            currentSymbol.append(if (elapsed < boundary) '.' else '-')
            wordSpaceEmitted = false
          }
        }
      }
      isLit = litNow
      lastEdgeMs = edgeTimestamp
    } else if (!isLit) {
      // Sustained dark gap -> decode letter or insert word space
      if (currentSymbol.isNotEmpty() && elapsed >= letterGapMs) {
        decoded.append(MorseCodec.decodeSymbol(currentSymbol.toString()))
        currentSymbol.clear()
      }
      if (!wordSpaceEmitted && decoded.isNotEmpty() && elapsed >= wordGapMs) {
        if (!decoded.endsWith(" ")) decoded.append(' ')
        wordSpaceEmitted = true
      }
    }

    return currentSymbol.toString() to decoded.toString()
  }
}

/**
 * TX: User types any message -> Android rear LED flash + optional high-visibility screen strobe
 * transmits International Morse Code with long-distance preamble lock support.
 */
@Composable
internal fun MorseFlashlightTransmitterPanel(
  hasFlashHardware: Boolean,
  torchError: String?,
  setHardwareTorch: (Boolean) -> Unit,
) {
  var message by remember { mutableStateOf("SOS") }
  var unitMs by remember { mutableIntStateOf(340) } // Default 340ms Long-Range for reliable distance capture
  var loopEnabled by remember { mutableStateOf(false) }
  var preambleLockEnabled by remember { mutableStateOf(true) }
  var dualScreenStrobe by remember { mutableStateOf(true) }
  var isTransmitting by remember { mutableStateOf(false) }
  var lampLit by remember { mutableStateOf(false) }
  var activeCharDisplay by remember { mutableStateOf("") }
  var activeSymbolDisplay by remember { mutableStateOf("") }

  val encodedPreview = remember(message) { MorseCodec.encodeToPreview(message) }
  val presets = remember { listOf("SOS", "OK", "STOP", "HELP", "FLAT TIRE", "LOW BATT", "WAIT", "RALLY HERE") }

  LaunchedEffect(isTransmitting, message, unitMs, loopEnabled, preambleLockEnabled) {
    if (!isTransmitting) {
      lampLit = false
      activeCharDisplay = ""
      activeSymbolDisplay = ""
      setHardwareTorch(false)
      return@LaunchedEffect
    }
    val cleaned = message.uppercase().trim()
    if (cleaned.isEmpty()) {
      isTransmitting = false
      return@LaunchedEffect
    }
    val u = unitMs.toLong()
    try {
      do {
        if (preambleLockEnabled) {
          activeCharDisplay = "AIMING LOCK"
          activeSymbolDisplay = "═══"
          lampLit = true
          setHardwareTorch(true)
          delay(1500L)
          lampLit = false
          setHardwareTorch(false)
          activeCharDisplay = "READY"
          activeSymbolDisplay = "..."
          delay(u * 4L)
        }
        val words = cleaned.split(Regex("\\s+")).filter { it.isNotEmpty() }
        words.forEachIndexed { wIdx, word ->
          word.forEachIndexed { cIdx, ch ->
            val code = MorseCodec.CHAR_TO_MORSE[ch] ?: return@forEachIndexed
            activeCharDisplay = ch.toString()
            code.forEachIndexed { sIdx, sym ->
              activeSymbolDisplay = code.take(sIdx + 1)
              lampLit = true
              setHardwareTorch(true)
              delay(if (sym == '.') u else u * 3L)
              lampLit = false
              setHardwareTorch(false)
              if (sIdx < code.lastIndex) {
                delay(u) // intra-character gap
              }
            }
            if (cIdx < word.lastIndex) {
              delay(u * 3L) // inter-character gap
            }
          }
          if (wIdx < words.lastIndex) {
            activeCharDisplay = "SPACE"
            activeSymbolDisplay = "/"
            delay(u * 7L) // word gap
          }
        }
        if (loopEnabled) {
          activeCharDisplay = "PAUSE"
          activeSymbolDisplay = ""
          delay(u * 8L)
        }
      } while (loopEnabled && isTransmitting)
    } finally {
      lampLit = false
      activeCharDisplay = ""
      activeSymbolDisplay = ""
      setHardwareTorch(false)
      isTransmitting = false
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      setHardwareTorch(false)
    }
  }

  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
    shape = RoundedCornerShape(14.dp),
    border =
      BorderStroke(
        width = if (lampLit) 2.dp else 1.dp,
        color = if (lampLit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
      ),
    modifier = Modifier.fillMaxWidth().testTag("morse_tx_panel"),
  ) {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Box(
            modifier =
              Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (lampLit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
          )
          Text(
            if (isTransmitting) "TRANSMITTING: $activeCharDisplay  $activeSymbolDisplay"
            else "LONG-RANGE MORSE TRANSMITTER (TX)",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = if (isTransmitting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      if (isTransmitting && dualScreenStrobe) {
        // High-visibility synchronized optical screen strobe beacon for long-distance front/rear visibility
        Surface(
          color = if (lampLit) Color(0xFFFFFFFF) else Color(0xFF070B12),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(2.dp, if (lampLit) Color(0xFF39FF14) else Color(0xFF1E293B)),
          modifier = Modifier.fillMaxWidth().height(64.dp),
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
              text = if (lampLit) "● OPTICAL PULSE ON ($activeCharDisplay $activeSymbolDisplay) ●" else "○ GAP ($activeCharDisplay) ○",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Black,
              color = if (lampLit) Color.Black else Color(0xFF64748B),
            )
          }
        }
      }

      if (!hasFlashHardware) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
          Text("No rear LED flash detected — High-Output Screen Strobe Beacon active.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
      } else if (torchError != null) {
        Text(torchError, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
      }

      OutlinedTextField(
        value = message,
        onValueChange = { next ->
          if (next.length <= 64) {
            message = next
            if (isTransmitting) isTransmitting = false
          }
        },
        label = { Text("Type Message to Flash in Morse") },
        placeholder = { Text("e.g. HELP FLAT TIRE") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("morse_tx_message_input"),
      )

      // Quick trail message chips
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(presets) { preset ->
          FilterChip(
            selected = message.equals(preset, ignoreCase = true),
            onClick = {
              isTransmitting = false
              message = preset
            },
            label = { Text(preset, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
          )
        }
      }

      // Live Morse Code Preview
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text("INTERNATIONAL MORSE CODE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            text = encodedPreview.ifBlank { "Type letters or numbers above..." },
            style = MaterialTheme.typography.titleSmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      // Speed selector (with Extreme Far 480ms option for long distance at night!)
      Text(
        "TRANSMIT CADENCE (USE LONG-RANGE OR EXTREME FOR DISTANT RIDERS)",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(
          listOf(
            140 to "Fast (140ms)",
            220 to "Standard (220ms)",
            340 to "Long-Range (340ms)",
            480 to "Extreme Far (480ms)",
          )
        ) { (ms, lbl) ->
          FilterChip(
            selected = unitMs == ms,
            onClick = { unitMs = ms },
            label = { Text(lbl, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
          )
        }
      }

      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("1.5s Aiming Lock Beam Before Message", style = MaterialTheme.typography.labelMedium)
        Switch(checked = preambleLockEnabled, onCheckedChange = { preambleLockEnabled = it })
      }

      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Sync High-Contrast Screen Strobe + LED", style = MaterialTheme.typography.labelMedium)
        Switch(checked = dualScreenStrobe, onCheckedChange = { dualScreenStrobe = it })
      }

      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Loop Repeat Signal", style = MaterialTheme.typography.labelMedium)
        Switch(checked = loopEnabled, onCheckedChange = { loopEnabled = it })
      }

      Button(
        onClick = { isTransmitting = !isTransmitting },
        enabled = message.isNotBlank(),
        colors =
          if (isTransmitting) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
          else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("morse_tx_toggle_button"),
      ) {
        Icon(
          if (isTransmitting) Icons.Default.Stop else Icons.Default.FlashlightOn,
          contentDescription = null,
          modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
          if (isTransmitting) "STOP FLASHING MORSE" else "FLASH MORSE MESSAGE",
          fontWeight = FontWeight.Black,
          maxLines = 1,
          softWrap = false,
        )
      }
    }
  }
}

/**
 * RX: Uses real CameraX Preview + Telephoto Zoom + 3x3 Point-Source Hotspot Peak Detector +
 * Schmitt-Trigger Temporal Contrast to optically read another rider's flashlight Morse code even far in the distance!
 */
@Composable
internal fun MorseCameraReceiverPanel(
  settings: MasterSettings,
  voiceAudio: VoiceAudioManager? = null,
) {
  val ctx = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
    )
  }
  val permLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasCameraPermission = granted
    }

  var unitMs by remember { mutableIntStateOf(340) }
  var autoSpeedAdapt by remember { mutableStateOf(true) }
  var distanceMode by remember { mutableStateOf(MorseDistanceMode.FAR_DISTANCE) }
  var autoThreshold by remember { mutableStateOf(true) }
  var manualThreshold by remember { mutableFloatStateOf(140f) }

  var zoomRatio by remember { mutableFloatStateOf(2.0f) }
  var maxSupportedZoom by remember { mutableFloatStateOf(8.0f) }
  var boundCamera by remember { mutableStateOf<Camera?>(null) }

  var livePeakLuma by remember { mutableIntStateOf(0) }
  var liveBgLuma by remember { mutableIntStateOf(0) }
  var liveContrastDelta by remember { mutableIntStateOf(0) }
  var liveEffectiveThreshold by remember { mutableIntStateOf(110) }
  var hotspotNormX by remember { mutableFloatStateOf(0.5f) }
  var hotspotNormY by remember { mutableFloatStateOf(0.5f) }
  var liveLit by remember { mutableStateOf(false) }
  var liveSymbols by remember { mutableStateOf("") }
  var decodedMessage by remember { mutableStateOf("") }
  var cameraError by remember { mutableStateOf<String?>(null) }
  val pulseHistory = remember { mutableStateListOf<Float>() }

  val decoder = remember { OpticalMorseDecoder(unitMs = 340L, autoSpeedAdapt = true) }
  LaunchedEffect(unitMs, autoSpeedAdapt) {
    decoder.unitMs = unitMs.toLong()
    decoder.autoSpeedAdapt = autoSpeedAdapt
  }

  LaunchedEffect(zoomRatio, boundCamera) {
    val cam = boundCamera ?: return@LaunchedEffect
    runCatching {
      val maxZ = (cam.cameraInfo.zoomState.value?.maxZoomRatio ?: 8.0f).coerceAtLeast(1.0f)
      maxSupportedZoom = maxZ
      cam.cameraControl.setZoomRatio(zoomRatio.coerceIn(1.0f, maxZ))
    }
  }

  // Keep latest threshold and distance mode readable on analyzer thread
  val analyzerConfig = remember {
    object {
      @Volatile var auto = true
      @Volatile var manual = 140
      @Volatile var mode = MorseDistanceMode.FAR_DISTANCE
    }
  }
  LaunchedEffect(autoThreshold, manualThreshold, distanceMode) {
    analyzerConfig.auto = autoThreshold
    analyzerConfig.manual = manualThreshold.roundToInt()
    analyzerConfig.mode = distanceMode
  }

  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
    shape = RoundedCornerShape(14.dp),
    border =
      BorderStroke(
        width = if (liveLit) 2.dp else 1.dp,
        color = if (liveLit) Color(0xFF22C55E) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f),
      ),
    modifier = Modifier.fillMaxWidth().testTag("morse_rx_panel"),
  ) {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(
          "LONG-RANGE MORSE RECEIVER (RX)",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Black,
          color = MaterialTheme.colorScheme.secondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Surface(
          color = if (liveLit) Color(0xFF22C55E).copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, if (liveLit) Color(0xFF22C55E) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        ) {
          Text(
            if (liveLit) "● LOCKED (PEAK $livePeakLuma / +$liveContrastDelta)"
            else "○ SCAN (PEAK $livePeakLuma / BG $liveBgLuma)",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = if (liveLit) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      if (!hasCameraPermission) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(36.dp))
          Text(
            "Point your rear camera at another rider's flashlight (even 100m+ down the trail) to lock onto the beam and decode Morse code live.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Button(
            onClick = { permLauncher.launch(Manifest.permission.CAMERA) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("morse_rx_grant_camera_button"),
          ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Enable Long-Range Camera Reader", maxLines = 1, softWrap = false)
          }
        }
      } else {
        val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
        var boundProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

        DisposableEffect(Unit) {
          onDispose {
            runCatching { boundProvider?.unbindAll() }
            runCatching { analysisExecutor.shutdown() }
          }
        }

        // LIVE CAMERAX VIEWFINDER WITH POINT-SOURCE HOTSPOT LOCK & PULSE OSCILLOSCOPE
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .height(215.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Color.Black),
        ) {
          AndroidView(
            factory = { viewCtx ->
              val previewView = PreviewView(viewCtx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
              }
              val future = ProcessCameraProvider.getInstance(viewCtx)
              future.addListener(
                {
                  runCatching {
                    val provider = future.get()
                    boundProvider = provider
                    val preview = Preview.Builder().build().also {
                      it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysis =
                      ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { ia ->
                          ia.setAnalyzer(analysisExecutor) { proxy ->
                            processMorseLuminanceFrame(
                              proxy = proxy,
                              autoThreshold = analyzerConfig.auto,
                              manualThreshold = analyzerConfig.manual,
                              distanceMode = analyzerConfig.mode,
                              decoder = decoder,
                              onUpdate = { peak, bg, delta, effTh, hx, hy, isLitNow, sym, text ->
                                previewView.post {
                                  livePeakLuma = peak
                                  liveBgLuma = bg
                                  liveContrastDelta = delta
                                  liveEffectiveThreshold = effTh
                                  hotspotNormX = hx
                                  hotspotNormY = hy
                                  liveLit = isLitNow
                                  liveSymbols = sym
                                  decodedMessage = text
                                  if (pulseHistory.size >= 48) pulseHistory.removeAt(0)
                                  pulseHistory.add(if (isLitNow) 1f else (peak / 255f).coerceIn(0f, 0.45f))
                                }
                              },
                            )
                          }
                        }
                    provider.unbindAll()
                    val cam =
                      provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                      )
                    boundCamera = cam
                    val maxZ = (cam.cameraInfo.zoomState.value?.maxZoomRatio ?: 8.0f).coerceAtLeast(1.0f)
                    maxSupportedZoom = maxZ
                    cam.cameraControl.setZoomRatio(zoomRatio.coerceIn(1.0f, maxZ))
                    cameraError = null
                  }.onFailure { err ->
                    cameraError = err.message ?: "Camera unavailable on this device."
                  }
                },
                ContextCompat.getMainExecutor(viewCtx),
              )
              previewView
            },
            modifier = Modifier.fillMaxSize(),
          )

          // Center Target Reticle + Live Hotspot Point-Source Lock Crosshair + Pulse Scope
          val reticleColor = if (liveLit) Color(0xFF22C55E) else MaterialTheme.colorScheme.primary
          Canvas(modifier = Modifier.fillMaxSize()) {
            val frac = distanceMode.reticleFraction
            val boxW = size.width * frac
            val boxH = size.height * (frac * 1.35f)
            val left = (size.width - boxW) / 2f
            val top = (size.height - boxH) / 2f
            drawRect(
              color = reticleColor,
              topLeft = Offset(left, top),
              size = Size(boxW, boxH),
              style = Stroke(width = if (liveLit) 3.dp.toPx() else 2.dp.toPx()),
            )
            // Center reticle reference ticks
            val cx = size.width / 2f
            val cy = size.height / 2f
            drawLine(reticleColor.copy(alpha = 0.55f), Offset(cx - 10.dp.toPx(), cy), Offset(cx + 10.dp.toPx(), cy), strokeWidth = 1.5.dp.toPx())
            drawLine(reticleColor.copy(alpha = 0.55f), Offset(cx, cy - 10.dp.toPx()), Offset(cx, cy + 10.dp.toPx()), strokeWidth = 1.5.dp.toPx())

            // Live Hotspot Lock Ring tracking the exact brightest distant point-source inside the reticle
            val lockX = left + hotspotNormX.coerceIn(0f, 1f) * boxW
            val lockY = top + hotspotNormY.coerceIn(0f, 1f) * boxH
            val lockColor = if (liveLit) Color(0xFF39FF14) else Color(0xFF38BDF8)
            drawCircle(
              color = lockColor,
              radius = if (liveLit) 9.dp.toPx() else 6.dp.toPx(),
              center = Offset(lockX, lockY),
              style = Stroke(width = 2.dp.toPx()),
            )
            if (liveLit) {
              drawCircle(
                color = Color(0xFF39FF14).copy(alpha = 0.35f),
                radius = 14.dp.toPx(),
                center = Offset(lockX, lockY),
              )
            }

            // Bottom Optical Pulse Oscilloscope Trace
            if (pulseHistory.size > 2) {
              val tracePath = Path()
              val stepX = size.width / 48f
              val baseLineY = size.height - 8.dp.toPx()
              val ampH = 22.dp.toPx()
              pulseHistory.forEachIndexed { i, v ->
                val px = i * stepX
                val py = baseLineY - (v * ampH)
                if (i == 0) tracePath.moveTo(px, py) else tracePath.lineTo(px, py)
              }
              drawPath(
                path = tracePath,
                color = if (liveLit) Color(0xFF39FF14) else Color(0xFF38BDF8).copy(alpha = 0.8f),
                style = Stroke(width = 2.dp.toPx()),
              )
            }
          }

          Surface(
            color = Color.Black.copy(alpha = 0.72f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
          ) {
            Text(
              "TELEPHOTO ${"%.1f".format(zoomRatio)}x • ${distanceMode.label.uppercase()}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF39FF14),
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }

          Surface(
            color = Color.Black.copy(alpha = 0.72f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.align(Alignment.BottomCenter).padding(6.dp),
          ) {
            Text(
              "Cyan ring auto-tracks brightest distant light inside box",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }

        if (cameraError != null) {
          Text(cameraError.orEmpty(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }

        // TELEPHOTO ZOOM CONTROLS FOR LONG-DISTANCE FLASHLIGHTS
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.ZoomIn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(
              "Telephoto Distance Zoom (${"%.1f".format(zoomRatio)}x)",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(1.0f to "1x", 2.0f to "2x", 4.0f to "4x", 6.0f to "6x").forEach { (z, lbl) ->
              val clampedZ = z.coerceAtMost(maxSupportedZoom)
              FilterChip(
                selected = abs(zoomRatio - clampedZ) < 0.25f,
                onClick = { zoomRatio = clampedZ },
                label = { Text(lbl, style = MaterialTheme.typography.labelSmall) },
              )
            }
          }
        }

        // DISTANCE SENSITIVITY MODE SELECTOR
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(Icons.Default.CenterFocusStrong, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
          MorseDistanceMode.entries.forEach { mode ->
            FilterChip(
              selected = distanceMode == mode,
              onClick = {
                distanceMode = mode
                if (mode == MorseDistanceMode.FAR_DISTANCE && zoomRatio < 2.0f) {
                  zoomRatio = 2.0f.coerceAtMost(maxSupportedZoom)
                }
              },
              label = { Text(mode.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
              modifier = Modifier.weight(1f),
            )
          }
        }

        // Real-time Hotspot Peak vs Threshold Bar
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(
            "Hotspot Peak: $livePeakLuma/255 (Contrast +$liveContrastDelta)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            "Trigger Gate: $liveEffectiveThreshold",
            style = MaterialTheme.typography.labelSmall,
            color = if (liveLit) Color(0xFF22C55E) else MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
          )
        }
        LinearProgressIndicator(
          progress = { (livePeakLuma / 255f).coerceIn(0f, 1f) },
          color = if (liveLit) Color(0xFF22C55E) else MaterialTheme.colorScheme.primary,
          modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
        )

        // Match TX Speed Chips + Auto-Speed Calibration
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          item {
            FilterChip(
              selected = autoSpeedAdapt,
              onClick = { autoSpeedAdapt = !autoSpeedAdapt },
              label = { Text("Auto-Speed Sync", style = MaterialTheme.typography.labelSmall) },
            )
          }
          items(
            listOf(
              140 to "Fast (140ms)",
              220 to "Standard (220ms)",
              340 to "Long-Range (340ms)",
              480 to "Extreme Far (480ms)",
            )
          ) { (ms, lbl) ->
            FilterChip(
              selected = unitMs == ms,
              onClick = { unitMs = ms },
              label = { Text(lbl, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
            )
          }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text(
            if (autoThreshold) "Auto Schmitt-Trigger Contrast (Best for Distance)"
            else "Manual Peak Threshold: ${manualThreshold.roundToInt()}/255",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
          )
          Switch(checked = autoThreshold, onCheckedChange = { autoThreshold = it })
        }
        if (!autoThreshold) {
          com.example.ui.components.TacticalSlider(
            value = manualThreshold,
            onValueChange = { manualThreshold = it },
            valueRange = 45f..245f,
          )
        }

        // DECODED LIVE MESSAGE BOX
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("DECODED MESSAGE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Black)
              Text(
                if (liveSymbols.isNotEmpty()) "BUFFER: $liveSymbols" else "Listening...",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
              )
            }
            Text(
              text = decodedMessage.ifBlank { "Waiting for distant light pulses..." },
              style = MaterialTheme.typography.titleMedium,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Black,
              color = if (decodedMessage.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        // ACTION BUTTONS (ALL 48dp HEIGHT)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = {
              if (decodedMessage.isNotBlank()) {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Morse Message", decodedMessage))
                Toast.makeText(ctx, "Copied decoded Morse!", Toast.LENGTH_SHORT).show()
              }
            },
            enabled = decodedMessage.isNotBlank(),
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("morse_rx_copy_button"),
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Copy", maxLines = 1, softWrap = false)
          }
          if (voiceAudio != null) {
            OutlinedButton(
              onClick = {
                if (decodedMessage.isNotBlank()) {
                  voiceAudio.speakText(decodedMessage, settings.globalVolume, settings.ttsSpeechRate, settings.ttsPitch)
                }
              },
              enabled = decodedMessage.isNotBlank(),
              modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("morse_rx_speak_button"),
            ) {
              Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Speak", maxLines = 1, softWrap = false)
            }
          }
          OutlinedButton(
            onClick = {
              decoder.reset()
              liveSymbols = ""
              decodedMessage = ""
              pulseHistory.clear()
            },
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("morse_rx_clear_button"),
          ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Clear", maxLines = 1, softWrap = false)
          }
        }
      }
    }
  }
}

/**
 * Long-Distance Point-Source Hotspot Luminance Analyzer:
 * Scans the target reticle for the brightest 3x3 pixel micro-cluster (so a tiny 2-pixel flashlight
 * 150 meters away is never diluted by the dark background around it) and compares it against the
 * local surrounding background ring using a Schmitt trigger.
 */
private fun processMorseLuminanceFrame(
  proxy: ImageProxy,
  autoThreshold: Boolean,
  manualThreshold: Int,
  distanceMode: MorseDistanceMode,
  decoder: OpticalMorseDecoder,
  onUpdate: (
    peakLuma: Int,
    bgLuma: Int,
    contrastDelta: Int,
    effectiveThreshold: Int,
    hotspotNormX: Float,
    hotspotNormY: Float,
    isLit: Boolean,
    buffer: String,
    decoded: String,
  ) -> Unit,
) {
  try {
    val yPlane = proxy.planes[0]
    val buffer = yPlane.buffer
    val rowStride = yPlane.rowStride
    val pixelStride = yPlane.pixelStride
    val width = proxy.width
    val height = proxy.height
    val limit = buffer.limit()

    val halfFrac = distanceMode.reticleFraction * 0.5f
    val xStart = (width * (0.5f - halfFrac)).toInt().coerceIn(2, width - 4)
    val xEnd = (width * (0.5f + halfFrac)).toInt().coerceIn(xStart + 2, width - 2)
    val yStart = (height * (0.5f - halfFrac)).toInt().coerceIn(2, height - 4)
    val yEnd = (height * (0.5f + halfFrac)).toInt().coerceIn(yStart + 2, height - 2)

    fun sampleLuma(px: Int, py: Int): Int {
      val idx = py * rowStride + px * pixelStride
      return if (idx in 0 until limit) (buffer.get(idx).toInt() and 0xFF) else 0
    }

    // 1. Fine 2-pixel stride scan across reticle to locate the brightest point-source hotspot
    var maxSpotLuma = 0
    var bestX = (xStart + xEnd) / 2
    var bestY = (yStart + yEnd) / 2
    var reticleSum = 0L
    var reticleCount = 0

    var y = yStart
    while (y < yEnd) {
      var x = xStart
      while (x < xEnd) {
        val c = sampleLuma(x, y)
        reticleSum += c
        reticleCount++
        if (c > maxSpotLuma) {
          maxSpotLuma = c
          bestX = x
          bestY = y
        }
        x += 2
      }
      y += 2
    }

    // 2. Compute 3x3 weighted peak cluster around (bestX, bestY) for sub-pixel stability
    var clusterSum = 0
    var clusterWeight = 0
    var peakSingle = maxSpotLuma
    for (dy in -1..1) {
      for (dx in -1..1) {
        val v = sampleLuma((bestX + dx).coerceIn(0, width - 1), (bestY + dy).coerceIn(0, height - 1))
        if (v > peakSingle) peakSingle = v
        val w = if (dx == 0 && dy == 0) 4 else 1
        clusterSum += v * w
        clusterWeight += w
      }
    }
    val clusterAvg = if (clusterWeight > 0) clusterSum / clusterWeight else peakSingle
    // Weight toward peakSingle in FAR_DISTANCE mode so a 1-2 pixel distant beam stays strong
    val hotspotPeak =
      when (distanceMode) {
        MorseDistanceMode.FAR_DISTANCE -> ((peakSingle * 3 + clusterAvg * 2) / 5)
        MorseDistanceMode.MID_TRAIL -> ((peakSingle + clusterAvg) / 2)
        MorseDistanceMode.CLOSE_RANGE -> clusterAvg
      }

    // 3. Measure local background ring (pixels inside/around reticle excluding the 11x11 box around hotspot)
    var ringSum = 0L
    var ringCount = 0
    var ry = (yStart - 12).coerceAtLeast(4)
    val ryMax = (yEnd + 12).coerceAtMost(height - 4)
    val rxMin = (xStart - 12).coerceAtLeast(4)
    val rxMax = (xEnd + 12).coerceAtMost(width - 4)
    while (ry < ryMax) {
      var rx = rxMin
      while (rx < rxMax) {
        if (abs(rx - bestX) > 7 || abs(ry - bestY) > 7) {
          ringSum += sampleLuma(rx, ry)
          ringCount++
        }
        rx += 8
      }
      ry += 8
    }
    val surroundBg =
      if (ringCount > 0) (ringSum / ringCount).toInt()
      else if (reticleCount > 0) (reticleSum / reticleCount).toInt()
      else 45

    val normX = ((bestX - xStart).toFloat() / (xEnd - xStart).coerceAtLeast(1)).coerceIn(0f, 1f)
    val normY = ((bestY - yStart).toFloat() / (yEnd - yStart).coerceAtLeast(1)).coerceIn(0f, 1f)

    val (litNow, effectiveThreshold, contrastDelta) =
      decoder.classifyLitState(
        hotspotPeak = hotspotPeak,
        surroundBg = surroundBg,
        autoThreshold = autoThreshold,
        manualThreshold = manualThreshold,
        distanceMode = distanceMode,
      )
    val (sym, text) = decoder.onFrameSample(litNow, System.currentTimeMillis())
    onUpdate(hotspotPeak, surroundBg, contrastDelta, effectiveThreshold, normX, normY, litNow, sym, text)
  } catch (_: Throwable) {
    // Ignore malformed frame buffers safely
  } finally {
    proxy.close()
  }
}
