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
class RoomRoutineStepRepository @Inject constructor(
    private val routineStepDao: RoutineStepDao
) : RoutineStepRepository {
    override suspend fun create(step: RoutineStep) {
        routineStepDao.insert(RoutineStepEntity.fromDomain(step))
        AppLogger.Database.info("Created routine step: ${step.id}")
    }

    override suspend fun update(step: RoutineStep) {
        routineStepDao.update(RoutineStepEntity.fromDomain(step))
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

    override suspend fun getByFamilyId(familyId: String): List<RoutineStep> {
        return routineStepDao.getByFamilyId(familyId).map { it.toDomain() }
    }
}

