package com.mobileorienteering.data.model.domain

import com.squareup.moshi.JsonClass
import java.time.Instant

data class OrienteeringMap(
    val id: Long,
    val userId: Long,
    val name: String,
    val description: String,
    val location: String,
    val controlPoints: List<ControlPoint>,
    val createdAt: Instant
)

@JsonClass(generateAdapter = true)
data class ControlPoint(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val name: String = ""
)
