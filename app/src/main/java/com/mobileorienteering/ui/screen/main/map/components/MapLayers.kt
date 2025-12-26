package com.mobileorienteering.ui.screen.main.map.components

import android.location.Location
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobileorienteering.data.model.domain.PathPoint
import com.mobileorienteering.data.model.domain.Checkpoint
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.format
import org.maplibre.compose.expressions.dsl.span
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position

enum class CheckpointStatus {
    INACTIVE,
    VISITED,
    NEXT,
    PENDING
}

@Composable
fun CheckpointsLayer(
    checkpoints: List<Checkpoint>,
    visitedIndices: Set<Int>,
    nextCheckpointIndex: Int,
    isRunActive: Boolean,
    draggingIndex: Int?,
    onCheckpointLongClick: (Int) -> Unit
) {
    if (checkpoints.isEmpty()) return

    checkpoints.forEachIndexed { index, checkpoint ->
        val status = when {
            !isRunActive -> CheckpointStatus.INACTIVE
            index in visitedIndices -> CheckpointStatus.VISITED
            index == nextCheckpointIndex -> CheckpointStatus.NEXT
            else -> CheckpointStatus.PENDING
        }

        CheckpointMarker(
            index = index,
            checkpoint = checkpoint,
            status = status,
            isDragging = index == draggingIndex,
            isRunActive = isRunActive,
            onLongClick = { onCheckpointLongClick(index) }
        )
    }
}

@Composable
private fun CheckpointMarker(
    index: Int,
    checkpoint: Checkpoint,
    status: CheckpointStatus,
    isDragging: Boolean,
    isRunActive: Boolean,
    onLongClick: () -> Unit
) {
    val circleColor = when (status) {
        CheckpointStatus.VISITED -> Color(0xFF4CAF50)
        CheckpointStatus.NEXT -> Color(0xFF2196F3)
        CheckpointStatus.PENDING -> Color(0xFF9E9E9E)
        CheckpointStatus.INACTIVE -> Color(0xFFFF5722)
    }

    val circleRadius = when {
        isDragging -> 16.dp
        status == CheckpointStatus.NEXT -> 14.dp
        else -> 12.dp
    }

    val strokeColor = if (isDragging) Color(0xFFFFEB3B) else Color.White
    val strokeWidth = if (isDragging) 3.dp else 2.dp

    val feature = Feature(
        geometry = Point(Position(checkpoint.position.longitude, checkpoint.position.latitude)),
        properties = null
    )

    val source = rememberGeoJsonSource(
        data = GeoJsonData.Features(feature)
    )

    CircleLayer(
        id = "control-point-circle-$index",
        source = source,
        color = const(circleColor),
        radius = const(circleRadius),
        strokeColor = const(strokeColor),
        strokeWidth = const(strokeWidth),
        onLongClick = {
            if (!isRunActive) {
                onLongClick()
            }
            ClickResult.Consume
        }
    )

    SymbolLayer(
        id = "control-point-label-$index",
        source = source,
        textField = format(span(const("${index + 1}"))),
        textSize = const(12.sp),
        textColor = const(Color.White),
        textFont = const(listOf("Noto Sans Regular")),
        textAllowOverlap = const(true),
        textIgnorePlacement = const(true)
    )
}

@Composable
fun UserLocationLayer(location: Location?) {
    location ?: return

    val locationFeature = Feature(
        geometry = Point(Position(location.longitude, location.latitude)),
        properties = null
    )

    val locationSource = rememberGeoJsonSource(
        data = GeoJsonData.Features(locationFeature)
    )

    CircleLayer(
        id = "user-location",
        source = locationSource,
        color = const(Color(0xFF2196F3)),
        radius = const(8.dp),
        strokeColor = const(Color.White),
        strokeWidth = const(3.dp)
    )
}

@Composable
fun NextCheckpointLineLayer(
    currentLocation: Location?,
    nextCheckpoint: Checkpoint?,
    isRunActive: Boolean
) {
    if (!isRunActive || currentLocation == null || nextCheckpoint == null) return

    val positions = listOf(
        Position(currentLocation.longitude, currentLocation.latitude),
        Position(nextCheckpoint.position.longitude, nextCheckpoint.position.latitude)
    )

    val lineString = LineString(positions)
    val feature = Feature(geometry = lineString, properties = null)

    val source = rememberGeoJsonSource(
        data = GeoJsonData.Features(feature)
    )

    LineLayer(
        id = "next-checkpoint-line",
        source = source,
        color = const(Color(0xFFFF5722)),
        opacity = const(0.6f),
        width = const(2.dp),
    )
}


@Composable
fun RoutePathLayer(
    pathData: List<PathPoint>,
    color: Color = Color(0xFF2196F3),
    width: Float = 4f
) {
    if (pathData.size < 2) return

    val positions = pathData.map { point ->
        Position(point.longitude, point.latitude)
    }

    val lineString = LineString(positions)
    val feature = Feature(geometry = lineString, properties = null)

    val source = rememberGeoJsonSource(
        data = GeoJsonData.Features(feature)
    )

    LineLayer(
        id = "route-path-outline",
        source = source,
        color = const(Color.White),
        width = const((width + 2).dp)
    )

    LineLayer(
        id = "route-path",
        source = source,
        color = const(color),
        width = const(width.dp)
    )
}