package com.example.crowdmeasure.presentation.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.crowdmeasure.presentation.screens.callsampling.CallSessionsScreen
import com.example.crowdmeasure.presentation.screens.history.HistoryScreen
import com.example.crowdmeasure.presentation.screens.history.HistoryTopBarActions
import com.example.crowdmeasure.presentation.screens.history.HistoryViewModel
import com.example.crowdmeasure.presentation.screens.history.MeasurementDetailScreen
import com.example.crowdmeasure.presentation.screens.history.MeasurementDetailViewModel
import com.example.crowdmeasure.presentation.screens.home.HomeScreen
import com.example.crowdmeasure.presentation.screens.home.HomeViewModel
import com.example.crowdmeasure.presentation.screens.settings.SettingsScreen
import com.example.crowdmeasure.presentation.screens.settings.SettingsViewModel

/**
 * Main navigation graph for the application.
 */
@Composable
fun AppNav() {
    val navController = rememberNavController()

    val topLevelRoutes = setOf(Routes.HOME, Routes.HISTORY, Routes.SETTINGS)

    val homeViewModel = hiltViewModel<HomeViewModel>()
    val settingsViewModel = hiltViewModel<SettingsViewModel>()
    val measurementDetailViewModel = hiltViewModel<MeasurementDetailViewModel>()
    val historyViewModel = hiltViewModel<HistoryViewModel>()
    val historyUiState by historyViewModel.uiState.collectAsStateWithLifecycle()
    var historySearchVisible by rememberSaveable { mutableStateOf(false) }

    AppShellScaffold(
        navController = navController,
        topBarActions = { route ->
            if (route == Routes.HISTORY) {
                HistoryTopBarActions(
                    state = historyUiState,
                    searchVisible = historySearchVisible,
                    onToggleSearch = { historySearchVisible = !historySearchVisible },
                    onFilterSelected = historyViewModel::setTransportFilter
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
        ) {

            val tabEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
                {
                    NavigationConfig.crossfadeEnterTransition
                }

            val tabExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
                {
                    if (targetState.destination.route in topLevelRoutes) {
                        NavigationConfig.crossfadeExitTransition
                    } else {
                        with(NavigationConfig) { exitTransition }
                    }
                }

            val tabPopEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
                {
                    if (initialState.destination.route in topLevelRoutes) {
                        NavigationConfig.crossfadeEnterTransition
                    } else {
                        with(NavigationConfig) { popEnterTransition }
                    }
                }

            val tabPopExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
                {
                    NavigationConfig.crossfadeExitTransition
                }

            composable(
                route = Routes.HOME,
                enterTransition = tabEnterTransition,
                exitTransition = tabExitTransition,
                popEnterTransition = tabPopEnterTransition,
                popExitTransition = tabPopExitTransition
            ) {
                HomeScreen(
                    viewModel = homeViewModel,
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
                    viewModel = historyViewModel,
                    contentPadding = paddingValues,
                    searchVisible = historySearchVisible,
                    onNavigateToNewMeasurement = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToDetail = { id ->
                        navController.navigateToDetail(Routes.detail(id))
                    },
                    onNavigateToCallSessions = {
                        navController.navigate(Routes.CALL_SESSIONS)
                    },
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
                    viewModel = settingsViewModel,
                    contentPadding = paddingValues
                )
            }

            composable(
                route = Routes.CALL_SESSIONS,
                enterTransition = {
                    with(NavigationConfig) { enterTransition }
                },
                exitTransition = {
                    with(NavigationConfig) { exitTransition }
                },
                popEnterTransition = {
                    with(NavigationConfig) { popEnterTransition }
                },
                popExitTransition = {
                    with(NavigationConfig) { popExitTransition }
                },
            ) {
                CallSessionsScreen(contentPadding = paddingValues)
            }

            composable(
                route = Routes.DETAIL_PATTERN,
                arguments = listOf(
                    navArgument(Routes.DETAIL_ARG_ID) {
                        type = NavType.StringType
                    }
                ),
                enterTransition = {
                    with(NavigationConfig) { enterTransition }
                },
                exitTransition = {
                    with(NavigationConfig) { exitTransition }
                },
                popEnterTransition = {
                    with(NavigationConfig) { popEnterTransition }
                },
                popExitTransition = {
                    with(NavigationConfig) { popExitTransition }
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
                    viewModel = measurementDetailViewModel,
                    contentPadding = paddingValues,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
