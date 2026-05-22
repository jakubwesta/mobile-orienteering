package com.mobileorienteering.util

import com.mobileorienteering.data.model.domain.PathPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class PathPlaybackTest {

    @Test
    fun pathPlaybackDurationMillis_fewerThanTwoPoints_returnsMinimum() {
        assertEquals(3_000L, pathPlaybackDurationMillis(emptyList()))
        assertEquals(3_000L, pathPlaybackDurationMillis(listOf(point(0))))
    }

    @Test
    fun pathPlaybackDurationMillis_shortTrack_returnsActualDuration() {
        val path = listOf(
            point(0),
            point(5_000)
        )
        assertEquals(5_000L, pathPlaybackDurationMillis(path))
    }

    @Test
    fun pathPlaybackDurationMillis_longTrack_clampsToMaximum() {
        val path = listOf(
            point(0),
            point(120_000)
        )
        assertEquals(30_000L, pathPlaybackDurationMillis(path))
    }

    @Test
    fun interpolatePathPosition_empty_returnsNull() {
        assertNull(interpolatePathPosition(emptyList(), 0.5f))
    }

    @Test
    fun interpolatePathPosition_singlePoint_returnsThatPoint() {
        val path = listOf(point(0, lat = 52.1, lon = 21.2))
        val position = interpolatePathPosition(path, 0.5f)
        assertNotNull(position)
        assertEquals(21.2, position!!.longitude, 0.0001)
        assertEquals(52.1, position.latitude, 0.0001)
    }

    @Test
    fun interpolatePathPosition_twoPoints_progressEndpoints() {
        val path = listOf(
            point(0, lat = 52.0, lon = 21.0),
            point(60, lat = 52.01, lon = 21.01)
        )

        val start = interpolatePathPosition(path, 0f)!!
        val end = interpolatePathPosition(path, 1f)!!

        assertEquals(21.0, start.longitude, 0.0001)
        assertEquals(52.0, start.latitude, 0.0001)
        assertEquals(21.01, end.longitude, 0.0001)
        assertEquals(52.01, end.latitude, 0.0001)
    }

    @Test
    fun interpolatePathPosition_twoPoints_midpointBetweenEndpoints() {
        val path = listOf(
            point(0, lat = 52.0, lon = 21.0),
            point(60, lat = 52.02, lon = 21.0)
        )

        val mid = interpolatePathPosition(path, 0.5f)!!

        assertTrue(mid.latitude > 52.0 && mid.latitude < 52.02)
        assertEquals(21.0, mid.longitude, 0.0001)
    }

    @Test
    fun interpolatePathPosition_duplicatePoints_returnsStart() {
        val path = listOf(
            point(0, lat = 52.0, lon = 21.0),
            point(60, lat = 52.0, lon = 21.0)
        )

        val mid = interpolatePathPosition(path, 0.5f)!!

        assertEquals(52.0, mid.latitude, 0.0001)
        assertEquals(21.0, mid.longitude, 0.0001)
    }

    @Test
    fun playbackSpeedKmh_fewerThanTwoPoints_returnsNull() {
        assertNull(playbackSpeedKmh(emptyList(), 0.5f))
        assertNull(playbackSpeedKmh(listOf(point(0)), 0.5f))
    }

    @Test
    fun playbackSpeedKmh_movingPath_returnsPositiveSpeed() {
        val path = listOf(
            point(0, lat = 52.0, lon = 21.0),
            point(10_000, lat = 52.001, lon = 21.0),
            point(20_000, lat = 52.002, lon = 21.0),
            point(30_000, lat = 52.003, lon = 21.0)
        )

        val speed = playbackSpeedKmh(path, 0.5f)

        assertNotNull(speed)
        assertTrue(speed!! > 0.0)
    }

    @Test
    fun playbackSpeedKmh_stationaryPath_returnsZero() {
        val path = listOf(
            point(0, lat = 52.0, lon = 21.0),
            point(10, lat = 52.0, lon = 21.0)
        )

        assertEquals(0.0, playbackSpeedKmh(path, 0.5f)!!, 0.0)
    }

    @Test
    fun formatPlaybackSpeedKmh_formatsWithOneDecimal() {
        assertEquals("12.3 km/h", formatPlaybackSpeedKmh(12.34))
        assertEquals("0.0 km/h", formatPlaybackSpeedKmh(0.0))
    }

    private fun point(
        millis: Long,
        lat: Double = 52.0,
        lon: Double = 21.0
    ) = PathPoint(
        id = 0,
        lat = lat,
        lon = lon,
        timestamp = Instant.parse("2024-01-01T10:00:00Z").plusMillis(millis)
    )
}
