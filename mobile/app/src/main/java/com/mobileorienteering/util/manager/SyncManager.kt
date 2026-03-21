package com.mobileorienteering.util.manager

import com.mobileorienteering.data.repository.AuthRepository
import com.mobileorienteering.data.repository.MapRepository
import com.mobileorienteering.data.repository.RunRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val mapRepository: MapRepository,
    private val runRepository: RunRepository,
    private val authRepository: AuthRepository,
    private val connectivityManager: ConnectivityManager
) {
    suspend fun syncAll(): Result<Unit> = withContext(Dispatchers.IO) {
        if (authRepository.getCurrentAuth()?.isGuestMode == true) {
            return@withContext Result.success(Unit)
        }

        if (!connectivityManager.isCurrentlyOnline()) {
            return@withContext Result.failure(Exception("No network connection"))
        }

        // Maps must sync before runs — uploadUnsyncedRuns needs originalMapId > 0
        val mapResult = mapRepository.syncMaps()
        val runResult = runRepository.syncRuns()

        val result = when {
            mapResult.isFailure && runResult.isFailure ->
                Result.failure(Exception("Sync failed"))
            mapResult.isFailure ->
                Result.failure(Exception("Map sync failed: ${mapResult.exceptionOrNull()?.message}"))
            runResult.isFailure ->
                Result.failure(Exception("Run sync failed: ${runResult.exceptionOrNull()?.message}"))
            else -> Result.success(Unit)
        }

        result
    }
}
