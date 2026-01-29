package com.HammersTech.RoutineChart.core.domain.repositories

import com.HammersTech.RoutineChart.core.domain.models.AuthUser
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication operations
 * Phase 2.1: Firebase Auth
 */
interface AuthRepository {
    /**
     * Flow of the current authenticated user (null if not authenticated)
     */
    val authStateFlow: Flow<AuthUser?>

    /**
     * Current authenticated user (null if not authenticated)
     */
    val currentUser: AuthUser?

    /**
     * Sign in with email and password (parent flow)
     */
    suspend fun signInWithEmail(
        email: String,
        password: String,
    ): Result<AuthUser>

    /**
     * Sign up with email and password (parent flow)
     */
    suspend fun signUpWithEmail(
        email: String,
        password: String,
    ): Result<AuthUser>

    /**
     * Sign in anonymously (child flow)
     */
    suspend fun signInAnonymously(): Result<AuthUser>

    /**
     * Link anonymous account to email (upgrade child to parent)
     */
    suspend fun linkAnonymousToEmail(
        email: String,
        password: String,
    ): Result<AuthUser>

    /**
     * Sign out
     */
    suspend fun signOut(): Result<Unit>

    /**
     * Send password reset email
     */
    suspend fun sendPasswordReset(email: String): Result<Unit>
}
