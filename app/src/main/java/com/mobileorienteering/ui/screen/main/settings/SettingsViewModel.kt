package com.mobileorienteering.ui.screen.main.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileorienteering.data.model.ContrastLevel
import com.mobileorienteering.data.model.SettingsModel
import com.mobileorienteering.data.preferences.SettingsPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsPreferences
) : ViewModel() {

    val settings = repo.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingsModel()
    )

    fun toggleDarkMode(enabled: Boolean) = viewModelScope.launch {
        repo.updateDarkMode(enabled)
    }

    fun updateContrastLevel(level: ContrastLevel) = viewModelScope.launch {
        repo.updateContrastLevel(level)
    }

    fun updateControlPointSound(enabled: Boolean) = viewModelScope.launch {
        repo.updateControlPointSound(enabled)
    }

    fun updateControlPointVibration(enabled: Boolean) = viewModelScope.launch {
        repo.updateControlPointVibration(enabled)
    }

    fun updateGpsAccuracy(value: Int) = viewModelScope.launch {
        repo.updateGpsAccuracy(value)
    }

    fun updateAll(settings: SettingsModel) = viewModelScope.launch {
        repo.updateSettings(settings)
    }
}
