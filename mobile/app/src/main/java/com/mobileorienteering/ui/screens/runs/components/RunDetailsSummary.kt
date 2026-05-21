package com.mobileorienteering.ui.screens.runs.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobileorienteering.R
import com.mobileorienteering.data.model.domain.PathPoint
import com.mobileorienteering.ui.core.Strings
import com.mobileorienteering.util.calculatePace
import com.mobileorienteering.util.calculateTotalDistance
import com.mobileorienteering.util.formatDistance
import com.mobileorienteering.util.formatDurationFromInstants
import java.time.Duration
import java.time.Instant

@Composable
fun RunDetailsSummary(
    pathPoints: List<PathPoint>,
    startedAt: Instant,
    finishedAt: Instant?,
    visitedControlPointCount: Int,
    totalControlPointCount: Int,
    modifier: Modifier = Modifier
) {
    val distance = remember(pathPoints) { calculateTotalDistance(pathPoints) }
    val durationText = remember(startedAt, finishedAt) {
        finishedAt?.let { formatDurationFromInstants(startedAt, it) } ?: "-"
    }
    val durationSeconds = remember(startedAt, finishedAt) {
        finishedAt?.let { Duration.between(startedAt, it).seconds } ?: 0L
    }
    val controlPointsText = remember(visitedControlPointCount, totalControlPointCount) {
        "$visitedControlPointCount/$totalControlPointCount"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = Strings.Run.distanceLabel,
                value = formatDistance(distance),
                icon = R.drawable.ic_route
            )
            StatItem(
                label = Strings.Run.paceLabel,
                value = calculatePace(distance, durationSeconds),
                icon = R.drawable.ic_play_arrow
            )
            StatItem(
                label = Strings.Run.timeLabel,
                value = durationText,
                icon = R.drawable.ic_runs_outlined
            )
            StatItem(
                label = Strings.Run.pointsLabel,
                value = controlPointsText,
                icon = R.drawable.ic_target
            )
        }
    }
}
