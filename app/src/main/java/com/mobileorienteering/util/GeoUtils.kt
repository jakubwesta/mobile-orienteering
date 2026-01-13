package com.mobileorienteering.util

import android.location.Location
import com.mobileorienteering.data.model.domain.Checkpoint
import com.mobileorienteering.data.model.domain.ControlPoint
import com.mobileorienteering.data.model.domain.PathPoint
import com.mobileorienteering.data.model.domain.VisitedControlPoint

fun calculateDistanceBetweenPoints(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Double {
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0].toDouble()
}


fun computeVisitedControlPoints(
    pathData: List<PathPoint>,
    controlPoints: List<ControlPoint>,
    radiusMeters: Int
): List<VisitedControlPoint> {
    if (pathData.isEmpty() || controlPoints.isEmpty()) {
        return emptyList()
    }

    val sortedPath = pathData.sortedBy { it.timestamp }
    val visited = mutableListOf<VisitedControlPoint>()
    var nextCheckpointIndex = 0

    for (pathPoint in sortedPath) {
        if (nextCheckpointIndex >= controlPoints.size) {
            break // Wszystkie checkpointy odwiedzone
        }

        val checkpoint = controlPoints[nextCheckpointIndex]
        val distance = calculateDistanceBetweenPoints(
            pathPoint.latitude, pathPoint.longitude,
            checkpoint.latitude, checkpoint.longitude
        )

        if (distance <= radiusMeters) {
            visited.add(
                VisitedControlPoint(
                    controlPointName = "Punkt ${nextCheckpointIndex + 1}",
                    order = nextCheckpointIndex + 1,
                    visitedAt = pathPoint.timestamp,
                    latitude = checkpoint.latitude,
                    longitude = checkpoint.longitude
                )
            )
            nextCheckpointIndex++
        }
    }

    return visited
}