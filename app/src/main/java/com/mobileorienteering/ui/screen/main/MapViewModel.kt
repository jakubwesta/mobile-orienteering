package com.mobileorienteering.ui.screen.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobileorienteering.util.LocationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.location.Location

data class LocationState(
    val currentLocation: Location? = null,
    val isTracking: Boolean = false,
    val hasPermission: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationManager: LocationManager
) : ViewModel() {

    private val stateFlow = MutableStateFlow(LocationState())
    val state: StateFlow<LocationState> = stateFlow.asStateFlow()

    fun updatePermissionState() {
        stateFlow.update {
            it.copy(hasPermission = locationManager.hasLocationPermission())
        }
    }

    fun startTracking() {
        if (!locationManager.hasLocationPermission()) {
            stateFlow.update { it.copy(error = "Location permission not granted") }
            return
        }

        viewModelScope.launch {
            locationManager.getLocationUpdates(intervalMillis = 2000L, minimalDistance = 5f)
                .onStart { stateFlow.update { it.copy(isTracking = true, error = null) } }
                .catch { e ->
                    stateFlow.update { it.copy(isTracking = false, error = e.message) }
                }
                .collectLatest { location ->
                    stateFlow.update { it.copy(currentLocation = location) }
                }
        }
    }

    fun stopTracking() {
        stateFlow.update { it.copy(isTracking = false) }
    }

    fun getCurrentLocation() {
        viewModelScope.launch {
            if (!locationManager.hasLocationPermission()) {
                stateFlow.update { it.copy(error = "Location permission not granted") }
                return@launch
            }

            val location = locationManager.getCurrentLocation()
            stateFlow.update {
                it.copy(
                    currentLocation = location,
                    error = if (location == null) "Failed to get location" else null
                )
            }
        }
    }
}
