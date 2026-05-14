package com.mobileorienteering.data.model.domain

import java.time.Instant

data class Run(
    val id: Long,
    val userId: Long,
    val map: Map,
    val name: String,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val runSettings: RunSettings,
    val pathPoints: List<PathPoint>
)

data class RunSettings(
    val id: Long,
    val detectionRadius: Float,
    val showSelfOnMap: Boolean = true,
    val orderedControlPoints: Boolean = true,
    val timerStart: TimerStart = TimerStart.RACE_START,
    val raceStyle: RaceStyle = RaceStyle.STANDARD,
    val orientationType: OrientationType = OrientationType.FOOT
)

enum class TimerStart(val value: String) {
    RACE_START("race_start"),
    FIRST_POINT("first_point");

    companion object {
        fun fromValue(value: String) = entries.firstOrNull { it.value == value } ?: RACE_START
    }
}

enum class RaceStyle(val value: String) {
    STANDARD("standard"),
    COMPASS_BEARING("compass_bearing");

    companion object {
        fun fromValue(value: String) = entries.firstOrNull { it.value == value } ?: STANDARD
    }
}

enum class OrientationType(val value: String) {
    FOOT("foot"),
    BICYCLE("bicycle");

    companion object {
        fun fromValue(value: String) = entries.firstOrNull { it.value == value } ?: FOOT
    }
}

data class PathPoint(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val timestamp: Instant
)
