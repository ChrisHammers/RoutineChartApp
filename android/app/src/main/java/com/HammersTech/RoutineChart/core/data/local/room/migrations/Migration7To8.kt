package com.HammersTech.RoutineChart.core.data.local.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.HammersTech.RoutineChart.core.utils.AppLogger

/**
 * Migration from version 7 to version 8
 * Phase 3.5: Sync RoutineAssignments - add synced and updatedAt to routine_assignments
 * Recreates the table so updatedAt is NOT NULL (Room schema validation requires it).
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        AppLogger.Database.info("Starting migration 7->8: Adding synced and updatedAt to routine_assignments")

        db.execSQL("""
            CREATE TABLE routine_assignments_new (
                id TEXT NOT NULL PRIMARY KEY,
                familyId TEXT NOT NULL,
                routineId TEXT NOT NULL,
                childId TEXT NOT NULL,
                isActive INTEGER NOT NULL,
                assignedAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                deletedAt INTEGER,
                synced INTEGER NOT NULL,
                FOREIGN KEY(familyId) REFERENCES families(id) ON DELETE CASCADE,
                FOREIGN KEY(routineId) REFERENCES routines(id) ON DELETE CASCADE,
                FOREIGN KEY(childId) REFERENCES child_profiles(id) ON DELETE CASCADE
            )
        """.trimIndent())

        // Copy data: v7 table has no synced/updatedAt columns; use assignedAt for updatedAt and 0 for synced
        db.execSQL("""
            INSERT INTO routine_assignments_new (id, familyId, routineId, childId, isActive, assignedAt, updatedAt, deletedAt, synced)
            SELECT id, familyId, routineId, childId, isActive, assignedAt, assignedAt, deletedAt, 0
            FROM routine_assignments
        """.trimIndent())

        db.execSQL("DROP TABLE routine_assignments")
        db.execSQL("ALTER TABLE routine_assignments_new RENAME TO routine_assignments")

        db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_assignments_familyId ON routine_assignments(familyId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_assignments_routineId ON routine_assignments(routineId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_assignments_childId ON routine_assignments(childId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_assignments_synced ON routine_assignments(synced)")

        AppLogger.Database.info("Migration 7->8 completed: Added synced and updatedAt to routine_assignments")
    }
}
