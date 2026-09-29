package com.grinch.rivo4.view.screen.settings
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
    var customPrimaryColor by remember(settingsState) {
        mutableIntStateOf(prefs.getInt("custom_primary_color", Color(0xFF6750A4).toArgb()))
    }
    var showCards by remember(settingsState) {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_CARDS, true))
    }
    var cardRoundness by remember(settingsState) {
        mutableIntStateOf(prefs.getInt(PreferenceManager.KEY_CARD_ROUNDNESS, 28).coerceAtLeast(5))
    }
    var uiBlurEnabled by remember(settingsState) {
        mutableStateOf(prefs.isUiBlurEnabled())
    }
    var dualSimButtons by remember(settingsState) {
        mutableStateOf(prefs.isDualSimDialpadButtonsEnabled())
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
                                                        contentDescription = "Custom color picker"
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
                                                            contentDescription = "Pick custom color",
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
                        }
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
                                        triggerThemeRestart()
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
                    }
                }

                // 3. Visual Effects & Blur
                item {
                    RivoExpressiveGroup(title = stringResource(R.string.settings_group_effects), icon = Icons.Outlined.BlurOn) {
                        item {
                            RivoSwitchListItem(
                                headline = stringResource(R.string.settings_ui_blur_title),
                                supporting = stringResource(R.string.settings_ui_blur_supporting),
                                leadingIcon = Icons.Outlined.BlurOn,
                                checked = uiBlurEnabled,
                                onCheckedChange = {
                                    uiBlurEnabled = it
                                    prefs.setUiBlurEnabled(it)
                                }
                            )
                        }
                        if (uiBlurEnabled) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.settings_ui_blur_active_hint),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Dialpad Interface
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
                        title = "More Display Settings",
                        icon = Icons.Outlined.DisplaySettings
                    ) {
                        item {
                            RivoListItem(
                                headline = "Navigation Bar",
                                supporting = "Floating bar style, blur effect, roundness & tab layout",
                                leadingIcon = Icons.Outlined.Dock,
                                onClick = { navigator.navigate(BottomNavScreenDestination) }
                            )
                        }
                        item {
                            RivoListItem(
                                headline = "Avatars & Contact Cards",
                                supporting = "11 avatar shapes, contact photos, initials & cards",
                                leadingIcon = Icons.Outlined.AccountCircle,
                                onClick = { navigator.navigate(AvatarSettingsScreenDestination) }
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
                    contentDescription = "Brightness",
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
