package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.LiveCloudServices
import com.example.data.ActiveNavRoute
import com.example.data.AlertTonePreset
import com.example.data.ChannelPresence
import com.example.data.ChannelSpeakerState
import com.example.data.ChannelWebRtcSignal
import com.example.data.CommunityRepository
import com.example.data.LiveChargingStation
import com.example.data.MasterSettings
import com.example.data.NavRouteProfile
import com.example.data.RadarHazard
import com.example.data.RadarMeetup
import com.example.data.RadarRider
import com.example.data.RadarWaypoint
import com.example.data.WalkieVoiceLog
import com.example.data.WeatherGeoLocation
import com.example.hardware.VoiceAudioManager
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "RadarViewModel"

class RadarViewModel(application: Application) : AndroidViewModel(application) {

  private val ctx = application.applicationContext
  private val repo = CommunityRepository(ctx)
  val audio = VoiceAudioManager(ctx)

  private val _riders = MutableStateFlow<List<RadarRider>>(emptyList())
  val riders: StateFlow<List<RadarRider>> = _riders.asStateFlow()

  private val _meetups = MutableStateFlow<List<RadarMeetup>>(emptyList())
  val meetups: StateFlow<List<RadarMeetup>> = _meetups.asStateFlow()

  private val _hazards = MutableStateFlow<List<RadarHazard>>(emptyList())
  val hazards: StateFlow<List<RadarHazard>> = _hazards.asStateFlow()

  private val _waypoints = MutableStateFlow<List<RadarWaypoint>>(emptyList())
  val waypoints: StateFlow<List<RadarWaypoint>> = _waypoints.asStateFlow()

  private val _elevationProfile = MutableStateFlow<List<Double>>(emptyList())
  val elevationProfile: StateFlow<List<Double>> = _elevationProfile.asStateFlow()

  private val _lockedTargetUid = MutableStateFlow<String?>(null)
  val lockedTargetUid: StateFlow<String?> = _lockedTargetUid.asStateFlow()

  private val _rulerAnchor = MutableStateFlow<Pair<Double, Double>?>(null)
  val rulerAnchor: StateFlow<Pair<Double, Double>?> = _rulerAnchor.asStateFlow()

  private val _geoFenceCenter = MutableStateFlow<Pair<Double, Double>?>(null)
  val geoFenceCenter: StateFlow<Pair<Double, Double>?> = _geoFenceCenter.asStateFlow()

  private val _geoFenceRadiusM = MutableStateFlow(800)
  val geoFenceRadiusM: StateFlow<Int> = _geoFenceRadiusM.asStateFlow()

  private val _geoFenceBreached = MutableStateFlow(false)
  val geoFenceBreached: StateFlow<Boolean> = _geoFenceBreached.asStateFlow()

  // Live Turn-by-Turn GPS Navigation State
  private val _activeNavRoute = MutableStateFlow<ActiveNavRoute?>(null)
  val activeNavRoute: StateFlow<ActiveNavRoute?> = _activeNavRoute.asStateFlow()

  private val _navSearching = MutableStateFlow(false)
  val navSearching: StateFlow<Boolean> = _navSearching.asStateFlow()

  private val _navRouting = MutableStateFlow(false)
  val navRouting: StateFlow<Boolean> = _navRouting.asStateFlow()

  private val _navSearchResults = MutableStateFlow<List<WeatherGeoLocation>>(emptyList())
  val navSearchResults: StateFlow<List<WeatherGeoLocation>> = _navSearchResults.asStateFlow()

  private val _navCurrentStepIndex = MutableStateFlow(0)
  val navCurrentStepIndex: StateFlow<Int> = _navCurrentStepIndex.asStateFlow()

  private val _navDistanceToTurnM = MutableStateFlow(0.0)
  val navDistanceToTurnM: StateFlow<Double> = _navDistanceToTurnM.asStateFlow()

  private val _navRemainingDistKm = MutableStateFlow(0.0)
  val navRemainingDistKm: StateFlow<Double> = _navRemainingDistKm.asStateFlow()

