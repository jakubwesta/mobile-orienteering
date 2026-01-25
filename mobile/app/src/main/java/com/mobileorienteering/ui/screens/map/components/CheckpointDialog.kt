package com.mobileorienteering.ui.screens.map.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.TextFieldValue
import com.mobileorienteering.ui.core.Strings
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
            title = { Text(Strings.Map.addControlPoint) },
            text = {
                Column {
                    Text(Strings.Map.checkpointPrompt)
                    TextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        singleLine = true,
                        placeholder = { Text(Strings.Map.checkpointNameOptional) }
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
                    Text(Strings.Action.add)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(Strings.Action.cancel)
                }
            }
        )
    }
}
