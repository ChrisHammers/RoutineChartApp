package com.HammersTech.RoutineChart.core.domain.models

import java.time.Instant

/**
 * Domain model for sync cursor
 * Tracks the last sync timestamp for a specific collection
 * Phase 3.1: Sync Infrastructure
 */
data class SyncCursor(
    val collection: String, // e.g., "routines", "events", "steps", "assignments", "children"
    val lastSyncedAt: Instant,
)
