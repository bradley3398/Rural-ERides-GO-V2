package com.example.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import java.io.IOException
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "SupabaseAuthService"
private val Context.supabaseAuthStore by preferencesDataStore(name = "volttrail_supabase_auth")

data class SupabaseUser(
  val id: String,
  val email: String,
  val callsign: String,
  val role: String = "authenticated",
)

data class SupabaseSession(
  val accessToken: String,
  val refreshToken: String,
  val expiresAtEpochSec: Long,
  val user: SupabaseUser,
)

sealed interface SupabaseAuthResult {
  data class Success(val session: SupabaseSession) : SupabaseAuthResult
  data class EmailConfirmationRequired(val email: String, val message: String) : SupabaseAuthResult
  data class Error(val message: String) : SupabaseAuthResult
}

data class ServiceHealthResult(
  val healthy: Boolean,
  val latencyMs: Long,
  val detail: String,
)

/**
 * Full Supabase GoTrue authentication service module.
 * Manages email/password sign-in, sign-up with pilot callsign metadata,
 * JWT token refresh, password recovery, user metadata updates, and health checks.
 */
class SupabaseAuthService(private val context: Context) {

  private object Keys {
    val ACCESS_TOKEN = stringPreferencesKey("sb_access_token")
    val REFRESH_TOKEN = stringPreferencesKey("sb_refresh_token")
    val EXPIRES_AT = longPreferencesKey("sb_expires_at")
    val USER_ID = stringPreferencesKey("sb_user_id")
    val USER_EMAIL = stringPreferencesKey("sb_user_email")
    val USER_CALLSIGN = stringPreferencesKey("sb_user_callsign")
    val USER_ROLE = stringPreferencesKey("sb_user_role")
    val PKCE_VERIFIER = stringPreferencesKey("sb_pkce_verifier")
    val PKCE_STATE = stringPreferencesKey("sb_pkce_state")
  }

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val httpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .protocols(listOf(Protocol.HTTP_1_1))
      .retryOnConnectionFailure(true)
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .writeTimeout(20, TimeUnit.SECONDS)
      .build()
  }

  private val _sessionFlow = MutableStateFlow<SupabaseSession?>(null)
  val sessionFlow: StateFlow<SupabaseSession?> = _sessionFlow.asStateFlow()

  init {
    scope.launch {
      restoreValidSession()
    }
  }

  fun isConfigured(): Boolean = isConfiguredValues(baseUrl(), anonKey())

  private fun baseUrl(): String = sanitizeBaseUrl(BuildConfig.SUPABASE_URL)
  private fun anonKey(): String = sanitizeAnonKey(BuildConfig.SUPABASE_ANON_KEY)

  private fun Request.Builder.withSupabaseHeaders(
    userAccessToken: String? = null,
    forceCloseConnection: Boolean = false,
  ): Request.Builder {
    val key = anonKey()
    header("apikey", key)
    header("Accept", "application/json")
    header("User-Agent", "RuralERidesGO/11.0 (Android)")
    if (forceCloseConnection) {
      header("Connection", "close")
    }
    val authCandidate = userAccessToken?.trim().orEmpty().ifBlank { key }
    if (isJwtToken(authCandidate)) {
      header("Authorization", "Bearer $authCandidate")
    }
    return this
  }

  private suspend fun executeWithRetry(buildRequest: (attempt: Int) -> Request): Response {
    var lastIoError: IOException? = null
    for (attempt in 0 until 4) {
      try {
        val baseReq = buildRequest(attempt)
        val req =
          if (attempt > 0) baseReq.newBuilder().header("Connection", "close").build()
          else baseReq
        val resp = httpClient.newCall(req).execute()
        val retryableCode = resp.code in listOf(520, 502, 503, 504)
        if (!retryableCode || attempt == 3) {
          return resp
        }
        Log.w(TAG, "Supabase transient HTTP ${resp.code} on attempt ${attempt + 1}; evicting pool & retrying...")
        resp.close()
        httpClient.connectionPool.evictAll()
      } catch (e: CancellationException) {
        throw e
      } catch (e: IOException) {
        lastIoError = e
        if (attempt == 3) throw e
        Log.w(TAG, "Supabase IO error on attempt ${attempt + 1}: ${e.message}")
        httpClient.connectionPool.evictAll()
      }
      delay(500L * (attempt + 1))
    }
    throw lastIoError ?: IOException("Supabase request failed after retries")
  }

  /**
   * Self-heals oversized Supabase Auth JWTs (>3200 chars) caused by legacy metadata backups
   * that can trigger Cloudflare HTTP 520 origin errors on subsequent requests.
   */
  private suspend fun healOversizedJwtIfNeeded(session: SupabaseSession): SupabaseSession {
    if (session.accessToken.length <= 3200) return session
    Log.w(TAG, "Detected oversized Supabase JWT (${session.accessToken.length} chars); pruning legacy user_metadata backup to prevent HTTP 520.")
    return runCatching {
      val prunePayload =
        JSONObject().apply {
          put(
            "data",
            JSONObject().apply {
              put("volttrail_backup", JSONObject.NULL)
            },
          )
        }.toString()
      executeWithRetry {
        Request.Builder()
          .url("${baseUrl()}/auth/v1/user")
          .withSupabaseHeaders(session.accessToken, forceCloseConnection = true)
          .header("Content-Type", "application/json")
          .put(prunePayload.toRequestBody("application/json".toMediaType()))
          .build()
      }.use { resp ->
        if (resp.isSuccessful && session.refreshToken.isNotBlank()) {
          refreshSupabaseSessionInternal(session.refreshToken) ?: session
        } else {
          session
        }
      }
    }.getOrDefault(session)
  }

  suspend fun restoreValidSession(): SupabaseSession? =
    withContext(Dispatchers.IO) {
      val prefs = context.supabaseAuthStore.data.first()
      val token = prefs[Keys.ACCESS_TOKEN].orEmpty()
      val refresh = prefs[Keys.REFRESH_TOKEN].orEmpty()
      val uid = prefs[Keys.USER_ID].orEmpty()
      val email = prefs[Keys.USER_EMAIL].orEmpty()
      val callsign = prefs[Keys.USER_CALLSIGN].orEmpty()
      val role = prefs[Keys.USER_ROLE] ?: "authenticated"
      val exp = prefs[Keys.EXPIRES_AT] ?: 0L

      if (uid.isBlank() || token.isBlank()) {
        _sessionFlow.value = null
        return@withContext null
      }

      val loaded =
        SupabaseSession(
          accessToken = token,
          refreshToken = refresh,
          expiresAtEpochSec = exp,
          user = SupabaseUser(id = uid, email = email, callsign = callsign, role = role),
        )
      val nowSec = System.currentTimeMillis() / 1000L
      if (exp > nowSec + 90L) {
        _sessionFlow.value = loaded
        return@withContext loaded
      }

      if (refresh.isNotBlank() && isConfigured()) {
        val refreshed = refreshSupabaseSessionInternal(refresh)
        if (refreshed != null) {
          return@withContext refreshed
        }
      }

      if (exp in 1L..nowSec) {
        context.supabaseAuthStore.edit { it.clear() }
        _sessionFlow.value = null
        return@withContext null
      }

      _sessionFlow.value = loaded
      loaded
    }

  suspend fun signInWithSupabasePassword(email: String, password: String): SupabaseAuthResult =
    withContext(Dispatchers.IO) {
      if (!isConfigured()) {
        return@withContext SupabaseAuthResult.Error(
          "Supabase backend not configured yet. Set SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets."
        )
      }
      val cleanEmail = email.trim().lowercase()
      if (!isValidEmail(cleanEmail) || password.isBlank()) {
        return@withContext SupabaseAuthResult.Error("Enter a valid email address and password.")
      }
      try {
        val payload =
          JSONObject().apply {
            put("email", cleanEmail)
            put("password", password)
          }
        val payloadStr = payload.toString()
        executeWithRetry {
          Request.Builder()
            .url("${baseUrl()}/auth/v1/token?grant_type=password")
            .withSupabaseHeaders()
            .header("Content-Type", "application/json")
            .post(payloadStr.toRequestBody("application/json".toMediaType()))
            .build()
        }.use { resp ->
          val raw = resp.body?.string().orEmpty()
          if (!resp.isSuccessful) {
            if (isEmailNotConfirmedError(raw)) {
              return@withContext SupabaseAuthResult.EmailConfirmationRequired(
                email = cleanEmail,
                message = "Email not confirmed yet for $cleanEmail. Check your inbox or tap Resend Verification Email below.",
              )
            }
            return@withContext SupabaseAuthResult.Error(parseSupabaseError(raw, resp.code))
          }
          val parsed = parseSessionJson(JSONObject(raw), fallbackEmail = cleanEmail, fallbackCallsign = "")
          if (parsed == null) {
            return@withContext SupabaseAuthResult.Error("Supabase returned an incomplete session payload.")
          }
          persistSession(parsed)
          val healed = healOversizedJwtIfNeeded(parsed)
          SupabaseAuthResult.Success(healed)
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Supabase sign-in failed", e)
        SupabaseAuthResult.Error(e.localizedMessage ?: "Network error contacting Supabase Auth.")
      }
    }

  suspend fun signUpWithSupabase(
    email: String,
    password: String,
    callsign: String,
  ): SupabaseAuthResult =
    withContext(Dispatchers.IO) {
      if (!isConfigured()) {
        return@withContext SupabaseAuthResult.Error(
          "Supabase backend not configured yet. Set SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets."
        )
      }
      val cleanEmail = email.trim().lowercase()
      val cleanCallsign = callsign.trim().ifBlank { cleanEmail.substringBefore("@").uppercase() }
      if (!isValidEmail(cleanEmail) || password.length < 6) {
        return@withContext SupabaseAuthResult.Error("Valid email address and password (6+ chars) required.")
      }
      try {
        val meta =
          JSONObject().apply {
            put("callsign", cleanCallsign)
            put("display_name", cleanCallsign)
          }
        val payload =
          JSONObject().apply {
            put("email", cleanEmail)
            put("password", password)
            put("data", meta)
          }
        val payloadStr = payload.toString()
        executeWithRetry {
          Request.Builder()
            .url("${baseUrl()}/auth/v1/signup")
            .withSupabaseHeaders()
            .header("Content-Type", "application/json")
            .post(payloadStr.toRequestBody("application/json".toMediaType()))
            .build()
        }.use { resp ->
          val raw = resp.body?.string().orEmpty()
          if (!resp.isSuccessful) {
            return@withContext SupabaseAuthResult.Error(parseSupabaseError(raw, resp.code))
          }
          val json = JSONObject(raw)
          val accessToken = json.optString("access_token")
          if (accessToken.isBlank()) {
            return@withContext SupabaseAuthResult.EmailConfirmationRequired(
              email = cleanEmail,
              message = "Account created! Check $cleanEmail to confirm your email, then sign in.",
            )
          }
          val parsed =
            parseSessionJson(json, fallbackEmail = cleanEmail, fallbackCallsign = cleanCallsign)
              ?: return@withContext SupabaseAuthResult.EmailConfirmationRequired(
                email = cleanEmail,
                message = "Account registered. Please verify your email and sign in.",
              )
          persistSession(parsed)
          SupabaseAuthResult.Success(parsed)
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Supabase sign-up failed", e)
        SupabaseAuthResult.Error(e.localizedMessage ?: "Network error during registration.")
      }
    }

  suspend fun launchGoogleOAuthInBrowser(activity: Activity): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        if (!isConfigured()) {
          error("Supabase backend not configured yet. Set SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets.")
        }
        val verifier = generatePkceVerifier()
        val challenge = computePkceChallenge(verifier)
        val state = generatePkceVerifier().take(24)
        context.supabaseAuthStore.edit { prefs ->
          prefs[Keys.PKCE_VERIFIER] = verifier
          prefs[Keys.PKCE_STATE] = state
        }
        val encodedRedirect = URLEncoder.encode(OAUTH_REDIRECT_URI, "UTF-8")
        val authUrl =
          "${baseUrl()}/auth/v1/authorize" +
            "?provider=google" +
            "&redirect_to=$encodedRedirect" +
            "&code_challenge=$challenge" +
            "&code_challenge_method=s256" +
            "&state=$state" +
            "&prompt=select_account"
        val authUri = Uri.parse(authUrl)
        withContext(Dispatchers.Main) {
          val customTabsLaunched =
            runCatching {
              val customTabs = CustomTabsIntent.Builder().setShowTitle(true).build()
              customTabs.intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
              customTabs.launchUrl(activity, authUri)
            }.isSuccess
          if (!customTabsLaunched) {
            activity.startActivity(
              Intent(Intent.ACTION_VIEW, authUri).addCategory(Intent.CATEGORY_BROWSABLE)
            )
          }
        }
      }
    }

  suspend fun handleOAuthCallbackUri(uri: Uri): SupabaseAuthResult =
    withContext(Dispatchers.IO) {
      if (!isConfigured()) {
        return@withContext SupabaseAuthResult.Error("Supabase backend not configured.")
      }
      val queryParams = parseUrlParams(uri.encodedQuery)
      val fragmentParams = parseUrlParams(uri.encodedFragment)
      val allParams = fragmentParams + queryParams

      val oauthErr = allParams["error_description"] ?: allParams["error"]
      if (!oauthErr.isNullOrBlank()) {
        return@withContext SupabaseAuthResult.Error("Google OAuth failed: $oauthErr")
      }

      val prefs = context.supabaseAuthStore.data.first()
      val savedState = prefs[Keys.PKCE_STATE].orEmpty()
      val returnedState = allParams["state"].orEmpty()
      if (savedState.isNotBlank() && returnedState.isNotBlank() && savedState != returnedState) {
        return@withContext SupabaseAuthResult.Error("OAuth state mismatch. Please retry Google Browser Sign-In.")
      }

      val authCode = allParams["code"].orEmpty()
      if (authCode.isNotBlank()) {
        val verifier = prefs[Keys.PKCE_VERIFIER].orEmpty()
        if (verifier.isBlank()) {
          return@withContext SupabaseAuthResult.Error("Missing PKCE verifier. Please tap Continue with Google again.")
        }
        return@withContext try {
          val payloadStr =
            JSONObject().apply {
              put("auth_code", authCode)
              put("code_verifier", verifier)
            }.toString()
          executeWithRetry {
            Request.Builder()
              .url("${baseUrl()}/auth/v1/token?grant_type=pkce")
              .withSupabaseHeaders()
              .header("Content-Type", "application/json")
              .post(payloadStr.toRequestBody("application/json".toMediaType()))
              .build()
          }.use { resp ->
            val raw = resp.body?.string().orEmpty()
            context.supabaseAuthStore.edit {
              it.remove(Keys.PKCE_VERIFIER)
              it.remove(Keys.PKCE_STATE)
            }
            if (!resp.isSuccessful) {
              return@withContext SupabaseAuthResult.Error(parseSupabaseError(raw, resp.code))
            }
            val parsed = parseSessionJson(JSONObject(raw), fallbackEmail = "", fallbackCallsign = "")
              ?: return@withContext SupabaseAuthResult.Error("Supabase PKCE exchange returned an incomplete session.")
            persistSession(parsed)
            val healed = healOversizedJwtIfNeeded(parsed)
            SupabaseAuthResult.Success(healed)
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.e(TAG, "Supabase PKCE token exchange failed", e)
          SupabaseAuthResult.Error(e.localizedMessage ?: "Failed to complete Google Browser Sign-In.")
        }
      }

      val implicitToken = allParams["access_token"].orEmpty()
      if (implicitToken.isNotBlank()) {
        val refreshToken = allParams["refresh_token"].orEmpty()
        val expiresIn = allParams["expires_in"]?.toLongOrNull() ?: 3600L
        val expiresAt = System.currentTimeMillis() / 1000L + expiresIn
        return@withContext try {
          executeWithRetry {
            Request.Builder()
              .url("${baseUrl()}/auth/v1/user")
              .withSupabaseHeaders(implicitToken)
              .get()
              .build()
          }.use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
              return@withContext SupabaseAuthResult.Error(parseSupabaseError(raw, resp.code))
            }
            val userObj = JSONObject(raw)
            val uid = userObj.optString("id")
            if (uid.isBlank()) {
              return@withContext SupabaseAuthResult.Error("Google Browser Sign-In returned no user ID.")
            }
            val email = userObj.optString("email")
            val meta = userObj.optJSONObject("user_metadata")
            val cs =
              meta?.optString("callsign").orEmpty()
                .ifBlank { meta?.optString("full_name").orEmpty() }
                .ifBlank { meta?.optString("name").orEmpty() }
                .ifBlank { email.substringBefore("@").uppercase() }
            val session =
              SupabaseSession(
                accessToken = implicitToken,
                refreshToken = refreshToken,
                expiresAtEpochSec = expiresAt,
                user = SupabaseUser(
                  id = uid,
                  email = email,
                  callsign = cs,
                  role = userObj.optString("role", "authenticated").ifBlank { "authenticated" },
                ),
              )
            persistSession(session)
            val healed = healOversizedJwtIfNeeded(session)
            SupabaseAuthResult.Success(healed)
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.e(TAG, "Supabase implicit OAuth hydration failed", e)
          SupabaseAuthResult.Error(e.localizedMessage ?: "Failed to complete Google Browser Sign-In.")
        }
      }

      SupabaseAuthResult.Error("No authorization code or session token returned by Google OAuth.")
    }

  suspend fun resendConfirmationEmail(email: String): Result<String> =
    withContext(Dispatchers.IO) {
      runCatching {
        if (!isConfigured()) error("Supabase not configured in Secrets panel.")
        val cleanEmail = email.trim().lowercase()
        if (!isValidEmail(cleanEmail)) error("Enter a valid email address first.")
        val payloadStr =
          JSONObject().apply {
            put("type", "signup")
            put("email", cleanEmail)
          }.toString()
        executeWithRetry {
          Request.Builder()
            .url("${baseUrl()}/auth/v1/resend")
            .withSupabaseHeaders()
            .header("Content-Type", "application/json")
            .post(payloadStr.toRequestBody("application/json".toMediaType()))
            .build()
        }.use { resp ->
          val raw = resp.body?.string().orEmpty()
          if (!resp.isSuccessful) error(parseSupabaseError(raw, resp.code))
          "Verification email resent to $cleanEmail. Check your inbox and spam folder."
        }
      }
    }

  private suspend fun requireActiveSession(): SupabaseSession {
    val current = _sessionFlow.value
    val nowSec = System.currentTimeMillis() / 1000L
    if (current != null && (current.expiresAtEpochSec <= 0L || current.expiresAtEpochSec > nowSec + 30L)) {
      return current
    }
    return refreshSupabaseSession() ?: error("Supabase session expired or not signed in.")
  }

  suspend fun getSupabaseUser(): Result<SupabaseUser> =
    withContext(Dispatchers.IO) {
      runCatching {
        if (!isConfigured()) error("Supabase not configured.")
        val session = requireActiveSession()
        executeWithRetry {
          Request.Builder()
            .url("${baseUrl()}/auth/v1/user")
            .withSupabaseHeaders(session.accessToken)
            .get()
            .build()
        }.use { resp ->
          val raw = resp.body?.string().orEmpty()
          if (!resp.isSuccessful) error(parseSupabaseError(raw, resp.code))
          val userObj = JSONObject(raw)
          val meta = userObj.optJSONObject("user_metadata")
          val cs =
            meta?.optString("callsign").orEmpty()
              .ifBlank { meta?.optString("display_name").orEmpty() }
              .ifBlank { session.user.callsign }
          val updatedUser =
            SupabaseUser(
              id = userObj.optString("id", session.user.id),
              email = userObj.optString("email", session.user.email),
              callsign = cs,
              role = userObj.optString("role", session.user.role),
            )
          persistSession(session.copy(user = updatedUser))
          updatedUser
        }
      }
    }

  suspend fun updateSupabaseCallsign(callsign: String): Result<SupabaseUser> =
    withContext(Dispatchers.IO) {
      runCatching {
        if (!isConfigured()) error("Supabase not configured.")
        val session = requireActiveSession()
        val cleanCallsign = callsign.trim().take(32).ifBlank { session.user.callsign }
        val payloadStr =
          JSONObject().apply {
            put(
              "data",
              JSONObject().apply {
                put("callsign", cleanCallsign)
                put("display_name", cleanCallsign)
              },
            )
          }.toString()
        executeWithRetry {
          Request.Builder()
            .url("${baseUrl()}/auth/v1/user")
            .withSupabaseHeaders(session.accessToken)
            .header("Content-Type", "application/json")
            .put(payloadStr.toRequestBody("application/json".toMediaType()))
            .build()
        }.use { resp ->
          val raw = resp.body?.string().orEmpty()
          if (!resp.isSuccessful) error(parseSupabaseError(raw, resp.code))
          val nextUser = session.user.copy(callsign = cleanCallsign)
          persistSession(session.copy(user = nextUser))
          nextUser
        }
      }
    }

  suspend fun saveCloudBackup(backupJson: String): Result<Long> =
    withContext(Dispatchers.IO) {
      runCatching {
        if (!isConfigured()) error("Supabase not configured in AI Studio Secrets.")
        val session = requireActiveSession()
        val nowMs = System.currentTimeMillis()
        // 1. Prefer dedicated PostgREST table so JWT user_metadata never bloats past Cloudflare header limits (HTTP 520).
        val tablePayload =
          JSONObject().apply {
            put("user_id", session.user.id)
            put("backup_json", backupJson)
            put("updated_at_ms", nowMs)
          }.toString()
        val postgrestOk =
          runCatching {
            executeWithRetry {
              Request.Builder()
                .url("${baseUrl()}/rest/v1/rider_backups?on_conflict=user_id")
                .withSupabaseHeaders(session.accessToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(tablePayload.toRequestBody("application/json".toMediaType()))
                .build()
            }.use { it.isSuccessful }
          }.getOrDefault(false)
        if (postgrestOk) return@runCatching nowMs

        // 2. Tiny metadata pointer fallback (<= 450 chars) so Auth JWT NEVER bloats past Cloudflare limits (HTTP 520).
        val metadataSafeJson = compactBackupForAuthMetadata(backupJson, maxChars = 450)
        val payloadStr =
          JSONObject().apply {
            put(
              "data",
              JSONObject().apply {
                put("volttrail_backup", metadataSafeJson)
                put("volttrail_backup_at", nowMs)
              },
            )
          }.toString()
        executeWithRetry {
          Request.Builder()
            .url("${baseUrl()}/auth/v1/user")
            .withSupabaseHeaders(session.accessToken)
            .header("Content-Type", "application/json")
            .put(payloadStr.toRequestBody("application/json".toMediaType()))
            .build()
        }.use { resp ->
          val raw = resp.body?.string().orEmpty()
          if (!resp.isSuccessful) error(parseSupabaseError(raw, resp.code))
          nowMs
        }
      }
    }

  suspend fun loadCloudBackup(): Result<Pair<String, Long>> =
    withContext(Dispatchers.IO) {
      runCatching {
        if (!isConfigured()) error("Supabase not configured in AI Studio Secrets.")
        val session = requireActiveSession()
        val encodedUid = URLEncoder.encode(session.user.id, "UTF-8")
        // 1. Check dedicated PostgREST rider_backups table first
        val fromTable =
          runCatching {
            executeWithRetry {
              Request.Builder()
                .url("${baseUrl()}/rest/v1/rider_backups?user_id=eq.$encodedUid&select=backup_json,updated_at_ms&limit=1")
                .withSupabaseHeaders(session.accessToken)
                .get()
                .build()
            }.use { resp ->
              if (!resp.isSuccessful) return@use null
              val arr = JSONArray(resp.body?.string().orEmpty())
              val first = arr.optJSONObject(0) ?: return@use null
              val json = first.optString("backup_json")
              if (json.isBlank()) null else json to first.optLong("updated_at_ms", 0L)
            }
          }.getOrNull()
        if (fromTable != null) return@runCatching fromTable

        // 2. Fallback to compact user_metadata backup
        executeWithRetry {
          Request.Builder()
            .url("${baseUrl()}/auth/v1/user")
            .withSupabaseHeaders(session.accessToken)
            .get()
            .build()
        }.use { resp ->
          val raw = resp.body?.string().orEmpty()
          if (!resp.isSuccessful) error(parseSupabaseError(raw, resp.code))
          val userObj = JSONObject(raw)
          val meta = userObj.optJSONObject("user_metadata")
          val backupJson = meta?.optString("volttrail_backup").orEmpty()
          if (backupJson.isBlank()) error("No Supabase cloud backup found for this account yet.")
          val ts = meta?.optLong("volttrail_backup_at", 0L) ?: 0L
          backupJson to ts
        }
      }
    }

  suspend fun refreshSupabaseSession(): SupabaseSession? =
    withContext(Dispatchers.IO) {
      val currentRefresh =
        _sessionFlow.value?.refreshToken?.takeIf { it.isNotBlank() }
          ?: context.supabaseAuthStore.data.first()[Keys.REFRESH_TOKEN].orEmpty()
      if (currentRefresh.isBlank() || !isConfigured()) return@withContext null
      refreshSupabaseSessionInternal(currentRefresh)
    }

  private suspend fun refreshSupabaseSessionInternal(refreshToken: String): SupabaseSession? =
    try {
      val payloadStr = JSONObject().apply { put("refresh_token", refreshToken) }.toString()
      executeWithRetry {
        Request.Builder()
          .url("${baseUrl()}/auth/v1/token?grant_type=refresh_token")
          .withSupabaseHeaders()
          .header("Content-Type", "application/json")
          .post(payloadStr.toRequestBody("application/json".toMediaType()))
          .build()
      }.use { resp ->
        if (!resp.isSuccessful) {
          if (resp.code == 400 || resp.code == 401) {
            Log.w(TAG, "Supabase refresh token invalid/expired (${resp.code}); clearing stored session.")
            context.supabaseAuthStore.edit { it.clear() }
            _sessionFlow.value = null
          }
          return@use null
        }
        val json = JSONObject(resp.body?.string().orEmpty())
        val parsed = parseSessionJson(json, fallbackEmail = "", fallbackCallsign = "") ?: return@use null
        persistSession(parsed)
        parsed
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(TAG, "Supabase session refresh skipped: ${e.message}")
      null
    }

  suspend fun sendSupabasePasswordReset(email: String): Result<String> =
    withContext(Dispatchers.IO) {
      runCatching {
        if (!isConfigured()) error("Supabase not configured in Secrets panel.")
        val cleanEmail = email.trim().lowercase()
        if (!isValidEmail(cleanEmail)) error("Enter a valid email address first.")
        val payloadStr = JSONObject().apply { put("email", cleanEmail) }.toString()
        executeWithRetry {
          Request.Builder()
            .url("${baseUrl()}/auth/v1/recover")
            .withSupabaseHeaders()
            .header("Content-Type", "application/json")
            .post(payloadStr.toRequestBody("application/json".toMediaType()))
            .build()
        }.use { resp ->
          val raw = resp.body?.string().orEmpty()
          if (!resp.isSuccessful) error(parseSupabaseError(raw, resp.code))
          "Password reset instructions sent to $cleanEmail"
        }
      }
    }

  suspend fun checkSupabaseHealth(): ServiceHealthResult =
    withContext(Dispatchers.IO) {
      if (!isConfigured()) {
        return@withContext ServiceHealthResult(true, 0L, "Firebase Auth primary (zero client secrets)")
      }
      val start = System.currentTimeMillis()
      try {
        executeWithRetry {
          Request.Builder()
            .url("${baseUrl()}/auth/v1/settings")
            .withSupabaseHeaders()
            .get()
            .build()
        }.use { resp ->
          val elapsed = System.currentTimeMillis() - start
          if (resp.isSuccessful) {
            return@withContext ServiceHealthResult(true, elapsed, "GoTrue Healthy (${resp.code})")
          }
          val raw = resp.body?.string().orEmpty()
          ServiceHealthResult(false, elapsed, parseSupabaseError(raw, resp.code))
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        ServiceHealthResult(
          false,
          System.currentTimeMillis() - start,
          e.localizedMessage ?: "Unreachable",
        )
      }
    }

  suspend fun signOutSupabase() =
    withContext(Dispatchers.IO) {
      val token = _sessionFlow.value?.accessToken.orEmpty()
      if (token.isNotBlank() && isConfigured()) {
        runCatching {
          val req =
            Request.Builder()
              .url("${baseUrl()}/auth/v1/logout")
              .withSupabaseHeaders(token)
              .post("{}".toRequestBody("application/json".toMediaType()))
              .build()
          httpClient.newCall(req).execute().close()
        }
      }
      context.supabaseAuthStore.edit { it.clear() }
      _sessionFlow.value = null
    }

  private suspend fun persistSession(session: SupabaseSession) {
    context.supabaseAuthStore.edit { prefs ->
      prefs[Keys.ACCESS_TOKEN] = session.accessToken
      prefs[Keys.REFRESH_TOKEN] = session.refreshToken
      prefs[Keys.EXPIRES_AT] = session.expiresAtEpochSec
      prefs[Keys.USER_ID] = session.user.id
      prefs[Keys.USER_EMAIL] = session.user.email
      prefs[Keys.USER_CALLSIGN] = session.user.callsign
      prefs[Keys.USER_ROLE] = session.user.role
    }
    _sessionFlow.value = session
  }

  companion object {
    const val OAUTH_REDIRECT_URI = "ruralerides://auth/callback"

    fun generatePkceVerifier(): String {
      val bytes = ByteArray(32)
      SecureRandom().nextBytes(bytes)
      return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun computePkceChallenge(verifier: String): String {
      val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
      return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun parseUrlParams(rawQueryOrFragment: String?): Map<String, String> {
      val raw = rawQueryOrFragment?.trim()?.trimStart('?', '#').orEmpty()
      if (raw.isBlank()) return emptyMap()
      return raw.split('&').mapNotNull { pair ->
        val idx = pair.indexOf('=')
        if (idx <= 0) null
        else {
          val k = runCatching { URLDecoder.decode(pair.substring(0, idx), "UTF-8") }.getOrDefault(pair.substring(0, idx))
          val v = runCatching { URLDecoder.decode(pair.substring(idx + 1), "UTF-8") }.getOrDefault(pair.substring(idx + 1))
          k to v
        }
      }.toMap()
    }
    const val SUPABASE_SCHEMA_SQL: String = """
create table if not exists public.board_posts (
  id text primary key default gen_random_uuid()::text,
  user_id text not null,
  author_callsign text not null,
  author_badge text default 'Pilot',
  category text default 'UNIVERSAL',
  title text not null,
  body text not null,
  image_url text default '',
  poll_question text default '',
  poll_options text[] default '{}',
  price_usd double precision default 0,
  location_label text default '',
  telemetry_summary text default '',
  help_needed boolean default false,
  speed_trap boolean default false,
  stolen_alert boolean default false,
  likes_count bigint default 0,
  comments_count bigint default 0,
  flagged_count bigint default 0,
  created_at timestamptz default now()
);
alter table public.board_posts add column if not exists poll_question text default '';
alter table public.board_posts add column if not exists poll_options text[] default '{}';
alter table public.board_posts add column if not exists flagged_count bigint default 0;

create table if not exists public.board_comments (
  id text primary key default gen_random_uuid()::text,
  post_id text not null,
  user_id text not null,
  author_callsign text not null,
  body text not null,
  created_at timestamptz default now()
);
create table if not exists public.pilot_profiles (
  user_id text primary key,
  callsign text not null,
  rank_badge text default 'Rookie Pilot',
  xp_points bigint default 50,
  total_km double precision default 0,
  rides_count bigint default 0,
  verified boolean default false,
  banned boolean default false,
  trust_score bigint default 100,
  vehicle_summary text default '',
  visibility text default 'PUBLIC',
  avatar_base64 text default '',
  updated_at timestamptz default now()
);
create table if not exists public.radar_riders (
  user_id text primary key,
  callsign text not null,
  vehicle_name text default 'PEV Rig',
  lat double precision default 0,
  lng double precision default 0,
  speed_kmh double precision default 0,
  heading_deg double precision default 0,
  battery_pct bigint default 100,
  status_tag text default 'CRUISING',
  visibility text default 'PUBLIC',
  updated_at timestamptz default now()
);
create table if not exists public.radar_meetups (
  id text primary key default gen_random_uuid()::text,
  user_id text not null,
  creator_callsign text not null,
  title text not null,
  description text default '',
  scheduled_time text default '',
  lat double precision default 0,
  lng double precision default 0,
  attendee_count bigint default 1,
  visibility text default 'PUBLIC',
  created_at timestamptz default now()
);
create table if not exists public.radar_hazards (
  id text primary key default gen_random_uuid()::text,
  user_id text not null,
  creator_callsign text not null,
  hazard_type text default 'POTHOLE',
  description text default '',
  lat double precision default 0,
  lng double precision default 0,
  upvotes bigint default 1,
  visibility text default 'PUBLIC',
  created_at timestamptz default now()
);
create table if not exists public.radar_waypoints (
  id text primary key default gen_random_uuid()::text,
  user_id text not null,
  creator_callsign text not null,
  waypoint_type text default 'CHARGER',
  title text not null,
  notes text default '',
  lat double precision default 0,
  lng double precision default 0,
  visibility text default 'PUBLIC',
  created_at timestamptz default now()
);
create table if not exists public.dm_threads (
  id text primary key,
  participant_uids text[] not null,
  participant_callsigns text[] not null,
  last_message text default '',
  self_destruct_sec bigint default 0,
  updated_at timestamptz default now()
);
create table if not exists public.dm_messages (
  id text primary key default gen_random_uuid()::text,
  thread_id text not null,
  participant_uids text[] not null,
  sender_id text not null,
  sender_callsign text not null,
  text text default '',
  image_url text default '',
  voice_base64 text default '',
  self_destruct_sec bigint default 0,
  created_at timestamptz default now()
);
create table if not exists public.walkie_presence (
  id text primary key,
  user_id text not null,
  callsign text not null,
  channel_id text not null,
  active boolean default true,
  updated_at timestamptz default now()
);
create table if not exists public.walkie_channel_state (
  channel_id text primary key,
  speaker_uid text default '',
  speaker_callsign text default '',
  active boolean default false,
  updated_at timestamptz default now()
);
create table if not exists public.walkie_logs (
  id text primary key default gen_random_uuid()::text,
  user_id text not null,
  sender_callsign text not null,
  channel_id text not null,
  audio_base64 text not null,
  duration_ms bigint default 1000,
  active boolean default true,
  created_at timestamptz default now()
);
create table if not exists public.walkie_signals (
  id text primary key default gen_random_uuid()::text,
  sender_id text not null,
  sender_callsign text not null,
  target_uid text default 'ALL',
  channel_id text not null,
  signal_type text not null,
  payload text default '',
  active boolean default true,
  created_at timestamptz default now()
);
create table if not exists public.call_sessions (
  id text primary key default gen_random_uuid()::text,
  caller_uid text not null,
  caller_callsign text not null,
  callee_uid text not null,
  callee_callsign text not null,
  participant_uids text[] not null,
  call_type text default 'AUDIO',
  status text default 'RINGING',
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);
create table if not exists public.call_signals (
  id text primary key default gen_random_uuid()::text,
  session_id text not null,
  sender_id text not null,
  signal_type text not null,
  payload text default '',
  created_at timestamptz default now()
);
create table if not exists public.youtube_engagement (
  video_id text primary key,
  volts_count bigint default 0,
  updated_at timestamptz default now()
);
create table if not exists public.youtube_comments (
  id text primary key default gen_random_uuid()::text,
  video_id text not null,
  user_id text not null,
  author_callsign text not null,
  text text not null,
  created_at timestamptz default now()
);
create table if not exists public.rider_backups (
  user_id text primary key,
  backup_json text not null,
  updated_at_ms bigint not null
);

alter table public.board_posts enable row level security;
alter table public.board_comments enable row level security;
alter table public.pilot_profiles enable row level security;
alter table public.radar_riders enable row level security;
alter table public.radar_meetups enable row level security;
alter table public.radar_hazards enable row level security;
alter table public.radar_waypoints enable row level security;
alter table public.dm_threads enable row level security;
alter table public.dm_messages enable row level security;
alter table public.walkie_presence enable row level security;
alter table public.walkie_channel_state enable row level security;
alter table public.walkie_logs enable row level security;
alter table public.walkie_signals enable row level security;
alter table public.call_sessions enable row level security;
alter table public.call_signals enable row level security;
alter table public.youtube_engagement enable row level security;
alter table public.youtube_comments enable row level security;
alter table public.rider_backups enable row level security;

drop policy if exists "public_read_posts" on public.board_posts;
create policy "public_read_posts" on public.board_posts for select to anon, authenticated using (true);
drop policy if exists "auth_rw_posts" on public.board_posts;
create policy "auth_rw_posts" on public.board_posts for all to authenticated using (true) with check (true);
drop policy if exists "public_read_comments" on public.board_comments;
create policy "public_read_comments" on public.board_comments for select to anon, authenticated using (true);
drop policy if exists "auth_rw_comments" on public.board_comments;
create policy "auth_rw_comments" on public.board_comments for all to authenticated using (true) with check (true);
drop policy if exists "public_read_pilots" on public.pilot_profiles;
create policy "public_read_pilots" on public.pilot_profiles for select to anon, authenticated using (true);
drop policy if exists "auth_rw_pilots" on public.pilot_profiles;
create policy "auth_rw_pilots" on public.pilot_profiles for all to authenticated using (true) with check (true);
drop policy if exists "auth_rw_radar_riders" on public.radar_riders;
create policy "auth_rw_radar_riders" on public.radar_riders for all to authenticated using (true) with check (true);
drop policy if exists "auth_rw_radar_meetups" on public.radar_meetups;
create policy "auth_rw_radar_meetups" on public.radar_meetups for all to authenticated using (true) with check (true);
drop policy if exists "auth_rw_radar_hazards" on public.radar_hazards;
create policy "auth_rw_radar_hazards" on public.radar_hazards for all to authenticated using (true) with check (true);
drop policy if exists "auth_rw_radar_waypoints" on public.radar_waypoints;
create policy "auth_rw_radar_waypoints" on public.radar_waypoints for all to authenticated using (true) with check (true);
drop policy if exists "auth_rw_dm_threads" on public.dm_threads;
create policy "auth_rw_dm_threads" on public.dm_threads for all to authenticated using (true) with check (true);
drop policy if exists "auth_rw_dm_messages" on public.dm_messages;
create policy "auth_rw_dm_messages" on public.dm_messages for all to authenticated using (true) with check (true);
drop policy if exists "auth_rw_walkie_presence" on public.walkie_presence;
create policy "auth_rw_walkie_presence" on public.walkie_presence for all to authenticated using (true) with check (true);
drop policy if exists "auth_rw_walkie_state" on public.walkie_channel_state;
create policy "auth_rw_walkie_state" on public.walkie_channel_state for all to authenticated using (true) with check (true);
drop policy if exists "auth_rw_walkie_logs" on public.walkie_logs;
create policy "auth_rw_walkie_logs" on public.walkie_logs for all to authenticated using (true) with check (true);
drop policy if exists "auth_rw_walkie_signals" on public.walkie_signals;
create policy "auth_rw_walkie_signals" on public.walkie_signals for all to authenticated using (true) with check (true);
drop policy if exists "auth_rw_call_sessions" on public.call_sessions;
create policy "auth_rw_call_sessions" on public.call_sessions for all to authenticated using (true) with check (true);
drop policy if exists "auth_rw_call_signals" on public.call_signals;
create policy "auth_rw_call_signals" on public.call_signals for all to authenticated using (true) with check (true);
drop policy if exists "auth_rw_yt_eng" on public.youtube_engagement;
create policy "auth_rw_yt_eng" on public.youtube_engagement for all to authenticated using (true) with check (true);
drop policy if exists "auth_rw_yt_comments" on public.youtube_comments;
create policy "auth_rw_yt_comments" on public.youtube_comments for all to authenticated using (true) with check (true);
drop policy if exists "owner_rw_backups" on public.rider_backups;
create policy "owner_rw_backups" on public.rider_backups for all to authenticated using (auth.uid()::text = user_id) with check (auth.uid()::text = user_id);
"""

    fun compactBackupForAuthMetadata(rawJson: String, maxChars: Int = 2350): String {
      if (rawJson.length <= maxChars) return rawJson
      return runCatching {
        val root = JSONObject(rawJson)
        val out = JSONObject()
        if (root.has("version")) out.put("version", root.opt("version"))
        if (root.has("uid")) out.put("uid", root.optString("uid"))
        if (root.has("exportedAtMs")) out.put("exportedAtMs", root.optLong("exportedAtMs"))

        // Preserve vehicles first (most valuable compact user data)
        val vehicles = root.optJSONArray("vehicles")
        if (vehicles != null) {
          val candidate = JSONObject(out.toString()).apply { put("vehicles", vehicles) }
          if (candidate.toString().length <= maxChars) {
            out.put("vehicles", vehicles)
          }
        }

        // Preserve settingsJson or settings object if it fits
        val settingsJsonStr = root.optString("settingsJson", "")
        if (settingsJsonStr.isNotBlank()) {
          val withSettings = JSONObject(out.toString()).apply { put("settingsJson", settingsJsonStr) }
          if (withSettings.toString().length <= maxChars) {
            out.put("settingsJson", settingsJsonStr)
          } else {
            runCatching {
              val sObj = JSONObject(settingsJsonStr).apply {
                remove("sosTemplate")
                remove("quickReplies")
              }
              val withTrimmed = JSONObject(out.toString()).apply { put("settingsJson", sObj.toString()) }
              if (withTrimmed.toString().length <= maxChars) {
                out.put("settingsJson", sObj.toString())
              }
            }
          }
        } else {
          val settingsObj = root.optJSONObject("settings")
          if (settingsObj != null) {
            val withSettings = JSONObject(out.toString()).apply { put("settings", settingsObj) }
            if (withSettings.toString().length <= maxChars) {
              out.put("settings", settingsObj)
            }
          }
        }

        val finalStr = out.toString()
        if (finalStr.length <= maxChars) finalStr else """{"compact":true}"""
      }.getOrElse { """{"compact":true}""" }
    }

    fun sanitizeBaseUrl(url: String): String =
      url
        .trim()
        .trim('"', '\'')
        .trimEnd('/')
        .removeSuffix("/auth/v1")
        .removeSuffix("/rest/v1")
        .trimEnd('/')

    fun sanitizeAnonKey(key: String): String = key.trim().trim('"', '\'')

    fun isValidEmail(email: String): Boolean {
      val clean = email.trim()
      val atIdx = clean.indexOf('@')
      if (atIdx <= 0) return false
      val domain = clean.substring(atIdx + 1)
      return domain.length >= 3 && domain.contains('.') && !domain.startsWith('.') && !domain.endsWith('.')
    }

    fun isEmailNotConfirmedError(raw: String): Boolean {
      if (raw.contains("email_not_confirmed", ignoreCase = true) ||
        raw.contains("Email not confirmed", ignoreCase = true)
      ) {
        return true
      }
      return runCatching {
        val obj = JSONObject(raw)
        obj.optString("error_code").equals("email_not_confirmed", ignoreCase = true) ||
          obj.optString("msg").contains("email not confirmed", ignoreCase = true)
      }.getOrDefault(false)
    }

    fun isJwtToken(token: String): Boolean {
      val t = token.trim()
      return t.isNotBlank() && !t.startsWith("sb_", ignoreCase = true) && t.count { it == '.' } == 2
    }

    fun isConfiguredValues(url: String, key: String): Boolean {
      val u = sanitizeBaseUrl(url)
      val k = sanitizeAnonKey(key)
      return u.startsWith("https://") &&
        !u.contains("YOUR_PROJECT", ignoreCase = true) &&
        k.isNotBlank() &&
        !k.contains("MY_SUPABASE", ignoreCase = true) &&
        !k.contains("YOUR_SUPABASE", ignoreCase = true)
    }

    fun parseSessionJson(
      json: JSONObject,
      fallbackEmail: String = "",
      fallbackCallsign: String = "",
    ): SupabaseSession? {
      val access = json.optString("access_token")
      if (access.isBlank()) return null
      val refresh = json.optString("refresh_token")
      val expiresIn = json.optLong("expires_in", 3600L)
      val expiresAt =
        json.optLong("expires_at", 0L).takeIf { it > 0L }
          ?: (System.currentTimeMillis() / 1000L + expiresIn)

      val userObj = json.optJSONObject("user") ?: return null
      val uid = userObj.optString("id")
      if (uid.isBlank()) return null
      val email = userObj.optString("email", fallbackEmail).ifBlank { fallbackEmail }
      val meta = userObj.optJSONObject("user_metadata")
      val callsign =
        meta?.optString("callsign").orEmpty()
          .ifBlank { meta?.optString("display_name").orEmpty() }
          .ifBlank { fallbackCallsign }
          .ifBlank { email.substringBefore("@").uppercase() }
      val role = userObj.optString("role", "authenticated").ifBlank { "authenticated" }

      return SupabaseSession(
        accessToken = access,
        refreshToken = refresh,
        expiresAtEpochSec = expiresAt,
        user = SupabaseUser(id = uid, email = email, callsign = callsign, role = role),
      )
    }

    fun parseSupabaseError(raw: String, code: Int): String {
      if (code == 520 || (raw.contains("cloudflare", true) && raw.contains("520"))) {
        return "Supabase HTTP 520: Cloudflare origin error. Your Supabase project may be paused or waking up in Supabase Dashboard, or use Google Sign-In below."
      }
      if (code in listOf(502, 503, 504)) {
        return "Supabase gateway unavailable (HTTP $code). Verify project status in Supabase Dashboard or retry."
      }
      return runCatching {
        val obj = JSONObject(raw)
        val errCode = obj.optString("error_code").ifBlank { obj.optString("code") }
        val baseMsg =
          obj.optString("msg")
            .ifBlank { obj.optString("error_description") }
            .ifBlank { obj.optString("message") }
            .ifBlank { obj.optString("error") }
        val hint = obj.optString("hint").takeIf { it.isNotBlank() && it != "null" }.orEmpty()
        val details = obj.optString("details").takeIf { it.isNotBlank() && it != "null" }.orEmpty()

        when (errCode.lowercase()) {
          "email_not_confirmed" ->
            "Email not confirmed yet. Check your inbox or tap Resend Verification Email."
          "invalid_credentials" ->
            "Invalid email or password. Double-check credentials or reset password."
          "user_already_exists" ->
            "An account with this email already exists. Switch to Sign In."
          "weak_password" ->
            baseMsg.ifBlank { "Password is too weak. Use at least 6 characters." }
          "over_email_send_rate_limit" ->
            "Email rate limit reached. Wait a minute before retrying."
          "signup_disabled" ->
            "New email sign-ups are disabled on this Supabase project."
          else -> {
            if (baseMsg.isBlank()) {
              ""
            } else {
              buildString {
                append(baseMsg)
                if (details.isNotBlank() && !baseMsg.contains(details, ignoreCase = true)) {
                  append(" ($details)")
                }
                if (hint.isNotBlank() && !baseMsg.contains(hint, ignoreCase = true)) {
                  append(" — $hint")
                }
              }
            }
          }
        }
      }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Supabase Auth error (HTTP $code)"
    }
  }
}
