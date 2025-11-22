package com.mobileorienteering.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.mobileorienteering.data.local.converter.Converters
import com.mobileorienteering.data.model.Activity
import com.mobileorienteering.data.model.PathPoint
import java.time.Instant

@Entity(tableName = "activities")
@TypeConverters(Converters::class)
data class ActivityEntity(
    @PrimaryKey val id: Long,
    val userId: Long,
    val mapId: Long,
    val title: String,
    val startTime: Instant,
    val duration: String,
    val distance: Double,
    val pathData: List<PathPoint>,
    val createdAt: Instant,
    val syncedWithServer: Boolean = true
)

fun ActivityEntity.toDomainModel(): Activity {
    return Activity(
        id = id,
        userId = userId,
        mapId = mapId,
        title = title,
        startTime = startTime,
        duration = duration,
        distance = distance,
        pathData = pathData,
        createdAt = createdAt
    )
}

fun Activity.toEntity(syncedWithServer: Boolean = true): ActivityEntity {
    return ActivityEntity(
        id = id,
        userId = userId,
        mapId = mapId,
        title = title,
        startTime = startTime,
        duration = duration,
        distance = distance,
        pathData = pathData,
        createdAt = createdAt,
        syncedWithServer = syncedWithServer
    )
}
