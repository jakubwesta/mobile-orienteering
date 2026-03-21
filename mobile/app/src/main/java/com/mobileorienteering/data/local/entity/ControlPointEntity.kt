package com.mobileorienteering.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "control_points",
    foreignKeys = [
        ForeignKey(
            entity = MapEntity::class,
            parentColumns = ["id"],
            childColumns = ["mapId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mapId")]
)
data class ControlPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mapId: Long,
    val lat: Double,
    val lon: Double,
    val name: String,
    val sequence: Int
)
