package com.HammersTech.RoutineChart.core.data.local.room.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.HammersTech.RoutineChart.core.data.local.room.entities.FamilyInviteEntity

/**
 * DAO for family invites
 * Phase 2.2: QR Family Joining
 */
@Dao
interface FamilyInviteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invite: FamilyInviteEntity)
    
    @Update
    suspend fun update(invite: FamilyInviteEntity)
    
    @Query("SELECT * FROM family_invites WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FamilyInviteEntity?
    
    @Query("SELECT * FROM family_invites WHERE token = :token LIMIT 1")
    suspend fun getByToken(token: String): FamilyInviteEntity?
    
    @Query("SELECT * FROM family_invites WHERE familyId = :familyId AND isActive = 1 ORDER BY createdAt DESC")
    suspend fun getActiveInvitesByFamily(familyId: String): List<FamilyInviteEntity>
    
    @Query("UPDATE family_invites SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: String)
    
    @Query("DELETE FROM family_invites WHERE expiresAt < :now")
    suspend fun deleteExpired(now: Long)
}

