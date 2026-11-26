package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ErrorCodeDiagResult
import com.example.ai.GeminiAiRepository
import com.example.ai.GeminiModelCatalog
import com.example.ai.UpgradeCompatibilityResult
import com.example.core.LiveCloudServices
import com.example.data.MechanicDiagMessage
import com.example.data.PevCatalogData
import com.example.data.PevPartItem
import com.example.data.PevSpecRecord
import com.example.data.PevStoreDirectory
import com.example.data.UiState
import com.example.data.VisionAnomalyBox
import com.example.data.YouTubeVideoItem
import java.util.UUID
import com.example.core.ReminderScheduler
import com.example.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONArray
import org.json.JSONObject

data class MechanicDiagSession(
  val id: String = UUID.randomUUID().toString(),
  val title: String = "Workshop Session",
  val scope: String = "GENERAL",
  val updatedAtMs: Long = System.currentTimeMillis(),
  val messages: List<MechanicDiagMessage> = emptyList(),
)

class MechanicViewModel(application: Application) : AndroidViewModel(application) {

  private val ctx = application.applicationContext
  private val aiRepo = GeminiAiRepository()
  private val prefs = application.getSharedPreferences("mechanic_workshop_history_v1", Context.MODE_PRIVATE)

  private val _diagSessions = MutableStateFlow<List<MechanicDiagSession>>(emptyList())
  val diagSessions: StateFlow<List<MechanicDiagSession>> = _diagSessions.asStateFlow()

  private val _activeSessionId = MutableStateFlow("")
  val activeSessionId: StateFlow<String> = _activeSessionId.asStateFlow()

  private val _diagMessages = MutableStateFlow<List<MechanicDiagMessage>>(emptyList())
  val diagMessages: StateFlow<List<MechanicDiagMessage>> = _diagMessages.asStateFlow()

  private val _diagBusy = MutableStateFlow(false)
  val diagBusy: StateFlow<Boolean> = _diagBusy.asStateFlow()

  private val _wiringPinout = MutableStateFlow<Map<String, String>>(emptyMap())
  val wiringPinout: StateFlow<Map<String, String>> = _wiringPinout.asStateFlow()

  private val _checkedItems = MutableStateFlow<Set<String>>(emptySet())
  val checkedItems: StateFlow<Set<String>> = _checkedItems.asStateFlow()

  data class MaintenanceItem(
    val key: String,
    val title: String,
    val intervalKm: Double,
    val lastServicedKm: Double,
    val description: String,
  )

  private val _maintenanceItems =
    MutableStateFlow<List<MaintenanceItem>>(
      listOf(
        MaintenanceItem("tires", "Tire Wear & Pressure Check", 500.0, 0.0, "Inspect tread depth and maintain correct PSI"),
        MaintenanceItem("brakes", "Brake Pad & Caliper Inspection", 350.0, 0.0, "Check pad thickness and rotor alignment"),
        MaintenanceItem("drivetrain", "Chain / Belt & Sprocket Lube", 160.0, 0.0, "Clean, lubricate and check tension"),
        MaintenanceItem("battery", "Battery Pack & BMS Balance Check", 800.0, 0.0, "Verify cell group delta and terminal torque"),
      )
    )
  val maintenanceItems: StateFlow<List<MaintenanceItem>> = _maintenanceItems.asStateFlow()

  private val _savedSpecKeys = MutableStateFlow<Set<String>>(emptySet())
  val savedSpecKeys: StateFlow<Set<String>> = _savedSpecKeys.asStateFlow()

  private val _savedPartKeys = MutableStateFlow<Set<String>>(emptySet())
  val savedPartKeys: StateFlow<Set<String>> = _savedPartKeys.asStateFlow()

  var lastAutoSpokenMsgId: String = ""
    private set

  fun markDiagAutoSpoken(id: String) {
    lastAutoSpokenMsgId = id
  }

  fun toggleSavedSpec(key: String) {
    _savedSpecKeys.update { cur ->
      val next = if (key in cur) cur - key else cur + key
      prefs.edit().putStringSet("saved_spec_keys", next).apply()
      next
    }
  }

  fun toggleSavedPart(key: String) {
    _savedPartKeys.update { cur ->
      val next = if (key in cur) cur - key else cur + key
      prefs.edit().putStringSet("saved_part_keys", next).apply()
      next
    }
  }

