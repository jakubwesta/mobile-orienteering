package com.mobileorienteering.data.model.network.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateRunRequest(
    val name: String,
    @param:Json(name = "map_id") val mapId: Long,
    @param:Json(name = "run_settings") val runSettings: RunSettingsRequest,
    @param:Json(name = "started_at") val startedAt: String,
    @param:Json(name = "finished_at") val finishedAt: String? = null,
    @param:Json(name = "path_points") val pathPoints: List<PathPointRequest> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PathPointRequest(
    val lat: Double,
    val lon: Double,
    val timestamp: String
)

@JsonClass(generateAdapter = true)
data class RunSettingsRequest(
    @param:Json(name = "detection_radius") val detectionRadius: Float,
    @param:Json(name = "show_self_on_map") val showSelfOnMap: Boolean = true,
    @param:Json(name = "ordered_control_points") val orderedControlPoints: Boolean = true,
    @param:Json(name = "timer_start") val timerStart: String = "race_start",
    @param:Json(name = "race_style") val raceStyle: String = "standard",
    @param:Json(name = "orientation_type") val orientationType: String = "foot"
)
