package com.mobileorienteering.ui.screens.settings

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileorienteering.util.manager.ConnectivityManager
import com.mobileorienteering.util.manager.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncManager: SyncManager,
    private val connectivityManager: ConnectivityManager
) : ViewModel() {

    var isLoading = mutableStateOf(false)
    var error = mutableStateOf<String?>(null)
    var successMessage = mutableStateOf<String?>(null)

    fun startConnectivityMonitoring() {
        viewModelScope.launch {
            connectivityManager.isOnline
                .filter { it }
                .collect {
                    syncManager.syncAll()
                }
        }
    }

    fun syncAllData() {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            successMessage.value = null

            syncManager.syncAll()
                .onSuccess { successMessage.value = "Data synced successfully" }
                .onFailure { e -> error.value = e.message ?: "Sync failed" }

            isLoading.value = false
        }
    }
}
