package com.mobileorienteering.util.manager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.mobileorienteering.util.filter.RunningLocationFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationFilter = RunningLocationFilter()

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

    @Suppress("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) {
            return null
        }

        return try {
            val currentLocation = suspendCancellableCoroutine { continuation ->
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).addOnSuccessListener { location ->
                    continuation.resume(location)
                }.addOnFailureListener { _ ->
                    continuation.resume(null)
                }
            }

            currentLocation ?: fusedLocationClient.lastLocation.await()
        } catch (_: Exception) {
            null
        }
    }


    fun resetFilter() {
        locationFilter.reset()
    }

    @Suppress("MissingPermission")
    fun getLocationUpdates(
        intervalMillis: Long = 2000L,
        minimalDistance: Float = 5f
    ): Flow<Location> = callbackFlow {
        if (!hasLocationPermission()) {
            close(IllegalStateException("Location permission not granted"))
            return@callbackFlow
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalMillis
        ).apply {
            setMinUpdateDistanceMeters(minimalDistance)
            setWaitForAccurateLocation(false)
            setMaxUpdateDelayMillis(intervalMillis)
            setMinUpdateIntervalMillis(intervalMillis / 2)
        }.build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    val filteredLocation = locationFilter.filter(location)
                    trySend(filteredLocation).isSuccess
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        ).addOnFailureListener { exception ->
            close(exception)
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}

fun <T> Task<T>.await(): T? {
    return try {
        Tasks.await(this)
    } catch (_: Exception) {
        null
    }
}