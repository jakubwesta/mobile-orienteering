package com.mobileorienteering.ui.core

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobileorienteering.ui.core.components.BottomNavBar
import com.mobileorienteering.ui.core.snackbar.LocalSnackbarController
import com.mobileorienteering.ui.core.snackbar.rememberSnackbarController
import com.mobileorienteering.ui.screens.settings.SyncViewModel
import com.mobileorienteering.ui.screens.first_launch.FirstLaunchViewModel
import com.mobileorienteering.ui.core.snackbar.SnackbarEvent
import com.mobileorienteering.ui.core.snackbar.SnackbarViewModel

@Composable
fun AppScaffold(
    navController: NavHostController,
    isFirstLaunch: Boolean?,
    isLoggedIn: Boolean?
) {
    if (isFirstLaunch == null || isLoggedIn == null) return

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomBar = currentRoute.shouldShowBottomBar() || (currentRoute == null && isLoggedIn)

    val syncViewModel: SyncViewModel = hiltViewModel()
    val firstLaunchViewModel: FirstLaunchViewModel = hiltViewModel()

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarController = rememberSnackbarController(snackbarHostState)
    val snackbarManager = hiltViewModel<SnackbarViewModel>().snackbarManager

    // Listen to ViewModel snackbar events
    LaunchedEffect(Unit) {
        snackbarManager.messages.collect { event ->
            when (event) {
                is SnackbarEvent.ShowSnackbar -> {
                    snackbarController.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel,
                        duration = event.duration,
                        onAction = event.onAction
                    )
                }
                is SnackbarEvent.ShowError -> {
                    snackbarController.showErrorSnackbar(event.message)
                }
                is SnackbarEvent.ShowSuccess -> {
                    snackbarController.showSuccessSnackbar(event.message)
                }
            }
        }
    }

    // Handle authentication state changes
    HandleAuthState(
        isLoggedIn = isLoggedIn,
        currentRoute = currentRoute,
        navController = navController,
        onLoggedIn = { syncViewModel.startConnectivityMonitoring() }
    )

    // Provide snackbar controller to all children
    CompositionLocalProvider(LocalSnackbarController provides snackbarController) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showBottomBar) {
                    BottomNavBar(navController)
                }
            }
        ) { padding ->
            AppNavGraph(
                navController = navController,
                startDestination = determineStartDestination(isFirstLaunch, isLoggedIn),
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
                onFirstLaunchComplete = { firstLaunchViewModel.markAsSeen() }
            )
        }
    }
}

@Composable
private fun HandleAuthState(
    isLoggedIn: Boolean,
    currentRoute: String?,
    navController: NavHostController,
    onLoggedIn: () -> Unit
) {
    LaunchedEffect(isLoggedIn, currentRoute) {
        when {
            !isLoggedIn && currentRoute.isMainScreen() -> {
                navController.navigate(AppScreen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            isLoggedIn -> {
                onLoggedIn()
            }
        }
    }
}

private fun String?.shouldShowBottomBar(): Boolean {
    if (this == null) return false
    return AppScreen.mainScreens.any { screen ->
        this.startsWith(screen.route)
    }
}

private fun String?.isMainScreen(): Boolean {
    if (this == null) return false
    return AppScreen.mainScreens.any { screen ->
        this.startsWith(screen.route)
    }
}

private fun determineStartDestination(
    isFirstLaunch: Boolean,
    isLoggedIn: Boolean
): String {
    return when {
        isFirstLaunch -> AppScreen.Welcome.route
        !isLoggedIn -> AppScreen.Login.route
        else -> AppScreen.Library.route
    }
}
