package com.mobileorienteering.ui.screen.main

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mobileorienteering.R

@Composable
fun RouteToggleButton(
    isTracking: Boolean,
    isShowingRoute: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isTracking) {
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            containerColor = if (isShowingRoute)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_route),
                contentDescription = if (isShowingRoute) "Ukryj trasę" else "Pokaż trasę",
                tint = if (isShowingRoute)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun AddCheckpointButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .shadow(
                elevation = 3.dp,
                spotColor = Color(0x4D000000),
                ambientColor = Color(0x4D000000)
            )
            .shadow(
                elevation = 8.dp,
                spotColor = Color(0x26000000),
                ambientColor = Color(0x26000000)
            )
            .size(80.dp),
        containerColor = Color(0xFF624000),
        shape = RoundedCornerShape(20.dp)
    ) {
        Icon(
            painter = painterResource(id = android.R.drawable.ic_input_add),
            contentDescription = "Dodaj checkpoint",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun LocationTrackingButton(
    hasPermission: Boolean,
    isTracking: Boolean,
    onRequestPermission: () -> Unit,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = {
            if (!hasPermission) {
                onRequestPermission()
            } else if (isTracking) {
                onStopTracking()
            } else {
                onStartTracking()
            }
        },
        modifier = modifier,
        containerColor = if (isTracking)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surface
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_location),
            contentDescription = if (isTracking) "Zatrzymaj" else "Start",
            tint = if (isTracking)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}