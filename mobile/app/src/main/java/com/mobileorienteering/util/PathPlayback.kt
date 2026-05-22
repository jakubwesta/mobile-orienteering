package com.mobileorienteering.util

import com.mobileorienteering.data.model.domain.PathPoint
import org.maplibre.spatialk.geojson.Position
import java.time.Duration
import java.util.Locale

private const val MIN_PLAYBACK_MS = 3_000L
private const val MAX_PLAYBACK_MS = 30_000L
private const val SPEED_LOOK_BACK_SEGMENTS = 2
private const val SPEED_LOOK_AHEAD_SEGMENTS = 2

fun pathPlaybackDurationMillis(pathData: List<PathPoint>): Long {
    if (pathData.size < 2) return MIN_PLAYBACK_MS
    val actualMs = Duration.between(
        pathData.first().timestamp,
        pathData.last().timestamp
    ).toMillis()
    return actualMs.coerceIn(MIN_PLAYBACK_MS, MAX_PLAYBACK_MS)
}

/**
 * Maps linear progress [0, 1] to a position along the path at constant geographic speed.
 */
fun interpolatePathPosition(pathData: List<PathPoint>, progress: Float): Position? {
    if (pathData.isEmpty()) return null
    if (pathData.size == 1) {
        val point = pathData.first()
        return Position(point.lon, point.lat)
    }

    val clamped = progress.coerceIn(0f, 1f)
    if (clamped <= 0f) {
        val start = pathData.first()
        return Position(start.lon, start.lat)
    }
    if (clamped >= 1f) {
        val end = pathData.last()
        return Position(end.lon, end.lat)
    }

    val totalDistance = calculateTotalDistance(pathData)
    if (totalDistance <= 0.0) {
        val start = pathData.first()
        return Position(start.lon, start.lat)
    }

    val targetDistance = totalDistance * clamped
    var accumulated = 0.0

    for (index in 0 until pathData.lastIndex) {
        val from = pathData[index]
        val to = pathData[index + 1]
        val segmentLength = calculateDistanceBetweenPoints(
            from.lat, from.lon,
            to.lat, to.lon
        )

        if (accumulated + segmentLength >= targetDistance) {
            val fraction = if (segmentLength <= 0.0) {
                0f
            } else {
                ((targetDistance - accumulated) / segmentLength).toFloat()
            }
            val lon = from.lon + (to.lon - from.lon) * fraction
            val lat = from.lat + (to.lat - from.lat) * fraction
            return Position(lon, lat)
        }

        accumulated += segmentLength
    }

    val end = pathData.last()
    return Position(end.lon, end.lat)
}

fun playbackSpeedKmh(pathData: List<PathPoint>, progress: Float): Double? {
    if (pathData.size < 2) return null

    val centerIndex = segmentIndexAtProgress(pathData, progress)
    val startIndex = (centerIndex - SPEED_LOOK_BACK_SEGMENTS).coerceAtLeast(0)
    val endIndex = (centerIndex + SPEED_LOOK_AHEAD_SEGMENTS).coerceAtMost(pathData.lastIndex - 1)

    var distanceMeters = 0.0
    for (index in startIndex..endIndex) {
        val from = pathData[index]
        val to = pathData[index + 1]
        distanceMeters += calculateDistanceBetweenPoints(
            from.lat, from.lon,
            to.lat, to.lon
        )
    }

    val durationSeconds = Duration.between(
        pathData[startIndex].timestamp,
        pathData[endIndex + 1].timestamp
    ).seconds
    if (durationSeconds <= 0L || distanceMeters <= 0.0) return 0.0

    return (distanceMeters / durationSeconds) * 3.6
}

fun formatPlaybackSpeedKmh(speedKmh: Double): String =
    String.format(Locale.US, "%.1f km/h", speedKmh)

private fun segmentIndexAtProgress(pathData: List<PathPoint>, progress: Float): Int {
    if (pathData.size < 2) return 0

    val clamped = progress.coerceIn(0f, 1f)
    if (clamped <= 0f) return 0
    if (clamped >= 1f) return pathData.lastIndex - 1

    val totalDistance = calculateTotalDistance(pathData)
    if (totalDistance <= 0.0) return 0

    val targetDistance = totalDistance * clamped
    var accumulated = 0.0

    for (index in 0 until pathData.lastIndex) {
        val from = pathData[index]
        val to = pathData[index + 1]
        val segmentLength = calculateDistanceBetweenPoints(
            from.lat, from.lon,
            to.lat, to.lon
        )

        if (accumulated + segmentLength >= targetDistance) {
            return index
        }

        accumulated += segmentLength
    }

    return pathData.lastIndex - 1
}
