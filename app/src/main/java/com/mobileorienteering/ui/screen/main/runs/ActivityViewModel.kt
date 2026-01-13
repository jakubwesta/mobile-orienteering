package com.mobileorienteering.ui.screen.main.runs

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileorienteering.data.model.domain.Activity
import com.mobileorienteering.data.preferences.SettingsPreferences
import com.mobileorienteering.data.repository.ActivityRepository
import com.mobileorienteering.data.repository.MapRepository
import com.mobileorienteering.data.model.domain.OrienteeringMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val mapRepository: MapRepository,
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {

    val activities: StateFlow<List<Activity>> = activityRepository.getAllActivitiesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val checkpointRadius: StateFlow<Int> = settingsPreferences.settingsFlow
        .map { it.gpsAccuracy }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    var isLoading = mutableStateOf(false)
    var error = mutableStateOf<String?>(null)

    var searchQuery = mutableStateOf("")
        private set

    var sortOrder = mutableStateOf(SortOrder.DATE_DESC)
        private set

    private val _filteredActivities = MutableStateFlow<List<Activity>>(emptyList())
    val filteredActivities: StateFlow<List<Activity>> = _filteredActivities.asStateFlow()

    init {
        viewModelScope.launch {
            activities.collect { allActivities ->
                updateFilteredActivities(allActivities)
            }
        }
    }

    fun getActivity(activityId: Long): Flow<Activity?> {
        return activityRepository.getActivityByIdFlow(activityId)
    }

    fun deleteActivity(activityId: Long) {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null

            try {
                activityRepository.deleteActivity(activityId)
            } catch (e: Exception) {
                error.value = e.message ?: "Unknown error"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
        updateFilteredActivities(activities.value)
    }

    fun setSortOrder(order: SortOrder) {
        sortOrder.value = order
        updateFilteredActivities(activities.value)
    }

    fun clearError() {
        error.value = null
    }

    private fun updateFilteredActivities(allActivities: List<Activity>) {
        var filtered = allActivities

        val query = searchQuery.value
        if (query.isNotBlank()) {
            filtered = filtered.filter { activity ->
                activity.title.contains(query, ignoreCase = true)
            }
        }

        filtered = when (sortOrder.value) {
            SortOrder.DATE_DESC -> filtered.sortedByDescending { it.startTime }
            SortOrder.DATE_ASC -> filtered.sortedBy { it.startTime }
            SortOrder.DISTANCE_DESC -> filtered.sortedByDescending { it.distance }
            SortOrder.DISTANCE_ASC -> filtered.sortedBy { it.distance }
            SortOrder.TITLE_ASC -> filtered.sortedBy { it.title }
        }

        _filteredActivities.value = filtered
    }

    fun getAllMaps(): StateFlow<List<OrienteeringMap>> {
        return mapRepository.getAllMapsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getMapForActivity(activityId: Long): Flow<OrienteeringMap?> {
        return getActivity(activityId).flatMapLatest { activity ->
            if (activity != null) {
                mapRepository.getMapByIdFlow(activity.mapId)
            } else {
                flowOf(null)
            }
        }
    }
}

enum class SortOrder {
    DATE_DESC,
    DATE_ASC,
    DISTANCE_DESC,
    DISTANCE_ASC,
    TITLE_ASC
}