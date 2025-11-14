package com.mobileorienteering.util

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleSignInHelper(private val context: Context) {

    private val signInClient: SignInClient = Identity.getSignInClient(context)

    suspend fun beginSignIn(webClientId: String): IntentSender? {
        return withContext(Dispatchers.IO) {
            try {
                val signInRequest = BeginSignInRequest.builder()
                    .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                            .setSupported(true)
                            .setServerClientId(webClientId)
                            .setFilterByAuthorizedAccounts(false)
                            .build()
                    )
                    .setAutoSelectEnabled(true)
                    .build()

                val result = Tasks.await(signInClient.beginSignIn(signInRequest))
                result.pendingIntent.intentSender
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun getSignInResultFromIntent(data: Intent?): String? {
        return withContext(Dispatchers.IO) {
            try {
                val credential = signInClient.getSignInCredentialFromIntent(data)
                credential.googleIdToken
            } catch (e: ApiException) {
                e.printStackTrace()
                null
            }
        }
    }
}
