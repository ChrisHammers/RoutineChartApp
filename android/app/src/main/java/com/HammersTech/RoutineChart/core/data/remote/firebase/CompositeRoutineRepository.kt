package com.HammersTech.RoutineChart.core.data.remote.firebase

import com.HammersTech.RoutineChart.core.data.local.repositories.RoomRoutineRepository
import com.HammersTech.RoutineChart.core.domain.models.Routine
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineRepository
import com.HammersTech.RoutineChart.core.utils.AppLogger
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Composite repository that uses Room as source of truth and syncs to Firestore
 * Phase 3.2: Upload Queue (Routines)
 */
@Singleton
class CompositeRoutineRepository @Inject constructor(
    private val localRepo: RoomRoutineRepository,
    private val uploadQueue: RoutineUploadQueueService
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
    
    override suspend fun getByFamilyId(familyId: String): List<Routine> {
        // Always read from local (offline-first)
        return localRepo.getByFamilyId(familyId)
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
        familyId: String?
    ): Int {
        return uploadQueue.uploadUnsyncedRoutines(userId, familyId)
    }
    
    /**
     * Get count of unsynced routines for a user or family
     */
    suspend fun getUnsyncedCount(
        userId: String,
        familyId: String?
    ): Int {
        return uploadQueue.getUnsyncedCount(userId, familyId)
    }
}
