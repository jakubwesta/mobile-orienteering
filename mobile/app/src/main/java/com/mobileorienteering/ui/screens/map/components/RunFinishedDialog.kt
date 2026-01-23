package com.mobileorienteering.ui.screens.map.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun RunFinishedDialog(
    isCompleted: Boolean,
    duration: String,
    visitedCount: Int,
    totalCount: Int,
    distance: Double,
    defaultTitle: String = "",
    onSave: (String) -> Unit,
    onDiscard: () -> Unit
) {
    var runTitle by remember { mutableStateOf(defaultTitle) }

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                if (isCompleted) "Run Completed!" else "Run Stopped",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = runTitle,
                    onValueChange = { runTitle = it },
                    label = { Text("Run name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Time: $duration")
                Text("Control Points: $visitedCount/$totalCount")
                Text("Distance: ${formatDistance(distance)}")

                val progress = if (totalCount > 0) visitedCount.toFloat() / totalCount else 0f
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Text("${(progress * 100).toInt()}% completed")
            }
        },
        confirmButton = {
            Button(onClick = { onSave(runTitle) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) {
                Text("Discard", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

private fun formatDistance(meters: Double): String {
    return if (meters >= 1000) {
        String.format(Locale.US, "%.2f km", meters / 1000)
    } else {
        String.format(Locale.US, "%.0f m", meters)
    }
}
