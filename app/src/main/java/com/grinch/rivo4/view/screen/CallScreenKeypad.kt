package com.grinch.rivo4.view.screen

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val RIVO_KEYPAD_ACCESSIBLE_TONE_MILLIS = 160L

private val RIVO_KEYPAD_ROWS: List<List<Char>> = listOf(
    listOf('1', '2', '3'),
    listOf('4', '5', '6'),
    listOf('7', '8', '9'),
    listOf('*', '0', '#')
)

@Composable
fun InCallKeypad(
    typedDigits: String,
    compact: Boolean,
    onDigitDown: (Char) -> Unit,
    onDigitUp: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val digitsDescription = if (typedDigits.isEmpty()) {
        stringResource(R.string.callkp_entered_empty)
    } else {
        stringResource(R.string.callkp_entered_desc, typedDigits)
    }
    val backspaceLabel = stringResource(R.string.content_desc_backspace)
    val actionSize = if (compact) 44.dp else 48.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (compact) 8.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(actionSize))

            Text(
                text = typedDigits,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .semantics {
                        contentDescription = digitsDescription
                        liveRegion = LiveRegionMode.Polite
                    },
                style = if (compact) {
                    MaterialTheme.typography.headlineSmallEmphasized
                } else {
                    MaterialTheme.typography.displaySmallEmphasized
                }.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                minLines = 1,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.StartEllipsis
            )

            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onBackspace()
                },
                enabled = typedDigits.isNotEmpty(),
                modifier = Modifier.size(actionSize),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = backspaceLabel,
                    modifier = Modifier.size(if (compact) 20.dp else 24.dp)
                )
            }
        }

        RIVO_KEYPAD_ROWS.forEach { keyRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                keyRow.forEach { key ->
                    InCallKeypadKey(
                        key = key,
                        compact = compact,
                        onDown = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onDigitDown(key)
                        },
                        onUp = onDigitUp,
                        onAccessibleTap = {
                            scope.launch {
                                onDigitDown(key)
                                delay(RIVO_KEYPAD_ACCESSIBLE_TONE_MILLIS)
                                onDigitUp()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InCallKeypadKey(
    key: Char,
    compact: Boolean,
    onDown: () -> Unit,
    onUp: () -> Unit,
    onAccessibleTap: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val keyLabel = stringResource(R.string.callkp_digit_desc, key.toString())

    Box(
        modifier = Modifier
            .size(if (compact) 52.dp else 72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .indication(interactionSource, ripple())
            .pointerInput(key) {
                detectTapGestures(
                    onPress = { position ->
                        val press = PressInteraction.Press(position)
                        interactionSource.emit(press)
                        onDown()
                        val released = tryAwaitRelease()
                        onUp()
                        interactionSource.emit(
                            if (released) {
                                PressInteraction.Release(press)
                            } else {
                                PressInteraction.Cancel(press)
                            }
                        )
                    }
                )
            }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = keyLabel
                onClick(label = null) {
                    onAccessibleTap()
                    true
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = key.toString(),
            modifier = Modifier.clearAndSetSemantics { },
            style = if (compact) {
                MaterialTheme.typography.titleLargeEmphasized
            } else {
                MaterialTheme.typography.headlineMediumEmphasized
            },
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
