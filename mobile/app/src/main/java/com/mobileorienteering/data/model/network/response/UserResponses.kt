package com.mobileorienteering.data.model.network.response

import com.mobileorienteering.data.model.domain.User
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    @param:Json(name = "full_name") val fullName: String?,
    @param:Json(name = "phone_number") val phoneNumber: String?,
    @param:Json(name = "created_at") val createdAt: String
)

fun UserResponse.toDomainModel(): User {
    return User(
        id = id,
        username = username,
        email = email,
        fullName = fullName,
        phoneNumber = phoneNumber
    )
}