  fun markMaintenanceServiced(itemKey: String, currentKm: Double) {
    _maintenanceItems.update { list ->
      val next = list.map { if (it.key == itemKey) it.copy(lastServicedKm = currentKm) else it }
      runCatching {
        val obj = JSONObject()
        next.forEach { obj.put(it.key, it.lastServicedKm) }
        prefs.edit().putString("maint_serviced_json", obj.toString()).apply()
      }
      next
    }
  }

  private val _errorCodeState = MutableStateFlow<UiState<ErrorCodeDiagResult>?>(null)
  val errorCodeState: StateFlow<UiState<ErrorCodeDiagResult>?> = _errorCodeState.asStateFlow()

  private val _upgradeCheckState = MutableStateFlow<UiState<UpgradeCompatibilityResult>?>(null)
  val upgradeCheckState: StateFlow<UiState<UpgradeCompatibilityResult>?> = _upgradeCheckState.asStateFlow()

  private val _specState =
    MutableStateFlow<UiState<List<PevSpecRecord>>>(UiState.Success(PevCatalogData.BUILT_IN_SPECS))
  val specState: StateFlow<UiState<List<PevSpecRecord>>> = _specState.asStateFlow()

  private val _specDigDeeperBusy = MutableStateFlow(false)
  val specDigDeeperBusy: StateFlow<Boolean> = _specDigDeeperBusy.asStateFlow()

  private val _specLoadMoreStatus = MutableStateFlow("")
  val specLoadMoreStatus: StateFlow<String> = _specLoadMoreStatus.asStateFlow()

  private val _specSearchHistory = MutableStateFlow<List<String>>(emptyList())
  val specSearchHistory: StateFlow<List<String>> = _specSearchHistory.asStateFlow()

  private val _comparedSpecs = MutableStateFlow<List<PevSpecRecord>>(emptyList())
  val comparedSpecs: StateFlow<List<PevSpecRecord>> = _comparedSpecs.asStateFlow()

  private val _partsState =
    MutableStateFlow<UiState<List<PevPartItem>>>(UiState.Success(PevCatalogData.BUILT_IN_PARTS))
  val partsState: StateFlow<UiState<List<PevPartItem>>> = _partsState.asStateFlow()

  private val _partsDigDeeperBusy = MutableStateFlow(false)
  val partsDigDeeperBusy: StateFlow<Boolean> = _partsDigDeeperBusy.asStateFlow()

  private val _partsLoadMoreStatus = MutableStateFlow("")
  val partsLoadMoreStatus: StateFlow<String> = _partsLoadMoreStatus.asStateFlow()

  private val _partsSearchHistory = MutableStateFlow<List<String>>(emptyList())
  val partsSearchHistory: StateFlow<List<String>> = _partsSearchHistory.asStateFlow()

  private val _bomCart = MutableStateFlow<List<PevPartItem>>(emptyList())
  val bomCart: StateFlow<List<PevPartItem>> = _bomCart.asStateFlow()

  private val _bomQuantities = MutableStateFlow<Map<String, Int>>(emptyMap())
  val bomQuantities: StateFlow<Map<String, Int>> = _bomQuantities.asStateFlow()

  // MULTI-IMAGE PRODUCT GALLERY CACHE (SPEC ENGINE & PARTS FINDER)
  private val _productImagesCache = MutableStateFlow<Map<String, List<String>>>(emptyMap())
  val productImagesCache: StateFlow<Map<String, List<String>>> = _productImagesCache.asStateFlow()

  private val _productImagesBusyKeys = MutableStateFlow<Set<String>>(emptySet())
  val productImagesBusyKeys: StateFlow<Set<String>> = _productImagesBusyKeys.asStateFlow()

  // LIVE YOUTUBE PEV / PART VIDEOS CACHE
  private val _pevVideosCache = MutableStateFlow<Map<String, List<YouTubeVideoItem>>>(emptyMap())
  val pevVideosCache: StateFlow<Map<String, List<YouTubeVideoItem>>> = _pevVideosCache.asStateFlow()

  private val _pevVideosBusyKeys = MutableStateFlow<Set<String>>(emptySet())
  val pevVideosBusyKeys: StateFlow<Set<String>> = _pevVideosBusyKeys.asStateFlow()

