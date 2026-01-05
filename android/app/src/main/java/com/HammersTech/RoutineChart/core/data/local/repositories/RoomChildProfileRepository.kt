package com.HammersTech.RoutineChart.core.data.local.repositories

import com.HammersTech.RoutineChart.core.data.local.room.dao.ChildProfileDao
import com.HammersTech.RoutineChart.core.data.local.room.entities.ChildProfileEntity
import com.HammersTech.RoutineChart.core.domain.models.ChildProfile
import com.HammersTech.RoutineChart.core.domain.repositories.ChildProfileRepository
import com.HammersTech.RoutineChart.core.utils.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Room implementation of ChildProfileRepository
 */
class RoomChildProfileRepository @Inject constructor(
    private val childProfileDao: ChildProfileDao
) : ChildProfileRepository {
    override suspend fun create(profile: ChildProfile) {
        childProfileDao.insert(ChildProfileEntity.fromDomain(profile))
        AppLogger.Database.info("Created child profile: ${profile.id}")
    }

    override suspend fun update(profile: ChildProfile) {
        childProfileDao.update(ChildProfileEntity.fromDomain(profile))
        AppLogger.Database.info("Updated child profile: ${profile.id}")
    }

    override suspend fun getById(id: String): ChildProfile? {
        return childProfileDao.getById(id)?.toDomain()
    }

    override fun observeById(id: String): Flow<ChildProfile?> {
        return childProfileDao.observeById(id).map { it?.toDomain() }
    }

    override suspend fun getByFamilyId(familyId: String): List<ChildProfile> {
        return childProfileDao.getByFamilyId(familyId).map { it.toDomain() }
    }

    override fun observeByFamilyId(familyId: String): Flow<List<ChildProfile>> {
        return childProfileDao.observeByFamilyId(familyId).map { list ->
            list.map { it.toDomain() }
        }
    }
}

