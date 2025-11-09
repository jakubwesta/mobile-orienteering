package com.mobileorienteering.ui.screen.main

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

private const val TAG = "MapViewModel"

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

        trackingJob = viewModelScope.launch {
            locationManager.getLocationUpdates(
                intervalMillis = 2000L,  // Update co 2 sekundy
                minimalDistance = 5f     // Minimalna zmiana: 5 metrów
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
                    stateFlow.update { it.copy(currentLocation = location) }
                }
        }
    }

    fun stopTracking() {
        Log.d(TAG, "Stopping location tracking")
        trackingJob?.cancel()
        trackingJob = null
        stateFlow.update { it.copy(isTracking = false) }
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