  // LIVE IN-STOCK STORE AVAILABILITY CACHE (SPEC ENGINE & PARTS FINDER)
  private val _verifiedStoreOffersCache =
    MutableStateFlow<Map<String, List<LiveCloudServices.VerifiedStoreOffer>>>(emptyMap())
  val verifiedStoreOffersCache: StateFlow<Map<String, List<LiveCloudServices.VerifiedStoreOffer>>> =
    _verifiedStoreOffersCache.asStateFlow()

  private val _stockCheckBusyKeys = MutableStateFlow<Set<String>>(emptySet())
  val stockCheckBusyKeys: StateFlow<Set<String>> = _stockCheckBusyKeys.asStateFlow()

  private val _stockCheckedKeys = MutableStateFlow<Set<String>>(emptySet())
  val stockCheckedKeys: StateFlow<Set<String>> = _stockCheckedKeys.asStateFlow()

  private val _autoFindStores =
    MutableStateFlow(prefs.getBoolean("auto_find_stores_enabled", true))
  val autoFindStores: StateFlow<Boolean> = _autoFindStores.asStateFlow()

  private val stockVerifySemaphore = Semaphore(4)

  init {
    loadPersistedHistory()
  }

  private fun loadPersistedHistory() {
    runCatching {
      val raw = prefs.getString("sessions_json", "").orEmpty()
      if (raw.isNotBlank()) {
        val arr = JSONArray(raw)
        val loaded = mutableListOf<MechanicDiagSession>()
        for (i in 0 until arr.length()) {
          val o = arr.optJSONObject(i) ?: continue
          val msgArr = o.optJSONArray("messages")
          val msgs = mutableListOf<MechanicDiagMessage>()
          if (msgArr != null) {
            for (j in 0 until msgArr.length()) {
              val m = msgArr.optJSONObject(j) ?: continue
              val chkArr = m.optJSONArray("checklist")
              val chk = if (chkArr != null) (0 until chkArr.length()).map { chkArr.optString(it) } else emptyList()
              val anomArr = m.optJSONArray("anomalies")
              val anoms =
                if (anomArr != null) {
                  (0 until anomArr.length()).mapNotNull { idx ->
                    val a = anomArr.optJSONObject(idx) ?: return@mapNotNull null
                    VisionAnomalyBox(
                      label = a.optString("label"),
                      severity = a.optString("severity", "WARNING"),
                      xNorm = a.optDouble("xNorm", 0.2).toFloat(),
                      yNorm = a.optDouble("yNorm", 0.2).toFloat(),
                      wNorm = a.optDouble("wNorm", 0.3).toFloat(),
                      hNorm = a.optDouble("hNorm", 0.3).toFloat(),
                    )
                  }
                } else emptyList()
              msgs +=
                MechanicDiagMessage(
                  id = m.optString("id", UUID.randomUUID().toString()),
                  isUser = m.optBoolean("isUser", false),
                  text = m.optString("text"),
                  checklist = chk,
                  anomalies = anoms,
                )
            }
          }
          loaded +=
            MechanicDiagSession(
              id = o.optString("id", UUID.randomUUID().toString()),
              title = o.optString("title", "Workshop Session"),
              scope = o.optString("scope", "GENERAL"),
              updatedAtMs = o.optLong("updatedAtMs", System.currentTimeMillis()),
              messages = msgs,
            )
        }
        _diagSessions.value = loaded.sortedByDescending { it.updatedAtMs }
        loaded.firstOrNull()?.let { first ->
          _activeSessionId.value = first.id
          _diagMessages.value = first.messages
          lastAutoSpokenMsgId = first.messages.lastOrNull { !it.isUser }?.id.orEmpty()
        }
      }
      val maintRaw = prefs.getString("maint_serviced_json", "").orEmpty()
      if (maintRaw.isNotBlank()) {
        val obj = JSONObject(maintRaw)
        _maintenanceItems.update { list ->
          list.map { item ->
            if (obj.has(item.key)) item.copy(lastServicedKm = obj.optDouble(item.key, item.lastServicedKm))
            else item
          }
        }
      }
      _savedSpecKeys.value = prefs.getStringSet("saved_spec_keys", emptySet()).orEmpty().toSet()
      _savedPartKeys.value = prefs.getStringSet("saved_part_keys", emptySet()).orEmpty().toSet()
      val specHist = prefs.getString("spec_history", "").orEmpty().split("||").filter { it.isNotBlank() }
      val partsHist = prefs.getString("parts_history", "").orEmpty().split("||").filter { it.isNotBlank() }
      _specSearchHistory.value = specHist
      _partsSearchHistory.value = partsHist
    }
  }

