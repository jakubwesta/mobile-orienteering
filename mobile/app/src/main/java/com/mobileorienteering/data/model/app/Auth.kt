package com.mobileorienteering.data.model.app

data class AuthModel(
    val userId: Long,
    val username: String,
    val token: String,
    val refreshToken: String,
    val isExternalLogin: Boolean = false,
    val isGuestMode: Boolean = false
)
