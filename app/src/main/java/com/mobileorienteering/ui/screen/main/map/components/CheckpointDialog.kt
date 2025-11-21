package com.mobileorienteering.ui.screen.main.map.components

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
            title = { Text("Dodaj punkt kontrolny") },
            text = {
                Column {
                    Text("Wprowadź nazwę punktu:")
                    TextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        singleLine = true,
                        placeholder = { Text("Nazwa (opcjonalna)") }
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
                    Text("Dodaj")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Anuluj")
                }
            }
        )
    }
}