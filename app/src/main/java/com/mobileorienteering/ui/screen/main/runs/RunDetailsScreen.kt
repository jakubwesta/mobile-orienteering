package com.mobileorienteering.ui.screen.main.runs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobileorienteering.R
import com.mobileorienteering.data.model.VisitedControlPoint
import com.mobileorienteering.util.calculateDistanceBetweenPoints
import com.mobileorienteering.util.formatDate
import com.mobileorienteering.util.formatDistance
import com.mobileorienteering.util.formatDuration
import com.mobileorienteering.util.formatDurationFromInstants
import com.mobileorienteering.util.formatTime
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunDetailsScreen(
    activityId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val activity by viewModel.getActivity(activityId).collectAsState(initial = null)
    val isLoading by remember { viewModel.isLoading }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Run Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_left),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading || activity == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        activity!!.title,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            formatDate(activity!!.startTime),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            formatTime(activity!!.startTime),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Distance: ${formatDistance(activity!!.distance)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Duration: ${formatDuration(activity!!.duration)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    val visitedControlPoints = activity!!.visitedControlPoints.sortedBy {
                        it.visitedAt
                    }

                    if (visitedControlPoints.isNotEmpty()) {
                        HorizontalDivider()

                        RunTimeline(
                            startTime = activity!!.startTime,
                            visitedPoints = visitedControlPoints
                        )
                    } else if (activity!!.totalControlPoints > 0) {
                        HorizontalDivider()

                        Text(
                            "No control points visited",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RunTimeline(
    startTime: Instant,
    visitedPoints: List<VisitedControlPoint>
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.primary
    val pointRadius = 10.dp
    val lineWidth = 3.dp

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
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
                    prev.latitude, prev.longitude,
                    visitedPoint.latitude, visitedPoint.longitude
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
                        prevCp.latitude, prevCp.longitude,
                        currCp.latitude, currCp.longitude
                    )
                }
                sum
            }

            val checkpointLabel = when {
                isFirst -> "Start"
                isLast -> "Finish"
                else -> visitedPoint.controlPointName.ifEmpty { "Control Point ${visitedPoint.order}" }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.Top
            ) {
                // Timeline line with point
                Box(
                    modifier = Modifier
                        .width(pointRadius * 2 + 4.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val centerX = size.width / 2
                        val pointRadiusPx = pointRadius.toPx()
                        val lineWidthPx = lineWidth.toPx()

                        // Draw line above point
                        if (!isFirst) {
                            drawLine(
                                color = lineColor,
                                start = Offset(centerX, 0f),
                                end = Offset(centerX, pointRadiusPx),
                                strokeWidth = lineWidthPx
                            )
                        }

                        // Draw line below point
                        if (!isLast) {
                            drawLine(
                                color = lineColor,
                                start = Offset(centerX, pointRadiusPx * 2),
                                end = Offset(centerX, size.height),
                                strokeWidth = lineWidthPx
                            )
                        }

                        // Draw point circle
                        drawCircle(
                            color = pointColor,
                            radius = pointRadiusPx,
                            center = Offset(centerX, pointRadiusPx)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Checkpoint info (right side)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        text = checkpointLabel,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // First row: Interval and Distance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Interval:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = intervalDuration,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (distanceFromPrevious != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Distance:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatDistance(distanceFromPrevious),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    // Second row: Total Time and Total Distance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Total Time:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = totalTime,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Total Distance:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatDistance(totalDistance),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}