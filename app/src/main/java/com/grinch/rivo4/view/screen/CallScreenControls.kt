package com.grinch.rivo4.view.screen

import android.telecom.Call
import android.telecom.CallAudioState
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
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
private fun EndCallButton(compact: Boolean, onEndCall: () -> Unit) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "endCallScale"
    )

    val barHeight = if (compact) 58.dp else 68.dp

    Surface(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            onEndCall()
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (compact) 24.dp else 48.dp)
            .height(barHeight)
            .scale(buttonScale),
        shape = CircleShape,
        color = MaterialTheme.callColors.decline,
        contentColor = MaterialTheme.callColors.onDecline,
        interactionSource = interactionSource
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.CallEnd,
                contentDescription = stringResource(R.string.action_end_call),
                modifier = Modifier.size(if (compact) 28.dp else 32.dp)
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(cellSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CallActionButton(
                icon = Icons.Default.Add,
                isActive = false,
                label = stringResource(R.string.action_add_call),
                compact = compact,
                modifier = Modifier.weight(1f),
                onClick = onAddCall
            )
            CallActionButton(
                icon = if (isHolding) Icons.Default.PlayArrow else Icons.Default.Pause,
                isActive = isHolding,
                label = if (isHolding) stringResource(R.string.action_resume) else stringResource(R.string.action_hold),
                compact = compact,
                modifier = Modifier.weight(1f),
                onClick = onToggleHold
            )
            if (recordingEnabled) {
                CallActionButton(
                    icon = if (isRecording) Icons.Default.StopCircle else Icons.Default.FiberManualRecord,
                    isActive = isRecording,
                    isDanger = isRecording,
                    label = if (isRecording) stringResource(R.string.action_stop_recording) else stringResource(R.string.action_record),
                    compact = compact,
                    modifier = Modifier.weight(1f),
                    onClick = onToggleRecording
                )
            } else {
                CallActionButton(
                    icon = Icons.AutoMirrored.Filled.Message,
                    isActive = false,
                    label = stringResource(R.string.action_message),
                    compact = compact,
                    modifier = Modifier.weight(1f),
                    onClick = onMessage
                )
            }
        }

        Spacer(modifier = Modifier.height(if (compact) 16.dp else 24.dp))
        EndCallButton(compact = compact, onEndCall = onEndCall)
    }
}
