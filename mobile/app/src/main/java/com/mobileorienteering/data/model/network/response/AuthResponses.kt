package com.mobileorienteering.data.model.network.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TokenResponse(
    @param:Json(name = "access_token") val accessToken: String,
    @param:Json(name = "refresh_token") val refreshToken: String,
    @param:Json(name = "token_type") val tokenType: String
)
