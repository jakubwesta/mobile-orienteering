package com.mobileorienteering.data.model.network.request

import com.mobileorienteering.data.model.network.response.PathPointDto
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateActivityRequest(
    val userId: Long,
    val mapId: Long,
    val title: String,
    val startTime: String,
    val duration: String,
    val distance: Double,
    val pathData: List<PathPointDto>
)
