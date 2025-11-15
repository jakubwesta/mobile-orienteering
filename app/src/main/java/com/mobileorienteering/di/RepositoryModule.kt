package com.mobileorienteering.di

import android.content.Context
import com.mobileorienteering.data.api.AuthApiService
import com.mobileorienteering.data.api.MapApiService
import com.mobileorienteering.data.repository.AuthRepository
import com.mobileorienteering.data.repository.FirstLaunchRepository
import com.mobileorienteering.data.repository.MapRepository
import com.mobileorienteering.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
        SettingsRepository(context)

    @Provides
    @Singleton
    fun provideFirstLaunchRepository(@ApplicationContext context: Context): FirstLaunchRepository =
        FirstLaunchRepository(context)

    @Provides
    @Singleton
    fun provideAuthRepository(
        @ApplicationContext context: Context,
        authApi: AuthApiService
    ): AuthRepository =
        AuthRepository(context, authApi)

    @Provides
    @Singleton
    fun provideMapRepository(
        mapApi: MapApiService
    ): MapRepository =
        MapRepository(mapApi)
}
