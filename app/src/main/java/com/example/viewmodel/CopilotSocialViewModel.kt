package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiAiRepository
import com.example.ai.GeminiModelCatalog
import com.example.ai.PreRideFlightCheckResult
import com.example.core.FamilySafetyGuard
import com.example.core.LiveCloudServices
import com.example.data.AppDatabase
import com.example.data.BoardComment
import com.example.data.BoardPost
import com.example.data.CallSession
import com.example.data.CallSignal
import com.example.data.CallStreamFrame
import com.example.data.CommunityRepository
import com.example.data.CopilotMessage
import com.example.data.CopilotMessageEntity
import com.example.data.CopilotSessionEntity
import com.example.data.DmMessage
import com.example.data.DmThread
import com.example.data.LocalRiderVault
import com.example.data.RiderRepository
import com.example.data.SavedCopilotBriefing
import com.example.data.SavedVideo
import com.example.data.UiState
import com.example.data.YouTubeVideoItem
import com.example.data.YoutubeComment
import com.example.data.getActiveCustomVoice
import com.example.data.resolveEffectiveBase
import com.example.hardware.VoiceAudioManager
import com.example.ui.components.bitmapToCloudBase64DataUri
import java.io.ByteArrayOutputStream
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

import com.example.core.ReminderScheduler
import com.example.data.SettingsStore
import kotlinx.coroutines.flow.first

private const val TAG = "CopilotSocialViewModel"

class CopilotSocialViewModel(application: Application) : AndroidViewModel(application) {

  private val ctx = application.applicationContext
  private val chatDao = AppDatabase.get(ctx).copilotChatDao()
  private val communityRepo = CommunityRepository(ctx)
  private val riderRepo = RiderRepository(ctx)
  private val localVault = LocalRiderVault(ctx)
  private val aiRepo = GeminiAiRepository()
  val audio = VoiceAudioManager(ctx)

  // CO-PILOT STATE
  private val _chatSessions = MutableStateFlow<List<CopilotSessionEntity>>(emptyList())
  val chatSessions: StateFlow<List<CopilotSessionEntity>> = _chatSessions.asStateFlow()

  private val _activeSessionId = MutableStateFlow("")
  val activeSessionId: StateFlow<String> = _activeSessionId.asStateFlow()

  private val _messages = MutableStateFlow<List<CopilotMessage>>(emptyList())
  val messages: StateFlow<List<CopilotMessage>> = _messages.asStateFlow()

  private val _streamingText = MutableStateFlow("")
  val streamingText: StateFlow<String> = _streamingText.asStateFlow()

  private val _copilotBusy = MutableStateFlow(false)
  val copilotBusy: StateFlow<Boolean> = _copilotBusy.asStateFlow()

  private val _flightCheckState = MutableStateFlow<UiState<PreRideFlightCheckResult>?>(null)
  val flightCheckState: StateFlow<UiState<PreRideFlightCheckResult>?> = _flightCheckState.asStateFlow()

  // YOUTUBE CHANNEL + IN-APP MINI-PLAYER STATE
  private val _youtubeState =
    MutableStateFlow<UiState<Pair<List<YouTubeVideoItem>, String>>>(UiState.Loading)
  val youtubeState: StateFlow<UiState<Pair<List<YouTubeVideoItem>, String>>> =
    _youtubeState.asStateFlow()

  private val _musicMixesState =
    MutableStateFlow<UiState<Pair<List<YouTubeVideoItem>, String>>>(UiState.Loading)
  val musicMixesState: StateFlow<UiState<Pair<List<YouTubeVideoItem>, String>>> =
    _musicMixesState.asStateFlow()

  private val _activeMusicCategory = MutableStateFlow("Synthwave")
  val activeMusicCategory: StateFlow<String> = _activeMusicCategory.asStateFlow()

  private val _playerQueue = MutableStateFlow<List<YouTubeVideoItem>>(emptyList())
  val playerQueue: StateFlow<List<YouTubeVideoItem>> = _playerQueue.asStateFlow()

  private val _recentlyPlayedVideos = MutableStateFlow<List<YouTubeVideoItem>>(emptyList())
  val recentlyPlayedVideos: StateFlow<List<YouTubeVideoItem>> = _recentlyPlayedVideos.asStateFlow()

  private val _recentMusicQueries = MutableStateFlow<List<String>>(emptyList())
  val recentMusicQueries: StateFlow<List<String>> = _recentMusicQueries.asStateFlow()

  private val _activeVideo = MutableStateFlow<YouTubeVideoItem?>(null)
  val activeVideo: StateFlow<YouTubeVideoItem?> = _activeVideo.asStateFlow()

  private val _playerPlaying = MutableStateFlow(false)
  val playerPlaying: StateFlow<Boolean> = _playerPlaying.asStateFlow()

  private val _playerExpanded = MutableStateFlow(false)
  val playerExpanded: StateFlow<Boolean> = _playerExpanded.asStateFlow()

  private val _playerMuted = MutableStateFlow(false)
  val playerMuted: StateFlow<Boolean> = _playerMuted.asStateFlow()

  private val _playerShuffle = MutableStateFlow(false)
  val playerShuffle: StateFlow<Boolean> = _playerShuffle.asStateFlow()

  private val _playerRepeatMode = MutableStateFlow("off")
  val playerRepeatMode: StateFlow<String> = _playerRepeatMode.asStateFlow()

  private val _playerSpeed = MutableStateFlow(1.0f)
  val playerSpeed: StateFlow<Float> = _playerSpeed.asStateFlow()

  private val _sleepTimerRemainingSec = MutableStateFlow(0)
  val sleepTimerRemainingSec: StateFlow<Int> = _sleepTimerRemainingSec.asStateFlow()

  private val _playerError = MutableStateFlow("")
  val playerError: StateFlow<String> = _playerError.asStateFlow()

  private val _savedVideos = MutableStateFlow<List<SavedVideo>>(emptyList())
  val savedVideos: StateFlow<List<SavedVideo>> = _savedVideos.asStateFlow()

  private val _videoVolts = MutableStateFlow(0L)
  val videoVolts: StateFlow<Long> = _videoVolts.asStateFlow()

  private val _videoComments = MutableStateFlow<List<YoutubeComment>>(emptyList())
  val videoComments: StateFlow<List<YoutubeComment>> = _videoComments.asStateFlow()

  private val _videoAiAnswer = MutableStateFlow("")
  val videoAiAnswer: StateFlow<String> = _videoAiAnswer.asStateFlow()

  private val _videoAiBusy = MutableStateFlow(false)
  val videoAiBusy: StateFlow<Boolean> = _videoAiBusy.asStateFlow()

  // BOARD STATE
  private val _boardPosts = MutableStateFlow<List<BoardPost>>(emptyList())
  val boardPosts: StateFlow<List<BoardPost>> = _boardPosts.asStateFlow()

  private val _selectedPostComments = MutableStateFlow<List<BoardComment>>(emptyList())
  val selectedPostComments: StateFlow<List<BoardComment>> = _selectedPostComments.asStateFlow()

  private val _bookmarkedIds = MutableStateFlow<Set<String>>(emptySet())
  val bookmarkedIds: StateFlow<Set<String>> = _bookmarkedIds.asStateFlow()

  private val _reportedPostIds = MutableStateFlow<Set<String>>(emptySet())
  val reportedPostIds: StateFlow<Set<String>> = _reportedPostIds.asStateFlow()

  private val _sosTriageMap = MutableStateFlow<Map<String, String>>(emptyMap())
  val sosTriageMap: StateFlow<Map<String, String>> = _sosTriageMap.asStateFlow()

  private val _sosTriageBusyId = MutableStateFlow<String?>(null)
  val sosTriageBusyId: StateFlow<String?> = _sosTriageBusyId.asStateFlow()

  // DIRECT MESSAGES
  private val _dmThreads = MutableStateFlow<List<DmThread>>(emptyList())
  val dmThreads: StateFlow<List<DmThread>> = _dmThreads.asStateFlow()

  private val _dmMessages = MutableStateFlow<List<DmMessage>>(emptyList())
  val dmMessages: StateFlow<List<DmMessage>> = _dmMessages.asStateFlow()

  private val _mutedPeerIds = MutableStateFlow<Set<String>>(emptySet())
  val mutedPeerIds: StateFlow<Set<String>> = _mutedPeerIds.asStateFlow()

  private val _followedPilotIds = MutableStateFlow<Set<String>>(emptySet())
  val followedPilotIds: StateFlow<Set<String>> = _followedPilotIds.asStateFlow()

  private val _pinnedDmThreadIds = MutableStateFlow<Set<String>>(emptySet())
  val pinnedDmThreadIds: StateFlow<Set<String>> = _pinnedDmThreadIds.asStateFlow()

  private val _savedBriefings = MutableStateFlow<List<SavedCopilotBriefing>>(emptyList())
  val savedBriefings: StateFlow<List<SavedCopilotBriefing>> = _savedBriefings.asStateFlow()

  // LIVE AUDIO / VIDEO CALL STATE (WEBRTC + FIRESTORE SIGNALING)
  private val _callSessions = MutableStateFlow<List<CallSession>>(emptyList())
  val callSessions: StateFlow<List<CallSession>> = _callSessions.asStateFlow()

  private val _callHistory = MutableStateFlow<List<CallSession>>(emptyList())
  val callHistory: StateFlow<List<CallSession>> = _callHistory.asStateFlow()

  private val _callMinimized = MutableStateFlow(false)
  val callMinimized: StateFlow<Boolean> = _callMinimized.asStateFlow()

  private val _incomingCallSession = MutableStateFlow<CallSession?>(null)
  val incomingCallSession: StateFlow<CallSession?> = _incomingCallSession.asStateFlow()

  private val _activeCallSession = MutableStateFlow<CallSession?>(null)
  val activeCallSession: StateFlow<CallSession?> = _activeCallSession.asStateFlow()

