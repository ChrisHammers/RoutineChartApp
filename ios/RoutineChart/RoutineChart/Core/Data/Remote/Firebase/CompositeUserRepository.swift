//
//  CompositeUserRepository.swift
//  RoutineChart
//
//  Created for Phase 2.3.4: Firestore Sync (Users)
//  Composite repository: SQLite (source of truth) + Firestore sync
//

import Foundation
import OSLog

/// Composite repository that uses SQLite as source of truth and syncs to Firestore
final class CompositeUserRepository: UserRepository {
    private let localRepo: SQLiteUserRepository
    private let syncService: FirestoreUserSyncService
    
    init(
        localRepo: SQLiteUserRepository = SQLiteUserRepository(),
        syncService: FirestoreUserSyncService = FirestoreUserSyncService()
    ) {
        self.localRepo = localRepo
        self.syncService = syncService
    }
    
    // MARK: - Repository Methods (Local-First)
    
    func create(_ user: User) async throws {
        // Always write to local first (offline-first)
        try await localRepo.create(user)
        
        // Sync to Firestore asynchronously (don't block on network)
        Task {
            do {
                try await syncService.syncToFirestore(user)
                AppLogger.database.info("✅ Synced user to Firestore: \(user.id)")
                print("✅ [Firestore Sync] User synced: \(user.id)")
            } catch {
                // Log error but don't fail - local operation succeeded
                let errorMsg = "Failed to sync user to Firestore: \(error.localizedDescription)"
                AppLogger.database.error("❌ \(errorMsg)")
                print("❌ [Firestore Sync Error] \(errorMsg)")
                print("❌ [Firestore Sync Error] Full error: \(error)")
                if let nsError = error as NSError? {
                    print("❌ [Firestore Sync Error] Domain: \(nsError.domain), Code: \(nsError.code)")
                    print("❌ [Firestore Sync Error] UserInfo: \(nsError.userInfo)")
                }
            }
        }
    }
    
    func get(id: String) async throws -> User? {
        // Always read from local (offline-first)
        return try await localRepo.get(id: id)
    }
    
    func update(_ user: User) async throws {
        // Always write to local first (offline-first)
        try await localRepo.update(user)
        
        // Sync to Firestore asynchronously
        Task {
            do {
                try await syncService.syncToFirestore(user)
                AppLogger.database.info("Synced user update to Firestore: \(user.id)")
            } catch {
                AppLogger.database.error("Failed to sync user update to Firestore: \(error.localizedDescription)")
            }
        }
    }
    
    func getAll(familyId: String) async throws -> [User] {
        // Always read from local (offline-first)
        return try await localRepo.getAll(familyId: familyId)
    }
    
    func updateFamilyId(userId: String, familyId: String) async throws {
        // Always write to local first
        try await localRepo.updateFamilyId(userId: userId, familyId: familyId)
        
        // Get the updated user and sync
        if let user = try await localRepo.get(id: userId) {
            Task {
                do {
                    try await syncService.syncToFirestore(user)
                    AppLogger.database.info("Synced user familyId update to Firestore: \(userId)")
                } catch {
                    AppLogger.database.error("Failed to sync user familyId update to Firestore: \(error.localizedDescription)")
                }
            }
        }
    }
    
    // MARK: - Sync Methods
    
    /// Sync user from Firestore to local database
    /// Used for initial sync or when going online
    func syncFromFirestore(userId: String) async throws {
        do {
            guard let firestoreUser = try await syncService.syncFromFirestore(userId: userId) else {
                AppLogger.database.info("User not found in Firestore: \(userId)")
                return
            }
            
            // Check if user exists locally
            if let localUser = try await localRepo.get(id: userId) {
                // Update local with Firestore data (assume Firestore is more authoritative)
                try await localRepo.update(firestoreUser)
            } else {
                // New user from Firestore, add to local
                try await localRepo.create(firestoreUser)
            }
            
            AppLogger.database.info("Synced user from Firestore: \(userId)")
        } catch {
            AppLogger.database.error("Failed to sync user from Firestore: \(error.localizedDescription)")
            throw error
        }
    }
}

