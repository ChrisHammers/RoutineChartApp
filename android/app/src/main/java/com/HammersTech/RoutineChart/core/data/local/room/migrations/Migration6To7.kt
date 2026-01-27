package com.HammersTech.RoutineChart.core.data.local.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.HammersTech.RoutineChart.core.utils.AppLogger

/**
 * Migration from version 6 to version 7
 * Phase 3.3: Remove foreign key constraint on routines.familyId
 * - Remove foreign key constraint to allow routines to reference families that don't exist locally yet
 * - This is necessary for pulling routines from Firestore that belong to families not yet synced
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        AppLogger.Database.info("Starting migration 6->7: Removing foreign key constraint on routines.familyId")

        // Recreate routines table without the foreign key constraint
        // SQLite doesn't support dropping foreign keys, so we need to recreate the table
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
                synced INTEGER NOT NULL
            )
        """)
        
        // Copy data from old table
        db.execSQL("""
            INSERT INTO routines_new (id, userId, familyId, title, iconName, version, completionRule, createdAt, updatedAt, deletedAt, synced)
            SELECT id, userId, familyId, title, iconName, version, completionRule, createdAt, updatedAt, deletedAt, synced
            FROM routines
        """)
        
        // Drop old table
        db.execSQL("DROP TABLE routines")
        
        // Rename new table
        db.execSQL("ALTER TABLE routines_new RENAME TO routines")
        
        // Recreate indexes (userId, familyId, synced)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routines_userId ON routines(userId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routines_familyId ON routines(familyId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routines_synced ON routines(synced)")

        AppLogger.Database.info("Migration 6->7 completed: Removed foreign key constraint on routines.familyId")
    }
}
