package com.mobileorienteering.ui.screens.runs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobileorienteering.R
import com.mobileorienteering.data.model.app.MapIconStyle
import com.mobileorienteering.data.model.app.MapStyle
import com.mobileorienteering.ui.core.Strings
import com.mobileorienteering.ui.screens.runs.components.RunDetailsSummary
import com.mobileorienteering.ui.screens.runs.components.RunMapPreview
import com.mobileorienteering.util.formatPlaybackSpeedKmh
import com.mobileorienteering.util.interpolatePathPosition
import com.mobileorienteering.util.pathPlaybackDurationMillis
import com.mobileorienteering.util.playbackSpeedKmh

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunDetailsScreen(
    runId: Long,
    onNavigateBack: () -> Unit,
    onViewSplits: () -> Unit,
    viewModel: RunViewModel = hiltViewModel()
) {
    val run by viewModel.getRun(runId).collectAsState(initial = null)
    val mapStyle by viewModel.mapStyle.collectAsState()
    val mapIconStyle by viewModel.mapIconStyle.collectAsState()
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
                    mapStyle = mapStyle,
                    mapIconStyle = mapIconStyle,
                    checkpointRadius = run!!.runSettings.detectionRadius.toInt(),
                    onViewSplits = onViewSplits,
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
    run: com.mobileorienteering.data.model.domain.Run,
    mapStyle: MapStyle,
    mapIconStyle: MapIconStyle,
    checkpointRadius: Int,
    onViewSplits: () -> Unit,
    modifier: Modifier = Modifier
) {
    val startPointLabel = Strings.Run.detailsStart
    val detailsData = remember(run.id, checkpointRadius, run.runSettings.orderedControlPoints, startPointLabel) {
        run.toDetailsData(checkpointRadius, startPointLabel)
    }
    val pathData = detailsData.sortedPathPoints

    var isPlaying by remember(run.id) { mutableStateOf(false) }
    var progress by remember(run.id) { mutableFloatStateOf(0f) }

    val playbackDurationMs = remember(pathData) { pathPlaybackDurationMillis(pathData) }
    val showPlaybackMarker = isPlaying || progress > 0f
    val playbackPosition = if (pathData.isEmpty() || !showPlaybackMarker) {
        null
    } else {
        interpolatePathPosition(pathData, progress)
    }
    val playbackSpeedLabel = playbackPosition?.let {
        formatPlaybackSpeedKmh(playbackSpeedKmh(pathData, progress) ?: 0.0)
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying || pathData.size < 2) return@LaunchedEffect

        if (progress >= 1f) {
            progress = 0f
        }

        val startProgress = progress
        val remaining = 1f - startProgress
        val durationMs = (playbackDurationMs * remaining).toLong().coerceAtLeast(1L)
        val startTime = withFrameMillis { it }

        while (isPlaying) {
            val elapsed = withFrameMillis { it } - startTime
            if (elapsed >= durationMs) {
                progress = 1f
                break
            }
            progress = startProgress + remaining * (elapsed / durationMs.toFloat())
        }

        if (!isPlaying) return@LaunchedEffect

        progress = 0f
        isPlaying = false
    }

    Column(modifier = modifier) {
        if (pathData.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                RunMapPreview(
                    pathData = pathData,
                    checkpoints = detailsData.stableCheckpoints,
                    visitedIndices = detailsData.visitedIndices,
                    mapStyle = mapStyle,
                    mapIconStyle = mapIconStyle,
                    playbackPosition = playbackPosition,
                    playbackSpeedLabel = playbackSpeedLabel,
                    modifier = Modifier.fillMaxSize()
                )

                if (pathData.size >= 2) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            isPlaying = !isPlaying
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        icon = {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null
                            )
                        },
                        text = {
                            Text(
                                if (isPlaying) {
                                    Strings.Run.detailsPauseTrack
                                } else {
                                    Strings.Run.detailsPlayTrack
                                }
                            )
                        }
                    )
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RunDetailsSummary(
                pathPoints = pathData,
                startedAt = run.startedAt,
                finishedAt = run.finishedAt,
                visitedControlPointCount = detailsData.visitedControlPointCount,
                totalControlPointCount = detailsData.totalControlPointCount
            )

            Button(
                onClick = onViewSplits,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Strings.Run.detailsViewSplits)
            }
        }
    }
}
