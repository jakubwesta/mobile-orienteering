package com.mobileorienteering.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mobileorienteering.data.local.converter.Converters
import com.mobileorienteering.data.local.dao.*
import com.mobileorienteering.data.local.entity.*

@Database(
    entities = [
        MapEntity::class,
        ControlPointEntity::class,
        RunEntity::class,
        RunSettingsEntity::class,
        PathPointEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mapDao(): MapDao
    abstract fun controlPointDao(): ControlPointDao
    abstract fun runDao(): RunDao
    abstract fun runSettingsDao(): RunSettingsDao
    abstract fun pathPointDao(): PathPointDao
}