  private fun persistSessions(sessions: List<MechanicDiagSession>) {
    runCatching {
      val arr = JSONArray()
      sessions.take(30).forEach { s ->
        val msgArr = JSONArray()
        s.messages.takeLast(40).forEach { m ->
          val chkArr = JSONArray().apply { m.checklist.forEach { put(it) } }
          val anomArr = JSONArray().apply {
            m.anomalies.forEach { a ->
              put(
                JSONObject().apply {
                  put("label", a.label)
                  put("severity", a.severity)
                  put("xNorm", a.xNorm.toDouble())
                  put("yNorm", a.yNorm.toDouble())
                  put("wNorm", a.wNorm.toDouble())
                  put("hNorm", a.hNorm.toDouble())
                }
              )
            }
          }
          msgArr.put(
            JSONObject().apply {
              put("id", m.id)
              put("isUser", m.isUser)
              put("text", m.text)
              put("checklist", chkArr)
              put("anomalies", anomArr)
            }
          )
        }
        arr.put(
          JSONObject().apply {
            put("id", s.id)
            put("title", s.title)
            put("scope", s.scope)
            put("updatedAtMs", s.updatedAtMs)
            put("messages", msgArr)
          }
        )
      }
      prefs.edit().putString("sessions_json", arr.toString()).apply()
    }
  }

  private fun syncActiveSession(scope: String = "GENERAL", titleHint: String = "") {
    val curMsgs = _diagMessages.value
    val sid = _activeSessionId.value.ifBlank { UUID.randomUUID().toString().also { _activeSessionId.value = it } }
    val existing = _diagSessions.value.firstOrNull { it.id == sid }
    val resolvedTitle =
      when {
        existing != null && existing.title != "Workshop Session" -> existing.title
        titleHint.isNotBlank() -> titleHint.take(44)
        curMsgs.isNotEmpty() -> curMsgs.first().text.take(44)
        else -> "Workshop Session"
      }
    val updated =
      MechanicDiagSession(
        id = sid,
        title = resolvedTitle,
        scope = scope,
        updatedAtMs = System.currentTimeMillis(),
        messages = curMsgs,
      )
    val nextList = (listOf(updated) + _diagSessions.value.filterNot { it.id == sid }).sortedByDescending { it.updatedAtMs }
    _diagSessions.value = nextList
    persistSessions(nextList)
  }

  fun startNewDiagSession(title: String = "Workshop Session", scope: String = "GENERAL") {
    val newId = UUID.randomUUID().toString()
    _activeSessionId.value = newId
    _diagMessages.value = emptyList()
    cancelVisionDiagnosis()
    _wiringPinout.value = emptyMap()
    val newSess = MechanicDiagSession(id = newId, title = title, scope = scope, updatedAtMs = System.currentTimeMillis(), messages = emptyList())
    val next = listOf(newSess) + _diagSessions.value
    _diagSessions.value = next
    persistSessions(next)
  }

  fun selectDiagSession(sessionId: String) {
    val target = _diagSessions.value.firstOrNull { it.id == sessionId } ?: return
    _activeSessionId.value = target.id
    _diagMessages.value = target.messages
    lastAutoSpokenMsgId = target.messages.lastOrNull { !it.isUser }?.id.orEmpty()
    _wiringPinout.value = emptyMap()
  }

  fun deleteDiagSession(sessionId: String) {
    val remaining = _diagSessions.value.filterNot { it.id == sessionId }
    _diagSessions.value = remaining
    persistSessions(remaining)
    if (_activeSessionId.value == sessionId) {
      val fallback = remaining.firstOrNull()
      _activeSessionId.value = fallback?.id.orEmpty()
      _diagMessages.value = fallback?.messages.orEmpty()
      lastAutoSpokenMsgId = fallback?.messages?.lastOrNull { !it.isUser }?.id.orEmpty()
    }
  }

  fun exportActiveDiagTranscript(): String {
    val curTitle = _diagSessions.value.firstOrNull { it.id == _activeSessionId.value }?.title ?: "PEV Workshop Session"
    return buildString {
      appendLine("=== RURAL ERIDES GO MECHANIC DIAGNOSTIC LOG ===")
      appendLine("Session: $curTitle")
      appendLine()
      _diagMessages.value.forEach { m ->
        appendLine(if (m.isUser) "[PILOT]: ${m.text}" else "[WORKSHOP CHIEF AI]: ${m.text}")
        if (m.checklist.isNotEmpty()) {
          m.checklist.forEach { c -> appendLine("  - [ ] $c") }
        }
        appendLine()
      }
    }
  }

