//
//  AssignmentUploadQueueService.swift
//  RoutineChart
//
//  Phase 3.5: Upload queue for unsynced RoutineAssignments
//

import Foundation
import OSLog

/// Service for uploading unsynced assignments to Firestore
final class AssignmentUploadQueueService {
    private let localRepo: SQLiteRoutineAssignmentRepository
    private let syncService: FirestoreAssignmentSyncService
    
    init(
        localRepo: SQLiteRoutineAssignmentRepository = SQLiteRoutineAssignmentRepository(),
        syncService: FirestoreAssignmentSyncService = FirestoreAssignmentSyncService()
    ) {
        self.localRepo = localRepo
        self.syncService = syncService
    }
    
    /// Upload all unsynced assignments for a family
    /// Returns the number of successfully uploaded assignments
    func uploadUnsyncedAssignments(familyId: String) async throws -> Int {
        AppLogger.database.info("🔄 Starting upload of unsynced assignments for familyId: \(familyId)")
        
        let unsynced = try await localRepo.getUnsynced(familyId: familyId)
        
        guard !unsynced.isEmpty else {
            AppLogger.database.info("✅ No unsynced assignments to upload for familyId: \(familyId)")
            return 0
        }
        
        AppLogger.database.info("📤 Found \(unsynced.count) unsynced assignment(s) to upload")
        
        var successCount = 0
        var failedIds: [String] = []
        
        for assignment in unsynced {
            do {
                try await syncService.syncToFirestore(assignment)
                successCount += 1
                AppLogger.database.info("✅ Uploaded assignment: \(assignment.id)")
            } catch {
                failedIds.append(assignment.id)
                AppLogger.database.error("❌ Failed to upload assignment \(assignment.id): \(error.localizedDescription)")
            }
        }
        
        if successCount > 0 {
            let syncedIds = unsynced.map(\.id).filter { !failedIds.contains($0) }
            try await localRepo.markAsSynced(assignmentIds: syncedIds)
            AppLogger.database.info("✅ Marked \(successCount) assignment(s) as synced")
        }
        
        return successCount
    }
    
    /// Get count of unsynced assignments for a family
    func getUnsyncedCount(familyId: String) async throws -> Int {
        let unsynced = try await localRepo.getUnsynced(familyId: familyId)
        return unsynced.count
    }
}
