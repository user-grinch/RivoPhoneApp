package com.grinch.rivo4.view.components

import com.grinch.rivo4.R
import android.provider.CallLog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.CallMerge
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Block
import androidx.compose.ui.graphics.vector.ImageVector
import com.grinch.rivo4.controller.util.PreferenceManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.modal.data.SwipeActionType
import com.grinch.rivo4.controller.util.ContactUtils
import com.grinch.rivo4.controller.util.formatDate
import com.grinch.rivo4.controller.util.formatPhoneNumber
import com.grinch.rivo4.controller.util.formatTime
import com.grinch.rivo4.modal.data.CallLogEntry

@Immutable
data class CallLogTileConfig(
    val showSim: Boolean = true,
    val swipeEnabled: Boolean = false,
    val swipeRightAction: SwipeActionType = SwipeActionType.CALL,
    val swipeLeftAction: SwipeActionType = SwipeActionType.MESSAGE,
    val displayOrder: Int = 0,
    val savedActions: List<String> = PreferenceManager.DEFAULT_CALL_LOG_ACTIONS_SAVED,
    val unsavedActions: List<String> = PreferenceManager.DEFAULT_CALL_LOG_ACTIONS_UNSAVED,
    val trailingAction: String = PreferenceManager.DEFAULT_CALL_LOG_TRAILING_BUTTON_ACTION
)

val LocalCallLogTileConfig: ProvidableCompositionLocal<CallLogTileConfig> =
    staticCompositionLocalOf { CallLogTileConfig() }

fun resolveSimNumber(log: CallLogEntry): Int? {
    if (log.simNumber != null) return log.simNumber
    val label = log.simLabel ?: return null
    val lower = label.lowercase()
    return when {
        lower.contains("sim 1") || lower.contains("sim1") || lower.contains("slot 1") || lower.contains("[1]") -> 1
        lower.contains("sim 2") || lower.contains("sim2") || lower.contains("slot 2") || lower.contains("[2]") -> 2
        lower.contains("1") && !lower.contains("2") -> 1
        lower.contains("2") && !lower.contains("1") -> 2
        else -> 1
    }
}

@Composable
fun SimBadge(
    simNumber: Int,
    modifier: Modifier = Modifier
) {
    val isSim1 = simNumber == 1
    val containerColor = if (isSim1) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
    }
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.SimCard,
                contentDescription = if (isSim1) stringResource(R.string.sim_slot_1) else stringResource(R.string.sim_slot_2),
                modifier = Modifier.size(10.5.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "$simNumber",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    lineHeight = 10.sp
                )
            )
        }
    }
}

@Composable
fun HdBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = "HD",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    lineHeight = 10.sp
                )
            )
        }
    }
}

