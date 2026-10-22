package com.grinch.rivo4.view.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.view.components.MenuTopAppBar
import com.grinch.rivo4.view.components.RivoAvatar
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoExpressiveGroup
import com.grinch.rivo4.view.components.RivoListItem
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.grinch.rivo4.view.screen.CallActionButton
import com.grinch.rivo4.view.screen.RivoCallButtonOrder
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

private data class CallButtonSlot(
    val id: String,
    val labelRes: Int,
    val icon: ImageVector
)

private val callButtonSlots = listOf(
    CallButtonSlot(RivoCallButtonOrder.MUTE, R.string.mute, Icons.Default.Mic),
    CallButtonSlot(RivoCallButtonOrder.KEYPAD, R.string.keypad, Icons.Default.Dialpad),
    CallButtonSlot(RivoCallButtonOrder.AUDIO, R.string.audio, Icons.AutoMirrored.Filled.VolumeUp),
    CallButtonSlot(RivoCallButtonOrder.RECORD, R.string.record, Icons.Default.FiberManualRecord),
    CallButtonSlot(RivoCallButtonOrder.HOLD, R.string.hold, Icons.Default.Pause),
    CallButtonSlot(RivoCallButtonOrder.ADD, R.string.add, Icons.Default.Add)
)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun CallScreenCustomizationScreen(
    navigator: DestinationsNavigator
) {
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()

    var order by remember(settingsState) {
        mutableStateOf(
            RivoCallButtonOrder.parse(
                prefs.getString(
                    PreferenceManager.KEY_CALL_BUTTON_ORDER,
                    PreferenceManager.DEFAULT_CALL_BUTTON_ORDER
                )
            )
        )
    }
    var hidden by remember(settingsState) {
        mutableStateOf(
            RivoCallButtonOrder.hiddenSet(
                prefs.getString(PreferenceManager.KEY_HIDDEN_CALL_BUTTONS, "")
            )
        )
    }
    val visibleOrder = order.filterNot { hidden.contains(it) }

    fun move(index: Int, delta: Int) {
        val visible = visibleOrder.toMutableList()
        val target = index + delta
        if (target !in visible.indices) return
        val item = visible.removeAt(index)
        visible.add(target, item)
        // Hidden buttons keep their absolute slots; only the visible sequence is reordered
        var nextVisible = 0
        val rebuilt = order.map { if (hidden.contains(it)) it else visible[nextVisible++] }
        order = rebuilt
        prefs.setString(PreferenceManager.KEY_CALL_BUTTON_ORDER, RivoCallButtonOrder.format(rebuilt))
    }

    fun toggleButton(id: String, show: Boolean) {
        val next = hidden.toMutableSet()
        if (show) {
            next.remove(id)
        } else {
            // Always keep at least one button on the call screen
            if (order.count { !next.contains(it) && it != id } <= 0) return
            next.add(id)
        }
        hidden = next
        prefs.setString(PreferenceManager.KEY_HIDDEN_CALL_BUTTONS, next.joinToString(","))
    }

    Scaffold(
        topBar = {
            MenuTopAppBar(
                text = stringResource(R.string.call_screen_customization_title),
                navigator = navigator
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                RivoExpressiveCard(
                    title = stringResource(R.string.call_screen_preview_title),
                    icon = Icons.Default.Dialpad
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RivoAvatar(
                            name = "Jane Doe",
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "Jane Doe",
                            style = MaterialTheme.typography.titleMediumEmphasized
                        )
                        Text(
                            text = "+1 (555) 123-4567",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            visibleOrder.chunked(3).forEach { rowIds ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowIds.forEach { id ->
                                        val slot = callButtonSlots.firstOrNull { it.id == id }
                                            ?: callButtonSlots.first()
                                        CallActionButton(
                                            icon = slot.icon,
                                            isActive = false,
                                            label = stringResource(slot.labelRes),
                                            compact = true,
                                            modifier = Modifier.weight(1f),
                                            onClick = {}
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                RivoExpressiveGroup(
                    title = stringResource(R.string.call_screen_visible_buttons_title),
                    icon = Icons.Outlined.Visibility
                ) {
                    callButtonSlots.forEach { slot ->
                        item(slot.id) {
                            RivoSwitchListItem(
                                headline = stringResource(slot.labelRes),
                                supporting = stringResource(R.string.call_screen_visible_buttons_supporting),
                                leadingIcon = slot.icon,
                                checked = !hidden.contains(slot.id),
                                onCheckedChange = { show -> toggleButton(slot.id, show) }
                            )
                        }
                    }
                }
            }

            item {
                RivoExpressiveGroup(
                    title = stringResource(R.string.rearrange_call_buttons),
                    icon = Icons.Outlined.KeyboardArrowUp
                ) {
                    visibleOrder.forEachIndexed { index, id ->
                        item(id) {
                            val slot = callButtonSlots.firstOrNull { it.id == id } ?: callButtonSlots.first()
                            RivoListItem(
                                headline = stringResource(slot.labelRes),
                                supporting = if (index < 3) {
                                    stringResource(R.string.call_slot_top, index + 1)
                                } else {
                                    stringResource(R.string.call_slot_bottom, index - 2)
                                },
                                leadingIcon = slot.icon,
                                trailingContent = {
                                    IconButton(
                                        onClick = { move(index, -1) },
                                        enabled = index > 0,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.KeyboardArrowUp,
                                            contentDescription = stringResource(R.string.call_slot_move_up)
                                        )
                                    }
                                    IconButton(
                                        onClick = { move(index, 1) },
                                        enabled = index < visibleOrder.lastIndex,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.KeyboardArrowDown,
                                            contentDescription = stringResource(R.string.call_slot_move_down)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.call_screen_customization_supporting),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                )
            }
        }
    }
}
