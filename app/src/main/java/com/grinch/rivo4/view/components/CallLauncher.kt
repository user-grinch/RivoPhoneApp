package com.grinch.rivo4.view.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telecom.TelecomManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.grinch.rivo4.controller.util.*
import com.grinch.rivo4.modal.data.Contact
import org.koin.compose.koinInject

class CallLauncher(
    private val onInitiate: (String, Contact?) -> Unit
) {
    fun dial(number: String, contact: Contact? = null) {
        onInitiate(number, contact)
    }
}

@Composable
fun rememberCallLauncher(): CallLauncher {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val telecomManager = remember { context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager }

    var showSimPicker by remember { mutableStateOf(false) }
    var showNumberPicker by remember { mutableStateOf(false) }
    var pendingNumber by remember { mutableStateOf("") }
    var pendingContact by remember { mutableStateOf<Contact?>(null) }

    val performFinalCall = { number: String, contactId: String? ->
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            val accounts = try { telecomManager.callCapablePhoneAccounts } catch (e: SecurityException) { emptyList() }
            
            val favSim = contactId?.let { prefs.getFavoriteSim(it) }
            val favNum = contactId?.let { prefs.getFavoriteNumber(it) }
            val preferredHandle = accounts.find { it.id == favSim }

            if (preferredHandle != null && areNumbersEqual(number, favNum)) {
                makeCall(context, number, preferredHandle, contactId = contactId)
            } else if (accounts.size > 1 && prefs.getInt("default_sim", 0) == 0) {
                pendingNumber = number
                // Keep pendingContact as is
                showSimPicker = true
            } else {
                makeCall(context, number, contactId = contactId)
            }
        } else {
            makeCall(context, number, contactId = contactId)
        }
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CALL_PHONE] == true) {
            performFinalCall(pendingNumber, pendingContact?.id)
        }
    }

    val initiateCall = { number: String, contact: Contact? ->
        pendingNumber = number
        pendingContact = contact
        
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            performFinalCall(number, contact?.id)
        } else {
            callPermissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE))
        }
    }

    val callLauncher = remember {
        CallLauncher { number, contact ->
            if (contact != null && contact.phoneNumbers.size > 1) {
                val favNum = prefs.getFavoriteNumber(contact.id)
                if (favNum != null) {
                    initiateCall(favNum, contact)
                } else {
                    pendingContact = contact
                    showNumberPicker = true
                }
            } else {
                initiateCall(number, contact)
            }
        }
    }

    if (showNumberPicker && pendingContact != null) {
        val contact = pendingContact!!
        val lastUsed = prefs.getLastUsedNumber(contact.id)
        
        RivoDialog(
            onDismissRequest = { showNumberPicker = false },
            title = "Select Number",
            icon = Icons.Default.Phone,
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
                        initiateCall(selectedNumber, contact)
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isRecent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Phone, null, tint = if (isRecent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
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

    if (showSimPicker) {
        SimPickerDialog(
            onDismissRequest = { showSimPicker = false },
            onSimSelected = { handle ->
                val contactId = pendingContact?.id ?: (pendingNumber.let { num ->
                   null
                })
                
                val favNum = contactId?.let { prefs.getFavoriteNumber(it) }
                if (contactId != null && areNumbersEqual(pendingNumber, favNum)) {
                    prefs.setFavoriteSim(contactId, handle.id)
                }
                
                makeCall(context, pendingNumber, handle, contactId = contactId)
                showSimPicker = false
            }
        )
    }

    return callLauncher
}
