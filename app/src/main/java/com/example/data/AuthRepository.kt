package com.example.data

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.MutableContextWrapper
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import java.security.MessageDigest
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthWebException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "Auth"

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

sealed interface SignInResult {
  data class Success(val user: FirebaseUser) : SignInResult
  data object Cancelled : SignInResult
  data object NoSavedCredential : SignInResult
  data class FallbackToLegacyPicker(val reason: String) : SignInResult
  data class FallbackToWebOAuth(val reason: String, val appSha1: String? = null) : SignInResult
  data class Error(val message: String) : SignInResult
}

class AuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {

  val currentUser: FirebaseUser?
    get() = auth.currentUser

  private fun isSystemOrTabletCredManagerAbort(msg: String, elapsedMs: Long): Boolean {
    // On Android 14 tablets with broken/disabled OS Credential Manager services,
    // getCredential() throws GetCredentialCancellationException almost instantaneously (< 700ms)
    // or carries OEM/Play Services failure text instead of a genuine user back press.
    if (elapsedMs < 700L) return true
    return msg.contains("reauth", ignoreCase = true) ||
      msg.contains("blocked", ignoreCase = true) ||
      msg.contains("cannot find", ignoreCase = true) ||
      msg.contains("developer", ignoreCase = true) ||
      msg.contains("[16]", ignoreCase = true) ||
      msg.contains("16:", ignoreCase = true) ||
      msg.contains("failed", ignoreCase = true) ||
      msg.contains("sync account", ignoreCase = true) ||
      msg.contains("provider", ignoreCase = true) ||
      msg.contains("unsupported", ignoreCase = true) ||
      msg.contains("credential", ignoreCase = true) ||
      msg.contains("unable", ignoreCase = true) ||
      msg.contains("timeout", ignoreCase = true) ||
      msg.contains("no such", ignoreCase = true)
  }

