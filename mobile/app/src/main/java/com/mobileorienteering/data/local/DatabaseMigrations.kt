package com.mobileorienteering.data.local

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * All database migrations for the app.
 * Add new migrations here when bumping database version.
 */
private val ALL_MIGRATIONS = arrayOf(
    // Migration 1->2: Added 'name' field to ControlPoint
    // Since controlPoints is stored as JSON text, no SQL schema changes are needed.
    // The new 'name' field will be automatically handled by JSON serialization.
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // No SQL changes needed - controlPoints column stores JSON
            // The Moshi JSON converter will handle the new 'name' field automatically
        }
    }
)

// Extension function that adds all migrations to the database builder
fun <T : RoomDatabase> RoomDatabase.Builder<T>.addAllMigrations(): RoomDatabase.Builder<T> {
    return addMigrations(*ALL_MIGRATIONS)
}
