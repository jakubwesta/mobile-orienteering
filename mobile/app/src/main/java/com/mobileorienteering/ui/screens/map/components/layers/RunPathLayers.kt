package com.mobileorienteering.ui.screens.map.components.layers

import android.location.Location
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.mobileorienteering.data.model.app.Checkpoint
import com.mobileorienteering.data.model.domain.PathPoint
import com.mobileorienteering.ui.theme.NextCheckpointLineColor
import com.mobileorienteering.ui.theme.RoutePathColor
import com.mobileorienteering.ui.theme.RoutePathOutline
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Position

@Composable
fun NextCheckpointLineLayer(
    currentLocation: Location?,
    nextCheckpoint: Checkpoint?
) {
    if (currentLocation == null || nextCheckpoint == null) return

    val source = rememberGeoJsonSource(
        data = GeoJsonData.Features(
            Feature(
                geometry = LineString(
                    listOf(
                        Position(currentLocation.longitude, currentLocation.latitude),
                        Position(nextCheckpoint.position.longitude, nextCheckpoint.position.latitude)
                    )
                ),
                properties = null
            )
        )
    )

    LineLayer(
        id = "next-checkpoint-line",
        source = source,
        color = const(NextCheckpointLineColor),
        opacity = const(0.6f),
        width = const(2.dp)
    )
}

@Composable
fun RoutePathLayer(
    pathData: List<PathPoint>,
    width: Float = 4f
) {
    if (pathData.size < 2) return

    val source = rememberGeoJsonSource(
        data = GeoJsonData.Features(
            Feature(
                geometry = LineString(pathData.map { Position(it.lon, it.lat) }),
                properties = null
            )
        )
    )

    LineLayer(
        id = "route-path-outline",
        source = source,
        color = const(RoutePathOutline),
        width = const((width + 2).dp)
    )

    LineLayer(
        id = "route-path",
        source = source,
        color = const(RoutePathColor),
        width = const(width.dp)
    )
}
