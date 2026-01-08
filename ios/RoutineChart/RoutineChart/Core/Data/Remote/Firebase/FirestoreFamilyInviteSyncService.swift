//
//  FirestoreFamilyInviteSyncService.swift
//  RoutineChart
//
//  Created for Phase 2.3.3: Firestore Sync (Invites)
//

import Foundation
import FirebaseFirestore
import OSLog

/// Service for syncing FamilyInvite data to/from Firestore
final class FirestoreFamilyInviteSyncService {
    private let db = Firestore.firestore()
    
    /// Collection path: /families/{familyId}/invites/{inviteId}
    private func collectionPath(for familyId: String) -> CollectionReference {
        db.collection("families").document(familyId).collection("invites")
    }
    
    /// Sync invite to Firestore
    func syncToFirestore(_ invite: FamilyInvite) async throws {
        let inviteData: [String: Any] = [
            "id": invite.id,
            "familyId": invite.familyId,
            "token": invite.token,
            "inviteCode": invite.inviteCode,
            "createdBy": invite.createdBy,
            "createdAt": Timestamp(date: invite.createdAt),
            "expiresAt": Timestamp(date: invite.expiresAt),
            "maxUses": invite.maxUses as Any,
            "usedCount": invite.usedCount,
            "isActive": invite.isActive
        ]
        
        try await collectionPath(for: invite.familyId)
            .document(invite.id)
            .setData(inviteData, merge: true)
        
        AppLogger.database.info("Synced invite to Firestore: \(invite.id)")
    }
    
    /// Sync invites from Firestore for a family
    func syncFromFirestore(familyId: String) async throws -> [FamilyInvite] {
        let snapshot = try await collectionPath(for: familyId).getDocuments()
        
        var invites: [FamilyInvite] = []
        for document in snapshot.documents {
            guard let invite = try? parseInvite(from: document.data(), id: document.documentID) else {
                AppLogger.database.error("Failed to parse invite from Firestore: \(document.documentID)")
                continue
            }
            invites.append(invite)
        }
        
        AppLogger.database.info("Synced \(invites.count) invites from Firestore for family: \(familyId)")
        return invites
    }
    
    /// Parse Firestore document data into FamilyInvite
    private func parseInvite(from data: [String: Any], id: String) throws -> FamilyInvite {
        guard let familyId = data["familyId"] as? String,
              let token = data["token"] as? String,
              let inviteCode = data["inviteCode"] as? String,
              let createdBy = data["createdBy"] as? String,
              let createdAtTimestamp = data["createdAt"] as? Timestamp,
              let expiresAtTimestamp = data["expiresAt"] as? Timestamp,
              let usedCount = data["usedCount"] as? Int,
              let isActive = data["isActive"] as? Bool else {
            throw SyncError.invalidData
        }
        
        let createdAt = createdAtTimestamp.dateValue()
        let expiresAt = expiresAtTimestamp.dateValue()
        let maxUses = data["maxUses"] as? Int
        
        return FamilyInvite(
            id: id,
            familyId: familyId,
            token: token,
            inviteCode: inviteCode,
            createdBy: createdBy,
            createdAt: createdAt,
            expiresAt: expiresAt,
            maxUses: maxUses,
            usedCount: usedCount,
            isActive: isActive
        )
    }
}


