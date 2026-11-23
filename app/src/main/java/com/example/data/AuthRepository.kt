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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "AuthRepository"

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
    return try {
      val credentialManager = CredentialManager.create(activity)
      val webClientId = activity.getString(R.string.default_web_client_id)
      val option = GetSignInWithGoogleOption.Builder(webClientId).build()
      val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
      val result = credentialManager.getCredential(context = activity, request = request)
      handleCredentialResponse(result.credential)
    } catch (e: GetCredentialCancellationException) {
      Log.w(TAG, "Interactive Google Sign-In cancelled.", e)
      SignInResult.Cancelled
    } catch (e: NoCredentialException) {
      Log.w(TAG, "No Google credential available.", e)
      SignInResult.Error(
        "No Google account credential returned. Make sure a Google account is signed into this device and you are running the latest installed build."
      )
    } catch (e: GetCredentialException) {
      Log.e(TAG, "CredentialManager error during interactive Google Sign-In", e)
      val raw = e.errorMessage?.toString() ?: e.localizedMessage ?: "Google Sign-In failed"
      SignInResult.Error(raw)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Unexpected error during interactive Google Sign-In", e)
      SignInResult.Error(e.localizedMessage ?: "Authentication failed")
    }
  }

  suspend fun trySilentGoogleSignIn(context: Context): SignInResult {
    return try {
      val credentialManager = CredentialManager.create(context)
      val webClientId = context.getString(R.string.default_web_client_id)
      val option =
        GetGoogleIdOption.Builder()
          .setFilterByAuthorizedAccounts(true)
          .setServerClientId(webClientId)
          .setAutoSelectEnabled(true)
          .build()
      val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
      val result = credentialManager.getCredential(context = context, request = request)
      handleCredentialResponse(result.credential)
    } catch (e: NoCredentialException) {
      SignInResult.NoSavedCredential
    } catch (e: GetCredentialCancellationException) {
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
