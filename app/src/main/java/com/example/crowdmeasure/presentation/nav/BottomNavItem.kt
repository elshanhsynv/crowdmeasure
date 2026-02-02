package com.example.crowdmeasure.presentation.nav

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

object BottomNavItems {
    val items = listOf(
        BottomNavItem(Destinations.Home, "Home", Icons.Filled.Home),
        BottomNavItem(Destinations.History, "History", Icons.Filled.History),
        BottomNavItem(Destinations.Settings, "Settings", Icons.Filled.Settings),
    )
}