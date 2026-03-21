package com.mobileorienteering.data.local.dao

import androidx.room.*
import com.mobileorienteering.data.local.entity.RunEntity
import com.mobileorienteering.data.local.entity.RunWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface RunDao {
    @Transaction
    @Query("SELECT * FROM runs WHERE id = :id")
    suspend fun getRunWithDetails(id: Long): RunWithDetails?

    @Transaction
    @Query("SELECT * FROM runs WHERE id = :id")
    fun getRunWithDetailsFlow(id: Long): Flow<RunWithDetails?>

    @Transaction
    @Query("SELECT * FROM runs WHERE userId = :userId ORDER BY startedAt DESC")
    fun getUserRunsWithDetails(userId: Long): Flow<List<RunWithDetails>>

    @Transaction
    @Query("SELECT * FROM runs WHERE mapId = :mapId ORDER BY startedAt DESC")
    fun getRunsByMapIdWithDetails(mapId: Long): Flow<List<RunWithDetails>>

    @Transaction
    @Query("SELECT * FROM runs ORDER BY startedAt DESC")
    fun getAllRunsWithDetails(): Flow<List<RunWithDetails>>

    @Query("SELECT * FROM runs WHERE id = :id")
    suspend fun getRunById(id: Long): RunEntity?

    @Query("SELECT * FROM runs WHERE mapId = :mapId")
    suspend fun getRunsByMapId(mapId: Long): List<RunEntity>

    @Query("SELECT * FROM runs WHERE syncedWithServer = 0 AND pendingDeletion = 0")
    suspend fun getUnsyncedRuns(): List<RunEntity>

    @Query("SELECT * FROM runs WHERE pendingDeletion = 1")
    suspend fun getRunsToDelete(): List<RunEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: RunEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRuns(runs: List<RunEntity>)

    @Update
    suspend fun updateRun(run: RunEntity)

    @Query("UPDATE runs SET syncedWithServer = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    @Query("UPDATE runs SET pendingDeletion = 1, syncedWithServer = 0 WHERE id = :id")
    suspend fun markForDeletion(id: Long)

    @Query("DELETE FROM runs WHERE id = :id")
    suspend fun deleteRun(id: Long)

    @Query("DELETE FROM runs")
    suspend fun deleteAllRuns()

    @Query("UPDATE runs SET id = :newId WHERE id = :oldId")
    suspend fun updateRunId(oldId: Long, newId: Long)
}