  private val _callSignals = MutableStateFlow<List<CallSignal>>(emptyList())
  val callSignals: StateFlow<List<CallSignal>> = _callSignals.asStateFlow()

  private val _remoteVideoStream = MutableStateFlow<CallStreamFrame?>(null)
  val remoteVideoStream: StateFlow<CallStreamFrame?> = _remoteVideoStream.asStateFlow()

  private val _remoteAudioStream = MutableStateFlow<CallStreamFrame?>(null)
  val remoteAudioStream: StateFlow<CallStreamFrame?> = _remoteAudioStream.asStateFlow()

  private var pendingCallGraceId: String = ""
  private var pendingCallGraceUntilMs: Long = 0L

  private val _socialNotice = MutableStateFlow("")
  val socialNotice: StateFlow<String> = _socialNotice.asStateFlow()

  private val _openDmSheetRequested = MutableStateFlow<String?>(null)
  val openDmSheetRequested: StateFlow<String?> = _openDmSheetRequested.asStateFlow()

  fun requestOpenDmSheet(threadIdOrPeer: String = "") {
    _openDmSheetRequested.value = threadIdOrPeer
  }

  fun clearOpenDmSheetRequest() {
    _openDmSheetRequested.value = null
  }

  private val notifiedDmMessageIds = mutableSetOf<String>()

  private var boundKey: String? = null
  private var boundUid = ""
  private var boundFirebaseAuth = false
  private var boundSupabaseToken = ""
  private var userJob: Job? = null
  private var msgJob: Job? = null
  private var postCommentJob: Job? = null
  private var ytWatchJob: Job? = null
  private var dmMsgJob: Job? = null
  private var callSignalJob: Job? = null
  private var callStreamJob: Job? = null
  private var callSessionDocJob: Job? = null
  private var sleepTimerJob: Job? = null

  private val cloudSyncReady: Boolean
    get() = boundFirebaseAuth || (boundSupabaseToken.isNotBlank() && boundUid.isNotBlank())

  init {
    loadYouTubeFeed("", channelOnly = true)
    loadMusicMixes("Synthwave")
  }

  fun bindUser(
    uid: String?,
    firebaseAuthenticated: Boolean,
    defaultPersona: String,
    supabaseAccessToken: String = "",
  ) {
    val cleanUid = uid.orEmpty().trim()
    val nextKey =
      if (cleanUid.isBlank()) null
      else "$cleanUid:$firebaseAuthenticated:${supabaseAccessToken.hashCode()}"
    boundSupabaseToken = supabaseAccessToken.trim()
    if (nextKey == boundKey) return
    boundKey = nextKey
    boundUid = cleanUid
    boundFirebaseAuth = firebaseAuthenticated
    userJob?.cancel()
    if (cleanUid.isBlank()) {
      ytWatchJob?.cancel()
      dmMsgJob?.cancel()
      callSignalJob?.cancel()
      _savedVideos.value = emptyList()
      _boardPosts.value = emptyList()
      _dmThreads.value = emptyList()
      _bookmarkedIds.value = emptySet()
      _mutedPeerIds.value = emptySet()
      _followedPilotIds.value = emptySet()
      _pinnedDmThreadIds.value = emptySet()
      _callSessions.value = emptyList()
      _callHistory.value = emptyList()
      _incomingCallSession.value = null
      _activeCallSession.value = null
      _callMinimized.value = false
      userJob =
        viewModelScope.launch {
          launch {
            _savedBriefings.value = localVault.loadSavedCopilotBriefings("local_pilot")
          }
          launch {
            chatDao.observeSessions("local_pilot").collect { list ->
              _chatSessions.value = list
              if (_activeSessionId.value.isBlank()) {
                if (list.isNotEmpty()) {
                  selectCopilotSession(list.first().id)
                } else {
                  createCopilotSession("local_pilot", "Primary Tactical Mission", defaultPersona)
                }
              }
            }
          }
        }
      return
    }
    userJob =
      viewModelScope.launch {
        launch {
          _bookmarkedIds.value = localVault.loadBookmarkedPostIds(cleanUid)
          _mutedPeerIds.value = localVault.loadMutedPeerIds(cleanUid)
          _followedPilotIds.value = localVault.loadFollowedPilotIds(cleanUid)
          _pinnedDmThreadIds.value = localVault.loadPinnedDmThreadIds(cleanUid)
          _savedBriefings.value = localVault.loadSavedCopilotBriefings(cleanUid)
        }
        launch {
          chatDao.observeSessions(cleanUid).collect { list ->
            _chatSessions.value = list
            if (_activeSessionId.value.isBlank()) {
              if (list.isNotEmpty()) {
                selectCopilotSession(list.first().id)
              } else {
                createCopilotSession(cleanUid, "Primary Tactical Mission", defaultPersona)
              }
            }
          }
        }
        if (firebaseAuthenticated) {
          launch {
            var initialBoardLoad = true
            communityRepo
              .observeBoardPosts()
              .catch { e ->
                if (e is CancellationException) throw e
                Log.w(TAG, "Board listener error: ${e.message}")
              }
              .collect { incoming ->
                val prevIds = _boardPosts.value.map { it.id }.toSet()
                _boardPosts.value = incoming
                if (initialBoardLoad) {
                  initialBoardLoad = false
                } else {
                  val newPost = incoming.firstOrNull { it.id !in prevIds && it.userId != cleanUid }
                  if (newPost != null && newPost.userId !in _mutedPeerIds.value) {
                    val settings = SettingsStore(ctx).settingsFlow.first()
                    if (settings.socialNotifsEnabled) {
                      ReminderScheduler.postSocialNotification(
                        context = ctx,
                        title = "Board Dispatch: ${newPost.authorCallsign}",
                        body = (newPost.title.ifBlank { newPost.body }).take(150),
                        targetTab = "BOARD",
                      )
                    }
                  }
                }
              }
          }
          launch {
            riderRepo
              .observeSavedVideos(cleanUid)
              .catch { e ->
                if (e is CancellationException) throw e
                Log.w(TAG, "Saved videos listener error: ${e.message}")
              }
              .collect { _savedVideos.value = it }
          }
          launch {
            riderRepo
              .observeFollowedPilotIds(cleanUid)
              .catch { e ->
                if (e is CancellationException) throw e
                Log.w(TAG, "Following listener error: ${e.message}")
              }
              .collect { cloudFollowed ->
                val localFollowed = localVault.loadFollowedPilotIds(cleanUid)
                if (cloudFollowed.isEmpty() && localFollowed.isNotEmpty()) {
                  // Migrate any previously saved local follows into Firestore once
                  localFollowed.forEach { targetUid ->
                    launch {
                      riderRepo.followPilot(
                        userId = cleanUid,
                        myCallsign = "",
                        targetUid = targetUid,
                        targetCallsign = "",
                      )
                    }
                  }
                  _followedPilotIds.value = localFollowed
                } else {
                  _followedPilotIds.value = cloudFollowed
                  localVault.saveFollowedPilotIds(cleanUid, cloudFollowed)
                }
              }
          }
          launch {
            var initialDmLoad = true
            val prevThreadMsgs = mutableMapOf<String, String>()
            communityRepo
              .observeMyDmThreads(cleanUid)
              .catch { e ->
                if (e is CancellationException) throw e
                Log.w(TAG, "DM threads error: ${e.message}")
              }
              .collect { list ->
                _dmThreads.value = list
                if (initialDmLoad) {
                  initialDmLoad = false
                  list.forEach { prevThreadMsgs[it.id] = it.lastMessage }
                } else {
                  for (th in list) {
                    val oldMsg = prevThreadMsgs[th.id]
                    val isFromMe = th.lastSenderId.isNotBlank() && th.lastSenderId == cleanUid
                    if (!isFromMe && oldMsg != null && oldMsg != th.lastMessage && th.lastMessage.isNotBlank()) {
                      val peerIdx = th.participantUids.indexOfFirst { it != cleanUid }
                      val peerCallsign =
                        th.participantCallsigns.getOrNull(peerIdx)?.takeIf { it.isNotBlank() }
                          ?: th.participantCallsigns.firstOrNull { it.isNotBlank() }
                          ?: "Rider"
                      val settings = SettingsStore(ctx).settingsFlow.first()
                      if (settings.dmNotifsEnabled) {
                        ReminderScheduler.postDmNotification(
                          context = ctx,
                          senderCallsign = peerCallsign,
                          body = th.lastMessage.take(150),
                          threadId = th.id,
                        )
                      }
                    }
                    prevThreadMsgs[th.id] = th.lastMessage
                  }
                }
              }
          }
          launch {
            communityRepo
              .observeMyCallSessions(cleanUid)
              .catch { e ->
                if (e is CancellationException) throw e
                Log.w(TAG, "Call sessions error: ${e.message}")
              }
              .collect { liveCalls ->
                reconcileCallSessions(cleanUid, liveCalls)
              }
          }
          launch {
            communityRepo
              .observeRecentCallHistory(cleanUid)
              .catch { e ->
                if (e is CancellationException) throw e
                Log.w(TAG, "Call history error: ${e.message}")
              }
              .collect { _callHistory.value = it }
          }
        } else {
          _savedVideos.value = localVault.loadSavedVideos(cleanUid)
          val token = boundSupabaseToken
          if (token.isNotBlank()) {
            launch {
              var initialSbBoardLoad = true
              var initialSbDmLoad = true
              val prevSbThreadMsgs = mutableMapOf<String, String>()
              while (isActive) {
                LiveCloudServices.fetchSupabaseBoardPosts(token)
                  .onSuccess { incoming ->
                    val prevIds = _boardPosts.value.map { it.id }.toSet()
                    _boardPosts.value = incoming
                    if (initialSbBoardLoad) {
                      initialSbBoardLoad = false
                    } else {
                      val newPost = incoming.firstOrNull { it.id !in prevIds && it.userId != cleanUid }
                      if (newPost != null && newPost.userId !in _mutedPeerIds.value) {
                        val settings = SettingsStore(ctx).settingsFlow.first()
                        if (settings.socialNotifsEnabled) {
                          ReminderScheduler.postSocialNotification(
                            context = ctx,
                            title = "Board Dispatch: ${newPost.authorCallsign}",
                            body = (newPost.title.ifBlank { newPost.body }).take(150),
                            targetTab = "BOARD",
                          )
                        }
                      }
                    }
                  }
                LiveCloudServices.fetchSupabaseDmThreads(token, cleanUid)
                  .onSuccess { list ->
                    _dmThreads.value = list
                    if (initialSbDmLoad) {
                      initialSbDmLoad = false
                      list.forEach { prevSbThreadMsgs[it.id] = it.lastMessage }
                    } else {
                      for (th in list) {
                        val oldMsg = prevSbThreadMsgs[th.id]
                        if (oldMsg != null && oldMsg != th.lastMessage && th.lastMessage.isNotBlank()) {
                          val peerCallsign = th.participantCallsigns.firstOrNull { it.isNotBlank() } ?: "Rider"
                          val settings = SettingsStore(ctx).settingsFlow.first()
                          if (settings.dmNotifsEnabled) {
                            ReminderScheduler.postDmNotification(
                              context = ctx,
                              senderCallsign = peerCallsign,
                              body = th.lastMessage.take(150),
                              threadId = th.id,
                            )
                          }
                        }
                        prevSbThreadMsgs[th.id] = th.lastMessage
                      }
                    }
                  }
                LiveCloudServices.fetchSupabaseCallSessions(token, cleanUid)
                  .onSuccess { allCalls ->
                    _callHistory.value = allCalls.take(30)
                    reconcileCallSessions(cleanUid, allCalls.filter { it.status != "ENDED" })
                  }
                delay(4_000L)
              }
            }
          } else {
            _dmThreads.value = emptyList()
            _callSessions.value = emptyList()
            _callHistory.value = emptyList()
            _incomingCallSession.value = null
            _activeCallSession.value = null
          }
        }
      }
  }

