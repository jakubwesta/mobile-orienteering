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
import com.mobileorienteering.ui.core.Strings

@Composable
fun LocationPermissionRationaleDialog(
    onDismiss: () -> Unit,
    onGrantPermission: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
        title = { Text(Strings.Permission.locationRequiredTitle) },
        text = {
            Text(Strings.Permission.locationRequiredMessage)
        },
        confirmButton = {
            TextButton(onClick = onGrantPermission) {
                Text(Strings.Permission.grant)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.Action.cancel)
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
        title = { Text(Strings.Permission.locationRequiredTitle) },
        text = {
            Text(Strings.Permission.locationRequiredSettings)
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
                Text(Strings.Permission.openSettings)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.Action.cancel)
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
        title = { Text(Strings.Permission.locationDisabledTitle) },
        text = { Text(Strings.Permission.locationDisabledMessage) },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    context.startActivity(intent)
                }
            ) {
                Text(Strings.Permission.openSettings)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.Action.cancel)
            }
        }
    )
}
