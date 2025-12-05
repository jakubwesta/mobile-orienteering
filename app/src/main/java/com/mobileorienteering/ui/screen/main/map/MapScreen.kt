package com.mobileorienteering.ui.screen.main.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobileorienteering.ui.screen.main.map.components.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.*
import org.maplibre.spatialk.geojson.Feature
import kotlinx.serialization.json.JsonPrimitive
import androidx.compose.ui.unit.sp
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.rememberStyleState
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.em
import com.mobileorienteering.ui.component.RunFinishedDialog
import com.mobileorienteering.ui.component.RunProgressPanel
import org.maplibre.compose.expressions.dsl.format
import org.maplibre.compose.expressions.dsl.span
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
    initialMapId: Long? = null,
    startRun: Boolean = false
) {
    val state by viewModel.state.collectAsState()
    val finishedRunState by viewModel.finishedRunState.collectAsState()
    val cameraState = rememberCameraState()
    val styleState = rememberStyleState()
    val coroutineScope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()

    var tapPosition by remember { mutableStateOf<Position?>(null) }
    var showCheckpointDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    val shouldMoveCamera by viewModel.shouldMoveCamera.collectAsState()

    // Wczytaj mapę jeśli przekazano initialMapId
    LaunchedEffect(initialMapId) {
        if (initialMapId != null) {
            viewModel.loadMap(initialMapId)
        }
    }

    // Auto-start biegu gdy startRun=true - tylko raz przy wejściu na ekran
    var hasAutoStarted by remember { mutableStateOf(false) }
    LaunchedEffect(startRun, state.checkpoints) {
        if (startRun && state.checkpoints.isNotEmpty() && !state.isRunActive && !hasAutoStarted) {
            hasAutoStarted = true
            viewModel.startRun()
        }
    }

    // Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            viewModel.startTracking()
        }
    }

    // Przesunięcie kamery tylko przy wczytaniu trasy
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
                    gestureOptions = if (state.isTracking || state.isRunActive)
                        GestureOptions.AllDisabled
                    else
                        GestureOptions.Standard
                ),
                onMapClick = { point, _ ->
                    if (!state.isRunActive) {
                        tapPosition = point
                        showCheckpointDialog = true
                    }
                    ClickResult.Pass
                }
            ) {
                RenderCheckpoints(
                    checkpoints = state.checkpoints,
                    visitedIndices = state.visitedCheckpointIndices,
                    nextCheckpointIndex = state.nextCheckpointIndex,
                    isRunActive = state.isRunActive
                )
                RenderUserLocation(state.currentLocation)
            }

            // Panel postępu biegu na górze
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

            // Ukryj przyciski podczas biegu
            if (!state.isRunActive) {
                FloatingActionButton(
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
                        .padding(end = 16.dp, bottom = 16.dp + 64.dp),
                    containerColor = if (state.isTracking)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = if (state.isTracking) "Stop tracking" else "Start tracking"
                    )
                }

                if (state.currentLocation != null) {
                    FloatingActionButton(
                        onClick = {
                            state.currentLocation?.let { location ->
                                tapPosition = Position(location.longitude, location.latitude)
                                showCheckpointDialog = true
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 88.dp + 64.dp),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add control point at current location")
                    }
                }
            }

            CheckpointDialog(
                position = tapPosition,
                onDismiss = {
                    showCheckpointDialog = false
                    tapPosition = null
                },
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

            if (showSaveDialog) {
                SaveRouteDialog(
                    onDismiss = { showSaveDialog = false },
                    onSave = { name ->
                        viewModel.saveCurrentMap(name)
                    }
                )
            }

            // Dialog zakończenia biegu
            finishedRunState?.let { run ->
                RunFinishedDialog(
                    isCompleted = run.isCompleted,
                    duration = run.duration,
                    visitedCount = run.visitedControlPoints.size,
                    totalCount = run.totalCheckpoints,
                    distance = run.distance,
                    onSave = { viewModel.saveFinishedRun() },
                    onDiscard = { viewModel.discardFinishedRun() }
                )
            }
        }
    }

    LaunchedEffect(state.currentLocation) {
        val location = state.currentLocation ?: return@LaunchedEffect
        if (state.isTracking || state.isRunActive) {
            coroutineScope.launch {
                cameraState.animateTo(
                    CameraPosition(
                        target = Position(location.longitude, location.latitude),
                        zoom = if (cameraState.position.zoom < 14) 16.0 else cameraState.position.zoom
                    )
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        cameraState.position = CameraPosition(
            target = Position(21.0122, 52.2297),
            zoom = 10.0
        )
    }
}

@Composable
private fun MapBottomSheetHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .width(40.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        )
    }
}

@Composable
private fun RenderCheckpoints(
    checkpoints: List<com.mobileorienteering.ui.screen.main.map.models.Checkpoint>,
    visitedIndices: Set<Int>,
    nextCheckpointIndex: Int,
    isRunActive: Boolean
) {
    if (checkpoints.isEmpty()) return

    checkpoints.forEachIndexed { index, checkpoint ->
        val status = when {
            !isRunActive -> "inactive"
            index in visitedIndices -> "visited"
            index == nextCheckpointIndex -> "next"
            else -> "pending"
        }

        // Kolor kółka zależny od statusu
        val circleColor = when (status) {
            "visited" -> Color(0xFF4CAF50)    // Zielony - zaliczony
            "next" -> Color(0xFF2196F3)        // Niebieski - następny
            "pending" -> Color(0xFF9E9E9E)     // Szary - nieosiągalny
            else -> Color(0xFFFF5722)          // Pomarańczowy - inactive (bieg nie aktywny)
        }

        // Rozmiar kółka - większy dla następnego punktu
        val circleRadius = if (status == "next") 14.dp else 12.dp

        val feature = Feature(
            geometry = Point(Position(checkpoint.position.longitude, checkpoint.position.latitude)),
            properties = null
        )

        val source = rememberGeoJsonSource(
            data = GeoJsonData.Features(feature)
        )

        // Kółko
        CircleLayer(
            id = "control-point-circle-$index",
            source = source,
            color = const(circleColor),
            radius = const(circleRadius),
            strokeColor = const(Color.White),
            strokeWidth = const(2.dp)
        )

        // Numer na wierzchu
        SymbolLayer(
            id = "control-point-label-$index",
            source = source,
            textField = format(span(const("${index + 1}"))),
            textSize = const(12.sp),
            textColor = const(Color.White),
            textFont = const(listOf("Noto Sans Regular")),  // font ze stylu Liberty
            textAllowOverlap = const(true),
            textIgnorePlacement = const(true)
        )
    }
}

@Composable
private fun RenderUserLocation(location: android.location.Location?) {
    location?.let { loc ->
        val source = rememberGeoJsonSource(
            data = GeoJsonData.Features(
                Point(Position(loc.longitude, loc.latitude))
            )
        )

        CircleLayer(
            id = "user-location",
            source = source,
            color = const(Color(0xFF2196F3)),
            radius = const(8.dp),
            strokeColor = const(Color.White),
            strokeWidth = const(3.dp)
        )
    }
}