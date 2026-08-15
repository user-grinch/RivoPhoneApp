package com.grinch.rivo4.view.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.view.components.BottomBar
import com.grinch.rivo4.view.components.TopBar
import com.grinch.rivo4.view.screen.transitions.NoTransitions
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Destination<RootGraph>(start = true, style = NoTransitions::class)
@Composable
fun MainScreen(
    navController: NavController,
    navigator: DestinationsNavigator,
    initialTab: Int? = null
) {
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()

    val visibleTabs = remember(settingsState) { prefs.getVisibleBottomNavTabs() }
    val defaultTab = remember(settingsState) {
        prefs.getInt(PreferenceManager.KEY_DEFAULT_BOTTOM_NAV, PreferenceManager.TAB_RECENTS)
    }

    val requestedTab = initialTab ?: defaultTab
    val startPage = visibleTabs.indexOf(requestedTab).coerceAtLeast(0)

    val pagerState = rememberPagerState(initialPage = startPage) { visibleTabs.size }
    val scope = rememberCoroutineScope()

    var isSelectingRecents by remember { mutableStateOf(false) }
    var recentsActionBar by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
    var isSelectingContacts by remember { mutableStateOf(false) }
    var contactsActionBar by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }

    val currentTab = visibleTabs.getOrNull(pagerState.currentPage)
    val isSelecting = when (currentTab) {
        PreferenceManager.TAB_RECENTS -> isSelectingRecents
        PreferenceManager.TAB_CONTACTS -> isSelectingContacts
        else -> false
    }

    LaunchedEffect(initialTab, visibleTabs) {
        val target = visibleTabs.indexOf(requestedTab)
        if (initialTab != null && target >= 0 && pagerState.currentPage != target) {
            pagerState.scrollToPage(target)
        } else if (pagerState.currentPage > visibleTabs.lastIndex) {
            pagerState.scrollToPage(visibleTabs.lastIndex.coerceAtLeast(0))
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AnimatedContent(
                targetState = isSelecting,
                transitionSpec = {
                    (fadeIn() + expandVertically()) togetherWith (fadeOut() + shrinkVertically())
                },
                label = "MainTopBarTransition"
            ) { selecting ->
                if (selecting) {
                    when (currentTab) {
                        PreferenceManager.TAB_RECENTS -> recentsActionBar?.invoke()
                        PreferenceManager.TAB_CONTACTS -> contactsActionBar?.invoke()
                        else -> TopBar(navController, navigator)
                    }
                } else {
                    TopBar(navController, navigator)
                }
            }
        },
        bottomBar = {
            BottomBar(
                navController = navController,
                navigator = navigator,
                pagerState = pagerState,
                visibleTabs = visibleTabs,
                onPageSelected = { page ->
                    scope.launch {
                        pagerState.animateScrollToPage(page)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                when (visibleTabs.getOrNull(page)) {
                    PreferenceManager.TAB_RECENTS -> RecentScreenContent(
                        navController = navController,
                        navigator = navigator,
                        onSelectionStateChange = { selecting, actionBar ->
                            isSelectingRecents = selecting
                            recentsActionBar = actionBar
                        }
                    )
                    PreferenceManager.TAB_FAVORITES -> FavoritesScreenContent(
                        navController = navController,
                        navigator = navigator,
                        showTopBar = false
                    )
                    PreferenceManager.TAB_CONTACTS -> ContactScreenContent(
                        navController = navController,
                        navigator = navigator,
                        onSelectionStateChange = { selecting, actionBar ->
                            isSelectingContacts = selecting
                            contactsActionBar = actionBar
                        }
                    )
                }
            }
        }
    }
}
