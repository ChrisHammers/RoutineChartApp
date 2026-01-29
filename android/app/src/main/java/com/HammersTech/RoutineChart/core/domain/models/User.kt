package com.HammersTech.RoutineChart.core.domain.models

import java.time.Instant

/**
 * User domain model
 * Represents a family member with authentication credentials
 */
data class User(
    val id: String,
    val familyId: String,
    val role: Role,
    val displayName: String,
    val email: String?,
    val createdAt: Instant,
)
