package com.mobileorienteering.data.repository

import com.mobileorienteering.data.api.service.MapApiService
import com.mobileorienteering.data.api.ApiHelper
import com.mobileorienteering.data.local.dao.MapDao
import com.mobileorienteering.data.local.entity.toEntity
import com.mobileorienteering.data.local.entity.toDomainModel
import com.mobileorienteering.data.model.domain.*
import com.mobileorienteering.data.model.network.request.*
import com.mobileorienteering.data.model.network.response.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import java.time.Instant
import javax.inject.Singleton

@Singleton
class MapRepository @Inject constructor(
    private val mapApi: MapApiService,
    private val mapDao: MapDao
) {

    fun getAllMapsFlow(): Flow<List<OrienteeringMap>> {
        return mapDao.getAllMaps().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    fun getMapByIdFlow(mapId: Long): Flow<OrienteeringMap?> {
        return mapDao.getMapByIdFlow(mapId).map { it?.toDomainModel() }
    }

    suspend fun createMap(
        userId: Long,
        name: String,
        description: String,
        location: String,
        controlPoints: List<ControlPoint>
    ): Result<Long> {
        return try {
            // Temporary negative ID for offline data
            val tempId = -(System.currentTimeMillis())

            val map = OrienteeringMap(
                id = tempId,
                userId = userId,
                name = name,
                description = description,
                location = location,
                controlPoints = controlPoints,
                createdAt = Instant.now()
            )

            mapDao.insertMap(
                map.toEntity(syncedWithServer = false)
            )

            uploadMapToServer(map)

            Result.success(tempId)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save map: ${e.message}"))
        }
    }

    suspend fun deleteMap(id: Long): Result<Unit> {
        // If ID is negative, it's not on server yet
        if (id < 0) {
            mapDao.deleteMapById(id)
            return Result.success(Unit)
        }

        return ApiHelper.safeApiCall("Failed to delete map") {
            mapApi.deleteMap(id)
        }.onSuccess {
            mapDao.deleteMapById(id)
        }.onFailure {
            mapDao.deleteMapById(id)
        }
    }

    /**
     * Full sync: uploads unsynced maps, then downloads from server.
     */
    suspend fun syncMaps(userId: Long): Result<Unit> {
        return try {
            try {
                uploadAllUnsyncedMaps()
            } catch (_: Exception) {

            }

            downloadMapsFromServer(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uploads all unsynced maps to the server.
     */
    private suspend fun uploadAllUnsyncedMaps(): Result<Unit> {
        return try {
            val unsyncedMaps = getUnsyncedMaps()
            var errorOccurred = false

            unsyncedMaps.forEach { map ->
                val result = uploadMapToServer(map)
                result.onFailure {
                    errorOccurred = true
                }
            }

            if (errorOccurred) {
                Result.failure(Exception("Failed to upload maps"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uploads a single local map to a server.
     * Deletes local map (with negative id).
     * Inserts the synced map from server (with positive id).
     */
    private suspend fun uploadMapToServer(map: OrienteeringMap): Result<Unit> {
        return try {
            val request = CreateMapRequest(
                userId = map.userId,
                name = map.name,
                description = map.description,
                location = map.location,
                mapData = map.controlPoints.toMapDataDto()
            )

            val result = ApiHelper.safeApiCall("Failed to sync map") {
                mapApi.createMap(request)
            }

            result.map { response ->
                mapDao.deleteMapById(map.id)

                val syncedMap = map.copy(id = response.id)
                mapDao.insertMap(
                    syncedMap.toEntity(syncedWithServer = true)
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads all maps from server and inserts them locally.
     * Deletes synced, local maps, that are no longer on a server.
     */
    private suspend fun downloadMapsFromServer(userId: Long): Result<Unit> {
        return ApiHelper.safeApiCall("Failed to sync maps") {
            mapApi.getMapsByUserId(userId)
        }.map { serverMaps ->
            val serverMapIds = serverMaps.map { it.id }.toSet()

            val localMaps = mapDao.getMapsByUserId(userId).first()

            localMaps
                .filter { it.syncedWithServer && it.id !in serverMapIds }
                .forEach { mapDao.deleteMapById(it.id) }

            val entities = serverMaps.map {
                it.toDomainModel().toEntity(syncedWithServer = true)
            }

            mapDao.insertMaps(entities)
        }
    }

    suspend fun getUnsyncedMaps(): List<OrienteeringMap> {
        return mapDao.getUnsyncedMaps().map { it.toDomainModel() }
    }

    suspend fun clearLocalMaps() {
        mapDao.deleteAllMaps()
    }
}
