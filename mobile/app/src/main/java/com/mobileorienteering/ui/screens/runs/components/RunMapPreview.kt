package com.mobileorienteering.ui.screens.runs.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.mobileorienteering.data.model.app.Checkpoint
import com.mobileorienteering.data.model.app.MapIconStyle
import com.mobileorienteering.data.model.app.MapStyle
import com.mobileorienteering.data.model.domain.PathPoint
import com.mobileorienteering.ui.screens.map.components.layers.CheckpointsLayer
import com.mobileorienteering.ui.screens.map.components.layers.RoutePathLayer
import com.mobileorienteering.ui.theme.CheckpointColorNext
import com.mobileorienteering.ui.theme.ModernMarkerLabelColor
import com.mobileorienteering.ui.theme.ModernMarkerStrokeColor
import com.mobileorienteering.ui.theme.UserLocationStrokeColor
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.format
import org.maplibre.compose.expressions.dsl.offset
import org.maplibre.compose.expressions.dsl.span
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.rememberStyleState
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position

@Composable
fun RunMapPreview(
    pathData: List<PathPoint>,
    checkpoints: List<Checkpoint>,
    visitedIndices: Set<Int>,
    mapStyle: MapStyle,
    mapIconStyle: MapIconStyle,
    playbackPosition: Position? = null,
    playbackSpeedLabel: String? = null,
    modifier: Modifier = Modifier
) {
    val cameraState = rememberCameraState()
    val styleState = rememberStyleState()

    val initialCameraPosition = remember(pathData) {
        if (pathData.isNotEmpty()) {
            val minLat = pathData.minOf { it.lat }
            val maxLat = pathData.maxOf { it.lat }
            val minLon = pathData.minOf { it.lon }
            val maxLon = pathData.maxOf { it.lon }

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

    MaplibreMap(
        modifier = modifier,
        cameraState = cameraState,
        styleState = styleState,
        baseStyle = BaseStyle.Uri(mapStyle.getUrl()),
        options = MapOptions(
            ornamentOptions = OrnamentOptions.AllDisabled,
            gestureOptions = GestureOptions.Standard
        )
    ) {
        RoutePathLayer(pathData = pathData)

        CheckpointsLayer(
            checkpoints = checkpoints,
            visitedIndices = visitedIndices,
            nextCheckpointIndex = -1,
            isRunActive = true,
            draggingIndex = null,
            cameraState = cameraState,
            iconStyle = mapIconStyle,
            onCheckpointLongClick = {}
        )

        playbackPosition?.let { position ->
            PlaybackMarkerLayer(
                position = position,
                speedLabel = playbackSpeedLabel
            )
        }
    }
}

@Composable
private fun PlaybackMarkerLayer(
    position: Position,
    speedLabel: String?,
) {
    val playbackSource = rememberGeoJsonSource(
        data = GeoJsonData.Features(
            Feature(geometry = Point(position), properties = null)
        )
    )

    CircleLayer(
        id = "preview-playback-point",
        source = playbackSource,
        color = const(CheckpointColorNext),
        radius = const(12.dp),
        strokeColor = const(UserLocationStrokeColor),
        strokeWidth = const(3.dp)
    )

    speedLabel?.let { label ->
        SymbolLayer(
            id = "preview-playback-speed",
            source = playbackSource,
            textField = format(span(const(label))),
            textSize = const(14.sp),
            textColor = const(ModernMarkerLabelColor),
            textHaloColor = const(ModernMarkerStrokeColor),
            textHaloWidth = const(1.5.dp),
            textAnchor = const(SymbolAnchor.Top),
            textOffset = offset(0f.em, 1.5f.em),
            textAllowOverlap = const(true),
            textIgnorePlacement = const(true)
        )
    }
}
