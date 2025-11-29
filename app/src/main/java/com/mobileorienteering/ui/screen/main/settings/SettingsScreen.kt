package com.mobileorienteering.ui.screen.main.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobileorienteering.R
import com.mobileorienteering.data.model.ContrastLevel
import com.mobileorienteering.ui.screen.auth.AuthViewModel
import com.mobileorienteering.ui.screen.main.runs.ActivityViewModel
import com.mobileorienteering.ui.screen.main.settings.components.SettingsClickableItem
import com.mobileorienteering.ui.screen.main.settings.components.SettingsNavigationItem
import com.mobileorienteering.ui.screen.main.settings.components.SettingsSection
import com.mobileorienteering.ui.screen.main.settings.components.SettingsSwitchItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    syncViewModel: SyncViewModel = hiltViewModel(),
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToEditPassword: () -> Unit = {}
) {
    val settings by viewModel.settings.collectAsState()
    val authModel by authViewModel.authModel.collectAsState()
    var showContrastDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            SettingsSection(title = "Account") {
                SettingsNavigationItem(
                    icon = R.drawable.ic_person_filled,
                    title = "Edit Profile",
                    onClick = onNavigateToEditProfile
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                if (authModel?.isGoogleLogin != true) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsNavigationItem(
                        icon = R.drawable.ic_lock_filled,
                        title = "Change Password",
                        onClick = onNavigateToEditPassword
                    )
                }
            }

            SettingsSection(title = "Appearance") {
                SettingsSwitchItem(
                    icon = R.drawable.ic_dark_mode,
                    title = "Dark Mode",
                    checked = settings.darkMode,
                    onCheckedChange = { viewModel.toggleDarkMode(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsClickableItem(
                    icon = R.drawable.ic_contrast,
                    title = "Contrast Level",
                    subtitle = settings.contrastLevel.getLabel(),
                    showRightArrow = true,
                    onClick = { showContrastDialog = true }
                )
            }

            SettingsSection(title = "Control Points") {
                SettingsSwitchItem(
                    icon = R.drawable.ic_volume,
                    title = "Sound",
                    checked = settings.controlPointSound,
                    onCheckedChange = { viewModel.updateControlPointSound(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsSwitchItem(
                    icon = R.drawable.ic_vibration,
                    title = "Vibration",
                    checked = settings.controlPointVibration,
                    onCheckedChange = { viewModel.updateControlPointVibration(it) }
                )
            }

            SettingsSection(title = "GPS") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_target),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "GPS Accuracy",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "${settings.gpsAccuracy} meters",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Slider(
                        value = settings.gpsAccuracy.toFloat(),
                        onValueChange = { viewModel.updateGpsAccuracy(it.toInt()) },
                        valueRange = 1f..50f,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            SettingsSection(title = "Advanced") {
                SettingsClickableItem(
                    icon = R.drawable.ic_sync,
                    title = "Sync data with server",
                    onClick = { showSyncDialog = true }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsClickableItem(
                    icon = R.drawable.ic_logout,
                    title = "Logout",
                    onClick = { showLogoutDialog = true }
                )
            }
        }
    }

    if (showContrastDialog) {
        AlertDialog(
            onDismissRequest = { showContrastDialog = false },
            title = { Text("Contrast Level") },
            text = {
                Column {
                    ContrastLevel.entries.forEach { level ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateContrastLevel(level)
                                    showContrastDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.contrastLevel == level,
                                onClick = {
                                    viewModel.updateContrastLevel(level)
                                    showContrastDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(level.getLabel())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContrastDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showSyncDialog) {
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            title = { Text("Sync data") },
            text = { Text("Are you sure you want to sync data with the server? \n" +
                    "This may remove your local, unsynced data. \n" +
                    "Only do this if you logged in on new device!") },
            confirmButton = {
                TextButton(
                    onClick = {
                        syncViewModel.syncAllData()
                        showSyncDialog = false
                    }
                ) {
                    Text("Sync", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSyncDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        authViewModel.logout()
                        showLogoutDialog = false
                    }
                ) {
                    Text("Logout", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