  private var diagJob: kotlinx.coroutines.Job? = null

  fun cancelVisionDiagnosis() {
    diagJob?.cancel()
    diagJob = null
    _diagBusy.value = false
  }

  fun runVisionDiagnosis(
    primaryBitmap: Bitmap?,
    secondaryBitmap: Bitmap?,
    query: String,
    fleetInfo: String,
    unitSystem: String,
    scope: String,
    detailLevel: String,
    imageLabels: List<String>,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
  ) {
    val recentHistory = _diagMessages.value.takeLast(6)
    val historySummary =
      recentHistory.joinToString("\n") { m ->
        val role = if (m.isUser) "Pilot" else "MechanicAI"
        "$role: ${m.text.take(350)}"
      }
    val userMsg =
      MechanicDiagMessage(
        id = UUID.randomUUID().toString(),
        isUser = true,
        text = query.ifBlank { "Optical PEV hardware scan ($scope)" },
        imageUris = imageLabels,
      )
    _diagMessages.update { it + userMsg }
    syncActiveSession(scope = scope, titleHint = userMsg.text)

    diagJob?.cancel()
    diagJob =
      viewModelScope.launch {
        _diagBusy.value = true
        try {
          val res =
            aiRepo.diagnoseVision(
              primaryBitmap = primaryBitmap,
              secondaryBitmap = secondaryBitmap,
              query = query,
              fleetInfo = fleetInfo,
              unitSystem = unitSystem,
              scope = scope,
              detailLevel = detailLevel,
              modelId = modelId,
              historySummary = historySummary,
            )
          res.onSuccess { diag ->
            if (diag.wiringMap.isNotEmpty()) {
              _wiringPinout.value = diag.wiringMap
            }
            _diagMessages.update {
              it +
                MechanicDiagMessage(
                  id = UUID.randomUUID().toString(),
                  isUser = false,
                  text = diag.diagnosisText,
                  checklist = diag.checklist,
                  anomalies = diag.anomalies,
                )
            }
            syncActiveSession(scope = scope)
            val settings = SettingsStore(ctx).settingsFlow.first()
            if (settings.aiNotifsEnabled) {
              ReminderScheduler.postAiNotification(
                context = ctx,
                title = "AI Mechanic Diagnosis Ready",
                body = diag.diagnosisText.take(150),
                targetTab = "MECHANIC",
              )
            }
          }
          res.onFailure { err ->
            if (err is kotlinx.coroutines.CancellationException) throw err
            _diagMessages.update {
              it +
                MechanicDiagMessage(
                  id = UUID.randomUUID().toString(),
                  isUser = false,
                  text = "Diagnostic Error: ${err.message ?: "Unable to complete AI analysis."}",
                )
            }
            syncActiveSession(scope = scope)
          }
        } finally {
          _diagBusy.value = false
        }
      }
  }

  fun toggleChecklistItem(itemKey: String) {
    _checkedItems.update { cur -> if (itemKey in cur) cur - itemKey else cur + itemKey }
  }

  fun clearDiagChat() {
    cancelVisionDiagnosis()
    _diagMessages.value = emptyList()
    _wiringPinout.value = emptyMap()
    if (_activeSessionId.value.isNotBlank()) {
      syncActiveSession()
    }
  }

  fun lookupLiveErrorCode(
    code: String,
    brand: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
  ) {
    viewModelScope.launch {
      _errorCodeState.value = UiState.Loading
      val res = aiRepo.lookupErrorCode(code, brand, modelId)
      _errorCodeState.value =
        res.fold(
          onSuccess = { UiState.Success(it) },
          onFailure = { UiState.Error(it.message ?: "Error lookup failed") },
        )
    }
  }

  fun clearErrorCodeResult() {
    _errorCodeState.value = null
  }

  fun resetSpecCatalog() {
    _specLoadMoreStatus.value = ""
    _specState.value = UiState.Success(PevCatalogData.BUILT_IN_SPECS)
  }

  private fun recordSpecHistory(query: String) {
    val clean = query.trim()
    if (clean.isBlank()) return
    val next = (listOf(clean) + _specSearchHistory.value.filterNot { it.equals(clean, ignoreCase = true) }).take(10)
    _specSearchHistory.value = next
    prefs.edit().putString("spec_history", next.joinToString("||")).apply()
  }

