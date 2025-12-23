package com.mobileorienteering.di

import android.content.Context
import androidx.room.Room
import com.mobileorienteering.data.local.AppDatabase
import com.mobileorienteering.data.local.dao.ActivityDao
import com.mobileorienteering.data.local.dao.MapDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private const val DATABASE_NAME = "mobile_orienteering_db"

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideActivityDao(database: AppDatabase): ActivityDao {
        return database.activityDao()
    }

    @Provides
    @Singleton
    fun provideMapDao(database: AppDatabase): MapDao {
        return database.mapDao()
    }
}