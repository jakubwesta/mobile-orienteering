package com.mobileorienteering.ui.screen.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileorienteering.data.preferences.FirstLaunchPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FirstLaunchViewModel @Inject constructor(
    private val repo: FirstLaunchPreferences
) : ViewModel() {

    val isFirstLaunch = repo.isFirstLaunchFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )

    fun markAsSeen() {
        viewModelScope.launch {
            repo.setFirstLaunchDone()
        }
    }
}
