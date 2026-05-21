package com.mobileorienteering.ui.screens.runs.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mobileorienteering.ui.core.Strings
import com.mobileorienteering.data.model.domain.VisitedControlPoint
import com.mobileorienteering.util.calculateDistanceBetweenPoints
import com.mobileorienteering.util.calculatePaceBetweenInstants
import com.mobileorienteering.util.formatDistance
import com.mobileorienteering.util.formatDurationFromInstants
import java.time.Instant

@Composable
fun RunTimeline(
    startTime: Instant,
    visitedPoints: List<VisitedControlPoint>,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.primary
    val pointRadius = 10.dp
    val lineWidth = 3.dp

    Column(modifier = modifier.fillMaxWidth()) {
        visitedPoints.forEachIndexed { index, visitedPoint ->
            val isFirst = index == 0
            val isLast = index == visitedPoints.lastIndex

            val previousTime = if (isFirst) startTime else visitedPoints[index - 1].visitedAt
            val intervalDuration = formatDurationFromInstants(previousTime, visitedPoint.visitedAt)
            val totalTime = formatDurationFromInstants(startTime, visitedPoint.visitedAt)

            val distanceFromPrevious = if (isFirst) {
                null
            } else {
                val prev = visitedPoints[index - 1]
                calculateDistanceBetweenPoints(
                    prev.lat, prev.lon,
                    visitedPoint.lat, visitedPoint.lon
                )
            }

            val totalDistance = if (isFirst) {
                0.0
            } else {
                var sum = 0.0
                for (i in 1..index) {
                    val prevCp = visitedPoints[i - 1]
                    val currCp = visitedPoints[i]
                    sum += calculateDistanceBetweenPoints(
                        prevCp.lat, prevCp.lon,
                        currCp.lat, currCp.lon
                    )
                }
                sum
            }

            val segmentPace = if (distanceFromPrevious != null && distanceFromPrevious > 0) {
                calculatePaceBetweenInstants(distanceFromPrevious, previousTime, visitedPoint.visitedAt)
            } else {
                null
            }

            val checkpointLabel = if (visitedPoint.checkpointIndex < 0) {
                visitedPoint.controlPointName
            } else {
                "${visitedPoint.checkpointIndex + 1}"
            }

            TimelineItem(
                checkpointLabel = checkpointLabel,
                intervalDuration = intervalDuration,
                totalTime = totalTime,
                distanceFromPrevious = distanceFromPrevious,
                totalDistance = totalDistance,
                segmentPace = segmentPace,
                isFirst = isFirst,
                isLast = isLast,
                lineColor = lineColor,
                pointColor = pointColor,
                pointRadius = pointRadius,
                lineWidth = lineWidth
            )
        }
    }
}

@Composable
private fun TimelineItem(
    checkpointLabel: String,
    intervalDuration: String,
    totalTime: String,
    distanceFromPrevious: Double?,
    totalDistance: Double,
    segmentPace: String?,
    isFirst: Boolean,
    isLast: Boolean,
    lineColor: Color,
    pointColor: Color,
    pointRadius: Dp,
    lineWidth: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(pointRadius * 2 + 4.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2
                val pointRadiusPx = pointRadius.toPx()
                val lineWidthPx = lineWidth.toPx()

                if (!isFirst) {
                    drawLine(
                        color = lineColor,
                        start = Offset(centerX, 0f),
                        end = Offset(centerX, pointRadiusPx),
                        strokeWidth = lineWidthPx
                    )
                }

                if (!isLast) {
                    drawLine(
                        color = lineColor,
                        start = Offset(centerX, pointRadiusPx * 2),
                        end = Offset(centerX, size.height),
                        strokeWidth = lineWidthPx
                    )
                }

                drawCircle(
                    color = pointColor,
                    radius = pointRadiusPx,
                    center = Offset(centerX, pointRadiusPx)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = checkpointLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TimelineStat(
                    label = Strings.Run.detailsIntervalLabel,
                    value = intervalDuration
                )

                if (distanceFromPrevious != null) {
                    TimelineStat(
                        label = Strings.Run.detailsDistLabel,
                        value = formatDistance(distanceFromPrevious)
                    )
                }
            }

            if (segmentPace != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TimelineStat(
                        label = Strings.Run.detailsPaceLabel,
                        value = segmentPace,
                        valueColor = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TimelineStat(
                    label = Strings.Run.detailsTotalLabel,
                    value = totalTime,
                    valueColor = MaterialTheme.colorScheme.primary
                )

                TimelineStat(
                    label = Strings.Run.detailsTotalDistLabel,
                    value = formatDistance(totalDistance),
                    valueColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun TimelineStat(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alignBy(FirstBaseline)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            modifier = Modifier.alignBy(FirstBaseline)
        )
    }
}
