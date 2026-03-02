package com.example.crowdmeasure.presentation.nav

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptionsBuilder

/**
 * Navigate to a detail/secondary screen with standard forward navigation.
 */
fun NavController.navigateToDetail(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}