package com.mobileorienteering.ui.screen.auth

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileorienteering.data.model.network.response.UserResponse
import com.mobileorienteering.data.repository.AuthRepository
import com.mobileorienteering.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUser = MutableStateFlow<UserResponse?>(null)

    var isLoading = mutableStateOf(false)
    var error = mutableStateOf<String?>(null)

    init {
        loadCurrentUser()
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null

            try {
                val auth = authRepository.getCurrentAuth()
                if (auth?.isGuestMode == true) {
                    currentUser.value = null
                    isLoading.value = false
                    return@launch
                }

                val result = userRepository.getCurrentUser()
                result.onSuccess { user ->
                    currentUser.value = user
                }.onFailure { e ->
                    error.value = e.message ?: "Failed to load user"
                }
            } catch (e: Exception) {
                error.value = e.message ?: "Unknown error"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun updateProfile(
        username: String? = null,
        fullName: String? = null,
        email: String? = null,
        phoneNumber: String? = null,
        isPrivate: Boolean? = null
    ) {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null

            try {
                val auth = authRepository.getCurrentAuth()
                if (auth?.isGuestMode == true) {
                    error.value = "Profile editing is not available in guest mode"
                    isLoading.value = false
                    return@launch
                }

                val userId = auth?.userId
                    ?: throw Exception("User not logged in")

                val result = userRepository.updateProfile(
                    userId = userId,
                    username = username,
                    fullName = fullName,
                    email = email,
                    phoneNumber = phoneNumber,
                    isPrivate = isPrivate
                )

                result.onSuccess { updatedUser ->
                    currentUser.value = updatedUser
                    if (username != null) {
                        authRepository.updateUsername(username)
                    }
                }.onFailure { e ->
                    error.value = e.message ?: "Failed to update profile"
                }
            } catch (e: Exception) {
                error.value = e.message ?: "Unknown error"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String
    ) {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null

            try {
                val auth = authRepository.getCurrentAuth()
                if (auth?.isGuestMode == true) {
                    error.value = "Password change is not available in guest mode"
                    isLoading.value = false
                    return@launch
                }

                val userId = auth?.userId
                    ?: throw Exception("User not logged in")

                val result = userRepository.changePassword(
                    userId = userId,
                    currentPassword = currentPassword,
                    newPassword = newPassword
                )

                result.onFailure { e ->
                    error.value = e.message ?: "Failed to change password"
                }
            } catch (e: Exception) {
                error.value = e.message ?: "Unknown error"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun clearError() {
        error.value = null
    }
}
