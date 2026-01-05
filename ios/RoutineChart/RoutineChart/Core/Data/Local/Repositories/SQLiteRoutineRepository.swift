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
        }
        AppLogger.database.info("Soft deleted routine: \(id)")
    }
}

