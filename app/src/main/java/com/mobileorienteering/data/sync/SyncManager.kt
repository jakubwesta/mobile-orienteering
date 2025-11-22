package com.mobileorienteering.data.sync

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
    suspend fun syncAllPendingData() = withContext(Dispatchers.IO) {
        try {
            activityRepository.getUnsyncedActivities().forEach { activity ->
                activityRepository.syncActivityWithServer(activity)
            }
        } catch (_: Exception) { }

        try {
            mapRepository.getUnsyncedMaps().forEach { map ->
                mapRepository.syncMapWithServer(map)
            }
        } catch (_: Exception) { }
    }

    suspend fun hasPendingSync(): Boolean = withContext(Dispatchers.IO) {
        val unsyncedActivities = activityRepository.getUnsyncedActivities()
        val unsyncedMaps = mapRepository.getUnsyncedMaps()
        unsyncedActivities.isNotEmpty() || unsyncedMaps.isNotEmpty()
    }
}
