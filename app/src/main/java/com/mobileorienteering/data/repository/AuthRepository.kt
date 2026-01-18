package com.mobileorienteering.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mobileorienteering.data.api.service.AuthApiService
import com.mobileorienteering.data.api.service.UserApiService
import com.mobileorienteering.data.api.ApiHelper
import com.mobileorienteering.data.model.domain.*
import com.mobileorienteering.data.model.network.request.*
import com.mobileorienteering.data.model.network.response.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore by preferencesDataStore("auth")

@Singleton
class AuthRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val authApi: AuthApiService,
    private val userApi: UserApiService
) {

    companion object {
        private val USER_ID = longPreferencesKey("user_id")
        private val USERNAME = stringPreferencesKey("username")
        private val TOKEN = stringPreferencesKey("token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val IS_GOOGLE_LOGIN = booleanPreferencesKey("is_google_login")
        private val IS_GUEST_MODE = booleanPreferencesKey("is_guest_mode")
    }

    @Volatile
    private var pendingToken: String? = null

    private val refreshMutex = Mutex()

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
        val isGoogleLogin = prefs[IS_GOOGLE_LOGIN] ?: false
        val isGuestMode = prefs[IS_GUEST_MODE] ?: false
        if (token != null && username != null && refreshToken != null && userId != null) {
            AuthModel(
                userId = userId,
                username = username,
                token = token,
                refreshToken = refreshToken,
                isGoogleLogin = isGoogleLogin,
                isGuestMode = isGuestMode
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
            fetchUserAndSaveAuth(authResponse, isGoogleLogin = false)
        }
    }

    suspend fun loginWithGoogle(idToken: String): Result<AuthModel> {
        return ApiHelper.safeApiCall("Google login failed") {
            authApi.loginWithGoogle(GoogleLoginRequest(idToken = idToken))
        }.mapCatching { authResponse ->
            fetchUserAndSaveAuth(authResponse, isGoogleLogin = true)
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
            fetchUserAndSaveAuth(authResponse, isGoogleLogin = false)
        }
    }

    suspend fun loginAsGuest(): Result<AuthModel> {
        return try {
            val guestAuth = AuthModel(
                userId = -1L,
                username = "Guest",
                token = "guest_token",
                refreshToken = "guest_refresh_token",
                isGoogleLogin = false,
                isGuestMode = true
            )
            saveAuth(guestAuth)
            Result.success(guestAuth)
        } catch (e: Exception) {
            Result.failure(Exception("Guest login failed: ${e.message}"))
        }
    }

    private suspend fun fetchUserAndSaveAuth(
        authResponse: AuthResponse,
        isGoogleLogin: Boolean
    ): AuthModel {
        pendingToken = authResponse.token

        try {
            saveTokenOnly(authResponse.token, authResponse.refreshToken, authResponse.username, isGoogleLogin)

            val userResult = ApiHelper.safeApiCall("Failed to fetch user details") {
                userApi.getCurrentUser()
            }

            return userResult.getOrThrow().let { userResponse ->
                val auth = AuthModel(
                    userId = userResponse.id,
                    username = authResponse.username,
                    token = authResponse.token,
                    refreshToken = authResponse.refreshToken,
                    isGoogleLogin = isGoogleLogin
                )

                saveAuth(auth)
                auth
            }
        } finally {
            pendingToken = null
        }
    }

    private suspend fun saveTokenOnly(
        token: String,
        refreshToken: String,
        username: String,
        isGoogleLogin: Boolean
    ) {
        context.authDataStore.edit { prefs ->
            prefs[TOKEN] = token
            prefs[REFRESH_TOKEN] = refreshToken
            prefs[USERNAME] = username
            prefs[IS_GOOGLE_LOGIN] = isGoogleLogin
            prefs[IS_GUEST_MODE] = false
        }
    }

    suspend fun refreshToken(): Result<String> {
        return refreshMutex.withLock {
            val currentAuth = getCurrentAuth()
            if (currentAuth?.refreshToken == null) {
                return@withLock Result.failure(Exception("No refresh token available"))
            }

            ApiHelper.safeApiCall("Token refresh failed") {
                authApi.refreshToken(RefreshTokenRequest(refreshToken = currentAuth.refreshToken))
            }.mapCatching { tokenResponse ->
                context.authDataStore.edit { prefs ->
                    prefs[TOKEN] = tokenResponse.accessToken
                    prefs[REFRESH_TOKEN] = tokenResponse.refreshToken
                }
                tokenResponse.accessToken
            }
        }
    }

    suspend fun logout() {
        context.authDataStore.edit { prefs ->
            prefs.remove(USER_ID)
            prefs.remove(TOKEN)
            prefs.remove(USERNAME)
            prefs.remove(REFRESH_TOKEN)
            prefs.remove(IS_GOOGLE_LOGIN)
            prefs.remove(IS_GUEST_MODE)
        }
    }

    suspend fun getCurrentAuth(): AuthModel? {
        pendingToken?.let { token ->
            val prefs = context.authDataStore.data.first()
            val username = prefs[USERNAME]
            val refreshToken = prefs[REFRESH_TOKEN]
            val isGoogleLogin = prefs[IS_GOOGLE_LOGIN] ?: false
            val isGuestMode = prefs[IS_GUEST_MODE] ?: false
            if (username != null && refreshToken != null) {
                return AuthModel(
                    userId = 0,  // Temporary
                    username = username,
                    token = token,
                    refreshToken = refreshToken,
                    isGoogleLogin = isGoogleLogin,
                    isGuestMode = isGuestMode
                )
            }
        }

        val prefs = context.authDataStore.data.first()
        val userId = prefs[USER_ID]
        val token = prefs[TOKEN]
        val username = prefs[USERNAME]
        val refreshToken = prefs[REFRESH_TOKEN]
        val isGoogleLogin = prefs[IS_GOOGLE_LOGIN] ?: false
        val isGuestMode = prefs[IS_GUEST_MODE] ?: false
        return if (token != null && username != null && refreshToken != null && userId != null) {
            AuthModel(
                userId = userId,
                username = username,
                token = token,
                refreshToken = refreshToken,
                isGoogleLogin = isGoogleLogin,
                isGuestMode = isGuestMode
            )
        } else null
    }

    private suspend fun saveAuth(auth: AuthModel) {
        context.authDataStore.edit { prefs ->
            prefs[USER_ID] = auth.userId
            prefs[TOKEN] = auth.token
            prefs[USERNAME] = auth.username
            prefs[REFRESH_TOKEN] = auth.refreshToken
            prefs[IS_GOOGLE_LOGIN] = auth.isGoogleLogin
            prefs[IS_GUEST_MODE] = auth.isGuestMode
        }
    }

    suspend fun updateUsername(newUsername: String) {
        context.authDataStore.edit { prefs ->
            prefs[USERNAME] = newUsername
        }
    }
}
