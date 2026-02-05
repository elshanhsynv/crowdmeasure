package com.example.crowdmeasure.presentation.nav

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.crowdmeasure.presentation.nav.components.AppBottomBar
import com.example.crowdmeasure.presentation.nav.components.AppTopBar

/**
 * Main application scaffold that provides consistent chrome (top bar, bottom bar) across all screens.
 *
 * Architecture:
 * - Chrome is determined by [ChromeResolver] based on current destination
 * - Uses [derivedStateOf] to minimize recomposition when chrome doesn't change
 * - Supports edge-to-edge layouts with proper window insets handling
 * - Centralized chrome management avoids duplication across screens
 *
 * Performance:
 * - Chrome calculation is memoized via derivedStateOf
 * - ChromeConfig is @Immutable so comparison is cheap
 * - Navigation callback is stable (passed directly to AppBottomBar)
 *
 * @param navController Navigation controller for the app
 * @param modifier Modifier for the scaffold
 * @param badgeCounts Optional badge counts for bottom nav items
 * @param content The screen content, receiving padding values
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShellScaffold(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    badgeCounts: Map<String, Int> = emptyMap(),
    content: @Composable (PaddingValues) -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    // Derive chrome config - only recalculates when destination changes
    val chrome by remember {
        derivedStateOf { ChromeResolver.resolve(destination) }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (chrome.showTopBar) {
                AppTopBar(
                    title = chrome.title,
                    showBackButton = chrome.showBackButton,
                    onBackClick = {
                        // Handle back press
                        if (!navController.popBackStack()) {
                            // If can't pop, we're at root - could exit app or do nothing
                        }
                    },
                    elevated = chrome.topBarElevated,
                )
            }
        },
        bottomBar = {
            if (chrome.showBottomBar) {
                AppBottomBar(
                    navController = navController,
                    badgeCounts = badgeCounts,
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        content = { padding ->
            content(padding)
        }
    )
}

/**
 * Alternative: AppShellScaffold with scroll behavior support for collapsing top bars.
 * Use this version if you want the top bar to hide/show on scroll in list screens.
 */
@Stable
class AppShellState {
    // Future: could hold scroll behavior, snackbar host state, etc.
}

@Composable
fun rememberAppShellState(): AppShellState {
    return remember { AppShellState() }
}

@Preview
@Composable
private fun AppShellScaffoldPreview() {
    AppShellScaffold(
        navController = NavHostController(LocalContext.current),
        content = {PaddingValues(5.dp)}
    )
}