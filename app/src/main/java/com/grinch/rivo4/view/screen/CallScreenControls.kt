package com.grinch.rivo4.view.screen

import androidx.compose.material.icons.automirrored.outlined.CallMerge

import android.telecom.Call
import android.telecom.CallAudioState
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CallMerge
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager
import com.grinch.rivo4.view.theme.RivoMaterialShapes
import com.grinch.rivo4.view.theme.callColors
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject

/**
 * Ordered slots of the in-call action grid: the first three form the top row,
 * the last three the bottom row.
 */
object RivoCallButtonOrder {
    const val MUTE = "mute"
    const val KEYPAD = "keypad"
    const val AUDIO = "audio"
    const val RECORD = "record"
    const val HOLD = "hold"
    const val ADD = "add"

    val All: List<String> = listOf(MUTE, KEYPAD, AUDIO, RECORD, HOLD, ADD)

    /** Parses the comma-separated hidden-button pref into a sanitized set of known ids. */
    fun hiddenSet(raw: String?): Set<String> =
        raw?.split(',')?.map { it.trim() }?.filter { it in All }?.toSet().orEmpty()

    fun parse(raw: String?, hidden: Set<String> = emptySet()): List<String> {
        val saved = raw
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it in All }
            ?.distinct()
            .orEmpty()
        if (raw == PreferenceManager.DEFAULT_CALL_BUTTON_ORDER) {
            return saved.filterNot { it in hidden }.ifEmpty { saved }
        }
        val available = All
        val full = saved + available.filterNot { it in saved }
        val visible = full.filterNot { it in hidden }
        // If every button was hidden (stale pref from another device), fall back to the full grid
        return visible.ifEmpty { full }
    }

    fun format(order: List<String>): String =
        order.filter { it in All }.distinct().joinToString(",")
}

@Composable
fun CallActionButton(
    icon: ImageVector,
    isActive: Boolean,
    label: String,
    compact: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val buttonHeight = if (compact) 68.dp else 80.dp
    val iconSize = if (compact) 24.dp else 28.dp

    // Morph shape: MD3 squircle container like Google Phone (corner radius ~28dp idle, ~20dp pressed/active)
    val cornerRadius by animateDpAsState(
        targetValue = when {
            isPressed && enabled -> if (compact) 16.dp else 20.dp
            isActive && enabled -> if (compact) 20.dp else 24.dp
            else -> if (compact) 24.dp else 28.dp
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "CallActionCornerRadius"
    )

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val containerColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            isDanger -> MaterialTheme.callColors.decline
            isActive -> MaterialTheme.colorScheme.primary
            isPressed -> MaterialTheme.colorScheme.primaryContainer
            else -> if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(durationMillis = 150, easing = LinearOutSlowInEasing),
        label = "btnBg"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            isDanger -> MaterialTheme.callColors.onDecline
            isActive -> MaterialTheme.colorScheme.onPrimary
            isPressed -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(durationMillis = 150, easing = LinearOutSlowInEasing),
        label = "btnFg"
    )

    Surface(
        onClick = {
            if (enabled) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
        },
        enabled = enabled,
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        tonalElevation = if (isActive || isDanger) 6.dp else 2.dp,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(buttonHeight)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(iconSize)
            )
            Spacer(modifier = Modifier.height(if (compact) 4.dp else 6.dp))
            Text(
                text = label,
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AuxiliaryPillButton(
    icon: ImageVector,
    label: String,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            onClick()
        },
        shape = CircleShape,
        color = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerHighest,
        interactionSource = interactionSource,
        modifier = modifier.height(if (compact) 32.dp else 36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 14.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(if (compact) 16.dp else 18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun callAudioRouteIcon(route: Int): ImageVector = when (route) {
    CallAudioState.ROUTE_BLUETOOTH -> Icons.Default.Bluetooth
    CallAudioState.ROUTE_WIRED_HEADSET -> Icons.Default.Headset
    else -> Icons.AutoMirrored.Filled.VolumeUp
}

@Composable
private fun callAudioRouteLabel(route: Int, audioState: CallAudioState? = null): String {
    val bluetoothFallback = stringResource(R.string.audio_route_bluetooth)
    val speakerLabel = stringResource(R.string.audio_route_speaker)
    val headsetLabel = stringResource(R.string.audio_route_headset)
    return when (route) {
        CallAudioState.ROUTE_BLUETOOTH -> {
            val devName = try {
                audioState?.activeBluetoothDevice?.name
            } catch (_: Exception) {
                null
            }
            devName ?: bluetoothFallback
        }
        CallAudioState.ROUTE_WIRED_HEADSET -> headsetLabel
        else -> speakerLabel
    }
}

@Composable
fun EndCallButton(
    compact: Boolean,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) (if (compact) 24.dp else 28.dp) else (if (compact) 32.dp else 36.dp),
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "endCallCorner"
    )

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Surface(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            onEndCall()
        },
        shape = RoundedCornerShape(cornerRadius),
        color = MaterialTheme.callColors.decline,
        contentColor = MaterialTheme.callColors.onDecline,
        tonalElevation = 6.dp,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(if (compact) 60.dp else 68.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = Icons.Default.CallEnd,
                contentDescription = stringResource(R.string.action_end_call),
                modifier = Modifier.size(if (compact) 28.dp else 32.dp),
                tint = MaterialTheme.callColors.onDecline
            )
        }
    }
}

