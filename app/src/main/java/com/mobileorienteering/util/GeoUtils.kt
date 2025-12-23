package com.mobileorienteering.util

import android.location.Location

fun calculateDistanceBetweenPoints(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Double {
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0].toDouble()
}
