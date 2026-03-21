package com.mobileorienteering.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "maps")
data class MapEntity(
    @PrimaryKey val id: Long,
    val userId: Long,
    val name: String,
    val description: String?,
    val isSnapshot: Boolean,
    val originalMapId: Long?,
    val createdAt: Instant,
    val syncedWithServer: Boolean = false,
    val pendingDeletion: Boolean = false
)
