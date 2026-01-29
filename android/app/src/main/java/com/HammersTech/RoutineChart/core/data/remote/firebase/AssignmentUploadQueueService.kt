package com.HammersTech.RoutineChart.core.data.remote.firebase

import com.HammersTech.RoutineChart.core.data.local.repositories.RoomRoutineAssignmentRepository
import com.HammersTech.RoutineChart.core.utils.AppLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for uploading unsynced RoutineAssignments to Firestore
 * Phase 3.5: Sync RoutineAssignments
 */
@Singleton
class AssignmentUploadQueueService @Inject constructor(
    private val localRepo: RoomRoutineAssignmentRepository,
    private val syncService: FirestoreAssignmentSyncService
) {

    /**
     * Upload all unsynced assignments for a family.
     * Returns the number of successfully uploaded assignments.
     */
    suspend fun uploadUnsyncedAssignments(familyId: String): Int {
        AppLogger.Database.info("🔄 Starting upload of unsynced assignments for familyId: $familyId")

        val unsynced = localRepo.getUnsynced(familyId)
        if (unsynced.isEmpty()) {
            AppLogger.Database.info("✅ No unsynced assignments to upload for familyId: $familyId")
            return 0
        }

        AppLogger.Database.info("📤 Found ${unsynced.size} unsynced assignment(s) to upload")

        var successCount = 0
        val failedIds = mutableListOf<String>()

        unsynced.forEach { assignment ->
            try {
                syncService.syncToFirestore(assignment)
                successCount++
                AppLogger.Database.info("✅ Uploaded assignment: ${assignment.id}")
            } catch (e: Exception) {
                failedIds.add(assignment.id)
                AppLogger.Database.error("❌ Failed to upload assignment ${assignment.id}: ${e.message}", e)
            }
        }

        if (successCount > 0) {
            val syncedIds = unsynced.map { it.id }.filter { it !in failedIds }
            localRepo.markAsSynced(syncedIds)
            AppLogger.Database.info("✅ Marked $successCount assignment(s) as synced")
        }

        return successCount
    }
}
