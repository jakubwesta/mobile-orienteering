package com.mobileorienteering.data.local.dao

import androidx.room.*
import com.mobileorienteering.data.local.entity.ControlPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ControlPointDao {
    @Query("SELECT * FROM control_points WHERE mapId = :mapId ORDER BY sequence ASC")
    suspend fun getControlPointsForMap(mapId: Long): List<ControlPointEntity>

    @Query("SELECT * FROM control_points WHERE mapId = :mapId ORDER BY sequence ASC")
    fun getControlPointsForMapFlow(mapId: Long): Flow<List<ControlPointEntity>>

    @Query("SELECT * FROM control_points WHERE id = :id")
    suspend fun getControlPointById(id: Long): ControlPointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertControlPoint(controlPoint: ControlPointEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertControlPoints(controlPoints: List<ControlPointEntity>)

    @Update
    suspend fun updateControlPoint(controlPoint: ControlPointEntity)

    @Delete
    suspend fun deleteControlPoint(controlPoint: ControlPointEntity)

    @Query("DELETE FROM control_points WHERE mapId = :mapId")
    suspend fun deleteControlPointsForMap(mapId: Long)

    @Query("DELETE FROM control_points")
    suspend fun deleteAllControlPoints()
}
