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
data class UserResponse(
    val id: Long,
    val username: String,
    val fullName: String?,
    val email: String,
    val phoneNumber: String?,
    @field:Json(name = "private") val isPrivate: Boolean
)

@JsonClass(generateAdapter = true)
data class ApiError(
    val message: String?,
    val status: Int?
)

@JsonClass(generateAdapter = true)
data class ControlPointDto(
    val latitude: Double,
    val longitude: Double,
    val id: Long
)

@JsonClass(generateAdapter = true)
data class MapDataDto(
    val controlPoints: List<ControlPointDto>
)

@JsonClass(generateAdapter = true)
data class CreateMapRequest(
    val userId: Long,
    val name: String,
    val description: String,
    val location: String,
    val mapData: MapDataDto
)

@JsonClass(generateAdapter = true)
data class CreateMapResponse(
    val id: Long
)

@JsonClass(generateAdapter = true)
data class MapResponse(
    val id: Long,
    val userId: Long,
    val name: String,
    val description: String,
    val location: String,
    val mapData: MapDataDto,
    val createdAt: String
)

@JsonClass(generateAdapter = true)
data class PathPointDto(
    val latitude: Double,
    val longitude: Double,
    val timestamp: String
)

@JsonClass(generateAdapter = true)
data class CreateActivityRequest(
    val userId: Long,
    val mapId: Long,
    val title: String,
    val startTime: String,
    val duration: String,
    val distance: Double,
    val pathData: List<PathPointDto>
)

@JsonClass(generateAdapter = true)
data class CreateActivityResponse(
    val id: Long
)

@JsonClass(generateAdapter = true)
data class ActivityResponse(
    val id: Long,
    val userId: Long,
    val mapId: Long,
    val title: String,
    val startTime: String,
    val duration: String,
    val distance: Double,
    val pathData: List<PathPointDto>,
    val createdAt: String
)
