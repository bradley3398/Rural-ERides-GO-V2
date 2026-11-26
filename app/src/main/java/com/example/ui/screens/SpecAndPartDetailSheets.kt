package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.core.LiveCloudServices
import com.example.core.PevBatteryPhysics
import com.example.data.PevCatalogData
import com.example.data.PevPartItem
import com.example.data.PevSpecRecord
import com.example.data.PevStoreDirectory
import com.example.data.YouTubeVideoItem
import com.example.ui.components.PevProductVisualBanner
import com.example.ui.components.StoreBrandLogo
import com.example.ui.components.TacticalCard
import com.example.viewmodel.CopilotSocialViewModel
import com.example.viewmodel.MechanicViewModel
import kotlin.math.roundToInt

/**
 * Full-screen interactive detail sheet when tapping any PEV card in Spec Engine:
 * - Multi-photo live product gallery (swipeable + thumbnail switcher)
 * - Live YouTube review / range / speed test videos launching directly in In-App Media Theater
 * - 24+ deep technical & derived engineering specs (series cells, full/cutoff V, W/lb, Wh/lb, $/Wh, $/mph, gas savings)
 * - Interactive Real-World Range Simulator (weight, cruise speed, temp, hill grade)
 * - Interactive Charger Speed & Electricity Cost Calculator
 */
