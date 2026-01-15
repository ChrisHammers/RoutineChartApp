//
//  CompositeUserRepository.swift
//  RoutineChart
//
//  Created for Phase 2.3.4: Firestore Sync (Users)
//  Composite repository: SQLite (source of truth) + Firestore sync
//

import Foundation
import GRDB
import OSLog

/// Composite repository that uses SQLite as source of truth and syncs to Firestore
final class CompositeUserRepository: UserRepository {
    private let localRepo: SQLiteUserRepository
    private let syncService: FirestoreUserSyncService
    private let familyRepo: FamilyRepository
    
    init(
        localRepo: SQLiteUserRepository = SQLiteUserRepository(),
        syncService: FirestoreUserSyncService = FirestoreUserSyncService(),
        familyRepo: FamilyRepository
    ) {
        self.localRepo = localRepo
        self.syncService = syncService
        self.familyRepo = familyRepo
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
    /// IMPORTANT: Syncs the Family first to satisfy foreign key constraints
    func syncFromFirestore(userId: String) async throws {
        do {
            guard let firestoreUser = try await syncService.syncFromFirestore(userId: userId) else {
                AppLogger.database.info("User not found in Firestore: \(userId)")
                return
            }
            
            AppLogger.database.info("🔄 Syncing user from Firestore: \(userId), familyId: \(firestoreUser.familyId)")
            
            // Log local User's familyId for comparison
            if let localUser = try await localRepo.get(id: userId) {
                AppLogger.database.info("📋 Local User familyId: \(localUser.familyId)")
            } else {
                AppLogger.database.info("📋 No local User found")
            }
            
            // Log all local families
            let allLocalFamilies = try await familyRepo.getAll()
            AppLogger.database.info("📋 Local families found: \(allLocalFamilies.count)")
            for family in allLocalFamilies {
                AppLogger.database.info("   - Family ID: \(family.id), name: \(family.name ?? "nil")")
            }
            
            // CRITICAL: Sync Family from Firestore FIRST to satisfy foreign key constraint
            // The User references a Family, so the Family must exist locally before we can update/create the User
            // Firestore is source of truth - use the familyId from Firestore User
            if let compositeFamilyRepo = familyRepo as? CompositeFamilyRepository {
                do {
                    // Check if Family exists locally first
                    AppLogger.database.info("🔍 Checking if Family exists locally: \(firestoreUser.familyId)")
                    var existingFamily = try await familyRepo.get(id: firestoreUser.familyId)
                    if existingFamily == nil {
                        // Family doesn't exist locally - try to sync it from Firestore
                        AppLogger.database.info("🔄 Family \(firestoreUser.familyId) not found locally, syncing from Firestore...")
                        do {
                            try await compositeFamilyRepo.syncFromFirestore(familyId: firestoreUser.familyId)
                            
                            // VERIFY: Check that Family was actually saved to local database
                            existingFamily = try await familyRepo.get(id: firestoreUser.familyId)
                            if existingFamily == nil {
                                // Family sync reported success but Family still not found locally
                                // This could mean the Family doesn't exist in Firestore, or there was a parsing error
                                AppLogger.database.warning("⚠️ Family sync reported success but Family \(firestoreUser.familyId) still not found locally. Creating placeholder Family to satisfy foreign key constraint.")
                                
                                // Create a minimal placeholder Family so we can update the User
                                // This allows the User sync to succeed even if the Family can't be synced
                                let placeholderFamily = Family(
                                    id: firestoreUser.familyId,
                                    name: nil,
                                    timeZone: TimeZone.current.identifier,
                                    weekStartsOn: 0,
                                    planTier: .free,
                                    createdAt: Date(),
                                    updatedAt: Date()
                                )
                                try await familyRepo.create(placeholderFamily)
                                AppLogger.database.info("✅ Created placeholder Family: \(firestoreUser.familyId)")
                            } else {
                                AppLogger.database.info("✅ Verified family exists locally after sync: \(firestoreUser.familyId)")
                            }
                        } catch {
                            // Family sync failed - create a placeholder Family so User sync can proceed
                            // This allows the User to be updated with the correct familyId even if Family sync fails
                            AppLogger.database.warning("⚠️ Failed to sync family from Firestore: \(error.localizedDescription). Creating placeholder Family to allow User sync.")
                            
                            // Create a minimal placeholder Family to satisfy foreign key constraint
                            let placeholderFamily = Family(
                                id: firestoreUser.familyId,
                                name: nil,
                                timeZone: TimeZone.current.identifier,
                                weekStartsOn: 0,
                                planTier: .free,
                                createdAt: Date(),
                                updatedAt: Date()
                            )
                            try await familyRepo.create(placeholderFamily)
                            AppLogger.database.info("✅ Created placeholder Family: \(firestoreUser.familyId) to allow User sync")
                        }
                    } else {
                        AppLogger.database.info("✅ Family already exists locally: \(firestoreUser.familyId)")
                    }
                } catch {
                    AppLogger.database.error("❌ Failed to sync/verify family from Firestore: \(error.localizedDescription)")
                    throw error // Don't continue if Family sync fails - we need it for foreign key
                }
            } else {
                // If not using composite repo, at least verify Family exists
                let existingFamily = try await familyRepo.get(id: firestoreUser.familyId)
                if existingFamily == nil {
                    let errorMsg = "Family \(firestoreUser.familyId) does not exist locally and cannot be synced (not using CompositeFamilyRepository)"
                    AppLogger.database.error("❌ \(errorMsg)")
                    throw NSError(domain: "CompositeUserRepository", code: 2, userInfo: [NSLocalizedDescriptionKey: errorMsg])
                }
            }
            
            // Use Firestore User data (source of truth)
            let userToSave = firestoreUser
            
            // Now safe to update/create User (Family exists locally and verified)
            if let localUser = try await localRepo.get(id: userId) {
                // Log if familyId is changing
                if localUser.familyId != userToSave.familyId {
                    AppLogger.database.info("🔄 Updating user familyId: \(localUser.familyId) → \(userToSave.familyId)")
                }
                
                // Update local with Firestore data (assume Firestore is more authoritative)
                try await localRepo.update(userToSave)
                AppLogger.database.info("✅ Updated user from Firestore: \(userId)")
                
                // Clean up orphaned families - keep only families referenced by users
                await cleanupOrphanedFamilies(keepFamilyId: userToSave.familyId)
            } else {
                // New user from Firestore, add to local
                try await localRepo.create(userToSave)
                AppLogger.database.info("✅ Created user from Firestore: \(userId) with familyId: \(userToSave.familyId)")
                
                // Clean up orphaned families - keep only families referenced by users
                await cleanupOrphanedFamilies(keepFamilyId: userToSave.familyId)
            }
            
            AppLogger.database.info("✅ Synced user from Firestore: \(userId)")
        } catch {
            AppLogger.database.error("❌ Failed to sync user from Firestore: \(error.localizedDescription)")
            throw error
        }
    }
    
    /// Clean up orphaned families - keep only families that are referenced by users
    private func cleanupOrphanedFamilies(keepFamilyId: String) async {
        do {
            // Get all local families
            let allLocalFamilies = try await familyRepo.getAll()
            guard !allLocalFamilies.isEmpty else {
                AppLogger.database.info("✅ No families to clean up")
                return
            }
            
            var familiesInUse = Set<String>()
            familiesInUse.insert(keepFamilyId) // Always keep the current user's family
            
            // Check each family to see if any users reference it
            for family in allLocalFamilies {
                do {
                    let usersInFamily = try await localRepo.getAll(familyId: family.id)
                    if !usersInFamily.isEmpty {
                        familiesInUse.insert(family.id)
                        AppLogger.database.info("   Family \(family.id) is in use by \(usersInFamily.count) user(s)")
                    } else {
                        AppLogger.database.info("   Family \(family.id) is orphaned (no users)")
                    }
                } catch {
                    AppLogger.database.warning("⚠️ Error checking users for family \(family.id): \(error.localizedDescription)")
                    // If we can't check, keep the family to be safe
                    familiesInUse.insert(family.id)
                }
            }
            
            // Delete families not in use
            let familiesToDelete = allLocalFamilies.filter { !familiesInUse.contains($0.id) }
            if !familiesToDelete.isEmpty {
                AppLogger.database.info("🧹 Cleaning up \(familiesToDelete.count) orphaned families...")
                let db = try SQLiteManager.shared.database()
                try await db.write { db in
                    for family in familiesToDelete {
                        do {
                            try db.execute(sql: "DELETE FROM families WHERE id = ?", arguments: [family.id])
                            AppLogger.database.info("   ✅ Deleted orphaned family: \(family.id)")
                        } catch {
                            AppLogger.database.error("   ❌ Failed to delete family \(family.id): \(error.localizedDescription)")
                            // Continue with other families even if one fails
                        }
                    }
                }
                AppLogger.database.info("✅ Cleanup complete. Kept \(familiesInUse.count) families in use, deleted \(familiesToDelete.count) orphaned families")
            } else {
                AppLogger.database.info("✅ No orphaned families to clean up")
            }
        } catch {
            AppLogger.database.error("❌ Failed to cleanup orphaned families: \(error.localizedDescription)")
            // Don't throw - cleanup failure shouldn't break sync
        }
    }
}

