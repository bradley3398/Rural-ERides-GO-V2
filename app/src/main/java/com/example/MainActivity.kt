package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.AppUpdateCheckState
import com.example.core.ReminderScheduler
import com.example.core.configureAppCheck
import com.example.data.VisualStyleMode
import com.example.hardware.VoiceAudioManager
import com.example.ui.compartments.AuthAndOnboardingCompartmentHost
import com.example.ui.compartments.CompartmentHostRoot
import com.example.ui.compartments.OverlayCompartment
import com.example.ui.compartments.OverlayCompartmentHost
import com.example.ui.compartments.PrimaryTabCompartment
import com.example.ui.compartments.PrimaryTabCompartmentHost
import com.example.ui.compartments.rememberCompartmentPopupsState
import com.example.ui.components.AuthenticWindowsStartMenuPopup
import com.example.ui.components.TacticalBottomNavigationHost
import com.example.ui.components.TacticalNavTabItem
import com.example.ui.components.TacticalSideNavigationRail
import com.example.ui.components.TacticalTopRibbonBar
import com.example.ui.components.Windows10FluentTaskbar
import com.example.ui.components.Windows11MicaTaskbar
import com.example.ui.components.Windows7AeroTaskbar
import com.example.ui.components.WindowsClassic95Taskbar
import com.example.ui.components.WindowsXpLunaTaskbar
import com.example.ui.components.YoutubeMiniPlayerHost
import com.example.ui.components.isAnyWindowsDesktopActive
import com.example.ui.components.isWindows10Active
import com.example.ui.components.isWindows11Active
import com.example.ui.components.isWindows7Active
import com.example.ui.components.isWindowsXpActive
import com.example.ui.components.navBarMiniPlayerBottomOffset
import com.example.ui.screens.AppUpdateAvailableBanner
import com.example.ui.screens.AppUpdateAvailableDialog
import com.example.ui.screens.DirectMessagesSheet
import com.example.ui.screens.LiveCallOverlayHost
import com.example.ui.screens.RidersPrayerModal
import com.example.ui.screens.StartupSplashSequence
import com.example.ui.theme.RuralERidesTheme
import com.example.ui.theme.TacticalBackgroundCanvas
import com.example.ui.theme.sanitizeVisualStyleForSeason
import com.example.viewmodel.CockpitViewModel
import com.example.viewmodel.CopilotSocialViewModel
import com.example.viewmodel.MechanicViewModel
import com.example.viewmodel.RadarViewModel
import com.example.viewmodel.SessionViewModel

class MainActivity : FragmentActivity() {
  private val openUpdatesFromNotif = mutableStateOf(false)
  private val pendingTargetTab = mutableStateOf<String?>(null)
  private val pendingOpenDm = mutableStateOf(false)
  private val pendingOAuthUri = mutableStateOf<Uri?>(null)

