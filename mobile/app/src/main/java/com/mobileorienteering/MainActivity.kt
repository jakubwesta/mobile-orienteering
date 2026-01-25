package com.mobileorienteering

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.mobileorienteering.data.model.domain.AppLanguage
import com.mobileorienteering.ui.core.AppScaffold
import com.mobileorienteering.ui.screens.auth.AuthViewModel
import com.mobileorienteering.ui.screens.settings.SettingsViewModel
import com.mobileorienteering.ui.screens.first_launch.FirstLaunchViewModel
import com.mobileorienteering.ui.theme.AppTheme
import com.mobileorienteering.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.attachBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val firstLaunchViewModel: FirstLaunchViewModel = hiltViewModel()
            val authViewModel: AuthViewModel = hiltViewModel()

            val settings by settingsViewModel.settings.collectAsState()
            val isFirstLaunch by firstLaunchViewModel.isFirstLaunch.collectAsState()
            val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

            var previousLanguage by remember { mutableStateOf<AppLanguage?>(null) }
            LaunchedEffect(settings.language) {
                LocaleHelper.cacheLocaleCode(this@MainActivity, settings.language.localeCode)

                if (previousLanguage != null && previousLanguage != settings.language) {
                    recreate()
                } else {
                    previousLanguage = settings.language
                }
            }

            val navController = rememberNavController()

            AppTheme(
                darkTheme = settings.darkMode,
                contrastLevel = settings.contrastLevel.toTheme()
            ) {
                AppScaffold(
                    navController = navController,
                    isFirstLaunch = isFirstLaunch,
                    isLoggedIn = isLoggedIn
                )
            }
        }
    }
}
