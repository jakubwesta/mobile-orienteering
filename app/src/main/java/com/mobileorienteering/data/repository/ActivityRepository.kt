package com.mobileorienteering.data.repository

import com.mobileorienteering.data.api.ActivityApiService
import com.mobileorienteering.data.local.dao.ActivityDao
import com.mobileorienteering.data.local.entity.toEntity
import com.mobileorienteering.data.local.entity.toDomainModel
import com.mobileorienteering.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import java.time.Instant

class ActivityRepository @Inject constructor(
    private val activityApi: ActivityApiService,
    private val activityDao: ActivityDao
) {

    fun getActivityByIdFlow(id: Long): Flow<Activity?> {
        return activityDao.getActivityByIdFlow(id).map { it?.toDomainModel() }
    }

    fun getActivitiesByUserIdFlow(userId: Long): Flow<List<Activity>> {
        return activityDao.getActivitiesByUserId(userId).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    fun getActivitiesByMapIdFlow(mapId: Long): Flow<List<Activity>> {
        return activityDao.getActivitiesByMapId(mapId).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    fun getAllActivitiesFlow(): Flow<List<Activity>> {
        return activityDao.getAllActivities().map { list ->
            list.map { it.toDomainModel() }
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

            syncActivityWithServer(activity)

            Result.success(tempId)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save activity: ${e.message}"))
        }
    }

    suspend fun syncActivityWithServer(activity: Activity): Result<Unit> {
        return try {
            val request = CreateActivityRequest(
                userId = activity.userId,
                mapId = activity.mapId,
                title = activity.title,
                startTime = activity.startTime.toString(),
                duration = activity.duration,
                distance = activity.distance,
                pathData = activity.pathData.map { it.toDto() }
            )

            val result = ApiHelper.safeApiCall("Failed to sync activity") {
                activityApi.createActivity(request)
            }

            result.map { response ->
                activityDao.deleteActivityById(activity.id)

                val syncedActivity = activity.copy(id = response.id)
                activityDao.insertActivity(
                    syncedActivity.toEntity(syncedWithServer = true)
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncActivitiesForUser(userId: Long): Result<Unit> {
        return ApiHelper.safeApiCall("Failed to sync activities") {
            activityApi.getActivitiesByUserId(userId)
        }.map { activities ->
            val entities = activities.map { it.toDomainModel().toEntity(syncedWithServer = true) }
            activityDao.insertActivities(entities)
        }
    }

    suspend fun syncActivitiesForMap(mapId: Long): Result<Unit> {
        return ApiHelper.safeApiCall("Failed to sync map activities") {
            activityApi.getActivitiesByMapId(mapId)
        }.map { activities ->
            val entities = activities.map { it.toDomainModel().toEntity(syncedWithServer = true) }
            activityDao.insertActivities(entities)
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

    suspend fun getUnsyncedActivities(): List<Activity> {
        return activityDao.getUnsyncedActivities().map { it.toDomainModel() }
    }

    suspend fun clearLocalActivities() {
        activityDao.deleteAllActivities()
    }
}
