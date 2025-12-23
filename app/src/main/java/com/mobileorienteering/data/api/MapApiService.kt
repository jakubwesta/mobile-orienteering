package com.mobileorienteering.data.api

import com.mobileorienteering.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface MapApiService {
    @POST("api/mobile/maps")
    suspend fun createMap(@Body request: CreateMapRequest): Response<CreateMapResponse>

    @DELETE("api/mobile/maps/{id}")
    suspend fun deleteMap(@Path("id") id: Long): Response<Unit>

    @GET("api/mobile/maps/user/{userId}")
    suspend fun getMapsByUserId(@Path("userId") userId: Long): Response<List<MapResponse>>
}
