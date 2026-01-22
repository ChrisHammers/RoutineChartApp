//
//  SQLiteRoutineStepRepository.swift
//  RoutineChart
//
//  SQLite implementation of RoutineStepRepository
//

import Foundation
import GRDB
import OSLog

final class SQLiteRoutineStepRepository: RoutineStepRepository {
    private let dbManager: SQLiteManager
    
    init(dbManager: SQLiteManager = .shared) {
        self.dbManager = dbManager
    }
    
    func create(_ step: RoutineStep) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            var mutableStep = step
            try mutableStep.insert(db)
            // Mark as unsynced (new records need to be uploaded)
            try db.execute(sql: "UPDATE routine_steps SET synced = 0 WHERE id = ?", arguments: [step.id])
        }
        AppLogger.database.info("Created step: \(step.id)")
    }
    
    func get(id: String) async throws -> RoutineStep? {
        let db = try dbManager.database()
        return try await db.read { db in
            try RoutineStep.fetchOne(db, key: id)
        }
    }
    
    func update(_ step: RoutineStep) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            var mutableStep = step
            try mutableStep.update(db)
            // Mark as unsynced (updated records need to be uploaded)
            try db.execute(sql: "UPDATE routine_steps SET synced = 0 WHERE id = ?", arguments: [step.id])
        }
        AppLogger.database.info("Updated step: \(step.id)")
    }
    
    func getAll(routineId: String) async throws -> [RoutineStep] {
        let db = try dbManager.database()
        return try await db.read { db in
            try RoutineStep
                .filter(Column("routineId") == routineId)
                .filter(Column("deletedAt") == nil)
                .order(Column("orderIndex"))
                .fetchAll(db)
        }
    }
    
    func softDelete(id: String) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            guard var step = try RoutineStep.fetchOne(db, key: id) else {
                throw DatabaseError.recordNotFound
            }
            step.deletedAt = Date()
            try step.update(db)
            // Mark as unsynced (deletions need to be uploaded)
            try db.execute(sql: "UPDATE routine_steps SET synced = 0 WHERE id = ?", arguments: [id])
        }
        AppLogger.database.info("Soft deleted step: \(id)")
    }
    
    // MARK: - Sync Methods (Phase 3.4: Upload Queue)
    
    /// Get all unsynced steps for a routine
    func getUnsynced(routineId: String) async throws -> [RoutineStep] {
        let db = try dbManager.database()
        return try await db.read { db in
            // SQLite stores booleans as INTEGER (0 = false, 1 = true)
            // Use raw SQL for more reliable boolean comparison
            let unsyncedIds = try String.fetchAll(
                db,
                sql: "SELECT id FROM routine_steps WHERE routineId = ? AND synced = 0",
                arguments: [routineId]
            )
            
            // Fetch the actual RoutineStep objects
            var steps: [RoutineStep] = []
            for id in unsyncedIds {
                if let step = try RoutineStep.fetchOne(db, key: id) {
                    steps.append(step)
                }
            }
            
            return steps
        }
    }
    
    /// Mark a step as synced
    func markAsSynced(stepId: String) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            try db.execute(sql: "UPDATE routine_steps SET synced = 1 WHERE id = ?", arguments: [stepId])
        }
        AppLogger.database.info("Marked step as synced: \(stepId)")
    }
    
    /// Mark multiple steps as synced
    func markAsSynced(stepIds: [String]) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            for stepId in stepIds {
                try db.execute(sql: "UPDATE routine_steps SET synced = 1 WHERE id = ?", arguments: [stepId])
            }
        }
        AppLogger.database.info("Marked \(stepIds.count) steps as synced")
    }
}

