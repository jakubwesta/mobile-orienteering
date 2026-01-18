package com.mobileorienteering.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mobileorienteering.data.local.converter.Converters
import com.mobileorienteering.data.local.dao.ActivityDao
import com.mobileorienteering.data.local.dao.MapDao
import com.mobileorienteering.data.local.entity.ActivityEntity
import com.mobileorienteering.data.local.entity.MapEntity

@Database(
    entities = [ActivityEntity::class, MapEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun mapDao(): MapDao

    companion object {
        /**
         * Migration from version 1 to 2: Added 'name' field to ControlPoint
         * Since controlPoints is stored as JSON text, no SQL schema changes are needed.
         * The new 'name' field will be automatically handled by JSON serialization.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // No SQL changes needed - controlPoints column stores JSON
                // The Moshi JSON converter will handle the new 'name' field automatically
            }
        }
    }
}
