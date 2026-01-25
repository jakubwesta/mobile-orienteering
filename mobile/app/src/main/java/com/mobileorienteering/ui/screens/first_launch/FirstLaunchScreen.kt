package com.mobileorienteering.ui.screens.first_launch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mobileorienteering.R
import com.mobileorienteering.ui.core.Strings
import com.mobileorienteering.ui.core.components.VideoPlayerFromRaw
import com.mobileorienteering.ui.screens.first_launch.components.FeatureItem

@Composable
fun FirstLaunchScreen(
    onContinue: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            Text(
                text = Strings.Welcome.welcome,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = Strings.App.name,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(32.dp))

            VideoPlayerFromRaw(
                rawResId = R.raw.welcome_screen_video,
                modifier = Modifier.fillMaxWidth(),
                aspectRatio = 16f / 9f
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = Strings.Welcome.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.weight(1f))

            FeaturesList()

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = Strings.Welcome.getStarted,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FeaturesList() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FeatureItem(
            icon = R.drawable.ic_map_outlined,
            text = Strings.Welcome.createCustomCourses
        )
        FeatureItem(
            icon = R.drawable.ic_runs_outlined,
            text = Strings.Welcome.trackWithGps
        )
        FeatureItem(
            icon = R.drawable.ic_library_outlined,
            text = Strings.Welcome.saveAndShare
        )
    }
}
