package com.HammersTech.RoutineChart.app

import android.app.Application
// import com.google.firebase.FirebaseApp  // Phase 2: Re-enable for Firebase
import com.HammersTech.RoutineChart.BuildConfig
import com.HammersTech.RoutineChart.core.utils.AppLogger
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Routine Chart App
 * Initializes Hilt and other app-wide dependencies
 */
@HiltAndroidApp
class RoutineChartApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        try {
            // Setup logging first
            AppLogger.init(BuildConfig.DEBUG)
            AppLogger.UI.info("RoutineChartApplication onCreate started")
            
            // Phase 2: Re-enable Firebase initialization
            // FirebaseApp.initializeApp(this)
            
            AppLogger.UI.info("RoutineChartApplication initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("RoutineChartApp", "Failed to initialize application", e)
            throw e
        }
    }
}