@Composable
fun CallLogTileSimple(
    log: CallLogEntry,
    contact: Contact? = null,
    displayOrder: Int = LocalCallLogTileConfig.current.displayOrder,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onCallClick: () -> Unit = {},
    selected: Boolean = false,
    showSim: Boolean = LocalCallLogTileConfig.current.showSim,
    swipeEnabled: Boolean = LocalCallLogTileConfig.current.swipeEnabled && !selected,
    swipeRightAction: SwipeActionType = LocalCallLogTileConfig.current.swipeRightAction,
    swipeLeftAction: SwipeActionType = LocalCallLogTileConfig.current.swipeLeftAction,
    onSwipeAction: ((SwipeActionType, CallLogEntry) -> Unit)? = null,
    shape: Shape = RoundedCornerShape(24.dp)
) {
    val isBlocked = log.isBlocked || log.type == CallLog.Calls.BLOCKED_TYPE
    val isMissedOrRejected = log.type == CallLog.Calls.MISSED_TYPE || log.type == CallLog.Calls.REJECTED_TYPE

    val icon = remember(log.type, isBlocked) {
        if (isBlocked) {
            Icons.Default.Block
        } else {
            when (log.type) {
                CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
                CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
                CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE -> Icons.AutoMirrored.Filled.CallMissed
                else -> Icons.Default.Call
            }
        }
    }

    val badgeColor = when {
        isBlocked -> MaterialTheme.colorScheme.error
        isMissedOrRejected -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val headlineColor = when {
        isBlocked -> MaterialTheme.colorScheme.error
        isMissedOrRejected -> MaterialTheme.colorScheme.error
        else -> Color.Unspecified
    }

    val displayName = remember(log.name, log.number, contact, displayOrder) {
        if (contact != null) {
            ContactUtils.formatContactName(contact, displayOrder)
        } else {
            log.name?.let {
                if (it.isNotEmpty()) ContactUtils.formatContactName(it, displayOrder) else null
            } ?: formatPhoneNumber(log.number)
        }
    }

    val headlineText = displayName

    val context = LocalContext.current
    val callTypeLabel = when {
        isBlocked -> stringResource(R.string.call_type_blocked)
        log.type == CallLog.Calls.INCOMING_TYPE -> stringResource(R.string.call_type_incoming)
        log.type == CallLog.Calls.OUTGOING_TYPE -> stringResource(R.string.call_type_outgoing)
        log.type == CallLog.Calls.MISSED_TYPE || log.type == CallLog.Calls.REJECTED_TYPE -> stringResource(R.string.call_type_missed)
        else -> stringResource(R.string.action_call)
    }

    val supportingText = remember(log.date, log.duration, callTypeLabel) {
        buildString {
            append(callTypeLabel)
            append(" • ")
            append(formatDate(context, log.date))
            if (log.duration > 0) {
                append(" • ${android.text.format.DateUtils.formatElapsedTime(log.duration)}")
            }
        }
    }

    val resolvedSimNumber = log.simNumber ?: resolveSimNumber(log)
    val hasSupportingBadge = (showSim && resolvedSimNumber != null) || log.isHd

    @Composable
    fun ContentBox() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = onClick,
                        onLongClick = onLongClick
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    RivoListItem(
                        headline = headlineText,
                        supporting = if (!hasSupportingBadge) supportingText else null,
                        supportingContent = if (hasSupportingBadge) {
                            {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 1.dp)
                                ) {
                                    if (showSim && resolvedSimNumber != null) {
                                        SimBadge(simNumber = resolvedSimNumber)
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    if (log.isHd) {
                                        HdBadge()
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = supportingText,
                                        style = RivoListItemDefaults.supportingStyle(),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else null,
                        supporting2 = null,
                        avatarName = displayName,
                        photoUri = log.photoUri,
                        badgeIcon = icon,
                        badgeColor = badgeColor,
                        headlineColor = headlineColor,
                        onClick = null,
                        onLongClick = null,
                        selected = false,
                        containerColor = Color.Transparent
                    )
                }

                if (!selected) {
                    IconButton(
                        onClick = onCallClick,
                        modifier = Modifier.padding(end = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = stringResource(R.string.action_call),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }

    if (swipeEnabled && onSwipeAction != null) {
        RivoSwipeToActionBox(
            enabled = true,
            swipeRightAction = swipeRightAction,
            swipeLeftAction = swipeLeftAction,
            onTriggerAction = { action -> onSwipeAction(action, log) }
        ) {
            ContentBox()
        }
    } else {
        ContentBox()
    }
}

data class CallLogActionInfo(
    val key: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun getCallLogActionInfo(key: String): CallLogActionInfo? = when (key) {
    "save" -> CallLogActionInfo("save", stringResource(R.string.action_save), Icons.Outlined.PersonAdd)
    "view_contact" -> CallLogActionInfo("view_contact", stringResource(R.string.swipe_action_view_contact), Icons.Outlined.Person)
    "block" -> CallLogActionInfo("block", stringResource(R.string.swipe_action_block), Icons.Outlined.Block)
    "message" -> CallLogActionInfo("message", stringResource(R.string.action_message), Icons.AutoMirrored.Outlined.Message)
    "call" -> CallLogActionInfo("call", stringResource(R.string.action_call), Icons.Rounded.Call)
    "video_call" -> CallLogActionInfo("video_call", stringResource(R.string.contact_details_video), Icons.Outlined.Videocam)
    "history" -> CallLogActionInfo("history", stringResource(R.string.action_history), Icons.Outlined.History)
    "share" -> CallLogActionInfo("share", stringResource(R.string.swipe_action_share), Icons.Outlined.Share)
    "whatsapp" -> CallLogActionInfo("whatsapp", stringResource(R.string.swipe_action_whatsapp), Icons.Outlined.ChatBubbleOutline)
    "copy_number" -> CallLogActionInfo("copy_number", stringResource(R.string.swipe_action_copy_number), Icons.Outlined.ContentCopy)
    else -> null
}

@Composable
fun CallLogTile(
    log: CallLogEntry,
    contact: Contact? = null,
    onTileClick: (CallLogEntry) -> Unit,
    onButtonClick: (CallLogEntry) -> Unit,
    onAvatarClick: ((CallLogEntry) -> Unit)? = null,
    onLongClick: (CallLogEntry) -> Unit = {},
    selected: Boolean = false,
    displayOrder: Int = LocalCallLogTileConfig.current.displayOrder,
    showSim: Boolean = LocalCallLogTileConfig.current.showSim,
    isFavorite: Boolean = false,
    swipeEnabled: Boolean = LocalCallLogTileConfig.current.swipeEnabled && !selected,
    swipeRightAction: SwipeActionType = LocalCallLogTileConfig.current.swipeRightAction,
    swipeLeftAction: SwipeActionType = LocalCallLogTileConfig.current.swipeLeftAction,
    onSwipeAction: ((SwipeActionType, CallLogEntry) -> Unit)? = null,
    isOptionsExpanded: Boolean = false,
    savedActions: List<String> = LocalCallLogTileConfig.current.savedActions,
    unsavedActions: List<String> = LocalCallLogTileConfig.current.unsavedActions,
    trailingAction: String = LocalCallLogTileConfig.current.trailingAction,
    onActionClick: ((String, CallLogEntry) -> Unit)? = null,
    onAddContact: ((CallLogEntry) -> Unit)? = null,
    onBlockContact: ((CallLogEntry) -> Unit)? = null,
    shape: Shape = RoundedCornerShape(24.dp)
) {
    val isBlocked = log.isBlocked || log.type == CallLog.Calls.BLOCKED_TYPE
    val isMissedOrRejected = log.type == CallLog.Calls.MISSED_TYPE || log.type == CallLog.Calls.REJECTED_TYPE

    val isMergedMixed = remember(log.types) {
        if (log.types.size > 1) {
            val distinctTypes = log.types.toSet()
            distinctTypes.size > 1
        } else false
    }

    val icon = remember(log.type, isBlocked, isMergedMixed) {
        if (isBlocked) {
            Icons.Default.Block
        } else if (isMergedMixed) {
            Icons.AutoMirrored.Outlined.CallMerge
        } else {
            when (log.type) {
                CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE -> Icons.AutoMirrored.Filled.CallMissed
                CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
                CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
                else -> Icons.Default.Call
            }
        }
    }

    val badgeColor = when {
        isBlocked -> MaterialTheme.colorScheme.error
        isMissedOrRejected -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val headlineColor = when {
        isBlocked -> MaterialTheme.colorScheme.error
        isMissedOrRejected -> MaterialTheme.colorScheme.error
        else -> Color.Unspecified
    }

    val displayName = remember(log.name, log.number, contact, displayOrder) {
        if (contact != null) {
            ContactUtils.formatContactName(contact, displayOrder)
        } else {
            log.name?.let {
                if (it.isNotEmpty()) ContactUtils.formatContactName(it, displayOrder) else null
            } ?: formatPhoneNumber(log.number)
        }
    }

    val headlineText = displayName

    val context = LocalContext.current
    val timeText = remember(log.date) {
        formatTime(context, log.date)
    }
    val resolvedSimNumber = log.simNumber ?: resolveSimNumber(log)

    val isStacked = log.count > 1
    var isHistoryExpanded by remember { mutableStateOf(false) }
    val isUnsaved = contact == null && (log.contactId == null || log.contactId == "null") && (log.name == null || log.name == log.number)
    val hasSupportingBadge = (showSim && resolvedSimNumber != null) || log.isHd

    val tileBgColor = when {
        selected -> MaterialTheme.colorScheme.secondaryContainer
        isOptionsExpanded -> MaterialTheme.colorScheme.surfaceContainer
        else -> Color.Transparent
    }
    val animatedTileBgColor by animateColorAsState(
        targetValue = tileBgColor,
        label = "CallLogTileBackground"
    )

    @Composable
    fun ContentBox() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(animatedTileBgColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = { onTileClick(log) },
                        onLongClick = { onLongClick(log) }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    RivoListItem(
                        headline = headlineText,
                        supporting = if (!hasSupportingBadge) timeText else null,
                        supportingContent = if (hasSupportingBadge) {
                            {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 1.dp)
                                ) {
                                    if (showSim && resolvedSimNumber != null) {
                                        SimBadge(simNumber = resolvedSimNumber)
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    if (log.isHd) {
                                        HdBadge()
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = timeText,
                                        style = RivoListItemDefaults.supportingStyle(),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else null,
                        supporting2 = null,
                        avatarName = null,
                        photoUri = null,
                        badgeIcon = null,
                        badgeColor = null,
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .padding(bottom = if (isStacked) 4.dp else 0.dp)
                                    .size(RivoListItemDefaults.AvatarSize)
                            ) {
                                RivoAvatar(
                                    name = displayName,
                                    photoUri = log.photoUri,
                                    badgeIcon = icon,
                                    badgeColor = badgeColor,
                                    onClick = { onAvatarClick?.invoke(log) ?: onTileClick(log) },
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isStacked) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .offset(x = (-2).dp, y = 3.dp)
                                            .height(18.dp)
                                            .widthIn(min = 24.dp),
                                        shape = RoundedCornerShape(9.dp),
                                        color = if (isOptionsExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        shadowElevation = RivoElevation.Raised
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "${log.count}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = if (isOptionsExpanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(1.dp))
                                            Icon(
                                                imageVector = if (isOptionsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                modifier = Modifier.size(11.dp),
                                                tint = if (isOptionsExpanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        headlineColor = headlineColor,
                        trailingIcon = if (isFavorite) Icons.Default.Star else null,
                        onClick = null,
                        onLongClick = null,
                        selected = false,
                        containerColor = Color.Transparent
                    )
                }

                if (!selected && trailingAction != "none") {
                    val trailingIcon = when (trailingAction) {
                        "message" -> Icons.AutoMirrored.Outlined.Message
                        "video_call" -> Icons.Outlined.Videocam
                        "whatsapp" -> Icons.Outlined.ChatBubbleOutline
                        "view_contact" -> Icons.Outlined.Person
                        "history" -> Icons.Outlined.History
                        else -> Icons.Rounded.Call
                    }
                    val trailingTint = when (trailingAction) {
                        "message" -> MaterialTheme.colorScheme.secondary
                        "video_call" -> MaterialTheme.colorScheme.tertiary
                        "whatsapp" -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.primary
                    }
                    IconButton(
                        onClick = { onButtonClick(log) },
                        modifier = Modifier.padding(end = 10.dp)
                    ) {
                        Icon(
                            imageVector = trailingIcon,
                            contentDescription = null,
                            tint = trailingTint
                        )
                    }
                }
            }

            if (isUnsaved && !selected && !isOptionsExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 72.dp, end = 16.dp, top = 2.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onAddContact?.invoke(log) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PersonAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.action_save),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onBlockContact?.invoke(log) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Report,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.swipe_action_block),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            @Composable
            fun SubCallsList(subItems: List<CallLogEntry>) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    subItems.forEachIndexed { subIndex, subLog ->
                        val subBlocked = subLog.isBlocked || subLog.type == CallLog.Calls.BLOCKED_TYPE
                        val subMissed = subLog.type == CallLog.Calls.MISSED_TYPE || subLog.type == CallLog.Calls.REJECTED_TYPE
                        val subIcon = when {
                            subBlocked -> Icons.Default.Block
                            subLog.type == CallLog.Calls.MISSED_TYPE || subLog.type == CallLog.Calls.REJECTED_TYPE -> Icons.AutoMirrored.Filled.CallMissed
                            subLog.type == CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
                            subLog.type == CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
                            else -> Icons.Default.Call
                        }
                        val subColor = when {
                            subBlocked || subMissed -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        }
                        val subTypeLabel = when {
                            subBlocked -> stringResource(R.string.call_type_blocked)
                            subLog.type == CallLog.Calls.INCOMING_TYPE -> stringResource(R.string.call_type_incoming)
                            subLog.type == CallLog.Calls.OUTGOING_TYPE -> stringResource(R.string.call_type_outgoing)
                            subMissed -> stringResource(R.string.call_type_missed)
                            else -> stringResource(R.string.action_call)
                        }

                        val subShape = rivoGroupedItemShape(subIndex, subItems.size)
                        Surface(
                            shape = subShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(subShape)
                                .clickable { onTileClick(subLog) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 9.dp, horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = subIcon,
                                    contentDescription = null,
                                    tint = subColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = subTypeLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = subColor
                                )
                                val subSimNumber = subLog.simNumber ?: resolveSimNumber(subLog)
                                if (showSim && subSimNumber != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    SimBadge(simNumber = subSimNumber)
                                }
                                if (subLog.isHd) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    HdBadge()
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = formatTime(context, subLog.date),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (subLog.duration > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${android.text.format.DateUtils.formatElapsedTime(subLog.duration)})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isOptionsExpanded && !selected,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    if (isStacked && log.subLogs.isNotEmpty()) {
                        SubCallsList(log.subLogs)
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val actionsToDisplay = if (isUnsaved) unsavedActions else savedActions
                        actionsToDisplay.forEachIndexed { index, actionKey ->
                            val actionInfo = getCallLogActionInfo(actionKey)
                            if (actionInfo != null) {
                                val itemShape = rivoGroupedItemShape(index, actionsToDisplay.size)
                                Surface(
                                    shape = itemShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(itemShape)
                                        .clickable { onActionClick?.invoke(actionKey, log) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 18.dp, vertical = 13.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = actionInfo.icon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Text(
                                            text = actionInfo.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isHistoryExpanded && !isOptionsExpanded && !selected,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val subItems = if (log.subLogs.isNotEmpty()) log.subLogs else listOf(log)
                SubCallsList(subItems)
            }
        }
    }

    if (swipeEnabled && onSwipeAction != null) {
        RivoSwipeToActionBox(
            enabled = true,
            swipeRightAction = swipeRightAction,
            swipeLeftAction = swipeLeftAction,
            onTriggerAction = { action -> onSwipeAction(action, log) }
        ) {
            ContentBox()
        }
    } else {
        ContentBox()
    }
}

@Composable
fun BatchCallLogActionBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onDelete: () -> Unit,
    onSelectAll: (() -> Unit)? = null,
    allSelected: Boolean = false
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, stringResource(R.string.action_clear_selection))
            }
            Text(
                text = pluralStringResource(R.plurals.selection_count_selected, selectedCount, selectedCount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            if (onSelectAll != null) {
                IconButton(onClick = onSelectAll) {
                    Icon(
                        imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                        contentDescription = stringResource(
                            if (allSelected) R.string.action_select_none else R.string.action_select_all
                        )
                    )
                }
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, stringResource(R.string.content_desc_delete_selected))
            }
        }
    }

    if (showDeleteConfirm) {
        RivoConfirmationDialog(
            onDismissRequest = { showDeleteConfirm = false },
            onConfirm = onDelete,
            title = stringResource(R.string.call_log_delete_title),
            message = pluralStringResource(R.plurals.call_log_delete_confirm, selectedCount, selectedCount),
            confirmLabel = stringResource(R.string.action_delete),
            dismissLabel = stringResource(R.string.action_cancel),
            icon = Icons.Default.Delete,
            isDestructive = true
        )
    }
}
