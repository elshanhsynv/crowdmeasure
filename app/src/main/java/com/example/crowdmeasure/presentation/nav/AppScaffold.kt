package com.example.crowdmeasure.presentation.nav

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    nav: NavController,
    title: String,
    showBottomBar: Boolean,
    content: @Composable (PaddingValues) -> Unit,
) {
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
            )
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BottomNavItems.items.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    nav.navigate(item.route) {
                                        // keep one copy of each tab
                                        launchSingleTop = true
                                        restoreState = true
                                        popUpTo(Destinations.Home) { saveState = true }
                                    }
                                }
                            },
                            icon = { Icon((item.icon), contentDescription = null) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        },
        content = content
    )
}