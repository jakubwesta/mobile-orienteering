package com.mobileorienteering.data.model

import com.mobileorienteering.util.toInstant
import com.squareup.moshi.JsonClass
import org.maplibre.android.geometry.LatLng
import java.time.Instant

data class Map(
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
    val longitude: Double
) {
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
}

fun MapResponse.toDomainModel(): Map {
    return Map(
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
