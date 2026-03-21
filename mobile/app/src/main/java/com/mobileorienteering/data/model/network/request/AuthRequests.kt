package com.mobileorienteering.data.model.network.request

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
    val email: String,
    val password: String,
    @param:Json(name = "full_name") val fullName: String? = null,
    @param:Json(name = "phone_number") val phoneNumber: String? = null
)

@JsonClass(generateAdapter = true)
data class GoogleLoginRequest(
    @param:Json(name = "id_token") val idToken: String
)

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    @param:Json(name = "refresh_token") val refreshToken: String
)
