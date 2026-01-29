package com.HammersTech.RoutineChart.core.data.remote.firebase

import com.HammersTech.RoutineChart.core.data.local.repositories.RoomUserRepository
import com.HammersTech.RoutineChart.core.domain.models.User
import com.HammersTech.RoutineChart.core.domain.repositories.FamilyRepository
import com.HammersTech.RoutineChart.core.domain.repositories.UserRepository
import com.HammersTech.RoutineChart.core.utils.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Composite repository that uses Room as source of truth and syncs to Firestore
 * Phase 2.3.4: Firestore Sync (Users)
 */
@Singleton
class CompositeUserRepository
    @Inject
    constructor(
        private val localRepo: RoomUserRepository,
        private val syncService: FirestoreUserSyncService,
        private val familyRepo: FamilyRepository,
    ) : UserRepository {
        private val syncScope = CoroutineScope(Dispatchers.IO)

        // MARK: - Repository Methods (Local-First)

        override suspend fun create(user: User) {
            // Always write to local first (offline-first)
            localRepo.create(user)

            // Sync to Firestore asynchronously (don't block on network)
            syncScope.launch {
                try {
                    syncService.syncToFirestore(user)
                    AppLogger.Database.info("Synced user to Firestore: ${user.id}")
                } catch (e: Exception) {
                    // Log error but don't fail - local operation succeeded
                    AppLogger.Database.error("Failed to sync user to Firestore: ${e.message}", e)
                }
            }
        }

        override suspend fun getById(id: String): User? {
            // Always read from local (offline-first)
            return localRepo.getById(id)
        }

        override fun observeById(id: String): Flow<User?> {
            // Always read from local (offline-first)
            return localRepo.observeById(id)
        }

        override suspend fun getByFamilyId(familyId: String): List<User> {
            // Always read from local (offline-first)
            return localRepo.getByFamilyId(familyId)
        }

        override fun observeByFamilyId(familyId: String): Flow<List<User>> {
            // Always read from local (offline-first)
            return localRepo.observeByFamilyId(familyId)
        }

        override suspend fun update(user: User) {
            // Always write to local first (offline-first)
            localRepo.update(user)

            // Sync to Firestore asynchronously
            syncScope.launch {
                try {
                    syncService.syncToFirestore(user)
                    AppLogger.Database.info("Synced user update to Firestore: ${user.id}")
                } catch (e: Exception) {
                    AppLogger.Database.error("Failed to sync user update to Firestore: ${e.message}", e)
                }
            }
        }

        override suspend fun updateFamilyId(
            userId: String,
            familyId: String,
        ) {
            // Always write to local first
            localRepo.updateFamilyId(userId, familyId)

            // Get the updated user and sync
            val user = localRepo.getById(userId)
            if (user != null) {
                syncScope.launch {
                    try {
                        syncService.syncToFirestore(user)
                        AppLogger.Database.info("Synced user familyId update to Firestore: $userId")
                    } catch (e: Exception) {
                        AppLogger.Database.error("Failed to sync user familyId update to Firestore: ${e.message}", e)
                    }
                }
            }
        }

        /**
         * Explicitly sync user to Firestore
         * Used to ensure user document exists before routine uploads
         */
        suspend fun syncToFirestore(user: User) {
            try {
                syncService.syncToFirestore(user)
                AppLogger.Database.info("✅ Explicitly synced user to Firestore: ${user.id}")
            } catch (e: Exception) {
                AppLogger.Database.error("Failed to sync user to Firestore: ${e.message}", e)
                throw e
            }
        }

        /**
         * Sync user from Firestore to local database
         * Used for initial sync or when going online
         * IMPORTANT: Syncs the Family first to satisfy foreign key constraints
         * If the Family doesn't exist in Firestore, the User sync will fail (data integrity issue)
         */
        suspend fun syncFromFirestore(userId: String) {
            try {
                val firestoreUser = syncService.syncFromFirestore(userId)
                if (firestoreUser == null) {
                    AppLogger.Database.info("User not found in Firestore: $userId")
                    return
                }

                AppLogger.Database.info("🔄 Syncing user from Firestore: $userId, familyId: ${firestoreUser.familyId}")

                // CRITICAL: Sync Family from Firestore FIRST to satisfy foreign key constraint
                // The User references a Family, so the Family must exist locally before we can update/create the User
                // Firestore is source of truth - use the familyId from Firestore User
                if (familyRepo is CompositeFamilyRepository) {
                    // Check if Family exists locally first
                    AppLogger.Database.info("🔍 Checking if Family exists locally: ${firestoreUser.familyId}")
                    var existingFamily = familyRepo.getById(firestoreUser.familyId)
                    if (existingFamily == null) {
                        // Family doesn't exist locally - try to sync it from Firestore
                        AppLogger.Database.info("🔄 Family ${firestoreUser.familyId} not found locally, syncing from Firestore...")
                        try {
                            familyRepo.syncFromFirestore(firestoreUser.familyId)

                            // VERIFY: Check that Family was actually saved to local database
                            existingFamily = familyRepo.getById(firestoreUser.familyId)
                            if (existingFamily == null) {
                                // Family sync reported success but Family still not found locally
                                // This indicates a data integrity issue - the Family doesn't exist in Firestore
                                val errorMsg = "Family ${firestoreUser.familyId} does not exist in Firestore. Cannot sync User $userId - data integrity issue."
                                AppLogger.Database.error("❌ $errorMsg")
                                throw IllegalStateException(errorMsg)
                            } else {
                                AppLogger.Database.info("✅ Verified family exists locally after sync: ${firestoreUser.familyId}")
                            }
                        } catch (e: Exception) {
                            // Family sync failed - this is a data integrity issue
                            // The User references a Family that doesn't exist in Firestore
                            // We should NOT create placeholder families - instead, fail the sync
                            val errorMsg = "Failed to sync Family ${firestoreUser.familyId} from Firestore: ${e.message}. Cannot sync User $userId - Family does not exist in Firestore (data integrity issue)."
                            AppLogger.Database.error("❌ $errorMsg")
                            throw IllegalStateException(errorMsg)
                        }
                    } else {
                        AppLogger.Database.info("✅ Family already exists locally: ${firestoreUser.familyId}")
                    }
                } else {
                    // If not using composite repo, at least verify Family exists
                    val existingFamily = familyRepo.getById(firestoreUser.familyId)
                    if (existingFamily == null) {
                        val errorMsg = "Family ${firestoreUser.familyId} does not exist locally and cannot be synced (not using CompositeFamilyRepository)"
                        AppLogger.Database.error("❌ $errorMsg")
                        throw IllegalStateException(errorMsg)
                    }
                }

                // Use Firestore User data (source of truth)
                val userToSave = firestoreUser

                // Now safe to update/create User (Family exists locally and verified)
                val localUser = localRepo.getById(userId)
                if (localUser != null) {
                    // Log if familyId is changing
                    if (localUser.familyId != userToSave.familyId) {
                        AppLogger.Database.info("🔄 Updating user familyId: ${localUser.familyId} → ${userToSave.familyId}")
                    }

                    // Update local with Firestore data (assume Firestore is more authoritative)
                    localRepo.update(userToSave)
                    AppLogger.Database.info("✅ Updated user from Firestore: $userId")
                } else {
                    // New user from Firestore, add to local
                    localRepo.create(userToSave)
                    AppLogger.Database.info("✅ Created user from Firestore: $userId with familyId: ${userToSave.familyId}")
                }

                AppLogger.Database.info("✅ Synced user from Firestore: $userId")
            } catch (e: Exception) {
                AppLogger.Database.error("❌ Failed to sync user from Firestore: ${e.message}", e)
                throw e
            }
        }
    }
