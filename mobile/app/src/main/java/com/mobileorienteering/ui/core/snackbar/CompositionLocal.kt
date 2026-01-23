package com.mobileorienteering.ui.core.snackbar

import androidx.compose.runtime.compositionLocalOf

val LocalSnackbarController = compositionLocalOf<SnackbarController> {
    error("No SnackbarController provided")
}
