package com.mobileorienteering.ui.screen.main.runs

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
import com.mobileorienteering.data.model.domain.Activity
import com.mobileorienteering.data.model.domain.Checkpoint
import com.mobileorienteering.data.model.domain.OrienteeringMap
import com.mobileorienteering.ui.screen.main.runs.components.RunMapPreview
import com.mobileorienteering.ui.screen.main.runs.components.RunStatsCard
import com.mobileorienteering.ui.screen.main.runs.components.RunTimeline
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
                RunDetailsContent(
                    activity = activity!!,
                    map = map,
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RunHeader(
            title = activity.title,
            startTime = activity.startTime
        )

        if (activity.pathData.isNotEmpty()) {
            RunMapPreview(
                pathData = activity.pathData,
                checkpoints = map?.controlPoints?.mapIndexed { index, cp ->
                    Checkpoint(
                        position = Position(cp.longitude, cp.latitude),
                        name = "Punkt ${index + 1}"
                    )
                } ?: emptyList(),
                visitedIndices = activity.visitedControlPoints.map { it.order - 1 }.toSet(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }

        RunStatsCard(
            distance = activity.distance,
            duration = activity.duration,
            startTime = activity.startTime,
            pathData = activity.pathData,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        val visitedControlPoints = activity.visitedControlPoints.sortedBy { it.visitedAt }

        if (visitedControlPoints.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            Text(
                "Splits",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            RunTimeline(
                startTime = activity.startTime,
                visitedPoints = visitedControlPoints,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else if (activity.totalControlPoints > 0) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            Text(
                "No control points visited",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun RunHeader(
    title: String,
    startTime: Instant
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium
        )

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