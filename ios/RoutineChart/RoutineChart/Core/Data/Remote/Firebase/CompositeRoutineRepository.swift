//
//  CompositeRoutineRepository.swift
//  RoutineChart
//
//  Created for Phase 3.2: Upload Queue (Routines)
//  Phase 3.3: Pull Cursor (Routines)
//  Composite repository: SQLite (source of truth) + Firestore sync via upload queue and pull cursor
//

import Foundation
import OSLog
import GRDB

/// Composite repository that uses SQLite as source of truth and syncs to Firestore via upload queue and pull cursor
final class CompositeRoutineRepository: RoutineRepository {
    private let localRepo: SQLiteRoutineRepository
    private let uploadQueue: RoutineUploadQueueService
    private let syncService: FirestoreRoutineSyncService
    private let cursorManager: SyncCursorManager
    private let stepSyncService: FirestoreRoutineStepSyncService
    private let stepRepo: SQLiteRoutineStepRepository
    
    init(
        localRepo: SQLiteRoutineRepository = SQLiteRoutineRepository(),
        uploadQueue: RoutineUploadQueueService = RoutineUploadQueueService(),
        syncService: FirestoreRoutineSyncService = FirestoreRoutineSyncService(),
        cursorManager: SyncCursorManager = SyncCursorManager(),
        stepSyncService: FirestoreRoutineStepSyncService = FirestoreRoutineStepSyncService(),
        stepRepo: SQLiteRoutineStepRepository = SQLiteRoutineStepRepository()
    ) {
        self.localRepo = localRepo
        self.uploadQueue = uploadQueue
        self.syncService = syncService
        self.cursorManager = cursorManager
        self.stepSyncService = stepSyncService
        self.stepRepo = stepRepo
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
    
    // MARK: - Pull Cursor Methods (Phase 3.3)
    
    /// Pull routines from Firestore that were updated since the last sync
    /// Applies merge logic (last-write-wins) and updates the sync cursor
    /// - Parameters:
    ///   - userId: The user ID to filter routines (required)
    ///   - familyId: Optional family ID to filter routines
    /// - Returns: The number of routines pulled and merged
    func pullRoutines(userId: String, familyId: String?) async throws -> Int {
        AppLogger.database.info("🔄 Starting pull of routines from Firestore for userId: \(userId), familyId: \(familyId ?? "nil")")
        
        // Get the sync cursor for "routines" collection
        // If no cursor exists, use a very old date to pull all routines
        let cursor = try await cursorManager.getCursor(collection: "routines")
        let lastSyncedAt = cursor?.lastSyncedAt ?? Date(timeIntervalSince1970: 0)
        
        AppLogger.database.info("📥 Last sync timestamp: \(lastSyncedAt)")
        
        // Query Firestore for routines updated since the cursor
        let remoteRoutines = try await syncService.getRoutinesUpdatedSince(
            userId: userId,
            familyId: familyId,
            since: lastSyncedAt
        )
        
        guard !remoteRoutines.isEmpty else {
            AppLogger.database.info("✅ No new routines to pull from Firestore")
            // Still update cursor to current time to avoid re-querying the same data
            try await cursorManager.updateCursor(collection: "routines", lastSyncedAt: Date())
            return 0
        }
        
        AppLogger.database.info("📥 Found \(remoteRoutines.count) routine(s) to pull from Firestore")
        
        var mergedCount = 0
        var skippedCount = 0
        
        // Apply merge logic (last-write-wins) for each remote routine
        var routinesToPullSteps: [Routine] = [] // Track routines that need steps pulled
        
        for remoteRoutine in remoteRoutines {
            do {
                // Check if routine exists locally
                if let localRoutine = try await localRepo.get(id: remoteRoutine.id) {
                    // Merge: last-write-wins based on updatedAt
                    if remoteRoutine.updatedAt > localRoutine.updatedAt {
                        // Remote is newer - overwrite local
                        // Save to local and mark as synced (it came from Firestore)
                        try await saveRoutineFromFirestore(remoteRoutine)
                        routinesToPullSteps.append(remoteRoutine) // Pull steps for this routine
                        mergedCount += 1
                        AppLogger.database.info("✅ Merged routine (remote wins): \(remoteRoutine.id) - remote: \(remoteRoutine.updatedAt), local: \(localRoutine.updatedAt)")
                    } else {
                        // Local is newer or same - skip (local changes will be uploaded via upload queue)
                        skippedCount += 1
                        AppLogger.database.info("⏭️ Skipped routine (local wins): \(remoteRoutine.id) - remote: \(remoteRoutine.updatedAt), local: \(localRoutine.updatedAt)")
                    }
                } else {
                    // Routine doesn't exist locally - insert it
                    // Save to local and mark as synced (it came from Firestore)
                    try await saveRoutineFromFirestore(remoteRoutine)
                    routinesToPullSteps.append(remoteRoutine) // Pull steps for this routine
                    mergedCount += 1
                    AppLogger.database.info("✅ Inserted new routine from Firestore: \(remoteRoutine.id)")
                }
            } catch {
                AppLogger.database.error("❌ Failed to merge routine \(remoteRoutine.id): \(error.localizedDescription)")
                // Continue with other routines even if one fails
            }
        }
        
        // Phase 3.4: Pull steps for routines that were merged
        if !routinesToPullSteps.isEmpty {
            AppLogger.database.info("📥 Pulling steps for \(routinesToPullSteps.count) routine(s)")
            var stepsPulledCount = 0
            
            for routine in routinesToPullSteps {
                do {
                    let pulled = try await pullStepsForRoutine(routineId: routine.id)
                    stepsPulledCount += pulled
                } catch {
                    AppLogger.database.error("❌ Failed to pull steps for routine \(routine.id): \(error.localizedDescription)")
                    // Continue with other routines even if steps fail
                }
            }
            
            if stepsPulledCount > 0 {
                AppLogger.database.info("✅ Pulled \(stepsPulledCount) step(s) for \(routinesToPullSteps.count) routine(s)")
            }
        }
        
        // Update sync cursor to current timestamp after successful pull
        try await cursorManager.updateCursor(collection: "routines", lastSyncedAt: Date())
        
        AppLogger.database.info("✅ Pull complete: merged \(mergedCount) routine(s), skipped \(skippedCount) routine(s)")
        
        return mergedCount
    }
    
    /// Save a routine from Firestore to local database and mark it as synced
    /// This is used when pulling routines from Firestore (they're already synced)
    private func saveRoutineFromFirestore(_ routine: Routine) async throws {
        let db = try SQLiteManager.shared.database()
        try await db.write { db in
            var mutableRoutine = routine
            // Insert or update the routine
            if try Routine.exists(db, key: routine.id) {
                try mutableRoutine.update(db)
            } else {
                try mutableRoutine.insert(db)
            }
            // Mark as synced (it came from Firestore, so it's already synced)
            try db.execute(sql: "UPDATE routines SET synced = 1 WHERE id = ?", arguments: [routine.id])
        }
    }
    
    // MARK: - Step Pulling (Phase 3.4)
    
    /// Pull all steps for a routine from Firestore and merge with local steps
    /// Returns the number of steps pulled/merged
    private func pullStepsForRoutine(routineId: String) async throws -> Int {
        AppLogger.database.info("📥 Pulling steps for routine: \(routineId)")
        
        // Get all steps from Firestore
        let remoteSteps = try await stepSyncService.getAllSteps(routineId: routineId)
        
        guard !remoteSteps.isEmpty else {
            AppLogger.database.info("✅ No steps found in Firestore for routine: \(routineId)")
            return 0
        }
        
        AppLogger.database.info("📥 Found \(remoteSteps.count) step(s) in Firestore for routine: \(routineId)")
        
        // Get local steps for this routine
        let localSteps = try await stepRepo.getAll(routineId: routineId)
        let localStepIds = Set(localSteps.map { $0.id })
        
        var mergedCount = 0
        var createdCount = 0
        var updatedCount = 0
        
        // Merge each remote step
        for remoteStep in remoteSteps {
            do {
                if let localStep = localSteps.first(where: { $0.id == remoteStep.id }) {
                    // Step exists locally - apply merge logic
                    // For steps, we use createdAt for comparison (steps don't have updatedAt)
                    // If remote was created after local, or if local has unsynced changes, we need to decide
                    // Simple approach: if local step is unsynced, keep local; otherwise use remote
                    
                    // Check if local step is unsynced (has local changes)
                    let db = try SQLiteManager.shared.database()
                    let isUnsynced = try await db.read { db in
                        let row = try Row.fetchOne(
                            db,
                            sql: "SELECT synced FROM routine_steps WHERE id = ?",
                            arguments: [localStep.id]
                        )
                        guard let row = row else { return false }
                        let synced = (row["synced"] as Int64) == 1
                        return !synced
                    }
                    
                    if isUnsynced {
                        // Local step has unsynced changes - keep local (it will upload)
                        AppLogger.database.info("⏭️ Skipped step (local has unsynced changes): \(remoteStep.id)")
                    } else {
                        // Local step is synced - use remote (last-write-wins based on createdAt)
                        if remoteStep.createdAt >= localStep.createdAt {
                            // Remote is newer or same - use remote
                            try await saveStepFromFirestore(remoteStep)
                            updatedCount += 1
                            AppLogger.database.info("✅ Updated step from Firestore: \(remoteStep.id)")
                        } else {
                            // Local is newer - keep local
                            AppLogger.database.info("⏭️ Skipped step (local is newer): \(remoteStep.id)")
                        }
                    }
                } else {
                    // Step doesn't exist locally - insert it
                    try await saveStepFromFirestore(remoteStep)
                    createdCount += 1
                    AppLogger.database.info("✅ Inserted new step from Firestore: \(remoteStep.id)")
                }
                mergedCount += 1
            } catch {
                AppLogger.database.error("❌ Failed to merge step \(remoteStep.id): \(error.localizedDescription)")
                // Continue with other steps even if one fails
            }
        }
        
        // Handle soft-deleted steps: if a local step is not in remote steps, check if it should be soft-deleted
        // Note: We don't automatically soft-delete local steps that aren't in remote, because:
        // 1. Steps might be deleted on remote (soft delete with deletedAt)
        // 2. Steps might not have been uploaded yet
        // 3. We rely on deletedAt field from Firestore to handle deletions
        
        // Soft delete steps that are marked as deleted in Firestore
        for remoteStep in remoteSteps where remoteStep.deletedAt != nil {
            if let localStep = localSteps.first(where: { $0.id == remoteStep.id }),
               localStep.deletedAt == nil {
                // Remote step is deleted, local is not - soft delete local
                try await stepRepo.softDelete(id: remoteStep.id)
                AppLogger.database.info("✅ Soft deleted step from Firestore: \(remoteStep.id)")
            }
        }
        
        AppLogger.database.info("✅ Pulled steps for routine \(routineId): created \(createdCount), updated \(updatedCount), total \(mergedCount)")
        
        return mergedCount
    }
    
    /// Save a step from Firestore to local database and mark it as synced
    /// This is used when pulling steps from Firestore (they're already synced)
    private func saveStepFromFirestore(_ step: RoutineStep) async throws {
        let db = try SQLiteManager.shared.database()
        try await db.write { db in
            var mutableStep = step
            // Insert or update the step
            if try RoutineStep.exists(db, key: step.id) {
                try mutableStep.update(db)
            } else {
                try mutableStep.insert(db)
            }
            // Mark as synced (it came from Firestore, so it's already synced)
            try db.execute(sql: "UPDATE routine_steps SET synced = 1 WHERE id = ?", arguments: [step.id])
        }
    }
}
