package com.mobileorienteering.data.model.domain

data class LoginModel(
    val username: String,
    val password: String
)

data class RegisterModel(
    val username: String,
    val email: String,
    val fullName: String? = null,
    val phoneNumber: String? = null,
    val password: String,
    val isPrivate: Boolean = false
)

data class AuthModel(
    val userId: Long,
    val username: String,
    val token: String,
    val refreshToken: String,
    val isGoogleLogin: Boolean = false
)
