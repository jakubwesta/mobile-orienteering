package com.mobileorienteering.ui.screens.runs

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileorienteering.data.model.app.MapIconStyle
import com.mobileorienteering.data.model.app.MapStyle
import com.mobileorienteering.data.model.domain.Map
import com.mobileorienteering.data.model.domain.Run
import com.mobileorienteering.data.preferences.SettingsPreferences
import com.mobileorienteering.data.repository.RunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RunViewModel @Inject constructor(
    private val runRepository: RunRepository,
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {

    private val _mapStyle = MutableStateFlow(MapStyle.CLASSIC)
    val mapStyle: StateFlow<MapStyle> = _mapStyle.asStateFlow()

    private val _mapIconStyle = MutableStateFlow(MapIconStyle.MODERN)
    val mapIconStyle: StateFlow<MapIconStyle> = _mapIconStyle.asStateFlow()

    val runs: StateFlow<List<Run>> = runRepository.getAllRunsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var isLoading = mutableStateOf(false)
    var error = mutableStateOf<String?>(null)

    var searchQuery = mutableStateOf("")
        private set

    var sortOrder = mutableStateOf(SortOrder.DATE_DESC)
        private set

    private val _filteredRuns = MutableStateFlow<List<Run>>(emptyList())
    val filteredRuns: StateFlow<List<Run>> = _filteredRuns.asStateFlow()

    init {
        viewModelScope.launch {
            runs.collect { updateFilteredRuns(it) }
        }
        viewModelScope.launch {
            settingsPreferences.settingsFlow.collect { settings ->
                _mapStyle.value = settings.mapStyle
                _mapIconStyle.value = settings.mapIconStyle
            }
        }
    }

    fun getRun(runId: Long): Flow<Run?> {
        return runRepository.getRunByIdFlow(runId)
    }

    fun getMapForRun(runId: Long): Flow<Map?> {
        return runRepository.getRunByIdFlow(runId).map { it?.map }
    }

    fun deleteRun(runId: Long) {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null

            runRepository.deleteRun(runId)
                .onFailure { error.value = it.message ?: "Failed to delete run" }

            isLoading.value = false
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
        updateFilteredRuns(runs.value)
    }

    fun setSortOrder(order: SortOrder) {
        sortOrder.value = order
        updateFilteredRuns(runs.value)
    }

    fun clearError() {
        error.value = null
    }

    private fun updateFilteredRuns(allRuns: List<Run>) {
        var filtered = allRuns

        val query = searchQuery.value
        if (query.isNotBlank()) {
            filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }
        }

        filtered = when (sortOrder.value) {
            SortOrder.DATE_DESC -> filtered.sortedByDescending { it.startedAt }
            SortOrder.DATE_ASC -> filtered.sortedBy { it.startedAt }
            SortOrder.NAME_ASC -> filtered.sortedBy { it.name }
        }

        _filteredRuns.value = filtered
    }
}

enum class SortOrder {
    DATE_DESC,
    DATE_ASC,
    NAME_ASC
}
