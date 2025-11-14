package com.mobileorienteering.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val username: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val username: String,
    val fullName: String?,
    val email: String,
    val phoneNumber: String?,
    val password: String,
    @field:Json(name = "private") val private: Boolean = false
)

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class GoogleLoginRequest(
    val idToken: String
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val token: String,
    val username: String,
    val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class ApiError(
    val message: String?,
    val status: Int?
)
