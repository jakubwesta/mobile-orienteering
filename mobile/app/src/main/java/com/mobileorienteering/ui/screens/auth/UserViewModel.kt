package com.mobileorienteering.ui.screens.auth

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileorienteering.data.model.domain.User
import com.mobileorienteering.data.repository.AuthRepository
import com.mobileorienteering.data.repository.UserRepository
import com.mobileorienteering.ui.core.Strings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUser = MutableStateFlow<User?>(null)

    var isLoading = mutableStateOf(false)
    var error = mutableStateOf<String?>(null)

    init {
        loadCurrentUser()
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            val auth = authRepository.getCurrentAuth()
            if (auth == null || auth.isGuestMode) {
                currentUser.value = null
                return@launch
            }

            isLoading.value = true
            error.value = null

            userRepository.getCurrentUser()
                .onSuccess { currentUser.value = it }
                .onFailure { error.value = it.message ?: Strings.Error.failedToLoadUser(context) }

            isLoading.value = false
        }
    }

    fun updateProfile(
        username: String? = null,
        fullName: String? = null,
        email: String? = null,
        phoneNumber: String? = null
    ) {
        viewModelScope.launch {
            if (authRepository.getCurrentAuth()?.isGuestMode == true) {
                error.value = Strings.Error.profileEditingNotAvailableGuest(context)
                return@launch
            }

            isLoading.value = true
            error.value = null

            userRepository.updateProfile(
                username = username,
                fullName = fullName,
                email = email,
                phoneNumber = phoneNumber
            ).onSuccess { updatedUser ->
                currentUser.value = updatedUser
                if (username != null) {
                    authRepository.updateUsername(username)
                }
            }.onFailure { error.value = it.message ?: Strings.Error.failedToUpdateProfile(context) }

            isLoading.value = false
        }
    }

    fun changePassword(
        oldPassword: String,
        newPassword: String
    ) {
        viewModelScope.launch {
            if (authRepository.getCurrentAuth()?.isGuestMode == true) {
                error.value = Strings.Error.passwordChangeNotAvailableGuest(context)
                return@launch
            }

            isLoading.value = true
            error.value = null

            userRepository.changePassword(
                oldPassword = oldPassword,
                newPassword = newPassword
            ).onFailure { error.value = it.message ?: Strings.Error.failedToChangePassword(context) }

            isLoading.value = false
        }
    }

    fun clearError() {
        error.value = null
    }
}
