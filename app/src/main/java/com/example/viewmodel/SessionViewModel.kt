package com.example.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.DailyPrayerResult
import com.example.ai.GeminiAiRepository
import com.example.core.DiagnosticCheckResult
import com.example.core.LiveCloudServices
import com.example.core.ReminderScheduler
import com.example.data.ActiveSessionUser
import com.example.data.AuthProviderType
import com.example.data.AuthRepository
import com.example.data.CommunityRepository
import com.example.data.EmergencyBroadcast
import com.example.data.LocalRiderVault
import com.example.data.MasterSettings
import com.example.data.NewsArticleItem
import com.example.data.PrayerJournalEntry
import com.example.data.PublicPilotProfile
import com.example.data.RiderMatchEngine
import com.example.data.RiderProfile
import com.example.data.RiderRepository
import com.example.data.SettingsStore
import com.example.data.SignInResult
import com.example.data.SupabaseAuthResult
import com.example.data.SupabaseAuthService
import com.example.data.UiState
import com.example.data.UserNotification
import com.example.data.WeatherGeoLocation
import com.example.data.WeatherMatrixData
import com.example.data.authStateFlow
import com.example.hardware.VoiceAudioManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "SessionViewModel"

class SessionViewModel(application: Application) : AndroidViewModel(application) {

  private val ctx = application.applicationContext
  private val auth = FirebaseAuth.getInstance()
  private val authRepo = AuthRepository(auth)
  val supabaseService = SupabaseAuthService(ctx)
  private val localVault = LocalRiderVault(ctx)
  val riderRepo = RiderRepository(ctx)
  val communityRepo = CommunityRepository(ctx)
  val settingsStore = SettingsStore(ctx)
  val aiRepo = GeminiAiRepository()
  val voiceAudio = VoiceAudioManager(ctx)

