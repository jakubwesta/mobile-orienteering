package com.mobileorienteering.data.model.network.response

import com.mobileorienteering.data.model.domain.Activity
import com.mobileorienteering.data.model.domain.PathPoint
import com.mobileorienteering.util.toInstant
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PathPointDto(
    val latitude: Double,
    val longitude: Double,
    val timestamp: String
)

@JsonClass(generateAdapter = true)
data class CreateActivityResponse(
    val id: Long
)

@JsonClass(generateAdapter = true)
data class ActivityResponse(
    val id: Long,
    val userId: Long,
    val mapId: Long,
    val title: String,
    val startTime: String,
    val duration: String,
    val distance: Double,
    val pathData: List<PathPointDto>,
    val createdAt: String
)

// Mapper functions
fun ActivityResponse.toDomainModel(): Activity {
    return Activity(
        id = id,
        userId = userId,
        mapId = mapId,
        title = title,
        startTime = startTime.toInstant(),
        duration = duration,
        distance = distance,
        pathData = pathData.map { it.toDomainModel() },
        createdAt = createdAt.toInstant()
    )
}

fun PathPointDto.toDomainModel(): PathPoint {
    return PathPoint(
        latitude = latitude,
        longitude = longitude,
        timestamp = timestamp.toInstant()
    )
}

fun PathPoint.toDto(): PathPointDto {
    return PathPointDto(
        latitude = latitude,
        longitude = longitude,
        timestamp = timestamp.toString()
    )
}
