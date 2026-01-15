package com.HammersTech.RoutineChart.core.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.HammersTech.RoutineChart.core.domain.models.Family
import com.HammersTech.RoutineChart.core.domain.models.PlanTier
import com.HammersTech.RoutineChart.core.utils.AppLogger
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for syncing Family data to/from Firestore
 * Phase 2.3: Firestore Sync (Families)
 */
@Singleton
class FirestoreFamilySyncService @Inject constructor() {
    
    private val db = FirebaseFirestore.getInstance()
    
    /**
     * Collection path: /families/{familyId}
     */
    private fun familyDocument(familyId: String) = db
        .collection("families")
        .document(familyId)
    
    /**
     * Sync family to Firestore. Uses set(merge: true) for upsert.
     */
    suspend fun syncToFirestore(family: Family) {
        try {
            val createdAtTimestamp = com.google.firebase.Timestamp(java.util.Date.from(family.createdAt))
            val updatedAtTimestamp = com.google.firebase.Timestamp(java.util.Date.from(family.updatedAt))
            
            val data = hashMapOf<String, Any>(
                "id" to family.id,
                "timeZone" to family.timeZone,
                "weekStartsOn" to family.weekStartsOn,
                "planTier" to family.planTier.toRawValue(),
                "createdAt" to createdAtTimestamp,
                "updatedAt" to updatedAtTimestamp
            )
            
            family.name?.let { data["name"] = it }
            
            familyDocument(family.id)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .await()
            
            AppLogger.Database.info("Synced family to Firestore: ${family.id}")
        } catch (e: Exception) {
            AppLogger.Database.error("Failed to sync family to Firestore", e)
            throw e
        }
    }
    
    /**
     * Fetches a family from Firestore by ID.
     */
    suspend fun syncFromFirestore(familyId: String): Family? {
        return try {
            val document = familyDocument(familyId).get().await()
            
            if (!document.exists()) {
                return null
            }
            
            val data = document.data ?: return null
            parseFamily(data, document.id)
        } catch (e: Exception) {
            AppLogger.Database.error("Failed to sync family from Firestore", e)
            throw e
        }
    }
    
    /**
     * Parse Firestore document data into Family
     */
    private fun parseFamily(data: Map<String, Any>, id: String): Family {
        val name = data["name"] as? String
        val timeZone = data["timeZone"] as? String
            ?: throw IllegalArgumentException("Missing timeZone")
        val weekStartsOn = (data["weekStartsOn"] as? Number)?.toInt()
            ?: throw IllegalArgumentException("Missing weekStartsOn")
        val planTierRaw = data["planTier"] as? String
            ?: throw IllegalArgumentException("Missing planTier")
        val planTier = PlanTier.fromRawValue(planTierRaw)
        val createdAtTimestamp = data["createdAt"] as? com.google.firebase.Timestamp
            ?: throw IllegalArgumentException("Missing createdAt")
        val updatedAtTimestamp = data["updatedAt"] as? com.google.firebase.Timestamp
            ?: throw IllegalArgumentException("Missing updatedAt")
        
        val createdAt = createdAtTimestamp.toDate().toInstant()
        val updatedAt = updatedAtTimestamp.toDate().toInstant()
        
        return Family(
            id = id,
            name = name,
            timeZone = timeZone,
            weekStartsOn = weekStartsOn,
            planTier = planTier,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
