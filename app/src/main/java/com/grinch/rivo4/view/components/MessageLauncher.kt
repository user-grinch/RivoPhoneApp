package com.grinch.rivo4.view.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.R
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
    val mobileLabel = stringResource(R.string.label_mobile)

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
                val validNumbers = contact.phoneNumbers.filter { it.isNotBlank() }
                if (validNumbers.size > 1) {
                    val defaultNum = prefs.getFavoriteNumber(contact.id)
                    val matchingDefault = if (defaultNum != null) {
                        validNumbers.find { areNumbersEqual(it, defaultNum) }
                    } else null

                    if (matchingDefault != null) {
                        initiateMessage(matchingDefault, contact)
                    } else {
                        pendingContact = contact
                        showNumberPicker = true
                    }
                } else if (validNumbers.isNotEmpty()) {
                    initiateMessage(validNumbers.first(), contact)
                }
            } else if (number.isNotBlank()) {
                initiateMessage(number, contact)
            }
        }
    }

    if (showNumberPicker && pendingContact != null) {
        val contact = pendingContact!!
        val lastUsed = prefs.getLastUsedNumber(contact.id)
        val defaultNum = prefs.getFavoriteNumber(contact.id)
        val validNumbers = contact.phoneNumbers.filter { it.isNotBlank() }
        
        RivoSelectionDialog(
            onDismissRequest = { 
                showNumberPicker = false 
                pendingContact = null
            },
            title = stringResource(R.string.select_number_title),
            items = validNumbers,
            itemLabel = { formatPhoneNumber(it) },
            onItemSelected = { selectedNumber ->
                showNumberPicker = false
                val c = pendingContact
                pendingContact = null
                if (c != null) {
                    prefs.setLastUsedNumber(c.id, selectedNumber)
                }
                initiateMessage(selectedNumber, c ?: contact)
            },
            itemSupporting = { mobileLabel },
            icon = Icons.AutoMirrored.Filled.Message,
            itemIcon = { if (areNumbersEqual(defaultNum, it)) Icons.Default.CheckCircle else if (areNumbersEqual(lastUsed, it)) Icons.Default.History else Icons.AutoMirrored.Filled.Message },
            isSelected = { areNumbersEqual(defaultNum, it) || areNumbersEqual(lastUsed, it) }
        )
    }

    return messageLauncher
}
