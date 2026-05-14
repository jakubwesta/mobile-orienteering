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
    },

    // Migration 2->3: Refactoring of whole database, destructive migration

    // Migration 3->4: Added run settings fields to run_settings table
    object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE run_settings ADD COLUMN showSelfOnMap INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE run_settings ADD COLUMN orderedControlPoints INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE run_settings ADD COLUMN timerStart TEXT NOT NULL DEFAULT 'race_start'")
            db.execSQL("ALTER TABLE run_settings ADD COLUMN raceStyle TEXT NOT NULL DEFAULT 'standard'")
            db.execSQL("ALTER TABLE run_settings ADD COLUMN orientationType TEXT NOT NULL DEFAULT 'foot'")
        }
    },

    // Migration 4->5: Added imageUrl to maps table for R2-hosted map image URL
    object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE maps ADD COLUMN imageUrl TEXT")
        }
    },

    // Migration 5->6: Added local map image path and pending upload state
    object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE maps ADD COLUMN localImagePath TEXT")
            db.execSQL("ALTER TABLE maps ADD COLUMN imageUploadPending INTEGER NOT NULL DEFAULT 0")
        }
    }
)

// Extension function that adds all migrations to the database builder
fun <T : RoomDatabase> RoomDatabase.Builder<T>.addAllMigrations(): RoomDatabase.Builder<T> {
    return addMigrations(*ALL_MIGRATIONS)
}
