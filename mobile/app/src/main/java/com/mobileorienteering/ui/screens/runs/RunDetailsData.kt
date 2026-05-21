package com.mobileorienteering.ui.screens.runs

import com.mobileorienteering.data.model.app.Checkpoint
import com.mobileorienteering.data.model.domain.PathPoint
import com.mobileorienteering.data.model.domain.Run
import com.mobileorienteering.data.model.domain.TimerStart
import com.mobileorienteering.data.model.domain.VisitedControlPoint
import com.mobileorienteering.util.computeVisitedControlPoints
import org.maplibre.spatialk.geojson.Position

data class RunDetailsData(
    val sortedPathPoints: List<PathPoint>,
    val stableCheckpoints: List<Checkpoint>,
    val visitedControlPoints: List<VisitedControlPoint>,
    val visitedIndices: Set<Int>,
    val sortedVisitedPoints: List<VisitedControlPoint>,
    val timelinePoints: List<VisitedControlPoint>,
    val visitedControlPointCount: Int,
    val totalControlPointCount: Int
)

fun Run.toDetailsData(checkpointRadius: Int, startPointLabel: String): RunDetailsData {
    val sortedPathPoints = pathPoints.sortedBy { it.timestamp }
    val stableCheckpoints = map.controlPoints.mapIndexed { index, cp ->
        Checkpoint(
            id = "run-cp-$index",
            position = Position(cp.lon, cp.lat),
            name = cp.name
        )
    }
    val visitedControlPoints = computeVisitedControlPoints(
        pathData = sortedPathPoints,
        controlPoints = map.controlPoints,
        radiusMeters = checkpointRadius,
        orderedControlPoints = runSettings.orderedControlPoints
    )
    val sortedVisitedPoints = visitedControlPoints.sortedBy { it.visitedAt }
    val includeRaceStartSplit =
        runSettings.timerStart == TimerStart.RACE_START && sortedPathPoints.isNotEmpty()
    val startPoint = if (includeRaceStartSplit) {
        val firstPath = sortedPathPoints.first()
        VisitedControlPoint(
            controlPointName = startPointLabel,
            order = 0,
            checkpointIndex = -1,
            visitedAt = startedAt,
            lat = firstPath.lat,
            lon = firstPath.lon
        )
    } else {
        null
    }
    val timelinePoints = if (startPoint != null) {
        listOf(startPoint) + sortedVisitedPoints
    } else {
        sortedVisitedPoints
    }

    return RunDetailsData(
        sortedPathPoints = sortedPathPoints,
        stableCheckpoints = stableCheckpoints,
        visitedControlPoints = visitedControlPoints,
        visitedIndices = visitedControlPoints.map { it.checkpointIndex }.toSet(),
        sortedVisitedPoints = sortedVisitedPoints,
        timelinePoints = timelinePoints,
        visitedControlPointCount = visitedControlPoints.size,
        totalControlPointCount = map.controlPoints.size
    )
}
