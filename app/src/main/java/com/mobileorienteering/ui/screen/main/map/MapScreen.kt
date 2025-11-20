package com.mobileorienteering.ui.screen.main.map

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobileorienteering.ui.component.LocationPermissionHandler
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.*
import org.maplibre.compose.expressions.value.*
import org.maplibre.compose.layers.*
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.sources.*
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.rememberStyleState
import org.maplibre.spatialk.geojson.*
import org.maplibre.compose.sources.GeoJsonData

@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val cameraState = rememberCameraState()
    val styleState = rememberStyleState()
    val coroutineScope = rememberCoroutineScope()

    // GeoJSON dla lokalizacji użytkownika
    val locationPoint = remember(state.currentLocation) {
        state.currentLocation?.let { location ->
            Point(
                Position(
                    longitude = location.longitude,
                    latitude = location.latitude
                )
            )
        }
    }

    LocationPermissionHandler(
        onPermissionGranted = {
            viewModel.updatePermissionState()
            viewModel.startTracking()
        },
        onPermissionDenied = {
            viewModel.updatePermissionState()
            viewModel.stopTracking()
        }
    ) { requestPermission ->
        Box(Modifier.fillMaxSize()) {
            MaplibreMap(
                modifier = Modifier.fillMaxSize(),
                cameraState = cameraState,
                styleState = styleState,
                baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
                options = MapOptions(
                    ornamentOptions = OrnamentOptions.AllDisabled,
                    gestureOptions = if (state.isTracking) {
                        GestureOptions.AllDisabled
                    } else {
                        GestureOptions.Standard
                    }
                )
            ) {
                // Źródło danych dla lokalizacji
                locationPoint?.let { point ->
                    val locationSource = rememberGeoJsonSource(
                        data = GeoJsonData.Features(point)
                    )

                    // Warstwa accuracy (większe przezroczyste koło)
                    state.currentLocation?.let { location ->
                        if (location.hasAccuracy() && location.accuracy > 0) {
                            CircleLayer(
                                id = "user-location-accuracy",
                                source = locationSource,
                                color = const(Color(0x302196F3)),  // Przezroczysty niebieski
                                radius = const((location.accuracy / 2).dp),  // Promień dokładności
                                strokeColor = const(Color(0xFF2196F3)),
                                strokeWidth = const(1.dp)
                            )
                        }
                    }

                    // Warstwa lokalizacji (niebieska kropka)
                    CircleLayer(
                        id = "user-location-dot",
                        source = locationSource,
                        color = const(Color(0xFF2196F3)),  // Niebieski
                        radius = const(8.dp),
                        strokeColor = const(Color.White),
                        strokeWidth = const(3.dp),
                        strokeOpacity = const(1f)
                    )

                    // Wewnętrzna biała kropka dla efektu
                    CircleLayer(
                        id = "user-location-inner",
                        source = locationSource,
                        color = const(Color.White),
                        radius = const(3.dp),
                        opacity = const(0.8f)
                    )
                }
            }

            // Panel informacji o lokalizacji
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
                            text = "Tracking aktywny",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        HorizontalDivider()

                        state.currentLocation?.let { location ->
                            Text(
                                text = "Szerokość: %.5f°".format(location.latitude),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Długość: %.5f°".format(location.longitude),
                                style = MaterialTheme.typography.bodySmall
                            )

                            if (location.hasAccuracy()) {
                                Text(
                                    text = "Dokładność: %.0f m".format(location.accuracy),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when {
                                        location.accuracy < 10 -> Color.Green
                                        location.accuracy < 30 -> Color.Yellow
                                        else -> Color.Red
                                    }
                                )
                            }

                            if (location.hasSpeed() && location.speed > 0) {
                                Text(
                                    text = "Prędkość: %.1f km/h".format(location.speed * 3.6),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        if (state.distanceTraveled > 0) {
                            HorizontalDivider()
                            Text(
                                text = "Dystans: %.2f km".format(state.distanceTraveled / 1000),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // Główny przycisk trackingu
            FloatingActionButton(
                onClick = {
                    if (!state.hasPermission) {
                        requestPermission()
                    } else if (state.isTracking) {
                        viewModel.stopTracking()
                    } else {
                        viewModel.startTracking()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = when {
                    !state.hasPermission -> MaterialTheme.colorScheme.errorContainer
                    state.isTracking -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer
                },
                contentColor = when {
                    !state.hasPermission -> MaterialTheme.colorScheme.onErrorContainer
                    state.isTracking -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                }
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = if (state.isTracking) {
                        "Zatrzymaj śledzenie"
                    } else {
                        "Rozpocznij śledzenie"
                    }
                )
            }

            // Informacja gdy brak GPS
            if (!state.hasPermission) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Brak uprawnień GPS\nKliknij przycisk poniżej",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Wyświetl błąd jeśli występuje
            state.error?.let { error ->
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
                    Text(text = error)
                }
            }
        }

        // Automatyczne centrowanie na lokalizacji użytkownika
        LaunchedEffect(state.currentLocation) {
            val location = state.currentLocation ?: return@LaunchedEffect

            if (state.isTracking) {
                coroutineScope.launch {
                    cameraState.animateTo(
                        CameraPosition(
                            target = Position(
                                longitude = location.longitude,
                                latitude = location.latitude
                            ),
                            zoom = if (cameraState.position.zoom < 14) 16.0 else cameraState.position.zoom
                        )
                    )
                }
            }
        }

        // Początkowa pozycja kamery
        LaunchedEffect(Unit) {
            cameraState.position = CameraPosition(
                target = Position(
                    longitude = 21.0122,
                    latitude = 52.2297
                ),
                zoom = 10.0
            )
        }
    }
}