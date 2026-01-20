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

data class SettingsModel(
    val darkMode: Boolean = true,
    val contrastLevel: ContrastLevel = ContrastLevel.LOW,
    val controlPointSound: Boolean = true,
    val controlPointVibration: Boolean = true,
    val gpsAccuracy: Int = 15,
    val mapZoom: Int = 16,
    val showLocationDuringRun: Boolean = true
)
