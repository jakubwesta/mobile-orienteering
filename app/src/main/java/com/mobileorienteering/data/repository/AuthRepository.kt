package com.mobileorienteering.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mobileorienteering.data.api.AuthApiService
import com.mobileorienteering.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import retrofit2.Response
import java.io.IOException

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
        return try {
            val response = authApi.login(
                LoginRequest(
                    username = model.username,
                    password = model.password
                )
            )
            handleAuthResponse(response)
        } catch (e: IOException) {
            Result.failure(Exception("Network error. Please check your connection."))
        } catch (e: Exception) {
            Result.failure(Exception("Login failed: ${e.message ?: "Unknown error"}"))
        }
    }

    suspend fun register(model: RegisterModel): Result<AuthModel> {
        return try {
            val response = authApi.register(
                RegisterRequest(
                    username = model.username,
                    fullName = model.fullName,
                    email = model.email,
                    phoneNumber = model.phoneNumber,
                    password = model.password,
                    private = model.isPrivate
                )
            )
            handleAuthResponse(response)
        } catch (e: IOException) {
            Result.failure(Exception("Network error. Please check your connection."))
        } catch (e: Exception) {
            Result.failure(Exception("Registration failed: ${e.message ?: "Unknown error"}"))
        }
    }

    suspend fun refreshToken(): Result<String> {
        return try {
            val currentAuth = getCurrentAuth()
            if (currentAuth?.refreshToken == null) {
                return Result.failure(Exception("No refresh token available"))
            }

            val response = authApi.refreshToken(
                RefreshTokenRequest(refreshToken = currentAuth.refreshToken)
            )

            if (response.isSuccessful && response.body() != null) {
                val tokenResponse = response.body()!!
                context.authDataStore.edit { prefs ->
                    prefs[TOKEN] = tokenResponse.accessToken
                    prefs[REFRESH_TOKEN] = tokenResponse.refreshToken
                }
                Result.success(tokenResponse.accessToken)
            } else {
                val errorMessage = extractErrorMessage(response, "Token refresh failed")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error. Please check your connection."))
        } catch (e: Exception) {
            Result.failure(Exception("Token refresh failed: ${e.message}"))
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

    private suspend fun handleAuthResponse(response: Response<AuthResponse>): Result<AuthModel> {
        return if (response.isSuccessful && response.body() != null) {
            val authResponse = response.body()!!
            val auth = AuthModel(
                username = authResponse.username,
                token = authResponse.token,
                refreshToken = authResponse.refreshToken
            )
            saveAuth(auth)
            Result.success(auth)
        } else {
            val errorMessage = extractErrorMessage(response, "Authentication failed")
            Result.failure(Exception(errorMessage))
        }
    }

    private fun <T> extractErrorMessage(response: Response<T>, defaultMessage: String): String {
        return try {
            val errorBody = response.errorBody()?.string()
            when {
                errorBody != null && errorBody.isNotEmpty() -> {
                    if (errorBody.contains("message")) {
                        errorBody
                            .substringAfter("\"message\":")
                            .substringAfter("\"")
                            .substringBefore("\"")
                            .ifEmpty { defaultMessage }
                    } else {
                        errorBody.take(100)
                    }
                }
                response.code() == 401 -> "Invalid username or password"
                response.code() == 403 -> "Access forbidden"
                response.code() == 404 -> "Server endpoint not found"
                response.code() == 409 -> "Username or email already exists"
                response.code() == 422 -> "Invalid input. Please check your data"
                response.code() >= 500 -> "Server error. Please try again later"
                else -> "$defaultMessage (${response.code()})"
            }
        } catch (e: Exception) {
            "$defaultMessage (${response.code()})"
        }
    }
}
