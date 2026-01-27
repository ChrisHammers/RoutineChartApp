package com.HammersTech.RoutineChart.core.data.local.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.HammersTech.RoutineChart.core.utils.AppLogger

/**
 * Migration from version 5 to version 6
 * Phase 3.4: Upload Queue (RoutineSteps)
 * - Add synced column to routine_steps
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        AppLogger.Database.info("Starting migration 5->6: Adding synced column to routine_steps")

        // Add synced column to routine_steps
        // Note: We add it as nullable first, then update existing rows, then make it NOT NULL
        // This avoids Room's strict schema checking issues with DEFAULT values
        db.execSQL("ALTER TABLE routine_steps ADD COLUMN synced INTEGER")
        
        // Set synced = 0 (false) for all existing rows
        db.execSQL("UPDATE routine_steps SET synced = 0 WHERE synced IS NULL")
        
        // Now make it NOT NULL
        // SQLite doesn't support ALTER COLUMN, so we need to recreate the table
        db.execSQL("""
            CREATE TABLE routine_steps_new (
                id TEXT NOT NULL PRIMARY KEY,
                routineId TEXT NOT NULL,
                orderIndex INTEGER NOT NULL,
                label TEXT,
                iconName TEXT,
                audioCueUrl TEXT,
                createdAt INTEGER NOT NULL,
                deletedAt INTEGER,
                synced INTEGER NOT NULL,
                FOREIGN KEY(routineId) REFERENCES routines(id) ON DELETE CASCADE
            )
        """)
        
        // Copy data from old table
        db.execSQL("""
            INSERT INTO routine_steps_new (id, routineId, orderIndex, label, iconName, audioCueUrl, createdAt, deletedAt, synced)
            SELECT id, routineId, orderIndex, label, iconName, audioCueUrl, createdAt, deletedAt, COALESCE(synced, 0)
            FROM routine_steps
        """)
        
        // Drop old table
        db.execSQL("DROP TABLE routine_steps")
        
        // Rename new table
        db.execSQL("ALTER TABLE routine_steps_new RENAME TO routine_steps")
        
        // Recreate index on routineId (the only index declared in the entity)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_steps_routineId ON routine_steps(routineId)")

        AppLogger.Database.info("Migration 5->6 completed: Added synced column to routine_steps")
    }
}
