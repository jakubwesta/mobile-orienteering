package com.mobileorienteering.ui.screen.main

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobileorienteering.ui.component.LocationPermissionHandler
import com.mobileorienteering.ui.component.CheckpointsBottomSheet
import com.mobileorienteering.ui.screen.main.map.MapViewModel
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap

@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var mapView: MapView? by remember { mutableStateOf(null) }
    var mapLibreMap: MapLibreMap? by remember { mutableStateOf(null) }
    var isMapReady by remember { mutableStateOf(false) }
    var locationComponentActive by remember { mutableStateOf(false) }

    // Camera animations
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

    // Location component
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

    // Draw current route
    LaunchedEffect(state.currentRoutePoints.size, locationComponentActive, isMapReady, state.isShowingRoute) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (!isMapReady || !locationComponentActive) return@LaunchedEffect

        map.style?.let { style ->
            if (state.isShowingRoute && state.currentRoutePoints.size >= 2) {
                drawRoute(style, state.currentRoutePoints, "current-route", android.graphics.Color.BLUE)
            } else {
                try {
                    style.removeLayer("current-route-layer")
                    style.removeSource("current-route-source")
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    // Draw saved runs
    LaunchedEffect(state.savedRuns, isMapReady) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (!isMapReady) return@LaunchedEffect

        map.style?.let { style ->
            state.savedRuns.forEachIndexed { index, run ->
                drawRoute(style, run.routePoints, "saved-route-$index", android.graphics.Color.BLUE)
            }
        }
    }

    // Draw checkpoints
    LaunchedEffect(state.checkpoints, isMapReady) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (!isMapReady) return@LaunchedEffect

        map.style?.let { style ->
            drawCheckpoints(style, state.checkpoints)
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
            // Map
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        mapView = this
                        getMapAsync { map ->
                            mapLibreMap = map

                            // Configure gestures
                            map.uiSettings.isZoomGesturesEnabled = true
                            map.uiSettings.isScrollGesturesEnabled = true
                            map.uiSettings.isDoubleTapGesturesEnabled = true
                            map.uiSettings.isRotateGesturesEnabled = false
                            map.uiSettings.isTiltGesturesEnabled = false
                            map.uiSettings.isQuickZoomGesturesEnabled = false

                            // Long click adds checkpoint
                            map.addOnMapLongClickListener { latLng ->
                                viewModel.addCheckpointAtLocation(latLng)
                                true
                            }

                            // Initial position
                            map.cameraPosition = CameraPosition.Builder()
                                .target(LatLng(51.1079, 17.0385))
                                .zoom(10.0)
                                .build()

                            // Load style
                            map.setStyle("https://tiles.openfreemap.org/styles/positron") { style ->
                                isMapReady = true
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view -> view.onResume() }
            )

            // Route toggle button
            RouteToggleButton(
                isTracking = state.isTracking,
                isShowingRoute = state.isShowingRoute,
                onClick = { viewModel.toggleRouteDrawing() },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 80.dp)
            )

            // Add checkpoint button
            AddCheckpointButton(
                onClick = { viewModel.addCheckpointAtCurrentLocation() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 80.dp)
            )

            // Location tracking button
            LocationTrackingButton(
                hasPermission = state.hasPermission,
                isTracking = state.isTracking,
                onRequestPermission = requestPermission,
                onStartTracking = { viewModel.startTracking() },
                onStopTracking = { viewModel.stopTracking() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 170.dp)
            )

            // Bottom Sheet
            CheckpointsBottomSheet(
                checkpoints = state.checkpoints,
                isExpanded = state.isBottomSheetExpanded,
                onToggle = { viewModel.toggleBottomSheet() },
                onCheckpointClick = { checkpoint ->
                    // TODO: Center map on checkpoint
                },
                onCheckpointRemove = { checkpointId ->
                    viewModel.removeCheckpoint(checkpointId)
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
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