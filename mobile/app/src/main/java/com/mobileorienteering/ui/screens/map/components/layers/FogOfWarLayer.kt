package com.mobileorienteering.ui.screens.map.components.layers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mobileorienteering.data.model.app.Checkpoint
import com.mobileorienteering.ui.theme.FogOfWarColor
import com.mobileorienteering.util.calculateDistanceBetweenPoints
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Polygon
import org.maplibre.spatialk.geojson.Position
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val FOG_VISIBILITY_RADIUS_METERS = 120.0
private const val FOG_CIRCLE_POINTS = 64

@Composable
fun FogOfWarLayer(checkpoints: List<Checkpoint>) {
    if (checkpoints.isEmpty()) return

    val worldRing = remember {
        listOf(
            Position(-180.0, -85.051129),
            Position( 180.0, -85.051129),
            Position( 180.0,  85.051129),
            Position(-180.0,  85.051129),
            Position(-180.0, -85.051129)
        )
    }

    val holes = remember(checkpoints) {
        computeVisibilityHoles(checkpoints)
    }

    val polygon = Polygon(listOf(worldRing) + holes)
    val feature = Feature(geometry = polygon, properties = null)
    val source = rememberGeoJsonSource(data = GeoJsonData.Features(feature))

    FillLayer(
        id = "fog-of-war",
        source = source,
        color = const(FogOfWarColor),
        opacity = const(1f)
    )
}

/**
 * Groups checkpoints into connected components of overlapping circles and returns
 * one hole ring per component. Overlapping circles are merged into a single ring
 * so their intersection never re-fills as fog due to double winding.
 */
private fun computeVisibilityHoles(
    checkpoints: List<Checkpoint>
): List<List<Position>> {
    val n = checkpoints.size
    if (n == 0) return emptyList()

    val parent = IntArray(n) { it }
    fun find(x: Int): Int {
        var i = x
        while (parent[i] != i) i = parent[i]
        var j = x
        while (parent[j] != i) { val tmp = parent[j]; parent[j] = i; j = tmp }
        return i
    }

    for (i in 0 until n) {
        for (j in i + 1 until n) {
            val dist = calculateDistanceBetweenPoints(
                checkpoints[i].position.latitude, checkpoints[i].position.longitude,
                checkpoints[j].position.latitude, checkpoints[j].position.longitude
            )
            if (dist < FOG_VISIBILITY_RADIUS_METERS * 2.0) {
                val ri = find(i); val rj = find(j)
                if (ri != rj) parent[ri] = rj
            }
        }
    }

    val components = mutableMapOf<Int, MutableList<Int>>()
    for (i in 0 until n) components.getOrPut(find(i)) { mutableListOf() }.add(i)

    return components.values.map { indices ->
        val cps = indices.map { checkpoints[it] }
        if (cps.size == 1) {
            singleCircleRing(cps[0].position.latitude, cps[0].position.longitude)
        } else {
            mergedCircleRing(cps)
        }
    }
}

private fun singleCircleRing(lat: Double, lon: Double): List<Position> {
    val earthRadius = 6_378_137.0
    val latRad = Math.toRadians(lat)
    val dLat = Math.toDegrees(FOG_VISIBILITY_RADIUS_METERS / earthRadius)
    val dLon = Math.toDegrees(FOG_VISIBILITY_RADIUS_METERS / (earthRadius * cos(latRad)))
    return (FOG_CIRCLE_POINTS downTo 0).map { i ->
        val angle = 2.0 * PI * i / FOG_CIRCLE_POINTS
        Position(lon + dLon * cos(angle), lat + dLat * sin(angle))
    }
}

/**
 * Computes a single clockwise ring for the union of multiple overlapping circles.
 *
 * For each circle, only arc points that lie outside all other circles are kept —
 * these form the visible portion of that circle's boundary. All kept points from
 * all circles are sorted clockwise around the cluster centroid, producing a valid
 * hole ring with no internal fog artifacts at overlapping areas.
 */
private fun mergedCircleRing(checkpoints: List<Checkpoint>): List<Position> {
    val earthRadius = 6_378_137.0
    data class CircleData(val lat: Double, val lon: Double, val dLat: Double, val dLon: Double)

    val circles = checkpoints.map { cp ->
        val latRad = Math.toRadians(cp.position.latitude)
        CircleData(
            lat = cp.position.latitude,
            lon = cp.position.longitude,
            dLat = Math.toDegrees(FOG_VISIBILITY_RADIUS_METERS / earthRadius),
            dLon = Math.toDegrees(FOG_VISIBILITY_RADIUS_METERS / (earthRadius * cos(latRad)))
        )
    }

    val centLat = circles.sumOf { it.lat } / circles.size
    val centLon = circles.sumOf { it.lon } / circles.size
    val resolution = FOG_CIRCLE_POINTS * 2

    val boundaryPoints = mutableListOf<Pair<Double, Position>>()

    for ((ci, circle) in circles.withIndex()) {
        for (ai in 0 until resolution) {
            val theta = 2.0 * PI * ai / resolution
            val pLat = circle.lat + circle.dLat * sin(theta)
            val pLon = circle.lon + circle.dLon * cos(theta)

            val onUnionBoundary = circles.indices
                .filter { it != ci }
                .all { cj ->
                    calculateDistanceBetweenPoints(
                        pLat, pLon, circles[cj].lat, circles[cj].lon
                    ) >= FOG_VISIBILITY_RADIUS_METERS
                }

            if (onUnionBoundary) {
                val angle = atan2(pLat - centLat, pLon - centLon)
                boundaryPoints.add(angle to Position(pLon, pLat))
            }
        }
    }

    if (boundaryPoints.isEmpty()) return singleCircleRing(circles[0].lat, circles[0].lon)

    val sorted = boundaryPoints.sortedByDescending { it.first }.map { it.second }
    return sorted + listOf(sorted.first())
}
