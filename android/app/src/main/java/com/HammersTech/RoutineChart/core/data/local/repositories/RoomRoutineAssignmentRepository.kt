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
class RoomRoutineAssignmentRepository
    @Inject
    constructor(
        private val routineAssignmentDao: RoutineAssignmentDao,
    ) : RoutineAssignmentRepository {
        override suspend fun create(assignment: RoutineAssignment) {
            routineAssignmentDao.insert(RoutineAssignmentEntity.fromDomain(assignment))
            AppLogger.Database.info("Created routine assignment: ${assignment.id}")
        }

        override suspend fun update(assignment: RoutineAssignment) {
            routineAssignmentDao.update(RoutineAssignmentEntity.fromDomain(assignment))
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
    }
