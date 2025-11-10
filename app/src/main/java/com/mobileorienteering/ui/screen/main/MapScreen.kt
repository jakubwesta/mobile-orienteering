package com.mobileorienteering.ui.screen.main

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobileorienteering.R
import com.mobileorienteering.ui.component.LocationPermissionHandler
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var mapView: MapView? by remember { mutableStateOf(null) }
    var mapLibreMap: MapLibreMap? by remember { mutableStateOf(null) }
    var isMapReady by remember { mutableStateOf(false) }
    var locationComponentActive by remember { mutableStateOf(false) }

    LaunchedEffect(state.currentLocation, isMapReady) {
        val location = state.currentLocation ?: return@LaunchedEffect
        val map = mapLibreMap ?: return@LaunchedEffect
        if (!isMapReady) return@LaunchedEffect

        map.animateCamera(
            org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(LatLng(location.latitude, location.longitude))
                    .zoom(18.0)
                    .build()
            ),
            1000
        )
    }

    LaunchedEffect(state.isTracking, state.hasPermission, isMapReady) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (!isMapReady) return@LaunchedEffect

        if (state.isTracking && state.hasPermission) {
            map.style?.let { style ->
                enableLocationComponent(context, map, style)
                locationComponentActive = true
            }
        } else {
            disableLocationComponent(map)
            locationComponentActive = false
        }
    }

    // Rysuj aktualną trasę gdy jest widoczna
    LaunchedEffect(state.currentRoutePoints.size, locationComponentActive, isMapReady, state.isShowingRoute) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (!isMapReady || !locationComponentActive) return@LaunchedEffect

        map.style?.let { style ->
            if (state.isShowingRoute && state.currentRoutePoints.size >= 2) {
                drawRoute(style, state.currentRoutePoints, "current-route", Color.BLUE)
            } else {
                // Usuń warstwę aktualnej trasy jeśli rysowanie wyłączone
                try {
                    style.removeLayer("current-route-layer")
                    style.removeSource("current-route-source")
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    // Rysuj zapisane biegi
    LaunchedEffect(state.savedRuns, isMapReady) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (!isMapReady) return@LaunchedEffect

        map.style?.let { style ->
            state.savedRuns.forEachIndexed { index, run ->
                drawRoute(style, run.routePoints, "saved-route-$index", Color.BLUE)
            }
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
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        mapView = this
                        getMapAsync { map ->
                            mapLibreMap = map

                            // Włącz tylko podstawowe gestures
                            map.uiSettings.isZoomGesturesEnabled = true          // Pinch-to-zoom (2 palce)
                            map.uiSettings.isScrollGesturesEnabled = true        // Przesuwanie (1 palec)
                            map.uiSettings.isDoubleTapGesturesEnabled = true     // Double tap - zoom in
                            map.uiSettings.isRotateGesturesEnabled = false       // Wyłącz obracanie
                            map.uiSettings.isTiltGesturesEnabled = false         // Wyłącz nachylanie
                            map.uiSettings.isQuickZoomGesturesEnabled = false    // Wyłącz quick zoom

                            map.cameraPosition = CameraPosition.Builder()
                                .target(LatLng(51.1079, 17.0385))
                                .zoom(10.0)
                                .build()

                            map.setStyle("https://tiles.openfreemap.org/styles/positron") { style ->
                                isMapReady = true
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    view.onResume()
                }
            )

            // Przycisk toggle widoczności trasy
            if (state.isTracking) {
                FloatingActionButton(
                    onClick = { viewModel.toggleRouteDrawing() },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    containerColor = if (state.isShowingRoute)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_route),
                        contentDescription = if (state.isShowingRoute) "Ukryj trasę" else "Pokaż trasę",
                        tint = if (state.isShowingRoute)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Przycisk start/stop tracking
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
                containerColor = if (state.isTracking)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_location),
                    contentDescription = if (state.isTracking) "Zatrzymaj" else "Start",
                    tint = if (state.isTracking)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                mapView?.onPause()
                mapView?.onStop()
                mapView?.onDestroy()
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun enableLocationComponent(context: Context, map: MapLibreMap, style: Style) {
    try {
        val locationComponentOptions = LocationComponentOptions.builder(context)
            .accuracyAlpha(0.15f)
            .accuracyColor(Color.BLUE)
            .foregroundTintColor(Color.BLUE)
            .pulseEnabled(true)
            .build()

        val locationComponentActivationOptions = LocationComponentActivationOptions.builder(context, style)
            .locationComponentOptions(locationComponentOptions)
            .useDefaultLocationEngine(true)
            .build()

        map.locationComponent.apply {
            if (!isLocationComponentActivated) {
                activateLocationComponent(locationComponentActivationOptions)
            }
            isLocationComponentEnabled = true
            cameraMode = CameraMode.TRACKING
            renderMode = RenderMode.COMPASS
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@SuppressLint("MissingPermission")
private fun disableLocationComponent(map: MapLibreMap) {
    try {
        map.locationComponent.isLocationComponentEnabled = false
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun drawRoute(style: Style, points: List<LatLng>, routeId: String, color: Int) {
    try {
        Log.d("MapScreen", "Drawing route $routeId with ${points.size} points")

        val coordinates = points.map {
            doubleArrayOf(it.longitude, it.latitude)
        }.toTypedArray()

        val lineGeoJson = """
            {
                "type": "Feature",
                "geometry": {
                    "type": "LineString",
                    "coordinates": ${coordinates.joinToString(",", "[", "]") { "[${it[0]},${it[1]}]" }}
                }
            }
        """.trimIndent()

        val sourceId = "$routeId-source"
        val layerId = "$routeId-layer"

        val source = style.getSource(sourceId)

        if (source == null) {
            Log.d("MapScreen", "Creating route source and layer: $routeId")

            style.addSource(
                org.maplibre.android.style.sources.GeoJsonSource(
                    sourceId,
                    lineGeoJson
                )
            )

            val lineLayer = org.maplibre.android.style.layers.LineLayer(layerId, sourceId)
                .withProperties(
                    org.maplibre.android.style.layers.PropertyFactory.lineColor(color),
                    org.maplibre.android.style.layers.PropertyFactory.lineWidth(5f),
                    org.maplibre.android.style.layers.PropertyFactory.lineOpacity(0.8f)
                )

            style.addLayer(lineLayer)
            Log.d("MapScreen", "Route layer added: $routeId")
        } else {
            (source as? org.maplibre.android.style.sources.GeoJsonSource)?.setGeoJson(lineGeoJson)
        }
    } catch (e: Exception) {
        Log.e("MapScreen", "Error drawing route", e)
    }
}