package com.example.data

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
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
  data class Error(val message: String) : SignInResult
}

class AuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {

  val currentUser: FirebaseUser?
    get() = auth.currentUser

  suspend fun signInWithGoogleInteractive(activity: Activity): SignInResult {
    val webClientId =
      runCatching { activity.getString(R.string.default_web_client_id) }.getOrDefault("")
    if (webClientId.isBlank()) {
      Log.w(TAG, "default_web_client_id missing; using Firebase Google OAuth browser sign-in.")
      return signInWithGoogleWebOAuth(activity)
    }

    val credentialManager = CredentialManager.create(activity)

    // 1. Primary path: Jetpack Credential Manager GetSignInWithGoogleOption (one option per request)
    val primaryOutcome: SignInResult? =
      try {
        val option = GetSignInWithGoogleOption.Builder(webClientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = credentialManager.getCredential(context = activity, request = request)
        handleCredentialResponse(result.credential)
      } catch (e: GetCredentialCancellationException) {
        val msg = e.errorMessage?.toString() ?: e.message.orEmpty()
        Log.w(TAG, "Google Sign-In flow cancelled or dismissed: $msg", e)
        val looksLikeSystemBlock =
          msg.contains("reauth", ignoreCase = true) ||
            msg.contains("blocked", ignoreCase = true) ||
            msg.contains("cannot find", ignoreCase = true) ||
            msg.contains("developer", ignoreCase = true) ||
            msg.contains("[16]", ignoreCase = true) ||
            msg.contains("failed", ignoreCase = true)
        if (looksLikeSystemBlock) null else SignInResult.Cancelled
      } catch (e: NoCredentialException) {
        Log.w(TAG, "GetSignInWithGoogleOption returned NoCredentialException; trying fallback.", e)
        null
      } catch (e: GetCredentialException) {
        Log.w(TAG, "GetSignInWithGoogleOption failed (${e.type}): ${e.errorMessage}", e)
        null
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(TAG, "Unexpected error in primary Google Sign-In; trying fallback.", e)
        null
      }

    if (primaryOutcome != null) return primaryOutcome

    // 2. Secondary Credential Manager path: single-option GetGoogleIdOption (unfiltered accounts)
    val secondaryOutcome: SignInResult? =
      try {
        val googleIdOption =
          GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
        val result = credentialManager.getCredential(context = activity, request = request)
        handleCredentialResponse(result.credential)
      } catch (e: GetCredentialCancellationException) {
        val msg = e.errorMessage?.toString() ?: e.message.orEmpty()
        Log.w(TAG, "Secondary Google Sign-In flow cancelled or dismissed: $msg", e)
        val looksLikeSystemBlock =
          msg.contains("reauth", ignoreCase = true) ||
            msg.contains("blocked", ignoreCase = true) ||
            msg.contains("cannot find", ignoreCase = true) ||
            msg.contains("[16]", ignoreCase = true) ||
            msg.contains("failed", ignoreCase = true)
        if (looksLikeSystemBlock) null else SignInResult.Cancelled
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(TAG, "Secondary CredentialManager Google Sign-In unavailable: ${e.message}")
        null
      }

    if (secondaryOutcome != null) return secondaryOutcome

    // 3. Universal Firebase Google OAuth fallback (Chrome Custom Tab — immune to Play Services Credential Manager issues)
    return signInWithGoogleWebOAuth(activity)
  }

  suspend fun signInWithGoogleWebOAuth(activity: Activity): SignInResult {
    return try {
      val pending = auth.pendingAuthResult
      val authResult =
        if (pending != null) {
          pending.await()
        } else {
          val provider =
            OAuthProvider.newBuilder("google.com")
              .setScopes(listOf("email", "profile"))
              .addCustomParameter("prompt", "select_account")
              .build()
          auth.startActivityForSignInWithProvider(activity, provider).await()
        }
      val user = authResult.user
      if (user != null) {
        SignInResult.Success(user)
      } else {
        SignInResult.Error("Google OAuth completed without returning a Firebase user.")
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      val msg = e.localizedMessage ?: "Google Sign-In failed"
      if (msg.contains("canceled", ignoreCase = true) || msg.contains("cancelled", ignoreCase = true)) {
        Log.w(TAG, "Firebase Google OAuth cancelled: $msg", e)
        SignInResult.Cancelled
      } else {
        Log.e(TAG, "Firebase Google OAuth failed", e)
        SignInResult.Error(msg)
      }
    }
  }

  suspend fun trySilentGoogleSignIn(activity: Activity): SignInResult {
    return try {
      val credentialManager = CredentialManager.create(activity)
      val webClientId = activity.getString(R.string.default_web_client_id)
      val option =
        GetGoogleIdOption.Builder()
          .setFilterByAuthorizedAccounts(true)
          .setServerClientId(webClientId)
          .setAutoSelectEnabled(true)
          .build()
      val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
      val result = credentialManager.getCredential(context = activity, request = request)
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

  suspend fun signOut(context: Context) {
    withContext(NonCancellable) {
      runCatching {
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
      }
      auth.signOut()
    }
  }
}
