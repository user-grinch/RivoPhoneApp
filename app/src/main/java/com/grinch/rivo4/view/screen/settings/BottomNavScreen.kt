package com.grinch.rivo4.view.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.view.components.RivoDivider
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.navigationTabIcon
import com.grinch.rivo4.view.components.navigationTabLabel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun BottomNavScreen(
    navigator: DestinationsNavigator
) {
    val prefs = koinInject<PreferenceManager>()

    var order by remember { mutableStateOf(prefs.getBottomNavOrder()) }
    var hidden by remember { mutableStateOf(prefs.getHiddenBottomNavTabs()) }

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
                title = { Text(stringResource(R.string.settings_bottom_nav_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings_bottom_nav_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            item {
                RivoExpressiveCard(title = stringResource(R.string.settings_bottom_nav_preview)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            visibleTabs.forEach { tabId ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        navigationTabIcon(tabId),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        navigationTabLabel(tabId),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                RivoExpressiveCard(title = stringResource(R.string.settings_bottom_nav_buttons)) {
                    order.forEachIndexed { index, tabId ->
                        val isVisible = !hidden.contains(tabId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
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
                }
            }

            item {
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
}
