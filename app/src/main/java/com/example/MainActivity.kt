package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
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
    if (intent.hasExtra("FIREBASE_APPCHECK_DEBUG_TOKEN")) {
      configureAppCheck(this, intent)
    }
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
  val activeFriendRequestAlert by sessionVm.activeFriendRequestAlert.collectAsStateWithLifecycle()

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

  val supabaseSession by sessionVm.supabaseSession.collectAsStateWithLifecycle()
  val openDmSheetRequested by socialVm.openDmSheetRequested.collectAsStateWithLifecycle()

  LaunchedEffect(activeUser?.uid, activeUser?.firebaseAuthenticated, supabaseSession?.accessToken, settings.copilotPersona) {
    val uid = activeUser?.uid
    val fbAuth = activeUser?.firebaseAuthenticated == true
    val sbToken = supabaseSession?.accessToken.orEmpty()
    cockpitVm.bindUser(uid, fbAuth)
    socialVm.bindUser(
      uid = uid,
      firebaseAuthenticated = fbAuth,
      defaultPersona = settings.copilotPersona,
      supabaseAccessToken = sbToken,
    )
    radarVm.bindCloudSession(
      uid = uid,
      firebaseAuthenticated = fbAuth,
      supabaseAccessToken = sbToken,
    )
  }

  var showSplash by rememberSaveable { mutableStateOf(true) }
  var activeTab by rememberSaveable { mutableStateOf(PrimaryTabCompartment.COCKPIT) }
  var activeOverlay by rememberSaveable { mutableStateOf(OverlayCompartment.NONE) }
  var telemetryStudioRideId by rememberSaveable { mutableStateOf<String?>(null) }
  var startMenuOpen by remember { mutableStateOf(false) }
  var dmSheetOpen by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(openDmSheetRequested) {
    if (openDmSheetRequested != null) {
      dmSheetOpen = true
      socialVm.clearOpenDmSheetRequest()
    }
  }

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

  val hasRootBackTarget =
    !showSplash &&
      (startMenuOpen ||
        updateModalOpen ||
        prayerOverlayOpen ||
        dmSheetOpen ||
        activeFriendRequestAlert != null ||
        activeOverlay != OverlayCompartment.NONE ||
        activeTab != PrimaryTabCompartment.COCKPIT)

  BackHandler(enabled = hasRootBackTarget) {
    when {
      startMenuOpen -> startMenuOpen = false
      activeFriendRequestAlert != null -> sessionVm.dismissFriendRequestAlert(markRead = true)
      dmSheetOpen -> dmSheetOpen = false
      updateModalOpen -> sessionVm.openUpdateModal(false)
      prayerOverlayOpen -> sessionVm.openPrayerOverlay(false)
      activeOverlay != OverlayCompartment.NONE -> activeOverlay = OverlayCompartment.NONE
      activeTab != PrimaryTabCompartment.COCKPIT -> activeTab = PrimaryTabCompartment.COCKPIT
    }
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

                val telemetryTicker =
                  remember(telemetry.speedKmh, telemetry.batteryPct, settings.distanceUnit) {
                    val speedVal = com.example.ui.components.formatSpeedValue(telemetry.speedKmh, settings.distanceUnit)
                    val unitLbl = com.example.ui.components.speedUnitLabel(settings.distanceUnit)
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

                  val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

                  if (!overlayConsumed) {
                    Column(
                      modifier =
                        Modifier
                          .fillMaxSize()
                          .then(if (!useTopRibbon) Modifier.statusBarsPadding() else Modifier)
                          .imePadding(),
                    ) {
                      if (useTopRibbon && !isImeVisible) {
                        TacticalTopRibbonBar(
                          tabs = navTabs,
                          activeTabIdx = activeTabIdx,
                          onSelectTab = { onSelectTabIndex(it) },
                        )
                      }

                      val availableRelease = (appUpdateState as? AppUpdateCheckState.UpdateAvailable)?.release
                      if (
                        !isImeVisible &&
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
                              if (!isImeVisible && (useTopRibbon || useLeftRail || useRightRail)) {
                                Modifier.navigationBarsPadding()
                              } else {
                                Modifier
                              }
                            ),
                      ) {
                        if (useLeftRail && !isImeVisible) {
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

                        if (useRightRail && !isImeVisible) {
                          TacticalSideNavigationRail(
                            tabs = navTabs,
                            activeTabIdx = activeTabIdx,
                            isRightSide = true,
                            onSelectTab = { onSelectTabIndex(it) },
                          )
                        }
                      }

                      if (!isImeVisible && !useTopRibbon && !useLeftRail && !useRightRail) {
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

                  val youtubeExpanded by socialVm.playerExpanded.collectAsStateWithLifecycle()
                  if (!isImeVisible || youtubeExpanded) {
                    Box(
                      modifier =
                        Modifier
                          .align(Alignment.BottomCenter)
                          .then(
                            when {
                              youtubeExpanded -> Modifier.fillMaxSize()
                              overlayConsumed || useTopRibbon || useLeftRail || useRightRail ->
                                Modifier
                                  .navigationBarsPadding()
                                  .padding(bottom = 12.dp)
                                  .widthIn(max = 760.dp)
                              else ->
                                Modifier
                                  .padding(bottom = miniPlayerBottomOffset)
                                  .widthIn(max = 760.dp)
                            }
                          ),
                    ) {
                      YoutubeMiniPlayerHost(
                        sessionVm = sessionVm,
                        socialVm = socialVm,
                      )
                    }
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
                liveTelemetrySummary =
                  "${"%.1f".format(com.example.ui.components.formatSpeedValue(telemetry.speedKmh, settings.distanceUnit))} ${com.example.ui.components.speedUnitLabel(settings.distanceUnit)} • BAT ${telemetry.batteryPct}%",
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

            activeFriendRequestAlert?.let { fr ->
              Surface(
                modifier =
                  Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                shadowElevation = 10.dp,
              ) {
                Column(
                  modifier = Modifier.padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                      Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = "Friend Request",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                      )
                      Spacer(Modifier.width(8.dp))
                      Column {
                        Text(
                          fr.title.ifBlank { "Friend Request" },
                          style = MaterialTheme.typography.titleSmall,
                          fontWeight = FontWeight.Black,
                          color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                          fr.body.ifBlank { "Wants to connect and ride with you." },
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                    }
                    IconButton(onClick = { sessionVm.dismissFriendRequestAlert(markRead = true) }) {
                      Icon(Icons.Default.Close, contentDescription = "Dismiss Friend Request")
                    }
                  }
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    val senderUid = fr.senderId.ifBlank { fr.userId }
                    val senderCs = fr.senderCallsign.ifBlank { "PILOT" }
                    val myUid = activeUser?.uid.orEmpty()
                    val myCs = profile?.callsign.orEmpty()
                    Button(
                      onClick = {
                        if (myUid.isNotBlank() && senderUid.isNotBlank() && senderUid != myUid) {
                          socialVm.toggleFollowPilot(myUid, senderUid, myCs, senderCs)
                        }
                        sessionVm.dismissFriendRequestAlert(markRead = true)
                      },
                      modifier = Modifier.weight(1f),
                    ) {
                      Text("Follow Back", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                      onClick = {
                        sessionVm.dismissFriendRequestAlert(markRead = true)
                        socialVm.requestOpenDmSheet(senderUid)
                      },
                      modifier = Modifier.weight(1f),
                    ) {
                      Text("Message")
                    }
                  }
                }
              }
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
