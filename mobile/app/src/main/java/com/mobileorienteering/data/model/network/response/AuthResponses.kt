package com.mobileorienteering.data.model.network.response

import com.squareup.moshi.JsonClass

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
