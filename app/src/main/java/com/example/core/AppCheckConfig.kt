package com.example.core

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerationConfig
import com.google.firebase.ai.type.SafetySetting
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val TOKEN_STORE_PREFIX = "com.google.firebase.appcheck.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val INJECTED_TOKEN_KEY = "com.example.core.AISTUDIO_INJECTED_DEBUG_TOKEN"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"
private const val DEVICE_ANCHOR_FILENAME = "appcheck_device_anchor_v2.txt"

// Firebase App Check SDK returns a base64 JSON dummy token `{"error":"..."}` when
// attestation fails internally; base64("{\"error\":") starts with "eyJlcnJvciI6".
private const val DUMMY_ERROR_TOKEN_PREFIX = "eyJlcnJvciI6"

private enum class InstalledAppCheckMode {
  NONE,
  DEBUG_INJECTED,
  DEBUG_LOCAL,
  RECAPTCHA,
}

object AppCheckHealthManager {
  private val installLock = Any()
  private val refreshMutex = Mutex()

  @Volatile private var appContext: Context? = null
  @Volatile private var cachedSiteKey: String = ""
  @Volatile private var installedMode: InstalledAppCheckMode = InstalledAppCheckMode.NONE
  @Volatile private var installedDebugToken: String? = null
  @Volatile private var lastErrorDetail: String = ""

  fun activeProviderLabel(): String =
    when (installedMode) {
      InstalledAppCheckMode.RECAPTCHA -> "reCAPTCHA Enterprise"
      InstalledAppCheckMode.DEBUG_INJECTED -> "Debug (Injected Token)"
      InstalledAppCheckMode.DEBUG_LOCAL -> "Debug (Local)"
      InstalledAppCheckMode.NONE -> "Not Initialized"
    }

