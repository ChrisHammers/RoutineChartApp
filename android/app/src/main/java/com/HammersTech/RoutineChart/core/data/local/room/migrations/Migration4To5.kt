package com.HammersTech.RoutineChart.core.data.local.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.HammersTech.RoutineChart.core.utils.AppLogger

/**
 * Migration from version 4 to version 5
 * Phase 3.2: Upload Queue (Routines)
 * - Add userId column to routines (required)
 * - Make familyId nullable in routines
 * - Add synced column to routines
 * - Remove familyId column from routine_steps
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        AppLogger.Database.info("Starting migration 4->5: Updating routines and routine_steps for Phase 3.2")

        // Step 1: Recreate routines table with new schema
        // SQLite doesn't support changing nullability or adding NOT NULL columns directly
        // So we need to recreate the table
        
        // First, backfill userId for existing routines
        db.execSQL("ALTER TABLE routines ADD COLUMN userId_temp TEXT")
        db.execSQL("""
            UPDATE routines 
            SET userId_temp = (
                SELECT u.id 
                FROM users u 
                WHERE u.familyId = routines.familyId 
                AND u.role = 'parent' 
                LIMIT 1
            )
            WHERE userId_temp IS NULL 
            AND familyId IS NOT NULL
        """)
        // For routines that still don't have a userId, set to migration marker
        db.execSQL("UPDATE routines SET userId_temp = '__MIGRATION_NEEDED__' WHERE userId_temp IS NULL")
        
        // Create new routines table with correct schema
        db.execSQL("""
            CREATE TABLE routines_new (
                id TEXT NOT NULL PRIMARY KEY,
                userId TEXT NOT NULL,
                familyId TEXT,
                title TEXT NOT NULL,
                iconName TEXT,
                version INTEGER NOT NULL,
                completionRule TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                deletedAt INTEGER,
                synced INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(familyId) REFERENCES families(id) ON DELETE CASCADE
            )
        """)
        
        // Copy data from old table to new table
        db.execSQL("""
            INSERT INTO routines_new (id, userId, familyId, title, iconName, version, completionRule, createdAt, updatedAt, deletedAt, synced)
            SELECT id, userId_temp, familyId, title, iconName, version, completionRule, createdAt, updatedAt, deletedAt, 0
            FROM routines
        """)
        
        // Drop old table
        db.execSQL("DROP TABLE routines")
        
        // Rename new table
        db.execSQL("ALTER TABLE routines_new RENAME TO routines")
        
        // Create indexes (matching Room entity definition)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routines_userId ON routines(userId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routines_familyId ON routines(familyId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routines_synced ON routines(synced)")

        // Step 4: Remove familyId from routine_steps
        // SQLite doesn't support DROP COLUMN, so we need to recreate the table
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
                FOREIGN KEY(routineId) REFERENCES routines(id) ON DELETE CASCADE
            )
        """)
        
        // Copy data (excluding familyId)
        db.execSQL("""
            INSERT INTO routine_steps_new (id, routineId, orderIndex, label, iconName, audioCueUrl, createdAt, deletedAt)
            SELECT id, routineId, orderIndex, label, iconName, audioCueUrl, createdAt, deletedAt
            FROM routine_steps
        """)
        
        // Drop old table
        db.execSQL("DROP TABLE routine_steps")
        
        // Rename new table
        db.execSQL("ALTER TABLE routine_steps_new RENAME TO routine_steps")
        
        // Recreate index on routineId
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_steps_routineId ON routine_steps(routineId)")

        AppLogger.Database.info("Migration 4->5 completed: Updated routines and routine_steps for Phase 3.2")
    }
}