  private fun reconcileCallSessions(uid: String, liveCalls: List<CallSession>) {
    val nowMs = System.currentTimeMillis()
    val freshLiveCalls =
      liveCalls.filter { s ->
        val tsSec = s.updatedAt?.seconds ?: s.createdAt?.seconds ?: 0L
        val ageMs = if (tsSec > 0L) (nowMs - tsSec * 1000L).coerceAtLeast(0L) else 0L
        // Ignore stale unanswered RINGING calls older than 4 minutes so old sessions never ghost-ring
        if (s.status == "RINGING" && ageMs > 240_000L) false else true
      }
    _callSessions.value = freshLiveCalls
    val curActive = _activeCallSession.value
    if (curActive != null) {
      val refreshed = liveCalls.firstOrNull { it.id == curActive.id }
      val inGraceWindow = curActive.id == pendingCallGraceId && nowMs < pendingCallGraceUntilMs
      if (refreshed == null) {
        if (!inGraceWindow) {
          clearActiveCallResources()
        }
      } else {
        if (curActive.id == pendingCallGraceId) {
          pendingCallGraceId = ""
          pendingCallGraceUntilMs = 0L
        }
        if (refreshed != curActive) {
          _activeCallSession.value = refreshed
        }
      }
    }
    val prevIncomingId = _incomingCallSession.value?.id
    val incoming =
      freshLiveCalls.firstOrNull {
        it.calleeUid == uid &&
          it.callerUid != uid &&
          it.status == "RINGING" &&
          it.id != _activeCallSession.value?.id
      }
    _incomingCallSession.value = incoming
    if (incoming != null && incoming.id != prevIncomingId) {
      ReminderScheduler.postSocialNotification(
        context = ctx,
        title = "Incoming ${incoming.callType} Call • ${incoming.callerCallsign.ifBlank { "Pilot" }}",
        body = "Tap to answer live ${incoming.callType.lowercase()} call from ${incoming.callerCallsign.ifBlank { "Pilot" }}",
        targetTab = "RADAR",
      )
    }
  }

  private fun clearActiveCallResources() {
    _activeCallSession.value = null
    _callSignals.value = emptyList()
    _remoteVideoStream.value = null
    _remoteAudioStream.value = null
    pendingCallGraceId = ""
    pendingCallGraceUntilMs = 0L
    callSignalJob?.cancel()
    callStreamJob?.cancel()
    callSessionDocJob?.cancel()
    audio.resetCallAudioMode()
  }

  fun refreshSupabaseBoard() {
    if (boundFirebaseAuth) {
      viewModelScope.launch {
        communityRepo.fetchBoardPostsFromServer()
          .onSuccess { fresh ->
            _boardPosts.value = fresh
            _socialNotice.value = "Live Firestore board synced (${fresh.size} posts)."
          }
          .onFailure { err ->
            _socialNotice.value = err.message ?: "Live Firestore board sync failed."
          }
      }
      return
    }
    if (boundSupabaseToken.isBlank()) return
    viewModelScope.launch {
      LiveCloudServices.fetchSupabaseBoardPosts(boundSupabaseToken)
        .onSuccess { _boardPosts.value = it }
        .onFailure { _socialNotice.value = it.message ?: "Supabase board sync unavailable" }
    }
  }

  fun createCopilotSession(uid: String, title: String, persona: String) {
    cancelCopilotPrompt()
    val sid = "chat_${UUID.randomUUID().toString().take(8)}"
    viewModelScope.launch {
      chatDao.upsertSession(
        CopilotSessionEntity(
          id = sid,
          userId = uid.ifBlank { "local_pilot" },
          title = title.ifBlank { "Mission Briefing" },
          persona = persona,
          updatedAtMs = System.currentTimeMillis(),
        )
      )
      selectCopilotSession(sid)
    }
  }

  private var copilotJob: Job? = null

  fun cancelCopilotPrompt() {
    copilotJob?.cancel()
    copilotJob = null
    _copilotBusy.value = false
    _streamingText.value = ""
    audio.stopSpeaking()
  }

  fun selectCopilotSession(sessionId: String) {
    _activeSessionId.value = sessionId
    msgJob?.cancel()
    msgJob =
      viewModelScope.launch {
        chatDao.observeMessages(sessionId).collect { rows ->
          _messages.value =
            rows.map {
              CopilotMessage(
                id = it.id,
                isUser = it.isUser,
                text = it.text,
                persona = it.persona,
                widgetTag = it.widgetTag,
                timestampMs = it.timestampMs,
              )
            }
        }
      }
  }

  fun deleteCopilotSession(sessionId: String) {
    if (_activeSessionId.value == sessionId) {
      cancelCopilotPrompt()
      msgJob?.cancel()
      msgJob = null
      _activeSessionId.value = ""
      _messages.value = emptyList()
    }
    viewModelScope.launch {
      chatDao.deleteSessionMessages(sessionId)
      chatDao.deleteSession(sessionId)
    }
  }

