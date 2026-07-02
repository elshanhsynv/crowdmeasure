package com.example.crowdmeasure.presentation.nav

import androidx.navigation.NavDestination

object ChromeResolver {
    fun resolve(destination: NavDestination?): ChromeConfig {
        val route = destination?.route ?: return defaultChrome()

        return when (route) {
            Routes.HOME -> ChromeConfig(
                title = "CrowdMeasure",
                showTopBar = true,
                showBottomBar = true,
                showBackButton = false,
                topBarElevated = false
            )

            Routes.HISTORY -> ChromeConfig(
                title = "CrowdMeasure",
                showTopBar = true,
                showBottomBar = true,
                showBackButton = false,
                topBarElevated = false
            )

            Routes.SETTINGS -> ChromeConfig(
                title = "CrowdMeasure",
                showTopBar = true,
                showBottomBar = true,
                showBackButton = false,
                topBarElevated = false
            )

            Routes.DETAIL_PATTERN -> ChromeConfig(
                title = "Measurement Details",
                showTopBar = true,
                showBottomBar = false,
                showBackButton = true,
                topBarElevated = true
            )

            Routes.CALL_SESSIONS -> ChromeConfig(
                title = "Call Sessions",
                showTopBar = true,
                showBottomBar = false,
                showBackButton = true,
                topBarElevated = true
            )

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
        showBackButton = false,
        topBarElevated = false
    )
}
