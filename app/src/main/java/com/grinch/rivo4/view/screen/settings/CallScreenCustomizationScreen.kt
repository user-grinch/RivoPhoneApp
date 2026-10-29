package com.grinch.rivo4.view.screen.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.ContactSupport
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.SpeakerPhone
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.PhoneInTalk
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.CallBackgroundStore
import com.grinch.rivo4.view.components.CallBackgroundDialog
import com.grinch.rivo4.view.components.MenuTopAppBar
import com.grinch.rivo4.view.components.RivoAvatar
import com.grinch.rivo4.view.components.RivoAvatarShapeSelectorRow
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoExpressiveGroup
import com.grinch.rivo4.view.components.RivoListItem
import com.grinch.rivo4.view.components.RivoSliderListItem
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.grinch.rivo4.view.components.RivoVisualOptionSelectorRow
import com.grinch.rivo4.view.screen.settings.IncomingCallUiPreview
import com.grinch.rivo4.view.screen.CallActionButton
import com.grinch.rivo4.view.screen.LocalCallHasBackground
import com.grinch.rivo4.view.screen.RivoCallButtonOrder
import com.grinch.rivo4.view.theme.RIVO_AVATAR_SHAPE_COOKIE_9
import com.grinch.rivo4.view.theme.RivoExpressiveAvatarShape
import com.grinch.rivo4.view.theme.rivoAvatarShape
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.roundToInt

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
    CallButtonSlot(RivoCallButtonOrder.ADD, R.string.add, Icons.Default.Add),
)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun CallScreenCustomizationScreen(
    navigator: DestinationsNavigator
) {
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var proximitySensor by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_PROXIMITY_SENSOR, true)) }
    var autoSpeakerProximity by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_AUTO_SPEAKER_PROXIMITY, false)) }
    var incomingCallUI by remember(settingsState) { mutableIntStateOf(prefs.getInt(PreferenceManager.KEY_INCOMING_CALL_UI_MODE, 0)) }
    var callScreenAvatarShape by remember(settingsState) {
        mutableIntStateOf(prefs.getInt(PreferenceManager.KEY_CALL_SCREEN_AVATAR_SHAPE, RIVO_AVATAR_SHAPE_COOKIE_9))
    }
    var bgBlur by remember(settingsState) {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_CALL_BACKGROUND_BLUR, false))
    }
    var bgBlurStrength by remember(settingsState) {
        mutableFloatStateOf(prefs.getInt(PreferenceManager.KEY_CALL_BACKGROUND_BLUR_STRENGTH, 25).toFloat())
    }
    var alwaysFullScreenCalls by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_ALWAYS_FULL_SCREEN_CALLS, false)) }
    var openCallScreenOnAnswer by remember(settingsState) { mutableStateOf(prefs.isOpenCallScreenOnAnswerEnabled()) }
    var pocketMode by remember(settingsState) { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_POCKET_MODE, false)) }
    var defaultCallBg by remember(settingsState) { mutableStateOf(CallBackgroundStore.defaultModel(context)) }
    var unknownCallBg by remember(settingsState) { mutableStateOf(CallBackgroundStore.unknownModel(context)) }
    var showDefaultBgDialog by remember { mutableStateOf(false) }
    var showUnknownBgDialog by remember { mutableStateOf(false) }
    var savingDefaultBg by remember { mutableStateOf(false) }
    var savingUnknownBg by remember { mutableStateOf(false) }

    val defaultCallBgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            savingDefaultBg = true
            scope.launch {
                val ok = CallBackgroundStore.saveDefault(context, uri)
                savingDefaultBg = false
                if (ok) defaultCallBg = CallBackgroundStore.defaultModelAsync(context)
            }
        }
    }
    val unknownCallBgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            savingUnknownBg = true
            scope.launch {
                val ok = CallBackgroundStore.saveUnknown(context, uri)
                savingUnknownBg = false
                if (ok) unknownCallBg = CallBackgroundStore.unknownModelAsync(context)
            }
        }
    }

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
            if (id !in order) {
                order = order + id
                prefs.setString(PreferenceManager.KEY_CALL_BUTTON_ORDER, RivoCallButtonOrder.format(order))
            }
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
                text = stringResource(R.string.loc_call_screen_buttons),
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
                RivoExpressiveGroup(title = stringResource(R.string.call_customization_behavior), icon = Icons.Outlined.PhoneInTalk) {
                    item {
                        RivoSwitchListItem(headline = stringResource(R.string.settings_call_proximity_sensor), supporting = stringResource(R.string.settings_call_proximity_sensor_supporting), leadingIcon = Icons.Outlined.Sensors, checked = proximitySensor, onCheckedChange = {
                            proximitySensor = it; prefs.setBoolean(PreferenceManager.KEY_PROXIMITY_SENSOR, it)
                        })
                    }
                    item {
                        RivoSwitchListItem(headline = stringResource(R.string.settings_call_auto_speaker_proximity), supporting = stringResource(R.string.settings_call_auto_speaker_proximity_supporting), leadingIcon = Icons.Outlined.SpeakerPhone, checked = autoSpeakerProximity, onCheckedChange = {
                            autoSpeakerProximity = it; prefs.setBoolean(PreferenceManager.KEY_AUTO_SPEAKER_PROXIMITY, it)
                        })
                    }
                    item {
                        RivoSwitchListItem(headline = stringResource(R.string.settings_call_always_fullscreen), supporting = stringResource(R.string.settings_call_always_fullscreen_supporting), leadingIcon = Icons.Outlined.Fullscreen, checked = alwaysFullScreenCalls, onCheckedChange = {
                            alwaysFullScreenCalls = it; prefs.setBoolean(PreferenceManager.KEY_ALWAYS_FULL_SCREEN_CALLS, it)
                        })
                    }
                    item {
                        RivoSwitchListItem(headline = stringResource(R.string.settings_open_call_screen_on_answer_title), supporting = stringResource(R.string.settings_open_call_screen_on_answer_supporting), leadingIcon = Icons.Outlined.PhoneInTalk, checked = openCallScreenOnAnswer, onCheckedChange = {
                            openCallScreenOnAnswer = it; prefs.setOpenCallScreenOnAnswerEnabled(it)
                        })
                    }
                    item {
                        RivoSwitchListItem(headline = stringResource(R.string.call_customization_pocket_mode), supporting = stringResource(R.string.call_customization_pocket_mode_supporting), leadingIcon = Icons.Outlined.ScreenLockPortrait, checked = pocketMode, onCheckedChange = {
                            pocketMode = it; prefs.setBoolean(PreferenceManager.KEY_POCKET_MODE, it)
                        })
                    }
                }
            }

            item {
                RivoExpressiveGroup(title = stringResource(R.string.call_customization_incoming_style), icon = Icons.Outlined.PhoneInTalk) {
                    item {
                        Box(modifier = Modifier.padding(vertical = 4.dp)) {
                            RivoVisualOptionSelectorRow(
                                headline = stringResource(R.string.settings_call_incoming_ui),
                                supporting = stringResource(R.string.settings_call_incoming_ui_supporting),
                                leadingIcon = Icons.Outlined.PhoneInTalk,
                                options = listOf(
                                    stringResource(R.string.settings_call_incoming_ui_horizontal_swipe) to 0,
                                    stringResource(R.string.settings_call_incoming_ui_buttons) to 1,
                                    stringResource(R.string.settings_call_incoming_ui_slide_ios) to 2,
                                    stringResource(R.string.settings_call_incoming_ui_vertical_swipe) to 3,
                                    stringResource(R.string.settings_call_incoming_ui_dual_swipe) to 4,
                                    stringResource(R.string.settings_call_incoming_ui_orbital_slider) to 5
                                ),
                                selectedValue = incomingCallUI,
                                onValueChange = {
                                    incomingCallUI = it
                                    prefs.setInt(PreferenceManager.KEY_INCOMING_CALL_UI_MODE, it)
                                },
                                tileWidth = 110.dp,
                                tileHeight = 76.dp
                            ) { value, _ ->
                                IncomingCallUiPreview(value)
                            }
                        }
                    }
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_call_default_background),
                            supporting = if (defaultCallBg != null) stringResource(R.string.settings_call_default_background_set) else stringResource(R.string.settings_call_default_background_none),
                            leadingIcon = Icons.Outlined.Wallpaper,
                            trailingContent = if (defaultCallBg != null) {
                                {
                                    IconButton(onClick = {
                                        CallBackgroundStore.clearDefault(context)
                                        defaultCallBg = null
                                    }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = stringResource(R.string.settings_call_background_remove),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            } else null,
                            onClick = { showDefaultBgDialog = true }
                        )
                    }
                    item {
                        RivoListItem(
                            headline = stringResource(R.string.settings_call_unknown_background),
                            supporting = if (unknownCallBg != null) stringResource(R.string.settings_call_unknown_background_set) else stringResource(R.string.settings_call_unknown_background_none),
                            leadingIcon = Icons.Outlined.Image,
                            trailingContent = if (unknownCallBg != null) {
                                {
                                    IconButton(onClick = {
                                        CallBackgroundStore.clearUnknown(context)
                                        unknownCallBg = null
                                    }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = stringResource(R.string.settings_call_background_remove),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            } else null,
                            onClick = { showUnknownBgDialog = true }
                        )
                    }
                    item {
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_call_background_blur),
                            supporting = stringResource(R.string.settings_call_background_blur_supporting),
                            leadingIcon = Icons.Outlined.BlurOn,
                            checked = bgBlur,
                            onCheckedChange = {
                                bgBlur = it
                                prefs.setBoolean(PreferenceManager.KEY_CALL_BACKGROUND_BLUR, it)
                            }
                        )
                    }
                    if (bgBlur) {
                        item {
                            RivoSliderListItem(
                                headline = stringResource(R.string.settings_call_background_blur_strength),
                                value = bgBlurStrength,
                                valueRange = 5f..60f,
                                steps = 10,
                                valueLabel = "${bgBlurStrength.roundToInt()} dp",
                                leadingIcon = Icons.Outlined.Tune,
                                onValueChange = {
                                    bgBlurStrength = it
                                },
                                onValueChangeFinished = {
                                    prefs.setInt(PreferenceManager.KEY_CALL_BACKGROUND_BLUR_STRENGTH, bgBlurStrength.roundToInt())
                                }
                            )
                        }
                    }
                }
            }

            item {
                RivoExpressiveGroup(
                    title = stringResource(R.string.settings_interface_call_screen_avatar_shape),
                    icon = Icons.Outlined.AccountCircle
                ) {
                    item {
                        RivoAvatarShapeSelectorRow(
                            headline = stringResource(R.string.settings_interface_call_screen_avatar_shape),
                            supporting = stringResource(R.string.settings_interface_call_screen_avatar_shape_supporting),
                            options = RivoExpressiveAvatarShape.entries.map { it.label to it.id },
                            selectedValue = callScreenAvatarShape,
                            onValueChange = { selected ->
                                callScreenAvatarShape = selected
                                prefs.setInt(PreferenceManager.KEY_CALL_SCREEN_AVATAR_SHAPE, selected)
                            }
                        )
                    }
                }
            }

            item {
                RivoExpressiveCard(
                    title = stringResource(R.string.call_screen_preview_title),
                    icon = Icons.Default.Dialpad
                ) {
                    CompositionLocalProvider(LocalCallHasBackground provides (defaultCallBg != null)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RivoAvatar(
                                name = "Jane Doe",
                                modifier = Modifier.size(56.dp),
                                shape = rivoAvatarShape(callScreenAvatarShape)
                            )
                            Text(
                                text = stringResource(R.string.loc_jane_doe),
                                style = MaterialTheme.typography.titleMediumEmphasized
                            )
                            Text(
                                text = stringResource(R.string.loc_1_555_123_4567),
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
                                checked = slot.id in order && !hidden.contains(slot.id),
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

        if (showDefaultBgDialog) {
            CallBackgroundDialog(
                title = stringResource(R.string.settings_call_default_background),
                icon = Icons.Outlined.Wallpaper,
                backgroundModel = defaultCallBg,
                supportingText = stringResource(R.string.settings_call_default_background_supporting),
                isUnknown = false,
                saving = savingDefaultBg,
                onDismissRequest = { showDefaultBgDialog = false },
                onChoose = {
                    showDefaultBgDialog = false
                    defaultCallBgLauncher.launch(arrayOf("image/*"))
                },
                onRemove = {
                    showDefaultBgDialog = false
                    CallBackgroundStore.clearDefault(context)
                    defaultCallBg = null
                }
            )
        }

        if (showUnknownBgDialog) {
            CallBackgroundDialog(
                title = stringResource(R.string.settings_call_unknown_background),
                icon = Icons.AutoMirrored.Outlined.ContactSupport,
                backgroundModel = unknownCallBg,
                supportingText = stringResource(R.string.settings_call_unknown_background_supporting),
                isUnknown = true,
                saving = savingUnknownBg,
                onDismissRequest = { showUnknownBgDialog = false },
                onChoose = {
                    showUnknownBgDialog = false
                    unknownCallBgLauncher.launch(arrayOf("image/*"))
                },
                onRemove = {
                    showUnknownBgDialog = false
                    CallBackgroundStore.clearUnknown(context)
                    unknownCallBg = null
                }
            )
        }
    }
}
