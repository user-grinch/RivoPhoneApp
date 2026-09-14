package com.grinch.rivo4.view.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.window.PopupProperties
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import org.koin.compose.koinInject
import com.grinch.rivo4.view.theme.LocalCardRoundness
import com.grinch.rivo4.view.theme.RivoMaterialShapes
import com.grinch.rivo4.view.theme.RivoMorphShape
import com.grinch.rivo4.view.theme.RivoMotion
import com.grinch.rivo4.view.theme.RivoShapeDefaults
import com.grinch.rivo4.view.theme.rememberRivoMorph
import com.grinch.rivo4.view.theme.rememberRivoMorphShape
import com.grinch.rivo4.view.theme.rivoCornerDp

val LocalRivoDialogDismiss = compositionLocalOf<((() -> Unit) -> Unit)?> { null }

@Immutable
data class RivoDialogAction(
    val label: String,
    val onClick: () -> Unit,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
    val dismissOnClick: Boolean = true
)

private val DialogMaxWidth = 460.dp
private val DialogActionHeight = 48.dp
private val DialogHeaderTileSize = 52.dp
private val DialogHeaderIconSize = 26.dp
private val SelectionTileSize = 42.dp
private val SelectionIconSize = 22.dp
private val SelectionPreviewSize = 52.dp
private const val ScrimAlpha = 0.38f
private const val DialogEnterScale = 0.72f
private const val DialogExitScale = 0.80f
private const val DialogExitDurationMs = 180

@Composable
fun ApplyDialogBlurBehind() {
    val prefs = koinInject<PreferenceManager>()
    val isBlurEnabled = prefs.isUiBlurEnabled()
    val view = LocalView.current

    DisposableEffect(isBlurEnabled, view) {
        val window = (view.parent as? DialogWindowProvider)?.window
            ?: (view.context as? Activity)?.window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && window != null) {
            if (isBlurEnabled) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                val lp = window.attributes
                lp.blurBehindRadius = 80
                window.attributes = lp
                window.setDimAmount(0.25f)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                val lp = window.attributes
                lp.blurBehindRadius = 0
                window.attributes = lp
                window.setDimAmount(0f)
            }
        }
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            }
        }
    }
}

