package com.mobileorienteering.data.model

import com.squareup.moshi.JsonClass
import java.time.Instant

data class Activity(
    val id: Long,
    val userId: Long,
    val mapId: Long,
    val title: String,
    val startTime: Instant,
    val duration: String,
    val distance: Double,
    val pathData: List<PathPoint>,
    val createdAt: Instant
)

@JsonClass(generateAdapter = true)
data class PathPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Instant
)

fun ActivityResponse.toDomainModel(): Activity {
    return Activity(
        id = id,
        userId = userId,
        mapId = mapId,
        title = title,
        startTime = Instant.parse(startTime),
        duration = duration,
        distance = distance,
        pathData = pathData.map { it.toDomainModel() },
        createdAt = Instant.parse(createdAt)
    )
}

fun PathPointDto.toDomainModel(): PathPoint {
    return PathPoint(
        latitude = latitude,
        longitude = longitude,
        timestamp = Instant.parse(timestamp)
    )
}

fun PathPoint.toDto(): PathPointDto {
    return PathPointDto(
        latitude = latitude,
        longitude = longitude,
        timestamp = timestamp.toString()
    )
}

