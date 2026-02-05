package com.example.crowdmeasure.presentation.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.crowdmeasure.presentation.screens.history.HistoryScreen
import com.example.crowdmeasure.presentation.screens.history.MeasurementDetailScreen
import com.example.crowdmeasure.presentation.screens.home.HomeScreen
import com.example.crowdmeasure.presentation.screens.settings.SettingsScreen

/**
 * Main navigation graph for the application.
 */
@Composable
fun AppNav() {
    val navController = rememberNavController()

    val topLevelRoutes = setOf(Routes.HOME, Routes.HISTORY, Routes.SETTINGS)

    AppShellScaffold(
        navController = navController,
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
        ) {

            val tabEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
                NavigationConfig.crossfadeTransition()
            }

            val tabExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
                if (targetState.destination.route in topLevelRoutes) {
                    NavigationConfig.crossfadeExit()
                } else {
                    with(NavigationConfig) { exitTransition() }
                }
            }

            val tabPopEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
                if (initialState.destination.route in topLevelRoutes) {
                    NavigationConfig.crossfadeTransition()
                } else {
                    with(NavigationConfig) { popEnterTransition() }
                }
            }

            val tabPopExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
                NavigationConfig.crossfadeExit()
            }

            composable(
                route = Routes.HOME,
                enterTransition = tabEnterTransition,
                exitTransition = tabExitTransition,
                popEnterTransition = tabPopEnterTransition,
                popExitTransition = tabPopExitTransition
            ) {
                HomeScreen(
                    contentPadding = paddingValues,
                    onNavigateToDetail = { id ->
                        navController.navigateToDetail(Routes.detail(id))
                    },
                )
            }

            composable(
                route = Routes.HISTORY,
                enterTransition = tabEnterTransition,
                exitTransition = tabExitTransition,
                popEnterTransition = tabPopEnterTransition,
                popExitTransition = tabPopExitTransition
            ) {
                HistoryScreen(
                    contentPadding = paddingValues,
                    onNavigateToDetail = { id ->
                        navController.navigateToDetail(Routes.detail(id))
                    }
                )
            }

            composable(
                route = Routes.SETTINGS,
                enterTransition = tabEnterTransition,
                exitTransition = tabExitTransition,
                popEnterTransition = tabPopEnterTransition,
                popExitTransition = tabPopExitTransition
            ) {
                SettingsScreen(
                    contentPadding = paddingValues
                )
            }

            composable(
                route = Routes.DETAIL_PATTERN,
                arguments = listOf(
                    navArgument(Routes.DETAIL_ARG_ID) {
                        type = NavType.StringType
                    }
                ),
                enterTransition = {
                    with(NavigationConfig) { enterTransition() }
                },
                exitTransition = {
                    with(NavigationConfig) { exitTransition() }
                },
                popEnterTransition = {
                    with(NavigationConfig) { popEnterTransition() }
                },
                popExitTransition = {
                    with(NavigationConfig) { popExitTransition() }
                },
            ) { backStackEntry ->
                val measurementId = backStackEntry.arguments
                    ?.getString(Routes.DETAIL_ARG_ID)
                    ?: run {
                        navController.popBackStack()
                        return@composable
                    }

                MeasurementDetailScreen(
                    id = measurementId,
                    contentPadding = paddingValues,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Future: add more destinations here (e.g., settings sub-screens, onboarding)
        }
    }
}