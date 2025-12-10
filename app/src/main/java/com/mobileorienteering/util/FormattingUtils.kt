package com.mobileorienteering.util

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatDate(instant: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

fun formatTime(instant: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

fun formatDistance(distanceMeters: Double): String {
    return if (distanceMeters >= 1000) {
        String.format(Locale.US, "%.2f km", distanceMeters / 1000)
    } else {
        String.format(Locale.US, "%.0f m", distanceMeters)
    }
}

fun formatDuration(durationString: String): String {
    return try {
        val duration = Duration.parse(durationString)
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        val seconds = duration.seconds % 60

        when {
            hours > 0 -> String.format(Locale.US, "%dh %02dm", hours, minutes)
            minutes > 0 -> String.format(Locale.US, "%dm %02ds", minutes, seconds)
            else -> String.format(Locale.US, "%ds", seconds)
        }
    } catch (_: Exception) {
        durationString
    }
}

fun formatDurationFromInstants(start: Instant, end: Instant): String {
    val duration = Duration.between(start, end)
    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60
    val seconds = duration.seconds % 60

    return when {
        hours > 0 -> String.format(Locale.US, "%dh %02dm %02ds", hours, minutes, seconds)
        minutes > 0 -> String.format(Locale.US, "%dm %02ds", minutes, seconds)
        else -> String.format(Locale.US, "%ds", seconds)
    }
}

/**
 * Oblicza tempo (pace) w min/km
 * @param distanceMeters dystans w metrach
 * @param durationSeconds czas w sekundach
 * @return tempo jako String (np. "5:30 min/km")
 */
fun calculatePace(distanceMeters: Double, durationSeconds: Long): String {
    if (distanceMeters <= 0 || durationSeconds <= 0) return "-"

    val distanceKm = distanceMeters / 1000.0
    val paceSecondsPerKm = durationSeconds / distanceKm

    val paceMinutes = (paceSecondsPerKm / 60).toInt()
    val paceSeconds = (paceSecondsPerKm % 60).toInt()

    return String.format(Locale.US, "%d:%02d min/km", paceMinutes, paceSeconds)
}

/**
 * Oblicza tempo między dwoma punktami w czasie
 */
fun calculatePaceBetweenInstants(
    distanceMeters: Double,
    start: Instant,
    end: Instant
): String {
    val durationSeconds = Duration.between(start, end).seconds
    return calculatePace(distanceMeters, durationSeconds)
}

/**
 * Oblicza średnią prędkość w km/h
 */
fun calculateSpeed(distanceMeters: Double, durationSeconds: Long): String {
    if (distanceMeters <= 0 || durationSeconds <= 0) return "-"

    val distanceKm = distanceMeters / 1000.0
    val hours = durationSeconds / 3600.0
    val speedKmh = distanceKm / hours

    return String.format(Locale.US, "%.1f km/h", speedKmh)
}
