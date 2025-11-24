package com.mobileorienteering.di

import android.content.Context
import com.mobileorienteering.data.api.ActivityApiService
import com.mobileorienteering.data.api.AuthApiService
import com.mobileorienteering.data.api.MapApiService
import com.mobileorienteering.data.api.UserApiService
import com.mobileorienteering.data.local.dao.ActivityDao
import com.mobileorienteering.data.local.dao.MapDao
import com.mobileorienteering.data.repository.ActivityRepository
import com.mobileorienteering.data.repository.AuthRepository
import com.mobileorienteering.data.repository.FirstLaunchRepository
import com.mobileorienteering.data.repository.MapRepository
import com.mobileorienteering.data.repository.SettingsRepository
import com.mobileorienteering.data.repository.UserRepository
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
    fun provideSettingsRepository
                (@ApplicationContext context: Context
    ): SettingsRepository =
        SettingsRepository(context)

    @Provides
    @Singleton
    fun provideFirstLaunchRepository(
        @ApplicationContext context: Context
    ): FirstLaunchRepository =
        FirstLaunchRepository(context)

    @Provides
    @Singleton
    fun provideAuthRepository(
        @ApplicationContext context: Context,
        authApi: AuthApiService,
        userApi: UserApiService
    ): AuthRepository =
        AuthRepository(context, authApi, userApi)

    @Provides
    @Singleton
    fun provideUserRepository(
        userApi: UserApiService
    ): UserRepository =
        UserRepository(userApi)

    @Provides
    @Singleton
    fun provideMapRepository(
        mapApi: MapApiService,
        mapDao: MapDao
    ): MapRepository =
        MapRepository(mapApi, mapDao)

    @Provides
    @Singleton
    fun provideActivityRepository(
        activityApi: ActivityApiService,
        activityDao: ActivityDao
    ): ActivityRepository =
        ActivityRepository(activityApi, activityDao)
}
