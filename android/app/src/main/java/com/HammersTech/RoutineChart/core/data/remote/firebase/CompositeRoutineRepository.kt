package com.HammersTech.RoutineChart.core.data.remote.firebase

import com.HammersTech.RoutineChart.core.data.local.repositories.RoomRoutineRepository
import com.HammersTech.RoutineChart.core.data.local.repositories.RoomRoutineStepRepository
import com.HammersTech.RoutineChart.core.data.local.room.dao.RoutineDao
import com.HammersTech.RoutineChart.core.data.local.room.dao.RoutineStepDao
import com.HammersTech.RoutineChart.core.data.local.room.entities.RoutineEntity
import com.HammersTech.RoutineChart.core.data.local.room.entities.RoutineStepEntity
import com.HammersTech.RoutineChart.core.domain.models.Routine
import com.HammersTech.RoutineChart.core.domain.models.RoutineStep
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineRepository
import com.HammersTech.RoutineChart.core.utils.AppLogger
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Composite repository that uses Room as source of truth and syncs to Firestore
 * Phase 3.2: Upload Queue (Routines)
 * Phase 3.3: Pull Cursor (Routines)
 */
@Singleton
class CompositeRoutineRepository
    @Inject
    constructor(
        private val localRepo: RoomRoutineRepository,
        private val uploadQueue: RoutineUploadQueueService,
        private val syncService: FirestoreRoutineSyncService,
        private val cursorManager: SyncCursorManager,
        private val routineDao: RoutineDao,
        private val stepSyncService: FirestoreRoutineStepSyncService,
        private val stepRepo: RoomRoutineStepRepository,
        private val stepDao: RoutineStepDao,
    ) : RoutineRepository {
        // MARK: - Repository Methods (Local-First)

        override suspend fun create(routine: Routine) {
            // Always write to local first (offline-first)
            // The local repo will mark it as unsynced
            localRepo.create(routine)

            // Upload queue will be processed separately
            AppLogger.Database.info("Created routine locally (will sync via upload queue): ${routine.id}")
        }

        override suspend fun getById(id: String): Routine? {
            // Always read from local (offline-first)
            return localRepo.getById(id)
        }

        override fun observeById(id: String): Flow<Routine?> {
            // Always read from local (offline-first)
            return localRepo.observeById(id)
        }

        override suspend fun update(routine: Routine) {
            // Always write to local first (offline-first)
            // The local repo will mark it as unsynced
            localRepo.update(routine)

            // Upload queue will be processed separately
            AppLogger.Database.info("Updated routine locally (will sync via upload queue): ${routine.id}")
        }

        override suspend fun getAll(
            userId: String,
            familyId: String?,
            includeDeleted: Boolean,
        ): List<Routine> {
            // Always read from local (offline-first)
            return localRepo.getAll(userId, familyId, includeDeleted)
        }

        override fun observeByFamilyId(familyId: String): Flow<List<Routine>> {
            // Always read from local (offline-first)
            return localRepo.observeByFamilyId(familyId)
        }

        // MARK: - Upload Queue Methods

        /**
         * Upload all unsynced routines for a user or family
         * Returns the number of successfully uploaded routines
         */
        suspend fun uploadUnsynced(
            userId: String,
            familyId: String?,
        ): Int {
            return uploadQueue.uploadUnsyncedRoutines(userId, familyId)
        }

        /**
         * Get count of unsynced routines for a user or family
         */
        suspend fun getUnsyncedCount(
            userId: String,
            familyId: String?,
        ): Int {
            return uploadQueue.getUnsyncedCount(userId, familyId)
        }

        // MARK: - Pull Cursor Methods (Phase 3.3)

        /**
         * Pull routines from Firestore that have been updated since the last sync.
         * Applies last-write-wins merge logic.
         * Returns the number of routines merged.
         */
        suspend fun pullRoutines(
            userId: String,
            familyId: String?,
        ): Int {
            AppLogger.Database.info("🔄 Starting pull of routines from Firestore for userId: $userId, familyId: ${familyId ?: "nil"}")

            // 1. Get the last sync timestamp for routines
            val cursorCollection = "routines"
            val cursor = cursorManager.getCursor(cursorCollection)
            val lastSyncedAt = cursor?.lastSyncedAt ?: Instant.ofEpochSecond(0)

            AppLogger.Database.info("📥 Last sync timestamp: $lastSyncedAt")
            AppLogger.Database.info("📥 Current timestamp: ${Instant.now()}")
            AppLogger.Database.info("📥 Time difference: ${java.time.Duration.between(lastSyncedAt, Instant.now()).seconds} seconds")

            // 2. Query Firestore for routines updated since lastSyncedAt
            var remoteRoutines =
                try {
                    syncService.getRoutinesUpdatedSince(userId, familyId, lastSyncedAt)
                } catch (e: Exception) {
                    AppLogger.Database.error("❌ Failed to query Firestore: ${e.message}", e)
                    emptyList()
                }

            // If no routines found but cursor exists, check if local database is empty
            // This handles the case where routines exist in Firestore but have older updatedAt timestamps
            if (remoteRoutines.isEmpty() && cursor != null) {
                // Check if local database has any routines for this user/family
                val localRoutines = localRepo.getAll(userId, familyId, includeDeleted = false)
                AppLogger.Database.info("🔍 No routines found with updatedAt filter. Local database has ${localRoutines.size} routine(s)")

                // If local is empty, try pulling ALL routines (ignore updatedAt filter)
                if (localRoutines.isEmpty()) {
                    AppLogger.Database.info("🔄 Local database is empty - pulling ALL routines from Firestore (ignoring updatedAt filter)")
                    remoteRoutines =
                        try {
                            syncService.getRoutinesUpdatedSince(
                                userId,
                                familyId,
                                Instant.ofEpochSecond(0), // Use epoch to get all routines
                            )
                        } catch (e: Exception) {
                            AppLogger.Database.error("❌ Failed to pull all routines: ${e.message}", e)
                            emptyList()
                        }
                    AppLogger.Database.info("📥 Found ${remoteRoutines.size} routine(s) when pulling all routines")
                } else {
                    // Local has routines but pull found none - this might mean routines in Firestore have older updatedAt
                    // Try pulling with epoch anyway to be safe
                    AppLogger.Database.info("🔄 Local has routines but pull found none - trying to pull ALL routines anyway")
                    remoteRoutines =
                        try {
                            syncService.getRoutinesUpdatedSince(
                                userId,
                                familyId,
                                Instant.ofEpochSecond(0), // Use epoch to get all routines
                            )
                        } catch (e: Exception) {
                            AppLogger.Database.error("❌ Failed to pull all routines: ${e.message}", e)
                            emptyList()
                        }
                    AppLogger.Database.info("📥 Found ${remoteRoutines.size} routine(s) when pulling all routines (fallback)")
                }
            }

            if (remoteRoutines.isEmpty()) {
                AppLogger.Database.info("✅ No new routines to pull from Firestore")
                // Still update cursor to current time to avoid re-querying the same data
                cursorManager.updateCursor(cursorCollection, Instant.now())
                return 0
            }

            AppLogger.Database.info("📥 Found ${remoteRoutines.size} routine(s) to pull from Firestore")

            var mergedCount = 0
            var skippedCount = 0
            val routinesToPullSteps = mutableListOf<Routine>() // Track routines that need steps pulled

            // Apply merge logic (last-write-wins) for each remote routine
            remoteRoutines.forEach { remoteRoutine ->
                try {
                    // Check if routine exists locally
                    val localRoutine = localRepo.getById(remoteRoutine.id)

                    if (localRoutine != null) {
                        // Merge: last-write-wins based on updatedAt
                        if (remoteRoutine.updatedAt.isAfter(localRoutine.updatedAt)) {
                            // Remote is newer - overwrite local
                            saveRoutineFromFirestore(remoteRoutine)
                            routinesToPullSteps.add(remoteRoutine) // Pull steps for this routine
                            mergedCount++
                            AppLogger.Database.info(
                                "✅ Merged routine (remote wins): ${remoteRoutine.id} - remote: ${remoteRoutine.updatedAt}, local: ${localRoutine.updatedAt}",
                            )
                        } else {
                            // Local is newer or same - skip (local changes will be uploaded via upload queue)
                            skippedCount++
                            AppLogger.Database.info(
                                "⏭️ Skipped routine (local wins): ${remoteRoutine.id} - remote: ${remoteRoutine.updatedAt}, local: ${localRoutine.updatedAt}",
                            )
                        }
                    } else {
                        // Routine doesn't exist locally - insert it
                        saveRoutineFromFirestore(remoteRoutine)
                        routinesToPullSteps.add(remoteRoutine) // Pull steps for this routine
                        mergedCount++
                        AppLogger.Database.info("✅ Inserted new routine from Firestore: ${remoteRoutine.id}")
                    }
                } catch (e: Exception) {
                    AppLogger.Database.error("❌ Failed to merge routine ${remoteRoutine.id}: ${e.message}", e)
                    // Continue with other routines even if one fails
                }
            }

            // Phase 3.4: Pull steps for routines that were merged
            if (routinesToPullSteps.isNotEmpty()) {
                AppLogger.Database.info("📥 Pulling steps for ${routinesToPullSteps.size} routine(s)")
                var stepsPulledCount = 0

                routinesToPullSteps.forEach { routine ->
                    try {
                        val pulled = pullStepsForRoutine(routine.id)
                        stepsPulledCount += pulled
                    } catch (e: Exception) {
                        AppLogger.Database.error("❌ Failed to pull steps for routine ${routine.id}: ${e.message}", e)
                        // Continue with other routines even if steps fail
                    }
                }

                if (stepsPulledCount > 0) {
                    AppLogger.Database.info("✅ Pulled $stepsPulledCount step(s) for ${routinesToPullSteps.size} routine(s)")
                }
            }

            // 3. Update the sync cursor to the current time
            cursorManager.updateCursor(cursorCollection, Instant.now())
            AppLogger.Database.info("Updated sync cursor for collection '$cursorCollection' to ${Instant.now()}")

            AppLogger.Database.info("✅ Pull complete: merged $mergedCount routine(s), skipped $skippedCount routine(s)")

            return mergedCount
        }

        /**
         * Save a routine from Firestore to local database and mark it as synced
         * This is used when pulling routines from Firestore (they're already synced)
         */
        private suspend fun saveRoutineFromFirestore(routine: Routine) {
            routineDao.insert(RoutineEntity.fromDomain(routine, synced = true))
            AppLogger.Database.info("Saved routine from Firestore (marked as synced): ${routine.id}")
        }

        // MARK: - Step Pulling (Phase 3.4)

        /**
         * Pull all steps for a routine from Firestore and merge with local steps
         * Returns the number of steps pulled/merged
         */
        private suspend fun pullStepsForRoutine(routineId: String): Int {
            AppLogger.Database.info("📥 Pulling steps for routine: $routineId")

            // Get all steps from Firestore
            val remoteSteps = stepSyncService.getAllSteps(routineId)

            if (remoteSteps.isEmpty()) {
                AppLogger.Database.info("✅ No steps found in Firestore for routine: $routineId")
                return 0
            }

            AppLogger.Database.info("📥 Found ${remoteSteps.size} step(s) in Firestore for routine: $routineId")

            // Get local steps for this routine
            val localSteps = stepRepo.getByRoutineId(routineId)
            val localStepIds = localSteps.map { it.id }.toSet()

            var mergedCount = 0
            var createdCount = 0
            var updatedCount = 0

            // Merge each remote step
            remoteSteps.forEach { remoteStep ->
                try {
                    val localStep = localSteps.firstOrNull { it.id == remoteStep.id }

                    if (localStep != null) {
                        // Step exists locally - apply merge logic
                        // Check if local step is unsynced (has local changes)
                        val localEntity = stepDao.getById(localStep.id)
                        val isUnsynced = localEntity?.synced == false

                        if (isUnsynced) {
                            // Local step has unsynced changes - keep local (it will upload)
                            AppLogger.Database.info("⏭️ Skipped step (local has unsynced changes): ${remoteStep.id}")
                        } else {
                            // Local step is synced - use remote (last-write-wins based on createdAt)
                            if (remoteStep.createdAt.isAfter(localStep.createdAt) || remoteStep.createdAt == localStep.createdAt) {
                                // Remote is newer or same - use remote
                                saveStepFromFirestore(remoteStep)
                                updatedCount++
                                AppLogger.Database.info("✅ Updated step from Firestore: ${remoteStep.id}")
                            } else {
                                // Local is newer - keep local
                                AppLogger.Database.info("⏭️ Skipped step (local is newer): ${remoteStep.id}")
                            }
                        }
                    } else {
                        // Step doesn't exist locally - insert it
                        saveStepFromFirestore(remoteStep)
                        createdCount++
                        AppLogger.Database.info("✅ Inserted new step from Firestore: ${remoteStep.id}")
                    }
                    mergedCount++
                } catch (e: Exception) {
                    AppLogger.Database.error("❌ Failed to merge step ${remoteStep.id}: ${e.message}", e)
                    // Continue with other steps even if one fails
                }
            }

            // Handle soft-deleted steps: if a remote step is deleted, soft delete local
            remoteSteps.forEach { remoteStep ->
                if (remoteStep.deletedAt != null) {
                    val localStep = localSteps.firstOrNull { it.id == remoteStep.id }
                    if (localStep != null && localStep.deletedAt == null) {
                        // Remote step is deleted, local is not - soft delete local
                        stepRepo.update(remoteStep) // Update will mark as unsynced, but that's okay
                        AppLogger.Database.info("✅ Soft deleted step from Firestore: ${remoteStep.id}")
                    }
                }
            }

            AppLogger.Database.info(
                "✅ Pulled steps for routine $routineId: created $createdCount, updated $updatedCount, total $mergedCount",
            )

            return mergedCount
        }

        /**
         * Save a step from Firestore to local database and mark it as synced
         * This is used when pulling steps from Firestore (they're already synced)
         */
        private suspend fun saveStepFromFirestore(step: RoutineStep) {
            stepDao.insert(RoutineStepEntity.fromDomain(step, synced = true))
            AppLogger.Database.info("Saved step from Firestore (marked as synced): ${step.id}")
        }
    }
