package com.grinch.rivo4.view.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.controller.util.*
import com.grinch.rivo4.modal.data.Contact
import org.koin.compose.koinInject

class MessageLauncher(
    private val onInitiate: (String, Contact?) -> Unit
) {
    fun sendMessage(number: String, contact: Contact? = null) {
        onInitiate(number, contact)
    }
}

@Composable
fun rememberMessageLauncher(): MessageLauncher {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()

    var showNumberPicker by remember { mutableStateOf(false) }
    var pendingContact by remember { mutableStateOf<Contact?>(null) }

    val performFinalMessage = { number: String ->
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$number"))
        context.startActivity(intent)
    }

    val initiateMessage = { number: String, contact: Contact? ->
        performFinalMessage(number)
    }

    val messageLauncher = remember {
        MessageLauncher { number, contact ->
            if (contact != null && contact.phoneNumbers.size > 1) {
                val favNum = prefs.getFavoriteNumber(contact.id)
                if (favNum != null) {
                    initiateMessage(favNum, contact)
                } else {
                    pendingContact = contact
                    showNumberPicker = true
                }
            } else {
                initiateMessage(number, contact)
            }
        }
    }

    if (showNumberPicker && pendingContact != null) {
        val contact = pendingContact!!
        val lastUsed = prefs.getLastUsedNumber(contact.id)
        
        RivoDialog(
            onDismissRequest = { showNumberPicker = false },
            title = "Select Number",
            icon = Icons.AutoMirrored.Filled.Message,
            dismissButton = {
                TextButton(onClick = { showNumberPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            contact.phoneNumbers.forEach { selectedNumber ->
                val isRecent = lastUsed != null && areNumbersEqual(lastUsed, selectedNumber)
                Surface(
                    onClick = {
                        showNumberPicker = false
                        initiateMessage(selectedNumber, contact)
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isRecent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Message, null, tint = if (isRecent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(formatPhoneNumber(selectedNumber), style = MaterialTheme.typography.bodyLarge, fontWeight = if (isRecent) FontWeight.Bold else FontWeight.Normal)
                            if (isRecent) {
                                Text("Recent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    return messageLauncher
}
