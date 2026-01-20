//
//  SyncCursor.swift
//  RoutineChart
//
//  Created for Phase 3.1: Sync Infrastructure
//  Tracks last sync timestamp per collection for pull cursor pattern
//

import Foundation
import GRDB

/// Tracks the last sync timestamp for a specific collection
struct SyncCursor: Identifiable, Equatable, Sendable {
    let id: String // collection name (e.g., "routines", "events")
    let collection: String
    let lastSyncedAt: Date
    
    init(collection: String, lastSyncedAt: Date) {
        self.id = collection
        self.collection = collection
        self.lastSyncedAt = lastSyncedAt
    }
}

// MARK: - GRDB Conformance

extension SyncCursor: FetchableRecord, MutablePersistableRecord {
    static let databaseTableName = "sync_cursors"
    
    enum Columns: String, ColumnExpression {
        case id, collection, lastSyncedAt
    }
    
    nonisolated init(row: Row) {
        id = row[Columns.id]
        collection = row[Columns.collection]
        lastSyncedAt = row[Columns.lastSyncedAt]
    }
    
    nonisolated func encode(to container: inout PersistenceContainer) {
        container[Columns.id] = id
        container[Columns.collection] = collection
        container[Columns.lastSyncedAt] = lastSyncedAt
    }
}
