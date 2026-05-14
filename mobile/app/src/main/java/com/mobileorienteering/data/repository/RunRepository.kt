package com.mobileorienteering.data.repository

import androidx.room.withTransaction
import com.mobileorienteering.data.api.ApiHelper
import com.mobileorienteering.data.api.service.RunApiService
import com.mobileorienteering.data.local.AppDatabase
import com.mobileorienteering.data.local.dao.ControlPointDao
import com.mobileorienteering.data.local.dao.MapDao
import com.mobileorienteering.data.local.dao.PathPointDao
import com.mobileorienteering.data.local.dao.RunDao
import com.mobileorienteering.data.local.dao.RunSettingsDao
import com.mobileorienteering.data.local.entity.ControlPointEntity
import com.mobileorienteering.data.local.entity.MapEntity
import com.mobileorienteering.data.local.entity.PathPointEntity
import com.mobileorienteering.data.local.entity.RunEntity
import com.mobileorienteering.data.local.entity.RunSettingsEntity
import com.mobileorienteering.data.local.toDomainModel
import com.mobileorienteering.data.model.domain.Run
import com.mobileorienteering.data.model.network.request.CreateRunRequest
import com.mobileorienteering.data.model.network.request.PathPointRequest
import com.mobileorienteering.data.model.network.request.RunSettingsRequest
import com.mobileorienteering.data.model.network.response.RunResponse
import com.mobileorienteering.data.model.network.response.toDomainModel
import com.mobileorienteering.util.toInstant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RunRepository @Inject constructor(
    private val db: AppDatabase,
    private val runApi: RunApiService,
    private val runDao: RunDao,
    private val runSettingsDao: RunSettingsDao,
    private val pathPointDao: PathPointDao,
    private val mapDao: MapDao,
    private val controlPointDao: ControlPointDao,
    private val authRepository: AuthRepository
) {

    fun getAllRunsFlow(): Flow<List<Run>> {
        return runDao.getAllRunsWithDetails().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    fun getRunByIdFlow(runId: Long): Flow<Run?> {
        return runDao.getRunWithDetailsFlow(runId).map { it?.toDomainModel() }
    }

    suspend fun createRun(request: CreateRunRequest): Result<Run> {
        val userId = authRepository.getCurrentAuth()?.userId
            ?: return Result.failure(Exception("Not authenticated"))

        val originalMap = mapDao.getMapById(request.mapId)
            ?: return Result.failure(Exception("Map not found locally"))

        return try {
            val base = System.currentTimeMillis()
            val tempRunId = -base
            val tempSnapshotMapId = -(base + 1)

            mapDao.insertMap(
                MapEntity(
                    id = tempSnapshotMapId,
                    userId = originalMap.userId,
                    name = originalMap.name,
                    description = originalMap.description,
                    isSnapshot = true,
                    originalMapId = originalMap.id,
                    createdAt = Instant.now(),
                    syncedWithServer = false,
                    pendingDeletion = false,
                    imageUrl = originalMap.imageUrl,
                    localImagePath = originalMap.localImagePath
                )
            )
            controlPointDao.insertControlPoints(
                controlPointDao.getControlPointsForMap(request.mapId)
                    .map { it.copy(id = 0, mapId = tempSnapshotMapId) }
            )

            runDao.insertRun(
                RunEntity(
                    id = tempRunId,
                    userId = userId,
                    mapId = tempSnapshotMapId,
                    name = request.name,
                    startedAt = request.startedAt.toInstant(),
                    finishedAt = request.finishedAt?.toInstant(),
                    syncedWithServer = false,
                    pendingDeletion = false
                )
            )

            runSettingsDao.insertRunSettings(
                RunSettingsEntity(
                    id = 0,
                    runId = tempRunId,
                    detectionRadius = request.runSettings.detectionRadius,
                    showSelfOnMap = request.runSettings.showSelfOnMap,
                    orderedControlPoints = request.runSettings.orderedControlPoints,
                    timerStart = request.runSettings.timerStart,
                    raceStyle = request.runSettings.raceStyle,
                    orientationType = request.runSettings.orientationType
                )
            )

            pathPointDao.insertPathPoints(
                request.pathPoints.map { pp ->
                    PathPointEntity(
                        id = 0,
                        runId = tempRunId,
                        lat = pp.lat,
                        lon = pp.lon,
                        timestamp = pp.timestamp.toInstant()
                    )
                }
            )

            if (request.mapId > 0) {
                ApiHelper.safeApiCall("Failed to create run") {
                    runApi.createRun(request)
                }.onSuccess { response ->
                    db.withTransaction {
                        deleteLocalRun(tempRunId, tempSnapshotMapId)
                        saveRunResponseLocally(response, userId)
                    }
                    return Result.success(response.toDomainModel())
                }
            }

            val local = runDao.getRunWithDetails(tempRunId)?.toDomainModel()
                ?: throw Exception("Run not found after save")
            Result.success(local)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to create run: ${e.message}"))
        }
    }

    suspend fun deleteRun(runId: Long): Result<Unit> {
        return try {
            val auth = authRepository.getCurrentAuth()

            if (runId < 0 || auth?.isGuestMode == true) {
                val run = runDao.getRunById(runId)
                run?.let { deleteLocalRun(runId, it.mapId) }
                return Result.success(Unit)
            }

            runDao.markForDeletion(runId)

            ApiHelper.safeApiCall("Failed to delete run") {
                runApi.deleteRun(runId)
            }.onSuccess {
                val run = runDao.getRunById(runId)
                run?.let { deleteLocalRun(runId, it.mapId) }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to delete run: ${e.message}"))
        }
    }

    suspend fun syncRuns(): Result<Unit> {
        val auth = authRepository.getCurrentAuth()
        if (auth?.isGuestMode == true) return Result.success(Unit)

        return try {
            uploadPendingDeletions()
            uploadUnsyncedRuns()
            downloadRunsFromServer()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadPendingDeletions() {
        runDao.getRunsToDelete().forEach { entity ->
            if (entity.id > 0) {
                ApiHelper.safeApiCall { runApi.deleteRun(entity.id) }
                    .onSuccess { deleteLocalRun(entity.id, entity.mapId) }
            } else {
                deleteLocalRun(entity.id, entity.mapId)
            }
        }
    }

    private suspend fun uploadUnsyncedRuns() {
        val userId = authRepository.getCurrentAuth()?.userId ?: return

        runDao.getUnsyncedRuns().forEach { entity ->
            val snapshotMap = mapDao.getMapById(entity.mapId) ?: return@forEach
            val originalMapId = snapshotMap.originalMapId?.takeIf { it > 0 } ?: return@forEach

            val runSettings = runSettingsDao.getRunSettingsByRunId(entity.id) ?: return@forEach
            val pathPoints = pathPointDao.getPathPointsForRun(entity.id)

            val request = CreateRunRequest(
                name = entity.name,
                mapId = originalMapId,
                runSettings = RunSettingsRequest(
                    detectionRadius = runSettings.detectionRadius,
                    showSelfOnMap = runSettings.showSelfOnMap,
                    orderedControlPoints = runSettings.orderedControlPoints,
                    timerStart = runSettings.timerStart,
                    raceStyle = runSettings.raceStyle,
                    orientationType = runSettings.orientationType
                ),
                startedAt = entity.startedAt.toString(),
                finishedAt = entity.finishedAt?.toString(),
                pathPoints = pathPoints.map { pp ->
                    PathPointRequest(lat = pp.lat, lon = pp.lon, timestamp = pp.timestamp.toString())
                }
            )

            ApiHelper.safeApiCall { runApi.createRun(request) }
                .onSuccess { response ->
                    db.withTransaction {
                        deleteLocalRun(entity.id, entity.mapId)
                        saveRunResponseLocally(response, userId)
                    }
                }
        }
    }

    private suspend fun downloadRunsFromServer(): Result<Unit> {
        return ApiHelper.safeApiCall("Failed to download runs") {
            runApi.getRuns()
        }.map { serverRuns ->
            val serverIds = serverRuns.map { it.id }.toSet()

            runDao.getAllRunsWithDetails().first()
                .filter { it.run.syncedWithServer && it.run.id !in serverIds }
                .forEach { deleteLocalRun(it.run.id, it.run.mapId) }

            val userId = authRepository.getCurrentAuth()?.userId ?: return@map
            serverRuns.forEach { response ->
                db.withTransaction { saveRunResponseLocally(response, userId) }
            }
        }
    }

    private suspend fun saveRunResponseLocally(response: RunResponse, userId: Long) {
        mapDao.insertMap(
            MapEntity(
                id = response.map.id,
                userId = response.map.userId,
                name = response.map.name,
                description = response.map.description,
                isSnapshot = true,
                originalMapId = response.map.originalMapId,
                createdAt = response.map.createdAt.toInstant(),
                syncedWithServer = true,
                pendingDeletion = false,
                imageUrl = response.map.imageUrl
            )
        )
        controlPointDao.deleteControlPointsForMap(response.map.id)
        controlPointDao.insertControlPoints(
            response.map.controlPoints.map { cp ->
                ControlPointEntity(
                    id = cp.id,
                    mapId = response.map.id,
                    lat = cp.lat,
                    lon = cp.lon,
                    name = cp.name,
                    sequence = cp.sequence
                )
            }
        )

        runDao.insertRun(
            RunEntity(
                id = response.id,
                userId = userId,
                mapId = response.map.id,
                name = response.name,
                startedAt = response.startedAt.toInstant(),
                finishedAt = response.finishedAt?.toInstant(),
                syncedWithServer = true,
                pendingDeletion = false
            )
        )

        runSettingsDao.insertRunSettings(
            RunSettingsEntity(
                id = response.runSettings.id,
                runId = response.id,
                detectionRadius = response.runSettings.detectionRadius,
                showSelfOnMap = response.runSettings.showSelfOnMap,
                orderedControlPoints = response.runSettings.orderedControlPoints,
                timerStart = response.runSettings.timerStart,
                raceStyle = response.runSettings.raceStyle,
                orientationType = response.runSettings.orientationType
            )
        )

        pathPointDao.deletePathPointsForRun(response.id)
        pathPointDao.insertPathPoints(
            response.pathPoints.map { pp ->
                PathPointEntity(
                    id = pp.id,
                    runId = response.id,
                    lat = pp.lat,
                    lon = pp.lon,
                    timestamp = pp.timestamp.toInstant()
                )
            }
        )
    }

    private suspend fun deleteLocalRun(runId: Long, snapshotMapId: Long) {
        runDao.deleteRun(runId)
        mapDao.deleteMap(snapshotMapId)
    }

    suspend fun clearLocalRuns() {
        runDao.deleteAllRuns()
    }
}
