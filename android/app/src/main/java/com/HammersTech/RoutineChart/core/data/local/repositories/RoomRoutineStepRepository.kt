package com.HammersTech.RoutineChart.core.data.local.repositories

import com.HammersTech.RoutineChart.core.data.local.room.dao.RoutineStepDao
import com.HammersTech.RoutineChart.core.data.local.room.entities.RoutineStepEntity
import com.HammersTech.RoutineChart.core.domain.models.RoutineStep
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineStepRepository
import com.HammersTech.RoutineChart.core.utils.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Room implementation of RoutineStepRepository
 */
class RoomRoutineStepRepository
    @Inject
    constructor(
        private val routineStepDao: RoutineStepDao,
    ) : RoutineStepRepository {
        override suspend fun create(step: RoutineStep) {
            // Mark as unsynced (new records need to be uploaded)
            routineStepDao.insert(RoutineStepEntity.fromDomain(step, synced = false))
            AppLogger.Database.info("Created routine step: ${step.id}")
        }

        override suspend fun update(step: RoutineStep) {
            // Mark as unsynced (updated records need to be uploaded)
            routineStepDao.update(RoutineStepEntity.fromDomain(step, synced = false))
            AppLogger.Database.info("Updated routine step: ${step.id}")
        }

        override suspend fun getById(id: String): RoutineStep? {
            return routineStepDao.getById(id)?.toDomain()
        }

        override suspend fun getByRoutineId(routineId: String): List<RoutineStep> {
            return routineStepDao.getByRoutineId(routineId).map { it.toDomain() }
        }

        override fun observeByRoutineId(routineId: String): Flow<List<RoutineStep>> {
            return routineStepDao.observeByRoutineId(routineId).map { list ->
                list.map { it.toDomain() }
            }
        }

        // MARK: - Sync Methods (Phase 3.4: Upload Queue)

        /**
         * Get all unsynced steps for a routine
         */
        suspend fun getUnsynced(routineId: String): List<RoutineStep> {
            return routineStepDao.getUnsynced(routineId).map { it.toDomain() }
        }

        /**
         * Mark a step as synced
         */
        suspend fun markAsSynced(stepId: String) {
            routineStepDao.markAsSynced(stepId)
            AppLogger.Database.info("Marked step as synced: $stepId")
        }

        /**
         * Mark multiple steps as synced
         */
        suspend fun markAsSynced(stepIds: List<String>) {
            routineStepDao.markAsSynced(stepIds)
            AppLogger.Database.info("Marked ${stepIds.size} step(s) as synced")
        }
    }
