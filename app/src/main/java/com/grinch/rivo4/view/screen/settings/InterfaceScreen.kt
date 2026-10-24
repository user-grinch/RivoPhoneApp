package com.grinch.rivo4.view.screen.settings
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import com.grinch.rivo4.view.components.MenuTopAppBar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.grinch.rivo4.view.components.RivoDialog
import com.grinch.rivo4.view.components.RivoDialogAction
import com.grinch.rivo4.view.components.RivoElevation
import com.grinch.rivo4.view.theme.RivoMaterialShapes
import com.grinch.rivo4.view.theme.RivoMotion
import com.grinch.rivo4.view.theme.rememberRivoMorphShape
import com.grinch.rivo4.view.theme.rivoPolygonShape
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import com.grinch.rivo4.view.theme.LocalCardRoundness
import com.grinch.rivo4.view.theme.RivoShapeDefaults
import com.grinch.rivo4.view.theme.rivoCornerDp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.view.components.RivoColorSwatchRow
import com.grinch.rivo4.view.components.RivoDivider
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoExpressiveGroup
import com.grinch.rivo4.view.components.RivoInteractiveRoundnessSlider
import com.grinch.rivo4.view.components.RivoListItem
import com.grinch.rivo4.view.components.RivoLeadingIconTile
import com.grinch.rivo4.view.components.RivoSliderListItem
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.grinch.rivo4.view.components.RivoVisualOptionSelectorRow
import com.grinch.rivo4.view.components.ScrollToTopButton
import com.grinch.rivo4.view.theme.RivoPaletteStyle
import com.grinch.rivo4.view.theme.rivoPaletteSwatches
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AvatarSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.BottomNavScreenDestination
import com.ramcosta.composedestinations.generated.destinations.CallScreenCustomizationScreenDestination
import com.ramcosta.composedestinations.generated.destinations.CallLogCustomizationScreenDestination
import com.ramcosta.composedestinations.generated.destinations.TypographyScreenDestination
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.grinch.rivo4.controller.util.SettingsHighlightManager
import kotlinx.coroutines.delay
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
    var showColorPickerDialog by remember { mutableStateOf(false) }
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
    var transitionTimeScale by remember(settingsState) {
        mutableFloatStateOf(prefs.getTransitionTimeScale())
    }
    var predictiveBackEnabled by remember(settingsState) {
        mutableStateOf(prefs.isPredictiveBackEnabled())
    }
    var customPrimaryColor by remember(settingsState) {
        mutableIntStateOf(prefs.getInt("custom_primary_color", Color(0xFF6750A4).toArgb()))
    }
    var cardRoundness by remember(settingsState) {
        mutableIntStateOf(prefs.getInt(PreferenceManager.KEY_CARD_ROUNDNESS, 28).coerceAtLeast(5))
    }
    var dualSimButtons by remember(settingsState) {
        mutableStateOf(prefs.isDualSimDialpadButtonsEnabled())
    }
    var themeMode by remember(settingsState) {
        mutableIntStateOf(prefs.getInt(PreferenceManager.KEY_THEME_MODE, PreferenceManager.THEME_MODE_SYSTEM))
    }
    var paletteVariant by remember(settingsState) {
        mutableStateOf(
            prefs.getString(PreferenceManager.KEY_DYNAMIC_VARIANT, RivoPaletteStyle.TonalSpot.id)
                ?: RivoPaletteStyle.TonalSpot.id
        )
    }

    val systemDark = isSystemInDarkTheme()
    val paletteSeed = remember(dynamicColors, customPrimaryColor, systemDark) {
        if (dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (systemDark) {
                androidx.compose.material3.dynamicDarkColorScheme(context).primary.toArgb()
            } else {
                androidx.compose.material3.dynamicLightColorScheme(context).primary.toArgb()
            }
        } else {
            customPrimaryColor
        }
    }
    val paletteSwatches = remember(dynamicColors, paletteSeed, systemDark) {
        if (!dynamicColors) emptyMap()
        else RivoPaletteStyle.entries.associateWith { style ->
            rivoPaletteSwatches(paletteSeed, style, systemDark)
        }
    }

    val presetColors = listOf(
        Color(0xFF6750A4), Color(0xFF0061A4), Color(0xFF006A60),
        Color(0xFF436916), Color(0xFF984061), Color(0xFF808080)
    )

    fun triggerThemeRestart() {
        // Rivo4Theme and MainActivity observe prefs.settingsChanged reactively;
        // no Activity recreation is needed.
    }

    LaunchedEffect(SettingsHighlightManager.highlightEpoch) {
        if (SettingsHighlightManager.highlightEpoch == 0L) return@LaunchedEffect
        val target = SettingsHighlightManager.highlightedSettingTitle ?: return@LaunchedEffect
        val targetIndex = when {
            SettingsHighlightManager.matches(context.getString(R.string.settings_interface_material_you)) ||
            SettingsHighlightManager.matches(context.getString(R.string.settings_interface_amoled)) ||
            target.contains("Material You", ignoreCase = true) ||
            target.contains("Dynamic Color", ignoreCase = true) ||
            target.contains("AMOLED", ignoreCase = true) -> 0

            SettingsHighlightManager.matches(context.getString(R.string.settings_interface_card_roundness)) ||
            SettingsHighlightManager.matches(context.getString(R.string.settings_interface_transition_animation)) ||
            SettingsHighlightManager.matches(context.getString(R.string.settings_predictive_back_title)) ||
            target.contains("Card", ignoreCase = true) ||
            target.contains("Roundness", ignoreCase = true) ||
            target.contains("Transition", ignoreCase = true) ||
            target.contains("Predictive", ignoreCase = true) -> 1

            SettingsHighlightManager.matches(context.getString(R.string.settings_interface_dual_sim_buttons_title)) ||
            target.contains("Dual SIM", ignoreCase = true) -> 2

            target.contains("Navigation Bar", ignoreCase = true) ||
            target.contains("Avatar", ignoreCase = true) -> 3

            else -> -1
        }
        if (targetIndex >= 0) {
            delay(150L)
            listState.animateScrollToItem(targetIndex)
        }
    }

    Scaffold(
        topBar = {
            MenuTopAppBar(
                text = stringResource(R.string.settings_interface_title),
                navigator = navigator
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 1. Theme & Colors
                item {
                    RivoExpressiveGroup(title = stringResource(R.string.settings_group_color), icon = Icons.Outlined.Palette) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RivoLeadingIconTile(icon = Icons.Outlined.Contrast)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.theme_mode),
                                            style = MaterialTheme.typography.titleSmallEmphasized
                                        )
                                        Text(
                                            text = stringResource(R.string.theme_mode_supporting),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val roundness = LocalCardRoundness.current
                                    listOf(
                                        Triple(PreferenceManager.THEME_MODE_SYSTEM, Icons.Outlined.Contrast, R.string.system),
                                        Triple(PreferenceManager.THEME_MODE_LIGHT, Icons.Outlined.LightMode, R.string.light),
                                        Triple(PreferenceManager.THEME_MODE_DARK, Icons.Outlined.DarkMode, R.string.dark)
                                    ).forEach { (mode, icon, labelRes) ->
                                        val selected = themeMode == mode
                                        Surface(
                                            selected = selected,
                                            onClick = {
                                                themeMode = mode
                                                prefs.setInt(PreferenceManager.KEY_THEME_MODE, mode)
                                            },
                                            shape = RoundedCornerShape(
                                                rivoCornerDp(
                                                    if (selected) RivoShapeDefaults.BaseExtraLarge else RivoShapeDefaults.BaseLargeIncreased,
                                                    roundness
                                                )
                                            ),
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerHigh
                                            },
                                            contentColor = if (selected) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            shadowElevation = RivoElevation.Flat,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(52.dp)
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = stringResource(labelRes),
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item {
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
                        }
                        if (dynamicColors) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.palette_variant_title),
                                        style = MaterialTheme.typography.labelLargeEmphasized,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        RivoPaletteStyle.entries.forEach { style ->
                                            RivoPaletteChip(
                                                label = stringResource(paletteStyleLabel(style)),
                                                swatches = paletteSwatches[style].orEmpty(),
                                                selected = paletteVariant == style.id,
                                                onClick = {
                                                    paletteVariant = style.id
                                                    prefs.setString(PreferenceManager.KEY_DYNAMIC_VARIANT, style.id)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (!dynamicColors) {
                            item {
                                Box(modifier = Modifier.padding(vertical = 4.dp)) {
                                    val isCustomActive = presetColors.none { it.toArgb() == customPrimaryColor }
                                    RivoColorSwatchRow(
                                        colors = presetColors,
                                        selectedColor = if (!isCustomActive) presetColors.firstOrNull { it.toArgb() == customPrimaryColor } else null,
                                        onColorSelected = { color ->
                                            customPrimaryColor = color.toArgb()
                                            prefs.setInt("custom_primary_color", color.toArgb())
                                            triggerThemeRestart()
                                        },
                                        trailingContent = {
                                            val progress by animateFloatAsState(
                                                targetValue = if (isCustomActive) 1f else 0f,
                                                animationSpec = RivoMotion.shapeMorph(),
                                                label = "CustomColorSwatchMorph"
                                            )
                                            val swatchShape = rememberRivoMorphShape(
                                                RivoMaterialShapes.Circle,
                                                RivoMaterialShapes.Cookie9Sided
                                            ) { progress }

                                            val customColor = remember(customPrimaryColor) { Color(customPrimaryColor) }
                                            val containerColor = if (isCustomActive) customColor else MaterialTheme.colorScheme.surfaceContainerHigh
                                            val contentColor = if (isCustomActive) {
                                                if (customColor.luminance() > 0.5f) MaterialTheme.colorScheme.scrim else MaterialTheme.colorScheme.surface
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            }

                                            Surface(
                                                selected = isCustomActive,
                                                onClick = { showColorPickerDialog = true },
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .semantics {
                                                        contentDescription = context.getString(R.string.content_desc_custom_color_picker)
                                                    },
                                                shape = swatchShape,
                                                color = containerColor,
                                                contentColor = contentColor,
                                                border = if (!isCustomActive) BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                                                shadowElevation = RivoElevation.Flat
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    if (isCustomActive) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = stringResource(R.string.content_desc_selected_item),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Colorize,
                                                            contentDescription = context.getString(R.string.content_desc_pick_custom_color),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        item {
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
                }

                // 2. Cards & Motion
                item {
                    RivoExpressiveGroup(title = stringResource(R.string.settings_group_shape_motion), icon = Icons.Outlined.ViewAgenda) {
                        item {
                            Box(modifier = Modifier.padding(vertical = 4.dp)) {
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
                            }
                        }
                        item {
                            Box(modifier = Modifier.padding(vertical = 4.dp)) {
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
                                    }
                                ) { value, selected ->
                                    val icon = when (value) {
                                        0 -> Icons.Outlined.Animation
                                        1 -> Icons.AutoMirrored.Outlined.CompareArrows
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
                        if (transitionStyle != 3) {
                            item {
                                val snappedScale = ((transitionTimeScale * 20f).roundToInt() / 20f).coerceIn(0.25f, 5.0f)
                                val scaleText = if (snappedScale * 100 % 10 == 0f) {
                                    String.format(java.util.Locale.US, "%.1fx", snappedScale)
                                } else {
                                    String.format(java.util.Locale.US, "%.2fx", snappedScale)
                                }
                                RivoSliderListItem(
                                    headline = stringResource(R.string.settings_transition_speed),
                                    supporting = stringResource(R.string.settings_transition_speed_supporting),
                                    leadingIcon = Icons.Outlined.Speed,
                                    value = transitionTimeScale.coerceIn(0.25f, 5.0f),
                                    valueRange = 0.25f..5.0f,
                                    steps = 18,
                                    valueLabel = scaleText,
                                    onValueChange = { raw ->
                                        transitionTimeScale = ((raw * 4f).roundToInt() / 4f).coerceIn(0.25f, 5.0f)
                                    },
                                    onValueChangeFinished = {
                                        prefs.setTransitionTimeScale(transitionTimeScale)
                                    }
                                )
                            }
                        }
                        item {
                            RivoSwitchListItem(
                                headline = stringResource(R.string.settings_predictive_back_title),
                                supporting = stringResource(R.string.settings_predictive_back_supporting),
                                leadingIcon = Icons.AutoMirrored.Outlined.ArrowBack,
                                checked = predictiveBackEnabled,
                                onCheckedChange = {
                                    predictiveBackEnabled = it
                                    prefs.setPredictiveBackEnabled(it)
                                }
                            )
                        }
                    }
                }

                // 3. Dialpad Interface
                item {
                    RivoExpressiveGroup(title = stringResource(R.string.settings_group_dialpad_calling), icon = Icons.Outlined.SimCard) {
                        item {
                            RivoSwitchListItem(
                                headline = stringResource(R.string.settings_interface_dual_sim_buttons_title),
                                supporting = stringResource(R.string.settings_interface_dual_sim_buttons_supporting),
                                leadingIcon = Icons.Outlined.SimCard,
                                checked = dualSimButtons,
                                onCheckedChange = {
                                    dualSimButtons = it
                                    prefs.setDualSimDialpadButtonsEnabled(it)
                                }
                            )
                        }
                    }
                }

                // 5. Related Styling Links
                item {
                    RivoExpressiveGroup(
                        title = stringResource(R.string.settings_more_display),
                        icon = Icons.Outlined.DisplaySettings
                    ) {
                        item {
                            RivoListItem(
                                headline = stringResource(R.string.typography_title),
                                supporting = stringResource(R.string.typography_entry_supporting),
                                leadingIcon = Icons.Outlined.TextFields,
                                onClick = { navigator.navigate(TypographyScreenDestination) }
                            )
                        }
                        item {
                            RivoListItem(
                                headline = stringResource(R.string.settings_navigation_bar),
                                supporting = stringResource(R.string.settings_navigation_bar_supporting),
                                leadingIcon = Icons.Outlined.Dock,
                                onClick = { navigator.navigate(BottomNavScreenDestination) }
                            )
                        }
                        item {
                            RivoListItem(
                                headline = stringResource(R.string.settings_avatars_cards),
                                supporting = stringResource(R.string.settings_avatars_cards_supporting),
                                leadingIcon = Icons.Outlined.AccountCircle,
                                onClick = { navigator.navigate(AvatarSettingsScreenDestination) }
                            )
                        }
                        item {
                            RivoListItem(
                                headline = stringResource(R.string.call_screen_customization_title),
                                supporting = stringResource(R.string.call_screen_customization_supporting),
                                leadingIcon = Icons.Outlined.Dialpad,
                                onClick = { navigator.navigate(CallScreenCustomizationScreenDestination) }
                            )
                        }
                        item {
                            RivoListItem(
                                headline = stringResource(R.string.settings_call_log_customization_title),
                                supporting = stringResource(R.string.settings_call_log_customization_supporting),
                                leadingIcon = Icons.Outlined.History,
                                onClick = { navigator.navigate(CallLogCustomizationScreenDestination) }
                            )
                        }
                    }
                }

                item {
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

        if (showColorPickerDialog) {
            RivoColorPickerDialog(
                initialColor = Color(customPrimaryColor),
                onDismissRequest = { showColorPickerDialog = false },
                onColorSelected = { color ->
                    customPrimaryColor = color.toArgb()
                    prefs.setInt("custom_primary_color", color.toArgb())
                    triggerThemeRestart()
                }
            )
        }
    }
}

@StringRes
private fun paletteStyleLabel(style: RivoPaletteStyle): Int = when (style) {
    RivoPaletteStyle.TonalSpot -> R.string.palette_tonal_spot
    RivoPaletteStyle.Expressive -> R.string.palette_expressive
    RivoPaletteStyle.Neutral -> R.string.palette_neutral
    RivoPaletteStyle.Vibrant -> R.string.palette_vibrant
    RivoPaletteStyle.FruitSalad -> R.string.palette_fruit_salad
    RivoPaletteStyle.Rainbow -> R.string.palette_rainbow
    RivoPaletteStyle.Content -> R.string.palette_content
    RivoPaletteStyle.Fidelity -> R.string.palette_fidelity
    RivoPaletteStyle.Monochrome -> R.string.palette_monochrome
    RivoPaletteStyle.BigClock -> R.string.palette_big_clock
    RivoPaletteStyle.Candy -> R.string.palette_candy
    RivoPaletteStyle.DeepOcean -> R.string.palette_deep_ocean
    RivoPaletteStyle.SunsetGlow -> R.string.palette_sunset_glow
}

@Composable
private fun RivoPaletteChip(
    label: String,
    swatches: List<Color>,
    selected: Boolean,
    onClick: () -> Unit
) {
    val roundness = LocalCardRoundness.current
    Surface(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(rivoCornerDp(RivoShapeDefaults.BaseLargeIncreased, roundness)),
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        shadowElevation = RivoElevation.Flat
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.size(width = 30.dp, height = 16.dp)) {
                swatches.take(3).forEachIndexed { index, color ->
                    Surface(
                        modifier = Modifier
                            .offset(x = (index * 7).dp)
                            .size(16.dp),
                        shape = CircleShape,
                        color = color,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    ) {}
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun RivoCircularColorWheel(
    hue: Float,
    saturation: Float,
    value: Float,
    onColorChange: (hue: Float, saturation: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(200.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val dx = offset.x - centerX
                    val dy = offset.y - centerY
                    val radius = size.width / 2f
                    val dist = hypot(dx, dy).coerceAtMost(radius)
                    val sat = (dist / radius).coerceIn(0f, 1f)
                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    if (angle < 0f) angle += 360f
                    onColorChange(angle, sat)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val dx = change.position.x - centerX
                    val dy = change.position.y - centerY
                    val radius = size.width / 2f
                    val dist = hypot(dx, dy).coerceAtMost(radius)
                    val sat = (dist / radius).coerceIn(0f, 1f)
                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    if (angle < 0f) angle += 360f
                    onColorChange(angle, sat)
                }
            }
    ) {
        val currentColor = remember(hue, saturation, value) {
            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f

            // Sweep hue gradient around circle
            val sweepGradient = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFFFF0000),
                    Color(0xFFFFFF00),
                    Color(0xFF00FF00),
                    Color(0xFF00FFFF),
                    Color(0xFF0000FF),
                    Color(0xFFFF00FF),
                    Color(0xFFFF0000)
                ),
                center = center
            )
            drawCircle(brush = sweepGradient, radius = radius, center = center)

            // Radial gradient for saturation (white at center to transparent at edge)
            val saturationGradient = Brush.radialGradient(
                colors = listOf(Color.White, Color.Transparent),
                center = center,
                radius = radius
            )
            drawCircle(brush = saturationGradient, radius = radius, center = center)

            // Dark overlay for brightness / value
            if (value < 1f) {
                drawCircle(
                    color = Color.Black.copy(alpha = 1f - value),
                    radius = radius,
                    center = center
                )
            }

            // Outer border
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Selector thumb
            val angleRad = Math.toRadians(hue.toDouble())
            val thumbDist = saturation * radius
            val thumbX = center.x + (thumbDist * cos(angleRad)).toFloat()
            val thumbY = center.y + (thumbDist * sin(angleRad)).toFloat()
            val thumbCenter = Offset(thumbX, thumbY)

            // Outer white ring
            drawCircle(
                color = Color.White,
                radius = 11.dp.toPx(),
                center = thumbCenter
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.4f),
                radius = 11.dp.toPx(),
                center = thumbCenter,
                style = Stroke(width = 2.dp.toPx())
            )
            // Color fill
            drawCircle(
                color = currentColor,
                radius = 8.dp.toPx(),
                center = thumbCenter
            )
        }
    }
}

@Composable
fun RivoColorPickerDialog(
    initialColor: Color,
    onDismissRequest: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    val initialHsv = remember(initialColor) {
        val array = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), array)
        array
    }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    val currentColor = remember(hue, saturation, value) {
        val colorInt = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
        Color(colorInt)
    }

    val quickColors = remember {
        listOf(
            Color(0xFFE91E63),
            Color(0xFFFF5722),
            Color(0xFFFF9800),
            Color(0xFFFFC107),
            Color(0xFF4CAF50),
            Color(0xFF009688),
            Color(0xFF00BCD4),
            Color(0xFF2196F3),
            Color(0xFF3F51B5),
            Color(0xFF9C27B0),
            Color(0xFF795548),
            Color(0xFF607D8B)
        )
    }

    RivoDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.settings_group_color),
        icon = Icons.Outlined.Colorize,
        confirmAction = RivoDialogAction(
            label = stringResource(R.string.action_done),
            onClick = {
                onColorSelected(currentColor)
                onDismissRequest()
            }
        ),
        dismissAction = RivoDialogAction(
            label = stringResource(R.string.action_cancel),
            onClick = onDismissRequest
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Swatch Preview & Hex
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = rivoPolygonShape(RivoMaterialShapes.Cookie9Sided),
                    color = currentColor,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 2.dp
                ) {}

                Text(
                    text = String.format("#%06X", 0xFFFFFF and currentColor.toArgb()),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Circular Color Picker Wheel
            RivoCircularColorWheel(
                hue = hue,
                saturation = saturation,
                value = value,
                onColorChange = { h, s ->
                    hue = h
                    saturation = s
                }
            )

            // Brightness / Value Slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = stringResource(R.string.content_desc_brightness),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )

                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 0.05f..1f,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${(value * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.End
                )
            }

            // Quick Color Presets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickColors.forEach { qc ->
                    val isSelected = (0xFFFFFF and qc.toArgb()) == (0xFFFFFF and currentColor.toArgb())
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape,
                        color = qc,
                        onClick = {
                            val hsv = FloatArray(3)
                            android.graphics.Color.colorToHSV(qc.toArgb(), hsv)
                            hue = hsv[0]
                            saturation = hsv[1]
                            value = hsv[2]
                        },
                        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else null
                    ) {}
                }
            }
        }
    }
}