  private fun recordPartsHistory(query: String) {
    val clean = query.trim()
    if (clean.isBlank()) return
    val next = (listOf(clean) + _partsSearchHistory.value.filterNot { it.equals(clean, ignoreCase = true) }).take(10)
    _partsSearchHistory.value = next
    prefs.edit().putString("parts_history", next.joinToString("||")).apply()
  }

  fun searchSpecs(
    query: String,
    category: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
    deepSearch: Boolean = true,
  ) {
    recordSpecHistory(query)
    _specLoadMoreStatus.value = ""
    viewModelScope.launch {
      _specState.value = UiState.Loading
      val localMatches = PevCatalogData.filterSpecCatalog(query = query, category = category)
      val res = aiRepo.searchLivePevSpecs(query, category, modelId, deepSearch = deepSearch)
      _specState.value =
        res.fold(
          onSuccess = { live ->
            val merged = (live + localMatches).distinctBy { "${it.brand.lowercase()}_${it.name.lowercase()}" }
            _specLoadMoreStatus.value = "Loaded ${merged.size} rigs (${live.size} live web/AI). Tap Load More below for additional models."
            UiState.Success(if (merged.isNotEmpty()) merged else PevCatalogData.BUILT_IN_SPECS)
          },
          onFailure = { err ->
            if (localMatches.isNotEmpty()) UiState.Success(localMatches)
            else UiState.Error(err.message ?: "Spec search failed")
          },
        )
    }
  }

  fun digDeeperSpecs(
    query: String,
    category: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
  ) {
    if (_specDigDeeperBusy.value) return
    val existing = (_specState.value as? UiState.Success)?.data.orEmpty()
    viewModelScope.launch {
      _specDigDeeperBusy.value = true
      _specLoadMoreStatus.value = "Loading more unique PEV specs from live web & AI..."
      val res =
        aiRepo.searchLivePevSpecs(
          query = query.ifBlank { category.takeIf { it != "ALL" } ?: "high performance PEV" },
          category = category,
          modelId = modelId,
          deepSearch = true,
          excludeNames = existing.map { "${it.brand} ${it.name}".trim() },
        )
      res.fold(
        onSuccess = { more ->
          val merged = (existing + more).distinctBy { "${it.brand.lowercase()}_${it.name.lowercase()}" }
          val added = (merged.size - existing.size).coerceAtLeast(0)
          _specState.value = UiState.Success(merged)
          _specLoadMoreStatus.value =
            if (added > 0) "+$added more rigs loaded (${merged.size} total). Tap Load More again to keep expanding!"
            else "No additional unique models found for this query. Try broadening your filter or query."
        },
        onFailure = { err ->
          _specLoadMoreStatus.value = "Load More paused: ${err.message ?: "check connection and retry"}"
        },
      )
      _specDigDeeperBusy.value = false
    }
  }

  fun toggleCompareSpec(spec: PevSpecRecord) {
    _comparedSpecs.update { cur ->
      if (cur.any { it.name == spec.name }) cur.filterNot { it.name == spec.name }
      else (cur + spec).takeLast(4)
    }
  }

  fun clearCompareSpecs() {
    _comparedSpecs.value = emptyList()
  }

  fun resetPartsCatalog() {
    _partsLoadMoreStatus.value = ""
    _partsState.value = UiState.Success(PevCatalogData.BUILT_IN_PARTS)
  }

  fun searchParts(
    query: String,
    make: String,
    model: String,
    tireSize: String,
    category: String,
    region: String,
    vehicleType: String = "ALL",
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
    deepSearch: Boolean = true,
  ) {
    recordPartsHistory(listOf(make, model, query).filter { it.isNotBlank() }.joinToString(" "))
    _partsLoadMoreStatus.value = ""
    viewModelScope.launch {
      _partsState.value = UiState.Loading
      val localMatches =
        PevCatalogData.filterPartCatalog(
          query = listOf(query, make, model, tireSize).filter { it.isNotBlank() }.joinToString(" "),
          vehicleType = vehicleType,
          category = category,
        )
      val enrichedQuery =
        listOf(if (vehicleType != "ALL") vehicleType else "", query).filter { it.isNotBlank() }.joinToString(" ")
      val res = aiRepo.searchLiveParts(enrichedQuery, make, model, tireSize, category, region, modelId, deepSearch = deepSearch)
      _partsState.value =
        res.fold(
          onSuccess = { live ->
            val merged = (live + localMatches).distinctBy { it.partName.lowercase() }
            _partsLoadMoreStatus.value = "Loaded ${merged.size} parts (${live.size} live web/AI). Tap Load More below to see additional parts."
            UiState.Success(if (merged.isNotEmpty()) merged else PevCatalogData.BUILT_IN_PARTS)
          },
          onFailure = { err ->
            if (localMatches.isNotEmpty()) UiState.Success(localMatches)
            else UiState.Error(err.message ?: "Parts finder failed")
          },
        )
    }
  }

