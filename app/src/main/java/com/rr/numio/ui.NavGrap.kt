package com.rr.numio.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalContext

object Routes {
    const val CALCULATOR = "calculator"
    const val SETTINGS = "settings"
    const val CONVERTER = "converter"
}

@Composable
fun NumioNavGraph() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val themeViewModel: ThemeViewModel = viewModel(
        factory = ThemeViewModel.factory(
            context.applicationContext as android.app.Application
        )
    )
    val accentColor by themeViewModel.accentColor.collectAsState()

    NavHost(navController = navController, startDestination = Routes.CALCULATOR) {
        composable(Routes.CALCULATOR) {
            CalculatorScreen(
                accentColor = accentColor,
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToConverter = { navController.navigate(Routes.CONVERTER) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                accentColor = accentColor,
                onAccentColorChange = { themeViewModel.setAccentColor(it) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.CONVERTER) {
            ConverterScreen(
                accentColor = accentColor,
                onBack = { navController.popBackStack() }
            )
        }
    }
}