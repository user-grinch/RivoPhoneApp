package com.grinch.rivo4.view.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.view.components.RivoDivider
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoInteractiveFloatingBarSlider
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.grinch.rivo4.view.components.RivoVisualOptionSelectorRow
import com.grinch.rivo4.view.components.navigationTabIcon
import com.grinch.rivo4.view.components.navigationTabLabel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun BottomNavScreen(
    navigator: DestinationsNavigator
) {
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()

    var order by remember { mutableStateOf(prefs.getBottomNavOrder()) }
    var hidden by remember { mutableStateOf(prefs.getHiddenBottomNavTabs()) }
    var navBarStyle by remember(settingsState) {
        mutableIntStateOf(prefs.getInt(PreferenceManager.KEY_NAV_BAR_STYLE, PreferenceManager.NAV_BAR_STYLE_STANDARD))
    }
    var floatingBarRoundness by remember(settingsState) {
        mutableIntStateOf(prefs.getFloatingBarRoundness())
    }
    var isBlurEnabled by remember(settingsState) {
        mutableStateOf(prefs.isUiBlurEnabled())
    }
    var iconOnly by remember(settingsState) {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_ICON_ONLY_NAV, false))
    }
    var defaultBottomBar by remember(settingsState) {
        mutableIntStateOf(prefs.getInt(PreferenceManager.KEY_DEFAULT_BOTTOM_NAV, PreferenceManager.TAB_RECENTS))
    }
    var startLocation by remember(settingsState) {
        mutableIntStateOf(prefs.getInt(PreferenceManager.KEY_START_LOCATION, PreferenceManager.START_LOCATION_NORMAL))
    }
    var mergeFavorites by remember(settingsState) {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_MERGE_FAVORITES_RECENTS, true))
    }

    val visibleTabs = order.filter { !hidden.contains(it) }

    fun persist(newOrder: List<Int>, newHidden: Set<Int>) {
        order = newOrder
        hidden = newHidden
        prefs.setBottomNavOrder(newOrder)
        prefs.setHiddenBottomNavTabs(newHidden)
    }

    fun move(index: Int, delta: Int) {
        val target = index + delta
        if (target < 0 || target > order.lastIndex) return
        val mutable = order.toMutableList()
        val item = mutable.removeAt(index)
        mutable.add(target, item)
        persist(mutable, hidden)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Navigation Bar", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Style & Appearance
            item {
                RivoExpressiveCard(title = "Style & Appearance") {
                    RivoVisualOptionSelectorRow(
                        headline = stringResource(R.string.settings_interface_nav_bar_style),
                        supporting = stringResource(R.string.settings_interface_nav_bar_style_supporting),
                        leadingIcon = Icons.Outlined.Dock,
                        options = listOf(
                            stringResource(R.string.settings_nav_bar_standard) to PreferenceManager.NAV_BAR_STYLE_STANDARD,
                            stringResource(R.string.settings_nav_bar_toolbar) to PreferenceManager.NAV_BAR_STYLE_TOOLBAR
                        ),
                        selectedValue = navBarStyle,
                        onValueChange = {
                            navBarStyle = it
                            prefs.setInt(PreferenceManager.KEY_NAV_BAR_STYLE, it)
                        }
                    ) { value, selected ->
                        val icon = if (value == PreferenceManager.NAV_BAR_STYLE_TOOLBAR) Icons.Outlined.DashboardCustomize else Icons.Outlined.ViewStream
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (navBarStyle == PreferenceManager.NAV_BAR_STYLE_TOOLBAR) {
                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                        RivoInteractiveFloatingBarSlider(
                            headline = stringResource(R.string.settings_floating_bar_roundness),
                            supporting = stringResource(R.string.settings_floating_bar_roundness_supporting),
                            value = floatingBarRoundness.toFloat(),
                            iconOnly = iconOnly,
                            onValueChange = { floatingBarRoundness = it.roundToInt() },
                            onValueChangeFinished = {
                                prefs.setFloatingBarRoundness(floatingBarRoundness)
                            }
                        )
                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_floating_bar_blur),
                            supporting = stringResource(R.string.settings_floating_bar_blur_supporting),
                            leadingIcon = Icons.Outlined.BlurOn,
                            checked = isBlurEnabled,
                            onCheckedChange = {
                                isBlurEnabled = it
                                prefs.setUiBlurEnabled(it)
                            }
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.WarningAmber,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.settings_floating_bar_blur_warning),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // 2. Tab Layout & Buttons
            item {
                RivoExpressiveCard(title = stringResource(R.string.settings_bottom_nav_buttons)) {
                    Text(
                        text = stringResource(R.string.settings_bottom_nav_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    RivoDivider(Modifier.padding(horizontal = 16.dp))
                    order.forEachIndexed { index, tabId ->
                        val isVisible = !hidden.contains(tabId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        navigationTabIcon(tabId),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = navigationTabLabel(tabId),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isVisible) {
                                        stringResource(R.string.settings_bottom_nav_position, visibleTabs.indexOf(tabId) + 1)
                                    } else {
                                        stringResource(R.string.settings_bottom_nav_hidden)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { move(index, -1) },
                                enabled = index > 0
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowUp,
                                    contentDescription = stringResource(R.string.settings_bottom_nav_move_up)
                                )
                            }
                            IconButton(
                                onClick = { move(index, 1) },
                                enabled = index < order.lastIndex
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = stringResource(R.string.settings_bottom_nav_move_down)
                                )
                            }
                            Switch(
                                checked = isVisible,
                                enabled = !isVisible || visibleTabs.size > 1,
                                onCheckedChange = { checked ->
                                    val newHidden = if (checked) hidden - tabId else hidden + tabId
                                    persist(order, newHidden)
                                }
                            )
                        }
                        if (index < order.lastIndex) {
                            RivoDivider(Modifier.padding(horizontal = 16.dp))
                        }
                    }
                    RivoDivider(Modifier.padding(horizontal = 16.dp))
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        OutlinedButton(
                            onClick = {
                                prefs.resetBottomNavLayout()
                                order = prefs.getBottomNavOrder()
                                hidden = prefs.getHiddenBottomNavTabs()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Icon(Icons.Outlined.Restore, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_bottom_nav_reset))
                        }
                    }
                }
            }

            // 3. Behavior & Defaults
            item {
                RivoExpressiveCard(title = "Behavior & Defaults") {
                    RivoSwitchListItem(
                        headline = stringResource(R.string.settings_interface_icon_only_bar),
                        supporting = stringResource(R.string.settings_interface_icon_only_bar_supporting),
                        leadingIcon = Icons.Outlined.ViewStream,
                        checked = iconOnly,
                        onCheckedChange = {
                            iconOnly = it
                            prefs.setBoolean(PreferenceManager.KEY_ICON_ONLY_NAV, it)
                        }
                    )
                    RivoDivider(Modifier.padding(horizontal = 16.dp))
                    RivoVisualOptionSelectorRow(
                        headline = stringResource(R.string.settings_interface_default_bottom_bar),
                        supporting = stringResource(R.string.settings_interface_default_bottom_bar_supporting),
                        leadingIcon = Icons.Outlined.SpaceDashboard,
                        options = listOf(
                            stringResource(R.string.nav_recents) to PreferenceManager.TAB_RECENTS,
                            stringResource(R.string.nav_favorites) to PreferenceManager.TAB_FAVORITES,
                            stringResource(R.string.nav_contacts) to PreferenceManager.TAB_CONTACTS,
                            stringResource(R.string.nav_call_recordings) to PreferenceManager.TAB_RECORDINGS
                        ),
                        selectedValue = defaultBottomBar,
                        onValueChange = {
                            defaultBottomBar = it
                            prefs.setInt(PreferenceManager.KEY_DEFAULT_BOTTOM_NAV, it)
                            if (it == PreferenceManager.TAB_RECORDINGS) {
                                val currentHidden = prefs.getHiddenBottomNavTabs().toMutableSet()
                                if (currentHidden.remove(PreferenceManager.TAB_RECORDINGS)) {
                                    prefs.setHiddenBottomNavTabs(currentHidden)
                                    hidden = currentHidden
                                }
                            }
                        }
                    ) { value, selected ->
                        val icon = when (value) {
                            PreferenceManager.TAB_FAVORITES -> Icons.Outlined.Star
                            PreferenceManager.TAB_CONTACTS -> Icons.Outlined.Person
                            PreferenceManager.TAB_RECORDINGS -> Icons.Outlined.Mic
                            else -> Icons.Outlined.History
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    RivoDivider(Modifier.padding(horizontal = 16.dp))
                    RivoVisualOptionSelectorRow(
                        headline = "Default Start Screen",
                        supporting = "Screen to display when opening the app",
                        leadingIcon = Icons.Outlined.Home,
                        options = listOf(
                            "Default Tab" to PreferenceManager.START_LOCATION_NORMAL,
                            "Dialpad (Recents)" to PreferenceManager.START_LOCATION_DIALPAD_RECENTS,
                            "Dialpad (Contacts)" to PreferenceManager.START_LOCATION_DIALPAD_CONTACTS
                        ),
                        selectedValue = startLocation,
                        onValueChange = {
                            startLocation = it
                            prefs.setInt(PreferenceManager.KEY_START_LOCATION, it)
                        }
                    ) { value, selected ->
                        val icon = when (value) {
                            PreferenceManager.START_LOCATION_DIALPAD_RECENTS,
                            PreferenceManager.START_LOCATION_DIALPAD_CONTACTS -> Icons.Outlined.Dialpad
                            else -> Icons.Outlined.Home
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    RivoDivider(Modifier.padding(horizontal = 16.dp))
                    RivoSwitchListItem(
                        headline = stringResource(R.string.settings_interface_merge_favorites),
                        supporting = stringResource(R.string.settings_interface_merge_favorites_supporting),
                        leadingIcon = Icons.Outlined.Star,
                        checked = mergeFavorites,
                        onCheckedChange = {
                            mergeFavorites = it
                            prefs.setBoolean(PreferenceManager.KEY_MERGE_FAVORITES_RECENTS, it)
                        }
                    )
                }
            }

            item {
                com.grinch.rivo4.view.components.ad.BannerAd()
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
