//
//  CompositeFamilyRepository.swift
//  RoutineChart
//
//  Created for Phase 2.3: Firestore Sync (Families)
//  Composite repository: SQLite (source of truth) + Firestore sync
//

import Foundation
import FirebaseFirestore
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
            AppLogger.database.info("🔄 Syncing family from Firestore: \(familyId)")
            
            // Log all local families before sync
            let allLocalFamilies = try await localRepo.getAll()
            AppLogger.database.info("📋 Local families before sync: \(allLocalFamilies.count)")
            for family in allLocalFamilies {
                AppLogger.database.info("   - Local Family ID: \(family.id), name: \(family.name ?? "nil")")
            }
            
            guard let firestoreFamily = try await syncService.syncFromFirestore(familyId: familyId) else {
                AppLogger.database.error("❌ Family not found in Firestore: \(familyId)")
                
                // Try to list all families in Firestore to see what's available
                AppLogger.database.info("🔍 Attempting to list all families in Firestore...")
                do {
                    let db = Firestore.firestore()
                    let snapshot = try await db.collection("families").getDocuments(source: .server)
                    AppLogger.database.info("📋 Found \(snapshot.documents.count) families in Firestore:")
                    for doc in snapshot.documents {
                        AppLogger.database.info("   - Firestore Family ID: \(doc.documentID), exists: \(doc.exists), data keys: \(doc.data().keys.joined(separator: ", "))")
                    }
                    
                    if snapshot.documents.isEmpty {
                        AppLogger.database.warning("⚠️ No families found in Firestore. This could indicate:")
                        AppLogger.database.warning("   1. Security rules are blocking read access")
                        AppLogger.database.warning("   2. Wrong Firestore database/project")
                        AppLogger.database.warning("   3. Collection name mismatch")
                        AppLogger.database.warning("   4. Network/permission error")
                    }
                } catch let error as NSError {
                    AppLogger.database.error("❌ Failed to list Firestore families: \(error.localizedDescription)")
                    AppLogger.database.error("   Error domain: \(error.domain), code: \(error.code)")
                    AppLogger.database.error("   UserInfo: \(error.userInfo)")
                    
                    // Check if it's a permission error
                    if error.domain == "FIRFirestoreErrorDomain" {
                        if error.code == 7 { // Permission denied
                            AppLogger.database.error("🚫 PERMISSION DENIED: Firestore security rules are blocking read access to 'families' collection")
                        }
                    }
                } catch {
                    AppLogger.database.error("❌ Failed to list Firestore families: \(error.localizedDescription)")
                }
                
                throw NSError(domain: "CompositeFamilyRepository", code: 1, userInfo: [NSLocalizedDescriptionKey: "Family \(familyId) not found in Firestore"])
            }
            
            AppLogger.database.info("✅ Successfully fetched Family from Firestore: \(firestoreFamily.id), name: \(firestoreFamily.name ?? "nil")")
            
            // Check if family exists locally
            if let localFamily = try await localRepo.get(id: familyId) {
                // Use the more recent data (by updatedAt)
                // For now, prefer Firestore data (assume it's more authoritative)
                AppLogger.database.info("🔄 Updating existing family from Firestore: \(familyId)")
                try await localRepo.update(firestoreFamily)
                
                // Verify update succeeded
                if let updatedFamily = try await localRepo.get(id: familyId) {
                    AppLogger.database.info("✅ Verified family updated: \(familyId)")
                } else {
                    AppLogger.database.error("❌ Family update failed - family not found after update: \(familyId)")
                    throw NSError(domain: "CompositeFamilyRepository", code: 2, userInfo: [NSLocalizedDescriptionKey: "Family update failed - family not found after update"])
                }
            } else {
                // New family from Firestore, add to local
                AppLogger.database.info("🔄 Creating new family from Firestore: \(familyId)")
                try await localRepo.create(firestoreFamily)
                
                // Verify create succeeded
                if let createdFamily = try await localRepo.get(id: familyId) {
                    AppLogger.database.info("✅ Verified family created: \(familyId)")
                } else {
                    AppLogger.database.error("❌ Family create failed - family not found after create: \(familyId)")
                    throw NSError(domain: "CompositeFamilyRepository", code: 3, userInfo: [NSLocalizedDescriptionKey: "Family create failed - family not found after create"])
                }
            }
            
            AppLogger.database.info("✅ Synced family from Firestore: \(familyId)")
            
            // Log all local families after sync
            let allLocalFamiliesAfter = try await localRepo.getAll()
            AppLogger.database.info("📋 Local families after sync: \(allLocalFamiliesAfter.count)")
            for family in allLocalFamiliesAfter {
                AppLogger.database.info("   - Local Family ID: \(family.id), name: \(family.name ?? "nil")")
            }
        } catch {
            AppLogger.database.error("❌ Failed to sync family from Firestore: \(familyId) - \(error.localizedDescription)")
            throw error
        }
    }
}
