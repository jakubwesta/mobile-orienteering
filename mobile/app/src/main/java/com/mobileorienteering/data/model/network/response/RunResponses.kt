package com.mobileorienteering.data.model.network.response

import com.mobileorienteering.data.model.domain.OrientationType
import com.mobileorienteering.data.model.domain.PathPoint
import com.mobileorienteering.data.model.domain.RaceStyle
import com.mobileorienteering.data.model.domain.Run
import com.mobileorienteering.data.model.domain.RunSettings
import com.mobileorienteering.data.model.domain.TimerStart
import com.mobileorienteering.util.toInstant
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RunResponse(
    val id: Long,
    @param:Json(name = "user_id") val userId: Long,
    val map: MapResponse,
    val name: String,
    @param:Json(name = "started_at") val startedAt: String,
    @param:Json(name = "finished_at") val finishedAt: String?,
    @param:Json(name = "run_settings") val runSettings: RunSettingsResponse,
    @param:Json(name = "path_points") val pathPoints: List<PathPointResponse>
)

@JsonClass(generateAdapter = true)
data class RunSettingsResponse(
    val id: Long,
    @param:Json(name = "detection_radius") val detectionRadius: Float,
    @param:Json(name = "show_self_on_map") val showSelfOnMap: Boolean = true,
    @param:Json(name = "ordered_control_points") val orderedControlPoints: Boolean = true,
    @param:Json(name = "timer_start") val timerStart: String = "race_start",
    @param:Json(name = "race_style") val raceStyle: String = "standard",
    @param:Json(name = "orientation_type") val orientationType: String = "foot"
)

@JsonClass(generateAdapter = true)
data class PathPointResponse(
    val id: Long,
    @param:Json(name = "run_id") val runId: Long,
    val lat: Double,
    val lon: Double,
    val timestamp: String
)

fun RunResponse.toDomainModel(): Run {
    return Run(
        id = id,
        userId = userId,
        map = map.toDomainModel(),
        name = name,
        startedAt = startedAt.toInstant(),
        finishedAt = finishedAt?.toInstant(),
        runSettings = runSettings.toDomainModel(),
        pathPoints = pathPoints.map { it.toDomainModel() }
    )
}

fun RunSettingsResponse.toDomainModel(): RunSettings {
    return RunSettings(
        id = id,
        detectionRadius = detectionRadius,
        showSelfOnMap = showSelfOnMap,
        orderedControlPoints = orderedControlPoints,
        timerStart = TimerStart.fromValue(timerStart),
        raceStyle = RaceStyle.fromValue(raceStyle),
        orientationType = OrientationType.fromValue(orientationType)
    )
}

fun PathPointResponse.toDomainModel(): PathPoint {
    return PathPoint(
        id = id,
        lat = lat,
        lon = lon,
        timestamp = timestamp.toInstant()
    )
}
