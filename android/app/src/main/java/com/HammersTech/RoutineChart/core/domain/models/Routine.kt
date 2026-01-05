package com.HammersTech.RoutineChart.core.domain.models

import java.time.Instant

/**
 * Routine domain model
 * Represents a sequence of steps to be completed
 */
data class Routine(
    val id: String,
    val familyId: String,
    val title: String,
    val iconName: String?,
    val version: Int,
    val completionRule: CompletionRule,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?
) {
    companion object {
        const val DEFAULT_VERSION = 1
    }

    val isDeleted: Boolean
        get() = deletedAt != null
}

