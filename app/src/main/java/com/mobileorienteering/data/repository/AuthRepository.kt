package com.mobileorienteering.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mobileorienteering.data.api.AuthApiService
import com.mobileorienteering.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.authDataStore by preferencesDataStore("auth")

class AuthRepository(
    private val context: Context,
    private val authApi: AuthApiService
) {

    companion object {
        private val USERNAME = stringPreferencesKey("username")
        private val TOKEN = stringPreferencesKey("token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }

    val isLoggedInFlow: Flow<Boolean> = context.authDataStore.data.map { prefs ->
        !prefs[TOKEN].isNullOrEmpty()
    }

    val authModelFlow: Flow<AuthModel?> = context.authDataStore.data.map { prefs ->
        val token = prefs[TOKEN]
        val username = prefs[USERNAME]
        val refreshToken = prefs[REFRESH_TOKEN]
        if (token != null && username != null && refreshToken != null) {
            AuthModel(
                username = username,
                token = token,
                refreshToken = refreshToken
            )
        } else null
    }

    suspend fun login(model: LoginModel): Result<AuthModel> {
        return ApiHelper.safeApiCall("Login failed") {
            authApi.login(
                LoginRequest(
                    username = model.username,
                    password = model.password
                )
            )
        }.mapCatching { authResponse ->
            val auth = AuthModel(
                username = authResponse.username,
                token = authResponse.token,
                refreshToken = authResponse.refreshToken
            )
            saveAuth(auth)
            auth
        }
    }

    suspend fun loginWithGoogle(idToken: String): Result<AuthModel> {
        return ApiHelper.safeApiCall("Google login failed") {
            authApi.loginWithGoogle(GoogleLoginRequest(idToken = idToken))
        }.mapCatching { authResponse ->
            val auth = AuthModel(
                username = authResponse.username,
                token = authResponse.token,
                refreshToken = authResponse.refreshToken
            )
            saveAuth(auth)
            auth
        }
    }

    suspend fun register(model: RegisterModel): Result<AuthModel> {
        return ApiHelper.safeApiCall("Registration failed") {
            authApi.register(
                RegisterRequest(
                    username = model.username,
                    fullName = model.fullName,
                    email = model.email,
                    phoneNumber = model.phoneNumber,
                    password = model.password,
                    private = model.isPrivate
                )
            )
        }.mapCatching { authResponse ->
            val auth = AuthModel(
                username = authResponse.username,
                token = authResponse.token,
                refreshToken = authResponse.refreshToken
            )
            saveAuth(auth)
            auth
        }
    }

    suspend fun refreshToken(): Result<String> {
        val currentAuth = getCurrentAuth()
        if (currentAuth?.refreshToken == null) {
            return Result.failure(Exception("No refresh token available"))
        }

        return ApiHelper.safeApiCall("Token refresh failed") {
            authApi.refreshToken(RefreshTokenRequest(refreshToken = currentAuth.refreshToken))
        }.mapCatching { tokenResponse ->
            context.authDataStore.edit { prefs ->
                prefs[TOKEN] = tokenResponse.accessToken
                prefs[REFRESH_TOKEN] = tokenResponse.refreshToken
            }
            tokenResponse.accessToken
        }
    }

    suspend fun logout() {
        context.authDataStore.edit { prefs ->
            prefs.remove(TOKEN)
            prefs.remove(USERNAME)
            prefs.remove(REFRESH_TOKEN)
        }
    }

    suspend fun getCurrentAuth(): AuthModel? {
        val prefs = context.authDataStore.data.first()
        val token = prefs[TOKEN]
        val username = prefs[USERNAME]
        val refreshToken = prefs[REFRESH_TOKEN]
        return if (token != null && username != null && refreshToken != null) {
            AuthModel(
                username = username,
                token = token,
                refreshToken = refreshToken
            )
        } else null
    }

    private suspend fun saveAuth(auth: AuthModel) {
        context.authDataStore.edit { prefs ->
            prefs[TOKEN] = auth.token
            prefs[USERNAME] = auth.username
            prefs[REFRESH_TOKEN] = auth.refreshToken
        }
    }
}
