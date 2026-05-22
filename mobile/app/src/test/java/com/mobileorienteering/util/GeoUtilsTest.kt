package com.mobileorienteering.util

import com.mobileorienteering.data.model.domain.ControlPoint
import com.mobileorienteering.data.model.domain.PathPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class GeoUtilsTest {

    @Test
    fun calculateDistanceBetweenPoints_samePoint_returnsZero() {
        val distance = calculateDistanceBetweenPoints(52.2297, 21.0122, 52.2297, 21.0122)
        assertEquals(0.0, distance, 0.1)
    }

    @Test
    fun calculateDistanceBetweenPoints_knownSeparation_isPositive() {
        val distance = calculateDistanceBetweenPoints(52.0, 21.0, 52.001, 21.0)
        assertTrue(distance > 50.0)
        assertTrue(distance < 200.0)
    }

    @Test
    fun calculateTotalDistance_fewerThanTwoPoints_returnsZero() {
        assertEquals(0.0, calculateTotalDistance(emptyList()), 0.0)
        assertEquals(0.0, calculateTotalDistance(listOf(pathPoint(52.0, 21.0, t(0)))), 0.0)
    }

    @Test
    fun calculateTotalDistance_twoPoints_equalsSegmentDistance() {
        val path = listOf(
            pathPoint(52.0, 21.0, t(0)),
            pathPoint(52.001, 21.0, t(60))
        )
        val expected = calculateDistanceBetweenPoints(52.0, 21.0, 52.001, 21.0)
        assertEquals(expected, calculateTotalDistance(path), 0.1)
    }

    @Test
    fun boundingCamera_empty_returnsDefaults() {
        val camera = boundingCamera(emptyList())
        assertEquals(0.0, camera.centerLat, 0.0)
        assertEquals(0.0, camera.centerLon, 0.0)
        assertEquals(10.0, camera.zoom, 0.0)
    }

    @Test
    fun boundingCamera_singlePoint_usesFixedZoom() {
        val camera = boundingCamera(listOf(52.0 to 21.0))
        assertEquals(52.0, camera.centerLat, 0.0)
        assertEquals(21.0, camera.centerLon, 0.0)
        assertEquals(15.0, camera.zoom, 0.0)
    }

    @Test
    fun boundingCamera_multiplePoints_centersAndClampsZoom() {
        val camera = boundingCamera(
            listOf(
                52.0 to 21.0,
                52.01 to 21.02
            )
        )
        assertEquals(52.005, camera.centerLat, 0.001)
        assertEquals(21.01, camera.centerLon, 0.001)
        assertTrue(camera.zoom in 10.0..17.0)
    }

    @Test
    fun offsetToward_zeroDistance_returnsFromPoint() {
        val (lat, lon) = offsetToward(52.0, 21.0, 52.0, 21.0, 10.0)
        assertEquals(52.0, lat, 0.0)
        assertEquals(21.0, lon, 0.0)
    }

    @Test
    fun offsetToward_movesAlongSegment() {
        val (lat, lon) = offsetToward(52.0, 21.0, 52.01, 21.0, 50.0)
        assertTrue(lat > 52.0 && lat < 52.01)
        assertEquals(21.0, lon, 0.0001)
    }

    @Test
    fun computeVisitedControlPoints_emptyInputs_returnsEmpty() {
        assertTrue(
            computeVisitedControlPoints(emptyList(), listOf(controlPoint(0)), 15).isEmpty()
        )
        assertTrue(
            computeVisitedControlPoints(listOf(pathPoint(52.0, 21.0, t(0))), emptyList(), 15).isEmpty()
        )
    }

    @Test
    fun computeVisitedControlPoints_ordered_visitsInSequence() {
        val controlPoints = listOf(
            controlPoint(0, 52.0, 21.0, "Start"),
            controlPoint(1, 52.001, 21.0, "CP2"),
            controlPoint(2, 52.002, 21.0, "Finish")
        )
        val path = listOf(
            pathPoint(52.0, 21.0, t(0)),
            pathPoint(52.001, 21.0, t(60)),
            pathPoint(52.002, 21.0, t(120))
        )

        val visited = computeVisitedControlPoints(
            pathData = path,
            controlPoints = controlPoints,
            radiusMeters = 50,
            orderedControlPoints = true
        )

        assertEquals(3, visited.size)
        assertEquals(listOf(0, 1, 2), visited.map { it.checkpointIndex })
        assertEquals(listOf(1, 2, 3), visited.map { it.order })
        assertEquals("Start", visited[0].controlPointName)
    }

    @Test
    fun computeVisitedControlPoints_ordered_skipsOutOfOrderVisit() {
        val controlPoints = listOf(
            controlPoint(0, 52.0, 21.0),
            controlPoint(1, 52.001, 21.0)
        )
        val path = listOf(
            pathPoint(52.001, 21.0, t(0)),
            pathPoint(52.0, 21.0, t(60))
        )

        val visited = computeVisitedControlPoints(
            pathData = path,
            controlPoints = controlPoints,
            radiusMeters = 50,
            orderedControlPoints = true
        )

        assertEquals(1, visited.size)
        assertEquals(0, visited[0].checkpointIndex)
    }

    @Test
    fun computeVisitedControlPoints_anyOrder_visitsInPathOrder() {
        val controlPoints = listOf(
            controlPoint(0, 52.0, 21.0, "A"),
            controlPoint(1, 52.001, 21.0, "B")
        )
        val path = listOf(
            pathPoint(52.001, 21.0, t(0)),
            pathPoint(52.0, 21.0, t(60))
        )

        val visited = computeVisitedControlPoints(
            pathData = path,
            controlPoints = controlPoints,
            radiusMeters = 50,
            orderedControlPoints = false
        )

        assertEquals(2, visited.size)
        assertEquals(1, visited[0].checkpointIndex)
        assertEquals(0, visited[1].checkpointIndex)
        assertEquals(listOf(1, 2), visited.map { it.order })
    }

    @Test
    fun computeVisitedControlPoints_sortsPathByTimestamp() {
        val controlPoints = listOf(controlPoint(0, 52.0, 21.0))
        val path = listOf(
            pathPoint(52.0, 21.0, t(120)),
            pathPoint(52.0, 21.0, t(0))
        )

        val visited = computeVisitedControlPoints(
            pathData = path,
            controlPoints = controlPoints,
            radiusMeters = 50
        )

        assertEquals(1, visited.size)
        assertEquals(t(0), visited[0].visitedAt)
    }

    @Test
    fun webMercatorMetersPerPixel_decreasesWithZoom() {
        val zoom10 = webMercatorMetersPerPixel(52.0, 10.0)
        val zoom14 = webMercatorMetersPerPixel(52.0, 14.0)
        assertTrue(zoom10 > zoom14)
    }

    private fun t(seconds: Long) = Instant.parse("2024-01-01T10:00:00Z").plusSeconds(seconds)

    private fun pathPoint(lat: Double, lon: Double, timestamp: Instant) =
        PathPoint(id = 0, lat = lat, lon = lon, timestamp = timestamp)

    private fun controlPoint(
        index: Int,
        lat: Double = 52.0,
        lon: Double = 21.0,
        name: String = ""
    ) = ControlPoint(
        id = index.toLong(),
        lat = lat,
        lon = lon,
        name = name,
        sequence = index + 1
    )
}
