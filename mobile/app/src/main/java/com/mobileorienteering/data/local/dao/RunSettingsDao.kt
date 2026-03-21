package com.mobileorienteering.data.local.dao

import androidx.room.*
import com.mobileorienteering.data.local.entity.RunSettingsEntity

@Dao
interface RunSettingsDao {
    @Query("SELECT * FROM run_settings WHERE id = :id")
    suspend fun getRunSettings(id: Long): RunSettingsEntity?

    @Query("SELECT * FROM run_settings WHERE runId = :runId")
    suspend fun getRunSettingsByRunId(runId: Long): RunSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRunSettings(settings: RunSettingsEntity): Long

    @Update
    suspend fun updateRunSettings(settings: RunSettingsEntity)

    @Delete
    suspend fun deleteRunSettings(settings: RunSettingsEntity)

    @Query("DELETE FROM run_settings WHERE id = :id")
    suspend fun deleteRunSettingsById(id: Long)

    @Query("DELETE FROM run_settings")
    suspend fun deleteAllRunSettings()
}
