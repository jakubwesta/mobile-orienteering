package com.mobileorienteering.ui.screen.main.map

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileorienteering.data.model.ActivityStatus
import com.mobileorienteering.data.model.ControlPoint
import com.mobileorienteering.data.model.VisitedControlPoint
import com.mobileorienteering.data.repository.ActivityRepository
import com.mobileorienteering.data.repository.AuthRepository
import com.mobileorienteering.data.repository.MapRepository
import com.mobileorienteering.data.repository.MapStateRepository
import com.mobileorienteering.ui.screen.main.map.models.Checkpoint
import com.mobileorienteering.ui.screen.main.map.models.MapState
import com.mobileorienteering.util.LocationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import org.maplibre.spatialk.geojson.Position
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationManager: LocationManager,
    private val mapRepository: MapRepository,
    private val authRepository: AuthRepository,
    private val mapStateRepository: MapStateRepository,
    private val activityRepository: ActivityRepository  // NOWE
) : ViewModel() {

    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    private var lastLocation: Location? = null
    private var trackingJob: Job? = null
    private var isInitialized = false

    private val _shouldMoveCamera = MutableStateFlow(false)
    val shouldMoveCamera: StateFlow<Boolean> = _shouldMoveCamera.asStateFlow()

    // NOWE - Stan ukończonego biegu
    data class FinishedRunState(
        val isCompleted: Boolean,
        val duration: String,
        val visitedControlPoints: List<VisitedControlPoint>,
        val totalCheckpoints: Int,
        val distance: Double,
        val mapId: Long,
        val mapName: String
    )

    private val _finishedRunState = MutableStateFlow<FinishedRunState?>(null)
    val finishedRunState: StateFlow<FinishedRunState?> = _finishedRunState.asStateFlow()

    init {
        restoreSavedState()
    }

    private fun restoreSavedState() {
        viewModelScope.launch {
            val savedState = mapStateRepository.getSavedState()

            _state.update {
                it.copy(
                    checkpoints = savedState.checkpoints,
                    currentMapId = savedState.currentMapId,
                    currentMapName = savedState.currentMapName,
                    distanceTraveled = savedState.distanceTraveled,
                    hasPermission = locationManager.hasLocationPermission()
                )
            }

            if (savedState.isTracking && locationManager.hasLocationPermission()) {
                android.util.Log.d("MapViewModel", "Restoring tracking state...")
                resumeTracking()
            }

            if (savedState.checkpoints.isNotEmpty()) {
                _shouldMoveCamera.value = true
            }

            isInitialized = true
        }
    }

    fun cameraMoved() {
        _shouldMoveCamera.value = false
    }

    fun updatePermissionState() {
        _state.update {
            it.copy(hasPermission = locationManager.hasLocationPermission())
        }
    }

    // ==================== BIEG ====================

    fun startRun() {
        if (_state.value.checkpoints.isEmpty()) {
            _state.update { it.copy(error = "No control points to run") }
            return
        }

        _state.update {
            it.copy(
                isRunActive = true,
                runStartTime = Instant.now(),
                visitedCheckpointIndices = emptySet(),
                runDistance = 0.0,
                nextCheckpointIndex = 0,  // Zaczynamy od pierwszego
                error = null
            )
        }

        // Uruchom śledzenie lokalizacji
        startTracking()
    }

    fun stopRun() {
        val state = _state.value
        if (!state.isRunActive) return

        val duration = state.runStartTime?.let {
            Duration.between(it, Instant.now())
        } ?: Duration.ZERO

        val visitedControlPoints = state.visitedCheckpointIndices.map { index ->
            val checkpoint = state.checkpoints[index]
            VisitedControlPoint(
                controlPointName = checkpoint.name,
                order = index + 1,
                visitedAt = Instant.now(),
                latitude = checkpoint.position.latitude,
                longitude = checkpoint.position.longitude
            )
        }

        val isCompleted = state.visitedCheckpointIndices.size == state.checkpoints.size

        _finishedRunState.value = FinishedRunState(
            isCompleted = isCompleted,
            duration = formatDurationString(duration.seconds),
            visitedControlPoints = visitedControlPoints,
            totalCheckpoints = state.checkpoints.size,
            distance = state.runDistance,
            mapId = state.currentMapId ?: 0L,
            mapName = state.currentMapName ?: "Unknown"
        )

        _state.update {
            it.copy(
                isRunActive = false,
                isTracking = false
            )
        }

        trackingJob?.cancel()
        saveTrackingState(false)
    }

    fun saveFinishedRun() {
        val finishedRun = _finishedRunState.value ?: return

        viewModelScope.launch {
            val auth = authRepository.getCurrentAuth()
            if (auth == null) {
                _state.update { it.copy(error = "You must be logged in") }
                return@launch
            }

            activityRepository.createRunActivity(
                userId = auth.userId,
                mapId = finishedRun.mapId,
                title = "Run: ${finishedRun.mapName}",
                duration = finishedRun.duration,
                distance = finishedRun.distance,
                pathData = emptyList(),
                status = if (finishedRun.isCompleted) ActivityStatus.COMPLETED else ActivityStatus.ABANDONED,
                visitedControlPoints = finishedRun.visitedControlPoints,
                totalCheckpoints = finishedRun.totalCheckpoints
            )

            _finishedRunState.value = null
        }
    }

    fun discardFinishedRun() {
        _finishedRunState.value = null
    }

    /**
     * Sprawdza czy użytkownik jest w pobliżu NASTĘPNEGO checkpointu w kolejności.
     * Checkpointy muszą być zaliczane po kolei (1→2→3→...).
     */
    private fun checkCheckpointVisit(location: Location) {
        if (!_state.value.isRunActive) return

        val checkpoints = _state.value.checkpoints
        val nextIndex = _state.value.nextCheckpointIndex

        // Sprawdź czy wszystkie checkpointy już zaliczone
        if (nextIndex >= checkpoints.size) return

        val nextCheckpoint = checkpoints[nextIndex]
        val distance = calculateDistance(
            location.latitude, location.longitude,
            nextCheckpoint.position.latitude, nextCheckpoint.position.longitude
        )

        // Promień zaliczenia: 25 metrów
        if (distance <= 25.0) {
            val newVisited = _state.value.visitedCheckpointIndices + nextIndex
            val newNextIndex = nextIndex + 1

            android.util.Log.d("MapViewModel", "Checkpoint ${nextIndex + 1} visited! Next: ${newNextIndex + 1}")

            _state.update {
                it.copy(
                    visitedCheckpointIndices = newVisited,
                    nextCheckpointIndex = newNextIndex
                )
            }

            // Auto-zakończenie gdy wszystkie checkpointy odwiedzone
            if (newNextIndex >= checkpoints.size) {
                stopRun()
            }
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }

    private fun formatDurationString(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }

    // ==================== TRACKING ====================

    fun startTracking() {
        android.util.Log.d("MapViewModel", "startTracking() called")

        if (!locationManager.hasLocationPermission()) {
            android.util.Log.e("MapViewModel", "No location permission!")
            _state.update { it.copy(error = "Location permission required") }
            return
        }

        _state.update {
            it.copy(
                isTracking = true,
                error = null,
                trackingStartTime = System.currentTimeMillis(),
                locationHistory = emptyList(),
                distanceTraveled = 0f
            )
        }

        saveTrackingState(true)
        startLocationUpdates()
    }

    private fun resumeTracking() {
        _state.update {
            it.copy(
                isTracking = true,
                error = null
            )
        }
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        trackingJob?.cancel()
        trackingJob = viewModelScope.launch {
            locationManager.getLocationUpdates(
                intervalMillis = 2000L,
                minimalDistance = 5f
            )
                .catch { e ->
                    _state.update {
                        it.copy(
                            isTracking = false,
                            error = "Tracking error: ${e.message}"
                        )
                    }
                    saveTrackingState(false)
                }
                .collectLatest { location ->
                    updateLocation(location)
                }
        }
    }

    private fun updateLocation(location: Location) {
        val distance = lastLocation?.distanceTo(location) ?: 0f

        _state.update { currentState ->
            currentState.copy(
                currentLocation = location,
                locationHistory = currentState.locationHistory + location,
                distanceTraveled = currentState.distanceTraveled + distance,
                runDistance = if (currentState.isRunActive) {
                    currentState.runDistance + distance
                } else {
                    currentState.runDistance
                }
            )
        }

        lastLocation = location

        // Sprawdź czy odwiedzono checkpoint
        checkCheckpointVisit(location)

        viewModelScope.launch {
            mapStateRepository.saveDistance(_state.value.distanceTraveled)
        }
    }

    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null

        _state.update {
            it.copy(isTracking = false)
        }

        saveTrackingState(false)
    }

    fun getCurrentLocation() {
        viewModelScope.launch {
            if (!locationManager.hasLocationPermission()) {
                _state.update { it.copy(error = "Location permission required") }
                return@launch
            }

            val location = locationManager.getCurrentLocation()
            _state.update {
                it.copy(
                    currentLocation = location,
                    error = if (location == null) "Failed to get location" else null
                )
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    // ==================== CHECKPOINTS ====================

    fun addCheckpoint(longitude: Double, latitude: Double, name: String = "") {
        val checkpoint = Checkpoint(
            position = Position(longitude = longitude, latitude = latitude),
            name = name.ifEmpty { "Control Point ${_state.value.checkpoints.size + 1}" }
        )
        _state.update {
            it.copy(checkpoints = it.checkpoints + checkpoint)
        }
        saveCheckpoints()
    }

    fun removeCheckpoint(id: String) {
        _state.update {
            it.copy(checkpoints = it.checkpoints.filter { checkpoint -> checkpoint.id != id })
        }
        saveCheckpoints()
    }

    fun clearCheckpoints() {
        _state.update {
            it.copy(
                checkpoints = emptyList(),
                currentMapId = null,
                currentMapName = null
            )
        }
        saveCheckpoints()
        saveCurrentMapInfo()
    }

    fun updateCheckpointName(id: String, newName: String) {
        _state.update { currentState ->
            currentState.copy(
                checkpoints = currentState.checkpoints.map { checkpoint ->
                    if (checkpoint.id == id) {
                        checkpoint.copy(name = newName)
                    } else {
                        checkpoint
                    }
                }
            )
        }
        saveCheckpoints()
    }

    fun moveCheckpointUp(id: String) {
        val list = _state.value.checkpoints.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index > 0) {
            val item = list.removeAt(index)
            list.add(index - 1, item)
            _state.update { it.copy(checkpoints = list) }
            saveCheckpoints()
        }
    }

    fun moveCheckpointDown(id: String) {
        val list = _state.value.checkpoints.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index != -1 && index < list.size - 1) {
            val item = list.removeAt(index)
            list.add(index + 1, item)
            _state.update { it.copy(checkpoints = list) }
            saveCheckpoints()
        }
    }

    fun resetTracking() {
        _state.update {
            MapState(hasPermission = it.hasPermission)
        }
        lastLocation = null

        viewModelScope.launch {
            mapStateRepository.clearState()
        }
    }

    fun saveCurrentMap(name: String, description: String = "", location: String = "") {
        viewModelScope.launch {
            val auth = authRepository.getCurrentAuth()
            if (auth == null) {
                _state.update { it.copy(error = "You must be logged in to save map") }
                return@launch
            }

            val controlPoints = _state.value.checkpoints.mapIndexed { index, cp ->
                ControlPoint(
                    id = index.toLong(),
                    latitude = cp.position.latitude,
                    longitude = cp.position.longitude
                )
            }

            val result = mapRepository.createMap(
                userId = auth.userId,
                name = name,
                description = description,
                location = location,
                controlPoints = controlPoints
            )

            result.onSuccess {
                _state.update { it.copy(error = null) }
            }.onFailure { e ->
                _state.update { it.copy(error = "Save error: ${e.message}") }
            }
        }
    }

    fun loadMap(mapId: Long) {
        viewModelScope.launch {
            val map = mapRepository.getMapByIdFlow(mapId).first()
            if (map == null) {
                _state.update { it.copy(error = "Map not found") }
                return@launch
            }

            val checkpoints = map.controlPoints.mapIndexed { index, cp ->
                Checkpoint(
                    position = Position(cp.longitude, cp.latitude),
                    name = "Punkt ${index + 1}"
                )
            }

            _state.update {
                it.copy(
                    checkpoints = checkpoints,
                    currentMapId = mapId,
                    currentMapName = map.name
                )
            }
            _shouldMoveCamera.value = true

            saveCheckpoints()
            saveCurrentMapInfo()
        }
    }

    fun moveCheckpoint(index: Int, newLongitude: Double, newLatitude: Double) {
        _state.update { currentState ->
            val updatedCheckpoints = currentState.checkpoints.toMutableList()
            if (index in updatedCheckpoints.indices) {
                val oldCheckpoint = updatedCheckpoints[index]
                updatedCheckpoints[index] = oldCheckpoint.copy(
                    position = Position(newLongitude, newLatitude)
                )
            }
            currentState.copy(checkpoints = updatedCheckpoints)
        }
    }

    // ==================== POMOCNICZE METODY ZAPISU ====================

    private fun saveCheckpoints() {
        viewModelScope.launch {
            mapStateRepository.saveCheckpoints(_state.value.checkpoints)
        }
    }

    private fun saveCurrentMapInfo() {
        viewModelScope.launch {
            mapStateRepository.saveCurrentMap(
                _state.value.currentMapId,
                _state.value.currentMapName
            )
        }
    }

    private fun saveTrackingState(isTracking: Boolean) {
        viewModelScope.launch {
            mapStateRepository.saveTrackingState(isTracking)
        }
    }

    override fun onCleared() {
        super.onCleared()
        trackingJob?.cancel()
    }
}