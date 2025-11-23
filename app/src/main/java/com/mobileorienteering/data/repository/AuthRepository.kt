package com.mobileorienteering.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mobileorienteering.data.api.AuthApiService
import com.mobileorienteering.data.api.UserApiService
import com.mobileorienteering.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.authDataStore by preferencesDataStore("auth")

class AuthRepository(
    private val context: Context,
    private val authApi: AuthApiService,
    private val userApi: UserApiService
) {

    companion object {
        private val USER_ID = longPreferencesKey("user_id")
        private val USERNAME = stringPreferencesKey("username")
        private val TOKEN = stringPreferencesKey("token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }

    val isLoggedInFlow: Flow<Boolean> = context.authDataStore.data.map { prefs ->
        val token = prefs[TOKEN]
        val userId = prefs[USER_ID]
        !token.isNullOrEmpty() && userId != null
    }

    val authModelFlow: Flow<AuthModel?> = context.authDataStore.data.map { prefs ->
        val userId = prefs[USER_ID]
        val token = prefs[TOKEN]
        val username = prefs[USERNAME]
        val refreshToken = prefs[REFRESH_TOKEN]
        if (token != null && username != null && refreshToken != null && userId != null) {
            AuthModel(
                userId = userId,
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
            fetchUserAndSaveAuth(authResponse)
        }
    }

    suspend fun loginWithGoogle(idToken: String): Result<AuthModel> {
        return ApiHelper.safeApiCall("Google login failed") {
            authApi.loginWithGoogle(GoogleLoginRequest(idToken = idToken))
        }.mapCatching { authResponse ->
            fetchUserAndSaveAuth(authResponse)
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
            fetchUserAndSaveAuth(authResponse)
        }
    }

    private suspend fun fetchUserAndSaveAuth(authResponse: AuthResponse): AuthModel {
        saveTokenOnly(authResponse.token, authResponse.refreshToken, authResponse.username)

        val userResult = ApiHelper.safeApiCall("Failed to fetch user details") {
            userApi.getCurrentUser("Bearer ${authResponse.token}")
        }

        return userResult.getOrThrow().let { userResponse ->
            val auth = AuthModel(
                userId = userResponse.id,
                username = authResponse.username,
                token = authResponse.token,
                refreshToken = authResponse.refreshToken
            )

            saveAuth(auth)
            auth
        }
    }

    private suspend fun saveTokenOnly(token: String, refreshToken: String, username: String) {
        context.authDataStore.edit { prefs ->
            prefs[TOKEN] = token
            prefs[REFRESH_TOKEN] = refreshToken
            prefs[USERNAME] = username
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
            prefs.remove(USER_ID)
            prefs.remove(TOKEN)
            prefs.remove(USERNAME)
            prefs.remove(REFRESH_TOKEN)
        }
    }

    suspend fun getCurrentAuth(): AuthModel? {
        val prefs = context.authDataStore.data.first()
        val userId = prefs[USER_ID]
        val token = prefs[TOKEN]
        val username = prefs[USERNAME]
        val refreshToken = prefs[REFRESH_TOKEN]
        return if (token != null && username != null && refreshToken != null && userId != null) {
            AuthModel(
                userId = userId,
                username = username,
                token = token,
                refreshToken = refreshToken
            )
        } else null
    }

    private suspend fun saveAuth(auth: AuthModel) {
        context.authDataStore.edit { prefs ->
            prefs[USER_ID] = auth.userId
            prefs[TOKEN] = auth.token
            prefs[USERNAME] = auth.username
            prefs[REFRESH_TOKEN] = auth.refreshToken
        }
    }
}
