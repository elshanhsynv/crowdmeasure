package com.example.crowdmeasure.presentation.nav

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShellScaffold(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    badgeCounts: Map<String, Int> = emptyMap(),
    topBarActions: @Composable RowScope.(String?) -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val chrome = ChromeResolver.resolve(destination)

    val activity = LocalActivity.current
    var showExitDialog by remember { mutableStateOf(false) }

    val isAtRoot = destination?.route == Routes.HOME

    BackHandler(enabled = isAtRoot) {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(text = "Exit App") },
            text = { Text(text = "Are you sure you want to exit?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        activity?.finish()
                    }
                ) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (chrome.showTopBar) {
                AppTopBar(
                    title = chrome.title,
                    showBackButton = chrome.showBackButton,
                    onBackClick = {
                        if (!navController.popBackStack()) {
                            showExitDialog = true
                        }
                    },
                    elevated = chrome.topBarElevated,
                    actions = {
                        topBarActions(destination?.route)
                    }
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
            content(PaddingValues(top=padding.calculateTopPadding()))
        }
    )
}

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
        content = { PaddingValues(5.dp) }
    )
}
