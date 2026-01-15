package com.HammersTech.RoutineChart.core.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
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
     * Uses Source.SERVER to force network request and bypass cache.
     */
    suspend fun syncFromFirestore(familyId: String): Family? {
        return try {
            AppLogger.Database.info("Fetching family from Firestore (server): $familyId")
            val document = familyDocument(familyId)
                .get(Source.SERVER)  // Force server read to bypass cache
                .await()
            
            if (!document.exists()) {
                AppLogger.Database.error("Family document does not exist: $familyId")
                return null
            }
            
            val data = document.data ?: return null
            val family = parseFamily(data, document.id)
            AppLogger.Database.info("Successfully fetched family from Firestore: $familyId")
            family
        } catch (e: Exception) {
            AppLogger.Database.error("Failed to sync family from Firestore: $familyId", e)
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
