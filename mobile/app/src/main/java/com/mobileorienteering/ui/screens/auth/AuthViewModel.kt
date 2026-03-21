package com.mobileorienteering.ui.screens.auth

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileorienteering.data.model.network.request.GoogleLoginRequest
import com.mobileorienteering.data.model.network.request.LoginRequest
import com.mobileorienteering.data.model.network.request.RegisterRequest
import com.mobileorienteering.data.preferences.MapStatePreferences
import com.mobileorienteering.data.repository.AuthRepository
import com.mobileorienteering.data.repository.MapRepository
import com.mobileorienteering.data.repository.RunRepository
import com.mobileorienteering.ui.core.Strings
import com.mobileorienteering.ui.core.snackbar.SnackbarManager
import com.mobileorienteering.util.manager.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repo: AuthRepository,
    private val runRepository: RunRepository,
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
            repo.login(LoginRequest(username = username.value, password = password.value))
                .onSuccess { syncManager.syncAll() }
                .onFailure { e ->
                    snackbarManager.showError(e.message ?: Strings.Error.loginFailed(context))
                }
            isLoading.value = false
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            isGoogleSignInLoading.value = true
            repo.loginWithGoogle(GoogleLoginRequest(idToken = idToken))
                .onSuccess { syncManager.syncAll() }
                .onFailure { e ->
                    snackbarManager.showError(e.message ?: Strings.Error.googleLoginFailed(context))
                }
            isGoogleSignInLoading.value = false
        }
    }

    fun register() {
        viewModelScope.launch {
            isLoading.value = true
            repo.register(
                RegisterRequest(
                    username = username.value,
                    email = email.value,
                    password = password.value,
                    fullName = fullName.value,
                    phoneNumber = phoneNumber.value
                )
            ).onSuccess {
                snackbarManager.showSuccess(Strings.Auth.accountCreated(context))
                syncManager.syncAll()
            }.onFailure { e ->
                snackbarManager.showError(e.message ?: Strings.Error.registrationFailed(context))
            }
            isLoading.value = false
        }
    }

    fun loginAsGuest() {
        viewModelScope.launch {
            isLoading.value = true
            repo.loginAsGuest()
                .onFailure { e ->
                    snackbarManager.showError(e.message ?: Strings.Error.guestLoginFailed(context))
                }
            isLoading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            mapStatePreferences.clearState()
            runRepository.clearLocalRuns()
            mapRepository.clearLocalMaps()
            repo.logout()
        }
    }

    fun showGoogleSignInError(message: String) {
        viewModelScope.launch {
            snackbarManager.showError(message)
        }
    }
}
