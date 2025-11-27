package com.mobileorienteering.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mobileorienteering.ui.navigation.AppScreen
import com.mobileorienteering.ui.screen.auth.LoginScreen
import com.mobileorienteering.ui.screen.auth.RegisterScreen
import com.mobileorienteering.ui.screen.main.library.LibraryScreen
import com.mobileorienteering.ui.screen.main.runs.RunsScreen
import com.mobileorienteering.ui.screen.main.settings.SettingsScreen
import com.mobileorienteering.ui.screen.main.map.MapScreen
import com.mobileorienteering.ui.screen.main.map.MapViewModel
import com.mobileorienteering.ui.screen.welcome.FirstLaunchScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
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
    val showBottomBar = currentRoute in AppScreen.mainScreens.map { it.route }

    val mapViewModel: MapViewModel = hiltViewModel()
    val syncViewModel: SyncViewModel = hiltViewModel()

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn && currentRoute in AppScreen.mainScreens.map { it.route }) {
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

            composable(AppScreen.Map.route) {
                MapScreen(viewModel = mapViewModel)
            }

            composable(AppScreen.Library.route) {
                LibraryScreen(
                    onEditMap = { mapId ->
                        mapViewModel.loadMap(mapId)
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