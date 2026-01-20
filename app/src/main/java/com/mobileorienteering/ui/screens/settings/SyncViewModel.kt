package com.mobileorienteering.ui.screens.settings

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileorienteering.data.repository.AuthRepository
import com.mobileorienteering.util.manager.SyncManager
import com.mobileorienteering.util.manager.ConnectivityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.filter
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncManager: SyncManager,
    private val connectivityManager: ConnectivityManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    var isLoading = mutableStateOf(false)
    var error = mutableStateOf<String?>(null)
    var successMessage = mutableStateOf<String?>(null)

    fun startConnectivityMonitoring() {
        viewModelScope.launch {
            connectivityManager.isOnline
                .filter { it }
                .collect {
                    syncIfLoggedIn()
                }
        }
    }

    fun syncAllData() {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            successMessage.value = null

            try {
                val userId = authRepository.getCurrentAuth()?.userId
                    ?: throw Exception("User not logged in")

                val result = syncManager.syncAllDataForUser(userId)

                result.onSuccess {
                    successMessage.value = "Data synced successfully"
                }.onFailure { e ->
                    error.value = e.message ?: "Sync failed"
                }
            } catch (e: Exception) {
                error.value = e.message ?: "Sync error"
            } finally {
                isLoading.value = false
            }
        }
    }

    private suspend fun syncIfLoggedIn() {
        val auth = authRepository.getCurrentAuth()
        if (auth != null && !auth.isGuestMode) {
            try {
                syncManager.syncAllDataForUser(auth.userId)
            } catch (_: Exception) {

            }
        }
    }
}
