package com.mobileorienteering.ui.component

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mobileorienteering.ui.navigation.AppScreen
import com.mobileorienteering.R

@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    NavigationBar {
        AppScreen.mainScreens.forEach { screen ->
            val selected = currentRoute == screen.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(
                            id = when (screen) {
                                AppScreen.Map -> {
                                    if (selected) R.drawable.ic_map_filled
                                    else R.drawable.ic_map_outlined
                                }
                                AppScreen.Library -> {
                                    if (selected) R.drawable.ic_library_filled
                                    else R.drawable.ic_library_outlined
                                }
                                AppScreen.Runs -> {
                                    if (selected) R.drawable.ic_runs_filled
                                    else R.drawable.ic_runs_outlined
                                }
                                AppScreen.Settings -> {
                                    if (selected) R.drawable.ic_settings_filled
                                    else R.drawable.ic_settings_outlined
                                }
                                else -> R.drawable.ic_map_outlined
                            }
                        ),
                        contentDescription = screen.route
                    )
                },
                label = { Text(screen.route) }
            )
        }
    }
}
