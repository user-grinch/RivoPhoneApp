package com.grinch.rivo4.view.screen.settings

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.view.components.RivoAvatarShapeSelectorRow
import com.grinch.rivo4.view.components.RivoInteractiveRoundnessSlider
import com.grinch.rivo4.view.components.RivoVisualOptionSelectorRow
import com.grinch.rivo4.view.components.RivoColorSwatchRow
import com.grinch.rivo4.view.components.RivoDivider
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoListItem
import com.grinch.rivo4.view.components.RivoOptionRow
import com.grinch.rivo4.view.components.RivoSliderListItem
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.grinch.rivo4.view.components.ScrollToTopButton
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.BottomNavScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun InterfaceScreen(
    navigator: DestinationsNavigator
) {
    val prefs = koinInject<PreferenceManager>()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val showButton by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 1 }
    }

    var dynamicColors by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, true)) }
    var amoledMode by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_AMOLED_MODE, false)) }
    var defaultBottomBar by remember { mutableStateOf(prefs.getInt(PreferenceManager.KEY_DEFAULT_BOTTOM_NAV, PreferenceManager.TAB_RECENTS)) }
    var mergeFavorites by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_MERGE_FAVORITES_RECENTS, true)) }
    var colorfulAvatars by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_COLORFUL_AVATARS, true)) }
    var gradientAvatars by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_GRADIENT_AVATARS, false)) }
    var showPicture by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_PICTURE, true)) }
    var iconOnlyNav by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_ICON_ONLY_NAV, false)) }
    var transitionStyle by remember { mutableStateOf(prefs.getInt(PreferenceManager.KEY_TRANSITION_STYLE, 0)) }
    var customPrimaryColor by remember { mutableStateOf(prefs.getInt("custom_primary_color", Color(0xFF6750A4).toArgb())) }
    var avatarShape by remember { mutableStateOf(prefs.getInt(PreferenceManager.KEY_AVATAR_SHAPE, 0)) }
    var showCallScreenAvatar by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_CALL_SCREEN_AVATAR, true)) }
    var cardRoundness by remember { mutableStateOf(prefs.getInt(PreferenceManager.KEY_CARD_ROUNDNESS, 28)) }

    val presetColors = listOf(
        Color(0xFF6750A4), Color(0xFF0061A4), Color(0xFF006A60),
        Color(0xFF436916), Color(0xFF984061), Color(0xFF808080)
    )

    val restartRequiredMessage = stringResource(R.string.settings_interface_restart_required)
    val restartActionLabel = stringResource(R.string.settings_interface_restart_action)

    fun showRestartPrompt() {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = restartRequiredMessage,
                actionLabel = restartActionLabel,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                (context as? Activity)?.recreate()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_interface_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ---- Colour ----
                item {
                    RivoExpressiveCard(title = stringResource(R.string.settings_group_color)) {
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_interface_material_you),
                            supporting = stringResource(R.string.settings_interface_material_you_supporting),
                            leadingIcon = Icons.Outlined.Palette,
                            checked = dynamicColors,
                            onCheckedChange = {
                                dynamicColors = it
                                prefs.setBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, it)
                                showRestartPrompt()
                            }
                        )

                        if (!dynamicColors) {
                            RivoDivider(Modifier.padding(horizontal = 16.dp))
                            RivoColorSwatchRow(
                                colors = presetColors,
                                selectedColor = presetColors.firstOrNull { it.toArgb() == customPrimaryColor },
                                onColorSelected = { color ->
                                    customPrimaryColor = color.toArgb()
                                    prefs.setInt("custom_primary_color", color.toArgb())
                                    showRestartPrompt()
                                }
                            )
                        }

                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_interface_amoled),
                            supporting = stringResource(R.string.settings_interface_amoled_supporting),
                            leadingIcon = Icons.Outlined.DarkMode,
                            checked = amoledMode,
                            onCheckedChange = {
                                amoledMode = it
                                prefs.setBoolean(PreferenceManager.KEY_AMOLED_MODE, it)
                                showRestartPrompt()
                            }
                        )
                    }
                }

                // ---- Avatars ----
                item {
                    RivoExpressiveCard(title = stringResource(R.string.settings_group_avatars)) {
                        RivoAvatarShapeSelectorRow(
                            headline = stringResource(R.string.settings_interface_avatar_shape),
                            supporting = stringResource(R.string.settings_interface_avatar_shape_supporting),
                            options = listOf(
                                stringResource(R.string.settings_interface_avatar_shape_squircle) to 0,
                                stringResource(R.string.settings_interface_avatar_shape_circle) to 1,
                                stringResource(R.string.settings_interface_avatar_shape_square) to 2,
                                stringResource(R.string.settings_interface_avatar_shape_cookie) to 3,
                                stringResource(R.string.settings_interface_avatar_shape_clover) to 4,
                                stringResource(R.string.settings_interface_avatar_shape_arch) to 5,
                                stringResource(R.string.settings_interface_avatar_shape_pill) to 6,
                                stringResource(R.string.settings_interface_avatar_shape_gem) to 7,
                                stringResource(R.string.settings_interface_avatar_shape_sunny) to 8,
                                stringResource(R.string.settings_interface_avatar_shape_heart) to 9,
                                stringResource(R.string.settings_interface_avatar_shape_burst) to 10
                            ),
                            selectedValue = avatarShape,
                            onValueChange = { selected ->
                                avatarShape = selected
                                prefs.setInt(PreferenceManager.KEY_AVATAR_SHAPE, selected)
                            }
                        )
                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_interface_show_picture),
                            supporting = stringResource(R.string.settings_interface_show_picture_supporting),
                            leadingIcon = Icons.Outlined.AccountCircle,
                            checked = showPicture,
                            onCheckedChange = {
                                showPicture = it
                                prefs.setBoolean(PreferenceManager.KEY_SHOW_PICTURE, it)
                            }
                        )
                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_interface_colorful_avatars),
                            supporting = stringResource(R.string.settings_interface_colorful_avatars_supporting),
                            leadingIcon = Icons.Outlined.Palette,
                            checked = colorfulAvatars,
                            onCheckedChange = {
                                colorfulAvatars = it
                                prefs.setBoolean(PreferenceManager.KEY_COLORFUL_AVATARS, it)
                            }
                        )
                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_interface_gradient_avatars),
                            supporting = stringResource(R.string.settings_interface_gradient_avatars_supporting),
                            leadingIcon = Icons.Outlined.Gradient,
                            checked = gradientAvatars,
                            onCheckedChange = {
                                gradientAvatars = it
                                prefs.setBoolean(PreferenceManager.KEY_GRADIENT_AVATARS, it)
                            }
                        )
                    }
                }

                item {
                    RivoExpressiveCard(title = stringResource(R.string.settings_group_shape_motion)) {
                        RivoInteractiveRoundnessSlider(
                            headline = stringResource(R.string.settings_interface_card_roundness),
                            supporting = stringResource(R.string.settings_interface_card_roundness_supporting),
                            value = cardRoundness.toFloat().coerceAtLeast(1f),
                            valueRange = 1f..32f,
                            steps = 7,
                            onValueChange = { cardRoundness = it.roundToInt() },
                            onValueChangeFinished = {
                                prefs.setInt(PreferenceManager.KEY_CARD_ROUNDNESS, cardRoundness)
                            }
                        )
                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                        RivoVisualOptionSelectorRow(
                            headline = stringResource(R.string.settings_interface_transition_animation),
                            supporting = stringResource(R.string.settings_interface_transition_animation_supporting),
                            leadingIcon = Icons.Outlined.Animation,
                            options = listOf(
                                stringResource(R.string.option_standard) to 0,
                                stringResource(R.string.settings_interface_transition_slide) to 1,
                                stringResource(R.string.settings_interface_transition_fade) to 2,
                                stringResource(R.string.settings_interface_transition_none) to 3
                            ),
                            selectedValue = transitionStyle,
                            onValueChange = {
                                transitionStyle = it
                                prefs.setInt(PreferenceManager.KEY_TRANSITION_STYLE, it)
                                showRestartPrompt()
                            }
                        ) { value, selected ->
                            val icon = when (value) {
                                0 -> Icons.Outlined.Animation
                                1 -> Icons.Outlined.CompareArrows
                                2 -> Icons.Outlined.AutoAwesome
                                else -> Icons.Outlined.Block
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ---- Navigation ----
                item {
                    RivoExpressiveCard(title = stringResource(R.string.settings_group_navigation)) {
                        RivoVisualOptionSelectorRow(
                            headline = stringResource(R.string.settings_interface_default_bottom_bar),
                            supporting = stringResource(R.string.settings_interface_default_bottom_bar_supporting),
                            leadingIcon = Icons.Outlined.SpaceDashboard,
                            options = listOf(
                                stringResource(R.string.nav_recents) to 0,
                                stringResource(R.string.nav_favorites) to 1,
                                stringResource(R.string.nav_contacts) to 2
                            ),
                            selectedValue = defaultBottomBar,
                            onValueChange = {
                                defaultBottomBar = it
                                prefs.setInt(PreferenceManager.KEY_DEFAULT_BOTTOM_NAV, it)
                            }
                        ) { value, selected ->
                            val icon = defaultTabIcon(value) ?: Icons.Outlined.SpaceDashboard
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                        RivoListItem(
                            headline = stringResource(R.string.settings_bottom_nav_title),
                            supporting = stringResource(R.string.settings_bottom_nav_supporting),
                            leadingIcon = Icons.Outlined.SwapHoriz,
                            onClick = { navigator.navigate(BottomNavScreenDestination) }
                        )
                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_interface_icon_only_bar),
                            supporting = stringResource(R.string.settings_interface_icon_only_bar_supporting),
                            leadingIcon = Icons.Outlined.ViewStream,
                            checked = iconOnlyNav,
                            onCheckedChange = {
                                iconOnlyNav = it
                                prefs.setBoolean(PreferenceManager.KEY_ICON_ONLY_NAV, it)
                            }
                        )
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

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }

            ScrollToTopButton(
                visible = showButton,
                onClick = {
                    scope.launch { listState.animateScrollToItem(0) }
                }
            )
        }
    }
}

private fun defaultTabIcon(tab: Int): ImageVector = when (tab) {
    PreferenceManager.TAB_FAVORITES -> Icons.Outlined.Star
    PreferenceManager.TAB_CONTACTS -> Icons.Outlined.Person
    else -> Icons.Outlined.History
}
