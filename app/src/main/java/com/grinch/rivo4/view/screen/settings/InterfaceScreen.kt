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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.controller.util.PreferenceManager
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
    var transitionStyle by remember { mutableStateOf(prefs.getInt(PreferenceManager.KEY_TRANSITION_STYLE, 0)) }
    var customPrimaryColor by remember { mutableStateOf(prefs.getInt("custom_primary_color", Color(0xFF6750A4).toArgb())) }
    var keepScreenOn by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_KEEP_SCREEN_ON, true)) }
    var avatarShape by remember { mutableStateOf(prefs.getInt(PreferenceManager.KEY_AVATAR_SHAPE, 0)) }
    var searchMatchMode by remember { mutableStateOf(prefs.getInt(PreferenceManager.KEY_SEARCH_MATCH_MODE, 0)) }
    var showSimIconHistory by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_SIM_ICON_HISTORY, true)) }

    val presetColors = listOf(
        Color(0xFF6750A4), Color(0xFF0061A4), Color(0xFF006A60), Color(0xFF436916),
        Color(0xFF984061), Color(0xFF006874), Color(0xFF705D00), Color(0xFFBF0031)
    )

    fun showRestartPrompt() {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Restart required to apply theme changes fully.",
                actionLabel = "Restart",
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
                title = { Text("User Interface", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                            headline = "Material You theming",
                            supporting = "Wallpaper based app color theming.",
                            leadingIcon = Icons.Outlined.Palette,
                            checked = dynamicColors,
                            onCheckedChange = {
                                dynamicColors = it
                                prefs.setBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, it)
                                showRestartPrompt()
                            }
                        )
                        
                        if (!dynamicColors) {
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Primary Color", style = MaterialTheme.typography.labelLarge)
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
                        
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        RivoSwitchListItem(
                            headline = "Amoled dark mode",
                            supporting = "Uses pitch black for UI elements.",
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
                            headline = "Show first letter in avatar",
                            supporting = "Displays letter when picture is missing",
                            leadingIcon = Icons.Outlined.Title,
                            checked = showFirstLetter,
                            onCheckedChange = {
                                showFirstLetter = it
                                prefs.setBoolean(PreferenceManager.KEY_SHOW_FIRST_LETTER, it)
                            }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        RivoSwitchListItem(
                            headline = "Use colorful avatars",
                            supporting = "Random colors based on contact name",
                            leadingIcon = Icons.Outlined.Palette,
                            checked = colorfulAvatars,
                            onCheckedChange = {
                                colorfulAvatars = it
                                prefs.setBoolean(PreferenceManager.KEY_COLORFUL_AVATARS, it)
                            }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        RivoSwitchListItem(
                            headline = "Show picture in avatar",
                            supporting = "Shows the contact picture if available",
                            leadingIcon = Icons.Outlined.AccountCircle,
                            checked = showPicture,
                            onCheckedChange = {
                                showPicture = it
                                prefs.setBoolean(PreferenceManager.KEY_SHOW_PICTURE, it)
                            }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        RivoSelectListItem(
                            headline = "Avatar shape",
                            supporting = "Choose the shape for contact avatars",
                            leadingIcon = Icons.Outlined.AccountBox,
                            options = listOf(
                                "Squircle" to 0,
                                "Circle" to 1,
                                "Square" to 2
                            ),
                            selectedValue = avatarShape,
                            onValueChange = {
                                avatarShape = it
                                prefs.setInt(PreferenceManager.KEY_AVATAR_SHAPE, it)
                            }
                        )
                    }
                }

                item {
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "Show dividers",
                            supporting = "Display lines between list items",
                            leadingIcon = Icons.Outlined.HorizontalRule,
                            checked = showDividers,
                            onCheckedChange = {
                                showDividers = it
                                prefs.setBoolean(PreferenceManager.KEY_SHOW_DIVIDERS, it)
                            }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        RivoSelectListItem(
                            headline = "Transition animation",
                            supporting = "Style of animations between screens",
                            leadingIcon = Icons.Outlined.Animation,
                            options = listOf(
                                "Standard" to 0,
                                "Slide" to 1,
                                "Fade" to 2,
                                "None" to 3
                            ),
                            selectedValue = transitionStyle,
                            onValueChange = {
                                transitionStyle = it
                                prefs.setInt(PreferenceManager.KEY_TRANSITION_STYLE, it)
                            }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        RivoSwitchListItem(
                            headline = "Keep Screen On during Call",
                            supporting = "Prevent screen timeout when inside the call UI",
                            leadingIcon = Icons.Outlined.LightMode,
                            checked = keepScreenOn,
                            onCheckedChange = {
                                keepScreenOn = it
                                prefs.setBoolean(PreferenceManager.KEY_KEEP_SCREEN_ON, it)
                            }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        RivoSelectListItem(
                            headline = "Search Matching Mode",
                            supporting = "Behavior of contacts filtering algorithm",
                            leadingIcon = Icons.Outlined.Search,
                            options = listOf(
                                "T9 & Contains" to 0,
                                "Starts With" to 1,
                                "Exact Match" to 2
                            ),
                            selectedValue = searchMatchMode,
                            onValueChange = {
                                searchMatchMode = it
                                prefs.setInt(PreferenceManager.KEY_SEARCH_MATCH_MODE, it)
                            }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        RivoSwitchListItem(
                            headline = "Show SIM Icon in History",
                            supporting = "Display carrier label next to call entries",
                            leadingIcon = Icons.Outlined.SimCard,
                            checked = showSimIconHistory,
                            onCheckedChange = {
                                showSimIconHistory = it
                                prefs.setBoolean(PreferenceManager.KEY_SHOW_SIM_ICON_HISTORY, it)
                            }
                        )
                    }
                }

                item {
                    RivoExpressiveCard {
                        RivoSelectListItem(
                            headline = "Default bottom bar",
                            supporting = "Select which tab opens initially",
                            leadingIcon = Icons.Outlined.SpaceDashboard,
                            options = listOf(
                                "Contacts" to 0,
                                "Recents" to 1,
                            ),
                            selectedValue = defaultBottomBar,
                            onValueChange = { selectedInt ->
                                defaultBottomBar = selectedInt
                                prefs.setInt(PreferenceManager.KEY_DEFAULT_BOTTOM_NAV, selectedInt)
                            }
                        )
                        RivoSwitchListItem(
                            headline = "Flip bottom bar",
                            supporting = "Flips the position of the contacts & recents tabs",
                            leadingIcon = Icons.Outlined.SwapHoriz,
                            checked = flipBottomBar,
                            onCheckedChange = {
                                flipBottomBar = it
                                prefs.setBoolean(PreferenceManager.KEY_FLIP_BOTTOM_NAV, it)
                            }
                        )
                        RivoSwitchListItem(
                            headline = "Icon-only bottom bar",
                            supporting = "Removes text labels from navigation",
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
