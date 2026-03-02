package com.example.crowdmeasure.presentation.nav

import androidx.navigation.NavDestination

object ChromeResolver {
    fun resolve(destination: NavDestination?): ChromeConfig {
        val route = destination?.route ?: return defaultChrome()

        return when {
            // Top-level destinations with bottom bar
            route == Routes.HOME -> ChromeConfig(
                title = "CrowdMeasure",
                showTopBar = true,
                showBottomBar = true,
                showBackButton = false,
                topBarElevated = false
            )

            route == Routes.HISTORY -> ChromeConfig(
                title = "History",
                showTopBar = true,
                showBottomBar = true,
                showBackButton = false,
                topBarElevated = false
            )

            route == Routes.SETTINGS -> ChromeConfig(
                title = "Settings",
                showTopBar = true,
                showBottomBar = true,
                showBackButton = false,
                topBarElevated = false
            )

            // Detail screen (pattern match, not specific instance)
            route == Routes.DETAIL_PATTERN -> ChromeConfig(
                title = "Measurement Details",
                showTopBar = true,
                showBottomBar = false,
                showBackButton = true,
                topBarElevated = true
            )

            // Future: handle nested graphs by checking route.startsWith(...)

            else -> defaultChrome()
        }
    }

    /**
     * Checks if the destination is a top-level screen with bottom navigation.
     */
    fun isTopLevel(destination: NavDestination?): Boolean {
        val route = destination?.route ?: return false
        return MainTab.fromRoute(route) != null
    }

    private fun defaultChrome() = ChromeConfig(
        title = "CrowdMeasure",
        showTopBar = true,
        showBottomBar = true,
        showBackButton = true,
        topBarElevated = false
    )
}