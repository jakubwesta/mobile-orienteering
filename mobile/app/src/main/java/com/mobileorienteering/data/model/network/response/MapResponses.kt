package com.mobileorienteering.data.model.network.response

import com.mobileorienteering.data.model.domain.ControlPoint
import com.mobileorienteering.data.model.domain.Map
import com.mobileorienteering.util.toInstant
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ControlPointResponse(
    val id: Long,
    @param:Json(name = "map_id") val mapId: Long,
    val lat: Double,
    val lon: Double,
    val name: String,
    val sequence: Int
)

@JsonClass(generateAdapter = true)
data class MapResponse(
    val id: Long,
    @param:Json(name = "user_id") val userId: Long,
    val name: String,
    val description: String?,
    @param:Json(name = "is_snapshot") val isSnapshot: Boolean,
    @param:Json(name = "original_map_id") val originalMapId: Long?,
    @param:Json(name = "created_at") val createdAt: String,
    @param:Json(name = "control_points") val controlPoints: List<ControlPointResponse>,
    @param:Json(name = "image_key") val imageKey: String? = null,
    @param:Json(name = "image_url") val imageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class MapImageUploadUrlResponse(
    @param:Json(name = "upload_url") val uploadUrl: String,
    @param:Json(name = "object_key") val objectKey: String,
    @param:Json(name = "expires_in") val expiresIn: Int
)

fun MapResponse.toDomainModel(): Map {
    return Map(
        id = id,
        userId = userId,
        name = name,
        description = description,
        isSnapshot = isSnapshot,
        originalMapId = originalMapId,
        createdAt = createdAt.toInstant(),
        controlPoints = controlPoints.map { it.toDomainModel() },
        imageUrl = imageUrl
    )
}

fun ControlPointResponse.toDomainModel(): ControlPoint {
    return ControlPoint(
        id = id,
        lat = lat,
        lon = lon,
        name = name,
        sequence = sequence
    )
}
