package com.example.ui.compartments

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.ActiveSessionUser
import com.example.data.LiveTelemetrySnapshot
import com.example.data.PevVehicle
import com.example.data.RiderProfile
import com.example.ui.screens.AppInfoAndChangelogScreen
import com.example.ui.screens.AuthLoginScreen
import com.example.ui.screens.BiometricLockScreen
import com.example.ui.screens.BoardScreen
import com.example.ui.screens.CallsignAndLegalOnboardingScreen
import com.example.ui.screens.ChannelScreen
import com.example.ui.screens.CockpitScreen
import com.example.ui.screens.CopilotScreen
import com.example.ui.screens.CreatorArcadeHubScreen
import com.example.ui.screens.CustomVoiceStudioScreen
import com.example.ui.screens.MechanicScreen
import com.example.ui.screens.RideTelemetryStudioScreen
import com.example.ui.screens.RiderRadarScreen
import com.example.ui.screens.SettingsMatrixScreen
import com.example.viewmodel.CockpitViewModel
import com.example.viewmodel.CopilotSocialViewModel
import com.example.viewmodel.MechanicViewModel
import com.example.viewmodel.RadarViewModel
import com.example.viewmodel.SessionViewModel

/**
 * Root coordinator for app-wide compartmentalized pop-ups (3D Lab Modal and Graph Inspection Modal).
 * Isolates GPU-heavy 3D CAD and interactive telemetry modals from every underlying tab/overlay compartment.
 */
@Composable
fun CompartmentHostRoot(
  popupsState: CompartmentPopupsState,
  activeVehicle: PevVehicle?,
  gpsLat: Double = 0.0,
  gpsLng: Double = 0.0,
  content: @Composable () -> Unit,
) {
  CompositionLocalProvider(LocalCompartmentPopups provides popupsState) {
    Box(modifier = Modifier.fillMaxSize()) {
      content()

      if (popupsState.is3dLabOpen) {
        Pev3dLabModal(
          activeVehicleName = activeVehicle?.name ?: "Rural Hyper-PEV",
          activeVoltage = activeVehicle?.voltage ?: 60.0,
          gpsLat = gpsLat,
          gpsLng = gpsLng,
          onDismiss = { popupsState.close3dLab() },
        )
      }

      popupsState.activeGraphInspect?.let { inspectData ->
        GraphInspectModal(
          data = inspectData,
          onDismiss = { popupsState.closeGraphInspect() },
        )
      }
    }
  }
}

/**
 * Security & Identity Compartment: isolates login, biometric lock, and callsign/legal onboarding
 * before activating any telemetry, radar, social, or 3D compartments.
 */
