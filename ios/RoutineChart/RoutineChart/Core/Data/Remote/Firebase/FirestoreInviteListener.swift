//
//  FirestoreInviteListener.swift
//  RoutineChart
//
//  Created for Phase 2.3.5: Real-time Updates
//  Real-time listener for Firestore invite updates
//

import Foundation
import FirebaseFirestore
import Combine
import OSLog

// Reuse the parse logic - import the sync service's parse method or make it shared
// For now, we'll duplicate the parse logic since it's simple

/// Real-time listener for a single invite document
final class FirestoreInviteListener {
    private let db = Firestore.firestore()
    private var listener: ListenerRegistration?
    
    /// Publisher that emits updated invite data
    let invitePublisher: PassthroughSubject<FamilyInvite?, Never> = PassthroughSubject()
    
    /// Start listening to a specific invite
    func startListening(inviteId: String, familyId: String) {
        // Stop any existing listener
        stopListening()
        
        let docRef = db.collection("families")
            .document(familyId)
            .collection("invites")
            .document(inviteId)
        
        listener = docRef.addSnapshotListener { [weak self] documentSnapshot, error in
            guard let self = self else { return }
            
            if let error = error {
                AppLogger.database.error("Firestore invite listener error: \(error.localizedDescription)")
                self.invitePublisher.send(nil)
                return
            }
            
            guard let document = documentSnapshot,
                  document.exists,
                  let data = document.data() else {
                // Document doesn't exist or was deleted
                AppLogger.database.info("Invite document \(inviteId) no longer exists")
                self.invitePublisher.send(nil)
                return
            }
            
            do {
                let invite = try self.parseInvite(from: data, id: document.documentID)
                AppLogger.database.info("Real-time update received for invite: \(inviteId)")
                self.invitePublisher.send(invite)
            } catch {
                AppLogger.database.error("Failed to parse invite from real-time update: \(error.localizedDescription)")
                self.invitePublisher.send(nil)
            }
        }
    }
    
    /// Stop listening
    func stopListening() {
        listener?.remove()
        listener = nil
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
    
    deinit {
        stopListening()
    }
}