  private val _navRemainingSec = MutableStateFlow(0L)
  val navRemainingSec: StateFlow<Long> = _navRemainingSec.asStateFlow()

  private val _navOffRouteAlert = MutableStateFlow(false)
  val navOffRouteAlert: StateFlow<Boolean> = _navOffRouteAlert.asStateFlow()

  private val _navStatusMsg = MutableStateFlow("")
  val navStatusMsg: StateFlow<String> = _navStatusMsg.asStateFlow()

  private val _liveChargers = MutableStateFlow<List<LiveChargingStation>>(emptyList())
  val liveChargers: StateFlow<List<LiveChargingStation>> = _liveChargers.asStateFlow()

  private val _chargersLoading = MutableStateFlow(false)
  val chargersLoading: StateFlow<Boolean> = _chargersLoading.asStateFlow()

  private var lastSpokenStepIdx = -1
  private var lastAutoRerouteMs = 0L
  private var navRouteJob: Job? = null

  private val _activeChannelId = MutableStateFlow("SECTOR_01_OPEN")
  val activeChannelId: StateFlow<String> = _activeChannelId.asStateFlow()

  private val _presence = MutableStateFlow<List<ChannelPresence>>(emptyList())
  val presence: StateFlow<List<ChannelPresence>> = _presence.asStateFlow()

  private val _speaker = MutableStateFlow<ChannelSpeakerState?>(null)
  val speaker: StateFlow<ChannelSpeakerState?> = _speaker.asStateFlow()

  private val _walkieLogs = MutableStateFlow<List<WalkieVoiceLog>>(emptyList())
  val walkieLogs: StateFlow<List<WalkieVoiceLog>> = _walkieLogs.asStateFlow()

  private val _webRtcSignals = MutableStateFlow<List<ChannelWebRtcSignal>>(emptyList())
  val webRtcSignals: StateFlow<List<ChannelWebRtcSignal>> = _webRtcSignals.asStateFlow()

  private val _isTransmitting = MutableStateFlow(false)
  val isTransmitting: StateFlow<Boolean> = _isTransmitting.asStateFlow()

  private var radarJob: Job? = null
  private var channelJob: Job? = null
  private var boundSessionKey: String? = null
  private var boundUid = ""
  private var firebaseBound = false
  private var supabaseToken = ""
  private var lastBeaconSentMs = 0L

  private val cloudBound: Boolean
    get() = firebaseBound || (supabaseToken.isNotBlank() && boundUid.isNotBlank())

  fun bindCloudSession(
    uid: String?,
    firebaseAuthenticated: Boolean,
    supabaseAccessToken: String = "",
  ) {
    val cleanUid = uid.orEmpty().trim()
    val nextKey =
      if (cleanUid.isBlank()) null
      else "$cleanUid:$firebaseAuthenticated:${supabaseAccessToken.hashCode()}"
    if (nextKey == boundSessionKey) return
    boundSessionKey = nextKey
    boundUid = cleanUid
    firebaseBound = firebaseAuthenticated
    supabaseToken = supabaseAccessToken.trim()

    radarJob?.cancel()
    channelJob?.cancel()
    if (!cloudBound) {
      _riders.value = emptyList()
      _meetups.value = emptyList()
      _hazards.value = emptyList()
      _waypoints.value = emptyList()
      _presence.value = emptyList()
      _speaker.value = null
      _walkieLogs.value = emptyList()
      _webRtcSignals.value = emptyList()
      return
    }

    if (firebaseBound) {
      radarJob =
        viewModelScope.launch {
          launch {
            repo
              .observeRadarRiders()
              .catch { e ->
                if (e is CancellationException) throw e
                Log.w(TAG, "Radar riders error: ${e.message}")
              }
              .collect { _riders.value = it }
          }
          launch {
            repo
              .observeRadarMeetups()
              .catch { e ->
                if (e is CancellationException) throw e
                Log.w(TAG, "Radar meetups error: ${e.message}")
              }
              .collect { _meetups.value = it }
          }
          launch {
            repo
              .observeRadarHazards()
              .catch { e ->
                if (e is CancellationException) throw e
                Log.w(TAG, "Radar hazards error: ${e.message}")
              }
              .collect { _hazards.value = it }
          }
          launch {
            repo
              .observeRadarWaypoints()
              .catch { e ->
                if (e is CancellationException) throw e
                Log.w(TAG, "Radar waypoints error: ${e.message}")
              }
              .collect { _waypoints.value = it }
          }
        }
    } else {
      val token = supabaseToken
      val myUid = boundUid
      radarJob =
        viewModelScope.launch {
          while (isActive) {
            LiveCloudServices.fetchSupabaseRadarRiders(token, myUid)
              .onSuccess { _riders.value = it }
              .onFailure { Log.w(TAG, "SB radar riders poll failed: ${it.message}") }
            LiveCloudServices.fetchSupabaseRadarMeetups(token)
              .onSuccess { _meetups.value = it }
            LiveCloudServices.fetchSupabaseRadarHazards(token)
              .onSuccess { _hazards.value = it }
            LiveCloudServices.fetchSupabaseRadarWaypoints(token)
              .onSuccess { _waypoints.value = it }
            delay(5_000L)
          }
        }
    }
    subscribeChannel(_activeChannelId.value)
  }

