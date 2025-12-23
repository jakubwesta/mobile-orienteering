package com.mobileorienteering.data.api

import com.mobileorienteering.data.repository.AuthRepository
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

class TokenAuthenticator @Inject constructor(
    private val authRepository: Lazy<AuthRepository>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header("Authorization-Retry") != null) {
            return null
        }

        if (response.request.url.encodedPath.contains("/api/auth/")) {
            return null
        }

        val newToken = runBlocking {
            val result = authRepository.get().refreshToken()
            result.getOrNull()
        }

        if (newToken == null) {
            runBlocking {
                authRepository.get().logout()
            }
            return null
        }

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .header("Authorization-Retry", "true")
            .build()
    }
}
