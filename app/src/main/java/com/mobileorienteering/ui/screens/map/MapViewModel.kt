package com.mobileorienteering.ui.screens.map

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileorienteering.data.model.domain.ActivityStatus
import com.mobileorienteering.data.model.domain.ControlPoint
import com.mobileorienteering.data.model.domain.PathPoint
import com.mobileorienteering.data.model.domain.VisitedControlPoint
import com.mobileorienteering.data.repository.ActivityRepository
import com.mobileorienteering.data.repository.AuthRepository
import com.mobileorienteering.data.repository.MapRepository
import com.mobileorienteering.data.preferences.MapStatePreferences
import com.mobileorienteering.data.preferences.SettingsPreferences
import com.mobileorienteering.data.model.domain.Checkpoint
import com.mobileorienteering.data.model.domain.MapState
import com.mobileorienteering.service.RunServiceManager
import com.mobileorienteering.service.RunState
import com.mobileorienteering.util.manager.LocationManager
import com.mobileorienteering.util.manager.NotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.maplibre.spatialk.geojson.Position
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationManager: LocationManager,
    private val notificationManager: NotificationManager,
    private val mapRepository: MapRepository,
    private val authRepository: AuthRepository,
    private val mapStatePreferences: MapStatePreferences,
    private val settingsPreferences: SettingsPreferences,
    private val activityRepository: ActivityRepository,
    private val runServiceManager: RunServiceManager
) : ViewModel() {

    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    private var trackingJob: Job? = null

    private val _mapZoom = MutableStateFlow(16.0)
    val mapZoom: StateFlow<Double> = _mapZoom.asStateFlow()

    private val _shouldMoveCamera = MutableStateFlow(false)
    val shouldMoveCamera: StateFlow<Boolean> = _shouldMoveCamera.asStateFlow()

    private val _showLocationDuringRun = MutableStateFlow(true)
    val showLocationDuringRun: StateFlow<Boolean> = _showLocationDuringRun.asStateFlow()

    private val _centerCameraOnce = MutableStateFlow(false)
    val centerCameraOnce: StateFlow<Boolean> = _centerCameraOnce.asStateFlow()

    data class FinishedRunState(
        val isCompleted: Boolean,
        val duration: String,
        val visitedControlPoints: List<VisitedControlPoint>,
        val totalCheckpoints: Int,
        val distance: Double,
        val mapId: Long,
        val mapName: String,
        val pathData: List<PathPoint>,
        val startTime: Instant
    )

    private val _finishedRunState = MutableStateFlow<FinishedRunState?>(null)
    val finishedRunState: StateFlow<FinishedRunState?> = _finishedRunState.asStateFlow()

    val runState: StateFlow<RunState> = runServiceManager.runState

    init {
        restoreSavedState()
        observeSettings()
        runServiceManager.tryReconnect()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsPreferences.settingsFlow.collect { settings ->
                _mapZoom.value = settings.mapZoom.toDouble()
                _showLocationDuringRun.value = settings.showLocationDuringRun
            }
        }
    }

    private fun restoreSavedState() {
        viewModelScope.launch {
            val savedState = mapStatePreferences.getSavedState()

            _state.update {
                it.copy(
                    checkpoints = savedState.checkpoints,
                    currentMapId = savedState.currentMapId,
                    currentMapName = savedState.currentMapName,
                    hasPermission = locationManager.hasLocationPermission()
                )
            }

            if (savedState.isTracking && locationManager.hasLocationPermission()) {
                resumeTracking()
            }

            if (savedState.checkpoints.isNotEmpty()) {
                _shouldMoveCamera.value = true
            }
        }
    }

    fun cameraMoved() {
        _shouldMoveCamera.value = false
    }

    fun cameraCentered() {
        _centerCameraOnce.value = false
    }

    fun requestCenterCamera() {
        _centerCameraOnce.value = true
    }

    fun updatePermissionState() {
        _state.update {
            it.copy(hasPermission = locationManager.hasLocationPermission())
        }
    }

    fun handleStartRun(
        onRequestLocationPermission: () -> Unit,
        onRequestNotificationPermission: () -> Unit,
        onStartRun: () -> Unit
    ) {
        if (!locationManager.hasLocationPermission()) {
            onRequestLocationPermission()
            return
        }

        if (!notificationManager.hasNotificationPermission()) {
            onRequestNotificationPermission()
        } else {
            onStartRun()
        }
    }

    fun startRun() {
        if (_state.value.checkpoints.isEmpty()) {
            _state.update { it.copy(error = "No control points to run") }
            return
        }

        val mapId = _state.value.currentMapId ?: 0L
        val mapName = _state.value.currentMapName ?: "Unknown"

        runServiceManager.startRun(
            checkpoints = _state.value.checkpoints,
            mapId = mapId,
            mapName = mapName
        )

        _state.update { it.copy(error = null) }

        _centerCameraOnce.value = true
    }

    fun stopRun() {
        val finalRunState = runServiceManager.stopRun()

        if (!finalRunState.isActive && finalRunState.startTime != null) {
            _finishedRunState.value = FinishedRunState(
                isCompleted = finalRunState.isCompleted,
                duration = finalRunState.durationString,
                visitedControlPoints = finalRunState.visitedControlPoints,
                totalCheckpoints = finalRunState.totalCheckpoints,
                distance = finalRunState.distance,
                mapId = finalRunState.mapId,
                mapName = finalRunState.mapName,
                pathData = finalRunState.pathData,
                startTime = finalRunState.startTime
            )
        }
    }

    fun saveFinishedRun(title: String) {
        val finishedRun = _finishedRunState.value ?: return

        viewModelScope.launch {
            val auth = authRepository.getCurrentAuth()
            val userId = auth?.userId ?: -1L

            activityRepository.createRunActivity(
                userId = userId,
                mapId = finishedRun.mapId,
                title = title.ifBlank { "Run: ${finishedRun.mapName}" },
                startTime = finishedRun.startTime,
                duration = finishedRun.duration,
                distance = finishedRun.distance,
                pathData = finishedRun.pathData,
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

    fun startTracking() {
        if (!locationManager.hasLocationPermission()) {
            _state.update { it.copy(error = "Location permission required") }
            return
        }

        _state.update {
            it.copy(
                isTracking = true,
                error = null
            )
        }

        _centerCameraOnce.value = true

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

        locationManager.resetFilter()

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
        _state.update { currentState ->
            currentState.copy(currentLocation = location)
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

    fun handleLocationFabClick(
        onRequestPermission: () -> Unit,
        onLocationEnabled: () -> Unit,
        onLocationDisabled: () -> Unit
    ) {
        if (!locationManager.hasLocationPermission()) {
            onRequestPermission()
        } else if (locationManager.isLocationEnabled()) {
            onLocationEnabled()
        } else {
            onLocationDisabled()
        }
    }

    fun handleLocationPermissionGranted(
        onLocationEnabled: () -> Unit,
        onLocationDisabled: () -> Unit
    ) {
        if (locationManager.isLocationEnabled()) {
            onLocationEnabled()
        } else {
            onLocationDisabled()
        }
    }

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

    fun detachFromMap() {
        _state.update {
            it.copy(
                currentMapId = null,
                currentMapName = null
            )
        }
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

    fun saveCurrentMap(name: String, description: String = "", location: String = "") {
        viewModelScope.launch {
            val auth = authRepository.getCurrentAuth()
            val userId = auth?.userId ?: -1L

            val controlPoints = _state.value.checkpoints.mapIndexed { index, cp ->
                ControlPoint(
                    id = index.toLong(),
                    latitude = cp.position.latitude,
                    longitude = cp.position.longitude,
                    name = cp.name
                )
            }

            val result = mapRepository.createMap(
                userId = userId,
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

    fun updateCurrentMap(name: String, description: String = "", location: String = "") {
        val mapId = _state.value.currentMapId
        Log.d("MapViewModel", "updateCurrentMap called, mapId=$mapId, name=$name")

        if (mapId == null) {
            Log.e("MapViewModel", "No map to update - currentMapId is null")
            _state.update { it.copy(error = "No map to update") }
            return
        }

        viewModelScope.launch {
            val auth = authRepository.getCurrentAuth()
            val userId = auth?.userId ?: -1L

            val controlPoints = _state.value.checkpoints.mapIndexed { index, cp ->
                ControlPoint(
                    id = index.toLong(),
                    latitude = cp.position.latitude,
                    longitude = cp.position.longitude,
                    name = cp.name
                )
            }

            Log.d("MapViewModel", "Calling mapRepository.updateMap for mapId=$mapId")

            val result = mapRepository.updateMap(
                mapId = mapId,
                userId = userId,
                name = name,
                description = description,
                location = location,
                controlPoints = controlPoints
            )

            result.onSuccess {
                Log.d("MapViewModel", "Update successful")
                _state.update {
                    it.copy(
                        currentMapName = name,
                        error = null
                    )
                }
                saveCurrentMapInfo()
            }.onFailure { e ->
                Log.e("MapViewModel", "Update failed: ${e.message}")
                _state.update { it.copy(error = "Update error: ${e.message}") }
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
                    name = cp.name.ifEmpty { "Point ${index + 1}" }
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
        saveCheckpoints()
    }

    private fun saveCheckpoints() {
        viewModelScope.launch {
            mapStatePreferences.saveCheckpoints(_state.value.checkpoints)
        }
    }

    private fun saveCurrentMapInfo() {
        viewModelScope.launch {
            mapStatePreferences.saveCurrentMap(
                _state.value.currentMapId,
                _state.value.currentMapName
            )
        }
    }

    private fun saveTrackingState(isTracking: Boolean) {
        viewModelScope.launch {
            mapStatePreferences.saveTrackingState(isTracking)
        }
    }

    override fun onCleared() {
        super.onCleared()
        trackingJob?.cancel()
    }
}