  fun switchWalkieChannel(channelId: String, callsign: String, joinPresence: Boolean) {
    _activeChannelId.value = channelId
    if (!cloudBound) return
    subscribeChannel(channelId)
    if (joinPresence) {
      viewModelScope.launch {
        if (firebaseBound) {
          repo.joinChannelPresence(channelId, callsign, true)
        } else {
          LiveCloudServices.upsertSupabaseWalkiePresence(supabaseToken, channelId, boundUid, callsign, true)
        }
      }
    }
  }

  private fun subscribeChannel(channelId: String) {
    if (!cloudBound) return
    channelJob?.cancel()
    if (firebaseBound) {
      channelJob =
        viewModelScope.launch {
          launch {
            repo
              .observeChannelPresence(channelId)
              .catch { e ->
                if (e is CancellationException) throw e
                Log.w(TAG, "Presence error: ${e.message}")
              }
              .collect { _presence.value = it }
          }
          launch {
            repo
              .observeChannelSpeaker(channelId)
              .catch { e ->
                if (e is CancellationException) throw e
                Log.w(TAG, "Speaker error: ${e.message}")
              }
              .collect { _speaker.value = it }
          }
          launch {
            repo
              .observeWalkieLogs(channelId)
              .catch { e ->
                if (e is CancellationException) throw e
                Log.w(TAG, "Walkie logs error: ${e.message}")
              }
              .collect { _walkieLogs.value = it }
          }
          launch {
            repo
              .observeChannelWebRtcSignals(channelId)
              .catch { e ->
                if (e is CancellationException) throw e
                Log.w(TAG, "Walkie WebRTC signals error: ${e.message}")
              }
              .collect { _webRtcSignals.value = it }
          }
        }
    } else {
      val token = supabaseToken
      channelJob =
        viewModelScope.launch {
          while (isActive) {
            LiveCloudServices.fetchSupabaseWalkiePresence(token, channelId)
              .onSuccess { _presence.value = it }
            LiveCloudServices.fetchSupabaseWalkieSpeaker(token, channelId)
              .onSuccess { _speaker.value = it }
            LiveCloudServices.fetchSupabaseWalkieLogs(token, channelId)
              .onSuccess { _walkieLogs.value = it }
            LiveCloudServices.fetchSupabaseWalkieSignals(token, channelId)
              .onSuccess { _webRtcSignals.value = it }
            delay(2_500L)
          }
        }
    }
  }

