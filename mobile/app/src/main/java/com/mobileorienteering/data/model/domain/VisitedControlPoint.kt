package com.mobileorienteering.data.model.domain

import java.time.Instant

data class VisitedControlPoint(
    val controlPointName: String,
    val order: Int,
    val visitedAt: Instant,
    val lat: Double,
    val lon: Double
)
