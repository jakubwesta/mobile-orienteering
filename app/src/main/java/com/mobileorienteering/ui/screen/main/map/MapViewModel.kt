package com.mobileorienteering.ui.screen.main.map

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileorienteering.data.model.ControlPoint
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
import org.maplibre.spatialk.geojson.Position
import javax.inject.Inject
import com.mobileorienteering.data.model.Map as OrienteeringMap

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationManager: LocationManager,
    private val mapRepository: MapRepository,
    private val authRepository: AuthRepository,
    private val mapStateRepository: MapStateRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    private var lastLocation: Location? = null
    private var trackingJob: Job? = null
    private var isInitialized = false

    private val _shouldMoveCamera = MutableStateFlow(false)
    val shouldMoveCamera: StateFlow<Boolean> = _shouldMoveCamera.asStateFlow()

    init {
        // Odczytaj zapisany stan przy starcie
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

            // Jeśli było włączone śledzenie, wznów je
            if (savedState.isTracking && locationManager.hasLocationPermission()) {
                android.util.Log.d("MapViewModel", "Restoring tracking state...")
                resumeTracking()
            }

            // Jeśli są checkpointy, przesuń kamerę
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

    fun startTracking() {
        android.util.Log.d("MapViewModel", "startTracking() called")
        android.util.Log.d("MapViewModel", "hasLocationPermission: ${locationManager.hasLocationPermission()}")

        if (!locationManager.hasLocationPermission()) {
            android.util.Log.e("MapViewModel", "No location permission!")
            _state.update { it.copy(error = "Brak uprawnień do lokalizacji") }
            return
        }

        android.util.Log.d("MapViewModel", "Permission OK, starting tracking...")
        _state.update {
            it.copy(
                isTracking = true,
                error = null,
                trackingStartTime = System.currentTimeMillis(),
                locationHistory = emptyList(),
                distanceTraveled = 0f
            )
        }

        // Zapisz stan śledzenia
        saveTrackingState(true)

        startLocationUpdates()
    }

    /**
     * Wznawia śledzenie bez resetowania historii
     */
    private fun resumeTracking() {
        android.util.Log.d("MapViewModel", "resumeTracking() called")

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
            android.util.Log.d("MapViewModel", "Requesting location updates...")
            locationManager.getLocationUpdates(
                intervalMillis = 2000L,
                minimalDistance = 5f
            )
                .catch { e ->
                    android.util.Log.e("MapViewModel", "Location error: ${e.message}", e)
                    _state.update {
                        it.copy(
                            isTracking = false,
                            error = "Błąd śledzenia: ${e.message}"
                        )
                    }
                    saveTrackingState(false)
                }
                .collectLatest { location ->
                    android.util.Log.d("MapViewModel", "Got location: ${location.latitude}, ${location.longitude}")
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
                distanceTraveled = currentState.distanceTraveled + distance
            )
        }

        lastLocation = location

        // Zapisz dystans okresowo
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
                _state.update { it.copy(error = "Brak uprawnień do lokalizacji") }
                return@launch
            }

            val location = locationManager.getCurrentLocation()
            _state.update {
                it.copy(
                    currentLocation = location,
                    error = if (location == null) "Nie udało się pobrać lokalizacji" else null
                )
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun addCheckpoint(longitude: Double, latitude: Double, name: String = "") {
        val checkpoint = Checkpoint(
            position = Position(longitude = longitude, latitude = latitude),
            name = name.ifEmpty { "Checkpoint ${_state.value.checkpoints.size + 1}" }
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

        // Wyczyść zapisany stan
        viewModelScope.launch {
            mapStateRepository.clearState()
        }
    }

    fun saveCurrentMap(name: String, description: String = "", location: String = "") {
        viewModelScope.launch {
            val auth = authRepository.getCurrentAuth()
            if (auth == null) {
                _state.update { it.copy(error = "Musisz być zalogowany aby zapisać mapę") }
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
                android.util.Log.d("MapViewModel", "Map saved successfully: $name")
            }.onFailure { e ->
                _state.update { it.copy(error = "Błąd zapisu: ${e.message}") }
            }
        }
    }

    fun loadMap(mapId: Long) {
        viewModelScope.launch {
            val map = mapRepository.getMapById(mapId)
            if (map == null) {
                _state.update { it.copy(error = "Nie znaleziono mapy") }
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

            // Zapisz stan
            saveCheckpoints()
            saveCurrentMapInfo()
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