  fun joinWalkiePresence(callsign: String, active: Boolean) {
    if (!cloudBound) return
    val ch = _activeChannelId.value
    viewModelScope.launch {
      if (firebaseBound) {
        repo.joinChannelPresence(ch, callsign, active)
      } else {
        LiveCloudServices.upsertSupabaseWalkiePresence(supabaseToken, ch, boundUid, callsign, active)
        LiveCloudServices.fetchSupabaseWalkiePresence(supabaseToken, ch)
          .onSuccess { _presence.value = it }
      }
    }
  }

  fun setLiveChannelSpeaker(callsign: String, speaking: Boolean, volumePct: Int) {
    _isTransmitting.value = speaking
    audio.playSquelchChirp(volumePct, isEnd = !speaking)
    if (!cloudBound) return
    val ch = _activeChannelId.value
    viewModelScope.launch {
      if (firebaseBound) {
        repo.setChannelSpeakerFloor(ch, callsign, speaking)
      } else {
        LiveCloudServices.upsertSupabaseWalkieSpeaker(supabaseToken, ch, boundUid, callsign, speaking)
      }
    }
  }

  fun sendWalkieWebRtcSignal(
    callsign: String,
    targetUid: String,
    signalType: String,
    payload: String,
  ) {
    if (!cloudBound) return
    val ch = _activeChannelId.value
    viewModelScope.launch {
      if (firebaseBound) {
        repo.sendChannelWebRtcSignal(ch, callsign, targetUid, signalType, payload)
      } else {
        LiveCloudServices.insertSupabaseWalkieSignal(
          supabaseToken,
          ChannelWebRtcSignal(
            id = "rtc_${UUID.randomUUID().toString().take(10)}",
            senderId = boundUid,
            senderCallsign = callsign,
            targetUid = targetUid,
            channelId = ch,
            signalType = signalType,
            payload = payload,
          ),
        )
      }
    }
  }

  fun clearMyWalkieWebRtcSignals() {
    if (!cloudBound) return
    val ch = _activeChannelId.value
    viewModelScope.launch {
      if (firebaseBound) {
        repo.clearMyChannelWebRtcSignals(ch)
      } else {
        LiveCloudServices.clearSupabaseWalkieSignals(supabaseToken, ch, boundUid)
      }
    }
  }

  fun beginPtt(callsign: String, volumePct: Int): Boolean {
    if (!cloudBound || _isTransmitting.value) return false
    val started =
      audio.startPttCapture(viewModelScope, maxMs = 10_000L) { b64, dur ->
        sendPttClip(callsign, b64, dur, volumePct)
      }
    if (started) {
      _isTransmitting.value = true
      audio.playSquelchChirp(volumePct, isEnd = false)
      val ch = _activeChannelId.value
      viewModelScope.launch {
        if (firebaseBound) {
          repo.setChannelSpeakerFloor(ch, callsign, true)
        } else {
          LiveCloudServices.upsertSupabaseWalkieSpeaker(supabaseToken, ch, boundUid, callsign, true)
        }
      }
    }
    return started
  }

  fun endPtt(callsign: String, volumePct: Int) {
    if (!_isTransmitting.value) return
    val (b64, dur) = audio.finishPttCapture()
    sendPttClip(callsign, b64, dur, volumePct)
  }

  private fun sendPttClip(callsign: String, b64: String, dur: Long, volumePct: Int) {
    _isTransmitting.value = false
    audio.playSquelchChirp(volumePct, isEnd = true)
    if (!cloudBound) return
    val ch = _activeChannelId.value
    viewModelScope.launch {
      if (firebaseBound) {
        repo.setChannelSpeakerFloor(ch, callsign, false)
        if (b64.isNotBlank()) {
          repo.sendWalkieVoiceLog(ch, callsign, b64, dur)
        }
      } else {
        LiveCloudServices.upsertSupabaseWalkieSpeaker(supabaseToken, ch, boundUid, callsign, false)
        if (b64.isNotBlank()) {
          val log =
            WalkieVoiceLog(
              id = "ptt_${UUID.randomUUID().toString().take(8)}",
              userId = boundUid,
              senderCallsign = callsign,
              channelId = ch,
              audioBase64 = b64,
              durationMs = dur,
            )
          LiveCloudServices.insertSupabaseWalkieLog(supabaseToken, log).onSuccess {
            _walkieLogs.value = listOf(log) + _walkieLogs.value
          }
        }
      }
    }
  }

