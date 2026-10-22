package com.grinch.rivo4.view.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.view.components.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun CallLogCustomizationScreen(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()

    var trailingAction by remember(settingsState) {
        mutableStateOf(prefs.getCallLogTrailingAction())
    }
    var savedActions by remember(settingsState) {
        mutableStateOf(prefs.getCallLogSavedActions())
    }
    var unsavedActions by remember(settingsState) {
        mutableStateOf(prefs.getCallLogUnsavedActions())
    }
    var showTrailingDialog by remember { mutableStateOf(false) }

    val trailingOptions = remember {
        listOf(
            "call" to (R.string.action_call to Icons.Rounded.Call),
            "message" to (R.string.action_message to Icons.AutoMirrored.Outlined.Message),
            "video_call" to (R.string.contact_details_video to Icons.Outlined.Videocam),
            "whatsapp" to (R.string.swipe_action_whatsapp to Icons.Outlined.ChatBubbleOutline),
            "view_contact" to (R.string.swipe_action_view_contact to Icons.Outlined.Person),
            "history" to (R.string.action_history to Icons.Outlined.History),
            "none" to (R.string.action_none to Icons.Outlined.VisibilityOff)
        )
    }

    val availableSavedActions: List<Pair<String, Pair<Int, ImageVector>>> = remember {
        listOf(
            "view_contact" to (R.string.swipe_action_view_contact to Icons.Outlined.Person),
            "block" to (R.string.swipe_action_block to Icons.Outlined.Block),
            "message" to (R.string.action_message to Icons.AutoMirrored.Outlined.Message),
            "call" to (R.string.action_call to Icons.Rounded.Call),
            "video_call" to (R.string.contact_details_video to Icons.Outlined.Videocam),
            "history" to (R.string.action_history to Icons.Outlined.History),
            "share" to (R.string.swipe_action_share to Icons.Outlined.Share),
            "whatsapp" to (R.string.swipe_action_whatsapp to Icons.Outlined.ChatBubbleOutline),
            "copy_number" to (R.string.swipe_action_copy_number to Icons.Outlined.ContentCopy)
        )
    }

    val availableUnsavedActions: List<Pair<String, Pair<Int, ImageVector>>> = remember {
        listOf(
            "save" to (R.string.action_save to Icons.Outlined.PersonAdd),
            "view_contact" to (R.string.swipe_action_view_contact to Icons.Outlined.Person),
            "block" to (R.string.swipe_action_block to Icons.Outlined.Block),
            "message" to (R.string.action_message to Icons.AutoMirrored.Outlined.Message),
            "call" to (R.string.action_call to Icons.Rounded.Call),
            "video_call" to (R.string.contact_details_video to Icons.Outlined.Videocam),
            "history" to (R.string.action_history to Icons.Outlined.History),
            "share" to (R.string.swipe_action_share to Icons.Outlined.Share),
            "whatsapp" to (R.string.swipe_action_whatsapp to Icons.Outlined.ChatBubbleOutline),
            "copy_number" to (R.string.swipe_action_copy_number to Icons.Outlined.ContentCopy)
        )
    }

    Scaffold(
        topBar = {
            MenuTopAppBar(
                text = stringResource(R.string.settings_call_log_customization_title),
                navigator = navigator
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        if (showTrailingDialog) {
            RivoSelectionDialog(
                onDismissRequest = { showTrailingDialog = false },
                title = stringResource(R.string.settings_call_log_trailing_title),
                icon = Icons.Outlined.TouchApp,
                items = trailingOptions,
                itemLabel = { (_, info) -> context.getString(info.first) },
                itemIcon = { (_, info) -> info.second },
                isSelected = { (key, _) -> key == trailingAction },
                onItemSelected = { (key, _) ->
                    trailingAction = key
                    prefs.setCallLogTrailingAction(key)
                    showTrailingDialog = false
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Interaction Guide Card
            item {
                RivoExpressiveCard(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_call_log_click_behavior_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.settings_call_log_click_behavior_supporting),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Trailing Button Action
            item {
                val currentInfo = trailingOptions.firstOrNull { it.first == trailingAction }?.second
                RivoExpressiveGroup(
                    title = stringResource(R.string.settings_call_log_trailing_title),
                    icon = Icons.Outlined.TouchApp
                ) {
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_call_log_trailing_title),
                            supporting = currentInfo?.first?.let { stringResource(it) } ?: trailingAction,
                            leadingIcon = currentInfo?.second ?: Icons.Rounded.Call,
                            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            onClick = { showTrailingDialog = true }
                        )
                    }
                }
            }

            // Saved Contacts Quick Actions
            item {
                RivoExpressiveGroup(
                    title = stringResource(R.string.settings_call_log_saved_actions_title),
                    icon = Icons.Outlined.Person
                ) {
                    availableSavedActions.forEach { (key, info) ->
                        val (labelRes, icon) = info
                        val isChecked = savedActions.contains(key)
                        item(key = "saved_$key") {
                            RivoSwitchListItem(
                                headline = stringResource(labelRes),
                                leadingIcon = icon,
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    val updated = if (checked) {
                                        if (!savedActions.contains(key)) savedActions + key else savedActions
                                    } else {
                                        savedActions.filter { it != key }
                                    }
                                    savedActions = updated
                                    prefs.setCallLogSavedActions(updated)
                                }
                            )
                        }
                    }
                }
            }

            // Unsaved Numbers Quick Actions
            item {
                RivoExpressiveGroup(
                    title = stringResource(R.string.settings_call_log_unsaved_actions_title),
                    icon = Icons.Outlined.PersonAdd
                ) {
                    availableUnsavedActions.forEach { (key, info) ->
                        val (labelRes, icon) = info
                        val isChecked = unsavedActions.contains(key)
                        item(key = "unsaved_$key") {
                            RivoSwitchListItem(
                                headline = stringResource(labelRes),
                                leadingIcon = icon,
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    val updated = if (checked) {
                                        if (!unsavedActions.contains(key)) unsavedActions + key else unsavedActions
                                    } else {
                                        unsavedActions.filter { it != key }
                                    }
                                    unsavedActions = updated
                                    prefs.setCallLogUnsavedActions(updated)
                                }
                            )
                        }
                    }
                }
            }

            // Reset Button
            item {
                RivoResetButton(
                    onClick = {
                        prefs.resetCallLogCustomization()
                        trailingAction = prefs.getCallLogTrailingAction()
                        savedActions = prefs.getCallLogSavedActions()
                        unsavedActions = prefs.getCallLogUnsavedActions()
                    },
                    text = stringResource(R.string.settings_call_log_reset),
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }
        }
    }
}
