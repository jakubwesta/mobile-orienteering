package com.mobileorienteering.data.repository

import retrofit2.Response
import java.io.IOException

object ApiHelper {

    suspend fun <T> safeApiCall(
        errorPrefix: String,
        apiCall: suspend () -> Response<T>
    ): Result<T> {
        return try {
            val response = apiCall()
            handleResponse(response, errorPrefix)
        } catch (e: IOException) {
            Result.failure(Exception("Network error. Please check your connection."))
        } catch (e: Exception) {
            Result.failure(Exception("$errorPrefix: ${e.message ?: "Unknown error"}"))
        }
    }

    fun <T> handleResponse(response: Response<T>, defaultMessage: String): Result<T> {
        return when {
            response.isSuccessful && response.body() != null -> {
                Result.success(response.body()!!)
            }

            response.isSuccessful && response.code() == 204 -> {
                @Suppress("UNCHECKED_CAST")
                Result.success(Unit as T)
            }

            response.isSuccessful -> {
                @Suppress("UNCHECKED_CAST")
                Result.success(Unit as T)
            }

            else -> {
                val errorMessage = extractErrorMessage(response, defaultMessage)
                Result.failure(Exception(errorMessage))
            }
        }
    }

    fun <T> extractErrorMessage(response: Response<T>, defaultMessage: String): String {
        return try {
            val errorBody = response.errorBody()?.string()
            when {
                errorBody != null && errorBody.isNotEmpty() -> {
                    if (errorBody.contains("message")) {
                        errorBody
                            .substringAfter("\"message\":")
                            .substringAfter("\"")
                            .substringBefore("\"")
                            .ifEmpty { defaultMessage }
                    } else {
                        errorBody.take(100)
                    }
                }
                response.code() == 401 -> "Unauthorized. Please log in again"
                response.code() == 403 -> "Access forbidden"
                response.code() == 404 -> "Resource not found"
                response.code() == 409 -> "Conflict. Resource already exists"
                response.code() == 422 -> "Invalid input. Please check your data"
                response.code() >= 500 -> "Server error. Please try again later"
                else -> "$defaultMessage (${response.code()})"
            }
        } catch (e: Exception) {
            "$defaultMessage (${response.code()})"
        }
    }
}
