package com.mobileorienteering.ui.screens.auth

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileorienteering.data.model.domain.LoginModel
import com.mobileorienteering.data.model.domain.RegisterModel
import com.mobileorienteering.data.repository.ActivityRepository
import com.mobileorienteering.data.repository.AuthRepository
import com.mobileorienteering.data.repository.MapRepository
import com.mobileorienteering.data.preferences.MapStatePreferences
import com.mobileorienteering.ui.core.snackbar.SnackbarManager
import com.mobileorienteering.util.manager.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository,
    private val activityRepository: ActivityRepository,
    private val mapRepository: MapRepository,
    private val mapStatePreferences: MapStatePreferences,
    private val syncManager: SyncManager,
    private val snackbarManager: SnackbarManager
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean?> = repo.isLoggedInFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        null
    )

    val authModel = repo.authModelFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        null
    )

    var username = mutableStateOf("")
    var password = mutableStateOf("")
    var email = mutableStateOf("")
    var fullName = mutableStateOf<String?>(null)
    var phoneNumber = mutableStateOf<String?>(null)

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
                    syncDataForUser(authModel.userId)
                }.onFailure { e ->
                    val errorMessage = e.message ?: "Login failed"
                    snackbarManager.showError(errorMessage)
                }
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Unknown error"
                snackbarManager.showError(errorMessage)
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
                    syncDataForUser(authModel.userId)
                }.onFailure { e ->
                    val errorMessage = e.message ?: "Google login failed"
                    snackbarManager.showError(errorMessage)
                }
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Unknown error"
                snackbarManager.showError(errorMessage)
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
                    snackbarManager.showSuccess("Account created successfully!")
                    syncDataForUser(authModel.userId)
                }.onFailure { e ->
                    val errorMessage = e.message ?: "Registration failed"
                    snackbarManager.showError(errorMessage)
                }
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Unknown error"
                snackbarManager.showError(errorMessage)
            } finally {
                isLoading.value = false
            }
        }
    }

    fun loginAsGuest() {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val result = repo.loginAsGuest()
                result.onSuccess {
                    // No sync needed for guest mode
                }.onFailure { e ->
                    val errorMessage = e.message ?: "Guest login failed"
                    snackbarManager.showError(errorMessage)
                }
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Unknown error"
                snackbarManager.showError(errorMessage)
            } finally {
                isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            mapStatePreferences.clearState()
            activityRepository.clearLocalActivities()
            mapRepository.clearLocalMaps()
            repo.logout()
        }
    }

    private suspend fun syncDataForUser(userId: Long) {
        try {
            syncManager.syncAllDataForUser(userId)
        } catch (_: Exception) { }
    }

    fun showGoogleSignInError(message: String) {
        viewModelScope.launch {
            snackbarManager.showError(message)
        }
    }
}
