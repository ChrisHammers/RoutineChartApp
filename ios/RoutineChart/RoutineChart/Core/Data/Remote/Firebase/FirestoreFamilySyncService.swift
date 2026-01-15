//
//  FirestoreFamilySyncService.swift
//  RoutineChart
//
//  Created for Phase 2.3: Firestore Sync (Families)
//

import Foundation
import FirebaseFirestore
import OSLog

/// Service for syncing Family data to/from Firestore
final class FirestoreFamilySyncService {
    private let db = Firestore.firestore()
    
    /// Collection path: /families/{familyId}
    private func familyDocument(familyId: String) -> DocumentReference {
        db.collection("families").document(familyId)
    }
    
    /// Sync family to Firestore
    func syncToFirestore(_ family: Family) async throws {
        let familyData: [String: Any] = [
            "id": family.id,
            "name": family.name as Any,
            "timeZone": family.timeZone,
            "weekStartsOn": family.weekStartsOn,
            "planTier": family.planTier.rawValue,
            "createdAt": Timestamp(date: family.createdAt),
            "updatedAt": Timestamp(date: family.updatedAt)
        ]
        
        try await familyDocument(familyId: family.id)
            .setData(familyData, merge: true)
        
        AppLogger.database.info("Synced family to Firestore: \(family.id)")
    }
    
    /// Sync family from Firestore
    func syncFromFirestore(familyId: String) async throws -> Family? {
        let document = try await familyDocument(familyId: familyId).getDocument()
        
        guard document.exists,
              let data = document.data() else {
            return nil
        }
        
        return try parseFamily(from: data, id: document.documentID)
    }
    
    /// Parse Firestore document data into Family
    private func parseFamily(from data: [String: Any], id: String) throws -> Family {
        guard let timeZone = data["timeZone"] as? String,
              let weekStartsOn = data["weekStartsOn"] as? Int,
              let planTierRaw = data["planTier"] as? String,
              let planTier = PlanTier(rawValue: planTierRaw),
              let createdAtTimestamp = data["createdAt"] as? Timestamp,
              let updatedAtTimestamp = data["updatedAt"] as? Timestamp else {
            throw SyncError.invalidData
        }
        
        let name = data["name"] as? String
        let createdAt = createdAtTimestamp.dateValue()
        let updatedAt = updatedAtTimestamp.dateValue()
        
        return Family(
            id: id,
            name: name,
            timeZone: timeZone,
            weekStartsOn: weekStartsOn,
            planTier: planTier,
            createdAt: createdAt,
            updatedAt: updatedAt
        )
    }
}