@Composable
fun ExpandedPevSpecDetailSheet(
  spec: PevSpecRecord,
  mechanicVm: MechanicViewModel,
  socialVm: CopilotSocialViewModel?,
  isCompared: Boolean,
  isSaved: Boolean,
  onToggleCompare: () -> Unit,
  onToggleSave: () -> Unit,
  onAddToGarage: (() -> Unit)?,
  onShare: () -> Unit,
  initialFocusVideos: Boolean = false,
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)
  val ctx = LocalContext.current
  val listState = rememberLazyListState()
  val specKey = remember(spec) { "spec_${spec.brand}_${spec.name}".lowercase() }
  val fullTitle = remember(spec) { "${spec.brand} ${spec.name}".trim() }

  val imagesCache by mechanicVm.productImagesCache.collectAsStateWithLifecycle()
  val imagesBusy by mechanicVm.productImagesBusyKeys.collectAsStateWithLifecycle()
  val videosCache by mechanicVm.pevVideosCache.collectAsStateWithLifecycle()
  val videosBusy by mechanicVm.pevVideosBusyKeys.collectAsStateWithLifecycle()
  val verifiedOffersCache by mechanicVm.verifiedStoreOffersCache.collectAsStateWithLifecycle()
  val stockBusyKeys by mechanicVm.stockCheckBusyKeys.collectAsStateWithLifecycle()
  val stockCheckedKeys by mechanicVm.stockCheckedKeys.collectAsStateWithLifecycle()

  val galleryImages = remember(imagesCache, specKey, spec.imageUrls) {
    (imagesCache[specKey].orEmpty() + spec.imageUrls.filter { it.startsWith("http") }).distinct()
  }
  val videos = videosCache[specKey].orEmpty()
  val loadingPhotos = specKey in imagesBusy
  val loadingVideos = specKey in videosBusy
  val verifiedOffers = verifiedOffersCache[specKey].orEmpty()
  val checkingStock = specKey in stockBusyKeys
  val stockChecked = specKey in stockCheckedKeys
  val candidateStores = remember(spec) { PevStoreDirectory.candidateStoresForSpec(spec) }
  val cleanSpecQuery = remember(spec) { PevStoreDirectory.buildCleanSpecQuery(spec) }

  var selectedPhotoIdx by remember(specKey) { mutableIntStateOf(0) }
  var simWeightLbs by remember { mutableFloatStateOf(175f) }
  var simCruiseMph by remember { mutableFloatStateOf(25f) }
  var simTempF by remember { mutableFloatStateOf(70f) }
  var simGradePct by remember { mutableFloatStateOf(2f) }
  var simSurface by remember { mutableStateOf("Paved") }
  var chargerAmps by remember { mutableIntStateOf(5) }

  LaunchedEffect(specKey) {
    mechanicVm.loadProductImages(
      cacheKey = specKey,
      query = "$fullTitle ${spec.category.lowercase()} electric",
      seedUrls = spec.imageUrls,
    )
    mechanicVm.loadPevVideos(
      cacheKey = specKey,
      query = "$fullTitle review range test top speed",
    )
    mechanicVm.verifyInStockStores(
      cacheKey = specKey,
      productTitle = cleanSpecQuery,
      candidateStores = candidateStores,
    )
    if (initialFocusVideos) {
      runCatching { listState.animateScrollToItem(1) }
    }
  }

  // Deep derived engineering calculations using authoritative PevBatteryPhysics
  val whVal = PevCatalogData.extractWh(spec.battery).toDouble().coerceAtLeast(0.0)
  val voltVal = spec.nominalVoltage.toDouble().coerceAtLeast(24.0)
  val packChemistry =
    if (spec.battery.contains("lfp", true) || spec.battery.contains("lifepo4", true)) "LFP" else "NMC"
  val packProfile = remember(voltVal, packChemistry) { PevBatteryPhysics.packProfile(voltVal, packChemistry) }
  val ahVal = PevBatteryPhysics.computePackAh(whVal, packProfile.nominalV)
  val seriesCells = packProfile.seriesCount
  val fullChargeV = packProfile.fullChargeV
  val storageV = packProfile.storageV
  val cutoffV = packProfile.cutoffEmptyV
  val priceVal = PevCatalogData.extractPriceDouble(spec.price)
  val weightLbsVal = PevCatalogData.extractFirstInt(spec.weight).toDouble().coerceAtLeast(0.0)
  val topMphVal = PevCatalogData.extractFirstInt(spec.topSpeed).toDouble().coerceAtLeast(0.0)
  val factoryRangeMi = PevCatalogData.extractFirstInt(spec.range).toDouble().coerceAtLeast(0.0)
  val peakWatts =
    remember(spec.motorPower) {
      Regex("""(\d[\d,]*)\s*[wW]""")
        .findAll(spec.motorPower)
        .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
        .maxOrNull() ?: 0.0
    }

  val wattsPerLb = if (peakWatts > 0 && weightLbsVal > 0) peakWatts / weightLbsVal else 0.0
  val whPerLb = if (whVal > 0 && weightLbsVal > 0) whVal / weightLbsVal else 0.0
  val dollarsPerWh = if (priceVal > 0 && whVal > 0) priceVal / whVal else 0.0
  val dollarsPerMph = if (priceVal > 0 && topMphVal > 0) priceVal / topMphVal else 0.0
  val dollarsPerMi = if (priceVal > 0 && factoryRangeMi > 0) priceVal / factoryRangeMi else 0.0

  // Interactive real-world simulated rural + road range
  val baseRangeMi = PevCatalogData.estimateSimulatedRangeMiles(spec, simWeightLbs.toDouble(), simCruiseMph.toDouble())
  val tempFactor = PevBatteryPhysics.coldCapacityFactor((simTempF.toDouble() - 32.0) * 5.0 / 9.0, packChemistry)
  val surfaceFactor =
    when (simSurface) {
      "Hard Dirt" -> 0.88
      "Loose Gravel" -> 0.76
      "Mud / Steep" -> 0.64
      else -> 1.0
    }
  val gradeFactor = (1.0 / (1.0 + simGradePct * 0.075)).coerceIn(0.35, 1.0)
  val liveSimRangeMi = (baseRangeMi * tempFactor * surfaceFactor * gradeFactor).coerceAtLeast(3.0)
  val liveSimRangeKm = liveSimRangeMi * 1.60934

  // Exact CC+CV Charge time & electricity cost math via PevBatteryPhysics
  val chargerWatts = fullChargeV * chargerAmps
  val fullChargeHrs =
    if (whVal > 0) {
      PevBatteryPhysics.estimateChargeTimeHours(
        batteryWh = whVal,
        ratedVoltage = voltVal,
        chargerAmps = chargerAmps.toDouble(),
        startSocPct = 0.0,
        targetSocPct = 100.0,
        chemistry = packChemistry,
      )
    } else 0.0
  val rapid20to80Hrs =
    if (whVal > 0) {
      PevBatteryPhysics.estimateChargeTimeHours(
        batteryWh = whVal,
        ratedVoltage = voltVal,
        chargerAmps = chargerAmps.toDouble(),
        startSocPct = 20.0,
        targetSocPct = 80.0,
        chemistry = packChemistry,
      )
    } else 0.0
  val costPerFullChargeUsd = PevBatteryPhysics.estimateChargeCostUsd(whVal, 0.0, 100.0, 0.16)
  val costPer1000MiUsd =
    if (liveSimRangeMi > 0) (1000.0 / liveSimRangeMi) * costPerFullChargeUsd else 4.50
  val gasSavings1000MiUsd = ((1000.0 / 28.0) * 3.60 - costPer1000MiUsd).coerceAtLeast(0.0)

  val gearTier =
    when {
      topMphVal >= 45 -> "ECE 22.06 / DOT Full-Face Moto Helmet + CE-2 Armored Jacket & Knee Guards"
      topMphVal >= 28 -> "Full-Face DH/ECE Helmet + Wrist Sliders + Armored Gloves"
      else -> "MIPS Urban/Trail Helmet + Protective Gloves + High-Vis Lights"
    }
  val recommendedPsi =
    when (spec.category.uppercase()) {
      "SCOOTER" -> "42–50 PSI (cold)"
      "EUC" -> "28–36 PSI (cold)"
      "ONEWHEEL" -> "${(simWeightLbs / 10f).roundToInt().coerceIn(12, 24)} PSI (~10% rider weight)"
      "EBIKE" -> "22–35 PSI (fat/MTB) or 45–60 PSI (commuter)"
      "EMOTO", "SURRON" -> "18–26 PSI (trail) / 28–34 PSI (street)"
      else -> "32–42 PSI (cold)"
    }
  val directUrl = PevStoreDirectory.directLinkForSpec(spec)

  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = Modifier.fillMaxSize().statusBarsPadding(),
  ) {
    Column(Modifier.fillMaxSize()) {
      // TOP HEADER BAR
      Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        com.example.ui.components.PevBrandLogoBadge(
          brandOrModel = spec.brand.ifBlank { fullTitle },
          size = 34.dp,
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
          Text(
            "${spec.brand.uppercase()} • ${spec.category} • ${spec.nominalVoltage}V • ${spec.price}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            fullTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onToggleSave) {
            Icon(
              if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
              contentDescription = "Bookmark Spec",
              tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
          }
          IconButton(onClick = onShare) {
            Icon(Icons.Default.Share, contentDescription = "Share Spec", tint = MaterialTheme.colorScheme.onSurface)
          }
          IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_expanded_spec_sheet")) {
            Icon(Icons.Default.Close, contentDescription = "Close Spec Detail", tint = MaterialTheme.colorScheme.onSurface)
          }
        }
      }

      LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // 1. MULTI-PHOTO LIVE GALLERY
        item {
          TacticalCard(highlight = true) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text(
                "PRODUCT PHOTO GALLERY (${galleryImages.size} PHOTOS)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
              )
              TextButton(
                onClick = {
                  mechanicVm.loadProductImages(specKey, "$fullTitle electric ${spec.category.lowercase()}", spec.imageUrls, forceRefresh = true)
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              ) {
                if (loadingPhotos) {
                  CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                } else {
                  Icon(Icons.Default.Refresh, null, Modifier.size(15.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text("Refresh Photos", style = MaterialTheme.typography.labelSmall)
              }
            }
            Spacer(Modifier.height(6.dp))
            if (galleryImages.isNotEmpty()) {
              val safeIdx = selectedPhotoIdx.coerceIn(0, galleryImages.lastIndex)
              Box(Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(12.dp))) {
                AsyncImage(
                  model = galleryImages[safeIdx],
                  contentDescription = "$fullTitle Photo ${safeIdx + 1}",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
                Surface(
                  color = Color.Black.copy(alpha = 0.72f),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                ) {
                  Text(
                    "${safeIdx + 1} / ${galleryImages.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  )
                }
              }
              if (galleryImages.size > 1) {
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  itemsIndexed(galleryImages) { idx, imgUrl ->
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      border =
                        BorderStroke(
                          if (idx == safeIdx) 2.dp else 1.dp,
                          if (idx == safeIdx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        ),
                      modifier = Modifier.size(66.dp).clickable { selectedPhotoIdx = idx },
                    ) {
                      AsyncImage(
                        model = imgUrl,
                        contentDescription = "Thumbnail ${idx + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                      )
                    }
                  }
                }
              }
            } else {
              PevProductVisualBanner(
                title = fullTitle,
                subtitle = "${spec.motorPower} • ${spec.battery}",
                category = spec.category,
                badgeLeft = "${spec.category} • ${spec.nominalVoltage}V • ${spec.ipRating}",
                badgeRight = spec.price,
                imageUrl = "",
              )
              if (loadingPhotos) {
                Spacer(Modifier.height(6.dp))
                Text("Fetching multi-angle product photos...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
              }
            }
          }
        }

        // 2. WATCH LIVE PEV VIDEOS (IN-APP MEDIA THEATER)
        item {
          TacticalCard(highlight = true) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                  "WATCH $fullTitle VIDEOS (${videos.size})",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              if (loadingVideos) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
              } else {
                TextButton(
                  onClick = { mechanicVm.loadPevVideos(specKey, "$fullTitle review range test", forceRefresh = true) },
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                ) { Text("Reload", style = MaterialTheme.typography.labelSmall) }
              }
            }
            Spacer(Modifier.height(6.dp))
            if (videos.isEmpty() && !loadingVideos) {
              Text(
                "Tap Reload to fetch live YouTube reviews, range tests & teardowns for $fullTitle.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            } else if (videos.isNotEmpty()) {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(videos) { _, vid ->
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier =
                      Modifier
                        .width(210.dp)
                        .clickable {
                          if (socialVm != null) {
                            socialVm.playInMiniPlayer(vid, videos, expandTheater = true)
                            onDismiss()
                          } else {
                            runCatching {
                              ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${vid.videoId}")))
                            }
                          }
                        }
                        .testTag("spec_video_${vid.videoId}"),
                  ) {
                    Column {
                      Box(Modifier.fillMaxWidth().height(112.dp)) {
                        AsyncImage(
                          model = vid.thumbnailUrl,
                          contentDescription = vid.title,
                          contentScale = ContentScale.Crop,
                          modifier = Modifier.fillMaxSize(),
                        )
                        Surface(
                          color = Color.Black.copy(alpha = 0.72f),
                          shape = RoundedCornerShape(6.dp),
                          modifier = Modifier.align(Alignment.Center),
                        ) {
                          Icon(Icons.Default.PlayArrow, contentDescription = "Play Video", tint = Color.White, modifier = Modifier.padding(6.dp).size(26.dp))
                        }
                        if (vid.durationLabel.isNotBlank()) {
                          Surface(
                            color = Color.Black.copy(alpha = 0.78f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp),
                          ) {
                            Text(
                              vid.durationLabel,
                              style = MaterialTheme.typography.labelSmall,
                              color = Color.White,
                              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            )
                          }
                        }
                      }
                      Column(Modifier.padding(8.dp)) {
                        Text(vid.title, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        Text(vid.channelTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                      }
                    }
                  }
                }
              }
            }
          }
        }

        // 3. COMPLETE 24-POINT TECHNICAL & ELECTRICAL SPEC SHEET
        item {
          TacticalCard {
            Text("COMPLETE TECHNICAL SPECIFICATIONS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            val specRows =
              listOf(
                "Manufacturer / Brand" to spec.brand,
                "Model & Trim" to spec.name,
                "Vehicle Class" to spec.category,
                "Estimated Retail Price" to spec.price,
                "Top Speed" to spec.topSpeed,
                "Claimed Max Range" to spec.range,
                "Motor Power Output" to spec.motorPower,
                "Battery Pack" to spec.battery,
                "Nominal / Series Pack" to "${packProfile.nominalV.roundToInt()}V (${seriesCells}S ${packProfile.chemistry})",
                "Full / Storage / Cutoff V" to "${"%.1f".format(fullChargeV)}V Full  •  ${"%.1f".format(storageV)}V Storage  •  ${"%.1f".format(cutoffV)}V Cutoff",
                "Calculated Pack Capacity" to if (whVal > 0) "${whVal.roundToInt()} Wh  (~${"%.1f".format(ahVal)} Ah)" else "See battery spec",
                "Factory Charge Time" to spec.chargingTime.ifBlank { "5–8 hrs" },
                "Efficiency Rating" to spec.efficiency.ifBlank { "24–35 Wh/mi" },
                "Braking Hardware" to spec.brakingSystem,
                "Suspension Architecture" to spec.suspensionType,
                "Tire & Wheel Profile" to spec.tireProfile,
                "Recommended Tire PSI" to recommendedPsi,
                "Curb Weight" to spec.weight,
                "Max Rider Load" to spec.maxLoad,
                "Weather / IP Ingress Rating" to spec.ipRating,
                "Power-to-Weight Ratio" to if (wattsPerLb > 0) "${"%.1f".format(wattsPerLb)} W/lb (${"%.1f".format(wattsPerLb * 2.205)} W/kg)" else "N/A",
                "Battery Energy Density" to if (whPerLb > 0) "${"%.1f".format(whPerLb)} Wh/lb" else "N/A",
                "Value Analytics" to buildString {
                  val useMetric = !com.example.ui.theme.LocalTacticalSettings.current.distanceUnit.equals("mi", ignoreCase = true)
                  if (dollarsPerWh > 0) append("$${"%.2f".format(dollarsPerWh)}/Wh")
                  if (dollarsPerMph > 0) {
                    if (useMetric) append("  •  $${"%.0f".format(dollarsPerMph / 1.60934)}/km/h")
                    else append("  •  $${"%.0f".format(dollarsPerMph)}/mph")
                  }
                  if (dollarsPerMi > 0) {
                    if (useMetric) append("  •  $${"%.0f".format(dollarsPerMi / 1.60934)}/km")
                    else append("  •  $${"%.0f".format(dollarsPerMi)}/mi")
                  }
                }.ifBlank { "Standard" },
                "Recommended Safety Gear" to gearTier,
              )
            specRows.forEach { (label, value) ->
              if (value.isNotBlank()) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.Top,
                ) {
                  Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.44f))
                  Spacer(Modifier.width(8.dp))
                  Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.56f))
                }
              }
            }
          }
        }

        // 4. INTERACTIVE LIVE RANGE SIMULATOR
        item {
          val curSettings = com.example.ui.theme.LocalTacticalSettings.current
          val isMetricSim = !curSettings.distanceUnit.equals("mi", ignoreCase = true)
          val isCelsiusSim = curSettings.tempUnit.equals("c", ignoreCase = true)
          val displayCruise = if (isMetricSim) "${(simCruiseMph * 1.60934f).roundToInt()} KM/H" else "${simCruiseMph.roundToInt()} MPH"
          val displayTemp = if (isCelsiusSim) "${((simTempF - 32f) * 5f / 9f).roundToInt()}°C" else "${simTempF.roundToInt()}°F"
          val primaryRangeText =
            if (isMetricSim) "${"%.1f".format(liveSimRangeKm)} km (${"%.1f".format(liveSimRangeMi)} mi)"
            else "${"%.1f".format(liveSimRangeMi)} mi (${"%.1f".format(liveSimRangeKm)} km)"
          TacticalCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Speed, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("REAL-WORLD RANGE SIMULATOR", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                "Estimated Real Range: $primaryRangeText",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(10.dp),
              )
            }
            Spacer(Modifier.height(6.dp))
            Text("Rider + Gear Weight: ${simWeightLbs.roundToInt()} lbs", style = MaterialTheme.typography.labelSmall)
            com.example.ui.components.TacticalSlider(value = simWeightLbs, onValueChange = { simWeightLbs = it }, valueRange = 100f..320f)
            Text("Target Cruise Speed: $displayCruise", style = MaterialTheme.typography.labelSmall)
            com.example.ui.components.TacticalSlider(value = simCruiseMph, onValueChange = { simCruiseMph = it }, valueRange = 10f..55f)
            Text("Ambient Temp: $displayTemp  |  Avg Hill Grade: ${simGradePct.roundToInt()}%", style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              com.example.ui.components.TacticalSlider(value = simTempF, onValueChange = { simTempF = it }, valueRange = 15f..105f, modifier = Modifier.weight(1f))
              com.example.ui.components.TacticalSlider(value = simGradePct, onValueChange = { simGradePct = it }, valueRange = 0f..15f, modifier = Modifier.weight(1f))
            }
            Text("Backroad Surface: $simSurface", style = MaterialTheme.typography.labelSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              itemsIndexed(listOf("Paved", "Hard Dirt", "Loose Gravel", "Mud / Steep")) { _, surf ->
                FilterChip(
                  selected = simSurface == surf,
                  onClick = { simSurface = surf },
                  label = { Text(surf, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                )
              }
            }
          }
        }

        // 5. CHARGING SPEED & OPERATING COST CALCULATOR (EXACT CC+CV)
        item {
          TacticalCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.BatteryChargingFull, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("BARN CHARGING & 1,000-MILE COST", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              itemsIndexed(listOf(2, 5, 10, 15, 20)) { _, amps ->
                FilterChip(
                  selected = chargerAmps == amps,
                  onClick = { chargerAmps = amps },
                  label = { Text("${amps}A Charger", maxLines = 1, softWrap = false) },
                )
              }
            }
            Spacer(Modifier.height(6.dp))
            Text(
              "• Peak Charge Power: ${chargerWatts.roundToInt()}W (${chargerAmps}A @ ${"%.1f".format(fullChargeV)}V ${seriesCells}S)\n" +
                "• 0–100% CC+CV Full: ~${"%.1f".format(fullChargeHrs)} hrs  |  20–80% Fast Top-Up: ~${"%.1f".format(rapid20to80Hrs)} hrs\n" +
                "• Cost per Full Charge (@ $0.16/kWh): $${"%.2f".format(costPerFullChargeUsd)}\n" +
                "• 1,000-Mile Electricity Cost: ~$${"%.2f".format(costPer1000MiUsd)}  (Saves ~$${"%.0f".format(gasSavings1000MiUsd)} vs gas!)",
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }

        // 6. LIVE IN-STOCK VERIFIED STORES & GARAGE ACTIONS (NO UNVERIFIED STORE BUTTONS!)
        item {
          TacticalCard(highlight = true) {
            VerifiedInStockStoreSection(
              title = "LIVE IN-STOCK STORES ONLY",
              offers = verifiedOffers,
              checking = checkingStock,
              checked = stockChecked,
              onRecheck = {
                mechanicVm.verifyInStockStores(
                  cacheKey = specKey,
                  productTitle = cleanSpecQuery,
                  candidateStores = candidateStores,
                  forceRefresh = true,
                )
              },
              onOpenOffer = { offer ->
                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(offer.url))) }
              },
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = onToggleCompare,
                modifier = Modifier.weight(1f),
              ) {
                Icon(Icons.Default.CompareArrows, null, Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text(if (isCompared) "In Compare ✓" else "Compare Rig", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
              }
              if (onAddToGarage != null) {
                Button(onClick = onAddToGarage, modifier = Modifier.weight(1f)) {
                  Icon(com.example.ui.components.pevCategoryIcon(spec.category), null, Modifier.size(16.dp))
                  Spacer(Modifier.width(5.dp))
                  Text("+ Barn Garage", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Full-screen expandable detail sheet for any Part card in Parts Finder:
 * - Multi-photo live gallery
 * - Live YouTube install & review videos playing in In-App Media Theater
 * - Complete dimensions, fitment, weight, BOM toggle & verified store links
 */
@Composable
fun ExpandedPartDetailSheet(
  part: PevPartItem,
  mechanicVm: MechanicViewModel,
  socialVm: CopilotSocialViewModel?,
  inCart: Boolean,
  isSaved: Boolean,
  onToggleBom: () -> Unit,
  onToggleSave: () -> Unit,
  onShare: () -> Unit,
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)
  val ctx = LocalContext.current
  val partKey = remember(part) { "part_${part.partName}_${part.exactDimensions}".lowercase() }

  val imagesCache by mechanicVm.productImagesCache.collectAsStateWithLifecycle()
  val imagesBusy by mechanicVm.productImagesBusyKeys.collectAsStateWithLifecycle()
  val videosCache by mechanicVm.pevVideosCache.collectAsStateWithLifecycle()
  val videosBusy by mechanicVm.pevVideosBusyKeys.collectAsStateWithLifecycle()
  val verifiedOffersCache by mechanicVm.verifiedStoreOffersCache.collectAsStateWithLifecycle()
  val stockBusyKeys by mechanicVm.stockCheckBusyKeys.collectAsStateWithLifecycle()
  val stockCheckedKeys by mechanicVm.stockCheckedKeys.collectAsStateWithLifecycle()

  val seed = remember(part.imageUrl) { if (part.imageUrl.startsWith("http")) listOf(part.imageUrl) else emptyList() }
  val galleryImages = remember(imagesCache, partKey, seed) {
    (imagesCache[partKey].orEmpty() + seed).distinct()
  }
  val videos = videosCache[partKey].orEmpty()
  val loadingPhotos = partKey in imagesBusy
  val loadingVideos = partKey in videosBusy
  val verifiedOffers = verifiedOffersCache[partKey].orEmpty()
  val checkingStock = partKey in stockBusyKeys
  val stockChecked = partKey in stockCheckedKeys
  val candidateStores = remember(part) { PevStoreDirectory.candidateStoresForPart(part) }
  val cleanPartQuery = remember(part) { PevStoreDirectory.buildCleanPartQuery(part) }
  var selectedPhotoIdx by remember(partKey) { mutableIntStateOf(0) }

  LaunchedEffect(partKey) {
    mechanicVm.loadProductImages(
      cacheKey = partKey,
      query = "${part.partName} ${part.recommendedBrands}",
      seedUrls = seed,
    )
    mechanicVm.loadPevVideos(
      cacheKey = partKey,
      query = "${part.partName} install review electric scooter ebike",
    )
    mechanicVm.verifyInStockStores(
      cacheKey = partKey,
      productTitle = cleanPartQuery,
      candidateStores = candidateStores,
    )
  }

  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = Modifier.fillMaxSize().statusBarsPadding(),
  ) {
    Column(Modifier.fillMaxSize()) {
      Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(Modifier.weight(1f)) {
          Text("${part.category} • ${part.estimatedPrice} • ${part.sourcePlatform}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
          Text(part.partName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row {
          IconButton(onClick = onToggleSave) {
            Icon(
              if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
              contentDescription = "Save Part",
              tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
          }
          IconButton(onClick = onShare) {
            Icon(Icons.Default.Share, contentDescription = "Share Part", tint = MaterialTheme.colorScheme.onSurface)
          }
          IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_expanded_part_sheet")) {
            Icon(Icons.Default.Close, contentDescription = "Close Part Detail", tint = MaterialTheme.colorScheme.onSurface)
          }
        }
      }

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // 1. MULTI-PHOTO GALLERY
        item {
          TacticalCard(highlight = true) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text("PART PHOTOS (${galleryImages.size})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
              TextButton(
                onClick = { mechanicVm.loadProductImages(partKey, "${part.partName} ${part.recommendedBrands}", seed, forceRefresh = true) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              ) {
                if (loadingPhotos) CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Refresh, null, Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("Refresh", style = MaterialTheme.typography.labelSmall)
              }
            }
            Spacer(Modifier.height(6.dp))
            if (galleryImages.isNotEmpty()) {
              val safeIdx = selectedPhotoIdx.coerceIn(0, galleryImages.lastIndex)
              Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp))) {
                AsyncImage(
                  model = galleryImages[safeIdx],
                  contentDescription = "${part.partName} photo ${safeIdx + 1}",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
                Surface(
                  color = Color.Black.copy(alpha = 0.72f),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                ) {
                  Text("${safeIdx + 1} / ${galleryImages.size}", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
              }
              if (galleryImages.size > 1) {
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  itemsIndexed(galleryImages) { idx, imgUrl ->
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      border = BorderStroke(if (idx == safeIdx) 2.dp else 1.dp, if (idx == safeIdx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                      modifier = Modifier.size(64.dp).clickable { selectedPhotoIdx = idx },
                    ) {
                      AsyncImage(model = imgUrl, contentDescription = "Part thumb ${idx + 1}", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                  }
                }
              }
            } else {
              PevProductVisualBanner(
                title = part.partName,
                subtitle = "${part.exactDimensions} • ${part.recommendedBrands}",
                category = "SCOOTER",
                badgeLeft = "${part.category} • ${part.sourcePlatform}",
                badgeRight = part.estimatedPrice,
                imageUrl = "",
              )
            }
          }
        }

        // 2. PART INSTALL & REVIEW VIDEOS
        item {
          TacticalCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text("INSTALL & REVIEW VIDEOS (${videos.size})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
              if (loadingVideos) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            }
            Spacer(Modifier.height(6.dp))
            if (videos.isNotEmpty()) {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(videos) { _, vid ->
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier =
                      Modifier
                        .width(200.dp)
                        .clickable {
                          if (socialVm != null) {
                            socialVm.playInMiniPlayer(vid, videos, expandTheater = true)
                            onDismiss()
                          } else {
                            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${vid.videoId}"))) }
                          }
                        },
                  ) {
                    Column {
                      Box(Modifier.fillMaxWidth().height(104.dp)) {
                        AsyncImage(model = vid.thumbnailUrl, contentDescription = vid.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        Icon(Icons.Default.PlayCircle, contentDescription = "Play", tint = Color.White, modifier = Modifier.align(Alignment.Center).size(34.dp))
                      }
                      Column(Modifier.padding(8.dp)) {
                        Text(vid.title, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        Text(vid.channelTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                      }
                    }
                  }
                }
              }
            } else if (!loadingVideos) {
              Text("No install videos found yet for this exact part name.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }

        // 3. COMPLETE PART SPECS
        item {
          TacticalCard {
            Text("PART SPECIFICATIONS & FITMENT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            val rows =
              listOf(
                "Part Name" to part.partName,
                "Category" to part.category,
                "Exact Dimensions" to part.exactDimensions,
                "PEV Compatibility" to part.compatibility,
                "Recommended Brands" to part.recommendedBrands,
                "Primary Store" to part.sourcePlatform,
                "Estimated Price" to part.estimatedPrice,
                "Install Difficulty" to part.difficulty,
                "Part Classification" to part.partType,
                "Shipping Weight" to "${"%.2f".format(part.weightLbs)} lbs",
              )
            rows.forEach { (k, v) ->
              if (v.isNotBlank()) {
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                  Text(k, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.42f))
                  Text(v, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.58f))
                }
              }
            }
          }
        }

        // 4. LIVE IN-STOCK STORES ONLY & BOM CART
        item {
          TacticalCard(highlight = true) {
            VerifiedInStockStoreSection(
              title = "LIVE IN-STOCK STORES ONLY",
              offers = verifiedOffers,
              checking = checkingStock,
              checked = stockChecked,
              onRecheck = {
                mechanicVm.verifyInStockStores(
                  cacheKey = partKey,
                  productTitle = cleanPartQuery,
                  candidateStores = candidateStores,
                  forceRefresh = true,
                )
              },
              onOpenOffer = { offer ->
                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(offer.url))) }
              },
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onToggleBom, modifier = Modifier.fillMaxWidth()) {
              Icon(Icons.Default.AddShoppingCart, null, Modifier.size(16.dp))
              Spacer(Modifier.width(5.dp))
              Text(if (inCart) "In Barn BOM Cart ✓" else "+ Add to Barn BOM Cart", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }
          }
        }
      }
    }
  }
}

/**
 * Displays store purchase buttons ONLY for retailers verified live via HTTP to have the product in stock.
 * Never shows unverified or out-of-stock store buttons.
 */
@Composable
fun VerifiedInStockStoreSection(
  title: String,
  offers: List<LiveCloudServices.VerifiedStoreOffer>,
  checking: Boolean,
  checked: Boolean,
  onRecheck: () -> Unit,
  onOpenOffer: (LiveCloudServices.VerifiedStoreOffer) -> Unit,
) {
  Row(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      title,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.primary,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      softWrap = false,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )
    TextButton(
      onClick = onRecheck,
      enabled = !checking,
      modifier = Modifier.testTag("recheck_live_stock_btn"),
    ) {
      Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
      Spacer(Modifier.width(4.dp))
      Text(if (checking) "Checking..." else "Recheck Stock", maxLines = 1, softWrap = false)
    }
  }
  Spacer(Modifier.height(4.dp))
  when {
    checking -> {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(8.dp))
        Text(
          "Checking live store stock before showing buy buttons...",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    offers.isNotEmpty() -> {
      Text(
        "Confirmed in stock at ${offers.size} store${if (offers.size == 1) "" else "s"}:",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
      )
      Spacer(Modifier.height(6.dp))
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        offers.forEach { offer ->
          val destHost =
            remember(offer.url) {
              runCatching { java.net.URI(offer.url).host?.removePrefix("www.").orEmpty() }.getOrDefault("")
            }
          Button(
            onClick = { onOpenOffer(offer) },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("verified_instock_store_${offer.storeId}"),
          ) {
            StoreBrandLogo(storeId = offer.storeId, storeName = offer.storeName, size = 22.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
              Text(
                text =
                  buildString {
                    append("${offer.storeName} • In Stock")
                    if (offer.priceHint.isNotBlank()) append(" (${offer.priceHint})")
                  },
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = listOf(offer.stockBadge, destHost).filter { it.isNotBlank() }.joinToString(" • "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Default.OpenInBrowser, contentDescription = "Open product destination", modifier = Modifier.size(16.dp))
          }
        }
      }
    }
    checked -> {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          "No stores confirmed in stock right now — store buy buttons stay hidden until stock is verified.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(10.dp),
        )
      }
    }
    else -> {
      Text(
        "Tap Recheck Stock to verify live store inventory.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
