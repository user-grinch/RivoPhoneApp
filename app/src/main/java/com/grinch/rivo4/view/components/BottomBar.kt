package com.grinch.rivo4.view.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import com.ramcosta.composedestinations.generated.destinations.ContactScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FavoritesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RecentScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

data class NavigationTab(
    val id: Int,
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val value: Int
)

@Composable
fun navigationTabLabel(tabId: Int): String = when (tabId) {
    PreferenceManager.TAB_FAVORITES -> stringResource(R.string.nav_favorites)
    PreferenceManager.TAB_CONTACTS -> stringResource(R.string.nav_contacts)
    else -> stringResource(R.string.nav_recents)
}

fun navigationTabIcon(tabId: Int): androidx.compose.ui.graphics.vector.ImageVector = when (tabId) {
    PreferenceManager.TAB_FAVORITES -> Icons.Default.Star
    PreferenceManager.TAB_CONTACTS -> Icons.Default.Person
    else -> Icons.Default.History
}

fun navigationTabRoute(tabId: Int): String = when (tabId) {
    PreferenceManager.TAB_FAVORITES -> FavoritesScreenDestination.route
    PreferenceManager.TAB_CONTACTS -> ContactScreenDestination.route
    else -> RecentScreenDestination.route
}

@Composable
fun BottomBar(
    navController: NavController,
    navigator: DestinationsNavigator,
    pagerState: androidx.compose.foundation.pager.PagerState? = null,
    onPageSelected: ((Int) -> Unit)? = null,
    visibleTabs: List<Int>? = null
) {
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()

    val iconOnly = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_ICON_ONLY_NAV, false) }
    val tabIds = visibleTabs ?: remember(settingsState) { prefs.getVisibleBottomNavTabs() }

    val tabs = tabIds.mapIndexed { index, id ->
        NavigationTab(
            id = id,
            route = navigationTabRoute(id),
            label = navigationTabLabel(id),
            icon = navigationTabIcon(id),
            value = index
        )
    }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        tabs.forEach { tab ->
            val isSelected = if (pagerState != null) {
                pagerState.currentPage == tab.value
            } else {
                currentDestination?.hierarchy?.any { it.route == tab.route } == true
            }

            NavigationBarItem(
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = if (iconOnly) null else ({ Text(tab.label) }),
                alwaysShowLabel = !iconOnly,
                selected = isSelected,
                onClick = {
                    if (onPageSelected != null && pagerState != null) {
                        onPageSelected(tab.value)
                    } else {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
