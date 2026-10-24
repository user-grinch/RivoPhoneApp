package com.grinch.rivo4.view.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.grinch.rivo4.R
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.view.components.LocalScrollToTopBottomPadding
import com.grinch.rivo4.view.components.LocalShowFloatingNavBar
import com.grinch.rivo4.view.screen.transitions.LocalTransitionStyle
import com.grinch.rivo4.view.screen.transitions.LocalTransitionTimeScale
import com.grinch.rivo4.view.screen.transitions.SmoothDecelerateEasing
import com.grinch.rivo4.view.theme.RivoEasing
import kotlin.math.roundToInt
import androidx.navigation.NavController
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.ads.RewardedAdManager
import com.grinch.rivo4.view.components.ad.IS_ADS_SUPPORTED
import com.grinch.rivo4.view.components.BottomBar
import com.grinch.rivo4.view.components.TopBar
import com.grinch.rivo4.view.screen.transitions.NoTransitions
import com.grinch.rivo4.view.theme.LocalNavBarStyle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.grinch.rivo4.LocalRequestedTab
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
    val rewardedAdManager = koinInject<RewardedAdManager>()
    val context = LocalContext.current
    val reminderMode = remember(settingsState) { prefs.getVideoAdReminderMode() }
    var showVideoAdFab by remember { mutableStateOf(false) }

    val requestedTabState = LocalRequestedTab.current
    val externalRequestedTab = requestedTabState.value

    var savedTab by rememberSaveable { mutableStateOf<Int?>(null) }

    val visibleTabs = remember(settingsState, externalRequestedTab, initialTab, savedTab) {
        val tabs = prefs.getVisibleBottomNavTabs().toMutableList()
        val targetTab = externalRequestedTab ?: initialTab ?: savedTab
        if (targetTab == PreferenceManager.TAB_CONTACTS && !tabs.contains(PreferenceManager.TAB_CONTACTS)) {
            tabs.add(PreferenceManager.TAB_CONTACTS)
        }
        tabs.toList()
    }
    val defaultTab = remember(settingsState) {
        val startLoc = prefs.getInt(PreferenceManager.KEY_START_LOCATION, PreferenceManager.START_LOCATION_NORMAL)
        when (startLoc) {
            PreferenceManager.START_LOCATION_DIALPAD_RECENTS -> PreferenceManager.TAB_RECENTS
            PreferenceManager.START_LOCATION_DIALPAD_CONTACTS -> PreferenceManager.TAB_CONTACTS
            else -> prefs.getInt(PreferenceManager.KEY_DEFAULT_BOTTOM_NAV, PreferenceManager.TAB_RECENTS)
        }
    }

    val lastActiveTab = savedTab ?: prefs.getLastSelectedTab()
    val requestedTab = externalRequestedTab ?: initialTab ?: lastActiveTab ?: defaultTab

    var currentTabId by rememberSaveable { mutableIntStateOf(requestedTab) }
    val saveableStateHolder = rememberSaveableStateHolder()
    val scope = rememberCoroutineScope()

    var isSelectingRecents by remember { mutableStateOf(false) }
    var recentsActionBar by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
    var isSelectingContacts by remember { mutableStateOf(false) }
    var contactsActionBar by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }

    val currentTab = currentTabId
    val isSelecting = when (currentTab) {
        PreferenceManager.TAB_RECENTS -> isSelectingRecents
        PreferenceManager.TAB_CONTACTS -> isSelectingContacts
        else -> false
    }

    LaunchedEffect(currentTabId) {
        savedTab = currentTabId
        prefs.setLastSelectedTab(currentTabId)
    }

    LaunchedEffect(externalRequestedTab) {
        if (externalRequestedTab != null) {
            if (visibleTabs.contains(externalRequestedTab)) {
                currentTabId = externalRequestedTab
                savedTab = externalRequestedTab
                prefs.setLastSelectedTab(externalRequestedTab)
            }
            requestedTabState.value = null
        }
    }

    LaunchedEffect(initialTab) {
        if (initialTab != null && visibleTabs.contains(initialTab)) {
            currentTabId = initialTab
            savedTab = initialTab
            prefs.setLastSelectedTab(initialTab)
        }
    }

    LaunchedEffect(visibleTabs) {
        if (!visibleTabs.contains(currentTabId)) {
            currentTabId = visibleTabs.firstOrNull() ?: PreferenceManager.TAB_RECENTS
        }
    }

    val navBarStyle = LocalNavBarStyle.current
    val isToolbar = navBarStyle == PreferenceManager.NAV_BAR_STYLE_TOOLBAR
    val isSwipeActionsEnabled = remember(settingsState) { prefs.isSwipeActionsEnabled() }

    var isToolbarVisible by remember { mutableStateOf(true) }

    LaunchedEffect(currentTabId, settingsState) {
        isToolbarVisible = true
        showVideoAdFab = IS_ADS_SUPPORTED && prefs.shouldShowVideoAdReminder()
        if (showVideoAdFab && reminderMode == PreferenceManager.VIDEO_AD_REMINDER_SIX_HOURS) {
            prefs.markVideoAdReminderShown()
        }
    }

    LaunchedEffect(isToolbar) {
        isToolbarVisible = true
    }

    LaunchedEffect(isSelecting) {
        if (isSelecting) {
            isToolbarVisible = true
        }
    }

    val density = LocalDensity.current
    val scrollThresholdPx = remember(density) { with(density) { 12.dp.toPx() } }

    val nestedScrollConnection = remember(isToolbar, scrollThresholdPx) {
        var accumulatedDelta = 0f
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!isToolbar) return Offset.Zero

                val dy = available.y
                val dx = available.x
                if (kotlin.math.abs(dy) > kotlin.math.abs(dx)) {
                    if ((dy > 0 && accumulatedDelta < 0) || (dy < 0 && accumulatedDelta > 0)) {
                        accumulatedDelta = 0f
                    }
                    accumulatedDelta += dy

                    if (accumulatedDelta < -scrollThresholdPx) {
                        if (isToolbarVisible) {
                            isToolbarVisible = false
                        }
                        accumulatedDelta = 0f
                    } else if (accumulatedDelta > scrollThresholdPx) {
                        if (!isToolbarVisible) {
                            isToolbarVisible = true
                        }
                        accumulatedDelta = 0f
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!isToolbar) return Offset.Zero

                if (available.y > 0f) {
                    isToolbarVisible = true
                    accumulatedDelta = 0f
                }
                return Offset.Zero
            }
        }
    }

    val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val targetScrollToTopPadding = if (isToolbar) {
        if (isToolbarVisible) navBarsBottom + 72.dp else navBarsBottom
    } else {
        0.dp
    }
    val animatedScrollToTopPadding by animateDpAsState(
        targetValue = targetScrollToTopPadding,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "scrollToTopBottomPadding"
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.surface,
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
            val transitionStyle = LocalTransitionStyle.current
            val transitionTimeScale = LocalTransitionTimeScale.current
            if (!isToolbar) {
                BottomBar(
                    navController = navController,
                    navigator = navigator,
                    selectedTabId = currentTabId,
                    visibleTabs = visibleTabs,
                    onTabSelected = { tabId ->
                        if (tabId != currentTabId) {
                            currentTabId = tabId
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        val warmTabs = remember { mutableStateListOf(currentTabId) }
        if (!warmTabs.contains(currentTabId)) {
            warmTabs.add(currentTabId)
        }

        // Pre-warm remaining tabs shortly after initial render so clicking any navigation button
        // switches immediately with zero cold-composition delay.
        LaunchedEffect(visibleTabs) {
            kotlinx.coroutines.delay(250L)
            for (tab in visibleTabs) {
                if (!warmTabs.contains(tab)) {
                    warmTabs.add(tab)
                    kotlinx.coroutines.delay(100L)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = if (isToolbar) 0.dp else innerPadding.calculateBottomPadding()
                )
                .nestedScroll(nestedScrollConnection)
        ) {
            CompositionLocalProvider(
                LocalScrollToTopBottomPadding provides animatedScrollToTopPadding,
                LocalShowFloatingNavBar provides { isToolbarVisible = true }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    warmTabs.forEach { tab ->
                        key(tab) {
                            val isActive = tab == currentTabId
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .layout { measurable, constraints ->
                                        val placeable = measurable.measure(constraints)
                                        if (isActive) {
                                            layout(placeable.width, placeable.height) {
                                                placeable.placeRelative(0, 0)
                                            }
                                        } else {
                                            layout(0, 0) {}
                                        }
                                    }
                            ) {
                                saveableStateHolder.SaveableStateProvider(tab) {
                                    when (tab) {
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
                                        PreferenceManager.TAB_RECORDINGS -> com.grinch.rivo4.view.screen.settings.CallRecordingsContent(
                                            showTopBar = false,
                                            initialShowList = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }


            if (isToolbar) {
                AnimatedVisibility(
                    visible = isToolbarVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(animationSpec = tween(150)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeOut(animationSpec = tween(150)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        BottomBar(
                            navController = navController,
                            navigator = navigator,
                            selectedTabId = currentTabId,
                            visibleTabs = visibleTabs,
                            onTabSelected = { tabId ->
                                currentTabId = tabId
                            }
                        )
                    }

                }
            }

            if (showVideoAdFab) {
                SmallFloatingActionButton(
                    onClick = {
                        (context as? Activity)?.let { rewardedAdManager.showAd(it) {} }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = 20.dp,
                            bottom = if (isToolbar) 88.dp else innerPadding.calculateBottomPadding() + 20.dp
                        )
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.support_watch_video),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
