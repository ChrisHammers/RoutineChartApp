package com.HammersTech.RoutineChart.core.domain.models

/**
 * User role in the family
 */
enum class Role {
    PARENT,
    CHILD,
    ;

    fun toRawValue(): String =
        when (this) {
            PARENT -> "parent"
            CHILD -> "child"
        }

    companion object {
        fun fromRawValue(value: String): Role =
            when (value) {
                "parent" -> PARENT
                "child" -> CHILD
                else -> throw IllegalArgumentException("Invalid Role: $value")
            }
    }
}

/**
 * Family plan tier
 */
enum class PlanTier {
    FREE,
    PAID,
    ;

    fun toRawValue(): String =
        when (this) {
            FREE -> "free"
            PAID -> "paid"
        }

    companion object {
        fun fromRawValue(value: String): PlanTier =
            when (value) {
                "free" -> FREE
                "paid" -> PAID
                else -> throw IllegalArgumentException("Invalid PlanTier: $value")
            }
    }
}

/**
 * Child age band for UI customization
 */
enum class AgeBand {
    AGE_2_4,
    AGE_5_7,
    AGE_8_10,
    AGE_11_PLUS,
    ;

    fun toRawValue(): String =
        when (this) {
            AGE_2_4 -> "2_4"
            AGE_5_7 -> "5_7"
            AGE_8_10 -> "8_10"
            AGE_11_PLUS -> "11_plus"
        }

    companion object {
        fun fromRawValue(value: String): AgeBand =
            when (value) {
                "2_4" -> AGE_2_4
                "5_7" -> AGE_5_7
                "8_10" -> AGE_8_10
                "11_plus" -> AGE_11_PLUS
                else -> throw IllegalArgumentException("Invalid AgeBand: $value")
            }
    }
}

/**
 * Reading mode for routine display
 */
enum class ReadingMode {
    VISUAL,
    LIGHT_TEXT,
    FULL_TEXT,
    ;

    fun toRawValue(): String =
        when (this) {
            VISUAL -> "visual"
            LIGHT_TEXT -> "light_text"
            FULL_TEXT -> "full_text"
        }

    companion object {
        fun fromRawValue(value: String): ReadingMode =
            when (value) {
                "visual" -> VISUAL
                "light_text" -> LIGHT_TEXT
                "full_text" -> FULL_TEXT
                else -> throw IllegalArgumentException("Invalid ReadingMode: $value")
            }
    }
}

/**
 * Routine completion rule
 */
enum class CompletionRule {
    ALL_STEPS_REQUIRED,
    ;

    fun toRawValue(): String =
        when (this) {
            ALL_STEPS_REQUIRED -> "all_steps_required"
        }

    companion object {
        fun fromRawValue(value: String): CompletionRule =
            when (value) {
                "all_steps_required" -> ALL_STEPS_REQUIRED
                else -> throw IllegalArgumentException("Invalid CompletionRule: $value")
            }
    }
}

/**
 * Completion event type
 */
enum class EventType {
    COMPLETE,
    UNDO,
    ;

    fun toRawValue(): String =
        when (this) {
            COMPLETE -> "complete"
            UNDO -> "undo"
        }

    companion object {
        fun fromRawValue(value: String): EventType =
            when (value) {
                "complete" -> COMPLETE
                "undo" -> UNDO
                else -> throw IllegalArgumentException("Invalid EventType: $value")
            }
    }
}
