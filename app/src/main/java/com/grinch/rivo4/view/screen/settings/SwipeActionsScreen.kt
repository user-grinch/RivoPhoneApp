package com.grinch.rivo4.view.screen.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.modal.data.SwipeActionType
import com.grinch.rivo4.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SwipeActionsScreen(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()

    var enabled by remember(settingsState) { mutableStateOf(prefs.isSwipeActionsEnabled()) }
    var rightActionId by remember(settingsState) { mutableIntStateOf(prefs.getSwipeRightAction()) }
    var leftActionId by remember(settingsState) { mutableIntStateOf(prefs.getSwipeLeftAction()) }

    val swipeRightAction = remember(rightActionId) { SwipeActionType.fromId(rightActionId) }
    val swipeLeftAction = remember(leftActionId) { SwipeActionType.fromId(leftActionId) }

    val actionOptions = remember {
        listOf(
            SwipeActionType.CALL,
            SwipeActionType.MESSAGE,
            SwipeActionType.VIDEO_CALL,
            SwipeActionType.WHATSAPP,
            SwipeActionType.COPY_NUMBER,
            SwipeActionType.DELETE,
            SwipeActionType.NONE
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_swipe_actions_title), fontWeight = FontWeight.Bold) },
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings_swipe_actions_supporting),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // Master Enable Toggle
            item {
                RivoExpressiveCard {
                    RivoSwitchListItem(
                        headline = stringResource(R.string.settings_swipe_actions_enable),
                        supporting = stringResource(R.string.settings_swipe_actions_enable_supporting),
                        leadingIcon = Icons.Outlined.Swipe,
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            prefs.setSwipeActionsEnabled(it)
                        }
                    )
                    if (enabled) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
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
                                    text = stringResource(R.string.settings_swipe_actions_tab_warning),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Live Preview Card
            item {
                RivoExpressiveGroup(
                    title = stringResource(R.string.swipe_action_preview_title),
                    icon = Icons.Outlined.Gesture
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.swipe_action_preview_supporting),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RivoSwipeToActionBox(
                                    enabled = enabled,
                                    swipeRightAction = swipeRightAction,
                                    swipeLeftAction = swipeLeftAction,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    onTriggerAction = { action ->
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.swipe_action_triggered_toast, context.getString(action.titleRes)),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                ) {
                                    RivoListItem(
                                        headline = stringResource(R.string.swipe_action_test_contact_name),
                                        supporting = stringResource(R.string.swipe_action_test_contact_number),
                                        avatarName = stringResource(R.string.swipe_action_test_contact_name),
                                        onClick = {}
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Swipe Right Configuration
            item {
                RivoExpressiveGroup(
                    title = stringResource(R.string.settings_swipe_right_action),
                    icon = Icons.Outlined.SwipeRight
                ) {
                    item {
                        RivoVisualOptionSelectorRow(
                            headline = stringResource(R.string.settings_swipe_right_action),
                            supporting = stringResource(R.string.settings_swipe_right_action_supporting),
                            leadingIcon = Icons.Outlined.SwipeRight,
                            options = actionOptions.map { stringResource(it.titleRes) to it.id },
                            selectedValue = rightActionId,
                            onValueChange = { selected ->
                                rightActionId = selected
                                prefs.setSwipeRightAction(selected)
                            }
                        ) { value, selected ->
                            val action = SwipeActionType.fromId(value)
                            Icon(
                                imageVector = action.icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Swipe Left Configuration
            item {
                RivoExpressiveGroup(
                    title = stringResource(R.string.settings_swipe_left_action),
                    icon = Icons.Outlined.SwipeLeft
                ) {
                    item {
                        RivoVisualOptionSelectorRow(
                            headline = stringResource(R.string.settings_swipe_left_action),
                            supporting = stringResource(R.string.settings_swipe_left_action_supporting),
                            leadingIcon = Icons.Outlined.SwipeLeft,
                            options = actionOptions.map { stringResource(it.titleRes) to it.id },
                            selectedValue = leftActionId,
                            onValueChange = { selected ->
                                leftActionId = selected
                                prefs.setSwipeLeftAction(selected)
                            }
                        ) { value, selected ->
                            val action = SwipeActionType.fromId(value)
                            Icon(
                                imageVector = action.icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Reset Button
            item {
                OutlinedButton(
                    onClick = {
                        prefs.resetSwipeActions()
                        enabled = prefs.isSwipeActionsEnabled()
                        rightActionId = prefs.getSwipeRightAction()
                        leftActionId = prefs.getSwipeLeftAction()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Outlined.Restore, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_swipe_actions_reset))
                }
            }

            item {
            }
        }
    }
}
