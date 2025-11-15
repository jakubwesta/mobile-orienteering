package com.mobileorienteering.data.api

import com.mobileorienteering.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface MapApiService {
    @POST("api/maps")
    suspend fun createMap(@Body request: CreateMapRequest): Response<CreateMapResponse>

    @GET("api/maps/{id}")
    suspend fun getMapById(@Path("id") id: Long): Response<MapResponse>

    @DELETE("api/maps/{id}")
    suspend fun deleteMap(@Path("id") id: Long): Response<Unit>

    @GET("api/maps/user/{userId}")
    suspend fun getMapsByUserId(@Path("userId") userId: Long): Response<List<MapResponse>>
}
