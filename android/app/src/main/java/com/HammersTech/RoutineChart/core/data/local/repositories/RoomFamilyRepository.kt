package com.HammersTech.RoutineChart.core.data.local.repositories

import com.HammersTech.RoutineChart.core.data.local.room.dao.FamilyDao
import com.HammersTech.RoutineChart.core.data.local.room.entities.FamilyEntity
import com.HammersTech.RoutineChart.core.domain.models.Family
import com.HammersTech.RoutineChart.core.domain.repositories.FamilyRepository
import com.HammersTech.RoutineChart.core.utils.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Room implementation of FamilyRepository
 */
class RoomFamilyRepository @Inject constructor(
    private val familyDao: FamilyDao
) : FamilyRepository {
    override suspend fun create(family: Family) {
        familyDao.insert(FamilyEntity.fromDomain(family))
        AppLogger.Database.info("Created family: ${family.id}")
    }

    override suspend fun update(family: Family) {
        familyDao.update(FamilyEntity.fromDomain(family))
        AppLogger.Database.info("Updated family: ${family.id}")
    }

    override suspend fun getById(id: String): Family? {
        return familyDao.getById(id)?.toDomain()
    }

    override fun observeById(id: String): Flow<Family?> {
        return familyDao.observeById(id).map { it?.toDomain() }
    }

    override suspend fun getFirst(): Family? {
        return familyDao.getFirst()?.toDomain()
    }

    override suspend fun getAll(): List<Family> {
        return familyDao.getAll().map { it.toDomain() }
    }
}

