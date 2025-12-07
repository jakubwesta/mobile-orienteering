package com.mobileorienteering.data.api

import com.mobileorienteering.data.model.UpdateUserRequest
import com.mobileorienteering.data.model.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface UserApiService {
    @GET("api/mobile/users/me")
    suspend fun getCurrentUser(): Response<UserResponse>

    @PATCH("api/mobile/users/{id}")
    suspend fun updateUser(
        @Path("id") userId: Long,
        @Body request: UpdateUserRequest
    ): Response<UserResponse>
}
