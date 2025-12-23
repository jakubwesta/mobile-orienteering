package com.mobileorienteering.ui.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mobileorienteering.ui.screen.auth.LoginScreen
import com.mobileorienteering.ui.screen.auth.RegisterScreen
import com.mobileorienteering.ui.screen.main.library.LibraryScreen
import com.mobileorienteering.ui.screen.main.runs.RunsScreen
import com.mobileorienteering.ui.screen.main.settings.SettingsScreen
import com.mobileorienteering.ui.screen.main.map.MapScreen
import com.mobileorienteering.ui.screen.welcome.FirstLaunchScreen
import com.mobileorienteering.ui.screen.main.runs.RunDetailsScreen
import com.mobileorienteering.ui.screen.main.settings.EditPasswordScreen
import com.mobileorienteering.ui.screen.main.settings.EditProfileScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    onFirstLaunchComplete: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Welcome & Auth screens
        composable(AppScreen.Welcome.route) {
            FirstLaunchScreen(
                onContinue = {
                    onFirstLaunchComplete()
                    navController.navigate(AppScreen.Login.route)
                }
            )
        }

        composable(AppScreen.Login.route) {
            LoginScreen(navController)
        }

        composable(AppScreen.Register.route) {
            RegisterScreen(navController)
        }

        // Main screens
        composable(
            route = "${AppScreen.Map.route}?mapId={mapId}&startRun={startRun}",
            arguments = listOf(
                navArgument("mapId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("startRun") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val mapId = backStackEntry.arguments?.getLong("mapId") ?: -1L
            val startRun = backStackEntry.arguments?.getBoolean("startRun") ?: false
            MapScreen(
                initialMapId = if (mapId != -1L) mapId else null,
                startRun = startRun
            )
        }

        composable(AppScreen.Library.route) {
            LibraryScreen(
                onEditMap = { mapId ->
                    navController.navigate("${AppScreen.Map.route}?mapId=$mapId")
                },
                onStartRun = { mapId ->
                    navController.navigate("${AppScreen.Map.route}?mapId=$mapId&startRun=true")
                }
            )
        }

        composable(AppScreen.Runs.route) {
            RunsScreen(navController = navController)
        }

        composable(
            route = AppScreen.RunDetails.route,
            arguments = listOf(
                navArgument("activityId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val activityId = backStackEntry.arguments?.getLong("activityId") ?: return@composable
            RunDetailsScreen(
                activityId = activityId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Settings screens
        composable(AppScreen.Settings.route) {
            SettingsScreen(
                onNavigateToEditProfile = {
                    navController.navigate(AppScreen.EditProfile.route)
                },
                onNavigateToEditPassword = {
                    navController.navigate(AppScreen.EditPassword.route)
                }
            )
        }

        composable(AppScreen.EditProfile.route) {
            EditProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppScreen.EditPassword.route) {
            EditPasswordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
