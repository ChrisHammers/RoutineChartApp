package com.HammersTech.RoutineChart.core.data.local.repositories

import com.HammersTech.RoutineChart.core.data.local.room.dao.RoutineAssignmentDao
import com.HammersTech.RoutineChart.core.data.local.room.entities.RoutineAssignmentEntity
import com.HammersTech.RoutineChart.core.domain.models.RoutineAssignment
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineAssignmentRepository
import com.HammersTech.RoutineChart.core.utils.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Room implementation of RoutineAssignmentRepository
 */
class RoomRoutineAssignmentRepository @Inject constructor(
    private val routineAssignmentDao: RoutineAssignmentDao
) : RoutineAssignmentRepository {
    override suspend fun create(assignment: RoutineAssignment) {
        routineAssignmentDao.insert(RoutineAssignmentEntity.fromDomain(assignment, synced = 0))
        AppLogger.Database.info("Created routine assignment: ${assignment.id}")
    }

    override suspend fun update(assignment: RoutineAssignment) {
        val toSave = assignment.copy(updatedAt = java.time.Instant.now())
        routineAssignmentDao.update(RoutineAssignmentEntity.fromDomain(toSave, synced = 0))
        AppLogger.Database.info("Updated routine assignment: ${assignment.id}")
    }

    override suspend fun getById(id: String): RoutineAssignment? {
        return routineAssignmentDao.getById(id)?.toDomain()
    }

    override suspend fun getByFamilyId(familyId: String): List<RoutineAssignment> {
        return routineAssignmentDao.getByFamilyId(familyId).map { it.toDomain() }
    }

    override suspend fun getActiveByChildId(childId: String): List<RoutineAssignment> {
        return routineAssignmentDao.getActiveByChildId(childId).map { it.toDomain() }
    }

    override fun observeActiveByChildId(childId: String): Flow<List<RoutineAssignment>> {
        return routineAssignmentDao.observeActiveByChildId(childId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getByRoutineId(routineId: String): List<RoutineAssignment> {
        return routineAssignmentDao.getByRoutineId(routineId).map { it.toDomain() }
    }

    override suspend fun softDelete(id: String) {
        val existing = routineAssignmentDao.getById(id) ?: return
        val updated = existing.copy(
            deletedAt = java.time.Instant.now(),
            updatedAt = java.time.Instant.now(),
            synced = 0
        )
        routineAssignmentDao.update(updated)
        AppLogger.Database.info("Soft deleted routine assignment: $id")
    }

    // Phase 3.5: Used by AssignmentUploadQueueService and CompositeRoutineAssignmentRepository
    suspend fun getUnsynced(familyId: String): List<RoutineAssignment> {
        return routineAssignmentDao.getUnsynced(familyId).map { it.toDomain() }
    }

    suspend fun markAsSynced(assignmentIds: List<String>) {
        if (assignmentIds.isEmpty()) return
        routineAssignmentDao.markAsSynced(assignmentIds)
    }

    /** Save assignment from Firestore (upsert and mark as synced). Used by composite when pulling. */
    suspend fun saveFromFirestore(assignment: RoutineAssignment) {
        routineAssignmentDao.insert(RoutineAssignmentEntity.fromDomain(assignment, synced = 1))
    }
}

