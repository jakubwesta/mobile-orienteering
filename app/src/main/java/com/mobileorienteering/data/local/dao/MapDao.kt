package com.mobileorienteering.data.local.dao

import androidx.room.*
import com.mobileorienteering.data.local.entity.MapEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MapDao {
    @Query("SELECT * FROM maps WHERE id = :id")
    suspend fun getMapById(id: Long): MapEntity?

    @Query("SELECT * FROM maps WHERE id = :id")
    fun getMapByIdFlow(id: Long): Flow<MapEntity?>

    @Query("SELECT * FROM maps WHERE userId = :userId ORDER BY createdAt DESC")
    fun getMapsByUserId(userId: Long): Flow<List<MapEntity>>

    @Query("SELECT * FROM maps ORDER BY createdAt DESC")
    fun getAllMaps(): Flow<List<MapEntity>>

    @Query("SELECT * FROM maps WHERE syncedWithServer = 0")
    suspend fun getUnsyncedMaps(): List<MapEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMap(map: MapEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaps(maps: List<MapEntity>)

    @Update
    suspend fun updateMap(map: MapEntity)

    @Query("UPDATE maps SET syncedWithServer = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    @Delete
    suspend fun deleteMap(map: MapEntity)

    @Query("DELETE FROM maps WHERE id = :id")
    suspend fun deleteMapById(id: Long)

    @Query("DELETE FROM maps")
    suspend fun deleteAllMaps()
}
