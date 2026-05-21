package com.mobileorienteering.ui.screens.runs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import kotlinx.coroutines.launch

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
    val playbackProgress = remember(run.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val playbackDurationMs = remember(pathData) { pathPlaybackDurationMillis(pathData) }
    val playbackPosition = remember(pathData, playbackProgress.value) {
        if (playbackProgress.value <= 0f && !isPlaying) {
            null
        } else {
            interpolatePathPosition(pathData, playbackProgress.value)
        }
    }
    val playbackSpeedLabel = remember(pathData, playbackProgress.value, playbackPosition) {
        if (playbackPosition == null) {
            null
        } else {
            formatPlaybackSpeedKmh(playbackSpeedKmh(pathData, playbackProgress.value) ?: 0.0)
        }
    }

    LaunchedEffect(isPlaying, pathData, playbackDurationMs) {
        if (!isPlaying || pathData.size < 2) return@LaunchedEffect

        val remainingProgress = 1f - playbackProgress.value
        val durationMs = (playbackDurationMs * remainingProgress).toInt().coerceAtLeast(1)
        playbackProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = durationMs,
                easing = LinearEasing
            )
        )
        playbackProgress.snapTo(0f)
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
                            if (isPlaying) {
                                isPlaying = false
                                scope.launch { playbackProgress.stop() }
                            } else {
                                if (playbackProgress.value >= 1f) {
                                    scope.launch { playbackProgress.snapTo(0f) }
                                }
                                isPlaying = true
                            }
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
                                stringResource(
                                    if (isPlaying) {
                                        R.string.run_details_pause_track
                                    } else {
                                        R.string.run_details_play_track
                                    }
                                )
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
                Text(stringResource(R.string.run_details_view_splits))
            }
        }
    }
}
