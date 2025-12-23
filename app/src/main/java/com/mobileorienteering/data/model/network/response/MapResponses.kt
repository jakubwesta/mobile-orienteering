package com.mobileorienteering.data.model.network.response

import com.mobileorienteering.data.model.domain.ControlPoint
import com.mobileorienteering.data.model.domain.OrienteeringMap
import com.mobileorienteering.util.toInstant
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ControlPointDto(
    val latitude: Double,
    val longitude: Double,
    val id: Long
)

@JsonClass(generateAdapter = true)
data class MapDataDto(
    val controlPoints: List<ControlPointDto>
)

@JsonClass(generateAdapter = true)
data class CreateMapResponse(
    val id: Long
)

@JsonClass(generateAdapter = true)
data class MapResponse(
    val id: Long,
    val userId: Long,
    val name: String,
    val description: String,
    val location: String,
    val mapData: MapDataDto,
    val createdAt: String
)

// Mapper functions
fun MapResponse.toDomainModel(): OrienteeringMap {
    return OrienteeringMap(
        id = id,
        userId = userId,
        name = name,
        description = description,
        location = location,
        controlPoints = mapData.controlPoints.map { it.toDomainModel() },
        createdAt = createdAt.toInstant()
    )
}

fun ControlPointDto.toDomainModel(): ControlPoint {
    return ControlPoint(
        id = id,
        latitude = latitude,
        longitude = longitude
    )
}

fun ControlPoint.toDto(): ControlPointDto {
    return ControlPointDto(
        id = id,
        latitude = latitude,
        longitude = longitude
    )
}

fun List<ControlPoint>.toMapDataDto(): MapDataDto {
    return MapDataDto(
        controlPoints = this.map { it.toDto() }
    )
}
