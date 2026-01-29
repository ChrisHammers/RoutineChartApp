package com.HammersTech.RoutineChart.core.data.remote.firebase

import com.HammersTech.RoutineChart.core.data.local.repositories.RoomFamilyRepository
import com.HammersTech.RoutineChart.core.domain.models.Family
import com.HammersTech.RoutineChart.core.domain.repositories.FamilyRepository
import com.HammersTech.RoutineChart.core.utils.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Composite repository that uses Room as source of truth and syncs to Firestore
 * Phase 2.3: Firestore Sync (Families)
 */
@Singleton
class CompositeFamilyRepository
    @Inject
    constructor(
        private val localRepo: RoomFamilyRepository,
        private val syncService: FirestoreFamilySyncService,
    ) : FamilyRepository {
        private val syncScope = CoroutineScope(Dispatchers.IO)

        // MARK: - Repository Methods (Local-First)

        override suspend fun create(family: Family) {
            // Always write to local first (offline-first)
            localRepo.create(family)

            // Sync to Firestore asynchronously (don't block on network)
            syncScope.launch {
                try {
                    syncService.syncToFirestore(family)
                    AppLogger.Database.info("Synced family to Firestore: ${family.id}")
                } catch (e: Exception) {
                    // Log error but don't fail - local operation succeeded
                    AppLogger.Database.error("Failed to sync family to Firestore: ${e.message}", e)
                }
            }
        }

        override suspend fun getById(id: String): Family? {
            // Always read from local (offline-first)
            return localRepo.getById(id)
        }

        override fun observeById(id: String): Flow<Family?> {
            // Always read from local (offline-first)
            return localRepo.observeById(id)
        }

        override suspend fun getFirst(): Family? {
            // Always read from local (offline-first)
            return localRepo.getFirst()
        }

        override suspend fun getAll(): List<Family> {
            // Always read from local (offline-first)
            return localRepo.getAll()
        }

        override suspend fun update(family: Family) {
            // Always write to local first (offline-first)
            localRepo.update(family)

            // Sync to Firestore asynchronously
            syncScope.launch {
                try {
                    syncService.syncToFirestore(family)
                    AppLogger.Database.info("Synced family update to Firestore: ${family.id}")
                } catch (e: Exception) {
                    AppLogger.Database.error("Failed to sync family update to Firestore: ${e.message}", e)
                }
            }
        }

        /**
         * Sync family from Firestore to local database
         * Used for initial sync or when going online
         */
        suspend fun syncFromFirestore(familyId: String) {
            try {
                val firestoreFamily = syncService.syncFromFirestore(familyId)
                if (firestoreFamily != null) {
                    // Check if family exists locally
                    val localFamily = localRepo.getById(familyId)
                    if (localFamily != null) {
                        // Use the more recent data (by updatedAt)
                        // For now, prefer Firestore data (assume it's more authoritative)
                        localRepo.update(firestoreFamily)
                    } else {
                        // New family from Firestore, add to local
                        localRepo.create(firestoreFamily)
                    }

                    AppLogger.Database.info("Synced family from Firestore: $familyId")
                }
            } catch (e: Exception) {
                AppLogger.Database.error("Failed to sync family from Firestore: ${e.message}", e)
                throw e
            }
        }
    }