  fun sendCopilotPrompt(
    uid: String,
    prompt: String,
    bitmap: Bitmap?,
    personaName: String,
    personaPrompt: String,
    telemetryContext: String,
    weatherContext: String,
    useWebSearch: Boolean,
    verbosity: String,
    customDirective: String,
    autoSpeak: Boolean,
    volumePct: Int,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
    temperature: Float = 0.7f,
    thinkingLevel: String = "medium",
    detailLevel: String = "standard",
    voiceProfile: com.example.data.AiVoiceProfile = com.example.data.AiVoiceProfile.WARM_RURAL_PORCH,
  ) {
    val cleanPrompt = prompt.trim()
    if (cleanPrompt.isBlank() && bitmap == null) return
    val effectiveUid = uid.ifBlank { "local_pilot" }
    val userMsg =
      CopilotMessage(
        id = UUID.randomUUID().toString(),
        isUser = true,
        text = cleanPrompt.ifBlank { "Analyze attached PEV photo." },
        persona = personaName,
      )
    copilotJob?.cancel()
    copilotJob =
      viewModelScope.launch {
        var streamingSession: VoiceAudioManager.StreamingTtsBufferSession? = null
        var completedNormally = false
        try {
          val historyBeforeTurn =
            if (_activeSessionId.value.isBlank()) emptyList()
            else _messages.value.toList()
          val sid =
            _activeSessionId.value.ifBlank {
              val newSid = "chat_${UUID.randomUUID().toString().take(8)}"
              chatDao.upsertSession(
                CopilotSessionEntity(
                  id = newSid,
                  userId = effectiveUid,
                  title = userMsg.text.take(32),
                  persona = personaName,
                  updatedAtMs = System.currentTimeMillis(),
                )
              )
              selectCopilotSession(newSid)
              newSid
            }
          chatDao.insertMessage(
            CopilotMessageEntity(
              id = userMsg.id,
              sessionId = sid,
              isUser = true,
              text = userMsg.text,
              persona = personaName,
              widgetTag = null,
              timestampMs = userMsg.timestampMs,
            )
          )
          _copilotBusy.value = true
          _streamingText.value = ""
          val webCtx = if (useWebSearch) LiveCloudServices.webSearchGrounding(userMsg.text) else ""
          var finalAnswer = ""

          val settings = SettingsStore(ctx).settingsFlow.first()
          val activeCustomVoice = settings.getActiveCustomVoice()
          val effectiveVoiceProfile = activeCustomVoice?.resolveEffectiveBase() ?: voiceProfile
          val useStreamingBuffer = autoSpeak && volumePct > 0 && settings.ttsPreloadBufferEnabled

          var sentenceSplitter: VoiceAudioManager.StreamingSentenceSplitter? = null
          var emittedSpeechLen = 0
          val fullWidgetRegex = Regex("\\[WIDGET:\\s*(TELEMETRY|WEATHER|SOS)\\s*]")
          val trailingIncompleteTagRegex = Regex("\\[(?:W(?:I(?:D(?:G(?:E(?:T(?::[^\\]]*)?)?)?)?)?)?)?$")

          if (useStreamingBuffer) {
            val session =
              audio.startStreamingSpeechSession(
                profile = effectiveVoiceProfile,
                customVoice = activeCustomVoice,
                volumePct = volumePct,
                speechRateMult = settings.ttsSpeechRate,
                pitchMult = settings.ttsPitch,
                preferFastLocal = false,
                bufferDepth = settings.ttsPreloadBufferDepth,
              )
            streamingSession = session
            sentenceSplitter =
              VoiceAudioManager.StreamingSentenceSplitter { sentence ->
                val cleanSentence =
                  sentence
                    .replace(fullWidgetRegex, "")
                    .trim()
                if (cleanSentence.isNotBlank()) {
                  session.enqueueTextFragment(cleanSentence)
                }
              }
          }

          aiRepo
            .streamCopilotResponse(
              history = historyBeforeTurn.filterNot { it.id == userMsg.id },
              prompt = userMsg.text,
              bitmap = bitmap,
              personaPrompt = personaPrompt,
              telemetryContext = telemetryContext,
              weatherContext = weatherContext,
              webContext = webCtx,
              verbosity = verbosity,
              customDirective = customDirective,
              modelId = modelId,
              temperature = temperature,
              thinkingLevel = thinkingLevel,
              detailLevel = detailLevel,
            )
            .catch { e ->
              if (e is kotlinx.coroutines.CancellationException) {
                streamingSession?.cancel()
                throw e
              }
              streamingSession?.cancel()
              streamingSession = null
              finalAnswer = "AI Co-Pilot Error: ${e.message ?: "Request failed."}"
            }
            .collect { partial ->
              finalAnswer = partial
              val strippedFull = partial.replace(fullWidgetRegex, "")
              val safeDisplay = strippedFull.replace(trailingIncompleteTagRegex, "").trimEnd()
              _streamingText.value = safeDisplay
              if (useStreamingBuffer && sentenceSplitter != null && safeDisplay.length > emittedSpeechLen) {
                val delta = safeDisplay.substring(emittedSpeechLen)
                emittedSpeechLen = safeDisplay.length
                sentenceSplitter.feed(delta)
              }
            }

          val widget =
            when {
              finalAnswer.contains("[WIDGET: TELEMETRY]") -> "TELEMETRY"
              finalAnswer.contains("[WIDGET: WEATHER]") -> "WEATHER"
              finalAnswer.contains("[WIDGET: SOS]") -> "SOS"
              else -> null
            }
          val cleaned =
            finalAnswer
              .replace(fullWidgetRegex, "")
              .trim()
              .ifBlank { "No response received." }

          chatDao.insertMessage(
            CopilotMessageEntity(
              id = UUID.randomUUID().toString(),
              sessionId = sid,
              isUser = false,
              text = cleaned,
              persona = personaName,
              widgetTag = widget,
              timestampMs = System.currentTimeMillis(),
            )
          )
          chatDao.upsertSession(
            CopilotSessionEntity(
              id = sid,
              userId = effectiveUid,
              title = userMsg.text.take(32),
              persona = personaName,
              updatedAtMs = System.currentTimeMillis(),
            )
          )

          if (useStreamingBuffer && streamingSession != null) {
            if (cleaned.length > emittedSpeechLen) {
              sentenceSplitter?.feed(cleaned.substring(emittedSpeechLen))
            }
            sentenceSplitter?.flush()
            streamingSession?.finish()
          } else if (autoSpeak && volumePct > 0) {
            audio.speakWithActiveSettings(cleaned, settings, volumePct)
          }

          if (settings.aiNotifsEnabled) {
            ReminderScheduler.postAiNotification(
              context = ctx,
              title = "AI Co-Pilot: $personaName",
              body = cleaned.take(150),
              targetTab = "COPILOT",
            )
          }
          completedNormally = true
        } finally {
          if (!completedNormally) {
            streamingSession?.cancel()
          }
          _streamingText.value = ""
          _copilotBusy.value = false
        }
      }
  }

  // YOUTUBE FEED & TRAIL MUSIC v3 + PERSISTENT IN-APP MINI-PLAYER
  private var youtubeLoadJob: Job? = null
  private var musicLoadJob: Job? = null

  fun loadYouTubeFeed(query: String, channelOnly: Boolean) {
    youtubeLoadJob?.cancel()
    youtubeLoadJob =
      viewModelScope.launch {
        _youtubeState.value = UiState.Loading
        val res = LiveCloudServices.fetchYouTubeVideos(query, channelOnly)
        _youtubeState.value =
          res.fold(
            onSuccess = { UiState.Success(it) },
            onFailure = { UiState.Error(it.message ?: "YouTube feed unavailable") },
          )
      }
  }

  fun loadMusicMixes(category: String, customQuery: String = "") {
    _activeMusicCategory.value = category
    val cleanCustom = customQuery.trim()
    if (cleanCustom.isNotBlank()) {
      _recentMusicQueries.value =
        (listOf(cleanCustom) + _recentMusicQueries.value.filterNot { it.equals(cleanCustom, ignoreCase = true) }).take(8)
    }
    musicLoadJob?.cancel()
    musicLoadJob =
      viewModelScope.launch {
        _musicMixesState.value = UiState.Loading
        val res = LiveCloudServices.fetchYouTubeMusicMixes(category, cleanCustom)
        _musicMixesState.value =
          res.fold(
            onSuccess = { UiState.Success(it) },
            onFailure = { UiState.Error(it.message ?: "YouTube Music unavailable") },
          )
      }
  }

  fun launchAutoDjPevMix(moodQuery: String = "Cyberpunk Synthwave Phonk Driving Mix") {
    _activeMusicCategory.value = "Auto-DJ"
    musicLoadJob?.cancel()
    musicLoadJob =
      viewModelScope.launch {
        _musicMixesState.value = UiState.Loading
        val res = LiveCloudServices.fetchYouTubeMusicMixes("Auto-DJ", moodQuery)
        res.fold(
          onSuccess = { pair ->
            _musicMixesState.value = UiState.Success(pair)
            val tracks = pair.first.shuffled()
            if (tracks.isNotEmpty()) {
              _playerShuffle.value = true
              playInMiniPlayer(tracks.first(), tracks, expandTheater = false)
            }
          },
          onFailure = { _musicMixesState.value = UiState.Error(it.message ?: "Auto-DJ unavailable") },
        )
      }
  }

  fun enqueuePlayNext(video: YouTubeVideoItem) {
    val cur = _activeVideo.value
    if (cur == null) {
      playInMiniPlayer(video, listOf(video), expandTheater = false)
      return
    }
    val q = _playerQueue.value.filterNot { it.videoId == video.videoId }.toMutableList()
    val curIdx = q.indexOfFirst { it.videoId == cur.videoId }
    if (curIdx >= 0) q.add(curIdx + 1, video) else q.add(video)
    _playerQueue.value = q
    _socialNotice.value = "Queued next: ${video.title.take(36)}"
  }

  fun playInMiniPlayer(
    video: YouTubeVideoItem,
    queue: List<YouTubeVideoItem> = emptyList(),
    expandTheater: Boolean = false,
  ) {
    _playerError.value = ""
    _playerQueue.value = if (queue.isNotEmpty()) queue else listOf(video)
    _activeVideo.value = video
    _recentlyPlayedVideos.value =
      (listOf(video) + _recentlyPlayedVideos.value.filterNot { it.videoId == video.videoId }).take(25)
    _playerPlaying.value = true
    _playerExpanded.value = expandTheater
    watchYouTubeVideo(video.videoId)
  }

  fun togglePlayerExpanded(expanded: Boolean) {
    _playerExpanded.value = expanded
  }

  fun togglePlayerPlaying() {
    _playerPlaying.value = !_playerPlaying.value
  }

  fun setPlayerPlayingFromWeb(playing: Boolean) {
    _playerPlaying.value = playing
  }

  fun togglePlayerMuted() {
    _playerMuted.value = !_playerMuted.value
  }

  fun togglePlayerShuffle() {
    _playerShuffle.value = !_playerShuffle.value
  }

  fun cyclePlayerRepeatMode() {
    _playerRepeatMode.value =
      when (_playerRepeatMode.value) {
        "off" -> "all"
        "all" -> "one"
        else -> "off"
      }
  }

  fun cyclePlayerSpeed() {
    _playerSpeed.value =
      when (_playerSpeed.value) {
        0.75f -> 1.0f
        1.0f -> 1.25f
        1.25f -> 1.5f
        1.5f -> 2.0f
        else -> 0.75f
      }
  }

  fun removeFromQueue(videoId: String) {
    val remaining = _playerQueue.value.filterNot { it.videoId == videoId }
    _playerQueue.value = remaining
    if (_activeVideo.value?.videoId == videoId) {
      if (remaining.isNotEmpty()) {
        playInMiniPlayer(remaining.first(), remaining, _playerExpanded.value)
      } else {
        closeMiniPlayer()
      }
    }
  }

  fun clearQueue() {
    val cur = _activeVideo.value
    _playerQueue.value = if (cur != null) listOf(cur) else emptyList()
  }

  fun setSleepTimerMinutes(minutes: Int) {
    sleepTimerJob?.cancel()
    if (minutes <= 0) {
      _sleepTimerRemainingSec.value = 0
      return
    }
    _sleepTimerRemainingSec.value = minutes * 60
    sleepTimerJob =
      viewModelScope.launch {
        while (_sleepTimerRemainingSec.value > 0) {
          delay(1000L)
          _sleepTimerRemainingSec.value = (_sleepTimerRemainingSec.value - 1).coerceAtLeast(0)
        }
        _playerPlaying.value = false
      }
  }

