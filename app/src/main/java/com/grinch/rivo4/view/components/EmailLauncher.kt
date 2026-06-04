package com.grinch.rivo4.view.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Star
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

class EmailLauncher(
    private val onInitiate: (String, Contact?) -> Unit
) {
    fun sendEmail(email: String, contact: Contact? = null) {
        onInitiate(email, contact)
    }
}

@Composable
fun rememberEmailLauncher(): EmailLauncher {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()

    var showEmailPicker by remember { mutableStateOf(false) }
    var pendingContact by remember { mutableStateOf<Contact?>(null) }

    val performFinalEmail = { email: String ->
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
        context.startActivity(intent)
    }

    val emailLauncher = remember {
        EmailLauncher { email, contact ->
            if (email.isBlank() && contact != null && contact.emails.size > 1) {
                val favEmail = prefs.getFavoriteEmail(contact.id)
                if (favEmail != null) {
                    performFinalEmail(favEmail)
                } else {
                    pendingContact = contact
                    showEmailPicker = true
                }
            } else if (email.isNotBlank()) {
                performFinalEmail(email)
            } else if (contact?.emails?.isNotEmpty() == true) {
                performFinalEmail(contact.emails.first())
            }
        }
    }

    if (showEmailPicker && pendingContact != null) {
        val contact = pendingContact!!
        val favEmail = prefs.getFavoriteEmail(contact.id)

        RivoSelectionDialog(
            onDismissRequest = { showEmailPicker = false },
            title = "Select Email",
            items = contact.emails,
            itemLabel = { it },
            onItemSelected = { performFinalEmail(it) },
            itemSupporting = { "Email" },
            icon = Icons.Default.Email,
            itemIcon = { if (it == favEmail) Icons.Default.Star else Icons.Default.Email },
            isSelected = { it == favEmail }
        )
    }

    return emailLauncher
}
