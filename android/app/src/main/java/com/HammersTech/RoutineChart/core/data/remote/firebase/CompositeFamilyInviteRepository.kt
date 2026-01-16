package com.HammersTech.RoutineChart.core.data.remote.firebase

import com.HammersTech.RoutineChart.core.data.local.repositories.RoomFamilyInviteRepository
import com.HammersTech.RoutineChart.core.domain.models.FamilyInvite
import com.HammersTech.RoutineChart.core.domain.repositories.FamilyInviteRepository
import com.HammersTech.RoutineChart.core.utils.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Composite repository that uses Room as source of truth and syncs to Firestore
 * Phase 2.3.3: Firestore Sync (Invites)
 */
@Singleton
class CompositeFamilyInviteRepository @Inject constructor(
    private val localRepo: RoomFamilyInviteRepository,
    private val syncService: FirestoreFamilyInviteSyncService
) : FamilyInviteRepository {
    
    private val syncScope = CoroutineScope(Dispatchers.IO)
    
    // MARK: - Repository Methods (Local-First)
    
    override suspend fun create(invite: FamilyInvite) {
        // Always write to local first (offline-first)
        localRepo.create(invite)
        
        // Sync to Firestore asynchronously (don't block on network)
        syncScope.launch {
            try {
                syncService.syncToFirestore(invite)
                AppLogger.Database.info("Synced invite to Firestore: ${invite.id}")
            } catch (e: Exception) {
                // Log error but don't fail - local operation succeeded
                AppLogger.Database.error("Failed to sync invite to Firestore: ${e.message}", e)
            }
        }
    }
    
    override suspend fun getById(id: String): FamilyInvite? {
        // Always read from local (offline-first)
        return localRepo.getById(id)
    }
    
    override suspend fun getByToken(token: String): FamilyInvite? {
        // Query Firestore first - token lookups require internet to validate expiration
        // This ensures we get the latest invite data and can validate expiration correctly
        return try {
            val invite = syncService.getByTokenFromFirestore(token)
            // Optionally cache to local for faster subsequent lookups
            invite?.let {
                try {
                    val existing = localRepo.getById(it.id)
                    if (existing != null) {
                        localRepo.update(it)
                    } else {
                        localRepo.create(it)
                    }
                } catch (e: Exception) {
                    // Log but don't fail - caching is optional
                    AppLogger.Database.error("Failed to cache invite to local: ${e.message}", e)
                }
            }
            invite
        } catch (e: Exception) {
            AppLogger.Database.error("Failed to query invite by token from Firestore: ${e.message}", e)
            throw e
        }
    }
    
    override suspend fun getByInviteCode(inviteCode: String): FamilyInvite? {
        // Query Firestore first - invite code lookups require internet to validate expiration
        // This ensures we get the latest invite data and can validate expiration correctly
        return try {
            val invite = syncService.getByInviteCodeFromFirestore(inviteCode)
            // Optionally cache to local for faster subsequent lookups
            invite?.let {
                try {
                    val existing = localRepo.getById(it.id)
                    if (existing != null) {
                        localRepo.update(it)
                    } else {
                        localRepo.create(it)
                    }
                } catch (e: Exception) {
                    // Log but don't fail - caching is optional
                    AppLogger.Database.error("Failed to cache invite to local: ${e.message}", e)
                }
            }
            invite
        } catch (e: Exception) {
            AppLogger.Database.error("Failed to query invite by code from Firestore: ${e.message}", e)
            throw e
        }
    }
    
    override suspend fun getActiveInvites(familyId: String): List<FamilyInvite> {
        // Always read from local (offline-first)
        return localRepo.getActiveInvites(familyId)
    }
    
    override suspend fun update(invite: FamilyInvite) {
        // Check if this is a usedCount increment before updating local
        val existingInvite = localRepo.getById(invite.id)
        val isUsedCountIncrement = existingInvite != null 
            && existingInvite.usedCount < invite.usedCount 
            && invite.usedCount == existingInvite.usedCount + 1
        
        // Always write to local first (offline-first)
        localRepo.update(invite)
        
        // For usedCount increments, use the optimized increment method
        // This ensures only the usedCount field is updated, which works better with security rules
        if (isUsedCountIncrement && existingInvite != null) {
            try {
                syncService.incrementUsedCount(invite.id, invite.usedCount)
                AppLogger.Database.info("Incremented usedCount for invite ${invite.id} from ${existingInvite.usedCount} to ${invite.usedCount}")
            } catch (e: Exception) {
                AppLogger.Database.error("Failed to increment usedCount: ${e.message}", e)
                throw e
            }
        } else {
            // Regular update - sync all fields
            try {
                syncService.syncToFirestore(invite)
                AppLogger.Database.info("Synced invite update to Firestore: ${invite.id}")
            } catch (e: Exception) {
                AppLogger.Database.error("Failed to sync invite update to Firestore: ${e.message}", e)
                throw e
            }
        }
    }
    
    override suspend fun deactivate(id: String) {
        // Always write to local first
        localRepo.deactivate(id)
        
        // Get the updated invite and sync
        val invite = localRepo.getById(id)
        if (invite != null) {
            syncScope.launch {
                try {
                    syncService.syncToFirestore(invite)
                    AppLogger.Database.info("Synced invite deactivation to Firestore: $id")
                } catch (e: Exception) {
                    AppLogger.Database.error("Failed to sync invite deactivation to Firestore: ${e.message}", e)
                }
            }
        }
    }
    
    override suspend fun deleteExpired() {
        // Local operation only - expired invites cleanup
        localRepo.deleteExpired()
        // Note: Firestore can handle TTL or we sync deletion separately if needed
    }
    
    /**
     * Sync invites from Firestore to local database
     * Used for initial sync or when going online
     * CRITICAL: Also deletes invites from other families to prevent showing stale data
     */
    suspend fun syncFromFirestore(familyId: String) {
        try {
            val firestoreInvites = syncService.syncFromFirestore(familyId)
            
            // CRITICAL: Delete invites from other families to prevent showing stale data
            // When a user joins a new family, we should only show invites for their current family
            localRepo.deleteInvitesFromOtherFamilies(familyId)
            AppLogger.Database.info("Cleaned up invites from other families, keeping: $familyId")
            
            // Merge Firestore data into local database
            for (invite in firestoreInvites) {
                // Check if invite exists locally
                val localInvite = localRepo.getById(invite.id)
                if (localInvite != null) {
                    // Use the more recent data (by updatedAt or createdAt)
                    // For now, prefer Firestore data (assume it's more authoritative)
                    localRepo.update(invite)
                } else {
                    // New invite from Firestore, add to local
                    localRepo.create(invite)
                }
            }
            
            AppLogger.Database.info("Synced ${firestoreInvites.size} invites from Firestore for family: $familyId")
        } catch (e: Exception) {
            AppLogger.Database.error("Failed to sync invites from Firestore: ${e.message}", e)
            throw e
        }
    }
}

