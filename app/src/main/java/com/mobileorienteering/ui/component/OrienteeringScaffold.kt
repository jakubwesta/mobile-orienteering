package com.mobileorienteering.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mobileorienteering.ui.navigation.AuthNavGraph
import com.mobileorienteering.ui.navigation.MainNavGraph
import com.mobileorienteering.ui.navigation.MainScreen

@Composable
fun OrienteeringScaffold(
    navController: NavHostController,
    isLoggedIn: Boolean
) {
    if (!isLoggedIn) {
        AuthNavGraph(navController)
    } else {
        val navBackStackEntry = navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry.value?.destination?.route

        val screensWithNavBar = MainScreen.items.map { it.route }

        Scaffold(
            bottomBar = {
                if (currentRoute in screensWithNavBar) {
                    BottomNavBar(navController)
                }
            }
        ) { padding ->
            MainNavGraph(navController, Modifier.padding(padding))
        }
    }
}
