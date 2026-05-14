package com.mobileorienteering.ui.screens.runs.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobileorienteering.R
import com.mobileorienteering.data.model.domain.OrientationType
import com.mobileorienteering.data.model.domain.RaceStyle
import com.mobileorienteering.data.model.domain.RunSettings
import com.mobileorienteering.data.model.domain.TimerStart
import com.mobileorienteering.ui.core.Strings

@Composable
fun RunSettingsDialog(
    settings: RunSettings,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = Strings.Run.optionsTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsRow(
                    label = Strings.Run.optionsCheckpointOrder,
                    value = if (settings.orderedControlPoints) Strings.Run.optionsSequential else Strings.Run.optionsAnyOrder
                )
                HorizontalDivider()
                SettingsRow(
                    label = Strings.Run.optionsTimerStart,
                    value = when (settings.timerStart) {
                        TimerStart.RACE_START -> Strings.Run.optionsStartButton
                        TimerStart.FIRST_POINT -> Strings.Run.optionsFirstCheckpoint
                    }
                )
                HorizontalDivider()
                SettingsRow(
                    label = Strings.Run.optionsRunStyle,
                    value = when (settings.raceStyle) {
                        RaceStyle.STANDARD -> Strings.Run.optionsClassic
                        RaceStyle.COMPASS_BEARING -> Strings.Run.optionsAzimuth
                    },
                    icon = when (settings.raceStyle) {
                        RaceStyle.STANDARD -> R.drawable.ic_map_outlined
                        RaceStyle.COMPASS_BEARING -> R.drawable.ic_compass
                    }
                )
                HorizontalDivider()
                SettingsRow(
                    label = Strings.Run.optionsOrientationType,
                    value = when (settings.orientationType) {
                        OrientationType.FOOT -> Strings.Run.optionsFoot
                        OrientationType.BICYCLE -> Strings.Run.optionsCycling
                    },
                    icon = when (settings.orientationType) {
                        OrientationType.FOOT -> R.drawable.ic_run
                        OrientationType.BICYCLE -> R.drawable.ic_bike
                    }
                )
                HorizontalDivider()
                SettingsRow(
                    label = Strings.Run.optionsDetectionRadius,
                    value = "${settings.detectionRadius.toInt()} m"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.Action.close)
            }
        }
    )
}

@Composable
private fun SettingsRow(
    label: String,
    value: String,
    icon: Int? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
