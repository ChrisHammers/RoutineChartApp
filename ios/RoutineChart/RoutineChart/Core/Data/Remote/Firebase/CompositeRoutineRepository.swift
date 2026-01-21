//
//  CompositeRoutineRepository.swift
//  RoutineChart
//
//  Created for Phase 3.2: Upload Queue (Routines)
//  Composite repository: SQLite (source of truth) + Firestore sync via upload queue
//

import Foundation
import OSLog

/// Composite repository that uses SQLite as source of truth and syncs to Firestore via upload queue
final class CompositeRoutineRepository: RoutineRepository {
    private let localRepo: SQLiteRoutineRepository
    private let uploadQueue: RoutineUploadQueueService
    
    init(
        localRepo: SQLiteRoutineRepository = SQLiteRoutineRepository(),
        uploadQueue: RoutineUploadQueueService = RoutineUploadQueueService()
    ) {
        self.localRepo = localRepo
        self.uploadQueue = uploadQueue
    }
    
    // MARK: - Repository Methods (Local-First)
    
    func create(_ routine: Routine) async throws {
        // Always write to local first (offline-first)
        // The local repo will mark it as unsynced
        try await localRepo.create(routine)
        
        // Upload queue will be processed separately (e.g., on app launch, periodic sync)
        // We don't block on network here - offline-first approach
        AppLogger.database.info("Created routine locally (will sync via upload queue): \(routine.id)")
    }
    
    func get(id: String) async throws -> Routine? {
        // Always read from local (offline-first)
        return try await localRepo.get(id: id)
    }
    
    func update(_ routine: Routine) async throws {
        // Always write to local first (offline-first)
        // The local repo will mark it as unsynced
        try await localRepo.update(routine)
        
        // Upload queue will be processed separately
        AppLogger.database.info("Updated routine locally (will sync via upload queue): \(routine.id)")
    }
    
    func getAll(familyId: String, includeDeleted: Bool) async throws -> [Routine] {
        // Always read from local (offline-first)
        return try await localRepo.getAll(familyId: familyId, includeDeleted: includeDeleted)
    }
    
    func softDelete(id: String) async throws {
        // Always write to local first (offline-first)
        // The local repo will mark it as unsynced
        try await localRepo.softDelete(id: id)
        
        // Upload queue will be processed separately
        AppLogger.database.info("Soft deleted routine locally (will sync via upload queue): \(id)")
    }
    
    // MARK: - Upload Queue Methods
    
    /// Upload all unsynced routines for a family
    /// Returns the number of successfully uploaded routines
    func uploadUnsynced(familyId: String) async throws -> Int {
        return try await uploadQueue.uploadUnsyncedRoutines(familyId: familyId)
    }
    
    /// Get count of unsynced routines for a family
    func getUnsyncedCount(familyId: String) async throws -> Int {
        return try await uploadQueue.getUnsyncedCount(familyId: familyId)
    }
}
