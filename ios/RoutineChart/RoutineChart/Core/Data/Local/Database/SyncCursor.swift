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

