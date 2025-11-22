package com.mobileorienteering.di

import android.content.Context
import com.mobileorienteering.data.repository.ActivityRepository
import com.mobileorienteering.data.repository.MapRepository
import com.mobileorienteering.data.sync.SyncManager
import com.mobileorienteering.util.ConnectivityMonitor
import com.mobileorienteering.util.LocationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {
    @Provides
    fun provideLocationManager(
        @ApplicationContext context: Context
    ): LocationManager = LocationManager(context)

    @Provides
    @Singleton
    fun provideConnectivityMonitor(
        @ApplicationContext context: Context
    ): ConnectivityMonitor = ConnectivityMonitor(context)

    @Provides
    @Singleton
    fun provideSyncManager(
        activityRepository: ActivityRepository,
        mapRepository: MapRepository
    ): SyncManager = SyncManager(activityRepository, mapRepository)
}
