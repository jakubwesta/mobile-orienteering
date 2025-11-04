package com.mobileorienteering.ui.navigation

sealed class AppScreen(val route: String) {
    object Welcome : AppScreen("welcome")

    object Login : AppScreen("login")
    object Register : AppScreen("register")

    object Map : AppScreen("map")
    object Library : AppScreen("library")
    object Runs : AppScreen("runs")
    object Settings : AppScreen("settings")

    companion object {
        val mainScreens = listOf(Map, Library, Runs, Settings)
    }
}
