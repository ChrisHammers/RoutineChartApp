//
//  CompositeFamilyInviteRepository.swift
//  RoutineChart
//
//  Created for Phase 2.3.3: Firestore Sync (Invites)
//  Composite repository: SQLite (source of truth) + Firestore sync
//

import Foundation
import OSLog

/// Composite repository that uses SQLite as source of truth and syncs to Firestore
final class CompositeFamilyInviteRepository: FamilyInviteRepository {
    private let localRepo: SQLiteFamilyInviteRepository
    private let syncService: FirestoreFamilyInviteSyncService
    
    init(
        localRepo: SQLiteFamilyInviteRepository = SQLiteFamilyInviteRepository(),
        syncService: FirestoreFamilyInviteSyncService = FirestoreFamilyInviteSyncService()
    ) {
        self.localRepo = localRepo
        self.syncService = syncService
    }
    
    // MARK: - Repository Methods (Local-First)
    
    func create(_ invite: FamilyInvite) async throws {
        // Always write to local first (offline-first)
        try await localRepo.create(invite)
        
        // Sync to Firestore asynchronously (don't block on network)
        Task {
            do {
                try await syncService.syncToFirestore(invite)
                AppLogger.database.info("Synced invite to Firestore: \(invite.id)")
            } catch {
                // Log error but don't fail - local operation succeeded
                AppLogger.database.error("Failed to sync invite to Firestore: \(error.localizedDescription)")
            }
        }
    }
    
    func get(id: String) async throws -> FamilyInvite? {
        // Always read from local (offline-first)
        return try await localRepo.get(id: id)
    }
    
    func getByToken(_ token: String) async throws -> FamilyInvite? {
        // Always read from local (offline-first)
        return try await localRepo.getByToken(token)
    }
    
    func getByInviteCode(_ inviteCode: String) async throws -> FamilyInvite? {
        // Always read from local (offline-first)
        return try await localRepo.getByInviteCode(inviteCode)
    }
    
    func getActiveInvites(familyId: String) async throws -> [FamilyInvite] {
        // Always read from local (offline-first)
        return try await localRepo.getActiveInvites(familyId: familyId)
    }
    
    func update(_ invite: FamilyInvite) async throws {
        // Always write to local first (offline-first)
        try await localRepo.update(invite)
        
        // Sync to Firestore asynchronously
        Task {
            do {
                try await syncService.syncToFirestore(invite)
                AppLogger.database.info("Synced invite update to Firestore: \(invite.id)")
            } catch {
                AppLogger.database.error("Failed to sync invite update to Firestore: \(error.localizedDescription)")
            }
        }
    }
    
    func deactivate(id: String) async throws {
        // Always write to local first
        try await localRepo.deactivate(id: id)
        
        // Get the updated invite and sync
        if let invite = try await localRepo.get(id: id) {
            Task {
                do {
                    try await syncService.syncToFirestore(invite)
                    AppLogger.database.info("Synced invite deactivation to Firestore: \(id)")
                } catch {
                    AppLogger.database.error("Failed to sync invite deactivation to Firestore: \(error.localizedDescription)")
                }
            }
        }
    }
    
    func deleteExpired() async throws {
        // Local operation only - expired invites cleanup
        try await localRepo.deleteExpired()
        // Note: Firestore can handle TTL or we sync deletion separately if needed
    }
    
    // MARK: - Sync Methods
    
    /// Sync invites from Firestore to local database
    /// Used for initial sync or when going online
    func syncFromFirestore(familyId: String) async throws {
        do {
            let firestoreInvites = try await syncService.syncFromFirestore(familyId: familyId)
            
            // Merge Firestore data into local database
            for invite in firestoreInvites {
                // Check if invite exists locally
                if let localInvite = try await localRepo.get(id: invite.id) {
                    // Use the more recent data (by updatedAt or createdAt)
                    // For now, prefer Firestore data (assume it's more authoritative)
                    try await localRepo.update(invite)
                } else {
                    // New invite from Firestore, add to local
                    try await localRepo.create(invite)
                }
            }
            
            AppLogger.database.info("Synced \(firestoreInvites.count) invites from Firestore for family: \(familyId)")
        } catch {
            AppLogger.database.error("Failed to sync invites from Firestore: \(error.localizedDescription)")
            throw error
        }
    }
}

