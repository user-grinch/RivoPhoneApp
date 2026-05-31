package com.grinch.rivo4.view.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.grinch.rivo4.controller.util.PreferenceManager
import com.ramcosta.composedestinations.generated.destinations.ContactScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RecentScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

data class NavigationTab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val value: Int
)

@Composable
fun BottomBar(navController: NavController, navigator: DestinationsNavigator) {
    val prefs = koinInject<PreferenceManager>()

    val flipBar = prefs.getBoolean(PreferenceManager.KEY_FLIP_BOTTOM_NAV, false)
    val iconOnly = prefs.getBoolean(PreferenceManager.KEY_ICON_ONLY_NAV, false)

    val tabs = listOf(
        NavigationTab(ContactScreenDestination.route, "Contacts", Icons.Default.Person, 0),
        NavigationTab(RecentScreenDestination.route, "Recents", Icons.Default.History, 1)
    )

    val organizedTabs = if (flipBar) tabs.reversed() else tabs

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        organizedTabs.forEach { tab ->
            NavigationBarItem(
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = if (iconOnly) null else ({ Text(tab.label) }),
                alwaysShowLabel = !iconOnly,
                selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}