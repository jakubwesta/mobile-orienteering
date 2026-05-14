package com.mobileorienteering.data.local.dao

import androidx.room.*
import com.mobileorienteering.data.local.entity.MapEntity
import com.mobileorienteering.data.local.entity.MapWithControlPoints
import kotlinx.coroutines.flow.Flow

@Dao
interface MapDao {
    @Transaction
    @Query("SELECT * FROM maps WHERE id = :id")
    suspend fun getMapWithControlPoints(id: Long): MapWithControlPoints?

    @Transaction
    @Query("SELECT * FROM maps WHERE id = :id")
    fun getMapWithControlPointsFlow(id: Long): Flow<MapWithControlPoints?>

    @Transaction
    @Query("SELECT * FROM maps WHERE userId = :userId AND isSnapshot = 0 ORDER BY createdAt DESC")
    fun getUserMapsWithControlPoints(userId: Long): Flow<List<MapWithControlPoints>>

    @Transaction
    @Query("SELECT * FROM maps WHERE isSnapshot = 0 ORDER BY createdAt DESC")
    fun getAllMapsWithControlPoints(): Flow<List<MapWithControlPoints>>

    @Query("SELECT * FROM maps WHERE id = :id")
    suspend fun getMapById(id: Long): MapEntity?

    @Query("SELECT * FROM maps WHERE syncedWithServer = 0 AND pendingDeletion = 0 AND isSnapshot = 0")
    suspend fun getUnsyncedMaps(): List<MapEntity>

    @Query("SELECT * FROM maps WHERE pendingDeletion = 1")
    suspend fun getMapsToDelete(): List<MapEntity>

    @Query("SELECT * FROM maps WHERE imageUploadPending = 1 AND pendingDeletion = 0 AND isSnapshot = 0")
    suspend fun getMapsWithPendingImageUploads(): List<MapEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMap(map: MapEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaps(maps: List<MapEntity>)

    @Update
    suspend fun updateMap(map: MapEntity)

    @Query("UPDATE maps SET syncedWithServer = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    @Query("UPDATE maps SET pendingDeletion = 1, syncedWithServer = 0 WHERE id = :id")
    suspend fun markForDeletion(id: Long)

    @Query("DELETE FROM maps WHERE id = :id")
    suspend fun deleteMap(id: Long)

    @Query("DELETE FROM maps")
    suspend fun deleteAllMaps()

    @Query("UPDATE maps SET id = :newId WHERE id = :oldId")
    suspend fun updateMapId(oldId: Long, newId: Long)

    @Query("UPDATE maps SET imageUrl = :imageUrl WHERE id = :id")
    suspend fun updateImageUrl(id: Long, imageUrl: String?)

    @Query("UPDATE maps SET localImagePath = :localImagePath, imageUploadPending = :imageUploadPending WHERE id = :id")
    suspend fun updateLocalImage(id: Long, localImagePath: String?, imageUploadPending: Boolean)

    @Query("UPDATE maps SET imageUrl = :imageUrl, imageUploadPending = :imageUploadPending WHERE id = :id")
    suspend fun updateRemoteImage(id: Long, imageUrl: String?, imageUploadPending: Boolean)
}
