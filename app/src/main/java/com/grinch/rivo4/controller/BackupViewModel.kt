package com.grinch.rivo4.controller

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grinch.rivo4.R
import com.grinch.rivo4.modal.`interface`.ICallLogRepository
import com.grinch.rivo4.modal.`interface`.IContactsRepository
import com.grinch.rivo4.modal.data.CallLogEntry
import com.grinch.rivo4.modal.data.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class BackupViewModel(
    private val contactsRepo: IContactsRepository,
    private val callLogRepo: ICallLogRepository
) : ViewModel() {

    private val _status = MutableStateFlow<String?>(null)
    val status = _status.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun exportContacts(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contacts = contactsRepo.getContacts()
                val vCard = buildString {
                    contacts.forEach { contact ->
                        append("BEGIN:VCARD\n")
                        append("VERSION:3.0\n")
                        append("FN:${contact.name}\n")
                        contact.phoneNumbers.forEach { number ->
                            append("TEL;TYPE=CELL:$number\n")
                        }
                        contact.emails.forEach { email ->
                            append("EMAIL;TYPE=INTERNET:$email\n")
                        }
                        append("END:VCARD\n")
                    }
                }
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(vCard)
                    }
                }
                _status.value = context.getString(R.string.backup_contacts_export_success)
            } catch (e: Exception) {
                _status.value = context.getString(R.string.backup_export_failed, e.message)
            }
        }
    }

    fun importContacts(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String?
                        var currentName = ""
                        val currentNumbers = mutableListOf<String>()
                        val currentEmails = mutableListOf<String>()
                        
                        while (reader.readLine().also { line = it } != null) {
                            when {
                                line!!.startsWith("FN:") -> currentName = line!!.substring(3)
                                line!!.startsWith("TEL") -> {
                                    val parts = line!!.split(":")
                                    if (parts.size > 1) currentNumbers.add(parts[1])
                                }
                                line!!.startsWith("EMAIL") -> {
                                    val parts = line!!.split(":")
                                    if (parts.size > 1) currentEmails.add(parts[1])
                                }
                                line!!.startsWith("END:VCARD") -> {
                                    if (currentName.isNotEmpty()) {
                                        contactsRepo.saveContact(
                                            Contact(
                                                id = "0",
                                                name = currentName,
                                                phoneNumbers = currentNumbers.toList(),
                                                emails = currentEmails.toList()
                                            )
                                        )
                                    }
                                    currentName = ""
                                    currentNumbers.clear()
                                    currentEmails.clear()
                                }
                            }
                        }
                    }
                }
                _status.value = context.getString(R.string.backup_contacts_import_success)
            } catch (e: Exception) {
                _status.value = context.getString(R.string.backup_import_failed, e.message)
            }
        }
    }

    fun exportCallLogs(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val logs = callLogRepo.getCallLogs()
                val jsonString = json.encodeToString(logs)
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(jsonString)
                    }
                }
                _status.value = context.getString(R.string.backup_call_logs_export_success)
            } catch (e: Exception) {
                _status.value = context.getString(R.string.backup_export_failed, e.message)
            }
        }
    }

    fun importCallLogs(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val jsonString = BufferedReader(InputStreamReader(inputStream)).readText()
                    val logs = json.decodeFromString<List<CallLogEntry>>(jsonString)
                    logs.forEach { log ->
                        callLogRepo.saveCallLog(log)
                    }
                }
                _status.value = context.getString(R.string.backup_call_logs_import_success)
            } catch (e: Exception) {
                _status.value = context.getString(R.string.backup_import_failed, e.message)
            }
        }
    }

    fun clearStatus() {
        _status.value = null
    }
}
