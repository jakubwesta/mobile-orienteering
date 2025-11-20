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
import com.mobileorienteering.ui.screen.main.*
import com.mobileorienteering.ui.screen.main.map.MapScreen
import com.mobileorienteering.ui.screen.welcome.FirstLaunchScreen
@Composable
fun AppScaffold(
    navController: NavHostController,
    isFirstLaunch: Boolean,
    isLoggedIn: Boolean
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomBar = currentRoute in AppScreen.mainScreens.map { it.route }

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn && currentRoute in AppScreen.mainScreens.map { it.route }) {
            navController.navigate(AppScreen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
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
            modifier = Modifier.padding(padding)
        ) {
            composable(AppScreen.Welcome.route) {
                FirstLaunchScreen(
                    onContinue = { navController.navigate(AppScreen.Login.route) }
                )
            }
            composable(AppScreen.Login.route) { LoginScreen(navController) }
            composable(AppScreen.Register.route) { RegisterScreen(navController) }

            composable(AppScreen.Map.route) { MapScreen() }
            composable(AppScreen.Library.route) { LibraryScreen() }
            composable(AppScreen.Runs.route) { RunsScreen() }
            composable(AppScreen.Settings.route) { SettingsScreen() }
        }
    }
}
