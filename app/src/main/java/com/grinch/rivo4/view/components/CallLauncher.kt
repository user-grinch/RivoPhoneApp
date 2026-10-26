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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.grinch.rivo4.R
import com.grinch.rivo4.controller.util.*
import com.grinch.rivo4.modal.data.Contact
import com.grinch.rivo4.modal.`interface`.IContactsRepository
import kotlinx.coroutines.launch
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
    val mobileLabel = stringResource(R.string.label_mobile)
    val telecomManager = remember { context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager }

    var showSimPicker by remember { mutableStateOf(false) }
    var showNumberPicker by remember { mutableStateOf(false) }
    var pendingNumber by remember { mutableStateOf("") }
    var pendingContact by remember { mutableStateOf<Contact?>(null) }

    val contactsRepo = koinInject<IContactsRepository>()
    val coroutineScope = rememberCoroutineScope()

    val performFinalCall = { number: String, contactId: String? ->
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            val accounts = try { telecomManager.callCapablePhoneAccounts } catch (e: SecurityException) { emptyList() }

            val favSim = contactId?.let { prefs.getDefaultSimForContact(it) }
            val preferredHandle = if (favSim != null) accounts.find { it.id == favSim } else null

            if (preferredHandle != null) {
                makeCall(context, number, preferredHandle, contactId = contactId)
            } else if (accounts.size > 1 && prefs.getInt("default_sim", 0) == 0) {
                pendingNumber = number
                if (pendingContact == null && number.isNotBlank()) {
                    coroutineScope.launch {
                        try {
                            val resolved = contactsRepo.getContactByNumber(number)
                            if (resolved != null) {
                                pendingContact = resolved
                                val resolvedFavSim = prefs.getDefaultSimForContact(resolved.id)
                                val resolvedHandle = if (resolvedFavSim != null) accounts.find { it.id == resolvedFavSim } else null
                                if (resolvedHandle != null) {
                                    makeCall(context, number, resolvedHandle, contactId = resolved.id)
                                    return@launch
                                }
                            }
                        } catch (_: Exception) {}
                        showSimPicker = true
                    }
                } else {
                    showSimPicker = true
                }
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
            if (number.isBlank() && contact != null) {
                val validNumbers = contact.phoneNumbers.filter { it.isNotBlank() }
                if (validNumbers.size > 1) {
                    val defaultNum = prefs.getFavoriteNumber(contact.id)
                    val matchingDefault = if (defaultNum != null) {
                        validNumbers.find { areNumbersEqual(it, defaultNum) }
                    } else null

                    if (matchingDefault != null) {
                        initiateCall(matchingDefault, contact)
                    } else {
                        pendingContact = contact
                        showNumberPicker = true
                    }
                } else if (validNumbers.isNotEmpty()) {
                    initiateCall(validNumbers.first(), contact)
                }
            } else if (number.isNotBlank()) {
                initiateCall(number, contact)
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
                initiateCall(selectedNumber, c ?: contact)
            },
            itemSupporting = { mobileLabel },
            icon = Icons.Default.Phone,
            itemIcon = { if (areNumbersEqual(defaultNum, it)) Icons.Default.CheckCircle else if (areNumbersEqual(lastUsed, it)) Icons.Default.History else Icons.Default.Phone },
            isSelected = { areNumbersEqual(defaultNum, it) || areNumbersEqual(lastUsed, it) }
        )
    }

    if (showSimPicker) {
        val targetContact = pendingContact
        SimPickerDialog(
            onDismissRequest = { showSimPicker = false },
            onSimSelected = { handle ->
                makeCall(context, pendingNumber, handle, contactId = targetContact?.id)
                showSimPicker = false
            },
            showRememberOption = targetContact != null,
            onSimSelectedWithRemember = { handle, rememberForContact ->
                if (rememberForContact && targetContact != null) {
                    prefs.setDefaultSimForContact(targetContact.id, handle.id)
                }
                makeCall(context, pendingNumber, handle, contactId = targetContact?.id)
                showSimPicker = false
            }
        )
    }

    return callLauncher
}
