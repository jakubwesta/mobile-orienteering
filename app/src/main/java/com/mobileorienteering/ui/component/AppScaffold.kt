package com.mobileorienteering.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mobileorienteering.ui.screen.auth.LoginScreen
import com.mobileorienteering.ui.screen.auth.RegisterScreen
import com.mobileorienteering.ui.screen.main.LibraryScreen
import com.mobileorienteering.ui.screen.main.MapScreen
import com.mobileorienteering.ui.screen.main.RunsScreen
import com.mobileorienteering.ui.screen.main.SettingsScreen
import com.mobileorienteering.ui.screen.welcome.FirstLaunchScreen
import com.mobileorienteering.ui.navigation.AppScreen

@Composable
fun AppScaffold(
    navController: NavHostController,
    isFirstLaunch: Boolean,
    isLoggedIn: Boolean
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    val showBottomBar = currentRoute in AppScreen.mainScreens.map { it.route }

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
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
