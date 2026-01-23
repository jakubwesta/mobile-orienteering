package com.mobileorienteering.ui.core.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun LocationPermissionRationaleDialog(
    onDismiss: () -> Unit,
    onGrantPermission: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
        title = { Text("Location Permission Required") },
        text = {
            Text("Precise location access is essential for tracking your position during runs and detecting when you reach checkpoints. Without it, the app cannot function.")
        },
        confirmButton = {
            TextButton(onClick = onGrantPermission) {
                Text("Grant Permission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LocationPermissionSettingsDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
        title = { Text("Location Permission Required") },
        text = {
            Text("Location permission is required for this app to work. Please enable it in Settings under Permissions > Location.")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            ) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun GpsSettingsDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Location Services Disabled") },
        text = { Text("Location services are turned off. Would you like to enable them in settings?") },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    context.startActivity(intent)
                }
            ) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
