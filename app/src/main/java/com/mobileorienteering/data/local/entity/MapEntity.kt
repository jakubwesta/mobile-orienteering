package com.mobileorienteering.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.mobileorienteering.data.local.converter.Converters
import com.mobileorienteering.data.model.ControlPoint
import com.mobileorienteering.data.model.Map
import java.time.Instant

@Entity(tableName = "maps")
@TypeConverters(Converters::class)
data class MapEntity(
    @PrimaryKey val id: Long,
    val userId: Long,
    val name: String,
    val description: String,
    val location: String,
    val controlPoints: List<ControlPoint>,
    val createdAt: Instant,
    val syncedWithServer: Boolean = true
)

fun MapEntity.toDomainModel(): Map {
    return Map(
        id = id,
        userId = userId,
        name = name,
        description = description,
        location = location,
        controlPoints = controlPoints,
        createdAt = createdAt
    )
}

fun Map.toEntity(syncedWithServer: Boolean = true): MapEntity {
    return MapEntity(
        id = id,
        userId = userId,
        name = name,
        description = description,
        location = location,
        controlPoints = controlPoints,
        createdAt = createdAt,
        syncedWithServer = syncedWithServer
    )
}
