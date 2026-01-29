package com.HammersTech.RoutineChart.core.data.remote.firebase

import com.HammersTech.RoutineChart.core.data.local.room.dao.RoutineDao
import com.HammersTech.RoutineChart.core.utils.AppLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing the upload queue of unsynced routines
 * Phase 3.2: Upload Queue (Routines)
 */
@Singleton
class RoutineUploadQueueService
    @Inject
    constructor(
        private val routineDao: RoutineDao,
        private val syncService: FirestoreRoutineSyncService,
        private val stepUploadQueue: RoutineStepUploadQueueService,
    ) {
        /**
         * Upload all unsynced routines for a user or family
         * Returns the number of successfully uploaded routines
         */
        suspend fun uploadUnsyncedRoutines(
            userId: String,
            familyId: String?,
        ): Int {
            AppLogger.Database.info("🔄 Starting upload of unsynced routines for userId: $userId, familyId: $familyId")

            // Get all unsynced routines
            val unsyncedEntities = routineDao.getUnsynced(familyId, userId)

            if (unsyncedEntities.isEmpty()) {
                AppLogger.Database.info("✅ No unsynced routines to upload")
                return 0
            }

            AppLogger.Database.info("📤 Found ${unsyncedEntities.size} unsynced routine(s) to upload")
            unsyncedEntities.forEach { entity ->
                AppLogger.Database.info("   - Will upload: ${entity.id} - ${entity.title}")
            }

            var successCount = 0
            val failedRoutineIds = mutableListOf<String>()

            // Upload each routine and its steps
            unsyncedEntities.forEach { entity ->
                try {
                    val routine = entity.toDomain()
                    syncService.syncToFirestore(routine)
                    successCount++
                    AppLogger.Database.info("✅ Uploaded routine: ${entity.id}")

                    // Then, upload all unsynced steps for this routine
                    try {
                        val uploadedSteps = stepUploadQueue.uploadUnsyncedRoutineSteps(routine.id)
                        if (uploadedSteps > 0) {
                            AppLogger.Database.info("✅ Uploaded $uploadedSteps step(s) for routine: ${routine.id}")
                        }
                    } catch (e: Exception) {
                        AppLogger.Database.error("❌ Failed to upload steps for routine ${routine.id}: ${e.message}", e)
                        // Don't fail the routine upload if steps fail - steps will be retried later
                    }
                } catch (e: Exception) {
                    failedRoutineIds.add(entity.id)
                    AppLogger.Database.error("❌ Failed to upload routine ${entity.id}: ${e.message}", e)
                    // Continue with other routines even if one fails
                }
            }

            // Mark successfully uploaded routines as synced
            if (successCount > 0) {
                val syncedIds =
                    unsyncedEntities
                        .filter { !failedRoutineIds.contains(it.id) }
                        .map { it.id }

                routineDao.markAsSynced(syncedIds)
                AppLogger.Database.info("✅ Marked $successCount routine(s) as synced")
            }

            if (failedRoutineIds.isNotEmpty()) {
                AppLogger.Database.error("⚠️ ${failedRoutineIds.size} routine(s) failed to upload and will be retried later")
            }

            return successCount
        }

        /**
         * Get count of unsynced routines for a user or family
         */
        suspend fun getUnsyncedCount(
            userId: String,
            familyId: String?,
        ): Int {
            val unsynced = routineDao.getUnsynced(familyId, userId)
            return unsynced.size
        }
    }
