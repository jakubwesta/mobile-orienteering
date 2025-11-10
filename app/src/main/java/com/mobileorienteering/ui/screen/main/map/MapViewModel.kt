package com.mobileorienteering.ui.screen.main.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileorienteering.util.LocationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import android.location.Location
import android.util.Log
import javax.inject.Inject
import org.maplibre.android.geometry.LatLng
import com.mobileorienteering.domain.model.Checkpoint
import java.util.UUID
import java.util.Date

data class Run(
    val id: String = UUID.randomUUID().toString(),
    val routePoints: List<LatLng>,
    val startTime: Date,
    val endTime: Date,
    val distance: Float = 0f  // w metrach
)

data class LocationState(
    val currentLocation: Location? = null,
    val isTracking: Boolean = false,
    val hasPermission: Boolean = false,
    val error: String? = null,
    val currentRoutePoints: List<LatLng> = emptyList(),  // Aktualna trasa
    val isShowingRoute: Boolean = true,  // Czy pokazujemy trasę na mapie
    val savedRuns: List<Run> = emptyList(),  // Zapisane biegi
    val currentRunStartTime: Date? = null,
    val checkpoints: List<Checkpoint> = emptyList(),  // Checkpointy
    val isBottomSheetExpanded: Boolean = false  // Stan Bottom Sheet
)

