//
//  CompositeFamilyRepository.swift
//  RoutineChart
//
//  Created for Phase 2.3: Firestore Sync (Families)
//  Composite repository: SQLite (source of truth) + Firestore sync
//

import Foundation
import OSLog

/// Composite repository that uses SQLite as source of truth and syncs to Firestore
final class CompositeFamilyRepository: FamilyRepository {
    private let localRepo: SQLiteFamilyRepository
    private let syncService: FirestoreFamilySyncService
    
    init(
        localRepo: SQLiteFamilyRepository = SQLiteFamilyRepository(),
        syncService: FirestoreFamilySyncService = FirestoreFamilySyncService()
    ) {
        self.localRepo = localRepo
        self.syncService = syncService
    }
    
    // MARK: - Repository Methods (Local-First)
    
    func create(_ family: Family) async throws {
        // Always write to local first (offline-first)
        try await localRepo.create(family)
        
        // Sync to Firestore asynchronously (don't block on network)
        Task {
            do {
                try await syncService.syncToFirestore(family)
                AppLogger.database.info("Synced family to Firestore: \(family.id)")
            } catch {
                // Log error but don't fail - local operation succeeded
                AppLogger.database.error("Failed to sync family to Firestore: \(error.localizedDescription)")
            }
        }
    }
    
    func get(id: String) async throws -> Family? {
        // Always read from local (offline-first)
        return try await localRepo.get(id: id)
    }
    
    func getAll() async throws -> [Family] {
        // Always read from local (offline-first)
        return try await localRepo.getAll()
    }
    
    func update(_ family: Family) async throws {
        // Always write to local first (offline-first)
        try await localRepo.update(family)
        
        // Sync to Firestore asynchronously
        Task {
            do {
                try await syncService.syncToFirestore(family)
                AppLogger.database.info("Synced family update to Firestore: \(family.id)")
            } catch {
                AppLogger.database.error("Failed to sync family update to Firestore: \(error.localizedDescription)")
            }
        }
    }
    
    // MARK: - Sync Methods
    
    /// Sync family from Firestore to local database
    /// Used for initial sync or when going online
    func syncFromFirestore(familyId: String) async throws {
        do {
            if let firestoreFamily = try await syncService.syncFromFirestore(familyId: familyId) {
                // Check if family exists locally
                if let localFamily = try await localRepo.get(id: familyId) {
                    // Use the more recent data (by updatedAt)
                    // For now, prefer Firestore data (assume it's more authoritative)
                    try await localRepo.update(firestoreFamily)
                } else {
                    // New family from Firestore, add to local
                    try await localRepo.create(firestoreFamily)
                }
                
                AppLogger.database.info("Synced family from Firestore: \(familyId)")
            }
        } catch {
            AppLogger.database.error("Failed to sync family from Firestore: \(error.localizedDescription)")
            throw error
        }
    }
}