  fun playWalkieClip(log: WalkieVoiceLog, volumePct: Int) {
    audio.playSquelchChirp(volumePct, isEnd = false)
    audio.playBase64Pcm(viewModelScope, log.audioBase64)
  }

  fun publishBeaconIfDue(
    callsign: String,
    vehicleName: String,
    lat: Double,
    lng: Double,
    speedKmh: Double,
    headingDeg: Double,
    batteryPct: Int,
    statusTag: String,
    visible: Boolean,
    force: Boolean = false,
  ) {
    if (lat == 0.0 && lng == 0.0) return
    val center = _geoFenceCenter.value
    if (center != null) {
      val distM = haversineKm(center.first, center.second, lat, lng) * 1000.0
      _geoFenceBreached.value = distM > _geoFenceRadiusM.value
    } else {
      _geoFenceBreached.value = false
    }

    if (!cloudBound) return
    val now = System.currentTimeMillis()
    if (!force && now - lastBeaconSentMs < 10_000L) return
    lastBeaconSentMs = now
    viewModelScope.launch {
      if (firebaseBound) {
        repo.publishMyRadarBeacon(
          callsign,
          vehicleName,
          lat,
          lng,
          speedKmh,
          headingDeg,
          batteryPct,
          statusTag,
          visible,
        )
      } else {
        val rider =
          RadarRider(
            id = boundUid,
            userId = boundUid,
            callsign = callsign.ifBlank { "PILOT" },
            vehicleName = vehicleName,
            lat = lat,
            lng = lng,
            speedKmh = speedKmh,
            headingDeg = headingDeg,
            batteryPct = batteryPct.toLong(),
            statusTag = if (visible) statusTag else "OFFLINE",
            visibility = if (visible) "PUBLIC" else "PRIVATE",
          )
        LiveCloudServices.upsertSupabaseRadarRider(supabaseToken, rider).onSuccess {
          val filtered = _riders.value.filterNot { it.userId == boundUid }
          _riders.value = if (visible) listOf(rider) + filtered else filtered
        }
      }
    }
  }

  fun toggleTargetLock(uid: String?) {
    _lockedTargetUid.value = if (_lockedTargetUid.value == uid) null else uid
  }

  fun setRulerAnchor(lat: Double, lng: Double) {
    _rulerAnchor.value = lat to lng
  }

  fun clearRulerAnchor() {
    _rulerAnchor.value = null
  }

  fun setGeoFence(lat: Double, lng: Double, radiusM: Int) {
    _geoFenceCenter.value = lat to lng
    _geoFenceRadiusM.value = radiusM.coerceIn(100, 25_000)
    _geoFenceBreached.value = false
  }

  fun clearGeoFence() {
    _geoFenceCenter.value = null
    _geoFenceBreached.value = false
  }

  fun createMeetup(callsign: String, title: String, desc: String, time: String, lat: Double, lng: Double) {
    if (!cloudBound) return
    viewModelScope.launch {
      if (firebaseBound) {
        repo.createMeetup(callsign, title, desc, time, lat, lng)
      } else {
        val meetup =
          RadarMeetup(
            id = "meet_${UUID.randomUUID().toString().take(8)}",
            userId = boundUid,
            creatorCallsign = callsign,
            title = title,
            description = desc,
            scheduledTime = time,
            lat = lat,
            lng = lng,
            attendeeCount = 1L,
          )
        LiveCloudServices.insertSupabaseRadarMeetup(supabaseToken, meetup).onSuccess {
          _meetups.value = listOf(meetup) + _meetups.value
        }
      }
    }
  }

