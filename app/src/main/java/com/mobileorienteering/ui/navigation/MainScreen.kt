package com.mobileorienteering.ui.navigation

import androidx.annotation.DrawableRes
import com.mobileorienteering.R

sealed class MainScreen(
    val route: String,
    val label: String,
    @get:DrawableRes val iconFilled: Int,
    @get:DrawableRes val iconOutlined: Int
) {
    object Map : MainScreen(
        "map",
        "Map",
        R.drawable.ic_map_filled,
        R.drawable.ic_map_outlined
    )

    object Library : MainScreen(
        "library",
        "Library",
        R.drawable.ic_library_filled,
        R.drawable.ic_library_outlined
    )

    object Runs : MainScreen(
        "runs",
        "Runs",
        R.drawable.ic_runs_filled,
        R.drawable.ic_runs_outlined
    )

    object Settings : MainScreen(
        "settings",
        "Settings",
        R.drawable.ic_settings_filled,
        R.drawable.ic_settings_outlined
    )

    companion object {
        val items = listOf(Map, Library, Runs, Settings)
    }
}
