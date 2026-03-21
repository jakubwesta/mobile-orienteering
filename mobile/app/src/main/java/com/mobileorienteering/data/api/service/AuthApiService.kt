package com.mobileorienteering.data.api.service

import com.mobileorienteering.data.model.network.request.GoogleLoginRequest
import com.mobileorienteering.data.model.network.request.LoginRequest
import com.mobileorienteering.data.model.network.request.RefreshTokenRequest
import com.mobileorienteering.data.model.network.request.RegisterRequest
import com.mobileorienteering.data.model.network.response.TokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<TokenResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @POST("api/auth/login/google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): Response<TokenResponse>

    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<TokenResponse>
}
