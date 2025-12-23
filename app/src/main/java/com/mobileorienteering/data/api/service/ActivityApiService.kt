package com.mobileorienteering.data.api.service

import com.mobileorienteering.data.model.network.response.ActivityResponse
import com.mobileorienteering.data.model.network.request.CreateActivityRequest
import com.mobileorienteering.data.model.network.response.CreateActivityResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ActivityApiService {
    @POST("api/mobile/activities")
    suspend fun createActivity(@Body request: CreateActivityRequest): Response<CreateActivityResponse>

    @DELETE("api/mobile/activities/{id}")
    suspend fun deleteActivity(@Path("id") id: Long): Response<Unit>

    @GET("api/mobile/activities/user/{userId}")
    suspend fun getActivitiesByUserId(@Path("userId") userId: Long): Response<List<ActivityResponse>>
}
