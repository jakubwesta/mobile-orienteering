package com.mobileorienteering.ui.screens.runs.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mobileorienteering.data.model.domain.PathPoint
import com.mobileorienteering.data.model.domain.Checkpoint
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.rememberStyleState
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position

@Composable
fun RunMapPreview(
    pathData: List<PathPoint>,
    checkpoints: List<Checkpoint>,
    visitedIndices: Set<Int>,
    modifier: Modifier = Modifier
) {
    val cameraState = rememberCameraState()
    val styleState = rememberStyleState()

    val initialCameraPosition = remember(pathData) {
        if (pathData.isNotEmpty()) {
            val minLat = pathData.minOf { it.latitude }
            val maxLat = pathData.maxOf { it.latitude }
            val minLon = pathData.minOf { it.longitude }
            val maxLon = pathData.maxOf { it.longitude }

            val centerLat = (minLat + maxLat) / 2
            val centerLon = (minLon + maxLon) / 2

            val latDiff = maxLat - minLat
            val lonDiff = maxLon - minLon
            val maxDiff = maxOf(latDiff, lonDiff)

            val zoom = when {
                maxDiff > 0.1 -> 11.0
                maxDiff > 0.05 -> 12.0
                maxDiff > 0.02 -> 13.0
                maxDiff > 0.01 -> 14.0
                maxDiff > 0.005 -> 15.0
                else -> 16.0
            }

            CameraPosition(
                target = Position(centerLon, centerLat),
                zoom = zoom
            )
        } else null
    }

    LaunchedEffect(initialCameraPosition) {
        initialCameraPosition?.let {
            cameraState.position = it
        }
    }

    val routeFeature = remember(pathData) {
        if (pathData.size >= 2) {
            val positions = pathData.map { Position(it.longitude, it.latitude) }
            Feature(geometry = LineString(positions), properties = null)
        } else null
    }

    val checkpointFeatures = remember(checkpoints) {
        checkpoints.map { checkpoint ->
            Feature(
                geometry = Point(Position(checkpoint.position.longitude, checkpoint.position.latitude)),
                properties = null
            )
        }
    }

    val startFeature = remember(pathData) {
        if (pathData.isNotEmpty()) {
            val startPoint = pathData.first()
            Feature(
                geometry = Point(Position(startPoint.longitude, startPoint.latitude)),
                properties = null
            )
        } else null
    }

    val endFeature = remember(pathData) {
        if (pathData.size > 1) {
            val endPoint = pathData.last()
            Feature(
                geometry = Point(Position(endPoint.longitude, endPoint.latitude)),
                properties = null
            )
        } else null
    }

    MaplibreMap(
        modifier = modifier,
        cameraState = cameraState,
        styleState = styleState,
        baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
        options = MapOptions(
            ornamentOptions = OrnamentOptions.AllDisabled,
            gestureOptions = GestureOptions.Standard
        )
    ) {
        // Route path
        routeFeature?.let { feature ->
            val routeSource = rememberGeoJsonSource(
                data = GeoJsonData.Features(feature)
            )

            LineLayer(
                id = "preview-route-outline",
                source = routeSource,
                color = const(Color.White),
                width = const(6.dp)
            )

            LineLayer(
                id = "preview-route",
                source = routeSource,
                color = const(Color(0xFF4CAF50)),
                width = const(4.dp)
            )
        }

        // Checkpoints
        checkpointFeatures.forEachIndexed { index, feature ->
            key(index) {
                val isVisited = index in visitedIndices
                val source = rememberGeoJsonSource(data = GeoJsonData.Features(feature))

                CircleLayer(
                    id = "preview-checkpoint-$index",
                    source = source,
                    color = const(if (isVisited) Color(0xFF4CAF50) else Color(0xFF9E9E9E)),
                    radius = const(10.dp),
                    strokeColor = const(Color.White),
                    strokeWidth = const(2.dp)
                )
            }
        }

        // Start point
        startFeature?.let { feature ->
            val startSource = rememberGeoJsonSource(data = GeoJsonData.Features(feature))

            CircleLayer(
                id = "preview-start-point",
                source = startSource,
                color = const(Color(0xFF2196F3)),
                radius = const(8.dp),
                strokeColor = const(Color.White),
                strokeWidth = const(3.dp)
            )
        }

        // End point
        endFeature?.let { feature ->
            val endSource = rememberGeoJsonSource(data = GeoJsonData.Features(feature))

            CircleLayer(
                id = "preview-end-point",
                source = endSource,
                color = const(Color(0xFFF44336)),
                radius = const(8.dp),
                strokeColor = const(Color.White),
                strokeWidth = const(3.dp)
            )
        }
    }
}
