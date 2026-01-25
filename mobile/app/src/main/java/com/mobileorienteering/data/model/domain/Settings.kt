package com.mobileorienteering.data.model.domain

import com.mobileorienteering.ui.theme.ContrastLevel as ThemeLevel

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
    val gpsAccuracy: Int = 15,
    val mapZoom: Int = 16,
    val showLocationDuringRun: Boolean = true
)
