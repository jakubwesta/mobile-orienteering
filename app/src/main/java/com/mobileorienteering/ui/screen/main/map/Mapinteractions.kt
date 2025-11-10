package com.mobileorienteering.ui.screen.main

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.mobileorienteering.domain.model.Checkpoint
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

@SuppressLint("MissingPermission")
fun enableLocationComponent(context: Context, map: MapLibreMap, style: Style) {
    try {
        val locationComponentOptions = LocationComponentOptions.builder(context)
            .accuracyAlpha(0.15f)
            .accuracyColor(android.graphics.Color.BLUE)
            .foregroundTintColor(android.graphics.Color.BLUE)
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
fun disableLocationComponent(map: MapLibreMap) {
    try {
        map.locationComponent.isLocationComponentEnabled = false
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun drawRoute(style: Style, points: List<LatLng>, routeId: String, color: Int) {
    try {
        Log.d("MapInteractions", "Drawing route $routeId with ${points.size} points")

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
        } else {
            val geoJsonSource = source as? org.maplibre.android.style.sources.GeoJsonSource
            geoJsonSource?.setGeoJson(lineGeoJson)
        }
    } catch (e: Exception) {
        Log.e("MapInteractions", "Error drawing route", e)
    }
}

fun drawCheckpoints(style: Style, checkpoints: List<Checkpoint>) {
    try {
        // Usuń stare markery
        try {
            style.removeLayer("checkpoints-layer")
            style.removeSource("checkpoints-source")
        } catch (e: Exception) {
            // Source/layer nie istnieje jeszcze
        }

        if (checkpoints.isEmpty()) return

        // Stwórz GeoJSON z punktami
        val features = checkpoints.map { checkpoint ->
            """
            {
                "type": "Feature",
                "geometry": {
                    "type": "Point",
                    "coordinates": [${checkpoint.location.longitude}, ${checkpoint.location.latitude}]
                },
                "properties": {
                    "number": ${checkpoint.number},
                    "name": "${checkpoint.name}"
                }
            }
            """.trimIndent()
        }

        val geoJson = """
            {
                "type": "FeatureCollection",
                "features": [${features.joinToString(",")}]
            }
        """.trimIndent()

        // Dodaj source
        style.addSource(
            org.maplibre.android.style.sources.GeoJsonSource(
                "checkpoints-source",
                geoJson
            )
        )

        // Dodaj layer z czerwonymi kropkami
        val circleLayer = org.maplibre.android.style.layers.CircleLayer("checkpoints-layer", "checkpoints-source")
            .withProperties(
                org.maplibre.android.style.layers.PropertyFactory.circleRadius(8f),
                org.maplibre.android.style.layers.PropertyFactory.circleColor(android.graphics.Color.RED),
                org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth(2f),
                org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE)
            )

        style.addLayer(circleLayer)

        Log.d("MapInteractions", "Drew ${checkpoints.size} checkpoints")
    } catch (e: Exception) {
        Log.e("MapInteractions", "Error drawing checkpoints", e)
        e.printStackTrace()
    }
}