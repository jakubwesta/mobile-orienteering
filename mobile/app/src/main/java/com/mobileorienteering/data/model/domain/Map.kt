package com.mobileorienteering.data.model.domain

import java.time.Instant

data class Map(
    val id: Long,
    val userId: Long,
    val name: String,
    val description: String?,
    val controlPoints: List<ControlPoint>,
    val createdAt: Instant,
    val isSnapshot: Boolean = false,
    val originalMapId: Long? = null,
    val imageUrl: String? = null,
    val localImagePath: String? = null
)

data class ControlPoint(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val name: String,
    val sequence: Int
)
