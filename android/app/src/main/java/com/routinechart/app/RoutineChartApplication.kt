package com.routinechart.app

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application class for Routine Chart App
 * Initializes Hilt, Firebase, and other app-wide dependencies
 */
@HiltAndroidApp
class RoutineChartApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Setup Timber logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("RoutineChartApplication initialized")
    }
}

