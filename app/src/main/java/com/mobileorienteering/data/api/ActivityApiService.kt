package com.mobileorienteering.data.api

import com.mobileorienteering.data.model.ActivityResponse
import com.mobileorienteering.data.model.CreateActivityRequest
import com.mobileorienteering.data.model.CreateActivityResponse
import retrofit2.Response
import retrofit2.http.*

interface ActivityApiService {
    @POST("api/mobile/activities")
    suspend fun createActivity(@Body request: CreateActivityRequest): Response<CreateActivityResponse>

    @GET("api/mobile/activities/{id}")
    suspend fun getActivityById(@Path("id") id: Long): Response<ActivityResponse>

    @DELETE("api/mobile/activities/{id}")
    suspend fun deleteActivity(@Path("id") id: Long): Response<Unit>

    @GET("api/mobile/activities/user/{userId}")
    suspend fun getActivitiesByUserId(@Path("userId") userId: Long): Response<List<ActivityResponse>>

    @GET("api/mobile/activities/map/{mapId}")
    suspend fun getActivitiesByMapId(@Path("mapId") mapId: Long): Response<List<ActivityResponse>>
}