  val settings: StateFlow<MasterSettings> =
    settingsStore.settingsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, MasterSettings())

  private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
  val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

  val supabaseSession = supabaseService.sessionFlow

  val activeUser: StateFlow<ActiveSessionUser?> =
    combine(_currentUser, supabaseSession) { fb, sb ->
        when {
          fb != null ->
            ActiveSessionUser(
              uid = fb.uid,
              rawId = fb.uid,
              email = fb.email.orEmpty(),
              displayName = fb.displayName.orEmpty(),
              provider = AuthProviderType.GOOGLE,
              firebaseAuthenticated = true,
            )
          sb != null ->
            ActiveSessionUser(
              uid = "sb_${sb.user.id}",
              rawId = sb.user.id,
              email = sb.user.email,
              displayName = sb.user.callsign,
              provider = AuthProviderType.SUPABASE,
              firebaseAuthenticated = false,
            )
          else -> null
        }
      }
      .stateIn(viewModelScope, SharingStarted.Eagerly, null)

  private val _profile = MutableStateFlow<RiderProfile?>(null)
  val profile: StateFlow<RiderProfile?> = _profile.asStateFlow()

  private val _prayers = MutableStateFlow<List<PrayerJournalEntry>>(emptyList())
  val prayers: StateFlow<List<PrayerJournalEntry>> = _prayers.asStateFlow()

  private val _notifications = MutableStateFlow<List<UserNotification>>(emptyList())
  val notifications: StateFlow<List<UserNotification>> = _notifications.asStateFlow()

  private val _broadcasts = MutableStateFlow<List<EmergencyBroadcast>>(emptyList())
  val broadcasts: StateFlow<List<EmergencyBroadcast>> = _broadcasts.asStateFlow()

  private val _publicPilots = MutableStateFlow<List<PublicPilotProfile>>(emptyList())
  val publicPilots: StateFlow<List<PublicPilotProfile>> = _publicPilots.asStateFlow()

  private val _dailyPrayerState = MutableStateFlow<UiState<DailyPrayerResult>>(UiState.Loading)
  val dailyPrayerState: StateFlow<UiState<DailyPrayerResult>> = _dailyPrayerState.asStateFlow()

  private val _weatherState = MutableStateFlow<UiState<WeatherMatrixData>>(UiState.Loading)
  val weatherState: StateFlow<UiState<WeatherMatrixData>> = _weatherState.asStateFlow()

  private val _weatherLocationResults = MutableStateFlow<List<WeatherGeoLocation>>(emptyList())
  val weatherLocationResults: StateFlow<List<WeatherGeoLocation>> = _weatherLocationResults.asStateFlow()

  private val _weatherLocSearching = MutableStateFlow(false)
  val weatherLocSearching: StateFlow<Boolean> = _weatherLocSearching.asStateFlow()

  private val _resolvedGpsCity = MutableStateFlow("")
  val resolvedGpsCity: StateFlow<String> = _resolvedGpsCity.asStateFlow()

  private val _newsState = MutableStateFlow<UiState<List<NewsArticleItem>>>(UiState.Loading)
  val newsState: StateFlow<UiState<List<NewsArticleItem>>> = _newsState.asStateFlow()

  private val _newsCategory = MutableStateFlow("ALL PEV")
  val newsCategory: StateFlow<String> = _newsCategory.asStateFlow()

  private val _authStatusMessage = MutableStateFlow("")
  val authStatusMessage: StateFlow<String> = _authStatusMessage.asStateFlow()

  private val _authBusy = MutableStateFlow(false)
  val authBusy: StateFlow<Boolean> = _authBusy.asStateFlow()

  private val _prayerOverlayOpen = MutableStateFlow(false)
  val prayerOverlayOpen: StateFlow<Boolean> = _prayerOverlayOpen.asStateFlow()

  private val _biometricLocked = MutableStateFlow(false)
  val biometricLocked: StateFlow<Boolean> = _biometricLocked.asStateFlow()

  private val _diagnosticResults = MutableStateFlow<List<DiagnosticCheckResult>>(emptyList())
  val diagnosticResults: StateFlow<List<DiagnosticCheckResult>> = _diagnosticResults.asStateFlow()

  private val _diagnosticsBusy = MutableStateFlow(false)
  val diagnosticsBusy: StateFlow<Boolean> = _diagnosticsBusy.asStateFlow()

  private val _cloudBackupBusy = MutableStateFlow(false)
  val cloudBackupBusy: StateFlow<Boolean> = _cloudBackupBusy.asStateFlow()

  private val _cloudBackupMessage = MutableStateFlow("")
  val cloudBackupMessage: StateFlow<String> = _cloudBackupMessage.asStateFlow()

  private var userListenersJob: Job? = null
  private var publicListenersJob: Job? = null
  private var lastWeatherCoords: Pair<Double, Double>? = null
  private var lastRoadLookupMs = 0L

  init {
    viewModelScope.launch {
      auth.authStateFlow().collect { fbUser ->
        _currentUser.value = fbUser
      }
    }

    viewModelScope.launch {
      combine(activeUser, supabaseSession) { active, sb ->
        active to sb?.accessToken.orEmpty()
      }.collect { (active, sbToken) ->
        userListenersJob?.cancel()
        publicListenersJob?.cancel()

        when {
          active == null -> {
            _profile.value = null
            _prayers.value = emptyList()
            _notifications.value = emptyList()
            _broadcasts.value = emptyList()
            _publicPilots.value = emptyList()
          }
          active.firebaseAuthenticated -> {
            startFirebaseUserCollectors(active.uid)
            startFirebasePublicCollectors()
          }
          else -> {
            _notifications.value = emptyList()
            _broadcasts.value = emptyList()
            _profile.value = localVault.loadProfile(active.uid)
            _prayers.value = localVault.loadPrayers(active.uid)
            if (sbToken.isNotBlank()) {
              startSupabasePublicCollectors(sbToken)
            } else {
              _publicPilots.value = emptyList()
            }
          }
        }
      }
    }

    viewModelScope.launch {
      if (auth.currentUser == null) {
        authRepo.trySilentGoogleSignIn(ctx)
      }
    }

    viewModelScope.launch {
      settings.collect { s ->
        runCatching { ReminderScheduler.syncSchedule(ctx, s) }
      }
    }

    refreshNews()
    loadDailyPrayer("Rural Pilot")
  }

  private fun startFirebaseUserCollectors(uid: String) {
    userListenersJob =
      viewModelScope.launch {
        launch {
          riderRepo
            .observeProfile(uid)
            .catch { e ->
              if (e is CancellationException) throw e
              Log.w(TAG, "Profile listener error: ${e.message}")
            }
            .collect { _profile.value = it }
        }
        launch {
          riderRepo
            .observePrayers(uid)
            .catch { e ->
              if (e is CancellationException) throw e
              Log.w(TAG, "Prayers listener error: ${e.message}")
            }
            .collect { _prayers.value = it }
        }
        launch {
          riderRepo
            .observeNotifications(uid)
            .catch { e ->
              if (e is CancellationException) throw e
              Log.w(TAG, "Notifications listener error: ${e.message}")
            }
            .collect { _notifications.value = it }
        }
      }
  }

  private fun startFirebasePublicCollectors() {
    publicListenersJob =
      viewModelScope.launch {
        launch {
          communityRepo
            .observeBroadcasts()
            .catch { e ->
              if (e is CancellationException) throw e
              Log.w(TAG, "Broadcasts listener error: ${e.message}")
            }
            .collect { _broadcasts.value = it }
        }
        launch {
          communityRepo
            .observePublicPilots()
            .catch { e ->
              if (e is CancellationException) throw e
              Log.w(TAG, "Public pilots listener error: ${e.message}")
            }
            .collect { _publicPilots.value = it }
        }
      }
  }

  private fun startSupabasePublicCollectors(sbToken: String) {
    publicListenersJob =
      viewModelScope.launch {
        while (isActive) {
          LiveCloudServices.fetchSupabasePublicPilots(sbToken, selfUid = activeUser.value?.uid.orEmpty())
            .onSuccess { _publicPilots.value = it }
            .onFailure { Log.w(TAG, "Supabase pilots sync failed: ${it.message}") }
          delay(10_000L)
        }
      }
  }

  fun setBiometricLockState(locked: Boolean) {
    _biometricLocked.value = locked
  }

  fun clearAuthMessage() {
    _authStatusMessage.value = ""
  }

  fun signInWithGoogle(activity: Activity) {
    viewModelScope.launch {
      _authBusy.value = true
      _authStatusMessage.value = "Requesting Google Identity..."
      when (val res = authRepo.signInWithGoogleInteractive(activity)) {
        is SignInResult.Success -> _authStatusMessage.value = ""
        is SignInResult.Cancelled ->
          _authStatusMessage.value =
            "Google Sign-In cancelled. Ensure SHA-1/SHA-256 is registered or use Supabase Login."
        is SignInResult.NoSavedCredential ->
          _authStatusMessage.value = "No Google account found on device. Add one or sign in with Supabase."
        is SignInResult.Error -> _authStatusMessage.value = res.message
      }
      _authBusy.value = false
    }
  }

  fun signInWithSupabase(email: String, password: String) {
    viewModelScope.launch {
      _authBusy.value = true
      _authStatusMessage.value = "Authenticating with Supabase GoTrue..."
      when (val res = supabaseService.signInWithSupabasePassword(email, password)) {
        is SupabaseAuthResult.Success -> _authStatusMessage.value = ""
        is SupabaseAuthResult.EmailConfirmationRequired -> _authStatusMessage.value = res.message
        is SupabaseAuthResult.Error -> _authStatusMessage.value = res.message
      }
      _authBusy.value = false
    }
  }

  fun signUpWithSupabase(email: String, password: String, callsign: String) {
    viewModelScope.launch {
      _authBusy.value = true
      _authStatusMessage.value = "Registering pilot account on Supabase GoTrue..."
      when (val res = supabaseService.signUpWithSupabase(email, password, callsign)) {
        is SupabaseAuthResult.Success -> _authStatusMessage.value = ""
        is SupabaseAuthResult.EmailConfirmationRequired -> _authStatusMessage.value = res.message
        is SupabaseAuthResult.Error -> _authStatusMessage.value = res.message
      }
      _authBusy.value = false
    }
  }

  fun sendSupabasePasswordReset(email: String) {
    viewModelScope.launch {
      _authBusy.value = true
      supabaseService
        .sendSupabasePasswordReset(email)
        .onSuccess { _authStatusMessage.value = it }
        .onFailure { _authStatusMessage.value = it.message ?: "Password reset failed" }
      _authBusy.value = false
    }
  }

  fun resendSupabaseConfirmation(email: String) {
    viewModelScope.launch {
      _authBusy.value = true
      supabaseService
        .resendConfirmationEmail(email)
        .onSuccess { _authStatusMessage.value = it }
        .onFailure { _authStatusMessage.value = it.message ?: "Could not resend confirmation email" }
      _authBusy.value = false
    }
  }

  fun refreshSupabaseSessionNow() {
    viewModelScope.launch {
      _authBusy.value = true
      val next = supabaseService.refreshSupabaseSession()
      _authStatusMessage.value =
        if (next != null) "Supabase JWT refreshed (exp ${next.expiresAtEpochSec})."
        else "Supabase session refresh skipped or unavailable."
      _authBusy.value = false
    }
  }

  fun signOut() {
    viewModelScope.launch {
      userListenersJob?.cancel()
      publicListenersJob?.cancel()
      authRepo.signOut(ctx)
      supabaseService.signOutSupabase()
    }
  }

  fun saveCallsignAndLegal(
    callsign: String,
    displayName: String,
    unitSystem: String,
    emergencyName: String,
    sosNumber: String,
    medicalInfo: String,
    acceptedLegal: Boolean,
  ) {
    val active = activeUser.value ?: return
    val existingLockedCallsign = _profile.value?.callsign?.trim().orEmpty()
    val effectiveCallsign =
      if (existingLockedCallsign.isNotBlank()) existingLockedCallsign
      else callsign.trim().uppercase().take(24).ifBlank { "PILOT_${active.uid.take(5).uppercase()}" }
    viewModelScope.launch {
      _authBusy.value = true
      if (active.firebaseAuthenticated) {
        val res =
          riderRepo.upsertProfileAndPublicIdentity(
            userId = active.uid,
            displayName = displayName.ifBlank { active.displayName.ifBlank { effectiveCallsign } },
            callsign = effectiveCallsign,
            email = active.email,
            unitSystem = unitSystem,
            emergencyContactName = emergencyName,
            sosContactNumber = sosNumber,
            medicalProfile = medicalInfo,
            acceptedLegal = acceptedLegal,
            activeVehicleId = _profile.value?.activeVehicleId.orEmpty(),
          )
        if (res.isFailure) {
          _authStatusMessage.value = res.exceptionOrNull()?.message ?: "Profile save failed."
        }
      } else {
        val localProf =
          RiderProfile(
            id = active.uid,
            userId = active.uid,
            displayName = displayName.ifBlank { active.displayName.ifBlank { effectiveCallsign } },
            callsign = effectiveCallsign,
            email = active.email,
            activeVehicleId = _profile.value?.activeVehicleId.orEmpty(),
            unitSystem = if (unitSystem == "METRIC") "METRIC" else "IMPERIAL",
            emergencyContactName = emergencyName.trim(),
            sosContactNumber = sosNumber.trim(),
            medicalProfile = medicalInfo.trim(),
            acceptedLegal = acceptedLegal,
          )
        localVault.saveProfile(localProf)
        _profile.value = localProf
        if (active.provider == AuthProviderType.SUPABASE) {
          if (existingLockedCallsign.isBlank()) {
            supabaseService.updateSupabaseCallsign(effectiveCallsign)
          }
          val sbToken = supabaseSession.value?.accessToken.orEmpty()
          if (sbToken.isNotBlank()) {
            val curPilot = _publicPilots.value.firstOrNull { it.userId == active.uid }
            val s = settings.value
            val summary =
              RiderMatchEngine.encodePilotMatchSummary(
                vehicleName = curPilot?.vehicleSummary?.substringBefore(" (")?.ifBlank { "PEV Rig" } ?: "PEV Rig",
                batteryWh = 1500L,
                pace = s.matchPaceTier,
                terrain = s.matchTerrainPref,
                usableRangeMi = s.matchMinRangeMi,
                vibe = s.matchScheduleVibe,
                zipOrTown = s.matchHomeZipOrTown,
              )
            LiveCloudServices.upsertSupabasePilotProfile(
              sbToken,
              (curPilot ?: PublicPilotProfile(id = active.uid, userId = active.uid, callsign = effectiveCallsign))
                .copy(
                  userId = active.uid,
                  callsign = effectiveCallsign,
                  vehicleSummary = curPilot?.vehicleSummary?.ifBlank { summary } ?: summary,
                  visibility = "PUBLIC",
                ),
            )
          }
        }
      }
      _authBusy.value = false
    }
  }

  fun updateRiderProfileDetails(
    riderWeightLbs: Double,
    avatarBase64: String,
    onDone: (String) -> Unit = {},
  ) {
    val active = activeUser.value ?: return
    val cleanWeight = riderWeightLbs.coerceIn(0.0, 600.0)
    val cleanAvatar = avatarBase64.take(62_000)
    viewModelScope.launch {
      updateSettings {
        if (cleanWeight > 0.0) it.copy(riderWeightLbs = cleanWeight.toInt()) else it
      }
      val cur = _profile.value
      val nextLocal =
        (cur
            ?: RiderProfile(
              id = active.uid,
              userId = active.uid,
              displayName = active.displayName.ifBlank { "Pilot" },
              callsign = active.displayName.uppercase().take(24).ifBlank { "PILOT" },
              email = active.email,
              acceptedLegal = true,
            ))
          .copy(riderWeightLbs = cleanWeight, avatarBase64 = cleanAvatar)
      localVault.saveProfile(nextLocal)
      _profile.value = nextLocal

      if (active.firebaseAuthenticated) {
        riderRepo
          .updateRiderWeightAndAvatar(active.uid, cleanWeight, cleanAvatar)
          .onSuccess { onDone("Pilot weight & Base64 profile photo synced to cloud.") }
          .onFailure { onDone(it.message ?: "Saved locally; cloud sync pending.") }
      } else {
        val sbToken = supabaseSession.value?.accessToken.orEmpty()
        if (sbToken.isNotBlank()) {
          val curPilot = _publicPilots.value.firstOrNull { it.userId == active.uid }
          LiveCloudServices.upsertSupabasePilotProfile(
            sbToken,
            (curPilot ?: PublicPilotProfile(id = active.uid, userId = active.uid, callsign = nextLocal.callsign))
              .copy(
                userId = active.uid,
                callsign = nextLocal.callsign,
                avatarBase64 = cleanAvatar,
                visibility = "PUBLIC",
              ),
          )
            .onSuccess { onDone("Pilot weight & profile photo synced to Supabase cloud.") }
            .onFailure { onDone("Saved to local vault (${it.message ?: "cloud sync pending"}).")}
        } else {
          onDone("Pilot weight & Base64 profile photo saved to vault.")
        }
      }
    }
  }

  fun openPrayerOverlay(open: Boolean) {
    _prayerOverlayOpen.value = open
    if (open && _dailyPrayerState.value !is UiState.Success) {
      loadDailyPrayer(_profile.value?.callsign ?: "Pilot")
    }
  }

  fun loadDailyPrayer(callsign: String) {
    viewModelScope.launch {
      _dailyPrayerState.value = UiState.Loading
      val res = aiRepo.fetchDailyPrayer(callsign, modelId = settings.value.aiModelId)
      _dailyPrayerState.value =
        res.fold(
          onSuccess = { UiState.Success(it) },
          onFailure = { UiState.Error(it.message ?: "Prayer engine offline") },
        )
    }
  }

  fun savePrayerToJournal(title: String, body: String, category: String) {
    val active = activeUser.value ?: return
    viewModelScope.launch {
      if (active.firebaseAuthenticated) {
        riderRepo.savePrayerEntry(active.uid, title, body, category)
      } else {
        val entry =
          PrayerJournalEntry(
            id = "pr_${UUID.randomUUID().toString().take(8)}",
            userId = active.uid,
            title = title.ifBlank { "Trail Prayer" },
            body = body,
            category = category.ifBlank { "Trail Safety" },
          )
        val updated = listOf(entry) + _prayers.value
        localVault.savePrayers(active.uid, updated)
        _prayers.value = updated
      }
    }
  }

  fun deletePrayer(id: String) {
    val active = activeUser.value ?: return
    viewModelScope.launch {
      if (active.firebaseAuthenticated) {
        riderRepo.deletePrayerEntry(active.uid, id)
      } else {
        val updated = _prayers.value.filterNot { it.id == id }
        localVault.savePrayers(active.uid, updated)
        _prayers.value = updated
      }
    }
  }

  fun markNotificationRead(notifId: String) {
    val active = activeUser.value ?: return
    if (!active.firebaseAuthenticated) return
    viewModelScope.launch { riderRepo.markNotificationRead(active.uid, notifId) }
  }

  fun searchWeatherLocations(query: String) {
    if (query.trim().length < 2) {
      _weatherLocationResults.value = emptyList()
      return
    }
    viewModelScope.launch {
      _weatherLocSearching.value = true
      LiveCloudServices.searchWeatherLocations(query)
        .onSuccess { _weatherLocationResults.value = it }
        .onFailure { _weatherLocationResults.value = emptyList() }
      _weatherLocSearching.value = false
    }
  }

  fun clearWeatherLocationResults() {
    _weatherLocationResults.value = emptyList()
  }

  fun selectCustomWeatherLocation(loc: WeatherGeoLocation) {
    _weatherLocationResults.value = emptyList()
    val label = loc.displayLabel()
    updateSettings {
      it.copy(
        useCustomWeatherLoc = true,
        customWeatherCity = label,
        customWeatherLat = loc.lat,
        customWeatherLng = loc.lng,
      )
    }
    lastWeatherCoords = loc.lat to loc.lng
    viewModelScope.launch {
      _weatherState.value = UiState.Loading
      val res = LiveCloudServices.fetchWeatherMatrix(loc.lat, loc.lng, explicitLabel = label, isGpsSource = false)
      _weatherState.value =
        res.fold(
          onSuccess = { UiState.Success(it) },
          onFailure = { UiState.Error(it.message ?: "Open-Meteo unreachable") },
        )
    }
  }

  fun useGpsWeatherLocation(lat: Double, lng: Double) {
    updateSettings { it.copy(useCustomWeatherLoc = false) }
    lastWeatherCoords = null
    refreshWeatherIfNeeded(lat, lng, force = true)
  }

  fun refreshWeatherIfNeeded(lat: Double, lng: Double, force: Boolean = false) {
    val curSettings = settings.value
    if (curSettings.useCustomWeatherLoc && curSettings.customWeatherLat != 0.0) {
      val prev = lastWeatherCoords
      if (!force && prev != null &&
        kotlin.math.abs(prev.first - curSettings.customWeatherLat) < 0.01 &&
        kotlin.math.abs(prev.second - curSettings.customWeatherLng) < 0.01
      ) {
        return
      }
      lastWeatherCoords = curSettings.customWeatherLat to curSettings.customWeatherLng
      viewModelScope.launch {
        _weatherState.value = UiState.Loading
        val res =
          LiveCloudServices.fetchWeatherMatrix(
            lat = curSettings.customWeatherLat,
            lng = curSettings.customWeatherLng,
            explicitLabel = curSettings.customWeatherCity,
            isGpsSource = false,
          )
        _weatherState.value =
          res.fold(
            onSuccess = { UiState.Success(it) },
            onFailure = { UiState.Error(it.message ?: "Open-Meteo unreachable") },
          )
      }
      return
    }

    if (lat == 0.0 && lng == 0.0) {
      if (force) {
        _weatherState.value = UiState.Error("Awaiting GPS fix — search a City/ZIP above to load weather immediately.")
      }
      return
    }
    val prev = lastWeatherCoords
    if (!force && prev != null && kotlin.math.abs(prev.first - lat) < 0.02 && kotlin.math.abs(prev.second - lng) < 0.02) {
      return
    }
    lastWeatherCoords = lat to lng
    viewModelScope.launch {
      _weatherState.value = UiState.Loading
      val res = LiveCloudServices.fetchWeatherMatrix(lat, lng, isGpsSource = true)
      _weatherState.value =
        res.fold(
          onSuccess = { data ->
            if (data.locationLabel.isNotBlank() && !data.locationLabel.startsWith("Awaiting")) {
              _resolvedGpsCity.value = data.locationLabel
            }
            UiState.Success(data)
          },
          onFailure = { UiState.Error(it.message ?: "Open-Meteo unreachable") },
        )
    }
  }

  fun pollRoadSpeedLimit(lat: Double, lng: Double, onUpdate: (String, Int?, Boolean) -> Unit) {
    if (lat == 0.0 && lng == 0.0) return
    val now = System.currentTimeMillis()
    if (now - lastRoadLookupMs < 14_000L) return
    lastRoadLookupMs = now
    viewModelScope.launch {
      LiveCloudServices.fetchLiveRoadAndSpeedLimit(lat, lng).onSuccess { info ->
        onUpdate(info.roadName, info.speedLimitMph, info.inSchoolZone)
      }
    }
  }

  fun setNewsCategory(category: String) {
    _newsCategory.value = category
    refreshNews(category = category)
  }

  fun setNewsLocationScope(scope: String, customCity: String = settings.value.customNewsCity) {
    updateSettings { it.copy(newsLocationScope = scope, customNewsCity = customCity) }
    val resolvedLoc =
      when (scope) {
        "GPS" -> _resolvedGpsCity.value
        "CUSTOM" -> customCity
        else -> ""
      }
    refreshNews(category = _newsCategory.value, explicitLocation = resolvedLoc)
  }

  fun refreshNews(
    topic: String = "",
    category: String = _newsCategory.value,
    explicitLocation: String? = null,
  ) {
    val curSettings = settings.value
    val loc =
      explicitLocation
        ?: when (curSettings.newsLocationScope) {
          "GPS" -> _resolvedGpsCity.value
          "CUSTOM" -> curSettings.customNewsCity
          else -> ""
        }
    viewModelScope.launch {
      _newsState.value = UiState.Loading
      val res = LiveCloudServices.fetchLivePevNews(topic = topic, category = category, locationLabel = loc)
      _newsState.value =
        res.fold(
          onSuccess = { UiState.Success(it) },
          onFailure = { UiState.Error(it.message ?: "Live PEV news wire unavailable") },
        )
    }
  }

  fun updateSettings(transform: (MasterSettings) -> MasterSettings) {
    viewModelScope.launch { settingsStore.update(transform) }
  }

  fun resetSettings() {
    viewModelScope.launch { settingsStore.resetAll() }
  }

  fun runLiveDiagnostics(gpsActive: Boolean) {
    viewModelScope.launch {
      _diagnosticsBusy.value = true
      _diagnosticResults.value =
        LiveCloudServices.runLiveConnectionDiagnostics(
          context = ctx,
          supabaseService = supabaseService,
          firebaseSignedIn = auth.currentUser != null,
          gpsActive = gpsActive,
        )
      _diagnosticsBusy.value = false
    }
  }

  suspend fun exportVaultJson(): String {
    val active = activeUser.value
    val settingsJson = settingsStore.exportAsJson()
    return when {
      active != null && active.firebaseAuthenticated ->
        riderRepo.exportCloudBackupJson(active.uid, settingsJson).getOrElse { settingsJson }
      active != null ->
        localVault.exportFullBackupJson(active.uid, settingsJson)
      else -> settingsJson
    }
  }

  fun importVaultJson(raw: String, onDone: (Result<Unit>) -> Unit) {
    viewModelScope.launch {
      val res = settingsStore.importFromJson(raw)
      val active = activeUser.value
      if (res.isSuccess && active != null) {
        if (active.firebaseAuthenticated) {
          riderRepo.restoreFirestoreFromBackupJson(active.uid, raw)
        } else {
          localVault.restoreFullBackupJson(active.uid, raw)
        }
      }
      onDone(res)
    }
  }

  fun clearCloudBackupMessage() {
    _cloudBackupMessage.value = ""
  }

  fun backupAllToCloud() {
    val active = activeUser.value
    if (active == null) {
      _cloudBackupMessage.value = "Sign in to back up to the cloud."
      return
    }
    viewModelScope.launch {
      _cloudBackupBusy.value = true
      _cloudBackupMessage.value = "Packaging encrypted settings & garage vault..."
      val fullJson = exportVaultJson()
      val hasSupabase = supabaseSession.value != null && supabaseService.isConfigured()
      val providers = mutableListOf<String>()
      var lastErr: Throwable? = null

      if (active.firebaseAuthenticated) {
        riderRepo
          .saveFirestoreCloudBackup(
            userId = active.uid,
            payloadJson = fullJson,
            summary = "Rural ERides GO • ${active.displayName.ifBlank { "Pilot" }}",
          )
          .onSuccess { providers += "Firestore" }
          .onFailure { lastErr = it }
      }
      if (hasSupabase || active.provider == AuthProviderType.SUPABASE) {
        supabaseService
          .saveCloudBackup(fullJson)
          .onSuccess { providers += "Supabase" }
          .onFailure { lastErr = it }
      }

      if (providers.isNotEmpty()) {
        val now = System.currentTimeMillis()
        val label = providers.joinToString(" + ")
        settingsStore.update {
          it.copy(lastCloudBackupMs = now, lastCloudBackupProvider = label)
        }
        _cloudBackupMessage.value = "Cloud backup complete ($label • ${fullJson.length / 1024} KB)."
      } else {
        _cloudBackupMessage.value = lastErr?.message ?: "Cloud backup failed — check network or login."
      }
      _cloudBackupBusy.value = false
    }
  }

  fun restoreAllFromCloud() {
    val active = activeUser.value
    if (active == null) {
      _cloudBackupMessage.value = "Sign in to restore from cloud."
      return
    }
    viewModelScope.launch {
      _cloudBackupBusy.value = true
      _cloudBackupMessage.value = "Fetching latest cloud backup..."
      var fetched: Pair<String, Long>? = null
      var source = ""
      var lastErr: Throwable? = null

      if (active.firebaseAuthenticated) {
        riderRepo
          .loadFirestoreCloudBackup(active.uid)
          .onSuccess {
            fetched = it
            source = "Firestore"
          }
          .onFailure { lastErr = it }
      }
      if (fetched == null && (supabaseSession.value != null || active.provider == AuthProviderType.SUPABASE)) {
        supabaseService
          .loadCloudBackup()
          .onSuccess {
            fetched = it
            source = "Supabase"
          }
          .onFailure { lastErr = it }
      }

      val payload = fetched
      if (payload != null) {
        val settingsRes = settingsStore.importFromJson(payload.first)
        if (active.firebaseAuthenticated) {
          riderRepo.restoreFirestoreFromBackupJson(active.uid, payload.first)
        } else {
          localVault.restoreFullBackupJson(active.uid, payload.first)
        }
        _cloudBackupMessage.value =
          if (settingsRes.isSuccess) "Restored full vault & settings from $source."
          else "Vault restored from $source (settings partial)."
      } else {
        _cloudBackupMessage.value = lastErr?.message ?: "No cloud backup found."
      }
      _cloudBackupBusy.value = false
    }
  }

  fun syncPilotMatchProfile(
    vehicleName: String,
    batteryWh: Long,
    matchSettings: MasterSettings,
  ) {
    val active = activeUser.value ?: return
    val callsign = _profile.value?.callsign?.ifBlank { active.displayName } ?: return
    val encodedSummary =
      RiderMatchEngine.encodePilotMatchSummary(
        vehicleName = vehicleName,
        batteryWh = batteryWh,
        pace = matchSettings.matchPaceTier,
        terrain = matchSettings.matchTerrainPref,
        usableRangeMi = matchSettings.matchMinRangeMi,
        vibe = matchSettings.matchScheduleVibe,
        zipOrTown = matchSettings.matchHomeZipOrTown,
      )
    viewModelScope.launch {
      if (active.firebaseAuthenticated) {
        riderRepo.updatePublicVehicleSummary(active.uid, encodedSummary)
      } else {
        val sbToken = supabaseSession.value?.accessToken.orEmpty()
        if (sbToken.isNotBlank()) {
          val curPilot = _publicPilots.value.firstOrNull { it.userId == active.uid }
          val updatedPilot =
            (curPilot ?: PublicPilotProfile(id = active.uid, userId = active.uid, callsign = callsign))
              .copy(
                userId = active.uid,
                callsign = callsign,
                vehicleSummary = encodedSummary,
                avatarBase64 = _profile.value?.avatarBase64 ?: curPilot?.avatarBase64.orEmpty(),
                visibility = "PUBLIC",
              )
          LiveCloudServices.upsertSupabasePilotProfile(sbToken, updatedPilot).onSuccess {
            _publicPilots.value =
              (listOf(updatedPilot) + _publicPilots.value.filterNot { it.userId == active.uid })
                .sortedByDescending { it.xpPoints }
          }
        }
      }
    }
  }

  fun readDeviceBatteryPct(): Int {
    val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    return bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0, 100) ?: 100
  }

  fun dispatchSosSms(lat: Double, lng: Double) {
    val num = _profile.value?.sosContactNumber.orEmpty()
    val template = settings.value.sosTemplate
    val body = "$template\nGPS: https://maps.google.com/?q=$lat,$lng"
    val intent =
      Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(num)}")).apply {
        putExtra("sms_body", body)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    val launched = runCatching { ctx.startActivity(intent) }.isSuccess
    if (!launched) {
      val fallbackShare =
        Intent(Intent.ACTION_SEND).apply {
          type = "text/plain"
          putExtra(Intent.EXTRA_SUBJECT, "EMERGENCY PEV SOS")
          putExtra(Intent.EXTRA_TEXT, body)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      val shared = runCatching { ctx.startActivity(Intent.createChooser(fallbackShare, "Send Emergency SOS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
      if (!shared) {
        android.widget.Toast.makeText(ctx, "No SMS app found on this device. GPS: $lat, $lng", android.widget.Toast.LENGTH_LONG).show()
      }
    }
  }

  fun dialEmergencyLine() {
    val num = _profile.value?.sosContactNumber.orEmpty().ifBlank { "911" }
    val intent =
      Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    val launched = runCatching { ctx.startActivity(intent) }.isSuccess
    if (!launched) {
      android.widget.Toast.makeText(ctx, "No phone dialer found. Emergency number: $num", android.widget.Toast.LENGTH_LONG).show()
    }
  }

  override fun onCleared() {
    voiceAudio.release()
    super.onCleared()
  }
}
