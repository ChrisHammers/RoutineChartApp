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
        let docRef = familyDocument(familyId: familyId)
        AppLogger.database.info("🔍 Fetching Family from Firestore: \(docRef.path)")
        
        do {
            let document = try await docRef.getDocument(source: .server)
            
            if !document.exists {
                AppLogger.database.error("❌ Family document does not exist at path: \(docRef.path)")
                AppLogger.database.error("   Attempted to fetch: families/\(familyId)")
                return nil
            }
        
        guard let data = document.data() else {
            AppLogger.database.error("❌ Family document exists but has no data at path: \(docRef.path)")
            return nil
        }
        
            AppLogger.database.info("✅ Found Family document in Firestore: \(familyId), data keys: \(data.keys.joined(separator: ", "))")
            
            do {
                return try parseFamily(from: data, id: document.documentID)
            } catch {
                AppLogger.database.error("❌ Failed to parse Family data: \(error.localizedDescription), data: \(data)")
                throw error
            }
        } catch let error as NSError {
            AppLogger.database.error("❌ Error fetching Family from Firestore: \(error.localizedDescription)")
            AppLogger.database.error("   Error domain: \(error.domain), code: \(error.code)")
            AppLogger.database.error("   UserInfo: \(error.userInfo)")
            
            // Check if it's a permission error
            if error.domain == "FIRFirestoreErrorDomain" {
                if error.code == 7 { // Permission denied
                    AppLogger.database.error("🚫 PERMISSION DENIED: Firestore security rules are blocking read access to 'families' collection")
                    AppLogger.database.error("   Please check your Firestore security rules for the 'families' collection")
                }
            }
            throw error
        }
    }
    
    /// Parse Firestore document data into Family
    private func parseFamily(from data: [String: Any], id: String) throws -> Family {
        guard let timeZone = data["timeZone"] as? String,
              let weekStartsOn = data["weekStartsOn"] as? Int,
              let planTierRaw = data["planTier"] as? String,
              let planTier = PlanTier(rawValue: planTierRaw),
              let createdAtTimestamp = data["createdAt"] as? Timestamp,
              let updatedAtTimestamp = data["updatedAt"] as? Timestamp else {
            throw SyncError.invalidData("Missing required fields for Family")
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
