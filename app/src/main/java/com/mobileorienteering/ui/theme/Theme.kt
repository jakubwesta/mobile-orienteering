package com.mobileorienteering.ui.theme

import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PurpleMedium,
    onPrimary = OnDark,
    secondary = PurpleLight,
    background = BackgroundDark,
    surface = BackgroundDark,
    onBackground = OnDark,
    onSurface = OnDark,
    outline = InputBorder,
    outlineVariant = OutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = PurpleMedium,
    onPrimary = OnDark,
    secondary = PurpleLight
)

@Composable
fun MobileOrienteeringTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
