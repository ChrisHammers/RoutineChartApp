//
//  FirestoreAssignmentSyncService.swift
//  RoutineChart
//
//  Phase 3.5: Sync RoutineAssignments
//  Assignments stored as top-level documents: /routine_assignments/{assignmentId}
//

import Foundation
import FirebaseFirestore
import OSLog

/// Service for syncing RoutineAssignment data to/from Firestore
final class FirestoreAssignmentSyncService {
    private let db = Firestore.firestore()
    
    /// Collection path: /routine_assignments/{assignmentId} (top-level collection)
    private func assignmentDocument(assignmentId: String) -> DocumentReference {
        db.collection("routine_assignments").document(assignmentId)
    }
    
    /// Sync assignment to Firestore
    func syncToFirestore(_ assignment: RoutineAssignment) async throws {
        let data: [String: Any] = [
            "id": assignment.id,
            "familyId": assignment.familyId,
            "routineId": assignment.routineId,
            "childId": assignment.childId,
            "isActive": assignment.isActive,
            "assignedAt": Timestamp(date: assignment.assignedAt),
            "updatedAt": Timestamp(date: assignment.updatedAt),
            "deletedAt": assignment.deletedAt.map { Timestamp(date: $0) } as Any
        ]
        
        try await assignmentDocument(assignmentId: assignment.id)
            .setData(data, merge: true)
        
        AppLogger.database.info("Synced assignment to Firestore: \(assignment.id)")
    }
    
    /// Get assignments for a family updated since a timestamp
    func getAssignmentsUpdatedSince(familyId: String, since: Date) async throws -> [RoutineAssignment] {
        let collectionRef = db.collection("routine_assignments")
        let query = collectionRef
            .whereField("familyId", isEqualTo: familyId)
            .whereField("updatedAt", isGreaterThan: Timestamp(date: since))
            .order(by: "updatedAt", descending: false)
        
        let snapshot = try await query.getDocuments()
        AppLogger.database.info("📥 Query assignments by familyId found \(snapshot.documents.count) document(s)")
        
        return try snapshot.documents.compactMap { document in
            guard let data = document.data() as? [String: Any] else { return nil }
            return try parseAssignment(from: data, id: document.documentID)
        }
    }
    
    /// Parse Firestore document data into RoutineAssignment
    private func parseAssignment(from data: [String: Any], id: String) throws -> RoutineAssignment? {
        guard let familyId = data["familyId"] as? String,
              let routineId = data["routineId"] as? String,
              let childId = data["childId"] as? String,
              let isActive = data["isActive"] as? Bool,
              let assignedAtTimestamp = data["assignedAt"] as? Timestamp,
              let updatedAtTimestamp = data["updatedAt"] as? Timestamp else {
            AppLogger.database.warning("⚠️ Missing required fields for RoutineAssignment \(id)")
            return nil
        }
        
        let deletedAtTimestamp = data["deletedAt"] as? Timestamp
        
        return RoutineAssignment(
            id: id,
            familyId: familyId,
            routineId: routineId,
            childId: childId,
            isActive: isActive,
            assignedAt: assignedAtTimestamp.dateValue(),
            updatedAt: updatedAtTimestamp.dateValue(),
            deletedAt: deletedAtTimestamp?.dateValue()
        )
    }
}
