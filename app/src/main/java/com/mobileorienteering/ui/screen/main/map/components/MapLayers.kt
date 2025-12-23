package com.mobileorienteering.ui.screen.main.map.components

import android.location.Location
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobileorienteering.data.model.domain.PathPoint
import com.mobileorienteering.ui.screen.main.map.models.Checkpoint
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

/**
 * Status checkpointu określający jego wygląd
 */
enum class CheckpointStatus {
    INACTIVE,   // Bieg nieaktywny - pomarańczowy
    VISITED,    // Zaliczony - zielony
    NEXT,       // Następny do zaliczenia - niebieski
    PENDING     // Oczekujący - szary
}

/**
 * Renderuje wszystkie checkpointy na mapie
 */
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

/**
 * Pojedynczy marker checkpointu (kółko + numer)
 */
@Composable
private fun CheckpointMarker(
    index: Int,
    checkpoint: Checkpoint,
    status: CheckpointStatus,
    isDragging: Boolean,
    isRunActive: Boolean,
    onLongClick: () -> Unit
) {
    // Kolor kółka zależny od statusu
    val circleColor = when (status) {
        CheckpointStatus.VISITED -> Color(0xFF4CAF50)    // Zielony
        CheckpointStatus.NEXT -> Color(0xFF2196F3)       // Niebieski
        CheckpointStatus.PENDING -> Color(0xFF9E9E9E)    // Szary
        CheckpointStatus.INACTIVE -> Color(0xFFFF5722)   // Pomarańczowy
    }

    // Rozmiar kółka
    val circleRadius = when {
        isDragging -> 16.dp
        status == CheckpointStatus.NEXT -> 14.dp
        else -> 12.dp
    }

    // Obramowanie - żółte dla przeciąganego
    val strokeColor = if (isDragging) Color(0xFFFFEB3B) else Color.White
    val strokeWidth = if (isDragging) 3.dp else 2.dp

    val feature = Feature(
        geometry = Point(Position(checkpoint.position.longitude, checkpoint.position.latitude)),
        properties = null
    )

    val source = rememberGeoJsonSource(
        data = GeoJsonData.Features(feature)
    )

    // Kółko
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

    // Numer na wierzchu
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

/**
 * Renderuje lokalizację użytkownika na mapie
 */
@Composable
fun UserLocationLayer(location: Location?) {
    location ?: return

    val feature = Feature(
        geometry = Point(Position(location.longitude, location.latitude)),
        properties = null
    )

    val source = rememberGeoJsonSource(
        data = GeoJsonData.Features(feature)
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

/**
 * Renderuje trasę GPS na mapie jako polyline
 */
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

    // Biała obwódka (tło)
    LineLayer(
        id = "route-path-outline",
        source = source,
        color = const(Color.White),
        width = const((width + 2).dp)
    )

    // Główna linia trasy
    LineLayer(
        id = "route-path",
        source = source,
        color = const(color),
        width = const(width.dp)
    )
}