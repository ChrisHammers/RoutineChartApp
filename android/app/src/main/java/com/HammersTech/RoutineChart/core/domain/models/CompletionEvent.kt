package com.HammersTech.RoutineChart.core.domain.models

import java.time.Instant

/**
 * CompletionEvent domain model
 * Append-only event log representing step completions and undos
 */
data class CompletionEvent(
    val id: String, // ULID format
    val familyId: String,
    val childId: String,
    val routineId: String,
    val stepId: String,
    val eventType: EventType,
    val eventAt: Instant,
    val localDayKey: String, // YYYY-MM-DD in family timezone
    val deviceId: String,
    val synced: Boolean = false
) {
    companion object {
        /**
         * Events are ordered by eventAt (primary) and eventId (tiebreaker)
         */
        val COMPARATOR: Comparator<CompletionEvent> = compareBy(
            { it.eventAt },
            { it.id }
        )
    }
}

