package com.grinch.rivo4.view.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.view.components.MenuTopAppBar
import com.grinch.rivo4.view.components.RivoExpressiveCard
import com.grinch.rivo4.view.components.RivoExpressiveGroup
import com.grinch.rivo4.view.components.RivoListItem
import com.grinch.rivo4.view.components.RivoOptionRow
import com.grinch.rivo4.view.components.RivoSwitchListItem
import com.grinch.rivo4.view.components.RivoVisualOptionSelectorRow
import com.grinch.rivo4.view.theme.rivoFontFamilyFromId
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun TypographyScreen(
    navigator: DestinationsNavigator
) {
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()

    var useSystemFont by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_USE_SYSTEM_FONT, true))
    }
    var fontFamily by remember {
        mutableStateOf(
            prefs.getString(PreferenceManager.KEY_FONT_FAMILY, PreferenceManager.FONT_FAMILY_SANS_SERIF)
                ?: PreferenceManager.FONT_FAMILY_SANS_SERIF
        )
    }
    var fontSize by remember {
        mutableIntStateOf(prefs.getInt(PreferenceManager.KEY_FONT_SIZE, PreferenceManager.FONT_SIZE_MEDIUM))
    }
    var showFontDialog by remember { mutableStateOf(false) }

    val fontLabels = listOf(
        stringResource(R.string.font_family_sans_serif),
        stringResource(R.string.font_family_serif),
        stringResource(R.string.font_family_monospace),
        stringResource(R.string.font_family_cursive),
        stringResource(R.string.font_family_casual),
        stringResource(R.string.font_family_condensed),
        stringResource(R.string.font_family_light),
        stringResource(R.string.font_family_medium),
        stringResource(R.string.font_family_black)
    )

    val selectedFontIndex =
        PreferenceManager.ALL_FONT_FAMILIES.indexOf(fontFamily).coerceAtLeast(0)
    val currentFontLabel = fontLabels.getOrElse(selectedFontIndex) { fontLabels[0] }

    val previewFamily = if (useSystemFont) {
        FontFamily.Default
    } else {
        rivoFontFamilyFromId(fontFamily)
    }
    val previewScale = when (fontSize) {
        PreferenceManager.FONT_SIZE_SMALL -> 0.9f
        PreferenceManager.FONT_SIZE_LARGE -> 1.12f
        else -> 1f
    }

    Scaffold(
        topBar = {
            MenuTopAppBar(
                text = stringResource(R.string.typography_title),
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
                RivoExpressiveCard(
                    title = stringResource(R.string.typography_title),
                    icon = Icons.Outlined.TextFields
                ) {
                    Text(
                        text = stringResource(R.string.typography_preview_text),
                        fontFamily = previewFamily,
                        fontSize = (MaterialTheme.typography.titleLarge.fontSize.value * previewScale).sp,
                        lineHeight = (MaterialTheme.typography.titleLarge.lineHeight.value * previewScale).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 20.dp)
                    )
                }
            }

            item {
                RivoExpressiveGroup(
                    title = stringResource(R.string.typography_title),
                    icon = Icons.Outlined.FormatSize
                ) {
                    item {
                        RivoSwitchListItem(
                            headline = stringResource(R.string.pref_system_font_toggle),
                            supporting = stringResource(R.string.pref_system_font_toggle_description),
                            leadingIcon = Icons.Outlined.TextFields,
                            checked = useSystemFont,
                            onCheckedChange = { enabled ->
                                useSystemFont = enabled
                                prefs.setBoolean(PreferenceManager.KEY_USE_SYSTEM_FONT, enabled)
                            }
                        )
                    }
                    if (!useSystemFont) {
                        item {
                            RivoListItem(
                                headline = stringResource(R.string.font_family_label),
                                supporting = currentFontLabel,
                                leadingIcon = Icons.Outlined.TextFields,
                                trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                onClick = { showFontDialog = true }
                            )
                        }
                        item {
                            RivoVisualOptionSelectorRow(
                                headline = stringResource(R.string.font_family_label),
                                supporting = stringResource(R.string.font_family_label_supporting),
                                leadingIcon = Icons.Outlined.TextFields,
                                options = fontLabels.mapIndexed { index, label -> label to index },
                                selectedValue = selectedFontIndex,
                                onValueChange = { index ->
                                    val id = PreferenceManager.ALL_FONT_FAMILIES[index]
                                    fontFamily = id
                                    prefs.setString(PreferenceManager.KEY_FONT_FAMILY, id)
                                },
                                tileWidth = 84.dp,
                                tileHeight = 68.dp
                            ) { index, selected ->
                                Text(
                                    text = "Aa",
                                    fontFamily = rivoFontFamilyFromId(PreferenceManager.ALL_FONT_FAMILIES[index]),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                    item {
                        RivoOptionRow(
                            headline = stringResource(R.string.font_size_label),
                            supporting = stringResource(R.string.font_size_label_supporting),
                            leadingIcon = Icons.Outlined.FormatSize,
                            options = listOf(
                                stringResource(R.string.font_size_small) to PreferenceManager.FONT_SIZE_SMALL,
                                stringResource(R.string.font_size_medium) to PreferenceManager.FONT_SIZE_MEDIUM,
                                stringResource(R.string.font_size_large) to PreferenceManager.FONT_SIZE_LARGE
                            ),
                            selectedValue = fontSize,
                            onValueChange = { value ->
                                fontSize = value
                                prefs.setInt(PreferenceManager.KEY_FONT_SIZE, value)
                            }
                        )
                    }
                }
            }
        }

        if (showFontDialog) {
            AlertDialog(
                onDismissRequest = { showFontDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.font_picker_dialog_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(PreferenceManager.ALL_FONT_FAMILIES.size) { index ->
                            val id = PreferenceManager.ALL_FONT_FAMILIES[index]
                            val label = fontLabels[index]
                            val isSelected = (id == fontFamily)
                            val family = rivoFontFamilyFromId(id)

                            Surface(
                                onClick = {
                                    fontFamily = id
                                    prefs.setString(PreferenceManager.KEY_FONT_FAMILY, id)
                                    showFontDialog = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            fontFamily = id
                                            prefs.setString(PreferenceManager.KEY_FONT_FAMILY, id)
                                            showFontDialog = false
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = family,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.typography_preview_text),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = family,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showFontDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }
}
