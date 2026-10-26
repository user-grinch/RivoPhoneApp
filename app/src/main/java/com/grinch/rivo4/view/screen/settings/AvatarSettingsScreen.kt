package com.grinch.rivo4.view.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.FormatColorReset
import androidx.compose.material.icons.outlined.Gradient
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.controller.util.SettingsHighlightManager
import com.grinch.rivo4.view.components.MenuTopAppBar
import com.grinch.rivo4.view.components.RivoAvatarShapeSelectorRow
import com.grinch.rivo4.view.components.RivoExpressiveGroup
import com.grinch.rivo4.view.components.RivoLeadingIconTile
import com.grinch.rivo4.view.components.RivoListItemDefaults
import com.grinch.rivo4.view.components.RivoPreviewTile
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.grinch.rivo4.view.components.gradientAvatarBrush
import com.grinch.rivo4.view.components.rivoAvatarColors
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun AvatarSettingsScreen(
    navigator: DestinationsNavigator
) {
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()

    var avatarShape by remember(settingsState) {
        mutableIntStateOf(prefs.getInt(PreferenceManager.KEY_AVATAR_SHAPE, 0))
    }
    var showPicture by remember(settingsState) {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_PICTURE, true))
    }
    var colorfulAvatars by remember(settingsState) {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_COLORFUL_AVATARS, true))
    }
    var gradientAvatars by remember(settingsState) {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_GRADIENT_AVATARS, false))
    }
    var hideAvatarWithBg by remember(settingsState) {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_HIDE_AVATAR_WITH_BACKGROUND, false))
    }

    val listState = rememberLazyListState()

    LaunchedEffect(SettingsHighlightManager.highlightEpoch) {
        if (SettingsHighlightManager.highlightEpoch == 0L) return@LaunchedEffect
        val target = SettingsHighlightManager.highlightedSettingTitle ?: return@LaunchedEffect
        val targetIndex = when {
            target.contains("Style", ignoreCase = true) || target.contains("Color", ignoreCase = true) -> 0
            target.contains("Shape", ignoreCase = true) -> 1
            else -> 2
        }
        delay(150L)
        listState.animateScrollToItem(targetIndex)
    }

    Scaffold(
        topBar = {
            MenuTopAppBar(
                text = stringResource(R.string.loc_avatars_contact_cards),
                navigator = navigator
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Avatar Style Selector Component (Colorful, Gradient, None)
            item {
                RivoExpressiveGroup(
                    title = stringResource(R.string.settings_avatar_style),
                    icon = Icons.Outlined.Palette
                ) {
                    item {
                        RivoAvatarStyleSelectorRow(
                            colorful = colorfulAvatars,
                            gradient = gradientAvatars,
                            onStyleChange = { newColorful, newGradient ->
                                colorfulAvatars = newColorful
                                gradientAvatars = newGradient
                                prefs.setBoolean(PreferenceManager.KEY_COLORFUL_AVATARS, newColorful)
                                prefs.setBoolean(PreferenceManager.KEY_GRADIENT_AVATARS, newGradient)
                            }
                        )
                    }
                }
            }

            // 3. Avatar Shape Selector Component
            item {
                RivoExpressiveGroup(
                    title = stringResource(R.string.settings_interface_avatar_shape),
                    icon = Icons.Outlined.Palette
                ) {
                    item {
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
                    }
                }
            }

            // 4. Other Display Options
            item {
                RivoExpressiveGroup(title = stringResource(R.string.settings_display_options), icon = Icons.Outlined.AccountCircle) {
                    item {
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
                    }
                    item {
                        RivoSwitchListItem(
                            headline = stringResource(R.string.settings_interface_hide_avatar_with_bg),
                            supporting = stringResource(R.string.settings_interface_hide_avatar_with_bg_supporting),
                            leadingIcon = Icons.Outlined.AccountCircle,
                            checked = hideAvatarWithBg,
                            onCheckedChange = {
                                hideAvatarWithBg = it
                                prefs.setBoolean(PreferenceManager.KEY_HIDE_AVATAR_WITH_BACKGROUND, it)
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun RivoAvatarStyleSelectorRow(
    colorful: Boolean,
    gradient: Boolean,
    onStyleChange: (colorful: Boolean, gradient: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentStyle = when {
        colorful && gradient -> 1 // Gradient
        colorful -> 0 // Colorful
        else -> 2 // None
    }
    val options = listOf(
        Pair(stringResource(R.string.avatar_style_colorful), 0),
        Pair(stringResource(R.string.avatar_style_gradient), 1),
        Pair(stringResource(R.string.avatar_style_none), 2)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = RivoListItemDefaults.VerticalPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RivoListItemDefaults.HorizontalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RivoLeadingIconTile(icon = Icons.Outlined.Palette)
            Spacer(modifier = Modifier.width(RivoListItemDefaults.Spacing))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_avatar_style),
                    style = RivoListItemDefaults.headlineStyle(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.settings_avatar_style_supporting),
                    style = RivoListItemDefaults.supportingStyle(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = RivoListItemDefaults.HorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(options.size) { index ->
                val (label, styleId) = options[index]
                val selected = styleId == currentStyle

                RivoPreviewTile(
                    label = label,
                    selected = selected,
                    onClick = {
                        when (styleId) {
                            0 -> onStyleChange(true, false)
                            1 -> onStyleChange(true, true)
                            2 -> onStyleChange(false, false)
                        }
                    },
                    width = 96.dp,
                    previewHeight = 72.dp
                ) {
                    when (styleId) {
                        1 -> { // Gradient
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        brush = gradientAvatarBrush("Rivo"),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "R",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        0 -> { // Colorful
                            val colors = rivoAvatarColors("Rivo", colorful = true)
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        color = colors.container,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "R",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.content
                                )
                            }
                        }
                        else -> { // None
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "R",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
