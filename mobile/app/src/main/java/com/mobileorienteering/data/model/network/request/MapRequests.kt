package com.mobileorienteering.data.model.network.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateMapRequest(
    val name: String,
    val description: String? = null,
    @param:Json(name = "control_points") val controlPoints: List<ControlPointRequest> = emptyList()
)

@JsonClass(generateAdapter = true)
data class UpdateMapRequest(
    val name: String,
    val description: String? = null,
    @param:Json(name = "control_points") val controlPoints: List<ControlPointRequest> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ControlPointRequest(
    val lat: Double,
    val lon: Double,
    val name: String,
    val sequence: Int
)
