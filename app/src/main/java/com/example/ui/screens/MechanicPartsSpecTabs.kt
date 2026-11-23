package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ElectricScooter
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.core.PevBatteryPhysics
import com.example.data.PevCatalogData
import com.example.data.PevPartItem
import com.example.data.PevSpecRecord
import com.example.data.PevStoreDirectory
import com.example.data.PevVehicle
import com.example.data.UiState
import com.example.ui.components.PevProductVisualBanner
import com.example.ui.components.SocialShareDialog
import com.example.ui.components.SocialSharePayload
import com.example.ui.components.StoreBrandLogo
import com.example.ui.components.TacticalCard
import com.example.viewmodel.CockpitViewModel
import com.example.viewmodel.CopilotSocialViewModel
import com.example.viewmodel.MechanicViewModel
import com.example.viewmodel.SessionViewModel
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun SpecEngineTab(
  mechanicVm: MechanicViewModel,
  cockpitVm: CockpitViewModel? = null,
  sessionVm: SessionViewModel? = null,
  socialVm: CopilotSocialViewModel? = null,
  aiModelId: String = com.example.ai.GeminiModelCatalog.GEMINI_3_5_FLASH_LITE,
) {
  val ctx = LocalContext.current
  val specState by mechanicVm.specState.collectAsStateWithLifecycle()
  val specDigDeeperBusy by mechanicVm.specDigDeeperBusy.collectAsStateWithLifecycle()
  val specLoadMoreStatus by mechanicVm.specLoadMoreStatus.collectAsStateWithLifecycle()
  val specSearchHistory by mechanicVm.specSearchHistory.collectAsStateWithLifecycle()
  val productImagesCache by mechanicVm.productImagesCache.collectAsStateWithLifecycle()
  val verifiedOffersCache by mechanicVm.verifiedStoreOffersCache.collectAsStateWithLifecycle()
  val stockBusyKeys by mechanicVm.stockCheckBusyKeys.collectAsStateWithLifecycle()
  val stockCheckedKeys by mechanicVm.stockCheckedKeys.collectAsStateWithLifecycle()
  val autoFindStores by mechanicVm.autoFindStores.collectAsStateWithLifecycle()
  val compared by mechanicVm.comparedSpecs.collectAsStateWithLifecycle()

  var query by remember { mutableStateOf("") }
  var category by remember { mutableStateOf("ALL") }
  var minVoltage by remember { mutableIntStateOf(0) }
  var maxPriceUsd by remember { mutableIntStateOf(0) }
  var sortMode by remember { mutableStateOf(PevCatalogData.SpecSortMode.DEFAULT) }
  var riderLbs by remember { mutableStateOf("175") }
  var cruiseMph by remember { mutableStateOf("25") }
  var ruralSurface by remember { mutableStateOf("Paved") }
  var sharePayload by remember { mutableStateOf<SocialSharePayload?>(null) }
  var expandedSpec by remember { mutableStateOf<PevSpecRecord?>(null) }
  var expandSpecFocusVideos by remember { mutableStateOf(false) }
  var savedSpecKeys by remember { mutableStateOf(setOf<String>()) }
  var savedOnly by remember { mutableStateOf(false) }
  var chargerAmps by remember { mutableIntStateOf(5) }
  val activeUid = sessionVm?.activeUser?.collectAsStateWithLifecycle()?.value?.uid.orEmpty()

  val csvExporter =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
      if (uri != null) {
        ctx.contentResolver.openOutputStream(uri)?.use { it.write(mechanicVm.exportSpecsCsv().toByteArray()) }
        android.widget.Toast.makeText(ctx, "Exported PEV Specs CSV!", android.widget.Toast.LENGTH_SHORT).show()
      }
    }

  val allCategories = listOf("ALL", "EBIKE", "SCOOTER", "EUC", "ONEWHEEL", "EMOTO", "SURRON", "ESKATE", "EMOPED", "ETRIKE", "ECARGO", "OTHER")
  val voltageOptions = listOf(0 to "All V", 48 to "48V+", 60 to "60V+", 72 to "72V+", 100 to "100V+")
  val specPriceCaps = listOf(0 to "Any $", 1000 to "<$1k", 2500 to "<$2.5k", 5000 to "<$5k")

  val rawList = (specState as? UiState.Success)?.data.orEmpty()
  val filteredSpecs =
    remember(rawList, query, category, minVoltage, maxPriceUsd, sortMode, savedSpecKeys, savedOnly) {
      val base =
        PevCatalogData.filterSpecCatalog(
          specs = rawList,
          query = query,
          category = category,
          minVoltage = minVoltage,
          sortMode = sortMode,
        )
      base.filter { spec ->
        val savedOk = !savedOnly || "${spec.brand}_${spec.name}" in savedSpecKeys
        val priceVal = PevCatalogData.extractPriceDouble(spec.price)
        val priceOk = maxPriceUsd == 0 || priceVal <= 0.0 || priceVal <= maxPriceUsd.toDouble()
        savedOk && priceOk
      }
    }

  Box(Modifier.fillMaxSize()) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item {
      TacticalCard(highlight = true) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text(
            "BARN SPEC ENGINE (${filteredSpecs.size} RIGS)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          TextButton(
            onClick = {
              query = ""
              category = "ALL"
              minVoltage = 0
              savedOnly = false
              sortMode = PevCatalogData.SpecSortMode.DEFAULT
              mechanicVm.resetSpecCatalog()
            },
            modifier = Modifier.testTag("reset_spec_catalog_button"),
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Reset Catalog", maxLines = 1, softWrap = false)
          }
        }
        Text(
          "Exact S-count full/storage/cutoff voltages, CC+CV charge math, rural gravel range & live in-stock store checks (buttons only show when verified in stock).",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          label = { Text("Search eBike, eScooter, EUC, Onewheel, eMoto, Sur-Ron...") },
          singleLine = true,
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
          modifier = Modifier.fillMaxWidth().testTag("spec_query_input"),
        )
        if (specSearchHistory.isNotEmpty()) {
          Spacer(Modifier.height(4.dp))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            item {
              Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(specSearchHistory) { past ->
              AssistChip(
                onClick = {
                  query = past
                  mechanicVm.searchSpecs(past, category, aiModelId, deepSearch = true)
                },
                label = { Text(past, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
              )
            }
          }
        }
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(allCategories) { cat ->
            FilterChip(
              selected = category == cat,
              onClick = { category = cat },
              label = { Text(cat, maxLines = 1, softWrap = false) },
              modifier = Modifier.testTag("spec_cat_${cat.lowercase()}"),
            )
          }
        }
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          item {
            FilterChip(
              selected = autoFindStores,
              onClick = { mechanicVm.toggleAutoFindStores() },
              label = { Text(if (autoFindStores) "Auto-Find Stores: ON" else "Auto-Find Stores: OFF", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
              leadingIcon = {
                Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(15.dp))
              },
              modifier = Modifier.testTag("spec_auto_find_stores_chip"),
            )
          }
          item {
            AssistChip(
              onClick = { mechanicVm.scanSpecsStores(filteredSpecs, forceRefresh = true) },
              label = { Text("Scan Visible Stores", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
              leadingIcon = {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
              },
              modifier = Modifier.testTag("spec_scan_visible_stores_chip"),
            )
          }
          item {
            FilterChip(
              selected = savedOnly,
              onClick = { savedOnly = !savedOnly },
              label = { Text("Saved (${savedSpecKeys.size})", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
              leadingIcon = {
                Icon(
                  if (savedOnly) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                  contentDescription = null,
                  modifier = Modifier.size(15.dp),
                )
              },
              modifier = Modifier.testTag("spec_saved_only_chip"),
            )
          }
          items(specPriceCaps) { (cap, lbl) ->
            FilterChip(
              selected = maxPriceUsd == cap,
              onClick = { maxPriceUsd = cap },
              label = { Text(lbl, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
            )
          }
          items(voltageOptions) { (v, lbl) ->
            FilterChip(
              selected = minVoltage == v,
              onClick = { minVoltage = v },
              label = { Text(lbl, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
            )
          }
          items(listOf(2, 5, 10, 15)) { amps ->
            FilterChip(
              selected = chargerAmps == amps,
              onClick = { chargerAmps = amps },
              label = { Text("${amps}A Chg", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
            )
          }
        }
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(PevCatalogData.SpecSortMode.entries.toList()) { sm ->
            FilterChip(
              selected = sortMode == sm,
              onClick = { sortMode = sm },
              label = { Text("Sort: ${sm.label}", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
            )
          }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = { mechanicVm.searchSpecs(query.ifBlank { category.takeIf { it != "ALL" } ?: "performance PEV" }, category, aiModelId, deepSearch = true) },
            modifier = Modifier.weight(1.2f).testTag("search_specs_button"),
          ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(5.dp))
            Text("Deep Search", maxLines = 1, softWrap = false)
          }
          OutlinedButton(
            onClick = { mechanicVm.digDeeperSpecs(query, category, aiModelId) },
            enabled = !specDigDeeperBusy,
            modifier = Modifier.weight(1f).testTag("spec_load_more_top_button"),
          ) {
            if (specDigDeeperBusy) {
              CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp)
              Spacer(Modifier.width(5.dp))
              Text("Loading...", maxLines = 1, softWrap = false)
            } else {
              Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(17.dp))
              Spacer(Modifier.width(4.dp))
              Text("Load More", maxLines = 1, softWrap = false)
            }
          }
        }
        if (specLoadMoreStatus.isNotBlank()) {
          Spacer(Modifier.height(4.dp))
          Text(specLoadMoreStatus, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
      }
    }

    if (compared.isNotEmpty()) {
      item {
        val fastest = compared.maxByOrNull { PevCatalogData.extractFirstInt(it.topSpeed) }
        val longest = compared.maxByOrNull { PevCatalogData.extractFirstInt(it.range) }
        val biggestPack = compared.maxByOrNull { PevCatalogData.extractWh(it.battery) }
        val surfFactor =
          when (ruralSurface) {
            "Hard Dirt" -> 0.88
            "Loose Gravel" -> 0.76
            "Mud / Hills" -> 0.64
            else -> 1.0
          }

        TacticalCard(highlight = true) {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
              "BARN COMPARE MATRIX (${compared.size}/4)",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              TextButton(onClick = { mechanicVm.clearCompareSpecs() }, modifier = Modifier.testTag("clear_compare_button")) {
                Text("Clear", maxLines = 1, softWrap = false)
              }
              OutlinedButton(
                onClick = { csvExporter.launch("pev_specs_compare.csv") },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("CSV", maxLines = 1, softWrap = false)
              }
            }
          }
          Spacer(Modifier.height(4.dp))
          Text(
            "Winners • Fastest: ${fastest?.name ?: "-"} | Range: ${longest?.name ?: "-"} | Pack: ${biggestPack?.name ?: "-"}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
          )
          Spacer(Modifier.height(6.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = riderLbs, onValueChange = { riderLbs = it }, label = { Text("Rider lbs", maxLines = 1) }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(value = cruiseMph, onValueChange = { cruiseMph = it }, label = { Text("Cruise MPH", maxLines = 1) }, singleLine = true, modifier = Modifier.weight(1f))
          }
          Spacer(Modifier.height(6.dp))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf("Paved", "Hard Dirt", "Loose Gravel", "Mud / Hills")) { surf ->
              FilterChip(
                selected = ruralSurface == surf,
                onClick = { ruralSurface = surf },
                label = { Text(surf, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
              )
            }
          }
          Spacer(Modifier.height(8.dp))
          compared.forEach { s ->
            val wt = riderLbs.toDoubleOrNull() ?: 175.0
            val spd = cruiseMph.toDoubleOrNull() ?: 25.0
            val estRangeMi = PevCatalogData.estimateSimulatedRangeMiles(s, wt, spd) * surfFactor
            val chem = if (s.battery.contains("lfp", true)) "LFP" else "NMC"
            val prof = PevBatteryPhysics.packProfile(s.nominalVoltage.toDouble().coerceAtLeast(24.0), chem)
            val wh = PevCatalogData.extractWh(s.battery).toDouble()
            val chgHrs =
              if (wh > 0) {
                PevBatteryPhysics.estimateChargeTimeHours(
                  batteryWh = wh,
                  ratedVoltage = prof.nominalPackV,
                  chargerAmps = chargerAmps.toDouble(),
                  startSocPct = 0.0,
                  targetSocPct = 100.0,
                  chemistry = chem,
                )
              } else 0.0
            val wLbs = PevCatalogData.extractFirstInt(s.weight).toDouble()
            val whLb = if (wh > 0 && wLbs > 0) "${"%.1f".format(wh / wLbs)} Wh/lb" else ""
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            ) {
              Column(Modifier.padding(8.dp)) {
                Text("${s.brand} ${s.name} (${s.price})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(
                  "${prof.seriesCount}S (${"%.1f".format(prof.fullChargeV)}V Full) • ${s.topSpeed} • Sim $ruralSurface ~${"%.1f".format(estRangeMi)} mi",
                  style = MaterialTheme.typography.bodySmall,
                )
                Text(
                  buildString {
                    append("${s.motorPower} • ${s.battery}")
                    if (whLb.isNotBlank()) append(" • $whLb")
                    if (chgHrs > 0) append(" • ~${"%.1f".format(chgHrs)}h @ ${chargerAmps}A")
                  },
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    }

    when (val st = specState) {
      is UiState.Loading -> item {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
          CircularProgressIndicator()
        }
      }
      is UiState.Error -> item {
        Text(st.message, color = MaterialTheme.colorScheme.error)
      }
      is UiState.Success -> {
        if (filteredSpecs.isEmpty()) {
          item {
            TacticalCard {
              Text(
                "No built-in specs matched \"$query\" ($category). Tap \"Query Live AI + Web Specs\" above to fetch live specs for any eBike, eScooter, EUC, Onewheel, or eMoto!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        } else {
          items(filteredSpecs, key = { "${it.brand}_${it.name}_${it.category}" }) { spec ->
            val specKey = "${spec.brand}_${spec.name}"
            val imgCacheKey = "spec_${spec.brand}_${spec.name}".lowercase()
            val cleanSpecQuery = remember(spec) { PevStoreDirectory.buildCleanSpecQuery(spec) }
            LaunchedEffect(imgCacheKey) {
              mechanicVm.loadProductImages(
                cacheKey = imgCacheKey,
                query = "${spec.brand} ${spec.name} ${spec.category.lowercase()} electric",
                seedUrls = spec.imageUrls,
              )
            }
            LaunchedEffect(imgCacheKey, autoFindStores) {
              if (autoFindStores) {
                mechanicVm.verifyInStockStores(
                  cacheKey = imgCacheKey,
                  productTitle = cleanSpecQuery,
                  candidateStores = PevStoreDirectory.candidateStoresForSpec(spec),
                )
              }
            }
            val cardPhotos = (productImagesCache[imgCacheKey].orEmpty() + spec.imageUrls.filter { it.startsWith("http") }).distinct()
            val verifiedOffers = verifiedOffersCache[imgCacheKey].orEmpty()
            val checkingStock = imgCacheKey in stockBusyKeys
            val stockChecked = imgCacheKey in stockCheckedKeys
            val isComp = compared.any { it.name == spec.name }
            val isSaved = specKey in savedSpecKeys
            val surfFactor =
              when (ruralSurface) {
                "Hard Dirt" -> 0.88
                "Loose Gravel" -> 0.76
                "Mud / Hills" -> 0.64
                else -> 1.0
              }
            val simRangeMi = PevCatalogData.estimateSimulatedRangeMiles(spec, riderLbs.toDoubleOrNull() ?: 175.0, cruiseMph.toDoubleOrNull() ?: 25.0) * surfFactor
            val whVal = PevCatalogData.extractWh(spec.battery).toDouble()
            val packChem = if (spec.battery.contains("lfp", true)) "LFP" else "NMC"
            val packProf = remember(spec.nominalVoltage, packChem) {
              PevBatteryPhysics.packProfile(spec.nominalVoltage.toDouble().coerceAtLeast(24.0), packChem)
            }
            val priceVal = PevCatalogData.extractPriceDouble(spec.price)
            val weightLbsVal = PevCatalogData.extractFirstInt(spec.weight).toDouble()
            val topMphVal = PevCatalogData.extractFirstInt(spec.topSpeed).toDouble()
            val dollarsPerWh = if (whVal > 0 && priceVal > 0) priceVal / whVal else 0.0
            val whPerLb = if (whVal > 0 && weightLbsVal > 0) whVal / weightLbsVal else 0.0
            val dollarsPerMph = if (topMphVal > 0 && priceVal > 0) priceVal / topMphVal else 0.0
            val estChargeHrs =
              if (whVal > 0) {
                PevBatteryPhysics.estimateChargeTimeHours(
                  batteryWh = whVal,
                  ratedVoltage = packProf.nominalPackV,
                  chargerAmps = chargerAmps.toDouble(),
                  startSocPct = 0.0,
                  targetSocPct = 100.0,
                  chemistry = packChem,
                )
              } else 0.0
            val directUrl = verifiedOffers.firstOrNull()?.url ?: PevStoreDirectory.directLinkForSpec(spec)
            TacticalCard(
              highlight = isComp || isSaved,
              onClick = { expandedSpec = spec },
              modifier = Modifier.testTag("spec_card_${spec.name.lowercase().replace(" ", "_")}"),
            ) {
              PevProductVisualBanner(
                title = "${spec.brand} ${spec.name}",
                subtitle = "${spec.motorPower} • ${spec.battery}",
                category = spec.category,
                badgeLeft = "${spec.category} • ${packProf.seriesCount}S (${"%.1f".format(packProf.fullChargeV)}V Full) • ${spec.ipRating}",
                badgeRight = spec.price,
                imageUrl = cardPhotos.firstOrNull().orEmpty(),
                brandName = spec.brand,
              )
              if (cardPhotos.size > 1) {
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  items(cardPhotos.take(5)) { thumbUrl ->
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                      modifier = Modifier.width(68.dp).height(46.dp).clickable { expandedSpec = spec },
                    ) {
                      AsyncImage(
                        model = thumbUrl,
                        contentDescription = "${spec.brand} ${spec.name} photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                      )
                    }
                  }
                }
              }
              Spacer(Modifier.height(6.dp))
              Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                  Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                  )
                  Spacer(Modifier.width(4.dp))
                  Text(
                    text = if (cardPhotos.size > 1) "${cardPhotos.size} photos • Tap card for full specs & videos" else "Tap card for 24+ specs, rural range & videos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Icon(
                  Icons.Default.OpenInFull,
                  contentDescription = "Expand specs",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(14.dp),
                )
              }
              Spacer(Modifier.height(6.dp))
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                  Text("Top Speed: ${spec.topSpeed} | Range: ${spec.range} (Sim ~${"%.0f".format(simRangeMi)} mi)", style = MaterialTheme.typography.bodyMedium)
                  Text(
                    "Pack: ${packProf.seriesCount}S ${packProf.chemistry} • ${"%.1f".format(packProf.fullChargeV)}V Full / ${"%.1f".format(packProf.storageV)}V Storage / ${"%.1f".format(packProf.cutoffEmptyV)}V Cutoff",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                  )
                  Text("Brakes: ${spec.brakingSystem} | Suspension: ${spec.suspensionType}", style = MaterialTheme.typography.labelSmall)
                  Text("Tires: ${spec.tireProfile} | Weight: ${spec.weight} | Max Load: ${spec.maxLoad}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(
                  onClick = {
                    savedSpecKeys = if (isSaved) savedSpecKeys - specKey else savedSpecKeys + specKey
                  },
                  modifier = Modifier.testTag("bookmark_spec_${spec.name.lowercase().replace(" ", "_")}"),
                ) {
                  Icon(
                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = if (isSaved) "Remove Bookmark" else "Bookmark Spec",
                    tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              if (whVal > 0) {
                Spacer(Modifier.height(6.dp))
                Surface(
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                  shape = MaterialTheme.shapes.small,
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Text(
                    text =
                      buildString {
                        if (dollarsPerWh > 0) append("$${"%.2f".format(dollarsPerWh)}/Wh")
                        if (whPerLb > 0) append("  •  ${"%.1f".format(whPerLb)} Wh/lb")
                        if (dollarsPerMph > 0) append("  •  $${"%.0f".format(dollarsPerMph)}/mph")
                        if (estChargeHrs > 0) append("  •  ~${"%.1f".format(estChargeHrs)}h CC+CV @ ${chargerAmps}A")
                      },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                  )
                }
              }
              // LIVE IN-STOCK STORE VERIFICATION ONLY (NEVER SHOW UNVERIFIED STORE BUTTONS!)
              Spacer(Modifier.height(6.dp))
              if (verifiedOffers.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  items(verifiedOffers, key = { it.storeId }) { offer ->
                    Button(
                      onClick = { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(offer.url))) } },
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                      modifier = Modifier.heightIn(min = 48.dp).testTag("spec_instock_store_${offer.storeId}"),
                    ) {
                      StoreBrandLogo(storeId = offer.storeId, storeName = offer.storeName, size = 18.dp)
                      Spacer(Modifier.width(5.dp))
                      Text(
                        "${offer.storeName} • In Stock${if (offer.priceHint.isNotBlank()) " ${offer.priceHint}" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                  }
                }
              } else if (checkingStock) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp)
                  Spacer(Modifier.width(6.dp))
                  Text(
                    "Auto-finding verified in-stock stores...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                  )
                }
              } else if (stockChecked) {
                Text(
                  "No stores verified in stock right now (store buttons hidden).",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Spacer(Modifier.height(8.dp))
              LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                  Button(
                    onClick = {
                      expandSpecFocusVideos = false
                      expandedSpec = spec
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("expand_spec_btn_${spec.name.lowercase().replace(" ", "_")}"),
                  ) {
                    Icon(Icons.Default.OpenInFull, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Full Specs", maxLines = 1, softWrap = false)
                  }
                }
                item {
                  OutlinedButton(
                    onClick = {
                      mechanicVm.verifyInStockStores(
                        cacheKey = imgCacheKey,
                        productTitle = cleanSpecQuery,
                        candidateStores = PevStoreDirectory.candidateStoresForSpec(spec),
                        forceRefresh = stockChecked,
                      )
                    },
                    enabled = !checkingStock,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("check_stock_spec_${spec.name.lowercase().replace(" ", "_")}"),
                  ) {
                    if (checkingStock) {
                      CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                      Spacer(Modifier.width(4.dp))
                      Text("Checking...", maxLines = 1, softWrap = false)
                    } else {
                      Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(Modifier.width(4.dp))
                      Text(if (stockChecked) "Recheck Stock" else "Check Stock", maxLines = 1, softWrap = false)
                    }
                  }
                }
                item {
                  OutlinedButton(
                    onClick = {
                      expandSpecFocusVideos = true
                      expandedSpec = spec
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("watch_spec_videos_${spec.name.lowercase().replace(" ", "_")}"),
                  ) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Videos", maxLines = 1, softWrap = false)
                  }
                }
                item {
                  OutlinedButton(
                    onClick = { mechanicVm.toggleCompareSpec(spec) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                  ) {
                    Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isComp) "Remove" else "Compare", maxLines = 1, softWrap = false)
                  }
                }
                if (cockpitVm != null) {
                  item {
                    OutlinedButton(
                      onClick = {
                        val wh = PevCatalogData.extractWh(spec.battery).toLong().coerceAtLeast(360L)
                        val v = spec.nominalVoltage.toDouble().coerceAtLeast(36.0)
                        cockpitVm.saveVehicle(
                          uid = activeUid.ifBlank { "local_pilot" },
                          vehicle =
                            PevVehicle(
                              id = UUID.randomUUID().toString(),
                              userId = activeUid.ifBlank { "local_pilot" },
                              name = "${spec.brand} ${spec.name}".trim(),
                              category = spec.category,
                              batteryWh = wh,
                              voltage = v,
                              tireSize = spec.tireProfile.take(24),
                              targetPsi = if (spec.category == "EBIKE") 30.0 else 42.0,
                              odometerKm = 0.0,
                            ),
                        )
                        android.widget.Toast.makeText(ctx, "Added ${spec.brand} ${spec.name} to Garage!", android.widget.Toast.LENGTH_SHORT).show()
                      },
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                      modifier = Modifier.testTag("add_spec_to_garage_${spec.name.lowercase().replace(" ", "_")}"),
                    ) {
                      Icon(com.example.ui.components.pevCategoryIcon(spec.category), contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(Modifier.width(4.dp))
                      Text("+ Garage", maxLines = 1, softWrap = false)
                    }
                  }
                }
                item {
                  OutlinedButton(
                    onClick = {
                      sharePayload =
                        SocialSharePayload(
                          title = "${spec.brand} ${spec.name} (${spec.category})",
                          text = "Top Speed: ${spec.topSpeed} | Range: ${spec.range}\nMotor: ${spec.motorPower} | Battery: ${spec.battery}\nBrakes: ${spec.brakingSystem} | Price: ${spec.price}",
                          url = directUrl,
                        )
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                  ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share", maxLines = 1, softWrap = false)
                  }
                }
              }
            }
          }
        }

        // PROMINENT LOAD MORE RESULTS FOOTER (REPEATABLE PAGINATION)
        item(key = "spec_load_more_footer") {
          TacticalCard(highlight = true) {
            Text(
              "SHOWING ${filteredSpecs.size} OF ${rawList.size} LOADED RIGS",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
            )
            if (specLoadMoreStatus.isNotBlank()) {
              Text(specLoadMoreStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
              Text(
                "Want to see more models, trims, and brands? Load another batch of unique live web + AI PEV specs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Spacer(Modifier.height(8.dp))
            Button(
              onClick = { mechanicVm.digDeeperSpecs(query, category, aiModelId) },
              enabled = !specDigDeeperBusy,
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("spec_load_more_bottom_button"),
            ) {
              if (specDigDeeperBusy) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Loading More PEV Specs...")
              } else {
                Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Load More Results (+20 More Rigs)")
              }
            }
          }
        }
      }
    }
  }

  expandedSpec?.let { spec ->
    val specKey = "${spec.brand}_${spec.name}"
    val specCacheKey = "spec_${spec.brand}_${spec.name}".lowercase()
    val isComp = compared.any { it.name == spec.name }
    val isSaved = specKey in savedSpecKeys
    val directUrl = verifiedOffersCache[specCacheKey]?.firstOrNull()?.url ?: PevStoreDirectory.directLinkForSpec(spec)
    ExpandedPevSpecDetailSheet(
      spec = spec,
      mechanicVm = mechanicVm,
      socialVm = socialVm,
      isCompared = isComp,
      isSaved = isSaved,
      onToggleCompare = { mechanicVm.toggleCompareSpec(spec) },
      onToggleSave = {
        savedSpecKeys = if (isSaved) savedSpecKeys - specKey else savedSpecKeys + specKey
      },
      onAddToGarage =
        cockpitVm?.let { vm ->
          {
            val wh = PevCatalogData.extractWh(spec.battery).toLong().coerceAtLeast(360L)
            val v = spec.nominalVoltage.toDouble().coerceAtLeast(36.0)
            vm.saveVehicle(
              uid = activeUid.ifBlank { "local_pilot" },
              vehicle =
                PevVehicle(
                  id = UUID.randomUUID().toString(),
                  userId = activeUid.ifBlank { "local_pilot" },
                  name = "${spec.brand} ${spec.name}".trim(),
                  category = spec.category,
                  batteryWh = wh,
                  voltage = v,
                  tireSize = spec.tireProfile.take(24),
                  targetPsi = if (spec.category == "EBIKE") 30.0 else 42.0,
                  odometerKm = 0.0,
                ),
            )
            android.widget.Toast.makeText(ctx, "Added ${spec.brand} ${spec.name} to Garage!", android.widget.Toast.LENGTH_SHORT).show()
          }
        },
      onShare = {
        sharePayload =
          SocialSharePayload(
            title = "${spec.brand} ${spec.name} (${spec.category})",
            text = "Top Speed: ${spec.topSpeed} | Range: ${spec.range}\nMotor: ${spec.motorPower} | Battery: ${spec.battery}\nBrakes: ${spec.brakingSystem} | Price: ${spec.price}",
            url = directUrl,
          )
      },
      initialFocusVideos = expandSpecFocusVideos,
      onDismiss = {
        expandedSpec = null
        expandSpecFocusVideos = false
      },
    )
  }
  }

  sharePayload?.let { payload ->
    SocialShareDialog(payload = payload, onDismiss = { sharePayload = null })
  }
}

@Composable
fun PartsFinderTab(
  mechanicVm: MechanicViewModel,
  cockpitVm: CockpitViewModel? = null,
  socialVm: CopilotSocialViewModel? = null,
  aiModelId: String = com.example.ai.GeminiModelCatalog.GEMINI_3_5_FLASH_LITE,
) {
  val ctx = LocalContext.current
  val partsState by mechanicVm.partsState.collectAsStateWithLifecycle()
  val partsDigDeeperBusy by mechanicVm.partsDigDeeperBusy.collectAsStateWithLifecycle()
  val partsLoadMoreStatus by mechanicVm.partsLoadMoreStatus.collectAsStateWithLifecycle()
  val partsSearchHistory by mechanicVm.partsSearchHistory.collectAsStateWithLifecycle()
  val productImagesCache by mechanicVm.productImagesCache.collectAsStateWithLifecycle()
  val verifiedOffersCache by mechanicVm.verifiedStoreOffersCache.collectAsStateWithLifecycle()
  val stockBusyKeys by mechanicVm.stockCheckBusyKeys.collectAsStateWithLifecycle()
  val stockCheckedKeys by mechanicVm.stockCheckedKeys.collectAsStateWithLifecycle()
  val autoFindStores by mechanicVm.autoFindStores.collectAsStateWithLifecycle()
  val bomCart by mechanicVm.bomCart.collectAsStateWithLifecycle()
  val bomQty by mechanicVm.bomQuantities.collectAsStateWithLifecycle()
  val vehicles = cockpitVm?.vehicles?.collectAsStateWithLifecycle()?.value.orEmpty()
  val activeVehicle = vehicles.firstOrNull()

  var query by remember { mutableStateOf("") }
  var make by remember { mutableStateOf("") }
  var model by remember { mutableStateOf("") }
  var tireSize by remember { mutableStateOf("") }
  var vehicleType by remember { mutableStateOf("ALL") }
  var category by remember { mutableStateOf("ALL") }
  var maxPartPriceUsd by remember { mutableIntStateOf(0) }
  var region by remember { mutableStateOf("US") }
  var taxPct by remember { mutableStateOf("8.0") }
  var markupPct by remember { mutableStateOf("0") }
  var sharePayload by remember { mutableStateOf<SocialSharePayload?>(null) }
  var expandedPart by remember { mutableStateOf<PevPartItem?>(null) }
  var difficultyFilter by remember { mutableStateOf("ALL") }
  var partTypeFilter by remember { mutableStateOf("ALL") }
  var savedPartKeys by remember { mutableStateOf(setOf<String>()) }
  var savedPartsOnly by remember { mutableStateOf(false) }
  var showFitmentGuide by remember { mutableStateOf(false) }

  val csvExporter =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
      if (uri != null) {
        ctx.contentResolver.openOutputStream(uri)?.use { it.write(mechanicVm.exportBomCsv().toByteArray()) }
        android.widget.Toast.makeText(ctx, "Exported Parts BOM CSV!", android.widget.Toast.LENGTH_SHORT).show()
      }
    }

  val vehicleTypes = listOf("ALL", "EBIKE", "SCOOTER", "EUC", "ONEWHEEL", "EMOTO", "SURRON", "ESKATE", "EMOPED", "ETRIKE", "OTHER")
  val difficultyOptions = listOf("ALL", "Easy", "Moderate", "Advanced")
  val partTypeOptions = listOf("ALL", "PERFORMANCE_UPGRADE", "MAINTENANCE", "OEM")
  val partPriceCaps = listOf(0 to "Any $", 50 to "<$50", 150 to "<$150", 400 to "<$400")
  val partCategories =
    listOf(
      "ALL",
      "Tires & Tubes",
      "Brakes & Rotors",
      "Controllers & BMS",
      "Batteries & Chargers",
      "Suspension",
      "Drivetrain & Motors",
      "Bearings & Hardware",
      "Cockpit & Lights",
      "Safety & Armor",
    )

  val rawParts = (partsState as? UiState.Success)?.data.orEmpty()
  val filteredParts =
    remember(rawParts, query, make, model, tireSize, vehicleType, category, maxPartPriceUsd, difficultyFilter, partTypeFilter, savedPartKeys, savedPartsOnly) {
      val combinedQuery = listOf(query, make, model, tireSize).filter { it.isNotBlank() }.joinToString(" ")
      PevCatalogData.filterPartCatalog(
        parts = rawParts,
        query = combinedQuery,
        vehicleType = vehicleType,
        category = category,
      ).filter { part ->
        val diffOk = difficultyFilter == "ALL" || part.difficulty.equals(difficultyFilter, ignoreCase = true)
        val typeOk = partTypeFilter == "ALL" || part.partType.equals(partTypeFilter, ignoreCase = true)
        val savedOk = !savedPartsOnly || part.partName in savedPartKeys
        val pVal = PevCatalogData.extractPriceDouble(part.estimatedPrice)
        val priceOk = maxPartPriceUsd == 0 || pVal <= 0.0 || pVal <= maxPartPriceUsd.toDouble()
        diffOk && typeOk && savedOk && priceOk
      }
    }

  Box(Modifier.fillMaxSize()) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item {
      TacticalCard(highlight = true) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text(
            "BARN PARTS FINDER (${filteredParts.size} PARTS)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          Row {
            if (activeVehicle != null) {
              TextButton(
                onClick = {
                  val pieces = activeVehicle.name.split(" ", limit = 2)
                  make = pieces.firstOrNull().orEmpty()
                  model = pieces.getOrNull(1).orEmpty()
                  tireSize = activeVehicle.tireSize
                  vehicleType = activeVehicle.category.uppercase().takeIf { it in vehicleTypes } ?: "ALL"
                },
                modifier = Modifier.testTag("use_active_pev_parts_button"),
              ) {
                Text("Use Active PEV", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall)
              }
            }
            TextButton(
              onClick = {
                query = ""
                make = ""
                model = ""
                tireSize = ""
                vehicleType = "ALL"
                category = "ALL"
                difficultyFilter = "ALL"
                partTypeFilter = "ALL"
                savedPartsOnly = false
                mechanicVm.resetPartsCatalog()
              },
              modifier = Modifier.testTag("reset_parts_catalog_button"),
            ) {
              Text("Reset", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall)
            }
          }
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          label = { Text("Search PEV Parts (e.g. PMT tire, brake pad, VESC, Surron belt, EUC pad)") },
          singleLine = true,
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
          modifier = Modifier.fillMaxWidth().testTag("parts_query_input"),
        )
        if (partsSearchHistory.isNotEmpty()) {
          Spacer(Modifier.height(4.dp))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            item {
              Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(partsSearchHistory) { past ->
              AssistChip(
                onClick = {
                  query = past
                  mechanicVm.searchParts(past, make, model, tireSize, category, region, vehicleType, aiModelId, deepSearch = true)
                },
                label = { Text(past, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
              )
            }
          }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = make, onValueChange = { make = it }, label = { Text("Make") }, singleLine = true, modifier = Modifier.weight(1f))
          OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model") }, singleLine = true, modifier = Modifier.weight(1f))
          OutlinedTextField(value = tireSize, onValueChange = { tireSize = it }, label = { Text("Tire/Spec") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Text("VEHICLE TYPE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(vehicleTypes) { vt ->
            FilterChip(
              selected = vehicleType == vt,
              onClick = { vehicleType = vt },
              label = { Text(vt, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
              modifier = Modifier.testTag("parts_vtype_${vt.lowercase()}"),
            )
          }
        }
        Spacer(Modifier.height(4.dp))
        Text("PART CATEGORY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(partCategories) { cat ->
            FilterChip(
              selected = category == cat,
              onClick = { category = cat },
              label = { Text(cat, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
            )
          }
        }
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          item {
            FilterChip(
              selected = autoFindStores,
              onClick = { mechanicVm.toggleAutoFindStores() },
              label = { Text(if (autoFindStores) "Auto-Find Stores: ON" else "Auto-Find Stores: OFF", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
              leadingIcon = {
                Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(15.dp))
              },
              modifier = Modifier.testTag("parts_auto_find_stores_chip"),
            )
          }
          item {
            AssistChip(
              onClick = { mechanicVm.scanPartsStores(filteredParts, forceRefresh = true) },
              label = { Text("Scan Visible Stores", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
              leadingIcon = {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
              },
              modifier = Modifier.testTag("parts_scan_visible_stores_chip"),
            )
          }
          item {
            FilterChip(
              selected = savedPartsOnly,
              onClick = { savedPartsOnly = !savedPartsOnly },
              label = { Text("Saved (${savedPartKeys.size})", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
              leadingIcon = {
                Icon(
                  if (savedPartsOnly) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                  contentDescription = null,
                  modifier = Modifier.size(15.dp),
                )
              },
            )
          }
          items(partPriceCaps) { (cap, lbl) ->
            FilterChip(
              selected = maxPartPriceUsd == cap,
              onClick = { maxPartPriceUsd = cap },
              label = { Text(lbl, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
            )
          }
          items(difficultyOptions) { diff ->
            FilterChip(
              selected = difficultyFilter == diff,
              onClick = { difficultyFilter = diff },
              label = { Text(if (diff == "ALL") "Any Skill" else diff, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
            )
          }
          items(partTypeOptions) { pt ->
            val lbl =
              when (pt) {
                "PERFORMANCE_UPGRADE" -> "Upgrade"
                "MAINTENANCE" -> "Maint"
                "OEM" -> "OEM"
                else -> "All Types"
              }
            FilterChip(
              selected = partTypeFilter == pt,
              onClick = { partTypeFilter = pt },
              label = { Text(lbl, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelSmall) },
            )
          }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = {
              val effectiveQuery =
                listOf(query, vehicleType.takeIf { it != "ALL" }).filterNotNull().filter { it.isNotBlank() }.joinToString(" ")
              mechanicVm.searchParts(effectiveQuery.ifBlank { category }, make, model, tireSize, category, region, vehicleType, aiModelId, deepSearch = true)
            },
            modifier = Modifier.weight(1.2f).testTag("search_parts_button"),
          ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(5.dp))
            Text("Deep Search", maxLines = 1, softWrap = false)
          }
          OutlinedButton(
            onClick = {
              val effectiveQuery =
                listOf(query, vehicleType.takeIf { it != "ALL" }).filterNotNull().filter { it.isNotBlank() }.joinToString(" ")
              mechanicVm.digDeeperParts(effectiveQuery.ifBlank { category }, make, model, tireSize, category, region, vehicleType, aiModelId)
            },
            enabled = !partsDigDeeperBusy,
            modifier = Modifier.weight(1f).testTag("parts_load_more_top_button"),
          ) {
            if (partsDigDeeperBusy) {
              CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp)
              Spacer(Modifier.width(5.dp))
              Text("Loading...", maxLines = 1, softWrap = false)
            } else {
              Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(17.dp))
              Spacer(Modifier.width(4.dp))
              Text("Load More", maxLines = 1, softWrap = false)
            }
          }
        }
        if (partsLoadMoreStatus.isNotBlank()) {
          Spacer(Modifier.height(4.dp))
          Text(partsLoadMoreStatus, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
      }
    }

    // Store buttons ONLY appear per-item after live HTTP in-stock verification (no unverified store directory card).

    item {
      TacticalCard {
        Row(
          Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("PEV FITMENT, TORQUE & CONNECTOR CHEAT SHEET", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
          }
          TextButton(onClick = { showFitmentGuide = !showFitmentGuide }, modifier = Modifier.testTag("toggle_fitment_guide_btn")) {
            Text(if (showFitmentGuide) "Hide" else "Show", maxLines = 1, softWrap = false)
          }
        }
        if (showFitmentGuide) {
          Spacer(Modifier.height(6.dp))
          Text("• Connectors: XT60 (60A cont) | XT90-S Anti-Spark (90A) | QS8-S (180A+ 72V rigs)", style = MaterialTheme.typography.labelSmall)
          Text("• Rotors (6-Bolt 44mm BCD): 140/160mm Scooters • 180/203mm eBikes • 220mm eMotos", style = MaterialTheme.typography.labelSmall)
          Text("• Torque (Blue Loctite 242): Rotor M5 6Nm • Stem M6 10-12Nm • Hub Axle M12/M14 45-65Nm", style = MaterialTheme.typography.labelSmall)
          Text("• Target PSI: eScooter 10-11\" (42-50 PSI) • EUC (28-36 PSI) • Onewheel (14-20 PSI) • Fat eBike (18-26 PSI)", style = MaterialTheme.typography.labelSmall)
        }
      }
    }

    if (bomCart.isNotEmpty()) {
      item {
        val subtotal =
          bomCart.sumOf { p ->
            val q = bomQty[p.partName] ?: 1
            PevCatalogData.extractPriceDouble(p.estimatedPrice) * q
          }
        val tax = subtotal * ((taxPct.toDoubleOrNull() ?: 0.0) / 100.0)
        val markup = subtotal * ((markupPct.toDoubleOrNull() ?: 0.0) / 100.0)
        val totalLbs = bomCart.sumOf { p -> p.weightLbs * (bomQty[p.partName] ?: 1) }
        val estShip = 8.50 + totalLbs * 1.45
        val totalUnits = bomCart.sumOf { bomQty[it.partName] ?: 1 }

        TacticalCard(highlight = true) {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(Modifier.width(6.dp))
              Text("BOM CART ($totalUnits units)", style = MaterialTheme.typography.titleMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              IconButton(onClick = { mechanicVm.clearBomCart() }, modifier = Modifier.testTag("clear_bom_button")) {
                Icon(Icons.Default.Delete, contentDescription = "Clear BOM Cart", modifier = Modifier.size(18.dp))
              }
              OutlinedButton(
                onClick = { csvExporter.launch("pev_parts_bom.csv") },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("CSV", maxLines = 1, softWrap = false)
              }
            }
          }
          Spacer(Modifier.height(6.dp))
          bomCart.forEach { p ->
            val q = bomQty[p.partName] ?: 1
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            ) {
              Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(Modifier.weight(1f)) {
                  Text(p.partName, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                  Text("${p.estimatedPrice} x $q", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                  IconButton(onClick = { mechanicVm.adjustBomQuantity(p.partName, -1) }) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease quantity", modifier = Modifier.size(16.dp))
                  }
                  Text("$q", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                  IconButton(onClick = { mechanicVm.adjustBomQuantity(p.partName, 1) }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase quantity", modifier = Modifier.size(16.dp))
                  }
                }
              }
            }
          }
          Spacer(Modifier.height(6.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = taxPct, onValueChange = { taxPct = it }, label = { Text("Tax %") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = markupPct, onValueChange = { markupPct = it }, label = { Text("Shop Markup %") }, modifier = Modifier.weight(1f))
          }
          Spacer(Modifier.height(6.dp))
          Text(
            "Subtotal: $${"%.2f".format(subtotal)} | Tax+Markup: $${"%.2f".format(tax + markup)} | Est Ship (${"%.1f".format(totalLbs)} lb): $${"%.2f".format(estShip)} | GRAND TOTAL: $${"%.2f".format(subtotal + tax + markup + estShip)}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }

    when (val st = partsState) {
      is UiState.Loading -> item {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
          CircularProgressIndicator()
        }
      }
      is UiState.Error -> item { Text(st.message, color = MaterialTheme.colorScheme.error) }
      is UiState.Success -> {
        if (filteredParts.isEmpty()) {
          item {
            TacticalCard {
              Text(
                "No built-in parts matched your filters. Tap \"Search Live AI + Web Verified Parts\" above to locate exact fitment & vendors!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        } else {
          items(filteredParts, key = { "${it.partName}_${it.exactDimensions}" }) { part ->
            val partImgKey = "part_${part.partName}_${part.exactDimensions}".lowercase()
            val cleanPartQuery = remember(part) { PevStoreDirectory.buildCleanPartQuery(part) }
            val seedUrls = listOfNotNull(part.imageUrl.takeIf { it.startsWith("http") })
            LaunchedEffect(partImgKey) {
              mechanicVm.loadProductImages(
                cacheKey = partImgKey,
                query = "${part.partName} ${part.recommendedBrands}",
                seedUrls = seedUrls,
              )
            }
            LaunchedEffect(partImgKey, autoFindStores) {
              if (autoFindStores) {
                mechanicVm.verifyInStockStores(
                  cacheKey = partImgKey,
                  productTitle = cleanPartQuery,
                  candidateStores = PevStoreDirectory.candidateStoresForPart(part),
                )
              }
            }
            val cardPhotos = (productImagesCache[partImgKey].orEmpty() + seedUrls).distinct()
            val verifiedOffers = verifiedOffersCache[partImgKey].orEmpty()
            val checkingStock = partImgKey in stockBusyKeys
            val stockChecked = partImgKey in stockCheckedKeys
            val inCart = bomCart.any { it.partName == part.partName }
            val isSaved = part.partName in savedPartKeys
            val directBuyUrl = verifiedOffers.firstOrNull()?.url ?: PevStoreDirectory.directLinkForPart(part)
            val inferredCat =
              when {
                part.compatibility.contains("EUC", true) -> "EUC"
                part.compatibility.contains("ONEWHEEL", true) -> "ONEWHEEL"
                part.compatibility.contains("SURRON", true) || part.compatibility.contains("EMOTO", true) -> "EMOTO"
                part.compatibility.contains("ESKATE", true) -> "ESKATE"
                part.compatibility.contains("EBIKE", true) -> "EBIKE"
                else -> "SCOOTER"
              }
            TacticalCard(
              highlight = inCart || isSaved,
              onClick = { expandedPart = part },
              modifier = Modifier.testTag("part_card_${part.partName.lowercase().replace(" ", "_")}"),
            ) {
              PevProductVisualBanner(
                title = part.partName,
                subtitle = "${part.exactDimensions} • ${part.recommendedBrands}",
                category = inferredCat,
                badgeLeft = "${part.category} • ${part.sourcePlatform}",
                badgeRight = part.estimatedPrice,
                imageUrl = cardPhotos.firstOrNull().orEmpty(),
              )
              if (cardPhotos.size > 1) {
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  items(cardPhotos.take(5)) { thumbUrl ->
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                      modifier = Modifier.width(68.dp).height(46.dp).clickable { expandedPart = part },
                    ) {
                      AsyncImage(
                        model = thumbUrl,
                        contentDescription = "${part.partName} photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                      )
                    }
                  }
                }
              }
              Spacer(Modifier.height(6.dp))
              Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                  Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                  )
                  Spacer(Modifier.width(4.dp))
                  Text(
                    text = if (cardPhotos.size > 1) "${cardPhotos.size} photos • Tap card for gallery & videos" else "Tap card for multi-photos, fitment & install videos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Icon(
                  Icons.Default.OpenInFull,
                  contentDescription = "Expand part details",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(14.dp),
                )
              }
              Spacer(Modifier.height(6.dp))
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                  Text("Dimensions: ${part.exactDimensions}", style = MaterialTheme.typography.bodyMedium)
                  Text("Fits: ${part.compatibility} • Install: ${part.difficulty} • ${"%.1f".format(part.weightLbs)} lb", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(
                  onClick = {
                    savedPartKeys = if (isSaved) savedPartKeys - part.partName else savedPartKeys + part.partName
                  },
                ) {
                  Icon(
                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = if (isSaved) "Remove Saved Part" else "Save Part",
                    tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              // LIVE IN-STOCK STORE VERIFICATION ONLY (NEVER SHOW UNVERIFIED STORE BUTTONS!)
              Spacer(Modifier.height(6.dp))
              if (verifiedOffers.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  items(verifiedOffers, key = { it.storeId }) { offer ->
                    Button(
                      onClick = { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(offer.url))) } },
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                      modifier = Modifier.heightIn(min = 48.dp).testTag("part_instock_store_${offer.storeId}"),
                    ) {
                      StoreBrandLogo(storeId = offer.storeId, storeName = offer.storeName, size = 18.dp)
                      Spacer(Modifier.width(5.dp))
                      Text(
                        "${offer.storeName} • In Stock${if (offer.priceHint.isNotBlank()) " ${offer.priceHint}" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                  }
                }
              } else if (checkingStock) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp)
                  Spacer(Modifier.width(6.dp))
                  Text(
                    "Auto-finding verified in-stock stores...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                  )
                }
              } else if (stockChecked) {
                Text(
                  "No stores verified in stock right now (store buttons hidden).",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Spacer(Modifier.height(8.dp))
              LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                  Button(
                    onClick = { expandedPart = part },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("expand_part_btn_${part.partName.lowercase().replace(" ", "_")}"),
                  ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Photos & Specs", maxLines = 1, softWrap = false)
                  }
                }
                item {
                  OutlinedButton(
                    onClick = {
                      mechanicVm.verifyInStockStores(
                        cacheKey = partImgKey,
                        productTitle = cleanPartQuery,
                        candidateStores = PevStoreDirectory.candidateStoresForPart(part),
                        forceRefresh = stockChecked,
                      )
                    },
                    enabled = !checkingStock,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("check_stock_part_${part.partName.lowercase().replace(" ", "_")}"),
                  ) {
                    if (checkingStock) {
                      CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                      Spacer(Modifier.width(4.dp))
                      Text("Checking...", maxLines = 1, softWrap = false)
                    } else {
                      Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(Modifier.width(4.dp))
                      Text(if (stockChecked) "Recheck Stock" else "Check Stock", maxLines = 1, softWrap = false)
                    }
                  }
                }
                item {
                  OutlinedButton(
                    onClick = { mechanicVm.toggleBomItem(part) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                  ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (inCart) "In BOM" else "+ BOM", maxLines = 1, softWrap = false)
                  }
                }
                item {
                  OutlinedButton(
                    onClick = {
                      sharePayload =
                        SocialSharePayload(
                          title = part.partName,
                          text = "${part.estimatedPrice} (${part.sourcePlatform})\nSpecs: ${part.exactDimensions}\nFits: ${part.compatibility}",
                          url = directBuyUrl,
                        )
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                  ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share", maxLines = 1, softWrap = false)
                  }
                }
              }
            }
          }
        }

        // PROMINENT LOAD MORE PARTS FOOTER (REPEATABLE PAGINATION)
        item(key = "parts_load_more_footer") {
          TacticalCard(highlight = true) {
            Text(
              "SHOWING ${filteredParts.size} OF ${rawParts.size} LOADED PARTS",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
            )
            if (partsLoadMoreStatus.isNotBlank()) {
              Text(partsLoadMoreStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
              Text(
                "Need more aftermarket upgrades, OEM spares, or alternate brands? Load another batch of live parts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Spacer(Modifier.height(8.dp))
            Button(
              onClick = {
                val effectiveQuery =
                  listOf(query, vehicleType.takeIf { it != "ALL" }).filterNotNull().filter { it.isNotBlank() }.joinToString(" ")
                mechanicVm.digDeeperParts(effectiveQuery.ifBlank { category }, make, model, tireSize, category, region, vehicleType, aiModelId)
              },
              enabled = !partsDigDeeperBusy,
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("parts_load_more_bottom_button"),
            ) {
              if (partsDigDeeperBusy) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Loading More Parts...")
              } else {
                Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Load More Results (+20 More Parts)")
              }
            }
          }
        }
      }
    }
  }

  expandedPart?.let { part ->
    val partCacheKey = "part_${part.partName}_${part.exactDimensions}".lowercase()
    val inCart = bomCart.any { it.partName == part.partName }
    val isSaved = part.partName in savedPartKeys
    val directBuyUrl = verifiedOffersCache[partCacheKey]?.firstOrNull()?.url ?: PevStoreDirectory.directLinkForPart(part)
    ExpandedPartDetailSheet(
      part = part,
      mechanicVm = mechanicVm,
      socialVm = socialVm,
      inCart = inCart,
      isSaved = isSaved,
      onToggleBom = { mechanicVm.toggleBomItem(part) },
      onToggleSave = {
        savedPartKeys = if (isSaved) savedPartKeys - part.partName else savedPartKeys + part.partName
      },
      onShare = {
        sharePayload =
          SocialSharePayload(
            title = part.partName,
            text = "${part.estimatedPrice} (${part.sourcePlatform})\nSpecs: ${part.exactDimensions}\nFits: ${part.compatibility}",
            url = directBuyUrl,
          )
      },
      onDismiss = { expandedPart = null },
    )
  }
  }

  sharePayload?.let { payload ->
    SocialShareDialog(payload = payload, onDismiss = { sharePayload = null })
  }
}
