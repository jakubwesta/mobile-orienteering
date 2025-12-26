package com.mobileorienteering.data.model.domain

import android.location.Location
import org.maplibre.spatialk.geojson.Position
import java.time.Instant

data class MapState(
    val currentLocation: Location? = null,
    val isTracking: Boolean = false,
    val hasPermission: Boolean = false,
    val error: String? = null,
    val locationHistory: List<Location> = emptyList(),
    val distanceTraveled: Float = 0f,
    val trackingStartTime: Long = 0L,
    val checkpoints: List<Checkpoint> = emptyList(),

    val currentMapId: Long? = null,
    val currentMapName: String? = null,

    val isRunActive: Boolean = false,
    val runStartTime: Instant? = null,
    val visitedCheckpointIndices: Set<Int> = emptySet(),
    val runDistance: Double = 0.0,

    val nextCheckpointIndex: Int = 0,

    val runPathData: List<PathPoint> = emptyList()
)

data class Checkpoint(
    val id: String = java.util.UUID.randomUUID().toString(),
    val position: Position,
    val name: String = "",
    val timestamp: Long = System.currentTimeMillis()
)


data class SavedMapState(
    val checkpoints: List<Checkpoint> = emptyList(),
    val currentMapId: Long? = null,
    val currentMapName: String? = null,
    val isTracking: Boolean = false,
    val distanceTraveled: Float = 0f,
    val locationHistoryJson: String = "[]"
)


data class CheckpointDto(
    val id: String,
    val longitude: Double,
    val latitude: Double,
    val name: String,
    val timestamp: Long
)

fun Checkpoint.toDto(): CheckpointDto {
    return CheckpointDto(
        id = id,
        longitude = position.longitude,
        latitude = position.latitude,
        name = name,
        timestamp = timestamp
    )
}

fun CheckpointDto.toCheckpoint(): Checkpoint {
    return Checkpoint(
        id = id,
        position = Position(longitude, latitude),
        name = name,
        timestamp = timestamp
    )
}