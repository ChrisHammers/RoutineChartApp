package com.HammersTech.RoutineChart.core.data.local.repositories

import com.HammersTech.RoutineChart.core.data.local.room.daos.FamilyInviteDao
import com.HammersTech.RoutineChart.core.data.local.room.entities.FamilyInviteEntity
import com.HammersTech.RoutineChart.core.domain.models.FamilyInvite
import com.HammersTech.RoutineChart.core.domain.repositories.FamilyInviteRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room implementation of FamilyInviteRepository
 * Phase 2.2: QR Family Joining
 */
@Singleton
class RoomFamilyInviteRepository @Inject constructor(
    private val familyInviteDao: FamilyInviteDao
) : FamilyInviteRepository {
    
    override suspend fun create(invite: FamilyInvite) {
        familyInviteDao.insert(FamilyInviteEntity.fromDomain(invite))
    }
    
    override suspend fun getById(id: String): FamilyInvite? {
        return familyInviteDao.getById(id)?.toDomain()
    }
    
    override suspend fun getByToken(token: String): FamilyInvite? {
        return familyInviteDao.getByToken(token)?.toDomain()
    }
    
    override suspend fun getByInviteCode(inviteCode: String): FamilyInvite? {
        return familyInviteDao.getByInviteCode(inviteCode)?.toDomain()
    }
    
    override suspend fun getActiveInvites(familyId: String): List<FamilyInvite> {
        return familyInviteDao.getActiveInvitesByFamily(familyId).map { it.toDomain() }
    }
    
    override suspend fun update(invite: FamilyInvite) {
        familyInviteDao.update(FamilyInviteEntity.fromDomain(invite))
    }
    
    override suspend fun deactivate(id: String) {
        familyInviteDao.deactivate(id)
    }
    
    override suspend fun deleteExpired() {
        val now = Instant.now().toEpochMilli()
        familyInviteDao.deleteExpired(now)
    }
}

