package com.dewijones.linguasupra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dewijones.linguasupra.ui.home.HomeScreen
import com.dewijones.linguasupra.ui.permissions.PermissionGate
import com.dewijones.linguasupra.ui.settings.SettingsScreen
import com.dewijones.linguasupra.ui.stats.StatsScreen
import com.dewijones.linguasupra.ui.theme.LinguaSupraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LinguaSupraTheme {
                PermissionGate { AppNav() }
            }
        }
    }
}

private const val ROUTE_HOME = "home"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_STATS = "stats"

@Composable
private fun AppNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = ROUTE_HOME) {
        composable(ROUTE_HOME) {
            HomeScreen(
                onOpenSettings = { nav.navigate(ROUTE_SETTINGS) },
                onOpenStats = { nav.navigate(ROUTE_STATS) },
            )
        }
        composable(ROUTE_SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
        composable(ROUTE_STATS) {
            StatsScreen(onBack = { nav.popBackStack() })
        }
    }
}
