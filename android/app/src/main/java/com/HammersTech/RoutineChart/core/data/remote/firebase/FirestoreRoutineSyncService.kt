package com.HammersTech.RoutineChart.core.data.remote.firebase

import com.HammersTech.RoutineChart.core.domain.models.CompletionRule
import com.HammersTech.RoutineChart.core.domain.models.Routine
import com.HammersTech.RoutineChart.core.utils.AppLogger
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
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
class FirestoreRoutineSyncService
    @Inject
    constructor() {
        private val db = FirebaseFirestore.getInstance()

        /**
         * Collection path: /routines/{routineId} (top-level collection)
         */
        private fun routineDocument(routineId: String) =
            db
                .collection("routines")
                .document(routineId)

        /**
         * Sync routine to Firestore
         */
        suspend fun syncToFirestore(routine: Routine) {
            try {
                val createdAtTimestamp = com.google.firebase.Timestamp(java.util.Date.from(routine.createdAt))
                val updatedAtTimestamp = com.google.firebase.Timestamp(java.util.Date.from(routine.updatedAt))

                val data =
                    hashMapOf<String, Any>(
                        "id" to routine.id,
                        "userId" to routine.userId,
                        "title" to routine.title,
                        "version" to routine.version,
                        "completionRule" to routine.completionRule.toRawValue(),
                        "createdAt" to createdAtTimestamp,
                        "updatedAt" to updatedAtTimestamp,
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
                val document =
                    routineDocument(routineId)
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
         * Queries by both userId and familyId (if provided) and combines results (deduplicated)
         * Similar to iOS implementation
         */
        suspend fun getRoutinesUpdatedSince(
            userId: String?,
            familyId: String?,
            since: Instant,
        ): List<Routine> {
            val allRoutines = mutableListOf<Routine>()
            val routineIds = mutableSetOf<String>() // For deduplication

            AppLogger.Database.info(
                "🔍 Querying Firestore for routines: userId=${userId ?: "nil"}, familyId=${familyId ?: "nil"}, since=$since",
            )

            // Query by userId if provided (gets user's personal + family routines)
            if (userId != null) {
                try {
                    val query =
                        db.collection("routines")
                            .whereEqualTo("userId", userId)
                            .whereGreaterThan("updatedAt", com.google.firebase.Timestamp(java.util.Date.from(since)))
                            .orderBy("updatedAt")

                    val snapshot = query.get(Source.SERVER).await()
                    AppLogger.Database.info("📥 Query by userId found ${snapshot.documents.size} document(s)")

                    snapshot.documents.forEach { document ->
                        val data = document.data ?: return@forEach

                        // Log document data for debugging
                        val docUserId = data["userId"] as? String ?: "nil"
                        val docFamilyId = data["familyId"] as? String ?: "nil"
                        val docTitle = data["title"] as? String ?: "nil"
                        AppLogger.Database.info("   - Document ${document.id}: userId=$docUserId, familyId=$docFamilyId, title=$docTitle")

                        try {
                            val routine = parseRoutine(data, document.id)
                            if (!routineIds.contains(routine.id)) {
                                allRoutines.add(routine)
                                routineIds.add(routine.id)
                                AppLogger.Database.info("   ✅ Added routine: ${routine.id} - ${routine.title}")
                            }
                        } catch (e: Exception) {
                            AppLogger.Database.error("⚠️ Failed to parse routine from document ${document.id}", e)
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.Database.error("❌ Query by userId failed: ${e.message}", e)
                    // Don't throw - continue with familyId query
                }
            }

            // Query by familyId if provided (gets all family routines, including other users')
            if (familyId != null) {
                try {
                    val query =
                        db.collection("routines")
                            .whereEqualTo("familyId", familyId)
                            .whereGreaterThan("updatedAt", com.google.firebase.Timestamp(java.util.Date.from(since)))
                            .orderBy("updatedAt")

                    val snapshot = query.get(Source.SERVER).await()
                    AppLogger.Database.info("📥 Query by familyId found ${snapshot.documents.size} document(s)")

                    snapshot.documents.forEach { document ->
                        val data = document.data ?: return@forEach

                        // Log document data for debugging
                        val docUserId = data["userId"] as? String ?: "nil"
                        val docFamilyId = data["familyId"] as? String ?: "nil"
                        val docTitle = data["title"] as? String ?: "nil"
                        AppLogger.Database.info("   - Document ${document.id}: userId=$docUserId, familyId=$docFamilyId, title=$docTitle")

                        try {
                            val routine = parseRoutine(data, document.id)
                            if (!routineIds.contains(routine.id)) {
                                allRoutines.add(routine)
                                routineIds.add(routine.id)
                                AppLogger.Database.info("   ✅ Added routine: ${routine.id} - ${routine.title}")
                            }
                        } catch (e: Exception) {
                            AppLogger.Database.error("⚠️ Failed to parse routine from document ${document.id}", e)
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.Database.error("❌ Query by familyId failed: ${e.message}", e)
                    // Don't throw - continue with results from userId query
                }
            }

            AppLogger.Database.info("✅ Total routines found: ${allRoutines.size} (deduplicated from ${routineIds.size} unique IDs)")

            // Sort by updatedAt (ascending) to maintain order
            return allRoutines.sortedBy { it.updatedAt }
        }

        /**
         * Parse Firestore document data into Routine
         */
        private fun parseRoutine(
            data: Map<String, Any>,
            id: String,
        ): Routine {
            val userId =
                data["userId"] as? String
                    ?: throw IllegalArgumentException("Missing userId for Routine $id")
            val title =
                data["title"] as? String
                    ?: throw IllegalArgumentException("Missing title for Routine $id")
            val version =
                (data["version"] as? Number)?.toInt()
                    ?: throw IllegalArgumentException("Missing version for Routine $id")
            val completionRuleRaw =
                data["completionRule"] as? String
                    ?: throw IllegalArgumentException("Missing completionRule for Routine $id")
            val completionRule = CompletionRule.fromRawValue(completionRuleRaw)
            val createdAtTimestamp =
                data["createdAt"] as? com.google.firebase.Timestamp
                    ?: throw IllegalArgumentException("Missing createdAt for Routine $id")
            val updatedAtTimestamp =
                data["updatedAt"] as? com.google.firebase.Timestamp
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
                deletedAt = deletedAt,
            )
        }
    }
