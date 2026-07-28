package com.grinch.rivo4.view.screen.settings

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.view.components.RivoDivider
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoSectionHeader
import com.grinch.rivo4.view.components.RivoSelectListItem
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.grinch.rivo4.view.components.ScrollToTopButton
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

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
        derivedStateOf {
            listState.firstVisibleItemIndex > 2
        }
    }
    
    var dynamicColors by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, true)) }
    var amoledMode by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_AMOLED_MODE, false)) }
    var flipBottomBar by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_FLIP_BOTTOM_NAV, defaultValue = false)) }
    var defaultBottomBar by remember { mutableStateOf(prefs.getInt(PreferenceManager.KEY_DEFAULT_BOTTOM_NAV, defaultValue = 0)) }
    var showFirstLetter by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_FIRST_LETTER, true)) }
    var colorfulAvatars by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_COLORFUL_AVATARS, true)) }
    var showPicture by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_PICTURE, true)) }
    var iconOnlyNav by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_ICON_ONLY_NAV, false)) }
    var roundAvatars by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_ROUND_AVATARS, true)) }
    var showDividers by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_DIVIDERS, true)) }
    var showCards by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_CARDS, true)) }
    var transitionStyle by remember { mutableStateOf(prefs.getInt(PreferenceManager.KEY_TRANSITION_STYLE, 0)) }
    var customPrimaryColor by remember { mutableStateOf(prefs.getInt("custom_primary_color", Color(0xFF6750A4).toArgb())) }
    var avatarShape by remember { mutableStateOf(prefs.getInt(PreferenceManager.KEY_AVATAR_SHAPE, 0)) }
    var showCallScreenAvatar by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_CALL_SCREEN_AVATAR, true)) }
    var searchMatchMode by remember { mutableStateOf(prefs.getInt(PreferenceManager.KEY_SEARCH_MATCH_MODE, 0)) }
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
                item {
                    RivoExpressiveCard {
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
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.settings_interface_primary_color), style = MaterialTheme.typography.labelLarge)
                                Spacer(Modifier.height(12.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(presetColors) { color ->
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .border(
                                                    width = if (customPrimaryColor == color.toArgb()) 3.dp else 0.dp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    customPrimaryColor = color.toArgb()
                                                    prefs.setInt("custom_primary_color", color.toArgb())
                                                    showRestartPrompt()
                                                }
                                        )
                                    }
                                }
                            }
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

                item {
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_interface_show_first_letter),
                            supporting = stringResource(R.string.settings_interface_show_first_letter_supporting),
                            leadingIcon = Icons.Outlined.Title,
                            checked = showFirstLetter,
                            onCheckedChange = {
                                showFirstLetter = it
                                prefs.setBoolean(PreferenceManager.KEY_SHOW_FIRST_LETTER, it)
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
                        RivoSelectListItem(
                            headline = stringResource(R.string.settings_interface_avatar_shape),
                            supporting = stringResource(R.string.settings_interface_avatar_shape_supporting),
                            leadingIcon = Icons.Outlined.AccountBox,
                            options = listOf(
                                stringResource(R.string.settings_interface_avatar_shape_squircle) to 0,
                                stringResource(R.string.settings_interface_avatar_shape_circle) to 1,
                                stringResource(R.string.settings_interface_avatar_shape_square) to 2
                            ),
                            selectedValue = avatarShape,
                            onValueChange = {
                                avatarShape = it
                                prefs.setInt(PreferenceManager.KEY_AVATAR_SHAPE, it)
                            }
                        )
                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_interface_call_screen_avatar),
                            supporting = stringResource(R.string.settings_interface_call_screen_avatar_supporting),
                            leadingIcon = Icons.Outlined.ContactPage,
                            checked = showCallScreenAvatar,
                            onCheckedChange = {
                                showCallScreenAvatar = it
                                prefs.setBoolean(PreferenceManager.KEY_SHOW_CALL_SCREEN_AVATAR, it)
                            }
                        )
                    }
                }

                item {
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_interface_show_dividers),
                            supporting = stringResource(R.string.settings_interface_show_dividers_supporting),
                            leadingIcon = Icons.Outlined.HorizontalRule,
                            checked = showDividers,
                            onCheckedChange = {
                                showDividers = it
                                prefs.setBoolean(PreferenceManager.KEY_SHOW_DIVIDERS, it)
                            }
                        )
                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_interface_expressive_cards),
                            supporting = stringResource(R.string.settings_interface_expressive_cards_supporting),
                            leadingIcon = Icons.Outlined.DashboardCustomize,
                            checked = showCards,
                            onCheckedChange = {
                                showCards = it
                                prefs.setBoolean(PreferenceManager.KEY_SHOW_CARDS, it)
                            }
                        )
                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                        RivoSelectListItem(
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
                            }
                        )
                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                        RivoSelectListItem(
                            headline = stringResource(R.string.settings_interface_search_matching_mode),
                            supporting = stringResource(R.string.settings_interface_search_matching_mode_supporting),
                            leadingIcon = Icons.Outlined.Search,
                            options = listOf(
                                stringResource(R.string.settings_interface_search_mode_t9_contains) to 0,
                                stringResource(R.string.settings_interface_search_mode_starts_with) to 1,
                                stringResource(R.string.settings_interface_search_mode_exact_match) to 2
                            ),
                            selectedValue = searchMatchMode,
                            onValueChange = {
                                searchMatchMode = it
                                prefs.setInt(PreferenceManager.KEY_SEARCH_MATCH_MODE, it)
                            }
                        )
                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                        RivoSelectListItem(
                            headline = stringResource(R.string.settings_interface_card_roundness),
                            supporting = stringResource(R.string.settings_interface_card_roundness_supporting),
                            leadingIcon = Icons.Outlined.CropFree,
                            options = listOf(
                                stringResource(R.string.settings_interface_roundness_extra_round) to 32,
                                stringResource(R.string.option_standard) to 28,
                                stringResource(R.string.settings_interface_roundness_rounded) to 20,
                                stringResource(R.string.settings_interface_roundness_semi_square) to 12,
                                stringResource(R.string.settings_interface_roundness_square) to 0
                            ),
                            selectedValue = cardRoundness,
                            onValueChange = {
                                cardRoundness = it
                                prefs.setInt(PreferenceManager.KEY_CARD_ROUNDNESS, it)
                            }
                        )
                    }
                }

                item {
                    RivoExpressiveCard {
                        RivoSelectListItem(
                            headline = stringResource(R.string.settings_interface_default_bottom_bar),
                            supporting = stringResource(R.string.settings_interface_default_bottom_bar_supporting),
                            leadingIcon = Icons.Outlined.SpaceDashboard,
                            options = listOf(
                                stringResource(R.string.nav_contacts) to 0,
                                stringResource(R.string.nav_recents) to 1,
                            ),
                            selectedValue = defaultBottomBar,
                            onValueChange = { selectedInt ->
                                defaultBottomBar = selectedInt
                                prefs.setInt(PreferenceManager.KEY_DEFAULT_BOTTOM_NAV, selectedInt)
                            }
                        )
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_interface_flip_bottom_bar),
                            supporting = stringResource(R.string.settings_interface_flip_bottom_bar_supporting),
                            leadingIcon = Icons.Outlined.SwapHoriz,
                            checked = flipBottomBar,
                            onCheckedChange = {
                                flipBottomBar = it
                                prefs.setBoolean(PreferenceManager.KEY_FLIP_BOTTOM_NAV, it)
                            }
                        )
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
                    }
                }
                
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
            
            ScrollToTopButton(
                visible = showButton,
                onClick = {
                    scope.launch {
                        listState.animateScrollToItem(0)
                    }
                }
            )
        }
    }
}
