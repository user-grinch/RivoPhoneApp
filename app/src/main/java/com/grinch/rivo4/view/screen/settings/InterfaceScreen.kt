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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.view.components.RivoColorSwatchRow
import com.grinch.rivo4.view.components.RivoDivider
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoInteractiveRoundnessSlider
import com.grinch.rivo4.view.components.RivoListItem
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.grinch.rivo4.view.components.RivoVisualOptionSelectorRow
import com.grinch.rivo4.view.components.ScrollToTopButton
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AvatarSettingsScreenDestination
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
    val settingsState by prefs.settingsChanged.collectAsState()

    val showButton by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 1 }
    }

    var dynamicColors by remember(settingsState) {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, true))
    }
    var amoledMode by remember(settingsState) {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_AMOLED_MODE, false))
    }
    var transitionStyle by remember(settingsState) {
        mutableIntStateOf(prefs.getInt(PreferenceManager.KEY_TRANSITION_STYLE, 0))
    }
    var customPrimaryColor by remember(settingsState) {
        mutableIntStateOf(prefs.getInt("custom_primary_color", Color(0xFF6750A4).toArgb()))
    }
    var showCards by remember(settingsState) {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_CARDS, true))
    }
    var cardRoundness by remember(settingsState) {
        mutableIntStateOf(prefs.getInt(PreferenceManager.KEY_CARD_ROUNDNESS, 28).coerceAtLeast(5))
    }

    val presetColors = listOf(
        Color(0xFF6750A4), Color(0xFF0061A4), Color(0xFF006A60),
        Color(0xFF436916), Color(0xFF984061), Color(0xFF808080)
    )

    fun triggerThemeRestart() {
        (context as? Activity)?.recreate()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theme & Appearance", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Theme & Colors
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
                                triggerThemeRestart()
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
                                    triggerThemeRestart()
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
                                triggerThemeRestart()
                            }
                        )
                    }
                }

                // 2. Cards & Motion
                item {
                    RivoExpressiveCard(title = stringResource(R.string.settings_group_shape_motion)) {
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_interface_use_cards),
                            supporting = stringResource(R.string.settings_interface_use_cards_supporting),
                            leadingIcon = Icons.Outlined.ViewAgenda,
                            checked = showCards,
                            onCheckedChange = {
                                showCards = it
                                prefs.setBoolean(PreferenceManager.KEY_SHOW_CARDS, it)
                            }
                        )
                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                        RivoInteractiveRoundnessSlider(
                            headline = stringResource(R.string.settings_interface_card_roundness),
                            supporting = stringResource(R.string.settings_interface_card_roundness_supporting),
                            value = cardRoundness.toFloat().coerceIn(5f, 32f),
                            valueRange = 5f..32f,
                            steps = 26,
                            onValueChange = { cardRoundness = it.roundToInt().coerceAtLeast(5) },
                            onValueChangeFinished = {
                                prefs.setInt(PreferenceManager.KEY_CARD_ROUNDNESS, cardRoundness.coerceAtLeast(5))
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
                                triggerThemeRestart()
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
                                modifier = Modifier.size(20.dp),
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 3. Related Styling Links
                item {
                    RivoExpressiveCard(title = "More Display Settings") {
                        RivoListItem(
                            headline = "Navigation Bar",
                            supporting = "Floating bar style, blur effect, roundness & tab layout",
                            leadingIcon = Icons.Outlined.Dock,
                            onClick = { navigator.navigate(BottomNavScreenDestination) }
                        )
                        RivoDivider(Modifier.padding(horizontal = 16.dp))
                        RivoListItem(
                            headline = "Avatars & Contact Cards",
                            supporting = "11 avatar shapes, contact photos, initials & cards",
                            leadingIcon = Icons.Outlined.AccountCircle,
                            onClick = { navigator.navigate(AvatarSettingsScreenDestination) }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
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
