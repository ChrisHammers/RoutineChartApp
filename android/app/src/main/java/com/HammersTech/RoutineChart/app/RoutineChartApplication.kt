package com.HammersTech.RoutineChart.app

import android.app.Application
import com.HammersTech.RoutineChart.BuildConfig
import com.HammersTech.RoutineChart.core.utils.AppLogger
import com.google.firebase.FirebaseApp // Phase 2.1: Enabled for Firebase Auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
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

            // Phase 2.1: Firebase initialization enabled
            try {
                // Check if Firebase is already initialized (prevents duplicate initialization)
                if (FirebaseApp.getApps(this).isEmpty()) {
                    FirebaseApp.initializeApp(this)
                    AppLogger.UI.info("Firebase initialized")
                } else {
                    AppLogger.UI.info("Firebase already initialized")
                }
            } catch (e: SecurityException) {
                // Google Play Services security exception - usually non-fatal
                // This happens when SHA-1 fingerprint isn't registered in Firebase Console
                AppLogger.UI.error("Firebase initialization warning (non-fatal): ${e.message}", e)
                AppLogger.UI.info("Note: If Firebase features don't work, register SHA-1 fingerprint in Firebase Console")
                // Continue anyway - Firebase might still work
            } catch (e: Exception) {
                AppLogger.UI.error("Firebase initialization failed: ${e.message}", e)
                // Continue anyway - app can still work without Firebase in some cases
            }

            // Configure Firestore settings for better network connectivity
            try {
                val firestore = FirebaseFirestore.getInstance()
                val settings =
                    FirebaseFirestoreSettings.Builder()
                        .setPersistenceEnabled(true) // Enable offline persistence
                        .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                        .build()
                firestore.firestoreSettings = settings
                AppLogger.UI.info("Firestore configured with persistence enabled")
            } catch (e: Exception) {
                AppLogger.UI.error("Firestore configuration warning: ${e.message}", e)
                // Continue anyway
            }

            AppLogger.UI.info("RoutineChartApplication initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("RoutineChartApp", "Failed to initialize application", e)
            // Don't throw - allow app to continue even if initialization partially fails
            AppLogger.UI.error("Application initialization had errors, but continuing anyway", e)
        }
    }
}
