package com.mobileorienteering.ui.screen.main.map.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.mobileorienteering.R

@Composable
fun LocationFab(
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
            MaterialTheme.colorScheme.surface,
        contentColor = if (isTracking)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurface
    ) {
        Icon(
            painter = painterResource(
                id = if (isTracking)
                    R.drawable.ic_my_location_filled
                else
                    R.drawable.ic_my_location_outlined
            ),
            contentDescription = if (isTracking) "Disable location tracking" else "Enable location tracking"
        )
    }
}
