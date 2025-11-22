package com.mobileorienteering.data.repository

import com.mobileorienteering.data.api.MapApiService
import com.mobileorienteering.data.local.dao.MapDao
import com.mobileorienteering.data.local.entity.toEntity
import com.mobileorienteering.data.local.entity.toDomainModel
import com.mobileorienteering.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import java.time.Instant
import com.mobileorienteering.data.model.Map as OrienteeringMap

class MapRepository @Inject constructor(
    private val mapApi: MapApiService,
    private val mapDao: MapDao
) {

    fun getMapByIdFlow(id: Long): Flow<OrienteeringMap?> {
        return mapDao.getMapByIdFlow(id).map { it?.toDomainModel() }
    }

    fun getMapsByUserIdFlow(userId: Long): Flow<List<OrienteeringMap>> {
        return mapDao.getMapsByUserId(userId).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    fun getAllMapsFlow(): Flow<List<OrienteeringMap>> {
        return mapDao.getAllMaps().map { list ->
            list.map { it.toDomainModel() }
        }
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

            mapDao.insertMap(map.toEntity(syncedWithServer = false))

            syncMapWithServer(map)

            Result.success(tempId)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save map: ${e.message}"))
        }
    }

    suspend fun syncMapWithServer(map: OrienteeringMap): Result<Unit> {
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
                mapDao.insertMap(syncedMap.toEntity(syncedWithServer = true))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncMapsForUser(userId: Long): Result<Unit> {
        return ApiHelper.safeApiCall("Failed to sync maps") {
            mapApi.getMapsByUserId(userId)
        }.map { maps ->
            val entities = maps.map { it.toDomainModel().toEntity(syncedWithServer = true) }
            mapDao.insertMaps(entities)
        }
    }

    suspend fun deleteMap(id: Long): Result<Unit> {
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

    suspend fun getUnsyncedMaps(): List<OrienteeringMap> {
        return mapDao.getUnsyncedMaps().map { it.toDomainModel() }
    }

    suspend fun clearLocalMaps() {
        mapDao.deleteAllMaps()
    }
}
