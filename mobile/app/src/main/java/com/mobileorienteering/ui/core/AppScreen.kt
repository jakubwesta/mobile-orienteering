package com.mobileorienteering.ui.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.mobileorienteering.R

sealed class AppScreen(
    val route: String,
    val iconFilled: Int? = null,
    val iconOutlined: Int? = null
) {
    @get:Composable
    @get:ReadOnlyComposable
    open val label: String? get() = null

    object Welcome : AppScreen("welcome")
    object Login : AppScreen("login")
    object Register : AppScreen("register")

    object EditProfile : AppScreen("edit_profile")
    object EditPassword : AppScreen("edit_password")

    object Map : AppScreen(
        route = "map",
        iconFilled = R.drawable.ic_map_filled,
        iconOutlined = R.drawable.ic_map_outlined
    ) {
        override val label: String
            @Composable
            @ReadOnlyComposable
            get() = Strings.Nav.map
    }

    object Library : AppScreen(
        route = "library",
        iconFilled = R.drawable.ic_library_filled,
        iconOutlined = R.drawable.ic_library_outlined
    ) {
        override val label: String
            @Composable
            @ReadOnlyComposable
            get() = Strings.Nav.library
    }

    object Runs : AppScreen(
        route = "runs",
        iconFilled = R.drawable.ic_runs_filled,
        iconOutlined = R.drawable.ic_runs_outlined
    ) {
        override val label: String
            @Composable
            @ReadOnlyComposable
            get() = Strings.Nav.runs
    }

    object RunDetails : AppScreen("run_details/{activityId}") {
        fun createRoute(activityId: Long) = "run_details/$activityId"
    }

    object Settings : AppScreen(
        route = "settings",
        iconFilled = R.drawable.ic_settings_filled,
        iconOutlined = R.drawable.ic_settings_outlined
    ) {
        override val label: String
            @Composable
            @ReadOnlyComposable
            get() = Strings.Nav.settings
    }

    companion object {
        val mainScreens = listOf(Map, Library, Runs, Settings)
    }
}
