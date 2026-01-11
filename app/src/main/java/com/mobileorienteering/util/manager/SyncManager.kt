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
     */
    suspend fun syncAllDataForUser(userId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val activityResult = activityRepository.syncActivities(userId)
            val mapResult = mapRepository.syncMaps(userId)

            when {
                activityResult.isFailure && mapResult.isFailure ->
                    Result.failure(Exception("Both syncs failed"))

                activityResult.isFailure ->
                    Result.failure(Exception("Activity sync failed: ${activityResult.exceptionOrNull()?.message}"))

                mapResult.isFailure ->
                    Result.failure(Exception("Map sync failed: ${mapResult.exceptionOrNull()?.message}"))

                else -> Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