  fun playNextInQueue(fromAutoEnded: Boolean = false) {
    val q = _playerQueue.value
    val cur = _activeVideo.value ?: return
    if (fromAutoEnded && _playerRepeatMode.value == "one") {
      playInMiniPlayer(cur, q, _playerExpanded.value)
      return
    }
    if (q.size <= 1) {
      if (fromAutoEnded && _playerRepeatMode.value == "all") {
        playInMiniPlayer(cur, q, _playerExpanded.value)
      }
      return
    }
    val nextIdx =
      if (_playerShuffle.value) {
        val candidates = q.indices.filter { q[it].videoId != cur.videoId }
        candidates.randomOrNull() ?: 0
      } else {
        val idx = q.indexOfFirst { it.videoId == cur.videoId }
        if (idx < 0 || idx + 1 >= q.size) 0 else idx + 1
      }
    playInMiniPlayer(q[nextIdx], q, _playerExpanded.value)
  }

  fun playPrevInQueue() {
    val q = _playerQueue.value
    val cur = _activeVideo.value ?: return
    if (q.size <= 1) return
    val idx = q.indexOfFirst { it.videoId == cur.videoId }
    val prevIdx = if (idx <= 0) q.lastIndex else idx - 1
    playInMiniPlayer(q[prevIdx], q, _playerExpanded.value)
  }

  fun setPlayerError(msg: String) {
    _playerError.value = msg
  }

  fun closeMiniPlayer() {
    _playerPlaying.value = false
    _playerExpanded.value = false
    _activeVideo.value = null
    _playerError.value = ""
  }

  fun watchYouTubeVideo(videoId: String) {
    ytWatchJob?.cancel()
    _videoAiAnswer.value = ""
    if (!cloudSyncReady) {
      _videoVolts.value = 0L
      _videoComments.value = emptyList()
      return
    }
    if (boundFirebaseAuth) {
      ytWatchJob =
        viewModelScope.launch {
          launch {
            communityRepo
              .observeYoutubeEngagement(videoId)
              .catch { e ->
                if (e is CancellationException) throw e
                Log.w(TAG, "YT volts error: ${e.message}")
              }
              .collect { _videoVolts.value = it }
          }
          launch {
            communityRepo
              .observeYoutubeComments(videoId)
              .catch { e ->
                if (e is CancellationException) throw e
                Log.w(TAG, "YT comments error: ${e.message}")
              }
              .collect { _videoComments.value = it }
          }
        }
    } else {
      val token = boundSupabaseToken
      ytWatchJob =
        viewModelScope.launch {
          while (isActive) {
            LiveCloudServices.fetchSupabaseYoutubeEngagement(token, videoId)
              .onSuccess { _videoVolts.value = it }
            LiveCloudServices.fetchSupabaseYoutubeComments(token, videoId)
              .onSuccess { _videoComments.value = it }
            delay(3_500L)
          }
        }
    }
  }

  fun toggleSaveVideo(uid: String, video: YouTubeVideoItem, note: String = "", tag: String = "PEV Tech") {
    viewModelScope.launch {
      val exists = _savedVideos.value.any { it.videoId == video.videoId }
      if (boundFirebaseAuth) {
        if (exists && note.isBlank()) {
          riderRepo.removeSavedVideo(uid, video.videoId)
        } else {
          riderRepo.saveVideoToVault(uid, video, tag, note)
        }
      } else {
        val next =
          if (exists && note.isBlank()) {
            _savedVideos.value.filterNot { it.videoId == video.videoId }
          } else {
            val item =
              SavedVideo(
                id = video.videoId,
                userId = uid,
                videoId = video.videoId,
                title = video.title,
                channelTitle = video.channelTitle,
                thumbnailUrl = video.thumbnailUrl,
                publishedAt = video.publishedAt,
                categoryTag = tag,
                personalNote = note,
              )
            listOf(item) + _savedVideos.value.filterNot { it.videoId == video.videoId }
          }
        localVault.saveSavedVideos(uid, next)
        _savedVideos.value = next
      }
    }
  }

  fun updateSavedVideoNote(uid: String, saved: SavedVideo, tag: String, note: String) {
    toggleSaveVideo(
      uid,
      YouTubeVideoItem(
        videoId = saved.videoId,
        title = saved.title,
        description = "",
        channelTitle = saved.channelTitle,
        publishedAt = saved.publishedAt,
        thumbnailUrl = saved.thumbnailUrl,
      ),
      note = note,
      tag = tag,
    )
  }

  fun voltVideo(videoId: String) {
    if (!cloudSyncReady) {
      _socialNotice.value = "Sign in to sync Volts to live cloud."
      return
    }
    viewModelScope.launch {
      if (boundFirebaseAuth) {
        communityRepo.voltYoutubeVideo(videoId)
      } else {
        LiveCloudServices.voltSupabaseYoutubeVideo(boundSupabaseToken, videoId)
          .onSuccess { _videoVolts.value = it }
          .onFailure { _socialNotice.value = it.message ?: "Could not sync Volt" }
      }
    }
  }

  fun postVideoComment(videoId: String, callsign: String, text: String) {
    if (text.isBlank()) return
    if (!cloudSyncReady) {
      _socialNotice.value = "Sign in to post live video comments."
      return
    }
    viewModelScope.launch {
      val moderation =
        FamilySafetyGuard.moderateSubmission(
          fields = listOf(text),
          contextLabel = "video comment",
        )
      if (!moderation.allowed) {
        _socialNotice.value = moderation.reason
        return@launch
      }
      if (boundFirebaseAuth) {
        communityRepo.commentYoutubeVideo(videoId, callsign, text)
      } else {
        LiveCloudServices.insertSupabaseYoutubeComment(boundSupabaseToken, videoId, boundUid, callsign, text)
          .onSuccess {
            val added =
              YoutubeComment(
                id = "yc_${UUID.randomUUID().toString().take(8)}",
                userId = boundUid,
                authorCallsign = callsign,
                text = text.trim(),
              )
            _videoComments.value = listOf(added) + _videoComments.value
          }
          .onFailure { _socialNotice.value = it.message ?: "Video comment sync failed" }
      }
    }
  }

  fun askVideoAi(
    video: YouTubeVideoItem,
    question: String,
    persona: String,
    detail: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
  ) {
    viewModelScope.launch {
      _videoAiBusy.value = true
      val res = aiRepo.interrogateVideo(video.title, video.description, question, persona, detail, modelId)
      _videoAiAnswer.value = res.getOrElse { "AI Error: ${it.message}" }
      _videoAiBusy.value = false
    }
  }

  fun submitVideoRequestToBoard(callsign: String, badge: String, topic: String, details: String) {
    if (!cloudSyncReady) {
      _socialNotice.value = "Sign in to publish requests to Community Board."
      return
    }
    createPost(
      callsign = callsign,
      badge = badge,
      category = "VIDEO_REQUEST",
      title = "Video Request: $topic",
      body = details,
      bitmap = null,
      pollQuestion = "",
      pollOptions = emptyList(),
      priceUsd = 0.0,
      location = "",
      telemetrySummary = "",
      helpNeeded = false,
      speedTrap = false,
      stolenAlert = false,
      onDone = { _socialNotice.value = "Video request posted to Community Board." },
    )
  }

  // BOARD OPERATIONS
  fun toggleBookmark(uid: String, postId: String) {
    if (uid.isBlank()) return
    viewModelScope.launch {
      _bookmarkedIds.value = localVault.toggleBookmarkedPostId(uid, postId)
    }
  }

  fun toggleMutePeer(uid: String, peerUid: String) {
    if (uid.isBlank() || peerUid.isBlank()) return
    viewModelScope.launch {
      _mutedPeerIds.value = localVault.toggleMutedPeerId(uid, peerUid)
    }
  }

  fun toggleFollowPilot(
    uid: String,
    targetUid: String,
    myCallsign: String = "",
    targetCallsign: String = "",
  ) {
    if (uid.isBlank() || targetUid.isBlank() || uid == targetUid) return
    val currentlyFollowing = targetUid in _followedPilotIds.value
    val optimistic =
      if (currentlyFollowing) _followedPilotIds.value - targetUid
      else _followedPilotIds.value + targetUid
    _followedPilotIds.value = optimistic
    viewModelScope.launch {
      localVault.saveFollowedPilotIds(uid, optimistic)
      if (boundFirebaseAuth) {
        if (currentlyFollowing) {
          riderRepo.unfollowPilot(uid, targetUid)
            .onSuccess { _socialNotice.value = "Unfollowed pilot." }
            .onFailure { _socialNotice.value = it.message ?: "Unfollow sync pending." }
        } else {
          riderRepo.followPilot(uid, myCallsign, targetUid, targetCallsign)
            .onSuccess {
              val label = targetCallsign.ifBlank { "pilot" }
              _socialNotice.value = "Following @$label • Friend request sent!"
            }
            .onFailure { _socialNotice.value = it.message ?: "Follow sync pending." }
        }
      }
    }
  }

  fun togglePinDmThread(uid: String, threadId: String) {
    if (uid.isBlank() || threadId.isBlank()) return
    viewModelScope.launch {
      _pinnedDmThreadIds.value = localVault.togglePinnedDmThreadId(uid, threadId)
    }
  }

  fun saveCopilotBriefing(uid: String, title: String, persona: String, content: String) {
    if (uid.isBlank() || content.isBlank()) return
    viewModelScope.launch {
      val item =
        SavedCopilotBriefing(
          id = "br_${UUID.randomUUID().toString().take(8)}",
          title = title.trim().take(80).ifBlank { "Co-Pilot Briefing" },
          persona = persona.ifBlank { "Tactical Co-Pilot" },
          content = content.trim(),
          savedAtMs = System.currentTimeMillis(),
        )
      val next = listOf(item) + _savedBriefings.value.filterNot { it.content == item.content }
      localVault.saveCopilotBriefings(uid, next)
      _savedBriefings.value = next
      _socialNotice.value = "Saved to Co-Pilot Briefings Vault."
    }
  }