  fun rsvpMeetup(id: String) {
    if (!cloudBound) return
    viewModelScope.launch {
      if (firebaseBound) {
        repo.rsvpMeetup(id)
      } else {
        val cur = _meetups.value.firstOrNull { it.id == id } ?: return@launch
        val next = cur.attendeeCount + 1L
        LiveCloudServices.rsvpSupabaseRadarMeetup(supabaseToken, id, next).onSuccess {
          _meetups.value = _meetups.value.map { if (it.id == id) it.copy(attendeeCount = next) else it }
        }
      }
    }
  }

  fun createHazard(callsign: String, type: String, desc: String, lat: Double, lng: Double) {
    if (!cloudBound) return
    viewModelScope.launch {
      if (firebaseBound) {
        repo.createHazard(callsign, type, desc, lat, lng)
      } else {
        val hazard =
          RadarHazard(
            id = "haz_${UUID.randomUUID().toString().take(8)}",
            userId = boundUid,
            creatorCallsign = callsign,
            hazardType = type,
            description = desc,
            lat = lat,
            lng = lng,
            upvotes = 1L,
          )
        LiveCloudServices.insertSupabaseRadarHazard(supabaseToken, hazard).onSuccess {
          _hazards.value = listOf(hazard) + _hazards.value
        }
      }
    }
  }

  fun upvoteHazard(id: String) {
    if (!cloudBound) return
    viewModelScope.launch {
      if (firebaseBound) {
        repo.upvoteHazard(id)
      } else {
        val cur = _hazards.value.firstOrNull { it.id == id } ?: return@launch
        val next = cur.upvotes + 1L
        LiveCloudServices.upvoteSupabaseRadarHazard(supabaseToken, id, next).onSuccess {
          _hazards.value = _hazards.value.map { if (it.id == id) it.copy(upvotes = next) else it }
        }
      }
    }
  }

  fun createWaypoint(callsign: String, type: String, title: String, notes: String, lat: Double, lng: Double) {
    if (!cloudBound) return
    viewModelScope.launch {
      if (firebaseBound) {
        repo.createWaypoint(callsign, type, title, notes, lat, lng)
      } else {
        val wp =
          RadarWaypoint(
            id = "wp_${UUID.randomUUID().toString().take(8)}",
            userId = boundUid,
            creatorCallsign = callsign,
            waypointType = type,
            title = title,
            notes = notes,
            lat = lat,
            lng = lng,
          )
        LiveCloudServices.insertSupabaseRadarWaypoint(supabaseToken, wp).onSuccess {
          _waypoints.value = listOf(wp) + _waypoints.value
        }
      }
    }
  }

  fun refreshElevationSparkline(points: List<Pair<Double, Double>>) {
    if (points.size < 2) return
    viewModelScope.launch {
      _elevationProfile.value = LiveCloudServices.fetchElevationSparkline(points)
    }
  }

  // --------------------------------------------------------------------------
  // LIVE TURN-BY-TURN GPS NAVIGATION & PUBLIC CHARGING STATIONS
  // --------------------------------------------------------------------------
  fun searchNavDestinations(query: String, nearLat: Double, nearLng: Double) {
    if (query.trim().length < 2) {
      _navSearchResults.value = emptyList()
      return
    }
    viewModelScope.launch {
      _navSearching.value = true
      _navStatusMsg.value = ""
      LiveCloudServices.searchNavDestinations(query, nearLat, nearLng)
        .onSuccess { list ->
          _navSearchResults.value = list
          if (list.isEmpty()) _navStatusMsg.value = "No matching destinations found for \"$query\"."
        }
        .onFailure { _navStatusMsg.value = it.localizedMessage ?: "Destination search failed." }
      _navSearching.value = false
    }
  }

  fun clearNavSearch() {
    _navSearchResults.value = emptyList()
  }