  fun digDeeperParts(
    query: String,
    make: String,
    model: String,
    tireSize: String,
    category: String,
    region: String,
    vehicleType: String = "ALL",
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
  ) {
    if (_partsDigDeeperBusy.value) return
    val existing = (_partsState.value as? UiState.Success)?.data.orEmpty()
    viewModelScope.launch {
      _partsDigDeeperBusy.value = true
      _partsLoadMoreStatus.value = "Loading more unique PEV parts & upgrades from live web & AI..."
      val enrichedQuery =
        listOf(if (vehicleType != "ALL") vehicleType else "", query).filter { it.isNotBlank() }.joinToString(" ")
      val res =
        aiRepo.searchLiveParts(
          query = enrichedQuery.ifBlank { category },
          make = make,
          model = model,
          tireSize = tireSize,
          category = category,
          region = region,
          modelId = modelId,
          deepSearch = true,
          excludeNames = existing.map { it.partName },
        )
      res.fold(
        onSuccess = { more ->
          val merged = (existing + more).distinctBy { it.partName.lowercase() }
          val added = (merged.size - existing.size).coerceAtLeast(0)
          _partsState.value = UiState.Success(merged)
          _partsLoadMoreStatus.value =
            if (added > 0) "+$added more parts loaded (${merged.size} total). Tap Load More again to see even more!"
            else "No additional unique parts found for this query. Try broadening your filter or query."
        },
        onFailure = { err ->
          _partsLoadMoreStatus.value = "Load More paused: ${err.message ?: "check connection and retry"}"
        },
      )
      _partsDigDeeperBusy.value = false
    }
  }

  fun toggleBomItem(part: PevPartItem) {
    val exists = _bomCart.value.any { it.partName == part.partName }
    _bomCart.update { cur ->
      if (exists) cur.filterNot { it.partName == part.partName } else cur + part
    }
    _bomQuantities.update { q ->
      if (exists) q - part.partName else q + (part.partName to 1)
    }
  }

  fun adjustBomQuantity(partName: String, delta: Int) {
    val current = _bomQuantities.value[partName] ?: 1
    val next = current + delta
    if (next <= 0) {
      _bomCart.update { it.filterNot { p -> p.partName == partName } }
      _bomQuantities.update { it - partName }
    } else {
      _bomQuantities.update { it + (partName to next.coerceAtMost(99)) }
    }
  }

  fun clearBomCart() {
    _bomCart.value = emptyList()
    _bomQuantities.value = emptyMap()
  }

  fun exportSpecsCsv(): String {
    val items = _comparedSpecs.value
    val header = "Name,Brand,Category,Price,TopSpeed,Range,Motor,Battery,Voltage,Weight"
    val rows =
      items.joinToString("\n") {
        "\"${it.name}\",\"${it.brand}\",${it.category},\"${it.price}\",\"${it.topSpeed}\",\"${it.range}\",\"${it.motorPower}\",\"${it.battery}\",${it.nominalVoltage},\"${it.weight}\""
      }
    return "$header\n$rows"
  }

  fun loadProductImages(
    cacheKey: String,
    query: String,
    seedUrls: List<String> = emptyList(),
    forceRefresh: Boolean = false,
  ) {
    if (!forceRefresh && (_productImagesCache.value[cacheKey]?.size ?: 0) >= 3) return
    if (cacheKey in _productImagesBusyKeys.value) return
    viewModelScope.launch {
      _productImagesBusyKeys.update { it + cacheKey }
      val fetched = LiveCloudServices.fetchLiveProductImages(query, limit = 6)
      val merged = (seedUrls.filter { it.startsWith("http") } + fetched).distinct().take(6)
      _productImagesCache.update { cur -> cur + (cacheKey to merged) }
      _productImagesBusyKeys.update { it - cacheKey }
    }
  }

