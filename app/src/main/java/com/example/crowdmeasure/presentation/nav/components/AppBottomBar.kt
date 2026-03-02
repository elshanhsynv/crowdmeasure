package com.example.crowdmeasure.presentation.nav.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.crowdmeasure.presentation.nav.MainTab

@Composable
fun AppBottomBar(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit = { route ->
        navController.navigate(route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
        }
    },
    badgeCounts: Map<String, Int> = emptyMap(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationBar(
        modifier = modifier,
        tonalElevation = 0.dp,
    ) {
        MainTab.all.forEach { tab ->
            val selected = currentDestination.isInHierarchy(tab.route)
            val badgeCount = badgeCounts[tab.route] ?: 0

            val scale by animateFloatAsState(
                targetValue = if (selected) 1.2f else 1.0f,
                animationSpec = spring(
                    dampingRatio = 0.5f,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "iconScale"
            )

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) onNavigate(tab.route)
                },
                icon = {
                    val iconModifier = Modifier.scale(if (selected) scale else 1.0f)

                    if (badgeCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    // Offset the badge slightly so the icon bounce doesn't clip it awkwardly
                                    modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                                ) {
                                    Text(
                                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                modifier = iconModifier
                            )
                        }
                    } else {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            modifier = iconModifier
                        )
                    }
                },
                label = {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.semantics {
                    contentDescription = tab.contentDescription
                }
            )
        }
    }
}

private fun NavDestination?.isInHierarchy(route: String): Boolean {
    return this?.hierarchy?.any { it.route == route } == true
}

@Preview
@Composable
private fun AppBottomBarPreview() {
    AppBottomBar(
        navController = NavHostController(LocalContext.current),
//        badgeCounts = mapOf(
//            MainTab.HOME.route to 1,
//            MainTab.HISTORY.route to 1,
//        )
    )
}