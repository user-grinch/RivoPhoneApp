package com.grinch.rivo4.view.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import androidx.navigation.NavController
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.view.components.BottomBar
import com.grinch.rivo4.view.components.TopBar
import com.grinch.rivo4.view.screen.transitions.NoTransitions
import com.grinch.rivo4.view.theme.LocalNavBarStyle
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
        val startLoc = prefs.getInt(PreferenceManager.KEY_START_LOCATION, PreferenceManager.START_LOCATION_NORMAL)
        when (startLoc) {
            PreferenceManager.START_LOCATION_DIALPAD_RECENTS -> PreferenceManager.TAB_RECENTS
            PreferenceManager.START_LOCATION_DIALPAD_CONTACTS -> PreferenceManager.TAB_CONTACTS
            else -> prefs.getInt(PreferenceManager.KEY_DEFAULT_BOTTOM_NAV, PreferenceManager.TAB_RECENTS)
        }
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

    val navBarStyle = LocalNavBarStyle.current
    val isToolbar = navBarStyle == PreferenceManager.NAV_BAR_STYLE_TOOLBAR
    val isSwipeActionsEnabled = remember(settingsState) { prefs.isSwipeActionsEnabled() }
    val isBlurEnabled = remember(settingsState) { prefs.isFloatingBarBlurEnabled() }

    var isToolbarVisible by remember { mutableStateOf(true) }

    LaunchedEffect(pagerState.currentPage) {
        isToolbarVisible = true
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
            if (!isToolbar) {
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
        }
    ) { innerPadding ->
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
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    userScrollEnabled = !isSwipeActionsEnabled
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
                        PreferenceManager.TAB_RECORDINGS -> com.grinch.rivo4.view.screen.settings.CallRecordingsContent(
                            showTopBar = false,
                            initialShowList = true
                        )
                    }
                }
            }

            if (isToolbar && isBlurEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AnimatedVisibility(
                    visible = isToolbarVisible,
                    enter = fadeIn(animationSpec = tween(150)),
                    exit = fadeOut(animationSpec = tween(150)),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    val blurRadiusPx = with(LocalDensity.current) { 44.dp.toPx() }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(navBarsBottom + 60.dp)
                            .graphicsLayer {
                                renderEffect = RenderEffect.createBlurEffect(
                                    blurRadiusPx, blurRadiusPx,
                                    Shader.TileMode.CLAMP
                                ).asComposeRenderEffect()
                            }
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                                    )
                                )
                            )
                    )
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
                            pagerState = pagerState,
                            visibleTabs = visibleTabs,
                            onPageSelected = { page ->
                                scope.launch {
                                    pagerState.animateScrollToPage(page)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
