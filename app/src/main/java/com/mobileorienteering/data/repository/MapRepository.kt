package com.mobileorienteering.data.repository

import com.mobileorienteering.data.api.MapApiService
import com.mobileorienteering.data.model.*
import javax.inject.Inject

class MapRepository @Inject constructor(
    private val mapApi: MapApiService
) {

    suspend fun createMap(request: CreateMapRequest): Result<CreateMapResponse> {
        return ApiHelper.safeApiCall("Failed to create map") {
            mapApi.createMap(request)
        }
    }

    suspend fun getMapById(id: Long): Result<MapResponse> {
        return ApiHelper.safeApiCall("Failed to fetch map") {
            mapApi.getMapById(id)
        }
    }

    suspend fun deleteMap(id: Long): Result<Unit> {
        return ApiHelper.safeApiCall("Failed to delete map") {
            mapApi.deleteMap(id)
        }
    }

    suspend fun getMapsByUserId(userId: Long): Result<List<MapResponse>> {
        return ApiHelper.safeApiCall("Failed to fetch user maps") {
            mapApi.getMapsByUserId(userId)
        }
    }
}
