package com.mobileorienteering.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobileorienteering.BuildConfig
import com.mobileorienteering.R
import com.mobileorienteering.data.model.domain.ContrastLevel
import com.mobileorienteering.ui.screens.auth.AuthViewModel
import com.mobileorienteering.ui.screens.settings.components.SettingsClickableItem
import com.mobileorienteering.ui.screens.settings.components.SettingsNavigationItem
import com.mobileorienteering.ui.screens.settings.components.SettingsSection
import com.mobileorienteering.ui.screens.settings.components.SettingsSwitchItem

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
    var showAboutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

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

            if (authModel?.isGuestMode == true) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Guest Mode",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "You're using the app as a guest. Sign in to sync your data across devices and access cloud features.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Button(
                            onClick = { 
                                authViewModel.logout()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sign In")
                        }
                    }
                }
            }

            if (authModel?.isGuestMode != true) {
                SettingsSection(title = "Account") {
                    SettingsNavigationItem(
                        icon = R.drawable.ic_person_filled,
                        title = "Edit Profile",
                        onClick = onNavigateToEditProfile
                    )

                    if (authModel?.isGoogleLogin != true) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        SettingsNavigationItem(
                            icon = R.drawable.ic_lock_filled,
                            title = "Change Password",
                            onClick = onNavigateToEditPassword
                        )
                    }
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

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

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
                                "Detection Radius",
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
                        valueRange = 5f..50f,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            SettingsSection(title = "GPS") {
                SettingsSwitchItem(
                    icon = R.drawable.ic_visibility_filled,
                    title = "Show location during run",
                    checked = settings.showLocationDuringRun,
                    onCheckedChange = { viewModel.updateShowLocationDuringRun(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

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
                            painter = painterResource(id = R.drawable.ic_search),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Map Zoom",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "${settings.mapZoom}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Slider(
                        value = settings.mapZoom.toFloat(),
                        onValueChange = { viewModel.updateMapZoom(it.toInt()) },
                        valueRange = 12f..20f,
                        steps = 7,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            SettingsSection(title = "Advanced") {
                if (authModel?.isGuestMode != true) {
                    SettingsClickableItem(
                        icon = R.drawable.ic_sync,
                        title = "Sync data with server",
                        onClick = { showSyncDialog = true }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }

                SettingsClickableItem(
                    icon = R.drawable.ic_info_outlined,
                    title = "About app",
                    onClick = { showAboutDialog = true }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsClickableItem(
                    icon = R.drawable.ic_logout,
                    title = if (authModel?.isGuestMode == true) "Sign In" else "Logout",
                    onClick = { 
                        if (authModel?.isGuestMode == true) {
                            authViewModel.logout()
                        } else {
                            showLogoutDialog = true
                        }
                    }
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

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About app") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    HorizontalDivider()
                    
                    Text(
                        text = "License",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Elastic License 2.0",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    HorizontalDivider()
                    
                    Text(
                        text = "Authors",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Jakub Westa & Dawid Pilarski",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    HorizontalDivider()

                    TextButton(
                        onClick = {
                            uriHandler.openUri("https://mobileorienteering.com/#/privacy-policy")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Privacy Policy",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}