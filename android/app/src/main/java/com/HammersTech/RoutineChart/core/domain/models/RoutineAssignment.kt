package com.HammersTech.RoutineChart.core.domain.models

import java.time.Instant

/**
 * RoutineAssignment domain model
 * Links a routine to a child profile
 */
data class RoutineAssignment(
    val id: String,
    val familyId: String,
    val routineId: String,
    val childId: String,
    val isActive: Boolean,
    val assignedAt: Instant,
    val deletedAt: Instant?
) {
    companion object {
        const val DEFAULT_IS_ACTIVE = true
    }

    val isDeleted: Boolean
        get() = deletedAt != null
}

