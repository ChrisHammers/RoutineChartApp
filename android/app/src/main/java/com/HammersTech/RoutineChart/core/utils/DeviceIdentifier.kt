package com.HammersTech.RoutineChart.core.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Device identifier generator and storage
 * Generates and persists a unique ID for this device
 */
class DeviceIdentifier(private val context: Context) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "device_prefs")

    companion object {
        private val DEVICE_ID_KEY = stringPreferencesKey("device_id")
    }

    /**
     * Get or create a device ID
     * @return Persistent device ID (UUID)
     */
    suspend fun getDeviceId(): String {
        val existingId =
            context.dataStore.data
                .map { preferences -> preferences[DEVICE_ID_KEY] }
                .first()

        if (existingId != null) {
            return existingId
        }

        // Generate new ID
        val newId = UUID.randomUUID().toString()
        context.dataStore.edit { preferences ->
            preferences[DEVICE_ID_KEY] = newId
        }
        AppLogger.Database.info("Generated new device ID: $newId")
        return newId
    }
}