  suspend fun signInWithGoogleInteractive(activity: Activity): SignInResult {
    val webClientId =
      runCatching { activity.getString(R.string.default_web_client_id) }.getOrDefault("")
    if (webClientId.isBlank()) {
      Log.w(TAG, "default_web_client_id missing; using Firebase Google OAuth browser sign-in.")
      return signInWithGoogleWebOAuth(activity)
    }

    val wrappedContext = MutableContextWrapper(activity)
    val credentialManager = CredentialManager.create(wrappedContext)
    var fallbackReason = ""

    // 1. Primary path: Jetpack Credential Manager GetSignInWithGoogleOption
    val primaryOutcome: SignInResult? =
      run {
        val startMs = SystemClock.elapsedRealtime()
        try {
          val option = GetSignInWithGoogleOption.Builder(webClientId).build()
          val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
          val result = credentialManager.getCredential(context = wrappedContext, request = request)
          handleCredentialResponse(result.credential)
        } catch (e: GetCredentialCancellationException) {
          val elapsed = SystemClock.elapsedRealtime() - startMs
          val msg = e.errorMessage?.toString() ?: e.message.orEmpty()
          Log.w(TAG, "GetSignInWithGoogleOption cancelled after ${elapsed}ms: $msg", e)
          if (isSystemOrTabletCredManagerAbort(msg, elapsed)) {
            fallbackReason = msg.ifBlank { "OS Credential Manager aborted (${elapsed}ms)" }
            null
          } else {
            SignInResult.Cancelled
          }
        } catch (e: GetCredentialProviderConfigurationException) {
          fallbackReason = "Credential provider unavailable on device (${e.errorMessage ?: e.type})"
          Log.w(TAG, fallbackReason, e)
          null
        } catch (e: GetCredentialUnsupportedException) {
          fallbackReason = "Credential Manager unsupported on device (${e.errorMessage ?: e.type})"
          Log.w(TAG, fallbackReason, e)
          null
        } catch (e: NoCredentialException) {
          fallbackReason = "No Google credential returned (${e.errorMessage ?: "NoCredential"})"
          Log.w(TAG, "$fallbackReason; trying secondary CredentialManager option.", e)
          null
        } catch (e: GetCredentialException) {
          fallbackReason = "${e.type}: ${e.errorMessage ?: e.message.orEmpty()}"
          Log.w(TAG, "GetSignInWithGoogleOption failed: $fallbackReason", e)
          null
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          fallbackReason = e.localizedMessage ?: "Credential Manager error"
          Log.w(TAG, "Unexpected error in primary Google Sign-In; trying fallback.", e)
          null
        }
      }

    if (primaryOutcome != null) return primaryOutcome

    // 2. Secondary Credential Manager path: unfiltered GetGoogleIdOption
    val secondaryOutcome: SignInResult? =
      run {
        val startMs = SystemClock.elapsedRealtime()
        try {
          val googleIdOption =
            GetGoogleIdOption.Builder()
              .setFilterByAuthorizedAccounts(false)
              .setServerClientId(webClientId)
              .setAutoSelectEnabled(false)
              .build()
          val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
          val result = credentialManager.getCredential(context = wrappedContext, request = request)
          handleCredentialResponse(result.credential)
        } catch (e: GetCredentialCancellationException) {
          val elapsed = SystemClock.elapsedRealtime() - startMs
          val msg = e.errorMessage?.toString() ?: e.message.orEmpty()
          Log.w(TAG, "GetGoogleIdOption cancelled after ${elapsed}ms: $msg", e)
          if (isSystemOrTabletCredManagerAbort(msg, elapsed)) {
            if (fallbackReason.isBlank()) fallbackReason = msg.ifBlank { "Credential Manager aborted" }
            null
          } else {
            SignInResult.Cancelled
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: GetCredentialException) {
          if (fallbackReason.isBlank()) fallbackReason = "${e.type}: ${e.errorMessage ?: e.message.orEmpty()}"
          Log.w(TAG, "Secondary CredentialManager unavailable: ${e.type} ${e.errorMessage}")
          null
        } catch (e: Exception) {
          if (fallbackReason.isBlank()) fallbackReason = e.localizedMessage ?: "Credential Manager unavailable"
          Log.w(TAG, "Secondary CredentialManager Google Sign-In unavailable: ${e.message}")
          null
        }
      }

    if (secondaryOutcome != null) return secondaryOutcome

    // 3. Hand off to native Google Play Services GoogleSignIn Account Picker (works on Android 14 tablets
    // even when OS Credential Manager or Chrome Custom Tabs is broken/disabled).
    return SignInResult.FallbackToLegacyPicker(
      fallbackReason.ifBlank { "Switching to Google Play Services Account Picker" }
    )
  }

  @Suppress("DEPRECATION")
  fun buildGoogleSignInIntent(activity: Activity): Intent? {
    val webClientId =
      runCatching { activity.getString(R.string.default_web_client_id) }.getOrDefault("")
    if (webClientId.isBlank()) return null
    val gmsAvailability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity)
    if (gmsAvailability != ConnectionResult.SUCCESS) {
      Log.w(TAG, "Google Play Services availability status=$gmsAvailability")
    }
    val gso =
      GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(webClientId)
        .requestEmail()
        .build()
    return GoogleSignIn.getClient(activity, gso).signInIntent
  }

