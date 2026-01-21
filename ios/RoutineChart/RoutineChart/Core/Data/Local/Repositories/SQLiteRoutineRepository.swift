//
//  SQLiteRoutineRepository.swift
//  RoutineChart
//
//  SQLite implementation of RoutineRepository
//

import Foundation
import GRDB
import OSLog

final class SQLiteRoutineRepository: RoutineRepository {
    private let dbManager: SQLiteManager
    
    init(dbManager: SQLiteManager = .shared) {
        self.dbManager = dbManager
    }
    
    func create(_ routine: Routine) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            var mutableRoutine = routine
            try mutableRoutine.insert(db)
            // Mark as unsynced (new records need to be uploaded)
            try db.execute(sql: "UPDATE routines SET synced = 0 WHERE id = ?", arguments: [routine.id])
        }
        AppLogger.database.info("Created routine: \(routine.id)")
    }
    
    func get(id: String) async throws -> Routine? {
        let db = try dbManager.database()
        return try await db.read { db in
            try Routine.fetchOne(db, key: id)
        }
    }
    
    func update(_ routine: Routine) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            var mutableRoutine = routine
            try mutableRoutine.update(db)
            // Mark as unsynced (updated records need to be uploaded)
            try db.execute(sql: "UPDATE routines SET synced = 0 WHERE id = ?", arguments: [routine.id])
        }
        AppLogger.database.info("Updated routine: \(routine.id)")
    }
    
    func getAll(familyId: String, includeDeleted: Bool) async throws -> [Routine] {
        let db = try dbManager.database()
        return try await db.read { db in
            var query = Routine
                .filter(Column("familyId") == familyId)
            
            if !includeDeleted {
                query = query.filter(Column("deletedAt") == nil)
            }
            
            return try query
                .order(Column("createdAt").desc)
                .fetchAll(db)
        }
    }
    
    func softDelete(id: String) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            guard var routine = try Routine.fetchOne(db, key: id) else {
                throw DatabaseError.recordNotFound
            }
            routine.deletedAt = Date()
            routine.updatedAt = Date()
            try routine.update(db)
            // Mark as unsynced (deletions need to be uploaded)
            try db.execute(sql: "UPDATE routines SET synced = 0 WHERE id = ?", arguments: [id])
        }
        AppLogger.database.info("Soft deleted routine: \(id)")
    }
    
    // MARK: - Sync Methods (Phase 3.2: Upload Queue)
    
    /// Get all unsynced routines for a family
    func getUnsynced(familyId: String) async throws -> [Routine] {
        let db = try dbManager.database()
        return try await db.read { db in
            // SQLite stores booleans as INTEGER (0 = false, 1 = true)
            // Use raw SQL for more reliable boolean comparison
            let unsyncedIds = try String.fetchAll(
                db,
                sql: "SELECT id FROM routines WHERE familyId = ? AND synced = 0",
                arguments: [familyId]
            )
            
            AppLogger.database.info("🔍 Found \(unsyncedIds.count) unsynced routine ID(s): \(unsyncedIds)")
            
            // Fetch the actual Routine objects
            var routines: [Routine] = []
            for id in unsyncedIds {
                if let routine = try Routine.fetchOne(db, key: id) {
                    routines.append(routine)
                }
            }
            
            return routines
        }
    }
    
    /// Mark a routine as synced
    func markAsSynced(routineId: String) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            try db.execute(sql: "UPDATE routines SET synced = 1 WHERE id = ?", arguments: [routineId])
        }
        AppLogger.database.info("Marked routine as synced: \(routineId)")
    }
    
    /// Mark multiple routines as synced
    func markAsSynced(routineIds: [String]) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            for routineId in routineIds {
                try db.execute(sql: "UPDATE routines SET synced = 1 WHERE id = ?", arguments: [routineId])
            }
        }
        AppLogger.database.info("Marked \(routineIds.count) routines as synced")
    }
}

