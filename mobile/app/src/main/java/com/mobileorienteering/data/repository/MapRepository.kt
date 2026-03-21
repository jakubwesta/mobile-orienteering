package com.mobileorienteering.data.repository

import com.mobileorienteering.data.api.ApiHelper
import com.mobileorienteering.data.api.service.MapApiService
import com.mobileorienteering.data.local.dao.ControlPointDao
import com.mobileorienteering.data.local.dao.MapDao
import com.mobileorienteering.data.local.entity.ControlPointEntity
import com.mobileorienteering.data.local.entity.MapEntity
import com.mobileorienteering.data.local.toDomainModel
import com.mobileorienteering.data.model.domain.Map
import com.mobileorienteering.data.model.network.request.CreateMapRequest
import com.mobileorienteering.data.model.network.request.UpdateMapRequest
import com.mobileorienteering.data.model.network.response.MapResponse
import com.mobileorienteering.data.model.network.response.toDomainModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapRepository @Inject constructor(
    private val mapApi: MapApiService,
    private val mapDao: MapDao,
    private val controlPointDao: ControlPointDao,
    private val authRepository: AuthRepository
) {

    fun getAllMapsFlow(): Flow<List<Map>> {
        return mapDao.getAllMapsWithControlPoints().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    fun getMapByIdFlow(mapId: Long): Flow<Map?> {
        return mapDao.getMapWithControlPointsFlow(mapId).map { it?.toDomainModel() }
    }

    suspend fun createMap(request: CreateMapRequest): Result<Map> {
        val userId = authRepository.getCurrentAuth()?.userId
            ?: return Result.failure(Exception("Not authenticated"))

        return try {
            val tempId = -(System.currentTimeMillis())

            mapDao.insertMap(
                MapEntity(
                    id = tempId,
                    userId = userId,
                    name = request.name,
                    description = request.description,
                    isSnapshot = false,
                    originalMapId = null,
                    createdAt = Instant.now(),
                    syncedWithServer = false
                )
            )
            controlPointDao.insertControlPoints(
                request.controlPoints.map { cp ->
                    ControlPointEntity(
                        id = 0,
                        mapId = tempId,
                        lat = cp.lat,
                        lon = cp.lon,
                        name = cp.name,
                        sequence = cp.sequence
                    )
                }
            )

            ApiHelper.safeApiCall("Failed to create map") {
                mapApi.createMap(request)
            }.onSuccess { response ->
                mapDao.deleteMap(tempId)
                saveMapResponseLocally(response)
            }.map { response ->
                response.toDomainModel()
            }.recoverCatching {
                mapDao.getMapWithControlPoints(tempId)?.toDomainModel()
                    ?: throw Exception("Map not found after save")
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to create map: ${e.message}"))
        }
    }

    suspend fun updateMap(mapId: Long, request: UpdateMapRequest): Result<Map> {
        return try {
            val existing = mapDao.getMapById(mapId)
                ?: return Result.failure(Exception("Map not found"))

            mapDao.updateMap(existing.copy(
                name = request.name,
                description = request.description,
                syncedWithServer = false
            ))
            controlPointDao.deleteControlPointsForMap(mapId)
            controlPointDao.insertControlPoints(
                request.controlPoints.map { cp ->
                    ControlPointEntity(
                        id = 0,
                        mapId = mapId,
                        lat = cp.lat,
                        lon = cp.lon,
                        name = cp.name,
                        sequence = cp.sequence
                    )
                }
            )

            ApiHelper.safeApiCall("Failed to update map") {
                mapApi.updateMap(mapId, request)
            }.onSuccess { response ->
                mapDao.updateMap(existing.copy(
                    name = response.name,
                    description = response.description,
                    syncedWithServer = true
                ))
                controlPointDao.deleteControlPointsForMap(mapId)
                controlPointDao.insertControlPoints(
                    response.controlPoints.map { cp ->
                        ControlPointEntity(
                            id = cp.id,
                            mapId = mapId,
                            lat = cp.lat,
                            lon = cp.lon,
                            name = cp.name,
                            sequence = cp.sequence
                        )
                    }
                )
            }.map { it.toDomainModel() }.recoverCatching {
                mapDao.getMapWithControlPoints(mapId)?.toDomainModel()
                    ?: throw Exception("Map not found after update")
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update map: ${e.message}"))
        }
    }

    suspend fun deleteMap(mapId: Long): Result<Unit> {
        return try {
            val auth = authRepository.getCurrentAuth()

            if (mapId < 0 || auth?.isGuestMode == true) {
                mapDao.deleteMap(mapId)
                return Result.success(Unit)
            }

            mapDao.markForDeletion(mapId)

            ApiHelper.safeApiCall("Failed to delete map") {
                mapApi.deleteMap(mapId)
            }.onSuccess {
                mapDao.deleteMap(mapId)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to delete map: ${e.message}"))
        }
    }

    suspend fun syncMaps(): Result<Unit> {
        val auth = authRepository.getCurrentAuth()
        if (auth?.isGuestMode == true) return Result.success(Unit)

        return try {
            uploadPendingDeletions()
            uploadUnsyncedMaps()
            downloadMapsFromServer()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadPendingDeletions() {
        mapDao.getMapsToDelete().forEach { entity ->
            if (entity.id > 0) {
                ApiHelper.safeApiCall { mapApi.deleteMap(entity.id) }
                    .onSuccess { mapDao.deleteMap(entity.id) }
            } else {
                mapDao.deleteMap(entity.id)
            }
        }
    }

    private suspend fun uploadUnsyncedMaps() {
        mapDao.getUnsyncedMaps().forEach { entity ->
            val controlPoints = controlPointDao.getControlPointsForMap(entity.id)

            val request = CreateMapRequest(
                name = entity.name,
                description = entity.description,
                controlPoints = controlPoints.map { cp ->
                    com.mobileorienteering.data.model.network.request.ControlPointRequest(
                        lat = cp.lat,
                        lon = cp.lon,
                        name = cp.name,
                        sequence = cp.sequence
                    )
                }
            )

            ApiHelper.safeApiCall { mapApi.createMap(request) }
                .onSuccess { response ->
                    mapDao.deleteMap(entity.id)
                    saveMapResponseLocally(response)
                }
        }
    }

    private suspend fun downloadMapsFromServer(): Result<Unit> {
        return ApiHelper.safeApiCall("Failed to download maps") {
            mapApi.getMaps()
        }.map { serverMaps ->
            val serverIds = serverMaps.map { it.id }.toSet()

            mapDao.getAllMapsWithControlPoints().first()
                .filter { it.map.syncedWithServer && it.map.id !in serverIds }
                .forEach { mapDao.deleteMap(it.map.id) }

            serverMaps.forEach { saveMapResponseLocally(it) }
        }
    }

    private suspend fun saveMapResponseLocally(response: MapResponse) {
        mapDao.insertMap(
            MapEntity(
                id = response.id,
                userId = response.userId,
                name = response.name,
                description = response.description,
                isSnapshot = response.isSnapshot,
                originalMapId = response.originalMapId,
                createdAt = Instant.parse(response.createdAt),
                syncedWithServer = true,
                pendingDeletion = false
            )
        )
        controlPointDao.deleteControlPointsForMap(response.id)
        controlPointDao.insertControlPoints(
            response.controlPoints.map { cp ->
                ControlPointEntity(
                    id = cp.id,
                    mapId = response.id,
                    lat = cp.lat,
                    lon = cp.lon,
                    name = cp.name,
                    sequence = cp.sequence
                )
            }
        )
    }

    suspend fun clearLocalMaps() {
        mapDao.deleteAllMaps()
    }
}
