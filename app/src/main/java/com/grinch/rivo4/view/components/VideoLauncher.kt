package com.grinch.rivo4.view.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.controller.util.*
import com.grinch.rivo4.modal.data.Contact
import org.koin.compose.koinInject

class VideoLauncher(
    private val onInitiate: (String, Contact?) -> Unit
) {
    fun startVideoCall(number: String, contact: Contact? = null) {
        onInitiate(number, contact)
    }
}

@Composable
fun rememberVideoLauncher(): VideoLauncher {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()

    var showAppPicker by remember { mutableStateOf(false) }
    var pendingNumber by remember { mutableStateOf("") }

    val launchApp = { pkg: String, number: String ->
        val uri = Uri.parse("tel:$number")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(pkg)
            if (pkg == "com.google.android.apps.meetings") { // Google Meet
                data = Uri.parse("https://meet.google.com/")
            }
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val chooser = Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), "Video Call with")
            context.startActivity(chooser)
        }
    }

    val videoLauncher = remember {
        VideoLauncher { number, contact ->
            pendingNumber = number
            showAppPicker = true
        }
    }

    if (showAppPicker) {
        val apps = listOf(
            "WhatsApp" to "com.whatsapp",
            "Google Meet" to "com.google.android.apps.meetings",
            "Zoom" to "us.zoom.videomeetings",
            "Telegram" to "org.telegram.messenger"
        )

        RivoSelectionDialog(
            onDismissRequest = { showAppPicker = false },
            title = "Video Call",
            items = apps + ("System Video Call" to "system"),
            itemLabel = { it.first },
            onItemSelected = { (name, pkg) ->
                if (pkg == "system") {
                    val uri = Uri.parse("tel:$pendingNumber")
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setDataAndType(uri, "vnd.android.cursor.item/video-chat-address")
                    }
                    context.startActivity(Intent.createChooser(intent, "Video Call"))
                } else {
                    launchApp(pkg, pendingNumber)
                }
            },
            icon = Icons.Default.VideoCall,
            itemIcon = { Icons.Default.VideoCall }
        )
    }

    return videoLauncher
}
