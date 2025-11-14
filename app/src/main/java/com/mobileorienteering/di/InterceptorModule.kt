package com.mobileorienteering.di

import com.mobileorienteering.data.api.AuthInterceptor
import com.mobileorienteering.data.repository.AuthRepository
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
    fun provideAuthInterceptor(authRepository: AuthRepository): AuthInterceptor =
        AuthInterceptor(authRepository)
}
