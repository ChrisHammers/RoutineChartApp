package com.HammersTech.RoutineChart.core.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.HammersTech.RoutineChart.core.domain.models.FamilyInvite
import com.HammersTech.RoutineChart.core.utils.AppLogger
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for syncing FamilyInvite data to/from Firestore
 * Phase 2.3.3: Firestore Sync (Invites)
 */
@Singleton
class FirestoreFamilyInviteSyncService @Inject constructor() {
    
    private val db = FirebaseFirestore.getInstance()
    
    /**
     * Collection path: /families/{familyId}/invites/{inviteId}
     */
    private fun collectionPath(familyId: String) = db
        .collection("families")
        .document(familyId)
        .collection("invites")
    
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
            
            collectionPath(invite.familyId)
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
     */
    suspend fun syncFromFirestore(familyId: String): List<FamilyInvite> {
        return try {
            val snapshot = collectionPath(familyId).get().await()
            snapshot.documents.mapNotNull { document ->
                try {
                    parseInvite(document.data ?: emptyMap(), document.id)
                } catch (e: Exception) {
                    AppLogger.Database.error("Error parsing Firestore invite document ${document.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            AppLogger.Database.error("Failed to sync invites from Firestore", e)
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


