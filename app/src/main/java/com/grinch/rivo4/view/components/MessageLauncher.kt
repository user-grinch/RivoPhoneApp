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
            if (number.isBlank() && contact != null) {
                if (contact.phoneNumbers.size > 1) {
                    val favNum = prefs.getFavoriteNumber(contact.id)
                    if (favNum != null) {
                        initiateMessage(favNum, contact)
                    } else {
                        pendingContact = contact
                        showNumberPicker = true
                    }
                } else if (contact.phoneNumbers.isNotEmpty()) {
                    initiateMessage(contact.phoneNumbers.first(), contact)
                }
            } else if (number.isNotBlank()) {
                initiateMessage(number, contact)
            }
        }
    }

    if (showNumberPicker && pendingContact != null) {
        val contact = pendingContact!!
        val lastUsed = prefs.getLastUsedNumber(contact.id)
        
        RivoSelectionDialog(
            onDismissRequest = { showNumberPicker = false },
            title = "Select Number",
            items = contact.phoneNumbers,
            itemLabel = { formatPhoneNumber(it) },
            onItemSelected = { selectedNumber ->
                initiateMessage(selectedNumber, contact)
            },
            itemSupporting = { "Mobile" },
            icon = Icons.AutoMirrored.Filled.Message,
            itemIcon = { Icons.AutoMirrored.Filled.Message },
            isSelected = { areNumbersEqual(lastUsed, it) }
        )
    }

    return messageLauncher
}
