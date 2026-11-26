package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.BoardPost
import com.example.ui.components.TacticalButton
import com.example.ui.components.TacticalCard
import com.example.ui.components.TacticalDialog
import com.example.ui.components.TacticalFilterChip
import com.example.ui.components.TacticalOutlinedButton
import com.example.ui.components.decodeSampledBitmap
import com.example.ui.components.tacticalOutlinedTextFieldColors
import com.example.ui.theme.LocalTacticalChrome
import com.example.viewmodel.CopilotSocialViewModel

val LIVE_BOARD_CATEGORIES =
  listOf(
    "UNIVERSAL",
    "RIDE_REPORTS",
    "BUILDS",
    "GARAGE",
    "GROUP_RIDES",
    "EVENTS",
    "MARKETPLACE",
    "CHARGERS",
    "REVIEWS",
    "CREW",
    "ALERTS",
    "SOS",
    "STOLEN",
    "CONFESSIONS",
    "VIDEO_REQUEST",
  )

@Composable
fun PostComposerDialog(
  socialVm: CopilotSocialViewModel,
  aiModelId: String,
  telemetrySummary: String,
  initialCategory: String = "UNIVERSAL",
  initialTitle: String = "",
  initialBody: String = "",
  initialAttachTelemetry: Boolean = false,
  initialHelpNeeded: Boolean = false,
  initialStolenAlert: Boolean = false,
  onDismiss: () -> Unit,
  onPublish: (
    category: String,
    title: String,
    body: String,
    bitmap: Bitmap?,
    pollQ: String,
    pollOpts: List<String>,
    priceUsd: Double,
    location: String,
    telemetry: String,
    help: Boolean,
    trap: Boolean,
    stolen: Boolean,
  ) -> Unit,
) {
  val ctx = LocalContext.current
  val chrome = LocalTacticalChrome.current
  var category by remember(initialCategory) { mutableStateOf(initialCategory.ifBlank { "UNIVERSAL" }) }
  var title by remember(initialTitle) { mutableStateOf(initialTitle) }
  var body by remember(initialBody) { mutableStateOf(initialBody) }
  var bitmap by remember { mutableStateOf<Bitmap?>(null) }
  var pollQ by remember { mutableStateOf("") }
  var pollOpt1 by remember { mutableStateOf("") }
  var pollOpt2 by remember { mutableStateOf("") }
  var pollOpt3 by remember { mutableStateOf("") }
  var pollOpt4 by remember { mutableStateOf("") }
  var priceStr by remember { mutableStateOf("") }
  var location by remember { mutableStateOf("") }
  var attachTelemetry by remember(initialAttachTelemetry) { mutableStateOf(initialAttachTelemetry) }
  var helpNeeded by remember(initialHelpNeeded) { mutableStateOf(initialHelpNeeded) }
  var speedTrap by remember { mutableStateOf(false) }
  var stolenAlert by remember(initialStolenAlert) { mutableStateOf(initialStolenAlert) }
  var aiTone by remember { mutableStateOf("Tactical") }

  val picker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) bitmap = decodeSampledBitmap(ctx, uri)
    }

  val hashtagSuggestions =
    listOf("#PEV", "#EUC", "#EScooter", "#EMoto", "#VESC", "#72V", "#GroupRide", "#ChargeSpot", "#TrailReport", "#BuildLog", "#RuralERidesGO")

  val draftSafetyCheck =
    remember(title, body, pollQ, pollOpt1, pollOpt2, pollOpt3, pollOpt4, location) {
      com.example.core.FamilySafetyGuard.checkText(title, body, pollQ, pollOpt1, pollOpt2, pollOpt3, pollOpt4, location)
    }

  TacticalDialog(
    onDismissRequest = onDismiss,
    title = "Live Board Transmission",
    subtitle = "Family-Friendly Cloud Sync • ${category.replace('_', ' ')}",
    icon = Icons.Default.CellTower,
    badgeText = "SAFE",
    confirmButton = {
      TacticalButton(
        onClick = {
          onPublish(
            category,
            title,
            body,
            bitmap,
            pollQ,
            listOf(pollOpt1, pollOpt2, pollOpt3, pollOpt4).filter { it.isNotBlank() },
            priceStr.toDoubleOrNull() ?: 0.0,
            location,
            if (attachTelemetry) telemetrySummary else "",
            helpNeeded,
            speedTrap,
            stolenAlert,
          )
        },
        enabled = (title.isNotBlank() || body.isNotBlank()) && draftSafetyCheck.allowed,
        modifier = Modifier.testTag("composer_transmit_button"),
      ) {
        Icon(Icons.Default.Send, null, Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Transmit Live", maxLines = 1, softWrap = false)
      }
    },
    dismissButton = {
      TacticalOutlinedButton(onClick = onDismiss) {
        Text("Cancel", maxLines = 1, softWrap = false)
      }
    },
  ) {
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Surface(
        color =
          if (draftSafetyCheck.allowed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
          else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
        shape = chrome.smallShape,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text =
            if (draftSafetyCheck.allowed) com.example.core.FamilySafetyGuard.COMMUNITY_PROMISE_SHORT
            else draftSafetyCheck.reason,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.SemiBold,
          color =
            if (draftSafetyCheck.allowed) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onErrorContainer,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        )
      }
      Text("Category Channel", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(LIVE_BOARD_CATEGORIES) { cat ->
          TacticalFilterChip(
            selected = category == cat,
            onClick = {
              category = cat
              if (cat == "SOS") helpNeeded = true
              if (cat == "STOLEN") stolenAlert = true
            },
            label = cat.replace('_', ' '),
          )
        }
      }

      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text("Headline") },
        singleLine = true,
        shape = chrome.smallShape,
        colors = tacticalOutlinedTextFieldColors(),
        modifier = Modifier.fillMaxWidth().testTag("composer_title_input"),
      )
      OutlinedTextField(
        value = body,
        onValueChange = { body = it },
        label = { Text("Live Post Body (supports #hashtags)") },
        minLines = 3,
        shape = chrome.smallShape,
        colors = tacticalOutlinedTextFieldColors(),
        modifier = Modifier.fillMaxWidth().testTag("composer_body_input"),
      )

      Text("Quick #Hashtags", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(hashtagSuggestions) { tag ->
          TacticalFilterChip(
            selected = body.contains(tag, ignoreCase = true),
            onClick = {
              if (!body.contains(tag, ignoreCase = true)) {
                body = if (body.isBlank()) tag else "${body.trimEnd()} $tag"
              }
            },
            label = tag,
          )
        }
      }

      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(listOf("Tactical", "Hype", "Concise", "Technical")) { tone ->
          TacticalFilterChip(selected = aiTone == tone, onClick = { aiTone = tone }, label = "Tone: $tone")
        }
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TacticalOutlinedButton(
          onClick = {
            if (body.isNotBlank()) {
              socialVm.polishPostDraft(body, "$aiTone PEV Rider", aiModelId) { body = it }
            }
          },
          modifier = Modifier.weight(1f),
        ) {
          Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("AI Polish", maxLines = 1, softWrap = false)
        }
        TacticalOutlinedButton(
          onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
          modifier = Modifier.weight(1f),
        ) {
          Icon(Icons.Default.PhotoLibrary, null, Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text(if (bitmap != null) "Swap Photo" else "Add Photo", maxLines = 1, softWrap = false)
        }
      }

      if (bitmap != null) {
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .clip(chrome.smallShape)
              .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), chrome.smallShape),
        ) {
          Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Attached photo preview",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(130.dp),
          )
          IconButton(
            onClick = { bitmap = null },
            modifier =
              Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(34.dp)
                .clip(chrome.smallShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
          ) {
            Icon(Icons.Default.Close, contentDescription = "Remove Photo", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
          }
        }
      }

      if (category == "MARKETPLACE" || category == "CHARGERS" || category == "EVENTS" || category == "GROUP_RIDES" || category == "STOLEN") {
        if (category == "MARKETPLACE") {
          OutlinedTextField(
            value = priceStr,
            onValueChange = { priceStr = it },
            label = { Text("Asking Price (USD)") },
            singleLine = true,
            shape = chrome.smallShape,
            colors = tacticalOutlinedTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
          )
        }
        OutlinedTextField(
          value = location,
          onValueChange = { location = it },
          label = { Text("Sector / City / Meetup Spot") },
          singleLine = true,
          shape = chrome.smallShape,
          colors = tacticalOutlinedTextFieldColors(),
          modifier = Modifier.fillMaxWidth(),
        )
      }

      OutlinedTextField(
        value = pollQ,
        onValueChange = { pollQ = it },
        label = { Text("Optional Live Poll Question") },
        singleLine = true,
        shape = chrome.smallShape,
        colors = tacticalOutlinedTextFieldColors(),
        modifier = Modifier.fillMaxWidth(),
      )
      if (pollQ.isNotBlank()) {
        OutlinedTextField(value = pollOpt1, onValueChange = { pollOpt1 = it }, label = { Text("Poll Option 1") }, singleLine = true, shape = chrome.smallShape, colors = tacticalOutlinedTextFieldColors(), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = pollOpt2, onValueChange = { pollOpt2 = it }, label = { Text("Poll Option 2") }, singleLine = true, shape = chrome.smallShape, colors = tacticalOutlinedTextFieldColors(), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = pollOpt3, onValueChange = { pollOpt3 = it }, label = { Text("Poll Option 3 (Optional)") }, singleLine = true, shape = chrome.smallShape, colors = tacticalOutlinedTextFieldColors(), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = pollOpt4, onValueChange = { pollOpt4 = it }, label = { Text("Poll Option 4 (Optional)") }, singleLine = true, shape = chrome.smallShape, colors = tacticalOutlinedTextFieldColors(), modifier = Modifier.fillMaxWidth())
      }

      TacticalCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = attachTelemetry, onCheckedChange = { attachTelemetry = it })
          Column(Modifier.weight(1f)) {
            Text("Attach Live Telemetry Snapshot", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            if (attachTelemetry && telemetrySummary.isNotBlank()) {
              Text(telemetrySummary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
          }
        }
      }

      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        item { TacticalFilterChip(selected = helpNeeded, onClick = { helpNeeded = !helpNeeded }, label = "SOS Help") }
        item { TacticalFilterChip(selected = speedTrap, onClick = { speedTrap = !speedTrap }, label = "Road Alert") }
        item { TacticalFilterChip(selected = stolenAlert, onClick = { stolenAlert = !stolenAlert }, label = "Stolen BOLO") }
      }
    }
  }
}

