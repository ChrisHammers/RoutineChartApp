package com.HammersTech.RoutineChart.core.domain.repositories

import com.HammersTech.RoutineChart.core.domain.models.FamilyInvite

/**
 * Repository interface for family invites
 * Phase 2.2: QR Family Joining
 */
interface FamilyInviteRepository {
    /**
     * Create a new family invite
     */
    suspend fun create(invite: FamilyInvite)

    /**
     * Get invite by ID
     */
    suspend fun getById(id: String): FamilyInvite?

    /**
     * Get invite by token
     */
    suspend fun getByToken(token: String): FamilyInvite?

    /**
     * Get invite by invite code (e.g., "ABC-1234")
     */
    suspend fun getByInviteCode(inviteCode: String): FamilyInvite?

    /**
     * Get all active invites for a family
     */
    suspend fun getActiveInvites(familyId: String): List<FamilyInvite>

    /**
     * Update an invite (for incrementing usedCount or deactivating)
     */
    suspend fun update(invite: FamilyInvite)

    /**
     * Deactivate an invite
     */
    suspend fun deactivate(id: String)

    /**
     * Delete expired invites
     */
    suspend fun deleteExpired()
}
