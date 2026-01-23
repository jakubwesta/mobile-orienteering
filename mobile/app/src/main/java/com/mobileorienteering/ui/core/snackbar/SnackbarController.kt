package com.mobileorienteering.ui.core.snackbar

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SnackbarController(
    val snackbarHostState: SnackbarHostState,
    private val coroutineScope: CoroutineScope
) {
    fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short,
        onAction: (() -> Unit)? = null
    ) {
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = duration,
                withDismissAction = actionLabel == null
            )

            if (result == SnackbarResult.ActionPerformed) {
                onAction?.invoke()
            }
        }
    }

    fun showErrorSnackbar(message: String) {
        showSnackbar(
            message = message,
            duration = SnackbarDuration.Long
        )
    }

    fun showSuccessSnackbar(message: String) {
        showSnackbar(
            message = message,
            duration = SnackbarDuration.Short
        )
    }
}

@Composable
fun rememberSnackbarController(
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
): SnackbarController {
    val coroutineScope = rememberCoroutineScope()
    return remember(snackbarHostState, coroutineScope) {
        SnackbarController(snackbarHostState, coroutineScope)
    }
}