@Composable
fun RivoDialog(
    onDismissRequest: () -> Unit,
    title: String? = null,
    icon: ImageVector? = null,
    confirmButton: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    confirmAction: RivoDialogAction? = null,
    dismissAction: RivoDialogAction? = null,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    showCloseButton: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val prefs = koinInject<PreferenceManager>()
    val isBlurEnabled = prefs.isUiBlurEnabled()

    var isVisible by remember { mutableStateOf(false) }
    var isDismissing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val dismissWithAnimation: (() -> Unit) -> Unit = { action ->
        if (!isDismissing) {
            isDismissing = true
            isVisible = false
            coroutineScope.launch {
                delay(DialogExitDurationMs.toLong())
                action()
            }
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else if (isDismissing) DialogExitScale else DialogEnterScale,
        animationSpec = if (isVisible) {
            spring(
                dampingRatio = 0.68f,
                stiffness = Spring.StiffnessMediumLow
            )
        } else {
            tween(
                durationMillis = DialogExitDurationMs,
                easing = FastOutLinearInEasing
            )
        },
        label = "RivoDialogScale"
    )
    val fade by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = if (isVisible) {
            tween(durationMillis = 200, easing = LinearOutSlowInEasing)
        } else {
            tween(durationMillis = 150, easing = LinearEasing)
        },
        label = "RivoDialogFade"
    )

    Dialog(
        onDismissRequest = {
            dismissWithAnimation { onDismissRequest() }
        },
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        CompositionLocalProvider(LocalRivoDialogDismiss provides dismissWithAnimation) {
            ApplyDialogBlurBehind()

            val scrimColor = MaterialTheme.colorScheme.scrim
            val scrimInteraction = remember { MutableInteractionSource() }
            val dismissLabel = stringResource(R.string.action_close)
            val currentScrimAlpha = if (isBlurEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.22f else ScrimAlpha

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind { drawRect(color = scrimColor, alpha = currentScrimAlpha * fade) }
                    .then(
                        if (dismissOnClickOutside) {
                            Modifier.clickable(
                                interactionSource = scrimInteraction,
                                indication = null,
                                onClickLabel = dismissLabel,
                                onClick = { dismissWithAnimation { onDismissRequest() } }
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                val destructive = confirmAction?.destructive == true
                val headerContainer = if (destructive) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
                val headerContent = if (destructive) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }

                val roundness = LocalCardRoundness.current
                val dialogCornerDp = rivoCornerDp(RivoShapeDefaults.BaseExtraLarge, roundness)

                Surface(
                    modifier = modifier
                        .fillMaxWidth()
                        .widthIn(max = DialogMaxWidth)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            alpha = fade
                        }
                        .pointerInput(Unit) { detectTapGestures { } }
                        .animateContentSize(animationSpec = RivoMotion.spatialDefault<IntSize>())
                        .semantics { if (title != null) paneTitle = title },
                    shape = RoundedCornerShape(dialogCornerDp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Top-Right Window Close Button
                        if (showCloseButton) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.75f),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 14.dp, end = 14.dp)
                                    .size(34.dp),
                                onClick = { dismissWithAnimation { onDismissRequest() } }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.action_close),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (icon != null || title != null || supportingText != null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 22.dp, bottom = 6.dp, start = 24.dp, end = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (icon != null) {
                                        val headerMorph = rememberRivoMorphShape(RivoMaterialShapes.Cookie12Sided, RivoMaterialShapes.Circle) { scale }
                                        Surface(
                                            modifier = Modifier.size(DialogHeaderTileSize),
                                            shape = headerMorph,
                                            color = headerContainer,
                                            contentColor = headerContent,
                                            shadowElevation = 1.dp
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(DialogHeaderIconSize)
                                                )
                                            }
                                        }
                                    }

                                    if (title != null) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleLargeEmphasized,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    if (supportingText != null) {
                                        Text(
                                            text = supportingText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 22.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                content = content
                            )

                            // Actions / Buttons: Cancel button is removed in favor of the top-right window close button
                            val cancelStrings = setOf("cancel", "close", "dismiss")
                            val isDismissPureCancel = dismissAction != null && dismissAction.label.lowercase().trim() in cancelStrings

                            if (confirmAction != null || (dismissAction != null && !isDismissPureCancel)) {
                                val hasConfirm = confirmAction != null
                                val hasDismiss = dismissAction != null && !isDismissPureCancel

                                if (hasConfirm && hasDismiss) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 22.dp, end = 22.dp, top = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RivoDialogActionButton(
                                            action = dismissAction!!,
                                            prominent = false,
                                            onTrigger = dismissWithAnimation,
                                            modifier = Modifier.weight(1f)
                                        )
                                        RivoDialogActionButton(
                                            action = confirmAction!!,
                                            prominent = true,
                                            onTrigger = dismissWithAnimation,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                } else if (hasConfirm) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 22.dp, end = 22.dp, top = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        RivoDialogActionButton(
                                            action = confirmAction!!,
                                            prominent = true,
                                            onTrigger = dismissWithAnimation,
                                            modifier = Modifier.widthIn(min = 160.dp, max = 240.dp)
                                        )
                                    }
                                } else if (hasDismiss) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 22.dp, end = 22.dp, top = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        RivoDialogActionButton(
                                            action = dismissAction!!,
                                            prominent = false,
                                            onTrigger = dismissWithAnimation,
                                            modifier = Modifier.widthIn(min = 160.dp, max = 240.dp)
                                        )
                                    }
                                }
                            } else if (confirmButton != null || dismissButton != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 22.dp, end = 22.dp, top = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (dismissButton != null) {
                                        dismissButton()
                                    }
                                    if (confirmButton != null) {
                                        confirmButton()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RivoDialogActionButton(
    action: RivoDialogAction,
    prominent: Boolean,
    modifier: Modifier = Modifier,
    onTrigger: ((() -> Unit) -> Unit)? = null
) {
    val roundness = LocalCardRoundness.current
    val buttonCornerDp = rivoCornerDp(RivoShapeDefaults.BaseLarge, roundness).coerceAtMost(20.dp)
    val buttonShape = RoundedCornerShape(buttonCornerDp)

    val handleClick = {
        if (action.dismissOnClick && onTrigger != null) {
            onTrigger { action.onClick() }
        } else {
            action.onClick()
        }
    }

    if (prominent) {
        Button(
            onClick = handleClick,
            shape = buttonShape,
            modifier = modifier.height(DialogActionHeight),
            enabled = action.enabled,
            colors = if (action.destructive) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            } else {
                ButtonDefaults.buttonColors()
            },
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp)
        ) {
            Text(
                text = action.label,
                style = MaterialTheme.typography.labelLargeEmphasized,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    } else {
        FilledTonalButton(
            onClick = handleClick,
            shape = buttonShape,
            modifier = modifier.height(DialogActionHeight),
            enabled = action.enabled,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp)
        ) {
            Text(
                text = action.label,
                style = MaterialTheme.typography.labelLargeEmphasized,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RivoConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    message: String,
    confirmLabel: String = stringResource(R.string.action_confirm),
    dismissLabel: String? = null,
    icon: ImageVector? = null,
    isDestructive: Boolean = false
) {
    RivoDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        icon = icon,
        confirmAction = RivoDialogAction(
            label = confirmLabel,
            onClick = onConfirm,
            destructive = isDestructive
        )
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun <T> RivoSelectionDialog(
    onDismissRequest: () -> Unit,
    title: String,
    items: List<T>,
    itemLabel: (T) -> String,
    onItemSelected: (T) -> Unit,
    itemSupporting: ((T) -> String)? = null,
    icon: ImageVector? = null,
    itemIcon: ((T) -> ImageVector)? = null,
    itemPreview: (@Composable (T) -> Unit)? = null,
    isSelected: (T) -> Boolean = { false },
    dismissLabel: String? = null
) {
    RivoDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        icon = icon
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                RivoSelectionRow(
                    label = itemLabel(item),
                    onClick = {
                        onItemSelected(item)
                        onDismissRequest()
                    },
                    supporting = itemSupporting?.invoke(item)?.takeIf { it.isNotBlank() },
                    icon = itemIcon?.invoke(item),
                    preview = itemPreview?.let { p -> { p(item) } },
                    selected = isSelected(item),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .widthIn(max = 380.dp)
                )
            }
        }
    }
}

@Composable
fun RivoSelectionRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
    preview: (@Composable () -> Unit)? = null,
    selected: Boolean = false
) {
    val roundness = LocalCardRoundness.current
    val animatedDismiss = LocalRivoDialogDismiss.current

    val selection by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = RivoMotion.shapeMorph(),
        label = "RivoSelectionRowMorph"
    )
    val corner by animateDpAsState(
        targetValue = rivoCornerDp(
            if (selected) RivoShapeDefaults.BaseExtraLarge else RivoShapeDefaults.BaseLarge,
            roundness
        ),
        animationSpec = RivoMotion.shapeMorph(),
        label = "RivoSelectionRowCorner"
    )
    val container by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = RivoMotion.colorChange(),
        label = "RivoSelectionRowContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = RivoMotion.colorChange(),
        label = "RivoSelectionRowContent"
    )
    val supportingColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = RivoMotion.colorChange(),
        label = "RivoSelectionRowSupporting"
    )
    val tileContainer by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        animationSpec = RivoMotion.colorChange(),
        label = "RivoSelectionRowTile"
    )
    val tileContent by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
        animationSpec = RivoMotion.colorChange(),
        label = "RivoSelectionRowTileContent"
    )

    val tileMorph = rememberRivoMorph(RivoMaterialShapes.Circle, RivoMaterialShapes.Cookie12Sided)
    val tileShape: Shape = RivoMorphShape(tileMorph) { selection }

    Surface(
        modifier = modifier
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = {
                    if (animatedDismiss != null) {
                        animatedDismiss { onClick() }
                    } else {
                        onClick()
                    }
                }
            ),
        shape = RoundedCornerShape(corner),
        color = container,
        contentColor = content
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (preview != null) {
                Surface(
                    modifier = Modifier.size(SelectionPreviewSize),
                    shape = RoundedCornerShape(rivoCornerDp(RivoShapeDefaults.BaseLarge, roundness)),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = if (selected) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        null
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        preview()
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
            } else if (icon != null) {
                Surface(
                    modifier = Modifier.size(SelectionTileSize),
                    shape = tileShape,
                    color = tileContainer,
                    contentColor = tileContent
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(SelectionIconSize)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = if (selected) {
                        MaterialTheme.typography.titleMediumEmphasized
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                    color = content
                )
                if (supporting != null) {
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.bodySmall,
                        color = supportingColor
                    )
                }
            }

            if (selection > 0f) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            scaleX = selection
                            scaleY = selection
                            alpha = selection
                        }
                )
            }
        }
    }
}

