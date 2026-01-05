package com.HammersTech.RoutineChart.core.domain.models

import java.time.Instant

/**
 * Family domain model
 * Represents a household unit with shared routines
 */
data class Family(
    val id: String,
    val name: String?,
    val timeZone: String,
    val weekStartsOn: Int,
    val planTier: PlanTier,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        const val DEFAULT_TIMEZONE = "America/Los_Angeles"
        const val DEFAULT_WEEK_STARTS_ON = 0 // Sunday
    }
}

