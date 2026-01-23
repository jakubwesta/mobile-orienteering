package com.mobileorienteering.data.model.network.request

import com.mobileorienteering.data.model.network.response.MapDataDto
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateMapRequest(
    val userId: Long,
    val name: String,
    val description: String,
    val location: String,
    val mapData: MapDataDto
)
