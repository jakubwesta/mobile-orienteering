package com.mobileorienteering.ui.screen.main.map.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mobileorienteering.R
import com.mobileorienteering.ui.screen.main.map.MapViewModel
import com.mobileorienteering.data.model.domain.MapState

@Composable
fun CheckpointBottomSheetContent(
    state: MapState,
    viewModel: MapViewModel,
    onSaveRoute: () -> Unit = {}
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            "Control points",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            state.checkpoints.forEachIndexed { index, checkpoint ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${index + 1}. ${checkpoint.name}", modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = { viewModel.moveCheckpointUp(checkpoint.id) },
                        enabled = index > 0
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Przesuń w górę")
                    }

                    IconButton(
                        onClick = { viewModel.moveCheckpointDown(checkpoint.id) },
                        enabled = index < state.checkpoints.size - 1
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Przesuń w dół")
                    }

                    IconButton(onClick = { viewModel.removeCheckpoint(checkpoint.id) }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_trash_filled),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        if (state.checkpoints.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.clearCheckpoints() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_trash_filled),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Delete all")
                }

                Button(
                    onClick = onSaveRoute,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save the route")
                }
            }
        }
    }
}