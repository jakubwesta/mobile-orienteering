package com.mobileorienteering.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Response
import java.io.IOException

object ApiHelper {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    suspend fun <T> safeApiCall(
        fallbackMessage: String? = null,
        apiCall: suspend () -> Response<T>
    ): Result<T> {
        return try {
            val response = apiCall()
            handleResponse(response)
        } catch (_: IOException) {
            Result.failure(Exception("Network error"))
        } catch (_: Exception) {
            Result.failure(Exception(fallbackMessage ?: "Unknown error"))
        }
    }

    private fun <T> handleResponse(response: Response<T>): Result<T> {
        return when {
            response.isSuccessful && response.body() != null -> {
                Result.success(response.body()!!)
            }

            response.isSuccessful -> {
                @Suppress("UNCHECKED_CAST")
                Result.success(Unit as T)
            }

            else -> {
                val errorMessage = parseErrorBody(response)
                    ?: "Request failed (HTTP ${response.code()})"
                Result.failure(Exception(errorMessage))
            }
        }
    }

    private fun <T> parseErrorBody(response: Response<T>): String? {
        return try {
            val errorBody = response.errorBody()?.string() ?: return null
            val adapter = moshi.adapter(ApiErrorResponse::class.java)
            adapter.fromJson(errorBody)?.error?.message
        } catch (_: Exception) {
            null
        }
    }
}

@JsonClass(generateAdapter = true)
data class ApiErrorResponse(
    val error: ApiErrorDetail
)

@JsonClass(generateAdapter = true)
data class ApiErrorDetail(
    val message: String,
    @param:Json(name = "status_code") val statusCode: Int
)
