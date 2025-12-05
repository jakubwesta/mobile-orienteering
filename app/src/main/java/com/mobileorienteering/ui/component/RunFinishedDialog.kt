package com.mobileorienteering.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RunFinishedDialog(
    isCompleted: Boolean,
    duration: String,
    visitedCount: Int,
    totalCount: Int,
    distance: Double,
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Nie pozwalamy zamknąć bez decyzji */ },
        icon = {
            Icon(
                imageVector = if (isCompleted) Icons.Default.Star else Icons.Default.Close,
                contentDescription = null,
                tint = if (isCompleted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                if (isCompleted) "Run Completed! 🎉" else "Run Stopped",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
            Button(onClick = onSave) {
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
        String.format("%.2f km", meters / 1000)
    } else {
        String.format("%.0f m", meters)
    }
}