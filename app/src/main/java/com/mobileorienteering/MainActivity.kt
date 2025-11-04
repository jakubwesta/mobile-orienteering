package com.mobileorienteering

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.mobileorienteering.ui.component.AppScaffold
import com.mobileorienteering.ui.screen.auth.AuthViewModel
import com.mobileorienteering.ui.screen.main.SettingsViewModel
import com.mobileorienteering.ui.screen.welcome.FirstLaunchViewModel
import com.mobileorienteering.ui.theme.MobileOrienteeringTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val firstLaunchViewModel: FirstLaunchViewModel = hiltViewModel()
            val authViewModel: AuthViewModel = hiltViewModel()

            val settings by settingsViewModel.settings.collectAsState()
            val isFirstLaunch by firstLaunchViewModel.isFirstLaunch.collectAsState()
            val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

            val navController = rememberNavController()

            MobileOrienteeringTheme(darkTheme = settings.darkMode) {
                AppScaffold(navController, isFirstLaunch, isLoggedIn)
            }
        }
    }
}




