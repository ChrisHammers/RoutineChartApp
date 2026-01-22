//
//  FirestoreRoutineStepSyncService.swift
//  RoutineChart
//
//  Created for Phase 3.4: Sync RoutineSteps
//  Service for syncing RoutineStep data to/from Firestore
//

import Foundation
import FirebaseFirestore
import OSLog

/// Service for syncing RoutineStep data to/from Firestore
final class FirestoreRoutineStepSyncService {
    private let db = Firestore.firestore()
    
    /// Collection path: /routines/{routineId}/steps/{stepId} (subcollection)
    private func stepDocument(routineId: String, stepId: String) -> DocumentReference {
        db.collection("routines").document(routineId).collection("steps").document(stepId)
    }
    
    /// Sync step to Firestore
    func syncToFirestore(_ step: RoutineStep) async throws {
        let stepData: [String: Any] = [
            "id": step.id,
            "routineId": step.routineId,
            "orderIndex": step.orderIndex,
            "label": step.label as Any,
            "iconName": step.iconName as Any,
            "audioCueUrl": step.audioCueUrl as Any,
            "createdAt": Timestamp(date: step.createdAt),
            "deletedAt": step.deletedAt.map { Timestamp(date: $0) } as Any
        ]
        
        try await stepDocument(routineId: step.routineId, stepId: step.id)
            .setData(stepData, merge: true)
        
        AppLogger.database.info("Synced step to Firestore: \(step.id) (routine: \(step.routineId))")
    }
    
    /// Sync step from Firestore
    func syncFromFirestore(routineId: String, stepId: String) async throws -> RoutineStep? {
        let document = try await stepDocument(routineId: routineId, stepId: stepId).getDocument()
        
        guard document.exists,
              let data = document.data() else {
            return nil
        }
        
        return try parseStep(from: data, id: document.documentID)
    }
    
    /// Get all steps for a routine
    func getAllSteps(routineId: String) async throws -> [RoutineStep] {
        let snapshot = try await db.collection("routines")
            .document(routineId)
            .collection("steps")
            .getDocuments()
        
        return try snapshot.documents.compactMap { document in
            guard let data = document.data() as? [String: Any] else {
                return nil
            }
            return try parseStep(from: data, id: document.documentID)
        }
    }
    
    /// Parse Firestore document data into RoutineStep
    private func parseStep(from data: [String: Any], id: String) throws -> RoutineStep {
        guard let routineId = data["routineId"] as? String,
              let orderIndex = data["orderIndex"] as? Int,
              let createdAtTimestamp = data["createdAt"] as? Timestamp else {
            throw SyncError.invalidData("Missing required fields for RoutineStep")
        }
        
        let label = data["label"] as? String
        let iconName = data["iconName"] as? String
        let audioCueUrl = data["audioCueUrl"] as? String
        let deletedAtTimestamp = data["deletedAt"] as? Timestamp
        
        return RoutineStep(
            id: id,
            routineId: routineId,
            orderIndex: orderIndex,
            label: label,
            iconName: iconName,
            audioCueUrl: audioCueUrl,
            createdAt: createdAtTimestamp.dateValue(),
            deletedAt: deletedAtTimestamp?.dateValue()
        )
    }
}
