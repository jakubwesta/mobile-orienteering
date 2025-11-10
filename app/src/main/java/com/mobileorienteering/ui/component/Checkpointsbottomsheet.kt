package com.mobileorienteering.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobileorienteering.domain.model.Checkpoint

@Composable
fun CheckpointsBottomSheet(
    checkpoints: List<Checkpoint>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onCheckpointClick: (Checkpoint) -> Unit,
    onCheckpointRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                spotColor = Color(0x26000000),
                ambientColor = Color(0x26000000),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
            .background(
                color = Color(0xFF201B13),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
            .clickable { onToggle() }
            .padding(horizontal = 20.dp)
    ) {
        // Handle bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        color = Color(0xFF7A7265),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }

        // Title (centered)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Manage checkpoints",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        // Checkpoints list (when expanded)
        if (isExpanded && checkpoints.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(checkpoints) { checkpoint ->
                    CheckpointItem(
                        checkpoint = checkpoint,
                        onClick = { onCheckpointClick(checkpoint) },
                        onRemove = { onCheckpointRemove(checkpoint.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CheckpointItem(
    checkpoint: Checkpoint,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF2D2620),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Number badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = Color(0xFFD32F2F),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = checkpoint.number.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Column {
                Text(
                    text = checkpoint.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = "Lat: %.4f, Lon: %.4f".format(
                        checkpoint.location.latitude,
                        checkpoint.location.longitude
                    ),
                    fontSize = 12.sp,
                    color = Color(0xFF9A8F7E)
                )
            }
        }

        // Remove button
        IconButton(onClick = onRemove) {
            Text(
                text = "×",
                fontSize = 24.sp,
                color = Color(0xFF9A8F7E)
            )
        }
    }
}