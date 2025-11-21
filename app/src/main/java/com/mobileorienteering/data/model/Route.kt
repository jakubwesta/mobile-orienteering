package com.mobileorienteering.data.model

data class Route(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val checkpoints: List<RouteCheckpoint>,
    val createdAt: Long = System.currentTimeMillis()
)

data class RouteCheckpoint(
    val longitude: Double,
    val latitude: Double,
    val name: String,
    val order: Int
)