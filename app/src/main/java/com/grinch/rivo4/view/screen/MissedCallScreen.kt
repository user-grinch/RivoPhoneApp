package com.grinch.rivo4.view.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.grinch.rivo4.MainActivity
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.SocialUtils
import com.grinch.rivo4.controller.util.makeCall
import com.grinch.rivo4.view.components.CallNotesSheet
import com.grinch.rivo4.view.components.RivoAvatar
import com.grinch.rivo4.view.components.ad.PostCallNativeAd
import java.util.Date

@Composable
fun MissedCallScreen(
    contactName: String,
    phoneNumber: String,
    photoUri: String?,
    ringSeconds: Int,
    timestampMillis: Long,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }
    var showNoteSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    BackHandler {
        onDismiss()
    }

    val installedSocialApps = remember {
        SocialUtils.getInstalledSocialApps(context)
    }

    val quickReplies = remember {
        listOf(
            context.getString(R.string.missed_call_msg_call_me_back),
            context.getString(R.string.missed_call_msg_sorry_busy),
            context.getString(R.string.missed_call_msg_call_right_back),
            context.getString(R.string.missed_call_msg_cant_talk)
        )
    }

    val displayName = remember(contactName, phoneNumber) {
        contactName.ifEmpty { phoneNumber.ifEmpty { context.getString(R.string.label_unknown) } }
    }

    val isUnsaved = remember(contactName, phoneNumber) {
        contactName.isEmpty() || contactName == phoneNumber
    }

    val timeFormatted = remember(timestampMillis) {
        DateFormat.getTimeFormat(context).format(Date(timestampMillis))
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(250)),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume clicks inside card
                    ),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 4.dp,
                shadowElevation = 14.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Top drag indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Top Status Bar: Logo + Rivo text + Missed Call + Close ✕ button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.logo),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = "RIVO",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.notif_channel_missed_calls),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Prominent ✕ Close Button
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(34.dp),
                            onClick = onDismiss
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Caller Card (Elevated Truecaller-style container)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(54.dp),
                                    contentAlignment = Alignment.TopStart
                                ) {
                                    RivoAvatar(
                                        name = displayName,
                                        photoUri = photoUri,
                                        modifier = Modifier
                                            .size(50.dp)
                                            .align(Alignment.Center)
                                    )
                                    Surface(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .align(Alignment.TopStart),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.error,
                                        shadowElevation = 2.dp
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.CallMissed,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onError,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    val missedStatusText = if (ringSeconds > 0) {
                                        stringResource(R.string.missed_call_just_now, ringSeconds)
                                    } else {
                                        "Missed call • $timeFormatted"
                                    }

                                    Text(
                                        text = if (displayName != phoneNumber && phoneNumber.isNotEmpty()) {
                                            "$phoneNumber • $missedStatusText"
                                        } else {
                                            missedStatusText
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Quick SMS reply chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                quickReplies.forEach { text ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        border = BorderStroke(
                                            width = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                        ),
                                        onClick = {
                                            val body = if (text.startsWith("Type custom")) "" else text
                                            sendOrComposeSms(context, phoneNumber, body)
                                            onDismiss()
                                        }
                                    ) {
                                        Text(
                                            text = text,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action buttons row (Call, SMS, Note, Save)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Call
                                FilledTonalButton(
                                    onClick = {
                                        makeCall(context, phoneNumber)
                                        onDismiss()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(stringResource(R.string.favorites_action_call), style = MaterialTheme.typography.labelMedium)
                                }

                                // Message
                                FilledTonalButton(
                                    onClick = {
                                        SocialUtils.openSms(context, phoneNumber)
                                        onDismiss()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(stringResource(R.string.favorites_action_message), style = MaterialTheme.typography.labelMedium)
                                }

                                // Note
                                FilledTonalButton(
                                    onClick = { showNoteSheet = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(Icons.Outlined.EditNote, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("Note", style = MaterialTheme.typography.labelMedium)
                                }

                                // If unsaved number, show "Save" button
                                if (isUnsaved && phoneNumber.isNotEmpty()) {
                                    FilledTonalButton(
                                        onClick = {
                                            addContact(context, phoneNumber)
                                            onDismiss()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(17.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("Save", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }

                            // Social apps row (WhatsApp, Telegram, etc.) if any installed
                            if (installedSocialApps.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    installedSocialApps.take(4).forEach { app ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                                            onClick = {
                                                app.action(context, phoneNumber)
                                                onDismiss()
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (app.iconDrawable != null) {
                                                    val bitmap = remember(app.iconDrawable) {
                                                        runCatching { app.iconDrawable.toBitmap(48, 48) }.getOrNull()
                                                    }
                                                    if (bitmap != null) {
                                                        Image(
                                                            bitmap = bitmap.asImageBitmap(),
                                                            contentDescription = app.name,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(Modifier.width(6.dp))
                                                    }
                                                }
                                                Text(
                                                    text = app.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // View Call Logs Pill Button
                            Surface(
                                onClick = {
                                    val intent = Intent(context, MainActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    }
                                    context.startActivity(intent)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp),
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.missed_call_view_logs),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Big Ad Card (Native Advanced with MediaView in Play variant)
                    PostCallNativeAd(
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        if (showNoteSheet) {
            CallNotesSheet(
                phoneNumber = phoneNumber,
                contactName = contactName,
                onDismiss = { showNoteSheet = false }
            )
        }
    }
}

private fun sendOrComposeSms(context: Context, number: String, text: String) {
    val intl = SocialUtils.formatInternationalNumber(context, number)
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$intl")).apply {
        if (text.isNotEmpty()) {
            putExtra("sms_body", text)
        }
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {}
}

private fun addContact(context: Context, number: String) {
    if (number.isEmpty()) return
    try {
        val intent = Intent(Intent.ACTION_INSERT_OR_EDIT).apply {
            type = "vnd.android.cursor.item/contact"
            putExtra(ContactsContract.Intents.Insert.PHONE, number)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
