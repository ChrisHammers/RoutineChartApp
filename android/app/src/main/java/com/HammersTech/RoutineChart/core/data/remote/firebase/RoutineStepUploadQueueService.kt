package com.HammersTech.RoutineChart.core.data.remote.firebase

import com.HammersTech.RoutineChart.core.data.local.repositories.RoomRoutineStepRepository
import com.HammersTech.RoutineChart.core.utils.AppLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing the upload queue of unsynced routine steps
 * Phase 3.4: Upload Queue (RoutineSteps)
 */
@Singleton
class RoutineStepUploadQueueService
    @Inject
    constructor(
        private val localRepo: RoomRoutineStepRepository,
        private val syncService: FirestoreRoutineStepSyncService,
    ) {
        /**
         * Upload all unsynced routine steps for a given routine
         * Returns the number of successfully uploaded steps
         */
        suspend fun uploadUnsyncedRoutineSteps(routineId: String): Int {
            AppLogger.Database.info("🔄 Starting upload of unsynced steps for routine: $routineId")

            val unsyncedSteps = localRepo.getUnsynced(routineId)

            if (unsyncedSteps.isEmpty()) {
                AppLogger.Database.info("✅ No unsynced steps to upload for routine: $routineId")
                return 0
            }

            AppLogger.Database.info("📤 Found ${unsyncedSteps.size} unsynced step(s) to upload for routine: $routineId")

            var successCount = 0
            val failedStepIds = mutableListOf<String>()

            unsyncedSteps.forEach { step ->
                try {
                    syncService.syncToFirestore(step)
                    successCount++
                    AppLogger.Database.info("✅ Uploaded step: ${step.id}")
                } catch (e: Exception) {
                    failedStepIds.add(step.id)
                    AppLogger.Database.error("❌ Failed to upload step ${step.id}: ${e.message}", e)
                }
            }

            if (successCount > 0) {
                val syncedIds =
                    unsyncedSteps
                        .filter { !failedStepIds.contains(it.id) }
                        .map { it.id }

                localRepo.markAsSynced(syncedIds)
                AppLogger.Database.info("✅ Marked $successCount step(s) as synced for routine: $routineId")
            }

            if (failedStepIds.isNotEmpty()) {
                AppLogger.Database.error("⚠️ ${failedStepIds.size} step(s) failed to upload and will be retried later")
            }

            return successCount
        }

        /**
         * Get count of unsynced routine steps for a routine
         */
        suspend fun getUnsyncedCount(routineId: String): Int {
            val unsynced = localRepo.getUnsynced(routineId)
            return unsynced.size
        }
    }
