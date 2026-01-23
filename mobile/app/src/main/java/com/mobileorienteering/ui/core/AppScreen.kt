package com.mobileorienteering.ui.core

import com.mobileorienteering.R

sealed class AppScreen(
    val route: String,
    val label: String? = null,
    val iconFilled: Int? = null,
    val iconOutlined: Int? = null
) {
    object Welcome : AppScreen("welcome")
    object Login : AppScreen("login")
    object Register : AppScreen("register")

    object EditProfile : AppScreen("edit_profile")
    object EditPassword : AppScreen("edit_password")

    object Map : AppScreen(
        route = "map",
        label = "Map",
        iconFilled = R.drawable.ic_map_filled,
        iconOutlined = R.drawable.ic_map_outlined
    )

    object Library : AppScreen(
        route = "library",
        label = "Library",
        iconFilled = R.drawable.ic_library_filled,
        iconOutlined = R.drawable.ic_library_outlined
    )

    object Runs : AppScreen(
        route = "runs",
        label = "Runs",
        iconFilled = R.drawable.ic_runs_filled,
        iconOutlined = R.drawable.ic_runs_outlined
    )

    object RunDetails : AppScreen("run_details/{activityId}") {
        fun createRoute(activityId: Long) = "run_details/$activityId"
    }

    object Settings : AppScreen(
        route = "settings",
        label = "Settings",
        iconFilled = R.drawable.ic_settings_filled,
        iconOutlined = R.drawable.ic_settings_outlined
    )

    companion object {
        val mainScreens = listOf(Map, Library, Runs, Settings)
    }
}
