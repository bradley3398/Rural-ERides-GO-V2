package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.roundToInt

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

  fun decodeSymbol(code: String): String = MORSE_TO_CHAR[code]?.toString() ?: "?"
}

/**
 * Real-time optical pulse timing decoder for CameraX Y-plane luminance frames.
 */
private class OpticalMorseDecoder(
  @Volatile var unitMs: Long = 220L,
) {
  private var isLit = false
  private var lastEdgeMs = 0L
  private var currentSymbol = StringBuilder()
  private var decoded = StringBuilder()
  private var wordSpaceEmitted = true

  @Synchronized
  fun reset() {
    isLit = false
    lastEdgeMs = 0L
    currentSymbol.clear()
    decoded.clear()
    wordSpaceEmitted = true
  }

  @Synchronized
  fun onFrameSample(litNow: Boolean, nowMs: Long): Pair<String, String> {
    if (lastEdgeMs == 0L) {
      isLit = litNow
      lastEdgeMs = nowMs
      return currentSymbol.toString() to decoded.toString()
    }

    val elapsed = (nowMs - lastEdgeMs).coerceAtLeast(0L)
    val minPulseMs = (unitMs * 0.25).toLong().coerceIn(35L, 85L)
    val dotDashBoundaryMs = (unitMs * 2.05).toLong()
    val letterGapMs = (unitMs * 2.25).toLong()
    val wordGapMs = (unitMs * 5.4).toLong()

    if (litNow != isLit) {
      if (isLit && !litNow) {
        // Falling edge: light turned OFF -> classify pulse as dot or dash
        if (elapsed >= minPulseMs) {
          currentSymbol.append(if (elapsed < dotDashBoundaryMs) '.' else '-')
          wordSpaceEmitted = false
        }
      }
      isLit = litNow
      lastEdgeMs = nowMs
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
 * TX: User types any message -> Android rear LED flash transmits International Morse Code.
 */
@Composable
internal fun MorseFlashlightTransmitterPanel(
  hasFlashHardware: Boolean,
  torchError: String?,
  setHardwareTorch: (Boolean) -> Unit,
) {
  var message by remember { mutableStateOf("SOS") }
  var unitMs by remember { mutableIntStateOf(220) } // 140=Fast, 220=Standard, 340=Long-Range
  var loopEnabled by remember { mutableStateOf(false) }
  var isTransmitting by remember { mutableStateOf(false) }
  var lampLit by remember { mutableStateOf(false) }
  var activeCharDisplay by remember { mutableStateOf("") }
  var activeSymbolDisplay by remember { mutableStateOf("") }

  val encodedPreview = remember(message) { MorseCodec.encodeToPreview(message) }
  val presets = remember { listOf("SOS", "OK", "STOP", "HELP", "FLAT TIRE", "LOW BATT", "WAIT") }

  LaunchedEffect(isTransmitting, message, unitMs, loopEnabled) {
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
            else "FLASHLIGHT MORSE TRANSMITTER",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = if (isTransmitting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      if (!hasFlashHardware) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
          Text("No rear LED flash detected — visual preview indicator active.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
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

      // Speed selector (must match receiver speed!) + Loop toggle
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(140 to "Fast", 220 to "Standard", 340 to "Long-Range").forEach { (ms, lbl) ->
          FilterChip(
            selected = unitMs == ms,
            onClick = { unitMs = ms },
            label = { Text(lbl, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
            modifier = Modifier.weight(1f),
          )
        }
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
 * RX: Uses real CameraX Preview + ImageAnalysis to optically read another rider's flashlight Morse code!
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

  var unitMs by remember { mutableIntStateOf(220) }
  var autoThreshold by remember { mutableStateOf(true) }
  var manualThreshold by remember { mutableFloatStateOf(195f) }
  var liveLuma by remember { mutableIntStateOf(0) }
  var liveLit by remember { mutableStateOf(false) }
  var liveSymbols by remember { mutableStateOf("") }
  var decodedMessage by remember { mutableStateOf("") }
  var cameraError by remember { mutableStateOf<String?>(null) }

  val decoder = remember { OpticalMorseDecoder(unitMs = 220L) }
  LaunchedEffect(unitMs) { decoder.unitMs = unitMs.toLong() }

  // Keep latest threshold config readable on analyzer thread
  val thresholdConfig = remember {
    object {
      @Volatile var auto = true
      @Volatile var manual = 195
    }
  }
  LaunchedEffect(autoThreshold, manualThreshold) {
    thresholdConfig.auto = autoThreshold
    thresholdConfig.manual = manualThreshold.roundToInt()
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
          "CAMERA MORSE DECODER (RX)",
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
            if (liveLit) "● LIGHT ON ($liveLuma)" else "○ DARK ($liveLuma)",
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
            "Point your rear camera at another rider's flashlight to decode their Morse code live.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Button(
            onClick = { permLauncher.launch(Manifest.permission.CAMERA) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("morse_rx_grant_camera_button"),
          ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Enable Camera Morse Reader", maxLines = 1, softWrap = false)
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

        // LIVE CAMERAX VIEWFINDER WITH CENTER OPTICAL TARGET RETICLE
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .height(190.dp)
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
                              autoThreshold = thresholdConfig.auto,
                              manualThreshold = thresholdConfig.manual,
                              decoder = decoder,
                              onUpdate = { luma, isLitNow, sym, text ->
                                previewView.post {
                                  liveLuma = luma
                                  liveLit = isLitNow
                                  liveSymbols = sym
                                  decodedMessage = text
                                }
                              },
                            )
                          }
                        }
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
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

          // Center Target Reticle Overlay
          val reticleColor = if (liveLit) Color(0xFF22C55E) else MaterialTheme.colorScheme.primary
          Canvas(modifier = Modifier.fillMaxSize()) {
            val boxW = size.width * 0.24f
            val boxH = size.height * 0.34f
            val left = (size.width - boxW) / 2f
            val top = (size.height - boxH) / 2f
            drawRect(
              color = reticleColor,
              topLeft = Offset(left, top),
              size = Size(boxW, boxH),
              style = Stroke(width = if (liveLit) 3.dp.toPx() else 2.dp.toPx()),
            )
            // Center crosshair ticks
            val cx = size.width / 2f
            val cy = size.height / 2f
            drawLine(reticleColor.copy(alpha = 0.75f), Offset(cx - 10.dp.toPx(), cy), Offset(cx + 10.dp.toPx(), cy), strokeWidth = 1.5.dp.toPx())
            drawLine(reticleColor.copy(alpha = 0.75f), Offset(cx, cy - 10.dp.toPx()), Offset(cx, cy + 10.dp.toPx()), strokeWidth = 1.5.dp.toPx())
          }

          Surface(
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.align(Alignment.BottomCenter).padding(6.dp),
          ) {
            Text(
              "Aim box directly at the other rider's flashlight",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }

        if (cameraError != null) {
          Text(cameraError.orEmpty(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }

        // Real-time Brightness Bar
        LinearProgressIndicator(
          progress = { (liveLuma / 255f).coerceIn(0f, 1f) },
          color = if (liveLit) Color(0xFF22C55E) else MaterialTheme.colorScheme.primary,
          modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
        )

        // Match TX Speed Chips
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(140 to "Fast", 220 to "Standard", 340 to "Long-Range").forEach { (ms, lbl) ->
            FilterChip(
              selected = unitMs == ms,
              onClick = { unitMs = ms },
              label = { Text(lbl, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
              modifier = Modifier.weight(1f),
            )
          }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text(
            if (autoThreshold) "Auto Light Threshold (Adaptive)" else "Manual Threshold: ${manualThreshold.roundToInt()}/255",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
          )
          Switch(checked = autoThreshold, onCheckedChange = { autoThreshold = it })
        }
        if (!autoThreshold) {
          com.example.ui.components.TacticalSlider(value = manualThreshold, onValueChange = { manualThreshold = it }, valueRange = 80f..245f)
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
              text = decodedMessage.ifBlank { "Waiting for light pulses..." },
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

private fun processMorseLuminanceFrame(
  proxy: ImageProxy,
  autoThreshold: Boolean,
  manualThreshold: Int,
  decoder: OpticalMorseDecoder,
  onUpdate: (luma: Int, isLit: Boolean, buffer: String, decoded: String) -> Unit,
) {
  try {
    val yPlane = proxy.planes[0]
    val buffer = yPlane.buffer
    val rowStride = yPlane.rowStride
    val pixelStride = yPlane.pixelStride
    val width = proxy.width
    val height = proxy.height

    val xStart = (width * 0.41f).toInt().coerceIn(0, width - 1)
    val xEnd = (width * 0.59f).toInt().coerceIn(xStart + 1, width)
    val yStart = (height * 0.41f).toInt().coerceIn(0, height - 1)
    val yEnd = (height * 0.59f).toInt().coerceIn(yStart + 1, height)

    var centerSum = 0L
    var centerCount = 0
    var y = yStart
    while (y < yEnd) {
      val rowOffset = y * rowStride
      var x = xStart
      while (x < xEnd) {
        val idx = rowOffset + x * pixelStride
        if (idx in 0 until buffer.limit()) {
          centerSum += (buffer.get(idx).toInt() and 0xFF)
          centerCount++
        }
        x += 4
      }
      y += 4
    }
    val centerAvg = if (centerCount > 0) (centerSum / centerCount).toInt() else 0

    // Sample outer border for adaptive ambient baseline
    var bgSum = 0L
    var bgCount = 0
    if (autoThreshold) {
      var by = 8
      while (by < height - 8) {
        val rowOffset = by * rowStride
        var bx = 8
        while (bx < width - 8) {
          if (bx !in xStart..xEnd || by !in yStart..yEnd) {
            val idx = rowOffset + bx * pixelStride
            if (idx in 0 until buffer.limit()) {
              bgSum += (buffer.get(idx).toInt() and 0xFF)
              bgCount++
            }
          }
          bx += 28
        }
        by += 28
      }
    }
    val bgAvg = if (bgCount > 0) (bgSum / bgCount).toInt() else 60
    val effectiveThreshold =
      if (autoThreshold) max(165, (bgAvg + 58).coerceAtMost(235))
      else manualThreshold

    val litNow = centerAvg >= effectiveThreshold
    val (sym, text) = decoder.onFrameSample(litNow, System.currentTimeMillis())
    onUpdate(centerAvg, litNow, sym, text)
  } catch (_: Throwable) {
    // Ignore malformed frame buffers safely
  } finally {
    proxy.close()
  }
}
