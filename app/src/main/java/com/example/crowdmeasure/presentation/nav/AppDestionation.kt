package com.example.crowdmeasure.presentation.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed hierarchy for type-safe navigation destinations.
 * Each destination knows its route, title, and chrome requirements.
 */
sealed interface AppDestination {
    val route: String
    val title: String
}

/**
 * Top-level destinations shown in the bottom navigation bar.
 */
sealed interface TopLevelDestination : AppDestination {
    val icon: ImageVector
    val contentDescription: String
}

/**
 * Centralized route definitions.
 * Use object properties for clarity and autocomplete.
 */
object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    // Detail screen uses path parameter
    const val DETAIL_PATTERN = "detail/{id}"
    const val DETAIL_ARG_ID = "id"

    fun detail(id: String): String = "detail/$id"
}

/**
 * Chrome configuration for a given screen.
 * Immutable to enable safe caching and comparison.
 */
@Immutable
data class ChromeConfig(
    val title: String,
    val showTopBar: Boolean = true,
    val showBottomBar: Boolean = true,
    val showBackButton: Boolean = false,
    val topBarElevated: Boolean = false,
)

/**
 * Top-level navigation tabs.
 */
@Immutable
enum class MainTab(
    override val route: String,
    override val title: String,
    override val icon: ImageVector,
    override val contentDescription: String,
) : TopLevelDestination {
    HOME(
        route = Routes.HOME,
        title = "Home",
        icon = Icons.Filled.Home,
        contentDescription = "Navigate to home screen"
    ),
    HISTORY(
        route = Routes.HISTORY,
        title = "History",
        icon = Icons.Filled.History,
        contentDescription = "View measurement history"
    ),
    SETTINGS(
        route = Routes.SETTINGS,
        title = "Settings",
        icon = Icons.Filled.Settings,
        contentDescription = "Open settings"
    );

    companion object {
        fun fromRoute(route: String?): MainTab? {
            return entries.find { it.route == route }
        }

        val all: List<MainTab> = entries
    }
}