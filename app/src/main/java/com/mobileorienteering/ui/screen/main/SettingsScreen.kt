package com.mobileorienteering.ui.screen.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val darkMode by viewModel.darkMode.collectAsState()
    val volume by viewModel.volume.collectAsState()

    Column(Modifier.padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Dark Mode")
            Switch(checked = darkMode, onCheckedChange = { viewModel.toggleDarkMode(it) })
        }

        Spacer(Modifier.height(16.dp))

        Text("Volume: $volume")
        Slider(
            value = volume.toFloat(),
            onValueChange = { viewModel.updateVolume(it.toInt()) },
            valueRange = 0f..100f
        )
    }
}
