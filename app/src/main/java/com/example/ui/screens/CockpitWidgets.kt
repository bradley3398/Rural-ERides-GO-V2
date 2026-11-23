package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MasterSettings
import com.example.data.NewsArticleItem
import com.example.data.PevVehicle
import com.example.data.RideSession
import com.example.data.UiState
import com.example.data.WeatherMatrixData
import com.example.ui.components.SocialShareDialog
import com.example.ui.components.SocialSharePayload
import com.example.ui.components.TacticalCard
import com.example.ui.components.TacticalIconButton
import com.example.ui.components.TacticalOutlinedButton
import com.example.ui.components.formatSpeedValue
import com.example.ui.components.formatTempLabel
import com.example.ui.components.speedUnitLabel
import com.example.viewmodel.CopilotSocialViewModel
import com.example.viewmodel.SessionViewModel

@Composable
fun WeatherMatrixCard(
  weatherState: UiState<WeatherMatrixData>,
  settings: MasterSettings,
  sessionVm: SessionViewModel,
  liveLat: Double,
  liveLng: Double,
  onRefresh: () -> Unit,
) {
  var subTab by remember { mutableIntStateOf(0) }
  var showLocationPicker by remember { mutableStateOf(false) }
  var cityQuery by remember { mutableStateOf(settings.customWeatherCity) }
  val locResults by sessionVm.weatherLocationResults.collectAsStateWithLifecycle()
  val locSearching by sessionVm.weatherLocSearching.collectAsStateWithLifecycle()
  val tabs = listOf("Overview", "12-Hr Hourly", "Aero Vectors", "Tire Grip", "Batt Derate", "Forecast")

  val currentLocLabel =
    when (val st = weatherState) {
      is UiState.Success -> st.data.locationLabel
      else -> if (settings.useCustomWeatherLoc && settings.customWeatherCity.isNotBlank()) settings.customWeatherCity else "Awaiting GPS Fix..."
    }
  val isGps =
    when (val st = weatherState) {
      is UiState.Success -> st.data.isGpsSource
      else -> !settings.useCustomWeatherLoc
    }

  TacticalCard(title = "WEATHER_MATRIX.EXE") {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Icon(Icons.Default.WbSunny, null, tint = MaterialTheme.colorScheme.tertiary)
        Spacer(Modifier.width(8.dp))
        Column {
          Text("OPEN-METEO WEATHER MATRIX", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              if (isGps) Icons.Default.GpsFixed else Icons.Default.LocationOn,
              contentDescription = "Weather Location Source",
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
              text = "${if (isGps) "GPS" else "CITY"}: $currentLocLabel",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.secondary,
              maxLines = 1,
            )
          }
        }
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        TacticalIconButton(
          onClick = { showLocationPicker = !showLocationPicker },
          modifier = Modifier.testTag("weather_location_toggle_button"),
        ) {
          Icon(Icons.Default.LocationOn, contentDescription = "Change Weather Location")
        }
        TacticalIconButton(onClick = onRefresh, modifier = Modifier.testTag("refresh_weather_button")) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh Weather")
        }
      }
    }

    if (showLocationPicker) {
      Spacer(Modifier.height(6.dp))
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
          value = cityQuery,
          onValueChange = { cityQuery = it },
          label = { Text("City or ZIP (e.g. Austin, Denver)") },
          singleLine = true,
          modifier = Modifier.weight(1f).testTag("weather_city_input"),
        )
        TacticalOutlinedButton(
          onClick = { sessionVm.searchWeatherLocations(cityQuery) },
          modifier = Modifier.testTag("weather_city_search_button"),
        ) {
          Icon(Icons.Default.Search, contentDescription = "Search City", modifier = Modifier.size(16.dp))
        }
      }
      Spacer(Modifier.height(4.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
          selected = !settings.useCustomWeatherLoc,
          onClick = {
            sessionVm.useGpsWeatherLocation(liveLat, liveLng)
            showLocationPicker = false
          },
          leadingIcon = { Icon(Icons.Default.GpsFixed, null, Modifier.size(15.dp)) },
          label = { Text("Use Live GPS", maxLines = 1, softWrap = false) },
          modifier = Modifier.testTag("weather_use_gps_chip"),
        )
        if (locSearching) {
          CircularProgressIndicator(Modifier.size(18.dp).align(Alignment.CenterVertically), strokeWidth = 2.dp)
        }
      }
      if (locResults.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          locResults.take(5).forEach { loc ->
            Surface(
              onClick = {
                sessionVm.selectCustomWeatherLocation(loc)
                cityQuery = loc.displayLabel()
                showLocationPicker = false
              },
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.fillMaxWidth().testTag("weather_loc_option_${loc.name}"),
            ) {
              Text(
                text = "${loc.displayLabel()} (${"%.2f".format(loc.lat)}, ${"%.2f".format(loc.lng)})",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              )
            }
          }
        }
      }
      Spacer(Modifier.height(6.dp))
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(tabs.size) { idx ->
        FilterChip(selected = subTab == idx, onClick = { subTab = idx }, label = { Text(tabs[idx], style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) })
      }
    }
    Spacer(Modifier.height(6.dp))

    when (weatherState) {
      is UiState.Loading -> Text("Syncing hyperlocal meteorology from Open-Meteo...", style = MaterialTheme.typography.bodyMedium)
      is UiState.Error -> {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(weatherState.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
          TacticalOutlinedButton(onClick = { showLocationPicker = true }) {
            Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Pick City / Location")
          }
        }
      }
      is UiState.Success -> {
        val w = weatherState.data
        // PEV Ride Window Score (0-100) from wind, precip, and ambient temperature
        val rideWindowScore =
          (100 -
            (w.precipProbPct * 0.6).toInt() -
            ((w.windSpeedKmh - 12.0).coerceAtLeast(0.0) * 1.4).toInt() -
            (if (w.tempC < 5.0 || w.tempC > 35.0) 20 else 0)).coerceIn(10, 100)
        when (subTab) {
          0 -> {
            val wetWarning = w.precipProbPct >= 40 || w.weatherCode >= 51
            Text(
              "${formatTempLabel(w.tempC, settings.tempUnit)} (Feels ${formatTempLabel(w.feelsLikeC, settings.tempUnit)}) • ${w.conditionLabel}",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              "PEV Ride Score: $rideWindowScore/100 • Wind ${"%.1f".format(formatSpeedValue(w.windSpeedKmh, settings.distanceUnit))} ${speedUnitLabel(settings.distanceUnit)} • Precip ${w.precipProbPct}% • UV ${w.uvIndex}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              "Humidity ${w.humidityPct}% • Barometer ${w.pressureHpa.toInt()} hPa • Sunrise ${w.sunrise} / Sunset ${w.sunset}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (wetWarning) {
              Spacer(Modifier.height(4.dp))
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WaterDrop, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("Wet Pavement Caution: Reduce lean angle & braking force.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
              }
            }
          }
          1 -> {
            if (w.hourly.isEmpty()) {
              Text("No hourly telemetry returned yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(w.hourly) { h ->
                  Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                    Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                      Text(h.timeLabel, style = MaterialTheme.typography.labelSmall)
                      Text(formatTempLabel(h.tempC, settings.tempUnit), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                      Text("${h.precipPct}% rain", style = MaterialTheme.typography.labelSmall)
                      Text("${"%.0f".format(formatSpeedValue(h.windKmh, settings.distanceUnit))} ${speedUnitLabel(settings.distanceUnit)}", style = MaterialTheme.typography.labelSmall)
                    }
                  }
                }
              }
            }
          }
          2 -> {
            val compass = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
            val dirStr = compass[((w.windDirDeg + 22) / 45) % 8]
            val gustRatio = if (w.windSpeedKmh > 1.0) w.windGustKmh / w.windSpeedKmh else 1.0
            val headwindPenaltyPct = (w.windSpeedKmh * 0.75).toInt().coerceIn(0, 38)
            val shearNote = if (w.windGustKmh >= 35.0) "HIGH CROSSWIND SHEAR — widen stance & tuck torso" else "Low aerodynamic drag penalty"
            Text("Sustained: ${"%.1f".format(formatSpeedValue(w.windSpeedKmh, settings.distanceUnit))} ${speedUnitLabel(settings.distanceUnit)} from $dirStr (${w.windDirDeg}°)", style = MaterialTheme.typography.bodyMedium)
            Text("Peak Gusts: ${"%.1f".format(formatSpeedValue(w.windGustKmh, settings.distanceUnit))} ${speedUnitLabel(settings.distanceUnit)} (${"%.2f".format(gustRatio)}x gust) • Direct Headwind Range Hit: -$headwindPenaltyPct%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text("Aero Advisory: $shearNote", style = MaterialTheme.typography.labelSmall)
          }
          3 -> {
            val coldDropPsi = ((20.0 - w.tempC) * 0.12).coerceIn(-3.0, 5.0)
            val wetDrop = if (w.precipProbPct >= 45 || w.weatherCode >= 51) -3 else 0
            Text("Thermal Pressure Shift: ${if (coldDropPsi >= 0) "+" else ""}${"%.1f".format(coldDropPsi)} PSI compensation vs 20°C baseline", style = MaterialTheme.typography.bodyMedium)
            Text("Surface Traction Mode: ${if (wetDrop < 0) "WET / SLIPPERY (-3 PSI wider contact patch)" else "DRY / NOMINAL"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
          }
          4 -> {
            val tempDeratePct =
              when {
                w.tempC < 0.0 -> 26
                w.tempC < 10.0 -> 14
                w.tempC > 38.0 -> 8
                else -> 0
              }
            Text("Lithium Cell Chemistry: ${settings.batteryChemistry} • Ambient ${formatTempLabel(w.tempC, settings.tempUnit)}", style = MaterialTheme.typography.bodyMedium)
            Text("Estimated Range Derate: -$tempDeratePct% (${if (w.tempC < 5.0) "Warm cells indoors before fast charging!" else "Optimal thermal window"})", style = MaterialTheme.typography.labelSmall, color = if (tempDeratePct > 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
          }
          else -> {
            if (w.daily.isEmpty()) {
              Text("Multi-day forecast awaiting sync.", style = MaterialTheme.typography.bodyMedium)
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                w.daily.take(7).forEach { d ->
                  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(d.dateLabel, style = MaterialTheme.typography.labelMedium)
                    Text("${formatTempLabel(d.minTempC, settings.tempUnit)} / ${formatTempLabel(d.maxTempC, settings.tempUnit)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text("${d.precipPct}% rain", style = MaterialTheme.typography.labelSmall)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun FinancialRoiCard(
  rides: List<RideSession>,
  settings: MasterSettings,
  onUpdateSettings: ((MasterSettings) -> MasterSettings) -> Unit,
) {
  var subTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("Summary", "ROI & Payback", "Projections", "Car Maint", "Eco Impact")

  val totalKm = rides.sumOf { it.distanceKm }
  val totalMiles = totalKm * 0.621371
  val totalWh = rides.sumOf { it.whUsed }
  val evCost = (totalWh / 1000.0) * settings.kwhRateUsd
  val gasCost = (totalMiles / settings.carMpg.coerceAtLeast(1.0)) * settings.gasPriceUsd
  val carMaintSaved = totalMiles * 0.11
  val netSaved = (gasCost + carMaintSaved - evCost).coerceAtLeast(0.0)
  val irsDeduction = totalMiles * 0.70
  val co2KgAvoided = totalMiles * 0.404
  val treesEq = co2KgAvoided / 21.77

  TacticalCard {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Default.Savings, null, tint = MaterialTheme.colorScheme.primary)
      Spacer(Modifier.width(8.dp))
      Text("EV FINANCIAL & CARBON ROI", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
    Spacer(Modifier.height(6.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(tabs.size) { idx ->
        FilterChip(selected = subTab == idx, onClick = { subTab = idx }, label = { Text(tabs[idx], style = MaterialTheme.typography.labelSmall) })
      }
    }
    Spacer(Modifier.height(8.dp))

    when (subTab) {
      0 -> {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Column {
            Text("Total Saved", style = MaterialTheme.typography.labelSmall)
            Text("$${"%.2f".format(netSaved)}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
          }
          Column {
            Text("EV Electricity", style = MaterialTheme.typography.labelSmall)
            Text("$${"%.2f".format(evCost)}", style = MaterialTheme.typography.titleLarge)
          }
          Column {
            Text("IRS Mileage Eq", style = MaterialTheme.typography.labelSmall)
            Text("$${"%.2f".format(irsDeduction)}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
          }
        }
      }
      1 -> {
        val costPerMiEv = if (totalMiles > 0.1) evCost / totalMiles else 0.005
        val costPerMiCar = (settings.gasPriceUsd / settings.carMpg.coerceAtLeast(1.0)) + 0.11
        val netGainPerMi = (costPerMiCar - costPerMiEv).coerceAtLeast(0.01)
        val milesToPay2k = 2000.0 / netGainPerMi
        Text("EV Cost/mi: $${"%.3f".format(costPerMiEv)} vs Car Cost/mi: $${"%.3f".format(costPerMiCar)}", style = MaterialTheme.typography.bodyMedium)
        Text("Breakeven on a $2,000 PEV build: ${"%.0f".format(milesToPay2k)} miles ridden", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
      }
      2 -> {
        val annualMiles = (totalMiles * 12.0).coerceAtLeast(1200.0)
        val projGas = (annualMiles / settings.carMpg.coerceAtLeast(1.0)) * settings.gasPriceUsd
        val projEv = (annualMiles * 0.035) * settings.kwhRateUsd
        Text("Projected 12-Month Savings (est ${annualMiles.toInt()} mi/yr): $${"%.2f".format(projGas + annualMiles * 0.11 - projEv)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Text("5-Year Cumulative Advantage: $${"%.0f".format((projGas + annualMiles * 0.11 - projEv) * 5.0)}", style = MaterialTheme.typography.labelSmall)
      }
      3 -> {
        Text("Avoided Car Wear ($0.11/mi oil, tires, brakes): $${"%.2f".format(carMaintSaved)}", style = MaterialTheme.typography.bodyMedium)
        Text("Adjust Live Cost Assumptions:", style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          TacticalOutlinedButton(onClick = { onUpdateSettings { it.copy(gasPriceUsd = (it.gasPriceUsd + 0.25).coerceAtMost(8.0)) } }, modifier = Modifier.weight(1f)) {
            Text("Gas + ($${"%.2f".format(settings.gasPriceUsd)})", maxLines = 1, softWrap = false)
          }
          TacticalOutlinedButton(onClick = { onUpdateSettings { it.copy(gasPriceUsd = (it.gasPriceUsd - 0.25).coerceAtLeast(1.5)) } }, modifier = Modifier.weight(1f)) {
            Text("Gas -", maxLines = 1, softWrap = false)
          }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          TacticalOutlinedButton(onClick = { onUpdateSettings { it.copy(carMpg = (it.carMpg + 2.0).coerceAtMost(60.0)) } }, modifier = Modifier.weight(1f)) {
            Text("MPG + (${settings.carMpg.toInt()})", maxLines = 1, softWrap = false)
          }
          TacticalOutlinedButton(onClick = { onUpdateSettings { it.copy(kwhRateUsd = (it.kwhRateUsd + 0.02).coerceAtMost(0.60)) } }, modifier = Modifier.weight(1f)) {
            Text("kWh + ($${"%.2f".format(settings.kwhRateUsd)})", maxLines = 1, softWrap = false)
          }
        }
      }
      else -> {
        Text("Tailpipe CO₂ Offset: ${"%.1f".format(co2KgAvoided)} kg avoided", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Text("Equivalent to ${"%.2f".format(treesEq)} mature trees absorbing carbon for a full year.", style = MaterialTheme.typography.labelSmall)
      }
    }
  }
}

@Composable
fun YoutubeMusicAndNewsDeck(
  sessionVm: SessionViewModel,
  socialVm: CopilotSocialViewModel,
  newsState: UiState<List<NewsArticleItem>>,
  onRefreshNews: () -> Unit,
) {
  val ctx = LocalContext.current
  val settings by sessionVm.settings.collectAsStateWithLifecycle()
  val newsCategory by sessionVm.newsCategory.collectAsStateWithLifecycle()
  val resolvedGpsCity by sessionVm.resolvedGpsCity.collectAsStateWithLifecycle()
  val mixesState by socialVm.musicMixesState.collectAsStateWithLifecycle()
  val activeGenre by socialVm.activeMusicCategory.collectAsStateWithLifecycle()
  val activeVideo by socialVm.activeVideo.collectAsStateWithLifecycle()
  val recentMusicQueries by socialVm.recentMusicQueries.collectAsStateWithLifecycle()
  var dashboardMusicQuery by remember { mutableStateOf("") }
  val genres =
    listOf(
      "Synthwave",
      "Lo-Fi",
      "Phonk",
      "Drum & Bass",
      "Cyberpunk",
      "Rock",
      "Hip-Hop",
      "Deep House",
      "Ambient",
      "Dubstep",
      "Retrowave 80s",
      "Acoustic",
    )
  val pevNewsCategories = listOf("ALL PEV", "E-Bikes", "E-Scooters", "EUCs & Onewheel", "E-Motos", "Laws & Trails")
  var customNewsCityInput by remember(settings.customNewsCity) { mutableStateOf(settings.customNewsCity) }
  var showNewsLocInput by remember { mutableStateOf(false) }
  var newsSharePayload by remember { mutableStateOf<SocialSharePayload?>(null) }

  TacticalCard(highlight = activeVideo != null, title = "DASHBOARD_MINI_PLAYER.EXE") {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Column {
          Text("YOUTUBE TRAIL AUDIO & SEARCH DECK", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
          Text("Search Any Song, Artist, Podcast or Mix • Zero App Switch", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TacticalIconButton(
          onClick = { socialVm.launchAutoDjPevMix() },
          modifier = Modifier.testTag("dashboard_autodj_button"),
        ) {
          Icon(Icons.Default.AutoAwesome, contentDescription = "Auto-DJ PEV Mix", tint = MaterialTheme.colorScheme.secondary)
        }
        val tracksNow = (mixesState as? UiState.Success)?.data?.first.orEmpty()
        if (tracksNow.isNotEmpty()) {
          TacticalOutlinedButton(
            onClick = { socialVm.playInMiniPlayer(tracksNow.first(), tracksNow, expandTheater = false) },
            modifier = Modifier.testTag("play_all_trail_mix_button"),
          ) {
            Text("Play All", maxLines = 1, softWrap = false)
          }
        }
      }
    }
    Spacer(Modifier.height(8.dp))
    // CUSTOM USER SEARCH BAR DIRECTLY ON THE DASHBOARD MINI-PLAYER
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      OutlinedTextField(
        value = dashboardMusicQuery,
        onValueChange = { dashboardMusicQuery = it },
        placeholder = { Text("Search your music, artist, song, or video...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingIcon = {
          if (dashboardMusicQuery.isNotBlank()) {
            IconButton(onClick = { dashboardMusicQuery = "" }) {
              Icon(Icons.Default.Close, contentDescription = "Clear search")
            }
          }
        },
        singleLine = true,
        modifier = Modifier.weight(1f).testTag("dashboard_music_search_input"),
      )
      Button(
        onClick = {
          val q = dashboardMusicQuery.trim()
          if (q.isNotBlank()) {
            socialVm.loadMusicMixes("Custom: $q", customQuery = q)
          }
        },
        enabled = dashboardMusicQuery.isNotBlank(),
        modifier = Modifier.height(52.dp).testTag("dashboard_music_search_button"),
      ) {
        Icon(Icons.Default.Search, contentDescription = "Search Music")
      }
    }
    if (recentMusicQueries.isNotEmpty()) {
      Spacer(Modifier.height(6.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(recentMusicQueries) { past ->
          FilterChip(
            selected = activeGenre.equals("Custom: $past", ignoreCase = true),
            onClick = {
              dashboardMusicQuery = past
              socialVm.loadMusicMixes("Custom: $past", customQuery = past)
            },
            label = { Text(past, maxLines = 1, softWrap = false) },
          )
        }
      }
    }
    Spacer(Modifier.height(6.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(genres) { genre ->
        FilterChip(
          selected = activeGenre.equals(genre, ignoreCase = true),
          onClick = { socialVm.loadMusicMixes(genre) },
          label = { Text(genre, maxLines = 1, softWrap = false) },
        )
      }
    }
    Spacer(Modifier.height(8.dp))
    when (val st = mixesState) {
      is UiState.Loading -> {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Center) {
          CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        }
      }
      is UiState.Error -> {
        Text(st.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
      }
      is UiState.Success -> {
        val tracks = st.data.first
        if (tracks.isEmpty()) {
          Text("No tracks matched your search yet. Try another artist, song title, or genre.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tracks.take(15), key = { it.videoId }) { track ->
              val playingThis = activeVideo?.videoId == track.videoId
              Surface(
                onClick = { socialVm.playInMiniPlayer(track, tracks, expandTheater = false) },
                color = if (playingThis) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, if (playingThis) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.width(210.dp).testTag("cockpit_music_track_${track.videoId}"),
              ) {
                Column(Modifier.padding(10.dp)) {
                  Text(
                    track.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    color = if (playingThis) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                  )
                  Spacer(Modifier.height(4.dp))
                  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      "${track.channelTitle} • ${track.durationLabel}",
                      style = MaterialTheme.typography.labelSmall,
                      maxLines = 1,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.weight(1f),
                    )
                    IconButton(
                      onClick = { socialVm.enqueuePlayNext(track) },
                      modifier = Modifier.size(28.dp),
                    ) {
                      Icon(
                        Icons.Default.QueueMusic,
                        contentDescription = "Queue Play Next",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp),
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
  }

  TacticalCard(title = "PEV_NEWS_WIRE.EXE") {
    val activeLocSummary =
      when (settings.newsLocationScope) {
        "GPS" -> if (resolvedGpsCity.isNotBlank()) "Local: $resolvedGpsCity" else "Local GPS (Auto)"
        "CUSTOM" -> "City: ${settings.customNewsCity.ifBlank { "Set City" }}"
        else -> "Global PEV Wire"
      }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Icon(Icons.Default.Newspaper, null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(8.dp))
        Column {
          Text("STRICTLY PEV-ONLY NEWS WIRE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
          Text("Zero Car/Auto Noise • $activeLocSummary", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
      }
      Row {
        TacticalIconButton(
          onClick = { showNewsLocInput = !showNewsLocInput },
          modifier = Modifier.testTag("news_location_toggle_button"),
        ) {
          Icon(Icons.Default.LocationOn, contentDescription = "Set News Location")
        }
        TacticalIconButton(onClick = onRefreshNews, modifier = Modifier.testTag("refresh_news_button")) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh PEV News")
        }
      }
    }
    Spacer(Modifier.height(6.dp))
    // Location scope chips (Global, GPS Local, Custom City)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      item {
        FilterChip(
          selected = settings.newsLocationScope == "GPS",
          onClick = { sessionVm.setNewsLocationScope("GPS") },
          leadingIcon = { Icon(Icons.Default.GpsFixed, null, Modifier.size(14.dp)) },
          label = { Text(if (resolvedGpsCity.isNotBlank()) resolvedGpsCity.substringBefore(",") else "GPS Local", maxLines = 1, softWrap = false) },
          modifier = Modifier.testTag("news_scope_gps"),
        )
      }
      item {
        FilterChip(
          selected = settings.newsLocationScope == "GLOBAL",
          onClick = { sessionVm.setNewsLocationScope("GLOBAL") },
          label = { Text("Global PEV", maxLines = 1, softWrap = false) },
          modifier = Modifier.testTag("news_scope_global"),
        )
      }
      item {
        FilterChip(
          selected = settings.newsLocationScope == "CUSTOM",
          onClick = { showNewsLocInput = true },
          leadingIcon = { Icon(Icons.Default.LocationOn, null, Modifier.size(14.dp)) },
          label = { Text(settings.customNewsCity.ifBlank { "Custom City" }, maxLines = 1, softWrap = false) },
          modifier = Modifier.testTag("news_scope_custom"),
        )
      }
    }
    if (showNewsLocInput) {
      Spacer(Modifier.height(6.dp))
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
          value = customNewsCityInput,
          onValueChange = { customNewsCityInput = it },
          label = { Text("City or State for Local PEV News") },
          singleLine = true,
          modifier = Modifier.weight(1f).testTag("news_custom_city_input"),
        )
        TacticalOutlinedButton(
          onClick = {
            sessionVm.setNewsLocationScope("CUSTOM", customNewsCityInput.trim())
            showNewsLocInput = false
          },
          modifier = Modifier.testTag("news_apply_city_button"),
        ) {
          Text("Apply", maxLines = 1, softWrap = false)
        }
      }
    }
    Spacer(Modifier.height(6.dp))
    // Strictly PEV category selector chips
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(pevNewsCategories) { cat ->
        FilterChip(
          selected = newsCategory.equals(cat, ignoreCase = true),
          onClick = { sessionVm.setNewsCategory(cat) },
          label = { Text(cat, maxLines = 1, softWrap = false) },
          modifier = Modifier.testTag("news_cat_${cat.lowercase().replace(" ", "_")}"),
        )
      }
    }
    Spacer(Modifier.height(6.dp))
    when (newsState) {
      is UiState.Loading -> Text("Fetching strictly PEV-only news wire...", style = MaterialTheme.typography.bodyMedium)
      is UiState.Error -> Text(newsState.message, color = MaterialTheme.colorScheme.error)
      is UiState.Success -> {
        if (newsState.data.isEmpty()) {
          Text("No PEV articles matched that filter. Tap Global PEV or Refresh.", style = MaterialTheme.typography.bodyMedium)
        } else {
          newsState.data.take(8).forEach { art ->
            Surface(
              onClick = { if (art.link.isNotBlank()) runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(art.link))) } },
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            ) {
              Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                  Text("${art.source} • ${art.publishedAt}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                  Text(art.title, style = MaterialTheme.typography.titleMedium)
                }
                IconButton(onClick = {
                  newsSharePayload =
                    SocialSharePayload(
                      title = art.title,
                      text = "${art.source} • ${art.publishedAt}\n${art.title}",
                      url = art.link,
                    )
                }) {
                  Icon(Icons.Default.Share, contentDescription = "Share Article", modifier = Modifier.size(18.dp))
                }
              }
            }
          }
        }
      }
    }
  }

  newsSharePayload?.let { payload ->
    SocialShareDialog(payload = payload, onDismiss = { newsSharePayload = null })
  }
}

@Composable
fun VehicleSetupDialog(
  initial: PevVehicle?,
  onDismiss: () -> Unit,
  onSave: (PevVehicle) -> Unit,
) {
  var name by remember { mutableStateOf(initial?.name ?: "") }
  var category by remember { mutableStateOf(initial?.category ?: "SCOOTER") }
  var whStr by remember { mutableStateOf((initial?.batteryWh ?: 1500L).toString()) }
  var voltStr by remember { mutableStateOf((initial?.voltage ?: 60.0).toInt().toString()) }
  var psiStr by remember { mutableStateOf((initial?.targetPsi ?: 36.0).toInt().toString()) }
  var tireSize by remember { mutableStateOf(initial?.tireSize ?: "11x3.5") }
  var intervalStr by remember { mutableStateOf((initial?.serviceIntervalKm ?: 500.0).toInt().toString()) }
  var photoUri by remember { mutableStateOf(initial?.photoUri ?: "") }

  val picker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) photoUri = uri.toString()
    }

  com.example.ui.components.TacticalDialog(
    onDismissRequest = onDismiss,
    title = if (initial == null) "Add PEV to Garage" else "Edit ${initial.name}",
    subtitle = "Vehicle Telemetry Profile",
    badgeText = category,
    confirmButton = {
      com.example.ui.components.TacticalButton(onClick = {
        onSave(
          (initial ?: PevVehicle()).copy(
            name = name.ifBlank { "Custom PEV" },
            category = category,
            batteryWh = whStr.toLongOrNull() ?: 1500L,
            voltage = voltStr.toDoubleOrNull() ?: 60.0,
            targetPsi = psiStr.toDoubleOrNull() ?: 36.0,
            serviceIntervalKm = intervalStr.toDoubleOrNull() ?: 500.0,
            tireSize = tireSize,
            photoUri = photoUri,
          )
        )
      }) { Text("Save PEV") }
    },
    dismissButton = {
      TacticalOutlinedButton(onClick = onDismiss) { Text("Cancel") }
    },
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Model Name (e.g. NAMI Burn-E 2)") },
        colors = com.example.ui.components.tacticalOutlinedTextFieldColors(),
        modifier = Modifier.fillMaxWidth(),
      )
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(listOf("SCOOTER", "EBIKE", "EUC", "ONEWHEEL", "EMOTO", "ESKATE", "OTHER")) { cat ->
          com.example.ui.components.TacticalFilterChip(
            selected = category == cat,
            onClick = { category = cat },
            label = cat,
          )
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = whStr,
          onValueChange = { whStr = it },
          label = { Text("Pack Wh") },
          colors = com.example.ui.components.tacticalOutlinedTextFieldColors(),
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = voltStr,
          onValueChange = { voltStr = it },
          label = { Text("Voltage") },
          colors = com.example.ui.components.tacticalOutlinedTextFieldColors(),
          modifier = Modifier.weight(1f),
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = psiStr,
          onValueChange = { psiStr = it },
          label = { Text("Target PSI") },
          colors = com.example.ui.components.tacticalOutlinedTextFieldColors(),
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = intervalStr,
          onValueChange = { intervalStr = it },
          label = { Text("Service km") },
          colors = com.example.ui.components.tacticalOutlinedTextFieldColors(),
          modifier = Modifier.weight(1f),
        )
      }
      OutlinedTextField(
        value = tireSize,
        onValueChange = { tireSize = it },
        label = { Text("Tire Size") },
        colors = com.example.ui.components.tacticalOutlinedTextFieldColors(),
        modifier = Modifier.fillMaxWidth(),
      )
      TacticalOutlinedButton(
        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(if (photoUri.isBlank()) "Attach Vehicle Photo" else "Photo Attached ✓")
      }
    }
  }
}
