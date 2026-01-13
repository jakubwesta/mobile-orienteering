package com.mobileorienteering.ui.screen.main.map.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
                if (showOptions) "Save map"
                else if (isEditingExistingMap) "Save as new"
                else "Save route"
            )
        },
        text = {
            Column {
                if (showOptions) {
                    Text(
                        "You are editing \"$existingMapName\". What would you like to do?",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = {
                            onUpdate?.invoke(existingMapName!!)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Update existing map")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showOptions = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save as new map")
                    }
                } else {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name of the route") },
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
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (!showOptions && isEditingExistingMap) {
                    showOptions = true
                    name = existingMapName ?: ""
                } else {
                    onDismiss()
                }
            }) {
                Text(if (!showOptions && isEditingExistingMap) "Back" else "Cancel")
            }
        }
    )
}