package com.mobileorienteering.data.api

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
        } catch (_: IOException) {
            Result.failure(Exception("Network error."))
        } catch (e: Exception) {
            Result.failure(Exception("$errorPrefix: ${e.message ?: "Unknown error"}"))
        }
    }

    private fun <T> handleResponse(response: Response<T>, defaultMessage: String): Result<T> {
        return when {
            response.isSuccessful && response.body() != null -> {
                Result.success(response.body()!!)
            }

            response.isSuccessful -> {
                @Suppress("UNCHECKED_CAST")
                Result.success(Unit as T)
            }

            else -> {
                Result.failure(Exception("$defaultMessage (HTTP ${response.code()})"))
            }
        }
    }
}
