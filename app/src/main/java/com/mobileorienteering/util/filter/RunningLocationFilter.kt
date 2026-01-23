package com.mobileorienteering.util.filter

import android.location.Location

class RunningLocationFilter(
    /** Low-pass filter smoothing factor (0.0-1.0). Lower = smoother but more lag */
    private val smoothingFactor: Float = 0.3f,
    private val maxAccuracyMeters: Float = 30f,
    private val teleportThresholdMeters: Float = 100f
) {
    private var filteredLat: Double = 0.0
    private var filteredLon: Double = 0.0
    private var isInitialized = false

    fun filter(location: Location): Location {
        // Reject very inaccurate readings when initialized
        if (location.accuracy > maxAccuracyMeters && isInitialized) {
            return createSmoothedLocation(location, filteredLat, filteredLon)
        }

        if (!isInitialized) {
            filteredLat = location.latitude
            filteredLon = location.longitude
            isInitialized = true
            return location
        }

        // Detect teleportation
        val distance = calculateDistance(
            filteredLat, filteredLon,
            location.latitude, location.longitude
        )

        if (distance > teleportThresholdMeters) {
            // Reset filter on large jumps
            filteredLat = location.latitude
            filteredLon = location.longitude
            return location
        }

        // Normal smoothing
        filteredLat += smoothingFactor * (location.latitude - filteredLat)
        filteredLon += smoothingFactor * (location.longitude - filteredLon)

        return createSmoothedLocation(location, filteredLat, filteredLon)
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    private fun createSmoothedLocation(
        original: Location,
        smoothedLat: Double,
        smoothedLon: Double
    ): Location {
        return Location(original.provider).apply {
            latitude = smoothedLat
            longitude = smoothedLon
            time = original.time
            accuracy = original.accuracy
            elapsedRealtimeNanos = original.elapsedRealtimeNanos
            if (original.hasAltitude()) altitude = original.altitude
            if (original.hasBearing()) bearing = original.bearing
            if (original.hasSpeed()) speed = original.speed
        }
    }

    fun reset() {
        filteredLat = 0.0
        filteredLon = 0.0
        isInitialized = false
    }
}
