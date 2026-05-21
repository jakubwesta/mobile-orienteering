package com.mobileorienteering.ui.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mobileorienteering.data.model.domain.OrientationType
import com.mobileorienteering.data.model.domain.RaceStyle
import com.mobileorienteering.data.model.domain.TimerStart
import com.mobileorienteering.data.model.app.RunSettings
import com.mobileorienteering.ui.screens.auth.LoginScreen
import com.mobileorienteering.ui.screens.auth.RegisterScreen
import com.mobileorienteering.ui.screens.library.LibraryScreen
import com.mobileorienteering.ui.screens.runs.RunsScreen
import com.mobileorienteering.ui.screens.settings.SettingsScreen
import com.mobileorienteering.ui.screens.map.MapScreen
import com.mobileorienteering.ui.screens.first_launch.FirstLaunchScreen
import com.mobileorienteering.ui.screens.runs.RunDetailsScreen
import com.mobileorienteering.ui.screens.runs.RunSplitsScreen
import com.mobileorienteering.ui.screens.settings.EditPasswordScreen
import com.mobileorienteering.ui.screens.settings.EditProfileScreen

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
            route = "${AppScreen.Map.route}?mapId={mapId}&startRun={startRun}" +
                    "&orderedCPs={orderedCPs}&timerStart={timerStart}" +
                    "&raceStyle={raceStyle}&orientationType={orientationType}" +
                    "&detectionRadius={detectionRadius}",
            arguments = listOf(
                navArgument("mapId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("startRun") {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument("orderedCPs") {
                    type = NavType.BoolType
                    defaultValue = true
                },
                navArgument("timerStart") {
                    type = NavType.StringType
                    defaultValue = TimerStart.RACE_START.value
                },
                navArgument("raceStyle") {
                    type = NavType.StringType
                    defaultValue = RaceStyle.STANDARD.value
                },
                navArgument("orientationType") {
                    type = NavType.StringType
                    defaultValue = OrientationType.FOOT.value
                },
                navArgument("detectionRadius") {
                    type = NavType.FloatType
                    defaultValue = 15f
                }
            )
        ) { backStackEntry ->
            val args = backStackEntry.arguments
            val mapId = args?.getLong("mapId") ?: -1L
            val startRun = args?.getBoolean("startRun") ?: false
            val runSettings = RunSettings(
                orderedControlPoints = args?.getBoolean("orderedCPs") ?: true,
                timerStart = TimerStart.fromValue(args?.getString("timerStart") ?: ""),
                raceStyle = RaceStyle.fromValue(args?.getString("raceStyle") ?: ""),
                orientationType = OrientationType.fromValue(args?.getString("orientationType") ?: ""),
                detectionRadius = args?.getFloat("detectionRadius") ?: 15f
            )
            MapScreen(
                initialMapId = if (mapId != -1L) mapId else null,
                startRun = startRun,
                runSettings = runSettings,
                onMapSaved = {
                    navController.navigate(AppScreen.Library.route) {
                        popUpTo(AppScreen.Library.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onRunFinished = {
                    navController.navigate(AppScreen.Library.route) {
                        popUpTo(AppScreen.Library.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(AppScreen.Library.route) {
            LibraryScreen(
                onEditMap = { mapId ->
                    navController.navigate("${AppScreen.Map.route}?mapId=$mapId")
                },
                onStartRun = { mapId, options ->
                    navController.navigate(
                        "${AppScreen.Map.route}?mapId=$mapId&startRun=true" +
                        "&orderedCPs=${options.orderedControlPoints}" +
                        "&timerStart=${options.timerStart.value}" +
                        "&raceStyle=${options.raceStyle.value}" +
                        "&orientationType=${options.orientationType.value}" +
                        "&detectionRadius=${options.detectionRadius}"
                    )
                },
                onCreateFirstMap = {
                    navController.navigate(AppScreen.Map.route)
                }
            )
        }

        composable(AppScreen.Runs.route) {
            RunsScreen(navController = navController)
        }

        composable(
            route = AppScreen.RunDetails.route,
            arguments = listOf(
                navArgument("runId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val runId = backStackEntry.arguments?.getLong("runId") ?: return@composable
            RunDetailsScreen(
                runId = runId,
                onNavigateBack = { navController.popBackStack() },
                onViewSplits = {
                    navController.navigate(AppScreen.RunSplits.createRoute(runId))
                }
            )
        }

        composable(
            route = AppScreen.RunSplits.route,
            arguments = listOf(
                navArgument("runId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val runId = backStackEntry.arguments?.getLong("runId") ?: return@composable
            RunSplitsScreen(
                runId = runId,
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
