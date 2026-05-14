package com.mobileorienteering.data.local

import com.mobileorienteering.data.local.entity.ControlPointEntity
import com.mobileorienteering.data.local.entity.MapWithControlPoints
import com.mobileorienteering.data.local.entity.PathPointEntity
import com.mobileorienteering.data.local.entity.RunSettingsEntity
import com.mobileorienteering.data.local.entity.RunWithDetails
import com.mobileorienteering.data.model.domain.ControlPoint
import com.mobileorienteering.data.model.domain.Map
import com.mobileorienteering.data.model.domain.OrientationType
import com.mobileorienteering.data.model.domain.PathPoint
import com.mobileorienteering.data.model.domain.RaceStyle
import com.mobileorienteering.data.model.domain.Run
import com.mobileorienteering.data.model.domain.RunSettings
import com.mobileorienteering.data.model.domain.TimerStart

fun MapWithControlPoints.toDomainModel(): Map {
    return Map(
        id = map.id,
        userId = map.userId,
        name = map.name,
        description = map.description,
        isSnapshot = map.isSnapshot,
        originalMapId = map.originalMapId,
        createdAt = map.createdAt,
        controlPoints = controlPoints.map { it.toDomainModel() },
        imageUrl = map.imageUrl,
        localImagePath = map.localImagePath
    )
}

fun ControlPointEntity.toDomainModel(): ControlPoint {
    return ControlPoint(
        id = id,
        lat = lat,
        lon = lon,
        name = name,
        sequence = sequence
    )
}

fun RunWithDetails.toDomainModel(): Run {
    return Run(
        id = run.id,
        userId = run.userId,
        name = run.name,
        startedAt = run.startedAt,
        finishedAt = run.finishedAt,
        map = Map(
            id = map.id,
            userId = map.userId,
            name = map.name,
            description = map.description,
            isSnapshot = map.isSnapshot,
            originalMapId = map.originalMapId,
            createdAt = map.createdAt,
            controlPoints = controlPoints.map { it.toDomainModel() },
            imageUrl = map.imageUrl,
            localImagePath = map.localImagePath
        ),
        runSettings = settings.toDomainModel(),
        pathPoints = pathPoints.map { it.toDomainModel() }
    )
}

fun RunSettingsEntity.toDomainModel(): RunSettings {
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

fun PathPointEntity.toDomainModel(): PathPoint {
    return PathPoint(
        id = id,
        lat = lat,
        lon = lon,
        timestamp = timestamp
    )
}
