package com.mobileorienteering.ui.screens.map.components.layers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.mobileorienteering.data.model.app.Checkpoint
import com.mobileorienteering.data.model.app.MapIconStyle
import com.mobileorienteering.ui.theme.CheckpointColorDrag
import com.mobileorienteering.ui.theme.CheckpointColorInactive
import com.mobileorienteering.ui.theme.CheckpointColorNext
import com.mobileorienteering.ui.theme.CheckpointColorPending
import com.mobileorienteering.ui.theme.CheckpointColorVisited
import com.mobileorienteering.ui.theme.CourseLineColor
import com.mobileorienteering.ui.theme.ModernMarkerLabelColor
import com.mobileorienteering.ui.theme.ModernMarkerStrokeColor
import com.mobileorienteering.util.calculateDistanceBetweenPoints
import com.mobileorienteering.util.metersOnGroundForDpInset
import com.mobileorienteering.util.offsetToward
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.format
import org.maplibre.compose.expressions.dsl.offset
import org.maplibre.compose.expressions.dsl.span
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.MultiLineString
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position

private const val CourseLineInsetDp = 18.0

private val LabelFontBold = listOf("Noto Sans Bold")

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
    cameraState: CameraState,
    iconStyle: MapIconStyle = MapIconStyle.MODERN,
    onCheckpointLongClick: (Int) -> Unit
) {
    if (checkpoints.isEmpty()) return

    CourseLineLayer(checkpoints, cameraState)

    checkpoints.forEachIndexed { index, checkpoint ->
        key(checkpoint.id) {
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
                iconStyle = iconStyle,
                isStart = index == 0,
                isFinish = index == checkpoints.size - 1 && checkpoints.size > 1,
                onLongClick = { onCheckpointLongClick(index) }
            )
        }
    }
}

@Composable
private fun CourseLineLayer(checkpoints: List<Checkpoint>, cameraState: CameraState) {
    if (checkpoints.size < 2) return

    val density = LocalDensity.current.density
    val mpd = cameraState.metersPerDpAtTarget
    val zoom = cameraState.position.zoom
    val latitudeDeg = cameraState.position.target.latitude

    val trimMeters =
        if (mpd > 0.0) mpd * CourseLineInsetDp
        else metersOnGroundForDpInset(latitudeDeg, zoom, CourseLineInsetDp, density)

    val segmentPairs = courseSegmentsTrimmedFromMarkers(checkpoints, trimMeters)
    if (segmentPairs.isEmpty()) return

    val geometry =
        if (segmentPairs.size == 1) LineString(segmentPairs.single().toList())
        else MultiLineString(*segmentPairs.map { it.toList() }.toTypedArray())

    val source = rememberGeoJsonSource(data = GeoJsonData.Features(Feature(geometry = geometry, properties = null)))

    LineLayer(
        id = "course-line",
        source = source,
        color = const(CourseLineColor),
        width = const(6.dp)
    )
}

private fun courseSegmentsTrimmedFromMarkers(
    checkpoints: List<Checkpoint>,
    trimMeters: Double
): List<Array<Position>> {
    if (checkpoints.size < 2) return emptyList()
    val out = mutableListOf<Array<Position>>()
    for (i in 0 until checkpoints.lastIndex) {
        val a = checkpoints[i].position
        val b = checkpoints[i + 1].position
        val latA = a.latitude
        val lonA = a.longitude
        val latB = b.latitude
        val lonB = b.longitude
        val len = calculateDistanceBetweenPoints(latA, lonA, latB, lonB)
        if (len <= trimMeters * 2 + 5.0) continue
        val (sLat, sLon) = offsetToward(latA, lonA, latB, lonB, trimMeters)
        val (eLat, eLon) = offsetToward(latB, lonB, latA, lonA, trimMeters)
        out.add(arrayOf(Position(sLon, sLat), Position(eLon, eLat)))
    }
    return out
}

@Composable
private fun CheckpointMarker(
    index: Int,
    checkpoint: Checkpoint,
    status: CheckpointStatus,
    isDragging: Boolean,
    isRunActive: Boolean,
    iconStyle: MapIconStyle,
    isStart: Boolean,
    isFinish: Boolean,
    onLongClick: () -> Unit
) {
    val markerColor = when (status) {
        CheckpointStatus.VISITED -> CheckpointColorVisited
        CheckpointStatus.NEXT -> CheckpointColorNext
        CheckpointStatus.PENDING -> CheckpointColorPending
        CheckpointStatus.INACTIVE -> CheckpointColorInactive
    }

    when (iconStyle) {
        MapIconStyle.MODERN -> ModernCheckpointMarker(
            index = index,
            checkpoint = checkpoint,
            markerColor = markerColor,
            status = status,
            isDragging = isDragging,
            isRunActive = isRunActive,
            onLongClick = onLongClick
        )
        MapIconStyle.ORIENTEERING -> OrienteeringCheckpointMarker(
            index = index,
            checkpoint = checkpoint,
            markerColor = markerColor,
            isDragging = isDragging,
            isRunActive = isRunActive,
            isStart = isStart,
            isFinish = isFinish,
            onLongClick = onLongClick
        )
    }
}

