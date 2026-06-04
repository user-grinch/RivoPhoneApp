package com.grinch.rivo4.view.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
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
            if (contact != null && contact.emails.size > 1) {
                val favEmail = prefs.getFavoriteEmail(contact.id)
                if (favEmail != null) {
                    performFinalEmail(favEmail)
                } else {
                    pendingContact = contact
                    showEmailPicker = true
                }
            } else if (email.isNotEmpty()) {
                performFinalEmail(email)
            }
        }
    }

    if (showEmailPicker && pendingContact != null) {
        val contact = pendingContact!!
        
        RivoDialog(
            onDismissRequest = { showEmailPicker = false },
            title = "Select Email",
            icon = Icons.Default.Email,
            dismissButton = {
                TextButton(onClick = { showEmailPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            contact.emails.forEach { selectedEmail ->
                Surface(
                    onClick = {
                        showEmailPicker = false
                        performFinalEmail(selectedEmail)
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(16.dp))
                        Text(selectedEmail, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }

    return emailLauncher
}
