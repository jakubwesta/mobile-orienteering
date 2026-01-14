package com.mobileorienteering.util.manager

import com.mobileorienteering.data.repository.ActivityRepository
import com.mobileorienteering.data.repository.MapRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val mapRepository: MapRepository
) {

    /**
     * Full sync: uploads all unsynced data, then downloads from server.
     * Maps are synced FIRST because activities need map data to compute status.
     */
    suspend fun syncAllDataForUser(userId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Sync maps FIRST - activities need map data to compute status
            val mapResult = mapRepository.syncMaps(userId)
            val activityResult = activityRepository.syncActivities(userId)

            when {
                activityResult.isFailure && mapResult.isFailure ->
                    Result.failure(Exception("Both syncs failed"))

                mapResult.isFailure ->
                    Result.failure(Exception("Map sync failed: ${mapResult.exceptionOrNull()?.message}"))

                activityResult.isFailure ->
                    Result.failure(Exception("Activity sync failed: ${activityResult.exceptionOrNull()?.message}"))

                else -> Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}