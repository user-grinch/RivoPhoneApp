package com.grinch.rivo4.view.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.grinch.rivo4.MainActivity
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.SocialAppInfo
import com.grinch.rivo4.controller.util.SocialUtils
import com.grinch.rivo4.controller.util.makeCall
import com.grinch.rivo4.view.components.RivoAvatar
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

    val timeFormatted = remember(timestampMillis) {
        DateFormat.getTimeFormat(context).format(Date(timestampMillis))
    }

    val displayName = contactName.ifBlank { phoneNumber.ifBlank { stringResource(R.string.label_unknown_number) } }

    val amberAccent = Color(0xFFE5B842)
    val amberContainer = Color(0xFF3B331A)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.92f, animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.92f, animationSpec = tween(180))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.98f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 22.dp, start = 20.dp, end = 20.dp)
                ) {
                    // 1. Header: Avatar + Missed badge + Subtitle + Close button
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
                            // Missed Call Badge
                            Surface(
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.TopStart),
                                shape = CircleShape,
                                color = amberAccent,
                                shadowElevation = 2.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.CallMissed,
                                        contentDescription = null,
                                        tint = Color(0xFF1E1C16),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (ringSeconds > 0) {
                                    stringResource(R.string.missed_call_just_now, ringSeconds)
                                } else {
                                    "Missed call • Just now"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = amberAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.action_dismiss),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // 2. View Call Logs Pill Button
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
                            .height(44.dp),
                        shape = RoundedCornerShape(50),
                        color = amberContainer
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = amberAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.missed_call_view_logs),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = amberAccent
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // 3. RESPOND WITH MESSAGE
                    Text(
                        text = stringResource(R.string.missed_call_respond_with_message),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = amberAccent,
                        letterSpacing = 0.8.sp
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickReplies.forEach { reply ->
                            Surface(
                                onClick = {
                                    sendOrComposeSms(context, phoneNumber, reply)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = reply,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    // 4. Action Row (Call, Message, WhatsApp, Telegram, Meet, Truecaller, etc.)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Call button
                        MissedCallActionItem(
                            label = stringResource(R.string.favorites_action_call),
                            containerColor = amberContainer,
                            content = {
                                Icon(
                                    imageVector = Icons.Filled.Call,
                                    contentDescription = null,
                                    tint = amberAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            onClick = {
                                makeCall(context, phoneNumber)
                                onDismiss()
                            }
                        )

                        // Message button
                        MissedCallActionItem(
                            label = stringResource(R.string.favorites_action_message),
                            containerColor = amberContainer,
                            content = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Message,
                                    contentDescription = null,
                                    tint = amberAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            onClick = {
                                SocialUtils.openSms(context, phoneNumber)
                                onDismiss()
                            }
                        )

                        // Dynamic social messaging apps
                        installedSocialApps.take(4).forEach { app ->
                            MissedCallActionItem(
                                label = app.name,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                content = {
                                    if (app.iconDrawable != null) {
                                        val bitmap = remember(app.iconDrawable) {
                                            runCatching { app.iconDrawable.toBitmap(56, 56) }.getOrNull()
                                        }
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = app.name,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        } else {
                                            Text(app.name.take(1), fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Text(app.name.take(1), fontWeight = FontWeight.Bold)
                                    }
                                },
                                onClick = {
                                    app.action(context, phoneNumber)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MissedCallActionItem(
    label: String,
    containerColor: Color,
    content: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(62.dp)
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = containerColor,
            shadowElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                content()
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun sendOrComposeSms(context: Context, number: String, text: String) {
    val intl = SocialUtils.formatInternationalNumber(context, number)
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$intl")).apply {
        putExtra("sms_body", text)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {}
}
