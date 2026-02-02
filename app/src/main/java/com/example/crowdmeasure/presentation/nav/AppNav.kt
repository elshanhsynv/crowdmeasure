package com.example.crowdmeasure.presentation.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.crowdmeasure.presentation.screens.history.HistoryScreen
import com.example.crowdmeasure.presentation.screens.history.MeasurementDetailScreen
import com.example.crowdmeasure.presentation.screens.home.HomeScreen
import com.example.crowdmeasure.presentation.screens.onboarding.OnboardingScreen
import com.example.crowdmeasure.presentation.screens.settings.SettingsScreen
import com.example.crowdmeasure.presentation.screens.onboarding.OnboardingViewModel

@Composable
fun AppNav() {
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = Destinations.Onboarding
    ) {
        composable(Destinations.Onboarding) {
            // Use onboarding vm state to auto-forward if already opted in (optional, but nice)
            val vm: OnboardingViewModel = hiltViewModel()
            val settings = vm.settings.collectAsState().value
            val canEnter = settings?.consentAccepted == true

            LaunchedEffect(canEnter) {
                if (canEnter) {
                    nav.navigate(Destinations.Main) {
                        popUpTo(Destinations.Onboarding) { inclusive = true }
                    }
                }
            }

            OnboardingScreen(
                onDone = {
                    nav.navigate(Destinations.Main) {
                        popUpTo(Destinations.Onboarding) { inclusive = true }
                    }
                }
            )
        }

        navigation(
            startDestination = Destinations.Home,
            route = Destinations.Main
        ) {
            composable(Destinations.Home) {
                AppScaffold(
                    nav = nav,
                    title = "CrowdMeasure",
                    showBottomBar = true
                ) { padding ->
                    HomeScreen(
                        contentPadding = padding,
                        onOpenDetail = { nav.navigate(Destinations.detail(it)) }
                    )
                }
            }

            composable(Destinations.History) {
                AppScaffold(
                    nav = nav,
                    title = "History",
                    showBottomBar = true
                ) { padding ->
                    HistoryScreen(
                        contentPadding = padding,
                        onOpenDetail = { nav.navigate(Destinations.detail(it)) }
                    )
                }
            }

            composable(Destinations.Settings) {
                AppScaffold(
                    nav = nav,
                    title = "Settings",
                    showBottomBar = true
                ) { padding ->
                    SettingsScreen(contentPadding = padding)
                }
            }

            composable(
                route = Destinations.Detail,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { backStack ->
                // detail is NOT part of bottom tabs
                AppScaffold(
                    nav = nav,
                    title = "Measurement detail",
                    showBottomBar = false
                ) { padding ->
                    MeasurementDetailScreen(
                        id = backStack.arguments?.getString("id").orEmpty(),
                        contentPadding = padding,
                        onBack = { nav.popBackStack() }
                    )
                }
            }
        }
    }
}