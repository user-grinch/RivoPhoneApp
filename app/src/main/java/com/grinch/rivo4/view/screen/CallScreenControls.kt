package com.grinch.rivo4.view.screen

import android.telecom.Call
import android.telecom.CallAudioState
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
import com.grinch.rivo4.view.theme.callColors

@Composable
private fun callAudioRouteLabel(route: Int): String = when (route) {
    CallAudioState.ROUTE_SPEAKER -> stringResource(R.string.audio_route_speaker)
    CallAudioState.ROUTE_BLUETOOTH -> stringResource(R.string.audio_route_bluetooth)
    CallAudioState.ROUTE_WIRED_HEADSET -> stringResource(R.string.audio_route_headset)
    else -> stringResource(R.string.audio_route_handset)
}

private fun callAudioRouteIcon(route: Int): ImageVector = when (route) {
    CallAudioState.ROUTE_SPEAKER -> Icons.AutoMirrored.Filled.VolumeUp
    CallAudioState.ROUTE_BLUETOOTH -> Icons.Default.Bluetooth
    CallAudioState.ROUTE_WIRED_HEADSET -> Icons.Default.Headset
    else -> Icons.Default.Phone
}

@Composable
fun CallActionButton(
    icon: ImageVector,
    isActive: Boolean,
    label: String,
    enabled: Boolean = true,
    compact: Boolean = false,
    isDanger: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "CallActionScale"
    )

    val restingRadius = if (compact) 22.dp else 28.dp
    val cornerRadius by animateFloatAsState(
        targetValue = if (isPressed) restingRadius.value * 1.6f else restingRadius.value,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "CallActionCorner"
    )

    val containerColor by animateColorAsState(
        targetValue = when {
            !enabled -> scheme.surfaceContainerHigh.copy(alpha = 0.4f)
            isDanger && isActive -> MaterialTheme.callColors.decline
            isActive -> scheme.primary
            else -> scheme.surfaceContainerHigh
        },
        label = "CallActionContainer"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> scheme.onSurface.copy(alpha = 0.38f)
            isDanger && isActive -> MaterialTheme.callColors.onDecline
            isActive -> scheme.onPrimary
            else -> scheme.onSurfaceVariant
        },
        label = "CallActionContent"
    )

    val buttonHeight = if (compact) 60.dp else 74.dp
    val iconSize = if (compact) 22.dp else 26.dp

    Surface(
        onClick = {
            if (enabled) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
        },
        modifier = modifier
            .height(buttonHeight)
            .scale(scale),
        enabled = enabled,
        shape = RoundedCornerShape(cornerRadius.dp),
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = contentColor
            )
            Spacer(modifier = Modifier.height(if (compact) 3.dp else 5.dp))
            Text(
                text = label,
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AuxiliaryPillButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val view = LocalView.current
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "AuxPillScale"
    )

    Surface(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onClick()
        },
        modifier = modifier
            .height(if (compact) 32.dp else 36.dp)
            .scale(scale),
        shape = CircleShape,
        color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
        contentColor = scheme.onSurface,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.35f)),
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 12.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(if (compact) 14.dp else 16.dp),
                tint = scheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EndCallButton(compact: Boolean, onEndCall: () -> Unit) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "endCallScale"
    )

    val buttonHeight = if (compact) 54.dp else 62.dp

    Surface(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            onEndCall()
        },
        modifier = Modifier
            .fillMaxWidth(if (compact) 0.88f else 0.84f)
            .height(buttonHeight)
            .scale(buttonScale),
        shape = CircleShape,
        color = MaterialTheme.callColors.decline,
        contentColor = MaterialTheme.callColors.onDecline,
        shadowElevation = 4.dp,
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CallEnd,
                contentDescription = stringResource(R.string.action_end_call),
                modifier = Modifier.size(if (compact) 26.dp else 30.dp),
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
    val audioActive = audioRoute == CallAudioState.ROUTE_SPEAKER ||
        audioRoute == CallAudioState.ROUTE_BLUETOOTH
    val isHolding = callState == Call.STATE_HOLDING

    val cellSpacing = if (compact) 8.dp else 12.dp

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Top Auxiliary Row: Tiny pills for Notes and Message
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
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
        }

        Spacer(modifier = Modifier.height(if (compact) 10.dp else 14.dp))

        // Main 6-button Grid: Row 1 (Mute, Keypad, Audio)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(cellSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CallActionButton(
                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                isActive = isMuted,
                label = stringResource(R.string.action_mute),
                compact = compact,
                modifier = Modifier.weight(1f),
                onClick = onToggleMute
            )
            CallActionButton(
                icon = Icons.Default.Dialpad,
                isActive = showKeypad,
                label = stringResource(R.string.action_keypad),
                compact = compact,
                modifier = Modifier.weight(1f),
                onClick = onToggleKeypad
            )
            CallActionButton(
                icon = callAudioRouteIcon(audioRoute),
                isActive = audioActive,
                label = callAudioRouteLabel(audioRoute),
                compact = compact,
                modifier = Modifier.weight(1f),
                onClick = onAudioClick
            )
        }

        Spacer(modifier = Modifier.height(cellSpacing))

        // Main 6-button Grid: Row 2 (Record, Hold, Add Call)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(cellSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CallActionButton(
                icon = if (isRecording) Icons.Default.StopCircle else Icons.Default.FiberManualRecord,
                isActive = isRecording,
                isDanger = isRecording,
                enabled = recordingEnabled,
                label = if (isRecording) stringResource(R.string.action_stop_recording) else stringResource(R.string.action_record),
                compact = compact,
                modifier = Modifier.weight(1f),
                onClick = onToggleRecording
            )
            CallActionButton(
                icon = if (isHolding) Icons.Default.PlayArrow else Icons.Default.Pause,
                isActive = isHolding,
                label = if (isHolding) stringResource(R.string.action_resume) else stringResource(R.string.action_hold),
                compact = compact,
                modifier = Modifier.weight(1f),
                onClick = onToggleHold
            )
            CallActionButton(
                icon = Icons.Default.Add,
                isActive = false,
                label = stringResource(R.string.action_add_call),
                compact = compact,
                modifier = Modifier.weight(1f),
                onClick = onAddCall
            )
        }

        Spacer(modifier = Modifier.height(if (compact) 16.dp else 22.dp))

        // Bottom: Google Dialer style wide End Call pill
        EndCallButton(compact = compact, onEndCall = onEndCall)
    }
}
