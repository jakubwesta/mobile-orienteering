package com.mobileorienteering.data.model.domain

data class User(
    val id: Long,
    val username: String,
    val email: String,
    val fullName: String?,
    val phoneNumber: String?
)