private const val TAG = "MapViewModel"

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationManager: LocationManager
) : ViewModel() {

    private val stateFlow = MutableStateFlow(LocationState())
    val state: StateFlow<LocationState> = stateFlow.asStateFlow()

    private var trackingJob: Job? = null

    init {
        // Sprawdź uprawnienia przy starcie
        updatePermissionState()
    }

    fun updatePermissionState() {
        val hasPermission = locationManager.hasLocationPermission()
        stateFlow.update { it.copy(hasPermission = hasPermission) }
        Log.d(TAG, "Permission state updated: $hasPermission")
    }

    fun startTracking() {
        // Anuluj poprzedni job jeśli istnieje
        trackingJob?.cancel()

        if (!locationManager.hasLocationPermission()) {
            stateFlow.update { it.copy(error = "Location permission not granted") }
            Log.e(TAG, "Cannot start tracking - no permission")
            return
        }

        Log.d(TAG, "Starting location tracking")

        // Wyczyść tylko aktualną trasę i ustaw czas startu
        stateFlow.update { it.copy(
            currentRoutePoints = emptyList(),
            currentRunStartTime = Date()
        ) }

        trackingJob = viewModelScope.launch {
            locationManager.getLocationUpdates(
                intervalMillis = 1000L,  // Co 1 sekundę (szybsze)
                minimalDistance = 2f     // Co 2 metry (precyzyjniejsze)
            )
                .onStart {
                    stateFlow.update { it.copy(isTracking = true, error = null) }
                    Log.d(TAG, "Location tracking started")
                }
                .catch { e ->
                    Log.e(TAG, "Error in location tracking", e)
                    stateFlow.update {
                        it.copy(
                            isTracking = false,
                            error = e.message ?: "Unknown error"
                        )
                    }
                }
                .collectLatest { location ->
                    Log.d(TAG, "New location received: lat=${location.latitude}, lon=${location.longitude}, accuracy=${location.accuracy}")

                    // ZAWSZE dodawaj punkt do trasy gdy tracking jest włączony
                    stateFlow.update { currentState ->
                        val newPoint = LatLng(location.latitude, location.longitude)
                        val updatedPoints = currentState.currentRoutePoints + newPoint

                        currentState.copy(
                            currentLocation = location,
                            currentRoutePoints = updatedPoints
                        )
                    }
                }
        }
    }

    fun stopTracking() {
        Log.d(TAG, "Stopping location tracking")
        trackingJob?.cancel()
        trackingJob = null
        stateFlow.update { it.copy(
            isTracking = false,
            currentRoutePoints = emptyList(),
            currentRunStartTime = null
        ) }
    }

    fun toggleRouteDrawing() {
        stateFlow.update { currentState ->
            val newShowingState = !currentState.isShowingRoute
            Log.d(TAG, "Route visibility toggled: $newShowingState")

            currentState.copy(isShowingRoute = newShowingState)
        }
    }

    fun saveCurrentRun() {
        stateFlow.update { currentState ->
            if (currentState.currentRoutePoints.size >= 2) {
                val distance = calculateDistance(currentState.currentRoutePoints)
                val newRun = Run(
                    routePoints = currentState.currentRoutePoints,
                    startTime = currentState.currentRunStartTime ?: Date(),
                    endTime = Date(),
                    distance = distance
                )

                Log.d(TAG, "Saving run: ${newRun.routePoints.size} points, ${distance}m")

                currentState.copy(
                    currentRoutePoints = emptyList(),
                    savedRuns = currentState.savedRuns + newRun,
                    currentRunStartTime = Date()
                )
            } else {
                Log.d(TAG, "Run too short, not saving")
                currentState
            }
        }
    }

    private fun calculateDistance(points: List<LatLng>): Float {
        if (points.size < 2) return 0f

        var distance = 0f
        for (i in 0 until points.size - 1) {
            val results = FloatArray(1)
            Location.distanceBetween(
                points[i].latitude, points[i].longitude,
                points[i + 1].latitude, points[i + 1].longitude,
                results
            )
            distance += results[0]
        }
        return distance
    }

    fun clearAllRuns() {
        Log.d(TAG, "Clearing all saved runs")
        stateFlow.update { it.copy(savedRuns = emptyList()) }
    }

    fun addCheckpointAtCurrentLocation() {
        stateFlow.update { currentState ->
            val location = currentState.currentLocation
            if (location != null) {
                val newCheckpoint = Checkpoint(
                    number = currentState.checkpoints.size + 1,
                    name = "CP ${currentState.checkpoints.size + 1}",
                    location = LatLng(location.latitude, location.longitude)
                )
                Log.d(TAG, "Adding checkpoint at current location: ${newCheckpoint.name}")
                currentState.copy(checkpoints = currentState.checkpoints + newCheckpoint)
            } else {
                Log.w(TAG, "Cannot add checkpoint - no current location")
                currentState.copy(error = "Brak lokalizacji")
            }
        }
    }

    fun addCheckpointAtLocation(latLng: LatLng) {
        stateFlow.update { currentState ->
            val newCheckpoint = Checkpoint(
                number = currentState.checkpoints.size + 1,
                name = "CP ${currentState.checkpoints.size + 1}",
                location = latLng
            )
            Log.d(TAG, "Adding checkpoint at map location: ${newCheckpoint.name}")
            currentState.copy(checkpoints = currentState.checkpoints + newCheckpoint)
        }
    }

    fun removeCheckpoint(checkpointId: String) {
        stateFlow.update { currentState ->
            val updatedCheckpoints = currentState.checkpoints
                .filter { it.id != checkpointId }
                .mapIndexed { index, checkpoint ->
                    checkpoint.copy(number = index + 1, name = "CP ${index + 1}")
                }
            Log.d(TAG, "Removed checkpoint, ${updatedCheckpoints.size} remaining")
            currentState.copy(checkpoints = updatedCheckpoints)
        }
    }

    fun clearAllCheckpoints() {
        Log.d(TAG, "Clearing all checkpoints")
        stateFlow.update { it.copy(checkpoints = emptyList()) }
    }

    fun toggleBottomSheet() {
        stateFlow.update { it.copy(isBottomSheetExpanded = !it.isBottomSheetExpanded) }
    }

    fun getCurrentLocation() {
        viewModelScope.launch {
            if (!locationManager.hasLocationPermission()) {
                stateFlow.update { it.copy(error = "Location permission not granted") }
                Log.e(TAG, "Cannot get current location - no permission")
                return@launch
            }

            Log.d(TAG, "Getting current location")
            val location = locationManager.getCurrentLocation()

            if (location != null) {
                Log.d(TAG, "Current location received: lat=${location.latitude}, lon=${location.longitude}")
                stateFlow.update { it.copy(currentLocation = location, error = null) }
            } else {
                Log.e(TAG, "Failed to get current location")
                stateFlow.update { it.copy(error = "Failed to get location") }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared, stopping tracking")
        stopTracking()
    }
}