//
//  FirestoreUserSyncService.swift
//  RoutineChart
//
//  Created for Phase 2.3.4: Firestore Sync (Users)
//

import Foundation
import FirebaseFirestore
import OSLog

/// Service for syncing User data to/from Firestore
final class FirestoreUserSyncService {
    private let db = Firestore.firestore()
    
    /// Collection path: /users/{userId}
    private func userDocument(userId: String) -> DocumentReference {
        db.collection("users").document(userId)
    }
    
    /// Sync user to Firestore
    func syncToFirestore(_ user: User) async throws {
        let userData: [String: Any] = [
            "id": user.id,
            "familyId": user.familyId,
            "role": user.role.rawValue,
            "displayName": user.displayName,
            "email": user.email as Any,
            "createdAt": Timestamp(date: user.createdAt)
        ]
        
        try await userDocument(userId: user.id)
            .setData(userData, merge: true)
        
        AppLogger.database.info("Synced user to Firestore: \(user.id)")
    }
    
    /// Sync user from Firestore
    func syncFromFirestore(userId: String) async throws -> User? {
        let document = try await userDocument(userId: userId).getDocument()
        
        guard document.exists,
              let data = document.data() else {
            return nil
        }
        
        return try parseUser(from: data, id: document.documentID)
    }
    
    /// Parse Firestore document data into User
    private func parseUser(from data: [String: Any], id: String) throws -> User {
        guard let familyId = data["familyId"] as? String,
              let roleString = data["role"] as? String,
              let role = Role(rawValue: roleString),
              let displayName = data["displayName"] as? String,
              let createdAtTimestamp = data["createdAt"] as? Timestamp else {
            throw SyncError.invalidData
        }
        
        let email = data["email"] as? String
        let createdAt = createdAtTimestamp.dateValue()
        
        return User(
            id: id,
            familyId: familyId,
            role: role,
            displayName: displayName,
            email: email,
            createdAt: createdAt
        )
    }
}

