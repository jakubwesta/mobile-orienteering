package com.mobileorienteering.data.api.service

import com.mobileorienteering.data.model.network.request.CreateRunRequest
import com.mobileorienteering.data.model.network.response.RunResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface RunApiService {
    @POST("api/runs/")
    suspend fun createRun(@Body request: CreateRunRequest): Response<RunResponse>

    @DELETE("api/runs/{id}")
    suspend fun deleteRun(@Path("id") id: Long): Response<Unit>

    @GET("api/runs/")
    suspend fun getRuns(): Response<List<RunResponse>>
}
