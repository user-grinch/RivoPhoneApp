package com.grinch.rivo4.view.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import com.ramcosta.composedestinations.generated.destinations.CallRecordingsScreenDestination
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
    PreferenceManager.TAB_RECORDINGS -> stringResource(R.string.nav_call_recordings)
    else -> stringResource(R.string.nav_recents)
}

fun navigationTabIcon(tabId: Int): ImageVector = when (tabId) {
    PreferenceManager.TAB_FAVORITES -> Icons.Filled.Star
    PreferenceManager.TAB_CONTACTS -> Icons.Filled.Person
    PreferenceManager.TAB_RECORDINGS -> Icons.Filled.Mic
    else -> Icons.Filled.History
}

fun navigationTabRoute(tabId: Int): String = when (tabId) {
    PreferenceManager.TAB_FAVORITES -> FavoritesScreenDestination.route
    PreferenceManager.TAB_CONTACTS -> ContactScreenDestination.route
    PreferenceManager.TAB_RECORDINGS -> CallRecordingsScreenDestination.route
    else -> RecentScreenDestination.route
}

private fun navigationTabUnselectedIcon(tabId: Int): ImageVector = when (tabId) {
    PreferenceManager.TAB_FAVORITES -> Icons.Outlined.Star
    PreferenceManager.TAB_CONTACTS -> Icons.Outlined.Person
    PreferenceManager.TAB_RECORDINGS -> Icons.Outlined.MicNone
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
    val navBarStyle = remember(settingsState) {
        prefs.getInt(PreferenceManager.KEY_NAV_BAR_STYLE, PreferenceManager.NAV_BAR_STYLE_STANDARD)
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

    if (navBarStyle == PreferenceManager.NAV_BAR_STYLE_TOOLBAR) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)),
                modifier = Modifier.wrapContentWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        val isSelected = if (pagerState != null) {
                            pagerState.currentPage == tab.value
                        } else {
                            currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        }

                        val containerColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "toolbarItemBg"
                        )
                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "toolbarItemContent"
                        )

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(containerColor)
                                .clickable {
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
                                .padding(
                                    horizontal = if (isSelected && !iconOnly) 14.dp else 10.dp,
                                    vertical = 8.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) tab.icon else navigationTabUnselectedIcon(tab.id),
                                contentDescription = tab.label,
                                tint = contentColor,
                                modifier = Modifier.size(22.dp)
                            )
                            if (isSelected && !iconOnly) {
                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter = fadeIn() + expandHorizontally(),
                                    exit = fadeOut() + shrinkHorizontally()
                                ) {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = contentColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
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
}
