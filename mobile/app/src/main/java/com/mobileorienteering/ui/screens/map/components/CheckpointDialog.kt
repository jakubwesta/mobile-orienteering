package com.mobileorienteering.ui.screens.map.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.TextFieldValue
import org.maplibre.spatialk.geojson.Position

@Composable
fun CheckpointDialog(
    position: Position?,
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit
) {
    if (position != null) {
        var nameText by remember { mutableStateOf(TextFieldValue("")) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add control point") },
            text = {
                Column {
                    Text("Enter the name of the point:")
                    TextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        singleLine = true,
                        placeholder = { Text("Name of the point (optional)") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm(nameText.text)
                        onDismiss()
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}