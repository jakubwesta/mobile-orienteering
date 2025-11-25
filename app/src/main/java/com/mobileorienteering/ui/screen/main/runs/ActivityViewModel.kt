package com.mobileorienteering.ui.screen.main.runs

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileorienteering.data.model.Activity
import com.mobileorienteering.data.model.PathPoint
import com.mobileorienteering.data.repository.ActivityRepository
import com.mobileorienteering.data.repository.MapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val mapRepository: MapRepository
) : ViewModel() {

    val activities: StateFlow<List<Activity>> = activityRepository.getAllActivitiesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var isLoading = mutableStateOf(false)
    var error = mutableStateOf<String?>(null)
    var successMessage = mutableStateOf<String?>(null)

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

    fun createActivity(
        userId: Long,
        mapId: Long,
        title: String,
        startTime: Instant,
        duration: String,
        distance: Double,
        pathData: List<PathPoint>
    ) {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null

            try {
                val result = activityRepository.createActivity(
                    userId = userId,
                    mapId = mapId,
                    title = title,
                    startTime = startTime,
                    duration = duration,
                    distance = distance,
                    pathData = pathData
                )

                result.onSuccess {
                    successMessage.value = "Activity saved!"
                }.onFailure { e ->
                    error.value = e.message ?: "Failed to save activity"
                }
            } catch (e: Exception) {
                error.value = e.message ?: "Unknown error"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun deleteActivity(activityId: Long) {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null

            try {
                val result = activityRepository.deleteActivity(activityId)

                result.onSuccess {
                    successMessage.value = "Activity deleted"
                }.onFailure { e ->
                    error.value = e.message ?: "Failed to delete activity"
                }
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

    fun clearSuccessMessage() {
        successMessage.value = null
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

    fun getStatistics(): ActivityStatistics {
        val allActivities = activities.value
        return ActivityStatistics(
            totalActivities = allActivities.size,
            totalDistance = allActivities.sumOf { it.distance },
            averageDistance = if (allActivities.isNotEmpty()) {
                allActivities.sumOf { it.distance } / allActivities.size
            } else 0.0,
            longestDistance = allActivities.maxOfOrNull { it.distance } ?: 0.0
        )
    }

    fun getAllMaps(): StateFlow<List<com.mobileorienteering.data.model.Map>> {
        return mapRepository.getAllMapsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }
}

enum class SortOrder {
    DATE_DESC,
    DATE_ASC,
    DISTANCE_DESC,
    DISTANCE_ASC,
    TITLE_ASC
}

data class ActivityStatistics(
    val totalActivities: Int = 0,
    val totalDistance: Double = 0.0,
    val averageDistance: Double = 0.0,
    val longestDistance: Double = 0.0
)
