package com.mobileorienteering

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.mobileorienteering.ui.component.OrienteeringScaffold
import com.mobileorienteering.ui.theme.MobileOrienteeringTheme
import com.mobileorienteering.ui.screen.main.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val darkMode by settingsViewModel.darkMode.collectAsState()

            MobileOrienteeringTheme(darkTheme = darkMode) {
                Surface {
                    val navController = rememberNavController()
                    OrienteeringScaffold(navController, true)
                }
            }
        }
    }
}
