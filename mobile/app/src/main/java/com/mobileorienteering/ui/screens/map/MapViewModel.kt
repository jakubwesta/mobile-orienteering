package com.mobileorienteering.ui.screens.map

import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileorienteering.data.model.domain.PathPoint
import com.mobileorienteering.data.model.domain.VisitedControlPoint
import com.mobileorienteering.data.model.network.request.ControlPointRequest
import com.mobileorienteering.data.model.network.request.CreateMapRequest
import com.mobileorienteering.data.model.network.request.CreateRunRequest
import com.mobileorienteering.data.model.network.request.PathPointRequest
import com.mobileorienteering.data.model.app.RunSettings
import com.mobileorienteering.data.model.network.request.RunSettingsRequest
import com.mobileorienteering.data.model.network.request.UpdateMapRequest
import com.mobileorienteering.data.repository.RunRepository
import com.mobileorienteering.data.repository.MapRepository
import com.mobileorienteering.data.preferences.MapStatePreferences
import com.mobileorienteering.data.preferences.SettingsPreferences
import com.mobileorienteering.data.model.app.Checkpoint
import com.mobileorienteering.data.model.app.MapState
import com.mobileorienteering.data.model.app.MapIconStyle
import com.mobileorienteering.data.model.app.MapQuality
import com.mobileorienteering.data.model.app.MapStyle
import com.mobileorienteering.data.model.domain.RaceStyle
import com.mobileorienteering.service.RunServiceManager
import com.mobileorienteering.service.RunState
import com.mobileorienteering.ui.core.Strings
import com.mobileorienteering.util.MapImageExporter
import com.mobileorienteering.util.manager.LocationManager
import com.mobileorienteering.util.manager.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.maplibre.spatialk.geojson.Position
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationManager: LocationManager,
    private val permissionManager: PermissionManager,
    private val mapRepository: MapRepository,
    private val mapStatePreferences: MapStatePreferences,
    private val settingsPreferences: SettingsPreferences,
    private val runRepository: RunRepository,
    private val runServiceManager: RunServiceManager,
    private val mapImageExporter: MapImageExporter,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    private var trackingJob: Job? = null
    private var rawLocation: Location? = null

    private var pendingRunSettings = RunSettings()

    fun setRunOptions(options: RunSettings) {
        pendingRunSettings = options
        _raceStyle.value = options.raceStyle
    }

    private val _shouldMoveCamera = MutableStateFlow(false)
    val shouldMoveCamera: StateFlow<Boolean> = _shouldMoveCamera.asStateFlow()

    private val _showLocationDuringRun = MutableStateFlow(true)
    val showLocationDuringRun: StateFlow<Boolean> = _showLocationDuringRun.asStateFlow()

    private val _centerCameraOnce = MutableStateFlow(false)
    val centerCameraOnce: StateFlow<Boolean> = _centerCameraOnce.asStateFlow()

    private val _mapStyle = MutableStateFlow(MapStyle.CLASSIC)
    val mapStyle: StateFlow<MapStyle> = _mapStyle.asStateFlow()

    private val _mapIconStyle = MutableStateFlow(MapIconStyle.MODERN)
    val mapIconStyle: StateFlow<MapIconStyle> = _mapIconStyle.asStateFlow()

    private val _mapQuality = MutableStateFlow(MapQuality.QUALITY_2K)

    private val _raceStyle = MutableStateFlow(RaceStyle.STANDARD)
    val raceStyle: StateFlow<RaceStyle> = _raceStyle.asStateFlow()

    data class FinishedRunState(
        val isCompleted: Boolean,
        val duration: String,
        val visitedControlPoints: List<VisitedControlPoint>,
        val totalCheckpoints: Int,
        val distance: Double,
        val mapId: Long,
        val mapName: String,
        val pathData: List<PathPoint>,
        val startTime: Instant,
        val finishedAt: Instant
    )

    private val _finishedRunState = MutableStateFlow<FinishedRunState?>(null)
    val finishedRunState: StateFlow<FinishedRunState?> = _finishedRunState.asStateFlow()

    val runState: StateFlow<RunState> = runServiceManager.runState

    init {
        restoreSavedState()
        observeSettings()
        observeLocationProviderState()
        runServiceManager.tryReconnect()
    }

    private fun observeLocationProviderState() {
        viewModelScope.launch {
            locationManager.observeLocationProviderChanges()
                .collect { isEnabled: Boolean ->
                    if (!isEnabled && _state.value.isTracking) {
                        stopTracking()
                        _state.update {
                            it.copy(
                                currentLocation = null,
                                error = "Location services disabled"
                            )
                        }
                    }
                }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsPreferences.settingsFlow.collect { settings ->
                _showLocationDuringRun.value = settings.showLocationDuringRun
                _mapStyle.value = settings.mapStyle
                _mapIconStyle.value = settings.mapIconStyle
                _mapQuality.value = settings.mapQuality
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
        // Use raw location for centering if available
        rawLocation?.let { raw ->
            _state.update { it.copy(currentLocation = raw) }
        }
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
        onLocationDisabled: () -> Unit,
        onStartRun: () -> Unit
    ) {
        if (!permissionManager.hasPreciseLocationPermission()) {
            onRequestLocationPermission()
            return
        }

        if (!locationManager.isLocationEnabled()) {
            onLocationDisabled()
            return
        }

        if (!permissionManager.hasNotificationPermission()) {
            onRequestNotificationPermission()
        } else {
            onStartRun()
        }
    }

    fun startRun() {
        if (!permissionManager.hasPreciseLocationPermission()) {
            _state.update { it.copy(error = "Location permission required") }
            return
        }

        if (_state.value.checkpoints.isEmpty()) {
            _state.update { it.copy(error = "No control points to run") }
            return
        }

        val mapId = _state.value.currentMapId ?: 0L
        val mapName = _state.value.currentMapName ?: "Unknown"

        runServiceManager.startRun(
            checkpoints = _state.value.checkpoints,
            mapId = mapId,
            mapName = mapName,
            detectionRadius = pendingRunSettings.detectionRadius.toInt()
        )

        _state.update { it.copy(error = null) }

        _centerCameraOnce.value = true
    }

    fun stopRun() {
        val finalRunState = runServiceManager.stopRun()

        if (!finalRunState.isActive && finalRunState.startTime != null) {
            val finishedAt = finalRunState.startTime.plusSeconds(finalRunState.elapsedSeconds)
            _finishedRunState.value = FinishedRunState(
                isCompleted = finalRunState.isCompleted,
                duration = finalRunState.durationString,
                visitedControlPoints = finalRunState.visitedControlPoints,
                totalCheckpoints = finalRunState.totalCheckpoints,
                distance = finalRunState.distance,
                mapId = finalRunState.mapId,
                mapName = finalRunState.mapName,
                pathData = finalRunState.pathData,
                startTime = finalRunState.startTime,
                finishedAt = finishedAt
            )
        }
    }

    fun saveFinishedRun(title: String) {
        val finishedRun = _finishedRunState.value ?: return

        viewModelScope.launch {
            val request = CreateRunRequest(
                name = title.ifBlank { Strings.Formatted.runTitleLabel(context, finishedRun.mapName) },
                mapId = finishedRun.mapId,
                runSettings = RunSettingsRequest(
                    detectionRadius = pendingRunSettings.detectionRadius,
                    showSelfOnMap = _showLocationDuringRun.value,
                    orderedControlPoints = pendingRunSettings.orderedControlPoints,
                    timerStart = pendingRunSettings.timerStart.value,
                    raceStyle = pendingRunSettings.raceStyle.value,
                    orientationType = pendingRunSettings.orientationType.value
                ),
                startedAt = finishedRun.startTime.toString(),
                finishedAt = finishedRun.finishedAt.toString(),
                pathPoints = finishedRun.pathData.map { pp ->
                    PathPointRequest(
                        lat = pp.lat,
                        lon = pp.lon,
                        timestamp = pp.timestamp.toString()
                    )
                }
            )

            runRepository.createRun(request)
            _finishedRunState.value = null
            _state.update { it.copy(runFinished = true) }
        }
    }

    fun discardFinishedRun() {
        _finishedRunState.value = null
        _state.update { it.copy(runFinished = true) }
    }

    fun onRunFinishedHandled() {
        _state.update { it.copy(runFinished = false) }
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
                .collectLatest { locationUpdate ->
                    rawLocation = locationUpdate.raw
                    updateLocation(locationUpdate.filtered)
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
            name = name.ifEmpty { Strings.Formatted.mapControlPointLabel(
                context,
                _state.value.checkpoints.size + 1
            ) }
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

    fun saveCurrentMap(name: String, description: String = "") {
        viewModelScope.launch {
            _state.update { it.copy(isSavingMap = true, error = null) }

            val controlPoints = _state.value.checkpoints.mapIndexed { index, cp ->
                ControlPointRequest(
                    lat = cp.position.latitude,
                    lon = cp.position.longitude,
                    name = cp.name,
                    sequence = index + 1
                )
            }

            val result = mapRepository.createMap(
                CreateMapRequest(
                    name = name,
                    description = description.ifBlank { null },
                    controlPoints = controlPoints
                )
            )

            result.onSuccess { map ->
                val imageError = generateAndUploadMapImage(map.id, _state.value.checkpoints)
                _state.update {
                    it.copy(
                        error = imageError,
                        isSavingMap = false,
                        mapSaved = imageError == null
                    )
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        error = "Save error: ${e.message}",
                        isSavingMap = false
                    )
                }
            }
        }
    }

    fun onMapSavedHandled() {
        _state.update { it.copy(mapSaved = false) }
    }

    fun updateCurrentMap(name: String, description: String = "") {
        val mapId = _state.value.currentMapId

        if (mapId == null) {
            _state.update { it.copy(error = "No map to update") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSavingMap = true, error = null) }

            val controlPoints = _state.value.checkpoints.mapIndexed { index, cp ->
                ControlPointRequest(
                    lat = cp.position.latitude,
                    lon = cp.position.longitude,
                    name = cp.name,
                    sequence = index + 1
                )
            }

            val result = mapRepository.updateMap(
                mapId = mapId,
                UpdateMapRequest(
                    name = name,
                    description = description.ifBlank { null },
                    controlPoints = controlPoints
                )
            )

            result.onSuccess { map ->
                val imageError = generateAndUploadMapImage(map.id, _state.value.checkpoints)
                _state.update {
                    it.copy(
                        currentMapName = name,
                        error = imageError,
                        isSavingMap = false
                    )
                }
                saveCurrentMapInfo()
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        error = "Update error: ${e.message}",
                        isSavingMap = false
                    )
                }
            }
        }
    }

    private suspend fun generateAndUploadMapImage(mapId: Long, checkpoints: List<Checkpoint>): String? {
        if (checkpoints.isEmpty()) return null

        return runCatching {
            val imageBytes = mapImageExporter.exportMapImage(
                checkpoints = checkpoints,
                styleUrl = _mapStyle.value.getUrl(),
                width = _mapQuality.value.width,
                height = _mapQuality.value.height
            )
            mapRepository.uploadMapImage(mapId, imageBytes, contentType = "image/jpeg").getOrThrow()
        }.fold(
            onSuccess = { null },
            onFailure = { e -> "Map saved, but image generation failed: ${e.message}" }
        )
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
                    position = Position(cp.lon, cp.lat),
                    name = cp.name.ifEmpty { Strings.Formatted.mapControlPointLabel(
                        context,
                        index + 1
                    ) }
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
