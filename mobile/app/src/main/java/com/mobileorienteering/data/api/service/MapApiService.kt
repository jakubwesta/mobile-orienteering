package com.mobileorienteering.data.api.service

import com.mobileorienteering.data.model.network.request.CreateMapRequest
import com.mobileorienteering.data.model.network.request.UpdateMapRequest
import com.mobileorienteering.data.model.network.response.MapResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface MapApiService {
    @POST("api/maps/")
    suspend fun createMap(@Body request: CreateMapRequest): Response<MapResponse>

    @PUT("api/maps/{id}")
    suspend fun updateMap(@Path("id") id: Long, @Body request: UpdateMapRequest): Response<MapResponse>

    @DELETE("api/maps/{id}")
    suspend fun deleteMap(@Path("id") id: Long): Response<Unit>

    @GET("api/maps/")
    suspend fun getMaps(): Response<List<MapResponse>>
}
