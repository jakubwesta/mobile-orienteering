package com.mobileorienteering.data.repository

import android.content.Context
import android.net.Uri
import com.mobileorienteering.data.api.ApiHelper
import com.mobileorienteering.data.api.service.MapApiService
import java.io.IOException
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
import com.mobileorienteering.util.toInstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.Instant
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class MapRepository @Inject constructor(
    private val mapApi: MapApiService,
    private val mapDao: MapDao,
    private val controlPointDao: ControlPointDao,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
    @Named("upload") private val uploadClient: OkHttpClient
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
        val auth = authRepository.getCurrentAuth()
            ?: return Result.failure(Exception("Not authenticated"))

        return try {
            val tempId = -(System.currentTimeMillis())

            mapDao.insertMap(
                MapEntity(
                    id = tempId,
                    userId = auth.userId,
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

            if (auth.isGuestMode) {
                return mapDao.getMapWithControlPoints(tempId)?.toDomainModel()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Map not found after save"))
            }

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

            val auth = authRepository.getCurrentAuth()
            if (auth?.isGuestMode == true) {
                return mapDao.getMapWithControlPoints(mapId)?.toDomainModel()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Map not found after update"))
            }

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
            val existing = mapDao.getMapById(mapId)

            if (mapId < 0 || auth?.isGuestMode == true) {
                deleteLocalImage(existing?.localImagePath)
                mapDao.deleteMap(mapId)
                return Result.success(Unit)
            }

            try {
                val response = mapApi.deleteMap(mapId)
                if (response.isSuccessful || response.code() == 404) {
                    deleteLocalImage(existing?.localImagePath)
                    mapDao.deleteMap(mapId)
                } else {
                    mapDao.markForDeletion(mapId)
                }
            } catch (_: IOException) {
                mapDao.markForDeletion(mapId)
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
            uploadPendingMapImages()
            downloadMapsFromServer()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadPendingDeletions() {
        mapDao.getMapsToDelete().forEach { entity ->
            if (entity.id > 0) {
                try {
                    val response = mapApi.deleteMap(entity.id)
                    if (response.isSuccessful || response.code() == 404) {
                        deleteLocalImage(entity.localImagePath)
                        mapDao.deleteMap(entity.id)
                    }
                } catch (_: IOException) {
                    // keep marked for deletion to retry next sync
                }
            } else {
                deleteLocalImage(entity.localImagePath)
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
                    val localImagePath = entity.localImagePath
                    val imageUploadPending = entity.imageUploadPending

                    mapDao.deleteMap(entity.id)
                    saveMapResponseLocally(response)

                    if (localImagePath != null) {
                        val migratedImagePath = migrateLocalImageFile(
                            oldMapId = entity.id,
                            newMapId = response.id,
                            localImagePath = localImagePath
                        )

                        if (migratedImagePath != null) {
                            mapDao.updateLocalImage(response.id, migratedImagePath, imageUploadPending)

                            if (imageUploadPending) {
                                val imageFile = File(migratedImagePath)
                                uploadRemoteMapImage(
                                    mapId = response.id,
                                    imageBytes = withContext(Dispatchers.IO) { imageFile.readBytes() },
                                    contentType = imageFile.contentType()
                                )
                            }
                        }
                    }
                }
        }
    }

    private suspend fun uploadPendingMapImages() {
        mapDao.getMapsWithPendingImageUploads()
            .filter { it.id > 0 && it.localImagePath != null }
            .forEach { entity ->
                val imageFile = File(entity.localImagePath!!)
                if (!imageFile.exists()) {
                    mapDao.updateLocalImage(entity.id, null, imageUploadPending = false)
                    return@forEach
                }

                uploadRemoteMapImage(
                    mapId = entity.id,
                    imageBytes = withContext(Dispatchers.IO) { imageFile.readBytes() },
                    contentType = imageFile.contentType()
                )
            }
    }

    private suspend fun downloadMapsFromServer(): Result<Unit> {
        return ApiHelper.safeApiCall("Failed to download maps") {
            mapApi.getMaps()
        }.map { serverMaps ->
            val serverIds = serverMaps.map { it.id }.toSet()

            mapDao.getAllMapsWithControlPoints().first()
                .filter { it.map.syncedWithServer && it.map.id !in serverIds }
                .forEach {
                    deleteLocalImage(it.map.localImagePath)
                    mapDao.deleteMap(it.map.id)
                }

            serverMaps.forEach { saveMapResponseLocally(it) }
        }
    }

    private suspend fun saveMapResponseLocally(response: MapResponse) {
        val existing = mapDao.getMapById(response.id)
        val keepPendingLocalImage =
            existing?.imageUploadPending == true &&
                existing.localImagePath != null &&
                response.imageUrl == null
        val keepLocalImage = existing?.localImagePath != null && (response.imageUrl != null || keepPendingLocalImage)

        if (existing?.localImagePath != null && !keepLocalImage) {
            deleteLocalImage(existing.localImagePath)
        }

        mapDao.insertMap(
            MapEntity(
                id = response.id,
                userId = response.userId,
                name = response.name,
                description = response.description,
                isSnapshot = response.isSnapshot,
                originalMapId = response.originalMapId,
                createdAt = response.createdAt.toInstant(),
                syncedWithServer = true,
                pendingDeletion = false,
                imageUrl = response.imageUrl ?: existing?.imageUrl.takeIf { keepPendingLocalImage },
                localImagePath = existing?.localImagePath.takeIf { keepLocalImage },
                imageUploadPending = keepPendingLocalImage
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

    suspend fun uploadMapImage(mapId: Long, imageBytes: ByteArray, contentType: String = "image/jpeg"): Result<Map> {
        return try {
            val localImagePath = saveMapImageLocally(mapId, imageBytes, contentType)
            mapDao.updateLocalImage(mapId, localImagePath, imageUploadPending = true)

            val auth = authRepository.getCurrentAuth()
            if (mapId < 0 || auth?.isGuestMode == true) {
                return mapDao.getMapWithControlPoints(mapId)?.toDomainModel()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Map not found after local image save"))
            }

            uploadRemoteMapImage(mapId, imageBytes, contentType).recoverCatching {
                mapDao.getMapWithControlPoints(mapId)?.toDomainModel()
                    ?: throw Exception("Map not found after local image save")
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to upload map image: ${e.message}"))
        }
    }

    suspend fun uploadMapImage(mapId: Long, imageUri: Uri): Result<Map> {
        return try {
            val contentType = context.contentResolver.getType(imageUri).toSupportedImageContentType()
            val imageBytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                    ?: throw IOException("Unable to read selected image")
            }

            uploadMapImage(mapId, imageBytes, contentType)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to upload map image: ${e.message}"))
        }
    }

    suspend fun deleteMapImage(mapId: Long): Result<Unit> {
        return try {
            val existing = mapDao.getMapById(mapId)

            if (mapId < 0 || authRepository.getCurrentAuth()?.isGuestMode == true) {
                deleteLocalImage(existing?.localImagePath)
                mapDao.updateLocalImage(mapId, null, imageUploadPending = false)
                mapDao.updateRemoteImage(mapId, null, imageUploadPending = false)
                return Result.success(Unit)
            }

            val response = mapApi.deleteImage(mapId)
            if (response.isSuccessful) {
                deleteLocalImage(existing?.localImagePath)
                mapDao.updateLocalImage(mapId, null, imageUploadPending = false)
                mapDao.updateRemoteImage(mapId, null, imageUploadPending = false)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete image (HTTP ${response.code()})"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error"))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to delete map image: ${e.message}"))
        }
    }

    private suspend fun uploadRemoteMapImage(
        mapId: Long,
        imageBytes: ByteArray,
        contentType: String
    ): Result<Map> {
        val uploadUrlResult = ApiHelper.safeApiCall("Failed to get upload URL") {
            mapApi.getImageUploadUrl(mapId, contentType)
        }

        val uploadUrlResponse = uploadUrlResult.getOrElse { return Result.failure(it) }

        val putResult = withContext(Dispatchers.IO) {
            val body = imageBytes.toRequestBody(contentType.toMediaType())
            val request = Request.Builder()
                .url(uploadUrlResponse.uploadUrl)
                .put(body)
                .header("Content-Type", contentType)
                .build()

            uploadClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to upload image to storage (HTTP ${response.code})"))
                }
            }
        }

        putResult.getOrElse { return Result.failure(it) }

        return ApiHelper.safeApiCall("Failed to confirm image upload") {
            mapApi.confirmImageUpload(mapId)
        }.onSuccess { response ->
            mapDao.updateRemoteImage(mapId, response.imageUrl, imageUploadPending = false)
        }.map { it.toDomainModel() }
    }

    private suspend fun saveMapImageLocally(
        mapId: Long,
        imageBytes: ByteArray,
        contentType: String
    ): String = withContext(Dispatchers.IO) {
        val imageFile = mapImageFile(mapId, contentType)
        imageFile.parentFile?.mkdirs()
        deleteOtherLocalImageFormats(mapId, imageFile)
        imageFile.writeBytes(imageBytes)
        imageFile.absolutePath
    }

    private fun mapImageFile(mapId: Long, contentType: String): File {
        val extension = when (contentType) {
            "image/png" -> "png"
            else -> "jpg"
        }
        return File(File(context.filesDir, "map_images"), "map_$mapId.$extension")
    }

    private fun deleteOtherLocalImageFormats(mapId: Long, keepFile: File) {
        listOf("jpg", "png").forEach { extension ->
            val file = File(File(context.filesDir, "map_images"), "map_$mapId.$extension")
            if (file.absolutePath != keepFile.absolutePath && file.exists()) {
                file.delete()
            }
        }
    }

    private suspend fun migrateLocalImageFile(
        oldMapId: Long,
        newMapId: Long,
        localImagePath: String
    ): String? = withContext(Dispatchers.IO) {
        val oldFile = File(localImagePath)
        if (!oldFile.exists()) return@withContext null
        if (oldMapId == newMapId) return@withContext oldFile.absolutePath

        val newFile = mapImageFile(newMapId, oldFile.contentType())
        newFile.parentFile?.mkdirs()
        deleteOtherLocalImageFormats(newMapId, newFile)

        if (oldFile.renameTo(newFile)) {
            newFile.absolutePath
        } else {
            oldFile.copyTo(newFile, overwrite = true)
            oldFile.delete()
            newFile.absolutePath
        }
    }

    private suspend fun deleteLocalImage(localImagePath: String?) {
        if (localImagePath == null) return
        withContext(Dispatchers.IO) {
            File(localImagePath).delete()
        }
    }

    private fun File.contentType(): String {
        return when (extension.lowercase()) {
            "png" -> "image/png"
            else -> "image/jpeg"
        }
    }

    private fun String?.toSupportedImageContentType(): String {
        return when (this) {
            "image/png" -> "image/png"
            else -> "image/jpeg"
        }
    }

    suspend fun clearLocalMaps() {
        withContext(Dispatchers.IO) {
            File(context.filesDir, "map_images").deleteRecursively()
        }
        mapDao.deleteAllMaps()
    }
}
