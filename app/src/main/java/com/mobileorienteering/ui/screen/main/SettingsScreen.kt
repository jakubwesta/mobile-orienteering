package com.mobileorienteering.ui.screen.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsState()

    Column {
        Text("Dark Mode")
        Switch(
            checked = settings.darkMode,
            onCheckedChange = { viewModel.toggleDarkMode(it) }
        )

        Spacer(Modifier.height(16.dp))

        Text("Volume: ${settings.volume}")
        Slider(
            value = settings.volume.toFloat(),
            onValueChange = { viewModel.updateVolume(it.toInt()) },
            valueRange = 0f..100f
        )

        Spacer(Modifier.height(16.dp))

        Row {
            Text("Vibration")
            Switch(
                checked = settings.vibration,
                onCheckedChange = { viewModel.updateVibration(it) }
            )
        }

        Spacer(Modifier.height(16.dp))

        Text("GPS Accuracy: ${settings.gpsAccuracy}")
        Slider(
            value = settings.gpsAccuracy.toFloat(),
            onValueChange = { viewModel.updateGpsAccuracy(it.toInt()) },
            valueRange = 1f..50f
        )
    }
}
