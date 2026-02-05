package com.example.crowdmeasure.presentation.nav

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptionsBuilder

/**
 * Navigate to a top-level destination with proper state management.
 *
 * Behavior:
 * - Single top: prevents duplicate instances of the same destination
 * - Restore state: brings back previously saved state when returning to a tab
 * - Save state: preserves state when navigating away from a tab
 * - Pop to start: ensures clean back stack (only one top-level at a time)
 *
 * This creates the standard Android bottom nav UX:
 * - Switching tabs feels instant (state restored)
 * - Back button from any tab takes you out of the app (clean stack)
 * - Each tab maintains its own navigation state independently
 */
fun NavController.navigateToTopLevel(route: String) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
    }
}

/**
 * Navigate to a detail/secondary screen with standard forward navigation.
 *
 * Use this for screens that should be "on top" of the current screen,
 * creating a proper navigation stack.
 */
fun NavController.navigateToDetail(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}

/**
 * Navigate and clear the entire back stack.
 * Useful for post-login navigation or major flow changes.
 */
fun NavController.navigateAndClearStack(route: String) {
    navigate(route) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}

/**
 * Safe navigation that checks if the route exists before navigating.
 * Useful when dealing with dynamic routes or deep links.
 *
 * @return true if navigation succeeded, false if route doesn't exist
 */
fun NavController.navigateSafely(
    route: String,
    builder: NavOptionsBuilder.() -> Unit = {}
): Boolean {
    return try {
        val destination = graph.findNode(route)
        if (destination != null) {
            navigate(route, builder)
            true
        } else {
            false
        }
    } catch (e: Exception) {
        // Handle navigation exceptions gracefully
        false
    }
}

/**
 * Navigate back with result.
 * Sets a result in the previous back stack entry's saved state.
 *
 * Usage:
 * ```
 * // In detail screen:
 * navController.navigateBackWithResult("selected_item", item)
 *
 * // In calling screen:
 * val result = navController.currentBackStackEntry
 *     ?.savedStateHandle
 *     ?.getLiveData<Item>("selected_item")
 * ```
 */
fun <T> NavController.navigateBackWithResult(key: String, result: T) {
    previousBackStackEntry?.savedStateHandle?.set(key, result)
    popBackStack()
}