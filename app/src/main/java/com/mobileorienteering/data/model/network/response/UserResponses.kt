package com.mobileorienteering.data.model.network.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserResponse(
    val id: Long,
    val username: String,
    val fullName: String?,
    val email: String,
    val phoneNumber: String?,
    @field:Json(name = "private") val isPrivate: Boolean
)
