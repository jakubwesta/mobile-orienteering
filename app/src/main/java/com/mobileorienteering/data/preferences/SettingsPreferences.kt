package com.mobileorienteering.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mobileorienteering.data.model.ContrastLevel
import com.mobileorienteering.data.model.SettingsModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore("settings")

@Singleton
class SettingsPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    companion object {
        private val DARK_MODE = booleanPreferencesKey("dark_mode")
        private val CONTRAST_LEVEL = stringPreferencesKey("contrast_level")
        private val CONTROL_POINT_SOUND = booleanPreferencesKey("control_point_sound")
        private val CONTROL_POINT_VIBRATION = booleanPreferencesKey("control_point_vibration")
        private val GPS_ACCURACY = intPreferencesKey("gps_accuracy")
    }

    val settingsFlow: Flow<SettingsModel> = context.settingsDataStore.data.map { prefs ->
        SettingsModel(
            darkMode = prefs[DARK_MODE] ?: SettingsModel().darkMode,
            contrastLevel = prefs[CONTRAST_LEVEL]?.let {
                ContrastLevel.valueOf(it)
            } ?: SettingsModel().contrastLevel,
            controlPointSound = prefs[CONTROL_POINT_SOUND] ?: SettingsModel().controlPointSound,
            controlPointVibration = prefs[CONTROL_POINT_VIBRATION] ?: SettingsModel().controlPointVibration,
            gpsAccuracy = prefs[GPS_ACCURACY] ?: SettingsModel().gpsAccuracy
        )
    }

    suspend fun updateSettings(settings: SettingsModel) {
        context.settingsDataStore.edit { prefs ->
            prefs[DARK_MODE] = settings.darkMode
            prefs[CONTRAST_LEVEL] = settings.contrastLevel.name
            prefs[CONTROL_POINT_SOUND] = settings.controlPointSound
            prefs[CONTROL_POINT_VIBRATION] = settings.controlPointVibration
            prefs[GPS_ACCURACY] = settings.gpsAccuracy
        }
    }

    suspend fun updateDarkMode(enabled: Boolean) =
        context.settingsDataStore.edit { it[DARK_MODE] = enabled }

    suspend fun updateContrastLevel(level: ContrastLevel) =
        context.settingsDataStore.edit { it[CONTRAST_LEVEL] = level.name }

    suspend fun updateControlPointSound(enabled: Boolean) =
        context.settingsDataStore.edit { it[CONTROL_POINT_SOUND] = enabled }

    suspend fun updateControlPointVibration(enabled: Boolean) =
        context.settingsDataStore.edit { it[CONTROL_POINT_VIBRATION] = enabled }

    suspend fun updateGpsAccuracy(value: Int) =
        context.settingsDataStore.edit { it[GPS_ACCURACY] = value }
}
