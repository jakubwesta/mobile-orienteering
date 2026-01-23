package com.mobileorienteering.data.model.network.request

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdateUserRequest(
    val username: String? = null,
    val fullName: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val isPrivate: Boolean? = null,
    val currentPassword: String? = null,
    val newPassword: String? = null
)
