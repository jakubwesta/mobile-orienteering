package com.mobileorienteering.data.repository

import com.mobileorienteering.data.api.ActivityApiService
import com.mobileorienteering.data.model.ActivityResponse
import com.mobileorienteering.data.model.CreateActivityRequest
import com.mobileorienteering.data.model.CreateActivityResponse
import javax.inject.Inject

class ActivityRepository @Inject constructor(
    private val activityApi: ActivityApiService
) {

    suspend fun createActivity(request: CreateActivityRequest): Result<CreateActivityResponse> {
        return ApiHelper.safeApiCall("Failed to create activity") {
            activityApi.createActivity(request)
        }
    }

    suspend fun getActivityById(id: Long): Result<ActivityResponse> {
        return ApiHelper.safeApiCall("Failed to fetch activity") {
            activityApi.getActivityById(id)
        }
    }

    suspend fun deleteActivity(id: Long): Result<Unit> {
        return ApiHelper.safeApiCall("Failed to delete activity") {
            activityApi.deleteActivity(id)
        }
    }

    suspend fun getActivitiesByUserId(userId: Long): Result<List<ActivityResponse>> {
        return ApiHelper.safeApiCall("Failed to fetch user activities") {
            activityApi.getActivitiesByUserId(userId)
        }
    }

    suspend fun getActivitiesByMapId(mapId: Long): Result<List<ActivityResponse>> {
        return ApiHelper.safeApiCall("Failed to fetch map activities") {
            activityApi.getActivitiesByMapId(mapId)
        }
    }
}
