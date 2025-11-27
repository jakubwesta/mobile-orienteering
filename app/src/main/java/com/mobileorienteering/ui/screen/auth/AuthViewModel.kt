package com.mobileorienteering.ui.screen.auth

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileorienteering.data.model.LoginModel
import com.mobileorienteering.data.model.RegisterModel
import com.mobileorienteering.data.repository.ActivityRepository
import com.mobileorienteering.data.repository.AuthRepository
import com.mobileorienteering.data.repository.MapRepository
import com.mobileorienteering.data.repository.MapStateRepository
import com.mobileorienteering.data.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository,
    private val activityRepository: ActivityRepository,
    private val mapRepository: MapRepository,
    private val mapStateRepository: MapStateRepository,  // NOWE
    private val syncManager: SyncManager
) : ViewModel() {

    val isLoggedIn = repo.isLoggedInFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    val authModel = repo.authModelFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    var username = mutableStateOf("")
    var password = mutableStateOf("")
    var email = mutableStateOf("")
    var fullName = mutableStateOf<String?>(null)
    var phoneNumber = mutableStateOf<String?>(null)

    var error = mutableStateOf<String?>(null)
    var isLoading = mutableStateOf(false)
    var isGoogleSignInLoading = mutableStateOf(false)

    fun login() {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val result = repo.login(
                    LoginModel(
                        username = username.value,
                        password = password.value
                    )
                )
                result.onSuccess { authModel ->
                    error.value = null
                    syncDataForUser(authModel.userId)
                }.onFailure { e ->
                    error.value = e.message ?: "Login failed"
                }
            } catch (e: Exception) {
                error.value = e.message ?: "Unknown error"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            isGoogleSignInLoading.value = true
            try {
                val result = repo.loginWithGoogle(idToken)
                result.onSuccess { authModel ->
                    error.value = null
                    syncDataForUser(authModel.userId)
                }.onFailure { e ->
                    error.value = e.message ?: "Google login failed"
                }
            } catch (e: Exception) {
                error.value = e.message ?: "Unknown error"
            } finally {
                isGoogleSignInLoading.value = false
            }
        }
    }

    fun register() {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val result = repo.register(
                    RegisterModel(
                        username = username.value,
                        email = email.value,
                        password = password.value,
                        fullName = fullName.value,
                        phoneNumber = phoneNumber.value
                    )
                )
                result.onSuccess { authModel ->
                    error.value = null
                    syncDataForUser(authModel.userId)
                }.onFailure { e ->
                    error.value = e.message ?: "Registration failed"
                }
            } catch (e: Exception) {
                error.value = e.message ?: "Unknown error"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            mapStateRepository.clearState()
            mapRepository.clearLocalMaps()
            repo.logout()
        }
    }

    private suspend fun syncDataForUser(userId: Long) {
        try {
            syncManager.syncAllDataForUser(userId)
        } catch (_: Exception) {

        }
    }

    fun clearError() {
        error.value = null
    }
}