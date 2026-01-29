package com.HammersTech.RoutineChart.core.data.local.room

import androidx.room.TypeConverter
import com.HammersTech.RoutineChart.core.domain.models.AgeBand
import com.HammersTech.RoutineChart.core.domain.models.CompletionRule
import com.HammersTech.RoutineChart.core.domain.models.EventType
import com.HammersTech.RoutineChart.core.domain.models.PlanTier
import com.HammersTech.RoutineChart.core.domain.models.ReadingMode
import com.HammersTech.RoutineChart.core.domain.models.Role
import java.time.Instant

/**
 * Room type converters for custom types
 */
class Converters {
    // Instant converters
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun instantToTimestamp(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }

    // Enum converters
    @TypeConverter
    fun fromRole(value: Role): String = value.toRawValue()

    @TypeConverter
    fun toRole(value: String): Role = Role.fromRawValue(value)

    @TypeConverter
    fun fromPlanTier(value: PlanTier): String = value.toRawValue()

    @TypeConverter
    fun toPlanTier(value: String): PlanTier = PlanTier.fromRawValue(value)

    @TypeConverter
    fun fromAgeBand(value: AgeBand): String = value.toRawValue()

    @TypeConverter
    fun toAgeBand(value: String): AgeBand = AgeBand.fromRawValue(value)

    @TypeConverter
    fun fromReadingMode(value: ReadingMode): String = value.toRawValue()

    @TypeConverter
    fun toReadingMode(value: String): ReadingMode = ReadingMode.fromRawValue(value)

    @TypeConverter
    fun fromCompletionRule(value: CompletionRule): String = value.toRawValue()

    @TypeConverter
    fun toCompletionRule(value: String): CompletionRule = CompletionRule.fromRawValue(value)

    @TypeConverter
    fun fromEventType(value: EventType): String = value.toRawValue()

    @TypeConverter
    fun toEventType(value: String): EventType = EventType.fromRawValue(value)
}
