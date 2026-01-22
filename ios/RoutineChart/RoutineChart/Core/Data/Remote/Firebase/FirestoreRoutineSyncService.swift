//
//  FirestoreRoutineSyncService.swift
//  RoutineChart
//
//  Created for Phase 3.2: Upload Queue (Routines)
//  Service for syncing Routine data to/from Firestore
//

import Foundation
import FirebaseFirestore
import OSLog

/// Service for syncing Routine data to/from Firestore
final class FirestoreRoutineSyncService {
    private let db = Firestore.firestore()
    
    /// Collection path: /routines/{routineId} (top-level collection)
    private func routineDocument(routineId: String) -> DocumentReference {
        db.collection("routines").document(routineId)
    }
    
    /// Sync routine to Firestore
    func syncToFirestore(_ routine: Routine) async throws {
        var routineData: [String: Any] = [
            "id": routine.id,
            "userId": routine.userId,
            "title": routine.title,
            "iconName": routine.iconName as Any,
            "version": routine.version,
            "completionRule": routine.completionRule.rawValue,
            "createdAt": Timestamp(date: routine.createdAt),
            "updatedAt": Timestamp(date: routine.updatedAt),
            "deletedAt": routine.deletedAt.map { Timestamp(date: $0) } as Any
        ]
        
        // Only include familyId if it's not nil
        if let familyId = routine.familyId {
            routineData["familyId"] = familyId
        }
        
        try await routineDocument(routineId: routine.id)
            .setData(routineData, merge: true)
        
        AppLogger.database.info("Synced routine to Firestore: \(routine.id)")
    }
    
    /// Sync routine from Firestore
    func syncFromFirestore(routineId: String) async throws -> Routine? {
        let document = try await routineDocument(routineId: routineId).getDocument()
        
        guard document.exists,
              let data = document.data() else {
            return nil
        }
        
        return try parseRoutine(from: data, id: document.documentID)
    }
    
    /// Get all routines for a user or family updated since a timestamp
    /// If both userId and familyId are provided, queries both and combines results (deduplicated)
    func getRoutinesUpdatedSince(userId: String?, familyId: String?, since: Date) async throws -> [Routine] {
        var allRoutines: [Routine] = []
        var routineIds = Set<String>() // For deduplication
        
        // Query by userId if provided (gets user's personal + family routines)
        if let userId = userId {
            let collectionRef = db.collection("routines")
            let query = collectionRef
                .whereField("userId", isEqualTo: userId)
                .whereField("updatedAt", isGreaterThan: Timestamp(date: since))
                .order(by: "updatedAt", descending: false)
            
            let snapshot = try await query.getDocuments()
            for document in snapshot.documents {
                guard let data = document.data() as? [String: Any],
                      let routine = try? parseRoutine(from: data, id: document.documentID) else {
                    continue
                }
                if !routineIds.contains(routine.id) {
                    allRoutines.append(routine)
                    routineIds.insert(routine.id)
                }
            }
        }
        
        // Query by familyId if provided (gets all family routines, including other users')
        if let familyId = familyId {
            let collectionRef = db.collection("routines")
            let query = collectionRef
                .whereField("familyId", isEqualTo: familyId)
                .whereField("updatedAt", isGreaterThan: Timestamp(date: since))
                .order(by: "updatedAt", descending: false)
            
            let snapshot = try await query.getDocuments()
            for document in snapshot.documents {
                guard let data = document.data() as? [String: Any],
                      let routine = try? parseRoutine(from: data, id: document.documentID) else {
                    continue
                }
                if !routineIds.contains(routine.id) {
                    allRoutines.append(routine)
                    routineIds.insert(routine.id)
                }
            }
        }
        
        // Sort by updatedAt (ascending) to maintain order
        return allRoutines.sorted { $0.updatedAt < $1.updatedAt }
    }
    
    /// Parse Firestore document data into Routine
    private func parseRoutine(from data: [String: Any], id: String) throws -> Routine {
        guard let userId = data["userId"] as? String,
              let title = data["title"] as? String,
              let version = data["version"] as? Int,
              let completionRuleRaw = data["completionRule"] as? String,
              let completionRule = CompletionRule(rawValue: completionRuleRaw),
              let createdAtTimestamp = data["createdAt"] as? Timestamp,
              let updatedAtTimestamp = data["updatedAt"] as? Timestamp else {
            throw SyncError.invalidData("Missing required fields for Routine")
        }
        
        let familyId = data["familyId"] as? String
        let iconName = data["iconName"] as? String
        let deletedAtTimestamp = data["deletedAt"] as? Timestamp
        
        return Routine(
            id: id,
            userId: userId,
            familyId: familyId,
            title: title,
            iconName: iconName,
            version: version,
            completionRule: completionRule,
            createdAt: createdAtTimestamp.dateValue(),
            updatedAt: updatedAtTimestamp.dateValue(),
            deletedAt: deletedAtTimestamp?.dateValue()
        )
    }
}