@Composable
private fun ModernCheckpointMarker(
    index: Int,
    checkpoint: Checkpoint,
    markerColor: Color,
    status: CheckpointStatus,
    isDragging: Boolean,
    isRunActive: Boolean,
    onLongClick: () -> Unit
) {
    val source = rememberGeoJsonSource(data = GeoJsonData.Features(checkpoint.toFeature()))
    val circleRadius: Dp = when {
        isDragging -> 16.dp
        status == CheckpointStatus.NEXT -> 14.dp
        else -> 12.dp
    }
    val strokeColor = if (isDragging) CheckpointColorDrag else ModernMarkerStrokeColor
    val strokeWidth = if (isDragging) 5.dp else 4.dp

    CircleLayer(
        id = "control-point-circle-$index",
        source = source,
        color = const(markerColor),
        radius = const(circleRadius),
        strokeColor = const(strokeColor),
        strokeWidth = const(strokeWidth),
        onLongClick = {
            if (!isRunActive) onLongClick()
            ClickResult.Consume
        }
    )

    SymbolLayer(
        id = "control-point-label-$index",
        source = source,
        textField = format(span(const("${index + 1}"))),
        textSize = const(24.sp),
        textColor = const(ModernMarkerLabelColor),
        textFont = const(LabelFontBold),
        textAllowOverlap = const(true),
        textIgnorePlacement = const(true)
    )
}

@Composable
private fun OrienteeringCheckpointMarker(
    index: Int,
    checkpoint: Checkpoint,
    markerColor: Color,
    isDragging: Boolean,
    isRunActive: Boolean,
    isStart: Boolean,
    isFinish: Boolean,
    onLongClick: () -> Unit
) {
    val strokeColor = if (isDragging) CheckpointColorDrag else markerColor
    val strokeWidth = if (isDragging) 5.dp else 4.dp

    when {
        isStart -> OrienteeringStartMarker(index, checkpoint, strokeColor, isRunActive, onLongClick)
        isFinish -> OrienteeringFinishMarker(index, checkpoint, strokeColor, strokeWidth, isDragging, isRunActive, onLongClick)
        else -> OrienteeringRegularMarker(index, checkpoint, strokeColor, strokeWidth, isDragging, isRunActive, onLongClick)
    }
}

@Composable
private fun OrienteeringStartMarker(
    index: Int,
    checkpoint: Checkpoint,
    strokeColor: Color,
    isRunActive: Boolean,
    onLongClick: () -> Unit
) {
    val source = rememberGeoJsonSource(data = GeoJsonData.Features(checkpoint.toFeature()))

    CircleLayer(
        id = "cp-click-target-$index",
        source = source,
        color = const(Color(0x01000000)),
        radius = const(18.dp),
        strokeColor = const(Color.Transparent),
        strokeWidth = const(0.dp),
        onLongClick = {
            if (!isRunActive) onLongClick()
            ClickResult.Consume
        }
    )
    SymbolLayer(
        id = "cp-start-triangle-$index",
        source = source,
        textField = format(span(const("△"))),
        textSize = const(40.sp),
        textColor = const(strokeColor),
        textHaloColor = const(strokeColor),
        textHaloWidth = const(1.6.dp),
        textFont = const(listOf("Noto Sans Regular")),
        textAllowOverlap = const(true),
        textIgnorePlacement = const(true)
    )
}

@Composable
private fun OrienteeringFinishMarker(
    index: Int,
    checkpoint: Checkpoint,
    strokeColor: Color,
    strokeWidth: Dp,
    isDragging: Boolean,
    isRunActive: Boolean,
    onLongClick: () -> Unit
) {
    val source = rememberGeoJsonSource(data = GeoJsonData.Features(checkpoint.toFeature()))

    CircleLayer(
        id = "cp-finish-outer-$index",
        source = source,
        color = const(Color.Transparent),
        radius = const(if (isDragging) 20.dp else 16.dp),
        strokeColor = const(strokeColor),
        strokeWidth = const(strokeWidth),
        onLongClick = {
            if (!isRunActive) onLongClick()
            ClickResult.Consume
        }
    )
    CircleLayer(
        id = "cp-finish-inner-$index",
        source = source,
        color = const(Color.Transparent),
        radius = const(if (isDragging) 12.dp else 9.dp),
        strokeColor = const(strokeColor),
        strokeWidth = const(strokeWidth)
    )
    val finishLabelOffsetEm = if (isDragging) 1f else 1.4f
    SymbolLayer(
        id = "cp-finish-label-$index",
        source = source,
        textField = format(span(const("${index + 1}"))),
        textSize = const(26.sp),
        textColor = const(strokeColor),
        textFont = const(LabelFontBold),
        textAnchor = const(SymbolAnchor.Top),
        textOffset = offset(0f.em, finishLabelOffsetEm.em),
        textAllowOverlap = const(true),
        textIgnorePlacement = const(true)
    )
}

@Composable
private fun OrienteeringRegularMarker(
    index: Int,
    checkpoint: Checkpoint,
    strokeColor: Color,
    strokeWidth: Dp,
    isDragging: Boolean,
    isRunActive: Boolean,
    onLongClick: () -> Unit
) {
    val source = rememberGeoJsonSource(data = GeoJsonData.Features(checkpoint.toFeature()))

    CircleLayer(
        id = "control-point-circle-$index",
        source = source,
        color = const(Color.Transparent),
        radius = const(if (isDragging) 20.dp else 16.dp),
        strokeColor = const(strokeColor),
        strokeWidth = const(strokeWidth),
        onLongClick = {
            if (!isRunActive) onLongClick()
            ClickResult.Consume
        }
    )
    val regularLabelOffsetEm = if (isDragging) 2.05f else 1.65f
    SymbolLayer(
        id = "control-point-label-$index",
        source = source,
        textField = format(span(const("${index + 1}"))),
        textSize = const(26.sp),
        textColor = const(strokeColor),
        textFont = const(LabelFontBold),
        textAnchor = const(SymbolAnchor.Top),
        textOffset = offset(0f.em, regularLabelOffsetEm.em),
        textAllowOverlap = const(true),
        textIgnorePlacement = const(true)
    )
}

private fun Checkpoint.toFeature() = Feature(
    geometry = Point(Position(position.longitude, position.latitude)),
    properties = null
)
