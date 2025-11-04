package com.mobileorienteering.data.model

data class LoginModel(
    val email: String,
    val password: String
)

data class RegisterModel(
    val username: String,
    val email: String,
    val fullName: String? = null,
    val phoneNumber: String? = null,
    val password: String
)

data class AuthModel(
    val token: String,
    val userId: String
)
