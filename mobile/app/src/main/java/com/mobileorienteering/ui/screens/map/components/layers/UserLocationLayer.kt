package com.mobileorienteering.ui.screens.map.components.layers

import android.location.Location
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.mobileorienteering.ui.theme.UserLocationColor
import com.mobileorienteering.ui.theme.UserLocationStrokeColor
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position

@Composable
fun UserLocationLayer(location: Location?) {
    location ?: return

    val source = rememberGeoJsonSource(
        data = GeoJsonData.Features(
            Feature(
                geometry = Point(Position(location.longitude, location.latitude)),
                properties = null
            )
        )
    )

    CircleLayer(
        id = "user-location",
        source = source,
        color = const(UserLocationColor),
        radius = const(8.dp),
        strokeColor = const(UserLocationStrokeColor),
        strokeWidth = const(3.dp)
    )
}
