package com.mobileorienteering.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val TAG = "LocationManager"

@Singleton
class LocationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return hasFineLocation || hasCoarseLocation
    }

    fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @Suppress("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) {
            Log.e(TAG, "No location permission")
            return null
        }

        return try {
            Log.d(TAG, "Requesting current location")

            // Najpierw próbuj pobrać aktualną lokalizację
            val currentLocation = suspendCancellableCoroutine { continuation ->
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).addOnSuccessListener { location ->
                    Log.d(TAG, "Current location success: $location")
                    continuation.resume(location)
                }.addOnFailureListener { exception ->
                    Log.e(TAG, "Current location failed", exception)
                    continuation.resume(null)
                }
            }

            // Jeśli nie udało się, spróbuj ostatniej znanej lokalizacji
            if (currentLocation == null) {
                Log.d(TAG, "Trying last known location")
                fusedLocationClient.lastLocation.await()
            } else {
                currentLocation
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current location", e)
            null
        }
    }

    @Suppress("MissingPermission")
    fun getLocationUpdates(
        intervalMillis: Long = 5000L,
        minimalDistance: Float = 10f
    ): Flow<Location> = callbackFlow {
        if (!hasLocationPermission()) {
            Log.e(TAG, "No location permission for updates")
            close(IllegalStateException("Location permission not granted"))
            return@callbackFlow
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalMillis
        ).apply {
            setMinUpdateDistanceMeters(minimalDistance)
            setWaitForAccurateLocation(false) // Nie czekaj na super dokładną lokalizację
            setMaxUpdateDelayMillis(intervalMillis)
            setMinUpdateIntervalMillis(intervalMillis / 2) // Pozwól na szybsze update
        }.build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    Log.d(TAG, "Location update: lat=${location.latitude}, lon=${location.longitude}, accuracy=${location.accuracy}")
                    trySend(location).isSuccess
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                Log.d(TAG, "Location availability changed: ${availability.isLocationAvailable}")
                if (!availability.isLocationAvailable) {
                    Log.w(TAG, "Location is not available")
                }
            }
        }

        Log.d(TAG, "Requesting location updates")
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        ).addOnSuccessListener {
            Log.d(TAG, "Location updates request successful")
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Failed to request location updates", exception)
            close(exception)
        }

        awaitClose {
            Log.d(TAG, "Removing location updates")
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}

suspend fun <T> Task<T>.await(): T? {
    return try {
        Tasks.await(this)
    } catch (e: Exception) {
        Log.e(TAG, "Task await failed", e)
        null
    }
}
