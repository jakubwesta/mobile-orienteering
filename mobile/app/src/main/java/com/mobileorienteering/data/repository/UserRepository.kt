package com.mobileorienteering.data.repository

import com.mobileorienteering.data.api.ApiHelper
import com.mobileorienteering.data.api.service.UserApiService
import com.mobileorienteering.data.model.domain.User
import com.mobileorienteering.data.model.network.request.UpdateUserRequest
import com.mobileorienteering.data.model.network.response.toDomainModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userApi: UserApiService
) {

    suspend fun getCurrentUser(): Result<User> {
        return ApiHelper.safeApiCall("Failed to fetch user") {
            userApi.getCurrentUser()
        }.map { it.toDomainModel() }
    }

    suspend fun updateProfile(
        username: String? = null,
        fullName: String? = null,
        email: String? = null,
        phoneNumber: String? = null
    ): Result<User> {
        return ApiHelper.safeApiCall("Failed to update profile") {
            userApi.updateUser(
                UpdateUserRequest(
                    username = username,
                    fullName = fullName,
                    email = email,
                    phoneNumber = phoneNumber
                )
            )
        }.map { it.toDomainModel() }
    }

    suspend fun changePassword(
        oldPassword: String,
        newPassword: String
    ): Result<User> {
        return ApiHelper.safeApiCall("Failed to change password") {
            userApi.updateUser(
                UpdateUserRequest(
                    oldPassword = oldPassword,
                    newPassword = newPassword
                )
            )
        }.map { it.toDomainModel() }
    }
}
