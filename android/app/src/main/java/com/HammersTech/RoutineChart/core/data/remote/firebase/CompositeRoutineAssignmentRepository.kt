package com.HammersTech.RoutineChart.core.data.remote.firebase

import com.HammersTech.RoutineChart.core.data.local.repositories.RoomRoutineAssignmentRepository
import com.HammersTech.RoutineChart.core.domain.models.RoutineAssignment
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineAssignmentRepository
import com.HammersTech.RoutineChart.core.utils.AppLogger
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Composite repository that uses Room as source of truth and syncs assignments to Firestore
 * Phase 3.5: Sync RoutineAssignments (Upload Queue + Pull Cursor)
 */
@Singleton
class CompositeRoutineAssignmentRepository @Inject constructor(
    private val localRepo: RoomRoutineAssignmentRepository,
    private val uploadQueue: AssignmentUploadQueueService,
    private val syncService: FirestoreAssignmentSyncService,
    private val cursorManager: SyncCursorManager
) : RoutineAssignmentRepository {

    override suspend fun create(assignment: RoutineAssignment) {
        localRepo.create(assignment)
        AppLogger.Database.info("Created assignment locally (will sync via upload queue): ${assignment.id}")
    }

    override suspend fun update(assignment: RoutineAssignment) {
        localRepo.update(assignment)
        AppLogger.Database.info("Updated assignment locally (will sync via upload queue): ${assignment.id}")
    }

    override suspend fun getById(id: String): RoutineAssignment? =
        localRepo.getById(id)

    override suspend fun getByFamilyId(familyId: String): List<RoutineAssignment> =
        localRepo.getByFamilyId(familyId)

    override suspend fun getActiveByChildId(childId: String): List<RoutineAssignment> =
        localRepo.getActiveByChildId(childId)

    override fun observeActiveByChildId(childId: String): Flow<List<RoutineAssignment>> =
        localRepo.observeActiveByChildId(childId)

    override suspend fun getByRoutineId(routineId: String): List<RoutineAssignment> =
        localRepo.getByRoutineId(routineId)

    override suspend fun softDelete(id: String) {
        localRepo.softDelete(id)
        AppLogger.Database.info("Soft deleted assignment locally (will sync via upload queue): $id")
    }

    /** Upload all unsynced assignments for a family. Returns count uploaded. */
    suspend fun uploadUnsynced(familyId: String): Int =
        uploadQueue.uploadUnsyncedAssignments(familyId)

    /** Pull assignments from Firestore updated since last sync. Last-write-wins by updatedAt. Returns count merged. */
    suspend fun pullAssignments(familyId: String): Int {
        AppLogger.Database.info("🔄 Starting pull of assignments from Firestore for familyId: $familyId")

        val cursorCollection = "routine_assignments"
        val cursor = cursorManager.getCursor(cursorCollection)
        val lastSyncedAt = cursor?.lastSyncedAt ?: Instant.ofEpochSecond(0)

        val remoteAssignments = try {
            syncService.getAssignmentsUpdatedSince(familyId, lastSyncedAt)
        } catch (e: Exception) {
            AppLogger.Database.error("❌ Failed to query Firestore for assignments: ${e.message}", e)
            cursorManager.updateCursor(cursorCollection, Instant.now())
            return 0
        }

        if (remoteAssignments.isEmpty()) {
            AppLogger.Database.info("✅ No new assignments to pull from Firestore")
            cursorManager.updateCursor(cursorCollection, Instant.now())
            return 0
        }

        AppLogger.Database.info("📥 Found ${remoteAssignments.size} assignment(s) to pull from Firestore")

        var mergedCount = 0
        var skippedCount = 0

        remoteAssignments.forEach { remote ->
            try {
                val local = localRepo.getById(remote.id)
                if (local != null) {
                    if (remote.updatedAt.isAfter(local.updatedAt)) {
                        localRepo.saveFromFirestore(remote)
                        mergedCount++
                        AppLogger.Database.info("✅ Merged assignment (remote wins): ${remote.id}")
                    } else {
                        skippedCount++
                        AppLogger.Database.info("⏭️ Skipped assignment (local wins): ${remote.id}")
                    }
                } else {
                    localRepo.saveFromFirestore(remote)
                    mergedCount++
                    AppLogger.Database.info("✅ Inserted new assignment from Firestore: ${remote.id}")
                }
            } catch (e: Exception) {
                AppLogger.Database.error("❌ Failed to merge assignment ${remote.id}: ${e.message}", e)
            }
        }

        cursorManager.updateCursor(cursorCollection, Instant.now())
        AppLogger.Database.info("✅ Pull complete: merged $mergedCount assignment(s), skipped $skippedCount")
        return mergedCount
    }
}
