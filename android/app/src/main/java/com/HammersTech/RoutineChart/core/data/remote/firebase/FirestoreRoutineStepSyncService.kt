package com.HammersTech.RoutineChart.core.data.remote.firebase

import com.HammersTech.RoutineChart.core.domain.models.RoutineStep
import com.HammersTech.RoutineChart.core.utils.AppLogger
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for syncing RoutineStep data to/from Firestore
 * Phase 3.4: Sync RoutineSteps
 * Steps are stored as subcollection: /routines/{routineId}/steps/{stepId}
 */
@Singleton
class FirestoreRoutineStepSyncService
    @Inject
    constructor() {
        private val db = FirebaseFirestore.getInstance()

        /**
         * Collection path: /routines/{routineId}/steps/{stepId} (subcollection)
         */
        private fun stepDocument(
            routineId: String,
            stepId: String,
        ) = db
            .collection("routines")
            .document(routineId)
            .collection("steps")
            .document(stepId)

        /**
         * Sync step to Firestore
         */
        suspend fun syncToFirestore(step: RoutineStep) {
            try {
                val createdAtTimestamp = com.google.firebase.Timestamp(java.util.Date.from(step.createdAt))
                val deletedAtTimestamp = step.deletedAt?.let { com.google.firebase.Timestamp(java.util.Date.from(it)) }

                val data =
                    hashMapOf<String, Any>(
                        "id" to step.id,
                        "routineId" to step.routineId,
                        "orderIndex" to step.orderIndex,
                        "createdAt" to createdAtTimestamp,
                    )

                // Only include optional fields if they're not null
                step.label?.let { data["label"] = it }
                step.iconName?.let { data["iconName"] = it }
                step.audioCueUrl?.let { data["audioCueUrl"] = it }
                deletedAtTimestamp?.let { data["deletedAt"] = it }

                stepDocument(step.routineId, step.id)
                    .set(data, com.google.firebase.firestore.SetOptions.merge())
                    .await()

                AppLogger.Database.info("Synced step to Firestore: ${step.id} (routine: ${step.routineId})")
            } catch (e: Exception) {
                AppLogger.Database.error("Failed to sync step to Firestore: ${step.id}", e)
                throw e
            }
        }

        /**
         * Sync step from Firestore
         */
        suspend fun syncFromFirestore(
            routineId: String,
            stepId: String,
        ): RoutineStep? {
            return try {
                val document =
                    stepDocument(routineId, stepId)
                        .get(Source.SERVER)
                        .await()

                if (!document.exists()) {
                    return null
                }

                val data = document.data ?: return null
                val step = parseStep(data, document.id)
                AppLogger.Database.info("Synced step from Firestore: $stepId (routine: $routineId)")
                step
            } catch (e: Exception) {
                AppLogger.Database.error("Failed to sync step from Firestore: $stepId", e)
                throw e
            }
        }

        /**
         * Get all steps for a routine
         */
        suspend fun getAllSteps(routineId: String): List<RoutineStep> {
            return try {
                val snapshot =
                    db.collection("routines")
                        .document(routineId)
                        .collection("steps")
                        .get(Source.SERVER)
                        .await()

                snapshot.documents.mapNotNull { document ->
                    val data = document.data ?: return@mapNotNull null
                    try {
                        parseStep(data, document.id)
                    } catch (e: Exception) {
                        AppLogger.Database.error("Failed to parse step: ${document.id}", e)
                        null
                    }
                }
            } catch (e: Exception) {
                AppLogger.Database.error("Failed to get all steps for routine: $routineId", e)
                throw e
            }
        }

        /**
         * Parse Firestore document data into RoutineStep
         */
        private fun parseStep(
            data: Map<String, Any>,
            id: String,
        ): RoutineStep {
            val routineId =
                data["routineId"] as? String
                    ?: throw IllegalArgumentException("Missing routineId for RoutineStep $id")
            val orderIndex =
                (data["orderIndex"] as? Number)?.toInt()
                    ?: throw IllegalArgumentException("Missing orderIndex for RoutineStep $id")
            val createdAtTimestamp =
                data["createdAt"] as? com.google.firebase.Timestamp
                    ?: throw IllegalArgumentException("Missing createdAt for RoutineStep $id")

            val label = data["label"] as? String
            val iconName = data["iconName"] as? String
            val audioCueUrl = data["audioCueUrl"] as? String
            val deletedAtTimestamp = data["deletedAt"] as? com.google.firebase.Timestamp

            val createdAt = createdAtTimestamp.toDate().toInstant()
            val deletedAt = deletedAtTimestamp?.toDate()?.toInstant()

            return RoutineStep(
                id = id,
                routineId = routineId,
                orderIndex = orderIndex,
                label = label,
                iconName = iconName,
                audioCueUrl = audioCueUrl,
                createdAt = createdAt,
                deletedAt = deletedAt,
            )
        }
    }
