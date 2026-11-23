package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.core.ReminderScheduler
import com.example.data.AuthProviderType
import com.example.data.LiveTelemetrySnapshot
import com.example.data.RingtonePreset
import com.example.data.SupabaseAuthService
import com.example.ui.components.RuralPevIcons
import com.example.ui.components.TacticalCard
import com.example.ui.components.TacticalTopBar
import com.example.viewmodel.SessionViewModel
import com.google.firebase.FirebaseApp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private enum class AndroidSettingsCategory(
  val title: String,
  val keywords: String,
  val icon: ImageVector,
  val accentColor: Color,
) {
  DISPLAY_THEMES(
    title = "Appearance, Themes & Windows Studio",
    keywords = "theme preview studio windows 95 2000 xp 7 10 11 fluent mica aero taskbar clock menu bar glass icon pack neon glow readiness quick bar visual style wallpaper pev startup animation logo intro splash duration replay awake spacious layout dark light hud font texture language night vision red shift sunlight opacity glow corner scale fps screen bright charts tooltip",
    icon = RuralPevIcons.ActionThemeStudio,
    accentColor = Color(0xFF00C2FF),
  ),
  AHRS_COCKPIT(
    title = "Dashboard, Sensors & Battery",
    keywords = "satellites gps barometer light sensor vibration tilt motion ahrs 9dof pitch roll yaw lean angle true north compass reserve gauge units miles km temp battery chemistry nmc lfp regen voltage alarm weight ride mode ar solar barn breaker chain lube cold weather odometer decimals auto pause",
    icon = RuralPevIcons.NavTrailheadGauge,
    accentColor = Color(0xFF39FF14),
  ),
  AI_COPILOT(
    title = "AI Helper & Repair Shop",
    keywords = "ai gemini flash lite model thinking creativity voice rural warmth auto speak mechanic repair depth copilot helper chat auto scroll compact toolbar persona",
    icon = RuralPevIcons.NavCopilotCore,
    accentColor = Color(0xFFA855F7),
  ),
  SOUND_NOTIFY(
    title = "Sound, Haptics & Alerts",
    keywords = "sound audio ringtone melody alert chime roger beep walkie dm nav turn low battery vibration haptic feedback pulse tts voice pitch rate volume reminder quiet hours psi maintenance touch acoustic marimba wind noise ducking verbosity detents",
    icon = Icons.Default.VolumeUp,
    accentColor = Color(0xFFFF9F1C),
  ),
  NAV_RADAR(
    title = "Maps, GPS & Speed Rules",
    keywords = "gps navigation cancel confirm arrival route bike trail avoid highway voice guidance reroute charger map heading follow speed limit badge legal compliance governor school zone radar privacy wildlife farm tractor creek washout surface gpx convoy beacon",
    icon = RuralPevIcons.NavRadarDish,
    accentColor = Color(0xFF3B82F6),
  ),
  SAFETY_BOARD(
    title = "Safety, SOS & Community",
    keywords = "safety sos crash g-force emergency sms template biometric wake lock community board feed category limit quick replies headlight dusk",
    icon = Icons.Default.Shield,
    accentColor = Color(0xFFF43F5E),
  ),
  CLOUD_VAULT(
    title = "Accounts & Cloud Backup",
    keywords = "account auth supabase google firebase jwt cloud backup restore firestore sql export import json data saver retention factory reset sign out",
    icon = Icons.Default.CloudDone,
    accentColor = Color(0xFF14B8A6),
  ),
  DIAGNOSTICS(
    title = "Device Health & Diagnostics",
    keywords = "diagnostics satellites hardware sensors barometer light haptic speaker test battery thermal ram storage ping api cloud cache clear",
    icon = RuralPevIcons.ActionHealthPulse,
    accentColor = Color(0xFFEC4899),
  ),
  ABOUT_DEV(
    title = "About Rural ERides GO",
    keywords = "about developer lord bradley callison version build package credits info",
    icon = Icons.Default.Info,
    accentColor = Color(0xFF6366F1),
  ),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMatrixScreen(
  sessionVm: SessionViewModel,
  gpsActive: Boolean = false,
  telemetry: LiveTelemetrySnapshot? = null,
  onReplayStartupIntro: () -> Unit = {},
  onBack: () -> Unit,
) {
  val ctx = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val activity = ctx as? Activity
  val scope = rememberCoroutineScope()
  val settings by sessionVm.settings.collectAsStateWithLifecycle()
  val activeUser by sessionVm.activeUser.collectAsStateWithLifecycle()
  val profile by sessionVm.profile.collectAsStateWithLifecycle()
  val diagResults by sessionVm.diagnosticResults.collectAsStateWithLifecycle()
  val diagBusy by sessionVm.diagnosticsBusy.collectAsStateWithLifecycle()
  val cloudBackupBusy by sessionVm.cloudBackupBusy.collectAsStateWithLifecycle()
  val cloudBackupMessage by sessionVm.cloudBackupMessage.collectAsStateWithLifecycle()

  var statusMsg by remember { mutableStateOf("") }
  var activeCategory by remember { mutableStateOf<AndroidSettingsCategory?>(null) }
  var activeSubCategory by remember { mutableStateOf<String?>(null) }
  var searchQuery by remember { mutableStateOf("") }
  var showSupabaseSql by remember { mutableStateOf(false) }
  var showFactoryResetConfirm by remember { mutableStateOf(false) }

  BackHandler {
    when {
      activeSubCategory != null -> activeSubCategory = null
      activeCategory != null -> activeCategory = null
      else -> onBack()
    }
  }

  val notifPermLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        sessionVm.updateSettings { it.copy(remindersEnabled = true) }
        ReminderScheduler.postNotification(
          ctx,
          "Rural ERides GO Reminders Armed",
          "Tire PSI, maintenance interval, and weather alerts are active.",
        )
        statusMsg = "Notifications authorized & test alert dispatched."
      } else {
        statusMsg = "Notification permission denied in Android settings."
      }
    }

  val exportVaultLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
      if (uri != null) {
        scope.launch {
          val json = sessionVm.exportVaultJson()
          ctx.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
          statusMsg = "JSON Vault exported cleanly."
        }
      }
    }

  val importVaultLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        val raw = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText().orEmpty()
        if (raw.isNotBlank()) {
          sessionVm.importVaultJson(raw) { res ->
            statusMsg = res.fold(onSuccess = { "Vault imported cleanly." }, onFailure = { "Invalid JSON Vault: ${it.message}" })
          }
        }
      }
    }

  val systemRingtonePicker =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
      if (res.resultCode == Activity.RESULT_OK) {
        @Suppress("DEPRECATION")
        val pickedUri = res.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (pickedUri != null) {
          sessionVm.updateSettings {
            it.copy(
              callRingtonePreset = RingtonePreset.SYSTEM_DEFAULT,
              customRingtoneUri = pickedUri.toString(),
            )
          }
          statusMsg = "Custom Android system ringtone saved."
        }
      }
    }

  DisposableEffect(Unit) {
    onDispose { sessionVm.voiceAudio.stopRingtone() }
  }

  fun categorySummary(cat: AndroidSettingsCategory): String =
    when (cat) {
      AndroidSettingsCategory.DISPLAY_THEMES ->
        "${settings.visualStyle.label} • ${if (settings.spaciousLayoutMode) "Spacious View" else "Compact"} • ${if (settings.showStartupAnimation) "Intro ${settings.splashDurationSec}s" else "Intro Off"}"
      AndroidSettingsCategory.AHRS_COCKPIT ->
        "${if (settings.showSatelliteStatusOnDash) "GPS Sats On" else "Sats Hidden"} • ${if (settings.showEnvironmentSensorsOnDash) "Live Sensors" else "Basic"} • ${if (settings.distanceUnit == "mi") "MPH" else "KM/H"}"
      AndroidSettingsCategory.AI_COPILOT ->
        "${settings.aiVoiceProfile.label} • ${if (settings.ruralPersonaMode) "Warm Rural Tone" else "Standard"} • ${settings.copilotPersona}"
      AndroidSettingsCategory.SOUND_NOTIFY ->
        "Vol ${settings.globalVolume}% • ${if (settings.hapticFeedback) "Haptics On" else "Haptics Off"} • ${settings.callRingtonePreset.label}"
      AndroidSettingsCategory.NAV_RADAR ->
        "${settings.navDefaultProfile.label} • ${settings.pevLegalClass.label.substringBefore(" (")} • ${settings.radarRadiusMiles}mi Map"
      AndroidSettingsCategory.SAFETY_BOARD ->
        "Crash ${"%.1f".format(settings.crashSensitivityG)}G • SOS ${settings.sosCancelWindowSec}s • Feed: ${settings.defaultFeedCategory}"
      AndroidSettingsCategory.CLOUD_VAULT ->
        when (activeUser?.provider) {
          AuthProviderType.GOOGLE -> "Google Cloud Sync • ${profile?.callsign?.ifBlank { activeUser?.email } ?: "Signed In"}"
          AuthProviderType.SUPABASE -> "Supabase Account • ${activeUser?.email ?: "Signed In"}"
          null -> "Local Storage • Cloud Backup & Export"
        }
      AndroidSettingsCategory.DIAGNOSTICS -> {
        val satPart = if ((telemetry?.satellitesVisible ?: 0) > 0) "${telemetry?.satellitesUsed}/${telemetry?.satellitesVisible} Satellites • " else ""
        "${satPart}Live Sensors, Haptic & Speaker Tests, Battery & Cache"
      }
      AndroidSettingsCategory.ABOUT_DEV ->
        "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}) • Created by Lord Bradley Callison"
    }

  @Composable
  fun RenderCategoryDetail(cat: AndroidSettingsCategory) {
    when (cat) {
      AndroidSettingsCategory.DISPLAY_THEMES ->
        DisplayThemesSettingsContent(
          settings = settings,
          sessionVm = sessionVm,
          selectedSubCategory = activeSubCategory,
          onSelectSubCategory = { activeSubCategory = it },
          onReplayStartupIntro = onReplayStartupIntro,
        )
      AndroidSettingsCategory.AHRS_COCKPIT ->
        AhrsCockpitBatterySettingsContent(
          settings = settings,
          sessionVm = sessionVm,
          selectedSubCategory = activeSubCategory,
          onSelectSubCategory = { activeSubCategory = it },
        )
      AndroidSettingsCategory.AI_COPILOT ->
        AiCoreChatSettingsContent(
          settings = settings,
          sessionVm = sessionVm,
          selectedSubCategory = activeSubCategory,
          onSelectSubCategory = { activeSubCategory = it },
        )
      AndroidSettingsCategory.SOUND_NOTIFY ->
        SoundRingtonesNotifySettingsContent(
          settings = settings,
          sessionVm = sessionVm,
          selectedSubCategory = activeSubCategory,
          onSelectSubCategory = { activeSubCategory = it },
          onLaunchSystemRingtonePicker = { intent -> runCatching { systemRingtonePicker.launch(intent) } },
          onRequestNotifPermission = { notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
          onStatusMessage = { statusMsg = it },
        )
      AndroidSettingsCategory.NAV_RADAR ->
        NavRadarLegalSettingsContent(
          settings = settings,
          sessionVm = sessionVm,
          selectedSubCategory = activeSubCategory,
          onSelectSubCategory = { activeSubCategory = it },
        )
      AndroidSettingsCategory.SAFETY_BOARD ->
        SafetyCommunitySettingsContent(
          settings = settings,
          sessionVm = sessionVm,
          selectedSubCategory = activeSubCategory,
          onSelectSubCategory = { activeSubCategory = it },
        )
      AndroidSettingsCategory.CLOUD_VAULT -> {
        SettingsAccountAndSecurityCard(
          activeUser = activeUser,
          onLinkGoogleAccount = { if (activity != null) sessionVm.signInWithGoogle(activity) },
          onRefreshSupabaseSession = { sessionVm.refreshSupabaseSessionNow() },
        )
        Spacer(Modifier.height(10.dp))
        TacticalCard(highlight = true, title = "CLOUD_BACKUP.SYS") {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("CLOUD VAULT BACKUP & RESTORE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
          }
          val activeProjectId =
            remember { runCatching { FirebaseApp.getInstance().options.projectId }.getOrNull().orEmpty().ifBlank { "Firebase" } }
          val lockedCallsign = profile?.callsign.orEmpty()
          if (lockedCallsign.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text("PERMANENT CALLSIGN: $lockedCallsign", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
          }
          Text("Build v${BuildConfig.VERSION_NAME} (#${BuildConfig.VERSION_CODE}) • Cloud: $activeProjectId", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
          Spacer(Modifier.height(6.dp))
          AndroidSettingSwitch("Auto Cloud Backup", checked = settings.autoCloudBackupEnabled) { v ->
            sessionVm.updateSettings { it.copy(autoCloudBackupEnabled = v) }
          }
          val backupTimestampText =
            remember(settings.lastCloudBackupMs, settings.lastCloudBackupProvider) {
              if (settings.lastCloudBackupMs > 0L) {
                val formatted = SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(Date(settings.lastCloudBackupMs))
                "Last Backup: $formatted • ${settings.lastCloudBackupProvider.ifBlank { "Cloud" }}"
              } else "No cloud snapshot uploaded yet"
            }
          Text(backupTimestampText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          if (cloudBackupMessage.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
              Text(cloudBackupMessage, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
              IconButton(onClick = { sessionVm.clearCloudBackupMessage() }, modifier = Modifier.testTag("clear_cloud_backup_msg_button")) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss cloud status")
              }
            }
          }
          if (activeUser?.firebaseAuthenticated != true && activity != null) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = { sessionVm.signInWithGoogle(activity) }, modifier = Modifier.fillMaxWidth().testTag("vault_link_google_button")) {
              Icon(Icons.AutoMirrored.Filled.Login, null)
              Spacer(Modifier.width(6.dp))
              Text("Sign In / Link Google (Firestore Board & Vault)")
            }
          }
          Spacer(Modifier.height(8.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { sessionVm.backupAllToCloud() }, enabled = !cloudBackupBusy, modifier = Modifier.weight(1f).testTag("cloud_backup_now_button")) {
              Icon(Icons.Default.CloudUpload, null)
              Spacer(Modifier.width(4.dp))
              Text(if (cloudBackupBusy) "Syncing..." else "Backup Now")
            }
            OutlinedButton(onClick = { sessionVm.restoreAllFromCloud() }, enabled = !cloudBackupBusy, modifier = Modifier.weight(1f).testTag("cloud_restore_now_button")) {
              Icon(Icons.Default.CloudDownload, null)
              Spacer(Modifier.width(4.dp))
              Text("Restore")
            }
          }
          Spacer(Modifier.height(8.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = {
                clipboard.setText(AnnotatedString(SupabaseAuthService.SUPABASE_SCHEMA_SQL))
                statusMsg = "Copied Supabase SQL schema! Paste & Run in Supabase Dashboard -> SQL Editor."
              },
              modifier = Modifier.weight(1f).testTag("copy_supabase_sql_button"),
            ) {
              Icon(Icons.Default.ContentCopy, null)
              Spacer(Modifier.width(4.dp))
              Text("Copy Supabase SQL", maxLines = 1, softWrap = false)
            }
            TextButton(onClick = { showSupabaseSql = !showSupabaseSql }) {
              Text(if (showSupabaseSql) "Hide SQL" else "View SQL")
            }
          }
          if (showSupabaseSql) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
              Text(SupabaseAuthService.SUPABASE_SCHEMA_SQL, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
            }
          }
        }
        Spacer(Modifier.height(10.dp))
        TacticalCard(title = "VAULT_EXPORT.SYS") {
          Text("DATA SAVER, JSON VAULT & RESET", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
          AndroidSettingSwitch("Data Saver Mode (Reduce Image Preloads)", checked = settings.dataSaverMode) { v -> sessionVm.updateSettings { it.copy(dataSaverMode = v) } }
          AndroidSettingSwitch("Auto Cloud Sync on Ride Finish", checked = settings.autoCloudSyncOnFinish) { v -> sessionVm.updateSettings { it.copy(autoCloudSyncOnFinish = v) } }
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("7 Days", "30 Days", "Unlimited").forEach { ret ->
              FilterChip(selected = settings.dataRetentionPolicy == ret, onClick = { sessionVm.updateSettings { it.copy(dataRetentionPolicy = ret) } }, label = { Text(ret) })
            }
          }
          Spacer(Modifier.height(8.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { exportVaultLauncher.launch("rural_erides_vault.json") }, modifier = Modifier.weight(1f).testTag("export_vault_button")) {
              Icon(Icons.Default.Download, null)
              Spacer(Modifier.width(4.dp))
              Text("Export JSON")
            }
            OutlinedButton(onClick = { importVaultLauncher.launch(arrayOf("application/json")) }, modifier = Modifier.weight(1f).testTag("import_vault_button")) {
              Icon(Icons.Default.Upload, null)
              Spacer(Modifier.width(4.dp))
              Text("Import JSON")
            }
          }
          Spacer(Modifier.height(8.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showFactoryResetConfirm = true }, modifier = Modifier.weight(1f).testTag("factory_reset_button")) {
              Icon(Icons.Default.RestartAlt, null)
              Spacer(Modifier.width(4.dp))
              Text("Factory Reset")
            }
            Button(
              onClick = {
                sessionVm.signOut()
                onBack()
              },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
              modifier = Modifier.weight(1f).testTag("sign_out_button"),
            ) {
              Icon(Icons.Default.Logout, null)
              Spacer(Modifier.width(4.dp))
              Text("Sign Out")
            }
          }
        }
      }
      AndroidSettingsCategory.DIAGNOSTICS -> {
        SettingsDiagnosticsAndCacheSection(
          diagnosticResults = diagResults,
          diagnosticsBusy = diagBusy,
          onRunDiagnostics = { sessionVm.runLiveDiagnostics(gpsActive = gpsActive) },
          onStatusMessage = { statusMsg = it },
          telemetry = telemetry,
          voiceAudio = sessionVm.voiceAudio,
        )
      }
      AndroidSettingsCategory.ABOUT_DEV -> AboutDeveloperSettingsContent()
    }
  }

  Scaffold(
    topBar = {
      TacticalTopBar(
        title = activeSubCategory ?: activeCategory?.title ?: "Settings",
        subtitle = if (activeSubCategory != null && activeCategory != null) activeCategory!!.title else "Rural ERides GO",
        onBack = {
          when {
            activeSubCategory != null -> activeSubCategory = null
            activeCategory != null -> activeCategory = null
            else -> onBack()
          }
        },
        backTestTag = "settings_back_button",
      )
    }
  ) { pad ->
    Column(modifier = Modifier.fillMaxSize().padding(pad)) {
      // NATIVE ANDROID SETTINGS PILL SEARCH BAR (SHOWN ON ROOT HUB)
      if (activeCategory == null) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          trailingIcon = {
            if (searchQuery.isNotBlank()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Close, contentDescription = "Clear Search")
              }
            }
          },
          placeholder = { Text("Search settings (satellites, haptics, themes, startup, AI...)") },
          singleLine = true,
          shape = RoundedCornerShape(28.dp),
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).testTag("settings_search_input"),
        )
      }

      if (statusMsg.isNotBlank()) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(statusMsg, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f))
            IconButton(onClick = { statusMsg = "" }) {
              Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
          }
        }
      }

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        val currentCat = activeCategory
        if (currentCat != null) {
          item(key = "cat_detail_${currentCat.name}") {
            RenderCategoryDetail(currentCat)
          }
        } else if (searchQuery.isNotBlank()) {
          val matches =
            AndroidSettingsCategory.entries.filter { cat ->
              cat.title.contains(searchQuery, ignoreCase = true) ||
                cat.keywords.contains(searchQuery, ignoreCase = true) ||
                categorySummary(cat).contains(searchQuery, ignoreCase = true)
            }
          if (matches.isEmpty()) {
            item {
              TacticalCard {
                Text("No settings match \"$searchQuery\". Try searching: AHRS, theme, wallpaper, AI, ringtone, radar, SOS, diagnostics, or developer.", style = MaterialTheme.typography.bodyMedium)
              }
            }
          } else {
            item {
              ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(Modifier.fillMaxWidth()) {
                  matches.forEachIndexed { idx, cat ->
                    AndroidSettingsCategoryRow(
                      icon = cat.icon,
                      iconTint = cat.accentColor,
                      iconBg = cat.accentColor.copy(alpha = 0.16f),
                      title = cat.title,
                      summary = categorySummary(cat),
                      testTagName = "settings_search_cat_${cat.name.lowercase()}",
                      onClick = { activeCategory = cat },
                    )
                    if (idx < matches.lastIndex) {
                      HorizontalDivider(modifier = Modifier.padding(start = 72.dp, end = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                  }
                }
              }
            }
          }
        } else {
          // GROUP 1: LOOK, FEEL & RIDE SCREEN
          item(key = "group_personalization") {
            AndroidSettingsGroupHeader("Look, Feel & Ride Screen")
            ElevatedCard(
              shape = RoundedCornerShape(22.dp),
              colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              val group1 =
                listOf(
                  AndroidSettingsCategory.DISPLAY_THEMES,
                  AndroidSettingsCategory.AHRS_COCKPIT,
                  AndroidSettingsCategory.AI_COPILOT,
                )
              Column(Modifier.fillMaxWidth()) {
                group1.forEachIndexed { idx, cat ->
                  AndroidSettingsCategoryRow(
                    icon = cat.icon,
                    iconTint = cat.accentColor,
                    iconBg = cat.accentColor.copy(alpha = 0.16f),
                    title = cat.title,
                    summary = categorySummary(cat),
                    testTagName = "settings_cat_${cat.name.lowercase()}",
                    onClick = { activeCategory = cat },
                  )
                  if (idx < group1.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp, end = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                  }
                }
              }
            }
          }

          // GROUP 2: SOUND, MAPS & SAFETY
          item(key = "group_riding_safety") {
            AndroidSettingsGroupHeader("Sound, Maps & Safety")
            ElevatedCard(
              shape = RoundedCornerShape(22.dp),
              colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              val group2 =
                listOf(
                  AndroidSettingsCategory.SOUND_NOTIFY,
                  AndroidSettingsCategory.NAV_RADAR,
                  AndroidSettingsCategory.SAFETY_BOARD,
                )
              Column(Modifier.fillMaxWidth()) {
                group2.forEachIndexed { idx, cat ->
                  AndroidSettingsCategoryRow(
                    icon = cat.icon,
                    iconTint = cat.accentColor,
                    iconBg = cat.accentColor.copy(alpha = 0.16f),
                    title = cat.title,
                    summary = categorySummary(cat),
                    testTagName = "settings_cat_${cat.name.lowercase()}",
                    onClick = { activeCategory = cat },
                  )
                  if (idx < group2.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp, end = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                  }
                }
              }
            }
          }

          // GROUP 3: ACCOUNT, DEVICE HEALTH & ABOUT
          item(key = "group_system_about") {
            AndroidSettingsGroupHeader("Account, Device Health & About")
            ElevatedCard(
              shape = RoundedCornerShape(22.dp),
              colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              val group3 =
                listOf(
                  AndroidSettingsCategory.CLOUD_VAULT,
                  AndroidSettingsCategory.DIAGNOSTICS,
                  AndroidSettingsCategory.ABOUT_DEV,
                )
              Column(Modifier.fillMaxWidth()) {
                group3.forEachIndexed { idx, cat ->
                  AndroidSettingsCategoryRow(
                    icon = cat.icon,
                    iconTint = cat.accentColor,
                    iconBg = cat.accentColor.copy(alpha = 0.16f),
                    title = cat.title,
                    summary = categorySummary(cat),
                    testTagName = "settings_cat_${cat.name.lowercase()}",
                    onClick = { activeCategory = cat },
                  )
                  if (idx < group3.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp, end = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (showFactoryResetConfirm) {
    com.example.ui.components.TacticalDialog(
      onDismissRequest = { showFactoryResetConfirm = false },
      title = "Confirm Factory Reset",
      subtitle = "Settings & Appearance Defaults",
      icon = Icons.Default.Warning,
      badgeText = "RESET",
      confirmButton = {
        com.example.ui.components.TacticalButton(
          onClick = {
            sessionVm.resetSettings()
            statusMsg = "All settings restored to factory defaults."
            showFactoryResetConfirm = false
          },
          isDestructive = true,
        ) { Text("Reset Defaults") }
      },
      dismissButton = {
        com.example.ui.components.TacticalOutlinedButton(onClick = { showFactoryResetConfirm = false }) { Text("Cancel") }
      },
    ) {
      Text(
        "Reset all app appearance, AHRS, unit, radar, and AI settings to factory defaults? Your saved vehicles and rides will stay safe.",
        style = MaterialTheme.typography.bodyMedium,
      )
    }
  }
}