  fun startGpsNavigation(
    startLat: Double,
    startLng: Double,
    destName: String,
    destLat: Double,
    destLng: Double,
    profile: NavRouteProfile,
    settings: MasterSettings,
    vehicleWh: Long = 1500L,
  ) {
    if (startLat == 0.0 && startLng == 0.0) {
      _navStatusMsg.value = "Waiting for live GPS fix before calculating route."
      return
    }
    navRouteJob?.cancel()
    navRouteJob =
      viewModelScope.launch {
        _navRouting.value = true
        _navStatusMsg.value = "Calculating live ${profile.label} route..."
      LiveCloudServices.fetchTurnByTurnRoute(
        startLat = startLat,
        startLng = startLng,
        endLat = destLat,
        endLng = destLng,
        destinationName = destName,
        profile = profile,
        avoidHighways = settings.navAvoidHighways,
        vehicleWh = vehicleWh,
      ).onSuccess { route ->
        _activeNavRoute.value = route
        _navCurrentStepIndex.value = 0
        _navRemainingDistKm.value = route.totalDistanceKm
        _navRemainingSec.value = route.totalDurationSec
        _navDistanceToTurnM.value = route.steps.firstOrNull()?.distanceM ?: (route.totalDistanceKm * 1000.0)
        _navOffRouteAlert.value = false
        _navSearchResults.value = emptyList()
        lastSpokenStepIdx = -1
        _navStatusMsg.value = "Live route locked via ${route.providerSource}."
        if (settings.navVoiceGuidance && !settings.autoMuteAlertsWhileRiding) {
          audio.playAlertTonePreset(settings.navTurnTone, settings.globalVolume)
          val firstInstr = route.steps.firstOrNull()?.instruction ?: "Proceed to $destName"
          audio.speakWithVoiceProfile(
            text = "Navigation started to $destName. $firstInstr",
            profile = settings.aiVoiceProfile,
            volumePct = settings.globalVolume,
            speechRateMult = settings.ttsSpeechRate,
            pitchMult = settings.ttsPitch,
          )
          lastSpokenStepIdx = 0
        }
      }.onFailure { err ->
        _navStatusMsg.value = "Route error: ${err.localizedMessage ?: "Unable to route"}"
      }
      _navRouting.value = false
    }
  }

  fun updateLiveNavigationProgress(
    lat: Double,
    lng: Double,
    speedKmh: Double,
    settings: MasterSettings,
    vehicleWh: Long = 1500L,
  ) {
    val route = _activeNavRoute.value ?: return
    if (lat == 0.0 && lng == 0.0) return

    val distToDestKm = haversineKm(lat, lng, route.destLat, route.destLng)
    _navRemainingDistKm.value = distToDestKm
    val effectiveSpeed = speedKmh.coerceAtLeast(18.0)
    _navRemainingSec.value = ((distToDestKm / effectiveSpeed) * 3600.0).toLong().coerceAtLeast(0L)

    if (distToDestKm < 0.045) {
      if (settings.navVoiceGuidance && lastSpokenStepIdx != 9999) {
        audio.playAlertTonePreset(settings.navTurnTone, settings.globalVolume)
        audio.speakWithVoiceProfile(
          text = "You have arrived at ${route.destinationName}.",
          profile = settings.aiVoiceProfile,
          volumePct = settings.globalVolume,
          speechRateMult = settings.ttsSpeechRate,
          pitchMult = settings.ttsPitch,
        )
        lastSpokenStepIdx = 9999
      }
      _navStatusMsg.value = "Arrived at ${route.destinationName}!"
      if (settings.navAutoEndOnArrival) {
        stopGpsNavigation(announce = false)
        _navStatusMsg.value = "Arrived at ${route.destinationName}!"
      }
      return
    }

    val steps = route.steps
    if (steps.isNotEmpty()) {
      var curIdx = _navCurrentStepIndex.value.coerceIn(0, steps.lastIndex)
      val nextIdx = (curIdx + 1).coerceAtMost(steps.lastIndex)
      val distNextM = haversineKm(lat, lng, steps[nextIdx].lat, steps[nextIdx].lng) * 1000.0
      _navDistanceToTurnM.value = distNextM

      if (distNextM < 32.0 && curIdx < steps.lastIndex) {
        curIdx = nextIdx
        _navCurrentStepIndex.value = curIdx
      }

      if (settings.navVoiceGuidance && !settings.autoMuteAlertsWhileRiding && distNextM < 130.0 && lastSpokenStepIdx != nextIdx) {
        lastSpokenStepIdx = nextIdx
        audio.playAlertTonePreset(settings.navTurnTone, settings.globalVolume)
        val distSpeak = if (settings.distanceUnit == "mi") "${(distNextM * 3.28084).toInt()} feet" else "${distNextM.toInt()} meters"
        audio.speakWithVoiceProfile(
          text = "In $distSpeak, ${steps[nextIdx].instruction}",
          profile = settings.aiVoiceProfile,
          volumePct = settings.globalVolume,
          speechRateMult = settings.ttsSpeechRate,
          pitchMult = settings.ttsPitch,
          preferFastLocal = true,
        )
      }
    }

    // Off-route check against route polyline
    val minPolyDistM =
      route.polylinePoints.minOfOrNull { (pLat, pLng) -> haversineKm(lat, lng, pLat, pLng) * 1000.0 } ?: 0.0
    val isOffRoute = minPolyDistM > 85.0
    _navOffRouteAlert.value = isOffRoute
    val now = System.currentTimeMillis()
    if (isOffRoute && settings.navAutoReroute && !_navRouting.value && now - lastAutoRerouteMs > 20_000L) {
      lastAutoRerouteMs = now
      startGpsNavigation(
        startLat = lat,
        startLng = lng,
        destName = route.destinationName,
        destLat = route.destLat,
        destLng = route.destLng,
        profile = route.profile,
        settings = settings,
        vehicleWh = vehicleWh,
      )
    }
  }

