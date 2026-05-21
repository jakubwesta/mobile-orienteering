package com.mobileorienteering.ui.screens.runs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobileorienteering.R
import com.mobileorienteering.ui.core.Strings
import com.mobileorienteering.ui.screens.runs.components.RunTimeline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunSplitsScreen(
    runId: Long,
    onNavigateBack: () -> Unit,
    viewModel: RunViewModel = hiltViewModel()
) {
    val run by viewModel.getRun(runId).collectAsState(initial = null)
    val isLoading by remember { viewModel.isLoading }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.Run.detailsSplits) },
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
                val startPointLabel = Strings.Run.detailsStart
                val detailsData = remember(
                    run!!.id,
                    run!!.runSettings.detectionRadius,
                    run!!.runSettings.orderedControlPoints,
                    run!!.runSettings.timerStart,
                    startPointLabel
                ) {
                    run!!.toDetailsData(
                        checkpointRadius = run!!.runSettings.detectionRadius.toInt(),
                        startPointLabel = startPointLabel
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (detailsData.sortedVisitedPoints.isNotEmpty()) {
                        RunTimeline(
                            startTime = run!!.startedAt,
                            visitedPoints = detailsData.timelinePoints
                        )
                    } else if (detailsData.totalControlPointCount > 0) {
                        HorizontalDivider()
                        Text(
                            Strings.Run.detailsNoControlPoints,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
