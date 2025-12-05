package com.mobileorienteering.ui.screen.main.map.models

import android.location.Location
import org.maplibre.spatialk.geojson.Position
import java.time.Instant

data class MapState(
    val currentLocation: Location? = null,
    val isTracking: Boolean = false,
    val hasPermission: Boolean = false,
    val error: String? = null,
    val locationHistory: List<Location> = emptyList(),
    val distanceTraveled: Float = 0f,
    val trackingStartTime: Long = 0L,
    val checkpoints: List<Checkpoint> = emptyList(),

    // Aktualnie załadowana mapa
    val currentMapId: Long? = null,
    val currentMapName: String? = null,

    // Stan aktywnego biegu
    val isRunActive: Boolean = false,
    val runStartTime: Instant? = null,
    val visitedCheckpointIndices: Set<Int> = emptySet(),
    val runDistance: Double = 0.0,

    // Kolejność checkpointów - następny do zaliczenia (0 = pierwszy)
    val nextCheckpointIndex: Int = 0
)

data class Checkpoint(
    val id: String = java.util.UUID.randomUUID().toString(),
    val position: Position,
    val name: String = "",
    val timestamp: Long = System.currentTimeMillis()
)