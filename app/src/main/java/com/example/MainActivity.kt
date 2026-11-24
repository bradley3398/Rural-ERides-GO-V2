package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ElectricBike
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.ElectricScooter
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.Coil
import coil.ImageLoader
import com.example.core.AppStrings
import com.example.core.configureAppCheck
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import com.example.data.VisualStyleMode
import com.example.ui.components.BroadcastBanner
import com.example.ui.components.TacticalBottomNavigationHost
import com.example.ui.components.TacticalNavTabItem
import com.example.ui.components.TacticalSideNavigationRail
import com.example.ui.components.TacticalTopRibbonBar
import com.example.ui.components.YoutubeMiniPlayerHost
import com.example.ui.components.ensureOsmdroidConfigured
import com.example.ui.components.formatSpeedValue
import com.example.ui.components.isWindowsClassicActive
import com.example.ui.components.navBarMiniPlayerBottomOffset
import com.example.ui.components.speedUnitLabel
import com.example.ui.components.windowsClassicBevel
import com.example.ui.screens.AppInfoAndChangelogScreen
import com.example.ui.screens.AuthLoginScreen
import com.example.ui.screens.BiometricLockScreen
import com.example.ui.screens.BoardScreen
import com.example.ui.screens.CallsignAndLegalOnboardingScreen
import com.example.ui.screens.ChannelScreen
import com.example.ui.screens.CockpitScreen
import com.example.ui.screens.CopilotScreen
import com.example.ui.screens.CreatorArcadeHubScreen
import com.example.ui.screens.LiveCallOverlayHost
import com.example.ui.screens.MechanicScreen
import com.example.ui.screens.RiderRadarScreen
import com.example.ui.screens.RideTelemetryStudioScreen
import com.example.ui.screens.RidersPrayerModal
import com.example.hardware.VoiceAudioManager
import com.example.ui.screens.SettingsMatrixScreen
import com.example.ui.screens.StartupSplashSequence
import com.example.ui.theme.RuralERidesTheme
import com.example.ui.theme.TacticalBackgroundCanvas
import com.example.ui.theme.TacticalColors
import com.example.viewmodel.CockpitViewModel
import com.example.viewmodel.CopilotSocialViewModel
import com.example.viewmodel.MechanicViewModel
import com.example.viewmodel.RadarViewModel
import com.example.viewmodel.SessionViewModel

private enum class MainTab(val icon: ImageVector, val testTag: String) {
  COCKPIT(com.example.ui.components.RuralPevIcons.NavTrailheadGauge, "nav_cockpit"),
  RADAR(com.example.ui.components.RuralPevIcons.NavRadarDish, "nav_radar"),
  MECHANIC(com.example.ui.components.RuralPevIcons.NavBarnMechanic, "nav_mechanic"),
  COPILOT(com.example.ui.components.RuralPevIcons.NavCopilotCore, "nav_copilot"),
  CHANNEL(com.example.ui.components.RuralPevIcons.NavChannelBroadcast, "nav_channel"),
  BOARD(com.example.ui.components.RuralPevIcons.NavPorchBoard, "nav_board"),
}

private enum class OverlayPage { NONE, SETTINGS, INFO, TELEMETRY_STUDIO, CREATOR_HUB }

