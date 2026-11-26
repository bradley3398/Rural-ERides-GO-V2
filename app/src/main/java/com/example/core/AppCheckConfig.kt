package com.example.core

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import java.io.File

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val TOKEN_STORE_PREFIX = "com.google.firebase.appcheck.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val INJECTED_TOKEN_KEY = "com.example.core.AISTUDIO_INJECTED_DEBUG_TOKEN"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"
private const val DEVICE_ANCHOR_FILENAME = "appcheck_device_anchor_v2.txt"

private enum class InstalledAppCheckMode {
  NONE,
  DEBUG_INJECTED,
  DEBUG_LOCAL,
  RECAPTCHA,
}

/**
 * Configures Firebase App Check for Firebase services (Firestore/Auth) if enabled in Firebase Console.
 * Gemini AI requests bypass Firebase App Check completely via [com.example.ai.DirectGeminiRestClient]
 * and Google Cloud Console API keys in [ApiKeyRing.geminiKeys].
 */
object AppCheckHealthManager {
  private val installLock = Any()

  @Volatile private var installedMode: InstalledAppCheckMode = InstalledAppCheckMode.NONE
  @Volatile private var installedDebugToken: String? = null

  internal fun configure(context: Context, intent: Intent?) {
    synchronized(installLock) {
      try {
        val appCtx = context.applicationContext ?: context
        if (FirebaseApp.getApps(appCtx).isEmpty()) {
          FirebaseApp.initializeApp(appCtx)
        }

        val firebaseApp = FirebaseApp.getInstance()
        val persistenceKey = firebaseApp.persistenceKey
        val debugPrefs =
          appCtx.getSharedPreferences("$DEBUG_STORE_PREFIX$persistenceKey", Context.MODE_PRIVATE)
        val tokenPrefs =
          appCtx.getSharedPreferences("$TOKEN_STORE_PREFIX$persistenceKey", Context.MODE_PRIVATE)

        val resId =
          appCtx.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", appCtx.packageName)
            .takeIf { it != 0 }
            ?: appCtx.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", "com.example")
        val siteKey = if (resId != 0) appCtx.getString(resId).trim() else ""

        val intentDebugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)?.trim().orEmpty()
        val anchorFile = File(appCtx.noBackupFilesDir, DEVICE_ANCHOR_FILENAME)
        if (!anchorFile.exists()) {
          tokenPrefs.edit().clear().commit()
          if (intentDebugToken.isEmpty()) {
            debugPrefs.edit().clear().commit()
          }
          runCatching { anchorFile.writeText("v2:${System.currentTimeMillis()}") }
        }

        val appCheck = FirebaseAppCheck.getInstance()

        if (BuildConfig.DEBUG) {
          if (intentDebugToken.isNotEmpty()) {
            debugPrefs
              .edit()
              .putString(DEBUG_SECRET_KEY, intentDebugToken)
              .putString(INJECTED_TOKEN_KEY, intentDebugToken)
              .commit()
          }
          val activeInjectedToken = debugPrefs.getString(INJECTED_TOKEN_KEY, null)?.trim().orEmpty()
          if (activeInjectedToken.isNotEmpty()) {
            if (debugPrefs.getString(DEBUG_SECRET_KEY, null) != activeInjectedToken) {
              debugPrefs.edit().putString(DEBUG_SECRET_KEY, activeInjectedToken).commit()
            }
            if (installedMode != InstalledAppCheckMode.DEBUG_INJECTED || installedDebugToken != activeInjectedToken) {
              appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance(), true)
              installedMode = InstalledAppCheckMode.DEBUG_INJECTED
              installedDebugToken = activeInjectedToken
            }
          } else if (siteKey.isNotBlank()) {
            if (debugPrefs.contains(DEBUG_SECRET_KEY)) {
              debugPrefs.edit().remove(DEBUG_SECRET_KEY).commit()
            }
            if (installedMode != InstalledAppCheckMode.RECAPTCHA) {
              appCheck.installAppCheckProviderFactory(RecaptchaAppCheckProviderFactory.getInstance(siteKey), true)
              installedMode = InstalledAppCheckMode.RECAPTCHA
              installedDebugToken = null
            }
          } else if (installedMode != InstalledAppCheckMode.DEBUG_LOCAL) {
            appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance(), true)
            installedMode = InstalledAppCheckMode.DEBUG_LOCAL
            installedDebugToken = null
          }
        } else {
          if (debugPrefs.all.isNotEmpty()) {
            debugPrefs.edit().clear().commit()
          }
          if (siteKey.isNotBlank() && installedMode != InstalledAppCheckMode.RECAPTCHA) {
            appCheck.installAppCheckProviderFactory(RecaptchaAppCheckProviderFactory.getInstance(siteKey), true)
            installedMode = InstalledAppCheckMode.RECAPTCHA
            installedDebugToken = null
          }
        }
      } catch (e: Exception) {
        Log.w(TAG, "Firebase App Check setup skipped: ${e.message}")
      }
    }
  }
}

fun configureAppCheck(context: Context, intent: Intent?) {
  AppCheckHealthManager.configure(context, intent)
}
