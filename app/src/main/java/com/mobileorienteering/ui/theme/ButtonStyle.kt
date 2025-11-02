package com.mobileorienteering.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AppButtonColors() = ButtonDefaults.buttonColors(
    containerColor = PurpleMedium,
    contentColor = Color.Black
)

val AppButtonShape = RoundedCornerShape(50)
