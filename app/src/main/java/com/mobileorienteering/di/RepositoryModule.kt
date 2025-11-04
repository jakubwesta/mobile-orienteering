package com.mobileorienteering.di

import android.content.Context
import com.mobileorienteering.data.repository.FirstLaunchRepository
import com.mobileorienteering.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
        SettingsRepository(context)

    @Provides
    fun provideFirstLaunchRepository(@ApplicationContext context: Context): FirstLaunchRepository =
        FirstLaunchRepository(context)
}
