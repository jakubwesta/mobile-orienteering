package com.mobileorienteering.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.mobileorienteering.ui.navigation.AppScreen
import com.mobileorienteering.ui.screen.auth.LoginScreen
import com.mobileorienteering.ui.screen.auth.RegisterScreen
import com.mobileorienteering.ui.screen.main.library.LibraryScreen
import com.mobileorienteering.ui.screen.main.runs.RunsScreen
import com.mobileorienteering.ui.screen.main.settings.SettingsScreen
import com.mobileorienteering.ui.screen.main.map.MapScreen
import com.mobileorienteering.ui.screen.welcome.FirstLaunchScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobileorienteering.ui.screen.main.runs.RunDetailsScreen
import com.mobileorienteering.ui.screen.main.settings.EditPasswordScreen
import com.mobileorienteering.ui.screen.main.settings.EditProfileScreen
import com.mobileorienteering.ui.screen.main.settings.SyncViewModel

@Composable
fun AppScaffold(
    navController: NavHostController,
    isFirstLaunch: Boolean,
    isLoggedIn: Boolean
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomBar = currentRoute != null && AppScreen.mainScreens.any {
        currentRoute.startsWith(it.route)
    }

    val syncViewModel: SyncViewModel = hiltViewModel()

    LaunchedEffect(isLoggedIn) {
        val isOnMainScreen = currentRoute != null && AppScreen.mainScreens.any {
            currentRoute.startsWith(it.route)
        }
        if (!isLoggedIn && isOnMainScreen) {
            navController.navigate(AppScreen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            syncViewModel.startConnectivityMonitoring()
        }
    }

    Scaffold(
        bottomBar = { if (showBottomBar) BottomNavBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = when {
                isFirstLaunch -> AppScreen.Welcome.route
                !isLoggedIn -> AppScreen.Login.route
                else -> AppScreen.Map.route
            },
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            composable(AppScreen.Welcome.route) {
                FirstLaunchScreen(
                    onContinue = { navController.navigate(AppScreen.Login.route) }
                )
            }
            composable(AppScreen.Login.route) { LoginScreen(navController) }
            composable(AppScreen.Register.route) { RegisterScreen(navController) }

            // MapScreen z opcjonalnym mapId i startRun
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
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(AppScreen.EditPassword.route) {
                EditPasswordScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}