package com.mobileorienteering.util.manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mobileorienteering.util.filter.RunningLocationFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton
import android.location.LocationManager as AndroidLocationManager


data class LocationUpdate(
    val raw: Location,
    val filtered: Location
)

@Singleton
class LocationManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val permissionManager: PermissionManager
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationFilter = RunningLocationFilter()

    fun hasLocationPermission(): Boolean {
        return permissionManager.hasLocationPermission()
    }

    fun hasPreciseLocationPermission(): Boolean {
        return permissionManager.hasPreciseLocationPermission()
    }

    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE)
                as android.location.LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(
            android.location.LocationManager.GPS_PROVIDER
        )
        val isNetworkEnabled = locationManager.isProviderEnabled(
            android.location.LocationManager.NETWORK_PROVIDER
        )
        return isGpsEnabled || isNetworkEnabled
    }

    fun resetFilter() {
        locationFilter.reset()
    }

    @Suppress("MissingPermission")
    fun getLocationUpdates(
        intervalMillis: Long = 2000L,
        minimalDistance: Float = 5f
    ): Flow<LocationUpdate> = callbackFlow {
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
                    trySend(LocationUpdate(
                        raw = location,
                        filtered = filteredLocation
                    )).isSuccess
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

    fun observeLocationProviderChanges(): Flow<Boolean> = callbackFlow {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE)
                as AndroidLocationManager

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val isGpsEnabled = locationManager.isProviderEnabled(
                    AndroidLocationManager.GPS_PROVIDER
                )
                val isNetworkEnabled = locationManager.isProviderEnabled(
                    AndroidLocationManager.NETWORK_PROVIDER
                )
                trySend(isGpsEnabled || isNetworkEnabled)
            }
        }

        val isEnabled = locationManager.isProviderEnabled(
            AndroidLocationManager.GPS_PROVIDER
        ) || locationManager.isProviderEnabled(
            AndroidLocationManager.NETWORK_PROVIDER
        )
        trySend(isEnabled)

        val filter = IntentFilter(AndroidLocationManager.PROVIDERS_CHANGED_ACTION)
        context.registerReceiver(receiver, filter)

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }
}
