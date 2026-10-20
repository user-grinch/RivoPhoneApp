package com.grinch.rivo4.view.screen

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grinch.rivo4.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val RIVO_KEYPAD_ACCESSIBLE_TONE_MILLIS = 160L

data class InCallKeyDef(
    val digit: Char,
    val letters: String
)

val RIVO_KEYPAD_DEF: List<List<InCallKeyDef>> = listOf(
    listOf(InCallKeyDef('1', ""), InCallKeyDef('2', "A B C"), InCallKeyDef('3', "D E F")),
    listOf(InCallKeyDef('4', "G H I"), InCallKeyDef('5', "J K L"), InCallKeyDef('6', "M N O")),
    listOf(InCallKeyDef('7', "P Q R S"), InCallKeyDef('8', "T U V"), InCallKeyDef('9', "W X Y Z")),
    listOf(InCallKeyDef('*', ""), InCallKeyDef('0', "+"), InCallKeyDef('#', ""))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InCallKeypadBottomSheet(
    typedDigits: String,
    onDigitDown: (Char) -> Unit,
    onDigitUp: () -> Unit,
    onBackspace: () -> Unit,
    onClearAll: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onEndCall: (() -> Unit)? = null
) {
    val view = LocalView.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        },
        modifier = Modifier.imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 28.dp, top = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Centered Typed Digits Display (no collapse button, no empty placeholder) + Backspace
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (typedDigits.isNotEmpty()) {
                    Text(
                        text = typedDigits,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 30.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum",
                            letterSpacing = 2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        minLines = 1,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.StartEllipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp)
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(44.dp)
                            .clip(CircleShape)
                            .pointerInput(typedDigits) {
                                detectTapGestures(
                                    onTap = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        onBackspace()
                                    },
                                    onLongPress = {
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                        if (onClearAll != null) {
                                            onClearAll()
                                        } else {
                                            for (i in typedDigits.indices) onBackspace()
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = stringResource(R.string.content_desc_backspace),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Dialpad Key Grid (Google Dialer style)
            RIVO_KEYPAD_DEF.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { keyDef ->
                        InCallKeypadSheetKey(
                            keyDef = keyDef,
                            onDown = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onDigitDown(keyDef.digit)
                            },
                            onUp = onDigitUp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InCallKeypadSheetKey(
    keyDef: InCallKeyDef,
    onDown: () -> Unit,
    onUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDark = isSystemInDarkTheme()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(
            stiffness = Spring.StiffnessHigh,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "KeyScale"
    )

    val containerColor by animateColorAsState(
        targetValue = when {
            isPressed -> MaterialTheme.colorScheme.primaryContainer
            isDark -> MaterialTheme.colorScheme.surfaceContainerHigh
            else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f)
        },
        animationSpec = tween(durationMillis = 80),
        label = "KeyBg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isPressed) MaterialTheme.colorScheme.onPrimaryContainer
                      else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 80),
        label = "KeyFg"
    )

    val letterColor by animateColorAsState(
        targetValue = if (isPressed) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 80),
        label = "KeyLetterFg"
    )

    // Morph shape: MD3 squircle container like Google Phone (corner radius ~34dp idle, ~16dp pressed)
    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) 16.dp else 34.dp,
        animationSpec = spring(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "KeyCorner"
    )

    Surface(
        modifier = modifier
            .height(68.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(keyDef.digit) {
                detectTapGestures(
                    onPress = { position ->
                        val press = PressInteraction.Press(position)
                        interactionSource.emit(press)
                        onDown()
                        val released = tryAwaitRelease()
                        onUp()
                        interactionSource.emit(
                            if (released) PressInteraction.Release(press)
                            else PressInteraction.Cancel(press)
                        )
                    }
                )
            }
            .semantics {
                contentDescription = when (keyDef.digit) {
                    '*' -> "Star"
                    '#' -> "Pound"
                    else -> "${keyDef.digit} ${keyDef.letters}".trim()
                }
            },
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (keyDef.digit) {
                '*' -> {
                    Text(
                        text = "∗",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 34.sp
                        ),
                        color = contentColor,
                        textAlign = TextAlign.Center
                    )
                }
                '#' -> {
                    Text(
                        text = "#",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 24.sp
                        ),
                        color = contentColor,
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    Text(
                        text = keyDef.digit.toString(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 28.sp
                        ),
                        color = contentColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    if (keyDef.letters.isNotEmpty()) {
                        Text(
                            text = keyDef.letters,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = if (keyDef.digit == '0') 14.sp else 11.sp,
                                fontWeight = if (keyDef.digit == '0') FontWeight.Bold else FontWeight.SemiBold,
                                letterSpacing = if (keyDef.digit == '0') 0.sp else 1.2.sp,
                                lineHeight = 14.sp
                            ),
                            color = letterColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false
                        )
                    } else {
                        // Spacer matching letter text height to keep baseline identical across 1, 2, 3
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun InCallKeypad(
    typedDigits: String,
    compact: Boolean,
    onDigitDown: (Char) -> Unit,
    onDigitUp: () -> Unit,
    onBackspace: () -> Unit,
    onClearAll: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (compact) 8.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
    ) {
        // Landscape Header: Back button on left, Centered typed digits (no empty placeholder text), Backspace on right
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 42.dp else 48.dp)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(if (compact) 38.dp else 44.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, radius = 20.dp),
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                onBack()
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(if (compact) 22.dp else 24.dp)
                    )
                }
            }

            if (typedDigits.isNotEmpty()) {
                Text(
                    text = typedDigits,
                    style = if (compact) {
                        MaterialTheme.typography.titleLarge.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum",
                            letterSpacing = 1.5.sp
                        )
                    } else {
                        MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum",
                            letterSpacing = 2.sp
                        )
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    minLines = 1,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.StartEllipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(if (compact) 38.dp else 44.dp)
                        .clip(CircleShape)
                        .pointerInput(typedDigits) {
                            detectTapGestures(
                                onTap = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    onBackspace()
                                },
                                onLongPress = {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    if (onClearAll != null) {
                                        onClearAll()
                                    } else {
                                        for (i in typedDigits.indices) onBackspace()
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = stringResource(R.string.content_desc_backspace),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(if (compact) 20.dp else 24.dp)
                    )
                }
            }
        }

        // Landscape Dialpad Grid matching M3 Expressive style
        RIVO_KEYPAD_DEF.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { keyDef ->
                    InCallLandscapeKey(
                        keyDef = keyDef,
                        compact = compact,
                        onDown = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onDigitDown(keyDef.digit)
                        },
                        onUp = onDigitUp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun InCallLandscapeKey(
    keyDef: InCallKeyDef,
    compact: Boolean,
    onDown: () -> Unit,
    onUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDark = isSystemInDarkTheme()

    // Larger key heights in landscape for better ergonomics
    val keyHeight = if (compact) 54.dp else 62.dp

    // Morph shape: MD3 squircle container like Google Phone (corner radius ~stadium pill idle, ~14dp pressed)
    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) 14.dp else (keyHeight / 2),
        animationSpec = spring(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "LandscapeKeyCorner"
    )
    val keyShape = RoundedCornerShape(cornerRadius)

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(
            stiffness = Spring.StiffnessHigh,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "LandscapeKeyScale"
    )

    val containerColor by animateColorAsState(
        targetValue = when {
            isPressed -> MaterialTheme.colorScheme.primaryContainer
            isDark -> MaterialTheme.colorScheme.surfaceContainerHigh
            else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f)
        },
        animationSpec = tween(durationMillis = 80),
        label = "LandscapeKeyBg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isPressed) MaterialTheme.colorScheme.onPrimaryContainer
                      else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 80),
        label = "LandscapeKeyFg"
    )

    val letterColor by animateColorAsState(
        targetValue = if (isPressed) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 80),
        label = "LandscapeKeyLetterFg"
    )

    Surface(
        modifier = modifier
            .height(keyHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(keyDef.digit) {
                detectTapGestures(
                    onPress = { position ->
                        val press = PressInteraction.Press(position)
                        interactionSource.emit(press)
                        onDown()
                        val released = tryAwaitRelease()
                        onUp()
                        interactionSource.emit(
                            if (released) PressInteraction.Release(press)
                            else PressInteraction.Cancel(press)
                        )
                    }
                )
            }
            .semantics {
                contentDescription = when (keyDef.digit) {
                    '*' -> "Star"
                    '#' -> "Pound"
                    else -> "${keyDef.digit} ${keyDef.letters}".trim()
                }
            },
        shape = keyShape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (keyDef.digit) {
                '*' -> {
                    Text(
                        text = "∗",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = if (compact) 28.sp else 34.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = if (compact) 28.sp else 34.sp
                        ),
                        color = contentColor,
                        textAlign = TextAlign.Center
                    )
                }
                '#' -> {
                    Text(
                        text = "#",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = if (compact) 20.sp else 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = if (compact) 20.sp else 24.sp
                        ),
                        color = contentColor,
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    Text(
                        text = keyDef.digit.toString(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = if (compact) 24.sp else 28.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = if (compact) 24.sp else 28.sp
                        ),
                        color = contentColor,
                        textAlign = TextAlign.Center
                    )
                    if (keyDef.letters.isNotEmpty()) {
                        Text(
                            text = keyDef.letters,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = if (keyDef.digit == '0') (if (compact) 12.sp else 14.sp) else (if (compact) 10.sp else 11.5.sp),
                                fontWeight = if (keyDef.digit == '0') FontWeight.Bold else FontWeight.SemiBold,
                                letterSpacing = if (keyDef.digit == '0') 0.sp else 1.sp,
                                lineHeight = if (compact) 11.sp else 13.sp
                            ),
                            color = letterColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false
                        )
                    } else {
                        Spacer(modifier = Modifier.height(if (compact) 11.dp else 13.dp))
                    }
                }
            }
        }
    }
}
