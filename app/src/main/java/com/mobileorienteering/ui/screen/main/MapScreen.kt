package com.mobileorienteering.ui.screen.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap

@Composable
fun MapScreen() {
    MaplibreMap(
        modifier = Modifier.fillMaxSize(),
        options = MapOptions(
            gestureOptions = GestureOptions(
                isTiltEnabled = true,
                isZoomEnabled = true,
                isRotateEnabled = true,
                isScrollEnabled = true,
            )
        )
    )
}