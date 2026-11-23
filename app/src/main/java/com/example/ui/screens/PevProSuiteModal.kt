package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.PevExpansionVaultData
import com.example.data.PevLiveExpansion200
import com.example.data.PevStoreDirectory
import com.example.ui.components.SocialShareDialog
import com.example.ui.components.SocialSharePayload
import com.example.ui.components.TacticalCard
import com.example.ui.components.TacticalTabStrip
import com.example.ui.components.TacticalTopBar
import com.example.ui.components.tacticalOutlinedTextFieldColors
import com.example.ui.theme.LocalTacticalChrome

/**
 * Full-screen modal housing 250+ PEV features across 6 instant offline + web hubs:
 * 1. Calculators (20)
 * 2. Error Codes (60)
 * 3. Field Repair & Mod Guides (42)
 * 4. Legal & Trail Rules (32) + Convoy Hand Signals (28)
 * 5. All Known PEV Web Stores (48)
 * 6. Technical PEV Glossary (40)
 */
@Composable
fun PevProSuiteModal(
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)
  val ctx = LocalContext.current
  var activeTab by remember { mutableIntStateOf(0) }
  var searchQuery by remember { mutableStateOf("") }
  var guideCategory by remember { mutableStateOf("ALL") }
  var storeCategory by remember { mutableStateOf("ALL") }
  var liveOpsCategory by remember { mutableStateOf("ALL") }
  var sharePayload by remember { mutableStateOf<SocialSharePayload?>(null) }

  val totalFeatures =
    20 +
      PevLiveExpansion200.MODULES.size +
      PevExpansionVaultData.MASTER_ERROR_CODES.size +
      PevExpansionVaultData.FIELD_REPAIR_GUIDES.size +
      PevExpansionVaultData.LEGAL_AND_TRAIL_RULES.size +
      PevExpansionVaultData.CONVOY_SIGNALS_AND_PROTOCOLS.size +
      PevExpansionVaultData.PEV_GLOSSARY.size +
      PevStoreDirectory.KNOWN_PEV_STORES.size

  val tabs =
    listOf(
      "Calculators (20)",
      "Live Ops (${PevLiveExpansion200.MODULES.size})",
      "E-Codes (${PevExpansionVaultData.MASTER_ERROR_CODES.size})",
      "Guides (${PevExpansionVaultData.FIELD_REPAIR_GUIDES.size})",
      "Laws & Convoy (${PevExpansionVaultData.LEGAL_AND_TRAIL_RULES.size + PevExpansionVaultData.CONVOY_SIGNALS_AND_PROTOCOLS.size})",
      "Web Stores (${PevStoreDirectory.KNOWN_PEV_STORES.size})",
      "Glossary (${PevExpansionVaultData.PEV_GLOSSARY.size})",
    )

  val chrome = LocalTacticalChrome.current
  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = Modifier.fillMaxSize(),
  ) {
    Column(Modifier.fillMaxSize()) {
      TacticalTopBar(
        title = "PEV Pro Suite ($totalFeatures+ Tools)",
        subtitle = "Calculators • Error Codes • Repair Guides • Laws • Stores • Glossary",
        icon = Icons.Default.ElectricBolt,
        badgeText = "PRO",
        actions = {
          IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_pev_pro_suite_button")) {
            Icon(Icons.Default.Close, contentDescription = "Close PEV Pro Suite")
          }
        },
      )

      TacticalTabStrip(
        tabs = tabs,
        selectedIndex = activeTab,
        onSelectTab = { activeTab = it },
        scrollable = true,
        testTagPrefix = "pro_suite_tab",
      )

      if (activeTab != 0) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
          placeholder = {
            Text(
              when (activeTab) {
                1 -> "Search 205 Live PEV Ops (VESC, GPS Nav, BMS, Suspension, Trail Safety, Mods)..."
                2 -> "Search error code or brand (E07, VESC, Bafang, Segway, Sur-Ron)..."
                3 -> "Search repair or mod guide (bleed, tubeless, wobble, waterproofing)..."
                4 -> "Search PEV law, TSA battery rule, or convoy hand signal..."
                5 -> "Search product across all 56+ known PEV web stores..."
                else -> "Search PEV technical term (PWM, FOC, Wh/mi, Voltage Sag)..."
              }
            )
          },
          singleLine = true,
          shape = chrome.smallShape,
          colors = tacticalOutlinedTextFieldColors(),
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp).testTag("pro_suite_search_input"),
        )
      }

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        when (activeTab) {
          0 -> {
            item {
              PevProCalculatorsHub(onShareResult = { sharePayload = it })
            }
          }
          1 -> {
            item {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(PevLiveExpansion200.CATEGORIES) { cat ->
                  FilterChip(
                    selected = liveOpsCategory == cat,
                    onClick = { liveOpsCategory = cat },
                    label = { Text(cat.replace("_", " "), style = MaterialTheme.typography.labelSmall) },
                  )
                }
              }
            }
            val filteredOps =
              PevLiveExpansion200.MODULES.filter { m ->
                (liveOpsCategory == "ALL" || m.category == liveOpsCategory) &&
                  (searchQuery.isBlank() ||
                    m.title.contains(searchQuery, true) ||
                    m.protocol.contains(searchQuery, true) ||
                    m.platform.contains(searchQuery, true) ||
                    m.category.contains(searchQuery, true))
              }
            items(filteredOps, key = { it.id }) { mod ->
              TacticalCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Column(Modifier.weight(1f)) {
                    Text(
                      "${mod.category.replace("_", " ")} • ${mod.platform} • ${mod.keyMetric}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(mod.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                  }
                  Row {
                    IconButton(
                      onClick = {
                        val enc = java.net.URLEncoder.encode(mod.storeSearchQuery, "UTF-8")
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?tbm=shop&q=$enc"))) }
                      },
                    ) { Icon(Icons.Default.OpenInBrowser, contentDescription = "Search Gear", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
                    IconButton(
                      onClick = {
                        sharePayload = SocialSharePayload(mod.title, "${mod.category} (${mod.platform}):\n${mod.protocol}")
                      },
                    ) { Icon(Icons.Default.Share, contentDescription = "Share Protocol", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface) }
                  }
                }
                Spacer(Modifier.height(4.dp))
                Text(mod.protocol, style = MaterialTheme.typography.bodySmall)
              }
            }
          }
          2 -> {
            val filtered =
              PevExpansionVaultData.MASTER_ERROR_CODES.filter { e ->
                searchQuery.isBlank() ||
                  e.brand.contains(searchQuery, true) ||
                  e.code.contains(searchQuery, true) ||
                  e.title.contains(searchQuery, true) ||
                  e.cause.contains(searchQuery, true) ||
                  e.fix.contains(searchQuery, true)
              }
            items(filtered) { e ->
              TacticalCard(highlight = e.severity == "CRITICAL") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Text("${e.brand} • ${e.code}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                  IconButton(
                    onClick = {
                      sharePayload =
                        SocialSharePayload(
                          title = "${e.brand} ${e.code}: ${e.title}",
                          text = "Cause: ${e.cause}\nFix: ${e.fix}",
                        )
                    },
                  ) { Icon(Icons.Default.Share, contentDescription = "Share error code", modifier = Modifier.size(18.dp)) }
                }
                Text(e.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text("Cause: ${e.cause}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("Fix: ${e.fix}", style = MaterialTheme.typography.bodyMedium)
              }
            }
          }
          3 -> {
            item {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf("ALL", "TIRES", "BRAKES", "BATTERY", "ELECTRICAL", "WATERPROOF", "SUSPENSION", "DRIVETRAIN")) { cat ->
                  FilterChip(
                    selected = guideCategory == cat,
                    onClick = { guideCategory = cat },
                    label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                  )
                }
              }
            }
            val filtered =
              PevExpansionVaultData.FIELD_REPAIR_GUIDES.filter { g ->
                (guideCategory == "ALL" || g.category == guideCategory) &&
                  (searchQuery.isBlank() || g.title.contains(searchQuery, true) || g.steps.contains(searchQuery, true))
              }
            items(filtered, key = { it.id }) { g ->
              TacticalCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Column(Modifier.weight(1f)) {
                    Text("${g.category} • ${g.difficulty} • ${g.timeEst}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text(g.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                  }
                  IconButton(
                    onClick = {
                      sharePayload = SocialSharePayload(g.title, "${g.category} Guide (${g.timeEst}):\n${g.steps}")
                    },
                  ) { Icon(Icons.Default.Share, contentDescription = "Share guide", modifier = Modifier.size(18.dp)) }
                }
                Spacer(Modifier.height(4.dp))
                Text(g.steps, style = MaterialTheme.typography.bodySmall)
              }
            }
          }
          4 -> {
            item {
              Text("PEV LEGAL & AIRLINE RULEBOOK (${PevExpansionVaultData.LEGAL_AND_TRAIL_RULES.size})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            val laws =
              PevExpansionVaultData.LEGAL_AND_TRAIL_RULES.filter { r ->
                searchQuery.isBlank() || r.region.contains(searchQuery, true) || r.topic.contains(searchQuery, true) || r.summary.contains(searchQuery, true)
              }
            items(laws) { r ->
              TacticalCard {
                Text("${r.region} • ${r.topic}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(r.summary, style = MaterialTheme.typography.bodySmall)
              }
            }
            item {
              Spacer(Modifier.height(6.dp))
              Text("CONVOY HAND SIGNALS & GROUP PROTOCOLS (${PevExpansionVaultData.CONVOY_SIGNALS_AND_PROTOCOLS.size})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            }
            val signals =
              PevExpansionVaultData.CONVOY_SIGNALS_AND_PROTOCOLS.filter { s ->
                searchQuery.isBlank() || s.signal.contains(searchQuery, true) || s.gesture.contains(searchQuery, true) || s.meaning.contains(searchQuery, true)
              }
            items(signals) { s ->
              TacticalCard {
                Text(s.signal, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                Text("Action: ${s.gesture}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(s.meaning, style = MaterialTheme.typography.bodySmall)
              }
            }
          }
          5 -> {
            item {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(PevStoreDirectory.STORE_CATEGORIES) { sc ->
                  FilterChip(
                    selected = storeCategory == sc,
                    onClick = { storeCategory = sc },
                    label = { Text(sc, style = MaterialTheme.typography.labelSmall) },
                  )
                }
              }
            }
            val stores =
              PevStoreDirectory.KNOWN_PEV_STORES.filter { st ->
                storeCategory == "ALL" || st.category == storeCategory
              }
            items(stores, key = { it.id }) { store ->
              TacticalCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Column(Modifier.weight(1f)) {
                    Text("${store.name} (${store.region})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text("${store.category} • ${store.specialty}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                  OutlinedButton(
                    onClick = {
                      val target = store.buildSearchUrl(searchQuery)
                      runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target))) }
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                  ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (searchQuery.isBlank()) "Visit" else "Search", maxLines = 1, softWrap = false)
                  }
                }
              }
            }
          }
          else -> {
            val glossary =
              PevExpansionVaultData.PEV_GLOSSARY.filter { g ->
                searchQuery.isBlank() || g.term.contains(searchQuery, true) || g.category.contains(searchQuery, true) || g.definition.contains(searchQuery, true)
              }
            items(glossary) { g ->
              TacticalCard {
                Text("${g.term} (${g.category})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(g.definition, style = MaterialTheme.typography.bodySmall)
              }
            }
          }
        }
      }
    }
  }

  sharePayload?.let { payload ->
    SocialShareDialog(payload = payload, onDismiss = { sharePayload = null })
  }
}