  private fun captureIntentExtras(incoming: Intent?) {
    if (incoming?.getBooleanExtra(ReminderScheduler.EXTRA_OPEN_UPDATES, false) == true) {
      openUpdatesFromNotif.value = true
    }
    incoming?.getStringExtra(ReminderScheduler.EXTRA_TARGET_TAB)?.takeIf { it.isNotBlank() }?.let {
      pendingTargetTab.value = it
    }
    if (incoming?.getBooleanExtra(ReminderScheduler.EXTRA_OPEN_DM, false) == true) {
      pendingOpenDm.value = true
    }
    val uri = incoming?.data ?: return
    if (uri.scheme == "ruralerides" && uri.host == "auth") {
      pendingOAuthUri.value = uri
      incoming.data = null
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureAppCheck(this, intent)
    captureIntentExtras(intent)
    enableEdgeToEdge()
    setContent {
      VoltTrailRootApp(
        activity = this,
        openUpdatesFromNotification = openUpdatesFromNotif.value,
        onConsumedOpenUpdatesNotification = { openUpdatesFromNotif.value = false },
        pendingTargetTab = pendingTargetTab.value,
        onConsumedTargetTab = { pendingTargetTab.value = null },
        pendingOpenDm = pendingOpenDm.value,
        onConsumedOpenDm = { pendingOpenDm.value = false },
        pendingOAuthUri = pendingOAuthUri.value,
        onConsumedOAuthUri = { pendingOAuthUri.value = null },
      )
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    captureIntentExtras(intent)
  }
}

@Composable
fun VoltTrailRootApp(
  activity: FragmentActivity,
  openUpdatesFromNotification: Boolean = false,
  onConsumedOpenUpdatesNotification: () -> Unit = {},
  pendingTargetTab: String? = null,
  onConsumedTargetTab: () -> Unit = {},
  pendingOpenDm: Boolean = false,
  onConsumedOpenDm: () -> Unit = {},
  pendingOAuthUri: Uri? = null,
  onConsumedOAuthUri: () -> Unit = {},
  sessionVm: SessionViewModel = viewModel(),
  cockpitVm: CockpitViewModel = viewModel(),
  radarVm: RadarViewModel = viewModel(),
  mechanicVm: MechanicViewModel = viewModel(),
  socialVm: CopilotSocialViewModel = viewModel(),
) {
  val settings by sessionVm.settings.collectAsStateWithLifecycle()
  val activeUser by sessionVm.activeUser.collectAsStateWithLifecycle()
  val profile by sessionVm.profile.collectAsStateWithLifecycle()
  val authBusy by sessionVm.authBusy.collectAsStateWithLifecycle()
  val authStatusMessage by sessionVm.authStatusMessage.collectAsStateWithLifecycle()
  val biometricLocked by sessionVm.biometricLocked.collectAsStateWithLifecycle()
  val prayerOverlayOpen by sessionVm.prayerOverlayOpen.collectAsStateWithLifecycle()
  val dailyPrayerState by sessionVm.dailyPrayerState.collectAsStateWithLifecycle()
  val prayers by sessionVm.prayers.collectAsStateWithLifecycle()
  val appUpdateState by sessionVm.appUpdateState.collectAsStateWithLifecycle()
  val updateModalOpen by sessionVm.updateModalOpen.collectAsStateWithLifecycle()
  val updateBannerDismissed by sessionVm.updateBannerDismissed.collectAsStateWithLifecycle()

  val telemetry by cockpitVm.telemetry.collectAsStateWithLifecycle()
  val vehicles by cockpitVm.vehicles.collectAsStateWithLifecycle()

  val activeVehicle =
    remember(vehicles, profile?.activeVehicleId) {
      vehicles.firstOrNull { it.id == profile?.activeVehicleId } ?: vehicles.firstOrNull()
    }

  LaunchedEffect(Unit) {
    sessionVm.checkForAppUpdatesIfNeededOnStartup()
  }

  LaunchedEffect(openUpdatesFromNotification) {
    if (openUpdatesFromNotification) {
      sessionVm.openUpdateModal(true)
      onConsumedOpenUpdatesNotification()
    }
  }

  LaunchedEffect(pendingOAuthUri) {
    val uri = pendingOAuthUri ?: return@LaunchedEffect
    sessionVm.handleSupabaseOAuthCallback(uri)
    onConsumedOAuthUri()
  }

  LaunchedEffect(activeUser?.uid, activeUser?.firebaseAuthenticated) {
    cockpitVm.bindUser(activeUser?.uid, activeUser?.firebaseAuthenticated == true)
  }

  var showSplash by rememberSaveable { mutableStateOf(true) }
  var activeTab by rememberSaveable { mutableStateOf(PrimaryTabCompartment.COCKPIT) }
  var activeOverlay by rememberSaveable { mutableStateOf(OverlayCompartment.NONE) }
  var telemetryStudioRideId by rememberSaveable { mutableStateOf<String?>(null) }
  var startMenuOpen by remember { mutableStateOf(false) }
  var dmSheetOpen by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(pendingTargetTab, pendingOpenDm) {
    pendingTargetTab?.let { tabKey ->
      runCatching { PrimaryTabCompartment.valueOf(tabKey.uppercase()) }.getOrNull()?.let { target ->
        activeOverlay = OverlayCompartment.NONE
        activeTab = target
      }
      onConsumedTargetTab()
    }
    if (pendingOpenDm) {
      dmSheetOpen = true
      onConsumedOpenDm()
    }
  }

  val popupsState = rememberCompartmentPopupsState()

  val navTabs =
    remember {
      listOf(
        TacticalNavTabItem("Cockpit", PrimaryTabCompartment.COCKPIT.icon, PrimaryTabCompartment.COCKPIT.testTag),
        TacticalNavTabItem("Radar", PrimaryTabCompartment.RADAR.icon, PrimaryTabCompartment.RADAR.testTag),
        TacticalNavTabItem("Mechanic", PrimaryTabCompartment.MECHANIC.icon, PrimaryTabCompartment.MECHANIC.testTag),
        TacticalNavTabItem("Co-Pilot", PrimaryTabCompartment.COPILOT.icon, PrimaryTabCompartment.COPILOT.testTag),
        TacticalNavTabItem("Channel", PrimaryTabCompartment.CHANNEL.icon, PrimaryTabCompartment.CHANNEL.testTag),
        TacticalNavTabItem("Porch Talk", PrimaryTabCompartment.BOARD.icon, PrimaryTabCompartment.BOARD.testTag),
      )
    }

  val tabCompartments = remember { PrimaryTabCompartment.entries }
  val activeTabIdx = tabCompartments.indexOf(activeTab).coerceAtLeast(0)

  fun onSelectTabIndex(idx: Int) {
    val nextTab = tabCompartments.getOrNull(idx) ?: PrimaryTabCompartment.COCKPIT
    sessionVm.voiceAudio.playTouchSound(
      enabled = settings.touchSoundsEnabled,
      pack = settings.touchSoundPack,
      cue = VoiceAudioManager.TouchCue.TAB_SWITCH,
      volumePct = settings.soundVolumePct,
    )
    activeOverlay = OverlayCompartment.NONE
    activeTab = nextTab
  }

  RuralERidesTheme(
    settings = settings,
  ) {
    TacticalBackgroundCanvas(texture = settings.bgTexture) {
      Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent,
      ) {
        CompartmentHostRoot(
          popupsState = popupsState,
          activeVehicle = activeVehicle,
          gpsLat = telemetry.lat,
          gpsLng = telemetry.lng,
        ) {
          Box(modifier = Modifier.fillMaxSize()) {
            if (showSplash && settings.showStartupAnimation) {
              StartupSplashSequence(
                hapticsEnabled = settings.hapticFeedback,
                voiceAudio = sessionVm.voiceAudio,
                activeSensorCount = telemetry.activeSensorsList.size,
                satellitesVisible = telemetry.satellitesVisible,
                durationSec = settings.splashDurationSec,
                onFinished = { showSplash = false },
              )
            } else {
              AuthAndOnboardingCompartmentHost(
                activity = activity,
                sessionVm = sessionVm,
                activeUser = activeUser,
                biometricLocked = biometricLocked,
                profile = profile,
                authBusy = authBusy,
                authStatusMessage = authStatusMessage,
              ) {
                val styleKey = settings.navBarStyle
                val isWinDesktop = isAnyWindowsDesktopActive(settings)
                val useClassicTaskbar = styleKey == "classic_taskbar" || isWinDesktop
                val useLeftRail = styleKey == "rail" && !useClassicTaskbar
                val useRightRail = styleKey == "right_rail" && !useClassicTaskbar
                val useTopRibbon = styleKey == "top_ribbon" && !useClassicTaskbar

                val miniPlayerBottomOffset =
                  navBarMiniPlayerBottomOffset(
                    styleKey = styleKey,
                    useLeftRail = useLeftRail,
                    useRightRail = useRightRail,
                    useTopRibbon = useTopRibbon,
                    useClassicTaskbar = useClassicTaskbar,
                  )

                val unitSystem = profile?.unitSystem ?: "IMPERIAL"
                val telemetryTicker =
                  remember(telemetry.speedKmh, telemetry.batteryPct, unitSystem) {
                    val speedVal =
                      if (unitSystem == "IMPERIAL") telemetry.speedKmh * 0.621371
                      else telemetry.speedKmh
                    val unitLbl = if (unitSystem == "IMPERIAL") "MPH" else "KM/H"
                    "${"%.1f".format(speedVal)} $unitLbl • BAT ${telemetry.batteryPct}%"
                  }

                val tabLabels = remember(navTabs) { navTabs.map { it.label } }
                val tabIcons = remember(navTabs) { navTabs.map { it.icon } }
                val tabTestTags = remember(navTabs) { navTabs.map { it.testTag } }
                val effectiveVisualStyle = sanitizeVisualStyleForSeason(settings.visualStyle)

                @Composable
                fun renderWindowsTaskbar() {
                  val startMenuComposable: @Composable () -> Unit = {
                    AuthenticWindowsStartMenuPopup(
                      expanded = startMenuOpen,
                      visualStyle = effectiveVisualStyle,
                      pilotCallsign = profile?.callsign.orEmpty(),
                      tabLabels = tabLabels,
                      tabIcons = tabIcons,
                      onDismiss = { startMenuOpen = false },
                      onSelectTab = { idx -> onSelectTabIndex(idx) },
                      onCycleTheme = {
                        val allStyles = VisualStyleMode.entries
                        val nextIdx = (allStyles.indexOf(settings.visualStyle) + 1) % allStyles.size
                        sessionVm.updateSettings { it.copy(visualStyle = allStyles[nextIdx]) }
                      },
                      onOpenSettings = { activeOverlay = OverlayCompartment.SETTINGS },
                      onOpenInfo = { activeOverlay = OverlayCompartment.INFO },
                      onOpenTelemetryStudio = {
                        telemetryStudioRideId = null
                        activeOverlay = OverlayCompartment.TELEMETRY_STUDIO
                      },
                      onOpenPrayer = { sessionVm.openPrayerOverlay(true) },
                      onSignOut = { sessionVm.signOut() },
                    )
                  }

                  when {
                    isWindows11Active(settings) ->
                      Windows11MicaTaskbar(
                        tabLabels = tabLabels,
                        tabIcons = tabIcons,
                        tabTestTags = tabTestTags,
                        activeTabIdx = activeTabIdx,
                        onSelectTab = { onSelectTabIndex(it) },
                        startMenuOpen = startMenuOpen,
                        onToggleStartMenu = { startMenuOpen = !startMenuOpen },
                        showClock = settings.winShowTaskbarClock,
                        compact = settings.winTaskbarCompact,
                        startMenuContent = startMenuComposable,
                      )
                    isWindows10Active(settings) ->
                      Windows10FluentTaskbar(
                        tabLabels = tabLabels,
                        tabIcons = tabIcons,
                        tabTestTags = tabTestTags,
                        activeTabIdx = activeTabIdx,
                        onSelectTab = { onSelectTabIndex(it) },
                        startMenuOpen = startMenuOpen,
                        onToggleStartMenu = { startMenuOpen = !startMenuOpen },
                        showClock = settings.winShowTaskbarClock,
                        compact = settings.winTaskbarCompact,
                        startMenuContent = startMenuComposable,
                      )
                    isWindows7Active(settings) ->
                      Windows7AeroTaskbar(
                        tabLabels = tabLabels,
                        tabIcons = tabIcons,
                        tabTestTags = tabTestTags,
                        activeTabIdx = activeTabIdx,
                        onSelectTab = { onSelectTabIndex(it) },
                        startMenuOpen = startMenuOpen,
                        onToggleStartMenu = { startMenuOpen = !startMenuOpen },
                        onAeroPeekHome = { onSelectTabIndex(0) },
                        showClock = settings.winShowTaskbarClock,
                        compact = settings.winTaskbarCompact,
                        startMenuContent = startMenuComposable,
                      )
                    isWindowsXpActive(settings) ->
                      WindowsXpLunaTaskbar(
                        tabLabels = tabLabels,
                        tabIcons = tabIcons,
                        tabTestTags = tabTestTags,
                        activeTabIdx = activeTabIdx,
                        onSelectTab = { onSelectTabIndex(it) },
                        startMenuOpen = startMenuOpen,
                        onToggleStartMenu = { startMenuOpen = !startMenuOpen },
                        showClock = settings.winShowTaskbarClock,
                        compact = settings.winTaskbarCompact,
                        startMenuContent = startMenuComposable,
                      )
                    else ->
                      WindowsClassic95Taskbar(
                        is2k = effectiveVisualStyle == VisualStyleMode.WINDOWS_2000,
                        tabLabels = tabLabels,
                        tabIcons = tabIcons,
                        tabTestTags = tabTestTags,
                        activeTabIdx = activeTabIdx,
                        onSelectTab = { onSelectTabIndex(it) },
                        startMenuOpen = startMenuOpen,
                        onToggleStartMenu = { startMenuOpen = !startMenuOpen },
                        showClock = settings.winShowTaskbarClock,
                        compact = settings.winTaskbarCompact,
                        startMenuContent = startMenuComposable,
                      )
                  }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                  val overlayConsumed =
                    OverlayCompartmentHost(
                      overlay = activeOverlay,
                      telemetry = telemetry,
                      telemetryStudioRideId = telemetryStudioRideId,
                      sessionVm = sessionVm,
                      cockpitVm = cockpitVm,
                      socialVm = socialVm,
                      onSelectOverlay = { activeOverlay = it },
                      onClearTelemetryRideId = { telemetryStudioRideId = null },
                      onReplayStartupIntro = { showSplash = true },
                    )

                  if (!overlayConsumed) {
                    Column(
                      modifier =
                        Modifier
                          .fillMaxSize()
                          .then(if (!useTopRibbon) Modifier.statusBarsPadding() else Modifier),
                    ) {
                      if (useTopRibbon) {
                        TacticalTopRibbonBar(
                          tabs = navTabs,
                          activeTabIdx = activeTabIdx,
                          onSelectTab = { onSelectTabIndex(it) },
                        )
                      }

                      val availableRelease = (appUpdateState as? AppUpdateCheckState.UpdateAvailable)?.release
                      if (
                        availableRelease != null &&
                        !updateBannerDismissed &&
                        availableRelease.tagName != settings.skippedUpdateTag
                      ) {
                        AppUpdateAvailableBanner(
                          release = availableRelease,
                          onOpenUpdateSheet = { sessionVm.openUpdateModal(true) },
                          onDismiss = { sessionVm.dismissUpdateBanner() },
                        )
                      }

                      Row(
                        modifier =
                          Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .then(
                              if (useTopRibbon || useLeftRail || useRightRail) {
                                Modifier.navigationBarsPadding()
                              } else {
                                Modifier
                              }
                            ),
                      ) {
                        if (useLeftRail) {
                          TacticalSideNavigationRail(
                            tabs = navTabs,
                            activeTabIdx = activeTabIdx,
                            isRightSide = false,
                            onSelectTab = { onSelectTabIndex(it) },
                          )
                        }

                        Box(
                          modifier = Modifier.weight(1f).fillMaxSize(),
                          contentAlignment = Alignment.TopCenter,
                        ) {
                          Box(modifier = Modifier.fillMaxSize().widthIn(max = 860.dp)) {
                            PrimaryTabCompartmentHost(
                              tab = activeTab,
                              sessionVm = sessionVm,
                              cockpitVm = cockpitVm,
                              radarVm = radarVm,
                              mechanicVm = mechanicVm,
                              socialVm = socialVm,
                              onSelectTab = { nextTab ->
                                activeOverlay = OverlayCompartment.NONE
                                activeTab = nextTab
                              },
                              onOpenOverlay = { nextOverlay -> activeOverlay = nextOverlay },
                              onOpenTelemetryStudio = { rideId ->
                                telemetryStudioRideId = rideId
                                activeOverlay = OverlayCompartment.TELEMETRY_STUDIO
                              },
                            )
                          }
                        }

                        if (useRightRail) {
                          TacticalSideNavigationRail(
                            tabs = navTabs,
                            activeTabIdx = activeTabIdx,
                            isRightSide = true,
                            onSelectTab = { onSelectTabIndex(it) },
                          )
                        }
                      }

                      if (!useTopRibbon && !useLeftRail && !useRightRail) {
                        if (useClassicTaskbar) {
                          renderWindowsTaskbar()
                        } else {
                          TacticalBottomNavigationHost(
                            styleKey = styleKey,
                            tabs = navTabs,
                            activeTabIdx = activeTabIdx,
                            telemetryTicker = telemetryTicker,
                            onSelectTab = { onSelectTabIndex(it) },
                          )
                        }
                      }
                    }
                  }

                  Box(
                    modifier =
                      Modifier
                        .align(Alignment.BottomCenter)
                        .then(
                          if (overlayConsumed || useTopRibbon || useLeftRail || useRightRail) {
                            Modifier.navigationBarsPadding()
                          } else {
                            Modifier
                          }
                        )
                        .padding(bottom = if (overlayConsumed) 12.dp else miniPlayerBottomOffset),
                  ) {
                    YoutubeMiniPlayerHost(
                      sessionVm = sessionVm,
                      socialVm = socialVm,
                    )
                  }
                }
              }
            }

            if (dmSheetOpen) {
              DirectMessagesSheet(
                sessionVm = sessionVm,
                socialVm = socialVm,
                telemetryLat = telemetry.lat,
                telemetryLng = telemetry.lng,
                activeVehicleSummary = activeVehicle?.let { "${it.name} (${it.voltage}V)" }.orEmpty(),
                liveTelemetrySummary = "${"%.1f".format(telemetry.speedKmh)} km/h • BAT ${telemetry.batteryPct}%",
                onDismiss = { dmSheetOpen = false },
              )
            }

            if (prayerOverlayOpen) {
              RidersPrayerModal(
                callsign = profile?.callsign.orEmpty(),
                prayerState = dailyPrayerState,
                journal = prayers,
                voiceAudio = sessionVm.voiceAudio,
                settings = settings,
                canSaveToCloud = activeUser != null,
                onReroll = { sessionVm.loadDailyPrayer(profile?.callsign ?: "Pilot") },
                onSaveJournal = { title, body, category ->
                  sessionVm.savePrayerToJournal(title, body, category)
                },
                onDeleteJournal = { entryId ->
                  sessionVm.deletePrayer(entryId)
                },
                onDismiss = { sessionVm.openPrayerOverlay(false) },
              )
            }

            if (updateModalOpen) {
              AppUpdateAvailableDialog(
                updateState = appUpdateState,
                settings = settings,
                onCheckNow = { sessionVm.checkForAppUpdates(manual = true) },
                onSkipBuild = { tag -> sessionVm.skipUpdateTag(tag) },
                onDismiss = { sessionVm.openUpdateModal(false) },
              )
            }

            LiveCallOverlayHost(
              sessionVm = sessionVm,
              socialVm = socialVm,
            )
          }
        }
      }
    }
  }
}