@Composable
fun RivoBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector? = null,
    supportingText: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(),
    confirmAction: RivoDialogAction? = null,
    dismissAction: RivoDialogAction? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val roundness = LocalCardRoundness.current
    val topCorner = rivoCornerDp(RivoShapeDefaults.BaseExtraLarge, roundness)
    val destructive = confirmAction?.destructive == true

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = topCorner, topEnd = topCorner),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        ApplyDialogBlurBehind()

        if (icon != null || title != null || supportingText != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    val sheetHeaderMorph = rememberRivoMorphShape(RivoMaterialShapes.Cookie12Sided, RivoMaterialShapes.Circle) { 0.5f }
                    Surface(
                        modifier = Modifier.size(DialogHeaderTileSize),
                        shape = sheetHeaderMorph,
                        color = if (destructive) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        contentColor = if (destructive) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        shadowElevation = 1.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(DialogHeaderIconSize)
                            )
                        }
                    }
                }
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )

        if (confirmAction != null || dismissAction != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dismissAction != null) {
                    RivoDialogActionButton(
                        action = dismissAction,
                        prominent = false,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (confirmAction != null) {
                    RivoDialogActionButton(
                        action = confirmAction,
                        prominent = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun RivoDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit
) {
    val roundness = LocalCardRoundness.current
    val cornerDp = rivoCornerDp(RivoShapeDefaults.BaseExtraLarge, roundness)

    MaterialTheme(
        shapes = MaterialTheme.shapes.copy(
            extraSmall = RoundedCornerShape(cornerDp)
        )
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            offset = offset,
            scrollState = rememberScrollState(),
            properties = properties,
            shape = RoundedCornerShape(cornerDp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            content = content
        )
    }
}

@Composable
fun RivoDropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    destructive: Boolean = false
) {
    val contentColor = if (destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        DropdownMenuItem(
            text = text,
            onClick = onClick,
            modifier = modifier,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            enabled = enabled,
            colors = MenuDefaults.itemColors(
                textColor = contentColor,
                leadingIconColor = contentColor,
                trailingIconColor = contentColor
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}
