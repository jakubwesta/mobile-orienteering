package com.mobileorienteering.di

import android.content.Context
import com.mobileorienteering.util.LocationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {
    @Provides
    fun provideLocationManager(@ApplicationContext context: Context): LocationManager =
        LocationManager(context)
}
