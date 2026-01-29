package com.HammersTech.RoutineChart.core.domain.models

import java.time.Instant

/**
 * ChildProfile domain model
 * Represents a child's profile with preferences for routine presentation
 */
data class ChildProfile(
    val id: String,
    val familyId: String,
    val displayName: String,
    val avatarIcon: String?,
    val ageBand: AgeBand,
    val readingMode: ReadingMode,
    val audioEnabled: Boolean,
    val createdAt: Instant,
) {
    companion object {
        const val DEFAULT_AUDIO_ENABLED = true
    }
}
