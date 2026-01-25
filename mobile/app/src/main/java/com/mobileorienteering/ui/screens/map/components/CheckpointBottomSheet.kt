package com.mobileorienteering.ui.screens.map.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mobileorienteering.R
import com.mobileorienteering.ui.core.Strings
import com.mobileorienteering.ui.screens.map.MapViewModel
import com.mobileorienteering.data.model.domain.MapState

@Composable
fun CheckpointBottomSheetContent(
    state: MapState,
    viewModel: MapViewModel,
    onSaveRoute: () -> Unit = {}
) {
    var editingCheckpointId by remember { mutableStateOf<String?>(null) }
    var editingName by remember { mutableStateOf("") }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            Strings.Map.controlPoints,
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
                        onClick = {
                            editingCheckpointId = checkpoint.id
                            editingName = checkpoint.name
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = Strings.Accessibility.editName,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { viewModel.moveCheckpointUp(checkpoint.id) },
                        enabled = index > 0
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_up),
                            contentDescription = Strings.Accessibility.moveUp
                        )
                    }

                    IconButton(
                        onClick = { viewModel.moveCheckpointDown(checkpoint.id) },
                        enabled = index < state.checkpoints.size - 1
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_down),
                            contentDescription = Strings.Accessibility.moveDown
                        )
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

            if (state.currentMapId != null && state.currentMapName != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            Strings.Map.editingMap,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            state.currentMapName,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(
                        onClick = { viewModel.detachFromMap() }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(Strings.Map.detach)
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showDeleteAllDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_trash_filled),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(Strings.Map.deleteAll)
                }

                Button(
                    onClick = onSaveRoute,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(Strings.Map.saveRoute)
                }
            }
        }
    }

    if (editingCheckpointId != null) {
        AlertDialog(
            onDismissRequest = { editingCheckpointId = null },
            title = { Text(Strings.Map.editCheckpointName) },
            text = {
                OutlinedTextField(
                    value = editingName,
                    onValueChange = { editingName = it },
                    label = { Text(Strings.Map.checkpointNameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateCheckpointName(editingCheckpointId!!, editingName)
                        editingCheckpointId = null
                    }
                ) {
                    Text(Strings.Action.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCheckpointId = null }) {
                    Text(Strings.Action.cancel)
                }
            }
        )
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text(Strings.Map.deleteAllConfirmTitle) },
            text = { Text(Strings.Map.deleteAllConfirmMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCheckpoints()
                        showDeleteAllDialog = false
                    }
                ) {
                    Text(Strings.Action.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text(Strings.Action.cancel)
                }
            }
        )
    }
}
