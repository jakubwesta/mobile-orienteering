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
import com.mobileorienteering.data.model.domain.Run
import com.mobileorienteering.data.model.domain.VisitedControlPoint
import com.mobileorienteering.data.model.app.Checkpoint
import com.mobileorienteering.ui.screens.runs.components.RunMapPreview
import com.mobileorienteering.ui.screens.runs.components.RunStatsCard
import com.mobileorienteering.ui.screens.runs.components.RunTimeline
import com.mobileorienteering.util.computeVisitedControlPoints
import com.mobileorienteering.util.formatDate
import com.mobileorienteering.util.formatTime
import org.maplibre.spatialk.geojson.Position
import java.time.Instant

private enum class RunStatus { COMPLETED, ABANDONED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunDetailsScreen(
    runId: Long,
    onNavigateBack: () -> Unit,
    viewModel: RunViewModel = hiltViewModel()
) {
    val run by viewModel.getRun(runId).collectAsState(initial = null)
    val isLoading by remember { viewModel.isLoading }

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
            isLoading || run == null -> {
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
                    run = run!!,
                    checkpointRadius = run!!.runSettings.detectionRadius.toInt(),
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
    run: Run,
    checkpointRadius: Int,
    modifier: Modifier = Modifier
) {
    val sortedPathPoints = remember(run.id) {
        run.pathPoints.sortedBy { it.timestamp }
    }

    val stableCheckpoints = remember(run.id) {
        run.map.controlPoints.map { cp ->
            Checkpoint(
                position = Position(cp.lon, cp.lat),
                name = cp.name
            )
        }
    }

    val visitedControlPoints = remember(run.id, checkpointRadius) {
        computeVisitedControlPoints(
            pathData = sortedPathPoints,
            controlPoints = run.map.controlPoints,
            radiusMeters = checkpointRadius
        )
    }

    val visitedIndices = remember(run.id, visitedControlPoints) {
        visitedControlPoints.map { it.order - 1 }.toSet()
    }

    val status = remember(run.id) {
        if (run.finishedAt != null) RunStatus.COMPLETED else RunStatus.ABANDONED
    }

    Column(modifier = modifier) {
        RunHeader(
            title = run.name,
            startTime = run.startedAt,
            status = status
        )

        if (sortedPathPoints.isNotEmpty()) {
            RunMapPreview(
                pathData = sortedPathPoints,
                checkpoints = stableCheckpoints,
                visitedIndices = visitedIndices,
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
                pathPoints = sortedPathPoints,
                startedAt = run.startedAt,
                finishedAt = run.finishedAt,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (sortedVisitedPoints.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                Text(
                    Strings.Run.detailsSplits,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                val startPoint = if (sortedPathPoints.isNotEmpty()) {
                    val firstPath = sortedPathPoints.first()
                    VisitedControlPoint(
                        controlPointName = Strings.Run.detailsStart,
                        order = 0,
                        visitedAt = run.startedAt,
                        lat = firstPath.lat,
                        lon = firstPath.lon
                    )
                } else null

                val timelinePoints = if (startPoint != null) {
                    listOf(startPoint) + sortedVisitedPoints
                } else {
                    sortedVisitedPoints
                }

                RunTimeline(
                    startTime = run.startedAt,
                    visitedPoints = timelinePoints,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else if (run.map.controlPoints.isNotEmpty()) {
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
    status: RunStatus
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
                    RunStatus.COMPLETED -> Strings.Run.detailsCompleted
                    RunStatus.ABANDONED -> Strings.Run.detailsAbandoned
                },
                style = MaterialTheme.typography.labelMedium,
                color = when (status) {
                    RunStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                    RunStatus.ABANDONED -> MaterialTheme.colorScheme.error
                }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
