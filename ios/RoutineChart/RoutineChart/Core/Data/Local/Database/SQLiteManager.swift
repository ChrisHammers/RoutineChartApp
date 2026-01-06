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
                t.column("familyId", .text).notNull()
                    .references("families", onDelete: .cascade)
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
        
        return migrator
    }
}

// MARK: - DatabaseError

enum DatabaseError: Error {
    case notInitialized
    case recordNotFound
    case invalidData
}

