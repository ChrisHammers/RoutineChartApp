//
//  SQLiteUserRepository.swift
//  RoutineChart
//
//  SQLite implementation of UserRepository
//

import Foundation
import GRDB
import OSLog

final class SQLiteUserRepository: UserRepository {
    private let dbManager: SQLiteManager
    
    init(dbManager: SQLiteManager = .shared) {
        self.dbManager = dbManager
    }
    
    func create(_ user: User) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            var mutableUser = user
            try mutableUser.insert(db)
        }
        AppLogger.database.info("Created user: \(user.id)")
    }
    
    func get(id: String) async throws -> User? {
        let db = try dbManager.database()
        return try await db.read { db in
            try User.fetchOne(db, key: id)
        }
    }
    
    func update(_ user: User) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            var mutableUser = user
            try mutableUser.update(db)
        }
        AppLogger.database.info("Updated user: \(user.id)")
    }
    
    func getAll(familyId: String) async throws -> [User] {
        let db = try dbManager.database()
        return try await db.read { db in
            try User
                .filter(Column("familyId") == familyId)
                .fetchAll(db)
        }
    }
    
    func updateFamilyId(userId: String, familyId: String) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            guard var user = try User.fetchOne(db, key: userId) else {
                throw DatabaseError.recordNotFound
            }
            // Create updated user with new familyId
            let updatedUser = User(
                id: user.id,
                familyId: familyId,
                role: user.role,
                displayName: user.displayName,
                email: user.email,
                createdAt: user.createdAt
            )
            var mutableUser = updatedUser
            try mutableUser.update(db)
        }
        AppLogger.database.info("Updated user \(userId) familyId to \(familyId)")
    }
}

