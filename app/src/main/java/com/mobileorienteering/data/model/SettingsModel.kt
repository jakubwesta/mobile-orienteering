package com.mobileorienteering.data.model

data class SettingsModel(
    val darkMode: Boolean = false,
    val volume: Int = 50,
    val vibration: Boolean = true,
    val gpsAccuracy: Int = 10
)
