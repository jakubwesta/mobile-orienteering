package com.mobileorienteering.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mobileorienteering.data.api.ApiHelper
import com.mobileorienteering.data.api.service.AuthApiService
import com.mobileorienteering.data.api.service.UserApiService
import com.mobileorienteering.data.model.app.AuthModel
import com.mobileorienteering.data.model.network.request.GoogleLoginRequest
import com.mobileorienteering.data.model.network.request.LoginRequest
import com.mobileorienteering.data.model.network.request.RefreshTokenRequest
import com.mobileorienteering.data.model.network.request.RegisterRequest
import com.mobileorienteering.data.model.network.response.TokenResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
        private val IS_EXTERNAL_LOGIN = booleanPreferencesKey("is_external_login")
        private val IS_GUEST_MODE = booleanPreferencesKey("is_guest_mode")
    }

    @Volatile private var cachedAuth: AuthModel? = null

    private val refreshMutex = Mutex()

    val isLoggedInFlow: Flow<Boolean> = context.authDataStore.data.map { prefs ->
        !prefs[TOKEN].isNullOrEmpty() && prefs[USER_ID] != null
    }

    val authModelFlow: Flow<AuthModel?> = context.authDataStore.data.map { prefs ->
        val userId = prefs[USER_ID] ?: return@map null
        val token = prefs[TOKEN] ?: return@map null
        val username = prefs[USERNAME] ?: return@map null
        val refreshToken = prefs[REFRESH_TOKEN] ?: return@map null
        AuthModel(
            userId = userId,
            username = username,
            token = token,
            refreshToken = refreshToken,
            isExternalLogin = prefs[IS_EXTERNAL_LOGIN] ?: false,
            isGuestMode = prefs[IS_GUEST_MODE] ?: false
        )
    }

    suspend fun login(request: LoginRequest): Result<AuthModel> {
        return ApiHelper.safeApiCall("Login failed") {
            authApi.login(request)
        }.mapCatching { fetchUserAndSaveAuth(it, isExternalLogin = false) }
    }

    suspend fun loginWithGoogle(request: GoogleLoginRequest): Result<AuthModel> {
        return ApiHelper.safeApiCall("Google login failed") {
            authApi.loginWithGoogle(request)
        }.mapCatching { fetchUserAndSaveAuth(it, isExternalLogin = true) }
    }

    suspend fun register(request: RegisterRequest): Result<AuthModel> {
        return ApiHelper.safeApiCall("Registration failed") {
            authApi.register(request)
        }.mapCatching { fetchUserAndSaveAuth(it, isExternalLogin = false) }
    }

    suspend fun loginAsGuest(): Result<AuthModel> {
        return try {
            val guestAuth = AuthModel(
                userId = -1L,
                username = "Guest",
                token = "guest_token",
                refreshToken = "guest_refresh_token",
                isExternalLogin = false,
                isGuestMode = true
            )
            cachedAuth = guestAuth
            saveAuth(guestAuth)
            Result.success(guestAuth)
        } catch (_: Exception) {
            Result.failure(Exception("Guest login failed"))
        }
    }

    private suspend fun fetchUserAndSaveAuth(
        tokenResponse: TokenResponse,
        isExternalLogin: Boolean
    ): AuthModel {
        cachedAuth = AuthModel(
            userId = 0,
            username = "",
            token = tokenResponse.accessToken,
            refreshToken = tokenResponse.refreshToken,
            isExternalLogin = isExternalLogin,
            isGuestMode = false
        )

        try {
            val userResponse = ApiHelper.safeApiCall("Failed to fetch user details") {
                userApi.getCurrentUser()
            }.getOrThrow()

            val auth = AuthModel(
                userId = userResponse.id,
                username = userResponse.username,
                token = tokenResponse.accessToken,
                refreshToken = tokenResponse.refreshToken,
                isExternalLogin = isExternalLogin
            )

            cachedAuth = auth
            saveAuth(auth)
            return auth
        } catch (e: Exception) {
            cachedAuth = null
            throw e
        }
    }

    suspend fun refreshToken(): Result<String> {
        return refreshMutex.withLock {
            val currentAuth = getCurrentAuth()
                ?: return@withLock Result.failure(Exception("No refresh token available"))

            if (currentAuth.isGuestMode) {
                return@withLock Result.failure(Exception("Guest mode cannot refresh token"))
            }

            ApiHelper.safeApiCall("Token refresh failed") {
                authApi.refreshToken(RefreshTokenRequest(refreshToken = currentAuth.refreshToken))
            }.mapCatching { tokenResponse ->
                val updated = currentAuth.copy(
                    token = tokenResponse.accessToken,
                    refreshToken = tokenResponse.refreshToken
                )
                cachedAuth = updated
                context.authDataStore.edit { prefs ->
                    prefs[TOKEN] = tokenResponse.accessToken
                    prefs[REFRESH_TOKEN] = tokenResponse.refreshToken
                }
                tokenResponse.accessToken
            }
        }
    }

    suspend fun logout() {
        cachedAuth = null
        context.authDataStore.edit { prefs ->
            prefs.remove(USER_ID)
            prefs.remove(TOKEN)
            prefs.remove(USERNAME)
            prefs.remove(REFRESH_TOKEN)
            prefs.remove(IS_EXTERNAL_LOGIN)
            prefs.remove(IS_GUEST_MODE)
        }
    }

    suspend fun getCurrentAuth(): AuthModel? {
        cachedAuth?.let { return it }

        val prefs = context.authDataStore.data.first()
        val userId = prefs[USER_ID] ?: return null
        val token = prefs[TOKEN] ?: return null
        val username = prefs[USERNAME] ?: return null
        val refreshToken = prefs[REFRESH_TOKEN] ?: return null

        return AuthModel(
            userId = userId,
            username = username,
            token = token,
            refreshToken = refreshToken,
            isExternalLogin = prefs[IS_EXTERNAL_LOGIN] ?: false,
            isGuestMode = prefs[IS_GUEST_MODE] ?: false
        ).also { cachedAuth = it }
    }

    private suspend fun saveAuth(auth: AuthModel) {
        context.authDataStore.edit { prefs ->
            prefs[USER_ID] = auth.userId
            prefs[TOKEN] = auth.token
            prefs[USERNAME] = auth.username
            prefs[REFRESH_TOKEN] = auth.refreshToken
            prefs[IS_EXTERNAL_LOGIN] = auth.isExternalLogin
            prefs[IS_GUEST_MODE] = auth.isGuestMode
        }
    }

    suspend fun updateUsername(newUsername: String) {
        cachedAuth = cachedAuth?.copy(username = newUsername)
        context.authDataStore.edit { prefs ->
            prefs[USERNAME] = newUsername
        }
    }
}
