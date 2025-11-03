package com.mobileorienteering.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mobileorienteering.ui.screen.main.LibraryScreen
import com.mobileorienteering.ui.screen.main.MapScreen
import com.mobileorienteering.ui.screen.main.RunsScreen
import com.mobileorienteering.ui.screen.main.SettingsScreen

@Composable
fun MainNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = MainScreen.Map.route,
        modifier = modifier
    ) {
        MainScreen.items.forEach { screen ->
            composable(screen.route) {
                when (screen) {
                    MainScreen.Map -> MapScreen()
                    MainScreen.Library -> LibraryScreen()
                    MainScreen.Runs -> RunsScreen()
                    MainScreen.Settings -> SettingsScreen()
                }
            }
        }
    }
}

