package com.HammersTech.RoutineChart.core.data.local.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.HammersTech.RoutineChart.core.utils.AppLogger

/**
 * Migration from version 3 to version 4
 * Adds sync_cursors table for tracking last sync timestamp per collection
 * Phase 3.1: Sync Infrastructure
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        AppLogger.Database.info("Starting migration 3->4: Adding sync_cursors table")
        
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS sync_cursors (
                id TEXT PRIMARY KEY NOT NULL,
                collection TEXT NOT NULL UNIQUE,
                lastSyncedAt INTEGER NOT NULL
            )
        """)
        
        // Note: lastSyncedAt is stored as INTEGER (Unix timestamp in milliseconds)
        // Room's TypeConverter will handle Instant <-> Long conversion
        // Note: UNIQUE constraint on collection automatically creates an index
        
        AppLogger.Database.info("Migration 3->4 completed: Added sync_cursors table")
    }
}
