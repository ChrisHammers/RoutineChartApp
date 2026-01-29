//
//  CompositeRoutineAssignmentRepository.swift
//  RoutineChart
//
//  Phase 3.5: Composite repository - SQLite (source of truth) + Firestore sync via upload queue and pull cursor
//

import Foundation
import GRDB
import OSLog

/// Composite repository that uses SQLite as source of truth and syncs assignments to Firestore via upload queue and pull cursor
final class CompositeRoutineAssignmentRepository: RoutineAssignmentRepository {
    private let localRepo: SQLiteRoutineAssignmentRepository
    private let uploadQueue: AssignmentUploadQueueService
    private let syncService: FirestoreAssignmentSyncService
    private let cursorManager: SyncCursorManager
    
    init(
        localRepo: SQLiteRoutineAssignmentRepository = SQLiteRoutineAssignmentRepository(),
        uploadQueue: AssignmentUploadQueueService = AssignmentUploadQueueService(),
        syncService: FirestoreAssignmentSyncService = FirestoreAssignmentSyncService(),
        cursorManager: SyncCursorManager = SyncCursorManager()
    ) {
        self.localRepo = localRepo
        self.uploadQueue = uploadQueue
        self.syncService = syncService
        self.cursorManager = cursorManager
    }
    
    // MARK: - Repository Methods (Local-First)
    
    func create(_ assignment: RoutineAssignment) async throws {
        try await localRepo.create(assignment)
        AppLogger.database.info("Created assignment locally (will sync via upload queue): \(assignment.id)")
    }
    
    func get(id: String) async throws -> RoutineAssignment? {
        try await localRepo.get(id: id)
    }
    
    func update(_ assignment: RoutineAssignment) async throws {
        try await localRepo.update(assignment)
        AppLogger.database.info("Updated assignment locally (will sync via upload queue): \(assignment.id)")
    }
    
    func getAll(familyId: String) async throws -> [RoutineAssignment] {
        try await localRepo.getAll(familyId: familyId)
    }
    
    func getByChild(familyId: String, childId: String) async throws -> [RoutineAssignment] {
        try await localRepo.getByChild(familyId: familyId, childId: childId)
    }
    
    func getByRoutine(familyId: String, routineId: String) async throws -> [RoutineAssignment] {
        try await localRepo.getByRoutine(familyId: familyId, routineId: routineId)
    }
    
    func softDelete(id: String) async throws {
        try await localRepo.softDelete(id: id)
        AppLogger.database.info("Soft deleted assignment locally (will sync via upload queue): \(id)")
    }
    
    // MARK: - Upload Queue (Phase 3.5)
    
    /// Upload all unsynced assignments for a family
    func uploadUnsynced(familyId: String) async throws -> Int {
        try await uploadQueue.uploadUnsyncedAssignments(familyId: familyId)
    }
    
    // MARK: - Pull Cursor (Phase 3.5)
    
    /// Pull assignments from Firestore that were updated since the last sync
    /// Applies merge logic (last-write-wins by updatedAt) and updates the sync cursor
    func pullAssignments(familyId: String) async throws -> Int {
        AppLogger.database.info("🔄 Starting pull of assignments from Firestore for familyId: \(familyId)")
        
        let cursor = try await cursorManager.getCursor(collection: "routine_assignments")
        let lastSyncedAt = cursor?.lastSyncedAt ?? Date(timeIntervalSince1970: 0)
        
        let remoteAssignments = try await syncService.getAssignmentsUpdatedSince(familyId: familyId, since: lastSyncedAt)
        
        guard !remoteAssignments.isEmpty else {
            AppLogger.database.info("✅ No new assignments to pull from Firestore")
            try await cursorManager.updateCursor(collection: "routine_assignments", lastSyncedAt: Date())
            return 0
        }
        
        AppLogger.database.info("📥 Found \(remoteAssignments.count) assignment(s) to pull from Firestore")
        
        var mergedCount = 0
        var skippedCount = 0
        
        for remote in remoteAssignments {
            do {
                if let local = try await localRepo.get(id: remote.id) {
                    if remote.updatedAt > local.updatedAt {
                        try await saveAssignmentFromFirestore(remote)
                        mergedCount += 1
                        AppLogger.database.info("✅ Merged assignment (remote wins): \(remote.id)")
                    } else {
                        skippedCount += 1
                        AppLogger.database.info("⏭️ Skipped assignment (local wins): \(remote.id)")
                    }
                } else {
                    try await saveAssignmentFromFirestore(remote)
                    mergedCount += 1
                    AppLogger.database.info("✅ Inserted new assignment from Firestore: \(remote.id)")
                }
            } catch {
                AppLogger.database.error("❌ Failed to merge assignment \(remote.id): \(error.localizedDescription)")
            }
        }
        
        try await cursorManager.updateCursor(collection: "routine_assignments", lastSyncedAt: Date())
        AppLogger.database.info("✅ Pull complete: merged \(mergedCount) assignment(s), skipped \(skippedCount)")
        return mergedCount
    }
    
    /// Save assignment from Firestore to local database and mark as synced
    private func saveAssignmentFromFirestore(_ assignment: RoutineAssignment) async throws {
        let db = try SQLiteManager.shared.database()
        try await db.write { db in
            var mutable = assignment
            if try RoutineAssignment.exists(db, key: assignment.id) {
                try mutable.update(db)
            } else {
                try mutable.insert(db)
            }
            try db.execute(sql: "UPDATE routine_assignments SET synced = 1 WHERE id = ?", arguments: [assignment.id])
        }
    }
}