  @Suppress("DEPRECATION")
  fun readAppSigningSha1(context: Context): String? {
    return runCatching {
      val pm = context.packageManager
      val pkgName = context.packageName
      val signatures =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          val info = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNING_CERTIFICATES)
          val signingInfo = info.signingInfo ?: return@runCatching null
          if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners
          else signingInfo.signingCertificateHistory
        } else {
          pm.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES).signatures
        }
      val cert = signatures?.firstOrNull() ?: return@runCatching null
      val digest = MessageDigest.getInstance("SHA-1").digest(cert.toByteArray())
      digest.joinToString(":") { "%02X".format(it) }
    }.getOrNull()
  }

  @Suppress("DEPRECATION")
  suspend fun handleGoogleSignInActivityResult(
    resultCode: Int,
    data: Intent?,
    context: Context? = null,
  ): SignInResult {
    if (data == null && resultCode == Activity.RESULT_CANCELED) {
      return SignInResult.Cancelled
    }
    val runtimeSha1 = context?.let(::readAppSigningSha1)
    return try {
      val account = GoogleSignIn.getSignedInAccountFromIntent(data).await()
      val idToken = account.idToken
      if (idToken.isNullOrBlank()) {
        SignInResult.FallbackToWebOAuth(
          reason = "Google Account Picker returned no ID token.",
          appSha1 = runtimeSha1,
        )
      } else {
        val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = auth.signInWithCredential(firebaseCred).await()
        val user = authResult.user
        if (user != null) SignInResult.Success(user)
        else SignInResult.Error("Firebase returned null user after Play Services sign-in.")
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: ApiException) {
      Log.w(TAG, "GoogleSignIn ApiException statusCode=${e.statusCode}: ${e.message}", e)
      when (e.statusCode) {
        GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> SignInResult.Cancelled
        CommonStatusCodes.CANCELED -> SignInResult.Cancelled
        CommonStatusCodes.DEVELOPER_ERROR ->
          SignInResult.FallbackToWebOAuth(
            reason = "Play Services OAuth SHA-1 mismatch [10]",
            appSha1 = runtimeSha1,
          )
        GoogleSignInStatusCodes.SIGN_IN_FAILED ->
          SignInResult.FallbackToWebOAuth(
            reason = "Google Play Services Sign-In failed [12500]",
            appSha1 = runtimeSha1,
          )
        CommonStatusCodes.NETWORK_ERROR ->
          SignInResult.Error("Network error during Google Sign-In [7]. Check connection and try again.")
        GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS ->
          SignInResult.Error("Google Sign-In already in progress. Please try again.")
        else ->
          SignInResult.Error("Play Services Sign-In error [${e.statusCode}]: ${e.localizedMessage.orEmpty()}")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Unexpected error handling Play Services sign-in result", e)
      SignInResult.Error(e.localizedMessage ?: "Google Account Picker failed.")
    }
  }

  suspend fun consumePendingWebAuthResult(): SignInResult? {
    val pending = auth.pendingAuthResult ?: return null
    return try {
      val result = pending.await()
      val user = result.user
      if (user != null) {
        Log.d(TAG, "Recovered pending Firebase Google Web OAuth user: ${user.uid}")
        SignInResult.Success(user)
      } else {
        null
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(TAG, "Cleared stale pendingAuthResult error: ${e.message}")
      null
    }
  }

  suspend fun signInWithGoogleWebOAuth(activity: Activity): SignInResult {
    consumePendingWebAuthResult()?.let { return it }

    val hasBrowser =
      runCatching {
        val probe = Intent(Intent.ACTION_VIEW, Uri.parse("https://accounts.google.com")).apply {
          addCategory(Intent.CATEGORY_BROWSABLE)
        }
        probe.resolveActivity(activity.packageManager) != null
      }.getOrDefault(true)
    if (!hasBrowser) {
      return SignInResult.Error(
        "No web browser is enabled on this tablet for Browser Sign-In. Enable Chrome or use Google Account Picker above."
      )
    }

    return try {
      val provider =
        OAuthProvider.newBuilder("google.com")
          .setScopes(listOf("email", "profile"))
          .addCustomParameter("prompt", "select_account")
          .build()
      val authResult = auth.startActivityForSignInWithProvider(activity, provider).await()
      val user = authResult.user
      if (user != null) {
        SignInResult.Success(user)
      } else {
        SignInResult.Error("Google Browser OAuth completed without returning a Firebase user.")
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: ActivityNotFoundException) {
      Log.e(TAG, "No browser/Custom Tab activity found for OAuth", e)
      SignInResult.Error("No browser found to launch Google Sign-In. Use Google Account Picker above.")
    } catch (e: FirebaseAuthWebException) {
      Log.w(TAG, "FirebaseAuthWebException (${e.errorCode}): ${e.localizedMessage}", e)
      when (e.errorCode) {
        "ERROR_WEB_CONTEXT_CANCELED" ->
          SignInResult.Error("Browser Sign-In closed before completing (${e.errorCode}). Try Google Account Picker above or make sure Chrome is updated.")
        "ERROR_WEB_CONTEXT_ALREADY_PRESENTED" ->
          SignInResult.Error("Browser Sign-In was already open. Wait a moment and try again.")
        else ->
          SignInResult.Error("Google Browser Sign-In failed (${e.errorCode}): ${e.localizedMessage.orEmpty()}")
      }
    } catch (e: FirebaseAuthException) {
      Log.e(TAG, "FirebaseAuthException (${e.errorCode}): ${e.localizedMessage}", e)
      SignInResult.Error("Firebase Google OAuth error (${e.errorCode}): ${e.localizedMessage.orEmpty()}")
    } catch (e: Exception) {
      val msg = e.localizedMessage ?: "Google Sign-In failed"
      if (msg.contains("canceled", ignoreCase = true) || msg.contains("cancelled", ignoreCase = true)) {
        Log.w(TAG, "Firebase Google OAuth cancelled: $msg", e)
        SignInResult.Error("Browser Sign-In interrupted: $msg. Try Google Account Picker above.")
      } else {
        Log.e(TAG, "Firebase Google OAuth failed", e)
        SignInResult.Error(msg)
      }
    }
  }

  suspend fun trySilentGoogleSignIn(activity: Activity): SignInResult {
    return try {
      val wrappedContext = MutableContextWrapper(activity)
      val credentialManager = CredentialManager.create(wrappedContext)
      val webClientId = activity.getString(R.string.default_web_client_id)
      val option =
        GetGoogleIdOption.Builder()
          .setFilterByAuthorizedAccounts(true)
          .setServerClientId(webClientId)
          .setAutoSelectEnabled(true)
          .build()
      val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
      val result = credentialManager.getCredential(context = wrappedContext, request = request)
      handleCredentialResponse(result.credential)
    } catch (e: NoCredentialException) {
      SignInResult.NoSavedCredential
    } catch (e: GetCredentialCancellationException) {
      Log.w(TAG, "Silent Google Sign-In cancelled: ${e.message}")
      SignInResult.Cancelled
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(TAG, "Silent Google Sign-In unavailable: ${e.message}")
      SignInResult.NoSavedCredential
    }
  }

  private suspend fun handleCredentialResponse(credential: androidx.credentials.Credential): SignInResult {
    if (credential is CustomCredential &&
      credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
      val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
      val firebaseCred = GoogleAuthProvider.getCredential(googleCred.idToken, null)
      val authResult = auth.signInWithCredential(firebaseCred).await()
      val user = authResult.user
      return if (user != null) {
        SignInResult.Success(user)
      } else {
        SignInResult.Error("Firebase returned null user after credential sign-in.")
      }
    }
    return SignInResult.Error("Unsupported credential type: ${credential.type}")
  }

  @Suppress("DEPRECATION")
  suspend fun signOut(context: Context) {
    withContext(NonCancellable) {
      runCatching {
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
      }
      runCatching {
        val webClientId = context.getString(R.string.default_web_client_id)
        if (webClientId.isNotBlank()) {
          val gso =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
              .requestIdToken(webClientId)
              .requestEmail()
              .build()
          GoogleSignIn.getClient(context, gso).signOut().await()
        }
      }
      auth.signOut()
    }
  }
}
