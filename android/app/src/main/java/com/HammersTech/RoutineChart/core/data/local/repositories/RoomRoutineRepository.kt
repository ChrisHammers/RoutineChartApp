package com.HammersTech.RoutineChart.core.data.local.repositories

import com.HammersTech.RoutineChart.core.data.local.room.dao.RoutineDao
import com.HammersTech.RoutineChart.core.data.local.room.entities.RoutineEntity
import com.HammersTech.RoutineChart.core.domain.models.Routine
import com.HammersTech.RoutineChart.core.domain.repositories.RoutineRepository
import com.HammersTech.RoutineChart.core.utils.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Room implementation of RoutineRepository
 */
class RoomRoutineRepository @Inject constructor(
    private val routineDao: RoutineDao
) : RoutineRepository {
    override suspend fun create(routine: Routine) {
        routineDao.insert(RoutineEntity.fromDomain(routine))
        AppLogger.Database.info("Created routine: ${routine.id}")
    }

    override suspend fun update(routine: Routine) {
        routineDao.update(RoutineEntity.fromDomain(routine))
        AppLogger.Database.info("Updated routine: ${routine.id}")
    }

    override suspend fun getById(id: String): Routine? {
        return routineDao.getById(id)?.toDomain()
    }

    override fun observeById(id: String): Flow<Routine?> {
        return routineDao.observeById(id).map { it?.toDomain() }
    }

    override suspend fun getByFamilyId(familyId: String): List<Routine> {
        return routineDao.getByFamilyId(familyId).map { it.toDomain() }
    }

    override fun observeByFamilyId(familyId: String): Flow<List<Routine>> {
        return routineDao.observeByFamilyId(familyId).map { list ->
            list.map { it.toDomain() }
        }
    }
}

