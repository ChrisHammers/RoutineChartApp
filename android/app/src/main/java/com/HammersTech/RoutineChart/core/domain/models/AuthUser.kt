package com.HammersTech.RoutineChart.core.domain.models

/**
 * Represents an authenticated user (separate from domain User)
 * Phase 2.1: Firebase Auth
 */
data class AuthUser(
    val id: String, // Firebase UID
    val email: String?,
    val isAnonymous: Boolean = false,
)
