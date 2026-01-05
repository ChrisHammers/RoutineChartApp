//
//  SQLiteRoutineAssignmentRepository.swift
//  RoutineChart
//
//  SQLite implementation of RoutineAssignmentRepository
//

import Foundation
import GRDB
import OSLog

final class SQLiteRoutineAssignmentRepository: RoutineAssignmentRepository {
    private let dbManager: SQLiteManager
    
    init(dbManager: SQLiteManager = .shared) {
        self.dbManager = dbManager
    }
    
    func create(_ assignment: RoutineAssignment) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            var mutableAssignment = assignment
            try mutableAssignment.insert(db)
        }
        AppLogger.database.info("Created assignment: \(assignment.id)")
    }
    
    func get(id: String) async throws -> RoutineAssignment? {
        let db = try dbManager.database()
        return try await db.read { db in
            try RoutineAssignment.fetchOne(db, key: id)
        }
    }
    
    func update(_ assignment: RoutineAssignment) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            var mutableAssignment = assignment
            try mutableAssignment.update(db)
        }
        AppLogger.database.info("Updated assignment: \(assignment.id)")
    }
    
    func getAll(familyId: String) async throws -> [RoutineAssignment] {
        let db = try dbManager.database()
        return try await db.read { db in
            try RoutineAssignment
                .filter(Column("familyId") == familyId)
                .filter(Column("deletedAt") == nil)
                .fetchAll(db)
        }
    }
    
    func getByChild(familyId: String, childId: String) async throws -> [RoutineAssignment] {
        let db = try dbManager.database()
        return try await db.read { db in
            try RoutineAssignment
                .filter(Column("familyId") == familyId)
                .filter(Column("childId") == childId)
                .filter(Column("deletedAt") == nil)
                .filter(Column("isActive") == true)
                .fetchAll(db)
        }
    }
    
    func getByRoutine(familyId: String, routineId: String) async throws -> [RoutineAssignment] {
        let db = try dbManager.database()
        return try await db.read { db in
            try RoutineAssignment
                .filter(Column("familyId") == familyId)
                .filter(Column("routineId") == routineId)
                .filter(Column("deletedAt") == nil)
                .fetchAll(db)
        }
    }
    
    func softDelete(id: String) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            guard var assignment = try RoutineAssignment.fetchOne(db, key: id) else {
                throw DatabaseError.recordNotFound
            }
            assignment.deletedAt = Date()
            try assignment.update(db)
        }
        AppLogger.database.info("Soft deleted assignment: \(id)")
    }
}

