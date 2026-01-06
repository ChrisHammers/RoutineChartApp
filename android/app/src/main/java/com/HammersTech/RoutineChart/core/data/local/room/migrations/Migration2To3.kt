package com.HammersTech.RoutineChart.core.data.local.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.HammersTech.RoutineChart.core.utils.AppLogger
import com.HammersTech.RoutineChart.core.utils.InviteCodeGenerator

/**
 * Migration from version 2 to version 3
 * Adds inviteCode column to family_invites table
 * Phase 2.2: QR Family Joining (Shareable Codes)
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        AppLogger.Database.info("Starting migration 2->3: Adding inviteCode column")
        
        // Check if column already exists
        val cursor = database.query("PRAGMA table_info(family_invites)")
        var columnExists = false
        while (cursor.moveToNext()) {
            val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            if (columnName == "inviteCode") {
                columnExists = true
                break
            }
        }
        cursor.close()
        
        if (!columnExists) {
            // Add inviteCode column
            database.execSQL("ALTER TABLE family_invites ADD COLUMN inviteCode TEXT NOT NULL DEFAULT ''")
            
            // Generate invite codes for existing rows
            val existingInvites = database.query("SELECT id FROM family_invites")
            while (existingInvites.moveToNext()) {
                val id = existingInvites.getString(existingInvites.getColumnIndexOrThrow("id"))
                var code = InviteCodeGenerator.generateInviteCode()
                
                // Ensure uniqueness (try up to 10 times)
                var attempts = 0
                while (attempts < 10) {
                    val checkCursor = database.query(
                        "SELECT COUNT(*) FROM family_invites WHERE inviteCode = ?",
                        arrayOf(code)
                    )
                    checkCursor.moveToFirst()
                    val count = checkCursor.getInt(0)
                    checkCursor.close()
                    
                    if (count == 0) break
                    code = InviteCodeGenerator.generateInviteCode()
                    attempts++
                }
                
                database.execSQL("UPDATE family_invites SET inviteCode = ? WHERE id = ?", arrayOf(code, id))
            }
            existingInvites.close()
            
            // Create unique index
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_family_invites_inviteCode ON family_invites(inviteCode)")
            
            AppLogger.Database.info("Migration 2->3 completed: Added inviteCode column")
        } else {
            AppLogger.Database.info("Migration 2->3 skipped: inviteCode column already exists")
        }
    }
}

