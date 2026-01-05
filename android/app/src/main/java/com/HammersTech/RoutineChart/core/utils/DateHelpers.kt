package com.HammersTech.RoutineChart.core.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Date helper utilities for converting timestamps to local day keys
 */
object DateHelpers {
    private val DAY_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Convert an Instant to a local day key (YYYY-MM-DD) in the specified timezone
     * @param instant The instant to convert
     * @param timeZone IANA timezone string (e.g., "America/Los_Angeles")
     * @return Local day key string (e.g., "2026-01-05")
     */
    fun localDayKey(instant: Instant, timeZone: String): String {
        val zoneId = ZoneId.of(timeZone)
        val localDate = instant.atZone(zoneId).toLocalDate()
        return localDate.format(DAY_KEY_FORMATTER)
    }

    /**
     * Get today's day key in the specified timezone
     * @param timeZone IANA timezone string
     * @return Today's local day key
     */
    fun todayDayKey(timeZone: String): String {
        return localDayKey(Instant.now(), timeZone)
    }

    /**
     * Parse a local day key string back to a LocalDate
     * @param dayKey Day key string (YYYY-MM-DD)
     * @return LocalDate object
     */
    fun parseDayKey(dayKey: String): LocalDate {
        return LocalDate.parse(dayKey, DAY_KEY_FORMATTER)
    }
}