@Composable
fun EditPostDialog(
  post: BoardPost,
  onDismiss: () -> Unit,
  onSave: (title: String, body: String) -> Unit,
) {
  val chrome = LocalTacticalChrome.current
  var title by remember(post.id) { mutableStateOf(post.title) }
  var body by remember(post.id) { mutableStateOf(post.body) }
  TacticalDialog(
    onDismissRequest = onDismiss,
    title = "Edit Live Transmission",
    subtitle = post.category.replace('_', ' '),
    icon = Icons.Default.Edit,
    confirmButton = {
      TacticalButton(onClick = { onSave(title, body) }, enabled = title.isNotBlank() || body.isNotBlank()) {
        Text("Save Live", maxLines = 1, softWrap = false)
      }
    },
    dismissButton = {
      TacticalOutlinedButton(onClick = onDismiss) {
        Text("Cancel", maxLines = 1, softWrap = false)
      }
    },
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text("Headline") },
        singleLine = true,
        shape = chrome.smallShape,
        colors = tacticalOutlinedTextFieldColors(),
        modifier = Modifier.fillMaxWidth(),
      )
      OutlinedTextField(
        value = body,
        onValueChange = { body = it },
        label = { Text("Message Body") },
        minLines = 3,
        shape = chrome.smallShape,
        colors = tacticalOutlinedTextFieldColors(),
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
fun PevSlangDecoderDialog(onDismiss: () -> Unit) {
  val chrome = LocalTacticalChrome.current
  var filter by remember { mutableStateOf("") }
  val glossary = remember {
    listOf(
      "Voltage Sag" to "Temporary drop in battery pack voltage under heavy throttle load.",
      "LVC (Low Voltage Cutoff)" to "BMS or controller safety shutdown voltage to protect lithium cells.",
      "VESC" to "Open-source programmable brushless ESC with custom phase/battery amp limits.",
      "Phase Amps vs Battery Amps" to "Phase amps drive low-end motor torque; battery amps determine BMS draw & top speed.",
      "Field Weakening" to "Injecting negative D-axis current to push RPM beyond nominal voltage ceiling (adds heat).",
      "PMT Tires" to "High-grip Italian racing slick/semi-slick rubber tires for performance scooters.",
      "Cutout" to "Loss of self-balancing torque on an EUC/Onewheel when exceeding ~85-90% PWM duty cycle.",
      "Bead Seating" to "High-pressure blast to seat tubeless tire beads evenly against the rim lip.",
      "PWM / Headroom" to "Percentage of available motor voltage being used on an EUC; riding >85% risks cutout.",
      "Smart BMS" to "Battery Management System with per-cell voltage balancing, temp sensors, and Bluetooth/UART telemetry.",
      "Statorade" to "Ferrofluid injected inside a direct-drive hub motor to conduct heat from stator windings to the outer shell.",
      "QS8 / XT90-S" to "Anti-spark high-current DC battery plugs preventing arc damage on 60V–134V packs.",
      "20S / 24S / 32S" to "Number of lithium cells wired in series (20S = 72V nominal / 84V full; 24S = 88.8V / 100.8V full).",
      "C-Rate" to "Charge or discharge current divided by pack Ah capacity (e.g., 50A draw on a 25Ah pack = 2C).",
      "Tiltback / Pushback" to "Self-balancing safety warning where an EUC or Onewheel raises the nose to slow you down.",
      "Split Rim" to "Two-piece bolted wheel hub allowing fast tire/tube swaps without prying stiff sidewalls.",
    )
  }
  val shown =
    glossary.filter { (t, d) ->
      filter.isBlank() || t.contains(filter, true) || d.contains(filter, true)
    }
  TacticalDialog(
    onDismissRequest = onDismiss,
    title = "PEV Slang & Tech Decoder",
    subtitle = "${shown.size} field terms loaded",
    icon = Icons.Default.MenuBook,
    badgeText = "${shown.size}",
    confirmButton = {
      TacticalButton(onClick = onDismiss) {
        Text("Close", maxLines = 1, softWrap = false)
      }
    },
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      OutlinedTextField(
        value = filter,
        onValueChange = { filter = it },
        label = { Text("Search PEV terms (e.g. VESC, PWM, LVC)") },
        singleLine = true,
        shape = chrome.smallShape,
        colors = tacticalOutlinedTextFieldColors(),
        modifier = Modifier.fillMaxWidth(),
      )
      LazyColumn(modifier = Modifier.height(310.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(shown) { (term, def) ->
          TacticalCard {
            Text(term, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(def, style = MaterialTheme.typography.bodySmall)
          }
        }
      }
    }
  }
}

