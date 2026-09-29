package com.grinch.rivo4.view.screen

import androidx.compose.material.icons.automirrored.outlined.CallMerge

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
import com.grinch.rivo4.view.theme.RivoMaterialShapes
import com.grinch.rivo4.view.theme.callColors

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

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "btnScale"
    )

    val containerColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            isDanger -> MaterialTheme.callColors.decline
            isActive -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "btnBg"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            isDanger -> MaterialTheme.callColors.onDecline
            isActive -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "btnFg"
    )

    val buttonSize = if (compact) 54.dp else 64.dp
    val iconSize = if (compact) 24.dp else 28.dp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            onClick = {
                if (enabled) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onClick()
                }
            },
            enabled = enabled,
            shape = CircleShape,
            color = containerColor,
            tonalElevation = if (isActive || isDanger) 6.dp else 1.dp,
            interactionSource = interactionSource,
            modifier = Modifier
                .size(buttonSize)
                .scale(scale)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
        Spacer(modifier = Modifier.height(if (compact) 4.dp else 6.dp))
        Text(
            text = label,
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.38f
            ),
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pillScale"
    )

    Surface(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            onClick()
        },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        interactionSource = interactionSource,
        modifier = modifier
            .scale(scale)
            .height(if (compact) 32.dp else 36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 12.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(if (compact) 14.dp else 16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun callAudioRouteIcon(route: Int): ImageVector = when (route) {
    CallAudioState.ROUTE_SPEAKER -> Icons.AutoMirrored.Filled.VolumeUp
    CallAudioState.ROUTE_BLUETOOTH -> Icons.Default.Bluetooth
    CallAudioState.ROUTE_WIRED_HEADSET -> Icons.Default.Headset
    else -> Icons.Default.Phone
}

@Composable
private fun callAudioRouteLabel(route: Int): String = when (route) {
    CallAudioState.ROUTE_SPEAKER -> stringResource(R.string.audio_route_speaker)
    CallAudioState.ROUTE_BLUETOOTH -> stringResource(R.string.audio_route_bluetooth)
    CallAudioState.ROUTE_WIRED_HEADSET -> stringResource(R.string.audio_route_headset)
    else -> stringResource(R.string.audio_route_handset)
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

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "endScale"
    )

    Surface(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            onEndCall()
        },
        shape = CircleShape,
        color = MaterialTheme.callColors.decline,
        tonalElevation = 6.dp,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth(if (compact) 0.65f else 0.72f)
            .height(if (compact) 56.dp else 64.dp)
            .scale(scale)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
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
    canMerge: Boolean = false,
    canSwap: Boolean = false,
    hasConference: Boolean = false,
    onMergeCalls: () -> Unit = {},
    onSwapCalls: () -> Unit = {},
    onManageConference: () -> Unit = {},
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
        // Top Auxiliary Row: Tiny pills for Notes, Message, and Swap / Manage Conference
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
            if (canSwap) {
                AuxiliaryPillButton(
                    icon = Icons.Default.SwapCalls,
                    label = stringResource(R.string.action_swap),
                    compact = compact,
                    onClick = onSwapCalls
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
        
        // Main 6-button Grid: Row 2 (Record, Hold, Add/Merge/Swap Call)
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
        label =
            if (isRecording) stringResource(R.string.action_stop_recording) else stringResource(R.string.action_record), 
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
        if (canMerge) {
            
            CallActionButton(
            icon = Icons.AutoMirrored.Outlined.CallMerge, 
            isActive = false, 
            label = stringResource(R.string.action_merge_calls), 
            compact = compact, 
            modifier = Modifier.weight(1f), 
            onClick = onMergeCalls
            )
        } else if (canSwap) {
            
            CallActionButton(
            icon = Icons.Default.SwapCalls, 
            isActive = false, 
            label = stringResource(R.string.action_swap), 
            compact = compact, 
            modifier = Modifier.weight(1f), 
            onClick = onSwapCalls
            )
        } else {
            
            CallActionButton(
            icon = Icons.Default.Add, 
            isActive = false, 
            label = stringResource(R.string.action_add_call), 
            compact = compact, 
            modifier = Modifier.weight(1f), 
            onClick = onAddCall
            )
        }
    }
        
        Spacer(modifier = Modifier.height(if (compact) 16.dp else 22.dp))
        
        // Bottom: Google Dialer style wide End Call pill
        EndCallButton(compact = compact, onEndCall = onEndCall)
    }
}
