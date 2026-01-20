//
//  SyncCursorManager.swift
//  RoutineChart
//
//  Created for Phase 3.1: Sync Infrastructure
//  Manages sync cursors for tracking last sync timestamp per collection
//

import Foundation
import GRDB
import OSLog

/// Manages sync cursors for tracking last sync timestamp per collection
final class SyncCursorManager {
    private let dbManager: SQLiteManager
    
    init(dbManager: SQLiteManager = .shared) {
        self.dbManager = dbManager
    }
    
    /// Get the last sync timestamp for a collection
    /// Returns nil if no cursor exists (first sync)
    func getCursor(collection: String) async throws -> SyncCursor? {
        let db = try dbManager.database()
        return try await db.read { db in
            try SyncCursor.fetchOne(db, key: collection)
        }
    }
    
    /// Update or create a cursor for a collection
    func updateCursor(collection: String, lastSyncedAt: Date) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            var cursor = SyncCursor(collection: collection, lastSyncedAt: lastSyncedAt)
            try cursor.save(db)
        }
        AppLogger.database.info("Updated sync cursor for collection '\(collection)' to \(lastSyncedAt)")
    }
    
    /// Delete a cursor (useful for resetting sync state)
    func deleteCursor(collection: String) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            try SyncCursor.deleteOne(db, key: collection)
        }
        AppLogger.database.info("Deleted sync cursor for collection '\(collection)'")
    }
    
    /// Get all cursors (for debugging/monitoring)
    func getAllCursors() async throws -> [SyncCursor] {
        let db = try dbManager.database()
        return try await db.read { db in
            try SyncCursor.fetchAll(db)
        }
    }
}
