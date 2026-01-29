package com.HammersTech.RoutineChart.core.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.HammersTech.RoutineChart.core.domain.models.RoutineAssignment
import com.HammersTech.RoutineChart.core.utils.AppLogger
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for syncing RoutineAssignment data to/from Firestore
 * Phase 3.5: Sync RoutineAssignments
 * Assignments stored as top-level documents: /routine_assignments/{assignmentId}
 */
@Singleton
class FirestoreAssignmentSyncService @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()

    private fun assignmentDocument(assignmentId: String) =
        db.collection("routine_assignments").document(assignmentId)

    /**
     * Sync assignment to Firestore
     */
    suspend fun syncToFirestore(assignment: RoutineAssignment) {
        val data = mutableMapOf<String, Any>(
            "id" to assignment.id,
            "familyId" to assignment.familyId,
            "routineId" to assignment.routineId,
            "childId" to assignment.childId,
            "isActive" to assignment.isActive,
            "assignedAt" to com.google.firebase.Timestamp(java.util.Date.from(assignment.assignedAt)),
            "updatedAt" to com.google.firebase.Timestamp(java.util.Date.from(assignment.updatedAt))
        )
        assignment.deletedAt?.let {
            data["deletedAt"] = com.google.firebase.Timestamp(java.util.Date.from(it))
        }

        assignmentDocument(assignment.id)
            .set(data, SetOptions.merge())
            .await()

        AppLogger.Database.info("Synced assignment to Firestore: ${assignment.id}")
    }

    /**
     * Get assignments for a family updated since a timestamp
     */
    suspend fun getAssignmentsUpdatedSince(familyId: String, since: Instant): List<RoutineAssignment> {
        val sinceTimestamp = com.google.firebase.Timestamp(java.util.Date.from(since))
        val snapshot = db.collection("routine_assignments")
            .whereEqualTo("familyId", familyId)
            .whereGreaterThan("updatedAt", sinceTimestamp)
            .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .get(Source.SERVER)
            .await()

        AppLogger.Database.info("📥 Query assignments by familyId found ${snapshot.documents.size} document(s)")

        return snapshot.documents.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            parseAssignment(data, doc.id)
        }
    }

    private fun parseAssignment(data: Map<String, Any>, id: String): RoutineAssignment? {
        val familyId = data["familyId"] as? String ?: return null
        val routineId = data["routineId"] as? String ?: return null
        val childId = data["childId"] as? String ?: return null
        val isActive = data["isActive"] as? Boolean ?: return null
        val assignedAtTs = data["assignedAt"] as? com.google.firebase.Timestamp ?: return null
        val updatedAtTs = data["updatedAt"] as? com.google.firebase.Timestamp ?: return null

        val assignedAt = assignedAtTs.toDate().toInstant()
        val updatedAt = updatedAtTs.toDate().toInstant()
        val deletedAt = (data["deletedAt"] as? com.google.firebase.Timestamp)?.toDate()?.toInstant()

        return RoutineAssignment(
            id = id,
            familyId = familyId,
            routineId = routineId,
            childId = childId,
            isActive = isActive,
            assignedAt = assignedAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt
        )
    }
}