  fun deleteCopilotBriefing(uid: String, briefingId: String) {
    if (uid.isBlank()) return
    viewModelScope.launch {
      val next = _savedBriefings.value.filterNot { it.id == briefingId }
      localVault.saveCopilotBriefings(uid, next)
      _savedBriefings.value = next
    }
  }

  fun editBoardPost(postId: String, title: String, body: String, onDone: () -> Unit = {}) {
    if (!cloudSyncReady || postId.isBlank()) return
    viewModelScope.launch {
      if (boundFirebaseAuth) {
        communityRepo
          .updateBoardPost(postId, title, body)
          .onSuccess {
            _socialNotice.value = "Transmission updated live."
            onDone()
          }
          .onFailure { _socialNotice.value = it.message ?: "Unable to edit post" }
      } else {
        LiveCloudServices.updateSupabaseBoardPost(boundSupabaseToken, postId, title, body)
          .onSuccess {
            _boardPosts.value =
              _boardPosts.value.map {
                if (it.id == postId) it.copy(title = title.trim(), body = body.trim()) else it
              }
            _socialNotice.value = "Transmission updated live."
            onDone()
          }
          .onFailure { _socialNotice.value = it.message ?: "Unable to edit post" }
      }
    }
  }

  fun repostToBoard(
    original: BoardPost,
    myCallsign: String,
    myBadge: String,
    commentary: String = "",
  ) {
    val boostBody =
      buildString {
        if (commentary.isNotBlank()) {
          append(commentary.trim())
          append("\n\n")
        }
        append("BOOSTED FROM @${original.authorCallsign}: ${original.title}\n")
        append(original.body.take(1800))
      }
    createPost(
      callsign = myCallsign,
      badge = myBadge,
      category = original.category.ifBlank { "UNIVERSAL" },
      title = "Boost: ${original.title}".take(115),
      body = boostBody,
      bitmap = null,
      pollQuestion = "",
      pollOptions = emptyList(),
      priceUsd = original.priceUsd,
      location = original.locationLabel,
      telemetrySummary = original.telemetrySummary,
      helpNeeded = original.helpNeeded,
      speedTrap = original.speedTrap,
      stolenAlert = original.stolenAlert,
      onDone = { _socialNotice.value = "Boosted live to Community Board." },
    )
  }

  fun suggestDmReplies(
    lastIncomingMessage: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
    onResult: (List<String>) -> Unit,
  ) {
    if (lastIncomingMessage.isBlank()) {
      onResult(listOf("Copy that — rolling out now.", "Give me 5 min to check voltage.", "Send your live pin!"))
      return
    }
    viewModelScope.launch {
      aiRepo
        .translateOrSummarize(
          lastIncomingMessage,
          "Generate exactly 3 ultra-short PEV rider DM quick replies (under 8 words each), separated by | with no bullets or numbering.",
          modelId,
        )
        .onSuccess { raw ->
          val parsed =
            raw
              .split("|", "\n")
              .map { it.trim().trimStart('-', '*', '1', '2', '3', '.', ' ') }
              .filter { it.isNotBlank() }
              .take(3)
          onResult(
            if (parsed.isNotEmpty()) parsed
            else listOf("Copy that — on my way.", "Checking battery % now.", "Drop your live radar pin.")
          )
        }
        .onFailure {
          onResult(listOf("Copy that — rolling out.", "Need 5 mins at charger.", "Share live GPS pin!"))
        }
    }
  }

  fun generateSocialCaptions(
    topic: String,
    platform: String,
    telemetrySummary: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
    onResult: (String) -> Unit,
  ) {
    viewModelScope.launch {
      val prompt =
        "Write an energetic, authentic PEV rider social post for $platform about: $topic. " +
          (if (telemetrySummary.isNotBlank()) "Include this live telemetry: $telemetrySummary. " else "") +
          "Add 5 relevant PEV hashtags at the end."
      aiRepo
        .polishText(prompt, "Hype", modelId)
        .onSuccess(onResult)
        .onFailure { _socialNotice.value = it.message ?: "Caption studio unavailable" }
    }
  }

  fun runSosAiTriage(post: BoardPost, modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE) {
    viewModelScope.launch {
      _sosTriageBusyId.value = post.id
      aiRepo
        .diagnoseBoardSos(post.title, post.body, post.telemetrySummary, modelId)
        .onSuccess { advice -> _sosTriageMap.value = _sosTriageMap.value + (post.id to advice) }
        .onFailure { _socialNotice.value = it.message ?: "SOS triage unavailable" }
      _sosTriageBusyId.value = null
    }
  }

  fun openPostComments(postId: String) {
    postCommentJob?.cancel()
    if (boundFirebaseAuth) {
      postCommentJob =
        viewModelScope.launch {
          communityRepo
            .observePostComments(postId)
            .catch { e ->
              if (e is CancellationException) throw e
              Log.w(TAG, "Post comments error: ${e.message}")
            }
            .collect { _selectedPostComments.value = it }
        }
    } else if (boundSupabaseToken.isNotBlank()) {
      postCommentJob =
        viewModelScope.launch {
          LiveCloudServices.fetchSupabaseBoardComments(boundSupabaseToken, postId)
            .onSuccess { _selectedPostComments.value = it }
            .onFailure { _selectedPostComments.value = emptyList() }
        }
    }
  }

  fun createPost(
    callsign: String,
    badge: String,
    category: String,
    title: String,
    body: String,
    bitmap: Bitmap?,
    pollQuestion: String,
    pollOptions: List<String>,
    priceUsd: Double,
    location: String,
    telemetrySummary: String,
    helpNeeded: Boolean,
    speedTrap: Boolean,
    stolenAlert: Boolean,
    externalImageUrl: String = "",
    onDone: () -> Unit,
  ) {
    viewModelScope.launch {
      val moderation =
        FamilySafetyGuard.moderateSubmission(
          fields = listOf(title, body, pollQuestion, location) + pollOptions,
          bitmap = bitmap,
          externalUrl = externalImageUrl,
          contextLabel = "community post",
        )
      if (!moderation.allowed) {
        _socialNotice.value = moderation.reason
        return@launch
      }
      var uploadedUrl = externalImageUrl.trim().takeIf { it.startsWith("http://") || it.startsWith("https://") }.orEmpty()
      if (bitmap != null) {
        uploadedUrl = bitmapToCloudBase64DataUri(bitmap, maxDimension = 560, initialQuality = 65, maxChars = 155_000)
      }
      val formattedPoll = pollOptions.filter { it.isNotBlank() }.map { "$it|0" }
      if (boundFirebaseAuth) {
        communityRepo.createBoardPost(
          authorCallsign = callsign,
          authorBadge = badge,
          category = category,
          title = title,
          body = body,
          imageUrl = uploadedUrl,
          pollQuestion = pollQuestion,
          pollOptions = formattedPoll,
          priceUsd = priceUsd,
          locationLabel = location,
          telemetrySummary = telemetrySummary,
          helpNeeded = helpNeeded,
          speedTrap = speedTrap,
          stolenAlert = stolenAlert,
        )
      } else if (boundSupabaseToken.isNotBlank()) {
        val sbPost =
          BoardPost(
            id = "sb_${UUID.randomUUID().toString().take(8)}",
            userId = boundUid,
            authorCallsign = callsign,
            authorBadge = badge,
            category = category,
            title = title,
            body = body,
            imageUrl = uploadedUrl,
            pollQuestion = pollQuestion,
            pollOptions = formattedPoll,
            priceUsd = priceUsd,
            locationLabel = location,
            telemetrySummary = telemetrySummary,
            helpNeeded = helpNeeded,
            speedTrap = speedTrap,
            stolenAlert = stolenAlert,
          )
        LiveCloudServices.insertSupabaseBoardPost(boundSupabaseToken, sbPost)
          .onSuccess { _boardPosts.value = listOf(sbPost) + _boardPosts.value }
          .onFailure { _socialNotice.value = it.message ?: "Live Board sync unavailable right now." }
      }
      onDone()
    }
  }

  fun polishPostDraft(
    draft: String,
    tone: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
    onResult: (String) -> Unit,
  ) {
    viewModelScope.launch {
      aiRepo.polishText(draft, tone, modelId).onSuccess(onResult).onFailure {
        _socialNotice.value = it.message ?: "AI Polish error"
      }
    }
  }

  fun translateText(
    text: String,
    targetLang: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
    onResult: (String) -> Unit,
  ) {
    viewModelScope.launch {
      aiRepo.translateOrSummarize(text, "Translate concisely into $targetLang", modelId)
        .onSuccess(onResult)
        .onFailure { _socialNotice.value = it.message ?: "Translation failed" }
    }
  }

  fun toggleLikePost(postId: String) {
    viewModelScope.launch {
      if (boundFirebaseAuth) {
        communityRepo.toggleLikePost(postId)
      } else if (boundSupabaseToken.isNotBlank()) {
        val cur = _boardPosts.value.firstOrNull { it.id == postId } ?: return@launch
        val next = cur.likesCount + 1L
        LiveCloudServices.updateSupabaseBoardLikes(boundSupabaseToken, postId, next)
          .onSuccess {
            _boardPosts.value =
              _boardPosts.value.map { if (it.id == postId) it.copy(likesCount = next) else it }
          }
          .onFailure { _socialNotice.value = it.message ?: "Supabase Volt update failed" }
      }
    }
  }

