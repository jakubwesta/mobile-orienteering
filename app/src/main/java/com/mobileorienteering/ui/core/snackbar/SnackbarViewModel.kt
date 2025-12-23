package com.mobileorienteering.ui.core.snackbar

import androidx.lifecycle.ViewModel
import com.mobileorienteering.ui.core.snackbar.SnackbarManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SnackbarViewModel @Inject constructor(
    val snackbarManager: SnackbarManager
) : ViewModel()