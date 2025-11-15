package com.mobileorienteering.di

import com.mobileorienteering.data.api.AuthInterceptor
import com.mobileorienteering.data.api.TokenAuthenticator
import com.mobileorienteering.data.repository.AuthRepository
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InterceptorModule {
    @Provides
    @Singleton
    fun provideAuthInterceptor(
        authRepository: Lazy<AuthRepository>
    ): AuthInterceptor = AuthInterceptor(authRepository)

    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        authRepository: Lazy<AuthRepository>
    ): TokenAuthenticator = TokenAuthenticator(authRepository)
}
