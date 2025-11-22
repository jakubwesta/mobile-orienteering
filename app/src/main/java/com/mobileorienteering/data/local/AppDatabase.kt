package com.mobileorienteering.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mobileorienteering.data.local.converter.Converters
import com.mobileorienteering.data.local.dao.ActivityDao
import com.mobileorienteering.data.local.dao.MapDao
import com.mobileorienteering.data.local.entity.ActivityEntity
import com.mobileorienteering.data.local.entity.MapEntity

@Database(
    entities = [ActivityEntity::class, MapEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun mapDao(): MapDao
}