  fun votePoll(postId: String, idx: Int, options: List<String>) {
    if (!cloudSyncReady) {
      _socialNotice.value = "Sign in to vote in live polls."
      return
    }
    viewModelScope.launch {
      if (boundFirebaseAuth) {
        communityRepo.votePollOption(postId, idx, options)
      } else {
        if (idx !in options.indices) return@launch
        val nextOptions =
          options.mapIndexed { i, item ->
            val parts = item.split("|")
            val label = parts.firstOrNull().orEmpty()
            val votes = parts.getOrNull(1)?.toLongOrNull() ?: 0L
            if (i == idx) "$label|${votes + 1L}" else item
          }
        LiveCloudServices.voteSupabaseBoardPoll(boundSupabaseToken, postId, nextOptions)
          .onSuccess {
            _boardPosts.value =
              _boardPosts.value.map { if (it.id == postId) it.copy(pollOptions = nextOptions) else it }
          }
          .onFailure { _socialNotice.value = it.message ?: "Poll vote failed" }
      }
    }
  }

  fun commentOnPost(postId: String, callsign: String, body: String) {
    if (body.isBlank()) return
    viewModelScope.launch {
      val moderation =
        FamilySafetyGuard.moderateSubmission(
          fields = listOf(body),
          contextLabel = "comment",
        )
      if (!moderation.allowed) {
        _socialNotice.value = moderation.reason
        return@launch
      }
      if (boundFirebaseAuth) {
        communityRepo.addPostComment(postId, callsign, body)
      } else if (boundSupabaseToken.isNotBlank()) {
        LiveCloudServices.insertSupabaseBoardComment(boundSupabaseToken, postId, boundUid, callsign, body)
          .onSuccess {
            _selectedPostComments.value =
              _selectedPostComments.value +
                BoardComment(
                  id = "sbc_${UUID.randomUUID().toString().take(6)}",
                  userId = boundUid,
                  authorCallsign = callsign,
                  body = body,
                )
          }
          .onFailure { _socialNotice.value = it.message ?: "Supabase comment failed" }
      }
    }
  }

  fun flagPost(postId: String) {
    if (!cloudSyncReady) return
    _reportedPostIds.value = _reportedPostIds.value + postId
    viewModelScope.launch {
      if (boundFirebaseAuth) {
        communityRepo.flagPost(postId)
        _socialNotice.value = "Post reported & hidden from your feed. Thank you for keeping Rural ERides GO family-friendly!"
      } else {
        val cur = _boardPosts.value.firstOrNull { it.id == postId } ?: return@launch
        val next = cur.flaggedCount + 1L
        LiveCloudServices.flagSupabaseBoardPost(boundSupabaseToken, postId, next)
          .onSuccess {
            _boardPosts.value =
              _boardPosts.value.map { if (it.id == postId) it.copy(flaggedCount = next) else it }
            _socialNotice.value = "Post reported & hidden from your feed. Thank you for keeping Rural ERides GO family-friendly!"
          }
      }
    }
  }

  fun deletePost(postId: String) {
    viewModelScope.launch {
      if (boundFirebaseAuth) {
        communityRepo.deletePost(postId)
      } else if (boundSupabaseToken.isNotBlank()) {
        LiveCloudServices.deleteSupabaseBoardPost(boundSupabaseToken, postId)
          .onSuccess { _boardPosts.value = _boardPosts.value.filterNot { it.id == postId } }
          .onFailure { _socialNotice.value = it.message ?: "Supabase delete failed" }
      }
    }
  }

  // DIRECT MESSAGES & CALL SIGNALING
  fun openDmThread(threadId: String, myUid: String) {
    if (!cloudSyncReady) return
    dmMsgJob?.cancel()
    if (boundFirebaseAuth) {
      dmMsgJob =
        viewModelScope.launch {
          communityRepo
            .observeDmMessages(threadId, myUid)
            .catch { e ->
              if (e is CancellationException) throw e
              Log.w(TAG, "DM messages error: ${e.message}")
            }
            .collect { list ->
              val nowSec = System.currentTimeMillis() / 1000L
              val visible =
                list.filter { msg ->
                  val exp = msg.selfDestructSec
                  val created = msg.createdAt?.seconds ?: nowSec
                  if (exp > 0L && nowSec - created > exp) {
                    viewModelScope.launch { communityRepo.deleteDmMessage(threadId, msg.id) }
                    false
                  } else true
                }
              _dmMessages.value = visible
              visible.forEach { notifiedDmMessageIds.add(it.id) }
            }
        }
    } else {
      val token = boundSupabaseToken
      dmMsgJob =
        viewModelScope.launch {
          while (isActive) {
            LiveCloudServices.fetchSupabaseDmMessages(token, threadId)
              .onSuccess { list ->
                val nowSec = System.currentTimeMillis() / 1000L
                val visible =
                  list.filter { msg ->
                    val exp = msg.selfDestructSec
                    val created = msg.createdAt?.seconds ?: nowSec
                    if (exp > 0L && nowSec - created > exp) {
                      viewModelScope.launch { LiveCloudServices.deleteSupabaseDmMessage(token, msg.id) }
                      false
                    } else true
                  }
                _dmMessages.value = visible
                visible.forEach { notifiedDmMessageIds.add(it.id) }
              }
            delay(3_000L)
          }
        }
    }
  }

  fun sendDm(
    myCallsign: String,
    peerUid: String,
    peerCallsign: String,
    text: String,
    bitmap: Bitmap? = null,
    voiceBase64: String = "",
    selfDestructSec: Long = 0L,
    onThreadReady: (String) -> Unit = {},
  ) {
    if (!cloudSyncReady) return
    viewModelScope.launch {
      val moderation =
        FamilySafetyGuard.moderateSubmission(
          fields = listOf(text),
          bitmap = bitmap,
          contextLabel = "direct message",
          runDeepAiTextCheck = text.trim().length >= 16,
        )
      if (!moderation.allowed) {
        _socialNotice.value = moderation.reason
        return@launch
      }
      var imgUrl = ""
      if (bitmap != null) {
        imgUrl = bitmapToCloudBase64DataUri(bitmap, maxDimension = 560, initialQuality = 65, maxChars = 155_000)
      }
      if (boundFirebaseAuth) {
        communityRepo
          .sendDirectMessage(myCallsign, peerUid, peerCallsign, text, imgUrl, voiceBase64, selfDestructSec)
          .onSuccess(onThreadReady)
      } else {
        LiveCloudServices.sendSupabaseDm(
          accessToken = boundSupabaseToken,
          myUid = boundUid,
          myCallsign = myCallsign,
          peerUid = peerUid,
          peerCallsign = peerCallsign,
          text = text,
          imageUrl = imgUrl,
          voiceBase64 = voiceBase64,
          selfDestructSec = selfDestructSec,
        )
          .onSuccess { tid ->
            LiveCloudServices.fetchSupabaseDmThreads(boundSupabaseToken, boundUid)
              .onSuccess { _dmThreads.value = it }
            LiveCloudServices.fetchSupabaseDmMessages(boundSupabaseToken, tid)
              .onSuccess { _dmMessages.value = it }
            onThreadReady(tid)
          }
          .onFailure { _socialNotice.value = it.message ?: "DM send failed" }
      }
    }
  }

  fun deleteDmMessage(threadId: String, msgId: String) {
    if (!cloudSyncReady) return
    viewModelScope.launch {
      if (boundFirebaseAuth) {
        communityRepo.deleteDmMessage(threadId, msgId)
      } else {
        LiveCloudServices.deleteSupabaseDmMessage(boundSupabaseToken, msgId).onSuccess {
          _dmMessages.value = _dmMessages.value.filterNot { it.id == msgId }
        }
      }
    }
  }

  // LIVE WEBRTC AUDIO & VIDEO CALLING
  fun startOutgoingCall(
    myUid: String,
    myCallsign: String,
    peerUid: String,
    peerCallsign: String,
    callType: String,
  ) {
    if (!cloudSyncReady) {
      _socialNotice.value = "Sign in to place live P2P Audio/Video calls."
      return
    }
    if (peerUid.isBlank() || peerUid == myUid) {
      _socialNotice.value = "Select another pilot to call."
      return
    }
    val cleanType = if (callType == "VIDEO") "VIDEO" else "AUDIO"
    _callMinimized.value = false
    viewModelScope.launch {
      if (boundFirebaseAuth) {
        communityRepo
          .startCallSession(myCallsign, peerUid, peerCallsign, cleanType)
          .onSuccess { sid ->
            val localSession =
              CallSession(
                id = sid,
                callerUid = myUid,
                callerCallsign = myCallsign,
                calleeUid = peerUid,
                calleeCallsign = peerCallsign,
                callType = cleanType,
                status = "RINGING",
              )
            pendingCallGraceId = sid
            pendingCallGraceUntilMs = System.currentTimeMillis() + 8_000L
            _activeCallSession.value = localSession
            observeSignalsForSession(sid)
            observeStreamsForSession(sid, peerUid)
          }
          .onFailure { _socialNotice.value = it.message ?: "Unable to initiate call." }
      } else {
        val sid = "call_${UUID.randomUUID().toString().take(8)}"
        val localSession =
          CallSession(
            id = sid,
            callerUid = myUid,
            callerCallsign = myCallsign,
            calleeUid = peerUid,
            calleeCallsign = peerCallsign,
            callType = cleanType,
            status = "RINGING",
          )
        LiveCloudServices.insertSupabaseCallSession(boundSupabaseToken, localSession)
          .onSuccess {
            pendingCallGraceId = sid
            pendingCallGraceUntilMs = System.currentTimeMillis() + 8_000L
            _activeCallSession.value = localSession
            observeSignalsForSession(sid)
          }
          .onFailure { _socialNotice.value = it.message ?: "Unable to initiate call." }
      }
    }
  }

