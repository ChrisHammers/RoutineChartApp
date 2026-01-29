package com.HammersTech.RoutineChart.core.data.local.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.HammersTech.RoutineChart.core.utils.AppLogger

/**
 * Migration from version 8 to version 9
 * Fixes routine_assignments schema: ensures updatedAt is NOT NULL (Room validation failed after 7->8).
 * For users who already ran the previous 7->8 migration that left updatedAt nullable.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        AppLogger.Database.info("Starting migration 8->9: Fix routine_assignments updatedAt NOT NULL")

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

        db.execSQL("""
            INSERT INTO routine_assignments_new (id, familyId, routineId, childId, isActive, assignedAt, updatedAt, deletedAt, synced)
            SELECT id, familyId, routineId, childId, isActive, assignedAt, COALESCE(updatedAt, assignedAt), deletedAt, COALESCE(synced, 0)
            FROM routine_assignments
        """.trimIndent())

        db.execSQL("DROP TABLE routine_assignments")
        db.execSQL("ALTER TABLE routine_assignments_new RENAME TO routine_assignments")

        db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_assignments_familyId ON routine_assignments(familyId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_assignments_routineId ON routine_assignments(routineId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_assignments_childId ON routine_assignments(childId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_assignments_synced ON routine_assignments(synced)")

        AppLogger.Database.info("Migration 8->9 completed: routine_assignments updatedAt NOT NULL")
    }
}
