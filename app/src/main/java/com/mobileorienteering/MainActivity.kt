package com.mobileorienteering

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.mobileorienteering.ui.component.OrienteeringScaffold
import com.mobileorienteering.ui.screen.main.SettingsViewModel
import com.mobileorienteering.ui.screen.welcome.FirstLaunchScreen
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

            val settings = settingsViewModel.settings.value
            val isFirstLaunch = firstLaunchViewModel.isFirstLaunch.value

            MobileOrienteeringTheme(darkTheme = settings.darkMode) {
                when (isFirstLaunch) {
                    true -> FirstLaunchScreen(onContinue = { firstLaunchViewModel.markAsSeen() })
                    false -> {
                        val navController = rememberNavController()
                        OrienteeringScaffold(navController, true)
                    }
                }
            }
        }
    }
}





