package com.mobileorienteering.ui.screen.main.map

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileorienteering.util.LocationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapState(
    val currentLocation: Location? = null,
    val isTracking: Boolean = false,
    val hasPermission: Boolean = false,
    val error: String? = null,
    val locationHistory: List<Location> = emptyList(),
    val distanceTraveled: Float = 0f,
    val trackingStartTime: Long = 0L,
    val checkpoints: List<Checkpoint> = emptyList()
)

data class Checkpoint(
    val id: String = java.util.UUID.randomUUID().toString(),
    val position: org.maplibre.spatialk.geojson.Position,
    val name: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationManager: LocationManager
) : ViewModel() {

    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    private var lastLocation: Location? = null

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
                intervalMillis = 2000L,  // Aktualizacja co 2 sekundy
                minimalDistance = 5f      // Minimalna zmiana 5 metrów
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
        // Oblicz dystans jeśli mamy poprzednią lokalizację
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
            position = org.maplibre.spatialk.geojson.Position(
                longitude = longitude,
                latitude = latitude
            ),
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

    fun resetTracking() {
        _state.update {
            MapState(hasPermission = it.hasPermission)
        }
        lastLocation = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }
}