  internal fun configure(context: Context, intent: Intent?) {
    synchronized(installLock) {
      try {
        val appCtx = context.applicationContext ?: context
        appContext = appCtx

        if (FirebaseApp.getApps(appCtx).isEmpty()) {
          FirebaseApp.initializeApp(appCtx)
        }

        val firebaseApp = FirebaseApp.getInstance()
        val persistenceKey = firebaseApp.persistenceKey
        val debugPrefs =
          appCtx.getSharedPreferences("$DEBUG_STORE_PREFIX$persistenceKey", Context.MODE_PRIVATE)
        val tokenPrefs =
          appCtx.getSharedPreferences("$TOKEN_STORE_PREFIX$persistenceKey", Context.MODE_PRIVATE)

        // Resolve reCAPTCHA Enterprise Android site key safely
        val resId =
          appCtx.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", appCtx.packageName)
            .takeIf { it != 0 }
            ?: appCtx.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", "com.example")
        val siteKey = if (resId != 0) appCtx.getString(resId).trim() else ""
        cachedSiteKey = siteKey

        // Protect tablets/restored devices from stale cross-device backed-up App Check prefs.
        // Android NEVER backs up or transfers `noBackupFilesDir`, so a missing anchor file
        // means a fresh install or a cloud/device-to-device restore (e.g. phone -> tablet).
        val intentDebugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)?.trim().orEmpty()
        val anchorFile = File(appCtx.noBackupFilesDir, DEVICE_ANCHOR_FILENAME)
        if (!anchorFile.exists()) {
          tokenPrefs.edit().clear().commit()
          if (intentDebugToken.isEmpty()) {
            debugPrefs.edit().clear().commit()
          }
          runCatching { anchorFile.writeText("v2:${System.currentTimeMillis()}") }
          Log.d(TAG, "Cleared restored App Check caches on fresh/restored device install.")
        }

        val appCheck = FirebaseAppCheck.getInstance()

        if (BuildConfig.DEBUG) {
          if (intentDebugToken.isNotEmpty()) {
            debugPrefs
              .edit()
              .putString(DEBUG_SECRET_KEY, intentDebugToken)
              .putString(INJECTED_TOKEN_KEY, intentDebugToken)
              .commit()
            Log.d(TAG, "App Check debug secret configured synchronously from intent.")
          }

          val activeInjectedToken = debugPrefs.getString(INJECTED_TOKEN_KEY, null)?.trim().orEmpty()
          if (activeInjectedToken.isNotEmpty()) {
            if (debugPrefs.getString(DEBUG_SECRET_KEY, null) != activeInjectedToken) {
              debugPrefs.edit().putString(DEBUG_SECRET_KEY, activeInjectedToken).commit()
            }
            if (installedMode != InstalledAppCheckMode.DEBUG_INJECTED || installedDebugToken != activeInjectedToken) {
              appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance(),
                true,
              )
              installedMode = InstalledAppCheckMode.DEBUG_INJECTED
              installedDebugToken = activeInjectedToken
              Log.d(TAG, "DebugAppCheckProviderFactory installed with provisioned token.")
              warmUpTokenAsync(appCheck)
            }
          } else if (siteKey.isNotBlank()) {
            // Physical phone/tablet running debug APK without an emulator-injected debug token:
            // use RecaptchaAppCheckProviderFactory so App Check tokens validate cleanly.
            if (debugPrefs.contains(DEBUG_SECRET_KEY)) {
              debugPrefs.edit().remove(DEBUG_SECRET_KEY).commit()
            }
            if (installedMode != InstalledAppCheckMode.RECAPTCHA) {
              appCheck.installAppCheckProviderFactory(
                RecaptchaAppCheckProviderFactory.getInstance(siteKey),
                true,
              )
              installedMode = InstalledAppCheckMode.RECAPTCHA
              installedDebugToken = null
              Log.d(TAG, "RecaptchaAppCheckProviderFactory installed on debug device.")
              warmUpTokenAsync(appCheck)
            }
          } else if (installedMode != InstalledAppCheckMode.DEBUG_LOCAL) {
            appCheck.installAppCheckProviderFactory(
              DebugAppCheckProviderFactory.getInstance(),
              true,
            )
            installedMode = InstalledAppCheckMode.DEBUG_LOCAL
            installedDebugToken = null
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")
            warmUpTokenAsync(appCheck)
          }
        } else {
          // Release build: clear any leftover debug secret from earlier debug installs
          if (debugPrefs.all.isNotEmpty()) {
            debugPrefs.edit().clear().commit()
          }
          if (siteKey.isNotBlank()) {
            if (installedMode != InstalledAppCheckMode.RECAPTCHA) {
              appCheck.installAppCheckProviderFactory(
                RecaptchaAppCheckProviderFactory.getInstance(siteKey),
                true,
              )
              installedMode = InstalledAppCheckMode.RECAPTCHA
              installedDebugToken = null
              Log.d(TAG, "RecaptchaAppCheckProviderFactory installed (Release).")
              warmUpTokenAsync(appCheck)
            }
          } else {
            Log.e(TAG, "recaptcha_site_key resource string is blank.")
          }
        }
      } catch (e: Exception) {
        lastErrorDetail = e.message ?: e::class.java.simpleName
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
      }
    }
  }

  private fun warmUpTokenAsync(appCheck: FirebaseAppCheck) {
    runCatching {
      appCheck
        .getAppCheckToken(false)
        .addOnSuccessListener { result ->
          val tok = result?.token.orEmpty()
          if (tok.isNotBlank() && !isDummyErrorToken(tok)) {
            lastErrorDetail = ""
            Log.d(TAG, "App Check warm-up token ready (${activeProviderLabel()}).")
          } else {
            Log.w(TAG, "App Check warm-up returned dummy/empty token; forcing refresh.")
            appCheck.getAppCheckToken(true)
          }
        }
        .addOnFailureListener { err ->
          lastErrorDetail = err.message ?: err::class.java.simpleName
          Log.w(TAG, "App Check initial warm-up failed (${activeProviderLabel()}): $lastErrorDetail")
        }
    }
  }

  fun isDummyErrorToken(token: String?): Boolean {
    if (token.isNullOrBlank()) return true
    if (token.startsWith(DUMMY_ERROR_TOKEN_PREFIX)) return true
    // Real App Check tokens are 3-segment JWTs (header.payload.signature)
    return token.count { it == '.' } < 2
  }

  fun isAppCheckError(t: Throwable): Boolean {
    var cursor: Throwable? = t
    var depth = 0
    while (cursor != null && depth < 8) {
      val cls = cursor::class.java.simpleName.lowercase()
      val msg = "${cursor.message.orEmpty()} ${cursor.localizedMessage.orEmpty()}".lowercase()
      if (
        cls.contains("appcheck") ||
          msg.contains("app check") ||
          msg.contains("appcheck") ||
          msg.contains("token is invalid") ||
          msg.contains("invalid app check") ||
          msg.contains("attestation")
      ) {
        return true
      }
      cursor = cursor.cause
      depth++
    }
    return false
  }

  private fun clearStaleTokenCache() {
    val ctx = appContext ?: return
    runCatching {
      val persistenceKey = FirebaseApp.getInstance().persistenceKey
      ctx
        .getSharedPreferences("$TOKEN_STORE_PREFIX$persistenceKey", Context.MODE_PRIVATE)
        .edit()
        .clear()
        .commit()
    }
  }

  private fun switchDebugToRecaptchaIfAvailable(): Boolean {
    val siteKey = cachedSiteKey
    val ctx = appContext ?: return false
    if (siteKey.isBlank()) return false
    synchronized(installLock) {
      if (installedMode == InstalledAppCheckMode.RECAPTCHA) return false
      return runCatching {
        val persistenceKey = FirebaseApp.getInstance().persistenceKey
        ctx
          .getSharedPreferences("$DEBUG_STORE_PREFIX$persistenceKey", Context.MODE_PRIVATE)
          .edit()
          .clear()
          .commit()
        ctx
          .getSharedPreferences("$TOKEN_STORE_PREFIX$persistenceKey", Context.MODE_PRIVATE)
          .edit()
          .clear()
          .commit()
        FirebaseAppCheck.getInstance()
          .installAppCheckProviderFactory(
            RecaptchaAppCheckProviderFactory.getInstance(siteKey),
            true,
          )
        installedMode = InstalledAppCheckMode.RECAPTCHA
        installedDebugToken = null
        Log.w(TAG, "Switched App Check from failing debug provider to RecaptchaAppCheckProviderFactory.")
        true
      }.getOrDefault(false)
    }
  }

  /**
   * Forces a fresh App Check token exchange (serialized across callers so parallel AI requests
   * don't stampede reCAPTCHA). Automatically clears stale cached tokens, falls back from a
   * stale debug token to reCAPTCHA on physical debug devices, and retries once on cold-start
   * tablet wake / Play Services init races.
   */
  suspend fun forceRefreshValidToken(): Result<String> =
    refreshMutex.withLock {
      val appCheck =
        runCatching { FirebaseAppCheck.getInstance() }.getOrElse { err ->
          return@withLock Result.failure(err)
        }

      var lastErr: Throwable? = null
      repeat(2) { attempt ->
        try {
          if (attempt > 0) {
            clearStaleTokenCache()
            delay(650L)
          }
          val res = withTimeout(12_000L) { appCheck.getAppCheckToken(true).await() }
          val tok = res?.token.orEmpty()
          if (!isDummyErrorToken(tok)) {
            lastErrorDetail = ""
            Log.d(TAG, "Forced App Check token refresh succeeded on attempt ${attempt + 1} (${activeProviderLabel()}).")
            return@withLock Result.success(tok)
          }
          lastErr = IllegalStateException("App Check returned an unverified placeholder token.")
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          lastErr = e
          lastErrorDetail = e.message ?: e::class.java.simpleName
          Log.w(TAG, "Forced App Check token refresh attempt ${attempt + 1} failed: $lastErrorDetail")
          // If a physical device is stuck on an expired/stale debug token, switch to reCAPTCHA immediately
          if (installedMode != InstalledAppCheckMode.RECAPTCHA && switchDebugToRecaptchaIfAvailable()) {
            delay(350L)
          }
        }
      }

      Result.failure(lastErr ?: IllegalStateException("Unable to mint a valid Firebase App Check token."))
    }

  private fun buildActionableTabletDiagnostic(underlying: Throwable?): String {
    val ctx = appContext
    val gmsNote =
      if (ctx != null) {
        val code =
          runCatching { GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(ctx) }
            .getOrDefault(ConnectionResult.SUCCESS)
        if (code != ConnectionResult.SUCCESS) {
          val label = GoogleApiAvailability.getInstance().getErrorString(code)
          " Google Play Services status: $label."
        } else ""
      } else ""
    val rawDetail =
      underlying?.message?.takeIf { it.isNotBlank() && !it.contains("token is invalid", ignoreCase = true) }
        ?: lastErrorDetail.takeIf { it.isNotBlank() }
        ?: "reCAPTCHA Enterprise attestation was rejected on this device"
    return "Firebase App Check token failed ($rawDetail).$gmsNote " +
      "On tablets, ensure Google Play Services & Android System WebView are updated, Automatic Date & Time is ON, " +
      "and no VPN/ad-blocker/Private DNS is blocking recaptchaenterprise.googleapis.com, then retry."
  }

  /**
   * Executes a Firebase AI Logic operation with transparent App Check token self-healing.
   * If Firebase rejects the request due to a stale/cold-start App Check token, forces a clean
   * token refresh and retries once automatically.
   */
  suspend fun <T> runWithAppCheckRecovery(
    tag: String,
    block: suspend () -> T,
  ): T {
    try {
      return block()
    } catch (e: CancellationException) {
      throw e
    } catch (firstErr: Exception) {
      if (!isAppCheckError(firstErr)) throw firstErr
      Log.w(tag, "AI call hit App Check error; forcing App Check token recovery: ${firstErr.message}")
      val refresh = forceRefreshValidToken()
      if (refresh.isSuccess) {
        delay(200L)
        return try {
          block()
        } catch (e: CancellationException) {
          throw e
        } catch (retryErr: Exception) {
          if (isAppCheckError(retryErr)) {
            Log.e(tag, "AI call still failed App Check after valid token refresh", retryErr)
            throw IllegalStateException(buildActionableTabletDiagnostic(retryErr), retryErr)
          }
          throw retryErr
        }
      } else {
        val cause = refresh.exceptionOrNull() ?: firstErr
        Log.e(tag, "App Check token recovery failed on device", cause)
        throw IllegalStateException(buildActionableTabletDiagnostic(cause), cause)
      }
    }
  }

  suspend fun probeLiveAppCheck(context: Context): Triple<Boolean, Long, String> {
    val startMs = System.currentTimeMillis()
    val gmsCode =
      runCatching { GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) }
        .getOrDefault(ConnectionResult.SUCCESS)
    val gmsLabel =
      if (gmsCode == ConnectionResult.SUCCESS) "Play Services OK"
      else "Play Services: ${GoogleApiAvailability.getInstance().getErrorString(gmsCode)}"
    val res = forceRefreshValidToken()
    val elapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)
    return res.fold(
      onSuccess = {
        Triple(
          true,
          elapsed,
          "${activeProviderLabel()} token verified • $gmsLabel",
        )
      },
      onFailure = { err ->
        Triple(
          false,
          elapsed,
          "${activeProviderLabel()} failed (${err.message ?: err::class.java.simpleName}) • $gmsLabel",
        )
      },
    )
  }
}

fun configureAppCheck(context: Context, intent: Intent?) {
  AppCheckHealthManager.configure(context, intent)
}
