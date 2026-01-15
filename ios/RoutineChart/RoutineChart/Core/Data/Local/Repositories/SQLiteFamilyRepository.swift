//
//  SQLiteFamilyRepository.swift
//  RoutineChart
//
//  SQLite implementation of FamilyRepository
//

import Foundation
import GRDB
import OSLog

final class SQLiteFamilyRepository: FamilyRepository {
    private let dbManager: SQLiteManager
    
    init(dbManager: SQLiteManager = .shared) {
        self.dbManager = dbManager
    }
    
    func create(_ family: Family) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            var mutableFamily = family
            try mutableFamily.insert(db)
        }
        AppLogger.database.info("Created family: \(family.id)")
    }
    
    func get(id: String) async throws -> Family? {
        let db = try dbManager.database()
        return try await db.read { db in
            try Family.fetchOne(db, key: id)
        }
    }
    
    func update(_ family: Family) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            var mutableFamily = family
            try mutableFamily.update(db)
        }
        AppLogger.database.info("Updated family: \(family.id)")
    }
    
    func getAll() async throws -> [Family] {
        let db = try dbManager.database()
        return try await db.read { db in
            try Family.fetchAll(db)
        }
    }
    
    func delete(id: String) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            try db.execute(sql: "DELETE FROM families WHERE id = ?", arguments: [id])
        }
        AppLogger.database.info("Deleted family: \(id)")
    }
    
    func deleteAllExcept(familyIds: Set<String>) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            let allFamilies = try Family.fetchAll(db)
            var deletedCount = 0
            for family in allFamilies {
                if !familyIds.contains(family.id) {
                    try db.execute(sql: "DELETE FROM families WHERE id = ?", arguments: [family.id])
                    deletedCount += 1
                    AppLogger.database.info("Deleted orphaned family: \(family.id)")
                }
            }
            if deletedCount > 0 {
                AppLogger.database.info("🧹 Cleaned up \(deletedCount) orphaned families")
            }
        }
    }
}

