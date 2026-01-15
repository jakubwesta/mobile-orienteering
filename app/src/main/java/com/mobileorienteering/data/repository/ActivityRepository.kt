package com.mobileorienteering.data.repository

import com.mobileorienteering.data.api.service.ActivityApiService
import com.mobileorienteering.data.api.service.MapApiService
import com.mobileorienteering.data.api.ApiHelper
import com.mobileorienteering.data.local.dao.ActivityDao
import com.mobileorienteering.data.local.dao.MapDao
import com.mobileorienteering.data.local.entity.*
import com.mobileorienteering.data.model.domain.*
import com.mobileorienteering.data.model.network.request.*
import com.mobileorienteering.data.model.network.response.*
import android.util.Log
import com.mobileorienteering.data.preferences.SettingsPreferences
import com.mobileorienteering.util.computeVisitedControlPoints
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import java.time.Duration
import java.time.Instant
import javax.inject.Singleton

@Singleton
class ActivityRepository @Inject constructor(
    private val activityApi: ActivityApiService,
    private val mapApi: MapApiService,
    private val activityDao: ActivityDao,
    private val mapDao: MapDao,
    private val settingsPreferences: SettingsPreferences
) {

    fun getAllActivitiesFlow(): Flow<List<Activity>> {
        return activityDao.getAllActivities().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    fun getActivityByIdFlow(activityId: Long): Flow<Activity?> {
        return activityDao.getActivityByIdFlow(activityId).map { it?.toDomainModel() }
    }

    suspend fun createRunActivity(
        userId: Long,
        mapId: Long,
        title: String,
        startTime: Instant,
        duration: String,
        distance: Double,
        pathData: List<PathPoint>,
        status: ActivityStatus,
        visitedControlPoints: List<VisitedControlPoint>,
        totalCheckpoints: Int
    ): Result<Long> {
        return try {
            val tempId = -(System.currentTimeMillis())

            val activity = Activity(
                id = tempId,
                userId = userId,
                mapId = mapId,
                title = title,
                startTime = startTime,
                duration = duration,
                distance = distance,
                pathData = pathData,
                createdAt = Instant.now(),
                status = status,
                visitedControlPoints = visitedControlPoints,
                totalControlPoints = totalCheckpoints
            )

            activityDao.insertActivity(
                activity.toEntity(syncedWithServer = false)
            )

            uploadActivityToServer(activity)

            Result.success(tempId)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save run activity: ${e.message}"))
        }
    }

    suspend fun deleteActivity(id: Long): Result<Unit> {
        if (id < 0) {
            activityDao.deleteActivityById(id)
            return Result.success(Unit)
        }

        return ApiHelper.safeApiCall("Failed to delete activity") {
            activityApi.deleteActivity(id)
        }.onSuccess {
            activityDao.deleteActivityById(id)
        }.onFailure {
            activityDao.deleteActivityById(id)
        }
    }

    /**
     * Full sync: uploads unsynced activities, then downloads from server.
     */
    suspend fun syncActivities(userId: Long): Result<Unit> {
        return try {
            try {
                uploadAllUnsyncedActivities()
            } catch (_: Exception) {

            }

            downloadActivitiesFromServer(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uploads all unsynced activities to the server.
     */
    private suspend fun uploadAllUnsyncedActivities(): Result<Unit> {
        return try {
            val unsyncedActivities = getUnsyncedActivities()
            var errorOccurred = false

            unsyncedActivities.forEach { activity ->
                val result = uploadActivityToServer(activity)
                result.onFailure {
                    errorOccurred = true
                }
            }

            if (errorOccurred) {
                Result.failure(Exception("Failed to upload activities"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    private suspend fun uploadActivityToServer(activity: Activity): Result<Unit> {
        return try {
            var actualMapId = activity.mapId
            var updatedActivity = activity

            if (actualMapId < 0) {
                Log.d("ActivityRepository", "Map ID is negative ($actualMapId), syncing map first...")

                val syncResult = syncMapAndGetNewId(actualMapId)
                if (syncResult.isFailure) {
                    Log.e("ActivityRepository", "Failed to sync map: ${syncResult.exceptionOrNull()?.message}")
                    return Result.failure(syncResult.exceptionOrNull() ?: Exception("Map sync failed"))
                }

                actualMapId = syncResult.getOrThrow()
                Log.d("ActivityRepository", "Map synced successfully, new ID: $actualMapId")

                updatedActivity = activity.copy(mapId = actualMapId)
                activityDao.updateActivity(updatedActivity.toEntity(syncedWithServer = false))
            }

            val request = CreateActivityRequest(
                userId = updatedActivity.userId,
                mapId = actualMapId,
                title = updatedActivity.title,
                startTime = updatedActivity.startTime.toString(),
                duration = convertToIsoDuration(updatedActivity.duration),
                distance = updatedActivity.distance,
                pathData = updatedActivity.pathData.map { it.toDto() }
            )

            val result = ApiHelper.safeApiCall("Failed to sync activity") {
                activityApi.createActivity(request)
            }

            result.map { response ->
                activityDao.deleteActivityById(updatedActivity.id)

                val syncedActivity = updatedActivity.copy(id = response.id)
                activityDao.insertActivity(
                    syncedActivity.toEntity(syncedWithServer = true)
                )
            }
        } catch (e: Exception) {
            Log.e("ActivityRepository", "uploadActivityToServer failed: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun syncMapAndGetNewId(localMapId: Long): Result<Long> {
        return try {
            val mapEntity = mapDao.getMapById(localMapId)
                ?: return Result.failure(Exception("Map not found locally: $localMapId"))

            val map = mapEntity.toDomainModel()

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
                val newMapId = response.id

                mapDao.deleteMapById(localMapId)

                val syncedMap = map.copy(id = newMapId)
                mapDao.insertMap(syncedMap.toEntity(syncedWithServer = true))

                updateActivitiesMapId(localMapId, newMapId)

                newMapId
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    private suspend fun updateActivitiesMapId(oldMapId: Long, newMapId: Long) {
        val activities = activityDao.getActivitiesByMapIdOnce(oldMapId)
        activities.forEach { entity ->
            val updated = entity.copy(mapId = newMapId)
            activityDao.updateActivity(updated)
        }
    }

    private suspend fun downloadActivitiesFromServer(userId: Long): Result<Unit> {
        return ApiHelper.safeApiCall("Failed to sync activities") {
            activityApi.getActivitiesByUserId(userId)
        }.map { serverActivities ->
            val serverActivityIds = serverActivities.map { it.id }.toSet()

            val localActivities = activityDao.getActivitiesByUserId(userId).first()
            val localActivitiesMap = localActivities.associateBy { it.id }

            val checkpointRadius = settingsPreferences.settingsFlow.first().gpsAccuracy

            if (serverActivities.isNotEmpty()) {
                localActivities
                    .filter { it.syncedWithServer && it.id !in serverActivityIds }
                    .forEach { activityDao.deleteActivityById(it.id) }
            }

            val entities = serverActivities.map { serverActivity ->
                val serverDomain = serverActivity.toDomainModel()
                val localEntity = localActivitiesMap[serverActivity.id]

                val mergedActivity = if (localEntity != null) {
                    serverDomain.copy(
                        status = localEntity.status,
                        visitedControlPoints = localEntity.visitedControlPoints,
                        totalControlPoints = localEntity.totalCheckpoints
                    )
                } else {
                    val mapEntity = mapDao.getMapById(serverActivity.mapId)
                    if (mapEntity != null && serverDomain.pathData.isNotEmpty()) {
                        val controlPoints = mapEntity.toDomainModel().controlPoints
                        val computed = computeVisitedControlPoints(
                            pathData = serverDomain.pathData,
                            controlPoints = controlPoints,
                            radiusMeters = checkpointRadius
                        )
                        val computedStatus = if (computed.size >= controlPoints.size) {
                            ActivityStatus.COMPLETED
                        } else {
                            ActivityStatus.ABANDONED
                        }
                        serverDomain.copy(
                            status = computedStatus,
                            visitedControlPoints = computed,
                            totalControlPoints = controlPoints.size
                        )
                    } else {
                        Log.w("ActivityRepository",
                            "Cannot compute status for activity ${serverActivity.id}: " +
                                    "mapEntity=${mapEntity != null}, pathData.size=${serverDomain.pathData.size}")
                        serverDomain.copy(status = ActivityStatus.ABANDONED)
                    }
                }

                mergedActivity.toEntity(syncedWithServer = true)
            }
            activityDao.insertActivities(entities)
        }
    }

    suspend fun getUnsyncedActivities(): List<Activity> {
        return activityDao.getUnsyncedActivities().map { it.toDomainModel() }
    }

    suspend fun clearLocalActivities() {
        activityDao.deleteAllActivities()
    }

    /**
     * Converts duration from display format (MM:SS or H:MM:SS) to ISO-8601 format (PT...S)
     */
    private fun convertToIsoDuration(displayDuration: String): String {
        val parts = displayDuration.split(":").map { it.toIntOrNull() ?: 0 }

        val totalSeconds = when (parts.size) {
            2 -> parts[0] * 60 + parts[1]  // MM:SS
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]  // H:MM:SS
            else -> 0
        }

        return Duration.ofSeconds(totalSeconds.toLong()).toString()
    }
}