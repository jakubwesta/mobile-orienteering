package com.mobileorienteering.ui.screens.map

import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobileorienteering.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobileorienteering.ui.core.components.GpsSettingsDialog
import com.mobileorienteering.ui.core.components.LocationPermissionRationaleDialog
import com.mobileorienteering.ui.core.components.LocationPermissionSettingsDialog
import com.mobileorienteering.ui.screens.map.components.CheckpointBottomSheetContent
import com.mobileorienteering.ui.screens.map.components.CheckpointDialog
import com.mobileorienteering.ui.screens.map.components.DraggingInfoBanner
import com.mobileorienteering.ui.screens.map.components.LocationFab
import com.mobileorienteering.ui.screens.map.components.MapBottomSheetHandle
import com.mobileorienteering.ui.screens.map.components.RunFinishedDialog
import com.mobileorienteering.ui.screens.map.components.RunProgressPanel
import com.mobileorienteering.ui.screens.map.components.SaveRouteDialog
import com.mobileorienteering.ui.screens.map.components.layers.CheckpointsLayer
import com.mobileorienteering.ui.screens.map.components.layers.FogOfWarLayer
import com.mobileorienteering.ui.screens.map.components.layers.NextCheckpointLineLayer
import com.mobileorienteering.ui.screens.map.components.layers.RoutePathLayer
import com.mobileorienteering.ui.screens.map.components.layers.UserLocationLayer
import com.mobileorienteering.data.model.app.Checkpoint
import com.mobileorienteering.data.model.app.RunSettings
import com.mobileorienteering.data.model.domain.RaceStyle
import com.mobileorienteering.ui.core.Strings
import com.mobileorienteering.util.boundingCamera
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.*
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.rememberStyleState
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Position

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
    initialMapId: Long? = null,
    startRun: Boolean = false,
    runSettings: RunSettings = RunSettings(),
    onMapSaved: () -> Unit = {},
    onRunFinished: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val runState by viewModel.runState.collectAsStateWithLifecycle()
    val finishedRunState by viewModel.finishedRunState.collectAsStateWithLifecycle()
    val showLocationDuringRun by viewModel.showLocationDuringRun.collectAsStateWithLifecycle()
    val raceStyle by viewModel.raceStyle.collectAsStateWithLifecycle()
    val mapStyle by viewModel.mapStyle.collectAsStateWithLifecycle()
    val mapIconStyle by viewModel.mapIconStyle.collectAsStateWithLifecycle()
    val centerCameraOnce by viewModel.centerCameraOnce.collectAsStateWithLifecycle()
    val shouldMoveCamera by viewModel.shouldMoveCamera.collectAsStateWithLifecycle()

    val cameraState = rememberCameraState()

    val context = LocalContext.current

    val isRunActive = runState.isActive

    val shouldShowLocation = !isRunActive || showLocationDuringRun

    val styleState = rememberStyleState()
    val coroutineScope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )

    var tapPosition by remember { mutableStateOf<Position?>(null) }
    var hasAutoStarted by rememberSaveable { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showGpsSettingsDialog by remember { mutableStateOf(false) }
    var showLocationPermissionRationale by remember { mutableStateOf(false) }
    var showLocationPermissionSettings by remember { mutableStateOf(false) }
    var pendingRunStart by remember { mutableStateOf(false) }
    var draggingCheckpointIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(state.mapSaved) {
        if (state.mapSaved) {
            viewModel.onMapSavedHandled()
            onMapSaved()
        }
    }

    LaunchedEffect(state.runFinished) {
        if (state.runFinished) {
            viewModel.onRunFinishedHandled()
            onRunFinished()
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.updatePermissionState()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            viewModel.startRun()
            pendingRunStart = false
        } else {
            viewModel.startRun()
            pendingRunStart = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            if (pendingRunStart) {
                viewModel.handleStartRun(
                    onRequestLocationPermission = {},
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.startRun()
                            pendingRunStart = false
                        }
                    },
                    onLocationDisabled = {
                        showGpsSettingsDialog = true
                        pendingRunStart = false
                    },
                    onStartRun = {
                        viewModel.startRun()
                        pendingRunStart = false
                    }
                )
            } else {
                viewModel.handleLocationPermissionGranted(
                    onLocationEnabled = {
                        viewModel.startTracking()
                        viewModel.requestCenterCamera()
                    },
                    onLocationDisabled = {
                        showGpsSettingsDialog = true
                    }
                )
            }
        } else {
            val activity = context as? ComponentActivity
            val shouldShowRationale = activity?.shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) ?: true

            if (shouldShowRationale) {
                showLocationPermissionRationale = true
            } else {
                showLocationPermissionSettings = true
            }

            pendingRunStart = false
        }
    }

    LaunchedEffect(initialMapId) {
        if (initialMapId != null) {
            viewModel.loadMap(initialMapId)
        }
    }

    LaunchedEffect(startRun, state.checkpoints) {
        if (startRun && state.checkpoints.isNotEmpty() && !isRunActive && !hasAutoStarted) {
            hasAutoStarted = true
            viewModel.setRunOptions(runSettings)
            pendingRunStart = true
        }
    }

    LaunchedEffect(pendingRunStart) {
        if (pendingRunStart) {
            viewModel.handleStartRun(
                onRequestLocationPermission = {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                onRequestNotificationPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        // For Android 12 and below, just start the run
                        viewModel.startRun()
                        pendingRunStart = false
                    }
                },
                onLocationDisabled = {
                    showGpsSettingsDialog = true
                    pendingRunStart = false
                },
                onStartRun = {
                    viewModel.startRun()
                    pendingRunStart = false
                }
            )
        }
    }

    LaunchedEffect(runState.autoFinished) {
        if (runState.autoFinished && runState.isActive) {
            viewModel.stopRun()
        }
    }

    LaunchedEffect(isRunActive) {
        if (isRunActive) {
            scaffoldState.bottomSheetState.partialExpand()
            if (state.checkpoints.isNotEmpty()) {
                val cam = state.checkpoints.toBoundingCamera()
                coroutineScope.launch {
                    cameraState.animateTo(CameraPosition(target = Position(cam.centerLon, cam.centerLat), zoom = cam.zoom))
                }
            }
        } else if (state.checkpoints.isNotEmpty()) {
            scaffoldState.bottomSheetState.partialExpand()
        }
    }

    LaunchedEffect(shouldMoveCamera) {
        if (shouldMoveCamera && state.checkpoints.isNotEmpty()) {
            val cam = state.checkpoints.toBoundingCamera()
            coroutineScope.launch {
                cameraState.animateTo(CameraPosition(target = Position(cam.centerLon, cam.centerLat), zoom = cam.zoom))
                viewModel.cameraMoved()
            }
        }
    }

    LaunchedEffect(centerCameraOnce, state.currentLocation, runState.currentLocation) {
        if (centerCameraOnce) {
            val location = if (isRunActive) runState.currentLocation else state.currentLocation
            location ?: return@LaunchedEffect
            coroutineScope.launch {
                cameraState.animateTo(
                    CameraPosition(
                        target = Position(location.longitude, location.latitude),
                        zoom = if (cameraState.position.zoom < 16.0) 16.0 else cameraState.position.zoom
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
        sheetPeekHeight = if (isRunActive || state.checkpoints.isEmpty()) 0.dp else 64.dp,
        sheetDragHandle = { if (!isRunActive && state.checkpoints.isNotEmpty()) MapBottomSheetHandle() },
        sheetContent = {
            if (!isRunActive && state.checkpoints.isNotEmpty()) {
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
                baseStyle = BaseStyle.Uri(mapStyle.getUrl()),
                options = MapOptions(
                    ornamentOptions = OrnamentOptions.AllDisabled,
                    gestureOptions = GestureOptions.Standard
                ),
                onMapClick = { point, _ ->
                    if (draggingCheckpointIndex != null) {
                        viewModel.moveCheckpoint(draggingCheckpointIndex!!, point.longitude, point.latitude)
                        draggingCheckpointIndex = null
                    } else if (!isRunActive) {
                        tapPosition = point
                    }
                    ClickResult.Pass
                }
            ) {
                val currentLocation = if (isRunActive) runState.currentLocation else state.currentLocation

                if (isRunActive && runState.pathData.isNotEmpty() && shouldShowLocation) {
                    RoutePathLayer(
                        pathData = runState.pathData,
                        width = 4f
                    )

                    if (runState.orderedControlPoints) {
                        NextCheckpointLineLayer(
                            currentLocation = currentLocation,
                            nextCheckpoint = if (runState.nextCheckpointIndex < state.checkpoints.size) {
                                state.checkpoints[runState.nextCheckpointIndex]
                            } else null
                        )
                    }
                }

                if (isRunActive && raceStyle == RaceStyle.COMPASS_BEARING) {
                    FogOfWarLayer(checkpoints = state.checkpoints)
                }

                CheckpointsLayer(
                    checkpoints = state.checkpoints,
                    visitedIndices = if (isRunActive) runState.visitedCheckpointIndices else emptySet(),
                    nextCheckpointIndex = if (isRunActive && runState.orderedControlPoints) {
                        runState.nextCheckpointIndex
                    } else {
                        -1
                    },
                    isRunActive = isRunActive,
                    draggingIndex = draggingCheckpointIndex,
                    cameraState = cameraState,
                    iconStyle = mapIconStyle,
                    onCheckpointLongClick = { index ->
                        draggingCheckpointIndex = index
                    }
                )

                if (shouldShowLocation) {
                    UserLocationLayer(location = currentLocation)
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

            if (!isRunActive && state.checkpoints.isEmpty() && draggingCheckpointIndex == null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = Strings.Map.clickToAddFirst,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (draggingCheckpointIndex == null) {
                RunProgressPanel(
                    isVisible = isRunActive,
                    startTime = runState.startTime,
                    visitedCount = runState.visitedCheckpointIndices.size,
                    totalCount = runState.totalCheckpoints,
                    distance = runState.distance,
                    nextCheckpointIndex = runState.nextCheckpointIndex,
                    showNextCheckpoint = runState.orderedControlPoints,
                    onStopClick = { viewModel.stopRun() },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            if (!isRunActive && draggingCheckpointIndex == null) {
                val bottomSheetOffset = if (state.checkpoints.isEmpty()) 0.dp else 64.dp

                LocationFab(
                    isTracking = state.isTracking,
                    onClick = {
                        if (state.isTracking) {
                            viewModel.requestCenterCamera()
                        } else {
                            viewModel.handleLocationFabClick(
                                onRequestPermission = {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                onLocationEnabled = {
                                    viewModel.startTracking()
                                    viewModel.requestCenterCamera()
                                },
                                onLocationDisabled = {
                                    showGpsSettingsDialog = true
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp + bottomSheetOffset)
                )
            }

            if (isRunActive
                && shouldShowLocation
                && runState.currentLocation != null
                && draggingCheckpointIndex == null) {
                LocationFab(
                    isTracking = true,
                    onClick = { viewModel.requestCenterCamera() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                )
            }

            if (draggingCheckpointIndex == null && !isRunActive) {
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

            if (state.isSavingMap) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = stringResource(R.string.map_saving),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            if (showLocationPermissionRationale) {
                LocationPermissionRationaleDialog(
                    onDismiss = {
                        showLocationPermissionRationale = false
                        pendingRunStart = false
                    },
                    onGrantPermission = {
                        showLocationPermissionRationale = false
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
            }

            if (showLocationPermissionSettings) {
                LocationPermissionSettingsDialog(
                    context = context,
                    onDismiss = {
                        showLocationPermissionSettings = false
                        pendingRunStart = false
                    }
                )
            }

            if (showGpsSettingsDialog) {
                GpsSettingsDialog(
                    context = context,
                    onDismiss = {
                        showGpsSettingsDialog = false
                        pendingRunStart = false
                    }
                )
            }

            if (showSaveDialog) {
                SaveRouteDialog(
                    onDismiss = { showSaveDialog = false },
                    onSave = { name ->
                        viewModel.saveCurrentMap(name)
                        showSaveDialog = false
                    },
                    existingMapName = state.currentMapName,
                    onUpdate = if (state.currentMapId != null) {
                        { name ->
                            viewModel.updateCurrentMap(name)
                            showSaveDialog = false
                        }
                    } else null
                )
            }

            finishedRunState?.let { run ->
                RunFinishedDialog(
                    isCompleted = run.isCompleted,
                    duration = run.duration,
                    visitedCount = run.visitedControlPoints.size,
                    totalCount = run.totalCheckpoints,
                    distance = run.distance,
                    defaultTitle = Strings.Formatted.runTitleLabel(run.mapName),
                    onSave = { title -> viewModel.saveFinishedRun(title) },
                    onDiscard = { viewModel.discardFinishedRun() }
                )
            }
        }
    }
}

private fun List<Checkpoint>.toBoundingCamera() =
    boundingCamera(map { it.position.latitude to it.position.longitude })
