package com.mobileorienteering.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class MapWithControlPoints(
    @Embedded val map: MapEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "mapId"
    )
    val controlPoints: List<ControlPointEntity>
)

data class RunWithDetails(
    @Embedded val run: RunEntity,
    @Relation(
        parentColumn = "mapId",
        entityColumn = "id"
    )
    val map: MapEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "runId"
    )
    val settings: RunSettingsEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "runId"
    )
    val pathPoints: List<PathPointEntity>,
    @Relation(
        parentColumn = "mapId",
        entityColumn = "mapId"
    )
    val controlPoints: List<ControlPointEntity>
)
