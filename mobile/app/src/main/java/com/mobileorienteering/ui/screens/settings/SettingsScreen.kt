package com.mobileorienteering.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobileorienteering.BuildConfig
import com.mobileorienteering.R
import com.mobileorienteering.data.model.domain.AppLanguage
import com.mobileorienteering.data.model.domain.ContrastLevel
import com.mobileorienteering.ui.core.Strings
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
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    val privacyPolicyUrl = "https://mobileorienteering.com/#/privacy-policy"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(Strings.Nav.settings) }
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
                            text = Strings.Settings.guestModeTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = Strings.Settings.guestModeMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Button(
                            onClick = { 
                                authViewModel.logout()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(Strings.Auth.signIn)
                        }
                    }
                }
            }

            if (authModel?.isGuestMode != true) {
                SettingsSection(title = Strings.Settings.account) {
                    SettingsNavigationItem(
                        icon = R.drawable.ic_person_filled,
                        title = Strings.Settings.editProfile,
                        onClick = onNavigateToEditProfile
                    )

                    if (authModel?.isGoogleLogin != true) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        SettingsNavigationItem(
                            icon = R.drawable.ic_lock_filled,
                            title = Strings.Auth.changePassword,
                            onClick = onNavigateToEditPassword
                        )
                    }
                }
            }

            SettingsSection(title = Strings.Settings.appearance) {
                SettingsSwitchItem(
                    icon = R.drawable.ic_dark_mode,
                    title = Strings.Settings.darkMode,
                    checked = settings.darkMode,
                    onCheckedChange = { viewModel.toggleDarkMode(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsClickableItem(
                    icon = R.drawable.ic_contrast,
                    title = Strings.Settings.contrastLevel,
                    subtitle = settings.contrastLevel.getLabel(),
                    showRightArrow = true,
                    onClick = { showContrastDialog = true }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsClickableItem(
                    icon = R.drawable.ic_language,
                    title = Strings.Settings.language,
                    subtitle = settings.language.getDisplayName(),
                    showRightArrow = true,
                    onClick = { showLanguageDialog = true }
                )
            }

            SettingsSection(title = Strings.Settings.controlPoints) {
                SettingsSwitchItem(
                    icon = R.drawable.ic_volume,
                    title = Strings.Settings.sound,
                    checked = settings.controlPointSound,
                    onCheckedChange = { viewModel.updateControlPointSound(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsSwitchItem(
                    icon = R.drawable.ic_vibration,
                    title = Strings.Settings.vibration,
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
                                Strings.Settings.detectionRadius,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = Strings.Plurals.GpsAccuracyValue(
                                    settings.gpsAccuracy,
                                    settings.gpsAccuracy
                                ),
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

            SettingsSection(title = Strings.Settings.gps) {
                SettingsSwitchItem(
                    icon = R.drawable.ic_visibility_filled,
                    title = Strings.Settings.locationDuringRun,
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
                                Strings.Settings.mapZoom,
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

            SettingsSection(title = Strings.Settings.advanced) {
                if (authModel?.isGuestMode != true) {
                    SettingsClickableItem(
                        icon = R.drawable.ic_sync,
                        title = Strings.Settings.syncData,
                        onClick = { showSyncDialog = true }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }

                SettingsClickableItem(
                    icon = R.drawable.ic_info_outlined,
                    title = Strings.Settings.aboutApp,
                    onClick = { showAboutDialog = true }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsClickableItem(
                    icon = R.drawable.ic_logout,
                    title = if (authModel?.isGuestMode == true) Strings.Auth.signIn else Strings.Settings.logout,
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
            title = { Text(Strings.Settings.contrastLevel) },
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
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.contrastLevel == level,
                                onClick = {
                                    viewModel.updateContrastLevel(level)
                                    showContrastDialog = false
                                }
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(level.getLabel())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContrastDialog = false }) {
                    Text(Strings.Action.close)
                }
            }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(Strings.Settings.language) },
            text = {
                Column {
                    Text(
                        text = Strings.Settings.translationsDisclaimer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    AppLanguage.entries.forEach { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateLanguage(language)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.language == language,
                                onClick = {
                                    viewModel.updateLanguage(language)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(language.getDisplayName())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(Strings.Action.close)
                }
            }
        )
    }

    if (showSyncDialog) {
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            title = { Text(Strings.Settings.syncDialogTitle) },
            text = { 
                Column {
                    Text(Strings.Settings.syncDialogMessage1)
                    Text(Strings.Settings.syncDialogMessage2)
                    Text(Strings.Settings.syncDialogMessage3)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        syncViewModel.syncAllData()
                        showSyncDialog = false
                    }
                ) {
                    Text(Strings.Settings.syncAction, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSyncDialog = false }) {
                    Text(Strings.Action.cancel)
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(Strings.Settings.logoutConfirmTitle) },
            text = { Text(Strings.Settings.logoutConfirmMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        authViewModel.logout()
                        showLogoutDialog = false
                    }
                ) {
                    Text(Strings.Settings.logout, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(Strings.Action.cancel)
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(Strings.Settings.aboutApp) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = Strings.App.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    HorizontalDivider()
                    
                    Text(
                        text = Strings.Settings.license,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = Strings.Settings.licenseName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    HorizontalDivider()
                    
                    Text(
                        text = Strings.Settings.authors,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = Strings.Settings.authorsNames,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    HorizontalDivider()

                    TextButton(
                        onClick = {
                            uriHandler.openUri(privacyPolicyUrl)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = Strings.Settings.privacyPolicy,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(Strings.Action.close)
                }
            }
        )
    }
}
