package com.mobileorienteering.domain.model

import org.maplibre.android.geometry.LatLng
import java.util.UUID

data class Checkpoint(
    val id: String = UUID.randomUUID().toString(),
    val number: Int,
    val name: String,
    val location: LatLng,
    val createdAt: Long = System.currentTimeMillis()
)