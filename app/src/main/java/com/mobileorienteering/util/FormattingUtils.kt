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
