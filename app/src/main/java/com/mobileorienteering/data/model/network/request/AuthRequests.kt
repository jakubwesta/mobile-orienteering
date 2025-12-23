package com.mobileorienteering.data.model.network.request

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
    @field:com.squareup.moshi.Json(name = "private") val private: Boolean = false
)

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class GoogleLoginRequest(
    val idToken: String
)
