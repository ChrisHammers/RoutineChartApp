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
/// MIGRATED: Now uses top-level /invites collection for simpler queries
final class FirestoreFamilyInviteSyncService {
    private let db = Firestore.firestore()
    
    /// Top-level invites collection: /invites/{inviteId}
    private func invitesCollection() -> CollectionReference {
        db.collection("invites")
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
        
        try await invitesCollection()
            .document(invite.id)
            .setData(inviteData, merge: true)
        
        AppLogger.database.info("Synced invite to Firestore: \(invite.id)")
    }
    
    /// Increment usedCount for an invite (optimized for security rules)
    /// Uses updateData to only update the usedCount field, which works better with security rules
    func incrementUsedCount(inviteId: String, newUsedCount: Int) async throws {
        let updateData: [String: Any] = [
            "usedCount": newUsedCount
        ]
        
        try await invitesCollection()
            .document(inviteId)
            .updateData(updateData)
        
        AppLogger.database.info("Incremented usedCount for invite \(inviteId) to \(newUsedCount)")
    }
    
    /// Sync invites from Firestore for a family
    func syncFromFirestore(familyId: String) async throws -> [FamilyInvite] {
        let snapshot = try await invitesCollection()
            .whereField("familyId", isEqualTo: familyId)
            .getDocuments(source: .server)  // Force server read to bypass cache
        
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
    
    /// Query Firestore for an invite by invite code.
    /// Uses top-level collection with direct query (much faster than collection group).
    /// Uses .server source to force network request and bypass cache.
    func getByInviteCodeFromFirestore(_ inviteCode: String) async throws -> FamilyInvite? {
        AppLogger.database.info("Querying Firestore (server) for invite code: \(inviteCode)")
        let snapshot = try await invitesCollection()
            .whereField("inviteCode", isEqualTo: inviteCode)
            .limit(to: 1)
            .getDocuments(source: .server)  // Force server read to bypass cache
        
        guard !snapshot.documents.isEmpty else {
            AppLogger.database.info("No invite found with code: \(inviteCode)")
            return nil
        }
        
        let document = snapshot.documents.first!
        let invite = try parseInvite(from: document.data(), id: document.documentID)
        AppLogger.database.info("Found invite \(invite.id) for code: \(inviteCode)")
        return invite
    }
    
    /// Query Firestore for an invite by token.
    /// Uses top-level collection with direct query (much faster than collection group).
    /// Uses .server source to force network request and bypass cache.
    func getByTokenFromFirestore(_ token: String) async throws -> FamilyInvite? {
        AppLogger.database.info("Querying Firestore (server) for invite token: \(token)")
        let snapshot = try await invitesCollection()
            .whereField("token", isEqualTo: token)
            .limit(to: 1)
            .getDocuments(source: .server)  // Force server read to bypass cache
        
        guard !snapshot.documents.isEmpty else {
            AppLogger.database.info("No invite found with token: \(token)")
            return nil
        }
        
        let document = snapshot.documents.first!
        let invite = try parseInvite(from: document.data(), id: document.documentID)
        AppLogger.database.info("Found invite \(invite.id) for token: \(token)")
        return invite
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
            throw SyncError.invalidData("Missing required fields for FamilyInvite")
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


