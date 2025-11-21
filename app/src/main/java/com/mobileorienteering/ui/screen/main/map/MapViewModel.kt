package com.mobileorienteering.ui.screen.main.map

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileorienteering.data.model.Route
import com.mobileorienteering.data.model.RouteCheckpoint
import com.mobileorienteering.data.repository.RouteRepository
import com.mobileorienteering.ui.screen.main.map.models.Checkpoint
import com.mobileorienteering.ui.screen.main.map.models.MapState
import com.mobileorienteering.util.LocationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.maplibre.spatialk.geojson.Position
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationManager: LocationManager,
    private val routeRepository: RouteRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    private var lastLocation: Location? = null

    private val _shouldMoveCamera = MutableStateFlow(false)
    val shouldMoveCamera: StateFlow<Boolean> = _shouldMoveCamera.asStateFlow()

    fun cameraMoved() {
        _shouldMoveCamera.value = false
    }

    fun updatePermissionState() {
        _state.update {
            it.copy(hasPermission = locationManager.hasLocationPermission())
        }
    }

    fun startTracking() {
        if (!locationManager.hasLocationPermission()) {
            _state.update { it.copy(error = "Brak uprawnień do lokalizacji") }
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

        viewModelScope.launch {
            locationManager.getLocationUpdates(
                intervalMillis = 2000L,
                minimalDistance = 5f
            )
                .catch { e ->
                    _state.update {
                        it.copy(
                            isTracking = false,
                            error = "Błąd śledzenia: ${e.message}"
                        )
                    }
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
                distanceTraveled = currentState.distanceTraveled + distance
            )
        }

        lastLocation = location
    }

    fun stopTracking() {
        _state.update {
            it.copy(isTracking = false)
        }
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
    }

    fun removeCheckpoint(id: String) {
        _state.update {
            it.copy(checkpoints = it.checkpoints.filter { checkpoint -> checkpoint.id != id })
        }
    }

    fun clearCheckpoints() {
        _state.update {
            it.copy(checkpoints = emptyList())
        }
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
    }

    fun moveCheckpointUp(id: String) {
        val list = _state.value.checkpoints.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index > 0) {
            val item = list.removeAt(index)
            list.add(index - 1, item)
            _state.update { it.copy(checkpoints = list) }
        }
    }

    fun moveCheckpointDown(id: String) {
        val list = _state.value.checkpoints.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index != -1 && index < list.size - 1) {
            val item = list.removeAt(index)
            list.add(index + 1, item)
            _state.update { it.copy(checkpoints = list) }
        }
    }

    fun resetTracking() {
        _state.update {
            MapState(hasPermission = it.hasPermission)
        }
        lastLocation = null
    }

    fun saveCurrentRoute(name: String) {
        val route = Route(
            name = name,
            checkpoints = _state.value.checkpoints.mapIndexed { index, cp ->
                RouteCheckpoint(
                    longitude = cp.position.longitude,
                    latitude = cp.position.latitude,
                    name = cp.name,
                    order = index
                )
            }
        )
        routeRepository.saveRoute(route)
    }

    fun loadRoute(routeId: String) {
        val route = routeRepository.getRoute(routeId) ?: return

        val checkpoints = route.checkpoints
            .sortedBy { it.order }
            .map { cp ->
                Checkpoint(
                    position = Position(cp.longitude, cp.latitude),
                    name = cp.name
                )
            }

        _state.update {
            it.copy(checkpoints = checkpoints)
        }
        _shouldMoveCamera.value = true
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }
}