package com.mobileorienteering.di

import android.content.Context
import androidx.room.Room
import com.mobileorienteering.BuildConfig
import com.mobileorienteering.data.local.AppDatabase
import com.mobileorienteering.data.local.addAllMigrations
import com.mobileorienteering.data.local.dao.ControlPointDao
import com.mobileorienteering.data.local.dao.RunDao
import com.mobileorienteering.data.local.dao.MapDao
import com.mobileorienteering.data.local.dao.PathPointDao
import com.mobileorienteering.data.local.dao.RunSettingsDao
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
        val builder = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DATABASE_NAME
        ).addAllMigrations()

        if (BuildConfig.DEBUG) {
            builder.fallbackToDestructiveMigration()
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideMapDao(database: AppDatabase): MapDao {
        return database.mapDao()
    }

    @Provides
    @Singleton
    fun provideControlPointDao(database: AppDatabase): ControlPointDao {
        return database.controlPointDao()
    }

    @Provides
    @Singleton
    fun provideRunDao(database: AppDatabase): RunDao {
        return database.runDao()
    }

    @Provides
    @Singleton
    fun provideRunSettingsDao(database: AppDatabase): RunSettingsDao {
        return database.runSettingsDao()
    }

    @Provides
    @Singleton
    fun providePathPointDao(database: AppDatabase): PathPointDao {
        return database.pathPointDao()
    }
}
