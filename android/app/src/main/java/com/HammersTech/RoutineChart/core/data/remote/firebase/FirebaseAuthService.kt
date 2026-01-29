package com.HammersTech.RoutineChart.core.data.remote.firebase

import com.HammersTech.RoutineChart.core.domain.models.AuthUser
import com.HammersTech.RoutineChart.core.domain.repositories.AuthRepository
import com.HammersTech.RoutineChart.core.utils.AppLogger
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase implementation of AuthRepository
 * Phase 2.1: Firebase Auth
 */
@Singleton
class FirebaseAuthService
    @Inject
    constructor() : AuthRepository {
        private val auth = FirebaseAuth.getInstance()

        override val authStateFlow: Flow<AuthUser?> =
            callbackFlow {
                val listener =
                    FirebaseAuth.AuthStateListener { firebaseAuth ->
                        val user = firebaseAuth.currentUser?.toAuthUser()
                        trySend(user)
                    }

                auth.addAuthStateListener(listener)

                // Send initial value
                trySend(auth.currentUser?.toAuthUser())

                awaitClose {
                    auth.removeAuthStateListener(listener)
                }
            }

        override val currentUser: AuthUser?
            get() = auth.currentUser?.toAuthUser()

        override suspend fun signInWithEmail(
            email: String,
            password: String,
        ): Result<AuthUser> {
            return try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user?.toAuthUser()
                if (user != null) {
                    AppLogger.Auth.info("User signed in with email: $email")
                    Result.success(user)
                } else {
                    Result.failure(Exception("Failed to sign in"))
                }
            } catch (e: Exception) {
                AppLogger.Auth.error("Sign in failed", e)
                Result.failure(e)
            }
        }

        override suspend fun signUpWithEmail(
            email: String,
            password: String,
        ): Result<AuthUser> {
            return try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user?.toAuthUser()
                if (user != null) {
                    AppLogger.Auth.info("User signed up with email: $email")
                    Result.success(user)
                } else {
                    Result.failure(Exception("Failed to create account"))
                }
            } catch (e: Exception) {
                AppLogger.Auth.error("Sign up failed", e)
                Result.failure(e)
            }
        }

        override suspend fun signInAnonymously(): Result<AuthUser> {
            return try {
                val result = auth.signInAnonymously().await()
                val user = result.user?.toAuthUser()
                if (user != null) {
                    AppLogger.Auth.info("User signed in anonymously")
                    Result.success(user)
                } else {
                    Result.failure(Exception("Failed to sign in anonymously"))
                }
            } catch (e: Exception) {
                AppLogger.Auth.error("Anonymous sign in failed", e)
                Result.failure(e)
            }
        }

        override suspend fun linkAnonymousToEmail(
            email: String,
            password: String,
        ): Result<AuthUser> {
            return try {
                val currentUser = auth.currentUser
                if (currentUser == null || !currentUser.isAnonymous) {
                    return Result.failure(Exception("Current user is not anonymous"))
                }

                val credential = EmailAuthProvider.getCredential(email, password)
                val result = currentUser.linkWithCredential(credential).await()
                val user = result.user?.toAuthUser()
                if (user != null) {
                    AppLogger.Auth.info("Anonymous account linked to email: $email")
                    Result.success(user)
                } else {
                    Result.failure(Exception("Failed to link account"))
                }
            } catch (e: Exception) {
                AppLogger.Auth.error("Account linking failed", e)
                Result.failure(e)
            }
        }

        override suspend fun signOut(): Result<Unit> {
            return try {
                auth.signOut()
                AppLogger.Auth.info("User signed out")
                Result.success(Unit)
            } catch (e: Exception) {
                AppLogger.Auth.error("Sign out failed", e)
                Result.failure(e)
            }
        }

        override suspend fun sendPasswordReset(email: String): Result<Unit> {
            return try {
                auth.sendPasswordResetEmail(email).await()
                AppLogger.Auth.info("Password reset email sent to: $email")
                Result.success(Unit)
            } catch (e: Exception) {
                AppLogger.Auth.error("Password reset failed", e)
                Result.failure(e)
            }
        }

        private fun FirebaseUser.toAuthUser(): AuthUser {
            return AuthUser(
                id = uid,
                email = email,
                isAnonymous = isAnonymous,
            )
        }
    }
