package com.HammersTech.RoutineChart.core.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.HammersTech.RoutineChart.core.domain.models.CompletionRule
import com.HammersTech.RoutineChart.core.domain.models.Routine
import com.HammersTech.RoutineChart.core.utils.AppLogger
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for syncing Routine data to/from Firestore
 * Phase 3.2: Upload Queue (Routines)
 * Routines are stored as top-level documents: /routines/{routineId}
 */
@Singleton
class FirestoreRoutineSyncService @Inject constructor() {
    
    private val db = FirebaseFirestore.getInstance()
    
    /**
     * Collection path: /routines/{routineId} (top-level collection)
     */
    private fun routineDocument(routineId: String) = db
        .collection("routines")
        .document(routineId)
    
    /**
     * Sync routine to Firestore
     */
    suspend fun syncToFirestore(routine: Routine) {
        try {
            val createdAtTimestamp = com.google.firebase.Timestamp(java.util.Date.from(routine.createdAt))
            val updatedAtTimestamp = com.google.firebase.Timestamp(java.util.Date.from(routine.updatedAt))
            
            val data = hashMapOf<String, Any>(
                "id" to routine.id,
                "userId" to routine.userId,
                "title" to routine.title,
                "version" to routine.version,
                "completionRule" to routine.completionRule.toRawValue(),
                "createdAt" to createdAtTimestamp,
                "updatedAt" to updatedAtTimestamp
            )
            
            // Only include familyId if it's not null
            routine.familyId?.let { data["familyId"] = it }
            
            // Only include iconName if it's not null
            routine.iconName?.let { data["iconName"] = it }
            
            // Only include deletedAt if it's not null
            routine.deletedAt?.let { 
                data["deletedAt"] = com.google.firebase.Timestamp(java.util.Date.from(it))
            }
            
            routineDocument(routine.id)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .await()
            
            AppLogger.Database.info("Synced routine to Firestore: ${routine.id}")
        } catch (e: Exception) {
            AppLogger.Database.error("Failed to sync routine to Firestore: ${routine.id}", e)
            throw e
        }
    }
    
    /**
     * Sync routine from Firestore
     */
    suspend fun syncFromFirestore(routineId: String): Routine? {
        return try {
            val document = routineDocument(routineId)
                .get(Source.SERVER)
                .await()
            
            if (!document.exists()) {
                return null
            }
            
            val data = document.data ?: return null
            val routine = parseRoutine(data, document.id)
            AppLogger.Database.info("Synced routine from Firestore: $routineId")
            routine
        } catch (e: Exception) {
            AppLogger.Database.error("Failed to sync routine from Firestore: $routineId", e)
            throw e
        }
    }
    
    /**
     * Get routines updated since a timestamp
     * Can filter by userId or familyId
     */
    suspend fun getRoutinesUpdatedSince(
        userId: String? = null,
        familyId: String? = null,
        since: Instant
    ): List<Routine> {
        return try {
            var query = db.collection("routines")
                .whereGreaterThan("updatedAt", com.google.firebase.Timestamp(java.util.Date.from(since)))
                .orderBy("updatedAt")
            
            // Filter by userId if provided, otherwise filter by familyId
            if (userId != null) {
                query = query.whereEqualTo("userId", userId)
            } else if (familyId != null) {
                query = query.whereEqualTo("familyId", familyId)
            }
            
            val snapshot = query.get(Source.SERVER).await()
            
            snapshot.documents.mapNotNull { document ->
                val data = document.data ?: return@mapNotNull null
                try {
                    parseRoutine(data, document.id)
                } catch (e: Exception) {
                    AppLogger.Database.error("Failed to parse routine: ${document.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            AppLogger.Database.error("Failed to get routines updated since: $since", e)
            throw e
        }
    }
    
    /**
     * Parse Firestore document data into Routine
     */
    private fun parseRoutine(data: Map<String, Any>, id: String): Routine {
        val userId = data["userId"] as? String
            ?: throw IllegalArgumentException("Missing userId for Routine $id")
        val title = data["title"] as? String
            ?: throw IllegalArgumentException("Missing title for Routine $id")
        val version = (data["version"] as? Number)?.toInt()
            ?: throw IllegalArgumentException("Missing version for Routine $id")
        val completionRuleRaw = data["completionRule"] as? String
            ?: throw IllegalArgumentException("Missing completionRule for Routine $id")
        val completionRule = CompletionRule.fromRawValue(completionRuleRaw)
        val createdAtTimestamp = data["createdAt"] as? com.google.firebase.Timestamp
            ?: throw IllegalArgumentException("Missing createdAt for Routine $id")
        val updatedAtTimestamp = data["updatedAt"] as? com.google.firebase.Timestamp
            ?: throw IllegalArgumentException("Missing updatedAt for Routine $id")
        
        val familyId = data["familyId"] as? String
        val iconName = data["iconName"] as? String
        val deletedAtTimestamp = data["deletedAt"] as? com.google.firebase.Timestamp
        
        val createdAt = createdAtTimestamp.toDate().toInstant()
        val updatedAt = updatedAtTimestamp.toDate().toInstant()
        val deletedAt = deletedAtTimestamp?.toDate()?.toInstant()
        
        return Routine(
            id = id,
            userId = userId,
            familyId = familyId,
            title = title,
            iconName = iconName,
            version = version,
            completionRule = completionRule,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt
        )
    }
}