  fun loadPevVideos(
    cacheKey: String,
    query: String,
    forceRefresh: Boolean = false,
  ) {
    if (!forceRefresh && _pevVideosCache.value.containsKey(cacheKey)) return
    if (cacheKey in _pevVideosBusyKeys.value) return
    viewModelScope.launch {
      _pevVideosBusyKeys.update { it + cacheKey }
      val res = LiveCloudServices.fetchYouTubeVideos(query = query, channelOnly = false)
      res.onSuccess { (videos, _) ->
        _pevVideosCache.update { cur -> cur + (cacheKey to videos.take(10)) }
      }
      _pevVideosBusyKeys.update { it - cacheKey }
    }
  }

  fun setAutoFindStores(enabled: Boolean) {
    _autoFindStores.value = enabled
    prefs.edit().putBoolean("auto_find_stores_enabled", enabled).apply()
  }

  fun toggleAutoFindStores() {
    setAutoFindStores(!_autoFindStores.value)
  }

  fun scanSpecsStores(specs: List<PevSpecRecord>, forceRefresh: Boolean = false) {
    specs.take(12).forEach { spec ->
      val key = "spec_${spec.brand}_${spec.name}".lowercase()
      verifyInStockStores(
        cacheKey = key,
        productTitle = PevStoreDirectory.buildCleanSpecQuery(spec),
        candidateStores = PevStoreDirectory.candidateStoresForSpec(spec),
        forceRefresh = forceRefresh,
      )
    }
  }

  fun scanPartsStores(parts: List<PevPartItem>, forceRefresh: Boolean = false) {
    parts.take(12).forEach { part ->
      val key = "part_${part.partName}_${part.exactDimensions}".lowercase()
      verifyInStockStores(
        cacheKey = key,
        productTitle = PevStoreDirectory.buildCleanPartQuery(part),
        candidateStores = PevStoreDirectory.candidateStoresForPart(part),
        forceRefresh = forceRefresh,
      )
    }
  }

  fun verifyInStockStores(
    cacheKey: String,
    productTitle: String,
    candidateStores: List<Pair<String, Pair<String, String>>>,
    forceRefresh: Boolean = false,
  ) {
    if (!forceRefresh && cacheKey in _stockCheckedKeys.value) return
    if (cacheKey in _stockCheckBusyKeys.value) return
    viewModelScope.launch {
      _stockCheckBusyKeys.update { it + cacheKey }
      try {
        val verified =
          stockVerifySemaphore.withPermit {
            LiveCloudServices.findVerifiedInStockStores(
              productTitle = productTitle,
              candidateStores = candidateStores,
            )
          }
        _verifiedStoreOffersCache.update { it + (cacheKey to verified) }
        _stockCheckedKeys.update { it + cacheKey }
      } finally {
        _stockCheckBusyKeys.update { it - cacheKey }
      }
    }
  }

  fun checkUpgradeCompatibility(
    vehicleSummary: String,
    plannedUpgrade: String,
    ridingGoal: String,
    modelId: String = GeminiModelCatalog.GEMINI_3_1_FLASH_LITE,
  ) {
    if (plannedUpgrade.isBlank()) return
    viewModelScope.launch {
      _upgradeCheckState.value = UiState.Loading
      val res =
        aiRepo.analyzeUpgradeCompatibility(
          vehicleSummary = vehicleSummary,
          plannedUpgrade = plannedUpgrade,
          ridingGoal = ridingGoal,
          modelId = modelId,
        )
      res.fold(
        onSuccess = { _upgradeCheckState.value = UiState.Success(it) },
        onFailure = { _upgradeCheckState.value = UiState.Error(it.message ?: "Upgrade compatibility check failed.") },
      )
    }
  }

  fun clearUpgradeCheck() {
    _upgradeCheckState.value = null
  }

  fun exportBomCsv(): String {
    val header = "Part,Qty,Category,Dimensions,Compatibility,EstPrice,Source,WeightLbs"
    val qtys = _bomQuantities.value
    val rows =
      _bomCart.value.joinToString("\n") {
        val qty = qtys[it.partName] ?: 1
        "\"${it.partName}\",$qty,\"${it.category}\",\"${it.exactDimensions}\",\"${it.compatibility}\",\"${it.estimatedPrice}\",\"${it.sourcePlatform}\",${it.weightLbs * qty}"
      }
    return "$header\n$rows"
  }
}
