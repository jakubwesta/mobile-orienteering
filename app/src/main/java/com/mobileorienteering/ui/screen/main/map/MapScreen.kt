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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobileorienteering.ui.screen.main.map.components.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.map.*
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.rememberStyleState
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val cameraState = rememberCameraState()
    val styleState = rememberStyleState()
    val coroutineScope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()

    var tapPosition by remember { mutableStateOf<Position?>(null) }
    var showCheckpointDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    val shouldMoveCamera by viewModel.shouldMoveCamera.collectAsState()

    // Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        android.util.Log.d("MapScreen", "Permission result: fine=$fineLocationGranted, coarse=$coarseLocationGranted")

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
        sheetPeekHeight = 64.dp,
        sheetDragHandle = { MapBottomSheetHandle() },
        sheetContent = {
            CheckpointBottomSheetContent(
                state = state,
                viewModel = viewModel,
                onSaveRoute = { showSaveDialog = true }
            )
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
                    gestureOptions = if (state.isTracking)
                        GestureOptions.AllDisabled
                    else
                        GestureOptions.Standard
                ),
                onMapClick = { point, _ ->
                    tapPosition = point
                    showCheckpointDialog = true
                    ClickResult.Pass
                }
            ) {
                RenderCheckpoints(state.checkpoints)
                RenderUserLocation(state.currentLocation)
            }

            LocationInfoCard(
                state = state,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )

            FloatingActionButton(
                onClick = {
                    android.util.Log.d("MapScreen", "Location button clicked, isTracking=${state.isTracking}")
                    if (state.isTracking) {
                        viewModel.stopTracking()
                    } else {
                        // Sprawdź uprawnienia i poproś jeśli brak
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
                    contentDescription = if (state.isTracking) "Zatrzymaj tracking" else "Rozpocznij tracking"
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
                    Icon(Icons.Default.Add, contentDescription = "Dodaj checkpoint w obecnej lokalizacji")
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
        }
    }

    LaunchedEffect(state.currentLocation) {
        val location = state.currentLocation ?: return@LaunchedEffect
        if (state.isTracking) {
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
private fun RenderCheckpoints(checkpoints: List<com.mobileorienteering.ui.screen.main.map.models.Checkpoint>) {
    if (checkpoints.isEmpty()) return

    checkpoints.forEachIndexed { index, checkpoint ->
        val source = rememberGeoJsonSource(
            data = GeoJsonData.Features(
                Point(Position(checkpoint.position.longitude, checkpoint.position.latitude))
            )
        )

        // Kółko - checkpoint marker
        CircleLayer(
            id = "checkpoint-circle-${index}",
            source = source,
            color = const(Color(0xFFFF5722)),
            radius = const(14.dp),
            strokeColor = const(Color.White),
            strokeWidth = const(2.dp)
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