package com.mobileorienteering.ui.screens.runs.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobileorienteering.R
import com.mobileorienteering.ui.core.Strings
import com.mobileorienteering.data.model.domain.PathPoint
import com.mobileorienteering.util.calculatePace
import com.mobileorienteering.util.formatDistance
import com.mobileorienteering.util.formatDuration
import java.time.Duration
import java.time.Instant

@Composable
fun RunStatsCard(
    distance: Double,
    duration: String,
    startTime: Instant,
    pathData: List<PathPoint>,
    modifier: Modifier = Modifier
) {
    val durationSeconds = try {
        Duration.parse(duration).seconds
    } catch (e: Exception) {
        val parts = duration.split(":")
        when (parts.size) {
            2 -> parts[0].toLongOrNull()?.times(60)?.plus(parts[1].toLongOrNull() ?: 0) ?: 0L
            3 -> parts[0].toLongOrNull()?.times(3600)
                ?.plus(parts[1].toLongOrNull()?.times(60) ?: 0)
                ?.plus(parts[2].toLongOrNull() ?: 0) ?: 0L
            else -> 0L
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = Strings.Run.distanceLabel,
                value = formatDistance(distance),
                icon = R.drawable.ic_route
            )

            StatItem(
                label = Strings.Run.durationLabel,
                value = formatDuration(duration),
                icon = R.drawable.ic_runs_outlined
            )

            StatItem(
                label = Strings.Run.paceLabel,
                value = calculatePace(distance, durationSeconds),
                icon = R.drawable.ic_play_arrow
            )
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    icon: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
