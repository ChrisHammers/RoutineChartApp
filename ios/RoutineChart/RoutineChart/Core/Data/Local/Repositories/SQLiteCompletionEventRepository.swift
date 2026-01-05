//
//  SQLiteCompletionEventRepository.swift
//  RoutineChart
//
//  SQLite implementation of CompletionEventRepository
//

import Foundation
import GRDB
import OSLog

final class SQLiteCompletionEventRepository: CompletionEventRepository {
    private let dbManager: SQLiteManager
    
    init(dbManager: SQLiteManager = .shared) {
        self.dbManager = dbManager
    }
    
    func create(_ event: CompletionEvent) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            var mutableEvent = event
            try mutableEvent.insert(db)
        }
        AppLogger.database.info("Created completion event: \(event.id)")
    }
    
    func getEvents(
        familyId: String,
        childId: String?,
        routineId: String?,
        stepId: String?,
        localDayKey: String?
    ) async throws -> [CompletionEvent] {
        let db = try dbManager.database()
        return try await db.read { db in
            var query = CompletionEvent
                .filter(Column("familyId") == familyId)
            
            if let childId = childId {
                query = query.filter(Column("childId") == childId)
            }
            
            if let routineId = routineId {
                query = query.filter(Column("routineId") == routineId)
            }
            
            if let stepId = stepId {
                query = query.filter(Column("stepId") == stepId)
            }
            
            if let localDayKey = localDayKey {
                query = query.filter(Column("localDayKey") == localDayKey)
            }
            
            return try query
                .order(Column("eventAt").asc, Column("id").asc)
                .fetchAll(db)
        }
    }
    
    func getUnsyncedEvents() async throws -> [CompletionEvent] {
        let db = try dbManager.database()
        return try await db.read { db in
            try CompletionEvent
                .filter(Column("synced") == false)
                .order(Column("eventAt").asc)
                .fetchAll(db)
        }
    }
    
    func markSynced(eventId: String) async throws {
        let db = try dbManager.database()
        try await db.write { db in
            guard var event = try CompletionEvent.fetchOne(db, key: eventId) else {
                throw DatabaseError.recordNotFound
            }
            event.synced = true
            try event.update(db)
        }
        AppLogger.database.info("Marked event as synced: \(eventId)")
    }
}

