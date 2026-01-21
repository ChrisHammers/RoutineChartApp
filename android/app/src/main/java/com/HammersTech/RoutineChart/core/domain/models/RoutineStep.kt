package com.HammersTech.RoutineChart.core.domain.models

import java.time.Instant

/**
 * RoutineStep domain model
 * Represents a single step within a routine
 * Steps are queried by routineId only, so they don't need userId or familyId
 */
data class RoutineStep(
    val id: String,
    val routineId: String,
    val orderIndex: Int,
    val label: String?,
    val iconName: String?,
    val audioCueUrl: String?,
    val createdAt: Instant,
    val deletedAt: Instant?
) {
    val isDeleted: Boolean
        get() = deletedAt != null
}