@Composable
fun AuthAndOnboardingCompartmentHost(
  activity: FragmentActivity,
  sessionVm: SessionViewModel,
  activeUser: ActiveSessionUser?,
  biometricLocked: Boolean,
  profile: RiderProfile?,
  authBusy: Boolean,
  authStatusMessage: String,
  authenticatedContent: @Composable () -> Unit,
) {
  when {
    activeUser == null -> {
      val lifecycleOwner = LocalLifecycleOwner.current
      val legacyGoogleLauncher =
        rememberLauncherForActivityResult(
          contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
          sessionVm.handleLegacyGoogleSignInResult(result.resultCode, result.data, activity)
        }

      DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
          if (event == Lifecycle.Event.ON_RESUME) {
            sessionVm.consumePendingGoogleAuth()
          }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
      }

      LaunchedEffect(Unit) {
        sessionVm.consumePendingGoogleAuth()
        sessionVm.attemptSilentGoogleSignIn(activity)
      }
      AuthLoginScreen(
        busy = authBusy,
        statusMessage = authStatusMessage,
        supabaseConfigured = remember { sessionVm.supabaseService.isConfigured() },
        onSignInGoogle = {
          sessionVm.signInWithGoogle(activity) { pickerIntent ->
            legacyGoogleLauncher.launch(pickerIntent)
          }
        },
        onSignInGooglePicker = {
          sessionVm.launchLegacyGooglePicker(activity) { pickerIntent ->
            legacyGoogleLauncher.launch(pickerIntent)
          }
        },
        onSignInGoogleBrowser = { sessionVm.signInWithGoogleBrowser(activity) },
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
    profile == null || profile.acceptedLegal != true || profile.callsign.isBlank() -> {
      CallsignAndLegalOnboardingScreen(
        defaultName = activeUser.displayName,
        busy = authBusy,
        statusMessage = authStatusMessage,
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
    else -> authenticatedContent()
  }
}

/**
 * Isolated full-screen overlay compartment router.
 * Returns true when an active overlay compartment is consuming the viewport.
 */
@Composable
fun OverlayCompartmentHost(
  overlay: OverlayCompartment,
  telemetry: LiveTelemetrySnapshot,
  telemetryStudioRideId: String?,
  sessionVm: SessionViewModel,
  cockpitVm: CockpitViewModel,
  socialVm: CopilotSocialViewModel,
  onSelectOverlay: (OverlayCompartment) -> Unit,
  onClearTelemetryRideId: () -> Unit,
  onReplayStartupIntro: () -> Unit,
): Boolean {
  val popups = LocalCompartmentPopups.current
  when (overlay) {
    OverlayCompartment.SETTINGS -> {
      SettingsMatrixScreen(
        sessionVm = sessionVm,
        gpsActive = telemetry.lat != 0.0,
        telemetry = telemetry,
        onReplayStartupIntro = {
          onSelectOverlay(OverlayCompartment.NONE)
          onReplayStartupIntro()
        },
        onOpenVoiceStudio = { onSelectOverlay(OverlayCompartment.VOICE_STUDIO) },
        onOpenInfo = { onSelectOverlay(OverlayCompartment.INFO) },
        onBack = { onSelectOverlay(OverlayCompartment.NONE) },
      )
      return true
    }
    OverlayCompartment.INFO -> {
      AppInfoAndChangelogScreen(
        sessionVm = sessionVm,
        cockpitVm = cockpitVm,
        onBack = { onSelectOverlay(OverlayCompartment.NONE) },
      )
      return true
    }
    OverlayCompartment.TELEMETRY_STUDIO -> {
      RideTelemetryStudioScreen(
        sessionVm = sessionVm,
        cockpitVm = cockpitVm,
        initialRideId = telemetryStudioRideId,
        onBack = {
          onClearTelemetryRideId()
          onSelectOverlay(OverlayCompartment.NONE)
        },
      )
      return true
    }
    OverlayCompartment.CREATOR_HUB -> {
      CreatorArcadeHubScreen(
        sessionVm = sessionVm,
        cockpitVm = cockpitVm,
        socialVm = socialVm,
        onBack = { onSelectOverlay(OverlayCompartment.NONE) },
      )
      return true
    }
    OverlayCompartment.VOICE_STUDIO -> {
      CustomVoiceStudioScreen(
        sessionVm = sessionVm,
        onBack = { onSelectOverlay(OverlayCompartment.NONE) },
      )
      return true
    }
    OverlayCompartment.NONE -> return false
  }
}

/**
 * Isolated primary tab compartment router.
 * Each compartment operates inside its own keyed boundary with clean dependency injection.
 */
@Composable
fun PrimaryTabCompartmentHost(
  tab: PrimaryTabCompartment,
  sessionVm: SessionViewModel,
  cockpitVm: CockpitViewModel,
  radarVm: RadarViewModel,
  mechanicVm: MechanicViewModel,
  socialVm: CopilotSocialViewModel,
  onSelectTab: (PrimaryTabCompartment) -> Unit,
  onOpenOverlay: (OverlayCompartment) -> Unit,
  onOpenTelemetryStudio: (String?) -> Unit,
) {
  val popups = LocalCompartmentPopups.current
  key(tab) {
    when (tab) {
      PrimaryTabCompartment.COCKPIT ->
        CockpitScreen(
          sessionVm = sessionVm,
          cockpitVm = cockpitVm,
          radarVm = radarVm,
          socialVm = socialVm,
          onOpenRadar = { onSelectTab(PrimaryTabCompartment.RADAR) },
          onOpenSettings = { onOpenOverlay(OverlayCompartment.SETTINGS) },
          onOpenInfo = { onOpenOverlay(OverlayCompartment.INFO) },
          onOpenTelemetryStudio = onOpenTelemetryStudio,
          onOpenCreatorHub = { onOpenOverlay(OverlayCompartment.CREATOR_HUB) },
          onOpen3dLab = { popups?.open3dLab() },
        )
      PrimaryTabCompartment.RADAR ->
        RiderRadarScreen(
          sessionVm = sessionVm,
          cockpitVm = cockpitVm,
          radarVm = radarVm,
          socialVm = socialVm,
        )
      PrimaryTabCompartment.MECHANIC ->
        MechanicScreen(
          sessionVm = sessionVm,
          cockpitVm = cockpitVm,
          radarVm = radarVm,
          mechanicVm = mechanicVm,
          socialVm = socialVm,
        )
      PrimaryTabCompartment.COPILOT ->
        CopilotScreen(
          sessionVm = sessionVm,
          cockpitVm = cockpitVm,
          socialVm = socialVm,
          onOpenVoiceStudio = { onOpenOverlay(OverlayCompartment.VOICE_STUDIO) },
        )
      PrimaryTabCompartment.CHANNEL ->
        ChannelScreen(
          sessionVm = sessionVm,
          socialVm = socialVm,
        )
      PrimaryTabCompartment.BOARD ->
        BoardScreen(
          sessionVm = sessionVm,
          cockpitVm = cockpitVm,
          socialVm = socialVm,
          onSelectTab = onSelectTab,
          onOpenOverlay = onOpenOverlay,
          onOpen3dLab = { popups?.open3dLab() },
        )
    }
  }
}
