package com.example.crowdmeasure.presentation.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.crowdmeasure.presentation.screens.history.HistoryScreen
import com.example.crowdmeasure.presentation.screens.history.MeasurementDetailScreen
import com.example.crowdmeasure.presentation.screens.home.HomeScreen
import com.example.crowdmeasure.presentation.screens.settings.SettingsScreen

@Composable
fun AppNav() {
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = Destinations.Home
    ) {
        composable(Destinations.Home) {
            AppScaffold(nav = nav, title = "CrowdMeasure", showBottomBar = true) { padding ->
                HomeScreen(
                    contentPadding = padding,
                    onOpenDetail = { nav.navigate(Destinations.detail(it)) }
                )
            }
        }

        composable(Destinations.History) {
            AppScaffold(nav = nav, title = "History", showBottomBar = true) { padding ->
                HistoryScreen(
                    contentPadding = padding,
                    onOpenDetail = { nav.navigate(Destinations.detail(it)) }
                )
            }
        }

        composable(Destinations.Settings) {
            AppScaffold(nav = nav, title = "Settings", showBottomBar = true) { padding ->
                SettingsScreen(contentPadding = padding)
            }
        }

        composable(
            route = Destinations.Detail,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStack ->
            AppScaffold(nav = nav, title = "Measurement detail", showBottomBar = false) { padding ->
                MeasurementDetailScreen(
                    id = backStack.arguments?.getString("id").orEmpty(),
                    contentPadding = padding,
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}