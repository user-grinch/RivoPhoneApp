package com.grinch.rivo4.modal.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.PreferenceManager

enum class SwipeActionType(
    val id: Int,
    val titleRes: Int,
    val icon: ImageVector
) {
    NONE(
        id = PreferenceManager.SWIPE_ACTION_NONE,
        titleRes = R.string.swipe_action_none,
        icon = Icons.Default.DoNotDisturb
    ),
    CALL(
        id = PreferenceManager.SWIPE_ACTION_CALL,
        titleRes = R.string.swipe_action_call,
        icon = Icons.Default.Call
    ),
    MESSAGE(
        id = PreferenceManager.SWIPE_ACTION_MESSAGE,
        titleRes = R.string.swipe_action_message,
        icon = Icons.AutoMirrored.Filled.Message
    ),
    VIDEO_CALL(
        id = PreferenceManager.SWIPE_ACTION_VIDEO_CALL,
        titleRes = R.string.swipe_action_video_call,
        icon = Icons.Default.VideoCall
    ),
    WHATSAPP(
        id = PreferenceManager.SWIPE_ACTION_WHATSAPP,
        titleRes = R.string.swipe_action_whatsapp,
        icon = Icons.AutoMirrored.Filled.Chat
    ),
    COPY_NUMBER(
        id = PreferenceManager.SWIPE_ACTION_COPY_NUMBER,
        titleRes = R.string.swipe_action_copy_number,
        icon = Icons.Default.ContentCopy
    ),
    DELETE(
        id = PreferenceManager.SWIPE_ACTION_DELETE,
        titleRes = R.string.swipe_action_delete,
        icon = Icons.Default.Delete
    ),
    VIEW_CONTACT(
        id = PreferenceManager.SWIPE_ACTION_VIEW_CONTACT,
        titleRes = R.string.swipe_action_view_contact,
        icon = Icons.Default.Person
    ),
    SHARE(
        id = PreferenceManager.SWIPE_ACTION_SHARE,
        titleRes = R.string.swipe_action_share,
        icon = Icons.Default.Share
    ),
    BLOCK(
        id = PreferenceManager.SWIPE_ACTION_BLOCK,
        titleRes = R.string.swipe_action_block,
        icon = Icons.Default.Block
    );

    @Composable
    fun containerColor(): Color = when (this) {
        NONE -> MaterialTheme.colorScheme.surfaceContainerHigh
        CALL -> Color(0xFF2E7D32)
        MESSAGE -> Color(0xFF1565C0)
        VIDEO_CALL -> Color(0xFF6A1B9A)
        WHATSAPP -> Color(0xFF25D366)
        COPY_NUMBER -> MaterialTheme.colorScheme.secondaryContainer
        DELETE -> MaterialTheme.colorScheme.errorContainer
        VIEW_CONTACT -> Color(0xFF00897B)
        SHARE -> Color(0xFF5E35B1)
        BLOCK -> MaterialTheme.colorScheme.error
    }

    @Composable
    fun contentColor(): Color = when (this) {
        NONE -> MaterialTheme.colorScheme.onSurfaceVariant
        CALL -> Color.White
        MESSAGE -> Color.White
        VIDEO_CALL -> Color.White
        WHATSAPP -> Color.White
        COPY_NUMBER -> MaterialTheme.colorScheme.onSecondaryContainer
        DELETE -> MaterialTheme.colorScheme.onErrorContainer
        VIEW_CONTACT -> Color.White
        SHARE -> Color.White
        BLOCK -> Color.White
    }

    companion object {
        fun fromId(id: Int): SwipeActionType {
            return entries.firstOrNull { it.id == id } ?: NONE
        }
    }
}
