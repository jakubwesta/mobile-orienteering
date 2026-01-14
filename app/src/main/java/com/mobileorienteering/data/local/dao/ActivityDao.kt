package com.mobileorienteering.data.local.dao

import androidx.room.*
import com.mobileorienteering.data.local.entity.ActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getActivityById(id: Long): ActivityEntity?

    @Query("SELECT * FROM activities WHERE id = :id")
    fun getActivityByIdFlow(id: Long): Flow<ActivityEntity?>

    @Query("SELECT * FROM activities WHERE userId = :userId ORDER BY startTime DESC")
    fun getActivitiesByUserId(userId: Long): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE mapId = :mapId ORDER BY startTime DESC")
    fun getActivitiesByMapId(mapId: Long): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE mapId = :mapId")
    suspend fun getActivitiesByMapIdOnce(mapId: Long): List<ActivityEntity>

    @Query("SELECT * FROM activities ORDER BY startTime DESC")
    fun getAllActivities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE syncedWithServer = 0")
    suspend fun getUnsyncedActivities(): List<ActivityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<ActivityEntity>)

    @Update
    suspend fun updateActivity(activity: ActivityEntity)

    @Query("UPDATE activities SET syncedWithServer = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    @Delete
    suspend fun deleteActivity(activity: ActivityEntity)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteActivityById(id: Long)

    @Query("DELETE FROM activities")
    suspend fun deleteAllActivities()
}