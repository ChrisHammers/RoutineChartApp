package com.HammersTech.RoutineChart.core.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.HammersTech.RoutineChart.core.domain.models.FamilyInvite
import com.HammersTech.RoutineChart.core.utils.AppLogger
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for syncing FamilyInvite data to/from Firestore
 * MIGRATED: Now uses top-level /invites collection for simpler queries
 */
@Singleton
class FirestoreFamilyInviteSyncService @Inject constructor() {
    
    private val db = FirebaseFirestore.getInstance()
    
    /**
     * Top-level invites collection: /invites/{inviteId}
     */
    private fun invitesCollection() = db.collection("invites")
    
    /**
     * Sync invite to Firestore. Uses set(merge: true) for upsert.
     */
    suspend fun syncToFirestore(invite: FamilyInvite) {
        try {
            val createdAtTimestamp = com.google.firebase.Timestamp(java.util.Date.from(invite.createdAt))
            val expiresAtTimestamp = com.google.firebase.Timestamp(java.util.Date.from(invite.expiresAt))
            
            val data = hashMapOf<String, Any>(
                "id" to invite.id,
                "familyId" to invite.familyId,
                "token" to invite.token,
                "inviteCode" to invite.inviteCode,
                "createdBy" to invite.createdBy,
                "createdAt" to createdAtTimestamp,
                "expiresAt" to expiresAtTimestamp,
                "usedCount" to invite.usedCount,
                "isActive" to invite.isActive
            )
            
            invite.maxUses?.let { data["maxUses"] = it }
            
            invitesCollection()
                .document(invite.id)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .await()
            
            AppLogger.Database.info("Synced invite to Firestore: ${invite.id}")
        } catch (e: Exception) {
            AppLogger.Database.error("Failed to sync invite to Firestore", e)
            throw e
        }
    }
    
    /**
     * Fetches all invites for a given family from Firestore.
     * Uses Source.SERVER to force network request and bypass cache.
     */
    suspend fun syncFromFirestore(familyId: String): List<FamilyInvite> {
        return try {
            AppLogger.Database.info("Fetching invites from Firestore (server) for family: $familyId")
            val snapshot = invitesCollection()
                .whereEqualTo("familyId", familyId)
                .get(Source.SERVER)  // Force server read to bypass cache
                .await()
            
            val invites = snapshot.documents.mapNotNull { document ->
                try {
                    parseInvite(document.data ?: emptyMap(), document.id)
                } catch (e: Exception) {
                    AppLogger.Database.error("Error parsing Firestore invite document ${document.id}", e)
                    null
                }
            }
            AppLogger.Database.info("Successfully fetched ${invites.size} invites from Firestore for family: $familyId")
            invites
        } catch (e: Exception) {
            AppLogger.Database.error("Failed to sync invites from Firestore for family: $familyId", e)
            // Log specific error details
            when {
                e.message?.contains("network", ignoreCase = true) == true -> {
                    AppLogger.Database.error("Network error detected. Check internet connection and Firebase configuration.")
                }
                e.message?.contains("permission", ignoreCase = true) == true -> {
                    AppLogger.Database.error("Permission denied. Check Firestore security rules.")
                }
                else -> {
                    AppLogger.Database.error("Unknown error: ${e.message}")
                }
            }
            throw e
        }
    }
    
    /**
     * Query Firestore for an invite by invite code.
     * Uses top-level collection with direct query (much faster than collection group).
     * Uses Source.SERVER to force network request and bypass cache.
     */
    suspend fun getByInviteCodeFromFirestore(inviteCode: String): FamilyInvite? {
        return try {
            AppLogger.Database.info("Querying Firestore (server) for invite code: $inviteCode")
            val snapshot = invitesCollection()
                .whereEqualTo("inviteCode", inviteCode)
                .limit(1)
                .get(Source.SERVER)  // Force server read to bypass cache
                .await()
            
            if (snapshot.isEmpty) {
                AppLogger.Database.info("No invite found with code: $inviteCode")
                return null
            }
            
            val document = snapshot.documents.first()
            val invite = parseInvite(document.data ?: emptyMap(), document.id)
            AppLogger.Database.info("Found invite ${invite.id} for code: $inviteCode")
            invite
        } catch (e: Exception) {
            AppLogger.Database.error("Failed to query invite by code from Firestore: $inviteCode", e)
            throw e
        }
    }
    
    /**
     * Query Firestore for an invite by token.
     * Uses top-level collection with direct query (much faster than collection group).
     * Uses Source.SERVER to force network request and bypass cache.
     */
    suspend fun getByTokenFromFirestore(token: String): FamilyInvite? {
        return try {
            AppLogger.Database.info("Querying Firestore (server) for invite token: $token")
            val snapshot = invitesCollection()
                .whereEqualTo("token", token)
                .limit(1)
                .get(Source.SERVER)  // Force server read to bypass cache
                .await()
            
            if (snapshot.isEmpty) {
                AppLogger.Database.info("No invite found with token: $token")
                return null
            }
            
            val document = snapshot.documents.first()
            val invite = parseInvite(document.data ?: emptyMap(), document.id)
            AppLogger.Database.info("Found invite ${invite.id} for token: $token")
            invite
        } catch (e: Exception) {
            AppLogger.Database.error("Failed to query invite by token from Firestore: $token", e)
            throw e
        }
    }
    
    /**
     * Parse Firestore document data into FamilyInvite
     */
    private fun parseInvite(data: Map<String, Any>, id: String): FamilyInvite {
        val familyId = data["familyId"] as? String
            ?: throw IllegalArgumentException("Missing familyId")
        val token = data["token"] as? String
            ?: throw IllegalArgumentException("Missing token")
        val inviteCode = data["inviteCode"] as? String
            ?: throw IllegalArgumentException("Missing inviteCode")
        val createdBy = data["createdBy"] as? String
            ?: throw IllegalArgumentException("Missing createdBy")
        val createdAtTimestamp = data["createdAt"] as? com.google.firebase.Timestamp
            ?: throw IllegalArgumentException("Missing createdAt")
        val expiresAtTimestamp = data["expiresAt"] as? com.google.firebase.Timestamp
            ?: throw IllegalArgumentException("Missing expiresAt")
        val usedCount = (data["usedCount"] as? Number)?.toInt()
            ?: throw IllegalArgumentException("Missing usedCount")
        val isActive = data["isActive"] as? Boolean
            ?: throw IllegalArgumentException("Missing isActive")
        
        val createdAt = createdAtTimestamp.toDate().toInstant()
        val expiresAt = expiresAtTimestamp.toDate().toInstant()
        val maxUses = (data["maxUses"] as? Number)?.toInt()
        
        return FamilyInvite(
            id = id,
            familyId = familyId,
            token = token,
            inviteCode = inviteCode,
            createdBy = createdBy,
            createdAt = createdAt,
            expiresAt = expiresAt,
            maxUses = maxUses,
            usedCount = usedCount,
            isActive = isActive
        )
    }
}
