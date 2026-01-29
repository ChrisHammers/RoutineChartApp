package com.HammersTech.RoutineChart.core.domain.models

import java.time.Instant

/**
 * Routine domain model
 * Represents a sequence of steps to be completed
 * Routines are owned by a user (userId required) and optionally shared with a family (familyId optional)
 */
data class Routine(
    val id: String,
    val userId: String, // Owner of the routine (required)
    val familyId: String?, // Optional - if null, routine is personal; if present, routine is shared with family
    val title: String,
    val iconName: String?,
    val version: Int,
    val completionRule: CompletionRule,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
) {
    companion object {
        const val DEFAULT_VERSION = 1
    }

    val isDeleted: Boolean
        get() = deletedAt != null
}
