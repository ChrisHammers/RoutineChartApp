//
//  SQLiteChildProfileRepository.swift
//  RoutineChart
//
//  SQLite implementation of ChildProfileRepository
//

import Foundation
import GRDB
import OSLog

final class SQLiteChildProfileRepository: ChildProfileRepository {
    private let dbManager: SQLiteManager
    
    init(dbManager: SQLiteManager = .shared) {
        self.dbManager = dbManager
    }
    
    func create(_ childProfile: ChildProfile) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            var mutableProfile = childProfile
            try mutableProfile.insert(db)
        }
        AppLogger.database.info("Created child profile: \(childProfile.id)")
    }
    
    func get(id: String) async throws -> ChildProfile? {
        let db = try dbManager.database()
        return try await db.read { db in
            try ChildProfile.fetchOne(db, key: id)
        }
    }
    
    func update(_ childProfile: ChildProfile) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            var mutableProfile = childProfile
            try mutableProfile.update(db)
        }
        AppLogger.database.info("Updated child profile: \(childProfile.id)")
    }
    
    func getAll(familyId: String) async throws -> [ChildProfile] {
        let db = try dbManager.database()
        return try await db.read { db in
            try ChildProfile
                .filter(Column("familyId") == familyId)
                .fetchAll(db)
        }
    }
}

