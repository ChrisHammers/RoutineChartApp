package com.HammersTech.RoutineChart.core.data.remote.firebase

import com.HammersTech.RoutineChart.core.domain.models.Role
import com.HammersTech.RoutineChart.core.domain.models.User
import com.HammersTech.RoutineChart.core.utils.AppLogger
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for syncing User data to/from Firestore
 * Phase 2.3.4: Firestore Sync (Users)
 */
@Singleton
class FirestoreUserSyncService
    @Inject
    constructor() {
        private val db = FirebaseFirestore.getInstance()

        /**
         * Collection path: /users/{userId}
         */
        private fun userDocument(userId: String) =
            db
                .collection("users")
                .document(userId)

        /**
         * Sync user to Firestore. Uses set(merge: true) for upsert.
         */
        suspend fun syncToFirestore(user: User) {
            try {
                val createdAtTimestamp = com.google.firebase.Timestamp(java.util.Date.from(user.createdAt))

                val data =
                    hashMapOf<String, Any>(
                        "id" to user.id,
                        "familyId" to user.familyId,
                        "role" to user.role.toRawValue(),
                        "displayName" to user.displayName,
                        "createdAt" to createdAtTimestamp,
                    )

                user.email?.let { data["email"] = it }

                userDocument(user.id)
                    .set(data, com.google.firebase.firestore.SetOptions.merge())
                    .await()

                AppLogger.Database.info("Synced user to Firestore: ${user.id}")
            } catch (e: Exception) {
                AppLogger.Database.error("Failed to sync user to Firestore", e)
                throw e
            }
        }

        /**
         * Fetches a user from Firestore by ID.
         * Uses Source.SERVER to force network request and bypass cache.
         */
        suspend fun syncFromFirestore(userId: String): User? {
            return try {
                AppLogger.Database.info("Fetching user from Firestore (server): $userId")
                val document =
                    userDocument(userId)
                        .get(Source.SERVER) // Force server read to bypass cache
                        .await()

                if (!document.exists()) {
                    AppLogger.Database.info("User document does not exist: $userId")
                    return null
                }

                val data = document.data ?: return null
                val user = parseUser(data, document.id)
                AppLogger.Database.info("Successfully fetched user from Firestore: $userId")
                user
            } catch (e: Exception) {
                AppLogger.Database.error("Failed to sync user from Firestore: $userId", e)
                // Log specific error details
                when {
                    e.message?.contains("network", ignoreCase = true) == true -> {
                        AppLogger.Database.error("Network error detected. Check internet connection and Firebase configuration.")
                    }
                    e.message?.contains("permission", ignoreCase = true) == true -> {
                        AppLogger.Database.error("Permission denied. Check Firestore security rules.")
                    }
                    else -> {
                        AppLogger.Database.error("Unknown error: ${e.message}")
                    }
                }
                throw e
            }
        }

        /**
         * Parse Firestore document data into User
         */
        private fun parseUser(
            data: Map<String, Any>,
            id: String,
        ): User {
            val familyId =
                data["familyId"] as? String
                    ?: throw IllegalArgumentException("Missing familyId")
            val roleRaw =
                data["role"] as? String
                    ?: throw IllegalArgumentException("Missing role")
            val role = Role.fromRawValue(roleRaw)
            val displayName =
                data["displayName"] as? String
                    ?: throw IllegalArgumentException("Missing displayName")
            val email = data["email"] as? String
            val createdAtTimestamp =
                data["createdAt"] as? com.google.firebase.Timestamp
                    ?: throw IllegalArgumentException("Missing createdAt")

            val createdAt = createdAtTimestamp.toDate().toInstant()

            return User(
                id = id,
                familyId = familyId,
                role = role,
                displayName = displayName,
                email = email,
                createdAt = createdAt,
            )
        }
    }
