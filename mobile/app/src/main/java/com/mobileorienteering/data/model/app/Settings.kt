package com.mobileorienteering.data.model.app

import com.mobileorienteering.BuildConfig
import com.mobileorienteering.ui.theme.ContrastLevel as ThemeLevel

enum class MapStyle {
    CLASSIC, SATELLITE, OUTDOOR, ORIENTEERING;

    fun getUrl(): String = when (this) {
        CLASSIC -> "https://tiles.openfreemap.org/styles/liberty"
        SATELLITE -> "https://api.maptiler.com/maps/satellite/style.json?key=${BuildConfig.MAPTILER_API_KEY}"
        OUTDOOR -> "https://api.maptiler.com/maps/outdoor/style.json?key=${BuildConfig.MAPTILER_API_KEY}"
        ORIENTEERING -> "https://api.maptiler.com/maps/019e0f7a-7030-7e56-9247-773f3d49ae59/style.json?key=${BuildConfig.MAPTILER_API_KEY}"
    }

    fun getLabel(): String = when (this) {
        CLASSIC -> "Classic"
        SATELLITE -> "Satellite"
        OUTDOOR -> "Outdoor"
        ORIENTEERING -> "Orienteering"
    }
}

enum class MapIconStyle {
    MODERN, ORIENTEERING;

    fun getLabel(): String = when (this) {
        MODERN -> "Modern"
        ORIENTEERING -> "Orienteering"
    }
}

enum class MapQuality(
    val width: Int,
    val height: Int
) {
    QUALITY_2K(2048, 1152),
    QUALITY_4K(4096, 2304),
    QUALITY_8K(7680, 4320);

    fun getLabel(): String = when (this) {
        QUALITY_2K -> "2K"
        QUALITY_4K -> "4K"
        QUALITY_8K -> "8K"
    }
}

enum class ContrastLevel {
    LOW, MEDIUM, HIGH;

    fun getLabel(): String = when (this) {
        LOW -> "Low"
        MEDIUM -> "Medium"
        HIGH -> "High"
    }

    fun toTheme(): ThemeLevel = when (this) {
        LOW -> ThemeLevel.DEFAULT
        MEDIUM -> ThemeLevel.MEDIUM
        HIGH -> ThemeLevel.HIGH
    }
}

enum class AppLanguage(val localeCode: String) {
    ENGLISH("en"),
    POLISH("pl"),
    SWEDISH("sv"),
    GERMAN("de"),
    FINNISH("fi"),
    SPANISH("es"),
    FRENCH("fr");

    fun getDisplayName(): String = when (this) {
        ENGLISH -> "English"
        POLISH -> "Polski"
        SWEDISH -> "Svenska"
        GERMAN -> "Deutsch"
        FINNISH -> "Suomi"
        SPANISH -> "Español"
        FRENCH -> "Français"
    }
}

data class SettingsModel(
    val darkMode: Boolean = true,
    val contrastLevel: ContrastLevel = ContrastLevel.LOW,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val controlPointSound: Boolean = true,
    val controlPointVibration: Boolean = true,
    val showLocationDuringRun: Boolean = true,
    val mapStyle: MapStyle = MapStyle.CLASSIC,
    val mapIconStyle: MapIconStyle = MapIconStyle.MODERN,
    val mapQuality: MapQuality = MapQuality.QUALITY_2K
)
