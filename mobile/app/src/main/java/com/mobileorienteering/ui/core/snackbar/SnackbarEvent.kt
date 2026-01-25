package com.mobileorienteering.ui.core.snackbar

import androidx.compose.material3.SnackbarDuration

sealed class SnackbarEvent {
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val duration: SnackbarDuration = SnackbarDuration.Short,
        val onAction: (() -> Unit)? = null
    ) : SnackbarEvent()

    data class ShowError(val message: String) : SnackbarEvent()
    data class ShowSuccess(val message: String) : SnackbarEvent()
}
