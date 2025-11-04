package com.mobileorienteering.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mobileorienteering.data.model.SettingsModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val DARK_MODE = booleanPreferencesKey("dark_mode")
        private val VOLUME = intPreferencesKey("volume")
        private val VIBRATION = booleanPreferencesKey("vibration")
        private val GPS_ACCURACY = intPreferencesKey("gps_accuracy")
    }

    val settingsFlow: Flow<SettingsModel> = context.dataStore.data.map { prefs ->
        SettingsModel(
            darkMode = prefs[DARK_MODE] ?: SettingsModel().darkMode,
            volume = prefs[VOLUME] ?: SettingsModel().volume,
            vibration = prefs[VIBRATION] ?: SettingsModel().vibration,
            gpsAccuracy = prefs[GPS_ACCURACY] ?: SettingsModel().gpsAccuracy
        )
    }

    suspend fun updateSettings(settings: SettingsModel) {
        context.dataStore.edit { prefs ->
            prefs[DARK_MODE] = settings.darkMode
            prefs[VOLUME] = settings.volume
            prefs[VIBRATION] = settings.vibration
            prefs[GPS_ACCURACY] = settings.gpsAccuracy
        }
    }

    suspend fun updateDarkMode(enabled: Boolean) =
        context.dataStore.edit { it[DARK_MODE] = enabled }

    suspend fun updateVolume(value: Int) =
        updateSettings(getCurrentSettings().copy(volume = value))

    suspend fun updateVibration(enabled: Boolean) =
        updateSettings(getCurrentSettings().copy(vibration = enabled))

    suspend fun updateGpsAccuracy(value: Int) =
        updateSettings(getCurrentSettings().copy(gpsAccuracy = value))

    private suspend fun getCurrentSettings(): SettingsModel {
        val prefs = context.dataStore.data.first()
        return SettingsModel(
            darkMode = prefs[DARK_MODE] ?: SettingsModel().darkMode,
            volume = prefs[VOLUME] ?: SettingsModel().volume,
            vibration = prefs[VIBRATION] ?: SettingsModel().vibration,
            gpsAccuracy = prefs[GPS_ACCURACY] ?: SettingsModel().gpsAccuracy
        )
    }
}
