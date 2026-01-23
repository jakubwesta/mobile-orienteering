package com.mobileorienteering.data.api.service

import com.mobileorienteering.data.model.network.request.CreateMapRequest
import com.mobileorienteering.data.model.network.response.CreateMapResponse
import com.mobileorienteering.data.model.network.response.MapResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface MapApiService {
    @POST("api/mobile/maps")
    suspend fun createMap(@Body request: CreateMapRequest): Response<CreateMapResponse>

    @PUT("api/mobile/maps/{id}")
    suspend fun updateMap(@Path("id") id: Long, @Body request: CreateMapRequest): Response<MapResponse>

    @DELETE("api/mobile/maps/{id}")
    suspend fun deleteMap(@Path("id") id: Long): Response<Unit>

    @GET("api/mobile/maps/user/{userId}")
    suspend fun getMapsByUserId(@Path("userId") userId: Long): Response<List<MapResponse>>
}
