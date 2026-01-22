//
//  RoutineStepUploadQueueService.swift
//  RoutineChart
//
//  Created for Phase 3.4: Sync RoutineSteps
//  Service for uploading unsynced steps to Firestore
//

import Foundation
import OSLog

/// Service for managing the upload queue of unsynced steps
final class RoutineStepUploadQueueService {
    private let localRepo: SQLiteRoutineStepRepository
    private let syncService: FirestoreRoutineStepSyncService
    
    init(
        localRepo: SQLiteRoutineStepRepository = SQLiteRoutineStepRepository(),
        syncService: FirestoreRoutineStepSyncService = FirestoreRoutineStepSyncService()
    ) {
        self.localRepo = localRepo
        self.syncService = syncService
    }
    
    /// Upload all unsynced steps for a routine
    /// Returns the number of successfully uploaded steps
    func uploadUnsyncedSteps(routineId: String) async throws -> Int {
        AppLogger.database.info("🔄 Starting upload of unsynced steps for routine: \(routineId)")
        
        // Get all unsynced steps for this routine
        let unsyncedSteps = try await localRepo.getUnsynced(routineId: routineId)
        
        guard !unsyncedSteps.isEmpty else {
            AppLogger.database.info("✅ No unsynced steps to upload for routine: \(routineId)")
            return 0
        }
        
        AppLogger.database.info("📤 Found \(unsyncedSteps.count) unsynced step(s) to upload for routine: \(routineId)")
        
        var successCount = 0
        var failedStepIds: [String] = []
        
        // Upload each step
        for step in unsyncedSteps {
            do {
                try await syncService.syncToFirestore(step)
                successCount += 1
                AppLogger.database.info("✅ Uploaded step: \(step.id) (routine: \(step.routineId))")
            } catch {
                failedStepIds.append(step.id)
                AppLogger.database.error("❌ Failed to upload step \(step.id): \(error.localizedDescription)")
                // Continue with other steps even if one fails
            }
        }
        
        // Mark successfully uploaded steps as synced
        if successCount > 0 {
            let syncedIds = unsyncedSteps
                .filter { !failedStepIds.contains($0.id) }
                .map { $0.id }
            
            try await localRepo.markAsSynced(stepIds: syncedIds)
            AppLogger.database.info("✅ Marked \(successCount) step(s) as synced")
        }
        
        if !failedStepIds.isEmpty {
            AppLogger.database.warning("⚠️ \(failedStepIds.count) step(s) failed to upload and will be retried later")
        }
        
        return successCount
    }
    
    /// Get count of unsynced steps for a routine
    func getUnsyncedCount(routineId: String) async throws -> Int {
        let unsynced = try await localRepo.getUnsynced(routineId: routineId)
        return unsynced.count
    }
}
