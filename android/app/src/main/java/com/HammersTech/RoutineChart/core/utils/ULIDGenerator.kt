package com.HammersTech.RoutineChart.core.utils

import io.azam.ulidj.ULID

/**
 * ULID (Universally Unique Lexicographically Sortable Identifier) generator
 * Used for event IDs to ensure time-based ordering
 */
object ULIDGenerator {
    /**
     * Generate a new ULID string
     * @return ULID in Base32 format (e.g., "01ARZ3NDEKTSV4RRFFQ69G5FAV")
     */
    fun generate(): String {
        return ULID.random()
    }
}

