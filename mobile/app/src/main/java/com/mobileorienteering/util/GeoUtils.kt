package com.mobileorienteering.util

import android.location.Location
import com.mobileorienteering.data.model.domain.ControlPoint
import com.mobileorienteering.data.model.domain.PathPoint
import com.mobileorienteering.data.model.domain.VisitedControlPoint

import kotlin.math.cos
import kotlin.math.log2
import kotlin.math.pow

private const val LATITUDE_TO_RADIANS = kotlin.math.PI / 180.0

/**
 * Web Mercator scale: ground metres per **screen pixel** at [latitudeDeg] for zoom level [zoom].
 */
fun webMercatorMetersPerPixel(latitudeDeg: Double, zoom: Double): Double {
    val latRad = latitudeDeg * LATITUDE_TO_RADIANS
    return 156543.03392 * cos(latRad) / 2.0.pow(zoom)
}

/**
 * Converts a map inset expressed in **dp** to ground metres using current [zoom], latitude, and
 * [screenDensity] (typically [android.content.res.Configuration.densityDpi]/160f context).
 */
fun metersOnGroundForDpInset(
    latitudeDeg: Double,
    zoom: Double,
    insetDp: Double,
    screenDensity: Float
): Double = webMercatorMetersPerPixel(latitudeDeg, zoom) * insetDp * screenDensity

/**
 * Linear interpolation along the segment [from]-[toward] by [distanceMeters] (planar lat/lon).
 * Fine for short legs (control-to-control). Returns (latitude, longitude).
 */
fun offsetToward(
    fromLat: Double, fromLon: Double,
    towardLat: Double, towardLon: Double,
    distanceMeters: Double
): Pair<Double, Double> {
    val segmentLen = calculateDistanceBetweenPoints(fromLat, fromLon, towardLat, towardLon)
    if (segmentLen <= 1e-3) return fromLat to fromLon
    val travel = distanceMeters.coerceIn(0.0, segmentLen * 0.499)
    val t = travel / segmentLen
    val lat = fromLat + (towardLat - fromLat) * t
    val lon = fromLon + (towardLon - fromLon) * t
    return lat to lon
}

fun calculateDistanceBetweenPoints(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Double {
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0].toDouble()
}

fun calculateTotalDistance(pathPoints: List<PathPoint>): Double {
    if (pathPoints.size < 2) return 0.0
    var total = 0.0
    for (i in 1 until pathPoints.size) {
        total += calculateDistanceBetweenPoints(
            pathPoints[i - 1].lat, pathPoints[i - 1].lon,
            pathPoints[i].lat, pathPoints[i].lon
        )
    }
    return total
}

data class BoundingCamera(val centerLat: Double, val centerLon: Double, val zoom: Double)

/**
 * Computes a camera center and zoom level that fits all [latLons] (lat, lon pairs) into view
 * with roughly 50% padding on each side. Returns zoom 15 for a single point.
 */
fun boundingCamera(latLons: List<Pair<Double, Double>>): BoundingCamera {
    if (latLons.isEmpty()) return BoundingCamera(0.0, 0.0, 10.0)
    if (latLons.size == 1) return BoundingCamera(latLons[0].first, latLons[0].second, 15.0)

    val minLat = latLons.minOf { it.first }
    val maxLat = latLons.maxOf { it.first }
    val minLon = latLons.minOf { it.second }
    val maxLon = latLons.maxOf { it.second }

    val centerLat = (minLat + maxLat) / 2.0
    val centerLon = (minLon + maxLon) / 2.0

    val latSpan = (maxLat - minLat).coerceAtLeast(1e-4)
    val lonSpan = (maxLon - minLon).coerceAtLeast(1e-4)
    val maxSpan = maxOf(latSpan, lonSpan)

    // log2(360/span) = zoom where the span fills the world; subtract 1 for ~50% padding margin
    val zoom = (log2(360.0 / maxSpan) - 1.0).coerceIn(10.0, 17.0)

    return BoundingCamera(centerLat, centerLon, zoom)
}

fun computeVisitedControlPoints(
    pathData: List<PathPoint>,
    controlPoints: List<ControlPoint>,
    radiusMeters: Int,
    orderedControlPoints: Boolean = true
): List<VisitedControlPoint> {
    if (pathData.isEmpty() || controlPoints.isEmpty()) {
        return emptyList()
    }

    val sortedPath = pathData.sortedBy { it.timestamp }
    return if (orderedControlPoints) {
        computeOrderedVisitedControlPoints(sortedPath, controlPoints, radiusMeters)
    } else {
        computeAnyOrderVisitedControlPoints(sortedPath, controlPoints, radiusMeters)
    }
}

private fun controlPointLabel(checkpoint: ControlPoint, index: Int): String =
    checkpoint.name.ifEmpty { "Punkt ${index + 1}" }

private fun computeOrderedVisitedControlPoints(
    sortedPath: List<PathPoint>,
    controlPoints: List<ControlPoint>,
    radiusMeters: Int
): List<VisitedControlPoint> {
    val visited = mutableListOf<VisitedControlPoint>()
    var nextCheckpointIndex = 0

    for (pathPoint in sortedPath) {
        if (nextCheckpointIndex >= controlPoints.size) break

        val checkpoint = controlPoints[nextCheckpointIndex]
        val distance = calculateDistanceBetweenPoints(
            pathPoint.lat, pathPoint.lon,
            checkpoint.lat, checkpoint.lon
        )

        if (distance <= radiusMeters) {
            visited.add(
                VisitedControlPoint(
                    controlPointName = controlPointLabel(checkpoint, nextCheckpointIndex),
                    order = nextCheckpointIndex + 1,
                    checkpointIndex = nextCheckpointIndex,
                    visitedAt = pathPoint.timestamp,
                    lat = checkpoint.lat,
                    lon = checkpoint.lon
                )
            )
            nextCheckpointIndex++
        }
    }

    return visited
}

private fun computeAnyOrderVisitedControlPoints(
    sortedPath: List<PathPoint>,
    controlPoints: List<ControlPoint>,
    radiusMeters: Int
): List<VisitedControlPoint> {
    val visited = mutableListOf<VisitedControlPoint>()
    val visitedIndices = mutableSetOf<Int>()
    var visitSequence = 0

    for (pathPoint in sortedPath) {
        if (visitedIndices.size >= controlPoints.size) break

        for (index in controlPoints.indices) {
            if (index in visitedIndices) continue

            val checkpoint = controlPoints[index]
            val distance = calculateDistanceBetweenPoints(
                pathPoint.lat, pathPoint.lon,
                checkpoint.lat, checkpoint.lon
            )

            if (distance <= radiusMeters) {
                visitSequence++
                visited.add(
                    VisitedControlPoint(
                        controlPointName = controlPointLabel(checkpoint, index),
                        order = visitSequence,
                        checkpointIndex = index,
                        visitedAt = pathPoint.timestamp,
                        lat = checkpoint.lat,
                        lon = checkpoint.lon
                    )
                )
                visitedIndices.add(index)
                break
            }
        }
    }

    return visited
}
