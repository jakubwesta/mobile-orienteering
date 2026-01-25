package com.mobileorienteering.ui.screens.runs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobileorienteering.R
import com.mobileorienteering.ui.core.Strings
import com.mobileorienteering.data.model.domain.Activity
import com.mobileorienteering.data.model.domain.ActivityStatus
import com.mobileorienteering.data.model.domain.Checkpoint
import com.mobileorienteering.data.model.domain.OrienteeringMap
import com.mobileorienteering.data.model.domain.VisitedControlPoint
import com.mobileorienteering.ui.screens.runs.components.RunMapPreview
import com.mobileorienteering.ui.screens.runs.components.RunStatsCard
import com.mobileorienteering.ui.screens.runs.components.RunTimeline
import com.mobileorienteering.util.computeVisitedControlPoints
import com.mobileorienteering.util.formatDate
import com.mobileorienteering.util.formatTime
import org.maplibre.spatialk.geojson.Position
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunDetailsScreen(
    activityId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val activity by viewModel.getActivity(activityId).collectAsState(initial = null)
    val map by viewModel.getMapForActivity(activityId).collectAsState(initial = null)
    val isLoading by remember { viewModel.isLoading }
    val checkpointRadius by viewModel.checkpointRadius.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.Run.detailsTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_left),
                            contentDescription = Strings.Action.back
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
                RunDetailsContent(
                    activity = activity!!,
                    map = map,
                    checkpointRadius = checkpointRadius,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun RunDetailsContent(
    activity: Activity,
    map: OrienteeringMap?,
    checkpointRadius: Int,
    modifier: Modifier = Modifier
) {
    val stablePathData = remember(activity.id) { activity.pathData }
    val stableCheckpoints = remember(activity.id, map?.id) {
        map?.controlPoints?.mapIndexed { index, cp ->
            Checkpoint(
                position = Position(cp.longitude, cp.latitude),
                name = "Point ${index + 1}"
            )
        } ?: emptyList()
    }

    val visitedControlPoints = remember(activity.id, map?.id, checkpointRadius) {
        if (activity.visitedControlPoints.isNotEmpty()) {
            activity.visitedControlPoints
        } else if (map != null && activity.pathData.isNotEmpty()) {
            computeVisitedControlPoints(
                pathData = activity.pathData,
                controlPoints = map.controlPoints,
                radiusMeters = checkpointRadius
            )
        } else {
            emptyList()
        }
    }

    val stableVisitedIndices = remember(activity.id, visitedControlPoints) {
        visitedControlPoints.map { it.order - 1 }.toSet()
    }

    val computedStatus = remember(visitedControlPoints.size, map?.controlPoints?.size) {
        when {
            map == null -> activity.status
            map.controlPoints.isEmpty() -> ActivityStatus.COMPLETED
            visitedControlPoints.size >= map.controlPoints.size -> ActivityStatus.COMPLETED
            else -> ActivityStatus.ABANDONED
        }
    }

    Column(modifier = modifier) {
        RunHeader(
            title = activity.title,
            startTime = activity.startTime,
            status = computedStatus
        )

        if (stablePathData.isNotEmpty()) {
            RunMapPreview(
                pathData = stablePathData,
                checkpoints = stableCheckpoints,
                visitedIndices = stableVisitedIndices,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val sortedVisitedPoints = remember(visitedControlPoints) {
            visitedControlPoints.sortedBy { it.visitedAt }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RunStatsCard(
                distance = activity.distance,
                duration = activity.duration,
                startTime = activity.startTime,
                pathData = stablePathData,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (sortedVisitedPoints.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                Text(
                    Strings.Run.detailsSplits,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                val startPoint = if (stablePathData.isNotEmpty()) {
                    val firstPath = stablePathData.minByOrNull { it.timestamp }!!
                    VisitedControlPoint(
                        controlPointName = Strings.Run.detailsStart,
                        order = 0,
                        visitedAt = activity.startTime,
                        latitude = firstPath.latitude,
                        longitude = firstPath.longitude
                    )
                } else null

                val timelinePoints = if (startPoint != null) {
                    listOf(startPoint) + sortedVisitedPoints
                } else {
                    sortedVisitedPoints
                }

                RunTimeline(
                    startTime = activity.startTime,
                    visitedPoints = timelinePoints,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else if (map != null && map.controlPoints.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                Text(
                    Strings.Run.detailsNoControlPoints,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RunHeader(
    title: String,
    startTime: Instant,
    status: ActivityStatus
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = when (status) {
                    ActivityStatus.COMPLETED -> Strings.Run.detailsCompleted
                    ActivityStatus.ABANDONED -> Strings.Run.detailsAbandoned
                    ActivityStatus.IN_PROGRESS -> Strings.Run.detailsInProgress
                },
                style = MaterialTheme.typography.labelMedium,
                color = when (status) {
                    ActivityStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                    ActivityStatus.ABANDONED -> MaterialTheme.colorScheme.error
                    ActivityStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary
                }
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                formatDate(startTime),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                formatTime(startTime),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
