package com.mobileorienteering.ui.screen.auth

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileorienteering.data.model.*
import com.mobileorienteering.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository
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

    var email = mutableStateOf("")
    var password = mutableStateOf("")
    var username = mutableStateOf("")
    var fullName = mutableStateOf<String?>(null)
    var phoneNumber = mutableStateOf<String?>(null)
    var error = mutableStateOf<String?>(null)

    fun login() {
        viewModelScope.launch {
            try {
                repo.login(
                    LoginModel(
                        email = email.value,
                        password = password.value
                    )
                )
                error.value = null
            } catch (e: Exception) {
                error.value = e.message
            }
        }
    }

    fun register() {
        viewModelScope.launch {
            try {
                repo.register(
                    RegisterModel(
                        username = username.value,
                        email = email.value,
                        password = password.value,
                        fullName = fullName.value,
                        phoneNumber = phoneNumber.value
                    )
                )
                error.value = null
            } catch (e: Exception) {
                error.value = e.message
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
        }
    }
}
