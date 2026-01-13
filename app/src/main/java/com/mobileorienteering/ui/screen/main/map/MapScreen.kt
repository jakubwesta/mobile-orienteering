package com.mobileorienteering.ui.screen.main.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobileorienteering.ui.screen.main.map.components.*
import com.mobileorienteering.ui.component.RunFinishedDialog
import com.mobileorienteering.ui.component.RunProgressPanel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.*
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.rememberStyleState
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Position
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
    initialMapId: Long? = null,
    startRun: Boolean = false
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val finishedRunState by viewModel.finishedRunState.collectAsStateWithLifecycle()
    val mapZoom by viewModel.mapZoom.collectAsStateWithLifecycle()
    val showLocationDuringRun by viewModel.showLocationDuringRun.collectAsStateWithLifecycle()
    val centerCameraOnce by viewModel.centerCameraOnce.collectAsStateWithLifecycle()
    val cameraState = rememberCameraState()
    val styleState = rememberStyleState()
    val coroutineScope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()

    var tapPosition by remember { mutableStateOf<Position?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    val shouldMoveCamera by viewModel.shouldMoveCamera.collectAsStateWithLifecycle()

    var draggingCheckpointIndex by remember { mutableStateOf<Int?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.updatePermissionState()
    }

    LaunchedEffect(initialMapId) {
        if (initialMapId != null) {
            viewModel.loadMap(initialMapId)
        }
    }

    var hasAutoStarted by remember { mutableStateOf(false) }
    LaunchedEffect(startRun, state.checkpoints) {
        if (startRun && state.checkpoints.isNotEmpty() && !state.isRunActive && !hasAutoStarted) {
            hasAutoStarted = true
            viewModel.startRun()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            viewModel.startTracking()
        }
    }

    LaunchedEffect(shouldMoveCamera) {
        if (shouldMoveCamera && state.checkpoints.isNotEmpty()) {
            val firstCheckpoint = state.checkpoints.first()
            coroutineScope.launch {
                cameraState.animateTo(
                    CameraPosition(
                        target = Position(
                            firstCheckpoint.position.longitude,
                            firstCheckpoint.position.latitude
                        ),
                        zoom = 15.0
                    )
                )
                viewModel.cameraMoved()
            }
        }
    }

    // Jednorazowe centrowanie kamery przy włączeniu lokalizacji lub na żądanie
    LaunchedEffect(centerCameraOnce, state.currentLocation) {
        if (centerCameraOnce) {
            val location = state.currentLocation ?: return@LaunchedEffect
            coroutineScope.launch {
                cameraState.animateTo(
                    CameraPosition(
                        target = Position(location.longitude, location.latitude),
                        zoom = if (cameraState.position.zoom < mapZoom) mapZoom else cameraState.position.zoom
                    )
                )
                viewModel.cameraCentered()
            }
        }
    }

    LaunchedEffect(Unit) {
        cameraState.position = CameraPosition(
            target = Position(21.0122, 52.2297),
            zoom = 10.0
        )
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = if (state.isRunActive) 0.dp else 64.dp,
        sheetDragHandle = { if (!state.isRunActive) MapBottomSheetHandle() },
        sheetContent = {
            if (!state.isRunActive) {
                CheckpointBottomSheetContent(
                    state = state,
                    viewModel = viewModel,
                    onSaveRoute = { showSaveDialog = true }
                )
            }
        }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            MaplibreMap(
                modifier = Modifier.fillMaxSize(),
                cameraState = cameraState,
                styleState = styleState,
                baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
                options = MapOptions(
                    ornamentOptions = OrnamentOptions.AllDisabled,
                    gestureOptions = GestureOptions.Standard
                ),
                onMapClick = { point, _ ->
                    if (draggingCheckpointIndex != null) {
                        viewModel.moveCheckpoint(draggingCheckpointIndex!!, point.longitude, point.latitude)
                        draggingCheckpointIndex = null
                    } else if (!state.isRunActive) {
                        tapPosition = point
                    }
                    ClickResult.Pass
                }
            ) {
                // Pokaż trasę i lokalizację tylko gdy:
                // - bieg nie jest aktywny, LUB
                // - bieg jest aktywny i showLocationDuringRun jest true
                val shouldShowLocation = !state.isRunActive || showLocationDuringRun

                if (shouldShowLocation) {
                    RoutePathLayer(
                        pathData = state.runPathData,
                        color = Color(0xFF2196F3),
                        width = 4f
                    )

                    NextCheckpointLineLayer(
                        currentLocation = state.currentLocation,
                        nextCheckpoint = if (state.nextCheckpointIndex < state.checkpoints.size) {
                            state.checkpoints[state.nextCheckpointIndex]
                        } else null,
                        isRunActive = state.isRunActive
                    )
                }

                CheckpointsLayer(
                    checkpoints = state.checkpoints,
                    visitedIndices = state.visitedCheckpointIndices,
                    nextCheckpointIndex = state.nextCheckpointIndex,
                    isRunActive = state.isRunActive,
                    draggingIndex = draggingCheckpointIndex,
                    onCheckpointLongClick = { index ->
                        draggingCheckpointIndex = index
                    }
                )

                if (shouldShowLocation) {
                    UserLocationLayer(location = state.currentLocation)
                }
            }

            if (draggingCheckpointIndex != null) {
                DraggingInfoBanner(
                    checkpointNumber = draggingCheckpointIndex!! + 1,
                    onCancel = { draggingCheckpointIndex = null },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                )
            }

            if (draggingCheckpointIndex == null) {
                RunProgressPanel(
                    isVisible = state.isRunActive,
                    startTime = state.runStartTime,
                    visitedCount = state.visitedCheckpointIndices.size,
                    totalCount = state.checkpoints.size,
                    distance = state.runDistance,
                    nextCheckpointIndex = state.nextCheckpointIndex,
                    onStopClick = { viewModel.stopRun() },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            if (!state.isRunActive && draggingCheckpointIndex == null) {
                TrackingButton(
                    isTracking = state.isTracking,
                    onClick = {
                        if (state.isTracking) {
                            viewModel.stopTracking()
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp + 64.dp)
                )

                if (state.currentLocation != null) {
                    AddCheckpointButton(
                        onClick = {
                            state.currentLocation?.let { location ->
                                tapPosition = Position(location.longitude, location.latitude)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 88.dp + 64.dp)
                    )

                    CenterCameraButton(
                        onClick = { viewModel.requestCenterCamera() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 160.dp + 64.dp)
                    )
                }
            }

            // Przycisk centrowania podczas biegu - zawsze widoczny
            if (state.isRunActive && state.currentLocation != null && draggingCheckpointIndex == null) {
                CenterCameraButton(
                    onClick = { viewModel.requestCenterCamera() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                )
            }

            if (draggingCheckpointIndex == null) {
                CheckpointDialog(
                    position = tapPosition,
                    onDismiss = { tapPosition = null },
                    onConfirm = { name ->
                        tapPosition?.let { pos ->
                            viewModel.addCheckpoint(
                                longitude = pos.longitude,
                                latitude = pos.latitude,
                                name = name
                            )
                        }
                    }
                )
            }

            if (showSaveDialog) {
                SaveRouteDialog(
                    onDismiss = { showSaveDialog = false },
                    onSave = { name ->
                        viewModel.saveCurrentMap(name)
                        showSaveDialog = false
                    }
                )
            }

            finishedRunState?.let { run ->
                RunFinishedDialog(
                    isCompleted = run.isCompleted,
                    duration = run.duration,
                    visitedCount = run.visitedControlPoints.size,
                    totalCount = run.totalCheckpoints,
                    distance = run.distance,
                    defaultTitle = "Run: ${run.mapName}",
                    onSave = { title -> viewModel.saveFinishedRun(title) },
                    onDiscard = { viewModel.discardFinishedRun() }
                )
            }
        }
    }
}