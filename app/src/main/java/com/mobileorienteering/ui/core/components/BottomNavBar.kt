package com.mobileorienteering.ui.core.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mobileorienteering.ui.core.AppScreen

@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    NavigationBar(
        modifier = Modifier
            .height(120.dp)
            .fillMaxWidth()
    ) {
        AppScreen.mainScreens.forEach { screen ->
            val selected = currentRoute?.startsWith(screen.route) == true

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
                            id = if (selected)
                                screen.iconFilled!!
                            else
                                screen.iconOutlined!!
                        ),
                        contentDescription = screen.label,
                        modifier = Modifier
                            .size(26.dp)
                    )
                },
                label = {
                    Text(
                        text = screen.label ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            )
        }
    }
}
