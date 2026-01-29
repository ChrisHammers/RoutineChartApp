package com.HammersTech.RoutineChart.core.utils

import timber.log.Timber

/**
 * Centralized logging utility using Timber
 * Provides structured logging for different app subsystems
 */
object AppLogger {
    /**
     * Initialize Timber logging
     * Should be called once in Application.onCreate()
     */
    fun init(isDebug: Boolean) {
        if (isDebug) {
            Timber.plant(Timber.DebugTree())
        }
    }

    // Subsystem loggers
    object Database {
        fun info(message: String) = Timber.tag("DB").i(message)

        fun error(
            message: String,
            error: Throwable? = null,
        ) = Timber.tag("DB").e(error, message)

        fun debug(message: String) = Timber.tag("DB").d(message)
    }

    object Sync {
        fun info(message: String) = Timber.tag("SYNC").i(message)

        fun error(
            message: String,
            error: Throwable? = null,
        ) = Timber.tag("SYNC").e(error, message)

        fun debug(message: String) = Timber.tag("SYNC").d(message)
    }

    object Auth {
        fun info(message: String) = Timber.tag("AUTH").i(message)

        fun error(
            message: String,
            error: Throwable? = null,
        ) = Timber.tag("AUTH").e(error, message)

        fun debug(message: String) = Timber.tag("AUTH").d(message)
    }

    object UseCase {
        fun info(message: String) = Timber.tag("USECASE").i(message)

        fun error(
            message: String,
            error: Throwable? = null,
        ) = Timber.tag("USECASE").e(error, message)

        fun debug(message: String) = Timber.tag("USECASE").d(message)
    }

    object UI {
        fun info(message: String) = Timber.tag("UI").i(message)

        fun error(
            message: String,
            error: Throwable? = null,
        ) = Timber.tag("UI").e(error, message)

        fun debug(message: String) = Timber.tag("UI").d(message)
    }
}
