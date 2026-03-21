package com.mobileorienteering.data.model.network.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdateUserRequest(
    val username: String? = null,
    val email: String? = null,
    @param:Json(name = "full_name") val fullName: String? = null,
    @param:Json(name = "phone_number") val phoneNumber: String? = null,
    @param:Json(name = "new_password") val newPassword: String? = null,
    @param:Json(name = "old_password") val oldPassword: String? = null
)
