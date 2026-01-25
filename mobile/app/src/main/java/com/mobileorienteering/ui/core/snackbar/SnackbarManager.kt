package com.mobileorienteering.ui.core.snackbar

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnackbarManager @Inject constructor() {
    private val _messages = MutableSharedFlow<SnackbarEvent>(replay = 0)
    val messages = _messages.asSharedFlow()

    suspend fun showError(message: String) {
        _messages.emit(SnackbarEvent.ShowError(message))
    }

    suspend fun showSuccess(message: String) {
        _messages.emit(SnackbarEvent.ShowSuccess(message))
    }
}
