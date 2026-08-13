package com.grinch.rivo4.view.components

import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
    val icon: ImageVector,
    val value: Int
)

@Composable
fun navigationTabLabel(tabId: Int): String = when (tabId) {
    PreferenceManager.TAB_FAVORITES -> stringResource(R.string.nav_favorites)
    PreferenceManager.TAB_CONTACTS -> stringResource(R.string.nav_contacts)
    else -> stringResource(R.string.nav_recents)
}

fun navigationTabIcon(tabId: Int): ImageVector = when (tabId) {
    PreferenceManager.TAB_FAVORITES -> Icons.Filled.Star
    PreferenceManager.TAB_CONTACTS -> Icons.Filled.Person
    else -> Icons.Filled.History
}

fun navigationTabRoute(tabId: Int): String = when (tabId) {
    PreferenceManager.TAB_FAVORITES -> FavoritesScreenDestination.route
    PreferenceManager.TAB_CONTACTS -> ContactScreenDestination.route
    else -> RecentScreenDestination.route
}

private fun navigationTabUnselectedIcon(tabId: Int): ImageVector = when (tabId) {
    PreferenceManager.TAB_FAVORITES -> Icons.Outlined.Star
    PreferenceManager.TAB_CONTACTS -> Icons.Outlined.Person
    else -> Icons.Outlined.History
}

@Composable
fun BottomBar(
    navController: NavController,
    navigator: DestinationsNavigator,
    pagerState: PagerState? = null,
    onPageSelected: ((Int) -> Unit)? = null,
    visibleTabs: List<Int>? = null
) {
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()

    val iconOnly = remember(settingsState) {
        prefs.getBoolean(PreferenceManager.KEY_ICON_ONLY_NAV, false)
    }
    val storedTabs = remember(settingsState) { prefs.getVisibleBottomNavTabs() }
    val tabIds = visibleTabs ?: storedTabs

    val tabs = tabIds.mapIndexed { index, id ->
        NavigationTab(
            id = id,
            route = navigationTabRoute(id),
            label = navigationTabLabel(id),
            icon = navigationTabIcon(id),
            value = index
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val itemColors = ShortNavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onSurface,
        selectedIndicatorColor = MaterialTheme.colorScheme.secondaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    ShortNavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        tabs.forEach { tab ->
            val isSelected = if (pagerState != null) {
                pagerState.currentPage == tab.value
            } else {
                currentDestination?.hierarchy?.any { it.route == tab.route } == true
            }

            ShortNavigationBarItem(
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
                },
                icon = {
                    Icon(
                        imageVector = if (isSelected) tab.icon else navigationTabUnselectedIcon(tab.id),
                        contentDescription = if (iconOnly) tab.label else null
                    )
                },
                label = if (iconOnly) null else ({
                    Text(
                        text = tab.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }),
                colors = itemColors
            )
        }
    }
}
