//
//  SQLiteFamilyInviteRepository.swift
//  RoutineChart
//
//  Created for Phase 2.2: QR Family Joining
//

import Foundation
import GRDB
import OSLog

final class SQLiteFamilyInviteRepository: FamilyInviteRepository {
    private let dbManager: SQLiteManager
    
    init(dbManager: SQLiteManager = .shared) {
        self.dbManager = dbManager
    }
    
    func create(_ invite: FamilyInvite) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            var mutableInvite = invite
            try mutableInvite.insert(db)
        }
        AppLogger.database.info("Created family invite: \(invite.id)")
    }
    
    func get(id: String) async throws -> FamilyInvite? {
        let db = try dbManager.database()
        return try await db.read { db in
            try FamilyInvite.fetchOne(db, key: id)
        }
    }
    
    func getByToken(_ token: String) async throws -> FamilyInvite? {
        let db = try dbManager.database()
        return try await db.read { db in
            try FamilyInvite
                .filter(Column("token") == token)
                .fetchOne(db)
        }
    }
    
    func getByInviteCode(_ inviteCode: String) async throws -> FamilyInvite? {
        let db = try dbManager.database()
        return try await db.read { db in
            try FamilyInvite
                .filter(Column("inviteCode") == inviteCode)
                .fetchOne(db)
        }
    }
    
    func getActiveInvites(familyId: String) async throws -> [FamilyInvite] {
        let db = try dbManager.database()
        return try await db.read { db in
            try FamilyInvite
                .filter(Column("familyId") == familyId)
                .filter(Column("isActive") == true)
                .order(Column("createdAt").desc)
                .fetchAll(db)
        }
    }
    
    func update(_ invite: FamilyInvite) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            var mutableInvite = invite
            try mutableInvite.update(db)
        }
        AppLogger.database.info("Updated family invite: \(invite.id)")
    }
    
    func deactivate(id: String) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            try db.execute(
                sql: "UPDATE family_invites SET isActive = 0 WHERE id = ?",
                arguments: [id]
            )
        }
        AppLogger.database.info("Deactivated family invite: \(id)")
    }
    
    func deleteExpired() async throws {
        let db = try dbManager.database()
        try await db.write { db in
            let now = Date()
            try db.execute(
                sql: "DELETE FROM family_invites WHERE expiresAt < ?",
                arguments: [now]
            )
        }
        AppLogger.database.info("Deleted expired family invites")
    }
}

