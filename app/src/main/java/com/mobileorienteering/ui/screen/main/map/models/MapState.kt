package com.mobileorienteering.ui.screen.main.map.models

import android.location.Location
import org.maplibre.spatialk.geojson.Position

data class MapState(
    val currentLocation: Location? = null,
    val isTracking: Boolean = false,
    val hasPermission: Boolean = false,
    val error: String? = null,
    val locationHistory: List<Location> = emptyList(),
    val distanceTraveled: Float = 0f,
    val trackingStartTime: Long = 0L,
    val checkpoints: List<Checkpoint> = emptyList()
)

data class Checkpoint(
    val id: String = java.util.UUID.randomUUID().toString(),
    val position: Position,
    val name: String = "",
    val timestamp: Long = System.currentTimeMillis()
)