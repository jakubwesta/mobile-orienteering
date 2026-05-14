package com.mobileorienteering.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mobileorienteering.data.model.app.AppLanguage
import com.mobileorienteering.data.model.app.ContrastLevel
import com.mobileorienteering.data.model.app.MapIconStyle
import com.mobileorienteering.data.model.app.MapQuality
import com.mobileorienteering.data.model.app.MapStyle
import com.mobileorienteering.data.model.app.SettingsModel
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
        private val LANGUAGE = stringPreferencesKey("language")
        private val CONTROL_POINT_SOUND = booleanPreferencesKey("control_point_sound")
        private val CONTROL_POINT_VIBRATION = booleanPreferencesKey("control_point_vibration")
        private val SHOW_LOCATION_DURING_RUN = booleanPreferencesKey("show_location_during_run")
        private val MAP_STYLE = stringPreferencesKey("map_style")
        private val MAP_ICON_STYLE = stringPreferencesKey("map_icon_style")
        private val MAP_QUALITY = stringPreferencesKey("map_quality")
    }

    val settingsFlow: Flow<SettingsModel> = context.settingsDataStore.data.map { prefs ->
        SettingsModel(
            darkMode = prefs[DARK_MODE]
                ?: SettingsModel().darkMode,

            contrastLevel = prefs[CONTRAST_LEVEL]?.let {
                ContrastLevel.valueOf(it)
            } ?: SettingsModel().contrastLevel,

            language = prefs[LANGUAGE]?.let {
                AppLanguage.valueOf(it)
            } ?: SettingsModel().language,

            controlPointSound = prefs[CONTROL_POINT_SOUND]
                ?: SettingsModel().controlPointSound,

            controlPointVibration = prefs[CONTROL_POINT_VIBRATION]
                ?: SettingsModel().controlPointVibration,

            showLocationDuringRun = prefs[SHOW_LOCATION_DURING_RUN]
                ?: SettingsModel().showLocationDuringRun,

            mapStyle = prefs[MAP_STYLE]?.let {
                runCatching { MapStyle.valueOf(it) }.getOrNull()
            } ?: SettingsModel().mapStyle,

            mapIconStyle = prefs[MAP_ICON_STYLE]?.let {
                runCatching { MapIconStyle.valueOf(it) }.getOrNull()
            } ?: SettingsModel().mapIconStyle,

            mapQuality = prefs[MAP_QUALITY]?.let {
                runCatching { MapQuality.valueOf(it) }.getOrNull()
            } ?: SettingsModel().mapQuality
        )
    }

    suspend fun updateDarkMode(
        enabled: Boolean
    ) = context.settingsDataStore.edit { it[DARK_MODE] = enabled }

    suspend fun updateContrastLevel(
        level: ContrastLevel
    ) = context.settingsDataStore.edit { it[CONTRAST_LEVEL] = level.name }

    suspend fun updateLanguage(
        language: AppLanguage
    ) = context.settingsDataStore.edit { it[LANGUAGE] = language.name }

    suspend fun updateControlPointSound(
        enabled: Boolean
    ) = context.settingsDataStore.edit { it[CONTROL_POINT_SOUND] = enabled }

    suspend fun updateControlPointVibration(
        enabled: Boolean
    ) = context.settingsDataStore.edit { it[CONTROL_POINT_VIBRATION] = enabled }

    suspend fun updateShowLocationDuringRun(
        enabled: Boolean
    ) = context.settingsDataStore.edit { it[SHOW_LOCATION_DURING_RUN] = enabled }

    suspend fun updateMapStyle(
        style: MapStyle
    ) = context.settingsDataStore.edit { it[MAP_STYLE] = style.name }

    suspend fun updateMapIconStyle(
        style: MapIconStyle
    ) = context.settingsDataStore.edit { it[MAP_ICON_STYLE] = style.name }

    suspend fun updateMapQuality(
        quality: MapQuality
    ) = context.settingsDataStore.edit { it[MAP_QUALITY] = quality.name }
}