  fun stopGpsNavigation(announce: Boolean = false, volumePct: Int = 90) {
    navRouteJob?.cancel()
    navRouteJob = null
    _navRouting.value = false
    _activeNavRoute.value = null
    _navCurrentStepIndex.value = 0
    _navDistanceToTurnM.value = 0.0
    _navRemainingDistKm.value = 0.0
    _navRemainingSec.value = 0L
    _navOffRouteAlert.value = false
    lastSpokenStepIdx = -1
    audio.stopSpeaking()
    if (announce) {
      audio.speakText("Navigation cancelled.", volumePct = volumePct)
    }
    _navStatusMsg.value = "Navigation cancelled."
  }

  fun loadLiveChargingStations(lat: Double, lng: Double, radiusKm: Int = 15) {
    if (lat == 0.0 && lng == 0.0) {
      _navStatusMsg.value = "Acquire GPS fix first to scan live nearby chargers."
      return
    }
    viewModelScope.launch {
      _chargersLoading.value = true
      LiveCloudServices.fetchLiveChargingStations(lat, lng, radiusKm)
        .onSuccess { list ->
          _liveChargers.value = list
          _navStatusMsg.value =
            if (list.isEmpty()) "No public charging stations mapped within ${radiusKm}km."
            else "Found ${list.size} live public charging stations nearby."
        }
        .onFailure { _navStatusMsg.value = "Charger scan failed: ${it.localizedMessage}" }
      _chargersLoading.value = false
    }
  }

  companion object {
    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
      val r = 6371.0
      val dLat = Math.toRadians(lat2 - lat1)
      val dLon = Math.toRadians(lon2 - lon1)
      val a =
        sin(dLat / 2) * sin(dLat / 2) +
          cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
      val c = 2 * atan2(sqrt(a), sqrt(1 - a))
      return r * c
    }

    fun bearingDeg(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
      val phi1 = Math.toRadians(lat1)
      val phi2 = Math.toRadians(lat2)
      val dLon = Math.toRadians(lon2 - lon1)
      val y = sin(dLon) * cos(phi2)
      val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLon)
      return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }
  }

  override fun onCleared() {
    audio.release()
    super.onCleared()
  }
}
