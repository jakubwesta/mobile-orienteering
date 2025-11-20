package com.mobileorienteering.ui.screen.main.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobileorienteering.ui.component.LocationPermissionHandler
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
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {

    val state by viewModel.state.collectAsState()
    val cameraState = rememberCameraState()
    val styleState = rememberStyleState()
    val coroutineScope = rememberCoroutineScope()

    // dialog dodawania checkpointa
    var longPressPosition by remember { mutableStateOf<Position?>(null) }
    var showCheckpointDialog by remember { mutableStateOf(false) }

    // bottom sheet state
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
    ) { paddingValues ->

        Box(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // --- MAPA ---
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
                )
            ) {

                // warstwy checkpointów
                state.checkpoints.forEach { checkpoint ->
                    val source = rememberGeoJsonSource(
                        data = GeoJsonData.Features(
                            Point(Position(checkpoint.position.longitude, checkpoint.position.latitude))
                        )
                    )

                    CircleLayer(
                        id = "checkpoint-${checkpoint.id}",
                        source = source,
                        color = const(Color(0xFFFF5722)),
                        radius = const(12.dp),
                        strokeColor = const(Color.White),
                        strokeWidth = const(2.dp)
                    )

                    CircleLayer(
                        id = "checkpoint-inner-${checkpoint.id}",
                        source = source,
                        color = const(Color.White),
                        radius = const(4.dp)
                    )
                }

                // warstwa lokalizacji użytkownika
                state.currentLocation?.let { location ->
                    val point = Point(Position(location.longitude, location.latitude))
                    val locSource = rememberGeoJsonSource(
                        data = GeoJsonData.Features(point)
                    )

                    CircleLayer(
                        id = "user-location-dot",
                        source = locSource,
                        color = const(Color(0xFF2196F3)),
                        radius = const(8.dp),
                        strokeColor = const(Color.White),
                        strokeWidth = const(3.dp)
                    )
                }
            }

            // panel lokalizacji
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
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Tracking aktywny",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        HorizontalDivider()

                        state.currentLocation?.let { loc ->
                            Text("Szerokość: %.5f°".format(loc.latitude))
                            Text("Długość: %.5f°".format(loc.longitude))

                            if (loc.hasAccuracy())
                                Text("Dokładność: %.0f m".format(loc.accuracy))

                            if (loc.hasSpeed())
                                Text("Prędkość: %.1f km/h".format(loc.speed * 3.6))
                        }

                        if (state.distanceTraveled > 0) {
                            HorizontalDivider()
                            Text(
                                "Dystans: %.2f km".format(state.distanceTraveled / 1000),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // --- FAB TRACKING ---
            FloatingActionButton(
                onClick = {
                    if (state.isTracking) viewModel.stopTracking()
                    else viewModel.startTracking()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = if (state.isTracking)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.secondaryContainer,
                contentColor = if (state.isTracking)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = if (state.isTracking) "Zatrzymaj" else "Start"
                )
            }

            // --- FAB DODAWANIA CHECKPOINTA ---
            if (state.currentLocation != null) {
                FloatingActionButton(
                    onClick = {
                        longPressPosition = Position(
                            state.currentLocation!!.longitude,
                            state.currentLocation!!.latitude
                        )
                        showCheckpointDialog = true
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp, 88.dp),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Dodaj checkpoint"
                    )
                }
            }

            // dialog dodawania checkpointa
            if (showCheckpointDialog && longPressPosition != null) {
                var name by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = {
                        showCheckpointDialog = false
                        longPressPosition = null
                        name = ""
                    },
                    title = { Text("Dodaj checkpoint") },
                    text = {
                        Column {
                            Text("Wprowadź nazwę:")
                            Spacer(Modifier.height(8.dp))
                            TextField(
                                value = name,
                                onValueChange = { name = it },
                                singleLine = true
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            longPressPosition?.let { pos ->
                                viewModel.addCheckpoint(
                                    longitude = pos.longitude,
                                    latitude = pos.latitude,
                                    name = name
                                )
                            }
                            showCheckpointDialog = false
                            longPressPosition = null
                            name = ""
                        }) { Text("Dodaj") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showCheckpointDialog = false
                        }) { Text("Anuluj") }
                    }
                )
            }

            // snackbar błędu
            state.error?.let { err ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(err)
                }
            }
        }
    }

    // kamera śledzi użytkownika
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
            Text(
                text = "Punkty kontrolne",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (state.checkpoints.isNotEmpty()) {
                IconButton(onClick = { viewModel.clearCheckpoints() }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        tint = MaterialTheme.colorScheme.error,
                        contentDescription = "Usuń wszystkie"
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (state.checkpoints.isEmpty()) {
            Text(
                text = "Brak zapisanych checkpointów.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            state.checkpoints.forEach { checkpoint ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        checkpoint.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.removeCheckpoint(checkpoint.id) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            tint = MaterialTheme.colorScheme.error,
                            contentDescription = "Usuń"
                        )
                    }
                }
            }
        }
    }
}
