package com.mobileorienteering.ui.screens.library.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobileorienteering.R
import com.mobileorienteering.data.model.app.RunSettings
import com.mobileorienteering.data.model.domain.OrientationType
import com.mobileorienteering.data.model.domain.RaceStyle
import com.mobileorienteering.data.model.domain.TimerStart
import com.mobileorienteering.ui.core.Strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunSettingsBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (RunSettings) -> Unit
) {
    var orderedControlPoints by remember { mutableStateOf(true) }
    var timerStart by remember { mutableStateOf(TimerStart.RACE_START) }
    var raceStyle by remember { mutableStateOf(RaceStyle.STANDARD) }
    var orientationType by remember { mutableStateOf(OrientationType.FOOT) }
    var detectionRadius by remember { mutableFloatStateOf(15f) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Strings.Run.optionsTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            OptionSection(label = Strings.Run.optionsCheckpointOrder) {
                PillToggle(
                    options = listOf(
                        Strings.Run.optionsSequential to orderedControlPoints,
                        Strings.Run.optionsAnyOrder to !orderedControlPoints
                    ),
                    onSelect = { index -> orderedControlPoints = index == 0 }
                )
            }

            OptionSection(label = Strings.Run.optionsTimerStart) {
                PillToggle(
                    options = listOf(
                        Strings.Run.optionsStartButton to (timerStart == TimerStart.RACE_START),
                        Strings.Run.optionsFirstCheckpoint to (timerStart == TimerStart.FIRST_POINT)
                    ),
                    onSelect = { index ->
                        timerStart = if (index == 0) TimerStart.RACE_START else TimerStart.FIRST_POINT
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SectionLabel(Strings.Run.optionsRunStyle)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectableIconCard(
                            label = Strings.Run.optionsClassic,
                            iconRes = R.drawable.ic_map_outlined,
                            selected = raceStyle == RaceStyle.STANDARD,
                            onClick = { raceStyle = RaceStyle.STANDARD },
                            modifier = Modifier.weight(1f)
                        )
                        SelectableIconCard(
                            label = Strings.Run.optionsAzimuth,
                            iconRes = R.drawable.ic_compass,
                            selected = raceStyle == RaceStyle.COMPASS_BEARING,
                            onClick = { raceStyle = RaceStyle.COMPASS_BEARING },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SectionLabel(Strings.Run.optionsOrientationType)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectableIconCard(
                            label = Strings.Run.optionsFoot,
                            iconRes = R.drawable.ic_run,
                            selected = orientationType == OrientationType.FOOT,
                            onClick = { orientationType = OrientationType.FOOT },
                            modifier = Modifier.weight(1f)
                        )
                        SelectableIconCard(
                            label = Strings.Run.optionsCycling,
                            iconRes = R.drawable.ic_bike,
                            selected = orientationType == OrientationType.BICYCLE,
                            onClick = { orientationType = OrientationType.BICYCLE },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            OptionSection(label = Strings.Run.optionsDetectionRadius) {
                Column {
                    Text(
                        text = "${detectionRadius.toInt()} m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = detectionRadius,
                        onValueChange = { detectionRadius = it },
                        valueRange = 5f..50f,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Button(
                onClick = {
                    onConfirm(
                        RunSettings(
                            orderedControlPoints = orderedControlPoints,
                            timerStart = timerStart,
                            raceStyle = raceStyle,
                            orientationType = orientationType,
                            detectionRadius = detectionRadius
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = Strings.Run.optionsInitializeCourse,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun OptionSection(
    label: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(label)
        content()
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PillToggle(
    options: List<Pair<String, Boolean>>,
    onSelect: (Int) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (label, selected) ->
            SegmentedButton(
                selected = selected,
                onClick = { onSelect(index) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun SelectableIconCard(
    label: String,
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 4.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