class MainActivity : FragmentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureAppCheck(this, intent)
    ensureCoilImageLoaderConfigured(applicationContext)
    ensureOsmdroidConfigured(this)
    enableEdgeToEdge()

    setContent {
      val sessionVm: SessionViewModel = viewModel()
      val cockpitVm: CockpitViewModel = viewModel()
      val radarVm: RadarViewModel = viewModel()
      val mechanicVm: MechanicViewModel = viewModel()
      val socialVm: CopilotSocialViewModel = viewModel()

      val settings by sessionVm.settings.collectAsStateWithLifecycle()
      val activeUser by sessionVm.activeUser.collectAsStateWithLifecycle()
      val sbSession by sessionVm.supabaseSession.collectAsStateWithLifecycle()
      val profile by sessionVm.profile.collectAsStateWithLifecycle()
      val broadcasts by sessionVm.broadcasts.collectAsStateWithLifecycle()
      val prayers by sessionVm.prayers.collectAsStateWithLifecycle()
      val dailyPrayerState by sessionVm.dailyPrayerState.collectAsStateWithLifecycle()
      val prayerOpen by sessionVm.prayerOverlayOpen.collectAsStateWithLifecycle()
      val biometricLocked by sessionVm.biometricLocked.collectAsStateWithLifecycle()
      val authBusy by sessionVm.authBusy.collectAsStateWithLifecycle()
      val authMsg by sessionVm.authStatusMessage.collectAsStateWithLifecycle()
      val liveTelemetry by cockpitVm.telemetry.collectAsStateWithLifecycle()
      var startupSplashDone by rememberSaveable { mutableStateOf(false) }

      LaunchedEffect(activeUser?.uid, activeUser?.firebaseAuthenticated, sbSession?.accessToken) {
        val uid = activeUser?.uid
        val fbAuth = activeUser?.firebaseAuthenticated == true
        val sbToken = sbSession?.accessToken.orEmpty()
        cockpitVm.bindUser(uid, fbAuth)
        radarVm.bindCloudSession(uid, fbAuth, sbToken)
        socialVm.bindUser(
          uid = uid,
          firebaseAuthenticated = fbAuth,
          defaultPersona = settings.copilotPersona,
          supabaseAccessToken = sbToken,
        )
      }

      LaunchedEffect(settings.biometricAppLock, activeUser?.uid) {
        if (settings.biometricAppLock && activeUser != null) {
          sessionVm.setBiometricLockState(true)
        }
      }

      val navActive by radarVm.activeNavRoute.collectAsStateWithLifecycle()
      val shouldKeepAwake =
        settings.wakeLockEnabled ||
          (settings.keepScreenAwakeRiding && (liveTelemetry.recording || navActive != null))

      DisposableEffect(shouldKeepAwake) {
        if (shouldKeepAwake) {
          window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
          window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
      }

      RuralERidesTheme(settings = settings) {
        TacticalBackgroundCanvas(texture = settings.bgTexture) {
          when {
            activeUser == null -> {
              AuthLoginScreen(
                busy = authBusy,
                statusMessage = authMsg,
                supabaseConfigured = remember { sessionVm.supabaseService.isConfigured() },
                onSignInGoogle = { sessionVm.signInWithGoogle(this@MainActivity) },
                onSupabaseSignIn = { email, pass -> sessionVm.signInWithSupabase(email, pass) },
                onSupabaseSignUp = { email, pass, cs -> sessionVm.signUpWithSupabase(email, pass, cs) },
                onSupabaseResetPassword = { email -> sessionVm.sendSupabasePasswordReset(email) },
                onSupabaseResendConfirmation = { email -> sessionVm.resendSupabaseConfirmation(email) },
                onOpenPrayer = { sessionVm.openPrayerOverlay(true) },
              )
            }
            biometricLocked -> {
              BiometricLockScreen(
                voiceAudio = sessionVm.voiceAudio,
                onUnlocked = { sessionVm.setBiometricLockState(false) },
                onSignOut = { sessionVm.signOut() },
              )
            }
            profile == null || profile?.acceptedLegal != true || profile?.callsign.isNullOrBlank() -> {
              CallsignAndLegalOnboardingScreen(
                defaultName = activeUser?.displayName ?: "",
                busy = authBusy,
                statusMessage = authMsg,
                onSubmit = { callsign, displayName, unitSystem, emergencyName, sosNum, medInfo ->
                  sessionVm.saveCallsignAndLegal(
                    callsign = callsign,
                    displayName = displayName,
                    unitSystem = unitSystem,
                    emergencyName = emergencyName,
                    sosNumber = sosNum,
                    medicalInfo = medInfo,
                    acceptedLegal = true,
                  )
                },
              )
            }
            else -> {
              MainTacticalScaffold(
                localeCode = settings.locale,
                topBroadcast = broadcasts.firstOrNull(),
                onReplayStartupIntro = { startupSplashDone = false },
                sessionVm = sessionVm,
                cockpitVm = cockpitVm,
                radarVm = radarVm,
                mechanicVm = mechanicVm,
                socialVm = socialVm,
              )
            }
          }

          if (prayerOpen) {
            RidersPrayerModal(
              callsign = profile?.callsign ?: "Pilot",
              prayerState = dailyPrayerState,
              journal = prayers,
              voiceAudio = sessionVm.voiceAudio,
              canSaveToCloud = activeUser != null,
              onReroll = { sessionVm.loadDailyPrayer(profile?.callsign ?: "Pilot") },
              onSaveJournal = { t, b, c -> sessionVm.savePrayerToJournal(t, b, c) },
              onDeleteJournal = { id -> sessionVm.deletePrayer(id) },
              onDismiss = { sessionVm.openPrayerOverlay(false) },
            )
          }

          if (!startupSplashDone && settings.showStartupAnimation && !settings.reducedMotion) {
            StartupSplashSequence(
              hapticsEnabled = settings.hapticFeedback,
              voiceAudio = sessionVm.voiceAudio,
              activeSensorCount = liveTelemetry.activeSensorsList.size,
              satellitesVisible = liveTelemetry.satellitesVisible,
              durationSec = settings.splashDurationSec,
              onFinished = { startupSplashDone = true },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MainTacticalScaffold(
  localeCode: String,
  topBroadcast: com.example.data.EmergencyBroadcast?,
  onReplayStartupIntro: () -> Unit,
  sessionVm: SessionViewModel,
  cockpitVm: CockpitViewModel,
  radarVm: RadarViewModel,
  mechanicVm: MechanicViewModel,
  socialVm: CopilotSocialViewModel,
) {
  var activeTabIdx by rememberSaveable { mutableIntStateOf(0) }
  var overlayPage by remember { mutableStateOf(OverlayPage.NONE) }
  var telemetryStudioRideId by remember { mutableStateOf<String?>(null) }
  var startMenuOpen by remember { mutableStateOf(false) }
  val tabs = MainTab.entries
  val settings by sessionVm.settings.collectAsStateWithLifecycle()
  val telemetry by cockpitVm.telemetry.collectAsStateWithLifecycle()
  val activeVideo by socialVm.activeVideo.collectAsStateWithLifecycle()
  val playerExpanded by socialVm.playerExpanded.collectAsStateWithLifecycle()
  val isClassic = isWindowsClassicActive(settings)

  val vehicles by cockpitVm.vehicles.collectAsStateWithLifecycle()
  val profile by sessionVm.profile.collectAsStateWithLifecycle()
  val activeVehicleWh =
    remember(vehicles, profile?.activeVehicleId) {
      (vehicles.firstOrNull { it.id == profile?.activeVehicleId } ?: vehicles.firstOrNull())?.batteryWh ?: 1500L
    }

  LaunchedEffect(settings.telemetryRateHz, settings.imuPollingRateHz, settings.ahrsGaugeDamping) {
    if (cockpitVm.telemetryEngine.hasLocationPermission()) {
      cockpitVm.startHardwareFeeds(settings)
    } else {
      cockpitVm.startSensorsOnly(settings)
    }
  }

  LaunchedEffect(telemetry.lat, telemetry.lng, telemetry.speedKmh) {
    if (telemetry.lat != 0.0 || telemetry.lng != 0.0) {
      radarVm.updateLiveNavigationProgress(
        lat = telemetry.lat,
        lng = telemetry.lng,
        speedKmh = telemetry.speedKmh,
        settings = settings,
        vehicleWh = activeVehicleWh,
      )
    }
  }

  var lastAutoNightDark by remember { mutableStateOf<Boolean?>(null) }
  LaunchedEffect(settings.autoNightFromLightSensor, telemetry.ambientLightLux) {
    if (settings.autoNightFromLightSensor && telemetry.ambientLightLux >= 0f) {
      val isDarkEnv = telemetry.ambientLightLux < 14f
      val isBrightEnv = telemetry.ambientLightLux > 45f
      if (isDarkEnv && lastAutoNightDark != true) {
        lastAutoNightDark = true
        if (!settings.nightVisionMode) sessionVm.updateSettings { it.copy(nightVisionMode = true, sunVisibilityMode = false) }
      } else if (isBrightEnv && lastAutoNightDark != false) {
        lastAutoNightDark = false
        if (settings.nightVisionMode) sessionVm.updateSettings { it.copy(nightVisionMode = false) }
      }
    }
  }

  fun selectMainTab(idx: Int) {
    if (activeTabIdx != idx) {
      sessionVm.voiceAudio.triggerHapticCue(
        enabled = settings.hapticFeedback && settings.buttonHapticFeedback,
        cue = VoiceAudioManager.HapticCue.LIGHT_TICK,
        durationMs = settings.hapticDurationMs,
      )
      sessionVm.voiceAudio.playTouchSound(
        enabled = settings.touchSoundsEnabled,
        pack = settings.touchSoundPack,
        cue = VoiceAudioManager.TouchCue.TAB_SWITCH,
        volumePct = settings.globalVolume,
      )
      activeTabIdx = idx
    }
  }

  val labels =
    remember(localeCode) {
      val loc = AppStrings.forLocale(localeCode)
      listOf(loc.cockpit, loc.radar, loc.mechanic, loc.copilot, loc.channel, loc.board)
    }

  when (overlayPage) {
    OverlayPage.SETTINGS -> {
      SettingsMatrixScreen(
        sessionVm = sessionVm,
        gpsActive = telemetry.lat != 0.0,
        telemetry = telemetry,
        onReplayStartupIntro = {
          overlayPage = OverlayPage.NONE
          onReplayStartupIntro()
        },
        onBack = { overlayPage = OverlayPage.NONE },
      )
      return
    }
    OverlayPage.INFO -> {
      AppInfoAndChangelogScreen(
        sessionVm = sessionVm,
        cockpitVm = cockpitVm,
        onBack = { overlayPage = OverlayPage.NONE },
      )
      return
    }
    OverlayPage.TELEMETRY_STUDIO -> {
      RideTelemetryStudioScreen(
        sessionVm = sessionVm,
        cockpitVm = cockpitVm,
        initialRideId = telemetryStudioRideId,
        onBack = {
          telemetryStudioRideId = null
          overlayPage = OverlayPage.NONE
        },
      )
      return
    }
    OverlayPage.CREATOR_HUB -> {
      CreatorArcadeHubScreen(
        sessionVm = sessionVm,
        cockpitVm = cockpitVm,
        socialVm = socialVm,
        onBack = { overlayPage = OverlayPage.NONE },
      )
      return
    }
    OverlayPage.NONE -> Unit
  }

  BoxWithConstraints(Modifier.fillMaxSize()) {
    val styleKey = settings.navBarStyle
    val navTabItems =
      remember(labels) {
        tabs.mapIndexed { idx, tab ->
          TacticalNavTabItem(
            label = labels.getOrElse(idx) { tab.name },
            icon = tab.icon,
            testTag = tab.testTag,
          )
        }
      }
    val useRightRail = styleKey == "right_rail"
    val useTopRibbon = styleKey == "top_ribbon"
    val useCustomBottomBar =
      styleKey == "rural_timber" ||
        styleKey == "compact_capsule" ||
        styleKey == "dock" ||
        styleKey == "cyber_deck" ||
        styleKey == "neon_underbar" ||
        styleKey == "split_command" ||
        styleKey == "brutal_strip"
    val useLeftRail =
      !useRightRail && !useTopRibbon && !useCustomBottomBar &&
        (maxWidth >= 600.dp || styleKey == "rail") && styleKey != "classic_taskbar"
    val useClassicTaskbar =
      !useLeftRail && !useRightRail && !useTopRibbon && !useCustomBottomBar &&
        (isClassic || styleKey == "classic_taskbar")
    val telemetryTicker =
      remember(telemetry.speedKmh, telemetry.batteryPct, telemetry.satellitesUsed, settings.distanceUnit, settings.rideMode) {
        val spd = "%.0f %s".format(formatSpeedValue(telemetry.speedKmh, settings.distanceUnit), speedUnitLabel(settings.distanceUnit))
        "$spd • BAT ${telemetry.batteryPct}% • ${telemetry.satellitesUsed} SATS • ${settings.rideMode}"
      }

    Scaffold(
      containerColor = Color.Transparent,
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      topBar = {
        if (useTopRibbon) {
          TacticalTopRibbonBar(
            tabs = navTabItems,
            activeTabIdx = activeTabIdx,
            onSelectTab = { selectMainTab(it) },
          )
        }
      },
      bottomBar = {
        when {
          useLeftRail || useRightRail || useTopRibbon -> Unit
          useClassicTaskbar -> {
            val isXp = settings.visualStyle == VisualStyleMode.WINDOWS_XP
            val isWin7 = settings.visualStyle == VisualStyleMode.WINDOWS_7
            val isWin10 = settings.visualStyle == VisualStyleMode.WINDOWS_10
            val isWin11 = settings.visualStyle == VisualStyleMode.WINDOWS_11
            val is2k = settings.visualStyle == VisualStyleMode.WINDOWS_2000
            val startMenuDropdown: @Composable () -> Unit = {
              com.example.ui.components.AuthenticWindowsStartMenuPopup(
                expanded = startMenuOpen,
                visualStyle = settings.visualStyle,
                pilotCallsign = profile?.callsign?.ifBlank { "Rider" } ?: "Rider",
                tabLabels = labels,
                tabIcons = tabs.map { it.icon },
                onDismiss = { startMenuOpen = false },
                onSelectTab = { selectMainTab(it) },
                onCycleTheme = {
                  val all = VisualStyleMode.entries
                  val next = all[(all.indexOf(settings.visualStyle) + 1) % all.size]
                  sessionVm.updateSettings { it.copy(visualStyle = next) }
                },
                onOpenSettings = { overlayPage = OverlayPage.SETTINGS },
                onOpenInfo = { overlayPage = OverlayPage.INFO },
                onOpenTelemetryStudio = {
                  telemetryStudioRideId = null
                  overlayPage = OverlayPage.TELEMETRY_STUDIO
                },
                onOpenPrayer = { sessionVm.openPrayerOverlay(true) },
                onSignOut = { sessionVm.signOut() },
              )
            }
            when {
              isXp -> {
                com.example.ui.components.WindowsXpLunaTaskbar(
                  tabLabels = labels,
                  tabIcons = tabs.map { it.icon },
                  tabTestTags = tabs.map { it.testTag },
                  activeTabIdx = activeTabIdx,
                  onSelectTab = { selectMainTab(it) },
                  startMenuOpen = startMenuOpen,
                  onToggleStartMenu = { startMenuOpen = !startMenuOpen },
                  showClock = settings.winShowTaskbarClock,
                  compact = settings.winTaskbarCompact,
                  startMenuContent = startMenuDropdown,
                )
              }
              isWin7 -> {
                com.example.ui.components.Windows7AeroTaskbar(
                  tabLabels = labels,
                  tabIcons = tabs.map { it.icon },
                  tabTestTags = tabs.map { it.testTag },
                  activeTabIdx = activeTabIdx,
                  onSelectTab = { selectMainTab(it) },
                  startMenuOpen = startMenuOpen,
                  onToggleStartMenu = { startMenuOpen = !startMenuOpen },
                  onAeroPeekHome = { selectMainTab(0) },
                  showClock = settings.winShowTaskbarClock,
                  compact = settings.winTaskbarCompact,
                  startMenuContent = startMenuDropdown,
                )
              }
              isWin10 -> {
                com.example.ui.components.Windows10FluentTaskbar(
                  tabLabels = labels,
                  tabIcons = tabs.map { it.icon },
                  tabTestTags = tabs.map { it.testTag },
                  activeTabIdx = activeTabIdx,
                  onSelectTab = { selectMainTab(it) },
                  startMenuOpen = startMenuOpen,
                  onToggleStartMenu = { startMenuOpen = !startMenuOpen },
                  showClock = settings.winShowTaskbarClock,
                  compact = settings.winTaskbarCompact,
                  startMenuContent = startMenuDropdown,
                )
              }
              isWin11 -> {
                com.example.ui.components.Windows11MicaTaskbar(
                  tabLabels = labels,
                  tabIcons = tabs.map { it.icon },
                  tabTestTags = tabs.map { it.testTag },
                  activeTabIdx = activeTabIdx,
                  onSelectTab = { selectMainTab(it) },
                  startMenuOpen = startMenuOpen,
                  onToggleStartMenu = { startMenuOpen = !startMenuOpen },
                  showClock = settings.winShowTaskbarClock,
                  compact = settings.winTaskbarCompact,
                  startMenuContent = startMenuDropdown,
                )
              }
              else -> {
                com.example.ui.components.WindowsClassic95Taskbar(
                  is2k = is2k,
                  tabLabels = labels,
                  tabIcons = tabs.map { it.icon },
                  tabTestTags = tabs.map { it.testTag },
                  activeTabIdx = activeTabIdx,
                  onSelectTab = { selectMainTab(it) },
                  startMenuOpen = startMenuOpen,
                  onToggleStartMenu = { startMenuOpen = !startMenuOpen },
                  showClock = settings.winShowTaskbarClock,
                  compact = settings.winTaskbarCompact,
                  startMenuContent = startMenuDropdown,
                )
              }
            }
          }
          else -> {
            TacticalBottomNavigationHost(
              styleKey = styleKey,
              tabs = navTabItems,
              activeTabIdx = activeTabIdx,
              telemetryTicker = telemetryTicker,
              copilotTabIdx = MainTab.COPILOT.ordinal,
              onSelectTab = { selectMainTab(it) },
            )
          }
        }
      },
    ) { innerPadding ->
      Row(Modifier.fillMaxSize().padding(innerPadding)) {
        if (useLeftRail) {
          TacticalSideNavigationRail(
            tabs = navTabItems,
            activeTabIdx = activeTabIdx,
            isRightSide = false,
            onSelectTab = { selectMainTab(it) },
          )
        }

        Column(
          Modifier
            .weight(1f)
            .fillMaxSize()
            .then(if (useTopRibbon) Modifier else Modifier.statusBarsPadding()),
        ) {
          if (topBroadcast != null) {
            BroadcastBanner(broadcast = topBroadcast)
          }
          Box(Modifier.weight(1f).fillMaxSize()) {
            when (tabs[activeTabIdx]) {
              MainTab.COCKPIT ->
                CockpitScreen(
                  sessionVm = sessionVm,
                  cockpitVm = cockpitVm,
                  radarVm = radarVm,
                  socialVm = socialVm,
                  onOpenRadar = { selectMainTab(MainTab.RADAR.ordinal) },
                  onOpenSettings = { overlayPage = OverlayPage.SETTINGS },
                  onOpenInfo = { overlayPage = OverlayPage.INFO },
                  onOpenTelemetryStudio = { rideId ->
                    telemetryStudioRideId = rideId
                    overlayPage = OverlayPage.TELEMETRY_STUDIO
                  },
                  onOpenCreatorHub = { overlayPage = OverlayPage.CREATOR_HUB },
                )
              MainTab.RADAR ->
                RiderRadarScreen(
                  sessionVm = sessionVm,
                  cockpitVm = cockpitVm,
                  radarVm = radarVm,
                  socialVm = socialVm,
                )
              MainTab.MECHANIC ->
                MechanicScreen(
                  sessionVm = sessionVm,
                  cockpitVm = cockpitVm,
                  radarVm = radarVm,
                  mechanicVm = mechanicVm,
                  socialVm = socialVm,
                )
              MainTab.COPILOT ->
                CopilotScreen(
                  sessionVm = sessionVm,
                  cockpitVm = cockpitVm,
                  socialVm = socialVm,
                )
              MainTab.CHANNEL ->
                ChannelScreen(
                  sessionVm = sessionVm,
                  socialVm = socialVm,
                )
              MainTab.BOARD ->
                BoardScreen(
                  sessionVm = sessionVm,
                  cockpitVm = cockpitVm,
                  socialVm = socialVm,
                )
            }
          }
        }

        if (useRightRail) {
          TacticalSideNavigationRail(
            tabs = navTabItems,
            activeTabIdx = activeTabIdx,
            isRightSide = true,
            onSelectTab = { selectMainTab(it) },
          )
        }
      }
    }

    if (activeVideo != null) {
      val dockModifier =
        if (playerExpanded) {
          Modifier.fillMaxSize()
        } else {
          val bottomPad =
            navBarMiniPlayerBottomOffset(
              styleKey = styleKey,
              useLeftRail = useLeftRail,
              useRightRail = useRightRail,
              useTopRibbon = useTopRibbon,
              useClassicTaskbar = useClassicTaskbar,
            )
          Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(
              start = if (useLeftRail) 84.dp else 0.dp,
              end = if (useRightRail) 84.dp else 0.dp,
              bottom = bottomPad,
            )
        }
      Box(modifier = dockModifier) {
        YoutubeMiniPlayerHost(sessionVm = sessionVm, socialVm = socialVm)
      }
    }

    LiveCallOverlayHost(sessionVm = sessionVm, socialVm = socialVm)
  }
}

@Volatile private var coilLoaderInitialized = false

private fun ensureCoilImageLoaderConfigured(appContext: android.content.Context) {
  if (coilLoaderInitialized) return
  synchronized(MainActivity::class.java) {
    if (coilLoaderInitialized) return
    val imageHttpClient =
      OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor { chain ->
          val orig = chain.request()
          val req =
            orig
              .newBuilder()
              .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36",
              )
              .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
              .build()
          chain.proceed(req)
        }
        .build()
    val loader =
      ImageLoader.Builder(appContext)
        .okHttpClient(imageHttpClient)
        .crossfade(true)
        .respectCacheHeaders(false)
        .build()
    Coil.setImageLoader(loader)
    coilLoaderInitialized = true
  }
}

