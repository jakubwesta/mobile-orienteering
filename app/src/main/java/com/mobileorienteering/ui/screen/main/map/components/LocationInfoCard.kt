package com.mobileorienteering.ui.screen.main.map.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobileorienteering.ui.screen.main.map.models.MapState

@Composable
fun LocationInfoCard(
    state: MapState,
    modifier: Modifier = Modifier
) {
    if (state.isTracking && state.currentLocation != null) {
        Card(
            modifier = modifier
                .widthIn(max = 200.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Tracking aktywny",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )

                HorizontalDivider()

                state.currentLocation?.let { location ->
                    Text(
                        "Lat: %.5f".format(location.latitude),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Lon: %.5f".format(location.longitude),
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (location.hasAccuracy()) {
                        Text(
                            "Dokładność: %.0f m".format(location.accuracy),
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                location.accuracy < 10 -> MaterialTheme.colorScheme.primary
                                location.accuracy < 30 -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            }
        }
    }
}