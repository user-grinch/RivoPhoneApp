package com.grinch.rivo4.view.screen.settings
import com.grinch.rivo4.view.components.MenuTopAppBar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import com.grinch.rivo4.controller.util.SettingsHighlightManager
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Gradient
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.view.components.RivoAvatar
import com.grinch.rivo4.view.components.RivoAvatarShapeSelectorRow
import com.grinch.rivo4.view.components.RivoDivider
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoExpressiveGroup
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
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
            target.contains("Shape", ignoreCase = true) -> 0
            else -> 1
        }
        delay(150L)
        listState.animateScrollToItem(targetIndex)
    }

    Scaffold(
        topBar = {
            MenuTopAppBar(
                text = stringResource(R.string.settings_avatars_title),
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
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
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
                            headline = stringResource(R.string.settings_interface_colorful_avatars),
                            supporting = stringResource(R.string.settings_interface_colorful_avatars_supporting),
                            leadingIcon = Icons.Outlined.Palette,
                            checked = colorfulAvatars,
                            onCheckedChange = {
                                colorfulAvatars = it
                                prefs.setBoolean(PreferenceManager.KEY_COLORFUL_AVATARS, it)
                            }
                        )
                    }
                    item {
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
                RivoExpressiveCard(
                    title = stringResource(R.string.avatar_preview_title),
                    icon = Icons.Outlined.AccountCircle
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("Dana Blue", "Milo Yellow", "Ava Green", "Eli Red").forEach { name ->
                                RivoAvatar(
                                    name = name,
                                    modifier = Modifier.weight(1f).aspectRatio(1f)
                                )
                            }
                        }
                        Text(
                            text = stringResource(
                                when {
                                    colorfulAvatars && gradientAvatars -> R.string.avatar_preview_colorful_gradient
                                    colorfulAvatars -> R.string.avatar_preview_colorful
                                    gradientAvatars -> R.string.avatar_preview_gradient
                                    else -> R.string.avatar_preview_plain
                                }
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
