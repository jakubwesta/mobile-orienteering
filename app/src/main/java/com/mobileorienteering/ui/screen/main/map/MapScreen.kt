package com.mobileorienteering.ui.screen.main.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.rememberStyleState
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {

    val state by viewModel.state.collectAsState()
    val cameraState = rememberCameraState()
    val styleState = rememberStyleState()
    val coroutineScope = rememberCoroutineScope()

    var tapPosition by remember { mutableStateOf<Position?>(null) }
    var showCheckpointDialog by remember { mutableStateOf(false) }

    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 64.dp,
        sheetDragHandle = {
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
        },
        sheetContent = {
            CheckpointBottomSheetContent(
                state = state,
                viewModel = viewModel
            )
        }
    ) {

        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {

            // MAPA
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

                // --- CHECKPOINTY BEZ NUMERÓW ---
                state.checkpoints.forEach { checkpoint ->
                    val source = rememberGeoJsonSource(
                        data = GeoJsonData.Features(
                            Point(Position(checkpoint.position.longitude, checkpoint.position.latitude))
                        )
                    )

                    // outer circle
                    CircleLayer(
                        id = "checkpoint-${checkpoint.id}",
                        source = source,
                        color = const(Color(0xFFFF5722)),
                        radius = const(12.dp),
                        strokeColor = const(Color.White),
                        strokeWidth = const(2.dp)
                    )

                    // inner circle
                    CircleLayer(
                        id = "checkpoint-inner-${checkpoint.id}",
                        source = source,
                        color = const(Color.White),
                        radius = const(4.dp)
                    )
                }

                // pozycja użytkownika
                state.currentLocation?.let { location ->
                    val locSource = rememberGeoJsonSource(
                        data = GeoJsonData.Features(
                            Point(Position(location.longitude, location.latitude))
                        )
                    )

                    CircleLayer(
                        id = "user-location",
                        source = locSource,
                        color = const(Color(0xFF2196F3)),
                        radius = const(8.dp),
                        strokeColor = const(Color.White),
                        strokeWidth = const(3.dp)
                    )
                }
            }

            // PANEL LOKALIZACJI
            if (state.isTracking && state.currentLocation != null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .widthIn(max = 200.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text("Tracking aktywny", color = MaterialTheme.colorScheme.primary)
                        HorizontalDivider()
                        state.currentLocation?.let { loc ->
                            Text("Lat: %.5f".format(loc.latitude))
                            Text("Lon: %.5f".format(loc.longitude))
                        }
                    }
                }
            }

            // FAB tracking
            FloatingActionButton(
                onClick = {
                    if (state.isTracking) viewModel.stopTracking()
                    else viewModel.startTracking()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp + 64.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
            }

            // FAB dodania checkpointa z lokalizacji
            if (state.currentLocation != null) {
                FloatingActionButton(
                    onClick = {
                        tapPosition = Position(
                            state.currentLocation!!.longitude,
                            state.currentLocation!!.latitude
                        )
                        showCheckpointDialog = true
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 88.dp + 64.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }

            // DIALOG DODAWANIA CHECKPOINTA
            if (showCheckpointDialog && tapPosition != null) {
                var name by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = {
                        showCheckpointDialog = false
                        tapPosition = null
                        name = ""
                    },
                    title = { Text("Dodaj checkpoint") },
                    text = {
                        Column {
                            TextField(value = name, onValueChange = { name = it }, singleLine = true)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            tapPosition?.let { pos ->
                                viewModel.addCheckpoint(
                                    longitude = pos.longitude,
                                    latitude = pos.latitude,
                                    name = name
                                )
                            }
                            showCheckpointDialog = false
                            tapPosition = null
                        }) {
                            Text("Dodaj")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showCheckpointDialog = false
                        }) {
                            Text("Anuluj")
                        }
                    }
                )
            }
        }
    }

    // Kamera śledzi użytkownika
    LaunchedEffect(state.currentLocation) {
        val loc = state.currentLocation ?: return@LaunchedEffect
        if (state.isTracking) {
            coroutineScope.launch {
                cameraState.animateTo(
                    CameraPosition(
                        target = Position(loc.longitude, loc.latitude),
                        zoom = if (cameraState.position.zoom < 14) 16.0 else cameraState.position.zoom
                    )
                )
            }
        }
    }

    // pozycja startowa kamery
    LaunchedEffect(Unit) {
        cameraState.position = CameraPosition(
            target = Position(21.0122, 52.2297),
            zoom = 10.0
        )
    }
}

@Composable
fun CheckpointBottomSheetContent(
    state: MapState,
    viewModel: MapViewModel
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Punkty kontrolne", color = MaterialTheme.colorScheme.primary)

            if (state.checkpoints.isNotEmpty()) {
                IconButton(onClick = { viewModel.clearCheckpoints() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Usuń wszystkie")
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        state.checkpoints.forEachIndexed { index, checkpoint ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${index + 1}. ${checkpoint.name}", modifier = Modifier.weight(1f))

                IconButton(
                    onClick = { viewModel.moveCheckpointUp(checkpoint.id) },
                    enabled = index > 0
                ) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = null) }

                IconButton(
                    onClick = { viewModel.moveCheckpointDown(checkpoint.id) },
                    enabled = index < state.checkpoints.size - 1
                ) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) }

                IconButton(onClick = { viewModel.removeCheckpoint(checkpoint.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            }
        }
    }
}
