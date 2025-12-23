package com.mobileorienteering.data.model.domain

import com.mobileorienteering.ui.screen.main.map.models.Checkpoint
import org.maplibre.spatialk.geojson.Position

data class SavedMapState(
    val checkpoints: List<Checkpoint> = emptyList(),
    val currentMapId: Long? = null,
    val currentMapName: String? = null,
    val isTracking: Boolean = false,
    val distanceTraveled: Float = 0f,
    val locationHistoryJson: String = "[]"
)

// DTOs for serialization
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