@Composable
fun ActiveCallControls(
    callState: Int,
    isMuted: Boolean,
    audioState: CallAudioState?,
    showKeypad: Boolean,
    recordingEnabled: Boolean,
    isRecording: Boolean,
    compact: Boolean,
    canMerge: Boolean = false,
    canSwap: Boolean = false,
    hasConference: Boolean = false,
    isVideoCall: Boolean = false,
    canVideoCall: Boolean = false,
    onMergeCalls: () -> Unit = {},
    onSwapCalls: () -> Unit = {},
    onManageConference: () -> Unit = {},
    onToggleVideo: () -> Unit = {},
    onSwitchCamera: () -> Unit = {},
    onToggleMute: () -> Unit,
    onToggleKeypad: () -> Unit,
    onAudioClick: () -> Unit,
    onAddCall: () -> Unit,
    onToggleHold: () -> Unit,
    onMessage: () -> Unit,
    onToggleRecording: () -> Unit,
    onEndCall: () -> Unit,
    onNotesClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val audioRoute = audioState?.route ?: CallAudioState.ROUTE_EARPIECE
    val audioActive = when (audioRoute) {
        CallAudioState.ROUTE_SPEAKER -> true
        CallAudioState.ROUTE_BLUETOOTH -> true
        CallAudioState.ROUTE_WIRED_HEADSET -> true
        else -> false // Earpiece (handset) is the disabled state of the Speaker button
    }
    val isHolding = callState == Call.STATE_HOLDING

    val cellSpacing = if (compact) 8.dp else 12.dp

    val prefs = koinInject<PreferenceManager>()
    val settingsVersion by prefs.settingsChanged.collectAsState()
    val buttonOrder = remember(settingsVersion) {
        RivoCallButtonOrder.parse(
            prefs.getString(PreferenceManager.KEY_CALL_BUTTON_ORDER, PreferenceManager.DEFAULT_CALL_BUTTON_ORDER),
            RivoCallButtonOrder.hiddenSet(
                prefs.getString(PreferenceManager.KEY_HIDDEN_CALL_BUTTONS, "")
            )
        )
    }

    @Composable
    fun CallActionFor(id: String, modifier: Modifier) {
        when (id) {
            RivoCallButtonOrder.MUTE -> CallActionButton(
                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                isActive = isMuted,
                label = stringResource(R.string.action_mute),
                compact = compact,
                modifier = modifier,
                onClick = onToggleMute
            )
            RivoCallButtonOrder.KEYPAD -> CallActionButton(
                icon = Icons.Default.Dialpad,
                isActive = showKeypad,
                label = stringResource(R.string.action_keypad),
                compact = compact,
                modifier = modifier,
                onClick = onToggleKeypad
            )
            RivoCallButtonOrder.AUDIO -> CallActionButton(
                icon = callAudioRouteIcon(audioRoute),
                isActive = audioActive,
                label = callAudioRouteLabel(audioRoute, audioState),
                compact = compact,
                modifier = modifier,
                onClick = onAudioClick
            )
            RivoCallButtonOrder.RECORD -> CallActionButton(
                icon = if (isRecording) Icons.Default.StopCircle else Icons.Default.FiberManualRecord,
                isActive = isRecording,
                isDanger = isRecording,
                enabled = recordingEnabled,
                label = if (isRecording) {
                    stringResource(R.string.action_stop_recording)
                } else {
                    stringResource(R.string.action_record)
                },
                compact = compact,
                modifier = modifier,
                onClick = onToggleRecording
            )
            RivoCallButtonOrder.HOLD -> CallActionButton(
                icon = if (isHolding) Icons.Default.PlayArrow else Icons.Default.Pause,
                isActive = isHolding,
                label = if (isHolding) stringResource(R.string.action_resume) else stringResource(R.string.action_hold),
                compact = compact,
                modifier = modifier,
                onClick = onToggleHold
            )
            else -> when {
                canSwap -> CallActionButton(
                    icon = Icons.Default.SwapCalls,
                    isActive = false,
                    label = stringResource(R.string.action_swap),
                    compact = compact,
                    modifier = modifier,
                    onClick = onSwapCalls
                )
                canMerge -> CallActionButton(
                    icon = Icons.AutoMirrored.Outlined.CallMerge,
                    isActive = false,
                    label = stringResource(R.string.action_merge_calls),
                    compact = compact,
                    modifier = modifier,
                    onClick = onMergeCalls
                )
                else -> CallActionButton(
                    icon = Icons.Default.Add,
                    isActive = false,
                    label = stringResource(R.string.action_add_call),
                    compact = compact,
                    modifier = modifier,
                    onClick = onAddCall
                )
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Top Auxiliary Row: Tiny pills for Notes, Message, Video, and Swap / Manage Conference
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AuxiliaryPillButton(
                icon = Icons.AutoMirrored.Filled.Notes,
                label = "Notes",
                compact = compact,
                onClick = onNotesClick
            )
            AuxiliaryPillButton(
                icon = Icons.AutoMirrored.Filled.Message,
                label = stringResource(R.string.action_message),
                compact = compact,
                onClick = onMessage
            )
            if (canVideoCall || isVideoCall) {
                AuxiliaryPillButton(
                    icon = if (isVideoCall) Icons.Default.VideocamOff else Icons.Default.Videocam,
                    label = stringResource(R.string.video_call_toggle_video),
                    compact = compact,
                    onClick = onToggleVideo
                )
            }
            if (isVideoCall) {
                AuxiliaryPillButton(
                    icon = Icons.Default.Cameraswitch,
                    label = stringResource(R.string.video_call_switch_camera),
                    compact = compact,
                    onClick = onSwitchCamera
                )
            }
            if (canSwap && canMerge) {
                AuxiliaryPillButton(
                    icon = Icons.AutoMirrored.Outlined.CallMerge,
                    label = stringResource(R.string.action_merge_calls),
                    compact = compact,
                    onClick = onMergeCalls
                )
            }
            if (hasConference) {
                AuxiliaryPillButton(
                    icon = Icons.Default.Groups,
                    label = stringResource(R.string.conference_manage),
                    compact = compact,
                    onClick = onManageConference
                )
            }
        }

        Spacer(modifier = Modifier.height(if (compact) 10.dp else 14.dp))

        // Main 2x3 action grid laid out in the user's saved slot order.
        buttonOrder.chunked(3).forEachIndexed { rowIndex, rowIds ->
            if (rowIndex > 0) Spacer(modifier = Modifier.height(cellSpacing))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(cellSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowIds.forEach { id -> CallActionFor(id, Modifier.weight(1f)) }
            }
        }

        Spacer(modifier = Modifier.height(if (compact) 16.dp else 22.dp))

        // Bottom: Google Dialer style wide End Call pill
        EndCallButton(compact = compact, onEndCall = onEndCall)
    }
}
