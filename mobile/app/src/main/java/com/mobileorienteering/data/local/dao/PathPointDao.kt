package com.mobileorienteering.data.local.dao

import androidx.room.*
import com.mobileorienteering.data.local.entity.PathPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PathPointDao {
    @Query("SELECT * FROM path_points WHERE runId = :runId ORDER BY timestamp ASC")
    suspend fun getPathPointsForRun(runId: Long): List<PathPointEntity>

    @Query("SELECT * FROM path_points WHERE runId = :runId ORDER BY timestamp ASC")
    fun getPathPointsForRunFlow(runId: Long): Flow<List<PathPointEntity>>

    @Query("SELECT * FROM path_points WHERE id = :id")
    suspend fun getPathPointById(id: Long): PathPointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPathPoint(pathPoint: PathPointEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPathPoints(pathPoints: List<PathPointEntity>)

    @Update
    suspend fun updatePathPoint(pathPoint: PathPointEntity)

    @Delete
    suspend fun deletePathPoint(pathPoint: PathPointEntity)

    @Query("DELETE FROM path_points WHERE runId = :runId")
    suspend fun deletePathPointsForRun(runId: Long)

    @Query("DELETE FROM path_points")
    suspend fun deleteAllPathPoints()

    @Query("SELECT COUNT(*) FROM path_points WHERE runId = :runId")
    suspend fun getPathPointCountForRun(runId: Long): Int
}