  fun acceptIncomingCall(session: CallSession) {
    if (!cloudSyncReady) return
    _incomingCallSession.value = null
    _callMinimized.value = false
    pendingCallGraceId = session.id
    pendingCallGraceUntilMs = System.currentTimeMillis() + 8_000L
    _activeCallSession.value = session.copy(status = "ACTIVE")
    observeSignalsForSession(session.id)
    val peerUid = if (session.callerUid == boundUid) session.calleeUid else session.callerUid
    if (peerUid.isNotBlank()) {
      observeStreamsForSession(session.id, peerUid)
    }
    viewModelScope.launch {
      if (boundFirebaseAuth) {
        communityRepo.updateCallStatus(session.id, "ACTIVE")
      } else {
        LiveCloudServices.updateSupabaseCallStatus(boundSupabaseToken, session.id, "ACTIVE")
      }
    }
  }

  fun setCallMinimized(minimized: Boolean) {
    _callMinimized.value = minimized
  }

  fun switchActiveCallType(nextType: String) {
    val cur = _activeCallSession.value ?: return
    val clean = if (nextType == "VIDEO") "VIDEO" else "AUDIO"
    _activeCallSession.value = cur.copy(callType = clean)
    if (!cloudSyncReady) return
    viewModelScope.launch {
      if (boundFirebaseAuth) {
        communityRepo.updateCallType(cur.id, clean)
      } else {
        LiveCloudServices.updateSupabaseCallType(boundSupabaseToken, cur.id, clean)
      }
    }
  }

  fun sendInCallChat(sessionId: String, senderCallsign: String, message: String) {
    if (!cloudSyncReady || sessionId.isBlank() || message.isBlank()) return
    val textCheck = FamilySafetyGuard.checkText(message)
    if (!textCheck.allowed) {
      _socialNotice.value = textCheck.reason
      return
    }
    val payload = "${senderCallsign.take(24)}|${message.trim().take(400)}"
    viewModelScope.launch {
      if (boundFirebaseAuth) {
        communityRepo.sendCallSignal(sessionId, "CHAT", payload)
      } else {
        LiveCloudServices.insertSupabaseCallSignal(
          boundSupabaseToken,
          sessionId,
          CallSignal(id = "sig_${UUID.randomUUID().toString().take(8)}", senderId = boundUid, signalType = "CHAT", payload = payload),
        )
      }
    }
  }

  fun sendInCallPing(sessionId: String, senderCallsign: String, pingCode: String) {
    if (!cloudSyncReady || sessionId.isBlank() || pingCode.isBlank()) return
    val payload = "${senderCallsign.take(24)}|${pingCode.trim().take(120)}"
    viewModelScope.launch {
      if (boundFirebaseAuth) {
        communityRepo.sendCallSignal(sessionId, "PING", payload)
      } else {
        LiveCloudServices.insertSupabaseCallSignal(
          boundSupabaseToken,
          sessionId,
          CallSignal(id = "sig_${UUID.randomUUID().toString().take(8)}", senderId = boundUid, signalType = "PING", payload = payload),
        )
      }
    }
  }

  fun quickDeclineCallWithDm(session: CallSession, myCallsign: String, quickMessage: String) {
    declineOrEndCall(session)
    if (quickMessage.isNotBlank() && session.callerUid.isNotBlank()) {
      sendDm(
        myCallsign = myCallsign,
        peerUid = session.callerUid,
        peerCallsign = session.callerCallsign.ifBlank { "PILOT" },
        text = quickMessage,
      )
      _socialNotice.value = "Declined call & sent quick DM to ${session.callerCallsign}."
    }
  }

  fun declineOrEndCall(session: CallSession? = _activeCallSession.value ?: _incomingCallSession.value) {
    val target = session ?: return
    _incomingCallSession.value = null
    _callMinimized.value = false
    if (_activeCallSession.value?.id == target.id) {
      clearActiveCallResources()
    } else {
      audio.resetCallAudioMode()
    }
    if (!cloudSyncReady) return
    viewModelScope.launch {
      if (boundFirebaseAuth) {
        communityRepo.updateCallStatus(target.id, "ENDED")
      } else {
        LiveCloudServices.updateSupabaseCallStatus(boundSupabaseToken, target.id, "ENDED")
      }
    }
  }

  fun sendWebRtcSignal(sessionId: String, signalType: String, payload: String) {
    if (!cloudSyncReady || sessionId.isBlank() || payload.isBlank()) return
    viewModelScope.launch {
      if (boundFirebaseAuth) {
        communityRepo.sendCallSignal(sessionId, signalType, payload)
      } else {
        LiveCloudServices.insertSupabaseCallSignal(
          boundSupabaseToken,
          sessionId,
          CallSignal(id = "sig_${UUID.randomUUID().toString().take(10)}", senderId = boundUid, signalType = signalType, payload = payload),
        )
      }
    }
  }

  suspend fun publishCallVideoFrame(
    sessionId: String,
    seq: Long,
    active: Boolean,
    jpegBase64: String,
  ): Boolean {
    if (!cloudSyncReady || sessionId.isBlank()) return false
    return if (boundFirebaseAuth) {
      communityRepo.publishCallStreamFrame(sessionId, "VIDEO", seq, active, jpegBase64).isSuccess
    } else {
      if (!active || jpegBase64.isBlank()) return true
      val payload = """{"__relayVideo":"data:image/jpeg;base64,$jpegBase64"}"""
      LiveCloudServices.insertSupabaseCallSignal(
        boundSupabaseToken,
        sessionId,
        CallSignal(id = "sig_${UUID.randomUUID().toString().take(10)}", senderId = boundUid, signalType = "ICE", payload = payload),
      ).isSuccess
    }
  }

  suspend fun publishCallAudioChunk(
    sessionId: String,
    seq: Long,
    active: Boolean,
    ulawBase64: String,
  ): Boolean {
    if (!cloudSyncReady || sessionId.isBlank()) return false
    return if (boundFirebaseAuth) {
      communityRepo.publishCallStreamFrame(sessionId, "AUDIO", seq, active, ulawBase64).isSuccess
    } else {
      if (!active || ulawBase64.isBlank()) return true
      val payload = """{"__relayMuLaw":"$ulawBase64"}"""
      LiveCloudServices.insertSupabaseCallSignal(
        boundSupabaseToken,
        sessionId,
        CallSignal(id = "sig_${UUID.randomUUID().toString().take(10)}", senderId = boundUid, signalType = "ICE", payload = payload),
      ).isSuccess
    }
  }

  private fun observeStreamsForSession(sessionId: String, peerUid: String) {
    callStreamJob?.cancel()
    callSessionDocJob?.cancel()
    _remoteVideoStream.value = null
    _remoteAudioStream.value = null
    if (!boundFirebaseAuth || sessionId.isBlank() || peerUid.isBlank()) return

    // Direct doc observer on call_sessions/{sessionId} for instant RINGING -> ACTIVE -> ENDED & AUDIO <-> VIDEO sync
    callSessionDocJob =
      viewModelScope.launch {
        communityRepo
          .observeCallSession(sessionId)
          .catch { e ->
            if (e is CancellationException) throw e
            Log.w(TAG, "Call session doc observer error: ${e.message}")
          }
          .collect { latest ->
            if (latest == null) return@collect
            if (latest.status == "ENDED") {
              if (_activeCallSession.value?.id == sessionId) {
                clearActiveCallResources()
              }
            } else if (_activeCallSession.value?.id == sessionId && latest != _activeCallSession.value) {
              _activeCallSession.value = latest
            }
          }
      }

    callStreamJob =
      viewModelScope.launch {
        launch {
          communityRepo
            .observePeerCallStream(sessionId, peerUid, "VIDEO")
            .catch { e ->
              if (e is CancellationException) throw e
              Log.w(TAG, "Peer video stream error: ${e.message}")
            }
            .collect { frame -> _remoteVideoStream.value = frame }
        }
        launch {
          communityRepo
            .observePeerCallStream(sessionId, peerUid, "AUDIO")
            .catch { e ->
              if (e is CancellationException) throw e
              Log.w(TAG, "Peer audio stream error: ${e.message}")
            }
            .collect { frame -> _remoteAudioStream.value = frame }
        }
      }
  }

  private fun observeSignalsForSession(sessionId: String) {
    callSignalJob?.cancel()
    _callSignals.value = emptyList()
    if (boundFirebaseAuth) {
      callSignalJob =
        viewModelScope.launch {
          communityRepo
            .observeCallSignals(sessionId)
            .catch { e ->
              if (e is CancellationException) throw e
              Log.w(TAG, "Call signal observer error: ${e.message}")
            }
            .collect { _callSignals.value = it }
        }
    } else if (boundSupabaseToken.isNotBlank()) {
      val token = boundSupabaseToken
      callSignalJob =
        viewModelScope.launch {
          while (isActive) {
            LiveCloudServices.fetchSupabaseCallSignals(token, sessionId)
              .onSuccess { _callSignals.value = it }
            delay(1_200L)
          }
        }
    }
  }

  fun runPreRideFlightCheck(
    vehicleSummary: String,
    telemetrySummary: String,
    weatherSummary: String,
    riderGoal: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
  ) {
    if (_flightCheckState.value is UiState.Loading) return
    viewModelScope.launch {
      _flightCheckState.value = UiState.Loading
      val res =
        aiRepo.runPreRideFlightCheck(
          vehicleSummary = vehicleSummary,
          telemetrySummary = telemetrySummary,
          weatherSummary = weatherSummary,
          riderGoal = riderGoal,
          modelId = modelId,
        )
      res.fold(
        onSuccess = { _flightCheckState.value = UiState.Success(it) },
        onFailure = { _flightCheckState.value = UiState.Error(it.message ?: "Pre-ride AI flight check failed.") },
      )
    }
  }

  fun clearFlightCheck() {
    _flightCheckState.value = null
  }

  fun clearNotice() {
    _socialNotice.value = ""
  }

  private fun bitmapToBase64(bitmap: Bitmap): String {
    val bos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 78, bos)
    return Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
  }

  override fun onCleared() {
    audio.release()
    super.onCleared()
  }
}
