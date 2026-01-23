package com.mobileorienteering

import com.mobileorienteering.data.local.converter.Converters
import com.mobileorienteering.data.model.domain.ControlPoint
import com.mobileorienteering.data.model.domain.PathPoint
import com.mobileorienteering.data.model.domain.VisitedControlPoint
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConvertersBenchmarkTest {

    private lateinit var converters: Converters

    @Before
    fun setup() {
        converters = Converters()
    }

    // Serialization

    @Test
    fun pathPointList_serialize_600points() {
        val points = generatePathPoints(600)

        val json = converters.fromPathPointList(points)

        assertNotNull(json)
        assertTrue(json.isNotEmpty())
    }

    @Test
    fun pathPointList_serialize_3600points() {
        val points = generatePathPoints(3600)

        val startTime = System.currentTimeMillis()
        val json = converters.fromPathPointList(points)
        val duration = System.currentTimeMillis() - startTime

        println("Serialize 3600 points: ${duration}ms, size: ${json?.length?.div(1024)} KB")

        assertNotNull(json)
        assertTrue(duration < 100, "Serialization took ${duration}ms, expected <100ms")
    }

    @Test
    fun pathPointList_serialize_10800points() {
        val points = generatePathPoints(10800)

        val startTime = System.currentTimeMillis()
        val json = converters.fromPathPointList(points)
        val duration = System.currentTimeMillis() - startTime

        println("Serialize 10800 points: ${duration}ms, size: ${json?.length?.div(1024)} KB")

        assertNotNull(json)
        assertTrue(duration < 300, "Serialization took ${duration}ms, expected <300ms")
    }

    // Deserialization

    @Test
    fun pathPointList_deserialize_3600points() {
        val points = generatePathPoints(3600)
        val json = converters.fromPathPointList(points)!!

        val startTime = System.currentTimeMillis()
        val result = converters.toPathPointList(json)
        val duration = System.currentTimeMillis() - startTime

        println("Deserialize 3600 points: ${duration}ms")

        assertNotNull(result)
        assertEquals(3600, result.size)
        assertTrue(duration < 100, "Deserialization took ${duration}ms, expected <100ms")
    }

    @Test
    fun pathPointList_deserialize_10800points() {
        val points = generatePathPoints(10800)
        val json = converters.fromPathPointList(points)!!

        val startTime = System.currentTimeMillis()
        val result = converters.toPathPointList(json)
        val duration = System.currentTimeMillis() - startTime

        println("Deserialize 10800 points: ${duration}ms")

        assertNotNull(result)
        assertEquals(10800, result.size)
        assertTrue(duration < 300, "Deserialization took ${duration}ms, expected <300ms")
    }

    // Roundtrip integrity

    @Test
    fun pathPointList_roundtrip_dataIntegrity() {
        val original = generatePathPoints(1000)

        val json = converters.fromPathPointList(original)
        val restored = converters.toPathPointList(json)

        assertNotNull(restored)
        assertEquals(original.size, restored.size)

        original.forEachIndexed { index, point ->
            assertEquals(point.latitude, restored[index].latitude)
            assertEquals(point.longitude, restored[index].longitude)
            assertEquals(point.timestamp, restored[index].timestamp)
        }
    }

    @Test
    fun controlPointList_roundtrip_dataIntegrity() {
        val original = generateControlPoints(20)

        val json = converters.fromControlPointList(original)
        val restored = converters.toControlPointList(json)

        assertNotNull(restored)
        assertEquals(original.size, restored.size)

        original.forEachIndexed { index, point ->
            assertEquals(point.id, restored[index].id)
            assertEquals(point.latitude, restored[index].latitude)
            assertEquals(point.longitude, restored[index].longitude)
            assertEquals(point.name, restored[index].name)
        }
    }

    @Test
    fun visitedControlPointList_roundtrip_dataIntegrity() {
        val original = generateVisitedControlPoints(15)

        val json = converters.fromVisitedCheckpointList(original)
        val restored = converters.toVisitedCheckpointList(json)

        assertNotNull(restored)
        assertEquals(original.size, restored.size)

        original.forEachIndexed { index, point ->
            assertEquals(point.controlPointName, restored[index].controlPointName)
            assertEquals(point.order, restored[index].order)
            assertEquals(point.visitedAt, restored[index].visitedAt)
        }
    }

    // JSON size

    @Test
    fun pathPointList_jsonSize_perPointCheck() {
        val points = generatePathPoints(1000)
        val json = converters.fromPathPointList(points)!!

        val bytesPerPoint = json.length / 1000

        println("JSON size: ${json.length / 1024} KB, ${bytesPerPoint} bytes/point")

        assertTrue(bytesPerPoint < 100, "JSON uses ${bytesPerPoint}B per point, expected <100B")
    }

    // Load simulation

    @Test
    fun loadMultipleActivities_performance() {
        val activitiesData = (1..10).map { generatePathPoints(1800) }
        val jsons = activitiesData.map { converters.fromPathPointList(it)!! }

        val startTime = System.currentTimeMillis()
        jsons.forEach { converters.toPathPointList(it) }
        val duration = System.currentTimeMillis() - startTime

        println("Load 10 activities (18000 points total): ${duration}ms")

        assertTrue(duration < 500, "Loading 10 activities took ${duration}ms, expected <500ms")
    }

    // Helpers

    private fun generatePathPoints(count: Int): List<PathPoint> {
        val baseTime = Instant.now()
        return (0 until count).map { i ->
            PathPoint(
                latitude = 52.2297 + (i * 0.00001),
                longitude = 21.0122 + (i * 0.00001),
                timestamp = baseTime.plusSeconds(i.toLong())
            )
        }
    }

    private fun generateControlPoints(count: Int): List<ControlPoint> {
        return (0 until count).map { i ->
            ControlPoint(
                id = i.toLong(),
                latitude = 52.2297 + (i * 0.001),
                longitude = 21.0122 + (i * 0.001),
                name = "Punkt ${i + 1}"
            )
        }
    }

    private fun generateVisitedControlPoints(count: Int): List<VisitedControlPoint> {
        val baseTime = Instant.now()
        return (0 until count).map { i ->
            VisitedControlPoint(
                controlPointName = "Punkt ${i + 1}",
                order = i + 1,
                visitedAt = baseTime.plusSeconds(i * 300L),
                latitude = 52.2297 + (i * 0.001),
                longitude = 21.0122 + (i * 0.001)
            )
        }
    }
}