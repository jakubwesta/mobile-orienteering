package com.mobileorienteering.data.repository

import com.mobileorienteering.data.api.UserApiService
import com.mobileorienteering.data.api.util.ApiHelper
import com.mobileorienteering.data.model.UpdateUserRequest
import com.mobileorienteering.data.model.UserResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userApi: UserApiService
) {

    suspend fun getCurrentUser(): Result<UserResponse> {
        return ApiHelper.safeApiCall("Failed to fetch user details") {
            userApi.getCurrentUser()
        }
    }

    suspend fun updateProfile(
        userId: Long,
        username: String? = null,
        fullName: String? = null,
        email: String? = null,
        phoneNumber: String? = null,
        isPrivate: Boolean? = null
    ): Result<UserResponse> {
        return ApiHelper.safeApiCall("Failed to update profile") {
            userApi.updateUser(
                userId = userId,
                request = UpdateUserRequest(
                    username = username,
                    fullName = fullName,
                    email = email,
                    phoneNumber = phoneNumber,
                    isPrivate = isPrivate
                )
            )
        }
    }

    suspend fun changePassword(
        userId: Long,
        currentPassword: String,
        newPassword: String
    ): Result<UserResponse> {
        return ApiHelper.safeApiCall("Failed to change password") {
            userApi.updateUser(
                userId = userId,
                request = UpdateUserRequest(
                    currentPassword = currentPassword,
                    newPassword = newPassword
                )
            )
        }
    }
}
