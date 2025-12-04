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
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun mapDao(): MapDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE activities ADD COLUMN status TEXT NOT NULL DEFAULT 'COMPLETED'")
                db.execSQL("ALTER TABLE activities ADD COLUMN visitedCheckpoints TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE activities ADD COLUMN totalCheckpoints INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}