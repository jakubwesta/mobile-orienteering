package com.mobileorienteering.ui.screens.first_launch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileorienteering.data.preferences.FirstLaunchPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FirstLaunchViewModel @Inject constructor(
    private val repo: FirstLaunchPreferences
) : ViewModel() {

    val isFirstLaunch: StateFlow<Boolean?> = repo.isFirstLaunchFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        null
    )

    fun markAsSeen() {
        viewModelScope.launch {
            repo.setFirstLaunchDone()
        }
    }
}
