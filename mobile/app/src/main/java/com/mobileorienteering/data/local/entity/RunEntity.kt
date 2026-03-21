package com.mobileorienteering.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "runs",
    foreignKeys = [
        ForeignKey(
            entity = MapEntity::class,
            parentColumns = ["id"],
            childColumns = ["mapId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mapId"), Index("userId")]
)
data class RunEntity(
    @PrimaryKey val id: Long,
    val userId: Long,
    val mapId: Long,
    val name: String,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val syncedWithServer: Boolean = false,
    val pendingDeletion: Boolean = false
)
