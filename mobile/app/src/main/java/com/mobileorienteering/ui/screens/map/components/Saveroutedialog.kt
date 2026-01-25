package com.mobileorienteering.ui.screens.map.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobileorienteering.ui.core.Strings

@Composable
fun SaveRouteDialog(
    onDismiss: () -> Unit,
    onSave: (name: String) -> Unit,
    existingMapName: String? = null,
    onUpdate: ((name: String) -> Unit)? = null
) {
    val isEditingExistingMap = existingMapName != null && onUpdate != null
    var name by remember { mutableStateOf(existingMapName ?: "") }
    var showOptions by remember { mutableStateOf(isEditingExistingMap) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (showOptions) Strings.Map.saveMap
                else if (isEditingExistingMap) Strings.Map.saveAsNew
                else Strings.Map.saveRoute
            )
        },
        text = {
            Column {
                if (showOptions) {
                    Text(
                        Strings.Formatted.mapUpdatePrompt(existingMapName!!),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = {
                            onUpdate?.invoke(existingMapName)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(Strings.Map.updateExisting)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showOptions = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(Strings.Map.saveAsNewMap)
                    }
                } else {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(Strings.Map.routeNameLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (!showOptions) {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSave(name.trim())
                            onDismiss()
                        }
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text(Strings.Action.save)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (!showOptions && isEditingExistingMap) {
                    showOptions = true
                    name = existingMapName
                } else {
                    onDismiss()
                }
            }) {
                Text(if (!showOptions && isEditingExistingMap) Strings.Action.back else Strings.Action.cancel)
            }
        }
    )
}
