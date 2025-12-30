package com.mobileorienteering.data.repository

import com.mobileorienteering.data.api.service.ActivityApiService
import com.mobileorienteering.data.api.ApiHelper
import com.mobileorienteering.data.local.dao.ActivityDao
import com.mobileorienteering.data.local.entity.toEntity
import com.mobileorienteering.data.local.entity.toDomainModel
import com.mobileorienteering.data.model.domain.*
import com.mobileorienteering.data.model.network.request.*
import com.mobileorienteering.data.model.network.response.*
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
    private val activityDao: ActivityDao
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

            Result.success(tempId)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save run activity: ${e.message}"))
        }
    }

    suspend fun createActivity(
        userId: Long,
        mapId: Long,
        title: String,
        startTime: Instant,
        duration: String,
        distance: Double,
        pathData: List<PathPoint>
    ): Result<Long> {
        return try {
            // Temporary negative ID for offline data
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
                createdAt = Instant.now()
            )

            activityDao.insertActivity(
                activity.toEntity(syncedWithServer = false)
            )

            uploadActivityToServer(activity)

            Result.success(tempId)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save activity: ${e.message}"))
        }
    }

    suspend fun deleteActivity(id: Long): Result<Unit> {
        // If ID is negative, it's not on server yet
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

    /**
     * Uploads a single local activity to a server.
     * Deletes local activity (with negative id).
     * Inserts the synced activity from server (with positive id).
     * Note: Backend doesn't support status/visitedControlPoints/totalCheckpoints fields,
     * so we preserve them locally using activity.copy()
     */
    private suspend fun uploadActivityToServer(activity: Activity): Result<Unit> {
        return try {
            val request = CreateActivityRequest(
                userId = activity.userId,
                mapId = activity.mapId,
                title = activity.title,
                startTime = activity.startTime.toString(),
                duration = convertToIsoDuration(activity.duration),
                distance = activity.distance,
                pathData = activity.pathData.map { it.toDto() }
            )

            val result = ApiHelper.safeApiCall("Failed to sync activity") {
                activityApi.createActivity(request)
            }

            result.map { response ->
                activityDao.deleteActivityById(activity.id)

                // copy() preserves status, visitedControlPoints, totalControlPoints
                val syncedActivity = activity.copy(id = response.id)
                activityDao.insertActivity(
                    syncedActivity.toEntity(syncedWithServer = true)
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads all activities from server and inserts them locally.
     * Preserves local status/checkpoint data since backend doesn't support these fields.
     * Deletes synced, local activities, that are no longer on a server.
     */
    private suspend fun downloadActivitiesFromServer(userId: Long): Result<Unit> {
        return ApiHelper.safeApiCall("Failed to sync activities") {
            activityApi.getActivitiesByUserId(userId)
        }.map { serverActivities ->
            val serverActivityIds = serverActivities.map { it.id }.toSet()

            val localActivities = activityDao.getActivitiesByUserId(userId).first()
            val localActivitiesMap = localActivities.associateBy { it.id }

            // Delete local activities that no longer exist on server
            localActivities
                .filter { it.syncedWithServer && it.id !in serverActivityIds }
                .forEach { activityDao.deleteActivityById(it.id) }

            // Merge server data while preserving local fields that backend doesn't support
            val entities = serverActivities.map { serverActivity ->
                val serverDomain = serverActivity.toDomainModel()
                val localEntity = localActivitiesMap[serverActivity.id]

                // Preserve local status/checkpoints - server doesn't support these fields
                val mergedActivity = if (localEntity != null) {
                    serverDomain.copy(
                        status = localEntity.status,
                        visitedControlPoints = localEntity.visitedControlPoints,
                        totalControlPoints = localEntity.totalCheckpoints
                    )
                } else {
                    serverDomain
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
     * Examples: "05:30" -> "PT5M30S", "1:02:03" -> "PT1H2M3S"
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