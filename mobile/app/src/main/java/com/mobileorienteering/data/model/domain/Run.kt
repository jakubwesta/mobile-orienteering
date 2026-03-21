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
    val detectionRadius: Float
)

data class PathPoint(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val timestamp: Instant
)
