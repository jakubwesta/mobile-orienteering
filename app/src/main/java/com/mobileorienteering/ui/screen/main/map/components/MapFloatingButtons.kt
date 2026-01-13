package com.mobileorienteering.ui.screen.main.map.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mobileorienteering.R

@Composable
fun TrackingButton(
    isTracking: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = if (isTracking)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.secondaryContainer
    ) {
        Icon(
            Icons.Default.LocationOn,
            contentDescription = if (isTracking) "Stop tracking" else "Start tracking"
        )
    }
}

@Composable
fun AddCheckpointButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Add control point at current location"
        )
    }
}

@Composable
fun CenterCameraButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SmallFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_target),
            contentDescription = "Center on my location",
            modifier = Modifier.size(20.dp)
        )
    }
}