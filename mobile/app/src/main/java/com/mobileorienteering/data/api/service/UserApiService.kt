package com.mobileorienteering.data.api.service

import com.mobileorienteering.data.model.network.request.UpdateUserRequest
import com.mobileorienteering.data.model.network.response.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface UserApiService {
    @GET("api/users/me")
    suspend fun getCurrentUser(): Response<UserResponse>

    @PATCH("api/users/me")
    suspend fun updateUser(@Body request: UpdateUserRequest): Response<UserResponse>
}
