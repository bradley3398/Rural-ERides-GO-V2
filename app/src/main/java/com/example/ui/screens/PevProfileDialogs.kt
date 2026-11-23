package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ElectricScooter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ai.ALL_PEV_VEHICLE_CATEGORIES
import com.example.ai.BatteryPredictionResult
import com.example.ai.PevAutoSpecResult
import com.example.data.PevVehicle
import com.example.data.RiderProfile
import com.example.ui.components.CloudImage
import com.example.ui.components.PilotAvatarThumb
import com.example.ui.components.pevCategoryDisplayLabel
import com.example.ui.components.pevCategoryIcon
import com.example.ui.components.uriToCloudBase64DataUri
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
fun PevVehicleProfileDialog(
  initial: PevVehicle?,
  currentRiderWeightLbs: Double,
  currentAvatarBase64: String,
  onLookupSpecs: (makeAndModel: String, onResult: (PevAutoSpecResult) -> Unit, onError: (String) -> Unit) -> Unit,
  onFetchPevPhoto: (makeAndModel: String, onResult: (String) -> Unit) -> Unit,
  onDeepSearchModels: ((query: String, onResult: (List<PevAutoSpecResult>) -> Unit, onError: (String) -> Unit) -> Unit)? = null,
  onDismiss: () -> Unit,
  onSave: (vehicle: PevVehicle, riderWeightLbs: Double, avatarBase64: String) -> Unit,
) {
  val ctx = LocalContext.current
  var name by remember { mutableStateOf(initial?.name ?: "") }
  var category by remember { mutableStateOf(initial?.category ?: "SCOOTER") }
  var voltage by remember { mutableDoubleStateOf(initial?.voltage ?: 60.0) }
  var voltStr by remember { mutableStateOf((initial?.voltage ?: 60.0).toInt().toString()) }
  val initAh =
    remember(initial) {
      when {
        (initial?.batteryAh ?: 0.0) > 0.5 -> initial!!.batteryAh
        initial != null && initial.voltage > 0.0 -> (initial.batteryWh / initial.voltage).coerceIn(5.0, 200.0)
        else -> 25.0
      }
    }
  var batteryAh by remember { mutableDoubleStateOf(initAh) }
  var ahStr by remember { mutableStateOf("%.1f".format(initAh)) }
  var batteryWh by remember { mutableStateOf((initial?.batteryWh ?: (voltage * initAh).toLong()).toString()) }
  var motorWattsStr by remember { mutableStateOf((initial?.motorWatts ?: 2000).toString()) }
  var topSpeedStr by remember { mutableStateOf((initial?.topSpeedKmh ?: 60.0).roundToInt().toString()) }
  var vehicleWeightStr by remember { mutableStateOf((initial?.vehicleWeightLbs ?: 72.0).roundToInt().toString()) }
  var psiStr by remember { mutableStateOf((initial?.targetPsi ?: 38.0).roundToInt().toString()) }
  var tireSize by remember { mutableStateOf(initial?.tireSize ?: "11x3.5") }
  var intervalStr by remember { mutableStateOf((initial?.serviceIntervalKm ?: 500.0).roundToInt().toString()) }
  var photoBase64 by remember { mutableStateOf(initial?.photoUri ?: "") }
  var brandLogoBase64 by remember { mutableStateOf(initial?.brandLogoUri ?: "") }

  // Rider weight & optional avatar right inside PEV Profile
  var riderWeightLbs by remember { mutableDoubleStateOf(if (currentRiderWeightLbs > 0.0) currentRiderWeightLbs else 175.0) }
  var riderWeightUnitKg by remember { mutableStateOf(false) }
  var riderWeightStr by remember { mutableStateOf(riderWeightLbs.roundToInt().toString()) }
  var avatarBase64 by remember { mutableStateOf(currentAvatarBase64) }

  // Live Gemini Auto-Spec & Photo lookup state
  var specLookupBusy by remember { mutableStateOf(false) }
  var deepTrimsBusy by remember { mutableStateOf(false) }
  var trimCandidates by remember { mutableStateOf<List<PevAutoSpecResult>>(emptyList()) }
  var photoFetchBusy by remember { mutableStateOf(false) }
  var specLookupStatus by remember { mutableStateOf("") }
  var lastAutoFetchedQuery by remember { mutableStateOf(initial?.name?.trim().orEmpty()) }
  var manualSpecTouched by remember { mutableStateOf(initial != null) }

  fun syncFromVoltAndAh(v: Double, ah: Double) {
    val clampedV = v.coerceIn(12.0, 250.0)
    val clampedAh = ah.coerceIn(1.0, 500.0)
    voltage = clampedV
    batteryAh = clampedAh
    voltStr = clampedV.roundToInt().toString()
    ahStr = if (clampedAh % 1.0 == 0.0) clampedAh.toInt().toString() else "%.1f".format(clampedAh)
    batteryWh = (clampedV * clampedAh).roundToInt().coerceIn(50, 15000).toString()
  }

  fun applyAutoSpecs(spec: PevAutoSpecResult) {
    category = spec.category
    syncFromVoltAndAh(spec.voltage, spec.batteryAh)
    batteryWh = spec.batteryWh.toString()
    motorWattsStr = spec.motorWatts.toString()
    topSpeedStr = spec.topSpeedKmh.roundToInt().toString()
    vehicleWeightStr = spec.vehicleWeightLbs.roundToInt().toString()
    tireSize = spec.tireSize
    psiStr = spec.targetPsi.roundToInt().toString()
    if (spec.imageBase64.isNotBlank()) {
      photoBase64 = spec.imageBase64
      specLookupStatus = "Auto-filled specs + PEV profile photo for ${spec.canonicalName} (${spec.voltage.toInt()}V • ${"%.1f".format(spec.batteryAh)}Ah • ${spec.batteryWh}Wh)"
    } else if (photoBase64.isBlank()) {
      photoFetchBusy = true
      specLookupStatus = "Specs filled for ${spec.canonicalName} — fetching PEV profile photo..."
      onFetchPevPhoto(spec.canonicalName.ifBlank { name.trim() }) { b64 ->
        photoFetchBusy = false
        if (b64.isNotBlank()) {
          photoBase64 = b64
          specLookupStatus = "Auto-filled specs + PEV profile photo set for ${spec.canonicalName}!"
        } else {
          specLookupStatus = "Auto-filled specs for ${spec.canonicalName} (${spec.voltage.toInt()}V • ${spec.batteryWh}Wh)."
        }
      }
    } else {
      specLookupStatus = "Auto-filled specs for ${spec.canonicalName} (${spec.voltage.toInt()}V • ${"%.1f".format(spec.batteryAh)}Ah • ${spec.batteryWh}Wh)"
    }
  }

  fun triggerSpecLookup(query: String) {
    val q = query.trim()
    if (q.length < 3) {
      specLookupStatus = "Type a PEV make & model (e.g., NAMI Burn-E 2, Sur-Ron Light Bee X, InMotion V14)."
      return
    }
    specLookupBusy = true
    specLookupStatus = "Retrieving factory specs & photo for \"$q\"..."
    lastAutoFetchedQuery = q
    onLookupSpecs(
      q,
      { res ->
        specLookupBusy = false
        applyAutoSpecs(res)
      },
      { err ->
        specLookupBusy = false
        specLookupStatus = err
      },
    )
  }

  // Automatic debounced spec retrieval + brand logo fetch when typing Make & Model
  LaunchedEffect(name) {
    val q = name.trim()
    if (q.length >= 3) {
      delay(450L)
      if (name.trim() == q) {
        val fetchedLogo = com.example.core.LiveCloudServices.fetchPevBrandLogoBase64(q)
        if (fetchedLogo.isNotBlank()) {
          brandLogoBase64 = fetchedLogo
        }
      }
    }
    if (!manualSpecTouched && q.length >= 6 && q.contains(" ") && q != lastAutoFetchedQuery && !specLookupBusy) {
      delay(900L)
      if (name.trim() == q && !manualSpecTouched && !specLookupBusy) {
        triggerSpecLookup(q)
      }
    }
  }

  val vehiclePhotoPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        val b64 = uriToCloudBase64DataUri(ctx, uri, maxDimension = 520, initialQuality = 65, maxChars = 145_000)
        if (b64 != null) {
          photoBase64 = b64
          specLookupStatus = "PEV photo encoded to cloud Base64 (${b64.length / 1024} KB)."
        }
      }
    }

  val avatarPhotoPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        val b64 = uriToCloudBase64DataUri(ctx, uri, maxDimension = 260, initialQuality = 68, maxChars = 54_000)
        if (b64 != null) {
          avatarBase64 = b64
          specLookupStatus = "Profile picture encoded to cloud Base64 (${b64.length / 1024} KB)."
        }
      }
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.ElectricScooter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(if (initial == null) "PEV & Rider Profile" else "Edit ${initial.name}", fontWeight = FontWeight.Black)
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // 1. MAKE & MODEL + AUTO-RETRIEVE SPECS
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("PEV Make & Model (Auto-Fetches Specs)") },
          placeholder = { Text("e.g. NAMI Burn-E 2 Max, Sur-Ron, Segway GT2") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("pev_make_model_input"),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedButton(
            onClick = { triggerSpecLookup(name) },
            enabled = !specLookupBusy && name.trim().length >= 3,
            modifier = Modifier.weight(1f).testTag("pev_auto_fetch_specs_button"),
          ) {
            if (specLookupBusy) {
              CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp)
              Spacer(Modifier.width(4.dp))
              Text("Fetching...", maxLines = 1)
            } else {
              Icon(Icons.Default.AutoAwesome, null, Modifier.size(15.dp))
              Spacer(Modifier.width(4.dp))
              Text("Auto-Specs", maxLines = 1)
            }
          }
          if (onDeepSearchModels != null) {
            OutlinedButton(
              onClick = {
                val q = name.trim()
                if (q.length < 2) {
                  specLookupStatus = "Type a brand or model (e.g. Dualtron, Sur-Ron, Begode, Lectric) to search all trims."
                  return@OutlinedButton
                }
                deepTrimsBusy = true
                specLookupStatus = "Deep-searching model trims & battery editions for \"$q\"..."
                onDeepSearchModels(
                  q,
                  { list ->
                    deepTrimsBusy = false
                    trimCandidates = list
                    specLookupStatus =
                      if (list.isNotEmpty()) "Found ${list.size} trims for \"$q\". Tap any trim below to auto-fill specs + photo."
                      else "No extra trims found for \"$q\". Try a broader brand name."
                  },
                  { err ->
                    deepTrimsBusy = false
                    specLookupStatus = err
                  },
                )
              },
              enabled = !deepTrimsBusy && name.trim().length >= 2,
              modifier = Modifier.weight(1f).testTag("pev_deep_search_trims_button"),
            ) {
              if (deepTrimsBusy) {
                CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(4.dp))
                Text("Searching...", maxLines = 1)
              } else {
                Icon(Icons.Default.Search, null, Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("Deep Trims", maxLines = 1)
              }
            }
          }
        }
        if (specLookupStatus.isNotBlank()) {
          Text(
            specLookupStatus,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        if (trimCandidates.isNotEmpty()) {
          Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("pev_trim_candidates_list"),
          ) {
            Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                  "SELECT MODEL / BATTERY TRIM (${trimCandidates.size})",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.secondary,
                  fontWeight = FontWeight.Black,
                )
                TextButton(
                  onClick = { trimCandidates = emptyList() },
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                ) { Text("Hide", style = MaterialTheme.typography.labelSmall) }
              }
              trimCandidates.forEach { cand ->
                Surface(
                  onClick = {
                    name = cand.canonicalName
                    manualSpecTouched = true
                    applyAutoSpecs(cand)
                    trimCandidates = emptyList()
                    if (photoBase64.isBlank()) {
                      photoFetchBusy = true
                      onFetchPevPhoto(cand.canonicalName) { b64 ->
                        photoFetchBusy = false
                        if (b64.isNotBlank()) photoBase64 = b64
                      }
                    }
                  },
                  color = MaterialTheme.colorScheme.surface,
                  shape = RoundedCornerShape(8.dp),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(Modifier.fillMaxWidth().padding(8.dp)) {
                    Text(cand.canonicalName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(
                      "${cand.voltage.toInt()}V • ${"%.1f".format(cand.batteryAh)}Ah • ${cand.batteryWh}Wh • ${cand.motorWatts}W • ${cand.tireSize}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }
          }
        }

        // 2. CATEGORY CHIPS (16 PEV TYPES)
        Text("PEV Type (${pevCategoryDisplayLabel(category)})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(ALL_PEV_VEHICLE_CATEGORIES) { cat ->
            FilterChip(
              selected = category == cat,
              onClick = {
                category = cat
                manualSpecTouched = true
              },
              leadingIcon = { Icon(pevCategoryIcon(cat), contentDescription = null, modifier = Modifier.size(16.dp)) },
              label = { Text(pevCategoryDisplayLabel(cat), maxLines = 1, softWrap = false) },
            )
          }
        }

        // 3. INTERACTIVE BATTERY SIZE NUMBER PICKER (VOLTAGE + Ah STEPPER/SLIDER + Wh)
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
          shape = RoundedCornerShape(12.dp),
        ) {
          Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.BatteryChargingFull, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
              Text("BATTERY SIZE PICKER", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            // Voltage quick chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
              items(listOf(36, 48, 52, 60, 72, 84, 100, 126, 134)) { v ->
                FilterChip(
                  selected = voltage.roundToInt() == v,
                  onClick = {
                    manualSpecTouched = true
                    syncFromVoltAndAh(v.toDouble(), batteryAh)
                  },
                  label = { Text("${v}V", style = MaterialTheme.typography.labelSmall) },
                )
              }
            }
            // Battery Ah Number Stepper
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text("Battery Ah:", style = MaterialTheme.typography.labelMedium)
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                  onClick = {
                    manualSpecTouched = true
                    syncFromVoltAndAh(voltage, (batteryAh - 1.0).coerceAtLeast(1.0))
                  },
                  modifier = Modifier.size(48.dp).testTag("pev_ah_minus_button"),
                ) { Icon(Icons.Default.Remove, contentDescription = "Decrease Ah") }
                OutlinedTextField(
                  value = ahStr,
                  onValueChange = { raw ->
                    manualSpecTouched = true
                    ahStr = raw
                    raw.toDoubleOrNull()?.let { parsed ->
                      batteryAh = parsed.coerceIn(1.0, 500.0)
                      batteryWh = (voltage * batteryAh).roundToInt().coerceIn(50, 15000).toString()
                    }
                  },
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                  modifier = Modifier.width(84.dp).testTag("pev_ah_input"),
                )
                IconButton(
                  onClick = {
                    manualSpecTouched = true
                    syncFromVoltAndAh(voltage, (batteryAh + 1.0).coerceAtMost(250.0))
                  },
                  modifier = Modifier.size(48.dp).testTag("pev_ah_plus_button"),
                ) { Icon(Icons.Default.Add, contentDescription = "Increase Ah") }
              }
            }
            Slider(
              value = batteryAh.toFloat().coerceIn(5f, 100f),
              onValueChange = { next ->
                manualSpecTouched = true
                syncFromVoltAndAh(voltage, next.roundToInt().toDouble())
              },
              valueRange = 5f..100f,
              modifier = Modifier.fillMaxWidth().testTag("pev_ah_slider"),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = voltStr,
                onValueChange = { raw ->
                  manualSpecTouched = true
                  voltStr = raw
                  raw.toDoubleOrNull()?.let { v ->
                    voltage = v.coerceIn(12.0, 250.0)
                    batteryWh = (voltage * batteryAh).roundToInt().coerceIn(50, 15000).toString()
                  }
                },
                label = { Text("Voltage (V)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
              )
              OutlinedTextField(
                value = batteryWh,
                onValueChange = { raw ->
                  manualSpecTouched = true
                  batteryWh = raw
                  raw.toDoubleOrNull()?.let { wh ->
                    if (voltage > 0.0) {
                      batteryAh = (wh / voltage).coerceIn(1.0, 500.0)
                      ahStr = "%.1f".format(batteryAh)
                    }
                  }
                },
                label = { Text("Pack Size (Wh)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f).testTag("pev_wh_input"),
              )
            }
          }
        }

        // 4. MOTOR, SPEED, WEIGHT & TIRE SPECS
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = motorWattsStr,
            onValueChange = {
              manualSpecTouched = true
              motorWattsStr = it
            },
            label = { Text("Motor Watts") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = topSpeedStr,
            onValueChange = {
              manualSpecTouched = true
              topSpeedStr = it
            },
            label = { Text("Top km/h") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = tireSize,
            onValueChange = {
              manualSpecTouched = true
              tireSize = it
            },
            label = { Text("Tire Size") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = psiStr,
            onValueChange = {
              manualSpecTouched = true
              psiStr = it
            },
            label = { Text("Target PSI") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = vehicleWeightStr,
            onValueChange = {
              manualSpecTouched = true
              vehicleWeightStr = it
            },
            label = { Text("PEV Weight (lbs)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = intervalStr,
            onValueChange = { intervalStr = it },
            label = { Text("Service km") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
          )
        }

        // 5. RIDER WEIGHT & PROFILE PICTURE (BASE64 CLOUD)
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
          shape = RoundedCornerShape(12.dp),
        ) {
          Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.MonitorWeight, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                Text("RIDER WEIGHT & AVATAR", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
              }
              FilterChip(
                selected = riderWeightUnitKg,
                onClick = {
                  riderWeightUnitKg = !riderWeightUnitKg
                  riderWeightStr =
                    if (riderWeightUnitKg) (riderWeightLbs * 0.453592).roundToInt().toString()
                    else riderWeightLbs.roundToInt().toString()
                },
                label = { Text(if (riderWeightUnitKg) "Unit: KG" else "Unit: LBS", style = MaterialTheme.typography.labelSmall) },
              )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              IconButton(
                onClick = {
                  riderWeightLbs = (riderWeightLbs - 5.0).coerceAtLeast(50.0)
                  riderWeightStr =
                    if (riderWeightUnitKg) (riderWeightLbs * 0.453592).roundToInt().toString()
                    else riderWeightLbs.roundToInt().toString()
                },
                modifier = Modifier.size(36.dp),
              ) { Icon(Icons.Default.Remove, contentDescription = "Decrease rider weight") }
              OutlinedTextField(
                value = riderWeightStr,
                onValueChange = { raw ->
                  riderWeightStr = raw
                  raw.toDoubleOrNull()?.let { v ->
                    riderWeightLbs = (if (riderWeightUnitKg) v / 0.453592 else v).coerceIn(50.0, 600.0)
                  }
                },
                label = { Text(if (riderWeightUnitKg) "Rider Weight (kg)" else "Rider Weight (lbs)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f).testTag("pev_rider_weight_input"),
              )
              IconButton(
                onClick = {
                  riderWeightLbs = (riderWeightLbs + 5.0).coerceAtMost(500.0)
                  riderWeightStr =
                    if (riderWeightUnitKg) (riderWeightLbs * 0.453592).roundToInt().toString()
                    else riderWeightLbs.roundToInt().toString()
                },
                modifier = Modifier.size(36.dp),
              ) { Icon(Icons.Default.Add, contentDescription = "Increase rider weight") }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              PilotAvatarThumb(avatarSource = avatarBase64, callsign = name.ifBlank { "P" }, size = 46.dp)
              OutlinedButton(
                onClick = { avatarPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.weight(1f).testTag("pev_profile_avatar_button"),
              ) {
                Icon(Icons.Default.CameraAlt, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (avatarBase64.isBlank()) "Upload Profile Pic (Base64)" else "Change Profile Pic ✓", maxLines = 1)
              }
            }
          }
        }

        // 6. OFFICIAL BRAND LOGO & VEHICLE PHOTO (AUTO-FETCH OR GALLERY BASE64 CLOUD)
        if (name.trim().length >= 2 || brandLogoBase64.isNotBlank()) {
          val resolvedBrandName =
            remember(name) { com.example.core.LiveCloudServices.resolvePevBrandInfo(name.ifBlank { "PEV" }).first }
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("pev_brand_logo_preview_card"),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              com.example.ui.components.PevBrandLogoBadge(
                brandOrModel = name.ifBlank { "PEV" },
                logoDataOrUrl = brandLogoBase64,
                size = 38.dp,
              )
              Column(Modifier.weight(1f)) {
                Text(
                  text = "BRAND EMBLEM: ${resolvedBrandName.uppercase()}",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Black,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = if (brandLogoBase64.isNotBlank()) "Official brand logo fetched & linked to PEV profile ✓" else "Auto-detecting official brand logo...",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
        if (photoBase64.isNotBlank()) {
          CloudImage(
            source = photoBase64,
            contentDescription = "Vehicle Photo Preview",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(10.dp)),
          )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = {
              val q = name.trim()
              if (q.length < 3) {
                specLookupStatus = "Type a PEV Make & Model first to retrieve its picture."
                return@OutlinedButton
              }
              photoFetchBusy = true
              specLookupStatus = "Searching live web image for \"$q\"..."
              onFetchPevPhoto(q) { b64 ->
                photoFetchBusy = false
                if (b64.isNotBlank()) {
                  photoBase64 = b64
                  specLookupStatus = "PEV picture retrieved & encoded to Base64 (${b64.length / 1024} KB)."
                } else {
                  specLookupStatus = "No public raster photo matched \"$q\". Use Gallery button to upload one."
                }
              }
            },
            enabled = !photoFetchBusy && name.trim().length >= 3,
            modifier = Modifier.weight(1f).testTag("pev_auto_fetch_photo_button"),
          ) {
            if (photoFetchBusy) {
              CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
              Spacer(Modifier.width(4.dp))
              Text("Finding...", maxLines = 1)
            } else {
              Icon(Icons.Default.AutoAwesome, null, Modifier.size(15.dp))
              Spacer(Modifier.width(4.dp))
              Text("Fetch PEV Pic", maxLines = 1)
            }
          }
          OutlinedButton(
            onClick = { vehiclePhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.weight(1f).testTag("pev_vehicle_photo_button"),
          ) {
            Icon(Icons.Default.CameraAlt, null, Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (photoBase64.isBlank()) "Upload Photo" else "Photo Set ✓", maxLines = 1)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val finalVolt = voltStr.toDoubleOrNull()?.coerceIn(12.0, 250.0) ?: voltage
          val finalAh = ahStr.toDoubleOrNull()?.coerceIn(1.0, 500.0) ?: batteryAh
          val finalWh = batteryWh.toLongOrNull()?.coerceIn(50L, 15000L) ?: (finalVolt * finalAh).toLong().coerceIn(50L, 15000L)
          onSave(
            (initial ?: PevVehicle()).copy(
              name = name.trim().ifBlank { "Custom PEV" },
              category = category,
              batteryWh = finalWh,
              batteryAh = finalAh,
              voltage = finalVolt,
              motorWatts = motorWattsStr.toIntOrNull()?.coerceIn(0, 50000) ?: 0,
              topSpeedKmh = topSpeedStr.toDoubleOrNull()?.coerceIn(0.0, 250.0) ?: 0.0,
              vehicleWeightLbs = vehicleWeightStr.toDoubleOrNull()?.coerceIn(0.0, 1000.0) ?: 0.0,
              targetPsi = psiStr.toDoubleOrNull()?.coerceIn(5.0, 100.0) ?: 36.0,
              serviceIntervalKm = intervalStr.toDoubleOrNull()?.coerceIn(25.0, 50000.0) ?: 500.0,
              tireSize = tireSize.trim().ifBlank { "10x3.0" },
              photoUri = photoBase64,
              brandLogoUri = brandLogoBase64,
            ),
            riderWeightLbs,
            avatarBase64,
          )
        },
        modifier = Modifier.testTag("save_pev_profile_button"),
      ) { Text("Save Profile") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
fun PilotProfileDialog(
  profile: RiderProfile?,
  fallbackWeightLbs: Int,
  onDismiss: () -> Unit,
  onSave: (riderWeightLbs: Double, avatarBase64: String) -> Unit,
) {
  val ctx = LocalContext.current
  val initWeight =
    remember(profile, fallbackWeightLbs) {
      if ((profile?.riderWeightLbs ?: 0.0) > 10.0) profile!!.riderWeightLbs else fallbackWeightLbs.toDouble().coerceAtLeast(100.0)
    }
  var weightLbs by remember { mutableDoubleStateOf(initWeight) }
  var useKg by remember { mutableStateOf(false) }
  var weightText by remember { mutableStateOf(initWeight.roundToInt().toString()) }
  var avatarBase64 by remember { mutableStateOf(profile?.avatarBase64.orEmpty()) }
  var uploadInfo by remember { mutableStateOf("") }

  val picker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        val encoded = uriToCloudBase64DataUri(ctx, uri, maxDimension = 260, initialQuality = 68, maxChars = 54_000)
        if (encoded != null) {
          avatarBase64 = encoded
          uploadInfo = "Encoded Base64 photo (${encoded.length / 1024} KB) — ready for cloud sync."
        }
      }
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Pilot Profile & Weight", fontWeight = FontWeight.Black) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        PilotAvatarThumb(
          avatarSource = avatarBase64,
          callsign = profile?.callsign ?: "PILOT",
          size = 86.dp,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
          Text(
            text = "${profile?.callsign?.ifBlank { "PILOT" } ?: "PILOT"} (Permanent Callsign)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.testTag("pilot_pick_avatar_button"),
          ) {
            Icon(Icons.Default.CameraAlt, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (avatarBase64.isBlank()) "Choose Photo" else "Change Photo")
          }
          if (avatarBase64.isNotBlank()) {
            TextButton(onClick = { avatarBase64 = "" }) { Text("Clear") }
          }
        }
        if (uploadInfo.isNotBlank()) {
          Text(uploadInfo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Rider Weight (Used for Range & PSI)", style = MaterialTheme.typography.labelMedium)
          FilterChip(
            selected = useKg,
            onClick = {
              useKg = !useKg
              weightText =
                if (useKg) (weightLbs * 0.453592).roundToInt().toString()
                else weightLbs.roundToInt().toString()
            },
            label = { Text(if (useKg) "KG" else "LBS", style = MaterialTheme.typography.labelSmall) },
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          IconButton(onClick = {
            weightLbs = (weightLbs - 5.0).coerceAtLeast(50.0)
            weightText = if (useKg) (weightLbs * 0.453592).roundToInt().toString() else weightLbs.roundToInt().toString()
          }) { Icon(Icons.Default.Remove, contentDescription = "Minus 5") }
          OutlinedTextField(
            value = weightText,
            onValueChange = { raw ->
              weightText = raw
              raw.toDoubleOrNull()?.let { v ->
                weightLbs = (if (useKg) v / 0.453592 else v).coerceIn(50.0, 600.0)
              }
            },
            label = { Text(if (useKg) "Weight (kg)" else "Weight (lbs)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).testTag("pilot_weight_input"),
          )
          IconButton(onClick = {
            weightLbs = (weightLbs + 5.0).coerceAtMost(500.0)
            weightText = if (useKg) (weightLbs * 0.453592).roundToInt().toString() else weightLbs.roundToInt().toString()
          }) { Icon(Icons.Default.Add, contentDescription = "Plus 5") }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(weightLbs, avatarBase64) },
        modifier = Modifier.testTag("save_pilot_profile_button"),
      ) { Text("Save to Cloud") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
fun StartRideBatteryDialog(
  vehicle: PevVehicle?,
  initialPct: Int,
  onDismiss: () -> Unit,
  onConfirmStart: (startBatteryPct: Int) -> Unit,
) {
  var pct by remember { mutableIntStateOf(initialPct.coerceIn(0, 100)) }
  var pctStr by remember { mutableStateOf(pct.toString()) }
  val packWh = (vehicle?.batteryWh ?: 1200L).toDouble()
  val startWh = (packWh * (pct / 100.0)).roundToInt()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.BatteryChargingFull, null, tint = MaterialTheme.colorScheme.primary)
        Text("Start-of-Ride Battery %", fontWeight = FontWeight.Black)
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Enter your ${vehicle?.name ?: "PEV"} starting charge before launching live GPS recording.",
          style = MaterialTheme.typography.bodyMedium,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          IconButton(onClick = {
            pct = (pct - 5).coerceAtLeast(0)
            pctStr = pct.toString()
          }) { Icon(Icons.Default.Remove, contentDescription = "Minus 5 percent") }
          OutlinedTextField(
            value = pctStr,
            onValueChange = { raw ->
              pctStr = raw.filter { it.isDigit() }.take(3)
              pctStr.toIntOrNull()?.let { pct = it.coerceIn(0, 100) }
            },
            label = { Text("Start %") },
            suffix = { Text("%") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(120.dp).testTag("start_ride_battery_input"),
          )
          IconButton(onClick = {
            pct = (pct + 5).coerceAtMost(100)
            pctStr = pct.toString()
          }) { Icon(Icons.Default.Add, contentDescription = "Plus 5 percent") }
        }
        Slider(
          value = pct.toFloat(),
          onValueChange = {
            pct = it.roundToInt().coerceIn(0, 100)
            pctStr = pct.toString()
          },
          valueRange = 0f..100f,
          modifier = Modifier.fillMaxWidth().testTag("start_ride_battery_slider"),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(listOf(100, 90, 80, 70, 60, 50)) { preset ->
            FilterChip(
              selected = pct == preset,
              onClick = {
                pct = preset
                pctStr = preset.toString()
              },
              label = { Text("$preset%") },
            )
          }
        }
        Text(
          "Estimated Available Pack Energy: $startWh Wh / ${packWh.toInt()} Wh",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirmStart(pct) },
        modifier = Modifier.testTag("confirm_start_ride_button"),
      ) { Text("Launch Ride ($pct%)") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
fun EndRideBatteryDialog(
  vehicle: PevVehicle?,
  startBatteryPct: Int,
  estimatedRemainingPct: Int,
  missionTitle: String,
  onTitleChange: (String) -> Unit,
  aiPrediction: BatteryPredictionResult? = null,
  predictionBusy: Boolean = false,
  onRequestAiPrediction: () -> Unit = {},
  onDismiss: () -> Unit,
  onConfirmSave: (endBatteryPct: Int) -> Unit,
) {
  var endPct by remember { mutableIntStateOf(estimatedRemainingPct.coerceIn(0, 100)) }
  var endPctStr by remember { mutableStateOf(endPct.toString()) }
  var userAdjusted by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    onRequestAiPrediction()
  }

  LaunchedEffect(aiPrediction?.predictedRemainingPct) {
    if (!userAdjusted && aiPrediction != null) {
      endPct = aiPrediction.predictedRemainingPct.coerceIn(0, 100)
      endPctStr = endPct.toString()
    }
  }

  val packWh = (vehicle?.batteryWh ?: 1200L).toDouble()
  val usedPct = (startBatteryPct - endPct).coerceAtLeast(0)
  val usedWh = (packWh * (usedPct / 100.0)).roundToInt()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.BatteryChargingFull, null, tint = MaterialTheme.colorScheme.primary)
        Text("End Ride & Remaining Charge", fontWeight = FontWeight.Black)
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = missionTitle,
          onValueChange = onTitleChange,
          label = { Text("Mission Title") },
          placeholder = { Text("Evening Country Loop") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("end_ride_title_input"),
        )

        // AI LEARNED RIDE-HISTORY BATTERY PREDICTION CARD
        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth().testTag("ai_battery_prediction_card"),
        ) {
          Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                Text(
                  text = "AI RIDE-RECORD PREDICTION",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.secondary,
                  fontWeight = FontWeight.Black,
                )
              }
              if (predictionBusy) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
              }
            }
            if (aiPrediction != null) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Predicted: ${aiPrediction.predictedRemainingPct}% (${aiPrediction.confidence})",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                TextButton(
                  onClick = {
                    endPct = aiPrediction.predictedRemainingPct.coerceIn(0, 100)
                    endPctStr = endPct.toString()
                    userAdjusted = true
                  },
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                ) { Text("Apply ${aiPrediction.predictedRemainingPct}%") }
              }
              Text(
                text = "Learned: ${"%.1f".format(aiPrediction.learnedWhPerKm)} Wh/km from ${aiPrediction.ridesAnalyzed} ride(s) • ${aiPrediction.reasoning}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            } else {
              Text(
                text = "Analyzing your past ride logs to predict remaining charge...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        Text(
          "Confirm or adjust your remaining PEV battery percentage after this session:",
          style = MaterialTheme.typography.bodyMedium,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          IconButton(onClick = {
            userAdjusted = true
            endPct = (endPct - 5).coerceAtLeast(0)
            endPctStr = endPct.toString()
          }) { Icon(Icons.Default.Remove, contentDescription = "Minus 5 percent") }
          OutlinedTextField(
            value = endPctStr,
            onValueChange = { raw ->
              userAdjusted = true
              endPctStr = raw.filter { it.isDigit() }.take(3)
              endPctStr.toIntOrNull()?.let { endPct = it.coerceIn(0, 100) }
            },
            label = { Text("Remaining %") },
            suffix = { Text("%") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(130.dp).testTag("end_ride_battery_input"),
          )
          IconButton(onClick = {
            userAdjusted = true
            endPct = (endPct + 5).coerceAtMost(100)
            endPctStr = endPct.toString()
          }) { Icon(Icons.Default.Add, contentDescription = "Plus 5 percent") }
        }
        Slider(
          value = endPct.toFloat(),
          onValueChange = {
            userAdjusted = true
            endPct = it.roundToInt().coerceIn(0, 100)
            endPctStr = endPct.toString()
          },
          valueRange = 0f..100f,
          modifier = Modifier.fillMaxWidth().testTag("end_ride_battery_slider"),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(listOf(85, 70, 55, 40, 25, 10)) { preset ->
            FilterChip(
              selected = endPct == preset,
              onClick = {
                userAdjusted = true
                endPct = preset
                endPctStr = preset.toString()
              },
              label = { Text("$preset%") },
            )
          }
        }
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "Started: $startBatteryPct% → Remaining: $endPct% • Used: $usedPct% (~$usedWh Wh)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(10.dp),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirmSave(endPct) },
        modifier = Modifier.testTag("confirm_save_ride_button"),
      ) { Text("Save Session") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Resume Ride") } },
  )
}
