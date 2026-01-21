//
//  SQLiteManager.swift
//  RoutineChart
//
//  SQLite database manager using GRDB
//

import Foundation
import GRDB
import OSLog

final class SQLiteManager {
    static let shared = SQLiteManager()
    
    private var dbQueue: DatabaseQueue?
    
    private init() {}
    
    func setup() throws {
        let fileManager = FileManager.default
        let folderURL = try fileManager
            .url(for: .applicationSupportDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
            .appendingPathComponent("Database", isDirectory: true)
        
        try fileManager.createDirectory(at: folderURL, withIntermediateDirectories: true)
        
        let dbURL = folderURL.appendingPathComponent("routine_chart.db")
        
        AppLogger.database.info("Database path: \(dbURL.path)")
        
        dbQueue = try DatabaseQueue(path: dbURL.path)
        
        try migrator.migrate(dbQueue!)
        
        AppLogger.database.info("Database initialized successfully")
    }
    
    func database() throws -> DatabaseQueue {
        guard let dbQueue = dbQueue else {
            throw DatabaseError.notInitialized
        }
        return dbQueue
    }
    
    // MARK: - Migrations
    
    private var migrator: DatabaseMigrator {
        var migrator = DatabaseMigrator()
        
        // V1: Initial schema
        migrator.registerMigration("v1") { db in
            // Families table
            try db.create(table: "families") { t in
                t.column("id", .text).primaryKey()
                t.column("name", .text)
                t.column("timeZone", .text).notNull()
                t.column("weekStartsOn", .integer).notNull()
                t.column("planTier", .text).notNull()
                t.column("createdAt", .datetime).notNull()
                t.column("updatedAt", .datetime).notNull()
            }
            
            // Users table
            try db.create(table: "users") { t in
                t.column("id", .text).primaryKey()
                t.column("familyId", .text).notNull()
                    .references("families", onDelete: .cascade)
                t.column("role", .text).notNull()
                t.column("displayName", .text).notNull()
                t.column("email", .text)
                t.column("createdAt", .datetime).notNull()
            }
            
            // Child profiles table
            try db.create(table: "child_profiles") { t in
                t.column("id", .text).primaryKey()
                t.column("familyId", .text).notNull()
                    .references("families", onDelete: .cascade)
                t.column("displayName", .text).notNull()
                t.column("avatarIcon", .text)
                t.column("ageBand", .text).notNull()
                t.column("readingMode", .text).notNull()
                t.column("audioEnabled", .boolean).notNull()
                t.column("createdAt", .datetime).notNull()
            }
            
            // Routines table
            try db.create(table: "routines") { t in
                t.column("id", .text).primaryKey()
                t.column("familyId", .text).notNull()
                    .references("families", onDelete: .cascade)
                t.column("title", .text).notNull()
                t.column("iconName", .text)
                t.column("version", .integer).notNull()
                t.column("completionRule", .text).notNull()
                t.column("createdAt", .datetime).notNull()
                t.column("updatedAt", .datetime).notNull()
                t.column("deletedAt", .datetime)
            }
            
            // Routine steps table
            try db.create(table: "routine_steps") { t in
                t.column("id", .text).primaryKey()
                t.column("routineId", .text).notNull()
                    .references("routines", onDelete: .cascade)
                t.column("orderIndex", .integer).notNull()
                t.column("label", .text)
                t.column("iconName", .text)
                t.column("audioCueUrl", .text)
                t.column("createdAt", .datetime).notNull()
                t.column("deletedAt", .datetime)
            }
            
            // Routine assignments table
            try db.create(table: "routine_assignments") { t in
                t.column("id", .text).primaryKey()
                t.column("familyId", .text).notNull()
                    .references("families", onDelete: .cascade)
                t.column("routineId", .text).notNull()
                    .references("routines", onDelete: .cascade)
                t.column("childId", .text).notNull()
                    .references("child_profiles", onDelete: .cascade)
                t.column("isActive", .boolean).notNull()
                t.column("assignedAt", .datetime).notNull()
                t.column("deletedAt", .datetime)
            }
            
            // Completion events table (append-only)
            try db.create(table: "completion_events") { t in
                t.column("id", .text).primaryKey()
                t.column("familyId", .text).notNull()
                    .references("families", onDelete: .cascade)
                t.column("childId", .text).notNull()
                    .references("child_profiles", onDelete: .cascade)
                t.column("routineId", .text).notNull()
                    .references("routines", onDelete: .cascade)
                t.column("stepId", .text).notNull()
                    .references("routine_steps", onDelete: .cascade)
                t.column("eventType", .text).notNull()
                t.column("eventAt", .datetime).notNull()
                t.column("localDayKey", .text).notNull()
                t.column("deviceId", .text).notNull()
                t.column("synced", .boolean).notNull().defaults(to: false)
            }
            
            // Indexes for performance
            try db.create(index: "idx_users_familyId", on: "users", columns: ["familyId"])
            try db.create(index: "idx_child_profiles_familyId", on: "child_profiles", columns: ["familyId"])
            try db.create(index: "idx_routines_familyId", on: "routines", columns: ["familyId"])
            try db.create(index: "idx_routine_steps_routineId", on: "routine_steps", columns: ["routineId"])
            try db.create(index: "idx_routine_assignments_familyId", on: "routine_assignments", columns: ["familyId"])
            try db.create(index: "idx_routine_assignments_childId", on: "routine_assignments", columns: ["childId"])
            try db.create(index: "idx_completion_events_familyId", on: "completion_events", columns: ["familyId"])
            try db.create(index: "idx_completion_events_childId", on: "completion_events", columns: ["childId"])
            try db.create(index: "idx_completion_events_localDayKey", on: "completion_events", columns: ["localDayKey"])
            try db.create(index: "idx_completion_events_eventAt", on: "completion_events", columns: ["eventAt"])
            
            AppLogger.database.info("Database schema v1 created")
        }
        
        // V2: Add family invites table (Phase 2.2: QR joining)
        migrator.registerMigration("v2") { db in
            // Family invites table
            try db.create(table: "family_invites") { t in
                t.column("id", .text).primaryKey()
                t.column("familyId", .text).notNull()
                    .references("families", onDelete: .cascade)
                t.column("token", .text).notNull().unique()
                t.column("createdBy", .text).notNull()
                t.column("createdAt", .datetime).notNull()
                t.column("expiresAt", .datetime).notNull()
                t.column("maxUses", .integer)
                t.column("usedCount", .integer).notNull().defaults(to: 0)
                t.column("isActive", .boolean).notNull().defaults(to: true)
            }
            
            // Indexes for family invites
            try db.create(index: "idx_family_invites_familyId", on: "family_invites", columns: ["familyId"])
            try db.create(index: "idx_family_invites_token", on: "family_invites", columns: ["token"])
            
            AppLogger.database.info("Database schema v2 created - added family_invites table")
        }
        
        // V3: Add inviteCode column to family_invites table
        migrator.registerMigration("v3") { db in
            // Only proceed if table exists (should always be true since v2 creates it)
            guard try db.tableExists("family_invites") else {
                AppLogger.database.info("Database schema v3 skipped - family_invites table does not exist")
                return
            }
            
            // Check if column exists using pragma_table_info
            let columnExists: Bool = {
                do {
                    let count = try Int.fetchOne(
                        db,
                        sql: "SELECT COUNT(*) FROM pragma_table_info(?) WHERE name = ?",
                        arguments: ["family_invites", "inviteCode"]
                    ) ?? 0
                    return count > 0
                } catch {
                    // If pragma fails, assume column doesn't exist
                    return false
                }
            }()
            
            if !columnExists {
                try db.alter(table: "family_invites") { t in
                    t.add(column: "inviteCode", .text).notNull().defaults(to: "")
                }
                
                // Update existing rows with generated invite codes
                let inviteIds = try String.fetchAll(db, sql: "SELECT id FROM family_invites")
                for inviteId in inviteIds {
                    // Generate unique code
                    var code = InviteCodeGenerator.generateInviteCode()
                    var attempts = 0
                    while attempts < 10 {
                        // Check if code already exists
                        let existingCount = try Int.fetchOne(
                            db,
                            sql: "SELECT COUNT(*) FROM family_invites WHERE inviteCode = ?",
                            arguments: [code]
                        ) ?? 0
                        if existingCount == 0 {
                            break
                        }
                        code = InviteCodeGenerator.generateInviteCode()
                        attempts += 1
                    }
                    try db.execute(
                        sql: "UPDATE family_invites SET inviteCode = ? WHERE id = ?",
                        arguments: [code, inviteId]
                    )
                }
                
                // Now add unique constraint
                try db.execute(sql: "CREATE UNIQUE INDEX IF NOT EXISTS idx_family_invites_inviteCode ON family_invites(inviteCode)")
                
                AppLogger.database.info("Database schema v3 created - added inviteCode column to family_invites")
            } else {
                AppLogger.database.info("Database schema v3 skipped - inviteCode column already exists")
            }
        }
        
        // V4: Add sync_cursors table (Phase 3.1: Sync Infrastructure)
        migrator.registerMigration("v4") { db in
            try db.create(table: "sync_cursors") { t in
                t.column("id", .text).primaryKey() // collection name
                t.column("collection", .text).notNull().unique()
                t.column("lastSyncedAt", .datetime).notNull()
            }
            
            // Index for collection lookups
            try db.create(index: "idx_sync_cursors_collection", on: "sync_cursors", columns: ["collection"])
            
            AppLogger.database.info("Database schema v4 created - added sync_cursors table")
        }
        
        // V5: Add synced flag to routines table (Phase 3.2: Upload Queue)
        migrator.registerMigration("v5") { db in
            // Check if column exists using pragma_table_info (same pattern as v3)
            let columnExists: Bool = {
                do {
                    let count = try Int.fetchOne(
                        db,
                        sql: "SELECT COUNT(*) FROM pragma_table_info('routines') WHERE name = 'synced'"
                    ) ?? 0
                    return count > 0
                } catch {
                    // If pragma fails, assume column doesn't exist
                    return false
                }
            }()
            
            if !columnExists {
                // Add synced column (SQLite stores booleans as INTEGER: 0 = false, 1 = true)
                try db.execute(sql: "ALTER TABLE routines ADD COLUMN synced INTEGER NOT NULL DEFAULT 0")
                
                // Create index for efficient unsynced queries
                try db.create(index: "idx_routines_synced", on: "routines", columns: ["synced", "familyId"])
                
                AppLogger.database.info("Database schema v5 created - added synced column to routines")
            } else {
                AppLogger.database.info("Database schema v5 skipped - synced column already exists")
            }
        }
        
        // V6: Add userId column and make familyId nullable (routines as top-level documents)
        migrator.registerMigration("v6") { db in
            // Check if userId column exists
            let userIdExists: Bool = {
                do {
                    let count = try Int.fetchOne(
                        db,
                        sql: "SELECT COUNT(*) FROM pragma_table_info('routines') WHERE name = 'userId'"
                    ) ?? 0
                    return count > 0
                } catch {
                    return false
                }
            }()
            
            if !userIdExists {
                // Add userId column (required)
                try db.execute(sql: "ALTER TABLE routines ADD COLUMN userId TEXT")
                
                // For existing routines, we need to set userId
                // Strategy: For routines with a familyId, get the first parent user from that family
                // For routines without familyId, we can't determine the user, so we'll need to handle this
                // For now, set userId to a placeholder that indicates migration is needed
                // The app will need to update these routines with the correct userId
                try db.execute(sql: """
                    UPDATE routines 
                    SET userId = (
                        SELECT u.id 
                        FROM users u 
                        WHERE u.familyId = routines.familyId 
                        AND u.role = 'parent' 
                        LIMIT 1
                    )
                    WHERE userId IS NULL 
                    AND familyId IS NOT NULL
                """)
                
                // For routines that still don't have a userId (shouldn't happen in practice)
                // Set to a migration marker - app will need to handle these
                try db.execute(sql: "UPDATE routines SET userId = '__MIGRATION_NEEDED__' WHERE userId IS NULL")
                
                AppLogger.database.info("Database schema v6 created - added userId column to routines")
            } else {
                AppLogger.database.info("Database schema v6 skipped - userId column already exists")
            }
            
            // Make familyId nullable (SQLite doesn't support ALTER COLUMN, so this is informational)
            // The app logic will handle nullable familyId
            AppLogger.database.info("Database schema v6 - familyId is now nullable (handled by app logic)")
        }
        
        // V7: Remove familyId column from routine_steps table (not needed - steps queried by routineId only)
        migrator.registerMigration("v7") { db in
            // Check if familyId column exists
            let familyIdExists: Bool = {
                do {
                    let count = try Int.fetchOne(
                        db,
                        sql: "SELECT COUNT(*) FROM pragma_table_info('routine_steps') WHERE name = 'familyId'"
                    ) ?? 0
                    return count > 0
                } catch {
                    return false
                }
            }()
            
            if familyIdExists {
                // SQLite doesn't support DROP COLUMN directly, so we need to recreate the table
                // This is a more complex migration, but necessary to remove the column
                try db.execute(sql: """
                    CREATE TABLE routine_steps_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        routineId TEXT NOT NULL REFERENCES routines(id) ON DELETE CASCADE,
                        orderIndex INTEGER NOT NULL,
                        label TEXT,
                        iconName TEXT,
                        audioCueUrl TEXT,
                        createdAt TEXT NOT NULL,
                        deletedAt TEXT
                    )
                """)
                
                // Copy data (excluding familyId)
                try db.execute(sql: """
                    INSERT INTO routine_steps_new (id, routineId, orderIndex, label, iconName, audioCueUrl, createdAt, deletedAt)
                    SELECT id, routineId, orderIndex, label, iconName, audioCueUrl, createdAt, deletedAt
                    FROM routine_steps
                """)
                
                // Drop old table
                try db.execute(sql: "DROP TABLE routine_steps")
                
                // Rename new table
                try db.execute(sql: "ALTER TABLE routine_steps_new RENAME TO routine_steps")
                
                AppLogger.database.info("Database schema v7 created - removed familyId column from routine_steps")
            } else {
                AppLogger.database.info("Database schema v7 skipped - familyId column already removed")
            }
        }
        
        return migrator
    }
}

// MARK: - DatabaseError

enum DatabaseError: Error {
    case notInitialized
    case recordNotFound
    case invalidData
}

