package com.mobileorienteering.data.api.service

import com.mobileorienteering.data.model.network.response.AuthResponse
import com.mobileorienteering.data.model.network.request.GoogleLoginRequest
import com.mobileorienteering.data.model.network.request.LoginRequest
import com.mobileorienteering.data.model.network.request.RefreshTokenRequest
import com.mobileorienteering.data.model.network.response.RefreshTokenResponse
import com.mobileorienteering.data.model.network.request.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("api/mobile/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/mobile/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/mobile/auth/login/google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): Response<AuthResponse>

    @POST("api/mobile/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<RefreshTokenResponse>
}
