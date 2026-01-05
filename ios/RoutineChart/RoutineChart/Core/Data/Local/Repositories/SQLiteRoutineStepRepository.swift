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
        }
        AppLogger.database.info("Soft deleted step: \(id)")
    }
}

