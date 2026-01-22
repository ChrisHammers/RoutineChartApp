//
//  RoutineUploadQueueService.swift
//  RoutineChart
//
//  Created for Phase 3.2: Upload Queue (Routines)
//  Service for uploading unsynced routines to Firestore
//

import Foundation
import GRDB
import OSLog

/// Service for managing the upload queue of unsynced routines
final class RoutineUploadQueueService {
    private let localRepo: SQLiteRoutineRepository
    private let syncService: FirestoreRoutineSyncService
    private let stepUploadQueue: RoutineStepUploadQueueService
    
    init(
        localRepo: SQLiteRoutineRepository = SQLiteRoutineRepository(),
        syncService: FirestoreRoutineSyncService = FirestoreRoutineSyncService(),
        stepUploadQueue: RoutineStepUploadQueueService = RoutineStepUploadQueueService()
    ) {
        self.localRepo = localRepo
        self.syncService = syncService
        self.stepUploadQueue = stepUploadQueue
    }
    
    /// Upload all unsynced routines for a family
    /// Returns the number of successfully uploaded routines
    func uploadUnsyncedRoutines(familyId: String) async throws -> Int {
        AppLogger.database.info("🔄 Starting upload of unsynced routines for family: \(familyId)")
        
        // Get all unsynced routines
        let unsyncedRoutines = try await localRepo.getUnsynced(familyId: familyId)
        
        guard !unsyncedRoutines.isEmpty else {
            AppLogger.database.info("✅ No unsynced routines to upload for family: \(familyId)")
            
            // Debug: Check total routines for this family
            let allRoutines = try await localRepo.getAll(familyId: familyId, includeDeleted: false)
            AppLogger.database.info("🔍 Debug: Total routines for family \(familyId): \(allRoutines.count)")
            for routine in allRoutines {
                let familyIdStr = routine.familyId ?? "nil"
                AppLogger.database.info("   - Routine: \(routine.id), title: \(routine.title), familyId: \(familyIdStr)")
            }
            
            // Debug: Check ALL routines in database (regardless of familyId) to see if there's a mismatch
            let db = try SQLiteManager.shared.database()
            let allRoutinesInDb = try await db.read { db in
                try Routine.fetchAll(db)
            }
            AppLogger.database.info("🔍 Debug: Total routines in entire database: \(allRoutinesInDb.count)")
            for routine in allRoutinesInDb {
                let familyIdStr = routine.familyId ?? "nil"
                AppLogger.database.info("   - Routine: \(routine.id), title: \(routine.title), familyId: \(familyIdStr)")
            }
            
            // Debug: Check synced status of all routines
            try await db.read { db in
                let rows = try Row.fetchAll(
                    db,
                    sql: "SELECT id, title, familyId, synced FROM routines"
                )
                AppLogger.database.info("🔍 Debug: Synced status of all routines:")
                for row in rows {
                    let id = row["id"] as String
                    let title = row["title"] as String
                    let routineFamilyId = row["familyId"] as String? // familyId is now nullable
                    let synced = (row["synced"] as Int64) == 1
                    AppLogger.database.info("   - \(id): '\(title)', familyId: \(routineFamilyId ?? "nil"), synced: \(synced)")
                }
            }
            
            return 0
        }
        
        AppLogger.database.info("📤 Found \(unsyncedRoutines.count) unsynced routine(s) to upload")
        for routine in unsyncedRoutines {
            AppLogger.database.info("   - Will upload: \(routine.id) - \(routine.title)")
        }
        
        var successCount = 0
        var failedRoutineIds: [String] = []
        
        // Upload each routine and its steps
        for routine in unsyncedRoutines {
            do {
                // First, upload the routine
                try await syncService.syncToFirestore(routine)
                successCount += 1
                AppLogger.database.info("✅ Uploaded routine: \(routine.id)")
                
                // Then, upload all unsynced steps for this routine
                do {
                    let uploadedSteps = try await stepUploadQueue.uploadUnsyncedSteps(routineId: routine.id)
                    if uploadedSteps > 0 {
                        AppLogger.database.info("✅ Uploaded \(uploadedSteps) step(s) for routine: \(routine.id)")
                    }
                } catch {
                    AppLogger.database.error("❌ Failed to upload steps for routine \(routine.id): \(error.localizedDescription)")
                    // Don't fail the routine upload if steps fail - steps will be retried later
                }
            } catch {
                failedRoutineIds.append(routine.id)
                AppLogger.database.error("❌ Failed to upload routine \(routine.id): \(error.localizedDescription)")
                // Continue with other routines even if one fails
            }
        }
        
        // Mark successfully uploaded routines as synced
        if successCount > 0 {
            let syncedIds = unsyncedRoutines
                .filter { !failedRoutineIds.contains($0.id) }
                .map { $0.id }
            
            try await localRepo.markAsSynced(routineIds: syncedIds)
            AppLogger.database.info("✅ Marked \(successCount) routine(s) as synced")
        }
        
        if !failedRoutineIds.isEmpty {
            AppLogger.database.warning("⚠️ \(failedRoutineIds.count) routine(s) failed to upload and will be retried later")
        }
        
        return successCount
    }
    
    /// Get count of unsynced routines for a family
    func getUnsyncedCount(familyId: String) async throws -> Int {
        let unsynced = try await localRepo.getUnsynced(familyId: familyId)
        return unsynced.count
    }
}
